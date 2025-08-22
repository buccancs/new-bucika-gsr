# Android Device Testing Suite - **CONSOLIDATED** ✅

Comprehensive Android device testing with IDE integration and wireless debugging support for the Multi-Sensor Recording System. **All Android tests have been consolidated** into the unified testing framework.

## Overview

This testing suite provides comprehensive validation of Android devices connected through IntelliJ IDEA or Android Studio. **All scattered Android test files have been moved** to the consolidated structure:

- **Device Detection**: USB, wireless debugging, and emulator detection
- **IDE Integration**: Android Studio and IntelliJ IDEA connection detection  
- **Comprehensive Testing**: UI, functional, requirements, integration, performance, compatibility, and security tests
- **Real-time Monitoring**: Continuous device and IDE status monitoring
- **Cross-platform Support**: Windows, macOS, and Linux
- **✅ Consolidated Structure**: All Android tests now in `tests_unified/unit/android/` and `tests_unified/integration/android/`

## Quick Start - **UPDATED FOR CONSOLIDATION** ✅

### 1. Consolidated Android Testing
```bash
# Test moved Android connection detection (moved from root)
python -m pytest tests_unified/unit/android/test_android_connection_detection.py -v

# Run all consolidated Android unit tests
python -m pytest tests_unified/unit/android/ -v

# Use unified framework for Android testing
python tests_unified/runners/run_unified_tests.py --category android
```

### 2. Legacy Device Detection (Still Available)
```bash
# Detect connected devices and IDEs
python android_device_testing_demo.py --mode detect

# Quick validation of Android setup
python run_local_tests.py android_quick
```

### 3. Comprehensive Testing
```bash
# Full testing suite via unified framework
python run_local_tests.py android_comprehensive
```

# IDE integration testing
python run_local_tests.py android_ide

# Standard Android tests
python run_local_tests.py android
```

### 3. Continuous Monitoring
```bash
# Monitor devices for 60 seconds
python android_device_testing_demo.py --mode monitor --duration 60
```

## Test Categories

The comprehensive test suite includes:

### UI Tests
- App launch and navigation
- Recording controls validation
- Settings and configuration UI
- Camera and Bluetooth interface testing

### Functional Tests  
- Shimmer device connection
- GSR data collection
- Thermal camera functionality
- Webcam recording
- Data synchronisation
- File storage operations
- Network communication
- Session management

### Requirements Tests
- Android version compatibility
- Bluetooth availability
- Camera permissions
- Storage requirements
- Network connectivity
- Sensor capabilities
- Performance thresholds

### Integration Tests
- PC-Android communication
- Shimmer-Android integration
- Multi-device synchronisation
- Data pipeline validation

### Performance Tests
- App launch time measurement
- Memory usage analysis
- CPU usage monitoring
- Battery consumption
- Data throughput testing

### Compatibility Tests
- API level compatibility
- Screen size adaptation
- Hardware compatibility
- Device model validation

### Security Tests
- App permissions verification
- Data encryption validation
- Network security checks

## Prerequisites

### Required Software
- Python 3.7+ with project dependencies
- Android SDK with ADB (for device communication)
- IntelliJ IDEA or Android Studio (for IDE integration)

### Optional (for full testing)
- Connected Android device with USB debugging enabled
- Wireless debugging setup (Android 11+)
- Shimmer GSR device for hardware integration tests

### Setup Instructions
1. **Install Android SDK**: Download from [Android Developer site](https://developer.android.com/studio)
2. **Add ADB to PATH**: Ensure `adb` command is available in terminal
3. **Enable USB Debugging**: On Android device, enable Developer Options and USB Debugging
4. **Setup Wireless Debugging**: For wireless testing, pair device via ADB over TCP/IP

## Usage Examples

### Command Line Interface

```bash
# Show all available modes
python android_device_testing_demo.py --help

# Quick status check
python android_device_testing_demo.py --mode status

# Detection with JSON output
python android_device_testing_demo.py --mode detect --json-output

# Full demo with comprehensive testing
python android_device_testing_demo.py --mode full --comprehensive
```

### Programmatic Usage

```python
from tests_unified.android_test_integration import AndroidTestIntegration

# Initialise integration
integration = AndroidTestIntegration()

# Detect devices and IDEs
results = integration.detect_android_devices_and_ides()
print(f"Found {len(results['devices'])} devices and {len(results['ides'])} IDEs")

# Run quick validation
validation = integration.run_quick_android_validation()
if validation['success']:
    print(f"Validation passed: {validation['success_rate']:.1f}% success rate")

