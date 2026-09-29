package com.example.kilivana_driver.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.network.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppLanguage(val displayName: String) {
    ENGLISH("English"),
    SWAHILI("Kiswahili")
}

data class SettingsUiState(
    val pushNotificationsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val apiTestInProgress: Boolean = false,
    val apiTestResult: String? = null,
    val apiTestSucceeded: Boolean = false
)

class SettingsViewModel : ViewModel() {

    // TODO: persist these with DataStore instead of losing them on app restart
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val healthRepository = HealthRepository()

    fun onPushNotificationsChange(enabled: Boolean) {
        _uiState.update { it.copy(pushNotificationsEnabled = enabled) }
    }

    fun onNotificationSoundChange(enabled: Boolean) {
        _uiState.update { it.copy(notificationSoundEnabled = enabled) }
    }

    fun onDarkModeChange(enabled: Boolean) {
        _uiState.update { it.copy(darkModeEnabled = enabled) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
    }

    fun testApiConnection() {
        if (_uiState.value.apiTestInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(apiTestInProgress = true, apiTestResult = null) }
            val result = healthRepository.checkConnection()
            _uiState.update {
                it.copy(
                    apiTestInProgress = false,
                    apiTestSucceeded = result.isSuccess,
                    apiTestResult = result.fold(
                        onSuccess = { body -> "Connected to the backend ✅ ($body)" },
                        onFailure = { e -> "Couldn't reach the backend: ${e.message}" }
                    )
                )
            }
        }
    }
}
