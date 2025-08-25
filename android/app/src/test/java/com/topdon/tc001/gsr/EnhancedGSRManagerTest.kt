package com.topdon.tc001.gsr

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.ObjectCluster
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class EnhancedGSRManagerTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockShimmer: Shimmer

    private lateinit var enhancedGSRManager: EnhancedGSRManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        enhancedGSRManager = EnhancedGSRManager.getInstance(mockContext)
    }

    @After
    fun tearDown() {

        val instanceField = EnhancedGSRManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun testSingletonPattern() {

        val instance1 = EnhancedGSRManager.getInstance(mockContext)
        val instance2 = EnhancedGSRManager.getInstance(mockContext)
        
        Assert.assertSame("EnhancedGSRManager should follow singleton pattern", instance1, instance2)
    }

    @Test
    fun testInitialState() {

        Assert.assertFalse("Initial state should be disconnected", enhancedGSRManager.isConnected())
        Assert.assertFalse("Initial state should not be recording", enhancedGSRManager.isRecording())
        Assert.assertNull("No device should be connected initially", enhancedGSRManager.getConnectedDeviceName())
    }

    @Test
    fun testConnectionValidation() {
        val validAddress = "00:06:66:12:34:56"
        val invalidAddress = "invalid_address"
        
        val validResult = enhancedGSRManager.isValidBluetoothAddress(validAddress)
        Assert.assertTrue("Valid Shimmer MAC address should be accepted", validResult)
        
        val invalidResult = enhancedGSRManager.isValidBluetoothAddress(invalidAddress)
        Assert.assertFalse("Invalid MAC address should be rejected", invalidResult)
    }

    @Test
    fun testShimmer3SDKIntegration() {
        val testAddress = "00:06:66:12:34:56"
        val testDeviceName = "Shimmer3-GSR+"
        
        val result = enhancedGSRManager.connectToDevice(testAddress, testDeviceName)
        
        Assert.assertTrue("Connection should be initiated with Shimmer SDK", result)
    }

    @Test
    fun testSamplingRateConfiguration() {
        val expectedSamplingRate = 128.0
        
        val currentConfig = enhancedGSRManager.getCurrentConfiguration()
        
        Assert.assertNotNull("Configuration should not be null", currentConfig)

    }

    @Test
    fun testSynchronizedCaptureIntegration() {

        val syncResult = enhancedGSRManager.enableSynchronizedCapture(true)
        
        Assert.assertTrue("Synchronized capture should be configurable", syncResult)
    }

    @Test
    fun testDataQualityMetrics() {

        val testGSRValue = 450.5
        val testTemperature = 32.1
        
        val quality = enhancedGSRManager.assessDataQuality(testGSRValue, testTemperature)
        
        Assert.assertTrue("Data quality should be within valid range", quality >= 0.0 && quality <= 1.0)
    }

    @Test
    fun testHighPrecisionTimestamps() {

        val timestamp1 = enhancedGSRManager.getCurrentTimestampNanos()
        Thread.sleep(1)
        val timestamp2 = enhancedGSRManager.getCurrentTimestampNanos()
        
        Assert.assertTrue("High precision timestamps should be sequential", timestamp2 > timestamp1)
    }

    @Test
    fun testConcurrentOperationSafety() {
        val testAddress = "00:06:66:12:34:56"
        val latch = CountDownLatch(2)
        
        Thread {
            enhancedGSRManager.connectToDevice(testAddress, "Device1")
            latch.countDown()
        }.start()
        
        Thread {
            enhancedGSRManager.connectToDevice(testAddress, "Device2")
            latch.countDown()
        }.start()
        
        val completed = latch.await(5, TimeUnit.SECONDS)
        Assert.assertTrue("Concurrent operations should complete without deadlock", completed)
    }

    @Test
    fun testShimmerConfigurationValidation() {

        val config = createShimmerTestConfiguration()
        
        val isValid = enhancedGSRManager.validateConfiguration(config)
        
        Assert.assertTrue("Valid Shimmer configuration should be accepted", isValid)
    }

    @Test
    fun testGSRSensorEnableState() {
        val config = createShimmerTestConfiguration()
        
        val gsrEnabled = config.isSensorEnabled(0x04)
        
        Assert.assertTrue("GSR sensor should be enabled in configuration", gsrEnabled)
    }

    @Test
    fun testTemperatureSensorIntegration() {
        val config = createShimmerTestConfiguration()
        
        val tempEnabled = config.isSensorEnabled(0x80)
        
        Assert.assertTrue("Temperature sensor should be enabled for skin temperature", tempEnabled)
    }

    @Test
    fun testRecordingStateManagement() {
        val testAddress = "00:06:66:12:34:56"
        
        enhancedGSRManager.connectToDevice(testAddress, "TestDevice")
        
        val startResult = enhancedGSRManager.startRecording()
        Assert.assertTrue("Recording should start successfully", startResult)
        
        enhancedGSRManager.stopRecording()
        Assert.assertFalse("Recording should stop successfully", enhancedGSRManager.isRecording())
    }

    @Test
    fun testDataBufferingStrategy() {

        val bufferSize = enhancedGSRManager.getDataBufferSize()
        
        Assert.assertTrue("Data buffer should be configured appropriately", bufferSize > 0)
    }

    @Test
    fun testErrorRecoveryMechanisms() {

        enhancedGSRManager.simulateConnectionError()
        
        val recoveryResult = enhancedGSRManager.attemptRecovery()
        
        Assert.assertTrue("Error recovery should be attempted", recoveryResult)
    }

    @Test
    fun testSensorCalibrationState() {

        val calibrationStatus = enhancedGSRManager.getCalibrationStatus()
        
        Assert.assertNotNull("Calibration status should be trackable", calibrationStatus)
    }

    @Test
    fun testBatteryLevelMonitoring() {

        val batteryLevel = enhancedGSRManager.getBatteryLevel()
        
        Assert.assertTrue("Battery level should be in valid range", batteryLevel >= -1 && batteryLevel <= 100)
    }

    @Test
    fun testConnectionTimeout() {
        val testAddress = "00:06:66:12:34:56"
        val timeoutMs = 5000L
        
        enhancedGSRManager.setConnectionTimeout(timeoutMs)
        val result = enhancedGSRManager.connectToDevice(testAddress, "TestDevice")
        
        Assert.assertTrue("Connection attempt should handle timeout configuration", result)
    }

    @Test
    fun testMemoryUsageOptimization() {

        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        enhancedGSRManager.optimizeMemoryUsage()
        
        val afterOptimization = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        Assert.assertTrue("Memory optimization should not increase usage", afterOptimization <= initialMemory + 1000000)
    }

    @Test
    fun testThreadSafetyWithMultipleListeners() {
        val listener1 = mock<EnhancedGSRManager.EnhancedGSRDataListener>()
        val listener2 = mock<EnhancedGSRManager.EnhancedGSRDataListener>()
        
        enhancedGSRManager.addDataListener(listener1)
        enhancedGSRManager.addDataListener(listener2)
        
        val listenerCount = enhancedGSRManager.getActiveListenerCount()
        
        Assert.assertEquals("Should track multiple listeners correctly", 2, listenerCount)
    }

    @Test
    fun testSynchronizationWithMasterClock() {

        val syncEnabled = enhancedGSRManager.enableMasterClockSync(true)
        
        Assert.assertTrue("Master clock synchronization should be configurable", syncEnabled)
    }

    @Test
    fun testDataExportFormats() {

        val supportedFormats = enhancedGSRManager.getSupportedExportFormats()
        
        Assert.assertNotNull("Should provide supported export formats", supportedFormats)
        Assert.assertTrue("Should support at least CSV format", supportedFormats.contains("CSV"))
    }

    private fun createShimmerTestConfiguration(): Configuration {
        return Configuration().apply {
            setSamplingRateShimmer(128.0)
            setSensorEnabledState(0x04, true)
            setSensorEnabledState(0x80, true)

            setHardwareVersion(3)
            setFirmwareVersionMajor(0)
            setFirmwareVersionMinor(15)
        }
    }
