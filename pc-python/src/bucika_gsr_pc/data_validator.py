#!/usr/bin/env python3
"""
Data Validation and Quality Assurance Module for Bucika GSR PC Orchestrator
Provides comprehensive validation, quality checks, and data integrity verification.
"""

import asyncio
import hashlib
import json
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any, Set
from dataclasses import dataclass, field
from enum import Enum
import csv
import re
from loguru import logger


class ValidationLevel(Enum):
    """Validation strictness levels"""
    BASIC = "basic"
    STANDARD = "standard"
    STRICT = "strict"
    RESEARCH_GRADE = "research_grade"


class QualityMetric(Enum):
    """Quality metrics for data assessment"""
    COMPLETENESS = "completeness"
    ACCURACY = "accuracy"
    CONSISTENCY = "consistency"
    TIMELINESS = "timeliness"
    VALIDITY = "validity"
    INTEGRITY = "integrity"


@dataclass
class ValidationResult:
    """Result of a validation check"""
    metric: QualityMetric
    passed: bool
    score: float  # 0.0 to 1.0
    message: str
    details: Dict[str, Any] = field(default_factory=dict)
    timestamp: datetime = field(default_factory=datetime.now)


@dataclass
class QualityReport:
    """Comprehensive quality assessment report"""
    session_id: str
    validation_level: ValidationLevel
    overall_score: float
    results: List[ValidationResult]
    recommendations: List[str]
    timestamp: datetime
    data_summary: Dict[str, Any]


