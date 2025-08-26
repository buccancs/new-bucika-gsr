package com.shimmerresearch.driver

class ObjectCluster {
    private val formatClusterValues = mutableMapOf<String, FormatClusterValue>()
    private var timestamp: Long = System.currentTimeMillis()
    
    fun addData(channelName: String, format: String, value: Double, unit: String) {
        formatClusterValues[channelName + "_" + format] = FormatClusterValue(channelName, format, value, unit)
    }
    
    fun addFormatClusterValue(channelName: String, format: String, unit: String, value: Double) {
        formatClusterValues[channelName + "_" + format] = FormatClusterValue(channelName, format, value, unit)
    }
    
    fun getFormatClusterValue(channelName: String, format: String): FormatClusterValue? {
        return formatClusterValues[channelName + "_" + format]
    }
    
    fun getChannelNames(): Set<String> {
        return formatClusterValues.values.map { it.channelName }.toSet()
    }
    
    fun getAllFormatClusterValues(): Map<String, FormatClusterValue> {
        return formatClusterValues.toMap()
    }
    
    fun setTimestamp(timestamp: Long) {
        this.timestamp = timestamp
    }
    
    fun getTimestamp(): Long = timestamp
    
    fun containsChannel(channelName: String, format: String): Boolean {
        return formatClusterValues.containsKey(channelName + "_" + format)
    }
    
    fun getValue(channelName: String, format: String): Double? {
        return formatClusterValues[channelName + "_" + format]?.data
    }
    
    fun getUnit(channelName: String, format: String): String? {
        return formatClusterValues[channelName + "_" + format]?.unit
    }
    
    data class FormatClusterValue(
        val channelName: String,
        val format: String,
        val data: Double,
        val unit: String
    )
