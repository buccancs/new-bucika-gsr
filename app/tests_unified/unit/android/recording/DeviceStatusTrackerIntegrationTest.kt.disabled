package com.multisensor.recording.recording

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import com.multisensor.recording.util.Logger

/**
 * comprehensive integration test for device status tracking
 * tests status aggregation, monitoring, and real-time updates
 */
class DeviceStatusTrackerIntegrationTest {
    
    @Mock
    private lateinit var mockLogger: Logger
    
    @Mock
    private lateinit var mockCameraRecorder: CameraRecorder
    
    @Mock
    private lateinit var mockThermalRecorder: ThermalRecorder
    
    @Mock
    private lateinit var mockShimmerRecorder: ShimmerRecorder
    
    private lateinit var deviceStatusTracker: DeviceStatusTracker
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        deviceStatusTracker = DeviceStatusTracker(mockLogger)
    }
    
    @Test
    fun `should track camera recording status changes`() = runTest {
        // given
        whenever(mockCameraRecorder.isRecording()).thenReturn(false)
        
        // when
        deviceStatusTracker.updateCameraStatus(false)
        val initialStatus = deviceStatusTracker.getDeviceStatus().first()
        
        deviceStatusTracker.updateCameraStatus(true)
        val updatedStatus = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertFalse(initialStatus.cameraRecording)
        assertTrue(updatedStatus.cameraRecording)
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should track thermal camera connection status`() = runTest {
        // given
        whenever(mockThermalRecorder.isConnected()).thenReturn(true)
        
        // when
        deviceStatusTracker.updateThermalStatus(true)
        val status = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertTrue(status.thermalConnected)
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should track shimmer sensor connection and data flow`() = runTest {
        // given
        whenever(mockShimmerRecorder.isConnected()).thenReturn(true)
        whenever(mockShimmerRecorder.getDataRate()).thenReturn(51.2) // Hz
        
        // when
        deviceStatusTracker.updateShimmerStatus(true, 51.2)
        val status = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertTrue(status.shimmerConnected)
        assertEquals(51.2, status.shimmerDataRate, 0.1)
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should aggregate overall recording status`() = runTest {
        // given - all components recording
        deviceStatusTracker.updateCameraStatus(true)
        deviceStatusTracker.updateThermalStatus(true) 
        deviceStatusTracker.updateShimmerStatus(true, 51.2)
        
        // when
        val status = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertTrue(status.isRecording)
        assertTrue(status.allSystemsOperational)
    }
    
    @Test
    fun `should detect system degradation when components fail`() = runTest {
        // given - initial good state
        deviceStatusTracker.updateCameraStatus(true)
        deviceStatusTracker.updateThermalStatus(true)
        deviceStatusTracker.updateShimmerStatus(true, 51.2)
        
        // when - thermal camera disconnects
        deviceStatusTracker.updateThermalStatus(false)
        val status = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertFalse(status.allSystemsOperational)
        assertFalse(status.thermalConnected)
        verify(mockLogger).logW(any())
    }
    
    @Test
    fun `should track battery level changes`() = runTest {
        // given
        val initialBattery = 85
        val lowBattery = 15
        
        // when
        deviceStatusTracker.updateBatteryLevel(initialBattery)
        var status = deviceStatusTracker.getDeviceStatus().first()
        assertEquals(initialBattery, status.batteryLevel)
        
        deviceStatusTracker.updateBatteryLevel(lowBattery)
        status = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertEquals(lowBattery, status.batteryLevel)
        assertTrue(status.lowBattery)
        verify(mockLogger).logW(any())
    }
    
    @Test
    fun `should monitor storage space availability`() = runTest {
        // given
        val availableGB = 2.5
        val lowStorageGB = 0.5
        
        // when
        deviceStatusTracker.updateStorageSpace(availableGB)
        var status = deviceStatusTracker.getDeviceStatus().first()
        assertFalse(status.lowStorage)
        
        deviceStatusTracker.updateStorageSpace(lowStorageGB)
        status = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertTrue(status.lowStorage)
        verify(mockLogger).logW(any())
    }
    
    @Test
    fun `should track network connectivity for pc communication`() = runTest {
        // given
        val strongSignal = -45 // dBm
        val weakSignal = -85 // dBm
        
        // when
        deviceStatusTracker.updateWifiSignal(strongSignal)
        var status = deviceStatusTracker.getDeviceStatus().first()
        assertTrue(status.goodWifiConnection)
        
        deviceStatusTracker.updateWifiSignal(weakSignal)
        status = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertFalse(status.goodWifiConnection)
        verify(mockLogger).logW(any())
    }
    
    @Test
    fun `should calculate data throughput metrics`() = runTest {
        // given
        val videoDataMB = 12.5
        val thermalDataMB = 3.2
        val gsrDataMB = 0.8
        
        // when
        deviceStatusTracker.updateDataThroughput(videoDataMB, thermalDataMB, gsrDataMB)
        val status = deviceStatusTracker.getDeviceStatus().first()
        
        // then
        assertEquals(16.5, status.totalDataThroughputMB, 0.1)
        verify(mockLogger).logD(any())
    }
    
    @Test
    fun `should provide comprehensive system health report`() = runTest {
        // given - configure complete system state
        deviceStatusTracker.updateCameraStatus(true)
        deviceStatusTracker.updateThermalStatus(true)
        deviceStatusTracker.updateShimmerStatus(true, 51.2)
        deviceStatusTracker.updateBatteryLevel(75)
        deviceStatusTracker.updateStorageSpace(8.5)
        deviceStatusTracker.updateWifiSignal(-55)
        deviceStatusTracker.updateDataThroughput(10.0, 2.5, 0.5)
        
        // when
        val healthReport = deviceStatusTracker.generateHealthReport()
        
        // then
        assertTrue(healthReport.contains("operational"))
        assertTrue(healthReport.contains("75%"))
        assertTrue(healthReport.contains("8.5 GB"))
        verify(mockLogger).logI(any())
    }
}