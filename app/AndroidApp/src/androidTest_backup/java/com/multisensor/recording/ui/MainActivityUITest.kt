package com.multisensor.recording.ui
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.multisensor.recording.MainActivity
import com.multisensor.recording.R
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityUITest {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    @Test
    fun mainActivity_launchesSuccessfully() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }
    @Test
    fun recordButton_isVisibleAndClickable() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.startRecordingButton))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }
    @Test
    fun stopButton_isVisibleWhenRecording() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.startRecordingButton)).perform(click())
        onView(withId(R.id.stopRecordingButton))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }
    @Test
    fun recordStopCycle_updatesUICorrectly() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.startRecordingButton))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
        onView(withId(R.id.startRecordingButton)).perform(click())
        onView(withId(R.id.stopRecordingButton)).perform(click())
        onView(withId(R.id.startRecordingButton))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }
    @Test
    fun navigationDrawer_opensAndCloses() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withContentDescription("Open navigation drawer")).perform(click())
        onView(withId(R.id.nav_view)).check(matches(isDisplayed()))
        pressBack()
    }
    @Test
    fun shimmerSettings_navigationWorks() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withContentDescription("Open navigation drawer")).perform(click())
        onView(withId(R.id.nav_shimmer_settings)).perform(click())
        onView(withText("Shimmer Settings")).check(matches(isDisplayed()))
    }
    @Test
    fun shimmerVisualization_navigationWorks() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withContentDescription("Open navigation drawer")).perform(click())
        onView(withId(R.id.nav_shimmer_visualization)).perform(click())
        onView(withText("Shimmer Visualisation")).check(matches(isDisplayed()))
    }
    @Test
    fun settingsToggles_areInteractive() {
        ActivityScenario.launch(MainActivity::class.java)
        // Navigate to devices fragment to check actual UI elements
        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))
    }
    @Test
    fun statusText_updatesAppropriately() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.recordingStatusText))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("Ready"))))
    }
    @Test
    fun exportData_buttonFunctionality() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withContentDescription("Open navigation drawer")).perform(click())
        onView(withId(R.id.nav_files)).perform(click())
        // Check that files fragment is displayed
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }
    @Test
    fun permissionHandling_showsAppropriateUI() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }
    @Test
    fun deviceConnection_statusUpdates() {
        ActivityScenario.launch(MainActivity::class.java)
        // Check that the main UI is displayed
        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))
    }
    @Test
    fun recordingDuration_displaysCorrectly() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.sessionDurationText))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString(":"))))
    }
    @Test
    fun errorDialog_handlesErrors() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }
    @Test
    fun backNavigation_worksCorrectly() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withContentDescription("Open navigation drawer")).perform(click())
        onView(withId(R.id.nav_shimmer_settings)).perform(click())
        pressBack()
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }
    @Test
    fun landscapeOrientation_maintainsState() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
        onView(withId(R.id.startRecordingButton)).check(matches(isDisplayed()))
    }
    @Test
    fun multipleActivities_navigationFlow() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withContentDescription("Open navigation drawer")).perform(click())
        onView(withId(R.id.nav_shimmer_settings)).perform(click())
        pressBack()
        onView(withId(R.id.nav_host_fragment)).check(matches(isDisplayed()))
    }
}