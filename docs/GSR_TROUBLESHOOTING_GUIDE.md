# GSR System Troubleshooting Guide

## Overview

This comprehensive troubleshooting guide covers common issues, diagnostic procedures, and solutions for the BucikaGSR ShimmerAndroidAPI integration.

## Common Issues and Solutions

### 1. Bluetooth Connection Issues

#### Issue: Device Not Found During Discovery
**Symptoms:**
- Shimmer device not appearing in discovery list
- "No devices found" message

**Diagnostic Steps:**
```kotlin
// Check Bluetooth adapter status
val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
if (bluetoothAdapter == null) {
    XLog.e("Bluetooth", "Device does not support Bluetooth")
    return false
}

if (!bluetoothAdapter.isEnabled) {
    XLog.w("Bluetooth", "Bluetooth is disabled")
    // Request user to enable Bluetooth
}

// Check permissions
val hasLocationPermission = ContextCompat.checkSelfPermission(
    context, Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED

if (!hasLocationPermission) {
    XLog.w("Permissions", "Location permission required for Bluetooth discovery")
}
```

**Solutions:**
1. **Enable Bluetooth**: Ensure Bluetooth is enabled on the device
2. **Grant Permissions**: Request and grant all required Bluetooth and location permissions
3. **Check Distance**: Ensure Shimmer device is within 10 meters
4. **Reset Bluetooth**: Turn Bluetooth off and on again
5. **Clear Bluetooth Cache**: Go to Settings → Apps → Bluetooth → Storage → Clear Cache

#### Issue: Connection Fails After Discovery
**Symptoms:**
- Device found but connection times out
- "Connection failed" errors

**Diagnostic Code:**
```kotlin
class BluetoothDiagnostics {
    fun diagnoseConnectionIssue(address: String): ConnectionDiagnostic {
        val diagnostic = ConnectionDiagnostic()
        
        // Check address format
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            diagnostic.addIssue("Invalid Bluetooth address format: $address")
        }
        
        // Check device pairing status
        val bondedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices
        val isDevicePaired = bondedDevices.any { it.address == address }
        diagnostic.devicePaired = isDevicePaired
        
        // Check connection state
        val connectionState = shimmerBluetooth.getConnectionState()
        diagnostic.currentState = connectionState
        
        return diagnostic
    }
}
```

**Solutions:**
1. **Verify Address Format**: Ensure Bluetooth address is valid (XX:XX:XX:XX:XX:XX)
2. **Clear Pairing**: Unpair and re-pair the device
3. **Restart Services**: Restart Bluetooth service
4. **Check Interference**: Move away from WiFi routers and other Bluetooth devices
5. **Battery Level**: Ensure Shimmer device has sufficient battery

### 2. Data Quality Issues

#### Issue: Poor Signal Quality
**Symptoms:**
- Signal quality consistently below 0.5
- Erratic GSR readings
- High noise in data

**Diagnostic Implementation:**
```kotlin
class DataQualityDiagnostics {
    fun analyzeSignalQuality(data: List<GSRDataPoint>): QualityReport {
        val report = QualityReport()
        
        // Calculate signal-to-noise ratio
        val mean = data.map { it.gsrValue }.average()
        val variance = data.map { (it.gsrValue - mean).pow(2) }.average()
        val snr = if (variance > 0) mean / sqrt(variance) else Double.MAX_VALUE
        
        report.signalToNoiseRatio = snr
        report.qualityScore = when {
            snr > 10 -> QualityLevel.EXCELLENT
            snr > 5 -> QualityLevel.GOOD
            snr > 2 -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.POOR
        }
        
        // Detect artifacts
        report.artifacts = detectArtifacts(data)
        
        return report
    }
    
    private fun detectArtifacts(data: List<GSRDataPoint>): List<Artifact> {
        val artifacts = mutableListOf<Artifact>()
        
        for (i in 1 until data.size) {
            val current = data[i].gsrValue
            val previous = data[i-1].gsrValue
            val change = abs(current - previous)
            
            // Detect sudden spikes (> 100 kΩ change)
            if (change > 100.0) {
                artifacts.add(Artifact(
                    type = ArtifactType.SPIKE,
                    timestamp = data[i].timestamp,
                    magnitude = change
                ))
            }
        }
        
        return artifacts
    }
}
```

**Solutions:**
1. **Check Electrode Contact**: Ensure proper skin contact with GSR electrodes
2. **Clean Electrodes**: Clean GSR electrodes with isopropyl alcohol
3. **Skin Preparation**: Clean and dry skin surface before application
4. **Reduce Movement**: Minimize hand/finger movement during measurement
5. **Check Environment**: Avoid humid or electrically noisy environments

#### Issue: Inconsistent Temperature Readings
**Symptoms:**
- Temperature values outside normal range (25-40°C)
- Rapid temperature fluctuations

