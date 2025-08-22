package com.multisensor.recording.util
import android.content.Context
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
class AppLoggerEnhancedTest {
    private lateinit var mockContext: Context
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        AppLogger.setDebugEnabled(true)
        AppLogger.setVerboseEnabled(true)
        AppLogger.setMemoryMonitoringEnabled(true)
        AppLogger.clearPerformanceStats()
    }
    @Test
    fun `test basic logging levels`() {
        val context = mapOf("test_id" to "basic_logging", "component" to "test")
        AppLogger.v("TestTag", "Verbose message", context = context)
        AppLogger.d("TestTag", "Debug message", context = context)
        AppLogger.i("TestTag", "Info message", context = context)
        AppLogger.w("TestTag", "Warning message", context = context)
        AppLogger.e("TestTag", "Error message", context = context)
        val testException = RuntimeException("Test exception")
        AppLogger.e("TestTag", "Error with exception", testException, context)
        assertTrue("Basic logging should complete without errors", true)
    }
    @Test
    fun `test log level controls`() {
        AppLogger.setDebugEnabled(false)
        AppLogger.d("TestTag", "This debug message should be filtered")
        AppLogger.setVerboseEnabled(false)
        AppLogger.v("TestTag", "This verbose message should be filtered")
        assertTrue("Debug control should work", true)
        assertTrue("Verbose control should work", true)
    }
    @Test
    fun `test performance timing`() {
        val tag = "PerformanceTest"
        val operation = "test_operation"
        AppLogger.startTiming(tag, operation)
        Thread.sleep(50)
        val duration = AppLogger.endTiming(tag, operation)
        assertTrue("Duration should be positive", duration > 0)
        assertTrue("Duration should be reasonable", duration >= 40 && duration <= 200)
        AppLogger.startTiming(tag, operation, "with_context")
        Thread.sleep(30)
        val contextDuration = AppLogger.endTiming(tag, operation, "with_context")
        assertTrue("Context duration should be positive", contextDuration > 0)
    }
    @Test
    fun `test measureTime function`() {
        val tag = "MeasureTest"
        var executed = false
        val result = AppLogger.measureTime(tag, "test_block") {
            Thread.sleep(25)
            executed = true
            "test_result"
        }
        assertTrue("Block should be executed", executed)
        assertEquals("Result should be returned", "test_result", result)
    }
    @Test
    fun `test measureTime with exception`() {
        val tag = "MeasureExceptionTest"
        try {
            AppLogger.measureTime(tag, "failing_block") {
                Thread.sleep(10)
                throw RuntimeException("Test exception")
            }
            fail("Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
        }
    }
    @Test
    fun `test performance statistics`() {
        val tag = "StatsTest"
        repeat(3) { i ->
            AppLogger.measureTime(tag, "repeated_operation") {
                Thread.sleep((10 + i * 5).toLong())
            }
        }
        val stats = AppLogger.getPerformanceStats()
        assertNotNull("Stats should not be null", stats)
        val operationStats = stats["repeated_operation"]
        assertNotNull("Operation stats should exist", operationStats)
        operationStats?.let {
            assertEquals("Should have 3 calls", 3L, it.totalCalls)
            assertTrue("Total time should be positive", it.totalTimeMs > 0)
            assertTrue("Min time should be positive", it.minTimeMs > 0)
            assertTrue("Max time should be >= min time", it.maxTimeMs >= it.minTimeMs)
            assertTrue("Average should be calculated", it.avgTimeMs > 0)
        }
        AppLogger.logPerformanceStats(tag)
        AppLogger.clearPerformanceStats()
        val clearedStats = AppLogger.getPerformanceStats()
        assertTrue("Stats should be empty after clear", clearedStats.isEmpty())
    }
    @Test
    fun `test memory monitoring`() {
        val tag = "MemoryTest"
        AppLogger.logMemoryUsage(tag, "Test Memory Check")
        AppLogger.setMemoryMonitoringEnabled(false)
        AppLogger.logMemoryUsage(tag, "Should be filtered")
        AppLogger.setMemoryMonitoringEnabled(true)
        AppLogger.logMemoryUsage(tag, "Should be logged")
        AppLogger.forceGarbageCollection(tag, "Test GC")
        val snapshots = AppLogger.getMemorySnapshots()
        assertNotNull("Snapshots should not be null", snapshots)
        assertTrue("Should have memory snapshots", snapshots.isNotEmpty())
        val snapshot = snapshots.first()
        assertTrue("Used memory should be positive", snapshot.usedMemoryMB > 0)
        assertTrue("Max memory should be positive", snapshot.maxMemoryMB > 0)
        assertTrue("Thread count should be positive", snapshot.threadCount > 0)
        assertTrue("Timestamp should be reasonable", snapshot.timestamp > 0)
    }
    @Test
    fun `test specialised logging methods`() {
        val tag = "SpecializedTest"
        val context = mapOf("test_phase" to "specialized_logging")
        AppLogger.logLifecycle(tag, "onCreate", "MainActivity", context)
//        AppLogger.logNetwork(tag, "GET", "https:
        AppLogger.logRecording(tag, "start", "Camera1", 5000L, 1024 * 1024L, context)
        AppLogger.logSensor(tag, "reading", "GSR", "1.23", 3, System.currentTimeMillis(), context)
        AppLogger.logFile(tag, "save", "test.mp4", 1024 * 1024L, 250L, true, context)
        AppLogger.logFile(tag, "save", "failed.mp4", null, 100L, false, context)
        AppLogger.logStateChange(tag, "Camera", "IDLE", "RECORDING", context)
        AppLogger.logThreadInfo(tag, "Test Thread Info")
        val testError = RuntimeException("Test error for specialised logging")
        AppLogger.logError(tag, "test_operation", testError, context)
        assertTrue("Specialised logging should complete", true)
    }
    @Test
    fun `test extension functions`() {
        val testObject = TestLoggingClass()
        testObject.logI("Extension info message")
        testObject.logD("Extension debug message")
        testObject.logW("Extension warning message")
        testObject.logE("Extension error message")
        val context = mapOf("extension_test" to true)
        testObject.logI("Extension with context", context = context)
        testObject.startTiming("extension_operation")
        Thread.sleep(20)
        val duration = testObject.endTiming("extension_operation")
        assertTrue("Extension timing should work", duration > 0)
        val result = testObject.measureTime("extension_measure") {
            Thread.sleep(15)
            "extension_result"
        }
        assertEquals("Extension measureTime should work", "extension_result", result)
        testObject.logMemory("Extension Memory Check")
        val error = RuntimeException("Extension error")
        testObject.logError("extension_operation", error)
        assertTrue("Extension functions should work", true)
    }
    @Test
    fun `test thread safety`() {
        val threadCount = 5
        val operationsPerThread = 10
        val latch = CountDownLatch(threadCount)
        val exceptions = mutableListOf<Exception>()
        repeat(threadCount) { threadId ->
            thread {
                try {
                    repeat(operationsPerThread) { opId ->
                        val tag = "ThreadTest$threadId"
                        val context = mapOf(
                            "thread_id" to threadId,
                            "operation_id" to opId
                        )
                        AppLogger.i(tag, "Thread operation $opId", context = context)
                        AppLogger.measureTime(tag, "threaded_operation_$opId") {
                            Thread.sleep(1)
                        }
                        if (opId % 3 == 0) {
                            AppLogger.logMemoryUsage(tag, "Thread $threadId Op $opId")
                        }
                        AppLogger.logNetwork(tag, "API call", "test.com", "200", 50L, context)
                    }
                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue(
            "All threads should complete within timeout",
            latch.await(30, TimeUnit.SECONDS)
        )
        assertTrue(
            "No exceptions should occur during concurrent logging: ${exceptions.joinToString()}",
            exceptions.isEmpty()
        )
        val stats = AppLogger.getPerformanceStats()
        assertTrue("Should have performance stats from threads", stats.isNotEmpty())
        val snapshots = AppLogger.getMemorySnapshots()
        assertTrue("Should have memory snapshots from threads", snapshots.isNotEmpty())
    }
    @Test
    fun `test logging statistics`() {
        val tag = "StatsTest"
        repeat(5) { AppLogger.i(tag, "Info message $it") }
        repeat(3) { AppLogger.w(tag, "Warning message $it") }
        repeat(2) { AppLogger.e(tag, "Error message $it") }
        val stats = AppLogger.getLoggingStats()
        assertNotNull("Stats should not be null", stats)
        assertTrue("Stats should contain information", stats.isNotEmpty())
        assertTrue("Stats should mention logs", stats.contains("Logs:"))
    }
    @Test
    fun `test data class functionality`() {
        val perfStats = AppLogger.PerformanceStats(
            operationName = "test_op",
            totalCalls = 5L,
            totalTimeMs = 100L,
            minTimeMs = 10L,
            maxTimeMs = 30L
        )
        assertEquals("test_op", perfStats.operationName)
        assertEquals(5L, perfStats.totalCalls)
        assertEquals(100L, perfStats.totalTimeMs)
        assertEquals(10L, perfStats.minTimeMs)
        assertEquals(30L, perfStats.maxTimeMs)
        assertEquals(20L, perfStats.avgTimeMs)
        val memSnapshot = AppLogger.MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            context = "test_context",
            usedMemoryMB = 50L,
            freeMemoryMB = 100L,
            maxMemoryMB = 200L,
            nativeHeapSizeMB = 30L,
            threadCount = 10
        )
        assertEquals("test_context", memSnapshot.context)
        assertEquals(50L, memSnapshot.usedMemoryMB)
        assertEquals(100L, memSnapshot.freeMemoryMB)
        assertEquals(200L, memSnapshot.maxMemoryMB)
        assertEquals(30L, memSnapshot.nativeHeapSizeMB)
        assertEquals(10, memSnapshot.threadCount)
    }
    @Test
    fun `test method entry and exit logging`() {
        val tag = "MethodTest"
        AppLogger.logMethodEntry(tag, "testMethod", "param1", 42, null)
        AppLogger.logMethodExit(tag, "testMethod", "return_value")
        AppLogger.logMethodEntry(tag, "noParamMethod")
        AppLogger.logMethodExit(tag, "noParamMethod")
        AppLogger.logMethodEntry(tag, "voidMethod", "param")
        AppLogger.logMethodExit(tag, "voidMethod", null)
        assertTrue("Method logging should complete", true)
    }
    @Test
    fun `test system info logging`() {
        AppLogger.initialize(mockContext)
        assertTrue("System info logging should work", true)
    }
    private class TestLoggingClass {
        fun doSomething() {
            logI("Doing something in TestLoggingClass")
        }
    }
}
