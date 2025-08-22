#!/bin/bash

# BucikaGSR Build Setup Validation Script
# This script validates that all required Gradle files and configurations are in place

echo "=== BucikaGSR Build Setup Validation ==="

# Check required files
REQUIRED_FILES=(
    "build.gradle"
    "settings.gradle"  
    "depend.gradle"
    "consolidate_app_setup.gradle"
    "gradle.properties"
    "gradlew"
    "gradlew.bat"
    "gradle/wrapper/gradle-wrapper.jar"
    "gradle/wrapper/gradle-wrapper.properties"
)

echo "Checking required Gradle files..."
MISSING_FILES=()

for file in "${REQUIRED_FILES[@]}"; do
    if [[ -f "$file" ]]; then
        echo "✓ $file"
    else
        echo "✗ $file (MISSING)"
        MISSING_FILES+=("$file")
    fi
done

# Check required module build.gradle files
REQUIRED_MODULES=(
    "app/build.gradle"
    "libapp/build.gradle"
    "libcom/build.gradle"
    "libir/build.gradle"
    "libmenu/build.gradle"
    "libui/build.gradle"
    "BleModule/build.gradle"
    "component/thermal-ir/build.gradle"
    "component/CommonComponent/build.gradle"
    "LocalRepo/libcommon/build.gradle"
    "LocalRepo/libirutils/build.gradle"
    "RangeSeekBar/build.gradle"
    "commonlibrary/build.gradle"
)

echo ""
echo "Checking module build files..."
for module in "${REQUIRED_MODULES[@]}"; do
    if [[ -f "$module" ]]; then
        echo "✓ $module"
    else
        echo "✗ $module (MISSING)"
        MISSING_FILES+=("$module")
    fi
done

# Check if any files are missing
if [[ ${#MISSING_FILES[@]} -eq 0 ]]; then
    echo ""
    echo "✅ All required files are present!"
    echo ""
    echo "Running build validation..."
    ./gradlew validateBuild --no-daemon
    
    if [[ $? -eq 0 ]]; then
        echo "✅ Build validation successful!"
        echo ""
        echo "Available tasks:"
        echo "  ./gradlew assembleDevDebug     - Build debug version"
        echo "  ./gradlew assembleAllFlavors   - Build all product flavors"  
        echo "  ./gradlew testAllModules       - Run all tests"
        echo "  ./gradlew validateBuild        - Validate configuration"
    else
        echo "❌ Build validation failed. Check the output above for details."
        exit 1
    fi
else
    echo ""
    echo "❌ Missing files detected:"
    for file in "${MISSING_FILES[@]}"; do
        echo "  - $file"
    done
    echo ""
    echo "Please ensure all required files are present before building."
    exit 1
fi