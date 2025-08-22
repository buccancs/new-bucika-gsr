# API Documentation

## Overview

This directory contains complete API documentation for the Multi-Sensor Recording System.

## Components

### Python Desktop Controller APIs

#### Core Modules
- **CalibrationManager**: Camera calibration and validation APIs with RGB-thermal alignment
- **SessionManager**: Recording session coordination APIs for multi-modal recording
- **ThermalRecorderManager**: Topdon thermal camera integration with reflection-based API safety
- **ShimmerManager**: Shimmer GSR sensor management APIs
- **Shimmer Extension Methods**: Complete sensor data access following original Shimmer Android API patterns
- **PCServer**: Network communication server APIs
- **AndroidDeviceManager**: Android device integration APIs with thermal camera support

#### Calibration APIs
```python
class CalibrationManager:
    def perform_complete_calibration(self, rgb_images: List[np.ndarray], 
                                   thermal_images: List[np.ndarray]) -> CalibrationResult
    def save_calibration_results(self, result: CalibrationResult, output_path: str) -> bool
    def load_calibration_results(self, file_path: str) -> Optional[CalibrationResult]
    def align_rgb_thermal_cameras(self, rgb_matrix: np.ndarray, 
                                thermal_matrix: np.ndarray) -> TransformationMatrix
```

#### Thermal Camera APIs
```kotlin
class ThermalRecorder {
    // Initialise thermal camera system with reflection-based API safety
    fun initialise(previewSurface: SurfaceView? = null): Boolean
    fun initialise(previewSurface: SurfaceView? = null, previewStreamer: Any? = null): Boolean
    
    // Preview control with real-time switching capability
    fun startPreview(): Boolean
    fun stopPreview(): Boolean
    
    // Recording control with progressive retry logic
    fun startRecording(sessionId: String): Boolean
    fun stopRecording(): Boolean
    
    // Status monitoring with comprehensive properties
    fun getThermalCameraStatus(): ThermalCameraStatus
    fun isThermalCameraAvailable(): Boolean
    
    // Calibration and capture with multiple fallback methods
    fun captureCalibrationImage(filePath: String): Boolean
    
    // Resource management with proper cleanup
    fun cleanup()
}

data class ThermalCameraStatus(
    val isAvailable: Boolean = false,
    val isRecording: Boolean = false,
    val isPreviewActive: Boolean = false,
    val deviceName: String = "No Device",
    val width: Int = 256,
    val height: Int = 192,
    val frameRate: Int = 25,
    val frameCount: Long = 0L
)
```

#### Shimmer Sensor APIs
```kotlin
// Complete sensor data access following original Shimmer Android API patterns
fun Shimmer.getGSRReading(): Double?           // GSR conductance in μS
fun Shimmer.getPPGReading(): Double?           // PPG from INT_EXP_ADC_A13
fun Shimmer.getAccelXReading(): Double?        // X-axis acceleration in g
fun Shimmer.getAccelYReading(): Double?        // Y-axis acceleration in g
fun Shimmer.getAccelZReading(): Double?        // Z-axis acceleration in g
fun Shimmer.getGyroXReading(): Double?         // X-axis angular velocity
fun Shimmer.getGyroYReading(): Double?         // Y-axis angular velocity
fun Shimmer.getGyroZReading(): Double?         // Z-axis angular velocity
fun Shimmer.getMagXReading(): Double?          // X-axis magnetic field
fun Shimmer.getMagYReading(): Double?          // Y-axis magnetic field
fun Shimmer.getMagZReading(): Double?          // Z-axis magnetic field
fun Shimmer.getECGReading(): Double?           // ECG from EXG1_24BIT
fun Shimmer.getEMGReading(): Double?           // EMG from EXG2_24BIT
fun Shimmer.getTemperatureReading(): Double?   // Temperature in °C
fun Shimmer.getPressureReading(): Double?      // Pressure in hPa
fun Shimmer.getBatteryLevel(): Int?            // Battery percentage 0-100

// Device state management following Shimmer API patterns
fun Shimmer.isConnected(): Boolean             // Connection status
fun Shimmer.isStreaming(): Boolean             // Streaming status
fun Shimmer.isSDLogging(): Boolean             // SD logging status
fun Shimmer.isSDLoggingActive(): Boolean       // Enhanced SD logging check

// Device information access
fun Shimmer.getFirmwareVersion(): String       // Firmware version
fun Shimmer.getHardwareVersion(): String       // Hardware version  
fun Shimmer.getSamplingRate(): Double          // Current sampling rate
fun Shimmer.getEnabledSensors(): Long          // Enabled sensors bitmask
fun Shimmer.getMacAddress(): String            // Device MAC address
fun Shimmer.getDeviceName(): String            // Device name

// Advanced configuration and status
fun Shimmer.getComprehensiveStatus(): Map<String, Any>           // Complete status
fun Shimmer.writeCompleteConfiguration(config: Map<String, Any>): Boolean  // Write config
fun Shimmer.readCurrentConfiguration(): Map<String, Any>        // Read config
fun Shimmer.performCalibration(sensorType: String): Boolean     // Sensor calibration
```

