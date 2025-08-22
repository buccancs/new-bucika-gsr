import asyncio
import gc
import json
import os
import platform
import statistics
import sys
import threading
import time
import tracemalloc
from contextlib import contextmanager
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
import psutil
current_dir = Path(__file__).parent
src_dir = current_dir.parent
sys.path.insert(0, str(src_dir))
from ..utils.logging_config import get_logger
try:
    import cv2
    import numpy as np
except ImportError:
    print("Warning: OpenCV and NumPy not available for performance tests")
    cv2 = None
    np = None
@dataclass
class PerformanceBenchmark:
    test_name: str
    duration_seconds: float
    memory_usage_mb: float
    cpu_usage_percent: float
    throughput_ops_per_sec: float
    success: bool
    error_message: Optional[str] = None
    metadata: Dict[str, Any] = None
@dataclass
class SystemInfo:
    platform: str
    python_version: str
    cpu_count: int
    total_memory_gb: float
    available_memory_gb: float
    timestamp: str
class PerformanceProfiler:
    def __init__(self, test_name: str):
        self.test_name = test_name
        self.start_time = None
        self.process = psutil.Process()
        self.initial_memory = None
        self.peak_memory = None
    def __enter__(self):
        tracemalloc.start()
        self.start_time = time.perf_counter()
        self.initial_memory = self.process.memory_info().rss / 1024 / 1024
        self.peak_memory = self.initial_memory
        self.process.cpu_percent()
        return self
    def __exit__(self, exc_type, exc_val, exc_tb):
        tracemalloc.stop()
    def get_current_memory(self) -> float:
        memory = self.process.memory_info().rss / 1024 / 1024
        self.peak_memory = max(self.peak_memory, memory)
        return memory
    def get_cpu_usage(self) -> float:
        return self.process.cpu_percent()
    def get_duration(self) -> float:
        return time.perf_counter() - self.start_time if self.start_time else 0.0
