package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiError
import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.model.LoginRequest
import com.example.kilivana_driver.data.model.ResetPasswordRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** The backend `role` value for a driver account. Confirm it against your API. */
const val DRIVER_ROLE = "DRIVER"
/** The backend `role` value for an inspector account. Confirm it against your API. */
const val INSPECTOR_ROLE = "INSPECTOR"

/** The backend `role` value the signed-in account must carry to use this app. */
private fun AuthUser.isAllowedRole(allowedRole: String): Boolean =
    role.equals(allowedRole, ignoreCase = true)

/**
 * Signs an account in and reads back the signed-in user. `allowedRole` limits
 * which role may sign in (drivers use [DRIVER_ROLE], inspectors [INSPECTOR_ROLE]).
 *
 * Same shape as the other repositories: suspend + [Result], never throws, and
 * surfaces the backend's own `error.code` / `error.details` rather than a bare
 * HTTP status. A failed login is a normal outcome here, not an exception.
 */
class AuthRepository(
    private val api: AuthApi = ApiClient.create(),
    private val preferences: SessionPreferences? = null,
    private val allowedRole: String = DRIVER_ROLE,
    private val restoreAllowedRoles: Set<String> = setOf(allowedRole)
) {
    private val errorJson = Json { ignoreUnknownKeys = true }

    /** True once a remembered session has been read back off disk at startup. */
    private val _restoreComplete = MutableStateFlow(false)
    val restoreComplete: StateFlow<Boolean> = _restoreComplete.asStateFlow()

    /**
     * Restores a remembered session instantly off disk so the app opens without
     * stalling on the splash screen. Validates the session asynchronously against
     * /auth/me in the background.
     */
    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        val prefs = preferences ?: run {
            _restoreComplete.value = true
            return@withContext false
        }
        val restored = prefs.restore()
        if (restored == null) {
            _restoreComplete.value = true
            return@withContext false
        }

        val access = prefs.accessToken()
        val refresh = prefs.refreshToken()
        if (access.isNullOrBlank()) {
            prefs.clear()
            _restoreComplete.value = true
            return@withContext false
        }

        if (!restoreAllowedRoles.any(restored.user::isAllowedRole)) {
            SessionStore.clear()
            prefs.clear()
            _restoreComplete.value = true
            return@withContext false
        }

        // Restore local session state immediately to dismiss splash screen without delay
        SessionStore.save(restored.user, access, refresh)
        _restoreComplete.value = true

        // Perform online token validation in the background
        CoroutineScope(Dispatchers.IO).launch {
            when (val check = checkSession()) {
                is SessionCheck.Valid -> {
                    if (restoreAllowedRoles.any(check.user::isAllowedRole)) {
                        SessionStore.save(check.user, SessionStore.accessToken.value, SessionStore.refreshToken.value)
                        preferences.save(check.user, SessionStore.accessToken.value.orEmpty(), SessionStore.refreshToken.value.orEmpty())
                    } else {
                        SessionStore.clear()
                        preferences.clear()
                    }
                }

                SessionCheck.Rejected -> {
                    SessionStore.clear()
                    preferences.clear()
                }

                SessionCheck.Unreachable -> {
                    // Server offline or tunnel restarting; keep cached session
                }
            }
        }

        return@withContext true
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
                    // Right password, wrong kind of account (e.g. a driver logging
                    // into the inspector app): do not keep the session. The server
                    // still enforces roles; this just stops the wrong app from
                    // opening for accounts that don't carry [allowedRole].
                    response.success != false && data != null && data.accessToken.isNotBlank() &&
                        !data.user.isAllowedRole(allowedRole) -> Result.failure(
                        Exception("This account is not an inspector.")
                    )

                    response.success != false && data != null && data.accessToken.isNotBlank() -> {
                        SessionStore.save(data.user, data.accessToken, data.refreshToken)
                        if (rememberMe) {
                            preferences?.save(data.user, data.accessToken, data.refreshToken)
                        } else {
                            preferences?.clear()
                        }
                        Result.success(data.user)
                    }

                    response.success == true -> Result.failure(
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

    /** Requests a password-reset email for the supplied address. */
    suspend fun forgotPassword(email: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.forgotPassword(email.trim())
                if (response.success == false) {
                    Result.failure(Exception(describe(response.error, response.message)))
                } else {
                    val message = response.data?.takeIf { it.isNotBlank() }
                        ?: response.message.takeIf { it.isNotBlank() }
                        ?: "Password recovery request submitted."
                    Result.success(message)
                }
            } catch (e: HttpException) {
                Result.failure(Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Consumes an email recovery token and sets the account's new password. */
    suspend fun resetPassword(token: String, newPassword: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.resetPassword(
                    ResetPasswordRequest(token = token.trim(), newPassword = newPassword)
                )
                if (response.success == false) {
                    Result.failure(Exception(describe(response.error, response.message)))
                } else {
                    val message = response.data?.takeIf { it.isNotBlank() }
                        ?: response.message.takeIf { it.isNotBlank() }
                        ?: "Password updated. Sign in with your new password."
                    Result.success(message)
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
                if (response.success != false && response.data != null) {
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
            if (response.success != false && user != null) SessionCheck.Valid(user) else SessionCheck.Rejected
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
