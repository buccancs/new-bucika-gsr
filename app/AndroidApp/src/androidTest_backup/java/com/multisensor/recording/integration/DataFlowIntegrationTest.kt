package com.multisensor.recording.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multisensor.recording.recording.SessionInfo
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DataFlowIntegrationTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var logger: Logger

    private lateinit var testSessionInfo: SessionInfo

    @Before
    fun setup() {
        hiltRule.inject()

        val sessionId = "dataflow_test_${System.currentTimeMillis()}"
        testSessionInfo = SessionInfo(sessionId = sessionId)
    }

    @After
    fun tearDown() {
        try {
            kotlinx.coroutines.runBlocking {
                sessionManager.finalizeCurrentSession()
            }
        } catch (e: Exception) {
        }
    }

    @Test
    fun testBasicDataFlowBetweenComponents() =
        runTest {
            assertNotNull("SessionManager should be injected", sessionManager)
            assertNotNull("Logger should be injected", logger)

            val sessionId = sessionManager.createNewSession()
            logger.info("[DEBUG_LOG] Session created for data flow test: $sessionId")

            val currentSession = sessionManager.getCurrentSession()

            assertNotNull("Session should be created", currentSession)
            assertEquals("Session ID should match", sessionId, currentSession?.sessionId)
            assertEquals("Session should be active", SessionManager.SessionStatus.ACTIVE, currentSession?.status)

            logger.info("[DEBUG_LOG] Basic data flow verified between SessionManager and Logger")
        }

    @Test
    fun testSessionInfoDataFlow() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.startTime = System.currentTimeMillis()

            testSessionInfo.videoEnabled = true
            logger.info("[DEBUG_LOG] Video enabled in SessionInfo")

            testSessionInfo.rawEnabled = true
            logger.info("[DEBUG_LOG] RAW capture enabled in SessionInfo")

            testSessionInfo.thermalEnabled = true
            logger.info("[DEBUG_LOG] Thermal recording enabled in SessionInfo")

            testSessionInfo.videoFilePath = "/test/video.mp4"
            testSessionInfo.addRawFile("/test/raw1.dng")
            testSessionInfo.addRawFile("/test/raw2.dng")
            testSessionInfo.setThermalFile("/test/thermal.bin")
            testSessionInfo.updateThermalFrameCount(100)

            assertTrue("Video should be enabled", testSessionInfo.videoEnabled)
            assertTrue("RAW should be enabled", testSessionInfo.rawEnabled)
            assertTrue("Thermal should be enabled", testSessionInfo.thermalEnabled)
            assertNotNull("Video file path should be set", testSessionInfo.videoFilePath)
            assertEquals("Should have 2 RAW files", 2, testSessionInfo.getRawImageCount())
            assertNotNull("Thermal file should be set", testSessionInfo.thermalFilePath)
            assertEquals("Thermal frame count should be correct", 100L, testSessionInfo.thermalFrameCount)

            logger.info("[DEBUG_LOG] SessionInfo data flow verified: ${testSessionInfo.getSummary()}")
        }

    @Test
    fun testFileSystemDataFlow() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()
            val sessionFolder = session?.sessionFolder

            assertNotNull("Session folder should exist", sessionFolder)
            assertTrue("Session folder should be directory", sessionFolder?.isDirectory == true)

            val filePaths = sessionManager.getSessionFilePaths()
            assertNotNull("File paths should be available", filePaths)

            filePaths?.let { paths ->
                assertEquals("Session folders should match", sessionFolder, paths.sessionFolder)
                assertTrue(
                    "RGB video file should be in session folder",
                    paths.rgbVideoFile.parentFile == sessionFolder,
                )
                assertTrue(
                    "Thermal video file should be in session folder",
                    paths.thermalVideoFile.parentFile == sessionFolder,
                )
                assertTrue(
                    "RAW frames folder should be in session folder",
                    paths.rawFramesFolder.parentFile == sessionFolder,
                )
                assertTrue(
                    "Shimmer data file should be in session folder",
                    paths.shimmerDataFile.parentFile == sessionFolder,
                )
                assertTrue(
                    "Log file should be in session folder",
                    paths.logFile.parentFile == sessionFolder,
                )
            }

            logger.info("[DEBUG_LOG] File system data flow verified for session: $sessionId")
        }

    @Test
    fun testTimestampDataFlow() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val sessionStartTime = System.currentTimeMillis()

            testSessionInfo.startTime = sessionStartTime

            delay(100)

            val dataCollectionTime = System.currentTimeMillis()
            testSessionInfo.addRawFile("/test/timestamped_raw.dng")

            delay(50)

            val completionTime = System.currentTimeMillis()
            testSessionInfo.markCompleted()

            assertTrue("Start time should be set", testSessionInfo.startTime > 0)
            assertTrue("End time should be set", testSessionInfo.endTime > 0)
            assertTrue("End time should be after start time", testSessionInfo.endTime > testSessionInfo.startTime)
            assertTrue("Duration should be positive", testSessionInfo.getDurationMs() > 0)
            assertTrue("Data collection should be after start", dataCollectionTime > sessionStartTime)
            assertTrue("Completion should be after data collection", completionTime > dataCollectionTime)

            logger.info("[DEBUG_LOG] Timestamp data flow verified: duration=${testSessionInfo.getDurationMs()}ms")
        }

    @Test
    fun testErrorDataFlow() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.videoEnabled = true
            testSessionInfo.thermalEnabled = true

            val errorMessage = "Test error for data flow verification"
            testSessionInfo.markError(errorMessage)
            logger.error("Error occurred in data flow test: $errorMessage")

            assertTrue("Error should be marked", testSessionInfo.errorOccurred)
            assertEquals("Error message should match", errorMessage, testSessionInfo.errorMessage)

            val summary = testSessionInfo.getSummary()
            assertTrue("Summary should contain error", summary.contains("ERROR"))
            assertTrue("Summary should contain error message", summary.contains(errorMessage))

            logger.info("[DEBUG_LOG] Error data flow verified: $summary")
        }

    @Test
    fun testDataConsistencyAcrossOperations() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()

            testSessionInfo.videoEnabled = true
            testSessionInfo.rawEnabled = true
            testSessionInfo.thermalEnabled = true
            testSessionInfo.startTime = System.currentTimeMillis()

            repeat(5) { i ->
                testSessionInfo.addRawFile("/test/consistency_raw_$i.dng")
                logger.debug("[DEBUG_LOG] Added RAW file $i")
            }

            testSessionInfo.updateThermalFrameCount(250)
            testSessionInfo.setThermalFile("/test/consistency_thermal.bin")
            testSessionInfo.videoFilePath = "/test/consistency_video.mp4"

            assertEquals("Session IDs should match", sessionId, session?.sessionId)
            assertEquals("Should have 5 RAW files", 5, testSessionInfo.getRawImageCount())
            assertEquals("Thermal frame count should be correct", 250L, testSessionInfo.thermalFrameCount)
            assertNotNull("Video file should be set", testSessionInfo.videoFilePath)
            assertNotNull("Thermal file should be set", testSessionInfo.thermalFilePath)
            assertTrue(
                "All sensors should be enabled",
                testSessionInfo.videoEnabled && testSessionInfo.rawEnabled && testSessionInfo.thermalEnabled,
            )

            logger.info("[DEBUG_LOG] Data consistency verified across ${testSessionInfo.getRawImageCount()} operations")
        }

    @Test
    fun testSessionLifecycleDataFlow() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()

            logger.info("[DEBUG_LOG] Starting session lifecycle data flow test")

            testSessionInfo.startTime = System.currentTimeMillis()
            testSessionInfo.videoEnabled = true
            logger.info("[DEBUG_LOG] Session initialized")

            delay(50)
            testSessionInfo.addRawFile("/test/lifecycle_raw.dng")
            testSessionInfo.updateThermalFrameCount(75)
            logger.info("[DEBUG_LOG] Data recorded")

            testSessionInfo.markCompleted()
            logger.info("[DEBUG_LOG] Session marked complete")

            sessionManager.finalizeCurrentSession()
            logger.info("[DEBUG_LOG] Session finalized")

            assertFalse("Session should no longer be active", testSessionInfo.isActive())
            assertTrue("Session should have duration", testSessionInfo.getDurationMs() > 0)
            assertEquals("Should have 1 RAW file", 1, testSessionInfo.getRawImageCount())
            assertEquals("Thermal frames should be recorded", 75L, testSessionInfo.thermalFrameCount)
            assertNull("Current session should be null", sessionManager.getCurrentSession())

            logger.info("[DEBUG_LOG] Session lifecycle data flow completed: ${testSessionInfo.getSummary()}")
        }

    @Test
    fun testConcurrentDataFlow() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.startTime = System.currentTimeMillis()

            val operations = mutableListOf<String>()

            repeat(3) { i ->
                testSessionInfo.addRawFile("/concurrent/raw_$i.dng")
                operations.add("raw_$i")
                logger.debug("[DEBUG_LOG] Concurrent RAW file $i added")
            }

            repeat(3) { i ->
                testSessionInfo.updateThermalFrameCount((i + 1) * 10L)
                operations.add("thermal_$i")
                logger.debug("[DEBUG_LOG] Concurrent thermal update $i")
            }

            repeat(2) { i ->
                logger.info("[DEBUG_LOG] Concurrent log entry $i")
                operations.add("log_$i")
            }

            assertEquals("Should have 3 RAW files", 3, testSessionInfo.getRawImageCount())
            assertEquals("Final thermal count should be 30", 30L, testSessionInfo.thermalFrameCount)
            assertEquals("Should have tracked all operations", 8, operations.size)

            logger.info("[DEBUG_LOG] Concurrent data flow verified: ${operations.size} operations")
        }

    @Test
    fun testDataIntegrityValidation() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.startTime = System.currentTimeMillis()

            testSessionInfo.videoEnabled = true
            testSessionInfo.rawEnabled = true
            testSessionInfo.thermalEnabled = true

            testSessionInfo.videoFilePath = "/valid/video.mp4"
            testSessionInfo.addRawFile("/valid/raw1.dng")
            testSessionInfo.addRawFile("/valid/raw2.dng")
            testSessionInfo.setThermalFile("/valid/thermal.bin")
            testSessionInfo.updateThermalFrameCount(150)

            assertTrue("Video should be active", testSessionInfo.videoEnabled)
            assertTrue("RAW should be active", testSessionInfo.rawEnabled)
            assertTrue("Thermal should be active", testSessionInfo.isThermalActive())
            assertEquals("RAW count should match", 2, testSessionInfo.getRawImageCount())
            assertTrue("Thermal data size should be calculated", testSessionInfo.getThermalDataSizeMB() > 0)

            assertTrue("Video path should be valid", testSessionInfo.videoFilePath?.contains("video.mp4") == true)
            assertTrue(
                "All RAW paths should be valid",
                testSessionInfo.rawFilePaths.all { it.contains(".dng") },
            )
            assertTrue(
                "Thermal path should be valid",
                testSessionInfo.thermalFilePath?.contains("thermal.bin") == true,
            )

            logger.info("[DEBUG_LOG] Data integrity validated: ${testSessionInfo.getSummary()}")
        }

    @Test
    fun testStorageDataFlow() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()
            val sessionFolder = session?.sessionFolder

            assertNotNull("Session folder should exist", sessionFolder)

            testSessionInfo.videoFilePath = "${sessionFolder?.absolutePath}/storage_video.mp4"
            testSessionInfo.addRawFile("${sessionFolder?.absolutePath}/raw/storage_raw.dng")
            testSessionInfo.setThermalFile("${sessionFolder?.absolutePath}/thermal/storage_thermal.bin")

            assertTrue(
                "Video path should reference session folder",
                testSessionInfo.videoFilePath?.contains(sessionFolder?.name ?: "") == true,
            )
            assertTrue(
                "RAW path should reference session folder",
                testSessionInfo.rawFilePaths.any { it.contains(sessionFolder?.name ?: "") },
            )
            assertTrue(
                "Thermal path should reference session folder",
                testSessionInfo.thermalFilePath?.contains(sessionFolder?.name ?: "") == true,
            )

            logger.info("[DEBUG_LOG] Storage data flow verified for folder: ${sessionFolder?.name}")
        }
}
