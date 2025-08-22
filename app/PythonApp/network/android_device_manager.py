import asyncio
import json
import logging
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Set

# Import Android connection detector
try:
    from ..utils.android_connection_detector import (
        AndroidConnectionDetector,
        ConnectionType,
        IDEType
    )
    CONNECTION_DETECTOR_AVAILABLE = True
except ImportError:
    CONNECTION_DETECTOR_AVAILABLE = False
from .pc_server import (
    AckMessage,
    BeepSyncCommand,
    ConnectedDevice,
    FileChunkMessage,
    FileEndMessage,
    FileInfoMessage,
    FlashSyncCommand,
    JsonMessage,
    PCServer,
    SensorDataMessage,
    StartRecordCommand,
    StatusMessage,
    StopRecordCommand,
)
@dataclass
class AndroidDevice:
    device_id: str
    capabilities: List[str]
    connection_time: float
    last_heartbeat: float
    status: Dict[str, Any] = field(default_factory=dict)
    shimmer_devices: Dict[str, Dict[str, Any]] = field(default_factory=dict)
    is_recording: bool = False
    current_session_id: Optional[str] = None
    data_callbacks: List[Callable] = field(default_factory=list)
    messages_received: int = 0
    messages_sent: int = 0
    data_samples_received: int = 0
    last_data_timestamp: Optional[float] = None
    pending_files: Dict[str, Dict[str, Any]] = field(default_factory=dict)
    transfer_progress: Dict[str, float] = field(default_factory=dict)
    # Enhanced connection information
    connection_type: Optional[str] = None
    wireless_debugging_enabled: bool = False
    ide_connections: List[str] = field(default_factory=list)
    ip_address: Optional[str] = None
    port: Optional[int] = None
@dataclass
class ShimmerDataSample:
    timestamp: float
    device_id: str
    android_device_id: str
    sensor_values: Dict[str, float]
    session_id: Optional[str] = None
    raw_message: Optional[SensorDataMessage] = None
@dataclass
class SessionInfo:
    session_id: str
    start_time: float
    end_time: Optional[float] = None
    participating_devices: Set[str] = field(default_factory=set)
    shimmer_devices: Set[str] = field(default_factory=set)
    data_samples: int = 0
    files_collected: Dict[str, List[str]] = field(default_factory=dict)
