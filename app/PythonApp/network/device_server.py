import base64
import json
import queue
import socket
import struct
import threading
import time
import weakref
from collections import defaultdict, deque
from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Any, Callable, Dict, List, Optional, Tuple
from PyQt5.QtCore import QMutex, QMutexLocker, QThread, QTimer, pyqtSignal
from ..utils.logging_config import get_logger
logger = get_logger(__name__)
class MessagePriority(Enum):
    CRITICAL = 1
    HIGH = 2
    NORMAL = 3
    LOW = 4
class ConnectionState(Enum):
    DISCONNECTED = auto()
    CONNECTING = auto()
    CONNECTED = auto()
    RECONNECTING = auto()
    ERROR = auto()
@dataclass
class NetworkMessage:
    type: str
    payload: Dict[str, Any]
    priority: MessagePriority = MessagePriority.NORMAL
    timestamp: float = field(default_factory=time.time)
    retry_count: int = 0
    max_retries: int = 3
    timeout: float = 30.0
    requires_ack: bool = False
    message_id: Optional[str] = None
@dataclass
class ConnectionStats:
    connected_at: float = field(default_factory=time.time)
    messages_sent: int = 0
    messages_received: int = 0
    bytes_sent: int = 0
    bytes_received: int = 0
    last_heartbeat: float = field(default_factory=time.time)
    reconnection_count: int = 0
    error_count: int = 0
    average_latency: float = 0.0
    latency_samples: deque = field(default_factory=lambda: deque(maxlen=100))
    min_latency: float = float("inf")
    max_latency: float = 0.0
    jitter: float = 0.0
    packet_loss_rate: float = 0.0
    ping_count: int = 0
    pong_count: int = 0
