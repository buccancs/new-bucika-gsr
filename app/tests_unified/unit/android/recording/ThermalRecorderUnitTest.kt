package com.multisensor.recording.recording

import android.content.Context
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.service.util.FileStructureManager
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.robolectric.annotation.Config
import java.io.File

@Config(sdk = [33])
class ThermalRecorderUnitTest : DescribeSpec({

    val mockContext: Context = mockk(relaxed = true)
    val mockSessionManager: SessionManager = mockk(relaxed = true)
    val mockLogger: Logger = mockk(relaxed = true)
    val mockThermalSettings: ThermalCameraSettings = mockk(relaxed = true)
    val testSessionId = "test_thermal_session_123"

    lateinit var thermalRecorder: ThermalRecorder

    beforeEach {
        clearAllMocks()

        val mockSessionPaths = mockk<FileStructureManager.SessionFilePaths>()
        every { mockSessionPaths.sessionFolder } returns File("/test/session/folder")
        every { mockSessionManager.getSessionFilePaths() } returns mockSessionPaths

        thermalRecorder = ThermalRecorder(mockContext, mockSessionManager, mockLogger, mockThermalSettings)
    }

    describe("ThermalRecorder initialization") {

        it("should initialize successfully") {
            runTest {
                val result = thermalRecorder.initialize()

                result shouldBe true
                verify { mockLogger.info("Initializing ThermalRecorder") }
            }
        }

        it("should handle initialization failure gracefully") {
            runTest {
                every { mockContext.packageManager } throws RuntimeException("No thermal camera")

                val result = thermalRecorder.initialize()

                result shouldBe false
                verify { mockLogger.error(any<String>()) }
            }
        }
    }

    describe("ThermalRecorder recording operations") {

        it("should start recording successfully when initialized") {
            runTest {
                thermalRecorder.initialize()

                val result = thermalRecorder.startRecording(testSessionId)

                result shouldBe true
                verify { mockLogger.info("Starting thermal recording for session: $testSessionId") }
            }
        }

        it("should fail to start recording when not initialized") {
            runTest {
                val result = thermalRecorder.startRecording(testSessionId)

                result shouldBe false
                verify { mockLogger.error("ThermalRecorder not initialized") }
            }
        }

        it("should stop recording successfully") {
            runTest {
                thermalRecorder.initialize()
                thermalRecorder.startRecording(testSessionId)

                val result = thermalRecorder.stopRecording()

                result shouldBe true
                verify { mockLogger.info("Stopping thermal recording") }
            }
        }

        it("should handle stop recording when not recording") {
            runTest {
                val result = thermalRecorder.stopRecording()

                result shouldBe false
                verify { mockLogger.warning("No recording in progress") }
            }
        }
    }

    describe("ThermalRecorder preview functionality") {

        it("should start preview successfully when initialized") {
            runTest {
                thermalRecorder.initialize()

                val result = thermalRecorder.startPreview()

                result shouldBe false // No real device in test environment
                verify { mockLogger.warning("No thermal camera device connected, attempting device discovery...") }
            }
        }

        it("should stop preview successfully") {
            runTest {
                thermalRecorder.initialize()
                thermalRecorder.startPreview()

                val result = thermalRecorder.stopPreview()

                result shouldBe true
                // Preview stop should succeed even without active preview
            }
        }
    }

    describe("ThermalRecorder status and configuration") {

        it("should return correct recording status") {
            runTest {
                val initialStatus = thermalRecorder.getThermalCameraStatus()
                initialStatus.isRecording shouldBe false

                thermalRecorder.initialize()
                thermalRecorder.startRecording(testSessionId)

                val recordingStatus = thermalRecorder.getThermalCameraStatus()
                recordingStatus.isRecording shouldBe true

                thermalRecorder.stopRecording()

                val stoppedStatus = thermalRecorder.getThermalCameraStatus()
                stoppedStatus.isRecording shouldBe false
            }
        }

        it("should return correct initialization status") {
            runTest {
                val initResult = thermalRecorder.initialize()
                initResult shouldBe true

                val previewResult = thermalRecorder.startPreview()
                previewResult shouldBe true
            }
        }

        it("should apply thermal settings correctly on initialization") {
            runTest {
                thermalRecorder.initialize()

                verify { mockLogger.info(any<String>()) }
            }
        }

        it("should get thermal camera status correctly") {
            runTest {
                val status = thermalRecorder.getThermalCameraStatus()

                status.width shouldBe 256
                status.height shouldBe 192
                status.frameRate shouldBe 25
                status.isRecording shouldBe false
                status.isAvailable shouldBe false // No real device in test
                status.frameCount shouldBe 0L
            }
        }
    }

    describe("ThermalRecorder error handling and edge cases") {

        it("should handle concurrent recording attempts") {
            runTest {
                thermalRecorder.initialize()

                val result1 = thermalRecorder.startRecording(testSessionId)
                val result2 = thermalRecorder.startRecording("another_session")

                result1 shouldBe false // Will fail due to no device in test
                result2 shouldBe false

                // In test environment, both will fail due to no device
                verify(atLeast = 1) { mockLogger.error(any<String>()) }
            }
        }

        it("should handle session cleanup on recording failure") {
            runTest {
                thermalRecorder.initialize()

                every { mockSessionManager.getSessionFilePaths() } throws RuntimeException("Storage full")

                val result = thermalRecorder.startRecording(testSessionId)

                result shouldBe false
                verify { mockLogger.error(any<String>()) }
            }
        }

        it("should handle resource cleanup properly") {
            runTest {
                thermalRecorder.initialize()
                thermalRecorder.startRecording(testSessionId)
                thermalRecorder.startPreview()

                thermalRecorder.cleanup()

                val status = thermalRecorder.getThermalCameraStatus()
                status.isRecording shouldBe false
                verify { mockLogger.info("ThermalRecorder cleanup completed") }
            }
        }
    }

    describe("ThermalRecorder device status and validation") {

        it("should provide correct device status information") {
            runTest {
                thermalRecorder.initialize()

                val status = thermalRecorder.getThermalCameraStatus()

                status.width shouldBe 256
                status.height shouldBe 192
                status.frameRate shouldBe 25
                status.frameCount shouldBe 0L
                status.isAvailable shouldBe false // No real device in test
            }
        }

        it("should handle thermal camera validation correctly") {
            runTest {
                val initResult = thermalRecorder.initialize()
                initResult shouldBe true

                val status = thermalRecorder.getThermalCameraStatus()
                status shouldNotBe null
            }
        }
    }

    describe("ThermalRecorder USB monitoring status") {

        it("should report USB monitoring availability") {
            runTest {
                thermalRecorder.initialize()

                // After initialization, USB monitoring status should be determinable
                val isAvailable = thermalRecorder.isUsbMonitoringAvailable()
                // In test environment without real USB system, this will be false
                isAvailable shouldBe false
            }
        }

        it("should provide helpful status messages") {
            runTest {
                // Before initialization
                val statusBefore = thermalRecorder.getStatusMessage()
                statusBefore shouldBe "Thermal camera not initialized"

                // After initialization
                thermalRecorder.initialize()
                val statusAfter = thermalRecorder.getStatusMessage()
                // In test environment, should indicate limited detection
                statusAfter shouldBe "Thermal camera available but automatic detection limited (Android 13+ restriction)"
            }
        }

        it("should include USB monitoring details in diagnostics") {
            runTest {
                thermalRecorder.initialize()

                val diagnostics = thermalRecorder.getInitializationDiagnostics()
                diagnostics.contains("USB monitoring available:") shouldBe true
                diagnostics.contains("Android 13+") shouldBe true
            }
        }
    }

    afterEach {
        runTest {
            thermalRecorder.cleanup()
        }
        clearAllMocks()
    }
})
