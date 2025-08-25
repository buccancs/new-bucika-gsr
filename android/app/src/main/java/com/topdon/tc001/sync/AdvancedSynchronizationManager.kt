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

class AdvancedSynchronizationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AdvancedSyncManager"
        
        private const val SYNC_SERVER_PORT = 9123
        private const val SYNC_TIMEOUT_MS = 5000
        private const val SYNC_SAMPLES = 10
        private const val SYNC_INTERVAL_MS = 30_000L
        private const val MAX_CLOCK_DRIFT_MS = 100
        
        private const val EXCELLENT_ACCURACY_MS = 1.0
        private const val GOOD_ACCURACY_MS = 5.0
        private const val ACCEPTABLE_ACCURACY_MS = 20.0
        
        private const val MIN_RTT_SAMPLES = 5
        private const val RTT_OUTLIER_THRESHOLD = 2.0
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private var baseTimeOffset: Long = 0L
    private var lastSyncTime: Long = 0L
    private var clockDriftRate: Double = 0.0
    
    private val rttHistory = mutableListOf<Double>()
    private var averageRtt: Double = 0.0
    private var rttStandardDeviation: Double = 0.0
    
    private var syncServerAddress: InetAddress? = null
    
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
        EXCELLENT,
        GOOD,
        ACCEPTABLE,
        POOR,
        FAILED,
        UNKNOWN
    }

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

    fun stopSync() {
        XLog.i(TAG, "Stopping synchronization")
        
        coroutineScope.cancel()
        _syncState.value = SyncState()
    }

    fun getSyncedTimestamp(): Long {
        val currentTime = SystemClock.elapsedRealtimeNanos()
        val driftCorrection = (currentTime - lastSyncTime) * clockDriftRate * 1e-6
        return currentTime + baseTimeOffset + driftCorrection.toLong()
    }

    fun getSyncedTimestamp(deviceTimestamp: Long): Long {
        val timeSinceSync = deviceTimestamp - lastSyncTime
        val driftCorrection = timeSinceSync * clockDriftRate * 1e-6
        return deviceTimestamp + baseTimeOffset + driftCorrection.toLong()
    }

    private suspend fun performInitialSync() {
        XLog.i(TAG, "Performing initial synchronization with ${SYNC_SAMPLES} samples")
        
        val syncResults = mutableListOf<SyncResult>()
        
        repeat(SYNC_SAMPLES) { sample ->
            try {
                val result = performSingleSync()
                syncResults.add(result)
                
                XLog.d(TAG, "Sync sample $sample: offset=${result.timeOffset}ns, rtt=${result.networkRtt}ms")
                
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

    private suspend fun performSingleSync(): SyncResult {
        val socket = DatagramSocket()
        socket.soTimeout = SYNC_TIMEOUT_MS
        
        try {

            val requestData = "SYNC_REQUEST".toByteArray()
            val requestPacket = DatagramPacket(
                requestData,
                requestData.size,
                syncServerAddress,
                SYNC_SERVER_PORT
            )
            
            val sendTime = SystemClock.elapsedRealtimeNanos()
            socket.send(requestPacket)
            
            val responseBuffer = ByteArray(1024)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            
            val receiveTime = SystemClock.elapsedRealtimeNanos()
            
            val responseString = String(responsePacket.data, 0, responsePacket.length)
            val serverTime = parseServerTime(responseString)
            
            val networkRtt = (receiveTime - sendTime) / 1_000_000.0
            
            val oneWayDelay = networkRtt / 2.0 * 1_000_000
            
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

    private fun processSyncResults(results: List<SyncResult>) {
        val validResults = results.filter { it.success }
        
        if (validResults.isEmpty()) {
            XLog.e(TAG, "No valid sync results available")
            _syncState.value = _syncState.value.copy(syncQuality = SyncQuality.FAILED)
            return
        }
        
        val rtts = validResults.map { it.networkRtt }
        averageRtt = rtts.average()
        rttStandardDeviation = calculateStandardDeviation(rtts)
        
        val filteredResults = validResults.filter { result ->
            abs(result.networkRtt - averageRtt) <= RTT_OUTLIER_THRESHOLD * rttStandardDeviation
        }.takeIf { it.size >= MIN_RTT_SAMPLES } ?: validResults
        
        val weights = filteredResults.map { 1.0 / (it.networkRtt + 1.0) }
        val totalWeight = weights.sum()
        
        val weightedOffset = filteredResults.zip(weights) { result, weight ->
            result.timeOffset * weight
        }.sum() / totalWeight
        
        baseTimeOffset = weightedOffset.toLong()
        lastSyncTime = SystemClock.elapsedRealtimeNanos()
        
        val offsetVariance = filteredResults.map { result ->
            (result.timeOffset - weightedOffset).pow(2.0)
        }.average()
        val syncAccuracy = sqrt(offsetVariance) / 1_000_000.0
        
        rttHistory.clear()
        rttHistory.addAll(rtts)
        if (rttHistory.size > 50) {
            rttHistory.removeAt(0)
        }
        
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

    private suspend fun startContinuousSync() {
        XLog.i(TAG, "Starting continuous synchronization monitoring")
        
        while (coroutineScope.isActive) {
            delay(SYNC_INTERVAL_MS)
            
            try {

                val driftCheck = performSingleSync()
                if (driftCheck.success) {
                    detectClockDrift(driftCheck)
                }
                
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

    private fun detectClockDrift(currentSync: SyncResult) {
        val currentTime = SystemClock.elapsedRealtimeNanos()
        val timeSinceLastSync = currentTime - lastSyncTime
        
        if (timeSinceLastSync > 0) {
            val expectedOffset = baseTimeOffset + (timeSinceLastSync * clockDriftRate * 1e-6).toLong()
            val actualOffset = currentSync.timeOffset
            val driftError = actualOffset - expectedOffset
            
            val newDriftRate = (driftError * 1e6) / timeSinceLastSync.toDouble()
            clockDriftRate = clockDriftRate * 0.8 + newDriftRate * 0.2
            
            XLog.d(TAG, "Clock drift detection - Error: ${driftError}ns, Rate: ${clockDriftRate}µs/ms")
            
            _syncState.value = _syncState.value.copy(driftRate = clockDriftRate)
        }
    }

    private fun shouldPerformResync(): Boolean {
        val timeSinceLastSync = SystemClock.elapsedRealtimeNanos() - lastSyncTime
        val estimatedDrift = abs(timeSinceLastSync * clockDriftRate * 1e-6)
        
        return estimatedDrift > MAX_CLOCK_DRIFT_MS * 1_000_000
    }

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

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2.0) }.average()
        return sqrt(variance)
    }

    private data class SyncResult(
        val timeOffset: Long = 0L,
        val networkRtt: Double = 0.0,
        val sendTime: Long = 0L,
        val receiveTime: Long = 0L,
        val serverTime: Long = 0L,
        val success: Boolean = false,
        val error: String = ""
    )
