import json
import threading
import time
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any, Callable, Dict, List, Optional, Tuple
import cv2
from ..utils.logging_config import get_logger
logger = get_logger(__name__)
class ErrorSeverity(Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"
class ErrorCategory(Enum):
    CAMERA_HARDWARE = "camera_hardware"
    CAMERA_RESOURCE = "camera_resource"
    NETWORK_CONNECTION = "network_connection"
    NETWORK_SYNCHRONIZATION = "network_synchronization"
    CODEC_ENCODING = "codec_encoding"
    FILE_SYSTEM = "file_system"
    CONFIGURATION = "configuration"
    UNKNOWN = "unknown"
@dataclass
class ErrorEvent:
    timestamp: datetime
    category: ErrorCategory
    severity: ErrorSeverity
    message: str
    details: Dict[str, Any]
    recovery_attempted: bool = False
    recovery_successful: bool = False
    retry_count: int = 0
class CameraResourceManager:
    def __init__(self):
        self.active_cameras: Dict[int, str] = {}
        self.resource_lock = threading.Lock()
        self.recovery_attempts: Dict[int, int] = {}
        self.max_recovery_attempts = 3
    def request_camera_access(
        self, camera_index: int, process_name: str = "webcam_capture"
    ) -> Tuple[bool, Optional[str]]:
        with self.resource_lock:
            if camera_index in self.active_cameras:
                current_user = self.active_cameras[camera_index]
                if current_user != process_name:
                    return (
                        False,
                        f"Camera {camera_index} is already in use by {current_user}",
                    )
                else:
                    return True, None
            success, error = self._test_camera_availability(camera_index)
            if not success:
                return False, error
            self.active_cameras[camera_index] = process_name
            print(f"[DEBUG_LOG] Camera {camera_index} reserved for {process_name}")
            return True, None
    def release_camera_access(
        self, camera_index: int, process_name: str = "webcam_capture"
    ):
        with self.resource_lock:
            if camera_index in self.active_cameras:
                if self.active_cameras[camera_index] == process_name:
                    del self.active_cameras[camera_index]
                    print(
                        f"[DEBUG_LOG] Camera {camera_index} released by {process_name}"
                    )
                else:
                    print(
                        f"[DEBUG_LOG] Warning: {process_name} tried to release camera {camera_index} owned by {self.active_cameras[camera_index]}"
                    )
    def _test_camera_availability(
        self, camera_index: int
    ) -> Tuple[bool, Optional[str]]:
        cap = None
        try:
            cap = cv2.VideoCapture(camera_index)
            if not cap.isOpened():
                return (
                    False,
                    f"Camera {camera_index} cannot be opened (may be in use by another application)",
                )
            ret, frame = cap.read()
            if not ret or frame is None:
                return (False, f"Camera {camera_index} opened but cannot read frames")
            return True, None
        except Exception as e:
            return False, f"Error testing camera {camera_index}: {str(e)}"
        finally:
            if cap:
                cap.release()
    def recover_camera_access(
        self, camera_index: int, process_name: str = "webcam_capture"
    ) -> Tuple[bool, Optional[str]]:
        if camera_index not in self.recovery_attempts:
            self.recovery_attempts[camera_index] = 0
        self.recovery_attempts[camera_index] += 1
        if self.recovery_attempts[camera_index] > self.max_recovery_attempts:
            return (
                False,
                f"Maximum recovery attempts ({self.max_recovery_attempts}) exceeded for camera {camera_index}",
            )
        print(
            f"[DEBUG_LOG] Attempting camera {camera_index} recovery (attempt {self.recovery_attempts[camera_index]})"
        )
        time.sleep(1.0 * self.recovery_attempts[camera_index])
        with self.resource_lock:
            if camera_index in self.active_cameras:
                old_user = self.active_cameras[camera_index]
                del self.active_cameras[camera_index]
                print(
                    f"[DEBUG_LOG] Forced release of camera {camera_index} from {old_user}"
                )
        success, error = self.request_camera_access(camera_index, process_name)
        if success:
            self.recovery_attempts[camera_index] = 0
            print(f"[DEBUG_LOG] Camera {camera_index} recovery successful")
        return success, error
    def get_camera_status(self) -> Dict[str, Any]:
        with self.resource_lock:
            return {
                "active_cameras": self.active_cameras.copy(),
                "recovery_attempts": self.recovery_attempts.copy(),
                "total_active": len(self.active_cameras),
            }
class NetworkRecoveryManager:
    def __init__(self):
        self.connection_status: Dict[str, bool] = {}
        self.last_sync_time: Dict[str, datetime] = {}
        self.sync_failures: Dict[str, int] = {}
        self.max_sync_failures = 5
        self.sync_timeout = 10.0
        self.recovery_lock = threading.Lock()
    def register_device(self, device_id: str):
        with self.recovery_lock:
            self.connection_status[device_id] = False
            self.sync_failures[device_id] = 0
            print(f"[DEBUG_LOG] Device {device_id} registered for network monitoring")
    def update_connection_status(self, device_id: str, connected: bool):
        with self.recovery_lock:
            old_status = self.connection_status.get(device_id, False)
            self.connection_status[device_id] = connected
            if connected and not old_status:
                self.sync_failures[device_id] = 0
                print(f"[DEBUG_LOG] Device {device_id} connection restored")
            elif not connected and old_status:
                print(f"[DEBUG_LOG] Device {device_id} connection lost")
    def record_sync_attempt(
        self, device_id: str, success: bool, response_time: float = None
    ):
        with self.recovery_lock:
            current_time = datetime.now()
            if success:
                self.last_sync_time[device_id] = current_time
                self.sync_failures[device_id] = 0
                print(
                    f"[DEBUG_LOG] Sync successful with {device_id} (response: {response_time:.2f}s)"
                )
            else:
                self.sync_failures[device_id] += 1
                print(
                    f"[DEBUG_LOG] Sync failed with {device_id} (failure count: {self.sync_failures[device_id]})"
                )
    def should_attempt_recovery(self, device_id: str) -> bool:
        with self.recovery_lock:
            failure_count = self.sync_failures.get(device_id, 0)
            return failure_count > 0 and failure_count <= self.max_sync_failures
    def attempt_sync_recovery(
        self, device_id: str, sync_function: Callable, *args, **kwargs
    ) -> Tuple[bool, Optional[str]]:
        failure_count = self.sync_failures.get(device_id, 0)
        if failure_count > self.max_sync_failures:
            return (
                False,
                f"Maximum sync failures ({self.max_sync_failures}) exceeded for {device_id}",
            )
        print(
            f"[DEBUG_LOG] Attempting sync recovery with {device_id} (failure count: {failure_count})"
        )
        delay = min(2.0**failure_count, 30.0)
        time.sleep(delay)
        try:
            start_time = time.time()
            result = sync_function(*args, **kwargs)
            response_time = time.time() - start_time
            success = bool(result)
            self.record_sync_attempt(device_id, success, response_time)
            if success:
                return True, None
            else:
                return False, f"Sync function returned falsy result: {result}"
        except Exception as e:
            self.record_sync_attempt(device_id, False)
            return False, f"Sync recovery failed: {str(e)}"
    def get_sync_status(self) -> Dict[str, Any]:
        with self.recovery_lock:
            current_time = datetime.now()
            status = {}
            for device_id in self.connection_status:
                last_sync = self.last_sync_time.get(device_id)
                time_since_sync = None
                if last_sync:
                    time_since_sync = (current_time - last_sync).total_seconds()
                status[device_id] = {
                    "connected": self.connection_status[device_id],
                    "sync_failures": self.sync_failures[device_id],
                    "last_sync_time": last_sync.isoformat() if last_sync else None,
                    "time_since_sync": time_since_sync,
                    "needs_recovery": self.should_attempt_recovery(device_id),
                }
            return status
class ErrorRecoveryManager:
    def __init__(self):
        self.camera_manager = CameraResourceManager()
        self.network_manager = NetworkRecoveryManager()
        self.error_history: List[ErrorEvent] = []
        self.recovery_strategies: Dict[ErrorCategory, Callable] = {}
        self.max_history_size = 1000
        self._register_default_strategies()
    def _register_default_strategies(self):
        self.recovery_strategies[ErrorCategory.CAMERA_RESOURCE] = (
            self._recover_camera_resource
        )
        self.recovery_strategies[ErrorCategory.NETWORK_SYNCHRONIZATION] = (
            self._recover_network_sync
        )
        self.recovery_strategies[ErrorCategory.CODEC_ENCODING] = (
            self._recover_codec_encoding
        )
        self.recovery_strategies[ErrorCategory.CAMERA_HARDWARE] = (
            self._recover_camera_hardware
        )
    def classify_error(
        self, error_message: str, context: Dict[str, Any] = None
    ) -> Tuple[ErrorCategory, ErrorSeverity]:
        error_lower = error_message.lower()
        context = context or {}
        if any(
            keyword in error_lower
            for keyword in ["in use", "already open", "resource busy", "access denied"]
        ):
            return ErrorCategory.CAMERA_RESOURCE, ErrorSeverity.MEDIUM
        if any(
            keyword in error_lower
            for keyword in [
                "camera not found",
                "no camera",
                "device not available",
                "hardware error",
            ]
        ):
            return ErrorCategory.CAMERA_HARDWARE, ErrorSeverity.HIGH
        if any(
            keyword in error_lower
            for keyword in ["connection", "network", "timeout", "unreachable"]
        ):
            if "sync" in error_lower or "synchronisation" in error_lower:
                return (ErrorCategory.NETWORK_SYNCHRONIZATION, ErrorSeverity.MEDIUM)
            else:
                return ErrorCategory.NETWORK_CONNECTION, ErrorSeverity.HIGH
        if any(
            keyword in error_lower
            for keyword in ["codec", "encoding", "fourcc", "video writer"]
        ):
            return ErrorCategory.CODEC_ENCODING, ErrorSeverity.MEDIUM
        if any(
            keyword in error_lower
            for keyword in ["file", "directory", "permission", "disk", "space"]
        ):
            return ErrorCategory.FILE_SYSTEM, ErrorSeverity.MEDIUM
        if any(
            keyword in error_lower
            for keyword in ["config", "setting", "parameter", "invalid"]
        ):
            return ErrorCategory.CONFIGURATION, ErrorSeverity.LOW
        return ErrorCategory.UNKNOWN, ErrorSeverity.MEDIUM
    def handle_error(
        self,
        error_message: str,
        context: Dict[str, Any] = None,
        auto_recover: bool = True,
    ) -> Tuple[bool, Optional[str]]:
        category, severity = self.classify_error(error_message, context)
        error_event = ErrorEvent(
            timestamp=datetime.now(),
            category=category,
            severity=severity,
            message=error_message,
            details=context or {},
        )
        self._add_to_history(error_event)
        print(
            f"[DEBUG_LOG] Error classified: {category.value} ({severity.value}) - {error_message}"
        )
        if auto_recover and category in self.recovery_strategies:
            error_event.recovery_attempted = True
            try:
                recovery_func = self.recovery_strategies[category]
                success, message = recovery_func(error_event)
                error_event.recovery_successful = success
                if success:
                    print(f"[DEBUG_LOG] Error recovery successful: {message}")
                else:
                    print(f"[DEBUG_LOG] Error recovery failed: {message}")
                return success, message
            except Exception as e:
                error_event.recovery_successful = False
                recovery_message = f"Recovery strategy failed: {str(e)}"
                print(f"[DEBUG_LOG] {recovery_message}")
                return False, recovery_message
        return False, "No recovery strategy available"
    def _recover_camera_resource(self, error_event: ErrorEvent) -> Tuple[bool, str]:
        context = error_event.details
        camera_index = context.get("camera_index", 0)
        process_name = context.get("process_name", "webcam_capture")
        success, message = self.camera_manager.recover_camera_access(
            camera_index, process_name
        )
        if success:
            return True, f"Camera {camera_index} access recovered"
        else:
            return False, f"Failed to recover camera {camera_index}: {message}"
    def _recover_network_sync(self, error_event: ErrorEvent) -> Tuple[bool, str]:
        context = error_event.details
        device_id = context.get("device_id", "unknown")
        sync_function = context.get("sync_function")
        sync_args = context.get("sync_args", [])
        sync_kwargs = context.get("sync_kwargs", {})
        if not sync_function:
            return False, "No sync function provided for recovery"
        success, message = self.network_manager.attempt_sync_recovery(
            device_id, sync_function, *sync_args, **sync_kwargs
        )
        return success, message or "Network sync recovery completed"
    def _recover_codec_encoding(self, error_event: ErrorEvent) -> Tuple[bool, str]:
        from config.webcam_config import CodecValidator, VideoCodec
        context = error_event.details
        failed_codec = context.get("codec")
        fallback_codecs = context.get(
            "fallback_codecs", [VideoCodec.MP4V, VideoCodec.XVID, VideoCodec.MJPG]
        )
        for codec in fallback_codecs:
            if codec.value != failed_codec and CodecValidator.test_codec(codec):
                return True, f"Switched to fallback codec: {codec.value}"
        return False, "No working fallback codec found"
    def _recover_camera_hardware(self, error_event: ErrorEvent) -> Tuple[bool, str]:
        context = error_event.details
        camera_index = context.get("camera_index", 0)
        for alt_index in range(5):
            if alt_index != camera_index:
                success, _ = self.camera_manager._test_camera_availability(alt_index)
                if success:
                    return (True, f"Alternative camera found at index {alt_index}")
        return False, "No alternative camera hardware found"
    def _add_to_history(self, error_event: ErrorEvent):
        self.error_history.append(error_event)
        if len(self.error_history) > self.max_history_size:
            self.error_history = self.error_history[-self.max_history_size :]
    def get_error_statistics(self) -> Dict[str, Any]:
        if not self.error_history:
            return {"total_errors": 0}
        category_counts = {}
        severity_counts = {}
        recovery_stats = {"attempted": 0, "successful": 0}
        recent_errors = [
            e
            for e in self.error_history
            if (datetime.now() - e.timestamp).total_seconds() < 3600
        ]
        for error in self.error_history:
            category_counts[error.category.value] = (
                category_counts.get(error.category.value, 0) + 1
            )
            severity_counts[error.severity.value] = (
                severity_counts.get(error.severity.value, 0) + 1
            )
            if error.recovery_attempted:
                recovery_stats["attempted"] += 1
                if error.recovery_successful:
                    recovery_stats["successful"] += 1
        recovery_rate = 0
        if recovery_stats["attempted"] > 0:
            recovery_rate = (
                recovery_stats["successful"] / recovery_stats["attempted"] * 100
            )
        return {
            "total_errors": len(self.error_history),
            "recent_errors": len(recent_errors),
            "category_breakdown": category_counts,
            "severity_breakdown": severity_counts,
            "recovery_stats": recovery_stats,
            "recovery_rate_percent": recovery_rate,
            "camera_status": self.camera_manager.get_camera_status(),
            "network_status": self.network_manager.get_sync_status(),
        }
    def register_recovery_strategy(
        self, category: ErrorCategory, strategy_func: Callable
    ):
        self.recovery_strategies[category] = strategy_func
        print(f"[DEBUG_LOG] Custom recovery strategy registered for {category.value}")
error_recovery_manager = ErrorRecoveryManager()
def handle_error_with_recovery(
    error_message: str, **context
) -> Tuple[bool, Optional[str]]:
    return error_recovery_manager.handle_error(error_message, context)
if __name__ == "__main__":
    print("[DEBUG_LOG] Testing Error Handling and Recovery System...")
    print("\n--- Testing Camera Resource Conflict ---")
    success, message = handle_error_with_recovery(
        "Camera 0 is already in use by another application",
        camera_index=0,
        process_name="test_process",
    )
    print(f"Recovery result: {success}, Message: {message}")
    print("\n--- Testing Network Synchronisation Error ---")
    def mock_sync_function():
        return True
    success, message = handle_error_with_recovery(
        "Network synchronisation timeout with device",
        device_id="test_device",
        sync_function=mock_sync_function,
        sync_args=[],
        sync_kwargs={},
    )
    print(f"Recovery result: {success}, Message: {message}")
    print("\n--- Testing Codec Encoding Error ---")
    success, message = handle_error_with_recovery(
        "Video codec XVID encoding failed",
        codec="XVID",
        fallback_codecs=["mp4v", "MJPG"],
    )
    print(f"Recovery result: {success}, Message: {message}")
    print("\n--- Error Statistics ---")
    stats = error_recovery_manager.get_error_statistics()
    print(json.dumps(stats, indent=2, default=str))
    print(
        "\n[DEBUG_LOG] Error handling and recovery system test completed successfully"
    )