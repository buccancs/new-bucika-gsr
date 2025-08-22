package com.shimmerresearch.driver

/**
 * Local implementation of ObjectCluster from official Shimmer SDK
 * Represents a single data packet from the Shimmer device
 */
class ObjectCluster {
    private val formatClusterValues = mutableMapOf<String, FormatClusterValue>()
    
    fun addData(channelName: String, format: String, value: Double, unit: String) {
        formatClusterValues[channelName + "_" + format] = FormatClusterValue(channelName, format, value, unit)
    }
    
    fun getFormatClusterValue(channelName: String, format: String): FormatClusterValue? {
        return formatClusterValues[channelName + "_" + format]
    }
    
    /**
     * Represents a single sensor channel value with metadata
     */
    data class FormatClusterValue(
        val channelName: String,
        val format: String,
        val data: Double,
        val unit: String
    )
}