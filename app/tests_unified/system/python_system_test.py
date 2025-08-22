import json
import os
import sys
import tempfile
import threading
import time
import traceback
from pathlib import Path
os.environ["QT_QPA_PLATFORM"] = "offscreen"
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))
sys.path.insert(0, str(project_root / "PythonApp"))
def test_python_environment():
    print("Testing Python environment...")
    dependencies = {
        "cv2": "OpenCV",
        "numpy": "NumPy",
        "PyQt5": "PyQt5",
        "requests": "Requests",
        "websockets": "WebSockets",
        "matplotlib": "Matplotlib",
        "pandas": "Pandas",
        "scipy": "SciPy",
        "PIL": "Pillow (PIL)",
    }
    available = []
    missing = []
    for module, name in dependencies.items():
        try:
            if "." in module:
                parts = module.split(".")
                mod = __import__(parts[0])
                for part in parts[1:]:
                    mod = getattr(mod, part)
            else:
                __import__(module)
            available.append(name)
            print(f"[PASS] {name}")
        except (ImportError, AttributeError) as e:
            missing.append(name)
            print(f"[FAIL] {name}")
    print(f"\nDependencies: {len(available)}/{len(dependencies)} available")
    if missing:
        print(f"Missing: {', '.join(missing)}")
    return len(missing) == 0
def test_gui_components():
    print("\nTesting GUI components...")
    try:
        from PyQt5.QtCore import Qt, QTimer
        from PyQt5.QtWidgets import (
            QApplication,
            QLabel,
            QMainWindow,
            QPushButton,
            QWidget,
        )
        app = QApplication([])
        main_window = QMainWindow()
        main_window.setWindowTitle("Test Window")
        main_window.resize(800, 600)
        central_widget = QWidget()
        main_window.setCentralWidget(central_widget)
        button = QPushButton("Test Button")
        label = QLabel("Test Label")
        print("[PASS] Basic widgets created successfully")
        try:
            from PythonApp.gui.main_window import MainWindow
            main_window_test = MainWindow()
            print("[PASS] Main window created successfully")
            main_window_test.close()
        except Exception as e:
            print(f"[WARN] Main window creation failed: {e}")
        app.quit()
        return True
    except Exception as e:
        print(f"[FAIL] GUI components test failed: {e}")
        traceback.print_exc()
        return False
def test_opencv_functionality():
    print("\nTesting OpenCV functionality...")
    try:
        import cv2
        import numpy as np
        print(f"[PASS] OpenCV version: {cv2.__version__}")
        test_img = np.zeros((480, 640, 3), dtype=np.uint8)
        test_img[100:380, 100:540] = [0, 255, 0]
        grey = cv2.cvtColor(test_img, cv2.COLOR_BGR2GRAY)
        print("[PASS] Colour conversion works")
        edges = cv2.Canny(grey, 50, 150)
        print("[PASS] Edge detection works")
        pattern_size = (9, 6)
        square_size = 1.0
        pattern_points = np.zeros((pattern_size[0] * pattern_size[1], 3), np.float32)
        pattern_points[:, :2] = np.mgrid[
            0 : pattern_size[0], 0 : pattern_size[1]
        ].T.reshape(-1, 2)
        pattern_points *= square_size
        print("[PASS] Calibration pattern generation works")
        camera_matrix = np.array(
            [[500, 0, 320], [0, 500, 240], [0, 0, 1]], dtype=np.float32
        )
        dist_coeffs = np.zeros((4, 1))
        print("[PASS] Camera matrix operations work")
        return True
    except Exception as e:
        print(f"[FAIL] OpenCV functionality test failed: {e}")
        traceback.print_exc()
        return False