# Run comprehensive tests
test_results = integration.run_android_device_tests(
    test_categories=['ui_tests', 'functional_tests'],
    generate_report=True
)
```

### Integration with Test Runner

```bash
# Use the integrated test runner
python run_local_tests.py android_quick          # Quick validation
python run_local_tests.py android_comprehensive  # Full testing suite  
python run_local_tests.py android_ide            # IDE integration tests
python run_local_tests.py android                # Standard Android tests
```

## Wireless Debugging Setup

### Android 11+ Devices
1. Enable Developer Options
2. Enable Wireless Debugging  
3. Pair with computer using QR code or pairing code
4. Connect via `adb connect <IP>:<PORT>`

### Detection Capabilities
- Automatically detects wireless debugging connections
- Extracts IP addresses and port numbers
- Validates wireless debugging settings
- Supports multiple simultaneous connections

## IDE Integration Features

### Supported IDEs
- **Android Studio**: All versions with Android project support
- **IntelliJ IDEA**: With Android plugin enabled
- **VS Code**: With Android development extensions

### Detection Capabilities
- Process detection and version identification
- Project path analysis for Android development
- Device correlation with IDE connections
- Active project monitoring

### Integration Testing
- Validates IDE-device communication
- Tests project compilation and deployment
- Monitors debugging session establishment
- Verifies device visibility in IDE

## Output and Reporting

### Report Generation
Tests generate comprehensive reports including:

- **JSON Reports**: Machine-readable test results
- **Markdown Reports**: Human-readable summary with recommendations
- **CSV Data**: Test results for analysis and trending
- **Artifacts**: Screenshots, logs, and diagnostic files

### Example Report Structure
```
android_test_results/
├── comprehensive_test_report.json     # Complete results
├── android_integration_report.md     # Summary report
├── test_results.csv                  # Data for analysis
├── test_summary.txt                  # Human-readable summary
└── device_<ID>/                      # Per-device artifacts
    ├── screenshots/
    ├── logs/
    └── diagnostic_files/
```

### Real-time Monitoring
- Live device connection status
- IDE state monitoring
- Test execution progress
- Performance metrics tracking

## Troubleshooting

### Common Issues

#### No Devices Detected
- **Check USB Connection**: Ensure device is properly connected
- **Enable USB Debugging**: In Developer Options on Android device
- **Install ADB**: Download Android SDK and add to PATH
- **Device Authorisation**: Accept ADB debugging prompt on device

#### ADB Not Available
- **Install Android SDK**: Download from official Android site
- **Update PATH**: Add `platform-tools` directory to system PATH
- **Restart Terminal**: Reload environment variables
- **Check Permissions**: Ensure user has access to ADB executable

#### IDE Not Detected
- **Start IDE**: Ensure Android Studio or IntelliJ IDEA is running
- **Open Android Project**: Have an Android project loaded
- **Install Plugins**: Ensure Android plugin is installed and enabled
- **Check Processes**: Verify IDE processes are running

#### Wireless Debugging Issues
- **Android Version**: Requires Android 11 or higher
- **Network Connection**: Ensure device and computer on same network
- **Firewall Settings**: Allow ADB connections through firewall
- **Pairing Process**: Complete wireless debugging pairing first

### Debug Mode
```bash
# Enable verbose logging
python android_device_testing_demo.py --mode detect --verbose

# Check capabilities
python android_device_testing_demo.py --mode status

# Test individual components
python tests_unified/android_test_integration.py --mode detect --verbose
```

### Performance Optimisation
- **Parallel Testing**: Use `--parallel` flag for faster execution
- **Category Selection**: Run specific test categories only
- **Quick Mode**: Use quick validation for rapid feedback
- **Timeout Settings**: Adjust timeouts for slower devices

## Integration with CI/CD

### GitHub Actions
```yaml
- name: Android Device Testing
  run: |
    python run_local_tests.py android_quick
    python android_device_testing_demo.py --mode status --json-output
```

### Local Development Workflow
1. **Daily**: Run `android_quick` for fast validation
2. **Pre-commit**: Run `android` for standard testing
3. **Release**: Run `android_comprehensive` for full validation
4. **Debugging**: Use `android_ide` for IDE integration testing

## Advanced Features

### Custom Test Categories
```python
# Define custom test categories
custom_categories = ['ui_tests', 'performance_tests']
results = integration.run_android_device_tests(
    test_categories=custom_categories
)
```

### Device Filtering
```python
# Test specific devices only
target_devices = ['emulator-5554', '192.168.1.100:5555']
results = integration.run_android_device_tests(
    target_devices=target_devices
)
```

### Continuous Monitoring
```python
from tests_unified.android_device_comprehensive_tests import AndroidDeviceTestRunner

runner = AndroidDeviceTestRunner()
while True:
    devices = runner.detect_connected_devices()
    print(f"Devices: {len(devices)}")
    time.sleep(5)
```

## Contributing

When adding new Android tests:

1. **Follow Test Structure**: Use existing test categories and patterns
2. **Add Documentation**: Include clear test descriptions and expected outcomes
3. **Handle Gracefully**: Ensure tests gracefully handle missing devices/ADB
4. **Test Cross-Platform**: Verify functionality on Windows, macOS, and Linux
5. **Update Reports**: Ensure new tests appear in generated reports

### Example Test Addition
```python
def _execute_custom_test(self, device: AndroidDevice, test_name: str, artifacts_dir: str) -> TestExecution:
    """Execute a custom test implementation."""
    start_time = time.time()
    
    try:
        # Test implementation
        result = self._run_custom_logic(device, artifacts_dir)
        test_result = TestResult.PASS if result['success'] else TestResult.FAIL
    except Exception as e:
        test_result = TestResult.ERROR
        result = {'error': str(e), 'artifacts': []}
    
    return TestExecution(
        test_name=test_name,
        category=TestCategory.FUNCTIONAL_TESTS,  # Choose appropriate category
        result=test_result,
        duration=time.time() - start_time,
        device_id=device.device_id,
        error_message=result.get('error'),
        artifacts=result.get('artifacts', [])
    )
```

## Licence

This Android testing suite is part of the Multi-Sensor Recording System project. See the main project LICENSE file for details.