#### Session Management APIs
```python
class SessionManager:
    def create_session(self, session_config: Dict) -> str
    def start_recording(self, session_id: str) -> bool
    def stop_recording(self, session_id: str) -> bool
    def get_session_status(self, session_id: str) -> SessionStatus
```

#### Network Communication APIs
```python
class PCServer:
    def start_server(self, port: int = 8080) -> bool
    def stop_server(self) -> bool
    def send_command(self, device_id: str, command: JsonMessage) -> bool
    def broadcast_command(self, command: JsonMessage) -> Dict[str, bool]
```

### Android Application APIs

#### Enhanced Recording Controllers
- **RecordingSessionController**: Pure recording operation management with enhanced error handling and thermal camera coordination
- **DeviceConnectionManager**: Device connectivity orchestration with improved timing control, race condition prevention, and thermal camera initialisation
- **ThermalRecorder**: Production-ready Topdon thermal camera integration with reflection-based API safety
- **FileTransferManager**: Data transfer operations with integrity validation including thermal data formats
- **CalibrationManager**: Calibration process coordination with multi-device synchronisation and RGB-thermal alignment

#### UI Component Architecture (New)
```kotlin
// Unified UI components eliminating 500+ lines of duplicate code
@Composable
fun RecordingIndicator(modifier: Modifier = Modifier)

@Composable  
fun DeviceStatusOverlay(
    deviceName: String,
    icon: ImageVector,
    isConnected: Boolean,
    isInitializing: Boolean,
    modifier: Modifier = Modifier,
    detailText: String? = null
)

@Composable
fun PreviewCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

#### Camera Preview Switching (New)
```kotlin
// Enhanced RecordingScreen with camera switching capability
@Composable
fun RecordingScreen(
    onNavigateToPreview: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    // Camera switching state - true for thermal/IR, false for RGB
    var showThermalCamera by remember { mutableStateOf(false) }
    
    // Toggle switch for camera preview selection
    SegmentedButton(
        onClick = { showThermalCamera = !showThermalCamera },
        selected = showThermalCamera
    ) {
        Text(if (showThermalCamera) "Thermal" else "RGB")
    }
}
```

#### Enhanced Device Initialisation (Updated)
```kotlin
class DeviceConnectionManager {
    // Enhanced with timing coordination to prevent race conditions
    suspend fun initializeWithDelay(
        cameraTextureView: TextureView,
        thermalSurfaceView: SurfaceView?
    ): Boolean
    
    // Improved error handling and logging
    fun getInitializationState(): DeviceInitializationState
    
    // Coordinated session management
    suspend fun startSessionWithCoordination(): Boolean
}
```

#### Network Communication
```kotlin
class NetworkClient {
    fun connect(serverAddress: String, port: Int): Boolean
    fun sendMessage(message: JsonMessage): Boolean
    fun registerMessageHandler(handler: MessageHandler)
    fun disconnect()
}
```

#### Recording Components

```kotlin
class CameraRecorder {
    // Enhanced with improved initialisation coordination
    fun startRecording(config: RecordingConfig): Boolean
    fun stopRecording(): Boolean
    fun getRecordingStatus(): RecordingStatus
    fun isInitialized(): Boolean  // New: Initialisation state checking
}

class ThermalRecorder {
    // Enhanced with comprehensive hardware integration and reflection-based API safety
    fun initialise(previewSurface: SurfaceView? = null): Boolean
    fun initialise(previewSurface: SurfaceView? = null, previewStreamer: Any? = null): Boolean
    fun startRecording(sessionId: String): Boolean
    fun stopRecording(): Boolean
    fun getThermalCameraStatus(): ThermalCameraStatus
    fun isThermalCameraAvailable(): Boolean
    fun captureCalibrationImage(filePath: String): Boolean
    fun cleanup()
}

