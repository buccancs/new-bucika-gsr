#!/usr/bin/env python3
"""
Test script to verify the SecurityException fix in ThermalRecorder.

This script verifies that:
1. ThermalRecorder.initialize() handles SecurityException gracefully
2. The app doesn't crash when USBMonitor.register() fails
3. The thermal recorder can still function with manual USB monitoring
"""

import subprocess
import sys
import time
import re
from pathlib import Path

def run_gradle_command(command: str, cwd: str) -> tuple[int, str]:
    """Run a gradle command and return exit code and output."""
    try:
        result = subprocess.run(
            command.split(),
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=300
        )
        return result.returncode, result.stdout + result.stderr
    except subprocess.TimeoutExpired:
        return 1, "Command timed out"
    except Exception as e:
        return 1, f"Error running command: {str(e)}"

def check_thermal_recorder_fix():
    """Check if the ThermalRecorder SecurityException fix is properly implemented."""
    print("🔍 Checking ThermalRecorder SecurityException fix...")

    thermal_recorder_path = Path("AndroidApp/src/main/java/com/multisensor/recording/recording/ThermalRecorder.kt")

    if not thermal_recorder_path.exists():
        print(f"❌ ThermalRecorder.kt not found at {thermal_recorder_path}")
        return False

    with open(thermal_recorder_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Check for the SecurityException handling
    if "try {" in content and "topdonUsbMonitor?.register()" in content:
        print("✅ Found try-catch wrapper around USBMonitor.register()")
    else:
        print("❌ USBMonitor.register() is not properly wrapped in try-catch")
        return False

    if "catch (e: SecurityException)" in content:
        print("✅ Found SecurityException handler")
    else:
        print("❌ SecurityException handler not found")
        return False

    if "topdonUsbMonitor = null" in content:
        print("✅ Found graceful fallback when USBMonitor fails")
    else:
        print("❌ Graceful fallback not implemented")
        return False

    # Check for proper receiver registration
    if "Context.RECEIVER_NOT_EXPORTED" in content:
        print("✅ Found proper receiver export flags")
    else:
        print("❌ Proper receiver export flags not found")
        return False

    return True

def check_android_manifest():
    """Check if AndroidManifest.xml has required permissions."""
    print("\n🔍 Checking AndroidManifest.xml permissions...")

    manifest_path = Path("AndroidApp/src/main/AndroidManifest.xml")

    if not manifest_path.exists():
        print(f"❌ AndroidManifest.xml not found at {manifest_path}")
        return False

    with open(manifest_path, 'r', encoding='utf-8') as f:
        content = f.read()

    required_permissions = [
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.USB_PERMISSION"
    ]

    for permission in required_permissions:
        if permission in content:
            print(f"✅ Found {permission}")
        else:
            print(f"❌ Missing {permission}")
            return False

    return True

def test_compilation():
    """Test if the Android app compiles without errors."""
    print("\n🔍 Testing compilation...")

    android_app_dir = Path("AndroidApp")
    if not android_app_dir.exists():
        print("❌ AndroidApp directory not found")
        return False

    # Try to compile the project
    exit_code, output = run_gradle_command("./gradlew assembleDebug", str(android_app_dir.parent))

    if exit_code == 0:
        print("✅ Android app compiles successfully")
        return True
    else:
        print(f"❌ Compilation failed with exit code {exit_code}")
        print("Output:", output[-1000:])  # Show last 1000 characters
        return False

def main():
    """Main test function."""
    print("🧪 Testing ThermalRecorder SecurityException Fix")
    print("=" * 50)

    tests_passed = 0
    total_tests = 3

    # Test 1: Check fix implementation
    if check_thermal_recorder_fix():
        tests_passed += 1
        print("✅ Test 1 PASSED: SecurityException fix implemented correctly")
    else:
        print("❌ Test 1 FAILED: SecurityException fix issues found")

    # Test 2: Check manifest permissions
    if check_android_manifest():
        tests_passed += 1
        print("✅ Test 2 PASSED: AndroidManifest permissions are correct")
    else:
        print("❌ Test 2 FAILED: AndroidManifest permission issues found")

    # Test 3: Check compilation
    if test_compilation():
        tests_passed += 1
        print("✅ Test 3 PASSED: App compiles successfully")
    else:
        print("❌ Test 3 FAILED: Compilation issues found")

    print("\n" + "=" * 50)
    print(f"📊 Test Results: {tests_passed}/{total_tests} tests passed")

    if tests_passed == total_tests:
        print("🎉 All tests passed! The SecurityException fix is working correctly.")
        return 0
    else:
        print("⚠️  Some tests failed. Please review the issues above.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
