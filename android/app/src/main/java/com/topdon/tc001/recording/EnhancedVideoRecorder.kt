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
}