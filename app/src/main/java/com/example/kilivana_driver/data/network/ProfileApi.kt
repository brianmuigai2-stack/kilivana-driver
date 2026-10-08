package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DriverImage
import com.example.kilivana_driver.data.model.DriverProfile
import com.example.kilivana_driver.data.model.DriverProfileRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Driver profile and vehicle endpoints, plus the licence/vehicle images that
 * hang off them.
 *
 * Per the integration spec §5 (D1) the backend does not trust an `X-User-Id`
 * header for identity, so none of these take one. The bearer token is attached
 * centrally by [AuthInterceptor], and the backend decides from that token
 * whether the caller is the driver themself or an administrator — it is not the
 * client's job to pick a userId, it comes from the signed-in session.
 */
interface ProfileApi {

    @GET("api/v1/profiles/drivers/{userId}")
    suspend fun getDriverProfile(
        @Path("userId") userId: Long
    ): ApiResponse<DriverProfile>

    @POST("api/v1/profiles/drivers/{userId}")
    suspend fun createDriverProfile(
        @Path("userId") userId: Long,
        @Body body: DriverProfileRequest
    ): ApiResponse<DriverProfile>

    @PUT("api/v1/profiles/drivers/{userId}")
    suspend fun updateDriverProfile(
        @Path("userId") userId: Long,
        @Body body: DriverProfileRequest
    ): ApiResponse<DriverProfile>

    @DELETE("api/v1/profiles/drivers/{userId}")
    suspend fun deleteDriverProfile(
        @Path("userId") userId: Long
    ): ApiResponse<Unit>

    @GET("api/v1/profiles/drivers/{userId}/images")
    suspend fun getDriverImages(
        @Path("userId") userId: Long
    ): ApiResponse<List<DriverImage>>

    /**
     * Uploads a licence or vehicle image as multipart/form-data under the part
     * name `image` (the server rejects any other part name). [isPrimary] is an
     * optional query flag, not a form field.
     *
     * Returns the profile's whole image list, not a single image.
     */
    @Multipart
    @POST("api/v1/profiles/drivers/{userId}/images")
    suspend fun uploadDriverImage(
        @Path("userId") userId: Long,
        @Part image: MultipartBody.Part,
        @Query("isPrimary") isPrimary: Boolean? = null
    ): ApiResponse<List<DriverImage>>

    @DELETE("api/v1/profiles/drivers/{userId}/images/{imageId}")
    suspend fun deleteDriverImage(
        @Path("userId") userId: Long,
        @Path("imageId") imageId: Long
    ): ApiResponse<Unit>
}
