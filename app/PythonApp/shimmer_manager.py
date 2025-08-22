import csv
import logging
import os
import queue
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import asdict, dataclass
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Set, Union
from ..network.android_device_manager import AndroidDeviceManager, ShimmerDataSample
from ..network.pc_server import PCServer
from ..utils.logging_config import get_logger
try:
    from .shimmer.shimmer_imports import (
        DEFAULT_BAUDRATE,
        DataPacket,
        Serial,
        ShimmerBluetooth,
        PYSHIMMER_AVAILABLE,
    )
except ImportError:
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(__file__)))
    from PythonApp.shimmer.shimmer_imports import (
        DEFAULT_BAUDRATE,
        DataPacket,
        Serial,
        ShimmerBluetooth,
        PYSHIMMER_AVAILABLE,
    )
class ConnectionType(Enum):
    DIRECT_BLUETOOTH = "direct_bluetooth"
    ANDROID_MEDIATED = "android_mediated"
    SIMULATION = "simulation"
class DeviceState(Enum):
    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    STREAMING = "streaming"
    ERROR = "error"
class ConnectionStatus(Enum):
    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    ERROR = "error"
@dataclass
class ShimmerStatus:
    is_available: bool = False
    is_connected: bool = False
    is_recording: bool = False
    is_streaming: bool = False
    connection_type: ConnectionType = ConnectionType.SIMULATION
    device_state: DeviceState = DeviceState.DISCONNECTED
    sampling_rate: int = 0
    enabled_channels: Set[str] = None
    device_name: Optional[str] = None
    mac_address: Optional[str] = None
    firmware_version: Optional[str] = None
    battery_level: Optional[int] = None
    signal_quality: Optional[str] = None
    samples_recorded: int = 0
    last_data_timestamp: Optional[float] = None
    android_device_id: Optional[str] = None
    last_error: Optional[str] = None
    connection_attempts: int = 0
    def __post_init__(self):
        if self.enabled_channels is None:
            self.enabled_channels = set()
@dataclass
class ShimmerSample:
    timestamp: float
    system_time: str
    device_id: str
    connection_type: ConnectionType = ConnectionType.SIMULATION
    android_device_id: Optional[str] = None
    gsr_conductance: Optional[float] = None
    ppg_a13: Optional[float] = None
    accel_x: Optional[float] = None
    accel_y: Optional[float] = None
    accel_z: Optional[float] = None
    gyro_x: Optional[float] = None
    gyro_y: Optional[float] = None
    gyro_z: Optional[float] = None
    mag_x: Optional[float] = None
    mag_y: Optional[float] = None
    mag_z: Optional[float] = None
    ecg: Optional[float] = None
    emg: Optional[float] = None
    battery_percentage: Optional[int] = None
    signal_strength: Optional[float] = None
    raw_data: Optional[Dict[str, Any]] = None
    session_id: Optional[str] = None
@dataclass
class DeviceConfiguration:
    device_id: str
    mac_address: str
    enabled_channels: Set[str]
    connection_type: ConnectionType = ConnectionType.SIMULATION
    sampling_rate: int = 128
    android_device_id: Optional[str] = None
    auto_reconnect: bool = True
    data_validation: bool = True
    buffer_size: int = 1000
@dataclass
class DeviceStatus:
    device_id: str
    mac_address: str
    connection_status: ConnectionStatus
    connection_type: ConnectionType
    last_seen: datetime
    is_streaming: bool = False
    samples_count: int = 0
    last_error: Optional[str] = None
