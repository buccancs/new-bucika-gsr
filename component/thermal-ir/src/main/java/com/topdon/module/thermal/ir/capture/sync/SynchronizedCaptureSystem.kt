package com.topdon.module.thermal.ir.capture.sync

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import com.elvishew.xlog.XLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * Synchronized Capture System for temporal alignment of concurrent 4K video and RAW DNG capture
 * Provides hardware-timestamp based synchronization for Samsung S22 research applications
 */
@SuppressLint("MissingPermission")
class SynchronizedCaptureSystem(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SynchronizedCaptureSystem"
        
        // Synchronization tolerances
        private const val MAX_TEMPORAL_DRIFT_NS = 16_666_666L // 16.67ms (0.5 frame @ 30fps)
        private const val SYNC_VALIDATION_INTERVAL_MS = 1000L // Check sync every second
        private const val TIMESTAMP_CORRELATION_WINDOW_NS = 33_333_333L // 33.33ms (1 frame @ 30fps)
    }
    
    // Master timestamp reference (hardware monotonic clock)
    private val masterClock = AtomicLong(0)
    private var captureStartTimeNs: Long = 0
    private var captureStartRealtimeMs: Long = 0
    
    // Frame correlation tracking
    private val videoFrameTimestamps = ConcurrentHashMap<Long, VideoFrameInfo>()
    private val dngFrameTimestamps = ConcurrentHashMap<Long, DNGFrameInfo>()
    
    // Synchronization metrics
    private var totalFramesPaired = 0
    private var averageTemporalDrift = 0.0
    private var maxTemporalDrift = 0L
    private var syncValidationCount = 0
    
    /**
     * Initialize the synchronized capture system
     */
    fun initialize(): Boolean {
        return try {
            // Reset all timing data
            masterClock.set(0)
            captureStartTimeNs = 0
            captureStartRealtimeMs = 0
            
            videoFrameTimestamps.clear()
            dngFrameTimestamps.clear()
            
            // Reset metrics
            totalFramesPaired = 0
            averageTemporalDrift = 0.0
            maxTemporalDrift = 0
            syncValidationCount = 0
            
            XLog.i(TAG, "Synchronized Capture System initialized")
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize sync system: ${e.message}", e)
            false
        }
    }
    
    /**
     * Start synchronized capture session
     */
    fun startSynchronizedCapture(): CaptureSessionInfo {
        captureStartTimeNs = SystemClock.elapsedRealtimeNanos()
        captureStartRealtimeMs = System.currentTimeMillis()
        masterClock.set(captureStartTimeNs)
        
        val sessionInfo = CaptureSessionInfo(
            sessionId = generateSessionId(),
            startTimeNs = captureStartTimeNs,
            startTimeMs = captureStartRealtimeMs
        )
        
        XLog.i(TAG, "Started synchronized capture session: ${sessionInfo.sessionId}")
        XLog.i(TAG, "Master clock reference: ${captureStartTimeNs}ns")
        
        return sessionInfo
    }
    
    /**
     * Register a video frame timestamp (called by video recorder)
     */
    fun registerVideoFrame(presentationTimeUs: Long): Long {
        val hardwareTimestampNs = SystemClock.elapsedRealtimeNanos()
        val relativeTimestampNs = hardwareTimestampNs - captureStartTimeNs
        
        val frameInfo = VideoFrameInfo(
            presentationTimeUs = presentationTimeUs,
            hardwareTimestampNs = hardwareTimestampNs,
            relativeTimestampNs = relativeTimestampNs,
            sequenceNumber = videoFrameTimestamps.size.toLong()
        )
        
        videoFrameTimestamps[hardwareTimestampNs] = frameInfo
        
        // Correlate with DNG frames if available
        tryCorrelateFrames(hardwareTimestampNs, isVideo = true)
        
        return hardwareTimestampNs
    }
    
    /**
     * Register a DNG frame timestamp (called by DNG capture manager)
     */
    fun registerDNGFrame(imageTimestampNs: Long, frameIndex: Int): Long {
        val hardwareTimestampNs = if (imageTimestampNs > 0) imageTimestampNs else SystemClock.elapsedRealtimeNanos()
        val relativeTimestampNs = hardwareTimestampNs - captureStartTimeNs
        
        val frameInfo = DNGFrameInfo(
            imageTimestampNs = imageTimestampNs,
            hardwareTimestampNs = hardwareTimestampNs,
            relativeTimestampNs = relativeTimestampNs,
            frameIndex = frameIndex,
            captureTimeMs = System.currentTimeMillis()
        )
        
        dngFrameTimestamps[hardwareTimestampNs] = frameInfo
        
        // Correlate with video frames if available
        tryCorrelateFrames(hardwareTimestampNs, isVideo = false)
        
        return hardwareTimestampNs
    }
    
    /**
     * Get temporal correlation info for a specific timestamp
     */
    fun getCorrelationInfo(timestampNs: Long): TemporalCorrelationInfo? {
        return findBestCorrelation(timestampNs)
    }
    
    /**
     * Get complete synchronization metrics
     */
    fun getSynchronizationMetrics(): SynchronizationMetrics {
        val currentTimeNs = SystemClock.elapsedRealtimeNanos()
        val sessionDurationMs = if (captureStartTimeNs > 0) {
            (currentTimeNs - captureStartTimeNs) / 1_000_000
        } else 0
        
        return SynchronizationMetrics(
            sessionDurationMs = sessionDurationMs,
            videoFramesRecorded = videoFrameTimestamps.size,
            dngFramesCaptured = dngFrameTimestamps.size,
            totalFramesPaired = totalFramesPaired,
            averageTemporalDriftNs = averageTemporalDrift,
            maxTemporalDriftNs = maxTemporalDrift,
            syncAccuracyPercent = calculateSyncAccuracy(),
            isWithinTolerance = maxTemporalDrift <= MAX_TEMPORAL_DRIFT_NS
        )
    }
    
    private fun tryCorrelateFrames(newTimestampNs: Long, isVideo: Boolean) {
        val otherFrames = if (isVideo) dngFrameTimestamps else videoFrameTimestamps
        
        // Find frames within correlation window
        val correlatedFrames = otherFrames.entries.filter { entry ->
            abs(entry.key - newTimestampNs) <= TIMESTAMP_CORRELATION_WINDOW_NS
        }
        
        if (correlatedFrames.isNotEmpty()) {
            // Find closest temporal match
            val closestMatch = correlatedFrames.minByOrNull { entry ->
                abs(entry.key - newTimestampNs)
            }
            
            closestMatch?.let { match ->
                val drift = abs(match.key - newTimestampNs)
                updateDriftMetrics(drift)
                totalFramesPaired++
                
                if (totalFramesPaired % 30 == 0) { // Log every second
                    XLog.d(TAG, "Frame correlation: ${totalFramesPaired} pairs, avg drift: %.2f ms"
                        .format(averageTemporalDrift / 1_000_000.0))
                }
            }
        }
    }
    
    private fun updateDriftMetrics(driftNs: Long) {
        maxTemporalDrift = maxOf(maxTemporalDrift, driftNs)
        
        // Update running average
        val totalSamples = totalFramesPaired + 1
        averageTemporalDrift = (averageTemporalDrift * totalFramesPaired + driftNs) / totalSamples
    }
    
    private fun calculateSyncAccuracy(): Double {
        return if (totalFramesPaired > 0) {
            val framesWithinTolerance = videoFrameTimestamps.size + dngFrameTimestamps.size - 
                (videoFrameTimestamps.size + dngFrameTimestamps.size - totalFramesPaired)
            val totalFrames = videoFrameTimestamps.size + dngFrameTimestamps.size
            (framesWithinTolerance.toDouble() / totalFrames) * 100.0
        } else 0.0
    }
    
    private fun findBestCorrelation(timestampNs: Long): TemporalCorrelationInfo? {
        val videoFrame = videoFrameTimestamps[timestampNs]
        val dngFrame = dngFrameTimestamps[timestampNs]
        
        if (videoFrame != null && dngFrame != null) {
            return TemporalCorrelationInfo(
                videoFrame = videoFrame,
                dngFrame = dngFrame,
                temporalDriftNs = abs(videoFrame.hardwareTimestampNs - dngFrame.hardwareTimestampNs),
                correlationQuality = CorrelationQuality.EXACT
            )
        }
        
        // Look for nearest matches
        val nearestVideo = findNearestVideoFrame(timestampNs)
        val nearestDNG = findNearestDNGFrame(timestampNs)
        
        if (nearestVideo != null && nearestDNG != null) {
            val drift = abs(nearestVideo.hardwareTimestampNs - nearestDNG.hardwareTimestampNs)
            val quality = when {
                drift <= MAX_TEMPORAL_DRIFT_NS / 4 -> CorrelationQuality.HIGH
                drift <= MAX_TEMPORAL_DRIFT_NS / 2 -> CorrelationQuality.MEDIUM
                drift <= MAX_TEMPORAL_DRIFT_NS -> CorrelationQuality.LOW
                else -> CorrelationQuality.POOR
            }
            
            return TemporalCorrelationInfo(
                videoFrame = nearestVideo,
                dngFrame = nearestDNG,
                temporalDriftNs = drift,
                correlationQuality = quality
            )
        }
        
        return null
    }
    
    private fun findNearestVideoFrame(timestampNs: Long): VideoFrameInfo? {
        return videoFrameTimestamps.entries.minByOrNull { entry ->
            abs(entry.key - timestampNs)
        }?.value
    }
    
    private fun findNearestDNGFrame(timestampNs: Long): DNGFrameInfo? {
        return dngFrameTimestamps.entries.minByOrNull { entry ->
            abs(entry.key - timestampNs)
        }?.value
    }
    
    private fun generateSessionId(): String {
        return "sync_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
    
    /**
     * Cleanup sync system resources
     */
    fun cleanup() {
        videoFrameTimestamps.clear()
        dngFrameTimestamps.clear()
        XLog.i(TAG, "Synchronized Capture System cleaned up")
    }
}

