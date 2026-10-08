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
 *
 * The backend is currently exposed through a free ngrok tunnel while it's
 * in development. Free ngrok tunnels show an interstitial "you're about to
 * visit..." warning page to any request that doesn't explicitly say it
 * isn't a browser — [ngrokHeaderInterceptor] adds that header to every
 * request made through this client. Without it, every API call gets
 * ngrok's HTML warning page back instead of real JSON, which looks like a
 * confusing parse error rather than what it actually is.
 *
 * The ngrok tunnel is unreliable — it drops connections often enough that a
 * call can fail with a bare [java.io.IOException] while the tunnel is
 * restarting. [HostFailoverInterceptor] retries those failures against the
 * Render deployment in [FALLBACK_BASE_URL], and remembers whichever host
 * answered last so later calls skip the dead one.
 */
object ApiClient {

    const val BASE_URL = "https://either-juvenile-progeny.ngrok-free.dev/"
    const val FALLBACK_BASE_URL = "https://kilivana-backend-a44w.onrender.com/"
    const val LOCAL_BASE_URL = "http://10.0.2.2:8080/"
    const val LAN_BASE_URL = "http://192.168.137.112:8080/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Retries a request against a list of fallback hosts when the primary ngrok
     * host fails at the transport level or returns an HTML error page.
     *
     * Only [java.io.IOException] or an HTML response body triggers a retry.
     * An HTTP error status (401, 404, etc.) from a real backend is returned to
     * the caller as usual — retrying it would only double the round trip.
     *
     * Remembers the last host that responded so a dead tunnel costs one slow
     * call rather than one slow call per request.
     */
    private class HostFailoverInterceptor(
        private val fallbackUrls: List<String>,
    ) : Interceptor {

        @Volatile
        private var preferredHost: String? = null

        private val fallbacks = fallbackUrls.map { it.toHttpUrl() }

        private fun isHtml(response: Response): Boolean {
            val type = response.header("Content-Type").orEmpty()
            return type.contains("text/html", ignoreCase = true)
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val hosts = buildList {
                preferredHost?.let { add(it) }
                add(request.url.host)
                fallbacks.forEach { add(it.host) }
            }.distinct()

            var lastFailure: IOException? = null

            for (host in hosts) {
                val fallback = fallbacks.find { it.host == host } ?: fallbacks.first()
                val target = if (host == request.url.host) {
                    request
                } else {
                    request.newBuilder()
                        .url(
                            request.url.newBuilder()
                                .scheme(fallback.scheme)
                                .host(host)
                                .port(fallback.port)
                                .build()
                        )
                        .build()
                }

                try {
                    val response = chain.proceed(target)

                    // A dead ngrok tunnel answers with HTTP 404 and an HTML
                    // error page, so it looks like a real server response.
                    // The API only ever speaks JSON, so treat any HTML body as
                    // a transport failure and move on to the next host instead
                    // of surfacing a misleading 404 to the user.
                    if (isHtml(response)) {
                        response.close()
                        lastFailure = IOException("Tunnel at $host is offline")
                        continue
                    }

                    // Any real backend response proves this host is alive, even
                    // a 401 — otherwise every later call would keep paying the
                    // dead tunnel's timeout first.
                    preferredHost = host
                    return response
                } catch (e: IOException) {
                    lastFailure = e
                }
            }

            throw lastFailure ?: IOException("All backend hosts failed")
        }
    }

    private val ngrokHeaderInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .apply {
                // Only the tunnel host understands this header; sending it to
                // the Render deployment would be noise.
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
            // Outermost so a retry re-enters the whole chain, including the
            // bearer token and debug logging.
            .addInterceptor(HostFailoverInterceptor(listOf(FALLBACK_BASE_URL, LOCAL_BASE_URL)))
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
                // Secrets are masked first: the Authorization header, and the
                // password / token fields inside JSON bodies, so a copied
                // Logcat never leaks a login.
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
.addInterceptor(HostFailoverInterceptor(listOf(FALLBACK_BASE_URL, LOCAL_BASE_URL, LAN_BASE_URL)))
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

    private val secretJsonFields = Regex("\"(password|accessToken|refreshToken)\"\\s*:\\s*\"[^\"]*\"")

    /** Replaces the value of password and token JSON fields with ***. */
    private fun redactSecrets(message: String): String =
        secretJsonFields.replace(message) { match -> "\"${match.groupValues[1]}\":\"***\"" }

    /** Creates a Retrofit API interface, e.g. ApiClient.create<AuthApi>(). */
    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}
