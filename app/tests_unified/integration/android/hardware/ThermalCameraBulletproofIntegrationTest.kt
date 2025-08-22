package com.multisensor.recording.recording

import android.content.Context
import android.hardware.usb.UsbDevice
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.multisensor.recording.managers.UsbDeviceManager
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ThermalCameraBulletproofIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var thermalSettings: ThermalCameraSettings

    private lateinit var context: Context
    private lateinit var thermalRecorder: ThermalRecorder
    private lateinit var usbDeviceManager: UsbDeviceManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext

        thermalRecorder = ThermalRecorder(context, sessionManager, logger, thermalSettings)
        usbDeviceManager = UsbDeviceManager()

        println("[BULLETPROOF_TEST] Test setup complete")
    }

    @Test
    fun testRapidInitializationCleanupCycles() = runBlocking {
        println("[BULLETPROOF_TEST] Testing rapid initialization/cleanup cycles...")

        repeat(10) { cycle ->
            println("[BULLETPROOF_TEST] Cycle $cycle: Initialize")

            val initResult = thermalRecorder.initialize()

            delay(100)

            println("[BULLETPROOF_TEST] Cycle $cycle: Cleanup")
            thermalRecorder.cleanup()

            delay(50)
        }

        println("[BULLETPROOF_TEST] Rapid cycles test completed - no crashes")
    }

    @Test
    fun testConcurrentOperationAttempts() = runBlocking {
        println("[BULLETPROOF_TEST] Testing concurrent operation attempts...")

        thermalRecorder.initialize()
        delay(500)

        val sessionId = "concurrent_test_${System.currentTimeMillis()}"

        val results = (1..5).map {
            thermalRecorder.startRecording("${sessionId}_$it")
        }

        val successCount = results.count { it }
        assertTrue("At most one recording should succeed", successCount <= 1)

        repeat(3) {
            thermalRecorder.stopRecording()
        }

        thermalRecorder.cleanup()
        println("[BULLETPROOF_TEST] Concurrent operations test completed")
    }

    @Test
    fun testStateTransitionEdgeCases() = runBlocking {
        println("[BULLETPROOF_TEST] Testing state transition edge cases...")

        assertFalse(
            "Recording should fail before init",
            thermalRecorder.startRecording("test")
        )
        assertFalse(
            "Stop should fail before init",
            thermalRecorder.stopRecording()
        )
        assertFalse(
            "Preview should fail before init",
            thermalRecorder.startPreview()
        )
        assertTrue(
            "Stop preview should succeed",
            thermalRecorder.stopPreview()
        )

        thermalRecorder.initialize()
        delay(500)

        val status1 = thermalRecorder.getThermalCameraStatus()
        val status2 = thermalRecorder.getThermalCameraStatus()

        assertEquals("Status should be consistent", status1.isRecording, status2.isRecording)
        assertEquals("Preview state should be consistent", status1.isPreviewActive, status2.isPreviewActive)

        thermalRecorder.cleanup()

        assertFalse(
            "Recording should fail after cleanup",
            thermalRecorder.startRecording("test")
        )
        assertFalse(
            "Preview should fail after cleanup",
            thermalRecorder.startPreview()
        )

        println("[BULLETPROOF_TEST] State transition test completed")
    }

    @Test
    fun testResourceManagementUnderStress() = runBlocking {
        println("[BULLETPROOF_TEST] Testing resource management under stress...")

        thermalRecorder.initialize()
        delay(500)

        repeat(100) {
            thermalRecorder.getThermalCameraStatus()
        }

        repeat(20) {
            thermalRecorder.startPreview()
            delay(10)
            thermalRecorder.stopPreview()
            delay(10)
        }

        repeat(10) { i ->
            val sessionId = "stress_test_$i"
            thermalRecorder.startRecording(sessionId)
            delay(50)
            thermalRecorder.stopRecording()
            delay(25)
        }

        thermalRecorder.cleanup()
        println("[BULLETPROOF_TEST] Resource stress test completed")
    }

    @Test
    fun testErrorRecoveryMechanisms() = runBlocking {
        println("[BULLETPROOF_TEST] Testing error recovery mechanisms...")

        thermalRecorder.initialize()
        delay(500)

        assertFalse(
            "Empty session ID should fail",
            thermalRecorder.startRecording("")
        )
        assertFalse(
            "Null-like session ID should fail",
            thermalRecorder.startRecording("null")
        )
        assertFalse(
            "Very long session ID should fail",
            thermalRecorder.startRecording("a".repeat(1000))
        )

        thermalRecorder.cleanup()
        thermalRecorder.cleanup()
        thermalRecorder.cleanup()

        assertFalse(
            "Should fail gracefully after multiple cleanups",
            thermalRecorder.startRecording("test")
        )

        println("[BULLETPROOF_TEST] Error recovery test completed")
    }

    @Test
    fun testMemoryManagementValidation() = runBlocking {
        println("[BULLETPROOF_TEST] Testing memory management...")

        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        println("[BULLETPROOF_TEST] Initial memory usage: ${initialMemory / 1024 / 1024}MB")

        repeat(5) { iteration ->
            val recorder = ThermalRecorder(context, sessionManager, logger, thermalSettings)
            recorder.initialize()
            delay(200)

            recorder.getThermalCameraStatus()
            recorder.startPreview()
            delay(100)
            recorder.stopPreview()

            recorder.cleanup()

            System.gc()
            delay(100)

            val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            println("[BULLETPROOF_TEST] Memory after iteration $iteration: ${currentMemory / 1024 / 1024}MB")
        }

        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        println("[BULLETPROOF_TEST] Final memory usage: ${finalMemory / 1024 / 1024}MB")
        println("[BULLETPROOF_TEST] Memory increase: ${memoryIncrease / 1024 / 1024}MB")

        assertTrue(
            "Memory increase should be reasonable (< 50MB)",
            memoryIncrease < 50 * 1024 * 1024
        )

        println("[BULLETPROOF_TEST] Memory management test completed")
    }

    @Test
    fun testDeviceFilterValidation() {
        println("[BULLETPROOF_TEST] Testing device filter validation...")

        val supportedDeviceIds = listOf(
            Pair(0x0BDA, 0x3901),
            Pair(0x0BDA, 0x5840),
            Pair(0x0BDA, 0x5830),
            Pair(0x0BDA, 0x5838)
        )

        supportedDeviceIds.forEach { (vendorId, productId) ->
            val mockDevice = createMockUsbDevice(vendorId, productId)
            assertTrue(
                "Device VID:0x${vendorId.toString(16)}, PID:0x${productId.toString(16)} should be supported",
                usbDeviceManager.isSupportedTopdonDevice(mockDevice)
            )
        }

        val edgeCaseIds = listOf(
            Pair(0x0BDA, 0x3900),
            Pair(0x0BDA, 0x3902),
            Pair(0x0BDB, 0x3901),
            Pair(0x0BD9, 0x3901),
            Pair(0x0000, 0x0000),
            Pair(0xFFFF, 0xFFFF)
        )

        edgeCaseIds.forEach { (vendorId, productId) ->
            val mockDevice = createMockUsbDevice(vendorId, productId)
            assertFalse(
                "Edge case device VID:0x${vendorId.toString(16)}, PID:0x${productId.toString(16)} should NOT be supported",
                usbDeviceManager.isSupportedTopdonDevice(mockDevice)
            )
        }

        println("[BULLETPROOF_TEST] Device filter validation completed")
    }

    @Test
    fun testThreadSafetyValidation() = runBlocking {
        println("[BULLETPROOF_TEST] Testing thread safety...")

        thermalRecorder.initialize()
        delay(500)

        val operations = listOf(
            suspend { repeat(20) { thermalRecorder.getThermalCameraStatus(); delay(5) } },
            suspend { repeat(10) { thermalRecorder.startPreview(); delay(10); thermalRecorder.stopPreview(); delay(10) } },
            suspend {
                repeat(5) {
                    thermalRecorder.startRecording("thread_test_$it"); delay(20); thermalRecorder.stopRecording(); delay(
                    20
                )
                }
            }
        )

        operations.forEach { operation ->
            launch {
                try {
                    operation()
                } catch (e: Exception) {
                    println("[BULLETPROOF_TEST] Thread safety test caught exception: ${e.message}")
                }
            }
        }

        delay(2000)

        thermalRecorder.cleanup()
        println("[BULLETPROOF_TEST] Thread safety test completed")
    }

    @Test
    fun testEdgeCaseRecovery() = runBlocking {
        println("[BULLETPROOF_TEST] Testing edge case recovery...")

        thermalRecorder.cleanup()

        thermalRecorder.initialize()
        val secondInit = thermalRecorder.initialize()

        delay(500)

        val weirdSessionIds = listOf(
            "session/with/slashes",
            "session with spaces",
            "session\nwith\nnewlines",
            "session\twith\ttabs",
            "session-with-unicode-😀",
            "../../../etc/passwd",
            "CON", "PRN", "AUX"
        )

        weirdSessionIds.forEach { sessionId ->
            try {
                thermalRecorder.startRecording(sessionId)
                delay(50)
                thermalRecorder.stopRecording()
                delay(25)
                println("[BULLETPROOF_TEST] Handled weird session ID: '$sessionId'")
            } catch (e: Exception) {
                println("[BULLETPROOF_TEST] Exception with session ID '$sessionId': ${e.message}")
            }
        }

        thermalRecorder.cleanup()
        println("[BULLETPROOF_TEST] Edge case recovery test completed")
    }

    private fun createMockUsbDevice(vendorId: Int, productId: Int): UsbDevice {
        return mockk<UsbDevice>(relaxed = true) {
            every { getVendorId() } returns vendorId
            every { getProductId() } returns productId
            every { deviceName } returns "/dev/bus/usb/001/002"
            every { deviceClass } returns 14
            every { deviceSubclass } returns 1
            every { deviceProtocol } returns 0
            every { manufacturerName } returns "Test Manufacturer"
            every { productName } returns "Test Product"
            every { version } returns "1.0"
            every { serialNumber } returns "123456"
            every { configurationCount } returns 1
            every { interfaceCount } returns 1
        }
    }
}
