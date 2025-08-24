# BucikaGSR Protocol Specification v1

## Overview

The BucikaGSR protocol defines WebSocket-based communication between a PC orchestrator and multiple Android clients for coordinated GSR data collection sessions.

## Message Envelope

All messages use a standard JSON envelope:

```json
{
  "id": "unique-message-id",
  "type": "MESSAGE_TYPE", 
  "ts": 1234567890123456789,
  "sessionId": "session-uuid-or-null",
  "deviceId": "device-unique-identifier",
  "payload": { ... }
}
```

## Message Types

### Control Messages

#### HELLO
- **Direction**: Android → PC
- **Purpose**: Initial connection greeting
- **Payload**:
  ```json
  {
    "deviceName": "Samsung Galaxy S22",
    "capabilities": ["RGB", "THERMAL", "GSR_LEADER"],
    "batteryLevel": 85,
    "version": "1.0.0"
  }
  ```

#### REGISTER  
- **Direction**: PC → Android
- **Purpose**: Register device with orchestrator
- **Payload**:
  ```json
  {
    "registered": true,
    "role": "GSR_LEADER",
    "syncConfig": {
      "syncInterval": 30000,
      "offsetThreshold": 5000000
    }
  }
  ```

#### PING/PONG
- **Direction**: Bidirectional
- **Purpose**: Liveness detection (3 missed = offline)
- **Payload**: `{}`

### Session Control

#### START
- **Direction**: PC → Android  
- **Purpose**: Begin recording session
- **Payload**:
  ```json
  {
    "sessionConfig": {
      "videoConfig": {
        "resolution": "1080p",
        "fps": 30,
        "bitrate": 8000000
      },
      "gsrConfig": {
        "sampleRate": 128,
        "channels": ["GSR", "TEMP"]
      }
    }
  }
  ```

#### STOP
- **Direction**: PC → Android
- **Purpose**: End recording session
- **Payload**: `{}`

#### SYNC_MARK
- **Direction**: PC → Android
- **Purpose**: Insert synchronization marker
- **Payload**: 
  ```json
  {
    "markerId": "sync-001",
    "referenceTime": 1234567890123456789
  }
  ```

### Data Transfer

#### GSR_SAMPLE (Bridged Mode)
- **Direction**: Android → PC
- **Purpose**: Live GSR data streaming
- **Payload**:
  ```json
  {
    "samples": [
      {
        "t_mono_ns": 1234567890123456789,
        "t_utc_ns": 1234567890123456789,
        "offset_ms": 2.5,
        "seq": 12345,
        "gsr_raw_uS": 15.6,
        "gsr_filt_uS": 15.2,
        "temp_C": 32.1,
        "flag_spike": false,
        "flag_sat": false,
        "flag_dropout": false
      }
    ]
  }
  ```

#### UPLOAD_BEGIN
- **Direction**: Android → PC
- **Purpose**: Start file upload
- **Payload**:
  ```json
  {
    "fileName": "gsr_data_20240101_120000.csv",
    "fileSize": 1048576,
    "checksum": "sha256:abcd1234...",
    "chunkSize": 65536
  }
  ```

#### UPLOAD_CHUNK
- **Direction**: Android → PC
- **Purpose**: Upload file chunk
- **Payload**:
  ```json
  {
    "fileName": "gsr_data_20240101_120000.csv", 
    "chunkIndex": 0,
    "data": "base64-encoded-chunk-data",
    "checksum": "sha256:chunk-hash"
  }
  ```

#### UPLOAD_END
- **Direction**: Android → PC
- **Purpose**: Complete file upload
- **Payload**:
  ```json
  {
    "fileName": "gsr_data_20240101_120000.csv",
    "totalChunks": 16,
    "finalChecksum": "sha256:final-hash"
  }
  ```

### Response Messages

#### ACK
- **Direction**: Bidirectional
- **Purpose**: Acknowledge message receipt
- **Payload**:
  ```json
  {
    "ackId": "original-message-id",
    "status": "OK"
  }
  ```

#### ERROR
- **Direction**: Bidirectional  
- **Purpose**: Report errors
- **Payload**:
  ```json
  {
    "errorCode": "DEVICE_OFFLINE",
    "message": "Device connection lost",
    "details": { ... }
  }
  ```

## Discovery Protocol

Devices use mDNS/UDP for automatic discovery:

- **Service Type**: `_bucika-gsr._tcp`
- **Port**: 8080 (configurable)
- **TXT Records**: 
  - `version=1.0`
  - `capabilities=RGB,THERMAL,GSR`
  - `deviceId=unique-id`

## Time Synchronization

1. PC runs SNTP-like service on port 9123
2. Android clients sync periodically (pre-start + every 30s during recording)
3. Target: median offset ≤5ms, p95 ≤15ms
4. Offsets recorded in CSV/sidecar files for reconciliation

## Error Handling

- **Connection Loss**: Exponential backoff reconnection (max 5 attempts)
- **Message Loss**: ACK timeout triggers retry (3 attempts max)
- **Time Sync Failure**: Continue with last known offset + warning
- **Upload Interruption**: Resume from last successful chunk