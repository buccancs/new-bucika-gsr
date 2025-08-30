package com.topdon.tc001.recording

class EnhancedVideoRecorder {
    
    var isRecording: Boolean = false
        private set
    
    fun startRecording() {
        isRecording = true
    }
    
    fun stopRecording() {
        isRecording = false
    }
    
    fun cleanup() {
        isRecording = false
    }
    
    enum class RecordingMode {
        SAMSUNG_4K_30FPS,
        STANDARD_1080P_30FPS,
        HIGH_SPEED_720P_60FPS,
        THERMAL_640x480_30FPS
    }
}