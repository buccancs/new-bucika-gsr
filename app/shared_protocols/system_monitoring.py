"""
Unified system monitoring and performance optimization.

This module consolidates system monitoring, performance optimization, and 
resource management functionality to eliminate duplication between different
parts of the application.
"""

import gc
import os
import platform
import psutil
import threading
import time
import weakref
from collections import defaultdict, deque
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Callable, Dict, List, Optional

try:
    from ..utils.logging_config import get_logger
    logger = get_logger(__name__)
except (ImportError, ValueError):
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)


@dataclass
class SystemInfo:
    """Comprehensive system information."""
    platform: str
    platform_release: str
    platform_version: str
    architecture: str
    hostname: str
    processor: str
    python_version: str
    cpu_count: int
    cpu_count_logical: int
    memory_total: int
    boot_time: float
    opencv_available: bool = False
    
    @classmethod
    def get_current(cls) -> "SystemInfo":
        """Get current system information."""
        try:
            import cv2
            opencv_available = True
        except ImportError:
            opencv_available = False
        
        return cls(
            platform=platform.system(),
            platform_release=platform.release(),
            platform_version=platform.version(),
            architecture=platform.machine(),
            hostname=platform.node(),
            processor=platform.processor(),
            python_version=platform.python_version(),
            cpu_count=psutil.cpu_count(),
            cpu_count_logical=psutil.cpu_count(logical=True),
            memory_total=psutil.virtual_memory().total,
            boot_time=psutil.boot_time(),
            opencv_available=opencv_available
        )


@dataclass
class PerformanceMetrics:
    """Performance metrics snapshot."""
    timestamp: float
    cpu_percent: float
    memory_mb: float
    memory_percent: float
    network_bytes_sent: int
    network_bytes_recv: int
    disk_io_read: int
    disk_io_write: int
    thread_count: int
    process_count: int
    gpu_usage: Optional[float] = None
    gpu_memory_mb: Optional[float] = None


@dataclass
class ResourceLimits:
    """Resource usage limits and thresholds."""
    max_memory_mb: float = 2048.0
    max_cpu_percent: float = 80.0
    max_network_mbps: float = 100.0
    max_threads: int = 16
    memory_warning_threshold: float = 0.8
    cpu_warning_threshold: float = 0.7
    cleanup_interval_seconds: int = 300


