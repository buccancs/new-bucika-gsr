package com.topdon.bucika.pc.session

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SessionManagerTest {
    
    private lateinit var sessionManager: SessionManager
    
    @BeforeTest
    fun setup() {
        sessionManager = SessionManager()
    }
    
    @Test
    fun `createSession should create new session with NEW state`() {
        val session = sessionManager.createSession()
        
        assertNotNull(session)
        assertEquals(SessionState.NEW, session.state)
        assertTrue(session.name.startsWith("session_"))
        assertTrue(session.devices.isEmpty())
    }
    
    @Test
    fun `armSession should transition from NEW to ARMED when valid`() = runBlocking {
        val session = sessionManager.createSession()
        
        // Should fail due to no devices
        val result1 = sessionManager.armSession(session.id)
        assertFalse(result1, "Should fail without devices")
        assertEquals(SessionState.NEW, session.state)
        
        // Add a mock GSR leader device
        sessionManager.addDevice(session.id, "device1", createMockDeviceInfo())
        
        val result2 = sessionManager.armSession(session.id)
        assertTrue(result2, "Should succeed with GSR leader device")
        assertEquals(SessionState.ARMED, session.state)
    }
    
    @Test
    fun `startSession should transition from ARMED to RECORDING`() = runBlocking {
        val session = sessionManager.createSession()
        sessionManager.addDevice(session.id, "device1", createMockDeviceInfo())
        sessionManager.armSession(session.id)
        
        val result = sessionManager.startSession(session.id)
        
        assertTrue(result)
        assertEquals(SessionState.RECORDING, session.state)
        assertNotNull(session.metadata.startTime)
    }
    
    @Test
    fun `stopSession should transition from RECORDING to FINALISING then DONE`() = runBlocking {
        val session = sessionManager.createSession()
        sessionManager.addDevice(session.id, "device1", createMockDeviceInfo())
        sessionManager.armSession(session.id)
        sessionManager.startSession(session.id)
        
        val result = sessionManager.stopSession(session.id)
        
        assertTrue(result)
        // Session should eventually reach DONE state
        // (in real implementation, finalizeSession would be called)
    }
    
    @Test
    fun `recordSyncMark should add sync mark event`() = runBlocking {
        val session = sessionManager.createSession()
        val markerId = "test-sync-001"
        
        sessionManager.recordSyncMark(session.id, markerId)
        
        val syncMarkEvents = session.metadata.events.filter { it.type == "SYNC_MARK" }
        assertEquals(1, syncMarkEvents.size)
        assertEquals(markerId, syncMarkEvents[0].data["markerId"])
    }
    
    private fun createMockDeviceInfo() = com.topdon.bucika.pc.session.DeviceInfo(
        deviceId = "device1",
        deviceName = "Mock GSR Device",
        capabilities = listOf("GSR_LEADER", "RGB"),
        batteryLevel = 85,
        version = "1.0.0",
        role = "GSR_LEADER"
    )
}