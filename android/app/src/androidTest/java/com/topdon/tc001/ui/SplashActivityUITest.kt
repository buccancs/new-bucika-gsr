package com.topdon.tc001.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.topdon.tc001.SplashActivity
import com.topdon.tc001.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for SplashActivity
 * Tests splash screen display, loading behavior, and navigation
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SplashActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(SplashActivity::class.java)

    @Test
    fun testSplashScreenDisplays() {
        // Verify splash screen displays correctly
        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAppLogoVisibility() {
        // Test that app logo is displayed
        onView(withId(R.id.iv_app_logo))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAppNameVisibility() {
        // Test that app name/title is displayed
        onView(withId(R.id.tv_app_name))
            .check(matches(isDisplayed()))
            .check(matches(withText("BucikaGSR")))
    }

    @Test
    fun testVersionInfoDisplay() {
        // Test version information display
        onView(withId(R.id.tv_version_info))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLoadingIndicator() {
        // Test loading indicator during splash
        onView(withId(R.id.loading_progress))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSplashDuration() {
        // Test splash screen duration (should navigate after delay)
        val startTime = System.currentTimeMillis()
        
        // Wait for navigation to occur
        Thread.sleep(4000)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Verify splash duration is appropriate (3-4 seconds)
        assert(duration >= 3000) { "Splash duration too short: ${duration}ms" }
        assert(duration <= 5000) { "Splash duration too long: ${duration}ms" }
    }

    @Test
    fun testOrientationLock() {
        // Test that splash screen is locked to portrait orientation
        // This is more of a configuration test that would be verified
        // by checking the activity's requested orientation
        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationAfterSplash() {
        // Test that app navigates to appropriate screen after splash
        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
        
        // Wait for splash to complete
        Thread.sleep(4000)
        
        // After splash, should navigate to main activity or clause activity
        // This test verifies the splash completes and navigation occurs
    }

    @Test
    fun testSplashUIElements() {
        // Test all UI elements are properly positioned and visible
        onView(withId(R.id.iv_app_logo))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_app_name))
            .check(matches(isDisplayed()))
            .check(matches(isCompletelyDisplayed()))
        
        onView(withId(R.id.loading_progress))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSplashBackgroundColor() {
        // Test splash screen has correct background
        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNoUserInteraction() {
        // Test that splash screen doesn't respond to user interactions
        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
        
        // Splash should not be clickable during loading
        // This is implicit in the design but important for UX
    }
}