package com.example.open3d

// Stub JNI tool for compilation - native implementation needed
object JNITool {
    
    @JvmStatic
    external fun processImage(data: ByteArray, width: Int, height: Int): ByteArray?
    
    @JvmStatic
    external fun initializeOpenCV(): Boolean
    
    @JvmStatic
    external fun processFrame(frame: ByteArray): ByteArray?
    
    @JvmStatic
    external fun cleanup(): Unit
    
    // Initialize native library (if available)
    init {
        try {
            System.loadLibrary("open3d")
        } catch (e: UnsatisfiedLinkError) {
            // Library not available - using stub implementation
        }
    }
}