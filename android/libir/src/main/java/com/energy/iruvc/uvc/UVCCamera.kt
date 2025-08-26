package com.energy.iruvc.uvc

// Minimal stub for UVCCamera with required methods
open class UVCCamera {
    
    var openStatus: Boolean = false
        private set
    
    fun getSupportSizeList(format: Any? = null): List<CameraSize> {
        // Return common camera sizes for thermal imaging
        return listOf(
            CameraSize(256, 192),
            CameraSize(640, 480),
            CameraSize(1024, 768)
        )
    }
    
    fun setDefaultPreviewMaxFps(fps: Int) {
        // Stub implementation
    }
    
    fun openUVCCamera(controlBlock: Any?) {
        // Stub implementation
        openStatus = true
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