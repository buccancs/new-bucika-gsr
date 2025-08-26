package com.topdon.thermal.capture.parallel

import android.annotation.SuppressLint
import android.content.Context
import com.elvishew.xlog.XLog
import com.topdon.thermal.capture.video.EnhancedVideoRecorder
import com.topdon.thermal.capture.raw.DNGCaptureManager
import com.topdon.thermal.capture.sync.EnhancedSynchronizedCaptureSystem
import com.topdon.thermal.capture.sync.EnhancedSynchronizationMetrics
import com.topdon.thermal.capture.sync.CaptureSessionInfo
import com.topdon.thermal.device.compatibility.DeviceCompatibilityChecker
import com.topdon.thermal.device.compatibility.CaptureCompatibilityResult

@SuppressLint("MissingPermission")
class ParallelCaptureManager(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ParallelCaptureManager"
    }
    
    private val compatibilityChecker = DeviceCompatibilityChecker(context)
    private val syncSystem = EnhancedSynchronizedCaptureSystem(context)
    private var videoRecorder: EnhancedVideoRecorder? = null
    private var dngCaptureManager: DNGCaptureManager? = null
    
    private var currentSession: CaptureSessionInfo? = null
    
    private var isRecording = false
    private var recordingStartTime: Long = 0
    
    fun initialize(videoRecorder: EnhancedVideoRecorder): Boolean {
        this.videoRecorder = videoRecorder
        this.dngCaptureManager = DNGCaptureManager(context)
        
        val syncInitialized = syncSystem.initialize()
        if (!syncInitialized) {
            XLog.e(TAG, "Failed to initialize synchronization system")
            return false
        }
        
        this.dngCaptureManager?.setSynchronizationSystem(syncSystem)
        
        XLog.i(TAG, "Parallel Capture Manager initialized with synchronization")
        XLog.i(TAG, "Device compatibility: ${getCompatibilityReport()}")
        
        return true
    }
    
    fun isParallelCaptureSupported(): Boolean {
        return compatibilityChecker.supportsConcurrent4KAndRaw()
    }
    
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
    
    fun startParallelCapture(): Boolean {
        if (isRecording) {
            XLog.w(TAG, "Parallel capture already in progress")
            return false
        }
        
        val compatibility = compatibilityChecker.validateConcurrentConfiguration(
            enable4K = true,
            enableRaw = true,
            targetFps = 30
        )
        
        if (!compatibility.isSupported) {
            XLog.e(TAG, "Parallel capture not supported: ${compatibility.issues.joinToString("; ")}")
            return false
        }
        
        currentSession = syncSystem.startSynchronizedCapture()
        XLog.i(TAG, "Starting parallel 4K + RAW DNG capture with synchronization...")
        XLog.i(TAG, "Session: ${currentSession?.sessionId}, Start time: ${currentSession?.startTimeNs}ns")
        
        recordingStartTime = System.currentTimeMillis()
        
        return try {

            val videoSuccess = videoRecorder?.startRecording(
                EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS,
                syncSystem
            ) ?: false
            
            if (!videoSuccess) {
                XLog.e(TAG, "Failed to start synchronized 4K video recording")
                currentSession = null
                return false
            }
            
            val rawSuccess = dngCaptureManager?.startConcurrentDNGCapture() ?: false
            if (!rawSuccess) {
                XLog.e(TAG, "Failed to start synchronized concurrent RAW DNG capture")

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
    
    fun stopParallelCapture(): Boolean {
        if (!isRecording) {
            XLog.w(TAG, "No parallel capture in progress")
            return false
        }
        
        return try {
            val recordingDuration = System.currentTimeMillis() - recordingStartTime
            XLog.i(TAG, "Stopping synchronized parallel capture after ${recordingDuration / 1000}s")
            
            val finalMetrics = getSynchronizationMetrics()
            XLog.i(TAG, "Final sync metrics: $finalMetrics")
            
            val rawStopped = dngCaptureManager?.stopDNGCapture() ?: false
            if (!rawStopped) {
                XLog.w(TAG, "Failed to stop RAW DNG capture cleanly")
            }
            
            val videoStopped = videoRecorder?.stopRecording() ?: false
            if (!videoStopped) {
                XLog.w(TAG, "Failed to stop video recording cleanly")
            }
            
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
    
    fun isRecording(): Boolean = isRecording
    
    fun getRecordingDuration(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0
        }
    }
    
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
    
    fun getSynchronizationMetrics(): EnhancedSynchronizationMetrics {
        return syncSystem.getSynchronizationMetrics()
    }
    
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
