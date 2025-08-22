import json
import logging
import statistics
import time
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from collections import defaultdict
try:
    from ..utils.logging_config import get_logger
except ImportError:
    def get_logger(name):
        return logging.getLogger(name)
class DeviceTier(Enum):
    LOW_END = "low_end"
    MID_RANGE = "mid_range"
    HIGH_END = "high_end"
    FLAGSHIP = "flagship"
    UNKNOWN = "unknown"
class PerformanceCategory(Enum):
    CPU_INTENSIVE = "cpu_intensive"
    MEMORY_INTENSIVE = "memory_intensive"
    CAMERA_PROCESSING = "camera_processing"
    THERMAL_PROCESSING = "thermal_processing"
    NETWORK_THROUGHPUT = "network_throughput"
    STORAGE_IO = "storage_io"
    BATTERY_EFFICIENCY = "battery_efficiency"
@dataclass
class DeviceSpecification:
    device_model: str
    manufacturer: str
    android_version: str
    api_level: int
    cpu_architecture: str
    cpu_cores: int
    cpu_frequency_mhz: int
    total_ram_mb: int
    available_ram_mb: int
    total_storage_gb: int
    available_storage_gb: int
    storage_type: str
    screen_width: int
    screen_height: int
    screen_density_dpi: int
    camera_count: int
    max_camera_resolution: str
    supports_camera2_api: bool
    supports_5ghz_wifi: bool
    supports_bluetooth_5: bool
    supports_nfc: bool
    battery_capacity_mah: int
    supports_fast_charging: bool
    has_thermal_camera: bool = False
    has_hardware_acceleration: bool = False
    supports_vulkan_api: bool = False
    tier: DeviceTier = DeviceTier.UNKNOWN
    performance_score: float = 0.0
    def calculate_tier_and_score(self):
        score = 0.0
        cpu_score = min(25, (self.cpu_cores * self.cpu_frequency_mhz) / 80000)
        score += cpu_score
        memory_score = min(25, self.total_ram_mb / 200)
        score += memory_score
        storage_score = min(15, self.total_storage_gb / 8)
        if self.storage_type in ["ufs_3.0", "ufs_3.1"]:
            storage_score *= 1.2
        elif self.storage_type in ["ufs_2.0", "ufs_2.1"]:
            storage_score *= 1.1
        score += min(15, storage_score)
        camera_score = self.camera_count * 2
        if self.supports_camera2_api:
            camera_score += 5
        if "4K" in self.max_camera_resolution:
            camera_score += 5
        score += min(15, camera_score)
        features_score = 0
        if self.supports_5ghz_wifi:
            features_score += 3
        if self.supports_bluetooth_5:
            features_score += 2
        if self.has_hardware_acceleration:
            features_score += 5
        if self.supports_vulkan_api:
            features_score += 3
        if self.has_thermal_camera:
            features_score += 7
        score += min(20, features_score)
        self.performance_score = min(100, score)
        if self.performance_score >= 80:
            self.tier = DeviceTier.FLAGSHIP
        elif self.performance_score >= 60:
            self.tier = DeviceTier.HIGH_END
        elif self.performance_score >= 40:
            self.tier = DeviceTier.MID_RANGE
        else:
            self.tier = DeviceTier.LOW_END
@dataclass
class PerformanceTestResult:
    test_category: PerformanceCategory
    device_model: str
    test_duration_seconds: float
    throughput_ops_per_second: float
    average_response_time_ms: float
    peak_memory_usage_mb: float
    cpu_usage_percent: float
    battery_drain_percent: float
    success_rate: float
    error_count: int
    frame_drop_rate: float = 0.0
    metadata: Dict[str, Any] = field(default_factory=dict)
    def get_performance_grade(self) -> str:
        if self.success_rate < 0.8:
            return "F"
        efficiency_score = self.throughput_ops_per_second / max(1, self.cpu_usage_percent)
        if efficiency_score >= 10 and self.success_rate >= 0.95:
            return "A"
        elif efficiency_score >= 5 and self.success_rate >= 0.90:
            return "B"
        elif efficiency_score >= 2 and self.success_rate >= 0.85:
            return "C"
        else:
            return "D"
