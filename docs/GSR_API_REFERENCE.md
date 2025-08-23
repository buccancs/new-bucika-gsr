# GSR API Reference

## Overview

Complete API reference for the BucikaGSR ShimmerAndroidAPI integration. This document provides detailed information about all public classes, methods, and interfaces available for GSR data collection and analysis.

## Core Classes

### GSRManager

**Package**: `com.topdon.tc001.gsr`

The central coordinator for GSR data collection and device management.

#### Constructor
```kotlin
private constructor(context: Context)
```
*Private constructor - use `getInstance()` to obtain instance*

#### Static Methods

##### getInstance
```kotlin
fun getInstance(context: Context): GSRManager
```
**Description**: Returns singleton instance of GSRManager  
**Parameters**:
- `context: Context` - Application context
**Returns**: `GSRManager` instance  
**Thread Safety**: Thread-safe singleton implementation

#### Connection Methods

##### connectToDevice
```kotlin
fun connectToDevice(bluetoothAddress: String, deviceName: String): Boolean
```
**Description**: Establishes connection to Shimmer GSR device  
**Parameters**:
- `bluetoothAddress: String` - Bluetooth MAC address (format: XX:XX:XX:XX:XX:XX)
- `deviceName: String` - Human-readable device name
**Returns**: `Boolean` - true if connection initiated successfully  
**Throws**: `SecurityException` if Bluetooth permissions not granted

##### disconnectDevice
```kotlin
fun disconnectDevice()
```
**Description**: Disconnects from currently connected Shimmer device  
**Side Effects**: Stops data streaming if active

##### isConnected
```kotlin
fun isConnected(): Boolean
```
**Description**: Checks current connection status  
**Returns**: `Boolean` - true if device is connected

##### getConnectedDeviceName
```kotlin
fun getConnectedDeviceName(): String?
```
**Description**: Gets name of currently connected device  
**Returns**: `String?` - device name or null if not connected

#### Data Collection Methods

##### startRecording
```kotlin
fun startRecording(): Boolean
```
**Description**: Starts GSR data recording  
**Returns**: `Boolean` - true if recording started successfully  
**Prerequisites**: Device must be connected

##### stopRecording
```kotlin
fun stopRecording()
```
**Description**: Stops GSR data recording  

##### isRecording
```kotlin
fun isRecording(): Boolean
```
**Description**: Checks current recording status  
**Returns**: `Boolean` - true if currently recording

#### Listener Methods

##### setGSRDataListener
```kotlin
fun setGSRDataListener(listener: GSRDataListener?)
```
**Description**: Sets basic GSR data listener  
**Parameters**:
- `listener: GSRDataListener?` - Listener for basic GSR data or null to remove

##### setAdvancedDataListener
```kotlin
fun setAdvancedDataListener(listener: AdvancedGSRDataListener?)
```
**Description**: Sets advanced GSR data listener  
**Parameters**:
- `listener: AdvancedGSRDataListener?` - Listener for advanced analytics or null to remove

#### Configuration Methods

##### applyConfiguration
```kotlin
fun applyConfiguration(config: Configuration): Boolean
```
**Description**: Applies sensor configuration to connected device  
**Parameters**:
- `config: Configuration` - Shimmer configuration object
**Returns**: `Boolean` - true if configuration applied successfully

##### getCurrentConfiguration
```kotlin
fun getCurrentConfiguration(): Configuration
```
**Description**: Gets current device configuration  
**Returns**: `Configuration` - current configuration settings

#### Utility Methods

##### assessDataQuality
```kotlin
fun assessDataQuality(gsrValue: Double, temperature: Double): Double
```
**Description**: Assesses quality of GSR data  
**Parameters**:
- `gsrValue: Double` - GSR value in kΩ
- `temperature: Double` - Temperature in °C
**Returns**: `Double` - quality score (0.0-1.0, higher is better)

##### hasRequiredPermissions
```kotlin
fun hasRequiredPermissions(): Boolean
```
**Description**: Checks if all required permissions are granted  
**Returns**: `Boolean` - true if all permissions available

