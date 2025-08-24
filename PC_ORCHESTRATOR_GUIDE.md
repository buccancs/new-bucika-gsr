# PC Orchestrator Application Guide

The Bucika GSR PC Orchestrator is now fully implemented and operational! This guide shows how to run and use both versions of the PC application.

## 🎯 Application Overview

The PC orchestrator provides centralized control for multiple Android GSR clients with:

- **Real-time WebSocket communication** on port 8080
- **mDNS service discovery** for automatic Android client detection
- **Nanosecond-precision time synchronization** via UDP on port 9123
- **Complete session management** with metadata tracking
- **Live GSR data streaming** (128Hz) with automatic CSV storage
- **Chunked file upload** system with MD5 integrity verification
- **Professional GUI** and headless console modes

## 🚀 Running the PC Orchestrator

### Option 1: JavaFX GUI Application (Recommended)

The full-featured GUI application with dashboard, device monitoring, and session controls:

```bash
cd /home/runner/work/new-bucika-gsr/new-bucika-gsr
./gradlew :pc:run
```

**Features:**
- Visual dashboard showing connected devices
- Real-time session status monitoring  
- Interactive session controls (start/stop/sync)
- File upload progress tracking
- Device discovery status
- Time synchronization statistics

### Option 2: Console Demo (Headless)

Perfect for testing, CI/CD, or server deployments without GUI requirements:

```bash
cd /home/runner/work/new-bucika-gsr/new-bucika-gsr
./gradlew :pc:runDemo
```

**Console Output Example:**
```
23:40:17 INFO Starting Bucika GSR Console Demo v1.0.0
23:40:17 INFO Time sync service started on port 9123
23:40:17 INFO Discovery service started on 10.1.0.108
23:40:17 INFO WebSocket server started on port 8080
23:40:17 INFO Demo monitoring complete. Services will continue running...

=== Bucika GSR Console Demo Running ===
Services are running and ready to accept connections.
Connect Android clients to ws://localhost:8080
Time sync available on UDP port 9123
Press Ctrl+C to stop...
```

## 🏗️ Architecture Implementation

### Core Services

1. **WebSocket Server** (`com.topdon.bucika.pc.websocket.WebSocketServer`)
   - JSON message protocol with envelope structure
   - Automatic client registration and device discovery
   - Real-time GSR data streaming support
   - File upload handling with progress tracking

2. **Discovery Service** (`com.topdon.bucika.pc.discovery.DiscoveryService`)  
   - mDNS broadcasting as `_bucika-gsr._tcp` service
   - Automatic network interface detection
   - Service announcement with capabilities metadata

3. **Time Synchronization** (`com.topdon.bucika.pc.time.TimeSyncService`)
   - SNTP-like UDP protocol on port 9123
   - Nanosecond-precision reference clock
   - Client offset statistics and monitoring

4. **Session Manager** (`com.topdon.bucika.pc.session.SessionManager`)
   - Complete lifecycle management (NEW → ARMED → RECORDING → FINALISING → DONE/FAILED)
   - Automatic session directory creation
   - Metadata tracking with JSON persistence
   - GSR data CSV storage with integrity verification

### Protocol Implementation

**Message Envelope:**
```json
{
  "id": "uuid",
  "type": "MESSAGE_TYPE", 
  "ts": 1234567890123456789,
  "sessionId": "session_uuid",
  "deviceId": "device_uuid",
  "payload": { /* message-specific data */ }
}
```

**Supported Message Types:**
- `HELLO` / `REGISTER` - Client connection and registration
- `PING` / `PONG` - Liveness detection (3 missed pings = offline)
- `START_SESSION` / `STOP_SESSION` - Session lifecycle control
- `SYNC_MARK` - Precision time synchronization marks  
- `GSR_SAMPLE` - Real-time physiological data streaming
- `UPLOAD_BEGIN` / `UPLOAD_CHUNK` / `UPLOAD_END` - File transfer
- `ACK` / `ERROR` - Response and error handling

### Data Pipeline Features

**Real-Time GSR Streaming:**
- 128Hz sample rate with sequence tracking
- Quality flags (spike detection, saturation, dropout)
- Automatic CSV storage with nanosecond timestamps
- Live data visualization support

**File Upload System:**
- Chunked transfer (8KB default chunks)
- MD5 checksum verification
- Progress tracking and notifications
- Automatic retry on network failures
- Session-organized file storage

## 🔧 Build and Development

### Building the Application

```bash
# Build the complete PC orchestrator
./gradlew :pc:build

# Compile Kotlin source only
./gradlew :pc:compileKotlin

# Run tests
./gradlew :pc:test
```

### Project Structure

```
pc/
├── src/main/kotlin/com/topdon/bucika/pc/
│   ├── BucikaOrchestrator.kt          # Main JavaFX application
│   ├── demo/ConsoleDemoOrchestrator.kt # Console demo app
│   ├── websocket/WebSocketServer.kt    # WebSocket communication
│   ├── discovery/DiscoveryService.kt   # mDNS service discovery
│   ├── time/TimeSyncService.kt         # Time synchronization
│   ├── session/SessionManager.kt       # Session lifecycle
│   ├── protocol/                       # Message protocols
│   └── ui/MainController.kt            # JavaFX GUI controller
├── src/main/resources/
│   └── fxml/main.fxml                  # JavaFX UI layout  
└── build.gradle.kts                   # Build configuration
```

## 🌟 Key Achievements

✅ **Complete PC orchestrator implementation** with both GUI and console modes  
✅ **Professional protocol implementation** with comprehensive message types  
✅ **Robust networking** with WebSocket, mDNS, and UDP time sync  
✅ **Research-grade data collection** with 128Hz GSR streaming and CSV storage  
✅ **Reliable file transfer** with chunked upload and integrity verification  
✅ **Build system integration** with Gradle tasks and proper dependency management  
✅ **Comprehensive testing** with unit tests for core functionality  

## 📡 Android Client Integration

The PC orchestrator is designed to work seamlessly with Android GSR clients:

1. **Auto-discovery**: Android devices automatically find the PC orchestrator via mDNS
2. **Manual connection**: Support for direct WebSocket URL connection 
3. **Protocol compatibility**: Full BucikaGSR protocol implementation
4. **Session coordination**: Synchronized start/stop across all connected devices
5. **Data collection**: Real-time streaming plus post-session file upload

## 🎯 Usage Scenarios

**Research Studies**: Centralized data collection from multiple participants with precise synchronization

**Clinical Applications**: Professional-grade physiological monitoring with automated data management  

**Stress Analysis**: Multi-modal data collection with coordinated GSR, video, and thermal recordings

**Development & Testing**: Console mode for CI/CD pipelines and automated testing

The PC orchestrator provides the foundation for sophisticated physiological data collection systems requiring precise timing, reliable communication, and comprehensive data management.