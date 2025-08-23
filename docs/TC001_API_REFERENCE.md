# Topdon TC001 API Reference

## Overview

Complete API reference for the BucikaGSR Topdon TC001 thermal imaging integration. This document provides detailed information about all public classes, methods, and interfaces available for thermal infrared imaging, temperature measurement, and thermal data analysis.

## Core Classes

### IRUVCTC

**Package**: `com.infisense.usbir.camera`

The central coordinator for TC001 thermal imaging data collection and USB device management.

#### Constructor
```java
public IRUVCTC(Context context, IFrameCallback frameCallback)
```
*Initializes TC001 thermal camera with context and frame callback*

**Parameters**:
- `context: Context` - Application context
- `frameCallback: IFrameCallback` - Callback for thermal frame data

#### Connection Methods

##### connectUSBDevice
```java
public boolean connectUSBDevice(UsbDevice device)
```
**Description**: Establishes connection to TC001 thermal imaging device via USB  
**Parameters**:
- `device: UsbDevice` - USB device reference from USB manager
**Returns**: `boolean` - true if connection initiated successfully  
**Throws**: `SecurityException` if USB permissions not granted

##### disconnectUSBDevice
```java
public void disconnectUSBDevice()
```
**Description**: Disconnects from currently connected TC001 device  
**Thread Safety**: Safe to call from any thread  
**Side Effects**: Stops thermal imaging stream and releases USB resources

##### isConnected
```java
public boolean isConnected()
```
**Description**: Checks current TC001 device connection status  
**Returns**: `boolean` - true if device is connected and operational

#### Thermal Imaging Control

##### startThermalStream
```java
public boolean startThermalStream()
```
**Description**: Starts thermal imaging data stream from TC001  
**Returns**: `boolean` - true if stream started successfully  
**Frequency**: 25 Hz thermal frame rate  
**Resolution**: 256x192 thermal resolution with temperature data

##### stopThermalStream
```java
public void stopThermalStream()
```
**Description**: Stops thermal imaging data stream  
**Thread Safety**: Safe to call from any thread  
**Resource Management**: Automatically releases thermal processing resources

##### setThermalParameters
```java
public void setThermalParameters(float emissivity, float ambientTemp, float distance)
```
**Description**: Sets thermal measurement parameters for accurate temperature calculation  
**Parameters**:
- `emissivity: float` - Surface emissivity (0.1 - 1.0)
- `ambientTemp: float` - Ambient temperature in Celsius
- `distance: float` - Distance to target in meters

#### Temperature Measurement

##### getTemperatureAt
```java
public float getTemperatureAt(int x, int y)
```
**Description**: Gets temperature at specific pixel coordinates  
**Parameters**:
- `x: int` - X coordinate (0-255)
- `y: int` - Y coordinate (0-191)
**Returns**: `float` - Temperature in Celsius  
**Accuracy**: ±2°C or ±2% of reading

##### getMaxTemperature
```java
public TemperaturePoint getMaxTemperature()
```
**Description**: Finds maximum temperature point in current thermal frame  
**Returns**: `TemperaturePoint` - Point with coordinates and temperature value  
**Thread Safety**: Safe to call during thermal streaming

##### getMinTemperature
```java
public TemperaturePoint getMinTemperature()
```
**Description**: Finds minimum temperature point in current thermal frame  
**Returns**: `TemperaturePoint` - Point with coordinates and temperature value

##### getAverageTemperature
```java
public float getAverageTemperature(Rect region)
```
**Description**: Calculates average temperature in specified region  
**Parameters**:
- `region: Rect` - Rectangular region for temperature averaging
**Returns**: `float` - Average temperature in Celsius

### TemperatureView

**Package**: `com.infisense.usbir.view`

Advanced UI component for thermal imaging display with temperature measurement capabilities.

#### Constructor
```java
public TemperatureView(Context context, AttributeSet attrs)
```

#### Display Control

##### setThermalBitmap
```java
public void setThermalBitmap(Bitmap thermalBitmap, byte[] temperatureData)
```
**Description**: Updates thermal display with new frame and temperature data  
**Parameters**:
- `thermalBitmap: Bitmap` - Visual thermal image
- `temperatureData: byte[]` - Raw temperature data array
**Thread Safety**: Must be called from UI thread