##### cleanup
```kotlin
fun cleanup()
```
**Description**: Releases all resources and stops operations  
**Usage**: Call in Activity.onDestroy() or similar lifecycle methods

#### Inner Interfaces

##### GSRDataListener
```kotlin
interface GSRDataListener {
    fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double)
}
```
**Description**: Basic GSR data callback interface  
**Parameters**:
- `timestamp: Long` - Unix timestamp in milliseconds
- `gsrValue: Double` - GSR value in kΩ
- `skinTemperature: Double` - Skin temperature in °C

##### AdvancedGSRDataListener
```kotlin
interface AdvancedGSRDataListener {
    fun onAdvancedGSRData(
        timestamp: Long,
        gsrValue: Double,
        skinTemperature: Double,
        arousalState: String,
        stressIndicators: Map<String, Double>,
        signalQuality: Double
    )
}
```
**Description**: Advanced GSR data callback with analytics  
**Parameters**:
- `timestamp: Long` - Unix timestamp in milliseconds
- `gsrValue: Double` - GSR value in kΩ
- `skinTemperature: Double` - Skin temperature in °C
- `arousalState: String` - Arousal level ("LOW", "MEDIUM", "HIGH")
- `stressIndicators: Map<String, Double>` - Stress analysis metrics
- `signalQuality: Double` - Signal quality score (0.0-1.0)

---

### ShimmerSensorPanel

**Package**: `com.topdon.tc001.gsr.ui`

Comprehensive UI configuration panel for Shimmer sensor settings.

#### Constructor
```kotlin
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
```
**Description**: Creates new sensor configuration panel  
**Parameters**:
- `context: Context` - UI context
- `attrs: AttributeSet?` - XML attributes (optional)
- `defStyleAttr: Int` - Default style (optional)

#### Configuration Methods

##### setSamplingRate
```kotlin
fun setSamplingRate(rate: Int)
```
**Description**: Sets data sampling rate  
**Parameters**:
- `rate: Int` - Sampling rate in Hz (1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024)
**Throws**: `IllegalArgumentException` if rate is not supported

##### getSamplingRate
```kotlin
fun getSamplingRate(): Int
```
**Description**: Gets current sampling rate  
**Returns**: `Int` - current sampling rate in Hz

##### setSensorEnabled
```kotlin
fun setSensorEnabled(sensorType: SensorType, enabled: Boolean)
```
**Description**: Enables or disables specific sensor  
**Parameters**:
- `sensorType: SensorType` - Type of sensor to configure
- `enabled: Boolean` - true to enable, false to disable

##### isSensorEnabled
```kotlin
fun isSensorEnabled(sensorType: SensorType): Boolean
```
**Description**: Checks if sensor is enabled  
**Parameters**:
- `sensorType: SensorType` - Type of sensor to check
**Returns**: `Boolean` - true if sensor is enabled

##### setGSRRange
```kotlin
fun setGSRRange(range: GSRRange)
```
**Description**: Sets GSR measurement range  
**Parameters**:
- `range: GSRRange` - GSR sensitivity range

##### getGSRRange
```kotlin
fun getGSRRange(): GSRRange
```
**Description**: Gets current GSR range  
**Returns**: `GSRRange` - current GSR sensitivity range

#### Settings Methods

##### generateConfiguration
```kotlin
fun generateConfiguration(): Configuration
```
**Description**: Generates Shimmer configuration from current settings  
**Returns**: `Configuration` - complete device configuration

##### validateConfiguration
```kotlin
fun validateConfiguration(config: Configuration): Boolean
```
**Description**: Validates configuration parameters  
**Parameters**:
- `config: Configuration` - Configuration to validate
**Returns**: `Boolean` - true if configuration is valid

##### saveSensorSettings
```kotlin
fun saveSensorSettings()
```
**Description**: Saves current settings to SharedPreferences  

##### loadSavedSettings
```kotlin
fun loadSavedSettings()
```
**Description**: Loads settings from SharedPreferences  

##### resetToDefaults
```kotlin
fun resetToDefaults()
```
**Description**: Resets all settings to default values  

