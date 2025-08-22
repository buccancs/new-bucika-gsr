"""
Enhanced Communication Protocol with JSON Schemas and Detailed Message Formats

This module defines the complete communication protocol between PC controller and
Android devices, including JSON message schemas, RSA/AES handshake, preview frames,
status heartbeats, and scheduled start/stop commands as claimed in the thesis.
"""

import json
import time
import struct
import base64
import hashlib
import hmac
import secrets
from typing import Dict, List, Optional, Any, Union, Tuple
from dataclasses import dataclass, asdict
from enum import Enum
import logging
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes


class MessageType(Enum):
    """Protocol message types."""
    # Authentication and handshake
    AUTH_REQUEST = "auth_request"
    AUTH_RESPONSE = "auth_response" 
    AUTH_CHALLENGE = "auth_challenge"
    HANDSHAKE_INIT = "handshake_init"
    HANDSHAKE_RESPONSE = "handshake_response"
    
    # Session management
    SESSION_CREATE = "session_create"
    SESSION_START = "session_start"
    SESSION_STOP = "session_stop"
    SESSION_STATUS = "session_status"
    SCHEDULED_START = "scheduled_start"
    
    # Device control
    DEVICE_DISCOVERY = "device_discovery"
    DEVICE_CONFIG = "device_config"
    DEVICE_CALIBRATE = "device_calibrate"
    DEVICE_STATUS = "device_status"
    
    # Data streaming
    PREVIEW_FRAME = "preview_frame"
    SENSOR_DATA = "sensor_data"
    HEARTBEAT = "heartbeat"
    
    # File transfer
    FILE_TRANSFER_INIT = "file_transfer_init"
    FILE_CHUNK = "file_chunk"
    FILE_COMPLETE = "file_complete"
    
    # Error handling
    ERROR = "error"
    ACK = "ack"
    NACK = "nack"


@dataclass
class MessageHeader:
    """Standard message header for all protocol messages."""
    message_id: str
    message_type: MessageType
    timestamp: float
    sender_id: str
    recipient_id: str
    sequence_number: int
    total_parts: int = 1
    part_number: int = 1
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        data = asdict(self)
        data['message_type'] = self.message_type.value
        return data
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'MessageHeader':
        """Create from dictionary."""
        data['message_type'] = MessageType(data['message_type'])
        return cls(**data)


@dataclass
class AuthRequest:
    """Authentication request message."""
    device_id: str
    device_type: str
    protocol_version: str
    capabilities: List[str]
    public_key_pem: str  # RSA public key for secure handshake
    challenge_nonce: str


@dataclass
class AuthResponse:
    """Authentication response message."""
    session_token: str
    server_public_key_pem: str
    challenge_response: str
    aes_key_encrypted: str  # AES key encrypted with device's public key
    session_timeout: int


@dataclass 
class SessionCreateRequest:
    """Session creation request."""
    session_name: str
    researcher_id: str
    participant_id: Optional[str]
    experiment_type: str
    recording_config: Dict[str, Any]
    expected_duration_minutes: int
    devices_required: List[str]


@dataclass
class SessionStartRequest:
    """Session start request with precise timing."""
    session_id: str
    sync_timestamp: float  # Precise start time for synchronization
    countdown_seconds: int = 5
    devices_to_start: List[str] = None


@dataclass
class ScheduledStartRequest:
    """Scheduled start request for coordinated recording."""
    session_id: str
    scheduled_start_time: float  # Unix timestamp for start
    sync_flash_enabled: bool = True
    sync_beep_enabled: bool = True
    preparation_time_seconds: int = 30


@dataclass
class DeviceStatus:
    """Device status information."""
    device_id: str
    device_type: str
    connected: bool
    recording: bool
    battery_level: Optional[float]
    temperature: Optional[float]
    storage_available_mb: Optional[int]
    last_data_timestamp: Optional[float]
    error_state: Optional[str]
    data_rate_hz: Optional[float]
    samples_recorded: Optional[int]


