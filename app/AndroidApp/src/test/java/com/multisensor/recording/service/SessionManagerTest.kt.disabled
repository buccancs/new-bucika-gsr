package com.multisensor.recording.service

import android.content.Context
import com.multisensor.recording.TestConstants
import com.multisensor.recording.testutils.BaseUnitTest
import com.multisensor.recording.persistence.*
import com.multisensor.recording.service.util.FileStructureManager
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.io.IOException

/**
 * Comprehensive unit tests for SessionManager.
 * Covers session lifecycle, file management, crash recovery, and error handling.
 * 
 * Test Categories:
 * - Session creation and initialization
 * - Session lifecycle management (start, pause, resume, stop)
 * - File structure management
 * - Session persistence and recovery
 * - Error handling and edge cases
 * - Concurrent session handling
 * - Memory and resource management
 */
@ExperimentalCoroutinesApi
class SessionManagerTest : BaseUnitTest() {

    @get:Rule
    val mockKRule = io.mockk.junit4.MockKRule(this)

    @MockK
    private lateinit var context: Context
    
    @MockK
    private lateinit var logger: Logger
    
    @MockK
    private lateinit var thermalSettings: ThermalCameraSettings
    
    @MockK
    private lateinit var sessionStateDao: SessionStateDao
    
    @MockK
    private lateinit var crashRecoveryManager: CrashRecoveryManager
    
    @MockK
    private lateinit var fileStructureManager: FileStructureManager

    private lateinit var sessionManager: SessionManager
    private lateinit var testScope: TestScope
    private lateinit var mockSessionFolder: File
    private lateinit var mockSessionInfoFile: File
    private lateinit var mockSessionConfigFile: File

