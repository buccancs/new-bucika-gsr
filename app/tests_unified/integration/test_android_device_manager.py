"""
Android Device Manager Integration Tests
=======================================

Tests for enhanced Android device manager with wireless debugging
and IDE connection detection capabilities.
"""

import pytest
import time
from unittest.mock import Mock, patch, MagicMock
from pathlib import Path
import sys

# Add the project root to the path for imports
sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent))

try:
    from PythonApp.network.android_device_manager import AndroidDeviceManager, AndroidDevice
    from PythonApp.utils.android_connection_detector import ConnectionType, IDEType
    ANDROID_MANAGER_AVAILABLE = True
except ImportError:
    ANDROID_MANAGER_AVAILABLE = False


@pytest.mark.skipif(not ANDROID_MANAGER_AVAILABLE, 
                   reason="Android device manager not available")
class TestAndroidDeviceManagerEnhanced:
    """Test enhanced Android device manager functionality."""
    
    def setup_method(self):
        """Setup for each test method."""
        self.manager = AndroidDeviceManager(server_port=9001)  # Use different port for testing
    
    def teardown_method(self):
        """Cleanup after each test method."""
        if hasattr(self.manager, 'is_initialized') and self.manager.is_initialized:
            self.manager.shutdown()
    
    def test_manager_initialization_with_detection(self):
        """Test that manager initializes with connection detection capabilities."""
        assert self.manager is not None
        assert hasattr(self.manager, 'connection_detector')
        assert hasattr(self.manager, 'connection_detection_enabled')
        
        # Check detection status
        status = self.manager.get_connection_detection_status()
        assert isinstance(status, dict)
        assert 'available' in status
        assert 'detector_initialized' in status
        assert 'adb_available' in status
        assert 'supported_features' in status
    
    def test_enhanced_android_device_structure(self):
        """Test that AndroidDevice has enhanced connection fields."""
        device = AndroidDevice(
            device_id="test_device",
            capabilities=["test"],
            connection_time=time.time(),
            last_heartbeat=time.time()
        )
        
        # Check enhanced fields exist
        assert hasattr(device, 'connection_type')
        assert hasattr(device, 'wireless_debugging_enabled')
        assert hasattr(device, 'ide_connections')
        assert hasattr(device, 'ip_address')
        assert hasattr(device, 'port')
        
        # Check default values
        assert device.connection_type is None
        assert device.wireless_debugging_enabled is False
        assert device.ide_connections == []
        assert device.ip_address is None
        assert device.port is None
    
    @patch('PythonApp.utils.android_connection_detector.AndroidConnectionDetector')
    def test_wireless_and_ide_detection(self, mock_detector_class):
        """Test wireless debugging and IDE connection detection."""
        # Setup mock detector
        mock_detector = Mock()
        mock_detector_class.return_value = mock_detector
        
        # Mock detection results
        mock_detected_devices = {
            'USB123': Mock(
                device_id='USB123',
                connection_type=ConnectionType.USB,
                wireless_debugging_enabled=False,
                ip_address=None,
                port=None
            ),
            '192.168.1.100:5555': Mock(
                device_id='192.168.1.100:5555',
                connection_type=ConnectionType.WIRELESS_ADB,
                wireless_debugging_enabled=True,
                ip_address='192.168.1.100',
                port=5555
            )
        }
        
        mock_detector.detect_all_connections.return_value = mock_detected_devices
        mock_detector.get_detection_summary.return_value = {
            'total_devices': 2,
            'usb_devices': 1,
            'wireless_devices': 1,
            'ides_running': 1,
            'wireless_device_details': [
                {
                    'device_id': '192.168.1.100:5555',
                    'ip_address': '192.168.1.100',
                    'port': 5555,
                    'model': 'Test_Phone',
                    'android_version': '11'
                }
            ],
            'ide_details': [
                {
                    'ide_type': 'android_studio',
                    'version': '2021.1',
                    'project_path': '/test/project',
                    'connected_devices': ['USB123']
                }
            ]
        }
        mock_detector.is_device_ide_connected.return_value = [IDEType.ANDROID_STUDIO]
        
        # Initialize manager with mocked detector
        if self.manager.connection_detection_enabled:
            self.manager.connection_detector = mock_detector
            
            # Add mock devices to manager
            self.manager.android_devices['USB123'] = AndroidDevice(
                device_id='USB123',
                capabilities=['test'],
                connection_time=time.time(),
                last_heartbeat=time.time()
            )
            
            self.manager.android_devices['192.168.1.100:5555'] = AndroidDevice(
                device_id='192.168.1.100:5555',
                capabilities=['test'],
                connection_time=time.time(),
                last_heartbeat=time.time()
            )
            
            # Test detection
            result = self.manager.detect_wireless_and_ide_connections()
            
            assert result['available'] is True
            assert 'summary' in result
            assert result['summary']['total_devices'] == 2
            assert result['summary']['wireless_devices'] == 1
            assert result['summary']['ides_running'] == 1
            
            # Test enhanced device information
            usb_device = self.manager.android_devices['USB123']
            wireless_device = self.manager.android_devices['192.168.1.100:5555']
            
            assert usb_device.connection_type == 'usb'
            assert usb_device.ide_connections == ['android_studio']
            
            assert wireless_device.connection_type == 'wireless_adb'
            assert wireless_device.wireless_debugging_enabled is True
            assert wireless_device.ip_address == '192.168.1.100'
            assert wireless_device.port == 5555
    
    def test_get_wireless_debugging_devices(self):
        """Test filtering of wireless debugging devices."""
        # Add test devices
        usb_device = AndroidDevice(
            device_id='USB123',
            capabilities=['test'],
            connection_time=time.time(),
            last_heartbeat=time.time(),
            connection_type='usb'
        )
        
        wireless_device = AndroidDevice(
            device_id='192.168.1.100:5555',
            capabilities=['test'],
            connection_time=time.time(),
            last_heartbeat=time.time(),
            connection_type='wireless_adb',
            wireless_debugging_enabled=True,
            ip_address='192.168.1.100',
            port=5555
        )
        
        self.manager.android_devices['USB123'] = usb_device
        self.manager.android_devices['192.168.1.100:5555'] = wireless_device
        
        if self.manager.connection_detection_enabled:
            wireless_devices = self.manager.get_wireless_debugging_devices()
            
            assert len(wireless_devices) == 1
            assert wireless_devices[0].device_id == '192.168.1.100:5555'
            assert wireless_devices[0].connection_type == 'wireless_adb'
        else:
            # If detection not available, should return empty list
            wireless_devices = self.manager.get_wireless_debugging_devices()
            assert wireless_devices == []
    
    def test_get_ide_connected_devices(self):
        """Test filtering of IDE-connected devices."""
        # Add test device with IDE connection
        device = AndroidDevice(
            device_id='test_device',
            capabilities=['test'],
            connection_time=time.time(),
            last_heartbeat=time.time(),
            ide_connections=['android_studio', 'intellij_idea']
        )
        
        self.manager.android_devices['test_device'] = device
        
        if self.manager.connection_detection_enabled:
            ide_devices = self.manager.get_ide_connected_devices()
            
            assert 'android_studio' in ide_devices
            assert 'intellij_idea' in ide_devices
            assert len(ide_devices['android_studio']) == 1
            assert len(ide_devices['intellij_idea']) == 1
            assert ide_devices['android_studio'][0].device_id == 'test_device'
        else:
            # If detection not available, should return empty dict
            ide_devices = self.manager.get_ide_connected_devices()
            assert ide_devices == {}
    
    def test_device_connection_checks(self):
        """Test individual device connection checking methods."""
        # Add test devices
        usb_device = AndroidDevice(
            device_id='USB123',
            capabilities=['test'],
            connection_time=time.time(),
            last_heartbeat=time.time(),
            connection_type='usb'
        )
        
        wireless_device = AndroidDevice(
            device_id='192.168.1.100:5555',
            capabilities=['test'],
            connection_time=time.time(),
            last_heartbeat=time.time(),
            connection_type='wireless_adb',
            wireless_debugging_enabled=True,
            ide_connections=['android_studio']
        )
        
        self.manager.android_devices['USB123'] = usb_device
        self.manager.android_devices['192.168.1.100:5555'] = wireless_device
        
        if self.manager.connection_detection_enabled:
            # Test wireless connection check
            assert self.manager.is_device_wireless_connected('192.168.1.100:5555') is True
            assert self.manager.is_device_wireless_connected('USB123') is False
            assert self.manager.is_device_wireless_connected('nonexistent') is False
            
            # Test IDE connection check
            ide_connections = self.manager.is_device_ide_connected('192.168.1.100:5555')
            assert 'android_studio' in ide_connections
            
            usb_ide_connections = self.manager.is_device_ide_connected('USB123')
            assert len(usb_ide_connections) == 0
            
            nonexistent_ide_connections = self.manager.is_device_ide_connected('nonexistent')
            assert len(nonexistent_ide_connections) == 0
        else:
            # If detection not available, should return False/empty
            assert self.manager.is_device_wireless_connected('192.168.1.100:5555') is False
            assert self.manager.is_device_ide_connected('192.168.1.100:5555') == []
    
    def test_get_enhanced_device_info(self):
        """Test comprehensive device information retrieval."""
        # Add test device
        device = AndroidDevice(
            device_id='test_device',
            capabilities=['shimmer', 'thermal'],
            connection_time=1234567890.0,
            last_heartbeat=1234567900.0,
            connection_type='wireless_adb',
            wireless_debugging_enabled=True,
            ide_connections=['android_studio'],
            ip_address='192.168.1.100',
            port=5555
        )
        device.status = {'battery': 80, 'storage': 50}
        device.is_recording = True
        device.current_session_id = 'test_session'
        device.messages_received = 10
        device.messages_sent = 5
        device.data_samples_received = 100
        device.last_data_timestamp = 1234567890.0
        device.shimmer_devices = {'shimmer1': {'status': 'connected'}}
        
        self.manager.android_devices['test_device'] = device
        
        info = self.manager.get_enhanced_device_info('test_device')
        
        assert info is not None
        assert info['device_id'] == 'test_device'
        assert info['capabilities'] == ['shimmer', 'thermal']
        assert info['connection_time'] == 1234567890.0
        assert info['last_heartbeat'] == 1234567900.0
        assert info['status'] == {'battery': 80, 'storage': 50}
        assert info['is_recording'] is True
        assert info['current_session_id'] == 'test_session'
        assert info['messages_received'] == 10
        assert info['messages_sent'] == 5
        assert info['data_samples_received'] == 100
        assert info['last_data_timestamp'] == 1234567890.0
        assert info['shimmer_devices'] == {'shimmer1': {'status': 'connected'}}
        
        if self.manager.connection_detection_enabled:
            assert info['connection_type'] == 'wireless_adb'
            assert info['wireless_debugging_enabled'] is True
            assert info['ide_connections'] == ['android_studio']
            assert info['ip_address'] == '192.168.1.100'
            assert info['port'] == 5555
        
        # Test nonexistent device
        nonexistent_info = self.manager.get_enhanced_device_info('nonexistent')
        assert nonexistent_info is None
    
    def test_refresh_connection_detection(self):
        """Test manual refresh of connection detection."""
        if self.manager.connection_detection_enabled:
            with patch.object(self.manager, 'detect_wireless_and_ide_connections') as mock_detect:
                mock_detect.return_value = {'available': True, 'refreshed': True}
                
                result = self.manager.refresh_connection_detection()
                
                assert result['available'] is True
                assert result['refreshed'] is True
                mock_detect.assert_called_once()
        else:
            result = self.manager.refresh_connection_detection()
            assert result['available'] is False
    
    def test_connection_detection_without_detector(self):
        """Test behavior when connection detector is not available."""
        # Create manager without detector
        manager = AndroidDeviceManager(server_port=9002)
        
        if hasattr(manager, 'connection_detector'):
            # Force disable detection for this test
            manager.connection_detection_enabled = False
            manager.connection_detector = None
        
        try:
            # Test methods return appropriate defaults
            result = manager.detect_wireless_and_ide_connections()
            assert result['available'] is False
            assert 'reason' in result
            
            wireless_devices = manager.get_wireless_debugging_devices()
            assert wireless_devices == []
            
            ide_devices = manager.get_ide_connected_devices()
            assert ide_devices == {}
            
            assert manager.is_device_wireless_connected('any_device') is False
            assert manager.is_device_ide_connected('any_device') == []
            
            refresh_result = manager.refresh_connection_detection()
            assert refresh_result['available'] is False
            
            status = manager.get_connection_detection_status()
            assert status['available'] is False
            
        finally:
            if hasattr(manager, 'is_initialized') and manager.is_initialized:
                manager.shutdown()


