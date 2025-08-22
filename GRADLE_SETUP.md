# BucikaGSR Gradle Build Setup

This document describes the Gradle build setup for the BucikaGSR Android application.

## Overview

The BucikaGSR project has been configured with a comprehensive Gradle build system that includes:

- **Unified dependency management** across all modules
- **Standardized build configurations** for Java 17 and modern Android development  
- **Product flavor consistency** across all modules
- **Build validation and optimization** scripts
- **Shared app setup** for easier maintenance

## Project Structure

```
BucikaGSR/
├── app/                          # Main Android application module
├── BleModule/                    # Bluetooth Low Energy module for GSR sensors
├── component/
│   ├── CommonComponent/          # Shared UI components
│   └── thermal-ir/              # Thermal imaging components
├── lib*/                        # Core library modules (libapp, libcom, libir, etc.)
├── LocalRepo/                   # Local AAR dependencies
├── RangeSeekBar/                # Custom UI component
├── commonlibrary/               # Shared utilities
├── build.gradle                 # Root build configuration
├── settings.gradle              # Module inclusion settings
├── depend.gradle               # Global dependency versions
├── shared.gradle                 # Shared configuration script
└── gradle.properties           # Global Gradle properties
```

## Key Build Files

### Root Configuration Files

1. **`build.gradle`** - Main build script with:
   - Buildscript dependencies (Kotlin 2.0, Android Gradle Plugin 8.1.4)
   - Global repository configuration
   - Custom validation and build tasks

2. **`depend.gradle`** - Global configuration including:
   - Android SDK versions (compileSdk: 35, minSdk: 24, targetSdk: 35)
   - App versioning (versionCode: 1100, versionName: "1.10.000")
   - Product flavor configurations (dev, beta, prod, etc.)

3. **`shared.gradle`** - Shared setup providing:
   - Version management for all dependencies
   - Common module configurations
   - Build optimization settings
   - Validation functions

### Module Build Scripts

All module `build.gradle` files have been standardized with:
- Java 17 compatibility
- Unified plugin configuration (Android, Kotlin, KSP)
- Consistent product flavors across all modules
- Modern build features (ViewBinding, DataBinding, BuildConfig)

## Product Flavors

The project supports multiple product flavors:

- **`dev`** - Development builds with debugging enabled
- **`beta`** - Beta testing builds  
- **`prod`** - Production builds for global markets
- **`prodTopdon`** - Production builds for Android 10 compatibility
- **`insideChina`** - Builds for Chinese market
- **`prodTopdonInsideChina`** - Chinese market builds with Android 10 support

## Available Gradle Tasks

### Standard Tasks
```bash
./gradlew assembleDevDebug        # Build debug version
./gradlew assembleDevRelease      # Build release version
./gradlew assembleBetaDebug       # Build beta debug
./gradlew assembleBetaRelease     # Build beta release
```

### BucikaGSR Custom Tasks  
```bash
./gradlew validateBuild           # Validate all modules are properly configured
./gradlew assembleAllFlavors      # Assemble all product flavors
./gradlew testAllModules          # Run tests for all modules
./gradlew clean                   # Clean all build artifacts
```

### Validation Script
```bash
./validate_setup.sh               # Comprehensive setup validation
```

## Dependencies

### Core Dependencies
- **Android Gradle Plugin**: 8.1.4
- **Kotlin**: 2.0.0 with KSP support
- **Target SDK**: 35 (Android 14)
- **Min SDK**: 24 (Android 7.0)

### Key Libraries
- **AndroidX Core/AppCompat**: Latest stable versions
- **Material Design**: 1.5.0
- **Retrofit**: 2.9.0 for networking
- **Glide**: 4.16.0 for image loading
- **RxJava2**: For reactive programming (GSR data processing)
- **EventBus**: 3.2.0 for inter-module communication
- **GSY Video Player**: v8.1.4 for video processing

### GSR/BLE Specific
- **BLE Module**: Custom Bluetooth Low Energy integration
- **Shimmer GSR**: Hardware sensor integration (via local AAR)
- **XLog**: 1.10.1 for advanced logging
- **RxPermissions**: 0.9.5 for permission handling

## Building the Project

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17 or later
- Android SDK 35
- NDK 21.3.6528147 (for native libraries)

### Quick Start
1. Clone the repository
2. Run validation: `./validate_setup.sh`
3. Build: `./gradlew assembleDevDebug`

### Development Workflow
1. **Validation**: Always run `./gradlew validateBuild` after major changes
2. **Development builds**: Use `dev` flavor for testing
3. **Release builds**: Use `prod` flavor for distribution

## Module Dependencies

```
app
├── component:thermal-ir
├── libapp (core application library)
├── libcom (communication utilities)  
├── libir (infrared/thermal imaging)
├── libmenu (UI menus)
├── libui (UI components)
└── BleModule (Bluetooth GSR)

BleModule
└── libapp (for LMS SDK integration)

component:thermal-ir
├── libapp, libcom, libir, libmenu, libui
├── component:CommonComponent  
└── OpenGL/native libraries

libapp (foundation module)
├── Room database (2.4.2)
├── Retrofit networking
├── RxJava reactive streams
├── AndroidX lifecycle components
└── Local AAR dependencies
```

## Troubleshooting

### Build Failures
1. Run `./validate_setup.sh` to check for missing files
2. Clean build: `./gradlew clean`
3. Check Java version: `java -version` (should be 17+)
4. Verify Android SDK: `$ANDROID_HOME/platform-tools/adb --version`

### Common Issues
- **ARouter errors**: ARouter has been replaced with ModernRouter (internal)
- **NDK issues**: Ensure NDK 21.3.6528147 is installed
- **Dependency conflicts**: Check `shared.gradle` for version management

### Validation Errors
If `./gradlew validateBuild` fails:
1. Check that all modules listed in `settings.gradle` have `build.gradle` files
2. Verify global properties in `depend.gradle`
3. Ensure consistent product flavors across modules

## Performance Optimizations

The build system includes several optimizations:
- **Incremental compilation** for faster builds
- **Build cache** enabled
- **Parallel execution** for multi-module builds
- **NDK optimization** with selective ABI filters (arm64-v8a priority)

## Maintenance

### Adding New Modules
1. Create module directory with `build.gradle`
2. Add to `settings.gradle` 
3. Apply common configuration from `shared.gradle`
4. Run `./gradlew validateBuild` to confirm

### Updating Dependencies
1. Update versions in `shared.gradle`
2. Test with `./gradlew assembleDevDebug`
3. Run validation to ensure consistency

### Product Flavor Changes
1. Update all module `build.gradle` files consistently
2. Update `depend.gradle` for flavor-specific configurations
3. Test all flavors: `./gradlew assembleAllFlavors`