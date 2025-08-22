# Appendix B: User Manual -- Comprehensive Guide for Researchers and Research Technicians

Purpose and Justification: This User Manual addresses the practical needs of researchers who will operate the Multi-Sensor Recording System. Since the thesis emphasises usability and ease of deployment for non-technical researchers, this appendix provides essential evidence that the system meets research workflow requirements. The manual demonstrates how the technical implementation translates into practical research tools, supporting the thesis claims about system usability and research readiness.

This User Manual provides comprehensive operational guidance for researchers and technical staff operating the Multi-Sensor Recording System for Contactless GSR Prediction Research. The manual is structured to support users with varying levels of technical expertise, from research scientists conducting studies to laboratory technicians managing equipment.

## B.1 Introduction and User Roles

The Multi-Sensor Recording System is designed for research environments where contactless physiological monitoring is required. This manual addresses the needs of two primary user groups:

Primary Researchers: Scientists and doctoral students conducting human participants research who require high-quality physiological data collection with minimal technical overhead. These users focus on experimental design, participant management, and data analysis rather than system administration.

Research Technicians: Laboratory staff responsible for equipment setup, maintenance, and technical support during research sessions. These users require deeper understanding of system configuration, troubleshooting, and quality assurance procedures.

Safety and Ethical Responsibilities: All users must complete ethics training and understand data protection requirements before operating the system. The equipment must be used only with approved research protocols and proper participant consent. Emergency procedures must be understood before conducting any session involving human participants.

### B.1.1 Ethics Approval and Compliance Framework

This research system operates under the ethics approval granted by the UCLIC Ethics Committee (Project ID: 1428) for the research titled "Investigating AI and physiological computing: App for Camera-based Contactless Sensing of Physiological Signals". Principal Investigator: Prof. Youngjun Cho (youngjun.cho@ucl.ac.uk). Researchers: Duy An Tran, Zikun Quan, and Jitesh Joshi. All research activities using this system must comply with the approved research protocol, which includes:

Participant Information and Consent: All participants must receive the approved participant information sheet detailing the 30-minute research sessions involving thermal cameras, RGB video recording, and GSR sensors [1]. Participants must provide informed consent before any data collection begins. The information sheet covers voluntary participation, data collection procedures, participant rights including withdrawal, data anonymisation protocols, and contact information for researchers and ethics committee.

Risk Assessment Compliance: The system has undergone comprehensive risk assessment reviewed and approved by supervisor Prof. Youngjun Cho covering technical safety, data protection, participant welfare, and laboratory environment protocols. All operational procedures documented in this manual align with the approved risk assessment framework including equipment safety protocols, emergency procedures, and participant protection measures.

Data Protection Requirements: Research conducted with this system must follow the approved data management plan, including participant anonymisation protocols, secure data storage procedures, GDPR compliance, and data retention/disposal schedules as specified in the ethics approval [3]. Special category data (physiological signals, body movement data, minimal health data) is processed under Scientific Research Purposes lawful basis.

Approved Research Scope: The ethics approval covers healthy adults aged 18+ able to consent, with exclusions for individuals with cardiovascular or neurological conditions (epilepsy, arrhythmia) or chronic serious illness. Any modifications to research procedures or participant criteria require ethics committee review and approval before implementation.

## B.2 Initial System Setup and Orientation

### B.2.1 Pre-Use Verification Checklist

Before conducting any research session, complete the following verification steps:

Equipment Inventory:
- [ ] Python Desktop Controller operational on primary computer
- [ ] Android device with Multi-Sensor Recording App installed
- [ ] TopDon TC001 thermal camera with USB-C cable [20]
- [ ] Shimmer3 GSR+ sensor with charged battery (>50%) [8]
- [ ] Network infrastructure: WiFi access point with stable connection
- [ ] Backup storage media and charging cables available

Software Verification:
- [ ] Desktop application launches without errors [17]
- [ ] Android application grants all required permissions (camera, storage, location) [13]
- [ ] Network connectivity test between devices passes [19]
- [ ] Synchronisation calibration completes successfully
- [ ] All sensors detected and responding to test commands

Documentation Review:
- [ ] Research protocol approved and UCLIC Ethics Committee clearance confirmed (Project ID: 1428, "Investigating AI and physiological computing: App for Camera-based Contactless Sensing of Physiological Signals")
- [ ] Participant information sheets and consent forms prepared and approved (including thermal imaging, GSR sensors, 30-minute sessions)
- [ ] Risk assessment documentation reviewed and mitigation procedures understood (Prof. Youngjun Cho approved)
- [ ] Data management plan reviewed and storage locations confirmed (GDPR compliance for physiological data) [3]
- [ ] Emergency contact information readily accessible (Primary: Prof. Youngjun Cho [youngjun.cho@ucl.ac.uk], Ethics: [ethics@ucl.ac.uk])

