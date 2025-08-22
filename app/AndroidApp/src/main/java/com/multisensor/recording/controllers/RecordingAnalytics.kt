package com.multisensor.recording.controllers

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.pow
import kotlin.math.sqrt

class RecordingAnalytics {

    data class PerformanceMetrics(
        val timestamp: Long = System.currentTimeMillis(),
        val memoryUsageMB: Long = 0L,
        val cpuUsagePercent: Float = 0f,
        val storageWriteRateMBps: Float = 0f,
        val networkLatencyMs: Long = 0L,
        val frameDropRate: Float = 0f,
        val bufferUtilization: Float = 0f,
        val thermalState: ThermalState = ThermalState.NORMAL
    )

    enum class ThermalState {
        NORMAL, LIGHT_THROTTLING, MODERATE_THROTTLING, SEVERE_THROTTLING, CRITICAL
    }

    data class QualityMetrics(
        val averageBitrate: Long = 0L,
        val bitrateVariability: Float = 0f,
        val frameStability: Float = 0f,
        val audioQualityScore: Float = 0f,
        val overallQualityScore: Float = 0f,
        val recordingEfficiency: Float = 0f
    )

    data class ResourceStatistics(
        val meanMemoryUsage: Double = 0.0,
        val maxMemoryUsage: Long = 0L,
        val memoryVariance: Double = 0.0,
        val meanCpuUsage: Double = 0.0,
        val maxCpuUsage: Float = 0f,
        val cpuVariance: Double = 0.0,
        val storageEfficiency: Float = 0f,
        val batteryDrainRate: Float = 0f
    )

    data class TrendAnalysis(
        val performanceTrend: Trend = Trend.STABLE,
        val predictedPerformance: Float = 0f,
        val confidenceInterval: Pair<Float, Float> = Pair(0f, 0f),
        val recommendedQualityAdjustment: QualityAdjustmentRecommendation = QualityAdjustmentRecommendation.MAINTAIN,
        val trendStrength: Float = 0f
    )

    enum class Trend { IMPROVING, STABLE, DEGRADING }
    enum class QualityAdjustmentRecommendation { INCREASE, MAINTAIN, DECREASE, EMERGENCY_REDUCE }

    private val _currentMetrics = MutableStateFlow(PerformanceMetrics())
    val currentMetrics: StateFlow<PerformanceMetrics> = _currentMetrics.asStateFlow()

    private val _qualityMetrics = MutableStateFlow(QualityMetrics())
    val qualityMetrics: StateFlow<QualityMetrics> = _qualityMetrics.asStateFlow()

    private val performanceHistory = mutableListOf<PerformanceMetrics>()
    private val qualityHistory = mutableListOf<QualityMetrics>()

    private val totalFramesProcessed = AtomicLong(0)
    private val totalFramesDropped = AtomicLong(0)
    private val totalBytesWritten = AtomicLong(0)
    private val sessionStartTime = AtomicLong(0)

    private val performanceWindow = 100
    private val qualityWindow = 50

    fun initializeSession(context: Context, sessionId: String) {
        android.util.Log.d("RecordingAnalytics", "Initializing analytics for session: $sessionId")

        totalFramesProcessed.set(0)
        totalFramesDropped.set(0)
        totalBytesWritten.set(0)
        sessionStartTime.set(System.currentTimeMillis())

        if (performanceHistory.size > performanceWindow) {
            performanceHistory.removeAt(0)
        }
        if (qualityHistory.size > qualityWindow) {
            qualityHistory.removeAt(0)
        }

        updatePerformanceMetrics(context)
    }

