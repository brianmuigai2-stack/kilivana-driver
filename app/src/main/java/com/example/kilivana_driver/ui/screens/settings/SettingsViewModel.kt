package com.example.kilivana_driver.ui.screens.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppLanguage(val displayName: String) {
    ENGLISH("English"),
    SWAHILI("Kiswahili")
}

data class SettingsUiState(
    val pushNotificationsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH
)

class SettingsViewModel : ViewModel() {

    // TODO: persist these with DataStore instead of losing them on app restart
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onPushNotificationsChange(enabled: Boolean) {
        _uiState.update { it.copy(pushNotificationsEnabled = enabled) }
    }

    fun onNotificationSoundChange(enabled: Boolean) {
        _uiState.update { it.copy(notificationSoundEnabled = enabled) }
    }

    fun onDarkModeChange(enabled: Boolean) {
        // TODO: wire this to an actual dark KilivanaTheme once one exists
        _uiState.update { it.copy(darkModeEnabled = enabled) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        // TODO: apply the chosen locale app-wide
        _uiState.update { it.copy(language = language) }
    }
}
