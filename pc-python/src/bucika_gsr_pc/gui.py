"""
Simplified GUI application for the Bucika GSR PC Orchestrator using PyQt6.
Provides visual interface with 3 focused tabs: Image Preview, Emotion Videos, and Device Monitoring.
"""

import sys
import threading
import asyncio
import os
import traceback
import json
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, List
from dataclasses import asdict
from loguru import logger

from PyQt6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
    QTabWidget, QTableWidget, QTableWidgetItem, QTextEdit, QLabel,
    QStatusBar, QSplitter, QListWidget, QPushButton, QFrame,
    QProgressBar, QComboBox, QFileDialog, QMessageBox, QHeaderView,
    QCheckBox, QScrollArea
)
from PyQt6.QtCore import QTimer, pyqtSignal, QThread, pyqtSlot, Qt, QSize
from PyQt6.QtGui import QPixmap, QFont, QAction

# Import only types, not the actual classes to avoid dependency issues during testing
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from .session_manager import SessionManager
    from .websocket_server import WebSocketServer
    from .discovery_service import DiscoveryService
else:
    # For runtime, try to import but handle failures gracefully
    try:
        from .session_manager import SessionManager
        from .websocket_server import WebSocketServer
        from .discovery_service import DiscoveryService
    except ImportError:
        # Create dummy classes for testing
        SessionManager = object
        WebSocketServer = object  
        DiscoveryService = object

# Try to import video playback dependencies
try:
    import cv2
    from PIL import Image
    VIDEO_SUPPORT = True
except ImportError:
    VIDEO_SUPPORT = False


class ImagePreviewWidget(QWidget):
    """Widget for displaying IR+RGB images from phones"""
    
    def __init__(self, device_id: str, device_name: str):
        super().__init__()
        self.device_id = device_id
        self.device_name = device_name
        self._init_ui()
        
    def _init_ui(self):
        """Initialize the image preview UI"""
        self.setFrameStyle(QFrame.Shape.StyledPanel)
        self.setMaximumHeight(300)
        
        layout = QVBoxLayout(self)
        
        # Device header
        header_label = QLabel(f"📱 {self.device_name} ({self.device_id})")
        header_label.setFont(QFont("Arial", 10, QFont.Weight.Bold))
        header_label.setStyleSheet("color: #2c3e50; padding: 5px;")
        layout.addWidget(header_label)
        
        # Image display area
        image_layout = QHBoxLayout()
        
        # IR Image
        ir_frame = QFrame()
        ir_frame.setFrameStyle(QFrame.Shape.StyledPanel)
        ir_layout = QVBoxLayout(ir_frame)
        ir_label = QLabel("🌡️ IR Camera")
        ir_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        ir_layout.addWidget(ir_label)
        
        self.ir_image_label = QLabel("No IR image")
        self.ir_image_label.setFixedSize(200, 150)
        self.ir_image_label.setStyleSheet("background-color: #34495e; color: white; border: 1px solid #7f8c8d;")
        self.ir_image_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        ir_layout.addWidget(self.ir_image_label)
        
        image_layout.addWidget(ir_frame)
        
        # RGB Image
        rgb_frame = QFrame()
        rgb_frame.setFrameStyle(QFrame.Shape.StyledPanel)
        rgb_layout = QVBoxLayout(rgb_frame)
        rgb_label = QLabel("📷 RGB Camera")
        rgb_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        rgb_layout.addWidget(rgb_label)
        
        self.rgb_image_label = QLabel("No RGB image")
        self.rgb_image_label.setFixedSize(200, 150)
        self.rgb_image_label.setStyleSheet("background-color: #34495e; color: white; border: 1px solid #7f8c8d;")
        self.rgb_image_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        rgb_layout.addWidget(self.rgb_image_label)
        
        image_layout.addWidget(rgb_frame)
        
        layout.addLayout(image_layout)
        
        # Status and timestamp
        self.status_label = QLabel("⏱️ Last updated: Never")
        self.status_label.setStyleSheet("color: #7f8c8d; font-style: italic;")
        layout.addWidget(self.status_label)
        
    def update_ir_image(self, image_data):
        """Update the IR image display"""
        try:
            # TODO: Convert image_data to QPixmap and display
            self.ir_image_label.setText("IR Image\n(Received)")
            self.status_label.setText(f"⏱️ Last updated: {datetime.now().strftime('%H:%M:%S')}")
        except Exception as e:
            logger.error(f"Failed to update IR image: {e}")
            
    def update_rgb_image(self, image_data):
        """Update the RGB image display"""
        try:
            # TODO: Convert image_data to QPixmap and display
            self.rgb_image_label.setText("RGB Image\n(Received)")
            self.status_label.setText(f"⏱️ Last updated: {datetime.now().strftime('%H:%M:%S')}")
        except Exception as e:
            logger.error(f"Failed to update RGB image: {e}")


