package com.topdon.thermal.overlay

import android.graphics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprehensive test suite for TemperatureOverlayManager
 * Tests thermal data processing, overlay generation, and GSR integration
 * Critical for improving test coverage from 84% to 90% target
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class TemperatureOverlayManagerTest {

    private lateinit var temperatureOverlayManager: TemperatureOverlayManager

    @Before
    fun setUp() {
        temperatureOverlayManager = TemperatureOverlayManager()
    }

    @Test
    fun testInitialization() {
        // Test manager initialization
        Assert.assertNotNull("TemperatureOverlayManager should initialize", temperatureOverlayManager)
    }

    @Test
    fun testTemperatureFormatting() {
        // Test temperature value formatting
        val testTemperature = 36.5f
        val formattedTemp = temperatureOverlayManager.formatTemperature(testTemperature)
        
        Assert.assertNotNull("Formatted temperature should not be null", formattedTemp)
        Assert.assertTrue("Should contain temperature value", formattedTemp.contains("36.5"))
        Assert.assertTrue("Should contain degree symbol", formattedTemp.contains("°"))
    }

    @Test
    fun testCelsiusFahrenheitConversion() {
        val celsius = 36.5f
        val fahrenheit = temperatureOverlayManager.celsiusToFahrenheit(celsius)
        
        val expectedFahrenheit = 97.7f
        Assert.assertEquals("Celsius to Fahrenheit conversion", expectedFahrenheit, fahrenheit, 0.1f)
    }

    @Test
    fun testFahrenheitCelsiusConversion() {
        val fahrenheit = 97.7f
        val celsius = temperatureOverlayManager.fahrenheitToCelsius(fahrenheit)
        
        val expectedCelsius = 36.5f
        Assert.assertEquals("Fahrenheit to Celsius conversion", expectedCelsius, celsius, 0.1f)
    }

    @Test
    fun testTimestampGeneration() {
        // Test timestamp generation for overlays
        val timestamp = System.currentTimeMillis()
        val formattedTime = temperatureOverlayManager.formatTimestamp(timestamp)
        
        Assert.assertNotNull("Timestamp should be formatted", formattedTime)
        Assert.assertTrue("Timestamp should contain time format", formattedTime.matches(Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")))
    }

    @Test
    fun testOverlayBoundsCalculation() {
        // Test overlay bounds calculation for different screen sizes
        val screenWidth = 1920
        val screenHeight = 1080
        
        val bounds = temperatureOverlayManager.calculateOverlayBounds(screenWidth, screenHeight)
        
        Assert.assertNotNull("Overlay bounds should be calculated", bounds)
        Assert.assertTrue("Bounds should be within screen width", bounds.right <= screenWidth)
        Assert.assertTrue("Bounds should be within screen height", bounds.bottom <= screenHeight)
        Assert.assertTrue("Bounds should have positive dimensions", bounds.width() > 0 && bounds.height() > 0)
    }

    @Test
    fun testCrosshairPositioning() {
        // Test crosshair positioning for temperature measurement points
        val centerX = 960f
        val centerY = 540f
        val crosshairSize = 50f
        
        val crosshairPath = temperatureOverlayManager.createCrosshairPath(centerX, centerY, crosshairSize)
        
        Assert.assertNotNull("Crosshair path should be created", crosshairPath)
        Assert.assertFalse("Crosshair path should not be empty", crosshairPath.isEmpty)
    }

    @Test
    fun testMultipleTemperaturePoints() {
        // Test handling multiple temperature measurement points
        val temperaturePoints = listOf(
            TemperaturePoint(100f, 100f, 36.5f),
            TemperaturePoint(200f, 200f, 37.2f),
            TemperaturePoint(300f, 150f, 35.8f)
        )
        
        val overlay = temperatureOverlayManager.createMultiPointOverlay(temperaturePoints, 800, 600)
        
        Assert.assertNotNull("Multi-point overlay should be created", overlay)
    }

    @Test
    fun testGSRDataIntegration() {
        // Test integration with GSR data in overlays
        val gsrData = GSROverlayData(
            gsrValue = 450.5,
            skinTemperature = 32.1f,
            signalQuality = 0.85f,
            arousalState = "MEDIUM"
        )
        
        val overlayText = temperatureOverlayManager.formatGSRData(gsrData)
        
        Assert.assertNotNull("GSR overlay text should be generated", overlayText)
        Assert.assertTrue("Should contain GSR value", overlayText.contains("450.5"))
        Assert.assertTrue("Should contain skin temperature", overlayText.contains("32.1"))
        Assert.assertTrue("Should contain signal quality", overlayText.contains("85%"))
    }

    @Test
    fun testColorTemperatureMapping() {
        // Test color mapping based on temperature ranges
        val temperatures = listOf(20.0f, 25.0f, 30.0f, 35.0f, 40.0f, 45.0f)
        
        temperatures.forEach { temp ->
            val color = temperatureOverlayManager.getTemperatureColor(temp)
            
            Assert.assertNotEquals("Color should be assigned", Color.TRANSPARENT, color)
            Assert.assertTrue("Alpha channel should be set", Color.alpha(color) > 0)
        }
    }

    @Test
    fun testHeatmapColorScale() {
        // Test heatmap color scale generation
        val minTemp = 20.0f
        val maxTemp = 50.0f
        val testTemp = 35.0f
        
        val color = temperatureOverlayManager.getHeatmapColor(testTemp, minTemp, maxTemp)
        
        Assert.assertNotEquals("Heatmap color should be generated", Color.TRANSPARENT, color)
        
        // Test boundary conditions
        val minColor = temperatureOverlayManager.getHeatmapColor(minTemp, minTemp, maxTemp)
        val maxColor = temperatureOverlayManager.getHeatmapColor(maxTemp, minTemp, maxTemp)
        
        Assert.assertNotEquals("Min temperature should have color", Color.TRANSPARENT, minColor)
        Assert.assertNotEquals("Max temperature should have color", Color.TRANSPARENT, maxColor)
    }

    @Test
    fun testOverlayTextMeasurement() {
        // Test text measurement for proper overlay positioning
        val testText = "36.5°C"
        val paint = Paint().apply {
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textBounds = temperatureOverlayManager.measureText(testText, paint)
        
        Assert.assertNotNull("Text bounds should be measured", textBounds)
        Assert.assertTrue("Text should have width", textBounds.width() > 0)
        Assert.assertTrue("Text should have height", textBounds.height() > 0)
    }

    @Test
    fun testBackgroundPadding() {
        // Test background padding calculation for text overlays
        val textBounds = Rect(0, 0, 100, 30)
        val padding = 10
        
        val backgroundBounds = temperatureOverlayManager.calculateBackgroundBounds(textBounds, padding)
        
        Assert.assertEquals("Background should extend left", textBounds.left - padding, backgroundBounds.left)
        Assert.assertEquals("Background should extend right", textBounds.right + padding, backgroundBounds.right)
        Assert.assertEquals("Background should extend top", textBounds.top - padding, backgroundBounds.top)
        Assert.assertEquals("Background should extend bottom", textBounds.bottom + padding, backgroundBounds.bottom)
    }

    @Test
    fun testOverlayVisibilityControl() {
        // Test overlay visibility control
        Assert.assertTrue("Temperature overlay should be visible by default", 
            temperatureOverlayManager.isTemperatureOverlayVisible())
        
        temperatureOverlayManager.setTemperatureOverlayVisible(false)
        Assert.assertFalse("Temperature overlay should be hidden", 
            temperatureOverlayManager.isTemperatureOverlayVisible())
        
        temperatureOverlayManager.setGSROverlayVisible(false)
        Assert.assertFalse("GSR overlay should be hidden", 
            temperatureOverlayManager.isGSROverlayVisible())
    }

    @Test
    fun testOverlayConfiguration() {
        // Test overlay configuration options
        val config = OverlayConfiguration(
            temperatureUnit = TemperatureUnit.CELSIUS,
            showCrosshair = true,
            showTimestamp = true,
            showGSRData = true,
            backgroundColor = Color.argb(150, 0, 0, 0),
            textColor = Color.WHITE
        )
        
        temperatureOverlayManager.applyConfiguration(config)
        
        val appliedConfig = temperatureOverlayManager.getCurrentConfiguration()
        Assert.assertEquals("Temperature unit should be applied", TemperatureUnit.CELSIUS, appliedConfig.temperatureUnit)
        Assert.assertTrue("Crosshair should be enabled", appliedConfig.showCrosshair)
        Assert.assertTrue("Timestamp should be enabled", appliedConfig.showTimestamp)
    }

    @Test
    fun testDataValidation() {
        // Test validation of temperature and GSR data
        val validTemperature = 36.5f
        val invalidTemperature = -300f
        
        Assert.assertTrue("Valid temperature should pass validation", 
            temperatureOverlayManager.isValidTemperature(validTemperature))
        Assert.assertFalse("Invalid temperature should fail validation", 
            temperatureOverlayManager.isValidTemperature(invalidTemperature))
        
        val validGSR = 450.0
        val invalidGSR = -50.0
        
        Assert.assertTrue("Valid GSR should pass validation", 
            temperatureOverlayManager.isValidGSRValue(validGSR))
        Assert.assertFalse("Invalid GSR should fail validation", 
            temperatureOverlayManager.isValidGSRValue(invalidGSR))
    }

    @Test
    fun testPerformanceOptimization() {
        // Test performance with rapid overlay generation
        val startTime = System.nanoTime()
        val iterations = 1000
        
        repeat(iterations) { index ->
            val temperaturePoint = TemperaturePoint(
                x = (index % 800).toFloat(),
                y = (index % 600).toFloat(), 
                temperature = 20f + (index % 30)
            )
            temperatureOverlayManager.createSinglePointOverlay(temperaturePoint, 800, 600)
        }
        
        val endTime = System.nanoTime()
        val averageTime = (endTime - startTime) / iterations
        
        // Should complete overlay generation efficiently
        Assert.assertTrue("Overlay generation should be efficient", 
            averageTime < 1_000_000L) // Less than 1ms per overlay
    }

    @Test
    fun testMemoryManagement() {
        // Test memory management during overlay operations
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Generate many overlays to test memory usage
        repeat(100) { index ->
            val points = (0 until 10).map { 
                TemperaturePoint(it * 50f, it * 40f, 20f + it)
            }
            temperatureOverlayManager.createMultiPointOverlay(points, 1920, 1080)
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val afterOperations = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryGrowth = afterOperations - initialMemory
        
        // Memory growth should be reasonable
        Assert.assertTrue("Memory growth should be controlled", memoryGrowth < 50_000_000L) // Less than 50MB
    }

    @Test
    fun testErrorHandling() {
        // Test error handling with invalid inputs
        val result1 = temperatureOverlayManager.createSinglePointOverlay(null, 800, 600)
        Assert.assertNull("Should handle null temperature point gracefully", result1)
        
        val result2 = temperatureOverlayManager.formatTemperature(Float.NaN)
        Assert.assertEquals("Should handle NaN temperature", "---", result2)
        
        val result3 = temperatureOverlayManager.getTemperatureColor(Float.POSITIVE_INFINITY)
        Assert.assertEquals("Should handle infinite temperature", Color.RED, result3)
    }

    // Data classes for testing
    data class TemperaturePoint(
        val x: Float,
        val y: Float,
        val temperature: Float
    )
    
    data class GSROverlayData(
        val gsrValue: Double,
        val skinTemperature: Float,
        val signalQuality: Float,
        val arousalState: String
    )
    
    data class OverlayConfiguration(
        val temperatureUnit: TemperatureUnit,
        val showCrosshair: Boolean,
        val showTimestamp: Boolean,
        val showGSRData: Boolean,
        val backgroundColor: Int,
        val textColor: Int
    )
    
    enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
}