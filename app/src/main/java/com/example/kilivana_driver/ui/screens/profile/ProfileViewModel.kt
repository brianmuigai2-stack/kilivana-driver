package com.example.kilivana_driver.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.example.kilivana_driver.data.model.Driver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {

    // TODO: replace sample data with the logged-in driver from the API
    private val _driver = MutableStateFlow(
        Driver(
            name = "James Mwangi",
            driverId = "DRI-0042",
            rating = 4.8,
            deliveriesCompleted = 12,
            vehiclePlate = "KDB 432A",
            vehicleType = "Truck",
            paymentMethod = "M-Pesa registered"
        )
    )
    val driver: StateFlow<Driver> = _driver.asStateFlow()
}
