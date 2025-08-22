# Device Connectivity Guide

## Ensuring Both Devices Are Detectable and Can Connect to Each Other

This guide provides comprehensive instructions for ensuring reliable connectivity between Android devices and PC servers in the Multi-Sensor Recording System.

## Overview: Bidirectional Device Discovery

The system supports bidirectional device discovery:

**PC → Android**: PC detects Android devices via ADB (USB/Wireless)
**Android → PC**: Android discovers PC servers via network scanning

## Prerequisites

### Network Requirements
- Both devices must be on the same network (WiFi or wired)
- Firewall must allow communication on required ports
- Network should allow inter-device communication

### Software Requirements
- **PC**: Python environment with project dependencies installed
- **Android**: App installed with proper permissions enabled
- **ADB**: Android Debug Bridge (for PC-side Android detection)

## Step-by-Step Connection Setup

### 1. Prepare the PC Environment

Start by ensuring the PC server is properly configured:

```bash
# Check system status and start server
python pc_server_helper.py --diagnose
python pc_server_helper.py --configure-firewall
python pc_server_helper.py --start
```

This will:
- Run network diagnostics
- Configure firewall rules automatically
- Start the PC server on port 9000
- Launch web dashboard on port 5000

### 2. Configure Android Device Network Settings

#### Option A: Automatic Discovery (Recommended)
The Android app automatically scans for PC servers using multiple strategies:

1. **Configured IP Testing**: Tests previously known server addresses
2. **Network Interface Detection**: Scans current network subnet
3. **Common Subnet Scanning**: Tests typical network ranges (192.168.x.x, 10.x.x.x)
4. **Cross-Subnet Discovery**: Scans across common enterprise/home networks

#### Option B: Manual Configuration
If automatic discovery fails:

1. Open Android app → Settings → Network Configuration
2. Find PC IP address: `python pc_server_helper.py --check`
3. Enter PC IP and port (default: 9000) in Android app
4. Test connection

### 3. Enable Android Device Detection (PC Side)

For PC to detect Android devices, enable USB or Wireless debugging:

#### USB Connection:
```bash
# Enable USB debugging on Android device
# Connect via USB cable
adb devices  # Should show your device
```

#### Wireless Connection:
```bash
# Enable Wireless debugging on Android (Settings → Developer options)
# Connect to same WiFi network
adb connect <android_ip>:5555
adb devices  # Should show wireless connection
```

### 4. Verify Bidirectional Connectivity

Use the built-in connectivity test tools:

```bash
# PC-side diagnostics
python pc_server_helper.py --diagnose

# Check what Android devices are detected
python -c "
from PythonApp.utils.android_connection_detector import AndroidConnectionDetector
detector = AndroidConnectionDetector()
devices = detector.detect_all_connections()
print(f'Detected {len(devices)} Android devices')
for device_id, device in devices.items():
    print(f'  - {device_id}: {device.connection_type.value}')
"
```

## Network Configuration Scenarios

### Scenario 1: Same WiFi Network (Most Common)
- **PC**: Connected to home/office WiFi
- **Android**: Connected to same WiFi network
- **IP Range**: Typically 192.168.1.x or 192.168.0.x
- **Discovery**: Automatic detection usually works

### Scenario 2: Wired PC + WiFi Android
- **PC**: Connected via Ethernet cable
- **Android**: Connected to same network's WiFi
- **Requirement**: Router must bridge wired/wireless segments
- **Discovery**: May require manual IP configuration

### Scenario 3: Enterprise Network
- **PC**: On corporate wired network
- **Android**: On corporate WiFi
- **IP Range**: Often 10.x.x.x or 172.16.x.x
- **Challenges**: Firewall restrictions, subnet isolation
- **Solution**: Network admin may need to configure firewall rules

### Scenario 4: Mobile Hotspot
- **Android**: Creating WiFi hotspot
- **PC**: Connected to Android's hotspot
- **IP Range**: Typically 192.168.43.x
- **Discovery**: Manual configuration usually required

## Troubleshooting Common Issues

### Issue 1: Android Can't Find PC Server

**Symptoms:**
- Android app shows "No PC server found"
- Connection attempts timeout

**Solutions:**
1. Verify PC server is running:
   ```bash
   python pc_server_helper.py --check
   ```

2. Check firewall settings:
   ```bash
   python pc_server_helper.py --configure-firewall
   ```

3. Test network connectivity:
   ```bash
   # On Android device, try ping
   ping <pc_ip_address>
   ```

4. Manual IP configuration in Android app

### Issue 2: PC Can't Detect Android Device

**Symptoms:**
- ADB shows "no devices/emulators found"
- Android device not appearing in PC app

**Solutions:**
1. Enable USB debugging on Android:
   - Settings → About phone → Tap "Build number" 7 times
   - Settings → Developer options → Enable "USB debugging"

