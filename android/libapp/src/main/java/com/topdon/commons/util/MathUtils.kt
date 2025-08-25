package com.topdon.commons.util

import androidx.annotation.NonNull
import java.util.Arrays
import kotlin.math.min
import kotlin.math.pow

object MathUtils {
    
    @JvmStatic
    fun setDoubleAccuracy(num: Double, scale: Int): Double {
        val factor = 10.0.pow(scale)
        return (num * factor).toInt() / factor
    }
    
    @JvmStatic
    fun getPercents(scale: Int, @NonNull vararg values: Float): FloatArray {
        val total = values.sum()
        val nonZeroIndices = values.mapIndexedNotNull { index, value -> 
            if (value != 0f) index else null 
        }
        
        if (total == 0f) {
            return FloatArray(values.size)
        }
        
        val fs = FloatArray(values.size)
        val sc = 10.0.pow(scale + 2).toInt()
        var sum = 0f
        
        nonZeroIndices.forEachIndexed { i, index ->
            if (i == nonZeroIndices.size - 1) {
                fs[index] = 1 - sum
            } else {
                fs[index] = ((values[index] / total * sc).toInt() / sc.toFloat())
                sum += fs[index]
            }
        }
        return fs
    }
    
    @JvmStatic
    @NonNull
    fun numberToBytes(bigEndian: Boolean, value: Long, len: Int): ByteArray {
        val bytes = ByteArray(8)
        for (i in 0..7) {
            val j = if (bigEndian) 7 - i else i
            bytes[i] = (value shr (8 * j) and 0xff).toByte()
        }
        return if (len > 8) {
            bytes
        } else {
            bytes.copyOfRange(
                if (bigEndian) 8 - len else 0,
                if (bigEndian) 8 else len
            )
        }
    }
    
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> bytesToNumber(bigEndian: Boolean, cls: Class<T>, @NonNull vararg src: Byte): T {
        val len = min(8, src.size)
        val bs = ByteArray(8)
        System.arraycopy(src, 0, bs, if (bigEndian) 8 - len else 0, len)
        var value = 0L
        
        for (i in 0..7) {
            val shift = (if (bigEndian) 7 - i else i) shl 3
            value = value or ((0xffL shl shift) and (bs[i].toLong() shl shift))
        }
        
        value = when (src.size) {
            1 -> value.toByte().toLong()
            2 -> value.toShort().toLong()
            in 3..4 -> value.toInt().toLong()
            else -> value
        }
        
        return when {
            cls == Short::class.java || cls == Short::class.javaPrimitiveType -> value.toShort() as T
            cls == Int::class.java || cls == Int::class.javaPrimitiveType -> value.toInt() as T
            cls == Long::class.java || cls == Long::class.javaPrimitiveType -> value as T
            else -> throw IllegalArgumentException("cls must be one of short, int and long")
        }
    }
    
    @JvmStatic
    fun reverseBitAndByte(src: ByteArray?): ByteArray? {
        if (src == null || src.isEmpty()) {
            return null
        }
        val target = ByteArray(src.size)
        
        for (i in src.indices) {
            var value = 0
            var tmp = src[src.size - 1 - i].toInt()
            for (j in 7 downTo 0) {
                value = value or ((tmp and 0x01) shl j)
                tmp = tmp shr 1
            }
            target[i] = value.toByte()
        }
        return target
    }
    
    @JvmStatic
    @NonNull
    fun splitPackage(@NonNull src: ByteArray, size: Int): List<ByteArray> {
        val list = mutableListOf<ByteArray>()
        val loop = src.size / size + if (src.size % size == 0) 0 else 1
        
        repeat(loop) { i ->
            val from = i * size
            val to = min(src.size, from + size)
            list.add(src.copyOfRange(from, to))
        }
        return list
    }
    
    @JvmStatic
    @NonNull
    fun joinPackage(@NonNull vararg src: ByteArray): ByteArray {
        var bytes = ByteArray(0)
        for (bs in src) {
            bytes = bytes.copyOf(bytes.size + bs.size)
            System.arraycopy(bs, 0, bytes, bytes.size - bs.size, bs.size)
        }
        return bytes
    }
    
    @JvmStatic
    fun calcCrc8(bytes: ByteArray): Int {
        var crc = 0
        for (b in bytes) {
            crc = crc xor b.toInt()
            repeat(8) {
                crc = if ((crc and 0x80) != 0) {
                    (crc shl 1) xor 0x07
                } else {
                    crc shl 1
                }
            }
        }
        return crc and 0xff
    }
    
    @JvmStatic
    fun calcCRC16_Modbus(data: ByteArray): Int {
        var crc = 0xffff
        for (b in data) {
            crc = crc xor if (b < 0) b.toInt() + 256 else b.toInt()
            repeat(8) {
                crc = if ((crc and 0x0001) != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }
        return crc and 0xffff
    }
    
    @JvmStatic
    fun calcCRC_CCITT_XModem(bytes: ByteArray): Int {
        var crc = 0
        val polynomial = 0x1021
        
        for (b in bytes) {
            repeat(8) { i ->
                val bit = ((b.toInt() shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return crc and 0xffff
    }
    
    @JvmStatic
    fun calcCRC_CCITT_XModem(bytes: ByteArray, offset: Int, len: Int): Int {
        var crc = 0
        val polynomial = 0x1021
        
        for (i in offset until offset + len) {
            val b = bytes[i]
            repeat(8) { j ->
                val bit = ((b.toInt() shr (7 - j)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return crc and 0xffff
    }
    
    @JvmStatic
    fun calcCRC_CCITT_0xFFFF(bytes: ByteArray): Int {
        var crc = 0xffff
        val polynomial = 0x1021
        
        for (b in bytes) {
            repeat(8) { i ->
                val bit = ((b.toInt() shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return crc and 0xffff
    }
    
    @JvmStatic
    fun calcCRC_CCITT_0xFFFF(bytes: ByteArray, offset: Int, len: Int): Int {
        var crc = 0xffff
        val polynomial = 0x1021
        
        for (i in offset until offset + len) {
            val b = bytes[i]
            repeat(8) { j ->
                val bit = ((b.toInt() shr (7 - j)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return crc and 0xffff
    }
}