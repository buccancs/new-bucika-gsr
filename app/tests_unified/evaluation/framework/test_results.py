from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional, Any
from enum import Enum
import uuid
from .test_categories import TestCategory, TestType, TestPriority
class TestStatus(Enum):
    PENDING = "pending"
    RUNNING = "running"
    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"
@dataclass
class PerformanceMetrics:
    execution_time: float = 0.0
    memory_usage_mb: float = 0.0
    cpu_usage_percent: float = 0.0
    network_latency_ms: float = 0.0
    disk_io_mb_per_sec: float = 0.0
    gpu_usage_percent: float = 0.0
    synchronization_precision_ms: float = 0.0
    data_quality_score: float = 0.0
    measurement_accuracy: float = 0.0
    frame_rate_fps: float = 0.0
    data_throughput_mb_per_sec: float = 0.0
@dataclass
class TestResult:
    test_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    test_name: str = ""
    test_type: TestType = TestType.UNIT_ANDROID
    test_category: TestCategory = TestCategory.FOUNDATION
    priority: TestPriority = TestPriority.MEDIUM
    status: TestStatus = TestStatus.PENDING
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    execution_time: float = 0.0
    success: bool = False
    error_message: Optional[str] = None
    exception_details: Optional[str] = None
    performance_metrics: PerformanceMetrics = field(default_factory=PerformanceMetrics)
    custom_metrics: Dict[str, Any] = field(default_factory=dict)
    validation_passed: bool = False
    quality_score: float = 0.0
    coverage_percentage: float = 0.0
    environment_info: Dict[str, Any] = field(default_factory=dict)
    test_data: Dict[str, Any] = field(default_factory=dict)
    artifacts: List[str] = field(default_factory=list)
    def to_dict(self) -> Dict[str, Any]:
        return {
            "test_id": self.test_id,
            "test_name": self.test_name,
            "test_type": self.test_type.name,
            "test_category": self.test_category.name,
            "priority": self.priority.value,
            "status": self.status.value,
            "start_time": self.start_time.isoformat() if self.start_time else None,
            "end_time": self.end_time.isoformat() if self.end_time else None,
            "execution_time": self.execution_time,
            "success": self.success,
            "error_message": self.error_message,
            "exception_details": self.exception_details,
            "performance_metrics": {
                "execution_time": self.performance_metrics.execution_time,
                "memory_usage_mb": self.performance_metrics.memory_usage_mb,
                "cpu_usage_percent": self.performance_metrics.cpu_usage_percent,
                "network_latency_ms": self.performance_metrics.network_latency_ms,
                "disk_io_mb_per_sec": self.performance_metrics.disk_io_mb_per_sec,
                "gpu_usage_percent": self.performance_metrics.gpu_usage_percent,
                "synchronization_precision_ms": self.performance_metrics.synchronization_precision_ms,
                "data_quality_score": self.performance_metrics.data_quality_score,
                "measurement_accuracy": self.performance_metrics.measurement_accuracy,
                "frame_rate_fps": self.performance_metrics.frame_rate_fps,
                "data_throughput_mb_per_sec": self.performance_metrics.data_throughput_mb_per_sec,
            },
            "custom_metrics": self.custom_metrics,
            "validation_passed": self.validation_passed,
            "quality_score": self.quality_score,
            "coverage_percentage": self.coverage_percentage,
            "environment_info": self.environment_info,
            "test_data": self.test_data,
            "artifacts": self.artifacts
        }
