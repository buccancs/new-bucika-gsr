"""
Protocol messages for WebSocket communication between PC orchestrator and Android clients.
Maintains compatibility with the existing Kotlin implementation.
"""

from abc import ABC, abstractmethod
from enum import Enum
from typing import Any, Dict, List, Optional, Union
from pydantic import BaseModel
import time


class MessageType(str, Enum):
    """Message types for WebSocket communication"""
    HELLO = "HELLO"
    REGISTER = "REGISTER"
    PING = "PING"
    PONG = "PONG"
    START = "START"
    STOP = "STOP"
    SYNC_MARK = "SYNC_MARK"
    ACK = "ACK"
    ERROR = "ERROR"
    GSR_SAMPLE = "GSR_SAMPLE"
    UPLOAD_BEGIN = "UPLOAD_BEGIN"
    UPLOAD_CHUNK = "UPLOAD_CHUNK"
    UPLOAD_END = "UPLOAD_END"


class MessagePayload(BaseModel, ABC):
    """Base class for all message payloads"""
    pass


class MessageEnvelope(BaseModel):
    """Standard message envelope for WebSocket communication"""
    id: str
    type: MessageType
    ts: int  # nanoseconds since Unix epoch
    sessionId: Optional[str] = None
    deviceId: str
    payload: Dict[str, Any]  # Will be parsed to specific payload type
    
    @classmethod
    def create(cls, 
               msg_id: str,
               msg_type: MessageType, 
               device_id: str,
               payload: MessagePayload,
               session_id: Optional[str] = None) -> 'MessageEnvelope':
        """Create a message envelope with current timestamp"""
        return cls(
            id=msg_id,
            type=msg_type,
            ts=time.time_ns(),
            sessionId=session_id,
            deviceId=device_id,
            payload=payload.model_dump()
        )


# Payload implementations
class EmptyPayload(MessagePayload):
    """Empty payload for simple messages like PING/PONG"""
    pass


class HelloPayload(MessagePayload):
    """Initial hello message from Android client"""
    deviceName: str
    capabilities: List[str]
    batteryLevel: int
    version: str


class RegisterPayload(MessagePayload):
    """Device registration response from PC"""
    accepted: bool
    reason: Optional[str] = None
    syncPort: int = 9123  # Time sync UDP port


class StartPayload(MessagePayload):
    """Start session message"""
    sessionName: str
    participantId: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None


class SyncMarkPayload(MessagePayload):
    """Synchronization marker"""
    markId: str
    description: Optional[str] = None


class AckPayload(MessagePayload):
    """Acknowledgment message"""
    messageId: str
    status: str = "OK"


class ErrorPayload(MessagePayload):
    """Error message"""
    code: str
    message: str
    details: Optional[Dict[str, Any]] = None


class GSRSample(BaseModel):
    """Individual GSR sample data"""
    t_mono_ns: int
    t_utc_ns: int 
    seq: int
    gsr_raw_uS: float
    gsr_filt_uS: float
    temp_C: float
    flag_spike: bool = False
    flag_sat: bool = False
    flag_dropout: bool = False


class GSRSamplePayload(MessagePayload):
    """GSR sample data message"""
    samples: List[GSRSample]


class UploadBeginPayload(MessagePayload):
    """Begin file upload message"""
    filename: str
    fileSize: int
    md5Hash: str
    chunkSize: int = 8192


class UploadChunkPayload(MessagePayload):
    """File upload chunk message"""
    filename: str
    chunkIndex: int
    data: str  # Base64 encoded chunk data
    isLast: bool = False


class UploadEndPayload(MessagePayload):
    """End file upload message"""
    filename: str
    totalChunks: int
    success: bool
    message: Optional[str] = None


# Payload type mapping for deserialization
PAYLOAD_TYPE_MAP = {
    MessageType.HELLO: HelloPayload,
    MessageType.REGISTER: RegisterPayload,
    MessageType.PING: EmptyPayload,
    MessageType.PONG: EmptyPayload,
    MessageType.START: StartPayload,
    MessageType.STOP: EmptyPayload,
    MessageType.SYNC_MARK: SyncMarkPayload,
    MessageType.ACK: AckPayload,
    MessageType.ERROR: ErrorPayload,
    MessageType.GSR_SAMPLE: GSRSamplePayload,
    MessageType.UPLOAD_BEGIN: UploadBeginPayload,
    MessageType.UPLOAD_CHUNK: UploadChunkPayload,
    MessageType.UPLOAD_END: UploadEndPayload,
}


def parse_message_payload(envelope: MessageEnvelope) -> MessagePayload:
    """Parse message payload based on message type"""
    payload_class = PAYLOAD_TYPE_MAP.get(envelope.type)
    if not payload_class:
        raise ValueError(f"Unknown message type: {envelope.type}")
    
    return payload_class(**envelope.payload)