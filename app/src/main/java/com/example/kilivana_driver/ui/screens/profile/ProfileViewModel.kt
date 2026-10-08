package com.example.kilivana_driver.ui.screens.profile

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.Driver
import com.example.kilivana_driver.data.model.DriverImage
import com.example.kilivana_driver.data.model.DriverProfile
import com.example.kilivana_driver.data.model.DriverProfileRequest
import com.example.kilivana_driver.data.network.ProfileRepository
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class ProfileUiState(
    val profile: DriverProfile? = null,
    val images: List<DriverImage> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isDeletingImage: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    /** True when the backend has no driver profile for this user yet. */
    val needsProfile: Boolean = false
) {
    val primaryImage: DriverImage?
        get() = images.firstOrNull { it.isPrimary } ?: images.minByOrNull { it.sortOrder }
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val contentResolver: ContentResolver get() = getApplication<Application>().contentResolver

    private val profileRepository = ProfileRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * The driver shown in the header, derived from whoever is signed in.
     *
     * Combines the session and the loaded profile rather than reading
     * [_uiState].value inside a map: Kotlin initialises properties in
     * declaration order, so a flow that touched _uiState declared after it
     * would run before that field existed and crash on launch.
     *
     * [Driver.driverId] is the code the UI shows (e.g. "DRV-0009"), derived from
     * the backend's numeric id because the API exposes no display code of its
     * own. The vehicle fields come from the driver profile, and the rating and
     * delivery count have no endpoint behind them yet.
     */
    val driver: StateFlow<Driver> = combine(
        SessionStore.currentUser,
        _uiState.map { it.profile }
    ) { user, profile ->
        Driver(
            name = user?.name.orEmpty(),
            driverId = user?.let { "DRV-%04d".format(Locale.US, it.id) }.orEmpty(),
            rating = 0.0,
            deliveriesCompleted = 0,
            vehiclePlate = profile?.vehicleNumber.orEmpty(),
            vehicleType = profile?.vehicleType.orEmpty(),
            paymentMethod = profile?.availabilityStatus.orEmpty(),
            photoUrl = profile?.primaryImage?.url.orEmpty()
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Driver("", "", 0.0, 0, "", "", "")
        )

    init {
        // Created at activity launch, which is *before* the driver logs in, so
        // an eager load would always see a null session. Watching the session
        // loads once a login populates it and clears again on logout.
        viewModelScope.launch {
            SessionStore.currentUser.collect { user ->
                if (user != null) refresh() else _uiState.update { ProfileUiState() }
            }
        }
    }

    /** Loads the profile and its images in one pass. */
    fun refresh() {
        if (_uiState.value.isLoading) return
        val userId = SessionStore.userId
        if (userId == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = profileRepository.getProfile(userId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // A missing profile is a state the UI can act on, not an
                        // error to shout about, so it gets its own flag.
                        needsProfile = isProfileMissing(result.exceptionOrNull()?.message),
                        errorMessage = if (isProfileMissing(result.exceptionOrNull()?.message)) null
                        else result.exceptionOrNull()?.message
                    )
                }
                return@launch
            }

            val profile = result.getOrThrow()
            val images = profileRepository.getImages(userId).getOrDefault(profile.images.orEmpty())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = profile,
                    images = images,
                    needsProfile = false,
                    errorMessage = null
                )
            }
        }
    }

    /**
     * Uploads the picture the driver picked. The response carries the server's
     * full image list, so what renders afterwards is what the backend stored.
     */
    fun uploadImage(uri: Uri) {
        if (_uiState.value.isUploading) return
        val userId = SessionStore.userId
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Please log in before uploading a photo") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isUploading = true, errorMessage = null, successMessage = null)
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
                    it.copy(isUploading = false, errorMessage = "Couldn't read that image")
                }
                return@launch
            }

            // A profile photo is a single visible picture, so each upload
            // replaces the one before it: the new image goes up as primary
            // (the backend demotes the old primary) and the old primary is
            // then deleted. Without this the backend just appends, leaving
            // the previous photo on screen.
            val previousPrimaryId = _uiState.value.primaryImage?.id

            val result = profileRepository.uploadImage(
                userId = userId,
                file = file,
                isPrimary = true
            )
            file.delete()

            if (result.isSuccess && previousPrimaryId != null) {
                profileRepository.deleteImage(userId, previousPrimaryId)
            }

            _uiState.update { state ->
                val newImages = result.getOrNull() ?: state.images
                state.copy(
                    isUploading = false,
                    images = if (result.isSuccess && previousPrimaryId != null) {
                        newImages.filterNot { it.id == previousPrimaryId }
                    } else newImages,
                    errorMessage = result.exceptionOrNull()?.message,
                    successMessage = if (result.isSuccess) "Photo uploaded" else null
                )
            }
        }
    }

    fun deleteImage(imageId: Long) {
        val userId = SessionStore.userId ?: return
        if (_uiState.value.isDeletingImage) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isDeletingImage = true, errorMessage = null, successMessage = null)
            }
            val result = profileRepository.deleteImage(userId, imageId)
            _uiState.update {
                it.copy(
                    isDeletingImage = false,
                    // Only drop it locally once the server confirms the delete.
                    images = if (result.isSuccess) it.images.filterNot { i -> i.id == imageId }
                    else it.images,
                    successMessage = if (result.isSuccess) "Photo removed" else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    /**
     * Deletes whatever the driver is currently showing as their primary photo.
     *
     * The "Delete photo" button on the profile screen targets the avatar itself,
     * not the photo grid, so it resolves the primary image at call time and
     * routes through the same [deleteImage] path the grid uses.
     */
    fun deletePrimaryPhoto() {
        val primaryId = _uiState.value.primaryImage?.id ?: return
        deleteImage(primaryId)
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun isProfileMissing(message: String?): Boolean =
        message?.contains("profile not found", ignoreCase = true) == true ||
            message?.contains("RESOURCE_NOT_FOUND", ignoreCase = true) == true
}
