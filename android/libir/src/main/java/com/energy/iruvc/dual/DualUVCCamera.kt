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
    
    fun setImageRotate(rotate: Any) {
        // Stub implementation for image rotation
    }
    
    fun addIrUVCCamera(irUVCCamera: Any?) {
        // Stub implementation for adding IR UVC camera
    }
    
    fun setFrameCallback(callback: Any?) {
        // Stub implementation for frame callback
    }
    
    fun onStartPreview() {
        // Stub implementation for start preview
    }
    
    fun onStopPreview() {
        // Stub implementation for stop preview
    }
    
    fun onDestroy() {
        // Stub implementation for cleanup
    }
    
    fun setIrDataPreHandleEnable(enabled: Boolean) {
        // Stub implementation for IR data pre-handle
    }
    
    fun setIrFrameCallback(callback: Any?) {
        // Stub implementation for IR frame callback
    }
    
    fun setFusion(enabled: Boolean) {
        // Stub implementation for fusion mode
    }
    
    fun setAlignCallback(callback: Any?) {
        // Stub implementation for alignment callback
    }
    
    fun startManualAlign() {
        // Stub implementation for manual alignment
    }
}