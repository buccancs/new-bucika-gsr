#!/usr/bin/env python3
"""
Quick Device Connection Setup
============================

Simple script to quickly set up and test device connectivity for the 
Multi-Sensor Recording System.

This script provides a guided setup process to ensure both Android devices
and PC servers can detect and connect to each other.

Usage:
    python quick_setup_devices.py
"""

import os
import sys
import time
from pathlib import Path

# Add project root to Python path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

def print_header():
    print("\n" + "="*60)
    print("🚀 MULTI-SENSOR RECORDING SYSTEM")
    print("   Quick Device Connection Setup")
    print("="*60)

def print_step(step_num, title):
    print(f"\n📋 Step {step_num}: {title}")
    print("-" * (len(title) + 15))

def wait_for_user(prompt="Press Enter to continue..."):
    input(f"\n{prompt}")

def run_command(command, description):
    print(f"  🔧 Running: {description}")
    print(f"     Command: {command}")
    
    result = os.system(command)
    if result == 0:
        print("  ✅ Success!")
        return True
    else:
        print("  ❌ Failed!")
        return False

def main():
    print_header()
    
    print("\nThis setup will help you:")
    print("• Start the PC server")
    print("• Test network connectivity") 
    print("• Check Android device detection")
    print("• Verify bidirectional communication")
    print("• Provide troubleshooting guidance")
    
    wait_for_user()
    
    # Step 1: Check system status
    print_step(1, "System Status Check")
    print("  Checking if PC server components are available...")
    
    try:
        from pc_server_helper import PCServerHelper
        from test_device_connectivity import DeviceConnectivityTester
        print("  ✅ All required modules are available")
    except ImportError as e:
        print(f"  ❌ Missing modules: {e}")
        print("\n  Please ensure you have installed all dependencies:")
        print("    pip install -r requirements.txt")
        sys.exit(1)
    
    wait_for_user()
    
    # Step 2: Network diagnostics
    print_step(2, "Network Diagnostics")
    print("  Running network diagnostics to check your setup...")
    
    success = run_command(
        "python pc_server_helper.py --diagnose",
        "Network diagnostics"
    )
    
    if not success:
        print("\n  ⚠️  Network diagnostics failed. Continuing anyway...")
    
    wait_for_user()
    
    # Step 3: Configure firewall
    print_step(3, "Firewall Configuration")
    print("  Configuring firewall to allow device connections...")
    
    success = run_command(
        "python pc_server_helper.py --configure-firewall",
        "Firewall configuration"
    )
    
    if not success:
        print("\n  ⚠️  Firewall configuration failed.")
        print("      You may need to manually configure your firewall.")
        print("      Allow incoming connections on port 9000.")
    
    wait_for_user()
    
    # Step 4: Start PC server
    print_step(4, "Start PC Server")
    print("  Starting the PC server for Android device connections...")
    
    success = run_command(
        "python pc_server_helper.py --start",
        "PC server startup"
    )
    
    if success:
        print("\n  🎉 PC server started successfully!")
        print("  📱 Android devices can now connect")
        print("  🌐 Web dashboard available at: http://localhost:5000")
    else:
        print("\n  ❌ Failed to start PC server")
        print("     Check the logs for details")
        sys.exit(1)
    
    wait_for_user("PC server is starting... Press Enter when ready to test connectivity")
    
    # Step 5: Test connectivity
    print_step(5, "Connectivity Test")
    print("  Running comprehensive connectivity test...")
    
    success = run_command(
        "python test_device_connectivity.py --full-test",
        "Device connectivity test"
    )
    
    if not success:
        print("\n  ⚠️  Connectivity test had issues.")
        print("      Check the output above for recommendations.")
    
    wait_for_user()
    
    # Step 6: Android device detection
    print_step(6, "Android Device Detection")
    print("  Checking for connected Android devices...")
    
    success = run_command(
        "python -c \"from PythonApp.utils.android_connection_detector import AndroidConnectionDetector; d=AndroidConnectionDetector(); devs=d.detect_all_connections(); print(f'Found {len(devs)} Android devices'); [print(f'  - {id}: {dev.connection_type.value}') for id, dev in devs.items()]\"",
        "Android device detection"
    )
    
    wait_for_user()
    
    # Step 7: Final instructions
    print_step(7, "Setup Complete!")
    print("\n🎉 Device setup is complete!")
    print("\n📱 To connect your Android device:")
    print("   1. Ensure Android device is on the same WiFi network")
    print("   2. Open the Multi-Sensor Recording app")
    print("   3. Go to Settings → Network Configuration")
    print("   4. The app should automatically find your PC")
    print("   5. If not, manually enter your PC's IP address")
    
    # Get PC IP for user
    try:
        helper = PCServerHelper()
        local_ip = helper._get_local_ip()
        print(f"\n💡 Your PC IP address: {local_ip}:9000")
    except:
        print("\n💡 To find your PC IP: python pc_server_helper.py --check")
    
    print("\n🌐 Web Dashboard:")
    print("   http://localhost:5000")
    
    print("\n🔧 Useful Commands:")
    print("   Check server status:     python pc_server_helper.py --check")
    print("   Stop server:             python pc_server_helper.py --stop")
    print("   Test connectivity:       python test_device_connectivity.py --full-test")
    print("   Network diagnostics:     python pc_server_helper.py --diagnose")
    
    print("\n📖 For detailed help, see: DEVICE_CONNECTIVITY_GUIDE.md")
    
    print("\n✅ Setup complete! Your system is ready for multi-sensor recording.")

if __name__ == "__main__":
    main()