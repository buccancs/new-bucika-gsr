package com.topdon.thermal.frame

data class FrameStruct(val data: ByteArray, val width: Int, val height: Int)

object FrameTool {
    // Rotation constants  
    const val ROTATE_90 = 90
    const val ROTATE_270 = 270
    
    fun processFrame(data: ByteArray): FrameStruct {
        return FrameStruct(data, 0, 0)
    }
    
    fun getAssetData(path: String): ByteArray {
        return ByteArray(0)
    }
    
    // Asset paths
    const val TAU_HIGH_GAIN_ASSET_PATH = "tau_high_gain.bin"
    const val TAU_LOW_GAIN_ASSET_PATH = "tau_low_gain.bin"
}

data class ImageParams(val minTemp: Float, val maxTemp: Float)