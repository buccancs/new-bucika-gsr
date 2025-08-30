package com.topdon.thermal.frame

import com.topdon.pseudo.bean.CustomPseudoBean

/**
 * Frame structure for thermal processing configuration
 */
data class FrameStruct(
    var textColor: Int = 0xffffffff.toInt(),
    var textSize: Int = 14,
    var customPseudoBean: CustomPseudoBean = CustomPseudoBean(),
    var watermarkBean: WatermarkBean = WatermarkBean(),
    var isAmplify: Boolean = false
) {
    fun isTC007(): Boolean = false
}

/**
 * Watermark configuration for thermal images
 */
data class WatermarkBean(
    var title: String = "",
    var address: String = "",
    var isAddTime: Boolean = false
)