#!/usr/bin/env python3
"""
Android Connection Detection Demo
================================

Demonstration script for detecting Android devices connected via
wireless debugging and IDE connections (IntelliJ IDEA, Android Studio).

This script shows how to:
1. Detect devices connected via wireless debugging (ADB over TCP/IP)
2. Detect IDEs with Android development capabilities
3. Correlate device connections with IDE environments
4. Monitor connection changes in real-time

Usage:
    python android_connection_demo.py [--continuous] [--json-output]
    
Examples:
    python android_connection_demo.py                  # Single detection
    python android_connection_demo.py --continuous    # Continuous monitoring
    python android_connection_demo.py --json-output   # JSON format output
"""

import argparse
import json
import logging
import time
from pathlib import Path
import sys

# Add the project root to the path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

try:
    from PythonApp.utils.android_connection_detector import (
        AndroidConnectionDetector,
        ConnectionType,
        IDEType
    )
    from PythonApp.network.android_device_manager import AndroidDeviceManager
    IMPORTS_AVAILABLE = True
except ImportError as e:
    IMPORTS_AVAILABLE = False
    IMPORT_ERROR = str(e)


def setup_logging(verbose: bool = False):
    """Setup logging configuration."""
    level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )


def print_banner():
    """Print application banner."""
    print("=" * 70)
    print("ANDROID CONNECTION DETECTION DEMO")
    print("Multi-Sensor Recording System")
    print("=" * 70)


def detect_connections_standalone():
    """Perform standalone connection detection using AndroidConnectionDetector."""
    print("\n[INFO] Performing standalone connection detection...")
    
    detector = AndroidConnectionDetector()
    
    # Detect all connections
    devices = detector.detect_all_connections()
    summary = detector.get_detection_summary()
    
    return devices, summary


def detect_connections_integrated():
    """Perform connection detection using AndroidDeviceManager integration."""
    print("\n[INFO] Performing integrated connection detection...")
    
    manager = AndroidDeviceManager(server_port=9005)  # Use test port
    
    try:
        # Check detection status
        status = manager.get_connection_detection_status()
        print(f"Connection detection available: {status['available']}")
        
        if not status['available']:
            print("[WARNING] Connection detection not available")
            return {}, {}
        
        # Detect connections
        result = manager.detect_wireless_and_ide_connections()
        
        if result.get('available'):
            return result.get('detected_devices', {}), result.get('summary', {})
        else:
            print(f"[ERROR] Detection failed: {result.get('error', 'Unknown error')}")
            return {}, {}
    
    finally:
        # Clean up
        if hasattr(manager, 'is_initialized') and manager.is_initialized:
            manager.shutdown()


def print_summary_table(summary: dict):
    """Print detection summary in table format."""
    print("\nDETECTION SUMMARY")
    print("-" * 50)
    print(f"Total devices detected:         {summary.get('total_devices', 0)}")
    print(f"USB connections:                {summary.get('usb_devices', 0)}")
    print(f"Wireless debugging connections: {summary.get('wireless_devices', 0)}")
    print(f"IDEs running with Android:      {summary.get('ides_running', 0)}")


def print_wireless_devices(summary: dict):
    """Print wireless debugging device details."""
    wireless_details = summary.get('wireless_device_details', [])
    
    if not wireless_details:
        print("\nNo wireless debugging devices detected.")
        return
    
    print(f"\nWIRELESS DEBUGGING DEVICES ({len(wireless_details)})")
    print("-" * 50)
    
    for i, device in enumerate(wireless_details, 1):
        print(f"{i}. Device ID: {device['device_id']}")
        if device.get('model'):
            print(f"   Model: {device['model']}")
        if device.get('ip_address') and device.get('port'):
            print(f"   Address: {device['ip_address']}:{device['port']}")
        if device.get('android_version'):
            print(f"   Android: {device['android_version']}")
        print()


def print_ide_connections(summary: dict):
    """Print IDE connection details."""
    ide_details = summary.get('ide_details', [])
    
    if not ide_details:
        print("\nNo IDE connections detected.")
        return
    
    print(f"\nIDE CONNECTIONS ({len(ide_details)})")
    print("-" * 50)
    
    for i, ide in enumerate(ide_details, 1):
        ide_name = ide['ide_type'].replace('_', ' ').title()
        print(f"{i}. {ide_name}")
        
        if ide.get('version') and ide['version'] != "Unknown":
            print(f"   Version: {ide['version']}")
        
        if ide.get('project_path') and ide['project_path'] != "Unknown":
            print(f"   Project: {ide['project_path']}")
        
        connected_devices = ide.get('connected_devices', [])
        if connected_devices:
            print(f"   Connected devices: {', '.join(connected_devices)}")
        else:
            print("   No devices currently connected")
        print()


