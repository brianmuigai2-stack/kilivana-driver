package com.example.kilivana_driver.data.model

data class Driver(
    val name: String,
    val driverId: String,
    val rating: Double,
    val deliveriesCompleted: Int,
    val vehiclePlate: String,
    val vehicleType: String,
    val paymentMethod: String
)
