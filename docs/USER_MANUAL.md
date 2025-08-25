# User Manual - Bucika GSR Multi-Modal Data Collection Platform

## Table of Contents

1. [Introduction](#introduction)
2. [System Requirements](#system-requirements)
3. [Getting Started](#getting-started)
4. [PC Orchestrator Setup](#pc-orchestrator-setup)
5. [Android App Installation](#android-app-installation)
6. [First Recording Session](#first-recording-session)
7. [Advanced Features](#advanced-features)
8. [Data Management](#data-management)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)

---

## Introduction

Welcome to the Bucika GSR Multi-Modal Data Collection Platform! This system enables synchronized recording of physiological data using:

- **Galvanic Skin Response (GSR)** - Real-time stress and emotional arousal measurement
- **Thermal Imaging** - Non-contact temperature monitoring via TC001 thermal camera  
- **RGB Video** - High-quality video recording for behavioral analysis
- **Multi-Device Synchronization** - Coordinated recording across multiple Android devices

### Key Benefits

✅ **Research-Grade Quality**: Professional data validation and quality assurance  
✅ **Real-Time Monitoring**: Live performance and data quality feedback  
✅ **Easy Setup**: Automatic device discovery and one-click session control  
✅ **Data Integrity**: Comprehensive quality checks and automatic error detection  

---

## System Requirements

### PC Orchestrator (Required)
- **Operating System**: Windows 10/11, macOS 10.14+, or Linux Ubuntu 18.04+
- **Java**: JDK 17 or higher
- **Memory**: Minimum 4GB RAM (8GB recommended for multiple devices)
- **Network**: WiFi capable with local network access
- **Storage**: 10GB+ free space for session data

### Android Device (Recording Clients)
- **Android Version**: 8.0 (API level 26) or higher
- **Memory**: Minimum 3GB RAM (4GB+ recommended)
- **Storage**: 32GB+ internal storage for local data buffering
- **Sensors**: GSR sensor support (Shimmer3 GSR+ or compatible)
- **Camera**: Rear camera with thermal imaging support (TC001 recommended)
- **Network**: WiFi connectivity on same network as PC

### Hardware Accessories
- **GSR Sensor**: Shimmer3 GSR+ unit with electrodes
- **Thermal Camera**: TopDon TC001 thermal imaging module
- **Cables**: USB-C or micro-USB for charging during long sessions

---

## Getting Started

### Step 1: Download Software

1. **Download PC Orchestrator**:
   ```
   https://github.com/buccancs/new-bucika-gsr/releases/latest
   ```
   Download `bucika-gsr-orchestrator-v1.0.zip`

2. **Download Android APK**:
   ```
   https://github.com/buccancs/new-bucika-gsr/releases/latest
   ```
   Download `bucika-gsr-android-v1.0.apk`

### Step 2: Network Setup

Ensure all devices are connected to the same WiFi network:

1. **PC**: Connect to your WiFi network
2. **Android devices**: Connect to the same WiFi network
3. **Verify connectivity**: Devices should be able to ping each other

> 💡 **Tip**: For best results, use a 5GHz WiFi network with good signal strength

---

## PC Orchestrator Setup

### Installation

1. **Extract files**:
   ```bash
   unzip bucika-gsr-orchestrator-v1.0.zip
   cd bucika-gsr-orchestrator
   ```

2. **Run the application**:
   
   **Windows**:
   ```cmd
   gradlew.bat :pc:run
   ```
   
   **Mac/Linux**:
   ```bash
   ./gradlew :pc:run
   ```

### First Launch

When you first start the PC Orchestrator:

1. **System Check**: The application will verify Java version and network connectivity
2. **Network Discovery**: Your PC will start broadcasting on the local network for Android devices to find
3. **Dashboard Opens**: The main control interface will appear

### Main Dashboard Overview

The PC Orchestrator dashboard shows:

- **Connected Devices**: List of Android clients currently connected
- **Session Status**: Current recording state (IDLE/ARMED/RECORDING/FINALISING)
- **Real-Time Metrics**: Live data quality and performance indicators
- **Control Buttons**: Start/Stop recording, sync markers, device management

### Network Services

The PC Orchestrator automatically starts three network services:

1. **WebSocket Server** (Port 8080): Main communication with Android devices
2. **mDNS Discovery Service**: Allows Android devices to find the PC automatically
3. **Time Synchronization** (Port 9123): Provides nanosecond-precision timing

> ⚠️ **Firewall Note**: You may need to allow these ports through your firewall

---

## Android App Installation

### Installing the APK

1. **Enable Unknown Sources**:
   - Go to Settings > Security > Unknown Sources
   - Enable "Install from Unknown Sources"

2. **Install APK**:
   - Copy `bucika-gsr-android-v1.0.apk` to your device
   - Tap the APK file and follow installation prompts
   - Grant all requested permissions

### Hardware Setup

1. **Connect GSR Sensor**:
   - Pair your Shimmer3 GSR+ device via Bluetooth
   - Attach GSR electrodes to index and middle fingers
   - Ensure good skin contact and proper electrode placement

2. **Attach Thermal Camera**:
   - Connect TC001 thermal camera to USB-C port
   - Allow USB permissions when prompted
   - Verify thermal imaging preview in the app

### App Configuration

1. **Launch the App**: Tap the Bucika GSR icon
2. **Grant Permissions**: Allow camera, storage, location, and sensor access
3. **Connect to PC**: Tap "Connect to PC Orchestrator"

---

## First Recording Session

### Step 1: Connect Devices

1. **Start PC Orchestrator**: Run the desktop application first
2. **Launch Android App**: Open the app on your recording device(s)
3. **Auto-Connect**: The app should automatically discover and connect to the PC
4. **Manual Connection** (if needed):
   - Tap "Manual Connection" in the Android app
   - Enter PC IP address (shown in PC orchestrator)
   - Connection format: `ws://192.168.1.100:8080`

### Step 2: Verify Connection

✅ **Green indicators** in PC orchestrator show connected devices  
✅ **Device name** appears in the connected devices list  
✅ **Real-time data** flows from Android to PC (GSR values updating)

### Step 3: Prepare Recording

1. **Check Data Quality**:
   - Verify GSR signal is stable (green quality indicator)
   - Ensure thermal camera shows clear image
   - Confirm good network signal strength

2. **Set Recording Parameters**:
   - Session name/ID
   - Expected recording duration
   - Special notes or conditions

### Step 4: Start Recording

1. **Click "Start Session"** in PC Orchestrator
2. **All connected devices begin recording simultaneously**:
   - GSR data streams at 128 Hz
   - Thermal video records at 30 FPS
   - RGB video captures at 60 FPS
   - All data is automatically synchronized

### Step 5: Monitor Session

During recording, monitor:

- **Data Quality Indicators**: Real-time quality scores (aim for >80%)
- **Performance Metrics**: Memory usage, battery levels
- **Network Status**: Connection stability between devices
- **Storage Space**: Available storage on all devices

### Step 6: End Session

1. **Click "Stop Session"** in PC Orchestrator
2. **Automatic Data Processing**:
   - All devices stop recording simultaneously
   - Android devices automatically upload session files
   - PC orchestrator saves synchronized data
   - Quality assessment reports are generated

---

## Advanced Features

### Multi-Device Synchronization

Record with multiple Android devices simultaneously:

1. **Connect Multiple Devices**: Each Android device connects to the same PC orchestrator
2. **Synchronized Start**: All devices begin recording at exactly the same time
3. **Time Alignment**: Sub-millisecond synchronization across all sensors
4. **Coordinated Control**: Single control point manages all recording devices

### Sync Markers

Add precision timing markers during recording:

1. **Add Marker**: Click "Sync Mark" button during recording
2. **Marker Types**: Event markers, stimulus timing, behavior annotations
3. **Cross-Device Alignment**: Markers appear in all device recordings simultaneously
4. **Post-Processing**: Use markers for precise event alignment

### Data Quality Monitoring

Real-time quality assessment includes:

- **GSR Signal Quality**: Range validation, spike detection, saturation monitoring
- **Thermal Image Quality**: Temperature calibration, lens obstruction detection
- **Performance Quality**: Memory usage, CPU load, battery levels
- **Network Quality**: Connection stability, data transmission rates

### Performance Optimization

Automatic performance optimization features:

- **Memory Management**: Automatic memory leak detection and cleanup
- **Battery Optimization**: Power-saving modes during long recordings
- **CPU Usage**: Smart processing load balancing
- **Storage Management**: Automatic cleanup of temporary files

---

## Data Management

### File Organization

Recorded data is automatically organized:

```
Sessions/
├── session_20241225_143022/
│   ├── device_001/
│   │   ├── gsr_recording_20241225_143022.csv
│   │   ├── thermal_video_20241225_143022.mp4
│   │   ├── rgb_video_20241225_143022.mp4
│   │   └── quality_report.json
│   ├── device_002/
│   │   └── [same file structure]
│   ├── synchronized_data.csv
│   └── session_metadata.json
```

### Data Formats

- **GSR Data**: CSV with columns for timestamp, raw GSR, filtered GSR, temperature, quality flags
- **Video Data**: MP4 format with H.264 encoding for RGB, specialized format for thermal
- **Quality Reports**: JSON format with detailed quality assessments and recommendations
- **Session Metadata**: JSON with session information, device details, sync timestamps

### Data Export

Export options include:

1. **CSV Export**: Raw data in spreadsheet-compatible format
2. **MATLAB Export**: `.mat` files for MATLAB analysis
3. **Python Export**: Pickle files for Python data science workflows
4. **Research Package**: Complete ZIP with all data, quality reports, and analysis scripts

### Data Backup

Automatic backup features:

- **Local Backup**: Automatic local copies on both PC and Android devices
- **Cloud Integration**: Optional cloud storage for long-term archival
- **Export Validation**: MD5 checksums verify data integrity during backup

---

## Troubleshooting

### Connection Issues

**Problem**: Android device cannot find PC orchestrator

**Solutions**:
1. Verify both devices on same WiFi network
2. Check firewall settings on PC (allow ports 8080, 9123)
3. Try manual connection with PC IP address
4. Restart PC orchestrator and Android app

**Problem**: Connection drops during recording

**Solutions**:
1. Move devices closer to WiFi router
2. Switch to 5GHz WiFi if available
3. Close other network-intensive apps
4. Check for WiFi power saving settings

### Data Quality Issues

**Problem**: Poor GSR signal quality

**Solutions**:
1. Clean electrode contacts with alcohol wipe
2. Ensure firm but comfortable electrode placement
3. Allow 2-3 minutes for skin conductance to stabilize
4. Check for loose connections or damaged electrodes

**Problem**: Thermal camera not working

**Solutions**:
1. Check USB connection is secure
2. Grant USB permissions in Android settings
3. Restart app after connecting thermal camera
4. Try different USB port if available

### Performance Issues

**Problem**: High memory usage warnings

**Solutions**:
1. Close unnecessary apps on Android device
2. Restart Android app periodically during long sessions
3. Use devices with 4GB+ RAM for optimal performance
4. Monitor memory usage in performance dashboard

**Problem**: Battery draining quickly

**Solutions**:
1. Use devices with 80%+ battery at session start
2. Connect to power during long recordings
3. Enable battery optimization in app settings
4. Reduce screen brightness during recording

### Data Issues

**Problem**: Missing or corrupt data files

**Solutions**:
1. Check available storage space on all devices
2. Verify network stability during file uploads
3. Use data validation tools in PC orchestrator
4. Restore from automatic backups if available

---

## Best Practices

### Pre-Session Setup

1. **Hardware Check**:
   - Verify all sensors are functioning
   - Clean electrode contacts
   - Ensure adequate battery levels
   - Test network connectivity

2. **Software Check**:
   - Update to latest versions
   - Clear storage space
   - Close unnecessary apps
   - Verify time synchronization

3. **Environment Setup**:
   - Stable WiFi environment
   - Controlled temperature and lighting
   - Minimize electromagnetic interference
   - Prepare backup equipment

### During Recording

1. **Monitor Quality**:
   - Watch real-time quality indicators
   - Address quality warnings immediately
   - Document any issues or anomalies
   - Use sync markers for important events

2. **System Health**:
   - Monitor battery levels regularly
   - Watch for overheating warnings
   - Check network connection stability
   - Observe performance metrics

3. **Data Integrity**:
   - Avoid moving or disconnecting devices
   - Maintain stable electrode placement
   - Keep thermal camera lens clear
   - Document session conditions

### Post-Session

1. **Data Verification**:
   - Review quality assessment reports
   - Verify all expected files are present
   - Check data integrity with checksums
   - Export data in preferred formats

2. **Equipment Maintenance**:
   - Clean electrodes and sensors
   - Charge all devices
   - Update software if needed
   - Document any hardware issues

3. **Data Management**:
   - Organize files with clear naming
   - Create backup copies
   - Archive completed sessions
   - Update session documentation

### Long-Duration Sessions

For sessions longer than 2 hours:

1. **Power Management**:
   - Connect devices to power sources
   - Use portable battery packs if needed
   - Monitor battery status continuously
   - Plan charging breaks if necessary

2. **Performance Optimization**:
   - Monitor memory usage trends
   - Restart apps periodically if needed
   - Clear temporary data during breaks
   - Watch for performance degradation

3. **Data Management**:
   - Monitor storage space regularly
   - Enable incremental data backup
   - Use multiple storage destinations
   - Plan for large file transfers

---

## Support and Resources

### Documentation

- **Developer Guide**: Technical implementation details
- **API Reference**: Complete protocol specification
- **Troubleshooting Guide**: Detailed problem resolution
- **Quality Guide**: Data quality best practices

### Community Support

- **GitHub Issues**: Report bugs and feature requests
- **Discussion Forums**: Community Q&A and tips
- **User Group**: Share experiences and best practices
- **Research Papers**: Published studies using the platform

### Professional Support

For research institutions and commercial users:

- **Technical Support**: Priority bug fixes and feature development
- **Custom Development**: Specialized features for specific research needs
- **Training Services**: On-site setup and training programs
- **Data Analysis**: Professional data analysis and consulting

### Contact Information

- **GitHub Repository**: https://github.com/buccancs/new-bucika-gsr
- **Issue Tracking**: https://github.com/buccancs/new-bucika-gsr/issues
- **Email Support**: support@bucika-gsr.com
- **Documentation**: https://bucika-gsr.readthedocs.io

---

*This manual is for Bucika GSR Platform v1.0. For the latest version, visit our GitHub repository.*