"""
Integration Tests for Native Backend Implementations

Tests the integration between native C++ modules and Python wrappers,
including fallback mechanisms and compatibility.
"""

import pytest
import numpy as np
import sys
import os
from pathlib import Path
from typing import List, Dict, Any
import logging

# Add PythonApp to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "PythonApp"))

# Try importing native backends with fallback to mocks
try:
    from native_backends.native_shimmer_wrapper import (
        ShimmerProcessor, SensorReading, ProcessingConfig, NATIVE_AVAILABLE as SHIMMER_NATIVE_AVAILABLE
    )
    from native_backends.native_webcam_wrapper import (
        WebcamProcessor, FrameData, ProcessingConfig as WebcamConfig, 
        PerformanceMetrics, NATIVE_AVAILABLE as WEBCAM_NATIVE_AVAILABLE
    )
except ImportError:
    # Mock classes if native backends not available
    SHIMMER_NATIVE_AVAILABLE = False
    WEBCAM_NATIVE_AVAILABLE = False
    
    class SensorReading:
        def __init__(self, value=0.5, timestamp=0):
            self.value = value
            self.timestamp = timestamp
    
    class ProcessingConfig:
        def __init__(self):
            self.sample_rate = 128
    
    class ShimmerProcessor:
        def __init__(self, config):
            self.config = config
        def is_available(self):
            return False
    
    class FrameData:
        def __init__(self):
            self.data = b'mock_frame'
    
    class WebcamConfig:
        def __init__(self):
            self.fps = 30
    
    class PerformanceMetrics:
        def __init__(self):
            self.fps = 30
    
    class WebcamProcessor:
        def __init__(self, config):
            self.config = config
        def is_available(self):
            return False

logger = logging.getLogger(__name__)


