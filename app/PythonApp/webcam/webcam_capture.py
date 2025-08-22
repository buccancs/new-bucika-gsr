import os
import threading
import time
from datetime import datetime
from typing import Optional
import cv2
import numpy as np
from PyQt5.QtCore import QThread, pyqtSignal
from PyQt5.QtGui import QImage, QPixmap
from ..utils.logging_config import get_logger
logger = get_logger(__name__)
class WebcamCapture(QThread):
    frame_ready = pyqtSignal(QPixmap)
    recording_started = pyqtSignal(str)
    recording_stopped = pyqtSignal(str, float)
    error_occurred = pyqtSignal(str)
    status_changed = pyqtSignal(str)
    def __init__(self, camera_index: int = 0, preview_fps: int = 30):
        super().__init__()
        self.camera_index = camera_index
        self.preview_fps = preview_fps
        self.frame_interval = 1.0 / preview_fps
        self.cap: Optional[cv2.VideoCapture] = None
        self.video_writer: Optional[cv2.VideoWriter] = None
        self.is_recording = False
        self.is_previewing = False
        self.running = False
        self.recording_fps = 30
        self.recording_resolution = 1280, 720
        self.recording_codec = cv2.VideoWriter_fourcc(*"mp4v")
        self.current_session_id: Optional[str] = None
        self.recording_start_time: Optional[float] = None
        self.output_directory = "recordings"
        self.last_frame: Optional[np.ndarray] = None
        self.frame_lock = threading.Lock()
        print(
            f"[DEBUG_LOG] WebcamCapture initialized with camera {camera_index}, preview FPS: {preview_fps}"
        )
    def initialize_camera(self) -> bool:
        try:
            self.cap = cv2.VideoCapture(self.camera_index)
            if not self.cap.isOpened():
                self.error_occurred.emit(f"Could not open camera {self.camera_index}")
                return False
            self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, self.recording_resolution[0])
            self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, self.recording_resolution[1])
            self.cap.set(cv2.CAP_PROP_FPS, self.recording_fps)
            actual_width = int(self.cap.get(cv2.CAP_PROP_FRAME_WIDTH))
            actual_height = int(self.cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
            actual_fps = self.cap.get(cv2.CAP_PROP_FPS)
            self.recording_resolution = actual_width, actual_height
            self.status_changed.emit(
                f"Camera initialized: {actual_width}x{actual_height} @ {actual_fps:.1f} FPS"
            )
            print(
                f"[DEBUG_LOG] Camera initialized: {actual_width}x{actual_height} @ {actual_fps:.1f} FPS"
            )
            return True
        except Exception as e:
            error_msg = f"Error initializing camera: {str(e)}"
            self.error_occurred.emit(error_msg)
            print(f"[DEBUG_LOG] {error_msg}")
            return False
    def start_preview(self):
        if not self.cap or not self.cap.isOpened():
            if not self.initialize_camera():
                return
        self.is_previewing = True
        self.running = True
        self.start()
        self.status_changed.emit("Preview started")
        print("[DEBUG_LOG] Webcam preview started")
    def stop_preview(self):
        self.is_previewing = False
        self.running = False
        if self.isRunning():
            self.quit()
            self.wait()
        self.status_changed.emit("Preview stopped")
        print("[DEBUG_LOG] Webcam preview stopped")
    def start_recording(self, session_id: str) -> bool:
        if self.is_recording:
            self.error_occurred.emit("Recording already in progress")
            return False
        if not self.cap or not self.cap.isOpened():
            if not self.initialize_camera():
                return False
        try:
            os.makedirs(self.output_directory, exist_ok=True)
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"webcam_{session_id}_{timestamp}.mp4"
            self.recording_filepath = os.path.join(self.output_directory, filename)
            self.video_writer = cv2.VideoWriter(
                self.recording_filepath,
                self.recording_codec,
                self.recording_fps,
                self.recording_resolution,
            )
            if not self.video_writer.isOpened():
                self.error_occurred.emit("Could not initialize video writer")
                return False
            self.is_recording = True
            self.current_session_id = session_id
            self.recording_start_time = time.time()
            if not self.is_previewing:
                self.start_preview()
            self.recording_started.emit(self.recording_filepath)
            self.status_changed.emit(f"Recording started: {filename}")
            print(f"[DEBUG_LOG] Recording started: {self.recording_filepath}")
            return True
        except Exception as e:
            error_msg = f"Error starting recording: {str(e)}"
            self.error_occurred.emit(error_msg)
            print(f"[DEBUG_LOG] {error_msg}")
            return False
    def stop_recording(self) -> Optional[str]:
        if not self.is_recording:
            return None
        try:
            self.is_recording = False
            duration = (
                time.time() - self.recording_start_time
                if self.recording_start_time
                else 0
            )
            if self.video_writer:
                self.video_writer.release()
                self.video_writer = None
            filepath = self.recording_filepath
            self.recording_filepath = None
            self.current_session_id = None
            self.recording_start_time = None
            self.recording_stopped.emit(filepath, duration)
            self.status_changed.emit(f"Recording stopped (duration: {duration:.1f}s)")
            print(
                f"[DEBUG_LOG] Recording stopped: {filepath} (duration: {duration:.1f}s)"
            )
            return filepath
        except Exception as e:
            error_msg = f"Error stopping recording: {str(e)}"
            self.error_occurred.emit(error_msg)
            print(f"[DEBUG_LOG] {error_msg}")
            return None
    def run(self):
        last_frame_time = 0
        while self.running:
            try:
                current_time = time.time()
                ret, frame = self.cap.read()
                if not ret:
                    self.error_occurred.emit("Failed to capture frame from webcam")
                    break
                with self.frame_lock:
                    self.last_frame = frame.copy()
                if self.is_recording and self.video_writer:
                    self.video_writer.write(frame)
                if (
                    self.is_previewing
                    and current_time - last_frame_time >= self.frame_interval
                ):
                    preview_pixmap = self.frame_to_pixmap(frame)
                    if preview_pixmap:
                        self.frame_ready.emit(preview_pixmap)
                    last_frame_time = current_time
                time.sleep(0.01)
            except Exception as e:
                error_msg = f"Error in webcam capture loop: {str(e)}"
                self.error_occurred.emit(error_msg)
                print(f"[DEBUG_LOG] {error_msg}")
                break
        print("[DEBUG_LOG] Webcam capture thread ended")
    def frame_to_pixmap(
        self, frame: np.ndarray, max_width: int = 640, max_height: int = 480
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
            print(f"[DEBUG_LOG] Error converting frame to pixmap: {str(e)}")
            return None
    def get_current_frame(self) -> Optional[np.ndarray]:
        with self.frame_lock:
            return self.last_frame.copy() if self.last_frame is not None else None
    def set_recording_parameters(
        self, fps: int = 30, resolution: tuple = (1280, 720), codec: str = "mp4v"
    ):
        self.recording_fps = fps
        self.recording_resolution = resolution
        self.recording_codec = cv2.VideoWriter_fourcc(*codec)
        print(
            f"[DEBUG_LOG] Recording parameters updated: {fps} FPS, {resolution}, codec: {codec}"
        )
    def set_output_directory(self, directory: str):
        self.output_directory = directory
        os.makedirs(directory, exist_ok=True)
        print(f"[DEBUG_LOG] Output directory set to: {directory}")
    def cleanup(self):
        try:
            self.running = False
            self.is_previewing = False
            if self.is_recording:
                self.stop_recording()
            if hasattr(self, "_finished") and not self._finished and self.isRunning():
                self.quit()
                self.wait(1000)
            if self.cap:
                self.cap.release()
                self.cap = None
            if self.video_writer:
                self.video_writer.release()
                self.video_writer = None
            print("[DEBUG_LOG] WebcamCapture cleanup completed")
        except Exception as e:
            print(f"[DEBUG_LOG] Error during cleanup: {e}")
    def __del__(self):
        try:
            if hasattr(self, "cap") and self.cap:
                self.cap.release()
            if hasattr(self, "video_writer") and self.video_writer:
                self.video_writer.release()
        except Exception:
            pass
def test_webcam_access():
    print("[DEBUG_LOG] Testing webcam access...")
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("[DEBUG_LOG] ERROR: Could not open webcam")
        return False
    ret, frame = cap.read()
    if ret:
        height, width = frame.shape[:2]
        print(f"[DEBUG_LOG] SUCCESS: Webcam accessible, frame size: {width}x{height}")
    else:
        print("[DEBUG_LOG] ERROR: Could not capture frame")
        cap.release()
        return False
    cap.release()
    return True
if __name__ == "__main__":
    test_webcam_access()