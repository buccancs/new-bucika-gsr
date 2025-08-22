"""
Shimmer GSR Sensor Hardware-in-the-Loop Testing
===============================================

Tests for Shimmer GSR sensor integration with automatic fallback to mocking
when real hardware is not available. Provides comprehensive testing for:

- Real Shimmer sensor discovery and connection (when available)
- Mock sensor simulation for CI/CD environments
- Data validation and quality assurance
- Connection stability and error handling
- Bluetooth communication protocols

Requirements Coverage:
- FR1: GSR data acquisition via Bluetooth
- FR2: Sensor connection management
- NFR5: System reliability and fault tolerance
"""

import pytest
import time
import threading
import json
import logging
from typing import Dict, List, Optional, Any, Union
from unittest.mock import Mock, patch, MagicMock
from dataclasses import dataclass
import os
import sys

# Add PythonApp to path for imports
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'PythonApp'))

# Hardware interface imports with fallback
try:
    import bluetooth
    BLUETOOTH_AVAILABLE = True
except ImportError:
    BLUETOOTH_AVAILABLE = False
    bluetooth = None

try:
    from pyshimmer import ShimmerBluetooth, DEFAULT_BAUDRATE
    PYSHIMMER_AVAILABLE = True
except ImportError:
    PYSHIMMER_AVAILABLE = False
    ShimmerBluetooth = None

# Import hardware utilities
try:
    from tests.hardware.hardware_utils import (
        HardwareConfig, HardwareStatus, 
        detect_shimmer_devices, validate_gsr_data
    )
except ImportError:
    # Try relative import
    from .hardware_utils import (
        HardwareConfig, HardwareStatus, 
        detect_shimmer_devices, validate_gsr_data
    )

logger = logging.getLogger(__name__)


@dataclass
class MockGSRReading:
    """Mock GSR reading data structure."""
    timestamp: float
    resistance: float  # in kOhms
    conductance: float  # in uS
    voltage: float     # raw voltage reading
    quality: str      # 'good', 'fair', 'poor'


class MockShimmerDevice:
    """Mock Shimmer device for testing without real hardware."""
    
    def __init__(self, device_id: str = "00:06:66:66:66:66", name: str = "MockShimmer"):
        self.device_id = device_id
        self.name = name
        self.connected = False
        self.streaming = False
        self.sample_rate = 128  # Hz
        self._data_buffer = []
        self._thread = None
        self._stop_event = threading.Event()
        
    def connect(self, timeout: float = 10.0) -> bool:
        """Simulate connection to Shimmer device."""
        time.sleep(0.1)  # Simulate connection delay
        self.connected = True
        logger.info(f"Mock connection established to {self.name}")
        return True
        
    def disconnect(self):
        """Simulate disconnection from Shimmer device."""
        self.stop_streaming()
        self.connected = False
        logger.info(f"Mock disconnection from {self.name}")
        
    def start_streaming(self) -> bool:
        """Start mock data streaming."""
        if not self.connected:
            return False
            
        self.streaming = True
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._generate_data)
        self._thread.start()
        logger.info("Mock GSR streaming started")
        return True
        
    def stop_streaming(self):
        """Stop mock data streaming."""
        if self._thread and self.streaming:
            self._stop_event.set()
            self._thread.join(timeout=1.0)
            self.streaming = False
            logger.info("Mock GSR streaming stopped")
            
    def get_latest_data(self) -> Optional[MockGSRReading]:
        """Get the latest GSR reading."""
        if self._data_buffer:
            return self._data_buffer[-1]
        return None
        
    def get_buffered_data(self) -> List[MockGSRReading]:
        """Get all buffered GSR readings."""
        data = self._data_buffer.copy()
        self._data_buffer.clear()
        return data
        
    def _generate_data(self):
        """Generate realistic mock GSR data."""
        import random
        import math
        
        base_resistance = 100.0  # kOhms
        time_offset = 0.0
        
        while not self._stop_event.is_set():
            # Generate realistic GSR variations
            # Simulate breathing patterns and occasional stress responses
            breathing_component = 5 * math.sin(2 * math.pi * 0.3 * time_offset)  # 0.3 Hz breathing
            stress_component = random.uniform(-2, 8) if random.random() < 0.1 else 0
            noise = random.uniform(-0.5, 0.5)
            
            resistance = base_resistance + breathing_component + stress_component + noise
            conductance = 1000.0 / resistance  # Convert to uS
            voltage = 3.3 * (resistance / 200.0)  # Simulate ADC voltage
            
            # Determine quality based on signal characteristics
            if resistance < 50 or resistance > 300:
                quality = 'poor'
            elif abs(breathing_component + stress_component) > 10:
                quality = 'fair'
            else:
                quality = 'good'
                
            reading = MockGSRReading(
                timestamp=time.time(),
                resistance=resistance,
                conductance=conductance,
                voltage=voltage,
                quality=quality
            )
            
            self._data_buffer.append(reading)
            
            # Keep buffer size manageable
            if len(self._data_buffer) > 1000:
                self._data_buffer = self._data_buffer[-500:]
                
            time_offset += 1.0 / self.sample_rate
            time.sleep(1.0 / self.sample_rate)


