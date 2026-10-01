package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single shared entry point for talking to the Kilivana backend.
 *
 * The backend is currently exposed through a free ngrok tunnel while it's
 * in development. Free ngrok tunnels show an interstitial "you're about to
 * visit..." warning page to any request that doesn't explicitly say it
 * isn't a browser — [ngrokHeaderInterceptor] adds that header to every
 * request made through this client. Without it, every API call gets
 * ngrok's HTML warning page back instead of real JSON, which looks like a
 * confusing parse error rather than what it actually is.
 *
 * TODO: replace BASE_URL with the real production URL once the backend
 * has a permanent home — ngrok URLs change every time the tunnel restarts.
 */
object ApiClient {

    const val BASE_URL = "https://either-juvenile-progeny.ngrok-free.dev/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val ngrokHeaderInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("ngrok-skip-browser-warning", "true")
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
            // Bearer token must be added after the ngrok header, and applies to
            // every protected call regardless of whether a token exists yet.
            .addInterceptor(AuthInterceptor())
            .apply {
                // Only wire the 401 refresh once we can read a refresh token;
                // without preferences it would loop on 401 with no way out.
                sessionPreferences?.let { prefs ->
                    authenticator(TokenAuthenticator(refreshApi = refreshApi, preferences = prefs))
                }
                // Full request/response bodies in Logcat, debug builds only.
                // Headers are included so the Authorization value is visible
                // while debugging — never enable this in a release build.
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
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

    /** Creates a Retrofit API interface, e.g. ApiClient.create<AuthApi>(). */
    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}