#### Validation Methods

##### validateCurrentSettings
```kotlin
fun validateCurrentSettings(): List<String>
```
**Description**: Validates current settings and returns errors  
**Returns**: `List<String>` - List of validation error messages (empty if valid)

##### generateSensorBitmap
```kotlin
fun generateSensorBitmap(): Int
```
**Description**: Generates sensor bitmap for Shimmer device  
**Returns**: `Int` - Sensor enable bitmap

#### Listener Interface

##### ShimmerConfigurationListener
```kotlin
interface ShimmerConfigurationListener {
    fun onConfigurationApplied(config: ShimmerConfiguration)
}
```
**Description**: Callback for configuration changes  

##### setConfigurationListener
```kotlin
fun setConfigurationListener(listener: ShimmerConfigurationListener?)
```
**Description**: Sets configuration change listener  
**Parameters**:
- `listener: ShimmerConfigurationListener?` - Listener or null to remove

#### Enums

##### SensorType
```kotlin
enum class SensorType {
    GSR,
    TEMPERATURE, 
    PPG,
    ACCELEROMETER
}
```

##### GSRRange
```kotlin
enum class GSRRange {
    RANGE_40K_OHM,      // High sensitivity
    RANGE_287K_OHM,     // Standard range
    RANGE_1M_OHM,       // Medium sensitivity  
    RANGE_3_3M_OHM      // Low sensitivity
}
```

---

### GSRDataWriter

**Package**: `com.topdon.tc001.gsr.data`

Complete file system integration service for GSR data recording and export.

#### Static Methods

##### getInstance
```kotlin
fun getInstance(context: Context): GSRDataWriter
```
**Description**: Returns singleton instance  
**Parameters**:
- `context: Context` - Application context
**Returns**: `GSRDataWriter` instance

#### File Creation Methods

##### createCSVFile
```kotlin
fun createCSVFile(sessionName: String, timestamp: Long): String
```
**Description**: Creates new CSV file for data recording  
**Parameters**:
- `sessionName: String` - Session identifier
- `timestamp: Long` - Session start timestamp
**Returns**: `String` - Generated filename
**Creates**: CSV file with appropriate headers

##### createJSONFile
```kotlin
fun createJSONFile(sessionName: String, timestamp: Long): String
```
**Description**: Creates new JSON file for structured data  
**Parameters**:
- `sessionName: String` - Session identifier  
- `timestamp: Long` - Session start timestamp
**Returns**: `String` - Generated filename

#### Data Writing Methods

##### writeGSRDataPoint
```kotlin
fun writeGSRDataPoint(filename: String, dataPoint: GSRDataPoint): Boolean
```
**Description**: Writes single GSR data point to file  
**Parameters**:
- `filename: String` - Target filename
- `dataPoint: GSRDataPoint` - Data to write
**Returns**: `Boolean` - true if write successful

##### writeBatchGSRData
```kotlin
fun writeBatchGSRData(filename: String, dataPoints: List<GSRDataPoint>): Boolean
```
**Description**: Writes multiple data points efficiently  
**Parameters**:
- `filename: String` - Target filename
- `dataPoints: List<GSRDataPoint>` - Data points to write
**Returns**: `Boolean` - true if write successful

#### Real-time Methods

##### startRealTimeRecording
```kotlin
fun startRealTimeRecording(filename: String)
```
**Description**: Starts background real-time data recording  
**Parameters**:
- `filename: String` - Target filename for recording

##### stopRealTimeRecording
```kotlin
fun stopRealTimeRecording()
```
**Description**: Stops real-time recording and flushes buffers  

##### queueDataPoint
```kotlin
fun queueDataPoint(dataPoint: GSRDataPoint)
```
**Description**: Queues data point for background writing  
**Parameters**:
- `dataPoint: GSRDataPoint` - Data to queue
**Thread Safety**: Thread-safe queuing mechanism

#### Export Methods

