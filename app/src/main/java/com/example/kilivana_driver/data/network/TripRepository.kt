package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.ProofOfDelivery
import com.example.kilivana_driver.data.model.TripStage
import com.example.kilivana_driver.data.model.TripStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Coordinates the backend-owned trip lifecycle. The trip ID comes from the
 * trip-accept response and is never substituted with the delivery-job ID.
 */
class TripRepository(
    private val api: TripApi = ApiClient.create(),
    private val locationRepository: LocationRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val errorJson = Json { ignoreUnknownKeys = true }

    val tripStage: StateFlow<TripStage?> = _tripStage.asStateFlow()
    val activeTripId: StateFlow<String?> = _activeTripId.asStateFlow()
    val trackingActive: StateFlow<Boolean> = _trackingActive.asStateFlow()
    val backendStatus: StateFlow<TripStatus?> = _backendStatus.asStateFlow()
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    suspend fun acceptJob(jobId: Long): Result<TripStatus> = withContext(ioDispatcher) {
        val result = request { api.acceptJob(jobId) }
        val trip = result.getOrNull() ?: return@withContext result
        if (trip.tripId.isBlank()) {
            val error = IllegalStateException("Backend accepted the job but returned no trip ID")
            _lastError.value = error.message
            return@withContext Result.failure(error)
        }
        _activeTripId.value = trip.tripId
        _trackingActive.value = false
        _backendStatus.value = trip
        _tripStage.value = tripStageForStatus(trip.status)
        locationRepository.clearQueue()
        result
    }

    /** Starts reporting only after the backend confirms tracking for this trip ID. */
    suspend fun startTracking(tripId: String): Boolean = withContext(ioDispatcher) {
        _isBusy.value = true
        _lastError.value = null
        try {
            val response = api.startTracking(tripId)
            if (response.success == false) {
                _lastError.value = describe(response, "Unable to start trip tracking")
                return@withContext false
            }
            _activeTripId.value = tripId
            _trackingActive.value = true
            if (_tripStage.value == null) _tripStage.value = TripStage.HEADING_TO_PICKUP
            locationRepository.clearQueue()
            true
        } catch (e: Exception) {
            _lastError.value = describeException(e)
            false
        } finally {
            _isBusy.value = false
        }
    }

    /** Records pickup on the backend before the UI changes to the buyer leg. */
    suspend fun confirmPickup(tripId: String): Result<TripStatus> = withContext(ioDispatcher) {
        val result = request { api.updateStatus(tripId, "picked_up") }
        val status = result.getOrNull() ?: return@withContext result
        if (tripStageForStatus(status.status) != TripStage.IN_TRANSIT) {
            val error = IllegalStateException(
                "Backend accepted the pickup request but returned status '${status.status}'"
            )
            _lastError.value = error.message
            return@withContext Result.failure(error)
        }
        _backendStatus.value = status
        _tripStage.value = TripStage.IN_TRANSIT
        result
    }

    suspend fun fileProof(
        tripId: String,
        proof: ProofOfDelivery,
        otp: String
    ): Result<ProofOfDelivery> = withContext(ioDispatcher) {
        val proofResult = request { api.fileProof(tripId, otp, proof) }
        if (proofResult.isFailure) return@withContext proofResult

        val tripResult = request { api.getTrip(tripId) }
        val trip = tripResult.getOrNull() ?: return@withContext Result.failure(
            tripResult.exceptionOrNull() ?: IllegalStateException("Unable to verify delivery completion")
        )
        if (!isCompletedStatus(trip.status)) {
            return@withContext Result.failure(
                IllegalStateException("Proof was received, but the backend has not confirmed delivery completion")
            )
        }
        _backendStatus.value = trip
        Result.success(proofResult.getOrThrow())
    }

    /** Verifies the buyer's delivery handover code for this trip. */
    suspend fun verifyOtp(tripId: String, otp: String): Result<TripStatus> = withContext(ioDispatcher) {
        request { api.verifyOtp(tripId, otp) }
    }

    /**
     * Clears local tracking only after the caller has received server
     * confirmation for completion or cancellation. There is no separate stop
     * endpoint in the supplied backend contract.
     */
    fun stopTracking(reason: String) {
        _tripStage.value = when (reason.uppercase()) {
            "COMPLETED" -> TripStage.DELIVERED
            else -> TripStage.CANCELLED
        }
        _activeTripId.value = null
        _trackingActive.value = false
        _backendStatus.value = null
        locationRepository.clearQueue()
    }

    suspend fun syncStatus(): TripStatus? = checkForActiveTrip()

    /** Restores the latest active trip identity/status after reconnect. */
    suspend fun checkForActiveTrip(): TripStatus? = withContext(ioDispatcher) {
        _lastError.value = null
        try {
            val response = api.getMyTrips()
            if (response.success == false) {
                _lastError.value = describe(response, "Unable to restore active trip")
                return@withContext null
            }
            val active = response.data.orEmpty()
                .lastOrNull { isActiveTripStatus(it.status) }
            if (active == null) {
                _backendStatus.value = null
                _activeTripId.value = null
                _trackingActive.value = false
                _tripStage.value = null
                return@withContext null
            }
            if (active.tripId.isBlank()) {
                _lastError.value = "Backend returned an active trip without a trip ID"
                return@withContext null
            }
            _backendStatus.value = active
            _activeTripId.value = active.tripId
            _tripStage.value = tripStageForStatus(active.status)
            if (!startTracking(active.tripId)) return@withContext null
            active
        } catch (e: Exception) {
            _lastError.value = describeException(e)
            null
        }
    }

    fun clear() {
        _activeTripId.value = null
        _trackingActive.value = false
        _tripStage.value = null
        _backendStatus.value = null
        _lastError.value = null
        locationRepository.clearQueue()
    }

    private suspend fun <T> request(block: suspend () -> ApiResponse<T>): Result<T> {
        _isBusy.value = true
        _lastError.value = null
        return try {
            val response = block()
            val data = response.data
            if (response.success != false && data != null) {
                Result.success(data)
            } else {
                val message = describe(response, "The backend returned no trip data")
                _lastError.value = message
                Result.failure(IllegalStateException(message))
            }
        } catch (e: Exception) {
            val message = describeException(e)
            _lastError.value = message
            Result.failure(IllegalStateException(message, e))
        } finally {
            _isBusy.value = false
        }
    }

    private fun describe(response: ApiResponse<*>, fallback: String): String {
        val code = response.error?.code?.takeIf { it.isNotBlank() }
        val details = response.error?.details?.takeIf { it.isNotBlank() }
        return listOfNotNull(code, details).joinToString(": ").ifBlank {
            response.message.takeIf { it.isNotBlank() } ?: fallback
        }
    }

    private fun describeException(error: Exception): String =
        if (error is HttpException) {
            readErrorBody(error) ?: "Server responded with HTTP ${error.code()}"
        } else {
            error.message ?: "Couldn't reach the backend"
        }

    private fun readErrorBody(error: HttpException): String? {
        val raw = runCatching { error.response()?.errorBody()?.string() }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val response = runCatching {
            errorJson.decodeFromString<ApiResponse<Unit>>(raw)
        }.getOrNull() ?: return null
        return describe(response, "").takeIf { it.isNotBlank() }
    }

    companion object {
        private val _tripStage = MutableStateFlow<TripStage?>(null)
        private val _activeTripId = MutableStateFlow<String?>(null)
        private val _trackingActive = MutableStateFlow(false)
        private val _backendStatus = MutableStateFlow<TripStatus?>(null)
        private val _isBusy = MutableStateFlow(false)
        private val _lastError = MutableStateFlow<String?>(null)

        fun tripStageForStatus(status: String): TripStage? = when (status.lowercase()) {
            "assigned", "accepted", "heading_to_pickup", "en_route_to_pickup" ->
                TripStage.HEADING_TO_PICKUP
            "picked_up", "in_transit", "in_progress", "en_route_to_buyer",
            "en_route_to_dropoff" -> TripStage.IN_TRANSIT
            "delivered", "completed" -> TripStage.DELIVERED
            "cancelled", "canceled", "rejected" -> TripStage.CANCELLED
            else -> null
        }

        private fun isActiveTripStatus(status: String): Boolean =
            tripStageForStatus(status) in setOf(TripStage.HEADING_TO_PICKUP, TripStage.IN_TRANSIT)

        private fun isCompletedStatus(status: String): Boolean =
            status.equals("delivered", ignoreCase = true) ||
                status.equals("completed", ignoreCase = true)
    }
}
