# Troubleshooting Guide

## Overview

Common issues and solutions for the BucikaGSR platform covering both hardware and software components.

## General Issues

### Build and Configuration Issues

#### Gradle Build Failures
```bash
# Clean and rebuild
./gradlew clean build

# Check for dependency conflicts
./gradlew dependencies --configuration implementation
```

#### Android Studio Setup Issues
- Ensure JDK 17 is configured in File → Project Structure
- Verify Android SDK path in SDK Manager
- Check Kotlin plugin version compatibility

## Hardware-Specific Issues

### GSR (Shimmer3) Issues

#### Bluetooth Connection Issues
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
```

**Solutions**:
- Verify Bluetooth permissions in AndroidManifest.xml
- Check device pairing status in Android settings
- Ensure Shimmer device is powered on and in range

#### Data Quality Issues
- **Low signal quality**: Check electrode placement and skin contact
- **Missing samples**: Verify sampling rate configuration (128Hz)
- **High noise**: Check for electromagnetic interference

### Thermal Camera (TC001) Issues

#### USB Connection Issues
```xml
<!-- Ensure USB permissions in AndroidManifest.xml -->
<uses-permission android:name="android.permission.USB_PERMISSION" />
<uses-feature android:name="android.hardware.usb.host" />
```

**Solutions**:
- Grant USB permission when prompted
- Check USB OTG cable connectivity
- Verify TC001 firmware version compatibility

#### Performance Issues
- **Low frame rate**: Check USB 3.0 connection
- **Processing lag**: Optimize OpenCV operations
- **Memory issues**: Monitor thermal frame buffer usage

## PC Orchestrator Issues

### Network and Discovery Issues

#### mDNS Service Not Found
```bash
# Check mDNS service broadcasting
avahi-browse -a | grep bucika-gsr

# Manual service registration
./gradlew :pc:run --args="--mdns-debug"
```

#### WebSocket Connection Issues  
- Check firewall settings on port 8080
- Verify network connectivity between devices
- Monitor connection logs for timeout issues

### Session Management Issues

#### Device Registration Failures
- Verify device ID uniqueness
- Check session capacity limits
- Monitor device heartbeat signals

#### Data Synchronization Issues
- Validate NTP/time sync accuracy
- Check timestamp alignment across devices
- Monitor for clock drift over long sessions

## Development Issues

### IDE and Environment Issues

#### Android Studio Performance
```bash
# Increase Android Studio memory
echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g" >> gradle.properties
```

#### Git and Version Control
```bash
# Fix line ending issues
git config core.autocrlf input

# Reset development environment
./setup-dev.sh
```

## Debug Configuration

### Logging Configuration
```kotlin
// Enable debug logging
XLog.init(
    LogConfiguration.Builder()
        .logLevel(LogLevel.ALL)
        .tag("BucikaGSR")
        .enableThreadInfo()
        .enableStackTrace(2)
        .build()
)
```

### Performance Monitoring
- Use Android Studio Profiler for memory and CPU analysis
- Monitor thermal processing frame rates
- Track GSR data streaming performance

## Getting Help

For additional support:
1. Check existing GitHub issues
2. Review architecture documentation in [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)  
3. Consult hardware integration guide in [HARDWARE_INTEGRATION.md](HARDWARE_INTEGRATION.md)