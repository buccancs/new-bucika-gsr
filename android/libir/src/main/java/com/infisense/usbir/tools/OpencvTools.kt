package com.infisense.usbir.tools

object OpencvTools {
    
    @JvmStatic
    fun getStatus(firstFrame: ByteArray?, currentFrame: ByteArray?): Boolean {
        // Stub implementation - compare frames
        return firstFrame != null && currentFrame != null && firstFrame.contentEquals(currentFrame)
    }
    
    @JvmStatic
    fun compareFrames(frame1: ByteArray?, frame2: ByteArray?): Boolean {
        return frame1 != null && frame2 != null
    }
    
    @JvmStatic
    fun supImage(imageDst: ByteArray?, height: Int, width: Int, amplifyArray: ByteArray) {
        // Stub implementation for image superresolution/enhancement
        if (imageDst != null && imageDst.size <= amplifyArray.size) {
            System.arraycopy(imageDst, 0, amplifyArray, 0, imageDst.size)
        }
    }
    
    @JvmStatic
    fun getOneColorByTempUnif(maxTemp: Float, minTemp: Float, currentTemp: Float, colorType: Int): Int {
        // Stub implementation for temperature-to-color mapping
        val ratio = if (maxTemp != minTemp) (currentTemp - minTemp) / (maxTemp - minTemp) else 0f
        val clampedRatio = ratio.coerceIn(0f, 1f)
        
        // Simple gradient from blue (cold) to red (hot)
        val red = (clampedRatio * 255).toInt()
        val blue = ((1f - clampedRatio) * 255).toInt()
        val green = 128 // neutral green
        
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
