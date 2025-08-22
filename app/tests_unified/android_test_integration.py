#!/usr/bin/env python3
"""
Android Device Test Integration
==============================

Integration module for the unified test framework to run comprehensive
Android device tests with IDE connection detection and wireless debugging support.
"""

import json
import logging
import sys
from pathlib import Path
from typing import Dict, List, Optional, Any

# Add project root to path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

# Import test framework components
try:
    from tests_unified.android_device_comprehensive_tests import (
        AndroidDeviceTestRunner,
        TestCategory,
        TestResult
    )
    ANDROID_TESTS_AVAILABLE = True
except ImportError as e:
    print(f"[WARN] Android comprehensive tests not available: {e}")
    ANDROID_TESTS_AVAILABLE = False

try:
    from PythonApp.utils.android_connection_detector import AndroidConnectionDetector
    CONNECTION_DETECTOR_AVAILABLE = True
except ImportError:
    CONNECTION_DETECTOR_AVAILABLE = False


class AndroidTestIntegration:
    """Integration class for Android device testing within the unified framework."""
    
    def __init__(self, output_dir: Optional[str] = None, verbose: bool = True):
        """Initialize Android test integration."""
        self.logger = self._setup_logging(verbose)
        self.output_dir = Path(output_dir) if output_dir else Path.cwd() / "android_test_results"
        self.output_dir.mkdir(exist_ok=True)
        
        if ANDROID_TESTS_AVAILABLE:
            self.test_runner = AndroidDeviceTestRunner(output_dir=str(self.output_dir), verbose=verbose)
        else:
            self.test_runner = None
            
        if CONNECTION_DETECTOR_AVAILABLE:
            self.connection_detector = AndroidConnectionDetector()
        else:
            self.connection_detector = None
            
    def _setup_logging(self, verbose: bool) -> logging.Logger:
        """Setup logging configuration."""
        logger = logging.getLogger(__name__)
        logger.setLevel(logging.DEBUG if verbose else logging.INFO)
        
        if not logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter('[%(asctime)s] Android Tests - %(levelname)s - %(message)s')
            handler.setFormatter(formatter)
            logger.addHandler(handler)
            
        return logger
        
    def detect_android_devices_and_ides(self) -> Dict[str, Any]:
        """
        Detect Android devices and IDE connections.
        
        Returns:
            Detection results with devices and IDE information
        """
        if not self.connection_detector:
            self.logger.warning("Connection detector not available")
            return {'devices': [], 'ides': [], 'summary': {'error': 'Connection detector unavailable'}}
            
        try:
            results = self.connection_detector.detect_all_connections()
            summary = self.connection_detector.get_detection_summary()
            
            self.logger.info(f"Detected {len(results.get('devices', []))} Android devices")
            self.logger.info(f"Detected {len(results.get('ides', []))} IDEs")
            
            # Add wireless debugging information
            wireless_devices = self.connection_detector.get_wireless_debugging_devices()
            for device in wireless_devices:
                self.logger.info(f"Wireless device: {device.device_id} at {device.ip_address}:{device.port}")
                
            return {
                'devices': results.get('devices', []),
                'ides': results.get('ides', []),
                'wireless_devices': wireless_devices,
                'summary': summary
            }
            
        except Exception as e:
            self.logger.error(f"Error detecting devices: {e}")
            return {'devices': [], 'ides': [], 'summary': {'error': str(e)}}
            
    def run_android_device_tests(self, 
                                test_categories: Optional[List[str]] = None,
                                target_devices: Optional[List[str]] = None,
                                generate_report: bool = True) -> Dict[str, Any]:
        """
        Run comprehensive Android device tests.
        
        Args:
            test_categories: List of test category names to run
            target_devices: List of specific device IDs to test
            generate_report: Whether to generate detailed reports
            
        Returns:
            Test execution results
        """
        if not self.test_runner:
            self.logger.error("Android test runner not available")
            return {
                'success': False,
                'error': 'Android test runner not available',
                'summary': {'total_tests_run': 0, 'tests_passed': 0, 'success_rate_percent': 0}
            }
            
        try:
            # Convert category strings to enums if provided
            categories = None
            if test_categories:
                category_map = {cat.value: cat for cat in TestCategory}
                categories = [category_map[cat] for cat in test_categories if cat in category_map]
                
            # Update test runner with specific categories
            if categories:
                self.test_runner.test_categories = categories
                
            self.logger.info("Starting comprehensive Android device tests")
            results = self.test_runner.run_comprehensive_tests(target_devices=target_devices)
            
            if generate_report:
                self._generate_integration_report(results)
                
            return {
                'success': True,
                'results': results,
                'summary': results.get('summary', {}),
                'output_dir': str(self.output_dir)
            }
            
        except Exception as e:
            self.logger.error(f"Error running Android device tests: {e}")
            return {
                'success': False,
                'error': str(e),
                'summary': {'total_tests_run': 0, 'tests_passed': 0, 'success_rate_percent': 0}
            }
            
    def run_quick_android_validation(self) -> Dict[str, Any]:
        """
        Run a quick validation of Android devices and basic functionality.
        
        Returns:
            Quick validation results
        """
        self.logger.info("Running quick Android device validation")
        
        # Detect devices first
        detection_results = self.detect_android_devices_and_ides()
        devices = detection_results.get('devices', [])
        ides = detection_results.get('ides', [])
        
        if not devices:
            return {
                'success': False,
                'message': 'No Android devices detected',
                'devices_found': 0,
                'ides_found': len(ides),
                'recommendations': [
                    'Connect an Android device via USB',
                    'Enable USB debugging on the device',
                    'Install ADB if not available',
                    'Try wireless debugging connection'
                ]
            }
            
        # Run essential tests only
        essential_categories = ['ui_tests', 'functional_tests', 'requirements_tests']
        
        results = self.run_android_device_tests(
            test_categories=essential_categories,
            generate_report=False
        )
        
        if results['success']:
            summary = results['summary']
            return {
                'success': True,
                'message': f"Quick validation completed successfully",
                'devices_tested': summary.get('total_devices_tested', 0),
                'tests_run': summary.get('total_tests_run', 0),
                'success_rate': summary.get('success_rate_percent', 0),
                'ides_detected': len(ides),
                'devices_found': len(devices)
            }
        else:
            return {
                'success': False,
                'message': 'Quick validation failed',
                'error': results.get('error', 'Unknown error'),
                'devices_found': len(devices),
                'ides_found': len(ides)
            }
            
    def run_ide_integrated_tests(self) -> Dict[str, Any]:
        """
        Run tests specifically designed for IDE integration.
        
        Returns:
            IDE integration test results
        """
        self.logger.info("Running IDE-integrated Android tests")
        
        # Detect IDE connections
        detection_results = self.detect_android_devices_and_ides()
        ides = detection_results.get('ides', [])
        devices = detection_results.get('devices', [])
        
        if not ides:
            return {
                'success': False,
                'message': 'No IDE connections detected',
                'recommendations': [
                    'Start Android Studio or IntelliJ IDEA',
                    'Open an Android project',
                    'Connect an Android device',
                    'Ensure the device is recognized by the IDE'
                ]
            }
            
        # Check for IDE-device correlation
        ide_device_pairs = []
        for ide in ides:
            for device in devices:
                # Simple correlation heuristic
                if self._correlate_ide_with_device(ide, device):
                    ide_device_pairs.append((ide, device))
                    
        if not ide_device_pairs:
            return {
                'success': False,
                'message': 'No IDE-device correlations found',
                'ides_detected': len(ides),
                'devices_detected': len(devices),
                'recommendations': [
                    'Ensure the device is connected to the IDE',
                    'Check that the Android project is open',
                    'Verify device appears in IDE device selector'
                ]
            }
            
        # Run comprehensive tests with IDE context
        results = self.run_android_device_tests(generate_report=True)
        
        # Add IDE integration information
        if results['success']:
            results['ide_integration'] = {
                'ides_detected': len(ides),
                'devices_correlated': len(ide_device_pairs),
                'ide_details': ides,
                'correlation_success': len(ide_device_pairs) > 0
            }
            
        return results
        
    def _correlate_ide_with_device(self, ide: Dict, device: Dict) -> bool:
        """
        Correlate an IDE with a device based on project context.
        
        This is a simplified heuristic - in practice would be more sophisticated.
        """
        # Check if IDE has Android-related project paths
        project_paths = ide.get('project_paths', [])
        android_indicators = ['android', 'bucika', 'multisensor', 'mobile']
        
        for path in project_paths:
            if any(indicator in str(path).lower() for indicator in android_indicators):
                return True
                
        return False
        
    def _generate_integration_report(self, results: Dict[str, Any]) -> None:
        """Generate an integration-specific report."""
        report_file = self.output_dir / "android_integration_report.md"
        
        with open(report_file, 'w') as f:
            f.write("# Android Device Testing Integration Report\n\n")
            
            summary = results.get('summary', {})
            f.write("## Executive Summary\n\n")
            f.write(f"- **Devices Tested**: {summary.get('total_devices_tested', 0)}\n")
            f.write(f"- **Total Tests**: {summary.get('total_tests_run', 0)}\n")
            f.write(f"- **Success Rate**: {summary.get('success_rate_percent', 0):.1f}%\n")
            f.write(f"- **ADB Available**: {summary.get('adb_available', False)}\n\n")
            
            # Device details
            f.write("## Device Summary\n\n")
            for device in results.get('device_summary', []):
                f.write(f"### Device: {device.get('device_id', 'Unknown')}\n\n")
                f.write(f"- **Model**: {device.get('model', 'Unknown')}\n")
                f.write(f"- **Android Version**: {device.get('android_version', 'Unknown')}\n")
                f.write(f"- **Connection Type**: {device.get('connection_type', 'Unknown')}\n")
                f.write(f"- **IDE Connected**: {device.get('ide_connected', False)}\n")
                f.write(f"- **Tests Passed**: {device.get('tests_passed', 0)}/{device.get('tests_run', 0)}\n\n")
                
            # Category breakdown
            f.write("## Test Category Results\n\n")
            for category, stats in results.get('category_breakdown', {}).items():
                f.write(f"### {category.replace('_', ' ').title()}\n\n")
                f.write(f"- **Total**: {stats.get('total', 0)}\n")
                f.write(f"- **Passed**: {stats.get('passed', 0)}\n")
                f.write(f"- **Failed**: {stats.get('failed', 0)}\n")
                f.write(f"- **Skipped**: {stats.get('skipped', 0)}\n")
                f.write(f"- **Errors**: {stats.get('errors', 0)}\n\n")
                
            # Recommendations
            f.write("## Recommendations\n\n")
            if summary.get('success_rate_percent', 0) < 80:
                f.write("- **Low Success Rate**: Investigate failing tests and device issues\n")
            if not summary.get('adb_available', False):
                f.write("- **ADB Unavailable**: Install Android SDK and add ADB to PATH\n")
            if summary.get('total_devices_tested', 0) == 0:
                f.write("- **No Devices**: Connect Android devices and enable USB debugging\n")
                
        self.logger.info(f"Integration report saved to {report_file}")
        
    def get_test_status(self) -> Dict[str, Any]:
        """Get current status of Android testing capabilities."""
        status = {
            'android_tests_available': ANDROID_TESTS_AVAILABLE,
            'connection_detector_available': CONNECTION_DETECTOR_AVAILABLE,
            'adb_available': False,
            'capabilities': []
        }
        
        if ANDROID_TESTS_AVAILABLE:
            status['capabilities'].append('Comprehensive Android testing')
            
        if CONNECTION_DETECTOR_AVAILABLE:
            status['capabilities'].append('Device and IDE detection')
            status['capabilities'].append('Wireless debugging support')
            
        # Check ADB availability
        if self.test_runner:
            status['adb_available'] = self.test_runner.adb_available
            if status['adb_available']:
                status['capabilities'].append('Direct device communication')
                
        return status


