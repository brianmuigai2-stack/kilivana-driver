package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
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

            // Read the refresh token from the live session, not from disk. Disk
            // only holds it when "Remember me" was ticked, so reading it from
            // there meant refresh silently never worked for everyone else.
            val refreshToken = SessionStore.refreshToken.value?.takeIf { it.isNotBlank() }
                ?: preferences.refreshToken()?.takeIf { it.isNotBlank() }
            if (refreshToken == null) {
                // Nothing to refresh with: end the session so the UI returns to login.
                endSession()
                return@withLock null
            }

            val refreshed = try {
                runBlocking { refreshApi.refresh(RefreshRequest(refreshToken)) }
            } catch (e: HttpException) {
                // The server answered and said no: the refresh token is dead.
                if (e.code() == 400 || e.code() == 401 || e.code() == 403) endSession()
                return@withLock null
            } catch (e: Exception) {
                // No signal, timeout, etc. The token may still be fine, so keep
                // the session and let the next request try again.
                return@withLock null
            }

            val data = refreshed.data
            if (!refreshed.success || data == null || data.accessToken.isBlank()) {
                endSession()
                return@withLock null
            }

            val newToken = data.accessToken
            val newRefresh = data.refreshToken.ifBlank { refreshToken }
            SessionStore.updateTokens(accessToken = newToken, refreshToken = newRefresh)
            // Only write to disk if the driver chose "Remember me" (tokens are on
            // disk only in that case); otherwise keep them in memory.
            if (!preferences.refreshToken().isNullOrBlank()) {
                preferences.saveTokens(accessToken = newToken, refreshToken = newRefresh)
            }
            response.request.withBearer(newToken)
        }
    }

    /** Drops the in-memory and stored session so the app falls back to login. */
    private fun endSession() {
        SessionStore.clear()
        runBlocking { preferences.clear() }
    }
}

private fun Request.withBearer(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()
