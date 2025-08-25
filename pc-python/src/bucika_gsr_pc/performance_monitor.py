"""
Performance monitoring and optimization for the Bucika GSR PC Orchestrator
"""

import asyncio
import time
import psutil
import threading
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass, asdict
from loguru import logger


@dataclass
class PerformanceMetrics:
    """Performance metrics snapshot"""
    timestamp: datetime
    cpu_percent: float
    memory_mb: float
    memory_percent: float
    network_sent_mb: float
    network_recv_mb: float
    active_sessions: int
    connected_devices: int
    messages_per_second: float
    avg_response_time_ms: float
    error_rate_percent: float


class PerformanceMonitor:
    """Real-time performance monitoring and optimization"""
    
    def __init__(self, collection_interval: float = 5.0):
        self.collection_interval = collection_interval
        self.metrics_history: List[PerformanceMetrics] = []
        self.max_history_size = 720  # 1 hour at 5-second intervals
        
        self.is_monitoring = False
        self.monitor_task: Optional[asyncio.Task] = None
        
        # Performance counters
        self.message_count = 0
        self.response_times: List[float] = []
        self.error_count = 0
        self.last_reset = datetime.now()
        
        # Process monitoring
        self.process = psutil.Process()
        self.initial_network = psutil.net_io_counters()
        
        # Performance thresholds for alerts
        self.cpu_threshold = 80.0  # %
        self.memory_threshold = 80.0  # %
        self.response_time_threshold = 100.0  # ms
        self.error_rate_threshold = 5.0  # %
        
        # Lock for thread-safe access
        self._lock = threading.Lock()
    
    async def start(self):
        """Start performance monitoring"""
        if self.is_monitoring:
            return
        
        self.is_monitoring = True
        self.monitor_task = asyncio.create_task(self._monitoring_loop())
        logger.info("Performance monitoring started")
    
    async def stop(self):
        """Stop performance monitoring"""
        if not self.is_monitoring:
            return
        
        self.is_monitoring = False
        if self.monitor_task:
            self.monitor_task.cancel()
            try:
                await self.monitor_task
            except asyncio.CancelledError:
                pass
        
        logger.info("Performance monitoring stopped")
    
    async def _monitoring_loop(self):
        """Main monitoring loop"""
        try:
            while self.is_monitoring:
                metrics = self._collect_metrics()
                self._add_metrics(metrics)
                self._check_alerts(metrics)
                
                await asyncio.sleep(self.collection_interval)
        except asyncio.CancelledError:
            pass
        except Exception as e:
            logger.error(f"Performance monitoring error: {e}")
    
    def _collect_metrics(self) -> PerformanceMetrics:
        """Collect current performance metrics"""
        with self._lock:
            # CPU and memory
            cpu_percent = self.process.cpu_percent()
            memory_info = self.process.memory_info()
            memory_mb = memory_info.rss / (1024 * 1024)
            memory_percent = self.process.memory_percent()
            
            # Network statistics  
            current_network = psutil.net_io_counters()
            network_sent_mb = (current_network.bytes_sent - self.initial_network.bytes_sent) / (1024 * 1024)
            network_recv_mb = (current_network.bytes_recv - self.initial_network.bytes_recv) / (1024 * 1024)
            
            # Performance statistics
            time_elapsed = (datetime.now() - self.last_reset).total_seconds()
            messages_per_second = self.message_count / max(time_elapsed, 1.0)
            
            avg_response_time_ms = 0.0
            if self.response_times:
                avg_response_time_ms = sum(self.response_times) / len(self.response_times) * 1000
            
            error_rate_percent = 0.0
            if self.message_count > 0:
                error_rate_percent = (self.error_count / self.message_count) * 100
            
            return PerformanceMetrics(
                timestamp=datetime.now(),
                cpu_percent=cpu_percent,
                memory_mb=memory_mb,
                memory_percent=memory_percent,
                network_sent_mb=network_sent_mb,
                network_recv_mb=network_recv_mb,
                active_sessions=0,  # Will be set by orchestrator
                connected_devices=0,  # Will be set by orchestrator
                messages_per_second=messages_per_second,
                avg_response_time_ms=avg_response_time_ms,
                error_rate_percent=error_rate_percent
            )
    
    def _add_metrics(self, metrics: PerformanceMetrics):
        """Add metrics to history"""
        with self._lock:
            self.metrics_history.append(metrics)
            
            # Limit history size
            if len(self.metrics_history) > self.max_history_size:
                self.metrics_history.pop(0)
    
    def _check_alerts(self, metrics: PerformanceMetrics):
        """Check for performance alerts"""
        alerts = []
        
        if metrics.cpu_percent > self.cpu_threshold:
            alerts.append(f"High CPU usage: {metrics.cpu_percent:.1f}%")
        
        if metrics.memory_percent > self.memory_threshold:
            alerts.append(f"High memory usage: {metrics.memory_percent:.1f}%")
        
        if metrics.avg_response_time_ms > self.response_time_threshold:
            alerts.append(f"High response time: {metrics.avg_response_time_ms:.1f}ms")
        
        if metrics.error_rate_percent > self.error_rate_threshold:
            alerts.append(f"High error rate: {metrics.error_rate_percent:.1f}%")
        
        for alert in alerts:
            logger.warning(f"Performance alert: {alert}")
    
    def record_message(self, response_time: float, error: bool = False):
        """Record a message for performance tracking"""
        with self._lock:
            self.message_count += 1
            self.response_times.append(response_time)
            if error:
                self.error_count += 1
            
            # Limit response time history
            if len(self.response_times) > 1000:
                self.response_times.pop(0)
    
    def update_session_count(self, active_sessions: int, connected_devices: int):
        """Update session and device counts"""
        if self.metrics_history:
            # Update the latest metrics
            with self._lock:
                if self.metrics_history:
                    self.metrics_history[-1].active_sessions = active_sessions
                    self.metrics_history[-1].connected_devices = connected_devices
    
    def reset_counters(self):
        """Reset performance counters"""
        with self._lock:
            self.message_count = 0
            self.response_times.clear()
            self.error_count = 0
            self.last_reset = datetime.now()
    
    def get_latest_metrics(self) -> Optional[PerformanceMetrics]:
        """Get the latest performance metrics"""
        with self._lock:
            return self.metrics_history[-1] if self.metrics_history else None
    
    def get_metrics_history(self, duration_minutes: int = 60) -> List[PerformanceMetrics]:
        """Get metrics history for specified duration"""
        cutoff_time = datetime.now() - timedelta(minutes=duration_minutes)
        
        with self._lock:
            return [
                metrics for metrics in self.metrics_history 
                if metrics.timestamp >= cutoff_time
            ]
    
    def get_performance_summary(self) -> Dict[str, float]:
        """Get performance summary statistics"""
        if not self.metrics_history:
            return {}
        
        with self._lock:
            recent_metrics = self.get_metrics_history(15)  # Last 15 minutes
            
            if not recent_metrics:
                return {}
            
            return {
                "avg_cpu_percent": sum(m.cpu_percent for m in recent_metrics) / len(recent_metrics),
                "max_cpu_percent": max(m.cpu_percent for m in recent_metrics),
                "avg_memory_mb": sum(m.memory_mb for m in recent_metrics) / len(recent_metrics),
                "max_memory_mb": max(m.memory_mb for m in recent_metrics),
                "avg_response_time_ms": sum(m.avg_response_time_ms for m in recent_metrics) / len(recent_metrics),
                "max_response_time_ms": max(m.avg_response_time_ms for m in recent_metrics),
                "avg_messages_per_second": sum(m.messages_per_second for m in recent_metrics) / len(recent_metrics),
                "total_network_sent_mb": recent_metrics[-1].network_sent_mb if recent_metrics else 0,
                "total_network_recv_mb": recent_metrics[-1].network_recv_mb if recent_metrics else 0,
                "avg_error_rate_percent": sum(m.error_rate_percent for m in recent_metrics) / len(recent_metrics)
            }
    
    def export_metrics_csv(self, filepath: str, duration_minutes: int = 60):
        """Export metrics to CSV file"""
        import csv
        
        metrics = self.get_metrics_history(duration_minutes)
        if not metrics:
            return
        
        with open(filepath, 'w', newline='') as csvfile:
            fieldnames = [
                'timestamp', 'cpu_percent', 'memory_mb', 'memory_percent',
                'network_sent_mb', 'network_recv_mb', 'active_sessions',
                'connected_devices', 'messages_per_second', 'avg_response_time_ms',
                'error_rate_percent'
            ]
            
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            
            for metric in metrics:
                row = asdict(metric)
                row['timestamp'] = metric.timestamp.isoformat()
                writer.writerow(row)
        
        logger.info(f"Performance metrics exported to {filepath}")


