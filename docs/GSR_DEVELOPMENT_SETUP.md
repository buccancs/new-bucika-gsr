# GSR Development Setup Guide

## Overview

This guide provides comprehensive setup instructions for developing and testing the BucikaGSR ShimmerAndroidAPI integration.

## Prerequisites

### Required Software
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **Android SDK**: API level 29 (Android 10) or higher
- **Java**: OpenJDK 17 or Oracle JDK 17
- **Kotlin**: 1.8.0 or later
- **Gradle**: 7.4.2 or later

### Hardware Requirements
- **Development Device**: Android 8.0+ with Bluetooth 4.0+
- **Shimmer3 GSR+ Device**: For hardware testing (optional - simulator available)
- **Minimum RAM**: 8GB (16GB recommended for optimal performance)
- **Storage**: 10GB free space

## Project Setup

### 1. Clone and Configure Repository

```bash
git clone <repository-url>
cd new-bucika-gsr
git checkout main
```

### 2. Android Studio Configuration

1. **Open Project**:
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned repository directory

2. **SDK Configuration**:
   ```gradle
   android {
       compileSdk 34
       defaultConfig {
           minSdk 26
           targetSdk 34
       }
   }
   ```

3. **Sync Project**:
   - Click "Sync Project with Gradle Files"
   - Wait for dependency resolution to complete

### 3. Build Configuration

The project uses multiple build variants:
- **dev**: Development build with debug features
- **beta**: Beta testing build
- **prod**: Production release build

```bash
# Build development version
./gradlew assembleDevDebug

# Run tests
./gradlew testDevDebugUnitTest
./gradlew connectedDevDebugAndroidTest
```

## Development Environment Setup

### 1. Enable Developer Options

Enable developer options on your Android device:
1. Go to Settings → About phone
2. Tap "Build number" 7 times
3. Return to Settings → System → Developer options
4. Enable "USB debugging"
5. Enable "Stay awake"

### 2. Bluetooth Permissions

Add required permissions to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

### 3. File System Permissions

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

## Testing Framework Setup

### 1. Unit Testing Dependencies

The project includes comprehensive testing dependencies:
```gradle
dependencies {
    // Unit testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.11.0'
    testImplementation 'org.mockito:mockito-inline:4.11.0'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:4.1.0'
    testImplementation 'org.robolectric:robolectric:4.10.3'
    
    // Integration testing
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
}
```

### 2. Running Tests

#### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew testDevDebugUnitTest --tests="GSRManagerTest"

# Run with coverage
./gradlew testDevDebugUnitTest jacocoTestReport
```

#### Integration Tests
```bash
# Run all integration tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.topdon.tc001.gsr.GSRIntegrationTest
```

### 3. Test Configuration

Create `test-config.properties` in the project root:
```properties
# Test Shimmer device configuration
test.shimmer.address=00:11:22:33:44:55
test.shimmer.name=TestShimmer3-GSR+
test.sampling.rate=128
test.data.directory=/data/data/com.topdon.tc001/files/gsr_test_data
```

## Development Workflow

### 1. Feature Development

1. **Create Feature Branch**:
   ```bash
   git checkout -b feature/new-gsr-feature
   ```

2. **Implement Changes**:
   - Follow existing code patterns
   - Add comprehensive logging
   - Include unit tests for new functionality

3. **Testing**:
   ```bash
   # Run linting
   ./gradlew lint
   
   # Run unit tests
   ./gradlew testDevDebugUnitTest
   
   # Run integration tests
   ./gradlew connectedDevDebugAndroidTest
   ```

### 2. Code Style Guidelines

#### Kotlin Conventions
```kotlin
// Class naming: PascalCase
class GSRDataProcessor {
    
    // Function naming: camelCase
    fun processGSRData(data: GSRData): ProcessedData {
        // Implementation
    }
    
    // Constants: UPPER_SNAKE_CASE
    companion object {
        private const val DEFAULT_SAMPLING_RATE = 128.0
        private const val TAG = "GSRDataProcessor"
    }
}
```

#### Logging Standards
```kotlin
import com.elvishew.xlog.XLog

class GSRManager {
    companion object {
        private const val TAG = "GSRManager"
    }
    
    fun connectToDevice(address: String, name: String): Boolean {
        XLog.d(TAG, "Attempting to connect to device: $name at $address")
        
        return try {
            // Connection logic
            XLog.i(TAG, "Successfully connected to device: $name")
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to connect to device: $name", e)
            false
        }
    }
}
```

### 3. Documentation Standards

#### Code Documentation
```kotlin
/**
 * Manages GSR data collection and device connectivity for Shimmer3 GSR+ devices.
 * 
 * This class provides comprehensive GSR data collection capabilities including:
 * - Device connection management
 * - Real-time data streaming at configurable sampling rates
 * - Advanced signal processing and quality assessment
 * - Multi-listener support for flexible data handling
 * 
 * Usage example:
 * ```kotlin
 * val gsrManager = GSRManager.getInstance(context)
 * gsrManager.setGSRDataListener { timestamp, gsrValue, temperature ->
 *     // Handle GSR data
 * }
 * gsrManager.connectToDevice("00:11:22:33:44:55", "Shimmer3-GSR+")
 * ```
 * 
 * @constructor Creates a new GSRManager instance (use getInstance() instead)
 * @param context Application context for system service access
 * 
 * @see ShimmerBluetooth for low-level device communication
 * @see GSRDataWriter for data persistence and export
 */
