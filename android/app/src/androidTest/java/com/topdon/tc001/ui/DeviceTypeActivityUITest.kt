package com.topdon.tc001.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.topdon.tc001.DeviceTypeActivity
import com.topdon.tc001.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for DeviceTypeActivity
 * Tests device selection, connection workflows, and device-specific UI
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DeviceTypeActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(DeviceTypeActivity::class.java)

    @Test
    fun testDeviceSelectionScreen() {
        // Verify device selection screen displays
        onView(withId(R.id.device_selection_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalDeviceSelection() {
        // Test thermal camera device selection
        onView(withId(R.id.card_thermal_device))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify thermal device configuration appears
        onView(withText("Thermal Camera Configuration"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testGSRDeviceSelection() {
        // Test GSR device (Shimmer) selection
        onView(withId(R.id.card_gsr_device))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify GSR device configuration appears
        onView(withText("GSR Device Configuration"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCombinedDeviceMode() {
        // Test combined device mode selection
        onView(withId(R.id.card_combined_mode))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify combined mode configuration
        onView(withText("Combined Mode Setup"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeviceConnectionWizard() {
        // Test device connection wizard
        onView(withId(R.id.card_thermal_device))
            .perform(click())
        
        onView(withId(R.id.btn_next))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify connection instructions
        onView(withText("Connect your thermal camera via USB"))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_test_connection))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testDeviceCompatibilityCheck() {
        // Test device compatibility checking
        onView(withId(R.id.btn_check_compatibility))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify compatibility results
        onView(withId(R.id.compatibility_results))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeviceInfoDisplay() {
        // Test device information display
        onView(withId(R.id.card_thermal_device))
            .perform(longClick())
        
        // Verify device info dialog
        onView(withText("Thermal Camera Information"))
            .check(matches(isDisplayed()))
        
        onView(withText("Supported resolutions"))
            .check(matches(isDisplayed()))
        
        onView(withText("Temperature range"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testHelpAndTutorial() {
        // Test help and tutorial access
        onView(withId(R.id.btn_help))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("Device Setup Guide"))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_start_tutorial))
            .check(matches(isDisplayed()))
            .perform(click())
    }
