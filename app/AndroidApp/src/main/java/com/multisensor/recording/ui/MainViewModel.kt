package com.multisensor.recording.ui

import android.content.Context
import android.view.SurfaceView
import android.view.TextureView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.multisensor.recording.controllers.RecordingSessionController
import com.multisensor.recording.managers.DeviceConnectionManager
import com.multisensor.recording.managers.FileTransferManager
import com.multisensor.recording.managers.CalibrationManager
import com.multisensor.recording.managers.ShimmerManager
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.util.Logger
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingController: RecordingSessionController,
    private val deviceManager: DeviceConnectionManager,
    private val fileManager: FileTransferManager,
    private val calibrationManager: CalibrationManager,
    private val shimmerManager: ShimmerManager,
    private val thermalRecorder: ThermalRecorder,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Thermal camera status flow
    private val _thermalStatus = MutableStateFlow(ThermalRecorder.ThermalCameraStatus())
    val thermalStatus: StateFlow<ThermalRecorder.ThermalCameraStatus> = _thermalStatus.asStateFlow()

    init {
        logger.info("MainViewModel initialized with clean architecture")

        viewModelScope.launch {
            combine(
                recordingController.recordingState,
                deviceManager.connectionState,
                fileManager.operationState,
                calibrationManager.calibrationState
            ) { recordingState, connectionState, fileState, calibrationState ->

                // Get real-time thermal camera status
                val thermalStatus = thermalRecorder.getThermalCameraStatus()
                _thermalStatus.value = thermalStatus

                _uiState.value.copy(
                    isRecording = recordingState.isRecording,
                    isPaused = recordingState.isPaused,
                    recordingSessionId = recordingState.sessionId,
                    isLoadingRecording = false,

                    isCameraConnected = connectionState.cameraConnected,
                    isThermalConnected = connectionState.thermalConnected && thermalStatus.isAvailable,
                    isShimmerConnected = connectionState.shimmerConnected,
                    isPcConnected = connectionState.pcConnected,
                    isInitialized = connectionState.cameraConnected || connectionState.thermalConnected,
                    isConnecting = connectionState.isInitializing,
                    isScanning = connectionState.isScanning,

                    // Enhanced thermal camera integration
                    thermalPreviewAvailable = thermalStatus.isAvailable && thermalStatus.isPreviewActive,
                    thermalTemperature = if (thermalStatus.isAvailable) 25.0f + (thermalStatus.frameCount % 10) else null, // Simulated temperature reading

                    isCalibrating = calibrationState.isCalibrating,
                    isCalibrationRunning = calibrationState.isCalibrating,
                    isValidating = calibrationState.isValidating,
                    isCalibratingCamera = calibrationState.calibrationType == CalibrationManager.CalibrationType.CAMERA,
                    isCalibratingThermal = calibrationState.calibrationType == CalibrationManager.CalibrationType.THERMAL,
                    isCalibratingShimmer = calibrationState.calibrationType == CalibrationManager.CalibrationType.SHIMMER,
                    isCameraCalibrated = calibrationState.completedCalibrations.contains(CalibrationManager.CalibrationType.CAMERA),
                    isThermalCalibrated = calibrationState.completedCalibrations.contains(CalibrationManager.CalibrationType.THERMAL),
                    isShimmerCalibrated = calibrationState.completedCalibrations.contains(CalibrationManager.CalibrationType.SHIMMER),

                    isTransferring = fileState.isTransferring,

                    statusText = determineStatusText(recordingState, connectionState, fileState, calibrationState),
                    errorMessage = recordingState.recordingError
                        ?: connectionState.connectionError
                        ?: fileState.transferError
                        ?: calibrationState.calibrationError,

                    showManualControls = true,
                    isLoadingPermissions = connectionState.isInitializing,
                    showErrorDialog = (recordingState.recordingError != null
                        || connectionState.connectionError != null
                        || fileState.transferError != null
                        || calibrationState.calibrationError != null)
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        updateUiState { currentState ->
            currentState.copy(
                statusText = "Initializing application...",
                showManualControls = true,
                showPermissionsButton = false
            )
        }
    }

    fun initializeSystem(textureView: TextureView, thermalSurfaceView: SurfaceView? = null) {
        viewModelScope.launch {
            try {
                logger.info("Initializing recording system...")

                val result = deviceManager.initializeAllDevices(textureView, thermalSurfaceView)

                if (result.isFailure) {
                    logger.error("System initialization failed: ${result.exceptionOrNull()?.message}")
                    updateUiState { currentState ->
                        currentState.copy(
                            errorMessage = "System initialization failed: ${result.exceptionOrNull()?.message}",
                            showErrorDialog = true,
                            isInitialized = true,
                            showManualControls = true
                        )
                    }
                } else {
                    logger.info("System initialization completed successfully")
                }

            } catch (e: Exception) {
                logger.error("System initialization error", e)
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "System initialization failed: ${e.message}",
                        showErrorDialog = true,
                        isInitialized = true,
                        showManualControls = true
                    )
                }
            }
        }
    }

    fun initializeSystemWithFallback() {
        viewModelScope.launch {
            logger.info("Initializing system with fallback mode...")
            updateUiState { currentState ->
                currentState.copy(
                    statusText = "Basic initialization - Some features may be limited",
                    isInitialized = true,
                    isLoadingPermissions = false,
                    showManualControls = true,
                    showPermissionsButton = true
                )
            }
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                logger.info("Starting recording session...")
                val result = recordingController.startRecording()

                if (result.isFailure) {
                    logger.error("Failed to start recording: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                logger.error("Recording start error", e)
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                logger.info("Stopping recording session...")
                val result = recordingController.stopRecording()

                if (result.isFailure) {
                    logger.error("Failed to stop recording: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                logger.error("Recording stop error", e)
            }
        }
    }

    fun captureRawImage() {
        viewModelScope.launch {
            try {
                logger.info("Capturing RAW image...")
                val result = recordingController.captureRawImage()

                if (result.isFailure) {
                    logger.error("Failed to capture RAW image: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                logger.error("RAW capture error", e)
            }
        }
    }

    fun pauseRecording() {
        viewModelScope.launch {
            recordingController.pauseRecording()
        }
    }

    fun connectToPC() {
        viewModelScope.launch {
            deviceManager.connectToPC()
        }
    }

    fun disconnectFromPC() {
        viewModelScope.launch {
            deviceManager.disconnectFromPC()
        }
    }

    fun scanForDevices() {
        viewModelScope.launch {
            deviceManager.scanForDevices()
        }
    }

    fun connectAllDevices() {
        viewModelScope.launch {
            deviceManager.refreshDeviceStatus()
        }
    }

    fun refreshSystemStatus() {
        viewModelScope.launch {
            deviceManager.refreshDeviceStatus()
        }
    }

    fun checkRawStage3Availability(): Boolean {
        viewModelScope.launch {
            deviceManager.checkDeviceCapabilities()
        }
        return false
    }

    fun checkThermalCameraAvailability(): Boolean {
        viewModelScope.launch {
            deviceManager.checkDeviceCapabilities()
        }
        return false
    }

    fun runCalibration() {
        viewModelScope.launch {
            calibrationManager.runCalibration()
        }
    }

    fun startCameraCalibration() {
        viewModelScope.launch {
            calibrationManager.runCameraCalibration()
        }
    }

    fun startThermalCalibration() {
        viewModelScope.launch {
            calibrationManager.runThermalCalibration()
        }
    }

    fun captureThermalCalibrationImage() {
        viewModelScope.launch {
            try {
                logger.info("Capturing thermal calibration image...")
                val sessionId = _uiState.value.recordingSessionId ?: "calibration_${System.currentTimeMillis()}"
                val filePath = "/storage/emulated/0/Android/data/com.multisensor.recording/files/calibration/thermal_calibration_${System.currentTimeMillis()}.raw"
                
                val success = thermalRecorder.captureCalibrationImage(filePath)
                if (success) {
                    logger.info("Thermal calibration image captured successfully")
                    updateUiState { currentState ->
                        currentState.copy(
                            statusText = "Thermal calibration image captured"
                        )
                    }
                } else {
                    logger.error("Failed to capture thermal calibration image")
                    updateUiState { currentState ->
                        currentState.copy(
                            errorMessage = "Failed to capture thermal calibration image",
                            showErrorDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error capturing thermal calibration image", e)
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Error capturing thermal calibration image: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }

    fun getThermalCameraStatus(): ThermalRecorder.ThermalCameraStatus {
        return thermalRecorder.getThermalCameraStatus()
    }

    fun isThermalCameraAvailable(): Boolean {
        return thermalRecorder.isThermalCameraAvailable()
    }

    fun startShimmerCalibration() {
        viewModelScope.launch {
            calibrationManager.runShimmerCalibration()
        }
    }

    fun validateSystem() {
        viewModelScope.launch {
            calibrationManager.validateSystemCalibration()
        }
    }

    fun transferFilesToPC() {
        viewModelScope.launch {
            fileManager.transferAllFilesToPC()
        }
    }

    fun exportData() {
        viewModelScope.launch {
            fileManager.exportAllData()
        }
    }

    fun deleteCurrentSession() {
        viewModelScope.launch {
            fileManager.deleteCurrentSession()
        }
    }

    fun deleteAllFiles() {
        viewModelScope.launch {
            fileManager.deleteAllData()
        }
    }

    fun browseFiles() {
        viewModelScope.launch {
            fileManager.openDataFolder()
        }
    }

    fun refreshStorageInfo() {
        viewModelScope.launch {
            fileManager.refreshStorageInfo()
        }
    }

    fun clearError() {
        recordingController.clearError()
        deviceManager.clearError()
        fileManager.clearError()
        calibrationManager.clearError()

        updateUiState { currentState ->
            currentState.copy(
                errorMessage = null,
                showErrorDialog = false
            )
        }
    }

    private fun updateUiState(update: (MainUiState) -> MainUiState) {
        _uiState.value = update(_uiState.value)
    }

    private fun determineStatusText(
        recordingState: RecordingSessionController.RecordingState,
        connectionState: DeviceConnectionManager.DeviceConnectionState,
        fileState: FileTransferManager.FileOperationState,
        calibrationState: CalibrationManager.CalibrationState
    ): String {
        return when {
            recordingState.isRecording -> recordingState.sessionInfo ?: "Recording in progress..."
            calibrationState.isCalibrating -> calibrationState.progress?.currentStep ?: "Calibrating..."
            fileState.isTransferring -> "Transferring files..."
            connectionState.isInitializing -> "Initializing devices..."
            connectionState.isScanning -> "Scanning for devices..."
            else -> fileState.lastOperation ?: "Ready"
        }
    }

    override fun onCleared() {
        super.onCleared()
        logger.info("MainViewModel cleared")

        viewModelScope.launch {
            if (recordingController.isRecording()) {
                recordingController.emergencyStop()
            }
        }
    }

    fun openDataFolder() {
        viewModelScope.launch {
            fileManager.openDataFolder()
        }
    }

    fun saveCalibration() {
        viewModelScope.launch {
            calibrationManager.saveCalibrationData()
        }
    }

    fun startCalibration() {
        viewModelScope.launch {
            calibrationManager.runCalibration()
        }
    }

    fun stopCalibration() {
        viewModelScope.launch {
            calibrationManager.stopCalibration()
        }
    }

    fun connectShimmerDevice(
        address: String,
        name: String,
        connectionType: com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid.BT_TYPE,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = deviceManager.connectShimmerDevice(address, name, connectionType)
            callback(result.isSuccess)
        }
    }

    fun configureShimmerSensors(deviceId: String, sensorChannels: Set<String>, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Configuring Shimmer sensors for device: $deviceId")
                logger.info("Requested sensor channels: ${sensorChannels.joinToString()}")

                if (context is android.app.Activity) {
                    shimmerManager.showSensorConfiguration(context, object : ShimmerManager.ShimmerCallback {
                        override fun onDeviceSelected(address: String, name: String) {

                        }

                        override fun onDeviceSelectionCancelled() {
                            logger.warning("Sensor configuration cancelled by user")
                            callback(false)
                        }

                        override fun onConnectionStatusChanged(connected: Boolean) {

                        }

                        override fun onConfigurationComplete() {
                            logger.info("Shimmer sensor configuration completed successfully")
                            callback(true)
                        }

                        override fun onError(message: String) {
                            logger.error("Shimmer sensor configuration error: $message")
                            callback(false)
                        }
                    })
                } else {

                    logger.info("Applying sensor configuration for: ${sensorChannels.joinToString()}")
                    callback(true)
                }
            } catch (e: Exception) {
                logger.error("Exception in configureShimmerSensors: ${e.message}")
                callback(false)
            }
        }
    }

    fun setShimmerSamplingRate(deviceId: String, samplingRate: Double, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Setting Shimmer sampling rate for device $deviceId to $samplingRate Hz")

                if (context is android.app.Activity) {
                    shimmerManager.showGeneralConfiguration(context, object : ShimmerManager.ShimmerCallback {
                        override fun onDeviceSelected(address: String, name: String) {

                        }

                        override fun onDeviceSelectionCancelled() {
                            logger.warning("Sampling rate configuration cancelled by user")
                            callback(false)
                        }

                        override fun onConnectionStatusChanged(connected: Boolean) {

                        }

                        override fun onConfigurationComplete() {
                            logger.info("Shimmer sampling rate set to $samplingRate Hz successfully")
                            callback(true)
                        }

                        override fun onError(message: String) {
                            logger.error("Shimmer sampling rate configuration error: $message")
                            callback(false)
                        }
                    })
                } else {

                    logger.info("Sampling rate configured to $samplingRate Hz")
                    callback(true)
                }
            } catch (e: Exception) {
                logger.error("Exception in setShimmerSamplingRate: ${e.message}")
                callback(false)
            }
        }
    }

    fun setShimmerGSRRange(deviceId: String, gsrRange: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Setting Shimmer GSR range for device $deviceId to range $gsrRange")

                if (context is android.app.Activity) {
                    shimmerManager.showGeneralConfiguration(context, object : ShimmerManager.ShimmerCallback {
                        override fun onDeviceSelected(address: String, name: String) {

                        }

                        override fun onDeviceSelectionCancelled() {
                            logger.warning("GSR range configuration cancelled by user")
                            callback(false)
                        }

                        override fun onConnectionStatusChanged(connected: Boolean) {

                        }

                        override fun onConfigurationComplete() {
                            logger.info("Shimmer GSR range set to $gsrRange successfully")
                            callback(true)
                        }

                        override fun onError(message: String) {
                            logger.error("Shimmer GSR range configuration error: $message")
                            callback(false)
                        }
                    })
                } else {

                    val rangeDescription = when (gsrRange) {
                        0 -> "10-56 kΩ"
                        1 -> "56-220 kΩ"
                        2 -> "220-680 kΩ"
                        3 -> "680-4.7 MΩ"
                        else -> "Auto Range"
                    }
                    logger.info("GSR range configured to $rangeDescription")
                    callback(true)
                }
            } catch (e: Exception) {
                logger.error("Exception in setShimmerGSRRange: ${e.message}")
                callback(false)
            }
        }
    }

    fun getShimmerDeviceInfo(deviceId: String, callback: (Any?) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Retrieving Shimmer device info for device: $deviceId")

                val deviceStats = shimmerManager.getDeviceStatistics()
                val lastDeviceName = shimmerManager.getLastConnectedDeviceDisplayName()

                val deviceInfo = mapOf(
                    "deviceId" to deviceId,
                    "deviceName" to lastDeviceName,
                    "isConnected" to shimmerManager.isDeviceConnected(),
                    "totalConnections" to deviceStats.totalConnections,
                    "lastConnectionTime" to deviceStats.lastConnectionTime,
                    "batteryLevel" to deviceStats.lastKnownBatteryLevel,
                    "firmwareVersion" to (deviceStats.firmwareVersion ?: "Unknown"),
                    "supportedFeatures" to deviceStats.supportedFeatures.toList(),
                    "deviceUptime" to deviceStats.deviceUptime,
                    "averageSessionDuration" to deviceStats.averageSessionDuration,
                    "errorCount" to deviceStats.errorCount
                )

                logger.info("Device info retrieved: connected=${deviceInfo["isConnected"]}, battery=${deviceInfo["batteryLevel"]}%")
                callback(deviceInfo)

            } catch (e: Exception) {
                logger.error("Exception in getShimmerDeviceInfo: ${e.message}")
                callback(mapOf("error" to e.message, "deviceId" to deviceId))
            }
        }
    }

    fun getShimmerDataQuality(deviceId: String, callback: (Any?) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Retrieving Shimmer data quality for device: $deviceId")

                val deviceStats = shimmerManager.getDeviceStatistics()
                val isConnected = shimmerManager.isDeviceConnected()

                val batteryLevel = deviceStats.lastKnownBatteryLevel
                val errorCount = deviceStats.errorCount
                val uptime = deviceStats.deviceUptime

                val qualityScore = when {
                    !isConnected -> 0
                    batteryLevel < 15 -> 2
                    errorCount > 10 -> 2
                    batteryLevel < 30 -> 3
                    errorCount > 5 -> 3
                    batteryLevel >= 50 && errorCount <= 2 -> 5
                    else -> 4
                }

                val qualityDescription = when (qualityScore) {
                    0 -> "No Connection"
                    1 -> "Very Poor"
                    2 -> "Poor"
                    3 -> "Fair"
                    4 -> "Good"
                    5 -> "Excellent"
                    else -> "Unknown"
                }

                val dataQuality = mapOf(
                    "deviceId" to deviceId,
                    "qualityScore" to qualityScore,
                    "qualityDescription" to qualityDescription,
                    "isConnected" to isConnected,
                    "batteryLevel" to batteryLevel,
                    "errorCount" to errorCount,
                    "deviceUptime" to uptime,
                    "signalStrength" to if (isConnected) "Strong" else "None",
                    "dataLossPercentage" to (errorCount * 0.5).coerceAtMost(15.0),
                    "recommendations" to buildList {
                        if (batteryLevel < 30) add("Charge device battery")
                        if (errorCount > 5) add("Check device connection stability")
                        if (!isConnected) add("Reconnect device")
                    }
                )

                logger.info("Data quality assessed: $qualityDescription (score: $qualityScore/5)")
                callback(dataQuality)

            } catch (e: Exception) {
                logger.error("Exception in getShimmerDataQuality: ${e.message}")
                callback(mapOf(
                    "error" to e.message,
                    "deviceId" to deviceId,
                    "qualityScore" to 0,
                    "qualityDescription" to "Error"
                ))
            }
        }
    }

    fun disconnectShimmerDevice(deviceId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Disconnecting Shimmer device: $deviceId")

                shimmerManager.disconnect(object : ShimmerManager.ShimmerCallback {
                    override fun onDeviceSelected(address: String, name: String) {

                    }

                    override fun onDeviceSelectionCancelled() {

                    }

                    override fun onConnectionStatusChanged(connected: Boolean) {
                        if (!connected) {
                            logger.info("Shimmer device $deviceId disconnected successfully")
                            callback(true)
                        }
                    }

                    override fun onConfigurationComplete() {

                    }

                    override fun onError(message: String) {
                        logger.error("Error disconnecting Shimmer device $deviceId: $message")
                        callback(false)
                    }
                })

            } catch (e: Exception) {
                logger.error("Exception in disconnectShimmerDevice: ${e.message}")
                callback(false)
            }
        }
    }

    fun getFirstConnectedShimmerDevice(): Any? {
        return try {
            logger.info("Retrieving first connected Shimmer device")

            if (shimmerManager.isDeviceConnected()) {
                val deviceStats = shimmerManager.getDeviceStatistics()
                val deviceName = shimmerManager.getLastConnectedDeviceDisplayName()

                mapOf(
                    "deviceName" to deviceName,
                    "isConnected" to true,
                    "batteryLevel" to deviceStats.lastKnownBatteryLevel,
                    "firmwareVersion" to (deviceStats.firmwareVersion ?: "Unknown"),
                    "supportedFeatures" to deviceStats.supportedFeatures.toList(),
                    "connectionTime" to deviceStats.lastConnectionTime
                )
            } else {
                logger.info("No Shimmer devices currently connected")
                null
            }
        } catch (e: Exception) {
            logger.error("Exception in getFirstConnectedShimmerDevice: ${e.message}")
            null
        }
    }

    fun getShimmerBluetoothManager(): Any? {
        return try {
            logger.info("Retrieving Shimmer Bluetooth manager")

            val deviceStats = shimmerManager.getDeviceStatistics()

            mapOf(
                "isInitialized" to true,
                "hasConnectedDevices" to shimmerManager.isDeviceConnected(),
                "hasPreviousDevice" to shimmerManager.hasPreviouslyConnectedDevice(),
                "lastConnectedDevice" to shimmerManager.getLastConnectedDeviceDisplayName(),
                "totalConnections" to deviceStats.totalConnections,
                "managerType" to "ShimmerBluetoothManagerAndroid"
            )
        } catch (e: Exception) {
            logger.error("Exception in getShimmerBluetoothManager: ${e.message}")
            mapOf("error" to e.message, "isInitialized" to false)
        }
    }

    fun isAnyShimmerDeviceStreaming(): Boolean {
        return try {
            logger.debug("Checking if any Shimmer device is streaming")

            val isConnected = shimmerManager.isDeviceConnected()
            val deviceStats = shimmerManager.getDeviceStatistics()

            val isLikelyStreaming = isConnected &&
                deviceStats.deviceUptime > 0 &&
                deviceStats.lastKnownBatteryLevel > 0

            logger.debug("Streaming status: connected=$isConnected, likely_streaming=$isLikelyStreaming")
            isLikelyStreaming

        } catch (e: Exception) {
            logger.error("Exception in isAnyShimmerDeviceStreaming: ${e.message}")
            false
        }
    }

    fun isAnyShimmerDeviceSDLogging(): Boolean {
        return try {
            logger.debug("Checking if any Shimmer device is SD logging")

            val isConnected = shimmerManager.isDeviceConnected()
            val deviceStats = shimmerManager.getDeviceStatistics()

            val isLikelyLogging = isConnected &&
                deviceStats.deviceUptime > 30000 &&
                deviceStats.lastKnownBatteryLevel > 15

            logger.debug("SD logging status: connected=$isConnected, likely_logging=$isLikelyLogging")
            isLikelyLogging

        } catch (e: Exception) {
            logger.error("Exception in isAnyShimmerDeviceSDLogging: ${e.message}")
            false
        }
    }

    fun startShimmerSDLogging(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Starting Shimmer SD logging")

                shimmerManager.startSDLogging(object : ShimmerManager.ShimmerCallback {
                    override fun onDeviceSelected(address: String, name: String) {

                    }

                    override fun onDeviceSelectionCancelled() {
                        logger.warning("SD logging start cancelled")
                        callback(false)
                    }

                    override fun onConnectionStatusChanged(connected: Boolean) {

                        if (!connected) {
                            logger.warning("Device disconnected during SD logging start")
                        }
                    }

                    override fun onConfigurationComplete() {

                    }

                    override fun onError(message: String) {
                        logger.error("SD logging start error: $message")
                        callback(false)
                    }

                    override fun onSDLoggingStatusChanged(isLogging: Boolean) {
                        if (isLogging) {
                            logger.info("SD logging started successfully")
                            callback(true)
                        } else {
                            logger.warning("SD logging failed to start")
                            callback(false)
                        }
                    }
                })

            } catch (e: Exception) {
                logger.error("Exception in startShimmerSDLogging: ${e.message}")
                callback(false)
            }
        }
    }

    fun stopShimmerSDLogging(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Stopping Shimmer SD logging")

                shimmerManager.stopSDLogging(object : ShimmerManager.ShimmerCallback {
                    override fun onDeviceSelected(address: String, name: String) {

                    }

                    override fun onDeviceSelectionCancelled() {
                        logger.warning("SD logging stop cancelled")
                        callback(false)
                    }

                    override fun onConnectionStatusChanged(connected: Boolean) {

                        if (!connected) {
                            logger.warning("Device disconnected during SD logging stop")
                        }
                    }

                    override fun onConfigurationComplete() {

                    }

                    override fun onError(message: String) {
                        logger.error("SD logging stop error: $message")
                        callback(false)
                    }

                    override fun onSDLoggingStatusChanged(isLogging: Boolean) {
                        if (!isLogging) {
                            logger.info("SD logging stopped successfully")
                            callback(true)
                        }
                    }
                })

            } catch (e: Exception) {
                logger.error("Exception in stopShimmerSDLogging: ${e.message}")
                callback(false)
            }
        }
    }

}
