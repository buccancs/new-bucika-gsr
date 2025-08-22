from typing import Any, Dict, List, Optional
from PyQt5.QtCore import QObject, pyqtSignal
from ..network.device_server import JsonSocketServer
from ..session.session_logger import get_session_logger
from ..session.session_manager import SessionManager
from ..utils.logging_config import get_logger
from ..gui.stimulus_controller import StimulusController
from ..webcam.webcam_capture import WebcamCapture
class MainController(QObject):
    server_status_changed = pyqtSignal(bool)
    webcam_status_changed = pyqtSignal(str)
    session_status_changed = pyqtSignal(str, bool)
    device_connected = pyqtSignal(str, list)
    device_disconnected = pyqtSignal(str)
    device_status_received = pyqtSignal(str, dict)
    preview_frame_received = pyqtSignal(str, str, str)
    webcam_frame_ready = pyqtSignal(object)
    sensor_data_received = pyqtSignal(str, dict)
    recording_started = pyqtSignal(str)
    recording_stopped = pyqtSignal(str, float)
    calibration_completed = pyqtSignal(str, dict)
    error_occurred = pyqtSignal(str, str)
    def __init__(self, parent=None):
        super().__init__(parent)
        self.logger = get_logger(__name__)
        self._session_manager: Optional[SessionManager] = None
        self._json_server: Optional[JsonSocketServer] = None
        self._webcam_capture: Optional[WebcamCapture] = None
        self._stimulus_controller: Optional[StimulusController] = None
        self._session_logger = None
        self._server_running = False
        self._webcam_previewing = False
        self._webcam_recording = False
        self._current_session_id: Optional[str] = None
        self.logger.info("MainController initialized")
    def inject_dependencies(
        self,
        session_manager: SessionManager,
        json_server: JsonSocketServer,
        webcam_capture: WebcamCapture,
        stimulus_controller: StimulusController,
    ):
        self._session_manager = session_manager
        self._json_server = json_server
        self._webcam_capture = webcam_capture
        self._stimulus_controller = stimulus_controller
        self._session_logger = get_session_logger()
        self._connect_service_signals()
        self.logger.info("Dependencies injected successfully")
    def _connect_service_signals(self):
        if not all(
            [self._json_server, self._webcam_capture, self._stimulus_controller]
        ):
            raise RuntimeError(
                "Dependencies must be injected before connecting signals"
            )
        self._json_server.device_connected.connect(self._on_device_connected)
        self._json_server.device_disconnected.connect(self._on_device_disconnected)
        self._json_server.status_received.connect(self._on_status_received)
        self._json_server.ack_received.connect(self._on_ack_received)
        self._json_server.preview_frame_received.connect(
            self._on_preview_frame_received
        )
        self._json_server.sensor_data_received.connect(self._on_sensor_data_received)
        self._json_server.notification_received.connect(self._on_notification_received)
        self._json_server.error_occurred.connect(self._on_server_error)
        self._webcam_capture.frame_ready.connect(self._on_webcam_frame_ready)
        self._webcam_capture.recording_started.connect(
            self._on_webcam_recording_started
        )
        self._webcam_capture.recording_stopped.connect(
            self._on_webcam_recording_stopped
        )
        self._webcam_capture.error_occurred.connect(self._on_webcam_error)
        self._webcam_capture.status_changed.connect(self._on_webcam_status_changed)
        self._stimulus_controller.seek_requested.connect(
            self._on_stimulus_seek_requested
        )
        self._stimulus_controller.screen_changed.connect(
            self._on_stimulus_screen_changed
        )
        self._stimulus_controller.start_recording_play_requested.connect(
            self._on_start_recording_play_requested
        )
        self._stimulus_controller.mark_event_requested.connect(
            self._on_mark_event_requested
        )
        self._stimulus_controller.status_changed.connect(
            self._on_stimulus_status_changed
        )
        self._stimulus_controller.experiment_started.connect(
            self._on_stimulus_experiment_started
        )
        self._stimulus_controller.experiment_ended.connect(
            self._on_stimulus_experiment_ended
        )
        self._stimulus_controller.error_occurred.connect(self._on_stimulus_error)
        if self._session_logger:
            self._session_logger.session_started.connect(
                self._on_session_logger_session_started
            )
            self._session_logger.session_ended.connect(
                self._on_session_logger_session_ended
            )
            self._session_logger.error_occurred.connect(self._on_session_logger_error)
    def start_server(self) -> bool:
        try:
            if not self._json_server:
                raise RuntimeError("JSON server not injected")
            if not self._server_running:
                self._json_server.start()
                self._server_running = True
                self.server_status_changed.emit(True)
                self.logger.info("Server started successfully")
                return True
            return True
        except Exception as e:
            self.logger.error(f"Failed to start server: {e}")
            self.error_occurred.emit("server", str(e))
            return False
    def stop_server(self) -> bool:
        try:
            if self._json_server and self._server_running:
                self._json_server.stop_server()
                self._server_running = False
                self.server_status_changed.emit(False)
                self.logger.info("Server stopped successfully")
            return True
        except Exception as e:
            self.logger.error(f"Failed to stop server: {e}")
            self.error_occurred.emit("server", str(e))
            return False
    def start_session(self) -> Optional[str]:
        try:
            if not self._session_manager:
                raise RuntimeError("Session manager not injected")
            session_id = self._session_manager.start_session()
            if session_id:
                self._current_session_id = session_id
                self.session_status_changed.emit(session_id, True)
                self.recording_started.emit(session_id)
                self.logger.info(f"Session started: {session_id}")
            return session_id
        except Exception as e:
            self.logger.error(f"Failed to start session: {e}")
            self.error_occurred.emit("session", str(e))
            return None
    def stop_session(self) -> bool:
        try:
            if not self._session_manager or not self._current_session_id:
                return False
            duration = self._session_manager.end_session(self._current_session_id)
            self.session_status_changed.emit(self._current_session_id, False)
            self.recording_stopped.emit(self._current_session_id, duration)
            self.logger.info(f"Session stopped: {self._current_session_id}")
            self._current_session_id = None
            return True
        except Exception as e:
            self.logger.error(f"Failed to stop session: {e}")
            self.error_occurred.emit("session", str(e))
            return False
    def start_webcam_preview(self) -> bool:
        try:
            if not self._webcam_capture:
                raise RuntimeError("Webcam capture not injected")
            if not self._webcam_previewing:
                self._webcam_capture.start_preview()
                self._webcam_previewing = True
                self.logger.info("Webcam preview started")
            return True
        except Exception as e:
            self.logger.error(f"Failed to start webcam preview: {e}")
            self.error_occurred.emit("webcam", str(e))
            return False
    def stop_webcam_preview(self) -> bool:
        try:
            if self._webcam_capture and self._webcam_previewing:
                self._webcam_capture.stop_preview()
                self._webcam_previewing = False
                self.logger.info("Webcam preview stopped")
            return True
        except Exception as e:
            self.logger.error(f"Failed to stop webcam preview: {e}")
            self.error_occurred.emit("webcam", str(e))
            return False
    def start_webcam_recording(self, session_id: str) -> bool:
        try:
            if not self._webcam_capture:
                raise RuntimeError("Webcam capture not injected")
            if not self._webcam_recording:
                self._webcam_capture.start_recording(session_id)
                self._webcam_recording = True
                self.logger.info(f"Webcam recording started for session: {session_id}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to start webcam recording: {e}")
            self.error_occurred.emit("webcam", str(e))
            return False
    def stop_webcam_recording(self) -> bool:
        try:
            if self._webcam_capture and self._webcam_recording:
                self._webcam_capture.stop_recording()
                self._webcam_recording = False
                self.logger.info("Webcam recording stopped")
            return True
        except Exception as e:
            self.logger.error(f"Failed to stop webcam recording: {e}")
            self.error_occurred.emit("webcam", str(e))
            return False
    def send_command_to_device(
        self, device_id: str, command: str, params: Dict[str, Any] = None
    ) -> bool:
        try:
            if not self._json_server:
                raise RuntimeError("JSON server not injected")
            from PythonApp.network.device_server import create_command_message
            command_dict = create_command_message(command, **params or {})
            self._json_server.send_command(device_id, command_dict)
            self.logger.debug(f"Command sent to {device_id}: {command}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to send command to {device_id}: {e}")
            self.error_occurred.emit("network", str(e))
            return False
    def broadcast_command(self, command: str, params: Dict[str, Any] = None) -> bool:
        try:
            if not self._json_server:
                raise RuntimeError("JSON server not injected")
            from PythonApp.network.device_server import create_command_message
            command_dict = create_command_message(command, **params or {})
            self._json_server.broadcast_command(command_dict)
            self.logger.debug(f"Command broadcast: {command}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to broadcast command: {e}")
            self.error_occurred.emit("network", str(e))
            return False
    def get_connected_devices(self) -> List[str]:
        if self._json_server:
            return list(self._json_server.get_connected_devices().keys())
        return []
    def is_server_running(self) -> bool:
        return self._server_running
    def is_webcam_previewing(self) -> bool:
        return self._webcam_previewing
    def is_webcam_recording(self) -> bool:
        return self._webcam_recording
    def get_current_session_id(self) -> Optional[str]:
        return self._current_session_id
    def _on_device_connected(self, device_id: str, capabilities: list):
        self.device_connected.emit(device_id, capabilities)
        self.logger.info(f"Device connected: {device_id}")
    def _on_device_disconnected(self, device_id: str):
        self.device_disconnected.emit(device_id)
        self.logger.info(f"Device disconnected: {device_id}")
    def _on_status_received(self, device_id: str, status_data: dict):
        self.device_status_received.emit(device_id, status_data)
    def _on_ack_received(self, device_id: str, cmd: str, success: bool, message: str):
        if not success:
            self.error_occurred.emit(device_id, f"Command {cmd} failed: {message}")
    def _on_preview_frame_received(
        self, device_id: str, frame_type: str, base64_data: str
    ):
        self.preview_frame_received.emit(device_id, frame_type, base64_data)
    def _on_sensor_data_received(self, device_id: str, sensor_data: dict):
        self.sensor_data_received.emit(device_id, sensor_data)
    def _on_notification_received(
        self, device_id: str, event_type: str, event_data: dict
    ):
        self.logger.debug(f"Notification from {device_id}: {event_type}")
    def _on_server_error(self, device_id: str, error_message: str):
        self.error_occurred.emit(device_id, error_message)
    def _on_webcam_frame_ready(self, pixmap):
        self.webcam_frame_ready.emit(pixmap)
    def _on_webcam_recording_started(self, filepath: str):
        self.logger.info(f"Webcam recording started: {filepath}")
    def _on_webcam_recording_stopped(self, filepath: str, duration: float):
        self.logger.info(
            f"Webcam recording stopped: {filepath} (duration: {duration}s)"
        )
    def _on_webcam_error(self, error_message: str):
        self.error_occurred.emit("webcam", error_message)
    def _on_webcam_status_changed(self, status_message: str):
        self.webcam_status_changed.emit(status_message)
    def _on_stimulus_seek_requested(self, position: float):
        try:
            if hasattr(self, "stimulus_controller") and self.stimulus_controller:
                duration = self.stimulus_controller.get_duration()
                if duration > 0:
                    seek_time = int(position / 100.0 * duration)
                    self.stimulus_controller.seek_to_position(seek_time)
                    self.stimulus_status_changed.emit(
                        f"Stimulus seek to {position:.1f}% ({seek_time}ms)"
                    )
                else:
                    self.stimulus_status_changed.emit("Cannot seek: no media loaded")
            else:
                self.stimulus_status_changed.emit("Stimulus controller not available")
        except Exception as e:
            error_msg = f"Error seeking stimulus: {str(e)}"
            self.stimulus_status_changed.emit(error_msg)
            self.error_occurred.emit(error_msg)
    def _on_stimulus_screen_changed(self, screen_index: int):
        try:
            if hasattr(self, "stimulus_controller") and self.stimulus_controller:
                if hasattr(self.stimulus_controller, "set_display_screen"):
                    self.stimulus_controller.set_display_screen(screen_index)
                    self.stimulus_status_changed.emit(
                        f"Stimulus display screen set to {screen_index}"
                    )
                else:
                    self.stimulus_controller.target_screen = screen_index
                    self.stimulus_status_changed.emit(
                        f"Stimulus target screen set to {screen_index}"
                    )
                self.stimulus_screen_changed.emit(screen_index)
            else:
                self.stimulus_status_changed.emit("Stimulus controller not available")
        except Exception as e:
            error_msg = f"Error changing stimulus screen: {str(e)}"
            self.stimulus_status_changed.emit(error_msg)
            self.error_occurred.emit(error_msg)
    def _on_start_recording_play_requested(self):
        self.start_session()
    def _on_mark_event_requested(self):
        if self._session_logger and self._current_session_id:
            self._session_logger.log_event(
                self._current_session_id, "user_marked_event", {}
            )
    def _on_stimulus_status_changed(self, status_message: str):
        self.logger.debug(f"Stimulus status: {status_message}")
    def _on_stimulus_experiment_started(self):
        self.logger.info("Stimulus experiment started")
    def _on_stimulus_experiment_ended(self):
        self.logger.info("Stimulus experiment ended")
    def _on_stimulus_error(self, error_message: str):
        self.error_occurred.emit("stimulus", error_message)
    def _on_session_logger_session_started(self, session_id: str):
        self.logger.info(f"Session logger started for: {session_id}")
    def _on_session_logger_session_ended(self, session_id: str, duration: float):
        self.logger.info(
            f"Session logger ended for: {session_id} (duration: {duration}s)"
        )
    def _on_session_logger_error(self, error_type: str, error_message: str):
        self.error_occurred.emit("session_logger", f"{error_type}: {error_message}")
    def cleanup(self):
        try:
            self.stop_server()
            self.stop_webcam_preview()
            self.stop_webcam_recording()
            if self._current_session_id:
                self.stop_session()
            self.logger.info("MainController cleanup completed")
        except Exception as e:
            self.logger.error(f"Error during cleanup: {e}")
