package com.multisensor.recording.managers

import android.content.Context
import android.view.SurfaceView
import android.view.TextureView
import com.multisensor.recording.network.JsonSocketClient
import com.multisensor.recording.network.NetworkConfiguration
import com.multisensor.recording.recording.CameraRecorder
import com.multisensor.recording.recording.ShimmerRecorder
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.util.Logger
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.robolectric.annotation.Config

@Config(sdk = [33])
class DeviceConnectionManagerPreviewTest : DescribeSpec({

    val mockContext: Context = mockk()
    val mockCameraRecorder: CameraRecorder = mockk()
    val mockThermalRecorder: ThermalRecorder = mockk()
    val mockShimmerRecorder: ShimmerRecorder = mockk()
    val mockJsonSocketClient: JsonSocketClient = mockk()
    val mockNetworkConfiguration: NetworkConfiguration = mockk()
    val mockLogger: Logger = mockk(relaxed = true)
    val mockTextureView: TextureView = mockk()
    val mockSurfaceView: SurfaceView = mockk()

    lateinit var deviceConnectionManager: DeviceConnectionManager

    beforeEach {
        clearAllMocks()

        // Setup default mock behaviors
        every { mockTextureView.isAvailable } returns true
        every { mockCameraRecorder.initialize(any()) } returns true
        every { mockThermalRecorder.initialize(any()) } returns true
        every { mockThermalRecorder.startPreview() } returns true
        every { mockShimmerRecorder.initialize() } returns true
        coEvery { mockCameraRecorder.startSession(any(), any()) } returns mockk()

        deviceConnectionManager = DeviceConnectionManager(
            context = mockContext,
            cameraRecorder = mockCameraRecorder,
            thermalRecorder = mockThermalRecorder,
            shimmerRecorder = mockShimmerRecorder,
            jsonSocketClient = mockJsonSocketClient,
            networkConfiguration = mockNetworkConfiguration,
            logger = mockLogger
        )
    }

    describe("Camera preview initialization with retry logic") {

        it("should succeed on first attempt when TextureView is available") {
            runTest {
                // Given
                every { mockTextureView.isAvailable } returns true

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true
                verify { mockCameraRecorder.initialize(mockTextureView) }
                coVerify(exactly = 1) { mockCameraRecorder.startSession(recordVideo = false, captureRaw = false) }
            }
        }

        it("should retry camera preview start when it fails initially") {
            runTest {
                // Given
                every { mockTextureView.isAvailable } returns true
                coEvery { mockCameraRecorder.startSession(any(), any()) } returns null andThen mockk()

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true
                coVerify(exactly = 2) { mockCameraRecorder.startSession(recordVideo = false, captureRaw = false) }
                verify { mockLogger.warning(match { it.contains("Camera preview failed to start on attempt 1") }) }
                verify { mockLogger.info(match { it.contains("Camera preview started successfully on attempt 2") }) }
            }
        }

        it("should handle TextureView not available initially") {
            runTest {
                // Given
                every { mockTextureView.isAvailable } returns false andThen false andThen true

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true
                verify(atLeast = 2) { mockTextureView.isAvailable }
                coVerify(exactly = 1) { mockCameraRecorder.startSession(recordVideo = false, captureRaw = false) }
            }
        }

        it("should fail gracefully after max retries") {
            runTest {
                // Given
                every { mockTextureView.isAvailable } returns true
                coEvery { mockCameraRecorder.startSession(any(), any()) } returns null

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true // Camera is still initialized even if preview fails
                coVerify(exactly = 3) { mockCameraRecorder.startSession(recordVideo = false, captureRaw = false) }
                verify { mockLogger.warning(match { it.contains("Camera preview failed to start after 3 attempts") }) }
            }
        }

        it("should handle camera initialization failure") {
            runTest {
                // Given
                every { mockCameraRecorder.initialize(any()) } returns false

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true // Overall success if other devices work
                verify { mockLogger.error("Camera initialization returned false") }
                coVerify(exactly = 0) { mockCameraRecorder.startSession(any(), any()) }
            }
        }
    }

    describe("Thermal camera preview initialization") {

        it("should succeed with thermal preview start") {
            runTest {
                // Given
                every { mockThermalRecorder.initialize(any()) } returns true
                every { mockThermalRecorder.startPreview() } returns true

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true
                verify { mockThermalRecorder.initialize(mockSurfaceView) }
                verify(exactly = 1) { mockThermalRecorder.startPreview() }
            }
        }

        it("should retry thermal initialization when it fails initially") {
            runTest {
                // Given
                every { mockThermalRecorder.initialize(any()) } returns false andThen true
                every { mockThermalRecorder.startPreview() } returns true

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true
                verify(exactly = 2) { mockThermalRecorder.initialize(mockSurfaceView) }
                verify { mockLogger.warning(match { it.contains("Thermal camera initialization failed on attempt 1") }) }
            }
        }

        it("should retry thermal preview start when it fails initially") {
            runTest {
                // Given
                every { mockThermalRecorder.initialize(any()) } returns true
                every { mockThermalRecorder.startPreview() } returns false andThen true

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true
                verify(exactly = 2) { mockThermalRecorder.startPreview() }
                verify { mockLogger.info(match { it.contains("Attempting thermal preview start (1/3)") }) }
                verify { mockLogger.info(match { it.contains("Attempting thermal preview start (2/3)") }) }
            }
        }

        it("should handle thermal initialization failure after retries") {
            runTest {
                // Given
                every { mockThermalRecorder.initialize(any()) } returns false

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true // Overall can still succeed with other devices
                verify(exactly = 3) { mockThermalRecorder.initialize(mockSurfaceView) }
                verify { mockLogger.warning(match { it.contains("Thermal camera not available after 3 attempts") }) }
            }
        }
    }

    describe("Error handling and recovery") {

        it("should handle exceptions during camera preview start gracefully") {
            runTest {
                // Given
                coEvery { mockCameraRecorder.startSession(any(), any()) } throws RuntimeException("Camera error")

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true
                verify { mockLogger.warning(match { it.contains("Failed to start camera preview on attempt 1: Camera error") }) }
            }
        }

        it("should succeed with partial device initialization") {
            runTest {
                // Given
                every { mockCameraRecorder.initialize(any()) } returns false
                every { mockThermalRecorder.initialize(any()) } returns true
                every { mockShimmerRecorder.initialize() } returns true

                // When
                val result = deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                result.isSuccess shouldBe true
                verify { mockLogger.info(match { it.contains("Device initialization: 2/3 successful") }) }
            }
        }

        it("should include timing delays in initialization") {
            runTest {
                // Given
                val startTime = System.currentTimeMillis()

                // When
                deviceConnectionManager.initializeAllDevices(mockTextureView, mockSurfaceView)

                // Then
                val elapsed = System.currentTimeMillis() - startTime
                // Should include at least the 1000ms delay for camera stabilization
                assert(elapsed >= 900) { "Initialization should include stabilization delay (took ${elapsed}ms)" }
            }
        }
    }
})