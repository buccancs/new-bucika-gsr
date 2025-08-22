"""
Native Shimmer Processor Module

Provides high-performance C++ implementation for Shimmer sensor data processing
with fallback to pure Python implementation when native module is unavailable.
"""

import logging
import time
from typing import List, Dict, Any, Optional
import numpy as np

logger = logging.getLogger(__name__)

try:
    # Try to import the native C++ module
    from native_shimmer import ShimmerProcessor as NativeShimmerProcessor
    from native_shimmer import SensorReading as NativeSensorReading
    from native_shimmer import ProcessingConfig as NativeProcessingConfig
    NATIVE_AVAILABLE = True
    logger.info("Native Shimmer processor loaded successfully")
except ImportError as e:
    logger.warning(f"Native Shimmer processor not available: {e}")
    NATIVE_AVAILABLE = False


class SensorReading:
    """Unified sensor reading structure compatible with both native and Python implementations"""
    
    def __init__(self, timestamp: float = 0.0, gsr_value: float = 0.0, ppg_value: float = 0.0,
                 accel_x: float = 0.0, accel_y: float = 0.0, accel_z: float = 0.0,
                 gyro_x: float = 0.0, gyro_y: float = 0.0, gyro_z: float = 0.0,
                 mag_x: float = 0.0, mag_y: float = 0.0, mag_z: float = 0.0,
                 battery_level: float = 100.0):
        self.timestamp = timestamp
        self.gsr_value = gsr_value
        self.ppg_value = ppg_value
        self.accel_x = accel_x
        self.accel_y = accel_y
        self.accel_z = accel_z
        self.gyro_x = gyro_x
        self.gyro_y = gyro_y
        self.gyro_z = gyro_z
        self.mag_x = mag_x
        self.mag_y = mag_y
        self.mag_z = mag_z
        self.battery_level = battery_level
    
    def __repr__(self):
        return f"<SensorReading timestamp={self.timestamp} gsr={self.gsr_value}uS>"
    
    def to_dict(self) -> Dict[str, float]:
        """Convert to dictionary for JSON serialization"""
        return {
            'timestamp': self.timestamp,
            'gsr_value': self.gsr_value,
            'ppg_value': self.ppg_value,
            'accel_x': self.accel_x,
            'accel_y': self.accel_y,
            'accel_z': self.accel_z,
            'gyro_x': self.gyro_x,
            'gyro_y': self.gyro_y,
            'gyro_z': self.gyro_z,
            'mag_x': self.mag_x,
            'mag_y': self.mag_y,
            'mag_z': self.mag_z,
            'battery_level': self.battery_level
        }


class ProcessingConfig:
    """Configuration for Shimmer data processing"""
    
    def __init__(self, sampling_rate: float = 128.0, enable_filtering: bool = True,
                 filter_cutoff: float = 5.0, enable_artifact_removal: bool = True,
                 artifact_threshold: float = 100.0):
        self.sampling_rate = sampling_rate
        self.enable_filtering = enable_filtering
        self.filter_cutoff = filter_cutoff
        self.enable_artifact_removal = enable_artifact_removal
        self.artifact_threshold = artifact_threshold


class PythonShimmerProcessor:
    """Pure Python implementation of Shimmer data processor"""
    
    def __init__(self):
        self.config = ProcessingConfig()
        self.filter_state = [0.0] * 4
        self.total_processing_time_ms = 0.0
        self.packets_processed = 0
        
    def configure(self, config: ProcessingConfig):
        self.config = config
        self.filter_state = [0.0] * 4
        
    def process_raw_packet(self, raw_data: List[int]) -> SensorReading:
        start_time = time.perf_counter()
        
        reading = SensorReading()
        reading.timestamp = time.time() * 1000.0  # Convert to milliseconds
        
        if len(raw_data) < 20:
            self._update_performance_metrics(start_time)
            return reading
            
        # Parse raw Shimmer packet (simplified)
        gsr_raw = (raw_data[1] << 8) | raw_data[0]
        ppg_raw = (raw_data[3] << 8) | raw_data[2]
        
        # Accelerometer
        accel_x_raw = (raw_data[5] << 8) | raw_data[4]
        accel_y_raw = (raw_data[7] << 8) | raw_data[6]
        accel_z_raw = (raw_data[9] << 8) | raw_data[8]
        
        # Convert to signed 16-bit
        if accel_x_raw > 32767: accel_x_raw -= 65536
        if accel_y_raw > 32767: accel_y_raw -= 65536
        if accel_z_raw > 32767: accel_z_raw -= 65536
        
        # Convert to engineering units
        reading.gsr_value = self._convert_gsr_raw_to_microsiemens(gsr_raw)
        reading.ppg_value = ppg_raw * 0.001
        
        # Accelerometer scaling
        accel_scale = 4.0 / 65536.0 * 9.81  # +/-2g range to m/s^2
        reading.accel_x = accel_x_raw * accel_scale
        reading.accel_y = accel_y_raw * accel_scale
        reading.accel_z = accel_z_raw * accel_scale
        
        # Battery level
        reading.battery_level = raw_data[20] if len(raw_data) > 20 else 100.0
        
        # Apply filtering
        if self.config.enable_filtering:
            reading.gsr_value = self._apply_simple_filter(reading.gsr_value)
            
        self._update_performance_metrics(start_time)
        return reading
    
    def process_batch(self, raw_packets: List[List[int]]) -> List[SensorReading]:
        return [self.process_raw_packet(packet) for packet in raw_packets]
    
    def apply_low_pass_filter(self, signal: List[float], cutoff_freq: float) -> List[float]:
        if not signal:
            return signal
            
        # Simple IIR low-pass filter
        dt = 1.0 / self.config.sampling_rate
        alpha = dt / (dt + 1.0 / (2.0 * np.pi * cutoff_freq))
        
        filtered = [signal[0]]
        for i in range(1, len(signal)):
            filtered.append(alpha * signal[i] + (1.0 - alpha) * filtered[i-1])
            
        return filtered
    
    def remove_artifacts(self, gsr_signal: List[float]) -> List[float]:
        if not self.config.enable_artifact_removal or len(gsr_signal) < 3:
            return gsr_signal
            
        clean_signal = gsr_signal.copy()
        
        for i in range(1, len(clean_signal) - 1):
            diff_before = abs(clean_signal[i] - clean_signal[i-1])
            diff_after = abs(clean_signal[i+1] - clean_signal[i])
            
            if diff_before > self.config.artifact_threshold or diff_after > self.config.artifact_threshold:
                clean_signal[i] = (clean_signal[i-1] + clean_signal[i+1]) / 2.0
                
        return clean_signal
    
    def get_average_processing_time_ms(self) -> float:
        return self.total_processing_time_ms / self.packets_processed if self.packets_processed > 0 else 0.0
    
    def get_packets_processed(self) -> int:
        return self.packets_processed
    
    def reset_performance_counters(self):
        self.total_processing_time_ms = 0.0
        self.packets_processed = 0
    
    def _convert_gsr_raw_to_microsiemens(self, raw_value: int) -> float:
        voltage_range = 3.0
        adc_resolution = 4096.0
        
        voltage = (raw_value / adc_resolution) * voltage_range
        resistance = 40000.0 / voltage if voltage > 0 else float('inf')
        conductance = 1000000.0 / resistance if resistance != float('inf') else 0.0
        
        return max(0.0, min(100.0, conductance))
    
    def _apply_simple_filter(self, value: float) -> float:
        # Simple moving average filter
        self.filter_state[0] = value * 0.1 + self.filter_state[1] * 0.9
        self.filter_state[1:] = self.filter_state[:-1]
        return self.filter_state[0]
    
    def _update_performance_metrics(self, start_time: float):
        processing_time = (time.perf_counter() - start_time) * 1000.0
        self.total_processing_time_ms += processing_time
        self.packets_processed += 1


