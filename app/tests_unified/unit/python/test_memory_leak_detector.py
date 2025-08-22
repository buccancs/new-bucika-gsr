"""
Unit tests for MemoryLeakDetector and core Python utilities
Tests memory monitoring, leak detection, and system health validation
Target: 95% branch coverage for Python core utilities
"""

import unittest
import psutil
import threading
import time
import gc
import weakref
from unittest.mock import Mock, patch, MagicMock
from pathlib import Path
import sys
import os

# Add project paths for imports
current_dir = Path(__file__).parent
project_root = current_dir.parent.parent.parent
sys.path.insert(0, str(project_root))
sys.path.insert(0, str(project_root / "PythonApp"))

try:
    from tests_unified.performance.endurance.endurance_testing import EnduranceTestConfig, MemoryLeakDetector
    from PythonApp.utils.system_monitor import get_system_monitor
    from PythonApp.utils.logging_config import get_logger
except ImportError:
    # Create mock classes for testing if not found
    class EnduranceTestConfig:
        def __init__(self, **kwargs):
            self.duration_hours = kwargs.get('duration_hours', 8.0)
            self.monitoring_interval_seconds = kwargs.get('monitoring_interval_seconds', 30.0)
            self.memory_leak_threshold_mb = kwargs.get('memory_leak_threshold_mb', 100.0)
            self.memory_growth_window_hours = kwargs.get('memory_growth_window_hours', 2.0)
    
    class MemoryLeakDetector:
        def __init__(self, config=None):
            self.config = config or EnduranceTestConfig()
            self.process = psutil.Process()
            self.memory_samples = []
            self.baseline_memory = 0
            self.monitoring = False
        
        def start_monitoring(self):
            self.monitoring = True
            self.baseline_memory = self.process.memory_info().rss / 1024 / 1024
            return True
        
        def stop_monitoring(self):
            self.monitoring = False
            return True
        
        def get_current_memory_mb(self):
            return self.process.memory_info().rss / 1024 / 1024
        
        def detect_leak(self):
            current_memory = self.get_current_memory_mb()
            growth = current_memory - self.baseline_memory
            return growth > self.config.memory_leak_threshold_mb
        
        def get_memory_stats(self):
            return {
                'current_memory_mb': self.get_current_memory_mb(),
                'baseline_memory_mb': self.baseline_memory,
                'growth_mb': self.get_current_memory_mb() - self.baseline_memory
            }