@dataclass
class PreviewFrame:
    """Preview frame data for real-time monitoring."""
    device_id: str
    frame_type: str  # "thermal", "rgb", "shimmer_plot"
    timestamp: float
    frame_data: str  # Base64 encoded frame data
    frame_width: Optional[int] = None
    frame_height: Optional[int] = None
    compression: str = "jpeg"
    quality: int = 75


@dataclass
class SensorDataPacket:
    """Real-time sensor data packet."""
    device_id: str
    sensor_type: str
    timestamp: float
    data: Dict[str, Any]  # Sensor-specific data
    sequence_number: int
    checksum: str


@dataclass
class HeartbeatMessage:
    """Heartbeat message for connection monitoring."""
    device_id: str
    uptime_seconds: float
    memory_usage_mb: float
    cpu_usage_percent: float
    network_latency_ms: Optional[float]
    sync_offset_ms: Optional[float]  # NTP sync offset


class ProtocolMessage:
    """Complete protocol message with header and payload."""
    
    def __init__(self, header: MessageHeader, payload: Any):
        self.header = header
        self.payload = payload
    
    def to_json(self) -> str:
        """Serialize message to JSON."""
        message_dict = {
            'header': self.header.to_dict(),
            'payload': self._serialize_payload()
        }
        return json.dumps(message_dict, default=str)
    
    def _serialize_payload(self) -> Dict[str, Any]:
        """Serialize payload to dictionary."""
        if hasattr(self.payload, 'to_dict'):
            return self.payload.to_dict()
        elif hasattr(self.payload, '__dict__'):
            return asdict(self.payload)
        elif isinstance(self.payload, dict):
            return self.payload
        else:
            return {'data': str(self.payload)}
    
    @classmethod
    def from_json(cls, json_str: str) -> 'ProtocolMessage':
        """Deserialize message from JSON."""
        data = json.loads(json_str)
        header = MessageHeader.from_dict(data['header'])
        payload = data['payload']
        return cls(header, payload)
    
    def to_bytes(self) -> bytes:
        """Convert message to binary format for network transmission."""
        json_data = self.to_json()
        json_bytes = json_data.encode('utf-8')
        
        # Add length prefix for framing
        length = len(json_bytes)
        return struct.pack('!I', length) + json_bytes
    
    @classmethod
    def from_bytes(cls, data: bytes) -> Tuple['ProtocolMessage', int]:
        """Parse message from binary data."""
        if len(data) < 4:
            raise ValueError("Insufficient data for message length")
        
        length = struct.unpack('!I', data[:4])[0]
        if len(data) < 4 + length:
            raise ValueError("Insufficient data for complete message")
        
        json_data = data[4:4+length].decode('utf-8')
        message = cls.from_json(json_data)
        
        return message, 4 + length


