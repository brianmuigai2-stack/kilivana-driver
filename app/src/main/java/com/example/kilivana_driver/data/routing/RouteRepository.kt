package com.example.kilivana_driver.data.routing

import android.util.Log
import com.example.kilivana_driver.BuildConfig
import com.example.kilivana_driver.ui.screens.map.GeoPoint
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RouteResult(
    val points: List<GeoPoint>,
    /** True if this is a real road-following route; false if it's a fallback straight line. */
    val isRealRoute: Boolean,
    /** Why the real route failed (null when it succeeded). Also written to Logcat. */
    val failureReason: String? = null
)

/**
 * Fetches a road-following route from the Mapbox Directions API.
 *
 * The OSRM public demo server at router.project-osrm.org is frequently
 * unreachable from mobile networks and emulators (TCP-level timeouts on port
 * 443), so Mapbox Directions is used instead — the app already ships with a
 * Mapbox access token for map tiles, and api.mapbox.com is reachable from the
 * same networks that reach the tile servers.
 *
 * Call this from a background thread (e.g. Dispatchers.IO) — it blocks.
 */
object RouteRepository {

    private const val TAG = "RouteRepository"
    private const val BASE_URL = "https://api.mapbox.com/directions/v5/mapbox/driving"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Fetches a Mapbox driving route synchronously; returns a straight line with a reason on failure. Call off the main thread. */
    fun fetchRoute(from: GeoPoint, to: GeoPoint): RouteResult {
        return try {
            val accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
            if (accessToken.isBlank()) {
                return fallback(from, to, "Mapbox access token not configured")
            }

            val url = String.format(
                Locale.US,
                "%s/%f,%f;%f,%f?geometries=geojson&overview=full&access_token=%s",
                BASE_URL, from.longitude, from.latitude, to.longitude, to.latitude,
                accessToken
            )
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "KilivanaDriver/1.0")
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return fallback(from, to, "HTTP ${response.code}")
            }

            val json = JSONObject(body)
            val code = json.optString("code")
            if (code != "Ok") {
                return fallback(from, to, "Mapbox code '$code'")
            }

            val coords = json
                .getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")

            val points = (0 until coords.length()).map { i ->
                val pair = coords.getJSONArray(i)
                GeoPoint(pair.getDouble(1), pair.getDouble(0))
            }
            RouteResult(points = points, isRealRoute = true)
        } catch (e: Exception) {
            fallback(from, to, "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun fallback(from: GeoPoint, to: GeoPoint, reason: String): RouteResult {
        Log.w(
            TAG,
            "Route failed (${from.latitude},${from.longitude} -> ${to.latitude},${to.longitude}): $reason"
        )
        return RouteResult(points = listOf(from, to), isRealRoute = false, failureReason = reason)
    }

    /** Rough length of a route in km, by summing point-to-point distances. */
    fun routeLengthKm(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var meters = 0.0
        for (i in 0 until points.size - 1) {
            meters += points[i].distanceToAsDouble(points[i + 1])
        }
        return meters / 1000.0
    }
}
