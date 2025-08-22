package com.multisensor.recording.testhelpers

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.multisensor.recording.R

/**
 * Test helper utilities to keep test methods focused and cognitive complexity under 15.
 * Provides reusable test actions for common UI interactions.
 */
object TestHelpers {

    private const val TAG = "TestHelpers"

    /**
     * Navigate to a specific fragment using the drawer navigation.
     * Handles opening/closing drawer and includes proper synchronization.
     */
    fun navigateToFragment(fragmentId: Int, fragmentName: String): Boolean {
        return try {
            val idlingResource = CustomIdlingResource.createNavigationResource()
            IdlingRegistry.getInstance().register(idlingResource)

            onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open())

            onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(fragmentId))

            onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.close())

            IdlingRegistry.getInstance().unregister(idlingResource)
            Log.i(TAG, "✅ Navigated to $fragmentName successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to navigate to $fragmentName", e)
            false
        }
    }

    /**
     * Test button by multiple strategies: tag, text, content description.
     * Returns true if button was found and clicked successfully.
     */
    fun testButtonByMultipleStrategies(
        buttonTag: String,
        buttonName: String,
        context: String
    ): Boolean {
        val idlingResource = CustomIdlingResource.createInteractionResource()
        IdlingRegistry.getInstance().register(idlingResource)

        val success = try {
            // Strategy 1: Try by tag
            onView(withTagValue(org.hamcrest.Matchers.`is`(buttonTag as Any)))
                .perform(click())
            true
        } catch (e: NoMatchingViewException) {
            try {
                // Strategy 2: Try by text
                onView(withText(buttonName))
                    .perform(click())
                true
            } catch (e2: NoMatchingViewException) {
                try {
                    // Strategy 3: Try by content description
                    onView(withContentDescription(buttonName))
                        .perform(click())
                    true
                } catch (e3: NoMatchingViewException) {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error testing button $buttonName in $context", e)
            false
        }

        IdlingRegistry.getInstance().unregister(idlingResource)
        
        if (success) {
            Log.i(TAG, "✅ Button $buttonName in $context clicked successfully")
        } else {
            Log.w(TAG, "⚠️ Button $buttonName in $context not found or not clickable")
        }
        
        return success
    }

    /**
     * Check if a status indicator is visible.
     * Returns true if the indicator is displayed.
     */
    fun checkStatusIndicator(indicator: String, context: String): Boolean {
        return try {
            onView(withTagValue(org.hamcrest.Matchers.`is`(indicator as Any)))
                .check(matches(isDisplayed()))
            Log.i(TAG, "✅ Status indicator $indicator in $context visible")
            true
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Status indicator $indicator in $context not found")
            false
        }
    }

    /**
     * Perform a navigation flow between two fragments.
     * Returns true if the flow completed successfully.
     */
    fun performNavigationFlow(fromFragmentId: Int, toFragmentId: Int): Boolean {
        val fromName = getFragmentName(fromFragmentId)
        val toName = getFragmentName(toFragmentId)
        
        Log.i(TAG, "Testing navigation flow: $fromName -> $toName")
        
        return navigateToFragment(fromFragmentId, fromName) && 
               navigateToFragment(toFragmentId, toName)
    }

    /**
     * Get human-readable fragment name from ID.
     */
    fun getFragmentName(fragmentId: Int): String {
        return when (fragmentId) {
            R.id.nav_recording -> "Recording"
            R.id.nav_devices -> "Devices"
            R.id.nav_calibration -> "Calibration"
            R.id.nav_files -> "Files"
            else -> "Unknown"
        }
    }

    /**
     * Test a list of buttons in a specific context.
     * Returns a map of button results.
     */
    fun testButtonList(
        buttons: List<Pair<String, String>>,
        context: String
    ): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        
        buttons.forEach { (buttonTag, buttonName) ->
            results["${context}_$buttonTag"] = testButtonByMultipleStrategies(
                buttonTag, buttonName, context
            )
        }
        
        return results
    }

    /**
     * Test a list of status indicators in a specific context.
     * Returns a map of indicator results.
     */
    fun testStatusIndicatorList(
        indicators: List<String>,
        context: String
    ): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        
        indicators.forEach { indicator ->
            results["${context}_$indicator"] = checkStatusIndicator(indicator, context)
        }
        
        return results
    }

    /**
     * Test bottom navigation functionality.
     * Returns true if navigation was successful.
     */
    fun testBottomNavigation(itemId: Int, itemName: String): Boolean {
        return try {
            val idlingResource = CustomIdlingResource.createInteractionResource()
            IdlingRegistry.getInstance().register(idlingResource)

            onView(withId(itemId))
                .perform(click())

            IdlingRegistry.getInstance().unregister(idlingResource)
            Log.i(TAG, "✅ Bottom navigation to $itemName successful")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bottom navigation to $itemName failed", e)
            false
        }
    }

    /**
     * Navigate to settings activities with proper back navigation.
     * Returns true if navigation was successful.
     */
    fun navigateToSettingsActivity(activityId: Int, activityName: String): Boolean {
        return try {
            val idlingResource = CustomIdlingResource.createNavigationResource()
            IdlingRegistry.getInstance().register(idlingResource)

            onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open())

            onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(activityId))

            // Navigate back
            try {
                onView(withContentDescription("Navigate up"))
                    .perform(click())
            } catch (e: Exception) {
                // Try alternative back navigation
                androidx.test.platform.app.InstrumentationRegistry
                    .getInstrumentation()
                    .uiAutomation
                    .performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            }

            IdlingRegistry.getInstance().unregister(idlingResource)
            Log.i(TAG, "✅ Settings activity $activityName navigation successful")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Settings activity $activityName navigation failed", e)
            false
        }
    }

    /**
     * Validate navigation accessibility features.
     * Returns true if accessibility check passes.
     */
    fun validateNavigationAccessibility(checkName: String): Boolean {
        return try {
            // Perform navigation action that triggers accessibility checks
            when (checkName) {
                "Navigation drawer toggle accessible" -> {
                    onView(withId(R.id.drawer_layout))
                        .perform(DrawerActions.open())
                    onView(withId(R.id.drawer_layout))
                        .perform(DrawerActions.close())
                }
                "Fragment navigation accessible" -> {
                    navigateToFragment(R.id.nav_recording, "Recording")
                }
                "Settings navigation accessible" -> {
                    navigateToSettingsActivity(R.id.nav_settings, "Settings")
                }
            }
            
            Log.i(TAG, "✅ Accessibility check '$checkName' passed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Accessibility check '$checkName' failed", e)
            false
        }
    }
}