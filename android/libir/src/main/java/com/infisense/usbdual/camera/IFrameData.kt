package com.infisense.usbdual.camera

import com.infisense.usbdual.Const

object IFrameData {
    /**
     * 融合图像数据长度，ARGB，故值为：
     * 融合图像输出宽度 x 融合图像输出高度 x 4.
     */
    val FUSION_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4
    
    /**
     * 原始红外数据长度、原始温度数据长度，YUV-Y16，故值为：
     * 原始红外宽度 x 原始红外高度 x 2.
     */
    val ORIGINAL_LEN = Const.IR_WIDTH * Const.IR_HEIGHT * 2
    
    /**
     * 缩放温度数据长度，YUV-422，故值为：
     * 融合图像输出宽度 x 融合图像输出高度 x 2.
     */
    val REMAP_TEMP_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2
    
    /**
     * 原始可见光数据长度，RGB24，故值为：
     * 原始可见光宽度 x 原始可见光高度 x 3.
     */
    val LIGHT_LEN = Const.VL_WIDTH * Const.VL_HEIGHT * 3
    
    /**
     * 缩放可见光数据长度，ARGB，故值为：
     * 原始可见光宽度 x 原始可见光高度 x 4.
     */
    val P_IN_P_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4
    
    /**
     * 一帧除画中画缩放缩放可见光数据之外的所有数据长度，
     * 包含 融合图像、原始红外、原始温度、缩放温度、原始可见光、画中画缩放可见光 数据.
     * 值为上述数据长度之和.
     */
    val FRAME_LEN = FUSION_LEN + ORIGINAL_LEN + ORIGINAL_LEN + REMAP_TEMP_LEN + LIGHT_LEN + P_IN_P_LEN

    /**
     * 将指定帧数据中 ARGB **融合图像数据** 复制到指定数组中.
     */
    fun readFusionData(frame: ByteArray, fusionData: ByteArray? = null): ByteArray {
        val data = fusionData ?: ByteArray(FUSION_LEN)
        System.arraycopy(frame, 0, data, 0, data.size) // 融合图像数据，ARGB
        return data
    }

    /**
     * 将指定帧数据中 YUV-16 **原始红外数据** 复制到指定数组中.
     */
    fun readNorIRData(frame: ByteArray, irData: ByteArray? = null): ByteArray {
        val data = irData ?: ByteArray(ORIGINAL_LEN)
        System.arraycopy(frame, FUSION_LEN, data, 0, data.size) // 原始红外数据，YUV-Y16
        return data
    }

    /**
     * 将指定帧数据中 YUV-16 **原始温度数据** 复制到指定数组中.
     */
    fun readNorTempData(frame: ByteArray, norTempData: ByteArray? = null): ByteArray {
        val data = norTempData ?: ByteArray(ORIGINAL_LEN)
        System.arraycopy(frame, FUSION_LEN + ORIGINAL_LEN, data, 0, data.size) // 原始温度数据，YUV-Y16
        return data
    }

    /**
     * 将指定帧数据中 YUV-422 **缩放温度数据** 复制到指定数组中.
     */
    fun readRemapTempData(frame: ByteArray, remapTempData: ByteArray? = null): ByteArray {
        val data = remapTempData ?: ByteArray(REMAP_TEMP_LEN)
        System.arraycopy(frame, FUSION_LEN + ORIGINAL_LEN + ORIGINAL_LEN, data, 0, data.size) // 缩放温度数据，YUV-422
        return data
    }
