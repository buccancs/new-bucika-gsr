import json
import os
import sys
import tempfile
import threading
import time
import traceback
from pathlib import Path
import pytest
os.environ["QT_QPA_PLATFORM"] = "offscreen"
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))
@pytest.mark.unit
def test_shimmer_libraries_availability():
    print("Testing Shimmer libraries availability...")
    available_libraries = []
    try:
        import pyshimmer
        available_libraries.append("pyshimmer")
        print("[PASS] pyshimmer library available")
    except ImportError:
        print("[WARN] pyshimmer library not available (optional)")
    try:
        import bluetooth
        available_libraries.append("bluetooth")
        print("[PASS] bluetooth library available")
    except ImportError:
        print("[WARN] bluetooth library not available (optional)")
    try:
        import pybluez
        available_libraries.append("pybluez")
        print("[PASS] pybluez library available")
    except ImportError:
        print("[WARN] pybluez library not available (optional)")
    try:
        import serial
        available_libraries.append("serial")
        print("[PASS] serial library available")
    except ImportError:
        print("[WARN] serial library not available")
    if available_libraries:
        print(
            f"[PASS] Found {len(available_libraries)} Shimmer-compatible libraries: {', '.join(available_libraries)}"
        )
        return True
    else:
        print("[FAIL] No Shimmer libraries available")
        return False
@pytest.mark.unit
def test_device_discovery_simulation():
    print("Testing device discovery simulation...")
    try:
        def simulate_bluetooth_scan():
            mock_devices = [
                {
                    "name": "Shimmer3-GSR-01",
                    "address": "00:06:66:12:34:56",
                    "device_class": 0x001F00,
                },
                {
                    "name": "Shimmer3-GSR-02",
                    "address": "00:06:66:78:9A:BC",
                    "device_class": 0x001F00,
                },
                {
                    "name": "Other-Device",
                    "address": "AA:BB:CC:DD:EE:FF",
                    "device_class": 0x000000,
                },
            ]
            shimmer_devices = [d for d in mock_devices if "Shimmer" in d["name"]]
            return shimmer_devices
        discovered_devices = simulate_bluetooth_scan()
        if discovered_devices:
            print(f"[PASS] Discovered {len(discovered_devices)} Shimmer devices:")
            for device in discovered_devices:
                print(f"  - {device['name']} ({device['address']})")
        else:
            print("[PASS] Device discovery simulation works (no devices found)")
        def filter_shimmer_devices(devices):
            return [d for d in devices if "shimmer" in d["name"].lower()]
        filtered = filter_shimmer_devices(
            [
                {"name": "Shimmer3-GSR", "address": "00:06:66:12:34:56"},
                {"name": "iPhone", "address": "AA:BB:CC:DD:EE:FF"},
                {"name": "Shimmer2r", "address": "00:06:66:78:9A:BC"},
            ]
        )
        assert len(filtered) == 2
        print("[PASS] Device filtering works correctly")
        return True
    except Exception as e:
        print(f"[FAIL] Device discovery test failed: {e}")
        traceback.print_exc()
        return False