class DataValidator:
    """Comprehensive data validation and quality assurance"""
    
    def __init__(self, validation_level: ValidationLevel = ValidationLevel.STANDARD):
        self.validation_level = validation_level
        self.quality_thresholds = self._get_quality_thresholds()
        self.validation_rules = self._setup_validation_rules()
        
    def _get_quality_thresholds(self) -> Dict[str, Dict[str, float]]:
        """Get quality thresholds based on validation level"""
        
        base_thresholds = {
            ValidationLevel.BASIC: {
                'completeness_min': 0.8,
                'accuracy_min': 0.7,
                'consistency_min': 0.6,
                'timeliness_tolerance': 10.0,  # seconds
                'data_loss_max': 0.2,
                'artifact_rate_max': 0.1
            },
            ValidationLevel.STANDARD: {
                'completeness_min': 0.9,
                'accuracy_min': 0.85,
                'consistency_min': 0.8,
                'timeliness_tolerance': 5.0,
                'data_loss_max': 0.1,
                'artifact_rate_max': 0.05
            },
            ValidationLevel.STRICT: {
                'completeness_min': 0.95,
                'accuracy_min': 0.9,
                'consistency_min': 0.9,
                'timeliness_tolerance': 2.0,
                'data_loss_max': 0.05,
                'artifact_rate_max': 0.02
            },
            ValidationLevel.RESEARCH_GRADE: {
                'completeness_min': 0.98,
                'accuracy_min': 0.95,
                'consistency_min': 0.95,
                'timeliness_tolerance': 1.0,
                'data_loss_max': 0.02,
                'artifact_rate_max': 0.01
            }
        }
        
        return base_thresholds[self.validation_level]
    
    def _setup_validation_rules(self) -> Dict[str, Any]:
        """Setup validation rules and constraints"""
        
        return {
            'gsr_range': {
                'min': 0.001,  # 1 nS minimum
                'max': 1000.0,  # 1 mS maximum
                'unit': 'microsiemens'
            },
            'temperature_range': {
                'min': 0.0,
                'max': 60.0,
                'unit': 'celsius'
            },
            'timestamp_format': {
                'pattern': r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}',
                'precision': 'nanoseconds'
            },
            'session_duration': {
                'min_seconds': 10,
                'max_seconds': 28800,  # 8 hours
            },
            'sampling_rate': {
                'expected': 128.0,
                'tolerance': 0.05  # 5% tolerance
            },
            'file_formats': {
                'csv_required_columns': [
                    'timestamp', 'datetime', 'gsr_microsiemens'
                ],
                'sync_required_columns': [
                    'timestamp', 'marker_id', 'description'
                ]
            }
        }
    
    async def validate_session(self, session_path: Path) -> QualityReport:
        """Perform comprehensive validation of a session"""
        
        session_id = session_path.name
        logger.info(f"Starting validation of session: {session_id}")
        
        results = []
        data_summary = {}
        
        try:
            # Load session data
            gsr_data, sync_marks, metadata = await self._load_session_data(session_path)
            
            if gsr_data is None:
                logger.error(f"No GSR data found for session {session_id}")
                return self._create_failed_report(session_id, "No GSR data found")
            
            data_summary = {
                'gsr_samples': len(gsr_data),
                'sync_marks': len(sync_marks) if sync_marks else 0,
                'has_metadata': metadata is not None,
                'file_count': len(list(session_path.glob('*')))
            }
            
            # Perform validation checks
            results.extend(await self._validate_completeness(gsr_data, sync_marks))
            results.extend(await self._validate_accuracy(gsr_data))
            results.extend(await self._validate_consistency(gsr_data))
            results.extend(await self._validate_timeliness(gsr_data))
            results.extend(await self._validate_data_validity(gsr_data))
            results.extend(await self._validate_file_integrity(session_path))
            
            # Calculate overall score
            overall_score = self._calculate_overall_score(results)
            
            # Generate recommendations
            recommendations = self._generate_recommendations(results, data_summary)
            
            # Create report
            report = QualityReport(
                session_id=session_id,
                validation_level=self.validation_level,
                overall_score=overall_score,
                results=results,
                recommendations=recommendations,
                timestamp=datetime.now(),
                data_summary=data_summary
            )
            
            # Save report
            await self._save_quality_report(session_path, report)
            
            logger.info(f"Validation completed for session {session_id}: Score {overall_score:.2%}")
            return report
            
        except Exception as e:
            logger.error(f"Error validating session {session_id}: {e}")
            return self._create_failed_report(session_id, str(e))
    
    async def _load_session_data(self, session_path: Path) -> Tuple[Optional[List], Optional[List], Optional[Dict]]:
        """Load session data files"""
        
        gsr_data = None
        sync_marks = None
        metadata = None
        
        # Load GSR data
        gsr_files = list(session_path.glob('gsr_data_*.csv'))
        if gsr_files:
            gsr_file = max(gsr_files, key=lambda x: x.stat().st_mtime)
            try:
                with open(gsr_file, 'r') as f:
                    reader = csv.DictReader(f)
                    gsr_data = list(reader)
            except Exception as e:
                logger.error(f"Error loading GSR data: {e}")
        
        # Load sync marks
        sync_file = session_path / 'sync_marks.csv'
        if sync_file.exists():
            try:
                with open(sync_file, 'r') as f:
                    reader = csv.DictReader(f)
                    sync_marks = list(reader)
            except Exception as e:
                logger.error(f"Error loading sync marks: {e}")
        
        # Load metadata
        metadata_file = session_path / 'session_metadata.json'
        if metadata_file.exists():
            try:
                with open(metadata_file, 'r') as f:
                    metadata = json.load(f)
            except Exception as e:
                logger.error(f"Error loading metadata: {e}")
        
        return gsr_data, sync_marks, metadata
    
    async def _validate_completeness(self, gsr_data: List[Dict], sync_marks: List[Dict]) -> List[ValidationResult]:
        """Validate data completeness"""
        results = []
        
        # Check GSR data completeness
        total_samples = len(gsr_data)
        missing_gsr = sum(1 for row in gsr_data if not row.get('gsr_microsiemens'))
        
        if total_samples > 0:
            completeness = 1.0 - (missing_gsr / total_samples)
        else:
            completeness = 0.0
        
        passed = completeness >= self.quality_thresholds['completeness_min']
        
        results.append(ValidationResult(
            metric=QualityMetric.COMPLETENESS,
            passed=passed,
            score=completeness,
            message=f"Data completeness: {completeness:.1%} ({missing_gsr} missing of {total_samples})",
            details={
                'total_samples': total_samples,
                'missing_samples': missing_gsr,
                'completeness_score': completeness
            }
        ))
        
        # Check required columns
        if gsr_data:
            required_columns = self.validation_rules['file_formats']['csv_required_columns']
            available_columns = set(gsr_data[0].keys())
            missing_columns = set(required_columns) - available_columns
            
            column_completeness = 1.0 if not missing_columns else 0.0
            
            results.append(ValidationResult(
                metric=QualityMetric.COMPLETENESS,
                passed=len(missing_columns) == 0,
                score=column_completeness,
                message=f"Column completeness: {'All required columns present' if not missing_columns else f'Missing: {missing_columns}'}",
                details={
                    'required_columns': required_columns,
                    'available_columns': list(available_columns),
                    'missing_columns': list(missing_columns)
                }
            ))
        
        return results
    
    async def _validate_accuracy(self, gsr_data: List[Dict]) -> List[ValidationResult]:
        """Validate data accuracy"""
        results = []
        
        # Validate GSR value ranges
        gsr_range = self.validation_rules['gsr_range']
        valid_values = 0
        total_values = 0
        out_of_range_count = 0
        
        for row in gsr_data:
            gsr_str = row.get('gsr_microsiemens', '')
            if gsr_str:
                try:
                    gsr_value = float(gsr_str)
                    total_values += 1
                    
                    if gsr_range['min'] <= gsr_value <= gsr_range['max']:
                        valid_values += 1
                    else:
                        out_of_range_count += 1
                        
                except ValueError:
                    total_values += 1  # Count invalid formats
        
        if total_values > 0:
            accuracy = valid_values / total_values
        else:
            accuracy = 0.0
        
        passed = accuracy >= self.quality_thresholds['accuracy_min']
        
        results.append(ValidationResult(
            metric=QualityMetric.ACCURACY,
            passed=passed,
            score=accuracy,
            message=f"GSR value accuracy: {accuracy:.1%} ({out_of_range_count} out of range)",
            details={
                'total_values': total_values,
                'valid_values': valid_values,
                'out_of_range': out_of_range_count,
                'valid_range': gsr_range
            }
        ))
        
        # Validate timestamp formats
        valid_timestamps = 0
        total_timestamps = 0
        
        timestamp_pattern = re.compile(self.validation_rules['timestamp_format']['pattern'])
        
        for row in gsr_data:
            timestamp = row.get('timestamp', '')
            datetime_str = row.get('datetime', '')
            
            if timestamp or datetime_str:
                total_timestamps += 1
                
                # Check timestamp format
                if datetime_str and timestamp_pattern.match(datetime_str):
                    valid_timestamps += 1
                elif timestamp:
                    try:
                        float(timestamp)  # Unix timestamp
                        valid_timestamps += 1
                    except ValueError:
                        pass
        
        if total_timestamps > 0:
            timestamp_accuracy = valid_timestamps / total_timestamps
        else:
            timestamp_accuracy = 0.0
        
        results.append(ValidationResult(
            metric=QualityMetric.ACCURACY,
            passed=timestamp_accuracy >= 0.95,  # High standard for timestamps
            score=timestamp_accuracy,
            message=f"Timestamp format accuracy: {timestamp_accuracy:.1%}",
            details={
                'total_timestamps': total_timestamps,
                'valid_timestamps': valid_timestamps,
                'expected_format': self.validation_rules['timestamp_format']['pattern']
            }
        ))
        
        return results
    
    async def _validate_consistency(self, gsr_data: List[Dict]) -> List[ValidationResult]:
        """Validate data consistency"""
        results = []
        
        if len(gsr_data) < 2:
            results.append(ValidationResult(
                metric=QualityMetric.CONSISTENCY,
                passed=False,
                score=0.0,
                message="Insufficient data for consistency check",
                details={'sample_count': len(gsr_data)}
            ))
            return results
        
        # Check sampling rate consistency
        timestamps = []
        for row in gsr_data:
            timestamp_str = row.get('timestamp', '')
            if timestamp_str:
                try:
                    timestamps.append(float(timestamp_str))
                except ValueError:
                    continue
        
        if len(timestamps) >= 2:
            timestamps.sort()
            intervals = [timestamps[i+1] - timestamps[i] for i in range(len(timestamps)-1)]
            
            # Filter out zero intervals (duplicates)
            intervals = [i for i in intervals if i > 0]
            
            if intervals:
                expected_interval = 1.0 / self.validation_rules['sampling_rate']['expected']
                tolerance = self.validation_rules['sampling_rate']['tolerance']
                
                consistent_intervals = sum(
                    1 for interval in intervals
                    if abs(interval - expected_interval) <= expected_interval * tolerance
                )
                
                consistency_score = consistent_intervals / len(intervals)
            else:
                consistency_score = 0.0
        else:
            consistency_score = 0.0
        
        passed = consistency_score >= self.quality_thresholds['consistency_min']
        
        results.append(ValidationResult(
            metric=QualityMetric.CONSISTENCY,
            passed=passed,
            score=consistency_score,
            message=f"Sampling rate consistency: {consistency_score:.1%}",
            details={
                'expected_rate': self.validation_rules['sampling_rate']['expected'],
                'tolerance': self.validation_rules['sampling_rate']['tolerance'],
                'consistent_intervals': consistent_intervals if 'consistent_intervals' in locals() else 0,
                'total_intervals': len(intervals) if 'intervals' in locals() else 0
            }
        ))
        
        # Check data sequence integrity
        sequence_breaks = 0
        if len(timestamps) >= 2:
            max_gap = expected_interval * 5  # Allow up to 5 missed samples
            
            for i in range(len(timestamps) - 1):
                gap = timestamps[i+1] - timestamps[i]
                if gap > max_gap:
                    sequence_breaks += 1
        
        sequence_score = 1.0 - (sequence_breaks / max(1, len(timestamps) - 1))
        
        results.append(ValidationResult(
            metric=QualityMetric.CONSISTENCY,
            passed=sequence_breaks <= len(timestamps) * 0.01,  # Max 1% breaks
            score=sequence_score,
            message=f"Sequence integrity: {sequence_breaks} breaks detected",
            details={
                'sequence_breaks': sequence_breaks,
                'sequence_score': sequence_score
            }
        ))
        
        return results
    
    async def _validate_timeliness(self, gsr_data: List[Dict]) -> List[ValidationResult]:
        """Validate data timeliness"""
        results = []
        
        if not gsr_data:
            results.append(ValidationResult(
                metric=QualityMetric.TIMELINESS,
                passed=False,
                score=0.0,
                message="No data for timeliness validation"
            ))
            return results
        
        # Check if data is recent
        now = time.time()
        latest_timestamp = 0
        
        for row in gsr_data:
            timestamp_str = row.get('timestamp', '')
            if timestamp_str:
                try:
                    timestamp = float(timestamp_str)
                    latest_timestamp = max(latest_timestamp, timestamp)
                except ValueError:
                    continue
        
        if latest_timestamp > 0:
            age_seconds = now - latest_timestamp
            tolerance = self.quality_thresholds['timeliness_tolerance']
            
            timeliness_score = max(0.0, 1.0 - (age_seconds / (tolerance * 10)))  # Scale factor
            passed = age_seconds <= tolerance
            
            results.append(ValidationResult(
                metric=QualityMetric.TIMELINESS,
                passed=passed,
                score=timeliness_score,
                message=f"Data timeliness: {age_seconds:.1f}s old (tolerance: {tolerance}s)",
                details={
                    'data_age_seconds': age_seconds,
                    'tolerance_seconds': tolerance,
                    'latest_timestamp': latest_timestamp
                }
            ))
        else:
            results.append(ValidationResult(
                metric=QualityMetric.TIMELINESS,
                passed=False,
                score=0.0,
                message="No valid timestamps found for timeliness check"
            ))
        
        return results
    
    async def _validate_data_validity(self, gsr_data: List[Dict]) -> List[ValidationResult]:
        """Validate overall data validity"""
        results = []
        
        if not gsr_data:
            results.append(ValidationResult(
                metric=QualityMetric.VALIDITY,
                passed=False,
                score=0.0,
                message="No data for validity validation"
            ))
            return results
        
        # Check for duplicate timestamps
        timestamps = []
        for row in gsr_data:
            timestamp_str = row.get('timestamp', '')
            if timestamp_str:
                timestamps.append(timestamp_str)
        
        unique_timestamps = len(set(timestamps))
        duplicate_count = len(timestamps) - unique_timestamps
        
        if len(timestamps) > 0:
            uniqueness_score = unique_timestamps / len(timestamps)
        else:
            uniqueness_score = 0.0
        
        results.append(ValidationResult(
            metric=QualityMetric.VALIDITY,
            passed=duplicate_count <= len(timestamps) * 0.01,  # Max 1% duplicates
            score=uniqueness_score,
            message=f"Timestamp uniqueness: {duplicate_count} duplicates found",
            details={
                'total_timestamps': len(timestamps),
                'unique_timestamps': unique_timestamps,
                'duplicate_count': duplicate_count
            }
        ))
        
        # Check for logical consistency in GSR values
        gsr_values = []
        for row in gsr_data:
            gsr_str = row.get('gsr_microsiemens', '')
            if gsr_str:
                try:
                    gsr_values.append(float(gsr_str))
                except ValueError:
                    continue
        
        if len(gsr_values) >= 3:
            # Check for impossible jumps (more than 50% change between consecutive samples)
            impossible_jumps = 0
            for i in range(len(gsr_values) - 1):
                if gsr_values[i] > 0:  # Avoid division by zero
                    change_ratio = abs(gsr_values[i+1] - gsr_values[i]) / gsr_values[i]
                    if change_ratio > 0.5:  # More than 50% change
                        impossible_jumps += 1
            
            logical_score = 1.0 - (impossible_jumps / max(1, len(gsr_values) - 1))
        else:
            logical_score = 0.0
            impossible_jumps = 0
        
        results.append(ValidationResult(
            metric=QualityMetric.VALIDITY,
            passed=impossible_jumps <= len(gsr_values) * 0.02,  # Max 2% impossible jumps
            score=logical_score,
            message=f"Logical consistency: {impossible_jumps} impossible value jumps detected",
            details={
                'total_values': len(gsr_values),
                'impossible_jumps': impossible_jumps,
                'logical_score': logical_score
            }
        ))
        
        return results
    
    async def _validate_file_integrity(self, session_path: Path) -> List[ValidationResult]:
        """Validate file integrity and structure"""
        results = []
        
        # Check required files exist
        required_files = ['gsr_data_*.csv']
        optional_files = ['sync_marks.csv', 'session_metadata.json']
        
        required_found = 0
        for pattern in required_files:
            if list(session_path.glob(pattern)):
                required_found += 1
        
        required_score = required_found / len(required_files)
        
        results.append(ValidationResult(
            metric=QualityMetric.INTEGRITY,
            passed=required_score == 1.0,
            score=required_score,
            message=f"Required files: {required_found}/{len(required_files)} found",
            details={
                'required_files': required_files,
                'found_count': required_found
            }
        ))
        
        # Check file sizes (should not be empty)
        file_sizes = {}
        integrity_issues = 0
        
        for file_path in session_path.iterdir():
            if file_path.is_file():
                size = file_path.stat().st_size
                file_sizes[file_path.name] = size
                
                if size == 0:
                    integrity_issues += 1
        
        if file_sizes:
            size_integrity_score = 1.0 - (integrity_issues / len(file_sizes))
        else:
            size_integrity_score = 0.0
        
        results.append(ValidationResult(
            metric=QualityMetric.INTEGRITY,
            passed=integrity_issues == 0,
            score=size_integrity_score,
            message=f"File integrity: {integrity_issues} empty files found",
            details={
                'file_sizes': file_sizes,
                'empty_files': integrity_issues
            }
        ))
        
        return results
    
    def _calculate_overall_score(self, results: List[ValidationResult]) -> float:
        """Calculate overall quality score"""
        if not results:
            return 0.0
        
        # Weight different metrics
        weights = {
            QualityMetric.COMPLETENESS: 0.25,
            QualityMetric.ACCURACY: 0.25,
            QualityMetric.CONSISTENCY: 0.20,
            QualityMetric.TIMELINESS: 0.10,
            QualityMetric.VALIDITY: 0.15,
            QualityMetric.INTEGRITY: 0.05
        }
        
        weighted_scores = {}
        metric_counts = {}
        
        for result in results:
            metric = result.metric
            if metric not in weighted_scores:
                weighted_scores[metric] = 0.0
                metric_counts[metric] = 0
            
            weighted_scores[metric] += result.score
            metric_counts[metric] += 1
        
        # Average scores for each metric
        for metric in weighted_scores:
            if metric_counts[metric] > 0:
                weighted_scores[metric] /= metric_counts[metric]
        
        # Calculate weighted overall score
        total_score = 0.0
        total_weight = 0.0
        
        for metric, weight in weights.items():
            if metric in weighted_scores:
                total_score += weighted_scores[metric] * weight
                total_weight += weight
        
        return total_score / total_weight if total_weight > 0 else 0.0
    
    def _generate_recommendations(self, results: List[ValidationResult], 
                                data_summary: Dict[str, Any]) -> List[str]:
        """Generate quality improvement recommendations"""
        recommendations = []
        
        # Analyze results by metric
        metric_issues = {}
        for result in results:
            if not result.passed:
                metric = result.metric
                if metric not in metric_issues:
                    metric_issues[metric] = []
                metric_issues[metric].append(result)
        
        # Generate specific recommendations
        if QualityMetric.COMPLETENESS in metric_issues:
            recommendations.append("Improve data collection completeness - check sensor connectivity and power stability")
        
        if QualityMetric.ACCURACY in metric_issues:
            recommendations.append("Review sensor calibration and value ranges - some readings are outside expected parameters")
        
        if QualityMetric.CONSISTENCY in metric_issues:
            recommendations.append("Address timing inconsistencies - verify system clock synchronization and sampling rate")
        
        if QualityMetric.TIMELINESS in metric_issues:
            recommendations.append("Ensure timely data processing - reduce system latency and processing delays")
        
        if QualityMetric.VALIDITY in metric_issues:
            recommendations.append("Investigate data validity issues - check for duplicate timestamps and logical inconsistencies")
        
        if QualityMetric.INTEGRITY in metric_issues:
            recommendations.append("Verify file integrity - ensure all required files are present and properly formatted")
        
        # Session-specific recommendations
        if data_summary.get('gsr_samples', 0) < 1000:
            recommendations.append("Consider longer recording sessions for more robust data analysis")
        
        if data_summary.get('sync_marks', 0) == 0:
            recommendations.append("Add synchronization marks to improve data alignment and analysis capabilities")
        
        if not recommendations:
            recommendations.append("Data quality meets validation standards - no specific improvements needed")
        
        return recommendations
    
    def _create_failed_report(self, session_id: str, error_message: str) -> QualityReport:
        """Create a failed validation report"""
        return QualityReport(
            session_id=session_id,
            validation_level=self.validation_level,
            overall_score=0.0,
            results=[ValidationResult(
                metric=QualityMetric.INTEGRITY,
                passed=False,
                score=0.0,
                message=f"Validation failed: {error_message}"
            )],
            recommendations=[
                f"Resolve validation error: {error_message}",
                "Ensure session data files are present and accessible",
                "Check file permissions and data integrity"
            ],
            timestamp=datetime.now(),
            data_summary={}
        )
    
    async def _save_quality_report(self, session_path: Path, report: QualityReport):
        """Save quality report to file"""
        try:
            report_path = session_path / "quality_report.json"
            
            # Convert report to serializable dict
            report_data = {
                'session_id': report.session_id,
                'validation_level': report.validation_level.value,
                'overall_score': report.overall_score,
                'timestamp': report.timestamp.isoformat(),
                'data_summary': report.data_summary,
                'recommendations': report.recommendations,
                'results': [
                    {
                        'metric': result.metric.value,
                        'passed': result.passed,
                        'score': result.score,
                        'message': result.message,
                        'details': result.details,
                        'timestamp': result.timestamp.isoformat()
                    }
                    for result in report.results
                ]
            }
            
            with open(report_path, 'w') as f:
                json.dump(report_data, f, indent=2)
                
            logger.info(f"Quality report saved to {report_path}")
            
        except Exception as e:
            logger.error(f"Error saving quality report: {e}")


