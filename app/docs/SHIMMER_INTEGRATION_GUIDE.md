# Shimmer Device Integration Guide

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](../../AndroidApp/build.gradle.kts)
[![API Integration](https://img.shields.io/badge/ShimmerAndroidAPI-Complete-blue)](api/shimmer-api.md)
[![Sensors](https://img.shields.io/badge/sensors-7%20types-green)](#supported-sensors)

## Overview

This guide provides comprehensive documentation for integrating Shimmer3 GSR+ devices with the Multi-Sensor Recording System. The implementation follows official ShimmerAndroidAPI patterns while providing enhanced functionality and research-grade user interfaces.

## Quick Start

### 1. Device Setup
```kotlin
// Initialise Shimmer manager
val shimmerManager = ShimmerManager(context)
shimmerManager.initialise()

// Discover paired devices
val availableDevices = shimmerManager.getAvailableDevices()
```

### 2. Connection and Configuration
```kotlin
// Connect to device
val deviceAddress = "00:06:66:12:34:56"
shimmerManager.connectToDevice(deviceAddress)

// Configure sensors
val config = mapOf(
    "samplingRate" to 128.0,    // 128 Hz optimal for physiology
    "gsrRange" to 0,            // High sensitivity range
    "enabledSensors" to 0x84L   // GSR + Accelerometer
)
shimmer.writeCompleteConfiguration(config)
```

### 3. Start Recording
```kotlin
// Start data streaming
shimmerManager.startStreaming()

// Access real-time data
val gsrValue = shimmer.getGSRReading()
val ppgValue = shimmer.getPPGReading()
```

## Supported Sensors

| Sensor | Channel | Sampling Rate | Range | Use Case |
|--------|---------|---------------|-------|----------|
| **GSR** | GSR_CONDUCTANCE | 25.6-512 Hz | 0.5-15 μS | Stress detection |
| **PPG** | INT_EXP_ADC_A13 | 25.6-512 Hz | Variable | Heart rate monitoring |
| **Accelerometer** | ACCEL_X/Y/Z | 25.6-512 Hz | ±2g to ±16g | Motion tracking |
| **Gyroscope** | GYRO_X/Y/Z | 25.6-512 Hz | ±250°/s to ±2000°/s | Rotation detection |
| **Magnetometer** | MAG_X/Y/Z | 25.6-512 Hz | ±1.3 to ±8.1 Ga | Orientation |
| **ECG** | EXG1_24BIT | 25.6-512 Hz | ±3mV | Cardiac monitoring |
| **EMG** | EXG2_24BIT | 25.6-512 Hz | ±3mV | Muscle activity |

## User Interface Components

### Shimmer Dashboard
Embedded directly in the main recording workflow:

- **Real-time Status**: Connection, battery, and signal strength indicators
- **Quick Controls**: Start/stop recording with immediate feedback
- **Live Preview**: Current sensor values during recording

### Shimmer Control Panel
Dedicated configuration screen (`ShimmerConfigActivity`):

- **Device Management**: Scan, connect, disconnect operations
- **Sensor Configuration**: Interactive toggle chips for all sensor types
- **Performance Monitoring**: Data rates, connection quality, battery status
- **Real-time Visualization**: Live charts for all sensor channels

### Configuration Options

#### Sampling Rate Configuration
```kotlin
// Available sampling rates (Hz)
val samplingRates = listOf(25.6, 51.2, 128.0, 256.0, 512.0)

// Set sampling rate
viewModel.updateSamplingRate(128.0) // Optimal for physiological signals
```

#### Sensor Range Configuration
```kotlin
// GSR range settings (resistance to conductance mapping)
val gsrRanges = mapOf(
    0 to "10-56 kΩ (High Sensitivity)",    // ~18-100 μS
    1 to "56-220 kΩ (Medium)",            // ~4.5-18 μS  
    2 to "220-680 kΩ (Low)",              // ~1.5-4.5 μS
    3 to "680-4.7 MΩ (Very Low)",         // ~0.2-1.5 μS
    4 to "Auto Range"                      // Automatic switching
)

// Accelerometer range settings
val accelRanges = listOf(2, 4, 8, 16) // ±g values
```

## API Integration Examples

### Basic Sensor Reading
```kotlin
class ShimmerDataCollector(private val shimmer: Shimmer) {
    
    fun collectSensorData(): SensorReading? {
        if (!shimmer.isConnected()) return null
        
        return SensorReading(
            timestamp = System.currentTimeMillis(),
            gsr = shimmer.getGSRReading(),
            ppg = shimmer.getPPGReading(),
            accelX = shimmer.getAccelXReading(),
            accelY = shimmer.getAccelYReading(),
            accelZ = shimmer.getAccelZReading(),
            battery = shimmer.getBatteryLevel()
        )
    }
}
```

### Multi-Device Coordination
```kotlin
class MultiDeviceCoordinator {
    
    fun synchronizeDevices(deviceAddresses: List<String>) {
        deviceAddresses.forEach { address ->
            val shimmer = shimmerManager.getConnectedDevice(address)
            shimmer?.let {
                // Synchronise sampling rates
                it.writeCompleteConfiguration(mapOf("samplingRate" to 128.0))
            }
        }
    }
    
    fun collectSynchronizedData(): Map<String, SensorReading> {
        val timestamp = System.currentTimeMillis()
        return connectedDevices.mapValues { (_, shimmer) ->
            collectSensorData(shimmer).copy(timestamp = timestamp)
        }
    }
}
```

### Data Quality Assessment
```kotlin
class DataQualityMonitor {
    
    fun assessSignalQuality(shimmer: Shimmer): DataQuality {
        val recentReadings = collectRecentReadings(shimmer, 10)
        
        if (recentReadings.isEmpty()) return DataQuality.POOR
        
        val gsrValues = recentReadings.mapNotNull { it.gsr }
        val variance = calculateVariance(gsrValues)
        val snr = calculateSNR(gsrValues)
        
        return when {
            snr > 0.8 && variance in 0.1..5.0 -> DataQuality.EXCELLENT
            snr > 0.6 && variance in 0.05..10.0 -> DataQuality.GOOD
            snr > 0.4 -> DataQuality.FAIR
            else -> DataQuality.POOR
        }
    }
}
```

## Recording Session Integration

### Session-based Recording
```kotlin
class ShimmerSessionManager {
    
    suspend fun startSession(sessionId: String, deviceIds: List<String>): Boolean {
        return try {
            // Configure all devices
            deviceIds.forEach { deviceId ->
                configureDeviceForSession(deviceId)
            }
            
            // Start coordinated recording
            shimmerRecorder.startRecording(sessionId)
            
            // Monitor session health
            monitorSessionHealth(sessionId, deviceIds)
            
            true
        } catch (e: Exception) {
            logger.error("Failed to start session", e)
            false
        }
    }
    
    private fun configureDeviceForSession(deviceId: String) {
        val shimmer = shimmerManager.getConnectedDevice(deviceId)
        shimmer?.let {
            val sessionConfig = mapOf(
                "samplingRate" to 128.0,
                "gsrRange" to 0,
                "accelRange" to 8,
                "enabledSensors" to 0x84L // GSR + Accelerometer
            )
            it.writeCompleteConfiguration(sessionConfig)
        }
    }
}
```

## Data Export and Analysis

### CSV Export Format
The system exports Shimmer data in research-grade CSV format:

```csv
timestamp,device_id,gsr_us,ppg,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,battery_percent,signal_quality
1640995200000,Shimmer_A1B2,5.23,1024.5,0.98,0.02,-0.15,2.1,-1.3,0.8,85,GOOD
1640995200008,Shimmer_A1B2,5.28,1026.1,0.97,0.03,-0.16,2.2,-1.2,0.9,85,GOOD
```

### Data Analysis Integration
```kotlin
class ShimmerDataAnalyzer {
    
    fun analyzeSessionData(sessionId: String): SessionAnalysis {
        val csvFiles = getSessionShimmerFiles(sessionId)
        
        return SessionAnalysis(
            duration = calculateSessionDuration(csvFiles),
            dataQuality = assessOverallQuality(csvFiles),
            gsrStatistics = calculateGSRStatistics(csvFiles),
            motionMetrics = calculateMotionMetrics(csvFiles),
            syncQuality = assessSynchronizationQuality(csvFiles)
        )
    }
    
    private fun calculateGSRStatistics(csvFiles: List<File>): GSRStatistics {
        val allGSRValues = csvFiles.flatMap { parseGSRValues(it) }
        
        return GSRStatistics(
            mean = allGSRValues.average(),
            variance = calculateVariance(allGSRValues),
            peaks = detectGSRPeaks(allGSRValues),
            driftRate = calculateDriftRate(allGSRValues)
        )
    }
}
```

## Error Handling and Troubleshooting

### Common Issues and Solutions

#### Connection Failures
```kotlin
class ConnectionTroubleshooter {
    
    fun diagnoseConnectionIssue(deviceAddress: String): DiagnosisResult {
        return when {
            !bluetoothAdapter.isEnabled -> 
                DiagnosisResult.Error("Bluetooth disabled")
            !isDevicePaired(deviceAddress) -> 
                DiagnosisResult.Error("Device not paired")
            !isDeviceInRange(deviceAddress) -> 
                DiagnosisResult.Warning("Device may be out of range")
            getBatteryLevel(deviceAddress) < 20 -> 
                DiagnosisResult.Warning("Low battery on device")
            else -> 
                DiagnosisResult.Success("Connection should be possible")
        }
    }
}
```

#### Data Quality Issues
```kotlin
class DataQualityTroubleshooter {
    
    fun diagnosePoorSignalQuality(deviceId: String): List<String> {
        val issues = mutableListOf<String>()
        
        val shimmer = shimmerManager.getConnectedDevice(deviceId)
        shimmer?.let { device ->
            val batteryLevel = device.getBatteryLevel()
            val signalStrength = getSignalStrength(deviceId)
            val gsrReading = device.getGSRReading()
            
            if (batteryLevel != null && batteryLevel < 30) {
                issues.add("Low battery level ($batteryLevel%)")
            }
            
            if (signalStrength < -70) {
                issues.add("Weak Bluetooth signal ($signalStrength dBm)")
            }
            
            if (gsrReading == null) {
                issues.add("No GSR data received - check sensor contact")
            } else if (gsrReading < 0.5) {
                issues.add("Very low GSR reading - poor skin contact")
            } else if (gsrReading > 20.0) {
                issues.add("Very high GSR reading - check sensor placement")
            }
        }
        
        return issues.ifEmpty { listOf("No obvious issues detected") }
    }
}
```

## Performance Optimisation

### Best Practices

#### Efficient Data Handling
```kotlin
class OptimizedShimmerHandler {
    private val dataBuffer = ConcurrentLinkedQueue<SensorReading>()
    private val maxBufferSize = 1000
    
    fun handleIncomingData(objectCluster: ObjectCluster) {
        val sensorReading = convertObjectCluster(objectCluster)
        
        // Add to buffer with size limit
        dataBuffer.offer(sensorReading)
        while (dataBuffer.size > maxBufferSize) {
            dataBuffer.poll() // Remove oldest data
        }
        
        // Process data in batches for efficiency
        if (dataBuffer.size >= batchSize) {
            processBatch(dataBuffer.take(batchSize))
        }
    }
}
```

#### Memory Management
```kotlin
class ShimmerMemoryManager {
    
    fun cleanupResources() {
        // Close file writers
        csvWriters.values.forEach { it.close() }
        csvWriters.clear()
        
        // Clear data buffers
        dataQueues.values.forEach { it.clear() }
        
        // Stop background handlers
        shimmerHandlers.values.forEach { handler ->
            handler.looper.quit()
        }
        
        // Disconnect devices
        connectedDevices.keys.forEach { deviceId ->
            disconnectDevice(deviceId)
        }
    }
}
```

## Research Applications

### Stress Detection Protocol
```kotlin
class StressDetectionProtocol {
    
    fun configureForStressResearch(deviceId: String) {
        val shimmer = shimmerManager.getConnectedDevice(deviceId)
        shimmer?.let {
            val stressConfig = mapOf(
                "samplingRate" to 128.0,     // Optimal for physiological signals
                "gsrRange" to 0,             // High sensitivity for subtle changes
                "accelRange" to 4,           // Moderate range for body movement
                "enabledSensors" to 0x8CL    // GSR + PPG + Accelerometer
            )
            it.writeCompleteConfiguration(stressConfig)
        }
    }
    
    fun detectStressEvents(gsrData: List<Double>): List<StressEvent> {
        val events = mutableListOf<StressEvent>()
        val baseline = gsrData.take(30).average() // First 30 seconds as baseline
        
        for (i in 30 until gsrData.size) {
            val current = gsrData[i]
            val percentIncrease = ((current - baseline) / baseline) * 100
            
            if (percentIncrease > 20) { // 20% increase threshold
                events.add(StressEvent(
                    timestamp = i * (1000 / 128), // Convert to milliseconds
                    magnitude = percentIncrease,
                    gsrValue = current
                ))
            }
        }
        
        return events
    }
}
```

### Heart Rate Variability Analysis
```kotlin
class HRVAnalyzer {
    
    fun analyzeHRV(ppgData: List<Double>, samplingRate: Double): HRVMetrics {
        val peaks = detectPeaks(ppgData, samplingRate)
        val intervals = calculateRRIntervals(peaks, samplingRate)
        
        return HRVMetrics(
            meanHR = 60000.0 / intervals.average(), // BPM
            rmssd = calculateRMSSD(intervals),      // HRV measure
            sdnn = calculateSDNN(intervals),        // Standard deviation
            pnn50 = calculatePNN50(intervals)       // Percentage of successive differences > 50ms
        )
    }
}
```

## Integration Testing

### Unit Tests
```kotlin
@Test
fun testShimmerDeviceConnection() {
    val mockShimmer = mockk<Shimmer>()
    every { mockShimmer.isConnected() } returns true
    every { mockShimmer.getGSRReading() } returns 5.25
    
    val reading = collectSensorData(mockShimmer)
    
    assertThat(reading?.gsr).isEqualTo(5.25)
}

@Test
fun testDataQualityAssessment() {
    val goodGSRData = listOf(5.1, 5.3, 5.0, 5.4, 5.2)
    val quality = assessDataQuality(goodGSRData)
    
    assertThat(quality).isEqualTo(DataQuality.EXCELLENT)
}
```

### Integration Tests
```kotlin
@Test
fun testEndToEndRecording() = runTest {
    // Setup
    val sessionId = "test_session_${System.currentTimeMillis()}"
    val deviceAddress = "00:06:66:12:34:56"
    
    // Start recording
    val success = shimmerRecorder.startRecording(sessionId)
    assertThat(success).isTrue()
    
    // Collect data for 5 seconds
    delay(5000)
    
    // Stop recording
    shimmerRecorder.stopRecording()
    
    // Verify data file was created
    val dataFile = getSessionDataFile(sessionId, deviceAddress)
    assertThat(dataFile.exists()).isTrue()
    assertThat(dataFile.length()).isGreaterThan(0)
}
```

## Related Documentation

- **[Shimmer API Reference](api/shimmer-api.md)**: Complete API documentation
- **[Usage Examples](api/shimmer-usage-examples.md)**: Code examples and patterns
- **[Android App Architecture](../AndroidApp/README.md)**: Overall app architecture
- **[Test Documentation](../tests_unified/README.md)**: Testing framework and procedures

## Support and Troubleshooting

For additional support:

1. **Check Logs**: Enable debug logging to diagnose issues
2. **Verify Permissions**: Ensure Bluetooth and location permissions are granted
3. **Device Compatibility**: Confirm device compatibility with Shimmer3 GSR+
4. **API Documentation**: Refer to official ShimmerAndroidAPI documentation
5. **Issue Tracking**: Report bugs via GitHub Issues

---

*This implementation provides a complete, research-grade interface to Shimmer sensors while maintaining compatibility with official API patterns and providing enhanced functionality for academic research applications.*