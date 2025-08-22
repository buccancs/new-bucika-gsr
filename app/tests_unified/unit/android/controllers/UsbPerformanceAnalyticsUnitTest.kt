package com.multisensor.recording.controllers

import android.content.Context
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UsbPerformanceAnalyticsUnitTest {

    private lateinit var performanceAnalytics: UsbPerformanceAnalytics
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        performanceAnalytics = UsbPerformanceAnalytics()
        mockContext = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should record performance events correctly`() {
        val eventType = UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT
        val duration = 50L
        val deviceKey = "test_device_001"

        performanceAnalytics.recordEvent(eventType, duration, deviceKey)

        val report = performanceAnalytics.generatePerformanceReport(mockContext)
        assertEquals(1L, report.totalEvents)
        assertEquals(50.0, report.averageResponseTime, 0.1)
    }

    @Test
    fun `should calculate connection quality metrics accurately`() {
        val deviceKey = "test_device_quality"

        repeat(10) {
            performanceAnalytics.recordEvent(
                UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
                5L,
                deviceKey
            )
        }

        performanceAnalytics.recordEvent(
            UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
            25L,
            deviceKey
        )

        val qualityMetrics = performanceAnalytics.calculateConnectionQuality(deviceKey)

        assertTrue("Stability score should be high", qualityMetrics.stabilityScore > 0.8)
        assertTrue("Overall quality should be good", qualityMetrics.overallQuality > 0.7)
        assertEquals(UsbPerformanceAnalytics.QualityAction.OPTIMAL, qualityMetrics.recommendedAction)
    }

    @Test
    fun `should detect poor connection quality and recommend actions`() {
        val deviceKey = "poor_quality_device"

        repeat(20) {
            performanceAnalytics.recordEvent(
                UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
                100L,
                deviceKey
            )
        }

        val qualityMetrics = performanceAnalytics.calculateConnectionQuality(deviceKey)

        assertTrue("Quality score should be low", qualityMetrics.overallQuality < 0.5)
        assertTrue(
            "Should recommend action",
            qualityMetrics.recommendedAction != UsbPerformanceAnalytics.QualityAction.OPTIMAL
        )
        assertTrue("Average response time should be high", qualityMetrics.averageResponseTime > 50.0)
    }

    @Test
    fun `should generate complete performance report`() {
        val eventTypes = UsbPerformanceAnalytics.PerformanceEventType.values()
        val deviceKeys = listOf("device_1", "device_2", "device_3")

        eventTypes.forEach { eventType ->
            deviceKeys.forEach { deviceKey ->
                repeat(5) {
                    performanceAnalytics.recordEvent(eventType, (10..50).random().toLong(), deviceKey)
                }
            }
        }

        val report = performanceAnalytics.generatePerformanceReport(mockContext)

        assertTrue("Should have recorded events", report.totalEvents > 0)
        assertTrue("Average response time should be reasonable", report.averageResponseTime > 0)
        assertTrue(
            "95th percentile should be >= average",
            report.percentile95ResponseTime >= report.averageResponseTime
        )
        assertTrue(
            "99th percentile should be >= 95th",
            report.percentile99ResponseTime >= report.percentile95ResponseTime
        )
        assertTrue("Should have quality metrics for devices", report.qualityMetrics.isNotEmpty())
        assertTrue("Should have recommendations", report.systemRecommendations.isNotEmpty())
    }

    @Test
    fun `should calculate statistical percentiles correctly`() {
        val deviceKey = "statistical_test_device"
        val testValues = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L)

        testValues.forEach { value ->
            performanceAnalytics.recordEvent(
                UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
                value,
                deviceKey
            )
        }

        val report = performanceAnalytics.generatePerformanceReport(mockContext)

        assertEquals(55.0, report.averageResponseTime, 0.1)
        assertTrue("95th percentile should be around 95", report.percentile95ResponseTime >= 90L)
        assertTrue("99th percentile should be around 99", report.percentile99ResponseTime >= 95L)
    }

    @Test
    fun `should handle empty data gracefully`() {
        val qualityMetrics = performanceAnalytics.calculateConnectionQuality("nonexistent_device")
        val report = performanceAnalytics.generatePerformanceReport(mockContext)

        assertEquals(0.0, qualityMetrics.overallQuality, 0.001)
        assertEquals(0L, report.totalEvents)
        assertEquals(0.0, report.averageResponseTime, 0.001)
        assertTrue("Should have default recommendations", report.systemRecommendations.isNotEmpty())
    }

    @Test
    fun `should monitor connection quality with detailed analysis`() {
        val deviceKey = "monitored_device"

        repeat(15) {
            performanceAnalytics.recordEvent(
                UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
                (1..8).random().toLong(),
                deviceKey
            )
        }

        repeat(5) {
            performanceAnalytics.recordEvent(
                UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
                (20..30).random().toLong(),
                deviceKey
            )
        }

        val qualityReport = performanceAnalytics.monitorConnectionQuality(deviceKey)

        assertTrue("Report should contain device key", qualityReport.contains(deviceKey))
        assertTrue("Report should contain quality score", qualityReport.contains("Overall Quality Score"))
        assertTrue("Report should contain stability info", qualityReport.contains("Stability Score"))
        assertTrue("Report should contain response time info", qualityReport.contains("Response Time"))
        assertTrue("Report should contain recommendations", qualityReport.contains("Recommended"))
    }

    @Test
    fun `should reset analytics data completely`() {
        val deviceKey = "test_device_reset"

        repeat(10) {
            performanceAnalytics.recordEvent(
                UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
                25L,
                deviceKey
            )
        }

        val reportBefore = performanceAnalytics.generatePerformanceReport(mockContext)
        assertTrue("Should have events before reset", reportBefore.totalEvents > 0)

        performanceAnalytics.resetAnalytics()

        val reportAfter = performanceAnalytics.generatePerformanceReport(mockContext)
        assertEquals(0L, reportAfter.totalEvents)
        assertEquals(0.0, reportAfter.averageResponseTime, 0.001)

        val qualityAfter = performanceAnalytics.calculateConnectionQuality(deviceKey)
        assertEquals(0.0, qualityAfter.overallQuality, 0.001)
    }

    @Test
    fun `should calculate resource utilisation metrics`() {
        val resourceMetrics = performanceAnalytics.getResourceUtilization()

        assertTrue("Should contain CPU usage", resourceMetrics.containsKey("cpu_usage"))
        assertTrue("Should contain memory usage", resourceMetrics.containsKey("memory_usage"))
        assertTrue("Should contain event rate", resourceMetrics.containsKey("event_rate"))
        assertTrue("Should contain efficiency score", resourceMetrics.containsKey("efficiency_score"))

        resourceMetrics.values.forEach { value ->
            assertTrue("All metrics should be non-negative", value >= 0.0)
        }
    }

    @Test
    fun `should handle multiple event types correctly`() {
        val deviceKey = "multi_event_device"
        val eventTypes = UsbPerformanceAnalytics.PerformanceEventType.values()

        eventTypes.forEach { eventType ->
            repeat(3) {
                performanceAnalytics.recordEvent(eventType, 15L, deviceKey)
            }
        }

        val report = performanceAnalytics.generatePerformanceReport(mockContext)
        val expectedEvents = eventTypes.size * 3
        assertEquals(expectedEvents.toLong(), report.totalEvents)
        assertEquals(15.0, report.averageResponseTime, 0.1)
    }

    @Test
    fun `should maintain sliding window for memory efficiency`() {
        val deviceKey = "sliding_window_device"
        val windowSize = 1000

        repeat(windowSize + 100) {
            performanceAnalytics.recordEvent(
                UsbPerformanceAnalytics.PerformanceEventType.DEVICE_SCAN,
                10L,
                deviceKey
            )
        }

        val report = performanceAnalytics.generatePerformanceReport(mockContext)

        assertTrue(
            "Total events should be around window size",
            report.totalEvents <= windowSize + 50
        )
    }

    @Test
    fun `should calculate trimmed mean correctly for outlier handling`() {
        val deviceKey = "outlier_test_device"

        repeat(18) {
            performanceAnalytics.recordEvent(
                UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
                10L,
                deviceKey
            )
        }

        performanceAnalytics.recordEvent(
            UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
            1000L,
            deviceKey
        )
        performanceAnalytics.recordEvent(
            UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
            1L,
            deviceKey
        )

        val qualityMetrics = performanceAnalytics.calculateConnectionQuality(deviceKey)

        assertTrue(
            "Trimmed mean should handle outliers",
            qualityMetrics.averageResponseTime < 50.0
        )
    }
}
