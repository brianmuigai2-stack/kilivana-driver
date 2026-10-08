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

/** The backend `role` value for a driver account. Confirm it against your API. */
private const val DRIVER_ROLE = "DRIVER"

private fun AuthUser.isDriver(): Boolean = role.equals(DRIVER_ROLE, ignoreCase = true)

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

        return when (val check = checkSession()) {
            is SessionCheck.Valid -> {
                if (check.user.isDriver()) {
                    SessionStore.save(check.user, SessionStore.accessToken.value, SessionStore.refreshToken.value)
                    _restoreComplete.value = true
                    true
                } else {
                    SessionStore.clear()
                    prefs.clear()
                    _restoreComplete.value = true
                    false
                }
            }

            SessionCheck.Rejected -> {
                // The server answered and said this session is no good (expired
                // or revoked), so the driver must log in again.
                SessionStore.clear()
                prefs.clear()
                _restoreComplete.value = true
                false
            }

            SessionCheck.Unreachable -> {
                // No signal or the server is down. That says nothing about the
                // session, so keep the cached user and tokens and let the
                // driver in; requests will work again once they are online.
                _restoreComplete.value = true
                true
            }
        }
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
                    // Right password, wrong kind of account (e.g. an admin): do not
                    // keep the session. The server still enforces roles; this just
                    // stops the driver app from opening for non-drivers.
                    response.success && data != null && data.accessToken.isNotBlank() &&
                        !data.user.isDriver() -> Result.failure(
                        Exception("This app is for Kilivana drivers only.")
                    )

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

    private sealed interface SessionCheck {
        data class Valid(val user: AuthUser) : SessionCheck
        data object Rejected : SessionCheck
        data object Unreachable : SessionCheck
    }

    /**
     * Asks /auth/me whether the stored session still works, and tells a real
     * rejection (401/403) apart from simply being offline. A 401 first goes
     * through the token authenticator, so an expired access token with a valid
     * refresh token is refreshed and counts as Valid.
     */
    private suspend fun checkSession(): SessionCheck = withContext(Dispatchers.IO) {
        try {
            val response = api.me()
            val user = response.data
            if (response.success && user != null) SessionCheck.Valid(user) else SessionCheck.Rejected
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) SessionCheck.Rejected else SessionCheck.Unreachable
        } catch (e: Exception) {
            SessionCheck.Unreachable
        }
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
