package com.topdon.thermal.frame

data class FrameStruct(val data: ByteArray, val width: Int, val height: Int)

object FrameTool {
    fun processFrame(data: ByteArray): FrameStruct {
        return FrameStruct(data, 0, 0)
    }
}

data class ImageParams(val minTemp: Float, val maxTemp: Float)