class ShimmerProcessor:
    """Unified Shimmer processor with automatic native/Python fallback"""
    
    def __init__(self, force_python: bool = False):
        self.use_native = NATIVE_AVAILABLE and not force_python
        
        if self.use_native:
            self.processor = NativeShimmerProcessor()
            logger.info("Using native C++ Shimmer processor")
        else:
            self.processor = PythonShimmerProcessor()
            logger.info("Using Python Shimmer processor")
    
    def configure(self, config: ProcessingConfig):
        if self.use_native:
            native_config = NativeProcessingConfig()
            native_config.sampling_rate = config.sampling_rate
            native_config.enable_filtering = config.enable_filtering
            native_config.filter_cutoff = config.filter_cutoff
            native_config.enable_artifact_removal = config.enable_artifact_removal
            native_config.artifact_threshold = config.artifact_threshold
            self.processor.configure(native_config)
        else:
            self.processor.configure(config)
    
    def process_raw_packet(self, raw_data: List[int]) -> SensorReading:
        if self.use_native:
            native_reading = self.processor.process_raw_packet(raw_data)
            # Convert native reading to Python reading
            return SensorReading(
                timestamp=native_reading.timestamp,
                gsr_value=native_reading.gsr_value,
                ppg_value=native_reading.ppg_value,
                accel_x=native_reading.accel_x,
                accel_y=native_reading.accel_y,
                accel_z=native_reading.accel_z,
                gyro_x=native_reading.gyro_x,
                gyro_y=native_reading.gyro_y,
                gyro_z=native_reading.gyro_z,
                mag_x=native_reading.mag_x,
                mag_y=native_reading.mag_y,
                mag_z=native_reading.mag_z,
                battery_level=native_reading.battery_level
            )
        else:
            return self.processor.process_raw_packet(raw_data)
    
    def process_batch(self, raw_packets: List[List[int]]) -> List[SensorReading]:
        if self.use_native:
            native_readings = self.processor.process_batch(raw_packets)
            return [SensorReading(
                timestamp=r.timestamp, gsr_value=r.gsr_value, ppg_value=r.ppg_value,
                accel_x=r.accel_x, accel_y=r.accel_y, accel_z=r.accel_z,
                gyro_x=r.gyro_x, gyro_y=r.gyro_y, gyro_z=r.gyro_z,
                mag_x=r.mag_x, mag_y=r.mag_y, mag_z=r.mag_z,
                battery_level=r.battery_level
            ) for r in native_readings]
        else:
            return self.processor.process_batch(raw_packets)
    
    def apply_low_pass_filter(self, signal: List[float], cutoff_freq: float) -> List[float]:
        return self.processor.apply_low_pass_filter(signal, cutoff_freq)
    
    def remove_artifacts(self, gsr_signal: List[float]) -> List[float]:
        return self.processor.remove_artifacts(gsr_signal)
    
    def get_average_processing_time_ms(self) -> float:
        return self.processor.get_average_processing_time_ms()
    
    def get_packets_processed(self) -> int:
        return self.processor.get_packets_processed()
    
    def reset_performance_counters(self):
        self.processor.reset_performance_counters()
    
    @property
    def backend_type(self) -> str:
        return "native" if self.use_native else "python"


# Export main classes
__all__ = ['ShimmerProcessor', 'SensorReading', 'ProcessingConfig', 'NATIVE_AVAILABLE']