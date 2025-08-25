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

@SuppressLint("MissingPermission")
class EnhancedSynchronizedCaptureSystem(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "EnhancedSyncCaptureSystem"
        
        private const val MAX_TEMPORAL_DRIFT_NS = 16_666_666L
        private const val SYNC_VALIDATION_INTERVAL_MS = 500L
        private const val TIMESTAMP_CORRELATION_WINDOW_NS = 33_333_333L
        
        private const val CORRELATION_THREAD_POOL_SIZE = 4
        private const val BUFFER_CLEANUP_INTERVAL_MS = 3000L
        private const val MAX_FRAME_BUFFER_SIZE = 512
        private const val PERFORMANCE_MONITORING_INTERVAL_MS = 1000L
        
        @Volatile
        private var globalMasterClock: AtomicLong? = null
        private var isGlobalClockInitialized = false
        
        fun getGlobalMasterClock(): Long {
            if (!isGlobalClockInitialized) {
                initializeGlobalMasterClock()
            }
            return globalMasterClock?.get() ?: SystemClock.elapsedRealtimeNanos()
        }
        
        fun initializeGlobalMasterClock(): Long {
            val currentTime = SystemClock.elapsedRealtimeNanos()
            globalMasterClock = AtomicLong(currentTime)
            isGlobalClockInitialized = true
            XLog.i(TAG, "Global master clock initialized: ${currentTime}ns")
            return currentTime
        }
        
        fun updateGlobalMasterClock(): Long {
            val currentTime = SystemClock.elapsedRealtimeNanos()
            globalMasterClock?.set(currentTime)
            return currentTime
        }
    }
    
    private val localMasterClock = AtomicLong(0)
    private var captureStartTimeNs: Long = 0
    private var captureStartRealtimeMs: Long = 0
    
    private val videoFrameTimestamps = ConcurrentHashMap<Long, VideoFrameInfo>()
    private val dngFrameTimestamps = ConcurrentHashMap<Long, DNGFrameInfo>()
    private val gsrSampleTimestamps = ConcurrentHashMap<Long, GSRSampleInfo>()
    
    private val totalFramesPaired = AtomicInteger(0)
    private val totalGSRSamplesSynced = AtomicInteger(0)
    private val averageTemporalDrift = AtomicLong(0)
    private val maxTemporalDrift = AtomicLong(0)
    private val syncValidationCount = AtomicInteger(0)
    private val frameDropCount = AtomicInteger(0)
    
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
    
    private val frameProcessingTimes = ConcurrentHashMap<String, CircularBuffer>()
    private val threadPerformanceMetrics = ConcurrentHashMap<String, ThreadPerformanceInfo>()
    private val correlationTaskQueue = AtomicInteger(0)
    
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
    
    fun initialize(): Boolean {
        return try {

            if (!isGlobalClockInitialized) {
                initializeGlobalMasterClock()
            }
            
            localMasterClock.set(0)
            captureStartTimeNs = 0
            captureStartRealtimeMs = 0
            
            videoFrameTimestamps.clear()
            dngFrameTimestamps.clear()
            gsrSampleTimestamps.clear()
            
            totalFramesPaired.set(0)
            totalGSRSamplesSynced.set(0)
            averageTemporalDrift.set(0)
            maxTemporalDrift.set(0)
            syncValidationCount.set(0)
            frameDropCount.set(0)
            
            frameProcessingTimes.clear()
            threadPerformanceMetrics.clear()
            correlationTaskQueue.set(0)
            
            startBackgroundTasks()
            
            XLog.i(TAG, "Enhanced Synchronized Capture System initialized with ${CORRELATION_THREAD_POOL_SIZE} correlation threads")
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize enhanced sync system: ${e.message}", e)
            false
        }
    }
    
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
        
        startPerformanceMonitoring()
        
        return sessionInfo
    }
    
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
        
        videoFrameTimestamps[hardwareTimestampNs] = frameInfo
        
        CompletableFuture.runAsync({
            correlateFrame(hardwareTimestampNs, true)
            recordProcessingTime("video_frame", System.nanoTime() - startTime)
        }, correlationExecutor)
        
        return hardwareTimestampNs
    }
    
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
        
        dngFrameTimestamps[hardwareTimestampNs] = frameInfo
        
        CompletableFuture.runAsync({
            correlateFrame(hardwareTimestampNs, false)
            recordProcessingTime("dng_frame", System.nanoTime() - startTime)
        }, correlationExecutor)
        
        return hardwareTimestampNs
    }
    
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
        
        CompletableFuture.runAsync({
            correlateGSRSample(hardwareTimestampNs)
            recordProcessingTime("gsr_sample", System.nanoTime() - startTime)
        }, correlationExecutor)
        
        return hardwareTimestampNs
    }
    
    private fun correlateFrame(newTimestampNs: Long, isVideo: Boolean) {
        correlationTaskQueue.incrementAndGet()
        
        try {
            val otherFrames = if (isVideo) dngFrameTimestamps else videoFrameTimestamps
            
            val correlatedFrames = otherFrames.entries.parallelStream()
                .filter { entry -> abs(entry.key - newTimestampNs) <= TIMESTAMP_CORRELATION_WINDOW_NS }
                .toList()
            
            if (correlatedFrames.isNotEmpty()) {

                val closestMatch = correlatedFrames.minByOrNull { entry ->
                    abs(entry.key - newTimestampNs)
                }
                
                closestMatch?.let { match ->
                    val drift = abs(match.key - newTimestampNs)
                    updateDriftMetrics(drift)
                    totalFramesPaired.incrementAndGet()
                    
                    val pairedCount = totalFramesPaired.get()
                    if (pairedCount % 30 == 0) {
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
    
    private fun correlateGSRSample(timestampNs: Long) {

        val correlationWindow = 8_333_333L
        
        val nearestVideoFrame = videoFrameTimestamps.entries
            .filter { entry -> abs(entry.key - timestampNs) <= correlationWindow }
            .minByOrNull { entry -> abs(entry.key - timestampNs) }
        
        if (nearestVideoFrame != null) {
            totalGSRSamplesSynced.incrementAndGet()
        }
    }
    
    private fun updateDriftMetrics(driftNs: Long) {
        maxTemporalDrift.updateAndGet { current -> maxOf(current, driftNs) }
        
        val currentCount = totalFramesPaired.get()
        if (currentCount > 0) {
            val currentAverage = averageTemporalDrift.get()
            val newAverage = (currentAverage * (currentCount - 1) + driftNs) / currentCount
            averageTemporalDrift.set(newAverage)
        }
    }
    
    private fun recordProcessingTime(operation: String, timeNs: Long) {
        frameProcessingTimes.computeIfAbsent(operation) { CircularBuffer() }
            .add(timeNs)
    }
    
    private fun startBackgroundTasks() {

        cleanupExecutor.scheduleAtFixedRate({
            cleanupOldFrames()
        }, BUFFER_CLEANUP_INTERVAL_MS, BUFFER_CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS)
        
        cleanupExecutor.scheduleAtFixedRate({
            updateGlobalMasterClock()
        }, 100L, 100L, TimeUnit.MILLISECONDS)
    }
    
    private fun startPerformanceMonitoring() {
        performanceMonitor.scheduleAtFixedRate({
            updatePerformanceMetrics()
        }, PERFORMANCE_MONITORING_INTERVAL_MS, PERFORMANCE_MONITORING_INTERVAL_MS, TimeUnit.MILLISECONDS)
    }
    
    private fun updatePerformanceMetrics() {
        metricsLock.read {
            val queueSize = correlationTaskQueue.get()
            if (queueSize > CORRELATION_THREAD_POOL_SIZE * 2) {
                XLog.w(TAG, "High correlation task queue: $queueSize")
            }
            
            val videoProcessingTime = frameProcessingTimes["video_frame"]?.getAverage() ?: 0.0
            val dngProcessingTime = frameProcessingTimes["dng_frame"]?.getAverage() ?: 0.0
            val gsrProcessingTime = frameProcessingTimes["gsr_sample"]?.getAverage() ?: 0.0
            
            if (videoProcessingTime > 1_000_000.0) {
                XLog.w(TAG, "Slow video frame processing: %.2f ms".format(videoProcessingTime / 1_000_000.0))
            }
        }
    }
    
    private fun cleanupOldFrames() {
        val cutoffTime = getGlobalMasterClock() - (MAX_FRAME_BUFFER_SIZE * 33_333_333L)
        
        val videoCleaned = videoFrameTimestamps.entries.removeIf { it.key < cutoffTime }
        val dngCleaned = dngFrameTimestamps.entries.removeIf { it.key < cutoffTime }
        val gsrCleaned = gsrSampleTimestamps.entries.removeIf { it.key < cutoffTime }
        
        if (videoCleaned || dngCleaned || gsrCleaned) {
            XLog.d(TAG, "Cleaned up old frames: video=$videoCleaned, dng=$dngCleaned, gsr=$gsrCleaned")
        }
    }
    
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

data class GSRSampleInfo(
    val gsrValue: Double,
    val skinTemperature: Double,
    val originalTimestampMs: Long,
    val synchronizedTimestampNs: Long,
    val relativeTimestampNs: Long
)

data class ThreadPerformanceInfo(
    val threadName: String,
    val averageProcessingTimeNs: Double,
    val maxProcessingTimeNs: Long,
    val taskCount: Long
)

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
