package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable

/**
 * The user object returned by both POST /api/v1/auth/login and GET
 * /api/v1/auth/me. `role`, `status` and `verificationStatus` are modelled as
 * plain strings rather than enums on purpose: the Swagger docs only show one
 * example each (FARMER / ACTIVE / PENDING), and a sealed enum would throw on
 * any value the backend adds later instead of just carrying it through.
 */
@Serializable
data class AuthUser(
    val id: Long = 0L,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val status: String = "",
    val verificationStatus: String = "",
    val createdAt: String = ""
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResult(
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenType: String = "",
    /**
     * Null in current development (JWT_EXPIRATION=0 means tokens don't expire).
     * Modelled nullable per the integration spec; if real expiry is enabled
     * this becomes a millisecond duration.
     */
    val expiresIn: Long? = null,
    val user: AuthUser = AuthUser()
)

/** Body for POST /api/v1/auth/refresh — the refresh token, not a token object. */
@Serializable
data class RefreshRequest(
    val refreshToken: String
)
