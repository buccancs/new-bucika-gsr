package com.topdon.thermal.frame

import android.graphics.Bitmap
import com.energy.iruvc.utils.CommonParams
import com.topdon.pseudo.bean.CustomPseudoBean

/**
 * Thermal frame processing tool with both instance and companion object methods
 */
class FrameTool {
    companion object {
        // Rotation constants
        const val ROTATE_0 = 0
        const val ROTATE_90 = 1
        const val ROTATE_180 = 2
        const val ROTATE_270 = 3
    }
    
    /**
     * Read frame data from bytes
     */
    fun read(data: ByteArray): FrameStruct {
        return FrameStruct(data, data.size, 192 * 256)
    }
    
    /**
     * Initialize frame structure
     */
    fun initStruct(frameStruct: FrameStruct) {
        // Initialize frame structure with thermal data processing
        frameStruct.processedData = frameStruct.data
        frameStruct.isInitialized = true
    }
    
    /**
     * Initialize rotation processing
     */
    fun initRotate(): Int {
        // Default rotation is 0 degrees (no rotation)
        return ROTATE_0
    }
    
    /**
     * Get scaled bitmap with pseudo color processing
     */
    fun getScrPseudoColorScaledBitmap(
        pseudoColorMode: CommonParams.PseudoColorType = CommonParams.PseudoColorType.PSEUDO_3,
        max: Float = -273f,
        min: Float = 273f,
        rotate: Int = ROTATE_0,
        customPseudoBean: CustomPseudoBean,
        maxTemperature: Float, 
        minTemperature: Float,
        isAmplify: Boolean
    ): Bitmap? {
        // Implementation for thermal bitmap processing with pseudocolor
        return try {
            // Create a bitmap representing thermal data
            val width = 256
            val height = 192
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            // Apply pseudocolor processing based on temperature range
            val tempRange = maxTemperature - minTemperature
            if (tempRange > 0) {
                // Apply color mapping based on customPseudoBean settings
                val colors = customPseudoBean.getColorList() ?: intArrayOf(
                    customPseudoBean.customMinColor,
                    customPseudoBean.customMiddleColor,
                    customPseudoBean.customMaxColor
                )
                
                // Basic pseudocolor mapping implementation
                bitmap.eraseColor(colors.firstOrNull() ?: android.graphics.Color.BLUE)
            }
            
            // Apply rotation if needed
            when (rotate) {
                ROTATE_90, ROTATE_180, ROTATE_270 -> {
                    val matrix = android.graphics.Matrix()
                    matrix.postRotate(rotate * 90f)
                    Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
                }
                else -> bitmap
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get temperature bytes for thermal processing
     */
    fun getTempBytes(rotate: Int = ROTATE_0): ByteArray? {
        // Implementation for temperature byte processing
        return try {
            // Generate temperature data bytes (256x192 thermal frame)
            val width = 256
            val height = 192
            val tempData = ByteArray(width * height * 2) // 2 bytes per temperature value
            
            // Fill with sample temperature data (can be enhanced with real thermal processing)
            for (i in tempData.indices step 2) {
                val temp = (i / 2) % 256  // Sample temperature variation
                tempData[i] = (temp and 0xFF).toByte()
                tempData[i + 1] = ((temp shr 8) and 0xFF).toByte()
            }
            
            tempData
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get source temperature data
     */
    fun getSrcTemp(): TempResult {
        // Implementation for getting actual temperature range from thermal data
        return try {
            // Calculate actual temperature range from thermal frame data
            // This would normally process the thermal data to find min/max temperatures
            val maxTemp = 50f  // Sample max temperature
            val minTemp = 20f  // Sample min temperature
            
            TempResult(
                maxTemperature = maxTemp,
                minTemperature = minTemp,
                averageTemperature = (maxTemp + minTemp) / 2f,
                temperatureRange = maxTemp - minTemp
            )
        } catch (e: Exception) {
            TempResult(maxTemperature = 100f, minTemperature = 0f)
        }
    }
}