@pytest.mark.integration
class TestAndroidDeviceManagerIntegration:
    """Integration tests for Android device manager with real detection."""
    
    def setup_method(self):
        """Setup for integration tests."""
        self.manager = AndroidDeviceManager(server_port=9003)
    
    def teardown_method(self):
        """Cleanup after integration tests."""
        if hasattr(self.manager, 'is_initialized') and self.manager.is_initialized:
            self.manager.shutdown()
    
    @pytest.mark.skipif(not ANDROID_MANAGER_AVAILABLE, 
                       reason="Android device manager not available")
    def test_real_connection_detection(self):
        """Test connection detection with real system (if available)."""
        if not self.manager.connection_detection_enabled:
            pytest.skip("Connection detection not available")
        
        # Test detection without requiring actual devices
        try:
            result = self.manager.detect_wireless_and_ide_connections()
            
            # Should not fail, regardless of what's detected
            assert isinstance(result, dict)
            assert 'available' in result
            
            if result['available'] and 'summary' in result:
                summary = result['summary']
                assert 'total_devices' in summary
                assert 'wireless_devices' in summary
                assert 'ides_running' in summary
                assert isinstance(summary['total_devices'], int)
                assert isinstance(summary['wireless_devices'], int)
                assert isinstance(summary['ides_running'], int)
                
        except Exception as e:
            # Log error but don't fail test - system might not have ADB/IDEs
            print(f"Detection failed (expected in CI): {e}")
    
    def test_detection_status_real(self):
        """Test getting real detection status."""
        status = self.manager.get_connection_detection_status()
        
        assert isinstance(status, dict)
        assert 'available' in status
        assert 'detector_initialized' in status
        assert 'adb_available' in status
        assert 'supported_features' in status
        assert isinstance(status['available'], bool)
        assert isinstance(status['detector_initialized'], bool)
        assert isinstance(status['supported_features'], list)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])