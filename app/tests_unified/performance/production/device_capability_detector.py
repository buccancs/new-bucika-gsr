import logging
import platform
import psutil
import threading
import time
from dataclasses import dataclass
from typing import Dict, List, Optional, Any, Tuple
import json
@dataclass
class DeviceCapabilities:
    platform_name: str
    architecture: str
    processor: str
    cpu_cores_physical: int
    cpu_cores_logical: int
    cpu_max_frequency_mhz: float
    total_memory_gb: float
    available_memory_gb: float
    memory_type: str
    storage_type: str
    storage_total_gb: float
    storage_available_gb: float
    cpu_performance_score: float
    memory_performance_score: float
    storage_performance_score: float
    overall_performance_tier: str
    gpu_available: bool
    gpu_names: List[str]
    gpu_memory_mb: List[float]
    network_interfaces: List[str]
    max_network_speed_mbps: float
    thermal_monitoring_available: bool
    current_cpu_temperature: Optional[float]
    thermal_throttling_detected: bool
    battery_available: bool
    battery_percent: Optional[float]
    power_plugged: bool
@dataclass
class PerformanceProfile:
    device_tier: str
    max_concurrent_devices: int
    recommended_fps: int
    recommended_resolution: Tuple[int, int]
    enable_preview: bool
    preview_quality: str
    max_parallel_operations: int
    buffer_size_mb: int
    gc_frequency_seconds: int
    video_quality: float
    compression_level: int
    enable_hardware_acceleration: bool
    monitoring_frequency_hz: float
    detailed_logging: bool
    cpu_warning_threshold: float
    memory_warning_threshold: float
    frame_drop_threshold: float
