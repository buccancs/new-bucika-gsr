package com.topdon.tc001.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import com.elvishew.xlog.XLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EnhancedVideoRecorder(private val context: Context? = null) {
    
    companion object {
        private const val TAG = "EnhancedVideoRecorder"
        private const val VIDEO_QUALITY_720P = 720
        private const val VIDEO_QUALITY_1080P = 1080
        private const val VIDEO_QUALITY_4K = 2160
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    
    var isRecording: Boolean = false
        private set
    
    fun startRecording(): Boolean {
        return startRecording(RecordingMode.STANDARD_1080P_30FPS)
    }
    
    fun startRecording(mode: RecordingMode): Boolean {
        if (isRecording) {
            XLog.w(TAG, "Recording already in progress")
            return false
        }
        
        return try {
            setupMediaRecorder(mode)
            mediaRecorder?.start()
            isRecording = true
            XLog.i(TAG, "Recording started with mode: $mode")
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start recording: ${e.message}", e)
            cleanup()
            false
        }
    }
    
    fun stopRecording(): Boolean {
        if (!isRecording) {
            XLog.w(TAG, "No recording in progress")
            return false
        }
        
        return try {
            mediaRecorder?.stop()
            isRecording = false
            XLog.i(TAG, "Recording stopped successfully")
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to stop recording: ${e.message}", e)
            false
        } finally {
            cleanup()
        }
    }
    
    fun getRecordedFiles(): List<File> {
        return outputFile?.let { 
            if (it.exists()) listOf(it) else emptyList() 
        } ?: emptyList()
    }
    
    fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            XLog.w(TAG, "Error during cleanup: ${e.message}")
        }
        isRecording = false
    }
    
    private fun setupMediaRecorder(mode: RecordingMode) {
        mediaRecorder = MediaRecorder().apply {
            // Setup video source
            setVideoSource(MediaRecorder.VideoSource.DEFAULT)
            
            // Set output format
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            
            // Configure video settings based on mode
            when (mode) {
                RecordingMode.SAMSUNG_4K_30FPS -> {
                    setVideoSize(3840, 2160)
                    setVideoFrameRate(30)
                    setVideoEncodingBitRate(20000000) // 20 Mbps for 4K
                }
                RecordingMode.STANDARD_1080P_30FPS -> {
                    setVideoSize(1920, 1080)
                    setVideoFrameRate(30)
                    setVideoEncodingBitRate(10000000) // 10 Mbps for 1080p
                }
                RecordingMode.HIGH_SPEED_720P_60FPS -> {
                    setVideoSize(1280, 720)
                    setVideoFrameRate(60)
                    setVideoEncodingBitRate(8000000) // 8 Mbps for 720p@60fps
                }
                RecordingMode.THERMAL_640x480_30FPS -> {
                    setVideoSize(640, 480)
                    setVideoFrameRate(30)
                    setVideoEncodingBitRate(2000000) // 2 Mbps for thermal
                }
            }
            
            // Set video encoder
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            
            // Set output file
            outputFile = createOutputFile(mode)
            setOutputFile(outputFile?.absolutePath)
            
            // Prepare the recorder
            prepare()
        }
    }
    
    private fun createOutputFile(mode: RecordingMode): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val modePrefix = when (mode) {
            RecordingMode.SAMSUNG_4K_30FPS -> "4K"
            RecordingMode.STANDARD_1080P_30FPS -> "1080P"
            RecordingMode.HIGH_SPEED_720P_60FPS -> "720P_60"
            RecordingMode.THERMAL_640x480_30FPS -> "THERMAL"
        }
        
        val fileName = "VIDEO_${modePrefix}_${timestamp}.mp4"
        val videoDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "BucikaGSR")
        
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }
        
        return File(videoDir, fileName)
    }
    
    enum class RecordingMode {
        SAMSUNG_4K_30FPS,
        STANDARD_1080P_30FPS,
        HIGH_SPEED_720P_60FPS,
        THERMAL_640x480_30FPS
    }
}