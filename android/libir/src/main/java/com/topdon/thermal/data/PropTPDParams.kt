package com.topdon.thermal.data

/**
 * Thermal processing and detection parameters for advanced thermal analysis
 */
data class PropTPDParams(
    var threshold: Float = 0.5f,
    var minSize: Int = 10,
    var maxSize: Int = 1000,
    var sensitivity: Float = 0.8f,
    var analysisMode: Int = 0,
    var processingLevel: Int = 1,
    var targetType: Int = 0,
    var detectionRange: IntRange = 0..255,
    var processingFlags: Int = 0,
    var algorithmType: String = "default",
    
    // Temperature analysis parameters
    var tempThresholdHigh: Float = 100.0f,
    var tempThresholdLow: Float = 0.0f,
    var tempAnalysisEnabled: Boolean = true,
    
    // Spatial analysis parameters
    var regionOfInterest: IntArray = intArrayOf(0, 0, 256, 192),
    var spatialFilterEnabled: Boolean = false,
    var temporalFilterEnabled: Boolean = false,
    
    // Processing optimization
    var useGPU: Boolean = false,
    var threadCount: Int = 4,
    var batchProcessing: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PropTPDParams

        if (threshold != other.threshold) return false
        if (minSize != other.minSize) return false
        if (maxSize != other.maxSize) return false
        if (sensitivity != other.sensitivity) return false
        if (analysisMode != other.analysisMode) return false
        if (processingLevel != other.processingLevel) return false
        if (targetType != other.targetType) return false
        if (detectionRange != other.detectionRange) return false
        if (processingFlags != other.processingFlags) return false
        if (algorithmType != other.algorithmType) return false
        if (tempThresholdHigh != other.tempThresholdHigh) return false
        if (tempThresholdLow != other.tempThresholdLow) return false
        if (tempAnalysisEnabled != other.tempAnalysisEnabled) return false
        if (!regionOfInterest.contentEquals(other.regionOfInterest)) return false
        if (spatialFilterEnabled != other.spatialFilterEnabled) return false
        if (temporalFilterEnabled != other.temporalFilterEnabled) return false
        if (useGPU != other.useGPU) return false
        if (threadCount != other.threadCount) return false
        if (batchProcessing != other.batchProcessing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = threshold.hashCode()
        result = 31 * result + minSize
        result = 31 * result + maxSize
        result = 31 * result + sensitivity.hashCode()
        result = 31 * result + analysisMode
        result = 31 * result + processingLevel
        result = 31 * result + targetType
        result = 31 * result + detectionRange.hashCode()
        result = 31 * result + processingFlags
        result = 31 * result + algorithmType.hashCode()
        result = 31 * result + tempThresholdHigh.hashCode()
        result = 31 * result + tempThresholdLow.hashCode()
        result = 31 * result + tempAnalysisEnabled.hashCode()
        result = 31 * result + regionOfInterest.contentHashCode()
        result = 31 * result + spatialFilterEnabled.hashCode()
        result = 31 * result + temporalFilterEnabled.hashCode()
        result = 31 * result + useGPU.hashCode()
        result = 31 * result + threadCount
        result = 31 * result + batchProcessing.hashCode()
        return result
    }
}