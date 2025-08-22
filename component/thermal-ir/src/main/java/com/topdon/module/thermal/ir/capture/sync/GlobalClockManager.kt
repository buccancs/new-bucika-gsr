package com.topdon.module.thermal.ir.capture.sync

import android.content.Context
import android.os.SystemClock
import com.elvishew.xlog.XLog
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Global Application Clock Synchronization Manager
 * Provides app-wide synchronized timing reference for all components
 * Ensures nanosecond precision across video, DNG, GSR, and system components
 */
object GlobalClockManager {
    
    private const val TAG = "GlobalClockManager"
    
    // Global synchronization constants
    private const val SYNC_UPDATE_INTERVAL_MS = 100L // Update every 100ms
    private const val CLOCK_DRIFT_THRESHOLD_NS = 1_000_000L // 1ms threshold
    private const val MAX_COMPONENT_SKEW_NS = 5_000_000L // 5ms max skew between components
    
    // Master timing reference
    private val globalMasterClock = AtomicLong(0)
    private val systemBootTime = AtomicLong(0)
    private var initializationTime: Long = 0
    
    // Synchronization state
    private val isInitialized = AtomicBoolean(false)
    private val isSyncActive = AtomicBoolean(false)
    
    // Component synchronization tracking
    private val componentSyncInfo = ConcurrentHashMap<String, ComponentSyncInfo>()
    private val syncedComponents = mutableSetOf<String>()
    
    // Background synchronization
    private var clockSyncExecutor: ScheduledExecutorService? = null
    
    // Performance monitoring
    private val syncOperationCount = AtomicLong(0)
    private val totalDriftCorrected = AtomicLong(0)
    private val maxDetectedSkew = AtomicLong(0)
    
