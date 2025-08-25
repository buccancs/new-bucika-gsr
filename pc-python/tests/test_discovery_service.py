#!/usr/bin/env python3
"""
Test suite for mDNS discovery service functionality
"""

import unittest
import asyncio
import tempfile
import shutil
from pathlib import Path
from unittest.mock import Mock, patch, AsyncMock, MagicMock
from src.bucika_gsr_pc.discovery_service import DiscoveryService


class TestDiscoveryService(unittest.TestCase):
    """Test mDNS discovery service functionality"""
    
    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()
        self.service = DiscoveryService(
            service_name="Test BucikaGSR PC",
            service_type="_bucika-gsr._tcp.local.",
            port=8080
        )
    
    def tearDown(self):
        """Clean up test environment"""
        if self.service.running:
            try:
                asyncio.get_event_loop().run_until_complete(self.service.stop())
            except Exception:
                pass
        shutil.rmtree(self.temp_dir)
    
    def test_service_initialization(self):
        """Test discovery service initialization"""
        self.assertEqual(self.service.service_name, "Test BucikaGSR PC")
        self.assertEqual(self.service.service_type, "_bucika-gsr._tcp.local.")
        self.assertEqual(self.service.port, 8080)
        self.assertFalse(self.service.running)
        self.assertIsNone(self.service.zeroconf)
    
    def test_service_info_creation(self):
        """Test service info creation for mDNS registration"""
        service_info = self.service._create_service_info()
        
        self.assertIsNotNone(service_info)
        self.assertEqual(service_info.port, 8080)
        self.assertEqual(service_info.type_, "_bucika-gsr._tcp.local.")
        
        # Check service properties
        properties = service_info.properties
        self.assertIn(b'version', properties)
        self.assertIn(b'capabilities', properties)
        self.assertIn(b'protocol', properties)
    
    @patch('zeroconf.Zeroconf')
    def test_service_registration(self, mock_zeroconf_class):
        """Test mDNS service registration"""
        async def run_test():
            mock_zeroconf = Mock()
            mock_zeroconf_class.return_value = mock_zeroconf
            
            # Start service
            await self.service.start()
            
            # Should create zeroconf instance
            mock_zeroconf_class.assert_called_once()
            
            # Should register service
            mock_zeroconf.register_service.assert_called_once()
            
            # Service should be running
            self.assertTrue(self.service.running)
            
            # Stop service
            await self.service.stop()
            
            # Should unregister service
            mock_zeroconf.unregister_service.assert_called_once()
            mock_zeroconf.close.assert_called_once()
            
            self.assertFalse(self.service.running)
        
        asyncio.run(run_test())
    
    @patch('zeroconf.Zeroconf')
    def test_service_registration_error_handling(self, mock_zeroconf_class):
        """Test error handling during service registration"""
        async def run_test():
            mock_zeroconf = Mock()
            mock_zeroconf.register_service.side_effect = Exception("Registration failed")
            mock_zeroconf_class.return_value = mock_zeroconf
            
            # Start should handle registration error gracefully
            try:
                await self.service.start()
            except Exception as e:
                self.fail(f"Service start should handle registration errors: {e}")
            
            # Service should not be running due to registration failure
            self.assertFalse(self.service.running)
        
        asyncio.run(run_test())
    
    def test_service_properties_configuration(self):
        """Test service properties configuration"""
        custom_properties = {
            'version': '2.0.0',
            'capabilities': 'GSR,VIDEO,THERMAL',
            'max_clients': '10',
            'sync_port': '9123'
        }
        
        service = DiscoveryService(
            service_name="Custom BucikaGSR",
            service_type="_bucika-gsr._tcp.local.",
            port=8081,
            properties=custom_properties
        )
        
        service_info = service._create_service_info()
        properties = service_info.properties
        
        # Check custom properties are included
        self.assertEqual(properties[b'version'], b'2.0.0')
        self.assertEqual(properties[b'capabilities'], b'GSR,VIDEO,THERMAL')
        self.assertEqual(properties[b'max_clients'], b'10')
        self.assertEqual(properties[b'sync_port'], b'9123')
    
    @patch('zeroconf.Zeroconf')
    def test_service_status_reporting(self, mock_zeroconf_class):
        """Test service status reporting"""
        async def run_test():
            mock_zeroconf = Mock()
            mock_zeroconf_class.return_value = mock_zeroconf
            
            # Get status before starting
            status = self.service.get_status()
            self.assertFalse(status['running'])
            self.assertIsNone(status['service_info'])
            
            # Start service and check status
            await self.service.start()
            
            status = self.service.get_status()
            self.assertTrue(status['running'])
            self.assertIsNotNone(status['service_info'])
            self.assertEqual(status['service_name'], "Test BucikaGSR PC")
            self.assertEqual(status['port'], 8080)
            
            await self.service.stop()
        
        asyncio.run(run_test())
    
    @patch('zeroconf.ServiceBrowser')
    @patch('zeroconf.Zeroconf')
    def test_service_discovery(self, mock_zeroconf_class, mock_browser_class):
        """Test discovery of other services"""
        async def run_test():
            mock_zeroconf = Mock()
            mock_browser = Mock()
            mock_zeroconf_class.return_value = mock_zeroconf
            mock_browser_class.return_value = mock_browser
            
            # Start discovery
            discovered_services = await self.service.discover_services(timeout=0.1)
            
            # Should create browser for discovery
            mock_browser_class.assert_called_once()
            
            # Should return list (may be empty in test)
            self.assertIsInstance(discovered_services, list)
        
        asyncio.run(run_test())
    
    def test_service_listener_functionality(self):
        """Test service listener for discovery events"""
        # Create listener
        listener = self.service._create_service_listener()
        
        self.assertIsNotNone(listener)
        
        # Test listener methods exist
        self.assertTrue(hasattr(listener, 'add_service'))
        self.assertTrue(hasattr(listener, 'remove_service'))
        self.assertTrue(hasattr(listener, 'update_service'))
        
        # Create mock service info
        mock_service_info = Mock()
        mock_service_info.name = "test-device._bucika-gsr._tcp.local."
        mock_service_info.addresses = [b'\xc0\xa8\x01\x64']  # 192.168.1.100
        mock_service_info.port = 8080
        mock_service_info.properties = {b'version': b'1.0.0'}
        
        # Test add service
        listener.add_service(None, "_bucika-gsr._tcp.local.", "test-device")
        
        # Should track discovered service
        self.assertIn("test-device", self.service.discovered_services)
    
    @patch('zeroconf.Zeroconf')
    def test_service_update_handling(self, mock_zeroconf_class):
        """Test handling of service property updates"""
        async def run_test():
            mock_zeroconf = Mock()
            mock_zeroconf_class.return_value = mock_zeroconf
            
            await self.service.start()
            
            # Update service properties
            new_properties = {
                'version': '1.1.0',
                'status': 'recording'
            }
            
            await self.service.update_service_properties(new_properties)
            
            # Should trigger service re-registration
            self.assertGreaterEqual(mock_zeroconf.register_service.call_count, 2)
            
            await self.service.stop()
        
        asyncio.run(run_test())
    
    def test_network_interface_detection(self):
        """Test network interface detection for service binding"""
        interfaces = self.service._get_network_interfaces()
        
        self.assertIsInstance(interfaces, list)
        
        # Should find at least loopback interface
        self.assertGreater(len(interfaces), 0)
        
        # Check interface format
        for interface in interfaces:
            self.assertIsInstance(interface, str)
            # Should be valid IP address format
            parts = interface.split('.')
            if len(parts) == 4:  # IPv4
                for part in parts:
                    self.assertLessEqual(int(part), 255)
    
    def test_service_name_uniqueness(self):
        """Test handling of service name conflicts"""
        # Create multiple services with similar names
        service1 = DiscoveryService(
            service_name="BucikaGSR PC",
            service_type="_bucika-gsr._tcp.local.",
            port=8080
        )
        
        service2 = DiscoveryService(
            service_name="BucikaGSR PC",
            service_type="_bucika-gsr._tcp.local.",
            port=8081
        )
        
        # Generate unique names
        unique_name1 = service1._ensure_unique_name("BucikaGSR PC")
        unique_name2 = service2._ensure_unique_name("BucikaGSR PC")
        
        # Names should be different when ports are different
        self.assertNotEqual(unique_name1, unique_name2)
    
    @patch('zeroconf.Zeroconf')
    def test_graceful_shutdown(self, mock_zeroconf_class):
        """Test graceful shutdown of discovery service"""
        async def run_test():
            mock_zeroconf = Mock()
            mock_zeroconf_class.return_value = mock_zeroconf
            
            await self.service.start()
            self.assertTrue(self.service.running)
            
            # Stop service
            await self.service.stop()
            
            # Should properly cleanup
            mock_zeroconf.unregister_service.assert_called_once()
            mock_zeroconf.close.assert_called_once()
            self.assertFalse(self.service.running)
            self.assertIsNone(self.service.zeroconf)
        
        asyncio.run(run_test())
    
    @patch('zeroconf.Zeroconf')
    def test_service_restart_capability(self, mock_zeroconf_class):
        """Test service restart capability"""
        async def run_test():
            mock_zeroconf = Mock()
            mock_zeroconf_class.return_value = mock_zeroconf
            
            # Start service
            await self.service.start()
            self.assertTrue(self.service.running)
            
            # Restart service
            await self.service.restart()
            
            # Should have stopped and started again
            self.assertTrue(self.service.running)
            
            # Should have called unregister and register
            mock_zeroconf.unregister_service.assert_called()
            self.assertGreaterEqual(mock_zeroconf.register_service.call_count, 2)
            
            await self.service.stop()
        
        asyncio.run(run_test())
    
    def test_service_configuration_validation(self):
        """Test validation of service configuration"""
        # Test invalid configurations
        invalid_configs = [
            {"service_name": "", "service_type": "_test._tcp.local.", "port": 8080},
            {"service_name": "Test", "service_type": "invalid_type", "port": 8080},
            {"service_name": "Test", "service_type": "_test._tcp.local.", "port": -1},
            {"service_name": "Test", "service_type": "_test._tcp.local.", "port": 70000}
        ]
        
        for config in invalid_configs:
            try:
                service = DiscoveryService(**config)
                # Should validate configuration
                is_valid = service._validate_configuration()
                self.assertFalse(is_valid)
            except (ValueError, TypeError):
                # Also acceptable to raise exception for invalid config
                pass
    
    def test_discovered_services_management(self):
        """Test management of discovered services list"""
        # Initially empty
        self.assertEqual(len(self.service.discovered_services), 0)
        
        # Add discovered service
        service_info = {
            'name': 'test-device-1',
            'address': '192.168.1.100',
            'port': 8080,
            'properties': {'version': '1.0.0'}
        }
        
        self.service._add_discovered_service('test-device-1', service_info)
        self.assertEqual(len(self.service.discovered_services), 1)
        self.assertIn('test-device-1', self.service.discovered_services)
        
        # Remove discovered service
        self.service._remove_discovered_service('test-device-1')
        self.assertEqual(len(self.service.discovered_services), 0)
        
        # Get discovered services list
        services_list = self.service.get_discovered_services()
        self.assertIsInstance(services_list, list)


if __name__ == "__main__":
    unittest.main()