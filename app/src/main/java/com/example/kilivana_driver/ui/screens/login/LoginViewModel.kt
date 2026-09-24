package com.example.kilivana_driver.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onPhoneChange(value: String) {
        // Only allow digits, "+" and spaces, capped in length
        val cleaned = value.filter { it.isDigit() || it == '+' || it == ' ' }.take(16)
        _uiState.update { it.copy(phoneNumber = cleaned, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onRememberMeChange(value: Boolean) {
        // TODO: persist with DataStore once real auth is wired up
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.isLoading) return

        val digitCount = state.phoneNumber.count { it.isDigit() }
        val validationError = when {
            state.phoneNumber.isBlank() -> "Enter your phone number"
            digitCount < 9 -> "Enter a valid phone number, e.g. +254 700 000000"
            state.password.isBlank() -> "Enter your password"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // TODO: replace this fake check with a real AuthRepository call
            delay(1200)
            val success = state.password.length >= 4

            _uiState.update {
                if (success) {
                    it.copy(isLoading = false, isLoggedIn = true)
                } else {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Incorrect phone number or password. Please try again."
                    )
                }
            }
        }
    }
}
