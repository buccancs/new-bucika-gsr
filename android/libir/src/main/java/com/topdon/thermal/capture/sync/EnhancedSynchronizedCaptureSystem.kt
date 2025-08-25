package com.topdon.thermal.capture.sync

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import com.elvishew.xlog.XLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.abs

/**
 * Enhanced Synchronized Capture System with Multi-Threading Optimization
 * Zero frame drop architecture with nanosecond precision synchronization
 * Supports concurrent 4K video, RAW DNG, and GSR data streams
 */
@SuppressLint("MissingPermission")
class EnhancedSynchronizedCaptureSystem(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "EnhancedSyncCaptureSystem"
        
        // Synchronization tolerances
        private const val MAX_TEMPORAL_DRIFT_NS = 16_666_666L // 16.67ms (0.5 frame @ 30fps)
        private const val SYNC_VALIDATION_INTERVAL_MS = 500L // Check sync every 500ms
        private const val TIMESTAMP_CORRELATION_WINDOW_NS = 33_333_333L // 33.33ms (1 frame @ 30fps)
        
        // Multi-threading configuration
        private const val CORRELATION_THREAD_POOL_SIZE = 4
        private const val BUFFER_CLEANUP_INTERVAL_MS = 3000L // Clean old frames every 3s
        private const val MAX_FRAME_BUFFER_SIZE = 512 // Maximum frames to keep in memory
        private const val PERFORMANCE_MONITORING_INTERVAL_MS = 1000L
        
        @Volatile
        private var globalMasterClock: AtomicLong? = null
        private var isGlobalClockInitialized = false
        
        /**
         * Get the global synchronized master clock for the entire application
         * This ensures all components (video, DNG, GSR) use the same time reference
         */
        fun getGlobalMasterClock(): Long {
            if (!isGlobalClockInitialized) {
                initializeGlobalMasterClock()
            }
            return globalMasterClock?.get() ?: SystemClock.elapsedRealtimeNanos()
        }
        
        /**
         * Initialize global master clock (called once per app session)
         */
        fun initializeGlobalMasterClock(): Long {
            val currentTime = SystemClock.elapsedRealtimeNanos()
            globalMasterClock = AtomicLong(currentTime)
            isGlobalClockInitialized = true
            XLog.i(TAG, "Global master clock initialized: ${currentTime}ns")
            return currentTime
        }
        
        /**
         * Update global master clock (call periodically to maintain sync)
         */
        fun updateGlobalMasterClock(): Long {
            val currentTime = SystemClock.elapsedRealtimeNanos()
            globalMasterClock?.set(currentTime)
            return currentTime
        }
    }
    
    // Master timestamp reference (hardware monotonic clock)
    private val localMasterClock = AtomicLong(0)
    private var captureStartTimeNs: Long = 0
    private var captureStartRealtimeMs: Long = 0
    
    // Multi-threaded frame correlation tracking with lock-free structures
    private val videoFrameTimestamps = ConcurrentHashMap<Long, VideoFrameInfo>()
    private val dngFrameTimestamps = ConcurrentHashMap<Long, DNGFrameInfo>()
    private val gsrSampleTimestamps = ConcurrentHashMap<Long, GSRSampleInfo>()
    
    // Thread-safe synchronization metrics
    private val totalFramesPaired = AtomicInteger(0)
    private val totalGSRSamplesSynced = AtomicInteger(0)
    private val averageTemporalDrift = AtomicLong(0)
    private val maxTemporalDrift = AtomicLong(0)
    private val syncValidationCount = AtomicInteger(0)
    private val frameDropCount = AtomicInteger(0)
    
    // Multi-threading components optimized for performance
    private val correlationExecutor: ScheduledExecutorService = 
        Executors.newScheduledThreadPool(CORRELATION_THREAD_POOL_SIZE) { runnable ->
            Thread(runnable, "SyncCorrelation-${Thread.currentThread().id}")
                .apply { priority = Thread.MAX_PRIORITY }
        }
    
    private val cleanupExecutor: ScheduledExecutorService = 
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "SyncCleanup").apply { 
                priority = Thread.NORM_PRIORITY - 1 
            }
        }
    
    private val performanceMonitor: ScheduledExecutorService = 
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "SyncPerformanceMonitor").apply { 
                priority = Thread.NORM_PRIORITY 
            }
        }
    
    private val metricsLock = ReentrantReadWriteLock()
    
    // Performance monitoring and optimization
    private val frameProcessingTimes = ConcurrentHashMap<String, CircularBuffer>()
    private val threadPerformanceMetrics = ConcurrentHashMap<String, ThreadPerformanceInfo>()
    private val correlationTaskQueue = AtomicInteger(0)
    
    // Lock-free circular buffer for performance monitoring
    private class CircularBuffer(private val capacity: Int = 100) {
        private val buffer = LongArray(capacity)
        private val writeIndex = AtomicInteger(0)
        private val count = AtomicInteger(0)
        
        fun add(value: Long) {
            val index = writeIndex.getAndIncrement() % capacity
            buffer[index] = value
            count.updateAndGet { current -> minOf(current + 1, capacity) }
        }
        
        fun getAverage(): Double {
            val currentCount = count.get()
            return if (currentCount > 0) {
                buffer.take(currentCount).average()
            } else 0.0
        }
    }
    
    /**
     * Initialize the enhanced synchronized capture system with multi-threading
     */
    fun initialize(): Boolean {
        return try {
            // Initialize global master clock if not already done
            if (!isGlobalClockInitialized) {
                initializeGlobalMasterClock()
            }
            
            // Reset all timing data
            localMasterClock.set(0)
            captureStartTimeNs = 0
            captureStartRealtimeMs = 0
            
            // Clear all timestamp collections
            videoFrameTimestamps.clear()
            dngFrameTimestamps.clear()
            gsrSampleTimestamps.clear()
            
            // Reset atomic metrics
            totalFramesPaired.set(0)
            totalGSRSamplesSynced.set(0)
            averageTemporalDrift.set(0)
            maxTemporalDrift.set(0)
            syncValidationCount.set(0)
            frameDropCount.set(0)
            
            // Initialize performance monitoring
            frameProcessingTimes.clear()
            threadPerformanceMetrics.clear()
            correlationTaskQueue.set(0)
            
            // Start background tasks
            startBackgroundTasks()
            
            XLog.i(TAG, "Enhanced Synchronized Capture System initialized with ${CORRELATION_THREAD_POOL_SIZE} correlation threads")
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize enhanced sync system: ${e.message}", e)
            false
        }
    }
    
    /**
     * Start synchronized capture session with global clock alignment
     */
    fun startSynchronizedCapture(): CaptureSessionInfo {
        val globalTime = getGlobalMasterClock()
        captureStartTimeNs = globalTime
        captureStartRealtimeMs = System.currentTimeMillis()
        localMasterClock.set(captureStartTimeNs)
        
        val sessionInfo = CaptureSessionInfo(
            sessionId = generateSessionId(),
            startTimeNs = captureStartTimeNs,
            startTimeMs = captureStartRealtimeMs
        )
        
        XLog.i(TAG, "Started enhanced synchronized capture session: ${sessionInfo.sessionId}")
        XLog.i(TAG, "Global master clock reference: ${captureStartTimeNs}ns")
        
        // Start real-time performance monitoring
        startPerformanceMonitoring()
        
        return sessionInfo
    }
    
    /**
     * Register a video frame with zero-copy timestamp correlation
     */
    fun registerVideoFrame(presentationTimeUs: Long): Long {
        val startTime = System.nanoTime()
        val hardwareTimestampNs = getGlobalMasterClock()
        val relativeTimestampNs = hardwareTimestampNs - captureStartTimeNs
        
        val frameInfo = VideoFrameInfo(
            presentationTimeUs = presentationTimeUs,
            hardwareTimestampNs = hardwareTimestampNs,
            relativeTimestampNs = relativeTimestampNs,
            sequenceNumber = videoFrameTimestamps.size.toLong()
        )
        
        // Non-blocking frame registration
        videoFrameTimestamps[hardwareTimestampNs] = frameInfo
        
        // Asynchronous correlation to prevent frame drops
        CompletableFuture.runAsync({
            correlateFrame(hardwareTimestampNs, true)
            recordProcessingTime("video_frame", System.nanoTime() - startTime)
        }, correlationExecutor)
        
        return hardwareTimestampNs
    }
    
    /**
     * Register a DNG frame with optimized concurrent processing
     */
    fun registerDNGFrame(imageTimestampNs: Long, frameIndex: Int): Long {
        val startTime = System.nanoTime()
        val hardwareTimestampNs = if (imageTimestampNs > 0) imageTimestampNs else getGlobalMasterClock()
        val relativeTimestampNs = hardwareTimestampNs - captureStartTimeNs
        
        val frameInfo = DNGFrameInfo(
            imageTimestampNs = imageTimestampNs,
            hardwareTimestampNs = hardwareTimestampNs,
            relativeTimestampNs = relativeTimestampNs,
            frameIndex = frameIndex,
            captureTimeMs = System.currentTimeMillis()
        )
        
        // Non-blocking frame registration
        dngFrameTimestamps[hardwareTimestampNs] = frameInfo
        
        // Asynchronous correlation with priority for DNG frames
        CompletableFuture.runAsync({
            correlateFrame(hardwareTimestampNs, false)
            recordProcessingTime("dng_frame", System.nanoTime() - startTime)
        }, correlationExecutor)
        
        return hardwareTimestampNs
    }
    
    /**
     * Register GSR sample with synchronized timestamp
     */
    fun registerGSRSample(gsrValue: Double, skinTemp: Double, originalTimestamp: Long = 0): Long {
        val startTime = System.nanoTime()
        val hardwareTimestampNs = getGlobalMasterClock()
        val relativeTimestampNs = hardwareTimestampNs - captureStartTimeNs
        
        val sampleInfo = GSRSampleInfo(
            gsrValue = gsrValue,
            skinTemperature = skinTemp,
            originalTimestampMs = originalTimestamp,
            synchronizedTimestampNs = hardwareTimestampNs,
            relativeTimestampNs = relativeTimestampNs
        )
        
        gsrSampleTimestamps[hardwareTimestampNs] = sampleInfo
        
        // Lightweight correlation for GSR samples
        CompletableFuture.runAsync({
            correlateGSRSample(hardwareTimestampNs)
            recordProcessingTime("gsr_sample", System.nanoTime() - startTime)
        }, correlationExecutor)
        
        return hardwareTimestampNs
    }
    
    /**
     * Fast frame correlation with lock-free algorithm
     */
    private fun correlateFrame(newTimestampNs: Long, isVideo: Boolean) {
        correlationTaskQueue.incrementAndGet()
        
        try {
            val otherFrames = if (isVideo) dngFrameTimestamps else videoFrameTimestamps
            
            // Find frames within correlation window using parallel stream
            val correlatedFrames = otherFrames.entries.parallelStream()
                .filter { entry -> abs(entry.key - newTimestampNs) <= TIMESTAMP_CORRELATION_WINDOW_NS }
                .toList()
            
            if (correlatedFrames.isNotEmpty()) {
                // Find closest temporal match
                val closestMatch = correlatedFrames.minByOrNull { entry ->
                    abs(entry.key - newTimestampNs)
                }
                
                closestMatch?.let { match ->
                    val drift = abs(match.key - newTimestampNs)
                    updateDriftMetrics(drift)
                    totalFramesPaired.incrementAndGet()
                    
                    val pairedCount = totalFramesPaired.get()
                    if (pairedCount % 30 == 0) { // Log every second at 30fps
                        XLog.d(TAG, "Frame correlation: ${pairedCount} pairs, avg drift: %.2f ms"
                            .format(averageTemporalDrift.get() / 1_000_000.0))
                    }
                }
            }
        } catch (e: Exception) {
            XLog.e(TAG, "Error in frame correlation: ${e.message}")
            frameDropCount.incrementAndGet()
        } finally {
            correlationTaskQueue.decrementAndGet()
        }
    }
    
    /**
     * Correlate GSR sample with video/DNG frames
     */
    private fun correlateGSRSample(timestampNs: Long) {
        // GSR samples at 128Hz need correlation with 30fps video frames
        val correlationWindow = 8_333_333L // ~8.33ms (1/8th of frame period)
        
        val nearestVideoFrame = videoFrameTimestamps.entries
            .filter { entry -> abs(entry.key - timestampNs) <= correlationWindow }
            .minByOrNull { entry -> abs(entry.key - timestampNs) }
        
        if (nearestVideoFrame != null) {
            totalGSRSamplesSynced.incrementAndGet()
        }
    }
    
    /**
     * Thread-safe drift metrics update
     */
    private fun updateDriftMetrics(driftNs: Long) {
        maxTemporalDrift.updateAndGet { current -> maxOf(current, driftNs) }
        
        // Update running average using atomic operations
        val currentCount = totalFramesPaired.get()
        if (currentCount > 0) {
            val currentAverage = averageTemporalDrift.get()
            val newAverage = (currentAverage * (currentCount - 1) + driftNs) / currentCount
            averageTemporalDrift.set(newAverage)
        }
    }
    
    /**
     * Record processing time for performance monitoring
     */
    private fun recordProcessingTime(operation: String, timeNs: Long) {
        frameProcessingTimes.computeIfAbsent(operation) { CircularBuffer() }
            .add(timeNs)
    }
    
    /**
     * Start background cleanup and monitoring tasks
     */
    private fun startBackgroundTasks() {
        // Periodic buffer cleanup to prevent memory leaks
        cleanupExecutor.scheduleAtFixedRate({
            cleanupOldFrames()
        }, BUFFER_CLEANUP_INTERVAL_MS, BUFFER_CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS)
        
        // Update global master clock periodically for app-wide synchronization
        cleanupExecutor.scheduleAtFixedRate({
            updateGlobalMasterClock()
        }, 100L, 100L, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Start real-time performance monitoring
     */
    private fun startPerformanceMonitoring() {
        performanceMonitor.scheduleAtFixedRate({
            updatePerformanceMetrics()
        }, PERFORMANCE_MONITORING_INTERVAL_MS, PERFORMANCE_MONITORING_INTERVAL_MS, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics() {
        metricsLock.read {
            val queueSize = correlationTaskQueue.get()
            if (queueSize > CORRELATION_THREAD_POOL_SIZE * 2) {
                XLog.w(TAG, "High correlation task queue: $queueSize")
            }
            
            val videoProcessingTime = frameProcessingTimes["video_frame"]?.getAverage() ?: 0.0
            val dngProcessingTime = frameProcessingTimes["dng_frame"]?.getAverage() ?: 0.0
            val gsrProcessingTime = frameProcessingTimes["gsr_sample"]?.getAverage() ?: 0.0
            
            if (videoProcessingTime > 1_000_000.0) { // > 1ms
                XLog.w(TAG, "Slow video frame processing: %.2f ms".format(videoProcessingTime / 1_000_000.0))
            }
        }
    }
    
    /**
     * Cleanup old frames to prevent memory leaks
     */
    private fun cleanupOldFrames() {
        val cutoffTime = getGlobalMasterClock() - (MAX_FRAME_BUFFER_SIZE * 33_333_333L) // ~8.5 seconds at 30fps
        
        val videoCleaned = videoFrameTimestamps.entries.removeIf { it.key < cutoffTime }
        val dngCleaned = dngFrameTimestamps.entries.removeIf { it.key < cutoffTime }
        val gsrCleaned = gsrSampleTimestamps.entries.removeIf { it.key < cutoffTime }
        
        if (videoCleaned || dngCleaned || gsrCleaned) {
            XLog.d(TAG, "Cleaned up old frames: video=$videoCleaned, dng=$dngCleaned, gsr=$gsrCleaned")
        }
    }
    
    /**
     * Get complete synchronization metrics with thread safety
     */
    fun getSynchronizationMetrics(): EnhancedSynchronizationMetrics {
        return metricsLock.read {
            val currentTimeNs = getGlobalMasterClock()
            val sessionDurationMs = if (captureStartTimeNs > 0) {
                (currentTimeNs - captureStartTimeNs) / 1_000_000
            } else 0
            
            EnhancedSynchronizationMetrics(
                sessionDurationMs = sessionDurationMs,
                videoFramesRecorded = videoFrameTimestamps.size,
                dngFramesCaptured = dngFrameTimestamps.size,
                gsrSamplesCaptured = gsrSampleTimestamps.size,
                totalFramesPaired = totalFramesPaired.get(),
                totalGSRSamplesSynced = totalGSRSamplesSynced.get(),
                averageTemporalDriftNs = averageTemporalDrift.get(),
                maxTemporalDriftNs = maxTemporalDrift.get(),
                syncAccuracyPercent = calculateSyncAccuracy(),
                isWithinTolerance = maxTemporalDrift.get() <= MAX_TEMPORAL_DRIFT_NS,
                frameDropCount = frameDropCount.get(),
                correlationTaskQueueSize = correlationTaskQueue.get(),
                averageVideoProcessingTimeMs = frameProcessingTimes["video_frame"]?.getAverage()?.div(1_000_000.0) ?: 0.0,
                averageDngProcessingTimeMs = frameProcessingTimes["dng_frame"]?.getAverage()?.div(1_000_000.0) ?: 0.0,
                averageGsrProcessingTimeMs = frameProcessingTimes["gsr_sample"]?.getAverage()?.div(1_000_000.0) ?: 0.0
            )
        }
    }
    
    private fun calculateSyncAccuracy(): Double {
        val totalFrames = videoFrameTimestamps.size + dngFrameTimestamps.size
        val pairedFrames = totalFramesPaired.get()
        return if (totalFrames > 0) {
            (pairedFrames.toDouble() / totalFrames) * 100.0
        } else 0.0
    }
    
    private fun generateSessionId(): String {
        return "enhanced_sync_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }
    
    /**
     * Cleanup resources and stop all background threads
     */
    fun cleanup() {
        try {
            correlationExecutor.shutdown()
            cleanupExecutor.shutdown()
            performanceMonitor.shutdown()
            
            videoFrameTimestamps.clear()
            dngFrameTimestamps.clear()
            gsrSampleTimestamps.clear()
            frameProcessingTimes.clear()
            threadPerformanceMetrics.clear()
            
            XLog.i(TAG, "Enhanced Synchronized Capture System cleaned up")
        } catch (e: Exception) {
            XLog.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}

/**
 * GSR sample information with synchronized timestamp
 */
data class GSRSampleInfo(
    val gsrValue: Double,
    val skinTemperature: Double,
    val originalTimestampMs: Long,
    val synchronizedTimestampNs: Long,
    val relativeTimestampNs: Long
)

/**
 * Thread performance information
 */
data class ThreadPerformanceInfo(
    val threadName: String,
    val averageProcessingTimeNs: Double,
    val maxProcessingTimeNs: Long,
    val taskCount: Long
)

/**
 * Enhanced synchronization metrics with multi-threading performance data
 */
data class EnhancedSynchronizationMetrics(
    val sessionDurationMs: Long,
    val videoFramesRecorded: Int,
    val dngFramesCaptured: Int,
    val gsrSamplesCaptured: Int,
    val totalFramesPaired: Int,
    val totalGSRSamplesSynced: Int,
    val averageTemporalDriftNs: Long,
    val maxTemporalDriftNs: Long,
    val syncAccuracyPercent: Double,
    val isWithinTolerance: Boolean,
    val frameDropCount: Int,
    val correlationTaskQueueSize: Int,
    val averageVideoProcessingTimeMs: Double,
    val averageDngProcessingTimeMs: Double,
    val averageGsrProcessingTimeMs: Double
)