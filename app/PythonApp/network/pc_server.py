import asyncio
import json
import logging
import socket
import struct
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Set, Tuple

# Import new security and discovery features
try:
    from .security import SecurityConfig, TokenManager, SecureSocketWrapper, RuntimeSecurityChecker
    from .zeroconf_discovery import MultiSensorDiscovery
    from .lsl_integration import LSLStreamer, DefaultLSLStreams, push_sync_marker
    from .file_integrity import SecureFileTransfer, FileIntegrityVerifier
    ENHANCED_FEATURES_AVAILABLE = True
except ImportError:
    ENHANCED_FEATURES_AVAILABLE = False
@dataclass
class ConnectedDevice:
    device_id: str
    capabilities: List[str]
    connection_time: float
    last_heartbeat: float
    status: Dict[str, Any]
    socket: socket.socket
    address: tuple
@dataclass
class JsonMessage:
    type: str = ""
    timestamp: float = None
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = time.time()
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)
    def to_json(self) -> str:
        return json.dumps(self.to_dict())
    @classmethod
    def from_json(cls, json_str: str) -> Optional["JsonMessage"]:
        try:
            data = json.loads(json_str)
            message_type = data.get("type")
            if message_type == "hello":
                return HelloMessage.from_dict(data)
            elif message_type == "status":
                return StatusMessage.from_dict(data)
            elif message_type == "sensor_data":
                return SensorDataMessage.from_dict(data)
            elif message_type == "ack":
                return AckMessage.from_dict(data)
            elif message_type == "file_info":
                return FileInfoMessage.from_dict(data)
            elif message_type == "file_chunk":
                return FileChunkMessage.from_dict(data)
            elif message_type == "file_end":
                return FileEndMessage.from_dict(data)
            else:
                return JsonMessage(
                    type=message_type, **{k: v for k, v in data.items() if k != "type"}
                )
        except (json.JSONDecodeError, KeyError, TypeError) as e:
            logging.error(f"Error parsing JSON message: {e}")
            return None
@dataclass
class HelloMessage(JsonMessage):
    device_id: str = ""
    capabilities: List[str] = None
    def __post_init__(self):
        if not hasattr(self, "type") or not self.type:
            self.type = "hello"
        super().__post_init__()
        if self.capabilities is None:
            self.capabilities = []
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "HelloMessage":
        return cls(
            type=data.get("type", "hello"),
            device_id=data.get("device_id", ""),
            capabilities=data.get("capabilities", []),
            timestamp=data.get("timestamp"),
        )
@dataclass
class StatusMessage(JsonMessage):
    battery: Optional[int] = None
    storage: Optional[str] = None
    temperature: Optional[float] = None
    recording: bool = False
    connected: bool = True
    def __post_init__(self):
        if not hasattr(self, "type") or not self.type:
            self.type = "status"
        super().__post_init__()
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "StatusMessage":
        return cls(
            type=data.get("type", "status"),
            battery=data.get("battery"),
            storage=data.get("storage"),
            temperature=data.get("temperature"),
            recording=data.get("recording", False),
            connected=data.get("connected", True),
            timestamp=data.get("timestamp"),
        )
@dataclass
class SensorDataMessage(JsonMessage):
    values: Dict[str, float] = None
    def __post_init__(self):
        if not hasattr(self, "type") or not self.type:
            self.type = "sensor_data"
        super().__post_init__()
        if self.values is None:
            self.values = {}
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SensorDataMessage":
        return cls(
            type=data.get("type", "sensor_data"),
            values=data.get("values", {}),
            timestamp=data.get("timestamp"),
        )
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SensorDataMessage":
        return cls(
            type=data.get("type", "sensor_data"),
            values=data.get("values", {}),
            timestamp=data.get("timestamp"),
        )
@dataclass
class AckMessage(JsonMessage):
    cmd: str = ""
    status: str = "ok"
    message: Optional[str] = None
    def __post_init__(self):
        if not hasattr(self, "type") or not self.type:
            self.type = "ack"
        super().__post_init__()
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AckMessage":
        return cls(
            type=data.get("type", "ack"),
            cmd=data.get("cmd", ""),
            status=data.get("status", "ok"),
            message=data.get("message"),
            timestamp=data.get("timestamp"),
        )
