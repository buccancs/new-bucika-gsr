package com.multisensor.recording.controllers

import android.content.Context
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

class UsbPerformanceAnalytics {

    companion object {
        private const val PERFORMANCE_WINDOW_SIZE = 1000
        private const val QUALITY_SAMPLE_WINDOW = 50
        private const val CONNECTION_STABILITY_THRESHOLD = 0.95
        private const val RESPONSE_TIME_THRESHOLD_MS = 10L
        private const val CPU_EFFICIENCY_THRESHOLD = 0.95

        private const val STABILITY_WEIGHT = 0.4
        private const val RESPONSE_TIME_WEIGHT = 0.3
        private const val THROUGHPUT_WEIGHT = 0.3
    }

    enum class PerformanceEventType {
        DEVICE_ATTACHMENT,
        DEVICE_DETACHMENT,
        STATE_PERSISTENCE,
        DEVICE_SCAN,
        STATUS_UPDATE,
        CALLBACK_NOTIFICATION,
        QUALITY_ASSESSMENT
    }

    data class ConnectionQualityMetrics(
        val stabilityScore: Double,
        val averageResponseTime: Double,
        val throughputScore: Double,
        val overallQuality: Double,
        val recommendedAction: QualityAction
    )

    enum class QualityAction {
        OPTIMAL,
        MONITOR,
        OPTIMIZE_SCANNING,
        CHECK_CONNECTIONS,
        RESTART_REQUIRED
    }

    data class PerformanceReport(
        val totalEvents: Long,
        val averageResponseTime: Double,
        val percentile95ResponseTime: Long,
        val percentile99ResponseTime: Long,
        val cpuEfficiencyScore: Double,
        val memoryUtilization: Long,
        val eventThroughput: Double,
        val qualityMetrics: Map<String, ConnectionQualityMetrics>,
        val systemRecommendations: List<String>
    )

    private val eventMetrics = ConcurrentHashMap<PerformanceEventType, MutableList<Long>>()
    private val deviceQualityHistory = ConcurrentHashMap<String, MutableList<QualityDataPoint>>()
    private val systemMetrics = ConcurrentHashMap<String, Double>()

    private data class QualityDataPoint(
        val timestamp: Long,
        val responseTime: Long,
        val successful: Boolean,
        val cpuUsage: Double
    )

    init {
        PerformanceEventType.values().forEach { eventType ->
            eventMetrics[eventType] = mutableListOf()
        }

        systemMetrics["cpu_baseline"] = 0.0
        systemMetrics["memory_baseline"] = 0.0
        systemMetrics["event_rate_baseline"] = 0.0
    }

    fun recordEvent(eventType: PerformanceEventType, duration: Long, deviceKey: String? = null) {
        val timestamp = SystemClock.elapsedRealtime()

        eventMetrics[eventType]?.add(duration)

        eventMetrics[eventType]?.let { list ->
            if (list.size > PERFORMANCE_WINDOW_SIZE) {
                list.removeAt(0)
            }
        }

        deviceKey?.let { key ->
            val qualityPoint = QualityDataPoint(
                timestamp = timestamp,
                responseTime = duration,
                successful = duration < RESPONSE_TIME_THRESHOLD_MS,
                cpuUsage = getCurrentCpuUsage()
            )

            deviceQualityHistory.computeIfAbsent(key) { mutableListOf() }.add(qualityPoint)

            deviceQualityHistory[key]?.let { history ->
                if (history.size > QUALITY_SAMPLE_WINDOW) {
                    history.removeAt(0)
                }
            }
        }

        updateSystemMetrics(eventType, duration)
    }

    fun calculateConnectionQuality(deviceKey: String): ConnectionQualityMetrics {
        val qualityHistory = deviceQualityHistory[deviceKey] ?: return getDefaultQualityMetrics()

        if (qualityHistory.isEmpty()) {
            return getDefaultQualityMetrics()
        }

        val successRate = qualityHistory.count { it.successful }.toDouble() / qualityHistory.size
        val stabilityScore = minOf(successRate / CONNECTION_STABILITY_THRESHOLD, 1.0)

        val responseTimes = qualityHistory.map { it.responseTime }.sorted()
        val averageResponseTime = calculateTrimmedMean(responseTimes, 0.1)

        val timeSpan = qualityHistory.last().timestamp - qualityHistory.first().timestamp
        val eventRate = if (timeSpan > 0) {
            (qualityHistory.size.toDouble() / timeSpan) * 1000
        } else 0.0

        val throughputScore = minOf(eventRate / 10.0, 1.0)

        val overallQuality = (
                stabilityScore * STABILITY_WEIGHT +
                        (1.0 - minOf(averageResponseTime / RESPONSE_TIME_THRESHOLD_MS, 1.0)) * RESPONSE_TIME_WEIGHT +
                        throughputScore * THROUGHPUT_WEIGHT
                )

        val recommendedAction = when {
            overallQuality >= 0.9 -> QualityAction.OPTIMAL
            overallQuality >= 0.75 -> QualityAction.MONITOR
            stabilityScore < 0.8 -> QualityAction.CHECK_CONNECTIONS
            averageResponseTime > RESPONSE_TIME_THRESHOLD_MS * 2 -> QualityAction.OPTIMIZE_SCANNING
            else -> QualityAction.RESTART_REQUIRED
        }

        return ConnectionQualityMetrics(
            stabilityScore = stabilityScore,
            averageResponseTime = averageResponseTime,
            throughputScore = throughputScore,
            overallQuality = overallQuality,
            recommendedAction = recommendedAction
        )
    }

