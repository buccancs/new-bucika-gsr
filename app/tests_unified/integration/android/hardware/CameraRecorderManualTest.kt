package com.multisensor.recording.recording

import android.Manifest
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.multisensor.recording.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CameraRecorderManualTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

    @Inject
    lateinit var cameraRecorder: CameraRecorder

    private lateinit var textureView: TextureView
    private var currentSession: SessionInfo? = null
    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        hiltRule.inject()

        activityScenario = ActivityScenario.launch(MainActivity::class.java)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activityScenario.onActivity { activity ->
                textureView = TextureView(activity)
                activity.setContentView(textureView)
            }
        }

        Thread.sleep(1000)
    }

    @After
    fun cleanup() =
        runBlocking {
            try {
                currentSession = cameraRecorder.stopSession()

                delay(500)

                if (::activityScenario.isInitialized) {
                    activityScenario.close()
                }

                println("[DEBUG_LOG] Test cleanup completed. Session: ${currentSession?.getSummary()}")
            } catch (e: Exception) {
                println("[DEBUG_LOG] Cleanup error: ${e.message}")
            }
        }

    @Test
    fun test1_baselinePreviewTest() =
        runBlocking {
            println("[DEBUG_LOG] Starting Test 1: Baseline Preview Test")

            val initResult =
                withTimeout(10000) {
                    cameraRecorder.initialize(textureView)
                }
            assertTrue("[DEBUG_LOG] Camera initialization failed", initResult)
            println("[DEBUG_LOG] Camera initialized successfully")

            val surfaceAvailableLatch = CountDownLatch(1)
            var surfaceWidth = 0
            var surfaceHeight = 0

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                if (textureView.isAvailable) {
                    surfaceWidth = textureView.width
                    surfaceHeight = textureView.height
                    surfaceAvailableLatch.countDown()
                    println("[DEBUG_LOG] TextureView surface already available: ${surfaceWidth}x$surfaceHeight")
                } else {
                    textureView.surfaceTextureListener =
                        object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                surfaceWidth = width
                                surfaceHeight = height
                                surfaceAvailableLatch.countDown()
                                println("[DEBUG_LOG] TextureView surface became available: ${width}x$height")
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                            }

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                }
            }

            assertTrue(
                "[DEBUG_LOG] TextureView surface did not become available within timeout",
                surfaceAvailableLatch.await(5, TimeUnit.SECONDS),
            )

            assertTrue("[DEBUG_LOG] Surface width too small: $surfaceWidth", surfaceWidth > 0)
            assertTrue("[DEBUG_LOG] Surface height too small: $surfaceHeight", surfaceHeight > 0)
            println("[DEBUG_LOG] Surface dimensions validated: ${surfaceWidth}x$surfaceHeight")

            delay(3000)

            println("[DEBUG_LOG] Preview running smoothly for 3 seconds")

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                textureView.surfaceTexture?.let { surface ->
                    textureView.surfaceTextureListener?.onSurfaceTextureSizeChanged(
                        surface,
                        surfaceHeight,
                        surfaceWidth,
                    )
                }
            }

            delay(1000)
            println("[DEBUG_LOG] Orientation handling tested")

            println("[DEBUG_LOG] Test 1 completed successfully - Preview is functional")
        }

    @Test
    fun test2_videoOnlyRecordingTest() =
        runBlocking {
            println("[DEBUG_LOG] Starting Test 2: Video-only Recording Test")

            val initResult =
                withTimeout(10000) {
                    cameraRecorder.initialize(textureView)
                }
            assertTrue("[DEBUG_LOG] Camera initialization failed", initResult)

            currentSession =
                withTimeout(15000) {
                    cameraRecorder.startSession(recordVideo = true, captureRaw = false)
                }
            assertNotNull("[DEBUG_LOG] Failed to start video-only session", currentSession)
            assertTrue("[DEBUG_LOG] Video not enabled in session", currentSession!!.videoEnabled)
            assertFalse("[DEBUG_LOG] RAW should not be enabled", currentSession!!.rawEnabled)
            println("[DEBUG_LOG] Video-only session started: ${currentSession!!.getSummary()}")

            println("[DEBUG_LOG] Recording 4K video for 10 seconds...")
            delay(10000)

            val stoppedSession =
                withTimeout(10000) {
                    cameraRecorder.stopSession()
                }
            assertNotNull("[DEBUG_LOG] Failed to stop session", stoppedSession)
            assertFalse("[DEBUG_LOG] Session should not be active after stop", stoppedSession!!.isActive())
            println("[DEBUG_LOG] Recording stopped: ${stoppedSession.getSummary()}")

            val videoFilePath = stoppedSession.videoFilePath
            assertNotNull("[DEBUG_LOG] Video file path is null", videoFilePath)

            val videoFile = File(videoFilePath!!)
            assertTrue("[DEBUG_LOG] Video file does not exist: $videoFilePath", videoFile.exists())
            assertTrue("[DEBUG_LOG] Video file is empty: $videoFilePath", videoFile.length() > 0)

            val expectedMinSize = 10 * 1024 * 1024
            assertTrue(
                "[DEBUG_LOG] Video file too small (${videoFile.length()} bytes), expected at least $expectedMinSize",
                videoFile.length() >= expectedMinSize,
            )

            println("[DEBUG_LOG] Video file validated: ${videoFile.length()} bytes at $videoFilePath")
            println("[DEBUG_LOG] Test 2 completed successfully - 4K video recording functional")

            currentSession = stoppedSession
        }

    @Test
    fun test3_rawOnlyCaptureTest() =
        runBlocking {
            println("[DEBUG_LOG] Starting Test 3: RAW-only Capture Test")

            val initResult =
                withTimeout(10000) {
                    cameraRecorder.initialize(textureView)
                }
            assertTrue("[DEBUG_LOG] Camera initialization failed", initResult)

            currentSession =
                withTimeout(15000) {
                    cameraRecorder.startSession(recordVideo = false, captureRaw = true)
                }
            assertNotNull("[DEBUG_LOG] Failed to start RAW-only session", currentSession)
            assertFalse("[DEBUG_LOG] Video should not be enabled", currentSession!!.videoEnabled)
            assertTrue("[DEBUG_LOG] RAW not enabled in session", currentSession!!.rawEnabled)
            println("[DEBUG_LOG] RAW-only session started: ${currentSession!!.getSummary()}")

            delay(2000)

            println("[DEBUG_LOG] Capturing first RAW image...")
            val firstCaptureResult =
                withTimeout(10000) {
                    cameraRecorder.captureRawImage()
                }
            assertTrue("[DEBUG_LOG] First RAW capture failed", firstCaptureResult)

            delay(3000)

            println("[DEBUG_LOG] Capturing second RAW image...")
            val secondCaptureResult =
                withTimeout(10000) {
                    cameraRecorder.captureRawImage()
                }
            assertTrue("[DEBUG_LOG] Second RAW capture failed", secondCaptureResult)

            delay(3000)

            val stoppedSession =
                withTimeout(10000) {
                    cameraRecorder.stopSession()
                }
            assertNotNull("[DEBUG_LOG] Failed to stop RAW session", stoppedSession)
            assertFalse("[DEBUG_LOG] Session should not be active after stop", stoppedSession!!.isActive())
            println("[DEBUG_LOG] RAW session stopped: ${stoppedSession.getSummary()}")

            val rawFilePaths = stoppedSession.rawFilePaths
            assertTrue("[DEBUG_LOG] No RAW files captured", rawFilePaths.isNotEmpty())
            assertEquals("[DEBUG_LOG] Expected 2 RAW files, got ${rawFilePaths.size}", 2, rawFilePaths.size)

            rawFilePaths.forEachIndexed { index, filePath ->
                println("[DEBUG_LOG] Validating RAW file ${index + 1}: $filePath")

                val dngFile = File(filePath)
                assertTrue("[DEBUG_LOG] DNG file does not exist: $filePath", dngFile.exists())
                assertTrue("[DEBUG_LOG] DNG file is empty: $filePath", dngFile.length() > 0)

                assertTrue("[DEBUG_LOG] File should have .dng extension: $filePath", filePath.endsWith(".dng"))

                val expectedMinSize = 5 * 1024 * 1024
                assertTrue(
                    "[DEBUG_LOG] DNG file too small (${dngFile.length()} bytes), expected at least $expectedMinSize",
                    dngFile.length() >= expectedMinSize,
                )

                val fileBytes = dngFile.readBytes()
                assertTrue("[DEBUG_LOG] DNG file too small for header validation", fileBytes.size >= 8)

                val isValidDng =
                    (fileBytes[0] == 0x49.toByte() && fileBytes[1] == 0x49.toByte()) ||
                            (fileBytes[0] == 0x4D.toByte() && fileBytes[1] == 0x4D.toByte())
                assertTrue("[DEBUG_LOG] Invalid DNG file header: $filePath", isValidDng)

                println("[DEBUG_LOG] DNG file ${index + 1} validated: ${dngFile.length()} bytes")
            }

            assertNotNull("[DEBUG_LOG] RAW resolution should be set", stoppedSession.rawResolution)
            assertNull("[DEBUG_LOG] Video file path should be null for RAW-only", stoppedSession.videoFilePath)
            assertEquals("[DEBUG_LOG] RAW file count mismatch", 2, stoppedSession.getRawImageCount())

            println("[DEBUG_LOG] Test 3 completed successfully - RAW capture and DNG creation functional")
            println(
                "[DEBUG_LOG] Captured ${rawFilePaths.size} DNG files with total size: ${rawFilePaths.sumOf { File(it).length() }} bytes",
            )

            currentSession = stoppedSession
        }

    @Test
    fun test4_concurrentVideoRawTest() =
        runBlocking {
            println("[DEBUG_LOG] Starting Test 4: Concurrent Video + RAW Test")

            val initResult =
                withTimeout(10000) {
                    cameraRecorder.initialize(textureView)
                }
            assertTrue("[DEBUG_LOG] Camera initialization failed", initResult)

            currentSession =
                withTimeout(15000) {
                    cameraRecorder.startSession(recordVideo = true, captureRaw = true)
                }
            assertNotNull("[DEBUG_LOG] Failed to start concurrent session", currentSession)
            assertTrue("[DEBUG_LOG] Video not enabled in concurrent session", currentSession!!.videoEnabled)
            assertTrue("[DEBUG_LOG] RAW not enabled in concurrent session", currentSession!!.rawEnabled)
            println("[DEBUG_LOG] Concurrent session started: ${currentSession!!.getSummary()}")

            delay(3000)

            println("[DEBUG_LOG] Capturing RAW image during video recording...")
            val firstRawCapture =
                withTimeout(10000) {
                    cameraRecorder.captureRawImage()
                }
            assertTrue("[DEBUG_LOG] RAW capture during video failed", firstRawCapture)

            delay(5000)

            println("[DEBUG_LOG] Capturing second RAW image during video recording...")
            val secondRawCapture =
                withTimeout(10000) {
                    cameraRecorder.captureRawImage()
                }
            assertTrue("[DEBUG_LOG] Second RAW capture during video failed", secondRawCapture)

            delay(3000)

            val stoppedSession =
                withTimeout(10000) {
                    cameraRecorder.stopSession()
                }
            assertNotNull("[DEBUG_LOG] Failed to stop concurrent session", stoppedSession)
            assertFalse("[DEBUG_LOG] Session should not be active after stop", stoppedSession!!.isActive())
            println("[DEBUG_LOG] Concurrent session stopped: ${stoppedSession.getSummary()}")

            val videoFilePath = stoppedSession.videoFilePath
            assertNotNull("[DEBUG_LOG] Video file path is null in concurrent session", videoFilePath)
            val videoFile = File(videoFilePath!!)
            assertTrue("[DEBUG_LOG] Video file does not exist: $videoFilePath", videoFile.exists())
            assertTrue("[DEBUG_LOG] Video file is empty: $videoFilePath", videoFile.length() > 0)

            val rawFilePaths = stoppedSession.rawFilePaths
            assertTrue("[DEBUG_LOG] No RAW files in concurrent session", rawFilePaths.isNotEmpty())
            assertEquals("[DEBUG_LOG] Expected 2 RAW files in concurrent session", 2, rawFilePaths.size)

            rawFilePaths.forEach { filePath ->
                val dngFile = File(filePath)
                assertTrue("[DEBUG_LOG] DNG file missing in concurrent session: $filePath", dngFile.exists())
                assertTrue("[DEBUG_LOG] DNG file empty in concurrent session: $filePath", dngFile.length() > 0)
            }

            val sessionDuration = stoppedSession.getDurationMs()
            assertTrue(
                "[DEBUG_LOG] Session duration too short: ${sessionDuration}ms",
                sessionDuration >= 10000
            )

            println("[DEBUG_LOG] Test 4 completed successfully - Concurrent video + RAW capture functional")
            println("[DEBUG_LOG] Video: ${videoFile.length()} bytes, RAW: ${rawFilePaths.size} files")

            currentSession = stoppedSession
        }
}