class UnifiedSystemMonitor:
    """
    Unified system monitoring and optimization.
    
    Consolidates system monitoring, performance optimization, and resource
    management functionality from multiple modules.
    """
    
    def __init__(self, limits: ResourceLimits = None):
        self.limits = limits or ResourceLimits()
        self.monitoring = False
        self.monitor_thread = None
        self.system_info = SystemInfo.get_current()
        self.metrics_history = deque(maxlen=3600)  # 1 hour at 1 second intervals
        self.performance_callbacks = []
        self.resource_warnings = []
        self._lock = threading.Lock()
        
        # Performance optimization state
        self.optimization_enabled = True
        self.auto_cleanup_enabled = True
        self.last_cleanup_time = time.time()
        
        logger.info("Unified system monitor initialized")
        logger.info(f"System: {self.system_info.platform} {self.system_info.platform_release}")
        logger.info(f"CPU: {self.system_info.cpu_count} cores, Memory: {self.system_info.memory_total // (1024**3)}GB")
    
    def start_monitoring(self, interval: float = 1.0) -> bool:
        """Start continuous system monitoring."""
        if self.monitoring:
            logger.warning("System monitoring already running")
            return True
        
        try:
            self.monitoring = True
            self.monitor_thread = threading.Thread(
                target=self._monitoring_loop,
                args=(interval,),
                daemon=True,
                name="SystemMonitor"
            )
            self.monitor_thread.start()
            logger.info(f"System monitoring started with {interval}s interval")
            return True
        except Exception as e:
            logger.error(f"Failed to start system monitoring: {e}")
            self.monitoring = False
            return False
    
    def stop_monitoring(self):
        """Stop system monitoring."""
        if not self.monitoring:
            return
        
        self.monitoring = False
        if self.monitor_thread and self.monitor_thread.is_alive():
            self.monitor_thread.join(timeout=5.0)
        logger.info("System monitoring stopped")
    
    def _monitoring_loop(self, interval: float):
        """Main monitoring loop."""
        logger.debug("System monitoring loop started")
        
        while self.monitoring:
            try:
                metrics = self._collect_metrics()
                
                with self._lock:
                    self.metrics_history.append(metrics)
                
                # Check for resource issues
                self._check_resource_thresholds(metrics)
                
                # Perform automatic cleanup if needed
                if self.auto_cleanup_enabled:
                    self._auto_cleanup_check()
                
                # Notify callbacks
                for callback in self.performance_callbacks:
                    try:
                        callback(metrics)
                    except Exception as e:
                        logger.error(f"Error in performance callback: {e}")
                
                time.sleep(interval)
                
            except Exception as e:
                logger.error(f"Error in monitoring loop: {e}")
                time.sleep(interval)
        
        logger.debug("System monitoring loop ended")
    
    def _collect_metrics(self) -> PerformanceMetrics:
        """Collect current performance metrics."""
        try:
            # CPU and memory
            cpu_percent = psutil.cpu_percent(interval=None)
            memory = psutil.virtual_memory()
            
            # Network I/O
            net_io = psutil.net_io_counters()
            
            # Disk I/O
            disk_io = psutil.disk_io_counters()
            
            # Process info
            process = psutil.Process()
            
            return PerformanceMetrics(
                timestamp=time.time(),
                cpu_percent=cpu_percent,
                memory_mb=memory.used / (1024 * 1024),
                memory_percent=memory.percent,
                network_bytes_sent=net_io.bytes_sent if net_io else 0,
                network_bytes_recv=net_io.bytes_recv if net_io else 0,
                disk_io_read=disk_io.read_bytes if disk_io else 0,
                disk_io_write=disk_io.write_bytes if disk_io else 0,
                thread_count=process.num_threads(),
                process_count=len(psutil.pids())
            )
        
        except Exception as e:
            logger.error(f"Error collecting metrics: {e}")
            return PerformanceMetrics(
                timestamp=time.time(),
                cpu_percent=0.0,
                memory_mb=0.0,
                memory_percent=0.0,
                network_bytes_sent=0,
                network_bytes_recv=0,
                disk_io_read=0,
                disk_io_write=0,
                thread_count=0,
                process_count=0
            )
    
    def _check_resource_thresholds(self, metrics: PerformanceMetrics):
        """Check if resource usage exceeds thresholds."""
        warnings = []
        
        # Memory check
        if metrics.memory_percent > self.limits.memory_warning_threshold * 100:
            warnings.append(f"High memory usage: {metrics.memory_percent:.1f}%")
        
        # CPU check
        if metrics.cpu_percent > self.limits.cpu_warning_threshold * 100:
            warnings.append(f"High CPU usage: {metrics.cpu_percent:.1f}%")
        
        # Thread count check
        if metrics.thread_count > self.limits.max_threads:
            warnings.append(f"High thread count: {metrics.thread_count}")
        
        if warnings:
            for warning in warnings:
                logger.warning(f"Resource threshold exceeded: {warning}")
                self.resource_warnings.append({
                    "timestamp": metrics.timestamp,
                    "warning": warning,
                    "metrics": metrics
                })
    
    def _auto_cleanup_check(self):
        """Perform automatic cleanup if needed."""
        current_time = time.time()
        if current_time - self.last_cleanup_time > self.limits.cleanup_interval_seconds:
            self.perform_cleanup()
            self.last_cleanup_time = current_time
    
    def perform_cleanup(self):
        """Perform system cleanup to free resources."""
        try:
            logger.info("Performing automatic cleanup")
            
            # Force garbage collection
            before_objects = len(gc.get_objects())
            collected = gc.collect()
            after_objects = len(gc.get_objects())
            
            # Clear old metrics
            if len(self.metrics_history) > 1800:  # Keep only 30 minutes
                with self._lock:
                    # Keep recent half
                    recent_metrics = list(self.metrics_history)[-900:]
                    self.metrics_history.clear()
                    self.metrics_history.extend(recent_metrics)
            
            # Clear old warnings
            current_time = time.time()
            self.resource_warnings = [
                w for w in self.resource_warnings 
                if current_time - w["timestamp"] < 3600  # Keep 1 hour
            ]
            
            logger.info(f"Cleanup completed: collected {collected} objects, "
                       f"freed {before_objects - after_objects} object references")
        
        except Exception as e:
            logger.error(f"Error during cleanup: {e}")
    
    def get_current_metrics(self) -> Optional[PerformanceMetrics]:
        """Get the most recent performance metrics."""
        with self._lock:
            return self.metrics_history[-1] if self.metrics_history else None
    
    def get_metrics_history(self, duration_seconds: int = 300) -> List[PerformanceMetrics]:
        """Get performance metrics for the specified duration."""
        current_time = time.time()
        cutoff_time = current_time - duration_seconds
        
        with self._lock:
            return [
                m for m in self.metrics_history 
                if m.timestamp >= cutoff_time
            ]
    
    def get_system_health_report(self) -> Dict[str, Any]:
        """Generate a comprehensive system health report."""
        current_metrics = self.get_current_metrics()
        recent_metrics = self.get_metrics_history(300)  # 5 minutes
        
        if not current_metrics or not recent_metrics:
            return {"status": "insufficient_data"}
        
        # Calculate averages
        avg_cpu = sum(m.cpu_percent for m in recent_metrics) / len(recent_metrics)
        avg_memory = sum(m.memory_percent for m in recent_metrics) / len(recent_metrics)
        
        # Determine health status
        if avg_cpu > 80 or avg_memory > 85:
            health_status = "critical"
        elif avg_cpu > 60 or avg_memory > 70:
            health_status = "warning"
        else:
            health_status = "good"
        
        return {
            "status": health_status,
            "timestamp": current_metrics.timestamp,
            "current": {
                "cpu_percent": current_metrics.cpu_percent,
                "memory_percent": current_metrics.memory_percent,
                "memory_mb": current_metrics.memory_mb,
                "thread_count": current_metrics.thread_count
            },
            "averages_5min": {
                "cpu_percent": avg_cpu,
                "memory_percent": avg_memory
            },
            "system_info": {
                "platform": self.system_info.platform,
                "cpu_count": self.system_info.cpu_count,
                "memory_total_gb": self.system_info.memory_total // (1024**3)
            },
            "recent_warnings": self.resource_warnings[-10:],  # Last 10 warnings
            "monitoring_active": self.monitoring
        }
    
    def add_performance_callback(self, callback: Callable[[PerformanceMetrics], None]):
        """Add a callback to be notified of performance metrics."""
        self.performance_callbacks.append(callback)
    
    def optimize_for_recording(self):
        """Optimize system settings for data recording."""
        try:
            logger.info("Optimizing system for recording")
            
            # Increase process priority
            current_process = psutil.Process()
            if hasattr(psutil, "HIGH_PRIORITY_CLASS"):
                current_process.nice(psutil.HIGH_PRIORITY_CLASS)
            else:
                current_process.nice(-5)  # Unix nice value
            
            # Force garbage collection
            gc.collect()
            
            # Set aggressive cleanup
            self.limits.cleanup_interval_seconds = 60  # More frequent cleanup
            
            logger.info("System optimization for recording completed")
            
        except Exception as e:
            logger.error(f"Error optimizing system for recording: {e}")
    
    def optimize_for_idle(self):
        """Optimize system settings for idle state."""
        try:
            logger.info("Optimizing system for idle state")
            
            # Reset process priority
            current_process = psutil.Process()
            current_process.nice(0)  # Normal priority
            
            # Perform cleanup
            self.perform_cleanup()
            
            # Reset cleanup interval
            self.limits.cleanup_interval_seconds = 300  # Normal interval
            
            logger.info("System optimization for idle completed")
            
        except Exception as e:
            logger.error(f"Error optimizing system for idle: {e}")


# Global instance for convenience
_global_monitor = None


def get_system_monitor() -> UnifiedSystemMonitor:
    """Get the global system monitor instance."""
    global _global_monitor
    if _global_monitor is None:
        _global_monitor = UnifiedSystemMonitor()
    return _global_monitor


def start_system_monitoring(interval: float = 1.0) -> bool:
    """Start global system monitoring."""
    return get_system_monitor().start_monitoring(interval)


def stop_system_monitoring():
    """Stop global system monitoring."""
    get_system_monitor().stop_monitoring()


def get_current_system_metrics() -> Optional[PerformanceMetrics]:
    """Get current system performance metrics."""
    return get_system_monitor().get_current_metrics()


def get_system_health_report() -> Dict[str, Any]:
    """Get comprehensive system health report."""
    return get_system_monitor().get_system_health_report()