##### exportGSRDataToFile
```kotlin
fun exportGSRDataToFile(sessionData: SessionData, includeAnalysis: Boolean): String?
```
**Description**: Exports complete session data with optional analysis  
**Parameters**:
- `sessionData: SessionData` - Complete session data
- `includeAnalysis: Boolean` - Include statistical analysis in export
**Returns**: `String?` - Path to exported file or null if failed

#### File Management Methods

##### getDataDirectorySize
```kotlin
fun getDataDirectorySize(): Long
```
**Description**: Calculates total size of data directory  
**Returns**: `Long` - Size in bytes

##### getFileCount
```kotlin
fun getFileCount(): Int
```
**Description**: Counts files in data directory  
**Returns**: `Int` - Number of data files

##### cleanupOldFiles
```kotlin
fun cleanupOldFiles(retentionDays: Int): Int
```
**Description**: Removes files older than retention period  
**Parameters**:
- `retentionDays: Int` - Files older than this are removed
**Returns**: `Int` - Number of files deleted

##### validateDataIntegrity
```kotlin
fun validateDataIntegrity(filename: String): Boolean
```
**Description**: Validates integrity of data file  
**Parameters**:
- `filename: String` - File to validate
**Returns**: `Boolean` - true if file is valid

##### compressDataFile
```kotlin
fun compressDataFile(filename: String): String?
```
**Description**: Compresses data file for archival  
**Parameters**:
- `filename: String` - File to compress
**Returns**: `String?` - Path to compressed file or null if failed

#### Data Classes

##### GSRDataPoint
```kotlin
data class GSRDataPoint(
    val timestamp: Long,
    val gsrValue: Double,
    val skinTemperature: Double,
    val signalQuality: Double,
    val batteryLevel: Int
)
```
**Description**: Single GSR measurement data point

##### SessionData
```kotlin
data class SessionData(
    val sessionName: String,
    val startTime: Long,
    val endTime: Long,
    val dataPoints: List<GSRDataPoint>,
    val participantId: String,
    val notes: String
)
```
**Description**: Complete recording session data

---

### ShimmerBluetooth

**Package**: `com.shimmerresearch.android`

Enhanced Bluetooth connectivity implementation for Shimmer devices.

#### Constructor
```kotlin
constructor(context: Context, deviceName: String)
```
**Description**: Creates Shimmer Bluetooth interface  
**Parameters**:
- `context: Context` - Application context
- `deviceName: String` - Device identifier

#### Connection Methods

##### connect
```kotlin
fun connect(bluetoothAddress: String, deviceName: String): Boolean
```
**Description**: Connects to Shimmer device  
**Parameters**:
- `bluetoothAddress: String` - Device Bluetooth address
- `deviceName: String` - Device name
**Returns**: `Boolean` - true if connection initiated

##### disconnect
```kotlin
fun disconnect()
```
**Description**: Disconnects from device  

##### isConnected
```kotlin
fun isConnected(): Boolean
```
**Description**: Checks connection status  
**Returns**: `Boolean` - connection state

#### Streaming Methods

##### startStreaming
```kotlin
fun startStreaming(): Boolean
```
**Description**: Starts data streaming  
**Returns**: `Boolean` - true if streaming started

##### stopStreaming
```kotlin
fun stopStreaming()
```
**Description**: Stops data streaming  

##### isStreaming
```kotlin
fun isStreaming(): Boolean
```
**Description**: Checks streaming status  
**Returns**: `Boolean` - streaming state

#### Configuration Methods

##### writeConfiguration
```kotlin
fun writeConfiguration(config: Configuration): Boolean
```
**Description**: Applies configuration to device  
**Parameters**:
- `config: Configuration` - Device configuration
**Returns**: `Boolean` - true if successful

##### setSamplingRate
```kotlin
fun setSamplingRate(rate: Double): Boolean
```
**Description**: Sets device sampling rate  
**Parameters**:
- `rate: Double` - Sampling rate in Hz
**Returns**: `Boolean` - true if successful

##### enableSensor
```kotlin
fun enableSensor(sensorId: Int): Boolean
```
**Description**: Enables specific sensor  
**Parameters**:
- `sensorId: Int` - Sensor identifier
**Returns**: `Boolean` - true if successful

