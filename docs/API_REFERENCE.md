# BucikaGSR Complete API Reference

## Overview

This document provides comprehensive API reference for all components of the BucikaGSR system:
- GSR sensor integration (ShimmerAndroidAPI)
- TC001 thermal imaging integration  
- PC Orchestrator WebSocket protocol

## Table of Contents

1. [GSR API Reference](#gsr-api-reference)
2. [TC001 Thermal Imaging API](#tc001-thermal-imaging-api)
3. [PC Orchestrator API](#pc-orchestrator-api)

---

# GSR API Reference

Complete API reference for the BucikaGSR ShimmerAndroidAPI integration for GSR data collection and analysis.

## Core Classes

### GSRManager

**Package**: `com.topdon.tc001.gsr`

The central coordinator for GSR data collection and device management.

#### Constructor
```kotlin
private constructor(context: Context)
```
*Private constructor - use `getInstance()` to obtain instance*

#### Static Methods

**getInstance(context: Context): GSRManager**
```kotlin
companion object {
    @JvmStatic
    fun getInstance(context: Context): GSRManager
}
```
Returns singleton instance of GSRManager.

#### Core Methods

**startScan(): Boolean**
```kotlin
fun startScan(): Boolean
```
Initiates Bluetooth scanning for available GSR devices.
- **Returns**: `true` if scan started successfully, `false` otherwise
- **Requires**: Bluetooth and location permissions

**stopScan()**
```kotlin
fun stopScan()
```
Stops active Bluetooth device scanning.

**connectToDevice(deviceAddress: String): Boolean**
```kotlin
fun connectToDevice(deviceAddress: String): Boolean
```
Attempts to connect to GSR device at specified address.
- **Parameters**: `deviceAddress` - MAC address of target device
- **Returns**: `true` if connection initiated, `false` if failed

---

# TC001 Thermal Imaging API

Complete API reference for the BucikaGSR Topdon TC001 thermal imaging integration.

## Core Classes

### IRUVCTC

**Package**: `com.infisense.usbir.camera`

The central coordinator for TC001 thermal imaging data collection and USB device management.

#### Constructor
```java
public IRUVCTC(Context context, IFrameCallback frameCallback)
```
Initializes TC001 thermal camera with context and frame callback.

#### Core Methods

**openCamera(): boolean**
```java
public boolean openCamera()
```
Opens connection to TC001 thermal camera.
- **Returns**: `true` if camera opened successfully, `false` otherwise
- **Requires**: USB host capabilities

**closeCamera()**
```java
public void closeCamera()
```
Closes camera connection and releases resources.

**startPreview()**
```java
public void startPreview()
```
Starts thermal imaging preview stream.

**captureFrame(): void**
```java
public void captureFrame()
```
Captures single thermal frame to callback.

#### Frame Callback Interface

**IFrameCallback**
```java
public interface IFrameCallback {
    void onFrame(byte[] thermalData, int width, int height);
    void onError(int errorCode, String message);
}
```

---

# PC Orchestrator API

The PC Orchestrator provides WebSocket-based communication protocol for coordinating multiple Android GSR clients in synchronized data collection sessions.

## Architecture Overview

```
PC Orchestrator (Python/Kotlin)
    ↕ WebSocket Protocol
Multiple Android Clients
    ↕ Bluetooth/USB
GSR Sensors + Thermal Cameras
```

## Communication Protocol

### Connection Endpoint
```
ws://[orchestrator-ip]:8765/gsr
```

### Message Format
```json
{
    "type": "message_type",
    "clientId": "unique_client_identifier",
    "timestamp": 1642684800000,
    "sessionId": "session_identifier",
    "data": { /* message-specific payload */ }
}
```

## Message Types

### Session Management

#### StartSession
```json
{
    "type": "start_session",
    "clientId": "client_001",
    "timestamp": 1642684800000,
    "data": {
        "sessionName": "Study_Session_001",
        "duration": 300,
        "samplingRate": 100
    }
}
```

#### StopSession
```json
{
    "type": "stop_session", 
    "clientId": "client_001",
    "timestamp": 1642684800000,
    "sessionId": "session_001"
}
```

### Data Streaming

#### GSRData
```json
{
    "type": "gsr_data",
    "clientId": "client_001", 
    "timestamp": 1642684800000,
    "sessionId": "session_001",
    "data": {
        "gsrValue": 15.7,
        "conductance": 0.063,
        "resistance": 15873.02
    }
}
```

#### ThermalData  
```json
{
    "type": "thermal_data",
    "clientId": "client_001",
    "timestamp": 1642684800000,
    "sessionId": "session_001", 
    "data": {
        "temperature": 36.8,
        "thermalMatrix": [[...]], 
        "frameWidth": 256,
        "frameHeight": 192
    }
}
```

### Discovery and Synchronization

#### ClientDiscovery
```json
{
    "type": "client_discovery",
    "clientId": "client_001",
    "timestamp": 1642684800000,
    "data": {
        "deviceType": "android",
        "capabilities": ["gsr", "thermal"],
        "version": "1.0.0"
    }
}
```

#### TimeSync
```json
{
    "type": "time_sync",
    "timestamp": 1642684800000,
    "data": {
        "serverTime": 1642684800000,
        "roundTripTime": 45
    }
}
```

## Integration Examples

### Android Client Connection
```kotlin
// Initialize WebSocket connection
val orchestratorClient = OrchestratorClient("ws://192.168.1.100:8765/gsr")

// Register message handlers
orchestratorClient.onSessionStart { sessionData ->
    // Start GSR and thermal data collection
    gsrManager.startSession(sessionData.sessionId)
    thermalManager.startCapture(sessionData.sessionId)
}

orchestratorClient.onTimeSync { syncData ->
    // Synchronize local timestamps
    TimeSyncManager.sync(syncData.serverTime, syncData.roundTripTime)
}

// Send discovery message
orchestratorClient.sendDiscovery("android_client_001")
```

### Python Orchestrator Setup
```python
import asyncio
import websockets
from bucika_orchestrator import OrchestratorServer

# Initialize orchestrator
orchestrator = OrchestratorServer(port=8765)

# Register session handlers
@orchestrator.on_client_connect
async def handle_client_connect(websocket, client_id):
    print(f"Client {client_id} connected")
    
@orchestrator.on_gsr_data  
async def handle_gsr_data(client_id, gsr_data):
    # Process and store GSR data
    await orchestrator.store_data(client_id, gsr_data)

# Start server
asyncio.run(orchestrator.start())
```

## Error Handling

### Error Message Format
```json
{
    "type": "error",
    "clientId": "client_001",
    "timestamp": 1642684800000,
    "data": {
        "errorCode": "GSR_CONNECTION_FAILED",
        "message": "Failed to connect to GSR device",
        "details": "Device not found at address XX:XX:XX:XX:XX:XX"
    }
}
```

### Common Error Codes
- `GSR_CONNECTION_FAILED` - GSR device connection error
- `THERMAL_INIT_FAILED` - Thermal camera initialization error  
- `SESSION_NOT_FOUND` - Invalid session ID referenced
- `WEBSOCKET_CONNECTION_LOST` - Network connectivity issue
- `TIME_SYNC_FAILED` - Clock synchronization error

---

# Python PC Orchestrator Implementation

## Overview

The **Bucika GSR PC Orchestrator Python Implementation** provides a complete research-grade Python-based PC orchestrator for coordinating GSR data collection from Android devices. This implementation features a streamlined 3-tab interface designed for physiological research studies.

## Core Architecture

### WebSocket Server (Port 8080)
```python
# High-performance async/await JSON-over-WebSocket server
class BucikaWebSocketServer:
    def __init__(self, host="0.0.0.0", port=8080):
        self.host = host
        self.port = port
        
    async def handle_connection(self, websocket, path):
        # Handle client connections and message routing
        await self.register_client(websocket)
        
    async def broadcast_message(self, message, exclude_clients=None):
        # Broadcast to all connected clients
        pass
```

### Service Discovery (mDNS)
```python
# Automatic device discovery with _bucika-gsr._tcp broadcasting
class ServiceDiscovery:
    def __init__(self):
        self.service_type = "_bucika-gsr._tcp.local."
        
    def register_service(self, port=8080):
        # Register mDNS service for automatic discovery
        pass
```

### Time Synchronization (UDP Port 9123)
```python  
# Sub-millisecond precision UDP time synchronization
class TimeSyncService:
    def handle_sync_request(self, client_addr):
        # High-precision time synchronization
        server_timestamp = time.time_ns()
        return {
            'server_timestamp': server_timestamp,
            'precision': 'nanosecond'
        }
```

## GUI Interface (3-Tab Design)

### 1. Image Preview Tab
- **Real-time IR+RGB camera display** from connected Android devices
- **Individual preview widgets** with side-by-side thermal and RGB images
- **Auto-refresh** with customizable intervals
- **Save functionality** for research documentation
- **Timestamped updates** showing last received data

### 2. Emotion Videos Tab  
- **Professional video player** for emotion elicitation studies
- **Support for major formats**: MP4, AVI, MOV, MKV, WebM, FLV, WMV
- **Advanced playback controls** with frame-by-frame navigation
- **Variable playback speed** (0.5x to 2.0x)
- **Keyboard shortcuts** for seamless operation
- **Category filtering** for organized stimulus management

### 3. Device Monitor Tab
- **Combined device connection and session management**
- **Real-time monitoring** with battery levels and connection status
- **Session state tracking** with color-coded indicators
- **Duration and sample count** display for active recordings
- **Integrated controls** for session management

## Message Protocol Details

### Enhanced Message Format
```json
{
  "id": "unique-message-id",
  "type": "MESSAGE_TYPE",
  "ts": 1234567890123456789,
  "deviceId": "device-identifier", 
  "sessionId": "session-identifier",
  "payload": {
    // Type-specific payload with enhanced metadata
  }
}
```

### Python-Specific Message Types

#### Session Control
```python
# Start session with enhanced parameters
{
  "type": "START_SESSION",
  "payload": {
    "sessionName": "Research_Study_001",
    "participantId": "P001",
    "condition": "baseline",
    "duration": 300,
    "samplingRate": 128,
    "dataQuality": "research_grade"
  }
}
```

#### Real-time Data Streaming  
```python
# Enhanced GSR data with quality metrics
{
  "type": "GSR_DATA",
  "payload": {
    "samples": [
      {
        "timestamp": 1234567890123456789,
        "gsrValue": 15.73,
        "conductance": 0.0634,
        "resistance": 15769.23,
        "quality": "good",
        "artifacts": false
      }
    ],
    "batchSize": 16,
    "samplingRate": 128
  }
}
```

## Research-Grade Features

### Data Analysis Engine
```python
class DataAnalyzer:
    def analyze_session(self, session_data):
        """
        Comprehensive session analysis including:
        - Statistical metrics calculation
        - Artifact detection algorithms  
        - Quality score generation
        - Visualization preparation
        """
        return {
            'statistics': self.calculate_statistics(session_data),
            'artifacts': self.detect_artifacts(session_data),
            'quality_score': self.calculate_quality(session_data),
            'visualizations': self.generate_plots(session_data)
        }
```

### Multi-Level Data Validation
```python
class DataValidator:
    def validate_data(self, data, level="research_grade"):
        """
        Multi-level validation:
        - Basic: Format and range validation
        - Standard: Completeness and consistency  
        - Strict: Advanced quality metrics
        - Research-Grade: Publication-ready validation
        """
        validators = {
            'basic': self.basic_validation,
            'standard': self.standard_validation, 
            'strict': self.strict_validation,
            'research_grade': self.research_grade_validation
        }
        return validators[level](data)
```

### Performance Monitoring
```python
class PerformanceMonitor:
    def get_system_metrics(self):
        """Real-time system performance tracking"""
        return {
            'cpu_usage': psutil.cpu_percent(),
            'memory_usage': psutil.virtual_memory()._asdict(),
            'network_io': psutil.net_io_counters()._asdict(),
            'disk_io': psutil.disk_io_counters()._asdict()
        }
        
    def get_application_metrics(self):
        """Application-specific performance metrics"""
        return {
            'active_sessions': len(self.session_manager.active_sessions),
            'message_throughput': self.websocket_server.message_rate,
            'data_rate': self.calculate_data_throughput(),
            'connected_devices': len(self.connected_devices)
        }
```

## Installation and Setup

### Requirements
```bash
# Core dependencies
pip install websockets asyncio
pip install zeroconf  # mDNS service discovery
pip install numpy pandas  # Data analysis
pip install matplotlib seaborn  # Visualization
pip install tkinter  # GUI framework
pip install opencv-python  # Image processing
pip install psutil  # Performance monitoring
```

### Quick Start
```python
# Initialize and start orchestrator
from bucika_orchestrator import BucikaOrchestrator

orchestrator = BucikaOrchestrator()
orchestrator.start_services()
orchestrator.show_gui()
```

## Integration with Android Clients

### Connection Establishment
```python
# Android clients auto-discover via mDNS
# Manual connection also supported:
ws://[PC_IP]:8080/bucika-gsr
```

### Session Coordination
```python
# Synchronized session management across multiple devices
await orchestrator.start_coordinated_session({
    'session_id': 'research_001',
    'devices': ['android_001', 'android_002'], 
    'synchronization': 'millisecond_precision',
    'data_collection': {
        'gsr': True,
        'thermal': True,
        'rgb': True
    }
})
```

---

*This comprehensive API reference covers all aspects of the BucikaGSR system integration, from low-level sensor APIs to high-level orchestration protocols.*

For detailed implementation examples, see the [Developer Guide](DEVELOPER_GUIDE.md) and [Integration Guide](HARDWARE_INTEGRATION.md).