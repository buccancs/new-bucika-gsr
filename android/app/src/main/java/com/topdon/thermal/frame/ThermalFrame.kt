package com.topdon.thermal.frame

import com.topdon.pseudo.bean.CustomPseudoBean
import com.topdon.lib.core.bean.AlarmBean
import com.topdon.lib.core.bean.WatermarkBean

data class FrameStruct(
    val data: ByteArray, 
    val width: Int, 
    val height: Int,
    val pseudo: Int = 0,
    val customPseudoBean: CustomPseudoBean = CustomPseudoBean(),
    val isAmplify: Boolean = false,
    val alarmBean: AlarmBean = AlarmBean(),
    val watermarkBean: WatermarkBean = WatermarkBean(),
    val textColor: Int = 0xffffffff.toInt(),
    val textSize: Int = 14
) {
    constructor(data: ByteArray) : this(data, 0, 0)
}

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
    
    fun read(path: String): FrameStruct {
        return FrameStruct(ByteArray(0), 0, 0)
    }
    
    fun initStruct(struct: FrameStruct) {
        // Initialize frame structure
    }
    
    fun initRotate(): Int {
        return ROTATE_270
    }
    
    fun getScrPseudoColorScaledBitmap(
        pseudoMode: Int,
        rotate: Int = ROTATE_270,
        customPseudoBean: CustomPseudoBean? = null,
        maxTemperature: Float = 0f,
        minTemperature: Float = 0f,
        isAmplify: Boolean = false
    ): android.graphics.Bitmap? {
        return null
    }
    
    fun getSrcTemp(): TempResult {
        return TempResult(0f, 0f)
    }
    
    fun getTempBytes(rotate: Int = ROTATE_270): ByteArray {
        return ByteArray(0)
    }
    
    fun setRotate(rotate: Int) {
        // Set rotation for thermal frame
    }
    
    fun tempCorrect(temp: Float): Float {
        return temp
    }
    
    fun updateImage(bitmap: android.graphics.Bitmap?) {
        // Update thermal image display
    }
    
    fun setDefLimit() {
        // Set default temperature limits
    }
    
    fun updateImageAndSeekbarColorList(customPseudoBean: CustomPseudoBean) {
        // Update image and seekbar colors
    }
    
    fun updateTemperatureSeekBar(flag: Boolean, drawableRes: Int, text: String) {
        // Update temperature seekbar
    }
    
    // Asset paths
    const val TAU_HIGH_GAIN_ASSET_PATH = "tau_high_gain.bin"
    const val TAU_LOW_GAIN_ASSET_PATH = "tau_low_gain.bin"
}

data class TempResult(val maxTemperature: Float, val minTemperature: Float)

data class ImageParams(val minTemp: Float, val maxTemp: Float) {
    companion object {
        const val ROTATE_90 = 90
        const val ROTATE_270 = 270
    }
}