"""
Lab Streaming Layer (LSL) integration for real-time data streaming.

This module provides LSL outlet functionality for streaming sensor data
from the multi-sensor recording system to external applications for
real-time monitoring and analysis.
"""

import logging
import time
from typing import Dict, Optional, List, Any
from dataclasses import dataclass
import numpy as np

# Try to import pylsl, with fallback to mock implementation
LSL_AVAILABLE = False
try:
    import pylsl
    LSL_AVAILABLE = True
except (ImportError, RuntimeError):
    # Create mock pylsl for when the library isn't available
    class MockPyLSL:
        IRREGULAR_RATE = 0.0
        
        class StreamInfo:
            def __init__(self, name, type, channel_count, nominal_srate, channel_format='float32', source_id=''):
                self.name = name
                self.type = type
                self.channel_count = channel_count
                self.nominal_srate = nominal_srate
                
            def desc(self):
                return MockPyLSL.XMLElement()
        
        class StreamOutlet:
            def __init__(self, info):
                self.info = info
                
            def push_sample(self, data, timestamp=None):
                pass  # Mock implementation
                
            def push_chunk(self, data, timestamps=None):
                pass  # Mock implementation
        
        class XMLElement:
            def append_child(self, name):
                return MockPyLSL.XMLElement()
                
            def append_child_value(self, name, value):
                pass
    
    pylsl = MockPyLSL()


@dataclass
class LSLStreamConfig:
    """Configuration for an LSL stream outlet."""
    name: str
    type: str
    channel_count: int
    nominal_srate: float
    channel_format: str = 'float32'
    source_id: str = ''
    channel_names: Optional[List[str]] = None
    channel_units: Optional[List[str]] = None


class LSLStreamer:
    """Manages LSL outlet streams for real-time sensor data broadcasting."""
    
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.outlets: Dict[str, Any] = {}
        self.stream_configs: Dict[str, LSLStreamConfig] = {}
        self.is_enabled = LSL_AVAILABLE
        
        if not LSL_AVAILABLE:
            self.logger.warning("LSL (pylsl) not available - LSL streaming disabled")
        else:
            self.logger.info("LSL integration initialized successfully")
    
    def create_outlet(self, stream_id: str, config: LSLStreamConfig) -> bool:
        """Create a new LSL outlet stream."""
        if not self.is_enabled:
            self.logger.debug(f"LSL not available, skipping outlet creation for {stream_id}")
            return False
        
        try:
            # Create stream info
            info = pylsl.StreamInfo(
                name=config.name,
                type=config.type,
                channel_count=config.channel_count,
                nominal_srate=config.nominal_srate,
                channel_format=config.channel_format,
                source_id=config.source_id or stream_id
            )
            
            # Add channel metadata if provided
            if config.channel_names or config.channel_units:
                channels = info.desc().append_child("channels")
                for i in range(config.channel_count):
                    channel = channels.append_child("channel")
                    
                    if config.channel_names and i < len(config.channel_names):
                        channel.append_child_value("label", config.channel_names[i])
                    else:
                        channel.append_child_value("label", f"Channel_{i+1}")
                    
                    if config.channel_units and i < len(config.channel_units):
                        channel.append_child_value("unit", config.channel_units[i])
                    else:
                        channel.append_child_value("unit", "unknown")
            
            # Add acquisition metadata
            acquisition = info.desc().append_child("acquisition")
            acquisition.append_child_value("manufacturer", "UCL Multi-Sensor System")
            acquisition.append_child_value("model", "BucikaGSR")
            
            # Create outlet
            outlet = pylsl.StreamOutlet(info)
            
            self.outlets[stream_id] = outlet
            self.stream_configs[stream_id] = config
            
            self.logger.info(f"Created LSL outlet '{config.name}' ({config.type}) with {config.channel_count} channels")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to create LSL outlet for {stream_id}: {e}")
            return False
    
    def push_sample(self, stream_id: str, data: List[float], timestamp: Optional[float] = None) -> bool:
        """Push a single sample to the specified LSL stream."""
        if not self.is_enabled or stream_id not in self.outlets:
            return False
        
        try:
            outlet = self.outlets[stream_id]
            config = self.stream_configs[stream_id]
            
            # Validate data length
            if len(data) != config.channel_count:
                self.logger.warning(f"Data length {len(data)} doesn't match channel count {config.channel_count}")
                return False
            
            # Push sample with optional timestamp
            if timestamp is not None:
                outlet.push_sample(data, timestamp)
            else:
                outlet.push_sample(data)
            
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to push sample to LSL stream {stream_id}: {e}")
            return False
    
    def push_chunk(self, stream_id: str, data: List[List[float]], timestamps: Optional[List[float]] = None) -> bool:
        """Push multiple samples to the specified LSL stream."""
        if not self.is_enabled or stream_id not in self.outlets:
            return False
        
        try:
            outlet = self.outlets[stream_id]
            config = self.stream_configs[stream_id]
            
            # Validate data format
            if not data or len(data[0]) != config.channel_count:
                self.logger.warning(f"Invalid data format for stream {stream_id}")
                return False
            
            # Push chunk with optional timestamps
            if timestamps is not None:
                outlet.push_chunk(data, timestamps)
            else:
                outlet.push_chunk(data)
            
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to push chunk to LSL stream {stream_id}: {e}")
            return False
    
    def remove_outlet(self, stream_id: str) -> bool:
        """Remove an LSL outlet stream."""
        if stream_id in self.outlets:
            try:
                # LSL outlets are automatically cleaned up when they go out of scope
                del self.outlets[stream_id]
                del self.stream_configs[stream_id]
                self.logger.info(f"Removed LSL outlet: {stream_id}")
                return True
            except Exception as e:
                self.logger.error(f"Error removing LSL outlet {stream_id}: {e}")
                return False
        return False
    
    def get_active_streams(self) -> List[str]:
        """Get list of active stream IDs."""
        return list(self.outlets.keys())
    
    def cleanup(self):
        """Clean up all LSL outlets."""
        for stream_id in list(self.outlets.keys()):
            self.remove_outlet(stream_id)
        self.logger.info("LSL integration cleaned up")


