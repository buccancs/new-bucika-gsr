package com.topdon.tc001.gsr.quality

import com.elvishew.xlog.XLog
import com.shimmerresearch.driver.ProcessedGSRData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.sqrt

class GSRDataQualityMonitor {
    
    companion object {
        private const val TAG = "GSRQualityMonitor"
        
        private const val MIN_ACCEPTABLE_QUALITY = 70.0
        private const val MAX_GSR_CHANGE_RATE = 5.0
        private const val MAX_TEMP_CHANGE_RATE = 1.0
        private const val MIN_SIGNAL_RANGE = 0.1
        private const val MAX_DROPOUT_DURATION = 1000L
        
        private const val QUALITY_WINDOW_SIZE = 128 * 5
        private const val SPIKE_DETECTION_WINDOW = 10
    }
    
    private val _qualityMetrics = MutableStateFlow(QualityMetrics())
    val qualityMetrics: StateFlow<QualityMetrics> = _qualityMetrics.asStateFlow()
    
    private val recentSamples = mutableListOf<ProcessedGSRData>()
    private val gsrValues = mutableListOf<Double>()
    private val temperatureValues = mutableListOf<Double>()
    private val qualityHistory = mutableListOf<Double>()
    
    private val totalSamples = AtomicLong(0)
    private val validSamples = AtomicLong(0)
    private val artifactCount = AtomicLong(0)
    private val dropoutCount = AtomicLong(0)
    
    private var lastSampleTime = 0L
    private var lastSequenceNumber = 0L
    private var inDropout = false
    private var dropoutStartTime = 0L
    
    fun processDataSample(data: ProcessedGSRData): ValidationResult {
        val sampleCount = totalSamples.incrementAndGet()
        
        recentSamples.add(data)
        if (recentSamples.size > QUALITY_WINDOW_SIZE) {
            recentSamples.removeAt(0)
        }
        
        val validationResult = validateSample(data)
        
        updateQualityMetrics(data, validationResult)
        
        updateDataBuffers(data)
        
        checkForDropouts(data)
        
        if (sampleCount % 32 == 0L) {
            computeAndEmitQualityMetrics()
        }
        
        return validationResult
    }
    
    private fun validateSample(data: ProcessedGSRData): ValidationResult {
        val issues = mutableListOf<QualityIssue>()
        var overallQuality = 100.0
        
        if (data.filteredGSR < 0.1 || data.filteredGSR > 50.0) {
            issues.add(QualityIssue.OUT_OF_RANGE_GSR)
            overallQuality -= 20.0
        }
        
        if (data.filteredTemperature < 20.0 || data.filteredTemperature > 45.0) {
            issues.add(QualityIssue.OUT_OF_RANGE_TEMPERATURE)
            overallQuality -= 15.0
        }
        
        if (lastSampleTime > 0L) {
            val timeDelta = data.timestamp - lastSampleTime
            val expectedDelta = 1000L / 128L
            
            if (abs(timeDelta - expectedDelta) > expectedDelta * 0.5) {
                issues.add(QualityIssue.TIMING_INCONSISTENCY)
                overallQuality -= 10.0
            }
        }
        
        if (lastSequenceNumber > 0L && data.sampleIndex != lastSequenceNumber + 1) {
            val missedSamples = data.sampleIndex - lastSequenceNumber - 1
            if (missedSamples > 0) {
                issues.add(QualityIssue.MISSING_SAMPLES)
                overallQuality -= minOf(missedSamples * 5.0, 30.0)
                XLog.w(TAG, "Missing $missedSamples samples between ${lastSequenceNumber} and ${data.sampleIndex}")
            }
        }
        
        if (recentSamples.size >= SPIKE_DETECTION_WINDOW) {
            val recent = recentSamples.takeLast(SPIKE_DETECTION_WINDOW)
            if (detectSpike(recent, data)) {
                issues.add(QualityIssue.GSR_SPIKE_DETECTED)
                overallQuality -= 15.0
                artifactCount.incrementAndGet()
            }
        }
        
        if (data.filteredGSR > 48.0) {
            issues.add(QualityIssue.SIGNAL_SATURATION)
            overallQuality -= 25.0
        }
        
        if (recentSamples.size >= 64) {
            val recent64 = recentSamples.takeLast(64)
            val gsrRange = recent64.maxOf { it.filteredGSR } - recent64.minOf { it.filteredGSR }
            if (gsrRange < MIN_SIGNAL_RANGE) {
                issues.add(QualityIssue.SIGNAL_FLATLINE)
                overallQuality -= 20.0
            }
        }
        
        if (recentSamples.isNotEmpty()) {
            val lastSample = recentSamples.last()
            val gsrChangeRate = abs(data.filteredGSR - lastSample.filteredGSR)
            val tempChangeRate = abs(data.filteredTemperature - lastSample.filteredTemperature)
            
            if (gsrChangeRate > MAX_GSR_CHANGE_RATE) {
                issues.add(QualityIssue.EXCESSIVE_GSR_CHANGE)
                overallQuality -= 10.0
            }
            
            if (tempChangeRate > MAX_TEMP_CHANGE_RATE) {
                issues.add(QualityIssue.EXCESSIVE_TEMP_CHANGE)
                overallQuality -= 8.0
            }
        }
        
        lastSampleTime = data.timestamp
        lastSequenceNumber = data.sampleIndex
        
        overallQuality = overallQuality.coerceIn(0.0, 100.0)
        
        if (overallQuality >= MIN_ACCEPTABLE_QUALITY) {
            validSamples.incrementAndGet()
        }
        
        return ValidationResult(
            isValid = overallQuality >= MIN_ACCEPTABLE_QUALITY,
            qualityScore = overallQuality,
            issues = issues,
            timestamp = data.timestamp,
            sampleIndex = data.sampleIndex
        )
    }
    
