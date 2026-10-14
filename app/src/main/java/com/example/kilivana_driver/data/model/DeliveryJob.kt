package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Delivery job as returned by the Kilivana logistics API:
 *
 * - GET /api/v1/logistics/jobs          (list, data is an array)
 * - GET /api/v1/logistics/jobs/{id}     (single, data is an object)
 *
 * This is the backend-facing serializable model: every field name and type
 * matches the JSON contract exactly. It is converted to the UI/domain [Job]
 * by [toDomain], because [Job] carries display-ready strings (formatted times,
 * "Xh Ym" durations, "KSh" payouts) that the API does not.
 *
 * The OTP fields are kept for completeness but the hash is never exposed to
 * the driver — the plain OTP lives only on the buyer's device. OTP
 * verification is an OTP-checked API call, not a local string comparison.
 */
@Serializable
data class DeliveryJob(
    val id: Long = 0L,
    val orderId: Long = 0L,
    val pickupAddress: String? = null,
    val destinationAddress: String? = null,
    val pickupLatitude: Double = 0.0,
    val pickupLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0,
    val cargoDescription: String? = null,
    /**
     * Nullable because the backend sends `null` when no cargo quantity is
     * specified. Declaring it non-nullable with a default would throw on an
     * explicit `null`: kotlinx.serialization only applies a default when the
     * key is *absent*, not when it's present-but-null. See DriverImage.assetId
     * for the same documented rationale.
     */
    val quantity: Int? = null,
    val payoutAmount: Double = 0.0,
    val scheduledPickupAt: String? = null,
    val scheduledDropoffAt: String? = null,
    val distanceKm: Double = 0.0,
    val estimatedMinutes: Int = 0,
    val deliveryOtpHash: String? = null,
    val deliveryOtpExpiresAt: String? = null,
    val deliveryOtpVerified: Boolean? = null,
    val deliveryOtpAttempts: Int = 0,
    val deliveryOtpLockedUntil: String? = null,
    val status: String? = null,
    val driverId: Long = 0L,
    /** Null when the job has not been cancelled; present only on cancellations. */
    val cancellationReason: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    /**
     * Maps the backend job to the UI/domain [Job] model.
     *
     * Backend statuses are mapped to the three tab statuses the UI supports:
     * - pending_assignment / assigned / accepted → AVAILABLE / ACCEPTED
     * - completed / delivered → COMPLETED
     * - anything else → AVAILABLE (safe default; the UI filters by tab)
     *
     * Nullable fields from the backend are coalesced to blank strings here
     * so the UI [Job] model stays non-nullable.
     */
    fun toDomain(): Job = Job(
        id = id.toString(),
        pickupLocation = pickupAddress ?: "",
        pickupPlace = pickupAddress ?: "",
        pickupTime = formatTime(scheduledPickupAt),
        pickupLat = pickupLatitude,
        pickupLon = pickupLongitude,
        dropoffLocation = destinationAddress ?: "",
        dropoffPlace = destinationAddress ?: "",
        dropoffTime = formatTime(scheduledDropoffAt),
        dropoffLat = destinationLatitude,
        dropoffLon = destinationLongitude,
        cargo = cargoDescription ?: "",
        quantity = if (quantity != null && quantity > 0) "$quantity kg" else "",
        distanceKm = distanceKm.roundToInt(),
        estimatedTime = formatMinutes(estimatedMinutes),
        payoutKsh = payoutAmount.toInt(),
        customer = "",
        deliveryOtp = "",
        status = mapStatus(status),
        isNew = false
    )

    private fun mapStatus(backendStatus: String?): JobStatus = when (backendStatus?.lowercase()) {
        "pending_assignment", "available", "unassigned" -> JobStatus.AVAILABLE
        "assigned", "accepted", "picked_up", "in_transit", "in_progress",
        "en_route_to_buyer", "en_route_to_dropoff" -> JobStatus.ACCEPTED
        "completed", "delivered" -> JobStatus.COMPLETED
        else -> JobStatus.AVAILABLE
    }

    private fun formatTime(isoTimestamp: String?): String {
        if (isoTimestamp.isNullOrBlank()) return ""
        return runCatching {
            val instant = Instant.parse(isoTimestamp)
            val zoned = instant.atZone(ZoneId.systemDefault())
            val today = ZonedDateTime.now().toLocalDate()
            val date = zoned.toLocalDate()
            val time = zoned.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
            val dayPrefix = when {
                date == today -> "Today"
                date == today.plusDays(1) -> "Tomorrow"
                else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            }
            "$dayPrefix $time"
        }.getOrDefault(isoTimestamp)
    }

    private fun formatMinutes(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    private fun Double.roundToInt(): Int = this.toInt()
}
