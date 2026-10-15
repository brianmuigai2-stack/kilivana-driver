package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable

/**
 * Location fix returned by tracking endpoints.
 */
@Serializable
data class TrackingLocationFix(
    val tripId: Long? = null,
    val orderId: Long? = null,
    val driverId: Long? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Double? = null,
    val bearing: Double? = null,
    val accuracyMetres: Double? = null,
    val status: String? = null,
    val clientTimestamp: String? = null,
    val serverTimestamp: String? = null
)

/**
 * Request body for reporting driver location.
 */
@Serializable
data class ReportLocationRequest(
    val tripId: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double? = null,
    val bearing: Double? = null,
    val accuracyMetres: Double? = null,
    val clientTimestamp: String? = null
)

/**
 * Response data for location report.
 */
@Serializable
data class ReportLocationResponseData(
    val tripId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val serverTimestamp: String? = null
)
