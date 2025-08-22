"""
GUI Test Utilities and Fixtures
===============================

Shared utilities and fixtures for comprehensive GUI testing of both
PC (PyQt5) and Android applications.

Provides base classes, setup/teardown helpers, and common testing utilities
for consistent and reliable GUI testing across platforms.
"""

import pytest
import sys
import os
import time
import tempfile
from typing import Dict, List, Optional, Any
from pathlib import Path
from unittest.mock import Mock, patch

# PyQt5 imports with fallback
try:
    from PyQt5.QtWidgets import QApplication, QWidget
    from PyQt5.QtCore import QTimer
    from PyQt5.QtTest import QTest
    PYQT_AVAILABLE = True
except ImportError:
    PYQT_AVAILABLE = False


class GUITestBase:
    """Base class for GUI tests with common setup and utilities."""
    
    def setup_method(self):
        """Setup for each test method."""
        self.test_data_dir = Path(tempfile.mkdtemp(prefix="gui_test_"))
        self.mock_patches = []
    
    def teardown_method(self):
        """Cleanup after each test method."""
        # Clean up mock patches
        for patch_obj in self.mock_patches:
            if hasattr(patch_obj, 'stop'):
                patch_obj.stop()
        
        # Clean up test data
        if hasattr(self, 'test_data_dir') and self.test_data_dir.exists():
            import shutil
            shutil.rmtree(self.test_data_dir, ignore_errors=True)
    
    def add_mock_patch(self, patch_obj):
        """Add a mock patch to be cleaned up automatically."""
        self.mock_patches.append(patch_obj)
        return patch_obj


@pytest.fixture(scope="session")
def test_app():
    """QApplication instance for PyQt5 tests."""
    if not PYQT_AVAILABLE:
        pytest.skip("PyQt5 not available")
    
    # Check if QApplication already exists
    app = QApplication.instance()
    if app is None:
        app = QApplication([])
    
    # Configure for testing
    app.setAttribute(app.AA_DisableWindowContextHelpButton)
    app.setQuitOnLastWindowClosed(False)
    
    yield app
    
    # Cleanup
    if app:
        app.quit()


def create_test_app():
    """Create a test QApplication if needed."""
    if not PYQT_AVAILABLE:
        return None
    
    app = QApplication.instance()
    if app is None:
        app = QApplication([])
        app.setAttribute(app.AA_DisableWindowContextHelpButton)
    
    return app


def cleanup_test_app(app):
    """Clean up test QApplication."""
    if app and PYQT_AVAILABLE:
        app.processEvents()
        # Don't quit the app as it might be used by other tests


class MockDeviceManager:
    """Mock device manager for testing device-related functionality."""
    
    def __init__(self):
        self.connected_devices = {}
        self.device_data = {}
    
    def connect_device(self, device_id: str) -> bool:
        """Mock device connection."""
        self.connected_devices[device_id] = {
            "status": "connected",
            "timestamp": time.time()
        }
        return True
    
    def disconnect_device(self, device_id: str) -> bool:
        """Mock device disconnection."""
        if device_id in self.connected_devices:
            self.connected_devices[device_id]["status"] = "disconnected"
        return True
    
    def get_device_status(self, device_id: str) -> str:
        """Get mock device status."""
        if device_id in self.connected_devices:
            return self.connected_devices[device_id]["status"]
        return "unknown"
    
    def update_device_data(self, device_id: str, data: Dict):
        """Update mock device data."""
        self.device_data[device_id] = data
    
    def get_device_data(self, device_id: str) -> Dict:
        """Get mock device data."""
        return self.device_data.get(device_id, {})


class MockSessionManager:
    """Mock session manager for testing session-related functionality."""
    
    def __init__(self):
        self.current_session = None
        self.sessions = {}
    
    def start_session(self, session_id: str = None) -> str:
        """Start a mock session."""
        if session_id is None:
            session_id = f"test_session_{int(time.time())}"
        
        self.current_session = {
            "id": session_id,
            "start_time": time.time(),
            "status": "active",
            "data": []
        }
        
        self.sessions[session_id] = self.current_session
        return session_id
    
    def stop_session(self):
        """Stop the current mock session."""
        if self.current_session:
            self.current_session["status"] = "stopped"
            self.current_session["end_time"] = time.time()
    
    def add_data(self, data: Dict):
        """Add data to current mock session."""
        if self.current_session:
            self.current_session["data"].append({
                "timestamp": time.time(),
                "data": data
            })
    
    def save_session(self, filepath: str) -> bool:
        """Mock session save."""
        # Just return success for testing
        return True
    
    def load_session(self, filepath: str) -> bool:
        """Mock session load."""
        # Return success for testing
        return True


@pytest.fixture
def mock_device_manager():
    """Fixture providing a mock device manager."""
    return MockDeviceManager()


@pytest.fixture
def mock_session_manager():
    """Fixture providing a mock session manager."""
    return MockSessionManager()