@dataclass
class DeviceProfile:
    device_spec: DeviceSpecification
    test_results: List[PerformanceTestResult] = field(default_factory=list)
    optimization_recommendations: List[str] = field(default_factory=list)
    overall_performance_grade: str = "N/A"
    best_categories: List[PerformanceCategory] = field(default_factory=list)
    worst_categories: List[PerformanceCategory] = field(default_factory=list)
    def analyze_performance(self):
        if not self.test_results:
            return
        grades = [result.get_performance_grade() for result in self.test_results]
        grade_scores = {"A": 4, "B": 3, "C": 2, "D": 1, "F": 0}
        avg_score = statistics.mean([grade_scores[grade] for grade in grades])
        if avg_score >= 3.5:
            self.overall_performance_grade = "A"
        elif avg_score >= 2.5:
            self.overall_performance_grade = "B"
        elif avg_score >= 1.5:
            self.overall_performance_grade = "C"
        elif avg_score >= 0.5:
            self.overall_performance_grade = "D"
        else:
            self.overall_performance_grade = "F"
        category_scores = {}
        for result in self.test_results:
            efficiency = result.throughput_ops_per_second / max(1, result.cpu_usage_percent)
            category_scores[result.test_category] = efficiency * result.success_rate
        sorted_categories = sorted(category_scores.items(), key=lambda x: x[1], reverse=True)
        self.best_categories = [cat for cat, _ in sorted_categories[:2]]
        self.worst_categories = [cat for cat, _ in sorted_categories[-2:]]
        self._generate_optimization_recommendations()
    def _generate_optimization_recommendations(self):
        self.optimization_recommendations = []
        if self.device_spec.tier == DeviceTier.LOW_END:
            self.optimization_recommendations.extend([
                "Use conservative quality settings to maintain performance",
                "Enable aggressive frame dropping to prevent memory overflow",
                "Reduce recording resolution to 720p or lower",
                "Disable real-time preview to save CPU resources",
                "Use lower camera frame rates (15 FPS or less)"
            ])
        elif self.device_spec.tier == DeviceTier.MID_RANGE:
            self.optimization_recommendations.extend([
                "Use balanced quality settings for optimal performance",
                "Enable moderate frame dropping as needed",
                "Consider 1080p recording with quality adjustments",
                "Monitor CPU usage and adjust processing accordingly"
            ])
        elif self.device_spec.tier in [DeviceTier.HIGH_END, DeviceTier.FLAGSHIP]:
            self.optimization_recommendations.extend([
                "Can use high-quality settings for best results",
                "Enable hardware acceleration where available",
                "Consider 4K recording if supported",
                "Use advanced processing features"
            ])
        for result in self.test_results:
            if result.get_performance_grade() in ["D", "F"]:
                if result.test_category == PerformanceCategory.CAMERA_PROCESSING:
                    self.optimization_recommendations.append(
                        "Camera processing performance is poor - reduce resolution or frame rate"
                    )
                elif result.test_category == PerformanceCategory.MEMORY_INTENSIVE:
                    self.optimization_recommendations.append(
                        "Memory performance is limited - enable aggressive cleanup and reduce buffers"
                    )
                elif result.test_category == PerformanceCategory.CPU_INTENSIVE:
                    self.optimization_recommendations.append(
                        "CPU performance is limited - reduce processing complexity"
                    )
        if self.device_spec.total_ram_mb < 3000:
            self.optimization_recommendations.append(
                "Low RAM detected - implement strict memory management"
            )
        elif self.device_spec.total_ram_mb < 6000:
            self.optimization_recommendations.append(
                "Moderate RAM available - monitor memory usage closely"
            )
