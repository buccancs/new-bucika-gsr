# Bucika GSR PC Orchestrator - Python Implementation

A complete Python implementation of the PC orchestrator for coordinating GSR (Galvanic Skin Response) data collection from Android devices. This replaces the previous Kotlin/Java implementation while maintaining full protocol compatibility.

## Features

### Core Functionality
- **WebSocket Server**: Complete JSON-over-WebSocket implementation on port 8080
- **Device Discovery**: mDNS service broadcasting with `_bucika-gsr._tcp` service type
- **Time Synchronization**: SNTP-like UDP service on port 9123 with nanosecond precision
- **Session Management**: Complete lifecycle states (NEW → RECORDING → DONE/FAILED)
- **Data Storage**: Automatic GSR sample storage as timestamped CSV files
- **File Upload**: Chunked file upload with MD5 integrity verification

### Protocol Support
- **Message Types**: HELLO/REGISTER, PING/PONG, START/STOP, SYNC_MARK, GSR_SAMPLE, UPLOAD_*
- **Real-time GSR Streaming**: 128Hz data collection with quality flags
- **File Management**: Automatic upload of GSR, video, and thermal files
- **Error Recovery**: Comprehensive error handling and reconnection support

### User Interfaces
- **GUI Mode**: Tkinter-based graphical interface with device monitoring
- **Console Mode**: Headless operation perfect for servers and automation

## Installation

### Prerequisites
- Python 3.8 or higher
- pip package manager

### Install Dependencies
```bash
cd pc-python
pip install -r requirements.txt
```

### Optional GUI Dependencies
For the graphical interface:
```bash
pip install tkinter-tooltip pillow
```

## Usage

### GUI Mode (Default)
```bash
python main.py
```

### Console Mode (Headless)
```bash
python demo.py
```

### With Options
```bash
# Headless mode with debug logging
python main.py --headless --debug

# GUI mode with debug logging
python main.py --debug
```

## Architecture

### Core Components

#### WebSocket Server (`websocket_server.py`)
- Handles Android client connections on port 8080
- Implements complete BucikaGSR protocol
- Manages device registration, sessions, and data streaming
- Supports chunked file uploads with integrity verification

#### Session Manager (`session_manager.py`)  
- Manages recording session lifecycle
- Stores GSR data as timestamped CSV files
- Handles file organization and metadata tracking
- Provides session monitoring and status updates

#### Discovery Service (`discovery_service.py`)
- Broadcasts mDNS service for automatic device discovery
- Uses `_bucika-gsr._tcp` service type for compatibility
- Provides service information and capabilities

#### Time Sync Service (`time_sync_service.py`)
- UDP server on port 9123 for high-precision timing
- SNTP-like protocol for nanosecond accuracy
- Network latency compensation

#### GUI (`gui.py`)
- Tkinter-based graphical interface
- Real-time device and session monitoring  
- Live log display and status updates
- Tabbed interface for organization

### Protocol Compatibility

The Python implementation maintains 100% protocol compatibility with the existing Android client:

```json
{
  "id": "msg-123",
  "type": "GSR_SAMPLE", 
  "ts": 1234567890123456789,
  "sessionId": "session-456",
  "deviceId": "android-device-789",
  "payload": {
    "samples": [{
      "t_mono_ns": 1234567890123456789,
      "t_utc_ns": 1234567890123456789,
      "seq": 12845,
      "gsr_raw_uS": 2.347,
      "gsr_filt_uS": 2.351,
      "temp_C": 32.4,
      "flag_spike": false,
      "flag_sat": false,
      "flag_dropout": false
    }]
  }
}
```

## Data Storage

### GSR Data Files
- Stored as CSV files with microsecond-precision timestamps
- Filename format: `gsr_data_YYYYMMDD_HHMMSS.csv`
- Columns: Timestamp_ns, DateTime_UTC, Sequence, GSR_Raw_uS, GSR_Filtered_uS, Temperature_C, Quality_Flags

### Session Organization
```
sessions/
├── device1_20241225_143022/
│   ├── gsr_data_20241225_143022.csv
│   ├── video_recording.mp4
│   └── thermal_data.txt
└── device2_20241225_150315/
    └── gsr_data_20241225_150315.csv
```

## Development

### Project Structure
```
pc-python/
├── src/
│   └── bucika_gsr_pc/
│       ├── __init__.py           # Main orchestrator class
│       ├── protocol.py           # Protocol messages and types  
│       ├── websocket_server.py   # WebSocket communication
│       ├── session_manager.py    # Session and data management
│       ├── discovery_service.py  # mDNS service discovery
│       ├── time_sync_service.py  # Time synchronization
│       └── gui.py               # Tkinter GUI interface
├── main.py                      # GUI entry point
├── demo.py                      # Console entry point  
├── setup.py                     # Package setup
├── requirements.txt             # Dependencies
└── README.md                    # This file
```

### Running Tests
```bash
# Install dev dependencies
pip install -r requirements.txt
pip install pytest pytest-asyncio

# Run tests
pytest tests/
```

### Code Style
```bash
# Format code
black src/

# Check formatting
black --check src/
```

## Migration from Kotlin/Java

This Python implementation replaces the previous Kotlin/Java version while maintaining:
- ✅ Full protocol compatibility with Android clients
- ✅ Same WebSocket server functionality (port 8080)  
- ✅ Same mDNS discovery service (`_bucika-gsr._tcp`)
- ✅ Same time sync service (UDP port 9123)
- ✅ Same session management and data storage
- ✅ Same file upload and integrity verification
- ✅ Compatible CSV data format

### Key Improvements
- **Simplified Deployment**: Single Python script vs. complex Gradle build
- **Better Resource Usage**: More efficient memory and CPU utilization
- **Easier Maintenance**: Pure Python vs. mixed Kotlin/Java/JavaFX
- **Cross-Platform**: Works on Windows, macOS, Linux without JVM
- **Modern Async**: Built with Python asyncio for better performance

## Network Configuration

### Firewall Settings
Ensure these ports are open:
- **8080/TCP**: WebSocket server for Android connections
- **9123/UDP**: Time synchronization service
- **5353/UDP**: mDNS discovery (automatic)

### Network Discovery
The orchestrator automatically broadcasts its availability via mDNS. Android devices on the same network will discover it automatically.

## Troubleshooting

### Common Issues

**Connection Issues**
- Check firewall settings for ports 8080 and 9123
- Ensure Android device and PC are on same network
- Verify mDNS is working (`ping hostname.local`)

**Performance Issues** 
- Use `--debug` flag to identify bottlenecks
- Monitor system resources during recording
- Check disk space for data storage

**GUI Issues**
- Run in console mode if GUI fails: `python demo.py`
- Check tkinter installation: `python -c "import tkinter"`

### Debug Mode
```bash
python main.py --debug
```

This enables verbose logging to help diagnose issues.

## License

This project is licensed under the MIT License. See LICENSE file for details.

## Support

For support and questions:
- Open an issue on GitHub
- Check the troubleshooting section above
- Review logs with `--debug` flag enabled