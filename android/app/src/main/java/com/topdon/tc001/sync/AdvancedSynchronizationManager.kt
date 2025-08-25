package com.topdon.tc001.sync

import android.content.Context
import android.os.SystemClock
import com.elvishew.xlog.XLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced Synchronization Manager for Bucika GSR Platform
 * 
 * Provides millisecond-level synchronization accuracy between Android devices and PC orchestrator:
 * - SNTP-like time synchronization with nanosecond precision
 * - Network latency compensation using multiple measurement rounds
 * - Clock drift detection and correction
 * - Adaptive synchronization based on network conditions
 * - Real-time synchronization quality monitoring
 * 
 * Key Features:
 * - Sub-millisecond accuracy under optimal conditions
 * - Automatic fallback to system time if network sync fails
 * - Quality scoring based on network stability
 * - Continuous monitoring and adjustment
 * - Integration with GSR data timestamping
 */
class AdvancedSynchronizationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AdvancedSyncManager"
        
        // Synchronization parameters
        private const val SYNC_SERVER_PORT = 9123
        private const val SYNC_TIMEOUT_MS = 5000
        private const val SYNC_SAMPLES = 10 // Multiple samples for accuracy
        private const val SYNC_INTERVAL_MS = 30_000L // Sync every 30 seconds
        private const val MAX_CLOCK_DRIFT_MS = 100 // Maximum allowed drift
        
        // Quality thresholds
        private const val EXCELLENT_ACCURACY_MS = 1.0
        private const val GOOD_ACCURACY_MS = 5.0
        private const val ACCEPTABLE_ACCURACY_MS = 20.0
        
        // Network latency compensation
        private const val MIN_RTT_SAMPLES = 5
        private const val RTT_OUTLIER_THRESHOLD = 2.0 // Standard deviations
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Synchronization state
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    // Time offset tracking
    private var baseTimeOffset: Long = 0L // Offset between device and server time
    private var lastSyncTime: Long = 0L
    private var clockDriftRate: Double = 0.0 // Microseconds per millisecond
    
    // Network performance tracking
    private val rttHistory = mutableListOf<Double>()
    private var averageRtt: Double = 0.0
    private var rttStandardDeviation: Double = 0.0
    
    // Sync server address
    private var syncServerAddress: InetAddress? = null
    
    /**
     * Data class representing synchronization state
     */
    data class SyncState(
        val isActive: Boolean = false,
        val lastSyncTime: Long = 0L,
        val syncAccuracyMs: Double = Double.MAX_VALUE,
        val syncQuality: SyncQuality = SyncQuality.UNKNOWN,
        val clockOffset: Long = 0L,
        val networkRtt: Double = 0.0,
        val syncServerAddress: String = "",
        val syncCount: Int = 0,
        val errorCount: Int = 0,
        val driftRate: Double = 0.0
    )
    
    enum class SyncQuality {
        EXCELLENT,    // < 1ms accuracy
        GOOD,         // < 5ms accuracy  
        ACCEPTABLE,   // < 20ms accuracy
        POOR,         // > 20ms accuracy
        FAILED,       // Sync failed
        UNKNOWN       // Not yet measured
    }

    /**
     * Start synchronization with PC orchestrator
     */
    fun startSync(serverAddress: String) {
        XLog.i(TAG, "Starting advanced synchronization with server: $serverAddress")
        
        try {
            syncServerAddress = InetAddress.getByName(serverAddress)
            
            coroutineScope.launch {
                performInitialSync()
                startContinuousSync()
            }
            
            _syncState.value = _syncState.value.copy(
                isActive = true,
                syncServerAddress = serverAddress
            )
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start synchronization: ${e.message}", e)
            _syncState.value = _syncState.value.copy(
                syncQuality = SyncQuality.FAILED,
                errorCount = _syncState.value.errorCount + 1
            )
        }
    }

    /**
     * Stop synchronization
     */
    fun stopSync() {
        XLog.i(TAG, "Stopping synchronization")
        
        coroutineScope.cancel()
        _syncState.value = SyncState()
    }

    /**
     * Get synchronized timestamp for current time
     */
    fun getSyncedTimestamp(): Long {
        val currentTime = SystemClock.elapsedRealtimeNanos()
        val driftCorrection = (currentTime - lastSyncTime) * clockDriftRate * 1e-6
        return currentTime + baseTimeOffset + driftCorrection.toLong()
    }

    /**
     * Get synchronized timestamp for specific device timestamp
     */
    fun getSyncedTimestamp(deviceTimestamp: Long): Long {
        val timeSinceSync = deviceTimestamp - lastSyncTime
        val driftCorrection = timeSinceSync * clockDriftRate * 1e-6
        return deviceTimestamp + baseTimeOffset + driftCorrection.toLong()
    }

    /**
     * Perform initial synchronization with multiple samples
     */
    private suspend fun performInitialSync() {
        XLog.i(TAG, "Performing initial synchronization with ${SYNC_SAMPLES} samples")
        
        val syncResults = mutableListOf<SyncResult>()
        
        repeat(SYNC_SAMPLES) { sample ->
            try {
                val result = performSingleSync()
                syncResults.add(result)
                
                XLog.d(TAG, "Sync sample $sample: offset=${result.timeOffset}ns, rtt=${result.networkRtt}ms")
                
                // Brief delay between samples
                delay(100)
                
            } catch (e: Exception) {
                XLog.w(TAG, "Sync sample $sample failed: ${e.message}")
            }
        }
        
        if (syncResults.isNotEmpty()) {
            processSyncResults(syncResults)
        } else {
            XLog.e(TAG, "All sync samples failed - using system time")
            _syncState.value = _syncState.value.copy(syncQuality = SyncQuality.FAILED)
        }
    }

    /**
     * Perform single synchronization measurement
     */
    private suspend fun performSingleSync(): SyncResult {
        val socket = DatagramSocket()
        socket.soTimeout = SYNC_TIMEOUT_MS
        
        try {
            // Prepare sync request
            val requestData = "SYNC_REQUEST".toByteArray()
            val requestPacket = DatagramPacket(
                requestData,
                requestData.size,
                syncServerAddress,
                SYNC_SERVER_PORT
            )
            
            // Record send time with high precision
            val sendTime = SystemClock.elapsedRealtimeNanos()
            socket.send(requestPacket)
            
            // Receive response
            val responseBuffer = ByteArray(1024)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            
            // Record receive time immediately
            val receiveTime = SystemClock.elapsedRealtimeNanos()
            
            // Parse server response
            val responseString = String(responsePacket.data, 0, responsePacket.length)
            val serverTime = parseServerTime(responseString)
            
            // Calculate network round-trip time
            val networkRtt = (receiveTime - sendTime) / 1_000_000.0 // Convert to milliseconds
            
            // Estimate one-way delay (assume symmetric network)
            val oneWayDelay = networkRtt / 2.0 * 1_000_000 // Convert to nanoseconds
            
            // Calculate time offset (server time - local time at transmission)
            val estimatedServerReceiveTime = sendTime + oneWayDelay.toLong()
            val timeOffset = serverTime - estimatedServerReceiveTime
            
            return SyncResult(
                timeOffset = timeOffset,
                networkRtt = networkRtt,
                sendTime = sendTime,
                receiveTime = receiveTime,
                serverTime = serverTime,
                success = true
            )
            
        } catch (e: Exception) {
            XLog.w(TAG, "Single sync failed: ${e.message}")
            return SyncResult(success = false, error = e.message ?: "Unknown error")
        } finally {
            socket.close()
        }
    }

    /**
     * Process multiple sync results to determine best offset
     */
    private fun processSyncResults(results: List<SyncResult>) {
        val validResults = results.filter { it.success }
        
        if (validResults.isEmpty()) {
            XLog.e(TAG, "No valid sync results available")
            _syncState.value = _syncState.value.copy(syncQuality = SyncQuality.FAILED)
            return
        }
        
        // Calculate network RTT statistics
        val rtts = validResults.map { it.networkRtt }
        averageRtt = rtts.average()
        rttStandardDeviation = calculateStandardDeviation(rtts)
        
        // Filter out RTT outliers for better accuracy
        val filteredResults = validResults.filter { result ->
            abs(result.networkRtt - averageRtt) <= RTT_OUTLIER_THRESHOLD * rttStandardDeviation
        }.takeIf { it.size >= MIN_RTT_SAMPLES } ?: validResults
        
        // Use weighted average based on RTT (lower RTT = higher weight)
        val weights = filteredResults.map { 1.0 / (it.networkRtt + 1.0) }
        val totalWeight = weights.sum()
        
        val weightedOffset = filteredResults.zip(weights) { result, weight ->
            result.timeOffset * weight
        }.sum() / totalWeight
        
        // Update synchronization state
        baseTimeOffset = weightedOffset.toLong()
        lastSyncTime = SystemClock.elapsedRealtimeNanos()
        
        // Calculate sync accuracy estimate
        val offsetVariance = filteredResults.map { result ->
            (result.timeOffset - weightedOffset).pow(2.0)
        }.average()
        val syncAccuracy = sqrt(offsetVariance) / 1_000_000.0 // Convert to milliseconds
        
        // Update RTT history
        rttHistory.clear()
        rttHistory.addAll(rtts)
        if (rttHistory.size > 50) {
            rttHistory.removeAt(0)
        }
        
        // Determine sync quality
        val syncQuality = when {
            syncAccuracy <= EXCELLENT_ACCURACY_MS -> SyncQuality.EXCELLENT
            syncAccuracy <= GOOD_ACCURACY_MS -> SyncQuality.GOOD
            syncAccuracy <= ACCEPTABLE_ACCURACY_MS -> SyncQuality.ACCEPTABLE
            else -> SyncQuality.POOR
        }
        
        XLog.i(TAG, "Sync complete - Offset: ${baseTimeOffset}ns, Accuracy: ${syncAccuracy}ms, Quality: ${syncQuality}")
        
        _syncState.value = _syncState.value.copy(
            lastSyncTime = lastSyncTime,
            syncAccuracyMs = syncAccuracy,
            syncQuality = syncQuality,
            clockOffset = baseTimeOffset,
            networkRtt = averageRtt,
            syncCount = _syncState.value.syncCount + 1
        )
    }

    /**
     * Start continuous synchronization monitoring
     */
    private suspend fun startContinuousSync() {
        XLog.i(TAG, "Starting continuous synchronization monitoring")
        
        while (coroutineScope.isActive) {
            delay(SYNC_INTERVAL_MS)
            
            try {
                // Check for clock drift
                val driftCheck = performSingleSync()
                if (driftCheck.success) {
                    detectClockDrift(driftCheck)
                }
                
                // Perform periodic re-sync if needed
                if (shouldPerformResync()) {
                    XLog.i(TAG, "Performing periodic re-synchronization")
                    performInitialSync()
                }
                
            } catch (e: Exception) {
                XLog.w(TAG, "Continuous sync check failed: ${e.message}")
                _syncState.value = _syncState.value.copy(
                    errorCount = _syncState.value.errorCount + 1
                )
            }
        }
    }

    /**
     * Detect and compensate for clock drift
     */
    private fun detectClockDrift(currentSync: SyncResult) {
        val currentTime = SystemClock.elapsedRealtimeNanos()
        val timeSinceLastSync = currentTime - lastSyncTime
        
        if (timeSinceLastSync > 0) {
            val expectedOffset = baseTimeOffset + (timeSinceLastSync * clockDriftRate * 1e-6).toLong()
            val actualOffset = currentSync.timeOffset
            val driftError = actualOffset - expectedOffset
            
            // Update drift rate using exponential smoothing
            val newDriftRate = (driftError * 1e6) / timeSinceLastSync.toDouble() // microseconds per nanosecond
            clockDriftRate = clockDriftRate * 0.8 + newDriftRate * 0.2 // Smooth drift rate
            
            XLog.d(TAG, "Clock drift detection - Error: ${driftError}ns, Rate: ${clockDriftRate}µs/ms")
            
            _syncState.value = _syncState.value.copy(driftRate = clockDriftRate)
        }
    }

    /**
     * Determine if re-synchronization is needed
     */
    private fun shouldPerformResync(): Boolean {
        val timeSinceLastSync = SystemClock.elapsedRealtimeNanos() - lastSyncTime
        val estimatedDrift = abs(timeSinceLastSync * clockDriftRate * 1e-6) // nanoseconds
        
        return estimatedDrift > MAX_CLOCK_DRIFT_MS * 1_000_000 // Convert ms to ns
    }

    /**
     * Parse server time from response
     */
    private fun parseServerTime(response: String): Long {
        return try {
            val parts = response.split(":")
            if (parts.size >= 2 && parts[0] == "SYNC_RESPONSE") {
                parts[1].toLong()
            } else {
                throw IllegalArgumentException("Invalid sync response format: $response")
            }
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to parse server time: $response", e)
            throw e
        }
    }

    /**
     * Calculate standard deviation of a list of doubles
     */
    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2.0) }.average()
        return sqrt(variance)
    }

    /**
     * Data class for individual sync results
     */
    private data class SyncResult(
        val timeOffset: Long = 0L,
        val networkRtt: Double = 0.0,
        val sendTime: Long = 0L,
        val receiveTime: Long = 0L,
        val serverTime: Long = 0L,
        val success: Boolean = false,
        val error: String = ""
    )
}