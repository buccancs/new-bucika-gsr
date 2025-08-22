"""
Android Connection Detector
==========================

Detects Android devices connected via wireless debugging and IDE connections
(IntelliJ IDEA or Android Studio). Provides comprehensive device connectivity
information for testing and development workflows.
"""

import json
import logging
import platform
import re
import subprocess
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple
from enum import Enum


class ConnectionType(Enum):
    """Types of Android device connections."""
    USB = "usb"
    WIRELESS_ADB = "wireless_adb"
    IDE_CONNECTED = "ide_connected"
    UNKNOWN = "unknown"


class IDEType(Enum):
    """Types of supported IDEs."""
    ANDROID_STUDIO = "android_studio"
    INTELLIJ_IDEA = "intellij_idea"
    VSCODE = "vscode"
    UNKNOWN = "unknown"


@dataclass
class AndroidDevice:
    """Represents a detected Android device."""
    device_id: str
    status: str  # device, offline, unauthorized, etc.
    connection_type: ConnectionType
    transport_id: Optional[str] = None
    model: Optional[str] = None
    product: Optional[str] = None
    device_name: Optional[str] = None
    android_version: Optional[str] = None
    api_level: Optional[int] = None
    ip_address: Optional[str] = None
    port: Optional[int] = None
    wireless_debugging_enabled: bool = False
    developer_options_enabled: bool = False
    usb_debugging_enabled: bool = False
    last_seen: float = field(default_factory=time.time)


@dataclass
class IDEConnection:
    """Represents an IDE connection to Android devices."""
    ide_type: IDEType
    ide_version: Optional[str] = None
    project_path: Optional[str] = None
    connected_devices: Set[str] = field(default_factory=set)
    process_id: Optional[int] = None
    last_activity: float = field(default_factory=time.time)


