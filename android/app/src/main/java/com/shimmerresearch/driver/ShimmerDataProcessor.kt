package com.shimmerresearch.driver

import com.elvishew.xlog.XLog
import kotlin.math.*

class ShimmerDataProcessor {
    
    companion object {
        private const val TAG = "ShimmerDataProcessor"
        
        private const val LOW_PASS_CUTOFF_HZ = 5.0
        private const val HIGH_PASS_CUTOFF_HZ = 0.05
        private const val NOTCH_FILTER_FREQ_HZ = 50.0
        
        private const val GSR_MIN_VALID_VALUE = 0.1
        private const val GSR_MAX_VALID_VALUE = 100.0
        private const val TEMP_MIN_VALID_VALUE = 20.0
        private const val TEMP_MAX_VALID_VALUE = 45.0
        
        private const val GSR_MAX_CHANGE_RATE = 5.0
        private const val TEMP_MAX_CHANGE_RATE = 2.0
    }
    
    private val gsrBuffer = mutableListOf<Double>()
    private val temperatureBuffer = mutableListOf<Double>()
    private val timestampBuffer = mutableListOf<Long>()
    
    private val maxBufferSize = 1280
    
    private var previousGSR = 0.0
    private var previousTemp = 0.0
    private var lastValidGSR = 5.0
    private var lastValidTemp = 32.5
    
    private var totalSamples = 0L
    private var validSamples = 0L
    private var artifactSamples = 0L
    
    fun processGSRData(objectCluster: ObjectCluster, samplingRate: Double): ProcessedGSRData {
        try {
            val timestamp = System.currentTimeMillis()
            
            val rawGSR = objectCluster.getValue(
                Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE,
                Configuration.CALIBRATED
            ) ?: return createInvalidData(timestamp)
            
            val rawTemp = objectCluster.getValue(
                Configuration.Shimmer3.ObjectClusterSensorName.SKIN_TEMPERATURE,
                Configuration.CALIBRATED
            ) ?: return createInvalidData(timestamp)
            
            totalSamples++
            
            val isValidGSR = isValidGSRValue(rawGSR)
            val isValidTemp = isValidTemperatureValue(rawTemp)
            
            val gsrArtifact = detectGSRArtifact(rawGSR, samplingRate)
            val tempArtifact = detectTemperatureArtifact(rawTemp, samplingRate)
            
            val hasArtifact = gsrArtifact || tempArtifact
            if (hasArtifact) {
                artifactSamples++
            }
            
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
            
            updateBuffers(processedGSR, processedTemp, timestamp)
            
            val filteredGSR = applyGSRFiltering(processedGSR, samplingRate)
            val filteredTemp = applyTemperatureFiltering(processedTemp, samplingRate)
            
            if (isValidGSR && isValidTemp && !hasArtifact) {
                validSamples++
            }
            
            val gsrDerivative = calculateGSRDerivative(samplingRate)
            val gsrVariability = calculateGSRVariability()
            val tempVariability = calculateTemperatureVariability()
            
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
    
    private fun isValidGSRValue(gsrValue: Double): Boolean {
        return gsrValue in GSR_MIN_VALID_VALUE..GSR_MAX_VALID_VALUE && gsrValue.isFinite()
    }
    
    private fun isValidTemperatureValue(tempValue: Double): Boolean {
        return tempValue in TEMP_MIN_VALID_VALUE..TEMP_MAX_VALID_VALUE && tempValue.isFinite()
    }
    
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
    
    private fun updateBuffers(gsr: Double, temp: Double, timestamp: Long) {
        gsrBuffer.add(gsr)
        temperatureBuffer.add(temp)
        timestampBuffer.add(timestamp)
        
        if (gsrBuffer.size > maxBufferSize) {
            gsrBuffer.removeAt(0)
            temperatureBuffer.removeAt(0)
            timestampBuffer.removeAt(0)
        }
    }
    
    private fun applyGSRFiltering(gsr: Double, samplingRate: Double): Double {

        val alpha = 2.0 * PI * LOW_PASS_CUTOFF_HZ / samplingRate
        val filterConstant = alpha / (alpha + 1.0)
        
        return if (gsrBuffer.size > 1) {
            val previousFiltered = gsrBuffer[gsrBuffer.size - 2]
            filterConstant * gsr + (1.0 - filterConstant) * previousFiltered
        } else {
            gsr
        }
    }
    
    private fun applyTemperatureFiltering(temp: Double, samplingRate: Double): Double {

        val alpha = 2.0 * PI * 0.1 / samplingRate
        val filterConstant = alpha / (alpha + 1.0)
        
        return if (temperatureBuffer.size > 1) {
            val previousFiltered = temperatureBuffer[temperatureBuffer.size - 2]
            filterConstant * temp + (1.0 - filterConstant) * previousFiltered
        } else {
            temp
        }
    }
    
    private fun calculateGSRDerivative(samplingRate: Double): Double {
        return if (gsrBuffer.size >= 2) {
            val current = gsrBuffer[gsrBuffer.size - 1]
            val previous = gsrBuffer[gsrBuffer.size - 2]
            (current - previous) * samplingRate
        } else {
            0.0
        }
    }
    
    private fun calculateGSRVariability(): Double {
        val windowSize = min(gsrBuffer.size, 128)
        if (windowSize < 2) return 0.0
        
        val recentData = gsrBuffer.takeLast(windowSize)
        val mean = recentData.average()
        val variance = recentData.map { (it - mean).pow(2) }.average()
        
        return sqrt(variance)
    }
    
    private fun calculateTemperatureVariability(): Double {
        val windowSize = min(temperatureBuffer.size, 128)
        if (windowSize < 2) return 0.0
        
        val recentData = temperatureBuffer.takeLast(windowSize)
        val mean = recentData.average()
        val variance = recentData.map { (it - mean).pow(2) }.average()
        
        return sqrt(variance)
    }
    
    private fun calculateSignalQuality(): Double {
        if (totalSamples == 0L) return 0.0
        
        val validRatio = validSamples.toDouble() / totalSamples
        val artifactRatio = artifactSamples.toDouble() / totalSamples
        
        val qualityScore = (validRatio * 100.0) - (artifactRatio * 50.0)
        
        return max(0.0, min(100.0, qualityScore))
    }
    
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

data class ProcessingStatistics(
    val totalSamples: Long,
    val validSamples: Long,
    val artifactSamples: Long,
    val validDataRatio: Double,
    val artifactRatio: Double,
    val currentSignalQuality: Double,
    val bufferSize: Int