##### setPseudocolorMode
```java
public void setPseudocolorMode(@PseudocolorMode int mode)
```
**Description**: Sets thermal image color mapping mode  
**Parameters**:
- `mode: int` - Color mode (IRON, RAINBOW, WHITE_HOT, BLACK_HOT, etc.)
**Available Modes**:
- `PSEUDOCOLOR_IRON` - Iron bow color mapping
- `PSEUDOCOLOR_RAINBOW` - Rainbow color mapping  
- `PSEUDOCOLOR_WHITE_HOT` - White hot mapping
- `PSEUDOCOLOR_BLACK_HOT` - Black hot mapping

##### setTemperatureRange
```java
public void setTemperatureRange(float minTemp, float maxTemp)
```
**Description**: Sets temperature display range for optimal contrast  
**Parameters**:
- `minTemp: float` - Minimum temperature in Celsius
- `maxTemp: float` - Maximum temperature in Celsius

#### Measurement Tools

##### enableSpotMeter
```java
public void enableSpotMeter(boolean enabled)
```
**Description**: Enables/disables center spot temperature measurement  
**Parameters**:
- `enabled: boolean` - true to enable spot meter

##### addTemperatureArea
```java
public int addTemperatureArea(Rect area, String label)
```
**Description**: Adds rectangular area for continuous temperature monitoring  
**Parameters**:
- `area: Rect` - Measurement area coordinates
- `label: String` - Display label for the area
**Returns**: `int` - Area ID for future reference

##### removeTemperatureArea
```java
public void removeTemperatureArea(int areaId)
```
**Description**: Removes temperature measurement area  
**Parameters**:
- `areaId: int` - Area ID returned from addTemperatureArea

##### setTemperatureLine
```java
public void setTemperatureLine(Point start, Point end)
```
**Description**: Sets line for temperature profile measurement  
**Parameters**:
- `start: Point` - Line start coordinates
- `end: Point` - Line end coordinates

### OpencvTools

**Package**: `com.infisense.usbir.utils`

Utility class for thermal image processing and analysis using OpenCV.

#### Image Processing

##### applyThermalFilter
```java
public static Bitmap applyThermalFilter(Bitmap input, FilterType filterType)
```
**Description**: Applies digital filtering to thermal images  
**Parameters**:
- `input: Bitmap` - Input thermal bitmap
- `filterType: FilterType` - Filter type (GAUSSIAN, MEDIAN, BILATERAL)
**Returns**: `Bitmap` - Filtered thermal image

##### detectThermalAnomalies
```java
public static List<Point> detectThermalAnomalies(byte[] temperatureData, int width, int height, float threshold)
```
**Description**: Detects temperature anomalies using computer vision  
**Parameters**:
- `temperatureData: byte[]` - Raw temperature data
- `width: int` - Image width
- `height: int` - Image height  
- `threshold: float` - Anomaly detection threshold
**Returns**: `List<Point>` - List of anomaly locations

##### calculateThermalHistogram
```java
public static int[] calculateThermalHistogram(byte[] temperatureData, float minTemp, float maxTemp, int bins)
```
**Description**: Calculates temperature histogram for analysis  
**Parameters**:
- `temperatureData: byte[]` - Temperature data array
- `minTemp: float` - Minimum temperature for histogram
- `maxTemp: float` - Maximum temperature for histogram
- `bins: int` - Number of histogram bins
**Returns**: `int[]` - Histogram data array

### ThermalDataWriter

**Package**: `com.infisense.usbir.data`

Professional data recording and export functionality for thermal imaging.

#### Constructor
```java
private ThermalDataWriter(Context context)
```
*Private constructor - use `getInstance()` to obtain instance*

#### Static Methods

##### getInstance
```java
public static ThermalDataWriter getInstance(Context context)
```
**Description**: Returns singleton instance of ThermalDataWriter  
**Parameters**:
- `context: Context` - Application context
**Returns**: `ThermalDataWriter` instance  
**Thread Safety**: Thread-safe singleton implementation

#### Recording Methods

##### startRecording
```java
public boolean startRecording(String filename, RecordingConfig config)
```
**Description**: Starts thermal data recording to file  
**Parameters**:
- `filename: String` - Output filename (without extension)
- `config: RecordingConfig` - Recording configuration
**Returns**: `boolean` - true if recording started successfully  
**File Formats**: Supports CSV, binary, and video formats

##### stopRecording
```java
public void stopRecording()
```
**Description**: Stops current thermal data recording  
**Thread Safety**: Safe to call from any thread  
**File Management**: Automatically finalizes and closes output file

