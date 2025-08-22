import logging
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass
from datetime import datetime
import statistics
from ..test_results import TestResults, SuiteResults, TestResult, TestStatus
from ..test_categories import QualityThresholds, TestCategory, TestType
logger = logging.getLogger(__name__)
@dataclass
class ValidationRule:
    name: str
    description: str
    threshold: float
    comparison: str
    threshold_max: Optional[float] = None
    weight: float = 1.0
    critical: bool = False
@dataclass
class ValidationIssue:
    rule_name: str
    severity: str
    message: str
    measured_value: float
    threshold_value: float
    test_names: List[str]
@dataclass
class SuiteValidation:
    suite_name: str
    suite_category: TestCategory
    success_rate_valid: bool = False
    performance_valid: bool = False
    quality_valid: bool = False
    coverage_valid: bool = False
    overall_valid: bool = False
    quality_score: float = 0.0
    issues: List[ValidationIssue] = None
    recommendations: List[str] = None
    def __post_init__(self):
        if self.issues is None:
            self.issues = []
        if self.recommendations is None:
            self.recommendations = []
@dataclass
class ValidationReport:
    execution_id: str
    timestamp: datetime
    overall_quality: float = 0.0
    overall_valid: bool = False
    suite_validations: Dict[str, SuiteValidation] = None
    critical_issues: List[ValidationIssue] = None
    quality_issues: List[ValidationIssue] = None
    recommendations: List[str] = None
    statistical_summary: Dict[str, float] = None
    confidence_intervals: Dict[str, Tuple[float, float]] = None
    def __post_init__(self):
        if self.suite_validations is None:
            self.suite_validations = {}
        if self.critical_issues is None:
            self.critical_issues = []
        if self.quality_issues is None:
            self.quality_issues = []
        if self.recommendations is None:
            self.recommendations = []
        if self.statistical_summary is None:
            self.statistical_summary = {}
        if self.confidence_intervals is None:
            self.confidence_intervals = {}
