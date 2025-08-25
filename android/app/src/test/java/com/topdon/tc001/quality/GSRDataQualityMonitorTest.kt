package com.topdon.tc001.quality

import com.topdon.tc001.gsr.quality.*
import com.shimmerresearch.driver.ProcessedGSRData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Comprehensive test suite for GSR data quality monitoring system
 * 
 * Tests the quality monitor's ability to:
 * - Detect various types of data artifacts
 * - Provide accurate quality assessments
 * - Generate appropriate recommendations
 * - Handle edge cases and boundary conditions
 */
class GSRDataQualityMonitorTest {
    
    private lateinit var qualityMonitor: GSRDataQualityMonitor
    
    @Before
    fun setup() {
        qualityMonitor = GSRDataQualityMonitor()
    }
    
    @Test
    fun `should detect high quality data correctly`() = runTest {
        // Generate high-quality synthetic GSR data
        val highQualityData = generateCleanGSRData(samples = 100)
        
        var totalQuality = 0.0
        highQualityData.forEach { sample ->
            val result = qualityMonitor.processDataSample(sample)
            totalQuality += result.qualityScore
            assertTrue("High quality sample should be valid", result.isValid)
            assertTrue("Quality score should be high", result.qualityScore > 85.0)
        }
        
        val avgQuality = totalQuality / highQualityData.size
        assertTrue("Average quality should be excellent", avgQuality > 90.0)
        
        val assessment = qualityMonitor.getCurrentQualityAssessment()
        assertEquals("Should have excellent grade", QualityGrade.EXCELLENT, assessment.grade)
    }
    
    @Test
    fun `should detect GSR spikes accurately`() = runTest {
        val normalData = generateCleanGSRData(samples = 50)
        
        // Process normal data first
        normalData.forEach { qualityMonitor.processDataSample(it) }
        
        // Inject a spike
        val spikeData = normalData.last().copy(
            filteredGSR = normalData.last().filteredGSR + 10.0, // Large spike
            sampleIndex = normalData.last().sampleIndex + 1
        )
        
        val result = qualityMonitor.processDataSample(spikeData)
        
        assertTrue("Spike should be detected", 
            result.issues.contains(QualityIssue.GSR_SPIKE_DETECTED))
        assertTrue("Quality should be reduced", result.qualityScore < 85.0)
    }
    
    @Test
    fun `should detect missing samples`() = runTest {
        val baseTime = System.currentTimeMillis()
        
        // Create samples with gap in sequence numbers
        val sample1 = createGSRSample(timestamp = baseTime, sampleIndex = 100)
        val sample2 = createGSRSample(timestamp = baseTime + 100, sampleIndex = 105) // Missing 4 samples
        
        qualityMonitor.processDataSample(sample1)
        val result = qualityMonitor.processDataSample(sample2)
        
        assertTrue("Missing samples should be detected",
            result.issues.contains(QualityIssue.MISSING_SAMPLES))
    }
    
    @Test
    fun `should detect out of range values`() = runTest {
        // Test extremely high GSR value
        val highGSRSample = createGSRSample(gsr = 100.0) // Way above normal range
        val result1 = qualityMonitor.processDataSample(highGSRSample)
        
        assertTrue("Out of range GSR should be detected",
            result1.issues.contains(QualityIssue.OUT_OF_RANGE_GSR))
        
        // Test extremely low temperature
        val lowTempSample = createGSRSample(temperature = 10.0) // Below normal body temp
        val result2 = qualityMonitor.processDataSample(lowTempSample)
        
        assertTrue("Out of range temperature should be detected",
            result2.issues.contains(QualityIssue.OUT_OF_RANGE_TEMPERATURE))
    }
    
    @Test
    fun `should detect signal saturation`() = runTest {
        val saturatedSample = createGSRSample(gsr = 49.0) // Near maximum sensor range
        val result = qualityMonitor.processDataSample(saturatedSample)
        
        assertTrue("Signal saturation should be detected",
            result.issues.contains(QualityIssue.SIGNAL_SATURATION))
    }
    
    @Test
    fun `should detect flatline signals`() = runTest {
        // Generate 1 second of identical values (flatline)
        val flatlineValue = 2.5
        val flatlineData = (1..128).map { i ->
            createGSRSample(
                timestamp = System.currentTimeMillis() + i * 8,
                sampleIndex = i.toLong(),
                gsr = flatlineValue
            )
        }
        
        flatlineData.forEach { qualityMonitor.processDataSample(it) }
        
        val lastResult = qualityMonitor.processDataSample(flatlineData.last())
        assertTrue("Flatline should be detected",
            lastResult.issues.contains(QualityIssue.SIGNAL_FLATLINE))
    }
    
    @Test
    fun `should detect excessive rate of change`() = runTest {
        val sample1 = createGSRSample(gsr = 2.0)
        val sample2 = createGSRSample(
            timestamp = sample1.timestamp + 8,
            sampleIndex = sample1.sampleIndex + 1,
            gsr = 10.0 // Extremely rapid change
        )
        
        qualityMonitor.processDataSample(sample1)
        val result = qualityMonitor.processDataSample(sample2)
        
        assertTrue("Excessive GSR change should be detected",
            result.issues.contains(QualityIssue.EXCESSIVE_GSR_CHANGE))
    }
    
