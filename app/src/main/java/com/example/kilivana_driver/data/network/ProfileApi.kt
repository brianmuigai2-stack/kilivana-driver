package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DriverImage
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * Driver profile image endpoints.
 *
 * Every call needs the logged-in user's id in BOTH places: as the `X-User-Id`
 * header (the backend's auth check) and as the `userId` path segment (which
 * profile is being asked for). The repository below supplies both from a
 * single value so they can't drift apart.
 */
interface ProfileApi {

    @GET("api/v1/profiles/drivers/{userId}/images")
    suspend fun getDriverImages(
        @Header("X-User-Id") userIdHeader: Long,
        @Path("userId") userIdPath: Long
    ): ApiResponse<List<DriverImage>>
}
