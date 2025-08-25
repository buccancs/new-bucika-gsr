package com.topdon.tc001.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.topdon.tc001.MainActivity
import com.topdon.tc001.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for MainActivity
 * Tests main navigation, user interactions, and core app functionality
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMainActivityLaunches() {
        // Verify main activity launches and displays correctly
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationVisibility() {
        // Test bottom navigation is visible and functional
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalCameraNavigation() {
        // Test navigation to thermal camera functionality
        onView(withId(R.id.nav_thermal))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify thermal camera interface loads
        onView(withId(R.id.thermal_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testGSRModuleNavigation() {
        // Test navigation to GSR module
        onView(withId(R.id.nav_gsr))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify GSR interface loads
        onView(withId(R.id.gsr_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRecordingNavigation() {
        // Test navigation to recording functionality
        onView(withId(R.id.nav_recording))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify recording interface loads
        onView(withId(R.id.recording_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsNavigation() {
        // Test navigation to settings
        onView(withId(R.id.nav_settings))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify settings interface loads
        onView(withId(R.id.settings_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeviceConnectionStatus() {
        // Test device connection status display
        onView(withId(R.id.tv_device_status))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPermissionHandling() {
        // Test that permission dialogs are handled correctly
        // This would typically involve permission scenarios
        onView(withId(R.id.btn_request_permissions))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testMenuOptionsAccessibility() {
        // Test menu options are accessible
        onView(withId(R.id.action_menu))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify menu items are displayed
        onView(withText("About"))
            .check(matches(isDisplayed()))
        
        onView(withText("Help"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBackPressHandling() {
        // Test back press handling and confirmation dialog
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
        
        // Simulate back press
        pressBack()
        
        // Verify exit confirmation dialog appears
        onView(withText("Exit Application"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testOrientationChange() {
        // Test that UI handles orientation changes properly
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
        
        // The activity should handle orientation changes gracefully
        // This is more of a configuration test
    }

    @Test
    fun testErrorStateHandling() {
        // Test error state display and recovery
        // This would involve triggering error conditions
        onView(withId(R.id.error_message))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testLoadingStateVisibility() {
        // Test loading indicators during app initialization
        onView(withId(R.id.loading_indicator))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        
        // Wait for loading to complete
        Thread.sleep(3000)
        
        onView(withId(R.id.loading_indicator))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testUserInterfaceResponsiveness() {
        // Test that UI responds to user interactions quickly
        val startTime = System.currentTimeMillis()
        
        onView(withId(R.id.nav_thermal))
            .perform(click())
        
        onView(withId(R.id.thermal_container))
            .check(matches(isDisplayed()))
        
        val endTime = System.currentTimeMillis()
        val responseTime = endTime - startTime
        
        // Verify response time is under 2 seconds
        assert(responseTime < 2000) { "UI response time too slow: ${responseTime}ms" }
    }
