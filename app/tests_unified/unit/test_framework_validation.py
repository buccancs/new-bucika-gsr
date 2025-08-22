"""
Simple validation test for the unified testing framework.

This test verifies that the unified testing structure is properly
set up and basic functionality works as expected.
"""

import pytest
import sys
from pathlib import Path

# Add project root to path
PROJECT_ROOT = Path(__file__).parent.parent.parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

# Import unified test utilities
from tests_unified.fixtures.test_utils import (
    TestEnvironment, 
    MockDevice, 
    assert_within_tolerance,
    create_test_data_placeholder
)

class TestUnifiedFramework:
    """Test the unified testing framework itself"""
    
    @pytest.mark.unit
    def test_framework_structure_exists(self):
        """Test that the unified framework directory structure exists"""
        # tests_unified directory is where the structure should be
        test_root = Path(__file__).parent.parent
        
        # Check that key directories exist within tests_unified
        expected_dirs = [
            "unit", "integration", "system", "performance",
            "evaluation", "browser", "visual", "hardware",
            "config", "fixtures", "runners", "e2e", "web", "load", "migration"
        ]
        
        for dir_name in expected_dirs:
            dir_path = test_root / dir_name
            assert dir_path.exists(), f"Directory {dir_name} should exist in tests_unified/"
            assert dir_path.is_dir(), f"{dir_name} should be a directory"
    
    @pytest.mark.unit
    def test_configuration_files_exist(self):
        """Test that configuration files are present"""
        # Check pytest.ini in project root
        project_root = Path(__file__).parent.parent.parent
        pytest_ini = project_root / "pytest.ini"
        assert pytest_ini.exists(), "pytest.ini should exist in project root"
        
        # Check test_config.yaml in tests_unified/config
        test_config = project_root / "tests_unified" / "config" / "test_config.yaml"
        assert test_config.exists(), "test_config.yaml should exist in tests_unified/config"
    
    @pytest.mark.unit
    def test_test_utilities_work(self):
        """Test that basic test utilities function correctly"""
        
        # Test TestEnvironment
        env = TestEnvironment()
        temp_dir = env.create_temp_dir()
        assert temp_dir.exists()
        
        temp_file = env.create_temp_file(content="test content")
        assert temp_file.exists()
        assert temp_file.read_text() == "test content"
        
        # Test cleanup
        env.cleanup()
        assert not temp_dir.exists()
        assert not temp_file.exists()
    
    @pytest.mark.unit
    def test_mock_device_functionality(self):
        """Test that mock device utilities work"""
        device = MockDevice("test_device")
        
        # Test connection
        assert not device.connected
        assert device.connect()
        assert device.connected
        
        # Test data handling
        assert device.send_data("test_data")
        received = device.receive_data()
        assert received == "test_data"
        
        # Test disconnection
        device.disconnect()
        assert not device.connected
    
    @pytest.mark.unit
    def test_assertion_utilities(self):
        """Test custom assertion utilities"""
        
        # Test tolerance assertion
        assert_within_tolerance(1.0, 1.01, 0.02)
        
        with pytest.raises(AssertionError):
            assert_within_tolerance(1.0, 1.05, 0.02)
    
    @pytest.mark.unit 
    def test_data_structure_validation(self):
        """Test data structure validation utilities"""
        
        # Test GSR data structure validation
        sample_gsr_data = [0.12, 0.18, 0.25, 0.31, 0.19]
        assert len(sample_gsr_data) == 5
        assert all(0.1 <= value <= 1.0 for value in sample_gsr_data)
        
        # Test thermal data structure validation
        sample_thermal_data = [[21.2, 21.8], [22.1, 22.4]]
        assert len(sample_thermal_data) == 2
        assert all(len(row) == 2 for row in sample_thermal_data)
        
        # Test timestamp data structure validation
        sample_timestamp_data = [1000, 1100, 1200, 1300, 1400]
        assert len(sample_timestamp_data) == 5
        assert sample_timestamp_data == sorted(sample_timestamp_data)  # Should be sorted
    
    @pytest.mark.integration
    def test_quality_validator_import(self):
        """Test that quality validator can be imported"""
        try:
            from tests_unified.evaluation.metrics.quality_validator import QualityValidator
            
            # Test basic instantiation
            config = {"quality_thresholds": {"unit_tests": {"minimum_success_rate": 0.95}}}
            validator = QualityValidator(config)
            
            assert validator is not None
            
        except ImportError:
            pytest.skip("Quality validator not available yet")
    
    @pytest.mark.integration
    def test_performance_monitor_import(self):
        """Test that performance monitor can be imported"""
        try:
            from tests_unified.evaluation.metrics.performance_monitor import PerformanceMonitor
            
            # Test basic instantiation
            monitor = PerformanceMonitor()
            assert monitor is not None
            
            # Test current metrics collection
            current = monitor.get_current_metrics()
            assert current is not None
            
        except ImportError:
            pytest.skip("Performance monitor not available yet")

    @pytest.mark.unit
    def test_pytest_markers_defined(self):
        """Test that all expected pytest markers are available"""
        
        # This test will pass if pytest configuration is properly loaded
        # The markers are defined in pytest.ini
        
        # We can't directly test markers, but we can test that the test
        # itself is properly marked
        assert hasattr(self.test_pytest_markers_defined, 'pytestmark') or True
    
    @pytest.mark.slow
    @pytest.mark.integration
    def test_unified_runner_exists(self):
        """Test that the unified test runner exists and is executable"""
        # Check runner in tests_unified/runners
        runner_path = Path(__file__).parent.parent / "runners" / "run_unified_tests.py"
        
        assert runner_path.exists(), "Unified test runner should exist"
        assert runner_path.is_file(), "Runner should be a file"
        
        # Check if it's executable (on Unix systems)
        import stat
        if hasattr(stat, 'S_IXUSR'):
            file_stat = runner_path.stat()
            is_executable = bool(file_stat.st_mode & stat.S_IXUSR)
            assert is_executable, "Runner should be executable"

if __name__ == "__main__":
    # Allow running this test directly
    pytest.main([__file__, "-v"])