class DefaultLSLStreams:
    """Factory for creating standard LSL stream configurations."""
    
    @staticmethod
    def shimmer_gsr_stream(device_id: str = "shimmer_001", sampling_rate: float = 128.0) -> LSLStreamConfig:
        """Create LSL stream config for Shimmer GSR data."""
        return LSLStreamConfig(
            name=f"ShimmerGSR_{device_id}",
            type="GSR",
            channel_count=4,
            nominal_srate=sampling_rate,
            channel_names=["GSR_Conductance", "PPG_A13", "Accel_Magnitude", "Battery"],
            channel_units=["microsiemens", "ADC", "g", "percent"],
            source_id=f"shimmer_{device_id}"
        )
    
    @staticmethod
    def thermal_stream(device_id: str = "thermal_001", sampling_rate: float = 25.0) -> LSLStreamConfig:
        """Create LSL stream config for thermal camera data markers."""
        return LSLStreamConfig(
            name=f"ThermalMarkers_{device_id}",
            type="Markers",
            channel_count=1,
            nominal_srate=sampling_rate,
            channel_names=["Frame_Marker"],
            channel_units=["frame_id"],
            source_id=f"thermal_{device_id}"
        )
    
    @staticmethod
    def rgb_camera_stream(device_id: str = "rgb_001", sampling_rate: float = 30.0) -> LSLStreamConfig:
        """Create LSL stream config for RGB camera markers."""
        return LSLStreamConfig(
            name=f"RGBMarkers_{device_id}",
            type="Markers",
            channel_count=1,
            nominal_srate=sampling_rate,
            channel_names=["Frame_Marker"],
            channel_units=["frame_id"],
            source_id=f"rgb_{device_id}"
        )
    
    @staticmethod
    def sync_markers_stream() -> LSLStreamConfig:
        """Create LSL stream config for synchronization markers."""
        return LSLStreamConfig(
            name="SyncMarkers",
            type="Markers",
            channel_count=1,
            nominal_srate=pylsl.IRREGULAR_RATE,
            channel_names=["Sync_Event"],
            channel_units=["event_type"],
            source_id="sync_master"
        )


# Convenience functions for quick LSL integration
def create_shimmer_lsl_outlet(device_id: str, sampling_rate: float = 128.0, logger: Optional[logging.Logger] = None) -> Optional[LSLStreamer]:
    """Create and configure LSL outlet for Shimmer GSR data."""
    streamer = LSLStreamer(logger)
    config = DefaultLSLStreams.shimmer_gsr_stream(device_id, sampling_rate)
    
    if streamer.create_outlet(f"shimmer_{device_id}", config):
        return streamer
    return None


def create_sync_marker_outlet(logger: Optional[logging.Logger] = None) -> Optional[LSLStreamer]:
    """Create LSL outlet for synchronization markers."""
    streamer = LSLStreamer(logger)
    config = DefaultLSLStreams.sync_markers_stream()
    
    if streamer.create_outlet("sync_markers", config):
        return streamer
    return None


def push_sync_marker(streamer: LSLStreamer, event_type: str, timestamp: Optional[float] = None):
    """Push a synchronization marker to LSL."""
    # Convert event type to numeric code for LSL
    event_codes = {
        "session_start": 1.0,
        "session_stop": 2.0,
        "flash_sync": 10.0,
        "beep_sync": 11.0,
        "calibration_start": 20.0,
        "calibration_end": 21.0,
        "recording_start": 30.0,
        "recording_stop": 31.0
    }
    
    event_code = event_codes.get(event_type, 99.0)  # 99.0 for unknown events
    streamer.push_sample("sync_markers", [event_code], timestamp)


if __name__ == "__main__":
    # Example usage
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    if not LSL_AVAILABLE:
        logger.error("LSL not available - install pylsl to test this module")
        exit(1)
    
    # Create test streamer
    streamer = LSLStreamer(logger)
    
    # Create Shimmer GSR stream
    config = DefaultLSLStreams.shimmer_gsr_stream("test_device")
    if streamer.create_outlet("test_shimmer", config):
        logger.info("Created test Shimmer GSR stream")
        
        # Push some test data
        for i in range(10):
            test_data = [
                5.0 + i * 0.1,  # GSR
                2048 + i * 10,  # PPG
                1.0 + i * 0.05, # Accel
                85 - i          # Battery
            ]
            streamer.push_sample("test_shimmer", test_data)
            time.sleep(1.0 / 128.0)  # 128 Hz
        
        logger.info("Pushed test data to LSL stream")
    
    # Create sync marker stream
    sync_config = DefaultLSLStreams.sync_markers_stream()
    if streamer.create_outlet("sync_test", sync_config):
        push_sync_marker(streamer, "session_start")
        logger.info("Pushed sync marker")
    
    # Cleanup
    streamer.cleanup()
    logger.info("Test completed")