package com.multisensor.recording.streaming

import android.graphics.*
import android.media.Image
import com.multisensor.recording.network.JsonSocketClient
import com.multisensor.recording.network.PreviewFrameMessage
import com.multisensor.recording.util.Logger
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@ServiceScoped
class PreviewStreamer
@Inject
constructor(
    private val jsonSocketClient: JsonSocketClient,
    private val logger: Logger,
) {
    private var streamingScope: CoroutineScope? = null
    private var isStreaming = false
    private var frameCount = 0L

    private var targetFps = 2
    private var jpegQuality = 70
    private var maxFrameWidth = 640
    private var maxFrameHeight = 480

    private var lastFrameTime = 0L
    private var frameIntervalMs = 1000L / targetFps

    companion object {
        private const val PREVIEW_RGB_TAG = "PREVIEW_RGB"
        private const val PREVIEW_THERMAL_TAG = "PREVIEW_THERMAL"
    }

    fun configure(
        fps: Int = 2,
        quality: Int = 70,
        maxWidth: Int = 640,
        maxHeight: Int = 480,
    ) {
        targetFps = fps
        jpegQuality = quality
        maxFrameWidth = maxWidth
        maxFrameHeight = maxHeight
        updateFrameInterval()

        logger.info("PreviewStreamer configured: ${fps}fps, quality=$quality, size=${maxWidth}x$maxHeight")
    }

    fun updateFrameRate(newFps: Float) {
        if (newFps <= 0) {
            logger.warning("Invalid frame rate: $newFps, ignoring update")
            return
        }

        val previousFps = targetFps
        targetFps = newFps.toInt()
        updateFrameInterval()

        logger.info("[DEBUG_LOG] PreviewStreamer frame rate updated from ${previousFps}fps to ${targetFps}fps")
    }

    fun getCurrentFrameRate(): Float = targetFps.toFloat()

    private fun updateFrameInterval() {
        frameIntervalMs =
            if (targetFps > 0) {
                (1000L / targetFps).coerceAtLeast(1L)
            } else {
                1000L
            }
    }

    fun startStreaming() {
        if (isStreaming) {
            logger.warning("PreviewStreamer already streaming")
            return
        }

        isStreaming = true
        frameCount = 0
        lastFrameTime = 0
        streamingScope = CoroutineScope(Dispatchers.IO + Job())

        logger.info("PreviewStreamer started")
    }

    fun stopStreaming() {
        if (!isStreaming) {
            logger.info("PreviewStreamer not currently streaming")
            return
        }

        logger.info("Stopping PreviewStreamer...")
        isStreaming = false

        try {
            streamingScope?.cancel()
            streamingScope = null

            logger.info("PreviewStreamer stopped successfully. Total frames: $frameCount")
        } catch (e: Exception) {
            logger.error("Error stopping PreviewStreamer", e)
        }
    }

    fun onRgbFrameAvailable(image: Image) {
        if (!isStreaming || !shouldProcessFrame()) {
            image.close()
            return
        }

        streamingScope?.launch {
            try {
                val jpegBytes = convertImageToJpeg(image)
                if (jpegBytes != null) {
                    sendPreviewFrame(PREVIEW_RGB_TAG, jpegBytes)
                    frameCount++
                }
            } catch (e: Exception) {
                logger.error("Error processing RGB frame", e)
            } finally {
                image.close()
            }
        }
    }

    fun onThermalFrameAvailable(
        thermalData: ByteArray,
        width: Int,
        height: Int,
    ) {
        if (!isStreaming || !shouldProcessFrame()) {
            return
        }

        streamingScope?.launch {
            try {
                val jpegBytes = convertThermalToJpeg(thermalData, width, height)
                if (jpegBytes != null) {
                    sendPreviewFrame(PREVIEW_THERMAL_TAG, jpegBytes)
                    frameCount++
                }
            } catch (e: Exception) {
                logger.error("Error processing thermal frame", e)
            }
        }
    }

    private fun shouldProcessFrame(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime >= frameIntervalMs) {
            lastFrameTime = currentTime
            return true
        }
        return false
    }

    private fun convertImageToJpeg(image: Image): ByteArray? =
        try {
            when (image.format) {
                ImageFormat.JPEG -> {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    resizeJpegIfNeeded(bytes)
                }

                ImageFormat.YUV_420_888 -> {
                    convertYuvToJpeg(image)
                }

                else -> {
                    logger.warning("Unsupported image format: ${image.format}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to convert image to JPEG", e)
            null
        }

    private fun convertYuvToJpeg(image: Image): ByteArray? =
        try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)

            val uvPixelStride = image.planes[1].pixelStride
            if (uvPixelStride == 1) {
                uBuffer.get(nv21, ySize, uSize)
                vBuffer.get(nv21, ySize + uSize, vSize)
            } else {
                val uvBuffer = ByteArray(uSize + vSize)
                uBuffer.get(uvBuffer, 0, uSize)
                vBuffer.get(uvBuffer, uSize, vSize)

                var uvIndex = ySize
                for (i in 0 until uSize step uvPixelStride) {
                    nv21[uvIndex++] = uvBuffer[i + uSize]
                    nv21[uvIndex++] = uvBuffer[i]
                }
            }

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val outputStream = ByteArrayOutputStream()

            yuvImage.compressToJpeg(
                Rect(0, 0, image.width, image.height),
                jpegQuality,
                outputStream,
            )

            val jpegBytes = outputStream.toByteArray()
            outputStream.close()

            resizeJpegIfNeeded(jpegBytes)
        } catch (e: Exception) {
            logger.error("Failed to convert YUV to JPEG", e)
            null
        }

    private fun resizeJpegIfNeeded(jpegBytes: ByteArray): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

            if (bitmap.width <= maxFrameWidth && bitmap.height <= maxFrameHeight) {
                return jpegBytes
            }

            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (newWidth, newHeight) =
                if (aspectRatio > 1) {
                    Pair(maxFrameWidth, (maxFrameWidth / aspectRatio).toInt())
                } else {
                    Pair((maxFrameHeight * aspectRatio).toInt(), maxFrameHeight)
                }

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            bitmap.recycle()

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)
            resizedBitmap.recycle()

            val resizedBytes = outputStream.toByteArray()
            outputStream.close()

            logger.debug("Resized frame from ${bitmap.width}x${bitmap.height} to ${newWidth}x$newHeight")
            resizedBytes
        } catch (e: Exception) {
            logger.error("Failed to resize JPEG", e)
            jpegBytes
        }
    }

    private fun convertThermalToJpeg(
        thermalData: ByteArray,
        width: Int,
        height: Int,
    ): ByteArray? =
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)

            var minTemp = Int.MAX_VALUE
            var maxTemp = Int.MIN_VALUE

            val tempValues = IntArray(width * height)
            for (i in thermalData.indices step 2) {
                if (i + 1 < thermalData.size && i / 2 < tempValues.size) {
                    val temp = ((thermalData[i + 1].toInt() and 0xFF) shl 8) or (thermalData[i].toInt() and 0xFF)
                    tempValues[i / 2] = temp
                    minTemp = minOf(minTemp, temp)
                    maxTemp = maxOf(maxTemp, temp)
                }
            }

            val tempRange = if (maxTemp > minTemp) maxTemp - minTemp else 1

            for (i in tempValues.indices) {
                if (i < pixels.size) {
                    val normalizedTemp = ((tempValues[i] - minTemp) * 255 / tempRange).coerceIn(0, 255)

                    pixels[i] = applyIronColorPalette(normalizedTemp)
                }
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)
            bitmap.recycle()

            val jpegBytes = outputStream.toByteArray()
            outputStream.close()

            jpegBytes
        } catch (e: Exception) {
            logger.error("Failed to convert thermal data to JPEG", e)
            null
        }

    private fun applyIronColorPalette(normalizedTemp: Int): Int {
        val temp = normalizedTemp.coerceIn(0, 255)

        val r: Int
        val g: Int
        val b: Int

        when {
            temp < 64 -> {
                r = (temp * 4).coerceIn(0, 255)
                g = 0
                b = 0
            }

            temp < 128 -> {
                r = 255
                g = ((temp - 64) * 4).coerceIn(0, 255)
                b = 0
            }

            temp < 192 -> {
                r = 255
                g = 255
                b = ((temp - 128) * 4).coerceIn(0, 255)
            }

            else -> {
                r = 255
                g = 255
                b = 255
            }
        }

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun sendPreviewFrame(
        tag: String,
        jpegBytes: ByteArray,
    ) {
        try {
            val base64Image = android.util.Base64.encodeToString(jpegBytes, android.util.Base64.NO_WRAP)
            val previewMessage = PreviewFrameMessage(
                cam = tag,
                timestamp = System.currentTimeMillis(),
                image = base64Image
            )

            jsonSocketClient.sendMessage(previewMessage)

            logger.debug("Sent $tag frame (${jpegBytes.size} bytes)")
        } catch (e: Exception) {
            logger.error("Failed to send preview frame", e)
        }
    }

    fun getStreamingStats(): StreamingStats =
        StreamingStats(
            isStreaming = isStreaming,
            frameCount = frameCount,
            targetFps = targetFps,
            jpegQuality = jpegQuality,
            maxFrameSize = "${maxFrameWidth}x$maxFrameHeight",
        )

    data class StreamingStats(
        val isStreaming: Boolean,
        val frameCount: Long,
        val targetFps: Int,
        val jpegQuality: Int,
        val maxFrameSize: String,
    )
}
