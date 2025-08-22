import json
import os
import threading
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Callable, Dict, List, Optional, Tuple
import cv2
import numpy as np
from PyQt5.QtCore import QThread, pyqtSignal
from PyQt5.QtGui import QImage, QPixmap
from ..utils.logging_config import get_logger
from .webcam.advanced_sync_algorithms import (
    AdaptiveSynchronizer,
    SynchronizationStrategy,
)
from .webcam.cv_preprocessing_pipeline import (
    AdvancedROIDetector,
    PhysiologicalSignalExtractor,
    ROIDetectionMethod,
    SignalExtractionMethod,
)
logger = get_logger(__name__)
@dataclass
class DualFrameData:
    timestamp: float
    frame_id: int
    camera1_frame: Optional[np.ndarray]
    camera2_frame: Optional[np.ndarray]
    camera1_timestamp: float
    camera2_timestamp: float
    sync_quality: float
@dataclass
class CameraStatus:
    camera_index: int
    is_active: bool
    fps: float
    resolution: Tuple[int, int]
    frames_captured: int
    last_error: Optional[str]
    temperature: Optional[float]
class DualWebcamCapture(QThread):
    dual_frame_ready = pyqtSignal(QPixmap, QPixmap)
    recording_started = pyqtSignal(str, str)
    recording_stopped = pyqtSignal(str, str, float)
    sync_status_changed = pyqtSignal(float)
    camera_status_changed = pyqtSignal(dict)
    error_occurred = pyqtSignal(str)
    timestamp_sync_update = pyqtSignal(float)
    def __init__(
        self,
        camera1_index: int = 0,
        camera2_index: int = 1,
        preview_fps: int = 30,
        recording_fps: int = 30,
        resolution: Tuple[int, int] = (3840, 2160),
        sync_callback: Optional[Callable[[float], None]] = None,
    ):
        super().__init__()
        self.camera1_index = camera1_index
        self.camera2_index = camera2_index
        self.preview_fps = preview_fps
        self.recording_fps = min(recording_fps, 30)
        self.target_resolution = resolution
        self.sync_callback = sync_callback
        self.frame_interval = 1.0 / preview_fps
        self.recording_interval = 1.0 / self.recording_fps
        self.cap1: Optional[cv2.VideoCapture] = None
        self.cap2: Optional[cv2.VideoCapture] = None
        self.writer1: Optional[cv2.VideoWriter] = None
        self.writer2: Optional[cv2.VideoWriter] = None
        self.is_recording = False
        self.is_previewing = False
        self.running = False
        self.recording_codec = cv2.VideoWriter_fourcc(*"mp4v")
        self.current_session_id: Optional[str] = None
        self.recording_start_time: Optional[float] = None
        self.output_directory = "recordings/dual_webcam"
        self.frame_counter = 0
        self.master_start_time = None
        self.sync_threshold_ms = 16.67
        self.frame_sync_buffer: List[DualFrameData] = []
        self.max_sync_buffer_size = 10
        self.synchronizer = AdaptiveSynchronizer(
            target_fps=preview_fps,
            sync_threshold_ms=self.sync_threshold_ms,
            strategy=SynchronizationStrategy.ADAPTIVE_HYBRID,
        )
        self.frame_lock = threading.Lock()
        self.last_frames = {"camera1": None, "camera2": None}
        self.last_sync_quality = 1.0
        self.roi_detector = AdvancedROIDetector(
            method=ROIDetectionMethod.MEDIAPIPE_HANDS,
            tracking_enabled=True,
            stability_threshold=0.8,
        )
        self.signal_extractor = PhysiologicalSignalExtractor(
            method=SignalExtractionMethod.CHROM_METHOD,
            sampling_rate=preview_fps,
            signal_length_seconds=10.0,
        )
        self.enable_physio_monitoring = False
        self.current_roi = None
        self.latest_physio_signal = None
        self.camera1_status = CameraStatus(
            camera1_index, False, 0, (0, 0), 0, None, None
        )
        self.camera2_status = CameraStatus(
            camera2_index, False, 0, (0, 0), 0, None, None
        )
        self.performance_stats = {
            "frames_processed": 0,
            "sync_violations": 0,
            "dropped_frames": 0,
            "average_processing_time_ms": 0.0,
        }
        logger.info(
            f"DualWebcamCapture initialized: cameras {camera1_index},{camera2_index}, recording {self.recording_fps}fps @ {resolution}"
        )
    def initialize_cameras(self) -> bool:
        try:
            logger.info("Initializing dual Logitech Brio cameras...")
            self.cap1 = cv2.VideoCapture(self.camera1_index)
            if not self.cap1.isOpened():
                self.error_occurred.emit(f"Could not open camera {self.camera1_index}")
                return False
            self.cap2 = cv2.VideoCapture(self.camera2_index)
            if not self.cap2.isOpened():
                self.error_occurred.emit(f"Could not open camera {self.camera2_index}")
                return False
            success1 = self._configure_brio_camera(self.cap1, 1)
            success2 = self._configure_brio_camera(self.cap2, 2)
            if not (success1 and success2):
                self.error_occurred.emit(
                    "Failed to configure cameras for optimal performance"
                )
                return False
            self._update_camera_status()
            logger.info("Dual cameras initialized successfully")
            return True
        except Exception as e:
            error_msg = f"Error initializing dual cameras: {str(e)}"
            self.error_occurred.emit(error_msg)
            logger.error(error_msg)
            return False
    def _configure_brio_camera(self, cap: cv2.VideoCapture, camera_num: int) -> bool:
        try:
            cap.set(cv2.CAP_PROP_FRAME_WIDTH, self.target_resolution[0])
            cap.set(cv2.CAP_PROP_FRAME_HEIGHT, self.target_resolution[1])
            cap.set(cv2.CAP_PROP_FPS, self.recording_fps)
            cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc("M", "J", "P", "G"))
            cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
            cap.set(cv2.CAP_PROP_AUTO_EXPOSURE, 1)
            cap.set(cv2.CAP_PROP_AUTOFOCUS, 1)
            actual_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
            actual_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
            actual_fps = cap.get(cv2.CAP_PROP_FPS)
            logger.info(
                f"Camera {camera_num} configured: {actual_width}x{actual_height} @ {actual_fps:.1f}fps"
            )
            ret, frame = cap.read()
            if not ret or frame is None:
                logger.error(f"Camera {camera_num} failed initial frame capture test")
                return False
            return True
        except Exception as e:
            logger.error(f"Error configuring camera {camera_num}: {e}")
            return False
    def _update_camera_status(self):
        try:
            if self.cap1 and self.cap1.isOpened():
                self.camera1_status.is_active = True
                self.camera1_status.fps = self.cap1.get(cv2.CAP_PROP_FPS)
                width = int(self.cap1.get(cv2.CAP_PROP_FRAME_WIDTH))
                height = int(self.cap1.get(cv2.CAP_PROP_FRAME_HEIGHT))
                self.camera1_status.resolution = width, height
            if self.cap2 and self.cap2.isOpened():
                self.camera2_status.is_active = True
                self.camera2_status.fps = self.cap2.get(cv2.CAP_PROP_FPS)
                width = int(self.cap2.get(cv2.CAP_PROP_FRAME_WIDTH))
                height = int(self.cap2.get(cv2.CAP_PROP_FRAME_HEIGHT))
                self.camera2_status.resolution = width, height
            status_dict = {
                "camera1": {
                    "index": self.camera1_status.camera_index,
                    "active": self.camera1_status.is_active,
                    "fps": self.camera1_status.fps,
                    "resolution": self.camera1_status.resolution,
                    "frames": self.camera1_status.frames_captured,
                },
                "camera2": {
                    "index": self.camera2_status.camera_index,
                    "active": self.camera2_status.is_active,
                    "fps": self.camera2_status.fps,
                    "resolution": self.camera2_status.resolution,
                    "frames": self.camera2_status.frames_captured,
                },
            }
            self.camera_status_changed.emit(status_dict)
        except Exception as e:
            logger.error(f"Error updating camera status: {e}")
    def start_preview(self):
        if not self.cap1 or not self.cap2:
            if not self.initialize_cameras():
                return
        self.is_previewing = True
        self.running = True
        self.master_start_time = time.time()
        self.start()
        logger.info("Dual camera preview started")
    def stop_preview(self):
        self.is_previewing = False
        self.running = False
        if self.isRunning():
            self.quit()
            self.wait()
        logger.info("Dual camera preview stopped")
    def start_recording(self, session_id: str) -> bool:
        if self.is_recording:
            self.error_occurred.emit("Recording already in progress")
            return False
        if not self.cap1 or not self.cap2:
            if not self.initialize_cameras():
                return False
        try:
            os.makedirs(self.output_directory, exist_ok=True)
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename1 = f"camera1_{session_id}_{timestamp}.mp4"
            filename2 = f"camera2_{session_id}_{timestamp}.mp4"
            self.recording_filepath1 = os.path.join(self.output_directory, filename1)
            self.recording_filepath2 = os.path.join(self.output_directory, filename2)
            resolution = self.camera1_status.resolution
            self.writer1 = cv2.VideoWriter(
                self.recording_filepath1,
                self.recording_codec,
                self.recording_fps,
                resolution,
            )
            self.writer2 = cv2.VideoWriter(
                self.recording_filepath2,
                self.recording_codec,
                self.recording_fps,
                resolution,
            )
            if not (self.writer1.isOpened() and self.writer2.isOpened()):
                self.error_occurred.emit("Could not initialize video writers")
                return False
            self.is_recording = True
            self.current_session_id = session_id
            self.recording_start_time = time.time()
            self.frame_counter = 0
            if not self.is_previewing:
                self.start_preview()
            self.recording_started.emit(
                self.recording_filepath1, self.recording_filepath2
            )
            master_timestamp = self.recording_start_time
            self.timestamp_sync_update.emit(master_timestamp)
            if self.sync_callback:
                self.sync_callback(master_timestamp)
            logger.info(f"Dual camera recording started: {filename1}, {filename2}")
            logger.info(f"Master timestamp: {master_timestamp}")
            return True
        except Exception as e:
            error_msg = f"Error starting dual camera recording: {str(e)}"
            self.error_occurred.emit(error_msg)
            logger.error(error_msg)
            return False
    def stop_recording(self) -> Tuple[Optional[str], Optional[str]]:
        if not self.is_recording:
            return None, None
        try:
            self.is_recording = False
            duration = (
                time.time() - self.recording_start_time
                if self.recording_start_time
                else 0
            )
            if self.writer1:
                self.writer1.release()
                self.writer1 = None
            if self.writer2:
                self.writer2.release()
                self.writer2 = None
            filepath1 = self.recording_filepath1
            filepath2 = self.recording_filepath2
            self.recording_filepath1 = None
            self.recording_filepath2 = None
            self.current_session_id = None
            self.recording_start_time = None
            self.recording_stopped.emit(filepath1, filepath2, duration)
            logger.info(f"Dual camera recording stopped (duration: {duration:.1f}s)")
            logger.info(f"Files: {filepath1}, {filepath2}")
            return filepath1, filepath2
        except Exception as e:
            error_msg = f"Error stopping dual camera recording: {str(e)}"
            self.error_occurred.emit(error_msg)
            logger.error(error_msg)
            return None, None
    def run(self):
        last_preview_time = 0
        last_recording_time = 0
        logger.info("Starting dual camera capture thread")
        while self.running:
            try:
                current_time = time.time()
                process_start_time = current_time
                ret1, frame1 = self.cap1.read()
                capture_timestamp1 = time.time()
                ret2, frame2 = self.cap2.read()
                capture_timestamp2 = time.time()
                if not (ret1 and ret2):
                    self.error_occurred.emit(
                        "Failed to capture frames from one or both cameras"
                    )
                    break
                frame_data = self._process_advanced_synchronization(
                    frame1, frame2, capture_timestamp1, capture_timestamp2
                )
                sync_quality = frame_data.sync_quality
                if sync_quality < 0.8:
                    self.performance_stats["sync_violations"] += 1
                self.last_sync_quality = sync_quality
                self.sync_status_changed.emit(sync_quality)
                with self.frame_lock:
                    self.last_frames["camera1"] = frame_data.camera1_frame.copy()
                    self.last_frames["camera2"] = frame_data.camera2_frame.copy()
                if (
                    self.is_recording
                    and self.writer1
                    and self.writer2
                    and current_time - last_recording_time >= self.recording_interval
                ):
                    self.writer1.write(frame_data.camera1_frame)
                    self.writer2.write(frame_data.camera2_frame)
                    last_recording_time = current_time
                    self.camera1_status.frames_captured += 1
                    self.camera2_status.frames_captured += 1
                    self.frame_counter += 1
                if (
                    self.is_previewing
                    and current_time - last_preview_time >= self.frame_interval
                ):
                    pixmap1 = self._frame_to_pixmap(frame_data.camera1_frame)
                    pixmap2 = self._frame_to_pixmap(frame_data.camera2_frame)
                    if pixmap1 and pixmap2:
                        self.dual_frame_ready.emit(pixmap1, pixmap2)
                    last_preview_time = current_time
                processing_time_ms = (time.time() - process_start_time) * 1000
                self.performance_stats["frames_processed"] += 1
                self.performance_stats["average_processing_time_ms"] = (
                    self.performance_stats["average_processing_time_ms"]
                    * (self.performance_stats["frames_processed"] - 1)
                    + processing_time_ms
                ) / self.performance_stats["frames_processed"]
                time.sleep(0.001)
            except Exception as e:
                error_msg = f"Error in dual camera capture loop: {str(e)}"
                self.error_occurred.emit(error_msg)
                logger.error(error_msg)
                break
        logger.info("Dual camera capture thread ended")
    def _frame_to_pixmap(
        self, frame: np.ndarray, max_width: int = 640, max_height: int = 360
    ) -> Optional[QPixmap]:
        try:
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            height, width, channels = rgb_frame.shape
            scale_w = max_width / width
            scale_h = max_height / height
            scale = min(scale_w, scale_h, 1.0)
            if scale < 1.0:
                new_width = int(width * scale)
                new_height = int(height * scale)
                rgb_frame = cv2.resize(rgb_frame, (new_width, new_height))
                height, width = new_height, new_width
            bytes_per_line = channels * width
            q_image = QImage(
                rgb_frame.data, width, height, bytes_per_line, QImage.Format_RGB888
            )
            return QPixmap.fromImage(q_image)
        except Exception as e:
            logger.error(f"Error converting frame to pixmap: {str(e)}")
            return None
    def get_master_timestamp(self) -> float:
        return time.time()
    def get_sync_quality(self) -> float:
        return self.last_sync_quality
    def get_performance_stats(self) -> Dict:
        return self.performance_stats.copy()
    def get_latest_frame(self) -> Optional[DualFrameData]:
        with self.frame_lock:
            if self.frame_sync_buffer:
                return self.frame_sync_buffer[-1]
            return None
    def get_camera_fps(self, camera_number: int) -> float:
        if camera_number == 1:
            return self.camera1_status.fps
        elif camera_number == 2:
            return self.camera2_status.fps
        else:
            logger.warning(f"Invalid camera number: {camera_number}")
            return 0.0
    def start_capture(self) -> bool:
        try:
            if not self._initialize_cameras():
                return False
            return self.start_preview()
        except Exception as e:
            logger.error(f"Error starting capture: {e}", exc_info=True)
            return False
    def stop_capture(self):
        try:
            if self.is_recording:
                self.stop_recording()
            self.stop_preview()
            self._release_cameras()
        except Exception as e:
            logger.error(f"Error stopping capture: {e}", exc_info=True)
    def _initialize_cameras(self) -> bool:
        return self.initialize_cameras()
    def _release_cameras(self):
        try:
            if hasattr(self, "cap1") and self.cap1:
                self.cap1.release()
                self.cap1 = None
            if hasattr(self, "cap2") and self.cap2:
                self.cap2.release()
                self.cap2 = None
            logger.debug("Camera resources released")
        except Exception as e:
            logger.error(f"Error releasing cameras: {e}", exc_info=True)
    def cleanup(self):
        try:
            self.running = False
            self.is_previewing = False
            if self.is_recording:
                self.stop_recording()
            if self.isRunning():
                self.quit()
                self.wait(1000)
            if self.cap1:
                self.cap1.release()
                self.cap1 = None
            if self.cap2:
                self.cap2.release()
                self.cap2 = None
            if self.writer1:
                self.writer1.release()
                self.writer1 = None
            if self.writer2:
                self.writer2.release()
                self.writer2 = None
            logger.info("DualWebcamCapture cleanup completed")
        except Exception as e:
            logger.error(f"Error during cleanup: {e}")
    def enable_physiological_monitoring(self, enabled: bool = True):
        self.enable_physio_monitoring = enabled
        if enabled:
            logger.info("Physiological monitoring enabled")
        else:
            logger.info("Physiological monitoring disabled")
            self.current_roi = None
            self.latest_physio_signal = None
    def get_synchronization_diagnostics(self) -> Dict:
        if hasattr(self, "synchronizer"):
            return self.synchronizer.get_diagnostics()
        else:
            return {"error": "Synchronizer not initialized"}
    def get_roi_metrics(self) -> Dict:
        if hasattr(self, "roi_detector"):
            metrics = self.roi_detector.get_metrics()
            return {
                "area_pixels": metrics.area_pixels,
                "center_coordinates": metrics.center_coordinates,
                "stability_score": metrics.stability_score,
                "illumination_uniformity": metrics.illumination_uniformity,
                "motion_magnitude": metrics.motion_magnitude,
                "skin_probability": metrics.skin_probability,
                "signal_to_noise_ratio": metrics.signal_to_noise_ratio,
                "is_valid": metrics.is_valid,
                "confidence_score": metrics.confidence_score,
                "frame_count": metrics.frame_count,
            }
        else:
            return {"error": "ROI detector not initialized"}
    def get_latest_physiological_signal(self) -> Optional[Dict]:
        if self.latest_physio_signal is not None:
            signal = self.latest_physio_signal
            hr_estimate = signal.get_heart_rate_estimate()
            return {
                "timestamp": signal.timestamp,
                "signal_type": signal.signal_type,
                "extraction_method": signal.extraction_method,
                "signal_length": len(signal.signal_data),
                "sampling_rate": signal.sampling_rate,
                "snr_db": signal.snr_db,
                "signal_quality_index": signal.signal_quality_index,
                "motion_artifacts": signal.motion_artifacts,
                "heart_rate_estimate_bpm": hr_estimate,
                "preprocessing_steps": signal.preprocessing_steps,
                "spectral_features": signal.spectral_features,
            }
        return None
    def set_synchronization_strategy(self, strategy: str):
        if hasattr(self, "synchronizer"):
            try:
                sync_strategy = SynchronizationStrategy(strategy)
                self.synchronizer.set_strategy(sync_strategy)
                logger.info(f"Synchronisation strategy changed to: {strategy}")
            except ValueError:
                logger.error(f"Invalid synchronisation strategy: {strategy}")
                logger.info(
                    f"Valid strategies: {[s.value for s in SynchronizationStrategy]}"
                )
        else:
            logger.error("Synchronizer not initialized")
    def reset_synchronization_metrics(self):
        if hasattr(self, "synchronizer"):
            self.synchronizer.reset_metrics()
        self.performance_stats = {
            "frames_processed": 0,
            "sync_violations": 0,
            "dropped_frames": 0,
            "average_processing_time_ms": 0.0,
        }
        logger.info("Synchronisation metrics reset")
    def export_synchronization_data(self, filepath: str) -> bool:
        try:
            import json
            from datetime import datetime
            diagnostics = self.get_synchronization_diagnostics()
            roi_metrics = self.get_roi_metrics()
            physio_signal = self.get_latest_physiological_signal()
            export_data = {
                "export_timestamp": datetime.now().isoformat(),
                "camera_configuration": {
                    "camera1_index": self.camera1_index,
                    "camera2_index": self.camera2_index,
                    "target_resolution": self.target_resolution,
                    "recording_fps": self.recording_fps,
                    "preview_fps": self.preview_fps,
                },
                "synchronization_diagnostics": diagnostics,
                "roi_metrics": roi_metrics,
                "physiological_signal": physio_signal,
                "performance_stats": self.performance_stats,
                "camera_status": {
                    "camera1": {
                        "index": self.camera1_status.camera_index,
                        "active": self.camera1_status.is_active,
                        "fps": self.camera1_status.fps,
                        "resolution": self.camera1_status.resolution,
                        "frames_captured": self.camera1_status.frames_captured,
                    },
                    "camera2": {
                        "index": self.camera2_status.camera_index,
                        "active": self.camera2_status.is_active,
                        "fps": self.camera2_status.fps,
                        "resolution": self.camera2_status.resolution,
                        "frames_captured": self.camera2_status.frames_captured,
                    },
                },
            }
            with open(filepath, "w") as f:
                json.dump(export_data, f, indent=2, default=str)
            logger.info(f"Synchronisation data exported to: {filepath}")
            return True
        except Exception as e:
            logger.error(f"Failed to export synchronisation data: {e}")
            return False
    def _process_advanced_synchronization(
        self,
        frame1: np.ndarray,
        frame2: np.ndarray,
        timestamp1: float,
        timestamp2: float,
    ) -> DualFrameData:
        try:
            sync_frame = self.synchronizer.synchronize_frames(
                frame1, frame2, timestamp1, timestamp2
            )
            frame_data = DualFrameData(
                timestamp=sync_frame.timestamp,
                frame_id=sync_frame.frame_id,
                camera1_frame=sync_frame.camera1_frame,
                camera2_frame=sync_frame.camera2_frame,
                camera1_timestamp=timestamp1,
                camera2_timestamp=timestamp2,
                sync_quality=sync_frame.sync_quality,
            )
            if self.enable_physio_monitoring:
                self._process_physiological_monitoring(frame1)
            return frame_data
        except Exception as e:
            logger.error(f"Advanced synchronisation processing failed: {e}")
            sync_diff_ms = abs(timestamp1 - timestamp2) * 1000
            sync_quality = max(0.0, 1.0 - sync_diff_ms / self.sync_threshold_ms)
            return DualFrameData(
                timestamp=min(timestamp1, timestamp2),
                frame_id=self.frame_counter,
                camera1_frame=frame1.copy(),
                camera2_frame=frame2.copy(),
                camera1_timestamp=timestamp1,
                camera2_timestamp=timestamp2,
                sync_quality=sync_quality,
            )
    def _process_physiological_monitoring(self, frame: np.ndarray):
        try:
            roi = self.roi_detector.detect_roi(frame)
            if roi is not None:
                self.current_roi = roi
                x, y, w, h = roi
                roi_region = frame[y : y + h, x : x + w]
                if roi_region.size > 0:
                    physio_signal = self.signal_extractor.extract_signal(roi_region)
                    if physio_signal is not None:
                        self.latest_physio_signal = physio_signal
                        physio_signal.roi_metrics = self.roi_detector.get_metrics()
        except Exception as e:
            logger.debug(f"Physiological monitoring processing failed: {e}")
    def __del__(self):
        try:
            if hasattr(self, "cap1") and self.cap1:
                self.cap1.release()
            if hasattr(self, "cap2") and self.cap2:
                self.cap2.release()
            if hasattr(self, "writer1") and self.writer1:
                self.writer1.release()
            if hasattr(self, "writer2") and self.writer2:
                self.writer2.release()
        except Exception:
            pass
