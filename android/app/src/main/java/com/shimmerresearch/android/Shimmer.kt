package com.shimmerresearch.android

import android.os.Handler

/**
 * Local implementation of base Shimmer class from official SDK
 * Base class for all Shimmer device implementations
 */
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
    
    /**
     * Set the message handler for receiving device events
     */
    fun setShimmerMessageHandler(handler: Handler) {
        this.messageHandler = handler
    }
    
    /**
     * Get device name
     */
    fun getDeviceName(): String = deviceName
    
    /**
     * Check if device is connected
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Check if device is streaming data
     */
    fun isStreaming(): Boolean = isStreaming
    
    /**
     * Set sampling rate for the device
     */
    open fun setSamplingRateShimmer(samplingRate: Double) {
        this.samplingRate = samplingRate
    }
    
    /**
     * Set enabled sensors using bitmask
     */
    open fun setEnabledSensors(sensorMask: Int) {
        this.enabledSensors = sensorMask
    }
    
    /**
     * Connect to device - must be implemented by subclasses
     */
    abstract fun connect()
    
    /**
     * Disconnect from device - must be implemented by subclasses
     */
    abstract fun disconnect()
    
    /**
     * Start streaming data - must be implemented by subclasses
     */
    abstract fun startStreaming()
    
    /**
     * Stop streaming data - must be implemented by subclasses
     */
    abstract fun stopStreaming()