class BatchValidator:
    """Batch validation of multiple sessions"""
    
    def __init__(self, validation_level: ValidationLevel = ValidationLevel.STANDARD):
        self.validator = DataValidator(validation_level)
        self.reports: Dict[str, QualityReport] = {}
    
    async def validate_all_sessions(self, data_directory: Path) -> Dict[str, QualityReport]:
        """Validate all sessions in directory"""
        
        logger.info(f"Starting batch validation with {self.validator.validation_level.value} level")
        
        for session_path in data_directory.iterdir():
            if session_path.is_dir():
                session_id = session_path.name
                logger.info(f"Validating session: {session_id}")
                
                report = await self.validator.validate_session(session_path)
                self.reports[session_id] = report
        
        logger.info(f"Batch validation completed: {len(self.reports)} sessions processed")
        return self.reports
    
    def generate_summary_report(self, output_path: Path = None) -> Path:
        """Generate batch validation summary report"""
        
        output_path = output_path or Path("validation_summary.json")
        
        if not self.reports:
            logger.warning("No validation reports to summarize")
            return output_path
        
        # Calculate aggregate statistics
        total_sessions = len(self.reports)
        passed_sessions = sum(1 for report in self.reports.values() if report.overall_score >= 0.8)
        
        scores = [report.overall_score for report in self.reports.values()]
        avg_score = sum(scores) / len(scores)
        
        # Analyze common issues
        all_recommendations = []
        for report in self.reports.values():
            all_recommendations.extend(report.recommendations)
        
        # Count recommendation frequency
        rec_counts = {}
        for rec in all_recommendations:
            rec_counts[rec] = rec_counts.get(rec, 0) + 1
        
        common_issues = sorted(rec_counts.items(), key=lambda x: x[1], reverse=True)[:5]
        
        summary = {
            'validation_timestamp': datetime.now().isoformat(),
            'validation_level': self.validator.validation_level.value,
            'summary_statistics': {
                'total_sessions': total_sessions,
                'passed_sessions': passed_sessions,
                'pass_rate': passed_sessions / total_sessions if total_sessions > 0 else 0,
                'average_score': avg_score,
                'min_score': min(scores) if scores else 0,
                'max_score': max(scores) if scores else 0
            },
            'common_issues': [
                {'recommendation': rec, 'frequency': count} 
                for rec, count in common_issues
            ],
            'session_results': [
                {
                    'session_id': session_id,
                    'overall_score': report.overall_score,
                    'passed': report.overall_score >= 0.8,
                    'validation_timestamp': report.timestamp.isoformat(),
                    'issues_count': sum(1 for result in report.results if not result.passed)
                }
                for session_id, report in self.reports.items()
            ]
        }
        
        try:
            with open(output_path, 'w') as f:
                json.dump(summary, f, indent=2)
            
            logger.info(f"Validation summary saved to {output_path}")
            
        except Exception as e:
            logger.error(f"Error saving validation summary: {e}")
        
        return output_path


if __name__ == "__main__":
    # Demo usage
    async def demo():
        # Create validator
        validator = DataValidator(ValidationLevel.RESEARCH_GRADE)
        
        # Create batch validator
        batch_validator = BatchValidator(ValidationLevel.STANDARD)
        
        # Example validation (would need actual session data)
        data_dir = Path("sessions")
        if data_dir.exists():
            reports = await batch_validator.validate_all_sessions(data_dir)
            summary_path = batch_validator.generate_summary_report()
            
            print(f"Validated {len(reports)} sessions")
            print(f"Summary report saved to: {summary_path}")
        else:
            print("No sessions directory found - create some test data first")
    
    asyncio.run(demo())