package com.multisensor.recording.ui.firebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multisensor.recording.firebase.AuthState
import com.multisensor.recording.firebase.FirebaseAnalyticsService
import com.multisensor.recording.firebase.FirebaseAuthService
import com.multisensor.recording.firebase.FirebaseFirestoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Firebase status screen
 */
@HiltViewModel
class FirebaseStatusViewModel @Inject constructor(
    private val analyticsService: FirebaseAnalyticsService,
    private val firestoreService: FirebaseFirestoreService,
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirebaseStatusUiState())
    val uiState: StateFlow<FirebaseStatusUiState> = _uiState.asStateFlow()

    init {
        // Monitor authentication state
        viewModelScope.launch {
            authService.authState.collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        _uiState.value = _uiState.value.copy(
                            isAuthenticated = true,
                            currentUser = authState.user,
                            authenticationEnabled = true
                        )
                        loadUsageStatistics()
                    }
                    is AuthState.NotAuthenticated -> {
                        _uiState.value = _uiState.value.copy(
                            isAuthenticated = false,
                            currentUser = null,
                            authenticationEnabled = true
                        )
                    }
                    else -> {
                        // Loading or Error states handled in AuthScreen
                    }
                }
            }
        }
    }

    fun loadFirebaseStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                analyticsEnabled = true, // Firebase Analytics is always enabled if properly configured
                firestoreEnabled = true, // Firestore is enabled if properly configured
                storageEnabled = true, // Storage is enabled if properly configured
                analyticsEventsCount = 0, // In a real app, this would come from local storage or API
                firestoreDocumentCount = 0, // In a real app, this would come from a Firestore query
                storageBytesUploaded = 0L // In a real app, this would come from storage metadata
            )
            
            if (_uiState.value.isAuthenticated) {
                loadUsageStatistics()
            }
        }
    }

    private fun loadUsageStatistics() {
        viewModelScope.launch {
            try {
                val result = firestoreService.getUsageStatistics()
                if (result.isSuccess) {
                    val stats = result.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        totalSessions = (stats["totalSessions"] as? Long)?.toInt() ?: 0,
                        totalDataSize = stats["totalDataSizeBytes"] as? Long ?: 0L,
                        averageSessionDuration = stats["averageSessionDurationMs"] as? Long ?: 0L
                    )
                }
            } catch (e: Exception) {
                addErrorActivity("Failed to load usage statistics: ${e.message}")
            }
        }
    }

    fun testAnalytics() {
        viewModelScope.launch {
            try {
                // Test analytics event
                analyticsService.logRecordingSessionStart("test-session-${System.currentTimeMillis()}", 2, "integration_test")
                analyticsService.logGSRSensorConnected("test-sensor-001", "shimmer", "bluetooth")
                analyticsService.logThermalCameraUsed("Test Camera", "640x480", 30)
                
                // Update UI state
                val currentState = _uiState.value
                _uiState.value = currentState.copy(
                    analyticsEventsCount = currentState.analyticsEventsCount + 3,
                    recentActivities = listOf(
                        FirebaseActivity(
                            action = "Analytics test events logged",
                            timestamp = getCurrentTimestamp()
                        )
                    ) + currentState.recentActivities
                )
            } catch (e: Exception) {
                addErrorActivity("Analytics test failed: ${e.message}")
            }
        }
    }

    fun testFirestore() {
        viewModelScope.launch {
            try {
                if (!_uiState.value.isAuthenticated) {
                    addErrorActivity("Authentication required for Firestore test")
                    return@launch
                }
                
                // Test Firestore document creation
                val testSession = FirebaseFirestoreService.RecordingSession(
                    startTime = Date(),
                    deviceCount = 2,
                    experimentType = "firebase_integration_test",
                    participantId = "test-participant-001",
                    notes = "Test session created from Firebase status screen"
                )
                
                val result = firestoreService.saveRecordingSession(testSession)
                
                if (result.isSuccess) {
                    val sessionId = result.getOrThrow()
                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(
                        firestoreDocumentCount = currentState.firestoreDocumentCount + 1,
                        recentActivities = listOf(
                            FirebaseActivity(
                                action = "Test session saved to Firestore (ID: ${sessionId.take(8)}...)",
                                timestamp = getCurrentTimestamp()
                            )
                        ) + currentState.recentActivities
                    )
                } else {
                    addErrorActivity("Firestore test failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                addErrorActivity("Firestore test failed: ${e.message}")
            }
        }
    }

    fun testAuthentication() {
        viewModelScope.launch {
            try {
                if (_uiState.value.isAuthenticated) {
                    // Test user profile update
                    val result = firestoreService.updateResearcherLastActive(
                        authService.getCurrentUserId() ?: ""
                    )
                    
                    if (result.isSuccess) {
                        addSuccessActivity("Researcher profile updated successfully")
                    } else {
                        addErrorActivity("Profile update failed: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    addErrorActivity("No user currently authenticated")
                }
            } catch (e: Exception) {
                addErrorActivity("Authentication test failed: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val result = authService.signOut()
            if (result.isSuccess) {
                addSuccessActivity("Successfully signed out")
            } else {
                addErrorActivity("Sign out failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun addErrorActivity(message: String) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            recentActivities = listOf(
                FirebaseActivity(
                    action = "❌ $message",
                    timestamp = getCurrentTimestamp()
                )
            ) + currentState.recentActivities
        )
    }

    private fun addSuccessActivity(message: String) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            recentActivities = listOf(
                FirebaseActivity(
                    action = "✅ $message",
                    timestamp = getCurrentTimestamp()
                )
            ) + currentState.recentActivities
        )
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}

/**
 * UI state for Firebase status screen
 */
data class FirebaseStatusUiState(
    val analyticsEnabled: Boolean = false,
    val firestoreEnabled: Boolean = false,
    val storageEnabled: Boolean = false,
    val authenticationEnabled: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: com.google.firebase.auth.FirebaseUser? = null,
    val analyticsEventsCount: Int = 0,
    val firestoreDocumentCount: Int = 0,
    val storageBytesUploaded: Long = 0L,
    val totalSessions: Int = 0,
    val totalDataSize: Long = 0L,
    val averageSessionDuration: Long = 0L,
    val recentActivities: List<FirebaseActivity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Data class for Firebase activity log
 */
data class FirebaseActivity(
    val action: String,
    val timestamp: String
)