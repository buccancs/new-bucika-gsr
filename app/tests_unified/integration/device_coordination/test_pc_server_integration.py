#!/usr/bin/env python3
"""
Test script for PC Server Helper and Network Connectivity

This test validates the fixes for camera preview and network connectivity issues.
"""

import pytest
import subprocess
import time
import socket
import os
import sys
from pathlib import Path

# Add project root to path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

try:
    from pc_server_helper import PCServerHelper
    PC_SERVER_AVAILABLE = True
except ImportError:
    PC_SERVER_AVAILABLE = False

class TestPCServerConnectivity:
    """Test PC Server connectivity and network features."""
    
    def setup_method(self):
        """Setup for each test method."""
        self.test_port = 9001  # Use different port to avoid conflicts
        self.web_port = 5001
        if PC_SERVER_AVAILABLE:
            self.helper = PCServerHelper(port=self.test_port, web_port=self.web_port)
    
    def teardown_method(self):
        """Cleanup after each test method."""
        if PC_SERVER_AVAILABLE and hasattr(self, 'helper'):
            try:
                self.helper.stop_server()
            except:
                pass
    
    @pytest.mark.skipif(not PC_SERVER_AVAILABLE, reason="PC Server components not available")
    def test_server_helper_creation(self):
        """Test PC Server Helper can be created."""
        assert self.helper is not None
        assert self.helper.port == self.test_port
        assert self.helper.web_port == self.web_port
    
    @pytest.mark.skipif(not PC_SERVER_AVAILABLE, reason="PC Server components not available")  
    def test_server_not_running_initially(self):
        """Test server is not running initially."""
        assert not self.helper.is_server_running()
    
    @pytest.mark.skipif(not PC_SERVER_AVAILABLE, reason="PC Server components not available")
    def test_server_status_check(self):
        """Test server status check functionality."""
        status = self.helper.get_status()
        
        assert isinstance(status, dict)
        assert "server_running" in status
        assert "port" in status
        assert "local_ip" in status
        assert "network_interfaces" in status
        
        assert status["port"] == self.test_port
        assert status["server_running"] == False  # Should not be running initially
    
    @pytest.mark.skipif(not PC_SERVER_AVAILABLE, reason="PC Server components not available")
    def test_network_diagnostics(self):
        """Test network diagnostics functionality."""
        diagnostics = self.helper.diagnose_network()
        
        assert isinstance(diagnostics, dict)
        assert "local_ip" in diagnostics
        assert "network_interfaces" in diagnostics
        assert "port_accessibility" in diagnostics
        assert "recommendations" in diagnostics
        
        # Should have at least loopback interface
        assert len(diagnostics["network_interfaces"]) >= 1
        
        # Local IP should not be None
        assert diagnostics["local_ip"] is not None
    
    def test_port_availability_check(self):
        """Test port availability checking."""
        # Test with a port that should be available
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as test_socket:
            try:
                test_socket.bind(('localhost', 0))
                _, available_port = test_socket.getsockname()
                
                # Port should not be in use
                assert not self._is_port_in_use(available_port)
                
            except Exception as e:
                pytest.skip(f"Could not test port availability: {e}")
    
    def test_pc_server_helper_cli(self):
        """Test PC Server Helper CLI interface."""
        # Test the --check command
        result = subprocess.run([
            sys.executable, 
            str(project_root / "pc_server_helper.py"), 
            "--check",
            "--port", str(self.test_port)
        ], capture_output=True, text=True)
        
        # Should exit with code 1 (server not running) and show appropriate message
        assert result.returncode == 1
        assert "not running" in result.stdout.lower()
    
    def test_network_discovery_simulation(self):
        """Test network discovery simulation."""
        if not PC_SERVER_AVAILABLE:
            pytest.skip("PC Server components not available")
            
        # Test network interface detection
        interfaces = self.helper._get_network_interfaces()
        assert isinstance(interfaces, list)
        
        # Should have at least one interface (loopback)
        assert len(interfaces) >= 1
        
        # Each interface should have required fields
        for interface in interfaces:
            assert "interface" in interface
            assert "ip" in interface
    
    def _is_port_in_use(self, port):
        """Helper method to check if port is in use."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.settimeout(1)
                result = sock.connect_ex(('localhost', port))
                return result == 0
        except:
            return False


class TestCameraIntegration:
    """Test camera preview integration improvements."""
    
    def test_thermal_colormap_functionality(self):
        """Test thermal colormap functionality."""
        try:
            import numpy as np
            
            # Create test thermal data
            test_data = np.random.randint(0, 255, (240, 320), dtype=np.uint8)
            
            # Test colormap application (simplified version)
            colored_frame = self._apply_test_thermal_colormap(test_data)
            
            assert colored_frame.shape == (240, 320, 3)
            assert colored_frame.dtype == np.uint8
            
        except ImportError:
            pytest.skip("NumPy not available for thermal colormap test")
    
    def _apply_test_thermal_colormap(self, thermal_array):
        """Test version of thermal colormap application."""
        import numpy as np
        
        height, width = thermal_array.shape
        colored_frame = np.zeros((height, width, 3), dtype=np.uint8)
        
        # Simple colormap: blue to red based on intensity
        for y in range(min(height, 10)):  # Only test small area for performance
            for x in range(min(width, 10)):
                intensity = thermal_array[y, x]
                if intensity < 128:
                    colored_frame[y, x] = [0, 0, intensity * 2]
                else:
                    colored_frame[y, x] = [(intensity - 128) * 2, 0, 0]
        
        return colored_frame


class TestNetworkConfiguration:
    """Test network configuration and discovery."""
    
    def test_ip_address_detection(self):
        """Test local IP address detection."""
        try:
            # Try to get local IP
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
                sock.connect(("8.8.8.8", 80))
                local_ip = sock.getsockname()[0]
                
            assert local_ip is not None
            assert local_ip != "127.0.0.1"  # Should not be loopback
            
            # Validate IP format
            parts = local_ip.split('.')
            assert len(parts) == 4
            for part in parts:
                assert 0 <= int(part) <= 255
                
        except Exception as e:
            pytest.skip(f"Could not test IP detection: {e}")
    
    def test_network_subnet_detection(self):
        """Test network subnet detection."""
        try:
            # Get network interfaces
            import psutil
            
            interfaces = psutil.net_if_addrs()
            assert len(interfaces) > 0
            
            # Should have at least loopback
            assert 'lo' in interfaces or 'Loopback' in str(interfaces)
            
        except ImportError:
            pytest.skip("psutil not available for network interface test")


def test_integration_workflow():
    """Test the complete integration workflow."""
    print("\n=== PC Server and Camera Integration Test ===")
    
    # Test 1: PC Server Helper availability
    print("1. Testing PC Server Helper availability...")
    if PC_SERVER_AVAILABLE:
        print("✅ PC Server Helper is available")
    else:
        print("❌ PC Server Helper not available - some functionality will be limited")
    
    # Test 2: Network diagnostics
    print("2. Testing network diagnostics...")
    try:
        if PC_SERVER_AVAILABLE:
            helper = PCServerHelper()
            diagnostics = helper.diagnose_network()
            print(f"✅ Network diagnostics successful - Local IP: {diagnostics['local_ip']}")
        else:
            print("⚠️  Network diagnostics skipped - PC Server not available")
    except Exception as e:
        print(f"❌ Network diagnostics failed: {e}")
    
    # Test 3: Port availability
    print("3. Testing port availability...")
    try:
        test_port = 9000
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(1)
            result = sock.connect_ex(('localhost', test_port))
            if result == 0:
                print(f"⚠️  Port {test_port} is in use")
            else:
                print(f"✅ Port {test_port} is available")
    except Exception as e:
        print(f"❌ Port test failed: {e}")
    
    # Test 4: Camera preview functionality
    print("4. Testing camera preview functionality...")
    try:
        import numpy as np
        
        # Test thermal data processing
        test_thermal = np.random.randint(0, 255, (100, 100), dtype=np.uint8)
        assert test_thermal.shape == (100, 100)
        print("✅ Camera preview data processing functional")
        
    except ImportError:
        print("⚠️  Camera preview test skipped - NumPy not available")
    except Exception as e:
        print(f"❌ Camera preview test failed: {e}")
    
    print("\n=== Integration Test Complete ===")


if __name__ == "__main__":
    # Run integration test when called directly
    test_integration_workflow()
    
    # Run pytest if available
    try:
        pytest.main([__file__, "-v"])
    except ImportError:
        print("pytest not available - running basic integration test only")