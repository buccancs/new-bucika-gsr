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
    
    fun setDefaultBandwidth(bandwidth: Int) {
        // Stub implementation
    }
    
    fun setDefaultPreviewMode(mode: Any) {
        // Stub implementation
    }
    
    fun setFrameCallback(callback: Any?) {
        // Stub implementation
    }
    
    fun setUSBPreviewSize(size: Any) {
        // Stub implementation
    }
    
    fun openUVCCamera(controlBlock: Any?) {
        // Stub implementation
        openStatus = true
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