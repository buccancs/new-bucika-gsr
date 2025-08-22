package com.multisensor.recording.recording

import kotlinx.coroutines.test.runTest
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
 * comprehensive test suite for pc communication handler
 * tests socket communication, command processing, and error handling
 */
class PCCommunicationHandlerTest {
    
    @Mock
    private lateinit var mockLogger: Logger
    
    @Mock  
    private lateinit var mockConnectionManager: ConnectionManager
    
    private lateinit var pcCommunicationHandler: PCCommunicationHandler
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        pcCommunicationHandler = PCCommunicationHandler(mockLogger, mockConnectionManager)
    }
    
    @Test
    fun `should establish connection to pc successfully`() = runTest {
        // given
        val serverAddress = "192.168.1.100"
        val serverPort = 8080
        whenever(mockConnectionManager.connect(serverAddress, serverPort)).thenReturn(true)
        
        // when
        val result = pcCommunicationHandler.connectToPC(serverAddress, serverPort)
        
        // then
        assertTrue(result)
        verify(mockConnectionManager).connect(serverAddress, serverPort)
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should handle connection failure gracefully`() = runTest {
        // given
        val serverAddress = "192.168.1.100" 
        val serverPort = 8080
        whenever(mockConnectionManager.connect(serverAddress, serverPort)).thenReturn(false)
        
        // when
        val result = pcCommunicationHandler.connectToPC(serverAddress, serverPort)
        
        // then
        assertFalse(result)
        verify(mockLogger).logE(any())
    }
    
    @Test
    fun `should send device status updates to pc`() = runTest {
        // given
        val deviceStatus = DeviceStatus(
            batteryLevel = 85,
            isRecording = true,
            thermalConnected = true,
            shimmerConnected = false
        )
        whenever(mockConnectionManager.isConnected()).thenReturn(true)
        
        // when
        pcCommunicationHandler.sendDeviceStatus(deviceStatus)
        
        // then
        verify(mockConnectionManager).sendMessage(any())
    }
    
    @Test
    fun `should process start recording command from pc`() = runTest {
        // given
        val startCommand = """{"command": "start_recording", "sessionId": "test_123"}"""
        var commandReceived = false
        
        pcCommunicationHandler.setCommandCallback { command ->
            if (command.type == "start_recording") {
                commandReceived = true
            }
        }
        
        // when
        pcCommunicationHandler.processIncomingMessage(startCommand)
        
        // then
        assertTrue(commandReceived)
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should handle malformed json commands gracefully`() = runTest {
        // given
        val malformedCommand = """{"command": "invalid_json"...}"""
        
        // when
        pcCommunicationHandler.processIncomingMessage(malformedCommand)
        
        // then
        verify(mockLogger).logE(any())
    }
    
    @Test  
    fun `should disconnect from pc cleanly`() = runTest {
        // given
        whenever(mockConnectionManager.isConnected()).thenReturn(true)
        
        // when
        pcCommunicationHandler.disconnect()
        
        // then
        verify(mockConnectionManager).disconnect()
        verify(mockLogger).logI(any())
    }
    
    @Test
    fun `should stream preview data to pc when connected`() = runTest {
        // given
        val previewData = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        whenever(mockConnectionManager.isConnected()).thenReturn(true)
        
        // when
        pcCommunicationHandler.sendPreviewFrame(previewData)
        
        // then
        verify(mockConnectionManager).sendBinaryData(previewData)
    }
    
    private data class DeviceStatus(
        val batteryLevel: Int,
        val isRecording: Boolean,
        val thermalConnected: Boolean,
        val shimmerConnected: Boolean
    )
}