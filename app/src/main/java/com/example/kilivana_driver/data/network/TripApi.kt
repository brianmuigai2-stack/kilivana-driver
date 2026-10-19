package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.LocationUpdate
import com.example.kilivana_driver.data.model.LocationUploadResponse
import com.example.kilivana_driver.data.model.ReportLocationRequest
import com.example.kilivana_driver.data.model.StopTrackingRequest
import com.example.kilivana_driver.data.model.TripStatus
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Trip/delivery tracking endpoints.
 *
 * The backend should implement these endpoints for live delivery tracking.
 * If the backend doesn't have these yet, the exact contract is documented here
 * so backend and frontend can be developed in parallel.
 *
 * All endpoints are protected by the bearer token (attached by AuthInterceptor).
 * The driver ID is derived from the token on the server side.
 */
interface TripApi {

    /**
     * Starts tracking for a trip: ASSIGNED → ACCEPTED on the backend.
     *
     * Must be called before sending location updates. The backend rejects
     * POST /api/v1/tracking/{jobId}/location with 400 until this succeeds.
     *
     * POST /api/v1/trips/{tripId}/tracking/start
     */
    @POST("api/v1/trips/{tripId}/tracking/start")
    suspend fun startTracking(
        @Path("tripId") tripId: String
    ): ApiResponse<Unit>

    /**
     * Stops tracking for a trip.
     *
     * Call this when the delivery is completed, cancelled, or the driver
     * explicitly ends tracking.
     *
     * POST /api/v1/trips/{tripId}/tracking/stop
     */
    @POST("api/v1/trips/{tripId}/tracking/stop")
    suspend fun stopTracking(
        @Path("tripId") tripId: String,
        @Body body: StopTrackingRequest
    ): ApiResponse<Unit>

    /**
     * Sends a single GPS location update for the active trip.
     *
     * The driver app should call this periodically (configured by TrackingConfig)
     * while a trip is in HEADING_TO_PICKUP or IN_TRANSIT stage.
     *
     * POST /api/v1/trips/{tripId}/location
     */
    @POST("api/v1/trips/{tripId}/location")
    suspend fun uploadLocation(
        @Path("tripId") tripId: String,
        @Body body: LocationUpdate
    ): ApiResponse<LocationUploadResponse>

    /**
     * Sends a batch of location updates (for offline queue flush).
     *
     * POST /api/v1/trips/{tripId}/location/batch
     */
    @POST("api/v1/trips/{tripId}/location/batch")
    suspend fun uploadLocationBatch(
        @Path("tripId") tripId: String,
        @Body body: List<LocationUpdate>
    ): ApiResponse<LocationUploadResponse>

    /**
     * Gets the current trip status including last known location.
     *
     * Used for initial sync when the app resumes or reconnects.
     *
     * GET /api/v1/trips/{tripId}/status
     */
    @GET("api/v1/trips/{tripId}/status")
    suspend fun getTripStatus(
        @Path("tripId") tripId: String
    ): ApiResponse<TripStatus>

    /**
     * Gets the list of active trips for the current driver.
     *
     * Typically returns 0 or 1 trip (the one currently being tracked).
     *
     * GET /api/v1/trips/active
     */
    @GET("api/v1/trips/active")
    suspend fun getActiveTrips(): ApiResponse<List<TripStatus>>
}