@dataclass
class SuiteResults:
    suite_name: str = ""
    suite_category: TestCategory = TestCategory.FOUNDATION
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    total_execution_time: float = 0.0
    test_results: List[TestResult] = field(default_factory=list)
    total_tests: int = 0
    passed_tests: int = 0
    failed_tests: int = 0
    skipped_tests: int = 0
    error_tests: int = 0
    success_rate: float = 0.0
    average_execution_time: float = 0.0
    total_coverage: float = 0.0
    overall_quality_score: float = 0.0
    peak_memory_mb: float = 0.0
    average_cpu_percent: float = 0.0
    average_latency_ms: float = 0.0
    def add_test_result(self, result: TestResult):
        self.test_results.append(result)
        self._update_statistics()
    def _update_statistics(self):
        self.total_tests = len(self.test_results)
        if self.total_tests == 0:
            return
        self.passed_tests = sum(1 for r in self.test_results if r.status == TestStatus.PASSED)
        self.failed_tests = sum(1 for r in self.test_results if r.status == TestStatus.FAILED)
        self.skipped_tests = sum(1 for r in self.test_results if r.status == TestStatus.SKIPPED)
        self.error_tests = sum(1 for r in self.test_results if r.status == TestStatus.ERROR)
        self.success_rate = self.passed_tests / self.total_tests if self.total_tests > 0 else 0.0
        execution_times = [r.execution_time for r in self.test_results if r.execution_time > 0]
        self.average_execution_time = sum(execution_times) / len(execution_times) if execution_times else 0.0
        coverages = [r.coverage_percentage for r in self.test_results if r.coverage_percentage > 0]
        self.total_coverage = sum(coverages) / len(coverages) if coverages else 0.0
        quality_scores = [r.quality_score for r in self.test_results if r.quality_score > 0]
        self.overall_quality_score = sum(quality_scores) / len(quality_scores) if quality_scores else 0.0
        memory_values = [r.performance_metrics.memory_usage_mb for r in self.test_results]
        self.peak_memory_mb = max(memory_values) if memory_values else 0.0
        cpu_values = [r.performance_metrics.cpu_usage_percent for r in self.test_results]
        self.average_cpu_percent = sum(cpu_values) / len(cpu_values) if cpu_values else 0.0
        latency_values = [r.performance_metrics.network_latency_ms for r in self.test_results]
        self.average_latency_ms = sum(latency_values) / len(latency_values) if latency_values else 0.0
@dataclass
class TestResults:
    execution_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    total_execution_time: float = 0.0
    suite_results: Dict[str, SuiteResults] = field(default_factory=dict)
    total_suites: int = 0
    total_tests: int = 0
    overall_success_rate: float = 0.0
    overall_quality_score: float = 0.0
    overall_coverage: float = 0.0
    system_performance_score: float = 0.0
    reliability_score: float = 0.0
    usability_score: float = 0.0
    def add_suite_results(self, suite_name: str, results: SuiteResults):
        self.suite_results[suite_name] = results
        self._update_overall_statistics()
    def _update_overall_statistics(self):
        self.total_suites = len(self.suite_results)
        self.total_tests = sum(suite.total_tests for suite in self.suite_results.values())
        if self.total_tests > 0:
            total_passed = sum(suite.passed_tests for suite in self.suite_results.values())
            self.overall_success_rate = total_passed / self.total_tests
            suite_weights = [(suite.total_tests, suite) for suite in self.suite_results.values()]
            total_weight = sum(weight for weight, _ in suite_weights)
            if total_weight > 0:
                self.overall_quality_score = sum(
                    weight * suite.overall_quality_score for weight, suite in suite_weights
                ) / total_weight
                self.overall_coverage = sum(
                    weight * suite.total_coverage for weight, suite in suite_weights
                ) / total_weight
    def get_summary_report(self) -> Dict[str, Any]:
        return {
            "execution_id": self.execution_id,
            "execution_time": {
                "start": self.start_time.isoformat() if self.start_time else None,
                "end": self.end_time.isoformat() if self.end_time else None,
                "total_seconds": self.total_execution_time
            },
            "statistics": {
                "total_suites": self.total_suites,
                "total_tests": self.total_tests,
                "success_rate": round(self.overall_success_rate * 100, 2),
                "quality_score": round(self.overall_quality_score, 3),
                "coverage_percentage": round(self.overall_coverage, 2)
            },
            "performance": {
                "system_performance_score": round(self.system_performance_score, 3),
                "reliability_score": round(self.reliability_score, 3),
                "usability_score": round(self.usability_score, 3)
            },
            "suite_summaries": {
                name: {
                    "category": suite.suite_category.name,
                    "total_tests": suite.total_tests,
                    "success_rate": round(suite.success_rate * 100, 2),
                    "execution_time": round(suite.total_execution_time, 2),
                    "quality_score": round(suite.overall_quality_score, 3)
                }
                for name, suite in self.suite_results.items()
            }
        }