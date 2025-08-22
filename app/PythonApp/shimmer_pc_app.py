import argparse
import asyncio
import logging
import signal
import sys
import threading
import time
from pathlib import Path
from typing import Any, Dict, List, Optional
sys.path.insert(0, str(Path(__file__).parent))
from ..network.android_device_manager import AndroidDeviceManager, ShimmerDataSample
from .shimmer_manager import (
    ConnectionType,
    DeviceState,
    ShimmerManager,
    ShimmerSample,
    ShimmerStatus,
)
class ShimmerPCApplication:
    def __init__(self, android_port: int = 9000, enable_gui: bool = False):
        self.android_port = android_port
        self.enable_gui = enable_gui
        self.logger = logging.getLogger(__name__)
        self.shimmer_manager: Optional[ShimmerManager] = None
        self.android_manager: Optional[AndroidDeviceManager] = None
        self.is_running = False
        self.current_session_id: Optional[str] = None
        self.connected_devices: Dict[str, Dict[str, Any]] = {}
        self.data_samples_received = 0
        self.session_start_time: Optional[float] = None
        self.device_stats: Dict[str, Dict[str, Any]] = {}
        self.monitor_thread: Optional[threading.Thread] = None
        self.stats_thread: Optional[threading.Thread] = None
        self.logger.info("Shimmer PC Application initialized")
    def initialize(self) -> bool:
        try:
            self.logger.info("Initializing Shimmer PC Application...")
            self.shimmer_manager = ShimmerManager(
                enable_android_integration=True, logger=self.logger
            )
            self.shimmer_manager.add_data_callback(self._on_shimmer_data)
            self.shimmer_manager.add_status_callback(self._on_status_update)
            self.shimmer_manager.add_android_device_callback(
                self._on_android_device_event
            )
            self.shimmer_manager.add_connection_state_callback(
                self._on_connection_state_change
            )
            if not self.shimmer_manager.initialize():
                self.logger.error("Failed to initialize Shimmer Manager")
                return False
            self.android_manager = self.shimmer_manager.android_device_manager
            self._start_monitoring_threads()
            self.is_running = True
            self.logger.info("Shimmer PC Application initialized successfully")
            return True
        except Exception as e:
            self.logger.error(f"Failed to initialize application: {e}")
            return False
    def run(self) -> None:
        try:
            if not self.is_running:
                self.logger.error("Application not initialized")
                return
            self.logger.info("Starting Shimmer PC Application...")
            self.logger.info(
                f"Android device server listening on port {self.android_port}"
            )
            self.logger.info("Connect Android devices to begin data collection")
            self.logger.info("Press Ctrl+C to stop the application")
            signal.signal(signal.SIGINT, self._signal_handler)
            signal.signal(signal.SIGTERM, self._signal_handler)
            while self.is_running:
                try:
                    self._check_device_discovery()
                    self._show_periodic_status()
                    self._handle_console_commands()
                    time.sleep(1)
                except KeyboardInterrupt:
                    break
                except Exception as e:
                    self.logger.error(f"Error in main loop: {e}")
                    time.sleep(5)
        except Exception as e:
            self.logger.error(f"Error running application: {e}")
        finally:
            self.shutdown()
    def shutdown(self) -> None:
        try:
            self.logger.info("Shutting down Shimmer PC Application...")
            self.is_running = False
            if self.current_session_id:
                self.stop_session()
            if self.monitor_thread and self.monitor_thread.is_alive():
                self.monitor_thread.join(timeout=5)
            if self.stats_thread and self.stats_thread.is_alive():
                self.stats_thread.join(timeout=5)
            if self.shimmer_manager:
                self.shimmer_manager.cleanup()
            self.logger.info("Shimmer PC Application shutdown completed")
        except Exception as e:
            self.logger.error(f"Error during shutdown: {e}")
    def start_session(self, session_id: str, record_shimmer: bool = True) -> bool:
        try:
            if self.current_session_id:
                self.logger.warning(
                    f"Session already active: {self.current_session_id}"
                )
                return False
            self.logger.info(f"Starting session: {session_id}")
            android_devices = self.shimmer_manager.get_android_devices()
            android_success = False
            if android_devices:
                android_success = self.shimmer_manager.start_android_session(
                    session_id,
                    record_shimmer=record_shimmer,
                    record_video=True,
                    record_thermal=True,
                )
                if android_success:
                    self.logger.info(
                        f"Android session started on {len(android_devices)} devices"
                    )
            local_success = self.shimmer_manager.start_recording(session_id)
            if android_success or local_success:
                self.current_session_id = session_id
                self.session_start_time = time.time()
                self.data_samples_received = 0
                self.logger.info(f"Session started successfully: {session_id}")
                return True
            else:
                self.logger.error("Failed to start session")
                return False
        except Exception as e:
            self.logger.error(f"Error starting session: {e}")
            return False
    def stop_session(self) -> bool:
        try:
            if not self.current_session_id:
                self.logger.warning("No active session to stop")
                return False
            session_id = self.current_session_id
            self.logger.info(f"Stopping session: {session_id}")
            android_success = self.shimmer_manager.stop_android_session()
            local_success = self.shimmer_manager.stop_recording()
            if self.session_start_time:
                duration = time.time() - self.session_start_time
                self.logger.info(f"Session completed: {session_id}")
                self.logger.info(f"  Duration: {duration:.1f} seconds")
                self.logger.info(f"  Data samples: {self.data_samples_received}")
                self.logger.info(
                    f"  Sample rate: {self.data_samples_received / duration:.1f} samples/sec"
                )
            self.current_session_id = None
            self.session_start_time = None
            return android_success or local_success
        except Exception as e:
            self.logger.error(f"Error stopping session: {e}")
            return False
    def send_sync_signal(self, signal_type: str = "flash", **kwargs) -> int:
        count = self.shimmer_manager.send_sync_signal(signal_type, **kwargs)
        self.logger.info(f"Sent {signal_type} sync signal to {count} devices")
        return count
    def get_status_summary(self) -> Dict[str, Any]:
        android_devices = self.shimmer_manager.get_android_devices()
        shimmer_devices = self.shimmer_manager.get_shimmer_status()
        return {
            "android_devices": len(android_devices),
            "shimmer_devices": len(shimmer_devices),
            "active_session": self.current_session_id,
            "data_samples_received": self.data_samples_received,
            "devices_streaming": sum(
                1 for status in shimmer_devices.values() if status.is_streaming
            ),
            "devices_recording": sum(
                1 for status in shimmer_devices.values() if status.is_recording
            ),
        }
    def _on_shimmer_data(self, sample: ShimmerSample) -> None:
        self.data_samples_received += 1
        if sample.device_id not in self.device_stats:
            self.device_stats[sample.device_id] = {
                "samples_received": 0,
                "last_timestamp": None,
                "connection_type": sample.connection_type.value,
            }
        stats = self.device_stats[sample.device_id]
        stats["samples_received"] += 1
        stats["last_timestamp"] = sample.timestamp
        if self.data_samples_received % 100 == 0:
            self.logger.debug(f"Received {self.data_samples_received} samples total")
    def _on_status_update(self, device_id: str, status: ShimmerStatus) -> None:
        self.connected_devices[device_id] = {
            "connection_type": status.connection_type.value,
            "device_state": status.device_state.value,
            "is_streaming": status.is_streaming,
            "is_recording": status.is_recording,
            "battery_level": status.battery_level,
            "samples_recorded": status.samples_recorded,
        }
    def _on_android_device_event(self, device_id: str, status: Dict[str, Any]) -> None:
        self.logger.debug(f"Android device {device_id} status: {status}")
    def _on_connection_state_change(
        self, device_id: str, state: DeviceState, connection_type: ConnectionType
    ) -> None:
        self.logger.info(f"Device {device_id} ({connection_type.value}): {state.value}")
    def _start_monitoring_threads(self) -> None:
        self.monitor_thread = threading.Thread(
            target=self._device_monitor_loop, daemon=True
        )
        self.monitor_thread.start()
        self.stats_thread = threading.Thread(target=self._statistics_loop, daemon=True)
        self.stats_thread.start()
    def _device_monitor_loop(self) -> None:
        while self.is_running:
            try:
                shimmer_devices = self.shimmer_manager.get_shimmer_status()
                android_devices = self.shimmer_manager.get_android_devices()
                for device_id, status in shimmer_devices.items():
                    if not status.is_connected:
                        self.logger.warning(f"Device disconnected: {device_id}")
                time.sleep(10)
            except Exception as e:
                self.logger.error(f"Error in device monitor: {e}")
                time.sleep(5)
    def _statistics_loop(self) -> None:
        while self.is_running:
            try:
                if self.current_session_id and self.session_start_time:
                    duration = time.time() - self.session_start_time
                    if int(duration) % 60 == 0 and duration > 0:
                        self.logger.info(
                            f"Session {self.current_session_id}: {duration:.0f}s, {self.data_samples_received} samples"
                        )
                time.sleep(1)
            except Exception as e:
                self.logger.error(f"Error in statistics loop: {e}")
                time.sleep(5)
    def _check_device_discovery(self) -> None:
        pass
    def _show_periodic_status(self) -> None:
        if int(time.time()) % 30 == 0:
            status = self.get_status_summary()
            self.logger.info(
                f"Status: {status['android_devices']} Android, {status['shimmer_devices']} Shimmer, {status['data_samples_received']} samples"
            )
    def _handle_console_commands(self) -> None:
        pass
    def _signal_handler(self, signum, frame) -> None:
        self.logger.info(f"Received signal {signum}, shutting down...")
        self.is_running = False
