package com.topdon.tc001.thermal.data

/**
 * Thermal metadata for thermal image processing
 */
data class ThermalMetadata(
    val width: Int = 256,
    val height: Int = 192,
    val emissivity: Float = 0.95f,
    val distance: Float = 1.0f,
    val ambientTemperature: Float = 23.0f,
    val reflectedTemperature: Float = 23.0f,
    val humidity: Float = 50.0f,
    val atmosphericTemperature: Float = 23.0f,
    val captureTimestamp: Long = System.currentTimeMillis(),
    val deviceModel: String = "TC001",
    val serialNumber: String = "",
    val firmwareVersion: String = "1.0.0"
) {
    /**
     * Validate thermal metadata parameters
     */
    fun isValid(): Boolean {
        return width > 0 && 
               height > 0 && 
               emissivity in 0.1f..1.0f && 
               distance > 0f &&
               ambientTemperature > -50f && ambientTemperature < 100f
    }
    
    /**
     * Get thermal frame total pixel count
     */
    fun getPixelCount(): Int = width * height
    
    /**
     * Get thermal data size in bytes
     */
    fun getDataSize(): Int = width * height * 2 // 2 bytes per pixel
    
    companion object {
        /**
         * Create default metadata for TC001 device
         */
        fun createDefault(): ThermalMetadata {
            return ThermalMetadata()
        }
        
        /**
         * Create metadata for specific device model
         */
        fun createForDevice(deviceModel: String): ThermalMetadata {
            return when (deviceModel.uppercase()) {
                "TC001" -> ThermalMetadata(deviceModel = "TC001")
                "TC004" -> ThermalMetadata(width = 160, height = 120, deviceModel = "TC004")
                "TC007" -> ThermalMetadata(width = 384, height = 288, deviceModel = "TC007")
                else -> createDefault()
            }
        }
    }
}