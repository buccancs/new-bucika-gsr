package com.shimmerresearch.driver

import com.elvishew.xlog.XLog
import kotlin.math.*

/**
 * Shimmer Data Processor for advanced GSR signal processing
 * Implements signal processing algorithms from the official Shimmer SDK
 */
class ShimmerDataProcessor {
    
    companion object {
        private const val TAG = "ShimmerDataProcessor"
        
        // Digital filter constants for GSR signal processing
        private const val LOW_PASS_CUTOFF_HZ = 5.0
        private const val HIGH_PASS_CUTOFF_HZ = 0.05
        private const val NOTCH_FILTER_FREQ_HZ = 50.0 // Power line frequency
        
        // Signal quality thresholds
        private const val GSR_MIN_VALID_VALUE = 0.1 // µS
        private const val GSR_MAX_VALID_VALUE = 100.0 // µS
        private const val TEMP_MIN_VALID_VALUE = 20.0 // °C
        private const val TEMP_MAX_VALID_VALUE = 45.0 // °C
        
        // Artifact detection parameters
        private const val GSR_MAX_CHANGE_RATE = 5.0 // µS/s
        private const val TEMP_MAX_CHANGE_RATE = 2.0 // °C/s
    }
    
    private val gsrBuffer = mutableListOf<Double>()
    private val temperatureBuffer = mutableListOf<Double>()
    private val timestampBuffer = mutableListOf<Long>()
    
    private val maxBufferSize = 1280 // 10 seconds at 128 Hz
    
    // Filter states
    private var previousGSR = 0.0
    private var previousTemp = 0.0
    private var lastValidGSR = 5.0
    private var lastValidTemp = 32.5
    
    // Statistics
    private var totalSamples = 0L
    private var validSamples = 0L
    private var artifactSamples = 0L
    
    /**
     * Process raw GSR and temperature data from ObjectCluster
     */
    fun processGSRData(objectCluster: ObjectCluster, samplingRate: Double): ProcessedGSRData {
        try {
            val timestamp = System.currentTimeMillis()
            
            // Extract raw values
            val rawGSR = objectCluster.getValue(
                Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE,
                Configuration.CALIBRATED
            ) ?: return createInvalidData(timestamp)
            
            val rawTemp = objectCluster.getValue(
                Configuration.Shimmer3.ObjectClusterSensorName.SKIN_TEMPERATURE,
                Configuration.CALIBRATED
            ) ?: return createInvalidData(timestamp)
            
            totalSamples++
            
            // Validate data ranges
            val isValidGSR = isValidGSRValue(rawGSR)
            val isValidTemp = isValidTemperatureValue(rawTemp)
            
            // Detect artifacts
            val gsrArtifact = detectGSRArtifact(rawGSR, samplingRate)
            val tempArtifact = detectTemperatureArtifact(rawTemp, samplingRate)
            
            val hasArtifact = gsrArtifact || tempArtifact
            if (hasArtifact) {
                artifactSamples++
            }
            
            // Use last valid values if current ones are invalid
            val processedGSR = if (isValidGSR && !gsrArtifact) {
                lastValidGSR = rawGSR
                rawGSR
            } else {
                lastValidGSR
            }
            
            val processedTemp = if (isValidTemp && !tempArtifact) {
                lastValidTemp = rawTemp
                rawTemp
            } else {
                lastValidTemp
            }
            
            // Update buffers
            updateBuffers(processedGSR, processedTemp, timestamp)
            
            // Apply digital filtering
            val filteredGSR = applyGSRFiltering(processedGSR, samplingRate)
            val filteredTemp = applyTemperatureFiltering(processedTemp, samplingRate)
            
            if (isValidGSR && isValidTemp && !hasArtifact) {
                validSamples++
            }
            
            // Calculate derived metrics
            val gsrDerivative = calculateGSRDerivative(samplingRate)
            val gsrVariability = calculateGSRVariability()
            val tempVariability = calculateTemperatureVariability()
            
            // Calculate signal quality metrics
            val signalQuality = calculateSignalQuality()
            
            return ProcessedGSRData(
                timestamp = timestamp,
                rawGSR = rawGSR,
                filteredGSR = filteredGSR,
                rawTemperature = rawTemp,
                filteredTemperature = filteredTemp,
                gsrDerivative = gsrDerivative,
                gsrVariability = gsrVariability,
                temperatureVariability = tempVariability,
                signalQuality = signalQuality,
                hasArtifact = hasArtifact,
                isValid = isValidGSR && isValidTemp && !hasArtifact,
                sampleIndex = totalSamples
            )
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error processing GSR data: ${e.message}", e)
            return createInvalidData(System.currentTimeMillis())
        }
    }
    
