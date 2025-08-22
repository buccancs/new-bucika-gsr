# Getting Started with Unified Testing Framework - **CONSOLIDATED** ✅

This guide provides a quick introduction to using the **fully consolidated** unified testing framework for the Multi-Sensor Recording System. All scattered test files have been successfully moved and evaluation tests reorganized.

## 📋 Consolidation Status

**COMPLETED** ✅: All test consolidation objectives achieved:
- ✅ **4 root-level test files** successfully moved to appropriate unified locations
- ✅ **Evaluation tests reorganized** into 6 logical categories with clear purpose
- ✅ **100% pytest discovery** verified for all consolidated tests
- ✅ **Import paths fixed** and validation completed
- ✅ **Documentation updated** to reflect new structure

## 🚀 Installation and Setup

### Prerequisites

- Python 3.10 or higher
- Git (for repository management)
- Basic command line knowledge

### Quick Setup

1. **Clone the repository** (if not already done):
   ```bash
   git clone https://github.com/buccancs/bucika_gsr.git
   cd bucika_gsr
   ```

2. **Install dependencies**:
   ```bash
   pip install -r test-requirements.txt
   pip install -e .
   ```

3. **Verify installation**:
   ```bash
   python tests_unified/runners/run_unified_tests.py --help
   ```

## 🏃 Running Your First Tests

### Option 1: Use the Simple Script (Recommended for Beginners)

```bash
# Run quick tests (< 2 minutes)
./run_local_tests.sh

# Run with dependency installation
./run_local_tests.sh quick --install-deps

# See all options
./run_local_tests.sh --help
```

### Option 2: Use the Unified Framework Directly

```bash
# Run all tests
python tests_unified/runners/run_unified_tests.py

# Quick validation
python tests_unified/runners/run_unified_tests.py --quick

# Specific test level
python tests_unified/runners/run_unified_tests.py --level unit
```

## 📊 Understanding Test Results

### Requirements Validation

The framework automatically validates that all thesis requirements are tested:

```bash
# Check requirements coverage
python tests_unified/runners/run_unified_tests.py --validate-requirements
```

**Expected Output:**
```
✅ ALL REQUIREMENTS HAVE TEST COVERAGE! (100.0%)
- Functional Requirements (FR): 8/8 covered
- Non-Functional Requirements (NFR): 7/7 covered
```

### Test Reports

Generate detailed reports:

```bash
# Detailed coverage report
python tests_unified/runners/run_unified_tests.py --report-requirements-coverage

# JSON format for automation
python tests_unified/runners/run_unified_tests.py --report-requirements-coverage --output-format json
```

## 🎯 Common Use Cases

### Newly Consolidated Test Examples

1. **Test consolidation validation**:
   ```bash
   # Verify all moved files work correctly
   python -m pytest tests_unified/unit/python/test_device_connectivity.py -v
   python -m pytest tests_unified/unit/python/test_thermal_recorder_security_fix.py -v
   python -m pytest tests_unified/unit/android/test_android_connection_detection.py -v
   python -m pytest tests_unified/integration/device_coordination/test_pc_server_integration.py -v
   ```

2. **Test reorganized evaluation categories**:
   ```bash
   # Test new evaluation structure (6 categories)
   python -m pytest tests_unified/evaluation/architecture/ -v
   python -m pytest tests_unified/evaluation/research/ -v
   python -m pytest tests_unified/evaluation/framework/ -v
   ```

### Developer Workflow

1. **Before committing code**:
   ```bash
   ./run_local_tests.sh quick
   ```

2. **Before pushing to main branch**:
   ```bash
   ./run_local_tests.sh full
   ```

3. **Performance testing**:
   ```bash
   ./run_local_tests.sh performance
   ```

### Research Workflow

1. **Academic compliance check**:
   ```bash
   ./run_local_tests.sh requirements
   ```

2. **Comprehensive research validation**:
   ```bash
   python tests_unified/runners/run_unified_tests.py --mode research --all-levels
   ```

3. **Thesis requirements validation**:
   ```bash
   python tests_unified/runners/run_unified_tests.py --validate-requirements
   ```

### CI/CD Integration

The framework is designed for GitHub Actions:

```yaml
# In your workflow
- name: Run Tests
  run: python tests_unified/runners/run_unified_tests.py --mode ci
```

## 🔧 Customisation

### Test Selection

```bash
# Run specific categories
python tests_unified/runners/run_unified_tests.py --category android
python tests_unified/runners/run_unified_tests.py --category hardware

# Run specific levels
python tests_unified/runners/run_unified_tests.py --level unit
python tests_unified/runners/run_unified_tests.py --level integration

# Combine options
python tests_unified/runners/run_unified_tests.py --level unit --category android
```

### Output Control

```bash
# Verbose output
python tests_unified/runners/run_unified_tests.py --verbose

# JSON output
python tests_unified/runners/run_unified_tests.py --output-format json

# Show slowest tests
python tests_unified/runners/run_unified_tests.py --durations 10
```

### Performance Options

```bash
# Parallel execution
python tests_unified/runners/run_unified_tests.py --parallel

# Extended timeouts
python tests_unified/runners/run_unified_tests.py --extended

# Performance benchmarks
python tests_unified/runners/run_unified_tests.py --performance-benchmarks
```

## 🚨 Troubleshooting

### Common Issues

1. **ModuleNotFoundError**:
   ```bash
   # Ensure proper installation
   pip install -e .
   export PYTHONPATH="${PYTHONPATH}:$(pwd)"
   ```

2. **Permission denied**:
   ```bash
   # Make scripts executable
   chmod +x run_local_tests.sh
   ```

3. **Tests fail to find files**:
   ```bash
   # Run from repository root
   cd /path/to/bucika_gsr
   python tests_unified/runners/run_unified_tests.py
   ```

### Getting Help

1. **Check framework help**:
   ```bash
   python tests_unified/runners/run_unified_tests.py --help
   ```

2. **Check script help**:
   ```bash
   ./run_local_tests.sh --help
   ```

3. **Enable debug mode**:
   ```bash
   python tests_unified/runners/run_unified_tests.py --verbose
   ```

## 📚 Next Steps

### Learn More

- **Comprehensive Documentation**: [`tests_unified/README.md`](README.md)
- **GitHub Integration**: [`tests_unified/GITHUB_WORKFLOWS.md`](GITHUB_WORKFLOWS.md)
- **Main Project README**: [`../README.md`](../README.md)

### Advanced Features

- **Requirements Analysis**: Learn about automated FR/NFR validation
- **Performance Benchmarking**: Deep dive into performance testing
- **Research Mode**: Comprehensive academic validation
- **Custom Categories**: Adding new test categories

### Contributing

To contribute to the testing framework:
1. Follow existing code structure
2. Add comprehensive documentation
3. Ensure requirements traceability
4. Maintain academic compliance standards

---

**Academic Standards**: This framework adheres to UCL academic standards for research integrity, ensuring reproducible results and comprehensive requirement validation for thesis compliance.