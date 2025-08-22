#!/usr/bin/env python3
"""
Android Device Testing Demo
===========================

Demonstration script for comprehensive Android device testing with
IDE integration and wireless debugging support.

This script showcases all testing capabilities:
- Device detection (USB, wireless, emulators)
- IDE integration (Android Studio, IntelliJ IDEA)
- Comprehensive test suites (UI, functional, requirements, etc.)
- Real-time monitoring and reporting
"""

import asyncio
import json
import time
import argparse
import sys
from pathlib import Path
from typing import Dict, Any, List

# Add project root to path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

try:
    from tests_unified.android_test_integration import AndroidTestIntegration
    INTEGRATION_AVAILABLE = True
except ImportError as e:
    print(f"[ERROR] Android test integration not available: {e}")
    INTEGRATION_AVAILABLE = False

try:
    from tests_unified.android_device_comprehensive_tests import TestCategory
    CATEGORIES_AVAILABLE = True
except ImportError:
    CATEGORIES_AVAILABLE = False


class AndroidTestingDemo:
    """Demonstration class for Android device testing capabilities."""
    
    def __init__(self, verbose: bool = True):
        """Initialize the demo."""
        self.verbose = verbose
        if INTEGRATION_AVAILABLE:
            self.integration = AndroidTestIntegration(verbose=verbose)
        else:
            self.integration = None
            
    def print_header(self, title: str) -> None:
        """Print a formatted header."""
        print("\n" + "=" * 60)
        print(f" {title}")
        print("=" * 60)
        
    def print_section(self, title: str) -> None:
        """Print a formatted section header."""
        print(f"\n--- {title} ---")
        
    def print_status(self, message: str, success: bool = True) -> None:
        """Print a status message."""
        status = "[PASS]" if success else "[FAIL]"
        print(f"{status} {message}")
        
    def print_info(self, message: str) -> None:
        """Print an info message."""
        print(f"[INFO] {message}")
        
    def demo_device_detection(self) -> Dict[str, Any]:
        """Demonstrate device and IDE detection capabilities."""
        self.print_header("ANDROID DEVICE & IDE DETECTION DEMO")
        
        if not self.integration:
            self.print_status("Android integration not available", False)
            return {'success': False, 'error': 'Integration unavailable'}
            
        self.print_info("Scanning for Android devices and IDE connections...")
        
        # Detect devices and IDEs
        results = self.integration.detect_android_devices_and_ides()
        
        devices = results.get('devices', [])
        ides = results.get('ides', [])
        wireless_devices = results.get('wireless_devices', [])
        summary = results.get('summary', {})
        
        self.print_section("Detection Results")
        self.print_info(f"USB/Wired Devices: {len(devices)}")
        self.print_info(f"Wireless Debugging Devices: {len(wireless_devices)}")
        self.print_info(f"IDEs Running: {len(ides)}")
        
        # Show device details
        if devices:
            self.print_section("Connected Devices")
            for i, device in enumerate(devices, 1):
                print(f"  Device {i}:")
                print(f"    ID: {device.device_id}")
                print(f"    Status: {device.status}")
                print(f"    Connection: {device.connection_type.value}")
                print(f"    Model: {device.model or 'Unknown'}")
                print(f"    Android: {device.android_version or 'Unknown'}")
                if hasattr(device, 'ip_address') and device.ip_address:
                    print(f"    IP Address: {device.ip_address}:{device.port}")
                    
        # Show wireless devices
        if wireless_devices:
            self.print_section("Wireless Debugging Devices")
            for device in wireless_devices:
                print(f"  Device: {device.device_id}")
                print(f"    IP: {device.ip_address}:{device.port}")
                print(f"    Debugging: {device.debugging_enabled}")
                
        # Show IDE details
        if ides:
            self.print_section("Detected IDEs")
            for i, ide in enumerate(ides, 1):
                print(f"  IDE {i}:")
                print(f"    Type: {ide.get('type', 'Unknown')}")
                print(f"    Version: {ide.get('version', 'Unknown')}")
                print(f"    Running: {ide.get('running', False)}")
                project_paths = ide.get('project_paths', [])
                if project_paths:
                    print(f"    Projects: {len(project_paths)} open")
                    for path in project_paths[:3]:  # Show first 3
                        print(f"      - {path}")
                        
        self.print_status(f"Detection completed: {len(devices + wireless_devices)} devices, {len(ides)} IDEs")
        return results
        
    def demo_quick_validation(self) -> Dict[str, Any]:
        """Demonstrate quick Android device validation."""
        self.print_header("QUICK ANDROID DEVICE VALIDATION DEMO")
        
        if not self.integration:
            self.print_status("Android integration not available", False)
            return {'success': False}
            
        self.print_info("Running quick validation of Android devices...")
        
        results = self.integration.run_quick_android_validation()
        
        if results['success']:
            self.print_status("Quick validation completed successfully")
            self.print_info(f"Devices tested: {results.get('devices_tested', 0)}")
            self.print_info(f"Tests run: {results.get('tests_run', 0)}")
            self.print_info(f"Success rate: {results.get('success_rate', 0):.1f}%")
            self.print_info(f"IDEs detected: {results.get('ides_detected', 0)}")
        else:
            self.print_status("Quick validation failed", False)
            self.print_info(f"Reason: {results.get('message', 'Unknown error')}")
            if 'recommendations' in results:
                self.print_section("Recommendations")
                for rec in results['recommendations']:
                    print(f"  - {rec}")
                    
        return results
        
    def demo_comprehensive_testing(self, categories: List[str] = None) -> Dict[str, Any]:
        """Demonstrate comprehensive Android device testing."""
        self.print_header("COMPREHENSIVE ANDROID DEVICE TESTING DEMO")
        
        if not self.integration:
            self.print_status("Android integration not available", False)
            return {'success': False}
            
        # Use all categories if none specified
        if not categories and CATEGORIES_AVAILABLE:
            categories = [cat.value for cat in TestCategory]
        elif not categories:
            categories = ['ui_tests', 'functional_tests', 'requirements_tests']
            
        self.print_info(f"Running comprehensive tests in categories: {', '.join(categories)}")
        self.print_info("This may take several minutes...")
        
        start_time = time.time()
        results = self.integration.run_android_device_tests(
            test_categories=categories,
            generate_report=True
        )
        execution_time = time.time() - start_time
        
        if results['success']:
            self.print_status("Comprehensive testing completed successfully")
            
            summary = results.get('summary', {})
            self.print_section("Test Results Summary")
            self.print_info(f"Devices tested: {summary.get('total_devices_tested', 0)}")
            self.print_info(f"Total tests run: {summary.get('total_tests_run', 0)}")
            self.print_info(f"Tests passed: {summary.get('tests_passed', 0)}")
            self.print_info(f"Tests failed: {summary.get('tests_failed', 0)}")
            self.print_info(f"Tests skipped: {summary.get('tests_skipped', 0)}")
            self.print_info(f"Success rate: {summary.get('success_rate_percent', 0):.1f}%")
            self.print_info(f"Execution time: {execution_time:.1f} seconds")
            
            # Show category breakdown
            test_results = results.get('results', {})
            category_breakdown = test_results.get('category_breakdown', {})
            if category_breakdown:
                self.print_section("Category Breakdown")
                for category, stats in category_breakdown.items():
                    total = stats.get('total', 0)
                    passed = stats.get('passed', 0)
                    rate = (passed / total * 100) if total > 0 else 0
                    print(f"  {category.replace('_', ' ').title()}: {passed}/{total} ({rate:.1f}%)")
                    
            # Show device summary
            device_summary = test_results.get('device_summary', [])
            if device_summary:
                self.print_section("Device Summary")
                for device in device_summary:
                    device_id = device.get('device_id', 'Unknown')
                    tests_passed = device.get('tests_passed', 0)
                    tests_run = device.get('tests_run', 0)
                    ide_connected = device.get('ide_connected', False)
                    ide_type = device.get('ide_type', 'None')
                    
                    print(f"  Device {device_id}:")
                    print(f"    Tests: {tests_passed}/{tests_run}")
                    print(f"    IDE: {ide_type if ide_connected else 'Not connected'}")
                    print(f"    Model: {device.get('model', 'Unknown')}")
                    
            self.print_info(f"Detailed results saved to: {results.get('output_dir', 'N/A')}")
            
        else:
            self.print_status("Comprehensive testing failed", False)
            self.print_info(f"Error: {results.get('error', 'Unknown error')}")
            
        return results
        
    def demo_ide_integration(self) -> Dict[str, Any]:
        """Demonstrate IDE integration testing."""
        self.print_header("IDE INTEGRATION TESTING DEMO")
        
        if not self.integration:
            self.print_status("Android integration not available", False)
            return {'success': False}
            
        self.print_info("Testing IDE integration capabilities...")
        self.print_info("This includes Android Studio and IntelliJ IDEA detection")
        
        results = self.integration.run_ide_integrated_tests()
        
        if results['success']:
            self.print_status("IDE integration testing completed successfully")
            
            ide_integration = results.get('ide_integration', {})
            self.print_section("IDE Integration Results")
            self.print_info(f"IDEs detected: {ide_integration.get('ides_detected', 0)}")
            self.print_info(f"Devices correlated: {ide_integration.get('devices_correlated', 0)}")
            self.print_info(f"Correlation success: {ide_integration.get('correlation_success', False)}")
            
            # Show IDE details
            ide_details = ide_integration.get('ide_details', [])
            if ide_details:
                self.print_section("IDE Details")
                for ide in ide_details:
                    print(f"  {ide.get('type', 'Unknown')} v{ide.get('version', 'Unknown')}")
                    print(f"    Running: {ide.get('running', False)}")
                    print(f"    Projects: {len(ide.get('project_paths', []))}")
                    
            summary = results.get('summary', {})
            if summary:
                self.print_section("Test Summary")
                self.print_info(f"Tests run: {summary.get('total_tests_run', 0)}")
                self.print_info(f"Success rate: {summary.get('success_rate_percent', 0):.1f}%")
                
        else:
            self.print_status("IDE integration testing failed", False)
            self.print_info(f"Reason: {results.get('message', 'Unknown error')}")
            
            if 'recommendations' in results:
                self.print_section("Recommendations")
                for rec in results['recommendations']:
                    print(f"  - {rec}")
                    
        return results
        
    def demo_continuous_monitoring(self, duration: int = 60) -> None:
        """Demonstrate continuous device monitoring."""
        self.print_header("CONTINUOUS DEVICE MONITORING DEMO")
        
        if not self.integration:
            self.print_status("Android integration not available", False)
            return
            
        self.print_info(f"Monitoring Android devices for {duration} seconds...")
        self.print_info("Connect/disconnect devices to see real-time updates")
        
        start_time = time.time()
        last_device_count = 0
        last_ide_count = 0
        
        try:
            while time.time() - start_time < duration:
                results = self.integration.detect_android_devices_and_ides()
                
                device_count = len(results.get('devices', []))
                ide_count = len(results.get('ides', []))
                wireless_count = len(results.get('wireless_devices', []))
                
                # Report changes
                if device_count != last_device_count or ide_count != last_ide_count:
                    elapsed = int(time.time() - start_time)
                    print(f"[{elapsed:03d}s] Devices: {device_count} | Wireless: {wireless_count} | IDEs: {ide_count}")
                    
                    last_device_count = device_count
                    last_ide_count = ide_count
                    
                time.sleep(2)  # Check every 2 seconds
                
        except KeyboardInterrupt:
            print("\n[INFO] Monitoring stopped by user")
            
        self.print_status("Continuous monitoring completed")
        
    def demo_test_status(self) -> Dict[str, Any]:
        """Demonstrate test status and capabilities."""
        self.print_header("ANDROID TESTING CAPABILITIES STATUS")
        
        if not self.integration:
            self.print_status("Android integration not available", False)
            return {'available': False}
            
        status = self.integration.get_test_status()
        
        self.print_section("Capability Status")
        self.print_status(f"Android tests available: {status['android_tests_available']}", 
                         status['android_tests_available'])
        self.print_status(f"Connection detector available: {status['connection_detector_available']}", 
                         status['connection_detector_available'])
        self.print_status(f"ADB available: {status['adb_available']}", 
                         status['adb_available'])
        
        capabilities = status.get('capabilities', [])
        if capabilities:
            self.print_section("Available Capabilities")
            for capability in capabilities:
                print(f"  [PASS] {capability}")
        else:
            self.print_section("Available Capabilities")
            print("  [WARN] No capabilities available")
            
        return status
        
    def run_full_demo(self, include_comprehensive: bool = False) -> None:
        """Run the full demonstration."""
        self.print_header("ANDROID DEVICE TESTING - FULL DEMONSTRATION")
        print("This demo showcases comprehensive Android device testing capabilities")
        print("including device detection, IDE integration, and test execution.")
        
        # 1. Test status
        self.demo_test_status()
        
        # 2. Device detection
        detection_results = self.demo_device_detection()
        
        # 3. Quick validation
        validation_results = self.demo_quick_validation()
        
        # 4. IDE integration (if devices found)
        if detection_results.get('devices') or detection_results.get('wireless_devices'):
            self.demo_ide_integration()
        else:
            self.print_info("Skipping IDE integration demo (no devices detected)")
            
        # 5. Comprehensive testing (optional)
        if include_comprehensive:
            if detection_results.get('devices') or detection_results.get('wireless_devices'):
                self.demo_comprehensive_testing(['ui_tests', 'functional_tests', 'requirements_tests'])
            else:
                self.print_info("Skipping comprehensive testing (no devices detected)")
        else:
            self.print_info("Comprehensive testing skipped (use --comprehensive to include)")
            
        # Final summary
        self.print_header("DEMONSTRATION COMPLETED")
        self.print_info("All Android device testing capabilities have been demonstrated")
        
        if validation_results.get('success'):
            self.print_status("Android device testing is fully functional")
        else:
            self.print_status("Some Android testing features may be limited", False)
            self.print_info("Check device connections and ADB installation")


