# Shimmer Android API Usage Examples

This document provides practical usage examples for the Shimmer Android API extension methods implementation.

## Basic Device Connection and Data Access

### Simple GSR Reading

```kotlin
// Get a connected Shimmer device
val shimmer = shimmerRecorder.getConnectedShimmerDevice("00:06:66:12:34:56")

// Check if device is connected and get GSR reading
shimmer?.let {
    if (it.isConnected()) {
        val gsrValue = it.getGSRReading()
        if (gsrValue != null) {
            println("GSR Conductance: $gsrValue μS")
            
            // Interpret GSR values
            when {
                gsrValue < 2.0 -> println("Low skin conductance - very relaxed")
                gsrValue < 5.0 -> println("Normal skin conductance - relaxed")
                gsrValue < 10.0 -> println("Elevated skin conductance - mild arousal")
                else -> println("High skin conductance - high arousal/stress")
            }
        } else {
            println("GSR reading not available")
        }
    } else {
        println("Device not connected")
    }
}
```

### Multi-Sensor Data Collection

```kotlin
// Collect data from multiple sensors simultaneously
fun collectMultiSensorData(shimmer: Shimmer): SensorReading? {
    if (!shimmer.isConnected()) return null
    
    return SensorReading(
        timestamp = System.currentTimeMillis(),
        gsr = shimmer.getGSRReading(),
        ppg = shimmer.getPPGReading(),
        accelX = shimmer.getAccelXReading(),
        accelY = shimmer.getAccelYReading(),
        accelZ = shimmer.getAccelZReading(),
        gyroX = shimmer.getGyroXReading(),
        gyroY = shimmer.getGyroYReading(),
        gyroZ = shimmer.getGyroZReading(),
        battery = shimmer.getBatteryLevel(),
        temperature = shimmer.getTemperatureReading()
    )
}

data class SensorReading(
    val timestamp: Long,
    val gsr: Double?,
    val ppg: Double?,
    val accelX: Double?,
    val accelY: Double?,
    val accelZ: Double?,
    val gyroX: Double?,
    val gyroY: Double?,
    val gyroZ: Double?,
    val battery: Int?,
    val temperature: Double?
)

// Usage
val reading = collectMultiSensorData(shimmer)
reading?.let {
    println("Sensor Reading at ${it.timestamp}:")
    println("  GSR: ${it.gsr} μS")
    println("  PPG: ${it.ppg}")
    println("  Acceleration: (${it.accelX}, ${it.accelY}, ${it.accelZ}) g")
    println("  Angular Velocity: (${it.gyroX}, ${it.gyroY}, ${it.gyroZ}) °/s")
    println("  Battery: ${it.battery}%")
    println("  Temperature: ${it.temperature}°C")
}
```

## Device Configuration Examples

### Complete Device Configuration

```kotlin
// Configure a Shimmer device with custom settings
fun configureShimmerDevice(shimmer: Shimmer): Boolean {
    if (!shimmer.isConnected()) {
        println("Device not connected, cannot configure")
        return false
    }
    
    // Define configuration
    val config = mapOf(
        "samplingRate" to 128.0,    // 128 Hz sampling rate
        "gsrRange" to 0,            // GSR range 0 (10-56 kΩ → ~18-100 μS)
        "accelRange" to 8,          // ±8g accelerometer range
        "enabledSensors" to 0xFFFF  // Enable all available sensors
    )
    
    // Apply configuration
    val success = shimmer.writeCompleteConfiguration(config)
    
    if (success) {
        println("Configuration applied successfully")
        
        // Verify configuration was applied
        val currentConfig = shimmer.readCurrentConfiguration()
        currentConfig.forEach { (key, value) ->
            println("  $key: $value")
        }
        
        return true
    } else {
        println("Failed to apply configuration")
        return false
    }
}
```

### Sensor Calibration

```kotlin
// Perform calibration for multiple sensors
fun calibrateAllSensors(shimmer: Shimmer) {
    if (!shimmer.isConnected()) {
        println("Device not connected, cannot calibrate")
        return
    }
    
    val sensorsToCalibrate = listOf("GSR", "ACCEL", "GYRO", "MAG")
    
    sensorsToCalibrate.forEach { sensorType ->
        println("Calibrating $sensorType sensor...")
        
        val success = shimmer.performCalibration(sensorType)
        if (success) {
            println("$sensorType calibration initiated successfully")
        } else {
            println("Failed to initiate $sensorType calibration")
        }
        
        // Wait between calibrations
        Thread.sleep(1000)
    }
}
```

## Real-time Monitoring Examples

### Continuous Data Monitoring