class UITestHelpers:
    """Helper methods for UI testing."""
    
    @staticmethod
    def wait_for_element_visible(widget, timeout=5.0):
        """Wait for widget to become visible."""
        if not PYQT_AVAILABLE:
            return False
        
        start_time = time.time()
        while time.time() - start_time < timeout:
            if widget.isVisible():
                return True
            QApplication.instance().processEvents()
            time.sleep(0.1)
        
        return False
    
    @staticmethod
    def wait_for_element_enabled(widget, timeout=5.0):
        """Wait for widget to become enabled."""
        if not PYQT_AVAILABLE:
            return False
        
        start_time = time.time()
        while time.time() - start_time < timeout:
            if widget.isEnabled():
                return True
            QApplication.instance().processEvents()
            time.sleep(0.1)
        
        return False
    
    @staticmethod
    def simulate_user_delay(delay_ms=100):
        """Simulate realistic user interaction delay."""
        if PYQT_AVAILABLE:
            QTest.qWait(delay_ms)
        else:
            time.sleep(delay_ms / 1000.0)
    
    @staticmethod
    def find_child_by_text(parent, text: str, widget_type=None):
        """Find child widget by text content."""
        if not PYQT_AVAILABLE:
            return None
        
        children = parent.findChildren(widget_type or QWidget)
        for child in children:
            if hasattr(child, 'text') and child.text() == text:
                return child
            elif hasattr(child, 'windowTitle') and child.windowTitle() == text:
                return child
        
        return None
    
    @staticmethod
    def get_all_buttons(parent):
        """Get all button widgets from parent."""
        if not PYQT_AVAILABLE:
            return []
        
        from PyQt5.QtWidgets import QPushButton
        return parent.findChildren(QPushButton)
    
    @staticmethod
    def click_button_by_text(parent, button_text: str):
        """Click button by its text."""
        if not PYQT_AVAILABLE:
            return False
        
        from PyQt5.QtWidgets import QPushButton
        from PyQt5.QtCore import Qt
        
        buttons = parent.findChildren(QPushButton)
        for button in buttons:
            if button.text() == button_text and button.isEnabled():
                QTest.mouseClick(button, Qt.LeftButton)
                return True
        
        return False


@pytest.fixture
def ui_helpers():
    """Fixture providing UI test helpers."""
    return UITestHelpers


class PerformanceMonitor:
    """Monitor performance metrics during GUI tests."""
    
    def __init__(self):
        self.start_time = None
        self.measurements = []
    
    def start_measurement(self, name: str = "default"):
        """Start performance measurement."""
        self.start_time = time.time()
        return self
    
    def end_measurement(self, name: str = "default"):
        """End performance measurement and record result."""
        if self.start_time is not None:
            duration = time.time() - self.start_time
            self.measurements.append({
                "name": name,
                "duration": duration,
                "timestamp": time.time()
            })
            self.start_time = None
            return duration
        return 0
    
    def get_measurements(self) -> List[Dict]:
        """Get all performance measurements."""
        return self.measurements.copy()
    
    def assert_performance(self, max_duration: float, name: str = None):
        """Assert that performance is within acceptable limits."""
        if name:
            measurements = [m for m in self.measurements if m["name"] == name]
        else:
            measurements = self.measurements
        
        if measurements:
            latest = measurements[-1]
            assert latest["duration"] <= max_duration, \
                f"Performance check failed: {latest['duration']:.3f}s > {max_duration}s"


@pytest.fixture
def performance_monitor():
    """Fixture providing performance monitoring."""
    return PerformanceMonitor()


def generate_test_data(data_type: str = "gsr", count: int = 100) -> List[Dict]:
    """Generate test data for GUI testing."""
    import random
    
    data = []
    base_time = time.time()
    
    for i in range(count):
        timestamp = base_time + i * 0.1  # 10Hz sampling
        
        if data_type == "gsr":
            value = 2.5 + random.uniform(-0.5, 0.5) + 0.1 * random.sin(i * 0.1)
        elif data_type == "temperature":
            value = 36.5 + random.uniform(-1.0, 1.0)
        elif data_type == "accelerometer":
            value = {
                "x": random.uniform(-1.0, 1.0),
                "y": random.uniform(-1.0, 1.0),
                "z": random.uniform(9.0, 11.0)
            }
        else:
            value = random.uniform(0, 100)
        
        data.append({
            "timestamp": timestamp,
            "value": value,
            "device_id": f"test_device_{data_type}",
            "sequence": i
        })
    
    return data


def create_temp_config_file(config_data: Dict) -> str:
    """Create temporary configuration file for testing."""
    import json
    
    temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False)
    json.dump(config_data, temp_file, indent=2)
    temp_file.close()
    
    return temp_file.name


def cleanup_temp_file(filepath: str):
    """Clean up temporary file."""
    try:
        os.unlink(filepath)
    except OSError:
        pass


# Common test configuration
DEFAULT_TEST_CONFIG = {
    "ui_test_timeout": 30.0,
    "performance_threshold_ms": 1000,
    "interaction_delay_ms": 100,
    "screenshot_on_failure": True,
    "mock_external_services": True,
    "test_data_size": 100
}