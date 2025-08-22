package com.multisensor.recording.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multisensor.recording.recording.ShimmerRecorder
import com.multisensor.recording.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class ShimmerConfigViewModel @Inject constructor(
    private val shimmerRecorder: ShimmerRecorder,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShimmerConfigUiState())
    val uiState: StateFlow<ShimmerConfigUiState> = _uiState.asStateFlow()
    companion object {
        private const val STATUS_UPDATE_INTERVAL_MS = 2000L
    }
    init {
        initialize()
        startPeriodicStatusUpdates()
    }
    private fun initialize() {
        viewModelScope.launch {
            try {
                shimmerRecorder.initialize()
                logger.info("ShimmerRecorder initialized successfully")
            } catch (e: Exception) {
                logger.error("Error initializing ShimmerRecorder", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "Error initializing shimmer: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    private fun startPeriodicStatusUpdates() {
        viewModelScope.launch {
            flow {
                while (true) {
                    emit(Unit)
                    delay(STATUS_UPDATE_INTERVAL_MS)
                }
            }.collect {
                if (_uiState.value.isDeviceConnected) {
                    updateRealTimeData()
                }
            }
        }
    }
    fun scanForDevices() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    connectionStatus = "Scanning...",
                    errorMessage = null
                )
            }
            try {
                val devices = shimmerRecorder.scanAndPairDevices()
                val deviceItems = devices.map { address ->
                    ShimmerDeviceItem(
                        name = "Shimmer Device",
                        macAddress = address,
                        rssi = -50,
                        isConnectable = true
                    )
                }
                _uiState.update {
                    it.copy(
                        availableDevices = deviceItems,
                        isScanning = false,
                        connectionStatus = if (deviceItems.isNotEmpty()) "Found ${deviceItems.size} devices" else "No devices found",
                        errorMessage = if (deviceItems.isEmpty()) "No Shimmer devices found. Make sure devices are paired in Bluetooth settings." else null
                    )
                }
            } catch (e: Exception) {
                logger.error("Error scanning for devices", e)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        connectionStatus = "Scan failed",
                        errorMessage = "Error scanning: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    fun connectToDevice() {
        val selectedDevice = _uiState.value.selectedDevice ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingConnection = true,
                    connectionStatus = "Connecting...",
                    errorMessage = null
                )
            }
            try {
                val connected = shimmerRecorder.connectDevicesWithStatus(listOf(selectedDevice.macAddress))
                if (connected) {
                    _uiState.update {
                        it.copy(
                            isDeviceConnected = true,
                            isLoadingConnection = false,
                            connectionStatus = "Connected",
                            deviceName = selectedDevice.name,
                            deviceMacAddress = selectedDevice.macAddress,
                            showConfigurationPanel = true,
                            showRecordingControls = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingConnection = false,
                            connectionStatus = "Connection failed",
                            errorMessage = "Failed to connect to ${selectedDevice.name}",
                            showErrorDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error connecting", e)
                _uiState.update {
                    it.copy(
                        isLoadingConnection = false,
                        connectionStatus = "Connection error",
                        errorMessage = "Connection error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    fun disconnectFromDevice() {
        viewModelScope.launch {
            try {
                if (_uiState.value.isRecording) {
                    shimmerRecorder.stopStreaming()
                }
                shimmerRecorder.cleanup()
                _uiState.update {
                    it.copy(
                        isDeviceConnected = false,
                        isRecording = false,
                        connectionStatus = "Disconnected",
                        deviceName = "",
                        deviceMacAddress = "",
                        batteryLevel = -1,
                        signalStrength = -1,
                        firmwareVersion = "",
                        showConfigurationPanel = false,
                        showRecordingControls = false,
                        recordingDuration = 0L,
                        dataPacketsReceived = 0,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                logger.error("Error disconnecting from device", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "Disconnect error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    fun startStreaming() {
        viewModelScope.launch {
            try {
                val started = shimmerRecorder.startStreaming()
                if (started) {
                    _uiState.update {
                        it.copy(
                            isRecording = true,
                            connectionStatus = "Recording",
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to start streaming",
                            showErrorDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error starting streaming", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "Streaming error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    fun stopStreaming() {
        viewModelScope.launch {
            try {
                val stopped = shimmerRecorder.stopStreaming()
                if (stopped) {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            connectionStatus = "Connected",
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to stop streaming",
                            showErrorDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error stopping streaming", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "Stop streaming error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    private fun updateRealTimeData() {
        viewModelScope.launch {
            try {
                val status = shimmerRecorder.getShimmerStatus()
                val readings = shimmerRecorder.getCurrentReadings()
                _uiState.update {
                    it.copy(
                        batteryLevel = status.batteryLevel ?: -1,
                        signalStrength = if (status.isConnected) -50 else -100,
                        dataPacketsReceived = it.dataPacketsReceived + if (readings.isNotEmpty()) 1 else 0,
                        recordingDuration = if (it.isRecording) it.recordingDuration + STATUS_UPDATE_INTERVAL_MS else 0L
                    )
                }
            } catch (e: Exception) {
                logger.error("Error updating real-time data", e)
            }
        }
    }
    fun onDeviceSelected(index: Int) {
        _uiState.update { it.copy(selectedDeviceIndex = index) }
    }
    fun updateSensorConfiguration(enabledSensors: Set<String>) {
        _uiState.update {
            it.copy(
                enabledSensors = enabledSensors,
                isConfiguring = true
            )
        }
        viewModelScope.launch {
            try {
                if (_uiState.value.isDeviceConnected) {
                    val sensorChannels = enabledSensors.mapNotNull { sensorName ->
                        try {
                            com.multisensor.recording.recording.DeviceConfiguration.SensorChannel.valueOf(sensorName.uppercase())
                        } catch (e: IllegalArgumentException) {
                            logger.warning("Unknown sensor channel: $sensorName")
                            null
                        }
                    }.toSet()
                    val result = _uiState.value.selectedDevice?.let { device ->
                        shimmerRecorder.setEnabledChannels(device.macAddress, sensorChannels)
                    } ?: false
                    if (!result) {
                        logger.warning("Failed to update sensor configuration")
                    } else {
                        logger.info("Sensor configuration updated successfully")
                    }
                    delay(500)
                }
                _uiState.update { it.copy(isConfiguring = false) }
            } catch (e: Exception) {
                logger.error("Error updating sensor configuration", e)
                _uiState.update {
                    it.copy(
                        isConfiguring = false,
                        errorMessage = "Configuration error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    fun updateSamplingRate(samplingRate: Int) {
        _uiState.update { it.copy(samplingRate = samplingRate) }
        viewModelScope.launch {
            try {
                if (_uiState.value.isDeviceConnected) {
                    val result = _uiState.value.selectedDevice?.let { device ->
                        shimmerRecorder.setSamplingRate(device.macAddress, samplingRate.toDouble())
                    } ?: false
                    if (!result) {
                        logger.warning("Failed to update sampling rate")
                    } else {
                        logger.info("Sampling rate updated to ${samplingRate}Hz")
                    }
                    delay(200)
                }
            } catch (e: Exception) {
                logger.error("Error updating sampling rate", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "Sampling rate error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    fun updateBluetoothState(isEnabled: Boolean, hasPermission: Boolean) {
        _uiState.update {
            it.copy(
                isBluetoothEnabled = isEnabled,
                hasBluetoothPermission = hasPermission
            )
        }
    }
    fun onErrorMessageShown() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                showErrorDialog = false
            )
        }
    }
    fun applyConfigurationPreset(presetName: String) {
        logger.info("Applying configuration preset: $presetName")
        val (enabledSensors, samplingRate) = when (presetName) {
            "Default" -> {
                Pair(setOf("GSR", "PPG", "ACCEL"), 256)
            }
            "High Performance" -> {
                Pair(setOf("GSR", "PPG", "ACCEL", "GYRO", "MAG", "ECG"), 512)
            }
            "Low Power" -> {
                Pair(setOf("GSR", "PPG"), 128)
            }
            "Custom" -> {
                return
            }
            else -> {
                logger.warning("Unknown preset: $presetName")
                return
            }
        }
        _uiState.update {
            it.copy(
                enabledSensors = enabledSensors,
                samplingRate = samplingRate
            )
        }
        updateSensorConfiguration(enabledSensors)
        updateSamplingRate(samplingRate)
    }
    fun getAvailablePresets(): List<String> = listOf("Default", "High Performance", "Low Power", "Custom")
    fun connectToSpecificDevice(macAddress: String, deviceName: String) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoadingConnection = true,
                        connectionStatus = "Connecting...",
                        errorMessage = null
                    )
                }
                logger.info("Connecting to specific device: $deviceName ($macAddress)")
                val connected = shimmerRecorder.connectSingleDevice(
                    macAddress,
                    deviceName,
                    com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid.BT_TYPE.BT_CLASSIC
                )
                if (connected) {
                    val deviceInfo = shimmerRecorder.getDeviceInformation(macAddress)
                    _uiState.update {
                        it.copy(
                            isLoadingConnection = false,
                            isDeviceConnected = true,
                            deviceName = deviceName,
                            deviceMacAddress = macAddress,
                            connectionStatus = "Connected",
                            firmwareVersion = deviceInfo?.firmwareVersion ?: "",
                            hardwareVersion = deviceInfo?.hardwareVersion ?: "",
                            batteryLevel = deviceInfo?.batteryLevel ?: -1,
                            showConfigurationPanel = true,
                            showRecordingControls = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingConnection = false,
                            connectionStatus = "Connection Failed",
                            errorMessage = "Failed to connect to $deviceName",
                            showErrorDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error connecting to specific device", e)
                _uiState.update {
                    it.copy(
                        isLoadingConnection = false,
                        connectionStatus = "Error",
                        errorMessage = "Connection error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    fun updateCrcConfiguration(crcMode: Int) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(crcMode = crcMode, isConfiguring = true)
                }
                val device = _uiState.value.selectedDevice
                if (device != null) {
                    logger.info("Updating CRC configuration for device ${device.macAddress}: mode $crcMode")
                    delay(500)
                    _uiState.update {
                        it.copy(isConfiguring = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isConfiguring = false,
                            errorMessage = "No device selected for CRC configuration",
                            showErrorDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error updating CRC configuration", e)
                _uiState.update {
                    it.copy(
                        isConfiguring = false,
                        errorMessage = "CRC configuration error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            }
        }
    }
    fun updateGsrRange(rangeIndex: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isConfiguring = true) }
                val device = _uiState.value.selectedDevice
                if (device != null) {
                    logger.info("Updating GSR range for device ${device.macAddress}: range $rangeIndex")
                    val success = shimmerRecorder.setGSRRange(device.macAddress, rangeIndex)
                    if (success) {
                        logger.info("GSR range updated successfully")
                    } else {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Failed to update GSR range",
                                showErrorDialog = true
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "No device selected for GSR range configuration",
                            showErrorDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error updating GSR range", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "GSR range configuration error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            } finally {
                _uiState.update { it.copy(isConfiguring = false) }
            }
        }
    }
    fun updateAccelRange(rangeG: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isConfiguring = true) }
                val device = _uiState.value.selectedDevice
                if (device != null) {
                    logger.info("Updating Accelerometer range for device ${device.macAddress}: ±${rangeG}g")
                    val success = shimmerRecorder.setAccelRange(device.macAddress, rangeG)
                    if (success) {
                        logger.info("Accelerometer range updated successfully")
                    } else {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Failed to update accelerometer range",
                                showErrorDialog = true
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "No device selected for accelerometer range configuration",
                            showErrorDialog = true
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error updating accelerometer range", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "Accelerometer range configuration error: ${e.message}",
                        showErrorDialog = true
                    )
                }
            } finally {
                _uiState.update { it.copy(isConfiguring = false) }
            }
        }
    }
}
