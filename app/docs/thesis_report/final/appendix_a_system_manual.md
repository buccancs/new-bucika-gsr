# Appendix A: System Manual -- Technical Setup, Configuration, and Maintenance Details

Purpose and Justification: This System Manual provides the technical foundation necessary for deploying the Multi-Sensor Recording System in research environments. As one of the thesis objectives was to create a practical, deployable system for contactless GSR research, this appendix demonstrates the achievement of that objective by providing comprehensive technical documentation that enables system replication and operational deployment. The manual addresses the practical implementation aspects that are essential for the system's research utility but too detailed for inclusion in the main thesis chapters.

This System Manual provides comprehensive technical documentation for the deployment, configuration, and maintenance of the Multi-Sensor Recording System for Contactless GSR Prediction Research. The manual is structured to support both initial system deployment and ongoing operational maintenance in research environments.

## A.1 System Architecture Overview

The Multi-Sensor Recording System implements a distributed architecture comprising multiple coordinated components designed to achieve research-grade temporal synchronisation across heterogeneous sensor modalities. The core system architecture consists of:

Primary Components:
- Python Desktop Controller: Central orchestration service providing master clock synchronisation, device management, and session coordination
- Android Mobile Application: Distributed sensor nodes supporting RGB camera, thermal imaging, and physiological sensor integration
- Shimmer3 GSR+ Sensors: Bluetooth-enabled physiological measurement devices for ground truth data collection [8]
- TopDon TC001 Thermal Cameras: USB-C connected thermal imaging sensors for contactless physiological monitoring

Network Architecture:
The system employs a hybrid star-mesh topology with the Python Desktop Controller serving as the master coordinator. Communication is implemented using WebSocket over TLS with structured JSON messaging protocol to ensure secure, real-time data exchange and temporal synchronisation [21]. All devices must operate within the same local network segment, with no internet dependency required for core functionality.

Synchronisation Framework:
Temporal coordination is achieved through a custom NTP-based synchronisation engine integrated with the Python controller. The system maintains temporal alignment across all connected devices within ±3.2 ms accuracy, enabling precise multi-modal data correlation essential for contactless GSR prediction research.

## A.2 Hardware Requirements and Specifications

### A.2.1 Desktop Controller Requirements

Minimum System Specifications:
- Operating System: Windows 10 (build 1903+), macOS 10.15+, or Ubuntu 18.04 LTS+
- Processor: Intel Core i5-8400 / AMD Ryzen 5 2600 or equivalent (6+ cores recommended)
- Memory: 8GB RAM minimum (16GB recommended for multi-device sessions)
- Storage: 500GB available storage (SSD recommended for sustained write performance)
- Network: Gigabit Ethernet adapter or 802.11ac WiFi capability
- USB Ports: USB 3.0+ ports for optional direct sensor connectivity

Recommended System Specifications:
- Processor: Intel Core i7-10700K / AMD Ryzen 7 3700X or better
- Memory: 32GB RAM for extended multi-device recording sessions
- Storage: 2TB NVMe SSD with sustained write speeds >500 MB/s
- Network: Dedicated Gigabit Ethernet connection for minimal latency
- Graphics: Discrete GPU for accelerated video processing (optional)

### A.2.2 Android Device Requirements

Hardware Compatibility:
- Android Version: API Level 24+ (Android 7.0 Nougat) minimum [13]
- Camera: Camera2 API support with 4K recording capability [13]
- Memory: 6GB RAM minimum (8GB+ recommended)
- Storage: 128GB internal storage minimum (256GB+ recommended)
- Connectivity: USB-C with OTG support, Bluetooth 4.0+, 802.11n WiFi [14]
- Sensors: Accelerometer, gyroscope, magnetometer for device orientation

Validated Device Models:
- Samsung Galaxy S22 (Android 15) - primary target platform

### A.2.3 Sensor Hardware Specifications

