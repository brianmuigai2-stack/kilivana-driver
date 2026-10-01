package com.example.kilivana_driver.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <accessToken>` to protected requests.
 *
 * Per the integration spec §3.4 the backend deliberately does not trust an
 * `X-User-Id` header for identity, so the bearer token added here is the only
 * thing that proves who the caller is.
 *
 * The token is read from [SessionStore] on every request rather than captured
 * once, so a refresh that swaps the token takes effect on the next call without
 * rebuilding the client.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = SessionStore.accessToken.value

        // Public endpoints: sending a stale token to login would be pointless
        // at best, and can trigger a pointless refresh loop at worst.
        if (token.isNullOrBlank() || request.isPublicAuthCall()) {
            return chain.proceed(request)
        }

        val authorized = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }
}

private fun okhttp3.Request.isPublicAuthCall(): Boolean {
    val path = url.encodedPath.orEmpty()
    return path.endsWith("/auth/login") ||
        path.endsWith("/auth/refresh") ||
        path.endsWith("/auth/forgot-password") ||
        path.endsWith("/auth/reset-password")
}