    fun generatePerformanceReport(context: Context): PerformanceReport {
        val allEventTimes = eventMetrics.values.flatten()

        if (allEventTimes.isEmpty()) {
            return getDefaultPerformanceReport()
        }

        val sortedTimes = allEventTimes.sorted()

        val averageResponseTime = allEventTimes.average()
        val percentile95 = calculatePercentile(sortedTimes, 0.95)
        val percentile99 = calculatePercentile(sortedTimes, 0.99)

        val cpuEfficiency = calculateCpuEfficiency()
        val memoryUtilization = calculateMemoryUtilization(context)
        val eventThroughput = calculateEventThroughput()

        val qualityMetrics = deviceQualityHistory.keys.associateWith { deviceKey ->
            calculateConnectionQuality(deviceKey)
        }

        val recommendations = generateSystemRecommendations(qualityMetrics, cpuEfficiency)

        return PerformanceReport(
            totalEvents = allEventTimes.size.toLong(),
            averageResponseTime = averageResponseTime,
            percentile95ResponseTime = percentile95,
            percentile99ResponseTime = percentile99,
            cpuEfficiencyScore = cpuEfficiency,
            memoryUtilization = memoryUtilization,
            eventThroughput = eventThroughput,
            qualityMetrics = qualityMetrics,
            systemRecommendations = recommendations
        )
    }

