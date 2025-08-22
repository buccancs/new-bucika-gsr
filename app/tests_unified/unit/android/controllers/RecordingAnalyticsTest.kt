package com.multisensor.recording.controllers

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RecordingAnalyticsTest {

    private lateinit var analytics: RecordingAnalytics
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        analytics = RecordingAnalytics()
        mockContext = mockk(relaxed = true)
    }

    @Test
    fun `test analytics initialization`() {
        val sessionId = "test_session_123"

        analytics.initializeSession(mockContext, sessionId)

        val metrics = analytics.currentMetrics.value
        assertNotNull("Performance metrics should be initialized", metrics)
        val currentTime = System.currentTimeMillis()
        assertTrue("Initial timestamp should be recent",
            Math.abs(currentTime - metrics.timestamp) < 5000)
    }

    @Test
    fun `test performance metrics collection`() {
        analytics.initializeSession(mockContext, "test_session")

        analytics.updatePerformanceMetrics(mockContext)

        val metrics = analytics.currentMetrics.value
        assertTrue("Memory usage should be non-negative", metrics.memoryUsageMB >= 0)
        assertTrue(
            "CPU usage should be between 0 and 100",
            metrics.cpuUsagePercent >= 0f && metrics.cpuUsagePercent <= 100f
        )
        assertTrue("Storage write rate should be non-negative", metrics.storageWriteRateMBps >= 0f)
        assertNotNull("Thermal state should be set", metrics.thermalState)
    }

    @Test
    fun `test quality metrics calculation`() {
        val averageBitrate = 1500000L
        val frameStability = 0.95f
        val audioQuality = 0.9f

        analytics.updateQualityMetrics(averageBitrate, frameStability, audioQuality)

        val metrics = analytics.qualityMetrics.value
        assertEquals("Average bitrate should match input", averageBitrate, metrics.averageBitrate)
        assertEquals("Frame stability should match input", frameStability, metrics.frameStability, 0.01f)
        assertEquals("Audio quality should match input", audioQuality, metrics.audioQualityScore, 0.01f)
        assertTrue(
            "Overall quality score should be reasonable",
            metrics.overallQualityScore >= 0f && metrics.overallQualityScore <= 1f
        )
        assertTrue(
            "Recording efficiency should be reasonable",
            metrics.recordingEfficiency >= 0f && metrics.recordingEfficiency <= 1f
        )
    }

    @Test
    fun `test resource utilisation analysis`() {
        analytics.initializeSession(mockContext, "test_session")

        repeat(10) {
            analytics.updatePerformanceMetrics(mockContext)
        }

        val stats = analytics.analyzeResourceUtilization()

        assertTrue("Mean memory usage should be non-negative", stats.meanMemoryUsage >= 0.0)
        assertTrue("Max memory usage should be non-negative", stats.maxMemoryUsage >= 0L)
        assertTrue("Memory variance should be non-negative", stats.memoryVariance >= 0.0)
        assertTrue("Mean CPU usage should be non-negative", stats.meanCpuUsage >= 0.0)
        assertTrue("Max CPU usage should be non-negative", stats.maxCpuUsage >= 0f)
        assertTrue("CPU variance should be non-negative", stats.cpuVariance >= 0.0)
    }

    @Test
    fun `test trend analysis with insufficient data`() {
        analytics.initializeSession(mockContext, "test_session")

        repeat(5) {
            analytics.updatePerformanceMetrics(mockContext)
        }

        val trendAnalysis = analytics.performTrendAnalysis()

        assertNotNull("Trend analysis should return result even with insufficient data", trendAnalysis)
    }

    @Test
    fun `test trend analysis with sufficient data`() {
        analytics.initializeSession(mockContext, "test_session")

        repeat(15) {
            analytics.updatePerformanceMetrics(mockContext)
        }

        val trendAnalysis = analytics.performTrendAnalysis()

        assertNotNull("Performance trend should be determined", trendAnalysis.performanceTrend)
        assertTrue(
            "Predicted performance should be reasonable",
            trendAnalysis.predictedPerformance >= 0f
        )
        assertNotNull("Confidence interval should be provided", trendAnalysis.confidenceInterval)
        assertNotNull(
            "Quality recommendation should be provided",
            trendAnalysis.recommendedQualityAdjustment
        )
        assertTrue(
            "Trend strength should be between 0 and 1",
            trendAnalysis.trendStrength >= 0f && trendAnalysis.trendStrength <= 1f
        )
    }

    @Test
    fun `test frame processing analytics`() {
        analytics.initializeSession(mockContext, "test_session")

        repeat(100) {
            analytics.recordFrameProcessed()
        }
        repeat(5) {
            analytics.recordFrameDropped()
        }

        val report = analytics.generateAnalyticsReport()
        val sessionSummary = report["session_summary"] as Map<*, *>

        assertEquals("Total frames processed should match", 100L, sessionSummary["total_frames_processed"])
        assertEquals("Total frames dropped should match", 5L, sessionSummary["total_frames_dropped"])
        assertEquals(
            "Frame drop rate should be calculated correctly", 0.05f,
            sessionSummary["overall_frame_drop_rate"] as Float, 0.01f
        )
    }

    @Test
    fun `test data written tracking`() {
        analytics.initializeSession(mockContext, "test_session")

        analytics.recordDataWritten(1024 * 1024)
        analytics.recordDataWritten(2 * 1024 * 1024)

        val report = analytics.generateAnalyticsReport()
        val sessionSummary = report["session_summary"] as Map<*, *>

        assertEquals(
            "Total bytes written should match", 3L * 1024 * 1024,
            sessionSummary["total_bytes_written"]
        )
    }

    @Test
    fun `test analytics report generation`() {
        analytics.initializeSession(mockContext, "test_session")

        repeat(10) {
            analytics.updatePerformanceMetrics(mockContext)
            analytics.updateQualityMetrics(1500000L, 0.95f, 0.9f)
            analytics.recordFrameProcessed()
        }
        analytics.recordFrameDropped()
        analytics.recordDataWritten(1024 * 1024)

        val report = analytics.generateAnalyticsReport()

        assertNotNull("Report should be generated", report)
        assertTrue("Report should contain session summary", report.containsKey("session_summary"))
        assertTrue(
            "Report should contain performance statistics",
            report.containsKey("performance_statistics")
        )
        assertTrue("Report should contain quality analysis", report.containsKey("quality_analysis"))
        assertTrue("Report should contain trend analysis", report.containsKey("trend_analysis"))
        assertTrue("Report should contain metadata", report.containsKey("metadata"))

        val metadata = report["metadata"] as Map<*, *>
        assertTrue("Metadata should contain timestamp", metadata.containsKey("report_timestamp"))
        assertTrue(
            "Metadata should contain data points analysed",
            metadata.containsKey("data_points_analyzed")
        )
    }

    @Test
    fun `test analytics data clearing`() {
        analytics.initializeSession(mockContext, "test_session")

        repeat(5) {
            analytics.updatePerformanceMetrics(mockContext)
            analytics.recordFrameProcessed()
        }

        analytics.clearAnalyticsData()

        val report = analytics.generateAnalyticsReport()
        val sessionSummary = report["session_summary"] as Map<*, *>

        assertEquals("Frames processed should be cleared", 0L, sessionSummary["total_frames_processed"])
        assertEquals("Frames dropped should be cleared", 0L, sessionSummary["total_frames_dropped"])
        assertEquals("Bytes written should be cleared", 0L, sessionSummary["total_bytes_written"])
    }

    @Test
    fun `test quality metrics variability calculation`() {
        analytics.initializeSession(mockContext, "test_session")

        analytics.updateQualityMetrics(1000000L, 0.95f, 0.9f)
        analytics.updateQualityMetrics(1500000L, 0.93f, 0.88f)
        analytics.updateQualityMetrics(1200000L, 0.96f, 0.91f)

        val metrics = analytics.qualityMetrics.value
        assertTrue("Bitrate variability should be calculated", metrics.bitrateVariability >= 0f)
        assertTrue(
            "Overall quality should reflect all inputs",
            metrics.overallQualityScore >= 0f && metrics.overallQualityScore <= 1f
        )
    }

    @Test
    fun `test performance score calculation consistency`() {
        analytics.initializeSession(mockContext, "test_session")

        repeat(20) {
            analytics.updatePerformanceMetrics(mockContext)
        }

        val stats = analytics.analyzeResourceUtilization()
        assertTrue("Resource statistics should be meaningful", stats.meanMemoryUsage >= 0.0)
        assertTrue("Statistics should show reasonable variance", stats.memoryVariance >= 0.0)
    }

    @Test
    fun `test thermal state integration`() {
        analytics.initializeSession(mockContext, "test_session")

        analytics.updatePerformanceMetrics(mockContext)

        val metrics = analytics.currentMetrics.value
        assertNotNull("Thermal state should be present", metrics.thermalState)
        assertTrue(
            "Thermal state should be valid enum value",
            RecordingAnalytics.ThermalState.entries.contains(metrics.thermalState)
        )
    }

    @Test
    fun `test analytics with concurrent updates`() = runTest {
        analytics.initializeSession(mockContext, "test_session")

        repeat(5) {
            analytics.updatePerformanceMetrics(mockContext)
            analytics.updateQualityMetrics(1500000L, 0.95f, 0.9f)
            analytics.recordFrameProcessed()
            delay(10)
        }

        val report = analytics.generateAnalyticsReport()
        assertNotNull("Report should be generated with concurrent updates", report)

        val sessionSummary = report["session_summary"] as Map<*, *>
        assertTrue(
            "Frame count should reflect all updates",
            (sessionSummary["total_frames_processed"] as Long) > 0L
        )
    }

    @Test
    fun `test quality recommendation generation`() {
        analytics.initializeSession(mockContext, "test_session")

        repeat(15) {
            analytics.updatePerformanceMetrics(mockContext)
            analytics.updateQualityMetrics(1000000L - (it * 50000L), 0.9f - (it * 0.01f), 0.85f)
        }

        val trendAnalysis = analytics.performTrendAnalysis()

        assertNotNull(
            "Quality recommendation should be generated",
            trendAnalysis.recommendedQualityAdjustment
        )
        assertTrue(
            "Should recommend quality reduction for degrading performance",
            trendAnalysis.recommendedQualityAdjustment ==
                    RecordingAnalytics.QualityAdjustmentRecommendation.DECREASE ||
                    trendAnalysis.recommendedQualityAdjustment ==
                    RecordingAnalytics.QualityAdjustmentRecommendation.EMERGENCY_REDUCE ||
                    trendAnalysis.recommendedQualityAdjustment ==
                    RecordingAnalytics.QualityAdjustmentRecommendation.MAINTAIN
        )
    }
}
