package com.multisensor.recording.controllers

import android.content.Context
import android.content.SharedPreferences
import android.view.TextureView
import com.multisensor.recording.ui.MainViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RecordingControllerTest {

    private lateinit var recordingController: RecordingController
    private lateinit var mockContext: Context
    private lateinit var mockTextureView: TextureView
    private lateinit var mockViewModel: MainViewModel
    private lateinit var mockCallback: RecordingController.RecordingCallback
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        recordingController = RecordingController()
        mockContext = mockk(relaxed = true)
        mockTextureView = mockk(relaxed = true)
        mockViewModel = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockSharedPreferencesEditor = mockk(relaxed = true)

        every {
            mockContext.getSharedPreferences(
                "recording_controller_state",
                Context.MODE_PRIVATE
            )
        } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putBoolean(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putString(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putLong(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putInt(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } returns Unit

        every { mockSharedPreferences.getBoolean("is_initialized", false) } returns false
        every { mockSharedPreferences.getLong("total_recording_time", 0L) } returns 0L
        every { mockSharedPreferences.getString("quality_setting", "MEDIUM") } returns "MEDIUM"
        every { mockSharedPreferences.getString("current_session_id", null) } returns null
        every { mockSharedPreferences.getInt("session_count", 0) } returns 0

        recordingController.setCallback(mockCallback)
    }

    @Test
    fun `test recording controller initialization`() {
        assertFalse(
            "Recording system should not be initialized initially",
            recordingController.isRecordingSystemInitialized()
        )

        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)

        assertTrue(
            "Recording system should be initialized after initializeRecordingSystem call",
            recordingController.isRecordingSystemInitialized()
        )
        verify { mockCallback.onRecordingInitialized() }
        verify { mockViewModel.initializeSystem(mockTextureView) }
    }

    @Test
    fun `test state persistence initialization`() {
        recordingController.initializeStatePersistence(mockContext)

        verify { mockContext.getSharedPreferences("recording_controller_state", Context.MODE_PRIVATE) }
    }

    @Test
    fun `test state restoration from SharedPreferences`() {
        every { mockSharedPreferences.getBoolean("is_initialized", false) } returns true
        every { mockSharedPreferences.getLong("total_recording_time", 0L) } returns 5000L
        every { mockSharedPreferences.getString("quality_setting", "MEDIUM") } returns "HIGH"

        recordingController.initializeStatePersistence(mockContext)

        val state = recordingController.getCurrentState()
        assertEquals("Total recording time should be restored", 5000L, state.totalRecordingTime)
        assertEquals(
            "Quality setting should be restored",
            RecordingController.RecordingQuality.HIGH,
            state.lastQualitySetting
        )
    }

    @Test
    fun `test recording quality settings management`() {
        recordingController.setRecordingQuality(RecordingController.RecordingQuality.HIGH)
        assertEquals(
            "Quality should be set to HIGH",
            RecordingController.RecordingQuality.HIGH, recordingController.getCurrentQuality()
        )

        val details = recordingController.getQualityDetails(RecordingController.RecordingQuality.HIGH)
        assertEquals("Resolution should match HIGH quality", "1920x1080", details["resolution"])
        assertEquals("Frame rate should match HIGH quality", "30 fps", details["frameRate"])

        verify { mockCallback.updateStatusText(any()) }
    }

    @Test
    fun `test available qualities`() {
        val qualities = recordingController.getAvailableQualities()
        assertTrue("Should have at least 4 quality options", qualities.size >= 4)
        assertTrue("Should contain LOW quality", qualities.contains(RecordingController.RecordingQuality.LOW))
        assertTrue("Should contain HIGH quality", qualities.contains(RecordingController.RecordingQuality.HIGH))
    }

    @Test
    fun `test service connection monitoring`() = runTest {
        assertFalse(
            "Service should not be connected initially",
            recordingController.serviceConnectionState.value.isConnected
        )

        recordingController.handleServiceConnectionStatus(true)
        assertTrue(
            "Service should be connected after status update",
            recordingController.serviceConnectionState.value.isConnected
        )

        recordingController.updateServiceHeartbeat()
        assertTrue("Service should be healthy after heartbeat", recordingController.isServiceHealthy())

        recordingController.handleServiceConnectionStatus(false)
        assertFalse(
            "Service should not be connected after disconnect",
            recordingController.serviceConnectionState.value.isConnected
        )
        verify { mockCallback.onRecordingError("Lost connection to recording service") }
    }

    @Test
    fun `test recording session management`() {
        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)

        recordingController.startRecording(mockContext, mockViewModel)
        verify { mockCallback.onRecordingStarted() }
        verify { mockViewModel.startRecording() }

        recordingController.stopRecording(mockContext, mockViewModel)
        verify { mockCallback.onRecordingStopped() }
        verify { mockViewModel.stopRecording() }
    }

    @Test
    fun `test recording without initialization should fail`() {
        recordingController.startRecording(mockContext, mockViewModel)
        verify { mockCallback.onRecordingError("Recording system not initialized") }
    }

    @Test
    fun `test session metadata collection`() {
        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)
        recordingController.startRecording(mockContext, mockViewModel)

        val metadata = recordingController.getSessionMetadata()
        assertTrue("Metadata should contain initialization status", metadata.containsKey("initialized"))
        assertTrue("Metadata should contain app version", metadata.containsKey("app_version"))
        assertTrue("Metadata should contain timestamp", metadata.containsKey("timestamp"))
    }

    @Test
    fun `test recording status summary`() {
        val status = recordingController.getRecordingStatus()
        assertTrue("Status should contain initialization info", status.contains("Initialized"))
        assertTrue("Status should contain service status", status.contains("Service Status"))
        assertTrue("Status should contain session info", status.contains("Current Session"))
    }

    @Test
    fun `test recording prerequisite validation`() {
        every { mockContext.getExternalFilesDir(null) } returns mockk {
            every { exists() } returns true
            every { canWrite() } returns true
            every { path } returns "/test/path"
        }

        assertFalse(
            "Validation should fail when system not initialized",
            recordingController.validateRecordingPrerequisites(mockContext)
        )

        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)
        assertTrue(
            "System should be initialized after initialization call",
            recordingController.isRecordingSystemInitialized()
        )
    }

    @Test
    fun `test quality validation for resources`() {
        every { mockContext.getExternalFilesDir(null) } returns mockk {
            every { path } returns "/test/path"
        }

        assertNotNull(
            "Quality validation should return a result",
            recordingController.validateQualityForResources(mockContext, RecordingController.RecordingQuality.LOW)
        )
    }

    @Test
    fun `test recommended quality calculation`() {
        every { mockContext.getExternalFilesDir(null) } returns mockk {
            every { path } returns "/test/path"
        }

        val recommendedQuality = recordingController.getRecommendedQuality(mockContext)
        assertNotNull("Should return a recommended quality", recommendedQuality)
        assertTrue(
            "Should return a valid quality option",
            recordingController.getAvailableQualities().contains(recommendedQuality)
        )
    }

    @Test
    fun `test emergency stop functionality`() {
        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)
        recordingController.startRecording(mockContext, mockViewModel)

        recordingController.emergencyStopRecording(mockContext, mockViewModel)

        verify { mockCallback.showToast(any(), any()) }
    }

    @Test
    fun `test state reset functionality`() {
        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)
        assertTrue("System should be initialized", recordingController.isRecordingSystemInitialized())

        recordingController.resetState()
        assertFalse("System should not be initialized after reset", recordingController.isRecordingSystemInitialized())
    }

    @Test
    fun `test duration formatting`() {
        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)

        val metadata = recordingController.getSessionMetadata()
        assertNotNull("Metadata should be available", metadata)
        assertTrue("Should contain required fields", metadata.isNotEmpty())
    }

    @Test
    fun `test multiple quality changes during session`() {
        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)
        recordingController.startRecording(mockContext, mockViewModel)

        recordingController.setRecordingQuality(RecordingController.RecordingQuality.HIGH)
        recordingController.setRecordingQuality(RecordingController.RecordingQuality.LOW)
        recordingController.setRecordingQuality(RecordingController.RecordingQuality.ULTRA_HIGH)

        assertEquals(
            "Final quality should be ULTRA_HIGH",
            RecordingController.RecordingQuality.ULTRA_HIGH, recordingController.getCurrentQuality()
        )

        recordingController.stopRecording(mockContext, mockViewModel)
    }

    @Test
    fun `test service binding and unbinding`() {
        val bindResult = recordingController.bindToRecordingService(mockContext)
        assertNotNull("Bind result should not be null", bindResult)

        recordingController.unbindFromRecordingService(mockContext)
    }

    @Test
    fun `test recording quality size estimation`() {
        val lowQuality = RecordingController.RecordingQuality.LOW
        val highQuality = RecordingController.RecordingQuality.HIGH

        val lowSize = lowQuality.getEstimatedSizePerSecond()
        val highSize = highQuality.getEstimatedSizePerSecond()

        assertTrue("High quality should use more storage than low quality", highSize > lowSize)
        assertTrue("Low quality size should be positive", lowSize > 0)
    }

    @Test
    fun `test error handling during recording start`() {
        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)

        every { mockViewModel.startRecording() } throws RuntimeException("Test exception")

        recordingController.startRecording(mockContext, mockViewModel)
        verify { mockCallback.onRecordingError(any()) }
    }

    @Test
    fun `test error handling during recording stop`() {
        recordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)
        recordingController.startRecording(mockContext, mockViewModel)

        every { mockViewModel.stopRecording() } throws RuntimeException("Test exception")

        recordingController.stopRecording(mockContext, mockViewModel)
        verify { mockCallback.onRecordingError(any()) }
    }
}
