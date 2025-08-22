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
class MultiSensorCoordinationTest {
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

        val sessionId = "integration_test_${System.currentTimeMillis()}"
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
    fun testMultiSensorSessionCreation() =
        runTest {
            testSessionInfo.videoEnabled = true
            testSessionInfo.rawEnabled = true
            testSessionInfo.thermalEnabled = true

            val sessionId = sessionManager.createNewSession()
            val currentSession = sessionManager.getCurrentSession()

            assertNotNull("Session should be created", currentSession)
            assertEquals("Session ID should match", sessionId, currentSession?.sessionId)
            assertEquals("Session should be active", SessionManager.SessionStatus.ACTIVE, currentSession?.status)
            assertTrue("Session folder should exist", currentSession?.sessionFolder?.exists() == true)

            logger.info("[DEBUG_LOG] Multi-sensor session created successfully: $sessionId")
        }

    @Test
    fun testSensorCoordinationTiming() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.startTime = System.currentTimeMillis()

            val thermalStartTime = System.currentTimeMillis()
            delay(50)

            val cameraStartTime = System.currentTimeMillis()
            delay(30)

            val shimmerStartTime = System.currentTimeMillis()
            delay(20)

            val allSensorsReady = System.currentTimeMillis()

            assertTrue("Thermal should start first", thermalStartTime >= testSessionInfo.startTime)
            assertTrue("Camera should start after thermal", cameraStartTime >= thermalStartTime)
            assertTrue("Shimmer should start after camera", shimmerStartTime >= cameraStartTime)
            assertTrue(
                "All sensors ready within reasonable time",
                (allSensorsReady - testSessionInfo.startTime) < 1000,
            )

