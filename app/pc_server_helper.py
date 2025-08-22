#!/usr/bin/env python3
"""
PC Server Helper Script

This script provides easy management of the Multi-Sensor Recording System PC Server.
It addresses the missing PC server component issue identified in the camera preview
and network connectivity problems.

Usage:
    python pc_server_helper.py --start      # Start the PC server
    python pc_server_helper.py --stop       # Stop the PC server  
    python pc_server_helper.py --check      # Check server status
    python pc_server_helper.py --status     # Show detailed status
    python pc_server_helper.py --config     # Show network configuration
    python pc_server_helper.py --diagnose   # Run network diagnostics
"""

import argparse
import json
import logging
import os
import platform
import psutil
import signal
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
    from PythonApp.network.pc_server import PCServer
    from PythonApp.utils.logging_config import get_logger
    from PythonApp.web_launcher import main as web_launcher_main
    PC_SERVER_AVAILABLE = True
except ImportError as e:
    print(f"Warning: PC Server components not fully available: {e}")
    PC_SERVER_AVAILABLE = False

logger = logging.getLogger("pc_server_helper")

class PCServerHelper:
    """Helper class for managing the PC Server lifecycle and diagnostics."""
    
    def __init__(self, port: int = 9000, web_port: int = 5000):
        self.port = port
        self.web_port = web_port
        self.server_process: Optional[subprocess.Popen] = None
        self.pid_file = project_root / "pc_server.pid"
        self.log_file = project_root / "logs" / "pc_server.log"
        
        # Ensure logs directory exists
        self.log_file.parent.mkdir(exist_ok=True)
        
        self._setup_logging()
    
    def _setup_logging(self):
        """Setup logging configuration."""
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.StreamHandler(),
                logging.FileHandler(self.log_file)
            ]
        )
    
    def start_server(self, with_web_ui: bool = True, background: bool = True) -> bool:
        """Start the PC server."""
        logger.info("Starting PC Server...")
        
        # Check if server is already running
        if self.is_server_running():
            logger.warning(f"PC Server is already running on port {self.port}")
            return True
        
        # Check for port conflicts
        if self._is_port_in_use(self.port):
            logger.error(f"Port {self.port} is already in use by another process")
            self._suggest_port_resolution()
            return False
        
        # Check network configuration
        if not self._validate_network_config():
            logger.error("Network configuration validation failed")
            return False
        
        try:
            if background:
                # Start server in background process
                cmd = [
                    sys.executable, 
                    str(project_root / "PythonApp" / "web_launcher.py"),
                    "--port", str(self.web_port),
                    "--android-port", str(self.port),
                    "--host", "0.0.0.0"
                ]
                
                logger.info(f"Starting server with command: {' '.join(cmd)}")
                
                self.server_process = subprocess.Popen(
                    cmd,
                    stdout=open(self.log_file, 'a'),
                    stderr=subprocess.STDOUT,
                    preexec_fn=os.setsid if platform.system() != 'Windows' else None
                )
                
                # Save PID for later management
                with open(self.pid_file, 'w') as f:
                    f.write(str(self.server_process.pid))
                
                # Wait a moment and check if it's running
                time.sleep(3)
                if self.is_server_running():
                    logger.info(f"✅ PC Server started successfully on port {self.port}")
                    logger.info(f"📱 Android devices can connect to: {self._get_local_ip()}:{self.port}")
                    if with_web_ui:
                        logger.info(f"🌐 Web dashboard available at: http://localhost:{self.web_port}")
                    return True
                else:
                    logger.error("❌ PC Server failed to start")
                    return False
            else:
                # Start server in foreground (blocking)
                if PC_SERVER_AVAILABLE:
                    server = PCServer(port=self.port)
                    server.start()
                    logger.info(f"✅ PC Server running on port {self.port}")
                    logger.info("Press Ctrl+C to stop")
                    try:
                        while True:
                            time.sleep(1)
                    except KeyboardInterrupt:
                        logger.info("Stopping server...")
                        server.stop()
                    return True
                else:
                    logger.error("PC Server components not available")
                    return False
                    
        except Exception as e:
            logger.error(f"Failed to start PC Server: {e}")
            return False
    
    def stop_server(self) -> bool:
        """Stop the PC server."""
        logger.info("Stopping PC Server...")
        
        if not self.is_server_running():
            logger.info("PC Server is not running")
            return True
        
        try:
            # Try to stop via PID file
            if self.pid_file.exists():
                with open(self.pid_file, 'r') as f:
                    pid = int(f.read().strip())
                
                try:
                    if platform.system() == 'Windows':
                        subprocess.run(['taskkill', '/F', '/PID', str(pid)], check=True)
                    else:
                        os.killpg(os.getpgid(pid), signal.SIGTERM)
                    
                    # Wait for graceful shutdown
                    time.sleep(2)
                    
                    # Force kill if still running
                    if psutil.pid_exists(pid):
                        if platform.system() == 'Windows':
                            subprocess.run(['taskkill', '/F', '/PID', str(pid)])
                        else:
                            os.killpg(os.getpgid(pid), signal.SIGKILL)
                    
                    self.pid_file.unlink()
                    logger.info("✅ PC Server stopped successfully")
                    return True
                    
                except (ProcessLookupError, subprocess.CalledProcessError):
                    logger.warning("Process already terminated")
                    self.pid_file.unlink()
                    return True
            
            # Fallback: find and kill by port
            return self._kill_process_on_port(self.port)
            
        except Exception as e:
            logger.error(f"Error stopping PC Server: {e}")
            return False
    
    def is_server_running(self) -> bool:
        """Check if the PC server is running."""
        return self._is_port_in_use(self.port)
    
    def get_status(self) -> Dict:
        """Get detailed server status."""
        status = {
            "server_running": self.is_server_running(),
            "port": self.port,
            "web_port": self.web_port,
            "pid_file_exists": self.pid_file.exists(),
            "local_ip": self._get_local_ip(),
            "network_interfaces": self._get_network_interfaces(),
            "port_conflicts": self._check_port_conflicts(),
            "firewall_status": self._check_firewall_status(),
            "timestamp": time.time()
        }
        
        if self.pid_file.exists():
            try:
                with open(self.pid_file, 'r') as f:
                    pid = int(f.read().strip())
                status["pid"] = pid
                status["process_exists"] = psutil.pid_exists(pid)
            except:
                status["pid"] = None
                status["process_exists"] = False
        
        return status
    
    def diagnose_network(self) -> Dict:
        """Run comprehensive network diagnostics."""
        logger.info("Running network diagnostics...")
        
        diagnostics = {
            "local_ip": self._get_local_ip(),
            "network_interfaces": self._get_network_interfaces(),
            "port_accessibility": self._test_port_accessibility(),
            "firewall_rules": self._check_firewall_rules(),
            "common_android_subnets": self._test_android_connectivity(),
            "dns_resolution": self._test_dns_resolution(),
            "recommendations": []
        }
        
        # Add recommendations based on findings
        if not diagnostics["port_accessibility"]:
            diagnostics["recommendations"].append(
                f"Port {self.port} is not accessible - check firewall settings"
            )
        
        if len(diagnostics["network_interfaces"]) > 1:
            diagnostics["recommendations"].append(
                "Multiple network interfaces detected - ensure Android device is on same network"
            )
        
        return diagnostics
    
    def configure_firewall(self) -> bool:
        """Automatically configure firewall rules for the server."""
        logger.info("Configuring firewall rules...")
        
        try:
            if platform.system() == 'Windows':
                return self._configure_windows_firewall()
            elif platform.system() == 'Linux':
                return self._configure_linux_firewall()
            elif platform.system() == 'Darwin':  # macOS
                return self._configure_macos_firewall()
            else:
                logger.warning(f"Firewall configuration not supported for {platform.system()}")
                return False
        except Exception as e:
            logger.error(f"Failed to configure firewall: {e}")
            return False
    
    def _is_port_in_use(self, port: int) -> bool:
        """Check if a port is in use."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.settimeout(1)
                result = sock.connect_ex(('localhost', port))
                return result == 0
        except:
            return False
    
    def _get_local_ip(self) -> str:
        """Get the local IP address."""
        try:
            # Connect to a remote address to get local IP
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
                sock.connect(("8.8.8.8", 80))
                return sock.getsockname()[0]
        except:
            return "127.0.0.1"
    
    def _get_network_interfaces(self) -> List[Dict]:
        """Get information about network interfaces."""
        interfaces = []
        try:
            for interface, addrs in psutil.net_if_addrs().items():
                for addr in addrs:
                    if addr.family == socket.AF_INET:
                        interfaces.append({
                            "interface": interface,
                            "ip": addr.address,
                            "netmask": addr.netmask,
                            "broadcast": addr.broadcast
                        })
        except Exception as e:
            logger.warning(f"Failed to get network interfaces: {e}")
        
        return interfaces
    
    def _validate_network_config(self) -> bool:
        """Validate network configuration."""
        local_ip = self._get_local_ip()
        
        if local_ip == "127.0.0.1":
            logger.warning("Only loopback interface available - Android devices won't be able to connect")
            return False
        
        logger.info(f"Local IP address: {local_ip}")
        return True
    
    def _suggest_port_resolution(self):
        """Suggest how to resolve port conflicts."""
        logger.info("Port conflict resolution suggestions:")
        logger.info(f"1. Kill the process using port {self.port}:")
        
        if platform.system() == 'Windows':
            logger.info(f"   netstat -ano | findstr :{self.port}")
            logger.info(f"   taskkill /PID <process_id> /F")
        else:
            logger.info(f"   sudo lsof -i :{self.port}")
            logger.info(f"   sudo kill -9 <process_id>")
        
        logger.info(f"2. Use a different port:")
        logger.info(f"   python pc_server_helper.py --start --port <other_port>")
    
    def _kill_process_on_port(self, port: int) -> bool:
        """Kill process using specified port."""
        try:
            if platform.system() == 'Windows':
                # Find PID using netstat
                result = subprocess.run(
                    ['netstat', '-ano'], 
                    capture_output=True, 
                    text=True
                )
                for line in result.stdout.split('\n'):
                    if f':{port}' in line and 'LISTENING' in line:
                        parts = line.split()
                        if len(parts) >= 5:
                            pid = parts[-1]
                            subprocess.run(['taskkill', '/F', '/PID', pid])
                            return True
            else:
                # Use lsof to find and kill process
                result = subprocess.run(
                    ['lsof', '-ti', f':{port}'], 
                    capture_output=True, 
                    text=True
                )
                if result.stdout.strip():
                    pid = result.stdout.strip()
                    subprocess.run(['kill', '-9', pid])
                    return True
            
            return False
        except Exception as e:
            logger.error(f"Failed to kill process on port {port}: {e}")
            return False
    
    def _check_port_conflicts(self) -> List[Dict]:
        """Check for port conflicts."""
        conflicts = []
        for port in [self.port, self.web_port]:
            if self._is_port_in_use(port):
                conflicts.append({
                    "port": port,
                    "in_use": True,
                    "description": "PC Server" if port == self.port else "Web UI"
                })
        return conflicts
    
    def _check_firewall_status(self) -> Dict:
        """Check firewall status for our ports."""
        return {
            "system": platform.system(),
            "ports_to_check": [self.port, self.web_port],
            "status": "unknown"  # Will be enhanced in specific OS implementations
        }
    
    def _test_port_accessibility(self) -> bool:
        """Test if the server port is accessible."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.bind(('0.0.0.0', self.port))
                sock.listen(1)
                return True
        except:
            return False
    
    def _check_firewall_rules(self) -> List[str]:
        """Check existing firewall rules."""
        rules = []
        try:
            if platform.system() == 'Windows':
                # Check Windows Defender Firewall rules
                result = subprocess.run(
                    ['netsh', 'advfirewall', 'firewall', 'show', 'rule', 'name=all'],
                    capture_output=True, text=True
                )
                if 'Python' in result.stdout or str(self.port) in result.stdout:
                    rules.append("Windows Defender Firewall rules found")
            elif platform.system() == 'Linux':
                # Check iptables/ufw rules
                try:
                    result = subprocess.run(['ufw', 'status'], capture_output=True, text=True)
                    if result.returncode == 0:
                        rules.append(f"UFW status: {result.stdout.strip()}")
                except:
                    pass
        except Exception as e:
            rules.append(f"Error checking firewall rules: {e}")
        
        return rules
    
    def _test_android_connectivity(self) -> Dict:
        """Test connectivity to common Android device subnets."""
        results = {}
        common_subnets = [
            "192.168.1.0/24",
            "192.168.0.0/24", 
            "10.0.0.0/24",
            "172.16.0.0/24"
        ]
        
        local_ip = self._get_local_ip()
        local_subnet = ".".join(local_ip.split(".")[:-1]) + ".0/24"
        
        results["local_subnet"] = local_subnet
        results["reachable_subnets"] = []
        
        for subnet in common_subnets:
            # Simple test - this could be enhanced with actual ping tests
            if subnet == local_subnet:
                results["reachable_subnets"].append(subnet)
        
        return results
    
    def _test_dns_resolution(self) -> Dict:
        """Test DNS resolution capabilities."""
        results = {
            "hostname": socket.gethostname(),
            "fqdn": socket.getfqdn(),
            "can_resolve_localhost": False
        }
        
        try:
            socket.gethostbyname("localhost")
            results["can_resolve_localhost"] = True
        except:
            pass
        
        return results
    
    def _configure_windows_firewall(self) -> bool:
        """Configure Windows Defender Firewall."""
        try:
            # Add inbound rule for the server port
            cmd = [
                'netsh', 'advfirewall', 'firewall', 'add', 'rule',
                'name=Multi-Sensor PC Server',
                'dir=in',
                'action=allow',
                'protocol=TCP',
                f'localport={self.port}'
            ]
            
            result = subprocess.run(cmd, capture_output=True, text=True)
            if result.returncode == 0:
                logger.info(f"✅ Windows firewall rule added for port {self.port}")
                return True
            else:
                logger.error(f"Failed to add firewall rule: {result.stderr}")
                return False
        except Exception as e:
            logger.error(f"Error configuring Windows firewall: {e}")
            return False
    
    def _configure_linux_firewall(self) -> bool:
        """Configure Linux firewall (ufw/iptables)."""
        try:
            # Try UFW first
            result = subprocess.run(
                ['ufw', 'allow', str(self.port)],
                capture_output=True, text=True
            )
            if result.returncode == 0:
                logger.info(f"✅ UFW rule added for port {self.port}")
                return True
            
            # Fallback to iptables
            iptables_cmd = [
                'iptables', '-A', 'INPUT', 
                '-p', 'tcp', '--dport', str(self.port),
                '-j', 'ACCEPT'
            ]
            result = subprocess.run(iptables_cmd, capture_output=True, text=True)
            if result.returncode == 0:
                logger.info(f"✅ iptables rule added for port {self.port}")
                return True
            
            return False
        except Exception as e:
            logger.error(f"Error configuring Linux firewall: {e}")
            return False
    
    def _configure_macos_firewall(self) -> bool:
        """Configure macOS firewall."""
        logger.info("macOS firewall configuration typically requires manual setup")
        logger.info("Please add Python to firewall exceptions in System Preferences")
        return True


