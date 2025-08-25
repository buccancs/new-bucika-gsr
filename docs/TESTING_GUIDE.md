# Testing Guide

## Overview

Comprehensive testing guide for the BucikaGSR platform covering unit tests, integration tests, and UI testing for all components.

## Test Dependencies

The project includes comprehensive testing dependencies configured in `shared.gradle`:

```gradle
dependencies {
    // Unit testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.11.0'
    testImplementation 'org.mockito:mockito-inline:4.11.0'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:4.1.0'
    testImplementation 'org.robolectric:robolectric:4.10.3'
    
    // Integration testing
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
}
```

## Running Tests

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run specific module tests
./gradlew :android:app:testDevDebugUnitTest
./gradlew :BleModule:test

# Run with coverage
./gradlew testDevDebugUnitTest jacocoTestReport
```

### Integration Tests
```bash
# Run all integration tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.topdon.tc001.gsr.GSRIntegrationTest
```

### UI Tests
```bash
# Run UI tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.topdon.tc001.ui
```

## Component-Specific Testing

### GSR Module Testing
- **GSRManager**: Connection, data processing, error handling
- **Shimmer Integration**: Device pairing, data streaming
- **Data Validation**: Signal quality, sampling rate accuracy

### Thermal Camera Testing  
- **TC001 Integration**: USB connection, frame capture
- **Thermal Processing**: Temperature calculation, frame rate
- **OpenCV Integration**: Image processing accuracy

### PC Orchestrator Testing
- **Session Management**: Multi-client coordination
- **Time Synchronization**: Precision validation
- **Data Storage**: File integrity, metadata accuracy

## Test Configuration

Create `test-config.properties` in project root:
```properties
# Test device configuration
test.shimmer.address=00:11:22:33:44:55
test.shimmer.name=TestShimmer3-GSR+
test.sampling.rate=128
test.data.directory=/data/data/com.topdon.tc001/files/test_data

# Test thresholds
test.sync.accuracy.ms=10
test.frame.rate.min=25
test.coverage.threshold=80
```

## Automated Testing

Tests run automatically on:
- Pull request creation
- Merge to main branch
- Nightly builds

See [CI_CD_README.md](../CI_CD_README.md) for CI/CD configuration details.