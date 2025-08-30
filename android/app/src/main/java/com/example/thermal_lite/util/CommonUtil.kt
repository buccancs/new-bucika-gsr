package com.example.thermal_lite.util

object CommonUtil {
    // Common thermal utility functions
    fun getDefaultConfig(): Map<String, Any> {
        return emptyMap()
    }
    
    fun formatTemperature(temp: Float): String {
        return "${temp}°C"
    }
}