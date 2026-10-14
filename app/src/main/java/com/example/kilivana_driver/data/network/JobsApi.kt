package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DeliveryJob
import com.example.kilivana_driver.data.model.TrackingHistoryPoint
import retrofit2.http.GET
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
}
