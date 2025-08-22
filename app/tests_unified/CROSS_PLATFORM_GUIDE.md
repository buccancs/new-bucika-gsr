# Cross-Platform Testing Guide - **CONSOLIDATED** ✅

This guide explains how to run the **fully consolidated** unified testing framework on Windows, Linux, and macOS systems. All scattered test files have been successfully moved and are now discoverable through unified commands.

## 🌐 Cross-Platform Overview

The Multi-Sensor Recording System testing framework is designed to work consistently across all major operating systems with **completed test consolidation**:

- **Windows 10/11** - Full support with multiple runner options
- **Linux** (Ubuntu, Fedora, etc.) - Native support with bash scripts  
- **macOS** - Full compatibility using Unix-style scripts
- **All Platforms** - **4 scattered test files now consolidated** into proper structure

## 📋 Consolidation Status

**COMPLETED** ✅ across all platforms:
- ✅ Root-level test files moved to unified structure
- ✅ Evaluation tests reorganized into 6 logical categories
- ✅ Cross-platform compatibility verified
- ✅ Consistent test discovery on all operating systems

## 📦 Prerequisites

### All Platforms
- **Python 3.7+** - Required for all testing frameworks
- **pip** - Python package manager
- **Git** - For repository management

### Platform-Specific
- **Windows**: PowerShell 5.0+ (for PowerShell scripts)
- **Linux/macOS**: Bash shell (standard on all distributions)

## 🚀 Quick Start by Platform

### Windows

#### Option 1: Universal Python Runner (Recommended)
```cmd
# Quick test suite
python run_local_tests.py

# Install dependencies and run comprehensive tests
python run_local_tests.py full --install-deps

# GUI testing for both platforms
python run_local_tests.py gui
```

#### Option 2: Windows Batch Script
```cmd
# Quick test suite
run_local_tests.bat

# Install dependencies first
run_local_tests.bat quick --install-deps

# Platform-specific testing
run_local_tests.bat pc
run_local_tests.bat android
```

#### Option 3: PowerShell Script
```powershell
# Quick test suite
.\run_local_tests.ps1

# With dependency installation
.\run_local_tests.ps1 -Mode full -InstallDeps

# Requirements validation
.\run_local_tests.ps1 -Mode requirements
```

### Linux

#### Option 1: Universal Python Runner (Recommended)
```bash
# Quick test suite
python3 run_local_tests.py

# Install dependencies and run tests
python3 run_local_tests.py full --install-deps

# Performance benchmarks
python3 run_local_tests.py performance
```

#### Option 2: Bash Script
```bash
# Make executable (first time only)
chmod +x run_local_tests.sh

# Quick test suite
./run_local_tests.sh

# Full test suite with dependency installation
./run_local_tests.sh full --install-deps

# GUI testing
./run_local_tests.sh gui
```

### macOS

#### Option 1: Universal Python Runner (Recommended)
```bash
# Quick test suite
python3 run_local_tests.py

# Install dependencies and run tests
python3 run_local_tests.py full --install-deps

# Cross-platform GUI testing
python3 run_local_tests.py gui
```

#### Option 2: Bash Script
```bash
# Make executable (first time only)
chmod +x run_local_tests.sh

# Quick test suite
./run_local_tests.sh

# Requirements validation
./run_local_tests.sh requirements
```

## 🧪 Testing Consolidated Files Cross-Platform

### Verify Consolidation Success

**Test moved files on all platforms:**

#### Windows
```cmd
# Test consolidated Python unit tests
python -m pytest tests_unified/unit/python/test_device_connectivity.py -v
python -m pytest tests_unified/unit/python/test_thermal_recorder_security_fix.py -v

# Test consolidated Android tests
python -m pytest tests_unified/unit/android/test_android_connection_detection.py -v

# Test consolidated integration tests
python -m pytest tests_unified/integration/device_coordination/test_pc_server_integration.py -v
```

#### Linux/macOS
```bash
# Test consolidated Python unit tests
python3 -m pytest tests_unified/unit/python/test_device_connectivity.py -v
python3 -m pytest tests_unified/unit/python/test_thermal_recorder_security_fix.py -v

# Test consolidated Android tests
python3 -m pytest tests_unified/unit/android/test_android_connection_detection.py -v

# Test consolidated integration tests  
python3 -m pytest tests_unified/integration/device_coordination/test_pc_server_integration.py -v
```

### Test Reorganized Evaluation Categories

**Cross-platform evaluation testing:**

#### All Platforms
```bash
# Test new organized evaluation structure
python -m pytest tests_unified/evaluation/architecture/ -v
python -m pytest tests_unified/evaluation/research/ -v
python -m pytest tests_unified/evaluation/framework/ -v
python -m pytest tests_unified/evaluation/data_collection/ -v
python -m pytest tests_unified/evaluation/foundation/ -v
python -m pytest tests_unified/evaluation/metrics/ -v
```

## 🔧 Installation & Setup

### Windows Setup

1. **Install Python**:
   ```cmd
   # Download from python.org or use Windows Store
   # Ensure Python is added to PATH
   python --version
   ```

2. **Install Dependencies**:
   ```cmd
   # Navigate to project root
   cd path\to\bucika_gsr
   
   # Auto-install dependencies
   python run_local_tests.py quick --install-deps
   ```

3. **Colour Support** (Optional):
   ```cmd
   pip install colorama
   ```

### Linux Setup

