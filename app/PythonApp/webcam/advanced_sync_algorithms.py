import statistics
import threading
import time
from collections import deque
from dataclasses import dataclass, field
from enum import Enum
from typing import Callable, Dict, List, Optional, Tuple
import cv2
import numpy as np
from ..utils.logging_config import get_logger, performance_timer
logger = get_logger(__name__)
class SynchronizationStrategy(Enum):
    MASTER_SLAVE = "master_slave"
    CROSS_CORRELATION = "cross_corr"
    HARDWARE_SYNC = "hardware_sync"
    ADAPTIVE_HYBRID = "adaptive_hybrid"
@dataclass
class TimingMetrics:
    capture_interval_ms: float = 0.0
    sync_offset_ms: float = 0.0
    jitter_ms: float = 0.0
    drift_rate_ppm: float = 0.0
    correlation_coefficient: float = 0.0
    quality_score: float = 0.0
    mean_offset_ms: float = 0.0
    std_dev_offset_ms: float = 0.0
    max_offset_ms: float = 0.0
    frames_processed: int = 0
    sync_violations: int = 0
    recovery_time_ms: float = 0.0
@dataclass
class SyncFrame:
    timestamp: float
    frame_id: int
    camera1_frame: np.ndarray
    camera2_frame: np.ndarray
    camera1_hardware_ts: Optional[float] = None
    camera2_hardware_ts: Optional[float] = None
    software_capture_ts: float = field(default_factory=time.time)
    sync_quality: float = 0.0
    processing_latency_ms: float = 0.0
    def get_sync_offset_ms(self) -> float:
        if self.camera1_hardware_ts and self.camera2_hardware_ts:
            return abs(self.camera1_hardware_ts - self.camera2_hardware_ts) * 1000
        return 0.0