/**
 * Capture session information
 */
data class CaptureSessionInfo(
    val sessionId: String,
    val startTimeNs: Long,
    val startTimeMs: Long
)

/**
 * Video frame timing information
 */
data class VideoFrameInfo(
    val presentationTimeUs: Long,
    val hardwareTimestampNs: Long,
    val relativeTimestampNs: Long,
    val sequenceNumber: Long
)

/**
 * DNG frame timing information
 */
data class DNGFrameInfo(
    val imageTimestampNs: Long,
    val hardwareTimestampNs: Long,
    val relativeTimestampNs: Long,
    val frameIndex: Int,
    val captureTimeMs: Long
)

/**
 * Temporal correlation between video and DNG frames
 */
data class TemporalCorrelationInfo(
    val videoFrame: VideoFrameInfo,
    val dngFrame: DNGFrameInfo,
    val temporalDriftNs: Long,
    val correlationQuality: CorrelationQuality
)

/**
 * Quality rating for frame correlation
 */
enum class CorrelationQuality {
    EXACT,    // Perfect timestamp match
    HIGH,     // < 4ms drift
    MEDIUM,   // < 8ms drift  
    LOW,      // < 16ms drift
    POOR      // > 16ms drift
}

/**
 * Complete synchronization metrics
 */
data class SynchronizationMetrics(
    val sessionDurationMs: Long,
    val videoFramesRecorded: Int,
    val dngFramesCaptured: Int,
    val totalFramesPaired: Int,
    val averageTemporalDriftNs: Double,
    val maxTemporalDriftNs: Long,
    val syncAccuracyPercent: Double,
    val isWithinTolerance: Boolean
)