1. **Install Python** (if not already installed):
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install python3 python3-pip
   
   # Fedora/RHEL
   sudo dnf install python3 python3-pip
   
   # Arch Linux
   sudo pacman -S python python-pip
   ```

2. **Install Dependencies**:
   ```bash
   # Navigate to project root
   cd /path/to/bucika_gsr
   
   # Auto-install dependencies
   python3 run_local_tests.py quick --install-deps
   ```

### macOS Setup

1. **Install Python**:
   ```bash
   # Using Homebrew (recommended)
   brew install python3
   
   # Or download from python.org
   ```

2. **Install Dependencies**:
   ```bash
   # Navigate to project root
   cd /path/to/bucika_gsr
   
   # Auto-install dependencies
   python3 run_local_tests.py quick --install-deps
   ```

## 📊 Test Modes Available

All platforms support the following test modes:

| Mode | Description | Typical Duration |
|------|-------------|------------------|
| `quick` | Essential tests only | ~2 minutes |
| `full` | Complete test suite | ~10-15 minutes |
| `requirements` | FR/NFR validation | ~5 minutes |
| `performance` | Benchmarks & profiling | ~8 minutes |
| `ci` | CI/CD optimised tests | ~5 minutes |
| `pc` | Desktop application tests | ~6 minutes |
| `android` | Mobile application tests | ~7 minutes |
| `gui` | GUI tests both platforms | ~12 minutes |

## 🎯 Platform-Specific Examples

### Windows Examples

```cmd
# Basic testing
python run_local_tests.py

# Development workflow
python run_local_tests.py quick --install-deps

# Pre-commit validation
python run_local_tests.py requirements

# Platform-specific testing
python run_local_tests.py pc
python run_local_tests.py android

# Performance validation
python run_local_tests.py performance

# GUI testing
python run_local_tests.py gui
```

### Linux Examples

```bash
# Basic testing
python3 run_local_tests.py

# Development workflow
./run_local_tests.sh quick --install-deps

# Pre-commit validation
./run_local_tests.sh requirements

# CI/CD simulation
./run_local_tests.sh ci

# Full test suite
./run_local_tests.sh full
```

### macOS Examples

```bash
# Basic testing
python3 run_local_tests.py

# Development workflow
python3 run_local_tests.py quick --install-deps

# Research validation
python3 run_local_tests.py requirements

# Performance profiling
python3 run_local_tests.py performance
```

## 🔍 Direct Framework Usage

For advanced users, the unified framework can be used directly on all platforms:

### Windows
```cmd
python tests_unified\runners\run_unified_tests.py --help
python tests_unified\runners\run_unified_tests.py --level unit
python tests_unified\runners\run_unified_tests.py --category android
python tests_unified\runners\run_unified_tests.py --validate-requirements
```

### Linux/macOS
```bash
python3 tests_unified/runners/run_unified_tests.py --help
python3 tests_unified/runners/run_unified_tests.py --level integration
python3 tests_unified/runners/run_unified_tests.py --category hardware
python3 tests_unified/runners/run_unified_tests.py --report-requirements-coverage
```

## ⚠️ Troubleshooting

### Common Issues

#### Python Not Found
**Windows**:
```cmd
# Try alternative Python commands
py run_local_tests.py
python3 run_local_tests.py
```

**Linux/macOS**:
```bash
# Try alternative Python commands
python run_local_tests.py
python3 run_local_tests.py
```

#### Permission Denied (Linux/macOS)
```bash
# Make scripts executable
chmod +x run_local_tests.sh
chmod +x tests_unified/runners/run_unified_tests.py
```

#### Missing Dependencies
```bash
# All platforms - install dependencies
python run_local_tests.py quick --install-deps

# Manual installation
pip install -r test-requirements.txt
pip install -e .
```

#### PowerShell Execution Policy (Windows)
```powershell
# If PowerShell script execution is blocked
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Platform-Specific Solutions

#### Windows Specific
- Ensure Python is in PATH
- Install Visual C++ Build Tools if needed
- Use PowerShell as Administrator for system-wide installations

#### Linux Specific
- Install python3-dev package for some dependencies
- Ensure adequate permissions for test file creation
- Install X11 libraries for GUI testing

#### macOS Specific
- Install Xcode Command Line Tools
- Use virtual environments to avoid system Python conflicts
- Install homebrew for easy dependency management

## 📈 Performance Optimisation

### Windows
- Use Python 3.9+ for better performance
- Install on SSD for faster I/O
- Close unnecessary applications during testing

### Linux
- Use Python 3.8+ compiled with optimizations
- Ensure adequate RAM (4GB+ recommended)
- Use tmpfs for temporary test files if available

### macOS
- Use Python 3.8+ from Homebrew
- Ensure adequate disk space for test artifacts
- Close resource-intensive applications

## 🔄 CI/CD Integration

The cross-platform testing framework integrates seamlessly with CI/CD systems:

### GitHub Actions
- Linux runners: Use bash scripts or Python runner
- Windows runners: Use PowerShell or Python runner
- macOS runners: Use bash scripts or Python runner

### Local CI Simulation
```bash
# All platforms
python run_local_tests.py ci

# Platform-specific CI simulation
./run_local_tests.sh ci  # Linux/macOS
run_local_tests.bat ci   # Windows
```

## 📚 Additional Resources

- [Main Testing Documentation](README.md)
- [GitHub Workflows Guide](GITHUB_WORKFLOWS.md)
- [Getting Started Guide](GETTING_STARTED.md)
- [Infrastructure Summary](INFRASTRUCTURE_SUMMARY.md)

## 💡 Best Practices

1. **Use the Universal Python Runner** for consistency across platforms
2. **Install dependencies** using `--install-deps` flag for new setups
3. **Run quick tests** during development for rapid feedback
4. **Use requirements mode** before commits to ensure compliance
5. **Test on target platform** before deployment
6. **Keep Python updated** to latest stable version
7. **Use virtual environments** to isolate dependencies

This cross-platform approach ensures that the Multi-Sensor Recording System testing framework provides consistent, reliable testing across all development and deployment environments.