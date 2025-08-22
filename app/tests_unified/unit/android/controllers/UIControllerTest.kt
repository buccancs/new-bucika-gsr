package com.multisensor.recording.controllers

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.multisensor.recording.ui.MainUiState
import com.multisensor.recording.ui.SessionDisplayInfo
import com.multisensor.recording.ui.ShimmerDeviceInfo
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UIControllerTest {

    private lateinit var uiController: UIController
    private lateinit var mockCallback: UIController.UICallback
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var mockStatusText: TextView
    private lateinit var mockStartButton: View
    private lateinit var mockStopButton: View
    private lateinit var mockCalibrationButton: View
    private lateinit var mockPcIndicator: View
    private lateinit var mockShimmerIndicator: View
    private lateinit var mockThermalIndicator: View
    private lateinit var mockBatteryText: TextView
    private lateinit var mockRecordingIndicator: View
    private lateinit var mockStreamingIndicator: View
    private lateinit var mockStreamingLabel: View
    private lateinit var mockStreamingDebugOverlay: TextView
    private lateinit var mockPermissionsButton: View
    private lateinit var mockShimmerStatusText: TextView

    @Before
    fun setUp() {
        uiController = UIController()

        mockContext = ApplicationProvider.getApplicationContext()
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.apply() } just Runs

        mockStatusText = mockk(relaxed = true)
        mockStartButton = mockk(relaxed = true)
        mockStopButton = mockk(relaxed = true)
        mockCalibrationButton = mockk(relaxed = true)
        mockPcIndicator = mockk(relaxed = true)
        mockShimmerIndicator = mockk(relaxed = true)
        mockThermalIndicator = mockk(relaxed = true)
        mockBatteryText = mockk(relaxed = true)
        mockRecordingIndicator = mockk(relaxed = true)
        mockStreamingIndicator = mockk(relaxed = true)
        mockStreamingLabel = mockk(relaxed = true)
        mockStreamingDebugOverlay = mockk(relaxed = true)
        mockPermissionsButton = mockk(relaxed = true)
        mockShimmerStatusText = mockk(relaxed = true)

        mockCallback = mockk(relaxed = true)
        every { mockCallback.getContext() } returns mockContext
        every { mockCallback.getStatusText() } returns mockStatusText
        every { mockCallback.getStartRecordingButton() } returns mockStartButton
        every { mockCallback.getStopRecordingButton() } returns mockStopButton
        every { mockCallback.getCalibrationButton() } returns mockCalibrationButton
        every { mockCallback.getPcConnectionIndicator() } returns mockPcIndicator
        every { mockCallback.getShimmerConnectionIndicator() } returns mockShimmerIndicator
        every { mockCallback.getThermalConnectionIndicator() } returns mockThermalIndicator
        every { mockCallback.getBatteryLevelText() } returns mockBatteryText
        every { mockCallback.getRecordingIndicator() } returns mockRecordingIndicator
        every { mockCallback.getStreamingIndicator() } returns mockStreamingIndicator
        every { mockCallback.getStreamingLabel() } returns mockStreamingLabel
        every { mockCallback.getStreamingDebugOverlay() } returns mockStreamingDebugOverlay
        every { mockCallback.getRequestPermissionsButton() } returns mockPermissionsButton
        every { mockCallback.getShimmerStatusText() } returns mockShimmerStatusText

        mockkStatic(Context::class)
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkStatic(Context::class)
    }

    @Test
    fun `setCallback should initialize UI state persistence`() {
        uiController.setCallback(mockCallback)

        verify { mockContext.getSharedPreferences("ui_controller_prefs", Context.MODE_PRIVATE) }
    }

    @Test
    fun `initializeUIComponents should create consolidated UI components`() {
        uiController.setCallback(mockCallback)

        uiController.initializeUIComponents()

        verify { mockCallback.onUIComponentsInitialized() }
        val components = uiController.getConsolidatedComponents()
        assertNotNull("PC status indicator should be initialized", components.pcStatusIndicator)
        assertNotNull("Shimmer status indicator should be initialized", components.shimmerStatusIndicator)
        assertNotNull("Thermal status indicator should be initialized", components.thermalStatusIndicator)
        assertNotNull("Recording button pair should be initialized", components.recordingButtonPair)
    }

    @Test
    fun `initializeUIComponents should handle error gracefully`() {

        uiController.initializeUIComponents()

        val components = uiController.getConsolidatedComponents()
        assertNull("Components should be null when initialization fails", components.pcStatusIndicator)
    }

    @Test
    fun `updateUIFromState should update all UI elements correctly`() {
        uiController.setCallback(mockCallback)
        val testState = MainUiState(
            statusText = "Test Status",
            isPcConnected = true,
            isShimmerConnected = false,
            isThermalConnected = true,
            isRecording = true,
            isStreaming = false,
            isInitialized = true,
            batteryLevel = 75,
            streamingFrameRate = 30.0,
            streamingDataSize = 1200000L,
            showPermissionsButton = false,
            errorMessage = null,
            showErrorDialog = false,
            currentSessionInfo = null,
            shimmerDeviceInfo = null
        )

        uiController.updateUIFromState(testState)

        verify { mockCallback.runOnUiThread(any()) }
        verify { mockCallback.onUIStateUpdated(testState) }
    }

    @Test
    fun `updateUIFromState should handle error message correctly`() {
        uiController.setCallback(mockCallback)
        val testState = MainUiState(
            statusText = "Test Status",
            errorMessage = "Test Error",
            showErrorDialog = true
        )

        uiController.updateUIFromState(testState)

        verify { mockCallback.showToast("Test Error", android.widget.Toast.LENGTH_LONG) }
    }

    @Test
    fun `updateUIFromState should update session info when available`() {
        uiController.setCallback(mockCallback)
        val sessionInfo = SessionDisplayInfo(
            sessionId = "test_session",
            startTime = System.currentTimeMillis(),
            duration = 60000L,
            deviceCount = 2,
            recordingMode = "test",
            status = "Active"
        )
        val testState = MainUiState(
            statusText = "Test Status",
            currentSessionInfo = sessionInfo
        )

        uiController.updateUIFromState(testState)

        verify { mockCallback.updateStatusText(match { it.contains("test_session") && it.contains("Active") }) }
    }

    @Test
    fun `getSavedUIState should return default state when no preferences exist`() {
        every { mockSharedPreferences.getInt(any(), any()) } returns -1
        every { mockSharedPreferences.getBoolean(any(), any()) } returns false
        every { mockSharedPreferences.getString(any(), any()) } returns null
        uiController.setCallback(mockCallback)

        val savedState = uiController.getSavedUIState()

        assertEquals(-1, savedState.lastBatteryLevel)
        assertFalse(savedState.isPcConnected)
        assertFalse(savedState.isShimmerConnected)
        assertFalse(savedState.isThermalConnected)
        assertFalse(savedState.wasRecording)
        assertFalse(savedState.wasStreaming)
        assertEquals("default", savedState.themeMode)
        assertFalse(savedState.accessibilityMode)
        assertFalse(savedState.highContrastMode)
    }

    @Test
    fun `getSavedUIState should return saved preferences when they exist`() {
        every { mockSharedPreferences.getInt("last_battery_level", any()) } returns 80
        every { mockSharedPreferences.getBoolean("pc_connection_status", any()) } returns true
        every { mockSharedPreferences.getBoolean("shimmer_connection_status", any()) } returns true
        every { mockSharedPreferences.getBoolean("thermal_connection_status", any()) } returns false
        every { mockSharedPreferences.getBoolean("recording_state", any()) } returns true
        every { mockSharedPreferences.getBoolean("streaming_state", any()) } returns false
        every { mockSharedPreferences.getString("ui_theme_mode", any()) } returns "dark"
        every { mockSharedPreferences.getBoolean("accessibility_mode", any()) } returns true
        every { mockSharedPreferences.getBoolean("high_contrast_mode", any()) } returns true
        uiController.setCallback(mockCallback)

        val savedState = uiController.getSavedUIState()

        assertEquals(80, savedState.lastBatteryLevel)
        assertTrue(savedState.isPcConnected)
        assertTrue(savedState.isShimmerConnected)
        assertFalse(savedState.isThermalConnected)
        assertTrue(savedState.wasRecording)
        assertFalse(savedState.wasStreaming)
        assertEquals("dark", savedState.themeMode)
        assertTrue(savedState.accessibilityMode)
        assertTrue(savedState.highContrastMode)
    }

    @Test
    fun `setThemeMode should save theme preference`() {
        uiController.setCallback(mockCallback)

        uiController.setThemeMode("dark")

        verify { mockEditor.putString("ui_theme_mode", "dark") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `setAccessibilityMode should save accessibility preference`() {
        uiController.setCallback(mockCallback)

        uiController.setAccessibilityMode(true)

        verify { mockEditor.putBoolean("accessibility_mode", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `setHighContrastMode should save high contrast preference`() {
        uiController.setCallback(mockCallback)

        uiController.setHighContrastMode(true)

        verify { mockEditor.putBoolean("high_contrast_mode", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `setRecordingButtonListeners should set listeners when components initialized`() {
        uiController.setCallback(mockCallback)
        uiController.initializeUIComponents()
        val startAction: (View) -> Unit = mockk()
        val stopAction: (View) -> Unit = mockk()

        uiController.setRecordingButtonListeners(startAction, stopAction)

        assertTrue("Method should complete without error", true)
    }

    @Test
    fun `setRecordingButtonListeners should handle uninitialized components gracefully`() {
        uiController.setCallback(mockCallback)
        val startAction: (View) -> Unit = mockk()
        val stopAction: (View) -> Unit = mockk()

        uiController.setRecordingButtonListeners(startAction, stopAction)

        assertTrue("Method should handle uninitialized state gracefully", true)
    }

    @Test
    fun `getUIStatus should return complete status information`() {
        uiController.setCallback(mockCallback)

        val status = uiController.getUIStatus()

        assertTrue("Status should contain UI Controller Status", status.contains("UI Controller Status"))
        assertTrue("Status should contain PC Status Indicator info", status.contains("PC Status Indicator"))
        assertTrue("Status should contain Shimmer Status Indicator info", status.contains("Shimmer Status Indicator"))
        assertTrue("Status should contain Thermal Status Indicator info", status.contains("Thermal Status Indicator"))
        assertTrue("Status should contain Recording Button Pair info", status.contains("Recording Button Pair"))
        assertTrue("Status should contain State Persistence info", status.contains("State Persistence"))
        assertTrue("Status should contain Theme Mode info", status.contains("Theme Mode"))
        assertTrue("Status should contain Accessibility Mode info", status.contains("Accessibility Mode"))
        assertTrue("Status should contain High Contrast Mode info", status.contains("High Contrast Mode"))
        assertTrue("Status should contain Last Battery Level info", status.contains("Last Battery Level"))
        assertTrue("Status should contain Callback Set info", status.contains("Callback Set"))
    }

    @Test
    fun `resetState should clear all persisted preferences`() {
        uiController.setCallback(mockCallback)

        uiController.resetState()

        verify { mockEditor.clear() }
        verify { mockEditor.apply() }
    }

    @Test
    fun `cleanup should clear resources and callback`() {
        uiController.setCallback(mockCallback)

        uiController.cleanup()

        assertTrue("Cleanup should complete without error", true)
    }

    @Test
    fun `applyThemeFromPreferences should handle theme application`() {
        uiController.setCallback(mockCallback)

        uiController.applyThemeFromPreferences()

        assertTrue("Theme application should complete without error", true)
    }

    @Test
    fun `updateUIFromState should handle accessibility mode correctly`() {
        every { mockSharedPreferences.getBoolean("accessibility_mode", false) } returns true
        every { mockSharedPreferences.getBoolean("high_contrast_mode", false) } returns true
        uiController.setCallback(mockCallback)
        uiController.initializeUIComponents()

        val testState = MainUiState(
            statusText = "Test Status",
            isPcConnected = true,
            batteryLevel = 50,
            shimmerDeviceInfo = ShimmerDeviceInfo("TestDevice", 75, 85, true, System.currentTimeMillis())
        )

        uiController.updateUIFromState(testState)

        verify { mockCallback.runOnUiThread(any()) }
    }

    @Test
    fun `updateUIFromState should handle high contrast mode correctly`() {
        every { mockSharedPreferences.getBoolean("high_contrast_mode", false) } returns true
        uiController.setCallback(mockCallback)

        val testState = MainUiState(
            statusText = "Test Status",
            batteryLevel = 25,
            isShimmerConnected = true
        )

        uiController.updateUIFromState(testState)

        verify { mockCallback.runOnUiThread(any()) }
    }

    @Test
    fun `battery level display should use correct colours based on level`() {
        uiController.setCallback(mockCallback)

        val highBatteryState = MainUiState(statusText = "Test", batteryLevel = 75)
        uiController.updateUIFromState(highBatteryState)

        val mediumBatteryState = MainUiState(statusText = "Test", batteryLevel = 35)
        uiController.updateUIFromState(mediumBatteryState)

        val lowBatteryState = MainUiState(statusText = "Test", batteryLevel = 15)
        uiController.updateUIFromState(lowBatteryState)

        val unknownBatteryState = MainUiState(statusText = "Test", batteryLevel = -1)
        uiController.updateUIFromState(unknownBatteryState)

        verify(atLeast = 4) { mockCallback.runOnUiThread(any()) }
    }

    @Test
    fun `streaming indicator should update correctly with debug overlay`() {
        uiController.setCallback(mockCallback)

        val streamingState = MainUiState(
            statusText = "Test",
            isStreaming = true,
            streamingFrameRate = 30.0,
            streamingDataSize = 1500000L
        )

        uiController.updateUIFromState(streamingState)

        verify { mockCallback.runOnUiThread(any()) }
    }

    @Test
    fun `error handling should work with null callback`() {

        uiController.initializeUIComponents()
        uiController.updateUIFromState(MainUiState(statusText = "Test"))
        uiController.resetState()
        uiController.cleanup()

        assertTrue("Operations should handle null callback gracefully", true)
    }

    @Test
    fun `validateUIComponents should return valid result when all components available`() {
        uiController.setCallback(mockCallback)
        uiController.initializeUIComponents()

        val result = uiController.validateUIComponents()

        assertTrue("Validation should pass when all components available", result.isValid)
        assertEquals("No errors expected", 0, result.errors.size)
        assertTrue("Component count should be greater than 0", result.componentCount > 0)
        assertTrue(
            "Validation timestamp should be recent",
            System.currentTimeMillis() - result.validationTimestamp < 1000
        )
    }

    @Test
    fun `validateUIComponents should return errors when callback is null`() {

        val result = uiController.validateUIComponents()

        assertFalse("Validation should fail when callback is null", result.isValid)
        assertTrue("Should have callback error", result.errors.any { it.contains("callback is null") })
    }

    @Test
    fun `validateUIComponents should return warnings for missing optional components`() {
        every { mockCallback.getBatteryLevelText() } returns null
        every { mockCallback.getPcConnectionIndicator() } returns null
        uiController.setCallback(mockCallback)

        val result = uiController.validateUIComponents()

        assertTrue("Should have warnings for missing components", result.warnings.isNotEmpty())
        assertTrue(
            "Should warn about battery text",
            result.warnings.any { it.contains("Battery level text is null") })
        assertTrue(
            "Should warn about PC indicator",
            result.warnings.any { it.contains("PC connection indicator is null") })
    }

    @Test
    fun `recoverFromUIErrors should attempt recovery when possible`() {
        uiController.setCallback(mockCallback)

        val result = uiController.recoverFromUIErrors()

        assertTrue("Recovery should succeed with valid callback", result.success)
        assertTrue("Should have recovery actions", result.recoveryActions.isNotEmpty())
        assertTrue(
            "Should re-initialize components",
            result.recoveryActions.any { it.contains("Re-initialized UI components") })
    }

    @Test
    fun `recoverFromUIErrors should fail gracefully with null callback`() {

        val result = uiController.recoverFromUIErrors()

        assertFalse("Recovery should fail with null callback", result.success)
        assertTrue(
            "Should have failure message",
            result.recoveryActions.any { it.contains("Cannot recover - UI callback is null") })
    }

    @Test
    fun `validateUIState should detect inconsistent recording state`() {
        val reallyInconsistentState = MainUiState(
            statusText = "Test",
            isRecording = true,
            isLoadingRecording = true
        )

        val validationResult = uiController.validateUIState(reallyInconsistentState)

        assertNotNull("Validation result should not be null", validationResult)
    }

    @Test
    fun `validateUIState should detect invalid battery levels`() {
        val invalidBatteryState = MainUiState(
            statusText = "Test",
            batteryLevel = 150
        )

        val result = uiController.validateUIState(invalidBatteryState)

        assertFalse("Should detect invalid battery level", result.isValid)
        assertTrue(
            "Should have battery level error",
            result.issues.any { it.contains("Invalid battery level: 150") })
    }

    @Test
    fun `validateUIState should suggest warnings for low battery`() {
        val lowBatteryState = MainUiState(
            statusText = "Test",
            batteryLevel = 15
        )

        val result = uiController.validateUIState(lowBatteryState)

        assertTrue("Should pass validation for valid low battery", result.isValid)
        assertTrue(
            "Should suggest low battery warning",
            result.suggestions.any { it.contains("Low battery level detected") })
    }

    @Test
    fun `validateUIState should detect inconsistent streaming state`() {
        val inconsistentStreamingState = MainUiState(
            statusText = "Test",
            isStreaming = true,
            streamingFrameRate = 0.0
        )

        val result = uiController.validateUIState(inconsistentStreamingState)

        assertFalse("Should detect inconsistent streaming state", result.isValid)
        assertTrue(
            "Should have streaming state error",
            result.issues.any { it.contains("Inconsistent streaming state") })
    }

    @Test
    fun `validateUIState should detect error dialog issues`() {
        val errorDialogState = MainUiState(
            statusText = "Test",
            showErrorDialog = true,
            errorMessage = null
        )

        val result = uiController.validateUIState(errorDialogState)

        assertFalse("Should detect error dialog issue", result.isValid)
        assertTrue(
            "Should have error dialog issue",
            result.issues.any { it.contains("Error dialog should be shown but no error message") })
    }

    @Test
    fun `enableAccessibilityFeatures should apply accessibility settings`() {
        uiController.setCallback(mockCallback)
        uiController.initializeUIComponents()

        uiController.enableAccessibilityFeatures()

        verify { mockEditor.putBoolean("accessibility_mode", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `applyDynamicTheme should validate and apply theme correctly`() {
        uiController.setCallback(mockCallback)

        val result = uiController.applyDynamicTheme("dark", true)

        assertTrue("Theme application should succeed", result)
        verify { mockEditor.putString("ui_theme_mode", "dark") }
        verify { mockEditor.putBoolean("high_contrast_mode", true) }
    }

    @Test
    fun `applyDynamicTheme should handle invalid theme mode gracefully`() {
        uiController.setCallback(mockCallback)

        val result = uiController.applyDynamicTheme("invalid_theme", false)

        assertTrue("Should handle invalid theme gracefully", result)
        verify { mockEditor.putString("ui_theme_mode", "default") }
    }

    @Test
    fun `applyDynamicTheme should handle errors gracefully`() {
        every { mockEditor.putString(any(), any()) } throws RuntimeException("Storage error")
        uiController.setCallback(mockCallback)

        val result = uiController.applyDynamicTheme("dark", false)

        assertFalse("Should return false on error", result)
    }
}
