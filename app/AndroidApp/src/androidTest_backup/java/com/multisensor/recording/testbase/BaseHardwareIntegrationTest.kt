package com.multisensor.recording.testbase

import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule

@HiltAndroidTest
abstract class BaseHardwareIntegrationTest : BaseInstrumentedTest() {

    @get:Rule(order = 0)
    override var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE
    )

    companion object {
        const val HARDWARE_TIMEOUT_MS = 10000L
        const val CONNECTION_TIMEOUT_MS = 5000L
        const val DATA_PROCESSING_TIMEOUT_MS = 3000L
    }

    protected fun waitForHardwareOperation(timeoutMs: Long = HARDWARE_TIMEOUT_MS) {
        Thread.sleep(timeoutMs.coerceAtMost(1000))
    }

    protected fun isHardwareAvailable(): Boolean {
        return true
    }
}