class PerformanceOptimizer:
    """Performance optimization utilities"""
    
    def __init__(self, monitor: PerformanceMonitor):
        self.monitor = monitor
        self.optimization_active = False
        
    def enable_optimizations(self):
        """Enable performance optimizations"""
        self.optimization_active = True
        self._optimize_garbage_collection()
        self._optimize_asyncio()
        logger.info("Performance optimizations enabled")
    
    def _optimize_garbage_collection(self):
        """Optimize Python garbage collection"""
        import gc
        
        # Tune garbage collection thresholds for better performance
        # Reduce frequency of generation 2 collections
        gc.set_threshold(700, 10, 10)
        
        # Disable automatic garbage collection during critical operations
        # This can be enabled/disabled dynamically based on load
        
    def _optimize_asyncio(self):
        """Optimize asyncio event loop"""
        try:
            # Use uvloop if available for better performance
            import uvloop
            uvloop.install()
            logger.info("Using uvloop for enhanced async performance")
        except ImportError:
            logger.debug("uvloop not available, using default event loop")
    
    def suggest_optimizations(self) -> List[str]:
        """Analyze performance and suggest optimizations"""
        suggestions = []
        
        summary = self.monitor.get_performance_summary()
        if not summary:
            return suggestions
        
        # CPU optimization suggestions
        if summary.get('avg_cpu_percent', 0) > 70:
            suggestions.append("Consider increasing collection intervals to reduce CPU load")
            suggestions.append("Enable message batching for high-throughput scenarios")
        
        # Memory optimization suggestions  
        if summary.get('avg_memory_mb', 0) > 200:
            suggestions.append("Consider limiting metrics history size")
            suggestions.append("Enable periodic garbage collection")
        
        # Response time suggestions
        if summary.get('avg_response_time_ms', 0) > 50:
            suggestions.append("Consider using message queuing for better throughput")
            suggestions.append("Optimize database/file I/O operations")
        
        # Network optimization suggestions
        if summary.get('avg_messages_per_second', 0) > 100:
            suggestions.append("Consider implementing message compression")
            suggestions.append("Use connection pooling for better network efficiency")
        
        return suggestions


# Decorators for performance measurement
def measure_performance(monitor: PerformanceMonitor):
    """Decorator to measure function performance"""
    def decorator(func):
        if asyncio.iscoroutinefunction(func):
            async def async_wrapper(*args, **kwargs):
                start_time = time.time()
                error = False
                try:
                    result = await func(*args, **kwargs)
                    return result
                except Exception as e:
                    error = True
                    raise e
                finally:
                    response_time = time.time() - start_time
                    monitor.record_message(response_time, error)
            return async_wrapper
        else:
            def sync_wrapper(*args, **kwargs):
                start_time = time.time()
                error = False
                try:
                    result = func(*args, **kwargs)
                    return result
                except Exception as e:
                    error = True
                    raise e
                finally:
                    response_time = time.time() - start_time
                    monitor.record_message(response_time, error)
            return sync_wrapper
    return decorator