### B.2.2 User Interface Overview

Desktop Controller Interface:
The primary control interface features a dashboard layout with real-time monitoring panels [17]:
- Device Status Panel: Shows connected Android devices with battery levels, storage capacity, and connection quality indicators
- Sensor Status Panel: Displays Shimmer GSR sensor status including signal quality, sampling rate, and calibration status
- Session Configuration Panel: Controls for session parameters including participant ID, recording duration, and sensor selection
- Live Monitoring Panel: Real-time preview streams from RGB and thermal cameras with data quality overlays [22]
- Synchronisation Status Panel: Clock offset indicators and temporal alignment quality metrics

Android Application Interface:
The mobile interface provides local device control and status information:
- Connection Status Display: Network connectivity and controller communication status
- Sensor Configuration Panel: Camera resolution, thermal calibration controls, and sensor selection options [13]
- Recording Status Display: Local recording state with indicators for active streams and storage usage
- Preview Display: Live camera feeds with optional thermal overlay and physiological signal indicators

## B.3 Standard Operating Procedures

### B.3.1 Session Preparation Protocol (15-20 minutes)

Phase 1: Environmental Setup (5 minutes)
1. Power Management: Ensure all devices have >60% battery charge or are connected to power supplies
2. Network Configuration: Verify WiFi connectivity with signal strength >-60 dBm on all devices
3. Workspace Preparation: Arrange equipment in recording environment with adequate lighting and minimal electromagnetic interference [4]
4. Temperature Equilibration: Allow thermal cameras to stabilise for minimum 5 minutes after power-on

Phase 2: System Initialisation (5 minutes)
1. Launch Desktop Controller: Start Python application and verify all subsystems initialise successfully [17]
2. Android Device Registration: Power on mobile devices and confirm automatic discovery by desktop controller [19]
3. Sensor Connectivity: Pair Shimmer GSR sensors via Bluetooth and verify data streaming at configured sample rate [15]
4. Calibration Validation: Execute thermal camera calibration routine and confirm accuracy within ±0.2°C tolerance

Phase 3: System Verification (5 minutes)
1. Synchronisation Test: Perform clock synchronisation and verify temporal alignment within ±3 ms tolerance
2. Data Quality Check: Confirm preview streams display correctly with acceptable signal-to-noise ratios
3. Storage Verification: Check available storage capacity across all devices (minimum 10GB per hour of recording)
4. Emergency Procedures Review: Verify emergency stop functionality and backup procedures are operational

### B.3.2 Participant Session Workflow

Pre-Recording Phase (10 minutes)
- Participant Briefing: Explain recording procedure, equipment function, and participant rights including withdrawal procedures [1]
- Consent Documentation: Complete informed consent process and document participant information with assigned anonymous ID
- Positioning and Setup: Position participant optimally for camera coverage while ensuring comfort and natural behaviour
- Reference Measurement: If using ground truth GSR sensor, attach electrodes following standard skin preparation procedures [1]
- Baseline Recording: Capture 2-minute baseline recording for signal normalisation and calibration verification

Recording Phase (Variable Duration)
- Session Initiation: Execute synchronised start command from desktop controller ensuring all devices begin recording simultaneously
- Quality Monitoring: Continuously monitor real-time data quality indicators including signal strength, temporal synchronisation, and storage status
- Event Annotation: Use timestamp markers to annotate significant events, stimulus presentations, or environmental changes
- Participant Welfare: Monitor participant comfort and respond to any requests for breaks or session termination
- Data Integrity: Observe system health indicators and address any warnings or errors using established troubleshooting procedures

Post-Recording Phase (10 minutes)
- Controlled Session End: Execute synchronised stop command ensuring all devices complete recording and file finalisation
- Data Validation: Verify recording completeness using automated integrity checks and manual file verification [23]
- Participant Debriefing: Conduct post-session interview and provide opportunity for questions or feedback
- Data Transfer: Initiate secure transfer of recorded data to central storage with redundant backup procedures
- Equipment Reset: Prepare equipment for subsequent sessions including cleaning, charging, and storage

### B.3.3 Quality Assurance Procedures

Real-Time Quality Monitoring:
The system provides continuous quality assessment through visual and auditory indicators:
- Green Indicators: All systems operational within acceptable parameters
- Yellow Warnings: Minor issues detected requiring attention but not immediate intervention
- Red Alerts: Critical issues requiring immediate corrective action or session termination

