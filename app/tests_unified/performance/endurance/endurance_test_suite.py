import asyncio
import gc
import json
import logging
import os
import platform
import threading
import time
import tracemalloc
from dataclasses import asdict, dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Dict, List, Optional, Callable
import psutil
try:
    import GPUtil
    GPU_AVAILABLE = True
except ImportError:
    GPU_AVAILABLE = False
try:
    import pynvml
    NVML_AVAILABLE = True
except ImportError:
    NVML_AVAILABLE = False
@dataclass
class EnduranceMetrics:
    timestamp: float
    elapsed_hours: float
    memory_rss_mb: float
    memory_vms_mb: float
    memory_percent: float
    cpu_percent: float
    thread_count: int
    open_files: int
    network_connections: int
    disk_io_read_mb: float
    disk_io_write_mb: float
    gpu_usage_percent: Optional[float] = None
    gpu_memory_mb: Optional[float] = None
    temperature_cpu: Optional[float] = None
@dataclass
class MemoryLeakDetection:
    start_memory_mb: float
    current_memory_mb: float
    peak_memory_mb: float
    leak_rate_mb_per_hour: float
    gc_collections: int
    unreachable_objects: int
    is_leak_suspected: bool
    trend_analysis: str
@dataclass
class EnduranceTestConfig:
    target_duration_hours: float = 8.0
    monitoring_interval_seconds: float = 30.0
    memory_leak_threshold_mb_per_hour: float = 10.0
    cpu_degradation_threshold_percent: float = 5.0
    enable_gpu_monitoring: bool = True
    enable_temperature_monitoring: bool = True
    enable_simulated_workload: bool = True
    workload_intensity: str = "medium"
    enable_automatic_gc: bool = True
    gc_interval_minutes: float = 5.0
    checkpoint_interval_hours: float = 1.0
    save_detailed_logs: bool = True
class SimulatedWorkload:
    def __init__(self, intensity: str = "medium", logger: Optional[logging.Logger] = None):
        self.intensity = intensity
        self.logger = logger or logging.getLogger(__name__)
        self.is_running = False
        self.metrics = {
            "frames_processed": 0,
            "messages_sent": 0,
            "calibrations_performed": 0,
            "data_written_mb": 0.0
        }
        self.workload_params = {
            "low": {
                "fps": 10,
                "resolution": (640, 480),
                "devices": 2,
                "message_rate": 5,
                "data_rate_mb_per_sec": 0.5
            },
            "medium": {
                "fps": 20,
                "resolution": (1280, 720),
                "devices": 4,
                "message_rate": 10,
                "data_rate_mb_per_sec": 2.0
            },
            "high": {
                "fps": 30,
                "resolution": (1920, 1080),
                "devices": 8,
                "message_rate": 20,
                "data_rate_mb_per_sec": 5.0
            }
        }[intensity]
    async def start_workload(self):
        self.is_running = True
        tasks = [
            self._simulate_video_processing(),
            self._simulate_network_communication(),
            self._simulate_data_storage(),
            self._simulate_sensor_processing()
        ]
        await asyncio.gather(*tasks)
    def stop_workload(self):
        self.is_running = False
    async def _simulate_video_processing(self):
        try:
            import cv2
            import numpy as np
            while self.is_running:
                frame = np.random.randint(0, 255,
                    (*self.workload_params["resolution"], 3), dtype=np.uint8)
                grey = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                blurred = cv2.GaussianBlur(grey, (5, 5), 0)
                _, encoded = cv2.imencode('.jpg', frame,
                    [cv2.IMWRITE_JPEG_QUALITY, 80])
                self.metrics["frames_processed"] += 1
                await asyncio.sleep(1.0 / self.workload_params["fps"])
        except ImportError:
            while self.is_running:
                data = bytearray(self.workload_params["resolution"][0] *
                               self.workload_params["resolution"][1] * 3)
                self.metrics["frames_processed"] += 1
                await asyncio.sleep(1.0 / self.workload_params["fps"])
        except Exception as e:
            self.logger.error(f"Video processing simulation error: {e}")
    async def _simulate_network_communication(self):
        while self.is_running:
            try:
                for device_id in range(self.workload_params["devices"]):
                    message = {
                        "type": "device_status",
                        "device_id": f"device_{device_id}",
                        "timestamp": time.time() * 1000,
                        "data": {
                            "status": "recording",
                            "battery": 85 + (device_id % 15),
                            "temperature": 35.0 + (device_id * 0.5),
                            "frame_rate": self.workload_params["fps"],
                            "quality": "high"
                        }
                    }
                    json_str = json.dumps(message)
                    parsed = json.loads(json_str)
                    self.metrics["messages_sent"] += 1
                await asyncio.sleep(1.0 / self.workload_params["message_rate"])
            except Exception as e:
                self.logger.error(f"Network simulation error: {e}")
    async def _simulate_data_storage(self):
        data_chunk_size = 1024 * 1024
        while self.is_running:
            try:
                data = bytearray(data_chunk_size)
                self.metrics["data_written_mb"] += len(data) / (1024 * 1024)
                expected_interval = data_chunk_size / (
                    self.workload_params["data_rate_mb_per_sec"] * 1024 * 1024)
                await asyncio.sleep(expected_interval)
            except Exception as e:
                self.logger.error(f"Data storage simulation error: {e}")
    async def _simulate_sensor_processing(self):
        while self.is_running:
            try:
                await asyncio.sleep(600)
                if self.is_running:
                    for _ in range(100):
                        result = sum(range(1000))
                    self.metrics["calibrations_performed"] += 1
            except Exception as e:
                self.logger.error(f"Sensor processing simulation error: {e}")
