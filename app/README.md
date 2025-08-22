# Bucika GSR Application

This directory contains the complete Bucika GSR (Galvanic Skin Response) multi-sensor synchronized recording system, migrated from the [buccancs/bucika_gsr](https://github.com/buccancs/bucika_gsr) repository.

## Structure

- **AndroidApp/**: Android application for mobile sensor recording
- **PythonApp/**: Python desktop application for sensor coordination and analysis

## Building

This is a multi-module Gradle project that supports both Android and Python components.

### Prerequisites

- Android SDK (API 24-35)
- Python 3.10+
- Gradle 8.0+

### Build Commands

```bash
# Validate configuration
./gradlew validateConfiguration

# Build all modules
./gradlew build

# Clean build artifacts
./gradlew clean
```

### Android App

The Android application is built using:
- Kotlin
- Android SDK 35
- Hilt for dependency injection
- Firebase for cloud services

### Python App

The Python application includes:
- Sensor management and coordination
- Cross-device calibration
- Real-time data processing
- Web UI for monitoring

## Configuration

Project configuration is managed through `gradle.properties` and individual module build files.

Key configuration values:
- `android.applicationId`: com.multisensor.recording
- `python.version`: >=3.10
- `project.version`: 1.0.0