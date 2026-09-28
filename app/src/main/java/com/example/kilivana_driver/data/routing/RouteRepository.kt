package com.example.kilivana_driver.data.routing

import android.util.Log
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RouteResult(
    val points: List<GeoPoint>,
    /** True if this is a real road-following route; false if it's a fallback straight line. */
    val isRealRoute: Boolean,
    /** Why the real route failed (null when it succeeded). Also written to Logcat. */
    val failureReason: String? = null
)

/**
 * Fetches a road-following route from OSRM's free public demo server.
 *
 * NOTE: router.project-osrm.org is meant for light development/demo use,
 * not production traffic. For a live product, point BASE_URL at a paid
 * or self-hosted OSRM/GraphHopper instance instead.
 *
 * Call this from a background thread (e.g. Dispatchers.IO) — it blocks.
 */
object RouteRepository {

    private const val TAG = "RouteRepository"
    private const val BASE_URL = "https://router.project-osrm.org/route/v1/driving"

    // The public OSRM server, like OSM's tile servers, expects clients to
    // identify themselves. Keep this in step with the tile User-Agent.
    private const val USER_AGENT = "KilivanaDriver/1.0 (Kilivana agricultural logistics app)"

    fun fetchRoute(from: GeoPoint, to: GeoPoint): RouteResult {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(
                String.format(
                    Locale.US,
                    "%s/%f,%f;%f,%f?overview=full&geometries=geojson",
                    BASE_URL, from.longitude, from.latitude, to.longitude, to.latitude
                )
            )
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            val json = JSONObject(body)
            val code = json.optString("code")
            if (status !in 200..299 || code != "Ok") {
                return fallback(from, to, "HTTP $status, OSRM code '$code'")
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
        } finally {
            connection?.disconnect()
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
