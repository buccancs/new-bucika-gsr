# Network Setup Guide for Multi-Sensor Recording System

## Overview

This guide helps resolve network connection issues between Android devices and the PC server, specifically addressing errors like:
- `Connection failed: failed to connect to /192.168.0.100 (port 9000)`
- `UVC camera not initialised - cannot start thermal preview`
- `Failed to connect to PC at 192.168.0.100:9000`

## Root Cause

The Android app expects a PC server to be running on port 9000 to handle device communication, file transfers, and thermal camera coordination. If this server is not running, all network-dependent features will fail.

## Quick Fix

### 1. Start the PC Server

```bash
# Option 1: Use the helper script (recommended)
python pc_server_helper.py --start

# Option 2: Direct launch
python PythonApp/web_launcher.py --android-port 9000 --host 0.0.0.0
```

### 2. Check Server Status

```bash
python pc_server_helper.py --check
```

### 3. Diagnose Network Issues

```bash
python pc_server_helper.py --diagnose
```

## Detailed Setup Instructions

### Prerequisites

1. **Python Environment**: Ensure Python 3.8+ is installed with required packages
2. **Network Access**: PC and Android device must be on the same network or have network connectivity
3. **Firewall**: Port 9000 must be accessible (Windows Firewall may need configuration)

### Step 1: Install Dependencies

```bash
# Install required packages
pip install flask flask-socketio eventlet

# Or install from requirements
pip install -r PythonApp/requirements.txt
```

### Step 2: Configure Network

#### Find Your PC's IP Address

```bash
# Windows
ipconfig

# Linux/Mac
ifconfig
```

Look for your local network IP (typically 192.168.x.x or 10.x.x.x).

#### Configure Android App

1. Open the Android app
2. Go to Settings/Network Configuration
3. Enter your PC's IP address (e.g., 192.168.1.100)
4. Ensure port is set to 9000
5. Test the connection

### Step 3: Start the PC Server

#### Method 1: Helper Script (Recommended)

```bash
# Start with default settings
python pc_server_helper.py --start

# Start with custom ports
python pc_server_helper.py --start --android-port 9000 --web-port 5000

# Start with diagnostics
python pc_server_helper.py --diagnose
python pc_server_helper.py --start
```

#### Method 2: Direct Launch

```bash
# Basic startup
python PythonApp/web_launcher.py

# Custom configuration
python PythonApp/web_launcher.py \
    --android-port 9000 \
    --port 5000 \
    --host 0.0.0.0 \
    --debug
```

#### Method 3: GUI Application

```bash
# Launch GUI with embedded server
python PythonApp/main.py
```

### Step 4: Verify Connection

1. **Check PC Server Status**:
   ```bash
   python pc_server_helper.py --check
   ```

2. **Test from Android Device**:
   - Open the Android app
   - Go to Devices tab
   - Try to connect to thermal camera
   - Check for successful PC connection messages

3. **Monitor Logs**:
   - PC server logs should show Android device connections
   - Android logs should show successful connection to PC

## Network Architecture

```
Android Device (192.168.1.167)
        |
        | TCP Connection
        | Port 9000
        |
   PC Server (192.168.0.100:9000)
        |
   ┌────┴────┐
   │ Services│
   ├─────────┤
   │ Android │ ← Device Management
   │ Manager │
   ├─────────┤
   │ Web     │ ← Dashboard (Port 5000)
   │ UI      │
   ├─────────┤
   │ File    │ ← File Transfer
   │ Transfer│
   ├─────────┤
   │ Thermal │ ← Camera Control
   │ Support │
   └─────────┘
```

## Troubleshooting

### Common Issues

#### 1. Port 9000 Already in Use

```bash
# Check what's using the port
netstat -ano | findstr :9000

# Kill the process (replace PID with actual process ID)
taskkill /PID 1234 /F

# Or use the helper script
python pc_server_helper.py --fix-network
```

#### 2. Firewall Blocking Connection

**Windows Defender Firewall**:
1. Open Windows Defender Firewall
2. Click "Allow an app or feature through Windows Defender Firewall"
3. Add Python.exe to the allowed apps
4. Ensure both "Private" and "Public" are checked

**Command Line (Run as Administrator)**:
```cmd
netsh advfirewall firewall add rule name="Python MultiSensor" dir=in action=allow protocol=TCP localport=9000
```

#### 3. Different Network Subnets

If your PC is on 192.168.0.x and Android device on 192.168.1.x:

1. **Check Router Configuration**: Ensure inter-VLAN routing is enabled
2. **Use Bridge Mode**: Configure router to use bridge mode
3. **Manual IP Configuration**: Set both devices to the same subnet

#### 4. IP Address Changes

Dynamic IP addresses can change. Solutions:

1. **Use Static IP**: Configure your PC with a static IP address
2. **mDNS/Bonjour**: The server supports device discovery
3. **Manual Configuration**: Update Android app with new IP when it changes

### Advanced Configuration

#### Custom Port Configuration

If port 9000 is not available:

```bash
# Start PC server on custom port
python pc_server_helper.py --start --android-port 9001

# Configure Android app to use port 9001
```

#### Security Configuration

The PC server includes TLS support:

```bash
# Start with enhanced security
python PythonApp/web_launcher.py --android-port 9000 --enable-tls
```

#### Multi-Device Support

The server supports multiple Android devices simultaneously:

```bash
# Check connected devices
python pc_server_helper.py --check

# Monitor connections in real-time
python PythonApp/web_launcher.py --debug
```

## Validation Scripts

### Test Network Connectivity

```python
# test_connection.py
import socket

def test_connection(host, port):
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(5)
            result = sock.connect_ex((host, port))
            return result == 0
    except:
        return False

# Test from Android device's perspective
pc_ip = "192.168.0.100"  # Replace with your PC's IP
if test_connection(pc_ip, 9000):
    print(f"✅ PC server is reachable at {pc_ip}:9000")
else:
    print(f"❌ Cannot reach PC server at {pc_ip}:9000")
```

## Best Practices

1. **Start PC Server First**: Always start the PC server before launching the Android app
2. **Use Static IP**: Configure your PC with a static IP for consistent connections
3. **Monitor Logs**: Keep an eye on both PC and Android logs during setup
4. **Test Incrementally**: Test basic connection before attempting thermal camera features
5. **Document Configuration**: Keep note of working IP addresses and ports

## Support

If you continue experiencing issues:

1. **Collect Logs**:
   ```bash
   python pc_server_helper.py --diagnose > network_diagnostics.txt
   ```

2. **Check Documentation**: Review the main README and technical documentation

3. **Contact Support**: Include network diagnostics and both PC and Android logs

## Related Files

- `pc_server_helper.py` - Helper script for server management
- `PythonApp/web_launcher.py` - Main PC server launcher
- `PythonApp/network/android_device_manager.py` - Android device communication
- `AndroidApp/src/main/java/com/multisensor/recording/network/` - Android networking code
