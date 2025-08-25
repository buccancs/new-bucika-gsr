package com.topdon.thermal.capture.sync

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class GlobalClockManagerTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        GlobalClockManager.reset()
    }

    @After
    fun tearDown() {

        GlobalClockManager.shutdown()
    }

    @Test
    fun testSingletonInitialization() {

        val result = GlobalClockManager.initialize(mockContext)
        
        Assert.assertTrue("GlobalClockManager should initialize successfully", result)
        Assert.assertTrue("Should be initialized after initialization", GlobalClockManager.isInitialized())
    }

    @Test
    fun testDoubleInitializationPrevention() {

        GlobalClockManager.initialize(mockContext)
        val secondResult = GlobalClockManager.initialize(mockContext)
        
        Assert.assertTrue("Second initialization should succeed but not reinitialize", secondResult)
        Assert.assertTrue("Should remain initialized", GlobalClockManager.isInitialized())
    }

    @Test
    fun testNanosecondPrecisionTiming() {
        GlobalClockManager.initialize(mockContext)
        
        val timestamp1 = GlobalClockManager.getCurrentTimeNanos()
        Thread.sleep(1)
        val timestamp2 = GlobalClockManager.getCurrentTimeNanos()
        
        Assert.assertTrue("Nanosecond timestamps should be sequential", timestamp2 > timestamp1)
        
        val diff = timestamp2 - timestamp1
        Assert.assertTrue("Timestamp difference should be in nanosecond range", diff < 10_000_000L)
    }

    @Test
    fun testComponentRegistration() {
        GlobalClockManager.initialize(mockContext)
        
        val componentId = "thermal_camera"
        val result = GlobalClockManager.registerComponent(componentId, "Thermal Camera Module")
        
        Assert.assertTrue("Component should register successfully", result)
        Assert.assertTrue("Component should be tracked", GlobalClockManager.isComponentRegistered(componentId))
    }

    @Test
    fun testMultipleComponentRegistration() {
        GlobalClockManager.initialize(mockContext)
        
        val components = listOf(
            "thermal_camera" to "Thermal Camera",
            "gsr_sensor" to "GSR Sensor", 
            "video_capture" to "Video Capture",
            "dng_capture" to "DNG Capture"
        )
        
        components.forEach { (id, name) ->
            val result = GlobalClockManager.registerComponent(id, name)
            Assert.assertTrue("Component $id should register successfully", result)
        }
        
        val registeredCount = GlobalClockManager.getRegisteredComponentCount()
        Assert.assertEquals("Should have registered all components", components.size, registeredCount)
    }

    @Test
    fun testSynchronizationAccuracy() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        val component1 = "thermal_camera"
        val component2 = "gsr_sensor"
        
        GlobalClockManager.registerComponent(component1, "Thermal Camera")
        GlobalClockManager.registerComponent(component2, "GSR Sensor")
        
        val sync1 = GlobalClockManager.synchronizeComponent(component1)
        val sync2 = GlobalClockManager.synchronizeComponent(component2)
        
        val skew = kotlin.math.abs(sync1.syncTime - sync2.syncTime)
        Assert.assertTrue("Component synchronization skew should be minimal", skew < 5_000_000L)
    }

    @Test
    fun testClockDriftDetection() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        val componentId = "test_component"
        GlobalClockManager.registerComponent(componentId, "Test Component")
        
        Thread.sleep(10)
        
        val driftInfo = GlobalClockManager.getClockDriftInfo(componentId)
        
        Assert.assertNotNull("Clock drift info should be available", driftInfo)
        Assert.assertTrue("Drift should be within acceptable range", 
            kotlin.math.abs(driftInfo.driftNanos) < 50_000_000L)
    }

    @Test
    fun testConcurrentSynchronization() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        val latch = CountDownLatch(3)
        val results = mutableListOf<Long>()
        
        repeat(3) { index ->
            Thread {
                val componentId = "component_$index"
                GlobalClockManager.registerComponent(componentId, "Component $index")
                val syncResult = GlobalClockManager.synchronizeComponent(componentId)
                synchronized(results) {
                    results.add(syncResult.syncTime)
                }
                latch.countDown()
            }.start()
        }
        
        val completed = latch.await(5, TimeUnit.SECONDS)
        Assert.assertTrue("Concurrent synchronization should complete", completed)
        Assert.assertEquals("Should have 3 synchronization results", 3, results.size)
        
        val maxTime = results.maxOrNull()!!
        val minTime = results.minOrNull()!!
        val maxSkew = maxTime - minTime
        
        Assert.assertTrue("Maximum skew should be within tolerance", maxSkew < 10_000_000L)
    }

    @Test
    fun testPerformanceMetrics() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        val componentId = "perf_test_component"
        GlobalClockManager.registerComponent(componentId, "Performance Test")
        
        repeat(10) {
            GlobalClockManager.synchronizeComponent(componentId)
        }
        
        val metrics = GlobalClockManager.getPerformanceMetrics()
        
        Assert.assertNotNull("Performance metrics should be available", metrics)
        Assert.assertTrue("Should track sync operations", metrics.syncOperationCount >= 10)
        Assert.assertTrue("Average sync time should be reasonable", metrics.averageSyncTimeNanos < 1_000_000L)
    }

    @Test
    fun testMasterClockStability() {
        GlobalClockManager.initialize(mockContext)
        
        val initialTime = GlobalClockManager.getMasterClockTime()
        Thread.sleep(100)
        val laterTime = GlobalClockManager.getMasterClockTime()
        
        val elapsed = laterTime - initialTime
        Assert.assertTrue("Master clock should advance monotonically", elapsed > 0)
        Assert.assertTrue("Clock advancement should be reasonable", elapsed < 200_000_000L)
    }

    @Test
    fun testSynchronizationStartStop() {
        GlobalClockManager.initialize(mockContext)
        
        val startResult = GlobalClockManager.startSynchronization()
        Assert.assertTrue("Synchronization should start successfully", startResult)
        Assert.assertTrue("Should be actively synchronizing", GlobalClockManager.isSynchronizationActive())
        
        GlobalClockManager.stopSynchronization()
        Assert.assertFalse("Synchronization should stop", GlobalClockManager.isSynchronizationActive())
    }

    @Test
    fun testComponentUnregistration() {
        GlobalClockManager.initialize(mockContext)
        
        val componentId = "temp_component"
        GlobalClockManager.registerComponent(componentId, "Temporary Component")
        
        Assert.assertTrue("Component should be registered", GlobalClockManager.isComponentRegistered(componentId))
        
        val unregisterResult = GlobalClockManager.unregisterComponent(componentId)
        Assert.assertTrue("Component should unregister successfully", unregisterResult)
        Assert.assertFalse("Component should no longer be registered", GlobalClockManager.isComponentRegistered(componentId))
    }

    @Test
    fun testErrorHandling() {

        val result = GlobalClockManager.registerComponent("test", "Test")
        Assert.assertFalse("Should fail to register without initialization", result)
        
        val syncResult = GlobalClockManager.synchronizeComponent("test")
        Assert.assertNull("Should return null sync info without initialization", syncResult)
    }

    @Test
    fun testShutdownCleanup() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        val componentId = "shutdown_test"
        GlobalClockManager.registerComponent(componentId, "Shutdown Test")
        
        GlobalClockManager.shutdown()
        
        Assert.assertFalse("Should not be initialized after shutdown", GlobalClockManager.isInitialized())
        Assert.assertFalse("Synchronization should be stopped", GlobalClockManager.isSynchronizationActive())
    }

    @Test
    fun testTimestampConsistency() {
        GlobalClockManager.initialize(mockContext)
        
        val timestamps = mutableListOf<Long>()
        
        repeat(100) {
            timestamps.add(GlobalClockManager.getCurrentTimeNanos())
        }
        
        for (i in 1 until timestamps.size) {
            Assert.assertTrue("Timestamps should be monotonically increasing", 
                timestamps[i] >= timestamps[i-1])
        }
    }

    @Test
    fun testSynchronizationConfiguration() {
        GlobalClockManager.initialize(mockContext)
        
        val config = GlobalClockManager.SyncConfiguration(
            updateIntervalMs = 50L,
            driftThresholdNanos = 500_000L,
            maxSkewNanos = 2_000_000L
        )
        
        val result = GlobalClockManager.applySynchronizationConfiguration(config)
        Assert.assertTrue("Configuration should be applied successfully", result)
        
        val currentConfig = GlobalClockManager.getCurrentConfiguration()
        Assert.assertEquals("Update interval should be updated", 50L, currentConfig.updateIntervalMs)
    }

    @Test
    fun testHighFrequencyOperations() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        val componentId = "high_freq_test"
        GlobalClockManager.registerComponent(componentId, "High Frequency Test")
        
        val startTime = System.nanoTime()
        
        repeat(1000) {
            GlobalClockManager.synchronizeComponent(componentId)
        }
        
        val endTime = System.nanoTime()
        val totalTime = endTime - startTime
        val averageTime = totalTime / 1000
        
        Assert.assertTrue("High frequency operations should be efficient", 
            averageTime < 100_000L)
    }

    @Test
    fun testMemoryUsage() {
        GlobalClockManager.initialize(mockContext)
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        repeat(100) { index ->
            GlobalClockManager.registerComponent("component_$index", "Component $index")
        }
        
        val afterRegistration = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryGrowth = afterRegistration - initialMemory
        
        Assert.assertTrue("Memory growth should be reasonable", memoryGrowth < 10_000_000L)
    }
