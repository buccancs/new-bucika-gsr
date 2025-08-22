package com.multisensor.recording.ui
data class ShimmerConfigUiState(
    val connectedDevices: List<ShimmerDeviceItem> = emptyList(),
    val selectedDeviceIndex: Int = -1,
    val maxSimultaneousDevices: Int = 4,
    val isDeviceConnected: Boolean = false,
    val deviceName: String = "",
    val deviceMacAddress: String = "",
    val connectionStatus: String = "Disconnected",
    val batteryLevel: Int = -1,
    val signalStrength: Int = -1,
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val crcMode: Int = 0,
    val isScanning: Boolean = false,
    val availableDevices: List<ShimmerDeviceItem> = emptyList(),
    val selectedScanDeviceIndex: Int = -1,
    val samplingRate: Int = 512,
    val enabledSensors: Set<String> = emptySet(),
    val isConfiguring: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0L,
    val dataPacketsReceived: Int = 0,
    val devicesRecording: Set<String> = emptySet(),
    val showDeviceList: Boolean = true,
    val showConfigurationPanel: Boolean = false,
    val showRecordingControls: Boolean = false,
    val showMultiDevicePanel: Boolean = false,
    val isLoadingConnection: Boolean = false,
    val isLoadingConfiguration: Boolean = false,
    val isLoadingDeviceInfo: Boolean = false,
    val devicesConnecting: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val showErrorDialog: Boolean = false,
    val deviceErrors: Map<String, String> = emptyMap(),
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothPermission: Boolean = false,
    val autoReconnectEnabled: Boolean = true,
    val devicesForAutoReconnect: List<String> = emptyList(),
    val autoReconnectInProgress: Boolean = false
) {
    val canStartScan: Boolean
        get() = isBluetoothEnabled &&
                hasBluetoothPermission &&
                !isScanning &&
                !isLoadingConnection &&
                connectedDevices.size < maxSimultaneousDevices
    val canConnectToDevice: Boolean
        get() = selectedScanDeviceIndex >= 0 &&
                selectedScanDeviceIndex < availableDevices.size &&
                !isLoadingConnection &&
                connectedDevices.size < maxSimultaneousDevices &&
                !isDeviceAlreadyConnected(availableDevices.getOrNull(selectedScanDeviceIndex)?.macAddress)
    val canDisconnectDevice: Boolean
        get() = selectedDeviceIndex >= 0 &&
                selectedDeviceIndex < connectedDevices.size &&
                !isLoadingConnection
    val canDisconnectAllDevices: Boolean
        get() = connectedDevices.isNotEmpty() && !isLoadingConnection
    val canApplyConfiguration: Boolean
        get() = selectedDeviceIndex >= 0 &&
                selectedDeviceIndex < connectedDevices.size &&
                !isConfiguring &&
                !isRecording &&
                enabledSensors.isNotEmpty()
    val canStartRecording: Boolean
        get() = connectedDevices.isNotEmpty() &&
                !isRecording &&
                !isConfiguring &&
                connectedDevices.any { it.isConnectable }
    val canStopRecording: Boolean
        get() = isRecording
    val connectionHealthStatus: ConnectionHealthStatus
        get() = when {
            !isBluetoothEnabled -> ConnectionHealthStatus.BLUETOOTH_DISABLED
            !hasBluetoothPermission -> ConnectionHealthStatus.NO_PERMISSION
            isLoadingConnection -> ConnectionHealthStatus.CONNECTING
            connectedDevices.isEmpty() -> ConnectionHealthStatus.DISCONNECTED
            connectedDevices.all { it.rssi > -70 } -> ConnectionHealthStatus.EXCELLENT
            connectedDevices.all { it.rssi > -80 } -> ConnectionHealthStatus.GOOD
            else -> ConnectionHealthStatus.POOR
        }
    val selectedDevice: ShimmerDeviceItem?
        get() = if (selectedDeviceIndex >= 0 && selectedDeviceIndex < connectedDevices.size) {
            connectedDevices[selectedDeviceIndex]
        } else null
    val selectedScanDevice: ShimmerDeviceItem?
        get() = if (selectedScanDeviceIndex >= 0 && selectedScanDeviceIndex < availableDevices.size) {
            availableDevices[selectedScanDeviceIndex]
        } else null
    private fun isDeviceAlreadyConnected(macAddress: String?): Boolean {
        return macAddress != null && connectedDevices.any { it.macAddress == macAddress }
    }
    val connectionSummary: String
        get() = when (connectedDevices.size) {
            0 -> "No devices connected"
            1 -> "1 device connected"
            else -> "${connectedDevices.size} devices connected"
        }
    val devicesWithErrors: List<String>
        get() = deviceErrors.keys.toList()
    val hasMultipleDevices: Boolean
        get() = connectedDevices.size > 1
    val canConnectMoreDevices: Boolean
        get() = connectedDevices.size < maxSimultaneousDevices
    val autoReconnectStatus: String
        get() = when {
            autoReconnectInProgress -> "Auto-reconnecting..."
            devicesForAutoReconnect.isEmpty() -> "No devices for auto-reconnect"
            else -> "${devicesForAutoReconnect.size} device(s) available for auto-reconnect"
        }
}
data class ShimmerDeviceItem(
    val name: String,
    val macAddress: String,
    val rssi: Int,
    val isConnectable: Boolean = true,
    val deviceType: String = "Shimmer3",
    val isConnected: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val batteryLevel: Int = -1,
    val connectionPriority: Int = 0,
    val autoReconnectEnabled: Boolean = true,
    val lastConnectedTimestamp: Long = 0L,
    val errorMessage: String? = null
) {
    val displayName: String
        get() = if (isConnected) "$name (Connected)" else name
    val statusWithBattery: String
        get() = if (isConnected && batteryLevel >= 0) {
            "$connectionStatus (Battery: $batteryLevel%)"
        } else {
            connectionStatus
        }
    val signalStrengthDescription: String
        get() = when {
            rssi > -50 -> "Excellent"
            rssi > -70 -> "Good"
            rssi > -80 -> "Fair"
            else -> "Poor"
        }
}
enum class ConnectionHealthStatus {
    BLUETOOTH_DISABLED,
    NO_PERMISSION,
    DISCONNECTED,
    CONNECTING,
    POOR,
    GOOD,
    EXCELLENT
}
enum class ShimmerSensor(val displayName: String, val key: String) {
    ACCELEROMETER("Accelerometer", "accel"),
    GYROSCOPE("Gyroscope", "gyro"),
    MAGNETOMETER("Magnetometer", "mag"),
    GSR("Galvanic Skin Response", "gsr"),
    PPG("Photoplethysmography", "ppg"),
    ECG("Electrocardiogram", "ecg"),
    EMG("Electromyography", "emg"),
    PRESSURE("Pressure", "pressure"),
    TEMPERATURE("Temperature", "temp")
}