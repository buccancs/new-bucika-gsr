package com.shimmerresearch.driver

/**
 * Local implementation of ShimmerDevice interface from official SDK
 * Base interface for all Shimmer devices
 */
interface ShimmerDevice {
    fun connect()
    fun disconnect()
    fun startStreaming()
    fun stopStreaming()
    fun setSamplingRateShimmer(samplingRate: Double)
    fun setEnabledSensors(sensorMask: Int)
    fun isConnected(): Boolean
    fun isStreaming(): Boolean
}