**Diagnostic Code:**
```kotlin
fun validateTemperatureData(temperature: Double, timestamp: Long): ValidationResult {
    val result = ValidationResult()
    
    // Check physiological range
    if (temperature < 25.0 || temperature > 40.0) {
        result.addWarning("Temperature outside physiological range: ${temperature}°C")
    }
    
    // Check for rapid changes (if we have previous data)
    previousTemperature?.let { prev ->
        val timeDiff = (timestamp - previousTimestamp) / 1000.0 // seconds
        val tempChange = abs(temperature - prev)
        val changeRate = tempChange / timeDiff
        
        if (changeRate > 0.5) { // More than 0.5°C per second
            result.addWarning("Rapid temperature change detected: ${changeRate}°C/s")
        }
    }
    
    return result
}
```

**Solutions:**
1. **Stabilization Time**: Allow 5-10 minutes for temperature stabilization
2. **Ambient Conditions**: Control room temperature (20-25°C)
3. **Sensor Calibration**: Perform temperature sensor calibration
4. **Contact Quality**: Ensure consistent sensor-skin contact

### 3. File System and Data Storage Issues

#### Issue: Failed to Create Data Files
**Symptoms:**
- "Permission denied" errors
- Files not created in expected directory

**Diagnostic Implementation:**
```kotlin
class StorageDiagnostics {
    fun diagnoseStorageIssues(context: Context): StorageDiagnostic {
        val diagnostic = StorageDiagnostic()
        
        // Check storage permissions
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        diagnostic.hasWritePermission = hasWritePermission
        
        // Check available storage space
        val dataDir = File(context.filesDir, "gsr_data")
        val availableSpace = dataDir.usableSpace
        diagnostic.availableSpace = availableSpace
        
        // Check directory access
        try {
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            
            // Test file creation
            val testFile = File(dataDir, "test_${System.currentTimeMillis()}.tmp")
            testFile.createNewFile()
            testFile.delete()
            diagnostic.canCreateFiles = true
        } catch (e: IOException) {
            diagnostic.canCreateFiles = false
            diagnostic.error = e.message
        }
        
        return diagnostic
    }
}
```

**Solutions:**
1. **Grant Permissions**: Request and grant storage permissions
2. **Use App Storage**: Use app-specific directories (context.filesDir)
3. **Check Space**: Ensure sufficient storage space (>100MB recommended)
4. **Directory Creation**: Ensure data directories are created properly

#### Issue: Data Export Fails
**Symptoms:**
- Export operation hangs or crashes
- Corrupted export files

**Diagnostic Code:**
```kotlin
fun diagnoseExportIssue(sessionData: SessionData): ExportDiagnostic {
    val diagnostic = ExportDiagnostic()
    
    // Check data size
    val dataSize = sessionData.dataPoints.size
    diagnostic.dataPointCount = dataSize
    
    if (dataSize == 0) {
        diagnostic.addError("No data points to export")
        return diagnostic
    }
    
    // Estimate file size
    val estimatedSize = dataSize * 100 // ~100 bytes per data point
    diagnostic.estimatedFileSize = estimatedSize
    
    // Check for data consistency
    val timestamps = sessionData.dataPoints.map { it.timestamp }
    val isChronological = timestamps.zipWithNext().all { it.first <= it.second }
    diagnostic.isDataChronological = isChronological
    
    return diagnostic
}
```

**Solutions:**
1. **Check Data Integrity**: Validate data before export
2. **Batch Processing**: Export large datasets in smaller batches
3. **Background Processing**: Use background threads for export operations
4. **Error Handling**: Implement comprehensive error handling and retry logic

### 4. Performance Issues

#### Issue: High Memory Usage
**Symptoms:**
- App crashes with OutOfMemoryError
- Slow performance during data collection

**Memory Monitoring:**
```kotlin
class MemoryMonitor {
    fun monitorMemoryUsage(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val stats = MemoryStats()
        
        stats.totalMemory = runtime.totalMemory()
        stats.freeMemory = runtime.freeMemory()
        stats.usedMemory = stats.totalMemory - stats.freeMemory
        stats.maxMemory = runtime.maxMemory()
        
        // Calculate usage percentage
        stats.usagePercentage = (stats.usedMemory.toDouble() / stats.maxMemory.toDouble()) * 100
        
        // Warning if usage > 80%
        if (stats.usagePercentage > 80.0) {
            XLog.w("Memory", "High memory usage: ${stats.usagePercentage}%")
        }
        
        return stats
    }
    
    fun optimizeMemoryUsage() {
        // Clear data buffers
        dataBuffer.clear()
        
        // Trigger garbage collection
        System.gc()
        
        // Reduce buffer sizes if necessary
        if (getMemoryUsagePercentage() > 70.0) {
            reduceBufferSizes()
        }
    }
}
```

**Solutions:**
1. **Buffer Management**: Implement circular buffers with size limits
2. **Data Streaming**: Stream data to files instead of keeping in memory
3. **Garbage Collection**: Trigger periodic garbage collection
4. **Memory Profiling**: Use Android Studio memory profiler

#### Issue: Battery Drain
**Symptoms:**
- Rapid battery consumption
- Device heating during data collection

