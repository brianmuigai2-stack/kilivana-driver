package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Payload for a single GPS location update sent to the backend.
 *
 * The field names match the backend's expected JSON contract.
 * Only non-null values are included in the request.
 */
@Serializable
data class LocationUpdate(
    /** The delivery/trip ID this location belongs to. */
    val tripId: String,
    /** Latitude in degrees (WGS-84). */
    val latitude: Double,
    /** Longitude in degrees (WGS-84). */
    val longitude: Double,
    /** Heading in degrees (0-360), clockwise from true north. Null when unavailable. */
    val bearing: Float? = null,
    /** Speed in metres per second. Null when unavailable. */
    val speed: Float? = null,
    /** Horizontal accuracy in metres. Null when unavailable. */
    val accuracy: Float? = null,
    /** Timestamp of the fix in UTC milliseconds since epoch. */
    val timestamp: Long = Instant.now().toEpochMilli()
)

/**
 * Configuration for the tracking service.
 * Values can be overridden via BuildConfig or remote config in the future.
 */
data class TrackingConfig(
    /** Minimum interval between location updates sent to the backend (ms). */
    val updateIntervalMs: Long = 10_000L,
    /** Minimum distance the driver must move before sending a new update (metres). */
    val minDisplacementMeters: Float = 15f,
    /** Minimum speed (m/s) required to trust GPS bearing. */
    val minSpeedForBearing: Float = 1.5f,
    /** Maximum time to wait for a location fix before considering it stale (ms). */
    val maxLocationAgeMs: Long = 30_000L,
    /** Maximum number of failed uploads to queue before dropping oldest. */
    val maxOfflineQueueSize: Int = 50
) {
    companion object {
        /** Default configuration for production use. */
        val DEFAULT = TrackingConfig()
    }
}

/**
 * Current state of an active trip/delivery from the driver's perspective.
 */
enum class TripStage(val label: String) {
    /** Driver is heading to the farmer for pickup. */
    HEADING_TO_PICKUP("Heading to pickup"),
    /** Cargo has been picked up; driver is en route to the buyer. */
    IN_TRANSIT("In transit"),
    /** Delivery completed and confirmed via OTP. */
    DELIVERED("Delivered"),
    /** Trip was cancelled before completion. */
    CANCELLED("Cancelled")
}

/**
 * Result of a location upload attempt.
 */
sealed interface UploadResult {
    data class Success(val response: LocationUploadResponse) : UploadResult
    data class Failure(val throwable: Throwable, val willRetry: Boolean) : UploadResult
}

/**
 * Backend response for a successful location upload.
 * Extend if the backend returns useful fields (e.g., server timestamp, sequence number).
 */
@Serializable
data class LocationUploadResponse(
    val success: Boolean? = true,
    val message: String? = null,
    val serverTimestamp: Long? = null
)

/**
 * Trip status received from the backend (e.g., via WebSocket or polling).
 */
@Serializable
data class TripStatus(
    val tripId: String,
    val status: String, // e.g., "ACTIVE", "COMPLETED", "CANCELLED"
    val driverId: Long,
    val updatedAt: String,
    /** Nullable because not all status updates include location. */
    val lastKnownLocation: LocationUpdate? = null
)

/**
 * Request to start tracking for a trip (if the backend requires explicit start).
 */
@Serializable
data class StartTrackingRequest(
    val tripId: String,
    val driverId: Long
)

/**
 * Request to stop tracking for a trip.
 */
@Serializable
data class StopTrackingRequest(
    val tripId: String,
    val driverId: Long,
    val reason: String // "COMPLETED", "CANCELLED", "DRIVER_ENDED"
)