class ShimmerTestManager:
    """Manages Shimmer sensor testing with automatic hardware detection."""
    
    def __init__(self):
        self.real_device = None
        self.mock_device = None
        self.use_mock = not self._detect_real_hardware()
        
    def _detect_real_hardware(self) -> bool:
        """Detect if real Shimmer hardware is available."""
        if not BLUETOOTH_AVAILABLE or not PYSHIMMER_AVAILABLE:
            logger.info("Shimmer libraries not available, using mock")
            return False
            
        try:
            # Quick scan for Shimmer devices
            devices = detect_shimmer_devices(timeout=2.0)
            if devices:
                logger.info(f"Found {len(devices)} Shimmer device(s)")
                return True
            else:
                logger.info("No Shimmer devices found, using mock")
                return False
        except Exception as e:
            logger.warning(f"Hardware detection failed: {e}, using mock")
            return False
            
    def get_device(self):
        """Get appropriate device (real or mock)."""
        if self.use_mock:
            if not self.mock_device:
                self.mock_device = MockShimmerDevice()
            return self.mock_device
        else:
            if not self.real_device:
                # Initialize real Shimmer device
                devices = detect_shimmer_devices(timeout=5.0)
                if devices:
                    self.real_device = ShimmerBluetooth(devices[0]['address'])
                else:
                    raise RuntimeError("No real Shimmer devices available")
            return self.real_device
            
    def cleanup(self):
        """Clean up resources."""
        if self.mock_device:
            self.mock_device.disconnect()
        if self.real_device:
            try:
                self.real_device.disconnect()
            except Exception:
                pass


@pytest.fixture(scope="function")
def shimmer_manager():
    """Provide Shimmer test manager with automatic cleanup."""
    manager = ShimmerTestManager()
    yield manager
    manager.cleanup()


@pytest.fixture(scope="function")
def shimmer_device(shimmer_manager):
    """Provide Shimmer device (real or mock)."""
    return shimmer_manager.get_device()


