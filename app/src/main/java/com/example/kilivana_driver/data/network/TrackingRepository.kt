package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.ReportLocationRequest
import com.example.kilivana_driver.data.model.ReportLocationResponseData
import com.example.kilivana_driver.data.model.TrackingLocationFix
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class TrackingRepository(
    private val api: TrackingApi = ApiClient.create(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val errorJson = Json { ignoreUnknownKeys = true }

    suspend fun getLatestLocation(jobId: Long): Result<TrackingLocationFix> =
        withContext(ioDispatcher) {
            runCatchingBody { api.getLatestLocation(jobId) }
        }

    suspend fun getTrackingStatus(jobId: Long): Result<TrackingLocationFix> =
        withContext(ioDispatcher) {
            runCatchingBody { api.getTrackingStatus(jobId) }
        }

    suspend fun reportLocation(jobId: Long, request: ReportLocationRequest): Result<ReportLocationResponseData> =
        withContext(ioDispatcher) {
            runCatchingBody { api.reportLocation(jobId, request) }
        }

    private suspend inline fun <reified T> runCatchingBody(
        crossinline block: suspend () -> ApiResponse<T>
    ): Result<T> = try {
        val response = block()
        if (response.success != false && response.data != null) {
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
