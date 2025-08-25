# Testing Guide - Bucika GSR PC Orchestrator

## 🧪 Comprehensive Test Suite

### Test Coverage Overview
- **Unit Tests**: 60+ tests covering all major modules
- **Integration Tests**: 8 tests for service interactions  
- **Performance Tests**: Resource usage and optimization validation
- **Protocol Tests**: WebSocket message handling and validation
- **Error Recovery Tests**: Fault tolerance and recovery scenarios

### Running Tests

#### Quick Test Run
```bash
# Run all tests with verbose output
python -m pytest tests/ -v

# Run with coverage report
python -m pytest tests/ --cov=src --cov-report=html

# Run specific test categories
python -m pytest tests/test_integration.py -v      # Integration tests
python -m pytest tests/test_protocol.py -v        # Protocol tests
```

#### Advanced Testing
```bash
# Run tests with performance profiling
python -m pytest tests/ -v --profile

# Run tests with memory usage monitoring
python -m pytest tests/ -v --memprof

# Run only fast tests (exclude slow integration tests)
python -m pytest tests/ -v -m "not slow"

# Run tests in parallel for faster execution
python -m pytest tests/ -v -n 4
```

### Test Categories

#### Core Protocol Tests (`test_protocol.py`)
```python
# Message creation and validation
def test_hello_payload_creation()
def test_register_payload_creation() 
def test_gsr_sample_payload_creation()
def test_sync_mark_payload_creation()
def test_message_envelope_creation()
```

#### Session Management Tests (`test_session_manager.py`)
```python
# Session lifecycle management
def test_start_session()
def test_stop_session()
def test_gsr_sample_storage()
def test_sync_mark_recording()
def test_multiple_sync_marks()
```

#### Integration Tests (`test_integration.py`)
```python
# End-to-end system testing
def test_orchestrator_initialization()
def test_service_startup_sequence()
def test_data_analysis_integration()
def test_validation_system_integration()
def test_error_recovery_integration()
def test_performance_monitoring_integration()
```

#### WebSocket Server Tests (`test_websocket_server.py`)
```python
# Client communication testing
def test_hello_message_handling()
def test_start_message_handling()
def test_gsr_data_message_handling()
def test_file_upload_handling()
def test_broadcast_functionality()
def test_connection_cleanup()
```

#### Data Analysis Tests (`test_data_analyzer.py`)
```python
# Research-grade analysis validation
def test_basic_analysis()
def test_artifact_detection()
def test_quality_score_calculation()
def test_visualization_generation()
def test_batch_analysis()
```

#### Data Validation Tests (`test_data_validator.py`)
```python
# Multi-level validation testing
def test_basic_validation_levels()
def test_data_completeness_validation()
def test_data_consistency_validation()
def test_research_grade_validation()
def test_batch_validation()
```

#### Error Recovery Tests (`test_error_recovery.py`)
```python
# Fault tolerance validation
def test_error_classification()
def test_recovery_strategy_selection()
def test_max_retries_exhausted()
def test_error_escalation()
def test_pattern_detection()
```

#### Performance Monitor Tests (`test_performance_monitor.py`)
```python
# Resource monitoring validation
def test_system_metrics_collection()
def test_monitoring_lifecycle()
def test_threshold_monitoring()
def test_performance_optimization_recommendations()
def test_memory_leak_detection()
```

#### Discovery Service Tests (`test_discovery_service.py`)
```python
# mDNS service testing
def test_service_registration()
def test_service_discovery()
def test_network_interface_detection()
def test_service_update_handling()
def test_graceful_shutdown()
```

#### Time Sync Service Tests (`test_time_sync_service.py`)
```python
# High-precision timing validation
def test_sync_request_parsing()
def test_sync_response_creation()
def test_clock_drift_calculation()
def test_high_precision_timing()
def test_multiple_concurrent_requests()
```

### Test Data Management

#### Creating Test Sessions
```python
def create_sample_gsr_data(session_path: Path, num_samples: int = 1000):
    """Create realistic GSR data for testing"""
    # Generate 128Hz GSR data with realistic variation
    # Include timestamps, quality flags, and metadata
    
def create_test_sync_marks(session_path: Path):
    """Create synchronization markers for testing"""
    # Generate stimulus timing markers
    # Include descriptions and metadata
```

#### Test Configuration
```python
# Test configuration in pytest.ini
[tool:pytest]
testpaths = tests
python_files = test_*.py
python_classes = Test*
python_functions = test_*
asyncio_mode = auto
markers =
    slow: marks tests as slow (deselect with '-m "not slow"')
    integration: marks tests as integration tests
    unit: marks tests as unit tests
```

### Continuous Integration

#### GitHub Actions Integration
```yaml
name: Python Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: [3.8, 3.9, '3.10', '3.11']
    steps:
    - uses: actions/checkout@v3
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: ${{ matrix.python-version }}
    - name: Install dependencies
      run: |
        pip install -r requirements.txt
        pip install pytest pytest-cov pytest-asyncio
    - name: Run tests
      run: python -m pytest tests/ -v --cov=src
```

### Performance Testing

#### Benchmark Tests
```bash
# Run performance benchmarks
python -m pytest tests/test_performance.py -v --benchmark-only

# Generate performance report
python scripts/performance_benchmark.py
```

#### Load Testing
```bash
# Test with multiple simulated clients
python scripts/load_test.py --clients 50 --duration 300

# Memory usage validation
python scripts/memory_test.py --sessions 100
```

### Test Best Practices

#### Writing New Tests
1. **Use descriptive names** - `test_websocket_handles_invalid_json_gracefully`
2. **Follow AAA pattern** - Arrange, Act, Assert
3. **Test edge cases** - Empty data, network failures, resource limits
4. **Mock external dependencies** - Network services, file systems
5. **Validate error handling** - Ensure graceful failures

#### Test Data Management
1. **Use temporary directories** - Clean up after each test
2. **Generate realistic data** - Match production data patterns
3. **Parameterize tests** - Test multiple scenarios efficiently
4. **Isolate test cases** - No dependencies between tests

#### Debugging Test Failures
```bash
# Run single test with detailed output
python -m pytest tests/test_specific.py::test_function -v -s

# Debug with pdb
python -m pytest tests/test_specific.py::test_function --pdb

# Capture logging output
python -m pytest tests/test_specific.py -v --capture=no --log-cli-level=DEBUG
```

### Quality Gates

#### Required Test Coverage
- **Unit Tests**: >90% line coverage for all modules
- **Integration Tests**: All major workflows covered  
- **Error Handling**: All exception paths tested
- **Performance**: No memory leaks or resource exhaustion

#### Test Execution Requirements
- All tests must pass before merging
- No test warnings or deprecation notices
- Performance tests must meet baseline thresholds
- Integration tests must pass with real network conditions