class AdaptiveSynchronizer:
    def __init__(
        self,
        target_fps: float = 30.0,
        sync_threshold_ms: float = 16.67,
        buffer_size: int = 100,
        strategy: SynchronizationStrategy = SynchronizationStrategy.ADAPTIVE_HYBRID,
    ):
        self.target_fps = target_fps
        self.frame_interval_ms = 1000.0 / target_fps
        self.sync_threshold_ms = sync_threshold_ms
        self.current_strategy = strategy
        self.timing_buffer = deque(maxlen=buffer_size)
        self.offset_history = deque(maxlen=buffer_size)
        self.quality_history = deque(maxlen=buffer_size)
        self.master_clock_offset = 0.0
        self.drift_compensation = 0.0
        self.adaptive_threshold = sync_threshold_ms
        self.metrics = TimingMetrics()
        self._lock = threading.Lock()
        self.cross_corr_window_size = 32
        self.adaptation_rate = 0.1
        self.drift_detection_window = 50
        logger.info(
            f"AdaptiveSynchronizer initialized: {target_fps}fps, threshold={sync_threshold_ms}ms, strategy={strategy.value}"
        )
    @performance_timer("synchronize_frames")
    def synchronize_frames(
        self,
        frame1: np.ndarray,
        frame2: np.ndarray,
        timestamp1: float,
        timestamp2: float,
        hardware_ts1: Optional[float] = None,
        hardware_ts2: Optional[float] = None,
    ) -> SyncFrame:
        process_start = time.time()
        sync_frame = SyncFrame(
            timestamp=min(timestamp1, timestamp2),
            frame_id=self.metrics.frames_processed,
            camera1_frame=frame1,
            camera2_frame=frame2,
            camera1_hardware_ts=hardware_ts1,
            camera2_hardware_ts=hardware_ts2,
            software_capture_ts=time.time(),
        )
        offset_ms = abs(timestamp1 - timestamp2) * 1000
        sync_frame.sync_quality = self._calculate_sync_quality(offset_ms)
        if self.current_strategy == SynchronizationStrategy.MASTER_SLAVE:
            sync_frame = self._master_slave_sync(sync_frame)
        elif self.current_strategy == SynchronizationStrategy.CROSS_CORRELATION:
            sync_frame = self._cross_correlation_sync(sync_frame)
        elif self.current_strategy == SynchronizationStrategy.HARDWARE_SYNC:
            sync_frame = self._hardware_sync(sync_frame)
        elif self.current_strategy == SynchronizationStrategy.ADAPTIVE_HYBRID:
            sync_frame = self._adaptive_hybrid_sync(sync_frame)
        with self._lock:
            self._update_metrics(sync_frame, offset_ms)
            self._adapt_parameters()
        sync_frame.processing_latency_ms = (time.time() - process_start) * 1000
        return sync_frame
    def _master_slave_sync(self, sync_frame: SyncFrame) -> SyncFrame:
        if sync_frame.camera1_hardware_ts and sync_frame.camera2_hardware_ts:
            offset = sync_frame.camera1_hardware_ts - sync_frame.camera2_hardware_ts
            self.master_clock_offset = offset
        sync_frame.sync_quality = min(
            1.0, 1.0 - abs(self.master_clock_offset) / self.sync_threshold_ms
        )
        return sync_frame
    def _cross_correlation_sync(self, sync_frame: SyncFrame) -> SyncFrame:
        try:
            gray1 = cv2.cvtColor(sync_frame.camera1_frame, cv2.COLOR_BGR2GRAY)
            gray2 = cv2.cvtColor(sync_frame.camera2_frame, cv2.COLOR_BGR2GRAY)
            h, w = gray1.shape
            scale = min(1.0, self.cross_corr_window_size / min(h, w))
            if scale < 1.0:
                new_h, new_w = int(h * scale), int(w * scale)
                gray1 = cv2.resize(gray1, (new_w, new_h))
                gray2 = cv2.resize(gray2, (new_w, new_h))
            correlation = cv2.matchTemplate(gray1, gray2, cv2.TM_CCOEFF_NORMED)
            _, max_corr, _, _ = cv2.minMaxLoc(correlation)
            sync_frame.sync_quality = max(0.0, max_corr)
        except Exception as e:
            logger.warning(f"Cross-correlation sync failed: {e}")
            sync_frame.sync_quality = 0.5
        return sync_frame
    def _hardware_sync(self, sync_frame: SyncFrame) -> SyncFrame:
        if sync_frame.camera1_hardware_ts and sync_frame.camera2_hardware_ts:
            offset_ms = sync_frame.get_sync_offset_ms()
            sync_frame.sync_quality = max(0.0, 1.0 - offset_ms / self.sync_threshold_ms)
        else:
            software_offset = (
                abs(sync_frame.timestamp - sync_frame.software_capture_ts) * 1000
            )
            sync_frame.sync_quality = max(
                0.0, 1.0 - software_offset / self.sync_threshold_ms
            )
        return sync_frame
    def _adaptive_hybrid_sync(self, sync_frame: SyncFrame) -> SyncFrame:
        if sync_frame.camera1_hardware_ts and sync_frame.camera2_hardware_ts:
            sync_frame = self._hardware_sync(sync_frame)
            if sync_frame.sync_quality > 0.8:
                return sync_frame
        sync_frame = self._cross_correlation_sync(sync_frame)
        if sync_frame.sync_quality < 0.6:
            sync_frame = self._master_slave_sync(sync_frame)
        return sync_frame
    def _calculate_sync_quality(self, offset_ms: float) -> float:
        if offset_ms <= self.adaptive_threshold:
            return 1.0
        elif offset_ms <= self.adaptive_threshold * 2:
            return 1.0 - (offset_ms - self.adaptive_threshold) / self.adaptive_threshold
        else:
            return 0.0
    def _update_metrics(self, sync_frame: SyncFrame, offset_ms: float):
        self.offset_history.append(offset_ms)
        self.quality_history.append(sync_frame.sync_quality)
        self.metrics.frames_processed += 1
        if len(self.offset_history) > 1:
            self.metrics.mean_offset_ms = statistics.mean(self.offset_history)
            self.metrics.std_dev_offset_ms = statistics.stdev(self.offset_history)
            self.metrics.max_offset_ms = max(self.offset_history)
        self.metrics.sync_offset_ms = offset_ms
        self.metrics.quality_score = sync_frame.sync_quality
        if offset_ms > self.adaptive_threshold:
            self.metrics.sync_violations += 1
        if len(self.offset_history) >= 3:
            recent_offsets = list(self.offset_history)[-3:]
            self.metrics.jitter_ms = statistics.stdev(recent_offsets)
    def _adapt_parameters(self):
        if len(self.quality_history) < 10:
            return
        recent_quality = list(self.quality_history)[-10:]
        avg_quality = statistics.mean(recent_quality)
        if avg_quality > 0.9:
            self.adaptive_threshold = max(
                self.sync_threshold_ms * 0.5,
                self.adaptive_threshold * (1 - self.adaptation_rate),
            )
        elif avg_quality < 0.7:
            self.adaptive_threshold = min(
                self.sync_threshold_ms * 2.0,
                self.adaptive_threshold * (1 + self.adaptation_rate),
            )
        if len(self.offset_history) >= self.drift_detection_window:
            recent_offsets = list(self.offset_history)[-self.drift_detection_window :]
            x = np.arange(len(recent_offsets))
            y = np.array(recent_offsets)
            if len(x) > 1:
                drift_slope = np.polyfit(x, y, 1)[0]
                self.drift_compensation += drift_slope * self.adaptation_rate
                self.metrics.drift_rate_ppm = drift_slope * 1000
    def get_diagnostics(self) -> Dict:
        with self._lock:
            return {
                "strategy": self.current_strategy.value,
                "adaptive_threshold_ms": self.adaptive_threshold,
                "master_clock_offset": self.master_clock_offset,
                "drift_compensation": self.drift_compensation,
                "buffer_sizes": {
                    "timing": len(self.timing_buffer),
                    "offset": len(self.offset_history),
                    "quality": len(self.quality_history),
                },
                "metrics": {
                    "frames_processed": self.metrics.frames_processed,
                    "sync_violations": self.metrics.sync_violations,
                    "violation_rate": self.metrics.sync_violations
                    / max(1, self.metrics.frames_processed),
                    "mean_offset_ms": self.metrics.mean_offset_ms,
                    "std_dev_offset_ms": self.metrics.std_dev_offset_ms,
                    "max_offset_ms": self.metrics.max_offset_ms,
                    "jitter_ms": self.metrics.jitter_ms,
                    "drift_rate_ppm": self.metrics.drift_rate_ppm,
                    "current_quality": self.metrics.quality_score,
                },
            }
    def reset_metrics(self):
        with self._lock:
            self.timing_buffer.clear()
            self.offset_history.clear()
            self.quality_history.clear()
            self.metrics = TimingMetrics()
            self.master_clock_offset = 0.0
            self.drift_compensation = 0.0
            self.adaptive_threshold = self.sync_threshold_ms
        logger.info("Synchronizer metrics reset")
    def set_strategy(self, strategy: SynchronizationStrategy):
        old_strategy = self.current_strategy
        self.current_strategy = strategy
        logger.info(
            f"Synchronisation strategy changed: {old_strategy.value} -> {strategy.value}"
        )
