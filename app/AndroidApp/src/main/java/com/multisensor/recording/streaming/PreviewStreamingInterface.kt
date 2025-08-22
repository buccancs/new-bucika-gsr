package com.multisensor.recording.streaming

import android.media.Image
import com.multisensor.recording.controllers.ControllerConnectionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.*
import javax.inject.Inject

interface PreviewStreamingInterface {
    suspend fun startStreaming(config: StreamingConfig = StreamingConfig())
    suspend fun stopStreaming()
    suspend fun streamFrame(image: Image, frameType: FrameType)
    fun isStreaming(): Boolean
    fun configure(config: StreamingConfig)
}

data class StreamingConfig(
    val targetFps: Int = 2,
    val jpegQuality: Int = 70,
    val maxFrameWidth: Int = 640,
    val maxFrameHeight: Int = 480,
    val enableCompression: Boolean = true
)

enum class FrameType {
    RGB_PREVIEW,
    THERMAL_PREVIEW,
    DEPTH_PREVIEW
}

@ServiceScoped
class NetworkPreviewStreamer @Inject constructor(
    private val controllerConnectionManager: ControllerConnectionManager,
    private val logger: Logger
) : PreviewStreamingInterface {

    private var streamingScope: CoroutineScope? = null
    private var isStreamingActive = false
    private var frameCount = 0L
    private var config = StreamingConfig()

    private var lastFrameTime = 0L
    private var frameIntervalMs = 1000L / config.targetFps

    override suspend fun startStreaming(config: StreamingConfig) {
        if (isStreamingActive) {
            logger.warning("Preview streaming already active")
            return
        }

        this.config = config
        updateFrameInterval()

        streamingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        isStreamingActive = true
        frameCount = 0L

        logger.info("Preview streaming started with config: $config")
    }

    override suspend fun stopStreaming() {
        if (!isStreamingActive) {
            return
        }

        streamingScope?.cancel()
        streamingScope = null
        isStreamingActive = false

        logger.info("Preview streaming stopped. Total frames: $frameCount")
    }

    override suspend fun streamFrame(image: Image, frameType: FrameType) {
        if (!isStreamingActive || !controllerConnectionManager.isConnected()) {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < frameIntervalMs) {
            return
        }

        streamingScope?.launch {
            try {
                val compressedFrame = compressFrame(image)
                val metadata = mapOf(
                    "frameType" to frameType.name,
                    "timestamp" to currentTime,
                    "frameNumber" to frameCount++,
                    "width" to image.width,
                    "height" to image.height
                )

                controllerConnectionManager.sendData(compressedFrame, metadata)
                lastFrameTime = currentTime

            } catch (e: Exception) {
                logger.error("Failed to stream frame: ${e.message}", e)
            }
        }
    }

    override fun isStreaming(): Boolean = isStreamingActive

    override fun configure(config: StreamingConfig) {
        this.config = config
        updateFrameInterval()
        logger.info("Preview streaming reconfigured: $config")
    }

    private fun updateFrameInterval() {
        frameIntervalMs = 1000L / config.targetFps
    }

    private suspend fun compressFrame(image: Image): ByteArray = withContext(Dispatchers.Default) {

        val planes = image.planes
        val buffer = planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return@withContext data
    }
}