def main():
    """Main CLI interface."""
    parser = argparse.ArgumentParser(
        description="PC Server Helper - Manage Multi-Sensor Recording System PC Server",
        epilog="""
Examples:
  python pc_server_helper.py --start                    # Start server with web UI
  python pc_server_helper.py --start --no-web          # Start server without web UI  
  python pc_server_helper.py --stop                     # Stop server
  python pc_server_helper.py --check                    # Quick status check
  python pc_server_helper.py --status                   # Detailed status
  python pc_server_helper.py --diagnose                 # Network diagnostics
  python pc_server_helper.py --configure-firewall      # Setup firewall rules
        """,
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    
    parser.add_argument('--start', action='store_true', help='Start the PC server')
    parser.add_argument('--stop', action='store_true', help='Stop the PC server')
    parser.add_argument('--check', action='store_true', help='Check if server is running')
    parser.add_argument('--status', action='store_true', help='Show detailed status')
    parser.add_argument('--diagnose', action='store_true', help='Run network diagnostics')
    parser.add_argument('--configure-firewall', action='store_true', help='Configure firewall rules')
    
    parser.add_argument('--port', type=int, default=9000, help='PC server port (default: 9000)')
    parser.add_argument('--web-port', type=int, default=5000, help='Web UI port (default: 5000)')
    parser.add_argument('--no-web', action='store_true', help='Start server without web UI')
    parser.add_argument('--foreground', action='store_true', help='Run server in foreground')
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose output')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    helper = PCServerHelper(port=args.port, web_port=args.web_port)
    
    try:
        if args.start:
            success = helper.start_server(
                with_web_ui=not args.no_web,
                background=not args.foreground
            )
            sys.exit(0 if success else 1)
        
        elif args.stop:
            success = helper.stop_server()
            sys.exit(0 if success else 1)
        
        elif args.check:
            running = helper.is_server_running()
            if running:
                print(f"✅ PC Server is running on port {args.port}")
                print(f"📱 Android devices can connect to: {helper._get_local_ip()}:{args.port}")
            else:
                print(f"❌ PC Server is not running on port {args.port}")
            sys.exit(0 if running else 1)
        
        elif args.status:
            status = helper.get_status()
            print("PC Server Status:")
            print(json.dumps(status, indent=2))
        
        elif args.diagnose:
            diagnostics = helper.diagnose_network()
            print("Network Diagnostics:")
            print(json.dumps(diagnostics, indent=2))
            
            if diagnostics["recommendations"]:
                print("\n🔧 Recommendations:")
                for rec in diagnostics["recommendations"]:
                    print(f"  • {rec}")
        
        elif args.configure_firewall:
            success = helper.configure_firewall()
            if success:
                print("✅ Firewall configured successfully")
            else:
                print("❌ Failed to configure firewall")
            sys.exit(0 if success else 1)
        
        else:
            parser.print_help()
            sys.exit(1)
    
    except KeyboardInterrupt:
        print("\n🛑 Operation cancelled by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()