##### recordThermalFrame
```java
public void recordThermalFrame(long timestamp, byte[] temperatureData, ThermalMetadata metadata)
```
**Description**: Records single thermal frame with metadata  
**Parameters**:
- `timestamp: long` - Frame timestamp in milliseconds
- `temperatureData: byte[]` - Raw temperature data
- `metadata: ThermalMetadata` - Frame metadata (emissivity, ambient temp, etc.)

#### Export Methods

##### exportThermalData
```java
public String exportThermalData(ExportConfig config)
```
**Description**: Exports recorded thermal data with analysis  
**Parameters**:
- `config: ExportConfig` - Export configuration and options
**Returns**: `String` - Path to exported file  
**Formats**: CSV, JSON, or binary export with statistical analysis

##### exportThermalVideo
```java
public String exportThermalVideo(VideoConfig config)
```
**Description**: Exports thermal recording as video file  
**Parameters**:
- `config: VideoConfig` - Video export configuration
**Returns**: `String` - Path to exported video file  
**Formats**: MP4 with thermal colormap overlay

### ThermalCalibration

**Package**: `com.infisense.usbir.calibration`

Professional thermal calibration utilities for accurate temperature measurement.

#### Calibration Methods

##### calibrateWithBlackbody
```java
public static CalibrationResult calibrateWithBlackbody(float[] knownTemperatures, float[] measuredTemperatures)
```
**Description**: Calibrates thermal sensor using blackbody reference  
**Parameters**:
- `knownTemperatures: float[]` - Known reference temperatures
- `measuredTemperatures: float[]` - Measured sensor temperatures
**Returns**: `CalibrationResult` - Calibration coefficients and accuracy metrics

##### applyCalibration
```java
public static float applyCalibration(float rawTemperature, CalibrationData calibration)
```
**Description**: Applies calibration correction to temperature reading  
**Parameters**:
- `rawTemperature: float` - Raw temperature from sensor
- `calibration: CalibrationData` - Calibration correction data
**Returns**: `float` - Calibrated temperature in Celsius

##### validateCalibration
```java
public static ValidationResult validateCalibration(CalibrationData calibration, float[] testTemperatures)
```
**Description**: Validates calibration accuracy with test measurements  
**Parameters**:
- `calibration: CalibrationData` - Calibration to validate
- `testTemperatures: float[]` - Test temperature measurements
**Returns**: `ValidationResult` - Accuracy metrics and validation status

## Data Classes

### TemperaturePoint

**Package**: `com.infisense.usbir.data`

Data class representing a temperature measurement point.

```java
public class TemperaturePoint {
    public final int x;           // X coordinate
    public final int y;           // Y coordinate  
    public final float temperature; // Temperature in Celsius
    public final long timestamp;   // Measurement timestamp
}
```

### ThermalMetadata

**Package**: `com.infisense.usbir.data`

Comprehensive metadata for thermal measurements.

```java
public class ThermalMetadata {
    public float emissivity;       // Surface emissivity
    public float ambientTemp;      // Ambient temperature
    public float distance;         // Distance to target
    public float humidity;         // Relative humidity
    public String location;        // GPS location
    public long timestamp;         // Measurement timestamp
}
```

### RecordingConfig

**Package**: `com.infisense.usbir.data`

Configuration for thermal data recording.

```java
public class RecordingConfig {
    public int frameRate;          // Recording frame rate (1-25 Hz)
    public RecordingFormat format; // Recording format (CSV, BINARY, VIDEO)
    public boolean includeMetadata; // Include measurement metadata
    public CompressionLevel compression; // Data compression level
}
```

### ExportConfig

**Package**: `com.infisense.usbir.data`

Configuration for thermal data export.

```java
public class ExportConfig {
    public ExportFormat format;     // Export format (CSV, JSON, BINARY)
    public boolean includeAnalysis; // Include statistical analysis
    public boolean includeImages;   // Include thermal images
    public String outputPath;       // Custom output directory
}
```

## Interfaces and Callbacks

### IFrameCallback

**Package**: `com.energy.iruvc.utils`

Callback interface for thermal frame data.

```java
public interface IFrameCallback {
    void onThermalFrame(Bitmap thermalBitmap, byte[] temperatureData, long timestamp);
    void onError(String errorMessage, int errorCode);
}
```

### IThermalDataListener

**Package**: `com.infisense.usbir.listener`

