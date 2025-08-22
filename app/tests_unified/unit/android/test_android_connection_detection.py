"""
Simple Test Runner for Android Connection Detection
==================================================

Basic test runner to validate the Android connection detection functionality
without requiring pytest.
"""

import sys
import time
import traceback
from pathlib import Path

# Add the project root to the path for imports
sys.path.insert(0, str(Path(__file__).parent))

def run_test(test_func, test_name):
    """Run a single test function."""
    try:
        print(f"[TEST] {test_name}... ", end="")
        test_func()
        print("[PASS]")
        return True
    except Exception as e:
        print(f"[FAIL] {e}")
        if "--verbose" in sys.argv:
            traceback.print_exc()
        return False

def test_android_connection_detector_import():
    """Test that AndroidConnectionDetector can be imported."""
    from PythonApp.utils.android_connection_detector import (
        AndroidConnectionDetector,
        AndroidDevice,
        IDEConnection,
        ConnectionType,
        IDEType
    )
    
    # Test enum values
    assert ConnectionType.USB.value == "usb"
    assert ConnectionType.WIRELESS_ADB.value == "wireless_adb"
    assert IDEType.ANDROID_STUDIO.value == "android_studio"
    assert IDEType.INTELLIJ_IDEA.value == "intellij_idea"

def test_android_connection_detector_initialization():
    """Test AndroidConnectionDetector initialization."""
    from PythonApp.utils.android_connection_detector import AndroidConnectionDetector
    
    detector = AndroidConnectionDetector()
    assert detector is not None
    assert hasattr(detector, 'detected_devices')
    assert hasattr(detector, 'ide_connections')
    assert detector.detected_devices == {}
    assert detector.ide_connections == {}

def test_connection_type_determination():
    """Test connection type determination logic."""
    from PythonApp.utils.android_connection_detector import AndroidConnectionDetector
    
    detector = AndroidConnectionDetector()
    
    # Test USB device ID
    usb_type = detector._determine_connection_type("ABC123456789")
    assert usb_type.value == "usb"
    
    # Test wireless device ID
    wireless_type = detector._determine_connection_type("192.168.1.100:5555")
    assert wireless_type.value == "wireless_adb"
    
    # Test emulator device ID
    emulator_type = detector._determine_connection_type("emulator-5554")
    assert emulator_type.value == "usb"

def test_wireless_connection_parsing():
    """Test parsing of wireless connection details."""
    from PythonApp.utils.android_connection_detector import AndroidConnectionDetector
    
    detector = AndroidConnectionDetector()
    
    # Valid wireless connection
    ip_port = detector._parse_wireless_connection("192.168.1.100:5555")
    assert ip_port == ("192.168.1.100", 5555)
    
    # Invalid format
    invalid = detector._parse_wireless_connection("ABC123456789")
    assert invalid is None

def test_enhanced_android_device_manager_import():
    """Test that enhanced AndroidDeviceManager can be imported."""
    from PythonApp.network.android_device_manager import AndroidDeviceManager, AndroidDevice
    
    device = AndroidDevice(
        device_id="test_device",
        capabilities=["test"],
        connection_time=time.time(),
        last_heartbeat=time.time()
    )
    
    # Check enhanced fields exist
    assert hasattr(device, 'connection_type')
    assert hasattr(device, 'wireless_debugging_enabled')
    assert hasattr(device, 'ide_connections')
    assert hasattr(device, 'ip_address')
    assert hasattr(device, 'port')

def test_android_device_manager_initialization():
    """Test AndroidDeviceManager initialization with connection detection."""
    from PythonApp.network.android_device_manager import AndroidDeviceManager
    
    manager = AndroidDeviceManager(server_port=9010)
    
    try:
        assert manager is not None
        assert hasattr(manager, 'connection_detector')
        assert hasattr(manager, 'connection_detection_enabled')
        
        # Test detection status
        status = manager.get_connection_detection_status()
        assert isinstance(status, dict)
        assert 'available' in status
        assert 'detector_initialized' in status
        assert 'adb_available' in status
        assert 'supported_features' in status
        
    finally:
        # Clean up
        if hasattr(manager, 'is_initialized') and manager.is_initialized:
            manager.shutdown()

def test_android_device_manager_enhanced_methods():
    """Test enhanced methods in AndroidDeviceManager."""
    from PythonApp.network.android_device_manager import AndroidDeviceManager, AndroidDevice
    
    manager = AndroidDeviceManager(server_port=9011)
    
    try:
        # Test methods exist and return appropriate types
        wireless_devices = manager.get_wireless_debugging_devices()
        assert isinstance(wireless_devices, list)
        
        ide_devices = manager.get_ide_connected_devices()
        assert isinstance(ide_devices, dict)
        
        # These methods return False/empty list when detection is not available
        wireless_result = manager.is_device_wireless_connected("test")
        assert wireless_result is False or isinstance(wireless_result, bool)
        
        ide_result = manager.is_device_ide_connected("test")
        assert isinstance(ide_result, list)
        
        # Test with a mock device
        device = AndroidDevice(
            device_id="test_device",
            capabilities=["test"],
            connection_time=time.time(),
            last_heartbeat=time.time(),
            connection_type="wireless_adb",
            wireless_debugging_enabled=True,
            ide_connections=["android_studio"]
        )
        manager.android_devices["test_device"] = device
        
        info = manager.get_enhanced_device_info("test_device")
        assert info is not None
        assert info['device_id'] == "test_device"
        
        if manager.connection_detection_enabled:
            assert 'connection_type' in info
            assert 'wireless_debugging_enabled' in info
            assert 'ide_connections' in info
        
    finally:
        # Clean up
        if hasattr(manager, 'is_initialized') and manager.is_initialized:
            manager.shutdown()

def test_demo_script_execution():
    """Test that the demo script can be imported without errors."""
    import subprocess
    import sys
    
    # Test help output
    result = subprocess.run([
        sys.executable, "android_connection_demo.py", "--help"
    ], capture_output=True, text=True, timeout=10)
    
    # Should not crash and should show help
    assert result.returncode == 0
    assert "Android Connection Detection Demo" in result.stdout

def main():
    """Run all tests."""
    print("=" * 60)
    print("ANDROID CONNECTION DETECTION TESTS")
    print("=" * 60)
    
    tests = [
        (test_android_connection_detector_import, "AndroidConnectionDetector Import"),
        (test_android_connection_detector_initialization, "AndroidConnectionDetector Initialization"),
        (test_connection_type_determination, "Connection Type Determination"),
        (test_wireless_connection_parsing, "Wireless Connection Parsing"),
        (test_enhanced_android_device_manager_import, "Enhanced AndroidDeviceManager Import"),
        (test_android_device_manager_initialization, "AndroidDeviceManager Initialization"),
        (test_android_device_manager_enhanced_methods, "AndroidDeviceManager Enhanced Methods"),
        (test_demo_script_execution, "Demo Script Execution"),
    ]
    
    passed = 0
    failed = 0
    
    for test_func, test_name in tests:
        if run_test(test_func, test_name):
            passed += 1
        else:
            failed += 1
    
    print("\n" + "=" * 60)
    print(f"TEST RESULTS: {passed} passed, {failed} failed")
    print("=" * 60)
    
    if failed == 0:
        print("\n[SUCCESS] All tests passed!")
        return 0
    else:
        print(f"\n[FAIL] {failed} test(s) failed.")
        return 1

if __name__ == "__main__":
    sys.exit(main())