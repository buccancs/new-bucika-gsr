package com.multisensor.recording.service

import android.content.Context
import com.multisensor.recording.persistence.CrashRecoveryManager
import com.multisensor.recording.persistence.SessionStateDao
import com.multisensor.recording.service.util.FileStructureManager
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {
    private lateinit var context: Context
    private lateinit var mockLogger: Logger
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockLogger = mockk(relaxed = true)
        val mockThermalSettings = mockk<ThermalCameraSettings>(relaxed = true)
        val mockSessionStateDao = mockk<SessionStateDao>(relaxed = true)
        val mockCrashRecoveryManager = mockk<CrashRecoveryManager>(relaxed = true)
        val mockFileStructureManager = mockk<FileStructureManager>(relaxed = true)
        sessionManager =
            SessionManager(context, mockLogger, mockThermalSettings, mockSessionStateDao, mockCrashRecoveryManager, mockFileStructureManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createNewSession should generate unique session ID`() =
        runTest {
            val sessionId = sessionManager.createNewSession()

            assertNotNull(sessionId)
            assertTrue(sessionId.isNotEmpty())

            verify { mockLogger.info(match { it.contains("Created new session") }) }
        }

    @Test
    fun `createNewSession should create active session`() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val currentSession = sessionManager.getCurrentSession()

            assertNotNull(currentSession)
            currentSession?.let { session ->
                assertEquals(sessionId, session.sessionId)
                assertEquals(SessionManager.SessionStatus.ACTIVE, session.status)
                assertTrue(session.sessionFolder.exists())
                assertTrue(session.sessionFolder.isDirectory)
            }
        }

    @Test
    fun `getCurrentSession should return null when no active session`() {
        val session = sessionManager.getCurrentSession()

        assertNull(session)
    }

    @Test
    fun `finalizeCurrentSession should complete active session`() =
        runTest {
            sessionManager.createNewSession()
            val originalSession = sessionManager.getCurrentSession()
            assertNotNull(originalSession)

            sessionManager.finalizeCurrentSession()

            val finalizedSession = sessionManager.getCurrentSession()
            assertNull(finalizedSession)

            verify { mockLogger.info(match { it.contains("Finalized session") }) }
        }

    @Test
    fun `finalizeCurrentSession should handle no active session gracefully`() =
        runTest {
            sessionManager.finalizeCurrentSession()

            val session = sessionManager.getCurrentSession()
            assertNull(session)

            verify { mockLogger.warning("No active session to finalize") }
        }

    @Test
    fun `getSessionFilePaths should return null when no active session`() {
        val filePaths = sessionManager.getSessionFilePaths()

        assertNull(filePaths)
    }

    @Test
    fun `getSessionFilePaths should return proper file paths for active session`() =
        runTest {
            sessionManager.createNewSession()

            val filePaths = sessionManager.getSessionFilePaths()

            assertNotNull(filePaths)
            filePaths?.let { paths ->
                assertTrue(paths.sessionFolder.exists())
                assertTrue(paths.sessionFolder.isDirectory)

                assertEquals("video.mp4", paths.rgbVideoFile.name)
                assertEquals("thermal.mp4", paths.thermalVideoFile.name)
                assertEquals("raw_frames", paths.rawFramesFolder.name)
                assertEquals("shimmer_data.csv", paths.shimmerDataFile.name)
                assertEquals("session.log", paths.logFile.name)

                assertEquals(paths.sessionFolder, paths.rgbVideoFile.parentFile)
                assertEquals(paths.sessionFolder, paths.thermalVideoFile.parentFile)
                assertEquals(paths.sessionFolder, paths.rawFramesFolder.parentFile)
                assertEquals(paths.sessionFolder, paths.shimmerDataFile.parentFile)
                assertEquals(paths.sessionFolder, paths.logFile.parentFile)
            }
        }

    @Test
    fun `getAvailableStorageSpace should return positive value`() {
        val availableSpace = sessionManager.getAvailableStorageSpace()

        assertTrue(availableSpace > 0)
    }

    @Test
    fun `hasSufficientStorage should return true for small requirements`() {
        val hasSufficient = sessionManager.hasSufficientStorage(1024)

        assertTrue(hasSufficient)
    }

    @Test
    fun `hasSufficientStorage should return false for excessive requirements`() {
        val hasSufficient = sessionManager.hasSufficientStorage(Long.MAX_VALUE)

        assertFalse(hasSufficient)
    }

    @Test
    fun `multiple sessions should have unique IDs and folders`() =
        runTest {
            val sessionId1 = sessionManager.createNewSession()
            val session1 = sessionManager.getCurrentSession()
            val folder1 = session1?.sessionFolder

            sessionManager.finalizeCurrentSession()

            val sessionId2 = sessionManager.createNewSession()
            val session2 = sessionManager.getCurrentSession()
            val folder2 = session2?.sessionFolder

            assertNotNull(session2)
            assertNotEquals(sessionId1, sessionId2)
            assertNotEquals(folder1, folder2)
            assertTrue(folder2?.exists() ?: false)
        }

    @Test
    fun `session timing should be tracked correctly`() =
        runTest {
            val startTime = System.currentTimeMillis()

            sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()

            assertNotNull(session)
            session?.let {
                assertTrue(it.startTime >= startTime)
                assertTrue(it.startTime <= System.currentTimeMillis())
                assertNull(it.endTime)
            }
        }

    @Test
    fun `SessionFilePaths should create raw_frames directory`() =
        runTest {
            sessionManager.createNewSession()

            val filePaths = sessionManager.getSessionFilePaths()

            assertNotNull(filePaths)
            filePaths?.let { paths ->
                assertTrue(paths.rawFramesFolder.exists())
                assertTrue(paths.rawFramesFolder.isDirectory)
            }
        }

    @Test
    fun `session folder should have proper naming format`() =
        runTest {
            sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()

            assertNotNull(session)
            session?.let {
                assertTrue(it.sessionFolder.name.startsWith("session_"))
                assertTrue(it.sessionFolder.name.contains("_"))
            }
        }

    @Test
    fun `SessionStatus enum should have all expected values`() {
        val statuses = SessionManager.SessionStatus.values()

        assertTrue(statuses.contains(SessionManager.SessionStatus.ACTIVE))
        assertTrue(statuses.contains(SessionManager.SessionStatus.COMPLETED))
        assertTrue(statuses.contains(SessionManager.SessionStatus.FAILED))
        assertTrue(statuses.contains(SessionManager.SessionStatus.CANCELLED))
        assertEquals(4, statuses.size)
    }

    @Test
    fun `SessionFilePaths data class should have all required properties`() =
        runTest {
            sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()

            assertNotNull(filePaths)
            filePaths?.let { paths ->
                assertNotNull(paths.sessionFolder)
                assertNotNull(paths.rgbVideoFile)
                assertNotNull(paths.thermalVideoFile)
                assertNotNull(paths.rawFramesFolder)
                assertNotNull(paths.shimmerDataFile)
                assertNotNull(paths.logFile)
            }
        }
}
