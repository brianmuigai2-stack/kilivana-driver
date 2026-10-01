package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Handles a 401 by refreshing the access token once, then replaying the request.
 *
 * OkHttp calls this only when a request comes back 401, which covers both
 * "token expired" and "token rejected" — the refresh call is what tells them
 * apart. If the refresh token is itself rejected, the session is cleared and
 * the 401 is returned so the app falls back to the login screen.
 *
 * A second OkHttp client is used for the refresh call on purpose: reusing the
 * authenticated client would recurse (refresh gets a 401, triggers refresh...).
 * This one has no authenticator and no bearer token, so it cannot loop.
 *
 * [Authenticator.authenticate] is a blocking OkHttp callback rather than a
 * suspend function, hence the [runBlocking] calls. They are safe here because
 * the refresh request is dispatched to a different client with its own threads,
 * so this cannot deadlock waiting on itself.
 */
class TokenAuthenticator(
    private val refreshApi: AuthApi,
    private val preferences: SessionPreferences
) : Authenticator {

    /**
     * Guards the refresh so that several parallel 401s trigger ONE refresh,
     * not one each. Without this, a screen firing five requests with a dead
     * token would burn five refresh tokens and the backend would likely
     * invalidate them.
     */
    private val lock = ReentrantLock()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Give up after one attempt: if the replay also 401s, retrying loops.
        if (response.priorResponse != null) return null

        return lock.withLock {
            // Another thread may have refreshed while we waited for the lock.
            val currentToken = SessionStore.accessToken.value
            val failedToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                return@withLock response.request.withBearer(currentToken)
            }

            val refreshToken = preferences.refreshToken() ?: return@withLock null
            val refreshed = runBlocking {
                runCatching { refreshApi.refresh(RefreshRequest(refreshToken)) }.getOrNull()
            }

            val newToken = refreshed?.data?.accessToken
            if (refreshed?.success != true || newToken.isNullOrBlank()) {
                // The refresh token is dead: drop the session so the UI can
                // send the driver back to login instead of looping on 401s.
                SessionStore.clear()
                runBlocking { preferences.clear() }
                return@withLock null
            }

            SessionStore.updateTokens(
                accessToken = newToken,
                refreshToken = refreshed.data.refreshToken.ifBlank { refreshToken }
            )
            preferences.saveTokens(
                accessToken = newToken,
                refreshToken = refreshed.data.refreshToken.ifBlank { refreshToken }
            )
            response.request.withBearer(newToken)
        }
    }
}

private fun Request.withBearer(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()
