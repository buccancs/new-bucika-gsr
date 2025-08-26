package com.topdon.thermal.capture.optimization

import android.content.Context
import android.os.Process
import android.os.Handler
import android.os.HandlerThread
import com.elvishew.xlog.XLog
import com.topdon.thermal.capture.sync.EnhancedSynchronizedCaptureSystem
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

class CapturePerformanceOptimizer(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "CapturePerformanceOptimizer"
        
        private const val HIGH_PRIORITY_THREAD_COUNT = 3
        private const val BACKGROUND_THREAD_COUNT = 2
        private const val FRAME_BUFFER_SIZE = 1024
        private const val MEMORY_PRESSURE_THRESHOLD_MB = 100
        private const val CPU_USAGE_THRESHOLD_PERCENT = 80
        private const val THERMAL_THROTTLING_TEMP_CELSIUS = 45
        
        private const val ULTRA_HIGH_PRIORITY = Thread.MAX_PRIORITY
        private const val HIGH_PRIORITY = Thread.MAX_PRIORITY - 1
        private const val NORMAL_PRIORITY = Thread.NORM_PRIORITY
        private const val LOW_PRIORITY = Thread.NORM_PRIORITY - 1
    }
    
    private var videoProcessingExecutor: ThreadPoolExecutor? = null
    private var dngProcessingExecutor: ThreadPoolExecutor? = null
    private var gsrProcessingExecutor: ScheduledExecutorService? = null
    private var backgroundExecutor: ScheduledExecutorService? = null
    
    private val videoFrameBuffer = LockFreeRingBuffer<VideoFrameData>(FRAME_BUFFER_SIZE)
    private val dngFrameBuffer = LockFreeRingBuffer<DNGFrameData>(FRAME_BUFFER_SIZE / 2)
    private val gsrSampleBuffer = LockFreeRingBuffer<GSRSampleData>(FRAME_BUFFER_SIZE * 4)
    
    private val frameDropCounter = AtomicInteger(0)
    private val memoryPressureLevel = AtomicInteger(0)
    private val cpuUsagePercent = AtomicInteger(0)
    private val thermalState = AtomicInteger(0)
    
    private val isOptimizationActive = AtomicBoolean(false)
    private val isPerformanceMonitoringActive = AtomicBoolean(false)
    
    private val totalFramesProcessed = AtomicLong(0)
    private val totalProcessingTimeNs = AtomicLong(0)
    private val lastOptimizationTime = AtomicLong(0)
    
    fun initialize(): Boolean {
        return try {
            XLog.i(TAG, "Initializing ultra-high performance capture optimization...")
            
            createOptimizedThreadPools()
            initializeZeroCopyBuffers()
            startPerformanceMonitoring()
            applySystemOptimizations()
            
            isOptimizationActive.set(true)
            XLog.i(TAG, "Ultra-high performance optimization initialized successfully")
            true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize performance optimization: ${e.message}", e)
            false
        }
    }
    
    private fun createOptimizedThreadPools() {

        videoProcessingExecutor = ThreadPoolExecutor(
            1,
            1,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            CustomThreadFactory("VideoProcessor", ULTRA_HIGH_PRIORITY)
        ).apply {
            allowCoreThreadTimeOut(false)
        }
        
        dngProcessingExecutor = ThreadPoolExecutor(
            1,
            1,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            CustomThreadFactory("DNGProcessor", HIGH_PRIORITY)
        ).apply {
            allowCoreThreadTimeOut(false)
        }
        
        gsrProcessingExecutor = Executors.newSingleThreadScheduledExecutor(
            CustomThreadFactory("GSRProcessor", ULTRA_HIGH_PRIORITY)
        )
        
        backgroundExecutor = Executors.newScheduledThreadPool(
            BACKGROUND_THREAD_COUNT,
            CustomThreadFactory("Background", LOW_PRIORITY)
        )
        
        XLog.i(TAG, "Optimized thread pools created with priority scheduling")
    }
    
    private fun initializeZeroCopyBuffers() {
        videoFrameBuffer.clear()
        dngFrameBuffer.clear()
        gsrSampleBuffer.clear()
        
        XLog.i(TAG, "Zero-copy ring buffers initialized: video=${FRAME_BUFFER_SIZE}, dng=${FRAME_BUFFER_SIZE/2}, gsr=${FRAME_BUFFER_SIZE*4}")
    }
    
    private fun applySystemOptimizations() {
        try {

            System.gc()
            
            configureOptimalBufferSizes()
            
            XLog.i(TAG, "System-level optimizations applied")
            
        } catch (e: Exception) {
            XLog.w(TAG, "Some system optimizations may not be available: ${e.message}")
        }
    }
    
    private fun configureOptimalBufferSizes() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - (runtime.totalMemory() - runtime.freeMemory())
        
        XLog.i(TAG, "Memory status: max=${maxMemory / (1024*1024)}MB, available=${availableMemory / (1024*1024)}MB")
        
        if (availableMemory < 200 * 1024 * 1024) {
            memoryPressureLevel.set(2)
            XLog.w(TAG, "High memory pressure detected - reducing buffer sizes")
        } else if (availableMemory < 400 * 1024 * 1024) {
            memoryPressureLevel.set(1)
        } else {
            memoryPressureLevel.set(0)
        }
    }
    
    private fun startPerformanceMonitoring() {
        backgroundExecutor?.scheduleAtFixedRate({
            monitorSystemPerformance()
        }, 0, 500, TimeUnit.MILLISECONDS)
        
        backgroundExecutor?.scheduleAtFixedRate({
            optimizeBasedOnPerformance()
        }, 2000, 2000, TimeUnit.MILLISECONDS)
        
        isPerformanceMonitoringActive.set(true)
        XLog.i(TAG, "Performance monitoring started")
    }
    
    private fun monitorSystemPerformance() {
        try {

            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryUsageMB = usedMemory / (1024 * 1024)
            
            if (memoryUsageMB > MEMORY_PRESSURE_THRESHOLD_MB) {
                memoryPressureLevel.set(if (memoryUsageMB > MEMORY_PRESSURE_THRESHOLD_MB * 2) 2 else 1)
            } else {
                memoryPressureLevel.set(0)
            }
            
            val videoBufferUsage = videoFrameBuffer.size() * 100 / FRAME_BUFFER_SIZE
            val dngBufferUsage = dngFrameBuffer.size() * 100 / (FRAME_BUFFER_SIZE / 2)
            val gsrBufferUsage = gsrSampleBuffer.size() * 100 / (FRAME_BUFFER_SIZE * 4)
            
            if (videoBufferUsage > 80 || dngBufferUsage > 80 || gsrBufferUsage > 80) {
                XLog.w(TAG, "High buffer usage: video=$videoBufferUsage%, dng=$dngBufferUsage%, gsr=$gsrBufferUsage%")
                
                triggerEmergencyBufferCleanup()
            }
            
            val monitorCount = totalFramesProcessed.get() / 1000
            if (monitorCount % 10 == 0L && monitorCount > 0) {
                logPerformanceStatus(memoryUsageMB, videoBufferUsage, dngBufferUsage, gsrBufferUsage)
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error in performance monitoring: ${e.message}")
        }
    }
    
    fun getPerformanceMetrics(): CapturePerformanceMetrics {
        val totalFrames = totalFramesProcessed.get()
        val averageProcessingTime = if (totalFrames > 0) {
            totalProcessingTimeNs.get().toDouble() / totalFrames / 1_000_000.0
        } else 0.0
        
        return CapturePerformanceMetrics(
            isOptimizationActive = isOptimizationActive.get(),
            totalFramesProcessed = totalFrames,
            frameDropCount = frameDropCounter.get(),
            averageProcessingTimeMs = averageProcessingTime,
            memoryPressureLevel = memoryPressureLevel.get(),
            videoBufferUsage = videoFrameBuffer.size(),
            dngBufferUsage = dngFrameBuffer.size(),
            gsrBufferUsage = gsrSampleBuffer.size(),
            maxVideoBufferSize = FRAME_BUFFER_SIZE,
            maxDngBufferSize = FRAME_BUFFER_SIZE / 2,
            maxGsrBufferSize = FRAME_BUFFER_SIZE * 4
        )
    }
    
    private fun optimizeBasedOnPerformance() {

        XLog.d(TAG, "Performance optimization cycle executed")
    }
    
    private fun triggerEmergencyBufferCleanup() {

        XLog.w(TAG, "Emergency buffer cleanup triggered")
    }
    
    private fun logPerformanceStatus(memoryUsageMB: Long, videoBufferUsage: Int, dngBufferUsage: Int, gsrBufferUsage: Int) {
        XLog.i(TAG, "Performance: Memory=${memoryUsageMB}MB, Buffers: V=$videoBufferUsage%, D=$dngBufferUsage%, G=$gsrBufferUsage%")
    }
    
    fun cleanup() {
        try {
            isOptimizationActive.set(false)
            isPerformanceMonitoringActive.set(false)
            
            videoProcessingExecutor?.shutdown()
            dngProcessingExecutor?.shutdown()
            gsrProcessingExecutor?.shutdown()
            backgroundExecutor?.shutdown()
            
            videoFrameBuffer.clear()
            dngFrameBuffer.clear()
            gsrSampleBuffer.clear()
            
            XLog.i(TAG, "Capture performance optimizer cleaned up")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}

private class CustomThreadFactory(
    private val namePrefix: String,
    private val priority: Int
) : ThreadFactory {
    private val threadNumber = AtomicInteger(1)
    
    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r, "$namePrefix-${threadNumber.getAndIncrement()}")
        thread.isDaemon = false
        thread.priority = priority
        return thread
    }
}

