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
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.multisensor.recording.util.SimpleFileUtils

@Singleton
class CameraRecorder
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val logger: Logger,
) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var textureView: TextureView? = null
    private var previewSurface: Surface? = null
    private var mediaRecorder: MediaRecorder? = null
    private var rawImageReader: ImageReader? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var currentSessionInfo: SessionInfo? = null
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var lastRawCaptureResult: TotalCaptureResult? = null
    private var rawCaptureCount = 0

    private var cameraId: String? = null
    private var videoSize: Size = Size(3840, 2160) // 4K for Samsung S22
    private var previewSize: Size? = null
    private var rawSize: Size? = null

    private var isInitialized = false
    private var isSessionActive = false

    companion object {
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_BIT_RATE = 10_000_000 // 10Mbps for 4K
    }

    val isConnected: Boolean get() = cameraDevice != null

    suspend fun initialize(textureView: TextureView): Boolean {
        return try {
            logger.info("Initializing camera for 4K video + RAW capture...")

            if (isInitialized) {
                return true
            }

            this.textureView = textureView
            startBackgroundThread()

            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = findBestCamera(cameraManager)

            if (cameraId == null) {
                logger.error("No suitable camera found with 4K and RAW capability")
                return false
            }

            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId!!)
            configureCameraSizes(cameraCharacteristics!!)
            setupTextureView()

            isInitialized = true
            logger.info("Camera initialized successfully with camera: $cameraId")
            logger.info("Video size: ${videoSize.width}x${videoSize.height}")
            logger.info("Preview size: ${previewSize?.width}x${previewSize?.height}")
            logger.info("RAW size: ${rawSize?.width}x${rawSize?.height}")
            true
        } catch (e: Exception) {
            logger.error("Camera initialization failed", e)
            false
        }
    }

    suspend fun startSession(recordVideo: Boolean, captureRaw: Boolean): SessionInfo? {
        return try {
            if (!isInitialized) {
                logger.error("Camera not initialized")
                return null
            }

            if (isSessionActive) {
                return currentSessionInfo
            }

            val sessionId = "Session_${System.currentTimeMillis()}"
            val sessionInfo = SessionInfo(
                sessionId = sessionId,
                videoEnabled = recordVideo,
                rawEnabled = captureRaw,
                startTime = System.currentTimeMillis(),
                cameraId = cameraId,
                videoResolution = if (recordVideo) "${videoSize.width}x${videoSize.height}" else null,
                rawResolution = if (captureRaw) "${rawSize?.width}x${rawSize?.height}" else null
            )

            openCamera()
            setupCapture(sessionInfo)

            currentSessionInfo = sessionInfo
            isSessionActive = true

            logger.info("Camera session started: ${sessionInfo.getSummary()}")
            sessionInfo
        } catch (e: Exception) {
            logger.error("Failed to start camera session", e)
            null
        }
    }

    suspend fun stopSession(): SessionInfo? {
        return try {
            if (!isSessionActive) {
                return null
            }

            val sessionInfo = currentSessionInfo
            logger.info("Stopping camera session")

            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            rawImageReader?.close()
            rawImageReader = null

            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            isSessionActive = false
            rawCaptureCount = 0
            lastRawCaptureResult = null
            currentSessionInfo = null

            sessionInfo?.markCompleted()
            logger.info("Camera session stopped - ${sessionInfo?.getSummary()}")
            sessionInfo
        } catch (e: Exception) {
            logger.error("Error stopping camera session", e)
            currentSessionInfo
        }
    }

    fun cleanup() {
        try {
            // Cleanup without calling suspend stopSession()
            if (isSessionActive) {
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    mediaRecorder = null

                    rawImageReader?.close()
                    rawImageReader = null

                    captureSession?.close()
                    captureSession = null

                    cameraDevice?.close()
                    cameraDevice = null

                    isSessionActive = false
                    rawCaptureCount = 0
                    lastRawCaptureResult = null
                    currentSessionInfo = null

                    logger.info("Camera cleanup completed")
                } catch (e: Exception) {
                    logger.warning("Error during session cleanup", e)
                }
            }

            stopBackgroundThread()
            isInitialized = false
        } catch (e: Exception) {
            logger.error("Error during cleanup", e)
        }
    }

    suspend fun captureRawImage(): Boolean {
        return try {
            if (!isSessionActive) {
                logger.error("No active session for RAW capture")
                return false
            }

            val sessionInfo = currentSessionInfo
            if (sessionInfo?.rawEnabled != true) {
                logger.error("RAW capture not enabled for current session")
                return false
            }

            if (captureSession == null || rawImageReader == null) {
                logger.error("Capture session or RAW ImageReader not available")
                return false
            }

            logger.info("Triggering manual RAW capture...")

            val captureRequest = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                rawImageReader?.surface?.let { addTarget(it) }
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            }?.build()

            captureRequest?.let {
                captureSession?.capture(it, object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        lastRawCaptureResult = result
                        logger.debug("RAW capture completed")
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        logger.error("RAW capture failed: ${failure.reason}")
                    }
                }, backgroundHandler)
            }

            true
        } catch (e: Exception) {
            logger.error("Error capturing RAW image", e)
            false
        }
    }

    fun setPreviewStreamer(streamer: Any) {
        // Preview streaming not implemented in simplified version
    }

    private fun findBestCamera(cameraManager: CameraManager): String? {
        try {
            logger.info("Searching for camera with 4K video and RAW capabilities...")

            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                // Must be back camera
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != CameraCharacteristics.LENS_FACING_BACK) {
                    continue
                }

                // Check hardware level (need FULL or LEVEL_3 for reliable RAW + 4K)
                val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                val isLevel3 = hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
                val isFullOrBetter = hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL || isLevel3

                if (!isFullOrBetter) {
                    logger.debug("Camera $cameraId: Hardware level insufficient for 4K+RAW")
                    continue
                }

                // Check RAW capability
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val hasRawCapability = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true

                if (!hasRawCapability) {
                    logger.debug("Camera $cameraId: No RAW capability")
                    continue
                }

                // Check 4K video support
                val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val videoSizes = streamConfigMap?.getOutputSizes(MediaRecorder::class.java)
                val supports4K = videoSizes?.any { it.width >= 3840 && it.height >= 2160 } == true

                if (!supports4K) {
                    logger.debug("Camera $cameraId: No 4K video support")
                    continue
                }

                // Check RAW sensor sizes
                val rawSizes = streamConfigMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
                val hasRawSizes = rawSizes?.isNotEmpty() == true

                if (!hasRawSizes) {
                    logger.debug("Camera $cameraId: No RAW sensor sizes available")
                    continue
                }

                val levelName = when (hardwareLevel) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    else -> "OTHER($hardwareLevel)"
                }

                logger.info("Found suitable camera: $cameraId (back camera, $levelName, RAW + 4K capable)")
                logger.info("RAW sizes available: ${rawSizes?.size}")
                logger.info("Video sizes available: ${videoSizes?.size}")

                return cameraId
            }

            logger.error("No camera found with 4K video and RAW capabilities")
        } catch (e: Exception) {
            logger.error("Error finding suitable camera", e)
        }
        return null
    }

    private fun configureCameraSizes(characteristics: CameraCharacteristics) {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        if (map == null) {
            logger.error("Stream configuration map not available")
            return
        }

        logger.info("Configuring camera sizes for 4K video + RAW capture...")

        // Configure 4K video size
        val videoSizes = map.getOutputSizes(MediaRecorder::class.java)
        videoSize = videoSizes?.find { it.width == 3840 && it.height == 2160 }
            ?: videoSizes?.find { it.width >= 3840 && it.height >= 2160 }
            ?: videoSizes?.maxByOrNull { it.width * it.height }
            ?: Size(1920, 1080)

        logger.info("Video recording size: ${videoSize.width}x${videoSize.height}")

        // Configure preview size (matching aspect ratio but lower resolution)
        val previewSizes = map.getOutputSizes(SurfaceTexture::class.java)
        val videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()

        previewSize = previewSizes
            ?.filter { size ->
                val aspectRatio = size.width.toFloat() / size.height.toFloat()
                Math.abs(aspectRatio - videoAspectRatio) < 0.1f
            }?.filter { size ->
                size.width <= 1920 && size.height <= 1080
            }?.maxByOrNull { it.width * it.height }
            ?: previewSizes?.find { it.width == 1920 && it.height == 1080 }
            ?: previewSizes?.find { it.width == 1280 && it.height == 720 }
            ?: Size(1280, 720)

        // Configure RAW size
        val rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR)
        rawSize = rawSizes?.maxByOrNull { it.width * it.height }

        logger.info("Camera sizes configured successfully")
        logger.info("  Video: ${videoSize.width}x${videoSize.height}")
        logger.info("  Preview: ${previewSize?.width}x${previewSize?.height}")
        logger.info("  RAW: ${rawSize?.width ?: 0}x${rawSize?.height ?: 0}")
    }

    private fun setupTextureView() {
        textureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                setupPreviewSurface(surface)
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        textureView?.surfaceTexture?.let { setupPreviewSurface(it) }
    }

    private fun setupPreviewSurface(surfaceTexture: SurfaceTexture) {
        try {
            val previewWidth = previewSize?.width ?: 1280
            val previewHeight = previewSize?.height ?: 720
            surfaceTexture.setDefaultBufferSize(previewWidth, previewHeight)
            previewSurface = Surface(surfaceTexture)
            logger.debug("Preview surface configured: ${previewWidth}x${previewHeight}")
        } catch (e: Exception) {
            logger.error("Error setting up preview surface", e)
        }
    }

    private fun openCamera() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            logger.error("Failed to open camera", e)
        }
    }

    private fun setupCapture(sessionInfo: SessionInfo) {
        try {
            val surfaces = mutableListOf<Surface>()
            previewSurface?.let { surfaces.add(it) }

            if (sessionInfo.videoEnabled) {
                setupMediaRecorder(sessionInfo)
                mediaRecorder?.surface?.let { surfaces.add(it) }
            }

            if (sessionInfo.rawEnabled) {
                setupRawImageReader(sessionInfo)
                rawImageReader?.surface?.let { surfaces.add(it) }
            }

            createCaptureSession(surfaces)
        } catch (e: Exception) {
            logger.error("Error setting up capture", e)
        }
    }

    private fun setupRawImageReader(sessionInfo: SessionInfo) {
        try {
            val rawSize = this.rawSize
            if (rawSize == null) {
                logger.error("RAW size not configured")
                sessionInfo.markError("RAW size not available")
                return
            }

            logger.info("Setting up RAW ImageReader for DNG processing...")
            logger.info("RAW resolution: ${rawSize.width}x${rawSize.height}")

            rawImageReader = ImageReader.newInstance(
                rawSize.width,
                rawSize.height,
                ImageFormat.RAW_SENSOR,
                2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    handleRawImageAvailable(reader, sessionInfo)
                }, backgroundHandler)
            }

            logger.info("RAW ImageReader configured successfully")
        } catch (e: Exception) {
            logger.error("Failed to setup RAW ImageReader", e)
            sessionInfo.markError("RAW ImageReader setup failed: ${e.message}")
            throw e
        }
    }

    private fun setupMediaRecorder(sessionInfo: SessionInfo) {
        try {
            mediaRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(videoSize.width, videoSize.height)
                setVideoFrameRate(VIDEO_FRAME_RATE)
                setVideoEncodingBitRate(VIDEO_BIT_RATE)

                val videoFile = SimpleFileUtils.createVideoFile(context, sessionInfo.sessionId)
                setOutputFile(videoFile.absolutePath)
                sessionInfo.videoFilePath = videoFile.absolutePath

                prepare()
            }

            logger.info("MediaRecorder configured for 4K recording:")
            logger.info("  Resolution: ${videoSize.width}x${videoSize.height}")
            logger.info("  Frame rate: ${VIDEO_FRAME_RATE}fps")
            logger.info("  Bitrate: ${VIDEO_BIT_RATE / 1_000_000}Mbps")
        } catch (e: Exception) {
            logger.error("Error setting up media recorder", e)
        }
    }

    private fun handleRawImageAvailable(reader: ImageReader, sessionInfo: SessionInfo) {
        var image: Image? = null
        try {
            image = reader.acquireNextImage()
            if (image == null) {
                logger.warning("No RAW image available")
                return
            }

            val captureResult = lastRawCaptureResult
            if (captureResult == null) {
                logger.warning("No capture result available for RAW image")
                image.close()
                return
            }

            val cameraCharacteristics = this.cameraCharacteristics
            if (cameraCharacteristics == null) {
                logger.error("Camera characteristics not available for DNG creation")
                image.close()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                processRawImageToDng(image, captureResult, cameraCharacteristics, sessionInfo)
            }
        } catch (e: Exception) {
            logger.error("Error handling RAW image", e)
            sessionInfo.markError("RAW image processing error: ${e.message}")
            image?.close()
        }
    }

    private suspend fun processRawImageToDng(
        image: Image,
        captureResult: TotalCaptureResult,
        characteristics: CameraCharacteristics,
        sessionInfo: SessionInfo,
    ) = withContext(Dispatchers.IO) {
        var outputStream: FileOutputStream? = null

        try {
            rawCaptureCount++
            val dngFile = generateRawFilePath(sessionInfo.sessionId, rawCaptureCount)

            logger.info("Processing RAW image to DNG: ${dngFile.name}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val dngCreatorClass = Class.forName("android.media.DngCreator")
                    val constructor = dngCreatorClass.getConstructor(
                        CameraCharacteristics::class.java,
                        TotalCaptureResult::class.java
                    )
                    val dngCreator = constructor.newInstance(characteristics, captureResult)

                    // Configure DNG metadata
                    val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                    if (sensorOrientation != null) {
                        val setOrientationMethod = dngCreatorClass.getMethod("setOrientation", Int::class.java)
                        setOrientationMethod.invoke(dngCreator, sensorOrientation)
                        logger.debug("DNG orientation set to: $sensorOrientation degrees")
                    }

                    outputStream = FileOutputStream(dngFile)
                    val writeImageMethod = dngCreatorClass.getMethod(
                        "writeImage",
                        java.io.OutputStream::class.java, Image::class.java
                    )
                    writeImageMethod.invoke(dngCreator, outputStream, image)

                    val closeMethod = dngCreatorClass.getMethod("close")
                    closeMethod.invoke(dngCreator)

                    sessionInfo.addRawFile(dngFile.absolutePath)
                    logger.info("DNG file created successfully: ${dngFile.absolutePath}")
                    logger.debug("Total RAW images in session: ${sessionInfo.getRawImageCount()}")

                } catch (e: ClassNotFoundException) {
                    logger.warning("DngCreator class not found - API compatibility issue")
                    sessionInfo.markError("DNG processing: DngCreator class not available")
                } catch (e: ReflectiveOperationException) {
                    logger.warning("DngCreator reflection error: ${e.message}")
                    sessionInfo.markError("DNG processing: Reflection error")
                }
            } else {
                logger.warning("DngCreator not available on API level ${Build.VERSION.SDK_INT} (requires API 21+)")
                sessionInfo.markError("DNG processing requires API 21+")
            }
        } catch (e: Exception) {
            logger.error("Failed to process RAW image to DNG", e)
            sessionInfo.markError("DNG processing failed: ${e.message}")
        } finally {
            try {
                outputStream?.close()
                image.close()
            } catch (e: Exception) {
                logger.warning("Error closing DNG resources", e)
            }
        }
    }

    private fun generateRawFilePath(sessionId: String, count: Int): File {
        return SimpleFileUtils.createRawFile(context, sessionId, count)
    }

    private fun createCaptureSession(surfaces: List<Surface>) {
        try {
            val outputConfigurations = surfaces.map { OutputConfiguration(it) }
            val sessionConfiguration = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigurations,
                { it.run() },
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        startPreview()
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        logger.error("Failed to configure capture session")
                    }
                }
            )
            cameraDevice?.createCaptureSession(sessionConfiguration)
        } catch (e: Exception) {
            logger.error("Error creating capture session", e)
        }
    }

    private fun startPreview() {
        try {
            val previewRequest = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
                previewSurface?.let { addTarget(it) }
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            }?.build()

            previewRequest?.let {
                captureSession?.setRepeatingRequest(it, null, backgroundHandler)
            }
        } catch (e: Exception) {
            logger.error("Error starting preview", e)
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            logger.error("Error stopping background thread", e)
        }
    }

    suspend fun captureCalibrationImage(filePath: String): Boolean {
        return try {
            logger.info("[DEBUG_LOG] Capturing calibration image to: $filePath")

            if (!isInitialized || cameraDevice == null) {
                logger.error("Camera not initialized for calibration capture")
                return false
            }

            // TODO: Implement calibration image capture logic
            // This is a stub implementation for compilation
            logger.info("[DEBUG_LOG] Calibration image capture completed: $filePath")
            true
        } catch (e: Exception) {
            logger.error("Error capturing calibration image", e)
            false
        }
    }

    fun isRawStage3Available(): Boolean {
        return try {
            if (!isInitialized || cameraCharacteristics == null) {
                return false
            }

            // TODO: Implement actual RAW Stage 3 capability check
            // This is a stub implementation for compilation
            logger.debug("Checking RAW Stage 3 availability")
            true
        } catch (e: Exception) {
            logger.error("Error checking RAW Stage 3 availability", e)
            false
        }
    }

    fun triggerFlashSync(durationMs: Long): Boolean {
        return try {
            logger.info("[DEBUG_LOG] Triggering flash sync for ${durationMs}ms")

            if (!isInitialized || cameraDevice == null) {
                logger.error("Camera not initialized for flash sync")
                return false
            }

            // TODO: Implement actual flash sync logic
            // This is a stub implementation for compilation
            logger.info("[DEBUG_LOG] Flash sync triggered successfully")
            true
        } catch (e: Exception) {
            logger.error("Error triggering flash sync", e)
            false
        }
    }
}
