package com.shimmerresearch.android

import android.os.Handler

abstract class Shimmer {
    companion object {
        const val MESSAGE_READ = 1
        const val MESSAGE_WRITE = 2
        const val MESSAGE_DEVICE_NAME = 3
        const val MESSAGE_TOAST = 4
        const val MESSAGE_ACK_RECEIVED = 5
        const val MESSAGE_STOP_STREAMING_COMPLETE = 6
    }
    
    protected var messageHandler: Handler? = null
    protected var deviceName: String = "Shimmer Device"
    protected var isConnected: Boolean = false
    protected var isStreaming: Boolean = false
    protected var samplingRate: Double = 128.0
    protected var enabledSensors: Int = 0
    
    fun setShimmerMessageHandler(handler: Handler) {
        this.messageHandler = handler
    }
    
    fun getDeviceName(): String = deviceName
    
    fun isConnected(): Boolean = isConnected
    
    fun isStreaming(): Boolean = isStreaming
    
    open fun setSamplingRateShimmer(samplingRate: Double) {
        this.samplingRate = samplingRate
    }
    
    open fun setEnabledSensors(sensorMask: Int) {
        this.enabledSensors = sensorMask
    }
    
    abstract fun connect()
    
    abstract fun disconnect()
    
    abstract fun startStreaming()
    
    abstract fun stopStreaming()
