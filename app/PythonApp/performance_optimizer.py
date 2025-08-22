"""
Performance optimization interface - delegates to unified system monitoring.

This module provides performance optimization functionality while using
the unified system monitoring and optimization features.
"""

import time
from typing import Dict, Any, Optional

from .shared_protocols.system_monitoring import (
    get_system_monitor,
    PerformanceMetrics,
    ResourceLimits
)

try:
    from .utils.logging_config import get_logger
    logger = get_logger(__name__)
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)


class PerformanceOptimizer:
    """
    Performance optimization interface that delegates to unified system monitoring.
    
    Provides backwards compatibility while using the consolidated monitoring system.
    """
    
    def __init__(self):
        self._monitor = get_system_monitor()
        self.optimization_active = False
        logger.info("Performance optimizer initialized using unified monitoring")
    
    def start_optimization(self):
        """Start performance optimization."""
        try:
            self._monitor.optimize_for_recording()
            self._monitor.start_monitoring()
            self.optimization_active = True
            logger.info("Performance optimization started")
            return True
        except Exception as e:
            logger.error(f"Failed to start performance optimization: {e}")
            return False
    
    def stop_optimization(self):
        """Stop performance optimization."""
        try:
            self._monitor.optimize_for_idle()
            self.optimization_active = False
            logger.info("Performance optimization stopped")
        except Exception as e:
            logger.error(f"Error stopping performance optimization: {e}")
    
    def get_performance_metrics(self) -> Optional[Dict[str, Any]]:
        """Get current performance metrics."""
        metrics = self._monitor.get_current_metrics()
        return metrics.__dict__ if metrics else None
    
    def get_optimization_recommendations(self) -> Dict[str, Any]:
        """Get optimization recommendations based on current metrics."""
        health_report = self._monitor.get_system_health_report()
        
        recommendations = []
        
        if health_report.get("status") == "critical":
            recommendations.append("System is under heavy load - consider reducing quality settings")
        elif health_report.get("status") == "warning":
            recommendations.append("System performance degraded - monitor closely")
        
        current = health_report.get("current", {})
        if current.get("memory_percent", 0) > 80:
            recommendations.append("High memory usage - consider enabling more aggressive cleanup")
        
        if current.get("cpu_percent", 0) > 80:
            recommendations.append("High CPU usage - consider reducing processing load")
        
        return {
            "timestamp": time.time(),
            "status": health_report.get("status", "unknown"),
            "recommendations": recommendations,
            "current_metrics": current
        }
    
    def perform_cleanup(self):
        """Trigger system cleanup."""
        self._monitor.perform_cleanup()
    
    def is_system_ready_for_recording(self) -> bool:
        """Check if system is ready for recording."""
        health_report = self._monitor.get_system_health_report()
        return health_report.get("status") in ["good", "warning"]


# Global instance for backwards compatibility
_global_optimizer = None


def get_performance_optimizer() -> PerformanceOptimizer:
    """Get the global performance optimizer instance."""
    global _global_optimizer
    if _global_optimizer is None:
        _global_optimizer = PerformanceOptimizer()
    return _global_optimizer


def optimize_for_recording():
    """Optimize system for recording."""
    return get_performance_optimizer().start_optimization()


def optimize_for_idle():
    """Optimize system for idle state."""
    get_performance_optimizer().stop_optimization()


def get_performance_status():
    """Get current performance status."""
    return get_performance_optimizer().get_optimization_recommendations()
    max_memory_mb: float = 2048.0
    max_cpu_percent: float = 80.0
    max_network_mbps: float = 100.0
    max_threads: int = 16
    memory_warning_threshold: float = 0.8
    cpu_warning_threshold: float = 0.7
    cleanup_interval_seconds: int = 300
    frame_drop_cpu_threshold: float = 85.0
    frame_drop_memory_threshold: float = 90.0
    quality_reduction_threshold: float = 80.0
    preview_disable_threshold: float = 95.0
    prefer_gpu_processing: bool = True
    use_hardware_codecs: bool = True
    detect_device_capabilities: bool = True