Data Quality Metrics:
- Temporal Synchronisation: Clock offset <±5 ms across all devices
- Video Quality: Frame rate stability within 2% of target, minimal frame drops (<0.1%)
- Thermal Accuracy: Temperature measurement accuracy within ±0.2°C of calibration standard
- GSR Signal Quality: Sampling rate stability within 1% of configured rate, signal-to-noise ratio >20 dB [7]

Quality Verification Checklist:
- [ ] Synchronisation status shows green across all devices
- [ ] Video preview streams display clearly without artifacts
- [ ] Thermal calibration within specified tolerance
- [ ] GSR signal shows physiological variation without saturation [1]
- [ ] Network latency <50 ms for all device communications
- [ ] Storage write speeds maintain minimum required throughput

## B.4 User Interface Detailed Walkthrough

### B.4.1 Desktop Controller Operation

Main Dashboard Navigation:
Upon launching the desktop application, users access the primary dashboard containing six main panels arranged in a logical workflow [17]:

Device Management Panel (Top Left):
- Displays connected Android devices with unique identifiers and IP addresses
- Battery level indicators with colour-coded warnings (red <20%, yellow <50%, green >50%)
- Connection quality metrics including signal strength and latency measurements
- Device-specific controls for individual configuration and troubleshooting

Session Configuration Panel (Top Right):
- Participant ID entry field with automatic validation and duplicate checking
- Recording duration controls supporting both time-limited and continuous recording modes
- Sensor selection checkboxes for RGB video, thermal imaging, and GSR data streams
- Advanced parameters including frame rates, resolution settings, and synchronisation tolerances

Live Preview Panel (Centre):
- Multi-window display showing real-time RGB and thermal camera feeds [22]
- Overlay options for physiological signal indicators and data quality metrics
- Zoom and pan controls for detailed examination of sensor coverage
- Screenshot functionality for documentation and quality assurance

Data Quality Monitor (Bottom Left):
- Real-time signal quality metrics with trend displays and threshold indicators
- Network performance monitoring including bandwidth utilisation and error rates
- Storage status indicators showing available capacity and write performance
- Historical quality data with configurable alert thresholds

Synchronisation Status Panel (Bottom Centre):
- Clock offset displays for each connected device with tolerance indicators
- Temporal drift tracking with automatic recalibration triggers
- Synchronisation quality score based on multi-device temporal alignment
- Manual synchronisation controls for troubleshooting and calibration

Control Interface Panel (Bottom Right):
- Primary recording controls including start, stop, pause, and emergency stop functions
- Export controls for data transfer and format conversion [23]
- System status indicators showing overall health and operational state
- Advanced controls for calibration routines and diagnostic procedures

### B.4.2 Android Application Interface

Main Screen Layout:
The Android application provides a streamlined interface optimised for mobile operation:

Status Bar (Top):
- Controller connection indicator with IP address and latency display
- WiFi signal strength indicator with automatic network quality assessment
- Battery level with estimated recording time remaining based on current usage
- Storage available with automatic cleanup recommendations

Camera Preview Section (Main Area):
- Live RGB camera feed with optional grid overlay for positioning guidance [13]
- Thermal camera overlay (when connected) with temperature scale and calibration indicators [16]
- Touch-to-focus controls and exposure adjustment for optimal image quality
- Recording status indicators including active stream markers and timestamp display

Sensor Status Section (Bottom):
- GSR sensor connection status with signal quality indicators [15]
- Sampling rate display and data buffer status
- Calibration status for thermal and physiological sensors
- Device temperature monitoring with thermal management indicators

Control Interface (Overlay):
- Local recording controls for emergency situations or manual operation
- Network configuration panel for controller IP address and port settings [19]
- Device settings including camera parameters and sensor configuration options
- Help system with quick access to troubleshooting guides and contact information

## B.5 Data Management and Export Procedures

### B.5.1 Automated Data Handling

File Structure and Naming:
The system automatically organises recorded data using a standardised directory structure [23]:
```
Sessions/
├── YYYY-MM-DD_HH-MM-SS_ParticipantID/
│   ├── metadata.json
│   ├── rgb_video_device1.mp4
│   ├── thermal_video_device1.dat
│   ├── gsr_data_sensor1.csv
│   ├── sync_log.txt
│   └── quality_report.html
```

Data Integrity Verification:
- Automatic checksum generation for all recorded files using SHA-256 algorithm
- Redundant metadata storage with cross-reference validation
- Real-time corruption detection during recording with automatic error correction
- Post-session integrity verification with comprehensive validation reports [23]

### B.5.2 Export and Analysis Preparation

Supported Export Formats:
- Video Data: MP4 (H.264), AVI (uncompressed), MOV (ProRes for high-quality analysis)
- Thermal Data: CSV with temperature matrices, MATLAB .mat files, NumPy .npy arrays [24]
- Physiological Data: CSV with timestamps, EDF for biomedical analysis, JSON for web applications
- Synchronised Datasets: Combined formats with aligned timestamps for multi-modal analysis