```kotlin
class ShimmerDataMonitor(private val shimmer: Shimmer) {
    private var isMonitoring = false
    private val monitoringScope = CoroutineScope(Dispatchers.IO)
    
    fun startMonitoring() {
        if (!shimmer.isConnected()) {
            println("Cannot start monitoring - device not connected")
            return
        }
        
        isMonitoring = true
        
        monitoringScope.launch {
            while (isMonitoring) {
                try {
                    // Collect current readings
                    val reading = collectMultiSensorData(shimmer)
                    
                    reading?.let {
                        // Process the reading
                        processReading(it)
                        
                        // Check for alert conditions
                        checkAlertConditions(it)
                    }
                    
                    // Delay based on sampling rate
                    delay(1000 / 128) // 128 Hz = ~7.8ms interval
                    
                } catch (e: Exception) {
                    println("Error during monitoring: ${e.message}")
                    // Continue monitoring despite errors
                }
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
    }
    
    private fun processReading(reading: SensorReading) {
        // Calculate derived metrics
        reading.apply {
            // Calculate total acceleration magnitude
            if (accelX != null && accelY != null && accelZ != null) {
                val totalAccel = sqrt(accelX*accelX + accelY*accelY + accelZ*accelZ)
                println("Total acceleration: $totalAccel g")
            }
            
            // Calculate angular velocity magnitude
            if (gyroX != null && gyroY != null && gyroZ != null) {
                val totalAngularVel = sqrt(gyroX*gyroX + gyroY*gyroY + gyroZ*gyroZ)
                println("Total angular velocity: $totalAngularVel °/s")
            }
        }
    }
    
    private fun checkAlertConditions(reading: SensorReading) {
        // GSR alert (high stress detection)
        reading.gsr?.let { gsr ->
            if (gsr > 12.0) {
                println("ALERT: High stress detected (GSR: $gsr μS)")
            }
        }
        
        // Battery alert
        reading.battery?.let { battery ->
            if (battery < 20) {
                println("ALERT: Low battery ($battery%)")
            }
        }
        
        // Motion alert (excessive movement)
        reading.apply {
            if (accelX != null && accelY != null && accelZ != null) {
                val movement = sqrt(accelX*accelX + accelY*accelY + accelZ*accelZ)
                if (movement > 2.0) {
                    println("ALERT: Excessive movement detected ($movement g)")
                }
            }
        }
    }
}

// Usage
val monitor = ShimmerDataMonitor(shimmer)
monitor.startMonitoring()

// Stop monitoring after some time
delay(60000) // 1 minute
monitor.stopMonitoring()
```

### Device Status Monitoring

```kotlin
// Monitor device health and connection status
fun monitorDeviceStatus(shimmer: Shimmer) {
    val status = shimmer.getComprehensiveStatus()
    
    println("=== Device Status ===")
    status.forEach { (key, value) ->
        when (key) {
            "isConnected" -> {
                val connected = value as Boolean
                println("Connection: ${if (connected) "✓ Connected" else "✗ Disconnected"}")
            }
            "isStreaming" -> {
                val streaming = value as Boolean
                println("Streaming: ${if (streaming) "✓ Active" else "✗ Inactive"}")
            }
            "batteryLevel" -> {
                val battery = value as Int
                val status = when {
                    battery > 80 -> "Excellent"
                    battery > 50 -> "Good"
                    battery > 20 -> "Low"
                    else -> "Critical"
                }
                println("Battery: $battery% ($status)")
            }
            "samplingRate" -> {
                val rate = value as Double
                println("Sampling Rate: $rate Hz")
            }
            "firmwareVersion" -> {
                println("Firmware: $value")
            }
            "hardwareVersion" -> {
                println("Hardware: $value")
            }
            else -> {
                println("$key: $value")
            }
        }
    }
}
```

## Advanced Usage Examples

### Multi-Device Coordination

```kotlin
// Coordinate multiple Shimmer devices
class MultiDeviceCoordinator(private val shimmerRecorder: ShimmerRecorder) {
    
    fun synchronizeDevices(deviceIds: List<String>): Boolean {
        var allSynchronized = true
        
        deviceIds.forEach { deviceId ->
            val shimmer = shimmerRecorder.getConnectedShimmerDevice(deviceId)
            
            if (shimmer?.isConnected() == true) {
                // Synchronise sampling rate across all devices
                val config = mapOf("samplingRate" to 128.0)
                val success = shimmer.writeCompleteConfiguration(config)
                
                if (!success) {
                    println("Failed to synchronise device $deviceId")
                    allSynchronized = false
                }
            } else {
                println("Device $deviceId not connected")
                allSynchronized = false
            }
        }
        
        return allSynchronized
    }
    
    fun collectSynchronizedReadings(deviceIds: List<String>): Map<String, SensorReading> {
        val readings = mutableMapOf<String, SensorReading>()
        val timestamp = System.currentTimeMillis()
        
        deviceIds.forEach { deviceId ->
            val shimmer = shimmerRecorder.getConnectedShimmerDevice(deviceId)
            shimmer?.let {
                collectMultiSensorData(it)?.let { reading ->
                    // Use synchronised timestamp
                    readings[deviceId] = reading.copy(timestamp = timestamp)
                }
            }
        }
        
        return readings
    }
}
```

