import json
import logging
import socket
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import asdict, dataclass
from typing import Callable, Dict, List, Optional, Set, Tuple
import ntplib
from PythonApp.network.pc_server import (
    JsonMessage,
    PCServer,
    StartRecordCommand,
    StopRecordCommand,
)
from PythonApp.utils.logging_config import get_logger
from PythonApp.ntp_time_server import NTPTimeServer
logger = get_logger(__name__)
@dataclass
class SyncStatus:
    device_id: str
    device_type: str
    is_synchronized: bool
    time_offset_ms: float
    last_sync_time: float
    sync_quality: float
    recording_active: bool
    frame_count: int
@dataclass
class SyncCommand:
    command_type: str
    session_id: str
    master_timestamp: float
    target_devices: List[str]
    sync_tolerance_ms: float = 50.0
@dataclass
class RecordingSession:
    session_id: str
    start_timestamp: float
    devices: Set[str]
    webcam_files: Dict[str, str]
    android_files: Dict[str, List[str]]
    is_active: bool
    sync_quality: float
class MasterClockSynchronizer:
    def __init__(
        self,
        ntp_port: int = 8889,
        pc_server_port: int = 9000,
        sync_interval: float = 5.0,
        logger_instance: Optional[logging.Logger] = None,
    ):
        self.logger = logger_instance or logger
        self.sync_interval = sync_interval
        self.ntp_server = NTPTimeServer(logger=self.logger, port=ntp_port)
        self.pc_server = PCServer(port=pc_server_port, logger=self.logger)
        self.connected_devices: Dict[str, SyncStatus] = {}
        self.active_sessions: Dict[str, RecordingSession] = {}
        self.master_start_time: Optional[float] = None
        self.is_running = False
        self.sync_thread: Optional[threading.Thread] = None
        self.thread_pool = ThreadPoolExecutor(max_workers=5)
        self.webcam_sync_callbacks: List[Callable[[float], None]] = []
        self.session_callbacks: List[Callable[[str, RecordingSession], None]] = []
        self.sync_status_callbacks: List[Callable[[Dict[str, SyncStatus]], None]] = []
        self.sync_tolerance_ms = 50.0
        self.quality_threshold = 0.8
        self.pc_server.add_device_callback(self._on_device_connected)
        self.pc_server.add_disconnect_callback(self._on_device_disconnected)
        self.pc_server.add_message_callback(self._on_message_received)
        self.logger.info("MasterClockSynchronizer initialized")
    def start(self) -> bool:
        try:
            self.logger.info("Starting master clock synchronisation system...")
            if not self.ntp_server.start():
                self.logger.error("Failed to start NTP server")
                return False
            if not self.pc_server.start():
                self.logger.error("Failed to start PC server")
                self.ntp_server.stop()
                return False
            self.is_running = True
            self.master_start_time = time.time()
            self.sync_thread = threading.Thread(
                target=self._sync_monitoring_loop, name="SyncMonitor"
            )
            self.sync_thread.daemon = True
            self.sync_thread.start()
            self.logger.info("Master clock synchronisation system started successfully")
            return True
        except Exception as e:
            self.logger.error(f"Failed to start synchronisation system: {e}")
            return False
    def stop(self):
        try:
            self.logger.info("Stopping master clock synchronisation system...")
            self.is_running = False
            for session_id in list(self.active_sessions.keys()):
                self.stop_synchronized_recording(session_id)
            self.pc_server.stop()
            self.ntp_server.stop()
            if self.sync_thread and self.sync_thread.is_alive():
                self.sync_thread.join(timeout=5.0)
            self.thread_pool.shutdown(wait=True)
            self.logger.info("Master clock synchronisation system stopped")
        except Exception as e:
            self.logger.error(f"Error stopping synchronisation system: {e}")
    def get_master_timestamp(self) -> float:
        return time.time()
    def _validate_recording_session(self, session_id: str, target_devices: Optional[List[str]]) -> Tuple[bool, List[str]]:
        if session_id in self.active_sessions:
            self.logger.error(f"Session {session_id} already active")
            return False, []
        if target_devices is None:
            target_devices = list(self.connected_devices.keys())
        if not target_devices:
            self.logger.error("No target devices available for recording")
            return False, []
        return True, target_devices
    def _check_sync_quality(self, target_devices: List[str]) -> None:
        poor_sync_devices = []
        for device_id in target_devices:
            if device_id in self.connected_devices:
                status = self.connected_devices[device_id]
                if status.sync_quality < self.quality_threshold:
                    poor_sync_devices.append(device_id)
        if poor_sync_devices:
            self.logger.warning(f"Devices with poor sync quality: {poor_sync_devices}")
    def _send_android_recording_commands(
        self, target_devices: List[str], session_id: str, master_timestamp: float,
        record_video: bool, record_thermal: bool, record_shimmer: bool
    ) -> None:
        android_devices = [
            d for d in target_devices
            if d in self.connected_devices and self.connected_devices[d].device_type == "android"
        ]
        for device_id in android_devices:
            start_cmd = StartRecordCommand(
                session_id=session_id,
                record_video=record_video,
                record_thermal=record_thermal,
                record_shimmer=record_shimmer,
            )
            start_cmd.timestamp = master_timestamp
            success = self.pc_server.send_message(device_id, start_cmd)
            if not success:
                self.logger.error(f"Failed to send start command to {device_id}")
            else:
                self.logger.info(f"Start recording command sent to {device_id}")
    def _trigger_sync_callbacks(self, master_timestamp: float, session_id: str, session: 'RecordingSession') -> None:
        for callback in self.webcam_sync_callbacks:
            try:
                callback(master_timestamp)
            except Exception as e:
                self.logger.error(f"Error in webcam sync callback: {e}")
        for callback in self.session_callbacks:
            try:
                callback(session_id, session)
            except Exception as e:
                self.logger.error(f"Error in session callback: {e}")
    def start_synchronized_recording(
        self,
        session_id: str,
        target_devices: Optional[List[str]] = None,
        record_video: bool = True,
        record_thermal: bool = True,
        record_shimmer: bool = False,
    ) -> bool:
        try:
            valid, target_devices = self._validate_recording_session(session_id, target_devices)
            if not valid:
                return False
            self._check_sync_quality(target_devices)
            master_timestamp = self.get_master_timestamp()
            session = RecordingSession(
                session_id=session_id,
                start_timestamp=master_timestamp,
                devices=set(target_devices),
                webcam_files={},
                android_files={},
                is_active=True,
                sync_quality=1.0,
            )
            self.active_sessions[session_id] = session
            self._send_android_recording_commands(
                target_devices, session_id, master_timestamp, record_video, record_thermal, record_shimmer
            )
            self._trigger_sync_callbacks(master_timestamp, session_id, session)
            self.logger.info(
                f"Synchronised recording started: session {session_id}, timestamp {master_timestamp}, devices: {target_devices}"
            )
            return True
        except Exception as e:
            self.logger.error(f"Error starting synchronised recording: {e}")
            return False
    def stop_synchronized_recording(self, session_id: str) -> bool:
        try:
            if session_id not in self.active_sessions:
                self.logger.error(f"Session {session_id} not found")
                return False
            session = self.active_sessions[session_id]
            if not session.is_active:
                self.logger.warning(f"Session {session_id} already stopped")
                return True
            master_timestamp = self.get_master_timestamp()
            android_devices = [
                d
                for d in session.devices
                if d in self.connected_devices
                and self.connected_devices[d].device_type == "android"
            ]
            for device_id in android_devices:
                stop_cmd = StopRecordCommand()
                stop_cmd.timestamp = master_timestamp
                success = self.pc_server.send_message(device_id, stop_cmd)
                if not success:
                    self.logger.error(f"Failed to send stop command to {device_id}")
                else:
                    self.logger.info(f"Stop recording command sent to {device_id}")
            session.is_active = False
            duration = master_timestamp - session.start_timestamp
            self.logger.info(
                f"Synchronised recording stopped: session {session_id}, duration {duration:.1f}s"
            )
            return True
        except Exception as e:
            self.logger.error(f"Error stopping synchronised recording: {e}")
            return False
    def add_webcam_sync_callback(self, callback: Callable[[float], None]):
        self.webcam_sync_callbacks.append(callback)
    def add_session_callback(self, callback: Callable[[str, RecordingSession], None]):
        self.session_callbacks.append(callback)
    def add_sync_status_callback(
        self, callback: Callable[[Dict[str, SyncStatus]], None]
    ):
        self.sync_status_callbacks.append(callback)
    def get_connected_devices(self) -> Dict[str, SyncStatus]:
        return self.connected_devices.copy()
    def get_active_sessions(self) -> Dict[str, RecordingSession]:
        return self.active_sessions.copy()
    def _on_device_connected(self, device_id: str, device_info):
        try:
            sync_status = SyncStatus(
                device_id=device_id,
                device_type="android",
                is_synchronized=False,
                time_offset_ms=0.0,
                last_sync_time=time.time(),
                sync_quality=0.0,
                recording_active=False,
                frame_count=0,
            )
            self.connected_devices[device_id] = sync_status
            self._initiate_device_sync(device_id)
            self.logger.info(f"Android device connected: {device_id}")
        except Exception as e:
            self.logger.error(f"Error handling device connection: {e}")
    def _on_device_disconnected(self, device_id: str):
        try:
            if device_id in self.connected_devices:
                del self.connected_devices[device_id]
            for session in self.active_sessions.values():
                session.devices.discard(device_id)
            self.logger.info(f"Android device disconnected: {device_id}")
        except Exception as e:
            self.logger.error(f"Error handling device disconnection: {e}")
    def _on_message_received(self, device_id: str, message: JsonMessage):
        try:
            if device_id in self.connected_devices:
                device_status = self.connected_devices[device_id]
                current_time = time.time()
                time_offset_ms = (current_time - message.timestamp) * 1000
                device_status.time_offset_ms = time_offset_ms
                device_status.last_sync_time = current_time
                if abs(time_offset_ms) <= self.sync_tolerance_ms:
                    device_status.sync_quality = (
                        1.0 - abs(time_offset_ms) / self.sync_tolerance_ms
                    )
                    device_status.is_synchronized = True
                else:
                    device_status.sync_quality = 0.0
                    device_status.is_synchronized = False
                self.logger.debug(
                    f"Device {device_id} sync update: offset {time_offset_ms:.1f}ms, quality {device_status.sync_quality:.2f}"
                )
        except Exception as e:
            self.logger.error(f"Error processing message from {device_id}: {e}")
    def _initiate_device_sync(self, device_id: str):
        try:
            sync_message = JsonMessage(type="sync_timestamp")
            sync_message.timestamp = self.get_master_timestamp()
            success = self.pc_server.send_message(device_id, sync_message)
            if success:
                self.logger.info(f"Sync initiated for device {device_id}")
            else:
                self.logger.error(f"Failed to initiate sync for device {device_id}")
        except Exception as e:
            self.logger.error(f"Error initiating device sync: {e}")
    def _sync_monitoring_loop(self):
        while self.is_running:
            try:
                for device_id, status in self.connected_devices.items():
                    current_time = time.time()
                    if current_time - status.last_sync_time > self.sync_interval * 2:
                        self.logger.warning(
                            f"Device {device_id} sync timeout, re-initiating"
                        )
                        self._initiate_device_sync(device_id)
                for session in self.active_sessions.values():
                    if session.is_active:
                        session_sync_qualities = []
                        for device_id in session.devices:
                            if device_id in self.connected_devices:
                                session_sync_qualities.append(
                                    self.connected_devices[device_id].sync_quality
                                )
                        if session_sync_qualities:
                            session.sync_quality = sum(session_sync_qualities) / len(
                                session_sync_qualities
                            )
                        else:
                            session.sync_quality = 0.0
                for callback in self.sync_status_callbacks:
                    try:
                        callback(self.connected_devices)
                    except Exception as e:
                        self.logger.error(f"Error in sync status callback: {e}")
                time.sleep(self.sync_interval)
            except Exception as e:
                self.logger.error(f"Error in sync monitoring loop: {e}")
                time.sleep(1.0)
_master_synchronizer: Optional[MasterClockSynchronizer] = None
def get_master_synchronizer() -> MasterClockSynchronizer:
    global _master_synchronizer
    if _master_synchronizer is None:
        _master_synchronizer = MasterClockSynchronizer()
    return _master_synchronizer
def initialize_master_synchronizer(
    ntp_port: int = 8889, pc_server_port: int = 9000
) -> bool:
    global _master_synchronizer
    try:
        _master_synchronizer = MasterClockSynchronizer(
            ntp_port=ntp_port, pc_server_port=pc_server_port
        )
        return _master_synchronizer.start()
    except Exception as e:
        logger.error(f"Failed to initialize master synchronizer: {e}")
        return False
def shutdown_master_synchronizer():
    global _master_synchronizer
    if _master_synchronizer:
        _master_synchronizer.stop()
        _master_synchronizer = None
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    sync_manager = MasterClockSynchronizer()
    try:
        if sync_manager.start():
            logger.info("Synchronisation system started successfully")
            time.sleep(60)
    except KeyboardInterrupt:
        logger.info("Shutting down...")
    finally:
        sync_manager.stop()
