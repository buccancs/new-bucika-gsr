"""
Common network protocols and message formats.

This module consolidates network communication patterns used between
Android and Python applications to ensure consistency.
"""

import json
import time
from dataclasses import dataclass, asdict
from typing import Dict, Any, Optional, List
from enum import Enum

from .data_structures import DeviceInfo, DeviceState, SessionConfig, SensorSample


class MessageType(Enum):
    """Standardized message types for network communication."""
    # Connection management
    HELLO = "hello"
    GOODBYE = "goodbye"
    HEARTBEAT = "heartbeat"
    
    # Session management
    SESSION_START = "session_start"
    SESSION_STOP = "session_stop"
    SESSION_STATUS = "session_status"
    
    # Device management
    DEVICE_DISCOVERY = "device_discovery"
    DEVICE_STATUS = "device_status"
    DEVICE_CONNECT = "device_connect"
    DEVICE_DISCONNECT = "device_disconnect"
    
    # Data streaming
    DATA_SAMPLE = "data_sample"
    DATA_BATCH = "data_batch"
    
    # Calibration
    CALIBRATION_START = "calibration_start"
    CALIBRATION_CAPTURE = "calibration_capture"
    CALIBRATION_COMPLETE = "calibration_complete"
    
    # Commands
    COMMAND = "command"
    RESPONSE = "response"
    ERROR = "error"