### Data Quality Assessment

```kotlin
// Assess data quality in real-time
class DataQualityAssessor {
    
    fun assessDataQuality(shimmer: Shimmer): DataQuality {
        val recentReadings = mutableListOf<SensorReading>()
        
        // Collect multiple readings for quality assessment
        repeat(10) {
            collectMultiSensorData(shimmer)?.let { reading ->
                recentReadings.add(reading)
            }
            Thread.sleep(50) // 20 Hz sampling for quality check
        }
        
        return if (recentReadings.size >= 5) {
            analyzeDataQuality(recentReadings)
        } else {
            DataQuality.POOR
        }
    }
    
    private fun analyzeDataQuality(readings: List<SensorReading>): DataQuality {
        // Analyse GSR signal stability
        val gsrValues = readings.mapNotNull { it.gsr }
        
        if (gsrValues.isEmpty()) {
            return DataQuality.POOR
        }
        
        val gsrMean = gsrValues.average()
        val gsrVariance = gsrValues.map { (it - gsrMean) * (it - gsrMean) }.average()
        
        // Analyse timestamp consistency
        val timestamps = readings.map { it.timestamp }
        val intervals = timestamps.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()
        val intervalVariability = intervals.map { abs(it - avgInterval) }.average()
        
        // Calculate quality score
        val variabilityScore = when {
            gsrVariance < 0.1 -> 0.2 // Too stable (poor contact)
            gsrVariance < 1.0 -> 1.0 // Good variability
            gsrVariance < 5.0 -> 0.8 // Acceptable variability
            else -> 0.3 // Too noisy
        }
        
        val timingScore = if (intervalVariability < avgInterval * 0.1) 1.0 else 0.5
        
        val overallScore = (variabilityScore + timingScore) / 2.0
        
        return when {
            overallScore > 0.8 -> DataQuality.EXCELLENT
            overallScore > 0.6 -> DataQuality.GOOD
            overallScore > 0.4 -> DataQuality.FAIR
            else -> DataQuality.POOR
        }
    }
}

enum class DataQuality {
    EXCELLENT, GOOD, FAIR, POOR
}
```

### Error Handling and Recovery

```kotlin
// Robust device communication with error handling
class RobustShimmerInterface(private val shimmer: Shimmer) {
    
    fun getSafeReading(): SafeSensorReading {
        return try {
            if (!shimmer.isConnected()) {
                return SafeSensorReading.disconnected()
            }
            
            val reading = collectMultiSensorData(shimmer)
            if (reading != null) {
                SafeSensorReading.success(reading)
            } else {
                SafeSensorReading.noData()
            }
            
        } catch (e: Exception) {
            SafeSensorReading.error(e.message ?: "Unknown error")
        }
    }
    
    fun performSafeConfiguration(config: Map<String, Any>): ConfigurationResult {
        return try {
            if (!shimmer.isConnected()) {
                return ConfigurationResult.Failure("Device not connected")
            }
            
            // Validate configuration before applying
            val validationResult = validateConfiguration(config)
            if (!validationResult.isValid) {
                return ConfigurationResult.Failure("Invalid configuration: ${validationResult.errors}")
            }
            
            val success = shimmer.writeCompleteConfiguration(config)
            if (success) {
                ConfigurationResult.Success("Configuration applied successfully")
            } else {
                ConfigurationResult.Failure("Failed to apply configuration")
            }
            
        } catch (e: Exception) {
            ConfigurationResult.Failure("Configuration error: ${e.message}")
        }
    }
    
    private fun validateConfiguration(config: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()
        
        config["samplingRate"]?.let { rate ->
            if (rate !is Double || rate <= 0 || rate > 1024) {
                errors.add("Invalid sampling rate: $rate")
            }
        }
        
        config["gsrRange"]?.let { range ->
            if (range !is Int || range < 0 || range > 4) {
                errors.add("Invalid GSR range: $range")
            }
        }
        
        config["accelRange"]?.let { range ->
            if (range !is Int || range !in listOf(2, 4, 8, 16)) {
                errors.add("Invalid accelerometer range: $range")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
}

sealed class SafeSensorReading {
    data class Success(val reading: SensorReading) : SafeSensorReading()
    object Disconnected : SafeSensorReading()
    object NoData : SafeSensorReading()
    data class Error(val message: String) : SafeSensorReading()
    
    companion object {
        fun success(reading: SensorReading) = Success(reading)
        fun disconnected() = Disconnected
        fun noData() = NoData
        fun error(message: String) = Error(message)
    }
}

sealed class ConfigurationResult {
    data class Success(val message: String) : ConfigurationResult()
    data class Failure(val message: String) : ConfigurationResult()
}

data class ValidationResult(val isValid: Boolean, val errors: List<String>)
```

