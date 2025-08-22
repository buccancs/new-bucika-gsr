package com.multisensor.recording.recording

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ThermalRecorderHardwareTest {
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

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext

        thermalRecorder = ThermalRecorder(context, sessionManager, logger, thermalSettings)

        println("[DEBUG_LOG] ThermalRecorder hardware test setup complete")
    }

    @Test
    fun testThermalCameraDetectionAndInitialization() =
        runBlocking {
            println("[DEBUG_LOG] Starting thermal camera detection test...")

            val initResult = thermalRecorder.initialize()
            println("[DEBUG_LOG] ThermalRecorder initialization result: $initResult")

            println("[DEBUG_LOG] Waiting for USB device detection (10 seconds)...")
            delay(10000)

            val status = thermalRecorder.getThermalCameraStatus()
            println("[DEBUG_LOG] Thermal camera status:")
            println("[DEBUG_LOG] - Available: ${status.isAvailable}")
            println("[DEBUG_LOG] - Device name: ${status.deviceName}")
            println("[DEBUG_LOG] - Width: ${status.width}")
            println("[DEBUG_LOG] - Height: ${status.height}")
            println("[DEBUG_LOG] - Frame rate: ${status.frameRate}")

            if (status.isAvailable) {
                println("[DEBUG_LOG] Camera detected! Testing preview...")

                val previewResult = thermalRecorder.startPreview()
                println("[DEBUG_LOG] Preview start result: $previewResult")

                if (previewResult) {
                    delay(5000)

                    val updatedStatus = thermalRecorder.getThermalCameraStatus()
                    println("[DEBUG_LOG] Preview status:")
                    println("[DEBUG_LOG] - Preview active: ${updatedStatus.isPreviewActive}")
                    println("[DEBUG_LOG] - Frame count: ${updatedStatus.frameCount}")

                    thermalRecorder.stopPreview()
                    println("[DEBUG_LOG] Preview stopped")
                }
            } else {
                println("[DEBUG_LOG] No thermal camera detected. Please check:")
                println("[DEBUG_LOG] 1. Camera is connected via USB-C OTG")
                println("[DEBUG_LOG] 2. USB permissions were granted")
                println("[DEBUG_LOG] 3. Camera is a supported Topdon model (TC001/Plus)")
            }

            thermalRecorder.cleanup()
            println("[DEBUG_LOG] Test completed")
        }

    @Test
    fun testThermalRecordingBasicFunctionality() =
        runBlocking {
            println("[DEBUG_LOG] Starting thermal recording functionality test...")

            val initResult = thermalRecorder.initialize()
            println("[DEBUG_LOG] ThermalRecorder initialization result: $initResult")

            delay(10000)

            val status = thermalRecorder.getThermalCameraStatus()
            if (!status.isAvailable) {
                println("[DEBUG_LOG] No thermal camera available - skipping recording test")
                return@runBlocking
            }

            println("[DEBUG_LOG] Testing thermal recording...")

            val sessionId = "test_session_${System.currentTimeMillis()}"
            val recordingResult = thermalRecorder.startRecording(sessionId)
            println("[DEBUG_LOG] Recording start result: $recordingResult")

            if (recordingResult) {
                println("[DEBUG_LOG] Recording for 10 seconds...")
                delay(10000)

                val recordingStatus = thermalRecorder.getThermalCameraStatus()
                println("[DEBUG_LOG] Recording status:")
                println("[DEBUG_LOG] - Recording: ${recordingStatus.isRecording}")
                println("[DEBUG_LOG] - Frame count: ${recordingStatus.frameCount}")

                val stopResult = thermalRecorder.stopRecording()
                println("[DEBUG_LOG] Recording stop result: $stopResult")

                val finalStatus = thermalRecorder.getThermalCameraStatus()
                println("[DEBUG_LOG] Final frame count: ${finalStatus.frameCount}")

                val expectedFrames = 250
                val actualFrames = finalStatus.frameCount
                val frameCountOk = actualFrames > (expectedFrames * 0.8)

                println("[DEBUG_LOG] Frame count validation:")
                println("[DEBUG_LOG] - Expected: ~$expectedFrames frames")
                println("[DEBUG_LOG] - Actual: $actualFrames frames")
                println("[DEBUG_LOG] - Validation: ${if (frameCountOk) "PASS" else "FAIL"}")
            }

            thermalRecorder.cleanup()
            println("[DEBUG_LOG] Recording test completed")
        }
}
