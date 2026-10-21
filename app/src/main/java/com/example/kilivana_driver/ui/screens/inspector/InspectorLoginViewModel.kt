package com.example.kilivana_driver.ui.screens.inspector

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.network.AuthRepository
import com.example.kilivana_driver.data.network.INSPECTOR_ROLE
import com.example.kilivana_driver.data.network.SessionPreferences
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InspectorLoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

class InspectorLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(InspectorLoginUiState())
    val uiState: StateFlow<InspectorLoginUiState> = _uiState.asStateFlow()

    private val authRepository = AuthRepository(
        preferences = SessionPreferences(getApplication()),
        allowedRole = INSPECTOR_ROLE
    )

    /** Whether an inspector is signed in, derived from the shared session. */
    val isSignedIn: StateFlow<Boolean> = SessionStore.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
        viewModelScope.launch { authRepository.logout() }
        _uiState.update { InspectorLoginUiState() }
    }

    fun signedInUser(): AuthUser? = SessionStore.currentUser.value

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

            val result = authRepository.login(
                email = state.email,
                password = state.password,
                rememberMe = false
            )

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