@dataclass
class FileInfoMessage(JsonMessage):
    name: str = ""
    size: int = 0
    def __post_init__(self):
        if not hasattr(self, "type") or not self.type:
            self.type = "file_info"
        super().__post_init__()
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "FileInfoMessage":
        return cls(
            type=data.get("type", "file_info"),
            name=data.get("name", ""),
            size=data.get("size", 0),
            timestamp=data.get("timestamp"),
        )
@dataclass
class FileChunkMessage(JsonMessage):
    seq: int = 0
    data: str = ""
    def __post_init__(self):
        if not hasattr(self, "type") or not self.type:
            self.type = "file_chunk"
        super().__post_init__()
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "FileChunkMessage":
        return cls(
            type=data.get("type", "file_chunk"),
            seq=data.get("seq", 0),
            data=data.get("data", ""),
            timestamp=data.get("timestamp"),
        )
@dataclass
class FileEndMessage(JsonMessage):
    name: str = ""
    def __post_init__(self):
        if not hasattr(self, "type") or not self.type:
            self.type = "file_end"
        super().__post_init__()
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "FileEndMessage":
        return cls(
            type=data.get("type", "file_end"),
            name=data.get("name", ""),
            timestamp=data.get("timestamp"),
        )
@dataclass
class StartRecordCommand(JsonMessage):
    session_id: str = ""
    record_video: bool = True
    record_thermal: bool = True
    record_shimmer: bool = False
    def __post_init__(self):
        super().__post_init__()
        self.type = "start_record"
@dataclass
class StopRecordCommand(JsonMessage):
    def __post_init__(self):
        super().__post_init__()
        self.type = "stop_record"
@dataclass
class FlashSyncCommand(JsonMessage):
    duration_ms: int = 200
    sync_id: Optional[str] = None
    def __post_init__(self):
        super().__post_init__()
        self.type = "flash_sync"
@dataclass
class BeepSyncCommand(JsonMessage):
    frequency_hz: int = 1000
    duration_ms: int = 200
    volume: float = 0.8
    sync_id: Optional[str] = None
    def __post_init__(self):
        super().__post_init__()
        self.type = "beep_sync"