def test_dual_camera_sync(
    camera1_index: int = 0, camera2_index: int = 1, duration_seconds: int = 10
) -> Dict:
    logger.info(
        f"Starting dual camera synchronisation test (cameras {camera1_index}, {camera2_index})"
    )
    synchronizer = AdaptiveSynchronizer(
        target_fps=30.0, strategy=SynchronizationStrategy.ADAPTIVE_HYBRID
    )
    try:
        cap1 = cv2.VideoCapture(camera1_index)
        cap2 = cv2.VideoCapture(camera2_index)
        if not cap1.isOpened() or not cap2.isOpened():
            return {"error": "Failed to open cameras", "success": False}
        for cap in [cap1, cap2]:
            cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
            cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
            cap.set(cv2.CAP_PROP_FPS, 30)
            cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
        start_time = time.time()
        frame_count = 0
        sync_frames = []
        while time.time() - start_time < duration_seconds:
            ret1, frame1 = cap1.read()
            ts1 = time.time()
            ret2, frame2 = cap2.read()
            ts2 = time.time()
            if ret1 and ret2:
                sync_frame = synchronizer.synchronize_frames(frame1, frame2, ts1, ts2)
                sync_frames.append(sync_frame)
                frame_count += 1
                if frame_count % 30 == 0:
                    logger.info(
                        f"Processed {frame_count} frames, sync quality: {sync_frame.sync_quality:.3f}"
                    )
            time.sleep(1 / 30)
        cap1.release()
        cap2.release()
        diagnostics = synchronizer.get_diagnostics()
        test_results = {
            "success": True,
            "duration_seconds": time.time() - start_time,
            "frames_captured": frame_count,
            "average_fps": frame_count / (time.time() - start_time),
            "synchronization_metrics": diagnostics,
            "quality_statistics": {
                "mean_quality": (
                    statistics.mean([f.sync_quality for f in sync_frames])
                    if sync_frames
                    else 0
                ),
                "min_quality": (
                    min([f.sync_quality for f in sync_frames]) if sync_frames else 0
                ),
                "max_quality": (
                    max([f.sync_quality for f in sync_frames]) if sync_frames else 0
                ),
                "frames_above_threshold": sum(
                    1 for f in sync_frames if f.sync_quality > 0.8
                ),
            },
        }
        logger.info(
            f"Synchronisation test completed: {frame_count} frames, avg quality: {test_results['quality_statistics']['mean_quality']:.3f}"
        )
        return test_results
    except Exception as e:
        logger.error(f"Synchronisation test failed: {e}")
        return {"error": str(e), "success": False}
if __name__ == "__main__":
    results = test_dual_camera_sync()
    print(f"Test Results: {results}")