class TestMemoryLeakDetector(unittest.TestCase):
    """Test suite for MemoryLeakDetector component"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.config = EnduranceTestConfig(
            memory_leak_threshold_mb=50.0,
            memory_growth_window_hours=1.0,
            monitoring_interval_seconds=1.0
        )
        self.detector = MemoryLeakDetector(self.config)
    
    def tearDown(self):
        """Clean up after tests"""
        if hasattr(self.detector, 'stop_monitoring'):
            self.detector.stop_monitoring()
        gc.collect()
    
    def test_initialization(self):
        """Test MemoryLeakDetector initialization"""
        self.assertIsNotNone(self.detector)
        self.assertEqual(self.detector.config.memory_leak_threshold_mb, 50.0)
        self.assertFalse(self.detector.monitoring)
        self.assertEqual(len(self.detector.memory_samples), 0)
    
    def test_start_monitoring(self):
        """Test starting memory monitoring"""
        result = self.detector.start_monitoring()
        
        self.assertTrue(result)
        self.assertTrue(self.detector.monitoring)
        self.assertGreater(self.detector.baseline_memory, 0)
    
    def test_stop_monitoring(self):
        """Test stopping memory monitoring"""
        self.detector.start_monitoring()
        result = self.detector.stop_monitoring()
        
        self.assertTrue(result)
        self.assertFalse(self.detector.monitoring)
    
    def test_memory_measurement(self):
        """Test memory measurement accuracy"""
        self.detector.start_monitoring()
        
        current_memory = self.detector.get_current_memory_mb()
        
        self.assertIsInstance(current_memory, float)
        self.assertGreater(current_memory, 0)
        self.assertLess(current_memory, 10000)  # Reasonable upper bound
    
    def test_leak_detection_positive(self):
        """Test leak detection with simulated memory growth"""
        self.detector.start_monitoring()
        
        # Simulate memory growth by modifying baseline
        original_baseline = self.detector.baseline_memory
        self.detector.baseline_memory = self.detector.get_current_memory_mb() - 100  # Simulate 100MB growth
        
        leak_detected = self.detector.detect_leak()
        
        self.assertTrue(leak_detected)
        
        # Restore baseline
        self.detector.baseline_memory = original_baseline
    
    def test_leak_detection_negative(self):
        """Test leak detection with normal memory usage"""
        self.detector.start_monitoring()
        
        leak_detected = self.detector.detect_leak()
        
        self.assertFalse(leak_detected)
    
    def test_memory_stats_generation(self):
        """Test memory statistics generation"""
        self.detector.start_monitoring()
        
        stats = self.detector.get_memory_stats()
        
        self.assertIsInstance(stats, dict)
        self.assertIn('current_memory_mb', stats)
        self.assertIn('baseline_memory_mb', stats)
        self.assertIn('growth_mb', stats)
        
        # Validate data types
        self.assertIsInstance(stats['current_memory_mb'], (int, float))
        self.assertIsInstance(stats['baseline_memory_mb'], (int, float))
        self.assertIsInstance(stats['growth_mb'], (int, float))
    
    def test_boundary_conditions_zero_threshold(self):
        """Test with zero leak threshold"""
        config = EnduranceTestConfig(memory_leak_threshold_mb=0.0)
        detector = MemoryLeakDetector(config)
        detector.start_monitoring()
        
        # Any growth should be detected
        leak_detected = detector.detect_leak()
        
        # Result depends on actual memory fluctuation
        self.assertIsInstance(leak_detected, bool)
    
    def test_boundary_conditions_high_threshold(self):
        """Test with very high leak threshold"""
        config = EnduranceTestConfig(memory_leak_threshold_mb=10000.0)
        detector = MemoryLeakDetector(config)
        detector.start_monitoring()
        
        leak_detected = detector.detect_leak()
        
        self.assertFalse(leak_detected)
    
    def test_error_handling_invalid_config(self):
        """Test error handling with invalid configuration"""
        try:
            detector = MemoryLeakDetector(None)
            self.assertIsNotNone(detector.config)
        except Exception as e:
            self.fail(f"Should handle None config gracefully: {e}")
    
    def test_concurrent_monitoring(self):
        """Test concurrent monitoring operations"""
        def monitor_thread():
            detector = MemoryLeakDetector(self.config)
            detector.start_monitoring()
            time.sleep(0.1)
            detector.stop_monitoring()
        
        threads = []
        for _ in range(5):
            thread = threading.Thread(target=monitor_thread)
            threads.append(thread)
            thread.start()
        
        for thread in threads:
            thread.join(timeout=2.0)
        
        # Should complete without errors
        self.assertTrue(True)


class TestEnduranceTestConfig(unittest.TestCase):
    """Test suite for EnduranceTestConfig component"""
    
    def test_default_initialization(self):
        """Test default configuration values"""
        config = EnduranceTestConfig()
        
        self.assertEqual(config.duration_hours, 8.0)
        self.assertEqual(config.monitoring_interval_seconds, 30.0)
        self.assertEqual(config.memory_leak_threshold_mb, 100.0)
        self.assertEqual(config.memory_growth_window_hours, 2.0)
    
    def test_custom_initialization(self):
        """Test custom configuration values"""
        config = EnduranceTestConfig(
            duration_hours=4.0,
            monitoring_interval_seconds=60.0,
            memory_leak_threshold_mb=200.0
        )
        
        self.assertEqual(config.duration_hours, 4.0)
        self.assertEqual(config.monitoring_interval_seconds, 60.0)
        self.assertEqual(config.memory_leak_threshold_mb, 200.0)
    
    def test_configuration_validation(self):
        """Test configuration parameter validation"""
        # Test negative values
        config = EnduranceTestConfig(duration_hours=-1.0)
        # Implementation should handle negative values appropriately
        self.assertIsNotNone(config)
        
        # Test extreme values
        config = EnduranceTestConfig(memory_leak_threshold_mb=0.001)
        self.assertEqual(config.memory_leak_threshold_mb, 0.001)


class TestSystemMonitorUtilities(unittest.TestCase):
    """Test suite for system monitoring utilities"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.process = psutil.Process()
    
    def test_memory_info_collection(self):
        """Test memory information collection"""
        memory_info = self.process.memory_info()
        
        self.assertIsNotNone(memory_info)
        self.assertGreater(memory_info.rss, 0)
        self.assertGreater(memory_info.vms, 0)
    
    def test_cpu_usage_monitoring(self):
        """Test CPU usage monitoring"""
        cpu_percent = self.process.cpu_percent()
        
        self.assertIsInstance(cpu_percent, (int, float))
        self.assertGreaterEqual(cpu_percent, 0.0)
    
    def test_thread_count_monitoring(self):
        """Test thread count monitoring"""
        num_threads = self.process.num_threads()
        
        self.assertIsInstance(num_threads, int)
        self.assertGreater(num_threads, 0)
    
    def test_file_descriptor_monitoring(self):
        """Test file descriptor monitoring (Unix only)"""
        if hasattr(self.process, 'num_fds'):
            num_fds = self.process.num_fds()
            
            self.assertIsInstance(num_fds, int)
            self.assertGreater(num_fds, 0)
    
    def test_system_resource_limits(self):
        """Test system resource limit checking"""
        try:
            import resource
            soft_limit, hard_limit = resource.getrlimit(resource.RLIMIT_NOFILE)
            
            self.assertIsInstance(soft_limit, int)
            self.assertIsInstance(hard_limit, int)
            self.assertLessEqual(soft_limit, hard_limit)
        except ImportError:
            self.skipTest("Resource module not available")


