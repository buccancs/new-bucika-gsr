package com.infisense.usbdual.camera

import com.infisense.usbdual.Const

object IFrameData {
    
    val FUSION_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4
    
    val ORIGINAL_LEN = Const.IR_WIDTH * Const.IR_HEIGHT * 2
    
    val REMAP_TEMP_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2
    
    val LIGHT_LEN = Const.VL_WIDTH * Const.VL_HEIGHT * 3
    
    val P_IN_P_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4
    
    val FRAME_LEN = FUSION_LEN + ORIGINAL_LEN + ORIGINAL_LEN + REMAP_TEMP_LEN + LIGHT_LEN + P_IN_P_LEN

    fun readFusionData(frame: ByteArray, fusionData: ByteArray? = null): ByteArray {
        val data = fusionData ?: ByteArray(FUSION_LEN)
        System.arraycopy(frame, 0, data, 0, data.size)
        return data
    }

    fun readNorIRData(frame: ByteArray, irData: ByteArray? = null): ByteArray {
        val data = irData ?: ByteArray(ORIGINAL_LEN)
        System.arraycopy(frame, FUSION_LEN, data, 0, data.size)
        return data
    }

    fun readNorTempData(frame: ByteArray, norTempData: ByteArray? = null): ByteArray {
        val data = norTempData ?: ByteArray(ORIGINAL_LEN)
        System.arraycopy(frame, FUSION_LEN + ORIGINAL_LEN, data, 0, data.size)
        return data
    }

    fun readRemapTempData(frame: ByteArray, remapTempData: ByteArray? = null): ByteArray {
        val data = remapTempData ?: ByteArray(REMAP_TEMP_LEN)
        System.arraycopy(frame, FUSION_LEN + ORIGINAL_LEN + ORIGINAL_LEN, data, 0, data.size)
        return data
    }
}