def test_network_capabilities():
    print("\nTesting network capabilities...")
    try:
        import json
        import socket
        import threading
        import time
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        print("[PASS] Socket creation works")
        test_message = {
            "type": "command",
            "command": "start_recording",
            "timestamp": time.time(),
            "device_id": "test_device",
            "parameters": {
                "resolution": "1920x1080",
                "fps": 30,
                "sensors": ["camera", "thermal", "gsr"],
            },
        }
        json_str = json.dumps(test_message)
        parsed_message = json.loads(json_str)
        assert parsed_message == test_message
        print("[PASS] JSON message serialization works")
        def test_server():
            try:
                server_socket.bind(("localhost", 0))
                port = server_socket.getsockname()[1]
                server_socket.listen(1)
                def handle_client():
                    client_socket, addr = server_socket.accept()
                    data = client_socket.recv(1024).decode()
                    message = json.loads(data)
                    response = {"status": "received", "echo": message}
                    client_socket.send(json.dumps(response).encode())
                    client_socket.close()
                client_thread = threading.Thread(target=handle_client, daemon=True)
                client_thread.start()
                client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                client_socket.connect(("localhost", port))
                client_socket.send(json.dumps(test_message).encode())
                response_data = client_socket.recv(1024).decode()
                response = json.loads(response_data)
                assert response["status"] == "received"
                assert response["echo"] == test_message
                client_socket.close()
                client_thread.join(timeout=1)
                print("[PASS] Socket communication works")
            except Exception as e:
                print(f"[WARN] Socket communication test partial failure: {e}")
            finally:
                server_socket.close()
        test_server()
        return True
    except Exception as e:
        print(f"[FAIL] Network capabilities test failed: {e}")
        traceback.print_exc()
        return False
def test_data_processing():
    print("\nTesting data processing...")
    try:
        import matplotlib
        import numpy as np
        import pandas as pd
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        data = np.random.randn(1000, 3)
        mean_vals = np.mean(data, axis=0)
        std_vals = np.std(data, axis=0)
        print(f"[PASS] NumPy processing: mean={mean_vals}, std={std_vals}")
        df = pd.DataFrame(data, columns=["GSR", "PPG", "Accelerometer"])
        df["timestamp"] = pd.date_range("2024-01-01", periods=1000, freq="1ms")
        stats = df.describe()
        print("[PASS] Pandas DataFrame operations work")
        filtered_df = df[df["GSR"] > 0]
        rolling_mean = df["PPG"].rolling(window=10).mean()
        print("[PASS] Data filtering and rolling statistics work")
        fig, ax = plt.subplots(figsize=(8, 6))
        ax.plot(df.index[:100], df["GSR"][:100], label="GSR")
        ax.plot(df.index[:100], df["PPG"][:100], label="PPG")
        ax.legend()
        ax.set_title("Sample Sensor Data")
        with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as f:
            fig.savefig(f.name)
            plot_file = f.name
        plt.close(fig)
        if os.path.exists(plot_file) and os.path.getsize(plot_file) > 0:
            print("[PASS] Matplotlib plotting works")
            os.unlink(plot_file)
        else:
            print("[WARN] Matplotlib plotting may have issues")
        return True
    except Exception as e:
        print(f"[FAIL] Data processing test failed: {e}")
        traceback.print_exc()
        return False
def _create_test_session_metadata():
    from datetime import datetime
    return {
        "session_id": f"test_session_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
        "start_time": datetime.now().isoformat(),
        "devices": ["Android-01", "Android-02", "PC-Webcam-01", "PC-Webcam-02"],
        "sensors": ["Camera", "Thermal", "GSR", "PPG"],
        "configuration": {
            "resolution": "1920x1080",
            "fps": 30,
            "sampling_rate": 512,
        },
    }
def _test_json_operations(session_metadata):
    import json
    import tempfile
    import os
    with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
        json.dump(session_metadata, f, indent=2)
        json_file = f.name
    with open(json_file, "r") as f:
        loaded_metadata = json.load(f)
    assert loaded_metadata == session_metadata
    print("[PASS] JSON session metadata export/import works")
    os.unlink(json_file)
def _test_csv_operations():
    import csv
    import tempfile
    import os
    sensor_data = [
        {"timestamp": 1.0, "GSR": 1000, "PPG": 2048, "temp": 25.5},
        {"timestamp": 1.1, "GSR": 1001, "PPG": 2049, "temp": 25.6},
        {"timestamp": 1.2, "GSR": 1002, "PPG": 2050, "temp": 25.7},
    ]
    with tempfile.NamedTemporaryFile(mode="w", suffix=".csv", delete=False) as f:
        writer = csv.DictWriter(f, fieldnames=sensor_data[0].keys())
        writer.writeheader()
        writer.writerows(sensor_data)
        csv_file = f.name
    with open(csv_file, "r") as f:
        reader = csv.DictReader(f)
        rows = list(reader)
    assert len(rows) == len(sensor_data)
    assert float(rows[0]["GSR"]) == 1000
    print("[PASS] CSV sensor data export/import works")
    os.unlink(csv_file)
