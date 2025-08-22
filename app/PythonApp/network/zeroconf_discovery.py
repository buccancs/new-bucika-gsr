"""
Zeroconf/mDNS device discovery for the multi-sensor recording system.

This module provides automatic discovery of devices on the local network
using Zeroconf/Bonjour protocol, enabling seamless device connections
without manual IP configuration.
"""

import logging
import threading
import time
import socket
from typing import Dict, List, Optional, Callable, Any
from dataclasses import dataclass
from zeroconf import Zeroconf, ServiceBrowser, ServiceListener, ServiceInfo
import ipaddress


@dataclass
class DiscoveredDevice:
    """Information about a discovered device."""
    name: str
    service_type: str
    hostname: str
    ip_address: str
    port: int
    properties: Dict[str, str]
    discovery_time: float
    last_seen: float


class DeviceDiscoveryListener(ServiceListener):
    """Zeroconf service listener for device discovery."""
    
    def __init__(self, discovery_manager: 'ZeroconfDiscovery'):
        self.discovery_manager = discovery_manager
        self.logger = discovery_manager.logger
    
    def add_service(self, zeroconf: Zeroconf, service_type: str, name: str) -> None:
        """Called when a new service is discovered."""
        self.logger.debug(f"Service discovered: {name} ({service_type})")
        
        # Get service info
        info = zeroconf.get_service_info(service_type, name)
        if info:
            self.discovery_manager._process_service_info(info, service_type, "added")
    
    def remove_service(self, zeroconf: Zeroconf, service_type: str, name: str) -> None:
        """Called when a service is removed."""
        self.logger.debug(f"Service removed: {name} ({service_type})")
        self.discovery_manager._handle_service_removed(name, service_type)
    
    def update_service(self, zeroconf: Zeroconf, service_type: str, name: str) -> None:
        """Called when a service is updated."""
        self.logger.debug(f"Service updated: {name} ({service_type})")
        
        # Get updated service info
        info = zeroconf.get_service_info(service_type, name)
        if info:
            self.discovery_manager._process_service_info(info, service_type, "updated")


