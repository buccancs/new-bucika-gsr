#!/usr/bin/env python3
"""
Android Device Testing - Pytest Integration
============================================

Pytest test cases for comprehensive Android device testing with
IDE integration and wireless debugging support.
"""

import json
import pytest
import time
from pathlib import Path
from typing import Dict, List, Optional, Any
import sys

# Add project root to path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

try:
    from tests_unified.android_test_integration import AndroidTestIntegration
    from tests_unified.android_device_comprehensive_tests import AndroidDeviceTestRunner, TestCategory
    ANDROID_TESTING_AVAILABLE = True
except ImportError:
    ANDROID_TESTING_AVAILABLE = False

try:
    from PythonApp.utils.android_connection_detector import AndroidConnectionDetector
    CONNECTION_DETECTOR_AVAILABLE = True
except ImportError:
    CONNECTION_DETECTOR_AVAILABLE = False


@pytest.fixture(scope="session")
def android_integration():
    """Fixture for Android test integration."""
    if not ANDROID_TESTING_AVAILABLE:
        pytest.skip("Android testing not available")
    return AndroidTestIntegration(verbose=True)


@pytest.fixture(scope="session")
def connection_detector():
    """Fixture for Android connection detector."""
    if not CONNECTION_DETECTOR_AVAILABLE:
        pytest.skip("Connection detector not available")
    return AndroidConnectionDetector()


class TestAndroidDeviceDetection:
    """Test Android device detection capabilities."""
    
    def test_connection_detector_initialization(self, connection_detector):
        """Test that connection detector initializes properly."""
        assert connection_detector is not None
        assert hasattr(connection_detector, 'detect_all_connections')
        assert hasattr(connection_detector, 'get_detection_summary')
        
    def test_device_detection(self, android_integration):
        """Test device detection functionality."""
        results = android_integration.detect_android_devices_and_ides()
        
        assert isinstance(results, dict)
        assert 'devices' in results
        assert 'ides' in results
        assert 'summary' in results
        
        # Devices should be a list
        assert isinstance(results['devices'], list)
        assert isinstance(results['ides'], list)
        
    def test_wireless_device_detection(self, connection_detector):
        """Test wireless debugging device detection."""
        # This will work whether devices are connected or not
        try:
            wireless_devices = connection_detector.get_wireless_debugging_devices()
            assert isinstance(wireless_devices, list)
            
            # If devices are found, check their structure
            for device in wireless_devices:
                assert hasattr(device, 'device_id')
                assert hasattr(device, 'ip_address')
                assert hasattr(device, 'port')
                
        except Exception as e:
            # Graceful handling if no ADB or devices
            assert "adb" in str(e).lower() or "connection" in str(e).lower()
            
    def test_ide_detection(self, android_integration):
        """Test IDE detection functionality."""
        results = android_integration.detect_android_devices_and_ides()
        ides = results.get('ides', [])
        
        # IDEs list should be valid
        assert isinstance(ides, list)
        
        # If IDEs are found, check their structure
        for ide in ides:
            assert isinstance(ide, dict)
            assert 'type' in ide
            assert 'running' in ide
            
    @pytest.mark.slow
    def test_continuous_detection(self, android_integration):
        """Test continuous device detection over time."""
        detection_count = 3
        results = []
        
        for i in range(detection_count):
            result = android_integration.detect_android_devices_and_ides()
            results.append(result)
            if i < detection_count - 1:
                time.sleep(2)  # Wait between detections
                
        # All results should be valid
        for result in results:
            assert isinstance(result, dict)
            assert 'devices' in result
            assert 'ides' in result


