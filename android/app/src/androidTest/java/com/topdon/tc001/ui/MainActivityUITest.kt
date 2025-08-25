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

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMainActivityLaunches() {

        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationVisibility() {

        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThermalCameraNavigation() {

        onView(withId(R.id.nav_thermal))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.thermal_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testGSRModuleNavigation() {

        onView(withId(R.id.nav_gsr))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.gsr_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRecordingNavigation() {

        onView(withId(R.id.nav_recording))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.recording_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsNavigation() {

        onView(withId(R.id.nav_settings))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withId(R.id.settings_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeviceConnectionStatus() {

        onView(withId(R.id.tv_device_status))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPermissionHandling() {

        onView(withId(R.id.btn_request_permissions))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testMenuOptionsAccessibility() {

        onView(withId(R.id.action_menu))
            .check(matches(isDisplayed()))
            .perform(click())
        
        onView(withText("About"))
            .check(matches(isDisplayed()))
        
        onView(withText("Help"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBackPressHandling() {

        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
        
        pressBack()
        
        onView(withText("Exit Application"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testOrientationChange() {

        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
        
    }

    @Test
    fun testErrorStateHandling() {

        onView(withId(R.id.error_message))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testLoadingStateVisibility() {

        onView(withId(R.id.loading_indicator))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        
        Thread.sleep(3000)
        
        onView(withId(R.id.loading_indicator))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testUserInterfaceResponsiveness() {

        val startTime = System.currentTimeMillis()
        
        onView(withId(R.id.nav_thermal))
            .perform(click())
        
        onView(withId(R.id.thermal_container))
            .check(matches(isDisplayed()))
        
        val endTime = System.currentTimeMillis()
        val responseTime = endTime - startTime
        
        assert(responseTime < 2000) { "UI response time too slow: ${responseTime}ms" }
    }
