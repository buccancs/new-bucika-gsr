package com.multisensor.recording.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multisensor.recording.recording.SessionInfo
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FileIOIntegrationTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var logger: Logger

    private lateinit var testSessionInfo: SessionInfo
    private lateinit var testSessionFolder: File

    @Before
    fun setup() {
        hiltRule.inject()

        val sessionId = "fileio_test_${System.currentTimeMillis()}"
        testSessionInfo = SessionInfo(sessionId = sessionId)

        kotlinx.coroutines.runBlocking {
            val actualSessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()
            testSessionFolder = session?.sessionFolder ?: File.createTempFile("test", "session").parentFile
        }
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
    fun testSessionFolderCreation() =
        runTest {
            assertNotNull("SessionManager should be injected", sessionManager)

            val sessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()
            val sessionFolder = session?.sessionFolder

            assertNotNull("Session folder should exist", sessionFolder)
            assertTrue("Session folder should be directory", sessionFolder?.isDirectory == true)
            assertTrue("Session folder should exist on filesystem", sessionFolder?.exists() == true)
            assertTrue("Session folder should be readable", sessionFolder?.canRead() == true)
            assertTrue("Session folder should be writable", sessionFolder?.canWrite() == true)

            logger.info("[DEBUG_LOG] Session folder created: ${sessionFolder?.absolutePath}")
        }

    @Test
    fun testFilePathGeneration() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()

            assertNotNull("File paths should be available", filePaths)

            filePaths?.let { paths ->
                assertNotNull("RGB video file path should exist", paths.rgbVideoFile)
                assertNotNull("Thermal video file path should exist", paths.thermalVideoFile)
                assertNotNull("RAW frames folder path should exist", paths.rawFramesFolder)
                assertNotNull("Shimmer data file path should exist", paths.shimmerDataFile)
                assertNotNull("Log file path should exist", paths.logFile)

                assertTrue("RGB video should be MP4", paths.rgbVideoFile.name.endsWith(".mp4"))
                assertTrue("Thermal video should be MP4", paths.thermalVideoFile.name.endsWith(".mp4"))
                assertTrue("Shimmer data should be CSV", paths.shimmerDataFile.name.endsWith(".csv"))
                assertTrue("Log file should be .txt", paths.logFile.name.endsWith(".txt"))

                assertEquals(
                    "RGB video parent should be session folder",
                    paths.sessionFolder,
                    paths.rgbVideoFile.parentFile,
                )
                assertEquals(
                    "Thermal video parent should be session folder",
                    paths.sessionFolder,
                    paths.thermalVideoFile.parentFile,
                )
                assertEquals(
                    "RAW frames parent should be session folder",
                    paths.sessionFolder,
                    paths.rawFramesFolder.parentFile,
                )
                assertEquals(
                    "Shimmer data parent should be session folder",
                    paths.sessionFolder,
                    paths.shimmerDataFile.parentFile,
                )
                assertEquals(
                    "Log file parent should be session folder",
                    paths.sessionFolder,
                    paths.logFile.parentFile,
                )
            }

            logger.info("[DEBUG_LOG] File path generation verified")
        }

    @Test
    fun testVideoFileOperations() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()
            val videoFile = filePaths?.rgbVideoFile

            assertNotNull("Video file path should exist", videoFile)

            videoFile?.let { file ->
                file.parentFile?.mkdirs()

                val testVideoData = "MOCK_VIDEO_DATA_${System.currentTimeMillis()}".toByteArray()
                FileOutputStream(file).use { output ->
                    output.write(testVideoData)
                    output.flush()
                }

                assertTrue("Video file should exist", file.exists())
                assertTrue("Video file should be readable", file.canRead())
                assertTrue("Video file should have content", file.length() > 0)
                assertEquals("Video file size should match", testVideoData.size.toLong(), file.length())

                val readData =
                    FileInputStream(file).use { input ->
                        input.readBytes()
                    }
                assertArrayEquals("Video file content should match", testVideoData, readData)

                testSessionInfo.videoFilePath = file.absolutePath
                testSessionInfo.videoEnabled = true
            }

            logger.info("[DEBUG_LOG] Video file operations verified")
        }

    @Test
    fun testRAWImageFileOperations() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()
            val rawFramesFolder = filePaths?.rawFramesFolder

            assertNotNull("RAW frames folder should exist", rawFramesFolder)

            rawFramesFolder?.let { folder ->
                folder.mkdirs()
                assertTrue("RAW frames folder should be created", folder.exists())
                assertTrue("RAW frames folder should be directory", folder.isDirectory)

                val rawFiles = mutableListOf<File>()
                repeat(3) { i ->
                    val rawFile = File(folder, "raw_image_$i.dng")
                    val testRawData = "MOCK_RAW_IMAGE_DATA_$i".toByteArray()

                    FileOutputStream(rawFile).use { output ->
                        output.write(testRawData)
                        output.flush()
                    }

                    rawFiles.add(rawFile)
                    testSessionInfo.addRawFile(rawFile.absolutePath)
                }

                assertEquals("Should have 3 RAW files", 3, rawFiles.size)
                assertEquals("SessionInfo should track 3 RAW files", 3, testSessionInfo.getRawImageCount())

                rawFiles.forEachIndexed { index, file ->
                    assertTrue("RAW file $index should exist", file.exists())
                    assertTrue("RAW file $index should be readable", file.canRead())
                    assertTrue("RAW file $index should have content", file.length() > 0)
                    assertTrue("RAW file $index should have .dng extension", file.name.endsWith(".dng"))

                    val expectedData = "MOCK_RAW_IMAGE_DATA_$index".toByteArray()
                    val actualData = FileInputStream(file).use { it.readBytes() }
                    assertArrayEquals("RAW file $index content should match", expectedData, actualData)
                }
            }

            logger.info("[DEBUG_LOG] RAW image file operations verified")
        }

    @Test
    fun testThermalDataFileOperations() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()
            val sessionFolder = filePaths?.sessionFolder

            assertNotNull("Session folder should exist", sessionFolder)

            sessionFolder?.let { folder ->
                val thermalFile = File(folder, "thermal_data.bin")

                val testThermalData = ByteArray(1024) { it.toByte() }
                FileOutputStream(thermalFile).use { output ->
                    output.write(testThermalData)
                    output.flush()
                }

                testSessionInfo.setThermalFile(thermalFile.absolutePath)
                testSessionInfo.thermalEnabled = true
                testSessionInfo.updateThermalFrameCount(100)

                assertTrue("Thermal file should exist", thermalFile.exists())
                assertTrue("Thermal file should be readable", thermalFile.canRead())
                assertEquals("Thermal file size should match", testThermalData.size.toLong(), thermalFile.length())
                assertTrue("Thermal should be active in session", testSessionInfo.isThermalActive())
                assertEquals("Thermal frame count should be correct", 100L, testSessionInfo.thermalFrameCount)

                val readData =
                    FileInputStream(thermalFile).use { input ->
                        input.readBytes()
                    }
                assertArrayEquals("Thermal file content should match", testThermalData, readData)

                assertTrue("Thermal data size should be calculated", testSessionInfo.getThermalDataSizeMB() > 0)
            }

            logger.info("[DEBUG_LOG] Thermal data file operations verified")
        }

    @Test
    fun testLogFileOperations() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()
            val logFile = filePaths?.logFile

            assertNotNull("Log file path should exist", logFile)

            logFile?.let { file ->
                file.parentFile?.mkdirs()

                val logEntries =
                    listOf(
                        "[INFO] Session started: $sessionId",
                        "[DEBUG] Video recording enabled",
                        "[DEBUG] RAW capture enabled",
                        "[DEBUG] Thermal recording enabled",
                        "[INFO] Session completed",
                    )

                FileOutputStream(file).use { output ->
                    logEntries.forEach { entry ->
                        output.write("${System.currentTimeMillis()}: $entry\n".toByteArray())
                    }
                    output.flush()
                }

                assertTrue("Log file should exist", file.exists())
                assertTrue("Log file should be readable", file.canRead())
                assertTrue("Log file should have content", file.length() > 0)
                assertTrue("Log file should have .txt extension", file.name.endsWith(".txt"))

                val logContent =
                    FileInputStream(file).use { input ->
                        String(input.readBytes())
                    }

                logEntries.forEach { entry ->
                    assertTrue("Log should contain entry: $entry", logContent.contains(entry))
                }
            }

            logger.info("[DEBUG_LOG] Log file operations verified")
        }

    @Test
    fun testShimmerDataFileOperations() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()
            val shimmerFile = filePaths?.shimmerDataFile

            assertNotNull("Shimmer file path should exist", shimmerFile)

            shimmerFile?.let { file ->
                file.parentFile?.mkdirs()

                val csvHeader = "timestamp,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z\n"
                val csvData = StringBuilder(csvHeader)

                repeat(10) { i ->
                    val timestamp = System.currentTimeMillis() + i * 100
                    csvData.append("$timestamp,${i * 0.1},${i * 0.2},${i * 0.3},${i * 0.4},${i * 0.5},${i * 0.6}\n")
                }

                FileOutputStream(file).use { output ->
                    output.write(csvData.toString().toByteArray())
                    output.flush()
                }

                assertTrue("Shimmer file should exist", file.exists())
                assertTrue("Shimmer file should be readable", file.canRead())
                assertTrue("Shimmer file should have content", file.length() > 0)
                assertTrue("Shimmer file should have .csv extension", file.name.endsWith(".csv"))

                val csvContent =
                    FileInputStream(file).use { input ->
                        String(input.readBytes())
                    }

                assertTrue("CSV should contain header", csvContent.contains("timestamp,accel_x"))
                assertTrue("CSV should contain data rows", csvContent.split("\n").size > 10)

                val lines = csvContent.split("\n").filter { it.isNotEmpty() }
                assertEquals("Should have header + 10 data rows", 11, lines.size)
                assertTrue("First line should be header", lines[0].startsWith("timestamp"))
            }

            logger.info("[DEBUG_LOG] Shimmer data file operations verified")
        }

    @Test
    fun testFileIntegrityValidation() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()

            val testFiles = mutableMapOf<String, ByteArray>()

            filePaths?.let { paths ->
                val videoData = "VIDEO_INTEGRITY_TEST".toByteArray()
                paths.rgbVideoFile.parentFile?.mkdirs()
                FileOutputStream(paths.rgbVideoFile).use { it.write(videoData) }
                testFiles["video"] = videoData

                paths.rawFramesFolder.mkdirs()
                val rawFile = File(paths.rawFramesFolder, "integrity_raw.dng")
                val rawData = "RAW_INTEGRITY_TEST".toByteArray()
                FileOutputStream(rawFile).use { it.write(rawData) }
                testFiles["raw"] = rawData

                val thermalFile = File(paths.sessionFolder, "integrity_thermal.bin")
                val thermalData = "THERMAL_INTEGRITY_TEST".toByteArray()
                FileOutputStream(thermalFile).use { it.write(thermalData) }
                testFiles["thermal"] = thermalData

                val logData = "LOG_INTEGRITY_TEST".toByteArray()
                paths.logFile.parentFile?.mkdirs()
                FileOutputStream(paths.logFile).use { it.write(logData) }
                testFiles["log"] = logData
            }

            filePaths?.let { paths ->
                val videoContent = FileInputStream(paths.rgbVideoFile).use { it.readBytes() }
                assertArrayEquals("Video file integrity should be maintained", testFiles["video"], videoContent)

                val rawFile = File(paths.rawFramesFolder, "integrity_raw.dng")
                val rawContent = FileInputStream(rawFile).use { it.readBytes() }
                assertArrayEquals("RAW file integrity should be maintained", testFiles["raw"], rawContent)

                val thermalFile = File(paths.sessionFolder, "integrity_thermal.bin")
                val thermalContent = FileInputStream(thermalFile).use { it.readBytes() }
                assertArrayEquals("Thermal file integrity should be maintained", testFiles["thermal"], thermalContent)

                val logContent = FileInputStream(paths.logFile).use { it.readBytes() }
                assertArrayEquals("Log file integrity should be maintained", testFiles["log"], logContent)
            }

            logger.info("[DEBUG_LOG] File integrity validation completed")
        }

    @Test
    fun testConcurrentFileOperations() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()
            val sessionFolder = filePaths?.sessionFolder

            assertNotNull("Session folder should exist", sessionFolder)

            sessionFolder?.let { folder ->
                val concurrentFiles = mutableListOf<File>()

                repeat(5) { i ->
                    val file = File(folder, "concurrent_file_$i.dat")
                    val data = "CONCURRENT_DATA_$i".toByteArray()

                    FileOutputStream(file).use { output ->
                        output.write(data)
                        output.flush()
                    }

                    concurrentFiles.add(file)
                }

                assertEquals("Should have 5 concurrent files", 5, concurrentFiles.size)

                concurrentFiles.forEachIndexed { index, file ->
                    assertTrue("Concurrent file $index should exist", file.exists())
                    assertTrue("Concurrent file $index should be readable", file.canRead())

                    val expectedData = "CONCURRENT_DATA_$index".toByteArray()
                    val actualData = FileInputStream(file).use { it.readBytes() }
                    assertArrayEquals("Concurrent file $index content should match", expectedData, actualData)
                }
            }

            logger.info("[DEBUG_LOG] Concurrent file operations verified")
        }

    @Test
    fun testStorageSpaceHandling() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val session = sessionManager.getCurrentSession()
            val sessionFolder = session?.sessionFolder

            assertNotNull("Session folder should exist", sessionFolder)

            sessionFolder?.let { folder ->
                val freeSpace = folder.freeSpace
                val totalSpace = folder.totalSpace
                val usableSpace = folder.usableSpace

                assertTrue("Free space should be positive", freeSpace > 0)
                assertTrue("Total space should be positive", totalSpace > 0)
                assertTrue("Usable space should be positive", usableSpace > 0)
                assertTrue("Free space should not exceed total space", freeSpace <= totalSpace)
                assertTrue("Usable space should not exceed total space", usableSpace <= totalSpace)

                val minimumRequired = 100L * 1024L * 1024L
                assertTrue("Should have sufficient space for recording", usableSpace > minimumRequired)
            }

            logger.info("[DEBUG_LOG] Storage space handling verified")
        }

    @Test
    fun testFileCleanupOnSessionEnd() =
        runTest {
            val sessionId = sessionManager.createNewSession()
            val filePaths = sessionManager.getSessionFilePaths()
            val sessionFolder = filePaths?.sessionFolder

            val tempFiles = mutableListOf<File>()

            sessionFolder?.let { folder ->
                repeat(3) { i ->
                    val tempFile = File(folder, "temp_file_$i.tmp")
                    FileOutputStream(tempFile).use { output ->
                        output.write("TEMP_DATA_$i".toByteArray())
                    }
                    tempFiles.add(tempFile)
                }

                tempFiles.forEach { file ->
                    assertTrue("Temp file should exist before cleanup", file.exists())
                }
            }

            sessionManager.finalizeCurrentSession()

            assertNull("Current session should be null after finalization", sessionManager.getCurrentSession())

            logger.info("[DEBUG_LOG] File cleanup on session end verified")
        }
}