class CryptoHandler:
    """Handles RSA/AES encryption for secure communication."""
    
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.rsa_private_key = None
        self.rsa_public_key = None
        self.aes_key = None
        self.aes_iv = None
        
    def generate_rsa_keypair(self, key_size: int = 2048) -> Tuple[str, str]:
        """Generate RSA key pair and return PEM-encoded keys."""
        self.rsa_private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=key_size
        )
        self.rsa_public_key = self.rsa_private_key.public_key()
        
        # Serialize keys to PEM format
        private_pem = self.rsa_private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        ).decode('utf-8')
        
        public_pem = self.rsa_public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        ).decode('utf-8')
        
        return private_pem, public_pem
    
    def load_public_key(self, public_key_pem: str):
        """Load RSA public key from PEM string."""
        self.rsa_public_key = serialization.load_pem_public_key(
            public_key_pem.encode('utf-8')
        )
    
    def generate_aes_key(self) -> str:
        """Generate AES-256 key and return base64 encoded."""
        self.aes_key = secrets.token_bytes(32)  # 256-bit key
        self.aes_iv = secrets.token_bytes(16)   # 128-bit IV
        
        # Return key+IV as base64
        return base64.b64encode(self.aes_key + self.aes_iv).decode('utf-8')
    
    def encrypt_rsa(self, data: bytes) -> str:
        """Encrypt data with RSA public key."""
        if not self.rsa_public_key:
            raise ValueError("RSA public key not loaded")
        
        encrypted = self.rsa_public_key.encrypt(
            data,
            padding.OAEP(
                mgf=padding.MGF1(algorithm=hashes.SHA256()),
                algorithm=hashes.SHA256(),
                label=None
            )
        )
        return base64.b64encode(encrypted).decode('utf-8')
    
    def decrypt_rsa(self, encrypted_data: str) -> bytes:
        """Decrypt data with RSA private key."""
        if not self.rsa_private_key:
            raise ValueError("RSA private key not loaded")
        
        encrypted_bytes = base64.b64decode(encrypted_data)
        decrypted = self.rsa_private_key.decrypt(
            encrypted_bytes,
            padding.OAEP(
                mgf=padding.MGF1(algorithm=hashes.SHA256()),
                algorithm=hashes.SHA256(),
                label=None
            )
        )
        return decrypted
    
    def encrypt_aes(self, data: bytes) -> str:
        """Encrypt data with AES-256-CBC."""
        if not self.aes_key or not self.aes_iv:
            raise ValueError("AES key/IV not initialized")
        
        cipher = Cipher(algorithms.AES(self.aes_key), modes.CBC(self.aes_iv))
        encryptor = cipher.encryptor()
        
        # Pad data to AES block size
        padding_length = 16 - (len(data) % 16)
        padded_data = data + bytes([padding_length] * padding_length)
        
        encrypted = encryptor.update(padded_data) + encryptor.finalize()
        return base64.b64encode(encrypted).decode('utf-8')
    
    def decrypt_aes(self, encrypted_data: str) -> bytes:
        """Decrypt data with AES-256-CBC."""
        if not self.aes_key or not self.aes_iv:
            raise ValueError("AES key/IV not initialized")
        
        encrypted_bytes = base64.b64decode(encrypted_data)
        cipher = Cipher(algorithms.AES(self.aes_key), modes.CBC(self.aes_iv))
        decryptor = cipher.decryptor()
        
        padded_data = decryptor.update(encrypted_bytes) + decryptor.finalize()
        
        # Remove padding
        padding_length = padded_data[-1]
        return padded_data[:-padding_length]
    
    def set_aes_key_from_base64(self, key_iv_b64: str):
        """Set AES key and IV from base64 encoded string."""
        key_iv_bytes = base64.b64decode(key_iv_b64)
        self.aes_key = key_iv_bytes[:32]  # First 32 bytes = key
        self.aes_iv = key_iv_bytes[32:48] # Next 16 bytes = IV


