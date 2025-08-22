package com.multisensor.recording.recording

import com.multisensor.recording.network.NetworkQualityMonitor
import com.multisensor.recording.util.Logger
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AdaptiveFrameRateController
@Inject
constructor(
    private val networkQualityMonitor: NetworkQualityMonitor,
    private val logger: Logger,
) : NetworkQualityMonitor.NetworkQualityListener {
    companion object {
        private const val PERFECT_FRAME_RATE = 5.0f
        private const val EXCELLENT_FRAME_RATE = 3.0f
        private const val GOOD_FRAME_RATE = 2.0f
        private const val FAIR_FRAME_RATE = 1.0f
        private const val POOR_FRAME_RATE = 0.5f

        private const val HYSTERESIS_THRESHOLD = 1
        private const val ADAPTATION_DELAY_MS = 3000L
        private const val STABILITY_WINDOW_SIZE = 3

        private const val DEFAULT_FRAME_RATE = GOOD_FRAME_RATE
        private const val MIN_FRAME_RATE = 0.1f
        private const val MAX_FRAME_RATE = 10.0f
    }

    data class FrameRateSettings(
        val currentFrameRate: Float,
        val targetFrameRate: Float,
        val networkQuality: Int,
        val isAdaptive: Boolean,
        val lastAdaptationTime: Long,
        val adaptationCount: Long,
    )

    interface FrameRateChangeListener {
        fun onFrameRateChanged(
            newFrameRate: Float,
            reason: String,
        )

        fun onAdaptationModeChanged(isAdaptive: Boolean)
    }

    private var currentFrameRate = DEFAULT_FRAME_RATE
    private var targetFrameRate = DEFAULT_FRAME_RATE
    private var isAdaptiveMode = true
    private var manualFrameRate = DEFAULT_FRAME_RATE

    private var lastAdaptationTime = 0L
    private var lastQualityScore = 3
    private var adaptationCount = 0L
    private val qualityHistory = mutableListOf<Int>()

    private val listeners = mutableSetOf<FrameRateChangeListener>()

    private var adaptationJob: Job? = null
    private var isActive = false

    fun start() {
        if (isActive) {
            logger.info("[DEBUG_LOG] AdaptiveFrameRateController already active")
            return
        }

        isActive = true
        logger.info("[DEBUG_LOG] Starting AdaptiveFrameRateController - Initial frame rate: ${currentFrameRate}fps")

        networkQualityMonitor.addListener(this)

        adaptationJob =
            CoroutineScope(Dispatchers.Main).launch {
                monitorAdaptation()
            }
    }

    fun stop() {
        logger.info("[DEBUG_LOG] Stopping AdaptiveFrameRateController")
        isActive = false

        networkQualityMonitor.removeListener(this)

        adaptationJob?.cancel()
        adaptationJob = null
    }

    fun addListener(listener: FrameRateChangeListener) {
        listeners.add(listener)
        listener.onFrameRateChanged(currentFrameRate, "Initial state")
        listener.onAdaptationModeChanged(isAdaptiveMode)
    }

    fun removeListener(listener: FrameRateChangeListener) {
        listeners.remove(listener)
    }

    fun setManualFrameRate(frameRate: Float) {
        val clampedFrameRate = frameRate.coerceIn(MIN_FRAME_RATE, MAX_FRAME_RATE)
        manualFrameRate = clampedFrameRate

        if (isAdaptiveMode) {
            isAdaptiveMode = false
            notifyAdaptationModeChanged()
        }

        updateFrameRate(clampedFrameRate, "Manual override to ${clampedFrameRate}fps")
        logger.info("[DEBUG_LOG] Manual frame rate set to ${clampedFrameRate}fps")
    }

    fun enableAdaptiveMode() {
        if (!isAdaptiveMode) {
            isAdaptiveMode = true
            notifyAdaptationModeChanged()

            val currentQuality = networkQualityMonitor.getCurrentQuality()
            onNetworkQualityChanged(currentQuality)

            logger.info("[DEBUG_LOG] Adaptive mode enabled - Current quality: ${currentQuality.score}")
        }
    }

    fun getCurrentSettings(): FrameRateSettings =
        FrameRateSettings(
            currentFrameRate = currentFrameRate,
            targetFrameRate = targetFrameRate,
            networkQuality = lastQualityScore,
            isAdaptive = isAdaptiveMode,
            lastAdaptationTime = lastAdaptationTime,
            adaptationCount = adaptationCount,
        )

    fun getCurrentFrameRate(): Float = currentFrameRate

    fun isAdaptiveModeEnabled(): Boolean = isAdaptiveMode

    override fun onNetworkQualityChanged(quality: NetworkQualityMonitor.NetworkQuality) {
        if (!isAdaptiveMode || !isActive) {
            return
        }

        addQualityToHistory(quality.score)

        if (shouldAdaptFrameRate(quality.score)) {
            val newFrameRate = calculateOptimalFrameRate(quality.score)
            val reason = "Network quality ${quality.score} (${getQualityDescription(quality.score)})"

            updateFrameRate(newFrameRate, reason)
            lastAdaptationTime = System.currentTimeMillis()
            adaptationCount++

            logger.info("[DEBUG_LOG] Frame rate adapted to ${newFrameRate}fps due to $reason")
        }

        lastQualityScore = quality.score
    }

    private fun shouldAdaptFrameRate(newQualityScore: Int): Boolean {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastAdaptationTime < ADAPTATION_DELAY_MS) {
            return false
        }

        val qualityDifference = abs(newQualityScore - lastQualityScore)
        if (qualityDifference < HYSTERESIS_THRESHOLD) {
            return false
        }

        if (qualityHistory.size >= STABILITY_WINDOW_SIZE) {
            val recentQualities = qualityHistory.takeLast(STABILITY_WINDOW_SIZE)
            val isStable = recentQualities.all { abs(it - newQualityScore) <= 1 }
            if (!isStable) {
                return false
            }
        }

        return true
    }

    private fun calculateOptimalFrameRate(qualityScore: Int): Float =
        when (qualityScore) {
            5 -> PERFECT_FRAME_RATE
            4 -> EXCELLENT_FRAME_RATE
            3 -> GOOD_FRAME_RATE
            2 -> FAIR_FRAME_RATE
            1 -> POOR_FRAME_RATE
            else -> DEFAULT_FRAME_RATE
        }

    private fun updateFrameRate(
        newFrameRate: Float,
        reason: String,
    ) {
        if (newFrameRate != currentFrameRate) {
            val previousFrameRate = currentFrameRate
            currentFrameRate = newFrameRate
            targetFrameRate = newFrameRate

            logger.info("[DEBUG_LOG] Frame rate changed from ${previousFrameRate}fps to ${newFrameRate}fps - $reason")

            listeners.forEach { listener ->
                try {
                    listener.onFrameRateChanged(newFrameRate, reason)
                } catch (e: Exception) {
                    logger.error("Error notifying frame rate change listener", e)
                }
            }
        }
    }

    private fun notifyAdaptationModeChanged() {
        listeners.forEach { listener ->
            try {
                listener.onAdaptationModeChanged(isAdaptiveMode)
            } catch (e: Exception) {
                logger.error("Error notifying adaptation mode change listener", e)
            }
        }
    }

    private fun addQualityToHistory(qualityScore: Int) {
        qualityHistory.add(qualityScore)
        if (qualityHistory.size > STABILITY_WINDOW_SIZE * 2) {
            qualityHistory.removeAt(0)
        }
    }

    private suspend fun monitorAdaptation() {
        while (isActive) {
            try {
                delay(30000)

                if (isAdaptiveMode) {
                    logger.debug("[DEBUG_LOG] Adaptation statistics: ${getAdaptationStatistics()}")
                }
            } catch (e: Exception) {
                logger.error("Error in adaptation monitoring", e)
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

    fun getAdaptationStatistics(): String =
        buildString {
            appendLine("Adaptive Frame Rate Statistics:")
            appendLine("  Current Frame Rate: ${currentFrameRate}fps")
            appendLine("  Target Frame Rate: ${targetFrameRate}fps")
            appendLine("  Adaptive Mode: $isAdaptiveMode")
            appendLine("  Manual Frame Rate: ${manualFrameRate}fps")
            appendLine("  Network Quality: $lastQualityScore (${getQualityDescription(lastQualityScore)})")
            appendLine("  Total Adaptations: $adaptationCount")
            appendLine(
                "  Last Adaptation: ${if (lastAdaptationTime > 0) "${System.currentTimeMillis() - lastAdaptationTime}ms ago" else "Never"}",
            )
            appendLine("  Quality History: ${qualityHistory.takeLast(5)}")
            appendLine("  Active Listeners: ${listeners.size}")
        }

    fun resetStatistics() {
        adaptationCount = 0
        lastAdaptationTime = 0
        qualityHistory.clear()
        logger.info("[DEBUG_LOG] Adaptation statistics reset")
    }
}
