"""
Performance Monitor for Unified Testing Framework

Provides real-time performance monitoring and metrics collection
during test execution.
"""

import time
import psutil
import threading
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, field
from datetime import datetime, timedelta

@dataclass
class PerformanceMetrics:
    """Performance metrics data structure"""
    timestamp: datetime
    cpu_percent: float
    memory_percent: float
    memory_mb: float
    disk_io_read_mb: float
    disk_io_write_mb: float
    network_sent_mb: float
    network_recv_mb: float

@dataclass
class PerformanceSnapshot:
    """Performance snapshot with aggregated metrics"""
    start_time: datetime
    end_time: datetime
    duration_seconds: float
    avg_cpu_percent: float
    max_cpu_percent: float
    avg_memory_percent: float
    max_memory_mb: float
    total_disk_read_mb: float
    total_disk_write_mb: float
    total_network_sent_mb: float
    total_network_recv_mb: float
    metrics_count: int

class PerformanceMonitor:
    """
    Real-time performance monitoring for test execution
    """
    
    def __init__(self, sample_interval: float = 1.0):
        self.sample_interval = sample_interval
        self.monitoring = False
        self.monitor_thread: Optional[threading.Thread] = None
        self.metrics: List[PerformanceMetrics] = []
        self.start_time: Optional[datetime] = None
        self.end_time: Optional[datetime] = None
        
        # Initial baseline measurements
        self._baseline_disk_io = psutil.disk_io_counters()
        self._baseline_network_io = psutil.net_io_counters()
    
    def start_monitoring(self):
        """Start performance monitoring in background thread"""
        if self.monitoring:
            return
        
        self.monitoring = True
        self.start_time = datetime.now()
        self.metrics.clear()
        
        # Reset baselines
        self._baseline_disk_io = psutil.disk_io_counters()
        self._baseline_network_io = psutil.net_io_counters()
        
        self.monitor_thread = threading.Thread(target=self._monitoring_loop)
        self.monitor_thread.daemon = True
        self.monitor_thread.start()
    
    def stop_monitoring(self) -> PerformanceSnapshot:
        """Stop monitoring and return performance snapshot"""
        if not self.monitoring:
            return self._create_empty_snapshot()
        
        self.monitoring = False
        self.end_time = datetime.now()
        
        if self.monitor_thread:
            self.monitor_thread.join(timeout=5.0)
        
        return self._create_snapshot()
    
    def _monitoring_loop(self):
        """Main monitoring loop running in background thread"""
        while self.monitoring:
            try:
                metrics = self._collect_current_metrics()
                self.metrics.append(metrics)
                time.sleep(self.sample_interval)
            except Exception as e:
                # Continue monitoring even if individual sample fails
                pass
    
    def _collect_current_metrics(self) -> PerformanceMetrics:
        """Collect current system performance metrics"""
        
        # CPU and memory
        cpu_percent = psutil.cpu_percent()
        memory = psutil.virtual_memory()
        
        # Disk I/O
        disk_io = psutil.disk_io_counters()
        disk_read_mb = 0
        disk_write_mb = 0
        
        if disk_io and self._baseline_disk_io:
            disk_read_mb = (disk_io.read_bytes - self._baseline_disk_io.read_bytes) / (1024 * 1024)
            disk_write_mb = (disk_io.write_bytes - self._baseline_disk_io.write_bytes) / (1024 * 1024)
        
        # Network I/O
        network_io = psutil.net_io_counters()
        network_sent_mb = 0
        network_recv_mb = 0
        
        if network_io and self._baseline_network_io:
            network_sent_mb = (network_io.bytes_sent - self._baseline_network_io.bytes_sent) / (1024 * 1024)
            network_recv_mb = (network_io.bytes_recv - self._baseline_network_io.bytes_recv) / (1024 * 1024)
        
        return PerformanceMetrics(
            timestamp=datetime.now(),
            cpu_percent=cpu_percent,
            memory_percent=memory.percent,
            memory_mb=memory.used / (1024 * 1024),
            disk_io_read_mb=disk_read_mb,
            disk_io_write_mb=disk_write_mb,
            network_sent_mb=network_sent_mb,
            network_recv_mb=network_recv_mb
        )
    
    def _create_snapshot(self) -> PerformanceSnapshot:
        """Create performance snapshot from collected metrics"""
        if not self.metrics or not self.start_time or not self.end_time:
            return self._create_empty_snapshot()
        
        duration = (self.end_time - self.start_time).total_seconds()
        
        # Calculate aggregated metrics
        cpu_values = [m.cpu_percent for m in self.metrics]
        memory_percents = [m.memory_percent for m in self.metrics]
        memory_mbs = [m.memory_mb for m in self.metrics]
        
        # Get final I/O values (cumulative)
        final_metrics = self.metrics[-1] if self.metrics else None
        
        return PerformanceSnapshot(
            start_time=self.start_time,
            end_time=self.end_time,
            duration_seconds=duration,
            avg_cpu_percent=sum(cpu_values) / len(cpu_values) if cpu_values else 0,
            max_cpu_percent=max(cpu_values) if cpu_values else 0,
            avg_memory_percent=sum(memory_percents) / len(memory_percents) if memory_percents else 0,
            max_memory_mb=max(memory_mbs) if memory_mbs else 0,
            total_disk_read_mb=final_metrics.disk_io_read_mb if final_metrics else 0,
            total_disk_write_mb=final_metrics.disk_io_write_mb if final_metrics else 0,
            total_network_sent_mb=final_metrics.network_sent_mb if final_metrics else 0,
            total_network_recv_mb=final_metrics.network_recv_mb if final_metrics else 0,
            metrics_count=len(self.metrics)
        )
    
    def _create_empty_snapshot(self) -> PerformanceSnapshot:
        """Create empty snapshot when no data is available"""
        now = datetime.now()
        return PerformanceSnapshot(
            start_time=now,
            end_time=now,
            duration_seconds=0,
            avg_cpu_percent=0,
            max_cpu_percent=0,
            avg_memory_percent=0,
            max_memory_mb=0,
            total_disk_read_mb=0,
            total_disk_write_mb=0,
            total_network_sent_mb=0,
            total_network_recv_mb=0,
            metrics_count=0
        )
    
    def get_current_metrics(self) -> Optional[PerformanceMetrics]:
        """Get current performance metrics without affecting monitoring"""
        try:
            return self._collect_current_metrics()
        except Exception:
            return None
    
    def get_metrics_summary(self) -> Dict[str, Any]:
        """Get summary of collected metrics"""
        if not self.metrics:
            return {"status": "no_data", "message": "No metrics collected"}
        
        snapshot = self._create_snapshot() if self.end_time else self._create_snapshot_from_current()
        
        return {
            "status": "active" if self.monitoring else "completed",
            "duration_seconds": snapshot.duration_seconds,
            "sample_count": snapshot.metrics_count,
            "resource_usage": {
                "cpu": {
                    "average_percent": snapshot.avg_cpu_percent,
                    "peak_percent": snapshot.max_cpu_percent
                },
                "memory": {
                    "average_percent": snapshot.avg_memory_percent,
                    "peak_mb": snapshot.max_memory_mb
                },
                "disk_io": {
                    "total_read_mb": snapshot.total_disk_read_mb,
                    "total_write_mb": snapshot.total_disk_write_mb
                },
                "network_io": {
                    "total_sent_mb": snapshot.total_network_sent_mb,
                    "total_received_mb": snapshot.total_network_recv_mb
                }
            }
        }
    
    def _create_snapshot_from_current(self) -> PerformanceSnapshot:
        """Create snapshot from current state (for active monitoring)"""
        now = datetime.now()
        duration = (now - self.start_time).total_seconds() if self.start_time else 0
        
        if not self.metrics:
            return self._create_empty_snapshot()
        
        cpu_values = [m.cpu_percent for m in self.metrics]
        memory_percents = [m.memory_percent for m in self.metrics]
        memory_mbs = [m.memory_mb for m in self.metrics]
        
        final_metrics = self.metrics[-1]
        
        return PerformanceSnapshot(
            start_time=self.start_time or now,
            end_time=now,
            duration_seconds=duration,
            avg_cpu_percent=sum(cpu_values) / len(cpu_values),
            max_cpu_percent=max(cpu_values),
            avg_memory_percent=sum(memory_percents) / len(memory_percents),
            max_memory_mb=max(memory_mbs),
            total_disk_read_mb=final_metrics.disk_io_read_mb,
            total_disk_write_mb=final_metrics.disk_io_write_mb,
            total_network_sent_mb=final_metrics.network_sent_mb,
            total_network_recv_mb=final_metrics.network_recv_mb,
            metrics_count=len(self.metrics)
        )
    
    def export_metrics_csv(self, filename: str):
        """Export collected metrics to CSV file"""
        import csv
        
        with open(filename, 'w', newline='') as csvfile:
            fieldnames = [
                'timestamp', 'cpu_percent', 'memory_percent', 'memory_mb',
                'disk_io_read_mb', 'disk_io_write_mb', 'network_sent_mb', 'network_recv_mb'
            ]
            
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            
            for metric in self.metrics:
                writer.writerow({
                    'timestamp': metric.timestamp.isoformat(),
                    'cpu_percent': metric.cpu_percent,
                    'memory_percent': metric.memory_percent,
                    'memory_mb': metric.memory_mb,
                    'disk_io_read_mb': metric.disk_io_read_mb,
                    'disk_io_write_mb': metric.disk_io_write_mb,
                    'network_sent_mb': metric.network_sent_mb,
                    'network_recv_mb': metric.network_recv_mb
                })
    
    def check_resource_limits(self, cpu_limit: float = 90.0, memory_limit_mb: float = 2048.0) -> Dict[str, Any]:
        """Check if current resource usage exceeds limits"""
        current = self.get_current_metrics()
        if not current:
            return {"status": "unknown", "message": "Could not collect current metrics"}
        
        violations = []
        
        if current.cpu_percent > cpu_limit:
            violations.append(f"CPU usage {current.cpu_percent:.1f}% exceeds limit {cpu_limit}%")
        
        if current.memory_mb > memory_limit_mb:
            violations.append(f"Memory usage {current.memory_mb:.1f}MB exceeds limit {memory_limit_mb}MB")
        
        return {
            "status": "violation" if violations else "ok",
            "violations": violations,
            "current_usage": {
                "cpu_percent": current.cpu_percent,
                "memory_mb": current.memory_mb,
                "memory_percent": current.memory_percent
            },
            "limits": {
                "cpu_percent": cpu_limit,
                "memory_mb": memory_limit_mb
            }
        }