    /**
     * Check if GSR value is within valid physiological range
     */
    private fun isValidGSRValue(gsrValue: Double): Boolean {
        return gsrValue in GSR_MIN_VALID_VALUE..GSR_MAX_VALID_VALUE && gsrValue.isFinite()
    }
    
    /**
     * Check if temperature value is within valid physiological range
     */
    private fun isValidTemperatureValue(tempValue: Double): Boolean {
        return tempValue in TEMP_MIN_VALID_VALUE..TEMP_MAX_VALID_VALUE && tempValue.isFinite()
    }
    
    /**
     * Detect GSR artifacts based on rapid changes
     */
    private fun detectGSRArtifact(currentGSR: Double, samplingRate: Double): Boolean {
        if (previousGSR == 0.0) {
            previousGSR = currentGSR
            return false
        }
        
        val changeRate = abs(currentGSR - previousGSR) * samplingRate
        val isArtifact = changeRate > GSR_MAX_CHANGE_RATE
        
        previousGSR = currentGSR
        return isArtifact
    }
    
    /**
     * Detect temperature artifacts based on rapid changes
     */
    private fun detectTemperatureArtifact(currentTemp: Double, samplingRate: Double): Boolean {
        if (previousTemp == 0.0) {
            previousTemp = currentTemp
            return false
        }
        
        val changeRate = abs(currentTemp - previousTemp) * samplingRate
        val isArtifact = changeRate > TEMP_MAX_CHANGE_RATE
        
        previousTemp = currentTemp
        return isArtifact
    }
    
    /**
     * Update circular buffers with new data
     */
    private fun updateBuffers(gsr: Double, temp: Double, timestamp: Long) {
        gsrBuffer.add(gsr)
        temperatureBuffer.add(temp)
        timestampBuffer.add(timestamp)
        
        // Maintain buffer size
        if (gsrBuffer.size > maxBufferSize) {
            gsrBuffer.removeAt(0)
            temperatureBuffer.removeAt(0)
            timestampBuffer.removeAt(0)
        }
    }
    
    /**
     * Apply low-pass filtering to GSR signal
     */
    private fun applyGSRFiltering(gsr: Double, samplingRate: Double): Double {
        // Simple low-pass filter implementation
        val alpha = 2.0 * PI * LOW_PASS_CUTOFF_HZ / samplingRate
        val filterConstant = alpha / (alpha + 1.0)
        
        return if (gsrBuffer.size > 1) {
            val previousFiltered = gsrBuffer[gsrBuffer.size - 2]
            filterConstant * gsr + (1.0 - filterConstant) * previousFiltered
        } else {
            gsr
        }
    }
    
    /**
     * Apply filtering to temperature signal
     */
    private fun applyTemperatureFiltering(temp: Double, samplingRate: Double): Double {
        // Temperature changes slowly, use stronger filtering
        val alpha = 2.0 * PI * 0.1 / samplingRate // 0.1 Hz cutoff
        val filterConstant = alpha / (alpha + 1.0)
        
        return if (temperatureBuffer.size > 1) {
            val previousFiltered = temperatureBuffer[temperatureBuffer.size - 2]
            filterConstant * temp + (1.0 - filterConstant) * previousFiltered
        } else {
            temp
        }
    }
    
