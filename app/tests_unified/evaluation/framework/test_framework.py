import asyncio
import logging
import time
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Type, Callable, Any
import psutil
import gc
from .test_categories import TestCategory, TestConfiguration, QualityThresholds
from .test_results import TestResults, SuiteResults, TestResult, TestStatus, PerformanceMetrics
from ..metrics.quality_validator import QualityValidator, ValidationReport
logger = logging.getLogger(__name__)
class TestSuite:
    def __init__(self, name: str, category: TestCategory, description: str = ""):
        self.name = name
        self.category = category
        self.description = description
        self.tests: List['BaseTest'] = []
        self.setup_functions: List[Callable] = []
        self.teardown_functions: List[Callable] = []
    def add_test(self, test: 'BaseTest'):
        self.tests.append(test)
    def add_setup(self, setup_func: Callable):
        self.setup_functions.append(setup_func)
    def add_teardown(self, teardown_func: Callable):
        self.teardown_functions.append(teardown_func)
class BaseTest:
    def __init__(self, name: str, description: str = "", timeout: int = 300):
        self.name = name
        self.description = description
        self.timeout = timeout
        self.setup_functions: List[Callable] = []
        self.teardown_functions: List[Callable] = []
    def add_setup(self, setup_func: Callable):
        self.setup_functions.append(setup_func)
    def add_teardown(self, teardown_func: Callable):
        self.teardown_functions.append(teardown_func)
    async def execute(self, test_env: Dict[str, Any]) -> TestResult:
        """Execute the test - to be implemented by subclasses"""
        raise NotImplementedError("Subclasses must implement execute method")
class PerformanceMonitor:
    def __init__(self):
        self.monitoring = False
        self.monitor_thread = None
        self.metrics_history = []
        self._lock = threading.Lock()
    def start_monitoring(self):
        with self._lock:
            if not self.monitoring:
                self.monitoring = True
                self.monitor_thread = threading.Thread(target=self._monitor_loop, daemon=True)
                self.monitor_thread.start()
                logger.debug("Performance monitoring started")
    def stop_monitoring(self):
        with self._lock:
            self.monitoring = False
            if self.monitor_thread:
                self.monitor_thread.join(timeout=5.0)
                self.monitor_thread = None
            logger.debug("Performance monitoring stopped")
    def _monitor_loop(self):
        while self.monitoring:
            try:
                cpu_percent = psutil.cpu_percent(interval=0.1)
                memory = psutil.virtual_memory()
                disk_io = psutil.disk_io_counters()
                network_io = psutil.net_io_counters()
                metrics = {
                    'timestamp': time.time(),
                    'cpu_percent': cpu_percent,
                    'memory_mb': memory.used / (1024 * 1024),
                    'memory_percent': memory.percent,
                    'disk_read_mb': disk_io.read_bytes / (1024 * 1024) if disk_io else 0,
                    'disk_write_mb': disk_io.write_bytes / (1024 * 1024) if disk_io else 0,
                    'network_sent_mb': network_io.bytes_sent / (1024 * 1024) if network_io else 0,
                    'network_recv_mb': network_io.bytes_recv / (1024 * 1024) if network_io else 0
                }
                with self._lock:
                    self.metrics_history.append(metrics)
                    if len(self.metrics_history) > 1000:
                        self.metrics_history = self.metrics_history[-1000:]
                time.sleep(1.0)
            except Exception as e:
                logger.error(f"Error in performance monitoring: {e}")
                time.sleep(5.0)
    def get_current_metrics(self) -> PerformanceMetrics:
        with self._lock:
            if not self.metrics_history:
                return PerformanceMetrics()
            recent_metrics = self.metrics_history[-10:]
            return PerformanceMetrics(
                memory_usage_mb=sum(m['memory_mb'] for m in recent_metrics) / len(recent_metrics),
                cpu_usage_percent=sum(m['cpu_percent'] for m in recent_metrics) / len(recent_metrics),
                disk_io_mb_per_sec=max(
                    recent_metrics[-1]['disk_read_mb'] + recent_metrics[-1]['disk_write_mb']
                    - recent_metrics[0]['disk_read_mb'] - recent_metrics[0]['disk_write_mb'], 0
                ) / len(recent_metrics) if len(recent_metrics) > 1 else 0
            )
