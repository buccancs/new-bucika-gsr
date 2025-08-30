package com.example.thermal_lite.util

import android.content.Context

object CommonUtil {
    // Common thermal utility functions
    fun getDefaultConfig(): Map<String, Any> {
        return emptyMap()
    }
    
    fun formatTemperature(temp: Float): String {
        return "${temp}°C"
    }
    
    fun getAssetData(context: Context, assetPath: String): ByteArray {
        return try {
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}