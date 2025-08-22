import asyncio
import logging
import time
from typing import Dict, Any, List
import sys
from pathlib import Path
current_dir = Path(__file__).parent
src_dir = current_dir.parent.parent
sys.path.insert(0, str(src_dir))
from tests_unified.evaluation.test_framework import TestSuite, TestResult
from PythonApp.performance_optimizer import PerformanceManager, OptimizationConfig
from PythonApp.production.endurance_test_suite import EnduranceTestSuite, EnduranceTestConfig
from PythonApp.production.device_capability_detector import DeviceCapabilityDetector
class PerformanceOptimizationTestSuite(TestSuite):
    def __init__(self):
        super().__init__(
            name="performance_optimization",
            category="PERFORMANCE",
            description="Performance optimisation and endurance testing capabilities"
        )
        self.logger = logging.getLogger(__name__)
    async def run_all_tests(self) -> List[TestResult]:
        """Run all performance optimisation tests"""
        results = []
        test_methods = [
            ("test_graceful_degradation", "Graceful degradation mechanisms"),
            ("test_hardware_acceleration", "Hardware acceleration detection"),
            ("test_profiling_integration", "Performance profiling integration"),
            ("test_device_capability_detection", "Device capability detection"),
            ("test_endurance_monitoring", "Short endurance test (1 minute)"),
            ("test_memory_leak_detection", "Memory leak detection capabilities"),
            ("test_adaptive_quality_control", "Adaptive quality control"),
            ("test_performance_degradation_alerts", "Performance degradation alerts")
        ]
        for test_method, description in test_methods:
            self.logger.info(f"Running {test_method}: {description}")
            try:
                result = await getattr(self, test_method)()
                results.append(result)
            except Exception as e:
                results.append(TestResult(
                    test_name=test_method,
                    success=False,
                    error_message=str(e),
                    duration=0.0
                ))
        return results
    async def test_graceful_degradation(self) -> TestResult:
        """Test graceful degradation mechanisms"""
        try:
            config = OptimizationConfig(
                enable_graceful_degradation=True,
                frame_drop_cpu_threshold=70.0,
                frame_drop_memory_threshold=80.0
            )
            manager = PerformanceManager()
            if not manager.initialize(config):
                return TestResult(
                    test_name="test_graceful_degradation",
                    success=False,
                    error_message="Failed to initialize performance manager",
                    duration=0.0
                )
            start_time = time.time()
            manager.start()
            await asyncio.sleep(3)
            degradation_mgr = manager.get_degradation_manager()
            if not degradation_mgr:
                manager.stop()
                return TestResult(
                    test_name="test_graceful_degradation",
                    success=False,
                    error_message="Degradation manager not available",
                    duration=time.time() - start_time
                )
            should_drop_normal = degradation_mgr.should_drop_frame(50.0, 60.0)
            should_drop_high = degradation_mgr.should_drop_frame(90.0, 95.0)
            should_reduce_normal = degradation_mgr.should_reduce_quality(50.0, 60.0)
            should_reduce_high = degradation_mgr.should_reduce_quality(85.0, 85.0)
            status = degradation_mgr.get_degradation_status()
            manager.stop()
            success = (
                not should_drop_normal and should_drop_high and
                not should_reduce_normal and should_reduce_high and
                "state" in status and "frame_drop_rate" in status
            )
            return TestResult(
                test_name="test_graceful_degradation",
                success=success,
                execution_time=time.time() - start_time,
                error_message="Degradation manager not available" if not success else None,
                custom_metrics={
                    "frame_drop_normal_load": should_drop_normal,
                    "frame_drop_high_load": should_drop_high,
                    "quality_reduce_normal": should_reduce_normal,
                    "quality_reduce_high": should_reduce_high,
                    "degradation_status": status
                }
            )
        except Exception as e:
            return TestResult(
                test_name="test_graceful_degradation",
                success=False,
                error_message=str(e),
                duration=time.time() - start_time if 'start_time' in locals() else 0.0
            )
    async def test_hardware_acceleration(self) -> TestResult:
        """Test hardware acceleration detection and capabilities"""
        try:
            start_time = time.time()
            config = OptimizationConfig(enable_hardware_acceleration=True)
            manager = PerformanceManager()
            if not manager.initialize(config):
                return TestResult(
                    test_name="test_hardware_acceleration",
                    success=False,
                    error_message="Failed to initialize performance manager",
                    duration=time.time() - start_time
                )
            manager.start()
            await asyncio.sleep(2)
            hardware_mgr = manager.get_hardware_manager()
            if not hardware_mgr:
                manager.stop()
                return TestResult(
                    test_name="test_hardware_acceleration",
                    success=False,
                    error_message="Hardware manager not available",
                    duration=time.time() - start_time
                )
            capabilities = hardware_mgr.get_capabilities_report()
            optimal_device = hardware_mgr.get_optimal_processing_device()
            try:
                video_writer = hardware_mgr.create_optimized_video_writer(
                    "/tmp/test_video.mp4", 30.0, (640, 480))
                video_writer_created = video_writer is not None
                if video_writer and hasattr(video_writer, 'release'):
                    video_writer.release()
            except:
                video_writer_created = False
            manager.stop()
            success = (
                "acceleration_enabled" in capabilities and
                "capabilities" in capabilities and
                optimal_device in ["cpu", "cuda", "opencl", "opencv_gpu"] and
                video_writer_created
            )
            return TestResult(
                test_name="test_hardware_acceleration",
                success=success,
                details={
                    "capabilities": capabilities,
                    "optimal_device": optimal_device,
                    "video_writer_created": video_writer_created
                },
                duration=time.time() - start_time
            )
        except Exception as e:
            return TestResult(
                test_name="test_hardware_acceleration",
                success=False,
                error_message=str(e),
                duration=time.time() - start_time if 'start_time' in locals() else 0.0
            )
    async def test_profiling_integration(self) -> TestResult:
        """Test performance profiling integration"""
        try:
            start_time = time.time()
            config = OptimizationConfig(enable_profiling_integration=True)
            manager = PerformanceManager()
            if not manager.initialize(config):
                return TestResult(
                    test_name="test_profiling_integration",
                    success=False,
                    error_message="Failed to initialize performance manager",
                    duration=time.time() - start_time
                )
            manager.start()
            await asyncio.sleep(1)
            profiling_started = manager.start_profiling("cprofile")
            if profiling_started:
                await asyncio.sleep(1)
                results = manager.stop_profiling("cprofile", save_results=False)
                profiling_stopped = results is not None
            else:
                profiling_stopped = False
            profiling_mgr = manager.get_profiling_manager()
            status = profiling_mgr.get_profiling_status() if profiling_mgr else {}
            manager.stop()
            success = (
                "available_profilers" in status and
                isinstance(status["available_profilers"], list)
            )
            return TestResult(
                test_name="test_profiling_integration",
                success=success,
                details={
                    "profiling_started": profiling_started,
                    "profiling_stopped": profiling_stopped,
                    "profiling_status": status
                },
                duration=time.time() - start_time
            )
        except Exception as e:
            return TestResult(
                test_name="test_profiling_integration",
                success=False,
                error_message=str(e),
                duration=time.time() - start_time if 'start_time' in locals() else 0.0
            )
    async def test_device_capability_detection(self) -> TestResult:
        """Test device capability detection"""
        try:
            start_time = time.time()
            detector = DeviceCapabilityDetector()
            capabilities = detector.detect_capabilities()
            profile = detector.generate_performance_profile()
            recommendations = detector.get_optimization_recommendations()
            success = (
                capabilities.platform_name and
                capabilities.cpu_cores_logical > 0 and
                capabilities.total_memory_gb > 0 and
                capabilities.overall_performance_tier in ["low", "medium", "high"] and
                profile.device_tier == capabilities.overall_performance_tier and
                isinstance(recommendations, list)
            )
            return TestResult(
                test_name="test_device_capability_detection",
                success=success,
                details={
                    "device_tier": capabilities.overall_performance_tier,
                    "cpu_cores": capabilities.cpu_cores_logical,
                    "memory_gb": capabilities.total_memory_gb,
                    "cpu_score": capabilities.cpu_performance_score,
                    "gpu_available": capabilities.gpu_available,
                    "max_devices": profile.max_concurrent_devices,
                    "recommended_fps": profile.recommended_fps,
                    "recommendations_count": len(recommendations)
                },
                duration=time.time() - start_time
            )
        except Exception as e:
            return TestResult(
                test_name="test_device_capability_detection",
                success=False,
                error_message=str(e),
                duration=time.time() - start_time if 'start_time' in locals() else 0.0
            )
    async def test_endurance_monitoring(self) -> TestResult:
        """Test short endurance monitoring (1 minute)"""
        try:
            start_time = time.time()
            config = EnduranceTestConfig(
                target_duration_hours=1.0 / 60.0,
                monitoring_interval_seconds=5.0,
                enable_simulated_workload=True,
                workload_intensity="low",
                save_detailed_logs=False
            )
            suite = EnduranceTestSuite(config, "/tmp/endurance_test")
            result = await suite.run_endurance_test()
            success = (
                "test_summary" in result and
                "memory_analysis" in result and
                "performance_analysis" in result and
                result["test_summary"]["samples_collected"] > 0
            )
            return TestResult(
                test_name="test_endurance_monitoring",
                success=success,
                details={
                    "duration_hours": result["test_summary"]["total_duration_hours"],
                    "samples_collected": result["test_summary"]["samples_collected"],
                    "memory_growth_mb": result["memory_analysis"]["memory_growth_mb"],
                    "leak_suspected": result["memory_analysis"]["leak_detection"]["is_leak_suspected"],
                    "final_cpu_percent": result["performance_analysis"]["final_cpu_percent"]
                },
                duration=time.time() - start_time
            )
        except Exception as e:
            return TestResult(
                test_name="test_endurance_monitoring",
                success=False,
                error_message=str(e),
                duration=time.time() - start_time if 'start_time' in locals() else 0.0
            )
    async def test_memory_leak_detection(self) -> TestResult:
        """Test memory leak detection capabilities"""
        try:
            start_time = time.time()
            config = OptimizationConfig(enable_memory_optimization=True)
            manager = PerformanceManager()
            if not manager.initialize(config):
                return TestResult(
                    test_name="test_memory_leak_detection",
                    success=False,
                    error_message="Failed to initialize performance manager",
                    duration=time.time() - start_time
                )
            manager.start()
            memory_blocks = []
            for i in range(5):
                await asyncio.sleep(1)
                block = bytearray(1024 * 1024)
                memory_blocks.append(block)
            optimization_result = manager.optimize_now()
            memory_optimized = "memory" in optimization_result
            manager.stop()
            return TestResult(
                test_name="test_memory_leak_detection",
                success=memory_optimized,
                details={
                    "optimization_result": optimization_result,
                    "memory_blocks_allocated": len(memory_blocks)
                },
                duration=time.time() - start_time
            )
        except Exception as e:
            return TestResult(
                test_name="test_memory_leak_detection",
                success=False,
                error_message=str(e),
                duration=time.time() - start_time if 'start_time' in locals() else 0.0
            )
    async def test_adaptive_quality_control(self) -> TestResult:
        """Test adaptive quality control mechanisms"""
        try:
            start_time = time.time()
            config = OptimizationConfig(enable_network_optimization=True)
            manager = PerformanceManager()
            if not manager.initialize(config):
                return TestResult(
                    test_name="test_adaptive_quality_control",
                    success=False,
                    error_message="Failed to initialize performance manager",
                    duration=time.time() - start_time
                )
            manager.start()
            await asyncio.sleep(2)
            quality_settings = manager.get_recommended_quality_settings()
            optimization_result = manager.optimize_now()
            network_optimized = "network" in optimization_result
            manager.stop()
            success = (
                quality_settings and
                "resolution" in quality_settings and
                "fps" in quality_settings and
                network_optimized
            )
            return TestResult(
                test_name="test_adaptive_quality_control",
                success=success,
                details={
                    "quality_settings": quality_settings,
                    "network_optimization": optimization_result.get("network", {})
                },
                duration=time.time() - start_time
            )
        except Exception as e:
            return TestResult(
                test_name="test_adaptive_quality_control",
                success=False,
                error_message=str(e),
                duration=time.time() - start_time if 'start_time' in locals() else 0.0
            )
    async def test_performance_degradation_alerts(self) -> TestResult:
        """Test performance degradation alert system"""
        try:
            start_time = time.time()
            alerts_received = []
            def alert_callback(violation_type: str, metrics):
                alerts_received.append({
                    "type": violation_type,
                    "cpu_percent": metrics.cpu_percent,
                    "memory_mb": metrics.memory_mb
                })
            config = OptimizationConfig(
                max_cpu_percent=50.0,
                max_memory_mb=100.0,
                alert_threshold_violations=1
            )
            manager = PerformanceManager()
            if not manager.initialize(config):
                return TestResult(
                    test_name="test_performance_degradation_alerts",
                    success=False,
                    error_message="Failed to initialize performance manager",
                    duration=time.time() - start_time
                )
            if manager.monitor:
                manager.monitor.add_alert_callback(alert_callback)
            manager.start()
            await asyncio.sleep(5)
            manager.stop()
            success = len(alerts_received) > 0
            return TestResult(
                test_name="test_performance_degradation_alerts",
                success=success,
                details={
                    "alerts_received": len(alerts_received),
                    "alert_details": alerts_received[:3]
                },
                duration=time.time() - start_time
            )
        except Exception as e:
            return TestResult(
                test_name="test_performance_degradation_alerts",
                success=False,
                error_message=str(e),
                duration=time.time() - start_time if 'start_time' in locals() else 0.0
            )
if __name__ == "__main__":
    import asyncio
    async def run_tests():
        suite = PerformanceOptimizationTestSuite()
        results = await suite.run_all_tests()
        print(f"\nPerformance Optimisation Test Results:")
        print(f"Tests run: {len(results)}")
        passed = sum(1 for r in results if r.success)
        print(f"Passed: {passed}/{len(results)} ({passed/len(results)*100:.1f}%)")
        for result in results:
            status = "[PASS]" if result.success else "[FAIL]"
            print(f"  {status} {result.test_name}: {result.duration:.2f}s")
            if not result.success:
                print(f"    Error: {result.error_message}")
    asyncio.run(run_tests())