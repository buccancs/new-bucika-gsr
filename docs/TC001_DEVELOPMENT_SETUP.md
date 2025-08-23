# TC001 Development Setup Guide

## Overview

This comprehensive guide provides step-by-step instructions for setting up a development environment for the Topdon TC001 thermal imaging integration within the BucikaGSR system. This guide covers everything from initial environment setup to advanced debugging and performance optimization.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Development Environment Setup](#development-environment-setup)
3. [Project Configuration](#project-configuration)
4. [USB Development Setup](#usb-development-setup)
5. [OpenCV Integration](#opencv-integration)
6. [Testing Framework](#testing-framework)
7. [Debugging Tools](#debugging-tools)
8. [Performance Optimization](#performance-optimization)
9. [Deployment Configuration](#deployment-configuration)
10. [Troubleshooting](#troubleshooting)

## Prerequisites

### Hardware Requirements

#### Development Hardware
- **Development Machine**: Intel/AMD x64 processor with 16GB+ RAM
- **Android Device**: Android 7.0+ (API 24+) with USB Host support
- **Topdon TC001 Device**: TC001 thermal imaging camera
- **USB OTG Cable**: For connecting TC001 to Android device

#### Recommended Specifications
```
CPU: Intel i7/AMD Ryzen 7 or equivalent
RAM: 32GB DDR4 (for optimal Android Studio performance)
Storage: 500GB+ SSD (for fast build times)
GPU: Dedicated graphics card (for OpenCV acceleration)
```

### Software Requirements

#### Core Development Tools
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: OpenJDK 11 or Oracle JDK 11
- **Android SDK**: API levels 24-33
- **NDK**: Version 23.1.7779620 or later
- **Git**: Version 2.30 or later

#### Additional Tools
- **OpenCV Android SDK**: Version 4.5.0+
- **USB debugging tools**: `adb`, `fastboot`
- **Thermal analysis tools**: FLIR Tools, IRSoft (optional)

## Development Environment Setup

### Step 1: Android Studio Installation

#### Download and Install Android Studio
```bash
# Download Android Studio from official website
# https://developer.android.com/studio

# Linux installation
sudo snap install android-studio --classic

# Or manual installation
wget https://dl.google.com/dl/android/studio/ide-zips/2021.3.1.17/android-studio-2021.3.1.17-linux.tar.gz
tar -xzf android-studio-*.tar.gz
cd android-studio/bin
./studio.sh
```

#### Configure Android Studio
```kotlin
// studio.vmoptions configuration for optimal performance
-Xms2048m
-Xmx8192m
-XX:ReservedCodeCacheSize=1024m
-XX:+UseConcMarkSweepGC
-XX:SoftRefLRUPolicyMSPerMB=50
-Dsun.io.useCanonPrefixCache=false
-Djdk.http.auth.tunneling.disabledSchemes=""
-Djdk.attach.allowAttachSelf=true
-Djb.vmOptionsFile=$USER_HOME/.AndroidStudio/studio.vmoptions
```

### Step 2: SDK and NDK Setup

#### Android SDK Configuration
```bash
# Set environment variables (add to ~/.bashrc)
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Install required SDK packages
sdkmanager "platforms;android-24"
sdkmanager "platforms;android-28"
sdkmanager "platforms;android-30"
sdkmanager "platforms;android-33"
sdkmanager "build-tools;30.0.3"
sdkmanager "build-tools;33.0.0"
sdkmanager "extras;android;m2repository"
sdkmanager "extras;google;m2repository"
```

#### NDK Configuration
```bash
# Install NDK
sdkmanager "ndk;23.1.7779620"

# Set NDK environment variables
export ANDROID_NDK_ROOT=$ANDROID_HOME/ndk/23.1.7779620
export PATH=$PATH:$ANDROID_NDK_ROOT
```

### Step 3: Project Repository Setup

#### Clone Project Repository
```bash
# Clone the BucikaGSR repository
git clone https://github.com/buccancs/new-bucika-gsr.git
cd new-bucika-gsr

# Setup git hooks for development
cp .githooks/* .git/hooks/
chmod +x .git/hooks/*
```

#### Configure Development Branch
```bash
# Create development branch
git checkout -b feature/tc001-development
git push -u origin feature/tc001-development

# Setup upstream tracking
git remote add upstream https://github.com/buccancs/new-bucika-gsr.git
git fetch upstream
```

## Project Configuration

### Step 4: Gradle Configuration

#### Root build.gradle Configuration
```groovy
// Top-level build file
buildscript {
    ext.kotlin_version = "1.7.10"
    ext.opencv_version = "4.5.0"
    
    dependencies {
        classpath "com.android.tools.build:gradle:7.2.2"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### App Module build.gradle
```groovy
android {
    compileSdk 33
    
    defaultConfig {
        applicationId "com.topdon.tc001"
        minSdk 24
        targetSdk 33
        versionCode 1
        versionName "1.0"
        
        ndk {
            abiFilters "arm64-v8a", "armeabi-v7a"
        }
        
        // Enable USB host support
        manifestPlaceholders = [
            USB_HOST_REQUIRED: "true"
        ]
    }
    
    buildFeatures {
        viewBinding true
        dataBinding true
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = '11'
    }
    
    packagingOptions {
        pickFirst '**/libc++_shared.so'
        pickFirst '**/libjsc.so'
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/LICENSE'
        exclude 'META-INF/LICENSE.txt'
        exclude 'META-INF/NOTICE'
        exclude 'META-INF/NOTICE.txt'
    }
}

dependencies {
    // Core Android dependencies
    implementation "androidx.core:core-ktx:1.8.0"
    implementation "androidx.appcompat:appcompat:1.5.0"
    implementation "com.google.android.material:material:1.6.1"
    implementation "androidx.constraintlayout:constraintlayout:2.1.4"
    
    // USB and hardware support
    implementation "androidx.core:core:1.8.0"
    
    // OpenCV for image processing
    implementation project(':opencv')
    
    // Thermal imaging libraries
    implementation project(':libir')
    implementation project(':commonlibrary')
    
    // Testing dependencies
    testImplementation "junit:junit:4.13.2"
    testImplementation "org.mockito:mockito-core:4.6.1"
    testImplementation "org.robolectric:robolectric:4.8.1"
    
    androidTestImplementation "androidx.test.ext:junit:1.1.3"
    androidTestImplementation "androidx.test.espresso:espresso-core:3.4.0"
}
```

### Step 5: LibIR Module Configuration

#### LibIR build.gradle
```groovy
android {
    compileSdk 33
    
    defaultConfig {
        minSdk 24
        targetSdk 33
        
        consumerProguardFiles "consumer-rules.pro"
        
        ndk {
            abiFilters "arm64-v8a", "armeabi-v7a"
        }
        
        externalNativeBuild {
            cmake {
                cppFlags "-std=c++14"
                arguments "-DANDROID_STL=c++_shared"
            }
        }
    }
    
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.18.1"
        }
    }
    
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    api project(':opencv')
    api "androidx.core:core-ktx:1.8.0"
    
    // USB communication
    api "com.github.saki4510t:UVCCamera:2.12.4"
    
    // Image processing
    api "org.bytedeco:javacv-platform:1.5.6"
}
```

## USB Development Setup

### Step 6: USB Host Configuration

#### AndroidManifest.xml Setup
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.topdon.tc001">

    <!-- USB Host permissions -->
    <uses-permission android:name="android.permission.USB_PERMISSION" />
    <uses-feature
        android:name="android.hardware.usb.host"
        android:required="true" />

    <!-- Camera and storage permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    <application
        android:name=".app.App"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <!-- Main Activity with USB device filter -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            
            <!-- USB device attached intent filter -->
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>
            
            <meta-data
                android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
                android:resource="@xml/device_filter" />
        </activity>

        <!-- Thermal Activity -->
        <activity
            android:name=".ThermalActivity"
            android:screenOrientation="landscape"
            android:theme="@style/AppTheme.FullScreen" />

    </application>
</manifest>
```

#### USB Device Filter Configuration
```xml
<!-- res/xml/device_filter.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Topdon TC001 USB device filter -->
    <usb-device 
        vendor-id="1234" 
        product-id="5678" />
    
    <!-- Alternative product IDs for different TC001 variants -->
    <usb-device 
        vendor-id="1234" 
        product-id="5679" />
</resources>
```

### Step 7: USB Debugging Setup

#### ADB Development Configuration
```bash
# Enable USB debugging on Android device
# Settings > Developer Options > USB Debugging

# Verify device connection
adb devices

# Install debug APK
adb install -r app-debug.apk

# View USB devices from Android device
adb shell dumpsys usb

# Monitor USB events
adb logcat | grep -i usb

# Check thermal camera connection
adb logcat | grep -i "TC001\|thermal\|IRUVC"
```

#### USB Development Scripts
```bash
#!/bin/bash
# scripts/usb-debug.sh

echo "=== USB Development Debug Script ==="

# Check ADB connection
echo "Checking ADB connection..."
adb devices

# Check USB host support
echo "Checking USB host support..."
adb shell cat /proc/version
adb shell ls /dev/bus/usb/

# Monitor USB events in real-time
echo "Monitoring USB events (Ctrl+C to stop)..."
adb logcat -c  # Clear log
adb logcat | grep -E "(USB|TC001|thermal)" --line-buffered
```

## OpenCV Integration

### Step 8: OpenCV Module Setup

#### OpenCV Module Configuration
```gradle
// opencv/build.gradle
apply plugin: 'com.android.library'

android {
    compileSdk 33
    
    defaultConfig {
        minSdk 24
        targetSdk 33
    }
    
    sourceSets {
        main {
            jniLibs.srcDirs = ['src/main/jniLibs']
            java.srcDirs = ['src/main/java']
        }
    }
}
```

#### Download and Setup OpenCV
```bash
#!/bin/bash
# scripts/setup-opencv.sh

OPENCV_VERSION="4.5.0"
OPENCV_ANDROID_SDK="opencv-${OPENCV_VERSION}-android-sdk.zip"

echo "Downloading OpenCV Android SDK..."
wget "https://github.com/opencv/opencv/releases/download/${OPENCV_VERSION}/${OPENCV_ANDROID_SDK}"

echo "Extracting OpenCV SDK..."
unzip -q "${OPENCV_ANDROID_SDK}"

echo "Setting up OpenCV module..."
cp -r "OpenCV-android-sdk/sdk" "opencv/"

echo "Copying native libraries..."
mkdir -p "opencv/src/main/jniLibs"
cp -r "OpenCV-android-sdk/sdk/native/libs/"* "opencv/src/main/jniLibs/"

echo "Copying Java sources..."
mkdir -p "opencv/src/main/java"
cp -r "OpenCV-android-sdk/sdk/java/src/"* "opencv/src/main/java/"

echo "OpenCV setup complete!"
```

### Step 9: Native Code Configuration

#### CMakeLists.txt for Native Components
```cmake
# libir/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.18.1)

project("thermal_native")

# Configure OpenCV
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/../../../../opencv/src/main/cpp)
find_package(OpenCV REQUIRED)

# Include directories
include_directories(${OpenCV_INCLUDE_DIRS})
include_directories(src/main/cpp/include)

# Source files
file(GLOB THERMAL_SOURCES
    "src/thermal/*.cpp"
    "src/processing/*.cpp"
    "src/usb/*.cpp"
)

# Create shared library
add_library(thermal_native SHARED ${THERMAL_SOURCES})

# Link libraries
target_link_libraries(thermal_native
    ${OpenCV_LIBS}
    android
    log
    camera2ndk
    mediandk
)

# Compiler flags
target_compile_options(thermal_native PRIVATE
    -std=c++14
    -frtti
    -fexceptions
    -O2
    -DANDROID
)
```

## Testing Framework

### Step 10: Testing Configuration

#### Unit Testing Setup
```kotlin
// app/src/test/java/com/topdon/tc001/thermal/ThermalTestSuite.kt
@RunWith(Suite::class)
@Suite.SuiteClasses(
    ThermalCameraTest::class,
    TemperatureViewTest::class,
    ThermalDataWriterTest::class,
    OpencvToolsTest::class,
    ThermalCalibrationTest::class,
    USBConnectionTest::class
)
class ThermalTestSuite

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThermalCameraTest {
    
    private lateinit var context: Context
    private lateinit var thermalCamera: IRUVCTC
    private lateinit var mockFrameCallback: IFrameCallback
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockFrameCallback = Mockito.mock(IFrameCallback::class.java)
        thermalCamera = IRUVCTC(context, mockFrameCallback)
    }
    
    @Test
    fun testThermalCameraInitialization() {
        assertNotNull("Thermal camera should be initialized", thermalCamera)
        assertFalse("Camera should not be connected initially", thermalCamera.isConnected())
    }
    
    @Test
    fun testUSBDeviceConnection() {
        // Mock USB device
        val mockDevice = Mockito.mock(UsbDevice::class.java)
        Mockito.`when`(mockDevice.vendorId).thenReturn(1234)
        Mockito.`when`(mockDevice.productId).thenReturn(5678)
        
        // Test connection
        val result = thermalCamera.connectUSBDevice(mockDevice)
        assertTrue("Connection should be initiated", result)
    }
    
    @Test
    fun testTemperatureCalculation() {
        // Test temperature conversion
        val temperatureData = ByteArray(256 * 192 * 2)
        // Fill with test data representing 25°C
        val rawValue = ((25.0f + 273.15f) * 64.0f).toInt()
        
        for (i in temperatureData.indices step 2) {
            temperatureData[i] = (rawValue and 0xFF).toByte()
            temperatureData[i + 1] = ((rawValue shr 8) and 0xFF).toByte()
        }
        
        val temperature = thermalCamera.getTemperatureAt(128, 96)
        assertEquals("Temperature should be approximately 25°C", 25.0f, temperature, 0.5f)
    }
}
```

#### Integration Testing Setup
```kotlin
// app/src/androidTest/java/com/topdon/tc001/thermal/ThermalIntegrationTest.kt
@RunWith(AndroidJUnit4::class)
class ThermalIntegrationTest {
    
    @get:Rule
    val activityRule = ActivityTestRule(ThermalActivity::class.java)
    
    @Test
    fun testThermalActivityLaunch() {
        // Verify thermal activity launches successfully
        onView(withId(R.id.temperature_view))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testUSBPermissionDialog() {
        // Test USB permission request flow
        val usbManager = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSystemService(Context.USB_SERVICE) as UsbManager
        
        // Verify permission dialog behavior
        onView(withText("USB Permission"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testThermalDataRecording() {
        // Test thermal data recording functionality
        onView(withId(R.id.record_button))
            .perform(click())
        
        // Verify recording status
        onView(withId(R.id.status_text))
            .check(matches(withText(containsString("Recording"))))
        
        // Stop recording
        onView(withId(R.id.record_button))
            .perform(click())
    }
}
```

### Step 11: Performance Testing

#### Performance Test Configuration
```kotlin
// app/src/test/java/com/topdon/tc001/thermal/ThermalPerformanceTest.kt
class ThermalPerformanceTest {
    
    @Test
    fun testThermalFrameProcessingPerformance() {
        val frameData = generateTestThermalFrame()
        val processingTimes = mutableListOf<Long>()
        
        // Test frame processing performance
        repeat(100) {
            val startTime = System.nanoTime()
            
            // Process thermal frame
            val bitmap = OpencvTools.processRawThermalData(frameData)
            val temperature = OpencvTools.calculateAverageTemperature(frameData)
            
            val endTime = System.nanoTime()
            processingTimes.add(endTime - startTime)
        }
        
        val averageTime = processingTimes.average() / 1_000_000.0 // Convert to ms
        assertTrue("Frame processing should be under 40ms", averageTime < 40.0)
        
        println("Average frame processing time: ${averageTime}ms")
    }
    
    @Test
    fun testMemoryUsageMonitoring() {
        val runtime = Runtime.getRuntime()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Process multiple thermal frames
        repeat(1000) {
            val frameData = generateTestThermalFrame()
            val bitmap = OpencvTools.processRawThermalData(frameData)
            // Don't forget to recycle bitmaps in real usage
            bitmap.recycle()
        }
        
        System.gc() // Force garbage collection
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryDelta = memoryAfter - memoryBefore
        
        assertTrue("Memory usage should not increase significantly", 
                   memoryDelta < 10 * 1024 * 1024) // 10MB threshold
    }
    
    private fun generateTestThermalFrame(): ByteArray {
        val frameData = ByteArray(256 * 192 * 2)
        val random = Random(42) // Fixed seed for consistent tests
        
        for (i in frameData.indices step 2) {
            val temperature = 20.0f + random.nextFloat() * 60.0f // 20-80°C range
            val rawValue = ((temperature + 273.15f) * 64.0f).toInt()
            
            frameData[i] = (rawValue and 0xFF).toByte()
            frameData[i + 1] = ((rawValue shr 8) and 0xFF).toByte()
        }
        
        return frameData
    }
}
```

## Debugging Tools

### Step 12: Debug Configuration

#### Logging Configuration
```kotlin
// app/src/main/java/com/topdon/tc001/utils/ThermalLogger.kt
object ThermalLogger {
    private const val TAG = "ThermalDebug"
    
    var isDebugEnabled = BuildConfig.DEBUG
    
    fun d(message: String) {
        if (isDebugEnabled) {
            Log.d(TAG, message)
        }
    }
    
    fun i(message: String) {
        Log.i(TAG, message)
    }
    
    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
    
    fun logUSBDevice(device: UsbDevice) {
        d("USB Device Info:")
        d("  Vendor ID: 0x${Integer.toHexString(device.vendorId)}")
        d("  Product ID: 0x${Integer.toHexString(device.productId)}")
        d("  Device Name: ${device.deviceName}")
        d("  Device Class: ${device.deviceClass}")
        d("  Interface Count: ${device.interfaceCount}")
    }
    
    fun logThermalFrame(timestamp: Long, tempData: ByteArray) {
        if (isDebugEnabled) {
            val minTemp = findMinTemperature(tempData)
            val maxTemp = findMaxTemperature(tempData)
            val avgTemp = calculateAverageTemperature(tempData)
            
            d("Thermal Frame @ $timestamp:")
            d("  Min: ${minTemp}°C")
            d("  Max: ${maxTemp}°C") 
            d("  Avg: ${avgTemp}°C")
        }
    }
    
    private fun findMinTemperature(tempData: ByteArray): Float {
        var min = Float.MAX_VALUE
        for (i in tempData.indices step 2) {
            val rawValue = (tempData[i + 1].toInt() shl 8) or (tempData[i].toInt() and 0xFF)
            val temperature = (rawValue / 64.0f) - 273.15f
            min = kotlin.math.min(min, temperature)
        }
        return min
    }
    
    private fun findMaxTemperature(tempData: ByteArray): Float {
        var max = Float.MIN_VALUE
        for (i in tempData.indices step 2) {
            val rawValue = (tempData[i + 1].toInt() shl 8) or (tempData[i].toInt() and 0xFF)
            val temperature = (rawValue / 64.0f) - 273.15f
            max = kotlin.math.max(max, temperature)
        }
        return max
    }
    
    private fun calculateAverageTemperature(tempData: ByteArray): Float {
        var sum = 0.0f
        var count = 0
        
        for (i in tempData.indices step 2) {
            val rawValue = (tempData[i + 1].toInt() shl 8) or (tempData[i].toInt() and 0xFF)
            val temperature = (rawValue / 64.0f) - 273.15f
            sum += temperature
            count++
        }
        
        return sum / count
    }
}
```

#### Debug Activity for Development
```kotlin
// app/src/debug/java/com/topdon/tc001/DebugActivity.kt
class DebugActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDebugBinding
    private var thermalCamera: IRUVCTC? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupDebugControls()
    }
    
    private fun setupDebugControls() {
        // USB device enumeration
        binding.btnEnumerateUsb.setOnClickListener {
            enumerateUSBDevices()
        }
        
        // Test thermal frame generation
        binding.btnGenerateTestFrame.setOnClickListener {
            generateTestThermalFrame()
        }
        
        // Memory usage monitoring
        binding.btnMemoryUsage.setOnClickListener {
            showMemoryUsage()
        }
        
        // Performance benchmarking
        binding.btnRunBenchmark.setOnClickListener {
            runPerformanceBenchmark()
        }
    }
    
    private fun enumerateUSBDevices() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        
        val debugInfo = StringBuilder()
        debugInfo.append("USB Devices Found: ${deviceList.size}\n\n")
        
        deviceList.values.forEach { device ->
            debugInfo.append("Device: ${device.deviceName}\n")
            debugInfo.append("Vendor ID: 0x${Integer.toHexString(device.vendorId)}\n")
            debugInfo.append("Product ID: 0x${Integer.toHexString(device.productId)}\n")
            debugInfo.append("Class: ${device.deviceClass}\n")
            debugInfo.append("Interfaces: ${device.interfaceCount}\n")
            debugInfo.append("Has Permission: ${usbManager.hasPermission(device)}\n")
            debugInfo.append("---\n")
        }
        
        binding.tvDebugOutput.text = debugInfo.toString()
    }
    
    private fun generateTestThermalFrame() {
        // Generate synthetic thermal data for testing
        val testData = ByteArray(256 * 192 * 2)
        val random = Random()
        
        for (i in testData.indices step 2) {
            val temperature = 20.0f + random.nextFloat() * 40.0f
            val rawValue = ((temperature + 273.15f) * 64.0f).toInt()
            
            testData[i] = (rawValue and 0xFF).toByte()
            testData[i + 1] = ((rawValue shr 8) and 0xFF).toByte()
        }
        
        // Process test frame
        val bitmap = OpencvTools.convertThermalToBitmap(testData, 256, 192)
        binding.ivThermalPreview.setImageBitmap(bitmap)
        
        ThermalLogger.logThermalFrame(System.currentTimeMillis(), testData)
    }
    
    private fun showMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        
        val memoryInfo = """
            Memory Usage:
            Used: ${usedMemory}MB
            Free: ${freeMemory}MB
            Total: ${totalMemory}MB
            Max: ${maxMemory}MB
        """.trimIndent()
        
        binding.tvDebugOutput.text = memoryInfo
    }
    
    private fun runPerformanceBenchmark() {
        val testData = generateLargeThermalDataset()
        val processingTimes = mutableListOf<Long>()
        
        repeat(100) {
            val startTime = System.nanoTime()
            OpencvTools.processRawThermalData(testData)
            val endTime = System.nanoTime()
            
            processingTimes.add(endTime - startTime)
        }
        
        val averageTime = processingTimes.average() / 1_000_000.0
        val minTime = processingTimes.minOrNull()?.div(1_000_000.0) ?: 0.0
        val maxTime = processingTimes.maxOrNull()?.div(1_000_000.0) ?: 0.0
        
        val benchmarkResults = """
            Performance Benchmark Results:
            Average: ${String.format("%.2f", averageTime)}ms
            Min: ${String.format("%.2f", minTime)}ms
            Max: ${String.format("%.2f", maxTime)}ms
            
            Frame Rate Capability: ${String.format("%.1f", 1000.0 / averageTime)} FPS
        """.trimIndent()
        
        binding.tvDebugOutput.text = benchmarkResults
    }
}
```

## Performance Optimization

### Step 13: Optimization Configuration

#### ProGuard Configuration
```proguard
# proguard-rules.pro

# Keep OpenCV classes
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Keep thermal imaging classes
-keep class com.infisense.usbir.** { *; }
-keep class com.energy.iruvc.** { *; }

# Keep USB classes
-keep class android.hardware.usb.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Optimize thermal processing
-optimizations !method/inlining/*
-keep class com.topdon.tc001.thermal.** { *; }

# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
```

#### R8 Configuration
```proguard
# R8 specific rules for thermal imaging
-keep,allowobfuscation,allowshrinking class com.topdon.tc001.thermal.**
-keep class com.topdon.tc001.thermal.** { *; }

# Preserve thermal data structures
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
```

## Deployment Configuration

### Step 14: Release Configuration

#### Gradle Release Configuration
```groovy
android {
    signingConfigs {
        debug {
            storeFile file('debug.keystore')
            keyAlias 'androiddebugkey'
            keyPassword 'android'
            storePassword 'android'
        }
        release {
            storeFile file('release.keystore')
            keyAlias 'release'
            keyPassword System.getenv("KEYSTORE_PASSWORD")
            storePassword System.getenv("KEYSTORE_PASSWORD")
        }
    }
    
    buildTypes {
        debug {
            debuggable true
            minifyEnabled false
            applicationIdSuffix ".debug"
            versionNameSuffix "-DEBUG"
            
            buildConfigField "boolean", "THERMAL_DEBUG", "true"
        }
        
        release {
            debuggable false
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),
                         'proguard-rules.pro'
            
            signingConfig signingConfigs.release
            buildConfigField "boolean", "THERMAL_DEBUG", "false"
        }
    }
}
```

#### Build Script
```bash
#!/bin/bash
# scripts/build-release.sh

set -e

echo "=== Building TC001 Thermal Release ==="

# Clean previous builds
./gradlew clean

# Run tests
echo "Running unit tests..."
./gradlew testDebugUnitTest

echo "Running integration tests..."
./gradlew connectedAndroidTest

# Build release APK
echo "Building release APK..."
./gradlew assembleRelease

# Generate proguard mapping
echo "Generating ProGuard mapping..."
cp app/build/outputs/mapping/release/mapping.txt release-mapping-$(date +%Y%m%d).txt

# Sign APK
echo "Signing release APK..."
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
    -keystore release.keystore \
    app/build/outputs/apk/release/app-release-unsigned.apk \
    release

# Align APK
echo "Aligning APK..."
zipalign -v 4 \
    app/build/outputs/apk/release/app-release-unsigned.apk \
    app/build/outputs/apk/release/TC001-Thermal-$(date +%Y%m%d).apk

echo "Release build complete!"
echo "APK location: app/build/outputs/apk/release/TC001-Thermal-$(date +%Y%m%d).apk"
```

## Troubleshooting

### Common Development Issues

#### Issue: OpenCV Loading Failed
```kotlin
// Solution: Check OpenCV initialization
if (!OpenCVLoaderCallback.initDebug()) {
    Log.e("OpenCV", "Unable to load OpenCV!")
    
    // Try manual OpenCV loading
    if (!OpenCVLoader.initDebug(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback)) {
        Log.e("OpenCV", "Cannot connect to OpenCV Manager");
    }
}
```

#### Issue: USB Permission Not Granted
```kotlin
// Solution: Implement proper permission handling
private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_USB_PERMISSION -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.let { connectThermalCamera(it) }
                } else {
                    Log.d("USB", "Permission denied for device $device")
                }
            }
        }
    }
}
```

#### Issue: Thermal Frame Processing Too Slow
```kotlin
// Solution: Optimize processing pipeline
class OptimizedThermalProcessor {
    private val processingExecutor = Executors.newSingleThreadExecutor()
    private val uiHandler = Handler(Looper.getMainLooper())
    
    fun processThermalFrame(frameData: ByteArray, callback: (Bitmap) -> Unit) {
        processingExecutor.execute {
            // Process on background thread
            val bitmap = OpencvTools.fastThermalProcessing(frameData)
            
            // Update UI on main thread
            uiHandler.post {
                callback(bitmap)
            }
        }
    }
}
```

This comprehensive development setup guide provides all necessary information for setting up a complete TC001 thermal imaging development environment within the BucikaGSR system.