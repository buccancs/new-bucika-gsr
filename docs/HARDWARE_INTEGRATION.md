# Hardware Integration Guide

## Overview

This guide covers the integration of specific hardware components with the BucikaGSR platform.

## Shimmer3 GSR+ Integration

### Setup Requirements
- Android device with Bluetooth 4.0+
- Shimmer3 GSR+ sensor with charged battery
- Shimmer Console software for initial configuration

### Configuration Steps

1. **Sensor Pairing**
   ```kotlin
   // Bluetooth pairing in GSRManager
   private fun connectToShimmer(bluetoothAddress: String) {
       shimmerDevice = Shimmer(this)
       shimmerDevice.connect(bluetoothAddress, "default")
   }
   ```

2. **Data Collection Setup**
   ```kotlin
   // Configure sampling rate and sensors
   shimmerDevice.writeGSRRange(0) // ±40µS range
   shimmerDevice.setSamplingRateShimmer(128.0) // 128Hz
   shimmerDevice.enableSensors(SENSOR_GSR)
   ```

## Topdon TC001 Thermal Camera Integration

### Prerequisites
- Android device with USB OTG support
- TC001 thermal camera
- USB OTG cable

### USB Configuration

Add to AndroidManifest.xml:
```xml
<uses-feature android:name="android.hardware.usb.host" />
<uses-permission android:name="android.permission.USB_PERMISSION" />
```

### OpenCV Integration
```cmake
# CMakeLists.txt for thermal processing
find_package(OpenCV REQUIRED)
target_link_libraries(thermal-processing ${OpenCV_LIBS})
```

### Performance Optimization
```kotlin
// Thermal frame processing optimization
class ThermalProcessor {
    companion object {
        init {
            if (!OpenCVLoaderCallback.OpenCVCallbackInterface.SUCCESS) {
                OpenCVLoaderCallback.initDebug()
            }
        }
    }
}
```

## Troubleshooting

### Common GSR Issues
- **Connection timeout**: Check Bluetooth permissions and device pairing
- **Data quality**: Verify sensor placement and skin conductance

### Common TC001 Issues  
- **USB permission**: Grant USB access in Android settings
- **Frame rate issues**: Check USB 3.0 connection and processing optimization