Listener interface for thermal data events.

```java
public interface IThermalDataListener {
    void onTemperatureChanged(float temperature, Point location);
    void onThermalRangeChanged(float minTemp, float maxTemp);
    void onRecordingStateChanged(boolean isRecording);
}
```

### ICalibrationListener

**Package**: `com.infisense.usbir.calibration`

Callback interface for calibration operations.

```java
public interface ICalibrationListener {
    void onCalibrationStarted();
    void onCalibrationProgress(int progress);
    void onCalibrationCompleted(CalibrationResult result);
    void onCalibrationError(String error);
}
```

## Constants and Enums

### PseudocolorMode

```java
public static final int PSEUDOCOLOR_IRON = 0;
public static final int PSEUDOCOLOR_RAINBOW = 1;
public static final int PSEUDOCOLOR_WHITE_HOT = 2;
public static final int PSEUDOCOLOR_BLACK_HOT = 3;
public static final int PSEUDOCOLOR_RED_HOT = 4;
public static final int PSEUDOCOLOR_GREEN_HOT = 5;
public static final int PSEUDOCOLOR_BLUE_HOT = 6;
```

### FilterType

```java
public enum FilterType {
    GAUSSIAN,    // Gaussian blur filter
    MEDIAN,      // Median noise reduction  
    BILATERAL,   // Bilateral edge-preserving filter
    MORPHOLOGY,  // Morphological operations
    SOBEL        // Edge detection filter
}
```

### RecordingFormat

```java
public enum RecordingFormat {
    CSV,         // Comma-separated values
    BINARY,      // Binary temperature data
    VIDEO,       // MP4 thermal video
    RADIOMETRIC  // Professional radiometric format
}
```

## Error Codes

### USB Connection Errors
- `ERROR_USB_PERMISSION_DENIED = 1001` - USB permission not granted
- `ERROR_USB_DEVICE_NOT_FOUND = 1002` - TC001 device not connected
- `ERROR_USB_CONNECTION_FAILED = 1003` - Failed to establish USB connection
- `ERROR_USB_COMMUNICATION_TIMEOUT = 1004` - USB communication timeout

### Thermal Imaging Errors
- `ERROR_THERMAL_STREAM_FAILED = 2001` - Failed to start thermal stream
- `ERROR_THERMAL_CALIBRATION_INVALID = 2002` - Invalid calibration data
- `ERROR_THERMAL_TEMPERATURE_OUT_OF_RANGE = 2003` - Temperature reading out of sensor range
- `ERROR_THERMAL_FRAME_CORRUPTED = 2004` - Corrupted thermal frame data

### File I/O Errors
- `ERROR_FILE_WRITE_FAILED = 3001` - Failed to write thermal data file
- `ERROR_FILE_READ_FAILED = 3002` - Failed to read thermal data file
- `ERROR_FILE_STORAGE_FULL = 3003` - Insufficient storage space
- `ERROR_FILE_PERMISSION_DENIED = 3004` - File system permission denied

## Usage Examples

### Basic Thermal Imaging Setup

```java
// Initialize thermal camera
IRUVCTC thermalCamera = new IRUVCTC(context, new IFrameCallback() {
    @Override
    public void onThermalFrame(Bitmap thermalBitmap, byte[] temperatureData, long timestamp) {
        // Update thermal display
        temperatureView.setThermalBitmap(thermalBitmap, temperatureData);
        
        // Get center spot temperature
        float centerTemp = thermalCamera.getTemperatureAt(128, 96);
        updateTemperatureDisplay(centerTemp);
    }
    
    @Override
    public void onError(String errorMessage, int errorCode) {
        Log.e("ThermalCamera", "Error: " + errorMessage + " (Code: " + errorCode + ")");
    }
});

// Connect to TC001 device
UsbDevice tc001Device = findTC001Device();
if (tc001Device != null) {
    boolean connected = thermalCamera.connectUSBDevice(tc001Device);
    if (connected) {
        thermalCamera.startThermalStream();
    }
}
```

### Temperature Measurement and Analysis

