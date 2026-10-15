package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.ReportLocationRequest
import com.example.kilivana_driver.data.model.ReportLocationResponseData
import com.example.kilivana_driver.data.model.TrackingLocationFix
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Live tracking endpoints as specified.
 */
interface TrackingApi {
    /**
     * Get the latest driver location for a delivery.
     */
    @GET("api/v1/tracking/{jobId}/location")
    suspend fun getLatestLocation(
        @Path("jobId") jobId: Long
    ): ApiResponse<TrackingLocationFix>

    /**
     * Get the tracking status of a delivery.
     */
    @GET("api/v1/tracking/{jobId}/status")
    suspend fun getTrackingStatus(
        @Path("jobId") jobId: Long
    ): ApiResponse<TrackingLocationFix>

    /**
     * Report a driver's location.
     */
    @POST("api/v1/tracking/{jobId}/location")
    suspend fun reportLocation(
        @Path("jobId") jobId: Long,
        @Body body: ReportLocationRequest
    ): ApiResponse<ReportLocationResponseData>
}
