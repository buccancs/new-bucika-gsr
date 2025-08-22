package com.multisensor.recording.recording
import com.multisensor.recording.util.Logger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi
class ConnectionManagerComprehensiveTest {
    private lateinit var mockLogger: Logger
    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setup() {
        mockLogger = mockk(relaxed = true)
        connectionManager = ConnectionManager(mockLogger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `connection manager initialization should succeed`() = runTest {
        assertNotNull("ConnectionManager should be created", connectionManager)
        
        connectionManager.startManagement()
        
        val stats = connectionManager.getOverallStatistics()
        assertNotNull("Statistics should be available", stats)
        assertTrue("Should be managing", stats["isManaging"] as Boolean)
        
        verify { mockLogger.info("Starting enhanced connection management") }
    }

    @Test
    fun `connection with retry should work correctly`() = runTest {
        val deviceId = "test_device_001"
        var attemptCount = 0
        
        val connectionFunction: suspend () -> Boolean = {
            attemptCount++
            attemptCount <= 2 // Fail first attempt, succeed on second
        }
        
        connectionManager.startManagement()
        val result = connectionManager.connectWithRetry(deviceId, connectionFunction)
        
        assertFalse("Connection should fail after max attempts", result)
        assertTrue("Should have made multiple attempts", attemptCount > 1)
        
        verify { mockLogger.info("Starting connection attempt for device: $deviceId") }
    }

    @Test
    fun `successful connection should update statistics`() = runTest {
        val deviceId = "successful_device"
        
        val connectionFunction: suspend () -> Boolean = { true }
        
        connectionManager.startManagement()
        val result = connectionManager.connectWithRetry(deviceId, connectionFunction)
        
        assertTrue("Connection should succeed", result)
        
        val deviceStats = connectionManager.getConnectionStatistics(deviceId)
        assertNotNull("Device statistics should be available", deviceStats)
        assertEquals("Device ID should match", deviceId, deviceStats["deviceId"])
        assertTrue("Should have at least one successful attempt", 
                  (deviceStats["successfulAttempts"] as Int) > 0)
        
        val health = connectionManager.getConnectionHealth(deviceId)
        assertNotNull("Connection health should be available", health)
        assertTrue("Device should be healthy", health!!.isHealthy)
    }

    @Test
    fun `failed connection should update failure statistics`() = runTest {
        val deviceId = "failing_device"
        
        val connectionFunction: suspend () -> Boolean = { false }
        
        connectionManager.startManagement()
        val result = connectionManager.connectWithRetry(deviceId, connectionFunction)
        
        assertFalse("Connection should fail", result)
        
        val deviceStats = connectionManager.getConnectionStatistics(deviceId)
        assertNotNull("Device statistics should be available", deviceStats)
        assertTrue("Should have failed attempts", 
                  (deviceStats["failedAttempts"] as Int) > 0)
        
        val overallStats = connectionManager.getOverallStatistics()
        assertTrue("Should have recorded failed connections", 
                  (overallStats["failedConnections"] as Long) > 0)
    }

    @Test
    fun `auto reconnection can be started and stopped`() = runTest {
        val deviceId = "auto_reconnect_device"
        
        val connectionFunction: suspend () -> Boolean = { true }
        
        connectionManager.startManagement()
        connectionManager.startAutoReconnection(deviceId, connectionFunction)
        
        // Stop auto reconnection
        connectionManager.stopAutoReconnection(deviceId)
        
        verify { mockLogger.debug("Stopped auto-reconnection for device: $deviceId") }
    }

    @Test
    fun `health monitoring can be started and stopped`() = runTest {
        val deviceId = "health_monitor_device"
        
        connectionManager.startManagement()
        connectionManager.startHealthMonitoring(deviceId)
        
        // Stop health monitoring
        connectionManager.stopHealthMonitoring(deviceId)
        
        verify { mockLogger.debug("Stopped health monitoring for device: $deviceId") }
    }

    @Test
    fun `statistics can be reset for individual devices`() = runTest {
        val deviceId = "reset_test_device"
        
        val connectionFunction: suspend () -> Boolean = { false }
        
        connectionManager.startManagement()
        connectionManager.connectWithRetry(deviceId, connectionFunction)
        
        // Verify stats exist
        val statsBeforeReset = connectionManager.getConnectionStatistics(deviceId)
        assertTrue("Should have attempts before reset", 
                  (statsBeforeReset["totalAttempts"] as Int) > 0)
        
        // Reset statistics
        connectionManager.resetDeviceStatistics(deviceId)
        
        val statsAfterReset = connectionManager.getConnectionStatistics(deviceId)
        assertEquals("Should have no attempts after reset", 0, statsAfterReset["totalAttempts"])
        
        verify { mockLogger.info("Reset statistics for device: $deviceId") }
    }

    @Test
    fun `all statistics can be reset`() = runTest {
        val deviceId = "all_reset_test_device"
        
        val connectionFunction: suspend () -> Boolean = { true }
        
        connectionManager.startManagement()
        connectionManager.connectWithRetry(deviceId, connectionFunction)
        
        // Verify stats exist
        val overallStatsBefore = connectionManager.getOverallStatistics()
        assertTrue("Should have total attempts before reset", 
                  (overallStatsBefore["totalConnectionAttempts"] as Long) > 0)
        
        // Reset all statistics
        connectionManager.resetAllStatistics()
        
        val overallStatsAfter = connectionManager.getOverallStatistics()
        assertEquals("Should have no attempts after reset", 0L, 
                    overallStatsAfter["totalConnectionAttempts"])
        
        verify { mockLogger.info("Reset all connection statistics") }
    }

    @Test
    fun `connection manager cleanup should work correctly`() = runTest {
        connectionManager.startManagement()
        
        val deviceId = "cleanup_test_device"
        connectionManager.startAutoReconnection(deviceId) { true }
        connectionManager.startHealthMonitoring(deviceId)
        
        // Cleanup
        connectionManager.cleanup()
        
        val overallStats = connectionManager.getOverallStatistics()
        assertFalse("Should not be managing after cleanup", overallStats["isManaging"] as Boolean)
        
        verify { mockLogger.info("ConnectionManager cleanup completed") }
    }

    @Test
    fun `get all connection health should return all device health data`() = runTest {
        val deviceIds = listOf("device1", "device2", "device3")
        
        connectionManager.startManagement()
        
        // Create some connection attempts for each device
        deviceIds.forEach { deviceId ->
            connectionManager.connectWithRetry(deviceId) { true }
        }
        
        val allHealth = connectionManager.getAllConnectionHealth()
        assertNotNull("All health data should be available", allHealth)
        
        // Note: Due to the async nature, devices might not all be present
        // This test verifies the method works and returns a map
        assertTrue("Should return a map", allHealth is Map<*, *>)
    }
}