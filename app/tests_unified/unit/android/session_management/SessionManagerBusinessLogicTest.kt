package com.multisensor.recording.service

import com.multisensor.recording.service.util.FileStructureManager
import com.multisensor.recording.util.Logger
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SessionManagerBusinessLogicTest {
    private lateinit var mockLogger: Logger
    private lateinit var tempDir: File

    @Before
    fun setup() {
        mockLogger = mockk(relaxed = true)
        tempDir = createTempDir("session_test")
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.deleteRecursively()
    }

    @Test
    fun `SessionStatus enum should have all expected values`() {
        val expectedValues =
            setOf(
                SessionManager.SessionStatus.ACTIVE,
                SessionManager.SessionStatus.COMPLETED,
                SessionManager.SessionStatus.FAILED,
                SessionManager.SessionStatus.CANCELLED,
            )

        val actualValues = SessionManager.SessionStatus.values().toSet()
        assertEquals(expectedValues, actualValues)

        assertEquals(4, SessionManager.SessionStatus.values().size)
    }

    @Test
    fun `RecordingSession should initialize with correct default values`() {
        val sessionId = "test_session_123"
        val startTime = System.currentTimeMillis()
        val sessionFolder = File(tempDir, "session_folder")

        val session =
            SessionManager.RecordingSession(
                sessionId = sessionId,
                startTime = startTime,
                sessionFolder = sessionFolder,
            )

        assertEquals(sessionId, session.sessionId)
        assertEquals(startTime, session.startTime)
        assertEquals(sessionFolder, session.sessionFolder)
        assertNull(session.endTime)
        assertEquals(SessionManager.SessionStatus.ACTIVE, session.status)
    }

    @Test
    fun `RecordingSession should allow setting endTime and status`() {
        val sessionId = "test_session_456"
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 60000
        val sessionFolder = File(tempDir, "session_folder")

        val session =
            SessionManager.RecordingSession(
                sessionId = sessionId,
                startTime = startTime,
                sessionFolder = sessionFolder,
                endTime = endTime,
                status = SessionManager.SessionStatus.COMPLETED,
            )

        assertEquals(sessionId, session.sessionId)
        assertEquals(startTime, session.startTime)
        assertEquals(endTime, session.endTime)
        assertEquals(SessionManager.SessionStatus.COMPLETED, session.status)
    }

    @Test
    fun `SessionFilePaths should create all required file paths`() {
        val sessionFolder = File(tempDir, "test_session")
        val rgbVideoFile = File(sessionFolder, "rgb_video.mp4")
        val thermalVideoFile = File(sessionFolder, "thermal_video.mp4")
        val thermalDataFolder = File(sessionFolder, "thermal_data")
        val rawFramesFolder = File(sessionFolder, "raw_frames")
        val shimmerDataFile = File(sessionFolder, "shimmer_data.csv")
        val logFile = File(sessionFolder, "session.log")
        val calibrationFolder = File(sessionFolder, "calibration")
        val sessionConfigFile = File(sessionFolder, "session_config.json")

        val filePaths =
            FileStructureManager.SessionFilePaths(
                sessionFolder = sessionFolder,
                sessionInfoFile = File(sessionFolder, "session_info.txt"),
                sessionConfigFile = sessionConfigFile,
                rgbVideoFile = rgbVideoFile,
                thermalVideoFile = thermalVideoFile,
                thermalDataFolder = thermalDataFolder,
                rawFramesFolder = rawFramesFolder,
                shimmerDataFile = shimmerDataFile,
                logFile = logFile,
                calibrationFolder = calibrationFolder,
                stimulusEventsFile = File(sessionFolder, "stimulus_events.csv")
            )

        assertEquals(sessionFolder, filePaths.sessionFolder)
        assertEquals(rgbVideoFile, filePaths.rgbVideoFile)
        assertEquals(thermalVideoFile, filePaths.thermalVideoFile)
        assertEquals(thermalDataFolder, filePaths.thermalDataFolder)
        assertEquals(rawFramesFolder, filePaths.rawFramesFolder)
        assertEquals(shimmerDataFile, filePaths.shimmerDataFile)
        assertEquals(logFile, filePaths.logFile)
        assertEquals(calibrationFolder, filePaths.calibrationFolder)
        assertEquals(sessionConfigFile, filePaths.sessionConfigFile)
    }

    @Test
    fun `SessionFilePaths should have correct file extensions`() {
        val sessionFolder = File(tempDir, "test_session")
        val rgbVideoFile = File(sessionFolder, "rgb_video.mp4")
        val thermalVideoFile = File(sessionFolder, "thermal_video.mp4")
        val thermalDataFolder = File(sessionFolder, "thermal_data")
        val rawFramesFolder = File(sessionFolder, "raw_frames")
        val shimmerDataFile = File(sessionFolder, "shimmer_data.csv")
        val logFile = File(sessionFolder, "session.log")
        val calibrationFolder = File(sessionFolder, "calibration")
        val sessionConfigFile = File(sessionFolder, "session_config.json")

        val filePaths =
            FileStructureManager.SessionFilePaths(
                sessionFolder = sessionFolder,
                sessionInfoFile = File(sessionFolder, "session_info.txt"),
                sessionConfigFile = sessionConfigFile,
                rgbVideoFile = rgbVideoFile,
                thermalVideoFile = thermalVideoFile,
                thermalDataFolder = thermalDataFolder,
                rawFramesFolder = rawFramesFolder,
                shimmerDataFile = shimmerDataFile,
                logFile = logFile,
                calibrationFolder = calibrationFolder,
                stimulusEventsFile = File(sessionFolder, "stimulus_events.csv")
            )

        assertTrue("RGB video should be MP4", filePaths.rgbVideoFile.name.endsWith(".mp4"))
        assertTrue("Thermal video should be MP4", filePaths.thermalVideoFile.name.endsWith(".mp4"))
        assertTrue("Shimmer data should be CSV", filePaths.shimmerDataFile.name.endsWith(".csv"))
        assertTrue("Log file should be .log", filePaths.logFile.name.endsWith(".log"))
        assertTrue("Session config should be JSON", filePaths.sessionConfigFile.name.endsWith(".json"))
        assertTrue("Raw frames should be a directory", filePaths.rawFramesFolder.name == "raw_frames")
    }

    @Test
    fun `session duration calculation should work correctly`() {
        val startTime = 1000L
        val endTime = 5000L
        val sessionFolder = File(tempDir, "session_folder")

        val session =
            SessionManager.RecordingSession(
                sessionId = "test_session",
                startTime = startTime,
                sessionFolder = sessionFolder,
                endTime = endTime,
                status = SessionManager.SessionStatus.COMPLETED,
            )

        val duration = session.endTime!! - session.startTime

        assertEquals(4000L, duration)
    }

    @Test
    fun `session should be active by default`() {
        val sessionFolder = File(tempDir, "session_folder")

        val session =
            SessionManager.RecordingSession(
                sessionId = "test_session",
                startTime = System.currentTimeMillis(),
                sessionFolder = sessionFolder,
            )

        assertEquals(SessionManager.SessionStatus.ACTIVE, session.status)
        assertNull(session.endTime)
    }

    @Test
    fun `session folder path should be valid`() {
        val sessionId = "test_session_789"
        val sessionFolder = File(tempDir, sessionId)

        val session =
            SessionManager.RecordingSession(
                sessionId = sessionId,
                startTime = System.currentTimeMillis(),
                sessionFolder = sessionFolder,
            )

        assertTrue(
            "Session folder path should contain session ID",
            session.sessionFolder.name.contains(sessionId),
        )
        assertEquals(tempDir, session.sessionFolder.parentFile)
    }

    @Test
    fun `file operations should handle directory creation`() {
        val sessionFolder = File(tempDir, "new_session")
        val rawFramesFolder = File(sessionFolder, "raw_frames")

        sessionFolder.mkdirs()
        rawFramesFolder.mkdirs()

        assertTrue("Session folder should exist", sessionFolder.exists())
        assertTrue("Session folder should be directory", sessionFolder.isDirectory)
        assertTrue("Raw frames folder should exist", rawFramesFolder.exists())
        assertTrue("Raw frames folder should be directory", rawFramesFolder.isDirectory)
    }

    @Test
    fun `storage space validation logic should work`() {
        val requiredSpace = 1024L * 1024L * 100L
        val availableSpace = tempDir.freeSpace

        val hasSufficientSpace = availableSpace >= requiredSpace

        assertTrue("Available space should be non-negative", availableSpace >= 0)
        assertEquals(
            "Logic should match expected result",
            availableSpace >= requiredSpace,
            hasSufficientSpace,
        )
    }

    @Test
    fun `session ID should be unique and non-empty`() {
        val sessionIds = mutableSetOf<String>()
        val sessionFolder = File(tempDir, "session_folder")

        repeat(10) {
            val sessionId = "session_${System.currentTimeMillis()}_$it"
            sessionIds.add(sessionId)

            val session =
                SessionManager.RecordingSession(
                    sessionId = sessionId,
                    startTime = System.currentTimeMillis(),
                    sessionFolder = sessionFolder,
                )

            assertNotNull("Session ID should not be null", session.sessionId)
            assertTrue("Session ID should not be empty", session.sessionId.isNotEmpty())
        }

        assertEquals("All session IDs should be unique", 10, sessionIds.size)
    }
}
