package com.topdon.tc001.recording.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.topdon.tc001.recording.EnhancedRecordingActivity
import com.topdon.tc001.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for EnhancedRecordingActivity
 * Tests recording functionality, data capture, and file management
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EnhancedRecordingActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(EnhancedRecordingActivity::class.java)

    @Test
    fun testRecordingActivityLaunches() {
        // Verify recording activity launches correctly
        onView(withId(R.id.recording_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testStartRecordingButton() {
        // Test start recording functionality
        onView(withId(R.id.btn_start_recording))
            .check(matches(isDisplayed()))
            .check(matches(withText("Start Recording")))
            .perform(click())
        
        // Verify recording state changes
        onView(withId(R.id.btn_start_recording))
            .check(matches(withText("Stop Recording")))
        
        // Verify recording indicator appears
        onView(withId(R.id.recording_indicator))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRecordingTimer() {
        // Test recording timer functionality
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Verify timer starts
        onView(withId(R.id.tv_recording_timer))
            .check(matches(isDisplayed()))
            .check(matches(withText("00:00:00")))
        
        // Wait for timer to update
        Thread.sleep(2000)
        
        onView(withId(R.id.tv_recording_timer))
            .check(matches(withText(containsString("00:00:0"))))
    }

    @Test
    fun testStopRecording() {
        // Test stop recording functionality
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Let recording run briefly
        Thread.sleep(1000)
        
        // Stop recording
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Verify recording stops
        onView(withId(R.id.recording_indicator))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        
        onView(withId(R.id.btn_start_recording))
            .check(matches(withText("Start Recording")))
    }

    @Test
    fun testRecordingSettings() {
        // Test recording settings configuration
        onView(withId(R.id.btn_recording_settings))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify settings panel opens
        onView(withId(R.id.recording_settings_panel))
            .check(matches(isDisplayed()))
        
        // Test quality setting
        onView(withText("Recording Quality"))
            .perform(click())
        
        onView(withText("High Quality"))
            .perform(click())
    }

    @Test
    fun testDataSourceSelection() {
        // Test data source selection for recording
        onView(withId(R.id.spinner_data_sources))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify data source options
        onView(withText("GSR Only"))
            .check(matches(isDisplayed()))
        
        onView(withText("Thermal Only"))
            .check(matches(isDisplayed()))
        
        onView(withText("Combined GSR + Thermal"))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testFileNamingConfiguration() {
        // Test file naming configuration
        onView(withId(R.id.edit_filename_prefix))
            .check(matches(isDisplayed()))
            .perform(clearText(), typeText("Test_Session"))
        
        onView(withId(R.id.switch_timestamp_suffix))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testRecordingProgress() {
        // Test recording progress display
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Verify progress indicators
        onView(withId(R.id.progress_data_captured))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_data_rate))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_file_size))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPauseResumeRecording() {
        // Test pause/resume functionality
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Pause recording
        onView(withId(R.id.btn_pause_recording))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify paused state
        onView(withId(R.id.recording_status))
            .check(matches(withText("Paused")))
        
        // Resume recording
        onView(withId(R.id.btn_resume_recording))
            .perform(click())
        
        onView(withId(R.id.recording_status))
            .check(matches(withText("Recording")))
    }

    @Test
    fun testRecordingStatistics() {
        // Test recording statistics display
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        Thread.sleep(2000)
        
        // Verify statistics are displayed
        onView(withId(R.id.tv_samples_recorded))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_average_sample_rate))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_buffer_usage))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRecordingFileList() {
        // Test recorded files list
        onView(withId(R.id.btn_view_recordings))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify file list appears
        onView(withId(R.id.recordings_recycler_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testFileExportOptions() {
        // Test file export functionality
        onView(withId(R.id.btn_export_recording))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Verify export options dialog
        onView(withText("Export Options"))
            .check(matches(isDisplayed()))
        
        onView(withText("CSV Format"))
            .check(matches(isDisplayed()))
        
        onView(withText("Excel Format"))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testStorageMonitoring() {
        // Test storage space monitoring
        onView(withId(R.id.tv_storage_available))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.progress_storage_usage))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testErrorHandling() {
        // Test error handling in recording
        // This would simulate various error conditions
        onView(withId(R.id.error_message_container))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testRecordingNotifications() {
        // Test recording notifications
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Verify notification settings
        onView(withId(R.id.switch_recording_notifications))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testAutoStopConfiguration() {
        // Test auto-stop recording configuration
        onView(withId(R.id.btn_recording_settings))
            .perform(click())
        
        onView(withText("Auto Stop"))
            .perform(click())
        
        onView(withText("After 30 minutes"))
            .perform(click())
        
        // Test duration setting
        onView(withId(R.id.edit_max_duration))
            .perform(clearText(), typeText("1800")) // 30 minutes
    }

    @Test
    fun testRecordingQualitySettings() {
        // Test recording quality and format settings
        onView(withId(R.id.btn_recording_settings))
            .perform(click())
        
        onView(withText("Quality Settings"))
            .perform(click())
        
        onView(withText("Sample Rate"))
            .perform(click())
        
        onView(withText("1000 Hz"))
            .perform(click())
        
        onView(withText("Compression"))
            .perform(click())
        
        onView(withText("None"))
            .perform(click())
    }

    @Test
    fun testComplexRecordingFlow() {
        // Test complex recording workflow
        // 1. Configure settings
        onView(withId(R.id.btn_recording_settings))
            .perform(click())
        
        onView(withText("Combined GSR + Thermal"))
            .perform(click())
        
        // 2. Set filename
        onView(withId(R.id.edit_filename_prefix))
            .perform(clearText(), typeText("Complex_Test"))
        
        // 3. Start recording
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // 4. Verify all components work together
        onView(withId(R.id.recording_indicator))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_recording_timer))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_data_rate))
            .check(matches(isDisplayed()))
        
        // 5. Stop recording
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // 6. Verify file is created
        onView(withId(R.id.btn_view_recordings))
            .perform(click())
        
        onView(withText(containsString("Complex_Test")))
            .check(matches(isDisplayed()))
    }