Shimmer3 GSR+ Sensor:
- Sampling Rate: 1-1024 Hz (configurable, 128 Hz default)
- GSR Range: 0-4 μS (microsiemens)
- Resolution: 16-bit ADC
- Battery Life: 12+ hours continuous operation
- Connectivity: Bluetooth 2.1+EDR, IEEE 802.15.1 compliant [15]
- Data Format: CSV export with timestamp synchronisation

TopDon TC001 Thermal Camera:
- Resolution: 256×192 thermal array
- Temperature Range: -20°C to +550°C
- Accuracy: ±2°C or ±2% of reading
- Frame Rate: 25 Hz
- Connectivity: USB-C direct connection [20]
- Power: Bus-powered via USB-C

## A.3 Software Installation and Environment Setup

### A.3.1 Python Desktop Controller Installation

Prerequisites Installation:

*Windows Environment:*
```bash
# Install Python 3.8+ from python.org
# Download and install Visual Studio Build Tools
# Install Git for Windows

# Verify installation
python --version  # Should show Python 3.8+
git --version     # Should show Git 2.30+
```

*macOS Environment:*
```bash
# Install Homebrew if not present
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install dependencies
brew install python@3.9 git opencv

# Verify installation
python3 --version
which git
```

*Ubuntu/Debian Environment:*
```bash
# Update package repositories
sudo apt update && sudo apt upgrade -y

# Install system dependencies
sudo apt install -y python3.9 python3-pip python3-venv git
sudo apt install -y libgl1-mesa-glx libglib2.0-0 libusb-1.0-0-dev
sudo apt install -y bluetooth libbluetooth-dev

# Verify installation
python3 --version
pip3 --version
```

Application Installation:
```bash
# Clone repository with submodules
git clone --recursive https://github.com/buccancs/bucika_gsr.git
cd bucika_gsr

# Create Python virtual environment
python3 -m venv venv

# Activate virtual environment
# Windows:
venv\Scripts\activate
# macOS/Linux:
source venv/bin/activate

# Install Python dependencies
pip install -r requirements.txt
pip install -r PythonApp/requirements.txt

# Install optional dependencies for enhanced functionality
pip install pyshimmer bluetooth psutil matplotlib scipy [18]

# Verify installation
python PythonApp/system_test.py
```

### A.3.2 Android Application Installation

Development Environment Setup:
```bash
# Download and install Android Studio Arctic Fox (2020.3.1) or later
# Configure Android SDK with API Level 24+ support [13]
# Enable Developer Options and USB Debugging on target device

# Build application
cd AndroidApp
./gradlew build

# Install on connected device
./gradlew installDevDebug

# Or install pre-built APK
adb install app-debug.apk
```

Production Deployment:
```bash
# Build release version
./gradlew assembleRelease

# Sign APK (production environments)
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore release-key.keystore app-release-unsigned.apk alias_name

# Install signed APK
adb install app-release.apk
```

## A.4 System Configuration Procedures

### A.4.1 Network Configuration

Local Network Setup:
1. Configure WiFi Access Point: Ensure all devices connect to the same network segment with sufficient bandwidth (minimum 50 Mbps for multi-device recording)
2. Firewall Configuration:
   - Open inbound ports 8080-8089 on the Python controller machine
   - Allow Python application through Windows Defender/macOS Firewall
   - Configure router to allow inter-device communication
3. IP Address Assignment: Configure static IP addresses or ensure DHCP reservation for consistent device addressing [19]

Network Quality Validation:
```bash
# Test network connectivity between devices
ping [ANDROID_DEVICE_IP]

# Measure network latency and bandwidth
iperf3 -s  # On controller machine
iperf3 -c [CONTROLLER_IP] -t 30  # On Android device

# Verify port accessibility
telnet [CONTROLLER_IP] 8080
```

### A.4.2 Device Pairing and Registration

Android Device Configuration:
1. Install Application: Deploy signed APK to target Android devices
2. Grant Permissions: Camera, microphone, storage, location, and network permissions [13]
3. Configure Network Settings: Set controller IP address and port in application settings
4. Test Connection: Use built-in connection test to verify communication