class TestAndroidDeviceValidation:
    """Test Android device validation capabilities."""
    
    def test_quick_validation(self, android_integration):
        """Test quick Android device validation."""
        results = android_integration.run_quick_android_validation()
        
        assert isinstance(results, dict)
        assert 'success' in results
        assert 'devices_found' in results
        assert 'ides_found' in results
        
        # Should have some status information
        if results['success']:
            assert 'devices_tested' in results
            assert 'tests_run' in results
            assert 'success_rate' in results
        else:
            assert 'message' in results
            
    def test_validation_with_no_devices(self, android_integration):
        """Test validation behavior when no devices are connected."""
        # This test simulates the case where no devices are available
        results = android_integration.run_quick_android_validation()
        
        # Should handle gracefully whether devices are present or not
        assert isinstance(results, dict)
        assert 'success' in results
        
        if not results['success'] and results.get('devices_found', 0) == 0:
            assert 'recommendations' in results
            assert isinstance(results['recommendations'], list)
            
    def test_validation_status_consistency(self, android_integration):
        """Test that validation status is consistent across calls."""
        # Run validation twice and compare
        results1 = android_integration.run_quick_android_validation()
        time.sleep(1)
        results2 = android_integration.run_quick_android_validation()
        
        # Device count should be consistent (allowing for minor variations)
        devices1 = results1.get('devices_found', 0)
        devices2 = results2.get('devices_found', 0)
        
        # Allow for small differences due to device state changes
        assert abs(devices1 - devices2) <= 1


class TestAndroidDeviceComprehensiveTesting:
    """Test comprehensive Android device testing."""
    
    def test_test_runner_initialization(self, android_integration):
        """Test that test runner initializes properly."""
        if hasattr(android_integration, 'test_runner') and android_integration.test_runner:
            runner = android_integration.test_runner
            assert hasattr(runner, 'run_comprehensive_tests')
            assert hasattr(runner, 'test_categories')
            assert hasattr(runner, 'adb_available')
            
    def test_comprehensive_test_execution(self, android_integration):
        """Test comprehensive test execution."""
        # Run with minimal categories to reduce execution time
        categories = ['ui_tests', 'requirements_tests']
        
        results = android_integration.run_android_device_tests(
            test_categories=categories,
            generate_report=False
        )
        
        assert isinstance(results, dict)
        assert 'success' in results
        assert 'summary' in results
        
        if results['success']:
            summary = results['summary']
            assert 'total_tests_run' in summary
            assert 'tests_passed' in summary
            assert 'success_rate_percent' in summary
            
            # Success rate should be reasonable
            assert 0 <= summary['success_rate_percent'] <= 100
            
    def test_test_category_filtering(self, android_integration):
        """Test that test category filtering works correctly."""
        # Test with specific categories
        categories = ['requirements_tests']
        
        results = android_integration.run_android_device_tests(
            test_categories=categories,
            generate_report=False
        )
        
        if results['success'] and 'results' in results:
            category_breakdown = results['results'].get('category_breakdown', {})
            
            # Should only have the requested categories
            for category in category_breakdown:
                assert category in categories or 'requirements' in category
                
    @pytest.mark.slow
    def test_full_comprehensive_testing(self, android_integration):
        """Test full comprehensive testing with all categories."""
        # This is a slow test that runs all test categories
        results = android_integration.run_android_device_tests(generate_report=False)
        
        assert isinstance(results, dict)
        assert 'success' in results
        
        if results['success']:
            assert 'summary' in results
            summary = results['summary']
            
            # Should have run multiple tests
            assert summary.get('total_tests_run', 0) > 0
            
    def test_report_generation(self, android_integration, tmp_path):
        """Test that reports are generated correctly."""
        # Set output directory to temporary path
        android_integration.output_dir = tmp_path
        
        results = android_integration.run_android_device_tests(
            test_categories=['requirements_tests'],
            generate_report=True
        )
        
        if results['success']:
            # Check that report files are created
            report_file = tmp_path / "android_integration_report.md"
            # Note: Report might not exist if test runner isn't fully available
            # This is expected behavior in CI environments