class ShimmerManager:
    def __init__(
        self, session_manager=None, logger=None, enable_android_integration=True
    ):
        self.session_manager = session_manager
        self.logger = logger or get_logger(__name__)
        self.enable_android_integration = enable_android_integration
        self.connected_devices: Dict[str, Union[ShimmerBluetooth, str]] = {}
        self.shimmer_devices: Dict[str, Any] = {}
        self.device_configurations: Dict[str, DeviceConfiguration] = {}
        self.device_status: Dict[str, ShimmerStatus] = {}
        self.android_device_manager: Optional[AndroidDeviceManager] = None
        self.android_shimmer_mapping: Dict[str, str] = {}
        self.data_queues: Dict[str, queue.Queue] = {}
        self.csv_writers: Dict[str, csv.DictWriter] = {}
        self.csv_files: Dict[str, Any] = {}
        self.is_initialized = False
        self.is_recording = False
        self.is_streaming = False
        self.data_processing_thread: Optional[threading.Thread] = None
        self.file_writing_thread: Optional[threading.Thread] = None
        self.stop_event = threading.Event()
        self.thread_pool = ThreadPoolExecutor(max_workers=8)
        self.current_session_id: Optional[str] = None
        self.session_start_time: Optional[datetime] = None
        self.data_callbacks: List[Callable[[ShimmerSample], None]] = []
        self.status_callbacks: List[Callable[[str, ShimmerStatus], None]] = []
        self.android_device_callbacks: List[Callable[[str, Dict[str, Any]], None]] = []
        self.connection_state_callbacks: List[
            Callable[[str, DeviceState, ConnectionType], None]
        ] = []
        self.default_sampling_rate = 128
        self.data_buffer_size = 1000
        self.connection_timeout = 30.0
        self.android_server_port = 9000
        self.sensor_ranges = {
            "gsr_conductance": (0.0, 100.0),
            "ppg_a13": (0.0, 4095.0),
            "accel_x": (-16.0, 16.0),
            "accel_y": (-16.0, 16.0),
            "accel_z": (-16.0, 16.0),
            "gyro_x": (-2000.0, 2000.0),
            "gyro_y": (-2000.0, 2000.0),
            "gyro_z": (-2000.0, 2000.0),
            "battery_percentage": (0, 100),
        }
        self.logger.info(
            "Enhanced ShimmerManager initialized with Android integration support"
        )
    def initialize(self) -> bool:
        try:
            self.logger.info("Initializing Enhanced ShimmerManager...")
            if not PYSHIMMER_AVAILABLE:
                self.logger.warning(
                    "PyShimmer library not available - using simulation/Android-only mode"
                )
            self.connected_devices.clear()
            self.device_configurations.clear()
            self.device_status.clear()
            self.data_queues.clear()
            if self.enable_android_integration:
                self.logger.info("Initializing Android device integration...")
                self.android_device_manager = AndroidDeviceManager(
                    server_port=self.android_server_port, logger=self.logger
                )
                self.android_device_manager.add_data_callback(
                    self._on_android_shimmer_data
                )
                self.android_device_manager.add_status_callback(
                    self._on_android_device_status
                )
                if not self.android_device_manager.initialize():
                    self.logger.error("Failed to initialize Android device manager")
                    if not PYSHIMMER_AVAILABLE:
                        return False
                    else:
                        self.logger.warning("Continuing with direct connections only")
                        self.enable_android_integration = False
                else:
                    self.logger.info(
                        f"Android device server listening on port {self.android_server_port}"
                    )
            self._start_background_threads()
            self.is_initialized = True
            self.logger.info("Enhanced ShimmerManager initialized successfully")
            return True
        except Exception as e:
            self.logger.error(f"Failed to initialize Enhanced ShimmerManager: {e}")
            return False
    def scan_and_pair_devices(self) -> Dict[str, List[str]]:
        discovered_devices = {"direct": [], "android": [], "simulated": []}
        try:
            self.logger.info(
                "Scanning for Shimmer devices across all connection methods..."
            )
            if PYSHIMMER_AVAILABLE:
                self.logger.info(
                    "Performing direct Bluetooth scanning for Shimmer devices..."
                )
                direct_devices = self._scan_direct_bluetooth_devices()
                discovered_devices["direct"] = direct_devices
                self.logger.info(
                    f"Direct Bluetooth scan found {len(direct_devices)} devices"
                )
            else:
                self.logger.info(
                    "pyshimmer not available - skipping direct Bluetooth scanning"
                )
            if self.enable_android_integration and self.android_device_manager:
                android_devices = self.android_device_manager.get_connected_devices()
                for device_id, device in android_devices.items():
                    if "shimmer" in device.capabilities:
                        discovered_devices["android"].append(device_id)
                        self.logger.info(
                            f"Found Shimmer-capable Android device: {device_id}"
                        )
            if not PYSHIMMER_AVAILABLE or len(discovered_devices["direct"]) == 0:
                simulated_devices = ["00:06:66:66:66:66", "00:06:66:66:66:67"]
                discovered_devices["simulated"] = simulated_devices
                self.logger.info(
                    f"Simulated discovery: {len(simulated_devices)} devices"
                )
            total_devices = sum(len(devices) for devices in discovered_devices.values())
            self.logger.info(
                f"Device scan completed: {total_devices} total devices found"
            )
            return discovered_devices
        except Exception as e:
            self.logger.error(f"Error during device scanning: {e}")
            return discovered_devices
    def connect_devices(
        self, device_info: Union[List[str], Dict[str, List[str]]]
    ) -> bool:
        try:
            if isinstance(device_info, list):
                return self._connect_legacy_devices(device_info)
            elif isinstance(device_info, dict):
                return self._connect_modern_devices(device_info)
            else:
                self.logger.error("Invalid device_info format")
                return False
        except Exception as e:
            self.logger.error(f"Error connecting devices: {e}")
            return False
    def _connect_legacy_devices(self, device_addresses: List[str]) -> bool:
        self.logger.info(f"Connecting to {len(device_addresses)} devices (legacy mode)...")
        success_count = 0
        for mac_address in device_addresses:
            if self._connect_single_device(mac_address, ConnectionType.SIMULATION):
                success_count += 1
        all_connected = success_count == len(device_addresses)
        self.logger.info(f"Connection results: {success_count}/{len(device_addresses)} successful")
        return all_connected
    def _connect_modern_devices(self, device_info: Dict[str, List[str]]) -> bool:
        total_devices = 0
        success_count = 0
        if "direct" in device_info:
            direct_success = self._connect_direct_devices(device_info["direct"])
            total_devices += len(device_info["direct"])
            success_count += direct_success
        if "android" in device_info and self.enable_android_integration:
            android_success = self._connect_android_devices(device_info["android"])
            total_devices += len(device_info["android"])
            success_count += android_success
        if "simulated" in device_info:
            simulated_success = self._connect_simulated_devices(device_info["simulated"])
            total_devices += len(device_info["simulated"])
            success_count += simulated_success
        all_connected = success_count == total_devices
        self.logger.info(f"Enhanced connection results: {success_count}/{total_devices} successful")
        return all_connected
    def _connect_direct_devices(self, device_addresses: List[str]) -> int:
        success_count = 0
        for mac_address in device_addresses:
            if self._connect_single_device(mac_address, ConnectionType.DIRECT_BLUETOOTH):
                success_count += 1
        return success_count
    def _connect_android_devices(self, android_device_ids: List[str]) -> int:
        success_count = 0
        for android_device_id in android_device_ids:
            if self._connect_android_device(android_device_id):
                success_count += 1
        return success_count
    def _connect_simulated_devices(self, device_addresses: List[str]) -> int:
        success_count = 0
        for mac_address in device_addresses:
            if self._connect_single_device(mac_address, ConnectionType.SIMULATION):
                success_count += 1
        return success_count
    def _connect_single_device(
        self, mac_address: str, connection_type: ConnectionType
    ) -> bool:
        try:
            device_id = f"shimmer_{mac_address.replace(':', '_')}"
            if connection_type == ConnectionType.SIMULATION or not PYSHIMMER_AVAILABLE:
                return self._setup_simulated_device(device_id, mac_address, connection_type)
            elif connection_type == ConnectionType.DIRECT_BLUETOOTH:
                return self._setup_bluetooth_device(device_id, mac_address, connection_type)
            return False
        except Exception as e:
            self.logger.error(f"Error connecting to device {mac_address}: {e}")
            return False
    def _setup_simulated_device(
        self, device_id: str, mac_address: str, connection_type: ConnectionType
    ) -> bool:
        self.device_status[device_id] = ShimmerStatus(
            is_available=True,
            is_connected=True,
            device_state=DeviceState.CONNECTED,
            connection_type=connection_type,
            device_name=f"Shimmer3_{device_id[-4:]}",
            mac_address=mac_address,
            firmware_version="1.0.0",
            sampling_rate=self.default_sampling_rate,
        )
        self.device_configurations[device_id] = DeviceConfiguration(
            device_id=device_id,
            mac_address=mac_address,
            enabled_channels={"GSR", "PPG_A13", "Accel_X", "Accel_Y", "Accel_Z"},
            connection_type=connection_type,
        )
        self.data_queues[device_id] = queue.Queue(maxsize=self.data_buffer_size)
        self.logger.info(f"Simulated connection to {device_id}")
        return True
    def _setup_bluetooth_device(
        self, device_id: str, mac_address: str, connection_type: ConnectionType
    ) -> bool:
        if not PYSHIMMER_AVAILABLE:
            self.logger.error(
                f"pyshimmer library not available for direct connection to {device_id}"
            )
            return False
        try:
            self.logger.info(f"Establishing direct Bluetooth connection to {mac_address}")
            serial_port = self._find_serial_port_for_device(mac_address)
            if not serial_port:
                self.logger.error(f"Could not find serial port for device {mac_address}")
                return False
            from pyshimmer import ShimmerBluetooth
            shimmer_device = ShimmerBluetooth(serial_port)
            connect_success = shimmer_device.connect(timeout=10.0)
            if not connect_success:
                self.logger.error(f"Failed to connect to Shimmer device at {mac_address}")
                return False
            shimmer_device.set_data_callback(
                lambda data: self._on_shimmer_data_received(device_id, data)
            )
            default_config = self.device_configurations.get(device_id)
            if default_config:
                self._configure_shimmer_device(shimmer_device, default_config)
            self.shimmer_devices[device_id] = shimmer_device
            self._store_device_status_and_config(device_id, mac_address, connection_type)
            self.logger.info(f"Successfully connected to Shimmer device {device_id} via Bluetooth")
            return True
        except Exception as e:
            self.logger.error(f"Exception during Bluetooth connection to {mac_address}: {e}")
            return False
    def _store_device_status_and_config(
        self, device_id: str, mac_address: str, connection_type: ConnectionType
    ) -> None:
        self.device_status[device_id] = DeviceStatus(
            device_id=device_id,
            mac_address=mac_address,
            connection_status=ConnectionStatus.CONNECTED,
            connection_type=connection_type,
            last_seen=datetime.now(),
        )
        if device_id not in self.device_configurations:
            self.device_configurations[device_id] = DeviceConfiguration(
                device_id=device_id,
                mac_address=mac_address,
                enabled_channels={"GSR", "PPG_A13", "Accel_X", "Accel_Y", "Accel_Z"},
                connection_type=connection_type,
            )
        self.data_queues[device_id] = queue.Queue(maxsize=self.data_buffer_size)
    def _connect_android_device(self, android_device_id: str) -> bool:
        try:
            if not self.enable_android_integration or not self.android_device_manager:
                self.logger.error("Android integration not available")
                return False
            android_devices = self.android_device_manager.get_connected_devices()
            if android_device_id not in android_devices:
                self.logger.error(f"Android device not connected: {android_device_id}")
                return False
            android_device = android_devices[android_device_id]
            if "shimmer" not in android_device.capabilities:
                self.logger.error(
                    f"Android device {android_device_id} does not support Shimmer"
                )
                return False
            shimmer_device_id = f"android_{android_device_id}_shimmer"
            self.device_status[shimmer_device_id] = ShimmerStatus(
                is_available=True,
                is_connected=True,
                device_state=DeviceState.CONNECTED,
                connection_type=ConnectionType.ANDROID_MEDIATED,
                device_name=f"AndroidShimmer_{android_device_id}",
                mac_address="android_mediated",
                android_device_id=android_device_id,
                sampling_rate=self.default_sampling_rate,
            )
            self.device_configurations[shimmer_device_id] = DeviceConfiguration(
                device_id=shimmer_device_id,
                mac_address="android_mediated",
                enabled_channels={"GSR", "PPG_A13", "Accel_X", "Accel_Y", "Accel_Z"},
                connection_type=ConnectionType.ANDROID_MEDIATED,
                android_device_id=android_device_id,
            )
            self.android_shimmer_mapping[android_device_id] = shimmer_device_id
            self.data_queues[shimmer_device_id] = queue.Queue(
                maxsize=self.data_buffer_size
            )
            self.logger.info(
                f"Connected to Shimmer via Android device: {android_device_id}"
            )
            return True
        except Exception as e:
            self.logger.error(
                f"Error connecting to Android device {android_device_id}: {e}"
            )
            return False
    def _scan_direct_bluetooth_devices(self) -> List[str]:
        discovered_devices = []
        try:
            if not PYSHIMMER_AVAILABLE:
                self.logger.warning(
                    "pyshimmer library not available for direct scanning"
                )
                return discovered_devices
            self.logger.info("Starting Bluetooth scan for Shimmer devices...")
            try:
                from pyshimmer import ShimmerBluetooth
                scan_timeout = 10.0
                self.logger.info(f"Scanning for {scan_timeout} seconds...")
                self.logger.info(
                    "Bluetooth scanning simulation - would scan for Shimmer devices"
                )
            except ImportError:
                self.logger.warning("ShimmerBluetooth.scan_devices not available")
            if len(discovered_devices) == 0:
                discovered_devices = self._generic_bluetooth_scan()
            self.logger.info(
                f"Bluetooth scan completed: {len(discovered_devices)} Shimmer devices found"
            )
            return discovered_devices
        except Exception as e:
            self.logger.error(f"Error during Bluetooth scanning: {e}")
            return discovered_devices
    def _generic_bluetooth_scan(self) -> List[str]:
        discovered_devices = []
        try:
            try:
                import bluetooth
                self.logger.info(
                    "Using generic bluetooth library for device discovery..."
                )
                nearby_devices = bluetooth.discover_devices(
                    duration=8, lookup_names=True
                )
                for addr, name in nearby_devices:
                    if name and ("shimmer" in name.lower() or "rn42" in name.lower()):
                        discovered_devices.append(addr)
                        self.logger.info(
                            f"Found potential Shimmer device: {name} ({addr})"
                        )
            except ImportError:
                self.logger.info("Generic bluetooth library not available")
            if len(discovered_devices) == 0:
                try:
                    import bluetooth as pybluez
                    self.logger.info("Using pybluez for device discovery...")
                    nearby_devices = pybluez.discover_devices(
                        duration=8, lookup_names=True
                    )
                    for addr, name in nearby_devices:
                        if name and (
                            "shimmer" in name.lower() or "rn42" in name.lower()
                        ):
                            discovered_devices.append(addr)
                            self.logger.info(
                                f"Found potential Shimmer device: {name} ({addr})"
                            )
                except ImportError:
                    self.logger.info("pybluez library not available")
            if len(discovered_devices) == 0:
                self.logger.info("No Bluetooth scanning libraries available.")
                self.logger.info(
                    "Install 'bluetooth' or 'pybluez' for device discovery."
                )
                self.logger.info("Or ensure pyshimmer library is properly installed.")
        except Exception as e:
            self.logger.error(f"Error in generic Bluetooth scanning: {e}")
        return discovered_devices
    def set_enabled_channels(self, device_id: str, channels: Set[str]) -> bool:
        try:
            if device_id not in self.device_status:
                self.logger.error(f"Device not found: {device_id}")
                return False
            if device_id not in self.device_configurations:
                self.device_configurations[device_id] = DeviceConfiguration(
                    device_id=device_id,
                    mac_address=self.device_status[device_id].mac_address or "",
                    enabled_channels=channels,
                )
            else:
                self.device_configurations[device_id].enabled_channels = channels
            self.logger.info(f"Configured channels for {device_id}: {channels}")
            return True
        except Exception as e:
            self.logger.error(f"Error configuring channels for {device_id}: {e}")
            return False
    def start_streaming(self) -> bool:
        try:
            if not self.is_initialized:
                self.logger.error("ShimmerManager not initialized")
                return False
            self.logger.info("Starting data streaming...")
            success_count = 0
            for device_id in self.connected_devices:
                if self._start_device_streaming(device_id):
                    success_count += 1
            for device_id in self.device_status:
                if device_id not in self.connected_devices:
                    if self._start_device_streaming(device_id):
                        success_count += 1
            self.is_streaming = success_count > 0
            self.logger.info(f"Streaming started for {success_count} devices")
            return self.is_streaming
        except Exception as e:
            self.logger.error(f"Error starting streaming: {e}")
            return False
    def _start_device_streaming(self, device_id: str) -> bool:
        try:
            if device_id in self.connected_devices:
                device = self.connected_devices[device_id]
                device.start_streaming()
            if device_id in self.device_status:
                self.device_status[device_id].is_streaming = True
            if not PYSHIMMER_AVAILABLE:
                self._start_simulated_streaming(device_id)
            return True
        except Exception as e:
            self.logger.error(f"Error starting streaming for {device_id}: {e}")
            return False
    def stop_streaming(self) -> bool:
        try:
            self.logger.info("Stopping data streaming...")
            for device_id in self.connected_devices:
                self._stop_device_streaming(device_id)
            for device_id in self.device_status:
                if device_id not in self.connected_devices:
                    self._stop_device_streaming(device_id)
            self.is_streaming = False
            self.logger.info("Data streaming stopped")
            return True
        except Exception as e:
            self.logger.error(f"Error stopping streaming: {e}")
            return False
    def _stop_device_streaming(self, device_id: str) -> bool:
        try:
            if device_id in self.connected_devices:
                device = self.connected_devices[device_id]
                device.stop_streaming()
            if device_id in self.device_status:
                self.device_status[device_id].is_streaming = False
            return True
        except Exception as e:
            self.logger.error(f"Error stopping streaming for {device_id}: {e}")
            return False
    def start_recording(self, session_id: str) -> bool:
        try:
            if not self.is_initialized:
                self.logger.error("ShimmerManager not initialized")
                return False
            self.logger.info(f"Starting recording for session: {session_id}")
            self.current_session_id = session_id
            self.session_start_time = datetime.now()
            session_dir = self._create_session_directory(session_id)
            if not session_dir:
                return False
            for device_id in self.device_status:
                if not self._initialize_csv_file(device_id, session_dir):
                    self.logger.error(f"Failed to initialize CSV file for {device_id}")
                    return False
            if not self.is_streaming:
                if not self.start_streaming():
                    self.logger.error("Failed to start streaming for recording")
                    return False
            self.is_recording = True
            self.logger.info(f"Recording started for session: {session_id}")
            return True
        except Exception as e:
            self.logger.error(f"Error starting recording: {e}")
            return False
    def stop_recording(self) -> bool:
        try:
            if not self.is_recording:
                self.logger.warning("Recording not active")
                return True
            self.logger.info("Stopping recording...")
            for device_id, writer in self.csv_writers.items():
                if device_id in self.csv_files:
                    self.csv_files[device_id].close()
                    self.logger.info(f"Closed CSV file for {device_id}")
            self.csv_writers.clear()
            self.csv_files.clear()
            self.is_recording = False
            self.current_session_id = None
            self.session_start_time = None
            self.logger.info("Recording stopped successfully")
            return True
        except Exception as e:
            self.logger.error(f"Error stopping recording: {e}")
            return False
    def get_shimmer_status(self) -> Dict[str, ShimmerStatus]:
        return self.device_status.copy()
    def add_data_callback(self, callback: Callable[[ShimmerSample], None]) -> None:
        self.data_callbacks.append(callback)
    def add_status_callback(
        self, callback: Callable[[str, ShimmerStatus], None]
    ) -> None:
        self.status_callbacks.append(callback)
    def add_android_device_callback(
        self, callback: Callable[[str, Dict[str, Any]], None]
    ) -> None:
        self.android_device_callbacks.append(callback)
    def add_connection_state_callback(
        self, callback: Callable[[str, DeviceState, ConnectionType], None]
    ) -> None:
        self.connection_state_callbacks.append(callback)
    def _on_android_shimmer_data(self, sample: ShimmerDataSample) -> None:
        try:
            if sample.android_device_id not in self.android_shimmer_mapping:
                self.logger.warning(
                    f"Received data from unmapped Android device: {sample.android_device_id}"
                )
                return
            shimmer_device_id = self.android_shimmer_mapping[sample.android_device_id]
            shimmer_sample = ShimmerSample(
                timestamp=sample.timestamp,
                system_time=datetime.fromtimestamp(sample.timestamp).isoformat(),
                device_id=shimmer_device_id,
                connection_type=ConnectionType.ANDROID_MEDIATED,
                android_device_id=sample.android_device_id,
                session_id=sample.session_id,
            )
            for sensor_name, value in sample.sensor_values.items():
                if sensor_name == "gsr_conductance":
                    shimmer_sample.gsr_conductance = value
                elif sensor_name == "ppg_a13":
                    shimmer_sample.ppg_a13 = value
                elif sensor_name == "accel_x":
                    shimmer_sample.accel_x = value
                elif sensor_name == "accel_y":
                    shimmer_sample.accel_y = value
                elif sensor_name == "accel_z":
                    shimmer_sample.accel_z = value
                elif sensor_name == "gyro_x":
                    shimmer_sample.gyro_x = value
                elif sensor_name == "gyro_y":
                    shimmer_sample.gyro_y = value
                elif sensor_name == "gyro_z":
                    shimmer_sample.gyro_z = value
                elif sensor_name == "mag_x":
                    shimmer_sample.mag_x = value
                elif sensor_name == "mag_y":
                    shimmer_sample.mag_y = value
                elif sensor_name == "mag_z":
                    shimmer_sample.mag_z = value
                elif sensor_name == "battery_percentage":
                    shimmer_sample.battery_percentage = (
                        int(value) if value is not None else None
                    )
            if self._validate_sample_data(shimmer_sample):
                if shimmer_device_id in self.data_queues:
                    try:
                        self.data_queues[shimmer_device_id].put_nowait(shimmer_sample)
                    except queue.Full:
                        try:
                            self.data_queues[shimmer_device_id].get_nowait()
                            self.data_queues[shimmer_device_id].put_nowait(
                                shimmer_sample
                            )
                        except queue.Empty:
                            pass
            else:
                self.logger.warning(f"Invalid data sample from {shimmer_device_id}")
        except Exception as e:
            self.logger.error(f"Error processing Android Shimmer data: {e}")
    def _on_android_device_status(self, android_device_id: str, android_device) -> None:
        try:
            if android_device_id in self.android_shimmer_mapping:
                shimmer_device_id = self.android_shimmer_mapping[android_device_id]
                if shimmer_device_id in self.device_status:
                    status = self.device_status[shimmer_device_id]
                    if "battery" in android_device.status:
                        status.battery_level = android_device.status["battery"]
                    status.last_data_timestamp = android_device.last_data_timestamp
                    status.is_recording = android_device.is_recording
                    if android_device.is_recording:
                        status.device_state = DeviceState.STREAMING
                        status.is_streaming = True
                    elif status.is_connected:
                        status.device_state = DeviceState.CONNECTED
                        status.is_streaming = False
                    for callback in self.status_callbacks:
                        try:
                            callback(shimmer_device_id, status)
                        except Exception as e:
                            self.logger.error(f"Error in status callback: {e}")
            for callback in self.android_device_callbacks:
                try:
                    callback(android_device_id, android_device.status)
                except Exception as e:
                    self.logger.error(f"Error in Android device callback: {e}")
        except Exception as e:
            self.logger.error(f"Error processing Android device status: {e}")
    def _validate_sample_data(self, sample: ShimmerSample) -> bool:
        try:
            if sample.timestamp <= 0:
                return False
            sensor_checks = [
                ("gsr_conductance", sample.gsr_conductance),
                ("ppg_a13", sample.ppg_a13),
                ("accel_x", sample.accel_x),
                ("accel_y", sample.accel_y),
                ("accel_z", sample.accel_z),
                ("gyro_x", sample.gyro_x),
                ("gyro_y", sample.gyro_y),
                ("gyro_z", sample.gyro_z),
                ("battery_percentage", sample.battery_percentage),
            ]
            for sensor_name, value in sensor_checks:
                if value is not None and sensor_name in self.sensor_ranges:
                    min_val, max_val = self.sensor_ranges[sensor_name]
                    if not min_val <= value <= max_val:
                        self.logger.warning(
                            f"Invalid {sensor_name} value: {value} (expected {min_val}-{max_val})"
                        )
                        return False
            return True
        except Exception as e:
            self.logger.error(f"Error validating sample data: {e}")
            return False
    def _find_serial_port_for_device(self, mac_address: str) -> Optional[str]:
        try:
            import serial.tools.list_ports
            ports = serial.tools.list_ports.comports()
            for port in ports:
                if hasattr(port, "device") and hasattr(port, "description"):
                    description = port.description.lower()
                    if (
                        "bluetooth" in description
                        or "bt" in description
                        or "shimmer" in description
                        or "rn42" in description
                    ):
                        if (
                            hasattr(port, "hwid")
                            and mac_address.replace(":", "").lower()
                            in port.hwid.lower()
                        ):
                            self.logger.info(
                                f"Found serial port {port.device} for device {mac_address}"
                            )
                            return port.device
                        self.logger.info(
                            f"Found potential Bluetooth port: {port.device} ({port.description})"
                        )
                        return port.device
            import platform
            system = platform.system().lower()
            if system == "windows":
                for i in range(1, 20):
                    port_name = f"COM{i}"
                    try:
                        import serial
                        test_port = serial.Serial(port_name, timeout=1)
                        test_port.close()
                        self.logger.info(f"Found available COM port: {port_name}")
                        return port_name
                    except (OSError, serial.SerialException) as e:
                        self.logger.debug(f"Port {port_name} not available: {e}")
                        continue
            elif system == "linux":
                import glob
                bt_ports = (
                    glob.glob("/dev/rfcomm*")
                    + glob.glob("/dev/ttyUSB*")
                    + glob.glob("/dev/ttyACM*")
                )
                if bt_ports:
                    self.logger.info(f"Found potential Bluetooth ports: {bt_ports}")
                    return bt_ports[0]
            elif system == "darwin":
                import glob
                bt_ports = glob.glob("/dev/tty.*Bluetooth*") + glob.glob(
                    "/dev/cu.*Bluetooth*"
                )
                if bt_ports:
                    self.logger.info(f"Found potential Bluetooth ports: {bt_ports}")
                    return bt_ports[0]
            self.logger.warning(f"Could not find serial port for device {mac_address}")
            return None
        except Exception as e:
            self.logger.error(f"Error finding serial port for {mac_address}: {e}")
            return None
    def _configure_shimmer_device(
        self, shimmer_device, device_config: DeviceConfiguration
    ) -> bool:
        try:
            self.logger.info(f"Configuring Shimmer device {device_config.device_id}")
            if hasattr(shimmer_device, "set_sampling_rate"):
                shimmer_device.set_sampling_rate(device_config.sampling_rate)
                self.logger.info(
                    f"Set sampling rate to {device_config.sampling_rate} Hz"
                )
            if hasattr(shimmer_device, "set_enabled_sensors"):
                sensor_ids = self._channels_to_sensor_ids(
                    device_config.enabled_channels
                )
                shimmer_device.set_enabled_sensors(sensor_ids)
                self.logger.info(f"Enabled sensors: {device_config.enabled_channels}")
            if device_config.sensor_range and hasattr(
                shimmer_device, "set_sensor_range"
            ):
                for sensor, range_val in device_config.sensor_range.items():
                    shimmer_device.set_sensor_range(sensor, range_val)
                    self.logger.info(f"Set {sensor} range to {range_val}")
            return True
        except Exception as e:
            self.logger.error(f"Error configuring Shimmer device: {e}")
            return False
    def _channels_to_sensor_ids(self, channels: Set[str]) -> List[int]:
        channel_mapping = {
            "GSR": 4,
            "PPG_A13": 16,
            "Accel_X": 128,
            "Accel_Y": 128,
            "Accel_Z": 128,
            "Gyro_X": 64,
            "Gyro_Y": 64,
            "Gyro_Z": 64,
            "Mag_X": 32,
            "Mag_Y": 32,
            "Mag_Z": 32,
        }
        sensor_ids = []
        for channel in channels:
            if channel in channel_mapping:
                sensor_id = channel_mapping[channel]
                if sensor_id not in sensor_ids:
                    sensor_ids.append(sensor_id)
        return sensor_ids
    def _on_shimmer_data_received(self, device_id: str, data) -> None:
        try:
            sample = self._convert_pyshimmer_data(device_id, data)
            if sample:
                if device_id in self.data_queues:
                    try:
                        self.data_queues[device_id].put_nowait(sample)
                        if device_id in self.device_status:
                            self.device_status[device_id].last_data_received = (
                                datetime.now()
                            )
                            self.device_status[device_id].samples_received += 1
                    except queue.Full:
                        self.logger.warning(f"Data queue full for device {device_id}")
                if self.is_recording and device_id in self.recording_files:
                    self._write_sample_to_file(device_id, sample)
        except Exception as e:
            self.logger.error(f"Error processing Shimmer data for {device_id}: {e}")
    def _convert_pyshimmer_data(self, device_id: str, data) -> Optional[Dict[str, Any]]:
        try:
            sample = {
                "device_id": device_id,
                "timestamp": datetime.now().isoformat(),
                "sample_number": getattr(data, "packet_id", 0),
                "channels": {},
            }
            if hasattr(data, "gsr") and data.gsr is not None:
                sample["channels"]["GSR"] = data.gsr
            if hasattr(data, "ppg") and data.ppg is not None:
                sample["channels"]["PPG_A13"] = data.ppg
            if hasattr(data, "accel_x") and data.accel_x is not None:
                sample["channels"]["Accel_X"] = data.accel_x
            if hasattr(data, "accel_y") and data.accel_y is not None:
                sample["channels"]["Accel_Y"] = data.accel_y
            if hasattr(data, "accel_z") and data.accel_z is not None:
                sample["channels"]["Accel_Z"] = data.accel_z
            if hasattr(data, "gyro_x") and data.gyro_x is not None:
                sample["channels"]["Gyro_X"] = data.gyro_x
            if hasattr(data, "gyro_y") and data.gyro_y is not None:
                sample["channels"]["Gyro_Y"] = data.gyro_y
            if hasattr(data, "gyro_z") and data.gyro_z is not None:
                sample["channels"]["Gyro_Z"] = data.gyro_z
            return sample
        except Exception as e:
            self.logger.error(f"Error converting pyshimmer data: {e}")
            return None
    def cleanup(self) -> None:
        try:
            self.logger.info("Cleaning up Enhanced ShimmerManager...")
            if self.is_recording:
                self.stop_recording()
            if self.is_streaming:
                self.stop_streaming()
            self.stop_event.set()
            if self.data_processing_thread and self.data_processing_thread.is_alive():
                self.data_processing_thread.join(timeout=5.0)
            if self.file_writing_thread and self.file_writing_thread.is_alive():
                self.file_writing_thread.join(timeout=5.0)
            if self.android_device_manager:
                self.android_device_manager.shutdown()
                self.android_device_manager = None
            for device_id, device in self.connected_devices.items():
                try:
                    if isinstance(device, str):
                        self.logger.info(
                            f"Disconnected Android-mediated device: {device_id}"
                        )
                    else:
                        device.shutdown()
                        self.logger.info(f"Disconnected direct device: {device_id}")
                except Exception as e:
                    self.logger.error(f"Error disconnecting {device_id}: {e}")
            for device_id, shimmer_device in self.shimmer_devices.items():
                try:
                    if hasattr(shimmer_device, "disconnect"):
                        shimmer_device.disconnect()
                    elif hasattr(shimmer_device, "close"):
                        shimmer_device.close()
                    self.logger.info(f"Disconnected pyshimmer device: {device_id}")
                except Exception as e:
                    self.logger.error(
                        f"Error disconnecting pyshimmer device {device_id}: {e}"
                    )
            self.thread_pool.shutdown(wait=True)
            self.connected_devices.clear()
            self.device_configurations.clear()
            self.device_status.clear()
            self.data_queues.clear()
            self.android_shimmer_mapping.clear()
            self.is_initialized = False
            self.logger.info("Enhanced ShimmerManager cleanup completed")
        except Exception as e:
            self.logger.error(f"Error during cleanup: {e}")
    def get_android_devices(self) -> Dict[str, Any]:
        if not self.android_device_manager:
            return {}
        android_devices = self.android_device_manager.get_connected_devices()
        shimmer_capable = {}
        for device_id, device in android_devices.items():
            if "shimmer" in device.capabilities:
                shimmer_capable[device_id] = {
                    "capabilities": device.capabilities,
                    "status": device.status,
                    "is_recording": device.is_recording,
                    "shimmer_devices": device.shimmer_devices,
                }
        return shimmer_capable
    def start_android_session(self, session_id: str, **kwargs) -> bool:
        if not self.android_device_manager:
            self.logger.error("Android device manager not available")
            return False
        return self.android_device_manager.start_session(session_id, **kwargs)
    def stop_android_session(self) -> bool:
        if not self.android_device_manager:
            self.logger.error("Android device manager not available")
            return False
        return self.android_device_manager.stop_session()
    def send_sync_signal(self, signal_type: str = "flash", **kwargs) -> int:
        if not self.android_device_manager:
            self.logger.error("Android device manager not available")
            return 0
        if signal_type == "flash":
            return self.android_device_manager.send_sync_flash(**kwargs)
        elif signal_type == "beep":
            return self.android_device_manager.send_sync_beep(**kwargs)
        else:
            self.logger.error(f"Unknown sync signal type: {signal_type}")
            return 0
    def _create_session_directory(self, session_id: str) -> Optional[Path]:
        try:
            if self.session_manager:
                session_dir = Path(
                    self.session_manager.get_session_directory(session_id)
                )
            else:
                base_dir = Path("recordings")
                session_dir = base_dir / session_id / "shimmer"
            session_dir.mkdir(parents=True, exist_ok=True)
            self.logger.info(f"Created session directory: {session_dir}")
            return session_dir
        except Exception as e:
            self.logger.error(f"Error creating session directory: {e}")
            return None
    def _initialize_csv_file(self, device_id: str, session_dir: Path) -> bool:
        try:
            csv_file_path = session_dir / f"{device_id}_data.csv"
            csv_file = open(csv_file_path, "w", newline="")
            fieldnames = [
                "timestamp",
                "system_time",
                "device_id",
                "connection_type",
                "android_device_id",
                "session_id",
                "gsr_conductance",
                "ppg_a13",
                "accel_x",
                "accel_y",
                "accel_z",
                "gyro_x",
                "gyro_y",
                "gyro_z",
                "mag_x",
                "mag_y",
                "mag_z",
                "ecg",
                "emg",
                "battery_percentage",
                "signal_strength",
            ]
            writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
            writer.writeheader()
            self.csv_files[device_id] = csv_file
            self.csv_writers[device_id] = writer
            self.logger.info(f"Initialized CSV file for {device_id}: {csv_file_path}")
            return True
        except Exception as e:
            self.logger.error(f"Error initializing CSV file for {device_id}: {e}")
            return False
    def _start_background_threads(self) -> None:
        self.stop_event.clear()
        self.data_processing_thread = threading.Thread(
            target=self._data_processing_loop, name="ShimmerDataProcessing"
        )
        self.data_processing_thread.daemon = True
        self.data_processing_thread.start()
        self.file_writing_thread = threading.Thread(
            target=self._file_writing_loop, name="ShimmerFileWriting"
        )
        self.file_writing_thread.daemon = True
        self.file_writing_thread.start()
    def _data_processing_loop(self) -> None:
        while not self.stop_event.is_set():
            try:
                for device_id, data_queue in self.data_queues.items():
                    try:
                        sample = data_queue.get_nowait()
                        self._process_data_sample(sample)
                    except queue.Empty:
                        continue
                    except Exception as e:
                        self.logger.error(f"Error processing data for {device_id}: {e}")
                time.sleep(0.01)
            except Exception as e:
                self.logger.error(f"Error in data processing loop: {e}")
                time.sleep(1.0)
    def _file_writing_loop(self) -> None:
        while not self.stop_event.is_set():
            try:
                if self.is_recording:
                    for csv_file in self.csv_files.values():
                        csv_file.flush()
                time.sleep(1.0)
            except Exception as e:
                self.logger.error(f"Error in file writing loop: {e}")
                time.sleep(1.0)
    def _process_data_sample(self, sample: ShimmerSample) -> None:
        try:
            if self.is_recording and sample.device_id in self.csv_writers:
                writer = self.csv_writers[sample.device_id]
                writer.writerow(asdict(sample))
            if sample.device_id in self.device_status:
                status = self.device_status[sample.device_id]
                status.samples_recorded += 1
                if sample.battery_percentage is not None:
                    status.battery_level = sample.battery_percentage
            for callback in self.data_callbacks:
                try:
                    callback(sample)
                except Exception as e:
                    self.logger.error(f"Error in data callback: {e}")
        except Exception as e:
            self.logger.error(f"Error processing data sample: {e}")
    def _start_simulated_streaming(self, device_id: str) -> None:
        def simulate_data():
            while not self.stop_event.is_set() and self.is_streaming:
                try:
                    if (
                        device_id in self.device_status
                        and self.device_status[device_id].is_streaming
                    ):
                        sample = self._generate_simulated_sample(device_id)
                        if device_id in self.data_queues:
                            try:
                                self.data_queues[device_id].put_nowait(sample)
                            except queue.Full:
                                try:
                                    self.data_queues[device_id].get_nowait()
                                    self.data_queues[device_id].put_nowait(sample)
                                except queue.Empty:
                                    pass
                    time.sleep(1.0 / self.default_sampling_rate)
                except Exception as e:
                    self.logger.error(
                        f"Error in simulated streaming for {device_id}: {e}"
                    )
                    time.sleep(1.0)
        self.thread_pool.submit(simulate_data)
    def _generate_simulated_sample(self, device_id: str) -> ShimmerSample:
        import random
        timestamp = time.time()
        system_time = datetime.now().isoformat()
        gsr_conductance = random.uniform(0.1, 10.0)
        ppg_a13 = random.uniform(1000, 4000)
        accel_x = random.uniform(-2.0, 2.0)
        accel_y = random.uniform(-2.0, 2.0)
        accel_z = random.uniform(0.8, 1.2)
        battery_percentage = random.randint(20, 100)
        return ShimmerSample(
            timestamp=timestamp,
            system_time=system_time,
            device_id=device_id,
            gsr_conductance=gsr_conductance,
            ppg_a13=ppg_a13,
            accel_x=accel_x,
            accel_y=accel_y,
            accel_z=accel_z,
            battery_percentage=battery_percentage,
        )