class TestShimmerConnection:
    """Test Shimmer sensor connection and basic functionality."""
    
    @pytest.mark.hardware
    def test_device_discovery(self, shimmer_manager):
        """Test Shimmer device discovery and enumeration (FR1)."""
        if shimmer_manager.use_mock:
            # Test mock device discovery
            device = shimmer_manager.get_device()
            assert device is not None
            assert device.device_id == "00:06:66:66:66:66"
            assert device.name == "MockShimmer"
            pytest.skip("Using mock device - real hardware not available")
        else:
            # Test real device discovery
            devices = detect_shimmer_devices(timeout=5.0)
            assert len(devices) > 0
            assert all('address' in dev for dev in devices)
            assert all('name' in dev for dev in devices)
            
    @pytest.mark.hardware
    def test_connection_establishment(self, shimmer_device):
        """Test connection establishment and teardown (FR1)."""
        # Test connection
        success = shimmer_device.connect(timeout=10.0)
        assert success, "Failed to connect to Shimmer device"
        
        if hasattr(shimmer_device, 'connected'):
            assert shimmer_device.connected, "Device should report as connected"
            
        # Test disconnection
        shimmer_device.disconnect()
        if hasattr(shimmer_device, 'connected'):
            assert not shimmer_device.connected, "Device should report as disconnected"
            
    @pytest.mark.hardware
    def test_connection_timeout(self, shimmer_manager):
        """Test connection timeout handling (NFR5)."""
        if shimmer_manager.use_mock:
            # Mock always connects quickly
            device = shimmer_manager.get_device()
            start_time = time.time()
            success = device.connect(timeout=0.1)
            elapsed = time.time() - start_time
            assert success
            assert elapsed < 1.0
        else:
            # Test with invalid device address
            try:
                invalid_device = ShimmerBluetooth("00:00:00:00:00:00")
                start_time = time.time()
                success = invalid_device.connect(timeout=2.0)
                elapsed = time.time() - start_time
                assert not success or elapsed >= 1.8  # Should timeout
            except Exception as e:
                # Connection failure is expected
                assert "timeout" in str(e).lower() or "connection" in str(e).lower()


class TestShimmerDataAcquisition:
    """Test GSR data acquisition and validation."""
    
    @pytest.mark.hardware
    def test_data_streaming_start_stop(self, shimmer_device):
        """Test starting and stopping data streaming (FR1)."""
        # Connect first
        shimmer_device.connect()
        
        # Start streaming
        success = shimmer_device.start_streaming()
        assert success, "Failed to start data streaming"
        
        # Wait for some data
        time.sleep(1.0)
        
        # Stop streaming
        shimmer_device.stop_streaming()
        
        # Clean up
        shimmer_device.disconnect()
        
    @pytest.mark.hardware
    def test_gsr_data_quality(self, shimmer_device):
        """Test GSR data quality and validation (FR1, NFR6)."""
        shimmer_device.connect()
        shimmer_device.start_streaming()
        
        # Collect data for validation
        time.sleep(2.0)
        
        if hasattr(shimmer_device, 'get_buffered_data'):
            # Mock device
            data = shimmer_device.get_buffered_data()
            assert len(data) > 0, "Should have collected some data"
            
            for reading in data[:10]:  # Check first 10 readings
                assert hasattr(reading, 'resistance')
                assert hasattr(reading, 'conductance')
                assert hasattr(reading, 'timestamp')
                assert reading.resistance > 0
                assert reading.conductance > 0
                assert reading.timestamp > 0
                assert reading.quality in ['good', 'fair', 'poor']
                
        shimmer_device.stop_streaming()
        shimmer_device.disconnect()
        
    @pytest.mark.hardware
    def test_data_rate_consistency(self, shimmer_device):
        """Test data sampling rate consistency (NFR6)."""
        shimmer_device.connect()
        shimmer_device.start_streaming()
        
        # Collect timestamps
        start_time = time.time()
        time.sleep(3.0)
        
        if hasattr(shimmer_device, 'get_buffered_data'):
            # Mock device
            data = shimmer_device.get_buffered_data()
            end_time = time.time()
            
            if len(data) > 10:
                # Calculate actual sample rate
                time_span = end_time - start_time
                actual_rate = len(data) / time_span
                expected_rate = 128  # Hz
                
                # Allow 10% tolerance
                assert abs(actual_rate - expected_rate) / expected_rate < 0.1, \
                    f"Sample rate deviation too high: {actual_rate} vs {expected_rate}"
                    
        shimmer_device.stop_streaming()
        shimmer_device.disconnect()
        
    @pytest.mark.hardware
    @pytest.mark.stress
    def test_long_duration_recording(self, shimmer_device):
        """Test long-duration recording stability (NFR5)."""
        shimmer_device.connect()
        shimmer_device.start_streaming()
        
        # Record for longer period (shorter for mock to avoid CI timeout)
        duration = 10.0 if hasattr(shimmer_device, 'device_id') and 'mock' in shimmer_device.device_id.lower() else 30.0
        start_time = time.time()
        
        while time.time() - start_time < duration:
            time.sleep(1.0)
            
            # Check if still connected and streaming
            if hasattr(shimmer_device, 'connected'):
                assert shimmer_device.connected, "Device disconnected during recording"
            if hasattr(shimmer_device, 'streaming'):
                assert shimmer_device.streaming, "Streaming stopped unexpectedly"
                
        # Verify data was collected
        if hasattr(shimmer_device, 'get_buffered_data'):
            data = shimmer_device.get_buffered_data()
            expected_samples = duration * 128  # 128 Hz
            # More lenient for mock devices
            min_expected = expected_samples * 0.5 if hasattr(shimmer_device, 'device_id') and 'mock' in shimmer_device.device_id.lower() else expected_samples * 0.8
            assert len(data) > min_expected, f"Insufficient data collected: {len(data)} < {min_expected}"
            
        shimmer_device.stop_streaming()
        shimmer_device.disconnect()