class MemoryOptimizer:
    def __init__(self, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.memory_pools = {}
        self.weak_references = weakref.WeakSet()
        self.cleanup_callbacks = []
    def start_memory_tracking(self):
        tracemalloc.start()
        self.logger.info("Memory tracking started")
    def stop_memory_tracking(self):
        if tracemalloc.is_tracing():
            current, peak = tracemalloc.get_traced_memory()
            tracemalloc.stop()
            return {"current_mb": current / 1024 / 1024, "peak_mb": peak / 1024 / 1024}
        return None
    def get_memory_snapshot(self) -> Dict[str, Any]:
        process = psutil.Process()
        memory_info = process.memory_info()
        snapshot = {
            "rss_mb": memory_info.rss / 1024 / 1024,
            "vms_mb": memory_info.vms / 1024 / 1024,
            "percent": process.memory_percent(),
            "available_mb": psutil.virtual_memory().available / 1024 / 1024,
            "total_mb": psutil.virtual_memory().total / 1024 / 1024,
        }
        if tracemalloc.is_tracing():
            current, peak = tracemalloc.get_traced_memory()
            snapshot.update(
                {
                    "traced_current_mb": current / 1024 / 1024,
                    "traced_peak_mb": peak / 1024 / 1024,
                }
            )
        return snapshot
    def optimize_memory_usage(self) -> Dict[str, Any]:
        initial_memory = self.get_memory_snapshot()
        collected = gc.collect()
        self.weak_references.clear()
        for callback in self.cleanup_callbacks:
            try:
                callback()
            except Exception as e:
                self.logger.error(f"Error in cleanup callback: {e}")
        for pool_name, pool in self.memory_pools.items():
            if hasattr(pool, "clear"):
                pool.clear()
                self.logger.debug(f"Cleared memory pool: {pool_name}")
        final_memory = self.get_memory_snapshot()
        optimization_result = {
            "initial_memory_mb": initial_memory["rss_mb"],
            "final_memory_mb": final_memory["rss_mb"],
            "memory_freed_mb": initial_memory["rss_mb"] - final_memory["rss_mb"],
            "objects_collected": collected,
            "cleanup_callbacks_run": len(self.cleanup_callbacks),
        }
        self.logger.info(
            f"Memory optimisation completed: freed {optimization_result['memory_freed_mb']:.2f}MB"
        )
        return optimization_result
    def register_cleanup_callback(self, callback: Callable[[], None]):
        self.cleanup_callbacks.append(callback)
    def create_memory_pool(self, name: str, initial_size: int = 100):
        self.memory_pools[name] = deque(maxlen=initial_size)
        return self.memory_pools[name]
    def get_top_memory_consumers(self, limit: int = 10) -> List[Dict[str, Any]]:
        if not tracemalloc.is_tracing():
            return []
        snapshot = tracemalloc.take_snapshot()
        top_stats = snapshot.statistics("lineno")
        consumers = []
        for stat in top_stats[:limit]:
            consumers.append(
                {
                    "filename": stat.traceback.format()[0],
                    "size_mb": stat.size / 1024 / 1024,
                    "count": stat.count,
                }
            )
        return consumers
class CPUOptimizer:
    def __init__(self, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.thread_pools = {}
        self.cpu_intensive_tasks = []
        self.load_balancer = None
    def create_optimized_thread_pool(
        self, name: str, max_workers: Optional[int] = None
    ) -> ThreadPoolExecutor:
        if max_workers is None:
            max_workers = min(psutil.cpu_count(logical=False), 8)
        pool = ThreadPoolExecutor(
            max_workers=max_workers, thread_name_prefix=f"OptimizedPool-{name}"
        )
        self.thread_pools[name] = pool
        self.logger.info(
            f"Created optimised thread pool '{name}' with {max_workers} workers"
        )
        return pool
    def get_cpu_metrics(self) -> Dict[str, Any]:
        cpu_percent = psutil.cpu_percent(interval=1, percpu=True)
        cpu_freq = psutil.cpu_freq()
        return {
            "overall_percent": psutil.cpu_percent(),
            "per_core_percent": cpu_percent,
            "core_count": psutil.cpu_count(logical=False),
            "logical_count": psutil.cpu_count(logical=True),
            "frequency_mhz": cpu_freq.current if cpu_freq else None,
            "load_average": os.getloadavg() if hasattr(os, "getloadavg") else None,
        }
    def optimize_cpu_usage(self) -> Dict[str, Any]:
        initial_metrics = self.get_cpu_metrics()
        optimisations = []
        for name, pool in self.thread_pools.items():
            if hasattr(pool, "_max_workers"):
                current_workers = pool._max_workers
                optimal_workers = self._calculate_optimal_workers(
                    initial_metrics["overall_percent"]
                )
                if optimal_workers != current_workers:
                    optimisations.append(
                        {
                            "pool": name,
                            "current_workers": current_workers,
                            "recommended_workers": optimal_workers,
                        }
                    )
        current_process = psutil.Process()
        if initial_metrics["overall_percent"] > 80:
            try:
                current_process.nice(1)
                optimisations.append({"action": "lowered_process_priority"})
            except OSError as e:
                logging.warning(f"Could not adjust process priority: {e}")
        final_metrics = self.get_cpu_metrics()
        return {
            "initial_cpu_percent": initial_metrics["overall_percent"],
            "final_cpu_percent": final_metrics["overall_percent"],
            "optimisations": optimisations,
        }
    def _calculate_optimal_workers(self, cpu_percent: float) -> int:
        base_workers = psutil.cpu_count(logical=False)
        if cpu_percent > 80:
            return max(1, base_workers // 2)
        elif cpu_percent < 30:
            return min(base_workers * 2, 16)
        else:
            return base_workers
    def cleanup_thread_pools(self):
        for name, pool in self.thread_pools.items():
            pool.shutdown(wait=True)
            self.logger.info(f"Shut down thread pool: {name}")
        self.thread_pools.clear()
class NetworkOptimizer:
    def __init__(self, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.bandwidth_history = deque(maxlen=60)
        self.quality_levels = {
            "high": {"resolution": (1920, 1080), "fps": 30, "quality": 0.9},
            "medium": {"resolution": (1280, 720), "fps": 20, "quality": 0.7},
            "low": {"resolution": (640, 480), "fps": 15, "quality": 0.5},
        }
        self.current_quality = "high"
    def get_network_metrics(self) -> Dict[str, Any]:
        net_io = psutil.net_io_counters()
        bandwidth_mbps = 0.0
        if self.bandwidth_history:
            last_measurement = self.bandwidth_history[-1]
            time_diff = time.time() - last_measurement["timestamp"]
            if time_diff > 0:
                bytes_diff = (
                    net_io.bytes_sent
                    + net_io.bytes_recv
                    - last_measurement["total_bytes"]
                )
                bandwidth_mbps = bytes_diff * 8 / (time_diff * 1024 * 1024)
        current_measurement = {
            "timestamp": time.time(),
            "bytes_sent": net_io.bytes_sent,
            "bytes_recv": net_io.bytes_recv,
            "total_bytes": net_io.bytes_sent + net_io.bytes_recv,
            "bandwidth_mbps": bandwidth_mbps,
        }
        self.bandwidth_history.append(current_measurement)
        return {
            "bytes_sent": net_io.bytes_sent,
            "bytes_recv": net_io.bytes_recv,
            "packets_sent": net_io.packets_sent,
            "packets_recv": net_io.packets_recv,
            "current_bandwidth_mbps": bandwidth_mbps,
            "average_bandwidth_mbps": self._calculate_average_bandwidth(),
        }
    def _calculate_average_bandwidth(self) -> float:
        if len(self.bandwidth_history) < 2:
            return 0.0
        bandwidths = [
            m["bandwidth_mbps"]
            for m in self.bandwidth_history
            if m["bandwidth_mbps"] > 0
        ]
        return sum(bandwidths) / len(bandwidths) if bandwidths else 0.0
    def optimize_network_usage(
        self, max_bandwidth_mbps: float = 50.0
    ) -> Dict[str, Any]:
        metrics = self.get_network_metrics()
        current_bandwidth = metrics["average_bandwidth_mbps"]
        optimization_result = {
            "current_bandwidth_mbps": current_bandwidth,
            "max_bandwidth_mbps": max_bandwidth_mbps,
            "previous_quality": self.current_quality,
            "actions_taken": [],
        }
        if current_bandwidth > max_bandwidth_mbps * 0.8:
            if self.current_quality == "high":
                self.current_quality = "medium"
                optimization_result["actions_taken"].append("reduced_quality_to_medium")
            elif self.current_quality == "medium":
                self.current_quality = "low"
                optimization_result["actions_taken"].append("reduced_quality_to_low")
        elif current_bandwidth < max_bandwidth_mbps * 0.4:
            if self.current_quality == "low":
                self.current_quality = "medium"
                optimization_result["actions_taken"].append(
                    "increased_quality_to_medium"
                )
            elif self.current_quality == "medium":
                self.current_quality = "high"
                optimization_result["actions_taken"].append("increased_quality_to_high")
        optimization_result["new_quality"] = self.current_quality
        optimization_result["quality_settings"] = self.quality_levels[
            self.current_quality
        ]
        return optimization_result
    def get_recommended_settings(self) -> Dict[str, Any]:
        return self.quality_levels[self.current_quality]
class GracefulDegradationManager:
    def __init__(self, config: OptimizationConfig, logger=None):
        self.config = config
        self.logger = logger or logging.getLogger(__name__)
        self.degradation_state = {
            "frame_dropping_enabled": False,
            "quality_reduced": False,
            "preview_disabled": False,
            "non_essential_disabled": False
        }
        self.frame_drop_count = 0
        self.total_frames = 0
        self.callbacks = {
            "frame_drop": [],
            "quality_reduce": [],
            "preview_disable": [],
            "feature_disable": []
        }
    def register_callback(self, event_type: str, callback: Callable):
        if event_type in self.callbacks:
            self.callbacks[event_type].append(callback)
    def should_drop_frame(self, cpu_percent: float, memory_percent: float) -> bool:
        if not self.config.enable_graceful_degradation:
            return False
        should_drop = (
            cpu_percent > self.config.frame_drop_cpu_threshold or
            memory_percent > self.config.frame_drop_memory_threshold
        )
        if should_drop:
            self.frame_drop_count += 1
            if not self.degradation_state["frame_dropping_enabled"]:
                self.degradation_state["frame_dropping_enabled"] = True
                self.logger.warning(f"Frame dropping enabled - CPU: {cpu_percent:.1f}%, Memory: {memory_percent:.1f}%")
                self._trigger_callbacks("frame_drop", True)
        else:
            if self.degradation_state["frame_dropping_enabled"]:
                self.degradation_state["frame_dropping_enabled"] = False
                self.logger.info("Frame dropping disabled - system load normalised")
                self._trigger_callbacks("frame_drop", False)
        self.total_frames += 1
        return should_drop
    def should_reduce_quality(self, cpu_percent: float, memory_percent: float) -> bool:
        should_reduce = (
            cpu_percent > self.config.quality_reduction_threshold or
            memory_percent > self.config.quality_reduction_threshold
        )
        if should_reduce and not self.degradation_state["quality_reduced"]:
            self.degradation_state["quality_reduced"] = True
            self.logger.warning("Reducing quality due to system load")
            self._trigger_callbacks("quality_reduce", True)
        elif not should_reduce and self.degradation_state["quality_reduced"]:
            self.degradation_state["quality_reduced"] = False
            self.logger.info("Restoring quality - system load normalised")
            self._trigger_callbacks("quality_reduce", False)
        return should_reduce
    def should_disable_preview(self, cpu_percent: float, memory_percent: float) -> bool:
        should_disable = (
            cpu_percent > self.config.preview_disable_threshold or
            memory_percent > self.config.preview_disable_threshold
        )
        if should_disable and not self.degradation_state["preview_disabled"]:
            self.degradation_state["preview_disabled"] = True
            self.logger.warning("Disabling preview due to critical system load")
            self._trigger_callbacks("preview_disable", True)
        elif not should_disable and self.degradation_state["preview_disabled"]:
            self.degradation_state["preview_disabled"] = False
            self.logger.info("Re-enabling preview - system load normalised")
            self._trigger_callbacks("preview_disable", False)
        return should_disable
    def _trigger_callbacks(self, event_type: str, enabled: bool):
        for callback in self.callbacks.get(event_type, []):
            try:
                callback(enabled)
            except Exception as e:
                self.logger.error(f"Error in degradation callback: {e}")
    def get_frame_drop_rate(self) -> float:
        if self.total_frames == 0:
            return 0.0
        return (self.frame_drop_count / self.total_frames) * 100
    def get_degradation_status(self) -> Dict[str, Any]:
        return {
            "state": self.degradation_state.copy(),
            "frame_drop_rate": self.get_frame_drop_rate(),
            "total_frames": self.total_frames,
            "dropped_frames": self.frame_drop_count
        }
class HardwareAccelerationManager:
    def __init__(self, config: OptimizationConfig, logger=None):
        self.config = config
        self.logger = logger or logging.getLogger(__name__)
        self.capabilities = {
            "opencv_gpu": False,
            "cuda_available": False,
            "opencl_available": False,
            "hardware_codecs": [],
            "gpu_devices": []
        }
        self._detect_capabilities()
    def _detect_capabilities(self):
        if not self.config.enable_hardware_acceleration:
            return
        try:
            import cv2
            self.capabilities["opencv_gpu"] = cv2.cuda.getCudaEnabledDeviceCount() > 0
            if self.capabilities["opencv_gpu"]:
                self.logger.info(f"OpenCV CUDA devices detected: {cv2.cuda.getCudaEnabledDeviceCount()}")
        except ImportError:
            logging.debug("OpenCV not available for GPU detection")
        try:
            import torch
            self.capabilities["cuda_available"] = torch.cuda.is_available()
            if self.capabilities["cuda_available"]:
                device_count = torch.cuda.device_count()
                self.capabilities["gpu_devices"] = [
                    torch.cuda.get_device_name(i) for i in range(device_count)
                ]
                self.logger.info(f"CUDA devices: {self.capabilities['gpu_devices']}")
        except ImportError:
            pass
        try:
            import pyopencl as cl
            platforms = cl.get_platforms()
            self.capabilities["opencl_available"] = len(platforms) > 0
            if self.capabilities["opencl_available"]:
                self.logger.info(f"OpenCL platforms detected: {len(platforms)}")
        except ImportError:
            pass
        self._detect_hardware_codecs()
    def _detect_hardware_codecs(self):
        try:
            import cv2
            fourcc_codes = ['H264', 'HEVC', 'VP8', 'VP9']
            available_codecs = []
            for codec in fourcc_codes:
                try:
                    fourcc = cv2.VideoWriter_fourcc(*codec)
                    available_codecs.append(codec)
                except ValueError:
                    logging.debug(f"Codec {codec} not supported")
            self.capabilities["hardware_codecs"] = available_codecs
            if available_codecs:
                self.logger.info(f"Hardware codecs available: {available_codecs}")
        except ImportError:
            logging.debug("Hardware codec detection not available")
    def get_optimal_processing_device(self) -> str:
        if self.config.prefer_gpu_processing:
            if self.capabilities["cuda_available"]:
                return "cuda"
            elif self.capabilities["opencl_available"]:
                return "opencl"
            elif self.capabilities["opencv_gpu"]:
                return "opencv_gpu"
        return "cpu"
    def create_optimized_video_writer(self, filename: str, fps: float, frame_size: tuple):
        try:
            import cv2
            if self.config.use_hardware_codecs and self.capabilities["hardware_codecs"]:
                for codec in ['H264', 'HEVC']:
                    if codec in self.capabilities["hardware_codecs"]:
                        try:
                            fourcc = cv2.VideoWriter_fourcc(*codec)
                            writer = cv2.VideoWriter(filename, fourcc, fps, frame_size)
                            if writer.isOpened():
                                self.logger.info(f"Using hardware codec: {codec}")
                                return writer
                        except Exception as e:
                            logging.debug(f"Failed to create writer with codec {codec}: {e}")
                            continue
            fourcc = cv2.VideoWriter_fourcc(*'mp4v')
            writer = cv2.VideoWriter(filename, fourcc, fps, frame_size)
            self.logger.info("Using software codec")
            return writer
        except Exception as e:
            self.logger.error(f"Failed to create video writer: {e}")
            return None
    def get_capabilities_report(self) -> Dict[str, Any]:
        return {
            "acceleration_enabled": self.config.enable_hardware_acceleration,
            "optimal_device": self.get_optimal_processing_device(),
            "capabilities": self.capabilities.copy(),
            "recommendations": self._get_acceleration_recommendations()
        }
    def _get_acceleration_recommendations(self) -> List[str]:
        recommendations = []
        if not self.capabilities["cuda_available"] and not self.capabilities["opencl_available"]:
            recommendations.append(
                "No GPU acceleration detected. Consider using CUDA or OpenCL for better performance."
            )
        if not self.capabilities["hardware_codecs"]:
            recommendations.append(
                "No hardware video codecs detected. Video encoding may be CPU intensive."
            )
        if not self.capabilities["opencv_gpu"]:
            recommendations.append(
                "OpenCV compiled without GPU support. Consider rebuilding with CUDA/OpenCL support."
            )
        if not recommendations:
            recommendations.append("Hardware acceleration capabilities look good!")
        return recommendations
class ProfilingIntegrationManager:
    def __init__(self, config: OptimizationConfig, logger=None):
        self.config = config
        self.logger = logger or logging.getLogger(__name__)
        self.profilers = {
            "cprofile": None,
            "pyinstrument": None,
            "memory_profiler": None
        }
        self.hotspots = []
        self._setup_profilers()
    def _setup_profilers(self):
        if not self.config.enable_profiling_integration:
            return
        try:
            import cProfile
            self.profilers["cprofile"] = cProfile.Profile()
            self.logger.info("cProfile integration enabled")
        except ImportError:
            pass
        try:
            from pyinstrument import Profiler
            self.profilers["pyinstrument"] = Profiler()
            self.logger.info("PyInstrument integration enabled")
        except ImportError:
            pass
        try:
            import memory_profiler
            self.profilers["memory_profiler"] = True
            self.logger.info("Memory profiler integration available")
        except ImportError:
            pass
    def start_profiling(self, profiler_type: str = "cprofile"):
        if profiler_type not in self.profilers or not self.profilers[profiler_type]:
            self.logger.warning(f"Profiler {profiler_type} not available")
            return False
        try:
            if profiler_type == "cprofile":
                self.profilers["cprofile"].enable()
            elif profiler_type == "pyinstrument":
                self.profilers["pyinstrument"].start()
            self.logger.info(f"Started {profiler_type} profiling")
            return True
        except Exception as e:
            self.logger.error(f"Failed to start profiling: {e}")
            return False
    def stop_profiling(self, profiler_type: str = "cprofile", save_results: bool = True):
        if profiler_type not in self.profilers or not self.profilers[profiler_type]:
            return None
        try:
            results = None
            if profiler_type == "cprofile":
                self.profilers["cprofile"].disable()
                if save_results:
                    results = self._save_cprofile_results()
            elif profiler_type == "pyinstrument":
                self.profilers["pyinstrument"].stop()
                if save_results:
                    results = self._save_pyinstrument_results()
            self.logger.info(f"Stopped {profiler_type} profiling")
            return results
        except Exception as e:
            self.logger.error(f"Failed to stop profiling: {e}")
            return None
    def _save_cprofile_results(self) -> str:
        import pstats
        import io
        stats_file = f"profile_results_{int(time.time())}.prof"
        self.profilers["cprofile"].dump_stats(stats_file)
        s = io.StringIO()
        ps = pstats.Stats(self.profilers["cprofile"], stream=s)
        ps.sort_stats('cumulative').print_stats(20)
        report_file = f"profile_report_{int(time.time())}.txt"
        with open(report_file, 'w') as f:
            f.write(s.getvalue())
        self._extract_hotspots_from_cprofile(ps)
        return report_file
    def _save_pyinstrument_results(self) -> str:
        session = self.profilers["pyinstrument"].last_session
        html_file = f"pyinstrument_report_{int(time.time())}.html"
        with open(html_file, 'w') as f:
            f.write(session.output_html())
        text_file = f"pyinstrument_report_{int(time.time())}.txt"
        with open(text_file, 'w') as f:
            f.write(session.output_text())
        return html_file
    def _extract_hotspots_from_cprofile(self, stats):
        stats.sort_stats('cumulative')
        hotspots = []
        for func, (cc, nc, tt, ct, callers) in stats.stats.items():
            if ct > 0.1:
                hotspots.append({
                    "function": f"{func[0]}:{func[1]}({func[2]})",
                    "cumulative_time": ct,
                    "total_time": tt,
                    "call_count": cc,
                    "per_call_time": ct / cc if cc > 0 else 0
                })
        hotspots.sort(key=lambda x: x["cumulative_time"], reverse=True)
        self.hotspots = hotspots[:10]
        self.logger.info(f"Identified {len(self.hotspots)} performance hotspots")
    def get_optimization_recommendations(self) -> List[str]:
        recommendations = []
        for hotspot in self.hotspots:
            func_name = hotspot["function"]
            cum_time = hotspot["cumulative_time"]
            per_call = hotspot["per_call_time"]
            if "numpy" in func_name or "cv2" in func_name:
                recommendations.append(
                    f"Consider GPU acceleration for {func_name} (takes {cum_time:.2f}s total)"
                )
            elif per_call > 0.01:
                recommendations.append(
                    f"Optimise {func_name} - high per-call time ({per_call:.4f}s per call)"
                )
            elif hotspot["call_count"] > 10000:
                recommendations.append(
                    f"Reduce call frequency for {func_name} - called {hotspot['call_count']} times"
                )
        return recommendations
    def get_profiling_status(self) -> Dict[str, Any]:
        return {
            "available_profilers": [k for k, v in self.profilers.items() if v],
            "hotspots_count": len(self.hotspots),
            "recent_hotspots": self.hotspots[:5],
            "recommendations": self.get_optimization_recommendations()
        }
class PerformanceMonitor:
    def __init__(self, config: OptimizationConfig, logger=None):
        self.config = config
        self.logger = logger or logging.getLogger(__name__)
        self.metrics_history = deque(
            maxlen=int(
                config.history_retention_minutes
                * 60
                / config.monitoring_interval_seconds
            )
        )
        self.alert_callbacks = []
        self.violation_counts = defaultdict(int)
        self.is_monitoring = False
        self.monitor_thread = None
        self.memory_optimizer = MemoryOptimizer(logger)
        self.cpu_optimizer = CPUOptimizer(logger)
        self.network_optimizer = NetworkOptimizer(logger)
        self.degradation_manager = GracefulDegradationManager(config, logger)
        self.hardware_manager = HardwareAccelerationManager(config, logger)
        self.profiling_manager = ProfilingIntegrationManager(config, logger)
    def start_monitoring(self):
        if self.is_monitoring:
            return
        self.is_monitoring = True
        self.monitor_thread = threading.Thread(
            target=self._monitoring_loop, daemon=True
        )
        self.monitor_thread.start()
        if self.config.enable_memory_optimization:
            self.memory_optimizer.start_memory_tracking()
        self.logger.info("Performance monitoring started")
    def stop_monitoring(self):
        self.is_monitoring = False
        if self.monitor_thread:
            self.monitor_thread.join(timeout=5.0)
        if self.config.enable_memory_optimization:
            final_stats = self.memory_optimizer.stop_memory_tracking()
            if final_stats:
                self.logger.info(f"Final memory stats: {final_stats}")
        self.cpu_optimizer.cleanup_thread_pools()
        self.logger.info("Performance monitoring stopped")
    def _monitoring_loop(self):
        while self.is_monitoring:
            try:
                metrics = self._collect_metrics()
                self.metrics_history.append(metrics)
                self._check_violations(metrics)
                if self.config.enable_automatic_cleanup:
                    self._perform_automatic_cleanup()
                time.sleep(self.config.monitoring_interval_seconds)
            except Exception as e:
                self.logger.error(f"Error in monitoring loop: {e}")
                time.sleep(self.config.monitoring_interval_seconds)
    def _collect_metrics(self) -> PerformanceMetrics:
        process = psutil.Process()
        net_io = psutil.net_io_counters()
        disk_io = psutil.disk_io_counters()
        return PerformanceMetrics(
            timestamp=time.time(),
            cpu_percent=process.cpu_percent(),
            memory_mb=process.memory_info().rss / 1024 / 1024,
            memory_percent=process.memory_percent(),
            network_bytes_sent=net_io.bytes_sent,
            network_bytes_recv=net_io.bytes_recv,
            disk_io_read=disk_io.read_bytes if disk_io else 0,
            disk_io_write=disk_io.write_bytes if disk_io else 0,
            thread_count=process.num_threads(),
            process_count=len(psutil.pids()),
        )
    def _check_violations(self, metrics: PerformanceMetrics):
        violations = []
        if self.config.enable_graceful_degradation:
            should_drop = self.degradation_manager.should_drop_frame(
                metrics.cpu_percent, metrics.memory_percent)
            should_reduce_quality = self.degradation_manager.should_reduce_quality(
                metrics.cpu_percent, metrics.memory_percent)
            should_disable_preview = self.degradation_manager.should_disable_preview(
                metrics.cpu_percent, metrics.memory_percent)
        if (
            metrics.memory_mb
            > self.config.max_memory_mb * self.config.memory_warning_threshold
        ):
            violations.append("memory")
            self.violation_counts["memory"] += 1
        else:
            self.violation_counts["memory"] = 0
        if (
            metrics.cpu_percent
            > self.config.max_cpu_percent * self.config.cpu_warning_threshold
        ):
            violations.append("cpu")
            self.violation_counts["cpu"] += 1
        else:
            self.violation_counts["cpu"] = 0
        for violation_type in violations:
            if (
                self.violation_counts[violation_type]
                >= self.config.alert_threshold_violations
            ):
                self._trigger_optimization(violation_type, metrics)
                self.violation_counts[violation_type] = 0
    def _trigger_optimization(self, violation_type: str, metrics: PerformanceMetrics):
        self.logger.warning(f"Performance violation detected: {violation_type}")
        if violation_type == "memory" and self.config.enable_memory_optimization:
            result = self.memory_optimizer.optimize_memory_usage()
            self.logger.info(f"Memory optimisation result: {result}")
        elif violation_type == "cpu" and self.config.enable_cpu_optimization:
            result = self.cpu_optimizer.optimize_cpu_usage()
            self.logger.info(f"CPU optimisation result: {result}")
        for callback in self.alert_callbacks:
            try:
                callback(violation_type, metrics)
            except Exception as e:
                self.logger.error(f"Error in alert callback: {e}")
    def _perform_automatic_cleanup(self):
        current_time = time.time()
        if not hasattr(self, "_last_cleanup_time"):
            self._last_cleanup_time = current_time
            return
        if (
            current_time - self._last_cleanup_time
            >= self.config.cleanup_interval_seconds
        ):
            self.logger.debug("Performing automatic cleanup")
            collected = gc.collect()
            if collected > 0:
                self.logger.debug(f"Garbage collection freed {collected} objects")
            self._last_cleanup_time = current_time
    def get_performance_summary(self) -> Dict[str, Any]:
        if not self.metrics_history:
            return {"error": "No metrics available"}
        recent_metrics = list(self.metrics_history)[-60:]
        summary = {
            "monitoring_duration_minutes": len(self.metrics_history)
            * self.config.monitoring_interval_seconds
            / 60,
            "current_metrics": recent_metrics[-1].__dict__ if recent_metrics else None,
            "averages": {
                "cpu_percent": sum(m.cpu_percent for m in recent_metrics)
                / len(recent_metrics),
                "memory_mb": sum(m.memory_mb for m in recent_metrics)
                / len(recent_metrics),
                "memory_percent": sum(m.memory_percent for m in recent_metrics)
                / len(recent_metrics),
            },
            "peaks": {
                "cpu_percent": max(m.cpu_percent for m in recent_metrics),
                "memory_mb": max(m.memory_mb for m in recent_metrics),
                "memory_percent": max(m.memory_percent for m in recent_metrics),
            },
            "violation_counts": dict(self.violation_counts),
            "optimization_recommendations": self._generate_recommendations(),
            "graceful_degradation": self.degradation_manager.get_degradation_status(),
            "hardware_acceleration": self.hardware_manager.get_capabilities_report(),
            "profiling_status": self.profiling_manager.get_profiling_status(),
        }
        return summary
    def _generate_recommendations(self) -> List[str]:
        recommendations = []
        if not self.metrics_history:
            return recommendations
        recent_metrics = list(self.metrics_history)[-10:]
        avg_memory = sum(m.memory_mb for m in recent_metrics) / len(recent_metrics)
        avg_cpu = sum(m.cpu_percent for m in recent_metrics) / len(recent_metrics)
        if avg_memory > self.config.max_memory_mb * 0.7:
            recommendations.append(
                "Consider reducing buffer sizes or implementing more aggressive cleanup"
            )
        if avg_cpu > self.config.max_cpu_percent * 0.7:
            recommendations.append(
                "Consider reducing processing frequency or optimising algorithms"
            )
        if len(recent_metrics) > 5:
            memory_trend = recent_metrics[-1].memory_mb - recent_metrics[-5].memory_mb
            if memory_trend > 50:
                recommendations.append(
                    "Memory usage is trending upward - check for memory leaks"
                )
        return recommendations
    def add_alert_callback(self, callback: Callable[[str, PerformanceMetrics], None]):
        self.alert_callbacks.append(callback)
    def export_metrics(self, filename: str):
        try:
            metrics_data = []
            for metric in self.metrics_history:
                metrics_data.append(metric.__dict__)
            with open(filename, "w") as f:
                json.dump(
                    {
                        "export_timestamp": datetime.now().isoformat(),
                        "config": self.config.__dict__,
                        "metrics": metrics_data,
                    },
                    f,
                    indent=2,
                )
            self.logger.info(f"Metrics exported to {filename}")
        except Exception as e:
            self.logger.error(f"Error exporting metrics: {e}")
class PerformanceManager:
    def __init__(self, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.monitor: Optional[PerformanceMonitor] = None
        self.config = OptimizationConfig()
    def initialize(self, config: Optional[OptimizationConfig] = None) -> bool:
        try:
            if config:
                self.config = config
            self.monitor = PerformanceMonitor(self.config, self.logger)
            self.monitor.add_alert_callback(self._on_performance_alert)
            return True
        except Exception as e:
            self.logger.error(f"Failed to initialize performance manager: {e}")
            return False
    def start(self) -> bool:
        if self.monitor:
            self.monitor.start_monitoring()
            return True
        return False
    def stop(self) -> None:
        if self.monitor:
            self.monitor.stop_monitoring()
    def get_status(self) -> Optional[Dict[str, Any]]:
        if self.monitor:
            return self.monitor.get_performance_summary()
        return None
    def optimize_now(self) -> Dict[str, Any]:
        if not self.monitor:
            return {"error": "Monitor not initialized"}
        results = {}
        if self.config.enable_memory_optimization:
            results["memory"] = self.monitor.memory_optimizer.optimize_memory_usage()
        if self.config.enable_cpu_optimization:
            results["cpu"] = self.monitor.cpu_optimizer.optimize_cpu_usage()
        if self.config.enable_network_optimization:
            results["network"] = self.monitor.network_optimizer.optimize_network_usage()
        return results
    def get_degradation_manager(self):
        return self.monitor.degradation_manager if self.monitor else None
    def get_hardware_manager(self):
        return self.monitor.hardware_manager if self.monitor else None
    def get_profiling_manager(self):
        return self.monitor.profiling_manager if self.monitor else None
    def start_profiling(self, profiler_type: str = "cprofile") -> bool:
        if self.monitor and self.monitor.profiling_manager:
            return self.monitor.profiling_manager.start_profiling(profiler_type)
        return False
    def stop_profiling(self, profiler_type: str = "cprofile", save_results: bool = True):
        if self.monitor and self.monitor.profiling_manager:
            return self.monitor.profiling_manager.stop_profiling(profiler_type, save_results)
        return None
    def should_drop_frame(self) -> bool:
        if not self.monitor or not self.monitor.metrics_history:
            return False
        latest_metrics = self.monitor.metrics_history[-1]
        return self.monitor.degradation_manager.should_drop_frame(
            latest_metrics.cpu_percent, latest_metrics.memory_percent)
    def get_recommended_quality_settings(self) -> Dict[str, Any]:
        if self.monitor:
            return self.monitor.network_optimizer.get_recommended_settings()
        return {}
    async def run_endurance_test(self, duration_hours: float = 1.0) -> Dict[str, Any]:
        from .endurance_test_suite import run_endurance_test
        return await run_endurance_test(duration_hours)
    def _on_performance_alert(self, violation_type: str, metrics: PerformanceMetrics):
        self.logger.warning(f"Performance alert: {violation_type} violation detected")
        self.logger.warning(
            f"Current usage - CPU: {metrics.cpu_percent:.1f}%, Memory: {metrics.memory_mb:.1f}MB"
        )
if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
    config = OptimizationConfig(
        max_memory_mb=1024, max_cpu_percent=70, monitoring_interval_seconds=2.0
    )
    manager = PerformanceManager()
    try:
        if manager.initialize(config):
            logging.info("Performance manager initialized successfully")
            if manager.start():
                logging.info("Performance monitoring started")
                logging.info("Monitoring performance... Press Ctrl+C to stop")
                while True:
                    time.sleep(10)
                    status = manager.get_status()
                    if status:
                        current = status.get("current_metrics", {})
                        logging.info(
                            f"CPU: {current.get('cpu_percent', 0):.1f}%, Memory: {current.get('memory_mb', 0):.1f}MB"
                        )
                        if current.get("memory_mb", 0) > 500:
                            logging.info("Triggering optimisation...")
                            result = manager.optimize_now()
                            logging.info(f"Optimisation result: {result}")
    except KeyboardInterrupt:
        logging.info("Shutting down performance manager...")
    finally:
        manager.stop()
        logging.info("Performance manager stopped")