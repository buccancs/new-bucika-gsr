package com.guide.zm04c.matrix

import android.graphics.Bitmap

class NativeGuideCore {

    init {
        System.loadLibrary("guide_zm04c_matrix")
    }

    external fun toFloatTempMatrix(floats: FloatArray, bytes: ByteArray)
    external fun yuv2Bitmap(bitmap: Bitmap, yuv: ByteArray)
    external fun crc(data: ByteArray): Int
}