def main():
    """Main function for Android test integration."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Android Device Test Integration')
    parser.add_argument('--mode', choices=['detect', 'quick', 'full', 'ide'], 
                       default='quick', help='Test mode to run')
    parser.add_argument('--categories', nargs='+',
                       help='Test categories to run')
    parser.add_argument('--devices', nargs='+',
                       help='Specific device IDs to test')
    parser.add_argument('--output-dir',
                       help='Output directory for results')
    parser.add_argument('--json', action='store_true',
                       help='Output in JSON format')
    parser.add_argument('--verbose', action='store_true',
                       help='Enable verbose logging')
    
    args = parser.parse_args()
    
    # Create integration instance
    integration = AndroidTestIntegration(
        output_dir=args.output_dir,
        verbose=args.verbose
    )
    
    try:
        if args.mode == 'detect':
            results = integration.detect_android_devices_and_ides()
        elif args.mode == 'quick':
            results = integration.run_quick_android_validation()
        elif args.mode == 'full':
            results = integration.run_android_device_tests(
                test_categories=args.categories,
                target_devices=args.devices
            )
        elif args.mode == 'ide':
            results = integration.run_ide_integrated_tests()
        else:
            print(f"Unknown mode: {args.mode}")
            return 1
            
        if args.json:
            print(json.dumps(results, indent=2, default=str))
        else:
            if results.get('success', False):
                print(f"[PASS] {args.mode.title()} mode completed successfully")
                summary = results.get('summary', {})
                if 'total_tests_run' in summary:
                    print(f"Tests run: {summary['total_tests_run']}")
                    print(f"Success rate: {summary.get('success_rate_percent', 0):.1f}%")
            else:
                print(f"[FAIL] {args.mode.title()} mode failed")
                if 'error' in results:
                    print(f"Error: {results['error']}")
                if 'message' in results:
                    print(f"Message: {results['message']}")
                    
        return 0 if results.get('success', False) else 1
        
    except KeyboardInterrupt:
        print("\nOperation interrupted by user")
        return 1
    except Exception as e:
        print(f"Error: {e}")
        return 1


if __name__ == "__main__":
    exit(main())