class VideoWidget(QWidget):
    """Custom widget for video display"""
    
    def __init__(self):
        super().__init__()
        self.setStyleSheet("background-color: black;")
        self.setMinimumSize(640, 480)
        self.pixmap = None
        
    def set_frame(self, pixmap: QPixmap):
        """Set the video frame to display"""
        self.pixmap = pixmap
        self.update()
        
    def paintEvent(self, event):
        """Paint the video frame"""
        if self.pixmap:
            from PyQt6.QtGui import QPainter
            painter = QPainter(self)
            # Scale pixmap to fit widget while maintaining aspect ratio
            scaled_pixmap = self.pixmap.scaled(
                self.size(), Qt.AspectRatioMode.KeepAspectRatio, 
                Qt.TransformationMode.SmoothTransformation
            )
            
            # Center the pixmap
            x = (self.width() - scaled_pixmap.width()) // 2
            y = (self.height() - scaled_pixmap.height()) // 2
            painter.drawPixmap(x, y, scaled_pixmap)


class VideoPlayer(QWidget):
    """Simplified video player for emotion illicitation videos"""
    
    def __init__(self):
        super().__init__()
        self.video_cap: Optional[cv2.VideoCapture] = None
        self.is_playing = False
        self.current_frame = 0
        self.total_frames = 0
        self.fps = 30.0
        
        self.timer = QTimer()
        self.timer.timeout.connect(self._play_next_frame)
        
        self._init_ui()
        self.setFocusPolicy(Qt.FocusPolicy.StrongFocus)
        
    def _init_ui(self):
        """Initialize the video player UI"""
        layout = QVBoxLayout(self)
        
        # Video display
        self.video_widget = VideoWidget()
        layout.addWidget(self.video_widget)
        
        # Controls
        controls_layout = QHBoxLayout()
        
        self.play_button = QPushButton("▶️ Play")
        self.play_button.clicked.connect(self.toggle_play_pause)
        controls_layout.addWidget(self.play_button)
        
        self.stop_button = QPushButton("⏹️ Stop")
        self.stop_button.clicked.connect(self.stop)
        controls_layout.addWidget(self.stop_button)
        
        # Progress bar
        self.progress_bar = QProgressBar()
        self.progress_bar.setTextVisible(True)
        self.progress_bar.setFormat("Frame %v / %m")
        controls_layout.addWidget(self.progress_bar)
        
        # Speed control
        controls_layout.addWidget(QLabel("Speed:"))
        self.speed_combo = QComboBox()
        self.speed_combo.addItems(["0.5x", "1.0x", "1.5x", "2.0x"])
        self.speed_combo.setCurrentText("1.0x")
        self.speed_combo.currentTextChanged.connect(self._on_speed_changed)
        controls_layout.addWidget(self.speed_combo)
        
        layout.addLayout(controls_layout)
        
    def load_video(self, video_path: str):
        """Load a video file"""
        if not VIDEO_SUPPORT:
            return
            
        try:
            self.stop()  # Stop any current playback
            
            self.video_cap = cv2.VideoCapture(video_path)
            if not self.video_cap.isOpened():
                QMessageBox.warning(self, "Video Error", f"Could not open video file:\n{video_path}")
                return
                
            self.total_frames = int(self.video_cap.get(cv2.CAP_PROP_FRAME_COUNT))
            self.fps = self.video_cap.get(cv2.CAP_PROP_FPS)
            self.current_frame = 0
            
            self.progress_bar.setMaximum(self.total_frames - 1)
            self.progress_bar.setValue(0)
            
            # Display first frame
            self._show_current_frame()
            
            logger.info(f"Loaded video: {video_path} ({self.total_frames} frames, {self.fps:.1f} fps)")
            
        except Exception as e:
            logger.error(f"Error loading video: {e}")
            QMessageBox.critical(self, "Video Error", f"Error loading video:\n{str(e)}")
            
    def toggle_play_pause(self):
        """Toggle between play and pause"""
        if not self.video_cap:
            return
            
        if self.is_playing:
            self.pause()
        else:
            self.play()
            
    def play(self):
        """Start playback"""
        if not self.video_cap:
            return
            
        self.is_playing = True
        self.play_button.setText("⏸️ Pause")
        
        # Calculate timer interval based on fps and speed
        speed_multiplier = float(self.speed_combo.currentText().replace('x', ''))
        interval = int(1000 / (self.fps * speed_multiplier))
        self.timer.start(interval)
        
    def pause(self):
        """Pause playback"""
        self.is_playing = False
        self.play_button.setText("▶️ Play")
        self.timer.stop()
        
    def stop(self):
        """Stop playback and return to beginning"""
        self.pause()
        self.current_frame = 0
        self.progress_bar.setValue(0)
        if self.video_cap:
            self.video_cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
            self._show_current_frame()
            
    def _play_next_frame(self):
        """Play the next frame"""
        if not self.video_cap or not self.is_playing:
            return
            
        ret, frame = self.video_cap.read()
        if ret:
            self.current_frame += 1
            self.progress_bar.setValue(self.current_frame)
            self._display_frame(frame)
            
            if self.current_frame >= self.total_frames:
                self.stop()  # End of video
        else:
            self.stop()
            
    def _show_current_frame(self):
        """Show the current frame"""
        if not self.video_cap:
            return
            
        ret, frame = self.video_cap.read()
        if ret:
            self._display_frame(frame)
            
    def _display_frame(self, frame):
        """Display a frame in the video widget"""
        try:
            # Convert BGR to RGB
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            height, width, channel = rgb_frame.shape
            bytes_per_line = 3 * width
            
            # Create QPixmap
            from PyQt6.QtGui import QImage
            q_image = QImage(rgb_frame.data, width, height, bytes_per_line, QImage.Format.Format_RGB888)
            pixmap = QPixmap.fromImage(q_image)
            
            self.video_widget.set_frame(pixmap)
            
        except Exception as e:
            logger.error(f"Error displaying frame: {e}")
            
    def _on_speed_changed(self, speed_text):
        """Handle speed change"""
        if self.is_playing:
            self.pause()
            self.play()  # Restart with new speed
            
    def keyPressEvent(self, event):
        """Handle keyboard shortcuts"""
        key = event.key()
        
        if key == Qt.Key.Key_Space:
            self.toggle_play_pause()
        elif key == Qt.Key.Key_Left:
            # Previous frame (if paused)
            if not self.is_playing and self.video_cap:
                self.current_frame = max(0, self.current_frame - 1)
                self.video_cap.set(cv2.CAP_PROP_POS_FRAMES, self.current_frame)
                self._show_current_frame()
                self.progress_bar.setValue(self.current_frame)
        elif key == Qt.Key.Key_Right:
            # Next frame (if paused)
            if not self.is_playing and self.video_cap:
                self.current_frame = min(self.total_frames - 1, self.current_frame + 1)
                self.video_cap.set(cv2.CAP_PROP_POS_FRAMES, self.current_frame)
                self._show_current_frame()
                self.progress_bar.setValue(self.current_frame)
        elif key == Qt.Key.Key_Home:
            self.stop()
        else:
            super().keyPressEvent(event)