Shimmer Sensor Pairing:
1. Power On Sensor: Ensure battery charge >50% for stable operation
2. Enable Bluetooth Pairing Mode: Press and hold sensor button until LED blinks blue
3. Pair with Controller: Use Python application's Bluetooth discovery feature [14]
4. Validate Connection: Verify GSR data streaming at configured sample rate

Thermal Camera Setup:
1. Connect USB-C Cable: Use high-quality USB-C OTG cable rated for data transfer [16]
2. Install Camera Drivers: May require manufacturer drivers on Windows systems
3. Configure Permissions: Grant USB device access to Android application [16]
4. Calibrate Camera: Run thermal calibration routine using black-body reference

### A.4.3 Session Configuration

Recording Parameters:
- Video Resolution: 3840×2160 (4K UHD) for research-grade image quality
- Frame Rate: 30 FPS (configurable 24-60 FPS based on requirements)
- GSR Sampling Rate: 128 Hz (configurable 1-1024 Hz)
- Thermal Frame Rate: 25 Hz (camera hardware limit)
- Session Duration: Configurable unlimited or time-limited sessions

Synchronisation Settings:
- Master Clock Source: Python controller system clock with NTP synchronisation
- Sync Tolerance: ±5 ms maximum temporal drift before recalibration
- Heartbeat Interval: 10-second status updates between devices
- Reconnection Timeout: 30-second automatic reconnection for transient failures

## A.5 Operational Procedures

### A.5.1 Standard Recording Session Workflow

Pre-Session Setup (15 minutes):
1. Power Management: Ensure all devices have >50% battery charge
2. Network Verification: Confirm all devices connected to local network with stable signal
3. Environment Preparation: Set up recording environment with appropriate lighting and minimal electromagnetic interference
4. Device Registration: Launch Python controller and verify all Android devices and sensors are discovered and connected
5. Calibration Verification: Confirm thermal cameras and GSR sensors are properly calibrated

Session Initialisation (5 minutes):
1. Create Session: Configure session parameters including participant ID, duration, and enabled sensors
2. Device Status Check: Verify all devices report "Ready" status with green indicators
3. Synchronisation Validation: Run synchronisation test to ensure temporal alignment within tolerance
4. Preview Verification: Confirm preview streams from all cameras are functioning correctly
5. Final Systems Check: Review session configuration and device health metrics

Recording Phase (Variable Duration):
1. Session Start: Initiate synchronised recording across all connected devices
2. Real-time Monitoring: Monitor device status, data quality indicators, and synchronisation metrics
3. Quality Assurance: Observe data integrity indicators and network performance metrics
4. Intervention Protocols: Address any warnings or errors using established troubleshooting procedures
5. Session Documentation: Record any notable events or environmental changes during session

Session Completion (10 minutes):
1. Controlled Stop: Terminate recording session using coordinated stop command
2. Data Integrity Validation: Verify all expected data files are present and uncorrupted
3. Metadata Generation: Ensure session metadata is complete with device information and timestamps
4. Data Transfer: Initiate file transfer from Android devices to central storage
5. Session Archival: Archive completed session data with appropriate backup procedures

### A.5.2 Quality Assurance Procedures

Real-time Quality Monitoring:
- Temporal Synchronisation: Continuous monitoring of device clock offsets with automatic alerts for drift >±5 ms
- Data Completeness: Frame drop detection for video streams and sample loss detection for GSR data
- Network Performance: Bandwidth utilisation and latency monitoring with adaptive quality adjustment
- Device Health: Battery levels, storage capacity, and thermal status monitoring

Post-Session Validation:
- File Integrity: MD5 checksum validation for all recorded files
- Temporal Alignment: Cross-correlation analysis of multi-modal timestamps
- Data Quality Metrics: Quantitative assessment of signal-to-noise ratio and data completeness
- Metadata Completeness: Verification of session documentation and device configuration records

## A.6 Maintenance Procedures

### A.6.1 Daily Maintenance Tasks

