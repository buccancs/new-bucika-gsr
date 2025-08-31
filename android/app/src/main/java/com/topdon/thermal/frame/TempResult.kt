package com.topdon.thermal.frame

/**
 * Temperature result data class for thermal processing
 */
data class TempResult(
    val maxTemperature: Float = 100f,
    val minTemperature: Float = 0f,
    val avgTemperature: Float = 50f,
    val averageTemperature: Float = avgTemperature, // Alias for compatibility
    val temperatureRange: Float = maxTemperature - minTemperature
)