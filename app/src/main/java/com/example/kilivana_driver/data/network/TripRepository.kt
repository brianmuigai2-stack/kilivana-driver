package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.model.JobStatus
import com.example.kilivana_driver.data.model.LocationUpdate
import com.example.kilivana_driver.data.model.StartTrackingRequest
import com.example.kilivana_driver.data.model.StopTrackingRequest
import com.example.kilivana_driver.data.model.TrackingConfig
import com.example.kilivana_driver.data.model.TripStage
import com.example.kilivana_driver.data.model.TripStatus
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Repository for managing the active trip/delivery lifecycle.
 *
 * Coordinates:
 * - Starting/stopping tracking on the backend
 * - Syncing trip status (polling or initial fetch)
 * - Converting between backend TripStatus and local TripStage
 *
 * This repository does NOT handle GPS location streaming — that's
 * LocationRepository's job. TripRepository owns the *trip* state machine.
 */
class TripRepository(
    private val api: TripApi = ApiClient.create(),
    private val locationRepository: LocationRepository,
    private val config: TrackingConfig = TrackingConfig.DEFAULT,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val errorJson = Json { ignoreUnknownKeys = true }

    /** Current trip stage derived from backend status + local actions. */
    private val _tripStage = MutableStateFlow<TripStage?>(null)
    val tripStage: StateFlow<TripStage?> = _tripStage.asStateFlow()

    /** The currently active trip ID, or null if no trip is being tracked. */
    private val _activeTripId = MutableStateFlow<String?>(null)
    val activeTripId: StateFlow<String?> = _activeTripId.asStateFlow()

    /** Latest trip status from the backend (for sync/reconnect). */
    private val _backendStatus = MutableStateFlow<TripStatus?>(null)
    val backendStatus: StateFlow<TripStatus?> = _backendStatus.asStateFlow()

    /** True while a start/stop/sync operation is in progress. */
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    /** Last error from a trip operation (cleared on next operation). */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Starts tracking for a trip that the driver has accepted.
     *
     * Called when the driver taps "Start Trip" (heading to pickup) or
     * "Mark as Picked Up" (entering in-transit). The backend is notified
     * so it can start accepting location updates for this trip.
     *
     * @param job The accepted job (must have status ACCEPTED).
     * @return true if tracking started successfully.
     */
    suspend fun startTracking(job: Job): Boolean = withContext(ioDispatcher) {
        _isBusy.value = true
        _lastError.value = null
        val userId = SessionStore.userId ?: run {
            _lastError.value = "Not signed in"
            _isBusy.value = false
            return@withContext false
        }

        val request = StartTrackingRequest(tripId = job.id, driverId = userId)
        try {
            val response = api.startTracking(job.id, request)
            if (response.success != false) {
                _activeTripId.value = job.id
                _tripStage.value = TripStage.HEADING_TO_PICKUP
                // Clear any stale offline queue from a previous trip.
                locationRepository.clearQueue()
                _isBusy.value = false
                return@withContext true
            } else {
                _lastError.value = describe(response.error, response.message)
                _isBusy.value = false
                return@withContext false
            }
        } catch (e: HttpException) {
            _lastError.value = readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"
            _isBusy.value = false
            return@withContext false
        } catch (e: Exception) {
            _lastError.value = e.message ?: "Couldn't reach the backend"
            _isBusy.value = false
            return@withContext false
        }
    }

    /**
     * Advances the trip to the IN_TRANSIT stage (cargo picked up).
     *
     * This is a local state transition; the backend is informed via
     * the regular location updates (which now carry the new stage context).
     * If the backend needs an explicit "picked up" call, add an endpoint
     * here and in TripApi.
     */
    fun advanceToInTransit() {
        if (_tripStage.value == null || _tripStage.value == TripStage.HEADING_TO_PICKUP) {
            _tripStage.value = TripStage.IN_TRANSIT
        }
    }

    /**
     * Stops tracking for the current trip.
     *
     * Called when delivery is confirmed (OTP verified), cancelled,
     * or the driver explicitly ends the trip.
     *
     * @param reason One of "COMPLETED", "CANCELLED", "DRIVER_ENDED".
     * @return true if the backend acknowledged the stop.
     */
    suspend fun stopTracking(reason: String): Boolean = withContext(ioDispatcher) {
        val tripId = _activeTripId.value ?: run {
            _isBusy.value = false
            return@withContext true // Nothing to stop
        }
        val userId = SessionStore.userId ?: run {
            _lastError.value = "Not signed in"
            _isBusy.value = false
            return@withContext false
        }

        _isBusy.value = true
        _lastError.value = null

        val request = StopTrackingRequest(tripId = tripId, driverId = userId, reason = reason)
        try {
            val response = api.stopTracking(tripId, request)
            val success = response.success != false
            if (success) {
                _tripStage.value = when (reason) {
                    "COMPLETED" -> TripStage.DELIVERED
                    "CANCELLED" -> TripStage.CANCELLED
                    else -> TripStage.CANCELLED
                }
                _activeTripId.value = null
                locationRepository.clearQueue()
            } else {
                _lastError.value = describe(response.error, response.message)
            }
            _isBusy.value = false
            return@withContext success
        } catch (e: HttpException) {
            _lastError.value = readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"
            _isBusy.value = false
            return@withContext false
        } catch (e: Exception) {
            _lastError.value = e.message ?: "Couldn't reach the backend"
            _isBusy.value = false
            return@withContext false
        }
    }

    /**
     * Syncs the current trip status from the backend.
     *
     * Use this on app resume, after reconnecting, or when a WebSocket
     * push indicates a status change.
     */
    suspend fun syncStatus(): TripStatus? = withContext(ioDispatcher) {
        val tripId = _activeTripId.value ?: return@withContext null
        try {
            val response = api.getTripStatus(tripId)
            if (response.success != false && response.data != null) {
                _backendStatus.value = response.data
                // Update local stage from backend status if it differs.
                updateStageFromBackend(response.data!!)
                return@withContext response.data
            } else {
                _lastError.value = describe(response.error, response.message)
                return@withContext null
            }
        } catch (e: HttpException) {
            // 404 might mean the trip was completed/cancelled on the backend.
            if (e.code() == 404) {
                _activeTripId.value = null
                _tripStage.value = null
                _backendStatus.value = null
                locationRepository.clearQueue()
            } else {
                _lastError.value = readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"
            }
            return@withContext null
        } catch (e: Exception) {
            _lastError.value = e.message ?: "Couldn't reach the backend"
            return@withContext null
        }
    }

    /**
     * Checks if there's an active trip for the current driver on the backend.
     *
     * Called on app startup to recover a tracking session that was
     * interrupted (process death, crash, etc.).
     */
    suspend fun checkForActiveTrip(): TripStatus? = withContext(ioDispatcher) {
        val userId = SessionStore.userId ?: return@withContext null
        try {
            val response = api.getActiveTrips()
            if (response.success != false && response.data != null && response.data!!.isNotEmpty()) {
                // Backend should return at most one active trip per driver.
                val status = response.data!!.first()
                _activeTripId.value = status.tripId
                _backendStatus.value = status
                updateStageFromBackend(status)
                return@withContext status
            }
            return@withContext null
        } catch (e: HttpException) {
            _lastError.value = readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"
            return@withContext null
        } catch (e: Exception) {
            _lastError.value = e.message ?: "Couldn't reach the backend"
            return@withContext null
        }
    }

    /** Clears all trip state (logout, session expiry, etc.). */
    fun clear() {
        _activeTripId.value = null
        _tripStage.value = null
        _backendStatus.value = null
        _lastError.value = null
        locationRepository.clearQueue()
    }

    /** Maps backend status string to local TripStage. */
    private fun updateStageFromBackend(status: TripStatus) {
        _tripStage.value = when (status.status.uppercase()) {
            "HEADING_TO_PICKUP", "EN_ROUTE_TO_PICKUP", "ASSIGNED", "ACCEPTED" -> TripStage.HEADING_TO_PICKUP
            "IN_TRANSIT", "PICKED_UP", "EN_ROUTE_TO_BUYER", "EN_ROUTE_TO_DROPOFF" -> TripStage.IN_TRANSIT
            "DELIVERED", "COMPLETED" -> TripStage.DELIVERED
            "CANCELLED", "REJECTED" -> TripStage.CANCELLED
            else -> TripStage.HEADING_TO_PICKUP // Default to safe state
        }
    }

    private fun readErrorBody(e: HttpException): String? {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        val envelope = runCatching { errorJson.decodeFromString<ApiResponse<Unit>>(raw) }.getOrNull()
            ?: return null
        return describe(envelope.error, envelope.message).takeIf { it.isNotBlank() }
    }

    private fun describe(error: ApiError?, fallback: String): String {
        val code = error?.code?.takeIf { it.isNotBlank() }
        val details = error?.details?.takeIf { it.isNotBlank() }
        return listOfNotNull(code, details).joinToString(": ").ifBlank { fallback }
    }
}
