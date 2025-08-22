package com.multisensor.recording.handsegmentation

import android.content.Context
import android.graphics.*
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class HandSegmentationEngine(private val context: Context) {

    companion object {
        private const val TAG = "HandSegmentationEngine"
        private const val MIN_HAND_AREA = 2000
        private const val SKIN_HUE_MIN = 0
        private const val SKIN_HUE_MAX = 25
        private const val SKIN_SAT_MIN = 40
        private const val SKIN_SAT_MAX = 255
        private const val SKIN_VAL_MIN = 60
        private const val SKIN_VAL_MAX = 255
    }

    data class HandRegion(
        val boundingBox: Rect,
        val confidence: Float,
        val timestamp: Long,
        val handType: HandType = HandType.UNKNOWN
    )

    enum class HandType {
        LEFT, RIGHT, UNKNOWN
    }

    data class SegmentationResult(
        val detectedHands: List<HandRegion>,
        val processedBitmap: Bitmap?,
        val maskBitmap: Bitmap?,
        val processingTimeMs: Long
    )

    interface HandSegmentationCallback {
        fun onHandDetected(handRegions: List<HandRegion>)
        fun onSegmentationResult(result: SegmentationResult)
        fun onError(error: String)
    }

    private var callback: HandSegmentationCallback? = null
    private var isInitialized = false
    private var outputDirectory: File? = null
    private val croppedDataset = mutableListOf<CroppedHandData>()

    data class CroppedHandData(
        val croppedBitmap: Bitmap,
        val boundingBox: Rect,
        val timestamp: Long,
        val handType: HandType,
        val confidence: Float
    )

    fun initialize(outputDir: File? = null, callback: HandSegmentationCallback? = null): Boolean {
        return try {
            this.callback = callback
            this.outputDirectory = outputDir ?: File(context.getExternalFilesDir(null), "hand_segmentation")

            outputDirectory?.let { dir ->
                if (!dir.exists() && !dir.mkdirs()) {
                    Log.e(TAG, "Failed to create output directory: ${dir.absolutePath}")
                    return false
                }
            }

            isInitialized = true
            Log.i(TAG, "Hand segmentation engine initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize hand segmentation engine", e)
            false
        }
    }

    suspend fun processFrame(inputBitmap: Bitmap, timestamp: Long = System.currentTimeMillis()): SegmentationResult {
        return withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()

            try {
                if (!isInitialized) {
                    throw IllegalStateException("Engine not initialized")
                }

                val handRegions = detectHandsColorBased(inputBitmap, timestamp)

                val maskBitmap = createHandMask(inputBitmap, handRegions)

                val processedBitmap = highlightHands(inputBitmap, handRegions)

                addToCroppedDataset(inputBitmap, handRegions, timestamp)

                val processingTime = System.currentTimeMillis() - startTime

                val result = SegmentationResult(
                    detectedHands = handRegions,
                    processedBitmap = processedBitmap,
                    maskBitmap = maskBitmap,
                    processingTimeMs = processingTime
                )

                withContext(Dispatchers.Main) {
                    callback?.onHandDetected(handRegions)
                    callback?.onSegmentationResult(result)
                }

                result

            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame", e)
                withContext(Dispatchers.Main) {
                    callback?.onError("Frame processing failed: ${e.message}")
                }
                SegmentationResult(
                    detectedHands = emptyList(),
                    processedBitmap = null,
                    maskBitmap = null,
                    processingTimeMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }

    private fun detectHandsColorBased(bitmap: Bitmap, timestamp: Long): List<HandRegion> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val skinMask = BooleanArray(width * height)
        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            Color.colorToHSV(pixel, hsv)

            val hue = hsv[0]
            val sat = hsv[1] * 255
            val value = hsv[2] * 255

            skinMask[i] = (hue >= SKIN_HUE_MIN && hue <= SKIN_HUE_MAX) &&
                    (sat >= SKIN_SAT_MIN && sat <= SKIN_SAT_MAX) &&
                    (value >= SKIN_VAL_MIN && value <= SKIN_VAL_MAX)
        }

        return findHandRegions(skinMask, width, height, timestamp)
    }

    private fun findHandRegions(skinMask: BooleanArray, width: Int, height: Int, timestamp: Long): List<HandRegion> {
        val visited = BooleanArray(skinMask.size)
        val handRegions = mutableListOf<HandRegion>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (skinMask[index] && !visited[index]) {
                    val component = floodFill(skinMask, visited, x, y, width, height)

                    if (component.size >= MIN_HAND_AREA) {
                        val boundingBox = calculateBoundingBox(component)
                        val confidence = calculateConfidence(component, boundingBox)
                        val handType = determineHandType(boundingBox, width)

                        handRegions.add(
                            HandRegion(
                                boundingBox = boundingBox,
                                confidence = confidence,
                                timestamp = timestamp,
                                handType = handType
                            )
                        )
                    }
                }
            }
        }

        return handRegions.sortedByDescending { it.confidence }.take(2)
    }

    private fun floodFill(
        skinMask: BooleanArray,
        visited: BooleanArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): List<Point> {
        val component = mutableListOf<Point>()
        val stack = mutableListOf<Point>()
        stack.add(Point(startX, startY))

        while (stack.isNotEmpty()) {
            val point = stack.removeAt(stack.size - 1)
            val x = point.x
            val y = point.y
            val index = y * width + x

            if (x < 0 || x >= width || y < 0 || y >= height || visited[index] || !skinMask[index]) {
                continue
            }

            visited[index] = true
            component.add(point)

            stack.add(Point(x + 1, y))
            stack.add(Point(x - 1, y))
            stack.add(Point(x, y + 1))
            stack.add(Point(x, y - 1))
        }

        return component
    }

    private fun calculateBoundingBox(points: List<Point>): Rect {
        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE

        for (point in points) {
            minX = min(minX, point.x)
            maxX = max(maxX, point.x)
            minY = min(minY, point.y)
            maxY = max(maxY, point.y)
        }

        return Rect(minX, minY, maxX, maxY)
    }

    private fun calculateConfidence(points: List<Point>, boundingBox: Rect): Float {
        val area = points.size.toFloat()
        val boundingArea = (boundingBox.width() * boundingBox.height()).toFloat()
        val fillRatio = area / boundingArea

        val sizeScore = min(1.0f, area / 10000f)
        val shapeScore = fillRatio

        return (sizeScore * 0.6f + shapeScore * 0.4f).coerceIn(0f, 1f)
    }

    private fun determineHandType(boundingBox: Rect, imageWidth: Int): HandType {
        val centerX = boundingBox.centerX()
        return if (centerX < imageWidth / 2) HandType.LEFT else HandType.RIGHT
    }

    private fun createHandMask(originalBitmap: Bitmap, handRegions: List<HandRegion>): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(maskBitmap)

        canvas.drawColor(Color.BLACK)

        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        for (handRegion in handRegions) {
            canvas.drawRect(handRegion.boundingBox, paint)
        }

        return maskBitmap
    }

    private fun highlightHands(originalBitmap: Bitmap, handRegions: List<HandRegion>): Bitmap {
        val processedBitmap = originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        for ((index, handRegion) in handRegions.withIndex()) {
            paint.color = if (index == 0) Color.GREEN else Color.BLUE
            canvas.drawRect(handRegion.boundingBox, paint)

            val textPaint = Paint().apply {
                color = paint.color
                textSize = 32f
                isAntiAlias = true
            }

            val confidence = String.format("%.2f", handRegion.confidence)
            val handTypeText = handRegion.handType.name
            canvas.drawText(
                "$handTypeText: $confidence",
                handRegion.boundingBox.left.toFloat(),
                handRegion.boundingBox.top - 10f,
                textPaint
            )
        }

        return processedBitmap
    }

    private fun addToCroppedDataset(originalBitmap: Bitmap, handRegions: List<HandRegion>, timestamp: Long) {
        for (handRegion in handRegions) {
            try {
                val padding = 20
                val paddedRect = Rect(
                    max(0, handRegion.boundingBox.left - padding),
                    max(0, handRegion.boundingBox.top - padding),
                    min(originalBitmap.width, handRegion.boundingBox.right + padding),
                    min(originalBitmap.height, handRegion.boundingBox.bottom + padding)
                )

                if (paddedRect.width() > 0 && paddedRect.height() > 0) {
                    val croppedBitmap = Bitmap.createBitmap(
                        originalBitmap,
                        paddedRect.left,
                        paddedRect.top,
                        paddedRect.width(),
                        paddedRect.height()
                    )

                    croppedDataset.add(
                        CroppedHandData(
                            croppedBitmap = croppedBitmap,
                            boundingBox = paddedRect,
                            timestamp = timestamp,
                            handType = handRegion.handType,
                            confidence = handRegion.confidence
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to crop hand region", e)
            }
        }
    }

    fun saveCroppedDataset(sessionId: String? = null): File? {
        return try {
            val sessionName = sessionId ?: SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val datasetDir = File(outputDirectory, "cropped_dataset_$sessionName")

            if (!datasetDir.exists() && !datasetDir.mkdirs()) {
                Log.e(TAG, "Failed to create dataset directory")
                return null
            }

            Log.i(TAG, "Saving ${croppedDataset.size} cropped hand images to ${datasetDir.absolutePath}")

            var savedCount = 0
            for ((index, data) in croppedDataset.withIndex()) {
                val filename =
                    "hand_${data.handType.name.lowercase()}_${data.timestamp}_${String.format("%04d", index)}.png"
                val file = File(datasetDir, filename)

                try {
                    FileOutputStream(file).use { out ->
                        data.croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        savedCount++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save cropped image: $filename", e)
                }
            }

            saveDatasetMetadata(datasetDir, savedCount)

            Log.i(TAG, "Successfully saved $savedCount cropped hand images")
            datasetDir

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cropped dataset", e)
            null
        }
    }

    private fun saveDatasetMetadata(datasetDir: File, savedCount: Int) {
        try {
            val metadataFile = File(datasetDir, "metadata.json")
            val metadata = mapOf(
                "total_images" to savedCount,
                "creation_timestamp" to System.currentTimeMillis(),
                "dataset_type" to "hand_segmentation",
                "hand_types" to croppedDataset.groupBy { it.handType }.mapValues { it.value.size },
                "average_confidence" to croppedDataset.map { it.confidence }.average(),
                "processing_engine" to "AndroidHandSegmentationEngine"
            )

            metadataFile.writeText(metadata.toString())

        } catch (e: Exception) {
            Log.w(TAG, "Failed to save dataset metadata", e)
        }
    }

    fun clearCroppedDataset() {
        croppedDataset.clear()
        Log.i(TAG, "Cropped dataset cleared")
    }

    fun getDatasetStats(): Map<String, Any> {
        return mapOf(
            "total_samples" to croppedDataset.size,
            "left_hands" to croppedDataset.count { it.handType == HandType.LEFT },
            "right_hands" to croppedDataset.count { it.handType == HandType.RIGHT },
            "unknown_hands" to croppedDataset.count { it.handType == HandType.UNKNOWN },
            "average_confidence" to if (croppedDataset.isNotEmpty()) croppedDataset.map { it.confidence }
                .average() else 0.0,
            "timespan_ms" to if (croppedDataset.isNotEmpty()) croppedDataset.maxOf { it.timestamp } - croppedDataset.minOf { it.timestamp } else 0L
        )
    }

    fun cleanup() {
        croppedDataset.forEach { data ->
            if (!data.croppedBitmap.isRecycled) {
                data.croppedBitmap.recycle()
            }
        }
        croppedDataset.clear()

        callback = null
        isInitialized = false
        Log.i(TAG, "Hand segmentation engine cleaned up")
    }
}