class DeviceCapabilityDetector:
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.capabilities: Optional[DeviceCapabilities] = None
        self.performance_profile: Optional[PerformanceProfile] = None
        self._benchmarked = False
        self.tier_definitions = {
            "low": {
                "cpu_score_min": 0,
                "memory_gb_min": 0,
                "description": "Basic devices, limited resources"
            },
            "medium": {
                "cpu_score_min": 30,
                "memory_gb_min": 4,
                "description": "Standard devices, moderate performance"
            },
            "high": {
                "cpu_score_min": 60,
                "memory_gb_min": 8,
                "description": "High-performance devices, optimal for research"
            }
        }
    def detect_capabilities(self) -> DeviceCapabilities:
        self.logger.info("Starting device capability detection...")
        cpu_info = self._get_cpu_info()
        memory_info = self._get_memory_info()
        storage_info = self._get_storage_info()
        gpu_info = self._get_gpu_info()
        network_info = self._get_network_info()
        thermal_info = self._get_thermal_info()
        battery_info = self._get_battery_info()
        cpu_score = self._benchmark_cpu_performance()
        memory_score = self._benchmark_memory_performance()
        storage_score = self._benchmark_storage_performance()
        overall_tier = self._determine_performance_tier(
            cpu_score, memory_info["total_gb"], gpu_info["available"]
        )
        self.capabilities = DeviceCapabilities(
            platform_name=platform.platform(),
            architecture=platform.architecture()[0],
            processor=cpu_info["name"],
            cpu_cores_physical=cpu_info["physical_cores"],
            cpu_cores_logical=cpu_info["logical_cores"],
            cpu_max_frequency_mhz=cpu_info["max_frequency"],
            total_memory_gb=memory_info["total_gb"],
            available_memory_gb=memory_info["available_gb"],
            memory_type=memory_info["type"],
            storage_type=storage_info["type"],
            storage_total_gb=storage_info["total_gb"],
            storage_available_gb=storage_info["available_gb"],
            cpu_performance_score=cpu_score,
            memory_performance_score=memory_score,
            storage_performance_score=storage_score,
            overall_performance_tier=overall_tier,
            gpu_available=gpu_info["available"],
            gpu_names=gpu_info["names"],
            gpu_memory_mb=gpu_info["memory_mb"],
            network_interfaces=network_info["interfaces"],
            max_network_speed_mbps=network_info["max_speed"],
            thermal_monitoring_available=thermal_info["available"],
            current_cpu_temperature=thermal_info["cpu_temp"],
            thermal_throttling_detected=thermal_info["throttling"],
            battery_available=battery_info["available"],
            battery_percent=battery_info["percent"],
            power_plugged=battery_info["plugged"]
        )
        self.logger.info(f"Device detection completed - Tier: {overall_tier}")
        self.logger.info(f"CPU: {cpu_score:.1f}/100, Memory: {memory_info['total_gb']:.1f}GB, GPU: {gpu_info['available']}")
        return self.capabilities
    def _get_cpu_info(self) -> Dict[str, Any]:
        try:
            cpu_freq = psutil.cpu_freq()
            return {
                "name": platform.processor(),
                "physical_cores": psutil.cpu_count(logical=False),
                "logical_cores": psutil.cpu_count(logical=True),
                "max_frequency": cpu_freq.max if cpu_freq else 0.0,
                "current_frequency": cpu_freq.current if cpu_freq else 0.0
            }
        except Exception as e:
            self.logger.warning(f"Error getting CPU info: {e}")
            return {
                "name": "Unknown",
                "physical_cores": 1,
                "logical_cores": 1,
                "max_frequency": 0.0,
                "current_frequency": 0.0
            }
    def _get_memory_info(self) -> Dict[str, Any]:
        try:
            mem = psutil.virtual_memory()
            return {
                "total_gb": mem.total / (1024**3),
                "available_gb": mem.available / (1024**3),
                "type": "Unknown"
            }
        except Exception as e:
            self.logger.warning(f"Error getting memory info: {e}")
            return {"total_gb": 0.0, "available_gb": 0.0, "type": "Unknown"}
    def _get_storage_info(self) -> Dict[str, Any]:
        try:
            disk = psutil.disk_usage('/')
            storage_type = "Unknown"
            try:
                with open('/proc/mounts', 'r') as f:
                    mounts = f.read()
                    if 'ssd' in mounts.lower() or 'nvme' in mounts.lower():
                        storage_type = "SSD/NVMe"
                    else:
                        storage_type = "HDD"
            except:
                storage_type = "Unknown"
            return {
                "type": storage_type,
                "total_gb": disk.total / (1024**3),
                "available_gb": disk.free / (1024**3)
            }
        except Exception as e:
            self.logger.warning(f"Error getting storage info: {e}")
            return {"type": "Unknown", "total_gb": 0.0, "available_gb": 0.0}
    def _get_gpu_info(self) -> Dict[str, Any]:
        gpu_info = {
            "available": False,
            "names": [],
            "memory_mb": []
        }
        try:
            import pynvml
            pynvml.nvmlInit()
            device_count = pynvml.nvmlDeviceGetCount()
            for i in range(device_count):
                handle = pynvml.nvmlDeviceGetHandleByIndex(i)
                name = pynvml.nvmlDeviceGetName(handle).decode('utf-8')
                mem_info = pynvml.nvmlDeviceGetMemoryInfo(handle)
                gpu_info["names"].append(name)
                gpu_info["memory_mb"].append(mem_info.total / (1024*1024))
                gpu_info["available"] = True
        except ImportError:
            pass
        except Exception as e:
            self.logger.debug(f"NVIDIA GPU detection failed: {e}")
        try:
            import GPUtil
            gpus = GPUtil.getGPUs()
            for gpu in gpus:
                if gpu.name not in gpu_info["names"]:
                    gpu_info["names"].append(gpu.name)
                    gpu_info["memory_mb"].append(gpu.memoryTotal)
                    gpu_info["available"] = True
        except ImportError:
            pass
        except Exception as e:
            self.logger.debug(f"GPUtil detection failed: {e}")
        return gpu_info
    def _get_network_info(self) -> Dict[str, Any]:
        try:
            interfaces = []
            max_speed = 0.0
            net_if_stats = psutil.net_if_stats()
            for interface, stats in net_if_stats.items():
                if stats.isup and interface != 'lo':
                    interfaces.append(interface)
                    if stats.speed > max_speed:
                        max_speed = stats.speed
            return {
                "interfaces": interfaces,
                "max_speed": max_speed
            }
        except Exception as e:
            self.logger.warning(f"Error getting network info: {e}")
            return {"interfaces": [], "max_speed": 0.0}
    def _get_thermal_info(self) -> Dict[str, Any]:
        thermal_info = {
            "available": False,
            "cpu_temp": None,
            "throttling": False
        }
        try:
            if hasattr(psutil, "sensors_temperatures"):
                temps = psutil.sensors_temperatures()
                if temps:
                    thermal_info["available"] = True
                    for name, entries in temps.items():
                        if 'cpu' in name.lower() or 'core' in name.lower():
                            if entries:
                                thermal_info["cpu_temp"] = entries[0].current
                                if entries[0].current > 80:
                                    thermal_info["throttling"] = True
                                break
        except Exception as e:
            self.logger.debug(f"Thermal monitoring detection failed: {e}")
        return thermal_info
    def _get_battery_info(self) -> Dict[str, Any]:
        battery_info = {
            "available": False,
            "percent": None,
            "plugged": False
        }
        try:
            battery = psutil.sensors_battery()
            if battery:
                battery_info["available"] = True
                battery_info["percent"] = battery.percent
                battery_info["plugged"] = battery.power_plugged
        except Exception as e:
            self.logger.debug(f"Battery detection failed: {e}")
        return battery_info
    def _benchmark_cpu_performance(self) -> float:
        if self._benchmarked:
            return getattr(self, '_cpu_score', 50.0)
        try:
            start_time = time.time()
            result = 0
            iterations = 1000000
            for i in range(iterations):
                result += i * 0.5
            duration = time.time() - start_time
            score = min(100, max(0, 100 - (duration - 1.0) * 50))
            self._cpu_score = score
            self.logger.debug(f"CPU benchmark: {duration:.2f}s -> {score:.1f}/100")
            return score
        except Exception as e:
            self.logger.warning(f"CPU benchmark failed: {e}")
            return 50.0
    def _benchmark_memory_performance(self) -> float:
        try:
            start_time = time.time()
            data_size = 10 * 1024 * 1024
            data = bytearray(data_size)
            for i in range(0, data_size, 1024):
                data[i] = i % 256
            checksum = 0
            for i in range(0, data_size, 1024):
                checksum += data[i]
            duration = time.time() - start_time
            score = min(100, max(0, 100 - duration * 20))
            self.logger.debug(f"Memory benchmark: {duration:.2f}s -> {score:.1f}/100")
            return score
        except Exception as e:
            self.logger.warning(f"Memory benchmark failed: {e}")
            return 50.0
    def _benchmark_storage_performance(self) -> float:
        try:
            import tempfile
            import os
            with tempfile.NamedTemporaryFile(delete=False) as tmp:
                tmp_path = tmp.name
            try:
                start_time = time.time()
                data = b'0' * (1024 * 1024)
                with open(tmp_path, 'wb') as f:
                    for _ in range(10):
                        f.write(data)
                with open(tmp_path, 'rb') as f:
                    while f.read(1024 * 1024):
                        pass
                duration = time.time() - start_time
                score = min(100, max(0, 100 - duration * 20))
                self.logger.debug(f"Storage benchmark: {duration:.2f}s -> {score:.1f}/100")
                return score
            finally:
                try:
                    os.unlink(tmp_path)
                except:
                    pass
        except Exception as e:
            self.logger.warning(f"Storage benchmark failed: {e}")
            return 50.0
    def _determine_performance_tier(self, cpu_score: float, memory_gb: float,
                                   has_gpu: bool) -> str:
        if (cpu_score >= 60 and memory_gb >= 8) or has_gpu:
            return "high"
        if cpu_score >= 30 and memory_gb >= 4:
            return "medium"
        return "low"
    def generate_performance_profile(self) -> PerformanceProfile:
        if not self.capabilities:
            self.detect_capabilities()
        tier = self.capabilities.overall_performance_tier
        profiles = {
            "low": PerformanceProfile(
                device_tier="low",
                max_concurrent_devices=2,
                recommended_fps=15,
                recommended_resolution=(640, 480),
                enable_preview=False,
                preview_quality="low",
                max_parallel_operations=2,
                buffer_size_mb=32,
                gc_frequency_seconds=30,
                video_quality=0.5,
                compression_level=7,
                enable_hardware_acceleration=False,
                monitoring_frequency_hz=0.5,
                detailed_logging=False,
                cpu_warning_threshold=60.0,
                memory_warning_threshold=70.0,
                frame_drop_threshold=50.0
            ),
            "medium": PerformanceProfile(
                device_tier="medium",
                max_concurrent_devices=4,
                recommended_fps=20,
                recommended_resolution=(1280, 720),
                enable_preview=True,
                preview_quality="medium",
                max_parallel_operations=4,
                buffer_size_mb=64,
                gc_frequency_seconds=60,
                video_quality=0.7,
                compression_level=5,
                enable_hardware_acceleration=self.capabilities.gpu_available,
                monitoring_frequency_hz=1.0,
                detailed_logging=False,
                cpu_warning_threshold=70.0,
                memory_warning_threshold=80.0,
                frame_drop_threshold=70.0
            ),
            "high": PerformanceProfile(
                device_tier="high",
                max_concurrent_devices=8,
                recommended_fps=30,
                recommended_resolution=(1920, 1080),
                enable_preview=True,
                preview_quality="high",
                max_parallel_operations=8,
                buffer_size_mb=128,
                gc_frequency_seconds=120,
                video_quality=0.9,
                compression_level=3,
                enable_hardware_acceleration=True,
                monitoring_frequency_hz=2.0,
                detailed_logging=True,
                cpu_warning_threshold=80.0,
                memory_warning_threshold=85.0,
                frame_drop_threshold=85.0
            )
        }
        self.performance_profile = profiles[tier]
        self._adjust_profile_for_device()
        self.logger.info(f"Generated {tier} performance profile")
        return self.performance_profile
    def _adjust_profile_for_device(self):
        if not self.capabilities or not self.performance_profile:
            return
        if self.capabilities.thermal_throttling_detected:
            self.performance_profile.recommended_fps = max(10,
                self.performance_profile.recommended_fps - 5)
            self.performance_profile.cpu_warning_threshold -= 10
            self.logger.info("Adjusted profile for thermal throttling")
        if (self.capabilities.battery_available and
            not self.capabilities.power_plugged and
            self.capabilities.battery_percent and
            self.capabilities.battery_percent < 50):
            self.performance_profile.enable_preview = False
            self.performance_profile.monitoring_frequency_hz *= 0.5
            self.performance_profile.max_parallel_operations = max(1,
                self.performance_profile.max_parallel_operations // 2)
            self.logger.info("Adjusted profile for low battery")
        if self.capabilities.available_memory_gb < 2:
            self.performance_profile.buffer_size_mb = max(16,
                self.performance_profile.buffer_size_mb // 2)
            self.performance_profile.max_concurrent_devices = max(1,
                self.performance_profile.max_concurrent_devices // 2)
            self.logger.info("Adjusted profile for limited memory")
    def save_device_profile(self, filename: str):
        if not self.capabilities or not self.performance_profile:
            self.logger.error("No capabilities or profile to save")
            return
        profile_data = {
            "capabilities": self.capabilities.__dict__,
            "performance_profile": self.performance_profile.__dict__,
            "detection_timestamp": time.time(),
            "platform": platform.platform()
        }
        try:
            with open(filename, 'w') as f:
                json.dump(profile_data, f, indent=2, default=str)
            self.logger.info(f"Device profile saved to {filename}")
        except Exception as e:
            self.logger.error(f"Failed to save device profile: {e}")
    def load_device_profile(self, filename: str) -> bool:
        try:
            with open(filename, 'r') as f:
                profile_data = json.load(f)
            caps_dict = profile_data["capabilities"]
            self.capabilities = DeviceCapabilities(**caps_dict)
            profile_dict = profile_data["performance_profile"]
            self.performance_profile = PerformanceProfile(**profile_dict)
            self.logger.info(f"Device profile loaded from {filename}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to load device profile: {e}")
            return False
    def get_optimization_recommendations(self) -> List[str]:
        if not self.capabilities:
            return ["Run device detection first"]
        recommendations = []
        if self.capabilities.cpu_performance_score < 30:
            recommendations.append(
                "Low CPU performance detected. Consider reducing frame rates and parallel operations."
            )
        if self.capabilities.total_memory_gb < 4:
            recommendations.append(
                "Limited memory available. Enable aggressive garbage collection and reduce buffer sizes."
            )
        if not self.capabilities.gpu_available:
            recommendations.append(
                "No GPU detected. CPU-only processing will be used, which may limit performance."
            )
        if self.capabilities.thermal_throttling_detected:
            recommendations.append(
                "Thermal throttling detected. Reduce processing intensity to prevent overheating."
            )
        if self.capabilities.storage_performance_score < 30:
            recommendations.append(
                "Slow storage detected. Consider reducing video quality or using external storage."
            )
        if (self.capabilities.battery_available and
            not self.capabilities.power_plugged):
            recommendations.append(
                "Running on battery power. Consider reducing preview features to extend battery life."
            )
        if not recommendations:
            recommendations.append("Device capabilities look good for optimal performance!")
        return recommendations
def detect_device_and_generate_profile(save_to_file: bool = True) -> Tuple[DeviceCapabilities, PerformanceProfile]:
    detector = DeviceCapabilityDetector()
    capabilities = detector.detect_capabilities()
    profile = detector.generate_performance_profile()
    if save_to_file:
        filename = f"device_profile_{int(time.time())}.json"
        detector.save_device_profile(filename)
    return capabilities, profile
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    print("=== Device Capability Detection ===")
    capabilities, profile = detect_device_and_generate_profile()
    print(f"\nDevice Tier: {capabilities.overall_performance_tier}")
    print(f"CPU Score: {capabilities.cpu_performance_score:.1f}/100")
    print(f"Memory: {capabilities.total_memory_gb:.1f}GB")
    print(f"GPU Available: {capabilities.gpu_available}")
    print(f"\n=== Performance Profile ===")
    print(f"Max Devices: {profile.max_concurrent_devices}")
    print(f"Recommended FPS: {profile.recommended_fps}")
    print(f"Recommended Resolution: {profile.recommended_resolution}")
    print(f"Hardware Acceleration: {profile.enable_hardware_acceleration}")
    detector = DeviceCapabilityDetector()
    detector.capabilities = capabilities
    recommendations = detector.get_optimization_recommendations()
    print(f"\n=== Recommendations ===")
    for i, rec in enumerate(recommendations, 1):
        print(f"{i}. {rec}")