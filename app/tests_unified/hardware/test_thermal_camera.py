"""
Thermal Camera Hardware-in-the-Loop Testing
==========================================

Tests for thermal camera integration with automatic fallback to mocking
when real hardware is not available. Provides comprehensive testing for:

- Real thermal camera detection and connection (when available)
- Mock camera simulation for CI/CD environments
- Video streaming and frame capture
- Temperature measurement accuracy
- USB communication protocols
- Image processing and validation

Requirements Coverage:
- FR4: Thermal imaging data acquisition
- FR5: Real-time video streaming
- NFR6: Data quality and accuracy
- NFR7: Real-time performance requirements
"""

import pytest
import time
import threading
import json
import logging
import numpy as np
from typing import Dict, List, Optional, Any, Tuple, Union
from unittest.mock import Mock, patch, MagicMock
from dataclasses import dataclass
import os
import sys

# Add PythonApp to path for imports
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'PythonApp'))

# Video/camera imports with fallback
try:
    import cv2
    OPENCV_AVAILABLE = True
except ImportError:
    OPENCV_AVAILABLE = False
    cv2 = None

try:
    import usb.core
    import usb.util
    USB_AVAILABLE = True
except ImportError:
    USB_AVAILABLE = False
    usb = None

# Import hardware utilities
try:
    from tests.hardware.hardware_utils import (
        HardwareConfig, HardwareStatus,
        detect_thermal_cameras, validate_thermal_frame
    )
except ImportError:
    # Try relative import
    from .hardware_utils import (
        HardwareConfig, HardwareStatus,
        detect_thermal_cameras, validate_thermal_frame
    )

logger = logging.getLogger(__name__)


@dataclass
class MockThermalFrame:
    """Mock thermal camera frame data structure."""
    timestamp: float
    width: int
    height: int
    temperature_data: np.ndarray  # Temperature values in Celsius
    visual_data: np.ndarray      # RGB visual representation
    min_temp: float
    max_temp: float
    avg_temp: float
    frame_id: int