def print_device_details(devices: dict):
    """Print detailed device information."""
    if not devices:
        print("\nNo devices detected.")
        return
    
    print(f"\nDETAILED DEVICE INFORMATION ({len(devices)} devices)")
    print("-" * 50)
    
    for i, (device_id, device) in enumerate(devices.items(), 1):
        print(f"{i}. Device: {device_id}")
        print(f"   Status: {device.status}")
        print(f"   Connection: {device.connection_type.value}")
        
        if hasattr(device, 'model') and device.model:
            print(f"   Model: {device.model}")
        
        if hasattr(device, 'android_version') and device.android_version:
            print(f"   Android: {device.android_version} (API {device.api_level})")
        
        if hasattr(device, 'ip_address') and device.ip_address:
            print(f"   IP Address: {device.ip_address}:{device.port}")
        
        print(f"   Developer options: {getattr(device, 'developer_options_enabled', 'Unknown')}")
        print(f"   USB debugging: {getattr(device, 'usb_debugging_enabled', 'Unknown')}")
        print(f"   Wireless debugging: {getattr(device, 'wireless_debugging_enabled', 'Unknown')}")
        
        print()


def print_json_output(devices: dict, summary: dict):
    """Print output in JSON format."""
    output = {
        'timestamp': time.time(),
        'summary': summary,
        'devices': {}
    }
    
    # Convert devices to JSON-serializable format
    for device_id, device in devices.items():
        device_info = {
            'device_id': device_id,
            'status': device.status,
            'connection_type': device.connection_type.value if hasattr(device.connection_type, 'value') else str(device.connection_type)
        }
        
        # Add optional fields if available
        optional_fields = [
            'model', 'product', 'device_name', 'android_version', 'api_level',
            'ip_address', 'port', 'wireless_debugging_enabled',
            'developer_options_enabled', 'usb_debugging_enabled', 'last_seen'
        ]
        
        for field in optional_fields:
            if hasattr(device, field):
                value = getattr(device, field)
                device_info[field] = value
        
        output['devices'][device_id] = device_info
    
    print(json.dumps(output, indent=2, default=str))


def continuous_monitoring(json_output: bool = False, interval: int = 10):
    """Perform continuous connection monitoring."""
    print(f"\n[INFO] Starting continuous monitoring (interval: {interval}s)")
    print("Press Ctrl+C to stop...")
    
    detector = AndroidConnectionDetector()
    last_device_count = -1
    last_ide_count = -1
    
    try:
        while True:
            devices = detector.detect_all_connections()
            summary = detector.get_detection_summary()
            
            # Check for changes
            current_device_count = summary.get('total_devices', 0)
            current_ide_count = summary.get('ides_running', 0)
            
            if (current_device_count != last_device_count or 
                current_ide_count != last_ide_count):
                
                print(f"\n[{time.strftime('%H:%M:%S')}] Connection change detected!")
                
                if json_output:
                    print_json_output(devices, summary)
                else:
                    print_summary_table(summary)
                    if summary.get('wireless_devices', 0) > 0:
                        print_wireless_devices(summary)
                    if summary.get('ides_running', 0) > 0:
                        print_ide_connections(summary)
                
                last_device_count = current_device_count
                last_ide_count = current_ide_count
            
            time.sleep(interval)
    
    except KeyboardInterrupt:
        print("\n[INFO] Monitoring stopped by user.")


def main():
    """Main application entry point."""
    parser = argparse.ArgumentParser(
        description="Android Connection Detection Demo",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    
    parser.add_argument(
        '--continuous', '-c',
        action='store_true',
        help='Continuous monitoring mode'
    )
    
    parser.add_argument(
        '--json-output', '-j',
        action='store_true',
        help='Output in JSON format'
    )
    
    parser.add_argument(
        '--interval', '-i',
        type=int,
        default=10,
        help='Monitoring interval in seconds (default: 10)'
    )
    
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help='Enable verbose logging'
    )
    
    parser.add_argument(
        '--integrated',
        action='store_true',
        help='Use AndroidDeviceManager integration instead of standalone detector'
    )
    
    args = parser.parse_args()
    
    # Setup logging
    setup_logging(args.verbose)
    
    # Check imports
    if not IMPORTS_AVAILABLE:
        print(f"[ERROR] Required modules not available: {IMPORT_ERROR}")
        print("Please ensure the project is properly set up.")
        return 1
    
    # Print banner
    if not args.json_output:
        print_banner()
    
    try:
        if args.continuous:
            continuous_monitoring(args.json_output, args.interval)
        else:
            # Single detection
            if args.integrated:
                devices, summary = detect_connections_integrated()
            else:
                devices, summary = detect_connections_standalone()
            
            if args.json_output:
                print_json_output(devices, summary)
            else:
                print_summary_table(summary)
                print_wireless_devices(summary)
                print_ide_connections(summary)
                print_device_details(devices)
                
                print("\n[INFO] Detection completed successfully.")
                print("Use --continuous flag for real-time monitoring.")
                print("Use --json-output flag for machine-readable output.")
    
    except Exception as e:
        if args.json_output:
            error_output = {
                'error': str(e),
                'timestamp': time.time()
            }
            print(json.dumps(error_output, indent=2))
        else:
            print(f"\n[ERROR] Detection failed: {e}")
            if args.verbose:
                import traceback
                traceback.print_exc()
        return 1
    
    return 0


if __name__ == "__main__":
    sys.exit(main())