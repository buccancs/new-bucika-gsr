# BucikaGSR Build and Setup Guide

This document provides comprehensive build, setup, and configuration information for the BucikaGSR Android application.

## Overview

The BucikaGSR project is a standalone thermal imaging application with GSR (Galvanic Skin Response) sensor integration. It combines thermal imaging capabilities with physiological monitoring.

## Build System

The project uses a comprehensive Gradle build system with:

- **Unified dependency management** across all modules
- **Standardized build configurations** for Java 17 and modern Android development  
- **Product flavor consistency** across all modules
- **Build validation and optimization** scripts
- **Shared app setup** for easier maintenance

## Project Structure

```
BucikaGSR/
├── android/
│   ├── app/                       # Main Android application module
│   ├── BleModule/                 # Bluetooth Low Energy module for GSR sensors
│   ├── libapp/                    # Core application library
│   ├── libcom/                    # Common utilities and dialogs
│   ├── libir/                     # Thermal imaging library
│   ├── libui/                     # UI components library
│   ├── libmenu/                   # Menu and settings library
│   └── component/                 # Reusable components
├── pc-python/                     # Python orchestrator for PC integration
├── docs/                          # Documentation
└── shared-spec/                   # Shared protocol specifications
```

## Quick Start

### Option 1: Using Build Scripts

**Linux/macOS:**
```bash
# Build debug APK
./build.sh assembleDebug

# Build release APK  
./build.sh assembleRelease

# Run tests
./build.sh test

# Validate build
./build.sh validateBuild
```

**Windows:**
```cmd
# Build debug APK
build.bat assembleDebug

# Build release APK
build.bat assembleRelease

# Run tests
build.bat test
```

### Option 2: Using Gradle Wrapper

```bash
# Clean and build
./gradlew clean assembleDebug

# Run all tests
./gradlew testAllModules

# Validate build configuration
./gradlew validateBuild

# Assemble all product flavors
./gradlew assembleAllFlavors
```

## Build Issue Resolution

### Primary Build Issues

1. **Module Dependencies** - Inter-module dependency resolution
2. **ViewBinding/DataBinding** - Configuration and resource binding issues
3. **Product Flavors** - Consistency across modules
4. **Java/Kotlin Versions** - Standardized to Java 17 and modern Kotlin

### Build Validation

The project includes automated build validation:

```bash
# Validate all modules are properly configured
./gradlew validateBuild

# Check dependency consistency
./gradlew dependencyInsight --configuration implementation --dependency androidx.core
```

## Product Flavors

All modules support these product flavors:

- **dev** - Development builds with debug features
- **beta** - Beta testing builds
- **prod** - Production builds
- **prodTopdon** - TopDon branded production builds
- **insideChina** - China-specific builds
- **prodTopdonInsideChina** - TopDon China builds

## Development Setup

1. **Prerequisites:**
   - Android Studio Arctic Fox or newer
   - JDK 17
   - Android SDK API 34
   - Git

2. **Clone and Setup:**
   ```bash
   git clone <repository-url>
   cd new-bucika-gsr
   ./setup-dev.sh
   ```

3. **Build Configuration:**
   - Uses Java 17 for all modules
   - Kotlin 2.0.21 with KSP support
   - Android Gradle Plugin 8.6.1
   - Target SDK 34, Min SDK 24

## Custom Gradle Tasks

- `validateBuild` - Validate module configurations
- `assembleAllFlavors` - Build all product flavors
- `testAllModules` - Run tests across all modules

## Troubleshooting

### Common Issues

1. **Build fails with "Module not found"**
   - Run `./gradlew validateBuild` to check module configuration
   - Ensure all modules are included in `settings.gradle`

2. **Dependency conflicts**
   - Use `./gradlew dependencyInsight` to analyze conflicts
   - Check `depend.gradle` for version consistency

3. **Java version issues**
   - Ensure JDK 17 is configured in Android Studio
   - Check `JAVA_HOME` environment variable

### Performance Optimization

The build system includes optimizations:
- Parallel builds enabled
- Gradle daemon optimization
- Build cache configuration
- Incremental compilation

For detailed troubleshooting, see the [Troubleshooting Guide](docs/TROUBLESHOOTING_GUIDE.md).