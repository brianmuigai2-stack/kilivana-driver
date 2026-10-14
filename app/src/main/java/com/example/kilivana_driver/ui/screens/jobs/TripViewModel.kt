package com.example.kilivana_driver.ui.screens.jobs

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.model.LocationUpdate
import com.example.kilivana_driver.data.model.TrackingConfig
import com.example.kilivana_driver.data.model.TrackingHistoryPoint
import com.example.kilivana_driver.data.model.TripStage
import com.example.kilivana_driver.data.model.UploadResult
import com.example.kilivana_driver.data.network.JobsRepository
import com.example.kilivana_driver.data.network.LocationRepository
import com.example.kilivana_driver.data.network.SessionStore
import com.example.kilivana_driver.data.network.TripRepository
import com.example.kilivana_driver.ui.screens.map.GeoPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the active trip (En Route screen).
 *
 * Coordinates:
 * - GPS location stream (from FusedLocationProvider via rememberUserLocation)
 * - Route calculation (Mapbox Directions via RouteRepository)
 * - Backend location uploads (via LocationRepository)
 * - Trip lifecycle (via TripRepository)
 * - Smooth vehicle animation (via CarMarker logic)
 *
 * This ViewModel is scoped to the JobRouteScreen lifecycle. The foreground
 * LocationTrackingService uses its own LocationRepository instance for
 * background uploads, but they share the same TripRepository state via
 * SessionStore and the active trip ID.
 */
