package com.example.kilivana_driver.data.model

enum class JobStatus(val label: String) {
    AVAILABLE("Available"),
    ACCEPTED("Accepted"),
    COMPLETED("Completed")
}

data class Job(
    val id: String,
    val pickupLocation: String,
    val pickupPlace: String,
    val pickupTime: String,
    val dropoffLocation: String,
    val dropoffPlace: String,
    val dropoffTime: String,
    val cargo: String,
    val quantity: String,
    val distanceKm: Int,
    val estimatedTime: String,
    val payoutKsh: Int,
    val customer: String,
    val status: JobStatus,
    val isNew: Boolean = false
)
