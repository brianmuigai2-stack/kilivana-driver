package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DeliveryJob
import com.example.kilivana_driver.data.model.JobsErrorType
import com.example.kilivana_driver.data.model.TrackingHistoryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * Repository for logistics delivery jobs.
 *
 * Wraps [JobsApi] with the same error-handling contract as the other
 * repositories: suspend + [Result], always on [Dispatchers.IO], never throws,
 * and surfaces the backend's `error.code` / `error.details` rather than a bare
 * HTTP status.
 *
 * [getJob] and [getJobs] return the backend-facing [DeliveryJob] model. Call
 * [DeliveryJob.toDomain] on the result to map to the UI [com.example.kilivana_driver.data.model.Job].
 */
class JobsRepository(
    private val api: JobsApi = ApiClient.create(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val errorJson = Json { ignoreUnknownKeys = true }

    /** Safety-net timeout: the OkHttp client has 15s per-host timeouts, but the
     * HostFailoverInterceptor retries across multiple hosts, so the total can
     * stretch to 45s+. This caps every call so the UI never hangs forever. */
    private val API_TIMEOUT_MS = 30_000L

    /**
     * Wraps a backend failure with a [JobsErrorType] classification so the UI
     * can show a context-specific message ("No internet connection" vs
     * "Server is not running" vs a generic error).
     */
    class JobsException(
        val errorType: JobsErrorType,
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)

    /**
     * Lists delivery jobs for the current driver (pageable).
     *
     * Pass `page`/`size`/`sort` to control pagination; the defaults return the
     * first page of jobs sorted by creation date descending.
     *
     * Unlike [getJob], a `null` data field is treated as an empty list rather
     * than an error: the backend may legitimately respond with
     * `{"success": true, "data": null}` when the driver has no jobs, and that
     * should render as "No jobs available" rather than a failure.
     */
    suspend fun getJobs(
        page: Int = 0,
        size: Int = 20,
        sort: List<String> = listOf("createdAt,desc")
    ): Result<List<DeliveryJob>> = withContext(ioDispatcher) {
        try {
            val response = withTimeoutOrNull(API_TIMEOUT_MS) { api.getJobs(page, size, sort) }
            if (response == null) {
                Result.failure(
                    JobsException(JobsErrorType.SERVER_DOWN, "Request timed out — check your connection")
                )
            } else if (response.success != false) {
                Result.success(response.data.orEmpty())
            } else {
                Result.failure(
                    JobsException(JobsErrorType.OTHER, describe(response.error, response.message))
                )
            }
        } catch (e: Exception) {
            Result.failure(classifyException(e))
        }
    }

    /**
     * Gets a single delivery job by ID.
     *
     * A 404 from the backend (job not found or driver has no access) is
     * surfaced as a failed Result, not thrown.
     */
    suspend fun getJob(id: Long): Result<DeliveryJob> = withContext(ioDispatcher) {
        runCatchingBody { api.getJob(id) }
    }

    /**
     * Gets the tracking history (GPS fixes) for a job.
     */
    suspend fun getTrackingHistory(jobId: Long): Result<List<TrackingHistoryPoint>> =
        withContext(ioDispatcher) {
            runCatchingBody { api.getTrackingHistory(jobId) }
        }

    /**
     * Accepts an ASSIGNED job on behalf of the driver.
     *
     * POST /api/v1/logistics/jobs/{id}/accept?driverId={driverId}
     * Returns 400 if the job is not in ASSIGNED status or the driver is not
     * the one assigned to it.
     */
    suspend fun acceptJob(jobId: Long, driverId: Long): Result<DeliveryJob> =
        withContext(ioDispatcher) {
            runCatchingBody { api.acceptJob(jobId, driverId) }
        }

    /**
     * Advances a job to the given status (e.g. "picked_up", "delivered").
     *
     * PUT /api/v1/logistics/jobs/{id}/status?status={status}
     * Returns 400 for illegal transitions.
     */
    suspend fun updateJobStatus(jobId: Long, status: String): Result<DeliveryJob> =
        withContext(ioDispatcher) {
            runCatchingBody { api.updateJobStatus(jobId, status) }
        }

    /**
     * Cancels a job with a reason (driver declined or cancelled mid-trip).
     *
     * PUT /api/v1/logistics/jobs/{id}/cancel  body: plain string reason
     */
    suspend fun cancelJob(jobId: Long, reason: String): Result<DeliveryJob> =
        withContext(ioDispatcher) {
            runCatchingBody { api.cancelJob(jobId, reason) }
        }

    /** Unwraps a response with data into a Result, classifying rejected responses and request exceptions as failures. */
    private suspend fun <T> runCatchingBody(
        block: suspend () -> ApiResponse<T>
    ): Result<T> = try {
        val response = block()
        if (response.success != false && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(JobsException(JobsErrorType.OTHER, describe(response.error, response.message)))
        }
    } catch (e: Exception) {
        Result.failure(classifyException(e))
    }

    /**
     * Inspects a raw exception and maps it to a [JobsException] with the right
     * [JobsErrorType], so the UI can show the right message for "no internet",
     * "server down", or a generic error.
     *
     * The network layer wraps the real OkHttp call in [HostFailoverInterceptor],
     * which retries across multiple hosts and re-throws the last IOException —
     * so the original exception subtype (UnknownHostException, ConnectException,
     * etc.) is preserved and used here for classification.
     */
    private fun classifyException(e: Exception): JobsException = when (e) {
        is UnknownHostException ->
            JobsException(JobsErrorType.NO_INTERNET, "No internet connection", e)
        is SocketTimeoutException ->
            JobsException(JobsErrorType.SERVER_DOWN, "Server is taking too long to respond", e)
        is ConnectException ->
            JobsException(JobsErrorType.SERVER_DOWN, "Server is not running", e)
        is HttpException -> {
            val type = when (e.code()) {
                in 500..599 -> JobsErrorType.SERVER_DOWN
                else -> JobsErrorType.OTHER
            }
            JobsException(type, readErrorBody(e) ?: "Server responded with HTTP ${e.code()}", e)
        }
        is java.io.IOException ->
            JobsException(JobsErrorType.SERVER_DOWN, "Server is not running", e)
        else ->
            JobsException(JobsErrorType.OTHER, e.message ?: "Something went wrong", e)
    }

    /** Reads an HTTP error envelope and returns its description, or null if it is missing or cannot be decoded. */
    private fun readErrorBody(e: HttpException): String? {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        val envelope = runCatching { errorJson.decodeFromString<ApiResponse<Unit>>(raw) }.getOrNull()
            ?: return null
        return describe(envelope.error, envelope.message).takeIf { it.isNotBlank() }
    }

    /** Combines nonblank error codes and details, using the fallback when neither is present. */
    private fun describe(error: ApiError?, fallback: String): String {
        val code = error?.code?.takeIf { it.isNotBlank() }
        val details = error?.details?.takeIf { it.isNotBlank() }
        return listOfNotNull(code, details).joinToString(": ").ifBlank { fallback }
    }
}
