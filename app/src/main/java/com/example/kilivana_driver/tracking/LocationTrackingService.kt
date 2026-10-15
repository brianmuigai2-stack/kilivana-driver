package com.example.kilivana_driver.tracking

import java.time.Instant
import com.example.kilivana_driver.data.network.TrackingRepository
import com.example.kilivana_driver.data.model.ReportLocationRequest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.kilivana_driver.R
import com.example.kilivana_driver.data.model.LocationUpdate
import com.example.kilivana_driver.data.model.TrackingConfig
import com.example.kilivana_driver.data.network.LocationRepository
import com.example.kilivana_driver.data.network.SessionStore
import com.example.kilivana_driver.data.network.TripRepository
import com.example.kilivana_driver.notifications.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground service for continuous GPS tracking during active deliveries.
 *
 * Lifecycle:
 * 1. Started by TripViewModel/JobRouteScreen when driver begins a trip.
 * 2. Requests location updates via FusedLocationProviderClient.
 * 3. Uploads locations to backend via LocationRepository.
 * 4. Shows persistent notification "Kilivana — Delivery in progress".
 * 5. Stopped when trip completes, cancels, or driver ends it.
 *
 * Android 12+ (API 31): Requires FOREGROUND_SERVICE_LOCATION permission
 * and foreground service type "location".
 *
 * Android 14+ (API 34): Requires explicit user consent for foreground
 * service location access (handled by the OS when the notification is shown).
 */
class LocationTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = NotificationHelper.CHANNEL_TRACKING_ID
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.kilivana_driver.tracking.START"
        const val ACTION_STOP = "com.example.kilivana_driver.tracking.STOP"
        const val EXTRA_TRIP_ID = "trip_id"

        /** Starts the tracking service for a trip. */
        fun startTracking(context: Context, tripId: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        /** Stops the tracking service. */
        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val scopeJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + scopeJob)
    private val locationRepository = LocationRepository()
    private val trackingRepository = TrackingRepository()
    private val tripRepository = TripRepository(locationRepository = locationRepository)
    private val trackingConfig = TrackingConfig.DEFAULT

    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var notificationManager: NotificationManager? = null
    private var currentTripId: String? = null
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val tripId = intent?.getStringExtra(EXTRA_TRIP_ID)

        when (action) {
            ACTION_START -> {
                if (tripId != null && !isTracking) {
                    startTrackingInternal(tripId)
                }
            }
            ACTION_STOP -> {
                stopTrackingInternal()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTrackingInternal(tripId: String) {
        currentTripId = tripId
        isTracking = true

        // Show persistent notification
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start location updates
        requestLocationUpdates()

        // Check for active trip on backend (recovery case)
        scope.launch {
            tripRepository.checkForActiveTrip()
        }
    }

    private fun stopTrackingInternal() {
        isTracking = false
        stopLocationUpdates()
        locationRepository.clearQueue()
        tripRepository.clear()
        stopForeground(true)
        stopSelf()
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, trackingConfig.updateIntervalMs)
            .setMinUpdateIntervalMillis(trackingConfig.updateIntervalMs / 2)
            .setMaxUpdateDelayMillis(trackingConfig.updateIntervalMs * 2)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onLocationReceived(location)
            }
        }

        try {
            fusedClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // Permission revoked; stop tracking.
            stopTrackingInternal()
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedClient?.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    private fun onLocationReceived(location: Location) {
        val tripId = currentTripId ?: return
        val stage = tripRepository.tripStage.value

        // Only upload during active tracking stages.
        if (stage == null || stage !in setOf(
                com.example.kilivana_driver.data.model.TripStage.HEADING_TO_PICKUP,
                com.example.kilivana_driver.data.model.TripStage.IN_TRANSIT
            )) {
            return
        }

        val jobId = tripId.toLongOrNull() ?: return
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6) else null
        val bearing = if (location.hasBearing()) location.bearing.toDouble() else null
        val accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else null
        val request = ReportLocationRequest(
            tripId = jobId,
            latitude = location.latitude,
            longitude = location.longitude,
            speedKmh = speedKmh,
            bearing = bearing,
            accuracyMetres = accuracy,
            clientTimestamp = java.time.Instant.now().toString()
        )
        scope.launch {
            trackingRepository.reportLocation(jobId, request)
        }
    }

    private fun buildNotification(): Notification {
        val title = "Kilivana — Delivery in progress"
        val text = currentTripId?.let { "Tracking trip $it" } ?: "Active delivery"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with proper icon
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority so it doesn't intrude
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Delivery Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when a delivery is being actively tracked"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        notificationManager?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        scopeJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}