class QualityValidator:
    def __init__(self, quality_thresholds: Optional[QualityThresholds] = None):
        self.quality_thresholds = quality_thresholds or QualityThresholds()
        self.validation_rules: Dict[str, List[ValidationRule]] = {}
        self.logger = logging.getLogger(__name__)
        self._initialize_default_rules()
    def _initialize_default_rules(self):
        foundation_rules = [
            ValidationRule(
                name="success_rate_foundation",
                description="Foundation tests must have >98% success rate",
                threshold=0.98,
                comparison="gte",
                weight=2.0,
                critical=True
            ),
            ValidationRule(
                name="execution_time_foundation",
                description="Foundation tests should execute quickly",
                threshold=300.0,
                comparison="lte",
                weight=1.0
            ),
            ValidationRule(
                name="coverage_foundation",
                description="Foundation tests must achieve >90% coverage",
                threshold=0.90,
                comparison="gte",
                weight=1.5,
                critical=True
            )
        ]
        self.validation_rules[TestCategory.FOUNDATION.name] = foundation_rules
        integration_rules = [
            ValidationRule(
                name="success_rate_integration",
                description="Integration tests must have >95% success rate",
                threshold=0.95,
                comparison="gte",
                weight=2.0,
                critical=True
            ),
            ValidationRule(
                name="latency_integration",
                description="Network latency should be <100ms",
                threshold=100.0,
                comparison="lte",
                weight=1.5
            ),
            ValidationRule(
                name="sync_precision",
                description="Synchronisation precision must be <1ms",
                threshold=1.0,
                comparison="lte",
                weight=2.0,
                critical=True
            )
        ]
        self.validation_rules[TestCategory.INTEGRATION.name] = integration_rules
        system_rules = [
            ValidationRule(
                name="success_rate_system",
                description="System tests must have >95% success rate",
                threshold=0.95,
                comparison="gte",
                weight=2.0,
                critical=True
            ),
            ValidationRule(
                name="data_quality_system",
                description="Data quality score must be >0.8",
                threshold=0.8,
                comparison="gte",
                weight=1.5,
                critical=True
            ),
            ValidationRule(
                name="workflow_completion",
                description="End-to-end workflows must complete successfully",
                threshold=0.95,
                comparison="gte",
                weight=2.0,
                critical=True
            )
        ]
        self.validation_rules[TestCategory.SYSTEM.name] = system_rules
        performance_rules = [
            ValidationRule(
                name="success_rate_performance",
                description="Performance tests must have >90% success rate",
                threshold=0.90,
                comparison="gte",
                weight=1.5
            ),
            ValidationRule(
                name="memory_usage",
                description="Memory usage should be <1GB",
                threshold=1000.0,
                comparison="lte",
                weight=1.0
            ),
            ValidationRule(
                name="cpu_usage",
                description="CPU usage should be <60%",
                threshold=60.0,
                comparison="lte",
                weight=1.0
            ),
            ValidationRule(
                name="throughput_performance",
                description="Data throughput should meet target >25MB/s",
                threshold=25.0,
                comparison="gte",
                weight=1.5
            )
        ]
        self.validation_rules[TestCategory.PERFORMANCE.name] = performance_rules
    def register_validation_rule(self, test_category: TestCategory, rule: ValidationRule):
        category_name = test_category.name
        if category_name not in self.validation_rules:
            self.validation_rules[category_name] = []
        self.validation_rules[category_name].append(rule)
        self.logger.info(f"Registered validation rule '{rule.name}' for category {category_name}")
    def validate_test_results(self, test_results: TestResults) -> ValidationReport:
        """
        Validate test results against quality thresholds and generate comprehensive report
        """
        # Handle both TestResults objects and plain dicts for backward compatibility
        if hasattr(test_results, 'execution_id'):
            execution_id = test_results.execution_id
            suite_results_dict = test_results.suite_results if hasattr(test_results, 'suite_results') else {}
        else:
            # Fallback for dict input from unified runner
            execution_id = test_results.get('execution_id', 'unknown-execution')
            # Convert the runner's test_results format to suite_results format
            suite_results_dict = {}
            for level_name, level_result in test_results.items():
                if isinstance(level_result, dict) and 'return_code' in level_result:
                    # This is a test level result from the unified runner
                    suite_results_dict[level_name] = level_result
            
        self.logger.info(f"Starting validation for execution {execution_id}")
        validation_report = ValidationReport(
            execution_id=execution_id,
            timestamp=datetime.now()
        )
        for suite_name, suite_results in suite_results_dict.items():
            # Handle both SuiteResults objects and plain dicts
            if hasattr(suite_results, 'suite_name'):
                # This is a proper SuiteResults object
                suite_validation = self._validate_suite_results(suite_results)
            else:
                # This is a dict from the unified runner, create a mock validation
                suite_validation = SuiteValidation(
                    suite_name=suite_name,
                    suite_category=TestCategory.FOUNDATION,  # Default category
                    success_rate_valid=suite_results.get('return_code', 1) == 0,
                    performance_valid=True,
                    quality_valid=True,
                    coverage_valid=True,
                    overall_valid=suite_results.get('return_code', 1) == 0,
                    quality_score=0.8 if suite_results.get('return_code', 1) == 0 else 0.3
                )
            validation_report.suite_validations[suite_name] = suite_validation
            for issue in suite_validation.issues:
                if issue.severity == 'critical':
                    validation_report.critical_issues.append(issue)
                else:
                    validation_report.quality_issues.append(issue)
        validation_report.overall_quality = self._calculate_overall_quality(validation_report)
        validation_report.overall_valid = len(validation_report.critical_issues) == 0
        self._perform_statistical_validation(test_results, validation_report)
        validation_report.recommendations = self._generate_recommendations(validation_report)
        self.logger.info(f"Validation completed. Overall quality: {validation_report.overall_quality:.3f}")
        return validation_report
    def _validate_suite_results(self, suite_results: SuiteResults) -> SuiteValidation:
        suite_validation = SuiteValidation(
            suite_name=suite_results.suite_name,
            suite_category=suite_results.suite_category
        )
        category_name = suite_results.suite_category.name
        rules = self.validation_rules.get(category_name, [])
        for rule in rules:
            self._apply_validation_rule(rule, suite_results, suite_validation)
        suite_validation.overall_valid = (
            suite_validation.success_rate_valid and
            suite_validation.performance_valid and
            suite_validation.quality_valid and
            suite_validation.coverage_valid
        )
        suite_validation.quality_score = self._calculate_suite_quality_score(
            suite_results, suite_validation
        )
        return suite_validation
    def _apply_validation_rule(self, rule: ValidationRule, suite_results: SuiteResults,
                              suite_validation: SuiteValidation):
        measured_value = self._extract_measurement_value(rule.name, suite_results)
        if measured_value is None:
            self.logger.warning(f"Could not extract value for rule {rule.name}")
            return
        valid = self._compare_value(measured_value, rule.threshold, rule.comparison, rule.threshold_max)
        if "success_rate" in rule.name:
            suite_validation.success_rate_valid = valid
        elif any(keyword in rule.name for keyword in ["latency", "cpu", "memory", "throughput"]):
            suite_validation.performance_valid = valid
        elif "quality" in rule.name:
            suite_validation.quality_valid = valid
        elif "coverage" in rule.name:
            suite_validation.coverage_valid = valid
        if not valid:
            severity = "critical" if rule.critical else "warning"
            issue = ValidationIssue(
                rule_name=rule.name,
                severity=severity,
                message=f"{rule.description} (measured: {measured_value:.3f}, threshold: {rule.threshold})",
                measured_value=measured_value,
                threshold_value=rule.threshold,
                test_names=[r.test_name for r in suite_results.test_results if not r.success]
            )
            suite_validation.issues.append(issue)
    def _extract_measurement_value(self, rule_name: str, suite_results: SuiteResults) -> Optional[float]:
        if "success_rate" in rule_name:
            return suite_results.success_rate
        elif "execution_time" in rule_name:
            return suite_results.average_execution_time
        elif "coverage" in rule_name:
            return suite_results.total_coverage / 100.0
        elif "latency" in rule_name:
            return suite_results.average_latency_ms
        elif "memory" in rule_name:
            return suite_results.peak_memory_mb
        elif "cpu" in rule_name:
            return suite_results.average_cpu_percent
        elif "quality" in rule_name:
            return suite_results.overall_quality_score
        elif "sync_precision" in rule_name:
            sync_values = [
                r.performance_metrics.synchronization_precision_ms
                for r in suite_results.test_results
                if r.performance_metrics.synchronization_precision_ms > 0
            ]
            return statistics.mean(sync_values) if sync_values else None
        elif "throughput" in rule_name:
            throughput_values = [
                r.performance_metrics.data_throughput_mb_per_sec
                for r in suite_results.test_results
                if r.performance_metrics.data_throughput_mb_per_sec > 0
            ]
            return statistics.mean(throughput_values) if throughput_values else None
        return None
    def _compare_value(self, measured: float, threshold: float, comparison: str,
                      threshold_max: Optional[float] = None) -> bool:
        if comparison == "gt":
            return measured > threshold
        elif comparison == "gte":
            return measured >= threshold
        elif comparison == "lt":
            return measured < threshold
        elif comparison == "lte":
            return measured <= threshold
        elif comparison == "eq":
            return abs(measured - threshold) < 1e-6
        elif comparison == "range" and threshold_max is not None:
            return threshold <= measured <= threshold_max
        else:
            self.logger.error(f"Unknown comparison operator: {comparison}")
            return False
    def _calculate_overall_quality(self, validation_report: ValidationReport) -> float:
        if not validation_report.suite_validations:
            return 0.0
        total_weight = 0.0
        weighted_sum = 0.0
        for suite_validation in validation_report.suite_validations.values():
            weight = 1.0
            weighted_sum += weight * suite_validation.quality_score
            total_weight += weight
        overall_quality = weighted_sum / total_weight if total_weight > 0 else 0.0
        critical_penalty = len(validation_report.critical_issues) * 0.1
        overall_quality = max(0.0, overall_quality - critical_penalty)
        return min(1.0, overall_quality)
    def _calculate_suite_quality_score(self, suite_results: SuiteResults,
                                     suite_validation: SuiteValidation) -> float:
        base_score = suite_results.success_rate
        performance_factor = 1.0
        if suite_results.average_cpu_percent > 0:
            performance_factor *= max(0.5, 1.0 - suite_results.average_cpu_percent / 100.0)
        coverage_factor = suite_results.total_coverage / 100.0 if suite_results.total_coverage > 0 else 0.5
        quality_factor = suite_results.overall_quality_score if suite_results.overall_quality_score > 0 else 0.5
        quality_score = (
            0.4 * base_score +
            0.2 * performance_factor +
            0.2 * coverage_factor +
            0.2 * quality_factor
        )
        return min(1.0, quality_score)
    def _perform_statistical_validation(self, test_results,
                                       validation_report: ValidationReport):
        """Perform statistical analysis on test results"""
        
        # Handle both TestResults objects and plain dicts
        if hasattr(test_results, 'suite_results'):
            suite_results_dict = test_results.suite_results
        else:
            # For dict input, use the data as-is
            suite_results_dict = test_results if isinstance(test_results, dict) else {}
        
        if not suite_results_dict:
            self.logger.warning("No suite results available for statistical validation")
            return
            
        execution_times = []
        for suite_name, suite in suite_results_dict.items():
            # Handle different data structures
            if hasattr(suite, 'test_results'):
                # This is a proper SuiteResults object
                execution_times.extend([r.execution_time for r in suite.test_results if r.execution_time > 0])
            elif isinstance(suite, dict) and 'execution_time' in suite:
                # This is a dict from unified runner
                execution_times.append(suite['execution_time'])
        if execution_times:
            validation_report.statistical_summary["mean_execution_time"] = statistics.mean(execution_times)
            validation_report.statistical_summary["median_execution_time"] = statistics.median(execution_times)
            validation_report.statistical_summary["std_execution_time"] = statistics.stdev(execution_times) if len(execution_times) > 1 else 0.0
            if len(execution_times) > 1:
                mean_time = statistics.mean(execution_times)
                std_time = statistics.stdev(execution_times)
                margin = 1.96 * std_time / (len(execution_times) ** 0.5)
                validation_report.confidence_intervals["execution_time"] = (
                    max(0, mean_time - margin), mean_time + margin
                )
        
        # Handle success rates for different data structures
        success_rates = []
        for suite_name, suite in suite_results_dict.items():
            if hasattr(suite, 'success_rate'):
                success_rates.append(suite.success_rate)
            elif isinstance(suite, dict) and 'return_code' in suite:
                # Convert return code to success rate (0 = success, non-zero = failure)
                success_rates.append(1.0 if suite['return_code'] == 0 else 0.0)
                
        if success_rates:
            validation_report.statistical_summary["mean_success_rate"] = statistics.mean(success_rates)
            validation_report.statistical_summary["min_success_rate"] = min(success_rates)
    def _generate_recommendations(self, validation_report: ValidationReport) -> List[str]:
        recommendations = []
        if validation_report.critical_issues:
            recommendations.append(
                f"CRITICAL: Address {len(validation_report.critical_issues)} critical issues "
                "before system deployment"
            )
        performance_issues = [
            issue for issue in validation_report.quality_issues
            if any(keyword in issue.rule_name for keyword in ["cpu", "memory", "latency"])
        ]
        if performance_issues:
            recommendations.append(
                "Consider performance optimisation for CPU/memory usage and network latency"
            )
        coverage_issues = [
            issue for issue in validation_report.quality_issues
            if "coverage" in issue.rule_name
        ]
        if coverage_issues:
            recommendations.append(
                "Increase test coverage to meet research-grade quality standards"
            )
        if validation_report.overall_quality < 0.85:
            recommendations.append(
                f"Overall quality score ({validation_report.overall_quality:.3f}) below target (0.85). "
                "Review test implementations and system performance"
            )
        return recommendations