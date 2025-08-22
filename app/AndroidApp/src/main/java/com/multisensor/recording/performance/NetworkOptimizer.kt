package com.multisensor.recording.performance

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var monitoringJob: Job? = null
    private var listeners = mutableListOf<NetworkPerformanceListener>()

    private var currentBandwidth: Long = 0L
    private var currentLatency: Long = 0L
    private var compressionEnabled: Boolean = false
    private var batchingEnabled: Boolean = false
    private var adaptiveQualityEnabled: Boolean = true

    interface NetworkPerformanceListener {
        fun onBandwidthChanged(bandwidth: Long)
        fun onLatencyChanged(latency: Long)
        fun onCompressionStateChanged(enabled: Boolean)
        fun onBatchingStateChanged(enabled: Boolean)
    }

    fun startOptimization() {
        logger.info("NetworkOptimizer: Starting network optimisation")

        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    measureNetworkPerformance()

                    optimizeNetworkSettings()

                    delay(5000)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SecurityException) {
                    logger.error("NetworkOptimizer: Security error during monitoring cycle", e)
                    delay(10000)
                } catch (e: IllegalStateException) {
                    logger.error("NetworkOptimizer: State error during monitoring cycle", e)
                    delay(10000)
                }
            }
        }
    }

    fun stopOptimization() {
        logger.info("NetworkOptimizer: Stopping network optimisation")
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun addListener(listener: NetworkPerformanceListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NetworkPerformanceListener) {
        listeners.remove(listener)
    }

    private suspend fun measureNetworkPerformance() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

            if (networkCapabilities != null) {
                val estimatedBandwidth = estimateBandwidth(networkCapabilities)

                val estimatedLatency = estimateLatency(networkCapabilities)

                if (currentBandwidth != estimatedBandwidth) {
                    currentBandwidth = estimatedBandwidth
                    notifyBandwidthChange()
                }

                if (currentLatency != estimatedLatency) {
                    currentLatency = estimatedLatency
                    notifyLatencyChange()
                }

                logger.debug("NetworkOptimizer: Bandwidth: ${currentBandwidth}bps, Latency: ${currentLatency}ms")
            }
        } catch (e: SecurityException) {
            logger.error("NetworkOptimizer: Security error accessing network capabilities", e)
        } catch (e: IllegalStateException) {
            logger.error("NetworkOptimizer: State error measuring network performance", e)
        }
    }

    private fun estimateBandwidth(capabilities: NetworkCapabilities): Long {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                if (capabilities.linkDownstreamBandwidthKbps > 0) {
                    capabilities.linkDownstreamBandwidthKbps * 1000L
                } else {
                    50_000_000L
                }
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                when {
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> 20_000_000L
                    else -> 5_000_000L
                }
            }

            else -> 1_000_000L
        }
    }

    private fun estimateLatency(capabilities: NetworkCapabilities): Long {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 20L
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 100L
            else -> 200L
        }
    }

    private fun optimizeNetworkSettings() {
        val shouldCompress = currentBandwidth < 5_000_000L
        if (compressionEnabled != shouldCompress) {
            compressionEnabled = shouldCompress
            notifyCompressionStateChange()
            logger.info("NetworkOptimizer: Compression ${if (compressionEnabled) "enabled" else "disabled"}")
        }

        val shouldBatch = currentLatency > 100L
        if (batchingEnabled != shouldBatch) {
            batchingEnabled = shouldBatch
            notifyBatchingStateChange()
            logger.info("NetworkOptimizer: Message batching ${if (batchingEnabled) "enabled" else "disabled"}")
        }
    }

    fun getOptimizationSettings(): NetworkOptimizationSettings {
        return NetworkOptimizationSettings(
            bandwidth = currentBandwidth,
            latency = currentLatency,
            compressionEnabled = compressionEnabled,
            batchingEnabled = batchingEnabled,
            adaptiveQualityEnabled = adaptiveQualityEnabled
        )
    }

    fun setAdaptiveQualityEnabled(enabled: Boolean) {
        adaptiveQualityEnabled = enabled
        logger.info("NetworkOptimizer: Adaptive quality ${if (enabled) "enabled" else "disabled"}")
    }

    fun getRecommendedFrameRate(): Float {
        return when {
            currentBandwidth > 20_000_000L -> 5.0f
            currentBandwidth > 10_000_000L -> 3.0f
            currentBandwidth > 5_000_000L -> 2.0f
            else -> 1.0f
        }
    }

    fun getRecommendedCompressionQuality(): Int {
        return when {
            currentBandwidth > 20_000_000L -> 85
            currentBandwidth > 10_000_000L -> 70
            currentBandwidth > 5_000_000L -> 55
            else -> 40
        }
    }

    private fun notifyBandwidthChange() {
        listeners.forEach { it.onBandwidthChanged(currentBandwidth) }
    }

    private fun notifyLatencyChange() {
        listeners.forEach { it.onLatencyChanged(currentLatency) }
    }

    private fun notifyCompressionStateChange() {
        listeners.forEach { it.onCompressionStateChanged(compressionEnabled) }
    }

    private fun notifyBatchingStateChange() {
        listeners.forEach { it.onBatchingStateChanged(batchingEnabled) }
    }
}

data class NetworkOptimizationSettings(
    val bandwidth: Long,
    val latency: Long,
    val compressionEnabled: Boolean,
    val batchingEnabled: Boolean,
    val adaptiveQualityEnabled: Boolean
)