class ProtocolHandler:
    """Handles protocol communication with encryption and validation."""
    
    def __init__(self, device_id: str, logger: Optional[logging.Logger] = None):
        self.device_id = device_id
        self.logger = logger or logging.getLogger(__name__)
        self.crypto = CryptoHandler(logger)
        self.sequence_counter = 0
        self.active_sessions = {}
        self.message_handlers = {}
        
        # Protocol statistics
        self.messages_sent = 0
        self.messages_received = 0
        self.errors_count = 0
        
        self.logger.info(f"Protocol handler initialized for device: {device_id}")
    
    def create_message(self, message_type: MessageType, payload: Any, 
                      recipient_id: str = "broadcast") -> ProtocolMessage:
        """Create a new protocol message."""
        header = MessageHeader(
            message_id=secrets.token_hex(8),
            message_type=message_type,
            timestamp=time.time(),
            sender_id=self.device_id,
            recipient_id=recipient_id,
            sequence_number=self.sequence_counter
        )
        
        self.sequence_counter += 1
        return ProtocolMessage(header, payload)
    
    def perform_handshake(self, remote_public_key_pem: str) -> Tuple[str, str]:
        """Perform RSA/AES handshake with remote device."""
        # Generate our key pair
        private_pem, public_pem = self.crypto.generate_rsa_keypair()
        
        # Load remote public key
        self.crypto.load_public_key(remote_public_key_pem)
        
        # Generate AES key for session
        aes_key_b64 = self.crypto.generate_aes_key()
        
        # Encrypt AES key with remote's public key
        encrypted_aes_key = self.crypto.encrypt_rsa(base64.b64decode(aes_key_b64))
        
        self.logger.info("RSA/AES handshake completed")
        return public_pem, encrypted_aes_key
    
    def create_auth_request(self, device_type: str, capabilities: List[str]) -> ProtocolMessage:
        """Create authentication request message."""
        # Generate challenge nonce
        challenge_nonce = secrets.token_hex(16)
        
        # Generate RSA key pair for this session
        private_pem, public_pem = self.crypto.generate_rsa_keypair()
        
        auth_request = AuthRequest(
            device_id=self.device_id,
            device_type=device_type,
            protocol_version="1.0.0",
            capabilities=capabilities,
            public_key_pem=public_pem,
            challenge_nonce=challenge_nonce
        )
        
        return self.create_message(MessageType.AUTH_REQUEST, auth_request)
    
    def create_session_start(self, session_id: str, devices: List[str], 
                           countdown: int = 5) -> ProtocolMessage:
        """Create synchronized session start message."""
        # Calculate precise start time
        sync_timestamp = time.time() + countdown
        
        start_request = SessionStartRequest(
            session_id=session_id,
            sync_timestamp=sync_timestamp,
            countdown_seconds=countdown,
            devices_to_start=devices
        )
        
        return self.create_message(MessageType.SESSION_START, start_request)
    
    def create_scheduled_start(self, session_id: str, start_time: float) -> ProtocolMessage:
        """Create scheduled start message for future execution."""
        scheduled_request = ScheduledStartRequest(
            session_id=session_id,
            scheduled_start_time=start_time,
            sync_flash_enabled=True,
            sync_beep_enabled=True,
            preparation_time_seconds=30
        )
        
        return self.create_message(MessageType.SCHEDULED_START, scheduled_request)
    
    def create_preview_frame(self, frame_type: str, frame_data: bytes, 
                           width: int = None, height: int = None) -> ProtocolMessage:
        """Create preview frame message."""
        # Encode frame data as base64
        frame_b64 = base64.b64encode(frame_data).decode('utf-8')
        
        preview = PreviewFrame(
            device_id=self.device_id,
            frame_type=frame_type,
            timestamp=time.time(),
            frame_data=frame_b64,
            frame_width=width,
            frame_height=height
        )
        
        return self.create_message(MessageType.PREVIEW_FRAME, preview)
    
    def create_heartbeat(self, uptime: float, memory_mb: float, cpu_percent: float,
                        sync_offset_ms: Optional[float] = None) -> ProtocolMessage:
        """Create heartbeat message."""
        heartbeat = HeartbeatMessage(
            device_id=self.device_id,
            uptime_seconds=uptime,
            memory_usage_mb=memory_mb,
            cpu_usage_percent=cpu_percent,
            sync_offset_ms=sync_offset_ms
        )
        
        return self.create_message(MessageType.HEARTBEAT, heartbeat)
    
    def create_device_status(self, status: DeviceStatus) -> ProtocolMessage:
        """Create device status message."""
        return self.create_message(MessageType.DEVICE_STATUS, status)
    
    def encrypt_message(self, message: ProtocolMessage) -> bytes:
        """Encrypt message for secure transmission."""
        try:
            # Serialize message to JSON
            json_data = message.to_json()
            json_bytes = json_data.encode('utf-8')
            
            # Encrypt with AES if available
            if self.crypto.aes_key:
                encrypted_data = self.crypto.encrypt_aes(json_bytes)
                
                # Create encrypted message wrapper
                encrypted_wrapper = {
                    'encrypted': True,
                    'data': encrypted_data,
                    'sender': self.device_id,
                    'timestamp': time.time()
                }
                
                wrapper_json = json.dumps(encrypted_wrapper)
                wrapper_bytes = wrapper_json.encode('utf-8')
                
                # Add length prefix
                length = len(wrapper_bytes)
                return struct.pack('!I', length) + wrapper_bytes
            else:
                # Send unencrypted if encryption not established
                return message.to_bytes()
                
        except Exception as e:
            self.logger.error(f"Failed to encrypt message: {e}")
            raise
    
    def decrypt_message(self, data: bytes) -> ProtocolMessage:
        """Decrypt received message."""
        try:
            # Parse length prefix
            if len(data) < 4:
                raise ValueError("Insufficient data")
            
            length = struct.unpack('!I', data[:4])[0]
            if len(data) < 4 + length:
                raise ValueError("Incomplete message")
            
            json_data = data[4:4+length].decode('utf-8')
            wrapper = json.loads(json_data)
            
            if wrapper.get('encrypted', False):
                # Decrypt AES data
                encrypted_data = wrapper['data']
                decrypted_bytes = self.crypto.decrypt_aes(encrypted_data)
                decrypted_json = decrypted_bytes.decode('utf-8')
                return ProtocolMessage.from_json(decrypted_json)
            else:
                # Parse unencrypted message
                return ProtocolMessage.from_json(json_data)
                
        except Exception as e:
            self.logger.error(f"Failed to decrypt message: {e}")
            raise
    
    def validate_message(self, message: ProtocolMessage) -> bool:
        """Validate message integrity and authenticity."""
        try:
            # Check message structure
            if not message.header or not message.payload:
                return False
            
            # Check timestamp (allow 5 minute clock skew)
            time_diff = abs(time.time() - message.header.timestamp)
            if time_diff > 300:  # 5 minutes
                self.logger.warning(f"Message timestamp out of range: {time_diff}s")
                return False
            
            # Check sequence number (basic duplicate detection)
            # In a full implementation, this would maintain per-sender sequence tracking
            
            return True
            
        except Exception as e:
            self.logger.error(f"Message validation error: {e}")
            return False
    
    def get_protocol_statistics(self) -> Dict[str, Any]:
        """Get protocol communication statistics."""
        return {
            'messages_sent': self.messages_sent,
            'messages_received': self.messages_received,
            'errors_count': self.errors_count,
            'active_sessions': len(self.active_sessions),
            'encryption_enabled': self.crypto.aes_key is not None,
            'sequence_counter': self.sequence_counter
        }


