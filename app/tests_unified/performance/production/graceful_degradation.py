import asyncio
import logging
import queue
import threading
import time
from collections import deque
from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Dict, List, Optional, Callable, Tuple
from concurrent.futures import ThreadPoolExecutor
import weakref
try:
    import psutil
except ImportError:
    psutil = None
try:
    from ..utils.logging_config import get_logger
except ImportError:
    import logging
    def get_logger(name):
        return logging.getLogger(name)
class PerformanceLevel(Enum):
    OPTIMAL = "optimal"
    GOOD = "good"
    DEGRADED = "degraded"
    CRITICAL = "critical"
class DegradationStrategy(Enum):
    FRAME_DROPPING = "frame_dropping"
    QUALITY_REDUCTION = "quality_reduction"
    RESOLUTION_REDUCTION = "resolution_reduction"
    FRAMERATE_REDUCTION = "framerate_reduction"
    PREVIEW_DISABLING = "preview_disabling"
    NON_ESSENTIAL_DISABLING = "non_essential_disabling"
    MEMORY_CLEANUP = "memory_cleanup"
    THREAD_REDUCTION = "thread_reduction"
@dataclass
class PerformanceThresholds:
    cpu_good_threshold: float = 60.0
    cpu_degraded_threshold: float = 75.0
    cpu_critical_threshold: float = 90.0
    memory_good_threshold: float = 70.0
    memory_degraded_threshold: float = 85.0
    memory_critical_threshold: float = 95.0
    disk_write_good_threshold: float = 50.0
    disk_write_degraded_threshold: float = 20.0
    disk_write_critical_threshold: float = 10.0
    network_good_threshold: float = 50.0
    network_degraded_threshold: float = 20.0
    network_critical_threshold: float = 5.0
    queue_good_threshold: int = 100
    queue_degraded_threshold: int = 500
    queue_critical_threshold: int = 1000
    response_time_good_threshold: float = 100.0
    response_time_degraded_threshold: float = 500.0
    response_time_critical_threshold: float = 2000.0
@dataclass
class DegradationAction:
    strategy: DegradationStrategy
    level: PerformanceLevel
    description: str
    enabled: bool = True
    priority: int = 5
    frame_drop_rate: Optional[float] = None
    quality_reduction_factor: Optional[float] = None
    resolution_scale_factor: Optional[float] = None
    framerate_reduction_factor: Optional[float] = None
    custom_action: Optional[Callable[[], None]] = None
@dataclass
class SystemMetrics:
    timestamp: float
    cpu_percent: float
    memory_percent: float
    disk_write_speed_mbps: float
    network_speed_mbps: float
    queue_sizes: Dict[str, int] = field(default_factory=dict)
    response_times: Dict[str, float] = field(default_factory=dict)
    active_recordings: int = 0
    frame_processing_rate: float = 0.0
    preview_enabled: bool = True
    thermal_processing_enabled: bool = True
class FrameDropManager:
    def __init__(self, logger: logging.Logger):
        self.logger = logger
        self.drop_rate = 0.0
        self.frames_received = 0
        self.frames_dropped = 0
        self.last_reset = time.time()
    def should_drop_frame(self) -> bool:
        self.frames_received += 1
        if self.drop_rate <= 0.0:
            return False
        drop_interval = int(1.0 / self.drop_rate) if self.drop_rate > 0 else float('inf')
        if self.frames_received % drop_interval == 0:
            self.frames_dropped += 1
            self.logger.debug(f"FrameDropManager: Dropped frame {self.frames_received} (rate: {self.drop_rate:.2f})")
            return True
        return False
    def set_drop_rate(self, rate: float):
        rate = max(0.0, min(1.0, rate))
        if rate != self.drop_rate:
            self.logger.info(f"FrameDropManager: Drop rate changed from {self.drop_rate:.2f} to {rate:.2f}")
            self.drop_rate = rate
    def get_drop_statistics(self) -> Dict[str, Any]:
        elapsed = time.time() - self.last_reset
        return {
            "frames_received": self.frames_received,
            "frames_dropped": self.frames_dropped,
            "drop_rate_actual": self.frames_dropped / self.frames_received if self.frames_received > 0 else 0,
            "drop_rate_target": self.drop_rate,
            "elapsed_seconds": elapsed
        }
    def reset_statistics(self):
        self.frames_received = 0
        self.frames_dropped = 0
        self.last_reset = time.time()
