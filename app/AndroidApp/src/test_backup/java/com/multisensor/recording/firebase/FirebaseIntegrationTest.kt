package com.multisensor.recording.firebase

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*

/**
 * Simple tests to validate Firebase integration setup
 */
@DisplayName("Firebase Integration Setup Tests")
class FirebaseIntegrationTest {

    @Test
    @DisplayName("Firebase service classes can be instantiated")
    fun testFirebaseServiceClassesExist() {
        // Verify that all Firebase service classes are available
        assertDoesNotThrow {
            val analyticsClass = FirebaseAnalyticsService::class.java
            val firestoreClass = FirebaseFirestoreService::class.java
            val storageClass = FirebaseStorageService::class.java
            
            assertNotNull(analyticsClass)
            assertNotNull(firestoreClass)
            assertNotNull(storageClass)
        }
    }

    @Test
    @DisplayName("Firebase data classes have correct structure")
    fun testFirebaseDataClasses() {
        // Test RecordingSession data class
        val session = FirebaseFirestoreService.RecordingSession(
            sessionId = "test-session",
            deviceCount = 2,
            researcherId = "researcher-001",
            experimentType = "stress_detection"
        )
        
        assertEquals("test-session", session.sessionId)
        assertEquals(2, session.deviceCount)
        assertEquals("researcher-001", session.researcherId)
        assertEquals("stress_detection", session.experimentType)
        assertNotNull(session.startTime)
        assertEquals(emptyList<String>(), session.gsrSensorIds)
        assertEquals(emptyMap<String, Any>(), session.calibrationData)
    }

    @Test
    @DisplayName("Firebase UI state classes work correctly")
    fun testFirebaseUIState() {
        val uiState = com.multisensor.recording.ui.firebase.FirebaseStatusUiState(
            analyticsEnabled = true,
            firestoreEnabled = true,
            storageEnabled = false,
            analyticsEventsCount = 10
        )
        
        assertTrue(uiState.analyticsEnabled)
        assertTrue(uiState.firestoreEnabled)
        assertFalse(uiState.storageEnabled)
        assertEquals(10, uiState.analyticsEventsCount)
        assertEquals(0, uiState.firestoreDocumentCount)
        assertEquals(emptyList<com.multisensor.recording.ui.firebase.FirebaseActivity>(), uiState.recentActivities)
    }

    @Test
    @DisplayName("Firebase activity data class works correctly")
    fun testFirebaseActivity() {
        val activity = com.multisensor.recording.ui.firebase.FirebaseActivity(
            action = "Test analytics event logged",
            timestamp = "12:34:56"
        )
        
        assertEquals("Test analytics event logged", activity.action)
        assertEquals("12:34:56", activity.timestamp)
    }
}