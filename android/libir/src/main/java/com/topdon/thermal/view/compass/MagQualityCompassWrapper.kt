package com.topdon.thermal.view.compass

// Stub compass wrapper for compilation - compass library not available
class MagQualityCompassWrapper {
    
    // Stub bearing value
    val bearing: Float = 0.0f
    
    // Stub declination  
    var declination: Float = 0.0f
    
    // Stub reading status
    val hasValidReading: Boolean = false
    
    // Stub raw bearing
    val rawBearing: Float = 0.0f
    
    // Stub quality (simplified as Int)
    val quality: Int = 0
    
    fun startImpl() {
        // Compass start stub
        println("MagQualityCompassWrapper: Started")
    }
    
    fun stopImpl() {
        // Compass stop stub  
        println("MagQualityCompassWrapper: Stopped")
    }
    
    private fun onReading(): Boolean {
        // Reading callback stub
        return true
    }
}