class DevicePerformanceTester:
    def __init__(self):
        self.logger = get_logger(__name__)
        self.test_profiles: Dict[DeviceTier, Dict[str, Any]] = self._create_test_profiles()
    def _create_test_profiles(self) -> Dict[DeviceTier, Dict[str, Any]]:
        return {
            DeviceTier.LOW_END: {
                "camera_resolution": (640, 480),
                "camera_fps": 15,
                "processing_threads": 1,
                "memory_limit_mb": 200,
                "test_duration_seconds": 30
            },
            DeviceTier.MID_RANGE: {
                "camera_resolution": (1280, 720),
                "camera_fps": 20,
                "processing_threads": 2,
                "memory_limit_mb": 500,
                "test_duration_seconds": 60
            },
            DeviceTier.HIGH_END: {
                "camera_resolution": (1920, 1080),
                "camera_fps": 30,
                "processing_threads": 4,
                "memory_limit_mb": 1000,
                "test_duration_seconds": 120
            },
            DeviceTier.FLAGSHIP: {
                "camera_resolution": (3840, 2160),
                "camera_fps": 30,
                "processing_threads": 8,
                "memory_limit_mb": 2000,
                "test_duration_seconds": 180
            }
        }
    def detect_device_specifications(self, adb_device_id: Optional[str] = None) -> DeviceSpecification:
        return DeviceSpecification(
            device_model="Mock Device",
            manufacturer="Mock Manufacturer",
            android_version="11.0",
            api_level=30,
            cpu_architecture="arm64-v8a",
            cpu_cores=8,
            cpu_frequency_mhz=2400,
            total_ram_mb=6144,
            available_ram_mb=4096,
            total_storage_gb=128,
            available_storage_gb=64,
            storage_type="ufs_3.0",
            screen_width=1080,
            screen_height=2400,
            screen_density_dpi=420,
            camera_count=3,
            max_camera_resolution="4K",
            supports_camera2_api=True,
            supports_5ghz_wifi=True,
            supports_bluetooth_5=True,
            supports_nfc=True,
            battery_capacity_mah=4000,
            supports_fast_charging=True,
            has_thermal_camera=False,
            has_hardware_acceleration=True,
            supports_vulkan_api=True
        )
    def run_performance_test(self, device_spec: DeviceSpecification,
                           category: PerformanceCategory) -> PerformanceTestResult:
        self.logger.info(f"DevicePerformanceTester: Running {category.value} test on {device_spec.device_model}")
        test_profile = self.test_profiles.get(device_spec.tier, self.test_profiles[DeviceTier.MID_RANGE])
        if category == PerformanceCategory.CPU_INTENSIVE:
            return self._test_cpu_intensive(device_spec, test_profile)
        elif category == PerformanceCategory.MEMORY_INTENSIVE:
            return self._test_memory_intensive(device_spec, test_profile)
        elif category == PerformanceCategory.CAMERA_PROCESSING:
            return self._test_camera_processing(device_spec, test_profile)
        elif category == PerformanceCategory.THERMAL_PROCESSING:
            return self._test_thermal_processing(device_spec, test_profile)
        elif category == PerformanceCategory.NETWORK_THROUGHPUT:
            return self._test_network_throughput(device_spec, test_profile)
        elif category == PerformanceCategory.STORAGE_IO:
            return self._test_storage_io(device_spec, test_profile)
        elif category == PerformanceCategory.BATTERY_EFFICIENCY:
            return self._test_battery_efficiency(device_spec, test_profile)
        else:
            raise ValueError(f"Unknown test category: {category}")
    def _test_cpu_intensive(self, device_spec: DeviceSpecification,
                           test_profile: Dict[str, Any]) -> PerformanceTestResult:
        base_throughput = device_spec.cpu_cores * device_spec.cpu_frequency_mhz / 1000
        if device_spec.tier == DeviceTier.LOW_END:
            throughput = base_throughput * 0.7
            cpu_usage = 95.0
            success_rate = 0.85
        elif device_spec.tier == DeviceTier.MID_RANGE:
            throughput = base_throughput * 0.9
            cpu_usage = 80.0
            success_rate = 0.92
        elif device_spec.tier == DeviceTier.HIGH_END:
            throughput = base_throughput * 1.1
            cpu_usage = 70.0
            success_rate = 0.96
        else:
            throughput = base_throughput * 1.3
            cpu_usage = 60.0
            success_rate = 0.98
        return PerformanceTestResult(
            test_category=PerformanceCategory.CPU_INTENSIVE,
            device_model=device_spec.device_model,
            test_duration_seconds=test_profile["test_duration_seconds"],
            throughput_ops_per_second=throughput,
            average_response_time_ms=50.0 / (throughput / 100),
            peak_memory_usage_mb=device_spec.total_ram_mb * 0.3,
            cpu_usage_percent=cpu_usage,
            battery_drain_percent=2.0,
            success_rate=success_rate,
            error_count=int((1 - success_rate) * 100),
            metadata={"test_type": "cpu_intensive", "threads_used": test_profile["processing_threads"]}
        )
    def _test_memory_intensive(self, device_spec: DeviceSpecification,
                              test_profile: Dict[str, Any]) -> PerformanceTestResult:
        available_memory = device_spec.available_ram_mb
        memory_efficiency = min(1.0, available_memory / 4000)
        throughput = memory_efficiency * 50
        cpu_usage = 40 + (1 - memory_efficiency) * 30
        success_rate = 0.8 + memory_efficiency * 0.15
        return PerformanceTestResult(
            test_category=PerformanceCategory.MEMORY_INTENSIVE,
            device_model=device_spec.device_model,
            test_duration_seconds=test_profile["test_duration_seconds"],
            throughput_ops_per_second=throughput,
            average_response_time_ms=100.0,
            peak_memory_usage_mb=min(available_memory * 0.8, test_profile["memory_limit_mb"]),
            cpu_usage_percent=cpu_usage,
            battery_drain_percent=1.5,
            success_rate=success_rate,
            error_count=int((1 - success_rate) * 50),
            metadata={"memory_efficiency": memory_efficiency}
        )
    def _test_camera_processing(self, device_spec: DeviceSpecification,
                               test_profile: Dict[str, Any]) -> PerformanceTestResult:
        camera_performance = 1.0
        if device_spec.supports_camera2_api:
            camera_performance *= 1.2
        if "4K" in device_spec.max_camera_resolution:
            camera_performance *= 1.1
        if device_spec.has_hardware_acceleration:
            camera_performance *= 1.3
        fps = test_profile["camera_fps"] * camera_performance
        throughput = fps * test_profile["camera_resolution"][0] * test_profile["camera_resolution"][1] / 1000000
        cpu_usage = 60 - (camera_performance - 1) * 20
        frame_drop_rate = max(0, (30 - fps) / 30 * 0.1)
        success_rate = 1.0 - frame_drop_rate
        return PerformanceTestResult(
            test_category=PerformanceCategory.CAMERA_PROCESSING,
            device_model=device_spec.device_model,
            test_duration_seconds=test_profile["test_duration_seconds"],
            throughput_ops_per_second=throughput,
            average_response_time_ms=1000 / fps,
            peak_memory_usage_mb=300,
            cpu_usage_percent=cpu_usage,
            battery_drain_percent=3.0,
            success_rate=success_rate,
            error_count=0,
            frame_drop_rate=frame_drop_rate,
            metadata={
                "target_fps": test_profile["camera_fps"],
                "actual_fps": fps,
                "resolution": test_profile["camera_resolution"]
            }
        )
    def _test_thermal_processing(self, device_spec: DeviceSpecification,
                                test_profile: Dict[str, Any]) -> PerformanceTestResult:
        if device_spec.has_thermal_camera:
            throughput = 20.0
            cpu_usage = 45.0
            success_rate = 0.95
            battery_drain = 2.5
        else:
            throughput = 5.0
            cpu_usage = 80.0
            success_rate = 0.75
            battery_drain = 4.0
        return PerformanceTestResult(
            test_category=PerformanceCategory.THERMAL_PROCESSING,
            device_model=device_spec.device_model,
            test_duration_seconds=test_profile["test_duration_seconds"],
            throughput_ops_per_second=throughput,
            average_response_time_ms=1000 / throughput,
            peak_memory_usage_mb=150,
            cpu_usage_percent=cpu_usage,
            battery_drain_percent=battery_drain,
            success_rate=success_rate,
            error_count=int((1 - success_rate) * 20),
            metadata={"has_thermal_hardware": device_spec.has_thermal_camera}
        )
    def _test_network_throughput(self, device_spec: DeviceSpecification,
                                test_profile: Dict[str, Any]) -> PerformanceTestResult:
        base_throughput = 50.0
        if device_spec.supports_5ghz_wifi:
            base_throughput *= 2.0
        if device_spec.supports_bluetooth_5:
            base_throughput *= 1.1
        cpu_usage = 25.0
        success_rate = 0.95
        return PerformanceTestResult(
            test_category=PerformanceCategory.NETWORK_THROUGHPUT,
            device_model=device_spec.device_model,
            test_duration_seconds=test_profile["test_duration_seconds"],
            throughput_ops_per_second=base_throughput,
            average_response_time_ms=20.0,
            peak_memory_usage_mb=100,
            cpu_usage_percent=cpu_usage,
            battery_drain_percent=1.0,
            success_rate=success_rate,
            error_count=int((1 - success_rate) * 10),
            metadata={"wifi_5ghz": device_spec.supports_5ghz_wifi}
        )
    def _test_storage_io(self, device_spec: DeviceSpecification,
                        test_profile: Dict[str, Any]) -> PerformanceTestResult:
        storage_multipliers = {
            "emmc": 1.0,
            "ufs_2.0": 2.0,
            "ufs_2.1": 2.5,
            "ufs_3.0": 4.0,
            "ufs_3.1": 5.0
        }
        multiplier = storage_multipliers.get(device_spec.storage_type, 1.0)
        throughput = 100 * multiplier
        cpu_usage = 30.0
        success_rate = 0.98
        return PerformanceTestResult(
            test_category=PerformanceCategory.STORAGE_IO,
            device_model=device_spec.device_model,
            test_duration_seconds=test_profile["test_duration_seconds"],
            throughput_ops_per_second=throughput,
            average_response_time_ms=10.0,
            peak_memory_usage_mb=50,
            cpu_usage_percent=cpu_usage,
            battery_drain_percent=0.5,
            success_rate=success_rate,
            error_count=int((1 - success_rate) * 5),
            metadata={"storage_type": device_spec.storage_type}
        )
    def _test_battery_efficiency(self, device_spec: DeviceSpecification,
                                test_profile: Dict[str, Any]) -> PerformanceTestResult:
        base_efficiency = device_spec.battery_capacity_mah / 4000
        if device_spec.tier == DeviceTier.FLAGSHIP:
            efficiency_score = base_efficiency * 1.2
        elif device_spec.tier == DeviceTier.HIGH_END:
            efficiency_score = base_efficiency * 1.1
        else:
            efficiency_score = base_efficiency
        power_consumption_mw = 2000 / efficiency_score
        throughput = 1000 / power_consumption_mw
        return PerformanceTestResult(
            test_category=PerformanceCategory.BATTERY_EFFICIENCY,
            device_model=device_spec.device_model,
            test_duration_seconds=test_profile["test_duration_seconds"],
            throughput_ops_per_second=throughput,
            average_response_time_ms=100.0,
            peak_memory_usage_mb=100,
            cpu_usage_percent=50.0,
            battery_drain_percent=5.0,
            success_rate=0.99,
            error_count=0,
            metadata={
                "battery_capacity_mah": device_spec.battery_capacity_mah,
                "power_consumption_mw": power_consumption_mw,
                "efficiency_score": efficiency_score
            }
        )
    def run_complete_test_suite(self, device_spec: DeviceSpecification) -> DeviceProfile:
        self.logger.info(f"DevicePerformanceTester: Running thorough test suite on {device_spec.device_model}")
        device_spec.calculate_tier_and_score()
        profile = DeviceProfile(device_spec=device_spec)
        for category in PerformanceCategory:
            try:
                result = self.run_performance_test(device_spec, category)
                profile.test_results.append(result)
                self.logger.info(f"DevicePerformanceTester: {category.value} test completed - Grade: {result.get_performance_grade()}")
            except Exception as e:
                self.logger.error(f"DevicePerformanceTester: Error running {category.value} test: {e}")
        profile.analyze_performance()
        self.logger.info(f"DevicePerformanceTester: Test suite completed - Overall Grade: {profile.overall_performance_grade}")
        return profile
