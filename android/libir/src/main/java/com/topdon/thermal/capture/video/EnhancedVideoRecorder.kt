package com.topdon.thermal.capture.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.MediaRecorder.VideoEncoder
import android.media.MediaRecorder.AudioEncoder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.elvishew.xlog.XLog
import com.topdon.lib.core.config.FileConfig
import com.topdon.thermal.overlay.TemperatureOverlayManager
import com.topdon.thermal.capture.raw.DNGCaptureManager
import com.topdon.thermal.device.compatibility.DeviceCompatibilityChecker
import com.topdon.thermal.device.compatibility.S22OptimizationParams
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class EnhancedVideoRecorder(
    private val context: Context,
    private val thermalView: TextureView?,
    private val visualView: TextureView?
) {

    enum class RecordingMode {
        SAMSUNG_4K_30FPS,
        RAD_DNG_LEVEL3_30FPS,  
        PARALLEL_DUAL_STREAM,
        CONCURRENT_4K_AND_RAW
    }

    private var isRecording = false
    private var recordingMode: RecordingMode = RecordingMode.SAMSUNG_4K_30FPS

    private val compatibilityChecker = DeviceCompatibilityChecker(context)
    private val optimizationParams: S22OptimizationParams = compatibilityChecker.getSamsungS22OptimizationParams()

    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private val cameraOpenCloseLock = Semaphore(1)

    private var mediaRecorder: MediaRecorder? = null
    private var thermalRecorder: MediaRecorder? = null
    private var visualRecorder: MediaRecorder? = null
    
    private var dngCaptureManager: DNGCaptureManager? = null
    
    private var syncSystem: SynchronizedCaptureSystem? = null

    private val samsung4KSize = Size(3840, 2160)
    private val radDngSize = Size(1920, 1080)
    private val targetFps = 30

    private var currentVideoFile: File? = null
    private var currentThermalFile: File? = null

    private val overlayManager = TemperatureOverlayManager()

    companion object {
        private const val TAG = "EnhancedVideoRecorder"
        
        private const val SAMSUNG_CAMERA_ID = "0"
        
        private const val SAMSUNG_4K_BITRATE = 20000000
        
        private const val RAD_DNG_BITRATE = 8000000
    }
    }

    init {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        dngCaptureManager = DNGCaptureManager(context)
        
        XLog.i(TAG, "Enhanced Video Recorder initialized:")
        XLog.i(TAG, "- Samsung S22: ${compatibilityChecker.isSamsungS22()}")
        XLog.i(TAG, "- 4K30fps support: ${compatibilityChecker.supports4K30fps()}")
        XLog.i(TAG, "- RAW capture support: ${compatibilityChecker.supportsRawCapture()}")
        XLog.i(TAG, "- Concurrent 4K+RAW: ${compatibilityChecker.supportsConcurrent4KAndRaw()}")
        XLog.i(TAG, "- Optimization params: $optimizationParams")
    }
    
    fun getCompatibilityInfo(): String {
        return buildString {
            appendLine("Device Compatibility Report:")
            appendLine("- Model: ${Build.MODEL}")
            appendLine("- Samsung S22 Series: ${compatibilityChecker.isSamsungS22()}")
            appendLine("- 4K 30fps Support: ${compatibilityChecker.supports4K30fps()}")
            appendLine("- RAW DNG Support: ${compatibilityChecker.supportsRawCapture()}")
            appendLine("- Concurrent 4K+RAW: ${compatibilityChecker.supportsConcurrent4KAndRaw()}")
            appendLine("- Max Concurrent Streams: ${compatibilityChecker.getMaxConcurrentStreams()}")
            appendLine("- Recommended 4K Bitrate: ${optimizationParams.recommended4KBitrate / 1_000_000} Mbps")
        }
    }
    
    fun validateRecordingMode(mode: RecordingMode): Pair<Boolean, String> {
        return when (mode) {
            RecordingMode.SAMSUNG_4K_30FPS -> {
                val result = compatibilityChecker.validateConcurrentConfiguration(true, false, 30)
                Pair(result.isSupported, result.issues.joinToString("; "))
            }
            RecordingMode.RAD_DNG_LEVEL3_30FPS -> {
                val result = compatibilityChecker.validateConcurrentConfiguration(false, true, 30)
                Pair(result.isSupported, result.issues.joinToString("; "))
            }
            RecordingMode.CONCURRENT_4K_AND_RAW -> {
                val result = compatibilityChecker.validateConcurrentConfiguration(true, true, 30)
                Pair(result.isSupported, result.issues.joinToString("; "))
            }
            RecordingMode.PARALLEL_DUAL_STREAM -> {

                val has4K = compatibilityChecker.supports4K30fps()
                val message = if (!has4K) "4K recording not supported" else ""
                Pair(has4K, message)
            }
        }
    }

    fun startRecording(mode: RecordingMode, syncSystem: SynchronizedCaptureSystem? = null): Boolean {
        if (isRecording) {
            XLog.w(TAG, "Recording already in progress")
            return false
        }

        this.syncSystem = syncSystem

        val (isSupported, issues) = validateRecordingMode(mode)
        if (!isSupported) {
            XLog.e(TAG, "Recording mode $mode not supported: $issues")
            return false
        }

        recordingMode = mode
        
        return try {
            setupBackgroundThread()
            
            when (mode) {
                RecordingMode.SAMSUNG_4K_30FPS -> startSamsung4KRecording()
                RecordingMode.RAD_DNG_LEVEL3_30FPS -> startRadDngRecording()
                RecordingMode.PARALLEL_DUAL_STREAM -> startParallelRecording()
                RecordingMode.CONCURRENT_4K_AND_RAW -> startConcurrent4KAndRawRecording()
            }
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start recording: ${e.message}", e)
            false
        }
    }
    }

    private fun startSamsung4KRecording(): Boolean {
        XLog.i(TAG, "Starting Samsung 4K 30FPS recording with optimizations")
        
        val videoFile = createVideoFile("samsung_4k_${Date().time}.mp4")
        currentVideoFile = videoFile
        
        mediaRecorder = MediaRecorder().apply {

            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(videoFile.absolutePath)
            
            setVideoEncoder(VideoEncoder.H264)
            setVideoSize(samsung4KSize.width, samsung4KSize.height)
            setVideoFrameRate(targetFps)
            setVideoEncodingBitRate(optimizationParams.recommended4KBitrate)
            
            setAudioEncoder(AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            
            try {
                prepare()
                start()
                
                syncSystem?.let { sync ->
                    startVideoFrameTimestampEstimation(sync)
                }
                
                isRecording = true
                XLog.i(TAG, "Samsung 4K recording started successfully with ${optimizationParams.recommended4KBitrate / 1_000_000} Mbps bitrate")
                return true
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to start Samsung 4K recording: ${e.message}", e)
                releaseMediaRecorder()
                return false
            }
        }
        
        return false
    }

    private fun startConcurrent4KAndRawRecording(): Boolean {
        if (!compatibilityChecker.isSamsungS22()) {
            XLog.e(TAG, "Concurrent 4K+RAW recording requires Samsung S22 series device")
            return false
        }
        
        XLog.i(TAG, "Starting concurrent 4K + RAW DNG recording on Samsung S22")
        
        val videoSuccess = startSamsung4KRecording()
        if (!videoSuccess) {
            XLog.e(TAG, "Failed to start 4K video for concurrent recording")
            return false
        }
        
        val rawSuccess = dngCaptureManager?.startConcurrentDNGCapture() ?: false
        if (!rawSuccess) {
            XLog.e(TAG, "Failed to start concurrent RAW DNG capture")

            stopRecording()
            return false
        }
        
        isRecording = true
        XLog.i(TAG, "Concurrent 4K + RAW DNG recording started successfully on Samsung S22")
        return true
    }

    private fun startRadDngRecording(): Boolean {
        XLog.i(TAG, "Starting STAGE 3/LEVEL 3 RAD DNG recording at 30FPS")
        
        return try {
            val success = dngCaptureManager?.startDNGCapture() ?: false
            if (success) {
                isRecording = true
                XLog.i(TAG, "RAD DNG Level 3 capture started successfully")
            } else {
                XLog.e(TAG, "Failed to start RAD DNG capture")
            }
            success
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start RAD DNG recording: ${e.message}", e)
            false
        }
    }

    private fun startParallelRecording(): Boolean {
        XLog.i(TAG, "Starting parallel dual stream recording")
        
        val thermalFile = createVideoFile("thermal_${Date().time}.mp4")
        val visualFile = createVideoFile("visual_${Date().time}.mp4")
        
        currentVideoFile = visualFile
        currentThermalFile = thermalFile
        
        thermalRecorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(thermalFile.absolutePath)
            
            setVideoEncoder(VideoEncoder.H264)
            setVideoSize(radDngSize.width, radDngSize.height)
            setVideoFrameRate(targetFps)
            setVideoEncodingBitRate(RAD_DNG_BITRATE)
        }
        
        visualRecorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(visualFile.absolutePath)
            
            setVideoEncoder(VideoEncoder.H264)
            setVideoSize(samsung4KSize.width, samsung4KSize.height)
            setVideoFrameRate(targetFps)
            setVideoEncodingBitRate(SAMSUNG_4K_BITRATE)
            
            setAudioEncoder(AudioEncoder.AAC)
            setAudioSamplingRate(48000)
            setAudioEncodingBitRate(192000)
        }
        
        return try {
            thermalRecorder?.prepare()
            visualRecorder?.prepare()
            
            thermalRecorder?.start()
            visualRecorder?.start()
            
            isRecording = true
            XLog.i(TAG, "Parallel dual stream recording started successfully")
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start parallel recording: ${e.message}", e)
            releaseAllRecorders()
            false
        }
    }

    fun stopRecording(): Boolean {
        if (!isRecording) {
            XLog.w(TAG, "No recording in progress")
            return false
        }
        
        return try {
            when (recordingMode) {
                RecordingMode.PARALLEL_DUAL_STREAM -> stopParallelRecording()
                RecordingMode.RAD_DNG_LEVEL3_30FPS -> stopDNGRecording()
                else -> stopSingleRecording()
            }
            
            isRecording = false
            stopBackgroundThread()
            
            XLog.i(TAG, "Recording stopped successfully")
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to stop recording: ${e.message}", e)
            false
        }
    }

    private fun stopDNGRecording() {
        val success = dngCaptureManager?.stopDNGCapture() ?: false
        if (success) {
            XLog.i(TAG, "DNG capture stopped successfully")
        } else {
            XLog.w(TAG, "Failed to stop DNG capture cleanly")
        }
    }

    private fun stopSingleRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    private fun stopParallelRecording() {
        thermalRecorder?.apply {
            stop()
            release()
        }
        visualRecorder?.apply {
            stop()
            release()
        }
        thermalRecorder = null
        visualRecorder = null
    }

    fun addTemperatureOverlay(
        frame: Bitmap, 
        temperature: Float, 
        x: Float, 
        y: Float,
        gsrValue: Double? = null,
        skinTemperature: Double? = null
    ): Bitmap {
        val mode = when (recordingMode) {
            RecordingMode.SAMSUNG_4K_30FPS -> "Samsung 4K"
            RecordingMode.RAD_DNG_LEVEL3_30FPS -> "RAD DNG Level 3"
            RecordingMode.PARALLEL_DUAL_STREAM -> "Parallel Dual"
        }
        
        return overlayManager.addOverlayToFrame(
            frame, temperature, x, y, gsrValue, skinTemperature,
            System.currentTimeMillis(), mode
        )
    }

    fun addDualStreamOverlays(
        thermalFrame: Bitmap,
        visualFrame: Bitmap,
        temperature: Float,
        x: Float,
        y: Float,
        gsrValue: Double? = null,
        skinTemperature: Double? = null
    ): Pair<Bitmap, Bitmap> {
        return overlayManager.addDualStreamOverlay(
            thermalFrame, visualFrame, temperature, x, y, gsrValue, skinTemperature
        )
    }

    fun isRecording(): Boolean = isRecording

    fun getRecordingMode(): RecordingMode = recordingMode

    fun getRecordedFiles(): List<File> {
        val files = mutableListOf<File>()
        currentVideoFile?.let { files.add(it) }
        currentThermalFile?.let { files.add(it) }
        
        if (recordingMode == RecordingMode.RAD_DNG_LEVEL3_30FPS) {
            val dngFiles = dngCaptureManager?.getCapturedFiles() ?: emptyList()
            files.addAll(dngFiles)
        }
        
        return files
    }
    
    fun getDNGCaptureStats(): Map<String, Any> {
        return dngCaptureManager?.getCaptureStats() ?: emptyMap()
    }

    private fun createVideoFile(filename: String): File {
        val videoDir = File(FileConfig.lineGalleryDir, "enhanced_recordings")
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }
        return File(videoDir, filename)
    }

    private fun setupBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            XLog.e(TAG, "Error stopping background thread", e)
        }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun releaseAllRecorders() {
        releaseMediaRecorder()
        thermalRecorder?.release()
        thermalRecorder = null
        visualRecorder?.release()
        visualRecorder = null
    }

    fun cleanup() {
        stopRecording()
        releaseAllRecorders()
        dngCaptureManager?.cleanup()
        stopBackgroundThread()
    }
    
    private fun startVideoFrameTimestampEstimation(syncSystem: SynchronizedCaptureSystem) {
        backgroundHandler?.post {
            val frameIntervalMs = 1000L / targetFps
            var frameCount = 0L
            
            val estimationRunnable = object : Runnable {
                override fun run() {
                    if (isRecording) {

                        val presentationTimeUs = frameCount * (frameIntervalMs * 1000)
                        syncSystem.registerVideoFrame(presentationTimeUs)
                        
                        frameCount++
                        
                        backgroundHandler?.postDelayed(this, frameIntervalMs)
                    }
                }
            }
            
            backgroundHandler?.post(estimationRunnable)
            XLog.d(TAG, "Started video frame timestamp estimation at ${targetFps}fps")
        }
    }