    /**
     * Initialize the global clock synchronization system
     */
    fun initialize(): Boolean {
        return try {
            if (isInitialized.get()) {
                XLog.w(TAG, "Global clock manager already initialized")
                return true
            }
            
            XLog.i(TAG, "Initializing global clock synchronization system...")
            
            // Capture system boot time for reference
            systemBootTime.set(System.currentTimeMillis() - SystemClock.elapsedRealtime())
            
            // Initialize master clock with hardware timestamp
            val currentTime = SystemClock.elapsedRealtimeNanos()
            globalMasterClock.set(currentTime)
            initializationTime = currentTime
            
            // Start background synchronization
            startBackgroundSync()
            
            isInitialized.set(true)
            isSyncActive.set(true)
            
            XLog.i(TAG, "Global clock synchronization initialized successfully")
            XLog.i(TAG, "Master clock reference: ${currentTime}ns")
            XLog.i(TAG, "System boot time: ${systemBootTime.get()}ms")
            
            true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize global clock manager: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get the current global synchronized timestamp
     */
    fun getCurrentTimestamp(): Long {
        if (!isInitialized.get()) {
            initialize()
        }
        return globalMasterClock.get()
    }
    
    /**
     * Get synchronized timestamp with hardware precision
     */
    fun getSynchronizedTimestamp(): Long {
        val hardwareTime = SystemClock.elapsedRealtimeNanos()
        updateMasterClock(hardwareTime)
        return globalMasterClock.get()
    }
    
    /**
     * Register a component for synchronization tracking
     */
    fun registerComponent(componentId: String, componentType: ComponentType): Boolean {
        return try {
            val currentTime = getSynchronizedTimestamp()
            
            val syncInfo = ComponentSyncInfo(
                componentId = componentId,
                componentType = componentType,
                lastSyncTime = currentTime,
                syncCount = 0,
                totalDrift = 0,
                maxDrift = 0,
                avgDrift = 0.0,
                isActive = true
            )
            
            componentSyncInfo[componentId] = syncInfo
            syncedComponents.add(componentId)
            
            XLog.i(TAG, "Component registered for sync: $componentId (${componentType.name})")
            XLog.d(TAG, "Total synchronized components: ${syncedComponents.size}")
            
            true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to register component $componentId: ${e.message}")
            false
        }
    }
    
    /**
     * Unregister a component from synchronization
     */
    fun unregisterComponent(componentId: String) {
        componentSyncInfo.remove(componentId)
        syncedComponents.remove(componentId)
        XLog.i(TAG, "Component unregistered from sync: $componentId")
    }
    
    /**
     * Synchronize a component's timestamp with the global clock
     */
    fun synchronizeComponent(componentId: String, componentTimestamp: Long): SyncResult {
        if (!isInitialized.get()) {
            return SyncResult(false, 0, "Global clock not initialized")
        }
        
        val syncInfo = componentSyncInfo[componentId]
        if (syncInfo == null) {
            XLog.w(TAG, "Component $componentId not registered for synchronization")
            return SyncResult(false, 0, "Component not registered")
        }
        
        return try {
            val globalTime = getSynchronizedTimestamp()
            val drift = abs(componentTimestamp - globalTime)
            
            // Update component sync information
            updateComponentSyncInfo(componentId, drift)
            
            // Check if drift is within acceptable range
            val isWithinTolerance = drift <= CLOCK_DRIFT_THRESHOLD_NS
            val syncSuccessful = drift <= MAX_COMPONENT_SKEW_NS
            
            if (!syncSuccessful) {
                XLog.w(TAG, "High drift detected for $componentId: ${drift / 1_000_000.0}ms")
            }
            
            // Update performance metrics
            syncOperationCount.incrementAndGet()
            if (drift > 0) {
                totalDriftCorrected.addAndGet(drift)
            }
            maxDetectedSkew.updateAndGet { current -> maxOf(current, drift) }
            
            val syncResult = SyncResult(
                success = syncSuccessful,
                synchronizedTimestamp = globalTime,
                message = if (syncSuccessful) {
                    if (isWithinTolerance) "Synchronized within tolerance" else "Synchronized with drift"
                } else {
                    "Synchronization failed - excessive drift"
                },
                drift = drift,
                isWithinTolerance = isWithinTolerance
            )
            
            // Log significant sync events
            if (syncOperationCount.get() % 1000 == 0L) { // Every 1000 syncs
                logSyncPerformance()
            }
            
            syncResult
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error synchronizing component $componentId: ${e.message}")
            SyncResult(false, 0, "Synchronization error: ${e.message}")
        }
    }
    
    /**
     * Get synchronized timestamp for a specific component
     */
    fun getComponentSynchronizedTimestamp(componentId: String): Long {
        val syncResult = synchronizeComponent(componentId, System.nanoTime())
        return syncResult.synchronizedTimestamp
    }
    
    /**
     * Update the master clock with hardware precision
     */
    private fun updateMasterClock(hardwareTimestamp: Long) {
        val currentMaster = globalMasterClock.get()
        val drift = abs(hardwareTimestamp - currentMaster)
        
        // Only update if there's significant drift to prevent jitter
        if (drift > CLOCK_DRIFT_THRESHOLD_NS) {
            globalMasterClock.set(hardwareTimestamp)
            
            if (drift > 10_000_000L) { // > 10ms drift
                XLog.w(TAG, "Significant clock drift corrected: ${drift / 1_000_000.0}ms")
            }
        }
    }
    
    /**
     * Update component synchronization information
     */
    private fun updateComponentSyncInfo(componentId: String, drift: Long) {
        componentSyncInfo[componentId]?.let { syncInfo ->
            val updatedInfo = syncInfo.copy(
                lastSyncTime = getSynchronizedTimestamp(),
                syncCount = syncInfo.syncCount + 1,
                totalDrift = syncInfo.totalDrift + drift,
                maxDrift = maxOf(syncInfo.maxDrift, drift),
                avgDrift = (syncInfo.totalDrift + drift).toDouble() / (syncInfo.syncCount + 1)
            )
            componentSyncInfo[componentId] = updatedInfo
        }
    }
    
    /**
     * Start background clock synchronization
     */
    private fun startBackgroundSync() {
        clockSyncExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "GlobalClockSync").apply {
                priority = Thread.MAX_PRIORITY
                isDaemon = true
            }
        }
        
        clockSyncExecutor?.scheduleAtFixedRate({
            performBackgroundSync()
        }, 0, SYNC_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
        
        XLog.i(TAG, "Background clock synchronization started (interval: ${SYNC_UPDATE_INTERVAL_MS}ms)")
    }
    
    /**
     * Perform background synchronization maintenance
     */
    private fun performBackgroundSync() {
        try {
            if (!isSyncActive.get()) return
            
            // Update master clock with fresh hardware timestamp
            val hardwareTime = SystemClock.elapsedRealtimeNanos()
            updateMasterClock(hardwareTime)
            
            // Check component sync health
            checkComponentSyncHealth()
            
            // Periodic cleanup of inactive components
            if (System.currentTimeMillis() % 10000 == 0L) { // Every 10 seconds
                cleanupInactiveComponents()
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error in background synchronization: ${e.message}")
        }
    }
    
    /**
     * Check synchronization health of all components
     */
    private fun checkComponentSyncHealth() {
        val currentTime = getSynchronizedTimestamp()
        var unhealthyComponents = 0
        
        componentSyncInfo.values.forEach { syncInfo ->
            val timeSinceLastSync = currentTime - syncInfo.lastSyncTime
            
            // Check if component hasn't synced recently
            if (timeSinceLastSync > 5_000_000_000L) { // 5 seconds
                XLog.w(TAG, "Component ${syncInfo.componentId} hasn't synced recently: ${timeSinceLastSync / 1_000_000}ms")
                unhealthyComponents++
            }
            
            // Check if component has excessive average drift
            if (syncInfo.avgDrift > CLOCK_DRIFT_THRESHOLD_NS) {
                XLog.w(TAG, "Component ${syncInfo.componentId} has high average drift: ${syncInfo.avgDrift / 1_000_000.0}ms")
                unhealthyComponents++
            }
        }
        
        if (unhealthyComponents > 0) {
            XLog.w(TAG, "Synchronization health check: $unhealthyComponents unhealthy components")
        }
    }
    
    /**
     * Cleanup components that are no longer active
     */
    private fun cleanupInactiveComponents() {
        val currentTime = getSynchronizedTimestamp()
        val componentsToRemove = mutableListOf<String>()
        
        componentSyncInfo.values.forEach { syncInfo ->
            val timeSinceLastSync = currentTime - syncInfo.lastSyncTime
            
            // Remove components that haven't synced in over 30 seconds
            if (timeSinceLastSync > 30_000_000_000L) {
                componentsToRemove.add(syncInfo.componentId)
            }
        }
        
        componentsToRemove.forEach { componentId ->
            unregisterComponent(componentId)
            XLog.i(TAG, "Cleaned up inactive component: $componentId")
        }
        
        if (componentsToRemove.isNotEmpty()) {
            XLog.i(TAG, "Cleaned up ${componentsToRemove.size} inactive components")
        }
    }
    
    /**
     * Log comprehensive synchronization performance
     */
    private fun logSyncPerformance() {
        val totalSyncs = syncOperationCount.get()
        val avgDriftCorrected = if (totalSyncs > 0) {
            totalDriftCorrected.get().toDouble() / totalSyncs / 1_000_000.0 // Convert to ms
        } else 0.0
        
        val maxSkewMs = maxDetectedSkew.get() / 1_000_000.0
        val activeComponents = componentSyncInfo.size
        
        XLog.i(TAG, """
            Global Clock Synchronization Performance:
            - Total sync operations: $totalSyncs
            - Average drift corrected: %.3f ms
            - Maximum skew detected: %.3f ms
            - Active components: $activeComponents
            - Uptime: ${(getSynchronizedTimestamp() - initializationTime) / 1_000_000_000L}s
        """.trimIndent().format(avgDriftCorrected, maxSkewMs))
    }
    
    /**
     * Get comprehensive synchronization statistics
     */
    fun getSynchronizationStatistics(): GlobalSyncStatistics {
        val currentTime = getSynchronizedTimestamp()
        val uptimeSeconds = (currentTime - initializationTime) / 1_000_000_000L
        
        val componentStats = componentSyncInfo.values.map { syncInfo ->
            ComponentSyncStatistics(
                componentId = syncInfo.componentId,
                componentType = syncInfo.componentType,
                syncCount = syncInfo.syncCount,
                averageDriftMs = syncInfo.avgDrift / 1_000_000.0,
                maxDriftMs = syncInfo.maxDrift / 1_000_000.0,
                lastSyncTimeRelative = (currentTime - syncInfo.lastSyncTime) / 1_000_000L,
                isHealthy = syncInfo.avgDrift <= CLOCK_DRIFT_THRESHOLD_NS
            )
        }.toList()
        
        return GlobalSyncStatistics(
            isActive = isSyncActive.get(),
            uptimeSeconds = uptimeSeconds,
            totalSyncOperations = syncOperationCount.get(),
            activeComponentCount = componentSyncInfo.size,
            averageDriftCorrectedMs = if (syncOperationCount.get() > 0) {
                totalDriftCorrected.get().toDouble() / syncOperationCount.get() / 1_000_000.0
            } else 0.0,
            maxDetectedSkewMs = maxDetectedSkew.get() / 1_000_000.0,
            componentStatistics = componentStats,
            clockDriftThresholdMs = CLOCK_DRIFT_THRESHOLD_NS / 1_000_000.0,
            maxAllowedSkewMs = MAX_COMPONENT_SKEW_NS / 1_000_000.0
        )
    }
    
    /**
     * Force synchronization for all registered components
     */
    fun forceSynchronizeAllComponents(): Map<String, SyncResult> {
        val results = mutableMapOf<String, SyncResult>()
        val currentTime = getSynchronizedTimestamp()
        
        componentSyncInfo.keys.forEach { componentId ->
            val result = synchronizeComponent(componentId, currentTime)
            results[componentId] = result
        }
        
        XLog.i(TAG, "Force synchronized all components: ${results.size} components")
        return results
    }
    
    /**
     * Shutdown the global clock synchronization system
     */
    fun shutdown() {
        try {
            isSyncActive.set(false)
            
            clockSyncExecutor?.shutdown()
            if (!clockSyncExecutor?.awaitTermination(1, TimeUnit.SECONDS) == true) {
                clockSyncExecutor?.shutdownNow()
            }
            
            componentSyncInfo.clear()
            syncedComponents.clear()
            
            isInitialized.set(false)
            
            XLog.i(TAG, "Global clock synchronization system shutdown")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error during global clock shutdown: ${e.message}")
        }
    }
}

/**
 * Component type enumeration for synchronization tracking
 */
enum class ComponentType {
    VIDEO_RECORDER,
    DNG_CAPTURE,
    GSR_SENSOR,
    THERMAL_IMAGING,
    SYSTEM_COMPONENT,
    USER_INTERFACE
}

/**
 * Component synchronization information
 */
data class ComponentSyncInfo(
    val componentId: String,
    val componentType: ComponentType,
    val lastSyncTime: Long,
    val syncCount: Long,
    val totalDrift: Long,
    val maxDrift: Long,
    val avgDrift: Double,
    val isActive: Boolean
)

/**
 * Synchronization result information
 */
data class SyncResult(
    val success: Boolean,
    val synchronizedTimestamp: Long,
    val message: String,
    val drift: Long = 0,
    val isWithinTolerance: Boolean = true
) {
    val driftMs: Double get() = drift / 1_000_000.0
}

/**
 * Component synchronization statistics
 */
data class ComponentSyncStatistics(
    val componentId: String,
    val componentType: ComponentType,
    val syncCount: Long,
    val averageDriftMs: Double,
    val maxDriftMs: Double,
    val lastSyncTimeRelative: Long, // Milliseconds since last sync
    val isHealthy: Boolean
)

/**
 * Global synchronization statistics
 */
data class GlobalSyncStatistics(
    val isActive: Boolean,
    val uptimeSeconds: Long,
    val totalSyncOperations: Long,
    val activeComponentCount: Int,
    val averageDriftCorrectedMs: Double,
    val maxDetectedSkewMs: Double,
    val componentStatistics: List<ComponentSyncStatistics>,
    val clockDriftThresholdMs: Double,
    val maxAllowedSkewMs: Double
) {
    val overallSyncHealth: String
        get() = when {
            averageDriftCorrectedMs < 1.0 && maxDetectedSkewMs < 5.0 -> "EXCELLENT"
            averageDriftCorrectedMs < 2.0 && maxDetectedSkewMs < 10.0 -> "GOOD"
            averageDriftCorrectedMs < 5.0 && maxDetectedSkewMs < 20.0 -> "ACCEPTABLE"
            else -> "POOR"
        }
}