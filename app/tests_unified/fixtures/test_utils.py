"""
Shared test utilities and fixtures for the unified testing framework.

This module provides common testing utilities, fixtures, and helper functions
that are used across all test levels and categories.
"""

import os
import sys
import tempfile
import shutil
from pathlib import Path
from typing import Dict, Any, Optional
import pytest
import logging

# Add project root to Python path
PROJECT_ROOT = Path(__file__).parent.parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

class TestEnvironment:
    """Manages test environment setup and cleanup"""
    
    def __init__(self):
        self.temp_dirs = []
        self.temp_files = []
        self.original_env = {}
        
    def create_temp_dir(self, prefix: str = "gsr_test_") -> Path:
        """Create a temporary directory for testing"""
        temp_dir = Path(tempfile.mkdtemp(prefix=prefix))
        self.temp_dirs.append(temp_dir)
        return temp_dir
    
    def create_temp_file(self, suffix: str = ".tmp", content: str = "") -> Path:
        """Create a temporary file for testing"""
        fd, temp_path = tempfile.mkstemp(suffix=suffix)
        temp_file = Path(temp_path)
        
        with os.fdopen(fd, 'w') as f:
            f.write(content)
        
        self.temp_files.append(temp_file)
        return temp_file
    
    def set_env_var(self, key: str, value: str):
        """Set environment variable with cleanup tracking"""
        if key not in self.original_env:
            self.original_env[key] = os.environ.get(key)
        os.environ[key] = value
    
    def cleanup(self):
        """Clean up all temporary resources"""
        # Remove temporary directories
        for temp_dir in self.temp_dirs:
            if temp_dir.exists():
                shutil.rmtree(temp_dir, ignore_errors=True)
        
        # Remove temporary files
        for temp_file in self.temp_files:
            if temp_file.exists():
                temp_file.unlink(missing_ok=True)
        
        # Restore environment variables
        for key, original_value in self.original_env.items():
            if original_value is None:
                os.environ.pop(key, None)
            else:
                os.environ[key] = original_value
        
        # Clear tracking lists
        self.temp_dirs.clear()
        self.temp_files.clear()
        self.original_env.clear()

# Global test environment instance
_test_environment = TestEnvironment()

def setup_test_environment():
    """Setup global test environment"""
    global _test_environment
    _test_environment = TestEnvironment()
    
    # Set common test environment variables
    _test_environment.set_env_var("GSR_TEST_MODE", "true")
    _test_environment.set_env_var("GSR_LOG_LEVEL", "DEBUG")
    
    return _test_environment

def cleanup_test_environment():
    """Cleanup global test environment"""
    global _test_environment
    _test_environment.cleanup()

@pytest.fixture(scope="session")
def test_environment():
    """Session-scoped test environment fixture"""
    env = setup_test_environment()
    yield env
    cleanup_test_environment()

@pytest.fixture(scope="function")
def temp_dir():
    """Function-scoped temporary directory"""
    temp_dir = _test_environment.create_temp_dir()
    yield temp_dir
    # Cleanup handled by test environment

@pytest.fixture(scope="function") 
def temp_file():
    """Function-scoped temporary file"""
    temp_file = _test_environment.create_temp_file()
    yield temp_file
    # Cleanup handled by test environment

@pytest.fixture
def mock_config():
    """Mock configuration for testing"""
    return {
        "test_mode": True,
        "log_level": "DEBUG",
        "timeout": 30,
        "retry_count": 2,
        "temp_dir": "/tmp/gsr_test"
    }

@pytest.fixture
def sample_test_data():
    """Sample test data for various test scenarios"""
    return {
        "gsr_data": [0.1, 0.2, 0.15, 0.3, 0.25],
        "thermal_data": [[20.5, 21.0], [21.2, 21.5]],
        "timestamp_data": [1000, 1100, 1200, 1300, 1400],
        "session_id": "test_session_001",
        "device_id": "test_device_001"
    }

class MockDevice:
    """Mock device for testing device interactions"""
    
    def __init__(self, device_id: str = "mock_device"):
        self.device_id = device_id
        self.connected = False
        self.data_buffer = []
    
    def connect(self) -> bool:
        """Mock device connection"""
        self.connected = True
        return True
    
    def disconnect(self):
        """Mock device disconnection"""
        self.connected = False
    
    def send_data(self, data: Any):
        """Mock sending data to device"""
        if self.connected:
            self.data_buffer.append(data)
            return True
        return False
    
    def receive_data(self) -> Optional[Any]:
        """Mock receiving data from device"""
        if self.connected and self.data_buffer:
            return self.data_buffer.pop(0)
        return None

@pytest.fixture
def mock_android_device():
    """Mock Android device fixture"""
    return MockDevice("mock_android_device")

@pytest.fixture
def mock_thermal_camera():
    """Mock thermal camera fixture"""
    return MockDevice("mock_thermal_camera")

