package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.model.LoginRequest
import com.example.kilivana_driver.data.model.LoginResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Authentication endpoints.
 *
 * Note both the login request and `me` are different things: login takes an
 * email + password in the body and needs no auth header, while `me` is called
 * with whatever identity you already hold (the X-User-Id header, or a bearer
 * token once the backend accepts one).
 */
interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResult>

    @GET("api/v1/auth/me")
    suspend fun me(@Header("X-User-Id") userId: Long): ApiResponse<AuthUser>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Header("X-User-Id") userId: Long): ApiResponse<Unit>
}
