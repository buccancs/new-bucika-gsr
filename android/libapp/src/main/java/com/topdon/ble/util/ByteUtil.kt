package com.topdon.ble.util

import android.util.Log

object ByteUtil {
    
    @JvmStatic
    fun byteMerger(byte1: ByteArray, byte2: Int, byte3: Int, byte4: Int): ByteArray {
        return byteMerger(byte1, intToByteArray(byte2), intToByteArray(byte3), intToByteArray2(byte4))
    }
    
    @JvmStatic
    fun byteMerger(byte1: ByteArray, byte2: String, byte3: String): ByteArray {
        return byteMerger(byte1, byte2.toByteArray(), byte3.toByteArray())
    }
    
    @JvmStatic
    fun byteMerger(byte1: ByteArray, byte2: String, byte3: String, byte4: String): ByteArray {
        return byteMerger(byte1, byte2.toByteArray(), byte3.toByteArray(), byte4.toByteArray())
    }
    
    @JvmStatic
    fun byteMerger(byte1: String, byte2: Int): ByteArray {
        return byteMerger(byte1.toByteArray(), intToByteArray(byte2))
    }
    
    @JvmStatic
    fun byteMerger(byte1: ByteArray, byte2: Int): ByteArray {
        return byteMerger(byte1, intToByteArray(byte2))
    }
    
    @JvmStatic
    fun byteMerger(byte1: String, byte2: String): ByteArray {
        return byteMerger(byte1.toByteArray(), byte2.toByteArray())
    }
    
    @JvmStatic
    fun byteMerger(byte1: String, byte2: ByteArray): ByteArray {
        return byteMerger(byte1.toByteArray(), byte2)
    }
    
    @JvmStatic
    fun byteMerger(byte1: ByteArray, byte2: String): ByteArray {
        return byteMerger(byte1, byte2.toByteArray())
    }
    
    @JvmStatic
    fun byteMerger(vararg bytes: ByteArray): ByteArray {
        val totalLength = bytes.sumOf { it.size }
        val result = ByteArray(totalLength)
        
        var offset = 0
        for (tmp in bytes) {
            System.arraycopy(tmp, 0, result, offset, tmp.size)
            offset += tmp.size
        }
        return result
    }
    
    @JvmStatic
    fun intToByteArray(i: Int): ByteArray {
        return byteArrayOf((i and 0xFF).toByte())
    }
    
    @JvmStatic
    fun intToByteArray2(i: Int): ByteArray {
        return byteArrayOf(
            ((i shr 24) and 0xFF).toByte(),
            ((i shr 16) and 0xFF).toByte(),
            ((i shr 8) and 0xFF).toByte(),
            (i and 0xFF).toByte()
        )
    }
    
    @JvmStatic
    fun longToBytes(values: Long): ByteArray {
        val buffer = ByteArray(4)
        for (i in 0..3) {
            val offset = (4 - i - 1) * 8
            buffer[i] = ((values shr offset) and 0xff).toByte()
        }
        return buffer
    }
    
    @JvmStatic
    fun bytesToFloat(bytes: ByteArray): Float {
        val value = HexUtil.bytesToHexString(bytes).toInt(16)
        return value.toFloat()
    }
    
    @JvmStatic
    fun byteToFloat(vararg bytes: Byte): Float {
        val resultByte = bytes.toByteArray()
        val value = HexUtil.bytesToHexString(resultByte).toInt(16)
        Log.e("bcf", "bytesToFloat bytes: ${HexUtil.bytesToHexString(resultByte)} float:$value")
        return value.toFloat()
    }
    
    @JvmStatic
    fun byteToInt(b: Byte): Int {
        return b.toInt() and 0xFF
    }
    
    @JvmStatic
    fun short2byte(s: Short): ByteArray {
        val b = ByteArray(2)
        for (i in 0..1) {
            val offset = 16 - (i + 1) * 8
            b[i] = ((s.toInt() shr offset) and 0xff).toByte()
        }
        return b
    }
    
    @JvmStatic
    fun byteArrayToInt(bytes: ByteArray): Int {
        var value = 0
        for (i in 0..3) {
            val shift = (3 - i) * 8
            value += (bytes[i].toInt() and 0xFF) shl shift
        }
        return value
    }
    
    @JvmStatic
    fun getCmdType(bytes: ByteArray): String {
        val hex = HexUtil.bytesToHexString(bytes)
        return if (hex.length >= 16) {
            hex.substring(12, 16)
        } else {
            ""
        }
    }
    
    @JvmStatic
    fun getCmd(bytes: ByteArray): String {
        val hex = HexUtil.bytesToHexString(bytes)
        return if (hex.length >= 16) {
            hex.substring(12, 14)
        } else {
            ""
        }
    }
}