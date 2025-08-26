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

@RunWith(AndroidJUnit4::class)
@LargeTest
class GSRSettingsActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(GSRSettingsActivity::class.java)

    @Test
    fun testSettingsActivityLaunches() {

        onView(withId(R.id.settings_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testGSRSamplingRateConfiguration() {

        onView(withText("Sampling Rate"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("128 Hz"))
            .check(matches(isDisplayed()))
        
        onView(withText("256 Hz"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Sampling Rate: 256 Hz"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testShimmerDeviceConfiguration() {

        onView(withText("Shimmer Device Settings"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.edit_device_name))
            .check(matches(isDisplayed()))
            .perform(clearText(), typeText("GSR_Device_01"))
        
        onView(withText("Connection Timeout"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("30 seconds"))
            .perform(click())
    }

    @Test
    fun testDataLoggingSettings() {

        onView(withText("Data Logging"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Log Format"))
            .perform(click())
        
        onView(withText("CSV"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.switch_auto_backup))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testCalibrationSettings() {

        onView(withText("Calibration"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.btn_calibrate_baseline))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Baseline Calibration"))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.slider_sensitivity))
            .check(matches(isDisplayed()))
            .perform(swipeRight())
    }

    @Test
    fun testNotificationSettings() {

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

        onView(withText("Export Settings"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Export Location"))
            .perform(click())
        
        onView(withText("Internal Storage"))
            .perform(click())
        
        onView(withText("Export Format"))
            .perform(click())
        
        onView(withText("Excel (.xlsx)"))
            .perform(click())
    }

    @Test
    fun testAdvancedSettings() {

        onView(withText("Advanced Settings"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Buffer Size"))
            .perform(click())
        
        onView(withText("1024 samples"))
            .perform(click())
        
        onView(withId(R.id.switch_high_priority))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testSettingsPersistence() {

        onView(withText("Sampling Rate"))
            .perform(click())
        
        onView(withText("512 Hz"))
            .perform(click())
        
        pressBack()
        
        onView(withId(R.id.btn_open_settings))
            .perform(click())
        
        onView(withText("Sampling Rate: 512 Hz"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsValidation() {

        onView(withId(R.id.edit_device_name))
            .perform(clearText(), typeText(""))
        
        onView(withId(R.id.btn_save_settings))
            .perform(click())
        
        onView(withText("Device name cannot be empty"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testResetToDefaults() {

        onView(withText("Reset to Defaults"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Reset all settings to default values?"))
            .check(matches(isDisplayed()))
        
        onView(withText("Reset"))
            .perform(click())
        
        onView(withText("Sampling Rate: 128 Hz"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testHelpAndInfo() {

        onView(withText("Help"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("GSR Configuration Guide"))
            .check(matches(isDisplayed()))
        
        onView(withText("About"))
            .perform(click())
        
        onView(withText("BucikaGSR"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAccessibilityInSettings() {

        onView(withText("Sampling Rate"))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.switch_auto_backup))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testSettingsNavigation() {

        onView(withText("Data Logging"))
            .perform(click())
        
        onView(withId(R.id.toolbar_back))
            .perform(click())
        
        onView(withText("GSR Settings"))
            .check(matches(isDisplayed()))
    }
