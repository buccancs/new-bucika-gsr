package com.topdon.tc001.recording

import android.content.Context
import android.graphics.Bitmap
import android.view.TextureView
import com.elvishew.xlog.XLog
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Parallel Capture Manager for simultaneous thermal and visual recording
 */
class ParallelCaptureManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ParallelCaptureManager"
        private const val THERMAL_FRAME_RATE = 30
        private const val VISUAL_FRAME_RATE = 30
        private const val SYNC_TOLERANCE_MS = 16 // ~1 frame at 60fps
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isCapturing = AtomicBoolean(false)
    
    private var thermalRecorder: EnhancedVideoRecorder? = null
    private var visualRecorder: EnhancedVideoRecorder? = null
    
    private var thermalView: TextureView? = null
    private var visualView: TextureView? = null
    
    private var syncTimestamps = mutableListOf<Long>()
    
    data class CaptureConfig(
        val thermalMode: EnhancedVideoRecorder.RecordingMode = EnhancedVideoRecorder.RecordingMode.THERMAL_640x480_30FPS,
        val visualMode: EnhancedVideoRecorder.RecordingMode = EnhancedVideoRecorder.RecordingMode.STANDARD_1080P_30FPS,
        val enableSync: Boolean = true,
        val outputDirectory: File? = null
    )
    
    fun initialize(thermalView: TextureView, visualView: TextureView) {
        this.thermalView = thermalView
        this.visualView = visualView
        
        thermalRecorder = EnhancedVideoRecorder(context)
        visualRecorder = EnhancedVideoRecorder(context)
        
        XLog.i(TAG, "Parallel capture manager initialized")
    }
    
    fun startParallelCapture(config: CaptureConfig): Boolean {
        if (isCapturing.get()) {
            XLog.w(TAG, "Parallel capture already in progress")
            return false
        }
        
        return try {
            val thermalStarted = thermalRecorder?.startRecording(config.thermalMode) ?: false
            val visualStarted = visualRecorder?.startRecording(config.visualMode) ?: false
            
            if (thermalStarted && visualStarted) {
                isCapturing.set(true)
                
                if (config.enableSync) {
                    startSynchronization()
                }
                
                XLog.i(TAG, "Parallel capture started successfully")
                true
            } else {
                // Stop any successful recording if one failed
                thermalRecorder?.stopRecording()
                visualRecorder?.stopRecording()
                
                XLog.e(TAG, "Failed to start parallel capture")
                false
            }
        } catch (e: Exception) {
            XLog.e(TAG, "Error starting parallel capture: ${e.message}", e)
            false
        }
    }
    
    fun stopParallelCapture(): Boolean {
        if (!isCapturing.get()) {
            XLog.w(TAG, "No parallel capture in progress")
            return false
        }
        
        return try {
            val thermalStopped = thermalRecorder?.stopRecording() ?: true
            val visualStopped = visualRecorder?.stopRecording() ?: true
            
            isCapturing.set(false)
            stopSynchronization()
            
            XLog.i(TAG, "Parallel capture stopped")
            thermalStopped && visualStopped
        } catch (e: Exception) {
            XLog.e(TAG, "Error stopping parallel capture: ${e.message}", e)
            false
        }
    }
    
    fun isCapturing(): Boolean = isCapturing.get()
    
    fun getParallelCaptureStats(): Map<String, Any> {
        val thermalStats = thermalRecorder?.getThermalCaptureStats() ?: emptyMap()
        val visualStats = visualRecorder?.getThermalCaptureStats() ?: emptyMap()
        
        return mapOf(
            "thermal" to thermalStats,
            "visual" to visualStats,
            "syncAccuracy" to calculateSyncAccuracy(),
            "totalFrames" to ((thermalStats["framesCaptured"] as? Int ?: 0) + 
                             (visualStats["framesCaptured"] as? Int ?: 0))
        )
    }
    
    fun getRecordedFiles(): Pair<List<File>, List<File>> {
        val thermalFiles = thermalRecorder?.getRecordedFiles() ?: emptyList()
        val visualFiles = visualRecorder?.getRecordedFiles() ?: emptyList()
        return Pair(thermalFiles, visualFiles)
    }
    
    private fun startSynchronization() {
        scope.launch {
            while (isCapturing.get()) {
                val timestamp = System.currentTimeMillis()
                syncTimestamps.add(timestamp)
                
                // Capture frames simultaneously
                captureSynchronizedFrame(timestamp)
                
                delay(1000 / THERMAL_FRAME_RATE.toLong()) // ~33ms for 30fps
            }
        }
    }
    
    private fun stopSynchronization() {
        scope.cancel()
    }
    
    private fun captureSynchronizedFrame(timestamp: Long) {
        // Implementation for synchronized frame capture
        // This would coordinate thermal and visual frame capture timing
        XLog.d(TAG, "Synchronized frame capture at timestamp: $timestamp")
    }
    
    private fun calculateSyncAccuracy(): Double {
        if (syncTimestamps.size < 2) return 0.0
        
        val intervals = syncTimestamps.zipWithNext { a, b -> b - a }
        val averageInterval = intervals.average()
        val variance = intervals.map { (it - averageInterval) * (it - averageInterval) }.average()
        
        return kotlin.math.sqrt(variance)
    }
    
    fun cleanup() {
        stopParallelCapture()
        scope.cancel()
        thermalRecorder?.cleanup()
        visualRecorder?.cleanup()
        
        thermalRecorder = null
        visualRecorder = null
        thermalView = null
        visualView = null
        
        XLog.i(TAG, "Parallel capture manager cleaned up")
    }
}