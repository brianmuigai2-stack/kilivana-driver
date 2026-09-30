package com.example.kilivana_driver.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.Driver
import com.example.kilivana_driver.data.model.DriverImage
import com.example.kilivana_driver.data.network.ProfileRepository
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

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

    private val profileRepository = ProfileRepository()

    /**
     * The driver shown in the header, derived from whoever is signed in.
     *
     * [Driver.driverId] is the profile code the UI shows (e.g. "DRV-0042"),
     * derived from the backend's numeric id since the API returns no such
     * code yet. The rating/delivery counts still have no endpoint behind them
     * and stay at their placeholder values until one exists.
     */
    val driver: StateFlow<Driver> = SessionStore.currentUser
        .map { user ->
            Driver(
                name = user?.name.orEmpty(),
                driverId = user?.let { "DRV-%04d".format(Locale.US, it.id) }.orEmpty(),
                rating = 0.0,
                deliveriesCompleted = 0,
                vehiclePlate = "",
                vehicleType = "",
                paymentMethod = ""
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Driver("", "", 0.0, 0, "", "", "")
        )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // This ViewModel is created at activity launch, which is *before* the
        // driver has logged in, so an eager load in init would always see a
        // null session. Watching the session means the images load as soon as
        // a login actually populates it, and clear again on logout.
        viewModelScope.launch {
            SessionStore.currentUser.collect { user ->
                if (user != null) {
                    loadImages()
                } else {
                    _uiState.update { ProfileUiState() }
                }
            }
        }
    }

    fun loadImages() {
        if (_uiState.value.imagesLoading) return
        val userId = SessionStore.userId
        if (userId == null) {
            _uiState.update { it.copy(imagesLoading = false, imagesError = null) }
            return
        }
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