2. For wireless debugging:
   - Enable "Wireless debugging" in Developer options
   - Connect to same network
   - Use `adb connect <android_ip>:5555`

3. Check ADB installation:
   ```bash
   adb version  # Should show ADB version
   ```

### Issue 3: Devices Connect But Lose Connection

**Symptoms:**
- Initial connection succeeds
- Connection drops after some time

**Solutions:**
1. Check network stability
2. Disable power saving modes on Android
3. Ensure PC doesn't sleep/hibernate
4. Check for firewall interference

### Issue 4: Cross-Network Discovery Fails

**Symptoms:**
- Devices on different subnets can't find each other

**Solutions:**
1. Manual IP configuration
2. VPN to bridge networks  
3. Network admin assistance for routing configuration

## Advanced Network Diagnostics

### PC-Side Network Analysis
```bash
# Comprehensive network diagnostics
python pc_server_helper.py --diagnose

# Check specific port accessibility
python -c "
import socket
try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('0.0.0.0', 9000))
    print('✅ Port 9000 is available')
    s.close()
except OSError as e:
    print(f'❌ Port 9000 issue: {e}')
"
```

### Android-Side Network Testing
Use Android terminal apps to test connectivity:
```bash
# Test if PC is reachable
ping -c 4 <pc_ip>

# Test if PC server port is open  
nc -zv <pc_ip> 9000

# Check Android's current IP
ip addr show wlan0
```

## Network Security Considerations

### Firewall Configuration
The system automatically configures firewall rules, but manual configuration may be needed:

**Windows:**
```cmd
netsh advfirewall firewall add rule name="Multi-Sensor PC Server" dir=in action=allow protocol=TCP localport=9000
```

**Linux (UFW):**
```bash
sudo ufw allow 9000/tcp
```

**macOS:**
System Preferences → Security & Privacy → Firewall → Add Python to exceptions

### Network Isolation
Some networks isolate devices for security:
- Guest networks often block inter-device communication
- Corporate networks may have device isolation policies
- Solution: Use same secure network or request network admin assistance

## Connection Monitoring

### Real-Time Status Monitoring
```bash
# Monitor PC server status
python pc_server_helper.py --status

# Check connected devices
curl http://localhost:5000/api/devices/status
```

### Android App Status
- Check connection status in Android app dashboard
- Monitor signal strength and connection quality
- Review connection logs in app settings

## Performance Optimisation

### Network Optimisation
- Use 5GHz WiFi when available (less congested)
- Minimize distance between devices
- Avoid interference from other devices
- Consider wired connection for PC when possible

### Connection Reliability
- Keep devices charged (low battery affects wireless performance)
- Disable aggressive power saving modes
- Close unnecessary network applications
- Monitor network quality metrics

## Getting Connection Information

### Find PC IP Address
```bash
python pc_server_helper.py --check
# Output: "Android devices can connect to: 192.168.1.100:9000"
```

### Find Android IP Address
```bash
# Via ADB
adb shell ip route | grep wlan
```

### Network Interface Information
```bash
python -c "
from PythonApp.utils.android_connection_detector import AndroidConnectionDetector
detector = AndroidConnectionDetector()
summary = detector.get_detection_summary()
print('Network Summary:')
print(f'  Total devices: {summary[\"total_devices\"]}')
print(f'  Wireless devices: {summary[\"wireless_devices\"]}')
"
```

## Quick Reference Commands

### Essential Commands
```bash
# Start complete system
python pc_server_helper.py --start

# Check connectivity status  
python pc_server_helper.py --check

# Run diagnostics
python pc_server_helper.py --diagnose

# Configure firewall
python pc_server_helper.py --configure-firewall

# Detect Android devices
python -m PythonApp.utils.android_connection_detector
```

### Emergency Troubleshooting
```bash
# Stop all servers
python pc_server_helper.py --stop

# Reset network configuration
python pc_server_helper.py --diagnose --verbose

# Check what's using port 9000
# Windows: netstat -ano | findstr :9000
# Linux/Mac: lsof -i :9000
```

## Best Practices

1. **Always start PC server before connecting Android devices**
2. **Use automatic discovery when possible**
3. **Keep both devices on same trusted network**
4. **Monitor connection quality regularly**
5. **Test connectivity before important recording sessions**
6. **Have backup connection methods ready (USB vs WiFi)**

## Support and Further Help

If connectivity issues persist:

1. Check the full system logs in `logs/pc_server.log`
2. Run diagnostics with verbose output: `python pc_server_helper.py --diagnose --verbose`
3. Review Android app logs in Settings → Debug → Export Logs
4. Test with minimal network configuration (disable VPNs, close other apps)
5. Consider network environment factors (enterprise restrictions, etc.)

The system is designed to handle most network configurations automatically, but some environments may require manual configuration or network administrator assistance.