package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DeliveryJob
import com.example.kilivana_driver.data.model.ProofOfDelivery
import com.example.kilivana_driver.data.model.ProofPhotoUpload
import com.example.kilivana_driver.data.model.ReportLocationResponseData
import com.example.kilivana_driver.data.model.TripLocationRequest
import com.example.kilivana_driver.data.model.TripStatus
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Driver trip lifecycle endpoints. The authenticated driver is determined by
 * the bearer token; trip actions do not send a client-selected driver ID.
 */
interface TripApi {

    @GET("api/v1/trips/mine")
    suspend fun getMyTrips(): ApiResponse<List<TripStatus>>

    @POST("api/v1/trips/jobs/{jobId}/accept")
    suspend fun acceptJob(
        @Path("jobId") jobId: Long
    ): ApiResponse<TripStatus>

    @GET("api/v1/trips/{tripId}")
    suspend fun getTrip(
        @Path("tripId") tripId: String
    ): ApiResponse<TripStatus>

    @POST("api/v1/trips/{tripId}/status")
    suspend fun updateStatus(
        @Path("tripId") tripId: String,
        @Query("status") status: String
    ): ApiResponse<TripStatus>

    @POST("api/v1/trips/{tripId}/location")
    suspend fun reportLocation(
        @Path("tripId") tripId: String,
        @Body body: TripLocationRequest
    ): ApiResponse<ReportLocationResponseData>

    @POST("api/v1/trips/{tripId}/otp/verify")
    suspend fun verifyOtp(
        @Path("tripId") tripId: String,
        @Query("otp") otp: String
    ): ApiResponse<TripStatus>

    @POST("api/v1/trips/{tripId}/proof")
    suspend fun fileProof(
        @Path("tripId") tripId: String,
        @Query("otp") otp: String?,
        @Body body: ProofOfDelivery
    ): ApiResponse<ProofOfDelivery>

    @Multipart
    @POST("api/v1/trips/{tripId}/proof-photo")
    suspend fun uploadProofPhoto(
        @Path("tripId") tripId: String,
        @Part image: MultipartBody.Part
    ): ApiResponse<ProofPhotoUpload>

    @GET("api/v1/trips/active")
    suspend fun getActiveTrips(): ApiResponse<List<DeliveryJob>>

    @GET("api/v1/trips/current")
    suspend fun getCurrentTrip(): ApiResponse<DeliveryJob>

    @POST("api/v1/trips/{tripId}/tracking/start")
    suspend fun startTracking(
        @Path("tripId") tripId: String
    ): ApiResponse<Unit>
}
