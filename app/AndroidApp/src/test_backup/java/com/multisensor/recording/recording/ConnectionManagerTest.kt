package com.multisensor.recording.recording

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multisensor.recording.util.Logger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for ConnectionManager component
 * Tests basic initialization and lifecycle management
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class ConnectionManagerTest {

    private lateinit var context: Context
    private lateinit var connectionManager: ConnectionManager  
    private lateinit var mockLogger: Logger
    private var testHost: String = "192.168.1.100"
    private var testPort: Int = 8080

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockLogger = mockk(relaxed = true)
        testHost = "192.168.1.100"
        testPort = 8080
        
        connectionManager = ConnectionManager(mockLogger)
    }

    @After
    fun tearDown() {
        if (this::connectionManager.isInitialized) {
            connectionManager.stopManagement()
        }
        clearAllMocks()
    }

    @Test
    fun `test ConnectionManager initialization`() {
        assertNotNull(connectionManager)
        connectionManager.startManagement()
        connectionManager.stopManagement()
    }

    @Test
    fun `test management lifecycle`() {
        connectionManager.startManagement()
        connectionManager.stopManagement()
        verify { mockLogger.info(any()) }
    }

    @Test
    fun `test auto reconnection start and stop`() {
        val deviceId = "test-device-123"
        
        connectionManager.startAutoReconnection(deviceId) {
            // Mock connection function
            true
        }
        connectionManager.stopAutoReconnection(deviceId)
        
        verify { mockLogger.info(any()) }
    }

    @Test
    fun `test health monitoring lifecycle`() {
        val deviceId = "test-device-456"
        
        connectionManager.startHealthMonitoring(deviceId)
        connectionManager.stopHealthMonitoring(deviceId)
        
        verify { mockLogger.info(any()) }
    }
}