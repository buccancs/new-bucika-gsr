package com.energy.iruvc.uvc

// Minimal stub for UVCCamera with required methods
open class UVCCamera {
    
    var openStatus: Boolean = false
    var supportedSizeList: List<CameraSize> = listOf(
        CameraSize(256, 192),
        CameraSize(640, 480), 
        CameraSize(1024, 768)
    )
    var nativePtr: Long = 0
    
    val supportedSize: String
        get() = "${supportedSizeList.size} sizes available"
    
    fun getSupportSizeList(format: Any? = null): List<CameraSize> {
        // Return common camera sizes for thermal imaging
        return supportedSizeList
    }
    
    fun setDefaultPreviewMaxFps(fps: Int) {
        // Stub implementation
    }
    
    fun setDefaultPreviewMinFps(fps: Int) {
        // Stub implementation  
    }
    
    fun setDefaultBandwidth(bandwidth: Float) {
        // Stub implementation - accept Float as expected by callers
    }
    
    fun setDefaultPreviewMode(mode: Any) {
        // Stub implementation
    }
    
    fun setFrameCallback(callback: Any?) {
        // Stub implementation
    }
    
    fun setUSBPreviewSize(width: Int, height: Int): Int {
        // Stub implementation
        return 0
    }
    
    fun openUVCCamera(controlBlock: Any?): Int {
        // Stub implementation
        openStatus = true
        return 0
    }
    
    fun onStartPreview() {
        // Stub implementation
    }
    
    fun onStopPreview() {
        // Stub implementation
    }
    
    fun onDestroyPreview() {
        // Stub implementation
    }
    
    fun close() {
        // Stub implementation
        openStatus = false
    }
    
    fun onPausePreview() {
        // Stub implementation
    }
    
    fun onResumePreview() {
        // Stub implementation
    }
    
    // Add other methods as needed
}

data class CameraSize(val width: Int, val height: Int)

enum class UVCType {
    USB, WIFI, BLE, USB_UVC
}

class ConcreateUVCBuilder {
    fun setUVCType(type: UVCType): ConcreateUVCBuilder = this
    fun build(): UVCCamera = UVCCamera()
}