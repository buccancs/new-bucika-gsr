package com.multisensor.recording.util
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
object LoggingTestUtility {
    private const val TAG = "LoggingTestUtility"
    fun runComprehensiveLoggingTest(context: Context) {
        AppLogger.logMethodEntry(TAG, "runComprehensiveLoggingTest", "Starting complete logging test")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                testBasicLogging()
                testSpecializedLogging()
                testPerformanceLogging(context)
                testMemoryLogging(context)
                testStateChangeLogging()
                AppLogger.i(TAG, "✅ All Android logging tests PASSED")
            } catch (e: Exception) {
                AppLogger.e(TAG, "❌ Android logging tests FAILED", e)
            }
        }
    }
    private fun testBasicLogging() {
        AppLogger.d(TAG, "Debug message test")
        AppLogger.i(TAG, "Info message test")
        AppLogger.w(TAG, "Warning message test")
        AppLogger.e(TAG, "Error message test")
        AppLogger.v(TAG, "Verbose message test")
    }
    private fun testSpecializedLogging() {
        AppLogger.logLifecycle(TAG, "test_lifecycle", "Testing lifecycle logging")
        AppLogger.logNetwork(TAG, "test_request", "https://api.example.com/test")
        AppLogger.logRecording(TAG, "test_recording", "1920x1080@30fps")
        AppLogger.logSensor(TAG, "test_sensor", "GSR", "value=1.23")
        AppLogger.logFile(TAG, "test_file", "test.mp4", 1024L)
        AppLogger.logStateChange(TAG, "test_state", "IDLE", "RECORDING")
    }
    private fun testPerformanceLogging(context: Context) {
        AppLogger.startTiming(TAG, "test_operation")
        Thread.sleep(100)
        AppLogger.endTiming(TAG, "test_operation")
        AppLogger.logMemoryUsage(TAG, "After performance test")
    }
    private fun testMemoryLogging(context: Context) {
        AppLogger.logMemoryUsage(TAG, "Memory test start")
        val testData = IntArray(1000) { it }
        AppLogger.logMemoryUsage(TAG, "Memory test end")
    }
    private fun testStateChangeLogging() {
        val states = listOf("INITIALIZING", "READY", "RECORDING", "PROCESSING", "COMPLETE")
        for (i in 0 until states.size - 1) {
            AppLogger.logStateChange(TAG, "test_component", states[i], states[i + 1])
            Thread.sleep(50)
        }
    }
    fun validateLoggingIntegration(): Boolean {
        AppLogger.logMethodEntry(TAG, "validateLoggingIntegration", "Validating logging integration")
        return try {
            AppLogger.i(TAG, "Logging integration validation")
            AppLogger.logMethodEntry(TAG, "test", "test")
            AppLogger.startTiming(TAG, "validation")
            AppLogger.endTiming(TAG, "validation")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Logging integration validation failed", e)
            false
        }
    }
}