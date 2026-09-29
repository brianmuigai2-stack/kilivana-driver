package com.example.kilivana_driver.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Talks to the backend's plain health-check endpoint (GET /), which has no
 * fixed JSON shape in the Swagger docs — we only care whether the server
 * answers. Used to prove the network setup and the ngrok tunnel actually
 * work before wiring up any real, driver-specific endpoints.
 */
class HealthRepository(
    private val client: OkHttpClient = ApiClient.okHttpClient,
    private val baseUrl: String = ApiClient.BASE_URL
) {
    suspend fun checkConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(baseUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.take(150)?.trim().orEmpty()
                    Result.success(body.ifBlank { "OK (HTTP ${response.code})" })
                } else {
                    Result.failure(Exception("Server responded with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
