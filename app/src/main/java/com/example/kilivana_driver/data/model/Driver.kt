package com.example.kilivana_driver.data.model

/**
 * The driver shown in headers across the app. Derived from whoever is signed
 * in, with the vehicle fields coming from the driver profile.
 *
 * [photoUrl] is the primary profile image URL, or blank when none has been
 * uploaded — the UI falls back to initials in that case.
 */
data class Driver(
    val name: String,
    val driverId: String,
    val rating: Double,
    val deliveriesCompleted: Int,
    val vehiclePlate: String,
    val vehicleType: String,
    val paymentMethod: String,
    val photoUrl: String = ""
)
