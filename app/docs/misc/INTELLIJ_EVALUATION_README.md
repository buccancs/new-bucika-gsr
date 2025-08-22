# IntelliJ IDEA Evaluation Guide for Samsung S22 Android 15

Complete guide for running Chapter 5 evaluation infrastructure from PC using IntelliJ IDEA with Samsung Galaxy S22 Android 15 as the primary testing platform.

## 🚀 Quick Start (5 Minutes)

### Prerequisites Check
- **IntelliJ IDEA** 2023.3+ (Ultimate or Community Edition)
- **Java 11+** (for Android development)
- **Python 3.10+** (for evaluation scripts)
- **Samsung Galaxy S22** with **Android 15** (SDK 35+)
- **ADB** (Android Debug Bridge) installed and accessible

### Immediate Setup
```bash
# 1. Clone and open in IntelliJ
git clone https://github.com/buccancs/bucika_gsr.git
# Open in IntelliJ IDEA: File → Open → Select bucika_gsr folder

# 2. Quick validation (run in IntelliJ Terminal)
python run_local_tests.py android_quick

# 3. Verify Samsung S22 Android 15 connection
adb devices
# Should show: device (not unauthorized)
```

## 📋 IntelliJ IDEA Setup

### 1. Project Import Configuration

**Step 1: Import Project**
1. **File** → **Open** → Select `bucika_gsr` directory
2. **Trust Project** when prompted
3. **Import Gradle project** when detected
4. Wait for indexing to complete (~2-3 minutes)

**Step 2: Configure Project Structure**
1. **File** → **Project Structure** (Ctrl+Alt+Shift+S)
2. **Project Settings** → **Project**:
   - **Project SDK**: Java 11 or higher
   - **Project language level**: 11 or higher
3. **Project Settings** → **Modules**:
   - Verify `AndroidApp` module is detected
   - Verify `PythonApp` module is detected

**Step 3: Install Required Plugins**
1. **File** → **Settings** → **Plugins**
2. Install/Enable:
   - **Android** (should be pre-installed)
   - **Python** (for evaluation scripts)
   - **Kotlin** (should be pre-installed)
   - **Gradle** (should be pre-installed)

### 2. Android Development Setup

**Step 1: Android SDK Configuration**
1. **File** → **Settings** → **Appearance & Behaviour** → **System Settings** → **Android SDK**
2. **SDK Platforms**: Ensure **Android 15 (API level 35)** is installed
3. **SDK Tools**: Verify these are installed:
   - Android SDK Build-Tools 35.0.0+
   - Android SDK Platform-Tools (for ADB)
   - Android SDK Tools
   - Android Emulator (optional)

**Step 2: Device Connection Setup**
1. **View** → **Tool Windows** → **Device Manager**
2. Connect Samsung S22 via USB
3. Enable **Developer Options** on Samsung S22:
   - **Settings** → **About phone** → Tap **Build number** 7 times
   - **Settings** → **Developer options** → **USB debugging** (Enable)
4. Accept ADB debugging prompt on device
5. Verify connection: **Terminal** → `adb devices`

### 3. Python Environment Setup

**Step 1: Python Interpreter Configuration**
1. **File** → **Settings** → **Project** → **Python Interpreter**
2. **Add Interpreter** → **System Interpreter**
3. Select Python 3.10+ installation
4. **Apply** and **OK**

**Step 2: Install Dependencies**
```bash
# In IntelliJ Terminal (View → Tool Windows → Terminal)
pip install -r test-requirements.txt
pip install pytest numpy psutil opencv-python-headless
```

## 🧪 Running Chapter 5 Evaluation

### 1. Academic Integrity Enforcement

**CRITICAL**: All evaluation tests enforce academic integrity by requiring **REAL Samsung S22 Android 15 hardware**. NO fake data, mock data, or simulated data is allowed.

**Verification Steps:**
1. **Connect Samsung S22 Android 15** via USB debugging
2. **Verify device detection**:
   ```bash
   # In IntelliJ Terminal
   python -c "
   import subprocess
   result = subprocess.run(['adb', 'devices'], capture_output=True, text=True)
   print('Samsung S22 detected:' if 'device' in result.stdout else 'NO DEVICE DETECTED')
   "
   ```