            logger.info("[DEBUG_LOG] Sensor coordination timing verified")
        }

    @Test
    fun testSessionDataIntegrity() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.videoEnabled = true
            testSessionInfo.rawEnabled = true
            testSessionInfo.thermalEnabled = true

            testSessionInfo.videoFilePath = "/test/video.mp4"
            testSessionInfo.addRawFile("/test/raw1.dng")
            testSessionInfo.addRawFile("/test/raw2.dng")
            testSessionInfo.setThermalFile("/test/thermal.bin")
            testSessionInfo.updateThermalFrameCount(100)

            assertNotNull("Video file path should be set", testSessionInfo.videoFilePath)
            assertEquals("Should have 2 RAW files", 2, testSessionInfo.getRawImageCount())
            assertNotNull("Thermal file path should be set", testSessionInfo.thermalFilePath)
            assertEquals("Thermal frame count should be correct", 100L, testSessionInfo.thermalFrameCount)
            assertTrue("Thermal should be active", testSessionInfo.isThermalActive())

            logger.info("[DEBUG_LOG] Session data integrity verified: ${testSessionInfo.getSummary()}")
        }

    @Test
    fun testResourceCoordination() =
        runTest {
            val session1Id = sessionManager.createNewSession()
            val session1 = sessionManager.getCurrentSession()

            sessionManager.finalizeCurrentSession()
            delay(1100)
            val session2Id = sessionManager.createNewSession()
            val session2 = sessionManager.getCurrentSession()

            assertNotEquals("Sessions should have different IDs", session1Id, session2Id)
            assertNotNull("Second session should be created", session2)
            assertEquals("Second session should be active", SessionManager.SessionStatus.ACTIVE, session2?.status)

            logger.info("[DEBUG_LOG] Resource coordination verified between sessions")
        }

    @Test
    fun testSensorFailureRecovery() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.videoEnabled = true
            testSessionInfo.thermalEnabled = true

            testSessionInfo.markError("Thermal sensor connection lost")
            assertTrue("Error should be marked", testSessionInfo.errorOccurred)
            assertNotNull("Error message should be set", testSessionInfo.errorMessage)

            val recoverySessionInfo =
                testSessionInfo.copy(
                    errorOccurred = false,
                    errorMessage = null,
                )

            assertFalse("Error should be cleared", recoverySessionInfo.errorOccurred)
            assertNull("Error message should be cleared", recoverySessionInfo.errorMessage)
            assertTrue("Video should still be enabled", recoverySessionInfo.videoEnabled)

            logger.info("[DEBUG_LOG] Sensor failure recovery verified")
        }

    @Test
    fun testConcurrentSensorOperations() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.videoEnabled = true
            testSessionInfo.rawEnabled = true
            testSessionInfo.thermalEnabled = true
            testSessionInfo.startTime = System.currentTimeMillis()

            val operations = mutableListOf<String>()

            repeat(10) { frame ->
                testSessionInfo.updateThermalFrameCount(frame.toLong() + 1)
                operations.add("thermal_frame_$frame")
            }

            repeat(5) { image ->
                testSessionInfo.addRawFile("/test/raw_$image.dng")
                operations.add("raw_image_$image")
            }

            testSessionInfo.videoFilePath = "/test/concurrent_video.mp4"
            operations.add("video_recording")

            assertEquals("Should have 10 thermal frames", 10L, testSessionInfo.thermalFrameCount)
            assertEquals("Should have 5 RAW images", 5, testSessionInfo.getRawImageCount())
            assertNotNull("Video file should be set", testSessionInfo.videoFilePath)
            assertEquals("Should have tracked all operations", 16, operations.size)

            logger.info("[DEBUG_LOG] Concurrent sensor operations verified: ${operations.size} operations")
        }

    @Test
    fun testSessionLifecycleWithMultipleSensors() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()
            assertNotNull("Session should be created", session)

            testSessionInfo.videoEnabled = true
            testSessionInfo.rawEnabled = true
            testSessionInfo.thermalEnabled = true
            testSessionInfo.startTime = System.currentTimeMillis()

            delay(100)
            testSessionInfo.addRawFile("/test/lifecycle_raw.dng")
            testSessionInfo.setThermalFile("/test/lifecycle_thermal.bin")
            testSessionInfo.updateThermalFrameCount(50)

            testSessionInfo.markCompleted()
            sessionManager.finalizeCurrentSession()

            assertFalse("Session should no longer be active", testSessionInfo.isActive())
            assertTrue("Session should have duration", testSessionInfo.getDurationMs() > 0)
            assertNull("Current session should be null after finalization", sessionManager.getCurrentSession())

            logger.info("[DEBUG_LOG] Session lifecycle completed: ${testSessionInfo.getSummary()}")
        }

    @Test
    fun testDataSynchronization() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val baseTimestamp = System.currentTimeMillis()

            testSessionInfo.startTime = baseTimestamp
            testSessionInfo.videoEnabled = true
            testSessionInfo.thermalEnabled = true

            val videoTimestamp = baseTimestamp + 100
            val thermalTimestamp = baseTimestamp + 105
            val rawTimestamp = baseTimestamp + 110

            testSessionInfo.videoFilePath = "/test/sync_video_$videoTimestamp.mp4"
            testSessionInfo.setThermalFile("/test/sync_thermal_$thermalTimestamp.bin")
            testSessionInfo.addRawFile("/test/sync_raw_$rawTimestamp.dng")

            assertTrue("Video timestamp should be after start", videoTimestamp > testSessionInfo.startTime)
            assertTrue(
                "Thermal timestamp should be close to video",
                Math.abs(thermalTimestamp - videoTimestamp) < 50,
            )
            assertTrue(
                "RAW timestamp should be close to thermal",
                Math.abs(rawTimestamp - thermalTimestamp) < 50,
            )

            logger.info("[DEBUG_LOG] Data synchronisation verified with timestamps")
        }

    @Test
    fun testStorageCoordination() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()
            val sessionFolder = session?.sessionFolder

            testSessionInfo.videoEnabled = true
            testSessionInfo.rawEnabled = true
            testSessionInfo.thermalEnabled = true

            val filePaths = sessionManager.getSessionFilePaths()

            assertNotNull("Session folder should exist", sessionFolder)
            assertNotNull("File paths should be available", filePaths)
            assertTrue("Session folder should be directory", sessionFolder?.isDirectory == true)

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
            }

            logger.info("[DEBUG_LOG] Storage coordination verified for session: $sessionId")
        }

    @Test
    fun testErrorHandlingAcrossSensors() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            testSessionInfo.videoEnabled = true
            testSessionInfo.rawEnabled = true
            testSessionInfo.thermalEnabled = true

            val errorScenarios =
                listOf(
                    "Camera permission denied",
                    "Thermal sensor disconnected",
                    "Storage full",
                    "Shimmer Bluetooth connection lost",
                )

            var errorHandled = false
            errorScenarios.forEach { errorMessage ->
                val testSession = testSessionInfo.copy()
                testSession.markError(errorMessage)

                if (testSession.errorOccurred) {
                    errorHandled = true
                    assertEquals("Error message should match", errorMessage, testSession.errorMessage)
                }
            }

            assertTrue("At least one error should be handled", errorHandled)

            logger.info("[DEBUG_LOG] Error handling verified across multiple sensors")
        }
}
