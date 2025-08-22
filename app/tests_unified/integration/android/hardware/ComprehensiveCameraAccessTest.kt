package com.multisensor.recording.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbManager
import android.view.TextureView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.multisensor.recording.MainActivity
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
class ComprehensiveCameraAccessTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )

    @Inject
    lateinit var cameraRecorder: CameraRecorder

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var thermalSettings: ThermalCameraSettings

    private lateinit var context: Context
    private lateinit var textureView: TextureView
    private lateinit var thermalRecorder: ThermalRecorder
    private var currentSession: SessionInfo? = null
    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext

        activityScenario = ActivityScenario.launch(MainActivity::class.java)

        thermalRecorder = ThermalRecorder(context, sessionManager, logger, thermalSettings)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activityScenario.onActivity { activity ->
                textureView = TextureView(activity)
                activity.setContentView(textureView)
            }
        }

        Thread.sleep(1000)

        println("[DEBUG_LOG] complete camera test setup complete")
    }

    @After
    fun cleanup() =
        runBlocking {
            try {
                currentSession = cameraRecorder.stopSession()
                thermalRecorder.cleanup()

                delay(500)

                if (::activityScenario.isInitialized) {
                    activityScenario.close()
                }

                println("[DEBUG_LOG] complete test cleanup completed")
            } catch (e: Exception) {
                println("[DEBUG_LOG] Cleanup error: ${e.message}")
            }
        }

    @Test
    fun test1_comprehensivePermissionVerification() {
        println("[DEBUG_LOG] Starting complete permission verification...")

        val requiredPermissions =
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )

        val permissionResults = mutableMapOf<String, Boolean>()

        for (permission in requiredPermissions) {
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            permissionResults[permission] = granted
            println("[DEBUG_LOG] Permission $permission: ${if (granted) "GRANTED" else "DENIED"}")
        }

        assertTrue("[DEBUG_LOG] CAMERA permission not granted", permissionResults[Manifest.permission.CAMERA] == true)
        assertTrue(
            "[DEBUG_LOG] RECORD_AUDIO permission not granted",
            permissionResults[Manifest.permission.RECORD_AUDIO] == true
        )
        assertTrue(
            "[DEBUG_LOG] WRITE_EXTERNAL_STORAGE permission not granted",
            permissionResults[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true,
        )

        println("[DEBUG_LOG] All required permissions verified successfully")
    }

    @Test
    fun test2_rgbCameraAccessAndFunctionality() =
        runBlocking {
            println("[DEBUG_LOG] Starting RGB camera access and functionality test...")

            val initResult =
                withTimeout(10000) {
                    cameraRecorder.initialize(textureView)
                }
            assertTrue("[DEBUG_LOG] RGB camera initialization failed", initResult)
            println("[DEBUG_LOG] RGB camera initialized successfully")

            val surfaceAvailableLatch = CountDownLatch(1)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                if (textureView.isAvailable) {
                    surfaceAvailableLatch.countDown()
                } else {
                    textureView.surfaceTextureListener =
                        object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                surfaceAvailableLatch.countDown()
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                            }

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                }
            }

            assertTrue(
                "[DEBUG_LOG] TextureView surface not available within timeout",
                surfaceAvailableLatch.await(10, TimeUnit.SECONDS),
            )

            delay(2000)
            println("[DEBUG_LOG] RGB camera preview active and stable")

            val sessionId = "rgb_test_${System.currentTimeMillis()}"
            currentSession = cameraRecorder.startSession(recordVideo = true, captureRaw = false)
            assertNotNull("[DEBUG_LOG] Failed to start RGB recording session", currentSession)
            println("[DEBUG_LOG] RGB recording session started: ${currentSession?.sessionId}")

            delay(5000)

            val finalSession = cameraRecorder.stopSession()
            assertNotNull("[DEBUG_LOG] Failed to stop RGB recording session", finalSession)
            println("[DEBUG_LOG] RGB recording completed. Duration: ${finalSession?.getDurationMs()}ms")

            currentSession = finalSession
        }

    @Test
    fun test3_irCameraRecognitionAndAccess() =
        runBlocking {
            println("[DEBUG_LOG] Starting IR camera recognition and access test...")

            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceList = usbManager.deviceList
            println("[DEBUG_LOG] Connected USB devices: ${deviceList.size}")

            for ((deviceName, device) in deviceList) {
                println("[DEBUG_LOG] USB Device: $deviceName")
                println("[DEBUG_LOG] - Vendor ID: ${device.vendorId}")
                println("[DEBUG_LOG] - Product ID: ${device.productId}")
                println("[DEBUG_LOG] - Device Name: ${device.deviceName}")
            }

            val thermalInitResult = thermalRecorder.initialize()
            println("[DEBUG_LOG] Thermal recorder initialization result: $thermalInitResult")

            println("[DEBUG_LOG] Waiting for IR camera detection (15 seconds)...")
            delay(15000)

            val status = thermalRecorder.getThermalCameraStatus()
            println("[DEBUG_LOG] IR Camera Status:")
            println("[DEBUG_LOG] - Available: ${status.isAvailable}")
            println("[DEBUG_LOG] - Device name: ${status.deviceName}")
            println("[DEBUG_LOG] - Width: ${status.width}")
            println("[DEBUG_LOG] - Height: ${status.height}")
            println("[DEBUG_LOG] - Frame rate: ${status.frameRate}")

            if (status.isAvailable) {
                println("[DEBUG_LOG] IR camera detected and recognized successfully!")

                val previewResult = thermalRecorder.startPreview()
                assertTrue("[DEBUG_LOG] Failed to start IR camera preview", previewResult)

                delay(3000)

                val previewStatus = thermalRecorder.getThermalCameraStatus()
                println("[DEBUG_LOG] IR Preview Status:")
                println("[DEBUG_LOG] - Preview active: ${previewStatus.isPreviewActive}")
                println("[DEBUG_LOG] - Frame count: ${previewStatus.frameCount}")

                thermalRecorder.stopPreview()
                println("[DEBUG_LOG] IR camera preview test completed successfully")
            } else {
                println("[DEBUG_LOG] WARNING: No IR camera detected. Please ensure:")
                println("[DEBUG_LOG] 1. Topdon thermal camera is connected via USB-C OTG")
                println("[DEBUG_LOG] 2. USB permissions were granted")
                println("[DEBUG_LOG] 3. Camera is a supported Topdon model (TC001/Plus)")

            }
        }

    @Test
    fun test4_fileWritingFunctionalityVerification() =
        runBlocking {
            println("[DEBUG_LOG] Starting file writing functionality verification...")

            val testSessionId = "file_test_${System.currentTimeMillis()}"
            val baseDir = File(context.getExternalFilesDir(null), "test_recordings")

            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            assertTrue("[DEBUG_LOG] Failed to create test directory", baseDir.exists())
            println("[DEBUG_LOG] Test directory created: ${baseDir.absolutePath}")

            println("[DEBUG_LOG] Testing RGB camera file writing...")

            val rgbInitResult = cameraRecorder.initialize(textureView)
            assertTrue("[DEBUG_LOG] RGB camera initialization failed", rgbInitResult)

            delay(2000)

            currentSession = cameraRecorder.startSession(recordVideo = true, captureRaw = true)
            assertNotNull("[DEBUG_LOG] Failed to start RGB recording session", currentSession)

            delay(3000)

            val finalSession = cameraRecorder.stopSession()
            assertNotNull("[DEBUG_LOG] Failed to stop RGB recording session", finalSession)

            val sessionDir = File(baseDir, finalSession?.sessionId ?: "unknown")
            if (sessionDir.exists()) {
                val files = sessionDir.listFiles()
                println("[DEBUG_LOG] RGB recording files created: ${files?.size ?: 0}")
                files?.forEach { file ->
                    println("[DEBUG_LOG] - File: ${file.name}, Size: ${file.length()} bytes")
                    assertTrue("[DEBUG_LOG] File is empty: ${file.name}", file.length() > 0)
                }
            }

            println("[DEBUG_LOG] Testing IR camera file writing...")

            val thermalInitResult = thermalRecorder.initialize()
            delay(10000)

            val thermalStatus = thermalRecorder.getThermalCameraStatus()
            if (thermalStatus.isAvailable) {
                println("[DEBUG_LOG] IR camera available, testing recording...")

                val thermalSessionId = "thermal_file_test_${System.currentTimeMillis()}"
                val recordingResult = thermalRecorder.startRecording(thermalSessionId)

                if (recordingResult) {
                    delay(5000)

                    val stopResult = thermalRecorder.stopRecording()
                    assertTrue("[DEBUG_LOG] Failed to stop IR recording", stopResult)

                    val thermalDir = File(baseDir, thermalSessionId)
                    if (thermalDir.exists()) {
                        val thermalFiles = thermalDir.listFiles()
                        println("[DEBUG_LOG] IR recording files created: ${thermalFiles?.size ?: 0}")
                        thermalFiles?.forEach { file ->
                            println("[DEBUG_LOG] - Thermal file: ${file.name}, Size: ${file.length()} bytes")
                            assertTrue("[DEBUG_LOG] Thermal file is empty: ${file.name}", file.length() > 0)
                        }
                    }
                }
            } else {
                println("[DEBUG_LOG] IR camera not available, skipping IR file writing test")
            }

            println("[DEBUG_LOG] File writing functionality verification completed")
        }

    @Test
    fun test5_concurrentCameraAccessTest() =
        runBlocking {
            println("[DEBUG_LOG] Starting concurrent camera access test...")

            val rgbInitResult = cameraRecorder.initialize(textureView)
            assertTrue("[DEBUG_LOG] RGB camera initialization failed", rgbInitResult)

            val thermalInitResult = thermalRecorder.initialize()
            delay(10000)

            val thermalStatus = thermalRecorder.getThermalCameraStatus()

            if (thermalStatus.isAvailable) {
                println("[DEBUG_LOG] Both cameras available, testing concurrent access...")

                delay(2000)

                val rgbSessionId = "concurrent_rgb_${System.currentTimeMillis()}"
                currentSession = cameraRecorder.startSession(recordVideo = true, captureRaw = false)
                assertNotNull("[DEBUG_LOG] Failed to start RGB session", currentSession)

                val thermalSessionId = "concurrent_thermal_${System.currentTimeMillis()}"
                val thermalRecordingResult = thermalRecorder.startRecording(thermalSessionId)
                assertTrue("[DEBUG_LOG] Failed to start thermal recording", thermalRecordingResult)

                println("[DEBUG_LOG] Both cameras recording concurrently...")

                delay(5000)

                val rgbFinalSession = cameraRecorder.stopSession()
                val thermalStopResult = thermalRecorder.stopRecording()

                assertNotNull("[DEBUG_LOG] Failed to stop RGB session", rgbFinalSession)
                assertTrue("[DEBUG_LOG] Failed to stop thermal recording", thermalStopResult)

                println("[DEBUG_LOG] Concurrent recording test completed successfully")
                println("[DEBUG_LOG] RGB session duration: ${rgbFinalSession?.getDurationMs()}ms")

                val finalThermalStatus = thermalRecorder.getThermalCameraStatus()
                println("[DEBUG_LOG] Thermal frame count: ${finalThermalStatus.frameCount}")
            } else {
                println("[DEBUG_LOG] IR camera not available, testing RGB only...")

                delay(2000)
                currentSession = cameraRecorder.startSession(recordVideo = true, captureRaw = true)
                assertNotNull("[DEBUG_LOG] Failed to start RGB-only session", currentSession)

                delay(3000)

                val finalSession = cameraRecorder.stopSession()
                assertNotNull("[DEBUG_LOG] Failed to stop RGB-only session", finalSession)

                println("[DEBUG_LOG] RGB-only test completed successfully")
            }
        }

    @Test
    fun test6_deviceSpecificHardwareVerification() {
        println("[DEBUG_LOG] Starting device-specific hardware verification...")

        val packageManager = context.packageManager

        val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        val hasCameraFront = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        val hasCameraFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        val hasCameraAutofocus = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)

        println("[DEBUG_LOG] Camera Hardware Features:")
        println("[DEBUG_LOG] - Camera: $hasCamera")
        println("[DEBUG_LOG] - Front Camera: $hasCameraFront")
        println("[DEBUG_LOG] - Camera Flash: $hasCameraFlash")
        println("[DEBUG_LOG] - Camera Autofocus: $hasCameraAutofocus")

        assertTrue("[DEBUG_LOG] Device does not have camera hardware", hasCamera)

        val hasUsbHost = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        println("[DEBUG_LOG] - USB Host: $hasUsbHost")
        assertTrue("[DEBUG_LOG] Device does not support USB host mode", hasUsbHost)

        val externalFilesDir = context.getExternalFilesDir(null)
        val internalFilesDir = context.filesDir

        println("[DEBUG_LOG] Storage Information:")
        println("[DEBUG_LOG] - External files dir: ${externalFilesDir?.absolutePath}")
        println("[DEBUG_LOG] - Internal files dir: ${internalFilesDir.absolutePath}")
        println("[DEBUG_LOG] - External storage available: ${externalFilesDir != null}")

        if (externalFilesDir != null) {
            val testFile = File(externalFilesDir, "test_write.tmp")
            try {
                testFile.writeText("test")
                assertTrue("[DEBUG_LOG] External storage not writable", testFile.exists())
                testFile.delete()
                println("[DEBUG_LOG] External storage write test: PASS")
            } catch (e: Exception) {
                println("[DEBUG_LOG] External storage write test: FAIL - ${e.message}")
            }
        }

        println("[DEBUG_LOG] Device hardware verification completed")
    }
}