3. **Run integrity check**:
   ```bash
   python tests_unified/evaluation/measurement_collection.py --verify-hardware
   ```

### 2. Evaluation Test Execution

#### Option A: Using IntelliJ Run Configurations

**Step 1: Create Run Configuration for Quick Evaluation**
1. **Run** → **Edit Configurations**
2. **+** → **Python**
3. **Configuration:**
   - **Name**: `Samsung S22 Quick Evaluation`
   - **Script path**: `run_local_tests.py`
   - **Parameters**: `android_quick`
   - **Working directory**: Project root
4. **Apply** → **OK**
5. **Run** using green arrow or **Shift+F10**

**Step 2: Create Run Configuration for Full Evaluation**
1. **Run** → **Edit Configurations**
2. **+** → **Python**
3. **Configuration:**
   - **Name**: `Samsung S22 Full Evaluation`
   - **Script path**: `run_local_tests.py`
   - **Parameters**: `android_comprehensive`
   - **Working directory**: Project root
4. **Apply** → **OK**

**Step 3: Create Run Configuration for Measurement Collection**
1. **Run** → **Edit Configurations**
2. **+** → **Python**
3. **Configuration:**
   - **Name**: `Chapter 5 Measurement Collection`
   - **Script path**: `tests_unified/evaluation/measurement_collection.py`
   - **Parameters**: `--device samsung-s22 --android-version 15`
   - **Working directory**: Project root
4. **Apply** → **OK**

#### Option B: Using IntelliJ Terminal

**Quick Evaluation (2-3 minutes):**
```bash
# Samsung S22 Android 15 quick validation
python run_local_tests.py android_quick

# Device-specific validation
python android_device_testing_demo.py --mode detect
```

**Comprehensive Evaluation (15-30 minutes):**
```bash
# Full Samsung S22 Android 15 testing suite
python run_local_tests.py android_comprehensive

# IDE integration testing
python run_local_tests.py android_ide
```

**Measurement Collection for Chapter 5:**
```bash
# Generate CSV artifacts for thesis evaluation
python tests_unified/evaluation/measurement_collection.py

# Specific measurement categories
python tests_unified/evaluation/measurement_collection.py --category synchronisation
python tests_unified/evaluation/measurement_collection.py --category calibration
python tests_unified/evaluation/measurement_collection.py --category network
```

### 3. Generated Artifacts

**Location**: `test_results/chapter5_artifacts/`

**Files Generated:**
- `drift_results.csv` - Synchronisation accuracy measurements
- `calib_metrics.csv` - Camera calibration accuracy data
- `net_bench.csv` - Network performance benchmarks
- `measurement_summary.json` - Metadata and generation info

**Viewing Results in IntelliJ:**
1. **Project View** → Navigate to `test_results/chapter5_artifacts/`
2. **Double-click CSV files** to view in built-in table editor
3. **Right-click** → **Open in Terminal** to access via command line

## 📊 Monitoring and Debugging

### 1. Real-time Monitoring

**Using IntelliJ Console:**
1. **Run evaluation** using Run Configuration
2. **Monitor output** in **Run** tool window
3. **Check device connectivity** in real-time:
   ```bash
   # In separate terminal tab
   watch -n 2 'adb devices'
   ```

**Using IntelliJ Debug Mode:**
1. **Add breakpoints** in evaluation scripts
2. **Run** → **Debug** instead of **Run**
3. **Step through code** to verify Samsung S22 detection logic

### 2. Log Analysis

**IntelliJ Log Viewer:**
1. **View** → **Tool Windows** → **Event Log**
2. **Filter** for Python and Android-related events
3. **Double-click** entries for detailed information

**Test Logs:**
```bash
# View latest test execution logs
cat test_results/latest/test_log.txt

# Monitor live log output
tail -f test_results/latest/test_log.txt
```

### 3. Performance Monitoring

**IntelliJ Built-in Profiler:**
1. **Run** → **Profile** (for Python scripts)
2. **Monitor CPU and Memory usage**
3. **Analyse performance bottlenecks**

