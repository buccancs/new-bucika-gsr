import json
import socket
import ssl
import threading
import time
import uuid
from collections import defaultdict
from typing import Any, Dict, List, Optional, Tuple
from PyQt5.QtCore import QThread, QTimer, pyqtSignal
class DeviceClient(QThread):
    device_connected = pyqtSignal(int, str)
    device_disconnected = pyqtSignal(int)
    frame_received = pyqtSignal(int, str, bytes)
    status_updated = pyqtSignal(int, dict)
    error_occurred = pyqtSignal(str)
    def __init__(self, parent=None):
        super().__init__(parent)
        self.devices: Dict[int, Dict[str, Any]] = {}
        self.running = False
        self.server_socket: Optional[socket.socket] = None
        self.device_counter = 0
        self._device_lock = threading.Lock()
        self.server_port = 8080
        self.buffer_size = 4096
        self.connection_timeout = 30
        self.heartbeat_interval = 5
        self.max_reconnect_attempts = 3
        self._pending_acknowledgments: Dict[str, Dict[str, Any]] = {}
        self._ack_timeout = 10
        self._retry_attempts = 3
        self._rate_limiter: Dict[str, List[float]] = defaultdict(list)
        self._max_requests_per_minute = 60
        self._ssl_enabled = False
        self._ssl_context: Optional[ssl.SSLContext] = None
        self._ssl_certfile = None
        self._ssl_keyfile = None
        self._supported_capabilities = {
            "recording",
            "streaming",
            "calibration",
            "thermal_imaging",
            "gsr_monitoring",
            "audio_capture",
        }
        self._message_stats = {
            "sent": 0,
            "received": 0,
            "errors": 0,
            "avg_latency": 0.0,
            "connection_count": 0,
        }
    def run(self):
        self.running = True
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.settimeout(1.0)
            if self._ssl_enabled and self._ssl_context:
                self.server_socket = self._ssl_context.wrap_socket(
                    self.server_socket, server_side=True
                )
                print(f"[DEBUG_LOG] SSL/TLS enabled for secure communication")
            self.server_socket.bind(("0.0.0.0", self.server_port))
            self.server_socket.listen(5)
            print(f"[DEBUG_LOG] DeviceClient server started on port {self.server_port}")
            while self.running:
                try:
                    client_socket, address = self.server_socket.accept()
                    if not self._check_rate_limit(address[0]):
                        print(f"[DEBUG_LOG] Rate limited connection from {address}")
                        client_socket.close()
                        continue
                    print(f"[DEBUG_LOG] New connection from {address}")
                    self._message_stats["connection_count"] += 1
                    connection_thread = threading.Thread(
                        target=self.handle_device_connection,
                        args=(client_socket, address),
                        daemon=True,
                    )
                    connection_thread.start()
                except socket.timeout:
                    continue
                except Exception as e:
                    if self.running:
                        self.error_occurred.emit(f"Server socket error: {str(e)}")
                    break
        except Exception as e:
            self.error_occurred.emit(f"Failed to start server: {str(e)}")
        finally:
            self._cleanup_server_socket()
        print("[DEBUG_LOG] DeviceClient thread stopped")
    def configure_ssl(
        self, certfile: str, keyfile: str, ca_certs: Optional[str] = None
    ) -> bool:
        try:
            self._ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
            self._ssl_context.load_cert_chain(certfile, keyfile)
            if ca_certs:
                self._ssl_context.load_verify_locations(ca_certs)
                self._ssl_context.verify_mode = ssl.CERT_REQUIRED
            else:
                self._ssl_context.verify_mode = ssl.CERT_NONE
            self._ssl_enabled = True
            self._ssl_certfile = certfile
            self._ssl_keyfile = keyfile
            print(f"[DEBUG_LOG] SSL/TLS configured successfully")
            return True
        except Exception as e:
            self.error_occurred.emit(f"SSL configuration failed: {str(e)}")
            return False
    def _check_rate_limit(self, device_ip: str) -> bool:
        current_time = time.time()
        requests = self._rate_limiter[device_ip]
        requests[:] = [
            req_time for req_time in requests if current_time - req_time < 60
        ]
        if len(requests) >= self._max_requests_per_minute:
            print(f"[DEBUG_LOG] Rate limit exceeded for {device_ip}")
            return False
        requests.append(current_time)
        return True
    def connect_to_device(self, device_ip: str, device_port: int = 8080) -> bool:
        print(
            f"[DEBUG_LOG] Attempting to connect to device at {device_ip}:{device_port}"
        )
        try:
            device_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            device_socket.settimeout(self.connection_timeout)
            device_socket.connect((device_ip, device_port))
            handshake_data = {
                "type": "handshake",
                "client_type": "recording_controller",
                "protocol_version": "1.0",
                "timestamp": time.time(),
            }
            handshake_message = json.dumps(handshake_data).encode("utf-8")
            device_socket.send(handshake_message)
            response = device_socket.recv(self.buffer_size)
            response_data = json.loads(response.decode("utf-8"))
            if response_data.get("status") == "accepted":
                with self._device_lock:
                    device_id = self.device_counter
                    self.device_counter += 1
                    self.devices[device_id] = {
                        "socket": device_socket,
                        "ip": device_ip,
                        "port": device_port,
                        "status": "connected",
                        "last_heartbeat": time.time(),
                        "device_info": response_data.get("device_info", {}),
                        "capabilities": response_data.get("capabilities", []),
                    }
                device_info = f"{device_ip}:{device_port}"
                self.device_connected.emit(device_id, device_info)
                print(
                    f"[DEBUG_LOG] Successfully connected to device {device_id} at {device_ip}:{device_port}"
                )
                return True
            else:
                device_socket.close()
                error_msg = response_data.get("error", "Handshake rejected")
                self.error_occurred.emit(
                    f"Handshake failed with {device_ip}: {error_msg}"
                )
                return False
        except socket.timeout:
            self.error_occurred.emit(f"Connection timeout to {device_ip}:{device_port}")
            return False
        except ConnectionRefusedError:
            self.error_occurred.emit(f"Connection refused by {device_ip}:{device_port}")
            return False
        except json.JSONDecodeError:
            self.error_occurred.emit(f"Invalid handshake response from {device_ip}")
            return False
        except Exception as e:
            self.error_occurred.emit(f"Failed to connect to {device_ip}: {str(e)}")
            return False
    def disconnect_device(self, device_index: int) -> None:
        print(f"[DEBUG_LOG] Disconnecting device {device_index}")
        with self._device_lock:
            if device_index in self.devices:
                device = self.devices[device_index]
                try:
                    disconnect_message = {
                        "type": "disconnect",
                        "reason": "client_initiated",
                        "timestamp": time.time(),
                    }
                    device_socket = device["socket"]
                    device_socket.send(json.dumps(disconnect_message).encode("utf-8"))
                    device_socket.shutdown(socket.SHUT_RDWR)
                    device_socket.close()
                except Exception as e:
                    print(f"[DEBUG_LOG] Error during disconnect cleanup: {e}")
                del self.devices[device_index]
                self.device_disconnected.emit(device_index)
                print(f"[DEBUG_LOG] Device {device_index} disconnected successfully")
            else:
                print(f"[DEBUG_LOG] Device {device_index} not found for disconnection")
    def send_command(
        self,
        device_index: int,
        command: str,
        parameters: Optional[Dict[str, Any]] = None,
        require_ack: bool = True,
    ) -> bool:
        print(f"[DEBUG_LOG] Sending command '{command}' to device {device_index}")
        with self._device_lock:
            if device_index not in self.devices:
                print(f"[DEBUG_LOG] Device {device_index} not found")
                return False
            device = self.devices[device_index]
            if not self._check_rate_limit(device["ip"]):
                self.error_occurred.emit(
                    f"Rate limit exceeded for device {device_index}"
                )
                return False
            try:
                message_id = str(uuid.uuid4())
                timestamp = time.time()
                message = {
                    "type": "command",
                    "command": command,
                    "parameters": parameters or {},
                    "timestamp": timestamp,
                    "message_id": message_id,
                    "require_ack": require_ack,
                }
                device_socket = device["socket"]
                json_data = json.dumps(message).encode("utf-8")
                device_socket.send(json_data)
                if require_ack:
                    self._pending_acknowledgments[message_id] = {
                        "device_index": device_index,
                        "command": command,
                        "timestamp": timestamp,
                        "attempts": 1,
                        "max_attempts": self._retry_attempts,
                    }
                    QTimer.singleShot(
                        self._ack_timeout * 1000,
                        lambda: self._handle_ack_timeout(message_id),
                    )
                self._message_stats["sent"] += 1
                print(
                    f"[DEBUG_LOG] Command '{command}' sent successfully to device {device_index} (msg_id: {message_id})"
                )
                return True
            except Exception as e:
                error_msg = f"Failed to send command '{command}' to device {device_index}: {str(e)}"
                self.error_occurred.emit(error_msg)
                print(f"[DEBUG_LOG] {error_msg}")
                self._message_stats["errors"] += 1
                self._remove_failed_device(device_index)
                return False
    def _handle_ack_timeout(self, message_id: str) -> None:
        if message_id not in self._pending_acknowledgments:
            return
        ack_info = self._pending_acknowledgments[message_id]
        device_index = ack_info["device_index"]
        command = ack_info["command"]
        attempts = ack_info["attempts"]
        max_attempts = ack_info["max_attempts"]
        if attempts < max_attempts:
            print(
                f"[DEBUG_LOG] Retrying command '{command}' to device {device_index} (attempt {attempts + 1}/{max_attempts})"
            )
            ack_info["attempts"] += 1
            ack_info["timestamp"] = time.time()
            with self._device_lock:
                if device_index in self.devices:
                    try:
                        device_socket = self.devices[device_index]["socket"]
                        message = {
                            "type": "command",
                            "command": command,
                            "parameters": ack_info.get("parameters", {}),
                            "timestamp": ack_info["timestamp"],
                            "message_id": message_id,
                            "require_ack": True,
                            "retry_attempt": attempts + 1,
                        }
                        json_data = json.dumps(message).encode("utf-8")
                        device_socket.send(json_data)
                        QTimer.singleShot(
                            self._ack_timeout * 1000,
                            lambda: self._handle_ack_timeout(message_id),
                        )
                    except Exception as e:
                        print(f"[DEBUG_LOG] Retry failed for message {message_id}: {e}")
                        del self._pending_acknowledgments[message_id]
                        self._remove_failed_device(device_index)
        else:
            print(f"[DEBUG_LOG] Max retry attempts reached for message {message_id}")
            del self._pending_acknowledgments[message_id]
            self.error_occurred.emit(
                f"Command '{command}' failed after {max_attempts} attempts to device {device_index}"
            )
    def stop_client(self) -> None:
        print("[DEBUG_LOG] Stopping DeviceClient")
        self.running = False
        with self._device_lock:
            for device_index in list(self.devices.keys()):
                self.disconnect_device(device_index)
        self._cleanup_server_socket()
        self.quit()
        self.wait()
        print("[DEBUG_LOG] DeviceClient stopped successfully")
    def negotiate_capabilities(
        self, device_index: int, requested_capabilities: List[str]
    ) -> Dict[str, bool]:
        print(f"[DEBUG_LOG] Negotiating capabilities with device {device_index}")
        with self._device_lock:
            if device_index not in self.devices:
                return {}
            device = self.devices[device_index]
            device_capabilities = set(device.get("capabilities", []))
            capability_status = {}
            for capability in requested_capabilities:
                if (
                    capability in self._supported_capabilities
                    and capability in device_capabilities
                ):
                    capability_status[capability] = True
                else:
                    capability_status[capability] = False
            negotiation_message = {
                "type": "capability_negotiation",
                "requested_capabilities": requested_capabilities,
                "supported_capabilities": list(self._supported_capabilities),
                "timestamp": time.time(),
                "message_id": str(uuid.uuid4()),
            }
            try:
                device_socket = device["socket"]
                json_data = json.dumps(negotiation_message).encode("utf-8")
                device_socket.send(json_data)
                print(
                    f"[DEBUG_LOG] Capability negotiation completed for device {device_index}"
                )
                return capability_status
            except Exception as e:
                self.error_occurred.emit(
                    f"Capability negotiation failed for device {device_index}: {str(e)}"
                )
                return {}
    def get_performance_metrics(self) -> Dict[str, Any]:
        with self._device_lock:
            connected_devices = len(self.devices)
        return {
            "connected_devices": connected_devices,
            "total_connections": self._message_stats["connection_count"],
            "messages_sent": self._message_stats["sent"],
            "messages_received": self._message_stats["received"],
            "error_count": self._message_stats["errors"],
            "average_latency_ms": self._message_stats["avg_latency"] * 1000,
            "pending_acknowledgments": len(self._pending_acknowledgments),
            "ssl_enabled": self._ssl_enabled,
            "rate_limit_per_minute": self._max_requests_per_minute,
        }
        with self._device_lock:
            devices_info = {}
            for device_id, device_data in self.devices.items():
                devices_info[device_id] = {
                    "ip": device_data["ip"],
                    "port": device_data["port"],
                    "status": device_data["status"],
                    "last_heartbeat": device_data["last_heartbeat"],
                    "connection_time": time.time()
                    - device_data.get("connected_at", time.time()),
                    "device_info": device_data.get("device_info", {}),
                    "capabilities": device_data.get("capabilities", []),
                }
            return devices_info
    def handle_device_connection(
        self, client_socket: socket.socket, address: tuple
    ) -> None:
        print(f"[DEBUG_LOG] Handling new device connection from {address}")
        try:
            client_socket.settimeout(self.connection_timeout)
            handshake_data = client_socket.recv(self.buffer_size)
            handshake = json.loads(handshake_data.decode("utf-8"))
            if handshake.get("type") == "handshake":
                response = {
                    "status": "accepted",
                    "server_info": {"type": "recording_controller", "version": "1.0"},
                    "timestamp": time.time(),
                }
                client_socket.send(json.dumps(response).encode("utf-8"))
                with self._device_lock:
                    device_id = self.device_counter
                    self.device_counter += 1
                    self.devices[device_id] = {
                        "socket": client_socket,
                        "ip": address[0],
                        "port": address[1],
                        "status": "connected",
                        "last_heartbeat": time.time(),
                        "connected_at": time.time(),
                        "device_info": handshake.get("device_info", {}),
                        "capabilities": handshake.get("capabilities", []),
                    }
                device_info = f"{address[0]}:{address[1]}"
                self.device_connected.emit(device_id, device_info)
                monitor_thread = threading.Thread(
                    target=self._monitor_device, args=(device_id,), daemon=True
                )
                monitor_thread.start()
                print(
                    f"[DEBUG_LOG] Device {device_id} registered and monitoring started"
                )
            else:
                response = {
                    "status": "rejected",
                    "error": "Invalid handshake",
                    "timestamp": time.time(),
                }
                client_socket.send(json.dumps(response).encode("utf-8"))
                client_socket.close()
        except Exception as e:
            print(f"[DEBUG_LOG] Error handling device connection: {e}")
            try:
                client_socket.close()
            except:
                pass
    def _monitor_device(self, device_id: int) -> None:
        print(f"[DEBUG_LOG] Starting device monitoring for device {device_id}")
        while self.running and device_id in self.devices:
            try:
                with self._device_lock:
                    if device_id not in self.devices:
                        break
                    device_socket = self.devices[device_id]["socket"]
                device_socket.settimeout(1.0)
                try:
                    data = device_socket.recv(self.buffer_size)
                    if data:
                        message = json.loads(data.decode("utf-8"))
                        self._process_device_message(device_id, message)
                    else:
                        print(f"[DEBUG_LOG] Device {device_id} disconnected")
                        self._remove_failed_device(device_id)
                        break
                except socket.timeout:
                    continue
            except Exception as e:
                print(f"[DEBUG_LOG] Device {device_id} monitoring error: {e}")
                self._remove_failed_device(device_id)
                break
        print(f"[DEBUG_LOG] Device {device_id} monitoring stopped")
    def _process_device_message(self, device_id: int, message: Dict[str, Any]) -> None:
        message_type = message.get("type")
        timestamp = time.time()
        if message_type == "heartbeat":
            with self._device_lock:
                if device_id in self.devices:
                    self.devices[device_id]["last_heartbeat"] = timestamp
        elif message_type == "acknowledgment":
            message_id = message.get("message_id")
            if message_id and message_id in self._pending_acknowledgments:
                ack_info = self._pending_acknowledgments[message_id]
                latency = timestamp - ack_info["timestamp"]
                self._update_latency_stats(latency)
                del self._pending_acknowledgments[message_id]
                print(
                    f"[DEBUG_LOG] Received acknowledgment for message {message_id} (latency: {latency:.3f}s)"
                )
        elif message_type == "frame":
            frame_type = message.get("frame_type", "unknown")
            frame_data = message.get("data", b"")
            self.frame_received.emit(device_id, frame_type, frame_data)
        elif message_type == "status":
            status_info = message.get("status", {})
            self.status_updated.emit(device_id, status_info)
        elif message_type == "capability_response":
            device_capabilities = message.get("capabilities", [])
            with self._device_lock:
                if device_id in self.devices:
                    self.devices[device_id]["capabilities"] = device_capabilities
            print(
                f"[DEBUG_LOG] Updated capabilities for device {device_id}: {device_capabilities}"
            )
        elif message_type == "error":
            error_msg = message.get("error", "Unknown error")
            self.error_occurred.emit(f"Device {device_id} error: {error_msg}")
        else:
            print(
                f"[DEBUG_LOG] Unknown message type from device {device_id}: {message_type}"
            )
        self._message_stats["received"] += 1
    def _update_latency_stats(self, latency: float) -> None:
        alpha = 0.1
        if self._message_stats["avg_latency"] == 0:
            self._message_stats["avg_latency"] = latency
        else:
            self._message_stats["avg_latency"] = (
                alpha * latency + (1 - alpha) * self._message_stats["avg_latency"]
            )
    def _remove_failed_device(self, device_id: int) -> None:
        with self._device_lock:
            if device_id in self.devices:
                try:
                    self.devices[device_id]["socket"].close()
                except:
                    pass
                del self.devices[device_id]
                self.device_disconnected.emit(device_id)
                print(f"[DEBUG_LOG] Removed failed device {device_id}")
    def _cleanup_server_socket(self) -> None:
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
            self.server_socket = None