package com.topdon.ble.util

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

object HexUtil {
    private var inputStream: FileInputStream? = null

    fun bytesToHexString(bArray: ByteArray?): String {
        if (bArray == null || bArray.isEmpty()) return "BYTE IS NULL"
        
        return buildString(bArray.size * 2) {
            bArray.forEach { byte ->
                val hex = Integer.toHexString(0xFF and byte.toInt())
                if (hex.length < 2) append('0')
                append(hex.uppercase())
            }
        }
    }

    fun byteToHex(byte1: Byte): String {
        val hex = Integer.toHexString(0xFF and byte1.toInt())
        return if (hex.length < 2) "0$hex".uppercase() else hex.uppercase()
    }

    fun toByteArray(hexStr: String): ByteArray {
        var s = hexStr.replace("", "")
        if (s.length % 2 != 0) {
            s = "0$s"
        }
        
        val bytes = ByteArray(s.length / 2)
        for (i in bytes.indices) {
            bytes[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }

    fun toByteArray1(hexStr: String): ByteArray {
        var s = hexStr.replace("", "")
        if (s.length % 2 != 0) {
            s = "0$s"
        }
        
        var bytes = ByteArray(s.length / 2)
        for (i in bytes.indices) {
            bytes[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        
        if (bytes.size != 2) {
            val v = bytes[0]
            bytes = ByteArray(2)
            bytes[0] = 0
            bytes[1] = v
        }
        return bytes
    }

    fun getString2HexBytes(src: String): ByteArray {
        val ret = ByteArray(src.length / 2)
        val tmp = src.toByteArray()
        
        for (i in 0 until src.length / 2) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1])
        }
        return ret
    }

    fun hexString2Bytes(src: String): ByteArray {
        val len = src.length / 2
        val ret = ByteArray(len)
        val tmp = src.toByteArray()
        
        for (i in 0 until len) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1])
        }
        return ret
    }

    fun uniteBytes(src0: Byte, src1: Byte): Byte {
        var b0 = String(byteArrayOf(src0)).toInt(16).toByte()
        b0 = (b0.toInt() shl 4).toByte()
        val b1 = String(byteArrayOf(src1)).toInt(16).toByte()
        return (b0.toInt() xor b1.toInt()).toByte()
    }

    fun hexToByte(hex: String): ByteArray {
        val byteLen = hex.length / 2
        val ret = ByteArray(byteLen)
        
        for (i in 0 until byteLen) {
            val m = i * 2 + 1
            val n = m + 1
            val intVal = Integer.decode("0x${hex.substring(i * 2, m)}${hex.substring(m, n)}")
            ret[i] = intVal.toByte()
        }
        return ret
    }

    fun hexToString(bytes: String): String {
        val upperBytes = bytes.uppercase()
        val hexString = "0123456789ABCDEFabcdef"
        val baos = ByteArrayOutputStream(upperBytes.length / 2)
        
        var i = 0
        while (i < upperBytes.length) {
            baos.write((hexString.indexOf(upperBytes[i]) shl 4) or hexString.indexOf(upperBytes[i + 1]))
            i += 2
        }
        return String(baos.toByteArray())
    }

    fun readFileToByteArray(path: String): ByteArray? {
        val file = File(path)
        if (!file.exists()) {
            Log.d("bcf", "File doesn't exist!")
            return null
        }
        
        return try {
            inputStream = FileInputStream(file)
            val inSize = inputStream!!.channel.size()
            if (inSize == 0L) {
                Log.d("bcf", "The FileInputStream has no content!")
                return null
            }
            
            val buffer = ByteArray(inputStream!!.available())
            inputStream!!.read(buffer)
            buffer
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {

            }
        }
    }

    fun byteSub(data: ByteArray, start: Int, length: Int): ByteArray {
        val actualLength = if (start + length > data.size) data.size - start else length
        val bt = ByteArray(actualLength)
        
        for (i in 0 until actualLength) {
            if (i + start < data.size) {
                bt[i] = data[i + start]
            }
        }
        return bt
    }
}