    fun monitorConnectionQuality(deviceKey: String): String {
        val quality = calculateConnectionQuality(deviceKey)

        return buildString {
            append("Connection Quality Analysis for Device: $deviceKey\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("Overall Quality Score: ${"%.3f".format(quality.overallQuality)} ")
            append(getQualityEmoji(quality.overallQuality))
            append("\n\n")

            append("Detailed Metrics:\n")
            append("├─ Stability Score: ${"%.3f".format(quality.stabilityScore)} ")
            append("(${(quality.stabilityScore * 100).toInt()}% reliable)\n")
            append("├─ Response Time: ${"%.2f".format(quality.averageResponseTime)}ms ")
            append("(target: <${RESPONSE_TIME_THRESHOLD_MS}ms)\n")
            append("├─ Throughput Score: ${"%.3f".format(quality.throughputScore)} ")
            append("(${(quality.throughputScore * 100).toInt()}% of optimal)\n")
            append("└─ Recommended Action: ${quality.recommendedAction.name}\n\n")

            append("Quality Interpretation:\n")
            when {
                quality.overallQuality >= 0.9 -> append("🟢 EXCELLENT - System performing optimally")
                quality.overallQuality >= 0.75 -> append("🟡 GOOD - Minor optimisation possible")
                quality.overallQuality >= 0.5 -> append("🟠 FAIR - Performance issues detected")
                else -> append("🔴 POOR - Immediate attention required")
            }
            append("\n")

            if (quality.recommendedAction != QualityAction.OPTIMAL) {
                append("\nRecommended Actions:\n")
                append(getActionRecommendations(quality.recommendedAction))
            }
        }
    }

    fun resetAnalytics() {
        eventMetrics.values.forEach { it.clear() }
        deviceQualityHistory.clear()
        systemMetrics.clear()
    }

    fun getResourceUtilization(): Map<String, Double> {
        return mapOf(
            "cpu_usage" to getCurrentCpuUsage(),
            "memory_usage" to getCurrentMemoryUsage(),
            "event_rate" to getCurrentEventRate(),
            "efficiency_score" to calculateCpuEfficiency()
        )
    }

    private fun calculateTrimmedMean(values: List<Long>, trimRatio: Double): Double {
        if (values.isEmpty()) return 0.0

        val trimSize = (values.size * trimRatio).toInt()
        val trimmedValues = values.drop(trimSize).dropLast(trimSize)

        return if (trimmedValues.isNotEmpty()) {
            trimmedValues.average()
        } else {
            values.average()
        }
    }

    private fun calculatePercentile(sortedValues: List<Long>, percentile: Double): Long {
        if (sortedValues.isEmpty()) return 0L

        val index = (percentile * (sortedValues.size - 1)).toInt()
        return sortedValues[index]
    }

    private fun getCurrentCpuUsage(): Double {
        return systemMetrics["cpu_baseline"] ?: 0.0
    }

    private fun getCurrentMemoryUsage(): Double {
        return Runtime.getRuntime().let { runtime ->
            val used = runtime.totalMemory() - runtime.freeMemory()
            used.toDouble() / runtime.maxMemory()
        }
    }

    private fun getCurrentEventRate(): Double {
        val recentEvents = eventMetrics.values.flatten().filter {
            SystemClock.elapsedRealtime() - it < 10000
        }
        return recentEvents.size / 10.0
    }

    private fun calculateCpuEfficiency(): Double {
        val currentUsage = getCurrentCpuUsage()
        val baseline = systemMetrics["cpu_baseline"] ?: 0.0
        return if (baseline > 0) {
            1.0 - ((currentUsage - baseline) / baseline)
        } else CPU_EFFICIENCY_THRESHOLD
    }

    private fun calculateMemoryUtilization(context: Context): Long {
        return Runtime.getRuntime().let { runtime ->
            runtime.totalMemory() - runtime.freeMemory()
        }
    }

    private fun calculateEventThroughput(): Double {
        val totalEvents = eventMetrics.values.sumOf { it.size }
        val timeSpan = 60000
        return totalEvents.toDouble() / (timeSpan / 1000.0)
    }

    private fun updateSystemMetrics(eventType: PerformanceEventType, duration: Long) {
        val currentRate = systemMetrics["event_rate_baseline"] ?: 0.0
        systemMetrics["event_rate_baseline"] = currentRate * 0.95 + (1000.0 / duration) * 0.05
    }

    private fun generateSystemRecommendations(
        qualityMetrics: Map<String, ConnectionQualityMetrics>,
        cpuEfficiency: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (cpuEfficiency < CPU_EFFICIENCY_THRESHOLD) {
            recommendations.add("Consider reducing scanning frequency to improve CPU efficiency")
        }

        val poorQualityDevices = qualityMetrics.filter { it.value.overallQuality < 0.5 }
        if (poorQualityDevices.isNotEmpty()) {
            recommendations.add("${poorQualityDevices.size} device(s) showing poor connection quality")
        }

        val avgResponseTime = qualityMetrics.values.map { it.averageResponseTime }.average()
        if (avgResponseTime > RESPONSE_TIME_THRESHOLD_MS * 2) {
            recommendations.add("High response times detected - consider system optimisation")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("System performance is optimal")
        }

        return recommendations
    }

    private fun getQualityEmoji(quality: Double): String = when {
        quality >= 0.9 -> "🟢"
        quality >= 0.75 -> "🟡"
        quality >= 0.5 -> "🟠"
        else -> "🔴"
    }

    private fun getActionRecommendations(action: QualityAction): String = when (action) {
        QualityAction.MONITOR -> "• Increase monitoring frequency\n• Watch for performance trends"
        QualityAction.OPTIMIZE_SCANNING -> "• Reduce scanning interval\n• Optimise resource usage"
        QualityAction.CHECK_CONNECTIONS -> "• Verify physical USB connections\n• Check cable integrity"
        QualityAction.RESTART_REQUIRED -> "• Restart application\n• Consider device reconnection"
        else -> "• System is operating optimally"
    }

    private fun getDefaultQualityMetrics(): ConnectionQualityMetrics {
        return ConnectionQualityMetrics(
            stabilityScore = 0.0,
            averageResponseTime = 0.0,
            throughputScore = 0.0,
            overallQuality = 0.0,
            recommendedAction = QualityAction.MONITOR
        )
    }

    private fun getDefaultPerformanceReport(): PerformanceReport {
        return PerformanceReport(
            totalEvents = 0L,
            averageResponseTime = 0.0,
            percentile95ResponseTime = 0L,
            percentile99ResponseTime = 0L,
            cpuEfficiencyScore = 1.0,
            memoryUtilization = 0L,
            eventThroughput = 0.0,
            qualityMetrics = emptyMap(),
            systemRecommendations = listOf("No performance data available yet")
        )
    }
}