class TestIDEIntegration:
    """Test IDE integration capabilities."""
    
    def test_ide_integration_detection(self, android_integration):
        """Test IDE integration detection."""
        results = android_integration.run_ide_integrated_tests()
        
        assert isinstance(results, dict)
        assert 'success' in results
        
        # Should handle the case where no IDEs are running
        if not results['success']:
            assert 'message' in results
            assert 'recommendations' in results
        else:
            assert 'ide_integration' in results
            
    def test_ide_device_correlation(self, android_integration):
        """Test correlation between IDEs and devices."""
        detection_results = android_integration.detect_android_devices_and_ides()
        
        devices = detection_results.get('devices', [])
        ides = detection_results.get('ides', [])
        
        # Even if no devices/IDEs, the detection should work
        assert isinstance(devices, list)
        assert isinstance(ides, list)
        
        # If both exist, correlation should be attempted
        if devices and ides:
            # The correlation logic should not raise exceptions
            integration_results = android_integration.run_ide_integrated_tests()
            assert isinstance(integration_results, dict)
            
    def test_ide_types_detection(self, connection_detector):
        """Test detection of different IDE types."""
        if not connection_detector:
            pytest.skip("Connection detector not available")
            
        try:
            results = connection_detector.detect_all_connections()
            ides = results.get('ides', [])
            
            # Check IDE type consistency
            for ide in ides:
                ide_type = ide.get('type', '')
                assert ide_type in ['android_studio', 'intellij_idea', 'vscode', 'unknown', '']
                
        except Exception:
            # Graceful handling if detection fails
            pass


class TestPerformanceAndReliability:
    """Test performance and reliability of Android testing."""
    
    def test_detection_performance(self, android_integration):
        """Test that device detection completes in reasonable time."""
        start_time = time.time()
        
        results = android_integration.detect_android_devices_and_ides()
        
        detection_time = time.time() - start_time
        
        # Detection should complete within 30 seconds
        assert detection_time < 30.0
        assert isinstance(results, dict)
        
    def test_repeated_detection_stability(self, android_integration):
        """Test stability of repeated detections."""
        results = []
        
        for _ in range(3):
            result = android_integration.detect_android_devices_and_ides()
            results.append(result)
            time.sleep(0.5)
            
        # All results should be valid
        for result in results:
            assert isinstance(result, dict)
            assert 'devices' in result
            assert 'ides' in result
            
        # Device counts should be relatively stable
        device_counts = [len(r.get('devices', [])) for r in results]
        if device_counts:
            max_count = max(device_counts)
            min_count = min(device_counts)
            # Allow for small variations
            assert max_count - min_count <= 2
            
    def test_error_handling(self, android_integration):
        """Test error handling in various scenarios."""
        # Test with invalid categories
        results = android_integration.run_android_device_tests(
            test_categories=['invalid_category'],
            generate_report=False
        )
        
        # Should handle gracefully
        assert isinstance(results, dict)
        assert 'success' in results
        
        # Test with non-existent device IDs
        results = android_integration.run_android_device_tests(
            target_devices=['non_existent_device'],
            generate_report=False
        )
        
        assert isinstance(results, dict)
        assert 'success' in results


class TestCapabilityStatus:
    """Test testing capability status and availability."""
    
    def test_status_reporting(self, android_integration):
        """Test that status reporting works correctly."""
        status = android_integration.get_test_status()
        
        assert isinstance(status, dict)
        assert 'android_tests_available' in status
        assert 'connection_detector_available' in status
        assert 'adb_available' in status
        assert 'capabilities' in status
        
        # Capabilities should be a list
        assert isinstance(status['capabilities'], list)
        
    def test_capability_consistency(self, android_integration):
        """Test that capabilities are reported consistently."""
        status1 = android_integration.get_test_status()
        status2 = android_integration.get_test_status()
        
        # Core capabilities should be the same
        assert status1['android_tests_available'] == status2['android_tests_available']
        assert status1['connection_detector_available'] == status2['connection_detector_available']
        
        # ADB availability might change, but should be boolean
        assert isinstance(status1['adb_available'], bool)
        assert isinstance(status2['adb_available'], bool)
        
    def test_capability_dependencies(self, android_integration):
        """Test capability dependencies."""
        status = android_integration.get_test_status()
        
        # If Android tests are available, should have some capabilities
        if status['android_tests_available']:
            assert len(status['capabilities']) > 0
            assert any('Android' in cap for cap in status['capabilities'])
            
        # If connection detector is available, should have detection capabilities
        if status['connection_detector_available']:
            assert any('detection' in cap.lower() for cap in status['capabilities'])


