package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable

/**
 * One point in a delivery job's tracking history.
 *
 * Returned by GET /api/v1/logistics/jobs/{jobId}/tracking. The backend stores
 * every GPS fix the driver uploaded while the job was active; this model is
 * that history. [speed], [bearing] and [accuracy] are nullable because they
 * come from `LocationUpdate` which allows null on those fields.
 */
@Serializable
data class TrackingHistoryPoint(
    val timestamp: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** Speed in metres per second, or null when the fix had no speed. */
    val speed: Double? = null,
    /** Heading in degrees (0-360), or null when unavailable. */
    val bearing: Double? = null,
    /** Horizontal accuracy in metres, or null when unavailable. */
    val accuracy: Double? = null,
    /** The trip stage at the time of this fix (e.g. "HEADING_TO_PICKUP"). */
    val status: String = ""
)
