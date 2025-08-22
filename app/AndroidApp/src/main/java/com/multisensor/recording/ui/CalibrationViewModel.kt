package com.multisensor.recording.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalibrationUiState(
    val isCameraCalibrated: Boolean = false,
    val isCameraCalibrating: Boolean = false,
    val cameraCalibrationProgress: Int = 0,
    val cameraCalibrationError: Double = 0.0,
    val cameraCalibrationDate: String = "",

    val isThermalCalibrated: Boolean = false,
    val isThermalCalibrating: Boolean = false,
    val thermalCalibrationProgress: Int = 0,
    val thermalTempRange: String = "",
    val thermalEmissivity: String = "",
    val thermalColorPalette: String = "",
    val thermalCalibrationDate: String = "",

    val isShimmerCalibrated: Boolean = false,
    val isShimmerCalibrating: Boolean = false,
    val shimmerCalibrationProgress: Int = 0,
    val shimmerMacAddress: String = "",
    val shimmerSensorConfig: String = "",
    val shimmerSamplingRate: String = "",
    val shimmerCalibrationDate: String = "",

    val isValidating: Boolean = false,
    val isSystemValid: Boolean = false,
    val validationErrors: List<String> = emptyList(),

    val canStartCalibration: Boolean = true
) {
    val isAnyCalibrating get() = isCameraCalibrating || isThermalCalibrating || isShimmerCalibrating || isValidating
}

@HiltViewModel
class CalibrationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    init {
        loadCalibrationStatus()
    }

    fun startCameraCalibration() {
        _uiState.value = _uiState.value.copy(
            isCameraCalibrating = true,
            cameraCalibrationProgress = 0
        )

        simulateCameraCalibration()
    }

    fun startThermalCalibration() {
        _uiState.value = _uiState.value.copy(
            isThermalCalibrating = true,
            thermalCalibrationProgress = 0
        )

        simulateThermalCalibration()
    }

    fun startShimmerCalibration() {
        _uiState.value = _uiState.value.copy(
            isShimmerCalibrating = true,
            shimmerCalibrationProgress = 0
        )

        simulateShimmerCalibration()
    }

    fun resetCameraCalibration() {
        _uiState.value = _uiState.value.copy(
            isCameraCalibrated = false,
            cameraCalibrationError = 0.0,
            cameraCalibrationDate = ""
        )
    }

    fun resetThermalCalibration() {
        _uiState.value = _uiState.value.copy(
            isThermalCalibrated = false,
            thermalTempRange = "",
            thermalEmissivity = "",
            thermalColorPalette = "",
            thermalCalibrationDate = ""
        )
    }

    fun resetShimmerCalibration() {
        _uiState.value = _uiState.value.copy(
            isShimmerCalibrated = false,
            shimmerMacAddress = "",
            shimmerSensorConfig = "",
            shimmerSamplingRate = "",
            shimmerCalibrationDate = ""
        )
    }

    fun saveCalibrationData() {
        android.util.Log.i("CalibrationVM", "Saving calibration data...")
    }

    fun loadCalibrationData() {
        android.util.Log.i("CalibrationVM", "Loading calibration data...")
        loadCalibrationStatus()
    }

    fun exportCalibrationData() {
        android.util.Log.i("CalibrationVM", "Exporting calibration data...")
    }

    fun validateSystem() {
        _uiState.value = _uiState.value.copy(
            isValidating = true,
            validationErrors = emptyList()
        )

        simulateSystemValidation()
    }

    private fun loadCalibrationStatus() {
        _uiState.value = _uiState.value.copy(
            isCameraCalibrated = false,
            isThermalCalibrated = false,
            isShimmerCalibrated = false,
            canStartCalibration = true
        )
    }

    private fun simulateCameraCalibration() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            for (progress in 0..100 step 10) {
                kotlinx.coroutines.delay(200)
                _uiState.value = _uiState.value.copy(
                    cameraCalibrationProgress = progress
                )
            }

            _uiState.value = _uiState.value.copy(
                isCameraCalibrating = false,
                isCameraCalibrated = true,
                cameraCalibrationProgress = 100,
                cameraCalibrationError = 0.342,
                cameraCalibrationDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )
        }
    }

    private fun simulateThermalCalibration() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            for (progress in 0..100 step 15) {
                kotlinx.coroutines.delay(150)
                _uiState.value = _uiState.value.copy(
                    thermalCalibrationProgress = progress
                )
            }

            _uiState.value = _uiState.value.copy(
                isThermalCalibrating = false,
                isThermalCalibrated = true,
                thermalCalibrationProgress = 100,
                thermalTempRange = "15°C to 45°C",
                thermalEmissivity = "0.95",
                thermalColorPalette = "Iron",
                thermalCalibrationDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )
        }
    }

    private fun simulateShimmerCalibration() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            for (progress in 0..100 step 20) {
                kotlinx.coroutines.delay(100)
                _uiState.value = _uiState.value.copy(
                    shimmerCalibrationProgress = progress
                )
            }

            _uiState.value = _uiState.value.copy(
                isShimmerCalibrating = false,
                isShimmerCalibrated = true,
                shimmerCalibrationProgress = 100,
                shimmerMacAddress = "00:06:66:12:34:56",
                shimmerSensorConfig = "GSR + PPG + Accel",
                shimmerSamplingRate = "512",
                shimmerCalibrationDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )
        }
    }

    private fun simulateSystemValidation() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(2000)

            val errors = mutableListOf<String>()
            val currentState = _uiState.value

            if (!currentState.isCameraCalibrated) {
                errors.add("Camera not calibrated")
            }
            if (!currentState.isThermalCalibrated) {
                errors.add("Thermal camera not calibrated")
            }
            if (!currentState.isShimmerCalibrated) {
                errors.add("Shimmer device not calibrated")
            }

            _uiState.value = _uiState.value.copy(
                isValidating = false,
                isSystemValid = errors.isEmpty(),
                validationErrors = errors
            )
        }
    }
}