class EnduranceTestSuite:
    def __init__(self, config: EnduranceTestConfig,
                 output_dir: str = "endurance_test_results"):
        self.config = config
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        self.logger = self._setup_logging()
        self.metrics_history: List[EnduranceMetrics] = []
        self.workload: Optional[SimulatedWorkload] = None
        self.start_time: Optional[float] = None
        self.is_running = False
        self.initial_memory = 0.0
        self.gc_stats = {"collections": 0, "unreachable": 0}
        self.baseline_cpu = 0.0
        self.baseline_memory = 0.0
        self.gpu_available = GPU_AVAILABLE or NVML_AVAILABLE
        if NVML_AVAILABLE:
            try:
                pynvml.nvmlInit()
            except:
                self.gpu_available = False
    def _setup_logging(self) -> logging.Logger:
        logger = logging.getLogger(f"{__name__}.endurance")
        logger.setLevel(logging.DEBUG if self.config.save_detailed_logs else logging.INFO)
        log_file = self.output_dir / f"endurance_test_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
        file_handler = logging.FileHandler(log_file)
        file_handler.setLevel(logging.DEBUG)
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
        return logger
    async def run_endurance_test(self) -> Dict[str, Any]:
        self.logger.info(f"Starting endurance test - Target duration: {self.config.target_duration_hours} hours")
        tracemalloc.start()
        self.start_time = time.time()
        self.is_running = True
        await self._capture_baseline_metrics()
        if self.config.enable_simulated_workload:
            self.workload = SimulatedWorkload(
                self.config.workload_intensity, self.logger)
            workload_task = asyncio.create_task(self.workload.start_workload())
        monitoring_task = asyncio.create_task(self._monitoring_loop())
        gc_task = asyncio.create_task(self._garbage_collection_loop())
        checkpoint_task = asyncio.create_task(self._checkpoint_loop())
        try:
            target_seconds = self.config.target_duration_hours * 3600
            await asyncio.sleep(target_seconds)
        except KeyboardInterrupt:
            self.logger.info("Endurance test interrupted by user")
        finally:
            self.is_running = False
            if self.workload:
                self.workload.stop_workload()
                try:
                    await asyncio.wait_for(workload_task, timeout=30.0)
                except asyncio.TimeoutError:
                    self.logger.warning("Workload shutdown timed out")
            monitoring_task.cancel()
            gc_task.cancel()
            checkpoint_task.cancel()
            return await self._generate_endurance_report()
    async def _capture_baseline_metrics(self):
        process = psutil.Process()
        cpu_samples = []
        for _ in range(6):
            cpu_samples.append(process.cpu_percent())
            await asyncio.sleep(5)
        self.baseline_cpu = sum(cpu_samples) / len(cpu_samples)
        self.baseline_memory = process.memory_info().rss / 1024 / 1024
        self.initial_memory = self.baseline_memory
        self.logger.info(f"Baseline metrics - CPU: {self.baseline_cpu:.1f}%, Memory: {self.baseline_memory:.1f}MB")
    async def _monitoring_loop(self):
        while self.is_running:
            try:
                metrics = await self._collect_metrics()
                self.metrics_history.append(metrics)
                await self._analyze_trends(metrics)
                await asyncio.sleep(self.config.monitoring_interval_seconds)
            except Exception as e:
                self.logger.error(f"Error in monitoring loop: {e}")
                await asyncio.sleep(self.config.monitoring_interval_seconds)
    async def _collect_metrics(self) -> EnduranceMetrics:
        process = psutil.Process()
        memory_info = process.memory_info()
        disk_io = psutil.disk_io_counters()
        gpu_usage = None
        gpu_memory = None
        if self.gpu_available:
            gpu_usage, gpu_memory = self._get_gpu_metrics()
        cpu_temp = self._get_cpu_temperature()
        elapsed_hours = (time.time() - self.start_time) / 3600
        return EnduranceMetrics(
            timestamp=time.time(),
            elapsed_hours=elapsed_hours,
            memory_rss_mb=memory_info.rss / 1024 / 1024,
            memory_vms_mb=memory_info.vms / 1024 / 1024,
            memory_percent=process.memory_percent(),
            cpu_percent=process.cpu_percent(),
            thread_count=process.num_threads(),
            open_files=len(process.open_files()),
            network_connections=len(process.connections()),
            disk_io_read_mb=disk_io.read_bytes / 1024 / 1024 if disk_io else 0,
            disk_io_write_mb=disk_io.write_bytes / 1024 / 1024 if disk_io else 0,
            gpu_usage_percent=gpu_usage,
            gpu_memory_mb=gpu_memory,
            temperature_cpu=cpu_temp
        )
    def _get_gpu_metrics(self) -> tuple[Optional[float], Optional[float]]:
        try:
            if NVML_AVAILABLE:
                device_count = pynvml.nvmlDeviceGetCount()
                if device_count > 0:
                    handle = pynvml.nvmlDeviceGetHandleByIndex(0)
                    util = pynvml.nvmlDeviceGetUtilizationRates(handle)
                    mem_info = pynvml.nvmlDeviceGetMemoryInfo(handle)
                    return util.gpu, mem_info.used / 1024 / 1024
            elif GPU_AVAILABLE:
                gpus = GPUtil.getGPUs()
                if gpus:
                    gpu = gpus[0]
                    return gpu.load * 100, gpu.memoryUsed
        except Exception as e:
            self.logger.debug(f"GPU metrics collection failed: {e}")
        return None, None
    def _get_cpu_temperature(self) -> Optional[float]:
        try:
            if hasattr(psutil, "sensors_temperatures"):
                temps = psutil.sensors_temperatures()
                if temps and "coretemp" in temps:
                    return temps["coretemp"][0].current
        except Exception:
            pass
        return None
    async def _analyze_trends(self, current_metrics: EnduranceMetrics):
        if len(self.metrics_history) < 10:
            return
        memory_trend = self._detect_memory_leak(current_metrics)
        if memory_trend.is_leak_suspected:
            self.logger.warning(
                f"Potential memory leak detected: {memory_trend.leak_rate_mb_per_hour:.2f} MB/hour"
            )
        cpu_degradation = self._detect_cpu_degradation(current_metrics)
        if cpu_degradation > self.config.cpu_degradation_threshold_percent:
            self.logger.warning(
                f"CPU performance degradation detected: {cpu_degradation:.1f}% above baseline"
            )
    def _detect_memory_leak(self, current_metrics: EnduranceMetrics) -> MemoryLeakDetection:
        recent_metrics = self.metrics_history[-60:]
        if len(recent_metrics) < 10:
            return MemoryLeakDetection(
                start_memory_mb=self.initial_memory,
                current_memory_mb=current_metrics.memory_rss_mb,
                peak_memory_mb=max(m.memory_rss_mb for m in self.metrics_history),
                leak_rate_mb_per_hour=0.0,
                gc_collections=self.gc_stats["collections"],
                unreachable_objects=self.gc_stats["unreachable"],
                is_leak_suspected=False,
                trend_analysis="Insufficient data"
            )
        memory_values = [m.memory_rss_mb for m in recent_metrics]
        time_values = [m.elapsed_hours for m in recent_metrics]
        n = len(memory_values)
        sum_x = sum(time_values)
        sum_y = sum(memory_values)
        sum_xy = sum(x * y for x, y in zip(time_values, memory_values))
        sum_x2 = sum(x * x for x in time_values)
        slope = (n * sum_xy - sum_x * sum_y) / (n * sum_x2 - sum_x * sum_x)
        leak_rate_mb_per_hour = slope
        is_leak_suspected = (
            leak_rate_mb_per_hour > self.config.memory_leak_threshold_mb_per_hour
        )
        trend_analysis = "Increasing" if slope > 0 else "Stable/Decreasing"
        return MemoryLeakDetection(
            start_memory_mb=self.initial_memory,
            current_memory_mb=current_metrics.memory_rss_mb,
            peak_memory_mb=max(m.memory_rss_mb for m in self.metrics_history),
            leak_rate_mb_per_hour=leak_rate_mb_per_hour,
            gc_collections=self.gc_stats["collections"],
            unreachable_objects=self.gc_stats["unreachable"],
            is_leak_suspected=is_leak_suspected,
            trend_analysis=trend_analysis
        )
    def _detect_cpu_degradation(self, current_metrics: EnduranceMetrics) -> float:
        recent_metrics = self.metrics_history[-20:]
        if len(recent_metrics) < 10:
            return 0.0
        avg_recent_cpu = sum(m.cpu_percent for m in recent_metrics) / len(recent_metrics)
        degradation_percent = ((avg_recent_cpu - self.baseline_cpu) / self.baseline_cpu) * 100
        return max(0.0, degradation_percent)
    async def _garbage_collection_loop(self):
        if not self.config.enable_automatic_gc:
            return
        interval_seconds = self.config.gc_interval_minutes * 60
        while self.is_running:
            try:
                await asyncio.sleep(interval_seconds)
                if self.is_running:
                    collected = gc.collect()
                    self.gc_stats["collections"] += 1
                    self.gc_stats["unreachable"] += collected
                    if collected > 0:
                        self.logger.debug(f"Garbage collection freed {collected} objects")
            except Exception as e:
                self.logger.error(f"Error in garbage collection loop: {e}")
    async def _checkpoint_loop(self):
        interval_seconds = self.config.checkpoint_interval_hours * 3600
        while self.is_running:
            try:
                await asyncio.sleep(interval_seconds)
                if self.is_running:
                    await self._generate_checkpoint_report()
            except Exception as e:
                self.logger.error(f"Error in checkpoint loop: {e}")
    async def _generate_checkpoint_report(self):
        if not self.metrics_history:
            return
        current_metrics = self.metrics_history[-1]
        elapsed_hours = current_metrics.elapsed_hours
        hour_metrics = [m for m in self.metrics_history
                       if current_metrics.elapsed_hours - m.elapsed_hours <= 1.0]
        if hour_metrics:
            avg_cpu = sum(m.cpu_percent for m in hour_metrics) / len(hour_metrics)
            avg_memory = sum(m.memory_rss_mb for m in hour_metrics) / len(hour_metrics)
            checkpoint_data = {
                "elapsed_hours": elapsed_hours,
                "current_memory_mb": current_metrics.memory_rss_mb,
                "memory_change_from_start": current_metrics.memory_rss_mb - self.initial_memory,
                "average_cpu_last_hour": avg_cpu,
                "average_memory_last_hour": avg_memory,
                "thread_count": current_metrics.thread_count,
                "open_files": current_metrics.open_files,
                "workload_metrics": self.workload.metrics if self.workload else None
            }
            checkpoint_file = self.output_dir / f"checkpoint_{elapsed_hours:.1f}h.json"
            with open(checkpoint_file, 'w') as f:
                json.dump(checkpoint_data, f, indent=2, default=str)
            self.logger.info(
                f"Checkpoint at {elapsed_hours:.1f}h - "
                f"Memory: {current_metrics.memory_rss_mb:.1f}MB "
                f"(+{current_metrics.memory_rss_mb - self.initial_memory:.1f}MB), "
                f"CPU: {avg_cpu:.1f}%"
            )
    async def _generate_endurance_report(self) -> Dict[str, Any]:
        if not self.metrics_history:
            return {"error": "No metrics collected"}
        final_metrics = self.metrics_history[-1]
        memory_leak_analysis = self._detect_memory_leak(final_metrics)
        total_duration = final_metrics.elapsed_hours
        memory_values = [m.memory_rss_mb for m in self.metrics_history]
        cpu_values = [m.cpu_percent for m in self.metrics_history]
        report = {
            "test_summary": {
                "start_time": datetime.fromtimestamp(self.start_time).isoformat(),
                "end_time": datetime.fromtimestamp(time.time()).isoformat(),
                "total_duration_hours": total_duration,
                "target_duration_hours": self.config.target_duration_hours,
                "completion_percentage": (total_duration / self.config.target_duration_hours) * 100,
                "samples_collected": len(self.metrics_history)
            },
            "memory_analysis": {
                "initial_memory_mb": self.initial_memory,
                "final_memory_mb": final_metrics.memory_rss_mb,
                "peak_memory_mb": max(memory_values),
                "memory_growth_mb": final_metrics.memory_rss_mb - self.initial_memory,
                "memory_growth_percent": ((final_metrics.memory_rss_mb - self.initial_memory) / self.initial_memory) * 100,
                "leak_detection": asdict(memory_leak_analysis)
            },
            "performance_analysis": {
                "baseline_cpu_percent": self.baseline_cpu,
                "final_cpu_percent": final_metrics.cpu_percent,
                "average_cpu_percent": sum(cpu_values) / len(cpu_values),
                "peak_cpu_percent": max(cpu_values),
                "cpu_degradation": self._detect_cpu_degradation(final_metrics),
                "thread_count_final": final_metrics.thread_count,
                "open_files_final": final_metrics.open_files
            },
            "system_stability": {
                "gc_collections": self.gc_stats["collections"],
                "objects_collected": self.gc_stats["unreachable"],
                "network_connections": final_metrics.network_connections,
                "disk_io_total_read_mb": final_metrics.disk_io_read_mb,
                "disk_io_total_write_mb": final_metrics.disk_io_write_mb
            },
            "workload_performance": self.workload.metrics if self.workload else None,
            "recommendations": self._generate_endurance_recommendations(memory_leak_analysis, final_metrics),
            "detailed_metrics": [asdict(m) for m in self.metrics_history]
        }
        report_file = self.output_dir / f"endurance_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(report_file, 'w') as f:
            json.dump(report, f, indent=2, default=str)
        await self._save_endurance_summary(report)
        self.logger.info(f"Endurance test completed - Report saved to {report_file}")
        return report
    def _generate_endurance_recommendations(self, memory_analysis: MemoryLeakDetection,
                                          final_metrics: EnduranceMetrics) -> List[str]:
        recommendations = []
        if memory_analysis.is_leak_suspected:
            recommendations.append(
                f"CRITICAL: Memory leak detected ({memory_analysis.leak_rate_mb_per_hour:.1f} MB/hour). "
                "Use memory profilers to identify leak sources. Consider implementing more frequent garbage collection."
            )
        elif memory_analysis.current_memory_mb > memory_analysis.start_memory_mb * 1.5:
            recommendations.append(
                "WARNING: Significant memory growth detected. Review memory usage patterns and implement cleanup strategies."
            )
        cpu_degradation = self._detect_cpu_degradation(final_metrics)
        if cpu_degradation > 10:
            recommendations.append(
                f"CPU performance degraded by {cpu_degradation:.1f}%. "
                "Consider optimising algorithms or implementing performance throttling."
            )
        if final_metrics.thread_count > 50:
            recommendations.append(
                f"High thread count ({final_metrics.thread_count}). Review thread pool usage and implement thread limits."
            )
        if final_metrics.open_files > 100:
            recommendations.append(
                f"High number of open files ({final_metrics.open_files}). Ensure proper file handle cleanup."
            )
        if final_metrics.gpu_usage_percent and final_metrics.gpu_usage_percent > 80:
            recommendations.append(
                "High GPU usage detected. Consider GPU-based optimisations or workload distribution."
            )
        if not recommendations:
            recommendations.append("System performed well during endurance testing. No major issues detected.")
        return recommendations
    async def _save_endurance_summary(self, report: Dict[str, Any]):
        summary_file = self.output_dir / f"endurance_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        with open(summary_file, 'w') as f:
            f.write("=== ENDURANCE TEST SUMMARY ===\n\n")
            summary = report["test_summary"]
            f.write(f"Duration: {summary['total_duration_hours']:.1f} / {summary['target_duration_hours']:.1f} hours ")
            f.write(f"({summary['completion_percentage']:.1f}%)\n")
            f.write(f"Start: {summary['start_time']}\n")
            f.write(f"End: {summary['end_time']}\n")
            f.write(f"Samples: {summary['samples_collected']}\n\n")
            memory = report["memory_analysis"]
            f.write("=== MEMORY ANALYSIS ===\n")
            f.write(f"Initial: {memory['initial_memory_mb']:.1f} MB\n")
            f.write(f"Final: {memory['final_memory_mb']:.1f} MB\n")
            f.write(f"Peak: {memory['peak_memory_mb']:.1f} MB\n")
            f.write(f"Growth: {memory['memory_growth_mb']:.1f} MB ({memory['memory_growth_percent']:.1f}%)\n")
            leak = memory["leak_detection"]
            f.write(f"Leak Rate: {leak['leak_rate_mb_per_hour']:.2f} MB/hour\n")
            f.write(f"Leak Suspected: {leak['is_leak_suspected']}\n\n")
            perf = report["performance_analysis"]
            f.write("=== PERFORMANCE ANALYSIS ===\n")
            f.write(f"CPU Baseline: {perf['baseline_cpu_percent']:.1f}%\n")
            f.write(f"CPU Final: {perf['final_cpu_percent']:.1f}%\n")
            f.write(f"CPU Average: {perf['average_cpu_percent']:.1f}%\n")
            f.write(f"CPU Degradation: {perf['cpu_degradation']:.1f}%\n")
            f.write(f"Threads: {perf['thread_count_final']}\n")
            f.write(f"Open Files: {perf['open_files_final']}\n\n")
            f.write("=== RECOMMENDATIONS ===\n")
            for i, rec in enumerate(report["recommendations"], 1):
                f.write(f"{i}. {rec}\n")
