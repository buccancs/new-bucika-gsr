# Appendix H: Consolidated Technical Reference - UPDATED

Purpose and Justification: This appendix provides a centralised navigation aid for the extensive technical content throughout the thesis. Visual content has been consolidated into Appendix Z - this appendix now focuses on technical reference materials, code organisation, and reproducibility resources.

This appendix provides a consolidated reference index for technical content throughout the thesis, enabling quick navigation to implementation details, hardware specifications, and reproducibility resources without duplication of visual materials now centralised in Appendix Z.

## H.1 Visual Content Reference - SEE APPENDIX Z

Important Notice: All figures, diagrams, tables, and visual content previously referenced in this appendix have been consolidated into Appendix Z: Consolidated Figures, Diagrams, and Visual Content for improved navigation and reduced duplication.

### H.1.1 Quick Navigation to Visual Content
- Background Figures (Chapter 2): Appendix Z, Section Z.1
- Architecture Diagrams (Chapter 3): Appendix Z, Section Z.2  
- Implementation Figures (Chapter 4): Appendix Z, Section Z.3
- Performance Diagnostics (Appendix G): Appendix Z, Section Z.4
- Cross-Reference Tables: Appendix Z, Section Z.5

## H.2 Code Listing Reference Index

### H.2.1 Key Implementation Components (Appendix F)

Synchronisation Code:
- Master clock coordination with NTP and PC server startup, error handling, and thread management [21]
- Synchronised recording start commands with distributed timestamp coordination
- Clock drift compensation and temporal alignment algorithms