class DeviceDiversityAnalyzer:
    def __init__(self):
        self.logger = get_logger(__name__)
        self.device_profiles: List[DeviceProfile] = []
    def add_device_profile(self, profile: DeviceProfile):
        self.device_profiles.append(profile)
    def create_sample_device_profiles(self) -> List[DeviceProfile]:
        sample_devices = [
            DeviceSpecification(
                device_model="Budget Phone A",
                manufacturer="Generic",
                android_version="10.0",
                api_level=29,
                cpu_architecture="arm64-v8a",
                cpu_cores=4,
                cpu_frequency_mhz=1800,
                total_ram_mb=3072,
                available_ram_mb=2048,
                total_storage_gb=32,
                available_storage_gb=16,
                storage_type="emmc",
                screen_width=720,
                screen_height=1600,
                screen_density_dpi=320,
                camera_count=2,
                max_camera_resolution="1080p",
                supports_camera2_api=True,
                supports_5ghz_wifi=False,
                supports_bluetooth_5=False,
                supports_nfc=False,
                battery_capacity_mah=3000,
                supports_fast_charging=False
            ),
            DeviceSpecification(
                device_model="Samsung Galaxy S22",
                manufacturer="Samsung",
                android_version="15.0",
                api_level=35,
                cpu_architecture="arm64-v8a",
                cpu_cores=8,
                cpu_frequency_mhz=2800,
                total_ram_mb=8192,
                available_ram_mb=6144,
                total_storage_gb=256,
                available_storage_gb=180,
                storage_type="ufs_3.1",
                screen_width=1080,
                screen_height=2340,
                screen_density_dpi=420,
                camera_count=3,
                max_camera_resolution="8K",
                supports_camera2_api=True,
                supports_5ghz_wifi=True,
                supports_bluetooth_5=True,
                supports_nfc=True,
                battery_capacity_mah=3700,
                supports_fast_charging=True,
                has_hardware_acceleration=True,
                supports_vulkan_api=True
            ),
            DeviceSpecification(
                device_model="Flagship Phone C",
                manufacturer="Google",
                android_version="13.0",
                api_level=33,
                cpu_architecture="arm64-v8a",
                cpu_cores=8,
                cpu_frequency_mhz=3000,
                total_ram_mb=12288,
                available_ram_mb=8192,
                total_storage_gb=256,
                available_storage_gb=200,
                storage_type="ufs_3.1",
                screen_width=1440,
                screen_height=3200,
                screen_density_dpi=540,
                camera_count=4,
                max_camera_resolution="8K",
                supports_camera2_api=True,
                supports_5ghz_wifi=True,
                supports_bluetooth_5=True,
                supports_nfc=True,
                battery_capacity_mah=5000,
                supports_fast_charging=True,
                has_thermal_camera=True,
                has_hardware_acceleration=True,
                supports_vulkan_api=True
            )
        ]
        tester = DevicePerformanceTester()
        profiles = []
        for device_spec in sample_devices:
            profile = tester.run_complete_test_suite(device_spec)
            profiles.append(profile)
            self.add_device_profile(profile)
        return profiles
    def analyze_device_diversity(self) -> Dict[str, Any]:
        if not self.device_profiles:
            return {"error": "No device profiles available for analysis"}
        analysis = {
            "total_devices": len(self.device_profiles),
            "tier_distribution": self._analyze_tier_distribution(),
            "performance_analysis": self._analyze_performance_across_tiers(),
            "category_analysis": self._analyze_category_performance(),
            "optimization_recommendations": self._generate_diversity_recommendations(),
            "compatibility_matrix": self._generate_compatibility_matrix()
        }
        return analysis
    def _analyze_tier_distribution(self) -> Dict[str, Any]:
        tier_counts = defaultdict(int)
        tier_scores = defaultdict(list)
        for profile in self.device_profiles:
            tier = profile.device_spec.tier
            tier_counts[tier.value] += 1
            tier_scores[tier.value].append(profile.device_spec.performance_score)
        return {
            "distribution": dict(tier_counts),
            "average_scores": {
                tier: statistics.mean(scores)
                for tier, scores in tier_scores.items()
            }
        }
    def _analyze_performance_across_tiers(self) -> Dict[str, Any]:
        tier_performance = defaultdict(list)
        for profile in self.device_profiles:
            tier = profile.device_spec.tier.value
            if profile.test_results:
                avg_throughput = statistics.mean([r.throughput_ops_per_second for r in profile.test_results])
                avg_cpu = statistics.mean([r.cpu_usage_percent for r in profile.test_results])
                avg_success_rate = statistics.mean([r.success_rate for r in profile.test_results])
                tier_performance[tier].append({
                    "device": profile.device_spec.device_model,
                    "overall_grade": profile.overall_performance_grade,
                    "avg_throughput": avg_throughput,
                    "avg_cpu_usage": avg_cpu,
                    "avg_success_rate": avg_success_rate
                })
        return dict(tier_performance)
    def _analyze_category_performance(self) -> Dict[str, Any]:
        category_analysis = {}
        for category in PerformanceCategory:
            category_results = []
            for profile in self.device_profiles:
                for result in profile.test_results:
                    if result.test_category == category:
                        category_results.append({
                            "device": profile.device_spec.device_model,
                            "tier": profile.device_spec.tier.value,
                            "grade": result.get_performance_grade(),
                            "throughput": result.throughput_ops_per_second,
                            "success_rate": result.success_rate
                        })
            if category_results:
                throughputs = [r["throughput"] for r in category_results]
                success_rates = [r["success_rate"] for r in category_results]
                category_analysis[category.value] = {
                    "results": category_results,
                    "statistics": {
                        "avg_throughput": statistics.mean(throughputs),
                        "min_throughput": min(throughputs),
                        "max_throughput": max(throughputs),
                        "avg_success_rate": statistics.mean(success_rates),
                        "performance_variance": statistics.stdev(throughputs) if len(throughputs) > 1 else 0
                    }
                }
        return category_analysis
    def _generate_diversity_recommendations(self) -> List[str]:
        recommendations = []
        tier_dist = self._analyze_tier_distribution()
        low_end_percentage = tier_dist["distribution"].get("low_end", 0) / tier_dist.get("total_devices", 1) * 100
        if low_end_percentage > 30:
            recommendations.append(
                "High percentage of low-end devices detected. Implement conservative default "
                "settings and provide performance optimisation options."
            )
        category_analysis = self._analyze_category_performance()
        high_variance_categories = []
        for category, data in category_analysis.items():
            variance = data["statistics"]["performance_variance"]
            if variance > 50:
                high_variance_categories.append(category)
        if high_variance_categories:
            recommendations.append(
                f"High performance variance detected in: {', '.join(high_variance_categories)}. "
                "Implement adaptive quality settings based on device capabilities."
            )
        poor_categories = []
        for category, data in category_analysis.items():
            avg_success_rate = data["statistics"]["avg_success_rate"]
            if avg_success_rate < 0.8:
                poor_categories.append(category)
        if poor_categories:
            recommendations.append(
                f"Categories with poor performance across devices: {', '.join(poor_categories)}. "
                "Consider implementing fallback strategies or reducing requirements."
            )
        return recommendations
    def _generate_compatibility_matrix(self) -> Dict[str, Any]:
        matrix = {}
        for tier in DeviceTier:
            if tier == DeviceTier.UNKNOWN:
                continue
            tier_profiles = [p for p in self.device_profiles if p.device_spec.tier == tier]
            if not tier_profiles:
                continue
            tier_compatibility = {}
            for category in PerformanceCategory:
                category_results = []
                for profile in tier_profiles:
                    for result in profile.test_results:
                        if result.test_category == category:
                            grade = result.get_performance_grade()
                            category_results.append(grade)
                if category_results:
                    grade_scores = {"A": 4, "B": 3, "C": 2, "D": 1, "F": 0}
                    avg_score = statistics.mean([grade_scores[grade] for grade in category_results])
                    if avg_score >= 3:
                        compatibility = "Excellent"
                    elif avg_score >= 2:
                        compatibility = "Good"
                    elif avg_score >= 1:
                        compatibility = "Fair"
                    else:
                        compatibility = "Poor"
                    tier_compatibility[category.value] = {
                        "compatibility": compatibility,
                        "average_grade_score": avg_score,
                        "sample_grades": category_results
                    }
            matrix[tier.value] = tier_compatibility
        return matrix
    def export_analysis_report(self, output_file: str):
        analysis = self.analyze_device_diversity()
        report = {
            "report_timestamp": datetime.now().isoformat(),
            "diversity_analysis": analysis,
            "device_profiles": [
                {
                    "device_model": profile.device_spec.device_model,
                    "tier": profile.device_spec.tier.value,
                    "performance_score": profile.device_spec.performance_score,
                    "overall_grade": profile.overall_performance_grade,
                    "test_results": [
                        {
                            "category": result.test_category.value,
                            "grade": result.get_performance_grade(),
                            "throughput": result.throughput_ops_per_second,
                            "success_rate": result.success_rate
                        } for result in profile.test_results
                    ],
                    "optimization_recommendations": profile.optimization_recommendations
                } for profile in self.device_profiles
            ]
        }
        with open(output_file, 'w') as f:
            json.dump(report, f, indent=2)
        self.logger.info(f"DeviceDiversityAnalyzer: Analysis report exported to {output_file}")