Export Wizard Operation:
1. Session Selection: Choose completed sessions from archive with filtering by date, participant, or quality criteria
2. Format Configuration: Select output formats optimised for target analysis software (MATLAB, Python, R, SPSS) [22,24]
3. Data Filtering: Apply temporal windows, sensor selection, and quality thresholds to exported datasets
4. Anonymisation Options: Remove or hash personally identifiable information according to research protocols [3]
5. Validation and Transfer: Verify export integrity and transfer to approved analysis environments

## B.6 Troubleshooting and Error Resolution

### B.6.1 Common Issues and Solutions

Device Connection Problems:

*Symptom*: Android device not detected by desktop controller
*Diagnostic Steps*:
1. Verify both devices connected to same WiFi network with internet connectivity test
2. Check firewall settings on desktop computer allowing inbound connections on ports 8080-8089 [21]
3. Confirm Android application has network permissions and is not in battery optimisation mode [13]
4. Test direct IP connection using manual device registration in desktop application [19]

*Resolution Procedures*:
- Restart network services on both devices and attempt reconnection
- Use alternative port configuration if default ports are blocked
- Enable WiFi hotspot on Android device for direct connection if network issues persist
- Contact technical support if hardware-level networking problems suspected

Synchronisation Drift Issues:

*Symptom*: Temporal alignment warnings or red synchronisation indicators
*Diagnostic Steps*:
1. Check system clock accuracy on all devices using NTP server validation
2. Measure network latency between devices using built-in diagnostic tools
3. Assess network stability and bandwidth availability during peak usage periods
4. Verify no background applications consuming significant system resources

*Resolution Procedures*:
- Execute manual clock synchronisation from desktop controller interface
- Reduce recording parameters (resolution, frame rate) to decrease network load
- Switch to higher quality network connection or relocate devices closer to WiFi access point
- Restart synchronisation service and allow 30-second stabilisation period

Data Quality Degradation:

*Symptom*: Poor signal quality indicators or corrupted data files
*Diagnostic Steps*:
1. Check sensor connections and cable integrity for all physical connections [16]
2. Monitor system resources (CPU, memory, storage) for capacity constraints
3. Verify environmental conditions within acceptable ranges for sensor operation [4]
4. Test sensor functionality using built-in calibration and diagnostic routines

*Resolution Procedures*:
- Clean sensor contacts and ensure proper cable seating
- Close unnecessary applications and allocate additional system resources
- Adjust environmental conditions or relocate equipment away from interference sources
- Replace faulty cables or sensors with backup equipment

### B.6.2 Emergency Procedures

Critical System Failure:
1. Immediate Response: Activate emergency stop function to prevent data corruption
2. Participant Safety: Ensure participant welfare and provide appropriate support
3. Data Recovery: Attempt recovery of partial recordings using automatic backup procedures [23]
4. Session Documentation: Record failure details and circumstances for technical analysis
5. Backup Protocol: Deploy backup equipment or reschedule session as appropriate

Data Security Incidents:
1. Containment: Immediately isolate affected systems and prevent further data exposure
2. Assessment: Evaluate scope of potential data breach and affected participant information [3]
3. Notification: Follow institutional procedures for data security incident reporting
4. Recovery: Restore system security and implement additional protective measures
5. Documentation: Complete incident reports and review security protocols

## B.7 User Training and Certification

### B.7.1 Training Requirements

Basic User Certification (8 hours):
- System overview and safety procedures (2 hours)
- Hands-on operation with supervised practice sessions (4 hours)
- Troubleshooting exercises and emergency response training (1 hour)
- Assessment and certification review (1 hour)

Advanced User Training (16 hours):
- System administration and configuration management (4 hours)
- Advanced troubleshooting and maintenance procedures (4 hours)
- Data management and export procedures (4 hours) [23]
- Quality assurance and validation techniques (4 hours)

### B.7.2 Competency Assessment

Practical Skills Evaluation:
- Successfully complete supervised recording session from setup to data export
- Demonstrate troubleshooting capabilities using simulated system failures
- Execute emergency procedures and participant safety protocols
- Perform quality assurance validation and documentation procedures

Knowledge Assessment:
- Understanding of physiological measurement principles and limitations [1,7]
- Comprehension of data protection and ethical research requirements [3]
- Familiarity with system capabilities and operational constraints
- Ability to interpret quality metrics and make appropriate operational decisions

This comprehensive User Manual provides the operational foundation necessary for successful deployment of the Multi-Sensor Recording System in research environments. Following these procedures ensures reliable data collection while maintaining the highest standards of participant safety and research integrity.
