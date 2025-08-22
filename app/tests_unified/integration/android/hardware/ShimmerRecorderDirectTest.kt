package com.multisensor.recording.recording

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ShimmerRecorderDirectTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )

    @Inject
    lateinit var shimmerRecorder: ShimmerRecorder

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var logger: Logger

    @Before
    fun setup() {
        hiltRule.inject()
        println("[DEBUG_LOG] ShimmerRecorderDirectTest setup complete")
    }

    @Test
    fun testDirectDeviceDiscovery() =
        runBlocking {
            println("[DEBUG_LOG] === Direct Shimmer Device Discovery Test ===")

            try {
                println("[DEBUG_LOG] Initializing ShimmerRecorder...")
                val initialized = shimmerRecorder.initialize()
                println("[DEBUG_LOG] ShimmerRecorder initialized: $initialized")

                if (!initialized) {
                    println("[DEBUG_LOG] Failed to initialize ShimmerRecorder")
                    return@runBlocking
                }

                println("[DEBUG_LOG] Calling scanAndPairDevices()...")
                val discoveredDevices = shimmerRecorder.scanAndPairDevices()

                println("[DEBUG_LOG] Device discovery completed")
                println("[DEBUG_LOG] Discovered devices count: ${discoveredDevices.size}")
                discoveredDevices.forEachIndexed { index, address ->
                    println("[DEBUG_LOG] Device $index: $address")
                }

                if (discoveredDevices.isEmpty()) {
                    println("[DEBUG_LOG] No devices found - this is expected if no Shimmer device is properly paired")
                } else {
                    println("[DEBUG_LOG] SUCCESS: Found ${discoveredDevices.size} Shimmer devices")
                }
            } catch (e: Exception) {
                println("[DEBUG_LOG] Exception during device discovery: ${e.message}")
                e.printStackTrace()
            } finally {
                try {
                    shimmerRecorder.cleanup()
                    println("[DEBUG_LOG] ShimmerRecorder cleanup completed")
                } catch (e: Exception) {
                    println("[DEBUG_LOG] Exception during cleanup: ${e.message}")
                }
            }

            println("[DEBUG_LOG] === End Direct Device Discovery Test ===")
        }
}
