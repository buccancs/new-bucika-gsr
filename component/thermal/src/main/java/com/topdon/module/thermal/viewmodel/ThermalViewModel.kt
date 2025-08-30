package com.topdon.module.thermal.viewmodel

import com.topdon.lib.core.ktbase.BaseViewModel

class ThermalViewModel : BaseViewModel() {
    /**
     * 修改yuv温度上下限的数据
     * white:82 FF
     * black:82 00
     */
    fun yuvArea(yuv: ByteArray, temp: FloatArray, max: Float, min: Float) {
        for (i in temp.indices) {
            if (temp[i] < min) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0x00.toByte()
            }
            if (temp[i] > max) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0xFF.toByte()
            }
        }
    }

}