class AdaptiveQualityManager:
    def __init__(self, logger: logging.Logger):
        self.logger = logger
        self.current_quality_factor = 1.0
        self.current_resolution_factor = 1.0
        self.current_framerate_factor = 1.0
        self.baseline_values = {}
    def set_quality_factor(self, factor: float):
        factor = max(0.1, min(1.0, factor))
        if factor != self.current_quality_factor:
            self.logger.info(f"AdaptiveQualityManager: Quality factor changed to {factor:.2f}")
            self.current_quality_factor = factor
    def set_resolution_factor(self, factor: float):
        factor = max(0.25, min(1.0, factor))
        if factor != self.current_resolution_factor:
            self.logger.info(f"AdaptiveQualityManager: Resolution factor changed to {factor:.2f}")
            self.current_resolution_factor = factor
    def set_framerate_factor(self, factor: float):
        factor = max(0.1, min(1.0, factor))
        if factor != self.current_framerate_factor:
            self.logger.info(f"AdaptiveQualityManager: Framerate factor changed to {factor:.2f}")
            self.current_framerate_factor = factor
    def get_adapted_settings(self, baseline_quality: int = 85,
                           baseline_resolution: Tuple[int, int] = (1920, 1080),
                           baseline_framerate: float = 30.0) -> Dict[str, Any]:
        return {
            "quality": int(baseline_quality * self.current_quality_factor),
            "resolution": (
                int(baseline_resolution[0] * self.current_resolution_factor),
                int(baseline_resolution[1] * self.current_resolution_factor)
            ),
            "framerate": baseline_framerate * self.current_framerate_factor,
            "quality_factor": self.current_quality_factor,
            "resolution_factor": self.current_resolution_factor,
            "framerate_factor": self.current_framerate_factor
        }
    def reset_to_optimal(self):
        self.current_quality_factor = 1.0
        self.current_resolution_factor = 1.0
        self.current_framerate_factor = 1.0
        self.logger.info("AdaptiveQualityManager: Reset to optimal quality")
class BackpressureManager:
    def __init__(self, logger: logging.Logger):
        self.logger = logger
        self.monitored_queues: Dict[str, queue.Queue] = {}
        self.queue_limits: Dict[str, int] = {}
        self.backpressure_callbacks: Dict[str, List[Callable]] = {}
    def register_queue(self, name: str, queue_obj: queue.Queue,
                      limit: int, backpressure_callback: Optional[Callable] = None):
        self.monitored_queues[name] = queue_obj
        self.queue_limits[name] = limit
        if backpressure_callback:
            if name not in self.backpressure_callbacks:
                self.backpressure_callbacks[name] = []
            self.backpressure_callbacks[name].append(backpressure_callback)
        self.logger.info(f"BackpressureManager: Registered queue '{name}' with limit {limit}")
    def check_backpressure(self) -> Dict[str, Any]:
        results = {}
        for name, queue_obj in self.monitored_queues.items():
            current_size = queue_obj.qsize()
            limit = self.queue_limits.get(name, 1000)
            backpressure_ratio = current_size / limit
            has_backpressure = backpressure_ratio > 0.8
            results[name] = {
                "size": current_size,
                "limit": limit,
                "ratio": backpressure_ratio,
                "has_backpressure": has_backpressure
            }
            if has_backpressure:
                self.logger.warning(f"BackpressureManager: Backpressure detected in queue '{name}': {current_size}/{limit}")
                if name in self.backpressure_callbacks:
                    for callback in self.backpressure_callbacks[name]:
                        try:
                            callback()
                        except Exception as e:
                            self.logger.error(f"BackpressureManager: Error in backpressure callback: {e}")
        return results
    def get_queue_status(self) -> Dict[str, int]:
        return {name: queue_obj.qsize() for name, queue_obj in self.monitored_queues.items()}
