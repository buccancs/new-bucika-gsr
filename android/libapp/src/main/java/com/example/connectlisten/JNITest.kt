package com.example.connectlisten

import com.topdon.lib.core.so.algorithm

object JNITest {
    init {
        System.loadLibrary("opencv_java4")

    }

    fun maxTempL(imgBytes: ByteArray, tempByte: ByteArray, width: Int, height: Int): ByteArray {
        return algorithm.maxTempL(imgBytes, tempByte, width, height)
    }
    
    fun lowTemTrack(imgBytes: ByteArray, tempByte: ByteArray, width: Int, height: Int): ByteArray {
        return algorithm.lowTemTrack(imgBytes, tempByte, width, height)
    }
}