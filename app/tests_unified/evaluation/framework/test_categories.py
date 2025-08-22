from enum import Enum, auto
from typing import Dict, Any
from dataclasses import dataclass
class TestCategory(Enum):
    FOUNDATION = auto()
    INTEGRATION = auto()
    SYSTEM = auto()
    PERFORMANCE = auto()
class TestType(Enum):
    UNIT_ANDROID = auto()
    UNIT_PC = auto()
    ALGORITHM_VALIDATION = auto()
    MULTI_DEVICE = auto()
    NETWORK_PERFORMANCE = auto()
    SYNCHRONISATION = auto()
    ERROR_HANDLING = auto()
    END_TO_END = auto()
    DATA_INTEGRITY = auto()
    WORKFLOW_VALIDATION = auto()
    THROUGHPUT = auto()
    SCALABILITY = auto()
    RELIABILITY = auto()
    FAULT_TOLERANCE = auto()
    USABILITY = auto()
    PERFORMANCE = auto()
    STRESS_TEST = auto()
class TestPriority(Enum):
    CRITICAL = 1
    HIGH = 2
    MEDIUM = 3
    LOW = 4
@dataclass
class TestConfiguration:
    timeout_seconds: int = 300
    retry_attempts: int = 3
    parallel_execution: bool = False
    cleanup_on_failure: bool = True
    collect_metrics: bool = True
    generate_reports: bool = True
@dataclass
class QualityThresholds:
    minimum_success_rate: float = 0.95
    maximum_execution_time: float = 1800.0
    minimum_coverage: float = 0.80
    maximum_memory_mb: int = 1000
    maximum_cpu_percent: float = 60.0
    maximum_latency_ms: float = 100.0
    minimum_quality_score: float = 0.85
    sync_precision_ms: float = 1.0
    data_quality_score: float = 0.8
    measurement_accuracy: float = 0.95