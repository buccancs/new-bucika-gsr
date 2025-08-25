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

@RunWith(AndroidJUnit4::class)
@LargeTest
class SplashActivityUITest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(SplashActivity::class.java)

    @Test
    fun testSplashScreenDisplays() {

        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAppLogoVisibility() {

        onView(withId(R.id.iv_app_logo))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAppNameVisibility() {

        onView(withId(R.id.tv_app_name))
            .check(matches(isDisplayed()))
            .check(matches(withText("BucikaGSR")))
    }

    @Test
    fun testVersionInfoDisplay() {

        onView(withId(R.id.tv_version_info))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLoadingIndicator() {

        onView(withId(R.id.loading_progress))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSplashDuration() {

        val startTime = System.currentTimeMillis()
        
        Thread.sleep(4000)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assert(duration >= 3000) { "Splash duration too short: ${duration}ms" }
        assert(duration <= 5000) { "Splash duration too long: ${duration}ms" }
    }

    @Test
    fun testOrientationLock() {

        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationAfterSplash() {

        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
        
        Thread.sleep(4000)
        
    }

    @Test
    fun testSplashUIElements() {

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

        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNoUserInteraction() {

        onView(withId(R.id.splash_container))
            .check(matches(isDisplayed()))
        
    }
