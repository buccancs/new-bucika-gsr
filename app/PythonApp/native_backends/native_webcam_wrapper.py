"""
Native Webcam Processor Module

Provides high-performance C++ implementation for webcam capture and processing
with fallback to pure Python implementation when native module is unavailable.
"""

import logging
import time
from typing import List, Dict, Any, Optional, Tuple
import numpy as np
import cv2

logger = logging.getLogger(__name__)

try:
    # Try to import the native C++ module
    from native_webcam import WebcamProcessor as NativeWebcamProcessor
    from native_webcam import FrameData as NativeFrameData
    from native_webcam import ProcessingConfig as NativeProcessingConfig
    from native_webcam import PerformanceMetrics as NativePerformanceMetrics
    NATIVE_AVAILABLE = True
    logger.info("Native Webcam processor loaded successfully")
except ImportError as e:
    logger.warning(f"Native Webcam processor not available: {e}")
    NATIVE_AVAILABLE = False


class FrameData:
    """Unified frame data structure compatible with both native and Python implementations"""
    
    def __init__(self, frame: Optional[np.ndarray] = None, timestamp: float = 0.0, 
                 frame_number: int = 0, is_valid: bool = False):
        self.frame = frame if frame is not None else np.array([])
        self.timestamp = timestamp
        self.frame_number = frame_number
        self.is_valid = is_valid
    
    def __repr__(self):
        return f"<FrameData #{self.frame_number} timestamp={self.timestamp} valid={self.is_valid}>"
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization (without frame data)"""
        return {
            'timestamp': self.timestamp,
            'frame_number': self.frame_number,
            'is_valid': self.is_valid,
            'frame_shape': self.frame.shape if self.frame is not None and self.frame.size > 0 else None
        }


class ProcessingConfig:
    """Configuration for webcam processing"""
    
    def __init__(self, width: int = 1920, height: int = 1080, fps: float = 30.0,
                 enable_preprocessing: bool = True, enable_motion_detection: bool = False,
                 motion_threshold: float = 30.0):
        self.width = width
        self.height = height
        self.fps = fps
        self.enable_preprocessing = enable_preprocessing
        self.enable_motion_detection = enable_motion_detection
        self.motion_threshold = motion_threshold


class PerformanceMetrics:
    """Performance metrics for webcam processing"""
    
    def __init__(self, average_frame_time_ms: float = 0.0, processing_fps: float = 0.0,
                 frames_processed: int = 0, frames_dropped: int = 0, cpu_usage_percent: float = 0.0):
        self.average_frame_time_ms = average_frame_time_ms
        self.processing_fps = processing_fps
        self.frames_processed = frames_processed
        self.frames_dropped = frames_dropped
        self.cpu_usage_percent = cpu_usage_percent
    
    def __repr__(self):
        return f"<PerformanceMetrics fps={self.processing_fps} processed={self.frames_processed} dropped={self.frames_dropped}>"


class PythonWebcamProcessor:
    """Pure Python implementation of webcam processor"""
    
    def __init__(self):
        self.config = ProcessingConfig()
        self.camera = None
        self.previous_frame = None
        self.motion_diff = None
        self.master_clock_offset_ms = 0.0
        self.total_processing_time_ms = 0.0
        self.frames_processed = 0
        self.frames_dropped = 0
        
    def initialize_camera(self, camera_id: int = 0) -> bool:
        self.release_camera()
        
        self.camera = cv2.VideoCapture(camera_id)
        if not self.camera.isOpened():
            return False
            
        # Set camera properties
        self.camera.set(cv2.CAP_PROP_FRAME_WIDTH, self.config.width)
        self.camera.set(cv2.CAP_PROP_FRAME_HEIGHT, self.config.height)
        self.camera.set(cv2.CAP_PROP_FPS, self.config.fps)
        
        # Verify settings
        actual_width = int(self.camera.get(cv2.CAP_PROP_FRAME_WIDTH))
        actual_height = int(self.camera.get(cv2.CAP_PROP_FRAME_HEIGHT))
        actual_fps = self.camera.get(cv2.CAP_PROP_FPS)
        
        logger.info(f"Camera initialized: {actual_width}x{actual_height} @ {actual_fps} fps")
        return True
    
    def initialize_camera_by_path(self, camera_path: str) -> bool:
        self.release_camera()
        
        self.camera = cv2.VideoCapture(camera_path)
        if not self.camera.isOpened():
            return False
            
        self.camera.set(cv2.CAP_PROP_FRAME_WIDTH, self.config.width)
        self.camera.set(cv2.CAP_PROP_FRAME_HEIGHT, self.config.height)
        self.camera.set(cv2.CAP_PROP_FPS, self.config.fps)
        
        return True
    
    def release_camera(self):
        if self.camera is not None and self.camera.isOpened():
            self.camera.release()
        self.camera = None
    
    def is_camera_active(self) -> bool:
        return self.camera is not None and self.camera.isOpened()
    
    def configure(self, config: ProcessingConfig):
        self.config = config
        
        if self.camera is not None and self.camera.isOpened():
            self.camera.set(cv2.CAP_PROP_FRAME_WIDTH, config.width)
            self.camera.set(cv2.CAP_PROP_FRAME_HEIGHT, config.height)
            self.camera.set(cv2.CAP_PROP_FPS, config.fps)
    
    def get_config(self) -> ProcessingConfig:
        return self.config
    
    def capture_frame(self) -> FrameData:
        start_time = time.perf_counter()
        
        frame_data = FrameData()
        frame_data.timestamp = self.get_synchronized_timestamp()
        frame_data.frame_number = self.frames_processed
        frame_data.is_valid = False
        
        if not self.is_camera_active():
            self.frames_dropped += 1
            self._update_performance_metrics(start_time)
            return frame_data
        
        ret, raw_frame = self.camera.read()
        
        if not ret or raw_frame is None:
            self.frames_dropped += 1
            self._update_performance_metrics(start_time)
            return frame_data
        
        # Apply preprocessing if enabled
        if self.config.enable_preprocessing:
            frame_data.frame = self.preprocess_frame(raw_frame)
        else:
            frame_data.frame = raw_frame.copy()
        
        frame_data.is_valid = True
        
        # Update motion detection
        if self.config.enable_motion_detection and self.previous_frame is not None:
            self.detect_motion(frame_data.frame, self.previous_frame)
        
        # Store for next motion detection
        if self.config.enable_motion_detection:
            self.previous_frame = cv2.cvtColor(frame_data.frame, cv2.COLOR_BGR2GRAY)
        
        self._update_performance_metrics(start_time)
        return frame_data
    
    def capture_batch(self, count: int) -> List[FrameData]:
        frames = []
        for i in range(count):
            frame = self.capture_frame()
            frames.append(frame)
            if not frame.is_valid:
                break
        return frames
    
    def preprocess_frame(self, input_frame: np.ndarray) -> np.ndarray:
        # Validate input frame
        if input_frame is None or input_frame.size == 0:
            raise ValueError("Input frame cannot be None or empty")
        
        processed = input_frame.copy()
        
        # Apply basic image enhancements
        if len(processed.shape) == 3 and processed.shape[2] == 3:
            # Convert to LAB color space for better contrast enhancement
            lab = cv2.cvtColor(processed, cv2.COLOR_BGR2LAB)
            l, a, b = cv2.split(lab)
            
            # Apply CLAHE to L channel
            clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
            l = clahe.apply(l)
            
            # Merge channels and convert back
            lab = cv2.merge([l, a, b])
            processed = cv2.cvtColor(lab, cv2.COLOR_LAB2BGR)
        
        # Light Gaussian blur for noise reduction
        processed = cv2.GaussianBlur(processed, (3, 3), 0.5)
        
        return processed
    
    def detect_motion(self, current_frame: np.ndarray, previous_frame: np.ndarray) -> bool:
        if current_frame is None or previous_frame is None:
            return False
        
        # Convert to grayscale if needed
        if len(current_frame.shape) == 3:
            current_gray = cv2.cvtColor(current_frame, cv2.COLOR_BGR2GRAY)
        else:
            current_gray = current_frame
        
        if len(previous_frame.shape) == 3:
            prev_gray = cv2.cvtColor(previous_frame, cv2.COLOR_BGR2GRAY)
        else:
            prev_gray = previous_frame
        
        # Compute absolute difference
        self.motion_diff = cv2.absdiff(current_gray, prev_gray)
        
        # Apply threshold
        _, self.motion_diff = cv2.threshold(
            self.motion_diff, self.config.motion_threshold, 255, cv2.THRESH_BINARY
        )
        
        # Calculate percentage of changed pixels
        non_zero = cv2.countNonZero(self.motion_diff)
        motion_percentage = (non_zero * 100.0) / (self.motion_diff.shape[0] * self.motion_diff.shape[1])
        
        return motion_percentage > 1.0
    
    def apply_timestamp_overlay(self, frame: np.ndarray, timestamp: float) -> np.ndarray:
        output = frame.copy()
        
        # Format timestamp as string
        timestamp_str = f"{int(timestamp)}"
        
        # Add timestamp overlay
        text_position = (10, 30)
        text_color = (0, 255, 0)  # Green in BGR
        font_face = cv2.FONT_HERSHEY_SIMPLEX
        font_scale = 0.7
        thickness = 2
        
        cv2.putText(output, timestamp_str, text_position, font_face, font_scale, text_color, thickness)
        
        return output
    
    def set_master_clock_offset(self, offset_ms: float):
        self.master_clock_offset_ms = offset_ms
    
    def get_synchronized_timestamp(self) -> float:
        return time.time() * 1000.0 + self.master_clock_offset_ms
    
    def get_performance_metrics(self) -> PerformanceMetrics:
        metrics = PerformanceMetrics()
        
        if self.frames_processed > 0:
            metrics.average_frame_time_ms = self.total_processing_time_ms / self.frames_processed
            metrics.processing_fps = 1000.0 / metrics.average_frame_time_ms
        
        metrics.frames_processed = self.frames_processed
        metrics.frames_dropped = self.frames_dropped
        metrics.cpu_usage_percent = 0.0  # Would need system-specific implementation
        
        return metrics
    
    def reset_performance_counters(self):
        self.total_processing_time_ms = 0.0
        self.frames_processed = 0
        self.frames_dropped = 0
    
    def _update_performance_metrics(self, start_time: float):
        processing_time = (time.perf_counter() - start_time) * 1000.0
        self.total_processing_time_ms += processing_time
        self.frames_processed += 1


class WebcamProcessor:
    """Unified Webcam processor with automatic native/Python fallback"""
    
    def __init__(self, force_python: bool = False):
        self.use_native = NATIVE_AVAILABLE and not force_python
        
        if self.use_native:
            self.processor = NativeWebcamProcessor()
            logger.info("Using native C++ Webcam processor")
        else:
            self.processor = PythonWebcamProcessor()
            logger.info("Using Python Webcam processor")
    
    def initialize_camera(self, camera_id: int = 0) -> bool:
        if hasattr(self.processor, 'initialize_camera'):
            return self.processor.initialize_camera(camera_id)
        else:
            return self.processor.initialize_camera_by_path(str(camera_id))
    
    def initialize_camera_by_path(self, camera_path: str) -> bool:
        if hasattr(self.processor, 'initialize_camera'):
            # Native version uses overloaded method
            return self.processor.initialize_camera(camera_path)
        else:
            return self.processor.initialize_camera_by_path(camera_path)
    
    def release_camera(self):
        self.processor.release_camera()
    
    def is_camera_active(self) -> bool:
        return self.processor.is_camera_active()
    
    def configure(self, config: ProcessingConfig):
        if self.use_native:
            native_config = NativeProcessingConfig()
            native_config.width = config.width
            native_config.height = config.height
            native_config.fps = config.fps
            native_config.enable_preprocessing = config.enable_preprocessing
            native_config.enable_motion_detection = config.enable_motion_detection
            native_config.motion_threshold = config.motion_threshold
            self.processor.configure(native_config)
        else:
            self.processor.configure(config)
    
    def get_config(self) -> ProcessingConfig:
        if self.use_native:
            native_config = self.processor.get_config()
            return ProcessingConfig(
                width=native_config.width,
                height=native_config.height,
                fps=native_config.fps,
                enable_preprocessing=native_config.enable_preprocessing,
                enable_motion_detection=native_config.enable_motion_detection,
                motion_threshold=native_config.motion_threshold
            )
        else:
            return self.processor.get_config()
    
    def capture_frame(self) -> FrameData:
        if self.use_native:
            native_frame = self.processor.capture_frame()
            return FrameData(
                frame=native_frame.frame,
                timestamp=native_frame.timestamp,
                frame_number=native_frame.frame_number,
                is_valid=native_frame.is_valid
            )
        else:
            return self.processor.capture_frame()
    
    def capture_batch(self, count: int) -> List[FrameData]:
        if self.use_native:
            native_frames = self.processor.capture_batch(count)
            return [FrameData(
                frame=f.frame, timestamp=f.timestamp,
                frame_number=f.frame_number, is_valid=f.is_valid
            ) for f in native_frames]
        else:
            return self.processor.capture_batch(count)
    
    def preprocess_frame(self, input_frame: np.ndarray) -> np.ndarray:
        return self.processor.preprocess_frame(input_frame)
    
    def detect_motion(self, current_frame: np.ndarray, previous_frame: np.ndarray) -> bool:
        return self.processor.detect_motion(current_frame, previous_frame)
    
    def apply_timestamp_overlay(self, frame: np.ndarray, timestamp: float) -> np.ndarray:
        return self.processor.apply_timestamp_overlay(frame, timestamp)
    
    def set_master_clock_offset(self, offset_ms: float):
        self.processor.set_master_clock_offset(offset_ms)
    
    def get_synchronized_timestamp(self) -> float:
        return self.processor.get_synchronized_timestamp()
    
    def get_performance_metrics(self) -> PerformanceMetrics:
        if self.use_native:
            native_metrics = self.processor.get_performance_metrics()
            return PerformanceMetrics(
                average_frame_time_ms=native_metrics.average_frame_time_ms,
                processing_fps=native_metrics.processing_fps,
                frames_processed=native_metrics.frames_processed,
                frames_dropped=native_metrics.frames_dropped,
                cpu_usage_percent=native_metrics.cpu_usage_percent
            )
        else:
            return self.processor.get_performance_metrics()
    
    def reset_performance_counters(self):
        self.processor.reset_performance_counters()
    
    @property
    def backend_type(self) -> str:
        return "native" if self.use_native else "python"


# Export main classes
__all__ = ['WebcamProcessor', 'FrameData', 'ProcessingConfig', 'PerformanceMetrics', 'NATIVE_AVAILABLE']