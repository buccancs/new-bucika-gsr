package com.topdon.tc001.performance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.elvishew.xlog.XLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class PerformanceTestFramework {
    
    companion object {
        private const val TAG = "PerformanceTestFramework"
        
        private const val MEMORY_BASELINE_MB = 100
        private const val MEMORY_MAX_ALLOWED_MB = 400
        private const val CPU_MAX_ALLOWED_PERCENT = 75.0
        private const val BATTERY_DRAIN_MAX_PERCENT_PER_HOUR = 15.0
        
        private const val SHORT_TEST_DURATION_MS = 30_000L
        private const val MEDIUM_TEST_DURATION_MS = 300_000L
        private const val LONG_TEST_DURATION_MS = 1_800_000L
        
        private const val SAMPLE_INTERVAL_MS = 1000L
        private const val STRESS_TEST_ITERATIONS = 1000
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.ACCESS_NETWORK_STATE,
        android.Manifest.permission.BATTERY_STATS
    )

    private lateinit var context: Context
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        performanceMonitor = PerformanceMonitor(context)
        testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        
        XLog.d(TAG, "Performance test framework initialized")
        
        performanceMonitor.startMonitoring()
        
        Thread.sleep(2000)
    }

    @After
    fun tearDown() {
        performanceMonitor.stopMonitoring()
        testScope.cancel()
        XLog.d(TAG, "Performance test framework cleanup completed")
    }

    @Test
    fun testBaselineMemoryUsage() = runBlocking {
        XLog.i(TAG, "Starting baseline memory usage test")
        
        val memoryReadings = mutableListOf<Double>()
        
        performanceMonitor.memoryMetrics
            .take(30)
            .collect { metrics ->
                memoryReadings.add(metrics.totalMemoryMB)
                delay(SAMPLE_INTERVAL_MS)
            }
        
        val averageMemory = memoryReadings.average()
        val maxMemory = memoryReadings.maxOrNull() ?: 0.0
        
        XLog.i(TAG, "Baseline memory - Average: ${averageMemory}MB, Max: ${maxMemory}MB")
        
        assertTrue(
            averageMemory <= MEMORY_BASELINE_MB,
            "Baseline memory usage too high: ${averageMemory}MB (max allowed: ${MEMORY_BASELINE_MB}MB)"
        )
        
        assertTrue(
            maxMemory <= MEMORY_MAX_ALLOWED_MB,
            "Peak memory usage too high: ${maxMemory}MB (max allowed: ${MEMORY_MAX_ALLOWED_MB}MB)"
        )
    }

    @Test
    fun testMemoryLeakDetection() = runBlocking {
        XLog.i(TAG, "Starting memory leak detection test")
        
        val memoryReadings = mutableListOf<Double>()
        var testDuration = 0L
        
        performanceMonitor.memoryMetrics.collect { metrics ->
            memoryReadings.add(metrics.totalMemoryMB)
            testDuration += SAMPLE_INTERVAL_MS
            
            if (testDuration >= MEDIUM_TEST_DURATION_MS) {

                val memoryTrend = analyzeMemoryTrend(memoryReadings)
                
                XLog.i(TAG, "Memory leak test complete - Trend: ${memoryTrend}")
                
                assertTrue(
                    memoryTrend <= 50.0,
                    "Potential memory leak detected: ${memoryTrend}MB increase"
                )
                return@collect
            }
            
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    @Test
    fun testMemoryStressTest() = runBlocking {
        XLog.i(TAG, "Starting memory stress test")
        
        val initialMemory = performanceMonitor.memoryMetrics.take(1).first().totalMemoryMB
        
        val stressJob = testScope.launch {
            repeat(STRESS_TEST_ITERATIONS) { iteration ->

                val largeArray = ByteArray(1024 * 1024)
                largeArray.fill(iteration.toByte())
                
                if (iteration % 100 == 0) {
                    val currentMemory = performanceMonitor.memoryMetrics.take(1).first().totalMemoryMB
                    XLog.d(TAG, "Stress test iteration $iteration - Memory: ${currentMemory}MB")
                    
                    assertTrue(
                        currentMemory <= MEMORY_MAX_ALLOWED_MB * 1.2,
                        "Memory exceeded critical threshold during stress test: ${currentMemory}MB"
                    )
                }
                
                delay(10)
            }
        }
        
        stressJob.join()
        
        delay(5000)
        val finalMemory = performanceMonitor.memoryMetrics.take(1).first().totalMemoryMB
        val memoryIncrease = finalMemory - initialMemory
        
        XLog.i(TAG, "Memory stress test complete - Initial: ${initialMemory}MB, Final: ${finalMemory}MB, Increase: ${memoryIncrease}MB")
        
        assertTrue(
            memoryIncrease <= 100.0,
            "Memory increase after stress test too high: ${memoryIncrease}MB"
        )
    }

    @Test
    fun testCPUUsageUnderLoad() = runBlocking {
        XLog.i(TAG, "Starting CPU usage under load test")
        
        val cpuReadings = mutableListOf<Double>()
        
        val cpuIntensiveJob = testScope.launch {
            var counter = 0L
            while (isActive) {

                counter += Math.sqrt(counter.toDouble()).toLong()
                if (counter % 1000000 == 0L) {
                    yield()
                }
            }
        }
        
        performanceMonitor.systemMetrics
            .take(30)
            .collect { metrics ->
                cpuReadings.add(metrics.cpuUsagePercent)
                delay(SAMPLE_INTERVAL_MS)
            }
        
        cpuIntensiveJob.cancel()
        
        val averageCPU = cpuReadings.average()
        val maxCPU = cpuReadings.maxOrNull() ?: 0.0
        
        XLog.i(TAG, "CPU load test complete - Average: ${averageCPU}%, Max: ${maxCPU}%")
        
        assertTrue(
            averageCPU <= CPU_MAX_ALLOWED_PERCENT,
            "Average CPU usage too high: ${averageCPU}% (max allowed: ${CPU_MAX_ALLOWED_PERCENT}%)"
        )
    }

    @Test
    fun testBatteryConsumption() = runBlocking {
        XLog.i(TAG, "Starting battery consumption test")
        
        val batteryReadings = mutableListOf<Int>()
        
        performanceMonitor.batteryMetrics
            .take(30)
            .collect { metrics ->
                batteryReadings.add(metrics.batteryLevel)
                delay(SAMPLE_INTERVAL_MS)
            }
        
        if (batteryReadings.size >= 2) {
            val initialBattery = batteryReadings.first()
            val finalBattery = batteryReadings.last()
            val batteryDrop = initialBattery - finalBattery
            
            val hourlyConsumption = (batteryDrop * 3600.0) / (SHORT_TEST_DURATION_MS / 1000.0)
            
            XLog.i(TAG, "Battery consumption test - Drop: ${batteryDrop}%, Hourly estimate: ${hourlyConsumption}%")
            
            assertTrue(
                hourlyConsumption <= BATTERY_DRAIN_MAX_PERCENT_PER_HOUR,
                "Battery consumption too high: ${hourlyConsumption}% per hour (max allowed: ${BATTERY_DRAIN_MAX_PERCENT_PER_HOUR}%)"
            )
        }
    }

    @Test
    fun testGSRDataProcessingPerformance() = runBlocking {
        XLog.i(TAG, "Starting GSR data processing performance test")
        
        val startTime = System.nanoTime()
        var samplesProcessed = 0
        
        repeat(7680) {

            val gsrValue = Math.random() * 10.0 + 2.0
            val filteredValue = gsrValue * 0.9 + Math.random() * 0.2
            val temperature = 32.0 + Math.random() * 2.0
            
            val isSpike = Math.abs(filteredValue - gsrValue) > 0.5
            val isSaturated = gsrValue > 50.0 || gsrValue < 0.1
            
            samplesProcessed++
            
            if (samplesProcessed % 1280 == 0) {
                delay(10)
                
                val currentMemory = performanceMonitor.memoryMetrics.take(1).first()
                assertTrue(
                    currentMemory.totalMemoryMB <= MEMORY_MAX_ALLOWED_MB,
                    "Memory exceeded limit during GSR processing: ${currentMemory.totalMemoryMB}MB"
                )
            }
        }
        
        val endTime = System.nanoTime()
        val processingTimeMs = (endTime - startTime) / 1_000_000
        val samplesPerSecond = (samplesProcessed * 1000.0) / processingTimeMs
        
        XLog.i(TAG, "GSR processing test complete - ${samplesProcessed} samples in ${processingTimeMs}ms (${samplesPerSecond} samples/sec)")
        
        assertTrue(
            samplesPerSecond >= 128.0,
            "GSR processing too slow: ${samplesPerSecond} samples/sec (minimum required: 128)"
        )
    }

    @Test
    fun testEnduranceStability() = runBlocking {
        XLog.i(TAG, "Starting endurance stability test (30 minutes)")
        
        val startTime = System.currentTimeMillis()
        var testRunning = true
        
        val healthJob = testScope.launch {
            while (testRunning) {
                val memory = performanceMonitor.memoryMetrics.take(1).first()
                val system = performanceMonitor.systemMetrics.take(1).first()
                
                XLog.d(TAG, "Endurance test status - Memory: ${memory.totalMemoryMB}MB, CPU: ${system.cpuUsagePercent}%")
                
                assertTrue(
                    memory.totalMemoryMB <= MEMORY_MAX_ALLOWED_MB,
                    "Memory exceeded limit during endurance test: ${memory.totalMemoryMB}MB"
                )
                
                assertTrue(
                    system.cpuUsagePercent <= CPU_MAX_ALLOWED_PERCENT + 10,
                    "CPU usage too high during endurance test: ${system.cpuUsagePercent}%"
                )
                
                delay(60_000)
            }
        }
        
        delay(LONG_TEST_DURATION_MS)
        testRunning = false
        healthJob.cancel()
        
        val testDurationMinutes = (System.currentTimeMillis() - startTime) / 60_000
        XLog.i(TAG, "Endurance stability test completed after ${testDurationMinutes} minutes")
        
        val finalMemory = performanceMonitor.memoryMetrics.take(1).first()
        assertTrue(
            finalMemory.totalMemoryMB <= MEMORY_MAX_ALLOWED_MB,
            "System not stable after endurance test - Memory: ${finalMemory.totalMemoryMB}MB"
        )
    }

    private fun analyzeMemoryTrend(readings: List<Double>): Double {
        if (readings.size < 2) return 0.0
        
        val firstQuarter = readings.take(readings.size / 4).average()
        val lastQuarter = readings.takeLast(readings.size / 4).average()
        
        return lastQuarter - firstQuarter
    }

    private suspend fun CoroutineScope.awaitCondition(
        timeoutMs: Long,
        checkIntervalMs: Long = 100,
        condition: suspend () -> Boolean
    ): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition()) return true
            delay(checkIntervalMs)
        }
        
        return false
    }