async def run_endurance_test(
    duration_hours: float = 8.0,
    workload_intensity: str = "medium",
    output_dir: str = "endurance_test_results"
) -> Dict[str, Any]:
    config = EnduranceTestConfig(
        target_duration_hours=duration_hours,
        workload_intensity=workload_intensity,
        enable_simulated_workload=True,
        enable_gpu_monitoring=True,
        enable_temperature_monitoring=True
    )
    suite = EnduranceTestSuite(config, output_dir)
    return await suite.run_endurance_test()
if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Run endurance test suite")
    parser.add_argument("--duration", type=float, default=8.0,
                       help="Test duration in hours (default: 8.0)")
    parser.add_argument("--intensity", choices=["low", "medium", "high"],
                       default="medium", help="Workload intensity")
    parser.add_argument("--output", default="endurance_test_results",
                       help="Output directory")
    parser.add_argument("--quick", action="store_true",
                       help="Quick test (10 minutes)")
    args = parser.parse_args()
    if args.quick:
        duration = 10.0 / 60.0
        print("Running quick endurance test (10 minutes)")
    else:
        duration = args.duration
        print(f"Running endurance test for {duration} hours")
    try:
        result = asyncio.run(run_endurance_test(
            duration_hours=duration,
            workload_intensity=args.intensity,
            output_dir=args.output
        ))
        print(f"\nEndurance test completed!")
        print(f"Duration: {result['test_summary']['total_duration_hours']:.1f} hours")
        print(f"Memory growth: {result['memory_analysis']['memory_growth_mb']:.1f} MB")
        print(f"Leak detected: {result['memory_analysis']['leak_detection']['is_leak_suspected']}")
        print(f"Results saved to: {args.output}")
    except KeyboardInterrupt:
        print("\nEndurance test interrupted")
    except Exception as e:
        print(f"Endurance test failed: {e}")
        import traceback
        traceback.print_exc()