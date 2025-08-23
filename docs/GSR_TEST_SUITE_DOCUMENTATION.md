# GSR Test Suite Documentation

## Overview

This document describes the comprehensive test suite for the BucikaGSR ShimmerAndroidAPI integration. The test suite includes unit tests, integration tests, UI tests, and performance tests to ensure reliable operation of all GSR functionality.

## Test Structure

### Directory Organization

```
app/src/test/                              # Unit tests
├── java/com/topdon/tc001/gsr/
│   ├── GSRManagerTest.kt                  # Core GSR manager tests
│   ├── GSRTestSuite.kt                   # Basic infrastructure validation
│   ├── data/
│   │   └── GSRDataWriterTest.kt          # Data writing and export tests
│   └── ui/
│       └── ShimmerSensorPanelTest.kt     # UI configuration panel tests
├── java/com/shimmerresearch/android/
│   └── ShimmerBluetoothTest.kt           # Bluetooth connectivity tests

app/src/androidTest/                       # Integration and UI tests
├── java/com/topdon/tc001/gsr/
│   ├── GSRIntegrationTest.kt             # End-to-end integration tests
│   └── ui/
│       └── GSRActivityUITest.kt          # UI interaction tests
```

## Test Categories

### 1. Unit Tests

#### GSRManagerTest.kt
- **Purpose**: Tests core GSR manager functionality
- **Coverage**: 
  - Device connection management
  - Data listener registration
  - Configuration application
  - Error handling
  - Memory management
- **Key Test Methods**:
  - `testSingletonInstance()`
  - `testConnectToDevice()`
  - `testDataListenerCallback()`
  - `testConfigurationApplication()`

#### GSRDataWriterTest.kt  
- **Purpose**: Tests file system integration and data writing
- **Coverage**:
  - CSV file creation and writing
  - Real-time data queuing
  - Data export functionality
  - File management utilities
  - Performance with high data rates
- **Key Test Methods**:
  - `testFileCreation()`
  - `testBatchDataWriting()`
  - `testRealTimeDataQueuing()`
  - `testDataExport()`

#### ShimmerSensorPanelTest.kt
- **Purpose**: Tests UI configuration panel functionality
- **Coverage**:
  - Sensor configuration options
  - Settings persistence
  - Configuration validation
  - UI state management
- **Key Test Methods**:
  - `testConfigurationGeneration()`
  - `testSensorBitmapGeneration()`
  - `testSettingsValidation()`

#### ShimmerBluetoothTest.kt
- **Purpose**: Tests Bluetooth connectivity and device communication
- **Coverage**:
  - Device discovery
  - Connection establishment
  - Data streaming
  - Realistic data generation
  - Error handling
- **Key Test Methods**:
  - `testBluetoothConnection()`
  - `testDataStreamingStart()`
  - `testDataGenerationRealistic()`
  - `testDeviceCompatibility()`

### 2. Integration Tests

#### GSRIntegrationTest.kt
- **Purpose**: Tests complete system workflows
- **Coverage**:
  - End-to-end data collection
  - Component integration
  - Settings API connectivity
  - Real-time data writing
  - Error recovery
  - Memory management
  - Concurrent operations
- **Key Test Methods**:
  - `testCompleteGSRWorkflow()`
  - `testSensorPanelConfigurationIntegration()`
  - `testAdvancedDataProcessingIntegration()`
  - `testRealTimeDataWritingIntegration()`

### 3. UI Tests

#### GSRActivityUITest.kt
- **Purpose**: Tests user interface interactions
- **Coverage**:
  - Activity launch
  - Button functionality
  - Settings access
  - Data visualization
  - Menu navigation
- **Key Test Methods**:
  - `testGSRActivityLaunch()`
  - `testConnectButtonFunctionality()`
  - `testGSRSettingsAccess()`
  - `testDataVisualization()`

## Running Tests

### Prerequisites

Ensure all testing dependencies are included in `app/build.gradle`:
```gradle
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:4.11.0'
testImplementation 'org.robolectric:robolectric:4.10.3'
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
```

### Running Unit Tests

```bash
# Run all unit tests
./gradlew testDevDebugUnitTest

# Run specific test class
./gradlew testDevDebugUnitTest --tests="GSRManagerTest"

# Run tests with coverage report
./gradlew testDevDebugUnitTest jacocoTestReport
```

### Running Integration Tests

```bash
# Run all integration tests (requires connected device/emulator)
./gradlew connectedDevDebugAndroidTest

# Run specific integration test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.topdon.tc001.gsr.GSRIntegrationTest
```

### Running UI Tests

```bash
# Run UI tests (requires connected device/emulator)
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.topdon.tc001.gsr.ui.GSRActivityUITest
```

## Test Configuration

### Mock Configuration

Tests use Mockito for mocking dependencies:
```kotlin
@Mock
private lateinit var mockContext: Context

@Mock
private lateinit var mockShimmerDataListener: GSRManager.GSRDataListener

@Before
fun setUp() {
    MockitoAnnotations.openMocks(this)
    // Configure mock behavior
}
```

### Test Data

Tests include realistic test data generation:
```kotlin
private fun createTestGSRDataPoint(): GSRDataPoint {
    return GSRDataPoint(
        timestamp = System.currentTimeMillis(),
        gsrValue = 425.7,
        skinTemperature = 32.4,
        signalQuality = 0.88,
        batteryLevel = 85
    )
}
```

## Test Coverage Goals

### Coverage Targets
- **Unit Tests**: >90% line coverage for core components
- **Integration Tests**: >80% coverage for critical workflows
- **UI Tests**: >70% coverage for user interactions

### Coverage Analysis

Generate and view coverage reports:
```bash
# Generate coverage report
./gradlew testDevDebugUnitTest jacocoTestReport

# View report (opens in browser)
open app/build/reports/jacoco/testDevDebugUnitTest/html/index.html
```

## Performance Testing

### Data Collection Performance

Tests verify system performance under load:
```kotlin
@Test
fun testBackgroundWritingPerformance() {
    // Simulate 128 Hz data rate for 5 seconds
    repeat(640) { i ->
        val dataPoint = createTestGSRDataPoint()
        gsrDataWriter.queueDataPoint(dataPoint)
    }
    // Verify all data is processed successfully
}
```

### Memory Usage Testing

Tests monitor memory consumption:
```kotlin
@Test
fun testMemoryManagementIntegration() {
    val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    // Simulate extended data collection
    // Verify memory increase is reasonable
}
```

## Continuous Integration

### Automated Testing

Configure GitHub Actions for automated testing:
```yaml
name: GSR Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Unit Tests
        run: ./gradlew testDevDebugUnitTest
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
```

## Test Maintenance

### Adding New Tests

When adding new functionality:
1. Create corresponding unit tests
2. Add integration tests for workflows
3. Include UI tests for user-facing features
4. Update documentation

### Test Data Management

- Use realistic physiological values
- Include edge cases and error conditions
- Validate data formats and ranges
- Test with various device configurations

## Quality Assurance

### Test Quality Metrics

- **Assertion Quality**: Each test should have meaningful assertions
- **Test Independence**: Tests should not depend on each other
- **Test Clarity**: Test names should clearly describe what is being tested
- **Error Coverage**: Include tests for error conditions and edge cases

### Code Review Guidelines

- All new code must include tests
- Test coverage should not decrease
- Tests should be reviewed for quality and completeness
- Performance tests should be included for critical paths

This comprehensive test suite ensures the reliability and quality of the BucikaGSR ShimmerAndroidAPI integration across all components and use cases.