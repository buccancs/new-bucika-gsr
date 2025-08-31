package com.topdon.tc001.thermal.data

/**
 * Temperature point data class for thermal measurements
 */
data class TemperaturePoint(
    val x: Int,
    val y: Int,
    val temperature: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if this point represents a valid temperature measurement
     */
    fun isValid(): Boolean {
        return x >= 0 && y >= 0 && temperature > -273.15f && temperature < 1000f
    }
    
    /**
     * Get temperature in Fahrenheit
     */
    fun getTemperatureFahrenheit(): Float {
        return temperature * 9f / 5f + 32f
    }
    
    /**
     * Get temperature in Kelvin
     */
    fun getTemperatureKelvin(): Float {
        return temperature + 273.15f
    }
    
    companion object {
        /**
         * Create invalid temperature point
         */
        fun invalid(): TemperaturePoint {
            return TemperaturePoint(-1, -1, Float.NaN)
        }
    }
}