package com.topdon.tc001.integration

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.topdon.module.thermal.ir.capture.parallel.ParallelCaptureManager
import com.topdon.module.thermal.ir.capture.video.EnhancedVideoRecorder
import com.topdon.module.thermal.ir.capture.sync.EnhancedSynchronizedCaptureSystem
import com.topdon.tc001.gsr.EnhancedGSRManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Comprehensive Multi-Modal Synchronized Recording System
 * Integrates 4K video, RAW DNG, and GSR data with nanosecond-precision synchronization
 * Optimized for Samsung S22 series with zero frame drop architecture
 */
class SynchronizedMultiModalRecorder(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SyncMultiModalRecorder"
        private const val METRICS_UPDATE_INTERVAL_MS = 1000L
    }
    
    // Core recording components
    private lateinit var parallelCaptureManager: ParallelCaptureManager
    private lateinit var videoRecorder: EnhancedVideoRecorder
    private lateinit var gsrManager: EnhancedGSRManager
    private lateinit var syncSystem: EnhancedSynchronizedCaptureSystem
    
    // State management
    private val isRecording = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    private var recordingStartTime: Long = 0
    
    // UI updates
    private val handler = Handler(Looper.getMainLooper())
    private var metricsUpdateRunnable: Runnable? = null
    
    // Listener interface for recording updates
    interface MultiModalRecordingListener {
        fun onRecordingStarted(sessionId: String)
        fun onRecordingStopped(duration: Long, metrics: RecordingSessionMetrics)
        fun onSyncMetricsUpdated(metrics: RecordingSessionMetrics)
        fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemp: Double)
        fun onVideoFrameRecorded(frameIndex: Int, timestamp: Long)
        fun onDNGFrameCaptured(frameIndex: Int, timestamp: Long)
        fun onError(error: String, exception: Exception?)
    }
    
    private var recordingListener: MultiModalRecordingListener? = null
    
    /**
     * Set listener for recording events and updates
     */
    fun setRecordingListener(listener: MultiModalRecordingListener) {
        this.recordingListener = listener
    }
    
    /**
     * Initialize the multi-modal recording system
     */
    fun initialize(): Boolean {
        return try {
            XLog.i(TAG, "Initializing synchronized multi-modal recording system...")
            
            // Initialize global master clock first
            EnhancedSynchronizedCaptureSystem.initializeGlobalMasterClock()
            
            // Initialize core components
            syncSystem = EnhancedSynchronizedCaptureSystem(context)
            videoRecorder = EnhancedVideoRecorder(context)
            gsrManager = EnhancedGSRManager.getInstance(context)
            parallelCaptureManager = ParallelCaptureManager(context)
            
            // Initialize synchronization system
            if (!syncSystem.initialize()) {
                throw Exception("Failed to initialize synchronization system")
            }
            
            // Initialize parallel capture manager
            if (!parallelCaptureManager.initialize(videoRecorder)) {
                throw Exception("Failed to initialize parallel capture manager")
            }
            
            // Initialize GSR manager with sync system
            gsrManager.setSynchronizationSystem(syncSystem)
            gsrManager.initializeShimmer()
            
            // Set up GSR data listener
            gsrManager.setGSRDataListener(object : EnhancedGSRManager.EnhancedGSRDataListener {
                override fun onGSRDataReceived(
                    timestamp: Long,
                    synchronizedTimestamp: Long,
                    gsrValue: Double,
                    skinTemperature: Double,
                    sampleIndex: Long
                ) {
                    recordingListener?.onGSRDataReceived(synchronizedTimestamp, gsrValue, skinTemperature)
                }
                
                override fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?) {
                    XLog.i(TAG, "GSR connection status: $isConnected ($deviceName)")
                }
                
                override fun onSyncQualityChanged(syncAccuracy: Double, temporalDrift: Double) {
                    if (syncAccuracy < 90.0 || temporalDrift > 10.0) {
                        XLog.w(TAG, "Sync quality degraded: accuracy=%.1f%%, drift=%.2fms"
                            .format(syncAccuracy, temporalDrift))
                    }
                }
            })
            
            // Check device compatibility
            if (!parallelCaptureManager.isParallelCaptureSupported()) {
                XLog.w(TAG, "Device may not fully support concurrent 4K+RAW capture")
                XLog.i(TAG, "Compatibility report:\n${parallelCaptureManager.getCompatibilityReport()}")
            }
            
            isInitialized.set(true)
            XLog.i(TAG, "Multi-modal recording system initialized successfully")
            true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize multi-modal recording system: ${e.message}", e)
            recordingListener?.onError("Initialization failed", e)
            false
        }
    }
    
    /**
     * Start synchronized multi-modal recording
     */
    fun startRecording(): Boolean {
        if (!isInitialized.get()) {
            XLog.e(TAG, "Cannot start recording - system not initialized")
            return false
        }
        
        if (isRecording.get()) {
            XLog.w(TAG, "Recording already in progress")
            return false
        }
        
        return try {
            XLog.i(TAG, "Starting synchronized multi-modal recording...")
            
            // Connect GSR device if not already connected
            if (!gsrManager.isConnected()) {
                gsrManager.connectToShimmer("00:06:66:66:66:66") // Simulated address
                
                // Wait for connection
                var connectionTimeout = 0
                while (!gsrManager.isConnected() && connectionTimeout < 30) {
                    Thread.sleep(100)
                    connectionTimeout++
                }
                
                if (!gsrManager.isConnected()) {
                    XLog.w(TAG, "GSR device connection timeout - proceeding without GSR")
                }
            }
            
            recordingStartTime = EnhancedSynchronizedCaptureSystem.getGlobalMasterClock()
            
            // Start parallel video + DNG capture
            if (!parallelCaptureManager.startParallelCapture()) {
                throw Exception("Failed to start parallel capture")
            }
            
            // Start GSR recording if connected
            if (gsrManager.isConnected()) {
                if (!gsrManager.startRecording()) {
                    XLog.w(TAG, "Failed to start GSR recording - continuing with video/DNG only")
                }
            }
            
            isRecording.set(true)
            
            // Start metrics monitoring
            startMetricsMonitoring()
            
            val sessionId = "multimodal_${System.currentTimeMillis()}"
            recordingListener?.onRecordingStarted(sessionId)
            
            XLog.i(TAG, "Synchronized multi-modal recording started successfully")
            XLog.i(TAG, "Session: $sessionId")
            XLog.i(TAG, "Components: Video=${parallelCaptureManager.isRecording()}, GSR=${gsrManager.isRecording()}")
            
            true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start multi-modal recording: ${e.message}", e)
            recordingListener?.onError("Recording start failed", e)
            stopRecording() // Cleanup partial state
            false
        }
    }
    
    /**
     * Stop synchronized multi-modal recording
     */
    fun stopRecording(): Boolean {
        if (!isRecording.get()) {
            XLog.w(TAG, "No recording in progress")
            return false
        }
        
        return try {
            XLog.i(TAG, "Stopping synchronized multi-modal recording...")
            
            // Stop metrics monitoring
            stopMetricsMonitoring()
            
            // Stop GSR recording
            val gsrStopped = if (gsrManager.isRecording()) {
                gsrManager.stopRecording()
            } else true
            
            // Stop parallel capture
            val parallelStopped = parallelCaptureManager.stopParallelCapture()
            
            // Calculate recording duration
            val recordingDuration = if (recordingStartTime > 0) {
                (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
            } else 0L
            
            isRecording.set(false)
            
            // Collect final metrics
            val finalMetrics = collectRecordingMetrics(recordingDuration)
            
            XLog.i(TAG, "Multi-modal recording stopped")
            XLog.i(TAG, "Duration: ${recordingDuration}ms")
            XLog.i(TAG, "Final metrics: $finalMetrics")
            
            recordingListener?.onRecordingStopped(recordingDuration, finalMetrics)
            
            gsrStopped && parallelStopped
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error stopping multi-modal recording: ${e.message}", e)
            isRecording.set(false)
            recordingListener?.onError("Recording stop failed", e)
            false
        }
    }
    
    /**
     * Start real-time metrics monitoring
     */
    private fun startMetricsMonitoring() {
        metricsUpdateRunnable = object : Runnable {
            override fun run() {
                if (isRecording.get()) {
                    try {
                        val currentDuration = if (recordingStartTime > 0) {
                            (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
                        } else 0L
                        
                        val metrics = collectRecordingMetrics(currentDuration)
                        recordingListener?.onSyncMetricsUpdated(metrics)
                        
                        // Schedule next update
                        handler.postDelayed(this, METRICS_UPDATE_INTERVAL_MS)
                        
                    } catch (e: Exception) {
                        XLog.e(TAG, "Error updating metrics: ${e.message}")
                    }
                }
            }
        }
        
        handler.post(metricsUpdateRunnable!!)
    }
    
    /**
     * Stop metrics monitoring
     */
    private fun stopMetricsMonitoring() {
        metricsUpdateRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            metricsUpdateRunnable = null
        }
    }
    
    /**
     * Collect comprehensive recording metrics
     */
    private fun collectRecordingMetrics(durationMs: Long): RecordingSessionMetrics {
        val parallelMetrics = parallelCaptureManager.getPerformanceMetrics()
        val syncMetrics = parallelCaptureManager.getSynchronizationMetrics()
        val gsrStats = gsrManager.getSamplingStatistics()
        
        return RecordingSessionMetrics(
            sessionDurationMs = durationMs,
            videoFramesRecorded = syncMetrics.videoFramesRecorded,
            dngFramesCaptured = syncMetrics.dngFramesCaptured,
            gsrSamplesCollected = gsrStats.totalSamples,
            synchronizedFramePairs = syncMetrics.totalFramesPaired,
            gsrSamplesSynchronized = syncMetrics.totalGSRSamplesSynced,
            averageTemporalDriftMs = syncMetrics.averageTemporalDriftNs / 1_000_000.0,
            maxTemporalDriftMs = syncMetrics.maxTemporalDriftNs / 1_000_000.0,
            syncAccuracyPercent = syncMetrics.syncAccuracyPercent,
            isWithinSyncTolerance = syncMetrics.isWithinTolerance,
            frameDropCount = syncMetrics.frameDropCount,
            gsrSamplingAccuracy = gsrStats.samplingAccuracy,
            actualGSRSamplingRate = gsrStats.actualSamplingRateHz,
            averageVideoProcessingTimeMs = syncMetrics.averageVideoProcessingTimeMs,
            averageDngProcessingTimeMs = syncMetrics.averageDngProcessingTimeMs,
            averageGsrProcessingTimeMs = syncMetrics.averageGsrProcessingTimeMs,
            correlationTaskQueueSize = syncMetrics.correlationTaskQueueSize
        )
    }
    
    /**
     * Get current recording status
     */
    fun isRecording(): Boolean = isRecording.get()
    
    /**
     * Get current recording duration in milliseconds
     */
    fun getRecordingDuration(): Long {
        return if (isRecording.get() && recordingStartTime > 0) {
            (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
        } else 0L
    }
    
    /**
     * Cleanup all resources
     */
    fun cleanup() {
        try {
            stopRecording()
            
            stopMetricsMonitoring()
            
            if (isInitialized.get()) {
                gsrManager.cleanup()
                syncSystem.cleanup()
            }
            
            isInitialized.set(false)
            
            XLog.i(TAG, "Multi-modal recording system cleaned up")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
}

/**
 * Comprehensive recording session metrics
 */
data class RecordingSessionMetrics(
    val sessionDurationMs: Long,
    val videoFramesRecorded: Int,
    val dngFramesCaptured: Int,
    val gsrSamplesCollected: Long,
    val synchronizedFramePairs: Int,
    val gsrSamplesSynchronized: Int,
    val averageTemporalDriftMs: Double,
    val maxTemporalDriftMs: Double,
    val syncAccuracyPercent: Double,
    val isWithinSyncTolerance: Boolean,
    val frameDropCount: Int,
    val gsrSamplingAccuracy: Double,
    val actualGSRSamplingRate: Double,
    val averageVideoProcessingTimeMs: Double,
    val averageDngProcessingTimeMs: Double,
    val averageGsrProcessingTimeMs: Double,
    val correlationTaskQueueSize: Int
) {
    /**
     * Get overall system performance score (0-100)
     */
    val overallPerformanceScore: Double
        get() {
            var score = 100.0
            
            // Deduct for sync accuracy
            if (syncAccuracyPercent < 95.0) score -= (95.0 - syncAccuracyPercent)
            
            // Deduct for frame drops
            if (frameDropCount > 0) score -= (frameDropCount * 2.0)
            
            // Deduct for high temporal drift
            if (averageTemporalDriftMs > 4.0) score -= (averageTemporalDriftMs - 4.0) * 5.0
            
            // Deduct for GSR sampling issues
            if (gsrSamplingAccuracy < 95.0) score -= (95.0 - gsrSamplingAccuracy) * 0.5
            
            // Deduct for processing delays
            if (averageVideoProcessingTimeMs > 2.0) score -= (averageVideoProcessingTimeMs - 2.0) * 2.0
            if (averageDngProcessingTimeMs > 5.0) score -= (averageDngProcessingTimeMs - 5.0) * 1.0
            
            return maxOf(0.0, minOf(100.0, score))
        }
    
    /**
     * Get performance category
     */
    val performanceCategory: String
        get() = when {
            overallPerformanceScore >= 95.0 -> "EXCELLENT"
            overallPerformanceScore >= 85.0 -> "GOOD"
            overallPerformanceScore >= 75.0 -> "ACCEPTABLE"
            overallPerformanceScore >= 60.0 -> "POOR"
            else -> "CRITICAL"
        }
}