    @Test
    fun `should detect timing inconsistencies`() = runTest {
        val baseTime = System.currentTimeMillis()
        val sample1 = createGSRSample(timestamp = baseTime, sampleIndex = 1)
        val sample2 = createGSRSample(
            timestamp = baseTime + 100, // Way too long for 128Hz
            sampleIndex = 2
        )
        
        qualityMonitor.processDataSample(sample1)
        val result = qualityMonitor.processDataSample(sample2)
        
        assertTrue("Timing inconsistency should be detected",
            result.issues.contains(QualityIssue.TIMING_INCONSISTENCY))
    }
    
    @Test
    fun `should provide accurate quality metrics`() = runTest {
        // Generate mixed quality data
        val goodSamples = generateCleanGSRData(samples = 80)
        val poorSamples = generateNoisyGSRData(samples = 20)
        
        val allSamples = goodSamples + poorSamples
        allSamples.forEach { qualityMonitor.processDataSample(it) }
        
        val metrics = qualityMonitor.qualityMetrics.first()
        
        assertEquals("Should have processed all samples", 100L, metrics.totalSamples)
        assertTrue("Valid data ratio should reflect quality mix", 
            metrics.validDataRatio in 0.7..0.9)
        assertTrue("Should have some artifacts", metrics.artifactCount > 0)
    }
    
    @Test
    fun `should generate appropriate recommendations`() = runTest {
        // Generate poor quality data
        val poorData = generateNoisyGSRData(samples = 100)
        poorData.forEach { qualityMonitor.processDataSample(it) }
        
        val assessment = qualityMonitor.getCurrentQualityAssessment()
        
        assertTrue("Should have recommendations for poor quality", 
            assessment.recommendations.isNotEmpty())
        
        val recommendationsText = assessment.recommendations.joinToString()
        assertTrue("Should recommend checking connections", 
            recommendationsText.contains("connection", ignoreCase = true))
    }
    
    @Test
    fun `should handle reset correctly`() = runTest {
        // Process some data
        generateCleanGSRData(samples = 50).forEach { 
            qualityMonitor.processDataSample(it) 
        }
        
        val metricsBefore = qualityMonitor.qualityMetrics.first()
        assertTrue("Should have processed samples", metricsBefore.totalSamples > 0)
        
        // Reset monitor
        qualityMonitor.reset()
        
        val metricsAfter = qualityMonitor.qualityMetrics.first()
        assertEquals("Should have zero samples after reset", 0L, metricsAfter.totalSamples)
        assertEquals("Should have zero valid samples", 0L, metricsAfter.validSamples)
        assertEquals("Should have zero artifacts", 0L, metricsAfter.artifactCount)
    }
    
    @Test
    fun `should handle edge cases gracefully`() = runTest {
        // Test with single sample
        val singleSample = createGSRSample()
        val result = qualityMonitor.processDataSample(singleSample)
        
        assertNotNull("Should handle single sample", result)
        assertTrue("Single clean sample should be valid", result.isValid)
        
        // Test with identical timestamps (should be handled gracefully)
        val duplicate = singleSample.copy(sampleIndex = singleSample.sampleIndex + 1)
        val result2 = qualityMonitor.processDataSample(duplicate)
        
        assertNotNull("Should handle duplicate timestamp", result2)
    }
    
    // Helper methods for generating test data
    
    private fun generateCleanGSRData(samples: Int, baseTime: Long = System.currentTimeMillis()): List<ProcessedGSRData> {
        return (1..samples).map { i ->
            createGSRSample(
                timestamp = baseTime + i * 8, // 128Hz = ~8ms intervals
                sampleIndex = i.toLong(),
                gsr = 2.5 + Random.nextDouble() * 0.2, // Stable GSR around 2.5µS
                temperature = 32.0 + Random.nextDouble() * 0.5 // Stable temperature
            )
        }
    }
    
    private fun generateNoisyGSRData(samples: Int, baseTime: Long = System.currentTimeMillis()): List<ProcessedGSRData> {
        return (1..samples).map { i ->
            createGSRSample(
                timestamp = baseTime + i * 8 + Random.nextLong(-3, 3), // Timing jitter
                sampleIndex = i.toLong(),
                gsr = 2.5 + Random.nextDouble() * 2.0 - 1.0, // Noisy GSR
                temperature = 32.0 + Random.nextDouble() * 2.0 - 1.0 // Noisy temperature
            )
        }
    }
    
    private fun createGSRSample(
        timestamp: Long = System.currentTimeMillis(),
        sampleIndex: Long = 1,
        gsr: Double = 2.5,
        temperature: Double = 32.0
    ): ProcessedGSRData {
        return ProcessedGSRData(
            timestamp = timestamp,
            rawGSR = gsr + Random.nextDouble() * 0.1 - 0.05,
            filteredGSR = gsr,
            rawTemperature = temperature + Random.nextDouble() * 0.2 - 0.1,
            filteredTemperature = temperature,
            gsrDerivative = 0.0,
            gsrVariability = 0.1,
            temperatureVariability = 0.05,
            signalQuality = 90.0,
            hasArtifact = false,
            isValid = true,
            sampleIndex = sampleIndex
        )
    }
}