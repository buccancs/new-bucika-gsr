# 🎬 Simplified PyQt6 GUI Features - Bucika GSR PC Orchestrator

## 🚀 Streamlined Research Interface

The Python PC orchestrator features a **simplified 3-tab PyQt6 GUI** designed exclusively for core research requirements. This streamlined interface eliminates distractions and focuses on essential physiological data collection tasks.

## 🎯 3-Tab Research Design

### 📷 **Tab 1: Image Preview**
Real-time monitoring of IR+RGB camera feeds from connected Android devices.

#### Key Features:
- **Individual Device Widgets**: Dedicated preview areas for each connected phone
- **Side-by-Side Display**: IR thermal and RGB camera feeds displayed simultaneously
- **Real-time Updates**: Live image refresh with configurable intervals (1-30 seconds)
- **Timestamp Tracking**: Last received image time displayed for each device
- **Save Functionality**: Capture and save current images for documentation
- **Auto-refresh Toggle**: Automatic image updating with customizable intervals

#### Technical Implementation:
```python
class ImagePreviewWidget(QWidget):
    def __init__(self, device_id: str):
        self.device_id = device_id
        self.ir_label = QLabel()      # IR camera display
        self.rgb_label = QLabel()     # RGB camera display
        self.timestamp_label = QLabel()
        
    def update_images(self, ir_image, rgb_image):
        # Display side-by-side IR+RGB images
        self.ir_label.setPixmap(QPixmap.fromImage(ir_image))
        self.rgb_label.setPixmap(QPixmap.fromImage(rgb_image))
```

### 🎬 **Tab 2: Emotion Videos**
Professional video player optimized for emotion illicitation studies.

#### Video Player Features:
- **Multi-Format Support**: MP4, AVI, MOV, MKV, WebM, FLV, WMV compatibility
- **Professional Controls**: Play, Pause, Stop with visual indicators
- **Frame Navigation**: Precise frame-by-frame control for research accuracy
- **Variable Speed**: 0.5x to 2.0x playback for timing requirements
- **Progress Tracking**: Visual progress bar with time display
- **Category Filtering**: Organize videos by emotion type or study category

#### Keyboard Shortcuts (Research Optimized):
- **Space**: Play/Pause toggle (hands-free operation)
- **← →**: Frame-by-frame navigation for precise control
- **↑ ↓**: Volume control
- **Home**: Return to video beginning
- **+/-**: Adjust playback speed

#### Research Benefits:
```python
class VideoPlayer(QWidget):
    def __init__(self):
        self.media_player = QMediaPlayer()
        self.video_widget = QVideoWidget()
        self.position_slider = QSlider(Qt.Horizontal)
        
    def load_emotion_video(self, video_path: str):
        # Load video for emotion illicitation study
        self.media_player.setSource(QUrl.fromLocalFile(video_path))
        
    def set_playback_speed(self, speed: float):
        # Precise speed control for timing requirements
        self.media_player.setPlaybackRate(speed)
```

### 📱 **Tab 3: Device Monitor**
Combined device connection monitoring and session management interface.

#### Device Monitoring:
- **Real-time Connection Status**: Live monitoring of Android device connections
- **Battery Level Tracking**: Monitor device power status during studies
- **Device Information**: ID, name, version, capabilities display
- **Connection Timeline**: Track when devices connect/disconnect

#### Session Management:
- **Session State Tracking**: Visual indicators for session progress
- **Duration Monitoring**: Live session timing and elapsed time display
- **Sample Count Display**: Real-time GSR data sample counting
- **Session Controls**: Start new sessions, stop recordings
- **Color-coded Status**: Visual indicators for session states

#### Interface Components:
```python
class DeviceMonitorTab(QWidget):
    def __init__(self):
        # Device connection table
        self.device_table = QTableWidget()
        self.device_table.setColumnCount(6)  # ID, Name, Version, Battery, Status, Connected
        
        # Session monitoring table  
        self.session_table = QTableWidget()
        self.session_table.setColumnCount(6)  # Session, Device, State, Duration, Samples, Actions
        
        # Control buttons
        self.start_session_btn = QPushButton("▶️ Start New Session")
        self.stop_all_btn = QPushButton("⏹️ Stop All Sessions")
        self.refresh_btn = QPushButton("🔄 Refresh")
```

## 🛠️ Technical Architecture

### Simplified Class Structure:
```python
class SimplifiedMainWindow(QMainWindow):
    """Main GUI class - streamlined for research use"""
    
    def __init__(self):
        self.init_3_tab_interface()
        
    def _create_image_preview_tab(self):
        """Tab 1: IR+RGB image preview from phones"""
        
    def _create_emotion_video_tab(self):
        """Tab 2: Video playback for emotion illicitation"""
        
    def _create_device_monitoring_tab(self):
        """Tab 3: Device and session management"""
```

### Key Improvements:
- **Reduced Complexity**: From 8+ tabs to 3 focused research tabs
- **Streamlined Codebase**: ~1,200 lines vs previous ~2,700+ lines
- **Research-Focused**: Eliminates unnecessary analytics and ML interfaces
- **Professional Appearance**: Clean, modern interface suitable for research environments
- **Keyboard Optimized**: Shortcuts designed for hands-free operation during studies

## 📊 Status Bar & Menu System

### Status Bar Information:
- **Service Status**: WebSocket (✅), mDNS (✅), TimeSync (✅)  
- **Device Counter**: Active devices and total connections
- **Session Statistics**: Active sessions and recording status

### Menu System:
- **File Menu**: Import videos, export data, application settings
- **View Menu**: Refresh intervals, display options, fullscreen toggle
- **Help Menu**: User documentation, keyboard shortcuts, about

## 🎯 Research Workflow Benefits

The simplified interface supports typical research workflows:

1. **Setup Phase**: Monitor device connections in Device Monitor tab
2. **Stimulus Presentation**: Use Emotion Videos tab for standardized stimuli  
3. **Data Collection**: Monitor real-time IR+RGB feeds in Image Preview tab
4. **Session Management**: Control recording sessions from Device Monitor tab

This design reduces cognitive load and potential operator errors during time-sensitive research data collection, while maintaining all essential functionality for multi-device GSR studies.