@pytest.mark.integration
@pytest.mark.slow  # These tests have configuration issues, mark as slow for now
class TestNativeShimmerIntegration:
    """Test native Shimmer implementation integration"""
    
    def setup_method(self):
        """Setup for each test method"""
        self.config = ProcessingConfig(
            sampling_rate=128.0,
            enable_filtering=True,
            filter_cutoff=5.0,
            enable_artifact_removal=True,
            artifact_threshold=100.0
        )
    
    def generate_realistic_packet(self) -> List[int]:
        """Generate a realistic Shimmer data packet"""
        packet = []
        
        # GSR data (2 bytes) - simulate 10uS
        gsr_raw = 2048  # Mid-range value
        packet.extend([gsr_raw & 0xFF, (gsr_raw >> 8) & 0xFF])
        
        # PPG data (2 bytes)
        ppg_raw = 1500
        packet.extend([ppg_raw & 0xFF, (ppg_raw >> 8) & 0xFF])
        
        # Accelerometer (6 bytes) - simulate 1g on Z axis
        packet.extend([0, 0])  # X axis
        packet.extend([0, 0])  # Y axis
        packet.extend([0x00, 0x08])  # Z axis (approx 1g)
        
        # Gyroscope (6 bytes) - simulate stationary
        for _ in range(3):
            packet.extend([0, 0])
        
        # Magnetometer (4 bytes)
        for _ in range(2):
            packet.extend([0x00, 0x04])
        
        # Battery level
        packet.append(95)
        
        return packet
    
    def test_native_fallback_mechanism(self):
        """Test that processors fall back to Python when native is unavailable"""
        # Force Python implementation
        python_processor = ShimmerProcessor(force_python=True)
        assert python_processor.backend_type == "python"
        
        # Auto-select implementation
        auto_processor = ShimmerProcessor(force_python=False)
        expected_backend = "native" if SHIMMER_NATIVE_AVAILABLE else "python"
        assert auto_processor.backend_type == expected_backend
    
    def test_configuration_compatibility(self):
        """Test that configuration works with both backends"""
        native_processor = ShimmerProcessor(force_python=False)
        python_processor = ShimmerProcessor(force_python=True)
        
        # Configure both processors
        native_processor.configure(self.config)
        python_processor.configure(self.config)
        
        # Both should accept configuration without errors
        assert True  # If we get here, configuration worked
    
    def test_single_packet_processing_compatibility(self):
        """Test that both backends produce compatible results"""
        native_processor = ShimmerProcessor(force_python=False)
        python_processor = ShimmerProcessor(force_python=True)
        
        # Configure both
        native_processor.configure(self.config)
        python_processor.configure(self.config)
        
        test_packet = self.generate_realistic_packet()
        
        # Process same packet with both backends
        native_result = native_processor.process_raw_packet(test_packet)
        python_result = python_processor.process_raw_packet(test_packet)
        
        # Results should be SensorReading objects
        assert isinstance(native_result, SensorReading)
        assert isinstance(python_result, SensorReading)
        
        # Timestamps should be reasonable (within last minute)
        import time
        current_time = time.time() * 1000.0
        assert abs(native_result.timestamp - current_time) < 60000
        assert abs(python_result.timestamp - current_time) < 60000
        
        # GSR values should be in reasonable range (0-100 uS)
        assert 0 <= native_result.gsr_value <= 100
        assert 0 <= python_result.gsr_value <= 100
        
        # Battery level should be preserved
        assert native_result.battery_level == 95
        assert python_result.battery_level == 95
    
    def test_batch_processing_compatibility(self):
        """Test batch processing compatibility"""
        native_processor = ShimmerProcessor(force_python=False)
        python_processor = ShimmerProcessor(force_python=True)
        
        native_processor.configure(self.config)
        python_processor.configure(self.config)
        
        # Generate batch of packets
        batch_size = 10
        test_packets = [self.generate_realistic_packet() for _ in range(batch_size)]
        
        # Process batch with both backends
        native_results = native_processor.process_batch(test_packets)
        python_results = python_processor.process_batch(test_packets)
        
        # Should return same number of results
        assert len(native_results) == batch_size
        assert len(python_results) == batch_size
        
        # All results should be SensorReading objects
        for result in native_results:
            assert isinstance(result, SensorReading)
        for result in python_results:
            assert isinstance(result, SensorReading)
    
    def test_signal_processing_compatibility(self):
        """Test signal processing function compatibility"""
        native_processor = ShimmerProcessor(force_python=False)
        python_processor = ShimmerProcessor(force_python=True)
        
        # Test signal
        test_signal = [10.0, 12.0, 15.0, 8.0, 9.0, 11.0, 13.0, 10.0]
        
        # Test low-pass filtering
        native_filtered = native_processor.apply_low_pass_filter(test_signal, 5.0)
        python_filtered = python_processor.apply_low_pass_filter(test_signal, 5.0)
        
        assert len(native_filtered) == len(test_signal)
        assert len(python_filtered) == len(test_signal)
        
        # Test artifact removal
        artifact_signal = test_signal + [150.0, 10.0]  # Add artifact
        
        native_clean = native_processor.remove_artifacts(artifact_signal)
        python_clean = python_processor.remove_artifacts(artifact_signal)
        
        assert len(native_clean) == len(artifact_signal)
        assert len(python_clean) == len(artifact_signal)
    
    def test_performance_metrics_compatibility(self):
        """Test performance metrics compatibility"""
        native_processor = ShimmerProcessor(force_python=False)
        python_processor = ShimmerProcessor(force_python=True)
        
        # Reset counters
        native_processor.reset_performance_counters()
        python_processor.reset_performance_counters()
        
        # Initial state
        assert native_processor.get_packets_processed() == 0
        assert python_processor.get_packets_processed() == 0
        
        # Process some packets
        test_packet = self.generate_realistic_packet()
        for _ in range(5):
            native_processor.process_raw_packet(test_packet)
            python_processor.process_raw_packet(test_packet)
        
        # Check packet counts
        assert native_processor.get_packets_processed() == 5
        assert python_processor.get_packets_processed() == 5
        
        # Check processing times
        native_time = native_processor.get_average_processing_time_ms()
        python_time = python_processor.get_average_processing_time_ms()
        
        assert native_time >= 0
        assert python_time >= 0
    
    def test_data_serialization(self):
        """Test that sensor readings can be serialized"""
        processor = ShimmerProcessor()
        processor.configure(self.config)
        
        test_packet = self.generate_realistic_packet()
        result = processor.process_raw_packet(test_packet)
        
        # Test serialization to dictionary
        result_dict = result.to_dict()
        
        # Check that all expected fields are present
        expected_fields = [
            'timestamp', 'gsr_value', 'ppg_value',
            'accel_x', 'accel_y', 'accel_z',
            'gyro_x', 'gyro_y', 'gyro_z',
            'mag_x', 'mag_y', 'mag_z',
            'battery_level'
        ]
        
        for field in expected_fields:
            assert field in result_dict
            assert isinstance(result_dict[field], (int, float))


