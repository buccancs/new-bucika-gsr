# Developer Guide - Bucika GSR Platform

## Overview

This comprehensive guide helps developers set up, understand, and contribute to the Bucika GSR multi-modal physiological data collection platform. The system integrates PC orchestration with Android clients for synchronized data collection.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Development Environment Setup](#development-environment-setup)
3. [Architecture Deep Dive](#architecture-deep-dive)
4. [Building and Running](#building-and-running)
5. [Testing Strategy](#testing-strategy)
6. [Contributing Guidelines](#contributing-guidelines)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Topics](#advanced-topics)

---

## Quick Start

### Prerequisites

- **Java Development Kit**: JDK 17 or higher
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **Gradle**: 8.10+ (included with project)
- **Git**: For version control
- **Hardware**: 
  - Shimmer3 GSR+ sensor (for GSR testing)
  - Topdon TC001 thermal camera (for thermal imaging)
  - Android device with USB OTG support

### 5-Minute Setup

```bash
# Clone the repository
git clone https://github.com/buccancs/new-bucika-gsr.git
cd new-bucika-gsr

# Build the entire project
./gradlew build

# Run PC orchestrator (GUI mode)
./gradlew :pc:run

# In another terminal, run console demo
./gradlew :pc:runDemo

# Build Android APK
./gradlew :android:app:assembleDevDebug
```

### Verify Installation

1. **PC Orchestrator**: Should start on `http://localhost:8080`
2. **mDNS Service**: Check for `_bucika-gsr._tcp` service
3. **Android Build**: APK created in `android/app/build/outputs/apk/`

---

## Development Environment Setup

### IDE Configuration

#### IntelliJ IDEA / Android Studio

```kotlin
// Recommended settings in .idea/misc.xml
<component name="ProjectRootManager" version="2" languageLevel="JDK_17" project-jdk-name="17" project-jdk-type="JavaSDK">
  <output url="file://$PROJECT_DIR$/build/classes" />
</component>
```

#### VS Code Extensions

- Kotlin Language Support
- Java Extension Pack
- Android iOS Emulator
- GitLens

### Project Structure

```
new-bucika-gsr/
├── android/                    # Android application modules
│   ├── app/                   # Main Android application
│   ├── libapp/                # Application library
│   ├── libcom/                # Communication library
│   └── ...                    # Additional Android modules
├── pc/                        # PC orchestrator application
│   ├── src/main/kotlin/       # PC orchestrator source
│   └── build.gradle.kts       # PC build configuration
├── docs/                      # Comprehensive documentation
├── shared-spec/               # Shared specifications
├── gradle/                    # Gradle wrapper and configs
└── build.gradle               # Root build configuration
```

### Environment Variables

Create `.env` file in project root:

```bash
# Development settings
BUCIKA_DEBUG=true
ORCHESTRATOR_PORT=8080
MDNS_SERVICE_NAME=_bucika-gsr._tcp
TIME_SYNC_PORT=9123

# Android settings
ANDROID_HOME=/path/to/android-sdk
JAVA_HOME=/path/to/jdk-17

# Logging
LOG_LEVEL=DEBUG
LOG_FILE=./logs/bucika.log
```

### Git Configuration

```bash
# Configure git hooks
./setup-dev.sh

# Set up commit message template
git config commit.template .gitmessage
```

---

## Architecture Deep Dive

### System Overview

```mermaid
graph TB
    subgraph "PC Orchestrator"
        direction TB
        WS[WebSocket Server] --> SM[Session Manager]
        MDNS[mDNS Discovery] --> WS
        UDP[Time Sync Service] --> TS[Time Synchronizer]
        SM --> DS[Data Storage]
        SM --> GUI[JavaFX GUI]
    end
    
    subgraph "Android Clients"
        direction TB
        OC[OrchestratorClient] --> OS[OrchestratorService]
        GSR[GSRManager] --> DW[GSRDataWriter]
        TC[Thermal Camera] --> VR[Video Recorder]
    end
    
    OC <--> WS
    GSR --> WS
    UDP --> OC
```

### Key Components

#### PC Orchestrator Components

1. **WebSocketServer**: Handles client connections and message routing
2. **SessionManager**: Manages recording sessions and device coordination
3. **TimeManager**: Provides high-precision time synchronization
4. **DataManager**: Stores and manages collected data files
5. **DiscoveryService**: Broadcasts mDNS service for auto-discovery

#### Android Components

1. **OrchestratorClient**: WebSocket client for PC communication
2. **OrchestratorService**: Foreground service for persistent connection
3. **GSRManager**: Shimmer sensor management and data processing
4. **GSRDataWriter**: Local data storage and file management
5. **ThermalCameraManager**: TC001 thermal camera integration

### Data Flow

```mermaid
sequenceDiagram
    participant A as Android Client
    participant P as PC Orchestrator
    participant F as File System
    
    A->>P: HELLO (device info)
    P->>A: REGISTER (session assignment)
    P->>A: START (begin recording)
    A->>A: Start GSR recording
    loop Real-time streaming
        A->>P: GSR_SAMPLE (128Hz data)
        P->>F: Store samples to CSV
    end
    P->>A: STOP (end recording)
    A->>A: Stop recording, prepare files
    loop File upload
        A->>P: UPLOAD_BEGIN/CHUNK/END
        P->>F: Store uploaded files
    end
    P->>A: ACK (session complete)
```

---

## Building and Running

### Build System Overview

The project uses a modular Gradle build system with:
- **Unified dependency management** via `shared.gradle` and `depend.gradle`  
- **Standardized configurations** across all modules
- **Product flavor consistency** for different deployment targets
- **Build validation** and optimization scripts

For detailed build configuration, see [GRADLE_SETUP.md](../GRADLE_SETUP.md).

### Quick Build Commands

```bash
# Build entire project
./gradlew build

# Build specific variants
./gradlew :android:app:assembleDevDebug     # Debug build
./gradlew :android:app:assembleDevRelease   # Release build

# Run validation
./gradlew validateBuild

# Clean build
./gradlew clean build
```

### Running the System

#### PC Orchestrator
```bash
./gradlew :pc:run                          # GUI mode
./gradlew :pc:runDemo                      # Console demo
```

#### Android Application
```bash
./gradlew :android:app:installDevDebug     # Install debug APK
```

For hardware-specific setup, see [HARDWARE_INTEGRATION.md](HARDWARE_INTEGRATION.md).

---

## Testing Strategy

### Comprehensive Testing Framework

The BucikaGSR platform includes three comprehensive testing suites:

#### 1. Android Testing Framework
- **89 comprehensive UI tests** covering all major user flows
- **Manager pattern integration** validation 
- **Performance and accessibility** testing
- **Test Coverage**: MainActivity, GSR Activities, Thermal Camera UI, Recording functionality

#### 2. PC Orchestrator Testing (Python)
- **70 tests total** with **69 passing (98.6% success rate)**
- **Protocol validation**, WebSocket communication, performance monitoring
- **Integration tests** for end-to-end workflows
- **Error recovery** and fault tolerance validation

#### 3. Android Unit Testing (Java/Kotlin)
- **Component-level testing** for GSR, Thermal, BLE modules
- **Manager pattern testing** for state management
- **Data processing** validation

### Running Tests

#### Android Tests
```bash
# Run all Android unit tests
./gradlew test

# Run Android UI tests
./gradlew connectedAndroidTest

# Run specific test suite
./gradlew :android:app:testDevDebugUnitTest

# Run with coverage
./gradlew testDevDebugUnitTest jacocoTestReport
```

#### PC Orchestrator Tests (Python)
```bash
# Run all PC orchestrator tests
python -m pytest tests/ -v

# Run specific test categories
python -m pytest tests/test_protocol.py -v
python -m pytest tests/test_websocket_server.py -v
python -m pytest tests/test_integration.py -v

# Run with coverage
python -m pytest tests/ --cov=src
```

#### UI Testing Framework
```bash
# Run comprehensive UI test suite
./scripts/run-ui-tests.sh comprehensive

# Run specific UI test categories
./scripts/run-ui-tests.sh thermal
./scripts/run-ui-tests.sh gsr
./scripts/run-ui-tests.sh recording
```

### Test Configuration

#### Android Test Dependencies
```gradle
dependencies {
    // Unit testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.11.0'
    testImplementation 'org.robolectric:robolectric:4.10.3'
    
    // Android UI testing
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
}
```

#### Test Environment Setup
Create `test-config.properties` in project root:
```properties
# Test device configuration
test.shimmer.address=00:11:22:33:44:55
test.shimmer.name=TestShimmer3-GSR+
test.sampling.rate=128
test.data.directory=/data/data/com.topdon.tc001/files/test_data

# Performance thresholds
test.sync.accuracy.ms=10
test.frame.rate.min=25
test.coverage.threshold=80
```

### Component-Specific Testing

#### GSR Module Testing
- **GSRManager**: Connection, data processing, error handling
- **Shimmer Integration**: Device pairing, data streaming
- **Data Validation**: Signal quality, sampling rate accuracy

#### Thermal Camera Testing  
- **TC001 Integration**: USB connection, frame capture
- **Thermal Processing**: Temperature calculation, frame rate
- **OpenCV Integration**: Image processing accuracy

#### PC Orchestrator Testing
- **Session Management**: Multi-client coordination
- **Time Synchronization**: Precision validation (±10ms accuracy)
- **Data Storage**: File integrity, metadata accuracy
- **WebSocket Communication**: Message handling, broadcast functionality

#### UI Integration Testing
```kotlin
// Example: Manager pattern integration testing
@Test
fun testManagerIntegration() {
    // Verify ThermalCameraManager integration
    onView(withId(R.id.btn_connect_camera))
        .perform(click())
    
    // UI State Manager should update
    onView(withId(R.id.tv_camera_status))
        .check(matches(isDisplayed()))
    
    // Configuration Manager should be accessible
    onView(withId(R.id.btn_thermal_settings))
        .check(matches(isDisplayed()))
}
```

### Automated Testing & CI/CD

Tests run automatically on:
- Pull request creation
- Merge to main branch
- Nightly builds

#### GitHub Actions Integration
```yaml
- name: Run Android Tests
  run: ./gradlew test connectedAndroidTest

- name: Run PC Tests  
  run: python -m pytest tests/ -v --cov=src

- name: Upload Test Results
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: |
      app/build/reports/tests/
      coverage-reports/
```

### Performance Testing

#### Benchmark Validation
- **UI Response Time**: <2000ms for navigation
- **Data Processing**: 128Hz GSR processing with <10ms latency
- **Memory Usage**: No leaks during extended sessions
- **Network Performance**: WebSocket message handling under load

#### Load Testing
```bash
# Test multiple simulated clients
python scripts/load_test.py --clients 50 --duration 300

# Memory usage validation
python scripts/memory_test.py --sessions 100
```

### Test Quality Gates

#### Coverage Requirements
- **Android Unit Tests**: >85% line coverage
- **PC Orchestrator Tests**: >90% line coverage (currently 98.6%)
- **Integration Tests**: All critical workflows covered
- **UI Tests**: All user interaction paths validated

#### Test Execution Standards
- All tests must pass before merging
- No test warnings or deprecation notices  
- Performance tests must meet baseline thresholds
- Integration tests must pass with real device conditions
        
        // 2. Connect Android client
        val client = connectMockClient()
        
        // 3. Run complete session
        val session = orchestrator.createSession("test")
        session.addDevice(client)
        session.start()
        
        // 4. Verify data collection
        assertThat(session.getCollectedData()).isNotEmpty()
    }
}
```

#### Performance Testing
```kotlin
@Test
fun testHighFrequencyDataStreaming() {
    val testDuration = 60_000L // 1 minute
    val expectedSamples = 60 * 128 // 128 Hz
    
    // Run high-frequency data generation
    val actualSamples = runStreamingTest(testDuration)
    
    assertThat(actualSamples).isCloseTo(expectedSamples, within(5.0))
}
```

### Test Data Generation

```kotlin
object TestDataGenerator {
    
    fun generateGSRData(samples: Int): List<ProcessedGSRData> {
        return (1..samples).map { i ->
            ProcessedGSRData(
                timestamp = System.currentTimeMillis() + i * 8, // 128 Hz
                rawGSR = 2.0 + Random.nextDouble() * 0.5,
                filteredGSR = 2.0 + Random.nextDouble() * 0.3,
                rawTemperature = 32.0 + Random.nextDouble() * 2.0,
                filteredTemperature = 32.0 + Random.nextDouble() * 1.0,
                signalQuality = 85.0 + Random.nextDouble() * 15.0,
                sampleIndex = i.toLong()
            )
        }
    }
}
```

---

## Contributing Guidelines

### Code Style

#### Kotlin Style Guide
```kotlin
// Class naming: PascalCase
class SessionManager

// Function naming: camelCase  
fun createSession(name: String): Session

// Constants: SCREAMING_SNAKE_CASE
companion object {
    private const val DEFAULT_TIMEOUT = 5000L
}

// Properties: camelCase
private val webSocketServer: WebSocketServer
```

#### Code Formatting
```bash
# Apply formatting
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck
```

### Commit Message Format

```
type(scope): description

[optional body]

[optional footer]
```

**Types**: feat, fix, docs, style, refactor, test, chore

**Examples**:
```
feat(orchestrator): add real-time GSR data streaming

- Implement WebSocket-based data transmission
- Add batching for 128Hz GSR samples  
- Include quality metrics in data packets

Fixes #123
```

### Pull Request Process

1. **Branch Naming**: `feature/description`, `fix/issue-number`, `docs/topic`
2. **Testing**: All tests must pass
3. **Documentation**: Update relevant documentation
4. **Review**: At least one approval required
5. **Merge**: Squash and merge preferred

### Code Review Checklist

- [ ] Code follows style guidelines
- [ ] Tests added for new functionality
- [ ] Documentation updated
- [ ] No breaking changes (or properly documented)
- [ ] Performance impact considered
- [ ] Security implications reviewed

---

## Troubleshooting

### Common Issues

#### PC Orchestrator

**Issue**: WebSocket server won't start
```bash
# Check port availability
netstat -tulpn | grep :8080

# Kill competing processes
sudo kill -9 $(lsof -ti:8080)
```

**Issue**: mDNS discovery not working
```bash
# Check mDNS service
avahi-browse -rt _bucika-gsr._tcp

# Test manual connection
wscat -c ws://localhost:8080
```

#### Android Development

**Issue**: Build fails with dependency conflicts
```bash
# Clear Gradle caches
./gradlew --stop
rm -rf ~/.gradle/caches/
./gradlew build
```

**Issue**: GSR sensor connection problems
```kotlin
// Enable Bluetooth debugging
adb shell settings put global bluetooth_hci_log 1
adb bugreport
```

### Detailed Troubleshooting

#### Build and Configuration Issues

**Gradle Build Failures**
```bash
# Clean and rebuild
./gradlew clean build

# Check for dependency conflicts
./gradlew dependencies --configuration implementation

# Fix Gradle daemon issues
./gradlew --stop
rm -rf ~/.gradle/caches/
./gradlew build
```

**Android Studio Setup Issues**
- Ensure JDK 17 is configured in File → Project Structure
- Verify Android SDK path in SDK Manager
- Check Kotlin plugin version compatibility
- Clear Android Studio caches: File → Invalidate Caches and Restart

#### Hardware-Specific Issues

**GSR (Shimmer3) Issues**

*Bluetooth Connection Issues*
```kotlin
// Check Bluetooth adapter status
val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
if (bluetoothAdapter == null) {
    XLog.e("Bluetooth", "Device does not support Bluetooth")
    return false
}

if (!bluetoothAdapter.isEnabled) {
    XLog.w("Bluetooth", "Bluetooth is disabled")
    // Request user to enable Bluetooth
}
```

*Solutions*:
- Verify Bluetooth permissions in AndroidManifest.xml
- Check device pairing status in Android settings
- Ensure Shimmer device is powered on and in range
- Reset Bluetooth stack: Settings → Apps → Bluetooth → Storage → Clear Data

*Data Quality Issues*:
- **Low signal quality**: Check electrode placement and skin contact
- **Missing samples**: Verify sampling rate configuration (128Hz)
- **High noise**: Check for electromagnetic interference
- **Signal saturation**: Adjust GSR range settings

**Thermal Camera (TC001) Issues**

*USB Connection Issues*
```xml
<!-- Ensure USB permissions in AndroidManifest.xml -->
<uses-permission android:name="android.permission.USB_PERMISSION" />
<uses-feature android:name="android.hardware.usb.host" />
```

*Solutions*:
- Grant USB permission when prompted
- Check USB OTG cable connectivity
- Verify TC001 firmware version compatibility
- Try different USB ports if available

*Performance Issues*:
- **Low frame rate**: Check USB 3.0 connection
- **Processing lag**: Optimize OpenCV operations
- **Memory issues**: Monitor thermal frame buffer usage
- **Overheating**: Allow thermal camera cooldown periods

#### PC Orchestrator Issues

**Network and Discovery Issues**

*mDNS Service Not Found*
```bash
# Check mDNS service broadcasting
avahi-browse -a | grep bucika-gsr

# Manual service registration
./gradlew :pc:run --args="--mdns-debug"

# Test network connectivity
ping [android-device-ip]
```

*WebSocket Connection Issues*:
- Check firewall settings on port 8080
- Verify network connectivity between devices  
- Monitor connection logs for timeout issues
- Test manual connection: `wscat -c ws://[pc-ip]:8080`

**Session Management Issues**

*Device Registration Failures*:
- Verify device ID uniqueness
- Check session capacity limits
- Monitor device heartbeat signals
- Validate JSON message format

*Data Synchronization Issues*:
- Validate NTP/time sync accuracy
- Check timestamp alignment across devices
- Monitor network latency and jitter
- Verify time zone consistency

#### Performance Troubleshooting

**Memory Issues**
```kotlin
// Monitor memory usage
val runtime = Runtime.getRuntime()
val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
XLog.d("Memory", "Used memory: ${usedMemory}MB")
```

*Solutions*:
- Implement proper object lifecycle management
- Use memory profiling tools
- Clear caches periodically
- Monitor for memory leaks in long-running sessions

**Network Performance**
```bash
# Monitor network statistics
netstat -i
iftop -i wlan0

# Test bandwidth
iperf3 -s (on PC)
iperf3 -c [pc-ip] (on Android)
```

*Solutions*:
- Use 5GHz WiFi when possible
- Reduce concurrent network usage
- Implement adaptive bitrate streaming
- Monitor packet loss and retransmissions

### Debug Configuration

#### Logging Configuration
```kotlin
// logback.xml for detailed logging
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.bucika" level="DEBUG"/>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

#### Remote Debugging
```bash
# PC orchestrator remote debugging
./gradlew :pc:run -Dorg.gradle.debug=true --debug-jvm

# Android remote debugging
adb forward tcp:5005 tcp:5005
```

---

## Advanced Topics

### Performance Optimization

#### Memory Management
```kotlin
// Use object pools for frequent allocations
class GSRDataPool {
    private val pool = ConcurrentLinkedQueue<ProcessedGSRData>()
    
    fun acquire(): ProcessedGSRData = pool.poll() ?: ProcessedGSRData()
    fun release(data: ProcessedGSRData) = pool.offer(data.reset())
}
```

#### Network Optimization
```kotlin
// Batch GSR samples for efficiency
class GSRBatcher(private val batchSize: Int = 16) {
    private val buffer = mutableListOf<GSRSample>()
    
    fun addSample(sample: GSRSample) {
        buffer.add(sample)
        if (buffer.size >= batchSize) {
            flushBuffer()
        }
    }
}
```

### Custom Extensions

#### Adding New Message Types
```kotlin
// Define new message type
@Serializable
data class CustomMessage(
    val customField: String,
    val customData: Map<String, Any>
) : MessagePayload

// Register with message handler
messageHandler.registerType("CUSTOM_MESSAGE", CustomMessage::class)
```

#### Custom Data Processors
```kotlin
interface DataProcessor<T> {
    suspend fun process(input: T): ProcessedData
}

class CustomGSRProcessor : DataProcessor<RawGSRData> {
    override suspend fun process(input: RawGSRData): ProcessedGSRData {
        // Custom processing logic
    }
}
```

### Security Considerations

#### Authentication
```kotlin
// Add authentication to WebSocket connections
class AuthenticatedWebSocketHandler : WebSocketHandler {
    override fun authenticate(headers: Map<String, String>): Boolean {
        val token = headers["Authorization"]
        return validateToken(token)
    }
}
```

#### Data Encryption
```kotlin
// Encrypt sensitive data before storage
class EncryptedDataWriter(private val encryptionKey: SecretKey) {
    fun writeEncryptedData(data: ByteArray): File {
        val encryptedData = encrypt(data, encryptionKey)
        return writeToFile(encryptedData)
    }
}
```

---

## Resources

### Documentation Links
- [API Reference](./PC_ORCHESTRATOR_API.md)
- [GSR Integration Guide](./GSR_DEVELOPMENT_SETUP.md)
- [Performance Optimization](./PERFORMANCE_OPTIMIZATION_GUIDE.md)

### External Resources
- [Shimmer3 GSR+ Documentation](https://www.shimmersensing.com/)
- [WebSocket Protocol RFC 6455](https://tools.ietf.org/html/rfc6455)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

### Community
- **Issues**: [GitHub Issues](https://github.com/buccancs/new-bucika-gsr/issues)
- **Discussions**: [GitHub Discussions](https://github.com/buccancs/new-bucika-gsr/discussions)
- **Releases**: [GitHub Releases](https://github.com/buccancs/new-bucika-gsr/releases)

---

*Last Updated: December 25, 2024*  
*Version: 2.0.0*