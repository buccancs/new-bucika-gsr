# new-bucika-gsr

This repository contains the Bucika GSR (Galvanic Skin Response) multi-sensor synchronized recording system, migrated from the original [buccancs/bucika_gsr](https://github.com/buccancs/bucika_gsr) repository.

## Structure

- **app/**: Complete Bucika GSR application with Android and Python components
  - **AndroidApp/**: Android mobile application for sensor recording
  - **PythonApp/**: Python desktop application for sensor coordination and analysis

## Quick Start

### Prerequisites

- Android SDK (API 24-35)
- Python 3.10+
- Gradle 8.0+

### Build and Validate

```bash
# Validate project configuration
./gradlew validateConfiguration

# Build all modules
./gradlew build

# Clean build artifacts  
./gradlew clean
```

### Development

For detailed information about the application structure and development, see the [app README](app/README.md).

## Migration Notes

This repository represents a reorganized version of the original bucika_gsr project:
- Original source copied into `app/` directory as requested
- Gradle build system reorganized for multi-module support
- Proper project structure with root-level build management

## Features

- **Multi-platform**: Android mobile app + Python desktop application
- **Gradle Build System**: Unified build management for both platforms
- **Multi-sensor Support**: GSR, thermal imaging, and device synchronization
- **Real-time Processing**: Cross-device calibration and data coordination