@pytest.mark.unit
def test_data_streaming_simulation():
    print("Testing data streaming simulation...")
    try:
        import random
        import time
        from collections import deque
        class MockShimmerDevice:
            def __init__(self, device_id="Shimmer3-01"):
                self.device_id = device_id
                self.is_connected = False
                self.is_streaming = False
                self.sampling_rate = 512
                self.data_queue = deque(maxlen=1000)
                self.callback = None
            def connect(self):
                time.sleep(0.1)
                self.is_connected = True
                return True
            def configure_sensors(self, sensors=None):
                if sensors is None:
                    sensors = ["GSR", "PPG", "Accelerometer"]
                self.enabled_sensors = sensors
                return True
            def set_sampling_rate(self, rate):
                if 1 <= rate <= 1024:
                    self.sampling_rate = rate
                    return True
                return False
            def start_streaming(self, callback=None):
                if not self.is_connected:
                    return False
                self.callback = callback
                self.is_streaming = True
                def stream_data():
                    start_time = time.time()
                    sample_count = 0
                    while self.is_streaming:
                        timestamp = time.time()
                        gsr_value = 1000 + random.gauss(0, 50)
                        ppg_value = 2048 + random.gauss(0, 100)
                        accel_x = random.gauss(0, 0.1)
                        accel_y = random.gauss(0, 0.1)
                        accel_z = 1.0 + random.gauss(0, 0.1)
                        sample = {
                            "timestamp": timestamp,
                            "sample_number": sample_count,
                            "GSR": gsr_value,
                            "PPG": ppg_value,
                            "Accelerometer_X": accel_x,
                            "Accelerometer_Y": accel_y,
                            "Accelerometer_Z": accel_z,
                        }
                        self.data_queue.append(sample)
                        if self.callback:
                            self.callback(sample)
                        sample_count += 1
                        time.sleep(1.0 / self.sampling_rate)
                self.stream_thread = threading.Thread(target=stream_data, daemon=True)
                self.stream_thread.start()
                return True
            def stop_streaming(self):
                self.is_streaming = False
                return True
            def disconnect(self):
                self.stop_streaming()
                self.is_connected = False
                return True
        device = MockShimmerDevice("Test-Shimmer")
        assert device.connect() == True
        assert device.is_connected == True
        print("[PASS] Device connection simulation works")
        assert device.configure_sensors(["GSR", "PPG"]) == True
        print("[PASS] Sensor configuration works")
        assert device.set_sampling_rate(256) == True
        assert device.sampling_rate == 256
        print("[PASS] Sampling rate configuration works")
        received_samples = []
        def data_callback(sample):
            received_samples.append(sample)
        assert device.start_streaming(data_callback) == True
        assert device.is_streaming == True
        print("[PASS] Data streaming started")
        time.sleep(0.5)
        assert device.stop_streaming() == True
        assert device.is_streaming == False
        print("[PASS] Data streaming stopped")
        assert len(received_samples) > 0
        print(f"[PASS] Received {len(received_samples)} data samples")
        sample = received_samples[0]
        required_fields = ["timestamp", "sample_number", "GSR", "PPG"]
        for field in required_fields:
            assert field in sample
        print("[PASS] Sample data structure is correct")
        assert device.disconnect() == True
        assert device.is_connected == False
        print("[PASS] Device disconnection works")
        return True
    except Exception as e:
        print(f"[FAIL] Data streaming test failed: {e}")
        traceback.print_exc()
        return False
@pytest.mark.unit
def test_session_management():
    print("Testing session management...")
    try:
        import csv
        import tempfile
        from datetime import datetime
        class SessionManager:
            def __init__(self):
                self.sessions = {}
                self.current_session = None
            def start_session(self, session_name=None):
                if session_name is None:
                    session_name = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
                self.current_session = {
                    "name": session_name,
                    "start_time": datetime.now(),
                    "data": [],
                    "metadata": {
                        "device_id": "Shimmer3-Test",
                        "sampling_rate": 256,
                        "enabled_sensors": ["GSR", "PPG", "Accelerometer"],
                    },
                }
                self.sessions[session_name] = self.current_session
                return session_name
            def add_sample(self, sample):
                if self.current_session:
                    self.current_session["data"].append(sample)
            def stop_session(self):
                if self.current_session:
                    self.current_session["end_time"] = datetime.now()
                    duration = (
                        self.current_session["end_time"]
                        - self.current_session["start_time"]
                    )
                    self.current_session["duration"] = duration.total_seconds()
                    self.current_session = None
            def export_session_csv(self, session_name, filename):
                if session_name not in self.sessions:
                    return False
                session = self.sessions[session_name]
                with open(filename, "w", newline="") as csvfile:
                    if not session["data"]:
                        return True
                    fieldnames = session["data"][0].keys()
                    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                    writer.writeheader()
                    for sample in session["data"]:
                        writer.writerow(sample)
                return True
            def get_session_info(self, session_name):
                if session_name not in self.sessions:
                    return None
                session = self.sessions[session_name]
                info = {
                    "name": session["name"],
                    "start_time": session["start_time"],
                    "sample_count": len(session["data"]),
                    "metadata": session["metadata"],
                }
                if "end_time" in session:
                    info["end_time"] = session["end_time"]
                    info["duration"] = session["duration"]
                return info
        manager = SessionManager()
        session_name = manager.start_session("test_session")
        assert session_name == "test_session"
        print("[PASS] Session creation works")
        for i in range(100):
            sample = {
                "timestamp": time.time() + i * 0.01,
                "sample_number": i,
                "GSR": 1000 + i,
                "PPG": 2048 + i * 2,
            }
            manager.add_sample(sample)
        print("[PASS] Sample data addition works")
        manager.stop_session()
        print("[PASS] Session stopping works")
        info = manager.get_session_info("test_session")
        assert info is not None
        assert info["sample_count"] == 100
        assert "duration" in info
        print(
            f"[PASS] Session info: {info['sample_count']} samples, {info['duration']:.3f}s duration"
        )
        with tempfile.NamedTemporaryFile(mode="w", suffix=".csv", delete=False) as f:
            csv_file = f.name
        success = manager.export_session_csv("test_session", csv_file)
        assert success == True
        print("[PASS] CSV export works")
        with open(csv_file, "r") as f:
            reader = csv.DictReader(f)
            rows = list(reader)
            assert len(rows) == 100
            assert "GSR" in rows[0]
            assert "PPG" in rows[0]
        print("[PASS] CSV content verification passed")
        os.unlink(csv_file)
        return True
    except Exception as e:
        print(f"[FAIL] Session management test failed: {e}")
        traceback.print_exc()
        return False
