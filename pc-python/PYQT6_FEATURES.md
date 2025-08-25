# 🎬 PyQt6 GUI Features - Bucika GSR PC Orchestrator

## 🚀 Professional GUI Implementation

The Python PC orchestrator now features a comprehensive **PyQt6-based GUI** that provides enterprise-grade functionality for research-quality GSR data collection and analysis.

## 🎯 Core GUI Components

### 1. **Connected Devices Tab** 📱
- Real-time device monitoring with connection status
- Device information display (ID, Name, Version, Battery, Connection Time)
- Auto-updating table with connected Android clients
- Visual indicators for device health and status

### 2. **Sessions Tab** 📊
- Active session monitoring and management
- Session details (ID, Device, Name, State, Start Time, Sample Count)
- Real-time updates of session progress
- Session state tracking (NEW → RECORDING → DONE/FAILED)

### 3. **Logs Tab** 📝
- Real-time application logging with timestamps
- Scrollable log viewer with auto-scroll to latest entries
- Color-coded log levels and structured information
- Copy and clear log functionality via menu system

### 4. **🎬 Video Playback Tab** (Advanced Professional Features)
#### Video Player Controls:
- **Professional Media Controls**: Play, Pause, Stop, Frame-by-frame navigation
- **Advanced Seeking**: Clickable progress bar for instant seeking to any position
- **Speed Control**: 9 speed levels from 0.1x to 4.0x playback speed
- **Timestamp Display**: Current time / Total time with MM:SS format
- **Fullscreen Mode**: Toggle fullscreen viewing with F11 or button
- **Multi-format Support**: MP4, AVI, MOV, MKV, WebM, FLV, WMV

#### Keyboard Shortcuts:
- **Space**: Play/Pause toggle
- **Arrow Keys**: Frame-by-frame navigation (←/→)
- **Home**: Reset to beginning
- **F/F11**: Toggle fullscreen
- **+/-**: Adjust playback speed

#### Video File Management:
- **Automatic Discovery**: Scans session directories for uploaded videos
- **File Browser**: Browse and load external video files
- **Session Integration**: Direct access to videos uploaded during sessions

### 5. **📊 Data Analysis Tab** (Research-Grade Analysis)
#### Analysis Features:
- **Session Selection**: Dropdown with all available sessions (active + saved)
- **Statistical Analysis**: Comprehensive GSR data analysis with quality metrics
- **Data Validation**: Multi-level validation (Basic/Standard/Strict/Research-Grade)
- **Visual Reports**: Detailed analysis summary and comprehensive results
- **Export Functionality**: Export analysis reports to text files

#### Analysis Capabilities:
- **Statistical Metrics**: Mean, StdDev, Min/Max, Dynamic Range, Quality Score
- **Artifact Detection**: Automated identification and classification of data artifacts
- **Quality Assessment**: Completeness, Accuracy, Consistency scoring
- **Recommendations**: Automated suggestions for data quality improvement
- **Batch Processing**: Support for analyzing multiple sessions

### 6. **📈 Real-time Plot Tab** (Live Data Visualization)
#### Plotting Features:
- **Matplotlib Integration**: Professional scientific plotting with high-quality graphs
- **Real-time Updates**: Live GSR data visualization during active sessions
- **Interactive Controls**: Auto-scale, time window selection, data source indicators
- **Demo Mode**: Built-in data simulation for testing and demonstrations
- **Export Capabilities**: Save plots in PNG, PDF, SVG formats

#### Plot Customization:
- **Time Windows**: 10s, 30s, 60s, 120s, 300s, or All data
- **Statistical Overlays**: Real-time mean, standard deviation, min/max displays
- **Data Highlighting**: Recent data highlighted in different colors
- **Grid and Labels**: Professional scientific graph formatting

### 7. **❓ Help & Info Tab** (Comprehensive Documentation)
#### Help System:
- **🚀 Quick Start Guide**: Step-by-step setup and usage instructions
- **⌨️ Keyboard Shortcuts**: Complete reference for all shortcuts and hotkeys
- **🔧 Technical Specifications**: Detailed system requirements and specifications
- **ℹ️ About**: Version information, features list, dependencies, and licensing

