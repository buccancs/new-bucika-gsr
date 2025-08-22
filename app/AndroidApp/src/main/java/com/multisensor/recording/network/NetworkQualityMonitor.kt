package com.multisensor.recording.network

import com.multisensor.recording.util.Logger
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class NetworkQualityMonitor
@Inject
constructor(
    private val logger: Logger,
) {
    companion object {
        private const val MONITORING_INTERVAL_MS = 5000L
        private const val LATENCY_SAMPLES = 3
        private const val BANDWIDTH_WINDOW_SIZE = 5
        private const val SOCKET_TIMEOUT_MS = 3000

        private const val PERFECT_LATENCY_MS = 50
        private const val EXCELLENT_LATENCY_MS = 100
        private const val GOOD_LATENCY_MS = 200
        private const val FAIR_LATENCY_MS = 500

        private const val PERFECT_BANDWIDTH_KBPS = 2000
        private const val EXCELLENT_BANDWIDTH_KBPS = 1000
        private const val GOOD_BANDWIDTH_KBPS = 500
        private const val FAIR_BANDWIDTH_KBPS = 100
    }

    data class NetworkQuality(
        val score: Int,
        val latencyMs: Long,
        val bandwidthKbps: Double,
        val timestamp: Long = System.currentTimeMillis(),
    )

    interface NetworkQualityListener {
        fun onNetworkQualityChanged(quality: NetworkQuality)
    }

    private var monitoringJob: Job? = null
    private var isMonitoring = false
    private val listeners = mutableSetOf<NetworkQualityListener>()

    private val latencyHistory = mutableListOf<Long>()
    private val bandwidthHistory = mutableListOf<Double>()
    private var lastFrameTransmissionTime = 0L
    private var lastFrameSize = 0L

    private var currentQuality = NetworkQuality(3, 100, 1000.0)
    private var serverHost = "192.168.1.100"
    private var serverPort = 8080

    fun startMonitoring(
        host: String,
        port: Int,
    ) {
        if (isMonitoring) {
            logger.info("[DEBUG_LOG] NetworkQualityMonitor already monitoring")
            return
        }

        serverHost = host
        serverPort = port
        isMonitoring = true

        logger.info("[DEBUG_LOG] Starting network quality monitoring for $host:$port")

        monitoringJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isMonitoring) {
                    try {
                        val quality = assessNetworkQuality()
                        updateNetworkQuality(quality)
                        delay(MONITORING_INTERVAL_MS)
                    } catch (e: Exception) {
                        logger.error("Error during network quality assessment", e)
                        delay(MONITORING_INTERVAL_MS)
                    }
                }
            }
    }

    fun stopMonitoring() {
        logger.info("[DEBUG_LOG] Stopping network quality monitoring")
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun addListener(listener: NetworkQualityListener) {
        listeners.add(listener)
        listener.onNetworkQualityChanged(currentQuality)
    }

    fun removeListener(listener: NetworkQualityListener) {
        listeners.remove(listener)
    }

    fun recordFrameTransmission(frameSizeBytes: Long) {
        val currentTime = System.currentTimeMillis()

        if (lastFrameTransmissionTime > 0) {
            val timeDeltaMs = currentTime - lastFrameTransmissionTime
            if (timeDeltaMs > 0) {
                val bandwidth = (frameSizeBytes * 8.0) / (timeDeltaMs / 1000.0) / 1000.0
                addBandwidthSample(bandwidth)
            }
        }

        lastFrameTransmissionTime = currentTime
        lastFrameSize = frameSizeBytes
    }

    fun getCurrentQuality(): NetworkQuality = currentQuality

    private suspend fun assessNetworkQuality(): NetworkQuality {
        val latency = measureLatency()
        val bandwidth = calculateAverageBandwidth()
        val score = calculateQualityScore(latency, bandwidth)

        logger.debug("[DEBUG_LOG] Network assessment - Latency: ${latency}ms, Bandwidth: ${bandwidth}Kbps, Score: $score")

        return NetworkQuality(score, latency, bandwidth)
    }

    private suspend fun measureLatency(): Long =
        withContext(Dispatchers.IO) {
            val latencies = mutableListOf<Long>()

            repeat(LATENCY_SAMPLES) {
                try {
                    val startTime = System.currentTimeMillis()
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(serverHost, serverPort), SOCKET_TIMEOUT_MS)
                    }
                    val latency = System.currentTimeMillis() - startTime
                    latencies.add(latency)
                } catch (e: Exception) {
                    logger.debug("Latency measurement failed: ${e.message}")
                    latencies.add(SOCKET_TIMEOUT_MS.toLong())
                }

                if (it < LATENCY_SAMPLES - 1) {
                    delay(100)
                }
            }

            val averageLatency = latencies.average().toLong()
            addLatencySample(averageLatency)
            return@withContext averageLatency
        }

    private fun calculateAverageBandwidth(): Double =
        if (bandwidthHistory.isNotEmpty()) {
            bandwidthHistory.average()
        } else {
            1000.0
        }

    private fun calculateQualityScore(
        latencyMs: Long,
        bandwidthKbps: Double,
    ): Int {
        val latencyScore =
            when {
                latencyMs <= PERFECT_LATENCY_MS -> 5
                latencyMs <= EXCELLENT_LATENCY_MS -> 4
                latencyMs <= GOOD_LATENCY_MS -> 3
                latencyMs <= FAIR_LATENCY_MS -> 2
                else -> 1
            }

        val bandwidthScore =
            when {
                bandwidthKbps >= PERFECT_BANDWIDTH_KBPS -> 5
                bandwidthKbps >= EXCELLENT_BANDWIDTH_KBPS -> 4
                bandwidthKbps >= GOOD_BANDWIDTH_KBPS -> 3
                bandwidthKbps >= FAIR_BANDWIDTH_KBPS -> 2
                else -> 1
            }

        return min(latencyScore, bandwidthScore)
    }

    private fun addLatencySample(latency: Long) {
        latencyHistory.add(latency)
        if (latencyHistory.size > BANDWIDTH_WINDOW_SIZE) {
            latencyHistory.removeAt(0)
        }
    }

    private fun addBandwidthSample(bandwidth: Double) {
        bandwidthHistory.add(bandwidth)
        if (bandwidthHistory.size > BANDWIDTH_WINDOW_SIZE) {
            bandwidthHistory.removeAt(0)
        }
    }

    private fun updateNetworkQuality(newQuality: NetworkQuality) {
        val qualityChanged = newQuality.score != currentQuality.score
        currentQuality = newQuality

        if (qualityChanged) {
            logger.info(
                "[DEBUG_LOG] Network quality changed to score ${newQuality.score} (${
                    getQualityDescription(
                        newQuality.score
                    )
                })"
            )
            listeners.forEach { listener ->
                try {
                    listener.onNetworkQualityChanged(newQuality)
                } catch (e: Exception) {
                    logger.error("Error notifying network quality listener", e)
                }
            }
        }
    }

    private fun getQualityDescription(score: Int): String =
        when (score) {
            5 -> "Perfect"
            4 -> "Excellent"
            3 -> "Good"
            2 -> "Fair"
            1 -> "Poor"
            else -> "Unknown"
        }

    fun getNetworkStatistics(): String =
        buildString {
            appendLine("Network Quality Statistics:")
            appendLine("  Current Score: ${currentQuality.score} (${getQualityDescription(currentQuality.score)})")
            appendLine("  Latency: ${currentQuality.latencyMs}ms")
            appendLine("  Bandwidth: ${String.format("%.1f", currentQuality.bandwidthKbps)}Kbps")
            appendLine("  Server: $serverHost:$serverPort")
            appendLine("  Monitoring: $isMonitoring")
            appendLine("  Latency History: ${latencyHistory.size} samples")
            appendLine("  Bandwidth History: ${bandwidthHistory.size} samples")
        }
}
