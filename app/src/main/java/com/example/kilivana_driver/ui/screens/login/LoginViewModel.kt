package com.example.kilivana_driver.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.network.AuthRepository
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
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

    private val authRepository = AuthRepository()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value.trim(), errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onLogout() {
        viewModelScope.launch { authRepository.logout(SessionStore.userId) }
        _uiState.update { LoginUiState() }
    }

    fun onRememberMeChange(value: Boolean) {
        // TODO: persist with DataStore once real auth is wired up
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.isLoading) return

        val validationError = when {
            state.email.isBlank() -> "Enter your email address"
            !state.email.contains("@") || !state.email.substringAfter("@").contains(".") ->
                "Enter a valid email address"
            state.password.isBlank() -> "Enter your password"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.login(state.email, state.password)

            _uiState.update {
                result.fold(
                    onSuccess = { _ ->
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            password = ""
                        )
                    },
                    onFailure = { e ->
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Couldn't reach the backend"
                        )
                    }
                )
            }
        }
    }
}
