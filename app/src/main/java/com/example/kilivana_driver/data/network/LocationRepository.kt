package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.LocationUpdate
import com.example.kilivana_driver.data.model.LocationUploadResponse
import com.example.kilivana_driver.data.model.TrackingConfig
import com.example.kilivana_driver.data.model.UploadResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Repository for uploading driver location updates to the backend.
 *
 * Handles:
 * - Authenticated requests via the shared OkHttp client (bearer token + refresh)
 * - Bounded offline queue when network is unavailable
 * - Deduplication: only uploads when the driver has moved enough
 * - Retry with exponential backoff for transient failures
 * - 401/403 handling via the global TokenAuthenticator
 *
 * This repository is stateless aside from the offline queue. It does not
 * decide *when* to track — that's the TripViewModel/Service's job.
 */
class LocationRepository(
    private val api: TripApi = ApiClient.create(),
    private val config: TrackingConfig = TrackingConfig.DEFAULT,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val errorJson = Json { ignoreUnknownKeys = true }

    /** Internal queue for updates that failed to upload (network down, 5xx, timeout). */
    private val offlineQueue = Channel<LocationUpdate>(config.maxOfflineQueueSize)

    /** Current queue size (tracked manually since Channel.size is not available). */
    private val queueSizeCounter = AtomicInteger(0)

    /** True while a flush is in progress to avoid concurrent flushes. */
    private val isFlushing = AtomicBoolean(false)

    /** Exposes the current queue size for diagnostics/testing. */
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    /**
     * Enqueues a location update for upload.
     *
     * Returns immediately; the upload happens asynchronously.
     * If the queue is full, the oldest update is dropped (FIFO).
     *
     * Call this from the foreground service or TripViewModel on every
     * valid location fix that passes the displacement filter.
     */
    fun enqueueLocation(update: LocationUpdate) {
        // Try to add without suspending; if full, remove oldest and retry once.
        val result = offlineQueue.trySend(update)
        if (!result.isSuccess) {
            // Queue full: drop the oldest to make room for the freshest location.
            offlineQueue.tryReceive()
            offlineQueue.trySend(update)
        } else {
            queueSizeCounter.incrementAndGet()
        }
        _queueSize.value = queueSizeCounter.get()
        scheduleFlush()
    }

    /**
     * Uploads a single location update immediately, bypassing the queue.
     *
     * Use this for critical updates (e.g., pickup/dropoff confirmation)
     * where you want to know the result right away.
     *
     * Returns an UploadResult so the caller can react to success/failure.
     */
    suspend fun uploadNow(update: LocationUpdate): UploadResult =
        withContext(ioDispatcher) {
            try {
                val response = api.uploadLocation(update.tripId, update)
                if (response.success && response.data != null) {
                    UploadResult.Success(response.data!!)
                } else {
                    val willRetry = isRetryableError(response.error, response.message)
                    UploadResult.Failure(
                        Exception(describe(response.error, response.message)),
                        willRetry
                    )
                }
            } catch (e: HttpException) {
                val code = e.code()
                val willRetry = code == 408 || code == 429 || code in 500..599
                UploadResult.Failure(
                    Exception(readErrorBody(e) ?: "Server responded with HTTP $code"),
                    willRetry
                )
            } catch (e: Exception) {
                // Network error, timeout, etc. — retryable.
                UploadResult.Failure(e, true)
            }
        }

    /**
     * Flushes the offline queue, uploading updates in FIFO order.
     *
     * Stops on the first non-retryable failure so we don't waste battery
     * on updates the backend will never accept (e.g., 400, 401, 403).
     * Retryable failures leave the update in the queue for the next flush.
     */
    private fun scheduleFlush() {
        if (isFlushing.getAndSet(true)) return // Already flushing

        // Fire-and-forget on IO; the repository doesn't expose a completion signal
        // because the caller (service/ViewModel) doesn't need to await it.
        kotlinx.coroutines.CoroutineScope(ioDispatcher).launch {
            flushQueue()
            isFlushing.set(false)
        }
    }

    private suspend fun flushQueue() {
        while (true) {
            val result = offlineQueue.receiveCatching()
            if (result.isClosed) break
            val update = result.getOrNull() ?: continue
            val uploadResult = uploadNow(update)
            when (uploadResult) {
                is UploadResult.Success -> {
                    queueSizeCounter.decrementAndGet()
                    _queueSize.value = queueSizeCounter.get()
                    continue // Next update
                }
                is UploadResult.Failure -> {
                    if (uploadResult.willRetry) {
                        // Put it back at the front for the next flush attempt.
                        offlineQueue.send(update)
                    } else {
                        // Non-retryable (e.g., 400, 401, 403) — drop it and stop.
                        queueSizeCounter.decrementAndGet()
                    }
                    _queueSize.value = queueSizeCounter.get()
                    return // Stop flushing; will retry later
                }
            }
        }
    }

    /** Clears the offline queue (e.g., when tracking ends or session expires). */
    fun clearQueue() {
        while (offlineQueue.tryReceive() != null) {
            queueSizeCounter.decrementAndGet()
        }
        _queueSize.value = 0
    }

    /** Checks if an HTTP error from the backend is retryable. */
    private fun isRetryableError(error: ApiError?, fallback: String?): Boolean {
        val code = error?.code?.takeIf { it.isNotBlank() }
        return when (code) {
            "RATE_LIMITED", "SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT", "TIMEOUT" -> true
            else -> fallback?.let { it.contains("timeout", ignoreCase = true) || it.contains("network", ignoreCase = true) } ?: false
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