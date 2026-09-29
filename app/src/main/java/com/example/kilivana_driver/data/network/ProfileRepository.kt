package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DriverImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Fetches the images attached to a driver's profile.
 *
 * Returns a [Result] rather than throwing, matching [HealthRepository]: the
 * UI shows a message on failure instead of crashing, and the backend's own
 * `error.code` / `error.details` are folded into the message so the user sees
 * why it failed rather than a bare HTTP status.
 */
class ProfileRepository(
    private val api: ProfileApi = ApiClient.create()
) {
    private val errorJson = Json { ignoreUnknownKeys = true }

    suspend fun getDriverImages(userId: Long): Result<List<DriverImage>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getDriverImages(
                    userIdHeader = userId,
                    userIdPath = userId
                )
                if (response.success) {
                    Result.success(response.data.orEmpty())
                } else {
                    Result.failure(Exception(describe(response.error, response.message)))
                }
            } catch (e: HttpException) {
                // The backend answers failures with a non-2xx status AND a full
                // error body, so surface the real reason instead of a bare 404.
                Result.failure(Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Pulls `message` / `error.details` out of a failed response's body, if it has one. */
    private fun readErrorBody(e: HttpException): String? {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        val envelope = runCatching { errorJson.decodeFromString<ApiResponse<Unit>>(raw) }.getOrNull()
            ?: return null
        return describe(envelope.error, envelope.message).takeIf { it.isNotBlank() }
    }

    /** The primary image, i.e. the one the backend flagged, else the first by sort order. */
    suspend fun getPrimaryImage(userId: Long): Result<DriverImage?> =
        getDriverImages(userId).map { images ->
            images.firstOrNull { it.isPrimary }
                ?: images.minByOrNull { it.sortOrder }
        }

    private fun describe(error: ApiError?, fallback: String): String {
        val code = error?.code?.takeIf { it.isNotBlank() }
        val details = error?.details?.takeIf { it.isNotBlank() }
        return listOfNotNull(code, details).joinToString(": ").ifBlank { fallback }
    }
}
