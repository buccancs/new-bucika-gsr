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

@RunWith(AndroidJUnit4::class)
@LargeTest
class IRThermalNightActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(IRThermalNightActivity::class.java)

    @Test
    fun testThermalActivityLaunches() {

        onView(withId(R.id.thermal_main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCameraPreviewVisibility() {

        onView(withId(R.id.camera_preview_surface))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalOverlayDisplay() {

        onView(withId(R.id.thermal_overlay_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCameraConnectionButton() {

        onView(withId(R.id.btn_connect_camera))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.tv_camera_status))
            .check(matches(withText(containsString("Connecting"))))
    }

    @Test
    fun testThermalSettingsAccess() {

        onView(withId(R.id.btn_thermal_settings))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.thermal_settings_panel))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTemperatureCalibration() {

        onView(withId(R.id.btn_calibrate_temperature))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Temperature Calibration"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalImageCapture() {

        onView(withId(R.id.btn_capture_image))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.capture_feedback))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalVideoRecording() {

        onView(withId(R.id.btn_record_video))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.recording_indicator))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_record_video))
            .perform(click())
        
        onView(withId(R.id.recording_indicator))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testTemperatureReadingDisplay() {

        onView(withId(R.id.tv_current_temperature))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalPaletteSelection() {

        onView(withId(R.id.btn_palette_selection))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.palette_options_container))
            .check(matches(isDisplayed()))
        
        onView(withText("Iron"))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testZoomFunctionality() {

        onView(withId(R.id.thermal_image_view))
            .check(matches(isDisplayed()))
            .perform(pinchOut())
        
        onView(withId(R.id.tv_zoom_level))
            .check(matches(withText(containsString("2.0x"))))
    }

    @Test
    fun testOrientationHandling() {

        onView(withId(R.id.thermal_main_container))
            .check(matches(isDisplayed()))
        
    }

    @Test
    fun testUSBDeviceConnection() {

        onView(withId(R.id.btn_usb_connect))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("USB Device Connection"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalDataExport() {

        onView(withId(R.id.btn_export_thermal_data))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Export Thermal Data"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testErrorHandlingDisplay() {

        onView(withId(R.id.error_message_container))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testPerformanceMonitoring() {

        onView(withId(R.id.performance_stats))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_fps_counter))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testManagerIntegration() {

        onView(withId(R.id.btn_connect_camera))
            .perform(click())
        
        onView(withId(R.id.tv_camera_status))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_thermal_settings))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAccessibilityFeatures() {

        onView(withId(R.id.btn_connect_camera))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.btn_capture_image))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.tv_current_temperature))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testComplexUserFlow() {

        onView(withId(R.id.btn_connect_camera))
            .perform(click())
        
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        onView(withText("Temperature Range"))
            .perform(click())
        
        onView(withId(R.id.btn_start_capture))
            .perform(click())
        
        onView(withId(R.id.thermal_overlay_view))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_current_temperature))
            .check(matches(isDisplayed()))
    }