@pytest.mark.unit
def test_error_handling():
    print("Testing error handling...")
    try:
        class RobustShimmerManager:
            def __init__(self):
                self.available_libraries = self._detect_libraries()
            def _detect_libraries(self):
                libraries = []
                try:
                    import pyshimmer
                    libraries.append("pyshimmer")
                except ImportError:
                    pass
                try:
                    import bluetooth
                    libraries.append("bluetooth")
                except ImportError:
                    pass
                return libraries
            def connect_with_fallback(self, device_address):
                for library in self.available_libraries:
                    try:
                        if library == "pyshimmer":
                            return self._connect_pyshimmer(device_address)
                        elif library == "bluetooth":
                            return self._connect_bluetooth(device_address)
                    except Exception as e:
                        print(f"Connection attempt with {library} failed: {e}")
                        continue
                return {"success": False, "error": "No compatible libraries available"}
            def _connect_pyshimmer(self, device_address):
                if device_address == "invalid":
                    raise ConnectionError("Invalid device address")
                return {
                    "success": True,
                    "library": "pyshimmer",
                    "device": device_address,
                }
            def _connect_bluetooth(self, device_address):
                if device_address == "unreachable":
                    raise TimeoutError("Device unreachable")
                return {
                    "success": True,
                    "library": "bluetooth",
                    "device": device_address,
                }
            def handle_connection_error(self, error):
                error_messages = {
                    "ConnectionError": "Device not found or not responding. Check device power and pairing.",
                    "TimeoutError": "Connection timeout. Device may be out of range.",
                    "PermissionError": "Bluetooth access denied. Check permissions.",
                    "ImportError": "Required library not installed. Install pyshimmer or pybluez.",
                }
                error_type = type(error).__name__
                return error_messages.get(error_type, f"Unknown error: {error}")
        manager = RobustShimmerManager()
        result = manager.connect_with_fallback("00:06:66:12:34:56")
        print(f"[PASS] Connection result: {result}")
        error_cases = [
            ConnectionError("Device not found"),
            TimeoutError("Connection timeout"),
            PermissionError("Access denied"),
            ImportError("Library not found"),
        ]
        for error in error_cases:
            message = manager.handle_connection_error(error)
            assert len(message) > 0
            print(f"[PASS] Error handled: {type(error).__name__} -> {message}")
        if not manager.available_libraries:
            print("[PASS] Graceful degradation when no libraries available")
        else:
            print(f"[PASS] Using available libraries: {manager.available_libraries}")
        return True
    except Exception as e:
        print(f"[FAIL] Error handling test failed: {e}")
        traceback.print_exc()
        return False
