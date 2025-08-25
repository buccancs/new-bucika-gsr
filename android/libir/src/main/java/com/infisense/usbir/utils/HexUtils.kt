package com.infisense.usbir.utils

/**
 * Created by fengjibo on 2022/12/9.
 */
object HexUtils {

    /**
     * 将字节数组转换成十六进制的字符串
     * @return
     */
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