package com.shimmerresearch.driver

interface ShimmerDevice {
    fun connect()
    fun disconnect()
    fun startStreaming()
    fun stopStreaming()
    fun setSamplingRateShimmer(samplingRate: Double)
    fun setEnabledSensors(sensorMask: Int)
    fun isConnected(): Boolean
    fun isStreaming(): Boolean
