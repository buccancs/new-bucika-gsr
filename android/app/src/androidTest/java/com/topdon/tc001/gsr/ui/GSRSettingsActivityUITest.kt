package com.topdon.tc001.gsr.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.topdon.tc001.gsr.settings.GSRSettingsActivity
import com.topdon.tc001.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for GSRSettingsActivity
 * Tests GSR configuration options, Shimmer SDK integration settings, and user preferences
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class GSRSettingsActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(GSRSettingsActivity::class.java)

    @Test
    fun testSettingsActivityLaunches() {
        // Verify settings activity launches correctly
        onView(withId(R.id.settings_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testGSRSamplingRateConfiguration() {
        // Test GSR sampling rate configuration
        onView(withText("Sampling Rate"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify sampling rate options
        onView(withText("128 Hz"))
            .check(matches(isDisplayed()))
        
        onView(withText("256 Hz"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify selection is applied
        onView(withText("Sampling Rate: 256 Hz"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testShimmerDeviceConfiguration() {
        // Test Shimmer device configuration options
        onView(withText("Shimmer Device Settings"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Test device name configuration
        onView(withId(R.id.edit_device_name))
            .check(matches(isDisplayed()))
            .perform(clearText(), typeText("GSR_Device_01"))
        
        // Test connection timeout setting
        onView(withText("Connection Timeout"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("30 seconds"))
            .perform(click())
    }

    @Test
    fun testDataLoggingSettings() {
        // Test data logging configuration
        onView(withText("Data Logging"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Test log file format selection
        onView(withText("Log Format"))
            .perform(click())
        
        onView(withText("CSV"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Test automatic backup setting
        onView(withId(R.id.switch_auto_backup))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testCalibrationSettings() {
        // Test GSR calibration settings
        onView(withText("Calibration"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Test baseline calibration
        onView(withId(R.id.btn_calibrate_baseline))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify calibration dialog
        onView(withText("Baseline Calibration"))
            .check(matches(isDisplayed()))
        
        // Test sensitivity adjustment
        onView(withId(R.id.slider_sensitivity))
            .check(matches(isDisplayed()))
            .perform(swipeRight())
    }

    @Test
    fun testNotificationSettings() {
        // Test notification preferences
        onView(withText("Notifications"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.switch_connection_alerts))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.switch_data_alerts))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testExportSettings() {
        // Test data export settings
        onView(withText("Export Settings"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Test export location
        onView(withText("Export Location"))
            .perform(click())
        
        onView(withText("Internal Storage"))
            .perform(click())
        
        // Test export format
        onView(withText("Export Format"))
            .perform(click())
        
        onView(withText("Excel (.xlsx)"))
            .perform(click())
    }

    @Test
    fun testAdvancedSettings() {
        // Test advanced configuration options
        onView(withText("Advanced Settings"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Test buffer size configuration
        onView(withText("Buffer Size"))
            .perform(click())
        
        onView(withText("1024 samples"))
            .perform(click())
        
        // Test thread priority setting
        onView(withId(R.id.switch_high_priority))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testSettingsPersistence() {
        // Test that settings are saved and persist
        onView(withText("Sampling Rate"))
            .perform(click())
        
        onView(withText("512 Hz"))
            .perform(click())
        
        // Navigate away and back
        pressBack()
        
        // Reopen settings
        onView(withId(R.id.btn_open_settings))
            .perform(click())
        
        // Verify setting persisted
        onView(withText("Sampling Rate: 512 Hz"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsValidation() {
        // Test settings validation and error handling
        onView(withId(R.id.edit_device_name))
            .perform(clearText(), typeText(""))
        
        onView(withId(R.id.btn_save_settings))
            .perform(click())
        
        // Verify validation error
        onView(withText("Device name cannot be empty"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testResetToDefaults() {
        // Test reset to default settings
        onView(withText("Reset to Defaults"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify confirmation dialog
        onView(withText("Reset all settings to default values?"))
            .check(matches(isDisplayed()))
        
        onView(withText("Reset"))
            .perform(click())
        
        // Verify settings are reset
        onView(withText("Sampling Rate: 128 Hz"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testHelpAndInfo() {
        // Test help and information sections
        onView(withText("Help"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("GSR Configuration Guide"))
            .check(matches(isDisplayed()))
        
        // Test about section
        onView(withText("About"))
            .perform(click())
        
        onView(withText("BucikaGSR"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAccessibilityInSettings() {
        // Test accessibility features in settings
        onView(withText("Sampling Rate"))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.switch_auto_backup))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testSettingsNavigation() {
        // Test navigation within settings
        onView(withText("Data Logging"))
            .perform(click())
        
        onView(withId(R.id.toolbar_back))
            .perform(click())
        
        onView(withText("GSR Settings"))
            .check(matches(isDisplayed()))
    }