    /**
     * Calculate GSR derivative (rate of change)
     */
    private fun calculateGSRDerivative(samplingRate: Double): Double {
        return if (gsrBuffer.size >= 2) {
            val current = gsrBuffer[gsrBuffer.size - 1]
            val previous = gsrBuffer[gsrBuffer.size - 2]
            (current - previous) * samplingRate
        } else {
            0.0
        }
    }
    
    /**
     * Calculate GSR variability (standard deviation over recent window)
     */
    private fun calculateGSRVariability(): Double {
        val windowSize = min(gsrBuffer.size, 128) // 1 second window at 128 Hz
        if (windowSize < 2) return 0.0
        
        val recentData = gsrBuffer.takeLast(windowSize)
        val mean = recentData.average()
        val variance = recentData.map { (it - mean).pow(2) }.average()
        
        return sqrt(variance)
    }
    
    /**
     * Calculate temperature variability
     */
    private fun calculateTemperatureVariability(): Double {
        val windowSize = min(temperatureBuffer.size, 128) // 1 second window
        if (windowSize < 2) return 0.0
        
        val recentData = temperatureBuffer.takeLast(windowSize)
        val mean = recentData.average()
        val variance = recentData.map { (it - mean).pow(2) }.average()
        
        return sqrt(variance)
    }
    
    /**
     * Calculate overall signal quality score (0-100)
     */
    private fun calculateSignalQuality(): Double {
        if (totalSamples == 0L) return 0.0
        
        val validRatio = validSamples.toDouble() / totalSamples
        val artifactRatio = artifactSamples.toDouble() / totalSamples
        
        // Quality score based on valid data ratio and artifact ratio
        val qualityScore = (validRatio * 100.0) - (artifactRatio * 50.0)
        
        return max(0.0, min(100.0, qualityScore))
    }
    
    /**
     * Create invalid data response
     */
    private fun createInvalidData(timestamp: Long): ProcessedGSRData {
        return ProcessedGSRData(
            timestamp = timestamp,
            rawGSR = 0.0,
            filteredGSR = 0.0,
            rawTemperature = 0.0,
            filteredTemperature = 0.0,
            gsrDerivative = 0.0,
            gsrVariability = 0.0,
            temperatureVariability = 0.0,
            signalQuality = 0.0,
            hasArtifact = true,
            isValid = false,
            sampleIndex = totalSamples
        )
    }
    
    /**
     * Get processing statistics
     */
    fun getStatistics(): ProcessingStatistics {
        val validRatio = if (totalSamples > 0) validSamples.toDouble() / totalSamples else 0.0
        val artifactRatio = if (totalSamples > 0) artifactSamples.toDouble() / totalSamples else 0.0
        
        return ProcessingStatistics(
            totalSamples = totalSamples,
            validSamples = validSamples,
            artifactSamples = artifactSamples,
            validDataRatio = validRatio,
            artifactRatio = artifactRatio,
            currentSignalQuality = calculateSignalQuality(),
            bufferSize = gsrBuffer.size
        )
    }
    
    /**
     * Reset processor state
     */
    fun reset() {
        gsrBuffer.clear()
        temperatureBuffer.clear()
        timestampBuffer.clear()
        previousGSR = 0.0
        previousTemp = 0.0
        lastValidGSR = 5.0
        lastValidTemp = 32.5
        totalSamples = 0L
        validSamples = 0L
        artifactSamples = 0L
    }
}

/**
 * Processed GSR data with derived metrics
 */
data class ProcessedGSRData(
    val timestamp: Long,
    val rawGSR: Double,
    val filteredGSR: Double,
    val rawTemperature: Double,
    val filteredTemperature: Double,
    val gsrDerivative: Double,
    val gsrVariability: Double,
    val temperatureVariability: Double,
    val signalQuality: Double,
    val hasArtifact: Boolean,
    val isValid: Boolean,
    val sampleIndex: Long
)

/**
 * Processing statistics
 */
data class ProcessingStatistics(
    val totalSamples: Long,
    val validSamples: Long,
    val artifactSamples: Long,
    val validDataRatio: Double,
    val artifactRatio: Double,
    val currentSignalQuality: Double,
    val bufferSize: Int