##### disableSensor  
```kotlin
fun disableSensor(sensorId: Int): Boolean
```
**Description**: Disables specific sensor  
**Parameters**:
- `sensorId: Int` - Sensor identifier  
**Returns**: `Boolean` - true if successful

#### Data Generation Methods

##### generateRealisticGSRData
```kotlin
fun generateRealisticGSRData(): GSRDataPoint
```
**Description**: Generates physiologically realistic GSR data for simulation  
**Returns**: `GSRDataPoint` - Simulated data point

#### Device Information Methods

##### readBatteryLevel
```kotlin
fun readBatteryLevel(): Int
```
**Description**: Reads device battery level  
**Returns**: `Int` - Battery percentage (0-100)

##### getDeviceInfo
```kotlin
fun getDeviceInfo(): DeviceInfo
```
**Description**: Gets device information  
**Returns**: `DeviceInfo` - Device details

##### getFirmwareVersion
```kotlin
fun getFirmwareVersion(): String
```
**Description**: Gets device firmware version  
**Returns**: `String` - Firmware version string

#### Utility Methods

##### isDeviceCompatible
```kotlin
fun isDeviceCompatible(deviceName: String): Boolean
```
**Description**: Checks device compatibility  
**Parameters**:
- `deviceName: String` - Device name to check
**Returns**: `Boolean` - true if compatible

##### validateDataPacket
```kotlin
fun validateDataPacket(packet: ByteArray): Boolean
```
**Description**: Validates received data packet  
**Parameters**:
- `packet: ByteArray` - Raw data packet
**Returns**: `Boolean` - true if valid

##### checkBluetoothPermissions
```kotlin
fun checkBluetoothPermissions(): Boolean
```
**Description**: Checks Bluetooth permissions  
**Returns**: `Boolean` - true if permissions granted

##### cleanup
```kotlin
fun cleanup()
```
**Description**: Releases Bluetooth resources  

#### Constants

```kotlin
companion object {
    const val SENSOR_GSR = 0x04
    const val SENSOR_TEMPERATURE = 0x80
    const val SENSOR_PPG = 0x40
    const val SENSOR_ACCELEROMETER = 0x80
}
```

## Usage Examples

### Basic GSR Data Collection

```kotlin
// Initialize GSR manager
val gsrManager = GSRManager.getInstance(context)

// Set up data listener
gsrManager.setGSRDataListener(object : GSRManager.GSRDataListener {
    override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
        // Handle GSR data
        Log.d("GSR", "GSR: ${gsrValue}kΩ, Temp: ${skinTemperature}°C")
    }
})

// Connect and start recording
gsrManager.connectToDevice("00:11:22:33:44:55", "Shimmer3-GSR+")
gsrManager.startRecording()
```

### Advanced Configuration

```kotlin
// Configure sensor panel
val sensorPanel = ShimmerSensorPanel(context)
sensorPanel.setSamplingRate(128)
sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.GSR, true)
sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.TEMPERATURE, true)
sensorPanel.setGSRRange(ShimmerSensorPanel.GSRRange.RANGE_287K_OHM)

// Apply configuration
val config = sensorPanel.generateConfiguration()
gsrManager.applyConfiguration(config)
```

### Data Recording and Export

```kotlin
// Set up data writer
val dataWriter = GSRDataWriter.getInstance(context)
val filename = dataWriter.createCSVFile("session_001", System.currentTimeMillis())

// Start real-time recording
dataWriter.startRealTimeRecording(filename)

// Queue data points
gsrManager.setGSRDataListener { timestamp, gsrValue, temperature ->
    dataWriter.queueDataPoint(GSRDataPoint(timestamp, gsrValue, temperature, 0.85, 80))
}

// Export complete session
val sessionData = SessionData(/* session parameters */)
val exportPath = dataWriter.exportGSRDataToFile(sessionData, includeAnalysis = true)
```

This API reference provides complete documentation for integrating GSR functionality into Android applications using the BucikaGSR ShimmerAndroidAPI.