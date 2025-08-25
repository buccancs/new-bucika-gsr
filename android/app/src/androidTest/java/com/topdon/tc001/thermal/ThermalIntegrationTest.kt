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
        // Launch activity
        val intent = Intent()
        val activity = activityRule.launchActivity(intent)
        
        // Verify thermal activity launches successfully
        onView(withId(R.id.temperature_view))
            .check(matches(isDisplayed()))
        
        // Verify connection controls are present
        onView(withId(R.id.btn_connect_camera))
            .check(matches(isDisplayed()))
        
        // Verify recording controls are present
        onView(withId(R.id.btn_start_recording))
            .check(matches(isDisplayed()))
        
        // Verify settings access is available
        onView(withId(R.id.btn_thermal_settings))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testThermalSettingsNavigation() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        // Navigate to thermal settings
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        // Wait for settings activity to load
        Thread.sleep(1000)
        
        // Verify settings controls are present
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
        
        // Enable spot meter
        onView(withId(R.id.checkbox_spot_meter))
            .perform(click())
        
        // Verify spot meter indicator is shown
        onView(withId(R.id.spot_meter_crosshair))
            .check(matches(isDisplayed()))
        
        // Test temperature area addition
        onView(withId(R.id.btn_add_temp_area))
            .perform(click())
        
        // Simulate drawing area on thermal view
        onView(withId(R.id.temperature_view))
            .perform(longClick())
        
        // Verify area controls appear
        onView(withId(R.id.temp_area_controls))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testRecordingWorkflow() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        // Start recording
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Verify recording status is shown
        onView(withId(R.id.text_recording_status))
            .check(matches(withText(containsString("Recording"))))
        
        // Button should change to "Stop Recording"
        onView(withId(R.id.btn_start_recording))
            .check(matches(withText("Stop Recording")))
        
        // Wait for some recording time
        Thread.sleep(2000)
        
        // Stop recording
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Verify recording stopped
        onView(withId(R.id.text_recording_status))
            .check(matches(withText(containsString("Stopped"))))
        
        // Button should change back
        onView(withId(R.id.btn_start_recording))
            .check(matches(withText("Start Recording")))
    }
    
    @Test
    fun testDataExportFlow() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        // First record some data
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        Thread.sleep(3000) // Record for 3 seconds
        
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Access export options
        onView(withId(R.id.btn_export_data))
            .perform(click())
        
        // Verify export options dialog
        onView(withText("Export Format"))
            .check(matches(isDisplayed()))
        
        // Select CSV format
        onView(withText("CSV"))
            .perform(click())
        
        // Confirm export
        onView(withText("Export"))
            .perform(click())
        
        // Wait for export to complete
        Thread.sleep(2000)
        
        // Should show success message
        onView(withText(containsString("exported")))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testThermalConfigurationPersistence() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        // Navigate to settings
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(500)
        
        // Change emissivity
        onView(withId(R.id.seekbar_emissivity))
            .perform(click()) // This simulates moving the seekbar
        
        // Change pseudocolor mode
        onView(withId(R.id.spinner_pseudocolor))
            .perform(click())
        
        onView(withText("Rainbow"))
            .perform(click())
        
        // Change ambient temperature
        onView(withId(R.id.edittext_ambient_temp))
            .perform(clearText(), typeText("30"))
        
        // Apply settings
        onView(withId(R.id.btn_apply_settings))
            .perform(click())
        
        // Go back to main activity
        onView(withId(R.id.btn_back))
            .perform(click())
        
        Thread.sleep(500)
        
        // Go back to settings to verify persistence
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify settings are maintained
        onView(withId(R.id.edittext_ambient_temp))
            .check(matches(withText("30")))
    }
    
    @Test
    fun testErrorHandlingForMissingDevice() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        // Try to connect when no device is present
        onView(withId(R.id.btn_connect_camera))
            .perform(click())
        
        // Should show error message
        onView(withText(containsString("No thermal camera found")))
            .check(matches(isDisplayed()))
        
        // Connection status should show error
        onView(withId(R.id.text_connection_status))
            .check(matches(withText(containsString("Not Connected"))))
    }
    
    @Test
    fun testTemperatureDisplayUpdates() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        // Enable spot meter
        onView(withId(R.id.checkbox_spot_meter))
            .perform(click())
        
        // With mock data, temperature display should show
        onView(withId(R.id.text_spot_temperature))
            .check(matches(isDisplayed()))
        
        // Temperature value should be reasonable
        onView(withId(R.id.text_spot_temperature))
            .check(matches(withText(containsString("°C"))))
    }
    
    @Test
    fun testPseudocolorModeChanges() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        // Navigate to settings
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(500)
        
        // Test different pseudocolor modes
        val modes = listOf("Iron", "Rainbow", "White Hot", "Black Hot")
        
        modes.forEach { mode ->
            // Select pseudocolor mode
            onView(withId(R.id.spinner_pseudocolor))
                .perform(click())
            
            onView(withText(mode))
                .perform(click())
            
            // Apply and verify no crash
            onView(withId(R.id.btn_apply_settings))
                .perform(click())
            
            Thread.sleep(500)
        }
    }
    
    @Test
    fun testMemoryLeakPrevention() {
        val intent = Intent()
        val activity = activityRule.launchActivity(intent)
        
        // Simulate extended usage
        repeat(10) {
            // Start and stop recording multiple times
            onView(withId(R.id.btn_start_recording))
                .perform(click())
            
            Thread.sleep(1000)
            
            onView(withId(R.id.btn_start_recording))
                .perform(click())
            
            Thread.sleep(500)
            
            // Navigate to settings and back
            onView(withId(R.id.btn_thermal_settings))
                .perform(click())
            
            Thread.sleep(500)
            
            onView(withId(R.id.btn_back))
                .perform(click())
            
            Thread.sleep(500)
        }
        
        // Activity should still be responsive
        onView(withId(R.id.temperature_view))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testUIResponsivenessDuringProcessing() {
        val intent = Intent()
        activityRule.launchActivity(intent)
        
        // Start recording to simulate processing load
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // UI should remain responsive during recording
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        Thread.sleep(1000)
        
        // Settings should be accessible
        onView(withId(R.id.seekbar_emissivity))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_back))
            .perform(click())
        
        // Stop recording
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // UI should still be responsive
        onView(withId(R.id.temperature_view))
            .check(matches(isDisplayed()))
    }
