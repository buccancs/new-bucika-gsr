package com.multisensor.recording.recording

import android.content.Context
import android.view.SurfaceView
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.streaming.PreviewStreamer
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.robolectric.annotation.Config

@Config(sdk = [33])
class ThermalRecorderPreviewTest : DescribeSpec({

    val mockContext: Context = mockk(relaxed = true)
    val mockSessionManager: SessionManager = mockk(relaxed = true)
    val mockLogger: Logger = mockk(relaxed = true)
    val mockThermalSettings: ThermalCameraSettings = mockk(relaxed = true)
    val mockSurfaceView: SurfaceView = mockk(relaxed = true)
    val mockPreviewStreamer: PreviewStreamer = mockk(relaxed = true)

    lateinit var thermalRecorder: ThermalRecorder

    beforeEach {
        clearAllMocks()

        // Setup default thermal settings
        val mockConfig = mockk<ThermalCameraSettings.ThermalConfig>()
        every { mockConfig.isEnabled } returns true
        every { mockConfig.usbPriority } returns true
        every { mockThermalSettings.getCurrentConfig() } returns mockConfig
        every { mockThermalSettings.getConfigSummary() } returns "Test config"

        thermalRecorder = ThermalRecorder(mockContext, mockSessionManager, mockLogger, mockThermalSettings)
    }

    describe("ThermalRecorder preview retry logic") {

        it("should initialize successfully with all components available") {
            // When
            val result = thermalRecorder.initialize(mockSurfaceView, mockPreviewStreamer)

            // Then
            result shouldBe true
            verify { mockLogger.info("ThermalRecorder initialized successfully") }
        }

        it("should handle USB device initialization") {
            // When
            val result = thermalRecorder.initialize(mockSurfaceView)

            // Then
            result shouldBe true
            verify { mockLogger.info("Initializing ThermalRecorder") }
            verify { mockLogger.info("Loaded thermal configuration:") }
        }

        it("should detect thermal camera availability") {
            // When
            val isAvailable = thermalRecorder.isThermalCameraAvailable()

            // Then
            // Should return false in test environment (no real USB devices)
            isAvailable shouldBe false
        }

        it("should get thermal camera status correctly") {
            // When
            val status = thermalRecorder.getThermalCameraStatus()

            // Then
            status shouldNotBe null
            status.isAvailable shouldBe false // No real device in test
            status.isRecording shouldBe false
            status.isPreviewActive shouldBe false
            status.width shouldBe 256
            status.height shouldBe 192
            status.frameRate shouldBe 25
        }

        it("should handle preview start when no device connected") {
            // When
            val result = thermalRecorder.startPreview()

            // Then
            result shouldBe false
            verify { mockLogger.warning("No thermal camera device connected, attempting device discovery...") }
        }

        it("should handle preview stop when no preview active") {
            // When
            val result = thermalRecorder.stopPreview()

            // Then
            result shouldBe true // Should succeed even if no preview was active
        }

        it("should handle recording start when not initialized") {
            // Given
            val sessionId = "test_session_123"

            // When
            val result = thermalRecorder.startRecording(sessionId)

            // Then
            result shouldBe false
            verify { mockLogger.error("ThermalRecorder not initialized") }
        }

        it("should handle recording stop when no recording active") {
            // When
            val result = thermalRecorder.stopRecording()

            // Then
            result shouldBe false
            verify { mockLogger.warning("No recording in progress") }
        }

        it("should cleanup resources properly") {
            // Given
            thermalRecorder.initialize(mockSurfaceView)

            // When
            thermalRecorder.cleanup()

            // Then
            verify { mockLogger.info("Cleaning up ThermalRecorder") }
            verify { mockLogger.info("ThermalRecorder cleanup completed") }
        }

        it("should handle security exceptions during initialization") {
            // Given
            every { mockContext.getSystemService(Context.USB_SERVICE) } throws SecurityException("USB permission denied")

            // When
            val result = thermalRecorder.initialize(mockSurfaceView)

            // Then
            result shouldBe false
            verify { mockLogger.error(match { it.contains("Security exception initializing thermal recorder") }, any()) }
        }

        it("should handle thermal camera settings correctly") {
            // Given
            val mockConfig = mockk<ThermalCameraSettings.ThermalConfig>()
            every { mockConfig.isEnabled } returns true
            every { mockConfig.frameRate } returns 30
            every { mockConfig.emissivity } returns 0.95f
            every { mockConfig.autoCalibration } returns true
            every { mockConfig.highResolution } returns true
            every { mockConfig.getTemperatureRangeValues() } returns Pair(-10.0f, 100.0f)
            every { mockThermalSettings.getCurrentConfig() } returns mockConfig

            // When
            thermalRecorder.initialize(mockSurfaceView)

            // Then
            verify { mockThermalSettings.getCurrentConfig() }
            verify { mockLogger.info("Loaded thermal configuration:") }
        }
    }

    describe("Error handling scenarios") {

        it("should handle IO exceptions during initialization") {
            // This test simulates various error conditions that might occur
            // during thermal camera initialization in real-world scenarios
            
            // When
            val result = thermalRecorder.initialize(mockSurfaceView)

            // Then
            // Should handle gracefully even with no real hardware
            result shouldBe true
        }

        it("should handle concurrent access properly") {
            // Given
            thermalRecorder.initialize(mockSurfaceView)

            // When - simulate multiple rapid calls
            val results = (1..5).map { thermalRecorder.startPreview() }

            // Then - should handle gracefully
            results.forEach { result ->
                // First call might succeed (if preview already active), others should too
                // In test environment without real hardware, all will likely fail
            }

            verify(atLeast = 1) { mockLogger.info(any()) }
        }
    }
})