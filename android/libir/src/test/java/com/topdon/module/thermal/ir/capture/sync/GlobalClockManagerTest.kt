package com.topdon.module.thermal.ir.capture.sync

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

/**
 * Comprehensive test suite for GlobalClockManager
 * Tests nanosecond precision timing, synchronization accuracy, and multi-component coordination
 * Critical for quality improvement - this is one of the high complexity components (CC: 22+)
 */
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
        // Reset singleton state before each test
        GlobalClockManager.reset()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        GlobalClockManager.shutdown()
    }

    @Test
    fun testSingletonInitialization() {
        // Test singleton initialization
        val result = GlobalClockManager.initialize(mockContext)
        
        Assert.assertTrue("GlobalClockManager should initialize successfully", result)
        Assert.assertTrue("Should be initialized after initialization", GlobalClockManager.isInitialized())
    }

    @Test
    fun testDoubleInitializationPrevention() {
        // Test that double initialization is handled gracefully
        GlobalClockManager.initialize(mockContext)
        val secondResult = GlobalClockManager.initialize(mockContext)
        
        Assert.assertTrue("Second initialization should succeed but not reinitialize", secondResult)
        Assert.assertTrue("Should remain initialized", GlobalClockManager.isInitialized())
    }

    @Test
    fun testNanosecondPrecisionTiming() {
        GlobalClockManager.initialize(mockContext)
        
        // Test nanosecond precision timestamp generation
        val timestamp1 = GlobalClockManager.getCurrentTimeNanos()
        Thread.sleep(1) // Small delay
        val timestamp2 = GlobalClockManager.getCurrentTimeNanos()
        
        Assert.assertTrue("Nanosecond timestamps should be sequential", timestamp2 > timestamp1)
        
        val diff = timestamp2 - timestamp1
        Assert.assertTrue("Timestamp difference should be in nanosecond range", diff < 10_000_000L) // Less than 10ms
    }

    @Test
    fun testComponentRegistration() {
        GlobalClockManager.initialize(mockContext)
        
        // Test component registration for synchronization
        val componentId = "thermal_camera"
        val result = GlobalClockManager.registerComponent(componentId, "Thermal Camera Module")
        
        Assert.assertTrue("Component should register successfully", result)
        Assert.assertTrue("Component should be tracked", GlobalClockManager.isComponentRegistered(componentId))
    }

    @Test
    fun testMultipleComponentRegistration() {
        GlobalClockManager.initialize(mockContext)
        
        // Test registration of multiple components
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
        
        // Test synchronization accuracy across components
        val component1 = "thermal_camera"
        val component2 = "gsr_sensor"
        
        GlobalClockManager.registerComponent(component1, "Thermal Camera")
        GlobalClockManager.registerComponent(component2, "GSR Sensor")
        
        val sync1 = GlobalClockManager.synchronizeComponent(component1)
        val sync2 = GlobalClockManager.synchronizeComponent(component2)
        
        val skew = kotlin.math.abs(sync1.syncTime - sync2.syncTime)
        Assert.assertTrue("Component synchronization skew should be minimal", skew < 5_000_000L) // Less than 5ms
    }

    @Test
    fun testClockDriftDetection() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        // Test clock drift detection and correction
        val componentId = "test_component"
        GlobalClockManager.registerComponent(componentId, "Test Component")
        
        // Simulate some operations
        Thread.sleep(10)
        
        val driftInfo = GlobalClockManager.getClockDriftInfo(componentId)
        
        Assert.assertNotNull("Clock drift info should be available", driftInfo)
        Assert.assertTrue("Drift should be within acceptable range", 
            kotlin.math.abs(driftInfo.driftNanos) < 50_000_000L) // Less than 50ms
    }

    @Test
    fun testConcurrentSynchronization() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        val latch = CountDownLatch(3)
        val results = mutableListOf<Long>()
        
        // Test concurrent synchronization from multiple threads
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
        
        // Check that all results are reasonably close
        val maxTime = results.maxOrNull()!!
        val minTime = results.minOrNull()!!
        val maxSkew = maxTime - minTime
        
        Assert.assertTrue("Maximum skew should be within tolerance", maxSkew < 10_000_000L) // Less than 10ms
    }

    @Test
    fun testPerformanceMetrics() {
        GlobalClockManager.initialize(mockContext)
        GlobalClockManager.startSynchronization()
        
        val componentId = "perf_test_component"
        GlobalClockManager.registerComponent(componentId, "Performance Test")
        
        // Perform multiple synchronizations to test performance tracking
        repeat(10) {
            GlobalClockManager.synchronizeComponent(componentId)
        }
        
        val metrics = GlobalClockManager.getPerformanceMetrics()
        
        Assert.assertNotNull("Performance metrics should be available", metrics)
        Assert.assertTrue("Should track sync operations", metrics.syncOperationCount >= 10)
        Assert.assertTrue("Average sync time should be reasonable", metrics.averageSyncTimeNanos < 1_000_000L) // Less than 1ms
    }

    @Test
    fun testMasterClockStability() {
        GlobalClockManager.initialize(mockContext)
        
        // Test master clock stability over time
        val initialTime = GlobalClockManager.getMasterClockTime()
        Thread.sleep(100)
        val laterTime = GlobalClockManager.getMasterClockTime()
        
        val elapsed = laterTime - initialTime
        Assert.assertTrue("Master clock should advance monotonically", elapsed > 0)
        Assert.assertTrue("Clock advancement should be reasonable", elapsed < 200_000_000L) // Less than 200ms
    }

    @Test
    fun testSynchronizationStartStop() {
        GlobalClockManager.initialize(mockContext)
        
        // Test synchronization lifecycle
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
        // Test error handling without initialization
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
        
        // Test graceful shutdown
        GlobalClockManager.shutdown()
        
        Assert.assertFalse("Should not be initialized after shutdown", GlobalClockManager.isInitialized())
        Assert.assertFalse("Synchronization should be stopped", GlobalClockManager.isSynchronizationActive())
    }

    @Test
    fun testTimestampConsistency() {
        GlobalClockManager.initialize(mockContext)
        
        val timestamps = mutableListOf<Long>()
        
        // Collect multiple timestamps rapidly
        repeat(100) {
            timestamps.add(GlobalClockManager.getCurrentTimeNanos())
        }
        
        // Verify timestamps are monotonically increasing
        for (i in 1 until timestamps.size) {
            Assert.assertTrue("Timestamps should be monotonically increasing", 
                timestamps[i] >= timestamps[i-1])
        }
    }

    @Test
    fun testSynchronizationConfiguration() {
        GlobalClockManager.initialize(mockContext)
        
        // Test configuration options
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
        
        // Perform high frequency synchronization operations
        repeat(1000) {
            GlobalClockManager.synchronizeComponent(componentId)
        }
        
        val endTime = System.nanoTime()
        val totalTime = endTime - startTime
        val averageTime = totalTime / 1000
        
        // Should complete high frequency operations efficiently
        Assert.assertTrue("High frequency operations should be efficient", 
            averageTime < 100_000L) // Less than 100 microseconds per operation
    }

    @Test
    fun testMemoryUsage() {
        GlobalClockManager.initialize(mockContext)
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Register many components to test memory usage
        repeat(100) { index ->
            GlobalClockManager.registerComponent("component_$index", "Component $index")
        }
        
        val afterRegistration = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryGrowth = afterRegistration - initialMemory
        
        // Memory growth should be reasonable
        Assert.assertTrue("Memory growth should be reasonable", memoryGrowth < 10_000_000L) // Less than 10MB
    }
}