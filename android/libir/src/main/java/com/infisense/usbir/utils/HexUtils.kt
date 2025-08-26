package com.infisense.usbir.utils

object HexUtils {

    fun binaryToHexString(bytes: ByteArray): String {
        val hexStr = "0123456789ABCDEF"
        val result = StringBuilder()
        for (b in bytes) {
            val hex = "${hexStr[(b.toInt() and 0xF0) shr 4]}${hexStr[b.toInt() and 0x0F]}"
            result.append("$hex ")
        }
        return result.toString()
    }
}