**Battery Optimization:**
```kotlin
class BatteryOptimization {
    fun optimizeForBatteryLife() {
        // Reduce sampling rate when on battery
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        if (batteryLevel < 20) {
            // Switch to power-saving mode
            gsrManager.setSamplingRate(64.0) // Reduce from 128 Hz
            gsrManager.disableNonEssentialSensors()
        }
        
        // Use wake lock judiciously
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GSR::DataCollection")
        
        wakeLock.acquire(30 * 60 * 1000) // 30 minutes max
    }
}
```

**Solutions:**
1. **Reduce Sampling Rate**: Lower sampling rate when battery is low
2. **Selective Sensors**: Disable non-essential sensors to save power
3. **Background Processing**: Optimize background operations
4. **Wake Lock Management**: Use wake locks judiciously

### 5. UI and User Experience Issues

#### Issue: UI Freezing During Data Collection
**Symptoms:**
- UI becomes unresponsive
- ANR (Application Not Responding) errors

**Solution Implementation:**
```kotlin
class DataCollectionManager {
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val uiHandler = Handler(Looper.getMainLooper())
    
    fun processDataInBackground(dataPoint: GSRDataPoint) {
        backgroundExecutor.execute {
            // Heavy processing on background thread
            val processedData = processGSRData(dataPoint)
            
            // Update UI on main thread
            uiHandler.post {
                updateVisualization(processedData)
            }
        }
    }
    
    fun cleanup() {
        backgroundExecutor.shutdown()
    }
}
```

**Solutions:**
1. **Background Threading**: Move heavy operations to background threads
2. **UI Updates**: Only update UI on main thread
3. **Data Throttling**: Limit UI update frequency (e.g., 30 FPS)
4. **Progress Indicators**: Show progress for long-running operations

## Diagnostic Tools

### 1. Built-in Diagnostics

```kotlin
class GSRSystemDiagnostics {
    fun runComprehensiveDiagnostic(): SystemDiagnostic {
        val diagnostic = SystemDiagnostic()
        
        // Bluetooth diagnostics
        diagnostic.bluetoothStatus = diagnoseBluetooth()
        
        // Storage diagnostics
        diagnostic.storageStatus = diagnoseStorage()
        
        // Memory diagnostics
        diagnostic.memoryStatus = diagnoseMemory()
        
        // Sensor diagnostics
        diagnostic.sensorStatus = diagnoseSensors()
        
        return diagnostic
    }
    
    fun generateDiagnosticReport(): String {
        val diagnostic = runComprehensiveDiagnostic()
        return buildString {
            appendLine("=== GSR System Diagnostic Report ===")
            appendLine("Timestamp: ${Date()}")
            appendLine()
            appendLine("Bluetooth Status: ${diagnostic.bluetoothStatus}")
            appendLine("Storage Status: ${diagnostic.storageStatus}")
            appendLine("Memory Status: ${diagnostic.memoryStatus}")
            appendLine("Sensor Status: ${diagnostic.sensorStatus}")
        }
    }
}
```

### 2. Log Analysis Tools

```kotlin
class LogAnalyzer {
    fun analyzeRecentLogs(): LogAnalysis {
        val logFile = File(context.filesDir, "logs/gsr_log.txt")
        val analysis = LogAnalysis()
        
        logFile.readLines().takeLast(1000).forEach { line ->
            when {
                line.contains("ERROR") -> analysis.errorCount++
                line.contains("WARN") -> analysis.warningCount++
                line.contains("Connection failed") -> analysis.connectionIssues++
                line.contains("Poor signal quality") -> analysis.qualityIssues++
            }
        }
        
        return analysis
    }
}
```

## Support Resources

### 1. Debug Information Collection

When reporting issues, collect the following information:

```kotlin
fun collectDebugInfo(): DebugInfo {
    return DebugInfo(
        appVersion = BuildConfig.VERSION_NAME,
        buildType = BuildConfig.BUILD_TYPE,
        deviceModel = Build.MODEL,
        androidVersion = Build.VERSION.RELEASE,
        availableMemory = getAvailableMemory(),
        bluetoothVersion = getBluetoothVersion(),
        recentLogs = getRecentLogs(),
        systemDiagnostic = runComprehensiveDiagnostic()
    )
}
```

### 2. Contact Information

- **Technical Support**: development-team@example.com
- **Bug Reports**: Submit through project issue tracker
- **Documentation**: Check docs/ directory for latest guides
- **Community Support**: Developer forum or chat channel

### 3. Known Limitations

1. **Android Version Compatibility**: Requires Android 8.0+ for optimal performance
2. **Bluetooth Range**: Effective range is 5-10 meters in typical environments
3. **Sampling Rate Limits**: Maximum sustainable rate depends on device performance
4. **File Size Limits**: Large datasets (>100MB) may require special handling

This troubleshooting guide provides comprehensive solutions for the most common issues encountered with the BucikaGSR ShimmerAndroidAPI integration. For issues not covered here, please contact technical support with detailed diagnostic information.