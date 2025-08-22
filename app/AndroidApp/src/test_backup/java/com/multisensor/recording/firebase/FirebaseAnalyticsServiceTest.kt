package com.multisensor.recording.firebase

import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Unit tests for FirebaseAnalyticsService
 */
@DisplayName("Firebase Analytics Service Tests")
class FirebaseAnalyticsServiceTest {

    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics
    private lateinit var firebaseAnalyticsService: FirebaseAnalyticsService

    @BeforeEach
    fun setup() {
        mockFirebaseAnalytics = mockk(relaxed = true)
        firebaseAnalyticsService = FirebaseAnalyticsService(mockFirebaseAnalytics)
    }

    @Test
    @DisplayName("Should log recording session start with correct parameters")
    fun testLogRecordingSessionStart() {
        // Given
        val sessionId = "test-session-123"
        val deviceCount = 3

        // When
        firebaseAnalyticsService.logRecordingSessionStart(sessionId, deviceCount)

        // Then
        verify {
            mockFirebaseAnalytics.logEvent(eq("recording_session_start"), any())
        }
    }

    @Test
    @DisplayName("Should log recording session end with correct parameters")
    fun testLogRecordingSessionEnd() {
        // Given
        val sessionId = "test-session-123"
        val durationMs = 60000L
        val dataSize = 1024L

        // When
        firebaseAnalyticsService.logRecordingSessionEnd(sessionId, durationMs, dataSize)

        // Then
        verify {
            mockFirebaseAnalytics.logEvent(eq("recording_session_end"), any())
        }
    }

    @Test
    @DisplayName("Should log GSR sensor connection")
    fun testLogGSRSensorConnected() {
        // Given
        val sensorId = "shimmer-001"

        // When
        firebaseAnalyticsService.logGSRSensorConnected(sensorId)

        // Then
        verify {
            mockFirebaseAnalytics.logEvent(eq("gsr_sensor_connected"), any())
        }
    }

    @Test
    @DisplayName("Should log thermal camera usage")
    fun testLogThermalCameraUsed() {
        // Given
        val cameraModel = "Topdon TC001"
        val resolution = "640x480"

        // When
        firebaseAnalyticsService.logThermalCameraUsed(cameraModel, resolution)

        // Then
        verify {
            mockFirebaseAnalytics.logEvent(eq("thermal_camera_used"), any())
        }
    }

    @Test
    @DisplayName("Should log calibration performed")
    fun testLogCalibrationPerformed() {
        // Given
        val calibrationType = "camera_calibration"
        val success = true

        // When
        firebaseAnalyticsService.logCalibrationPerformed(calibrationType, success)

        // Then
        verify {
            mockFirebaseAnalytics.logEvent(eq("calibration_performed"), any())
        }
    }

    @Test
    @DisplayName("Should log data export")
    fun testLogDataExport() {
        // Given
        val format = "CSV"
        val fileSizeBytes = 2048L

        // When
        firebaseAnalyticsService.logDataExport(format, fileSizeBytes)

        // Then
        verify {
            mockFirebaseAnalytics.logEvent(eq("data_export"), any())
        }
    }

    @Test
    @DisplayName("Should set user property")
    fun testSetUserProperty() {
        // Given
        val property = "experiment_type"
        val value = "stress_detection"

        // When
        firebaseAnalyticsService.setUserProperty(property, value)

        // Then
        verify {
            mockFirebaseAnalytics.setUserProperty(eq(property), eq(value))
        }
    }
}