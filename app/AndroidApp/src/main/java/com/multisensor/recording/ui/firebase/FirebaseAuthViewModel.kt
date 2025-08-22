package com.multisensor.recording.ui.firebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multisensor.recording.firebase.AuthState
import com.multisensor.recording.firebase.FirebaseAuthService
import com.multisensor.recording.firebase.ResearcherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Firebase Authentication screen
 */
@HiltViewModel
class FirebaseAuthViewModel @Inject constructor(
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirebaseAuthUiState())
    val uiState: StateFlow<FirebaseAuthUiState> = _uiState.asStateFlow()

    init {
        // Monitor authentication state
        viewModelScope.launch {
            authService.authState.collect { authState ->
                when (authState) {
                    is AuthState.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    is AuthState.Authenticated -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            errorMessage = null,
                            currentUser = authState.user
                        )
                    }
                    is AuthState.NotAuthenticated -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            currentUser = null
                        )
                    }
                    is AuthState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = authState.message
                        )
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            clearMessages()
            val result = authService.signInWithEmailAndPassword(email, password)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    fun register(email: String, password: String, displayName: String, researcherType: ResearcherType) {
        viewModelScope.launch {
            clearMessages()
            val result = authService.createResearcherAccount(email, password, displayName, researcherType)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Account created successfully! You can now sign in."
                )
                switchToLoginMode()
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter your email address"
            )
            return
        }

        viewModelScope.launch {
            clearMessages()
            val result = authService.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Password reset email sent to $email"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.signOut()
        }
    }

    fun switchToRegisterMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = true,
            errorMessage = null,
            successMessage = null
        )
    }

    fun switchToLoginMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateDisplayName(displayName: String) {
        _uiState.value = _uiState.value.copy(displayName = displayName)
    }

    fun updateResearcherType(researcherType: ResearcherType) {
        _uiState.value = _uiState.value.copy(selectedResearcherType = researcherType)
    }

    private fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

/**
 * UI state for Firebase Authentication screen
 */
data class FirebaseAuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isRegisterMode: Boolean = false,
    val currentUser: com.google.firebase.auth.FirebaseUser? = null,
    val displayName: String = "",
    val selectedResearcherType: ResearcherType = ResearcherType.RESEARCHER,
    val errorMessage: String? = null,
    val successMessage: String? = null
)