class ZeroconfDiscovery:
    """Manages Zeroconf device discovery and service announcement."""
    
    # Standard service types for the multi-sensor system
    SERVICE_TYPES = [
        "_multisensor._tcp.local.",
        "_bucika-gsr._tcp.local.",
        "_android-device._tcp.local.",
        "_pc-controller._tcp.local."
    ]
    
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.zeroconf: Optional[Zeroconf] = None
        self.browsers: List[ServiceBrowser] = []
        self.discovered_devices: Dict[str, DiscoveredDevice] = {}
        self.own_services: Dict[str, ServiceInfo] = {}
        self.is_running = False
        self.lock = threading.RLock()
        
        # Callbacks
        self.device_added_callbacks: List[Callable[[DiscoveredDevice], None]] = []
        self.device_removed_callbacks: List[Callable[[str], None]] = []
        self.device_updated_callbacks: List[Callable[[DiscoveredDevice], None]] = []
        
        self.logger.info("Zeroconf discovery initialized")
    
    def start_discovery(self, service_types: Optional[List[str]] = None) -> bool:
        """Start discovering devices on the network."""
        if self.is_running:
            self.logger.warning("Discovery already running")
            return True
        
        try:
            self.logger.info("Starting Zeroconf device discovery...")
            
            # Initialize Zeroconf
            self.zeroconf = Zeroconf()
            
            # Create listener
            listener = DeviceDiscoveryListener(self)
            
            # Start browsers for each service type
            service_types = service_types or self.SERVICE_TYPES
            for service_type in service_types:
                browser = ServiceBrowser(self.zeroconf, service_type, listener)
                self.browsers.append(browser)
                self.logger.debug(f"Started browser for service type: {service_type}")
            
            self.is_running = True
            self.logger.info(f"Zeroconf discovery started, monitoring {len(service_types)} service types")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to start Zeroconf discovery: {e}")
            self.stop_discovery()
            return False
    
    def stop_discovery(self):
        """Stop device discovery."""
        try:
            self.logger.info("Stopping Zeroconf discovery...")
            
            self.is_running = False
            
            # Close browsers
            for browser in self.browsers:
                browser.cancel()
            self.browsers.clear()
            
            # Unregister own services
            for service_info in self.own_services.values():
                if self.zeroconf:
                    self.zeroconf.unregister_service(service_info)
            self.own_services.clear()
            
            # Close Zeroconf
            if self.zeroconf:
                self.zeroconf.close()
                self.zeroconf = None
            
            # Clear discovered devices
            with self.lock:
                self.discovered_devices.clear()
            
            self.logger.info("Zeroconf discovery stopped")
            
        except Exception as e:
            self.logger.error(f"Error stopping Zeroconf discovery: {e}")
    
    def register_service(self, name: str, service_type: str, port: int, 
                        properties: Optional[Dict[str, str]] = None) -> bool:
        """Register our own service for others to discover."""
        if not self.zeroconf:
            self.logger.error("Zeroconf not initialized")
            return False
        
        try:
            # Get local IP address
            hostname = socket.gethostname()
            local_ip = self._get_local_ip()
            
            if not local_ip:
                self.logger.error("Could not determine local IP address")
                return False
            
            # Prepare properties
            props = properties or {}
            props.update({
                "version": "1.0.0",
                "type": "multi-sensor-system",
                "hostname": hostname
            })
            
            # Convert properties to bytes
            props_bytes = {k.encode('utf-8'): v.encode('utf-8') for k, v in props.items()}
            
            # Create service info
            service_info = ServiceInfo(
                type_=service_type,
                name=f"{name}.{service_type}",
                addresses=[socket.inet_aton(local_ip)],
                port=port,
                properties=props_bytes,
                server=f"{hostname}.local."
            )
            
            # Register service
            self.zeroconf.register_service(service_info)
            self.own_services[name] = service_info
            
            self.logger.info(f"Registered service: {name} on {local_ip}:{port}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to register service {name}: {e}")
            return False
    
    def unregister_service(self, name: str) -> bool:
        """Unregister a previously registered service."""
        if not self.zeroconf or name not in self.own_services:
            return False
        
        try:
            service_info = self.own_services[name]
            self.zeroconf.unregister_service(service_info)
            del self.own_services[name]
            
            self.logger.info(f"Unregistered service: {name}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to unregister service {name}: {e}")
            return False
    
    def get_discovered_devices(self) -> Dict[str, DiscoveredDevice]:
        """Get all currently discovered devices."""
        with self.lock:
            return self.discovered_devices.copy()
    
    def find_devices_by_type(self, device_type: str) -> List[DiscoveredDevice]:
        """Find devices by type (from properties)."""
        devices = []
        with self.lock:
            for device in self.discovered_devices.values():
                if device.properties.get("type") == device_type:
                    devices.append(device)
        return devices
    
    def add_device_callback(self, callback: Callable[[DiscoveredDevice], None]):
        """Add callback for when devices are discovered."""
        self.device_added_callbacks.append(callback)
    
    def add_removal_callback(self, callback: Callable[[str], None]):
        """Add callback for when devices are removed."""
        self.device_removed_callbacks.append(callback)
    
    def add_update_callback(self, callback: Callable[[DiscoveredDevice], None]):
        """Add callback for when devices are updated."""
        self.device_updated_callbacks.append(callback)
    
    def _process_service_info(self, info: ServiceInfo, service_type: str, action: str):
        """Process discovered service information."""
        try:
            # Extract basic information
            name = info.name.replace(f".{service_type}", "")
            hostname = info.server.rstrip(".")
            
            # Get IP address
            ip_address = None
            if info.addresses:
                ip_address = socket.inet_ntoa(info.addresses[0])
            
            if not ip_address:
                self.logger.warning(f"No IP address found for service {name}")
                return
            
            # Parse properties
            properties = {}
            if info.properties:
                for key, value in info.properties.items():
                    try:
                        key_str = key.decode('utf-8') if isinstance(key, bytes) else str(key)
                        value_str = value.decode('utf-8') if isinstance(value, bytes) else str(value)
                        properties[key_str] = value_str
                    except UnicodeDecodeError:
                        self.logger.warning(f"Could not decode property {key}={value}")
            
            # Create device object
            current_time = time.time()
            device = DiscoveredDevice(
                name=name,
                service_type=service_type,
                hostname=hostname,
                ip_address=ip_address,
                port=info.port,
                properties=properties,
                discovery_time=current_time,
                last_seen=current_time
            )
            
            # Update device list and notify callbacks
            with self.lock:
                existing = self.discovered_devices.get(name)
                self.discovered_devices[name] = device
                
                if action == "added" or not existing:
                    self.logger.info(f"Device discovered: {name} ({ip_address}:{info.port})")
                    for callback in self.device_added_callbacks:
                        try:
                            callback(device)
                        except Exception as e:
                            self.logger.error(f"Error in device added callback: {e}")
                
                elif action == "updated":
                    self.logger.debug(f"Device updated: {name}")
                    for callback in self.device_updated_callbacks:
                        try:
                            callback(device)
                        except Exception as e:
                            self.logger.error(f"Error in device updated callback: {e}")
            
        except Exception as e:
            self.logger.error(f"Error processing service info: {e}")
    
    def _handle_service_removed(self, name: str, service_type: str):
        """Handle removal of a service."""
        clean_name = name.replace(f".{service_type}", "")
        
        with self.lock:
            if clean_name in self.discovered_devices:
                del self.discovered_devices[clean_name]
                self.logger.info(f"Device removed: {clean_name}")
                
                for callback in self.device_removed_callbacks:
                    try:
                        callback(clean_name)
                    except Exception as e:
                        self.logger.error(f"Error in device removed callback: {e}")
    
    def _get_local_ip(self) -> Optional[str]:
        """Get the local IP address."""
        try:
            # Create a socket to determine the local IP
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                # Connect to a remote address (doesn't actually connect)
                s.connect(("8.8.8.8", 80))
                local_ip = s.getsockname()[0]
                
                # Validate IP address
                ipaddress.ip_address(local_ip)
                return local_ip
                
        except Exception as e:
            self.logger.error(f"Error getting local IP: {e}")
            return None
    
    def cleanup(self):
        """Clean up resources."""
        self.stop_discovery()


