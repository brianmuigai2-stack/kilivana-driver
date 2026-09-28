package com.example.kilivana_driver.data.model

enum class DeliveryOutcome { COMPLETED, CANCELLED }

data class DeliveryRecord(
    val id: String,
    val cargo: String,
    val dateLabel: String,
    val earningsKsh: Int,
    val outcome: DeliveryOutcome
)