@dataclass
class BaseMessage:
    """Base message structure for all network communications."""
    message_type: MessageType
    timestamp: float = None
    session_id: Optional[str] = None
    device_id: Optional[str] = None
    
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = time.time()
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization."""
        result = asdict(self)
        result["message_type"] = self.message_type.value
        return result
    
    def to_json(self) -> str:
        """Convert to JSON string."""
        return json.dumps(self.to_dict())
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "BaseMessage":
        """Create from dictionary."""
        data["message_type"] = MessageType(data["message_type"])
        return cls(**data)


@dataclass
class HelloMessage(BaseMessage):
    """Device introduction message."""
    device_info: DeviceInfo
    capabilities: List[str]
    protocol_version: str = "1.0"
    
    def __post_init__(self):
        super().__post_init__()
        self.message_type = MessageType.HELLO
        self.device_id = self.device_info.device_id


@dataclass
class DeviceStatusMessage(BaseMessage):
    """Device status update message."""
    device_state: DeviceState
    battery_level: Optional[float] = None
    error_message: Optional[str] = None
    additional_info: Dict[str, Any] = None
    
    def __post_init__(self):
        super().__post_init__()
        self.message_type = MessageType.DEVICE_STATUS
        if self.additional_info is None:
            self.additional_info = {}


@dataclass
class SessionControlMessage(BaseMessage):
    """Session control message (start/stop)."""
    action: str  # "start" or "stop"
    session_config: Optional[SessionConfig] = None
    
    def __post_init__(self):
        super().__post_init__()
        if self.action == "start":
            self.message_type = MessageType.SESSION_START
        elif self.action == "stop":
            self.message_type = MessageType.SESSION_STOP


@dataclass
class DataMessage(BaseMessage):
    """Data streaming message."""
    samples: List[SensorSample]
    batch_id: Optional[str] = None
    
    def __post_init__(self):
        super().__post_init__()
        if len(self.samples) == 1:
            self.message_type = MessageType.DATA_SAMPLE
        else:
            self.message_type = MessageType.DATA_BATCH


@dataclass
class CalibrationMessage(BaseMessage):
    """Calibration-related message."""
    action: str  # "start", "capture", "complete"
    pattern_info: Optional[Dict[str, Any]] = None
    image_data: Optional[str] = None  # base64 encoded
    results: Optional[Dict[str, Any]] = None
    
    def __post_init__(self):
        super().__post_init__()
        if self.action == "start":
            self.message_type = MessageType.CALIBRATION_START
        elif self.action == "capture":
            self.message_type = MessageType.CALIBRATION_CAPTURE
        elif self.action == "complete":
            self.message_type = MessageType.CALIBRATION_COMPLETE


@dataclass
class CommandMessage(BaseMessage):
    """Command message for device control."""
    command: str
    parameters: Dict[str, Any] = None
    
    def __post_init__(self):
        super().__post_init__()
        self.message_type = MessageType.COMMAND
        if self.parameters is None:
            self.parameters = {}


@dataclass
class ResponseMessage(BaseMessage):
    """Response to a command message."""
    original_command: str
    success: bool
    result: Optional[Dict[str, Any]] = None
    error_message: Optional[str] = None
    
    def __post_init__(self):
        super().__post_init__()
        self.message_type = MessageType.RESPONSE


@dataclass
class ErrorMessage(BaseMessage):
    """Error notification message."""
    error_code: str
    error_message: str
    context: Optional[Dict[str, Any]] = None
    
    def __post_init__(self):
        super().__post_init__()
        self.message_type = MessageType.ERROR


def create_message_from_json(json_str: str) -> Optional[BaseMessage]:
    """Create appropriate message object from JSON string."""
    try:
        data = json.loads(json_str)
        message_type = MessageType(data.get("message_type"))
        
        if message_type == MessageType.HELLO:
            return HelloMessage.from_dict(data)
        elif message_type in [MessageType.DEVICE_STATUS]:
            return DeviceStatusMessage.from_dict(data)
        elif message_type in [MessageType.SESSION_START, MessageType.SESSION_STOP]:
            return SessionControlMessage.from_dict(data)
        elif message_type in [MessageType.DATA_SAMPLE, MessageType.DATA_BATCH]:
            return DataMessage.from_dict(data)
        elif message_type in [MessageType.CALIBRATION_START, MessageType.CALIBRATION_CAPTURE, MessageType.CALIBRATION_COMPLETE]:
            return CalibrationMessage.from_dict(data)
        elif message_type == MessageType.COMMAND:
            return CommandMessage.from_dict(data)
        elif message_type == MessageType.RESPONSE:
            return ResponseMessage.from_dict(data)
        elif message_type == MessageType.ERROR:
            return ErrorMessage.from_dict(data)
        else:
            return BaseMessage.from_dict(data)
            
    except (json.JSONDecodeError, KeyError, ValueError) as e:
        return ErrorMessage(
            error_code="INVALID_MESSAGE",
            error_message=f"Failed to parse message: {e}",
            context={"raw_data": json_str[:200]}  # First 200 chars for debugging
        )


# Standard commands
STANDARD_COMMANDS = {
    "PING": "ping",
    "GET_STATUS": "get_status",
    "START_STREAMING": "start_streaming", 
    "STOP_STREAMING": "stop_streaming",
    "CALIBRATE": "calibrate",
    "SYNC_TIME": "sync_time",
    "GET_BATTERY": "get_battery",
    "SET_SAMPLING_RATE": "set_sampling_rate",
    "RESTART_DEVICE": "restart_device"
}

# Standard error codes
STANDARD_ERROR_CODES = {
    "DEVICE_NOT_FOUND": "E001",
    "CONNECTION_FAILED": "E002", 
    "INVALID_COMMAND": "E003",
    "DEVICE_BUSY": "E004",
    "LOW_BATTERY": "E005",
    "CALIBRATION_FAILED": "E006",
    "DATA_CORRUPTION": "E007",
    "TIMEOUT": "E008",
    "PERMISSION_DENIED": "E009",
    "INVALID_SESSION": "E010"
}


def create_standard_hello_message(device_info: DeviceInfo) -> HelloMessage:
    """Create a standardized hello message."""
    return HelloMessage(
        device_info=device_info,
        capabilities=device_info.capabilities,
        protocol_version="1.0"
    )


def create_error_response(command: str, error_code: str, error_message: str) -> ResponseMessage:
    """Create a standardized error response."""
    return ResponseMessage(
        original_command=command,
        success=False,
        error_message=f"{error_code}: {error_message}"
    )


def create_success_response(command: str, result: Dict[str, Any] = None) -> ResponseMessage:
    """Create a standardized success response."""
    return ResponseMessage(
        original_command=command,
        success=True,
        result=result or {}
    )