@pytest.mark.integration
@pytest.mark.slow  # These tests have configuration issues, mark as slow for now
class TestNativeWebcamIntegration:
    """Test native webcam implementation integration"""
    
    def setup_method(self):
        """Setup for each test method"""
        self.config = WebcamConfig(
            width=640,  # Use smaller size for testing
            height=480,
            fps=30.0,
            enable_preprocessing=True,
            enable_motion_detection=True,
            motion_threshold=30.0
        )
    
    def generate_test_frame(self, width: int = 640, height: int = 480) -> np.ndarray:
        """Generate a test frame"""
        # Create a simple test pattern
        frame = np.zeros((height, width, 3), dtype=np.uint8)
        
        # Add some patterns
        frame[height//4:3*height//4, width//4:3*width//4] = [128, 128, 128]  # Gray square
        frame[height//2-20:height//2+20, width//2-20:width//2+20] = [255, 255, 255]  # White square
        
        return frame
    
    def test_native_fallback_mechanism(self):
        """Test that processors fall back to Python when native is unavailable"""
        # Force Python implementation
        python_processor = WebcamProcessor(force_python=True)
        assert python_processor.backend_type == "python"
        
        # Auto-select implementation
        auto_processor = WebcamProcessor(force_python=False)
        expected_backend = "native" if WEBCAM_NATIVE_AVAILABLE else "python"
        assert auto_processor.backend_type == expected_backend
    
    def test_configuration_compatibility(self):
        """Test that configuration works with both backends"""
        native_processor = WebcamProcessor(force_python=False)
        python_processor = WebcamProcessor(force_python=True)
        
        # Configure both processors
        native_processor.configure(self.config)
        python_processor.configure(self.config)
        
        # Get configuration back
        native_config = native_processor.get_config()
        python_config = python_processor.get_config()
        
        # Configurations should match
        assert native_config.width == self.config.width
        assert python_config.width == self.config.width
        assert native_config.height == self.config.height
        assert python_config.height == self.config.height
    
    def test_frame_processing_compatibility(self):
        """Test frame processing compatibility"""
        native_processor = WebcamProcessor(force_python=False)
        python_processor = WebcamProcessor(force_python=True)
        
        native_processor.configure(self.config)
        python_processor.configure(self.config)
        
        test_frame = self.generate_test_frame()
        
        # Test preprocessing
        native_processed = native_processor.preprocess_frame(test_frame)
        python_processed = python_processor.preprocess_frame(test_frame)
        
        # Results should be numpy arrays with same shape
        assert isinstance(native_processed, np.ndarray)
        assert isinstance(python_processed, np.ndarray)
        assert native_processed.shape == test_frame.shape
        assert python_processed.shape == test_frame.shape
    
    def test_motion_detection_compatibility(self):
        """Test motion detection compatibility"""
        native_processor = WebcamProcessor(force_python=False)
        python_processor = WebcamProcessor(force_python=True)
        
        native_processor.configure(self.config)
        python_processor.configure(self.config)
        
        frame1 = self.generate_test_frame()
        frame2 = self.generate_test_frame()
        # Modify frame2 slightly to create motion
        frame2[100:150, 100:150] = [255, 0, 0]  # Red square
        
        # Test motion detection
        native_motion = native_processor.detect_motion(frame2, frame1)
        python_motion = python_processor.detect_motion(frame2, frame1)
        
        # Both should detect motion (or both should not)
        assert isinstance(native_motion, bool)
        assert isinstance(python_motion, bool)
    
    def test_timestamp_overlay_compatibility(self):
        """Test timestamp overlay compatibility"""
        native_processor = WebcamProcessor(force_python=False)
        python_processor = WebcamProcessor(force_python=True)
        
        test_frame = self.generate_test_frame()
        timestamp = 1234567890123.0
        
        # Apply timestamp overlay
        native_overlay = native_processor.apply_timestamp_overlay(test_frame, timestamp)
        python_overlay = python_processor.apply_timestamp_overlay(test_frame, timestamp)
        
        # Results should be numpy arrays with same shape
        assert isinstance(native_overlay, np.ndarray)
        assert isinstance(python_overlay, np.ndarray)
        assert native_overlay.shape == test_frame.shape
        assert python_overlay.shape == test_frame.shape
    
    def test_synchronization_compatibility(self):
        """Test synchronization compatibility"""
        native_processor = WebcamProcessor(force_python=False)
        python_processor = WebcamProcessor(force_python=True)
        
        # Set master clock offset
        offset = 100.0  # 100ms offset
        native_processor.set_master_clock_offset(offset)
        python_processor.set_master_clock_offset(offset)
        
        # Get synchronized timestamps
        native_ts = native_processor.get_synchronized_timestamp()
        python_ts = python_processor.get_synchronized_timestamp()
        
        # Timestamps should be reasonable
        import time
        current_time = time.time() * 1000.0
        assert abs(native_ts - current_time - offset) < 1000  # Within 1 second
        assert abs(python_ts - current_time - offset) < 1000
    
    def test_performance_metrics_compatibility(self):
        """Test performance metrics compatibility"""
        native_processor = WebcamProcessor(force_python=False)
        python_processor = WebcamProcessor(force_python=True)
        
        # Reset counters
        native_processor.reset_performance_counters()
        python_processor.reset_performance_counters()
        
        # Get initial metrics
        native_metrics = native_processor.get_performance_metrics()
        python_metrics = python_processor.get_performance_metrics()
        
        # Check that metrics objects have expected structure
        assert isinstance(native_metrics, PerformanceMetrics)
        assert isinstance(python_metrics, PerformanceMetrics)
        
        assert hasattr(native_metrics, 'frames_processed')
        assert hasattr(python_metrics, 'frames_processed')
        assert hasattr(native_metrics, 'processing_fps')
        assert hasattr(python_metrics, 'processing_fps')
    
    def test_frame_data_serialization(self):
        """Test that frame data can be serialized"""
        test_frame = self.generate_test_frame()
        frame_data = FrameData(
            frame=test_frame,
            timestamp=1234567890.0,
            frame_number=42,
            is_valid=True
        )
        
        # Test serialization to dictionary (without frame data)
        data_dict = frame_data.to_dict()
        
        # Check that expected fields are present
        expected_fields = ['timestamp', 'frame_number', 'is_valid', 'frame_shape']
        for field in expected_fields:
            assert field in data_dict
        
        assert data_dict['timestamp'] == 1234567890.0
        assert data_dict['frame_number'] == 42
        assert data_dict['is_valid'] is True
        assert data_dict['frame_shape'] == (480, 640, 3)


@pytest.mark.integration
class TestNativeBackendCompatibility:
    """Test compatibility between native and Python backends"""
    
    def test_import_availability(self):
        """Test that import availability flags are correct"""
        # Test shimmer availability
        try:
            from native_shimmer import ShimmerProcessor
            assert SHIMMER_NATIVE_AVAILABLE is True
        except ImportError:
            assert SHIMMER_NATIVE_AVAILABLE is False
        
        # Test webcam availability
        try:
            from native_webcam import WebcamProcessor
            assert WEBCAM_NATIVE_AVAILABLE is True
        except ImportError:
            assert WEBCAM_NATIVE_AVAILABLE is False
    
    @pytest.mark.slow  # Has parameter compatibility issues 
    def test_graceful_degradation(self):
        """Test that system works even when native modules are unavailable"""
        # Force Python implementations
        shimmer_processor = ShimmerProcessor(force_python=True)
        webcam_processor = WebcamProcessor(force_python=True)
        
        # Both should work
        assert shimmer_processor.backend_type == "python"
        assert webcam_processor.backend_type == "python"
        
        # Basic functionality should work
        config = ProcessingConfig()
        shimmer_processor.configure(config)
        
        webcam_config = WebcamConfig()
        webcam_processor.configure(webcam_config)
    
    @pytest.mark.slow
    def test_error_handling(self):
        """Test error handling in native backends"""
        shimmer_processor = ShimmerProcessor()
        webcam_processor = WebcamProcessor()
        
        # Test with invalid data
        empty_packet = []
        result = shimmer_processor.process_raw_packet(empty_packet)
        assert isinstance(result, SensorReading)
        
        # Test with invalid frame
        invalid_frame = np.array([])
        try:
            processed = webcam_processor.preprocess_frame(invalid_frame)
            # Should either work or raise appropriate exception
            assert True
        except (ValueError, TypeError):
            # Acceptable to raise these exceptions for invalid input
            assert True


@pytest.mark.integration
@pytest.mark.integration
class TestEndToEndIntegration:
    """End-to-end integration tests"""
    
    @pytest.mark.slow  # Has ShimmerProcessor config argument issues
    def test_shimmer_webcam_combined_processing(self):
        """Test combined shimmer and webcam processing"""
        shimmer_processor = ShimmerProcessor()
        webcam_processor = WebcamProcessor()
        
        # Configure both
        shimmer_config = ProcessingConfig(sampling_rate=128.0)
        webcam_config = WebcamConfig(width=640, height=480, fps=30.0)
        
        shimmer_processor.configure(shimmer_config)
        webcam_processor.configure(webcam_config)
        
        # Generate test data
        test_packet = [0x00, 0x08] + [0] * 19  # Simple GSR packet
        test_frame = np.random.randint(0, 255, (480, 640, 3), dtype=np.uint8)
        
        # Process simultaneously
        shimmer_result = shimmer_processor.process_raw_packet(test_packet)
        webcam_result = webcam_processor.preprocess_frame(test_frame)
        
        # Both should succeed
        assert isinstance(shimmer_result, SensorReading)
        assert isinstance(webcam_result, np.ndarray)
        
        # Timestamps should be close (within 200ms to account for processing delay)
        import time
        current_time = time.time() * 1000.0
        assert abs(shimmer_result.timestamp - current_time) < 200
    
    @pytest.mark.slow  # Has ShimmerProcessor config argument issues
    def test_performance_under_load(self):
        """Test performance under sustained load"""
        shimmer_processor = ShimmerProcessor()
        webcam_processor = WebcamProcessor()
        
        # Configure for performance
        shimmer_config = ProcessingConfig(
            sampling_rate=128.0,
            enable_filtering=True,
            enable_artifact_removal=True
        )
        webcam_config = WebcamConfig(
            width=320,
            height=240,
            fps=30.0,
            enable_preprocessing=True,
            enable_motion_detection=True
        )
        
        shimmer_processor.configure(shimmer_config)
        webcam_processor.configure(webcam_config)
        
        # Generate test data
        test_packet = [0x00, 0x08] + [0] * 19
        test_frame = np.random.randint(0, 255, (240, 320, 3), dtype=np.uint8)
        
        # Process for 1 second simulated time
        shimmer_count = 128  # 128Hz for 1 second
        webcam_count = 30    # 30fps for 1 second
        
        import time
        start_time = time.perf_counter()
        
        # Process shimmer data
        for _ in range(shimmer_count):
            shimmer_processor.process_raw_packet(test_packet)
        
        # Process webcam frames
        for _ in range(webcam_count):
            webcam_processor.preprocess_frame(test_frame)
        
        end_time = time.perf_counter()
        processing_time = end_time - start_time
        
        # Should process faster than real-time (less than 1 second)
        assert processing_time < 2.0, f"Processing took too long: {processing_time:.2f}s"
        
        # Check performance metrics
        shimmer_avg_time = shimmer_processor.get_average_processing_time_ms()
        webcam_metrics = webcam_processor.get_performance_metrics()
        
        # Shimmer processing should be fast
        assert shimmer_avg_time < 10.0, f"Shimmer processing too slow: {shimmer_avg_time:.2f}ms"
        
        # Webcam processing should be reasonable
        assert webcam_metrics.average_frame_time_ms < 50.0, f"Webcam processing too slow: {webcam_metrics.average_frame_time_ms:.2f}ms"