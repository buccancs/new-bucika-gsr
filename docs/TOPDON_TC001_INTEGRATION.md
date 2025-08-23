# Topdon TC001 Integration - Comprehensive Documentation

## Overview

The BucikaGSR system provides professional-grade thermal infrared imaging through comprehensive Topdon TC001 integration. This documentation covers the complete architecture, APIs, and usage patterns for the enhanced thermal imaging functionality with synchronized GSR data collection.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [API Reference](#api-reference)
4. [Integration Guide](#integration-guide)
5. [Data Formats](#data-formats)
6. [Configuration Reference](#configuration-reference)
7. [Synchronization](#synchronization)
8. [Troubleshooting](#troubleshooting)
9. [Examples](#examples)

## Architecture Overview

The TC001 thermal imaging system is built on a modular architecture that provides:

- **Professional Thermal Imaging**: 256x192 resolution at 25 Hz with sub-degree precision
- **Complete USB Device Management**: Full control over TC001 camera parameters
- **Real-time Temperature Analysis**: Advanced measurement tools and anomaly detection
- **Professional Data Recording**: Background CSV/binary export with thermal metadata
- **Multi-Modal Synchronization**: Concurrent operation with GSR sensors
- **Advanced Image Processing**: OpenCV-based thermal filtering and analysis

### Component Architecture

```
┌──────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│IRGalleryEditActivity│    │ TemperatureView │    │ ThermalSettings │
│                  │    │                 │    │                 │
└─────────┬────────┘    └─────────┬───────┘    └─────────┬───────┘
          │                       │                      │
          │               ┌───────▼────────┐             │
          └───────────────►│    IRUVCTC     │◄────────────┘
                          │                │
                          └───────┬────────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
 ┌───────▼────────┐    ┌──────────▼──────────┐    ┌────────▼──────┐
 │   UVCCamera    │    │   OpencvTools       │    │ThermalDataWriter│
 │                │    │                     │    │               │
 └────────────────┘    └─────────────────────┘    └───────────────┘
```

## Core Components

### IRUVCTC - Thermal Camera Controller

The central coordinator managing TC001 thermal camera operations.

**Key Responsibilities**:
- USB device connection and communication
- Thermal imaging data stream management  
- Temperature measurement coordination
- Frame synchronization and timing

**Features**:
- Real-time thermal streaming at 25 Hz
- Automatic device detection and connection
- Professional temperature calibration support
- Thread-safe operation for concurrent access

```java
// Basic IRUVCTC initialization
IRUVCTC thermalCamera = new IRUVCTC(context, frameCallback);
boolean connected = thermalCamera.connectUSBDevice(usbDevice);
thermalCamera.startThermalStream();
```

### TemperatureView - Advanced Thermal Display

Professional UI component for thermal imaging visualization and measurement.

**Capabilities**:
- Multiple pseudocolor modes (Iron, Rainbow, White Hot, Black Hot)
- Interactive temperature measurement tools
- Real-time spot meter and area monitoring
- Temperature line profiling
- Multi-point temperature tracking

**Display Modes**:
- **Spot Meter**: Center point temperature display
- **Area Measurement**: Rectangular region temperature statistics
- **Line Profile**: Temperature gradient analysis along line
- **Multi-Point**: Multiple simultaneous temperature points

```java
// Configure temperature display
temperatureView.setPseudocolorMode(PSEUDOCOLOR_IRON);
temperatureView.enableSpotMeter(true);
temperatureView.addTemperatureArea(measurementRect, "Hot Zone");
```

### ThermalDataWriter - Professional Recording

Comprehensive thermal data recording and export system.

**Recording Formats**:
- **CSV**: Human-readable temperature data with timestamps
- **Binary**: Compact binary format for large datasets
- **Video**: MP4 thermal video export with colormap overlay
- **Radiometric**: Professional radiometric data format

**Features**:
- Background recording with minimal performance impact
- Automatic file management and compression
- Metadata inclusion (emissivity, ambient temperature, etc.)
- Statistical analysis generation

```java
// Professional thermal recording
ThermalDataWriter writer = ThermalDataWriter.getInstance(context);
RecordingConfig config = new RecordingConfig();
config.format = RecordingFormat.CSV;
config.includeMetadata = true;
writer.startRecording("thermal_session", config);
```

### OpencvTools - Advanced Image Processing

Computer vision utilities for thermal image analysis.

**Processing Capabilities**:
- **Digital Filtering**: Gaussian, median, bilateral filtering
- **Anomaly Detection**: Automatic hot/cold spot identification  
- **Histogram Analysis**: Temperature distribution analysis
- **Edge Detection**: Thermal gradient analysis
- **Morphological Operations**: Noise reduction and feature enhancement

```java
// Apply thermal image processing
Bitmap filtered = OpencvTools.applyThermalFilter(thermal, FilterType.GAUSSIAN);
List<Point> anomalies = OpencvTools.detectThermalAnomalies(tempData, 256, 192, 5.0f);
```

## API Reference

### USB Device Management

#### Device Detection
```java
// Find TC001 thermal camera device
private UsbDevice findTC001Device() {
    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
    
    for (UsbDevice device : deviceList.values()) {
        if (device.getVendorId() == TC001_VENDOR_ID && 
            device.getProductId() == TC001_PRODUCT_ID) {
            return device;
        }
    }
    return null;
}
```

#### Connection Management
```java
// Connect to TC001 with permission handling
private void connectTC001() {
    UsbDevice device = findTC001Device();
    if (device != null) {
        if (usbManager.hasPermission(device)) {
            thermalCamera.connectUSBDevice(device);
        } else {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(device, permissionIntent);
        }
    }
}
```

### Thermal Streaming Integration

#### Frame Processing Pipeline
```java
private IFrameCallback thermalFrameCallback = new IFrameCallback() {
    @Override
    public void onThermalFrame(Bitmap thermalBitmap, byte[] temperatureData, long timestamp) {
        runOnUiThread(() -> {
            // Update thermal display
            temperatureView.setThermalBitmap(thermalBitmap, temperatureData);
            
            // Record thermal data if recording active
            if (isRecording) {
                ThermalMetadata metadata = new ThermalMetadata();
                metadata.timestamp = timestamp;
                metadata.emissivity = currentEmissivity;
                metadata.ambientTemp = ambientTemperature;
                
                thermalDataWriter.recordThermalFrame(timestamp, temperatureData, metadata);
            }
            
            // Process temperature measurements
            processTemperatureMeasurements(temperatureData, timestamp);
        });
    }
    
    @Override
    public void onError(String errorMessage, int errorCode) {
        Log.e(TAG, "Thermal camera error: " + errorMessage + " (Code: " + errorCode + ")");
        handleThermalError(errorCode);
    }
};
```

### Temperature Measurement Integration

#### Multi-Point Temperature Monitoring
```java
public class ThermalMeasurementManager {
    private List<TemperatureArea> measurementAreas = new ArrayList<>();
    private TemperaturePoint spotMeter = null;
    
    public void updateMeasurements(byte[] temperatureData, long timestamp) {
        // Update spot meter
        if (spotMeterEnabled) {
            spotMeter = new TemperaturePoint(128, 96, 
                getTemperatureAt(temperatureData, 128, 96), timestamp);
        }
        
        // Update measurement areas
        for (TemperatureArea area : measurementAreas) {
            updateAreaStatistics(area, temperatureData, timestamp);
        }
        
        // Notify listeners
        notifyTemperatureListeners();
    }
    
    private void updateAreaStatistics(TemperatureArea area, byte[] tempData, long timestamp) {
        float sum = 0, min = Float.MAX_VALUE, max = Float.MIN_VALUE;
        int count = 0;
        
        for (int y = area.bounds.top; y < area.bounds.bottom; y++) {
            for (int x = area.bounds.left; x < area.bounds.right; x++) {
                float temp = getTemperatureAt(tempData, x, y);
                sum += temp;
                min = Math.min(min, temp);
                max = Math.max(max, temp);
                count++;
            }
        }
        
        area.statistics = new TemperatureStatistics(
            sum / count,  // average
            min,          // minimum
            max,          // maximum
            timestamp     // timestamp
        );
    }
}
```

## Data Formats

### Thermal Frame Data Structure

#### Raw Temperature Data
```java
// Temperature data format: 16-bit signed integers
// Resolution: 256 × 192 pixels
// Data size: 98,304 bytes (256 * 192 * 2)
// Temperature conversion: (rawValue / 64.0f) - 273.15f

public class ThermalFrameData {
    public final int width = 256;
    public final int height = 192;
    public final byte[] temperatureData;  // Raw temperature data
    public final long timestamp;          // Frame timestamp
    public final ThermalMetadata metadata; // Measurement parameters
    
    public float getTemperatureAt(int x, int y) {
        int index = (y * width + x) * 2;
        int rawValue = (temperatureData[index + 1] << 8) | 
                      (temperatureData[index] & 0xFF);
        return (rawValue / 64.0f) - 273.15f;
    }
}
```

### CSV Export Format

#### Thermal Data CSV Structure
```csv
# Thermal Recording Session
# Device: Topdon TC001
# Resolution: 256x192
# Session Start: 2024-01-15T10:30:00Z
# Emissivity: 0.95
# Ambient Temperature: 25.0°C
# Distance: 1.0m

Timestamp,Frame,MinTemp,MaxTemp,AvgTemp,SpotTemp,Area1_Avg,Area1_Min,Area1_Max
1705316200000,1,18.5,45.2,24.8,32.1,38.7,35.2,42.8
1705316200040,2,18.3,45.8,24.9,32.3,38.9,35.4,43.1
1705316200080,3,18.2,46.1,25.0,32.2,39.1,35.6,43.5
```

#### Temperature Map Export
```csv
# Temperature Map Export
# Frame: 150
# Timestamp: 1705316206000
# Format: X,Y,Temperature

X,Y,Temperature
0,0,25.2
1,0,25.1
2,0,25.3
...
255,191,28.7
```

### Binary Data Format

#### Radiometric Binary Format
```java
public class RadiameterFormat {
    // File header (64 bytes)
    public static class Header {
        byte[] signature = "TC001RAD".getBytes();  // 8 bytes
        int version = 1;                           // 4 bytes
        int width = 256;                           // 4 bytes
        int height = 192;                          // 4 bytes
        long sessionStart;                         // 8 bytes
        float emissivity;                          // 4 bytes
        float ambientTemp;                         // 4 bytes
        float distance;                            // 4 bytes
        byte[] reserved = new byte[24];            // 24 bytes
    }
    
    // Frame data (98,320 bytes per frame)
    public static class FrameData {
        long timestamp;                            // 8 bytes
        float[] temperatures = new float[49152];   // 196,608 bytes
        byte[] reserved = new byte[8];             // 8 bytes
    }
}
```

## Configuration Reference

### Thermal Parameters

#### Measurement Configuration
```java
public class ThermalConfig {
    // Temperature measurement parameters
    public float emissivity = 0.95f;        // Surface emissivity (0.1 - 1.0)
    public float ambientTemperature = 25.0f; // Ambient temperature (°C)
    public float distance = 1.0f;            // Distance to target (meters)
    public float humidity = 50.0f;           // Relative humidity (%)
    
    // Display configuration
    public int pseudocolorMode = PSEUDOCOLOR_IRON;
    public boolean autoRange = true;
    public float manualMinTemp = -20.0f;
    public float manualMaxTemp = 120.0f;
    
    // Recording configuration
    public RecordingFormat recordingFormat = RecordingFormat.CSV;
    public boolean includeMetadata = true;
    public int compressionLevel = 2;         // 0-3 compression level
    
    // Image processing
    public boolean enableFiltering = true;
    public FilterType defaultFilter = FilterType.GAUSSIAN;
    public boolean anomalyDetection = false;
    public float anomalyThreshold = 5.0f;    // Temperature difference threshold
}
```

#### SharedPreferences Integration
```java
public class ThermalSettings {
    private static final String PREF_NAME = "thermal_settings";
    private static final String KEY_EMISSIVITY = "emissivity";
    private static final String KEY_AMBIENT_TEMP = "ambient_temperature";
    private static final String KEY_DISTANCE = "distance";
    private static final String KEY_PSEUDOCOLOR_MODE = "pseudocolor_mode";
    
    public static void saveThermalConfig(Context context, ThermalConfig config) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putFloat(KEY_EMISSIVITY, config.emissivity);
        editor.putFloat(KEY_AMBIENT_TEMP, config.ambientTemperature);
        editor.putFloat(KEY_DISTANCE, config.distance);
        editor.putInt(KEY_PSEUDOCOLOR_MODE, config.pseudocolorMode);
        
        editor.apply();
    }
    
    public static ThermalConfig loadThermalConfig(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ThermalConfig config = new ThermalConfig();
        
        config.emissivity = prefs.getFloat(KEY_EMISSIVITY, 0.95f);
        config.ambientTemperature = prefs.getFloat(KEY_AMBIENT_TEMP, 25.0f);
        config.distance = prefs.getFloat(KEY_DISTANCE, 1.0f);
        config.pseudocolorMode = prefs.getInt(KEY_PSEUDOCOLOR_MODE, PSEUDOCOLOR_IRON);
        
        return config;
    }
}
```

## Synchronization

### GSR and Thermal Data Synchronization

#### Synchronized Data Collection
```java
public class MultiModalDataManager {
    private GSRManager gsrManager;
    private IRUVCTC thermalCamera;
    private SynchronizedDataWriter dataWriter;
    private final Object syncLock = new Object();
    
    public void startSynchronizedRecording() {
        // Initialize synchronized timestamp
        long sessionStart = System.currentTimeMillis();
        
        // Configure GSR data listener
        gsrManager.setGSRDataListener(new GSRManager.GSRDataListener() {
            @Override
            public void onGSRDataReceived(long timestamp, double gsrValue, double skinTemp) {
                synchronized (syncLock) {
                    dataWriter.recordGSRData(timestamp - sessionStart, gsrValue, skinTemp);
                }
            }
        });
        
        // Configure thermal frame callback
        thermalCamera.setFrameCallback(new IFrameCallback() {
            @Override
            public void onThermalFrame(Bitmap bitmap, byte[] tempData, long timestamp) {
                synchronized (syncLock) {
                    dataWriter.recordThermalFrame(timestamp - sessionStart, tempData);
                }
            }
        });
        
        // Start synchronized data collection
        dataWriter.startSession(sessionStart);
        gsrManager.startDataCollection();
        thermalCamera.startThermalStream();
    }
}
```

#### Temporal Alignment
```java
public class DataSynchronizer {
    private static final long MAX_SYNC_OFFSET = 50; // 50ms tolerance
    
    public void alignDataStreams(List<GSRDataPoint> gsrData, List<ThermalFrame> thermalData) {
        // Sort data by timestamp
        gsrData.sort(Comparator.comparingLong(dp -> dp.timestamp));
        thermalData.sort(Comparator.comparingLong(tf -> tf.timestamp));
        
        // Create synchronized data pairs
        List<SynchronizedDataPoint> syncedData = new ArrayList<>();
        
        int gsrIndex = 0, thermalIndex = 0;
        while (gsrIndex < gsrData.size() && thermalIndex < thermalData.size()) {
            GSRDataPoint gsrPoint = gsrData.get(gsrIndex);
            ThermalFrame thermalFrame = thermalData.get(thermalIndex);
            
            long timeDiff = Math.abs(gsrPoint.timestamp - thermalFrame.timestamp);
            
            if (timeDiff <= MAX_SYNC_OFFSET) {
                // Timestamps close enough - create synchronized point
                syncedData.add(new SynchronizedDataPoint(gsrPoint, thermalFrame));
                gsrIndex++;
                thermalIndex++;
            } else if (gsrPoint.timestamp < thermalFrame.timestamp) {
                gsrIndex++;
            } else {
                thermalIndex++;
            }
        }
    }
}
```

## Integration Guide

### Step 1: USB Permission Setup

#### AndroidManifest.xml Configuration
```xml
<!-- USB host support -->
<uses-feature android:name="android.hardware.usb.host" android:required="true" />
<uses-permission android:name="android.permission.USB_PERMISSION" />

<!-- USB device filter for TC001 -->
<activity android:name=".ThermalActivity">
    <intent-filter>
        <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
    </intent-filter>
    <meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
               android:resource="@xml/device_filter" />
</activity>
```

#### Device Filter Configuration (res/xml/device_filter.xml)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <usb-device vendor-id="1234" product-id="5678" />
</resources>
```

### Step 2: Basic Integration

#### Activity Setup
```java
public class ThermalActivity extends AppCompatActivity {
    private IRUVCTC thermalCamera;
    private TemperatureView temperatureView;
    private UsbManager usbManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermal);
        
        // Initialize views
        temperatureView = findViewById(R.id.temperature_view);
        
        // Initialize USB manager
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        
        // Initialize thermal camera
        thermalCamera = new IRUVCTC(this, thermalFrameCallback);
        
        // Check for connected TC001 device
        checkForTC001Device();
    }
    
    private void checkForTC001Device() {
        UsbDevice device = findTC001Device();
        if (device != null) {
            if (usbManager.hasPermission(device)) {
                connectThermalCamera(device);
            } else {
                requestUSBPermission(device);
            }
        }
    }
}
```

### Step 3: Advanced Configuration

#### Thermal Settings Activity
```java
public class ThermalSettingsActivity extends AppCompatActivity {
    private SeekBar emissivitySeekBar;
    private SeekBar ambientTempSeekBar;
    private SeekBar distanceSeekBar;
    private Spinner pseudocolorSpinner;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupThermalControls();
        loadCurrentSettings();
    }
    
    private void setupThermalControls() {
        // Emissivity control (0.1 - 1.0)
        emissivitySeekBar = findViewById(R.id.emissivity_seekbar);
        emissivitySeekBar.setMax(90); // 0.1 to 1.0 in 0.01 steps
        emissivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float emissivity = 0.1f + (progress / 100.0f);
                updateEmissivity(emissivity);
            }
        });
        
        // Pseudocolor mode selection
        String[] pseudocolorModes = {"Iron", "Rainbow", "White Hot", "Black Hot", "Red Hot"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, pseudocolorModes);
        pseudocolorSpinner.setAdapter(adapter);
        pseudocolorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                temperatureView.setPseudocolorMode(position);
                ThermalSettings.savePseudocolorMode(ThermalSettingsActivity.this, position);
            }
        });
    }
}
```

### Step 4: Professional Data Export

#### Export Configuration
```java
public class ThermalExportManager {
    public void exportThermalSession(Context context, String sessionId) {
        ThermalDataWriter writer = ThermalDataWriter.getInstance(context);
        
        // Configure comprehensive export
        ExportConfig config = new ExportConfig();
        config.format = ExportFormat.CSV;
        config.includeAnalysis = true;
        config.includeImages = true;
        config.outputPath = getExportDirectory(context);
        
        // Export thermal data
        String exportPath = writer.exportThermalData(config);
        
        // Generate analysis report
        generateAnalysisReport(sessionId, exportPath);
        
        // Create export summary
        createExportSummary(sessionId, exportPath);
    }
    
