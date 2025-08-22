package com.multisensor.recording.calibration

import android.graphics.Bitmap
import com.multisensor.recording.util.Logger
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream
import kotlin.system.measureTimeMillis

/**
 * Tests for CalibrationProcessor to ensure real calibration processing without fake delays.
 * This validates that the fake calibration implementation has been replaced with real logic.
 */
@RunWith(RobolectricTestRunner::class)
class CalibrationProcessorTest {

    private lateinit var mockLogger: Logger
    private lateinit var calibrationProcessor: CalibrationProcessor
    private lateinit var testImageFile: File

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        calibrationProcessor = CalibrationProcessor(mockLogger)
        
        // Create a test image file
        testImageFile = File.createTempFile("test_calibration", ".jpg")
        createTestImage(testImageFile)
    }

    @After
    fun tearDown() {
        if (testImageFile.exists()) {
            testImageFile.delete()
        }
    }

    @Test
    fun `processCameraCalibration should complete without artificial delays`() = runTest {
        // Test that camera calibration completes quickly (no fake delays)
        val executionTime = measureTimeMillis {
            val result = calibrationProcessor.processCameraCalibration(
                rgbImagePath = testImageFile.absolutePath,
                thermalImagePath = null,
                highResolution = true
            )
            
            // Verify result structure
            assertNotNull(result)
            assertTrue("Camera calibration should have meaningful focal length", result.focalLengthX > 0)
            assertTrue("Camera calibration should have meaningful focal length", result.focalLengthY > 0)
            assertTrue("Principal point should be reasonable", result.principalPointX > 0)
            assertTrue("Principal point should be reasonable", result.principalPointY > 0)
            assertNotNull("Distortion coefficients should be provided", result.distortionCoefficients)
            assertTrue("Calibration quality should be between 0 and 1", result.calibrationQuality in 0.0..1.0)
        }
        
        // Should complete much faster than fake delays (which were 3+ seconds)
        assertTrue("Calibration should complete quickly without fake delays (was ${executionTime}ms)", 
                  executionTime < 1000)
        
        verify { mockLogger.info(match { it.contains("Starting real camera calibration") }) }
        verify { mockLogger.info(match { it.contains("Camera calibration completed") }) }
    }

    @Test
    fun `processCameraCalibration should return failure for missing image`() = runTest {
        val result = calibrationProcessor.processCameraCalibration(
            rgbImagePath = null,
            thermalImagePath = null,
            highResolution = true
        )
        
        assertFalse("Should fail when no RGB image provided", result.success)
        assertEquals("Should return zero focal lengths on failure", 0.0, result.focalLengthX, 0.001)
        assertEquals("Should return zero focal lengths on failure", 0.0, result.focalLengthY, 0.001)
        assertEquals("Should have zero quality on failure", 0.0, result.calibrationQuality, 0.001)
        assertNotNull("Should provide error message", result.errorMessage)
        assertTrue("Error message should mention missing image", 
                  result.errorMessage!!.contains("No RGB image provided"))
    }

    @Test
    fun `processThermalCalibration should complete without artificial delays`() = runTest {
        val executionTime = measureTimeMillis {
            val result = calibrationProcessor.processThermalCalibration(testImageFile.absolutePath)
            
            // Verify result structure
            assertNotNull(result)
            assertNotNull("Temperature range should be provided", result.temperatureRange)
            assertTrue("Temperature range should be reasonable", 
                      result.temperatureRange.first < result.temperatureRange.second)
            assertTrue("Calibration quality should be between 0 and 1", result.calibrationQuality in 0.0..1.0)
            assertTrue("Noise level should be non-negative", result.noiseLevel >= 0.0)
            assertTrue("Uniformity error should be non-negative", result.uniformityError >= 0.0)
        }
        
        // Should complete much faster than fake delays (which were 7+ seconds)
        assertTrue("Thermal calibration should complete quickly without fake delays (was ${executionTime}ms)", 
                  executionTime < 1000)
        
        verify { mockLogger.info(match { it.contains("Starting real thermal calibration") }) }
        verify { mockLogger.info(match { it.contains("Thermal calibration completed") }) }
    }

    @Test
    fun `processShimmerCalibration should complete without artificial delays`() = runTest {
        val executionTime = measureTimeMillis {
            val result = calibrationProcessor.processShimmerCalibration()
            
            // Verify result structure
            assertNotNull(result)
            assertTrue("GSR baseline should be reasonable", result.gsrBaseline > 0.0)
            assertNotNull("GSR range should be provided", result.gsrRange)
            assertTrue("GSR range should be valid", result.gsrRange.first < result.gsrRange.second)
            assertTrue("Sampling accuracy should be between 0 and 1", result.samplingAccuracy in 0.0..1.0)
            assertTrue("Signal-to-noise ratio should be positive", result.signalNoiseRatio > 0.0)
            assertTrue("Calibration quality should be between 0 and 1", result.calibrationQuality in 0.0..1.0)
        }
        
        // Should complete much faster than fake delays (which were 8+ seconds)
        assertTrue("Shimmer calibration should complete quickly without fake delays (was ${executionTime}ms)", 
                  executionTime < 1000)
        
        verify { mockLogger.info(match { it.contains("Starting real Shimmer sensor calibration") }) }
        verify { mockLogger.info(match { it.contains("Shimmer calibration completed") }) }
    }

    @Test
    fun `calibration quality calculation should be meaningful`() = runTest {
        val result = calibrationProcessor.processCameraCalibration(
            rgbImagePath = testImageFile.absolutePath,
            thermalImagePath = null,
            highResolution = true
        )
        
        // Quality should be calculated based on actual image analysis, not just return a fixed value
        assertTrue("Calibration quality should be meaningful (not just 1.0 or 0.0)", 
                  result.calibrationQuality > 0.0 && result.calibrationQuality < 1.0)
        
        // Should have actual calibration parameters calculated
        assertTrue("Focal length should be calculated from image dimensions", 
                  result.focalLengthX > 100) // Reasonable for test image
        
        assertNotEquals("Reprojection error should be calculated", 
                       Double.MAX_VALUE, result.reprojectionError, 0.1)
    }

    @Test
    fun `calibration should fail with poor quality images`() = runTest {
        // Create a very small, poor quality image
        val poorImageFile = File.createTempFile("poor_test", ".jpg")
        createPoorQualityImage(poorImageFile)
        
        try {
            val result = calibrationProcessor.processCameraCalibration(
                rgbImagePath = poorImageFile.absolutePath,
                thermalImagePath = null,
                highResolution = false
            )
            
            // Should either fail or have low quality score
            assertTrue("Poor quality image should either fail or have low quality score",
                      !result.success || result.calibrationQuality < 0.6)
                      
        } finally {
            poorImageFile.delete()
        }
    }

    @Test
    fun `thermal calibration should analyze actual thermal data`() = runTest {
        val result = calibrationProcessor.processThermalCalibration(testImageFile.absolutePath)
        
        if (result.success) {
            // Should have realistic temperature range
            val tempRange = result.temperatureRange
            assertTrue("Temperature range should be realistic", 
                      tempRange.first >= 0.0 && tempRange.second <= 100.0)
            assertTrue("Temperature range should span reasonable values",
                      (tempRange.second - tempRange.first) > 1.0)
                      
            // Calibration matrix should be provided
            assertTrue("Calibration matrix should not be empty", 
                      result.calibrationMatrix.isNotEmpty())
            assertTrue("Calibration matrix should be 3x3", 
                      result.calibrationMatrix.size == 3 && result.calibrationMatrix[0].size == 3)
        }
    }

    private fun createTestImage(file: File) {
        // Create a simple bitmap and save it as JPEG
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.RGB_565)
        
        // Fill with some pattern to make it more realistic
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val color = ((x + y) % 256) shl 16 or ((x * 2) % 256) shl 8 or ((y * 2) % 256)
                bitmap.setPixel(x, y, color)
            }
        }
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    private fun createPoorQualityImage(file: File) {
        // Create a very small, low quality image
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.RGB_565)
        
        // Fill with uniform color (poor for calibration)
        bitmap.eraseColor(0xFF808080.toInt())
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, out) // Very low quality
        }
    }
}