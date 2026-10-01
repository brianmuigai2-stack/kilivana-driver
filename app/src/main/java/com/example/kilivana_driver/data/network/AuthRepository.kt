package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.model.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Signs the driver in and reads back the signed-in user.
 *
 * Same shape as the other repositories: suspend + [Result], never throws, and
 * surfaces the backend's own `error.code` / `error.details` rather than a bare
 * HTTP status. A failed login is a normal outcome here, not an exception.
 */
class AuthRepository(
    private val api: AuthApi = ApiClient.create(),
    private val preferences: SessionPreferences? = null
) {
    private val errorJson = Json { ignoreUnknownKeys = true }

    /** True once a remembered session has been read back off disk at startup. */
    private val _restoreComplete = MutableStateFlow(false)
    val restoreComplete: StateFlow<Boolean> = _restoreComplete.asStateFlow()

    /**
     * Re-signs the driver in from the stored session when they asked to be
     * remembered, recovering the user from `/auth/me` rather than trusting the
     * cached copy, so a revoked or expired session is caught at startup.
     */
    suspend fun restoreSession(): Boolean {
        val prefs = preferences ?: run {
            _restoreComplete.value = true
            return false
        }
        val restored = prefs.restore()
        if (restored == null) {
            _restoreComplete.value = true
            return false
        }

        // Seed the token first so the /auth/me call is authenticated.
        val access = prefs.accessToken()
        val refresh = prefs.refreshToken()
        if (access.isNullOrBlank()) {
            prefs.clear()
            _restoreComplete.value = true
            return false
        }
        SessionStore.save(restored.user, access, refresh)

        val me = me()
        if (me.isFailure) {
            // /auth/me is authoritative: if the token is no longer good, the
            // driver must log in again rather than half-work.
            SessionStore.clear()
            prefs.clear()
            _restoreComplete.value = true
            return false
        }

        me.getOrNull()?.let { SessionStore.save(it, SessionStore.accessToken.value, refresh) }
        _restoreComplete.value = true
        return true
    }

    /**
     * On success the user and tokens are saved to [SessionStore], so every
     * later endpoint can read the id without being handed it again.
     */
    suspend fun login(
        email: String,
        password: String,
        rememberMe: Boolean = false
    ): Result<AuthUser> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.login(LoginRequest(email = email.trim(), password = password))
                val data = response.data
                when {
                    response.success && data != null && data.accessToken.isNotBlank() -> {
                        SessionStore.save(data.user, data.accessToken, data.refreshToken)
                        if (rememberMe) {
                            preferences?.save(data.user, data.accessToken, data.refreshToken)
                        } else {
                            preferences?.clear()
                        }
                        Result.success(data.user)
                    }

                    response.success -> Result.failure(
                        Exception("Logged in but the server returned no access token")
                    )

                    else -> Result.failure(Exception(describe(response.error, response.message)))
                }
            } catch (e: HttpException) {
                Result.failure(Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Recovers the signed-in user from the bearer token. */
    suspend fun me(): Result<AuthUser> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.me()
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(describe(response.error, response.message)))
                }
            } catch (e: HttpException) {
                Result.failure(Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Best-effort sign-out. Never fails the caller: whether or not the server
     * hears about it, the local session is cleared either way.
     */
    suspend fun logout(): Result<Unit> =
        withContext(Dispatchers.IO) {
            val result = runCatching { api.logout() }
            SessionStore.clear()
            preferences?.clear()
            if (result.isFailure) Result.failure(result.exceptionOrNull()!!)
            else Result.success(Unit)
        }

    private fun readErrorBody(e: HttpException): String? {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        val envelope = runCatching { errorJson.decodeFromString<ApiResponse<Unit>>(raw) }.getOrNull()
            ?: return null
        return describe(envelope.error, envelope.message).takeIf { it.isNotBlank() }
    }

    private fun describe(error: ApiError?, fallback: String): String {
        val code = error?.code?.takeIf { it.isNotBlank() }
        val details = error?.details?.takeIf { it.isNotBlank() }
        return listOfNotNull(code, details).joinToString(": ").ifBlank { fallback }
    }
}