class TestEnvironment:
    def __init__(self, config: TestConfiguration):
        self.config = config
        self.temp_files: List[Path] = []
        self.temp_dirs: List[Path] = []
        self.resources: Dict[str, Any] = {}
        self.cleanup_functions: List[Callable] = []
    def add_temp_file(self, file_path: Path):
        self.temp_files.append(file_path)
    def add_temp_dir(self, dir_path: Path):
        self.temp_dirs.append(dir_path)
    def add_resource(self, name: str, resource: Any):
        self.resources[name] = resource
    def add_cleanup(self, cleanup_func: Callable):
        self.cleanup_functions.append(cleanup_func)
    def cleanup(self):
        logger.debug("Cleaning up test environment")
        for cleanup_func in self.cleanup_functions:
            try:
                cleanup_func()
            except Exception as e:
                logger.error(f"Error in cleanup function: {e}")
        for file_path in self.temp_files:
            try:
                if file_path.exists():
                    file_path.unlink()
            except Exception as e:
                logger.error(f"Error cleaning up temp file {file_path}: {e}")
        for dir_path in self.temp_dirs:
            try:
                if dir_path.exists():
                    import shutil
                    shutil.rmtree(dir_path)
            except Exception as e:
                logger.error(f"Error cleaning up temp dir {dir_path}: {e}")
        self.resources.clear()
