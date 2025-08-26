package com.topdon.tc001.thermal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import android.content.Intent
import android.content.Context
import android.hardware.usb.UsbManager
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import com.topdon.tc001.ThermalActivity
import com.topdon.tc001.R

@RunWith(AndroidJUnit4::class)
class ThermalIntegrationTest {
    
    @get:Rule
    val activityRule = ActivityTestRule(ThermalActivity::class.java, false, false)
    
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    @Test
    fun testThermalActivityLaunchAndBasicUI() {

        val intent = Intent()
        val activity = activityRule.launchActivity(intent)
        
        onView(withId(R.id.temperature_view))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_connect_camera))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_start_recording))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_thermal_settings))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testThermalSettingsNavigation() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(1000)
        
        onView(withId(R.id.seekbar_emissivity))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.spinner_pseudocolor))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.edittext_ambient_temp))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testThermalMeasurementToolsUI() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.checkbox_spot_meter))
            .perform(click())
        
        onView(withId(R.id.spot_meter_crosshair))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_add_temp_area))
            .perform(click())
        
        onView(withId(R.id.temperature_view))
            .perform(longClick())
        
        onView(withId(R.id.temp_area_controls))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testRecordingWorkflow() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        onView(withId(R.id.text_recording_status))
            .check(matches(withText(containsString("Recording"))))
        
        onView(withId(R.id.btn_start_recording))
            .check(matches(withText("Stop Recording")))
        
        Thread.sleep(2000)
        
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        onView(withId(R.id.text_recording_status))
            .check(matches(withText(containsString("Stopped"))))
        
        onView(withId(R.id.btn_start_recording))
            .check(matches(withText("Start Recording")))
    }
    
    @Test
    fun testDataExportFlow() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        Thread.sleep(3000)
        
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        onView(withId(R.id.btn_export_data))
            .perform(click())
        
        onView(withText("Export Format"))
            .check(matches(isDisplayed()))
        
        onView(withText("CSV"))
            .perform(click())
        
        onView(withText("Export"))
            .perform(click())
        
        Thread.sleep(2000)
        
        onView(withText(containsString("exported")))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testThermalConfigurationPersistence() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(500)
        
        onView(withId(R.id.seekbar_emissivity))
            .perform(click())
        
        onView(withId(R.id.spinner_pseudocolor))
            .perform(click())
        
        onView(withText("Rainbow"))
            .perform(click())
        
        onView(withId(R.id.edittext_ambient_temp))
            .perform(clearText(), typeText("30"))
        
        onView(withId(R.id.btn_apply_settings))
            .perform(click())
        
        onView(withId(R.id.btn_back))
            .perform(click())
        
        Thread.sleep(500)
        
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(500)
        
        onView(withId(R.id.edittext_ambient_temp))
            .check(matches(withText("30")))
    }
    
    @Test
    fun testErrorHandlingForMissingDevice() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.btn_connect_camera))
            .perform(click())
        
        onView(withText(containsString("No thermal camera found")))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.text_connection_status))
            .check(matches(withText(containsString("Not Connected"))))
    }
    
    @Test
    fun testTemperatureDisplayUpdates() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.checkbox_spot_meter))
            .perform(click())
        
        onView(withId(R.id.text_spot_temperature))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.text_spot_temperature))
            .check(matches(withText(containsString("°C"))))
    }
    
    @Test
    fun testPseudocolorModeChanges() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(500)
        
        val modes = listOf("Iron", "Rainbow", "White Hot", "Black Hot")
        
        modes.forEach { mode ->

            onView(withId(R.id.spinner_pseudocolor))
                .perform(click())
            
            onView(withText(mode))
                .perform(click())
            
            onView(withId(R.id.btn_apply_settings))
                .perform(click())
            
            Thread.sleep(500)
        }
    }
    
    @Test
    fun testMemoryLeakPrevention() {
        val intent = Intent()
        val activity = activityRule.launchActivity(intent)
        
        repeat(10) {

            onView(withId(R.id.btn_start_recording))
                .perform(click())
            
            Thread.sleep(1000)
            
            onView(withId(R.id.btn_start_recording))
                .perform(click())
            
            Thread.sleep(500)
            
            onView(withId(R.id.btn_thermal_settings))
                .perform(click())
            
            Thread.sleep(500)
            
            onView(withId(R.id.btn_back))
                .perform(click())
            
            Thread.sleep(500)
        }
        
        onView(withId(R.id.temperature_view))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testUIResponsivenessDuringProcessing() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(1000)
        
        onView(withId(R.id.seekbar_emissivity))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_back))
            .perform(click())
        
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        onView(withId(R.id.temperature_view))
            .check(matches(isDisplayed()))
    }
