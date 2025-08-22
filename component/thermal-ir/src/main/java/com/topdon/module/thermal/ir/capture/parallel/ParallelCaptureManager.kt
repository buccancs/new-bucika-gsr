package com.topdon.module.thermal.ir.capture.parallel

import android.annotation.SuppressLint
import android.content.Context
import com.elvishew.xlog.XLog
import com.topdon.module.thermal.ir.capture.video.EnhancedVideoRecorder
import com.topdon.module.thermal.ir.capture.raw.DNGCaptureManager
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
    private var videoRecorder: EnhancedVideoRecorder? = null
    private var dngCaptureManager: DNGCaptureManager? = null
    
    // State
    private var isRecording = false
    private var recordingStartTime: Long = 0
    
    /**
     * Initialize the parallel capture manager
     */
    fun initialize(videoRecorder: EnhancedVideoRecorder): Boolean {
        this.videoRecorder = videoRecorder
        this.dngCaptureManager = DNGCaptureManager(context)
        
        XLog.i(TAG, "Parallel Capture Manager initialized")
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
     * Start parallel 4K video + RAW DNG capture
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
        
        XLog.i(TAG, "Starting parallel 4K + RAW DNG capture...")
        recordingStartTime = System.currentTimeMillis()
        
        return try {
            // Start video recording first
            val videoSuccess = videoRecorder?.startRecording(EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS) ?: false
            if (!videoSuccess) {
                XLog.e(TAG, "Failed to start 4K video recording")
                return false
            }
            
            // Start concurrent RAW DNG capture
            val rawSuccess = dngCaptureManager?.startConcurrentDNGCapture() ?: false
            if (!rawSuccess) {
                XLog.e(TAG, "Failed to start concurrent RAW DNG capture")
                // Stop video recording since parallel mode failed
                videoRecorder?.stopRecording()
                return false
            }
            
            isRecording = true
            XLog.i(TAG, "Parallel 4K + RAW DNG capture started successfully")
            true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start parallel capture: ${e.message}", e)
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
            XLog.i(TAG, "Stopping parallel capture after ${recordingDuration / 1000}s")
            
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
            
            isRecording = false
            XLog.i(TAG, "Parallel capture stopped")
            rawStopped && videoStopped
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error stopping parallel capture: ${e.message}", e)
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
     * Get performance metrics for current session
     */
    fun getPerformanceMetrics(): ParallelCaptureMetrics {
        val duration = getRecordingDuration()
        val optimizationParams = compatibilityChecker.getSamsungS22OptimizationParams()
        
        return ParallelCaptureMetrics(
            isRecording = isRecording,
            durationMs = duration,
            estimated4KFrames = if (duration > 0) (duration * 30 / 1000).toInt() else 0,
            estimatedRawFrames = if (duration > 0) (duration * 30 / 1000).toInt() else 0,
            maxConcurrentStreams = optimizationParams.maxConcurrentStreams,
            actualBitrate = optimizationParams.recommended4KBitrate,
            hardwareAccelerated = optimizationParams.enableHardwareAcceleration
        )
    }
}

/**
 * Performance metrics for parallel capture session
 */
data class ParallelCaptureMetrics(
    val isRecording: Boolean,
    val durationMs: Long,
    val estimated4KFrames: Int,
    val estimatedRawFrames: Int,
    val maxConcurrentStreams: Int,
    val actualBitrate: Int,
    val hardwareAccelerated: Boolean
)