package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.model.LoginRequest
import com.example.kilivana_driver.data.model.LoginResult
import com.example.kilivana_driver.data.model.RefreshRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Authentication endpoints.
 *
 * None of these take an auth header parameter. The [Authorization] header is
 * attached centrally by the OkHttp interceptor for every protected call, so
 * the identity is decided in one place instead of at each call site.
 */
interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResult>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): ApiResponse<LoginResult>

    /** Recovers the signed-in user from the bearer token alone. */
    @GET("api/v1/auth/me")
    suspend fun me(): ApiResponse<AuthUser>

    @POST("api/v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}
