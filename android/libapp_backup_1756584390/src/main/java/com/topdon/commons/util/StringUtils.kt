package com.topdon.commons.util

import android.text.TextUtils
import androidx.annotation.NonNull
import java.util.Locale
import java.util.UUID

object StringUtils {
    
    @JvmStatic
    fun randomUuid(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
    
    @JvmStatic
    fun fillZero(src: String?, targetLen: Int, head: Boolean): String? {
        if (src == null) return null
        val sb = StringBuilder(src)
        while (sb.length % targetLen != 0) {
            if (head) {
                sb.insert(0, "0")
            } else {
                sb.append("0")
            }
        }
        return sb.toString()
    }
    
    @JvmStatic
    fun toHex(num: Int): String {
        return fillZero(Integer.toHexString(num), 2, true)!!
    }
    
    @JvmStatic
    fun toHex(num: Long): String {
        return fillZero(java.lang.Long.toHexString(num), 2, true)!!
    }
    
    @JvmStatic
    fun toBinary(num: Int): String {
        return fillZero(Integer.toBinaryString(num), 8, true)!!
    }
    
    @JvmStatic
    fun toBinary(num: Long): String {
        return fillZero(java.lang.Long.toBinaryString(num), 8, true)!!
    }
    
    @JvmStatic
    fun toHex(bytes: ByteArray?): String? {
        return toHex(bytes, " ")
    }
    
    @JvmStatic
    fun toHex(bytes: ByteArray?, separator: String): String? {
        return when {
            bytes == null -> null
            bytes.isEmpty() -> ""
            else -> {
                val sb = StringBuilder()
                for (aSrc in bytes) {
                    val v = aSrc.toInt() and 0xFF
                    val hv = Integer.toHexString(v)
                    if (hv.length < 2) {
                        sb.append(0)
                    }
                    sb.append(hv)
                    if (!TextUtils.isEmpty(separator)) {
                        sb.append(separator)
                    }
                }
                var s = sb.toString().uppercase(Locale.ENGLISH)
                if (!TextUtils.isEmpty(separator)) {
                    s = s.substring(0, s.length - separator.length)
                }
                s
            }
        }
    }
    
    @JvmStatic
    fun toBinary(bytes: ByteArray?): String? {
        return toBinary(bytes, " ")
    }
    
    @JvmStatic
    fun toBinary(bytes: ByteArray?, separator: String): String? {
        return when {
            bytes == null -> null
            bytes.isEmpty() -> ""
            else -> {
                val sb = StringBuilder()
                for (aSrc in bytes) {
                    val v = aSrc.toInt() and 0xFF
                    val hv = Integer.toBinaryString(v)
                    val loop = 8 - hv.length
                    repeat(loop) {
                        sb.append(0)
                    }
                    sb.append(hv)
                    if (!TextUtils.isEmpty(separator)) {
                        sb.append(separator)
                    }
                }
                var s = sb.toString()
                if (!TextUtils.isEmpty(separator)) {
                    s = s.substring(0, s.length - separator.length)
                }
                s
            }
        }
    }
    
    @JvmStatic
    fun subZeroAndDot(number: String?): String? {
        if (TextUtils.isEmpty(number)) return number
        var result = number
        if (result!!.contains(".")) {
            result = result.replace("0+?$", "")
            result = result.replace("[.]$", "")
        }
        return result
    }
    
    @JvmStatic
    @NonNull
    fun toDuration(duration: Int): String {
        return toDuration(duration, null)
    }
    
    @JvmStatic
    @NonNull
    fun toDuration(duration: Int, format: String?): String {
        return if (format != null) {
            String.format(Locale.ENGLISH, format, duration / 3600, duration % 3600 / 60, duration % 60)
        } else {
            String.format(Locale.ENGLISH, "%02d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60)
        }
    }
    
    @JvmStatic
    fun toByteArray(hexStr: String, separator: String): ByteArray {
        var s = hexStr.replace(separator, "")
        if (s.length % 2 != 0) {
            s = "0$s"
        }
        val bytes = ByteArray(s.length / 2)
        for (i in bytes.indices) {
            bytes[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }
}