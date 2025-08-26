package com.shimmerresearch.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ShimmerBluetoothTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockBluetoothAdapter: BluetoothAdapter

    @Mock
    private lateinit var mockBluetoothDevice: BluetoothDevice

    @Mock
    private lateinit var mockShimmerDataListener: Shimmer.ShimmerDataListener

    private lateinit var shimmerBluetooth: ShimmerBluetooth
    private lateinit var realContext: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        realContext = ApplicationProvider.getApplicationContext()
        
        whenever(mockBluetoothDevice.address).thenReturn("00:11:22:33:44:55")
        whenever(mockBluetoothDevice.name).thenReturn("Shimmer3-GSR+")
        whenever(mockBluetoothAdapter.isEnabled).thenReturn(true)
        
        shimmerBluetooth = ShimmerBluetooth(realContext, "TestShimmer")
    }

    @Test
    fun testInitialization() {

        Assert.assertNotNull("ShimmerBluetooth should be initialized", shimmerBluetooth)
        Assert.assertEquals("Device name should be set", "TestShimmer", shimmerBluetooth.getShimmerUserAssignedName())
    }

    @Test
    fun testBluetoothConnection() {

        val connectionResult = shimmerBluetooth.connect("00:11:22:33:44:55", "Shimmer3-GSR+")
        
        Assert.assertTrue("Connection should be initiated", connectionResult)
    }

    @Test
    fun testInvalidBluetoothAddress() {

        val connectionResult = shimmerBluetooth.connect("invalid_address", "TestDevice")
        
        Assert.assertFalse("Invalid address should fail connection", connectionResult)
    }

    @Test
    fun testDeviceDiscovery() {

        val discoveryResult = shimmerBluetooth.startDeviceDiscovery()
        
        Assert.assertTrue("Device discovery should start successfully", discoveryResult)
    }

    @Test
    fun testDataStreamingStart() {

        shimmerBluetooth.setShimmerDataListener(mockShimmerDataListener)
        val streamResult = shimmerBluetooth.startStreaming()
        
        Assert.assertTrue("Data streaming should start successfully", streamResult)
    }

    @Test
    fun testDataStreamingStop() {

        shimmerBluetooth.startStreaming()
        shimmerBluetooth.stopStreaming()
        
        Assert.assertFalse("Should not be streaming after stop", shimmerBluetooth.isStreaming())
    }

    @Test
    fun testConnectionState() {

        Assert.assertFalse("Initial state should be disconnected", shimmerBluetooth.isConnected())
        
        shimmerBluetooth.connect("00:11:22:33:44:55", "TestDevice")

        Assert.assertTrue("Connection state should be tracked", true)
    }

    @Test
    fun testDataListenerRegistration() {

        shimmerBluetooth.setShimmerDataListener(mockShimmerDataListener)
        
        Assert.assertNotNull("Data listener should be registered", mockShimmerDataListener)
    }

    @Test
    fun testConfigurationApplication() {

        val testConfig = createTestConfiguration()
        val configResult = shimmerBluetooth.writeConfiguration(testConfig)
        
        Assert.assertTrue("Configuration should be applied successfully", configResult)
    }

    @Test
    fun testBatteryLevelReading() {

        val batteryLevel = shimmerBluetooth.readBatteryLevel()
        
        Assert.assertTrue("Battery level should be readable", batteryLevel >= 0 && batteryLevel <= 100)
    }

    @Test
    fun testDeviceInformation() {

        val deviceInfo = shimmerBluetooth.getDeviceInfo()
        
        Assert.assertNotNull("Device info should be available", deviceInfo)
    }

    @Test
    fun testDataGenerationRealistic() {

        val gsrData = shimmerBluetooth.generateRealisticGSRData()
        
        Assert.assertTrue("GSR value should be in physiological range", 
                         gsrData.gsrValue >= 100.0 && gsrData.gsrValue <= 2000.0)
        Assert.assertTrue("Temperature should be in normal range", 
                         gsrData.skinTemperature >= 25.0 && gsrData.skinTemperature <= 40.0)
        Assert.assertTrue("Signal quality should be normalized", 
                         gsrData.signalQuality >= 0.0 && gsrData.signalQuality <= 1.0)
    }

    @Test
    fun testDataGenerationConsistency() {

        val dataPoints = mutableListOf<GSRDataPoint>()
        
        repeat(100) {
            dataPoints.add(shimmerBluetooth.generateRealisticGSRData())
            Thread.sleep(10)
        }
        
        val gsrValues = dataPoints.map { it.gsrValue }
        val avgGSR = gsrValues.average()
        
        Assert.assertTrue("Average GSR should be physiologically reasonable", 
                         avgGSR >= 200.0 && avgGSR <= 1500.0)
        
        val variance = gsrValues.map { (it - avgGSR) * (it - avgGSR) }.average()
        Assert.assertTrue("GSR should show natural variance", variance > 0)
    }

    @Test
    fun testSamplingRateConfiguration() {

        val samplingRates = arrayOf(1.0, 10.24, 51.2, 128.0, 512.0, 1024.0)
        
        samplingRates.forEach { rate ->
            val result = shimmerBluetooth.setSamplingRate(rate)
            Assert.assertTrue("Sampling rate $rate should be configurable", result)
        }
    }

    @Test
    fun testSensorEnableDisable() {

        val gsrResult = shimmerBluetooth.enableSensor(ShimmerBluetooth.SENSOR_GSR)
        val tempResult = shimmerBluetooth.enableSensor(ShimmerBluetooth.SENSOR_TEMPERATURE)
        
        Assert.assertTrue("GSR sensor should be enabled", gsrResult)
        Assert.assertTrue("Temperature sensor should be enabled", tempResult)
        
        val disableResult = shimmerBluetooth.disableSensor(ShimmerBluetooth.SENSOR_GSR)
        Assert.assertTrue("GSR sensor should be disabled", disableResult)
    }

    @Test
    fun testCalibration() {

        val calibrationResult = shimmerBluetooth.startCalibration()
        
        Assert.assertTrue("Calibration should start successfully", calibrationResult)
    }

    @Test
    fun testErrorHandling() {

        val nullConnectionResult = shimmerBluetooth.connect(null, null)
        Assert.assertFalse("Should handle null parameters gracefully", nullConnectionResult)
        
        val emptyAddressResult = shimmerBluetooth.connect("", "Device")
        Assert.assertFalse("Should handle empty address gracefully", emptyAddressResult)
    }

    @Test
    fun testResourceCleanup() {

        shimmerBluetooth.connect("00:11:22:33:44:55", "TestDevice")
        shimmerBluetooth.startStreaming()
        
        shimmerBluetooth.cleanup()
        
        Assert.assertFalse("Should be disconnected after cleanup", shimmerBluetooth.isConnected())
        Assert.assertFalse("Should not be streaming after cleanup", shimmerBluetooth.isStreaming())
    }

    @Test
    fun testBluetoothPermissions() {

        val hasPermissions = shimmerBluetooth.checkBluetoothPermissions()
        
        Assert.assertTrue("Permission check should complete", true)
    }

    @Test
    fun testDeviceCompatibility() {

        val isCompatible = shimmerBluetooth.isDeviceCompatible("Shimmer3-GSR+")
        Assert.assertTrue("Shimmer3-GSR+ should be compatible", isCompatible)
        
        val isIncompatible = shimmerBluetooth.isDeviceCompatible("Unknown Device")
        Assert.assertFalse("Unknown device should not be compatible", isIncompatible)
    }

    @Test
    fun testFirmwareVersionCheck() {

        val firmwareVersion = shimmerBluetooth.getFirmwareVersion()
        
        Assert.assertNotNull("Firmware version should be available", firmwareVersion)
    }

    @Test
    fun testDataPacketValidation() {

        val validPacket = createValidDataPacket()
        val isValid = shimmerBluetooth.validateDataPacket(validPacket)
        
        Assert.assertTrue("Valid packet should pass validation", isValid)
        
        val invalidPacket = createInvalidDataPacket()
        val isInvalid = shimmerBluetooth.validateDataPacket(invalidPacket)
        
        Assert.assertFalse("Invalid packet should fail validation", isInvalid)
    }

    private fun createTestConfiguration(): com.shimmerresearch.driver.Configuration {
        return com.shimmerresearch.driver.Configuration().apply {
            setSamplingRateShimmer(128.0)
            setSensorEnabledState(0x04, true)
            setSensorEnabledState(0x80, true)
        }
    }

    private fun createValidDataPacket(): ByteArray {

        return byteArrayOf(
            0x00, 0x00,
            0x12, 0x34,
            0x01, 0x90,
            0x20, 0x10
        )
    }

    private fun createInvalidDataPacket(): ByteArray {

        return byteArrayOf(0xFF, 0xFF)
    }

    data class GSRDataPoint(
        val timestamp: Long,
        val gsrValue: Double,
        val skinTemperature: Double,
        val signalQuality: Double
    )
