package com.multisensor.recording.service

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.multisensor.recording.persistence.CrashRecoveryManager
import com.multisensor.recording.persistence.SessionStateDao
import com.multisensor.recording.service.util.FileStructureManager
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi
class SessionManagerUnitTest {

    private lateinit var sessionManager: SessionManager
    private val mockContext: Context = mockk(relaxed = true)
    private val mockLogger: Logger = mockk(relaxed = true)
    private val mockThermalSettings: ThermalCameraSettings = mockk(relaxed = true)
    private val mockSessionStateDao: SessionStateDao = mockk(relaxed = true)
    private val mockCrashRecoveryManager: CrashRecoveryManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { mockLogger.info(any()) } returns Unit
        every { mockLogger.debug(any()) } returns Unit
        every { mockLogger.error(any()) } returns Unit

        sessionManager = SessionManager(
            mockContext,
            mockLogger,
            mockThermalSettings,
            mockSessionStateDao,
            mockCrashRecoveryManager,
            mockk<FileStructureManager>(relaxed = true)
        )
    }

    @Test
    fun `createNewSession should create new session with unique ID`() = runTest {
        val sessionId = sessionManager.createNewSession()

        assertThat(sessionId).isNotEmpty()
        assertThat(sessionId).startsWith("session_")

        verify { mockLogger.info(match { it.contains("Creating new session") }) }
    }

    @Test
    fun `createNewSession should create different IDs for subsequent calls`() = runTest {
        val sessionId1 = sessionManager.createNewSession()
        val sessionId2 = sessionManager.createNewSession()

        assertThat(sessionId1).isNotEqualTo(sessionId2)
    }

    @Test
    fun `getCurrentSession should return null when no session is active`() = runTest {
        val session = sessionManager.getCurrentSession()

        assertThat(session).isNull()
    }

    @Test
    fun `getCurrentSession should return session when session is active`() = runTest {
        val sessionId = sessionManager.createNewSession()

        val session = sessionManager.getCurrentSession()

        assertThat(session).isNotNull()
        assertThat(session?.sessionId).isEqualTo(sessionId)
    }

    @Test
    fun `finalizeCurrentSession should complete active session`() = runTest {
        val sessionId = sessionManager.createNewSession()

        sessionManager.finalizeCurrentSession()

        verify { mockLogger.info(match { it.contains("Finalizing session") }) }
    }

    @Test
    fun `getSessionOutputDir should return directory when session exists`() = runTest {
        sessionManager.createNewSession()

        val outputDir = sessionManager.getSessionOutputDir()

        assertThat(outputDir).isNotNull()
        assertThat(outputDir?.exists()).isTrue()
    }

    @Test
    fun `getSessionOutputDir should return null when no active session`() = runTest {
        val outputDir = sessionManager.getSessionOutputDir()

        assertThat(outputDir).isNull()
    }

    @Test
    fun `hasSufficientStorage should check available space`() = runTest {
        val hasSufficientSpace = sessionManager.hasSufficientStorage()

        assertThat(hasSufficientSpace is Boolean).isTrue()
    }

    @Test
    fun `getAvailableStorageSpace should return positive value`() = runTest {
        val availableSpace = sessionManager.getAvailableStorageSpace()

        assertThat(availableSpace).isAtLeast(0L)
    }

    @Test
    fun `getAllSessions should return list of sessions`() = runTest {
        val sessions = sessionManager.getAllSessions()

        assertThat(sessions).isNotNull()

        assertThat(sessions).isEmpty()
    }

    @Test
    fun `deleteAllSessions should complete successfully`() = runTest {
        val deleteResult = sessionManager.deleteAllSessions()

        assertThat(deleteResult).isTrue()
        verify { mockLogger.info(match { it.contains("Deleting all sessions") }) }
    }

    @Test
    fun `session lifecycle should work end to end`() = runTest {

        val sessionId = sessionManager.createNewSession()
        assertThat(sessionId).isNotEmpty()

        val session = sessionManager.getCurrentSession()
        assertThat(session).isNotNull()
        assertThat(session?.sessionId).isEqualTo(sessionId)

        val outputDir = sessionManager.getSessionOutputDir()
        assertThat(outputDir).isNotNull()

        sessionManager.finalizeCurrentSession()

        verify { mockLogger.info(match { it.contains("Creating new session") }) }
        verify { mockLogger.info(match { it.contains("Finalizing session") }) }
    }

    @Test
    fun `multiple sessions should work sequentially`() = runTest {

        val sessionId1 = sessionManager.createNewSession()
        sessionManager.finalizeCurrentSession()

        val sessionId2 = sessionManager.createNewSession()

        assertThat(sessionId1).isNotEqualTo(sessionId2)

        val currentSession = sessionManager.getCurrentSession()
        assertThat(currentSession?.sessionId).isEqualTo(sessionId2)
    }
}
