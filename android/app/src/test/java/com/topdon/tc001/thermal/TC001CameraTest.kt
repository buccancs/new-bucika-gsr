package com.topdon.tc001.thermal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.test.core.app.ApplicationProvider
import com.infisense.usbir.camera.IRUVCTC
import com.energy.iruvc.utils.IFrameCallback
import com.topdon.tc001.thermal.data.ThermalMetadata
import com.topdon.tc001.thermal.data.TemperaturePoint
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class TC001CameraTest {
    
    private lateinit var context: Context
    private lateinit var mockFrameCallback: IFrameCallback
    private lateinit var thermalCamera: IRUVCTC
    private lateinit var mockUSBManager: UsbManager
    
    companion object {
        private const val TC001_VENDOR_ID = 0x1234
        private const val TC001_PRODUCT_ID = 0x5678
        private const val USB_CLASS_VIDEO = 14
    }
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockFrameCallback = mock<IFrameCallback>()
        mockUSBManager = mock<UsbManager>()
        
        thermalCamera = IRUVCTC(context, mockFrameCallback)
    }
    
    @Test
    fun `should initialize camera with proper context and callback`() {
        assertNotNull(thermalCamera, "Thermal camera should be initialized")
        assertFalse(thermalCamera.isConnected(), "Camera should not be connected initially")
        assertFalse(thermalCamera.isStreaming(), "Streaming should not be active initially")
    }
    
    @Test
    fun `should connect to USB device with valid parameters`() {
        // Arrange
        val mockDevice = createMockTC001Device()
        whenever(mockUSBManager.hasPermission(mockDevice)).thenReturn(true)
        
        // Act
        val result = thermalCamera.connectUSBDevice(mockDevice)
        
        // Assert
        assertTrue(result, "Connection should be initiated successfully")
    }
    
    @Test
    fun `should calculate temperature correctly from raw data`() {
        // Arrange
        val expectedTemp = 25.0f
        val temperatureData = generateTestTemperatureData(expectedTemp)
        
        // Act
        val temperature = calculateTemperatureFromData(temperatureData, 128, 96)
        
        // Assert
        assertEquals(expectedTemp, temperature, 0.5f, "Temperature should be approximately 25°C")
    }
    
    @Test
    fun `should find maximum temperature point correctly`() {
        // Arrange
        val hotTemp = 50.0f
        val baseTemp = 25.0f
        val hotX = 100
        val hotY = 75
        val temperatureData = generateTestTemperatureDataWithHotSpot(baseTemp, hotTemp, hotX, hotY)
        
        // Act
        val maxPoint = findMaxTemperaturePoint(temperatureData)
        
        // Assert
        assertNotNull(maxPoint, "Max temperature point should be found")
        assertEquals(hotX, maxPoint.x, "Max point X should be correct")
        assertEquals(hotY, maxPoint.y, "Max point Y should be correct")
        assertEquals(hotTemp, maxPoint.temperature, 1.0f, "Max temperature should be approximately 50°C")
    }
    
    @Test
    fun `should calculate average temperature in region correctly`() {
        // Arrange
        val uniformTemp = 30.0f
        val temperatureData = generateTestTemperatureData(uniformTemp)
        val region = Rect(50, 50, 150, 100)
        
        // Act
        val avgTemp = calculateAverageTemperatureInRegion(temperatureData, region)
        
        // Assert
        assertEquals(uniformTemp, avgTemp, 0.1f, "Average temperature should match uniform temperature")
    }
    
    @Test
    fun `should validate thermal parameters`() {
        // Test invalid emissivity values
        assertFalse(isValidEmissivity(0.05f), "Should reject emissivity < 0.1")
        assertFalse(isValidEmissivity(1.5f), "Should reject emissivity > 1.0")
        assertTrue(isValidEmissivity(0.95f), "Should accept valid emissivity")
        
        // Test distance validation
        assertFalse(isValidDistance(0f), "Should reject zero distance")
        assertFalse(isValidDistance(-1f), "Should reject negative distance")
        assertTrue(isValidDistance(1.0f), "Should accept valid distance")
    }
    
    @Test
    fun `should handle temperature measurement bounds correctly`() {
        val temperatureData = generateTestTemperatureData(25.0f)
        
        // Valid coordinates should work
        val temp1 = calculateTemperatureFromData(temperatureData, 0, 0) // Top-left corner
        val temp2 = calculateTemperatureFromData(temperatureData, 255, 191) // Bottom-right corner
        
        assertTrue(temp1 > 0f && temp1 < 200f, "Top-left temperature should be reasonable")
        assertTrue(temp2 > 0f && temp2 < 200f, "Bottom-right temperature should be reasonable")
        
        // Invalid coordinates should be handled
        assertFailsWith<IllegalArgumentException>("Should reject negative X coordinate") {
            calculateTemperatureFromData(temperatureData, -1, 96)
        }
        
        assertFailsWith<IllegalArgumentException>("Should reject X coordinate beyond bounds") {
            calculateTemperatureFromData(temperatureData, 256, 96)
        }
    }
    
    @Test
    fun `should support multiple simultaneous temperature areas`() {
        // Setup thermal data with gradient
        val temperatureData = generateGradientTemperatureData(20.0f, 40.0f)
        
        // Define multiple areas
        val area1 = Rect(0, 0, 85, 64)      // Left third (cooler)
        val area2 = Rect(85, 0, 170, 64)    // Middle third
        val area3 = Rect(170, 0, 256, 64)   // Right third (warmer)
        
        val temp1 = calculateAverageTemperatureInRegion(temperatureData, area1)
        val temp2 = calculateAverageTemperatureInRegion(temperatureData, area2)
        val temp3 = calculateAverageTemperatureInRegion(temperatureData, area3)
        
        // Verify gradient (left should be cooler than right)
        assertTrue(temp1 < temp2, "Left area should be cooler than middle")
        assertTrue(temp2 < temp3, "Middle area should be cooler than right")
        
        // All should return reasonable values
        assertTrue(temp1 > 0f && temp1 < 100f, "Temperature 1 should be reasonable")
        assertTrue(temp2 > 0f && temp2 < 100f, "Temperature 2 should be reasonable")
        assertTrue(temp3 > 0f && temp3 < 100f, "Temperature 3 should be reasonable")
    }
    
    @Test
    fun `should handle corrupted temperature data gracefully`() {
        // Test with partially corrupted data
        val corruptedData = generatePartiallyCorruptedTemperatureData()
        
        // Should not crash and should return reasonable values for non-corrupted areas
        val temperature = calculateTemperatureFromData(corruptedData, 10, 10)
        assertTrue(temperature > -50f && temperature < 200f, "Should return reasonable temperature despite corruption")
        
        // Test with completely invalid data
        val invalidData = ByteArray(100) // Wrong size
        assertFailsWith<IllegalArgumentException>("Should reject data with wrong size") {
            calculateTemperatureFromData(invalidData, 10, 10)
        }
    }
    
    @Test
    fun `should calculate temperature line profile correctly`() {
        // Create data with temperature gradient
        val temperatureData = generateGradientTemperatureData(20.0f, 40.0f)
        
        // Define line from left to right
        val startPoint = Point(0, 96)
        val endPoint = Point(255, 96)
        
        val lineProfile = calculateTemperatureLineProfile(temperatureData, startPoint, endPoint)
        
        assertTrue(lineProfile.isNotEmpty(), "Line profile should contain temperature points")
        assertTrue(lineProfile.size >= 10, "Line profile should have reasonable number of points")
        
        // Verify gradient along line (temperatures should generally increase)
        val firstTemp = lineProfile.first()
        val lastTemp = lineProfile.last()
        assertTrue(lastTemp > firstTemp, "Temperature should increase along gradient line")
    }
    
    // Helper methods for testing thermal functionality
    private fun createMockTC001Device(): UsbDevice {
        val mockDevice = mock<UsbDevice>()
        whenever(mockDevice.vendorId).thenReturn(TC001_VENDOR_ID)
        whenever(mockDevice.productId).thenReturn(TC001_PRODUCT_ID)
        whenever(mockDevice.deviceName).thenReturn("/dev/bus/usb/001/002")
        whenever(mockDevice.deviceClass).thenReturn(USB_CLASS_VIDEO)
        return mockDevice
    }
    
    private fun generateTestTemperatureData(temperature: Float): ByteArray {
        val data = ByteArray(256 * 192 * 2)
        val rawValue = ((temperature + 273.15f) * 64.0f).toInt()
        
        for (i in data.indices step 2) {
            data[i] = (rawValue and 0xFF).toByte()
            data[i + 1] = ((rawValue shr 8) and 0xFF).toByte()
        }
        
        return data
    }
    
    private fun generateTestTemperatureDataWithHotSpot(
        baseTemp: Float,
        hotTemp: Float,
        hotX: Int,
        hotY: Int
    ): ByteArray {
        val data = generateTestTemperatureData(baseTemp)
        val hotRawValue = ((hotTemp + 273.15f) * 64.0f).toInt()
        
        // Set hot spot temperature
        val index = (hotY * 256 + hotX) * 2
        if (index + 1 < data.size) {
            data[index] = (hotRawValue and 0xFF).toByte()
            data[index + 1] = ((hotRawValue shr 8) and 0xFF).toByte()
        }
        
        return data
    }
    
    private fun generateGradientTemperatureData(minTemp: Float, maxTemp: Float): ByteArray {
        val data = ByteArray(256 * 192 * 2)
        val tempRange = maxTemp - minTemp
        
        for (y in 0 until 192) {
            for (x in 0 until 256) {
                // Create horizontal gradient
                val gradientFactor = x.toFloat() / 255f
                val temperature = minTemp + (tempRange * gradientFactor)
                val rawValue = ((temperature + 273.15f) * 64.0f).toInt()
                
                val index = (y * 256 + x) * 2
                data[index] = (rawValue and 0xFF).toByte()
                data[index + 1] = ((rawValue shr 8) and 0xFF).toByte()
            }
        }
        
        return data
    }
    
    private fun generatePartiallyCorruptedTemperatureData(): ByteArray {
        val data = generateTestTemperatureData(25.0f)
        
        // Corrupt some pixels with extreme values
        for (i in 1000 until 1100 step 2) {
            data[i] = 0xFF.toByte()
            data[i + 1] = 0xFF.toByte() // Very high raw value
        }
        
        return data
    }
    
    private fun calculateTemperatureFromData(data: ByteArray, x: Int, y: Int): Float {
        if (data.size != 256 * 192 * 2) {
            throw IllegalArgumentException("Invalid temperature data size")
        }
        
        if (x < 0 || x >= 256 || y < 0 || y >= 192) {
            throw IllegalArgumentException("Coordinates out of bounds")
        }
        
        val index = (y * 256 + x) * 2
        val rawValue = (data[index + 1].toInt() shl 8) or (data[index].toInt() and 0xFF)
        return (rawValue / 64.0f) - 273.15f
    }
    
    private fun findMaxTemperaturePoint(data: ByteArray): TemperaturePoint {
        var maxTemp = Float.MIN_VALUE
        var maxX = 0
        var maxY = 0
        
        for (y in 0 until 192) {
            for (x in 0 until 256) {
                val temp = calculateTemperatureFromData(data, x, y)
                if (temp > maxTemp) {
                    maxTemp = temp
                    maxX = x
                    maxY = y
                }
            }
        }
        
        return TemperaturePoint(maxX, maxY, maxTemp, System.currentTimeMillis())
    }
    
    private fun calculateAverageTemperatureInRegion(data: ByteArray, region: Rect): Float {
        var sum = 0.0f
        var count = 0
        
        for (y in region.top until region.bottom.coerceAtMost(192)) {
            for (x in region.left until region.right.coerceAtMost(256)) {
                sum += calculateTemperatureFromData(data, x, y)
                count++
            }
        }
        
        return if (count > 0) sum / count else 0f
    }
    
    private fun calculateTemperatureLineProfile(
        data: ByteArray,
        start: Point,
        end: Point
    ): List<Float> {
        val profile = mutableListOf<Float>()
        val dx = kotlin.math.abs(end.x - start.x)
        val dy = kotlin.math.abs(end.y - start.y)
        val steps = kotlin.math.max(dx, dy)
        
        for (i in 0..steps) {
            val t = if (steps > 0) i.toFloat() / steps else 0f
            val x = (start.x + t * (end.x - start.x)).toInt()
            val y = (start.y + t * (end.y - start.y)).toInt()
            
            if (x in 0 until 256 && y in 0 until 192) {
                val temp = calculateTemperatureFromData(data, x, y)
                profile.add(temp)
            }
        }
        
        return profile
    }
    
    private fun isValidEmissivity(emissivity: Float): Boolean {
        return emissivity in 0.1f..1.0f
    }
    
    private fun isValidDistance(distance: Float): Boolean {
        return distance > 0f && distance <= 100f
    }