class MultiSensorDiscovery:
    """High-level interface for multi-sensor system device discovery."""
    
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.discovery = ZeroconfDiscovery(logger)
        self.android_devices: Dict[str, DiscoveredDevice] = {}
        self.pc_controllers: Dict[str, DiscoveredDevice] = {}
        
        # Set up callbacks
        self.discovery.add_device_callback(self._on_device_discovered)
        self.discovery.add_removal_callback(self._on_device_removed)
    
    def start(self) -> bool:
        """Start device discovery."""
        return self.discovery.start_discovery()
    
    def stop(self):
        """Stop device discovery."""
        self.discovery.stop_discovery()
    
    def register_pc_controller(self, name: str, port: int) -> bool:
        """Register this PC as a controller."""
        properties = {
            "type": "pc-controller",
            "capabilities": "session-management,data-collection,synchronization",
            "version": "1.0.0"
        }
        return self.discovery.register_service(name, "_pc-controller._tcp.local.", port, properties)
    
    def register_android_device(self, name: str, port: int, capabilities: List[str]) -> bool:
        """Register an Android device."""
        properties = {
            "type": "android-device",
            "capabilities": ",".join(capabilities),
            "version": "1.0.0"
        }
        return self.discovery.register_service(name, "_android-device._tcp.local.", port, properties)
    
    def get_android_devices(self) -> List[DiscoveredDevice]:
        """Get all discovered Android devices."""
        return list(self.android_devices.values())
    
    def get_pc_controllers(self) -> List[DiscoveredDevice]:
        """Get all discovered PC controllers."""
        return list(self.pc_controllers.values())
    
    def find_device_by_capability(self, capability: str) -> List[DiscoveredDevice]:
        """Find devices that support a specific capability."""
        devices = []
        all_devices = {**self.android_devices, **self.pc_controllers}
        
        for device in all_devices.values():
            capabilities = device.properties.get("capabilities", "").split(",")
            if capability in capabilities:
                devices.append(device)
        
        return devices
    
    def _on_device_discovered(self, device: DiscoveredDevice):
        """Handle device discovery."""
        device_type = device.properties.get("type", "unknown")
        
        if device_type == "android-device":
            self.android_devices[device.name] = device
            self.logger.info(f"Android device discovered: {device.name} at {device.ip_address}")
        
        elif device_type == "pc-controller":
            self.pc_controllers[device.name] = device
            self.logger.info(f"PC controller discovered: {device.name} at {device.ip_address}")
        
        else:
            self.logger.debug(f"Unknown device type discovered: {device_type} ({device.name})")
    
    def _on_device_removed(self, device_name: str):
        """Handle device removal."""
        if device_name in self.android_devices:
            del self.android_devices[device_name]
            self.logger.info(f"Android device removed: {device_name}")
        
        if device_name in self.pc_controllers:
            del self.pc_controllers[device_name]
            self.logger.info(f"PC controller removed: {device_name}")
    
    def cleanup(self):
        """Clean up resources."""
        self.discovery.cleanup()


if __name__ == "__main__":
    # Example usage
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    # Create discovery manager
    discovery = MultiSensorDiscovery(logger)
    
    try:
        # Start discovery
        if discovery.start():
            logger.info("Discovery started successfully")
            
            # Register as PC controller
            discovery.register_pc_controller("test_pc_controller", 9000)
            
            # Run discovery for 30 seconds
            for i in range(30):
                time.sleep(1)
                
                # Log discovered devices every 10 seconds
                if i % 10 == 9:
                    android_devices = discovery.get_android_devices()
                    pc_controllers = discovery.get_pc_controllers()
                    
                    logger.info(f"Found {len(android_devices)} Android devices, {len(pc_controllers)} PC controllers")
            
        else:
            logger.error("Failed to start discovery")
    
    except KeyboardInterrupt:
        logger.info("Interrupted by user")
    
    finally:
        discovery.cleanup()
        logger.info("Discovery test completed")