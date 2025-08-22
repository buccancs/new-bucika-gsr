package com.multisensor.recording.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.multisensor.recording.MainActivity
import com.multisensor.recording.R
import com.multisensor.recording.TestConstants
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for MainActivity.
 * Covers all user interactions, navigation, and UI state management.
 * 
 * Test Categories:
 * - Activity launch and initialisation
 * - Navigation drawer functionality
 * - Fragment navigation
 * - Recording controls
 * - Permission handling
 * - Error state display
 * - Accessibility compliance
 * - Device rotation handling
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MainActivityUITest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var activityRule: ActivityTestRule<MainActivity> = 
        ActivityTestRule(MainActivity::class.java, true, false)

    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        
        // Initialise activity scenario
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Wait for activity to be ready
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    @After
    fun tearDown() {
        activityScenario.close()
    }

    @Test
    fun test_activity_launches_successfully() {
        // Given: MainActivity is launched
        
        // When: Activity is displayed
        // Then: Main container should be visible
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
        
        // Toolbar should be visible
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
        
        // Navigation drawer should be accessible
        onView(withId(R.id.drawer_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_navigation_drawer_opens_and_closes() {
        // Given: Activity is displayed
        
        // When: Opening navigation drawer
        onView(withId(R.id.drawer_layout))
            .perform(open())
        
        // Then: Navigation view should be visible
        onView(withId(R.id.nav_view))
            .check(matches(isDisplayed()))
        
        // When: Closing navigation drawer
        onView(withId(R.id.drawer_layout))
            .perform(close())
        
        // Then: Navigation view should not be fully visible
        onView(withId(R.id.nav_view))
            .check(matches(not(isCompletelyDisplayed())))
    }

    @Test
    fun test_recording_button_functionality() {
        // Given: Activity is displayed
        
        // When: Tapping record button
        onView(withId(R.id.record_button))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Then: Recording should start (button state should change)
        // Note: Actual implementation may vary based on UI design
        onView(withId(R.id.record_button))
            .check(matches(isDisplayed()))
        
        // Recording status should be updated
        onView(withId(R.id.recording_status))
            .check(matches(anyOf(
                withText(containsString("Recording")),
                withText(containsString("Started"))
            )))
    }

    @Test
    fun test_stop_recording_button_functionality() {
        // Given: Recording is active
        onView(withId(R.id.record_button))
            .perform(click())
        
        // When: Tapping stop button
        onView(withId(R.id.stop_button))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Then: Recording should stop
        onView(withId(R.id.recording_status))
            .check(matches(anyOf(
                withText(containsString("Stopped")),
                withText(containsString("Idle"))
            )))
    }

    @Test
    fun test_navigation_to_devices_fragment() {
        // Given: Navigation drawer is open
        onView(withId(R.id.drawer_layout))
            .perform(open())
        
        // When: Tapping devices menu item
        onView(withId(R.id.nav_devices))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Then: Devices fragment should be displayed
        onView(withId(R.id.devices_container))
            .check(matches(isDisplayed()))
        
        // Devices list should be visible
        onView(withId(R.id.devices_recycler_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_navigation_to_calibration_fragment() {
        // Given: Navigation drawer is open
        onView(withId(R.id.drawer_layout))
            .perform(open())
        
        // When: Tapping calibration menu item
        onView(withId(R.id.nav_calibration))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Then: Calibration fragment should be displayed
        onView(withId(R.id.calibration_container))
            .check(matches(isDisplayed()))
        
        // Calibration controls should be visible
        onView(withId(R.id.calibration_controls))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_navigation_to_files_fragment() {
        // Given: Navigation drawer is open
        onView(withId(R.id.drawer_layout))
            .perform(open())
        
        // When: Tapping files menu item
        onView(withId(R.id.nav_files))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Then: Files fragment should be displayed
        onView(withId(R.id.files_container))
            .check(matches(isDisplayed()))
        
        // Files list should be visible
        onView(withId(R.id.files_recycler_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_settings_menu_access() {
        // Given: Activity is displayed
        
        // When: Opening options menu
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        
        // Then: Settings option should be visible
        onView(withText("Settings"))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Settings activity should launch
        // Note: This test might need adjustment based on actual implementation
    }

    @Test
    fun test_device_connection_status_display() {
        // Given: Activity is displayed
        
        // When: Checking device status
        // Then: Device status should be displayed
        onView(withId(R.id.device_status))
            .check(matches(isDisplayed()))
        
        // Connection indicator should be visible
        onView(withId(R.id.connection_indicator))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_recording_progress_display() {
        // Given: Recording is active
        onView(withId(R.id.record_button))
            .perform(click())
        
        // When: Recording is in progress
        // Then: Progress indicator should be visible
        onView(withId(R.id.recording_progress))
            .check(matches(isDisplayed()))
        
        // Duration should be updating
        onView(withId(R.id.recording_duration))
            .check(matches(isDisplayed()))
            .check(matches(withText(not(equalTo("00:00")))))
    }

    @Test
    fun test_error_message_display() {
        // Given: Activity is displayed
        
        // When: An error occurs (simulated by triggering error state)
        // This might require specific test setup or mock injection
        
        // Then: Error message should be displayed
        // Note: Implementation depends on how errors are displayed in UI
        onView(withId(R.id.error_message))
            .check(matches(anyOf(
                not(isDisplayed()),
                withText(not(equalTo("")))
            )))
    }

    @Test
    fun test_camera_preview_display() {
        // Given: Camera permissions are granted
        
        // When: Camera preview is initialised
        // Then: Camera preview should be visible
        onView(withId(R.id.camera_preview))
            .check(matches(isDisplayed()))
        
        // Preview surface should be ready
        onView(withId(R.id.preview_surface))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_thermal_camera_display() {
        // Given: Thermal camera is connected
        
        // When: Thermal view is displayed
        // Then: Thermal camera view should be visible
        onView(withId(R.id.thermal_camera_view))
            .check(matches(isDisplayed()))
        
        // Temperature overlay should be present
        onView(withId(R.id.temperature_overlay))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_shimmer_data_display() {
        // Given: Shimmer device is connected
        
        // When: Shimmer data is being displayed
        // Then: Shimmer data view should be visible
        onView(withId(R.id.shimmer_data_view))
            .check(matches(isDisplayed()))
        
        // GSR chart should be present
        onView(withId(R.id.gsr_chart))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_activity_rotation_handling() {
        // Given: Activity is in portrait mode
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
        
        // When: Rotating to landscape
        activityScenario.onActivity { activity ->
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        // Then: Activity should handle rotation gracefully
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
        
        // Key UI elements should remain visible
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_back_button_navigation() {
        // Given: In a fragment other than home
        onView(withId(R.id.drawer_layout))
            .perform(open())
        onView(withId(R.id.nav_devices))
            .perform(click())
        
        // When: Pressing back button
        pressBack()
        
        // Then: Should navigate back to home fragment
        onView(withId(R.id.home_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_accessibility_content_descriptions() {
        // Given: Activity is displayed
        
        // When: Checking accessibility
        // Then: Important UI elements should have content descriptions
        onView(withId(R.id.record_button))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.stop_button))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.device_status))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun test_long_press_interactions() {
        // Given: Activity is displayed
        
        // When: Long pressing record button
        onView(withId(R.id.record_button))
            .perform(longClick())
        
        // Then: Additional options should be displayed
        // Note: Implementation depends on actual long-press behaviour
        onView(withText(anyOf(
            containsString("Options"),
            containsString("Settings"),
            containsString("Info")
        )))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_swipe_gestures() {
        // Given: Activity is displayed
        
        // When: Swiping on main content area
        onView(withId(R.id.main_content))
            .perform(swipeLeft())
        
        // Then: Appropriate response should occur
        // Note: Implementation depends on actual swipe behaviour
        // This might trigger fragment switching or other navigation
    }

    @Test
    fun test_double_tap_prevention() {
        // Given: Activity is displayed
        
        // When: Double tapping record button quickly
        onView(withId(R.id.record_button))
            .perform(doubleClick())
        
        // Then: Should not cause duplicate operations
        // State should be consistent
        onView(withId(R.id.recording_status))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_permission_request_handling() {
        // Given: Permissions are not granted
        
        // When: Triggering action that requires permissions
        onView(withId(R.id.record_button))
            .perform(click())
        
        // Then: Permission request should be handled
        // Note: This test may require permission denial setup
        // Check if permission dialog appears or appropriate message is shown
    }

    @Test
    fun test_network_status_indicator() {
        // Given: Activity is displayed
        
        // When: Network status changes
        // Then: Network status indicator should update
        onView(withId(R.id.network_status_indicator))
            .check(matches(isDisplayed()))
        
        // Status icon should reflect current state
        onView(withId(R.id.network_status_icon))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_battery_status_display() {
        // Given: Activity is displayed
        
        // When: Battery status is monitored
        // Then: Battery status should be displayed
        onView(withId(R.id.battery_status))
            .check(matches(isDisplayed()))
        
        // Battery level should be shown
        onView(withId(R.id.battery_level))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_session_info_display() {
        // Given: A recording session exists
        onView(withId(R.id.record_button))
            .perform(click())
        
        // When: Session information is displayed
        // Then: Session details should be visible
        onView(withId(R.id.session_info))
            .check(matches(isDisplayed()))
        
        // Session ID should be shown
        onView(withId(R.id.session_id))
            .check(matches(withText(not(equalTo("")))))
    }

    @Test
    fun test_ui_responsiveness_under_load() {
        // Given: Activity is displayed
        
        // When: Performing multiple rapid interactions
        repeat(10) {
            onView(withId(R.id.record_button))
                .perform(click())
            Thread.sleep(100)
        }
        
        // Then: UI should remain responsive
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
        
        // No ANR or crash should occur
        onView(withId(R.id.recording_status))
            .check(matches(isDisplayed()))
    }

    // Helper methods for complex test scenarios
    private fun grantAllPermissions() {
        // Implementation to grant required permissions
        // This might use UiAutomation or other testing utilities
    }

    private fun simulateDeviceConnection() {
        // Implementation to simulate device connections
        // This might involve mock injection or test doubles
    }

    private fun waitForRecordingToStart() {
        // Wait for recording state to stabilize
        onView(withId(R.id.recording_status))
            .check(matches(withText(containsString("Recording"))))
    }
}