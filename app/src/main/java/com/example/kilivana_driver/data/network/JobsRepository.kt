package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DeliveryJob
import com.example.kilivana_driver.data.model.TrackingHistoryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
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
                Result.failure(Exception("Request timed out after ${API_TIMEOUT_MS / 1000}s"))
            } else if (response.success) {
                Result.success(response.data.orEmpty())
            } else {
                Result.failure(Exception(describe(response.error, response.message)))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
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

    private suspend fun <T> runCatchingBody(
        block: suspend () -> ApiResponse<T>
    ): Result<T> = try {
        val response = block()
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(describe(response.error, response.message)))
        }
    } catch (e: HttpException) {
        Result.failure(Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
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