// New: Enhanced error handling for device coordination
enum class DeviceInitializationState {
    NOT_STARTED,
    INITIALIZING,
    COMPLETED,
    FAILED
}
```

## Communication Protocol

### JSON Message Format
```json
{
    "type": "command|response|notification",
    "timestamp": 1234567890.123,
    "session_id": "session_uuid",
    "device_id": "device_identifier",
    "payload": {
        "command": "start_recording|stop_recording|calibrate",
        "parameters": {}
    }
}
```

### WebSocket Events
- **device_connected**: New device joins network
- **device_disconnected**: Device leaves network  
- **recording_started**: Recording session begins
- **recording_stopped**: Recording session ends
- **calibration_complete**: Calibration process finished
- **error_occurred**: Error condition detected

## Data Formats

### Calibration Data
```json
{
    "calibration_id": "cal_uuid",
    "timestamp": "2024-01-01T12:00:00Z",
    "rgb_camera_matrix": [[fx, 0, cx], [0, fy, cy], [0, 0, 1]],
    "rgb_distortion_coeffs": [k1, k2, p1, p2, k3],
    "thermal_camera_matrix": [[fx, 0, cx], [0, fy, cy], [0, 0, 1]],
    "thermal_distortion_coeffs": [k1, k2, p1, p2, k3],
    "stereo_parameters": {
        "rotation_matrix": [[r11, r12, r13], [r21, r22, r23], [r31, r32, r33]],
        "translation_vector": [tx, ty, tz]
    }
}
```

### Recording Session Data
```json
{
    "session_id": "session_uuid",
    "start_time": "2024-01-01T12:00:00Z",
    "end_time": "2024-01-01T12:30:00Z",
    "devices": [
        {
            "device_id": "device_uuid",
            "device_type": "android|shimmer|thermal",
            "data_files": ["file1.csv", "file2.mp4"],
            "calibration_id": "cal_uuid"
        }
    ],
    "synchronization_data": {
        "master_timestamps": [1234567890.123, 1234567890.456],
        "device_offsets": {"device1": 0.001, "device2": -0.002}
    }
}
```

## Error Handling

### Error Response Format
```json
{
    "type": "error",
    "timestamp": 1234567890.123,
    "error_code": "DEVICE_NOT_FOUND|CALIBRATION_FAILED|NETWORK_ERROR",
    "error_message": "Human readable error description",
    "details": {
        "device_id": "device_identifier",
        "additional_context": "Specific error details"
    }
}
```

### Common Error Codes
- **DEVICE_NOT_FOUND**: Requested device not available
- **CALIBRATION_FAILED**: Camera calibration process failed
- **NETWORK_ERROR**: Communication failure between components
- **RECORDING_ERROR**: Error during data recording
- **VALIDATION_ERROR**: Data validation failure

## Usage Examples

### Python Desktop Controller
```python
# Initialise system components
calibration_manager = CalibrationManager()
session_manager = SessionManager()
pc_server = PCServer()

# Start network server
pc_server.start_server(port=8080)

# Create and start recording session
session_id = session_manager.create_session({
    "duration": 300,  # 5 minutes
    "devices": ["android_device_1", "shimmer_device_1"]
})
session_manager.start_recording(session_id)
```

### Android Application
```kotlin
// Initialise recording components with thermal camera support
val networkClient = NetworkClient()
val cameraRecorder = CameraRecorder()
val thermalRecorder = ThermalRecorder()

// Connect to PC controller
networkClient.connect("192.168.1.100", 8080)

// Initialise thermal camera with Topdon hardware support
val thermalInitialized = thermalRecorder.initialise(surfaceView)
if (thermalInitialized) {
    val status = thermalRecorder.getThermalCameraStatus()
    Log.d("Thermal", "Device: ${status.deviceName}, Available: ${status.isAvailable}")
}

// Start recording when commanded with thermal integration
networkClient.registerMessageHandler { message ->
    when (message.payload.command) {
        "start_recording" -> {
            cameraRecorder.startRecording(message.payload.parameters)
            if (thermalInitialized) {
                thermalRecorder.startRecording(message.payload.sessionId)
            }
        }
        "stop_recording" -> {
            cameraRecorder.stopRecording()
            if (thermalInitialized) {
                thermalRecorder.stopRecording()
            }
        }
        "capture_calibration" -> {
            thermalRecorder.captureCalibrationImage("/path/to/calibration.png")
        }
    }
}
```

For complete API reference and additional examples, see the individual component documentation files.