class RemoteDevice:
    def __init__(
        self,
        device_id: str,
        capabilities: List[str],
        client_socket: socket.socket,
        address: Tuple[str, int],
    ):
        self.device_id = device_id
        self.capabilities = capabilities
        self.client_socket = client_socket
        self.address = address
        self.state = ConnectionState.CONNECTED
        self.mutex = QMutex()
        self.outbound_queue = queue.PriorityQueue()
        self.pending_acks = {}
        self.stats = ConnectionStats()
        self.last_heartbeat = time.time()
        self.heartbeat_interval = 5.0
        self.heartbeat_timeout = 15.0
        self.streaming_quality = "medium"
        self.max_frame_rate = 15
        self.last_frame_time = 0.0
        self.send_buffer_size = 64 * 1024
        self.recv_buffer_size = 64 * 1024
        self.consecutive_errors = 0
        self.max_consecutive_errors = 5
        logger.info(
            f"Enhanced RemoteDevice created: {device_id} @ {address[0]}:{address[1]}"
        )
    def is_alive(self) -> bool:
        with QMutexLocker(self.mutex):
            return time.time() - self.last_heartbeat < self.heartbeat_timeout
    def should_send_frame(self) -> bool:
        current_time = time.time()
        frame_interval = 1.0 / self.max_frame_rate
        if current_time - self.last_frame_time >= frame_interval:
            self.last_frame_time = current_time
            return True
        return False
    def adapt_streaming_quality(self, network_latency: float, error_rate: float):
        with QMutexLocker(self.mutex):
            if error_rate > 0.1 or network_latency > 200:
                self.streaming_quality = "low"
                self.max_frame_rate = 5
            elif error_rate < 0.05 and network_latency < 50:
                self.streaming_quality = "high"
                self.max_frame_rate = 30
            else:
                self.streaming_quality = "medium"
                self.max_frame_rate = 15
    def queue_message(self, message: NetworkMessage):
        priority_value = message.priority.value
        self.outbound_queue.put((priority_value, time.time(), message))
    def get_next_message(self, timeout: float = 0.1) -> Optional[NetworkMessage]:
        try:
            _, _, message = self.outbound_queue.get(timeout=timeout)
            return message
        except queue.Empty:
            return None
    def update_latency(self, latency: float):
        with QMutexLocker(self.mutex):
            self.stats.latency_samples.append(latency)
            self.stats.min_latency = min(self.stats.min_latency, latency)
            self.stats.max_latency = max(self.stats.max_latency, latency)
            if self.stats.latency_samples:
                self.stats.average_latency = sum(self.stats.latency_samples) / len(
                    self.stats.latency_samples
                )
                if len(self.stats.latency_samples) >= 2:
                    variance = sum(
                        (x - self.stats.average_latency) ** 2
                        for x in self.stats.latency_samples
                    ) / len(self.stats.latency_samples)
                    self.stats.jitter = variance**0.5
                if self.stats.ping_count > 0:
                    self.stats.packet_loss_rate = max(
                        0,
                        (self.stats.ping_count - self.stats.pong_count)
                        / self.stats.ping_count
                        * 100,
                    )
    def update_ping_stats(self, is_response: bool = False):
        with QMutexLocker(self.mutex):
            if is_response:
                self.stats.pong_count += 1
            else:
                self.stats.ping_count += 1
    def increment_error_count(self):
        with QMutexLocker(self.mutex):
            self.stats.error_count += 1
            self.consecutive_errors += 1
    def reset_error_count(self):
        with QMutexLocker(self.mutex):
            self.consecutive_errors = 0
    def should_reconnect(self) -> bool:
        with QMutexLocker(self.mutex):
            return self.consecutive_errors >= self.max_consecutive_errors
    def get_status_summary(self) -> Dict[str, Any]:
        with QMutexLocker(self.mutex):
            return {
                "device_id": self.device_id,
                "state": self.state.name,
                "capabilities": self.capabilities,
                "address": f"{self.address[0]}:{self.address[1]}",
                "is_alive": self.is_alive(),
                "streaming_quality": self.streaming_quality,
                "stats": {
                    "messages_sent": self.stats.messages_sent,
                    "messages_received": self.stats.messages_received,
                    "bytes_sent": self.stats.bytes_sent,
                    "bytes_received": self.stats.bytes_received,
                    "error_count": self.stats.error_count,
                    "average_latency": round(self.stats.average_latency, 2),
                    "min_latency": (
                        round(self.stats.min_latency, 2)
                        if self.stats.min_latency != float("inf")
                        else 0
                    ),
                    "max_latency": round(self.stats.max_latency, 2),
                    "jitter": round(self.stats.jitter, 2),
                    "packet_loss_rate": round(self.stats.packet_loss_rate, 2),
                    "ping_count": self.stats.ping_count,
                    "pong_count": self.stats.pong_count,
                    "connection_duration": round(
                        time.time() - self.stats.connected_at, 1
                    ),
                    "latency_samples": len(self.stats.latency_samples),
                },
            }
