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

For detailed implementation examples, see the [Developer Guide](DEVELOPER_GUIDE.md) and [Integration Guide](HARDWARE_INTEGRATION.md).