/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.topdon.thermal.utils

/*
 * @Description:
 * @Author:         brilliantzhao
 * @CreateDate:     2022.9.8 10:25
 * @UpdateUser:
 * @UpdateDate:     2022.9.8 10:25
 * @UpdateRemark:
 */
object HexDump {
    private val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    private val HEX_LOWER_CASE_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    fun dumpHexString(array: ByteArray?): String {
        return if (array == null) "(null)" else dumpHexString(array, 0, array.size)
    }

    fun dumpHexString(array: ByteArray?, offset: Int, length: Int): String {
        if (array == null) return "(null)"
        
        val result = StringBuilder()
        val line = ByteArray(16)
        var lineIndex = 0

        result.append("\n0x")
        result.append(toHexString(offset))

        for (i in offset until offset + length) {
            if (lineIndex == 16) {
                result.append(" ")

                for (j in 0..15) {
                    if (line[j] > ' '.code.toByte() && line[j] < '~'.code.toByte()) {
                        result.append(String(line, j, 1))
                    } else {
                        result.append(".")
                    }
                }

                result.append("\n0x")
                result.append(toHexString(i))
                lineIndex = 0
            }

            val b = array[i]
            result.append(" ")
            result.append(HEX_DIGITS[(b.toInt() ushr 4) and 0x0F])
            result.append(HEX_DIGITS[b.toInt() and 0x0F])

            line[lineIndex++] = b
        }

        if (lineIndex != 16) {
            var count = (16 - lineIndex) * 3
            count++
            repeat(count) { result.append(" ") }

            for (i in 0 until lineIndex) {
                if (line[i] > ' '.code.toByte() && line[i] < '~'.code.toByte()) {
                    result.append(String(line, i, 1))
                } else {
                    result.append(".")
                }
            }
        }

        return result.toString()
    }

    fun toHexString(b: Byte): String {
        return toHexString(toByteArray(b))
    }

    fun toHexString(array: ByteArray): String {
        return toHexString(array, 0, array.size, true)
    }

    fun toHexString(array: ByteArray, upperCase: Boolean): String {
        return toHexString(array, 0, array.size, upperCase)
    }

    fun toHexString(array: ByteArray, offset: Int, length: Int): String {
        return toHexString(array, offset, length, true)
    }

    fun toHexString(array: ByteArray, offset: Int, length: Int, upperCase: Boolean): String {
        val digits = if (upperCase) HEX_DIGITS else HEX_LOWER_CASE_DIGITS
        val buf = CharArray(length * 2)

        var bufIndex = 0
        for (i in offset until offset + length) {
            val b = array[i]
            buf[bufIndex++] = digits[(b.toInt() ushr 4) and 0x0F]
            buf[bufIndex++] = digits[b.toInt() and 0x0F]
        }

        return String(buf)
    }

    fun toHexString(i: Int): String {
        return toHexString(toByteArray(i))
    }

    fun toByteArray(b: Byte): ByteArray {
        return byteArrayOf(b)
    }

    fun toByteArray(i: Int): ByteArray {
        return byteArrayOf(
            (i ushr 24).toByte(),
            (i ushr 16).toByte(),
            (i ushr 8).toByte(),
            i.toByte()
        )
    }

    private fun toByte(c: Char): Int {
        return when (c) {
            in '0'..'9' -> c - '0'
            in 'A'..'F' -> c - 'A' + 10
            in 'a'..'f' -> c - 'a' + 10
            else -> throw RuntimeException("Invalid hex char '$c'")
        }
    }

    fun hexStringToByteArray(hexString: String): ByteArray {
        val length = hexString.length
        val buffer = ByteArray(length / 2)

        var i = 0
        while (i < length) {
            buffer[i / 2] = ((toByte(hexString[i]) shl 4) or toByte(hexString[i + 1])).toByte()
            i += 2
        }

        return buffer
    }

    fun appendByteAsHex(sb: StringBuilder, b: Byte, upperCase: Boolean): StringBuilder {
        val digits = if (upperCase) HEX_DIGITS else HEX_LOWER_CASE_DIGITS
        sb.append(digits[(b.toInt() shr 4) and 0xf])
        sb.append(digits[b.toInt() and 0xf])
        return sb
    }

    fun bytesToInt(src: ByteArray, offset: Int): Int {
        return (src[offset].toInt() and 0xFF) or
                ((src[offset + 1].toInt() and 0xFF) shl 8) or
                ((src[offset + 2].toInt() and 0xFF) shl 16) or
                ((src[offset + 3].toInt() and 0xFF) shl 24)
    }

    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value ushr 8).toByte(),
            (value ushr 16).toByte(),
            (value ushr 24).toByte()
        )
    }

    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。  和bytesToInt2（）配套使用
     */
    fun intToBytes2(value: Int): ByteArray {
        return byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte()
        )
    }

    fun float2byte(num: Float, numbyte: ByteArray) {
        val fbit = java.lang.Float.floatToIntBits(num)

        for (i in 0..3) {
            numbyte[i] = (fbit shr (i * 8)).toByte() // little-endian
        }
    }
}