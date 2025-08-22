#!/usr/bin/env python3
"""
Test script to verify Android app face blurring removal for hands-only video processing.

This script validates that the Android app has been updated to:
1. Disable face blurring by default (since video contains only hands)
2. Update UI text to reflect hands-only video processing
3. Maintain backward compatibility for face detection features

Usage:
    python test_android_face_blurring_removal.py
"""

import os
import re
import sys
from pathlib import Path

def check_file_exists(file_path):
    """Check if a file exists and is readable."""
    if not os.path.exists(file_path):
        print(f"[FAIL] ERROR: File not found: {file_path}")
        return False
    return True

def check_kotlin_face_blurring_defaults():
    """Check that PrivacyManager.kt has face blurring disabled by default."""
    privacy_manager_path = "AndroidApp/src/main/java/com/multisensor/recording/security/PrivacyManager.kt"
    
    if not check_file_exists(privacy_manager_path):
        return False
    
    print(f"[FOLDER] Checking Android PrivacyManager configuration...")
    
    with open(privacy_manager_path, 'r') as f:
        content = f.read()
    
    # Check that face blurring defaults to false
    expected_patterns = [
        r'\.putBoolean\(KEY_FACE_BLURRING,\s*false\)\s*//.*hands',
        r'securePrefs\.getBoolean\(KEY_FACE_BLURRING,\s*false\).*hands',
        r'return\s+securePrefs\.getBoolean\(KEY_FACE_BLURRING,\s*false\).*hands'
    ]
    
    success = True
    for pattern in expected_patterns:
        if not re.search(pattern, content, re.IGNORECASE):
            print(f"[FAIL] FAIL: Expected pattern not found: {pattern}")
            success = False
        else:
            print(f"[PASS] PASS: Found correct face blurring default setting")
    
    # Check that comments reference hands-only video
    if "hands-only" not in content.lower():
        print(f"[FAIL] FAIL: Missing 'hands-only' reference in comments")
        success = False
    else:
        print(f"[PASS] PASS: Found 'hands-only' reference in comments")
    
    return success

def check_ui_text_updates():
    """Check that UI text has been updated for hands-only video."""
    ui_files = [
        "AndroidApp/src/main/res/layout/dialog_privacy_settings.xml",
        "AndroidApp/src/main/res/layout/dialog_consent.xml",
        "AndroidApp/src/main/java/com/multisensor/recording/ui/privacy/ConsentDialog.kt"
    ]
    
    success = True
    
    for ui_file in ui_files:
        if not check_file_exists(ui_file):
            success = False
            continue
        
        print(f"[FOLDER] Checking UI file: {ui_file}")
        
        with open(ui_file, 'r') as f:
            content = f.read()
        
        # Check for hands-only or hands references
        if "hands-only" not in content.lower() and "hands" not in content.lower():
            print(f"[FAIL] FAIL: No hands-only reference found in {ui_file}")
            success = False
        else:
            print(f"[PASS] PASS: Found hands reference in {ui_file}")
    
    return success

def check_backward_compatibility():
    """Check that face detection features are still available for explicit use."""
    privacy_manager_path = "AndroidApp/src/main/java/com/multisensor/recording/security/PrivacyManager.kt"
    
    if not check_file_exists(privacy_manager_path):
        return False
    
    print(f"[FOLDER] Checking backward compatibility...")
    
    with open(privacy_manager_path, 'r') as f:
        content = f.read()
    
    # Check that face detection methods still exist
    required_methods = [
        "applyFaceBlurring",
        "isFaceBlurringEnabled",
        "initializeFaceDetection"
    ]
    
    success = True
    for method in required_methods:
        if method not in content:
            print(f"[FAIL] FAIL: Missing required method: {method}")
            success = False
        else:
            print(f"[PASS] PASS: Found method: {method}")
    
    return success

def check_security_features_preserved():
    """Check that other security features remain enabled."""
    privacy_manager_path = "AndroidApp/src/main/java/com/multisensor/recording/security/PrivacyManager.kt"
    
    if not check_file_exists(privacy_manager_path):
        return False
    
    print(f"[FOLDER] Checking other security features...")
    
    with open(privacy_manager_path, 'r') as f:
        content = f.read()
    
    # Check that other security features default to true
    security_features = [
        "KEY_ENCRYPT_DATA_AT_REST",
        "KEY_SECURE_FILE_DELETION", 
        "KEY_LOG_SECURITY_EVENTS"
    ]
    
    success = True
    for feature in security_features:
        # Look for the feature being set to true
        pattern = rf'\.putBoolean\({feature},\s*true\)'
        if not re.search(pattern, content):
            print(f"[FAIL] FAIL: Security feature not enabled by default: {feature}")
            success = False
        else:
            print(f"[PASS] PASS: Security feature enabled: {feature}")
    
    return success

def main():
    """Main test function."""
    print("[DEPLOY] Starting Android app face blurring removal validation...")
    print("=" * 60)
    
    # Change to repository root
    repo_root = Path(__file__).parent
    os.chdir(repo_root)
    
    test_results = []
    
    # Run all tests
    test_results.append(("Kotlin face blurring defaults", check_kotlin_face_blurring_defaults()))
    test_results.append(("UI text updates", check_ui_text_updates()))
    test_results.append(("Backward compatibility", check_backward_compatibility()))
    test_results.append(("Security features preserved", check_security_features_preserved()))
    
    # Print summary
    print("\n" + "=" * 60)
    print("[CLIPBOARD] TEST SUMMARY")
    print("=" * 60)
    
    passed = 0
    total = len(test_results)
    
    for test_name, result in test_results:
        status = "[PASS] PASS" if result else "[FAIL] FAIL"
        print(f"{status}: {test_name}")
        if result:
            passed += 1
    
    print(f"\nResults: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n[SUCCESS] SUCCESS: All Android face blurring removal tests passed!")
        print("\nThe Android app has been successfully updated for hands-only video processing:")
        print("* Face blurring disabled by default (no faces in video)")
        print("* UI text updated to reflect hands-only video processing")
        print("* Backward compatibility maintained for face detection features")
        print("* Other security features remain enabled")
        return True
    else:
        print(f"\n[FAIL] FAILURE: {total - passed} test(s) failed!")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)