package com.example.kilivana_driver.data.model

/** How a finished delivery ended up. */
enum class DeliveryOutcome { COMPLETED, CANCELLED }

/**
 * One row of the delivery history screen.
 *
 * Display-ready row derived from the signed-in driver's delivery jobs.
 * [dateLabel] and [earningsKsh] are display values, not raw timestamps.
 */
data class DeliveryRecord(
    val id: String,
    val cargo: String,
    val dateLabel: String,
    val earningsKsh: Int,
    val outcome: DeliveryOutcome
)
