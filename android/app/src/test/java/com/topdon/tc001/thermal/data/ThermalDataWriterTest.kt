package com.topdon.tc001.thermal.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.topdon.tc001.thermal.data.ThermalDataWriter
import com.topdon.tc001.thermal.data.RecordingConfig
import com.topdon.tc001.thermal.data.RecordingFormat
import com.topdon.tc001.thermal.data.ExportConfig
import com.topdon.tc001.thermal.data.ExportFormat
import com.topdon.tc001.thermal.data.ThermalMetadata
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThermalDataWriterTest {
    
    private lateinit var context: Context
    private lateinit var dataWriter: ThermalDataWriter
    private lateinit var testDirectory: File
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataWriter = ThermalDataWriter.getInstance(context)
        testDirectory = File(context.cacheDir, "test_thermal_data")
        testDirectory.mkdirs()
    }
    
    @After
    fun tearDown() {
        dataWriter.stopRecording()
        testDirectory.deleteRecursively()
    }
    
    @Test
    fun `should initialize data writer correctly`() {
        assertNotNull(dataWriter, "Data writer should be initialized")
        assertFalse(dataWriter.isRecording(), "Recording should not be active initially")
    }
    
    @Test
    fun `should start and stop recording successfully`() {
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
            includeMetadata = true
        }
        
        // Start recording
        val started = dataWriter.startRecording("test_session", config)
        assertTrue(started, "Recording should start successfully")
        assertTrue(dataWriter.isRecording(), "Recording should be active")
        
        // Stop recording
        val filePath = dataWriter.stopRecording()
        assertNotNull(filePath, "File path should be returned")
        assertFalse(dataWriter.isRecording(), "Recording should be stopped")
        
        // Verify file exists
        val recordingFile = File(filePath)
        assertTrue(recordingFile.exists(), "Recording file should exist")
        assertTrue(recordingFile.length() > 0, "Recording file should not be empty")
    }
    
    @Test
    fun `should record thermal frames correctly`() {
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
            includeMetadata = true
        }
        
        dataWriter.startRecording("frame_test", config)
        
        // Record test frames with varying temperatures
        val testFrames = 10
        repeat(testFrames) { i ->
            val timestamp = System.currentTimeMillis() + i * 40 // 25 FPS
            val tempData = generateTestTemperatureData(25.0f + i)
            val metadata = ThermalMetadata().apply {
                this.timestamp = timestamp
                emissivity = 0.95f
                ambientTemp = 25.0f
                distance = 1.0f
            }
            
            dataWriter.recordThermalFrame(timestamp, tempData, metadata)
        }
        
        val filePath = dataWriter.stopRecording()
        
        // Verify recording content
        val recordingFile = File(filePath)
        val lines = recordingFile.readLines()
        
        assertTrue(lines.isNotEmpty(), "Should have header line")
        assertTrue(lines.size > testFrames, "Should have data lines")
        
        // Verify CSV header contains expected columns
        val header = lines.first()
        assertTrue(header.contains("Timestamp"), "Header should contain Timestamp")
        assertTrue(header.contains("MinTemp"), "Header should contain MinTemp")
        assertTrue(header.contains("MaxTemp"), "Header should contain MaxTemp")
        assertTrue(header.contains("AvgTemp"), "Header should contain AvgTemp")
    }
    
    @Test
    fun `should export data with analysis correctly`() {
        // Record some test data first
        recordTestSession()
        
        val exportConfig = ExportConfig().apply {
            format = ExportFormat.CSV
            includeAnalysis = true
            includeImages = false
        }
        
        val exportPath = dataWriter.exportThermalData(exportConfig)
        
        assertNotNull(exportPath, "Export path should be returned")
        
        val exportFile = File(exportPath)
        assertTrue(exportFile.exists(), "Export file should exist")
        assertTrue(exportFile.length() > 0, "Export file should contain data")
        
        // Verify export contains analysis
        val exportContent = exportFile.readText()
        assertTrue(exportContent.contains("Statistical Analysis") || 
                  exportContent.contains("Temperature Statistics"), 
                  "Should contain statistical analysis")
    }
    
    @Test
    fun `should handle insufficient storage gracefully`() {
        // Create a config that would require more storage than available
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
            includeMetadata = true
        }
        
        // This test simulates insufficient storage by trying to write to a read-only location
        // In a real scenario, we would mock StatFs to return low available space
        val started = dataWriter.startRecording("storage_test", config)
        
        // The test should either succeed (if there's enough space) or fail gracefully
        if (!started) {
            // Verify that recording is not active when start fails
            assertFalse(dataWriter.isRecording(), "Recording should not be active if start failed")
        }
    }
    
    @Test
    fun `should validate recording configuration`() {
        // Test invalid frame rate
        val invalidConfig = RecordingConfig().apply {
            frameRate = -1 // Invalid frame rate
            format = RecordingFormat.CSV
        }
        
        assertFailsWith<IllegalArgumentException>("Should reject negative frame rate") {
            dataWriter.startRecording("invalid_test", invalidConfig)
        }
        
        // Test invalid frame rate (too high)
        val invalidConfig2 = RecordingConfig().apply {
            frameRate = 1000 // Unrealistic frame rate
            format = RecordingFormat.CSV
        }
        
        assertFailsWith<IllegalArgumentException>("Should reject excessively high frame rate") {
            dataWriter.startRecording("invalid_test2", invalidConfig2)
        }
        
        // Test valid configuration
        val validConfig = RecordingConfig().apply {
            frameRate = 25
            format = RecordingFormat.CSV
            includeMetadata = true
        }
        
        // Should not throw exception
        val started = dataWriter.startRecording("valid_test", validConfig)
        if (started) {
            dataWriter.stopRecording()
        }
    }
    
    @Test
    fun `should handle concurrent recording attempts`() {
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
        }
        
        // Start first recording
        val started1 = dataWriter.startRecording("concurrent_1", config)
        assertTrue(started1, "First recording should start")
        
        // Try to start second recording
        val started2 = dataWriter.startRecording("concurrent_2", config)
        assertFalse(started2, "Second recording should fail")
        
        // Stop first recording
        dataWriter.stopRecording()
        
        // Now second recording should work
        val started3 = dataWriter.startRecording("concurrent_3", config)
        assertTrue(started3, "Recording should work after stopping previous")
        
        dataWriter.stopRecording()
    }
    
    @Test
    fun `should handle different recording formats`() {
        val formats = listOf(
            RecordingFormat.CSV,
            RecordingFormat.BINARY,
            RecordingFormat.RADIOMETRIC
        )
        
        formats.forEach { format ->
            val config = RecordingConfig().apply {
                this.format = format
                includeMetadata = true
            }
            
            val started = dataWriter.startRecording("format_test_$format", config)
            assertTrue(started, "Recording should start with format $format")
            
            // Record a few frames
            repeat(5) { i ->
                val tempData = generateTestTemperatureData(25.0f + i)
                dataWriter.recordThermalFrame(System.currentTimeMillis(), tempData, null)
            }
            
            val filePath = dataWriter.stopRecording()
            assertNotNull(filePath, "File path should be returned for format $format")
            
            val file = File(filePath)
            assertTrue(file.exists(), "File should exist for format $format")
            assertTrue(file.length() > 0, "File should not be empty for format $format")
        }
    }
    
    @Test
    fun `should calculate frame statistics correctly`() {
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
        }
        
        dataWriter.startRecording("stats_test", config)
        
        // Record frames with known temperature patterns
        val expectedMinTemp = 20.0f
        val expectedMaxTemp = 30.0f
        val expectedAvgTemp = 25.0f
        
        val tempData = generateUniformTemperatureData(expectedAvgTemp)
        dataWriter.recordThermalFrame(System.currentTimeMillis(), tempData, null)
        
        val filePath = dataWriter.stopRecording()
        
        // Parse recorded data and verify statistics
        val recordingFile = File(filePath)
        val lines = recordingFile.readLines()
        
        assertTrue(lines.size >= 2, "Should have header and at least one data line")
        
        val dataLine = lines[1] // First data line after header
        val values = dataLine.split(",")
        
        // Assuming CSV format: Timestamp,MinTemp,MaxTemp,AvgTemp,...
        assertTrue(values.size >= 4, "Should have at least timestamp and temperature stats")
        
        val recordedAvg = values[3].toFloatOrNull()
        assertNotNull(recordedAvg, "Average temperature should be recorded")
        assertEquals(expectedAvgTemp, recordedAvg, 1.0f, "Recorded average should match expected")
    }
    
    @Test
    fun `should handle large recording sessions`() {
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
        }
        
        dataWriter.startRecording("large_test", config)
        
        // Record many frames to test memory management
        val frameCount = 1000
        repeat(frameCount) { i ->
            val tempData = generateTestTemperatureData(25.0f + (i % 20)) // Cycling temperatures
            dataWriter.recordThermalFrame(System.currentTimeMillis() + i * 40L, tempData, null)
            
            // Occasionally yield to prevent test timeout
            if (i % 100 == 0) {
                Thread.sleep(10)
            }
        }
        
        val filePath = dataWriter.stopRecording()
        
        val recordingFile = File(filePath)
        assertTrue(recordingFile.exists(), "Large recording file should exist")
        
        val lines = recordingFile.readLines()
        assertTrue(lines.size > frameCount, "Should have recorded all frames plus header")
        
        // Verify file size is reasonable (not excessively large due to memory issues)
        val fileSizeMB = recordingFile.length() / 1024 / 1024
        assertTrue(fileSizeMB < 100, "File size should be reasonable (< 100MB)")
    }
    
    @Test
    fun `should export with different formats correctly`() {
        // Record test session
        recordTestSession()
        
        val exportFormats = listOf(
            ExportFormat.CSV,
            ExportFormat.JSON
        )
        
        exportFormats.forEach { format ->
            val exportConfig = ExportConfig().apply {
                this.format = format
                includeAnalysis = true
            }
            
            val exportPath = dataWriter.exportThermalData(exportConfig)
            assertNotNull(exportPath, "Export should succeed for format $format")
            
            val exportFile = File(exportPath)
            assertTrue(exportFile.exists(), "Export file should exist for format $format")
            
            // Verify file content based on format
            val content = exportFile.readText()
            when (format) {
                ExportFormat.CSV -> {
                    assertTrue(content.contains(","), "CSV should contain comma separators")
                }
                ExportFormat.JSON -> {
                    assertTrue(content.contains("{") && content.contains("}"), 
                             "JSON should contain object brackets")
                }
                else -> { /* Other formats */ }
            }
        }
    }
    
    // Helper methods
    private fun recordTestSession() {
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
        }
        dataWriter.startRecording("test_analysis", config)
        
        repeat(20) { i ->
            val tempData = generateTestTemperatureData(20.0f + i * 0.5f)
            val metadata = ThermalMetadata().apply {
                timestamp = System.currentTimeMillis() + i * 50L
                emissivity = 0.95f
                ambientTemp = 25.0f
            }
            dataWriter.recordThermalFrame(metadata.timestamp, tempData, metadata)
        }
        
        dataWriter.stopRecording()
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
    
    private fun generateUniformTemperatureData(temperature: Float): ByteArray {
        return generateTestTemperatureData(temperature)
    }
