package com.example.kilivana_driver.data.network

import android.content.ContentResolver
import android.net.Uri
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DeliveryJob
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.net.URI

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
        val status = result.getOrNull()?.let { response ->
            response.copy(tripId = response.tripId.ifBlank { tripId })
        } ?: return@withContext result
        if (tripStageForStatus(status.status) != TripStage.IN_TRANSIT) {
            val error = IllegalStateException(
                "Backend accepted the pickup request but returned status '${status.status}'"
            )
            _lastError.value = error.message
            return@withContext Result.failure(error)
        }
        _backendStatus.value = status
        _tripStage.value = TripStage.IN_TRANSIT
        Result.success(status)
    }

    suspend fun fileProof(
        tripId: String,
        proof: ProofOfDelivery,
        otp: String? = null
    ): Result<ProofOfDelivery> = withContext(ioDispatcher) {
        val photoUrl = proof.photoUrl
        if (photoUrl != null && !isBackendPhotoUrl(photoUrl)) {
            return@withContext Result.failure(
                IllegalArgumentException("Upload the delivery photo before submitting proof.")
            )
        }
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

    /** Uploads a selected photo and returns its durable backend URL. */
    suspend fun uploadProofPhoto(
        tripId: String,
        contentResolver: ContentResolver,
        uri: Uri,
        cacheDir: File
    ): Result<String> = withContext(ioDispatcher) {
        var tempFile: File? = null
        try {
            val mimeType = contentResolver.getType(uri)
                ?.substringBefore(';')
                ?.lowercase()
                ?: if (uri.scheme == "file") "image/jpeg" else null
                ?: throw IllegalArgumentException("Could not determine the selected photo type.")
            val extension = when (mimeType) {
                "image/jpeg", "image/jpg" -> ".jpg"
                "image/png" -> ".png"
                "image/gif" -> ".gif"
                "image/webp" -> ".webp"
                else -> throw IllegalArgumentException(
                    "Choose a JPEG, PNG, GIF, or WebP delivery photo."
                )
            }

            val file = File.createTempFile("delivery-proof-", extension, cacheDir)
            tempFile = file
            val input = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not read the selected delivery photo.")
            input.use { source ->
                file.outputStream().use { destination ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val bytesRead = source.read(buffer)
                        if (bytesRead < 0) break
                        totalBytes += bytesRead
                        require(totalBytes <= MAX_PROOF_PHOTO_BYTES) {
                            "Delivery photos must be 10 MB or smaller."
                        }
                        destination.write(buffer, 0, bytesRead)
                    }
                }
            }

            val imagePart = MultipartBody.Part.createFormData(
                name = "image",
                filename = file.name,
                body = file.asRequestBody(mimeType.toMediaTypeOrNull())
            )
            val response = api.uploadProofPhoto(tripId, imagePart)
            if (response.success == false) {
                throw IllegalStateException(describe(response, "Could not upload the delivery photo."))
            }
            val url = response.data?.url?.trim()
                ?.takeIf(::isBackendPhotoUrl)
                ?: throw IllegalStateException(
                    "The server did not return a valid delivery photo URL. Please retry."
                )
            Result.success(url)
        } catch (e: HttpException) {
            val message = readErrorBody(e) ?: "Photo upload failed (HTTP ${e.code()}). Please retry."
            Result.failure(IllegalStateException(message, e))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempFile?.delete()
        }
    }

    /** Verifies the buyer's delivery handover code for this trip. */
    suspend fun verifyOtp(tripId: String, otp: String): Result<TripStatus> = withContext(ioDispatcher) {
        val result = request { api.verifyOtp(tripId, otp) }
        val status = result.getOrNull()?.let { response ->
            response.copy(tripId = response.tripId.ifBlank { tripId })
        } ?: return@withContext result
        _backendStatus.value = status
        tripStageForStatus(status.status)?.let { _tripStage.value = it }
        Result.success(status)
    }

    /** Restores the stage received with the persisted job before opening its route screen. */
    fun restoreSavedStatus(status: String?, tripId: String?) {
        val stage = status?.let(::tripStageForStatus) ?: return
        _tripStage.value = stage
        tripId?.takeIf { it.isNotBlank() }?.let { _activeTripId.value = it }
    }

    /** Resolves a missing trip ID from the backend's current-trip endpoint, scoped to this job. */
    suspend fun resolveTripIdForJob(jobId: String): String? = withContext(ioDispatcher) {
        val numericJobId = jobId.toLongOrNull()
        if (numericJobId == null) {
            _lastError.value = "This job has an invalid ID."
            return@withContext null
        }

        val result = request { api.getCurrentTrip() }
        val currentTrip: DeliveryJob = result.getOrNull() ?: return@withContext null
        if (currentTrip.id != numericJobId) {
            _lastError.value = "The backend's current trip does not match this job."
            return@withContext null
        }

        val tripId = currentTrip.tripId?.takeIf { it.isNotBlank() }
        if (tripId == null) {
            _lastError.value = "The backend's current trip has no trip ID. Contact dispatch to link this job to a trip."
            return@withContext null
        }

        _activeTripId.value = tripId
        currentTrip.status?.let { status ->
            tripStageForStatus(status)?.let { _tripStage.value = it }
        }
        tripId
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
        private const val MAX_PROOF_PHOTO_BYTES = 10L * 1024L * 1024L

        private fun isBackendPhotoUrl(value: String): Boolean {
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            return (uri.scheme.equals("https", ignoreCase = true) ||
                uri.scheme.equals("http", ignoreCase = true)) && !uri.host.isNullOrBlank()
        }

        private val _tripStage = MutableStateFlow<TripStage?>(null)
        private val _activeTripId = MutableStateFlow<String?>(null)
        private val _trackingActive = MutableStateFlow(false)
        private val _backendStatus = MutableStateFlow<TripStatus?>(null)
        private val _isBusy = MutableStateFlow(false)
        private val _lastError = MutableStateFlow<String?>(null)

        fun tripStageForStatus(status: String): TripStage? = when (status.lowercase()) {
            "assigned", "accepted", "heading_to_pickup", "en_route_to_pickup" ->
                TripStage.HEADING_TO_PICKUP
            "picked_up", "pickup_confirmed", "shipment", "in_shipment", "shipped",
            "in_transit", "in-transit", "transit", "in_progress", "en_route_to_buyer",
            "en_route_to_dropoff" -> TripStage.IN_TRANSIT
            "otp_verified", "delivered", "completed" -> TripStage.DELIVERED
            "cancelled", "canceled", "rejected" -> TripStage.CANCELLED
            else -> null
        }

        private fun isActiveTripStatus(status: String): Boolean =
            tripStageForStatus(status) in setOf(TripStage.HEADING_TO_PICKUP, TripStage.IN_TRANSIT)

        private fun isCompletedStatus(status: String): Boolean =
            tripStageForStatus(status) == TripStage.DELIVERED
    }
}
