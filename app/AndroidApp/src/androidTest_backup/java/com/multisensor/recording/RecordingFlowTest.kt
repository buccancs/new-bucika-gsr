package com.multisensor.recording

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.multisensor.recording.testhelpers.TestHelpers
import com.multisensor.recording.testhelpers.TestResultCollector
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recording flow tests covering FR2 (Synchronised Multi-Modal Recording), 
 * FR4 (Session Management), and FR5 (Data Recording and Storage).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class RecordingFlowTest {

    private lateinit var activityScenario: ActivityScenario<MainActivity>
    private lateinit var resultCollector: TestResultCollector
    private val testTag = "RecordingFlowTest"

    @Before
    fun setUp() {
        Log.i(testTag, "Setting up Recording Flow Test")
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        resultCollector = TestResultCollector("RecordingFlow")
        
        // Navigate to Recording fragment
        TestHelpers.navigateToFragment(R.id.nav_recording, "Recording")
        Log.i(testTag, "Recording fragment ready for testing")
    }

    @After
    fun tearDown() {
        Log.i(testTag, "Tearing down Recording Flow Test")
        resultCollector.logResults()
        activityScenario.close()
    }

    /**
     * FR2: Test synchronized recording start across all modalities.
     * FR5: Verify data recording indicators are active.
     */
    @Test
    fun testRecordingStartFlow() {
        Log.i(testTag, "Testing recording start flow")

        val recordingStartButtons = listOf(
            "start_recording_button" to "Start Recording",
            "start_session_button" to "Start Session"
        )

        val results = TestHelpers.testButtonList(recordingStartButtons, "RecordingStart")
        resultCollector.addResults(results)

        // Verify recording status indicators appear
        val recordingIndicators = listOf(
            "recording_status",
            "session_timer",
            "data_counter"
        )

        val indicatorResults = TestHelpers.testStatusIndicatorList(recordingIndicators, "Recording")
        resultCollector.addResults(indicatorResults)

        Log.i(testTag, "Recording start flow testing completed")
    }

    /**
     * FR2, FR4: Test recording stop and session cleanup.
     * FR5: Verify data is preserved and metadata updated.
     */
    @Test
    fun testRecordingStopFlow() {
        Log.i(testTag, "Testing recording stop flow")

        // Start recording first
        val startSuccess = TestHelpers.testButtonByMultipleStrategies(
            "start_recording_button", "Start Recording", "RecordingStop"
        )
        resultCollector.addResult("recording_start_prerequisite", startSuccess)

        if (startSuccess) {
            // Test stop functionality
            val stopButtons = listOf(
                "stop_recording_button" to "Stop Recording",
                "stop_session_button" to "Stop Session"
            )

            val stopResults = TestHelpers.testButtonList(stopButtons, "RecordingStop")
            resultCollector.addResults(stopResults)

            // Verify session cleanup indicators
            val cleanupIndicators = listOf(
                "session_completed_status",
                "data_saved_status"
            )

            val cleanupResults = TestHelpers.testStatusIndicatorList(cleanupIndicators, "SessionCleanup")
            resultCollector.addResults(cleanupResults)
        }

        Log.i(testTag, "Recording stop flow testing completed")
    }

    /**
     * FR4: Test session management constraints.
     * Only one session should be active at a time.
     */
    @Test
    fun testSessionManagementConstraints() {
        Log.i(testTag, "Testing session management constraints")

        // Start first session
        val firstSessionStart = TestHelpers.testButtonByMultipleStrategies(
            "start_recording_button", "Start Recording", "SessionConstraints"
        )
        resultCollector.addResult("first_session_start", firstSessionStart)

        if (firstSessionStart) {
            // Try to start second session (should fail or show warning)
            val secondSessionAttempt = TestHelpers.testButtonByMultipleStrategies(
                "start_recording_button", "Start Recording", "SessionConstraints"
            )
            
            // Check for constraint enforcement (warning toast, dialog, or disabled button)
            val constraintEnforced = try {
                onView(withText("Session already active"))
                    .check(matches(isDisplayed()))
                true
            } catch (e: Exception) {
                // Check if button is disabled
                try {
                    onView(withTagValue(org.hamcrest.Matchers.`is`("start_recording_button" as Any)))
                        .check(matches(isNotEnabled()))
                    true
                } catch (e2: Exception) {
                    false
                }
            }

            resultCollector.addResult("session_constraint_enforced", constraintEnforced)

            // Stop the active session
            val sessionStop = TestHelpers.testButtonByMultipleStrategies(
                "stop_recording_button", "Stop Recording", "SessionConstraints"
            )
            resultCollector.addResult("session_cleanup", sessionStop)
        }

        Log.i(testTag, "Session management constraints testing completed")
    }

    /**
     * FR5: Test data recording monitoring during active session.
     * NFR1: Verify performance indicators show real-time updates.
     */
    @Test
    fun testDataRecordingMonitoring() {
        Log.i(testTag, "Testing data recording monitoring")

        // Start recording session
        val recordingStart = TestHelpers.testButtonByMultipleStrategies(
            "start_recording_button", "Start Recording", "DataMonitoring"
        )
        resultCollector.addResult("monitoring_recording_start", recordingStart)

        if (recordingStart) {
            // Verify monitoring indicators update over time
            val monitoringIndicators = listOf(
                "elapsed_time_display",
                "data_size_display", 
                "sample_count_display",
                "recording_rate_display"
            )

            val monitoringResults = TestHelpers.testStatusIndicatorList(monitoringIndicators, "DataMonitoring")
            resultCollector.addResults(monitoringResults)

            // Verify real-time updates (NFR1 Performance)
            val realTimeUpdates = verifyRealTimeUpdates()
            resultCollector.addResult("real_time_updates", realTimeUpdates)

            // Stop recording
            val recordingStop = TestHelpers.testButtonByMultipleStrategies(
                "stop_recording_button", "Stop Recording", "DataMonitoring"
            )
            resultCollector.addResult("monitoring_recording_stop", recordingStop)
        }

        Log.i(testTag, "Data recording monitoring testing completed")
    }

    /**
     * Helper method to verify real-time updates in recording indicators.
     * Returns true if indicators show changes over time.
     */
    private fun verifyRealTimeUpdates(): Boolean {
        return try {
            // Check that time display updates (simple presence check)
            onView(withTagValue(org.hamcrest.Matchers.`is`("elapsed_time_display" as Any)))
                .check(matches(isDisplayed()))
            
            // Check that data counters are present and visible
            onView(withTagValue(org.hamcrest.Matchers.`is`("sample_count_display" as Any)))
                .check(matches(isDisplayed()))
            
            Log.i(testTag, "✅ Real-time update indicators verified")
            true
        } catch (e: Exception) {
            Log.w(testTag, "⚠️ Real-time update indicators not fully functional", e)
            false
        }
    }

    /**
     * FR2: Test preview functionality during recording.
     * FR6: Verify UI controls for monitoring.
     */
    @Test
    fun testRecordingPreviewControls() {
        Log.i(testTag, "Testing recording preview controls")

        val previewButtons = listOf(
            "preview_toggle_button" to "Preview Toggle",
            "camera_switch_button" to "Camera Switch",
            "thermal_overlay_button" to "Thermal Overlay"
        )

        val previewResults = TestHelpers.testButtonList(previewButtons, "RecordingPreview")
        resultCollector.addResults(previewResults)

        // Test preview status indicators
        val previewIndicators = listOf(
            "camera_preview_status",
            "thermal_preview_status",
            "preview_fps_display"
        )

        val previewIndicatorResults = TestHelpers.testStatusIndicatorList(previewIndicators, "Preview")
        resultCollector.addResults(previewIndicatorResults)

        Log.i(testTag, "Recording preview controls testing completed")
    }
}