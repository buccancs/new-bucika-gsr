package com.multisensor.recording.recording
enum class DeviceStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR;
    val isWorking: Boolean get() = this == CONNECTED
    val isTransitional: Boolean get() = this == CONNECTING
    val hasIssue: Boolean get() = this == DISCONNECTED || this == ERROR
    val displayText: String get() = when (this) {
        CONNECTED -> "Connected"
        DISCONNECTED -> "Disconnected"
        CONNECTING -> "Connecting..."
        ERROR -> "Error"
    }
}