class SimplifiedMainWindow(QMainWindow):
    """Simplified main GUI window with 3 focused tabs"""
    
    def __init__(self, session_manager: SessionManager, 
                 websocket_server: WebSocketServer,
                 discovery_service: DiscoveryService):
        super().__init__()
        
        self.session_manager = session_manager
        self.websocket_server = websocket_server
        self.discovery_service = discovery_service
        
        self.running = False
        
        # Data storage
        self.device_image_widgets = {}
        self.emotion_video_file_paths: List[str] = []
        
        self._init_ui()
        self._init_timers()
        
    def _init_ui(self):
        """Initialize the user interface"""
        self.setWindowTitle("Bucika GSR PC Orchestrator - Research Platform")
        self.setGeometry(100, 100, 1400, 900)
        
        # Create menu bar
        self._create_menu_bar()
        
        # Central widget
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        
        # Main layout
        layout = QVBoxLayout(central_widget)
        
        # Status bar
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        
        # Status labels
        self.websocket_status = QLabel("WebSocket: Online")
        self.discovery_status = QLabel("mDNS: Broadcasting")
        self.general_status = QLabel("Status: Ready")
        
        self.status_bar.addWidget(self.general_status)
        self.status_bar.addPermanentWidget(self.websocket_status)
        self.status_bar.addPermanentWidget(self.discovery_status)
        
        # System status panel
        self._create_system_status_panel(layout)
        
        # Create tab widget
        self.tab_widget = QTabWidget()
        layout.addWidget(self.tab_widget)
        
        # Create the 3 main tabs
        self._create_image_preview_tab()
        self._create_emotion_video_tab()
        self._create_device_monitoring_tab()
        
        logger.info("Simplified PyQt6 GUI initialized successfully")
        
    def _create_menu_bar(self):
        """Create the menu bar"""
        menubar = self.menuBar()
        
        # File menu
        file_menu = menubar.addMenu('File')
        
        exit_action = QAction('Exit', self)
        exit_action.setShortcut('Ctrl+Q')
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)
        
        # View menu
        view_menu = menubar.addMenu('View')
        
        refresh_action = QAction('Refresh All', self)
        refresh_action.setShortcut('F5')
        refresh_action.triggered.connect(self._refresh_all_tabs)
        view_menu.addAction(refresh_action)
        
        # Help menu
        help_menu = menubar.addMenu('Help')
        
        about_action = QAction('About', self)
        about_action.triggered.connect(self._show_about)
        help_menu.addAction(about_action)
        
    def _create_system_status_panel(self, parent_layout):
        """Create the system status panel"""
        status_frame = QFrame()
        status_frame.setFrameStyle(QFrame.Shape.StyledPanel)
        status_frame.setMaximumHeight(60)
        
        status_layout = QHBoxLayout(status_frame)
        
        # Services status
        self.services_status = QLabel("🖥️ WebSocket: ✅ | mDNS: ✅ | TimeSync: ✅")
        self.services_status.setStyleSheet("color: green; font-weight: bold;")
        status_layout.addWidget(self.services_status)
        
        # Active sessions
        self.active_sessions_status = QLabel("📱 Active Sessions: 0 | Connected Devices: 0")
        self.active_sessions_status.setStyleSheet("color: blue; font-weight: bold;")
        status_layout.addWidget(self.active_sessions_status)
        
        parent_layout.addWidget(status_frame)
        
    def _create_image_preview_tab(self):
        """Create the image preview tab for IR+RGB images from phones"""
        image_widget = QWidget()
        self.tab_widget.addTab(image_widget, "📷 Image Preview")
        
        layout = QVBoxLayout(image_widget)
        
        # Title
        title_label = QLabel("IR + RGB Image Preview from Connected Phones")
        title_label.setFont(QFont("Arial", 14, QFont.Weight.Bold))
        title_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        title_label.setStyleSheet("color: #2c3e50; padding: 10px;")
        layout.addWidget(title_label)
        
        # Scrollable area for multiple devices
        scroll_area = QScrollArea()
        scroll_widget = QWidget()
        self.image_scroll_layout = QVBoxLayout(scroll_widget)
        
        # Default message when no devices
        self.no_devices_label = QLabel("🔍 No devices connected\n\nConnect Android devices to see IR+RGB image previews here")
        self.no_devices_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.no_devices_label.setStyleSheet("color: #7f8c8d; font-size: 16px; padding: 50px;")
        self.image_scroll_layout.addWidget(self.no_devices_label)
        
        scroll_area.setWidget(scroll_widget)
        scroll_area.setWidgetResizable(True)
        layout.addWidget(scroll_area)
        
        # Controls
        controls_layout = QHBoxLayout()
        
        refresh_images_btn = QPushButton("🔄 Refresh Images")
        refresh_images_btn.clicked.connect(self._refresh_device_images)
        controls_layout.addWidget(refresh_images_btn)
        
        save_images_btn = QPushButton("💾 Save Current Images")
        save_images_btn.clicked.connect(self._save_current_images)
        controls_layout.addWidget(save_images_btn)
        
        auto_refresh_checkbox = QCheckBox("Auto-refresh every 5s")
        auto_refresh_checkbox.setChecked(True)
        controls_layout.addWidget(auto_refresh_checkbox)
        
        controls_layout.addStretch()
        
        layout.addLayout(controls_layout)
        
    def _create_emotion_video_tab(self):
        """Create the video playback tab for emotion illicitation"""
        video_widget = QWidget()
        self.tab_widget.addTab(video_widget, "🎬 Emotion Videos")
        
        layout = QHBoxLayout(video_widget)
        
        # Left panel - Video list and controls
        left_panel = QWidget()
        left_layout = QVBoxLayout(left_panel)
        left_panel.setMaximumWidth(400)
        
        # Title
        title_label = QLabel("Emotion Illicitation Videos")
        title_label.setFont(QFont("Arial", 12, QFont.Weight.Bold))
        title_label.setStyleSheet("color: #2c3e50; padding: 5px;")
        left_layout.addWidget(title_label)
        
        # Video list
        self.emotion_video_list = QListWidget()
        self.emotion_video_list.itemSelectionChanged.connect(self._on_emotion_video_select)
        left_layout.addWidget(self.emotion_video_list)
        
        # Video info
        self.video_info_label = QLabel("Select a video to see details")
        self.video_info_label.setStyleSheet("color: #7f8c8d; font-style: italic; padding: 5px;")
        left_layout.addWidget(self.video_info_label)
        
        # Controls
        controls_layout = QVBoxLayout()
        
        # File management
        file_controls_layout = QHBoxLayout()
        browse_video_btn = QPushButton("📁 Browse Videos...")
        browse_video_btn.clicked.connect(self._browse_emotion_video)
        file_controls_layout.addWidget(browse_video_btn)
        
        refresh_video_btn = QPushButton("🔄 Refresh")
        refresh_video_btn.clicked.connect(self._refresh_emotion_video_list)
        file_controls_layout.addWidget(refresh_video_btn)
        
        controls_layout.addLayout(file_controls_layout)
        
        # Emotion category filter
        category_layout = QHBoxLayout()
        category_layout.addWidget(QLabel("Category:"))
        self.emotion_category_combo = QComboBox()
        self.emotion_category_combo.addItems(["All", "Happy", "Sad", "Fear", "Anger", "Neutral", "Surprise"])
        self.emotion_category_combo.currentTextChanged.connect(self._filter_emotion_videos)
        category_layout.addWidget(self.emotion_category_combo)
        controls_layout.addLayout(category_layout)
        
        left_layout.addLayout(controls_layout)
        
        layout.addWidget(left_panel)
        
        # Right panel - Video player
        if VIDEO_SUPPORT:
            self.emotion_video_player = VideoPlayer()
            layout.addWidget(self.emotion_video_player, 1)
        else:
            no_video_label = QLabel("🎬 Video playback not available\n\nInstall opencv-python and pillow for video support")
            no_video_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
            no_video_label.setStyleSheet("color: gray; font-size: 16px; padding: 50px;")
            layout.addWidget(no_video_label, 1)
            
        # Initialize emotion video list
        self._refresh_emotion_video_list()
        
    def _create_device_monitoring_tab(self):
        """Create the device monitoring and connection tab"""
        monitoring_widget = QWidget()
        self.tab_widget.addTab(monitoring_widget, "📱 Device Monitor")
        
        layout = QVBoxLayout(monitoring_widget)
        
        # Title and status
        header_layout = QHBoxLayout()
        title_label = QLabel("Connected Devices & Active Sessions")
        title_label.setFont(QFont("Arial", 14, QFont.Weight.Bold))
        title_label.setStyleSheet("color: #2c3e50; padding: 5px;")
        header_layout.addWidget(title_label)
        
        # Connection status
        self.connection_status_label = QLabel("🟢 Ready for connections")
        self.connection_status_label.setStyleSheet("color: green; font-weight: bold;")
        header_layout.addWidget(self.connection_status_label)
        
        layout.addLayout(header_layout)
        
        # Devices section
        devices_frame = QFrame()
        devices_frame.setFrameStyle(QFrame.Shape.StyledPanel)
        devices_layout = QVBoxLayout(devices_frame)
        
        devices_label = QLabel("📱 Connected Devices")
        devices_label.setFont(QFont("Arial", 11, QFont.Weight.Bold))
        devices_label.setStyleSheet("color: #34495e; padding: 5px;")
        devices_layout.addWidget(devices_label)
        
        # Devices table
        self.devices_table = QTableWidget()
        self.devices_table.setColumnCount(6)
        self.devices_table.setHorizontalHeaderLabels([
            "Device ID", "Name", "Type", "Version", "Battery", "Status"
        ])
        
        # Make columns resize to content
        header = self.devices_table.horizontalHeader()
        header.setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        
        devices_layout.addWidget(self.devices_table)
        layout.addWidget(devices_frame)
        
        # Sessions section
        sessions_frame = QFrame()
        sessions_frame.setFrameStyle(QFrame.Shape.StyledPanel)
        sessions_layout = QVBoxLayout(sessions_frame)
        
        sessions_label = QLabel("🎯 Active Recording Sessions")
        sessions_label.setFont(QFont("Arial", 11, QFont.Weight.Bold))
        sessions_label.setStyleSheet("color: #34495e; padding: 5px;")
        sessions_layout.addWidget(sessions_label)
        
        # Sessions table
        self.sessions_table = QTableWidget()
        self.sessions_table.setColumnCount(7)
        self.sessions_table.setHorizontalHeaderLabels([
            "Session ID", "Device", "State", "Started", "Duration", "Samples", "Actions"
        ])
        
        # Make columns resize to content
        header = self.sessions_table.horizontalHeader()
        header.setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        
        sessions_layout.addWidget(self.sessions_table)
        layout.addWidget(sessions_frame)
        
        # Control buttons
        controls_layout = QHBoxLayout()
        
        refresh_btn = QPushButton("🔄 Refresh All")
        refresh_btn.clicked.connect(self._refresh_monitoring_data)
        controls_layout.addWidget(refresh_btn)
        
        start_session_btn = QPushButton("▶️ Start New Session")
        start_session_btn.clicked.connect(self._start_new_session)
        start_session_btn.setStyleSheet("background-color: #d4edda; color: #155724;")
        controls_layout.addWidget(start_session_btn)
        
        stop_all_btn = QPushButton("⏹️ Stop All Sessions")
        stop_all_btn.clicked.connect(self._stop_all_sessions)
        stop_all_btn.setStyleSheet("background-color: #f8d7da; color: #721c24;")
        controls_layout.addWidget(stop_all_btn)
        
        controls_layout.addStretch()
        
        layout.addLayout(controls_layout)
        
    def _init_timers(self):
        """Initialize update timers"""
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_display)
        self.update_timer.start(3000)  # Update every 3 seconds
        
    # Event handlers and helper methods
    
    def _update_display(self):
        """Update the display data"""
        try:
            self._update_system_status()
            self._update_devices_table()
            self._update_sessions_table()
            self._update_device_images()
        except Exception as e:
            logger.error(f"Error updating display: {e}")
    
    def _update_system_status(self):
        """Update the system status panel"""
        try:
            # Count connected devices and active sessions
            connected_devices = len(self.websocket_server.connected_clients)
            active_sessions = sum(1 for s in self.session_manager.sessions.values() 
                                if s.state in ['NEW', 'RECORDING'])
            
            # Update services status
            ws_status = "✅" if connected_devices >= 0 else "❌"
            mdns_status = "✅" if self.discovery_service.is_running() else "❌"
            time_status = "✅"
            
            self.services_status.setText(f"🖥️ WebSocket: {ws_status} | mDNS: {mdns_status} | TimeSync: {time_status}")
            
            # Update session status
            self.active_sessions_status.setText(f"📱 Active Sessions: {active_sessions} | Connected Devices: {connected_devices}")
            
            if connected_devices > 0:
                self.connection_status_label.setText(f"🟢 {connected_devices} device{'s' if connected_devices != 1 else ''} connected")
                self.connection_status_label.setStyleSheet("color: green; font-weight: bold;")
            else:
                self.connection_status_label.setText("🟡 Waiting for device connections...")
                self.connection_status_label.setStyleSheet("color: orange; font-weight: bold;")
                
        except Exception as e:
            logger.error(f"Error updating system status: {e}")
    
    def _update_devices_table(self):
        """Update the devices table"""
        try:
            connected_clients = self.websocket_server.connected_clients
            
            self.devices_table.setRowCount(len(connected_clients))
            
            for row, (device_id, client_info) in enumerate(connected_clients.items()):
                self.devices_table.setItem(row, 0, QTableWidgetItem(device_id))
                self.devices_table.setItem(row, 1, QTableWidgetItem(client_info.get('device_name', 'Unknown')))
                self.devices_table.setItem(row, 2, QTableWidgetItem('Android GSR'))
                self.devices_table.setItem(row, 3, QTableWidgetItem(client_info.get('version', '1.0')))
                self.devices_table.setItem(row, 4, QTableWidgetItem(f"{client_info.get('battery', 0)}%"))
                self.devices_table.setItem(row, 5, QTableWidgetItem('🟢 Connected'))
                
        except Exception as e:
            logger.error(f"Error updating devices table: {e}")
    
    def _update_sessions_table(self):
        """Update the sessions table"""
        try:
            sessions = list(self.session_manager.sessions.values())
            
            self.sessions_table.setRowCount(len(sessions))
            
            for row, session in enumerate(sessions):
                self.sessions_table.setItem(row, 0, QTableWidgetItem(session.session_id))
                self.sessions_table.setItem(row, 1, QTableWidgetItem(session.device_id))
                
                # State with color coding
                state_item = QTableWidgetItem(session.state)
                if session.state == 'RECORDING':
                    state_item.setBackground(Qt.GlobalColor.green)
                elif session.state == 'FAILED':
                    state_item.setBackground(Qt.GlobalColor.red)
                elif session.state == 'DONE':
                    state_item.setBackground(Qt.GlobalColor.blue)
                    
                self.sessions_table.setItem(row, 2, state_item)
                
                self.sessions_table.setItem(row, 3, QTableWidgetItem(session.started_at.strftime('%H:%M:%S')))
                
                # Calculate duration
                if session.state in ['RECORDING', 'DONE']:
                    duration = datetime.now() - session.started_at
                    duration_str = str(duration).split('.')[0]  # Remove microseconds
                else:
                    duration_str = "00:00:00"
                self.sessions_table.setItem(row, 4, QTableWidgetItem(duration_str))
                
                # Sample count
                sample_count = len(session.gsr_data) if hasattr(session, 'gsr_data') else 0
                self.sessions_table.setItem(row, 5, QTableWidgetItem(str(sample_count)))
                
                # Actions
                if session.state == 'RECORDING':
                    actions_text = "🛑 Stop Available"
                else:
                    actions_text = "📊 View Data"
                self.sessions_table.setItem(row, 6, QTableWidgetItem(actions_text))
                
        except Exception as e:
            logger.error(f"Error updating sessions table: {e}")
    
    def _update_device_images(self):
        """Update device image previews"""
        try:
            connected_clients = self.websocket_server.connected_clients
            
            # Hide "no devices" message if we have devices
            if connected_clients and self.no_devices_label.isVisible():
                self.no_devices_label.hide()
                
            # Create image widgets for new devices
            for device_id, client_info in connected_clients.items():
                if device_id not in self.device_image_widgets:
                    device_name = client_info.get('device_name', f'Device {device_id}')
                    image_widget = ImagePreviewWidget(device_id, device_name)
                    self.device_image_widgets[device_id] = image_widget
                    self.image_scroll_layout.addWidget(image_widget)
                    
            # Remove widgets for disconnected devices
            for device_id in list(self.device_image_widgets.keys()):
                if device_id not in connected_clients:
                    widget = self.device_image_widgets.pop(device_id)
                    widget.hide()
                    widget.deleteLater()
                    
            # Show "no devices" message if no devices connected
            if not connected_clients and not self.no_devices_label.isVisible():
                self.no_devices_label.show()
                
        except Exception as e:
            logger.error(f"Error updating device images: {e}")
    
    # Tab-specific helper methods
    
    def _refresh_device_images(self):
        """Refresh images from all connected devices"""
        self.general_status.setText("Requesting image refresh from connected devices...")
        # TODO: Send image refresh requests to connected devices
        
    def _save_current_images(self):
        """Save current images to disk"""
        if not self.device_image_widgets:
            QMessageBox.information(self, "Save Images", "No images to save")
            return
            
        try:
            save_dir = QFileDialog.getExistingDirectory(self, "Select Save Directory", "")
            if save_dir:
                timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
                # TODO: Implement actual image saving
                QMessageBox.information(self, "Save Images", f"Images would be saved to {save_dir}")
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save images:\n{str(e)}")
            
    def _refresh_emotion_video_list(self):
        """Refresh the list of emotion videos"""
        self.emotion_video_list.clear()
        self.emotion_video_file_paths = []
        
        # Look for videos in an "emotion_videos" directory
        emotion_videos_dir = Path("emotion_videos")
        if not emotion_videos_dir.exists():
            emotion_videos_dir.mkdir(parents=True, exist_ok=True)
            
        video_extensions = {'.mp4', '.avi', '.mov', '.mkv', '.webm', '.flv', '.wmv'}
        video_files = []
        
        if emotion_videos_dir.exists():
            for file_path in emotion_videos_dir.iterdir():
                if file_path.is_file() and file_path.suffix.lower() in video_extensions:
                    video_files.append((file_path.name, str(file_path)))
                    
        # Sort and add to list
        video_files.sort(key=lambda x: x[0])
        
        for display_name, full_path in video_files:
            self.emotion_video_list.addItem(display_name)
            self.emotion_video_file_paths.append(full_path)
            
        if video_files:
            self.video_info_label.setText(f"Found {len(video_files)} emotion videos")
        else:
            self.video_info_label.setText("No emotion videos found. Use 'Browse Videos...' to add videos.")
            
    def _browse_emotion_video(self):
        """Browse for emotion video files to load"""
        if not VIDEO_SUPPORT:
            QMessageBox.warning(self, "Video Support", 
                              "Video playback requires opencv-python and pillow to be installed")
            return
            
        file_dialog = QFileDialog()
        file_paths, _ = file_dialog.getOpenFileNames(
            self, 
            "Select Emotion Video Files",
            "",
            "Video files (*.mp4 *.avi *.mov *.mkv *.webm *.flv *.wmv);;All files (*.*)"
        )
        
        if file_paths:
            # Copy selected videos to emotion_videos directory
            emotion_videos_dir = Path("emotion_videos")
            emotion_videos_dir.mkdir(parents=True, exist_ok=True)
            
            copied_count = 0
            for file_path in file_paths:
                source_path = Path(file_path)
                dest_path = emotion_videos_dir / source_path.name
                
                try:
                    import shutil
                    shutil.copy2(source_path, dest_path)
                    copied_count += 1
                except Exception as e:
                    QMessageBox.warning(self, "Copy Error", 
                                      f"Failed to copy {source_path.name}:\n{str(e)}")
                    
            if copied_count > 0:
                QMessageBox.information(self, "Videos Added", 
                                      f"Successfully added {copied_count} video{'s' if copied_count != 1 else ''}")
                self._refresh_emotion_video_list()
                
    def _on_emotion_video_select(self):
        """Handle emotion video selection"""
        current_item = self.emotion_video_list.currentItem()
        if current_item and VIDEO_SUPPORT and hasattr(self, 'emotion_video_player'):
            selected_index = self.emotion_video_list.currentRow()
            if 0 <= selected_index < len(self.emotion_video_file_paths):
                video_path = self.emotion_video_file_paths[selected_index]
                self.emotion_video_player.load_video(video_path)
                
                # Update info label
                video_file = Path(video_path)
                file_size = video_file.stat().st_size / (1024 * 1024)  # MB
                self.video_info_label.setText(f"📹 {video_file.name}\n📁 Size: {file_size:.1f} MB")
                
    def _filter_emotion_videos(self, category):
        """Filter videos by emotion category"""
        # TODO: Implement filtering based on filename patterns or metadata
        self._refresh_emotion_video_list()
        
    def _refresh_monitoring_data(self):
        """Refresh device and session monitoring data"""
        self._update_devices_table()
        self._update_sessions_table()
        self.general_status.setText("Monitoring data refreshed")
        
    def _start_new_session(self):
        """Start a new recording session"""
        try:
            # Get connected devices
            connected_devices = list(self.websocket_server.connected_clients.keys())
            if not connected_devices:
                QMessageBox.warning(self, "No Devices", "No devices are currently connected.\n\nConnect an Android device first.")
                return
                
            # For now, start session with first connected device
            device_id = connected_devices[0]
            session_id = self.session_manager.create_session(device_id, "Manual Session")
            
            QMessageBox.information(self, "Session Started", 
                                  f"✅ Started new recording session: {session_id}\n\nDevice: {device_id}")
            self._refresh_monitoring_data()
            
        except Exception as e:
            QMessageBox.critical(self, "Session Error", 
                               f"Failed to start session:\n{str(e)}")
            
    def _stop_all_sessions(self):
        """Stop all active recording sessions"""
        try:
            active_sessions = [s for s in self.session_manager.sessions.values() 
                             if s.state in ['NEW', 'RECORDING']]
            
            if not active_sessions:
                QMessageBox.information(self, "No Active Sessions", 
                                      "No active sessions to stop")
                return
                
            reply = QMessageBox.question(self, "Stop Sessions", 
                                       f"🛑 Stop {len(active_sessions)} active session{'s' if len(active_sessions) != 1 else ''}?\n\nThis action cannot be undone.",
                                       QMessageBox.StandardButton.Yes | 
                                       QMessageBox.StandardButton.No)
                                       
            if reply == QMessageBox.StandardButton.Yes:
                for session in active_sessions:
                    self.session_manager.stop_session(session.session_id)
                    
                QMessageBox.information(self, "Sessions Stopped", 
                                      f"✅ Stopped {len(active_sessions)} session{'s' if len(active_sessions) != 1 else ''}")
                self._refresh_monitoring_data()
                
        except Exception as e:
            QMessageBox.critical(self, "Stop Error", 
                               f"Failed to stop sessions:\n{str(e)}")
    
    # General helper methods
    
    def _refresh_all_tabs(self):
        """Refresh all tabs"""
        self._refresh_device_images()
        self._refresh_emotion_video_list()
        self._refresh_monitoring_data()
        self.general_status.setText("All tabs refreshed")
        
    def _show_about(self):
        """Show about dialog"""
        QMessageBox.about(self, "About Bucika GSR PC Orchestrator", 
                         "🔬 Bucika GSR PC Orchestrator\n"
                         "Research-grade physiological data collection platform\n\n"
                         "Features:\n"
                         "• Real-time IR+RGB image preview from Android devices\n"
                         "• Emotion illicitation video playback\n"
                         "• Multi-device monitoring and session management\n"
                         "• WebSocket-based communication protocol\n\n"
                         "Built with PyQt6 and Python\n"
                         "© 2024 Bucika Research Team")
                         
    def start(self):
        """Start the GUI"""
        self.running = True
        self.show()
        logger.info("Simplified GUI started successfully")
        
    def stop(self):
        """Stop the GUI"""
        self.running = False
        
    def closeEvent(self, event):
        """Handle window close event"""
        logger.info("GUI shutdown requested")
        self.running = False
        event.accept()