## 🎛️ Advanced System Features

### System Status Panel
Real-time monitoring dashboard showing:
- **🖥️ Services**: WebSocket, mDNS, TimeSync status indicators
- **📊 Performance**: CPU usage, RAM consumption, Network throughput
- **📱 Sessions**: Active session count, connected device count
- **⚠️ Errors**: Error tracking and recovery rate monitoring

### Professional Menu System
Complete menu bar with organized functionality:

#### 📤 File Menu:
- **Export**: Analysis reports, plots, session data
- **Recent Sessions**: Quick access to recently analyzed sessions
- **Preferences**: Application settings and configuration
- **Quit**: Clean application shutdown

#### ✂️ Edit Menu:
- **Copy Logs**: Copy application logs to clipboard
- **Clear Logs**: Clear log display
- **Select All**: Standard text selection

#### 👁️ View Menu:
- **Refresh**: Update current tab data
- **Zoom**: In/Out/Reset zoom levels
- **Full Screen**: Toggle fullscreen mode

#### 📊 Data Menu:
- **Demo Data**: Start/stop data simulation
- **Analyze/Validate**: Quick access to analysis functions
- **Clear Plot**: Reset plotting display

#### 🛠️ Tools Menu:
- **Performance Monitor**: System performance analysis
- **Diagnostics**: System health checking
- **Network Status**: Network connectivity information

#### ❓ Help Menu:
- **Keyboard Shortcuts**: Quick reference (F1)
- **Documentation**: User documentation
- **About**: Application information

## 🔥 Enterprise-Grade Features

### Error Recovery Integration
- **Visual Error Indicators**: Real-time error status in system panel
- **Recovery Rate Monitoring**: Success rate tracking and display
- **Service Health Monitoring**: Individual service status indicators

### Performance Monitoring
- **Real-time Metrics**: Live CPU, memory, network usage display
- **Resource Tracking**: Performance impact monitoring
- **Optimization Recommendations**: Automated performance suggestions

### Cross-Platform Compatibility
- **Windows**: Full support with native look and feel
- **macOS**: Native macOS integration and styling
- **Linux**: Complete functionality on major distributions

### Research-Grade Reliability
- **Data Integrity**: Comprehensive validation and quality checking
- **Professional Export**: Multiple format support for research workflows
- **Scientific Visualization**: High-quality plots suitable for publications
- **Session Management**: Complete session lifecycle tracking

## 🚀 Installation & Usage

### Requirements
```bash
# Core GUI requirements
PyQt6>=6.6.0
opencv-python>=4.8.0
pillow>=10.1.0

# Visualization requirements
matplotlib>=3.7.0
numpy>=1.24.0

# System requirements
python>=3.8
```

### Quick Start
```bash
# GUI mode (recommended)
python main.py

# Headless mode (servers)
python main.py --headless

# Debug mode
python main.py --debug
```

### System Requirements
- **OS**: Windows 10+, macOS 10.14+, Linux (Ubuntu 18.04+)
- **Python**: 3.8 or later
- **Memory**: 4GB RAM minimum, 8GB recommended
- **Display**: GUI requires display support (headless mode available)

## 🎯 Key Advantages

### For Researchers:
- **Professional Interface**: Publication-quality visualization and analysis
- **Data Integrity**: Research-grade validation and quality assurance
- **Export Flexibility**: Multiple formats for analysis workflows
- **Comprehensive Documentation**: Complete user guides and technical specifications

### For Developers:
- **Modern Architecture**: PyQt6 with async/await integration
- **Extensible Design**: Modular components for easy enhancement
- **Professional Standards**: Enterprise-grade error handling and monitoring
- **Cross-Platform**: Single codebase for all major operating systems

### For Operators:
- **Real-time Monitoring**: Live system health and performance tracking
- **Intuitive Interface**: User-friendly design with comprehensive help system
- **Automated Operations**: Self-managing error recovery and optimization
- **Professional Tools**: Complete suite of analysis and management capabilities

This PyQt6 implementation represents a significant advancement over traditional desktop applications, providing a modern, professional, and highly capable platform for research-grade GSR data collection and analysis.