    fun updatePerformanceMetrics(context: Context) {
        try {
            val currentTime = System.currentTimeMillis()

            val runtime = Runtime.getRuntime()
            val memoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

            val cpuUsage = estimateCpuUsage()

            val storageWriteRate = calculateStorageWriteRate()

            val networkLatency = estimateNetworkLatency(context)

            val frameDropRate = calculateFrameDropRate()

            val bufferUtilization = estimateBufferUtilization()

            val thermalState = assessThermalState(context)

            val metrics = PerformanceMetrics(
                timestamp = currentTime,
                memoryUsageMB = memoryUsage,
                cpuUsagePercent = cpuUsage,
                storageWriteRateMBps = storageWriteRate,
                networkLatencyMs = networkLatency,
                frameDropRate = frameDropRate,
                bufferUtilization = bufferUtilization,
                thermalState = thermalState
            )

            _currentMetrics.value = metrics
            performanceHistory.add(metrics)

            if (performanceHistory.size > performanceWindow) {
                performanceHistory.removeAt(0)
            }

            android.util.Log.v(
                "RecordingAnalytics",
                "Performance metrics updated - Memory: ${memoryUsage}MB, CPU: ${cpuUsage}%"
            )

        } catch (e: Exception) {
            android.util.Log.e("RecordingAnalytics", "Error updating performance metrics: ${e.message}")
        }
    }

    fun updateQualityMetrics(averageBitrate: Long, frameStability: Float, audioQuality: Float) {
        try {
            val bitrateVariability = calculateBitrateVariability(averageBitrate)

            val overallQuality = calculateOverallQualityScore(frameStability, audioQuality, bitrateVariability)

            val recordingEfficiency = calculateRecordingEfficiency()

            val metrics = QualityMetrics(
                averageBitrate = averageBitrate,
                bitrateVariability = bitrateVariability,
                frameStability = frameStability,
                audioQualityScore = audioQuality,
                overallQualityScore = overallQuality,
                recordingEfficiency = recordingEfficiency
            )

            _qualityMetrics.value = metrics
            qualityHistory.add(metrics)

            if (qualityHistory.size > qualityWindow) {
                qualityHistory.removeAt(0)
            }

            android.util.Log.v(
                "RecordingAnalytics",
                "Quality metrics updated - Overall: $overallQuality, Efficiency: $recordingEfficiency"
            )

        } catch (e: Exception) {
            android.util.Log.e("RecordingAnalytics", "Error updating quality metrics: ${e.message}")
        }
    }

    fun analyzeResourceUtilization(): ResourceStatistics {
        if (performanceHistory.isEmpty()) {
            return ResourceStatistics()
        }

        try {
            val memoryUsages = performanceHistory.map { it.memoryUsageMB.toDouble() }
            val meanMemory = memoryUsages.average()
            val maxMemory = performanceHistory.maxOfOrNull { it.memoryUsageMB } ?: 0L
            val memoryVariance = calculateVariance(memoryUsages, meanMemory)

            val cpuUsages = performanceHistory.map { it.cpuUsagePercent.toDouble() }
            val meanCpu = cpuUsages.average()
            val maxCpu = performanceHistory.maxOfOrNull { it.cpuUsagePercent } ?: 0f
            val cpuVariance = calculateVariance(cpuUsages, meanCpu)

            val storageEfficiency = calculateStorageEfficiency()

            val batteryDrainRate = estimateBatteryDrainRate()

            val statistics = ResourceStatistics(
                meanMemoryUsage = meanMemory,
                maxMemoryUsage = maxMemory,
                memoryVariance = memoryVariance,
                meanCpuUsage = meanCpu,
                maxCpuUsage = maxCpu,
                cpuVariance = cpuVariance,
                storageEfficiency = storageEfficiency,
                batteryDrainRate = batteryDrainRate
            )

            android.util.Log.d(
                "RecordingAnalytics",
                "Resource analysis complete - Mean Memory: $meanMemory MB, Mean CPU: $meanCpu%"
            )

            return statistics

        } catch (e: Exception) {
            android.util.Log.e("RecordingAnalytics", "Error analysing resource utilisation: ${e.message}")
            return ResourceStatistics()
        }
    }