if __name__ == "__main__":
    logger = get_logger(__name__)
    def on_data_received(sample: ShimmerSample):
        logger.info(
            f"Data from {sample.device_id} ({sample.connection_type.value}): GSR={sample.gsr_conductance}, PPG={sample.ppg_a13}"
        )
    def on_status_update(device_id: str, status: ShimmerStatus):
        logger.info(
            f"Status {device_id}: {status.device_state.value} - Battery: {status.battery_level}%"
        )
    def on_android_device(device_id: str, status: Dict[str, Any]):
        logger.info(f"Android device {device_id}: {status}")
    manager = ShimmerManager(enable_android_integration=True)
    manager.add_data_callback(on_data_received)
    manager.add_status_callback(on_status_update)
    manager.add_android_device_callback(on_android_device)
    try:
        if manager.initialize():
            logger.info("Enhanced ShimmerManager initialized successfully")
            logger.info("Listening for Android devices on port 9000...")
            logger.info("Connect Android devices and they will appear here.")
            devices = manager.scan_and_pair_devices()
            logger.info(f"Found devices: {devices}")
            if any(devices.values()) and manager.connect_devices(devices):
                logger.info("Connected to devices")
                channels = {"GSR", "PPG_A13", "Accel_X", "Accel_Y", "Accel_Z"}
                for device_id in manager.device_status:
                    manager.set_enabled_channels(device_id, channels)
                if manager.start_streaming():
                    logger.info("Streaming started")
                    logger.info("Monitoring data for 30 seconds...")
                    for i in range(30):
                        time.sleep(1)
                        if i % 10 == 0:
                            android_devices = manager.get_android_devices()
                            shimmer_status = manager.get_shimmer_status()
                            print(f"\nStatus update:")
                            print(f"  Android devices: {len(android_devices)}")
                            print(f"  Shimmer devices: {len(shimmer_status)}")
                            if android_devices:
                                sync_count = manager.send_sync_signal(
                                    "flash", duration_ms=200
                                )
                                print(f"  Sent sync flash to {sync_count} devices")
                    android_devices = manager.get_android_devices()
                    if android_devices:
                        session_id = f"test_session_{int(time.time())}"
                        print(f"\nStarting Android recording session: {session_id}")
                        if manager.start_android_session(
                            session_id, record_shimmer=True
                        ):
                            print("Android recording session started")
                            time.sleep(15)
                            manager.stop_android_session()
                            print("Android recording session stopped")
                    session_id = f"local_session_{int(time.time())}"
                    if manager.start_recording(session_id):
                        print(f"Local recording started for session: {session_id}")
                        time.sleep(10)
                        manager.stop_recording()
                        print("Local recording stopped")
                    manager.stop_streaming()
                    print("Streaming stopped")
            status = manager.get_shimmer_status()
            for device_id, device_status in status.items():
                print(
                    f"Device {device_id} ({device_status.connection_type.value}): {device_status.samples_recorded} samples recorded"
                )
    except KeyboardInterrupt:
        print("\nShutting down...")
    finally:
        manager.cleanup()
        print("Enhanced ShimmerManager cleanup completed")