class DeviceServer(QThread):
    device_connected = pyqtSignal(str, dict)
    device_disconnected = pyqtSignal(str, str)
    device_status_changed = pyqtSignal(str, str)
    message_received = pyqtSignal(str, dict)
    message_sent = pyqtSignal(str, dict)
    message_failed = pyqtSignal(str, dict, str)
    preview_frame_received = pyqtSignal(str, str, bytes, dict)
    streaming_quality_changed = pyqtSignal(str, str)
    network_stats_updated = pyqtSignal(dict)
    connection_quality_changed = pyqtSignal(str, float)
    error_occurred = pyqtSignal(str, str, str)
    warning_occurred = pyqtSignal(str, str)
    def __init__(
        self,
        host: str = "0.0.0.0",
        port: int = 9000,
        max_connections: int = 10,
        heartbeat_interval: float = 5.0,
    ):
        super().__init__()
        self.host = host
        self.port = port
        self.max_connections = max_connections
        self.heartbeat_interval = heartbeat_interval
        self.server_socket: Optional[socket.socket] = None
        self.running = False
        self.devices: Dict[str, RemoteDevice] = {}
        self.devices_mutex = QMutex()
        self.client_handlers: Dict[str, threading.Thread] = {}
        self.connection_pool = []
        self.heartbeat_timer = QTimer()
        self.heartbeat_timer.timeout.connect(self.send_heartbeats)
        self.message_counter = 0
        self.pending_messages: Dict[str, NetworkMessage] = {}
        self.network_stats = {
            "total_connections": 0,
            "active_connections": 0,
            "total_messages": 0,
            "total_bytes": 0,
            "error_rate": 0.0,
            "average_latency": 0.0,
        }
        self.enable_compression = True
        self.compression_threshold = 1024
        logger.info(f"Enhanced Device Server initialized: {host}:{port}")
    def start_server(self):
        if self.running:
            logger.warning("Server is already running")
            return False
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            self.server_socket.setsockopt(
                socket.SOL_SOCKET, socket.SO_RCVBUF, 64 * 1024
            )
            self.server_socket.setsockopt(
                socket.SOL_SOCKET, socket.SO_SNDBUF, 64 * 1024
            )
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(self.max_connections)
            self.running = True
            self.start()
            self.heartbeat_timer.start(int(self.heartbeat_interval * 1000))
            logger.info(f"Enhanced Device Server started on {self.host}:{self.port}")
            return True
        except Exception as e:
            logger.error(f"Failed to start server: {e}")
            self.error_occurred.emit("server", "startup", str(e))
            return False
    def stop_server(self):
        logger.info("Stopping Enhanced Device Server...")
        self.running = False
        self.heartbeat_timer.stop()
        with QMutexLocker(self.devices_mutex):
            for device_id in list(self.devices.keys()):
                self.disconnect_device(device_id, "Server shutdown")
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
            self.server_socket = None
        if self.isRunning():
            self.wait(5000)
        logger.info("Enhanced Device Server stopped")
    def run(self):
        while self.running and self.server_socket:
            try:
                client_socket, address = self.server_socket.accept()
                if len(self.devices) >= self.max_connections:
                    logger.warning(f"Maximum connections reached, rejecting {address}")
                    client_socket.close()
                    continue
                client_socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
                client_socket.settimeout(30.0)
                handler_thread = threading.Thread(
                    target=self.handle_client_connection,
                    args=(client_socket, address),
                    daemon=True,
                )
                handler_thread.start()
                logger.info(f"New client connection from {address[0]}:{address[1]}")
            except socket.error as e:
                if self.running:
                    logger.error(f"Accept error: {e}")
                    self.error_occurred.emit("server", "accept", str(e))
            except Exception as e:
                if self.running:
                    logger.error(f"Unexpected server error: {e}")
                    self.error_occurred.emit("server", "unexpected", str(e))
    def handle_client_connection(
        self, client_socket: socket.socket, address: Tuple[str, int]
    ):
        client_addr = f"{address[0]}:{address[1]}"
        device_id = None
        device = None
        try:
            handshake_msg = self.receive_message(client_socket, timeout=10.0)
            if not handshake_msg or handshake_msg.get("type") != "handshake":
                logger.warning(f"Invalid handshake from {client_addr}")
                return
            device_id = handshake_msg.get("device_id")
            capabilities = handshake_msg.get("capabilities", [])
            if not device_id:
                logger.warning(f"Missing device_id in handshake from {client_addr}")
                return
            device = RemoteDevice(
                device_id, capabilities, client_socket, address
            )
            with QMutexLocker(self.devices_mutex):
                self.devices[device_id] = device
                self.client_handlers[device_id] = threading.current_thread()
            ack_msg = {
                "type": "handshake_ack",
                "protocol_version": 1,
                "server_name": "Enhanced Device Server",
                "server_version": "1.0.0",
                "compatible": True,
                "timestamp": time.time(),
            }
            self.send_message(device, ack_msg, MessagePriority.CRITICAL)
            self.device_connected.emit(device_id, device.get_status_summary())
            sender_thread = threading.Thread(
                target=self.message_sender_loop, args=(device,), daemon=True
            )
            sender_thread.start()
            self.message_receiver_loop(device)
        except Exception as e:
            logger.error(f"Client handler error for {client_addr}: {e}")
            self.error_occurred.emit(device_id or client_addr, "handler", str(e))
        finally:
            if device_id and device:
                self.disconnect_device(device_id, "Connection closed")
    def message_receiver_loop(self, device: RemoteDevice):
        while self.running and device.state == ConnectionState.CONNECTED:
            try:
                message = self.receive_message(device.client_socket, timeout=1.0)
                if not message:
                    continue
                device.stats.messages_received += 1
                device.last_heartbeat = time.time()
                device.reset_error_count()
                self.process_message(device, message)
            except socket.timeout:
                if not device.is_alive():
                    logger.warning(f"Device {device.device_id} heartbeat timeout")
                    break
            except Exception as e:
                logger.error(f"Receive error for {device.device_id}: {e}")
                device.increment_error_count()
                if device.should_reconnect():
                    break
    def message_sender_loop(self, device: RemoteDevice):
        while self.running and device.state == ConnectionState.CONNECTED:
            try:
                message = device.get_next_message(timeout=0.5)
                if not message:
                    continue
                success = self.send_message_immediate(device, message)
                if success:
                    device.stats.messages_sent += 1
                    device.reset_error_count()
                    self.message_sent.emit(device.device_id, message.payload)
                else:
                    device.increment_error_count()
                    self.message_failed.emit(
                        device.device_id, message.payload, "Send failed"
                    )
                    if device.should_reconnect():
                        break
            except Exception as e:
                logger.error(f"Send error for {device.device_id}: {e}")
                device.increment_error_count()
    def send_message_immediate(
        self, device: RemoteDevice, message: NetworkMessage
    ) -> bool:
        try:
            json_data = json.dumps(message.payload).encode("utf-8")
            if self.enable_compression and len(json_data) > self.compression_threshold:
                pass
            length_header = struct.pack(">I", len(json_data))
            device.client_socket.sendall(length_header + json_data)
            device.stats.bytes_sent += len(length_header) + len(json_data)
            return True
        except Exception as e:
            logger.error(f"Failed to send message to {device.device_id}: {e}")
            return False
    def receive_message(
        self, sock: socket.socket, timeout: float = 1.0
    ) -> Optional[Dict[str, Any]]:
        sock.settimeout(timeout)
        try:
            length_data = self.recv_exact(sock, 4)
            if not length_data:
                return None
            message_length = struct.unpack(">I", length_data)[0]
            if message_length <= 0 or message_length > 10 * 1024 * 1024:
                raise ValueError(f"Invalid message length: {message_length}")
            json_data = self.recv_exact(sock, message_length)
            if not json_data:
                return None
            return json.loads(json_data.decode("utf-8"))
        except socket.timeout:
            return None
        except Exception as e:
            logger.error(f"Receive message error: {e}")
            return None
    def recv_exact(self, sock: socket.socket, length: int) -> Optional[bytes]:
        data = b""
        while len(data) < length:
            chunk = sock.recv(length - len(data))
            if not chunk:
                return None
            data += chunk
        return data
    def process_message(self, device: RemoteDevice, message: Dict[str, Any]):
        msg_type = message.get("type", "unknown")
        if "timestamp" in message:
            latency = (time.time() - message["timestamp"]) * 1000
            device.update_latency(latency)
        if msg_type == "heartbeat":
            self.handle_heartbeat(device, message)
        elif msg_type == "status":
            self.handle_status_update(device, message)
        elif msg_type == "preview_frame":
            self.handle_preview_frame(device, message)
        elif msg_type == "ack":
            self.handle_acknowledgment(device, message)
        elif msg_type == "sensor_data":
            self.handle_sensor_data(device, message)
        else:
            self.message_received.emit(device.device_id, message)
    def handle_preview_frame(
        self, device: RemoteDevice, message: Dict[str, Any]
    ):
        if not device.should_send_frame():
            return
        frame_type = message.get("frame_type", "rgb")
        image_data = message.get("image_data", "")
        width = message.get("width", 0)
        height = message.get("height", 0)
        try:
            image_bytes = base64.b64decode(image_data)
            metadata = {
                "width": width,
                "height": height,
                "frame_type": frame_type,
                "quality": device.streaming_quality,
                "timestamp": message.get("timestamp", time.time()),
            }
            self.preview_frame_received.emit(
                device.device_id, frame_type, image_bytes, metadata
            )
            error_rate = device.stats.error_count / max(
                1, device.stats.messages_received
            )
            device.adapt_streaming_quality(device.stats.average_latency, error_rate)
        except Exception as e:
            logger.error(f"Preview frame processing error: {e}")
    def handle_heartbeat(self, device: RemoteDevice, message: Dict[str, Any]):
        response = NetworkMessage(
            type="heartbeat_response",
            payload={
                "type": "heartbeat_response",
                "timestamp": time.time(),
                "server_time": time.time(),
            },
            priority=MessagePriority.HIGH,
        )
        device.queue_message(response)
    def send_heartbeats(self):
        with QMutexLocker(self.devices_mutex):
            current_time = time.time()
            for device in list(self.devices.values()):
                if device.is_alive():
                    heartbeat = NetworkMessage(
                        type="heartbeat",
                        payload={"type": "heartbeat", "timestamp": current_time},
                        priority=MessagePriority.HIGH,
                    )
                    device.queue_message(heartbeat)
                else:
                    self.disconnect_device(device.device_id, "Heartbeat timeout")
    def send_command_to_device(self, device_id: str, command: str, **kwargs) -> bool:
        with QMutexLocker(self.devices_mutex):
            device = self.devices.get(device_id)
            if not device:
                logger.warning(f"Device {device_id} not found")
                return False
        message = NetworkMessage(
            type="command",
            payload={
                "type": "command",
                "command": command,
                "timestamp": time.time(),
                **kwargs,
            },
            priority=MessagePriority.CRITICAL,
            requires_ack=True,
        )
        device.queue_message(message)
        return True
    def broadcast_command(self, command: str, **kwargs) -> int:
        count = 0
        with QMutexLocker(self.devices_mutex):
            for device_id in self.devices:
                if self.send_command_to_device(device_id, command, **kwargs):
                    count += 1
        return count
    def disconnect_device(self, device_id: str, reason: str = "Unknown"):
        with QMutexLocker(self.devices_mutex):
            device = self.devices.get(device_id)
            if not device:
                return
            try:
                device.client_socket.close()
            except:
                pass
            device.state = ConnectionState.DISCONNECTED
            del self.devices[device_id]
            if device_id in self.client_handlers:
                del self.client_handlers[device_id]
        self.device_disconnected.emit(device_id, reason)
        logger.info(f"Device {device_id} disconnected: {reason}")
    def get_network_statistics(self) -> Dict[str, Any]:
        with QMutexLocker(self.devices_mutex):
            stats = {
                "active_devices": len(self.devices),
                "total_connections": self.network_stats["total_connections"],
                "overall_stats": {
                    "total_messages": sum(
                        device.stats.messages_sent + device.stats.messages_received
                        for device in self.devices.values()
                    ),
                    "total_bytes": sum(
                        device.stats.bytes_sent + device.stats.bytes_received
                        for device in self.devices.values()
                    ),
                    "average_latency": self._calculate_overall_latency(),
                    "network_quality": self._assess_overall_network_quality(),
                },
                "devices": {},
            }
            for device_id, device in self.devices.items():
                stats["devices"][device_id] = device.get_status_summary()
        return stats
    def _calculate_overall_latency(self) -> float:
        if not self.devices:
            return 0.0
        total_latency = 0.0
        device_count = 0
        for device in self.devices.values():
            if device.stats.latency_samples:
                total_latency += device.stats.average_latency
                device_count += 1
        return total_latency / device_count if device_count > 0 else 0.0
    def _assess_overall_network_quality(self) -> str:
        if not self.devices:
            return "unknown"
        avg_latency = self._calculate_overall_latency()
        if avg_latency < 50:
            return "excellent"
        elif avg_latency < 100:
            return "good"
        elif avg_latency < 200:
            return "fair"
        else:
            return "poor"
    def get_device_latency_statistics(self, device_id: str) -> Dict[str, Any]:
        with QMutexLocker(self.devices_mutex):
            device = self.devices.get(device_id)
            if not device:
                return {"error": f"Device {device_id} not found"}
            with QMutexLocker(device.mutex):
                return {
                    "device_id": device_id,
                    "average_latency": device.stats.average_latency,
                    "min_latency": (
                        device.stats.min_latency
                        if device.stats.min_latency != float("inf")
                        else 0
                    ),
                    "max_latency": device.stats.max_latency,
                    "jitter": device.stats.jitter,
                    "packet_loss_rate": device.stats.packet_loss_rate,
                    "ping_count": device.stats.ping_count,
                    "pong_count": device.stats.pong_count,
                    "sample_count": len(device.stats.latency_samples),
                    "recent_samples": (
                        list(device.stats.latency_samples)[-10:]
                        if device.stats.latency_samples
                        else []
                    ),
                }
    def handle_status_update(
        self, device: RemoteDevice, message: Dict[str, Any]
    ):
        storage = message.get("storage", "")
        if isinstance(storage, str) and storage.startswith("ping:"):
            self.handle_ping_message(
                device, storage, message.get("timestamp", time.time())
            )
        else:
            status_data = {k: v for k, v in message.items() if k != "type"}
            self.message_received.emit(device.device_id, message)
    def handle_ping_message(
        self, device: RemoteDevice, ping_data: str, original_timestamp: float
    ):
        try:
            parts = ping_data.split(":")
            if len(parts) >= 4:
                ping_id = parts[1]
                ping_timestamp = float(parts[2])
                sequence = int(parts[3])
                current_time = time.time()
                pong_data = f"pong:{ping_id}:{ping_timestamp}:{current_time}:{sequence}"
                response = NetworkMessage(
                    type="status",
                    payload={
                        "type": "status",
                        "storage": pong_data,
                        "timestamp": current_time,
                        "battery": None,
                        "temperature": None,
                        "recording": False,
                        "connected": True,
                    },
                    priority=MessagePriority.HIGH,
                )
                device.queue_message(response)
                ping_latency = (current_time - ping_timestamp) * 1000
                device.update_latency(ping_latency / 2)
                device.update_ping_stats(is_response=True)
                logger.debug(
                    f"Responded to ping {ping_id} from {device.device_id}, RTT: {ping_latency:.2f}ms"
                )
        except Exception as e:
            logger.error(f"Error handling ping message: {e}")
    def handle_acknowledgment(
        self, device: RemoteDevice, message: Dict[str, Any]
    ):
        message_id = message.get("message_id")
        success = message.get("success", False)
        if message_id in self.pending_messages:
            del self.pending_messages[message_id]
        self.message_received.emit(device.device_id, message)
    def handle_sensor_data(self, device: RemoteDevice, message: Dict[str, Any]):
        self.message_received.emit(device.device_id, message)
    def send_message(
        self,
        device: RemoteDevice,
        message_data: Dict[str, Any],
        priority: MessagePriority = MessagePriority.NORMAL,
    ) -> bool:
        message = NetworkMessage(
            type=message_data.get("type", "unknown"),
            payload=message_data,
            priority=priority,
        )
        device.queue_message(message)
        return True
def create_command_message(command: str, **kwargs) -> Dict[str, Any]:
    return {"type": "command", "command": command, "timestamp": time.time(), **kwargs}
def decode_base64_image(data: str) -> Optional[bytes]:
    try:
        if data.startswith("data:"):
            data = data.split(",", 1)[1]
        return base64.b64decode(data)
    except Exception as e:
        logger.error(f"Base64 decode error: {e}")
        return None