    @Before
    override fun setUp() {
        super.setUp()
        testScope = TestScope(testDispatcher)
        
        // Setup mock files
        mockSessionFolder = mockk<File>(relaxed = true)
        mockSessionInfoFile = mockk<File>(relaxed = true)
        mockSessionConfigFile = mockk<File>(relaxed = true)
        
        setupDefaultMocks()
        
        sessionManager = SessionManager(
            context = context,
            logger = logger,
            thermalSettings = thermalSettings,
            sessionStateDao = sessionStateDao,
            crashRecoveryManager = crashRecoveryManager,
            fileStructureManager = fileStructureManager
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        testScope.cancel()
    }

    private fun setupDefaultMocks() {
        // Context mocks
        every { context.applicationContext } returns context
        every { context.filesDir } returns File("/test/files")
        every { context.getExternalFilesDir(any()) } returns File("/test/external")
        
        // Logger mocks
        every { logger.info(any()) } just Runs
        every { logger.error(any(), any()) } just Runs
        every { logger.debug(any()) } just Runs
        every { logger.warn(any()) } just Runs
        
        // Thermal settings mocks
        every { thermalSettings.resolutionWidth } returns TestConstants.TEST_CAMERA_WIDTH
        every { thermalSettings.resolutionHeight } returns TestConstants.TEST_CAMERA_HEIGHT
        every { thermalSettings.frameRate } returns TestConstants.TEST_CAMERA_FPS
        
        // Session state DAO mocks
        coEvery { sessionStateDao.insertSessionState(any()) } returns Unit
        coEvery { sessionStateDao.updateSessionState(any()) } returns Unit
        coEvery { sessionStateDao.deleteSessionState(any()) } returns Unit
        coEvery { sessionStateDao.getAllActiveSessions() } returns emptyList()
        coEvery { sessionStateDao.getSessionById(any()) } returns null
        
        // Crash recovery manager mocks
        every { crashRecoveryManager.saveSessionState(any()) } just Runs
        every { crashRecoveryManager.clearSessionState(any()) } just Runs
        coEvery { crashRecoveryManager.recoverActiveSessions() } returns emptyList()
        
        // File structure manager mocks
        every { fileStructureManager.createSessionFolder(any()) } returns mockSessionFolder
        every { fileStructureManager.getSessionInfoFile(any()) } returns mockSessionInfoFile
        every { fileStructureManager.getSessionConfigFile(any()) } returns mockSessionConfigFile
        every { fileStructureManager.validateSessionStructure(any()) } returns true
        every { fileStructureManager.cleanupSession(any()) } just Runs
        
        // File mocks
        every { mockSessionFolder.exists() } returns true
        every { mockSessionFolder.mkdirs() } returns true
        every { mockSessionFolder.absolutePath } returns "${TestConstants.TEST_SESSION_DIR}/${TestConstants.TEST_SESSION_ID}"
        every { mockSessionFolder.name } returns TestConstants.TEST_SESSION_ID
        
        every { mockSessionInfoFile.exists() } returns false
        every { mockSessionInfoFile.createNewFile() } returns true
        every { mockSessionInfoFile.writeText(any()) } just Runs
        
        every { mockSessionConfigFile.exists() } returns false
        every { mockSessionConfigFile.createNewFile() } returns true
        every { mockSessionConfigFile.writeText(any()) } just Runs
    }

    @Test
    fun `test_session_creation_success`() = testScope.runTest {
        // Given: SessionManager is initialized
        
        // When: Creating a new session
        val sessionId = sessionManager.createSession()
        
        // Then: Session should be created successfully
        assertNotNull("Session ID should not be null", sessionId)
        assertTrue("Session ID should not be empty", sessionId.isNotEmpty())
        
        // Verify interactions
        verify { fileStructureManager.createSessionFolder(sessionId) }
        verify { logger.info("Creating new session: $sessionId") }
        coVerify { sessionStateDao.insertSessionState(any()) }
        verify { crashRecoveryManager.saveSessionState(any()) }
    }

    @Test
    fun `test_session_creation_with_custom_id`() = testScope.runTest {
        // Given: A custom session ID
        val customSessionId = TestConstants.TEST_SESSION_ID
        
        // When: Creating session with custom ID
        val sessionId = sessionManager.createSession(customSessionId)
        
        // Then: Custom session ID should be used
        assertEquals("Session ID should match custom ID", customSessionId, sessionId)
        
        // Verify file structure creation with custom ID
        verify { fileStructureManager.createSessionFolder(customSessionId) }
    }

    @Test
    fun `test_session_creation_failure_file_system_error`() = testScope.runTest {
        // Given: File system error during folder creation
        every { fileStructureManager.createSessionFolder(any()) } throws IOException("Disk full")
        
        // When: Creating a session
        // Then: Exception should be thrown
        assertThrows(IOException::class.java) {
            runBlocking { sessionManager.createSession() }
        }
        
        // Verify error logging
        verify { logger.error("Failed to create session folder", any()) }
    }

    @Test
    fun `test_start_session_success`() = testScope.runTest {
        // Given: A session is created
        val sessionId = sessionManager.createSession()
        
        // When: Starting the session
        val result = sessionManager.startSession(sessionId)
        
        // Then: Session should start successfully
        assertTrue("Session should start successfully", result)
        
        // Verify session state updates
        coVerify { sessionStateDao.updateSessionState(any()) }
        verify { logger.info("Session started: $sessionId") }
    }

    @Test
    fun `test_start_session_invalid_id`() = testScope.runTest {
        // Given: Invalid session ID
        val invalidSessionId = "invalid_session"
        
        // When: Starting invalid session
        val result = sessionManager.startSession(invalidSessionId)
        
        // Then: Should fail
        assertFalse("Invalid session should not start", result)
        
        // Verify error logging
        verify { logger.error("Cannot start session: $invalidSessionId not found", any()) }
    }

    @Test
    fun `test_stop_session_success`() = testScope.runTest {
        // Given: An active session
        val sessionId = sessionManager.createSession()
        sessionManager.startSession(sessionId)
        
        // When: Stopping the session
        val result = sessionManager.stopSession(sessionId)
        
        // Then: Session should stop successfully
        assertTrue("Session should stop successfully", result)
        
        // Verify cleanup and state updates
        verify { fileStructureManager.validateSessionStructure(sessionId) }
        coVerify { sessionStateDao.updateSessionState(any()) }
        verify { crashRecoveryManager.clearSessionState(sessionId) }
        verify { logger.info("Session completed: $sessionId") }
    }

    @Test
    fun `test_pause_session_success`() = testScope.runTest {
        // Given: An active session
        val sessionId = sessionManager.createSession()
        sessionManager.startSession(sessionId)
        
        // When: Pausing the session
        val result = sessionManager.pauseSession(sessionId)
        
        // Then: Session should pause successfully
        assertTrue("Session should pause successfully", result)
        
        // Verify state updates
        coVerify { sessionStateDao.updateSessionState(any()) }
        verify { logger.info("Session paused: $sessionId") }
    }

    @Test
    fun `test_resume_session_success`() = testScope.runTest {
        // Given: A paused session
        val sessionId = sessionManager.createSession()
        sessionManager.startSession(sessionId)
        sessionManager.pauseSession(sessionId)
        
        // When: Resuming the session
        val result = sessionManager.resumeSession(sessionId)
        
        // Then: Session should resume successfully
        assertTrue("Session should resume successfully", result)
        
        // Verify state updates
        coVerify { sessionStateDao.updateSessionState(any()) }
        verify { logger.info("Session resumed: $sessionId") }
    }

    @Test
    fun `test_cancel_session_success`() = testScope.runTest {
        // Given: An active session
        val sessionId = sessionManager.createSession()
        sessionManager.startSession(sessionId)
        
        // When: Cancelling the session
        val result = sessionManager.cancelSession(sessionId)
        
        // Then: Session should be cancelled successfully
        assertTrue("Session should be cancelled successfully", result)
        
        // Verify cleanup
        verify { fileStructureManager.cleanupSession(sessionId) }
        coVerify { sessionStateDao.deleteSessionState(sessionId) }
        verify { crashRecoveryManager.clearSessionState(sessionId) }
        verify { logger.info("Session cancelled: $sessionId") }
    }

    @Test
    fun `test_get_current_session_info`() = testScope.runTest {
        // Given: An active session
        val sessionId = sessionManager.createSession()
        sessionManager.startSession(sessionId)
        
        // When: Getting current session info
        val sessionInfo = sessionManager.getCurrentSessionInfo()
        
        // Then: Session info should be available
        assertNotNull("Session info should not be null", sessionInfo)
        assertEquals("Session ID should match", sessionId, sessionInfo?.sessionId)
        assertTrue("Start time should be positive", sessionInfo?.startTime ?: 0 > 0)
        assertEquals("Status should be ACTIVE", SessionManager.SessionStatus.ACTIVE, sessionInfo?.status)
    }

    @Test
    fun `test_get_current_session_info_no_active_session`() = testScope.runTest {
        // Given: No active session
        
        // When: Getting current session info
        val sessionInfo = sessionManager.getCurrentSessionInfo()
        
        // Then: Session info should be null
        assertNull("Session info should be null when no active session", sessionInfo)
    }

    @Test
    fun `test_get_session_duration`() = testScope.runTest {
        // Given: An active session running for some time
        val sessionId = sessionManager.createSession()
        sessionManager.startSession(sessionId)
        
        // Simulate session running (advance time)
        val startTime = System.currentTimeMillis()
        Thread.sleep(100) // Small delay to ensure duration > 0
        
        // When: Getting session duration
        val duration = sessionManager.getSessionDuration(sessionId)
        
        // Then: Duration should be positive
        assertTrue("Session duration should be positive", duration > 0)
    }

    @Test
    fun `test_get_all_sessions`() = testScope.runTest {
        // Given: Multiple sessions exist
        val mockSessions = listOf(
            createMockSessionState(TestConstants.TEST_SESSION_ID + "1"),
            createMockSessionState(TestConstants.TEST_SESSION_ID + "2")
        )
        coEvery { sessionStateDao.getAllActiveSessions() } returns mockSessions
        
        // When: Getting all sessions
        val sessions = sessionManager.getAllSessions()
        
        // Then: All sessions should be returned
        assertEquals("Should return all sessions", 2, sessions.size)
        
        // Verify DAO interaction
        coVerify { sessionStateDao.getAllActiveSessions() }
    }

    @Test
    fun `test_delete_session_success`() = testScope.runTest {
        // Given: A completed session
        val sessionId = sessionManager.createSession()
        sessionManager.startSession(sessionId)
        sessionManager.stopSession(sessionId)
        
        // When: Deleting the session
        val result = sessionManager.deleteSession(sessionId)
        
        // Then: Session should be deleted successfully
        assertTrue("Session should be deleted successfully", result)
        
        // Verify cleanup
        verify { fileStructureManager.cleanupSession(sessionId) }
        coVerify { sessionStateDao.deleteSessionState(sessionId) }
        verify { logger.info("Session deleted: $sessionId") }
    }

    @Test
    fun `test_crash_recovery_on_initialization`() = testScope.runTest {
        // Given: Crashed sessions exist
        val crashedSessions = listOf(
            createMockSessionState("crashed_session_1", SessionManager.SessionStatus.ACTIVE),
            createMockSessionState("crashed_session_2", SessionManager.SessionStatus.ACTIVE)
        )
        coEvery { crashRecoveryManager.recoverActiveSessions() } returns crashedSessions
        
        // When: Initializing SessionManager (recovery should happen)
        sessionManager.recoverCrashedSessions()
        
        // Then: Crashed sessions should be recovered
        coVerify { crashRecoveryManager.recoverActiveSessions() }
        verify(atLeast = 2) { logger.info(match { it.contains("Recovering crashed session") }) }
    }

    @Test
    fun `test_session_file_structure_validation`() = testScope.runTest {
        // Given: A session with invalid file structure
        val sessionId = sessionManager.createSession()
        every { fileStructureManager.validateSessionStructure(sessionId) } returns false
        
        // When: Validating session structure
        val isValid = sessionManager.validateSessionStructure(sessionId)
        
        // Then: Validation should fail
        assertFalse("Session structure should be invalid", isValid)
        
        // Verify validation was performed
        verify { fileStructureManager.validateSessionStructure(sessionId) }
    }

    @Test
    fun `test_concurrent_session_management`() = testScope.runTest {
        // Given: Multiple sessions being managed concurrently
        val sessionId1 = sessionManager.createSession("session_1")
        val sessionId2 = sessionManager.createSession("session_2")
        
        // When: Starting both sessions
        val result1 = sessionManager.startSession(sessionId1)
        val result2 = sessionManager.startSession(sessionId2)
        
        // Then: Only one session should be active at a time
        assertTrue("First session should start", result1)
        assertFalse("Second session should not start while first is active", result2)
        
        // Verify appropriate logging
        verify { logger.error("Cannot start session $sessionId2: another session is already active", any()) }
    }

    @Test
    fun `test_session_info_file_creation`() = testScope.runTest {
        // Given: A new session
        val sessionId = sessionManager.createSession()
        
        // When: Starting the session (which creates info file)
        sessionManager.startSession(sessionId)
        
        // Then: Session info file should be created
        verify { fileStructureManager.getSessionInfoFile(sessionId) }
        verify { mockSessionInfoFile.createNewFile() }
    }

    @Test
    fun `test_session_config_file_creation`() = testScope.runTest {
        // Given: A new session
        val sessionId = sessionManager.createSession()
        
        // When: Starting the session (which creates config file)
        sessionManager.startSession(sessionId)
        
        // Then: Session config file should be created
        verify { fileStructureManager.getSessionConfigFile(sessionId) }
        verify { mockSessionConfigFile.createNewFile() }
    }

    @Test
    fun `test_memory_cleanup_on_session_completion`() = testScope.runTest {
        // Given: An active session
        val sessionId = sessionManager.createSession()
        sessionManager.startSession(sessionId)
        
        // When: Stopping the session
        sessionManager.stopSession(sessionId)
        
        // Then: Memory should be cleaned up
        verify { crashRecoveryManager.clearSessionState(sessionId) }
        
        // Current session should be cleared
        val currentSession = sessionManager.getCurrentSessionInfo()
        assertNull("Current session should be null after completion", currentSession)
    }

    @Test
    fun `test_session_status_transitions`() = testScope.runTest {
        // Given: A new session
        val sessionId = sessionManager.createSession()
        
        // When: Transitioning through states
        sessionManager.startSession(sessionId)
        var sessionInfo = sessionManager.getCurrentSessionInfo()
        assertEquals("Should be ACTIVE after start", SessionManager.SessionStatus.ACTIVE, sessionInfo?.status)
        
        sessionManager.pauseSession(sessionId)
        sessionInfo = sessionManager.getCurrentSessionInfo()
        assertEquals("Should be PAUSED after pause", SessionManager.SessionStatus.PAUSED, sessionInfo?.status)
        
        sessionManager.resumeSession(sessionId)
        sessionInfo = sessionManager.getCurrentSessionInfo()
        assertEquals("Should be ACTIVE after resume", SessionManager.SessionStatus.ACTIVE, sessionInfo?.status)
        
        sessionManager.stopSession(sessionId)
        sessionInfo = sessionManager.getCurrentSessionInfo()
        // Current session should be null after stop
        assertNull("Current session should be null after stop", sessionInfo)
    }

    // Helper methods
    private fun createMockSessionState(
        sessionId: String, 
        status: SessionManager.SessionStatus = SessionManager.SessionStatus.COMPLETED
    ): SessionState {
        return SessionState(
            sessionId = sessionId,
            startTime = System.currentTimeMillis() - 30000,
            endTime = System.currentTimeMillis(),
            status = status.name,
            folderPath = "${TestConstants.TEST_SESSION_DIR}/$sessionId"
        )
    }
}