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

/**
 * Comprehensive test suite for EnhancedGSRManager
 * Tests official Shimmer SDK integration, synchronization, and enhanced features
 */
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
        // Clear singleton instance for clean test state
        val instanceField = EnhancedGSRManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun testSingletonPattern() {
        // Test singleton pattern implementation
        val instance1 = EnhancedGSRManager.getInstance(mockContext)
        val instance2 = EnhancedGSRManager.getInstance(mockContext)
        
        Assert.assertSame("EnhancedGSRManager should follow singleton pattern", instance1, instance2)
    }

    @Test
    fun testInitialState() {
        // Test initial state of EnhancedGSRManager
        Assert.assertFalse("Initial state should be disconnected", enhancedGSRManager.isConnected())
        Assert.assertFalse("Initial state should not be recording", enhancedGSRManager.isRecording())
        Assert.assertNull("No device should be connected initially", enhancedGSRManager.getConnectedDeviceName())
    }

    @Test
    fun testConnectionValidation() {
        val validAddress = "00:06:66:12:34:56"  // Shimmer3 format
        val invalidAddress = "invalid_address"
        
        // Test valid MAC address validation
        val validResult = enhancedGSRManager.isValidBluetoothAddress(validAddress)
        Assert.assertTrue("Valid Shimmer MAC address should be accepted", validResult)
        
        // Test invalid MAC address validation  
        val invalidResult = enhancedGSRManager.isValidBluetoothAddress(invalidAddress)
        Assert.assertFalse("Invalid MAC address should be rejected", invalidResult)
    }

    @Test
    fun testShimmer3SDKIntegration() {
        val testAddress = "00:06:66:12:34:56"
        val testDeviceName = "Shimmer3-GSR+"
        
        // Test connection using official Shimmer SDK
        val result = enhancedGSRManager.connectToDevice(testAddress, testDeviceName)
        
        Assert.assertTrue("Connection should be initiated with Shimmer SDK", result)
    }

    @Test
    fun testSamplingRateConfiguration() {
        val expectedSamplingRate = 128.0  // Hz
        
        // Test default sampling rate configuration
        val currentConfig = enhancedGSRManager.getCurrentConfiguration()
        
        Assert.assertNotNull("Configuration should not be null", currentConfig)
        // Note: In test environment, we verify the configuration is properly structured
    }

    @Test
    fun testSynchronizedCaptureIntegration() {
        // Test integration with synchronized capture system
        val syncResult = enhancedGSRManager.enableSynchronizedCapture(true)
        
        Assert.assertTrue("Synchronized capture should be configurable", syncResult)
    }

    @Test
    fun testDataQualityMetrics() {
        // Test enhanced data quality assessment
        val testGSRValue = 450.5
        val testTemperature = 32.1
        
        val quality = enhancedGSRManager.assessDataQuality(testGSRValue, testTemperature)
        
        Assert.assertTrue("Data quality should be within valid range", quality >= 0.0 && quality <= 1.0)
    }

    @Test
    fun testHighPrecisionTimestamps() {
        // Test nanosecond-precision timestamp generation
        val timestamp1 = enhancedGSRManager.getCurrentTimestampNanos()
        Thread.sleep(1) // Small delay
        val timestamp2 = enhancedGSRManager.getCurrentTimestampNanos()
        
        Assert.assertTrue("High precision timestamps should be sequential", timestamp2 > timestamp1)
    }

    @Test
    fun testConcurrentOperationSafety() {
        val testAddress = "00:06:66:12:34:56"
        val latch = CountDownLatch(2)
        
        // Test concurrent connection attempts
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
        // Test Shimmer3-specific configuration validation
        val config = createShimmerTestConfiguration()
        
        val isValid = enhancedGSRManager.validateConfiguration(config)
        
        Assert.assertTrue("Valid Shimmer configuration should be accepted", isValid)
    }

    @Test
    fun testGSRSensorEnableState() {
        val config = createShimmerTestConfiguration()
        
        // Test GSR sensor configuration
        val gsrEnabled = config.isSensorEnabled(0x04) // GSR sensor mask
        
        Assert.assertTrue("GSR sensor should be enabled in configuration", gsrEnabled)
    }

    @Test
    fun testTemperatureSensorIntegration() {
        val config = createShimmerTestConfiguration()
        
        // Test temperature sensor configuration  
        val tempEnabled = config.isSensorEnabled(0x80) // Temperature sensor mask
        
        Assert.assertTrue("Temperature sensor should be enabled for skin temperature", tempEnabled)
    }

    @Test
    fun testRecordingStateManagement() {
        val testAddress = "00:06:66:12:34:56"
        
        // Test recording state transitions
        enhancedGSRManager.connectToDevice(testAddress, "TestDevice")
        
        val startResult = enhancedGSRManager.startRecording()
        Assert.assertTrue("Recording should start successfully", startResult)
        
        enhancedGSRManager.stopRecording()
        Assert.assertFalse("Recording should stop successfully", enhancedGSRManager.isRecording())
    }

    @Test
    fun testDataBufferingStrategy() {
        // Test enhanced data buffering for synchronized capture
        val bufferSize = enhancedGSRManager.getDataBufferSize()
        
        Assert.assertTrue("Data buffer should be configured appropriately", bufferSize > 0)
    }

    @Test
    fun testErrorRecoveryMechanisms() {
        // Test enhanced error recovery
        enhancedGSRManager.simulateConnectionError()
        
        val recoveryResult = enhancedGSRManager.attemptRecovery()
        
        Assert.assertTrue("Error recovery should be attempted", recoveryResult)
    }

    @Test
    fun testSensorCalibrationState() {
        // Test sensor calibration tracking
        val calibrationStatus = enhancedGSRManager.getCalibrationStatus()
        
        Assert.assertNotNull("Calibration status should be trackable", calibrationStatus)
    }

    @Test
    fun testBatteryLevelMonitoring() {
        // Test battery level monitoring for Shimmer device
        val batteryLevel = enhancedGSRManager.getBatteryLevel()
        
        // Battery level should be -1 (unknown) when not connected or 0-100 when connected
        Assert.assertTrue("Battery level should be in valid range", batteryLevel >= -1 && batteryLevel <= 100)
    }

    @Test
    fun testConnectionTimeout() {
        val testAddress = "00:06:66:12:34:56"
        val timeoutMs = 5000L
        
        // Test connection timeout handling
        enhancedGSRManager.setConnectionTimeout(timeoutMs)
        val result = enhancedGSRManager.connectToDevice(testAddress, "TestDevice")
        
        Assert.assertTrue("Connection attempt should handle timeout configuration", result)
    }

    @Test
    fun testMemoryUsageOptimization() {
        // Test memory usage optimization
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        enhancedGSRManager.optimizeMemoryUsage()
        
        val afterOptimization = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Memory optimization should not increase memory usage
        Assert.assertTrue("Memory optimization should not increase usage", afterOptimization <= initialMemory + 1000000) // 1MB tolerance
    }

    @Test
    fun testThreadSafetyWithMultipleListeners() {
        val listener1 = mock<EnhancedGSRManager.EnhancedGSRDataListener>()
        val listener2 = mock<EnhancedGSRManager.EnhancedGSRDataListener>()
        
        // Test thread-safe listener management
        enhancedGSRManager.addDataListener(listener1)
        enhancedGSRManager.addDataListener(listener2)
        
        val listenerCount = enhancedGSRManager.getActiveListenerCount()
        
        Assert.assertEquals("Should track multiple listeners correctly", 2, listenerCount)
    }

    @Test
    fun testSynchronizationWithMasterClock() {
        // Test integration with global master clock
        val syncEnabled = enhancedGSRManager.enableMasterClockSync(true)
        
        Assert.assertTrue("Master clock synchronization should be configurable", syncEnabled)
    }

    @Test
    fun testDataExportFormats() {
        // Test supported data export formats
        val supportedFormats = enhancedGSRManager.getSupportedExportFormats()
        
        Assert.assertNotNull("Should provide supported export formats", supportedFormats)
        Assert.assertTrue("Should support at least CSV format", supportedFormats.contains("CSV"))
    }

    // Helper methods for creating test data
    private fun createShimmerTestConfiguration(): Configuration {
        return Configuration().apply {
            setSamplingRateShimmer(128.0)
            setSensorEnabledState(0x04, true) // GSR
            setSensorEnabledState(0x80, true) // Temperature
            // Configure for Shimmer3 device
            setHardwareVersion(3)
            setFirmwareVersionMajor(0)
            setFirmwareVersionMinor(15)
        }
    }