    private fun detectSpike(recentSamples: List<ProcessedGSRData>, currentSample: ProcessedGSRData): Boolean {
        if (recentSamples.isEmpty()) return false
        
        val recentValues = recentSamples.map { it.filteredGSR }
        val mean = recentValues.average()
        val variance = recentValues.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)
        
        val deviation = abs(currentSample.filteredGSR - mean)
        return deviation > 3 * stdDev && stdDev > 0.1
    }
    
    private fun checkForDropouts(data: ProcessedGSRData) {
        if (lastSampleTime > 0L) {
            val timeSinceLastSample = data.timestamp - lastSampleTime
            val expectedInterval = 1000L / 128L
            
            if (timeSinceLastSample > expectedInterval * 3) {
                if (!inDropout) {
                    inDropout = true
                    dropoutStartTime = lastSampleTime
                    dropoutCount.incrementAndGet()
                    XLog.w(TAG, "Dropout detected: ${timeSinceLastSample}ms gap")
                }
            } else {
                if (inDropout) {
                    val dropoutDuration = data.timestamp - dropoutStartTime
                    inDropout = false
                    XLog.i(TAG, "Dropout recovered after ${dropoutDuration}ms")
                }
            }
        }
    }
    
    private fun updateDataBuffers(data: ProcessedGSRData) {
        gsrValues.add(data.filteredGSR)
        temperatureValues.add(data.filteredTemperature)
        
        if (gsrValues.size > QUALITY_WINDOW_SIZE) {
            gsrValues.removeAt(0)
            temperatureValues.removeAt(0)
        }
    }
    
    private fun updateQualityMetrics(data: ProcessedGSRData, validation: ValidationResult) {
        qualityHistory.add(validation.qualityScore)
        if (qualityHistory.size > QUALITY_WINDOW_SIZE) {
            qualityHistory.removeAt(0)
        }
    }
    
    private fun computeAndEmitQualityMetrics() {
        val total = totalSamples.get()
        val valid = validSamples.get()
        val artifacts = artifactCount.get()
        val dropouts = dropoutCount.get()
        
        if (total == 0L) return
        
        val currentQuality = if (qualityHistory.isNotEmpty()) qualityHistory.average() else 0.0
        val qualityStdDev = if (qualityHistory.size > 1) {
            val mean = qualityHistory.average()
            sqrt(qualityHistory.map { (it - mean) * (it - mean) }.average())
        } else 0.0
        
        val gsrStats = if (gsrValues.isNotEmpty()) {
            SignalStatistics(
                mean = gsrValues.average(),
                min = gsrValues.minOrNull() ?: 0.0,
                max = gsrValues.maxOrNull() ?: 0.0,
                stdDev = if (gsrValues.size > 1) {
                    val mean = gsrValues.average()
                    sqrt(gsrValues.map { (it - mean) * (it - mean) }.average())
                } else 0.0
            )
        } else SignalStatistics()
        
        val tempStats = if (temperatureValues.isNotEmpty()) {
            SignalStatistics(
                mean = temperatureValues.average(),
                min = temperatureValues.minOrNull() ?: 0.0,
                max = temperatureValues.maxOrNull() ?: 0.0,
                stdDev = if (temperatureValues.size > 1) {
                    val mean = temperatureValues.average()
                    sqrt(temperatureValues.map { (it - mean) * (it - mean) }.average())
                } else 0.0
            )
        } else SignalStatistics()
        
        _qualityMetrics.value = QualityMetrics(
            totalSamples = total,
            validSamples = valid,
            validDataRatio = valid.toDouble() / total.toDouble(),
            currentQuality = currentQuality,
            qualityStability = 100.0 - (qualityStdDev * 2).coerceAtMost(100.0),
            artifactCount = artifacts,
            dropoutCount = dropouts,
            gsrStatistics = gsrStats,
            temperatureStatistics = tempStats,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    fun getCurrentQualityAssessment(): QualityAssessment {
        val metrics = _qualityMetrics.value
        
        val overallGrade = when {
            metrics.currentQuality >= 90.0 -> QualityGrade.EXCELLENT
            metrics.currentQuality >= 80.0 -> QualityGrade.GOOD
            metrics.currentQuality >= 70.0 -> QualityGrade.ACCEPTABLE
            metrics.currentQuality >= 50.0 -> QualityGrade.POOR
            else -> QualityGrade.UNACCEPTABLE
        }
        
        val recommendations = generateRecommendations(metrics)
        
        return QualityAssessment(
            grade = overallGrade,
            qualityScore = metrics.currentQuality,
            validDataRatio = metrics.validDataRatio,
            recommendations = recommendations,
            detailedMetrics = metrics
        )
    }
    
    private fun generateRecommendations(metrics: QualityMetrics): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (metrics.validDataRatio < 0.8) {
            recommendations.add("Data quality is poor (${(metrics.validDataRatio * 100).toInt()}% valid). Check sensor connection.")
        }
        
        if (metrics.artifactCount > metrics.totalSamples * 0.1) {
            recommendations.add("High artifact rate detected. Ensure proper sensor placement and subject stillness.")
        }
        
        if (metrics.dropoutCount > 0) {
            recommendations.add("${metrics.dropoutCount} data dropouts detected. Check Bluetooth connection stability.")
        }
        
        if (metrics.gsrStatistics.stdDev > 5.0) {
            recommendations.add("High GSR variability. Consider longer stabilization period before recording.")
        }
        
        if (metrics.qualityStability < 70.0) {
            recommendations.add("Quality inconsistent. Check for environmental interference or sensor drift.")
        }
        
        return recommendations
    }
    
    fun reset() {
        totalSamples.set(0)
        validSamples.set(0)
        artifactCount.set(0)
        dropoutCount.set(0)
        
        recentSamples.clear()
        gsrValues.clear()
        temperatureValues.clear()
        qualityHistory.clear()
        
        lastSampleTime = 0L
        lastSequenceNumber = 0L
        inDropout = false
        dropoutStartTime = 0L
        
        _qualityMetrics.value = QualityMetrics()
        
        XLog.i(TAG, "Quality monitor reset")
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val qualityScore: Double,
    val issues: List<QualityIssue>,
    val timestamp: Long,
    val sampleIndex: Long
)

data class QualityMetrics(
    val totalSamples: Long = 0,
    val validSamples: Long = 0,
    val validDataRatio: Double = 0.0,
    val currentQuality: Double = 0.0,
    val qualityStability: Double = 100.0,
    val artifactCount: Long = 0,
    val dropoutCount: Long = 0,
    val gsrStatistics: SignalStatistics = SignalStatistics(),
    val temperatureStatistics: SignalStatistics = SignalStatistics(),
    val lastUpdateTime: Long = 0
)

data class SignalStatistics(
    val mean: Double = 0.0,
    val min: Double = 0.0,
    val max: Double = 0.0,
    val stdDev: Double = 0.0
)

data class QualityAssessment(
    val grade: QualityGrade,
    val qualityScore: Double,
    val validDataRatio: Double,
    val recommendations: List<String>,
    val detailedMetrics: QualityMetrics
)

enum class QualityGrade {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    POOR,
    UNACCEPTABLE
}

enum class QualityIssue {
    OUT_OF_RANGE_GSR,
    OUT_OF_RANGE_TEMPERATURE,
    TIMING_INCONSISTENCY,
    MISSING_SAMPLES,
    GSR_SPIKE_DETECTED,
    SIGNAL_SATURATION,
    SIGNAL_FLATLINE,
    EXCESSIVE_GSR_CHANGE,
    EXCESSIVE_TEMP_CHANGE