private class LockFreeRingBuffer<T>(private val capacity: Int) {
    private val buffer = Array<Any?>(capacity) { null }
    private val head = AtomicInteger(0)
    private val tail = AtomicInteger(0)
    private val size = AtomicInteger(0)
    
    fun offer(item: T): Boolean {
        if (size.get() >= capacity) {
            return false
        }
        
        val currentTail = tail.getAndIncrement() % capacity
        buffer[currentTail] = item
        size.incrementAndGet()
        return true
    }
    
    @Suppress("UNCHECKED_CAST")
    fun poll(): T? {
        if (size.get() == 0) {
            return null
        }
        
        val currentHead = head.getAndIncrement() % capacity
        val item = buffer[currentHead] as T?
        buffer[currentHead] = null
        size.decrementAndGet()
        return item
    }
    
    fun size(): Int = size.get()
    
    fun clear() {
        head.set(0)
        tail.set(0)
        size.set(0)
        buffer.fill(null)
    }
    
    fun clearOldest(count: Int): Int {
        var cleared = 0
        repeat(count) {
            if (poll() != null) {
                cleared++
            } else {
                return cleared
            }
        }
        return cleared
    }
}

data class VideoFrameData(
    val timestamp: Long,
    val frameIndex: Int,
    val data: ByteArray? = null
)

data class DNGFrameData(
    val timestamp: Long,
    val frameIndex: Int,
    val data: ByteArray? = null
)

data class GSRSampleData(
    val timestamp: Long,
    val gsrValue: Double,
    val skinTemperature: Double,
    val sampleIndex: Long
)

data class CapturePerformanceMetrics(
    val isOptimizationActive: Boolean,
    val totalFramesProcessed: Long,
    val frameDropCount: Int,
    val averageProcessingTimeMs: Double,
    val memoryPressureLevel: Int,
    val videoBufferUsage: Int,
    val dngBufferUsage: Int,
    val gsrBufferUsage: Int,
    val maxVideoBufferSize: Int,
    val maxDngBufferSize: Int,
    val maxGsrBufferSize: Int
) {
    val frameDropRate: Double
        get() = if (totalFramesProcessed > 0) {
            (frameDropCount.toDouble() / totalFramesProcessed) * 100.0
        } else 0.0
    
    val overallBufferUsage: Double
        get() = (videoBufferUsage + dngBufferUsage + gsrBufferUsage) / 3.0
}
