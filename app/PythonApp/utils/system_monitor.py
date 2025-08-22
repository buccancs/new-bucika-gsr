"""
Simplified system monitoring interface - delegates to unified system monitoring.

This module provides a simplified interface to maintain backwards compatibility
while using the unified system monitoring from shared_protocols.
"""

import time

from shared_protocols.system_monitoring import (
    get_system_monitor, 
    start_system_monitoring, 
    stop_system_monitoring,
    get_current_system_metrics,
    get_system_health_report,
    UnifiedSystemMonitor,
    PerformanceMetrics,
    SystemInfo,
    ResourceLimits
)

try:
    from .logging_config import get_logger
    logger = get_logger(__name__)
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)


class SystemMonitor:
    """Simplified interface that delegates to UnifiedSystemMonitor."""
    
    def __init__(self):
        self._unified_monitor = get_system_monitor()
        self.monitoring = False
        self.monitor_thread = None
        self.system_info = self._get_system_info()
        self._last_update = time.time()
    
    def _get_system_info(self):
        """Get system information using unified monitor."""
        return self._unified_monitor.system_info.__dict__
    
    def start_monitoring(self, interval=1.0):
        """Start monitoring using unified system."""
        self.monitoring = start_system_monitoring(interval)
        return self.monitoring
    
    def stop_monitoring(self):
        """Stop monitoring using unified system."""
        stop_system_monitoring()
        self.monitoring = False
    
    def get_current_metrics(self):
        """Get current metrics using unified system."""
        metrics = get_current_system_metrics()
        return metrics.__dict__ if metrics else {}
    
    def get_health_report(self):
        """Get health report using unified system."""
        return get_system_health_report()
    
    def is_system_healthy(self):
        """Check if system is healthy."""
        report = self.get_health_report()
        return report.get("status") == "good"
    
    def get_memory_usage(self):
        """Get current memory usage."""
        metrics = get_current_system_metrics()
        if metrics:
            return {
                "memory_mb": metrics.memory_mb,
                "memory_percent": metrics.memory_percent
            }
        return {"memory_mb": 0, "memory_percent": 0}
    
    def get_cpu_usage(self):
        """Get current CPU usage."""
        metrics = get_current_system_metrics()
        return metrics.cpu_percent if metrics else 0.0


    # Backwards compatibility
    def create_system_monitor():
        """Create a system monitor instance."""
        return SystemMonitor()


    # Global instance for backwards compatibility  
    _global_simple_monitor = None


    def get_simple_monitor():
        """Get the simplified monitor instance."""
        global _global_simple_monitor
        if _global_simple_monitor is None:
            _global_simple_monitor = SystemMonitor()
        return _global_simple_monitor

    def get_cpu_usage(self) -> Dict[str, Any]:
        try:
            cpu_percent = psutil.cpu_percent(interval=0.1)
            cpu_freq = psutil.cpu_freq()
            cpu_stats = psutil.cpu_stats()
            return {
                "usage_percent": cpu_percent,
                "frequency_current": cpu_freq.current if cpu_freq else 0,
                "frequency_min": cpu_freq.min if cpu_freq else 0,
                "frequency_max": cpu_freq.max if cpu_freq else 0,
                "ctx_switches": cpu_stats.ctx_switches,
                "interrupts": cpu_stats.interrupts,
                "soft_interrupts": cpu_stats.soft_interrupts,
                "syscalls": cpu_stats.syscalls,
                "load_average": (
                    os.getloadavg() if hasattr(os, "getloadavg") else [0, 0, 0]
                ),
            }
        except Exception as e:
            logger.error(f"Error getting CPU usage: {e}")
            return {"usage_percent": 0}
    def get_memory_usage(self) -> Dict[str, Any]:
        try:
            memory = psutil.virtual_memory()
            swap = psutil.swap_memory()
            return {
                "total": memory.total,
                "available": memory.available,
                "used": memory.used,
                "free": memory.free,
                "percent": memory.percent,
                "buffers": getattr(memory, "buffers", 0),
                "cached": getattr(memory, "cached", 0),
                "swap_total": swap.total,
                "swap_used": swap.used,
                "swap_free": swap.free,
                "swap_percent": swap.percent,
            }
        except Exception as e:
            logger.error(f"Error getting memory usage: {e}")
            return {"total": 0, "used": 0, "percent": 0}
    def get_disk_usage(self) -> Dict[str, Any]:
        try:
            disk_info = {}
            partitions = psutil.disk_partitions()
            for partition in partitions:
                try:
                    usage = psutil.disk_usage(partition.mountpoint)
                    disk_info[partition.device] = {
                        "mountpoint": partition.mountpoint,
                        "fstype": partition.fstype,
                        "total": usage.total,
                        "used": usage.used,
                        "free": usage.free,
                        "percent": (
                            round(usage.used / usage.total * 100, 2)
                            if usage.total > 0
                            else 0
                        ),
                    }
                except (PermissionError, OSError):
                    continue
            try:
                disk_io = psutil.disk_io_counters()
                if disk_io:
                    disk_info["io_stats"] = {
                        "read_count": disk_io.read_count,
                        "write_count": disk_io.write_count,
                        "read_bytes": disk_io.read_bytes,
                        "write_bytes": disk_io.write_bytes,
                        "read_time": disk_io.read_time,
                        "write_time": disk_io.write_time,
                    }
            except Exception:
                pass
            return disk_info
        except Exception as e:
            logger.error(f"Error getting disk usage: {e}")
            return {}
    def get_network_info(self) -> Dict[str, Any]:
        try:
            network_info = {}
            net_io = psutil.net_io_counters(pernic=True)
            for interface, stats in net_io.items():
                network_info[interface] = {
                    "bytes_sent": stats.bytes_sent,
                    "bytes_recv": stats.bytes_recv,
                    "packets_sent": stats.packets_sent,
                    "packets_recv": stats.packets_recv,
                    "errin": stats.errin,
                    "errout": stats.errout,
                    "dropin": stats.dropin,
                    "dropout": stats.dropout,
                }
            try:
                net_addrs = psutil.net_if_addrs()
                for interface, addrs in net_addrs.items():
                    if interface in network_info:
                        network_info[interface]["addresses"] = []
                        for addr in addrs:
                            network_info[interface]["addresses"].append(
                                {
                                    "family": str(addr.family),
                                    "address": addr.address,
                                    "netmask": addr.netmask,
                                    "broadcast": addr.broadcast,
                                }
                            )
            except Exception:
                pass
            return network_info
        except Exception as e:
            logger.error(f"Error getting network info: {e}")
            return {}
    def detect_webcams(self) -> List[Dict[str, Any]]:
        if not OPENCV_AVAILABLE:
            logger.warning("OpenCV not available, cannot detect webcams")
            return []
        webcams = []
        try:
            for index in range(10):
                cap = cv2.VideoCapture(index)
                if cap.isOpened():
                    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
                    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
                    fps = cap.get(cv2.CAP_PROP_FPS)
                    webcams.append(
                        {
                            "index": index,
                            "name": f"Camera {index}",
                            "resolution": f"{width}x{height}",
                            "fps": fps,
                            "status": "available",
                        }
                    )
                    cap.release()
                else:
                    cap.release()
                    break
            logger.info(f"Detected {len(webcams)} webcam(s)")
            return webcams
        except Exception as e:
            logger.error(f"Error detecting webcams: {e}")
            return []
    def detect_bluetooth_devices(self) -> List[Dict[str, Any]]:
        bluetooth_devices = []
        try:
            if platform.system() == "Linux":
                try:
                    result = subprocess.run(
                        ["bluetoothctl", "list"],
                        capture_output=True,
                        text=True,
                        timeout=5,
                    )
                    if result.returncode == 0:
                        for line in result.stdout.split("\n"):
                            if "Controller" in line:
                                parts = line.split()
                                if len(parts) >= 3:
                                    mac = parts[1]
                                    name = " ".join(parts[2:])
                                    bluetooth_devices.append(
                                        {
                                            "mac": mac,
                                            "name": name,
                                            "type": "controller",
                                            "status": "available",
                                        }
                                    )
                except (subprocess.TimeoutExpired, FileNotFoundError):
                    pass
            elif platform.system() == "Windows":
                try:
                    result = subprocess.run(
                        [
                            "powershell",
                            "-Command",
                            'Get-PnpDevice | Where-Object {$_.Class -eq "Bluetooth"} | Select-Object Name, Status',
                        ],
                        capture_output=True,
                        text=True,
                        timeout=10,
                    )
                    if result.returncode == 0:
                        lines = result.stdout.split("\n")[3:]
                        for line in lines:
                            line = line.strip()
                            if line and not line.startswith("-"):
                                parts = line.split()
                                if len(parts) >= 2:
                                    status = parts[-1]
                                    name = " ".join(parts[:-1])
                                    bluetooth_devices.append(
                                        {
                                            "name": name,
                                            "status": status.lower(),
                                            "type": "device",
                                        }
                                    )
                except (subprocess.TimeoutExpired, FileNotFoundError):
                    pass
            logger.info(f"Detected {len(bluetooth_devices)} Bluetooth device(s)")
            return bluetooth_devices
        except Exception as e:
            logger.error(f"Error detecting Bluetooth devices: {e}")
            return []
    def get_process_info(self) -> List[Dict[str, Any]]:
        try:
            processes = []
            current_pid = os.getpid()
            for proc in psutil.process_iter(
                ["pid", "name", "cpu_percent", "memory_percent", "create_time"]
            ):
                try:
                    proc_info = proc.info
                    if (
                        proc_info["name"].lower().startswith("python")
                        or proc_info["pid"] == current_pid
                        or "bucika" in proc_info["name"].lower()
                    ):
                        processes.append(
                            {
                                "pid": proc_info["pid"],
                                "name": proc_info["name"],
                                "cpu_percent": proc_info["cpu_percent"],
                                "memory_percent": proc_info["memory_percent"],
                                "create_time": proc_info["create_time"],
                                "is_current": proc_info["pid"] == current_pid,
                            }
                        )
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    continue
            return processes
        except Exception as e:
            logger.error(f"Error getting process info: {e}")
            return []
    def get_temperature_info(self) -> Dict[str, Any]:
        try:
            temps = {}
            if hasattr(psutil, "sensors_temperatures"):
                temperatures = psutil.sensors_temperatures()
                for name, entries in temperatures.items():
                    temps[name] = []
                    for entry in entries:
                        temps[name].append(
                            {
                                "label": entry.label,
                                "current": entry.current,
                                "high": entry.high,
                                "critical": entry.critical,
                            }
                        )
            return temps
        except Exception as e:
            logger.error(f"Error getting temperature info: {e}")
            return {}
    def get_complete_status(self) -> Dict[str, Any]:
        current_time = time.time()
        status = {
            "timestamp": current_time,
            "system_info": self.system_info,
            "cpu": self.get_cpu_usage(),
            "memory": self.get_memory_usage(),
            "disk": self.get_disk_usage(),
            "network": self.get_network_info(),
            "webcams": self.detect_webcams(),
            "bluetooth": self.detect_bluetooth_devices(),
            "processes": self.get_process_info(),
            "temperature": self.get_temperature_info(),
            "uptime": current_time - self.system_info.get("boot_time", current_time),
        }
        self._last_update = current_time
        return status

_system_monitor = None

def get_system_monitor() -> SystemMonitor:
    global _system_monitor
    if _system_monitor is None:
        _system_monitor = SystemMonitor()
    return _system_monitor