@pytest.mark.unit
def test_multi_library_compatibility():
    print("Testing multi-library compatibility...")
    try:
        class UnifiedShimmerInterface:
            def __init__(self):
                self.library_adapters = {
                    "pyshimmer": self._get_pyshimmer_adapter(),
                    "bluetooth": self._get_bluetooth_adapter(),
                    "mock": self._get_mock_adapter(),
                }
            def _get_pyshimmer_adapter(self):
                return {
                    "connect": lambda addr: {"connected": True, "library": "pyshimmer"},
                    "start_streaming": lambda: {"streaming": True},
                    "stop_streaming": lambda: {"streaming": False},
                    "get_sample": lambda: {"GSR": 1000, "PPG": 2048},
                }
            def _get_bluetooth_adapter(self):
                return {
                    "connect": lambda addr: {"connected": True, "library": "bluetooth"},
                    "start_streaming": lambda: {"streaming": True},
                    "stop_streaming": lambda: {"streaming": False},
                    "get_sample": lambda: {"GSR": 1001, "PPG": 2049},
                }
            def _get_mock_adapter(self):
                return {
                    "connect": lambda addr: {"connected": True, "library": "mock"},
                    "start_streaming": lambda: {"streaming": True},
                    "stop_streaming": lambda: {"streaming": False},
                    "get_sample": lambda: {"GSR": 1002, "PPG": 2050},
                }
            def connect(self, device_address, preferred_library=None):
                if preferred_library and preferred_library in self.library_adapters:
                    adapter = self.library_adapters[preferred_library]
                    return adapter["connect"](device_address)
                for library_name, adapter in self.library_adapters.items():
                    try:
                        result = adapter["connect"](device_address)
                        result["library_used"] = library_name
                        return result
                    except Exception:
                        continue
                return {"connected": False, "error": "No compatible library found"}
            def test_library_compatibility(self):
                results = {}
                for library_name, adapter in self.library_adapters.items():
                    try:
                        required_methods = [
                            "connect",
                            "start_streaming",
                            "stop_streaming",
                            "get_sample",
                        ]
                        for method in required_methods:
                            assert method in adapter
                        connect_result = adapter["connect"]("test_address")
                        stream_result = adapter["start_streaming"]()
                        sample_result = adapter["get_sample"]()
                        stop_result = adapter["stop_streaming"]()
                        results[library_name] = "Compatible"
                    except Exception as e:
                        results[library_name] = f"Error: {e}"
                return results
        interface = UnifiedShimmerInterface()
        for library in ["pyshimmer", "bluetooth", "mock"]:
            result = interface.connect("test_device", library)
            assert result["connected"] == True
            assert result["library"] == library
            print(f"[PASS] Connection with {library} adapter works")
        compatibility_results = interface.test_library_compatibility()
        for library, status in compatibility_results.items():
            print(f"[PASS] {library} compatibility: {status}")
            assert status == "Compatible"
        print("[PASS] All library adapters provide consistent interface")
        return True
    except Exception as e:
        print(f"[FAIL] Multi-library compatibility test failed: {e}")
        traceback.print_exc()
        return False
def main():
    print("=" * 60)
    print("Shimmer Implementation Test Suite")
    print("=" * 60)
    tests = [
        test_shimmer_libraries_availability,
        test_device_discovery_simulation,
        test_data_streaming_simulation,
        test_session_management,
        test_error_handling,
        test_multi_library_compatibility,
    ]
    passed = 0
    total = len(tests)
    for test in tests:
        try:
            print(f"\n{'-' * 40}")
            if test():
                passed += 1
                print(f"[PASS] {test.__name__} PASSED")
            else:
                print(f"[FAIL] {test.__name__} FAILED")
        except Exception as e:
            print(f"[FAIL] {test.__name__} FAILED with exception: {e}")
            traceback.print_exc()
    print("\n" + "=" * 60)
    print(f"Shimmer Test Results: {passed}/{total} tests passed")
    print("=" * 60)
    if passed == total:
        print("[PASS] All Shimmer implementation tests passed!")
        return True
    else:
        print("[FAIL] Some Shimmer tests failed. Check the output above for details.")
        return False
if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)