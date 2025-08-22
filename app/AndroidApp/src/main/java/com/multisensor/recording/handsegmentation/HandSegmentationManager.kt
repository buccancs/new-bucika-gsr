package com.multisensor.recording.handsegmentation

import android.content.Context
import android.graphics.Bitmap
import com.multisensor.recording.util.logD
import com.multisensor.recording.util.logE
import com.multisensor.recording.util.logI
import com.multisensor.recording.util.logW
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandSegmentationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "HandSegmentationManager"
    }

    interface HandSegmentationListener {
        fun onHandDetectionStatusChanged(isEnabled: Boolean, handsDetected: Int)
        fun onDatasetProgress(totalSamples: Int, leftHands: Int, rightHands: Int)
        fun onDatasetSaved(datasetPath: String, totalSamples: Int)
        fun onError(error: String)
    }

    private val handSegmentationEngine = HandSegmentationEngine(context)
    private var isEnabled = false
    private var listener: HandSegmentationListener? = null
    private var processingJob: Job? = null
    private var currentSessionId: String? = null

    var isRealTimeProcessingEnabled = false
        private set
    var isCroppedDatasetEnabled = false
        private set

    fun initializeForSession(sessionId: String, listener: HandSegmentationListener? = null): Boolean {
        this.currentSessionId = sessionId
        this.listener = listener

        return try {
            val outputDir = File(context.getExternalFilesDir(null), "sessions/$sessionId/hand_segmentation")

            val success = handSegmentationEngine.initialize(
                outputDir = outputDir,
                callback = object : HandSegmentationEngine.HandSegmentationCallback {
                    override fun onHandDetected(handRegions: List<HandSegmentationEngine.HandRegion>) {
                        listener?.onHandDetectionStatusChanged(isEnabled, handRegions.size)

                        if (isCroppedDatasetEnabled) {
                            val stats = handSegmentationEngine.getDatasetStats()
                            listener?.onDatasetProgress(
                                totalSamples = stats["total_samples"] as Int,
                                leftHands = stats["left_hands"] as Int,
                                rightHands = stats["right_hands"] as Int
                            )
                        }
                    }

                    override fun onSegmentationResult(result: HandSegmentationEngine.SegmentationResult) {
                        logD("Processed frame in ${result.processingTimeMs}ms, found ${result.detectedHands.size} hands")
                    }

                    override fun onError(error: String) {
                        logE("Hand segmentation error: $error")
                        listener?.onError(error)
                    }
                }
            )

            if (success) {
                logI("Hand segmentation initialized for session: $sessionId")
            }

            success
        } catch (e: Exception) {
            logE("Failed to initialize hand segmentation", e)
            false
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return

        isEnabled = enabled

        if (enabled) {
            logI("Hand segmentation enabled")
        } else {
            logI("Hand segmentation disabled")
            stopProcessing()
        }

        listener?.onHandDetectionStatusChanged(isEnabled, 0)
    }

    fun setRealTimeProcessing(enabled: Boolean) {
        isRealTimeProcessingEnabled = enabled
        logI("Real-time hand segmentation processing: ${if (enabled) "enabled" else "disabled"}")
    }

    fun setCroppedDatasetEnabled(enabled: Boolean) {
        isCroppedDatasetEnabled = enabled
        if (!enabled) {
            handSegmentationEngine.clearCroppedDataset()
        }
        logI("Cropped dataset creation: ${if (enabled) "enabled" else "disabled"}")
    }

    fun processFrame(bitmap: Bitmap, timestamp: Long = System.currentTimeMillis()) {
        if (!isEnabled || !isRealTimeProcessingEnabled) return

        processingJob?.cancel()
        processingJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                handSegmentationEngine.processFrame(bitmap, timestamp)
            } catch (e: Exception) {
                logE("Error processing frame", e)
            }
        }
    }

    fun processRecordedVideo(videoPath: String, callback: (success: Boolean, outputPath: String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logI("Starting post-processing of recorded video: $videoPath")

                val outputDir = File(videoPath).parent?.let { "$it/hand_segmentation" } ?: "/tmp/hand_segmentation"
                File(outputDir).mkdirs()

                val mediaRetriever = android.media.MediaMetadataRetriever()
                try {
                    mediaRetriever.setDataSource(videoPath)

                    val durationMs =
                        mediaRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L
                    val frameRate = 10

                    var frameCount = 0
                    var currentTimeMs = 0L

                    logI("Extracting frames from video for hand segmentation: duration=${durationMs}ms")

                    while (currentTimeMs < durationMs && frameCount < 100) {
                        try {
                            val bitmap = mediaRetriever.getFrameAtTime(
                                currentTimeMs * 1000,
                                android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )

                            if (bitmap != null) {
                                val frameFile = File(outputDir, "frame_${String.format("%04d", frameCount)}.jpg")
                                val out = java.io.FileOutputStream(frameFile)
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                out.close()
                                bitmap.recycle()

                                frameCount++
                                logD("Extracted frame $frameCount at ${currentTimeMs}ms")
                            }

                            currentTimeMs += (1000 / frameRate)

                        } catch (e: Exception) {
                            logW("Failed to extract frame at ${currentTimeMs}ms: ${e.message}")
                            currentTimeMs += (1000 / frameRate)
                        }
                    }

                    logI("Hand segmentation frame extraction complete: $frameCount frames extracted")

                } finally {
                    try {
                        mediaRetriever.release()
                    } catch (e: Exception) {
                        logW("Error releasing MediaMetadataRetriever: ${e.message}")
                    }
                }

                withContext(Dispatchers.Main) {
                    callback(true, outputDir)
                }

            } catch (e: Exception) {
                logE("Error processing recorded video", e)
                withContext(Dispatchers.Main) {
                    callback(false, null)
                }
            }
        }
    }

    fun saveCroppedDataset(callback: (success: Boolean, datasetPath: String?, totalSamples: Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = handSegmentationEngine.getDatasetStats()
                val totalSamples = stats["total_samples"] as Int

                if (totalSamples == 0) {
                    withContext(Dispatchers.Main) {
                        callback(false, null, 0)
                        listener?.onError("No hand data to save")
                    }
                    return@launch
                }

                val datasetDir = handSegmentationEngine.saveCroppedDataset(currentSessionId)

                withContext(Dispatchers.Main) {
                    if (datasetDir != null) {
                        logI("Saved cropped dataset: ${datasetDir.absolutePath}")
                        callback(true, datasetDir.absolutePath, totalSamples)
                        listener?.onDatasetSaved(datasetDir.absolutePath, totalSamples)
                    } else {
                        callback(false, null, totalSamples)
                        listener?.onError("Failed to save cropped dataset")
                    }
                }

            } catch (e: Exception) {
                logE("Error saving cropped dataset", e)
                withContext(Dispatchers.Main) {
                    callback(false, null, 0)
                    listener?.onError("Error saving dataset: ${e.message}")
                }
            }
        }
    }

    fun getCurrentDatasetStats(): Map<String, Any> {
        return handSegmentationEngine.getDatasetStats()
    }

    fun clearCurrentDataset() {
        handSegmentationEngine.clearCroppedDataset()
        logI("Cleared current hand dataset")

        val stats = getCurrentDatasetStats()
        listener?.onDatasetProgress(
            totalSamples = stats["total_samples"] as Int,
            leftHands = stats["left_hands"] as Int,
            rightHands = stats["right_hands"] as Int
        )
    }

    private fun stopProcessing() {
        processingJob?.cancel()
        processingJob = null
    }

    fun cleanup() {
        stopProcessing()
        handSegmentationEngine.cleanup()
        currentSessionId = null
        listener = null
        logI("Hand segmentation manager cleaned up")
    }

    fun getStatus(): HandSegmentationStatus {
        val stats = getCurrentDatasetStats()
        return HandSegmentationStatus(
            isEnabled = isEnabled,
            isRealTimeProcessing = isRealTimeProcessingEnabled,
            isCroppedDatasetEnabled = isCroppedDatasetEnabled,
            currentSessionId = currentSessionId,
            totalSamples = stats["total_samples"] as Int,
            leftHands = stats["left_hands"] as Int,
            rightHands = stats["right_hands"] as Int,
            averageConfidence = stats["average_confidence"] as Double
        )
    }

    data class HandSegmentationStatus(
        val isEnabled: Boolean,
        val isRealTimeProcessing: Boolean,
        val isCroppedDatasetEnabled: Boolean,
        val currentSessionId: String?,
        val totalSamples: Int,
        val leftHands: Int,
        val rightHands: Int,
        val averageConfidence: Double
    )
}
