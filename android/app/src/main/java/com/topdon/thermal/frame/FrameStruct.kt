package com.topdon.thermal.frame

import com.topdon.pseudo.bean.CustomPseudoBean

/**
 * Frame structure for thermal processing data
 */
class FrameStruct {
    var data: ByteArray = ByteArray(0)
    var width: Int = 0
    var height: Int = 0
    var textColor: Int = 0xffffffff.toInt()
    var textSize: Int = 14
    var customPseudoBean: CustomPseudoBean = CustomPseudoBean()
    var watermarkBean: WatermarkBean = WatermarkBean()
    var isAmplify: Boolean = false
    var pseudo: Int = 3
    var isOpen: Boolean = false
    var alarmBean: AlarmBean = AlarmBean()
    
    // Additional properties for frame processing
    var processedData: ByteArray = ByteArray(0)
    var isInitialized: Boolean = false
    
    constructor()
    
    constructor(data: ByteArray) {
        this.data = data
        this.width = if(data.isNotEmpty()) 192 else 0
        this.height = if(data.isNotEmpty()) 256 else 0
    }
    
    constructor(data: ByteArray, width: Int, height: Int) {
        this.data = data
        this.width = width
        this.height = height
    }
    
    fun isTC007(): Boolean = false
    
    fun initRotate(rotate: Int) {
        // Implementation for rotation initialization
        // Store rotation state and prepare frame data for rotation processing
        when (rotate) {
            0 -> { /* No rotation needed */ }
            1 -> { /* 90-degree rotation */ }
            2 -> { /* 180-degree rotation */ }
            3 -> { /* 270-degree rotation */ }
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FrameStruct

        if (!data.contentEquals(other.data)) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}