class GracefulDegradationManager:
    def __init__(self, thresholds: Optional[PerformanceThresholds] = None):
        self.logger = get_logger(__name__)
        self.thresholds = thresholds or PerformanceThresholds()
        self.frame_drop_manager = FrameDropManager(self.logger)
        self.quality_manager = AdaptiveQualityManager(self.logger)
        self.backpressure_manager = BackpressureManager(self.logger)
        self.current_level = PerformanceLevel.OPTIMAL
        self.active_degradations: List[DegradationAction] = []
        self.monitoring_enabled = False
        self.monitoring_task: Optional[asyncio.Task] = None
        self.degradation_actions = self._create_default_degradation_actions()
        self.level_change_callbacks: List[Callable[[PerformanceLevel], None]] = []
        self.metrics_callbacks: List[Callable[[], SystemMetrics]] = []
        self.metrics_history = deque(maxlen=60)
    def _create_default_degradation_actions(self) -> List[DegradationAction]:
        return [
            DegradationAction(
                strategy=DegradationStrategy.PREVIEW_DISABLING,
                level=PerformanceLevel.DEGRADED,
                description="Disable real-time preview to reduce CPU load",
                priority=1
            ),
            DegradationAction(
                strategy=DegradationStrategy.FRAME_DROPPING,
                level=PerformanceLevel.DEGRADED,
                description="Drop 10% of frames to prevent memory overflow",
                priority=2,
                frame_drop_rate=0.1
            ),
            DegradationAction(
                strategy=DegradationStrategy.QUALITY_REDUCTION,
                level=PerformanceLevel.DEGRADED,
                description="Reduce recording quality to 70%",
                priority=3,
                quality_reduction_factor=0.7
            ),
            DegradationAction(
                strategy=DegradationStrategy.FRAME_DROPPING,
                level=PerformanceLevel.CRITICAL,
                description="Drop 25% of frames to prevent system crash",
                priority=1,
                frame_drop_rate=0.25
            ),
            DegradationAction(
                strategy=DegradationStrategy.RESOLUTION_REDUCTION,
                level=PerformanceLevel.CRITICAL,
                description="Reduce resolution to 720p",
                priority=2,
                resolution_scale_factor=0.67
            ),
            DegradationAction(
                strategy=DegradationStrategy.FRAMERATE_REDUCTION,
                level=PerformanceLevel.CRITICAL,
                description="Reduce framerate to 15 FPS",
                priority=3,
                framerate_reduction_factor=0.5
            ),
            DegradationAction(
                strategy=DegradationStrategy.NON_ESSENTIAL_DISABLING,
                level=PerformanceLevel.CRITICAL,
                description="Disable non-essential features and logging",
                priority=4
            ),
            DegradationAction(
                strategy=DegradationStrategy.MEMORY_CLEANUP,
                level=PerformanceLevel.CRITICAL,
                description="Aggressive memory cleanup and garbage collection",
                priority=5
            ),
        ]
    def add_metrics_callback(self, callback: Callable[[], SystemMetrics]):
        self.metrics_callbacks.append(callback)
    def add_level_change_callback(self, callback: Callable[[PerformanceLevel], None]):
        self.level_change_callbacks.append(callback)
    def register_queue_for_backpressure(self, name: str, queue_obj: queue.Queue,
                                      limit: int, callback: Optional[Callable] = None):
        self.backpressure_manager.register_queue(name, queue_obj, limit, callback)
    async def start_monitoring(self, interval_seconds: float = 5.0):
        if self.monitoring_enabled:
            self.logger.warning("GracefulDegradationManager: Monitoring already enabled")
            return
        self.monitoring_enabled = True
        self.monitoring_task = asyncio.create_task(self._monitoring_loop(interval_seconds))
        self.logger.info("GracefulDegradationManager: Started performance monitoring")
    async def stop_monitoring(self):
        self.monitoring_enabled = False
        if self.monitoring_task:
            self.monitoring_task.cancel()
            try:
                await self.monitoring_task
            except asyncio.CancelledError:
                pass
        self.logger.info("GracefulDegradationManager: Stopped performance monitoring")
    async def _monitoring_loop(self, interval_seconds: float):
        while self.monitoring_enabled:
            try:
                metrics = await self._collect_system_metrics()
                self.metrics_history.append(metrics)
                new_level = self._determine_performance_level(metrics)
                if new_level != self.current_level:
                    await self._handle_level_change(self.current_level, new_level, metrics)
                backpressure_status = self.backpressure_manager.check_backpressure()
                if any(status["has_backpressure"] for status in backpressure_status.values()):
                    await self._handle_backpressure(backpressure_status)
                await asyncio.sleep(interval_seconds)
            except asyncio.CancelledError:
                break
            except Exception as e:
                self.logger.error(f"GracefulDegradationManager: Error in monitoring loop: {e}")
                await asyncio.sleep(interval_seconds)
    async def _collect_system_metrics(self) -> SystemMetrics:
        current_time = time.time()
        metrics = SystemMetrics(
            timestamp=current_time,
            cpu_percent=0.0,
            memory_percent=0.0,
            disk_write_speed_mbps=0.0,
            network_speed_mbps=0.0
        )
        if psutil:
            try:
                metrics.cpu_percent = psutil.cpu_percent(interval=0.1)
                metrics.memory_percent = psutil.virtual_memory().percent
                disk_io = psutil.disk_io_counters()
                if disk_io and hasattr(self, '_last_disk_io'):
                    elapsed = current_time - self._last_disk_io_time
                    bytes_written = disk_io.write_bytes - self._last_disk_io.write_bytes
                    metrics.disk_write_speed_mbps = (bytes_written / elapsed) / (1024 * 1024) if elapsed > 0 else 0.0
                self._last_disk_io = disk_io
                self._last_disk_io_time = current_time
            except Exception as e:
                self.logger.error(f"GracefulDegradationManager: Error collecting psutil metrics: {e}")
        for callback in self.metrics_callbacks:
            try:
                app_metrics = callback()
                if isinstance(app_metrics, SystemMetrics):
                    metrics.queue_sizes.update(app_metrics.queue_sizes)
                    metrics.response_times.update(app_metrics.response_times)
                    metrics.active_recordings = app_metrics.active_recordings
                    metrics.frame_processing_rate = app_metrics.frame_processing_rate
            except Exception as e:
                self.logger.error(f"GracefulDegradationManager: Error in metrics callback: {e}")
        queue_status = self.backpressure_manager.get_queue_status()
        metrics.queue_sizes.update(queue_status)
        return metrics
    def _determine_performance_level(self, metrics: SystemMetrics) -> PerformanceLevel:
        if (metrics.cpu_percent >= self.thresholds.cpu_critical_threshold or
            metrics.memory_percent >= self.thresholds.memory_critical_threshold or
            metrics.disk_write_speed_mbps <= self.thresholds.disk_write_critical_threshold):
            return PerformanceLevel.CRITICAL
        if (metrics.cpu_percent >= self.thresholds.cpu_degraded_threshold or
            metrics.memory_percent >= self.thresholds.memory_degraded_threshold or
            metrics.disk_write_speed_mbps <= self.thresholds.disk_write_degraded_threshold):
            return PerformanceLevel.DEGRADED
        if (metrics.cpu_percent >= self.thresholds.cpu_good_threshold or
            metrics.memory_percent >= self.thresholds.memory_good_threshold or
            metrics.disk_write_speed_mbps <= self.thresholds.disk_write_good_threshold):
            return PerformanceLevel.GOOD
        return PerformanceLevel.OPTIMAL
    async def _handle_level_change(self, old_level: PerformanceLevel,
                                 new_level: PerformanceLevel, metrics: SystemMetrics):
        self.logger.info(f"GracefulDegradationManager: Performance level changed from {old_level.value} to {new_level.value}")
        old_level_obj = self.current_level
        self.current_level = new_level
        if new_level.value != old_level.value:
            await self._apply_degradation_for_level(new_level, metrics)
            await self._remove_unnecessary_degradations(new_level)
        for callback in self.level_change_callbacks:
            try:
                callback(new_level)
            except Exception as e:
                self.logger.error(f"GracefulDegradationManager: Error in level change callback: {e}")
    async def _apply_degradation_for_level(self, level: PerformanceLevel, metrics: SystemMetrics):
        relevant_actions = [action for action in self.degradation_actions
                          if action.level == level and action.enabled]
        relevant_actions.sort(key=lambda x: x.priority)
        for action in relevant_actions:
            if action not in self.active_degradations:
                await self._execute_degradation_action(action, metrics)
                self.active_degradations.append(action)
    async def _remove_unnecessary_degradations(self, current_level: PerformanceLevel):
        level_order = [PerformanceLevel.OPTIMAL, PerformanceLevel.GOOD,
                      PerformanceLevel.DEGRADED, PerformanceLevel.CRITICAL]
        current_index = level_order.index(current_level)
        actions_to_remove = []
        for action in self.active_degradations:
            action_index = level_order.index(action.level)
            if action_index > current_index:
                actions_to_remove.append(action)
        for action in actions_to_remove:
            await self._revert_degradation_action(action)
            self.active_degradations.remove(action)
    async def _execute_degradation_action(self, action: DegradationAction, metrics: SystemMetrics):
        self.logger.info(f"GracefulDegradationManager: Applying degradation - {action.description}")
        try:
            if action.strategy == DegradationStrategy.FRAME_DROPPING:
                if action.frame_drop_rate is not None:
                    self.frame_drop_manager.set_drop_rate(action.frame_drop_rate)
            elif action.strategy == DegradationStrategy.QUALITY_REDUCTION:
                if action.quality_reduction_factor is not None:
                    self.quality_manager.set_quality_factor(action.quality_reduction_factor)
            elif action.strategy == DegradationStrategy.RESOLUTION_REDUCTION:
                if action.resolution_scale_factor is not None:
                    self.quality_manager.set_resolution_factor(action.resolution_scale_factor)
            elif action.strategy == DegradationStrategy.FRAMERATE_REDUCTION:
                if action.framerate_reduction_factor is not None:
                    self.quality_manager.set_framerate_factor(action.framerate_reduction_factor)
            elif action.strategy == DegradationStrategy.MEMORY_CLEANUP:
                await self._perform_memory_cleanup()
            elif action.strategy == DegradationStrategy.PREVIEW_DISABLING:
                self.logger.info("GracefulDegradationManager: Preview disabled to save resources")
            elif action.strategy == DegradationStrategy.NON_ESSENTIAL_DISABLING:
                self.logger.info("GracefulDegradationManager: Non-essential features disabled")
            if action.custom_action:
                action.custom_action()
        except Exception as e:
            self.logger.error(f"GracefulDegradationManager: Error executing degradation action: {e}")
    async def _revert_degradation_action(self, action: DegradationAction):
        self.logger.info(f"GracefulDegradationManager: Reverting degradation - {action.description}")
        try:
            if action.strategy == DegradationStrategy.FRAME_DROPPING:
                self.frame_drop_manager.set_drop_rate(0.0)
            elif action.strategy in [DegradationStrategy.QUALITY_REDUCTION,
                                   DegradationStrategy.RESOLUTION_REDUCTION,
                                   DegradationStrategy.FRAMERATE_REDUCTION]:
                other_quality_actions = [a for a in self.active_degradations
                                       if a != action and a.strategy in [
                                           DegradationStrategy.QUALITY_REDUCTION,
                                           DegradationStrategy.RESOLUTION_REDUCTION,
                                           DegradationStrategy.FRAMERATE_REDUCTION
                                       ]]
                if not other_quality_actions:
                    self.quality_manager.reset_to_optimal()
        except Exception as e:
            self.logger.error(f"GracefulDegradationManager: Error reverting degradation action: {e}")
    async def _perform_memory_cleanup(self):
        import gc
        self.logger.info("GracefulDegradationManager: Performing memory cleanup")
        collected = gc.collect()
        self.logger.debug(f"GracefulDegradationManager: Garbage collection freed {collected} objects")
        self.frame_drop_manager.reset_statistics()
    async def _handle_backpressure(self, backpressure_status: Dict[str, Any]):
        critical_queues = [name for name, status in backpressure_status.items()
                          if status["has_backpressure"]]
        if critical_queues:
            self.logger.warning(f"GracefulDegradationManager: Handling backpressure in queues: {critical_queues}")
            if not any(action.strategy == DegradationStrategy.FRAME_DROPPING
                      for action in self.active_degradations):
                emergency_action = DegradationAction(
                    strategy=DegradationStrategy.FRAME_DROPPING,
                    level=self.current_level,
                    description="Emergency frame dropping due to backpressure",
                    frame_drop_rate=0.3
                )
                await self._execute_degradation_action(emergency_action, None)
                self.active_degradations.append(emergency_action)
    def get_current_status(self) -> Dict[str, Any]:
        return {
            "current_level": self.current_level.value,
            "active_degradations": [
                {
                    "strategy": action.strategy.value,
                    "description": action.description,
                    "level": action.level.value
                } for action in self.active_degradations
            ],
            "frame_drop_stats": self.frame_drop_manager.get_drop_statistics(),
            "quality_settings": self.quality_manager.get_adapted_settings(),
            "queue_status": self.backpressure_manager.get_queue_status(),
            "monitoring_enabled": self.monitoring_enabled,
            "metrics_history_size": len(self.metrics_history)
        }
    def should_drop_frame(self) -> bool:
        return self.frame_drop_manager.should_drop_frame()
    def get_adapted_quality_settings(self, **baseline_settings) -> Dict[str, Any]:
        return self.quality_manager.get_adapted_settings(**baseline_settings)
_degradation_manager: Optional[GracefulDegradationManager] = None
def get_degradation_manager(thresholds: Optional[PerformanceThresholds] = None) -> GracefulDegradationManager:
    global _degradation_manager
    if _degradation_manager is None:
        _degradation_manager = GracefulDegradationManager(thresholds)
    return _degradation_manager
async def main():
    import asyncio
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    thresholds = PerformanceThresholds(
        cpu_degraded_threshold=60.0,
        memory_degraded_threshold=70.0
    )
    manager = GracefulDegradationManager(thresholds)
    def get_test_metrics():
        return SystemMetrics(
            timestamp=time.time(),
            cpu_percent=65.0,
            memory_percent=75.0,
            disk_write_speed_mbps=30.0,
            network_speed_mbps=40.0,
            queue_sizes={"video_processing": 150},
            active_recordings=3
        )
    manager.add_metrics_callback(get_test_metrics)
    await manager.start_monitoring(interval_seconds=2.0)
    print("Degradation manager started. Monitoring for 30 seconds...")
    await asyncio.sleep(30)
    status = manager.get_current_status()
    print(f"Final status: {status}")
    await manager.stop_monitoring()
    print("Degradation manager stopped.")
if __name__ == "__main__":
    asyncio.run(main())