package com.example.kilivana_driver.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.kilivana_driver.data.model.AuthUser

/**
 * Holds the signed-in user for the current app session.
 *
 * Deliberately in-memory only: a token in a plain field dies with the process,
 * which is the safe default until real persistence (EncryptedSharedPreferences
 * or DataStore) is added. Saving a token to disk unencrypted would be worse
 * than not persisting it at all.
 */
object SessionStore {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    /**
     * The id every X-User-Id endpoint needs. Null until a login succeeds, so
     * callers must handle the signed-out case rather than sending 0.
     */
    val userId: Long?
        get() = _currentUser.value?.id?.takeIf { it > 0L }

    fun save(user: AuthUser, accessToken: String?, refreshToken: String?) {
        _currentUser.value = user
        _accessToken.value = accessToken
        _refreshToken = refreshToken
    }

    private var _refreshToken: String? = null

    fun clear() {
        _currentUser.value = null
        _accessToken.value = null
        _refreshToken = null
    }
}