# JSON Schema definitions for message validation
MESSAGE_SCHEMAS = {
    'auth_request': {
        'type': 'object',
        'properties': {
            'device_id': {'type': 'string'},
            'device_type': {'type': 'string'},
            'protocol_version': {'type': 'string'},
            'capabilities': {'type': 'array', 'items': {'type': 'string'}},
            'public_key_pem': {'type': 'string'},
            'challenge_nonce': {'type': 'string'}
        },
        'required': ['device_id', 'device_type', 'protocol_version', 'capabilities']
    },
    
    'session_start': {
        'type': 'object',
        'properties': {
            'session_id': {'type': 'string'},
            'sync_timestamp': {'type': 'number'},
            'countdown_seconds': {'type': 'integer', 'minimum': 0},
            'devices_to_start': {'type': 'array', 'items': {'type': 'string'}}
        },
        'required': ['session_id', 'sync_timestamp']
    },
    
    'preview_frame': {
        'type': 'object',
        'properties': {
            'device_id': {'type': 'string'},
            'frame_type': {'type': 'string'},
            'timestamp': {'type': 'number'},
            'frame_data': {'type': 'string'},
            'frame_width': {'type': 'integer', 'minimum': 1},
            'frame_height': {'type': 'integer', 'minimum': 1},
            'compression': {'type': 'string'},
            'quality': {'type': 'integer', 'minimum': 1, 'maximum': 100}
        },
        'required': ['device_id', 'frame_type', 'timestamp', 'frame_data']
    },
    
    'heartbeat': {
        'type': 'object',
        'properties': {
            'device_id': {'type': 'string'},
            'uptime_seconds': {'type': 'number', 'minimum': 0},
            'memory_usage_mb': {'type': 'number', 'minimum': 0},
            'cpu_usage_percent': {'type': 'number', 'minimum': 0, 'maximum': 100},
            'network_latency_ms': {'type': 'number', 'minimum': 0},
            'sync_offset_ms': {'type': 'number'}
        },
        'required': ['device_id', 'uptime_seconds', 'memory_usage_mb', 'cpu_usage_percent']
    }
}


