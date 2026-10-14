package com.example.kilivana_driver.ui.screens.map

import com.mapbox.geojson.Point

/**
 * A simple GeoPoint replacement.
 * Provides the same API surface as osmdroid's GeoPoint for easy migration.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {

    /** Converts this coordinate to a Mapbox Point using longitude before latitude. */
    fun toMapboxPoint(): Point = Point.fromLngLat(longitude, latitude)

    companion object {
        /** Creates a GeoPoint from longitude followed by latitude, both in degrees. */
        fun fromLngLat(longitude: Double, latitude: Double): GeoPoint {
            return GeoPoint(latitude, longitude)
        }
    }

    /** Returns the great-circle distance to the other point in meters using the haversine formula. */
    fun distanceToAsDouble(other: GeoPoint): Double {
        // Haversine formula for distance between two points in meters
        val R = 6371000.0 // Earth radius in meters
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    /** Returns the initial bearing to the other point in degrees clockwise from north, in the range [0, 360). */
    fun bearingTo(other: GeoPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLon = Math.toRadians(other.longitude - longitude)

        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)

        val bearing = Math.toDegrees(Math.atan2(y, x))
        return if (bearing < 0) bearing + 360 else bearing
    }
}