class PerformanceBenchmarkSuite:
    def __init__(self, output_dir: str = "performance_reports"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        self.logger = get_logger(__name__)
        self.results: List[PerformanceBenchmark] = []
        self.system_info = self._get_system_info()
    def _get_system_info(self) -> SystemInfo:
        return SystemInfo(
            platform=platform.platform(),
            python_version=platform.python_version(),
            cpu_count=psutil.cpu_count(),
            total_memory_gb=psutil.virtual_memory().total / 1024**3,
            available_memory_gb=psutil.virtual_memory().available / 1024**3,
            timestamp=datetime.now().isoformat(),
        )
    async def run_all_benchmarks(self) -> Dict[str, Any]:
        self.logger.info("Starting complete performance benchmark suite")
        await self._benchmark_memory_allocation()
        await self._benchmark_cpu_intensive_task()
        await self._benchmark_file_io()
        await self._benchmark_network_simulation()
        if cv2 and np:
            await self._benchmark_image_processing()
            await self._benchmark_video_processing()
        await self._benchmark_json_processing()
        await self._benchmark_concurrent_operations()
        await self._benchmark_memory_stress()
        await self._benchmark_multithreading()
        report = self._generate_report()
        self._save_report(report)
        return report
    async def _benchmark_memory_allocation(self):
        with PerformanceProfiler("memory_allocation") as profiler:
            try:
                data_blocks = []
                for i in range(100):
                    block = bytearray(10 * 1024 * 1024)
                    data_blocks.append(block)
                    if i % 20 == 0:
                        gc.collect()
                ops_per_sec = 100 / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="memory_allocation",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "allocated_blocks": len(data_blocks),
                            "block_size_mb": 10,
                            "peak_memory_mb": profiler.peak_memory,
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="memory_allocation",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_cpu_intensive_task(self):
        with PerformanceProfiler("cpu_intensive") as profiler:
            try:
                result = 0
                iterations = 1000000
                for i in range(iterations):
                    result += i * i * 0.5
                ops_per_sec = iterations / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="cpu_intensive",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={"iterations": iterations, "final_result": result},
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="cpu_intensive",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_file_io(self):
        with PerformanceProfiler("file_io") as profiler:
            try:
                test_file = self.output_dir / "test_io.dat"
                data = b"x" * 1024 * 1024
                files_written = 0
                for i in range(50):
                    test_file_i = self.output_dir / f"test_io_{i}.dat"
                    test_file_i.write_bytes(data)
                    files_written += 1
                files_read = 0
                for i in range(50):
                    test_file_i = self.output_dir / f"test_io_{i}.dat"
                    _ = test_file_i.read_bytes()
                    files_read += 1
                for i in range(50):
                    test_file_i = self.output_dir / f"test_io_{i}.dat"
                    test_file_i.unlink(missing_ok=True)
                ops_per_sec = (files_written + files_read) / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="file_io",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "files_written": files_written,
                            "files_read": files_read,
                            "file_size_mb": 1,
                            "total_data_mb": 50,
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="file_io",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_network_simulation(self):
        with PerformanceProfiler("network_simulation") as profiler:
            try:
                messages_processed = 0
                for i in range(1000):
                    message = {
                        "type": "preview_frame",
                        "frame_id": i,
                        "timestamp": time.time() * 1000,
                        "image_data": "x" * 1024,
                        "width": 640,
                        "height": 480,
                    }
                    json_str = json.dumps(message)
                    parsed = json.loads(json_str)
                    await asyncio.sleep(0.001)
                    messages_processed += 1
                ops_per_sec = messages_processed / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="network_simulation",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "messages_processed": messages_processed,
                            "message_size_bytes": len(json_str),
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="network_simulation",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_image_processing(self):
        if not cv2 or not np:
            return
        with PerformanceProfiler("image_processing") as profiler:
            try:
                images_processed = 0
                for i in range(100):
                    image = np.random.randint(0, 255, (480, 640, 3), dtype=np.uint8)
                    grey = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
                    blurred = cv2.GaussianBlur(grey, (15, 15), 0)
                    edges = cv2.Canny(blurred, 50, 150)
                    resized = cv2.resize(image, (320, 240))
                    images_processed += 1
                ops_per_sec = images_processed / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="image_processing",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "images_processed": images_processed,
                            "image_size": "640x480",
                            "operations": [
                                "color_conversion",
                                "blur",
                                "edge_detection",
                                "resize",
                            ],
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="image_processing",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_video_processing(self):
        if not cv2 or not np:
            return
        with PerformanceProfiler("video_processing") as profiler:
            try:
                frames_processed = 0
                for frame_num in range(300):
                    frame = np.random.randint(0, 255, (1080, 1920, 3), dtype=np.uint8)
                    _, encoded = cv2.imencode(
                        ".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 80]
                    )
                    decoded = cv2.imdecode(encoded, cv2.IMREAD_COLOR)
                    preview = cv2.resize(decoded, (640, 480))
                    frames_processed += 1
                    if frame_num % 30 == 0:
                        await asyncio.sleep(0.01)
                ops_per_sec = frames_processed / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="video_processing",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "frames_processed": frames_processed,
                            "frame_size": "1920x1080",
                            "target_fps": 30,
                            "operations": ["encode", "decode", "resize"],
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="video_processing",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_json_processing(self):
        with PerformanceProfiler("json_processing") as profiler:
            try:
                operations = 0
                test_data = {
                    "session_id": "test_session_123",
                    "recordings": [
                        {
                            "id": f"recording_{i}",
                            "timestamp": time.time() + i,
                            "duration": 120.5 + i,
                            "file_size": 1024 * 1024 * (i + 1),
                            "metadata": {
                                "camera_settings": {
                                    "resolution": "1920x1080",
                                    "fps": 30,
                                    "bitrate": 5000000,
                                },
                                "sensors": [
                                    {
                                        "type": "shimmer",
                                        "id": f"shimmer_{j}",
                                        "data": list(range(1000)),
                                    }
                                    for j in range(5)
                                ],
                            },
                        }
                        for i in range(50)
                    ],
                }
                for _ in range(100):
                    json_str = json.dumps(test_data)
                    parsed = json.loads(json_str)
                    assert len(parsed["recordings"]) == 50
                    operations += 2
                ops_per_sec = operations / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="json_processing",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "operations": operations,
                            "json_size_bytes": len(json_str),
                            "recordings_count": 50,
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="json_processing",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_concurrent_operations(self):
        with PerformanceProfiler("concurrent_operations") as profiler:
            try:
                async def worker_task(worker_id: int, work_items: int) -> int:
                    completed = 0
                    for i in range(work_items):
                        await asyncio.sleep(0.01)
                        result = sum(range(100))
                        completed += 1
                    return completed
                workers = 10
                work_per_worker = 20
                tasks = [worker_task(i, work_per_worker) for i in range(workers)]
                results = await asyncio.gather(*tasks)
                total_operations = sum(results)
                ops_per_sec = total_operations / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="concurrent_operations",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "workers": workers,
                            "work_per_worker": work_per_worker,
                            "total_operations": total_operations,
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="concurrent_operations",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_memory_stress(self):
        with PerformanceProfiler("memory_stress") as profiler:
            try:
                allocations = 0
                for cycle in range(10):
                    large_blocks = []
                    for i in range(50):
                        block = bytearray(20 * 1024 * 1024)
                        large_blocks.append(block)
                        allocations += 1
                    gc.collect()
                    large_blocks.clear()
                    gc.collect()
                    await asyncio.sleep(0.1)
                ops_per_sec = allocations / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="memory_stress",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "allocations": allocations,
                            "cycles": 10,
                            "block_size_mb": 20,
                            "peak_memory_mb": profiler.peak_memory,
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="memory_stress",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    async def _benchmark_multithreading(self):
        with PerformanceProfiler("multithreading") as profiler:
            try:
                def cpu_bound_task(iterations: int) -> int:
                    result = 0
                    for i in range(iterations):
                        result += i * i
                    return result
                import concurrent.futures
                with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
                    futures = []
                    iterations_per_task = 100000
                    num_tasks = 20
                    for i in range(num_tasks):
                        future = executor.submit(cpu_bound_task, iterations_per_task)
                        futures.append(future)
                    results = [future.result() for future in futures]
                total_operations = num_tasks * iterations_per_task
                ops_per_sec = total_operations / profiler.get_duration()
                self.results.append(
                    PerformanceBenchmark(
                        test_name="multithreading",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=ops_per_sec,
                        success=True,
                        metadata={
                            "num_tasks": num_tasks,
                            "iterations_per_task": iterations_per_task,
                            "total_operations": total_operations,
                            "worker_threads": 4,
                        },
                    )
                )
            except Exception as e:
                self.results.append(
                    PerformanceBenchmark(
                        test_name="multithreading",
                        duration_seconds=profiler.get_duration(),
                        memory_usage_mb=profiler.get_current_memory(),
                        cpu_usage_percent=profiler.get_cpu_usage(),
                        throughput_ops_per_sec=0.0,
                        success=False,
                        error_message=str(e),
                    )
                )
    def _generate_report(self) -> Dict[str, Any]:
        successful_tests = [r for r in self.results if r.success]
        failed_tests = [r for r in self.results if not r.success]
        if successful_tests:
            durations = [r.duration_seconds for r in successful_tests]
            memory_usage = [r.memory_usage_mb for r in successful_tests]
            cpu_usage = [r.cpu_usage_percent for r in successful_tests]
            throughput = [r.throughput_ops_per_sec for r in successful_tests]
            stats = {
                "duration": {
                    "mean": statistics.mean(durations),
                    "median": statistics.median(durations),
                    "min": min(durations),
                    "max": max(durations),
                },
                "memory_usage_mb": {
                    "mean": statistics.mean(memory_usage),
                    "median": statistics.median(memory_usage),
                    "min": min(memory_usage),
                    "max": max(memory_usage),
                },
                "cpu_usage_percent": {
                    "mean": statistics.mean(cpu_usage),
                    "median": statistics.median(cpu_usage),
                    "min": min(cpu_usage),
                    "max": max(cpu_usage),
                },
                "throughput_ops_per_sec": {
                    "mean": statistics.mean(throughput),
                    "median": statistics.median(throughput),
                    "min": min(throughput),
                    "max": max(throughput),
                },
            }
        else:
            stats = {}
        return {
            "system_info": asdict(self.system_info),
            "benchmark_summary": {
                "total_tests": len(self.results),
                "successful_tests": len(successful_tests),
                "failed_tests": len(failed_tests),
                "success_rate": (
                    len(successful_tests) / len(self.results) if self.results else 0.0
                ),
            },
            "performance_statistics": stats,
            "detailed_results": [asdict(r) for r in self.results],
            "recommendations": self._generate_recommendations(),
        }
    def _generate_recommendations(self) -> List[str]:
        recommendations = []
        successful_tests = [r for r in self.results if r.success]
        if not successful_tests:
            return ["No successful tests to analyse"]
        avg_memory = statistics.mean([r.memory_usage_mb for r in successful_tests])
        if avg_memory > 1000:
            recommendations.append(
                f"High memory usage detected (avg: {avg_memory:.1f}MB). Consider implementing memory pooling or streaming for large data."
            )
        avg_cpu = statistics.mean([r.cpu_usage_percent for r in successful_tests])
        if avg_cpu > 80:
            recommendations.append(
                f"High CPU usage detected (avg: {avg_cpu:.1f}%). Consider optimising CPU-intensive operations or using multiprocessing."
            )
        throughputs = [r.throughput_ops_per_sec for r in successful_tests]
        if throughputs:
            min_throughput = min(throughputs)
            if min_throughput < 10:
                recommendations.append(
                    f"Low throughput detected (min: {min_throughput:.1f} ops/sec). Consider optimising slow operations or implementing caching."
                )
        failed_tests = [r for r in self.results if not r.success]
        if failed_tests:
            recommendations.append(
                f"Failed tests detected: {[r.test_name for r in failed_tests]}. Review error messages and fix stability issues."
            )
        if not recommendations:
            recommendations.append("Performance looks good! No major issues detected.")
        return recommendations
    def _save_report(self, report: Dict[str, Any]):
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = self.output_dir / f"performance_benchmark_{timestamp}.json"
        with open(report_file, "w") as f:
            json.dump(report, f, indent=2, default=str)
        self.logger.info(f"Performance benchmark report saved to: {report_file}")
        summary_file = self.output_dir / f"performance_summary_{timestamp}.txt"
        with open(summary_file, "w") as f:
            f.write("=== Performance Benchmark Summary ===\n\n")
            f.write(f"System: {report['system_info']['platform']}\n")
            f.write(f"Python: {report['system_info']['python_version']}\n")
            f.write(f"CPU Cores: {report['system_info']['cpu_count']}\n")
            f.write(f"Memory: {report['system_info']['total_memory_gb']:.1f}GB\n\n")
            summary = report["benchmark_summary"]
            f.write(f"Tests Run: {summary['total_tests']}\n")
            f.write(f"Success Rate: {summary['success_rate']:.1%}\n\n")
            if report["performance_statistics"]:
                stats = report["performance_statistics"]
                f.write("Performance Statistics:\n")
                f.write(f"  Average Duration: {stats['duration']['mean']:.2f}s\n")
                f.write(f"  Average Memory: {stats['memory_usage_mb']['mean']:.1f}MB\n")
                f.write(f"  Average CPU: {stats['cpu_usage_percent']['mean']:.1f}%\n")
                f.write(
                    f"""  Average Throughput: {stats['throughput_ops_per_sec']['mean']:.1f} ops/sec

"""
                )
            f.write("Recommendations:\n")
            for rec in report["recommendations"]:
                f.write(f"  - {rec}\n")
async def main():
    print("Starting Phase 4 Performance Benchmark Suite...")
    benchmark = PerformanceBenchmark()
    report = await benchmark.run_complete_suite()
    print(f"\nBenchmark completed!")
    print(f"Total tests: {report['summary']['total_tests']}")
    print(f"Successful: {report['summary']['successful_tests']}")
    print(f"Failed: {report['summary']['failed_tests']}")
    print(f"Success rate: {report['summary']['success_rate']:.1%}")
    if report['performance_statistics']:
        stats = report['performance_statistics']
        print(f"\nPerformance Summary:")
        print(f"  Average duration: {stats['duration']['mean']:.2f}s")
        print(f"  Average memory: {stats['memory_usage_mb']['mean']:.1f}MB")
        print(f"  Average CPU: {stats['cpu_usage_percent']['mean']:.1f}%")
        print(f"  Average throughput: {stats['throughput_ops_per_sec']['mean']:.1f} ops/sec")
    if report['recommendations']:
        print(f"\nRecommendations:")
        for rec in report['recommendations']:
            print(f"  * {rec}")
    print(f"\nDetailed report saved to: performance_benchmark_report.txt")
if __name__ == "__main__":
    asyncio.run(main())