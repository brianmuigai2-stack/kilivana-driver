package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.TrackingEvent
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TrackingEventsApi {
    @GET("api/v1/logistics/tracking-events/{id}")
    suspend fun getTrackingEvent(@Path("id") id: Long): ApiResponse<TrackingEvent>

    @GET("api/v1/logistics/tracking-events/job/{jobId}")
    suspend fun getTrackingEventsByJob(@Path("jobId") jobId: Long): ApiResponse<List<TrackingEvent>>

    @GET("api/v1/logistics/tracking-events/driver/{driverId}")
    suspend fun getTrackingEventsByDriver(@Path("driverId") driverId: Long): ApiResponse<List<TrackingEvent>>

    @POST("api/v1/logistics/tracking-events")
    suspend fun recordTrackingEvent(@Body body: TrackingEvent): ApiResponse<TrackingEvent>
}
