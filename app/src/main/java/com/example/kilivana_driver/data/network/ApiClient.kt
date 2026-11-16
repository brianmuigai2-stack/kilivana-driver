package com.example.kilivana_driver.data.network

import android.util.Log
import com.example.kilivana_driver.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Single shared entry point for talking to the Kilivana backend.
 */
object ApiClient {

    const val BASE_URL = "https://kilivana-backend-1jte.onrender.com/"

    @Volatile
    private var preferredBackendBaseUrl: String? = null

    fun backendUrlFor(path: String): String {
        val baseUrl = (preferredBackendBaseUrl ?: BASE_URL).trimEnd('/')
        val formattedPath = if (path.startsWith("/")) path else "/$path"
        return "$baseUrl$formattedPath"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val ngrokHeaderInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .apply {
                if (chain.request().url.host.endsWith("ngrok-free.dev")) {
                    header("ngrok-skip-browser-warning", "true")
                }
            }
            .build()
        chain.proceed(request)
    }

    /**
     * Injected once the Application context exists, because secure token
     * storage needs it. [install] is called from the Application class.
     */
    @Volatile
    private var sessionPreferences: SessionPreferences? = null

    fun install(preferences: SessionPreferences) {
        sessionPreferences = preferences
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ngrokHeaderInterceptor)
            .addInterceptor(AuthInterceptor())
            .apply {
                sessionPreferences?.let { prefs ->
                    authenticator(TokenAuthenticator(refreshApi = refreshApi, preferences = prefs))
                }
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor { message ->
                            Log.d("KilivanaHttp", redactSecrets(message))
                        }.apply {
                            redactHeader("Authorization")
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * A separate client used only by [TokenAuthenticator] to call the refresh
     * endpoint. It deliberately has no authenticator and no bearer interceptor,
     * so a 401 here cannot trigger another refresh.
     */
    private val refreshApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(ngrokHeaderInterceptor)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val secretJsonFields = Regex("\"(password|accessToken|refreshToken|token|newPassword)\"\\s*:\\s*\"[^\"]*\"")

    /** Replaces the value of password and token JSON fields with ***. */
    private fun redactSecrets(message: String): String =
        secretJsonFields.replace(message) { match -> "\"${match.groupValues[1]}\":\"***\"" }

    /** Creates a Retrofit API interface, e.g. ApiClient.create<AuthApi>(). */
    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}