class TestFramework:
    def __init__(self, quality_thresholds: Optional[QualityThresholds] = None):
        self.test_suites: Dict[str, TestSuite] = {}
        self.quality_validator = QualityValidator(quality_thresholds)
        self.performance_monitor = PerformanceMonitor()
        self.config = TestConfiguration()
        self.is_running = False
        self.current_execution_id = None
        self.execution_history: List[TestResults] = []
        self.logger = logging.getLogger(__name__)
        self._setup_logging()
    def _setup_logging(self):
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.StreamHandler(),
                logging.FileHandler('test_framework.log')
            ]
        )
    def register_test_suite(self, name: str, test_suite: TestSuite):
        self.test_suites[name] = test_suite
        self.logger.info(f"Registered test suite: {name} ({test_suite.category.name})")
    def configure(self, config: TestConfiguration):
        self.config = config
        self.logger.info("Test framework configuration updated")
    async def run_all_tests(self) -> TestResults:
        """Execute all registered test suites"""
        self.logger.info("Starting thorough test execution")
        if self.is_running:
            raise RuntimeError("Test execution already in progress")
        self.is_running = True
        try:
            results = TestResults()
            results.start_time = datetime.now()
            self.current_execution_id = results.execution_id
            self.performance_monitor.start_monitoring()
            execution_order = [
                TestCategory.FOUNDATION,
                TestCategory.INTEGRATION,
                TestCategory.SYSTEM,
                TestCategory.PERFORMANCE
            ]
            for category in execution_order:
                category_suites = {
                    name: suite for name, suite in self.test_suites.items()
                    if suite.category == category
                }
                if category_suites:
                    self.logger.info(f"Executing {category.name} test suites")
                    for suite_name, test_suite in category_suites.items():
                        suite_results = await self._run_test_suite(suite_name, test_suite)
                        results.add_suite_results(suite_name, suite_results)
                        if suite_results.success_rate < 0.5:
                            self.logger.warning(
                                f"Suite {suite_name} has low success rate ({suite_results.success_rate:.2%})"
                            )
            results.end_time = datetime.now()
            results.total_execution_time = (results.end_time - results.start_time).total_seconds()
            validation_report = self.quality_validator.validate_test_results(results)
            self.execution_history.append(results)
            self._generate_execution_report(results, validation_report)
            self.logger.info(
                f"Test execution completed. Overall success rate: {results.overall_success_rate:.2%}"
            )
            return results
        finally:
            self.performance_monitor.stop_monitoring()
            self.is_running = False
            self.current_execution_id = None
    async def run_test_category(self, category: TestCategory) -> TestResults:
        """Run tests for specific category"""
        self.logger.info(f"Running tests for category: {category.name}")
        category_suites = {
            name: suite for name, suite in self.test_suites.items()
            if suite.category == category
        }
        if not category_suites:
            self.logger.warning(f"No test suites found for category {category.name}")
            return TestResults()
        results = TestResults()
        results.start_time = datetime.now()
        self.performance_monitor.start_monitoring()
        try:
            for suite_name, test_suite in category_suites.items():
                suite_results = await self._run_test_suite(suite_name, test_suite)
                results.add_suite_results(suite_name, suite_results)
            results.end_time = datetime.now()
            results.total_execution_time = (results.end_time - results.start_time).total_seconds()
        finally:
            self.performance_monitor.stop_monitoring()
        return results
    async def _run_test_suite(self, name: str, test_suite: TestSuite) -> SuiteResults:
        """Execute individual test suite"""
        self.logger.info(f"Running test suite: {name}")
        suite_results = SuiteResults(
            suite_name=name,
            suite_category=test_suite.category,
            start_time=datetime.now()
        )
        test_env = TestEnvironment(self.config)
        try:
            for setup_func in test_suite.setup_functions:
                try:
                    if asyncio.iscoroutinefunction(setup_func):
                        await setup_func(test_env)
                    else:
                        setup_func(test_env)
                except Exception as e:
                    self.logger.error(f"Suite setup failed for {name}: {e}")
                    raise
            if self.config.parallel_execution and len(test_suite.tests) > 1:
                suite_results = await self._run_tests_parallel(test_suite, test_env, suite_results)
            else:
                suite_results = await self._run_tests_sequential(test_suite, test_env, suite_results)
            for teardown_func in test_suite.teardown_functions:
                try:
                    if asyncio.iscoroutinefunction(teardown_func):
                        await teardown_func(test_env)
                    else:
                        teardown_func(test_env)
                except Exception as e:
                    self.logger.error(f"Suite teardown error for {name}: {e}")
        except Exception as e:
            self.logger.error(f"Test suite {name} failed with exception: {e}")
        finally:
            if self.config.cleanup_on_failure or suite_results.success_rate > 0.5:
                test_env.cleanup()
            suite_results.end_time = datetime.now()
            if suite_results.start_time:
                suite_results.total_execution_time = (
                    suite_results.end_time - suite_results.start_time
                ).total_seconds()
        self.logger.info(
            f"Suite {name} completed: {suite_results.passed_tests}/{suite_results.total_tests} passed"
        )
        return suite_results
    async def _run_tests_sequential(self, test_suite: TestSuite, test_env: TestEnvironment,
                                   suite_results: SuiteResults) -> SuiteResults:
        """Run tests sequentially"""
        for test in test_suite.tests:
            test_result = await self._execute_single_test(test, test_env)
            suite_results.add_test_result(test_result)
            gc.collect()
        return suite_results
    async def _run_tests_parallel(self, test_suite: TestSuite, test_env: TestEnvironment,
                                 suite_results: SuiteResults) -> SuiteResults:
        """Run tests in parallel"""
        tasks = []
        for test in test_suite.tests:
            isolated_env = TestEnvironment(self.config)
            isolated_env.resources.update(test_env.resources)
            task = asyncio.create_task(self._execute_single_test(test, isolated_env))
            tasks.append(task)
        test_results = await asyncio.gather(*tasks, return_exceptions=True)
        for result in test_results:
            if isinstance(result, Exception):
                error_result = TestResult(
                    test_name="unknown_parallel_test",
                    status=TestStatus.ERROR,
                    success=False,
                    error_message=str(result)
                )
                suite_results.add_test_result(error_result)
            else:
                suite_results.add_test_result(result)
        return suite_results
    async def _execute_single_test(self, test: BaseTest, test_env: TestEnvironment) -> TestResult:
        """Execute individual test with full monitoring"""
        test_result = TestResult(
            test_name=test.name,
            start_time=datetime.now()
        )
        try:
            for setup_func in test.setup_functions:
                if asyncio.iscoroutinefunction(setup_func):
                    await setup_func(test_env)
                else:
                    setup_func(test_env)
            baseline_metrics = self.performance_monitor.get_current_metrics()
            execution_start = time.time()
            try:
                test_result = await asyncio.wait_for(
                    test.execute(test_env.resources),
                    timeout=test.timeout
                )
                test_result.status = TestStatus.PASSED
                test_result.success = True
            except asyncio.TimeoutError:
                test_result.status = TestStatus.ERROR
                test_result.success = False
                test_result.error_message = f"Test timed out after {test.timeout} seconds"
            except Exception as e:
                test_result.status = TestStatus.FAILED
                test_result.success = False
                test_result.error_message = str(e)
                test_result.exception_details = repr(e)
            execution_end = time.time()
            test_result.execution_time = execution_end - execution_start
            final_metrics = self.performance_monitor.get_current_metrics()
            test_result.performance_metrics = PerformanceMetrics(
                execution_time=test_result.execution_time,
                memory_usage_mb=final_metrics.memory_usage_mb - baseline_metrics.memory_usage_mb,
                cpu_usage_percent=final_metrics.cpu_usage_percent,
                disk_io_mb_per_sec=final_metrics.disk_io_mb_per_sec
            )
            for teardown_func in test.teardown_functions:
                try:
                    if asyncio.iscoroutinefunction(teardown_func):
                        await teardown_func(test_env)
                    else:
                        teardown_func(test_env)
                except Exception as e:
                    self.logger.error(f"Test teardown error for {test.name}: {e}")
        except Exception as e:
            test_result.status = TestStatus.ERROR
            test_result.success = False
            test_result.error_message = f"Test execution framework error: {str(e)}"
            self.logger.error(f"Framework error executing test {test.name}: {e}")
        finally:
            test_result.end_time = datetime.now()
            import platform
            test_result.environment_info = {
                "python_version": platform.python_version(),
                "cpu_count": psutil.cpu_count(),
                "memory_total_mb": psutil.virtual_memory().total / (1024 * 1024),
                "platform": platform.system() + " " + platform.release()
            }
        return test_result
    def _generate_execution_report(self, results: TestResults, validation_report: ValidationReport):
        report_path = Path(f"test_execution_report_{results.execution_id[:8]}.json")
        report_data = {
            "execution_summary": results.get_summary_report(),
            "validation_report": {
                "overall_quality": validation_report.overall_quality,
                "overall_valid": validation_report.overall_valid,
                "critical_issues_count": len(validation_report.critical_issues),
                "quality_issues_count": len(validation_report.quality_issues),
                "recommendations": validation_report.recommendations,
                "statistical_summary": validation_report.statistical_summary
            },
            "detailed_results": {
                suite_name: {
                    "category": suite.suite_category.name,
                    "test_count": suite.total_tests,
                    "success_rate": suite.success_rate,
                    "execution_time": suite.total_execution_time,
                    "quality_score": suite.overall_quality_score,
                    "test_details": [result.to_dict() for result in suite.test_results]
                }
                for suite_name, suite in results.suite_results.items()
            }
        }
        import json
        with open(report_path, 'w') as f:
            json.dump(report_data, f, indent=2, default=str)
        self.logger.info(f"complete test report generated: {report_path}")
    def get_execution_history(self) -> List[TestResults]:
        return self.execution_history.copy()
    def get_quality_trends(self) -> Dict[str, List[float]]:
        trends = {
            "overall_quality": [],
            "success_rates": [],
            "execution_times": []
        }
        for execution in self.execution_history:
            trends["success_rates"].append(execution.overall_success_rate)
            trends["execution_times"].append(execution.total_execution_time)
            trends["overall_quality"].append(execution.overall_success_rate)
        return trends