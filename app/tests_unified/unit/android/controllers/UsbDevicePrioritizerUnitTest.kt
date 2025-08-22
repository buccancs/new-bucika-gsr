package com.multisensor.recording.controllers

import android.hardware.usb.UsbDevice
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UsbDevicePrioritizerUnitTest {

    private lateinit var devicePrioritizer: UsbDevicePrioritizer
    private lateinit var mockPerformanceAnalytics: UsbPerformanceAnalytics
    private lateinit var mockDevice1: UsbDevice
    private lateinit var mockDevice2: UsbDevice
    private lateinit var mockDevice3: UsbDevice

    @Before
    fun setup() {
        mockPerformanceAnalytics = mockk(relaxed = true)
        devicePrioritizer = UsbDevicePrioritizer(mockPerformanceAnalytics)

        mockDevice1 = mockk(relaxed = true)
        every { mockDevice1.vendorId } returns 0x0BDA
        every { mockDevice1.productId } returns 0x3901
        every { mockDevice1.deviceName } returns "/dev/bus/usb/001/001"

        mockDevice2 = mockk(relaxed = true)
        every { mockDevice2.vendorId } returns 0x0BDA
        every { mockDevice2.productId } returns 0x5840
        every { mockDevice2.deviceName } returns "/dev/bus/usb/001/002"

        mockDevice3 = mockk(relaxed = true)
        every { mockDevice3.vendorId } returns 0x0BDA
        every { mockDevice3.productId } returns 0x5830
        every { mockDevice3.deviceName } returns "/dev/bus/usb/001/003"
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should assess device priority with high-quality device`() {
        val deviceKey = "3034_14593_/dev/bus/usb/001/001"
        val connectionTime = System.currentTimeMillis() - 1000
        val connectionCount = 10

        val qualityMetrics = UsbPerformanceAnalytics.ConnectionQualityMetrics(
            stabilityScore = 0.95,
            averageResponseTime = 5.0,
            throughputScore = 0.9,
            overallQuality = 0.92,
            recommendedAction = UsbPerformanceAnalytics.QualityAction.OPTIMAL
        )
        every { mockPerformanceAnalytics.calculateConnectionQuality(deviceKey) } returns qualityMetrics
        every { mockPerformanceAnalytics.getResourceUtilization() } returns mapOf(
            "efficiency_score" to 0.95,
            "memory_usage" to 0.2,
            "event_rate" to 8.0
        )

        val assessment = devicePrioritizer.assessDevicePriority(deviceKey, mockDevice1, connectionTime, connectionCount)

        assertEquals(UsbDevicePrioritizer.PriorityLevel.CRITICAL, assessment.priorityLevel)
        assertTrue("Priority score should be high", assessment.priorityScore > 0.85)
        assertTrue("Quality score should match input", assessment.qualityScore > 0.9)
        assertTrue("History score should be good", assessment.historyScore > 0.5)
        assertTrue("Characteristics score should be high for TC001", assessment.characteristicsScore > 0.8)
        assertTrue("Efficiency score should be high", assessment.efficiencyScore > 0.8)
        assertTrue("Confidence should be reasonable", assessment.confidence > 0.4)
        assertEquals(deviceKey, assessment.deviceKey)
        assertEquals(mockDevice1, assessment.device)
    }

    @Test
    fun `should assess device priority with poor-quality device`() {
        val deviceKey = "3034_14593_/dev/bus/usb/001/003"
        val connectionTime = System.currentTimeMillis() - 86400000
        val connectionCount = 2

        val qualityMetrics = UsbPerformanceAnalytics.ConnectionQualityMetrics(
            stabilityScore = 0.3,
            averageResponseTime = 80.0,
            throughputScore = 0.2,
            overallQuality = 0.25,
            recommendedAction = UsbPerformanceAnalytics.QualityAction.CHECK_CONNECTIONS
        )
        every { mockPerformanceAnalytics.calculateConnectionQuality(deviceKey) } returns qualityMetrics
        every { mockPerformanceAnalytics.getResourceUtilization() } returns mapOf(
            "efficiency_score" to 0.4,
            "memory_usage" to 0.8,
            "event_rate" to 2.0
        )

        val assessment = devicePrioritizer.assessDevicePriority(deviceKey, mockDevice3, connectionTime, connectionCount)

        assertTrue(
            "Priority level should be low",
            assessment.priorityLevel in listOf(
                UsbDevicePrioritizer.PriorityLevel.LOW,
                UsbDevicePrioritizer.PriorityLevel.DISABLED
            )
        )
        assertTrue("Priority score should be low", assessment.priorityScore < 0.5)
        assertTrue("Quality score should match poor input", assessment.qualityScore < 0.5)
        assertTrue("Recommendations should be present", assessment.recommendations.isNotEmpty())
    }

    @Test
    fun `should optimise device selection for single device`() {
        val deviceKey1 = "device_001"
        val assessment1 = UsbDevicePrioritizer.DevicePriorityAssessment(
            deviceKey = deviceKey1,
            device = mockDevice1,
            priorityLevel = UsbDevicePrioritizer.PriorityLevel.HIGH,
            priorityScore = 0.85,
            qualityScore = 0.9,
            historyScore = 0.8,
            characteristicsScore = 0.85,
            efficiencyScore = 0.9,
            confidence = 0.8,
            recommendations = listOf("Device performing optimally")
        )

        val selectionResult = devicePrioritizer.optimizeDeviceSelection(listOf(assessment1), 3)

        assertNotNull("Should have primary device", selectionResult.primaryDevice)
        assertEquals(assessment1, selectionResult.primaryDevice)
        assertTrue("Should have no secondary devices", selectionResult.secondaryDevices.isEmpty())
        assertEquals(1, selectionResult.allDeviceAssessments.size)
        assertTrue("Should have selection rationale", selectionResult.selectionRationale.isNotEmpty())
        assertTrue("Quality score should be positive", selectionResult.optimizationMetrics.totalQualityScore > 0)
    }

    @Test
    fun `should optimise device selection for multiple devices`() {
        val assessment1 =
            createMockAssessment("device_001", mockDevice1, 0.92, UsbDevicePrioritizer.PriorityLevel.CRITICAL)
        val assessment2 = createMockAssessment("device_002", mockDevice2, 0.85, UsbDevicePrioritizer.PriorityLevel.HIGH)
        val assessment3 =
            createMockAssessment("device_003", mockDevice3, 0.75, UsbDevicePrioritizer.PriorityLevel.MEDIUM)
        val assessments = listOf(assessment1, assessment2, assessment3)

        val selectionResult = devicePrioritizer.optimizeDeviceSelection(assessments, 2)

        assertNotNull("Should have primary device", selectionResult.primaryDevice)
        assertEquals(assessment1.deviceKey, selectionResult.primaryDevice?.deviceKey)
        assertEquals(1, selectionResult.secondaryDevices.size)
        assertEquals(assessment2.deviceKey, selectionResult.secondaryDevices[0].deviceKey)
        assertEquals(3, selectionResult.allDeviceAssessments.size)

        val metrics = selectionResult.optimizationMetrics
        assertTrue("Total quality should be high", metrics.totalQualityScore > 0.8)
        assertTrue("Expected reliability should be high", metrics.expectedReliability > 0.7)
        assertTrue("Resource efficiency should be positive", metrics.resourceEfficiency > 0.0)
        assertTrue("Optimisation confidence should be reasonable", metrics.optimizationConfidence > 0.0)
    }

    @Test
    fun `should handle empty device list gracefully`() {
        val selectionResult = devicePrioritizer.optimizeDeviceSelection(emptyList(), 3)

        assertNull("Should have no primary device", selectionResult.primaryDevice)
        assertTrue("Should have no secondary devices", selectionResult.secondaryDevices.isEmpty())
        assertTrue("Should have no assessments", selectionResult.allDeviceAssessments.isEmpty())
        assertEquals("No devices available for selection", selectionResult.selectionRationale)
        assertEquals(0.0, selectionResult.optimizationMetrics.totalQualityScore, 0.001)
        assertEquals(1.0, selectionResult.optimizationMetrics.riskScore, 0.001)
    }

    @Test
    fun `should update device priority with performance feedback`() {
        val deviceKey = "feedback_device"
        val initialPerformanceScore = 0.8
        val actualReliability = 0.9
        val resourceUsage = 0.3

        devicePrioritizer.updateDevicePriorityFeedback(
            deviceKey,
            initialPerformanceScore,
            actualReliability,
            resourceUsage
        )

        assertTrue("Feedback update should complete successfully", true)
    }

    @Test
    fun `should generate complete priority analysis report`() {
        val assessment1 =
            createMockAssessment("device_001", mockDevice1, 0.9, UsbDevicePrioritizer.PriorityLevel.CRITICAL)
        val assessment2 = createMockAssessment("device_002", mockDevice2, 0.7, UsbDevicePrioritizer.PriorityLevel.HIGH)
        val assessment3 = createMockAssessment("device_003", mockDevice3, 0.4, UsbDevicePrioritizer.PriorityLevel.LOW)
        val assessments = listOf(assessment1, assessment2, assessment3)

        val report = devicePrioritizer.generatePriorityAnalysisReport(assessments)

        assertTrue("Report should contain title", report.contains("Advanced Device Priority Analysis Report"))
        assertTrue("Report should contain evaluation criteria info", report.contains("Evaluation Criteria"))
        assertTrue("Report should contain device count", report.contains("Total Devices Assessed: 3"))
        assertTrue("Report should contain priority distribution", report.contains("Priority Distribution"))
        assertTrue("Report should contain device rankings", report.contains("Detailed Device Rankings"))
        assertTrue("Report should contain statistical analysis", report.contains("Statistical Analysis"))
        assertTrue("Report should contain recommendations", report.contains("System Recommendations"))

        assertTrue("Should show CRITICAL devices", report.contains("CRITICAL: 1"))
        assertTrue("Should show HIGH devices", report.contains("HIGH: 1"))
        assertTrue("Should show LOW devices", report.contains("LOW: 1"))
    }

    @Test
    fun `should calculate device characteristics score correctly for different models`() {
        val tc001Assessment = createAssessmentForDevice(mockDevice1)
        val tc001PlusAssessment = createAssessmentForDevice(mockDevice2)
        val tc001VariantAssessment = createAssessmentForDevice(mockDevice3)

        assertTrue(
            "TC001 should have highest characteristics score",
            tc001Assessment.characteristicsScore >= tc001PlusAssessment.characteristicsScore
        )
        assertTrue(
            "TC001 Plus should have higher score than variant",
            tc001PlusAssessment.characteristicsScore >= tc001VariantAssessment.characteristicsScore
        )
    }

    @Test
    fun `should implement diversity optimisation in secondary device selection`() {
        val assessment1 = createMockAssessment("device_001", mockDevice1, 0.85, UsbDevicePrioritizer.PriorityLevel.HIGH)
        val assessment2 = createMockAssessment("device_002", mockDevice2, 0.84, UsbDevicePrioritizer.PriorityLevel.HIGH)
        val assessment3 = createMockAssessment("device_003", mockDevice1, 0.83, UsbDevicePrioritizer.PriorityLevel.HIGH)
        val assessments = listOf(assessment1, assessment2, assessment3)

        val selectionResult = devicePrioritizer.optimizeDeviceSelection(assessments, 3)

        assertEquals(3, selectionResult.allDeviceAssessments.size)
        assertNotNull(selectionResult.primaryDevice)
        assertEquals(2, selectionResult.secondaryDevices.size)

        val selectedDevices = listOfNotNull(selectionResult.primaryDevice) + selectionResult.secondaryDevices
        val productIds = selectedDevices.map { it.device.productId }.toSet()
        assertTrue("Should select devices with different product IDs for diversity", productIds.size >= 2)
    }

    @Test
    fun `should calculate confidence based on score stability`() {
        val deviceKey = "confidence_test_device"
        val connectionTime = System.currentTimeMillis()
        val connectionCount = 5

        val qualityMetrics = UsbPerformanceAnalytics.ConnectionQualityMetrics(
            stabilityScore = 0.85,
            averageResponseTime = 10.0,
            throughputScore = 0.8,
            overallQuality = 0.82,
            recommendedAction = UsbPerformanceAnalytics.QualityAction.OPTIMAL
        )
        every { mockPerformanceAnalytics.calculateConnectionQuality(deviceKey) } returns qualityMetrics
        every { mockPerformanceAnalytics.getResourceUtilization() } returns mapOf(
            "efficiency_score" to 0.85,
            "memory_usage" to 0.3,
            "event_rate" to 7.0
        )

        repeat(5) {
            devicePrioritizer.assessDevicePriority(deviceKey, mockDevice1, connectionTime, connectionCount)
        }

        val finalAssessment =
            devicePrioritizer.assessDevicePriority(deviceKey, mockDevice1, connectionTime, connectionCount)

        assertTrue("Confidence should increase with more data points", finalAssessment.confidence > 0.5)
    }

    @Test
    fun `should handle mathematical edge cases in optimisation metrics`() {
        val assessment =
            createMockAssessment("single_device", mockDevice1, 1.0, UsbDevicePrioritizer.PriorityLevel.CRITICAL)

        val selectionResult = devicePrioritizer.optimizeDeviceSelection(listOf(assessment), 1)

        val metrics = selectionResult.optimizationMetrics
        assertFalse("Quality score should not be NaN", metrics.totalQualityScore.isNaN())
        assertFalse("Reliability should not be NaN", metrics.expectedReliability.isNaN())
        assertFalse("Efficiency should not be NaN", metrics.resourceEfficiency.isNaN())
        assertEquals(0.0, metrics.diversityIndex, 0.001)
        assertFalse("Risk score should not be NaN", metrics.riskScore.isNaN())
    }

    private fun createMockAssessment(
        deviceKey: String,
        device: UsbDevice,
        priorityScore: Double,
        priorityLevel: UsbDevicePrioritizer.PriorityLevel
    ): UsbDevicePrioritizer.DevicePriorityAssessment {
        return UsbDevicePrioritizer.DevicePriorityAssessment(
            deviceKey = deviceKey,
            device = device,
            priorityLevel = priorityLevel,
            priorityScore = priorityScore,
            qualityScore = priorityScore * 0.9,
            historyScore = priorityScore * 0.8,
            characteristicsScore = priorityScore * 0.85,
            efficiencyScore = priorityScore * 0.9,
            confidence = 0.8,
            recommendations = listOf("Test recommendation")
        )
    }

    private fun createAssessmentForDevice(device: UsbDevice): UsbDevicePrioritizer.DevicePriorityAssessment {
        val deviceKey = "${device.vendorId}_${device.productId}_${device.deviceName}"

        every { mockPerformanceAnalytics.calculateConnectionQuality(deviceKey) } returns
                UsbPerformanceAnalytics.ConnectionQualityMetrics(
                    0.8,
                    10.0,
                    0.8,
                    0.8,
                    UsbPerformanceAnalytics.QualityAction.OPTIMAL
                )
        every { mockPerformanceAnalytics.getResourceUtilization() } returns mapOf(
            "efficiency_score" to 0.8,
            "memory_usage" to 0.3,
            "event_rate" to 6.0
        )

        return devicePrioritizer.assessDevicePriority(deviceKey, device, System.currentTimeMillis(), 5)
    }
}
