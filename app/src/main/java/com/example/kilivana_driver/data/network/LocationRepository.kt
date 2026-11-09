package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.LocationUpdate
import com.example.kilivana_driver.data.model.LocationUploadResponse
import com.example.kilivana_driver.data.model.TripLocationRequest
import com.example.kilivana_driver.data.model.TrackingConfig
import com.example.kilivana_driver.data.model.UploadResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.ArrayDeque

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
    private val queueLock = Any()
    private val retryQueue = ArrayDeque<LocationUpdate>()
    private val repositoryScope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + ioDispatcher)
    private var flushJob: Job? = null
    private var queueGeneration = 0L
    private var flushRequested = false

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
        synchronized(queueLock) {
            if (!offlineQueue.trySend(update).isSuccess) {
                // Preserve pending retries; drop the oldest queued update only.
                if (offlineQueue.tryReceive().isSuccess) {
                    queueSizeCounter.decrementAndGet()
                } else if (retryQueue.isNotEmpty()) {
                    _queueSize.value = queueSizeCounter.get()
                    return
                }
                if (offlineQueue.trySend(update).isSuccess) {
                    queueSizeCounter.incrementAndGet()
                }
            } else {
                queueSizeCounter.incrementAndGet()
            }
            _queueSize.value = queueSizeCounter.get()
        }
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
            if (update.tripId.isBlank()) return@withContext UploadResult.Failure(
                Exception("Missing trip ID"),
                false
            )
            if (!update.hasValidCoordinates()) return@withContext UploadResult.Failure(
                Exception("GPS coordinates are outside valid latitude/longitude ranges"),
                false
            )
            val request = update.toTripRequest()
            try {
                val response = api.reportLocation(update.tripId, request)
                if (response.success != false && response.data != null) {
                    UploadResult.Success(
                        LocationUploadResponse(
                            success = true,
                            message = null,
                            serverTimestamp = response.data!!.serverTimestamp?.toLongOrNull()
                        )
                    )
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Network error, timeout, etc. — retryable.
                UploadResult.Failure(e, true)
            }
        }

    /** Starts a single asynchronous drain, or records a pending drain request. */
    private fun scheduleFlush() {
        synchronized(queueLock) {
            if (flushJob != null || isFlushing.getAndSet(true)) {
                flushRequested = true
                return
            }
            val generation = queueGeneration
            flushRequested = false
            flushJob = repositoryScope.launch {
                try {
                    flushQueue(generation)
                } finally {
                    synchronized(queueLock) {
                        isFlushing.set(false)
                        flushJob = null
                        if ((queueGeneration != generation || flushRequested) &&
                            queueSizeCounter.get() > 0
                        ) {
                            scheduleFlush()
                        }
                    }
                }
            }
        }
    }

    /**
     * Uploads buffered updates in FIFO order, retaining transient failures without suspending.
     * Stops on a non-retryable failure or after retaining one transient failure for a later retry.
     */
    private suspend fun flushQueue(generation: Long) {
        while (true) {
            val update = synchronized(queueLock) {
                if (queueGeneration != generation) return
                if (retryQueue.isNotEmpty()) retryQueue.removeFirst()
                else offlineQueue.tryReceive().getOrNull()
            } ?: return
            val uploadResult = try {
                uploadNow(update)
            } catch (e: CancellationException) {
                synchronized(queueLock) {
                    queueSizeCounter.decrementAndGet()
                    _queueSize.value = queueSizeCounter.get().coerceAtLeast(0)
                }
                throw e
            }
            when (uploadResult) {
                is UploadResult.Success -> {
                    synchronized(queueLock) {
                        queueSizeCounter.decrementAndGet()
                        _queueSize.value = queueSizeCounter.get().coerceAtLeast(0)
                    }
                    continue // Next update
                }
                is UploadResult.Failure -> {
                    synchronized(queueLock) {
                        if (queueGeneration != generation) {
                            queueSizeCounter.decrementAndGet()
                        } else if (uploadResult.willRetry) {
                            retryQueue.addFirst(update)
                        } else {
                            queueSizeCounter.decrementAndGet()
                        }
                        _queueSize.value = queueSizeCounter.get().coerceAtLeast(0)
                    }
                    return // Stop flushing; will retry later
                }
            }
        }
    }

    /** Retries buffered updates when connectivity returns. */
    fun retryPendingUploads() {
        scheduleFlush()
    }

    /** Clears the offline queue (e.g., when tracking ends or session expires). */
    fun clearQueue() {
        synchronized(queueLock) {
            queueGeneration++
            while (offlineQueue.tryReceive().isSuccess) {
                queueSizeCounter.decrementAndGet()
            }
            while (retryQueue.isNotEmpty()) {
                retryQueue.removeFirst()
                queueSizeCounter.decrementAndGet()
            }
            _queueSize.value = queueSizeCounter.get().coerceAtLeast(0)
            flushJob?.cancel()
        }
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

/**
 * Converts the internal [LocationUpdate] model into the request body expected by
 * the backend's tracking endpoints (POST /api/v1/tracking/{jobId}/location).
 */
private fun LocationUpdate.toTripRequest(): TripLocationRequest =
    TripLocationRequest(
        latitude = latitude,
        longitude = longitude,
        speedKmh = speed?.let { it.toDouble() * 3.6 },
        bearing = bearing?.toDouble(),
        accuracyMetres = accuracy?.toDouble(),
        clientTimestamp = java.time.Instant.ofEpochMilli(timestamp).toString()
    )