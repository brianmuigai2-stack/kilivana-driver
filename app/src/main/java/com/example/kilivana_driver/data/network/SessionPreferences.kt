package com.example.kilivana_driver.data.network

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kilivana_driver.data.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Persists the signed-in session so "Remember me" survives an app restart.
 *
 * Only written when the driver ticks the box at login; without it the session
 * stays in [SessionStore] only and is gone when the process dies.
 *
 * NOTE: these values are plaintext in the app's private storage, which is
 * adequate for the demo backend's placeholder tokens but is not adequate for
 * real credentials. If real JWTs get issued, move this behind
 * EncryptedSharedPreferences or the Android Keystore before shipping.
 */
class SessionPreferences(private val context: Context) {

    private object Keys {
        val REMEMBER = booleanPreferencesKey("remember_me")
        val USER_ID = longPreferencesKey("user_id")
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val PHONE = stringPreferencesKey("user_phone")
        val ROLE = stringPreferencesKey("user_role")
        val STATUS = stringPreferencesKey("user_status")
        val VERIFICATION = stringPreferencesKey("user_verification")
        val CREATED_AT = stringPreferencesKey("user_created_at")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val rememberMe: Flow<Boolean> =
        context.authDataStore.data.map { it[Keys.REMEMBER] ?: false }

    suspend fun isRemembered(): Boolean = rememberMe.first()

    /** Called after a successful login, only when [rememberMe] was ticked. */
    suspend fun save(user: AuthUser, accessToken: String?, refreshToken: String?) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.REMEMBER] = true
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.NAME] = user.name
            prefs[Keys.EMAIL] = user.email
            prefs[Keys.PHONE] = user.phone
            prefs[Keys.ROLE] = user.role
            prefs[Keys.STATUS] = user.status
            prefs[Keys.VERIFICATION] = user.verificationStatus
            prefs[Keys.CREATED_AT] = user.createdAt
            if (accessToken != null) prefs[Keys.ACCESS_TOKEN] = accessToken
            if (refreshToken != null) prefs[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    /** Restores a remembered session, or null when there is nothing to restore. */
    suspend fun restore(): RestoredSession? {
        val prefs = context.authDataStore.data.first()
        if (!(prefs[Keys.REMEMBER] ?: false)) return null
        val id = prefs[Keys.USER_ID] ?: return null
        val user = AuthUser(
            id = id,
            name = prefs[Keys.NAME].orEmpty(),
            email = prefs[Keys.EMAIL].orEmpty(),
            phone = prefs[Keys.PHONE].orEmpty(),
            role = prefs[Keys.ROLE].orEmpty(),
            status = prefs[Keys.STATUS].orEmpty(),
            verificationStatus = prefs[Keys.VERIFICATION].orEmpty(),
            createdAt = prefs[Keys.CREATED_AT].orEmpty()
        )
        return RestoredSession(user, prefs[Keys.ACCESS_TOKEN])
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}

data class RestoredSession(val user: AuthUser, val accessToken: String?)
