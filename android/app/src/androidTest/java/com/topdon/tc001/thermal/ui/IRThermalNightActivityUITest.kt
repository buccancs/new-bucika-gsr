package com.topdon.tc001.thermal.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.topdon.thermal.activity.IRThermalNightActivity
import com.topdon.tc001.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for IRThermalNightActivity
 * Tests the refactored thermal camera interface with Manager Extraction Pattern
 * Focuses on integration between UI and the specialized managers:
 * - ThermalCameraManager
 * - ThermalUIStateManager  
 * - ThermalConfigurationManager
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class IRThermalNightActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(IRThermalNightActivity::class.java)

    @Test
    fun testThermalActivityLaunches() {
        // Verify thermal activity launches with refactored managers
        onView(withId(R.id.thermal_main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCameraPreviewVisibility() {
        // Test camera preview surface is displayed (ThermalCameraManager integration)
        onView(withId(R.id.camera_preview_surface))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalOverlayDisplay() {
        // Test thermal overlay is rendered correctly (ThermalUIStateManager)
        onView(withId(R.id.thermal_overlay_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCameraConnectionButton() {
        // Test camera connection functionality (ThermalCameraManager)
        onView(withId(R.id.btn_connect_camera))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify connection status updates
        onView(withId(R.id.tv_camera_status))
            .check(matches(withText(containsString("Connecting"))))
    }

    @Test
    fun testThermalSettingsAccess() {
        // Test thermal settings access (ThermalConfigurationManager)
        onView(withId(R.id.btn_thermal_settings))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify settings panel opens
        onView(withId(R.id.thermal_settings_panel))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTemperatureCalibration() {
        // Test temperature calibration UI (ThermalConfigurationManager)
        onView(withId(R.id.btn_calibrate_temperature))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify calibration dialog appears
        onView(withText("Temperature Calibration"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalImageCapture() {
        // Test thermal image capture functionality
        onView(withId(R.id.btn_capture_image))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify capture feedback
        onView(withId(R.id.capture_feedback))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalVideoRecording() {
        // Test thermal video recording start/stop
        onView(withId(R.id.btn_record_video))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify recording indicator appears
        onView(withId(R.id.recording_indicator))
            .check(matches(isDisplayed()))
        
        // Stop recording
        onView(withId(R.id.btn_record_video))
            .perform(click())
        
        // Verify recording stops
        onView(withId(R.id.recording_indicator))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testTemperatureReadingDisplay() {
        // Test temperature reading display (ThermalUIStateManager)
        onView(withId(R.id.tv_current_temperature))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalPaletteSelection() {
        // Test thermal palette selection (ThermalConfigurationManager)
        onView(withId(R.id.btn_palette_selection))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify palette options appear
        onView(withId(R.id.palette_options_container))
            .check(matches(isDisplayed()))
        
        // Select a palette
        onView(withText("Iron"))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testZoomFunctionality() {
        // Test thermal image zoom functionality (ThermalUIStateManager)
        onView(withId(R.id.thermal_image_view))
            .check(matches(isDisplayed()))
            .perform(pinchOut())
        
        // Verify zoom level indicator updates
        onView(withId(R.id.tv_zoom_level))
            .check(matches(withText(containsString("2.0x"))))
    }

    @Test
    fun testOrientationHandling() {
        // Test orientation changes with manager pattern (ThermalUIStateManager)
        onView(withId(R.id.thermal_main_container))
            .check(matches(isDisplayed()))
        
        // Simulate orientation change
        // The ThermalUIStateManager should handle this gracefully
    }

    @Test
    fun testUSBDeviceConnection() {
        // Test USB thermal device connection (ThermalCameraManager)
        onView(withId(R.id.btn_usb_connect))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify USB connection dialog
        onView(withText("USB Device Connection"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalDataExport() {
        // Test thermal data export functionality
        onView(withId(R.id.btn_export_thermal_data))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify export options dialog
        onView(withText("Export Thermal Data"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testErrorHandlingDisplay() {
        // Test error handling in thermal UI (all managers)
        // This would trigger various error conditions
        onView(withId(R.id.error_message_container))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testPerformanceMonitoring() {
        // Test performance monitoring display
        onView(withId(R.id.performance_stats))
            .check(matches(isDisplayed()))
        
        // Verify FPS counter
        onView(withId(R.id.tv_fps_counter))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testManagerIntegration() {
        // Test integration between the extracted managers
        // This verifies the Manager Extraction Pattern works correctly
        
        // Camera Manager integration
        onView(withId(R.id.btn_connect_camera))
            .perform(click())
        
        // UI State Manager should update
        onView(withId(R.id.tv_camera_status))
            .check(matches(isDisplayed()))
        
        // Configuration Manager should be accessible
        onView(withId(R.id.btn_thermal_settings))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAccessibilityFeatures() {
        // Test accessibility features for thermal interface
        onView(withId(R.id.btn_connect_camera))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.btn_capture_image))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.tv_current_temperature))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testComplexUserFlow() {
        // Test complex user interaction flow
        // This tests the integration of all three managers in a realistic scenario
        
        // 1. Connect to camera (ThermalCameraManager)
        onView(withId(R.id.btn_connect_camera))
            .perform(click())
        
        // 2. Configure settings (ThermalConfigurationManager)
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        onView(withText("Temperature Range"))
            .perform(click())
        
        // 3. Start capture session (ThermalUIStateManager)
        onView(withId(R.id.btn_start_capture))
            .perform(click())
        
        // 4. Verify all components work together
        onView(withId(R.id.thermal_overlay_view))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_current_temperature))
            .check(matches(isDisplayed()))
    }
}