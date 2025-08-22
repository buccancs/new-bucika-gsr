package com.multisensor.recording.recording

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat
import com.multisensor.recording.handsegmentation.HandSegmentationInterface
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.streaming.PreviewStreamingInterface
import com.multisensor.recording.streaming.FrameType
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class ModularCameraRecorder
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val logger: Logger,
    private val handSegmentationInterface: HandSegmentationInterface,
    private val previewStreamer: PreviewStreamingInterface,
) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    val isConnected: Boolean
        get() = cameraDevice != null

    private var cameraCharacteristics: CameraCharacteristics? = null

    private var mediaRecorder: MediaRecorder? = null
    private var rawImageReader: ImageReader? = null
    private var previewImageReader: ImageReader? = null
    private var textureView: TextureView? = null
    private var previewSurface: Surface? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraLock = Semaphore(1)
    private val cameraDispatcher = Dispatchers.IO.limitedParallelism(1)

    private var currentSessionInfo: SessionInfo? = null
    private var lastRawCaptureResult: TotalCaptureResult? = null
    private var rawCaptureCount = 0

    private var cameraId: String? = null
    private var videoSize: Size = Size(3840, 2160)
    private var previewSize: Size? = null
    private var rawSize: Size? = null

    private var isInitialized = false
    private var isSessionActive = false

    companion object {
        private const val THREAD_NAME = "ModularCameraRecorder"
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_BIT_RATE = 10_000_000
        private const val CAMERA_LOCK_TIMEOUT_MS = 2500L

        private val ORIENTATIONS =
            mapOf(
                android.view.Surface.ROTATION_0 to 90,
                android.view.Surface.ROTATION_90 to 0,
                android.view.Surface.ROTATION_180 to 270,
                android.view.Surface.ROTATION_270 to 180,
            )
    }

    suspend fun initialize(textureView: TextureView): Boolean =
        withContext(cameraDispatcher) {
            try {
                if (!cameraLock.tryAcquire(CAMERA_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    logger.error("Camera lock timeout during initialization")
                    return@withContext false
                }

                try {
                    logger.info("Initializing ModularCameraRecorder with TextureView...")

                    if (isInitialized) {
                        logger.info("ModularCameraRecorder already initialized")
                        return@withContext true
                    }

                    this@ModularCameraRecorder.textureView = textureView

                    startBackgroundThread()

                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    cameraId = selectBestCamera(cameraManager)

                    if (cameraId == null) {
                        logger.error("No suitable camera found")
                        return@withContext false
                    }

                    cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId!!)
                    setupCameraSizes()

                    isInitialized = true
                    logger.info("ModularCameraRecorder initialized successfully with camera: $cameraId")
                    return@withContext true

                } finally {
                    cameraLock.release()
                }
            } catch (e: Exception) {
                logger.error("Failed to initialize ModularCameraRecorder: ${e.message}", e)
                return@withContext false
            }
        }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(THREAD_NAME).also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun selectBestCamera(cameraManager: CameraManager): String? {
        return try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId
                }
            }
            null
        } catch (e: Exception) {
            logger.error("Error selecting camera: ${e.message}", e)
            null
        }
    }

    private fun setupCameraSizes() {
        cameraCharacteristics?.let { characteristics ->
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            map?.let {

                val outputSizes = it.getOutputSizes(MediaRecorder::class.java)
                videoSize = outputSizes.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

                val previewSizes = it.getOutputSizes(android.graphics.ImageFormat.PRIVATE)
                previewSize = previewSizes.find { it.width <= 1920 && it.height <= 1080 } ?: Size(1280, 720)

                val rawSizes = it.getOutputSizes(ImageFormat.JPEG)
                rawSize = rawSizes.maxByOrNull { it.width * it.height } ?: Size(3840, 2160)

                logger.info("Camera sizes configured - Video: $videoSize, Preview: $previewSize, Raw: $rawSize")
            }
        }
    }

    suspend fun openCamera(): Boolean = withContext(cameraDispatcher) {
        if (!isInitialized) {
            logger.error("ModularCameraRecorder not initialized")
            return@withContext false
        }

        if (cameraDevice != null) {
            logger.info("Camera already open")
            return@withContext true
        }

        try {
            if (!cameraLock.tryAcquire(CAMERA_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                logger.error("Camera lock timeout during camera opening")
                return@withContext false
            }

            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

                return@withContext suspendCancellableCoroutine { continuation ->
                    val stateCallback = object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraDevice = camera
                            logger.info("Camera opened successfully")
                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                            cameraDevice = null
                            logger.info("Camera disconnected")
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            cameraDevice = null
                            logger.error("Camera error: $error")
                            if (continuation.isActive) {
                                continuation.resumeWithException(RuntimeException("Camera error: $error"))
                            }
                        }
                    }

                    try {
                        cameraManager.openCamera(cameraId!!, stateCallback, backgroundHandler)
                    } catch (e: SecurityException) {
                        logger.error("Security exception when opening camera: ${e.message}", e)
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            } finally {
                cameraLock.release()
            }
        } catch (e: Exception) {
            logger.error("Failed to open camera: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun startPreviewSession(): Boolean = withContext(cameraDispatcher) {
        if (cameraDevice == null || textureView == null) {
            logger.error("Camera device or TextureView not available for preview session")
            return@withContext false
        }

        try {
            if (!cameraLock.tryAcquire(CAMERA_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                logger.error("Camera lock timeout during preview session start")
                return@withContext false
            }

            try {
                setupPreviewImageReader()
                setupPreviewSurface()

                val surfaces = mutableListOf<Surface>()
                previewSurface?.let { surfaces.add(it) }
                previewImageReader?.surface?.let { surfaces.add(it) }

                return@withContext suspendCancellableCoroutine { continuation ->
                    val stateCallback = object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            startRepeatingPreview()
                            logger.info("Preview session configured successfully")

                            CoroutineScope(Dispatchers.IO).launch {
                                previewStreamer.startStreaming()
                            }

                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            logger.error("Preview session configuration failed")
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val outputConfigs = surfaces.map { OutputConfiguration(it) }
                        val sessionConfig = SessionConfiguration(
                            SessionConfiguration.SESSION_REGULAR,
                            outputConfigs,
                            ContextCompat.getMainExecutor(context),
                            stateCallback
                        )
                        cameraDevice!!.createCaptureSession(sessionConfig)
                    } else {
                        @Suppress("DEPRECATION")
                        cameraDevice!!.createCaptureSession(surfaces, stateCallback, backgroundHandler)
                    }
                }
            } finally {
                cameraLock.release()
            }
        } catch (e: Exception) {
            logger.error("Failed to start preview session: ${e.message}", e)
            return@withContext false
        }
    }

    private fun setupPreviewImageReader() {
        previewSize?.let { size ->
            previewImageReader = ImageReader.newInstance(
                size.width, size.height, ImageFormat.YUV_420_888, 1
            ).apply {
                setOnImageAvailableListener({
                    val image = it.acquireLatestImage()
                    image?.let { img ->

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                previewStreamer.streamFrame(img, FrameType.RGB_PREVIEW)

                                handSegmentationInterface.processFrame(img)
                            } catch (e: Exception) {
                                logger.error("Error processing preview frame: ${e.message}", e)
                            } finally {
                                img.close()
                            }
                        }
                    }
                }, backgroundHandler)
            }
        }
    }

    private fun setupPreviewSurface() {
        textureView?.let { tv ->
            val surfaceTexture = tv.surfaceTexture
            previewSize?.let { size ->
                surfaceTexture?.setDefaultBufferSize(size.width, size.height)
                previewSurface = Surface(surfaceTexture)
            }
        }
    }

    private fun startRepeatingPreview() {
        try {
            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            previewSurface?.let { captureRequestBuilder?.addTarget(it) }
            previewImageReader?.surface?.let { captureRequestBuilder?.addTarget(it) }

            captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)

            val captureRequest = captureRequestBuilder?.build()
            captureRequest?.let {
                captureSession?.setRepeatingRequest(it, null, backgroundHandler)
            }
        } catch (e: Exception) {
            logger.error("Failed to start repeating preview: ${e.message}", e)
        }
    }

    suspend fun stopSession() = withContext(cameraDispatcher) {
        try {
            if (!cameraLock.tryAcquire(CAMERA_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                logger.error("Camera lock timeout during session stop")
                return@withContext
            }

            try {

                previewStreamer.stopStreaming()

                captureSession?.close()
                captureSession = null
                isSessionActive = false

                previewImageReader?.close()
                previewImageReader = null

                previewSurface?.release()
                previewSurface = null

                logger.info("Camera session stopped successfully")
            } finally {
                cameraLock.release()
            }
        } catch (e: Exception) {
            logger.error("Error stopping camera session: ${e.message}", e)
        }
    }

    suspend fun close() = withContext(cameraDispatcher) {
        try {
            stopSession()

            cameraDevice?.close()
            cameraDevice = null

            stopBackgroundThread()

            isInitialized = false
            logger.info("ModularCameraRecorder closed successfully")
        } catch (e: Exception) {
            logger.error("Error closing ModularCameraRecorder: ${e.message}", e)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            logger.error("Error stopping background thread: ${e.message}", e)
        }
    }
}
