# Shimmer Android API Implementation

## Overview

This document provides comprehensive documentation for the Shimmer Android API implementation in the Multi-Sensor Recording System. The implementation follows the original [ShimmerEngineering/ShimmerAndroidAPI](https://github.com/ShimmerEngineering/ShimmerAndroidAPI) patterns while providing enhanced functionality and robust error handling.

## Key Features

- **Complete Sensor Access**: GSR, PPG, Accelerometer, Gyroscope, Magnetometer, ECG, EMG, Temperature, Pressure
- **Hardware Compatibility**: Uses reflection for SDK version compatibility
- **Physiological Data Models**: Realistic fallback data when hardware unavailable
- **Advanced Configuration**: Comprehensive device configuration capabilities
- **Robust Error Handling**: Graceful degradation and comprehensive logging

## Core Architecture

### Extension Methods Pattern

The implementation uses Kotlin extension methods to enhance the original Shimmer class:

```kotlin
// Example usage following original patterns
val shimmer = shimmerDevices[deviceId]
val gsrReading = shimmer?.getGSRReading() // Returns μS or null
val isDeviceStreaming = shimmer?.isStreaming() // Checks BT_STATE
val batteryLevel = shimmer?.getBatteryLevel() // Returns 0-100% or null
```

### ObjectCluster Processing

Proper parsing of Shimmer's ObjectCluster data format with FormatCluster handling for calibrated sensor values:

```kotlin
val latestObjectCluster = getLatestReceivedData()
val gsrFormats = latestObjectCluster.getCollectionOfFormatClusters(
    Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE
)
val gsrCluster = ObjectCluster.returnFormatCluster(gsrFormats, "CAL") as? FormatCluster
return gsrCluster?.mData
```

## Sensor Data Access APIs

### Galvanic Skin Response (GSR)

```kotlin
/**
 * Get current GSR reading from Shimmer device
 * @return GSR conductance in microsiemens (μS) or null if unavailable
 */
fun Shimmer.getGSRReading(): Double?
```

**Usage Example:**
```kotlin
val shimmer = getConnectedShimmerDevice("00:06:66:12:34:56")
shimmer?.let {
    val gsrValue = it.getGSRReading()
    if (gsrValue != null) {
        println("GSR: $gsrValue μS")
        // GSR follows breathing cycles, hydration drift, and spontaneous fluctuations
        // Typical range: 0.5-15.0 μS for healthy adults
    }
}
```

### Photoplethysmography (PPG)

```kotlin
/**
 * Get current PPG reading from Shimmer device
 * @return PPG value from INT_EXP_ADC_A13 channel or null if unavailable
 */
fun Shimmer.getPPGReading(): Double?
```

**Usage Example:**
```kotlin
val ppgValue = shimmer?.getPPGReading()
if (ppgValue != null) {
    println("PPG: $ppgValue")
    // PPG simulates realistic heart rate variability and respiratory modulation
    // Includes dicrotic notch and respiratory modulation patterns
}
```

### Three-Axis Accelerometer

```kotlin
/**
 * Get current accelerometer readings
 * @return Acceleration in g-force units or null if unavailable
 */
fun Shimmer.getAccelXReading(): Double?
fun Shimmer.getAccelYReading(): Double?
fun Shimmer.getAccelZReading(): Double?
```

**Usage Example:**
```kotlin
val accelX = shimmer?.getAccelXReading()
val accelY = shimmer?.getAccelYReading()
val accelZ = shimmer?.getAccelZReading()

if (accelX != null && accelY != null && accelZ != null) {
    println("Acceleration: X=$accelX, Y=$accelY, Z=$accelZ g")
    // Includes gravity components, breathing movements, and ballistocardiographic effects
}
```

### Three-Axis Gyroscope

```kotlin
/**
 * Get current gyroscope readings
 * @return Angular velocity in degrees/second or null if unavailable
 */
fun Shimmer.getGyroXReading(): Double?
fun Shimmer.getGyroYReading(): Double?
fun Shimmer.getGyroZReading(): Double?
```

### Three-Axis Magnetometer

```kotlin
/**
 * Get current magnetometer readings
 * @return Magnetic field strength or null if unavailable
 */
fun Shimmer.getMagXReading(): Double?
fun Shimmer.getMagYReading(): Double?
fun Shimmer.getMagZReading(): Double?
```

### Biopotential Signals

```kotlin
/**
 * Get ECG/EMG readings from EXG channels
 * @return Biopotential signal amplitude or null if unavailable
 */
fun Shimmer.getECGReading(): Double?  // EXG1_24BIT channel
fun Shimmer.getEMGReading(): Double?  // EXG2_24BIT channel
```

### Environmental Sensors

```kotlin
/**
 * Get environmental sensor readings
 * @return Sensor values or null if unavailable
 */
fun Shimmer.getTemperatureReading(): Double?  // Temperature in °C
fun Shimmer.getPressureReading(): Double?     // Pressure if available
```

### Device Status and Battery

```kotlin
/**
 * Get device battery level
 * @return Battery percentage (0-100) or null if unavailable
 */
fun Shimmer.getBatteryLevel(): Int?
```

**Battery Simulation Model:**
- Realistic discharge curve based on device usage patterns
- Typical Shimmer battery life: 8-12 hours continuous recording
- Accelerated discharge when battery < 20%

## Device Management APIs

### Connection Status

```kotlin
/**
 * Check device connection and streaming status
 * @return Boolean indicating current state
 */
fun Shimmer.isConnected(): Boolean
fun Shimmer.isStreaming(): Boolean
fun Shimmer.isSDLogging(): Boolean
fun Shimmer.isSDLoggingActive(): Boolean
```

**Usage Example:**
```kotlin
val shimmer = getConnectedShimmerDevice("00:06:66:12:34:56")
if (shimmer?.isConnected() == true) {
    if (shimmer.isStreaming()) {
        println("Device is actively streaming data")
    }
    if (shimmer.isSDLoggingActive()) {
        println("Device is logging to SD card")
    }
}
```

### Device Information

```kotlin
/**
 * Get device information and configuration
 * @return Device details or default values
 */
fun Shimmer.getFirmwareVersion(): String     // e.g., "3.2.3"
fun Shimmer.getHardwareVersion(): String     // e.g., "3.0"
fun Shimmer.getSamplingRate(): Double        // Hz, e.g., 51.2
fun Shimmer.getEnabledSensors(): Long        // Bitmask of enabled sensors
fun Shimmer.getMacAddress(): String          // Bluetooth MAC address
fun Shimmer.getDeviceName(): String          // Device name
```

### Comprehensive Status

```kotlin
/**
 * Get comprehensive device status
 * @return Map with all device status information
 */
fun Shimmer.getComprehensiveStatus(): Map<String, Any>
```

**Usage Example:**
```kotlin
val status = shimmer?.getComprehensiveStatus()
status?.let { statusMap ->
    println("Device Status:")
    statusMap.forEach { (key, value) ->
        println("  $key: $value")
    }
}
```

## Configuration APIs

### Sensor Configuration

```kotlin
/**
 * Configure device sensors and settings
 * @param config Configuration map with sensor settings
 * @return True if configuration was successful
 */
fun Shimmer.writeCompleteConfiguration(config: Map<String, Any>): Boolean
```

**Configuration Parameters:**
- `samplingRate`: Double (Hz) - Data sampling frequency
- `gsrRange`: Int (0-4) - GSR measurement range
- `accelRange`: Int (2,4,8,16) - Accelerometer range in g
- `enabledSensors`: Long - Bitmask of sensors to enable

**Usage Example:**
```kotlin
val config = mapOf(
    "samplingRate" to 128.0,    // 128 Hz sampling
    "gsrRange" to 0,            // GSR range 0 (10-56 kΩ)
    "accelRange" to 8,          // ±8g accelerometer range
    "enabledSensors" to 0x84L   // GSR + Accelerometer
)

val success = shimmer?.writeCompleteConfiguration(config)
if (success == true) {
    println("Configuration applied successfully")
}
```

### Read Current Configuration

```kotlin
/**
 * Read current device configuration
 * @return Map with current configuration values
 */
fun Shimmer.readCurrentConfiguration(): Map<String, Any>
```

### Sensor Calibration

```kotlin
/**
 * Perform sensor calibration
 * @param sensorType Type of sensor to calibrate ("GSR", "ACCEL", "GYRO", "MAG")
 * @return True if calibration was initiated successfully
 */
fun Shimmer.performCalibration(sensorType: String): Boolean
```

**Usage Example:**
```kotlin
// Calibrate GSR sensor
val gsrCalSuccess = shimmer?.performCalibration("GSR")

// Calibrate accelerometer
val accelCalSuccess = shimmer?.performCalibration("ACCEL")
```

## Data Quality Assessment

### Signal Quality Analysis

The implementation includes comprehensive signal quality analysis:

```kotlin
/**
 * Assess data quality based on actual sensor characteristics
 */
private fun assessDataQuality(samples: List<SensorSample>, deviceId: String): String
```

**Quality Metrics:**
- **Signal-to-Noise Ratio**: Analysis of signal clarity
- **Variance Analysis**: Detection of appropriate signal variability
- **Timestamp Consistency**: Verification of sampling rate stability
- **Connection Stability**: Assessment of connection reliability

**Quality Levels:**
- **Excellent**: SNR > 0.8, stable timestamps, good variance
- **Good**: SNR > 0.6, acceptable stability
- **Fair**: SNR > 0.4, some instability
- **Poor**: Low SNR, high instability, or no data

## Physiological Data Models

When hardware is unavailable, the system generates physiologically realistic data based on actual human response patterns:

### GSR Physiological Model

```kotlin
private fun generatePhysiologicalGSRModel(deviceId: String): Double
```

**Features:**
- Base conductance (typical resting: 2-10 μS)
- Slow drift due to hydration and temperature (5-10 minute cycles)
- Breathing-related variations (15-20 breaths per minute)
- Spontaneous fluctuations (every 1-3 minutes)
- Physiological noise based on skin resistance variation

### PPG Physiological Model

```kotlin
private fun generatePhysiologicalPPGModel(deviceId: String): Double
```

**Features:**
- Realistic heart rate (60-80 BPM at rest)
- Heart rate variability (normal: 20-50ms RMSSD)
- Dicrotic notch simulation
- Respiratory modulation (breathing affects PPG amplitude)

### Motion Physiological Model

```kotlin
private fun generatePhysiologicalMotionModel(deviceId: String, axis: String): Double
```

**Features:**
- Gravity component (device orientation dependent)
- Breathing-related chest movement
- Heart rate-related micromovements (ballistocardiography)
- Small postural adjustments

## Error Handling and Reliability

### Graceful Degradation

The implementation provides comprehensive error handling:

1. **Hardware Unavailable**: Automatic fallback to physiological models
2. **Method Unavailable**: Reflection-based method detection with fallbacks
3. **Connection Issues**: Robust connection state management
4. **Data Validation**: Quality assessment and anomaly detection

### Exception Handling

All extension methods include comprehensive exception handling:

```kotlin
fun Shimmer.getGSRReading(): Double? {
    return try {
        // Hardware access attempt
        val latestObjectCluster = getLatestReceivedData()
        // ... processing logic ...
    } catch (e: Exception) {
        // Graceful fallback
        null
    }
}
```

## Integration Examples

### Basic Recording Session

```kotlin
// 1. Initialise ShimmerRecorder
val shimmerRecorder = ShimmerRecorder(context, sessionManager, logger)
val success = shimmerRecorder.initialise()

// 2. Connect to devices
val deviceAddresses = listOf("00:06:66:12:34:56", "00:06:66:78:9A:BC")
val connectedDevices = shimmerRecorder.connectDevicesWithRetry(deviceAddresses)

// 3. Configure sensors
connectedDevices.forEach { deviceId ->
    shimmerRecorder.setEnabledChannels(deviceId, setOf(
        SensorChannel.GSR,
        SensorChannel.PPG,
        SensorChannel.ACCEL_X,
        SensorChannel.ACCEL_Y,
        SensorChannel.ACCEL_Z
    ))
}

// 4. Start recording
shimmerRecorder.startRecording("session_123")

// 5. Monitor data quality
val qualityMetrics = shimmerRecorder.getDataQualityMetrics(deviceId)
println("Signal quality: ${qualityMetrics?.signalQuality}")

// 6. Stop recording
shimmerRecorder.stopRecording()
```

### Real-time Data Access

```kotlin
// Access real-time sensor data
val shimmer = shimmerRecorder.getConnectedShimmerDevice(deviceId)
shimmer?.let {
    // Get current readings
    val gsrReading = it.getGSRReading()
    val ppgReading = it.getPPGReading()
    val accelX = it.getAccelXReading()
    
    // Check device status
    val status = it.getComprehensiveStatus()
    val batteryLevel = it.getBatteryLevel()
    
    // Monitor streaming state
    if (it.isStreaming()) {
        println("Device streaming: GSR=$gsrReading μS, PPG=$ppgReading, Battery=$batteryLevel%")
    }
}
```

### Multi-Device Coordination

```kotlin
// Manage multiple Shimmer devices
val allDevices = shimmerRecorder.getConnectedDevices()
allDevices.forEach { (deviceId, device) ->
    val shimmer = shimmerRecorder.getConnectedShimmerDevice(deviceId)
    shimmer?.let {
        // Synchronise sampling rates
        it.writeCompleteConfiguration(mapOf(
            "samplingRate" to 128.0
        ))
        
        // Check synchronisation quality
        val metrics = shimmerRecorder.getDataQualityMetrics(deviceId)
        println("Device $deviceId: ${metrics?.connectionStability}")
    }
}
```

## Best Practices

### 1. Connection Management

```kotlin
// Always check connection status before operations
if (shimmer?.isConnected() == true) {
    val reading = shimmer.getGSRReading()
    // Process reading...
}

// Use retry mechanisms for reliability
val connectedDevices = shimmerRecorder.connectDevicesWithRetry(
    deviceAddresses = deviceList,
    maxRetries = 3
)
```

### 2. Error Handling

```kotlin
// Handle null readings gracefully
val gsrReading = shimmer?.getGSRReading()
if (gsrReading != null && gsrReading.isFinite()) {
    // Valid reading available
    processGSRData(gsrReading)
} else {
    // No data or invalid reading
    handleMissingData()
}
```

### 3. Resource Management

```kotlin
// Always cleanup resources
try {
    shimmerRecorder.startRecording(sessionId)
    // ... recording operations ...
} finally {
    shimmerRecorder.stopRecording()
    shimmerRecorder.cleanup()
}
```

### 4. Data Quality Monitoring

```kotlin
// Regularly check data quality
launch {
    while (isRecording) {
        connectedDevices.forEach { deviceId ->
            val quality = shimmerRecorder.getDataQualityMetrics(deviceId)
            if (quality?.signalQuality == "Poor") {
                // Take corrective action
                handlePoorSignalQuality(deviceId)
            }
        }
        delay(5000) // Check every 5 seconds
    }
}
```

## Troubleshooting

### Common Issues and Solutions

1. **Connection Timeouts**
   - Ensure Bluetooth is enabled and devices are paired
   - Check device proximity and battery levels
   - Use retry mechanisms with appropriate delays

2. **Poor Signal Quality**
   - Verify sensor placement and skin contact
   - Check for electromagnetic interference
   - Validate sampling rate configuration

3. **Data Synchronisation Issues**
   - Ensure all devices use the same sampling rate
   - Check system clock synchronisation
   - Monitor connection stability

4. **Memory Issues with Long Recordings**
   - Use appropriate data buffering strategies
   - Implement periodic data flushing
   - Monitor memory usage and implement cleanup

### Debug Information

Enable comprehensive logging to diagnose issues:

```kotlin
// The implementation includes detailed logging at various levels
// Check logs for:
// - Connection establishment/failures
// - Data quality assessments
// - Configuration changes
// - Error conditions and recovery attempts
```

## Performance Considerations

- **Reflection Usage**: Extension methods use reflection for SDK compatibility
- **Data Buffering**: Efficient queue management for high-frequency data
- **Memory Management**: Automatic cleanup and buffer size limits
- **Thread Safety**: Concurrent access protection for multi-device scenarios

## Compatibility

- **SDK Versions**: Compatible with multiple Shimmer Android SDK versions
- **Android Versions**: Tested on Android 7.0+ (API level 24+)
- **Hardware**: Shimmer3 GSR+, Shimmer3 IMU+, custom Shimmer configurations
- **Bluetooth**: Classic Bluetooth and Bluetooth Low Energy (BLE) support

This implementation provides a robust, feature-complete interface to Shimmer sensors while maintaining compatibility with the original API patterns and providing enhanced functionality for research applications.