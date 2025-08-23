# ShimmerAndroidAPI Integration - Comprehensive Documentation

## Overview

The BucikaGSR system provides professional-grade GSR (Galvanic Skin Response) data collection and analysis through comprehensive ShimmerAndroidAPI integration. This documentation covers the complete architecture, APIs, and usage patterns for the enhanced GSR functionality.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [API Reference](#api-reference)
4. [Integration Guide](#integration-guide)
5. [Data Formats](#data-formats)
6. [Configuration Reference](#configuration-reference)
7. [Troubleshooting](#troubleshooting)
8. [Examples](#examples)

## Architecture Overview

The GSR system is built on a modular architecture that provides:

- **Professional Data Collection**: 128 Hz sampling with sub-millisecond precision
- **Complete Settings Management**: Full UI control over Shimmer sensor parameters
- **Real-time Data Writing**: Background CSV export with data queuing
- **Advanced Signal Processing**: Digital filtering and artifact detection
- **Multi-Modal Synchronization**: Concurrent operation with thermal IR imaging

### Component Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   GSRActivity   │    │ ShimmerSensorPanel│    │ GSRSettingsActivity│
│                 │    │                  │    │                 │
└─────────┬───────┘    └─────────┬────────┘    └─────────┬───────┘
          │                      │                       │
          │              ┌───────▼────────┐              │
          └──────────────►│   GSRManager   │◄─────────────┘
                         │                │
                         └───────┬────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────▼────────┐    ┌──────────▼──────────┐    ┌────────▼──────┐
│ShimmerBluetooth│    │ ShimmerDataProcessor│    │ GSRDataWriter │
│                │    │                    │    │               │
└────────────────┘    └─────────────────────┘    └───────────────┘
```

## Core Components

### GSRManager

The central coordinator for GSR data collection and device management.

**Key Features:**
- Singleton pattern for system-wide access
- Dual listener support (basic and advanced)
- Device connection management
- Configuration application
- Data quality assessment

**Usage:**
```kotlin
val gsrManager = GSRManager.getInstance(context)
gsrManager.setGSRDataListener(object : GSRManager.GSRDataListener {
    override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
        // Handle basic GSR data
    }
})
```

### ShimmerSensorPanel

Comprehensive UI configuration panel with Settings API connectivity.

**Key Features:**
- Sampling rate selection (1 Hz to 1024 Hz)
- Sensor enable/disable controls
- GSR sensitivity ranges (40kΩ to 1MΩ)
- Digital filtering options
- Real-time configuration preview
- SharedPreferences persistence

**Usage:**
```kotlin
val sensorPanel = ShimmerSensorPanel(context)
sensorPanel.setConfigurationListener(object : ShimmerSensorPanel.ShimmerConfigurationListener {
    override fun onConfigurationApplied(config: ShimmerConfiguration) {
        // Configuration automatically applied to connected device
    }
})
```

### GSRDataWriter

Complete file system integration service for data recording and export.

**Key Features:**
- Real-time CSV recording with background queuing
- Comprehensive data export with analysis
- File management utilities
- Compression and cleanup capabilities
- Multiple format support (CSV, JSON)

**Usage:**
```kotlin
val dataWriter = GSRDataWriter.getInstance(context)
val exportPath = dataWriter.exportGSRDataToFile(gsrData, includeAnalysis = true)
```

### ShimmerBluetooth

Enhanced Bluetooth connectivity with realistic data generation.

**Key Features:**
- Professional Shimmer device simulation
- 128 Hz real-time data streaming
- Physiologically accurate GSR data generation
- Battery level monitoring
- Device discovery and pairing

## API Reference

### GSRManager API

#### Connection Management
```kotlin
// Connect to Shimmer device
fun connectToDevice(bluetoothAddress: String, deviceName: String): Boolean

// Disconnect from device
fun disconnectDevice()

// Check connection status
fun isConnected(): Boolean

// Get connected device name
fun getConnectedDeviceName(): String?
```

#### Data Collection
```kotlin
// Start/stop recording
fun startRecording(): Boolean
fun stopRecording()
fun isRecording(): Boolean

// Data listeners
fun setGSRDataListener(listener: GSRDataListener?)
fun setAdvancedDataListener(listener: AdvancedGSRDataListener?)
```

#### Configuration Management
```kotlin
// Apply configuration
fun applyConfiguration(config: Configuration): Boolean

// Get current configuration
fun getCurrentConfiguration(): Configuration

// Data quality assessment
fun assessDataQuality(gsrValue: Double, temperature: Double): Double
```

### ShimmerSensorPanel API

#### Sensor Configuration
```kotlin
// Sampling rate configuration
fun setSamplingRate(rate: Int)
fun getSamplingRate(): Int

// Sensor enable/disable
fun setSensorEnabled(sensorType: SensorType, enabled: Boolean)
fun isSensorEnabled(sensorType: SensorType): Boolean

// GSR range configuration
fun setGSRRange(range: GSRRange)
fun getGSRRange(): GSRRange
```

#### Settings Management
```kotlin
// Generate configuration
fun generateConfiguration(): Configuration

// Validate configuration
fun validateConfiguration(config: Configuration): Boolean

// Settings persistence
fun saveSensorSettings()
fun loadSavedSettings()
fun resetToDefaults()
```

### GSRDataWriter API

#### File Operations
```kotlin
// Create files
fun createCSVFile(sessionName: String, timestamp: Long): String
fun createJSONFile(sessionName: String, timestamp: Long): String

// Write data
fun writeGSRDataPoint(filename: String, dataPoint: GSRDataPoint): Boolean
fun writeBatchGSRData(filename: String, dataPoints: List<GSRDataPoint>): Boolean
```

#### Real-time Recording
```kotlin
// Real-time operations
fun startRealTimeRecording(filename: String)
fun stopRealTimeRecording()
fun queueDataPoint(dataPoint: GSRDataPoint)
```

#### Data Management
```kotlin
// Export and analysis
fun exportGSRDataToFile(sessionData: SessionData, includeAnalysis: Boolean): String?

// File management
fun getDataDirectorySize(): Long
fun getFileCount(): Int
fun cleanupOldFiles(retentionDays: Int): Int
```

## Integration Guide

### Basic Setup

1. **Initialize Components:**
```kotlin
class GSRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize GSR system
        val gsrManager = GSRManager.getInstance(this)
        val dataWriter = GSRDataWriter.getInstance(this)
    }
}
```

2. **Activity Integration:**
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var gsrManager: GSRManager
    private lateinit var sensorPanel: ShimmerSensorPanel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        gsrManager = GSRManager.getInstance(this)
        sensorPanel = ShimmerSensorPanel(this)
        
        setupGSRDataListener()
        setupSensorConfiguration()
    }
    
    private fun setupGSRDataListener() {
        gsrManager.setGSRDataListener(object : GSRManager.GSRDataListener {
            override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
                updateUI(gsrValue, skinTemperature)
            }
        })
    }
}
```

### Advanced Configuration

1. **Custom Sensor Settings:**
```kotlin
// Configure for stress analysis
sensorPanel.setSamplingRate(128)
sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.GSR, true)
sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.TEMPERATURE, true)
sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.PPG, true)
sensorPanel.setGSRRange(ShimmerSensorPanel.GSRRange.RANGE_287K_OHM)
sensorPanel.setFilterEnabled(true)
```

2. **Real-time Data Processing:**
```kotlin
gsrManager.setAdvancedDataListener(object : GSRManager.AdvancedGSRDataListener {
    override fun onAdvancedGSRData(
        timestamp: Long,
        gsrValue: Double,
        skinTemperature: Double,
        arousalState: String,
        stressIndicators: Map<String, Double>,
        signalQuality: Double
    ) {
        // Process advanced analytics
        handleStressAnalysis(arousalState, stressIndicators)
        updateVisualization(gsrValue, signalQuality)
    }
})
```

## Data Formats

### CSV Format
```csv
Timestamp,GSR_Value,Skin_Temperature,Signal_Quality,Battery_Level
1640995200000,425.7,32.4,0.88,85
1640995200008,427.1,32.4,0.89,85
1640995200016,423.8,32.3,0.87,85
```

### JSON Export Format
```json
{
  "session_info": {
    "session_name": "stress_analysis_001",
    "start_time": 1640995200000,
    "end_time": 1640995500000,
    "participant_id": "PARTICIPANT_001",
    "sampling_rate": 128.0
  },
  "data_points": [
    {
      "timestamp": 1640995200000,
      "gsr_value": 425.7,
      "skin_temperature": 32.4,
      "signal_quality": 0.88,
      "battery_level": 85
    }
  ],
  "analysis_summary": {
    "mean_gsr": 428.5,
    "gsr_variability": 0.42,
    "arousal_periods": 3,
    "stress_indicators": {
      "high_arousal_percentage": 15.2,
      "temperature_stability": 0.91
    }
  }
}
```

## Configuration Reference

### Sampling Rates
- **1 Hz**: Ultra-low power mode
- **10.24 Hz**: Basic monitoring
- **51.2 Hz**: Standard physiological monitoring
- **128 Hz**: Professional data collection (recommended)
- **256 Hz**: High-precision research
- **512 Hz**: Advanced research applications
- **1024 Hz**: Maximum precision (high power consumption)

### GSR Ranges
- **40kΩ**: High sensitivity, dry skin conditions
- **287kΩ**: Standard range, normal skin conditions
- **1MΩ**: Medium sensitivity, moist skin conditions
- **3.3MΩ**: Low sensitivity, very moist conditions

### Sensor Combinations
```kotlin
// Basic GSR monitoring
config.setSensorEnabledState(0x04, true)  // GSR
config.setSensorEnabledState(0x80, true)  // Temperature

// Advanced physiological monitoring
config.setSensorEnabledState(0x04, true)  // GSR
config.setSensorEnabledState(0x80, true)  // Temperature
config.setSensorEnabledState(0x40, true)  // PPG
config.setSensorEnabledState(0x80, true)  // Accelerometer
```

## Troubleshooting

### Common Issues

1. **Bluetooth Connection Failures:**
```kotlin
// Check Bluetooth permissions
if (!gsrManager.hasRequiredPermissions()) {
    requestBluetoothPermissions()
}

// Verify device compatibility
if (!shimmerBluetooth.isDeviceCompatible(deviceName)) {
    showIncompatibilityError()
}
```

2. **Data Quality Issues:**
```kotlin
// Monitor signal quality
val quality = gsrManager.assessDataQuality(gsrValue, temperature)
if (quality < 0.5) {
    showDataQualityWarning()
}
```

3. **File Writing Problems:**
```kotlin
// Check storage permissions and space
val available = dataWriter.getAvailableStorage()
if (available < MIN_REQUIRED_SPACE) {
    requestStorageCleanup()
}
```

### Performance Optimization

1. **Battery Life:**
- Use appropriate sampling rates
- Disable unused sensors
- Implement data batching

2. **Memory Management:**
- Monitor data queue sizes
- Implement periodic cleanup
- Use background processing

3. **File System:**
- Regular cleanup of old files
- Data compression for archival
- Efficient CSV writing

## Examples

### Complete Stress Analysis Session
```kotlin
class StressAnalysisSession {
    private lateinit var gsrManager: GSRManager
    private lateinit var dataWriter: GSRDataWriter
    private lateinit var sessionFilename: String
    
    fun startSession(participantId: String) {
        // Configure for stress analysis
        val sensorPanel = ShimmerSensorPanel(context)
        sensorPanel.setSamplingRate(128)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.GSR, true)
        sensorPanel.setSensorEnabled(ShimmerSensorPanel.SensorType.TEMPERATURE, true)
        
        // Apply configuration
        val config = sensorPanel.generateConfiguration()
        gsrManager.applyConfiguration(config)
        
        // Setup data recording
        sessionFilename = dataWriter.createCSVFile("stress_${participantId}", System.currentTimeMillis())
        dataWriter.startRealTimeRecording(sessionFilename)
        
        // Setup data listener
        gsrManager.setAdvancedDataListener(object : GSRManager.AdvancedGSRDataListener {
            override fun onAdvancedGSRData(
                timestamp: Long,
                gsrValue: Double,
                skinTemperature: Double,
                arousalState: String,
                stressIndicators: Map<String, Double>,
                signalQuality: Double
            ) {
                // Queue data for file writing
                dataWriter.queueDataPoint(createDataPoint(timestamp, gsrValue, skinTemperature))
                
                // Real-time analysis
                analyzeStressLevel(arousalState, stressIndicators)
                updateVisualization(gsrValue, signalQuality)
            }
        })
        
        // Start data collection
        gsrManager.connectToDevice(SHIMMER_ADDRESS, "StressAnalyzer")
        gsrManager.startRecording()
    }
    
    fun stopSession(): String? {
        gsrManager.stopRecording()
        dataWriter.stopRealTimeRecording()
        
        // Export complete session data
        val sessionData = createSessionData()
        return dataWriter.exportGSRDataToFile(sessionData, includeAnalysis = true)
    }
}
```

### Custom Data Visualization
```kotlin
class GSRVisualizationManager {
    private val dataBuffer = CircularBuffer<GSRDataPoint>(1000)
    
    fun updateVisualization(gsrValue: Double, temperature: Double, quality: Double) {
        val dataPoint = GSRDataPoint(System.currentTimeMillis(), gsrValue, temperature, quality)
        dataBuffer.add(dataPoint)
        
        // Update real-time chart
        updateGSRChart(dataBuffer.getLastN(200))
        updateTemperatureChart(dataBuffer.getLastN(200))
        updateQualityIndicator(quality)
    }
    
    private fun updateGSRChart(data: List<GSRDataPoint>) {
        val chartData = data.map { Entry(it.timestamp.toFloat(), it.gsrValue.toFloat()) }
        gsrChart.setData(LineData(LineDataSet(chartData, "GSR")))
        gsrChart.invalidate()
    }
}
```

## Support and Resources

- **Technical Support**: Contact development team for integration assistance
- **API Documentation**: Complete JavaDoc/KDoc available in source code
- **Sample Projects**: Reference implementations in examples directory
- **Performance Guidelines**: Optimization best practices documentation
- **Testing Framework**: Comprehensive test suite for validation

---

This documentation provides a complete reference for integrating and using the enhanced ShimmerAndroidAPI functionality in the BucikaGSR system. For additional support or specific integration questions, please refer to the technical support channels.