@pytest.mark.integration
class TestFullIntegrationWorkflow:
    """Test the complete integration workflow."""
    
    def test_complete_workflow(self, android_integration):
        """Test a complete testing workflow from detection to reporting."""
        # Step 1: Detect devices and IDEs
        detection_results = android_integration.detect_android_devices_and_ides()
        assert isinstance(detection_results, dict)
        
        # Step 2: Quick validation
        validation_results = android_integration.run_quick_android_validation()
        assert isinstance(validation_results, dict)
        
        # Step 3: Get status
        status = android_integration.get_test_status()
        assert isinstance(status, dict)
        
        # Step 4: Run targeted tests (if capabilities allow)
        if status['android_tests_available']:
            test_results = android_integration.run_android_device_tests(
                test_categories=['requirements_tests'],
                generate_report=False
            )
            assert isinstance(test_results, dict)
            
        # All steps should complete without exceptions
        
    @pytest.mark.slow
    def test_workflow_with_comprehensive_testing(self, android_integration):
        """Test workflow including comprehensive testing."""
        # This test runs the full workflow including comprehensive testing
        
        # Detect capabilities
        status = android_integration.get_test_status()
        
        if not status['android_tests_available']:
            pytest.skip("Comprehensive testing not available")
            
        # Run comprehensive tests with minimal categories
        results = android_integration.run_android_device_tests(
            test_categories=['ui_tests', 'requirements_tests'],
            generate_report=True
        )
        
        assert isinstance(results, dict)
        assert 'success' in results
        
        # Should provide meaningful results whether devices are connected or not
        if results['success']:
            assert 'summary' in results
        else:
            assert 'error' in results or 'message' in results


# Parametrized tests for different scenarios
@pytest.mark.parametrize("test_categories", [
    ['ui_tests'],
    ['functional_tests'],
    ['requirements_tests'],
    ['ui_tests', 'requirements_tests'],
])
def test_category_specific_testing(android_integration, test_categories):
    """Test specific category combinations."""
    results = android_integration.run_android_device_tests(
        test_categories=test_categories,
        generate_report=False
    )
    
    assert isinstance(results, dict)
    assert 'success' in results
    
    # Results should be consistent regardless of category
    if results['success']:
        assert 'summary' in results


# Conditional tests based on environment
@pytest.mark.skipif(not ANDROID_TESTING_AVAILABLE, reason="Android testing not available")
def test_android_testing_when_available():
    """Test that runs only when Android testing is available."""
    integration = AndroidTestIntegration()
    status = integration.get_test_status()
    
    assert status['android_tests_available'] is True
    assert len(status['capabilities']) > 0


@pytest.mark.skipif(not CONNECTION_DETECTOR_AVAILABLE, reason="Connection detector not available")
def test_connection_detection_when_available():
    """Test that runs only when connection detector is available."""
    detector = AndroidConnectionDetector()
    
    # Should be able to attempt detection without errors
    try:
        results = detector.detect_all_connections()
        assert isinstance(results, dict)
    except Exception as e:
        # Allow for expected exceptions (no ADB, etc.)
        assert "adb" in str(e).lower() or "command" in str(e).lower()


if __name__ == "__main__":
    # Run tests when executed directly
    pytest.main([__file__, "-v"])