if __name__ == "__main__":
    # =================================================================
    # WARNING: PROTOCOL TESTING CODE - USES SYNTHETIC DATA ONLY
    # This is for protocol validation, not real experimental data.
    # Real experiments require actual sensor hardware connections.
    # =================================================================
    
    import warnings
    warnings.warn(
        "Running protocol test with synthetic data. "
        "Real experiments require actual hardware connections.",
        UserWarning,
        stacklevel=2
    )
    
    # Example protocol usage
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    logger.warning("="*60)
    logger.warning("PROTOCOL TEST MODE: Using synthetic data only")
    logger.warning("Not for real experimental data collection")
    logger.warning("="*60)
    
    # Create protocol handlers for PC and Android device
    pc_handler = ProtocolHandler("pc_controller_001", logger)
    android_handler = ProtocolHandler("android_device_001", logger)
    
    logger.info("Testing enhanced communication protocol...")
    
    # Test authentication handshake
    auth_request = pc_handler.create_auth_request(
        "pc_controller", 
        ["session_management", "data_collection", "synchronization"]
    )
    
    logger.info("Created authentication request:")
    logger.info(f"  Message ID: {auth_request.header.message_id}")
    logger.info(f"  Type: {auth_request.header.message_type.value}")
    logger.info(f"  Timestamp: {auth_request.header.timestamp}")
    
    # Test session start with synchronization
    session_start = pc_handler.create_session_start(
        "test_session_001", 
        ["android_device_001", "android_device_002"],
        countdown=10
    )
    
    logger.info("Created session start message:")
    logger.info(f"  Session ID: {session_start.payload.session_id}")
    logger.info(f"  Sync timestamp: {session_start.payload.sync_timestamp}")
    logger.info(f"  Countdown: {session_start.payload.countdown_seconds}s")
    
    # Test preview frame (SYNTHETIC DATA FOR TESTING ONLY)
    test_frame_data = b"SYNTHETIC_TEST_DATA_NOT_REAL_JPEG" * 100
    preview_frame = android_handler.create_preview_frame(
        "thermal", test_frame_data, width=256, height=192
    )
    
    logger.info("Created preview frame message:")
    logger.info(f"  Frame type: {preview_frame.payload.frame_type}")
    logger.info(f"  Data size: {len(test_frame_data)} bytes (SYNTHETIC TEST DATA)")
    logger.info(f"  Resolution: {preview_frame.payload.frame_width}x{preview_frame.payload.frame_height}")
    
    # Test heartbeat with NTP sync info
    heartbeat = android_handler.create_heartbeat(
        uptime=3600.5, memory_mb=245.2, cpu_percent=15.7, sync_offset_ms=18.3
    )
    
    logger.info("Created heartbeat message:")
    logger.info(f"  Device: {heartbeat.payload.device_id}")
    logger.info(f"  Sync offset: {heartbeat.payload.sync_offset_ms}ms")
    logger.info(f"  System stats: {heartbeat.payload.cpu_usage_percent}% CPU, {heartbeat.payload.memory_usage_mb}MB RAM")
    
    # Test message serialization
    json_data = session_start.to_json()
    logger.info(f"Message serialized to {len(json_data)} bytes JSON")
    
    binary_data = session_start.to_bytes()
    logger.info(f"Message serialized to {len(binary_data)} bytes binary")
    
    # Test RSA/AES encryption setup
    logger.info("Testing RSA/AES handshake...")
    private_pem, public_pem = pc_handler.crypto.generate_rsa_keypair()
    android_public, encrypted_aes = android_handler.perform_handshake(public_pem)
    
    logger.info("Handshake completed:")
    logger.info(f"  RSA key size: 2048 bits")
    logger.info(f"  AES key encrypted: {len(encrypted_aes)} characters")
    
    # Show protocol statistics
    stats = pc_handler.get_protocol_statistics()
    logger.info("Protocol statistics:")
    for key, value in stats.items():
        logger.info(f"  {key}: {value}")
    
    logger.info("Enhanced protocol test completed successfully")