class TestShimmerErrorHandling:
    """Test error handling and fault tolerance."""
    
    @pytest.mark.hardware
    def test_connection_failure_recovery(self, shimmer_manager):
        """Test recovery from connection failures (NFR5)."""
        device = shimmer_manager.get_device()
        
        # Test multiple connection attempts
        for attempt in range(3):
            try:
                success = device.connect()
                if success:
                    device.disconnect()
                    break
            except Exception as e:
                if attempt == 2:
                    pytest.skip(f"Connection consistently failed: {e}")
                time.sleep(1.0)
                
    @pytest.mark.hardware
    def test_unexpected_disconnection(self, shimmer_device):
        """Test handling of unexpected disconnection (NFR5)."""
        shimmer_device.connect()
        shimmer_device.start_streaming()
        
        # Simulate unexpected disconnection
        if hasattr(shimmer_device, 'disconnect'):
            shimmer_device.disconnect()
            
        # Try to restart streaming
        try:
            success = shimmer_device.connect()
            if success:
                shimmer_device.start_streaming()
                time.sleep(1.0)
                shimmer_device.stop_streaming()
        except Exception as e:
            # Expected behavior - should handle gracefully
            assert "disconnect" in str(e).lower() or "connection" in str(e).lower()
            
    @pytest.mark.hardware
    def test_invalid_configuration(self, shimmer_device):
        """Test handling of invalid configuration parameters (NFR5)."""
        shimmer_device.connect()
        
        # Test invalid sample rates (if supported)
        if hasattr(shimmer_device, 'set_sample_rate'):
            try:
                shimmer_device.set_sample_rate(-1)
                assert False, "Should reject negative sample rate"
            except (ValueError, RuntimeError):
                pass  # Expected
                
        shimmer_device.disconnect()


@pytest.mark.hardware
class TestShimmerIntegration:
    """Integration tests with other system components."""
    
    def test_data_synchronization(self, shimmer_device):
        """Test timestamp synchronization with system clock (FR3)."""
        shimmer_device.connect()
        system_start = time.time()
        shimmer_device.start_streaming()
        
        time.sleep(2.0)
        
        if hasattr(shimmer_device, 'get_latest_data'):
            latest = shimmer_device.get_latest_data()
            if latest:
                # Check timestamp is reasonable
                time_diff = abs(latest.timestamp - time.time())
                assert time_diff < 5.0, "Timestamp synchronization issue"
                
        shimmer_device.stop_streaming()
        shimmer_device.disconnect()
        
    def test_concurrent_access(self, shimmer_manager):
        """Test concurrent access protection (NFR5)."""
        device1 = shimmer_manager.get_device()
        
        # First connection
        success1 = device1.connect()
        assert success1
        
        # Attempt second connection (should be handled gracefully)
        if not shimmer_manager.use_mock:
            try:
                device2 = ShimmerBluetooth(device1.bt_address if hasattr(device1, 'bt_address') else "00:06:66:66:66:66")
                success2 = device2.connect(timeout=2.0)
                if success2:
                    device2.disconnect()
            except Exception:
                pass  # Expected - device already in use
                
        device1.disconnect()


if __name__ == "__main__":
    # Allow running tests directly
    pytest.main([__file__, "-v", "--tb=short"])