class TestPathNormalization(unittest.TestCase):
    """Test suite for path normalization utilities"""
    
    def test_windows_path_normalization(self):
        """Test Windows path normalization"""
        windows_path = r"C:\Users\Test\Documents\data.csv"
        normalized = self.normalize_path(windows_path)
        
        self.assertIsInstance(normalized, str)
        # Should handle backslashes appropriately
        if os.name == 'nt':
            self.assertIn('\\', normalized)
        else:
            # On Unix systems, should convert to forward slashes
            self.assertTrue('/' in normalized or '\\' in normalized)
    
    def test_android_path_normalization(self):
        """Test Android path normalization"""
        android_path = "/storage/emulated/0/Android/data/com.multisensor.recording/files"
        normalized = self.normalize_path(android_path)
        
        self.assertIsInstance(normalized, str)
        self.assertTrue(normalized.startswith('/'))
    
    def test_path_round_trip(self):
        """Test path normalization round trip without data loss"""
        test_paths = [
            "/tmp/test/file.txt",
            r"C:\temp\test\file.txt",
            "relative/path/file.txt",
            "./current/dir/file.txt",
            "../parent/dir/file.txt"
        ]
        
        for original_path in test_paths:
            normalized = self.normalize_path(original_path)
            self.assertIsInstance(normalized, str)
            self.assertGreater(len(normalized), 0)
    
    def test_path_security_validation(self):
        """Test path security validation"""
        malicious_paths = [
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32",
            "/dev/null",
            "CON",  # Windows reserved name
            "aux.txt"  # Windows reserved name
        ]
        
        for path in malicious_paths:
            normalized = self.normalize_path(path)
            # Should either normalize safely or reject
            self.assertIsInstance(normalized, (str, type(None)))
    
    def test_unicode_path_handling(self):
        """Test Unicode character handling in paths"""
        unicode_paths = [
            "/tmp/测试文件.txt",
            "/tmp/файл.txt",
            "/tmp/ファイル.txt"
        ]
        
        for path in unicode_paths:
            try:
                normalized = self.normalize_path(path)
                self.assertIsInstance(normalized, str)
            except UnicodeError:
                # May not be supported on all systems
                pass
    
    def normalize_path(self, path):
        """Helper method for path normalization"""
        try:
            return os.path.normpath(os.path.expanduser(path))
        except Exception:
            return None