class MainWindowManager:
    """Manager for the main GUI window"""
    
    def __init__(self, session_manager: SessionManager, 
                 websocket_server: WebSocketServer,
                 discovery_service: DiscoveryService):
        self.session_manager = session_manager
        self.websocket_server = websocket_server
        self.discovery_service = discovery_service
        self.window: Optional[SimplifiedMainWindow] = None
        self.app: Optional[QApplication] = None
        
    def start_gui(self):
        """Start the GUI application"""
        try:
            logger.info("Starting simplified PyQt6 GUI...")
            
            # Run in separate thread to avoid blocking
            gui_thread = threading.Thread(target=self._run_gui, daemon=True)
            gui_thread.start()
            
            return gui_thread
            
        except Exception as e:
            logger.error(f"Failed to start GUI: {e}")
            return None
    
    def _run_gui(self):
        """Run the GUI main loop"""
        try:
            # Create QApplication
            self.app = QApplication(sys.argv)
            
            # Create main window
            self.window = SimplifiedMainWindow(
                self.session_manager,
                self.websocket_server, 
                self.discovery_service
            )
            
            # Start the window
            self.window.start()
            
            # Run the application
            sys.exit(self.app.exec())
            
        except Exception as e:
            logger.error(f"GUI error: {e}")


# For backward compatibility, alias MainWindow to MainWindowManager
MainWindow = MainWindowManager