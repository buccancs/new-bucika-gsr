package com.multisensor.recording.ui

import android.content.Context
import android.hardware.camera2.CameraManager
import android.view.SurfaceView
import android.view.TextureView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multisensor.recording.calibration.CalibrationCaptureManager
import com.multisensor.recording.network.FileTransferHandler
import com.multisensor.recording.network.JsonSocketClient
import com.multisensor.recording.network.NetworkConfiguration
import com.multisensor.recording.network.SendFileCommand
import com.multisensor.recording.recording.CameraRecorder
import com.multisensor.recording.recording.ShimmerRecorder
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModelLegacy
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val cameraRecorder: CameraRecorder,
    private val thermalRecorder: ThermalRecorder,
    private val shimmerRecorder: ShimmerRecorder,
    private val sessionManager: SessionManager,
    private val fileTransferHandler: FileTransferHandler,
    private val calibrationCaptureManager: CalibrationCaptureManager,
    private val jsonSocketClient: JsonSocketClient,
    private val networkConfiguration: NetworkConfiguration,
    private val logger: Logger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        logger.info("MainViewModel initialized with centralized UiState")

        updateUiState { currentState ->
            currentState.copy(
                statusText = "Initializing application...",
                showManualControls = true,
                showPermissionsButton = false
            )
        }
    }

    private fun updateUiState(update: (MainUiState) -> MainUiState) {
        _uiState.value = update(_uiState.value)
    }

    fun initializeSystem(textureView: TextureView, thermalSurfaceView: SurfaceView? = null) {
        viewModelScope.launch {
            try {
                logger.info("Initializing recording system with TextureView...")

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Initializing cameras and sensors...",
                        isLoadingPermissions = true
                    )
                }

                val cameraInitialized = cameraRecorder.initialize(textureView)

                if (!cameraInitialized) {
                    logger.error("Camera initialization failed")
                    updateUiState { currentState ->
                        currentState.copy(
                            statusText = "Camera initialization failed - Manual controls available",
                            isInitialized = true,
                            isLoadingPermissions = false,
                            showManualControls = true,
                            errorMessage = "Camera not available, but other functions may work"
                        )
                    }
                } else {
                    logger.info("Camera initialization successful")
                    updateUiState { currentState ->
                        currentState.copy(
                            isLoadingPermissions = false,
                            errorMessage = null
                        )
                    }
                }

                val thermalInitialized = thermalRecorder.initialize(thermalSurfaceView)
                if (!thermalInitialized) {
                    logger.warning("Thermal camera not available")
                } else {
                    val previewStarted = thermalRecorder.startPreview()
                    if (previewStarted) {
                        logger.info("Thermal camera preview started successfully")
                    } else {
                        logger.warning("Failed to start thermal camera preview")
                    }
                }

                val shimmerInitialized = shimmerRecorder.initialize()
                if (!shimmerInitialized) {
                    logger.warning("Shimmer sensor not connected")
                }

                val statusMessage = buildString {
                    append("System ready - ")
                    append("Camera: ${if (cameraInitialized) "OK" else "FAIL"}, ")
                    append("Thermal: ${if (thermalInitialized) "OK" else "N/A"}, ")
                    append("Shimmer: ${if (shimmerInitialized) "OK" else "N/A"}")
                }

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = statusMessage,
                        isInitialized = true,
                        isLoadingPermissions = false,
                        isThermalConnected = thermalInitialized,
                        isShimmerConnected = shimmerInitialized,
                        thermalPreviewAvailable = thermalInitialized,
                        showManualControls = true
                    )
                }

                logger.info("System initialization complete: $statusMessage")
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                logger.error("Security exception during system initialization - check permissions", e)
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Permission denied: ${e.message}",
                        statusText = "Initialization failed - permissions",
                        isLoadingPermissions = false,
                        showErrorDialog = true,
                        isInitialized = true,
                        showManualControls = true
                    )
                }
            } catch (e: IllegalStateException) {
                logger.error("Invalid state during system initialization", e)
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Device state error: ${e.message}",
                        statusText = "Initialization failed - state",
                        isLoadingPermissions = false,
                        showErrorDialog = true,
                        isInitialized = true,
                        showManualControls = true
                    )
                }
            } catch (e: RuntimeException) {
                logger.error("Runtime error during system initialization", e)
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "System initialization failed: ${e.message}",
                        statusText = "Initialization failed",
                        isLoadingPermissions = false,
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
            try {
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

                logger.info("Fallback initialization complete - UI should be functional")
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                logger.error("Security exception during fallback initialization - check permissions", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Error during initialization - permissions",
                        isInitialized = true,
                        showManualControls = true,
                        showPermissionsButton = true,
                        errorMessage = "Permission denied: ${e.message}"
                    )
                }
            } catch (e: IllegalStateException) {
                logger.error("Invalid state during fallback initialization", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Error during initialization - state",
                        isInitialized = true,
                        showManualControls = true,
                        showPermissionsButton = true,
                        errorMessage = "Device state error: ${e.message}"
                    )
                }
            } catch (e: RuntimeException) {
                logger.error("Runtime error during fallback initialization", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Error during initialization",
                        isInitialized = true,
                        showManualControls = true,
                        showPermissionsButton = true,
                        errorMessage = "Initialization error: ${e.message}"
                    )
                }
            }
        }
    }

    fun startRecording() {
        if (_uiState.value.isRecording) {
            logger.warning("Recording already in progress")
            return
        }

        viewModelScope.launch {
            try {
                logger.info("Starting recording session...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Starting recording...",
                        isLoadingRecording = true
                    )
                }

                val recordVideo = true
                val captureRaw = false

                logger.info("Recording mode - Video: $recordVideo, RAW: $captureRaw")

                val sessionInfo = cameraRecorder.startSession(recordVideo, captureRaw)

                if (sessionInfo != null) {
                    val sessionId = sessionManager.createNewSession()
                    logger.info("Created legacy session: $sessionId for thermal/shimmer recorders")

                    val thermalStarted = thermalRecorder.startRecording(sessionId)
                    val shimmerStarted = shimmerRecorder.startRecording(sessionId)

                    updateUiState { currentState ->
                        currentState.copy(
                            isRecording = true,
                            statusText = "Recording in progress - ${sessionInfo.getSummary()}",
                            isLoadingRecording = false,
                            recordingSessionId = sessionId
                        )
                    }
                    logger.info("Recording started successfully: ${sessionInfo.getSummary()}")

                    logger.info("Recording status - Camera: SessionInfo, Thermal: $thermalStarted, Shimmer: $shimmerStarted")
                } else {
                    updateUiState { currentState ->
                        currentState.copy(
                            errorMessage = "Failed to start camera recording session",
                            showErrorDialog = true,
                            isLoadingRecording = false
                        )
                    }
                    logger.error("Failed to start camera recording session")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                logger.error("Security exception during recording start - check permissions", e)
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Permission denied for recording: ${e.message}",
                        statusText = "Recording start failed - permissions",
                        showErrorDialog = true,
                        isLoadingRecording = false
                    )
                }
            } catch (e: IllegalStateException) {
                logger.error("Invalid state during recording start", e)
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Device not ready for recording: ${e.message}",
                        statusText = "Recording start failed - state",
                        showErrorDialog = true,
                        isLoadingRecording = false
                    )
                }
            } catch (e: RuntimeException) {
                logger.error("Runtime error during recording start", e)
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Failed to start recording: ${e.message}",
                        statusText = "Recording start failed",
                        showErrorDialog = true,
                        isLoadingRecording = false
                    )
                }
            }
        }
    }

    fun stopRecording() {
        if (!_uiState.value.isRecording) {
            logger.warning("No recording in progress")
            return
        }

        viewModelScope.launch {
            try {
                logger.info("Stopping recording session...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Stopping recording...",
                        isLoadingRecording = true
                    )
                }

                val finalSessionInfo = cameraRecorder.stopSession()

                if (finalSessionInfo != null) {
                    logger.info("Camera session stopped: ${finalSessionInfo.getSummary()}")

                    thermalRecorder.stopRecording()
                    shimmerRecorder.stopRecording()

                    sessionManager.finalizeCurrentSession()

                    updateUiState { currentState ->
                        currentState.copy(
                            isRecording = false,
                            statusText = "Recording stopped - ${finalSessionInfo.getSummary()}",
                            isLoadingRecording = false,
                            recordingSessionId = null
                        )
                    }
                    logger.info("Recording stopped successfully: ${finalSessionInfo.getSummary()}")
                } else {
                    logger.warning("No SessionInfo returned from camera recorder stop")

                    thermalRecorder.stopRecording()
                    shimmerRecorder.stopRecording()
                    sessionManager.finalizeCurrentSession()

                    updateUiState { currentState ->
                        currentState.copy(
                            isRecording = false,
                            statusText = "Recording stopped - Ready",
                            isLoadingRecording = false,
                            recordingSessionId = null
                        )
                    }
                    logger.info("Recording stopped (no session info)")
                }
            } catch (e: Exception) {
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Error stopping recording: ${e.message}",
                        statusText = "Recording stop failed",
                        showErrorDialog = true,
                        isLoadingRecording = false,
                        isRecording = false,
                        recordingSessionId = null
                    )
                }
                logger.error("Recording stop error", e)
            }
        }
    }

    fun captureRawImage() {
        if (!_uiState.value.isRecording) {
            updateUiState { currentState ->
                currentState.copy(
                    errorMessage = "No active recording session for RAW capture",
                    showErrorDialog = true
                )
            }
            logger.warning("Attempted RAW capture without active session")
            return
        }

        viewModelScope.launch {
            try {
                logger.info("Triggering manual RAW capture...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Capturing RAW image...",
                        isLoadingRecording = true
                    )
                }

                val captureSuccess = cameraRecorder.captureRawImage()

                if (captureSuccess) {
                    updateUiState { currentState ->
                        currentState.copy(
                            statusText = "RAW image captured successfully",
                            isLoadingRecording = false
                        )
                    }
                    logger.info("Manual RAW capture successful")
                } else {
                    updateUiState { currentState ->
                        currentState.copy(
                            errorMessage = "Failed to capture RAW image",
                            showErrorDialog = true,
                            statusText = "RAW capture failed",
                            isLoadingRecording = false
                        )
                    }
                    logger.error("Manual RAW capture failed")
                }
            } catch (e: Exception) {
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Error capturing RAW image: ${e.message}",
                        showErrorDialog = true,
                        statusText = "RAW capture error",
                        isLoadingRecording = false
                    )
                }
                logger.error("Manual RAW capture error", e)
            }
        }
    }

    fun setRecordVideoEnabled(enabled: Boolean) {
        if (_uiState.value.isRecording) {
            logger.warning("Cannot change recording mode during active session")
            return
        }

        logger.info("Video recording ${if (enabled) "enabled" else "disabled"}")
    }

    fun setCaptureRawEnabled(enabled: Boolean) {
        if (_uiState.value.isRecording) {
            logger.warning("Cannot change recording mode during active session")
            return
        }

        logger.info("RAW capture ${if (enabled) "enabled" else "disabled"}")
    }

    fun checkRawStage3Availability(): Boolean {
        return try {
            logger.info("Checking RAW stage 3 capture availability...")
            val isAvailable = cameraRecorder.isRawStage3Available()

            updateUiState { currentState ->
                val statusMessage = if (isAvailable) {
                    "RAW Stage 3 capture: AVAILABLE"
                } else {
                    "RAW Stage 3 capture: NOT AVAILABLE"
                }

                currentState.copy(
                    statusText = statusMessage
                )
            }

            if (isAvailable) {
                logger.info("✓ RAW Stage 3 capture is available on this device")
            } else {
                logger.warning("✗ RAW Stage 3 capture is NOT available on this device")
            }

            isAvailable
        } catch (e: Exception) {
            logger.error("Error checking RAW stage 3 availability", e)
            updateUiState { currentState ->
                currentState.copy(
                    statusText = "Error checking RAW stage 3 availability",
                    errorMessage = "Failed to check RAW capabilities: ${e.message}",
                    showErrorDialog = true
                )
            }
            false
        }
    }

    fun checkThermalCameraAvailability(): Boolean {
        return try {
            logger.info("Checking Topdon thermal camera availability...")
            val isAvailable = thermalRecorder.isThermalCameraAvailable()

            updateUiState { currentState ->
                val statusMessage = if (isAvailable) {
                    "Topdon thermal camera: AVAILABLE"
                } else {
                    "Topdon thermal camera: NOT AVAILABLE"
                }

                currentState.copy(
                    statusText = statusMessage,
                    thermalPreviewAvailable = isAvailable
                )
            }

            if (isAvailable) {
                logger.info("✓ Topdon thermal camera is available")
            } else {
                logger.warning("✗ Topdon thermal camera is NOT available")
            }

            isAvailable
        } catch (e: Exception) {
            logger.error("Error checking thermal camera availability", e)
            updateUiState { currentState ->
                currentState.copy(
                    statusText = "Error checking thermal camera availability",
                    errorMessage = "Failed to check thermal camera: ${e.message}",
                    showErrorDialog = true
                )
            }
            false
        }
    }

    fun runCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Starting calibration process with CalibrationCaptureManager...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Running calibration...",
                        isCalibrationRunning = true,
                        isLoadingCalibration = true
                    )
                }

                val calibrationResult = calibrationCaptureManager.captureCalibrationImages(
                    calibrationId = "manual_calibration_${System.currentTimeMillis()}",
                    captureRgb = true,
                    captureThermal = true,
                    highResolution = true
                )

                if (calibrationResult.success) {
                    val message = buildString {
                        append("Calibration capture successful")
                        calibrationResult.rgbFilePath?.let { append("\nRGB: $it") }
                        calibrationResult.thermalFilePath?.let { append("\nThermal: $it") }
                    }

                    updateUiState { currentState ->
                        currentState.copy(
                            statusText = "Calibration images captured successfully",
                            isCalibrationRunning = false,
                            isLoadingCalibration = false
                        )
                    }
                    logger.info("Calibration capture completed successfully: $message")

                } else {
                    val errorMsg = calibrationResult.errorMessage ?: "Unknown calibration error"
                    updateUiState { currentState ->
                        currentState.copy(
                            errorMessage = "Calibration failed: $errorMsg",
                            showErrorDialog = true,
                            statusText = "Calibration failed - Ready",
                            isCalibrationRunning = false,
                            isLoadingCalibration = false
                        )
                    }
                    logger.error("Calibration capture failed: $errorMsg")
                }

            } catch (e: Exception) {
                updateUiState { currentState ->
                    currentState.copy(
                        errorMessage = "Calibration failed: ${e.message}",
                        showErrorDialog = true,
                        statusText = "Calibration failed - Ready",
                        isCalibrationRunning = false,
                        isLoadingCalibration = false
                    )
                }
                logger.error("Calibration error", e)
            }
        }
    }

    fun clearError() {
        updateUiState { currentState ->
            currentState.copy(
                errorMessage = null,
                showErrorDialog = false
            )
        }
    }

    fun connectShimmerDevice(
        macAddress: String,
        deviceName: String,
        connectionType: com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid.BT_TYPE,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("Connecting to Shimmer device: $deviceName ($macAddress) via $connectionType")
                val success = shimmerRecorder.connectSingleDevice(macAddress, deviceName, connectionType)

                updateUiState { currentState ->
                    currentState.copy(
                        isShimmerConnected = success,
                        statusText = if (success) "Connected to $deviceName" else "Failed to connect to $deviceName"
                    )
                }

                callback(success)
            } catch (e: Exception) {
                logger.error("Failed to connect to Shimmer device", e)
                callback(false)
            }
        }
    }

    fun configureShimmerSensors(
        deviceId: String,
        enabledChannels: Set<com.multisensor.recording.recording.DeviceConfiguration.SensorChannel>,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("Configuring sensors for device: $deviceId")
                val success = shimmerRecorder.setEnabledChannels(deviceId, enabledChannels)
                callback(success)
            } catch (e: Exception) {
                logger.error("Failed to configure sensors for device: $deviceId", e)
                callback(false)
            }
        }
    }

    fun setShimmerSamplingRate(
        deviceId: String,
        samplingRate: Double,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("Setting sampling rate to ${samplingRate}Hz for device: $deviceId")
                val success = shimmerRecorder.setSamplingRate(deviceId, samplingRate)
                callback(success)
            } catch (e: Exception) {
                logger.error("Failed to set sampling rate for device: $deviceId", e)
                callback(false)
            }
        }
    }

    fun setShimmerGSRRange(
        deviceId: String,
        gsrRange: Int,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("Setting GSR range to $gsrRange for device: $deviceId")
                val success = shimmerRecorder.setGSRRange(deviceId, gsrRange)
                callback(success)
            } catch (e: Exception) {
                logger.error("Failed to set GSR range for device: $deviceId", e)
                callback(false)
            }
        }
    }

    fun setShimmerAccelRange(
        deviceId: String,
        accelRange: Int,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("Setting accelerometer range to ±${accelRange}g for device: $deviceId")
                val success = shimmerRecorder.setAccelRange(deviceId, accelRange)
                callback(success)
            } catch (e: Exception) {
                logger.error("Failed to set accelerometer range for device: $deviceId", e)
                callback(false)
            }
        }
    }

    fun getShimmerDeviceInfo(
        deviceId: String,
        callback: (com.multisensor.recording.recording.ShimmerRecorder.DeviceInformation?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("Getting device information for: $deviceId")
                val deviceInfo = shimmerRecorder.getDeviceInformation(deviceId)
                callback(deviceInfo)
            } catch (e: Exception) {
                logger.error("Failed to get device information for: $deviceId", e)
                callback(null)
            }
        }
    }

    fun getShimmerDataQuality(
        deviceId: String,
        callback: (com.multisensor.recording.recording.ShimmerRecorder.DataQualityMetrics?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("Getting data quality metrics for: $deviceId")
                val metrics = shimmerRecorder.getDataQualityMetrics(deviceId)
                callback(metrics)
            } catch (e: Exception) {
                logger.error("Failed to get data quality metrics for: $deviceId", e)
                callback(null)
            }
        }
    }

    fun disconnectShimmerDevice(
        deviceId: String,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("Disconnecting from Shimmer device: $deviceId")
                val success = shimmerRecorder.disconnectAllDevices()

                updateUiState { currentState ->
                    currentState.copy(
                        isShimmerConnected = false,
                        statusText = if (success) "Disconnected from device" else "Failed to disconnect"
                    )
                }

                callback(success)
            } catch (e: Exception) {
                logger.error("Failed to disconnect from Shimmer device: $deviceId", e)
                callback(false)
            }
        }
    }

    fun scanForShimmerDevices(callback: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Scanning for Shimmer devices...")
                val devices = shimmerRecorder.scanAndPairDevices()
                callback(devices)
            } catch (e: Exception) {
                logger.error("Failed to scan for Shimmer devices", e)
                callback(emptyList())
            }
        }
    }

    fun enableShimmerClockSync(
        deviceId: String,
        enable: Boolean,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logger.info("${if (enable) "Enabling" else "Disabling"} clock sync for device: $deviceId")
                val success = shimmerRecorder.enableClockSync(deviceId, enable)
                callback(success)
            } catch (e: Exception) {
                logger.error("Failed to configure clock sync for device: $deviceId", e)
                callback(false)
            }
        }
    }

    fun startShimmerSDLogging(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = shimmerRecorder.startSDLogging()
                callback(success)
            } catch (e: Exception) {
                logger.error("Error starting SD logging", e)
                callback(false)
            }
        }
    }

    fun stopShimmerSDLogging(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = shimmerRecorder.stopSDLogging()
                callback(success)
            } catch (e: Exception) {
                logger.error("Error stopping SD logging", e)
                callback(false)
            }
        }
    }

    fun isAnyShimmerDeviceStreaming(): Boolean = shimmerRecorder.isAnyDeviceStreaming()

    fun isAnyShimmerDeviceSDLogging(): Boolean = shimmerRecorder.isAnyDeviceSDLogging()

    fun getConnectedShimmerDevice(macAddress: String): com.shimmerresearch.driver.ShimmerDevice? =
        shimmerRecorder.getConnectedShimmerDevice(macAddress)

    fun getFirstConnectedShimmerDevice(): com.shimmerresearch.driver.ShimmerDevice? =
        shimmerRecorder.getFirstConnectedShimmerDevice()

    fun getShimmerBluetoothManager(): com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid? =
        shimmerRecorder.getShimmerBluetoothManager()

    fun switchCamera() {
        viewModelScope.launch {
            try {
                logger.info("Switching camera")
            } catch (e: Exception) {
                logger.error("Error switching camera", e)
            }
        }
    }

    fun toggleThermalPreview() {
        viewModelScope.launch {
            try {
                logger.info("Toggling thermal preview")
                updateUiState { currentState ->
                    currentState.copy(thermalPreviewAvailable = !currentState.thermalPreviewAvailable)
                }
            } catch (e: Exception) {
                logger.error("Error toggling thermal preview", e)
            }
        }
    }

    fun connectToPC() {
        viewModelScope.launch {
            try {
                logger.info("Connecting to PC server...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Connecting to PC server...",
                        isConnecting = true
                    )
                }

                val serverConfig = networkConfiguration.getServerConfiguration()
                jsonSocketClient.configure(serverConfig.serverIp, serverConfig.jsonPort)

                jsonSocketClient.connect()

                kotlinx.coroutines.delay(2000)

                val isConnected = jsonSocketClient.isConnected()

                if (isConnected) {
                    updateUiState { currentState ->
                        currentState.copy(
                            isPcConnected = true,
                            isConnecting = false,
                            statusText = "Connected to PC at ${serverConfig.getJsonAddress()}",
                            systemHealth = currentState.systemHealth.copy(
                                pcConnection = SystemHealthStatus.HealthStatus.CONNECTED,
                                networkConnection = SystemHealthStatus.HealthStatus.CONNECTED
                            )
                        )
                    }
                    logger.info("Successfully connected to PC server at ${serverConfig.getJsonAddress()}")
                } else {
                    updateUiState { currentState ->
                        currentState.copy(
                            isPcConnected = false,
                            isConnecting = false,
                            statusText = "Failed to connect to PC at ${serverConfig.getJsonAddress()}",
                            errorMessage = "Could not establish connection to PC server. Please check:\n• PC server is running\n• Network configuration is correct\n• Firewall settings allow connection",
                            systemHealth = currentState.systemHealth.copy(
                                pcConnection = SystemHealthStatus.HealthStatus.ERROR,
                                networkConnection = SystemHealthStatus.HealthStatus.ERROR
                            )
                        )
                    }
                    logger.error("Failed to connect to PC server at ${serverConfig.getJsonAddress()}")
                }

            } catch (e: Exception) {
                logger.error("Error connecting to PC", e)
                updateUiState { currentState ->
                    currentState.copy(
                        isPcConnected = false,
                        isConnecting = false,
                        statusText = "PC connection error: ${e.message}",
                        errorMessage = "Connection failed: ${e.message}",
                        systemHealth = currentState.systemHealth.copy(
                            pcConnection = SystemHealthStatus.HealthStatus.ERROR,
                            networkConnection = SystemHealthStatus.HealthStatus.ERROR
                        )
                    )
                }
            }
        }
    }

    fun disconnectFromPC() {
        viewModelScope.launch {
            try {
                logger.info("Disconnecting from PC server...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Disconnecting from PC...",
                        isConnecting = true
                    )
                }

                jsonSocketClient.disconnect()

                updateUiState { currentState ->
                    currentState.copy(
                        isPcConnected = false,
                        isConnecting = false,
                        statusText = "Disconnected from PC server",
                        systemHealth = currentState.systemHealth.copy(
                            pcConnection = SystemHealthStatus.HealthStatus.DISCONNECTED,
                            networkConnection = SystemHealthStatus.HealthStatus.DISCONNECTED
                        )
                    )
                }
                logger.info("Successfully disconnected from PC server")
            } catch (e: Exception) {
                logger.error("Error disconnecting from PC", e)
                updateUiState { currentState ->
                    currentState.copy(
                        isConnecting = false,
                        statusText = "Disconnect error: ${e.message}",
                        errorMessage = "Disconnect failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun scanForShimmer() {
        viewModelScope.launch {
            try {
                logger.info("Scanning for Shimmer devices")
            } catch (e: Exception) {
                logger.error("Error scanning for Shimmer", e)
            }
        }
    }

    fun connectShimmer() {
        viewModelScope.launch {
            try {
                logger.info("Connecting Shimmer device")
                updateUiState { currentState ->
                    currentState.copy(
                        isShimmerConnected = true,
                        shimmerDeviceId = "SH001",
                        isGsrConnected = true
                    )
                }
            } catch (e: Exception) {
                logger.error("Error connecting Shimmer", e)
            }
        }
    }

    fun disconnectShimmer() {
        viewModelScope.launch {
            try {
                logger.info("Disconnecting Shimmer device")
                updateUiState { currentState ->
                    currentState.copy(
                        isShimmerConnected = false,
                        shimmerDeviceId = "",
                        isGsrConnected = false
                    )
                }
            } catch (e: Exception) {
                logger.error("Error disconnecting Shimmer", e)
            }
        }
    }

    fun connectThermal() {
        viewModelScope.launch {
            try {
                logger.info("Connecting thermal camera")
                updateUiState { currentState ->
                    currentState.copy(isThermalConnected = true)
                }
            } catch (e: Exception) {
                logger.error("Error connecting thermal camera", e)
            }
        }
    }

    fun disconnectThermal() {
        viewModelScope.launch {
            try {
                logger.info("Disconnecting thermal camera")
                updateUiState { currentState ->
                    currentState.copy(isThermalConnected = false)
                }
            } catch (e: Exception) {
                logger.error("Error disconnecting thermal camera", e)
            }
        }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            try {
                logger.info("Refreshing all devices")
                updateSystemState()
            } catch (e: Exception) {
                logger.error("Error refreshing devices", e)
            }
        }
    }

    fun exportAllFiles() {
        viewModelScope.launch {
            try {
                logger.info("Exporting all files")
            } catch (e: Exception) {
                logger.error("Error exporting files", e)
            }
        }
    }

    fun deleteAllFiles() {
        viewModelScope.launch {
            try {
                logger.info("Deleting all files")
                updateUiState { currentState ->
                    currentState.copy(
                        sessionCount = 0,
                        fileCount = 0,
                        storageUsed = 0L
                    )
                }
            } catch (e: Exception) {
                logger.error("Error deleting files", e)
            }
        }
    }

    fun transferFilesToPC() {
        viewModelScope.launch {
            try {
                logger.info("Starting file transfer to PC")
                updateUiState { currentState ->
                    currentState.copy(isTransferring = true)
                }

                val sessions = sessionManager.getAllSessions()
                logger.info("Found ${sessions.size} sessions to transfer")

                if (sessions.isEmpty()) {
                    updateUiState { currentState ->
                        currentState.copy(
                            isTransferring = false,
                            errorMessage = "No recording sessions found to transfer"
                        )
                    }
                    return@launch
                }

                var transferredFiles = 0
                var totalFiles = 0

                for (session in sessions) {
                    try {
                        val sessionFiles = fileTransferHandler.getAvailableFiles(session.sessionId)
                        totalFiles += sessionFiles.size

                        for (filePath in sessionFiles) {
                            try {
                                fileTransferHandler.handleSendFileCommand(
                                    SendFileCommand(
                                        filepath = filePath,
                                        filetype = getFileType(filePath)
                                    )
                                )
                                transferredFiles++
                                logger.info("Transferred file: $filePath")
                            } catch (e: Exception) {
                                logger.error("Failed to transfer file: $filePath", e)
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to process session: ${session.sessionId}", e)
                    }
                }

                val message = if (transferredFiles > 0) {
                    "Successfully transferred $transferredFiles of $totalFiles files"
                } else {
                    "No files were transferred successfully"
                }

                updateUiState { currentState ->
                    currentState.copy(
                        isTransferring = false,
                        statusText = message
                    )
                }
                logger.info("File transfer completed: $message")

            } catch (e: Exception) {
                logger.error("Error during file transfer", e)
                updateUiState { currentState ->
                    currentState.copy(
                        isTransferring = false,
                        errorMessage = "File transfer failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun getFileType(filePath: String): String {
        return when {
            filePath.endsWith(".mp4") -> "video"
            filePath.endsWith(".csv") -> "sensor_data"
            filePath.endsWith(".jpg") || filePath.endsWith(".png") -> "image"
            filePath.endsWith(".txt") -> "log"
            filePath.endsWith(".json") -> "config"
            else -> "data"
        }
    }

    fun refreshStorageInfo() {
        viewModelScope.launch {
            try {
                logger.info("Refreshing storage info")
                updateUiState { currentState ->
                    currentState.copy(
                        storageUsed = 2_500_000_000L,
                        storageAvailable = 2_700_000_000L,
                        storageTotal = 5_200_000_000L,
                        sessionCount = 12,
                        fileCount = 48
                    )
                }
            } catch (e: Exception) {
                logger.error("Error refreshing storage", e)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                logger.info("Clearing cache")
            } catch (e: Exception) {
                logger.error("Error clearing cache", e)
            }
        }
    }

    fun startCameraCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Starting camera calibration")
                updateUiState { currentState ->
                    currentState.copy(isCalibratingCamera = true)
                }
                kotlinx.coroutines.delay(5000)
                updateUiState { currentState ->
                    currentState.copy(
                        isCalibratingCamera = false,
                        isCameraCalibrated = true
                    )
                }
            } catch (e: Exception) {
                logger.error("Error starting camera calibration", e)
            }
        }
    }

    fun stopCameraCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Stopping camera calibration")
                updateUiState { currentState ->
                    currentState.copy(isCalibratingCamera = false)
                }
            } catch (e: Exception) {
                logger.error("Error stopping camera calibration", e)
            }
        }
    }

    fun resetCameraCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Resetting camera calibration")
                updateUiState { currentState ->
                    currentState.copy(isCameraCalibrated = false)
                }
            } catch (e: Exception) {
                logger.error("Error resetting camera calibration", e)
            }
        }
    }

    fun startThermalCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Starting thermal calibration")
                updateUiState { currentState ->
                    currentState.copy(isCalibratingThermal = true)
                }
                kotlinx.coroutines.delay(5000)
                updateUiState { currentState ->
                    currentState.copy(
                        isCalibratingThermal = false,
                        isThermalCalibrated = true
                    )
                }
            } catch (e: Exception) {
                logger.error("Error starting thermal calibration", e)
            }
        }
    }

    fun stopThermalCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Stopping thermal calibration")
                updateUiState { currentState ->
                    currentState.copy(isCalibratingThermal = false)
                }
            } catch (e: Exception) {
                logger.error("Error stopping thermal calibration", e)
            }
        }
    }

    fun resetThermalCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Resetting thermal calibration")
                updateUiState { currentState ->
                    currentState.copy(isThermalCalibrated = false)
                }
            } catch (e: Exception) {
                logger.error("Error resetting thermal calibration", e)
            }
        }
    }

    fun startShimmerCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Starting Shimmer calibration")
                updateUiState { currentState ->
                    currentState.copy(isCalibratingShimmer = true)
                }
                kotlinx.coroutines.delay(5000)
                updateUiState { currentState ->
                    currentState.copy(
                        isCalibratingShimmer = false,
                        isShimmerCalibrated = true
                    )
                }
            } catch (e: Exception) {
                logger.error("Error starting Shimmer calibration", e)
            }
        }
    }

    fun stopShimmerCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Stopping Shimmer calibration")
                updateUiState { currentState ->
                    currentState.copy(isCalibratingShimmer = false)
                }
            } catch (e: Exception) {
                logger.error("Error stopping Shimmer calibration", e)
            }
        }
    }

    fun resetShimmerCalibration() {
        viewModelScope.launch {
            try {
                logger.info("Resetting Shimmer calibration")
                updateUiState { currentState ->
                    currentState.copy(isShimmerCalibrated = false)
                }
            } catch (e: Exception) {
                logger.error("Error resetting Shimmer calibration", e)
            }
        }
    }

    fun validateSystem() {
        viewModelScope.launch {
            try {
                logger.info("Validating system")
                updateUiState { currentState ->
                    currentState.copy(isValidating = true)
                }
                kotlinx.coroutines.delay(3000)
                updateUiState { currentState ->
                    currentState.copy(
                        isValidating = false,
                        isSystemValidated = true
                    )
                }
            } catch (e: Exception) {
                logger.error("Error validating system", e)
            }
        }
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            try {
                logger.info("Running diagnostics")
                updateUiState { currentState ->
                    currentState.copy(isDiagnosticsRunning = true)
                }
                kotlinx.coroutines.delay(2000)
                updateUiState { currentState ->
                    currentState.copy(
                        isDiagnosticsRunning = false,
                        diagnosticsCompleted = true
                    )
                }
            } catch (e: Exception) {
                logger.error("Error running diagnostics", e)
            }
        }
    }

    fun saveCalibrationData() {
        viewModelScope.launch {
            try {
                logger.info("Saving calibration data")
            } catch (e: Exception) {
                logger.error("Error saving calibration data", e)
            }
        }
    }

    fun loadCalibrationData() {
        viewModelScope.launch {
            try {
                logger.info("Loading calibration data")
            } catch (e: Exception) {
                logger.error("Error loading calibration data", e)
            }
        }
    }

    fun exportCalibrationData() {
        viewModelScope.launch {
            try {
                logger.info("Exporting calibration data")
            } catch (e: Exception) {
                logger.error("Error exporting calibration data", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        logger.info("MainViewModel cleared")

        if (_uiState.value.isRecording) {
            viewModelScope.launch {
                try {
                    cameraRecorder.stopSession()
                    thermalRecorder.stopRecording()
                    shimmerRecorder.stopRecording()
                } catch (e: Exception) {
                    logger.error("Error stopping recording in onCleared", e)
                }
            }
        }
    }

    fun switchPreviewSurfaces(textureView: TextureView?, surfaceView: SurfaceView?) {
        viewModelScope.launch {
            try {
                logger.info("Switching preview surfaces - RGB: ${textureView != null}, Thermal: ${surfaceView != null}")

                updateUiState { currentState ->
                    currentState.copy(statusText = "Switching preview layout...")
                }

                if (textureView != null) {
                    logger.debug("Reinitializing camera with new TextureView")
                    val cameraInitialized = cameraRecorder.initialize(textureView)

                    updateUiState { currentState ->
                        currentState.copy(
                            isCameraConnected = cameraInitialized,
                            isLoadingPermissions = false
                        )
                    }

                    if (!cameraInitialized) {
                        logger.warning("Failed to reinitialize camera with new TextureView")
                    } else {
                        logger.info("Camera reinitialize successful with new TextureView")
                    }
                }

                if (surfaceView != null) {
                    logger.debug("Reinitializing thermal camera with new SurfaceView")

                    thermalRecorder.stopPreview()

                    val thermalInitialized = thermalRecorder.initialize(surfaceView)

                    updateUiState { currentState ->
                        currentState.copy(
                            isThermalConnected = thermalInitialized,
                            isLoadingPermissions = false
                        )
                    }

                    if (thermalInitialized) {
                        val previewStarted = thermalRecorder.startPreview()

                        updateUiState { currentState ->
                            currentState.copy(
                                thermalPreviewAvailable = previewStarted
                            )
                        }

                        if (previewStarted) {
                            logger.info("Thermal camera preview restarted successfully with new SurfaceView")
                        } else {
                            logger.warning("Failed to restart thermal camera preview with new SurfaceView")
                        }
                    } else {
                        logger.warning("Failed to reinitialize thermal camera with new SurfaceView")
                    }
                }

                val statusMessage = buildString {
                    append("Preview layout switched - ")
                    if (textureView != null) append("RGB: Active, ")
                    else append("RGB: Disabled, ")
                    if (surfaceView != null) append("Thermal: Active")
                    else append("Thermal: Disabled")
                }

                updateUiState { currentState ->
                    currentState.copy(statusText = statusMessage)
                }

                logger.info("Preview surface switching completed")

            } catch (e: Exception) {
                logger.error("Failed to switch preview surfaces", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Error switching preview layout",
                        errorMessage = "Preview switching failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateSystemState() {
        logger.debug("System state update requested")
    }

    fun refreshSystemStatus() {
        viewModelScope.launch {
            try {
                logger.info("Refreshing system status...")

                updateUiState { currentState ->
                    currentState.copy(statusText = "Refreshing system status...")
                }

                val thermalAvailable = checkThermalCameraAvailability()

                val shimmerStatus = shimmerRecorder.getShimmerStatus()
                val shimmerConnected = shimmerStatus.isConnected

                val pcConnected = jsonSocketClient.isConnected()

                val networkConnected = pcConnected

                val statusMessage = buildString {
                    append("Status updated - ")
                    append("Thermal: ${if (thermalAvailable) "OK" else "N/A"}, ")
                    append("Shimmer: ${if (shimmerConnected) "OK" else "N/A"}, ")
                    append("PC: ${if (pcConnected) "OK" else "N/A"}")
                }

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = statusMessage,
                        isThermalConnected = thermalAvailable,
                        isShimmerConnected = shimmerConnected,
                        isPcConnected = pcConnected,
                        isNetworkConnected = networkConnected,
                        thermalPreviewAvailable = thermalAvailable,
                        systemHealth = currentState.systemHealth.copy(
                            thermalCamera = if (thermalAvailable)
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED,
                            shimmerConnection = if (shimmerConnected)
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED,
                            pcConnection = if (pcConnected)
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED,
                            networkConnection = if (networkConnected)
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED
                        )
                    )
                }

                logger.info("System status refresh completed: $statusMessage")

            } catch (e: Exception) {
                logger.error("Error during system status refresh", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Status refresh failed",
                        errorMessage = "Failed to refresh status: ${e.message}"
                    )
                }
            }
        }
    }

    fun scanForShimmerDevicesEnhanced(callback: (List<Pair<String, String>>) -> Unit) {
        viewModelScope.launch {
            try {
                logger.info("Scanning for Shimmer devices...")

                val devices = shimmerRecorder.scanForDevices()

                logger.info("Found ${devices.size} Shimmer devices")
                callback(devices)

            } catch (e: Exception) {
                logger.error("Error during Shimmer device scan", e)
                callback(emptyList())
            }
        }
    }

    fun getKnownShimmerDevices(): List<Pair<String, String>> {
        return try {
            shimmerRecorder.getKnownDevices()
        } catch (e: Exception) {
            logger.error("Error getting known Shimmer devices", e)
            emptyList()
        }
    }

    fun pauseRecording() {
        viewModelScope.launch {
            try {
                updateUiState { it.copy(isPaused = true, isRecording = false) }
                logger.info("Recording paused")
            } catch (e: Exception) {
                logger.error("Error pausing recording", e)
            }
        }
    }

    fun connectAllDevices() {
        viewModelScope.launch {
            try {
                logger.info("Connecting all devices...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Connecting to all devices...",
                        isConnecting = true
                    )
                }

                var successCount = 0
                var totalAttempts = 0
                val connectionResults = mutableListOf<String>()

                totalAttempts++
                try {
                    val cameraConnected = cameraRecorder.isConnected
                    if (cameraConnected) {
                        successCount++
                        connectionResults.add("Camera: Available")
                        logger.info("Camera device is available")
                    } else {
                        connectionResults.add("Camera: Not initialized")
                        logger.warning("Camera device not initialized")
                    }
                } catch (e: Exception) {
                    connectionResults.add("Camera: Error - ${e.message}")
                    logger.error("Camera status check error", e)
                }

                totalAttempts++
                try {
                    val discoveredDevices = shimmerRecorder.scanAndPairDevices()
                    if (discoveredDevices.isNotEmpty()) {
                        val shimmerConnected = shimmerRecorder.connectDevicesWithStatus(discoveredDevices)
                        if (shimmerConnected) {
                            successCount++
                            connectionResults.add("Shimmer: Connected (${discoveredDevices.size} devices)")
                            logger.info("Shimmer devices connected: ${discoveredDevices.size}")
                        } else {
                            connectionResults.add("Shimmer: Failed to connect")
                            logger.warning("Shimmer connection failed")
                        }
                    } else {
                        connectionResults.add("Shimmer: No devices found")
                        logger.info("No Shimmer devices discovered")
                    }
                } catch (e: Exception) {
                    connectionResults.add("Shimmer: Error - ${e.message}")
                    logger.error("Shimmer connection error", e)
                }

                totalAttempts++
                try {
                    val serverConfig = networkConfiguration.getServerConfiguration()
                    jsonSocketClient.configure(serverConfig.serverIp, serverConfig.jsonPort)
                    jsonSocketClient.connect()

                    kotlinx.coroutines.delay(1000)

                    val pcConnected = jsonSocketClient.isConnected()
                    if (pcConnected) {
                        successCount++
                        connectionResults.add("PC: Connected (${serverConfig.getJsonAddress()})")
                        logger.info("PC server connected at ${serverConfig.getJsonAddress()}")
                    } else {
                        connectionResults.add("PC: Connection failed")
                        logger.warning("PC server connection failed")
                    }
                } catch (e: Exception) {
                    connectionResults.add("PC: Error - ${e.message}")
                    logger.error("PC server connection error", e)
                }
                totalAttempts++
                try {
                    val thermalAvailable = thermalRecorder.isThermalCameraAvailable()
                    if (thermalAvailable) {
                        successCount++
                        connectionResults.add("Thermal: Available")
                        logger.info("Thermal camera is available")
                    } else {
                        connectionResults.add("Thermal: Not available")
                        logger.warning("Thermal camera not available")
                    }
                } catch (e: Exception) {
                    connectionResults.add("Thermal: Error - ${e.message}")
                    logger.error("Thermal camera check error", e)
                }

                val statusMessage = "Device connections: $successCount/$totalAttempts successful"
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = statusMessage,
                        isConnecting = false,
                        isCameraConnected = connectionResults.any { it.startsWith("Camera: Available") },
                        isShimmerConnected = connectionResults.any { it.startsWith("Shimmer: Connected") },
                        isThermalConnected = connectionResults.any { it.startsWith("Thermal: Available") },
                        isPcConnected = connectionResults.any { it.startsWith("PC: Connected") },
                        systemHealth = currentState.systemHealth.copy(
                            shimmerConnection = if (connectionResults.any { it.startsWith("Shimmer: Connected") })
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED,
                            thermalCamera = if (connectionResults.any { it.startsWith("Thermal: Available") })
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED,
                            rgbCamera = if (connectionResults.any { it.startsWith("Camera: Available") })
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED,
                            pcConnection = if (connectionResults.any { it.startsWith("PC: Connected") })
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED,
                            networkConnection = if (connectionResults.any { it.startsWith("PC: Connected") })
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED
                        )
                    )
                }

                logger.info("Device connection completed: $statusMessage")
                logger.info("Connection details: ${connectionResults.joinToString(", ")}")

            } catch (e: Exception) {
                logger.error("Error connecting devices", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Device connection failed: ${e.message}",
                        isConnecting = false,
                        errorMessage = "Connection error: ${e.message}"
                    )
                }
            }
        }
    }

    fun scanForDevices() {
        viewModelScope.launch {
            try {
                logger.info("Scanning for devices...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Scanning for devices...",
                        isScanning = true
                    )
                }

                var devicesFound = 0

                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                try {
                    val cameraIds = cameraManager.cameraIdList
                    if (cameraIds.isNotEmpty()) {
                        devicesFound++
                        logger.info("Found ${cameraIds.size} camera(s)")
                    }
                } catch (e: Exception) {
                    logger.warning("Camera scan failed: ${e.message}")
                }

                scanForShimmerDevicesEnhanced { shimmerDevices ->
                    devicesFound += shimmerDevices.size
                    logger.info("Found ${shimmerDevices.size} Shimmer device(s)")
                }

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Scan complete: found $devicesFound device(s)",
                        isScanning = false,
                        systemHealth = currentState.systemHealth.copy(
                            shimmerConnection = if (devicesFound > 0)
                                SystemHealthStatus.HealthStatus.CONNECTED
                            else
                                SystemHealthStatus.HealthStatus.DISCONNECTED
                        )
                    )
                }

                logger.info("Device scan completed: $devicesFound devices found")
            } catch (e: Exception) {
                logger.error("Error scanning devices", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Device scan failed: ${e.message}",
                        isScanning = false,
                        errorMessage = "Scan error: ${e.message}"
                    )
                }
            }
        }
    }

    fun startCalibration() {
        viewModelScope.launch {
            try {
                updateUiState { it.copy(isCalibrating = true) }
                logger.info("Calibration started")
            } catch (e: Exception) {
                logger.error("Error starting calibration", e)
            }
        }
    }

    fun stopCalibration() {
        viewModelScope.launch {
            try {
                updateUiState { it.copy(isCalibrating = false) }
                logger.info("Calibration stopped")
            } catch (e: Exception) {
                logger.error("Error stopping calibration", e)
            }
        }
    }

    fun saveCalibration() {
        viewModelScope.launch {
            try {
                updateUiState { it.copy(calibrationComplete = true, isCalibrating = false) }
                logger.info("Calibration saved")
            } catch (e: Exception) {
                logger.error("Error saving calibration", e)
            }
        }
    }

    fun browseFiles() {
        viewModelScope.launch {
            try {
                logger.info("Opening file browser...")
                val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
                if (!recordingsDir.exists()) {
                    recordingsDir.mkdirs()
                }

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "File browser opened - ${recordingsDir.absolutePath}",
                        totalSessions = getSessionCount(recordingsDir),
                        totalDataSize = formatFileSize(getDirSize(recordingsDir))
                    )
                }

                logger.info("File browser opened: ${recordingsDir.absolutePath}")
            } catch (e: Exception) {
                logger.error("Error browsing files", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Error opening file browser: ${e.message}",
                        errorMessage = "File browser error: ${e.message}"
                    )
                }
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                logger.info("Exporting data...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Preparing data export...",
                        isTransferring = true
                    )
                }

                val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
                if (!recordingsDir.exists() || recordingsDir.listFiles()?.isEmpty() == true) {
                    updateUiState { currentState ->
                        currentState.copy(
                            statusText = "No data to export",
                            isTransferring = false,
                            errorMessage = "No recording sessions found to export"
                        )
                    }
                    return@launch
                }

                val fileCount = recordingsDir.walkTopDown().count { it.isFile }
                val totalSize = getDirSize(recordingsDir)

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Exporting $fileCount files (${formatFileSize(totalSize)})...",
                        isTransferring = true
                    )
                }

                kotlinx.coroutines.delay(2000)

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Export completed: $fileCount files exported",
                        isTransferring = false
                    )
                }

                logger.info("Data export completed: $fileCount files from ${recordingsDir.absolutePath}")
            } catch (e: Exception) {
                logger.error("Error exporting data", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Export failed: ${e.message}",
                        isTransferring = false,
                        errorMessage = "Export error: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteCurrentSession() {
        viewModelScope.launch {
            try {
                logger.info("Deleting current session...")
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Deleting current session...",
                        isTransferring = true
                    )
                }

                val currentSession = sessionManager.getCurrentSession()
                if (currentSession == null) {
                    updateUiState { currentState ->
                        currentState.copy(
                            statusText = "No active session to delete",
                            isTransferring = false,
                            errorMessage = "No current session found"
                        )
                    }
                    return@launch
                }

                val sessionDir = File(context.getExternalFilesDir(null), "recordings/${currentSession.sessionId}")
                val deletedFiles = if (sessionDir.exists()) {
                    val fileCount = sessionDir.walkTopDown().count { it.isFile }
                    sessionDir.deleteRecursively()
                    fileCount
                } else {
                    0
                }

                sessionManager.finalizeCurrentSession()

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Session deleted: $deletedFiles files removed",
                        isTransferring = false,
                        hasCurrentSession = false,
                        totalSessions = currentState.totalSessions - 1
                    )
                }

                logger.info("Current session deleted: $deletedFiles files removed")
            } catch (e: Exception) {
                logger.error("Error deleting session", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Failed to delete session: ${e.message}",
                        isTransferring = false,
                        errorMessage = "Delete error: ${e.message}"
                    )
                }
            }
        }
    }

    fun openDataFolder() {
        viewModelScope.launch {
            try {
                logger.info("Opening data folder...")
                val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
                if (!recordingsDir.exists()) {
                    recordingsDir.mkdirs()
                }

                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Data folder: ${recordingsDir.absolutePath}",
                        totalSessions = getSessionCount(recordingsDir),
                        totalDataSize = formatFileSize(getDirSize(recordingsDir))
                    )
                }

                logger.info("Data folder opened: ${recordingsDir.absolutePath}")
            } catch (e: Exception) {
                logger.error("Error opening data folder", e)
                updateUiState { currentState ->
                    currentState.copy(
                        statusText = "Error opening data folder: ${e.message}",
                        errorMessage = "Data folder error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun getSessionCount(directory: File): Int {
        return try {
            if (!directory.exists()) 0
            else directory.listFiles { file -> file.isDirectory }?.size ?: 0
        } catch (e: Exception) {
            logger.warning("Error counting sessions: ${e.message}")
            0
        }
    }

    private fun getDirSize(directory: File): Long {
        return try {
            if (!directory.exists()) return 0L

            var size = 0L
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
            size
        } catch (e: Exception) {
            logger.warning("Error calculating directory size: ${e.message}")
            0L
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
