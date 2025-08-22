"""
Common data structures used across Android and Python platforms.

This module consolidates data structures that are duplicated between the
Android and Python applications to ensure consistency.
"""

from dataclasses import dataclass
from enum import Enum
from typing import Dict, List, Optional, Any
import json
import time


# Device and Connection States
class DeviceType(Enum):
    """Standardized device types."""
    SHIMMER_GSR = "shimmer_gsr"
    THERMAL_CAMERA = "thermal_camera" 
    RGB_CAMERA = "rgb_camera"
    ANDROID_PHONE = "android_phone"
    PC_WEBCAM = "pc_webcam"


class DeviceState(Enum):
    """Standardized device connection states."""
    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    STREAMING = "streaming"
    ERROR = "error"
    CALIBRATING = "calibrating"


class SessionStatus(Enum):
    """Session lifecycle states."""
    INACTIVE = "inactive"
    STARTING = "starting"
    ACTIVE = "active"
    STOPPING = "stopping"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


# Data Structures
@dataclass
class DeviceInfo:
    """Standardized device information."""
    device_id: str
    device_type: DeviceType
    capabilities: List[str]
    firmware_version: str = "unknown"
    battery_level: Optional[float] = None
    connection_time: Optional[float] = None
    last_seen: Optional[float] = None
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "device_id": self.device_id,
            "device_type": self.device_type.value,
            "capabilities": self.capabilities,
            "firmware_version": self.firmware_version,
            "battery_level": self.battery_level,
            "connection_time": self.connection_time,
            "last_seen": self.last_seen
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "DeviceInfo":
        """Create from dictionary."""
        return cls(
            device_id=data["device_id"],
            device_type=DeviceType(data["device_type"]),
            capabilities=data["capabilities"],
            firmware_version=data.get("firmware_version", "unknown"),
            battery_level=data.get("battery_level"),
            connection_time=data.get("connection_time"),
            last_seen=data.get("last_seen")
        )


@dataclass
class SessionConfig:
    """Standardized session configuration."""
    session_id: str
    session_name: str
    participant_id: str
    researcher_id: str
    experiment_type: str
    expected_duration_minutes: int
    devices_enabled: List[DeviceType]
    sampling_rates: Dict[str, float]
    video_quality: str = "1080p"
    audio_enabled: bool = True
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "session_id": self.session_id,
            "session_name": self.session_name,
            "participant_id": self.participant_id,
            "researcher_id": self.researcher_id,
            "experiment_type": self.experiment_type,
            "expected_duration_minutes": self.expected_duration_minutes,
            "devices_enabled": [d.value for d in self.devices_enabled],
            "sampling_rates": self.sampling_rates,
            "video_quality": self.video_quality,
            "audio_enabled": self.audio_enabled
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SessionConfig":
        """Create from dictionary."""
        return cls(
            session_id=data["session_id"],
            session_name=data["session_name"],
            participant_id=data["participant_id"],
            researcher_id=data["researcher_id"],
            experiment_type=data["experiment_type"],
            expected_duration_minutes=data["expected_duration_minutes"],
            devices_enabled=[DeviceType(d) for d in data["devices_enabled"]],
            sampling_rates=data["sampling_rates"],
            video_quality=data.get("video_quality", "1080p"),
            audio_enabled=data.get("audio_enabled", True)
        )


@dataclass 
class SensorSample:
    """Standardized sensor data sample."""
    device_id: str
    device_type: DeviceType
    timestamp_ms: int
    data: Dict[str, Any]
    session_id: Optional[str] = None
    sync_verified: bool = False
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "device_id": self.device_id,
            "device_type": self.device_type.value,
            "timestamp_ms": self.timestamp_ms,
            "data": self.data,
            "session_id": self.session_id,
            "sync_verified": self.sync_verified
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SensorSample":
        """Create from dictionary."""
        return cls(
            device_id=data["device_id"],
            device_type=DeviceType(data["device_type"]),
            timestamp_ms=data["timestamp_ms"],
            data=data["data"],
            session_id=data.get("session_id"),
            sync_verified=data.get("sync_verified", False)
        )


@dataclass
class CalibrationPattern:
    """Standardized calibration pattern specification."""
    pattern_type: str  # "chessboard", "circles", "aruco"
    pattern_size: tuple  # (width, height) in pattern units
    square_size_mm: float  # physical size in millimeters
    marker_size_mm: Optional[float] = None  # for ArUco patterns
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "pattern_type": self.pattern_type,
            "pattern_size": self.pattern_size,
            "square_size_mm": self.square_size_mm,
            "marker_size_mm": self.marker_size_mm
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "CalibrationPattern":
        """Create from dictionary."""
        return cls(
            pattern_type=data["pattern_type"],
            pattern_size=tuple(data["pattern_size"]),
            square_size_mm=data["square_size_mm"],
            marker_size_mm=data.get("marker_size_mm")
        )


# Standard CSV formats
STANDARD_CSV_FORMATS = {
    "shimmer_gsr": [
        "timestamp_ms", "device_time_ms", "system_time_ms",
        "gsr_conductance_us", "ppg_a13", "accel_x_g", "accel_y_g", 
        "accel_z_g", "accel_magnitude_g", "battery_percentage"
    ],
    "thermal_camera": [
        "timestamp_ms", "frame_id", "min_temp_c", "max_temp_c", 
        "mean_temp_c", "std_temp_c", "median_temp_c", "device_temp_c",
        "emissivity", "reflected_temp_c", "atmospheric_temp_c", 
        "distance_m", "humidity_percent", "raw_data_base64", 
        "radiometric_data_base64"
    ],
    "rgb_camera": [
        "timestamp_ms", "frame_id", "video_filename", "frame_width",
        "frame_height", "fps", "codec", "bitrate_kbps", "exposure_time_ms",
        "iso", "focal_length_mm", "focus_distance_m"
    ],
    "session_events": [
        "timestamp_ms", "event_type", "event_description", "device_id",
        "phase", "sync_verified", "offset_ms"
    ]
}

# Standard sampling rates for academic compliance
STANDARD_SAMPLING_RATES = {
    "shimmer_gsr": 128.0,     # Hz
    "thermal_camera": 25.0,   # Hz  
    "rgb_camera": 30.0,       # fps
    "audio": 44100.0          # Hz
}


def create_standard_session_config(
    session_name: str, 
    participant_id: str, 
    researcher_id: str,
    experiment_type: str = "gsr_measurement",
    duration_minutes: int = 10
) -> SessionConfig:
    """Create a standardized session configuration with recommended defaults."""
    session_id = f"{session_name}_{int(time.time())}"
    
    return SessionConfig(
        session_id=session_id,
        session_name=session_name,
        participant_id=participant_id,
        researcher_id=researcher_id,
        experiment_type=experiment_type,
        expected_duration_minutes=duration_minutes,
        devices_enabled=[
            DeviceType.SHIMMER_GSR,
            DeviceType.THERMAL_CAMERA,
            DeviceType.RGB_CAMERA
        ],
        sampling_rates=STANDARD_SAMPLING_RATES.copy(),
        video_quality="1080p",
        audio_enabled=True
    )


def create_standard_calibration_pattern() -> CalibrationPattern:
    """Create standardized calibration pattern for academic compliance."""
    return CalibrationPattern(
        pattern_type="chessboard",
        pattern_size=(9, 6),
        square_size_mm=25.0
    )