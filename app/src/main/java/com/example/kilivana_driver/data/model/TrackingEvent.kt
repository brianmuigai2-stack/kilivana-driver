package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackingEvent(
    val id: Long = 0L,
    val jobId: Long = 0L,
    val driverId: Long = 0L,
    val status: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val note: String? = null,
    val createdAt: String = ""
)
