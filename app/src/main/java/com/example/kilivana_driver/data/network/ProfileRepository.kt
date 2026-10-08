package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DriverImage
import com.example.kilivana_driver.data.model.DriverProfile
import com.example.kilivana_driver.data.model.DriverProfileRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

/**
 * Driver profile and vehicle details, plus the licence/vehicle images.
 *
 * Consistent with the other repositories: suspend + [Result], never throws, and
 * surfaces the backend's `error.code` / `error.details` rather than a bare HTTP
 * status. Failures here are ordinary (a driver may not have a profile yet), so
 * they are returned, not raised.
 */
class ProfileRepository(
    private val api: ProfileApi = ApiClient.create()
) {
    private val errorJson = Json { ignoreUnknownKeys = true }

    /** The signed-in driver's profile, or a failure if they have not created one. */
    suspend fun getProfile(userId: Long): Result<DriverProfile> =
        call { api.getDriverProfile(userId).mapProfileImages() }

    suspend fun createProfile(
        userId: Long,
        body: DriverProfileRequest
    ): Result<DriverProfile> = call { api.createDriverProfile(userId, body) }

    /**
     * Partial update. Blank fields are dropped before sending so a driver
     * editing only the vehicle number does not blank their licence number.
     */
    suspend fun updateProfile(
        userId: Long,
        body: DriverProfileRequest
    ): Result<DriverProfile> = call { api.updateDriverProfile(userId, body.withoutBlanks()) }

    /** Administrator only on the backend; kept for parity, not used by the app. */
    suspend fun deleteProfile(userId: Long): Result<Unit> =
        callUnit { api.deleteDriverProfile(userId) }

    suspend fun getImages(userId: Long): Result<List<DriverImage>> =
        call { api.getDriverImages(userId).mapImages() }

    /**
     * Uploads one image from a local file, e.g. a photo the driver picked, and
     * returns the profile's images as the server now sees them.
     *
     * The file is read into memory first, which suits the handful of small
     * images a profile holds but not multi-megabyte camera files without
     * streaming them.
     */
    suspend fun uploadImage(
        userId: Long,
        file: File,
        isPrimary: Boolean? = null
    ): Result<List<DriverImage>> = withContext(Dispatchers.IO) {
        try {
            val part = MultipartBody.Part.createFormData(
                name = "image",
                filename = file.name,
                body = file.asRequestBody(mediaTypeFor(file))
            )
            val response = api.uploadDriverImage(
                userId = userId,
                image = part,
                isPrimary = isPrimary
            ).mapImages()
            if (response.success) {
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

    suspend fun deleteImage(userId: Long, imageId: Long): Result<Unit> =
        callUnit { api.deleteDriverImage(userId, imageId) }

    private suspend fun <T> call(block: suspend () -> ApiResponse<T>): Result<T> =
        withContext(Dispatchers.IO) { runCatchingBody(block) }

    private suspend fun callUnit(block: suspend () -> ApiResponse<Unit>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = block()
                if (response.success) Result.success(Unit)
                else Result.failure(Exception(describe(response.error, response.message)))
            } catch (e: HttpException) {
                Result.failure(Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun <T> runCatchingBody(block: suspend () -> ApiResponse<T>): Result<T> = try {
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

    /**
     * Ensures every image URL is absolute. The backend sometimes returns a
     * relative path like "/uploads/foo.jpg" — prepend the base URL so Coil
     * can actually load it.
     */
    private fun resolveUrl(url: String): String {
        val base = ApiClient.BASE_URL.trimEnd('/')
        return if (url.startsWith("http")) url else "$base$url"
    }

    private fun ApiResponse<List<DriverImage>>.mapImages(): ApiResponse<List<DriverImage>> =
        copy(data = data?.map { it.copy(url = resolveUrl(it.url)) })

    private fun ApiResponse<DriverProfile>.mapProfileImages(): ApiResponse<DriverProfile> =
        copy(data = data?.let { p ->
            p.copy(images = p.images?.map { it.copy(url = resolveUrl(it.url)) })
        })

    private fun mediaTypeFor(file: File) = when (file.extension.lowercase()) {
        "png" -> "image/png".toMediaTypeOrNull()
        "jpg", "jpeg" -> "image/jpeg".toMediaTypeOrNull()
        "webp" -> "image/webp".toMediaTypeOrNull()
        else -> "image/*".toMediaTypeOrNull()
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

/** Drops blank/empty strings so a partial update cannot wipe untouched fields. */
private fun DriverProfileRequest.withoutBlanks() = DriverProfileRequest(
    licenseNumber = licenseNumber?.takeIf { it.isNotBlank() },
    vehicleType = vehicleType?.takeIf { it.isNotBlank() },
    vehicleNumber = vehicleNumber?.takeIf { it.isNotBlank() },
    vehicleDetails = vehicleDetails?.takeIf { it.isNotBlank() },
    availabilityStatus = availabilityStatus?.takeIf { it.isNotBlank() }
)