System Health Checks:
- Verify network connectivity and bandwidth availability
- Check battery levels on all Android devices and Shimmer sensors
- Confirm storage capacity on all recording devices (minimum 20GB free space)
- Validate thermal camera calibration using reference temperature source
- Test synchronisation accuracy using built-in diagnostic tools

Data Management:
- Archive completed sessions to secure storage with redundant backup
- Clear temporary files and cache from Android devices
- Verify data backup integrity using automated validation scripts [23]
- Update session metadata database with new recordings
- Monitor storage usage trends and plan capacity expansion

### A.6.2 Weekly Maintenance Tasks

Software Updates:
- Check for and install Python package updates using `pip list --outdated`
- Update Android application if new versions are available
- Install operating system security updates on controller machine
- Update device drivers for thermal cameras and Bluetooth adapters
- Verify all software components remain compatible after updates

Hardware Maintenance:
- Clean thermal camera lenses using appropriate optical cleaning materials
- Inspect USB cables for damage and replace if necessary
- Charge all Shimmer sensors to full capacity and verify battery health
- Clean Android device screens and cameras to ensure optimal image quality
- Check network equipment for proper operation and cooling

### A.6.3 Monthly Maintenance Tasks

Comprehensive System Calibration:
- Perform complete thermal camera calibration using certified black-body reference
- Validate GSR sensor accuracy using known conductance standards
- Conduct end-to-end synchronisation validation across all device configurations
- Verify camera calibration parameters using calibrated checkerboard patterns [22]
- Test emergency recovery procedures and backup restoration processes

Performance Optimisation:
- Analyse system performance logs to identify bottlenecks or degradation trends
- Optimise network configuration based on usage patterns and performance metrics
- Clean and defragment storage systems to maintain optimal write performance
- Review and optimise session configurations based on research requirements
- Update documentation to reflect any configuration changes or lessons learned

### A.6.4 Annual Maintenance Tasks

System Lifecycle Management:
- Comprehensive security audit of all system components and network configurations
- Hardware lifecycle assessment and replacement planning for aging components
- Software dependency audit and migration planning for deprecated packages
- Backup and disaster recovery testing with full system restoration validation
- Documentation review and updates to reflect current best practices and procedures

Compliance and Validation:
- Validation of data protection and privacy compliance procedures
- Audit of research data handling and retention policies
- Review of safety procedures and emergency response protocols
- Assessment of system capacity and scalability for expanding research requirements
- Training refresh for all system operators and maintenance personnel

## A.7 Troubleshooting Reference

### A.7.1 Common System Issues

Network Connectivity Problems:
- Symptom: Android devices cannot connect to Python controller
- Diagnosis: Verify network connectivity, firewall settings, and port availability
- Resolution: Check IP configuration, restart network services, validate controller application status

Synchronisation Drift Issues:
- Symptom: Temporal synchronisation exceeds ±5 ms tolerance
- Diagnosis: Monitor network latency and system clock stability
- Resolution: Restart synchronisation service, check NTP configuration, verify network quality

Data Quality Degradation:
- Symptom: Frame drops, sample loss, or corrupted data files
- Diagnosis: Monitor system resources, network performance, and storage availability
- Resolution: Adjust recording parameters, increase system resources, optimise network configuration

### A.7.2 Emergency Procedures

Critical System Failure:
1. Immediate Response: Stop all active recording sessions to prevent data corruption
2. System Assessment: Identify failed components and assess impact on ongoing research
3. Data Recovery: Attempt recovery of any partially recorded sessions using backup procedures
4. Fallback Configuration: Deploy backup equipment or reduced-capability configuration if available
5. Incident Documentation: Record failure details for post-incident analysis and prevention

Data Loss Prevention:
- Implement automated backup procedures with multiple redundancy levels
- Maintain real-time data replication for critical research sessions
- Establish recovery procedures for various failure scenarios
- Document and regularly test emergency response protocols
- Maintain emergency contact procedures for technical support

This comprehensive System Manual provides the technical foundation necessary for successful deployment and operation of the Multi-Sensor Recording System in research environments, ensuring reliable data collection for contactless GSR prediction studies while maintaining research-grade quality standards.
