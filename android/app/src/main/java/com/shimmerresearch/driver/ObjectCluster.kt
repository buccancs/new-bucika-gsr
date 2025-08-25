package com.shimmerresearch.driver

/**
 * Local implementation of ObjectCluster from official Shimmer SDK
 * Represents a single data packet from the Shimmer device
 */
class ObjectCluster {
    private val formatClusterValues = mutableMapOf<String, FormatClusterValue>()
    private var timestamp: Long = System.currentTimeMillis()
    
    /**
     * Add sensor data with channel name, format, value and unit
     */
    fun addData(channelName: String, format: String, value: Double, unit: String) {
        formatClusterValues[channelName + "_" + format] = FormatClusterValue(channelName, format, value, unit)
    }
    
    /**
     * Add formatted cluster value (alternative method used by ShimmerBluetooth)
     */
    fun addFormatClusterValue(channelName: String, format: String, unit: String, value: Double) {
        formatClusterValues[channelName + "_" + format] = FormatClusterValue(channelName, format, value, unit)
    }
    
    /**
     * Get formatted cluster value by channel name and format
     */
    fun getFormatClusterValue(channelName: String, format: String): FormatClusterValue? {
        return formatClusterValues[channelName + "_" + format]
    }
    
    /**
     * Get all available channel names
     */
    fun getChannelNames(): Set<String> {
        return formatClusterValues.values.map { it.channelName }.toSet()
    }
    
    /**
     * Get all format cluster values
     */
    fun getAllFormatClusterValues(): Map<String, FormatClusterValue> {
        return formatClusterValues.toMap()
    }
    
    /**
     * Set timestamp for this data packet
     */
    fun setTimestamp(timestamp: Long) {
        this.timestamp = timestamp
    }
    
    /**
     * Get timestamp of this data packet
     */
    fun getTimestamp(): Long = timestamp
    
    /**
     * Check if channel exists with specified format
     */
    fun containsChannel(channelName: String, format: String): Boolean {
        return formatClusterValues.containsKey(channelName + "_" + format)
    }
    
    /**
     * Get value directly by channel name and format
     */
    fun getValue(channelName: String, format: String): Double? {
        return formatClusterValues[channelName + "_" + format]?.data
    }
    
    /**
     * Get unit directly by channel name and format
     */
    fun getUnit(channelName: String, format: String): String? {
        return formatClusterValues[channelName + "_" + format]?.unit
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