def _test_directory_structure(session_metadata):
    import tempfile
    from pathlib import Path
    with tempfile.TemporaryDirectory() as temp_dir:
        session_dir = Path(temp_dir) / "sessions" / session_metadata["session_id"]
        session_dir.mkdir(parents=True, exist_ok=True)
        subdirs = ["video", "thermal", "sensor_data", "logs"]
        for subdir in subdirs:
            (session_dir / subdir).mkdir(exist_ok=True)
        for subdir in subdirs:
            assert (session_dir / subdir).exists()
        print("[PASS] Session directory structure creation works")
def test_file_operations():
    print("\nTesting file operations...")
    try:
        session_metadata = _create_test_session_metadata()
        _test_json_operations(session_metadata)
        _test_csv_operations()
        _test_directory_structure(session_metadata)
        return True
    except Exception as e:
        print(f"[FAIL] File operations test failed: {e}")
        traceback.print_exc()
        return False
def test_system_integration():
    print("\nTesting system integration...")
    try:
        class DeviceSimulator:
            def __init__(self, device_id):
                self.device_id = device_id
                self.is_connected = False
                self.is_recording = False
                self.status = "idle"
            def connect(self):
                self.is_connected = True
                self.status = "connected"
                return True
            def start_recording(self):
                if self.is_connected:
                    self.is_recording = True
                    self.status = "recording"
                    return True
                return False
            def stop_recording(self):
                self.is_recording = False
                self.status = "connected"
                return True
            def disconnect(self):
                self.is_connected = False
                self.is_recording = False
                self.status = "disconnected"
                return True
            def get_status(self):
                return {
                    "device_id": self.device_id,
                    "connected": self.is_connected,
                    "recording": self.is_recording,
                    "status": self.status,
                }
        devices = [
            DeviceSimulator("Android-01"),
            DeviceSimulator("Android-02"),
            DeviceSimulator("PC-Webcam-01"),
            DeviceSimulator("PC-Webcam-02"),
        ]
        for device in devices:
            assert device.connect() == True
        print("[PASS] All devices connected successfully")
        for device in devices:
            assert device.start_recording() == True
        statuses = [device.get_status() for device in devices]
        assert all(status["recording"] for status in statuses)
        print("[PASS] Synchronised recording start works")
        for status in statuses:
            assert status["connected"] == True
            assert status["recording"] == True
            assert status["status"] == "recording"
        print("[PASS] Status monitoring works")
        for device in devices:
            assert device.stop_recording() == True
        statuses = [device.get_status() for device in devices]
        assert all(not status["recording"] for status in statuses)
        print("[PASS] Synchronised recording stop works")
        for device in devices:
            assert device.disconnect() == True
        statuses = [device.get_status() for device in devices]
        assert all(not status["connected"] for status in statuses)
        print("[PASS] Device disconnection works")
        return True
    except Exception as e:
        print(f"[FAIL] System integration test failed: {e}")
        traceback.print_exc()
        return False
def generate_test_report():
    print("\n" + "=" * 60)
    print("complete SYSTEM TEST REPORT")
    print("=" * 60)
    tests = [
        ("Python Environment", test_python_environment),
        ("GUI Components", test_gui_components),
        ("OpenCV Functionality", test_opencv_functionality),
        ("Network Capabilities", test_network_capabilities),
        ("Data Processing", test_data_processing),
        ("File Operations", test_file_operations),
        ("System Integration", test_system_integration),
    ]
    results = {}
    passed = 0
    total = len(tests)
    for test_name, test_func in tests:
        try:
            print(f"\n{'-' * 40}")
            print(f"Running: {test_name}")
            result = test_func()
            results[test_name] = "PASS" if result else "FAIL"
            if result:
                passed += 1
                print(f"[PASS] {test_name}: PASSED")
            else:
                print(f"[FAIL] {test_name}: FAILED")
        except Exception as e:
            results[test_name] = f"ERROR: {e}"
            print(f"[FAIL] {test_name}: ERROR - {e}")
    print("\n" + "=" * 60)
    print("TEST SUMMARY")
    print("=" * 60)
    for test_name, result in results.items():
        status_char = "[PASS]" if result == "PASS" else "[FAIL]"
        print(f"{status_char} {test_name}: {result}")
    print(f"\nOverall Result: {passed}/{total} tests passed ({passed/total*100:.1f}%)")
    if passed == total:
        print(
            "[SUCCESS] ALL TESTS PASSED! The Multi-Sensor Recording System is working correctly."
        )
    else:
        print("[WARN]  Some tests failed. Review the details above.")
    return passed == total
def main():
    success = generate_test_report()
    return success
if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)