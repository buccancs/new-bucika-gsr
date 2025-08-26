package com.topdon.thermal.events

/**
 * Gallery event classes for thermal image management
 */

/**
 * Event for deleting gallery items
 */
data class GalleryDelEvent(
    val itemId: String,
    val itemType: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Event for adding gallery items  
 */
data class GalleryAddEvent(
    val itemPath: String,
    val itemType: String,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Custom pseudocolor configuration
 */
data class CustomPseudoBean(
    var name: String = "Custom",
    var colorMap: IntArray = intArrayOf(),
    var temperatureRange: FloatArray = floatArrayOf(),
    var isEnabled: Boolean = true,
    var index: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomPseudoBean

        if (name != other.name) return false
        if (!colorMap.contentEquals(other.colorMap)) return false
        if (!temperatureRange.contentEquals(other.temperatureRange)) return false
        if (isEnabled != other.isEnabled) return false
        if (index != other.index) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + colorMap.contentHashCode()
        result = 31 * result + temperatureRange.contentHashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + index
        return result
    }
}