    private void generateAnalysisReport(String sessionId, String exportPath) {
        // Load exported data for analysis
        ThermalDataAnalyzer analyzer = new ThermalDataAnalyzer();
        ThermalSession session = analyzer.loadSession(exportPath);
        
        // Generate comprehensive statistics
        ThermalStatistics stats = analyzer.generateStatistics(session);
        
        // Create analysis report
        String reportPath = exportPath + "/analysis_report.json";
        saveAnalysisReport(stats, reportPath);
    }
}
```

## Troubleshooting

### Common Issues and Solutions

#### USB Connection Issues

**Issue**: TC001 device not detected
```java
// Solution: Check USB device enumeration
private void debugUSBDevices() {
    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
    
    Log.d("USB Debug", "Connected USB devices:");
    for (UsbDevice device : deviceList.values()) {
        Log.d("USB Debug", String.format("Device: VID=0x%04X, PID=0x%04X, Name=%s",
            device.getVendorId(), device.getProductId(), device.getDeviceName()));
    }
}
```

**Issue**: USB permission denied
```java
// Solution: Implement proper permission handling
private void handleUSBPermission() {
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    registerReceiver(usbPermissionReceiver, filter);
    
    PendingIntent permissionIntent = PendingIntent.getBroadcast(
        this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    usbManager.requestPermission(device, permissionIntent);
}
```

#### Thermal Streaming Issues

**Issue**: Thermal frames not updating
```java
// Solution: Check frame callback registration
private void validateFrameCallback() {
    if (thermalCamera.isConnected()) {
        Log.d("Thermal Debug", "Camera connected, checking stream status");
        if (!thermalCamera.isStreaming()) {
            Log.w("Thermal Debug", "Camera connected but not streaming");
            thermalCamera.startThermalStream();
        }
    } else {
        Log.e("Thermal Debug", "Camera not connected");
        reconnectThermalCamera();
    }
}
```

#### Temperature Accuracy Issues

**Issue**: Inaccurate temperature readings
```java
// Solution: Verify measurement parameters
private void validateMeasurementParams() {
    ThermalConfig config = ThermalSettings.loadThermalConfig(this);
    
    // Check emissivity range
    if (config.emissivity < 0.1f || config.emissivity > 1.0f) {
        Log.w("Thermal", "Invalid emissivity: " + config.emissivity);
        config.emissivity = 0.95f; // Default value
    }
    
    // Check ambient temperature
    if (Math.abs(config.ambientTemperature) > 100.0f) {
        Log.w("Thermal", "Invalid ambient temperature: " + config.ambientTemperature);
        config.ambientTemperature = 25.0f; // Default value
    }
    
    // Apply corrected parameters
    thermalCamera.setThermalParameters(config.emissivity, 
        config.ambientTemperature, config.distance);
}
```

## Examples

### Complete Thermal Imaging Application

```java
public class CompleteThermalApp extends AppCompatActivity implements IThermalDataListener {
    private IRUVCTC thermalCamera;
    private TemperatureView temperatureView;
    private ThermalDataWriter dataWriter;
    private ThermalMeasurementManager measurementManager;
    
    // UI components
    private Button connectButton;
    private Button recordButton;
    private TextView statusText;
    private SeekBar emissivityControl;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermal);
        
        initializeComponents();
        setupUI();
        
        // Check for TC001 device on startup
        checkForTC001Device();
    }
    
    private void initializeComponents() {
        // Initialize thermal camera
        thermalCamera = new IRUVCTC(this, new IFrameCallback() {
            @Override
            public void onThermalFrame(Bitmap thermalBitmap, byte[] temperatureData, long timestamp) {
                handleThermalFrame(thermalBitmap, temperatureData, timestamp);
            }
            
            @Override
            public void onError(String errorMessage, int errorCode) {
                handleThermalError(errorMessage, errorCode);
            }
        });
        
        // Initialize data writer
        dataWriter = ThermalDataWriter.getInstance(this);
        
        // Initialize measurement manager
        measurementManager = new ThermalMeasurementManager();
        measurementManager.addThermalDataListener(this);
        
        // Find UI components
        temperatureView = findViewById(R.id.temperature_view);
        connectButton = findViewById(R.id.connect_button);
        recordButton = findViewById(R.id.record_button);
        statusText = findViewById(R.id.status_text);
        emissivityControl = findViewById(R.id.emissivity_seekbar);
    }
    
    private void setupUI() {
        // Connect button
        connectButton.setOnClickListener(v -> {
            if (thermalCamera.isConnected()) {
                disconnectThermalCamera();
            } else {
                connectThermalCamera();
            }
        });
        
        // Record button
        recordButton.setOnClickListener(v -> {
            if (dataWriter.isRecording()) {
                stopRecording();
            } else {
                startRecording();
            }
        });
        
        // Emissivity control
        emissivityControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float emissivity = 0.1f + (progress / 900.0f);
                    updateEmissivity(emissivity);
                }
            }
        });
        
        // Temperature view configuration
        temperatureView.setPseudocolorMode(PSEUDOCOLOR_IRON);
        temperatureView.enableSpotMeter(true);
        
        // Add measurement areas
        Rect centerArea = new Rect(100, 75, 156, 117); // Center 56x42 area
        measurementManager.addTemperatureArea(centerArea, "Center");
    }
    
    private void handleThermalFrame(Bitmap thermalBitmap, byte[] temperatureData, long timestamp) {
        runOnUiThread(() -> {
            // Update display
            temperatureView.setThermalBitmap(thermalBitmap, temperatureData);
            
            // Update measurements
            measurementManager.updateMeasurements(temperatureData, timestamp);
            
            // Record if active
            if (dataWriter.isRecording()) {
                ThermalMetadata metadata = createFrameMetadata(timestamp);
                dataWriter.recordThermalFrame(timestamp, temperatureData, metadata);
            }
        });
    }
    
    private void startRecording() {
        RecordingConfig config = new RecordingConfig();
        config.format = RecordingFormat.CSV;
        config.includeMetadata = true;
        config.frameRate = 25;
        
        String sessionId = "thermal_" + System.currentTimeMillis();
        boolean started = dataWriter.startRecording(sessionId, config);
        
        if (started) {
            recordButton.setText("Stop Recording");
            statusText.setText("Recording: " + sessionId);
        } else {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopRecording() {
        dataWriter.stopRecording();
        recordButton.setText("Start Recording");
        statusText.setText("Recording stopped");
        
        // Export data
        ExportConfig exportConfig = new ExportConfig();
        exportConfig.format = ExportFormat.CSV;
        exportConfig.includeAnalysis = true;
        
        String exportPath = dataWriter.exportThermalData(exportConfig);
        Toast.makeText(this, "Data exported to: " + exportPath, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onTemperatureChanged(float temperature, Point location) {
        runOnUiThread(() -> {
            statusText.setText(String.format("Temp: %.1f°C at (%d,%d)", 
                temperature, location.x, location.y));
        });
    }
    
    @Override
    public void onThermalRangeChanged(float minTemp, float maxTemp) {
        runOnUiThread(() -> {
            // Update temperature range display
            updateTemperatureRangeDisplay(minTemp, maxTemp);
        });
    }
}
```

This comprehensive integration documentation provides complete guidance for implementing professional-grade thermal imaging applications using the Topdon TC001 device integration with the BucikaGSR system.