**System Resource Monitoring:**
```bash
# Monitor Samsung S22 device resources
adb shell top

# Monitor PC resources during evaluation
python -c "
import psutil
import time
while True:
    print(f'CPU: {psutil.cpu_percent()}%, Memory: {psutil.virtual_memory().percent}%')
    time.sleep(5)
"
```

## 🛠️ Advanced Configuration

### 1. Custom Evaluation Configurations

**Create Custom Test Suite:**
1. **Copy existing configuration**: `android_comprehensive`
2. **Modify parameters** in `run_local_tests.py`:
   ```python
   # Add custom Samsung S22 Android 15 specific tests
   elif mode == 'samsung_s22_custom':
       test_categories = ['ui_tests', 'performance_tests', 'hardware_integration']
       return run_samsung_s22_evaluation(test_categories)
   ```

**Environment Variables in IntelliJ:**
1. **Run Configuration** → **Environment variables**
2. **Add variables:**
   - `GSR_TEST_LOG_LEVEL=DEBUG` (detailed logging)
   - `GSR_TEST_DEVICE_COUNT=1` (single Samsung S22)
   - `GSR_TEST_DURATION=300` (5-minute tests)
   - `ANDROID_TARGET_SDK=35` (Android 15)

### 2. Multi-Device Setup

**Multiple Samsung S22 Devices:**
1. **Connect additional devices** via USB or wireless debugging
2. **Verify detection**:
   ```bash
   adb devices
   # Should show multiple devices
   ```
3. **Run multi-device evaluation**:
   ```bash
   python tests_unified/evaluation/measurement_collection.py --device-count 2
   ```

**Wireless Debugging Setup (Android 15):**
1. **Samsung S22**: **Settings** → **Developer options** → **Wireless debugging**
2. **Pair with computer** using QR code or pairing code
3. **Connect via ADB**:
   ```bash
   adb connect <SAMSUNG_S22_IP>:5555
   ```

## 📈 Results Analysis

### 1. CSV Data Analysis in IntelliJ

**Built-in CSV Viewer:**
1. **Open CSV files** in IntelliJ
2. **Sort and filter columns**
3. **Export to Excel** if needed

**Python Analysis Scripts:**
```python
# Quick data analysis in IntelliJ Python Console
import pandas as pd
import matplotlib.pyplot as plt

# Load synchronisation data
sync_data = pd.read_csv('test_results/chapter5_artifacts/drift_results.csv')
print(f"Median drift: {sync_data['drift_ms'].median():.2f}ms")
print(f"95th percentile: {sync_data['drift_ms'].quantile(0.95):.2f}ms")

# Plot distribution
sync_data['drift_ms'].hist(bins=20)
plt.title('Samsung S22 Android 15 Synchronisation Accuracy')
plt.xlabel('Drift (ms)')
plt.ylabel('Frequency')
plt.show()
```

### 2. Integration with Thesis

**Academic Compliance Verification:**
```bash
# Verify all data is from real Samsung S22 Android 15 hardware
python tests_unified/evaluation/compliance/verify_academic_integrity.py

# Generate thesis-ready summary
python tests_unified/evaluation/metrics/generate_thesis_summary.py
```

**Artifact Validation:**
- All CSV files contain `measurement_source: REAL_SAMSUNG_S22_ANDROID_15_HARDWARE`
- No `fake_data_flag` or `simulated_data_flag` entries
- Timestamps match actual test execution times
- Device IDs correspond to real Samsung S22 hardware

## 🚨 Troubleshooting

### 1. Common IntelliJ Issues

**Problem: Project not detecting Android modules**
```bash
# Solution: Refresh Gradle project
# In IntelliJ: Gradle tool window → Refresh button
# Or Terminal: ./gradlew clean build
```

**Problem: Python interpreter not found**
```bash
# Solution: Configure Python interpreter
# File → Settings → Project → Python Interpreter → Add Interpreter
```