def main():
    """Main function for the Android testing demo."""
    parser = argparse.ArgumentParser(description='Android Device Testing Demonstration')
    parser.add_argument('--mode', 
                       choices=['detect', 'quick', 'comprehensive', 'ide', 'monitor', 'status', 'full'],
                       default='full',
                       help='Demo mode to run')
    parser.add_argument('--categories', nargs='+',
                       help='Test categories for comprehensive testing')
    parser.add_argument('--duration', type=int, default=60,
                       help='Duration for monitoring mode (seconds)')
    parser.add_argument('--comprehensive', action='store_true',
                       help='Include comprehensive testing in full demo')
    parser.add_argument('--json-output', action='store_true',
                       help='Output results in JSON format')
    parser.add_argument('--verbose', action='store_true',
                       help='Enable verbose output')
    
    args = parser.parse_args()
    
    # Create demo instance
    demo = AndroidTestingDemo(verbose=args.verbose)
    
    try:
        if args.mode == 'detect':
            results = demo.demo_device_detection()
        elif args.mode == 'quick':
            results = demo.demo_quick_validation()
        elif args.mode == 'comprehensive':
            results = demo.demo_comprehensive_testing(args.categories)
        elif args.mode == 'ide':
            results = demo.demo_ide_integration()
        elif args.mode == 'monitor':
            demo.demo_continuous_monitoring(args.duration)
            results = {'success': True, 'mode': 'monitoring'}
        elif args.mode == 'status':
            results = demo.demo_test_status()
        elif args.mode == 'full':
            demo.run_full_demo(include_comprehensive=args.comprehensive)
            results = {'success': True, 'mode': 'full_demo'}
        else:
            print(f"Unknown mode: {args.mode}")
            return 1
            
        if args.json_output and 'results' in locals():
            print(json.dumps(results, indent=2, default=str))
            
        return 0
        
    except KeyboardInterrupt:
        print("\n[INFO] Demo interrupted by user")
        return 0
    except Exception as e:
        print(f"[ERROR] Demo failed: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1


if __name__ == "__main__":
    exit(main())