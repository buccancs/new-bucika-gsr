package com.topdon.tc001.gsr.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.topdon.tc001.gsr.GSRActivity
import com.topdon.tc001.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GSRActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(GSRActivity::class.java)

    @Test
    fun testGSRActivityLaunch() {

        onView(withId(R.id.main_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testConnectButtonFunctionality() {

        onView(withId(R.id.btn_connect_device))
            .check(matches(isDisplayed()))
            .perform(click())
        
    }

    @Test
    fun testStartRecordingButton() {

        onView(withId(R.id.btn_start_recording))
            .check(matches(isDisplayed()))
            .perform(click())
        
    }

    @Test
    fun testGSRSettingsAccess() {

        onView(withId(R.id.btn_gsr_settings))
            .check(matches(isDisplayed()))
            .perform(click())
        
    }

    @Test
    fun testDataVisualization() {

        onView(withId(R.id.gsr_chart_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeviceStatusDisplay() {

        onView(withId(R.id.tv_device_status))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBatteryLevelDisplay() {

        onView(withId(R.id.tv_battery_level))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSignalQualityIndicator() {

        onView(withId(R.id.progress_signal_quality))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDataExportButton() {

        onView(withId(R.id.btn_export_data))
            .check(matches(isDisplayed()))
            .perform(click())
        
    }

    @Test
    fun testMenuNavigation() {

        onView(withId(R.id.menu_button))
            .check(matches(isDisplayed()))
            .perform(click())
        
    }
