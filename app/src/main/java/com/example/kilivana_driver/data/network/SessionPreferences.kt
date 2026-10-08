package com.example.kilivana_driver.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.kilivana_driver.data.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Stores the session, with the tokens encrypted at rest.
 *
 * Per the integration spec §3.3 the refresh token is a long-lived credential,
 * so it goes into [EncryptedSharedPreferences], which is backed by a master key
 * held in the Android Keystore — the plaintext key never leaves the keystore,
 * so a rooted-device or backup extraction can't read the token directly.
 *
 * The non-secret profile fields (name, role, status) stay in DataStore: they
 * are not credentials, and keeping them out of the encrypted file means the UI
 * can read them without touching the keystore.
 */
class SessionPreferences(private val context: Context) {

    private val securePrefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "kilivana_secure_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    /**
     * True when encryption is unavailable and tokens would have to be stored
     * in the clear. Surfaced so the app can warn rather than silently
     * downgrade — see [saveTokens] for how it behaves.
     */
    val isEncryptionAvailable: Boolean get() = securePrefs != null

    fun saveTokens(accessToken: String, refreshToken: String) {
        val prefs = securePrefs
        if (prefs == null) {
            // Refusing to persist a token in plaintext is the safe failure: the
            // driver stays signed in for this process, but "remember me"
            // silently does not work rather than writing a credential to disk.
            return
        }
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    fun accessToken(): String? = securePrefs?.getString(KEY_ACCESS, null)

    fun refreshToken(): String? = securePrefs?.getString(KEY_REFRESH, null)

    /** Clears only the credentials, e.g. after a refresh token is rejected. */
    fun clearTokens() {
        securePrefs?.edit()?.remove(KEY_ACCESS)?.remove(KEY_REFRESH)?.apply()
    }

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
        }
        if (accessToken != null && refreshToken != null) {
            saveTokens(accessToken, refreshToken)
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
        return RestoredSession(user)
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
        clearTokens()
    }

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
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

data class RestoredSession(val user: AuthUser)
