package com.topdon.tc001.gsr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.topdon.tc001.gsr.data.GSRDataWriter
import com.topdon.tc001.gsr.ui.ShimmerSensorPanel
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for comprehensive GSR system functionality
 * Tests end-to-end workflows, component integration, and real-world scenarios
 */
@RunWith(AndroidJUnit4::class)
class GSRIntegrationTest {

    @get:Rule
    val activityTestRule = ActivityTestRule(GSRActivity::class.java)

    private lateinit var context: Context
    private lateinit var gsrManager: GSRManager
    private lateinit var dataWriter: GSRDataWriter
    private lateinit var sensorPanel: ShimmerSensorPanel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        gsrManager = GSRManager.getInstance(context)
        dataWriter = GSRDataWriter.getInstance(context)
        sensorPanel = ShimmerSensorPanel(context)
    }

    @After
    fun tearDown() {
        // Cleanup after tests
        gsrManager.cleanup()
        dataWriter.cleanup()
        
        // Clear singleton instances
        clearSingletonInstances()
    }

    @Test
    fun testCompleteGSRWorkflow() {
        // Test complete GSR data collection workflow
        val latch = CountDownLatch(1)
        var dataReceived = false
        var fileWritten = false

        // Step 1: Configure sensor panel
        sensorPanel.setSamplingRate(128)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.GSR, true)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.TEMPERATURE, true)

        // Step 2: Apply configuration to GSR manager
        val config = sensorPanel.generateConfiguration()
        gsrManager.applyConfiguration(config)

        // Step 3: Setup data listener with file writing
        gsrManager.setGSRDataListener(object : GSRManager.GSRDataListener {
            override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
                dataReceived = true
                
                // Write data to file
                val filename = "integration_test_${System.currentTimeMillis()}.csv"
                val dataPoint = createGSRDataPoint(timestamp, gsrValue, skinTemperature)
                fileWritten = dataWriter.writeGSRDataPoint(filename, dataPoint)
                
                if (dataReceived && fileWritten) {
                    latch.countDown()
                }
            }
        })

        // Step 4: Start data collection
        val connectionResult = gsrManager.connectToDevice("00:11:22:33:44:55", "TestShimmer")
        val recordingResult = gsrManager.startRecording()

        // Wait for data reception
        val completed = latch.await(10, TimeUnit.SECONDS)

        Assert.assertTrue("Should connect to device", connectionResult)
        Assert.assertTrue("Should start recording", recordingResult)
        Assert.assertTrue("Should receive and write data", completed)
        Assert.assertTrue("Should receive GSR data", dataReceived)
        Assert.assertTrue("Should write data to file", fileWritten)
    }

    @Test
    fun testSensorPanelConfigurationIntegration() {
        // Test sensor panel configuration integration with GSR manager
        
        // Configure multiple sensors
        sensorPanel.setSamplingRate(256)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.GSR, true)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.TEMPERATURE, true)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.PPG, true)
        
        // Generate and apply configuration
        val config = sensorPanel.generateConfiguration()
        val applyResult = gsrManager.applyConfiguration(config)
        
        Assert.assertTrue("Configuration should be applied successfully", applyResult)
        
        // Verify configuration was applied
        val currentConfig = gsrManager.getCurrentConfiguration()
        Assert.assertEquals("Sampling rate should match", 256.0, currentConfig.getSamplingRateShimmer(), 0.1)
        Assert.assertTrue("GSR should be enabled", currentConfig.isSensorEnabled(0x04))
        Assert.assertTrue("Temperature should be enabled", currentConfig.isSensorEnabled(0x80))
    }

    @Test
    fun testAdvancedDataProcessingIntegration() {
        // Test advanced data processing with complete pipeline
        val latch = CountDownLatch(1)
        var advancedDataReceived = false

        // Setup advanced data listener
        gsrManager.setAdvancedDataListener(object : GSRManager.AdvancedGSRDataListener {
            override fun onAdvancedGSRData(
                timestamp: Long,
                gsrValue: Double,
                skinTemperature: Double,
                arousalState: String,
                stressIndicators: Map<String, Double>,
                signalQuality: Double
            ) {
                advancedDataReceived = true
                
                // Verify advanced processing results
                Assert.assertTrue("Arousal state should be valid", 
                                arousalState in listOf("LOW", "MEDIUM", "HIGH"))
                Assert.assertTrue("Should have stress indicators", stressIndicators.isNotEmpty())
                Assert.assertTrue("Signal quality should be normalized", 
                                signalQuality >= 0.0 && signalQuality <= 1.0)
                
                latch.countDown()
            }
        })

        // Start data collection
        gsrManager.connectToDevice("00:11:22:33:44:55", "AdvancedTestShimmer")
        gsrManager.startRecording()

        val completed = latch.await(10, TimeUnit.SECONDS)
        Assert.assertTrue("Should receive advanced data", completed && advancedDataReceived)
    }

    @Test
    fun testRealTimeDataWritingIntegration() {
        // Test real-time data writing with high frequency
        val sessionName = "realtime_integration_test"
        val filename = dataWriter.createCSVFile(sessionName, System.currentTimeMillis())
        
        // Start real-time recording
        dataWriter.startRealTimeRecording(filename)
        
        // Setup GSR manager with data forwarding to writer
        gsrManager.setGSRDataListener(object : GSRManager.GSRDataListener {
            override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
                val dataPoint = createGSRDataPoint(timestamp, gsrValue, skinTemperature)
                dataWriter.queueDataPoint(dataPoint)
            }
        })

        // Simulate high-frequency data collection
        gsrManager.connectToDevice("00:11:22:33:44:55", "RealTimeTestShimmer")
        gsrManager.startRecording()

        // Allow data collection for a few seconds
        Thread.sleep(3000)

        // Stop recording and real-time writing
        gsrManager.stopRecording()
        dataWriter.stopRealTimeRecording()

        // Verify file was created and contains data
        val dataFile = java.io.File(dataWriter.getDataDirectory(), filename)
        Assert.assertTrue("Data file should exist", dataFile.exists())
        
        val lines = dataFile.readLines()
        Assert.assertTrue("Should have header plus data lines", lines.size > 10)
    }

    @Test
    fun testSettingsAPIConnectivity() {
        // Test Settings API connectivity between components
        
        // Configure settings through sensor panel
        sensorPanel.setSamplingRate(512)
        sensorPanel.setGSRRange(ShimmerSensorPanel.GSRRange.RANGE_1M_OHM)
        sensorPanel.setFilterEnabled(true)
        
        // Save settings to SharedPreferences
        sensorPanel.saveSensorSettings()
        
        // Create new sensor panel instance and verify settings persistence
        val newSensorPanel = ShimmerSensorPanel(context)
        newSensorPanel.loadSavedSettings()
        
        val newConfig = newSensorPanel.generateConfiguration()
        Assert.assertEquals("Sampling rate should be persisted", 512.0, newConfig.getSamplingRateShimmer(), 0.1)
    }

    @Test
    fun testDataExportIntegration() {
        // Test comprehensive data export integration
        val sessionData = createTestSessionData()
        
        // Export data with analysis
        val exportPath = dataWriter.exportGSRDataToFile(sessionData, includeAnalysis = true)
        
        Assert.assertNotNull("Export path should be returned", exportPath)
        
        val exportFile = java.io.File(exportPath)
        Assert.assertTrue("Export file should exist", exportFile.exists())
        
        val content = exportFile.readText()
        Assert.assertTrue("Should include session metadata", content.contains("SESSION_INFO"))
        Assert.assertTrue("Should include analysis results", content.contains("ANALYSIS_SUMMARY"))
        Assert.assertTrue("Should include statistical measures", content.contains("STATISTICS"))
    }

    @Test
    fun testErrorRecoveryIntegration() {
        // Test error recovery across components
        
        // Simulate connection error
        val connectionResult = gsrManager.connectToDevice("invalid_address", "ErrorTestDevice")
        Assert.assertFalse("Invalid connection should fail", connectionResult)
        
        // Verify system recovers gracefully
        Assert.assertFalse("Should not be connected after failed attempt", gsrManager.isConnected())
        
        // Test successful connection after error
        val recoveryResult = gsrManager.connectToDevice("00:11:22:33:44:55", "RecoveryTestShimmer")
        Assert.assertTrue("Should recover and connect successfully", recoveryResult)
    }

    @Test
    fun testMemoryManagementIntegration() {
        // Test memory management across long-running operations
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Simulate extended data collection
        repeat(1000) { i ->
            val dataPoint = createGSRDataPoint(
                timestamp = System.currentTimeMillis() + i,
                gsrValue = 400.0 + i * 0.5,
                skinTemperature = 32.0 + Math.sin(i * 0.1)
            )
            dataWriter.queueDataPoint(dataPoint)
            
            if (i % 100 == 0) {
                // Trigger garbage collection periodically
                System.gc()
            }
        }
        
        // Allow processing to complete
        Thread.sleep(2000)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable (less than 50MB)
        Assert.assertTrue("Memory usage should be reasonable", memoryIncrease < 50 * 1024 * 1024)
    }

    @Test
    fun testConcurrentOperationsIntegration() {
        // Test concurrent operations across components
        val latch = CountDownLatch(3) // Three concurrent operations
        
        // Operation 1: Data collection
        Thread {
            gsrManager.connectToDevice("00:11:22:33:44:55", "ConcurrentTest1")
            gsrManager.startRecording()
            Thread.sleep(2000)
            gsrManager.stopRecording()
            latch.countDown()
        }.start()
        
        // Operation 2: Configuration changes
        Thread {
            repeat(10) { i ->
                sensorPanel.setSamplingRate(if (i % 2 == 0) 128 else 256)
                Thread.sleep(100)
            }
            latch.countDown()
        }.start()
        
        // Operation 3: File writing
        Thread {
            val filename = dataWriter.createCSVFile("concurrent_test", System.currentTimeMillis())
            repeat(50) { i ->
                val dataPoint = createGSRDataPoint(System.currentTimeMillis() + i, 400.0 + i, 32.0)
                dataWriter.writeGSRDataPoint(filename, dataPoint)
                Thread.sleep(50)
            }
            latch.countDown()
        }.start()
        
        val completed = latch.await(30, TimeUnit.SECONDS)
        Assert.assertTrue("All concurrent operations should complete", completed)
    }

    // Helper methods
    private fun createGSRDataPoint(timestamp: Long, gsrValue: Double, skinTemperature: Double): GSRDataPoint {
        return GSRDataPoint(
            timestamp = timestamp,
            gsrValue = gsrValue,
            skinTemperature = skinTemperature,
            signalQuality = 0.85,
            batteryLevel = 80
        )
    }

    private fun createTestSessionData(): SessionData {
        return SessionData(
            sessionName = "integration_test_session",
            startTime = System.currentTimeMillis() - 300000,
            endTime = System.currentTimeMillis(),
            dataPoints = (1..300).map { i ->
                createGSRDataPoint(
                    timestamp = System.currentTimeMillis() + i * 10,
                    gsrValue = 400.0 + i * 1.5,
                    skinTemperature = 32.0 + Math.sin(i * 0.1)
                )
            },
            participantId = "INTEGRATION_TEST_001",
            notes = "Integration test session"
        )
    }

    private fun clearSingletonInstances() {
        try {
            // Clear GSRManager singleton
            val gsrManagerField = GSRManager::class.java.getDeclaredField("INSTANCE")
            gsrManagerField.isAccessible = true
            gsrManagerField.set(null, null)
            
            // Clear GSRDataWriter singleton
            val dataWriterField = GSRDataWriter::class.java.getDeclaredField("INSTANCE")
            dataWriterField.isAccessible = true
            dataWriterField.set(null, null)
        } catch (e: Exception) {
            // Handle reflection exceptions gracefully
        }
    }

    // Test data classes
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
