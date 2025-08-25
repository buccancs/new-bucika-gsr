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

        gsrManager.cleanup()
        dataWriter.cleanup()
        
        clearSingletonInstances()
    }

    @Test
    fun testCompleteGSRWorkflow() {

        val latch = CountDownLatch(1)
        var dataReceived = false
        var fileWritten = false

        sensorPanel.setSamplingRate(128)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.GSR, true)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.TEMPERATURE, true)

        val config = sensorPanel.generateConfiguration()
        gsrManager.applyConfiguration(config)

        gsrManager.setGSRDataListener(object : GSRManager.GSRDataListener {
            override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
                dataReceived = true
                
                val filename = "integration_test_${System.currentTimeMillis()}.csv"
                val dataPoint = createGSRDataPoint(timestamp, gsrValue, skinTemperature)
                fileWritten = dataWriter.writeGSRDataPoint(filename, dataPoint)
                
                if (dataReceived && fileWritten) {
                    latch.countDown()
                }
            }
        })

        val connectionResult = gsrManager.connectToDevice("00:11:22:33:44:55", "TestShimmer")
        val recordingResult = gsrManager.startRecording()

        val completed = latch.await(10, TimeUnit.SECONDS)

        Assert.assertTrue("Should connect to device", connectionResult)
        Assert.assertTrue("Should start recording", recordingResult)
        Assert.assertTrue("Should receive and write data", completed)
        Assert.assertTrue("Should receive GSR data", dataReceived)
        Assert.assertTrue("Should write data to file", fileWritten)
    }

    @Test
    fun testSensorPanelConfigurationIntegration() {

        sensorPanel.setSamplingRate(256)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.GSR, true)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.TEMPERATURE, true)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.PPG, true)
        
        val config = sensorPanel.generateConfiguration()
        val applyResult = gsrManager.applyConfiguration(config)
        
        Assert.assertTrue("Configuration should be applied successfully", applyResult)
        
        val currentConfig = gsrManager.getCurrentConfiguration()
        Assert.assertEquals("Sampling rate should match", 256.0, currentConfig.getSamplingRateShimmer(), 0.1)
        Assert.assertTrue("GSR should be enabled", currentConfig.isSensorEnabled(0x04))
        Assert.assertTrue("Temperature should be enabled", currentConfig.isSensorEnabled(0x80))
    }

    @Test
    fun testAdvancedDataProcessingIntegration() {

        val latch = CountDownLatch(1)
        var advancedDataReceived = false

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
                
                Assert.assertTrue("Arousal state should be valid", 
                                arousalState in listOf("LOW", "MEDIUM", "HIGH"))
                Assert.assertTrue("Should have stress indicators", stressIndicators.isNotEmpty())
                Assert.assertTrue("Signal quality should be normalized", 
                                signalQuality >= 0.0 && signalQuality <= 1.0)
                
                latch.countDown()
            }
        })

        gsrManager.connectToDevice("00:11:22:33:44:55", "AdvancedTestShimmer")
        gsrManager.startRecording()

        val completed = latch.await(10, TimeUnit.SECONDS)
        Assert.assertTrue("Should receive advanced data", completed && advancedDataReceived)
    }

    @Test
    fun testRealTimeDataWritingIntegration() {

        val sessionName = "realtime_integration_test"
        val filename = dataWriter.createCSVFile(sessionName, System.currentTimeMillis())
        
        dataWriter.startRealTimeRecording(filename)
        
        gsrManager.setGSRDataListener(object : GSRManager.GSRDataListener {
            override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
                val dataPoint = createGSRDataPoint(timestamp, gsrValue, skinTemperature)
                dataWriter.queueDataPoint(dataPoint)
            }
        })

        gsrManager.connectToDevice("00:11:22:33:44:55", "RealTimeTestShimmer")
        gsrManager.startRecording()

        Thread.sleep(3000)

        gsrManager.stopRecording()
        dataWriter.stopRealTimeRecording()

        val dataFile = java.io.File(dataWriter.getDataDirectory(), filename)
        Assert.assertTrue("Data file should exist", dataFile.exists())
        
        val lines = dataFile.readLines()
        Assert.assertTrue("Should have header plus data lines", lines.size > 10)
    }

    @Test
    fun testSettingsAPIConnectivity() {

        sensorPanel.setSamplingRate(512)
        sensorPanel.setGSRRange(ShimmerSensorPanel.GSRRange.RANGE_1M_OHM)
        sensorPanel.setFilterEnabled(true)
        
        sensorPanel.saveSensorSettings()
        
        val newSensorPanel = ShimmerSensorPanel(context)
        newSensorPanel.loadSavedSettings()
        
        val newConfig = newSensorPanel.generateConfiguration()
        Assert.assertEquals("Sampling rate should be persisted", 512.0, newConfig.getSamplingRateShimmer(), 0.1)
    }

    @Test
    fun testDataExportIntegration() {

        val sessionData = createTestSessionData()
        
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

        val connectionResult = gsrManager.connectToDevice("invalid_address", "ErrorTestDevice")
        Assert.assertFalse("Invalid connection should fail", connectionResult)
        
        Assert.assertFalse("Should not be connected after failed attempt", gsrManager.isConnected())
        
        val recoveryResult = gsrManager.connectToDevice("00:11:22:33:44:55", "RecoveryTestShimmer")
        Assert.assertTrue("Should recover and connect successfully", recoveryResult)
    }

    @Test
    fun testMemoryManagementIntegration() {

        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        repeat(1000) { i ->
            val dataPoint = createGSRDataPoint(
                timestamp = System.currentTimeMillis() + i,
                gsrValue = 400.0 + i * 0.5,
                skinTemperature = 32.0 + Math.sin(i * 0.1)
            )
            dataWriter.queueDataPoint(dataPoint)
            
            if (i % 100 == 0) {

                System.gc()
            }
        }
        
        Thread.sleep(2000)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        Assert.assertTrue("Memory usage should be reasonable", memoryIncrease < 50 * 1024 * 1024)
    }

    @Test
    fun testConcurrentOperationsIntegration() {

        val latch = CountDownLatch(3)
        
        Thread {
            gsrManager.connectToDevice("00:11:22:33:44:55", "ConcurrentTest1")
            gsrManager.startRecording()
            Thread.sleep(2000)
            gsrManager.stopRecording()
            latch.countDown()
        }.start()
        
        Thread {
            repeat(10) { i ->
                sensorPanel.setSamplingRate(if (i % 2 == 0) 128 else 256)
                Thread.sleep(100)
            }
            latch.countDown()
        }.start()
        
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

            val gsrManagerField = GSRManager::class.java.getDeclaredField("INSTANCE")
            gsrManagerField.isAccessible = true
            gsrManagerField.set(null, null)
            
            val dataWriterField = GSRDataWriter::class.java.getDeclaredField("INSTANCE")
            dataWriterField.isAccessible = true
            dataWriterField.set(null, null)
        } catch (e: Exception) {

        }
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
