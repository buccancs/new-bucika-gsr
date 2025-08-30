package com.topdon.thermal.frame

/**
 * Watermark bean for thermal images
 */
data class WatermarkBean(
    var isOpen: Boolean = false,
    var title: String = "BucikaGSR",
    var address: String = "",
    var isAddTime: Boolean = false,
) {
    companion object {
        fun loadFromArray(data: ByteArray): WatermarkBean {
            if (data.size < 450) {
                return WatermarkBean()
            }

            val titleLen = ((data[1].toInt() and 0xFF) shl 24) or
                          ((data[2].toInt() and 0xFF) shl 16) or
                          ((data[3].toInt() and 0xFF) shl 8) or
                          (data[4].toInt() and 0xFF)
            
            val titleBytes = if (titleLen > 0 && titleLen < 120) {
                ByteArray(titleLen)
            } else {
                ByteArray(0)
            }
            
            if (titleBytes.isNotEmpty()) {
                System.arraycopy(data, 5, titleBytes, 0, titleBytes.size)
            }

            val addressLen = ((data[125].toInt() and 0xFF) shl 24) or
                           ((data[126].toInt() and 0xFF) shl 16) or
                           ((data[127].toInt() and 0xFF) shl 8) or
                           (data[128].toInt() and 0xFF)
            
            val addressBytes = if (addressLen > 0 && addressLen < 320) {
                ByteArray(addressLen)
            } else {
                ByteArray(0)
            }
            
            if (addressBytes.isNotEmpty()) {
                System.arraycopy(data, 129, addressBytes, 0, addressBytes.size)
            }

            return WatermarkBean(
                isOpen = data[0].toInt() == 1,
                title = if (titleBytes.isEmpty()) "" else String(titleBytes),
                address = if (addressBytes.isEmpty()) "" else String(addressBytes),
                isAddTime = data[449].toInt() == 1
            )
        }
    }

    fun toByteArray(): ByteArray {
        val result = ByteArray(450)

        val titleByteArray = title.toByteArray()
        val addressByteArray = address.toByteArray()

        result[0] = if (isOpen) 1 else 0

        result[1] = (titleByteArray.size ushr 24).toByte()
        result[2] = (titleByteArray.size ushr 16).toByte()
        result[3] = (titleByteArray.size ushr 8).toByte()
        result[4] = titleByteArray.size.toByte()
        System.arraycopy(titleByteArray, 0, result, 5, titleByteArray.size)

        result[125] = (addressByteArray.size ushr 24).toByte()
        result[126] = (addressByteArray.size ushr 16).toByte()
        result[127] = (addressByteArray.size ushr 8).toByte()
        result[128] = addressByteArray.size.toByte()
        System.arraycopy(addressByteArray, 0, result, 129, addressByteArray.size)

        result[449] = if (isAddTime) 1 else 0

        return result
    }
}