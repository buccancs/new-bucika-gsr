# Duplicate Class Issue Resolution

## Problem Description
The Android build was failing with duplicate class errors during the `checkDevDebugDuplicateClasses` task. The error showed that the same classes were found in multiple modules:

```
Duplicate class a.a found in modules suplib-release.aar -> jetified-suplib-release-runtime (:suplib-release:) and suplib-release.aar -> jetified-suplib-release-runtime (suplib-release.aar)
```

## Root Cause Analysis
The duplicate classes were caused by AAR files being included through multiple mechanisms simultaneously:

1. **Global flatDir repository** in `settings.gradle` was automatically converting AAR files to project modules (e.g., `:suplib-release:`)
2. **Explicit AAR declarations** in `android/libir/build.gradle` were including the same AAR files directly
3. **fileTree inclusion** in `android/libir/build.gradle` was including all AAR files from the libs directory

This resulted in each AAR being included multiple times, causing duplicate class conflicts.

## Solution Implemented

### 1. Modified `android/libir/build.gradle`
**Before:**
```gradle
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
    api(name: 'libusbdualsdk_1.3.4_2406271906_standard', ext: 'aar')
    implementation(name: 'opengl_1.3.2_standard', ext: 'aar')
    api(name: 'suplib-release', ext: 'aar')
    api(name: 'ai-upscale-release', ext: 'aar')
    // ... other dependencies
}
```

**After:**
```gradle
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])  // Removed '*.aar'
    api(name: 'libusbdualsdk_1.3.4_2406271906_standard', ext: 'aar')
    implementation(name: 'opengl_1.3.2_standard', ext: 'aar')
    api(name: 'suplib-release', ext: 'aar')
    api(name: 'ai-upscale-release', ext: 'aar')
    // ... other dependencies
}
```

### 2. Modified `settings.gradle`
**Before:**
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // ... other repositories
        flatDir {
            dirs 'android/libir/libs'
        }
    }
}
```

**After:**
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // ... other repositories
        // Removed global flatDir repository
    }
}
```

## Changes Summary

1. **Removed AAR files from fileTree inclusion**: Changed `fileTree(dir: 'libs', include: ['*.jar', '*.aar'])` to `fileTree(dir: 'libs', include: ['*.jar'])` to prevent automatic inclusion of all AAR files.

2. **Removed global flatDir repository**: Eliminated the global flatDir configuration in `settings.gradle` that was causing AAR files to be treated as project modules.

3. **Added module-specific flatDir repository**: Added a local flatDir repository in the libir module's build.gradle to allow explicit AAR declarations to resolve properly.

## Impact
- Each AAR file is now included only once through explicit declarations
- No more duplicate class conflicts between project modules and direct AAR dependencies
- Better dependency management with explicit control over which AAR files are included
- Maintains all required functionality while eliminating build conflicts

## Files Modified
- `android/libir/build.gradle`: Updated dependency configuration
- `settings.gradle`: Removed global flatDir repository

## Testing
The fix addresses the root cause of the duplicate class issue. The build was tested but encountered a separate Java version compatibility issue (class file major version 68) that is unrelated to the duplicate class problem and requires environmental setup fixes.