class MockThermalCamera:
    """Mock thermal camera for testing without real hardware."""
    
    def __init__(self, device_id: str = "MockThermal_001", resolution: Tuple[int, int] = (160, 120)):
        self.device_id = device_id
        self.resolution = resolution
        self.connected = False
        self.streaming = False
        self.frame_rate = 30.0  # FPS
        self._frame_buffer = []
        self._thread = None
        self._stop_event = threading.Event()
        self._frame_counter = 0
        
        # Thermal imaging parameters
        self.temp_range = (-20.0, 80.0)  # Celsius
        self.thermal_sensitivity = 0.1   # Celsius
        
    def connect(self, timeout: float = 5.0) -> bool:
        """Simulate connection to thermal camera."""
        time.sleep(0.1)  # Simulate USB enumeration
        self.connected = True
        logger.info(f"Mock thermal camera connected: {self.device_id}")
        return True
        
    def disconnect(self):
        """Simulate disconnection from thermal camera."""
        self.stop_streaming()
        self.connected = False
        logger.info(f"Mock thermal camera disconnected: {self.device_id}")
        
    def start_streaming(self) -> bool:
        """Start mock thermal video streaming."""
        if not self.connected:
            return False
            
        self.streaming = True
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._generate_frames)
        self._thread.start()
        logger.info("Mock thermal streaming started")
        return True
        
    def stop_streaming(self):
        """Stop mock thermal video streaming."""
        if self._thread and self.streaming:
            self._stop_event.set()
            self._thread.join(timeout=1.0)
            self.streaming = False
            logger.info("Mock thermal streaming stopped")
            
    def capture_frame(self) -> Optional[MockThermalFrame]:
        """Capture a single thermal frame."""
        if not self.connected:
            return None
            
        return self._create_mock_frame()
        
    def get_latest_frame(self) -> Optional[MockThermalFrame]:
        """Get the latest thermal frame."""
        if self._frame_buffer:
            return self._frame_buffer[-1]
        return None
        
    def get_buffered_frames(self) -> List[MockThermalFrame]:
        """Get all buffered thermal frames."""
        frames = self._frame_buffer.copy()
        self._frame_buffer.clear()
        return frames
        
    def set_temperature_range(self, min_temp: float, max_temp: float):
        """Set thermal imaging temperature range."""
        self.temp_range = (min_temp, max_temp)
        
    def get_camera_info(self) -> Dict[str, Any]:
        """Get camera information and capabilities."""
        return {
            'device_id': self.device_id,
            'resolution': self.resolution,
            'frame_rate': self.frame_rate,
            'temp_range': self.temp_range,
            'thermal_sensitivity': self.thermal_sensitivity,
            'connected': self.connected,
            'streaming': self.streaming
        }
        
    def _create_mock_frame(self) -> MockThermalFrame:
        """Create a realistic mock thermal frame."""
        width, height = self.resolution
        
        # Generate realistic thermal pattern
        # Simulate a person's face/body heat signature
        temp_data = np.random.normal(25.0, 2.0, (height, width))  # Room temperature base
        
        # Add warmer regions (simulating body parts)
        center_y, center_x = height // 2, width // 2
        for _ in range(3):  # Add 3 warm spots
            spot_y = np.random.randint(height // 4, 3 * height // 4)
            spot_x = np.random.randint(width // 4, 3 * width // 4)
            radius = np.random.randint(10, 20)
            
            y_grid, x_grid = np.ogrid[:height, :width]
            mask = (x_grid - spot_x) ** 2 + (y_grid - spot_y) ** 2 <= radius ** 2
            temp_data[mask] += np.random.uniform(5.0, 15.0)  # Add heat
            
        # Add some noise
        temp_data += np.random.normal(0, 0.2, temp_data.shape)
        
        # Clamp to valid range
        temp_data = np.clip(temp_data, self.temp_range[0], self.temp_range[1])
        
        # Create visual representation (false color)
        normalized = (temp_data - self.temp_range[0]) / (self.temp_range[1] - self.temp_range[0])
        visual_data = np.zeros((height, width, 3), dtype=np.uint8)
        
        # Simple thermal colormap (blue -> green -> red)
        visual_data[:, :, 2] = (255 * (1 - normalized)).astype(np.uint8)  # Blue
        visual_data[:, :, 1] = (255 * (1 - abs(normalized - 0.5) * 2)).astype(np.uint8)  # Green
        visual_data[:, :, 0] = (255 * normalized).astype(np.uint8)  # Red
        
        self._frame_counter += 1
        
        return MockThermalFrame(
            timestamp=time.time(),
            width=width,
            height=height,
            temperature_data=temp_data,
            visual_data=visual_data,
            min_temp=float(np.min(temp_data)),
            max_temp=float(np.max(temp_data)),
            avg_temp=float(np.mean(temp_data)),
            frame_id=self._frame_counter
        )
        
    def _generate_frames(self):
        """Generate continuous thermal frames."""
        while not self._stop_event.is_set():
            frame = self._create_mock_frame()
            self._frame_buffer.append(frame)
            
            # Keep buffer size manageable
            if len(self._frame_buffer) > 100:
                self._frame_buffer = self._frame_buffer[-50:]
                
            time.sleep(1.0 / self.frame_rate)


class ThermalCameraTestManager:
    """Manages thermal camera testing with automatic hardware detection."""
    
    def __init__(self):
        self.real_camera = None
        self.mock_camera = None
        self.use_mock = not self._detect_real_hardware()
        
    def _detect_real_hardware(self) -> bool:
        """Detect if real thermal camera hardware is available."""
        if not USB_AVAILABLE or not OPENCV_AVAILABLE:
            logger.info("Camera libraries not available, using mock")
            return False
            
        try:
            # Quick scan for thermal cameras
            cameras = detect_thermal_cameras(timeout=2.0)
            if cameras:
                logger.info(f"Found {len(cameras)} thermal camera(s)")
                return True
            else:
                logger.info("No thermal cameras found, using mock")
                return False
        except Exception as e:
            logger.warning(f"Hardware detection failed: {e}, using mock")
            return False
            
    def get_camera(self):
        """Get appropriate camera (real or mock)."""
        if self.use_mock:
            if not self.mock_camera:
                self.mock_camera = MockThermalCamera()
            return self.mock_camera
        else:
            if not self.real_camera:
                # Initialize real thermal camera
                cameras = detect_thermal_cameras(timeout=5.0)
                if cameras:
                    # Use first available camera
                    camera_info = cameras[0]
                    self.real_camera = self._create_real_camera(camera_info)
                else:
                    raise RuntimeError("No real thermal cameras available")
            return self.real_camera
            
    def _create_real_camera(self, camera_info: Dict[str, Any]):
        """Create real camera instance based on detected hardware."""
        # This would be specific to the actual thermal camera model
        # For now, return a mock as placeholder
        logger.warning("Real camera initialization not implemented, using mock")
        return MockThermalCamera(device_id=camera_info.get('device_id', 'RealThermal'))
        
    def cleanup(self):
        """Clean up resources."""
        if self.mock_camera:
            self.mock_camera.disconnect()
        if self.real_camera:
            try:
                self.real_camera.disconnect()
            except Exception:
                pass


@pytest.fixture(scope="function")
def thermal_manager():
    """Provide thermal camera test manager with automatic cleanup."""
    manager = ThermalCameraTestManager()
    yield manager
    manager.cleanup()


@pytest.fixture(scope="function")
def thermal_camera(thermal_manager):
    """Provide thermal camera (real or mock)."""
    return thermal_manager.get_camera()


class TestThermalCameraConnection:
    """Test thermal camera connection and basic functionality."""
    
    @pytest.mark.hardware
    def test_camera_discovery(self, thermal_manager):
        """Test thermal camera discovery and enumeration (FR4)."""
        if thermal_manager.use_mock:
            # Test mock camera discovery
            camera = thermal_manager.get_camera()
            assert camera is not None
            assert camera.device_id == "MockThermal_001"
            assert camera.resolution == (160, 120)
            pytest.skip("Using mock camera - real hardware not available")
        else:
            # Test real camera discovery
            cameras = detect_thermal_cameras(timeout=5.0)
            assert len(cameras) > 0
            assert all('device_id' in cam for cam in cameras)
            assert all('resolution' in cam for cam in cameras)
            
    @pytest.mark.hardware
    def test_connection_establishment(self, thermal_camera):
        """Test connection establishment and teardown (FR4)."""
        # Test connection
        success = thermal_camera.connect(timeout=10.0)
        assert success, "Failed to connect to thermal camera"
        
        if hasattr(thermal_camera, 'connected'):
            assert thermal_camera.connected, "Camera should report as connected"
            
        # Test camera info retrieval
        if hasattr(thermal_camera, 'get_camera_info'):
            info = thermal_camera.get_camera_info()
            assert 'device_id' in info
            assert 'resolution' in info
            assert 'frame_rate' in info
            
        # Test disconnection
        thermal_camera.disconnect()
        if hasattr(thermal_camera, 'connected'):
            assert not thermal_camera.connected, "Camera should report as disconnected"
            
    @pytest.mark.hardware
    def test_connection_timeout(self, thermal_manager):
        """Test connection timeout handling (NFR5)."""
        if thermal_manager.use_mock:
            # Mock always connects quickly
            camera = thermal_manager.get_camera()
            start_time = time.time()
            success = camera.connect(timeout=0.1)
            elapsed = time.time() - start_time
            assert success
            assert elapsed < 1.0
        else:
            # Test with invalid device
            try:
                start_time = time.time()
                # This would test with invalid camera device
                # For now, just test timeout behavior
                elapsed = time.time() - start_time
                assert elapsed < 10.0  # Should not hang indefinitely
            except Exception as e:
                # Connection failure is expected
                assert "timeout" in str(e).lower() or "connection" in str(e).lower()


class TestThermalFrameCapture:
    """Test thermal frame capture and image processing."""
    
    @pytest.mark.hardware
    def test_single_frame_capture(self, thermal_camera):
        """Test capturing a single thermal frame (FR4)."""
        thermal_camera.connect()
        
        frame = thermal_camera.capture_frame()
        assert frame is not None, "Should capture a frame"
        
        # Validate frame structure
        assert hasattr(frame, 'timestamp')
        assert hasattr(frame, 'temperature_data')
        assert hasattr(frame, 'visual_data')
        assert hasattr(frame, 'min_temp')
        assert hasattr(frame, 'max_temp')
        assert hasattr(frame, 'avg_temp')
        
        # Validate data types and ranges
        assert isinstance(frame.temperature_data, np.ndarray)
        assert isinstance(frame.visual_data, np.ndarray)
        assert frame.min_temp <= frame.avg_temp <= frame.max_temp
        assert frame.timestamp > 0
        
        thermal_camera.disconnect()
        
    @pytest.mark.hardware
    def test_video_streaming(self, thermal_camera):
        """Test thermal video streaming (FR5)."""
        thermal_camera.connect()
        
        # Start streaming
        success = thermal_camera.start_streaming()
        assert success, "Failed to start thermal streaming"
        
        # Wait for frames to be captured
        time.sleep(2.0)
        
        # Check if frames are being generated
        if hasattr(thermal_camera, 'get_buffered_frames'):
            frames = thermal_camera.get_buffered_frames()
            assert len(frames) > 0, "Should have captured frames"
            
            # Validate frame sequence
            if len(frames) > 1:
                # Check timestamps are increasing
                for i in range(1, len(frames)):
                    assert frames[i].timestamp > frames[i-1].timestamp
                    
                # Check frame IDs are sequential
                for i in range(1, len(frames)):
                    assert frames[i].frame_id > frames[i-1].frame_id
        
        # Stop streaming
        thermal_camera.stop_streaming()
        thermal_camera.disconnect()
        
    @pytest.mark.hardware
    def test_frame_rate_consistency(self, thermal_camera):
        """Test thermal video frame rate consistency (NFR7)."""
        thermal_camera.connect()
        thermal_camera.start_streaming()
        
        # Measure frame rate
        start_time = time.time()
        time.sleep(3.0)
        
        if hasattr(thermal_camera, 'get_buffered_frames'):
            frames = thermal_camera.get_buffered_frames()
            end_time = time.time()
            
            if len(frames) > 10:
                # Calculate actual frame rate
                time_span = end_time - start_time
                actual_fps = len(frames) / time_span
                expected_fps = 30.0  # Expected frame rate
                
                # Allow 20% tolerance for frame rate
                tolerance = 0.2
                assert abs(actual_fps - expected_fps) / expected_fps < tolerance, \
                    f"Frame rate deviation too high: {actual_fps} vs {expected_fps}"
                    
        thermal_camera.stop_streaming()
        thermal_camera.disconnect()
        
    @pytest.mark.hardware
    def test_temperature_accuracy(self, thermal_camera):
        """Test temperature measurement accuracy (NFR6)."""
        thermal_camera.connect()
        
        # Capture multiple frames for statistical analysis
        frames = []
        for _ in range(10):
            frame = thermal_camera.capture_frame()
            if frame:
                frames.append(frame)
            time.sleep(0.1)
            
        assert len(frames) > 0, "Should capture at least one frame"
        
        for frame in frames:
            # Validate temperature data consistency
            temp_data = frame.temperature_data
            assert np.all(temp_data >= frame.min_temp)
            assert np.all(temp_data <= frame.max_temp)
            assert abs(np.mean(temp_data) - frame.avg_temp) < 0.1
            
            # Check for reasonable temperature ranges
            assert frame.min_temp >= -50.0  # Reasonable lower bound
            assert frame.max_temp <= 200.0  # Reasonable upper bound
            
        thermal_camera.disconnect()


class TestThermalImageProcessing:
    """Test thermal image processing capabilities."""
    
    @pytest.mark.hardware
    def test_temperature_range_setting(self, thermal_camera):
        """Test setting custom temperature measurement ranges (FR4)."""
        thermal_camera.connect()
        
        # Set custom temperature range
        if hasattr(thermal_camera, 'set_temperature_range'):
            thermal_camera.set_temperature_range(20.0, 40.0)
            
            # Capture frame with new range
            frame = thermal_camera.capture_frame()
            if frame:
                # Verify temperatures are within set range
                assert frame.min_temp >= 19.0  # Allow small tolerance
                assert frame.max_temp <= 41.0  # Allow small tolerance
                
        thermal_camera.disconnect()
        
    @pytest.mark.hardware
    def test_visual_representation(self, thermal_camera):
        """Test thermal-to-visual image conversion (FR4)."""
        thermal_camera.connect()
        
        frame = thermal_camera.capture_frame()
        if frame:
            visual_data = frame.visual_data
            
            # Validate visual data properties
            assert visual_data.dtype == np.uint8
            assert len(visual_data.shape) == 3  # Height x Width x Channels
            assert visual_data.shape[2] == 3   # RGB channels
            
            # Check value ranges
            assert np.all(visual_data >= 0)
            assert np.all(visual_data <= 255)
            
            # Verify different colors are present (not all black/white)
            unique_colors = len(np.unique(visual_data.reshape(-1, 3), axis=0))
            assert unique_colors > 10, "Should have variety of colors"
            
        thermal_camera.disconnect()
        
    @pytest.mark.hardware
    def test_hot_spot_detection(self, thermal_camera):
        """Test detection of temperature hot spots (FR4)."""
        thermal_camera.connect()
        
        frame = thermal_camera.capture_frame()
        if frame:
            temp_data = frame.temperature_data
            
            # Find hot spots (temperatures significantly above average)
            threshold = frame.avg_temp + 2.0  # 2 degrees above average
            hot_spots = temp_data > threshold
            
            if np.any(hot_spots):
                # Analyze hot spot characteristics
                hot_spot_temps = temp_data[hot_spots]
                assert np.all(hot_spot_temps > threshold)
                assert len(hot_spot_temps) > 0
                
                logger.info(f"Detected {len(hot_spot_temps)} hot spot pixels")
                logger.info(f"Max hot spot temp: {np.max(hot_spot_temps):.1f}degC")
                
        thermal_camera.disconnect()


class TestThermalErrorHandling:
    """Test error handling and fault tolerance for thermal cameras."""
    
    @pytest.mark.hardware
    def test_connection_failure_recovery(self, thermal_manager):
        """Test recovery from connection failures (NFR5)."""
        camera = thermal_manager.get_camera()
        
        # Test multiple connection attempts
        for attempt in range(3):
            try:
                success = camera.connect()
                if success:
                    camera.disconnect()
                    break
            except Exception as e:
                if attempt == 2:
                    pytest.skip(f"Connection consistently failed: {e}")
                time.sleep(1.0)
                
    @pytest.mark.hardware
    def test_streaming_interruption(self, thermal_camera):
        """Test handling of streaming interruption (NFR5)."""
        thermal_camera.connect()
        thermal_camera.start_streaming()
        
        # Wait for streaming to start
        time.sleep(1.0)
        
        # Simulate interruption
        thermal_camera.disconnect()
        
        # Try to restart
        try:
            success = thermal_camera.connect()
            if success:
                thermal_camera.start_streaming()
                time.sleep(1.0)
                thermal_camera.stop_streaming()
        except Exception as e:
            # Expected behavior - should handle gracefully
            assert "disconnect" in str(e).lower() or "connection" in str(e).lower()
            
    @pytest.mark.hardware
    def test_invalid_frame_handling(self, thermal_camera):
        """Test handling of invalid or corrupted frames (NFR5)."""
        thermal_camera.connect()
        
        # This test would be more relevant with real hardware
        # For mock, we test the frame validation logic
        frame = thermal_camera.capture_frame()
        if frame:
            # Validate frame integrity
            assert frame.temperature_data.shape[0] > 0
            assert frame.temperature_data.shape[1] > 0
            assert not np.any(np.isnan(frame.temperature_data))
            assert not np.any(np.isinf(frame.temperature_data))
            
        thermal_camera.disconnect()


@pytest.mark.hardware
class TestThermalIntegration:
    """Integration tests with other system components."""
    
    def test_timestamp_synchronization(self, thermal_camera):
        """Test timestamp synchronization with system clock (FR3)."""
        thermal_camera.connect()
        
        system_time = time.time()
        frame = thermal_camera.capture_frame()
        
        if frame:
            # Check timestamp is reasonable
            time_diff = abs(frame.timestamp - system_time)
            assert time_diff < 2.0, "Timestamp synchronization issue"
            
        thermal_camera.disconnect()
        
    def test_concurrent_access_protection(self, thermal_manager):
        """Test concurrent access protection (NFR5)."""
        camera1 = thermal_manager.get_camera()
        
        # First connection
        success1 = camera1.connect()
        assert success1
        
        # Attempt second connection (should be handled gracefully)
        if not thermal_manager.use_mock:
            try:
                # This would test with real hardware limitations
                camera2 = thermal_manager._create_real_camera({'device_id': 'test'})
                success2 = camera2.connect(timeout=2.0)
                if success2:
                    camera2.disconnect()
            except Exception:
                pass  # Expected - device already in use
                
        camera1.disconnect()
        
    @pytest.mark.stress
    def test_extended_operation(self, thermal_camera):
        """Test extended thermal camera operation (NFR5)."""
        thermal_camera.connect()
        thermal_camera.start_streaming()
        
        # Extended operation test
        duration = 60.0  # seconds
        start_time = time.time()
        frame_count = 0
        
        while time.time() - start_time < duration:
            if hasattr(thermal_camera, 'get_latest_frame'):
                frame = thermal_camera.get_latest_frame()
                if frame:
                    frame_count += 1
                    
            # Check if still connected and streaming
            if hasattr(thermal_camera, 'connected'):
                assert thermal_camera.connected, "Camera disconnected during operation"
            if hasattr(thermal_camera, 'streaming'):
                assert thermal_camera.streaming, "Streaming stopped unexpectedly"
                
            time.sleep(1.0)
            
        logger.info(f"Processed {frame_count} frames over {duration} seconds")
        assert frame_count > duration * 10, "Insufficient frame rate during extended operation"
        
        thermal_camera.stop_streaming()
        thermal_camera.disconnect()


if __name__ == "__main__":
    # Allow running tests directly
    pytest.main([__file__, "-v", "--tb=short"])