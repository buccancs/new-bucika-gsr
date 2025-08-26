package com.energy.iruvc.dual

/**
 * Dual UVC Camera stub implementation for thermal imaging
 */
class DualUVCCamera {
    
    fun getDualIRWidthFromFrame(): Int {
        // Stub implementation - returns default width
        return 256
    }
    
    fun getDualIRHeightFromFrame(): Int {
        // Stub implementation - returns default height
        return 192
    }
    
    fun getTemperatureDataFromFrame(tempData: ByteArray?): ByteArray? {
        // Stub implementation - returns the input data
        return tempData
    }
    
    fun updateTemperatureData(data: FloatArray) {
        // Stub implementation
    }
    
    fun getMaxTemperature(): Float {
        // Stub implementation
        return 100f
    }
    
    fun getMinTemperature(): Float {
        // Stub implementation  
        return -20f
    }
    
    fun getUserHighTemperature(): Float {
        // Stub implementation
        return getMaxTemperature()
    }
    
    fun getUserLowTemperature(): Float {
        // Stub implementation
        return getMinTemperature()
    }
}