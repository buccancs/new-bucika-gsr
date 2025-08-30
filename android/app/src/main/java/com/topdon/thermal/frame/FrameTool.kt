package com.topdon.thermal.frame

import android.graphics.Bitmap
import com.energy.iruvc.utils.CommonParams
import com.topdon.pseudo.bean.CustomPseudoBean

/**
 * Thermal frame processing tool
 */
class FrameTool {
    
    /**
     * Get scaled bitmap with pseudo color processing
     */
    fun getScrPseudoColorScaledBitmap(
        pseudoColorMode: CommonParams.PseudoColorType = CommonParams.PseudoColorType.PSEUDO_3,
        max: Float = -273f,
        min: Float = 273f,
        rotate: ImageParams = ImageParams.ROTATE_0,
        customPseudoBean: CustomPseudoBean,
        maxTemperature: Float, 
        minTemperature: Float,
        isAmplify: Boolean
    ): Bitmap? {
        // TODO: Implement thermal processing logic
        return null
    }
    
    /**
     * Get temperature bytes for thermal processing
     */
    fun getTempBytes(rotate: ImageParams = ImageParams.ROTATE_0): ByteArray? {
        // TODO: Implement temperature byte processing
        return null
    }
    
    /**
     * Get source temperature data
     */
    fun getSrcTemp(): TempResult {
        return TempResult(maxTemperature = 100f, minTemperature = 0f)
    }
}