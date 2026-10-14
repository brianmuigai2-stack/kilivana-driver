package com.example.kilivana_driver.data.model

/**
 * Lifecycle of a delivery job, as shown by the Jobs screen tabs.
 *
 * AVAILABLE → ACCEPTED → COMPLETED. The label is the exact text the
 * tab chips show, so it is part of the UI contract.
 */
enum class JobStatus(val label: String) {
    AVAILABLE("Available"),
    ACCEPTED("Accepted"),
    COMPLETED("Completed")
}

/**
 * One delivery job: pickup at a farm, drop-off at a market or depot.
 *
 * Still sample data (see [JobsViewModel]) — the coordinates, times and
 * payout all come from the backend once the jobs endpoint is wired up.
 *
 * [pickupLat]/[pickupLon] and [dropoffLat]/[dropoffLon] are what the
 * En Route screen draws the Mapbox Directions route between.
 *
 * [deliveryOtp] is the buyer's delivery confirmation code. It is
 * currently a made-up sample value; in production the backend generates
 * it and the driver types it in to confirm the delivery.
 *
 * [isNew] drives the amber "NEW" badge on job cards until the job is
 * opened or accepted.
 */
data class Job(
    val id: String,
    val pickupLocation: String,
    val pickupPlace: String,
    val pickupTime: String,
    val pickupLat: Double,
    val pickupLon: Double,
    val dropoffLocation: String,
    val dropoffPlace: String,
    val dropoffTime: String,
    val dropoffLat: Double,
    val dropoffLon: Double,
    val cargo: String,
    val quantity: String,
    val distanceKm: Int,
    val estimatedTime: String,
    val payoutKsh: Int,
    val customer: String,
    val deliveryOtp: String,
    val status: JobStatus,
    val isNew: Boolean = false
)