class AndroidConnectionDetector:
    """
    Detects Android devices connected via various methods including
    wireless debugging and IDE connections.
    """
    
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.detected_devices: Dict[str, AndroidDevice] = {}
        self.ide_connections: Dict[str, IDEConnection] = {}
        self.adb_path = self._find_adb_executable()
        
    def _find_adb_executable(self) -> Optional[str]:
        """Find ADB executable in system PATH or common locations."""
        # Try system PATH first
        try:
            result = subprocess.run(['adb', 'version'], 
                                    capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                return 'adb'
        except (subprocess.TimeoutExpired, subprocess.SubprocessError, FileNotFoundError):
            pass
        
        # Try common installation paths
        common_paths = []
        
        if platform.system() == "Windows":
            common_paths.extend([
                Path.home() / "AppData" / "Local" / "Android" / "Sdk" / "platform-tools" / "adb.exe",
                Path("C:") / "Android" / "platform-tools" / "adb.exe",
                Path("C:") / "Users" / "Public" / "platform-tools" / "adb.exe"
            ])
        elif platform.system() == "Darwin":  # macOS
            common_paths.extend([
                Path.home() / "Library" / "Android" / "sdk" / "platform-tools" / "adb",
                Path("/usr/local/bin/adb"),
                Path("/opt/homebrew/bin/adb")
            ])
        else:  # Linux
            common_paths.extend([
                Path.home() / "Android" / "Sdk" / "platform-tools" / "adb",
                Path("/usr/bin/adb"),
                Path("/usr/local/bin/adb"),
                Path("/opt/android-sdk/platform-tools/adb")
            ])
        
        for path in common_paths:
            if path.exists() and path.is_file():
                try:
                    result = subprocess.run([str(path), 'version'], 
                                            capture_output=True, text=True, timeout=5)
                    if result.returncode == 0:
                        self.logger.info(f"Found ADB at: {path}")
                        return str(path)
                except (subprocess.TimeoutExpired, subprocess.SubprocessError):
                    continue
        
        self.logger.warning("ADB executable not found in PATH or common locations")
        return None
    
    def detect_all_connections(self) -> Dict[str, AndroidDevice]:
        """
        Detect all Android device connections including USB, wireless ADB,
        and IDE connections.
        """
        self.detected_devices.clear()
        
        # Detect ADB connections
        if self.adb_path:
            self._detect_adb_devices()
            self._enhance_device_info()
        
        # Detect IDE connections
        self._detect_ide_connections()
        
        # Cross-reference IDE connections with ADB devices
        self._correlate_ide_adb_connections()
        
        return self.detected_devices.copy()
    
    def _detect_adb_devices(self) -> None:
        """Detect devices via ADB."""
        try:
            # Get device list
            result = subprocess.run([self.adb_path, 'devices', '-l'], 
                                    capture_output=True, text=True, timeout=10)
            
            if result.returncode != 0:
                self.logger.error(f"ADB devices command failed: {result.stderr}")
                return
            
            lines = result.stdout.strip().split('\n')[1:]  # Skip header
            
            for line in lines:
                line = line.strip()
                if not line:
                    continue
                
                parts = line.split()
                if len(parts) < 2:
                    continue
                
                device_id = parts[0]
                status = parts[1]
                
                # Determine connection type based on device ID
                connection_type = self._determine_connection_type(device_id)
                
                # Extract additional info from device list output
                device_info = self._parse_device_list_info(line)
                
                device = AndroidDevice(
                    device_id=device_id,
                    status=status,
                    connection_type=connection_type,
                    **device_info
                )
                
                # Extract IP and port for wireless connections
                if connection_type == ConnectionType.WIRELESS_ADB:
                    ip_port = self._parse_wireless_connection(device_id)
                    if ip_port:
                        device.ip_address, device.port = ip_port
                
                self.detected_devices[device_id] = device
                
                self.logger.info(f"Detected device: {device_id} ({status}, {connection_type.value})")
        
        except subprocess.TimeoutExpired:
            self.logger.error("ADB devices command timed out")
        except Exception as e:
            self.logger.error(f"Error detecting ADB devices: {e}")
    
    def _determine_connection_type(self, device_id: str) -> ConnectionType:
        """Determine connection type based on device ID format."""
        if ':' in device_id and device_id.count(':') == 1:
            # Format like "192.168.1.100:5555" indicates wireless ADB
            try:
                ip_part, port_part = device_id.split(':')
                # Basic IP validation
                ip_parts = ip_part.split('.')
                if len(ip_parts) == 4 and all(0 <= int(part) <= 255 for part in ip_parts):
                    return ConnectionType.WIRELESS_ADB
            except ValueError:
                pass
        
        # Check for emulator pattern
        if device_id.startswith('emulator-'):
            return ConnectionType.USB  # Emulator treated as USB for simplicity
        
        # Default to USB for standard device IDs
        return ConnectionType.USB
    
    def _parse_device_list_info(self, line: str) -> Dict:
        """Parse additional device information from ADB devices -l output."""
        info = {}
        
        # Look for model, product, device, transport_id
        patterns = {
            'model': r'model:(\S+)',
            'product': r'product:(\S+)', 
            'device_name': r'device:(\S+)',
            'transport_id': r'transport_id:(\S+)'
        }
        
        for key, pattern in patterns.items():
            match = re.search(pattern, line)
            if match:
                info[key] = match.group(1)
        
        return info
    
    def _parse_wireless_connection(self, device_id: str) -> Optional[Tuple[str, int]]:
        """Parse IP address and port from wireless device ID."""
        try:
            if ':' in device_id:
                ip, port_str = device_id.split(':')
                return ip, int(port_str)
        except ValueError:
            pass
        return None
    
    def _enhance_device_info(self) -> None:
        """Enhance device information with additional properties."""
        for device_id, device in self.detected_devices.items():
            if device.status != 'device':
                continue  # Skip offline/unauthorized devices
            
            try:
                # Get Android version and API level
                android_info = self._get_android_version(device_id)
                if android_info:
                    device.android_version, device.api_level = android_info
                
                # Check developer options and debugging status
                device.developer_options_enabled = self._check_developer_options(device_id)
                device.usb_debugging_enabled = self._check_usb_debugging(device_id)
                device.wireless_debugging_enabled = self._check_wireless_debugging(device_id)
                
            except Exception as e:
                self.logger.warning(f"Failed to enhance info for device {device_id}: {e}")
    
    def _get_android_version(self, device_id: str) -> Optional[Tuple[str, int]]:
        """Get Android version and API level for device."""
        try:
            # Get Android version
            version_result = subprocess.run(
                [self.adb_path, '-s', device_id, 'shell', 'getprop', 'ro.build.version.release'],
                capture_output=True, text=True, timeout=5
            )
            
            # Get API level
            api_result = subprocess.run(
                [self.adb_path, '-s', device_id, 'shell', 'getprop', 'ro.build.version.sdk'],
                capture_output=True, text=True, timeout=5
            )
            
            if version_result.returncode == 0 and api_result.returncode == 0:
                version = version_result.stdout.strip()
                api_level = int(api_result.stdout.strip())
                return version, api_level
                
        except (subprocess.TimeoutExpired, ValueError, subprocess.SubprocessError):
            pass
        
        return None
    
    def _check_developer_options(self, device_id: str) -> bool:
        """Check if developer options are enabled."""
        try:
            result = subprocess.run(
                [self.adb_path, '-s', device_id, 'shell', 'settings', 'get', 'global', 'development_settings_enabled'],
                capture_output=True, text=True, timeout=5
            )
            return result.returncode == 0 and result.stdout.strip() == '1'
        except (subprocess.TimeoutExpired, subprocess.SubprocessError):
            return False
    
    def _check_usb_debugging(self, device_id: str) -> bool:
        """Check if USB debugging is enabled."""
        try:
            result = subprocess.run(
                [self.adb_path, '-s', device_id, 'shell', 'settings', 'get', 'global', 'adb_enabled'],
                capture_output=True, text=True, timeout=5
            )
            return result.returncode == 0 and result.stdout.strip() == '1'
        except (subprocess.TimeoutExpired, subprocess.SubprocessError):
            return False
    
    def _check_wireless_debugging(self, device_id: str) -> bool:
        """Check if wireless debugging is enabled."""
        try:
            # Check for wireless debugging feature (Android 11+)
            result = subprocess.run(
                [self.adb_path, '-s', device_id, 'shell', 'settings', 'get', 'global', 'adb_wifi_enabled'],
                capture_output=True, text=True, timeout=5
            )
            
            if result.returncode == 0 and result.stdout.strip() == '1':
                return True
            
            # Alternative check - if connected via IP, likely wireless debugging
            return self.detected_devices[device_id].connection_type == ConnectionType.WIRELESS_ADB
            
        except (subprocess.TimeoutExpired, subprocess.SubprocessError):
            return False
    
    def _detect_ide_connections(self) -> None:
        """Detect IDE connections to Android devices."""
        self.ide_connections.clear()
        
        # Check for Android Studio
        self._detect_android_studio()
        
        # Check for IntelliJ IDEA
        self._detect_intellij_idea()
        
        # Check for VS Code with Android extensions
        self._detect_vscode_android()
    
    def _detect_android_studio(self) -> None:
        """Detect Android Studio connections."""
        try:
            if platform.system() == "Windows":
                cmd = ['wmic', 'process', 'where', 'name="studio64.exe"', 'get', 'ProcessId,CommandLine', '/format:csv']
            elif platform.system() == "Darwin":  # macOS
                cmd = ['ps', 'aux']
            else:  # Linux
                cmd = ['ps', 'aux']
            
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
            
            if result.returncode == 0:
                studio_processes = self._parse_ide_processes(result.stdout, IDEType.ANDROID_STUDIO)
                for process in studio_processes:
                    self.ide_connections[f"android_studio_{process.process_id}"] = process
                    
        except (subprocess.TimeoutExpired, subprocess.SubprocessError) as e:
            self.logger.warning(f"Failed to detect Android Studio: {e}")
    
    def _detect_intellij_idea(self) -> None:
        """Detect IntelliJ IDEA connections."""
        try:
            if platform.system() == "Windows":
                cmd = ['wmic', 'process', 'where', 'name="idea64.exe"', 'get', 'ProcessId,CommandLine', '/format:csv']
            elif platform.system() == "Darwin":  # macOS
                cmd = ['ps', 'aux']
            else:  # Linux
                cmd = ['ps', 'aux']
            
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
            
            if result.returncode == 0:
                idea_processes = self._parse_ide_processes(result.stdout, IDEType.INTELLIJ_IDEA)
                for process in idea_processes:
                    self.ide_connections[f"intellij_idea_{process.process_id}"] = process
                    
        except (subprocess.TimeoutExpired, subprocess.SubprocessError) as e:
            self.logger.warning(f"Failed to detect IntelliJ IDEA: {e}")
    
    def _detect_vscode_android(self) -> None:
        """Detect VS Code with Android extensions."""
        try:
            if platform.system() == "Windows":
                cmd = ['wmic', 'process', 'where', 'name="Code.exe"', 'get', 'ProcessId,CommandLine', '/format:csv']
            elif platform.system() == "Darwin":  # macOS
                cmd = ['ps', 'aux']
            else:  # Linux
                cmd = ['ps', 'aux']
            
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
            
            if result.returncode == 0:
                vscode_processes = self._parse_ide_processes(result.stdout, IDEType.VSCODE)
                for process in vscode_processes:
                    # Check if Android extensions are active
                    if self._check_vscode_android_extensions(process.process_id):
                        self.ide_connections[f"vscode_{process.process_id}"] = process
                        
        except (subprocess.TimeoutExpired, subprocess.SubprocessError) as e:
            self.logger.warning(f"Failed to detect VS Code: {e}")
    
    def _parse_ide_processes(self, process_output: str, ide_type: IDEType) -> List[IDEConnection]:
        """Parse IDE process information from system output."""
        processes = []
        
        try:
            lines = process_output.strip().split('\n')
            
            for line in lines:
                if not line.strip():
                    continue
                
                # Extract process ID and project path based on IDE type and OS
                process_info = self._extract_process_info(line, ide_type)
                if process_info:
                    pid, project_path, version = process_info
                    
                    connection = IDEConnection(
                        ide_type=ide_type,
                        ide_version=version,
                        project_path=project_path,
                        process_id=pid
                    )
                    processes.append(connection)
                    
        except Exception as e:
            self.logger.warning(f"Failed to parse IDE processes: {e}")
        
        return processes
    
    def _extract_process_info(self, line: str, ide_type: IDEType) -> Optional[Tuple[int, str, str]]:
        """Extract process ID, project path, and version from process line."""
        try:
            # Simplified extraction - would need platform-specific parsing
            if platform.system() == "Windows" and "," in line:
                parts = line.split(',')
                if len(parts) >= 3:
                    pid = int(parts[1].strip()) if parts[1].strip().isdigit() else None
                    command_line = parts[2].strip()
                    
                    # Extract project path from command line
                    project_path = self._extract_project_path(command_line)
                    version = "Unknown"
                    
                    if pid:
                        return pid, project_path, version
            else:
                # Unix-like systems
                parts = line.split()
                if len(parts) >= 2:
                    pid = int(parts[1]) if parts[1].isdigit() else None
                    command_line = ' '.join(parts[10:]) if len(parts) > 10 else ""
                    
                    # Check if this is the IDE we're looking for
                    ide_names = {
                        IDEType.ANDROID_STUDIO: ['studio', 'android-studio'],
                        IDEType.INTELLIJ_IDEA: ['idea', 'intellij'],
                        IDEType.VSCODE: ['code', 'vscode']
                    }
                    
                    if any(name in command_line.lower() for name in ide_names.get(ide_type, [])):
                        project_path = self._extract_project_path(command_line)
                        version = "Unknown"
                        
                        if pid:
                            return pid, project_path, version
                            
        except (ValueError, IndexError):
            pass
        
        return None
    
    def _extract_project_path(self, command_line: str) -> str:
        """Extract project path from IDE command line."""
        # Look for common project path patterns
        patterns = [
            r'--path[= ]([^\s]+)',
            r'--project[= ]([^\s]+)',
            r'-Didea.project.path=([^\s]+)',
            r'([/\\][\w\s.-]+[/\\][\w\s.-]+)'  # Generic path pattern
        ]
        
        for pattern in patterns:
            match = re.search(pattern, command_line)
            if match:
                return match.group(1)
        
        return "Unknown"
    
    def _check_vscode_android_extensions(self, process_id: int) -> bool:
        """Check if VS Code has Android-related extensions active."""
        # This is a simplified check - in practice, would need to examine
        # VS Code's extension state or workspace configuration
        return True  # Assume true for now
    
    def _correlate_ide_adb_connections(self) -> None:
        """Correlate IDE connections with ADB device connections."""
        for ide_id, ide_conn in self.ide_connections.items():
            # Check for devices that might be connected through this IDE
            for device_id, device in self.detected_devices.items():
                if device.status == 'device':
                    # If IDE is running and device is connected, assume correlation
                    ide_conn.connected_devices.add(device_id)
                    
                    # Update device to indicate IDE connection
                    if device.connection_type == ConnectionType.USB:
                        # Don't override wireless detection, but note IDE connection
                        pass
    
    def get_wireless_debugging_devices(self) -> List[AndroidDevice]:
        """Get devices connected via wireless debugging."""
        return [device for device in self.detected_devices.values() 
                if device.connection_type == ConnectionType.WIRELESS_ADB or 
                device.wireless_debugging_enabled]
    
    def get_ide_connected_devices(self) -> Dict[IDEType, List[str]]:
        """Get devices connected through IDEs."""
        ide_devices = {}
        
        for ide_conn in self.ide_connections.values():
            if ide_conn.ide_type not in ide_devices:
                ide_devices[ide_conn.ide_type] = []
            ide_devices[ide_conn.ide_type].extend(ide_conn.connected_devices)
        
        return ide_devices
    
    def is_device_wireless_connected(self, device_id: str) -> bool:
        """Check if specific device is connected via wireless debugging."""
        device = self.detected_devices.get(device_id)
        return device and (device.connection_type == ConnectionType.WIRELESS_ADB or
                          device.wireless_debugging_enabled)
    
    def is_device_ide_connected(self, device_id: str) -> List[IDEType]:
        """Check if specific device is connected through IDEs."""
        connected_ides = []
        
        for ide_conn in self.ide_connections.values():
            if device_id in ide_conn.connected_devices:
                connected_ides.append(ide_conn.ide_type)
        
        return connected_ides
    
    def get_detection_summary(self) -> Dict:
        """Get comprehensive detection summary."""
        wireless_devices = self.get_wireless_debugging_devices()
        ide_devices = self.get_ide_connected_devices()
        
        return {
            'total_devices': len(self.detected_devices),
            'usb_devices': len([d for d in self.detected_devices.values() 
                               if d.connection_type == ConnectionType.USB]),
            'wireless_devices': len(wireless_devices),
            'wireless_device_details': [
                {
                    'device_id': d.device_id,
                    'ip_address': d.ip_address,
                    'port': d.port,
                    'model': d.model,
                    'android_version': d.android_version
                }
                for d in wireless_devices
            ],
            'ide_connections': {
                ide_type.value: devices for ide_type, devices in ide_devices.items()
            },
            'ides_running': len(self.ide_connections),
            'ide_details': [
                {
                    'ide_type': conn.ide_type.value,
                    'version': conn.ide_version,
                    'project_path': conn.project_path,
                    'connected_devices': list(conn.connected_devices)
                }
                for conn in self.ide_connections.values()
            ]
        }


def main():
    """Example usage of AndroidConnectionDetector."""
    logging.basicConfig(level=logging.INFO,
                       format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    
    detector = AndroidConnectionDetector()
    
    print("Detecting Android device connections...")
    devices = detector.detect_all_connections()
    
    summary = detector.get_detection_summary()
    
    print("\n" + "="*60)
    print("ANDROID CONNECTION DETECTION SUMMARY")
    print("="*60)
    
    print(f"Total devices detected: {summary['total_devices']}")
    print(f"USB connections: {summary['usb_devices']}")
    print(f"Wireless debugging connections: {summary['wireless_devices']}")
    print(f"IDEs running with Android support: {summary['ides_running']}")
    
    if summary['wireless_device_details']:
        print("\nWireless Debugging Devices:")
        for device in summary['wireless_device_details']:
            print(f"  - {device['device_id']} ({device.get('model', 'Unknown')})")
            if device['ip_address']:
                print(f"    IP: {device['ip_address']}:{device['port']}")
            if device['android_version']:
                print(f"    Android: {device['android_version']}")
    
    if summary['ide_details']:
        print("\nIDE Connections:")
        for ide in summary['ide_details']:
            print(f"  - {ide['ide_type'].replace('_', ' ').title()}")
            if ide['project_path'] != "Unknown":
                print(f"    Project: {ide['project_path']}")
            if ide['connected_devices']:
                print(f"    Connected devices: {', '.join(ide['connected_devices'])}")
    
    print("\nDetailed Device Information:")
    for device_id, device in devices.items():
        print(f"\nDevice: {device_id}")
        print(f"  Status: {device.status}")
        print(f"  Connection: {device.connection_type.value}")
        if device.model:
            print(f"  Model: {device.model}")
        if device.android_version:
            print(f"  Android: {device.android_version} (API {device.api_level})")
        print(f"  Developer options: {device.developer_options_enabled}")
        print(f"  USB debugging: {device.usb_debugging_enabled}")
        print(f"  Wireless debugging: {device.wireless_debugging_enabled}")
        
        ide_connections = detector.is_device_ide_connected(device_id)
        if ide_connections:
            print(f"  IDE connections: {[ide.value for ide in ide_connections]}")


if __name__ == "__main__":
    main()