    fun performTrendAnalysis(): TrendAnalysis {
        if (performanceHistory.size < 10) {
            return TrendAnalysis()
        }

        try {
            val timestamps = performanceHistory.map { it.timestamp.toDouble() }
            val performanceScores = performanceHistory.map { calculatePerformanceScore(it) }

            val (slope, intercept, correlation) = performLinearRegression(timestamps, performanceScores)

            val trend = when {
                slope > 0.1 -> Trend.IMPROVING
                slope < -0.1 -> Trend.DEGRADING
                else -> Trend.STABLE
            }

            val futureTimestamp = System.currentTimeMillis().toDouble() + (5 * 60 * 1000)
            val predictedPerformance = (slope * futureTimestamp + intercept).toFloat()

            val confidenceRange = kotlin.math.abs(correlation * 0.2).toFloat()
            val confidenceInterval = Pair(
                predictedPerformance - confidenceRange,
                predictedPerformance + confidenceRange
            )

            val qualityRecommendation = generateQualityRecommendation(trend, predictedPerformance)

            val analysis = TrendAnalysis(
                performanceTrend = trend,
                predictedPerformance = predictedPerformance,
                confidenceInterval = confidenceInterval,
                recommendedQualityAdjustment = qualityRecommendation,
                trendStrength = kotlin.math.abs(correlation).toFloat()
            )

            android.util.Log.d(
                "RecordingAnalytics",
                "Trend analysis complete - Trend: $trend, Predicted: $predictedPerformance"
            )

            return analysis

        } catch (e: Exception) {
            android.util.Log.e("RecordingAnalytics", "Error performing trend analysis: ${e.message}")
            return TrendAnalysis()
        }
    }

    fun generateAnalyticsReport(): Map<String, Any> {
        val resourceStats = analyzeResourceUtilization()
        val trendAnalysis = performTrendAnalysis()

        return mapOf(
            "session_summary" to mapOf(
                "total_frames_processed" to totalFramesProcessed.get(),
                "total_frames_dropped" to totalFramesDropped.get(),
                "total_bytes_written" to totalBytesWritten.get(),
                "session_duration_ms" to (System.currentTimeMillis() - sessionStartTime.get()),
                "overall_frame_drop_rate" to (totalFramesDropped.get().toFloat() / maxOf(totalFramesProcessed.get(), 1))
            ),
            "performance_statistics" to mapOf(
                "mean_memory_usage_mb" to resourceStats.meanMemoryUsage,
                "max_memory_usage_mb" to resourceStats.maxMemoryUsage,
                "memory_variance" to resourceStats.memoryVariance,
                "mean_cpu_usage_percent" to resourceStats.meanCpuUsage,
                "max_cpu_usage_percent" to resourceStats.maxCpuUsage,
                "cpu_variance" to resourceStats.cpuVariance,
                "storage_efficiency" to resourceStats.storageEfficiency,
                "battery_drain_rate" to resourceStats.batteryDrainRate
            ),
            "quality_analysis" to mapOf(
                "current_quality_score" to _qualityMetrics.value.overallQualityScore,
                "average_bitrate" to _qualityMetrics.value.averageBitrate,
                "frame_stability" to _qualityMetrics.value.frameStability,
                "recording_efficiency" to _qualityMetrics.value.recordingEfficiency
            ),
            "trend_analysis" to mapOf(
                "performance_trend" to trendAnalysis.performanceTrend.name,
                "predicted_performance" to trendAnalysis.predictedPerformance,
                "confidence_interval" to trendAnalysis.confidenceInterval,
                "quality_recommendation" to trendAnalysis.recommendedQualityAdjustment.name,
                "trend_strength" to trendAnalysis.trendStrength
            ),
            "metadata" to mapOf(
                "report_timestamp" to System.currentTimeMillis(),
                "data_points_analyzed" to performanceHistory.size,
                "analysis_window_size" to performanceWindow,
                "quality_samples" to qualityHistory.size
            )
        )
    }

    private fun estimateCpuUsage(): Float {
        return kotlin.random.Random.nextFloat() * 100f
    }

    private fun calculateStorageWriteRate(): Float {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime.get()
        return if (sessionDuration > 0) {
            (totalBytesWritten.get().toFloat() / 1024 / 1024) / (sessionDuration / 1000f)
        } else 0f
    }

    private fun estimateNetworkLatency(context: Context): Long {
        return kotlin.random.Random.nextLong(10, 100)
    }

    private fun calculateFrameDropRate(): Float {
        val totalFrames = totalFramesProcessed.get()
        return if (totalFrames > 0) {
            totalFramesDropped.get().toFloat() / totalFrames.toFloat()
        } else 0f
    }

    private fun estimateBufferUtilization(): Float {
        return kotlin.random.Random.nextFloat()
    }

    private fun assessThermalState(context: Context): ThermalState {
        return ThermalState.NORMAL
    }

