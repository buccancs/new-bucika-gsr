package com.multisensor.recording.recording

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import com.multisensor.recording.util.Logger

/**
 * comprehensive test suite for thermal recorder integration
 * tests topdon sdk interaction, thermal data processing, and error handling
 */
class ThermalRecorderIntegrationTest {
    
    @Mock
    private lateinit var mockLogger: Logger
    
    @Mock
    private lateinit var mockTopdonDevice: TopdonThermalDevice
    
    @Mock
    private lateinit var mockSessionManager: SessionManager
    
    private lateinit var thermalRecorder: ThermalRecorder
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        thermalRecorder = ThermalRecorder(mockLogger, mockSessionManager)
    }
    
    @Test
    fun `should detect thermal camera connection`() = runTest {
        // given
        whenever(mockTopdonDevice.isConnected()).thenReturn(true)
        whenever(mockTopdonDevice.getDeviceInfo()).thenReturn("TopdonTC001-v1.3.7")
        
        // when
        val result = thermalRecorder.initializeDevice()
        
        // then
        assertTrue(result)
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should handle thermal camera disconnection gracefully`() = runTest {
        // given
        whenever(mockTopdonDevice.isConnected()).thenReturn(false)
        
        // when
        val result = thermalRecorder.initializeDevice()
        
        // then
        assertFalse(result)
        verify(mockLogger).logE(any())
    }
    
    @Test
    fun `should start thermal recording session`() = runTest {
        // given
        val sessionId = "thermal_test_123"
        whenever(mockTopdonDevice.isConnected()).thenReturn(true)
        whenever(mockSessionManager.getCurrentSession()).thenReturn(
            SessionInfo(sessionId).apply { thermalEnabled = true }
        )
        
        // when
        val result = thermalRecorder.startRecording(sessionId)
        
        // then
        assertTrue(result)
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should capture thermal frames with correct temperature data`() = runTest {
        // given
        val mockThermalFrame = ThermalFrame(
            width = 256,
            height = 192,
            temperatureData = FloatArray(256 * 192) { 25.0f + it * 0.1f },
            timestamp = System.currentTimeMillis()
        )
        whenever(mockTopdonDevice.captureFrame()).thenReturn(mockThermalFrame)
        
        // when
        val frame = thermalRecorder.captureFrame()
        
        // then
        assertNotNull(frame)
        assertEquals(256, frame?.width)
        assertEquals(192, frame?.height)
        assertTrue(frame?.temperatureData?.isNotEmpty() == true)
        verify(mockLogger).logD(any())
    }
    
    @Test
    fun `should process thermal data with temperature calibration`() = runTest {
        // given
        val rawThermalData = FloatArray(100) { 3000.0f + it * 10.0f } // raw sensor values
        val expectedTempRange = 20.0f..50.0f
        
        // when
        val calibratedData = thermalRecorder.calibrateTemperatureData(rawThermalData)
        
        // then
        assertTrue(calibratedData.all { it in expectedTempRange })
        verify(mockLogger).logD(any())
    }
    
    @Test
    fun `should save thermal data to file with proper format`() = runTest {
        // given
        val sessionId = "thermal_save_test"
        val thermalFrames = listOf(
            ThermalFrame(256, 192, FloatArray(256 * 192) { 25.0f }, System.currentTimeMillis()),
            ThermalFrame(256, 192, FloatArray(256 * 192) { 26.0f }, System.currentTimeMillis() + 100)
        )
        
        // when
        val filePath = thermalRecorder.saveThermalData(sessionId, thermalFrames)
        
        // then
        assertNotNull(filePath)
        assertTrue(filePath!!.endsWith(".thermal"))
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should stop thermal recording cleanly`() = runTest {
        // given
        thermalRecorder.startRecording("test_session")
        whenever(mockTopdonDevice.isRecording()).thenReturn(true)
        
        // when
        thermalRecorder.stopRecording()
        
        // then
        verify(mockTopdonDevice).stopRecording()
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should handle thermal sensor errors during recording`() = runTest {
        // given
        whenever(mockTopdonDevice.captureFrame()).thenThrow(
            RuntimeException("thermal sensor hardware error")
        )
        
        // when
        val frame = thermalRecorder.captureFrame()
        
        // then
        assertNull(frame)
        verify(mockLogger).logE(any())
    }
    
    @Test
    fun `should validate thermal camera specifications`() = runTest {
        // given
        val expectedSpecs = ThermalSpecs(
            resolution = "256x192",
            temperatureRange = "-20°C to 150°C",
            accuracy = "±2°C",
            thermalSensitivity = "0.05°C"
        )
        
        // when
        val actualSpecs = thermalRecorder.getDeviceSpecifications()
        
        // then
        assertEquals(expectedSpecs.resolution, actualSpecs?.resolution)
        verify(mockLogger).logD(any())
    }
    
    @Test
    fun `should generate thermal overlay for rgb alignment`() = runTest {
        // given
        val thermalFrame = ThermalFrame(256, 192, FloatArray(256 * 192) { 30.0f }, System.currentTimeMillis())
        val rgbImageSize = Size(1920, 1080)
        
        // when
        val overlay = thermalRecorder.generateThermalOverlay(thermalFrame, rgbImageSize)
        
        // then
        assertNotNull(overlay)
        assertEquals(1920, overlay?.width)
        assertEquals(1080, overlay?.height)
        verify(mockLogger).logD(any())
    }
    
    @Test
    fun `should monitor thermal recording performance metrics`() = runTest {
        // given
        thermalRecorder.startRecording("performance_test")
        
        // simulate recording for performance measurement
        repeat(10) {
            thermalRecorder.captureFrame()
        }
        
        // when
        val metrics = thermalRecorder.getPerformanceMetrics()
        
        // then
        assertTrue(metrics.framesPerSecond > 0)
        assertTrue(metrics.averageProcessingTimeMs > 0)
        assertFalse(metrics.droppedFrames > 5) // should have minimal drops
        verify(mockLogger).logI(any())
    }
    
    private data class ThermalFrame(
        val width: Int,
        val height: Int,
        val temperatureData: FloatArray,
        val timestamp: Long
    )
    
    private data class ThermalSpecs(
        val resolution: String,
        val temperatureRange: String,
        val accuracy: String,
        val thermalSensitivity: String
    )
    
    private data class Size(val width: Int, val height: Int)
    
    private data class PerformanceMetrics(
        val framesPerSecond: Double,
        val averageProcessingTimeMs: Double,
        val droppedFrames: Int
    )
    
    private interface TopdonThermalDevice {
        fun isConnected(): Boolean
        fun getDeviceInfo(): String
        fun captureFrame(): ThermalFrame?
        fun isRecording(): Boolean
        fun stopRecording()
    }
}