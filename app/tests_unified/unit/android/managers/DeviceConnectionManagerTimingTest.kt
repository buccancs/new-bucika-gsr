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
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class DeviceConnectionManagerTimingTest {

    private lateinit var deviceConnectionManager: DeviceConnectionManager
    private val mockContext: Context = mockk()
    private val mockCameraRecorder: CameraRecorder = mockk()
    private val mockThermalRecorder: ThermalRecorder = mockk()
    private val mockShimmerRecorder: ShimmerRecorder = mockk()
    private val mockJsonSocketClient: JsonSocketClient = mockk()
    private val mockNetworkConfiguration: NetworkConfiguration = mockk()
    private val mockLogger: Logger = mockk()
    private val mockTextureView: TextureView = mockk()
    private val mockSurfaceView: SurfaceView = mockk()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        // Setup default mock behaviors
        every { mockLogger.info(any()) } just Runs
        every { mockLogger.warning(any()) } just Runs
        every { mockLogger.error(any()) } just Runs
        every { mockCameraRecorder.initialize(any()) } returns true
        every { mockThermalRecorder.initialize(any()) } returns true
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

    @Test
    fun initializeCameras_includesDelayBeforeStartingSession() = runTest {
        // Given
        val startTime = System.currentTimeMillis()

        // When
        deviceConnectionManager.initializeCameras(mockTextureView, mockSurfaceView)

        // Then
        val endTime = System.currentTimeMillis()
        val elapsed = endTime - startTime
        
        // Verify that the delay was included (at least 400ms, allowing for some variance)
        assertTrue("Initialization should include delay (took ${elapsed}ms)", elapsed >= 400)
        
        // Verify that initialization was called before session start
        verifyOrder {
            mockCameraRecorder.initialize(mockTextureView)
            mockCameraRecorder.startSession(recordVideo = false, captureRaw = false)
        }
    }

    @Test
    fun initializeCameras_handlesSessionStartFailureGracefully() = runTest {
        // Given
        every { mockCameraRecorder.initialize(any()) } returns true
        coEvery { mockCameraRecorder.startSession(any(), any()) } throws RuntimeException("Session start failed")

        // When
        val result = deviceConnectionManager.initializeCameras(mockTextureView, mockSurfaceView)

        // Then
        assertTrue("Initialization should still succeed even if session start fails", result)
        verify { mockLogger.warning(match { it.contains("Failed to start camera preview") }) }
    }

    @Test
    fun initializeCameras_doesNotStartSessionIfInitializationFails() = runTest {
        // Given
        every { mockCameraRecorder.initialize(any()) } returns false

        // When
        val result = deviceConnectionManager.initializeCameras(mockTextureView, mockSurfaceView)

        // Then
        assertFalse("Initialization should fail if camera initialization fails", result)
        coVerify(exactly = 0) { mockCameraRecorder.startSession(any(), any()) }
    }

    @Test
    fun initializeCameras_initializesBothCameraAndThermal() = runTest {
        // When
        deviceConnectionManager.initializeCameras(mockTextureView, mockSurfaceView)

        // Then
        verify { mockCameraRecorder.initialize(mockTextureView) }
        verify { mockThermalRecorder.initialize(mockSurfaceView) }
    }

    @Test
    fun initializeCameras_logsProgressMessages() = runTest {
        // When
        deviceConnectionManager.initializeCameras(mockTextureView, mockSurfaceView)

        // Then
        verify { mockLogger.info(match { it.contains("Starting camera preview session") }) }
        verify { mockLogger.info(match { it.contains("Camera preview started successfully") }) }
    }

    @Test
    fun initializeCameras_returnsFailureIfBothCamerasFailToInitialize() = runTest {
        // Given
        every { mockCameraRecorder.initialize(any()) } returns false
        every { mockThermalRecorder.initialize(any()) } returns false

        // When
        val result = deviceConnectionManager.initializeCameras(mockTextureView, mockSurfaceView)

        // Then
        assertFalse("Initialization should fail if both cameras fail", result)
    }

    @Test
    fun initializeCameras_succeedsIfOnlyOneCameraInitializes() = runTest {
        // Given
        every { mockCameraRecorder.initialize(any()) } returns true
        every { mockThermalRecorder.initialize(any()) } returns false

        // When
        val result = deviceConnectionManager.initializeCameras(mockTextureView, mockSurfaceView)

        // Then
        assertTrue("Initialization should succeed if at least one camera initializes", result)
    }
}