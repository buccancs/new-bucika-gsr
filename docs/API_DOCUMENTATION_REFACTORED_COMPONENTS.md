# API Documentation - Refactored Components

## Overview

This document provides comprehensive API documentation for the refactored components created during the code quality improvement initiative. These components implement the Manager Extraction Pattern and follow Single Responsibility Principle to reduce complexity and improve maintainability.

## Table of Contents

1. [ThermalCameraManager](#thermalcameramanager)
2. [ThermalUIStateManager](#thermaluistatemanager)
3. [ThermalConfigurationManager](#thermalconfigurationmanager)
4. [Enhanced GSR Manager](#enhanced-gsr-manager)
5. [Global Clock Manager](#global-clock-manager)
6. [Temperature Overlay Manager](#temperature-overlay-manager)
7. [Performance Optimization Framework](#performance-optimization-framework)

---

## ThermalCameraManager

**Package:** `com.topdon.tc001.thermal.camera`  
**Purpose:** Manages thermal camera initialization, USB connections, and sensor communication  
**Complexity Reduction:** 3,324 lines → 268 lines specialized component

### Class Overview

```kotlin
class ThermalCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : LifecycleObserver
```

### Key Features

- **USB Connection Management**: Handles device detection and connection lifecycle
- **Thermal Sensor Communication**: Manages data acquisition from thermal sensors
- **Camera Initialization**: Configures camera parameters and settings
- **Error Recovery**: Implements automatic retry and fallback mechanisms
- **Performance Optimization**: Optimized for zero frame drops and minimal latency

### Public API

#### Initialization

```kotlin
/**
 * Initialize the thermal camera manager
 * @param config Camera configuration parameters
 * @return CompletableFuture<Boolean> indicating success/failure
 */
fun initialize(config: ThermalCameraConfig): CompletableFuture<Boolean>

/**
 * Check if thermal camera is available and accessible
 * @return Boolean indicating camera availability
 */
fun isCameraAvailable(): Boolean
```

#### Camera Operations

```kotlin
/**
 * Start thermal camera capture
 * @param captureCallback Callback for frame data
 * @return Boolean indicating success
 */
fun startCapture(captureCallback: ThermalFrameCallback): Boolean

/**
 * Stop thermal camera capture
 * @return CompletableFuture<Void> completing when stopped
 */
fun stopCapture(): CompletableFuture<Void>

/**
 * Get current camera configuration
 * @return Current ThermalCameraConfig
 */
fun getCurrentConfig(): ThermalCameraConfig
```

#### USB Connection Management

```kotlin
/**
 * Establish USB connection to thermal camera
 * @param device UsbDevice to connect to
 * @return CompletableFuture<Boolean> indicating connection success
 */
fun connectToDevice(device: UsbDevice): CompletableFuture<Boolean>

/**
 * Disconnect from current thermal camera
 * @return CompletableFuture<Void> completing when disconnected
 */
fun disconnect(): CompletableFuture<Void>

/**
 * Get current USB connection status
 * @return USBConnectionStatus enum
 */
fun getConnectionStatus(): USBConnectionStatus
```

### Configuration Data Classes

```kotlin
data class ThermalCameraConfig(
    val resolution: ThermalResolution = ThermalResolution.RESOLUTION_320x240,
    val frameRate: Int = 30,
    val temperatureRange: TemperatureRange = TemperatureRange.AUTO,
    val emissivity: Float = 0.95f,
    val backgroundCorrection: Boolean = true,
    val noiseFiltering: Boolean = true
)

data class ThermalFrameData(
    val timestamp: Long,
    val frameIndex: Int,
    val temperatureData: FloatArray,
    val imageData: ByteArray,
    val metadata: ThermalFrameMetadata
)

enum class USBConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    PERMISSION_REQUIRED
}
```

### Usage Example

```kotlin
class ThermalActivity : AppCompatActivity() {
    private lateinit var thermalCameraManager: ThermalCameraManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        thermalCameraManager = ThermalCameraManager(this, this)
        
        val config = ThermalCameraConfig(
            resolution = ThermalResolution.RESOLUTION_320x240,
            frameRate = 30
        )
        
        thermalCameraManager.initialize(config).thenAccept { success ->
            if (success) {
                thermalCameraManager.startCapture { frameData ->
                    // Process thermal frame data
                    processFrameData(frameData)
                }
            }
        }
    }
}
```

---

## ThermalUIStateManager

**Package:** `com.topdon.tc001.thermal.ui`  
**Purpose:** Manages UI state, orientation handling, and RecyclerView management  
**Complexity Reduction:** 3,324 lines → 323 lines specialized component

### Class Overview

```kotlin
class ThermalUIStateManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityIrThermalNightBinding
) : LifecycleObserver
```

### Key Features

- **UI State Management**: Centralized UI state coordination
- **Orientation Handling**: Automatic layout adaptation for device rotation
- **RecyclerView Management**: Optimized list management for thermal data
- **Animation Control**: Smooth transitions and visual feedback
- **Memory Efficient**: Minimizes UI-related memory allocations

### Public API

#### UI State Management

```kotlin
/**
 * Initialize UI state manager
 */
fun initialize()

/**
 * Update UI state based on current thermal capture status
 * @param state New ThermalCaptureState
 */
fun updateCaptureState(state: ThermalCaptureState)

/**
 * Get current UI state
 * @return Current ThermalUIState
 */
fun getCurrentState(): ThermalUIState
```

#### Orientation Management

```kotlin
/**
 * Handle device orientation change
 * @param newOrientation New device orientation
 */
fun handleOrientationChange(newOrientation: Int)

/**
 * Lock/unlock orientation
 * @param locked Boolean indicating if orientation should be locked
 */
fun setOrientationLocked(locked: Boolean)
```

#### RecyclerView Management

```kotlin
/**
 * Initialize thermal data RecyclerView
 * @param recyclerView RecyclerView to manage
 * @param adapter Data adapter
 */
fun initializeRecyclerView(recyclerView: RecyclerView, adapter: ThermalDataAdapter)

/**
 * Update RecyclerView with new thermal data
 * @param data List of thermal data items
 */
fun updateThermalData(data: List<ThermalDataItem>)

/**
 * Scroll RecyclerView to specific position
 * @param position Target position
 * @param smooth Whether to use smooth scrolling
 */
fun scrollToPosition(position: Int, smooth: Boolean = true)
```

### State Data Classes

```kotlin
data class ThermalUIState(
    val captureState: ThermalCaptureState,
    val orientation: Int,
    val isRecording: Boolean,
    val recordingDuration: Long,
    val frameCount: Int,
    val lastFrameTimestamp: Long
)

enum class ThermalCaptureState {
    IDLE,
    INITIALIZING,
    CAPTURING,
    RECORDING,
    PAUSED,
    STOPPED,
    ERROR
}

data class ThermalDataItem(
    val timestamp: Long,
    val temperature: Float,
    val location: Point,
    val metadata: ThermalItemMetadata
)
```

---

## ThermalConfigurationManager

**Package:** `com.topdon.tc001.thermal.config`  
**Purpose:** Manages device configuration, settings persistence, and temperature calibration  
**Complexity Reduction:** 3,324 lines → 339 lines specialized component

### Class Overview

```kotlin
class ThermalConfigurationManager(
    private val context: Context
) 
```

### Key Features

- **Settings Persistence**: Automatic save/load of user preferences
- **Temperature Calibration**: Advanced calibration algorithms
- **Device Configuration**: Hardware-specific optimizations
- **Export/Import**: Configuration backup and restore
- **Validation**: Configuration parameter validation

### Public API

#### Configuration Management

```kotlin
/**
 * Load thermal configuration from persistent storage
 * @return CompletableFuture<ThermalConfiguration>
 */
fun loadConfiguration(): CompletableFuture<ThermalConfiguration>

/**
 * Save thermal configuration to persistent storage
 * @param config Configuration to save
 * @return CompletableFuture<Boolean> indicating success
 */
fun saveConfiguration(config: ThermalConfiguration): CompletableFuture<Boolean>

/**
 * Reset configuration to factory defaults
 * @return CompletableFuture<Boolean> indicating success
 */
fun resetToDefaults(): CompletableFuture<Boolean>
```

#### Temperature Calibration

```kotlin
/**
 * Perform temperature calibration
 * @param referenceTemperature Known reference temperature
 * @param measuredValues Array of measured temperature values
 * @return CompletableFuture<CalibrationResult>
 */
fun performCalibration(
    referenceTemperature: Float,
    measuredValues: FloatArray
): CompletableFuture<CalibrationResult>

/**
 * Apply temperature calibration to raw data
 * @param rawTemperature Raw temperature reading
 * @return Calibrated temperature value
 */
fun applyCalibration(rawTemperature: Float): Float

/**
 * Get current calibration status
 * @return CalibrationStatus
 */
fun getCalibrationStatus(): CalibrationStatus
```

#### Import/Export

```kotlin
/**
 * Export configuration to file
 * @param file Target file for export
 * @return CompletableFuture<Boolean> indicating success
 */
fun exportConfiguration(file: File): CompletableFuture<Boolean>

/**
 * Import configuration from file
 * @param file Source file for import
 * @return CompletableFuture<Boolean> indicating success
 */
fun importConfiguration(file: File): CompletableFuture<Boolean>
```

---

## Enhanced GSR Manager

**Package:** `com.shimmer.android.gsr`  
**Purpose:** Official Shimmer SDK integration with enhanced performance and reliability  
**Test Coverage:** 29 comprehensive test cases

### Key Features

- **Official Shimmer SDK Integration**: Direct integration with Shimmer3 GSR+ devices
- **Thread-Safe Operations**: Concurrent access protection
- **Performance Optimized**: High-frequency data acquisition (256Hz)
- **Error Recovery**: Automatic reconnection and error handling
- **Real-time Processing**: Low-latency data processing pipeline

### Public API

```kotlin
/**
 * Initialize GSR manager with device configuration
 * @param config GSR device configuration
 * @return CompletableFuture<Boolean> indicating initialization success
 */
fun initialize(config: GSRConfiguration): CompletableFuture<Boolean>

/**
 * Start GSR data collection
 * @param dataCallback Callback for GSR data samples
 * @return Boolean indicating start success
 */
fun startDataCollection(dataCallback: GSRDataCallback): Boolean

/**
 * Stop GSR data collection
 * @return CompletableFuture<Void> completing when stopped
 */
fun stopDataCollection(): CompletableFuture<Void>

/**
 * Get current GSR device status
 * @return GSRDeviceStatus
 */
fun getDeviceStatus(): GSRDeviceStatus
```

---

## Global Clock Manager

**Package:** `com.topdon.tc001.sync`  
**Purpose:** High-complexity synchronization component for multi-device timing  
**Complexity:** CC: 22+ (High complexity component)  
**Test Coverage:** 24 comprehensive test cases

### Key Features

- **Precision Timing**: Microsecond-level synchronization accuracy
- **Multi-Device Sync**: Coordinates timing across thermal camera, GSR, and recording systems
- **Clock Drift Compensation**: Automatic drift detection and correction
- **Network Time Sync**: Optional NTP synchronization
- **Performance Monitoring**: Real-time sync quality metrics

### Public API

```kotlin
/**
 * Initialize global clock synchronization
 * @param syncConfig Synchronization configuration
 * @return CompletableFuture<Boolean> indicating success
 */
fun initialize(syncConfig: SyncConfiguration): CompletableFuture<Boolean>

/**
 * Get current synchronized timestamp
 * @return Long timestamp in nanoseconds
 */
fun getCurrentTimestamp(): Long

/**
 * Synchronize with reference clock
 * @param referenceTimestamp Reference timestamp
 * @return CompletableFuture<SyncResult>
 */
fun synchronizeWithReference(referenceTimestamp: Long): CompletableFuture<SyncResult>

/**
 * Get synchronization quality metrics
 * @return SyncQualityMetrics
 */
fun getSyncQualityMetrics(): SyncQualityMetrics
```

---

## Temperature Overlay Manager

**Package:** `com.topdon.tc001.overlay`  
**Purpose:** Thermal data processing and overlay generation for visualization  
**Test Coverage:** 23 comprehensive test cases

### Key Features

- **Real-time Overlay Generation**: High-performance overlay rendering
- **Temperature Color Mapping**: Advanced thermal visualization
- **Overlay Composition**: Multi-layer overlay support
- **Performance Optimized**: GPU-accelerated rendering when available
- **Customizable Palettes**: User-configurable color schemes

### Public API

```kotlin
/**
 * Generate temperature overlay for thermal image
 * @param thermalData Raw thermal data
 * @param overlayConfig Overlay configuration
 * @return CompletableFuture<Bitmap> with generated overlay
 */
fun generateOverlay(
    thermalData: ThermalFrameData,
    overlayConfig: OverlayConfiguration
): CompletableFuture<Bitmap>

/**
 * Apply temperature color mapping
 * @param temperature Temperature value
 * @param palette Color palette to use
 * @return Color as ARGB integer
 */
fun mapTemperatureToColor(temperature: Float, palette: ColorPalette): Int

/**
 * Update overlay configuration
 * @param config New overlay configuration
 */
fun updateConfiguration(config: OverlayConfiguration)
```

---

## Performance Optimization Framework

### CapturePerformanceOptimizer

**Purpose:** Ultra-high performance capture optimization with zero frame drops  
**Features:**
- Lock-free ring buffers for high-throughput data
- Priority-optimized thread pools
- Real-time performance monitoring
- Automatic optimization based on system metrics

### Performance Monitoring

```kotlin
/**
 * Get current performance metrics
 * @return CapturePerformanceMetrics with detailed performance data
 */
fun getPerformanceMetrics(): CapturePerformanceMetrics

/**
 * Start performance monitoring
 */
fun startPerformanceMonitoring()

/**
 * Generate performance report
 * @return CompletableFuture<PerformanceReport>
 */
fun generatePerformanceReport(): CompletableFuture<PerformanceReport>
```

---

## Integration Guidelines

### Manager Lifecycle

All managers follow a consistent lifecycle pattern:

1. **Creation**: Instantiate with required dependencies
2. **Initialization**: Call `initialize()` method
3. **Operation**: Use public API methods
4. **Cleanup**: Call `cleanup()` method in onDestroy()

### Error Handling

All managers implement consistent error handling:
- CompletableFuture for asynchronous operations
- Specific exception types for different error conditions
- Automatic retry for transient errors
- Detailed error logging for debugging

### Thread Safety

All managers are designed to be thread-safe:
- Concurrent access protection using appropriate synchronization
- Thread-safe data structures for shared state
- Atomic operations for counters and flags

### Performance Considerations

- Minimal memory allocations in hot paths
- Efficient data structures for high-throughput operations  
- Background thread processing for expensive operations
- Lazy initialization of heavyweight resources

---

## Migration Guide

For existing code using the monolithic IRThermalNightActivity:

### Before (Monolithic)
```kotlin
class IRThermalNightActivity {
    // 3,324 lines of mixed responsibilities
    
    private fun initializeCamera() { /* 500+ lines */ }
    private fun updateUI() { /* 800+ lines */ }
    private fun manageConfiguration() { /* 400+ lines */ }
}
```

### After (Manager Pattern)
```kotlin
class IRThermalNightActivity {
    private lateinit var cameraManager: ThermalCameraManager
    private lateinit var uiStateManager: ThermalUIStateManager
    private lateinit var configurationManager: ThermalConfigurationManager
    
    private fun initializeManagers() {
        cameraManager = ThermalCameraManager(this, this)
        uiStateManager = ThermalUIStateManager(this, binding)
        configurationManager = ThermalConfigurationManager(this)
        
        // Initialize all managers
        CompletableFuture.allOf(
            cameraManager.initialize(config),
            configurationManager.loadConfiguration()
        ).thenRun {
            uiStateManager.initialize()
        }
    }
}
```

---

## Best Practices

1. **Single Responsibility**: Each manager handles one specific domain
2. **Dependency Injection**: Pass dependencies through constructor
3. **Async Operations**: Use CompletableFuture for long-running operations
4. **Resource Management**: Properly cleanup resources in lifecycle methods
5. **Error Handling**: Handle errors gracefully with appropriate user feedback
6. **Testing**: Each manager has comprehensive unit test coverage
7. **Performance**: Monitor and optimize based on real-world usage patterns

---

*Generated: 2025-01-23*  
*Version: 1.0*  
*Part of BucikaGSR Quality Improvement Initiative*