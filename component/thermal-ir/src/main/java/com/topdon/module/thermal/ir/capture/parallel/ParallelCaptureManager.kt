package com.topdon.module.thermal.ir.capture.parallel

import android.annotation.SuppressLint
import android.content.Context
import com.elvishew.xlog.XLog
import com.topdon.module.thermal.ir.capture.video.EnhancedVideoRecorder
import com.topdon.module.thermal.ir.capture.raw.DNGCaptureManager
import com.topdon.module.thermal.ir.capture.sync.SynchronizedCaptureSystem
import com.topdon.module.thermal.ir.capture.sync.CaptureSessionInfo
import com.topdon.module.thermal.ir.capture.sync.SynchronizationMetrics
import com.topdon.module.thermal.ir.device.compatibility.DeviceCompatibilityChecker
import com.topdon.module.thermal.ir.device.compatibility.CaptureCompatibilityResult

/**
 * Parallel Capture Manager for simultaneous 4K video and RAW DNG capture
 * Optimized for Samsung S22 series concurrent capture capabilities
 */
@SuppressLint("MissingPermission")
class ParallelCaptureManager(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ParallelCaptureManager"
    }
    
    // Components
    private val compatibilityChecker = DeviceCompatibilityChecker(context)
    private val syncSystem = SynchronizedCaptureSystem(context)
    private var videoRecorder: EnhancedVideoRecorder? = null
    private var dngCaptureManager: DNGCaptureManager? = null
    
    // Synchronization state
    private var currentSession: CaptureSessionInfo? = null
    
    // State
    private var isRecording = false
    private var recordingStartTime: Long = 0
    
    /**
     * Initialize the parallel capture manager
     */
    fun initialize(videoRecorder: EnhancedVideoRecorder): Boolean {
        this.videoRecorder = videoRecorder
        this.dngCaptureManager = DNGCaptureManager(context)
        
        // Initialize synchronization system
        val syncInitialized = syncSystem.initialize()
        if (!syncInitialized) {
            XLog.e(TAG, "Failed to initialize synchronization system")
            return false
        }
        
        // Pass sync system to capture managers for integration
        this.dngCaptureManager?.setSynchronizationSystem(syncSystem)
        
        XLog.i(TAG, "Parallel Capture Manager initialized with synchronization")
        XLog.i(TAG, "Device compatibility: ${getCompatibilityReport()}")
        
        return true
    }
    
    /**
     * Check if parallel 4K + RAW capture is supported
     */
    fun isParallelCaptureSupported(): Boolean {
        return compatibilityChecker.supportsConcurrent4KAndRaw()
    }
    
    /**
     * Get detailed compatibility report
     */
    fun getCompatibilityReport(): String {
        val result = compatibilityChecker.validateConcurrentConfiguration(
            enable4K = true,
            enableRaw = true,
            targetFps = 30
        )
        
        return buildString {
            appendLine("Parallel Capture Compatibility Report:")
            appendLine("- Device: ${android.os.Build.MODEL}")
            appendLine("- Samsung S22: ${compatibilityChecker.isSamsungS22()}")
            appendLine("- Supported: ${result.isSupported}")
            if (result.issues.isNotEmpty()) {
                appendLine("- Issues:")
                result.issues.forEach { issue ->
                    appendLine("  • $issue")
                }
            }
            appendLine("- Max Concurrent Streams: ${compatibilityChecker.getMaxConcurrentStreams()}")
            appendLine("- Recommended 4K Bitrate: ${result.optimizationParams.recommended4KBitrate / 1_000_000} Mbps")
            appendLine("- Max RAW Buffer: ${result.optimizationParams.maxRawBufferSize}")
            appendLine("- Hardware Acceleration: ${result.optimizationParams.enableHardwareAcceleration}")
        }
    }
    
    /**
     * Start parallel 4K video + RAW DNG capture with synchronized timestamps
     */
    fun startParallelCapture(): Boolean {
        if (isRecording) {
            XLog.w(TAG, "Parallel capture already in progress")
            return false
        }
        
        // Validate compatibility first
        val compatibility = compatibilityChecker.validateConcurrentConfiguration(
            enable4K = true,
            enableRaw = true,
            targetFps = 30
        )
        
        if (!compatibility.isSupported) {
            XLog.e(TAG, "Parallel capture not supported: ${compatibility.issues.joinToString("; ")}")
            return false
        }
        
        // Initialize synchronized capture session
        currentSession = syncSystem.startSynchronizedCapture()
        XLog.i(TAG, "Starting parallel 4K + RAW DNG capture with synchronization...")
        XLog.i(TAG, "Session: ${currentSession?.sessionId}, Start time: ${currentSession?.startTimeNs}ns")
        
        recordingStartTime = System.currentTimeMillis()
        
        return try {
            // Start video recording first with sync system integration
            val videoSuccess = videoRecorder?.startRecording(
                EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS,
                syncSystem
            ) ?: false
            
            if (!videoSuccess) {
                XLog.e(TAG, "Failed to start synchronized 4K video recording")
                currentSession = null
                return false
            }
            
            // Start concurrent RAW DNG capture with sync system integration
            val rawSuccess = dngCaptureManager?.startConcurrentDNGCapture() ?: false
            if (!rawSuccess) {
                XLog.e(TAG, "Failed to start synchronized concurrent RAW DNG capture")
                // Stop video recording since parallel mode failed
                videoRecorder?.stopRecording()
                currentSession = null
                return false
            }
            
            isRecording = true
            XLog.i(TAG, "Synchronized parallel 4K + RAW DNG capture started successfully")
            XLog.i(TAG, "Session ID: ${currentSession?.sessionId}")
            true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start synchronized parallel capture: ${e.message}", e)
            stopParallelCapture()
            false
        }
    }
    
    /**
     * Stop parallel capture
     */
    fun stopParallelCapture(): Boolean {
        if (!isRecording) {
            XLog.w(TAG, "No parallel capture in progress")
            return false
        }
        
        return try {
            val recordingDuration = System.currentTimeMillis() - recordingStartTime
            XLog.i(TAG, "Stopping synchronized parallel capture after ${recordingDuration / 1000}s")
            
            // Get final synchronization metrics before stopping
            val finalMetrics = getSynchronizationMetrics()
            XLog.i(TAG, "Final sync metrics: $finalMetrics")
            
            // Stop DNG capture first
            val rawStopped = dngCaptureManager?.stopDNGCapture() ?: false
            if (!rawStopped) {
                XLog.w(TAG, "Failed to stop RAW DNG capture cleanly")
            }
            
            // Stop video recording
            val videoStopped = videoRecorder?.stopRecording() ?: false
            if (!videoStopped) {
                XLog.w(TAG, "Failed to stop video recording cleanly")
            }
            
            // Clear session
            currentSession = null
            isRecording = false
            XLog.i(TAG, "Synchronized parallel capture stopped")
            rawStopped && videoStopped
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error stopping synchronized parallel capture: ${e.message}", e)
            currentSession = null
            isRecording = false
            false
        }
    }
    
    /**
     * Get current recording status
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Get recording duration in milliseconds
     */
    fun getRecordingDuration(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0
        }
    }
    
    /**
     * Get performance metrics for current session including synchronization data
     */
    fun getPerformanceMetrics(): ParallelCaptureMetrics {
        val duration = getRecordingDuration()
        val optimizationParams = compatibilityChecker.getSamsungS22OptimizationParams()
        val syncMetrics = getSynchronizationMetrics()
        
        return ParallelCaptureMetrics(
            isRecording = isRecording,
            durationMs = duration,
            estimated4KFrames = if (duration > 0) (duration * 30 / 1000).toInt() else 0,
            estimatedRawFrames = if (duration > 0) (duration * 30 / 1000).toInt() else 0,
            actualVideoFrames = syncMetrics.videoFramesRecorded,
            actualDngFrames = syncMetrics.dngFramesCaptured,
            synchronizedFramePairs = syncMetrics.totalFramesPaired,
            averageTemporalDriftMs = syncMetrics.averageTemporalDriftNs / 1_000_000.0,
            maxTemporalDriftMs = syncMetrics.maxTemporalDriftNs / 1_000_000.0,
            syncAccuracyPercent = syncMetrics.syncAccuracyPercent,
            isWithinSyncTolerance = syncMetrics.isWithinTolerance,
            maxConcurrentStreams = optimizationParams.maxConcurrentStreams,
            actualBitrate = optimizationParams.recommended4KBitrate,
            hardwareAccelerated = optimizationParams.enableHardwareAcceleration,
            sessionId = currentSession?.sessionId ?: "no-session"
        )
    }
    
    /**
     * Get current synchronization metrics
     */
    fun getSynchronizationMetrics(): SynchronizationMetrics {
        return syncSystem.getSynchronizationMetrics()
    }
    
    /**
     * Get detailed synchronization report
     */
    fun getSynchronizationReport(): String {
        val metrics = getSynchronizationMetrics()
        
        return buildString {
            appendLine("Synchronized Capture Report:")
            appendLine("- Session: ${currentSession?.sessionId ?: "none"}")
            appendLine("- Duration: ${metrics.sessionDurationMs / 1000.0}s")
            appendLine("- Video frames: ${metrics.videoFramesRecorded}")
            appendLine("- DNG frames: ${metrics.dngFramesCaptured}")
            appendLine("- Synchronized pairs: ${metrics.totalFramesPaired}")
            appendLine("- Average drift: %.2f ms".format(metrics.averageTemporalDriftNs / 1_000_000.0))
            appendLine("- Max drift: %.2f ms".format(metrics.maxTemporalDriftNs / 1_000_000.0))
            appendLine("- Sync accuracy: %.1f%%".format(metrics.syncAccuracyPercent))
            appendLine("- Within tolerance: ${if (metrics.isWithinTolerance) "YES" else "NO"}")
        }
    }
}

/**
 * Performance metrics for parallel capture session with synchronization data
 */
data class ParallelCaptureMetrics(
    val isRecording: Boolean,
    val durationMs: Long,
    val estimated4KFrames: Int,
    val estimatedRawFrames: Int,
    val actualVideoFrames: Int,
    val actualDngFrames: Int,
    val synchronizedFramePairs: Int,
    val averageTemporalDriftMs: Double,
    val maxTemporalDriftMs: Double,
    val syncAccuracyPercent: Double,
    val isWithinSyncTolerance: Boolean,
    val maxConcurrentStreams: Int,
    val actualBitrate: Int,
    val hardwareAccelerated: Boolean,
    val sessionId: String
)