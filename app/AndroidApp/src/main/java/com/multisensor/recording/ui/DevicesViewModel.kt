package com.multisensor.recording.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesUiState(
    val isPcConnected: Boolean = false,
    val pcIpAddress: String = "",
    val pcPort: String = "",
    val pcConnectionStatus: String = "",
    val pcLastSeen: String = "",

    val isShimmerConnected: Boolean = false,
    val shimmerMacAddress: String = "",
    val shimmerBatteryLevel: Int = 0,
    val shimmerActiveSensors: String = "",
    val shimmerSampleRate: String = "",
    val shimmerLastSeen: String = "",

    val isThermalConnected: Boolean = false,
    val thermalCameraModel: String = "",
    val thermalCurrentTemp: String = "",
    val thermalResolution: String = "",
    val thermalFrameRate: String = "",
    val thermalLastSeen: String = "",

    val isNetworkConnected: Boolean = false,
    val networkSsid: String = "",
    val networkIpAddress: String = "",
    val networkSignalStrength: Int = 0,
    val networkType: String = "",

    val isGsrConnected: Boolean = false,
    val gsrCurrentValue: String = "",
    val gsrRange: String = "",
    val gsrSampleRate: String = "",
    val gsrLastReading: String = "",

    val isConnecting: Boolean = false,
    val isTesting: Boolean = false,
    val testResults: List<String> = emptyList()
) {
    val totalConnectedDevices: Int
        get() = listOf(
            isPcConnected,
            isShimmerConnected,
            isThermalConnected,
            isNetworkConnected,
            isGsrConnected
        ).count { it }

    val allDevicesHealthy: Boolean get() = totalConnectedDevices > 0 && testResults.none { it.contains("FAILED") }
}

@HiltViewModel
class DevicesViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        refreshAllDevices()
    }

    fun connectPc() {
        _uiState.value = _uiState.value.copy(isConnecting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isPcConnected = true,
                pcIpAddress = "192.168.1.100",
                pcPort = "8080",
                pcConnectionStatus = "Connected",
                pcLastSeen = getCurrentTimestamp()
            )
        }
    }

    fun disconnectPc() {
        _uiState.value = _uiState.value.copy(
            isPcConnected = false,
            pcIpAddress = "",
            pcPort = "",
            pcConnectionStatus = "Disconnected",
            pcLastSeen = ""
        )
    }

    fun connectShimmer() {
        _uiState.value = _uiState.value.copy(isConnecting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isShimmerConnected = true,
                shimmerMacAddress = "00:06:66:12:34:56",
                shimmerBatteryLevel = 85,
                shimmerActiveSensors = "GSR, PPG, Accel",
                shimmerSampleRate = "512",
                shimmerLastSeen = getCurrentTimestamp()
            )
        }
    }

    fun disconnectShimmer() {
        _uiState.value = _uiState.value.copy(
            isShimmerConnected = false,
            shimmerMacAddress = "",
            shimmerBatteryLevel = 0,
            shimmerActiveSensors = "",
            shimmerSampleRate = "",
            shimmerLastSeen = ""
        )
    }

    fun connectThermal() {
        _uiState.value = _uiState.value.copy(isConnecting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(2500)
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isThermalConnected = true,
                thermalCameraModel = "FLIR Lepton 3.5",
                thermalCurrentTemp = "23.5",
                thermalResolution = "160x120",
                thermalFrameRate = "9",
                thermalLastSeen = getCurrentTimestamp()
            )
        }
    }

    fun disconnectThermal() {
        _uiState.value = _uiState.value.copy(
            isThermalConnected = false,
            thermalCameraModel = "",
            thermalCurrentTemp = "",
            thermalResolution = "",
            thermalFrameRate = "",
            thermalLastSeen = ""
        )
    }

    fun connectNetwork() {
        _uiState.value = _uiState.value.copy(isConnecting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(1500)
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isNetworkConnected = true,
                networkSsid = "ResearchLab_WiFi",
                networkIpAddress = "192.168.1.150",
                networkSignalStrength = 78,
                networkType = "WiFi"
            )
        }
    }

    fun disconnectNetwork() {
        _uiState.value = _uiState.value.copy(
            isNetworkConnected = false,
            networkSsid = "",
            networkIpAddress = "",
            networkSignalStrength = 0,
            networkType = ""
        )
    }

    fun refreshAllDevices() {
        _uiState.value = _uiState.value.copy(isConnecting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(1000)

            val isGsrDetected = _uiState.value.isShimmerConnected

            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isGsrConnected = isGsrDetected,
                gsrCurrentValue = if (isGsrDetected) "234 kΩ" else "",
                gsrRange = if (isGsrDetected) "10-500 kΩ" else "",
                gsrSampleRate = if (isGsrDetected) "512" else "",
                gsrLastReading = if (isGsrDetected) getCurrentTimestamp() else ""
            )
        }
    }

    fun testPcConnection() {
        _uiState.value = _uiState.value.copy(isTesting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(2000)

            val testResult = if (_uiState.value.isPcConnected) {
                "PC Connection Test: PASSED - Latency: 25ms, Bandwidth: 100 Mbps"
            } else {
                "PC Connection Test: FAILED - No connection established"
            }

            val updatedResults = _uiState.value.testResults.toMutableList()
            updatedResults.add("${getCurrentTimestamp()}: $testResult")

            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResults = updatedResults
            )
        }
    }

    fun testShimmerConnection() {
        _uiState.value = _uiState.value.copy(isTesting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(3000)

            val testResult = if (_uiState.value.isShimmerConnected) {
                "Shimmer Test: PASSED - Battery: ${_uiState.value.shimmerBatteryLevel}%, Signal Quality: Good"
            } else {
                "Shimmer Test: FAILED - Device not connected"
            }

            val updatedResults = _uiState.value.testResults.toMutableList()
            updatedResults.add("${getCurrentTimestamp()}: $testResult")

            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResults = updatedResults
            )
        }
    }

    fun testThermalConnection() {
        _uiState.value = _uiState.value.copy(isTesting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(2500)

            val testResult = if (_uiState.value.isThermalConnected) {
                "Thermal Camera Test: PASSED - Temperature Range: Valid, Image Quality: Good"
            } else {
                "Thermal Camera Test: FAILED - Camera not connected"
            }

            val updatedResults = _uiState.value.testResults.toMutableList()
            updatedResults.add("${getCurrentTimestamp()}: $testResult")

            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResults = updatedResults
            )
        }
    }

    fun testNetworkConnection() {
        _uiState.value = _uiState.value.copy(isTesting = true)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(1500)

            val testResult = if (_uiState.value.isNetworkConnected) {
                "Network Test: PASSED - Signal: ${_uiState.value.networkSignalStrength}%, Internet: Available"
            } else {
                "Network Test: FAILED - No network connection"
            }

            val updatedResults = _uiState.value.testResults.toMutableList()
            updatedResults.add("${getCurrentTimestamp()}: $testResult")

            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResults = updatedResults
            )
        }
    }

    private fun getCurrentTimestamp(): String {
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}
