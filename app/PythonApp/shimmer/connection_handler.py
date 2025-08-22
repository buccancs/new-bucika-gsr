import logging
import queue
import threading
from typing import Dict, List, Optional, Set
from PythonApp.utils.logging_config import get_logger
from .device_models import (
    ConnectionType,
    DeviceConfiguration,
    DeviceState,
    ShimmerStatus,
)
from .shimmer_imports import (
    DEFAULT_BAUDRATE,
    Serial,
    ShimmerBluetooth,
    PYSHIMMER_AVAILABLE,
)
class ShimmerConnectionHandler:
    def __init__(self, default_sampling_rate: int = 128, data_buffer_size: int = 1000):
        self.logger = get_logger(__name__)
        self.default_sampling_rate = default_sampling_rate
        self.data_buffer_size = data_buffer_size
        self.connected_devices: Dict[str, ShimmerBluetooth] = {}
        self.device_status: Dict[str, ShimmerStatus] = {}
        self.device_configurations: Dict[str, DeviceConfiguration] = {}
        self.data_queues: Dict[str, queue.Queue] = {}
    def connect_devices(self, device_info: Dict[str, List[str]]) -> bool:
        total_devices = 0
        success_count = 0
        if "direct" in device_info:
            direct_success = self._connect_direct_devices(device_info["direct"])
            total_devices += len(device_info["direct"])
            success_count += direct_success
        if "simulated" in device_info:
            simulated_success = self._connect_simulated_devices(
                device_info["simulated"]
            )
            total_devices += len(device_info["simulated"])
            success_count += simulated_success
        all_connected = success_count == total_devices
        self.logger.info(
            f"Connection results: {success_count}/{total_devices} successful"
        )
        return all_connected
    def _connect_direct_devices(self, device_addresses: List[str]) -> int:
        success_count = 0
        for mac_address in device_addresses:
            if self._connect_single_device(
                mac_address, ConnectionType.DIRECT_BLUETOOTH
            ):
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
                return self._setup_simulated_device(
                    device_id, mac_address, connection_type
                )
            elif connection_type == ConnectionType.DIRECT_BLUETOOTH:
                return self._setup_bluetooth_device(
                    device_id, mac_address, connection_type
                )
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
            shimmer_bt = ShimmerBluetooth(mac_address)
            if shimmer_bt.connect():
                self.connected_devices[device_id] = shimmer_bt
                self._configure_bluetooth_device(shimmer_bt, device_id, mac_address)
                self.logger.info(
                    f"Successfully connected to Bluetooth device {device_id}"
                )
                return True
            else:
                self.logger.error(f"Failed to connect to Bluetooth device {device_id}")
                return False
        except Exception as e:
            self.logger.error(f"Bluetooth connection error for {device_id}: {e}")
            return False
    def _configure_bluetooth_device(
        self, shimmer_bt: ShimmerBluetooth, device_id: str, mac_address: str
    ) -> None:
        self.device_status[device_id] = ShimmerStatus(
            is_available=True,
            is_connected=True,
            device_state=DeviceState.CONNECTED,
            connection_type=ConnectionType.DIRECT_BLUETOOTH,
            device_name=f"Shimmer3_{device_id[-4:]}",
            mac_address=mac_address,
            sampling_rate=self.default_sampling_rate,
        )
        self.device_configurations[device_id] = DeviceConfiguration(
            device_id=device_id,
            mac_address=mac_address,
            enabled_channels={"GSR", "PPG_A13", "Accel_X", "Accel_Y", "Accel_Z"},
            connection_type=ConnectionType.DIRECT_BLUETOOTH,
        )
        self.data_queues[device_id] = queue.Queue(maxsize=self.data_buffer_size)
    def disconnect_device(self, device_id: str) -> bool:
        try:
            if device_id in self.connected_devices:
                device = self.connected_devices[device_id]
                if hasattr(device, "disconnect"):
                    device.disconnect()
                del self.connected_devices[device_id]
            if device_id in self.device_status:
                self.device_status[device_id].is_connected = False
                self.device_status[device_id].device_state = DeviceState.DISCONNECTED
            self.logger.info(f"Disconnected device {device_id}")
            return True
        except Exception as e:
            self.logger.error(f"Error disconnecting device {device_id}: {e}")
            return False
    def disconnect_all(self) -> None:
        device_ids = list(self.connected_devices.keys())
        for device_id in device_ids:
            self.disconnect_device(device_id)
    def get_device_status(self, device_id: str) -> Optional[ShimmerStatus]:
        return self.device_status.get(device_id)
    def get_all_device_status(self) -> Dict[str, ShimmerStatus]:
        return self.device_status.copy()
    def is_device_connected(self, device_id: str) -> bool:
        status = self.device_status.get(device_id)
        return status is not None and status.is_connected