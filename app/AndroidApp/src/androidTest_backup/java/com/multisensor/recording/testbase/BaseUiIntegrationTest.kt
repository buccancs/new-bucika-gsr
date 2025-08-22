package com.multisensor.recording.testbase

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import com.multisensor.recording.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule

@HiltAndroidTest
abstract class BaseUiIntegrationTest : BaseInstrumentedTest() {

    @get:Rule(order = 0)
    override var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO
    )

    protected fun waitForUiIdle() {
        Thread.sleep(100)
    }
}