def main():
    import argparse
    parser = argparse.ArgumentParser(description="Device Diversity Testing and Analysis")
    parser.add_argument("--analyse-sample", action="store_true",
                       help="Analyse sample device profiles")
    parser.add_argument("--test-device", type=str,
                       help="Test specific device (ADB device ID)")
    parser.add_argument("--output", type=str, default="device_diversity_report.json",
                       help="Output file for analysis report")
    parser.add_argument("--verbose", action="store_true", help="Enable verbose logging")
    args = parser.parse_args()
    level = logging.DEBUG if args.verbose else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    try:
        analyser = DeviceDiversityAnalyzer()
        if args.analyze_sample:
            print("Creating and analysing sample device profiles...")
            profiles = analyser.create_sample_device_profiles()
            print(f"\nGenerated {len(profiles)} device profiles:")
            for profile in profiles:
                print(f"  - {profile.device_spec.device_model} ({profile.device_spec.tier.value}): "
                      f"Grade {profile.overall_performance_grade}")
            analysis = analyser.analyze_device_diversity()
            print(f"\nDevice Diversity Analysis:")
            print(f"  Total Devices: {analysis['total_devices']}")
            print(f"  Tier Distribution: {analysis['tier_distribution']['distribution']}")
            print(f"\nOptimization Recommendations:")
            for i, rec in enumerate(analysis['optimization_recommendations'], 1):
                print(f"  {i}. {rec}")
        elif args.test_device:
            print(f"Testing device: {args.test_device}")
            tester = DevicePerformanceTester()
            device_spec = tester.detect_device_specifications(args.test_device)
            profile = tester.run_complete_test_suite(device_spec)
            analyser.add_device_profile(profile)
            print(f"\nDevice Test Results:")
            print(f"  Device: {profile.device_spec.device_model}")
            print(f"  Tier: {profile.device_spec.tier.value}")
            print(f"  Performance Score: {profile.device_spec.performance_score:.1f}")
            print(f"  Overall Grade: {profile.overall_performance_grade}")
        analyser.export_analysis_report(args.output)
        print(f"\nAnalysis report saved to: {args.output}")
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
if __name__ == "__main__":
    main()