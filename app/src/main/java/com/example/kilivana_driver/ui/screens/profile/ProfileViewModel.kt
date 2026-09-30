package com.example.kilivana_driver.ui.screens.profile

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.Driver
import com.example.kilivana_driver.data.model.DriverImage
import com.example.kilivana_driver.data.network.ProfileRepository
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class ProfileUiState(
    val images: List<DriverImage> = emptyList(),
    val imagesLoading: Boolean = false,
    val imagesError: String? = null,
    val isUploading: Boolean = false,
    val uploadError: String? = null,
    val uploadSuccessMessage: String? = null
) {
    /** The image the header avatar should show, or null to fall back to initials. */
    val primaryImage: DriverImage?
        get() = images.firstOrNull { it.isPrimary } ?: images.minByOrNull { it.sortOrder }
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val contentResolver: ContentResolver get() = getApplication<Application>().contentResolver

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

    /**
     * Uploads the picture the driver picked, then reloads the list so the new
     * image comes straight from the server rather than being faked into local
     * state — this way what you see is what the backend actually stored.
     */
    fun uploadImage(uri: Uri) {
        if (_uiState.value.isUploading) return
        val userId = SessionStore.userId
        if (userId == null) {
            _uiState.update { it.copy(uploadError = "Please log in before uploading a photo") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isUploading = true, uploadError = null, uploadSuccessMessage = null)
            }

            val file = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val temp = File.createTempFile("profile_upload_", ".jpg")
                        temp.outputStream().use { out -> stream.copyTo(out) }
                        temp
                    }
                }.getOrNull()
            }
            if (file == null) {
                _uiState.update {
                    it.copy(isUploading = false, uploadError = "Couldn't read that image")
                }
                return@launch
            }

            val result = profileRepository.uploadDriverImage(
                userId = userId,
                file = file,
                isPrimary = _uiState.value.images.isEmpty()
            )
            file.delete()

            _uiState.update {
                it.copy(
                    isUploading = false,
                    uploadError = result.exceptionOrNull()?.message,
                    uploadSuccessMessage = if (result.isSuccess) "Photo uploaded" else null
                )
            }
            if (result.isSuccess) loadImages()
        }
    }

    fun dismissUploadMessage() {
        _uiState.update { it.copy(uploadError = null, uploadSuccessMessage = null) }
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
