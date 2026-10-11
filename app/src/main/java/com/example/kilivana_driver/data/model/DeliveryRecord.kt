package com.example.kilivana_driver.data.model

/** How a finished delivery ended up. */
enum class DeliveryOutcome { COMPLETED, CANCELLED }

/**
 * One row of the delivery history screen.
 *
 * Still sample data (see `sampleDeliveryHistory()`) — the API has no
 * history endpoint wired up yet. [dateLabel] and [earningsKsh] are
 * display-ready strings/values, not raw timestamps.
 */
data class DeliveryRecord(
    val id: String,
    val cargo: String,
    val dateLabel: String,
    val earningsKsh: Int,
    val outcome: DeliveryOutcome
)