@pytest.fixture
def mock_gsr_sensor():
    """Mock GSR sensor fixture"""
    return MockDevice("mock_gsr_sensor")

class TestLogger:
    """Test-specific logger that captures log messages"""
    
    def __init__(self):
        self.messages = []
        self.logger = logging.getLogger("test_logger")
        self.handler = logging.StreamHandler()
        self.logger.addHandler(self.handler)
        self.logger.setLevel(logging.DEBUG)
    
    def debug(self, message: str):
        self.messages.append(("DEBUG", message))
        self.logger.debug(message)
    
    def info(self, message: str):
        self.messages.append(("INFO", message))
        self.logger.info(message)
    
    def warning(self, message: str):
        self.messages.append(("WARNING", message))
        self.logger.warning(message)
    
    def error(self, message: str):
        self.messages.append(("ERROR", message))
        self.logger.error(message)
    
    def get_messages(self, level: Optional[str] = None) -> list:
        """Get captured log messages, optionally filtered by level"""
        if level:
            return [msg for lvl, msg in self.messages if lvl == level]
        return [msg for lvl, msg in self.messages]
    
    def clear(self):
        """Clear captured messages"""
        self.messages.clear()

@pytest.fixture
def test_logger():
    """Test logger fixture"""
    logger = TestLogger()
    yield logger
    logger.clear()

# Test markers and decorators
def skip_if_no_hardware(test_func):
    """Decorator to skip tests if hardware is not available"""
    import pytest
    
    def wrapper(*args, **kwargs):
        if os.environ.get("GSR_TEST_NO_HARDWARE", "false").lower() == "true":
            pytest.skip("Hardware not available for testing")
        return test_func(*args, **kwargs)
    
    return wrapper

def require_network(test_func):
    """Decorator to skip tests if network is not available"""
    import pytest
    import socket
    
    def wrapper(*args, **kwargs):
        try:
            socket.create_connection(("8.8.8.8", 53), timeout=3)
        except OSError:
            pytest.skip("Network not available for testing")
        return test_func(*args, **kwargs)
    
    return wrapper

def slow_test(test_func):
    """Decorator to mark tests as slow"""
    import pytest
    return pytest.mark.slow(test_func)

def android_test(test_func):
    """Decorator to mark tests as Android-specific"""
    import pytest
    return pytest.mark.android(test_func)

def hardware_test(test_func):
    """Decorator to mark tests as requiring hardware"""
    import pytest
    return pytest.mark.hardware(test_func)

# Utility functions
def assert_within_tolerance(actual: float, expected: float, tolerance: float = 0.01):
    """Assert that actual value is within tolerance of expected value"""
    assert abs(actual - expected) <= tolerance, \
        f"Expected {expected} +/- {tolerance}, got {actual}"

def assert_timing_within_bounds(actual_time: float, min_time: float, max_time: float):
    """Assert that timing is within expected bounds"""
    assert min_time <= actual_time <= max_time, \
        f"Expected timing between {min_time}s and {max_time}s, got {actual_time}s"

def create_test_data_placeholder(data_type: str, length: int = 100) -> list:
    """DEPRECATED: Mock test data creation is not allowed for academic evaluation.
    
    This function is preserved only for unit testing of data structure validation.
    It must NOT be used for generating measurement artifacts or evaluation results.
    
    For Samsung S22 Android 15 academic evaluation, use only REAL data sources:
    - Actual Samsung S22 device measurements
    - Real sensor data from Shimmer3 GSR+ hardware
    - Authentic network timing measurements
    - Real camera calibration results
    """
    raise RuntimeError(
        "ACADEMIC INTEGRITY VIOLATION: create_mock_test_data() is not allowed for Samsung S22 Android 15 evaluation. "
        "Academic evaluation requires REAL data from actual hardware, not mock/fake/simulated data. "
        "Use actual Samsung S22 Android 15 device measurements only."
    )

# Test data validation utilities
def validate_test_data_structure(data: Dict[str, Any], expected_keys: list) -> bool:
    """Validate that test data has expected structure"""
    return all(key in data for key in expected_keys)

def validate_test_results(results: Dict[str, Any]) -> bool:
    """Validate test results structure and content"""
    required_keys = ["success", "execution_time", "error_count"]
    return validate_test_data_structure(results, required_keys)

# Export commonly used utilities
__all__ = [
    "TestEnvironment",
    "setup_test_environment", 
    "cleanup_test_environment",
    "MockDevice",
    "TestLogger",
    "skip_if_no_hardware",
    "require_network", 
    "slow_test",
    "android_test",
    "hardware_test",
    "assert_within_tolerance",
    "assert_timing_within_bounds",
    "create_test_data_placeholder",  # DEPRECATED: Academic integrity warning only
    "validate_test_data_structure",
    "validate_test_results"
]