class AndroidDeviceManager:
    def __init__(
        self, server_port: int = 9000, logger: Optional[logging.Logger] = None
    ):
        self.logger = logger or logging.getLogger(__name__)
        self.server_port = server_port
        self.pc_server = PCServer(port=server_port, logger=self.logger)
        self.android_devices: Dict[str, AndroidDevice] = {}
        self.device_capabilities: Dict[str, Set[str]] = {}
        self.current_session: Optional[SessionInfo] = None
        self.session_history: List[SessionInfo] = []
        self.data_callbacks: List[Callable[[ShimmerDataSample], None]] = []
        self.status_callbacks: List[Callable[[str, AndroidDevice], None]] = []
        self.session_callbacks: List[Callable[[SessionInfo], None]] = []
        self.thread_pool = ThreadPoolExecutor(max_workers=8)
        self.is_initialized = False
        self.heartbeat_interval = 30.0
        self.data_timeout = 60.0
        self.file_transfer_chunk_size = 8192
        
        # Initialize connection detector if available
        if CONNECTION_DETECTOR_AVAILABLE:
            self.connection_detector = AndroidConnectionDetector(logger=self.logger)
            self.connection_detection_enabled = True
        else:
            self.connection_detector = None
            self.connection_detection_enabled = False
            self.logger.warning("Android connection detector not available - enhanced detection disabled")
        
        self._setup_server_callbacks()
        self.logger.info("AndroidDeviceManager initialized")
    def initialize(self) -> bool:
        try:
            self.logger.info("Initializing AndroidDeviceManager...")
            if not self.pc_server.start():
                self.logger.error("Failed to start PC server")
                return False
            self.thread_pool.submit(self._device_monitor_loop)
            self.thread_pool.submit(self._data_processing_loop)
            self.is_initialized = True
            self.logger.info("AndroidDeviceManager initialized successfully")
            return True
        except Exception as e:
            self.logger.error(f"Failed to initialize AndroidDeviceManager: {e}")
            return False
    def shutdown(self) -> None:
        try:
            self.logger.info("Shutting down AndroidDeviceManager...")
            if self.current_session:
                self.stop_session()
            for device_id in list(self.android_devices.keys()):
                self.disconnect_device(device_id)
            self.pc_server.stop()
            self.thread_pool.shutdown(wait=True)
            self.is_initialized = False
            self.logger.info("AndroidDeviceManager shutdown completed")
        except Exception as e:
            self.logger.error(f"Error during shutdown: {e}")
    def get_connected_devices(self) -> Dict[str, AndroidDevice]:
        return self.android_devices.copy()
    def get_device(self, device_id: str) -> Optional[AndroidDevice]:
        return self.android_devices.get(device_id)
    def get_shimmer_devices(self) -> Dict[str, Dict[str, Any]]:
        shimmer_devices = {}
        for android_device in self.android_devices.values():
            for shimmer_id, shimmer_info in android_device.shimmer_devices.items():
                shimmer_key = f"{android_device.device_id}:{shimmer_id}"
                shimmer_devices[shimmer_key] = {
                    **shimmer_info,
                    "android_device_id": android_device.device_id,
                    "shimmer_device_id": shimmer_id,
                }
        return shimmer_devices
    def start_session(
        self,
        session_id: str,
        record_shimmer: bool = True,
        record_video: bool = True,
        record_thermal: bool = True,
    ) -> bool:
        try:
            if self.current_session:
                self.logger.warning(
                    f"Session already active: {self.current_session.session_id}"
                )
                return False
            self.logger.info(f"Starting session: {session_id}")
            self.current_session = SessionInfo(
                session_id=session_id,
                start_time=time.time(),
                participating_devices=set(self.android_devices.keys()),
            )
            start_cmd = StartRecordCommand(
                session_id=session_id,
                record_video=record_video,
                record_thermal=record_thermal,
                record_shimmer=record_shimmer,
            )
            success_count = self.pc_server.broadcast_message(start_cmd)
            if success_count > 0:
                self.logger.info(f"Session started on {success_count} devices")
                for device in self.android_devices.values():
                    device.is_recording = True
                    device.current_session_id = session_id
                for callback in self.session_callbacks:
                    try:
                        callback(self.current_session)
                    except Exception as e:
                        self.logger.error(f"Error in session callback: {e}")
                return True
            else:
                self.current_session = None
                self.logger.error("Failed to start session on any device")
                return False
        except Exception as e:
            self.logger.error(f"Error starting session: {e}")
            self.current_session = None
            return False
    def stop_session(self) -> bool:
        try:
            if not self.current_session:
                self.logger.warning("No active session to stop")
                return False
            session_id = self.current_session.session_id
            self.logger.info(f"Stopping session: {session_id}")
            stop_cmd = StopRecordCommand()
            success_count = self.pc_server.broadcast_message(stop_cmd)
            self.current_session.end_time = time.time()
            self.session_history.append(self.current_session)
            for device in self.android_devices.values():
                device.is_recording = False
                device.current_session_id = None
            completed_session = self.current_session
            self.current_session = None
            for callback in self.session_callbacks:
                try:
                    callback(completed_session)
                except Exception as e:
                    self.logger.error(f"Error in session callback: {e}")
            self.logger.info(f"Session stopped: {session_id}")
            return True
        except Exception as e:
            self.logger.error(f"Error stopping session: {e}")
            return False
    def send_sync_flash(
        self, duration_ms: int = 200, sync_id: Optional[str] = None
    ) -> int:
        flash_cmd = FlashSyncCommand(duration_ms=duration_ms, sync_id=sync_id)
        success_count = self.pc_server.broadcast_message(flash_cmd)
        self.logger.info(f"Sent flash sync to {success_count} devices")
        return success_count
    def send_sync_beep(
        self,
        frequency_hz: int = 1000,
        duration_ms: int = 200,
        volume: float = 0.8,
        sync_id: Optional[str] = None,
    ) -> int:
        beep_cmd = BeepSyncCommand(
            frequency_hz=frequency_hz,
            duration_ms=duration_ms,
            volume=volume,
            sync_id=sync_id,
        )
        success_count = self.pc_server.broadcast_message(beep_cmd)
        self.logger.info(f"Sent beep sync to {success_count} devices")
        return success_count
    def request_file_transfer(self, device_id: str, filepath: str) -> bool:
        if device_id not in self.android_devices:
            self.logger.error(f"Device not connected: {device_id}")
            return False
        try:
            from .pc_server import SendFileCommand
            file_request = SendFileCommand(
                command="send_file", filepath=filepath, timestamp=time.time()
            )
            if hasattr(self.server, "send_message_to_device"):
                success = self.server.send_message_to_device(device_id, file_request)
                if success:
                    self.logger.info(
                        f"File transfer request sent to {device_id}: {filepath}"
                    )
                    return True
                else:
                    self.logger.error(
                        f"Failed to send file transfer request to {device_id}"
                    )
                    return False
            else:
                self.logger.info(
                    f"File transfer requested from {device_id}: {filepath} (logged for future processing)"
                )
                return True
        except ImportError:
            self.logger.info(
                f"File transfer requested from {device_id}: {filepath} (command class not available)"
            )
            return True
        except Exception as e:
            self.logger.error(f"Error requesting file transfer from {device_id}: {e}")
            return False
    def disconnect_device(self, device_id: str) -> bool:
        if device_id not in self.android_devices:
            self.logger.warning(f"Device not connected: {device_id}")
            return False
        try:
            del self.android_devices[device_id]
            if device_id in self.device_capabilities:
                del self.device_capabilities[device_id]
            self.logger.info(f"Device disconnected: {device_id}")
            return True
        except Exception as e:
            self.logger.error(f"Error disconnecting device {device_id}: {e}")
            return False
    def add_data_callback(self, callback: Callable[[ShimmerDataSample], None]) -> None:
        self.data_callbacks.append(callback)
    def add_status_callback(
        self, callback: Callable[[str, AndroidDevice], None]
    ) -> None:
        self.status_callbacks.append(callback)
    def add_session_callback(self, callback: Callable[[SessionInfo], None]) -> None:
        self.session_callbacks.append(callback)
    def get_session_history(self) -> List[SessionInfo]:
        return self.session_history.copy()
    def get_current_session(self) -> Optional[SessionInfo]:
        return self.current_session

    def detect_wireless_and_ide_connections(self) -> Dict[str, Any]:
        """
        Detect devices connected via wireless debugging and IDE connections.
        Returns comprehensive connection information.
        """
        if not self.connection_detection_enabled:
            self.logger.warning("Connection detection not available")
            return {
                'available': False,
                'reason': 'Connection detector not available'
            }
        
        try:
            # Detect all connections
            detected_devices = self.connection_detector.detect_all_connections()
            
            # Update existing device information with enhanced details
            self._enhance_devices_with_connection_info(detected_devices)
            
            # Get summary
            summary = self.connection_detector.get_detection_summary()
            
            self.logger.info(f"Connection detection completed: {summary['total_devices']} devices, "
                           f"{summary['wireless_devices']} wireless, {summary['ides_running']} IDEs")
            
            return {
                'available': True,
                'summary': summary,
                'detected_devices': detected_devices,
                'timestamp': time.time()
            }
            
        except Exception as e:
            self.logger.error(f"Error during connection detection: {e}")
            return {
                'available': True,
                'error': str(e),
                'timestamp': time.time()
            }
    
    def _enhance_devices_with_connection_info(self, detected_devices: Dict) -> None:
        """
        Enhance existing Android devices with connection detection information.
        """
        for device_id, detected_device in detected_devices.items():
            if device_id in self.android_devices:
                android_device = self.android_devices[device_id]
                
                # Update connection information
                android_device.connection_type = detected_device.connection_type.value
                android_device.wireless_debugging_enabled = detected_device.wireless_debugging_enabled
                android_device.ip_address = detected_device.ip_address
                android_device.port = detected_device.port
                
                # Update IDE connections
                ide_connections = self.connection_detector.is_device_ide_connected(device_id)
                android_device.ide_connections = [ide.value for ide in ide_connections]
                
                self.logger.debug(f"Enhanced device {device_id} with connection info: "
                                f"type={android_device.connection_type}, "
                                f"wireless={android_device.wireless_debugging_enabled}, "
                                f"ides={android_device.ide_connections}")
    
    def get_wireless_debugging_devices(self) -> List[AndroidDevice]:
        """Get devices connected via wireless debugging."""
        if not self.connection_detection_enabled:
            return []
        
        wireless_devices = []
        for device in self.android_devices.values():
            if (device.connection_type == "wireless_adb" or 
                device.wireless_debugging_enabled):
                wireless_devices.append(device)
        
        return wireless_devices
    
    def get_ide_connected_devices(self) -> Dict[str, List[AndroidDevice]]:
        """Get devices connected through IDEs, grouped by IDE type."""
        if not self.connection_detection_enabled:
            return {}
        
        ide_devices = {}
        for device in self.android_devices.values():
            for ide_type in device.ide_connections:
                if ide_type not in ide_devices:
                    ide_devices[ide_type] = []
                ide_devices[ide_type].append(device)
        
        return ide_devices
    
    def is_device_wireless_connected(self, device_id: str) -> bool:
        """Check if specific device is connected via wireless debugging."""
        if not self.connection_detection_enabled:
            return False
        
        device = self.android_devices.get(device_id)
        if not device:
            return False
        
        return (device.connection_type == "wireless_adb" or
                device.wireless_debugging_enabled)
    
    def is_device_ide_connected(self, device_id: str) -> List[str]:
        """Check if specific device is connected through IDEs."""
        if not self.connection_detection_enabled:
            return []
        
        device = self.android_devices.get(device_id)
        return device.ide_connections if device else []
    
    def get_enhanced_device_info(self, device_id: str) -> Optional[Dict[str, Any]]:
        """Get comprehensive device information including connection details."""
        device = self.android_devices.get(device_id)
        if not device:
            return None
        
        info = {
            'device_id': device.device_id,
            'status': device.status,
            'capabilities': device.capabilities,
            'connection_time': device.connection_time,
            'last_heartbeat': device.last_heartbeat,
            'is_recording': device.is_recording,
            'current_session_id': device.current_session_id,
            'messages_received': device.messages_received,
            'messages_sent': device.messages_sent,
            'data_samples_received': device.data_samples_received,
            'last_data_timestamp': device.last_data_timestamp,
            'shimmer_devices': device.shimmer_devices,
        }
        
        # Add enhanced connection information if available
        if self.connection_detection_enabled:
            info.update({
                'connection_type': device.connection_type,
                'wireless_debugging_enabled': device.wireless_debugging_enabled,
                'ide_connections': device.ide_connections,
                'ip_address': device.ip_address,
                'port': device.port,
            })
        
        return info
    
    def refresh_connection_detection(self) -> Dict[str, Any]:
        """Manually refresh connection detection information."""
        if not self.connection_detection_enabled:
            return {'available': False}
        
        self.logger.info("Refreshing connection detection...")
        return self.detect_wireless_and_ide_connections()
    
    def get_connection_detection_status(self) -> Dict[str, Any]:
        """Get status of connection detection capabilities."""
        return {
            'available': self.connection_detection_enabled,
            'detector_initialized': self.connection_detector is not None,
            'adb_available': (self.connection_detector.adb_path is not None 
                            if self.connection_detector else False),
            'supported_features': [
                'wireless_debugging_detection',
                'ide_connection_detection', 
                'android_studio_detection',
                'intellij_idea_detection',
                'vscode_android_detection'
            ] if self.connection_detection_enabled else []
        }
    def _setup_server_callbacks(self) -> None:
        self.pc_server.add_message_callback(self._on_message_received)
        self.pc_server.add_device_callback(self._on_device_connected)
        self.pc_server.add_disconnect_callback(self._on_device_disconnected)
    def _on_device_connected(
        self, device_id: str, connected_device: ConnectedDevice
    ) -> None:
        try:
            android_device = AndroidDevice(
                device_id=device_id,
                capabilities=connected_device.capabilities,
                connection_time=connected_device.connection_time,
                last_heartbeat=connected_device.last_heartbeat,
                status=connected_device.status.copy(),
            )
            self.android_devices[device_id] = android_device
            self.device_capabilities[device_id] = set(connected_device.capabilities)
            self.logger.info(f"Android device connected: {device_id}")
            self.logger.info(f"Device capabilities: {connected_device.capabilities}")
            if "shimmer" in connected_device.capabilities:
                self.logger.info(f"Device {device_id} supports Shimmer integration")
            for callback in self.status_callbacks:
                try:
                    callback(device_id, android_device)
                except Exception as e:
                    self.logger.error(f"Error in status callback: {e}")
        except Exception as e:
            self.logger.error(f"Error handling device connection: {e}")
    def _on_device_disconnected(self, device_id: str) -> None:
        try:
            if device_id in self.android_devices:
                device = self.android_devices[device_id]
                if (
                    self.current_session
                    and device_id in self.current_session.participating_devices
                ):
                    self.current_session.participating_devices.discard(device_id)
                del self.android_devices[device_id]
                if device_id in self.device_capabilities:
                    del self.device_capabilities[device_id]
                self.logger.info(f"Android device disconnected: {device_id}")
        except Exception as e:
            self.logger.error(f"Error handling device disconnection: {e}")
    def _on_message_received(self, device_id: str, message: JsonMessage) -> None:
        try:
            if device_id not in self.android_devices:
                self.logger.warning(
                    f"Received message from unknown device: {device_id}"
                )
                return
            device = self.android_devices[device_id]
            device.messages_received += 1
            device.last_heartbeat = time.time()
            if isinstance(message, StatusMessage):
                self._process_status_message(device_id, message)
            elif isinstance(message, SensorDataMessage):
                self._process_sensor_data(device_id, message)
            elif isinstance(message, FileInfoMessage):
                self._process_file_info(device_id, message)
            elif isinstance(message, FileChunkMessage):
                self._process_file_chunk(device_id, message)
            elif isinstance(message, FileEndMessage):
                self._process_file_end(device_id, message)
            elif isinstance(message, AckMessage):
                self._process_acknowledgment(device_id, message)
        except Exception as e:
            self.logger.error(f"Error processing message from {device_id}: {e}")
    def _process_status_message(self, device_id: str, message: StatusMessage) -> None:
        device = self.android_devices[device_id]
        device.status.update(
            {
                "battery": message.battery,
                "storage": message.storage,
                "temperature": message.temperature,
                "recording": message.recording,
                "connected": message.connected,
            }
        )
        device.is_recording = message.recording
        self.logger.debug(f"Status update from {device_id}: {message.to_dict()}")
        for callback in self.status_callbacks:
            try:
                callback(device_id, device)
            except Exception as e:
                self.logger.error(f"Error in status callback: {e}")
    def _process_sensor_data(self, device_id: str, message: SensorDataMessage) -> None:
        device = self.android_devices[device_id]
        device.data_samples_received += 1
        device.last_data_timestamp = message.timestamp
        data_sample = ShimmerDataSample(
            timestamp=message.timestamp or time.time(),
            device_id=f"{device_id}:shimmer",
            android_device_id=device_id,
            sensor_values=message.values,
            session_id=device.current_session_id,
            raw_message=message,
        )
        if self.current_session:
            self.current_session.data_samples += 1
            self.current_session.shimmer_devices.add(data_sample.device_id)
        for callback in self.data_callbacks:
            try:
                callback(data_sample)
            except Exception as e:
                self.logger.error(f"Error in data callback: {e}")
    def _process_file_info(self, device_id: str, message: FileInfoMessage) -> None:
        device = self.android_devices[device_id]
        device.pending_files[message.name] = {
            "size": message.size,
            "received_bytes": 0,
            "start_time": time.time(),
            "chunks": {},
        }
        device.transfer_progress[message.name] = 0.0
        self.logger.info(
            f"File transfer started from {device_id}: {message.name} ({message.size} bytes)"
        )
    def _process_file_chunk(self, device_id: str, message: FileChunkMessage) -> None:
        try:
            device = self.android_devices[device_id]
            if (
                hasattr(message, "filename")
                and message.filename in device.pending_files
            ):
                filename = message.filename
            else:
                active_transfers = getattr(device, "active_transfers", {})
                if active_transfers:
                    filename = list(active_transfers.keys())[0]
                else:
                    self.logger.warning(
                        f"Received file chunk from {device_id} but no active transfer found"
                    )
                    return
            if not hasattr(device, "file_buffers"):
                device.file_buffers = {}
            if filename not in device.file_buffers:
                device.file_buffers[filename] = {}
            chunk_seq = getattr(message, "seq", 0)
            chunk_data = getattr(message, "data", b"")
            device.file_buffers[filename][chunk_seq] = chunk_data
            if (
                hasattr(device, "transfer_progress")
                and filename in device.transfer_progress
            ):
                device.transfer_progress[filename]["chunks_received"] += 1
                device.transfer_progress[filename]["bytes_received"] += len(chunk_data)
            self.logger.debug(
                f"File chunk {chunk_seq} received from {device_id} for {filename} ({len(chunk_data)} bytes)"
            )
            if hasattr(self, "file_progress_callbacks"):
                for callback in self.file_progress_callbacks:
                    try:
                        callback(device_id, filename, chunk_seq, len(chunk_data))
                    except Exception as e:
                        self.logger.error(f"Error in file progress callback: {e}")
        except Exception as e:
            self.logger.error(f"Error processing file chunk from {device_id}: {e}")
    def _process_file_end(self, device_id: str, message: FileEndMessage) -> None:
        device = self.android_devices[device_id]
        if message.name in device.pending_files:
            del device.pending_files[message.name]
            if message.name in device.transfer_progress:
                del device.transfer_progress[message.name]
            self.logger.info(
                f"File transfer completed from {device_id}: {message.name}"
            )
    def _process_acknowledgment(self, device_id: str, message: AckMessage) -> None:
        self.logger.debug(f"ACK from {device_id}: {message.cmd} - {message.status}")
        if message.status == "error" and message.message:
            self.logger.warning(f"Command error from {device_id}: {message.message}")
    def _device_monitor_loop(self) -> None:
        while self.is_initialized:
            try:
                current_time = time.time()
                for device_id, device in list(self.android_devices.items()):
                    if (
                        device.last_data_timestamp
                        and current_time - device.last_data_timestamp
                        > self.data_timeout
                    ):
                        self.logger.warning(
                            f"Device {device_id} data stream appears stale"
                        )
                time.sleep(self.heartbeat_interval)
            except Exception as e:
                self.logger.error(f"Error in device monitor loop: {e}")
                time.sleep(5.0)
    def _data_processing_loop(self) -> None:
        while self.is_initialized:
            try:
                if self.current_session:
                    session_duration = time.time() - self.current_session.start_time
                    if int(session_duration) % 60 == 0:
                        self.logger.info(
                            f"Session {self.current_session.session_id}: {len(self.current_session.participating_devices)} devices, {self.current_session.data_samples} samples, {session_duration:.0f}s duration"
                        )
                time.sleep(1.0)
            except Exception as e:
                self.logger.error(f"Error in data processing loop: {e}")
                time.sleep(5.0)