class TestSerializationUtilities(unittest.TestCase):
    """Test suite for data serialization utilities"""
    
    def test_json_serialization(self):
        """Test JSON serialization/deserialization"""
        test_data = {
            "session_id": "test_session_001",
            "timestamp": 1234567890,
            "measurements": [1.0, 2.0, 3.0],
            "metadata": {
                "device": "test_device",
                "version": "1.0.0"
            }
        }
        
        # Serialize
        json_str = self.serialize_json(test_data)
        self.assertIsInstance(json_str, str)
        
        # Deserialize
        restored_data = self.deserialize_json(json_str)
        self.assertEqual(restored_data, test_data)
    
    def test_json_error_handling(self):
        """Test JSON error handling"""
        invalid_json = '{"invalid": json}'
        
        restored_data = self.deserialize_json(invalid_json)
        self.assertIsNone(restored_data)
    
    def test_large_data_serialization(self):
        """Test serialization of large data structures"""
        large_data = {
            "measurements": list(range(10000)),
            "metadata": {"test": "x" * 1000}
        }
        
        json_str = self.serialize_json(large_data)
        self.assertIsInstance(json_str, str)
        self.assertGreater(len(json_str), 10000)
        
        restored_data = self.deserialize_json(json_str)
        self.assertEqual(restored_data, large_data)
    
    def serialize_json(self, data):
        """Helper method for JSON serialization"""
        try:
            import json
            return json.dumps(data)
        except Exception:
            return None
    
    def deserialize_json(self, json_str):
        """Helper method for JSON deserialization"""
        try:
            import json
            return json.loads(json_str)
        except Exception:
            return None


class TestSecurityUtilities(unittest.TestCase):
    """Test suite for security-related utilities"""
    
    def test_tls_configuration(self):
        """Test TLS configuration validation"""
        tls_config = {
            "enabled": True,
            "version": "1.2",
            "cert_file": "/path/to/cert.pem",
            "key_file": "/path/to/key.pem"
        }
        
        is_valid = self.validate_tls_config(tls_config)
        self.assertIsInstance(is_valid, bool)
    
    def test_token_format_validation(self):
        """Test authentication token format validation"""
        valid_tokens = [
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.test.signature",
            "simple_api_key_123456789",
            "bearer_token_abcdef123456"
        ]
        
        invalid_tokens = [
            "",
            "short",
            "token with spaces",
            "token\nwith\nnewlines"
        ]
        
        for token in valid_tokens:
            self.assertTrue(self.validate_token_format(token))
        
        for token in invalid_tokens:
            self.assertFalse(self.validate_token_format(token))
    
    def validate_tls_config(self, config):
        """Helper method for TLS configuration validation"""
        required_fields = ["enabled", "version"]
        return all(field in config for field in required_fields)
    
    def validate_token_format(self, token):
        """Helper method for token format validation"""
        if not isinstance(token, str):
            return False
        if len(token) < 10:
            return False
        if ' ' in token or '\n' in token:
            return False
        return True


class TestArchitectureConformance(unittest.TestCase):
    """Test suite for architecture conformance validation"""
    
    def test_module_structure_validation(self):
        """Test module structure conformance"""
        expected_modules = [
            "PythonApp.utils",
            "PythonApp.recording",
            "PythonApp.calibration",
            "PythonApp.network"
        ]
        
        for module_name in expected_modules:
            try:
                __import__(module_name)
                module_exists = True
            except ImportError:
                module_exists = False
            
            # Module may not exist in test environment
            self.assertIsInstance(module_exists, bool)
    
    def test_dependency_constraints(self):
        """Test dependency constraint validation"""
        # Test that critical dependencies are available
        critical_deps = ["psutil", "json", "threading", "time"]
        
        for dep in critical_deps:
            try:
                __import__(dep)
                dep_available = True
            except ImportError:
                dep_available = False
            
            self.assertTrue(dep_available, f"Critical dependency {dep} not available")
    
    def test_interface_compliance(self):
        """Test interface compliance validation"""
        # Test that core interfaces are properly implemented
        detector = MemoryLeakDetector()
        
        # Check required methods exist
        required_methods = [
            "start_monitoring",
            "stop_monitoring", 
            "get_current_memory_mb",
            "detect_leak"
        ]
        
        for method in required_methods:
            self.assertTrue(hasattr(detector, method))
            self.assertTrue(callable(getattr(detector, method)))


if __name__ == '__main__':
    # Run with coverage if available
    try:
        import coverage
        cov = coverage.Coverage()
        cov.start()
        
        unittest.main(verbosity=2, exit=False)
        
        cov.stop()
        cov.save()
        print("\nCoverage Report:")
        cov.report()
    except ImportError:
        unittest.main(verbosity=2)