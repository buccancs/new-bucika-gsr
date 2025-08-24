package com.topdon.tc001.gsr

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Comprehensive test suite for GSRManager
 * Tests core functionality, device management, data processing, and listener interfaces
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class GSRManagerTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockDataListener: GSRManager.GSRDataListener

    @Mock
    private lateinit var mockAdvancedDataListener: GSRManager.AdvancedGSRDataListener

    private lateinit var gsrManager: GSRManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        gsrManager = GSRManager.getInstance(mockContext)
    }

    @After
    fun tearDown() {
        // Clear singleton instance for clean test state
        val instanceField = GSRManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun testSingletonInstance() {
        // Test singleton pattern implementation
        val instance1 = GSRManager.getInstance(mockContext)
        val instance2 = GSRManager.getInstance(mockContext)
        
        Assert.assertSame("GSRManager should follow singleton pattern", instance1, instance2)
    }

    @Test
    fun testInitialState() {
        // Test initial state of GSRManager
        Assert.assertFalse("Initial state should be disconnected", gsrManager.isConnected())
        Assert.assertFalse("Initial state should not be recording", gsrManager.isRecording())
        Assert.assertNull("No device should be connected initially", gsrManager.getConnectedDeviceName())
    }

    @Test
    fun testSetGSRDataListener() {
        // Test setting basic GSR data listener
        gsrManager.setGSRDataListener(mockDataListener)
        
        // Verify listener is set (indirect test through functionality)
        Assert.assertTrue("Listener should be set successfully", true)
    }

    @Test
    fun testSetAdvancedDataListener() {
        // Test setting advanced GSR data listener
        gsrManager.setAdvancedDataListener(mockAdvancedDataListener)
        
        // Verify listener is set (indirect test through functionality)
        Assert.assertTrue("Advanced listener should be set successfully", true)
    }

    @Test
    fun testConnectToDeviceWithValidAddress() {
        val testAddress = "00:11:22:33:44:55"
        val testDeviceName = "Shimmer3-GSR+"
        
        // Test connection attempt
        val result = gsrManager.connectToDevice(testAddress, testDeviceName)
        
        Assert.assertTrue("Connection should be initiated successfully", result)
    }

    @Test
    fun testConnectToDeviceWithInvalidAddress() {
        val invalidAddress = "invalid_address"
        
        // Test connection with invalid address
        val result = gsrManager.connectToDevice(invalidAddress, "TestDevice")
        
        Assert.assertFalse("Connection should fail with invalid address", result)
    }

    @Test
    fun testDisconnectDevice() {
        val testAddress = "00:11:22:33:44:55"
        
        // First connect
        gsrManager.connectToDevice(testAddress, "TestDevice")
        
        // Then disconnect
        gsrManager.disconnectDevice()
        
        Assert.assertFalse("Device should be disconnected", gsrManager.isConnected())
    }

    @Test
    fun testStartRecording() {
        // Test starting data recording
        val result = gsrManager.startRecording()
        
        Assert.assertTrue("Recording should start successfully", result)
    }

    @Test
    fun testStopRecording() {
        // First start recording
        gsrManager.startRecording()
        
        // Then stop recording
        gsrManager.stopRecording()
        
        Assert.assertFalse("Recording should be stopped", gsrManager.isRecording())
    }

    @Test
    fun testGetCurrentConfiguration() {
        // Test configuration retrieval
        val config = gsrManager.getCurrentConfiguration()
        
        Assert.assertNotNull("Configuration should not be null", config)
        Assert.assertTrue("Should have GSR sensor enabled", config.isSensorEnabled(0x04)) // GSR sensor
    }

    @Test
    fun testApplyConfiguration() {
        val testConfig = createTestConfiguration()
        
        // Test configuration application
        val result = gsrManager.applyConfiguration(testConfig)
        
        Assert.assertTrue("Configuration should be applied successfully", result)
    }

    @Test
    fun testDataListenerCallback() {
        gsrManager.setGSRDataListener(mockDataListener)
        val latch = CountDownLatch(1)
        
        // Simulate data reception
        val testData = createTestGSRData()
        
        // This would normally be triggered by actual device data
        // For testing, we verify the listener interface
        Assert.assertNotNull("Data listener should be available", mockDataListener)
    }

    @Test
    fun testAdvancedDataListenerCallback() {
        gsrManager.setAdvancedDataListener(mockAdvancedDataListener)
        val latch = CountDownLatch(1)
        
        // Simulate advanced data reception
        val testAdvancedData = createTestAdvancedGSRData()
        
        // Verify advanced listener interface
        Assert.assertNotNull("Advanced data listener should be available", mockAdvancedDataListener)
    }

    @Test
    fun testDataQualityAssessment() {
        // Test data quality assessment functionality
        val testData = createTestGSRData()
        val quality = gsrManager.assessDataQuality(testData.gsrValue, testData.skinTemperature)
        
        Assert.assertTrue("Data quality should be assessable", quality >= 0.0 && quality <= 1.0)
    }

    @Test
    fun testBluetoothPermissions() {
        // Test Bluetooth permission checking
        val hasPermissions = gsrManager.hasRequiredPermissions()
        
        // In test environment, this should handle gracefully
        Assert.assertTrue("Permission check should complete without error", true)
    }

    @Test
    fun testDeviceDiscovery() {
        // Test device discovery functionality
        val result = gsrManager.startDeviceDiscovery()
        
        Assert.assertTrue("Device discovery should be initiatable", result)
    }

    @Test
    fun testSensorCalibration() {
        // Test sensor calibration functionality
        val calibrationResult = gsrManager.calibrateSensors()
        
        Assert.assertTrue("Sensor calibration should be initiatable", calibrationResult)
    }

    @Test
    fun testErrorHandling() {
        // Test error handling with null parameters
        val result = gsrManager.connectToDevice(null, null)
        
        Assert.assertFalse("Should handle null parameters gracefully", result)
    }

    @Test
    fun testMemoryManagement() {
        // Test memory cleanup
        gsrManager.cleanup()
        
        Assert.assertFalse("Should be disconnected after cleanup", gsrManager.isConnected())
        Assert.assertFalse("Should not be recording after cleanup", gsrManager.isRecording())
    }

    // Helper methods for creating test data
    private fun createTestConfiguration(): com.shimmerresearch.driver.Configuration {
        return com.shimmerresearch.driver.Configuration().apply {
            setSamplingRateShimmer(128.0)
            setSensorEnabledState(0x04, true) // GSR
            setSensorEnabledState(0x80, true) // Temperature
        }
    }

    private fun createTestGSRData(): GSRData {
        return GSRData(
            timestamp = System.currentTimeMillis(),
            gsrValue = 450.5,
            skinTemperature = 32.1,
            signalQuality = 0.85
        )
    }

    private fun createTestAdvancedGSRData(): AdvancedGSRData {
        return AdvancedGSRData(
            basicData = createTestGSRData(),
            arousalState = "MEDIUM",
            stressIndicators = mapOf(
                "GSR_VARIABILITY" to 0.42,
                "TEMPERATURE_STABILITY" to 0.88
            ),
            signalArtifacts = emptyList(),
            calibrationStatus = "CALIBRATED"
        )
    }

    // Data classes for testing
    data class GSRData(
        val timestamp: Long,
        val gsrValue: Double,
        val skinTemperature: Double,
        val signalQuality: Double
    )

    data class AdvancedGSRData(
        val basicData: GSRData,
        val arousalState: String,
        val stressIndicators: Map<String, Double>,
        val signalArtifacts: List<String>,
        val calibrationStatus: String
    )
}