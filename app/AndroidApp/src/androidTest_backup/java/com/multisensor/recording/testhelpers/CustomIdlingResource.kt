package com.multisensor.recording.testhelpers

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Custom IdlingResource for managing background operations in Android UI tests.
 * Replaces Thread.sleep() with proper synchronization for test stability.
 */
class CustomIdlingResource(
    private val resourceName: String,
    private val waitTime: Long = 2000L
) : IdlingResource {

    private val isIdleNow = AtomicBoolean(true)
    private var callback: IdlingResource.ResourceCallback? = null
    private var startTime = 0L

    fun setIdle(idle: Boolean) {
        if (idle) {
            isIdleNow.set(true)
            callback?.onTransitionToIdle()
        } else {
            isIdleNow.set(false)
            startTime = System.currentTimeMillis()
        }
    }

    override fun getName(): String = resourceName

    override fun isIdleNow(): Boolean {
        val idle = isIdleNow.get()
        if (!idle) {
            // Check if enough time has passed
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime >= waitTime) {
                isIdleNow.set(true)
                callback?.onTransitionToIdle()
                return true
            }
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }

    companion object {
        /**
         * Creates a simple timing-based IdlingResource that becomes idle after specified time
         */
        fun createTimingResource(name: String, waitTimeMs: Long): CustomIdlingResource {
            return CustomIdlingResource(name, waitTimeMs).apply {
                setIdle(false) // Start as busy
            }
        }

        /**
         * Creates an IdlingResource for navigation transitions
         */
        fun createNavigationResource(): CustomIdlingResource {
            return createTimingResource("NavigationTransition", 1500L)
        }

        /**
         * Creates an IdlingResource for button clicks and UI interactions
         */
        fun createInteractionResource(): CustomIdlingResource {
            return createTimingResource("UIInteraction", 500L)
        }
    }
}