if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
    def on_data_received(sample: ShimmerDataSample):
        print(
            f"Shimmer data from {sample.android_device_id}: {len(sample.sensor_values)} sensors"
        )
        print(f"  Values: {sample.sensor_values}")
    def on_device_status(device_id: str, device: AndroidDevice):
        print(f"Device status {device_id}: {device.status}")
        
        # Show enhanced connection information if available
        if hasattr(device, 'connection_type') and device.connection_type:
            print(f"  Connection type: {device.connection_type}")
            if device.wireless_debugging_enabled:
                print(f"  Wireless debugging: Enabled")
                if device.ip_address and device.port:
                    print(f"  IP: {device.ip_address}:{device.port}")
            if device.ide_connections:
                print(f"  IDE connections: {', '.join(device.ide_connections)}")
    def on_session_event(session: SessionInfo):
        if session.end_time:
            duration = session.end_time - session.start_time
            print(f"Session completed: {session.session_id} ({duration:.1f}s)")
        else:
            print(f"Session started: {session.session_id}")
    manager = AndroidDeviceManager(port=9000)
    manager.add_data_callback(on_data_received)
    manager.add_status_callback(on_device_status)
    manager.add_session_callback(on_session_event)
    try:
        if manager.initialize():
            print("AndroidDeviceManager started. Waiting for device connections...")
            print("Press Ctrl+C to stop.")
            
            # Show connection detection status
            detection_status = manager.get_connection_detection_status()
            print(f"\nConnection Detection Status:")
            print(f"  Available: {detection_status['available']}")
            print(f"  ADB Available: {detection_status.get('adb_available', 'Unknown')}")
            if detection_status['supported_features']:
                print(f"  Features: {', '.join(detection_status['supported_features'])}")
            
            # Detect initial connections
            if detection_status['available']:
                print("\nDetecting wireless debugging and IDE connections...")
                connection_info = manager.detect_wireless_and_ide_connections()
                if connection_info.get('available'):
                    summary = connection_info.get('summary', {})
                    print(f"  Total devices: {summary.get('total_devices', 0)}")
                    print(f"  Wireless devices: {summary.get('wireless_devices', 0)}")
                    print(f"  IDEs running: {summary.get('ides_running', 0)}")
                    
                    # Show wireless devices
                    wireless_details = summary.get('wireless_device_details', [])
                    if wireless_details:
                        print("\n  Wireless Debugging Devices:")
                        for device in wireless_details:
                            print(f"    - {device['device_id']} ({device.get('model', 'Unknown')})")
                            if device.get('ip_address'):
                                print(f"      IP: {device['ip_address']}:{device['port']}")
                    
                    # Show IDE connections
                    ide_details = summary.get('ide_details', [])
                    if ide_details:
                        print("\n  IDE Connections:")
                        for ide in ide_details:
                            print(f"    - {ide['ide_type'].replace('_', ' ').title()}")
                            if ide.get('project_path') and ide['project_path'] != "Unknown":
                                print(f"      Project: {ide['project_path']}")
                            if ide.get('connected_devices'):
                                print(f"      Devices: {', '.join(ide['connected_devices'])}")
            
            while True:
                time.sleep(10)
                devices = manager.get_connected_devices()
                shimmer_devices = manager.get_shimmer_devices()
                
                print(f"""
Status: {len(devices)} Android devices, {len(shimmer_devices)} Shimmer devices""")
                
                # Show wireless and IDE connection status
                if manager.connection_detection_enabled:
                    wireless_devices = manager.get_wireless_debugging_devices()
                    ide_devices = manager.get_ide_connected_devices()
                    
                    if wireless_devices:
                        print(f"  Wireless debugging devices: {len(wireless_devices)}")
                        for device in wireless_devices:
                            print(f"    - {device.device_id}")
                    
                    if ide_devices:
                        print(f"  IDE-connected devices:")
                        for ide_type, device_list in ide_devices.items():
                            print(f"    {ide_type}: {len(device_list)} devices")
                
                if devices and not manager.get_current_session():
                    session_id = f"test_session_{int(time.time())}"
                    print(f"Starting test session: {session_id}")
                    manager.start_session(session_id)
                    time.sleep(30)
                    manager.stop_session()
    except KeyboardInterrupt:
        print("\nShutting down...")
    finally:
        manager.shutdown()