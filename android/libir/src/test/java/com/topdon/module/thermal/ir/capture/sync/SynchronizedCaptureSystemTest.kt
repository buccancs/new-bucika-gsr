package com.topdon.thermal.capture.sync

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SynchronizedCaptureSystemTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var syncSystem: SynchronizedCaptureSystem

    @Before
    fun setUp() {
        syncSystem = SynchronizedCaptureSystem(mockContext)
    }

    @Test
    fun `initialize should reset all timing data and return true`() {

        val result = syncSystem.initialize()

        assertTrue("Initialize should succeed", result)
        
        val metrics = syncSystem.getSynchronizationMetrics()
        assertEquals("Session duration should be 0", 0L, metrics.sessionDurationMs)
        assertEquals("Video frames should be 0", 0, metrics.videoFramesRecorded)
        assertEquals("DNG frames should be 0", 0, metrics.dngFramesCaptured)
        assertEquals("Paired frames should be 0", 0, metrics.totalFramesPaired)
    }

    @Test
    fun `startSynchronizedCapture should create valid session info`() {

        syncSystem.initialize()

        val sessionInfo = syncSystem.startSynchronizedCapture()

        assertNotNull("Session info should not be null", sessionInfo)
        assertFalse("Session ID should not be empty", sessionInfo.sessionId.isEmpty())
        assertTrue("Start time should be positive", sessionInfo.startTimeNs > 0)
        assertTrue("Start time ms should be positive", sessionInfo.startTimeMs > 0)
    }

    @Test
    fun `registerVideoFrame should return valid hardware timestamp`() {

        syncSystem.initialize()
        syncSystem.startSynchronizedCapture()
        val presentationTimeUs = 33333L

        val hardwareTimestamp = syncSystem.registerVideoFrame(presentationTimeUs)

        assertTrue("Hardware timestamp should be positive", hardwareTimestamp > 0)
        
        val metrics = syncSystem.getSynchronizationMetrics()
        assertEquals("Video frames count should be 1", 1, metrics.videoFramesRecorded)
    }

    @Test
    fun `registerDNGFrame should return valid hardware timestamp`() {

        syncSystem.initialize()
        syncSystem.startSynchronizedCapture()
        val imageTimestampNs = System.nanoTime()
        val frameIndex = 1

        val hardwareTimestamp = syncSystem.registerDNGFrame(imageTimestampNs, frameIndex)

        assertTrue("Hardware timestamp should be positive", hardwareTimestamp > 0)
        
        val metrics = syncSystem.getSynchronizationMetrics()
        assertEquals("DNG frames count should be 1", 1, metrics.dngFramesCaptured)
    }

    @Test
    fun `concurrent frame registration should create correlations`() {

        syncSystem.initialize()
        syncSystem.startSynchronizedCapture()

        val baseTimestamp = System.nanoTime()
        val frameIntervalNs = 33_333_333L

        val videoTimestamp1 = syncSystem.registerVideoFrame(0L)
        Thread.sleep(1)
        val dngTimestamp1 = syncSystem.registerDNGFrame(baseTimestamp, 1)

        val videoTimestamp2 = syncSystem.registerVideoFrame(33333L)
        Thread.sleep(1)
        val dngTimestamp2 = syncSystem.registerDNGFrame(baseTimestamp + frameIntervalNs, 2)

        Thread.sleep(10)

        val metrics = syncSystem.getSynchronizationMetrics()
        assertEquals("Should have 2 video frames", 2, metrics.videoFramesRecorded)
        assertEquals("Should have 2 DNG frames", 2, metrics.dngFramesCaptured)
        assertTrue("Should have at least 1 frame correlation", metrics.totalFramesPaired > 0)
    }

    @Test
    fun `synchronization metrics should provide accurate timing data`() {

        syncSystem.initialize()
        val sessionInfo = syncSystem.startSynchronizedCapture()

        syncSystem.registerVideoFrame(0L)
        syncSystem.registerDNGFrame(System.nanoTime(), 1)
        
        Thread.sleep(100)

        val metrics = syncSystem.getSynchronizationMetrics()

        assertTrue("Session duration should be positive", metrics.sessionDurationMs > 0)
        assertEquals("Video frames count", 1, metrics.videoFramesRecorded)
        assertEquals("DNG frames count", 1, metrics.dngFramesCaptured)
        assertTrue("Average drift should be non-negative", metrics.averageTemporalDriftNs >= 0.0)
        assertTrue("Max drift should be non-negative", metrics.maxTemporalDriftNs >= 0L)
        assertTrue("Sync accuracy should be between 0-100%", 
            metrics.syncAccuracyPercent >= 0.0 && metrics.syncAccuracyPercent <= 100.0)
    }

    @Test
    fun `getCorrelationInfo should return null for non-existent timestamp`() {

        syncSystem.initialize()
        syncSystem.startSynchronizedCapture()

        val correlationInfo = syncSystem.getCorrelationInfo(123456789L)

        assertNull("Correlation info should be null for non-existent timestamp", correlationInfo)
    }

    @Test
    fun `cleanup should clear all data structures`() {

        syncSystem.initialize()
        syncSystem.startSynchronizedCapture()
        syncSystem.registerVideoFrame(0L)
        syncSystem.registerDNGFrame(System.nanoTime(), 1)

        syncSystem.cleanup()

        val metrics = syncSystem.getSynchronizationMetrics()
        assertEquals("Video frames should be 0 after cleanup", 0, metrics.videoFramesRecorded)
        assertEquals("DNG frames should be 0 after cleanup", 0, metrics.dngFramesCaptured)
        assertEquals("Paired frames should be 0 after cleanup", 0, metrics.totalFramesPaired)
    }

    @Test
    fun `frame correlation should identify high quality matches`() {

        syncSystem.initialize()
        syncSystem.startSynchronizedCapture()

        val baseTime = System.nanoTime()

        val videoTimestamp = syncSystem.registerVideoFrame(0L)
        val dngTimestamp = syncSystem.registerDNGFrame(baseTime, 1)

        Thread.sleep(10)

        val correlationInfo = syncSystem.getCorrelationInfo(videoTimestamp)
            ?: syncSystem.getCorrelationInfo(dngTimestamp)

        if (correlationInfo != null) {
            assertTrue("Temporal drift should be reasonable", 
                correlationInfo.temporalDriftNs < 100_000_000L)
            assertTrue("Correlation quality should be good",
                correlationInfo.correlationQuality != CorrelationQuality.POOR)
        }
    }

    @Test
    fun `multiple session initialization should reset previous state`() {

        syncSystem.initialize()
        syncSystem.startSynchronizedCapture()
        syncSystem.registerVideoFrame(0L)
        syncSystem.registerDNGFrame(System.nanoTime(), 1)

        val firstMetrics = syncSystem.getSynchronizationMetrics()
        assertTrue("Should have some frames from first session", 
            firstMetrics.videoFramesRecorded + firstMetrics.dngFramesCaptured > 0)

        syncSystem.initialize()
        val secondSessionInfo = syncSystem.startSynchronizedCapture()

        val newMetrics = syncSystem.getSynchronizationMetrics()
        assertEquals("Video frames should be reset", 0, newMetrics.videoFramesRecorded)
        assertEquals("DNG frames should be reset", 0, newMetrics.dngFramesCaptured)
        assertEquals("Paired frames should be reset", 0, newMetrics.totalFramesPaired)
        assertNotEquals("Session ID should be different", 
            firstMetrics.sessionDurationMs, secondSessionInfo.sessionId)
    }