class GSRManager private constructor(private val context: Context) {
    // Implementation
}
```

## Hardware Integration

### 1. Shimmer3 GSR+ Setup

1. **Device Configuration**:
   - Install Shimmer Android SDK (simulation provided)
   - Configure device for GSR + Temperature sensors
   - Set appropriate sampling rate (recommended: 128 Hz)

2. **Connection Testing**:
   ```kotlin
   // Test device discovery
   val discoveredDevices = shimmerBluetooth.discoverDevices()
   discoveredDevices.forEach { device ->
       XLog.d("DeviceDiscovery", "Found device: ${device.name} at ${device.address}")
   }
   ```

### 2. Data Validation

```kotlin
class GSRDataValidator {
    fun validateGSRData(gsrValue: Double, temperature: Double): Boolean {
        return gsrValue in 100.0..2000.0 && temperature in 25.0..40.0
    }
    
    fun assessSignalQuality(data: List<GSRDataPoint>): Double {
        val variance = calculateVariance(data.map { it.gsrValue })
        return when {
            variance < 100 -> 0.9 // High quality
            variance < 500 -> 0.7 // Good quality
            variance < 1000 -> 0.5 // Acceptable
            else -> 0.3 // Poor quality
        }
    }
}
```

## Debugging and Diagnostics

### 1. Logging Configuration

Configure XLog for comprehensive debugging:
```kotlin
// Application class initialization
class GSRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        XLog.init(
            LogConfiguration.Builder()
                .logLevel(LogLevel.ALL)
                .tag("BucikaGSR")
                .enableThreadInfo()
                .enableStackTrace(2)
                .build(),
            AndroidPrinter(),
            FilePrinter.Builder("${filesDir}/logs")
                .fileNameGenerator(DateFileNameGenerator())
                .build()
        )
    }
}
```

### 2. Performance Monitoring

```kotlin
class PerformanceMonitor {
    fun monitorMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        XLog.d("Performance", "Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB")
    }
    
    fun measureDataProcessingTime(operation: () -> Unit): Long {
        val startTime = System.nanoTime()
        operation()
        val endTime = System.nanoTime()
        return (endTime - startTime) / 1_000_000 // Convert to milliseconds
    }
}
```

### 3. Common Issues and Solutions

#### Bluetooth Connection Issues
```kotlin
// Check Bluetooth availability
if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
    requestBluetoothEnable()
}

// Verify permissions
if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
    != PackageManager.PERMISSION_GRANTED) {
    requestBluetoothPermissions()
}
```

#### Data Quality Issues
```kotlin
// Monitor signal quality continuously
gsrManager.setAdvancedDataListener { timestamp, gsrValue, temperature, arousal, indicators, quality ->
    if (quality < 0.5) {
        XLog.w("DataQuality", "Poor signal quality detected: $quality")
        showSignalQualityWarning()
    }
}
```

## Deployment

### 1. Build Variants

```bash
# Development build
./gradlew assembleDevDebug

# Beta testing build
./gradlew assembleBetaRelease

# Production build
./gradlew assembleProdRelease
```

### 2. Testing Checklist

Before deployment, verify:
- [ ] All unit tests pass
- [ ] Integration tests complete successfully
- [ ] UI tests validate user interactions
- [ ] Performance benchmarks meet requirements
- [ ] Memory usage remains within acceptable limits
- [ ] File system operations work correctly
- [ ] Bluetooth connectivity functions properly
- [ ] Data export generates valid files

### 3. Release Configuration

```gradle
android {
    signingConfigs {
        release {
            storeFile file('release-keystore.jks')
            keyAlias 'release-key'
            storePassword project.hasProperty('KEYSTORE_PASSWORD') ? KEYSTORE_PASSWORD : ''
            keyPassword project.hasProperty('KEY_PASSWORD') ? KEY_PASSWORD : ''
        }
    }
    
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
        }
    }
}
```

## Continuous Integration

### 1. GitHub Actions Configuration

```yaml
name: GSR CI/CD
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'adopt'
      
      - name: Run Unit Tests
        run: ./gradlew testDevDebugUnitTest
      
      - name: Generate Test Report
        run: ./gradlew jacocoTestReport
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
```

### 2. Quality Gates

Set up quality gates for:
- **Test Coverage**: Minimum 80%
- **Code Quality**: No critical issues in SonarQube
- **Performance**: Memory usage < 100MB
- **Build Time**: < 5 minutes for debug builds

This setup guide ensures a robust development environment for the BucikaGSR ShimmerAndroidAPI integration with comprehensive testing, debugging, and deployment capabilities.