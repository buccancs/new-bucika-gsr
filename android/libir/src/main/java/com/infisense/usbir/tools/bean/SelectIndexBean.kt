package com.infisense.usbir.tools.bean

/**
 * Data class for holding temperature selection indices
 */
data class SelectIndexBean(
    val maxIndex: IntArray,
    val minIndex: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SelectIndexBean
        if (!maxIndex.contentEquals(other.maxIndex)) return false
        if (!minIndex.contentEquals(other.minIndex)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = maxIndex.contentHashCode()
        result = 31 * result + minIndex.contentHashCode()
        return result
    }
}
