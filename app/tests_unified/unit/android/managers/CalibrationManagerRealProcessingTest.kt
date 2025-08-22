package com.multisensor.recording.managers

import com.multisensor.recording.calibration.CalibrationCaptureManager
import com.multisensor.recording.calibration.CalibrationProcessor
import com.multisensor.recording.util.Logger
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis

/**
 * Tests for CalibrationManager to ensure fake delays have been replaced with real calibration processing.
 */
@RunWith(RobolectricTestRunner::class)
class CalibrationManagerRealProcessingTest {

    private lateinit var mockCalibrationCaptureManager: CalibrationCaptureManager
    private lateinit var mockCalibrationProcessor: CalibrationProcessor
    private lateinit var mockLogger: Logger
    private lateinit var calibrationManager: CalibrationManager

    @Before
    fun setUp() {
        mockCalibrationCaptureManager = mockk(relaxed = true)
        mockCalibrationProcessor = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        
        calibrationManager = CalibrationManager(
            calibrationCaptureManager = mockCalibrationCaptureManager,
            calibrationProcessor = mockCalibrationProcessor,
            logger = mockLogger
        )
    }

    @After
    fun tearDown() {
        // Clean up
    }

    @Test
    fun `runCalibration should use real processing instead of fake delays`() = runTest {
        // Setup mocks for successful calibration
        val captureResult = CalibrationCaptureManager.CalibrationCaptureResult(
            success = true,
            calibrationId = "test_calibration",
            rgbFilePath = "/test/rgb.jpg",
            thermalFilePath = "/test/thermal.png",
            timestamp = System.currentTimeMillis(),
            syncedTimestamp = System.currentTimeMillis(),
            thermalConfig = null
        )
        
        val cameraCalibrationResult = CalibrationProcessor.CameraCalibrationResult(
            success = true,
            focalLengthX = 800.0,
            focalLengthY = 800.0,
            principalPointX = 320.0,
            principalPointY = 240.0,
            distortionCoefficients = doubleArrayOf(-0.1, 0.05, 0.0, 0.0, 0.0),
            reprojectionError = 0.5,
            calibrationQuality = 0.85
        )
        
        val thermalCalibrationResult = CalibrationProcessor.ThermalCalibrationResult(
            success = true,
            temperatureRange = Pair(20.0, 40.0),
            calibrationMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0)
            ),
            noiseLevel = 0.1,
            uniformityError = 0.05,
            calibrationQuality = 0.90
        )

        coEvery { 
            mockCalibrationCaptureManager.captureCalibrationImages(any(), any(), any(), any()) 
        } returns captureResult
        
        coEvery { 
            mockCalibrationProcessor.processCameraCalibration(any(), any(), any()) 
        } returns cameraCalibrationResult
        
        coEvery { 
            mockCalibrationProcessor.processThermalCalibration(any()) 
        } returns thermalCalibrationResult

        // Test that calibration completes quickly without fake delays
        val executionTime = measureTimeMillis {
            val result = calibrationManager.runCalibration()
            
            assertTrue("Calibration should succeed", result.isSuccess)
            val calibrationResult = result.getOrNull()
            assertNotNull("Should return calibration result", calibrationResult)
            assertTrue("Should indicate success", calibrationResult!!.success)
            assertTrue("Should include quality information", 
                      calibrationResult.message.contains("quality"))
        }
        
        // Should complete much faster than old fake delays (which were 4+ seconds total)
        assertTrue("Calibration should complete quickly without fake delays (was ${executionTime}ms)", 
                  executionTime < 2000)
        
        // Verify that real processing methods were called
        coVerify { mockCalibrationProcessor.processCameraCalibration(any(), any(), any()) }
        coVerify { mockCalibrationProcessor.processThermalCalibration(any()) }
        
        // Verify no delay-related logging
        verify(exactly = 0) { mockLogger.info(match { it.contains("delay") || it.contains("waiting") }) }
    }

    @Test
    fun `runCameraCalibration should use real processing instead of fake delays`() = runTest {
        val captureResult = CalibrationCaptureManager.CalibrationCaptureResult(
            success = true,
            calibrationId = "camera_test",
            rgbFilePath = "/test/rgb.jpg",
            thermalFilePath = null,
            timestamp = System.currentTimeMillis(),
            syncedTimestamp = System.currentTimeMillis(),
            thermalConfig = null
        )
        
        val cameraCalibrationResult = CalibrationProcessor.CameraCalibrationResult(
            success = true,
            focalLengthX = 750.0,
            focalLengthY = 750.0,
            principalPointX = 320.0,
            principalPointY = 240.0,
            distortionCoefficients = doubleArrayOf(-0.1, 0.05, 0.0, 0.0, 0.0),
            reprojectionError = 0.3,
            calibrationQuality = 0.92
        )

        coEvery { 
            mockCalibrationCaptureManager.captureCalibrationImages(any(), any(), any(), any()) 
        } returns captureResult
        
        coEvery { 
            mockCalibrationProcessor.processCameraCalibration(any(), any(), any()) 
        } returns cameraCalibrationResult

        val executionTime = measureTimeMillis {
            val result = calibrationManager.runCameraCalibration()
            
            assertTrue("Camera calibration should succeed", result.isSuccess)
            val calibrationResult = result.getOrNull()
            assertNotNull("Should return calibration result", calibrationResult)
            assertTrue("Should include quality score", 
                      calibrationResult!!.message.contains("0.92"))
        }
        
        // Should complete much faster than old fake delays (which were 2+ seconds)
        assertTrue("Camera calibration should complete quickly (was ${executionTime}ms)", 
                  executionTime < 1500)
        
        coVerify { mockCalibrationProcessor.processCameraCalibration(any(), any(), any()) }
    }

    @Test
    fun `runThermalCalibration should use real processing instead of fake delays`() = runTest {
        val captureResult = CalibrationCaptureManager.CalibrationCaptureResult(
            success = true,
            calibrationId = "thermal_test",
            rgbFilePath = null,
            thermalFilePath = "/test/thermal.png",
            timestamp = System.currentTimeMillis(),
            syncedTimestamp = System.currentTimeMillis(),
            thermalConfig = null
        )
        
        val thermalCalibrationResult = CalibrationProcessor.ThermalCalibrationResult(
            success = true,
            temperatureRange = Pair(18.0, 42.0),
            calibrationMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0)
            ),
            noiseLevel = 0.08,
            uniformityError = 0.03,
            calibrationQuality = 0.88
        )

        coEvery { 
            mockCalibrationCaptureManager.captureCalibrationImages(any(), any(), any(), any()) 
        } returns captureResult
        
        coEvery { 
            mockCalibrationProcessor.processThermalCalibration(any()) 
        } returns thermalCalibrationResult

        val executionTime = measureTimeMillis {
            val result = calibrationManager.runThermalCalibration()
            
            assertTrue("Thermal calibration should succeed", result.isSuccess)
            val calibrationResult = result.getOrNull()
            assertNotNull("Should return calibration result", calibrationResult)
            assertTrue("Should include quality score", 
                      calibrationResult!!.message.contains("0.88"))
        }
        
        // Should complete much faster than old fake delays (which were 7+ seconds)
        assertTrue("Thermal calibration should complete quickly (was ${executionTime}ms)", 
                  executionTime < 1500)
        
        coVerify { mockCalibrationProcessor.processThermalCalibration(any()) }
    }

    @Test
    fun `runShimmerCalibration should use real processing instead of fake delays`() = runTest {
        val shimmerCalibrationResult = CalibrationProcessor.ShimmerCalibrationResult(
            success = true,
            gsrBaseline = 2.5,
            gsrRange = Pair(1.0, 8.0),
            samplingAccuracy = 0.95,
            signalNoiseRatio = 45.0,
            calibrationQuality = 0.91
        )

        coEvery { 
            mockCalibrationProcessor.processShimmerCalibration() 
        } returns shimmerCalibrationResult

        val executionTime = measureTimeMillis {
            val result = calibrationManager.runShimmerCalibration()
            
            assertTrue("Shimmer calibration should succeed", result.isSuccess)
            val calibrationResult = result.getOrNull()
            assertNotNull("Should return calibration result", calibrationResult)
            assertTrue("Should include quality score", 
                      calibrationResult!!.message.contains("0.91"))
        }
        
        // Should complete much faster than old fake delays (which were 8+ seconds)
        assertTrue("Shimmer calibration should complete quickly (was ${executionTime}ms)", 
                  executionTime < 1500)
        
        coVerify { mockCalibrationProcessor.processShimmerCalibration() }
    }

    @Test
    fun `calibration should fail when processing fails`() = runTest {
        val captureResult = CalibrationCaptureManager.CalibrationCaptureResult(
            success = true,
            calibrationId = "test_calibration",
            rgbFilePath = "/test/rgb.jpg",
            thermalFilePath = "/test/thermal.png",
            timestamp = System.currentTimeMillis(),
            syncedTimestamp = System.currentTimeMillis(),
            thermalConfig = null
        )
        
        // Mock failed camera calibration processing
        val failedCameraResult = CalibrationProcessor.CameraCalibrationResult(
            success = false,
            focalLengthX = 0.0,
            focalLengthY = 0.0,
            principalPointX = 0.0,
            principalPointY = 0.0,
            distortionCoefficients = doubleArrayOf(),
            reprojectionError = Double.MAX_VALUE,
            calibrationQuality = 0.0,
            errorMessage = "Image quality too poor for calibration"
        )

        coEvery { 
            mockCalibrationCaptureManager.captureCalibrationImages(any(), any(), any(), any()) 
        } returns captureResult
        
        coEvery { 
            mockCalibrationProcessor.processCameraCalibration(any(), any(), any()) 
        } returns failedCameraResult

        val result = calibrationManager.runCalibration()
        
        assertTrue("Should fail when processing fails", result.isFailure)
        
        // Verify that processing was attempted
        coVerify { mockCalibrationProcessor.processCameraCalibration(any(), any(), any()) }
    }

    @Test
    fun `validateSystemCalibration should be based on actual calibration results`() = runTest {
        // First, complete a successful calibration
        val captureResult = CalibrationCaptureManager.CalibrationCaptureResult(
            success = true,
            calibrationId = "validation_test",
            rgbFilePath = "/test/rgb.jpg",
            thermalFilePath = null,
            timestamp = System.currentTimeMillis(),
            syncedTimestamp = System.currentTimeMillis(),
            thermalConfig = null
        )
        
        val cameraCalibrationResult = CalibrationProcessor.CameraCalibrationResult(
            success = true,
            focalLengthX = 800.0,
            focalLengthY = 800.0,
            principalPointX = 320.0,
            principalPointY = 240.0,
            distortionCoefficients = doubleArrayOf(-0.1, 0.05, 0.0, 0.0, 0.0),
            reprojectionError = 0.5,
            calibrationQuality = 0.85
        )

        coEvery { 
            mockCalibrationCaptureManager.captureCalibrationImages(any(), any(), any(), any()) 
        } returns captureResult
        
        coEvery { 
            mockCalibrationProcessor.processCameraCalibration(any(), any(), any()) 
        } returns cameraCalibrationResult

        // Run calibration first
        calibrationManager.runCameraCalibration(includeRgb = true, includeThermal = false)
        
        // Now validate - should complete quickly without delays
        val validationTime = measureTimeMillis {
            val isValid = calibrationManager.validateSystemCalibration()
            assertTrue("Validation should succeed after successful calibration", 
                      isValid.isSuccess && isValid.getOrNull() == true)
        }
        
        // Should be instant, no fake delays
        assertTrue("Validation should be immediate (was ${validationTime}ms)", 
                  validationTime < 500)
    }
}