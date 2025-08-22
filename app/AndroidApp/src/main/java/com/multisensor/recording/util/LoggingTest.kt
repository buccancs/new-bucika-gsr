package com.multisensor.recording.util
import android.content.Context
import android.os.Build
object LoggingTest {
    fun runLoggingTest(context: Context) {
        AppLogger.i("LoggingTest", "=== Android Logging System Test Starting ===")
        AppLogger.i("LoggingTest", "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        AppLogger.i("LoggingTest", "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        AppLogger.v("LoggingTest", "This is a VERBOSE message")
        AppLogger.d("LoggingTest", "This is a DEBUG message")
        AppLogger.i("LoggingTest", "This is an INFO message")
        AppLogger.w("LoggingTest", "This is a WARNING message")
        AppLogger.e("LoggingTest", "This is an ERROR message")
        try {
            throw RuntimeException("Test exception for logging")
        } catch (e: Exception) {
            AppLogger.logError("LoggingTest", "exception handling", e)
        }
        AppLogger.logMethodEntry("LoggingTest", "testMethod", "param1", "param2")
        AppLogger.logMethodExit("LoggingTest", "testMethod", "success")
        AppLogger.logLifecycle("LoggingTest", "onCreate", "TestActivity")
        AppLogger.logLifecycle("LoggingTest", "onStart", "TestActivity")
        AppLogger.logLifecycle("LoggingTest", "onResume", "TestActivity")
        AppLogger.logNetwork("LoggingTest", "HTTP GET", "https://api.example.com/test")
        AppLogger.logNetwork("LoggingTest", "HTTP POST", "https://api.example.com/data")
        AppLogger.logRecording("LoggingTest", "start recording", "camera_front")
        AppLogger.logRecording("LoggingTest", "stop recording", "camera_front")
        AppLogger.logSensor("LoggingTest", "reading", "accelerometer", "x=1.2, y=0.8, z=9.8")
        AppLogger.logSensor("LoggingTest", "connected", "GSR sensor")
        AppLogger.logFile("LoggingTest", "save", "test_video.mp4", 1024L * 1024 * 50)
        AppLogger.logFile("LoggingTest", "delete", "old_file.txt")
        AppLogger.startTiming("LoggingTest", "video_processing")
        Thread.sleep(100)
        AppLogger.endTiming("LoggingTest", "video_processing")
        AppLogger.logMemoryUsage("LoggingTest", "After test operations")
        AppLogger.logThreadInfo("LoggingTest", "Test thread status")
        AppLogger.logStateChange("LoggingTest", "CameraRecorder", "IDLE", "RECORDING")
        AppLogger.logStateChange("LoggingTest", "NetworkClient", "DISCONNECTED", "CONNECTED")
        val testObject = TestClass()
        testObject.logI("Extension function test message")
        testObject.logD("Debug message from extension")
        testObject.logW("Warning message with extension")
        AppLogger.i("LoggingTest", "Testing debug mode enable")
        AppLogger.setDebugEnabled(true)
        AppLogger.d("LoggingTest", "This debug message should be visible")
        AppLogger.setVerboseEnabled(true)
        AppLogger.v("LoggingTest", "This verbose message should be visible")
        AppLogger.setDebugEnabled(false)
        AppLogger.d("LoggingTest", "This debug message should be filtered out")
        AppLogger.setVerboseEnabled(false)
        AppLogger.v("LoggingTest", "This verbose message should be filtered out")
        AppLogger.setDebugEnabled(true)
        AppLogger.setVerboseEnabled(false)
        AppLogger.i("LoggingTest", "=== Android Logging System Test Completed ===")
    }
    private class TestClass {
    }
}