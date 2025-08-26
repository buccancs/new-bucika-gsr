package com.topdon.thermal.device.compatibility

import android.content.Context
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageFormat
import android.os.Build
import android.util.Size
import com.elvishew.xlog.XLog

class DeviceCompatibilityChecker(private val context: Context) {

    companion object {
        private const val TAG = "DeviceCompatibilityChecker"
        
        private val SAMSUNG_S22_MODELS = setOf(
            "SM-S901B", "SM-S901U", "SM-S901W", "SM-S901N",
            "SM-S906B", "SM-S906U", "SM-S906W", "SM-S906N",
            "SM-S908B", "SM-S908U", "SM-S908W", "SM-S908N"
        )
        
        private val TARGET_4K_SIZE = Size(3840, 2160)
        private const val TARGET_FPS = 30
        
        private const val S22_MAX_CONCURRENT_STREAMS = 3
        private const val S22_MAX_RAW_BUFFER_SIZE = 8
    }

    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun isSamsungS22(): Boolean {
        val model = Build.MODEL
        val isSamsungS22 = SAMSUNG_S22_MODELS.contains(model)
        XLog.i(TAG, "Device model: $model, Samsung S22: $isSamsungS22")
        return isSamsungS22
    }

    fun supports4K30fps(): Boolean {
        return try {
            val cameraIds = cameraManager.cameraIdList
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    streamConfigMap?.let { configMap ->

                        val outputSizes = configMap.getOutputSizes(ImageFormat.YUV_420_888)
                        val supports4K = outputSizes.any { size ->
                            size.width >= TARGET_4K_SIZE.width && size.height >= TARGET_4K_SIZE.height
                        }
                        
                        if (supports4K) {

                            val fpsRanges = configMap.getHighSpeedVideoFpsRanges()
                            val supports30fps = fpsRanges.any { range ->
                                range.lower <= TARGET_FPS && range.upper >= TARGET_FPS
                            }
                            
                            XLog.i(TAG, "Camera $cameraId: 4K support=$supports4K, 30fps support=$supports30fps")
                            return supports4K && supports30fps
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            XLog.e(TAG, "Error checking 4K30fps support: ${e.message}", e)
            false
        }
    }

    fun supportsRawCapture(): Boolean {
        return try {
            val cameraIds = cameraManager.cameraIdList
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    streamConfigMap?.let { configMap ->
                        val rawSizes = configMap.getOutputSizes(ImageFormat.RAW_SENSOR)
                        val supportsRaw = rawSizes != null && rawSizes.isNotEmpty()
                        
                        XLog.i(TAG, "Camera $cameraId: RAW support=$supportsRaw")
                        return supportsRaw
                    }
                }
            }
            false
        } catch (e: Exception) {
            XLog.e(TAG, "Error checking RAW support: ${e.message}", e)
            false
        }
    }

    fun supportsConcurrent4KAndRaw(): Boolean {
        if (!isSamsungS22()) {
            XLog.w(TAG, "Device is not Samsung S22 series, concurrent capture may not be optimized")
            return false
        }

        return try {
            val cameraIds = cameraManager.cameraIdList
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {

                    val maxInputStreams = characteristics.get(CameraCharacteristics.REQUEST_MAX_NUM_INPUT_STREAMS) ?: 0
                    val maxOutputStreams = characteristics.get(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS_RAW) ?: 0
                    
                    val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    streamConfigMap?.let { configMap ->
                        val supports4K = supports4K30fps()
                        val supportsRaw = supportsRawCapture()
                        val supportsMultipleStreams = maxOutputStreams >= 2
                        
                        val result = supports4K && supportsRaw && supportsMultipleStreams
                        XLog.i(TAG, "Concurrent 4K+RAW check - 4K:$supports4K, RAW:$supportsRaw, MultiStream:$supportsMultipleStreams, Result:$result")
                        return result
                    }
                }
            }
            false
        } catch (e: Exception) {
            XLog.e(TAG, "Error checking concurrent 4K+RAW support: ${e.message}", e)
            false
        }
    }

    fun getMaxConcurrentStreams(): Int {
        return try {
            val cameraIds = cameraManager.cameraIdList
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val maxOutputStreams = characteristics.get(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS_RAW) ?: 1
                    val maxProcessedStreams = characteristics.get(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS_PROCESSED) ?: 1
                    
                    val maxStreams = maxOf(maxOutputStreams, maxProcessedStreams)
                    XLog.i(TAG, "Max concurrent streams: $maxStreams")
                    return maxStreams
                }
            }
            1
        } catch (e: Exception) {
            XLog.e(TAG, "Error getting max concurrent streams: ${e.message}", e)
            1
        }
    }

    fun getSamsungS22OptimizationParams(): S22OptimizationParams {
        return if (isSamsungS22()) {
            S22OptimizationParams(
                maxConcurrentStreams = S22_MAX_CONCURRENT_STREAMS,
                maxRawBufferSize = S22_MAX_RAW_BUFFER_SIZE,
                recommended4KBitrate = 20_000_000,
                recommendedRawFormat = ImageFormat.RAW_SENSOR,
                enableHardwareAcceleration = true,
                enableZeroCopyBuffer = true
            )
        } else {

            S22OptimizationParams(
                maxConcurrentStreams = 2,
                maxRawBufferSize = 4,
                recommended4KBitrate = 15_000_000,
                recommendedRawFormat = ImageFormat.RAW_SENSOR,
                enableHardwareAcceleration = false,
                enableZeroCopyBuffer = false
            )
        }
    }

    fun validateConcurrentConfiguration(
        enable4K: Boolean,
        enableRaw: Boolean,
        targetFps: Int
    ): CaptureCompatibilityResult {
        
        val issues = mutableListOf<String>()
        
        if (enable4K && !supports4K30fps()) {
            issues.add("4K recording not supported on this device")
        }
        
        if (enableRaw && !supportsRawCapture()) {
            issues.add("RAW capture not supported on this device")
        }
        
        if (enable4K && enableRaw && !supportsConcurrent4KAndRaw()) {
            issues.add("Concurrent 4K + RAW capture not supported on this device")
        }
        
        if (targetFps > TARGET_FPS) {
            issues.add("Frame rate $targetFps not supported, maximum is $TARGET_FPS")
        }
        
        val maxStreams = getMaxConcurrentStreams()
        val requiredStreams = (if (enable4K) 1 else 0) + (if (enableRaw) 1 else 0)
        if (requiredStreams > maxStreams) {
            issues.add("Required streams ($requiredStreams) exceed maximum concurrent streams ($maxStreams)")
        }
        
        return CaptureCompatibilityResult(
            isSupported = issues.isEmpty(),
            issues = issues,
            optimizationParams = getSamsungS22OptimizationParams()
        )
    }
}

data class S22OptimizationParams(
    val maxConcurrentStreams: Int,
    val maxRawBufferSize: Int,
    val recommended4KBitrate: Int,
    val recommendedRawFormat: Int,
    val enableHardwareAcceleration: Boolean,
    val enableZeroCopyBuffer: Boolean
)

data class CaptureCompatibilityResult(
    val isSupported: Boolean,
    val issues: List<String>,
    val optimizationParams: S22OptimizationParams