```java
// Set thermal measurement parameters
thermalCamera.setThermalParameters(0.95f, 25.0f, 1.0f); // emissivity, ambient temp, distance

// Enable temperature measurement tools
temperatureView.enableSpotMeter(true);

// Add temperature monitoring areas
Rect hotSpotArea = new Rect(50, 50, 150, 150);
int areaId = temperatureView.addTemperatureArea(hotSpotArea, "Hot Spot");

// Get temperature statistics
TemperaturePoint maxTemp = thermalCamera.getMaxTemperature();
TemperaturePoint minTemp = thermalCamera.getMinTemperature();
float avgTemp = thermalCamera.getAverageTemperature(hotSpotArea);

Log.d("ThermalStats", String.format("Max: %.1f°C at (%d,%d)", 
    maxTemp.temperature, maxTemp.x, maxTemp.y));
Log.d("ThermalStats", String.format("Min: %.1f°C, Avg: %.1f°C", 
    minTemp.temperature, avgTemp));
```

### Professional Data Recording

```java
// Configure thermal data recording
ThermalDataWriter dataWriter = ThermalDataWriter.getInstance(context);

RecordingConfig recordingConfig = new RecordingConfig();
recordingConfig.frameRate = 25;
recordingConfig.format = RecordingFormat.CSV;
recordingConfig.includeMetadata = true;
recordingConfig.compression = CompressionLevel.MEDIUM;

// Start recording thermal data
boolean recordingStarted = dataWriter.startRecording("thermal_session_001", recordingConfig);

if (recordingStarted) {
    // Recording will continue automatically with thermal frames
    // Stop recording when finished
    dataWriter.stopRecording();
    
    // Export data with analysis
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.format = ExportFormat.CSV;
    exportConfig.includeAnalysis = true;
    exportConfig.includeImages = true;
    
    String exportPath = dataWriter.exportThermalData(exportConfig);
    Log.d("ThermalExport", "Data exported to: " + exportPath);
}
```

### Thermal Image Processing

```java
// Apply image processing to thermal data
Bitmap filteredThermal = OpencvTools.applyThermalFilter(thermalBitmap, FilterType.GAUSSIAN);

// Detect thermal anomalies
List<Point> anomalies = OpencvTools.detectThermalAnomalies(
    temperatureData, 256, 192, 5.0f); // 5°C threshold

// Calculate temperature histogram
int[] histogram = OpencvTools.calculateThermalHistogram(
    temperatureData, 0.0f, 100.0f, 50); // 0-100°C range, 50 bins

// Update display with processed data
temperatureView.setThermalBitmap(filteredThermal, temperatureData);
highlightAnomalies(anomalies);
```

### Calibration and Validation

```java
// Perform thermal calibration
float[] knownTemps = {20.0f, 40.0f, 60.0f, 80.0f, 100.0f};
float[] measuredTemps = {19.8f, 40.3f, 59.7f, 80.2f, 100.1f};

CalibrationResult calibration = ThermalCalibration.calibrateWithBlackbody(
    knownTemps, measuredTemps);

if (calibration.isValid()) {
    // Apply calibration to measurements
    float calibratedTemp = ThermalCalibration.applyCalibration(
        rawTemperature, calibration.getCalibrationData());
    
    // Validate calibration accuracy
    float[] testTemps = {30.0f, 50.0f, 70.0f};
    ValidationResult validation = ThermalCalibration.validateCalibration(
        calibration.getCalibrationData(), testTemps);
    
    Log.d("Calibration", "Accuracy: " + validation.getAccuracy() + "%");
}
```

## Thread Safety

### UI Thread Requirements
- `TemperatureView.setThermalBitmap()` - Must be called from UI thread
- All View-related operations - UI thread required

### Background Thread Safe
- `IRUVCTC.connectUSBDevice()` - Safe to call from background thread
- `ThermalDataWriter` operations - Thread-safe implementation
- Temperature calculation methods - Thread-safe

### Synchronization
- All thermal data callbacks use internal synchronization
- Multiple listeners supported with concurrent access protection
- File I/O operations use background thread queuing

## Performance Considerations

### Memory Usage
- Thermal frames: ~98KB per frame (256x192x2 bytes)
- Bitmap display: ~196KB per frame (ARGB_8888)
- Recommended: Recycle bitmaps when not needed

### Processing Performance  
- Thermal stream: 25 FPS maximum
- Temperature calculations: <1ms per point
- Image processing: 10-50ms depending on filter complexity

### Storage Requirements
- CSV recording: ~2.5MB per minute
- Binary recording: ~1.5MB per minute  
- Video export: ~5-10MB per minute (compressed)

This comprehensive API reference provides complete documentation for professional-grade thermal imaging applications using the Topdon TC001 device integration.