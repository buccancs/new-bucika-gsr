package com.multisensor.recording.recording

data class DataQualityMetrics(
    val deviceId: String,
    val samplesAnalyzed: Int,
    val averageSamplingRate: Double,
    val signalQuality: String,
    val batteryLevel: Int,
    val dataLossPercentage: Double,
    val lastSampleTimestamp: Long,
    val connectionStability: String,
    val signalStrength: Int,
    val errorCount: Int
)

data class ShimmerStatus(
    val isAvailable: Boolean,
    val isConnected: Boolean,
    val isRecording: Boolean,
    val samplingRate: Int,
    val batteryLevel: Int? = null,
    val deviceId: String,
    val connectionTime: Long? = null,
    val lastDataReceived: Long? = null
)

data class ShimmerSample(
    val timestamp: Long,
    val systemTime: String,
    val gsrConductance: Double,
    val ppgA13: Double,
    val accelX: Double,
    val accelY: Double,
    val accelZ: Double,
    val gyroX: Double,
    val gyroY: Double,
    val gyroZ: Double,
    val magX: Double,
    val magY: Double,
    val magZ: Double,
    val ecg: Double,
    val emg: Double,
    val batteryVoltage: Double,
    val deviceId: String,
    val sequenceNumber: Long
)

data class DeviceInformation(
    val deviceId: String,
    val macAddress: String,
    val deviceName: String,
    val firmwareVersion: String,
    val hardwareVersion: String,
    val serialNumber: String,
    val batteryLevel: Int,
    val connectionTime: Long,
    val lastSeenTime: Long,
    val deviceType: String,
    val supportedSensors: List<String>,
    val currentConfiguration: Map<String, Any>,
    val capabilities: List<String>
)

data class SensorConfiguration(
    val gsrRange: Int,
    val accelerometerRange: Int,
    val gyroscopeRange: Int,
    val magnetometerRange: Int,
    val samplingRate: Double,
    val enabledSensors: List<String>,
    val calibrationData: Map<String, Double>
)

enum class ShimmerConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STREAMING,
    LOST_CONNECTION,
    RECONNECTING,
    ERROR
}

enum class DataQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL
}
