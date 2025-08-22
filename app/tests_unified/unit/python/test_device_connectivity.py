#!/usr/bin/env python3
"""
Device Connectivity Tester
==========================

Comprehensive tool for testing bidirectional connectivity between Android devices
and PC servers in the Multi-Sensor Recording System.

This tool helps ensure both devices can detect and connect to each other by:
1. Testing PC server accessibility from different network positions
2. Detecting Android devices via multiple methods (ADB, wireless)
3. Verifying bidirectional communication
4. Providing actionable troubleshooting guidance

Usage:
    python test_device_connectivity.py --full-test
    python test_device_connectivity.py --android-discovery
    python test_device_connectivity.py --pc-server-test
    python test_device_connectivity.py --network-scan
"""

import argparse
import json
import logging
import socket
import subprocess
import sys
import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple

# Add project root to Python path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

try:
    from PythonApp.utils.android_connection_detector import AndroidConnectionDetector
    from PythonApp.network.pc_server import PCServer
    from pc_server_helper import PCServerHelper
    MODULES_AVAILABLE = True
except ImportError as e:
    print(f"Warning: Some modules not available: {e}")
    MODULES_AVAILABLE = False

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class DeviceConnectivityTester:
    """Comprehensive device connectivity testing tool."""
    
    def __init__(self, pc_port: int = 9000, web_port: int = 5000):
        self.pc_port = pc_port
        self.web_port = web_port
        self.android_detector = AndroidConnectionDetector() if MODULES_AVAILABLE else None
        self.pc_helper = PCServerHelper(port=pc_port, web_port=web_port) if MODULES_AVAILABLE else None
        
    def run_full_connectivity_test(self) -> Dict:
        """Run comprehensive bidirectional connectivity test."""
        logger.info("🔍 Starting comprehensive device connectivity test...")
        
        results = {
            "test_timestamp": time.time(),
            "pc_server_status": {},
            "android_detection": {},
            "network_analysis": {},
            "bidirectional_test": {},
            "recommendations": []
        }
        
        # Test 1: PC Server Status
        logger.info("📡 Testing PC server status...")
        results["pc_server_status"] = self._test_pc_server_status()
        
        # Test 2: Android Device Detection  
        logger.info("📱 Testing Android device detection...")
        results["android_detection"] = self._test_android_detection()
        
        # Test 3: Network Analysis
        logger.info("🌐 Analyzing network configuration...")
        results["network_analysis"] = self._analyze_network_config()
        
        # Test 4: Bidirectional Communication Test
        logger.info("🔄 Testing bidirectional communication...")
        results["bidirectional_test"] = self._test_bidirectional_communication()
        
        # Generate recommendations
        results["recommendations"] = self._generate_recommendations(results)
        
        return results
    
    def _test_pc_server_status(self) -> Dict:
        """Test PC server status and accessibility."""
        status = {
            "server_running": False,
            "port_accessible": False,
            "web_ui_accessible": False,
            "local_ip": None,
            "firewall_configured": False,
            "process_info": {}
        }
        
        try:
            if self.pc_helper:
                # Check if server is running
                status["server_running"] = self.pc_helper.is_server_running()
                
                # Get detailed status
                detailed_status = self.pc_helper.get_status()
                status["local_ip"] = detailed_status.get("local_ip")
                status["process_info"] = {
                    "pid_exists": detailed_status.get("pid_file_exists", False),
                    "process_running": detailed_status.get("process_exists", False)
                }
                
                # Test port accessibility
                status["port_accessible"] = self._test_port_accessible(self.pc_port)
                status["web_ui_accessible"] = self._test_port_accessible(self.web_port)
                
                # Check firewall status (simplified)
                status["firewall_configured"] = self._check_firewall_status()
            
        except Exception as e:
            logger.error(f"Error testing PC server status: {e}")
            status["error"] = str(e)
        
        return status
    
    def _test_android_detection(self) -> Dict:
        """Test Android device detection via ADB and other methods."""
        detection = {
            "adb_available": False,
            "devices_found": 0,
            "usb_devices": [],
            "wireless_devices": [],
            "ide_connections": {},
            "detection_methods": []
        }
        
        try:
            if self.android_detector:
                # Detect all Android connections
                devices = self.android_detector.detect_all_connections()
                detection["devices_found"] = len(devices)
                
                # Categorize devices
                for device_id, device in devices.items():
                    device_info = {
                        "device_id": device_id,
                        "status": device.status,
                        "model": device.model,
                        "android_version": device.android_version,
                        "connection_type": device.connection_type.value
                    }
                    
                    if device.connection_type.value == "usb":
                        detection["usb_devices"].append(device_info)
                    elif device.connection_type.value == "wireless_adb":
                        device_info["ip_address"] = device.ip_address
                        device_info["port"] = device.port
                        detection["wireless_devices"].append(device_info)
                
                # Get IDE connections
                detection["ide_connections"] = self.android_detector.get_ide_connected_devices()
                
                # Check ADB availability
                detection["adb_available"] = self.android_detector.adb_path is not None
                
                # Record detection methods that worked
                if detection["devices_found"] > 0:
                    detection["detection_methods"].append("ADB detection successful")
                if detection["ide_connections"]:
                    detection["detection_methods"].append("IDE connections found")
        
        except Exception as e:
            logger.error(f"Error testing Android detection: {e}")
            detection["error"] = str(e)
        
        return detection
    
    def _analyze_network_config(self) -> Dict:
        """Analyze network configuration for connectivity issues."""
        network = {
            "interfaces": [],
            "reachable_subnets": [],
            "potential_android_ips": [],
            "firewall_ports": {},
            "dns_resolution": {}
        }
        
        try:
            # Get network interfaces
            network["interfaces"] = self._get_network_interfaces()
            
            # Identify potential Android device subnets
            network["reachable_subnets"] = self._get_reachable_subnets()
            
            # Generate potential Android IP addresses
            network["potential_android_ips"] = self._generate_potential_android_ips()
            
            # Test common ports
            network["firewall_ports"] = self._test_common_ports()
            
            # Test DNS resolution
            network["dns_resolution"] = self._test_dns_resolution()
        
        except Exception as e:
            logger.error(f"Error analyzing network config: {e}")
            network["error"] = str(e)
        
        return network
    
    def _test_bidirectional_communication(self) -> Dict:
        """Test bidirectional communication between PC and Android devices."""
        bidirectional = {
            "pc_to_android": {},
            "android_to_pc": {},
            "communication_quality": {}
        }
        
        try:
            # Test PC → Android communication
            bidirectional["pc_to_android"] = self._test_pc_to_android()
            
            # Test Android → PC communication 
            bidirectional["android_to_pc"] = self._test_android_to_pc()
            
            # Assess communication quality
            bidirectional["communication_quality"] = self._assess_communication_quality()
        
        except Exception as e:
            logger.error(f"Error testing bidirectional communication: {e}")
            bidirectional["error"] = str(e)
        
        return bidirectional
    
    def _test_port_accessible(self, port: int) -> bool:
        """Test if a port is accessible locally."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.settimeout(2)
                result = sock.connect_ex(('localhost', port))
                return result == 0
        except:
            return False
    
    def _check_firewall_status(self) -> bool:
        """Check if firewall is properly configured (simplified check)."""
        try:
            # This is a simplified check - actual implementation would be OS-specific
            return self._test_port_accessible(self.pc_port)
        except:
            return False
    
    def _get_network_interfaces(self) -> List[Dict]:
        """Get network interface information."""
        interfaces = []
        try:
            import psutil
            for interface, addrs in psutil.net_if_addrs().items():
                for addr in addrs:
                    if addr.family == socket.AF_INET:
                        interfaces.append({
                            "interface": interface,
                            "ip": addr.address,
                            "netmask": addr.netmask
                        })
        except Exception as e:
            logger.warning(f"Failed to get network interfaces: {e}")
        
        return interfaces
    
    def _get_reachable_subnets(self) -> List[str]:
        """Get subnets reachable from current network interfaces."""
        subnets = []
        try:
            interfaces = self._get_network_interfaces()
            for interface in interfaces:
                ip = interface["ip"]
                if not ip.startswith("127."):  # Skip loopback
                    subnet = ".".join(ip.split(".")[:-1]) + ".0/24"
                    subnets.append(subnet)
        except Exception:
            pass
        
        return list(set(subnets))  # Remove duplicates
    
    def _generate_potential_android_ips(self) -> List[str]:
        """Generate potential Android device IP addresses."""
        potential_ips = []
        
        # Get current network subnets
        subnets = self._get_reachable_subnets()
        
        for subnet in subnets:
            base = subnet.split("/")[0].rsplit(".", 1)[0]
            # Common Android device IP ranges
            potential_ips.extend([
                f"{base}.{i}" for i in [2, 3, 4, 5, 10, 20, 50, 100, 101, 102]
            ])
        
        # Add common mobile hotspot ranges
        potential_ips.extend([
            "192.168.43.1",  # Android hotspot gateway
            "192.168.43.2",  # First Android hotspot client
        ])
        
        return list(set(potential_ips))
    
    def _test_common_ports(self) -> Dict[int, bool]:
        """Test accessibility of common ports."""
        ports = [self.pc_port, self.web_port, 5555]  # 5555 is common ADB wireless port
        results = {}
        
        for port in ports:
            results[port] = self._test_port_accessible(port)
        
        return results
    
    def _test_dns_resolution(self) -> Dict:
        """Test DNS resolution capabilities."""
        dns_tests = {
            "localhost": False,
            "hostname": None,
            "can_resolve_hostname": False
        }
        
        try:
            # Test localhost resolution
            socket.gethostbyname("localhost")
            dns_tests["localhost"] = True
        except:
            pass
        
        try:
            # Get and test hostname
            hostname = socket.gethostname()
            dns_tests["hostname"] = hostname
            socket.gethostbyname(hostname)
            dns_tests["can_resolve_hostname"] = True
        except:
            pass
        
        return dns_tests
    
    def _test_pc_to_android(self) -> Dict:
        """Test PC ability to reach Android devices."""
        results = {
            "adb_devices_reachable": 0,
            "wireless_devices_pingable": 0,
            "connection_methods": []
        }
        
        try:
            if self.android_detector:
                devices = self.android_detector.detect_all_connections()
                
                # Count ADB-reachable devices
                results["adb_devices_reachable"] = len([
                    d for d in devices.values() if d.status == "device"
                ])
                
                # Test ping to wireless devices
                wireless_devices = self.android_detector.get_wireless_debugging_devices()
                pingable = 0
                for device in wireless_devices:
                    if device.ip_address and self._ping_device(device.ip_address):
                        pingable += 1
                
                results["wireless_devices_pingable"] = pingable
                
                if results["adb_devices_reachable"] > 0:
                    results["connection_methods"].append("ADB")
                if results["wireless_devices_pingable"] > 0:
                    results["connection_methods"].append("Network ping")
        
        except Exception as e:
            results["error"] = str(e)
        
        return results
    
    def _test_android_to_pc(self) -> Dict:
        """Test Android device ability to reach PC server."""
        results = {
            "server_port_accessible": False,
            "web_port_accessible": False,
            "discovery_simulation": {}
        }
        
        try:
            # Test if PC server ports are accessible (simulating Android perspective)
            results["server_port_accessible"] = self._test_port_accessible(self.pc_port)
            results["web_port_accessible"] = self._test_port_accessible(self.web_port)
            
            # Simulate Android discovery process
            results["discovery_simulation"] = self._simulate_android_discovery()
        
        except Exception as e:
            results["error"] = str(e)
        
        return results
    
    def _simulate_android_discovery(self) -> Dict:
        """Simulate Android device discovery process."""
        simulation = {
            "configured_ip_test": False,
            "network_scan_results": [],
            "common_ips_found": []
        }
        
        try:
            # Test current IP (simulating configured IP test)
            local_ip = self._get_local_ip()
            if local_ip:
                simulation["configured_ip_test"] = self._test_server_connection(local_ip, self.pc_port)
            
            # Simulate network scanning
            potential_ips = self._generate_potential_android_ips()[:10]  # Limit for testing
            for ip in potential_ips:
                if self._test_server_connection(ip, self.pc_port):
                    simulation["network_scan_results"].append(ip)
            
            # Test common server IPs
            common_ips = ["192.168.1.100", "192.168.0.100", "10.0.0.100"]
            for ip in common_ips:
                if self._test_server_connection(ip, self.pc_port):
                    simulation["common_ips_found"].append(ip)
        
        except Exception as e:
            simulation["error"] = str(e)
        
        return simulation
    
    def _assess_communication_quality(self) -> Dict:
        """Assess overall communication quality."""
        quality = {
            "overall_score": 0,
            "connection_reliability": "unknown",
            "latency_estimate": "unknown",
            "bandwidth_estimate": "unknown"
        }
        
        try:
            score = 0
            
            # Basic connectivity (50 points)
            if self._test_port_accessible(self.pc_port):
                score += 50
            
            # Android detection (25 points)
            if self.android_detector:
                devices = self.android_detector.detect_all_connections()
                if len(devices) > 0:
                    score += 25
            
            # Network quality (25 points)
            network_score = 0
            interfaces = self._get_network_interfaces()
            if len(interfaces) > 1:  # Multiple interfaces available
                network_score += 15
            if any("192.168" in iface["ip"] for iface in interfaces):  # Local network
                network_score += 10
            
            score += network_score
            
            quality["overall_score"] = min(score, 100)
            
            # Assess reliability
            if score >= 75:
                quality["connection_reliability"] = "excellent"
            elif score >= 50:
                quality["connection_reliability"] = "good"
            elif score >= 25:
                quality["connection_reliability"] = "fair"
            else:
                quality["connection_reliability"] = "poor"
        
        except Exception as e:
            quality["error"] = str(e)
        
        return quality
    
    def _ping_device(self, ip: str) -> bool:
        """Ping a device to test reachability."""
        try:
            import platform
            param = "-n" if platform.system().lower() == "windows" else "-c"
            result = subprocess.run(
                ["ping", param, "1", ip], 
                capture_output=True, 
                timeout=3
            )
            return result.returncode == 0
        except:
            return False
    
    def _get_local_ip(self) -> Optional[str]:
        """Get local IP address."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
                sock.connect(("8.8.8.8", 80))
                return sock.getsockname()[0]
        except:
            return None
    
    def _test_server_connection(self, ip: str, port: int) -> bool:
        """Test connection to a server."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.settimeout(2)
                result = sock.connect_ex((ip, port))
                return result == 0
        except:
            return False
    
    def _generate_recommendations(self, results: Dict) -> List[str]:
        """Generate actionable recommendations based on test results."""
        recommendations = []
        
        # PC Server recommendations
        pc_status = results.get("pc_server_status", {})
        if not pc_status.get("server_running", False):
            recommendations.append("🚀 Start PC server: python pc_server_helper.py --start")
        
        if not pc_status.get("port_accessible", False):
            recommendations.append("🔥 Configure firewall: python pc_server_helper.py --configure-firewall")
        
        # Android detection recommendations
        android_detection = results.get("android_detection", {})
        if android_detection.get("devices_found", 0) == 0:
            recommendations.append("📱 Enable USB debugging on Android device")
            recommendations.append("🔌 Connect Android device via USB or enable wireless debugging")
        
        if not android_detection.get("adb_available", False):
            recommendations.append("⚙️ Install Android Debug Bridge (ADB)")
        
        # Network recommendations
        network = results.get("network_analysis", {})
        if len(network.get("interfaces", [])) < 2:
            recommendations.append("🌐 Ensure both devices are on the same network")
        
        # Bidirectional communication recommendations
        bidirectional = results.get("bidirectional_test", {})
        android_to_pc = bidirectional.get("android_to_pc", {})
        if not android_to_pc.get("server_port_accessible", False):
            recommendations.append("🔄 Check network connectivity between devices")
            recommendations.append("📍 Try manual IP configuration in Android app")
        
        # Quality-based recommendations
        quality = bidirectional.get("communication_quality", {})
        if quality.get("overall_score", 0) < 50:
            recommendations.append("⚡ Consider using wired connection for better reliability")
            recommendations.append("📶 Move devices closer together for better WiFi signal")
        
        return recommendations
    
    def print_test_results(self, results: Dict):
        """Print formatted test results."""
        print("\n" + "="*80)
        print("🔍 DEVICE CONNECTIVITY TEST RESULTS")
        print("="*80)
        
        # Overall summary
        quality = results.get("bidirectional_test", {}).get("communication_quality", {})
        score = quality.get("overall_score", 0)
        reliability = quality.get("connection_reliability", "unknown")
        
        print(f"\n📊 Overall Connectivity Score: {score}/100 ({reliability.upper()})")
        
        # PC Server Status
        print(f"\n📡 PC Server Status:")
        pc_status = results.get("pc_server_status", {})
        print(f"  Server Running: {'✅' if pc_status.get('server_running') else '❌'}")
        print(f"  Port Accessible: {'✅' if pc_status.get('port_accessible') else '❌'}")
        print(f"  Web UI Accessible: {'✅' if pc_status.get('web_ui_accessible') else '❌'}")
        if pc_status.get("local_ip"):
            print(f"  Local IP: {pc_status['local_ip']}")
        
        # Android Detection
        print(f"\n📱 Android Device Detection:")
        android = results.get("android_detection", {})
        print(f"  ADB Available: {'✅' if android.get('adb_available') else '❌'}")
        print(f"  Devices Found: {android.get('devices_found', 0)}")
        print(f"  USB Devices: {len(android.get('usb_devices', []))}")
        print(f"  Wireless Devices: {len(android.get('wireless_devices', []))}")
        
        # Network Analysis
        print(f"\n🌐 Network Analysis:")
        network = results.get("network_analysis", {})
        print(f"  Network Interfaces: {len(network.get('interfaces', []))}")
        print(f"  Reachable Subnets: {len(network.get('reachable_subnets', []))}")
        
        # Bidirectional Communication
        print(f"\n🔄 Bidirectional Communication:")
        bidirectional = results.get("bidirectional_test", {})
        pc_to_android = bidirectional.get("pc_to_android", {})
        android_to_pc = bidirectional.get("android_to_pc", {})
        
        print(f"  PC → Android:")
        print(f"    ADB Devices Reachable: {pc_to_android.get('adb_devices_reachable', 0)}")
        print(f"    Wireless Devices Pingable: {pc_to_android.get('wireless_devices_pingable', 0)}")
        
        print(f"  Android → PC:")
        print(f"    Server Port Accessible: {'✅' if android_to_pc.get('server_port_accessible') else '❌'}")
        print(f"    Web Port Accessible: {'✅' if android_to_pc.get('web_port_accessible') else '❌'}")
        
        # Recommendations
        recommendations = results.get("recommendations", [])
        if recommendations:
            print(f"\n💡 RECOMMENDATIONS:")
            for i, rec in enumerate(recommendations, 1):
                print(f"  {i}. {rec}")
        
        print(f"\n✅ Test completed at {time.strftime('%Y-%m-%d %H:%M:%S')}")


def main():
    """Main CLI interface."""
    parser = argparse.ArgumentParser(
        description="Device Connectivity Tester - Test bidirectional connectivity",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    
    parser.add_argument('--full-test', action='store_true', 
                       help='Run comprehensive connectivity test')
    parser.add_argument('--android-discovery', action='store_true',
                       help='Test Android device discovery only')
    parser.add_argument('--pc-server-test', action='store_true',
                       help='Test PC server status only')
    parser.add_argument('--network-scan', action='store_true',
                       help='Analyze network configuration only')
    parser.add_argument('--port', type=int, default=9000,
                       help='PC server port (default: 9000)')
    parser.add_argument('--web-port', type=int, default=5000,
                       help='Web UI port (default: 5000)')
    parser.add_argument('--output', help='Save results to JSON file')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Verbose output')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    if not MODULES_AVAILABLE:
        print("❌ Required modules not available. Please ensure the project is properly set up.")
        sys.exit(1)
    
    tester = DeviceConnectivityTester(port=args.port, web_port=args.web_port)
    
    try:
        results = None
        
        if args.full_test or not any([args.android_discovery, args.pc_server_test, args.network_scan]):
            results = tester.run_full_connectivity_test()
        elif args.android_discovery:
            results = {"android_detection": tester._test_android_detection()}
        elif args.pc_server_test:
            results = {"pc_server_status": tester._test_pc_server_status()}
        elif args.network_scan:
            results = {"network_analysis": tester._analyze_network_config()}
        
        if results:
            tester.print_test_results(results)
            
            if args.output:
                with open(args.output, 'w') as f:
                    json.dump(results, f, indent=2, default=str)
                print(f"\n📄 Results saved to: {args.output}")
    
    except KeyboardInterrupt:
        print("\n🛑 Test cancelled by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Test failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()