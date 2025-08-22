package com.multisensor.recording.recording

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class ShimmerDevice(
    val macAddress: String,
    val deviceName: String,
    val deviceId: String = macAddress.takeLast(4),
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    var batteryLevel: Int = 0,
    var firmwareVersion: String = "",
    var hardwareVersion: String = "",
    val sampleCount: AtomicLong = AtomicLong(0),
    val isStreaming: AtomicBoolean = AtomicBoolean(false),
    var lastSampleTime: Long = 0L,
    var reconnectionAttempts: Int = 0,
    var configuration: DeviceConfiguration? = null,
) {
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        STREAMING,
        RECONNECTING,
        ERROR,
    }

    fun getDisplayName(): String =
        if (deviceName.isNotBlank() && deviceName != "Shimmer") {
            "$deviceName ($deviceId)"
        } else {
            "Shimmer $deviceId"
        }

    fun isConnected(): Boolean =
        connectionState in
                listOf(
                    ConnectionState.CONNECTED,
                    ConnectionState.STREAMING,
                )

    fun isActivelyStreaming(): Boolean = connectionState == ConnectionState.STREAMING && isStreaming.get()

    fun updateConnectionState(
        newState: ConnectionState,
        logger: com.multisensor.recording.util.Logger? = null,
    ) {
        val oldState = connectionState
        connectionState = newState

        logger?.debug("Device ${getDisplayName()} state changed: $oldState -> $newState")

        if (newState == ConnectionState.DISCONNECTED) {
            isStreaming.set(false)
            reconnectionAttempts = 0
        }
    }

    fun recordSample() {
        sampleCount.incrementAndGet()
        lastSampleTime = System.currentTimeMillis()
    }

    fun getSamplesPerSecond(): Double {
        val timeSinceLastSample = System.currentTimeMillis() - lastSampleTime
        return if (timeSinceLastSample < 5000 && sampleCount.get() > 0) {
            sampleCount.get()
                .toDouble() / ((System.currentTimeMillis() - (lastSampleTime - timeSinceLastSample)) / 1000.0)
        } else {
            0.0
        }
    }

    fun resetStatistics() {
        sampleCount.set(0)
        lastSampleTime = 0L
        reconnectionAttempts = 0
    }

    override fun toString(): String =
        "ShimmerDevice(${getDisplayName()}, state=$connectionState, samples=${sampleCount.get()}, battery=$batteryLevel%)"
}