def main():
    parser = argparse.ArgumentParser(description="Shimmer PC Integration Application")
    parser.add_argument(
        "--port",
        type=int,
        default=9000,
        help="Android device server port (default: 9000)",
    )
    parser.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR"],
        help="Logging level (default: INFO)",
    )
    parser.add_argument(
        "--session-id",
        type=str,
        help="Automatically start a recording session with this ID",
    )
    parser.add_argument(
        "--duration",
        type=int,
        default=0,
        help="Session duration in seconds (0 = unlimited)",
    )
    args = parser.parse_args()
    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler("shimmer_pc_app.log"),
        ],
    )
    logger = logging.getLogger(__name__)
    logger.info("=== Shimmer PC Integration Application Starting ===")
    app = ShimmerPCApplication(android_port=args.port)
    try:
        if app.initialize():
            logger.info("Application initialized successfully")
            if args.session_id:
                time.sleep(2)
                if app.start_session(args.session_id):
                    logger.info(f"Auto-started session: {args.session_id}")
                    if args.duration > 0:
                        logger.info(f"Running for {args.duration} seconds...")
                        time.sleep(args.duration)
                        app.stop_session()
                        app.shutdown()
                        return
            app.run()
        else:
            logger.error("Failed to initialize application")
            sys.exit(1)
    except Exception as e:
        logger.error(f"Application error: {e}")
        sys.exit(1)
if __name__ == "__main__":
    main()