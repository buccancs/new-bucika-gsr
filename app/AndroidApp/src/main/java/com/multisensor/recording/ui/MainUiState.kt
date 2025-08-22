package com.multisensor.recording.ui
import android.graphics.Bitmap
import com.multisensor.recording.recording.DeviceStatus
import com.multisensor.recording.util.ThermalColorPalette
import com.multisensor.recording.util.TemperatureRange
data class SessionDisplayInfo(
    val sessionId: String,
    val startTime: Long,
    val duration: Long,
    val deviceCount: Int,
    val recordingMode: String,
    val status: String
)
enum class BatteryStatus {
    CHARGING,
    DISCHARGING,
    FULL,
    LOW,
    UNKNOWN
}
data class ShimmerDeviceInfo(
    val deviceId: String,
    val batteryLevel: Int,
    val signalStrength: Int,
    val isConnected: Boolean,
    val lastDataReceived: Long?
)
data class MainUiState(
    val statusText: String = "Initializing...",
    val isInitialized: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0L,
    val recordingSessionId: String? = null,
    val isReadyToRecord: Boolean = false,
    val isPcConnected: Boolean = false,
    val isShimmerConnected: Boolean = false,
    val isThermalConnected: Boolean = false,
    val isGsrConnected: Boolean = false,
    val isNetworkConnected: Boolean = false,
    val isCameraConnected: Boolean = false,
    val shimmerBatteryLevel: Int = -1,
    val thermalTemperature: Float? = null,
    val networkAddress: String = "",
    val batteryLevel: Int = -1,
    val batteryStatus: BatteryStatus = BatteryStatus.UNKNOWN,
    val shimmerDeviceInfo: ShimmerDeviceInfo? = null,
    val isStreaming: Boolean = false,
    val streamingFrameRate: Double = 0.0,
    val streamingDataSize: Long = 0L,
    val showPermissionsButton: Boolean = false,
    val showManualControls: Boolean = false,
    val isLoadingPermissions: Boolean = false,
    val currentSessionInfo: SessionDisplayInfo? = null,
    val thermalPreviewAvailable: Boolean = false,
    val shimmerDeviceId: String? = null,
    val isCameraCalibrated: Boolean = false,
    val isThermalCalibrated: Boolean = false,
    val isShimmerCalibrated: Boolean = false,
    val isCalibrationRunning: Boolean = false,
    val isCalibratingCamera: Boolean = false,
    val isCalibratingThermal: Boolean = false,
    val isCalibratingShimmer: Boolean = false,
    val isCalibrating: Boolean = false,
    val calibrationComplete: Boolean = false,
    val isPaused: Boolean = false,
    val sessionDuration: String = "00:00:00",
    val currentFileSize: String = "0 MB",
    val storageUsagePercent: Int = 0,
    val totalSessions: Int = 0,
    val totalDataSize: String = "0 MB",
    val hasCurrentSession: Boolean = false,
    val systemHealth: com.multisensor.recording.ui.SystemHealthStatus = com.multisensor.recording.ui.SystemHealthStatus(),
    val isValidating: Boolean = false,
    val isSystemValidated: Boolean = false,
    val isDiagnosticsRunning: Boolean = false,
    val diagnosticsCompleted: Boolean = false,
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val storageUsed: Long = 0L,
    val storageAvailable: Long = 0L,
    val storageTotal: Long = 0L,
    val sessionCount: Int = 0,
    val fileCount: Int = 0,
    val isTransferring: Boolean = false,
    val errorMessage: String? = null,
    val showErrorDialog: Boolean = false,
    val isLoadingRecording: Boolean = false,
    val isLoadingCalibration: Boolean = false,
    val currentThermalFrame: Bitmap? = null,
    val temperatureRange: TemperatureRange = TemperatureRange.BODY_TEMPERATURE,
    val colorPalette: ThermalColorPalette = ThermalColorPalette.IRON,
    val connectedDevices: Map<String, DeviceStatus> = emptyMap()
) {
    val canStartRecording: Boolean
        get() = isInitialized &&
                !isRecording &&
                !isLoadingRecording &&
                isCameraConnected
    val canStopRecording: Boolean
        get() = isRecording && !isLoadingRecording
    val canRunCalibration: Boolean
        get() = isInitialized &&
                !isRecording &&
                !isCalibrationRunning &&
                !isLoadingCalibration
    val systemHealthStatus: SystemHealthStatus
        get() = SystemHealthStatus(
            pcConnection = if (isPcConnected) SystemHealthStatus.HealthStatus.CONNECTED else SystemHealthStatus.HealthStatus.DISCONNECTED,
            shimmerConnection = if (isShimmerConnected) SystemHealthStatus.HealthStatus.CONNECTED else SystemHealthStatus.HealthStatus.DISCONNECTED,
            thermalCamera = if (isThermalConnected) SystemHealthStatus.HealthStatus.CONNECTED else SystemHealthStatus.HealthStatus.DISCONNECTED,
            networkConnection = if (isNetworkConnected) SystemHealthStatus.HealthStatus.CONNECTED else SystemHealthStatus.HealthStatus.DISCONNECTED,
            rgbCamera = if (isCameraConnected) SystemHealthStatus.HealthStatus.CONNECTED else SystemHealthStatus.HealthStatus.DISCONNECTED
        )
    val storageUsagePercentage: Int
        get() = if (storageTotal > 0) {
            ((storageUsed * 100) / storageTotal).toInt()
        } else 0
}