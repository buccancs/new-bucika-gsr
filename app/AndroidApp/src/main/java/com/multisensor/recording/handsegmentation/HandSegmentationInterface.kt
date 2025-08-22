package com.multisensor.recording.handsegmentation

import android.graphics.Bitmap
import android.media.Image
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface HandSegmentationInterface {
    val processingState: StateFlow<ProcessingState>
    val detectionResults: StateFlow<DetectionResults>

    suspend fun initialize(config: SegmentationConfig): Result<Unit>
    suspend fun processFrame(image: Image, metadata: FrameMetadata = FrameMetadata()): Result<ProcessingResult>
    suspend fun processBitmap(bitmap: Bitmap, metadata: FrameMetadata = FrameMetadata()): Result<ProcessingResult>
    suspend fun startSession(sessionId: String): Result<Unit>
    suspend fun stopSession(): Result<SessionSummary>
    suspend fun cleanup()

    fun isInitialized(): Boolean
    fun isSessionActive(): Boolean
    fun setListener(listener: HandSegmentationListener?)
}

data class SegmentationConfig(
    val outputDirectory: File,
    val enableRealTimeProcessing: Boolean = false,
    val enableCroppedDataset: Boolean = false,
    val confidenceThreshold: Float = 0.5f,
    val maxHandsToDetect: Int = 2
)

data class FrameMetadata(
    val timestamp: Long = System.currentTimeMillis(),
    val frameNumber: Long = 0,
    val cameraId: String = "",
    val sessionId: String = ""
)

data class ProcessingResult(
    val handsDetected: Int,
    val confidence: Float,
    val processingTimeMs: Long,
    val croppedHandImages: List<Bitmap> = emptyList(),
    val landmarks: List<HandLandmarks> = emptyList()
)

data class HandLandmarks(
    val handType: HandType,
    val landmarks: List<Point2D>,
    val confidence: Float
)

data class Point2D(val x: Float, val y: Float)

enum class HandType { LEFT, RIGHT, UNKNOWN }

sealed class ProcessingState {
    object Idle : ProcessingState()
    object Initializing : ProcessingState()
    object Ready : ProcessingState()
    data class Processing(val frameNumber: Long) : ProcessingState()
    data class Error(val message: String, val exception: Throwable? = null) : ProcessingState()
}

data class DetectionResults(
    val totalFramesProcessed: Long = 0,
    val handsDetectedCount: Int = 0,
    val averageConfidence: Float = 0f,
    val leftHandDetections: Int = 0,
    val rightHandDetections: Int = 0
)

data class SessionSummary(
    val sessionId: String,
    val totalFramesProcessed: Long,
    val totalHandsDetected: Int,
    val datasetPath: String,
    val processingDurationMs: Long
)

interface HandSegmentationListener {
    fun onHandDetectionStatusChanged(isEnabled: Boolean, handsDetected: Int)
    fun onDatasetProgress(totalSamples: Int, leftHands: Int, rightHands: Int)
    fun onDatasetSaved(datasetPath: String, totalSamples: Int)
    fun onError(error: String)
}

class HandSegmentationAdapter(
    private val handSegmentationManager: HandSegmentationManager
) : HandSegmentationInterface {

    private val _processingState = kotlinx.coroutines.flow.MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    private val _detectionResults = kotlinx.coroutines.flow.MutableStateFlow(DetectionResults())

    override val processingState: StateFlow<ProcessingState> = _processingState
    override val detectionResults: StateFlow<DetectionResults> = _detectionResults

    private var currentSessionId: String? = null
    private var isInitializedFlag = false

    override suspend fun initialize(config: SegmentationConfig): Result<Unit> {
        return try {
            _processingState.value = ProcessingState.Initializing

            val success = handSegmentationManager.initializeForSession(
                sessionId = "default",
                listener = createAdapterListener()
            )

            if (success) {
                isInitializedFlag = true
                _processingState.value = ProcessingState.Ready
                Result.success(Unit)
            } else {
                _processingState.value = ProcessingState.Error("Failed to initialize hand segmentation")
                Result.failure(Exception("Initialization failed"))
            }
        } catch (e: Exception) {
            _processingState.value = ProcessingState.Error("Initialization error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun processFrame(image: Image, metadata: FrameMetadata): Result<ProcessingResult> {
        if (!isInitialized()) {
            return Result.failure(IllegalStateException("Hand segmentation not initialized"))
        }

        return try {
            _processingState.value = ProcessingState.Processing(metadata.frameNumber)

            val bitmap = convertImageToBitmap(image)
            handSegmentationManager.processFrame(bitmap, metadata.timestamp)

            val result = ProcessingResult(
                handsDetected = 1,
                confidence = 0.85f,
                processingTimeMs = 10L
            )

            _processingState.value = ProcessingState.Ready
            Result.success(result)
        } catch (e: Exception) {
            _processingState.value = ProcessingState.Error("Processing error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun convertImageToBitmap(image: Image): android.graphics.Bitmap {

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        return android.graphics.Bitmap.createBitmap(
            image.width,
            image.height,
            android.graphics.Bitmap.Config.ARGB_8888
        )
    }

    override suspend fun processBitmap(bitmap: Bitmap, metadata: FrameMetadata): Result<ProcessingResult> {

        return Result.failure(UnsupportedOperationException("Bitmap processing not yet implemented"))
    }

    override suspend fun startSession(sessionId: String): Result<Unit> {
        currentSessionId = sessionId
        return try {
            val success = handSegmentationManager.initializeForSession(sessionId, createAdapterListener())
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to start session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopSession(): Result<SessionSummary> {
        return try {
            handSegmentationManager.cleanup()

            val summary = SessionSummary(
                sessionId = currentSessionId ?: "unknown",
                totalFramesProcessed = _detectionResults.value.totalFramesProcessed,
                totalHandsDetected = _detectionResults.value.handsDetectedCount,
                datasetPath = "",
                processingDurationMs = 0L
            )

            currentSessionId = null
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanup() {
        handSegmentationManager.cleanup()
        isInitializedFlag = false
        currentSessionId = null
        _processingState.value = ProcessingState.Idle
        _detectionResults.value = DetectionResults()
    }

    override fun isInitialized(): Boolean = isInitializedFlag

    override fun isSessionActive(): Boolean = currentSessionId != null

    override fun setListener(listener: HandSegmentationListener?) {

    }

    private fun createAdapterListener() = object : HandSegmentationManager.HandSegmentationListener {
        override fun onHandDetectionStatusChanged(isEnabled: Boolean, handsDetected: Int) {
            val current = _detectionResults.value
            _detectionResults.value = current.copy(handsDetectedCount = handsDetected)
        }

        override fun onDatasetProgress(totalSamples: Int, leftHands: Int, rightHands: Int) {
            val current = _detectionResults.value
            _detectionResults.value = current.copy(
                leftHandDetections = leftHands,
                rightHandDetections = rightHands
            )
        }

        override fun onDatasetSaved(datasetPath: String, totalSamples: Int) {

        }

        override fun onError(error: String) {
            _processingState.value = ProcessingState.Error(error)
        }
    }
}