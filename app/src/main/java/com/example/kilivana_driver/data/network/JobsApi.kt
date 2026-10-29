package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DeliveryJob
import com.example.kilivana_driver.data.model.ProofOfDelivery
import com.example.kilivana_driver.data.model.TrackingHistoryPoint
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Logistics / delivery jobs endpoints.
 *
 * GET /api/v1/logistics/jobs             — list delivery jobs (pageable)
 * GET /api/v1/logistics/jobs/{id}        — get a single delivery job by ID
 * GET /api/v1/logistics/jobs/{jobId}/tracking — get the tracking history of a job
 *
 * All endpoints are protected by the bearer token attached centrally by
 * [AuthInterceptor]. The driver identity is derived from the token server-side.
 */
interface JobsApi {

    /**
     * Lists delivery jobs available / assigned to the current driver.
     *
     * Spring-style pageable: pass `page`, `size`, and one or more `sort`
     * directives (e.g. "createdAt,desc"). Defaults return the first page of
     * jobs sorted by creation date descending.
     *
     * GET /api/v1/logistics/jobs
     */
    @GET("api/v1/logistics/jobs")
    suspend fun getJobs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: List<String> = listOf("createdAt,desc")
    ): ApiResponse<List<DeliveryJob>>

    /**
     * Gets a single delivery job by its ID.
     *
     * Returns the same [DeliveryJob] shape as one element of [getJobs].
     * A 404 means the job does not exist or the driver has no access to it.
     *
     * GET /api/v1/logistics/jobs/{id}
     */
    @GET("api/v1/logistics/jobs/{id}")
    suspend fun getJob(
        @Path("id") id: Long
    ): ApiResponse<DeliveryJob>

    /**
     * Gets the location-tracking history for a job.
     *
     * Returns the GPS fixes the driver uploaded while the job was active,
     * ordered chronologically (oldest first).
     *
     * GET /api/v1/logistics/jobs/{jobId}/tracking
     */
    @GET("api/v1/logistics/jobs/{jobId}/tracking")
    suspend fun getTrackingHistory(
        @Path("jobId") jobId: Long
    ): ApiResponse<List<TrackingHistoryPoint>>

    /** POST /api/v1/logistics/jobs/{id}/accept — ASSIGNED → ACCEPTED */
    @POST("api/v1/logistics/jobs/{id}/accept")
    suspend fun acceptJob(
        @Path("id") id: Long,
        @Query("driverId") driverId: Long
    ): ApiResponse<DeliveryJob>

    /** PUT /api/v1/logistics/jobs/{id}/status — advance job through the pipeline */
    @PUT("api/v1/logistics/jobs/{id}/status")
    suspend fun updateJobStatus(
        @Path("id") id: Long,
        @Query("status") status: String
    ): ApiResponse<DeliveryJob>

    /** PUT /api/v1/logistics/jobs/{id}/cancel — cancel with a plain-string reason body */
    @PUT("api/v1/logistics/jobs/{id}/cancel")
    suspend fun cancelJob(
        @Path("id") id: Long,
        @Body reason: String
    ): ApiResponse<DeliveryJob>
    @DELETE("api/v1/logistics/jobs/{id}")
    suspend fun deleteJob(@Path("id") id: Long): ApiResponse<Unit>

    @GET("api/v1/logistics/jobs/status/{status}")
    suspend fun getJobsByStatus(@Path("status") status: String): ApiResponse<List<DeliveryJob>>

    @GET("api/v1/logistics/jobs/order/{orderId}")
    suspend fun getJobsByOrder(@Path("orderId") orderId: Long): ApiResponse<List<DeliveryJob>>

    @GET("api/v1/logistics/jobs/driver/{driverId}")
    suspend fun getJobsByDriver(@Path("driverId") driverId: Long): ApiResponse<List<DeliveryJob>>

    @POST("api/v1/logistics/jobs")
    suspend fun createJob(@Body body: DeliveryJob): ApiResponse<DeliveryJob>

    @POST("api/v1/logistics/jobs/{id}/status")
    suspend fun updateJobStatusPost(@Path("id") id: Long, @Query("status") status: String): ApiResponse<DeliveryJob>

    @POST("api/v1/logistics/jobs/{id}/assign")
    suspend fun assignJobPost(@Path("id") id: Long, @Query("driverId") driverId: Long): ApiResponse<DeliveryJob>

    @POST("api/v1/logistics/jobs/{id}/accept")
    suspend fun acceptJobPost(@Path("id") id: Long, @Query("driverId") driverId: Long): ApiResponse<DeliveryJob>

    @POST("api/v1/logistics/jobs/{jobId}/proof-of-delivery")
    suspend fun fileProofOfDeliveryForJob(@Path("jobId") jobId: Long, @Body body: ProofOfDelivery, @Query("otp") otp: String? = null): ApiResponse<ProofOfDelivery>

    @POST("api/v1/logistics/jobs/{id}/otp/verify")
    suspend fun verifyOtp(@Path("id") id: Long, @Query("code") code: String): ApiResponse<DeliveryJob>

    @PUT("api/v1/logistics/jobs/{id}/assign")
    suspend fun assignJobPut(@Path("id") id: Long, @Query("driverId") driverId: Long): ApiResponse<DeliveryJob>

    @PUT("api/v1/logistics/jobs/{id}/accept")
    suspend fun acceptJobPut(@Path("id") id: Long, @Query("driverId") driverId: Long): ApiResponse<DeliveryJob>
}
