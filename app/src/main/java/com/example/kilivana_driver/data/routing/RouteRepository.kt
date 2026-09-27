package com.example.kilivana_driver.data.routing

import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RouteResult(
    val points: List<GeoPoint>,
    /** True if this is a real road-following route; false if it's a fallback straight line. */
    val isRealRoute: Boolean
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

    private const val BASE_URL = "https://router.project-osrm.org/route/v1/driving"

    fun fetchRoute(from: GeoPoint, to: GeoPoint): RouteResult {
        return try {
            val url = URL(
                String.format(
                    Locale.US,
                    "%s/%f,%f;%f,%f?overview=full&geometries=geojson",
                    BASE_URL, from.longitude, from.latitude, to.longitude, to.latitude
                )
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val coords = JSONObject(body)
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
            // Offline, timeout, or the demo server is busy: fall back to a
            // straight line so the screen still shows a usable route. The
            // caller shows a note to the driver when this happens.
            RouteResult(points = listOf(from, to), isRealRoute = false)
        }
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