class PCServer:
    def __init__(self, port: int = 9000, logger: Optional[logging.Logger] = None, 
                 enable_security: bool = True, enable_discovery: bool = True, enable_lsl: bool = True):
        self.port = port
        self.logger = logger or logging.getLogger(__name__)
        self.server_socket: Optional[socket.socket] = None
        self.is_running = False
        self.connected_devices: Dict[str, ConnectedDevice] = {}
        self.server_thread: Optional[threading.Thread] = None
        self.client_threads: Dict[str, threading.Thread] = {}
        self.thread_pool = ThreadPoolExecutor(max_workers=10)
        self.message_callbacks: List[Callable[[str, JsonMessage], None]] = []
        self.device_callbacks: List[Callable[[str, ConnectedDevice], None]] = []
        self.disconnect_callbacks: List[Callable[[str], None]] = []
        self.heartbeat_interval = 30.0
        self.heartbeat_timeout = 60.0
        self.message_buffer_size = 4096
        self.max_message_size = 1024 * 1024
        
        # Enhanced features
        self.enable_security = enable_security and ENHANCED_FEATURES_AVAILABLE
        self.enable_discovery = enable_discovery and ENHANCED_FEATURES_AVAILABLE
        self.enable_lsl = enable_lsl and ENHANCED_FEATURES_AVAILABLE
        
        # Security components
        self.security_config: Optional[SecurityConfig] = None
        self.token_manager: Optional[TokenManager] = None
        self.security_wrapper: Optional[SecureSocketWrapper] = None
        self.security_checker: Optional[RuntimeSecurityChecker] = None
        
        # Discovery components
        self.discovery: Optional[MultiSensorDiscovery] = None
        
        # LSL components
        self.lsl_streamer: Optional[LSLStreamer] = None
        
        # File transfer
        self.file_transfer: Optional[SecureFileTransfer] = None
        self.file_verifier: Optional[FileIntegrityVerifier] = None
        
        self._initialize_enhanced_features()
        
        self.logger.info(f"PCServer initialized for port {port} "
                        f"(Security: {self.enable_security}, "
                        f"Discovery: {self.enable_discovery}, "
                        f"LSL: {self.enable_lsl})")
    
    def _initialize_enhanced_features(self):
        """Initialize enhanced security, discovery, and LSL features."""
        if not ENHANCED_FEATURES_AVAILABLE:
            self.logger.warning("Enhanced features not available - some functionality disabled")
            return
        
        # Initialize security
        if self.enable_security:
            self.security_config = SecurityConfig(
                use_tls=True,
                tls_version="TLSv1.3",
                min_token_length=32,
                max_token_age_seconds=24 * 3600  # 24 hours
            )
            self.token_manager = TokenManager(logger=self.logger)
            self.security_wrapper = SecureSocketWrapper(
                self.security_config, self.token_manager, self.logger
            )
            self.security_checker = RuntimeSecurityChecker(
                self.security_config, self.logger
            )
            self.logger.info("Security features initialized")
        
        # Initialize discovery
        if self.enable_discovery:
            self.discovery = MultiSensorDiscovery(self.logger)
            self.logger.info("Device discovery initialized")
        
        # Initialize LSL
        if self.enable_lsl:
            self.lsl_streamer = LSLStreamer(self.logger)
            # Create sync marker stream
            sync_config = DefaultLSLStreams.sync_markers_stream()
            self.lsl_streamer.create_outlet("sync_markers", sync_config)
            self.logger.info("LSL streaming initialized")
        
        # Initialize file transfer
        self.file_transfer = SecureFileTransfer(logger=self.logger)
        self.file_verifier = FileIntegrityVerifier(logger=self.logger)
    def start(self) -> bool:
        try:
            if self.is_running:
                self.logger.warning("Server already running")
                return True
            
            self.logger.info(f"Starting PC server on port {self.port}...")
            
            # Check security configuration if enabled
            if self.enable_security and self.security_checker:
                issues = self.security_checker.check_configuration()
                if issues:
                    self.logger.warning("Security configuration issues detected:")
                    for issue in issues:
                        self.logger.warning(f"  - {issue}")
            
            # Create and configure socket
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            
            # Apply TLS if security is enabled
            if self.enable_security and self.security_wrapper:
                ssl_context = self.security_wrapper.create_server_context()
                if ssl_context:
                    self.server_socket = ssl_context.wrap_socket(
                        self.server_socket, server_side=True
                    )
                    self.logger.info("TLS encryption enabled for server socket")
            
            self.server_socket.bind(("0.0.0.0", self.port))
            self.server_socket.listen(5)
            self.is_running = True
            
            # Start server thread
            self.server_thread = threading.Thread(
                target=self._server_loop, name="PCServer"
            )
            self.server_thread.daemon = True
            self.server_thread.start()
            
            # Start heartbeat monitor
            self.thread_pool.submit(self._heartbeat_monitor)
            
            # Start security token cleanup if enabled
            if self.enable_security and self.token_manager:
                self.thread_pool.submit(self._security_cleanup_loop)
            
            # Register service for discovery if enabled
            if self.enable_discovery and self.discovery:
                self.discovery.start()
                hostname = socket.gethostname()
                self.discovery.register_pc_controller(f"pc_controller_{hostname}", self.port)
                self.logger.info("Device discovery service started")
            
            # Send session start marker to LSL if enabled
            if self.enable_lsl and self.lsl_streamer:
                push_sync_marker(self.lsl_streamer, "session_start")
                self.logger.info("LSL session start marker sent")
            
            self.logger.info(f"PC server started successfully on port {self.port}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to start PC server: {e}")
            self.is_running = False
            return False
    def stop(self) -> None:
        try:
            self.logger.info("Stopping PC server...")
            self.is_running = False
            
            # Send session stop marker to LSL if enabled
            if self.enable_lsl and self.lsl_streamer:
                push_sync_marker(self.lsl_streamer, "session_stop")
                self.lsl_streamer.cleanup()
                self.logger.info("LSL session stopped")
            
            # Stop discovery service if enabled
            if self.enable_discovery and self.discovery:
                self.discovery.cleanup()
                self.logger.info("Device discovery stopped")
            
            # Disconnect all devices
            for device_id in list(self.connected_devices.keys()):
                self._disconnect_device(device_id)
            
            # Close server socket
            if self.server_socket:
                self.server_socket.close()
                self.server_socket = None
            
            # Wait for server thread
            if self.server_thread and self.server_thread.is_alive():
                self.server_thread.join(timeout=5.0)
            
            # Shutdown thread pool
            self.thread_pool.shutdown(wait=True)
            
            self.logger.info("PC server stopped successfully")
            
        except Exception as e:
            self.logger.error(f"Error stopping PC server: {e}")
    def send_message(self, device_id: str, message: JsonMessage) -> bool:
        if device_id not in self.connected_devices:
            self.logger.error(f"Device not connected: {device_id}")
            return False
        try:
            device = self.connected_devices[device_id]
            json_data = message.to_json()
            json_bytes = json_data.encode("utf-8")
            length_header = struct.pack(">I", len(json_bytes))
            device.socket.sendall(length_header + json_bytes)
            self.logger.debug(f"Sent message to {device_id}: {message.type}")
            return True
        except Exception as e:
            self.logger.error(f"Error sending message to {device_id}: {e}")
            self._disconnect_device(device_id)
            return False
    def broadcast_message(self, message: JsonMessage) -> int:
        success_count = 0
        for device_id in list(self.connected_devices.keys()):
            if self.send_message(device_id, message):
                success_count += 1
        return success_count
    def get_connected_devices(self) -> Dict[str, ConnectedDevice]:
        return self.connected_devices.copy()
    def add_message_callback(
        self, callback: Callable[[str, JsonMessage], None]
    ) -> None:
        self.message_callbacks.append(callback)
    def add_device_callback(
        self, callback: Callable[[str, ConnectedDevice], None]
    ) -> None:
        self.device_callbacks.append(callback)
    def add_disconnect_callback(self, callback: Callable[[str], None]) -> None:
        self.disconnect_callbacks.append(callback)
    def _server_loop(self) -> None:
        try:
            while self.is_running:
                try:
                    client_socket, address = self.server_socket.accept()
                    self.logger.info(f"New connection from {address}")
                    client_thread = threading.Thread(
                        target=self._handle_client,
                        args=(client_socket, address),
                        name=f"Client-{address[0]}:{address[1]}",
                    )
                    client_thread.daemon = True
                    client_thread.start()
                except socket.error as e:
                    if self.is_running:
                        self.logger.error(f"Server socket error: {e}")
                        break
        except Exception as e:
            self.logger.error(f"Error in server loop: {e}")
        finally:
            self.is_running = False
    def _handle_client(self, client_socket: socket.socket, address: tuple) -> None:
        device_id = None
        try:
            client_socket.settimeout(30.0)
            while self.is_running:
                length_data = self._recv_exact(client_socket, 4)
                if not length_data:
                    break
                message_length = struct.unpack(">I", length_data)[0]
                if message_length <= 0 or message_length > self.max_message_size:
                    self.logger.error(f"Invalid message length: {message_length}")
                    break
                message_data = self._recv_exact(client_socket, message_length)
                if not message_data:
                    break
                json_string = message_data.decode("utf-8")
                message = JsonMessage.from_json(json_string)
                if not message:
                    self.logger.warning(f"Failed to parse message from {address}")
                    continue
                if isinstance(message, HelloMessage) and not device_id:
                    device_id = message.device_id
                    device = ConnectedDevice(
                        device_id=device_id,
                        capabilities=message.capabilities,
                        connection_time=time.time(),
                        last_heartbeat=time.time(),
                        status={},
                        socket=client_socket,
                        address=address,
                    )
                    self.connected_devices[device_id] = device
                    self.client_threads[device_id] = threading.current_thread()
                    self.logger.info(
                        f"Device registered: {device_id} with capabilities: {message.capabilities}"
                    )
                    for callback in self.device_callbacks:
                        try:
                            callback(device_id, device)
                        except Exception as e:
                            self.logger.error(f"Error in device callback: {e}")
                if device_id and device_id in self.connected_devices:
                    self.connected_devices[device_id].last_heartbeat = time.time()
                if device_id:
                    self.logger.debug(f"Received {message.type} from {device_id}")
                    for callback in self.message_callbacks:
                        try:
                            callback(device_id, message)
                        except Exception as e:
                            self.logger.error(f"Error in message callback: {e}")
        except socket.timeout:
            self.logger.warning(f"Client {address} timed out")
        except Exception as e:
            self.logger.error(f"Error handling client {address}: {e}")
        finally:
            if device_id:
                self._disconnect_device(device_id)
            else:
                try:
                    client_socket.close()
                except:
                    pass
    def _recv_exact(self, sock: socket.socket, length: int) -> Optional[bytes]:
        data = b""
        while len(data) < length:
            chunk = sock.recv(length - len(data))
            if not chunk:
                return None
            data += chunk
        return data
    def _disconnect_device(self, device_id: str) -> None:
        if device_id not in self.connected_devices:
            return
        try:
            device = self.connected_devices[device_id]
            device.socket.close()
            del self.connected_devices[device_id]
            if device_id in self.client_threads:
                del self.client_threads[device_id]
            self.logger.info(f"Device disconnected: {device_id}")
            for callback in self.disconnect_callbacks:
                try:
                    callback(device_id)
                except Exception as e:
                    self.logger.error(f"Error in disconnect callback: {e}")
        except Exception as e:
            self.logger.error(f"Error disconnecting device {device_id}: {e}")
    def _heartbeat_monitor(self) -> None:
        while self.is_running:
            try:
                current_time = time.time()
                stale_devices = []
                for device_id, device in self.connected_devices.items():
                    if current_time - device.last_heartbeat > self.heartbeat_timeout:
                        stale_devices.append(device_id)
                for device_id in stale_devices:
                    self.logger.warning(
                        f"Device {device_id} heartbeat timeout, disconnecting"
                    )
                    self._disconnect_device(device_id)
                time.sleep(self.heartbeat_interval)
            except Exception as e:
                self.logger.error(f"Error in heartbeat monitor: {e}")
                time.sleep(5.0)
    
    def _security_cleanup_loop(self) -> None:
        """Periodic cleanup of expired security tokens."""
        while self.is_running:
            try:
                if self.token_manager:
                    self.token_manager.cleanup_expired_tokens()
                time.sleep(300)  # Clean up every 5 minutes
            except Exception as e:
                self.logger.error(f"Error in security cleanup: {e}")
                time.sleep(60)
    
    def generate_device_token(self, device_id: str, permissions: List[str]) -> Optional[str]:
        """Generate authentication token for a device."""
        if not self.enable_security or not self.token_manager:
            return "no_security_enabled"
        
        token = self.token_manager.generate_token(device_id, permissions)
        if token:
            self.logger.info(f"Generated token for device {device_id}")
            return token.token
        return None
    
    def validate_device_token(self, token: str) -> bool:
        """Validate device authentication token."""
        if not self.enable_security or not self.token_manager:
            return True  # Security disabled
        
        auth_token = self.token_manager.validate_token(token)
        return auth_token is not None
    
    def send_lsl_marker(self, event_type: str):
        """Send synchronization marker to LSL."""
        if self.enable_lsl and self.lsl_streamer:
            push_sync_marker(self.lsl_streamer, event_type)
            self.logger.debug(f"Sent LSL marker: {event_type}")
    
    def get_discovered_devices(self) -> List[Dict[str, Any]]:
        """Get devices discovered via Zeroconf."""
        if not self.enable_discovery or not self.discovery:
            return []
        
        devices = []
        android_devices = self.discovery.get_android_devices()
        pc_controllers = self.discovery.get_pc_controllers()
        
        for device in android_devices + pc_controllers:
            devices.append({
                "name": device.name,
                "type": device.properties.get("type", "unknown"),
                "ip_address": device.ip_address,
                "port": device.port,
                "capabilities": device.properties.get("capabilities", "").split(","),
                "last_seen": device.last_seen
            })
        
        return devices
    
    def create_file_integrity_manifest(self, directory: str, output_file: str) -> bool:
        """Create file integrity manifest for a directory."""
        if self.file_verifier:
            return self.file_verifier.create_manifest(directory, output_file)
        return False
    
    def verify_file_integrity_manifest(self, directory: str, manifest_file: str) -> Tuple[bool, List[str]]:
        """Verify files against integrity manifest."""
        if self.file_verifier:
            return self.file_verifier.verify_manifest(directory, manifest_file)
        return False, ["File verifier not available"]
if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
    def on_message(device_id: str, message: JsonMessage):
        print(f"Message from {device_id}: {message.type}")
        if isinstance(message, SensorDataMessage):
            print(f"  Sensor data: {message.values}")
    def on_device_connected(device_id: str, device: ConnectedDevice):
        print(f"Device connected: {device_id} with capabilities: {device.capabilities}")
    def on_device_disconnected(device_id: str):
        print(f"Device disconnected: {device_id}")
    server = PCServer(port=9000)
    server.add_message_callback(on_message)
    server.add_device_callback(on_device_connected)
    server.add_disconnect_callback(on_device_disconnected)
    try:
        if server.start():
            print("Server started successfully. Press Ctrl+C to stop.")
            while True:
                time.sleep(1)
                devices = server.get_connected_devices()
                if devices:
                    sync_cmd = FlashSyncCommand(duration_ms=200, sync_id="test_sync")
                    server.broadcast_message(sync_cmd)
                    time.sleep(10)
    except KeyboardInterrupt:
        print("\nShutting down server...")
    finally:
        server.stop()