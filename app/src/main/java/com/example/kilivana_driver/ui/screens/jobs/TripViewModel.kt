package com.example.kilivana_driver.ui.screens.jobs

import android.app.Application
import android.net.Uri
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.model.ProofOfDelivery
import com.example.kilivana_driver.data.model.TrackingConfig
import com.example.kilivana_driver.data.model.TripStage
import com.example.kilivana_driver.data.network.JobsRepository
import com.example.kilivana_driver.data.network.LocationRepository
import com.example.kilivana_driver.data.network.TripRepository
import com.example.kilivana_driver.ui.screens.map.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the active trip (En Route screen).
 *
 * Coordinates:
 * - GPS location stream (from FusedLocationProvider via rememberUserLocation)
 * - Route calculation (Mapbox Directions via RouteRepository)
 * - Active-trip state (via TripRepository)
 * - Trip lifecycle (via TripRepository)
 * - Smooth vehicle animation (via CarMarker logic)
 *
 * This ViewModel is scoped to the JobRouteScreen lifecycle. The foreground
 * The foreground LocationTrackingService is the sole GPS uploader, avoiding
 * duplicate fixes while this screen is visible.
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

    private val _isSubmittingProof = MutableStateFlow(false)
    val isSubmittingProof: StateFlow<Boolean> = _isSubmittingProof.asStateFlow()

    private val _isVerifyingOtp = MutableStateFlow(false)
    val isVerifyingOtp: StateFlow<Boolean> = _isVerifyingOtp.asStateFlow()

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError.asStateFlow()

    private val _otpVerified = MutableStateFlow(false)
    val otpVerified: StateFlow<Boolean> = _otpVerified.asStateFlow()
    private var otpVerifiedJobId: String? = null

    private val _proofError = MutableStateFlow<String?>(null)
    val proofError: StateFlow<String?> = _proofError.asStateFlow()

    private val _proofFiled = MutableStateFlow(false)
    val proofFiled: StateFlow<Boolean> = _proofFiled.asStateFlow()

    /** Active trip ID (null when no trip is being tracked). */
    val activeTripId: StateFlow<String?> = tripRepository.activeTripId
    val trackingActive: StateFlow<Boolean> = tripRepository.trackingActive

    /** Current route points for the active leg. */
    private val _routePoints = MutableStateFlow<List<GeoPoint>?>(null)
    val routePoints: StateFlow<List<GeoPoint>?> = _routePoints.asStateFlow()

    /** Whether the current route is a fallback straight line (route service failed). */
    var _usingEstimatedRoute = MutableStateFlow(false)
    val usingEstimatedRoute: StateFlow<Boolean> = _usingEstimatedRoute.asStateFlow()

    /** Distance label for the current leg (e.g., "12.3 km to farmer"). */
    private val _distanceLabel = MutableStateFlow<String>("")
    val distanceLabel: StateFlow<String> = _distanceLabel.asStateFlow()

    // ---- Heading/animation state (mirrors CarMarker logic) ----
    private var lastLocationForHeading: GeoPoint? = null
    private var _currentHeading = 0f
    private var hasInitialHeading = false
    private var lastRouteFetchAtMs = 0L

    /** Initializes the ViewModel with a job and starts tracking. */
    fun initialize(job: Job) {
        _job = job
        _isSubmittingProof.value = false
        _isVerifyingOtp.value = false
        _otpError.value = null
        _otpVerified.value = otpVerifiedJobId == job.id
        if (!_otpVerified.value) otpVerifiedJobId = null
        _proofError.value = null
        _proofFiled.value = false
        tripRepository.restoreSavedStatus(job.backendStatus, job.tripId)
        val isInTransit = TripRepository.tripStageForStatus(job.backendStatus.orEmpty()) ==
            TripStage.IN_TRANSIT
        _targetGeoPoint = if (isInTransit) {
            GeoPoint(job.dropoffLat, job.dropoffLon)
        } else {
            GeoPoint(job.pickupLat, job.pickupLon)
        }
        _routePoints.value = null
        _usingEstimatedRoute.value = false
        _distanceLabel.value = if (isInTransit) "Finding route to buyer…" else "Finding route to farmer…"
        lastRouteFetchAtMs = 0L
    }

    /** Called by the screen when a new GPS fix arrives. */
    fun onLocationUpdate(location: Location) {
        currentLocation = location
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        // Update heading for vehicle rotation.
        updateHeading(location)

        // Update vehicle position on map (handled by screen via CarMarker).
        // Screen will call getCurrentVehiclePosition().

        // Maybe recalculate route if we've moved far enough.
        maybeRecalculateRoute(geoPoint)
    }

    /** Starts tracking for the current job (driver tapped "Start Trip"). */
    fun startTrip() {
        val job = _job ?: return
        _proofError.value = null
        viewModelScope.launch {
            val tripId = resolveTripId(job)
            if (tripId == null) {
                _proofError.value = tripRepository.lastError.value
                    ?: "Could not find a trip for this job. Refresh jobs or contact dispatch."
                return@launch
            }
            if (!tripRepository.startTracking(tripId)) {
                _proofError.value = tripRepository.lastError.value ?: "Could not start delivery tracking."
            }
            fetchRouteToTarget()
        }
    }

    /** Advances to IN_TRANSIT stage (driver tapped "Mark as Picked Up"). */
    fun markPickedUp() {
        val job = _job ?: return
        _proofError.value = null
        viewModelScope.launch {
            val tripId = resolveTripId(job)
            if (tripId == null) {
                _proofError.value = tripRepository.lastError.value
                    ?: "Could not find a trip for this job. Refresh jobs or contact dispatch."
                return@launch
            }
            val result = tripRepository.confirmPickup(tripId)
            if (result.isFailure) {
                _proofError.value = result.exceptionOrNull()?.message ?: "Could not confirm pickup."
                return@launch
            }
            _targetGeoPoint = GeoPoint(job.dropoffLat, job.dropoffLon)
            _routePoints.value = null
            _usingEstimatedRoute.value = false
            _distanceLabel.value = "Finding route to buyer…"
            lastRouteFetchAtMs = 0L
            fetchRouteToTarget()
        }
    }

    /** Resolves this job's trip before considering the driver's current trip. */
    private suspend fun resolveTripId(job: Job): String? =
        job.tripId?.takeIf { it.isNotBlank() }
            ?: tripRepository.resolveTripIdForJob(job.id)
            ?: tripRepository.activeTripId.value?.takeIf { it.isNotBlank() }


    /** Validates the buyer's OTP before moving on to evidence collection. */
    fun verifyDeliveryOtp(otp: String) {
        if (_isVerifyingOtp.value || _otpVerified.value) return
        if (otp.isBlank()) {
            _otpError.value = "Enter the buyer's delivery code."
            return
        }
        val job = _job ?: return
        viewModelScope.launch {
            _isVerifyingOtp.value = true
            _otpError.value = null
            val jobId = job.id.toLongOrNull()
            if (jobId == null) {
                _otpError.value = "This job has an invalid ID."
                _isVerifyingOtp.value = false
                return@launch
            }
            val preparedJob = jobsRepository.prepareJobForOtpVerification(jobId)
            if (preparedJob.isFailure) {
                _otpError.value = preparedJob.exceptionOrNull()?.message
                    ?: "Could not prepare the job for OTP verification."
                _isVerifyingOtp.value = false
                return@launch
            }
            _job = job.copy(backendStatus = preparedJob.getOrThrow().status)
            val result = jobsRepository.verifyOtp(jobId, otp.trim())
            _isVerifyingOtp.value = false
            result.fold(
                onSuccess = { updatedJob ->
                    tripRepository.restoreSavedStatus(
                        updatedJob.status,
                        updatedJob.tripId ?: tripRepository.activeTripId.value
                    )
                    otpVerifiedJobId = job.id
                    _otpVerified.value = true
                },
                onFailure = { error ->
                    _otpError.value = error.message ?: "Could not verify the delivery code."
                }
            )
        }
    }

    /** Uploads delivery evidence after the backend has accepted the buyer's OTP. */
    fun fileDeliveryProof(
        recipientName: String,
        photoUri: Uri?,
        signatureUrl: String
    ) {
        if (_isSubmittingProof.value) return
        val jobId = _job?.id?.toLongOrNull()?.takeIf { it > 0L }
        if (jobId == null) {
            _proofError.value = "This job has an invalid ID."
            return
        }
        if (!_otpVerified.value) {
            _proofError.value = "Verify the buyer's delivery code before submitting evidence."
            return
        }
        if (recipientName.isBlank() && photoUri == null && signatureUrl.isBlank()) {
            _proofError.value = "Add recipient name, photo, or signature as delivery evidence."
            return
        }

        viewModelScope.launch {
            _isSubmittingProof.value = true
            _proofError.value = null
            val job = _job
            if (job == null) {
                _isSubmittingProof.value = false
                _proofError.value = "Could not find the active delivery."
                return@launch
            }
            val tripId = resolveTripId(job)
            if (tripId == null) {
                _proofError.value = tripRepository.lastError.value
                    ?: "Could not find the active delivery."
                _isSubmittingProof.value = false
                return@launch
            }
            val photoUrl = if (photoUri != null) {
                val upload = tripRepository.uploadProofPhoto(
                    tripId = tripId,
                    contentResolver = getApplication<Application>().contentResolver,
                    uri = photoUri,
                    cacheDir = getApplication<Application>().cacheDir
                )
                upload.getOrElse { error ->
                    _proofError.value = error.message
                        ?: "Could not upload the delivery photo. Please retry."
                    _isSubmittingProof.value = false
                    return@launch
                }
            } else {
                null
            }
            val proof = ProofOfDelivery(
                logisticsJobId = jobId,
                recipientName = recipientName.trim().takeIf { it.isNotEmpty() },
                photoUrl = photoUrl,
                signatureUrl = signatureUrl.trim().takeIf { it.isNotEmpty() }
            )
            val result = tripRepository.fileProof(tripId, proof)
            _isSubmittingProof.value = false
            result.fold(
                onSuccess = {
                    tripRepository.stopTracking("COMPLETED")
                    _proofFiled.value = true
                },
                onFailure = { error ->
                    _proofError.value = error.message ?: "Could not submit delivery proof."
                }
            )
        }
    }

    /** Cancels the trip. */
    fun cancelTrip() {
        viewModelScope.launch {
            val id = _job?.id?.toLongOrNull() ?: return@launch
            val cancelled = jobsRepository.cancelJob(id, "Driver cancelled the delivery")
            if (cancelled.isSuccess) {
                tripRepository.stopTracking("CANCELLED")
            } else {
                _proofError.value = cancelled.exceptionOrNull()?.message ?: "Could not cancel delivery."
            }
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
        // Proof submission or cancellation ends the local trip state explicitly.
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
