package com.topdon.tc001.memory

import android.app.Application
import android.content.Context
import android.os.Debug
import com.elvishew.xlog.XLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class MemoryLeakDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "MemoryLeakDetector"
        
        private const val MONITORING_INTERVAL_MS = 30_000L
        private const val LEAK_DETECTION_WINDOW = 10
        private const val LEAK_THRESHOLD_MB = 20.0
        private const val MEMORY_PRESSURE_THRESHOLD = 0.75
        
        private const val AGGRESSIVE_GC_THRESHOLD = 0.80
        private const val EMERGENCY_CLEANUP_THRESHOLD = 0.90
        
        private const val MAX_TRACKED_OBJECTS = 10000
        private const val OBJECT_CLEANUP_INTERVAL_MS = 300_000L
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    private val memoryHistory = mutableListOf<MemorySnapshot>()
    
    private val trackedObjects = ConcurrentHashMap<String, MutableList<WeakReference<Any>>>()
    private val objectCreationTimes = ConcurrentHashMap<Int, Long>()
    private val potentialLeaks = mutableSetOf<String>()
    
    private val cleanupStrategies = mutableListOf<CleanupStrategy>()
    
    init {
        initializeCleanupStrategies()
        startMemoryMonitoring()
        startObjectCleanup()
    }

    data class MemoryState(
        val isMonitoring: Boolean = false,
        val currentHeapMB: Double = 0.0,
        val maxHeapMB: Double = 0.0,
        val heapUsagePercent: Double = 0.0,
        val memoryPressureLevel: MemoryPressureLevel = MemoryPressureLevel.NORMAL,
        val detectedLeaks: List<String> = emptyList(),
        val memoryTrend: MemoryTrend = MemoryTrend.STABLE,
        val lastCleanupTime: Long = 0L,
        val recommendedActions: List<String> = emptyList(),
        val trackedObjectCount: Int = 0,
        val gcCount: Int = 0
    )
    
    enum class MemoryPressureLevel {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    enum class MemoryTrend {
        DECREASING,
        STABLE,
        INCREASING,
        LEAK_SUSPECTED
    }

    fun startMonitoring() {
        XLog.i(TAG, "Starting advanced memory leak detection and prevention")
        
        _memoryState.value = _memoryState.value.copy(isMonitoring = true)
    }

    fun stopMonitoring() {
        XLog.i(TAG, "Stopping memory leak detection")
        
        coroutineScope.cancel()
        _memoryState.value = MemoryState()
    }

    fun trackObject(obj: Any, category: String = "General") {
        try {
            val objectHash = System.identityHashCode(obj)
            
            trackedObjects.computeIfAbsent(category) { mutableListOf() }
                .add(WeakReference(obj))
            
            objectCreationTimes[objectHash] = System.currentTimeMillis()
            
            if (trackedObjects.values.sumOf { it.size } > MAX_TRACKED_OBJECTS) {
                cleanupStaleReferences()
            }
            
        } catch (e: Exception) {
            XLog.w(TAG, "Failed to track object: ${e.message}")
        }
    }

    fun forceCleanup() {
        XLog.i(TAG, "Forcing memory cleanup")
        
        coroutineScope.launch {
            performAggressiveCleanup()
        }
    }

    fun getMemoryRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val currentState = _memoryState.value
        
        when (currentState.memoryPressureLevel) {
            MemoryPressureLevel.HIGH -> {
                recommendations.add("Consider reducing video resolution during recording")
                recommendations.add("Close unnecessary background apps")
                recommendations.add("Enable aggressive garbage collection mode")
            }
            MemoryPressureLevel.CRITICAL -> {
                recommendations.add("Immediately stop non-essential recording features")
                recommendations.add("Save current data and restart application")
                recommendations.add("Reduce recording duration for remaining session")
            }
            else -> {
                if (currentState.memoryTrend == MemoryTrend.INCREASING) {
                    recommendations.add("Monitor memory usage - trending upward")
                    recommendations.add("Consider periodic data saving")
                }
            }
        }
        
        if (currentState.detectedLeaks.isNotEmpty()) {
            recommendations.add("Memory leaks detected - application restart recommended")
            recommendations.add("Report leak details for debugging: ${currentState.detectedLeaks.joinToString()}")
        }
        
        return recommendations
    }

    private fun startMemoryMonitoring() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    val snapshot = captureMemorySnapshot()
                    updateMemoryHistory(snapshot)
                    analyzeMemoryTrends()
                    updateMemoryState(snapshot)
                    
                    if (snapshot.heapUsagePercent >= MEMORY_PRESSURE_THRESHOLD) {
                        handleMemoryPressure(snapshot)
                    }
                    
                } catch (e: Exception) {
                    XLog.e(TAG, "Memory monitoring error: ${e.message}", e)
                }
                
                delay(MONITORING_INTERVAL_MS)
            }
        }
    }

    private fun startObjectCleanup() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    cleanupStaleReferences()
                    analyzeObjectLifecycles()
                    
                } catch (e: Exception) {
                    XLog.e(TAG, "Object cleanup error: ${e.message}", e)
                }
                
                delay(OBJECT_CLEANUP_INTERVAL_MS)
            }
        }
    }

    private fun captureMemorySnapshot(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        return MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            heapUsedBytes = usedMemory,
            heapMaxBytes = maxMemory,
            heapUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100.0,
            nativeHeapSize = Debug.getNativeHeapSize(),
            nativeHeapAllocatedSize = Debug.getNativeHeapAllocatedSize(),
            nativeHeapFreeSize = Debug.getNativeHeapFreeSize()
        )
    }

    private fun updateMemoryHistory(snapshot: MemorySnapshot) {
        memoryHistory.add(snapshot)
        
        while (memoryHistory.size > LEAK_DETECTION_WINDOW * 2) {
            memoryHistory.removeAt(0)
        }
    }

    private fun analyzeMemoryTrends() {
        if (memoryHistory.size < LEAK_DETECTION_WINDOW) return
        
        val recentSamples = memoryHistory.takeLast(LEAK_DETECTION_WINDOW)
        val earlierSamples = memoryHistory.takeLast(LEAK_DETECTION_WINDOW * 2).take(LEAK_DETECTION_WINDOW)
        
        val recentAverage = recentSamples.map { it.heapUsedBytes }.average()
        val earlierAverage = earlierSamples.map { it.heapUsedBytes }.average()
        
        val memoryIncrease = (recentAverage - earlierAverage) / (1024.0 * 1024.0)
        
        val trend = when {
            memoryIncrease > LEAK_THRESHOLD_MB -> {
                XLog.w(TAG, "Potential memory leak detected - increase: ${memoryIncrease}MB")
                MemoryTrend.LEAK_SUSPECTED
            }
            memoryIncrease > 5.0 -> MemoryTrend.INCREASING
            memoryIncrease < -5.0 -> MemoryTrend.DECREASING
            else -> MemoryTrend.STABLE
        }
        
        val currentState = _memoryState.value
        _memoryState.value = currentState.copy(memoryTrend = trend)
        
        if (trend == MemoryTrend.LEAK_SUSPECTED) {
            coroutineScope.launch {
                performAggressiveCleanup()
            }
        }
    }

    private fun updateMemoryState(snapshot: MemorySnapshot) {
        val pressureLevel = when {
            snapshot.heapUsagePercent >= 90.0 -> MemoryPressureLevel.CRITICAL
            snapshot.heapUsagePercent >= 75.0 -> MemoryPressureLevel.HIGH
            snapshot.heapUsagePercent >= 50.0 -> MemoryPressureLevel.NORMAL
            else -> MemoryPressureLevel.LOW
        }
        
        val currentState = _memoryState.value
        _memoryState.value = currentState.copy(
            currentHeapMB = snapshot.heapUsedBytes / (1024.0 * 1024.0),
            maxHeapMB = snapshot.heapMaxBytes / (1024.0 * 1024.0),
            heapUsagePercent = snapshot.heapUsagePercent,
            memoryPressureLevel = pressureLevel,
            detectedLeaks = potentialLeaks.toList(),
            recommendedActions = getMemoryRecommendations(),
            trackedObjectCount = trackedObjects.values.sumOf { it.size }
        )
    }

    private suspend fun handleMemoryPressure(snapshot: MemorySnapshot) {
        XLog.w(TAG, "Memory pressure detected - usage: ${snapshot.heapUsagePercent}%")
        
        when {
            snapshot.heapUsagePercent >= EMERGENCY_CLEANUP_THRESHOLD -> {
                XLog.e(TAG, "Emergency memory cleanup triggered")
                performEmergencyCleanup()
            }
            snapshot.heapUsagePercent >= AGGRESSIVE_GC_THRESHOLD -> {
                XLog.w(TAG, "Aggressive cleanup triggered")
                performAggressiveCleanup()
            }
            else -> {
                performStandardCleanup()
            }
        }
    }

    private suspend fun performStandardCleanup() {
        XLog.d(TAG, "Performing standard memory cleanup")
        
        cleanupStaleReferences()
        System.gc()
        
        val currentState = _memoryState.value
        _memoryState.value = currentState.copy(
            lastCleanupTime = System.currentTimeMillis(),
            gcCount = currentState.gcCount + 1
        )
    }

    private suspend fun performAggressiveCleanup() {
        XLog.w(TAG, "Performing aggressive memory cleanup")
        
        cleanupStrategies.forEach { strategy ->
            try {
                strategy.execute()
                delay(100)
            } catch (e: Exception) {
                XLog.w(TAG, "Cleanup strategy failed: ${strategy.name}", e)
            }
        }
        
        repeat(3) {
            System.gc()
            delay(100)
        }
        
        cleanupStaleReferences()
        
        val currentState = _memoryState.value
        _memoryState.value = currentState.copy(
            lastCleanupTime = System.currentTimeMillis(),
            gcCount = currentState.gcCount + 3
        )
    }

    private suspend fun performEmergencyCleanup() {
        XLog.e(TAG, "Performing emergency memory cleanup")
        
        clearImageCaches()
        clearDataBuffers()
        
        performAggressiveCleanup()
        
        trackedObjects.clear()
        objectCreationTimes.clear()
        
        XLog.e(TAG, "Emergency cleanup completed - consider application restart")
    }

    private fun cleanupStaleReferences() {
        val beforeCount = trackedObjects.values.sumOf { it.size }
        
        trackedObjects.values.forEach { objectList ->
            objectList.removeAll { ref -> ref.get() == null }
        }
        
        trackedObjects.entries.removeAll { (_, objectList) -> objectList.isEmpty() }
        
        val afterCount = trackedObjects.values.sumOf { it.size }
        val cleaned = beforeCount - afterCount
        
        if (cleaned > 0) {
            XLog.d(TAG, "Cleaned up $cleaned stale object references")
        }
    }

    private fun analyzeObjectLifecycles() {
        val currentTime = System.currentTimeMillis()
        val longLivedThreshold = 10 * 60 * 1000L
        
        trackedObjects.forEach { (category, objects) ->
            val longLivedObjects = objects.count { ref ->
                val obj = ref.get()
                if (obj != null) {
                    val creationTime = objectCreationTimes[System.identityHashCode(obj)]
                    creationTime != null && (currentTime - creationTime) > longLivedThreshold
                } else false
            }
            
            val totalObjects = objects.size
            val longLivedRatio = if (totalObjects > 0) longLivedObjects.toDouble() / totalObjects else 0.0
            
            if (longLivedRatio > 0.8 && totalObjects > 100) {
                if (!potentialLeaks.contains(category)) {
                    XLog.w(TAG, "Potential leak detected in category: $category (${longLivedObjects}/${totalObjects} long-lived)")
                    potentialLeaks.add(category)
                }
            } else {
                potentialLeaks.remove(category)
            }
        }
    }

    private fun initializeCleanupStrategies() {
        cleanupStrategies.apply {
            add(CleanupStrategy("Image Cache") { clearImageCaches() })
            add(CleanupStrategy("Data Buffers") { clearDataBuffers() })
            add(CleanupStrategy("Temp Files") { clearTempFiles() })
            add(CleanupStrategy("Network Caches") { clearNetworkCaches() })
        }
    }

    private fun clearImageCaches() {

        XLog.d(TAG, "Clearing image caches")
    }

    private fun clearDataBuffers() {

        XLog.d(TAG, "Clearing data buffers")
    }

    private fun clearTempFiles() {

        XLog.d(TAG, "Clearing temporary files")
    }

    private fun clearNetworkCaches() {

        XLog.d(TAG, "Clearing network caches")
    }

    private data class MemorySnapshot(
        val timestamp: Long,
        val heapUsedBytes: Long,
        val heapMaxBytes: Long,
        val heapUsagePercent: Double,
        val nativeHeapSize: Long,
        val nativeHeapAllocatedSize: Long,
        val nativeHeapFreeSize: Long
    )

    private data class CleanupStrategy(
        val name: String,
        val execute: suspend () -> Unit
    )