## Integration with Recording Sessions

### Session-based Data Collection

```kotlin
// Integrate Shimmer readings with recording sessions
class ShimmerSessionRecorder(
    private val shimmerRecorder: ShimmerRecorder,
    private val sessionManager: SessionManager
) {
    
    suspend fun startSessionWithShimmerDevices(
        sessionId: String,
        deviceIds: List<String>
    ): Boolean {
        try {
            // Configure all devices
            deviceIds.forEach { deviceId ->
                val shimmer = shimmerRecorder.getConnectedShimmerDevice(deviceId)
                shimmer?.let {
                    configureForSession(it)
                }
            }
            
            // Start recording session
            val success = shimmerRecorder.startRecording(sessionId)
            
            if (success) {
                println("Session $sessionId started with ${deviceIds.size} Shimmer devices")
                
                // Monitor session health
                monitorSessionHealth(sessionId, deviceIds)
            }
            
            return success
            
        } catch (e: Exception) {
            println("Failed to start session: ${e.message}")
            return false
        }
    }
    
    private fun configureForSession(shimmer: Shimmer) {
        val sessionConfig = mapOf(
            "samplingRate" to 128.0,
            "gsrRange" to 0,
            "accelRange" to 8,
            "enabledSensors" to 0x84L // GSR + Accelerometer
        )
        
        shimmer.writeCompleteConfiguration(sessionConfig)
    }
    
    private suspend fun monitorSessionHealth(sessionId: String, deviceIds: List<String>) {
        val monitoringScope = CoroutineScope(Dispatchers.IO)
        
        monitoringScope.launch {
            while (shimmerRecorder.getShimmerStatus().isRecording) {
                deviceIds.forEach { deviceId ->
                    val qualityMetrics = shimmerRecorder.getDataQualityMetrics(deviceId)
                    
                    qualityMetrics?.let { metrics ->
                        if (metrics.signalQuality == "Poor") {
                            println("WARNING: Poor signal quality on device $deviceId")
                        }
                        
                        if (metrics.batteryLevel < 20) {
                            println("WARNING: Low battery on device $deviceId (${metrics.batteryLevel}%)")
                        }
                    }
                }
                
                delay(5000) // Check every 5 seconds
            }
        }
    }
}
```

## Best Practices Summary

### 1. Always Check Connection Status

```kotlin
// Good practice
if (shimmer.isConnected()) {
    val reading = shimmer.getGSRReading()
    // Process reading...
}

// Poor practice - may cause exceptions
val reading = shimmer.getGSRReading() // Could fail if disconnected
```

### 2. Handle Null Values Gracefully

```kotlin
// Good practice
val gsrReading = shimmer.getGSRReading()
if (gsrReading != null && gsrReading.isFinite()) {
    processGSRData(gsrReading)
} else {
    handleMissingData()
}

// Poor practice - may cause crashes
val gsrReading = shimmer.getGSRReading()!!
processGSRData(gsrReading) // Will crash if reading is null
```

### 3. Use Coroutines for Long-Running Operations

```kotlin
// Good practice - non-blocking
suspend fun collectDataPeriodically(shimmer: Shimmer) = withContext(Dispatchers.IO) {
    while (isActive) {
        val reading = collectMultiSensorData(shimmer)
        // Process reading...
        delay(1000 / 128) // Respect sampling rate
    }
}

// Poor practice - blocks UI thread
fun collectDataPeriodically(shimmer: Shimmer) {
    while (true) {
        val reading = collectMultiSensorData(shimmer)
        Thread.sleep(1000 / 128) // Blocks thread
    }
}
```

### 4. Monitor Data Quality

```kotlin
// Regularly assess data quality
val qualityAssessor = DataQualityAssessor()
val quality = qualityAssessor.assessDataQuality(shimmer)

when (quality) {
    DataQuality.EXCELLENT -> println("Excellent signal quality")
    DataQuality.GOOD -> println("Good signal quality")
    DataQuality.FAIR -> println("Fair signal quality - consider checking sensor placement")
    DataQuality.POOR -> println("Poor signal quality - check connections and sensor contact")
}
```

### 5. Implement Proper Resource Management

```kotlin
// Use try-finally or use blocks for resource management
try {
    shimmerRecorder.startRecording(sessionId)
    // ... recording operations ...
} finally {
    shimmerRecorder.stopRecording()
    shimmerRecorder.cleanup()
}
```

This comprehensive set of examples demonstrates how to effectively use the Shimmer Android API extension methods for various real-world scenarios, from basic sensor reading to advanced multi-device coordination and quality monitoring.