def test_dual_webcam_access(camera_indices: List[int] = None):
    if camera_indices is None:
        camera_indices = [0, 1]
    if len(camera_indices) != 2:
        logger.error(f"ERROR: Expected 2 camera indices, got {len(camera_indices)}")
        return False
    camera1_index, camera2_index = camera_indices
    logger.info(
        f"Testing dual webcam access with cameras {camera1_index} and {camera2_index}..."
    )
    cap1 = cv2.VideoCapture(camera1_index)
    if not cap1.isOpened():
        logger.error(f"ERROR: Could not open camera 1 (index {camera1_index})")
        return False
    ret1, frame1 = cap1.read()
    if ret1:
        height1, width1 = frame1.shape[:2]
        logger.info(
            f"SUCCESS: Camera {camera1_index} accessible, frame size: {width1}x{height1}"
        )
    else:
        logger.error(f"ERROR: Could not capture frame from camera {camera1_index}")
        cap1.release()
        return False
    cap2 = cv2.VideoCapture(camera2_index)
    if not cap2.isOpened():
        logger.error(f"ERROR: Could not open camera 2 (index {camera2_index})")
        cap1.release()
        return False
    ret2, frame2 = cap2.read()
    if ret2:
        height2, width2 = frame2.shape[:2]
        logger.info(
            f"SUCCESS: Camera {camera2_index} accessible, frame size: {width2}x{height2}"
        )
    else:
        logger.error(f"ERROR: Could not capture frame from camera {camera2_index}")
        cap1.release()
        cap2.release()
        return False
    cap1.release()
    cap2.release()
    logger.info(
        f"SUCCESS: Both cameras ({camera1_index}, {camera2_index}) accessible for dual recording"
    )
    return True
if __name__ == "__main__":
    test_dual_webcam_access()