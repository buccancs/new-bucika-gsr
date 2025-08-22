package com.multisensor.recording.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multisensor.recording.R
import com.multisensor.recording.testbase.BaseUiIntegrationTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityIntegrationTest : BaseUiIntegrationTest() {

    @Test
    fun mainActivity_should_launch_successfully() {
        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_should_display_toolbar() {
        waitForUiIdle()

        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_should_show_bottom_navigation() {
        waitForUiIdle()

        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_should_show_navigation_drawer() {
        waitForUiIdle()

        onView(withId(R.id.nav_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_should_display_app_bar() {
        waitForUiIdle()

        onView(withId(R.id.appBarLayout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavigation_should_be_clickable() {
        waitForUiIdle()

        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun activity_should_handle_configuration_changes() {
        waitForUiIdle()

        activityRule.scenario.recreate()
        waitForUiIdle()

        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))

        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun navigation_drawer_should_be_accessible() {
        waitForUiIdle()

        onView(withId(R.id.drawer_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.nav_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun activity_should_survive_background_foreground_cycle() {
        waitForUiIdle()

        activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        waitForUiIdle()

        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun fragment_container_should_be_visible() {
        waitForUiIdle()

        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))
    }
}
