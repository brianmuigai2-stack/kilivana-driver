package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.TripStatus
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
     * Gets the list of active trips for the current driver.
     *
     * Typically returns 0 or 1 trip (the one currently being tracked).
     *
     * GET /api/v1/trips/active
     */
    @GET("api/v1/trips/active")
    suspend fun getActiveTrips(): ApiResponse<List<TripStatus>>
}