class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository()
    private val tripRepository = TripRepository(locationRepository = locationRepository)
    private val jobsRepository = JobsRepository()
    private val trackingConfig = TrackingConfig.DEFAULT

    // ---- Inputs from the screen ----
    /** The job being tracked. Set once when the screen opens. */
    private var _job: Job? = null
    val job: Job? get() = _job

    /** Current GPS fix from rememberUserLocation (set by the screen). */
    var currentLocation: Location? = null
        private set

    /** Destination for the current stage (farmer or buyer). */
    private var _targetGeoPoint: GeoPoint? = null
    val targetGeoPoint: GeoPoint? get() = _targetGeoPoint

    // ---- Derived state ----
    /** Current trip stage (heading to pickup / in transit / delivered). */
    val tripStage: StateFlow<TripStage?> = tripRepository.tripStage

    /** True while a trip operation (start/stop/sync) is in progress. */
    val isBusy: StateFlow<Boolean> = tripRepository.isBusy

    /** Last error from trip operations. */
    val lastError: StateFlow<String?> = tripRepository.lastError

    /** Active trip ID (null when no trip is being tracked). */
    val activeTripId: StateFlow<String?> = tripRepository.activeTripId

    /** Current route points for the active leg. */
    private val _routePoints = MutableStateFlow<List<GeoPoint>?>(null)
    val routePoints: StateFlow<List<GeoPoint>?> = _routePoints.asStateFlow()

    /** Whether the current route is a fallback straight line (route service failed). */
    var _usingEstimatedRoute = MutableStateFlow(false)
    val usingEstimatedRoute: StateFlow<Boolean> = _usingEstimatedRoute.asStateFlow()

    /** Distance label for the current leg (e.g., "12.3 km to farmer"). */
    private val _distanceLabel = MutableStateFlow<String>("")
    val distanceLabel: StateFlow<String> = _distanceLabel.asStateFlow()

    /** Last known location upload result (for debugging/diagnostics). */
    private val _lastUploadResult = MutableStateFlow<UploadResult?>(null)
    val lastUploadResult: StateFlow<UploadResult?> = _lastUploadResult.asStateFlow()

    /** Offline queue size (for diagnostics). */
    val queueSize: StateFlow<Int> = locationRepository.queueSize

    /** GPS fixes the driver uploaded for this job, fetched from GET /tracking. */
    private val _trackingPoints = MutableStateFlow<List<TrackingHistoryPoint>>(emptyList())
    val trackingPoints: StateFlow<List<TrackingHistoryPoint>> = _trackingPoints.asStateFlow()

    /** Last error from a tracking-history fetch, if any. */
    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()

    // ---- Heading/animation state (mirrors CarMarker logic) ----
    private var lastLocationForHeading: GeoPoint? = null
    private var _currentHeading = 0f
    private var hasInitialHeading = false
    private var lastRouteFetchAtMs = 0L

    /** Initializes the ViewModel with a job and starts tracking. */
    fun initialize(job: Job) {
        _job = job
        _targetGeoPoint = GeoPoint(job.pickupLat, job.pickupLon)
        _routePoints.value = null
        _usingEstimatedRoute.value = false
        _distanceLabel.value = "Finding route to farmer…"
        lastRouteFetchAtMs = 0L
        loadTrackingHistory(job.id)
    }

    /** Fetches the GPS tracking history for this job from the backend. */
    private fun loadTrackingHistory(jobId: String) {
        val id = jobId.toLongOrNull() ?: return
        viewModelScope.launch {
            val result = jobsRepository.getTrackingHistory(id)
            if (result.isSuccess) {
                _trackingPoints.value = result.getOrNull().orEmpty()
                _trackingError.value = null
            } else {
                _trackingError.value = result.exceptionOrNull()?.message
            }
        }
    }

    /** Called by the screen when a new GPS fix arrives. */
    fun onLocationUpdate(location: Location) {
        currentLocation = location
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        // Update heading for vehicle rotation.
        updateHeading(location)

        // Update vehicle position on map (handled by screen via CarMarker).
        // Screen will call getCurrentVehiclePosition().

        // Upload location to backend if tracking is active.
        maybeUploadLocation(location)

        // Maybe recalculate route if we've moved far enough.
        maybeRecalculateRoute(geoPoint)
    }

    /** Starts tracking for the current job (driver tapped "Start Trip"). */
    fun startTrip() {
        _job?.let { job ->
            viewModelScope.launch {
                tripRepository.startTracking(job)
                // The route is fetched regardless of whether the backend tracking
                // endpoint succeeded — Mapbox route lookup is a local operation that
                // must not be blocked by a backend endpoint that may not be
                // implemented yet (TripApi documents these as "should implement").
                fetchRouteToTarget()
            }
        }
    }

    /** Advances to IN_TRANSIT stage (driver tapped "Mark as Picked Up"). */
    fun markPickedUp() {
        tripRepository.advanceToInTransit()
        _job?.let { job ->
            _targetGeoPoint = GeoPoint(job.dropoffLat, job.dropoffLon)
            _routePoints.value = null
            _usingEstimatedRoute.value = false
            _distanceLabel.value = "Finding route to buyer…"
            lastRouteFetchAtMs = 0L
            fetchRouteToTarget()
        }
    }

    /** Completes the delivery (driver verified OTP). */
    fun completeDelivery() {
        viewModelScope.launch {
            tripRepository.stopTracking("COMPLETED")
        }
    }

    /** Cancels the trip. */
    fun cancelTrip() {
        viewModelScope.launch {
            tripRepository.stopTracking("CANCELLED")
        }
    }

    /** Syncs trip status from backend (on resume/reconnect). */
    fun syncFromBackend() {
        viewModelScope.launch {
            tripRepository.syncStatus()
        }
    }

    /** Checks for an active trip on startup (process death recovery). */
    fun checkForActiveTrip() {
        viewModelScope.launch {
            tripRepository.checkForActiveTrip()
        }
    }

    /** Gets the current vehicle position for the map marker. */
    fun getCurrentVehiclePosition(): GeoPoint? =
        currentLocation?.let { GeoPoint(it.latitude, it.longitude) }

    /** Gets the current vehicle heading (degrees, 0-360). */
    fun getCurrentHeading(): Float = _currentHeading

    /** Sets the current vehicle heading (called from screen for CarMarker sync). */
    fun setCurrentHeading(heading: Float) {
        _currentHeading = heading
    }

    /** Gets the pickup point. */
    fun getPickupPoint(): GeoPoint? = _job?.let { GeoPoint(it.pickupLat, it.pickupLon) }

    /** Gets the dropoff point. */
    fun getDropoffPoint(): GeoPoint? = _job?.let { GeoPoint(it.dropoffLat, it.dropoffLon) }

    // ---- Private helpers ----

    private fun updateHeading(location: Location) {
        val now = GeoPoint(location.latitude, location.longitude)
        val previous = lastLocationForHeading

        when {
            location.hasBearing() && location.speed >= trackingConfig.minSpeedForBearing -> {
                _currentHeading = location.bearing
                lastLocationForHeading = now
                hasInitialHeading = true
            }
            previous != null && previous.distanceToAsDouble(now) >= MIN_MOVE_METERS_FOR_HEADING -> {
                _currentHeading = previous.bearingTo(now).toFloat()
                lastLocationForHeading = now
                hasInitialHeading = true
            }
            !hasInitialHeading && _targetGeoPoint != null -> {
                // First fix: point towards the target.
                _currentHeading = now.bearingTo(_targetGeoPoint!!).toFloat()
                lastLocationForHeading = now
                hasInitialHeading = true
            }
        }
    }

    private fun maybeUploadLocation(location: Location) {
        val tripId = tripRepository.activeTripId.value ?: return
        val stage = tripRepository.tripStage.value ?: return

        // Only upload during active tracking stages.
        if (stage !in setOf(TripStage.HEADING_TO_PICKUP, TripStage.IN_TRANSIT)) return

        val update = LocationUpdate(
            tripId = tripId,
            latitude = location.latitude,
            longitude = location.longitude,
            bearing = if (location.hasBearing()) location.bearing else null,
            speed = if (location.hasSpeed()) location.speed else null,
            accuracy = if (location.hasAccuracy()) location.accuracy else null
        )

        // Fire-and-forget: the repository handles queuing/retry.
        viewModelScope.launch {
            val result = locationRepository.uploadNow(update)
            _lastUploadResult.value = result
        }
    }

    /** Requests a route when missing or sufficiently far from its origin, respecting the minimum refetch interval. */
    private fun maybeRecalculateRoute(current: GeoPoint) {
        val stage = tripRepository.tripStage.value ?: TripStage.HEADING_TO_PICKUP
        val target = _targetGeoPoint ?: return
        val route = _routePoints.value
        val now = System.currentTimeMillis()

        // Recalculate if we don't have a route yet, or if we've moved
        // far enough from the last route origin.
        val shouldRecalculate = when {
            route == null -> true
            stage == TripStage.HEADING_TO_PICKUP -> {
                // For pickup leg, recalculate from driver's current position.
                // We use a simple distance check from the route start.
                val routeStart = route.firstOrNull()
                routeStart == null || current.distanceToAsDouble(routeStart) > REROUTE_DISTANCE_METERS
            }
            stage == TripStage.IN_TRANSIT -> {
                // For delivery leg, same logic.
                val routeStart = route.firstOrNull()
                routeStart == null || current.distanceToAsDouble(routeStart) > REROUTE_DISTANCE_METERS
            }
            else -> false
        }

        if (shouldRecalculate && (route == null || now - lastRouteFetchAtMs >= REROUTE_MIN_INTERVAL_MS)) {
            fetchRouteToTarget()
        }
    }

    /** Fetches the route from the current location to the target on IO and updates its points, fallback flag and distance label. */
    private fun fetchRouteToTarget() {
        val target = _targetGeoPoint ?: return
        val current = currentLocation?.let { GeoPoint(it.latitude, it.longitude) } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            lastRouteFetchAtMs = System.currentTimeMillis()
            val result = com.example.kilivana_driver.data.routing.RouteRepository.fetchRoute(current, target)
            _routePoints.value = result.points
            _usingEstimatedRoute.value = !result.isRealRoute
            _distanceLabel.value = when (tripRepository.tripStage.value ?: TripStage.HEADING_TO_PICKUP) {
                TripStage.HEADING_TO_PICKUP -> "%.1f km to farmer".format(
                    com.example.kilivana_driver.data.routing.RouteRepository.routeLengthKm(result.points)
                )
                TripStage.IN_TRANSIT -> "%.1f km to buyer".format(
                    com.example.kilivana_driver.data.routing.RouteRepository.routeLengthKm(result.points)
                )
                else -> ""
            }
        }
    }

    /** Cleans up when the trip ends or screen is destroyed. */
    override fun onCleared() {
        super.onCleared()
        // Don't stop tracking here — the service continues in background.
        // The screen calls completeDelivery() or cancelTrip() explicitly.
    }

    companion object {
        /** Minimum distance (metres) between route recalculations. */
        const val REROUTE_DISTANCE_METERS = 300.0
        /** Minimum time (ms) between route recalculations. */
        const val REROUTE_MIN_INTERVAL_MS = 15_000L
        /** Minimum distance (metres) moved before using GPS-derived heading. */
        const val MIN_MOVE_METERS_FOR_HEADING = 10.0
    }
}
