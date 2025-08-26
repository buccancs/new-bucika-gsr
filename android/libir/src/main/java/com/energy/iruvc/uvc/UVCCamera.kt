package com.energy.iruvc.uvc

// Minimal stub for UVCCamera with required methods
open class UVCCamera {
    
    var openStatus: Boolean = false
        
    var supportedSizeList: List<CameraSize> = listOf(
        CameraSize(256, 192),
        CameraSize(384, 288),
        CameraSize(640, 480), 
        CameraSize(1024, 768),
        CameraSize(1280, 960)
    )
        private set
        
    var nativePtr: Long = 0
        private set
    
    private var currentFps = 30
    private var minFps = 1
    private var maxFps = 60
    private var bandwidth = 1.0f
    private var previewMode: Any? = null
    private var frameCallback: Any? = null
    private var previewWidth = 640
    private var previewHeight = 480
    
    val supportedSize: String
        get() = "${supportedSizeList.size} sizes available: ${supportedSizeList.joinToString { "${it.width}x${it.height}" }}"
    
    fun getSupportSizeList(format: Any? = null): List<CameraSize> {
        // Return filtered sizes based on format if needed
        return when (format?.toString()) {
            "Y16" -> supportedSizeList.filter { it.width <= 640 }
            "RGB" -> supportedSizeList
            else -> supportedSizeList
        }
    }
    
    fun setDefaultPreviewMaxFps(fps: Int) {
        maxFps = fps.coerceIn(1, 120)
        if (currentFps > maxFps) {
            currentFps = maxFps
        }
        println("UVCCamera: Max FPS set to $maxFps")
    }
    
    fun setDefaultPreviewMinFps(fps: Int) {
        minFps = fps.coerceIn(1, 60)
        if (currentFps < minFps) {
            currentFps = minFps
        }
        println("UVCCamera: Min FPS set to $minFps")
    }
    
    fun setDefaultBandwidth(bandwidth: Float) {
        this.bandwidth = bandwidth.coerceIn(0.1f, 10.0f)
        println("UVCCamera: Bandwidth set to ${this.bandwidth}")
    }
    
    fun setDefaultPreviewMode(mode: Any) {
        previewMode = mode
        println("UVCCamera: Preview mode set to $mode")
    }
    
    fun setFrameCallback(callback: Any?) {
        frameCallback = callback
        println("UVCCamera: Frame callback ${if (callback != null) "set" else "cleared"}")
    }
    
    fun setUSBPreviewSize(width: Int, height: Int): Int {
        val supportedSize = supportedSizeList.find { it.width == width && it.height == height }
        return if (supportedSize != null) {
            previewWidth = width
            previewHeight = height
            println("UVCCamera: Preview size set to ${width}x${height}")
            0 // Success
        } else {
            println("UVCCamera: Unsupported preview size ${width}x${height}")
            -1 // Error
        }
    }
    
    fun openUVCCamera(controlBlock: Any?): Int {
        return if (!openStatus) {
            openStatus = true
            nativePtr = System.currentTimeMillis() // Simulate native pointer
            println("UVCCamera: Camera opened with control block $controlBlock")
            0 // Success
        } else {
            println("UVCCamera: Camera already open")
            -2 // Already open
        }
    }
    
    fun onStartPreview() {
        if (openStatus) {
            println("UVCCamera: Preview started at ${previewWidth}x${previewHeight} @ ${currentFps}fps")
            
            // Simulate frame callback if set
            frameCallback?.let {
                println("UVCCamera: Frame callback active")
            }
        } else {
            println("UVCCamera: Cannot start preview - camera not open")
        }
    }
    
    fun onStopPreview() {
        println("UVCCamera: Preview stopped")
    }
    
    fun onDestroyPreview() {
        onStopPreview()
        frameCallback = null
        println("UVCCamera: Preview destroyed")
    }
    
    fun close() {
        onDestroyPreview()
        openStatus = false
        nativePtr = 0
        println("UVCCamera: Camera closed")
    }
    
    fun onPausePreview() {
        println("UVCCamera: Preview paused")
    }
    
    fun onResumePreview() {
        if (openStatus) {
            println("UVCCamera: Preview resumed")
        }
    }
    
    // Additional utility methods
    fun getCurrentResolution(): CameraSize {
        return CameraSize(previewWidth, previewHeight)
    }
    
    fun getCurrentFps(): Int {
        return currentFps
    }
    
    fun setCurrentFps(fps: Int) {
        currentFps = fps.coerceIn(minFps, maxFps)
    }
}

data class CameraSize(val width: Int, val height: Int)

enum class UVCType {
    USB, WIFI, BLE, USB_UVC
}

class ConcreateUVCBuilder {
    fun setUVCType(type: UVCType): ConcreateUVCBuilder = this
    fun build(): UVCCamera = UVCCamera()
}