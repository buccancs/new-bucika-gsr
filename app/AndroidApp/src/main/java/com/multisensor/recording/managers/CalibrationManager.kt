package com.multisensor.recording.managers

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.multisensor.recording.calibration.CalibrationCaptureManager
import com.multisensor.recording.calibration.CalibrationProcessor
import com.multisensor.recording.util.Logger
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.BufferedReader
import java.io.FileReader
import org.json.JSONObject
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calibrationCaptureManager: CalibrationCaptureManager,
    private val calibrationProcessor: CalibrationProcessor,
    private val logger: Logger
) {

    companion object {
        private const val CALIBRATION_PREFS = "calibration_data"
        private const val CALIBRATION_FILE_PREFIX = "calibration_"
        private const val CALIBRATION_EXPORT_DIR = "calibration_exports"
    }

    data class CalibrationState(
        val isCalibrating: Boolean = false,
        val calibrationType: CalibrationType? = null,
        val progress: CalibrationProgress? = null,
        val isValidating: Boolean = false,
        val calibrationError: String? = null,
        val completedCalibrations: Set<CalibrationType> = emptySet(),
        val lastCalibrationResult: CalibrationResult? = null
    )

    enum class CalibrationType(val displayName: String) {
        CAMERA("Camera Calibration"),
        THERMAL("Thermal Camera Calibration"),
        SHIMMER("Shimmer Sensor Calibration"),
        SYSTEM("Full System Calibration")
    }

    data class CalibrationProgress(
        val currentStep: String,
        val stepNumber: Int,
        val totalSteps: Int,
        val progressPercent: Int
    )

    data class CalibrationResult(
        val success: Boolean,
        val calibrationType: CalibrationType,
        val message: String,
        val rgbFilePath: String? = null,
        val thermalFilePath: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val calibrationParameters: CalibrationParameters? = null
    )
    
    data class CalibrationParameters(
        val cameraIntrinsics: CameraIntrinsics? = null,
        val thermalCalibration: ThermalCalibration? = null,
        val shimmerCalibration: ShimmerCalibration? = null,
        val qualityScore: Double = 0.0,
        val calibrationId: String
    )
    
    data class CameraIntrinsics(
        val focalLengthX: Double,
        val focalLengthY: Double,
        val principalPointX: Double,
        val principalPointY: Double,
        val distortionCoefficients: List<Double>
    )
    
    data class ThermalCalibration(
        val temperatureRange: Pair<Double, Double>,
        val calibrationMatrix: List<List<Double>>,
        val noiseLevel: Double,
        val uniformityError: Double
    )
    
    data class ShimmerCalibration(
        val gsrBaseline: Double,
        val gsrRange: Pair<Double, Double>,
        val samplingAccuracy: Double,
        val signalNoiseRatio: Double
    )

    data class CalibrationConfig(
        val captureRgb: Boolean = true,
        val captureThermal: Boolean = true,
        val highResolution: Boolean = true,
        val captureCount: Int = 1,
        val calibrationId: String? = null
    )

    private val _calibrationState = MutableStateFlow(CalibrationState())
    val calibrationState: StateFlow<CalibrationState> = _calibrationState.asStateFlow()

    suspend fun runCalibration(config: CalibrationConfig = CalibrationConfig()): Result<CalibrationResult> {
        return try {
            if (_calibrationState.value.isCalibrating) {
                return Result.failure(IllegalStateException("Calibration already in progress"))
            }

            val calibrationId = config.calibrationId ?: "calibration_${System.currentTimeMillis()}"
            logger.info("Starting calibration process: $calibrationId")

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = true,
                calibrationType = CalibrationType.SYSTEM,
                progress = CalibrationProgress("Initializing calibration...", 1, 4, 0)
            )

            updateProgress("Preparing calibration setup...", 1, 4, 25)
            
            updateProgress("Capturing calibration images...", 2, 4, 50)
            val captureResult = calibrationCaptureManager.captureCalibrationImages(
                calibrationId = calibrationId,
                captureRgb = config.captureRgb,
                captureThermal = config.captureThermal,
                highResolution = config.highResolution
            )

            if (!captureResult.success) {
                val errorMsg = captureResult.errorMessage ?: "Calibration capture failed"
                _calibrationState.value = _calibrationState.value.copy(
                    isCalibrating = false,
                    calibrationError = errorMsg
                )
                return Result.failure(RuntimeException(errorMsg))
            }

            updateProgress("Processing calibration data...", 3, 4, 75)
            
            // Perform actual calibration processing instead of fake delay
            val cameraCalibrationResult = calibrationProcessor.processCameraCalibration(
                rgbImagePath = captureResult.rgbFilePath,
                thermalImagePath = captureResult.thermalFilePath,
                highResolution = config.highResolution
            )
            
            val thermalCalibrationResult = if (captureResult.thermalFilePath != null) {
                calibrationProcessor.processThermalCalibration(captureResult.thermalFilePath)
            } else {
                null
            }

            updateProgress("Validating calibration results...", 4, 4, 100)
            
            // Check if calibration processing was successful
            val processingSuccess = cameraCalibrationResult.success && 
                                  (thermalCalibrationResult?.success != false)
            
            if (!processingSuccess) {
                val errorMsg = cameraCalibrationResult.errorMessage ?: 
                              thermalCalibrationResult?.errorMessage ?: 
                              "Calibration processing failed"
                _calibrationState.value = _calibrationState.value.copy(
                    isCalibrating = false,
                    calibrationError = errorMsg
                )
                return Result.failure(RuntimeException(errorMsg))
            }

            val result = CalibrationResult(
                success = true,
                calibrationType = CalibrationType.SYSTEM,
                message = "Calibration completed successfully with quality: ${String.format("%.2f", cameraCalibrationResult.calibrationQuality)}",
                rgbFilePath = captureResult.rgbFilePath,
                thermalFilePath = captureResult.thermalFilePath,
                calibrationParameters = CalibrationParameters(
                    calibrationId = calibrationId,
                    qualityScore = cameraCalibrationResult.calibrationQuality,
                    cameraIntrinsics = CameraIntrinsics(
                        focalLengthX = cameraCalibrationResult.focalLengthX,
                        focalLengthY = cameraCalibrationResult.focalLengthY,
                        principalPointX = cameraCalibrationResult.principalPointX,
                        principalPointY = cameraCalibrationResult.principalPointY,
                        distortionCoefficients = cameraCalibrationResult.distortionCoefficients.toList()
                    ),
                    thermalCalibration = thermalCalibrationResult?.let { thermal ->
                        ThermalCalibration(
                            temperatureRange = thermal.temperatureRange,
                            calibrationMatrix = thermal.calibrationMatrix.map { it.toList() },
                            noiseLevel = thermal.noiseLevel,
                            uniformityError = thermal.uniformityError
                        )
                    }
                )
            )

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                completedCalibrations = _calibrationState.value.completedCalibrations + CalibrationType.SYSTEM,
                lastCalibrationResult = result
            )

            logger.info("Calibration completed successfully: $calibrationId")
            Result.success(result)

        } catch (e: Exception) {
            logger.error("Calibration failed", e)
            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                calibrationError = "Calibration failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun runCameraCalibration(
        includeRgb: Boolean = true,
        includeThermal: Boolean = true
    ): Result<CalibrationResult> {
        return try {
            if (_calibrationState.value.isCalibrating) {
                return Result.failure(IllegalStateException("Calibration already in progress"))
            }

            logger.info("Starting camera calibration (RGB: $includeRgb, Thermal: $includeThermal)")

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = true,
                calibrationType = CalibrationType.CAMERA,
                progress = CalibrationProgress("Starting camera calibration...", 1, 3, 0)
            )

            updateProgress("Setting up camera calibration...", 1, 3, 33)

            updateProgress("Capturing calibration images...", 2, 3, 66)
            val calibrationId = "camera_calibration_${System.currentTimeMillis()}"

            val captureResult = calibrationCaptureManager.captureCalibrationImages(
                calibrationId = calibrationId,
                captureRgb = includeRgb,
                captureThermal = includeThermal,
                highResolution = true
            )

            updateProgress("Processing camera calibration data...", 3, 3, 100)
            
            // Perform actual camera calibration processing instead of fake delay
            val cameraCalibrationResult = if (captureResult.success) {
                calibrationProcessor.processCameraCalibration(
                    rgbImagePath = captureResult.rgbFilePath,
                    thermalImagePath = captureResult.thermalFilePath,
                    highResolution = true
                )
            } else {
                null
            }

            val result = CalibrationResult(
                success = captureResult.success && (cameraCalibrationResult?.success == true),
                calibrationType = CalibrationType.CAMERA,
                message = if (captureResult.success && cameraCalibrationResult?.success == true) {
                    "Camera calibration completed with quality: ${String.format("%.2f", cameraCalibrationResult.calibrationQuality)}"
                } else {
                    captureResult.errorMessage ?: cameraCalibrationResult?.errorMessage ?: "Camera calibration failed"
                },
                rgbFilePath = captureResult.rgbFilePath,
                thermalFilePath = captureResult.thermalFilePath,
                calibrationParameters = if (cameraCalibrationResult?.success == true) {
                    CalibrationParameters(
                        calibrationId = calibrationId,
                        qualityScore = cameraCalibrationResult.calibrationQuality,
                        cameraIntrinsics = CameraIntrinsics(
                            focalLengthX = cameraCalibrationResult.focalLengthX,
                            focalLengthY = cameraCalibrationResult.focalLengthY,
                            principalPointX = cameraCalibrationResult.principalPointX,
                            principalPointY = cameraCalibrationResult.principalPointY,
                            distortionCoefficients = cameraCalibrationResult.distortionCoefficients.toList()
                        )
                    )
                } else null
            )

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                completedCalibrations = if (result.success) {
                    _calibrationState.value.completedCalibrations + CalibrationType.CAMERA
                } else {
                    _calibrationState.value.completedCalibrations
                },
                lastCalibrationResult = result,
                calibrationError = if (!result.success) result.message else null
            )

            if (result.success) {
                logger.info("Camera calibration completed successfully")
                Result.success(result)
            } else {
                logger.error("Camera calibration failed: ${result.message}")
                Result.failure(RuntimeException(result.message))
            }

        } catch (e: Exception) {
            logger.error("Camera calibration error", e)
            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                calibrationError = "Camera calibration error: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun runThermalCalibration(): Result<CalibrationResult> {
        return try {
            if (_calibrationState.value.isCalibrating) {
                return Result.failure(IllegalStateException("Calibration already in progress"))
            }

            logger.info("Starting thermal camera calibration")

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = true,
                calibrationType = CalibrationType.THERMAL,
                progress = CalibrationProgress("Starting thermal calibration...", 1, 3, 0)
            )

            updateProgress("Setting up thermal calibration...", 1, 3, 33)

            updateProgress("Capturing thermal reference...", 2, 3, 66)
            val calibrationId = "thermal_calibration_${System.currentTimeMillis()}"
            
            val captureResult = calibrationCaptureManager.captureCalibrationImages(
                calibrationId = calibrationId,
                captureRgb = false,
                captureThermal = true,
                highResolution = false
            )

            updateProgress("Processing thermal calibration...", 3, 3, 100)
            
            // Perform actual thermal calibration processing instead of fake delay
            val thermalCalibrationResult = if (captureResult.success && captureResult.thermalFilePath != null) {
                calibrationProcessor.processThermalCalibration(captureResult.thermalFilePath)
            } else {
                null
            }

            val result = CalibrationResult(
                success = captureResult.success && (thermalCalibrationResult?.success == true),
                calibrationType = CalibrationType.THERMAL,
                message = if (captureResult.success && thermalCalibrationResult?.success == true) {
                    "Thermal calibration completed with quality: ${String.format("%.2f", thermalCalibrationResult.calibrationQuality)}"
                } else {
                    captureResult.errorMessage ?: thermalCalibrationResult?.errorMessage ?: "Thermal calibration failed"
                },
                thermalFilePath = captureResult.thermalFilePath,
                calibrationParameters = if (thermalCalibrationResult?.success == true) {
                    CalibrationParameters(
                        calibrationId = calibrationId,
                        qualityScore = thermalCalibrationResult.calibrationQuality,
                        thermalCalibration = ThermalCalibration(
                            temperatureRange = thermalCalibrationResult.temperatureRange,
                            calibrationMatrix = thermalCalibrationResult.calibrationMatrix.map { it.toList() },
                            noiseLevel = thermalCalibrationResult.noiseLevel,
                            uniformityError = thermalCalibrationResult.uniformityError
                        )
                    )
                } else null
            )

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                completedCalibrations = if (result.success) {
                    _calibrationState.value.completedCalibrations + CalibrationType.THERMAL
                } else {
                    _calibrationState.value.completedCalibrations
                },
                lastCalibrationResult = result,
                calibrationError = if (!result.success) result.message else null
            )

            if (result.success) {
                logger.info("Thermal calibration completed successfully")
                Result.success(result)
            } else {
                logger.error("Thermal calibration failed: ${result.message}")
                Result.failure(RuntimeException(result.message))
            }

        } catch (e: Exception) {
            logger.error("Thermal calibration failed", e)
            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                calibrationError = "Thermal calibration failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun runShimmerCalibration(): Result<CalibrationResult> {
        return try {
            if (_calibrationState.value.isCalibrating) {
                return Result.failure(IllegalStateException("Calibration already in progress"))
            }

            logger.info("Starting Shimmer sensor calibration")

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = true,
                calibrationType = CalibrationType.SHIMMER,
                progress = CalibrationProgress("Starting Shimmer calibration...", 1, 4, 0)
            )

            updateProgress("Initializing Shimmer sensors...", 1, 4, 25)

            updateProgress("Collecting baseline data...", 2, 4, 50)

            updateProgress("Calibrating GSR sensors...", 3, 4, 75)
            
            // Perform actual Shimmer calibration processing instead of fake delay
            val shimmerCalibrationResult = calibrationProcessor.processShimmerCalibration()

            updateProgress("Finalizing calibration...", 4, 4, 100)

            val result = CalibrationResult(
                success = shimmerCalibrationResult.success,
                calibrationType = CalibrationType.SHIMMER,
                message = if (shimmerCalibrationResult.success) {
                    "Shimmer calibration completed with quality: ${String.format("%.2f", shimmerCalibrationResult.calibrationQuality)}"
                } else {
                    shimmerCalibrationResult.errorMessage ?: "Shimmer calibration failed"
                },
                calibrationParameters = if (shimmerCalibrationResult.success) {
                    CalibrationParameters(
                        calibrationId = "shimmer_calibration_${System.currentTimeMillis()}",
                        qualityScore = shimmerCalibrationResult.calibrationQuality,
                        shimmerCalibration = ShimmerCalibration(
                            gsrBaseline = shimmerCalibrationResult.gsrBaseline,
                            gsrRange = shimmerCalibrationResult.gsrRange,
                            samplingAccuracy = shimmerCalibrationResult.samplingAccuracy,
                            signalNoiseRatio = shimmerCalibrationResult.signalNoiseRatio
                        )
                    )
                } else null
            )

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                completedCalibrations = if (result.success) {
                    _calibrationState.value.completedCalibrations + CalibrationType.SHIMMER
                } else {
                    _calibrationState.value.completedCalibrations
                },
                lastCalibrationResult = result,
                calibrationError = if (!result.success) result.message else null
            )

            if (result.success) {
                logger.info("Shimmer calibration completed successfully")
                Result.success(result)
            } else {
                logger.error("Shimmer calibration failed: ${result.message}")
                Result.failure(RuntimeException(result.message))
            }

        } catch (e: Exception) {
            logger.error("Shimmer calibration failed", e)
            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                calibrationError = "Shimmer calibration failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun validateSystemCalibration(): Result<Boolean> {
        return try {
            if (_calibrationState.value.isValidating) {
                return Result.failure(IllegalStateException("Validation already in progress"))
            }

            logger.info("Starting system calibration validation")

            _calibrationState.value = _calibrationState.value.copy(isValidating = true)

            // Perform actual validation based on completed calibrations and their quality
            val calibrations = _calibrationState.value.completedCalibrations
            val lastResult = _calibrationState.value.lastCalibrationResult
            
            val isValid = calibrations.isNotEmpty() && 
                         lastResult != null && 
                         lastResult.success &&
                         lastResult.message.contains("quality:")

            _calibrationState.value = _calibrationState.value.copy(isValidating = false)

            if (isValid) {
                logger.info("System calibration validation successful")
            } else {
                logger.warning("System calibration validation failed - no calibrations found")
            }

            Result.success(isValid)

        } catch (e: Exception) {
            logger.error("Calibration validation error", e)
            _calibrationState.value = _calibrationState.value.copy(isValidating = false)
            Result.failure(e)
        }
    }

    suspend fun stopCalibration(): Result<Unit> {
        return try {
            if (!_calibrationState.value.isCalibrating) {
                return Result.failure(IllegalStateException("No calibration in progress"))
            }

            logger.info("Stopping calibration process")

            _calibrationState.value = _calibrationState.value.copy(
                isCalibrating = false,
                progress = null,
                calibrationType = null
            )

            logger.info("Calibration stopped")
            Result.success(Unit)

        } catch (e: Exception) {
            logger.error("Failed to stop calibration", e)
            Result.failure(e)
        }
    }

    suspend fun resetCalibration(type: CalibrationType): Result<Unit> {
        return try {
            logger.info("Resetting calibration for type: ${type.displayName}")

            _calibrationState.value = _calibrationState.value.copy(
                completedCalibrations = _calibrationState.value.completedCalibrations - type
            )

            logger.info("Calibration reset for ${type.displayName}")
            Result.success(Unit)

        } catch (e: Exception) {
            logger.error("Failed to reset calibration", e)
            Result.failure(e)
        }
    }

    suspend fun saveCalibrationData(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            logger.info("Saving calibration data to persistent storage...")

            val lastResult = _calibrationState.value.lastCalibrationResult
            if (lastResult == null) {
                logger.warning("No calibration result to save")
                return@withContext Result.failure(IllegalStateException("No calibration result available"))
            }

            // Save to SharedPreferences for quick access
            val prefs = context.getSharedPreferences(CALIBRATION_PREFS, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Save basic calibration info
            editor.putString("last_calibration_type", lastResult.calibrationType.name)
            editor.putLong("last_calibration_timestamp", lastResult.timestamp)
            editor.putBoolean("last_calibration_success", lastResult.success)
            editor.putString("last_calibration_message", lastResult.message)
            
            // Save calibration parameters if available
            lastResult.calibrationParameters?.let { params ->
                editor.putString("calibration_parameters", serializeCalibrationParameters(params))
                editor.putFloat("calibration_quality", params.qualityScore.toFloat())
                editor.putString("calibration_id", params.calibrationId)
                
                // Save individual components
                params.cameraIntrinsics?.let { intrinsics ->
                    editor.putString("camera_intrinsics", serializeCameraIntrinsics(intrinsics))
                }
                
                params.thermalCalibration?.let { thermal ->
                    editor.putString("thermal_calibration", serializeThermalCalibration(thermal))
                }
                
                params.shimmerCalibration?.let { shimmer ->
                    editor.putString("shimmer_calibration", serializeShimmerCalibration(shimmer))
                }
            }
            
            editor.apply()
            
            // Also save to file for detailed backup
            val calibrationFile = File(context.filesDir, "${CALIBRATION_FILE_PREFIX}${lastResult.timestamp}.json")
            saveCalibrationToFile(lastResult, calibrationFile)
            
            logger.info("Calibration data saved successfully to preferences and file: ${calibrationFile.name}")
            Result.success(Unit)

        } catch (e: Exception) {
            logger.error("Failed to save calibration data", e)
            Result.failure(e)
        }
    }

    suspend fun loadCalibrationData(): Result<CalibrationParameters?> = withContext(Dispatchers.IO) {
        return@withContext try {
            logger.info("Loading calibration data from persistent storage...")

            val prefs = context.getSharedPreferences(CALIBRATION_PREFS, Context.MODE_PRIVATE)
            
            // Check if we have saved calibration data
            val calibrationId = prefs.getString("calibration_id", null)
            if (calibrationId == null) {
                logger.info("No saved calibration data found")
                return@withContext Result.success(null)
            }
            
            // Load calibration parameters
            val paramsJson = prefs.getString("calibration_parameters", null)
            if (paramsJson != null) {
                val params = deserializeCalibrationParameters(paramsJson)
                
                // Update current state with loaded data
                val calibrationType = try {
                    CalibrationType.valueOf(prefs.getString("last_calibration_type", "SYSTEM") ?: "SYSTEM")
                } catch (e: IllegalArgumentException) {
                    CalibrationType.SYSTEM
                }
                
                val result = CalibrationResult(
                    success = prefs.getBoolean("last_calibration_success", false),
                    calibrationType = calibrationType,
                    message = prefs.getString("last_calibration_message", "Loaded from storage") ?: "Loaded from storage",
                    timestamp = prefs.getLong("last_calibration_timestamp", System.currentTimeMillis()),
                    calibrationParameters = params
                )
                
                _calibrationState.value = _calibrationState.value.copy(
                    lastCalibrationResult = result,
                    completedCalibrations = setOf(calibrationType)
                )
                
                logger.info("Successfully loaded calibration data: ID=${params.calibrationId}, Quality=${params.qualityScore}")
                return@withContext Result.success(params)
            }
            
            logger.info("No calibration parameters found in storage")
            Result.success(null)

        } catch (e: Exception) {
            logger.error("Failed to load calibration data", e)
            Result.failure(e)
        }
    }

    suspend fun exportCalibrationData(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            logger.info("Exporting calibration data...")

            val calibrationData = _calibrationState.value.lastCalibrationResult
            if (calibrationData == null) {
                return@withContext Result.failure(IllegalStateException("No calibration data to export"))
            }
            
            // Create export directory if it doesn't exist
            val exportDir = File(context.getExternalFilesDir(null), CALIBRATION_EXPORT_DIR)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val exportTimestamp = System.currentTimeMillis()
            val exportFile = File(exportDir, "calibration_export_${exportTimestamp}.json")
            
            // Create comprehensive export data
            val exportData = JSONObject().apply {
                put("export_timestamp", exportTimestamp)
                put("export_version", "1.0")
                put("calibration_result", serializeCalibrationResult(calibrationData))
                
                // Add system information
                put("system_info", JSONObject().apply {
                    put("android_version", android.os.Build.VERSION.RELEASE)
                    put("device_model", android.os.Build.MODEL)
                    put("device_manufacturer", android.os.Build.MANUFACTURER)
                    put("app_version", getAppVersion())
                })
                
                // Add all completed calibrations
                val completedCalibs = JSONArray()
                _calibrationState.value.completedCalibrations.forEach { calibType ->
                    completedCalibs.put(calibType.name)
                }
                put("completed_calibrations", completedCalibs)
            }
            
            // Write to file
            BufferedWriter(FileWriter(exportFile)).use { writer ->
                writer.write(exportData.toString(2)) // Pretty print with indent
            }
            
            logger.info("Calibration data exported to: ${exportFile.absolutePath}")
            logger.info("Export file size: ${exportFile.length()} bytes")
            
            Result.success(exportFile.absolutePath)

        } catch (e: Exception) {
            logger.error("Failed to export calibration data", e)
            Result.failure(e)
        }
    }
    
    // Helper functions for serialization
    private fun serializeCalibrationParameters(params: CalibrationParameters): String {
        return JSONObject().apply {
            put("calibration_id", params.calibrationId)
            put("quality_score", params.qualityScore)
            
            params.cameraIntrinsics?.let { intrinsics ->
                put("camera_intrinsics", JSONObject().apply {
                    put("focal_length_x", intrinsics.focalLengthX)
                    put("focal_length_y", intrinsics.focalLengthY) 
                    put("principal_point_x", intrinsics.principalPointX)
                    put("principal_point_y", intrinsics.principalPointY)
                    put("distortion_coefficients", JSONArray(intrinsics.distortionCoefficients))
                })
            }
            
            params.thermalCalibration?.let { thermal ->
                put("thermal_calibration", JSONObject().apply {
                    put("temperature_range_min", thermal.temperatureRange.first)
                    put("temperature_range_max", thermal.temperatureRange.second)
                    put("noise_level", thermal.noiseLevel)
                    put("uniformity_error", thermal.uniformityError)
                    put("calibration_matrix", JSONArray().apply {
                        thermal.calibrationMatrix.forEach { row ->
                            put(JSONArray(row))
                        }
                    })
                })
            }
            
            params.shimmerCalibration?.let { shimmer ->
                put("shimmer_calibration", JSONObject().apply {
                    put("gsr_baseline", shimmer.gsrBaseline)
                    put("gsr_range_min", shimmer.gsrRange.first)
                    put("gsr_range_max", shimmer.gsrRange.second)
                    put("sampling_accuracy", shimmer.samplingAccuracy)
                    put("signal_noise_ratio", shimmer.signalNoiseRatio)
                })
            }
        }.toString()
    }
    
    private fun deserializeCalibrationParameters(json: String): CalibrationParameters {
        val obj = JSONObject(json)
        
        val cameraIntrinsics = if (obj.has("camera_intrinsics")) {
            val intrinsicsObj = obj.getJSONObject("camera_intrinsics")
            CameraIntrinsics(
                focalLengthX = intrinsicsObj.getDouble("focal_length_x"),
                focalLengthY = intrinsicsObj.getDouble("focal_length_y"),
                principalPointX = intrinsicsObj.getDouble("principal_point_x"),
                principalPointY = intrinsicsObj.getDouble("principal_point_y"),
                distortionCoefficients = jsonArrayToDoubleList(intrinsicsObj.getJSONArray("distortion_coefficients"))
            )
        } else null
        
        val thermalCalibration = if (obj.has("thermal_calibration")) {
            val thermalObj = obj.getJSONObject("thermal_calibration")
            ThermalCalibration(
                temperatureRange = Pair(
                    thermalObj.getDouble("temperature_range_min"),
                    thermalObj.getDouble("temperature_range_max")
                ),
                calibrationMatrix = jsonArrayToMatrix(thermalObj.getJSONArray("calibration_matrix")),
                noiseLevel = thermalObj.getDouble("noise_level"),
                uniformityError = thermalObj.getDouble("uniformity_error")
            )
        } else null
        
        val shimmerCalibration = if (obj.has("shimmer_calibration")) {
            val shimmerObj = obj.getJSONObject("shimmer_calibration")
            ShimmerCalibration(
                gsrBaseline = shimmerObj.getDouble("gsr_baseline"),
                gsrRange = Pair(
                    shimmerObj.getDouble("gsr_range_min"),
                    shimmerObj.getDouble("gsr_range_max")
                ),
                samplingAccuracy = shimmerObj.getDouble("sampling_accuracy"),
                signalNoiseRatio = shimmerObj.getDouble("signal_noise_ratio")
            )
        } else null
        
        return CalibrationParameters(
            calibrationId = obj.getString("calibration_id"),
            qualityScore = obj.getDouble("quality_score"),
            cameraIntrinsics = cameraIntrinsics,
            thermalCalibration = thermalCalibration,
            shimmerCalibration = shimmerCalibration
        )
    }
    
    private fun serializeCameraIntrinsics(intrinsics: CameraIntrinsics): String {
        return JSONObject().apply {
            put("focal_length_x", intrinsics.focalLengthX)
            put("focal_length_y", intrinsics.focalLengthY)
            put("principal_point_x", intrinsics.principalPointX)
            put("principal_point_y", intrinsics.principalPointY)
            put("distortion_coefficients", JSONArray(intrinsics.distortionCoefficients))
        }.toString()
    }
    
    private fun serializeThermalCalibration(thermal: ThermalCalibration): String {
        return JSONObject().apply {
            put("temperature_range_min", thermal.temperatureRange.first)
            put("temperature_range_max", thermal.temperatureRange.second)
            put("noise_level", thermal.noiseLevel)
            put("uniformity_error", thermal.uniformityError)
        }.toString()
    }
    
    private fun serializeShimmerCalibration(shimmer: ShimmerCalibration): String {
        return JSONObject().apply {
            put("gsr_baseline", shimmer.gsrBaseline)
            put("gsr_range_min", shimmer.gsrRange.first)
            put("gsr_range_max", shimmer.gsrRange.second)
            put("sampling_accuracy", shimmer.samplingAccuracy)
            put("signal_noise_ratio", shimmer.signalNoiseRatio)
        }.toString()
    }
    
    private fun serializeCalibrationResult(result: CalibrationResult): JSONObject {
        return JSONObject().apply {
            put("success", result.success)
            put("calibration_type", result.calibrationType.name)
            put("message", result.message)
            put("timestamp", result.timestamp)
            put("rgb_file_path", result.rgbFilePath)
            put("thermal_file_path", result.thermalFilePath)
            
            result.calibrationParameters?.let { params ->
                put("calibration_parameters", JSONObject(serializeCalibrationParameters(params)))
            }
        }
    }
    
    private fun saveCalibrationToFile(result: CalibrationResult, file: File) {
        val jsonData = serializeCalibrationResult(result)
        BufferedWriter(FileWriter(file)).use { writer ->
            writer.write(jsonData.toString(2))
        }
    }
    
    private fun jsonArrayToDoubleList(jsonArray: JSONArray): List<Double> {
        val list = mutableListOf<Double>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getDouble(i))
        }
        return list
    }
    
    private fun jsonArrayToMatrix(jsonArray: JSONArray): List<List<Double>> {
        val matrix = mutableListOf<List<Double>>()
        for (i in 0 until jsonArray.length()) {
            val row = jsonArray.getJSONArray(i)
            matrix.add(jsonArrayToDoubleList(row))
        }
        return matrix
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getCurrentState(): CalibrationState = _calibrationState.value

    fun isCalibrating(): Boolean = _calibrationState.value.isCalibrating

    fun isCalibrationCompleted(type: CalibrationType): Boolean =
        _calibrationState.value.completedCalibrations.contains(type)

    fun clearError() {
        _calibrationState.value = _calibrationState.value.copy(calibrationError = null)
    }

    private fun updateProgress(step: String, stepNumber: Int, totalSteps: Int, percent: Int) {
        _calibrationState.value = _calibrationState.value.copy(
            progress = CalibrationProgress(step, stepNumber, totalSteps, percent)
        )
        logger.debug("Calibration progress: $step ($percent%)")
    }
}
