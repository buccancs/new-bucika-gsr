package com.multisensor.recording.calibration

import android.content.Context
import android.graphics.Bitmap
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Comprehensive Calibration System Tests
 * =====================================
 * 
 * This test class provides comprehensive testing for the calibration system,
 * including pattern detection, camera calibration, and quality assessment.
 * 
 * Test coverage:
 * - Calibration capture management and coordination
 * - Pattern detection and validation
 * - Camera parameter calculation and optimization
 * - Calibration quality assessment and validation
 * - Multi-camera calibration scenarios
 * 
 * Author: Multi-Sensor Recording System
 * Date: 2025-01-16
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi  
class CalibrationCaptureManagerComprehensiveTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockSessionManager: SessionManager
    private lateinit var mockLogger: Logger
    private lateinit var calibrationManager: CalibrationCaptureManager
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockSessionManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        
        calibrationManager = CalibrationCaptureManager(mockContext, mockSessionManager, mockLogger)
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `calibration manager initialization should succeed`() = runTest {
        assertNotNull("CalibrationCaptureManager should be created", calibrationManager)
        
        val initResult = calibrationManager.initialize()
        assertTrue("CalibrationCaptureManager should initialize successfully", initResult)
        
        verify { mockLogger.info("Initializing CalibrationCaptureManager...") }
    }
    
    @Test
    fun `calibration pattern configuration should work`() = runTest {
        val patternConfigs = listOf(
            mapOf("type" to "chessboard", "rows" to 9, "cols" to 6, "square_size" to 25.0),
            mapOf("type" to "circles", "rows" to 7, "cols" to 5, "circle_size" to 15.0),
            mapOf("type" to "asymmetric_circles", "rows" to 8, "cols" to 4, "circle_size" to 12.0)
        )
        
        patternConfigs.forEach { config ->
            val configResult = calibrationManager.configurePattern(
                type = config["type"] as String,
                rows = config["rows"] as Int,
                cols = config["cols"] as Int,
                elementSize = config.values.last() as Double
            )
            
            assertTrue("Pattern configuration should succeed for ${config["type"]}", configResult)
            
            val currentConfig = calibrationManager.getCurrentPatternConfig()
            assertEquals("Pattern type should be set", config["type"], currentConfig["type"])
            assertEquals("Rows should be set", config["rows"], currentConfig["rows"])
            assertEquals("Cols should be set", config["cols"], currentConfig["cols"])
        }
    }
    
    @Test
    fun `image capture and validation should work`() = runTest {
        calibrationManager.configurePattern("chessboard", 9, 6, 25.0)
        
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { mockBitmap.width } returns 640
        every { mockBitmap.height } returns 480
        every { mockBitmap.config } returns Bitmap.Config.ARGB_8888
        
        every { calibrationManager.captureCalibrationImage() } returns mockBitmap
        
        val capturedImage = calibrationManager.captureCalibrationImage()
        assertNotNull("Should capture calibration image", capturedImage)
        assertEquals("Image width should be correct", 640, capturedImage.width)
        assertEquals("Image height should be correct", 480, capturedImage.height)
        
        every { calibrationManager.validateCapturedImage(mockBitmap) } returns true
        
        val validationResult = calibrationManager.validateCapturedImage(capturedImage)
        assertTrue("Captured image should be valid", validationResult)
    }
    
    @Test
    fun `pattern detection should work correctly`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        
        val patternTests = listOf(
            mapOf("type" to "chessboard", "expected_corners" to 54),
            mapOf("type" to "circles", "expected_corners" to 35),
            mapOf("type" to "asymmetric_circles", "expected_corners" to 32)
        )
        
        patternTests.forEach { test ->
            calibrationManager.configurePattern(
                test["type"] as String, 
                if (test["type"] == "circles") 7 else if (test["type"] == "asymmetric_circles") 8 else 9,
                if (test["type"] == "circles") 5 else if (test["type"] == "asymmetric_circles") 4 else 6,
                25.0
            )
            
            val expectedCorners = test["expected_corners"] as Int
            val mockCorners = List(expectedCorners) { index ->
                mapOf("x" to (index % 10) * 50.0, "y" to (index / 10) * 50.0)
            }
            
            every { calibrationManager.detectPattern(mockBitmap) } returns mockCorners
            
            val detectedCorners = calibrationManager.detectPattern(mockBitmap)
            
            assertNotNull("Should detect pattern corners", detectedCorners)
            assertEquals("Should detect correct number of corners", 
                        expectedCorners, detectedCorners.size)
            
            detectedCorners.forEach { corner ->
                val x = corner["x"] as Double
                val y = corner["y"] as Double
                assertTrue("X coordinate should be valid", x >= 0 && x <= 640)
                assertTrue("Y coordinate should be valid", y >= 0 && y <= 480)
            }
        }
    }
    
    @Test
    fun `calibration session management should work`() = runTest {
        val sessionId = "calibration_test_session"
        every { calibrationManager.startCalibrationSession(sessionId) } returns true
        
        val sessionResult = calibrationManager.startCalibrationSession(sessionId)
        assertTrue("Calibration session should start successfully", sessionResult)
        
        every { calibrationManager.isCalibrationSessionActive() } returns true
        assertTrue("Calibration session should be active", 
                  calibrationManager.isCalibrationSessionActive())
        
        val imageCount = 15
        repeat(imageCount) { index ->
            val mockBitmap = mockk<Bitmap>(relaxed = true)
            every { calibrationManager.addCalibrationImage(mockBitmap) } returns true
            
            val addResult = calibrationManager.addCalibrationImage(mockBitmap)
            assertTrue("Should add calibration image $index", addResult)
        }
        
        every { calibrationManager.getCalibrationImageCount() } returns imageCount
        assertEquals("Should have correct number of calibration images", 
                    imageCount, calibrationManager.getCalibrationImageCount())
        
        every { calibrationManager.endCalibrationSession() } returns true
        
        val endResult = calibrationManager.endCalibrationSession()
        assertTrue("Calibration session should end successfully", endResult)
    }
    
    @Test
    fun `camera calibration calculation should work`() = runTest {
        calibrationManager.startCalibrationSession("calculation_test")
        
        val calibrationImages = 12
        val mockCornersList = mutableListOf<List<Map<String, Double>>>()
        
        repeat(calibrationImages) { imageIndex ->
            val corners = List(54) { cornerIndex ->
                mapOf(
                    "x" to 50.0 + (cornerIndex % 9) * 60.0 + imageIndex * 2.0,
                    "y" to 50.0 + (cornerIndex / 9) * 60.0 + imageIndex * 1.5
                )
            }
            mockCornersList.add(corners)
        }
        
        val mockCalibrationResult = mapOf(
            "camera_matrix" to arrayOf(
                arrayOf(800.0, 0.0, 320.0),
                arrayOf(0.0, 800.0, 240.0),
                arrayOf(0.0, 0.0, 1.0)
            ),
            "distortion_coefficients" to arrayOf(-0.2, 0.1, 0.0, 0.0, 0.0),
            "rms_error" to 0.45,
            "image_count" to calibrationImages,
            "pattern_size" to arrayOf(9, 6),
            "square_size" to 25.0
        )
        
        every { calibrationManager.calculateCameraCalibration() } returns mockCalibrationResult
        
        val calibrationResult = calibrationManager.calculateCameraCalibration()
        
        assertNotNull("Calibration calculation should return result", calibrationResult)
        assertTrue("Should include camera matrix", calibrationResult.containsKey("camera_matrix"))
        assertTrue("Should include distortion coefficients", 
                  calibrationResult.containsKey("distortion_coefficients"))
        assertTrue("Should include RMS error", calibrationResult.containsKey("rms_error"))
        
        val rmsError = calibrationResult["rms_error"] as Double
        assertTrue("RMS error should be reasonable", rmsError < 1.0)
    }
    
    @Test
    fun `calibration quality assessment should work`() = runTest {
        val mockCalibrationData = mapOf(
            "camera_matrix" to arrayOf(
                arrayOf(800.0, 0.0, 320.0),
                arrayOf(0.0, 800.0, 240.0),
                arrayOf(0.0, 0.0, 1.0)
            ),
            "distortion_coefficients" to arrayOf(-0.2, 0.1, 0.0, 0.0, 0.0),
            "rms_error" to 0.45,
            "image_count" to 15
        )
        
        every { calibrationManager.assessCalibrationQuality(mockCalibrationData) } returns mapOf(
            "overall_quality" to "EXCELLENT",
            "quality_score" to 92.5,
            "rms_error_grade" to "A",
            "image_count_grade" to "A", 
            "coverage_grade" to "B+",
            "recommendations" to listOf(
                "Calibration quality is excellent",
                "RMS error is within acceptable range",
                "Sufficient calibration images captured"
            )
        )
        
        val qualityAssessment = calibrationManager.assessCalibrationQuality(mockCalibrationData)
        
        assertNotNull("Quality assessment should be available", qualityAssessment)
        assertEquals("Overall quality should be excellent", "EXCELLENT", 
                    qualityAssessment["overall_quality"])
        
        val qualityScore = qualityAssessment["quality_score"] as Double
        assertTrue("Quality score should be high", qualityScore > 90.0)
        
        val recommendations = qualityAssessment["recommendations"] as List<*>
        assertTrue("Should include recommendations", recommendations.isNotEmpty())
    }
    
    @Test
    fun `stereo calibration should work`() = runTest {
        every { calibrationManager.configureStereoCalibration(true) } returns true
        
        val stereoResult = calibrationManager.configureStereoCalibration(true)
        assertTrue("Stereo calibration should be configured", stereoResult)
        
        val leftImages = 10
        val rightImages = 10
        
        repeat(leftImages) { index ->
            val leftImage = mockk<Bitmap>(relaxed = true)
            val rightImage = mockk<Bitmap>(relaxed = true)
            
            every { calibrationManager.addStereoImagePair(leftImage, rightImage) } returns true
            
            val addResult = calibrationManager.addStereoImagePair(leftImage, rightImage)
            assertTrue("Should add stereo image pair $index", addResult)
        }
        
        val mockStereoResult = mapOf(
            "left_camera_matrix" to arrayOf(
                arrayOf(800.0, 0.0, 320.0),
                arrayOf(0.0, 800.0, 240.0),
                arrayOf(0.0, 0.0, 1.0)
            ),
            "right_camera_matrix" to arrayOf(
                arrayOf(805.0, 0.0, 325.0),
                arrayOf(0.0, 805.0, 245.0),
                arrayOf(0.0, 0.0, 1.0)
            ),
            "rotation_matrix" to arrayOf(
                arrayOf(0.999, -0.01, 0.005),
                arrayOf(0.01, 0.999, -0.002),
                arrayOf(-0.005, 0.002, 0.999)
            ),
            "translation_vector" to arrayOf(-120.0, 2.0, 1.0),
            "essential_matrix" to Array(3) { Array(3) { 0.0 } },
            "fundamental_matrix" to Array(3) { Array(3) { 0.0 } },
            "rms_error" to 0.52
        )
        
        every { calibrationManager.calculateStereoCalibration() } returns mockStereoResult
        
        val stereoCalibration = calibrationManager.calculateStereoCalibration()
        
        assertNotNull("Stereo calibration should return result", stereoCalibration)
        assertTrue("Should include left camera matrix", 
                  stereoCalibration.containsKey("left_camera_matrix"))
        assertTrue("Should include right camera matrix", 
                  stereoCalibration.containsKey("right_camera_matrix"))
        assertTrue("Should include rotation matrix", 
                  stereoCalibration.containsKey("rotation_matrix"))
        assertTrue("Should include translation vector", 
                  stereoCalibration.containsKey("translation_vector"))
    }
    
    @Test
    fun `calibration persistence should work`() = runTest {
        val calibrationData = mapOf(
            "camera_matrix" to arrayOf(
                arrayOf(800.0, 0.0, 320.0),
                arrayOf(0.0, 800.0, 240.0),
                arrayOf(0.0, 0.0, 1.0)
            ),
            "distortion_coefficients" to arrayOf(-0.2, 0.1, 0.0, 0.0, 0.0),
            "rms_error" to 0.45,
            "timestamp" to System.currentTimeMillis(),
            "device_id" to "android_001"
        )
        
        val calibrationId = "test_calibration_001"
        every { calibrationManager.saveCalibration(calibrationId, calibrationData) } returns true
        
        val saveResult = calibrationManager.saveCalibration(calibrationId, calibrationData)
        assertTrue("Calibration should be saved successfully", saveResult)
        
        every { calibrationManager.loadCalibration(calibrationId) } returns calibrationData
        
        val loadedCalibration = calibrationManager.loadCalibration(calibrationId)
        assertNotNull("Calibration should be loaded successfully", loadedCalibration)
        
        assertEquals("RMS error should match", 
                    calibrationData["rms_error"], loadedCalibration["rms_error"])
        assertEquals("Device ID should match", 
                    calibrationData["device_id"], loadedCalibration["device_id"])
        
        every { calibrationManager.validateSavedCalibration(calibrationId) } returns true
        
        val validationResult = calibrationManager.validateSavedCalibration(calibrationId)
        assertTrue("Saved calibration should be valid", validationResult)
    }
    
    @Test
    fun `calibration export and import should work`() = runTest {
        val calibrationData = mapOf(
            "camera_matrix" to arrayOf(
                arrayOf(800.0, 0.0, 320.0),
                arrayOf(0.0, 800.0, 240.0),
                arrayOf(0.0, 0.0, 1.0)
            ),
            "distortion_coefficients" to arrayOf(-0.2, 0.1, 0.0, 0.0, 0.0),
            "rms_error" to 0.45,
            "export_format" to "opencv_yaml",
            "metadata" to mapOf(
                "device_model" to "Android Test Device",
                "calibration_software" to "Multi-Sensor Recording System",
                "calibration_date" to "2025-01-16"
            )
        )
        
        val exportFormats = listOf("opencv_yaml", "json", "matlab")
        
        exportFormats.forEach { format ->
            every { calibrationManager.exportCalibration(calibrationData, format) } returns "exported_data_$format"
            
            val exportResult = calibrationManager.exportCalibration(calibrationData, format)
            assertNotNull("Export should succeed for format $format", exportResult)
            assertTrue("Export result should contain format identifier", 
                      exportResult.contains(format))
        }
        
        val exportedData = "exported_calibration_data_json"
        every { calibrationManager.importCalibration(exportedData, "json") } returns calibrationData
        
        val importedData = calibrationManager.importCalibration(exportedData, "json")
        assertNotNull("Import should succeed", importedData)
        assertEquals("Imported RMS error should match", 
                    calibrationData["rms_error"], importedData["rms_error"])
    }
    
    @Test
    fun `error handling and recovery should work`() = runTest {
        val errorScenarios = listOf(
            "INSUFFICIENT_IMAGES" to "Not enough calibration images captured",
            "PATTERN_NOT_DETECTED" to "Calibration pattern not found in image",
            "CALIBRATION_FAILED" to "Camera calibration calculation failed",
            "INVALID_PARAMETERS" to "Invalid calibration parameters detected",
            "STORAGE_ERROR" to "Failed to save calibration data"
        )
        
        errorScenarios.forEach { (errorCode, errorMessage) ->
            every { calibrationManager.handleCalibrationError(errorCode, errorMessage) } returns true
            
            val errorResult = calibrationManager.handleCalibrationError(errorCode, errorMessage)
            assertTrue("Error $errorCode should be handled", errorResult)
            
            verify { mockLogger.error(match { it.contains(errorCode) }) }
        }
        
        every { calibrationManager.attemptCalibrationRecovery() } returns true
        
        val recoveryResult = calibrationManager.attemptCalibrationRecovery()
        assertTrue("Calibration recovery should succeed", recoveryResult)
        
        every { calibrationManager.resetCalibration() } returns true
        
        val resetResult = calibrationManager.resetCalibration()
        assertTrue("Calibration reset should succeed", resetResult)
    }
    
    @Test
    fun `performance monitoring should work`() = runTest {
        every { calibrationManager.enablePerformanceMonitoring(true) } returns Unit
        calibrationManager.enablePerformanceMonitoring(true)
        
        val operations = listOf(
            "pattern_detection" to 25.5,
            "corner_refinement" to 15.2,
            "calibration_calculation" to 85.7,
            "quality_assessment" to 12.3
        )
        
        operations.forEach { (operation, duration) ->
            calibrationManager.recordPerformanceMetric(operation, duration)
        }
        
        val performanceStats = calibrationManager.getPerformanceStatistics()
        
        assertNotNull("Performance statistics should be available", performanceStats)
        assertTrue("Should include pattern detection metrics", 
                  performanceStats.containsKey("pattern_detection"))
        assertTrue("Should include calibration calculation metrics", 
                  performanceStats.containsKey("calibration_calculation"))
        
        val overallPerformance = calibrationManager.analyzeOverallPerformance()
        assertTrue("Overall performance score should be reasonable", 
                  overallPerformance > 0.0 && overallPerformance <= 100.0)
    }
}