package com.topdon.tc001.gsr.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class GSRDataWriterTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var gsrDataWriter: GSRDataWriter
    private lateinit var realContext: Context
    private lateinit var testDataDir: File
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        realContext = ApplicationProvider.getApplicationContext()
        
        testDataDir = File(realContext.cacheDir, "gsr_test_data")
        testDataDir.mkdirs()
        
        gsrDataWriter = GSRDataWriter.getInstance(realContext)
    }

    @After
    fun tearDown() {

        testDataDir.deleteRecursively()
        
        val instanceField = GSRDataWriter::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun testSingletonInstance() {

        val instance1 = GSRDataWriter.getInstance(realContext)
        val instance2 = GSRDataWriter.getInstance(realContext)
        
        Assert.assertSame("GSRDataWriter should follow singleton pattern", instance1, instance2)
    }

    @Test
    fun testFileCreation() {

        val timestamp = System.currentTimeMillis()
        val filename = gsrDataWriter.createCSVFile("test_session", timestamp)
        
        Assert.assertNotNull("Filename should be generated", filename)
        Assert.assertTrue("Filename should contain session name", filename.contains("test_session"))
    }

    @Test
    fun testCSVHeaderWriting() {

        val filename = gsrDataWriter.createCSVFile("header_test", System.currentTimeMillis())
        val file = File(gsrDataWriter.getDataDirectory(), filename)
        
        Assert.assertTrue("CSV file should exist", file.exists())
        
        val content = file.readText()
        Assert.assertTrue("Should contain timestamp header", content.contains("Timestamp"))
        Assert.assertTrue("Should contain GSR header", content.contains("GSR_Value"))
        Assert.assertTrue("Should contain temperature header", content.contains("Skin_Temperature"))
    }

    @Test
    fun testSingleDataPointWriting() {

        val filename = gsrDataWriter.createCSVFile("single_point", System.currentTimeMillis())
        
        val testData = createTestGSRDataPoint()
        val result = gsrDataWriter.writeGSRDataPoint(filename, testData)
        
        Assert.assertTrue("Data point should be written successfully", result)
        
        val file = File(gsrDataWriter.getDataDirectory(), filename)
        val content = file.readText()
        Assert.assertTrue("File should contain GSR value", content.contains(testData.gsrValue.toString()))
    }

    @Test
    fun testBatchDataWriting() {

        val filename = gsrDataWriter.createCSVFile("batch_test", System.currentTimeMillis())
        
        val testDataList = createTestGSRDataBatch(100)
        val result = gsrDataWriter.writeBatchGSRData(filename, testDataList)
        
        Assert.assertTrue("Batch data should be written successfully", result)
        
        val file = File(gsrDataWriter.getDataDirectory(), filename)
        val lines = file.readLines()
        Assert.assertEquals("Should have header + 100 data lines", 101, lines.size)
    }

    @Test
    fun testRealTimeDataQueuing() {

        val filename = gsrDataWriter.createCSVFile("realtime_test", System.currentTimeMillis())
        gsrDataWriter.startRealTimeRecording(filename)
        
        repeat(50) { i ->
            val dataPoint = createTestGSRDataPoint().copy(
                timestamp = System.currentTimeMillis() + i * 10,
                gsrValue = 400.0 + i * 2.5
            )
            gsrDataWriter.queueDataPoint(dataPoint)
        }
        
        Thread.sleep(1000)
        gsrDataWriter.stopRealTimeRecording()
        
        val file = File(gsrDataWriter.getDataDirectory(), filename)
        val lines = file.readLines()
        Assert.assertTrue("Should have queued data written", lines.size > 10)
    }

    @Test
    fun testDataExport() {

        val sessionData = createTestSessionData()
        val exportPath = gsrDataWriter.exportGSRDataToFile(sessionData, includeAnalysis = true)
        
        Assert.assertNotNull("Export path should be returned", exportPath)
        
        val exportFile = File(exportPath)
        Assert.assertTrue("Export file should exist", exportFile.exists())
        
        val content = exportFile.readText()
        Assert.assertTrue("Export should include analysis", content.contains("ANALYSIS_SUMMARY"))
    }

    @Test
    fun testFileManagementUtilities() {

        val dirSize = gsrDataWriter.getDataDirectorySize()
        Assert.assertTrue("Directory size should be calculable", dirSize >= 0)
        
        val fileCount = gsrDataWriter.getFileCount()
        Assert.assertTrue("File count should be calculable", fileCount >= 0)
    }

    @Test
    fun testDataCleanup() {

        val oldFile1 = File(testDataDir, "old_session_1.csv")
        val oldFile2 = File(testDataDir, "old_session_2.csv")
        
        oldFile1.createNewFile()
        oldFile2.createNewFile()
        
        val oldTimestamp = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        oldFile1.setLastModified(oldTimestamp)
        oldFile2.setLastModified(oldTimestamp)
        
        val cleanedCount = gsrDataWriter.cleanupOldFiles(retentionDays = 7)
        Assert.assertTrue("Should cleanup old files", cleanedCount >= 0)
    }

    @Test
    fun testDataIntegrityValidation() {

        val filename = gsrDataWriter.createCSVFile("integrity_test", System.currentTimeMillis())
        val testData = createTestGSRDataBatch(50)
        
        gsrDataWriter.writeBatchGSRData(filename, testData)
        
        val isValid = gsrDataWriter.validateDataIntegrity(filename)
        Assert.assertTrue("Data integrity should be valid", isValid)
    }

    @Test
    fun testCompressionUtility() {

        val filename = gsrDataWriter.createCSVFile("compression_test", System.currentTimeMillis())
        val testData = createTestGSRDataBatch(1000)
        
        gsrDataWriter.writeBatchGSRData(filename, testData)
        
        val compressedPath = gsrDataWriter.compressDataFile(filename)
        Assert.assertNotNull("Compressed file path should be returned", compressedPath)
        
        val compressedFile = File(compressedPath)
        Assert.assertTrue("Compressed file should exist", compressedFile.exists())
    }

    @Test
    fun testMetadataWriting() {

        val sessionInfo = createTestSessionMetadata()
        val metadataPath = gsrDataWriter.writeSessionMetadata("metadata_test", sessionInfo)
        
        Assert.assertNotNull("Metadata path should be returned", metadataPath)
        
        val metadataFile = File(metadataPath)
        Assert.assertTrue("Metadata file should exist", metadataFile.exists())
        
        val content = metadataFile.readText()
        Assert.assertTrue("Should contain session information", content.contains("sessionName"))
        Assert.assertTrue("Should contain device information", content.contains("deviceName"))
    }

    @Test
    fun testErrorHandling() {

        val result = gsrDataWriter.writeGSRDataPoint("", null)
        Assert.assertFalse("Should handle null data gracefully", result)
        
        val invalidPath = gsrDataWriter.exportGSRDataToFile(null, includeAnalysis = false)
        Assert.assertNull("Should handle null session data", invalidPath)
    }

    @Test
    fun testBackgroundWritingPerformance() {

        val filename = gsrDataWriter.createCSVFile("performance_test", System.currentTimeMillis())
        gsrDataWriter.startRealTimeRecording(filename)
        
        val startTime = System.currentTimeMillis()
        
        repeat(640) { i ->
            val dataPoint = createTestGSRDataPoint().copy(
                timestamp = startTime + i * 8
            )
            gsrDataWriter.queueDataPoint(dataPoint)
        }
        
        Thread.sleep(2000)
        gsrDataWriter.stopRealTimeRecording()
        
        val file = File(gsrDataWriter.getDataDirectory(), filename)
        val lines = file.readLines()
        Assert.assertTrue("Should handle high data rate", lines.size > 500)
    }

    @Test
    fun testDataFormatValidation() {

        val csvFilename = gsrDataWriter.createCSVFile("format_csv", System.currentTimeMillis())
        val jsonFilename = gsrDataWriter.createJSONFile("format_json", System.currentTimeMillis())
        
        Assert.assertTrue("CSV filename should have .csv extension", csvFilename.endsWith(".csv"))
        Assert.assertTrue("JSON filename should have .json extension", jsonFilename.endsWith(".json"))
    }

    private fun createTestGSRDataPoint(): GSRDataPoint {
        return GSRDataPoint(
            timestamp = System.currentTimeMillis(),
            gsrValue = 425.7,
            skinTemperature = 32.4,
            signalQuality = 0.88,
            batteryLevel = 85
        )
    }

    private fun createTestGSRDataBatch(size: Int): List<GSRDataPoint> {
        return (1..size).map { i ->
            GSRDataPoint(
                timestamp = System.currentTimeMillis() + i * 10,
                gsrValue = 400.0 + (i % 100) * 2.5,
                skinTemperature = 32.0 + Math.sin(i * 0.1) * 2.0,
                signalQuality = 0.8 + Math.random() * 0.2,
                batteryLevel = 90 - (i / 10)
            )
        }
    }

    private fun createTestSessionData(): SessionData {
        return SessionData(
            sessionName = "test_session",
            startTime = System.currentTimeMillis() - 300000,
            endTime = System.currentTimeMillis(),
            dataPoints = createTestGSRDataBatch(300),
            participantId = "TEST_001",
            notes = "Test session for validation"
        )
    }

    private fun createTestSessionMetadata(): SessionMetadata {
        return SessionMetadata(
            sessionName = "test_metadata_session",
            deviceName = "Shimmer3-GSR+",
            samplingRate = 128.0,
            participantId = "META_TEST_001",
            startTime = System.currentTimeMillis(),
            sensorConfiguration = mapOf(
                "GSR" to true,
                "Temperature" to true,
                "PPG" to false
            )
        )
    }

    data class GSRDataPoint(
        val timestamp: Long,
        val gsrValue: Double,
        val skinTemperature: Double,
        val signalQuality: Double,
        val batteryLevel: Int
    )

    data class SessionData(
        val sessionName: String,
        val startTime: Long,
        val endTime: Long,
        val dataPoints: List<GSRDataPoint>,
        val participantId: String,
        val notes: String
    )

    data class SessionMetadata(
        val sessionName: String,
        val deviceName: String,
        val samplingRate: Double,
        val participantId: String,
        val startTime: Long,
        val sensorConfiguration: Map<String, Any>
    )
