package com.multisensor.recording.calibration

import com.multisensor.recording.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class SyncClockManager
@Inject
constructor(
    private val logger: Logger,
) {
    companion object {
        private const val TARGET_ACCURACY_MS = 10L
        private const val MAX_OFFSET_DRIFT_MS = 50L
        private const val SYNC_VALIDITY_DURATION_MS = 180000L

        private const val MIN_SYNC_SAMPLES = 3
        private const val MAX_SYNC_SAMPLES = 8
        private const val OUTLIER_THRESHOLD_FACTOR = 2.0
        private const val DRIFT_CORRECTION_INTERVAL_MS = 30000L

        private const val MAX_ACCEPTABLE_RTT_MS = 200L
        private const val LATENCY_MEASUREMENT_SAMPLES = 5
    }

    data class SyncMeasurement(
        val t1: Long,
        val t2: Long,
        val t3: Long,
        val t4: Long,
        val roundTripDelay: Long = (t4 - t1) - (t3 - t2),
        val clockOffset: Long = ((t2 - t1) + (t3 - t4)) / 2,
        val timestamp: Long = System.currentTimeMillis(),
    )

    data class SyncQualityMetrics(
        val accuracy: Float,
        val stability: Float,
        val latency: Long,
        val jitter: Float,
        val driftRate: Double,
        val sampleCount: Int,
        val lastUpdateTime: Long = System.currentTimeMillis(),
    )

    data class SyncResult(
        val success: Boolean,
        val measurement: SyncMeasurement,
        val error: String? = null,
    )

    private val mutex = Mutex()
    private var clockOffsetMs: Long = 0L
    private var lastSyncTimestamp: Long = 0L
    private var pcReferenceTime: Long = 0L
    private var isSynchronized: Boolean = false

    private val syncMeasurements = ConcurrentLinkedQueue<SyncMeasurement>()
    private val latencyMeasurements = ConcurrentLinkedQueue<Long>()
    private var driftRate: Double = 0.0
    private var lastDriftCorrectionTime: Long = 0L
    private var syncQualityScore: Float = 0.0f

    data class SyncStatus(
        val isSynchronized: Boolean,
        val clockOffsetMs: Long,
        val lastSyncTimestamp: Long,
        val pcReferenceTime: Long,
        val syncAge: Long,
    )

    suspend fun synchronizeWithPc(
        pcTimestamp: Long,
        syncId: String? = null,
    ): Boolean =
        mutex.withLock {
            return try {
                logger.info("[DEBUG_LOG] Enhanced NTP-style synchronisation requested")
                syncId?.let { logger.info("[DEBUG_LOG] Sync ID: $it") }

                val syncResult = performNTPStyleSync(pcTimestamp)

                if (syncResult.success) {
                    syncMeasurements.offer(syncResult.measurement)

                    while (syncMeasurements.size > MAX_SYNC_SAMPLES) {
                        syncMeasurements.poll()
                    }

                    val enhancedOffset = calculateEnhancedOffset()

                    updateDriftRate(enhancedOffset)

                    val correctedOffset = applyDriftCorrection(enhancedOffset)

                    clockOffsetMs = correctedOffset
                    lastSyncTimestamp = System.currentTimeMillis()
                    pcReferenceTime = pcTimestamp
                    isSynchronized = true

                    updateSyncQualityMetrics()

                    logger.info("[DEBUG_LOG] Enhanced sync complete - Offset: ${clockOffsetMs}ms, Quality: $syncQualityScore")
                    true
                } else {
                    logger.error("NTP-style synchronisation failed: ${syncResult.error}")
                    false
                }
            } catch (e: Exception) {
                logger.error("Error during enhanced clock synchronisation", e)
                false
            }
        }

    fun getSyncedTimestamp(deviceTimestamp: Long = System.currentTimeMillis()): Long =
        if (isSynchronized) {
            deviceTimestamp + clockOffsetMs
        } else {
            logger.warning("Clock not synchronized, using device timestamp")
            deviceTimestamp
        }

    fun getCurrentSyncedTime(): Long = getSyncedTimestamp()

    fun isSyncValid(): Boolean {
        if (!isSynchronized) return false

        val syncAge = System.currentTimeMillis() - lastSyncTimestamp
        return syncAge < SYNC_VALIDITY_DURATION_MS
    }

    fun getSyncStatus(): SyncStatus {
        val currentTime = System.currentTimeMillis()
        val syncAge = if (isSynchronized) currentTime - lastSyncTimestamp else -1L

        return SyncStatus(
            isSynchronized = isSynchronized,
            clockOffsetMs = clockOffsetMs,
            lastSyncTimestamp = lastSyncTimestamp,
            pcReferenceTime = pcReferenceTime,
            syncAge = syncAge,
        )
    }

    suspend fun resetSync(): Unit =
        mutex.withLock {
            logger.info("[DEBUG_LOG] Resetting clock synchronisation")
            clockOffsetMs = 0L
            lastSyncTimestamp = 0L
            pcReferenceTime = 0L
            isSynchronized = false
        }

    fun deviceToPcTime(deviceTimestamp: Long): Long = deviceTimestamp + clockOffsetMs

    fun pcToDeviceTime(pcTimestamp: Long): Long = pcTimestamp - clockOffsetMs

    fun getSyncStatistics(): String {
        val status = getSyncStatus()
        return buildString {
            appendLine("Clock Synchronisation Statistics:")
            appendLine("  Synchronised: ${status.isSynchronized}")
            appendLine("  Clock Offset: ${status.clockOffsetMs}ms")
            appendLine("  Last Sync: ${if (status.lastSyncTimestamp > 0) "${status.syncAge}ms ago" else "Never"}")
            appendLine("  PC Reference Time: ${status.pcReferenceTime}")
            appendLine("  Sync Valid: ${isSyncValid()}")
            appendLine("  Current Synced Time: ${getCurrentSyncedTime()}")
        }
    }

    fun estimateNetworkLatency(
        pcTimestamp: Long,
        requestSentTime: Long,
    ): Long {
        val responseReceivedTime = System.currentTimeMillis()
        val roundTripTime = responseReceivedTime - requestSentTime

        val estimatedLatency = roundTripTime / 2

        logger.debug("[DEBUG_LOG] Network latency estimation:")
        logger.debug("[DEBUG_LOG] Round-trip time: ${roundTripTime}ms")
        logger.debug("[DEBUG_LOG] Estimated latency: ${estimatedLatency}ms")

        return estimatedLatency
    }

    fun validateSyncHealth(): Boolean {
        if (!isSynchronized) {
            logger.warning("Clock synchronisation not established")
            return false
        }

        if (!isSyncValid()) {
            logger.warning("Clock synchronisation expired - re-sync recommended")
            return false
        }

        val syncAge = System.currentTimeMillis() - lastSyncTimestamp
        if (syncAge > SYNC_VALIDITY_DURATION_MS / 2) {
            logger.info("Clock synchronisation aging - consider re-sync soon")
        }

        return true
    }

    private suspend fun performNTPStyleSync(pcTimestamp: Long): SyncResult {
        return try {
            val t1 = System.currentTimeMillis()
            val t2 = pcTimestamp
            val t3 = pcTimestamp

            delay(1)

            val t4 = System.currentTimeMillis()

            val roundTripTime = t4 - t1
            if (roundTripTime > MAX_ACCEPTABLE_RTT_MS) {
                return SyncResult(
                    false,
                    SyncMeasurement(t1, t2, t3, t4),
                    "Round-trip time too high: ${roundTripTime}ms"
                )
            }

            val measurement = SyncMeasurement(t1, t2, t3, t4)

            logger.debug("[DEBUG_LOG] NTP measurement - RTT: ${measurement.roundTripDelay}ms, Offset: ${measurement.clockOffset}ms")

            SyncResult(true, measurement)
        } catch (e: Exception) {
            SyncResult(false, SyncMeasurement(0, 0, 0, 0), "NTP sync failed: ${e.message}")
        }
    }

    private fun calculateEnhancedOffset(): Long {
        if (syncMeasurements.size < MIN_SYNC_SAMPLES) {
            return syncMeasurements.lastOrNull()?.clockOffset ?: 0L
        }

        val offsets = syncMeasurements.map { it.clockOffset }

        val mean = offsets.average()
        val variance = offsets.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)

        val filteredOffsets =
            offsets.filter {
                abs(it - mean) <= OUTLIER_THRESHOLD_FACTOR * stdDev
            }

        val weights =
            filteredOffsets.indices.map { index ->
                1.0 + (index.toDouble() / filteredOffsets.size)
            }

        val weightedSum = filteredOffsets.zip(weights).sumOf { (offset, weight) -> offset * weight }
        val totalWeight = weights.sum()

        val enhancedOffset = (weightedSum / totalWeight).toLong()

        logger.debug("[DEBUG_LOG] Enhanced offset calculation - Mean: $mean, StdDev: $stdDev, Enhanced: $enhancedOffset")

        return enhancedOffset
    }

    private fun updateDriftRate(newOffset: Long) {
        if (lastSyncTimestamp > 0 && clockOffsetMs != 0L) {
            val timeDelta = System.currentTimeMillis() - lastSyncTimestamp
            val offsetDelta = newOffset - clockOffsetMs

            if (timeDelta > 0) {
                val newDriftRate = offsetDelta.toDouble() / timeDelta.toDouble()

                driftRate =
                    if (driftRate == 0.0) {
                        newDriftRate
                    } else {
                        0.7 * driftRate + 0.3 * newDriftRate
                    }

                logger.debug("[DEBUG_LOG] Drift rate updated: $driftRate ms/ms")
            }
        }
    }

    private fun applyDriftCorrection(offset: Long): Long {
        if (driftRate == 0.0 || lastDriftCorrectionTime == 0L) {
            lastDriftCorrectionTime = System.currentTimeMillis()
            return offset
        }

        val timeSinceLastCorrection = System.currentTimeMillis() - lastDriftCorrectionTime
        val driftCorrection = (driftRate * timeSinceLastCorrection).toLong()

        val correctedOffset = offset - driftCorrection

        logger.debug("[DEBUG_LOG] Drift correction applied: ${driftCorrection}ms, Corrected offset: ${correctedOffset}ms")

        lastDriftCorrectionTime = System.currentTimeMillis()
        return correctedOffset
    }

    private fun updateSyncQualityMetrics() {
        if (syncMeasurements.isEmpty()) {
            syncQualityScore = 0.0f
            return
        }

        val offsets = syncMeasurements.map { it.clockOffset }
        val rtts = syncMeasurements.map { it.roundTripDelay }

        val offsetMean = offsets.average()
        val offsetVariance = offsets.map { (it - offsetMean).pow(2) }.average()
        val accuracy = sqrt(offsetVariance).toFloat()

        val stability =
            if (offsetMean != 0.0) {
                (1.0f / (accuracy / abs(offsetMean).toFloat())).coerceIn(0.0f, 1.0f)
            } else {
                if (accuracy < TARGET_ACCURACY_MS) 1.0f else 0.0f
            }

        val avgLatency = (rtts.average() / 2).toLong()
        val latencyMean = rtts.average()
        val jitter = sqrt(rtts.map { (it - latencyMean).pow(2) }.average()).toFloat()

        val accuracyScore = (TARGET_ACCURACY_MS.toFloat() / (accuracy + 1.0f)).coerceIn(0.0f, 1.0f)
        val latencyScore = (50.0f / (avgLatency + 1.0f)).coerceIn(0.0f, 1.0f)
        val jitterScore = (10.0f / (jitter + 1.0f)).coerceIn(0.0f, 1.0f)

        syncQualityScore = (accuracyScore * 0.5f + stability * 0.3f + latencyScore * 0.1f + jitterScore * 0.1f)

        logger.debug("[DEBUG_LOG] Quality metrics - Accuracy: ${accuracy}ms, Stability: $stability, Quality: $syncQualityScore")
    }

    fun getSyncQualityMetrics(): SyncQualityMetrics? {
        if (syncMeasurements.isEmpty()) return null

        val offsets = syncMeasurements.map { it.clockOffset }
        val rtts = syncMeasurements.map { it.roundTripDelay }

        val offsetMean = offsets.average()
        val accuracy = sqrt(offsets.map { (it - offsetMean).pow(2) }.average()).toFloat()
        val stability = syncQualityScore
        val avgLatency = (rtts.average() / 2).toLong()
        val jitter = sqrt(rtts.map { (it - rtts.average()).pow(2) }.average()).toFloat()

        return SyncQualityMetrics(
            accuracy = accuracy,
            stability = stability,
            latency = avgLatency,
            jitter = jitter,
            driftRate = driftRate,
            sampleCount = syncMeasurements.size,
        )
    }
}