**Problem: ADB not recognised**
```bash
# Solution: Add Android SDK platform-tools to PATH
# Windows: Add to PATH: C:\Users\<USER>\AppData\Local\Android\Sdk\platform-tools
# macOS/Linux: Add to ~/.bashrc: export PATH=$PATH:~/Android/Sdk/platform-tools
```

### 2. Samsung S22 Android 15 Issues

**Problem: Device not detected**
```bash
# Check USB debugging is enabled
adb devices

# If "unauthorized", accept debug prompt on Samsung S22
# If "offline", try:
adb kill-server
adb start-server
```

**Problem: Academic integrity violations**
```bash
# Verify real hardware connection
python -c "
from tests_unified.evaluation.measurement_collection import SynchronizationAccuracyCollector
collector = SynchronizationAccuracyCollector('.')
try:
    collector._detect_samsung_s22_android15_devices()
    print('✅ Samsung S22 Android 15 detected')
except RuntimeError as e:
    print(f'❌ {e}')
"
```

**Problem: Evaluation tests failing**
```bash
# Enable debug mode
export GSR_TEST_LOG_LEVEL=DEBUG
python run_local_tests.py android_quick

# Check device capabilities
adb shell getprop ro.build.version.sdk
# Should return: 35 (Android 15)
```

### 3. Performance Issues

**Problem: Slow test execution**
```bash
# Reduce test scope
python run_local_tests.py android_quick  # Instead of comprehensive

# Monitor system resources
# IntelliJ: Help → Diagnostic Tools → Activity Monitor
```

**Problem: Memory issues during evaluation**
```bash
# Increase IntelliJ memory
# Help → Edit Custom VM Options
# Add: -Xmx4g (4GB heap)

# Monitor Python memory usage
python -c "
import psutil
process = psutil.Process()
print(f'Memory usage: {process.memory_info().rss / 1024 / 1024:.1f}MB')
"
```

## 📚 Additional Resources

### IntelliJ IDEA Documentation
- [Android Development in IntelliJ](https://www.jetbrains.com/help/idea/android.html)
- [Python Plugin for IntelliJ](https://www.jetbrains.com/help/idea/python.html)
- [Run/Debug Configurations](https://www.jetbrains.com/help/idea/run-debug-configuration.html)

### Project-Specific Guides
- [`ANDROID_TESTING_README.md`](ANDROID_TESTING_README.md) - Comprehensive Android testing
- [`CHAPTER5_TESTING_SUMMARY.md`](CHAPTER5_TESTING_SUMMARY.md) - Testing implementation overview
- [`tests_unified/README.md`](tests_unified/README.md) - Unified testing framework

### Samsung S22 Android 15 Resources
- [Android 15 Developer Guide](https://developer.android.com/about/versions/15)
- [Samsung Developer Documentation](https://developer.samsung.com/)
- [ADB User Guide](https://developer.android.com/studio/command-line/adb)

## 🎯 Success Criteria

### Evaluation Completion Checklist
- [ ] **IntelliJ IDEA** properly configured with Android and Python support
- [ ] **Samsung Galaxy S22** with **Android 15** connected and detected
- [ ] **Quick evaluation** (`android_quick`) passes without errors
- [ ] **Comprehensive evaluation** (`android_comprehensive`) completes successfully
- [ ] **CSV artifacts** generated in `test_results/chapter5_artifacts/`
- [ ] **Academic integrity** verified (no fake data flags)
- [ ] **Measurement summary** shows `REAL_SAMSUNG_S22_ANDROID_15_HARDWARE` source
- [ ] **All test categories** show PASS status
- [ ] **Performance metrics** within acceptable thresholds

### Quality Assurance Validation
```bash
# Final validation script
python tests_unified/evaluation/compliance/final_validation.py \
    --device samsung-s22 \
    --android-version 15 \
    --artifacts-dir test_results/chapter5_artifacts/ \
    --generate-report
```

---

**Status**: ✅ **IntelliJ Ready** | 🚀 **Samsung S22 Android 15 Optimised** | 🔒 **Academic Integrity Enforced** | 📊 **Thesis Evaluation Ready**

For additional support or questions, refer to the troubleshooting section or check the project's GitHub Issues page.