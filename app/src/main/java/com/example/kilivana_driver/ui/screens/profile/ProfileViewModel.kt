package com.example.kilivana_driver.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.Driver
import com.example.kilivana_driver.data.model.DriverImage
import com.example.kilivana_driver.data.network.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val images: List<DriverImage> = emptyList(),
    val imagesLoading: Boolean = false,
    val imagesError: String? = null
) {
    /** The image the header avatar should show, or null to fall back to initials. */
    val primaryImage: DriverImage?
        get() = images.firstOrNull { it.isPrimary } ?: images.minByOrNull { it.sortOrder }
}

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

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val profileRepository = ProfileRepository()

    // TODO: source this from the login/session response instead of a constant
    private val userId: Long = 42L

    init {
        loadImages()
    }

    fun loadImages() {
        if (_uiState.value.imagesLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(imagesLoading = true, imagesError = null) }
            val result = profileRepository.getDriverImages(userId)
            _uiState.update {
                it.copy(
                    imagesLoading = false,
                    images = result.getOrDefault(emptyList()),
                    imagesError = result.exceptionOrNull()?.message
                )
            }
        }
    }
}