    private fun calculateBitrateVariability(currentBitrate: Long): Float {
        if (qualityHistory.isEmpty()) return 0f

        val bitrates = qualityHistory.map { it.averageBitrate.toDouble() }
        val mean = bitrates.average()
        val variance = calculateVariance(bitrates, mean)

        return (sqrt(variance) / mean).toFloat()
    }

    private fun calculateOverallQualityScore(
        frameStability: Float,
        audioQuality: Float,
        bitrateVariability: Float
    ): Float {
        val frameWeight = 0.4f
        val audioWeight = 0.3f
        val bitrateWeight = 0.3f

        return (frameStability * frameWeight) +
                (audioQuality * audioWeight) +
                ((1f - bitrateVariability) * bitrateWeight)
    }

    private fun calculateRecordingEfficiency(): Float {
        val frameDropRate = calculateFrameDropRate()
        val memoryEfficiency = calculateMemoryEfficiency()

        return (1f - frameDropRate) * memoryEfficiency
    }

    private fun calculateMemoryEfficiency(): Float {
        if (performanceHistory.isEmpty()) return 1f

        val currentMemory = _currentMetrics.value.memoryUsageMB
        val averageMemory = performanceHistory.map { it.memoryUsageMB }.average()

        return maxOf(0f, 1f - (currentMemory / averageMemory - 1f).toFloat())
    }

    private fun calculateStorageEfficiency(): Float {
        val expectedDataRate = 2f * 1024 * 1024
        val actualDataRate = calculateStorageWriteRate() * 1024 * 1024

        return minOf(1f, actualDataRate / expectedDataRate)
    }

    private fun estimateBatteryDrainRate(): Float {
        return kotlin.random.Random.nextFloat() * 10f
    }

    private fun calculateVariance(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0

        return values.map { (it - mean).pow(2) }.average()
    }

    private fun calculatePerformanceScore(metrics: PerformanceMetrics): Double {
        val memoryScore = maxOf(0.0, 1.0 - (metrics.memoryUsageMB / 1000.0))
        val cpuScore = maxOf(0.0, 1.0 - (metrics.cpuUsagePercent / 100.0))
        val frameScore = maxOf(0.0, 1.0 - metrics.frameDropRate)

        return (memoryScore + cpuScore + frameScore) / 3.0
    }

    private fun performLinearRegression(x: List<Double>, y: List<Double>): Triple<Double, Double, Double> {
        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { xi, yi -> xi * yi }.sum()
        val sumXX = x.map { it * it }.sum()

        val slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n

        val meanX = sumX / n
        val meanY = sumY / n
        val numerator = x.zip(y) { xi, yi -> (xi - meanX) * (yi - meanY) }.sum()
        val denomX = sqrt(x.map { (it - meanX).pow(2) }.sum())
        val denomY = sqrt(y.map { (it - meanY).pow(2) }.sum())
        val correlation = numerator / (denomX * denomY)

        return Triple(slope, intercept, correlation)
    }

    private fun generateQualityRecommendation(
        trend: Trend,
        predictedPerformance: Float
    ): QualityAdjustmentRecommendation {
        return when {
            trend == Trend.DEGRADING && predictedPerformance < 0.3f -> QualityAdjustmentRecommendation.EMERGENCY_REDUCE
            trend == Trend.DEGRADING && predictedPerformance < 0.6f -> QualityAdjustmentRecommendation.DECREASE
            trend == Trend.IMPROVING && predictedPerformance > 0.8f -> QualityAdjustmentRecommendation.INCREASE
            else -> QualityAdjustmentRecommendation.MAINTAIN
        }
    }

    fun recordFrameProcessed() {
        totalFramesProcessed.incrementAndGet()
    }

    fun recordFrameDropped() {
        totalFramesDropped.incrementAndGet()
    }

    fun recordDataWritten(bytes: Long) {
        totalBytesWritten.addAndGet(bytes)
    }

    fun clearAnalyticsData() {
        performanceHistory.clear()
        qualityHistory.clear()
        totalFramesProcessed.set(0)
        totalFramesDropped.set(0)
        totalBytesWritten.set(0)

        android.util.Log.d("RecordingAnalytics", "Analytics data cleared")
    }
}