Data Pipeline Code:
- Heart rate estimation using Fourier transform (Welch's method) for physiological signal processing [5,22]
- Multi-modal data fusion combining RGB video, thermal imaging, and GSR sensor data [1,6,7]
- Real-time feature extraction and contactless GSR prediction algorithms

Integration Code:
- Sensor and device integration logic with Android-mediated connections and fallback mechanisms [13,15,19]
- Multi-device coordination supporting heterogeneous sensor types (Shimmer GSR, thermal cameras, mobile devices) [8,16,20]
- Connection monitoring and automatic error recovery implementations

Shimmer GSR Streaming:
- Modular import handling with graceful library fallback [18]
- Real-time data streaming with timestamp synchronisation
- Quality monitoring and connection health assessment

### H.2.2 Source Code Repository Structure

Python Controller (`/PythonApp/`):
- Main desktop application and synchronisation logic [17]
- Master clock synchronisation implementation
- Device management and session coordination
- Data processing and export functionality [23,24]

Android Application (`/AndroidApp/`):
- Mobile sensor integration and data collection [13]
- Camera interface and thermal sensor management [16,20]
- Network communication and local data storage
- User interface for research technicians

System Configuration (`/config/`):
- Configuration files and calibration data
- Network topology and security settings [21]
- Sensor calibration parameters and validation protocols

Documentation (`/docs/`):
- Technical specifications and user guides
- API documentation and protocol specifications
- Ethics approval and risk assessment documentation

## H.3 Repository Structure and Build Instructions

### H.3.1 Repository Components

Build Configuration:
- Android Application: `build.gradle` (Gradle 8.2), minSdk 26, targetSdk 34 [13]
- Python Environment: `environment.yml` (Conda), requires Python 3.10+
- Dependencies: `requirements.txt` and `test-requirements.txt` for Python packages [18]

Core Components:
- Python Controller: `PythonApp/src/controller/MainWindow.py` (487 lines) [17]
- Android App: `AndroidApp/app/src/main/` (Kotlin implementation)
- Documentation: `docs/architecture.md`, `docs/api/` (OpenAPI specifications)
- Testing Framework: `tests/` with comprehensive unit and integration tests

### H.3.2 Build Commands

Android Application Build:
```bash
cd AndroidApp
./gradlew assembleDebug
# Requires Android SDK 34, validated on Samsung Galaxy S22+ [13]
```

Python Environment Setup:
```bash
conda env create -f environment.yml
# Alternative: pip install -r requirements.txt
# Requires Python 3.10+, OpenCV, PyQt6 [17,18,22]
```

Development Environment:
```bash
# Install development dependencies
pip install -r test-requirements.txt
# Run test suite
pytest tests/ --coverage
# Expected coverage: >95% across core modules
```

## H.4 Hardware Specifications and Configuration Files

### H.4.1 Tested Hardware Configuration

Desktop Controller:
- Development Platform: ThinkPad T480s, Ubuntu 22.04, 16GB RAM
- Production Recommendation: Intel Core i7-10700K, 32GB RAM, NVMe SSD [17]
- Network: Gigabit Ethernet preferred, 802.11ac WiFi minimum [19,21]

Mobile Devices:
- Primary: Samsung Galaxy S22 (Android 15) [13]
- Requirements: Camera2 API, USB-C OTG, 6GB+ RAM [13,16]

Sensor Hardware:
- Thermal Camera: TopDon TC001 (320×240, 25° FOV, USB-C) [16,20]
- GSR Sensor: Shimmer3 GSR+ v4.1 firmware (Bluetooth 2.1+EDR) [8,15]
- Calibration Equipment: Fluke 4180 IR calibrator, precision resistor arrays

### H.4.2 Configuration Files

Network Configuration:
- NTP Server: `config/chrony.conf` (pool 2.android.pool.ntp.org) [21]
- Network Settings: `config/network_topology.json` (IP ranges, ports 8080-8089)
- Security: TLS 1.3 certificates and device authentication [21]

Sensor Calibration:
- Camera Parameters: `calibration/camera_params.yaml` (OpenCV format) [22]
- Thermal Calibration: Monthly recalibration using NIST-traceable standards
- GSR Validation: Precision resistor arrays (0.25-4.0 μS range) [7]

## H.5 Test Data and Validation Results

### H.5.1 Sample Datasets (Anonymised)

Validation Collections:
- Timing Precision: `results/validation_sessions/` (14 synchronisation tests)
- Calibration Accuracy: `results/calibration_accuracy/` (checkerboard validation) [22]
- Network Performance: `results/network_performance/` (latency, packet loss logs) [21]
- User Experience: `results/usability_studies/` (SUS evaluations, task timing) [10]

Data Format Standards:
- Session Metadata: JSON schemas with device information and quality metrics [23]
- Thermal Data: Binary format with 64-byte headers and CRC32 checksums
- GSR Data: CSV with timestamp synchronisation and quality flags [8]
- Video Data: H.264 compression with synchronised audio tracks [13,22]

### H.5.2 Reproducibility Verification

Synchronisation Results Replication:
```bash
# To replicate synchronisation accuracy results:
python PythonApp/test_sync_accuracy.py
# Expected output: ~2.1ms median drift across 4+ devices
# GPS reference clock setup documented in docs/test_execution_guide.md
```

Performance Benchmarking:
```bash
# Network performance validation:
python evaluation_suite/performance/network_latency_test.py
# Expected: <12.3ms RTT, <0.001% packet loss [21]

# Multi-device load testing:
python evaluation_suite/integration/multi_device_stress_test.py  
# Expected: 12 device capacity, 45.2 MB/s throughput
```

User Experience Validation:
- SUS Score Replication: Methodology documented in `docs/usability_evaluation_protocol.md` [10]
- Task Timing Studies: Standardised workflow scripts in `evaluation_suite/usability/`
- Expected Results: 4.9/5.0 SUS score, 8.2-minute setup time, 100% recommendation rate

## H.6 Integration with External Systems

### H.6.1 Analysis Software Compatibility

Supported Analysis Platforms:
- MATLAB: Export via .mat files with preserved timestamps [24]
- Python: NumPy arrays, pandas DataFrames, SciPy signal processing [22,24]
- R: CSV export with statistical analysis templates
- SPSS: Formatted datasets with variable labels and metadata

Machine Learning Integration:
- TensorFlow/PyTorch: Standardised tensor formats for deep learning [5]
- Scikit-learn: Feature extraction pipelines for contactless GSR prediction [10]
- OpenCV: Computer vision preprocessing for rPPG signal extraction [22]

### H.6.2 Research Infrastructure Compatibility

Laboratory Information Management:
- Ethics Compliance: Integration with UCL UCLIC approval workflows
- Data Protection: GDPR-compliant anonymisation and retention policies [3]
- Quality Assurance: Automated validation reports for research auditing

Multi-Site Deployment:
- Standardised Protocols: Reproducible setup across different laboratories [4]
- Remote Monitoring: Cloud-based quality assessment and technical support
- Version Control: Git-based configuration management for consistent deployments

## H.7 Cross-Reference to Consolidated Visual Content

Primary Visual Content Location: All figures, diagrams, tables, and visual materials are now consolidated in Appendix Z: Consolidated Figures, Diagrams, and Visual Content.

Quick Navigation Guide:
- Chapter 2 Background Figures: Appendix Z, Sections Z.1.1-Z.1.5
- Chapter 3 Architecture Diagrams: Appendix Z, Sections Z.2.1-Z.2.5  
- Chapter 4 Implementation Figures: Appendix Z, Sections Z.3.1-Z.3.3
- Performance Analysis Charts: Appendix Z, Sections Z.4.1-Z.4.5
- Cross-Reference Tables: Appendix Z, Section Z.5

This consolidated index provides quick navigation to all technical content throughout the thesis while maintaining the single source of truth for visual materials in Appendix Z. The comprehensive cross-referencing enables efficient examination and supports the reproducibility objectives of the research project.
