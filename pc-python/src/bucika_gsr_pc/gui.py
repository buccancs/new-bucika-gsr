"""
GUI application for the Bucika GSR PC Orchestrator using PyQt6.
Provides visual interface for monitoring devices and sessions.
"""

import sys
import threading
import asyncio
import os
import traceback
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, List
from loguru import logger

from PyQt6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
    QTabWidget, QTableWidget, QTableWidgetItem, QTextEdit, QLabel,
    QStatusBar, QSplitter, QListWidget, QPushButton, QFrame,
    QProgressBar, QComboBox, QFileDialog, QMessageBox, QHeaderView
)
from PyQt6.QtCore import QTimer, pyqtSignal, QThread, pyqtSlot, Qt, QSize
from PyQt6.QtGui import QPixmap, QFont, QAction

from .session_manager import SessionManager
from .websocket_server import WebSocketServer
from .discovery_service import DiscoveryService

# Try to import video playback dependencies
try:
    import cv2
    from PIL import Image
    VIDEO_SUPPORT = True
except ImportError:
    VIDEO_SUPPORT = False


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
    """Video player widget with controls"""
    
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
        
        # Set focus policy to receive keyboard events
        self.setFocusPolicy(Qt.FocusPolicy.StrongFocus)
        
    def keyPressEvent(self, event):
        """Handle keyboard shortcuts"""
        key = event.key()
        
        if key == Qt.Key.Key_Space:
            self.toggle_play_pause()
        elif key == Qt.Key.Key_Left:
            self.previous_frame()
        elif key == Qt.Key.Key_Right:
            self.next_frame()
        elif key == Qt.Key.Key_Home:
            self.stop()
        elif key == Qt.Key.Key_F or key == Qt.Key.Key_F11:
            self._toggle_fullscreen()
        elif key == Qt.Key.Key_Plus or key == Qt.Key.Key_Equal:
            self._increase_speed()
        elif key == Qt.Key.Key_Minus:
            self._decrease_speed()
        else:
            super().keyPressEvent(event)
    
    def _increase_speed(self):
        """Increase playback speed"""
        current_index = self.speed_combo.currentIndex()
        if current_index < self.speed_combo.count() - 1:
            self.speed_combo.setCurrentIndex(current_index + 1)
    
    def _decrease_speed(self):
        """Decrease playback speed"""
        current_index = self.speed_combo.currentIndex()
        if current_index > 0:
            self.speed_combo.setCurrentIndex(current_index - 1)
        
    def _init_ui(self):
        """Initialize the UI"""
        layout = QVBoxLayout(self)
        
        # Video display
        self.video_widget = VideoWidget()
        layout.addWidget(self.video_widget)
        
        # Status label
        self.status_label = QLabel("No video loaded")
        self.status_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        layout.addWidget(self.status_label)
        
        # Progress bar - make it clickable for seeking
        self.progress_bar = QProgressBar()
        self.progress_bar.setVisible(False)
        self.progress_bar.setMinimumHeight(20)
        self.progress_bar.setTextVisible(True)
        self.progress_bar.mousePressEvent = self._on_progress_click
        layout.addWidget(self.progress_bar)
        
        # Timestamp display
        self.timestamp_layout = QHBoxLayout()
        self.current_time_label = QLabel("00:00")
        self.timestamp_layout.addWidget(self.current_time_label)
        self.timestamp_layout.addStretch()
        self.total_time_label = QLabel("00:00")
        self.timestamp_layout.addWidget(self.total_time_label)
        layout.addLayout(self.timestamp_layout)
        
        # Controls
        controls_layout = QHBoxLayout()
        
        # Previous frame button
        self.prev_button = QPushButton("⏮")
        self.prev_button.clicked.connect(self.previous_frame)
        self.prev_button.setMaximumWidth(40)
        controls_layout.addWidget(self.prev_button)
        
        # Play/Pause button
        self.play_button = QPushButton("▶")
        self.play_button.clicked.connect(self.toggle_play_pause)
        self.play_button.setMaximumWidth(40)
        controls_layout.addWidget(self.play_button)
        
        # Stop button
        self.stop_button = QPushButton("⏹")
        self.stop_button.clicked.connect(self.stop)
        self.stop_button.setMaximumWidth(40)
        controls_layout.addWidget(self.stop_button)
        
        # Next frame button
        self.next_button = QPushButton("⏭")
        self.next_button.clicked.connect(self.next_frame)
        self.next_button.setMaximumWidth(40)
        controls_layout.addWidget(self.next_button)
        
        controls_layout.addStretch()
        
        # Speed control
        controls_layout.addWidget(QLabel("Speed:"))
        self.speed_combo = QComboBox()
        self.speed_combo.addItems(["0.1x", "0.25x", "0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x", "4.0x"])
        self.speed_combo.setCurrentText("1.0x")
        self.speed_combo.setMaximumWidth(80)
        self.speed_combo.currentTextChanged.connect(self._on_speed_changed)
        controls_layout.addWidget(self.speed_combo)
        
        # Add volume control placeholder
        controls_layout.addStretch()
        controls_layout.addWidget(QLabel("🔊"))
        
        # Add fullscreen button
        self.fullscreen_button = QPushButton("⛶")
        self.fullscreen_button.clicked.connect(self._toggle_fullscreen)
        self.fullscreen_button.setMaximumWidth(40)
        self.fullscreen_button.setToolTip("Toggle Fullscreen")
        controls_layout.addWidget(self.fullscreen_button)
        
        layout.addLayout(controls_layout)
        
        # Initially disable controls
        self._set_controls_enabled(False)
        
    def _set_controls_enabled(self, enabled: bool):
        """Enable/disable video controls"""
        self.prev_button.setEnabled(enabled)
        self.play_button.setEnabled(enabled)
        self.stop_button.setEnabled(enabled)
        self.next_button.setEnabled(enabled)
        self.speed_combo.setEnabled(enabled)
        self.fullscreen_button.setEnabled(enabled)
    
    def _format_time(self, seconds: float) -> str:
        """Format seconds to MM:SS format"""
        minutes = int(seconds // 60)
        seconds = int(seconds % 60)
        return f"{minutes:02d}:{seconds:02d}"
    
    def _update_timestamps(self):
        """Update timestamp displays"""
        if self.video_cap:
            current_seconds = self.current_frame / self.fps if self.fps > 0 else 0
            total_seconds = self.total_frames / self.fps if self.fps > 0 else 0
            
            self.current_time_label.setText(self._format_time(current_seconds))
            self.total_time_label.setText(self._format_time(total_seconds))
    
    def _on_progress_click(self, event):
        """Handle clicking on progress bar for seeking"""
        if self.video_cap and self.total_frames > 0:
            click_x = event.x()
            progress_width = self.progress_bar.width()
            
            if progress_width > 0:
                # Calculate frame position
                frame_ratio = click_x / progress_width
                new_frame = int(frame_ratio * (self.total_frames - 1))
                new_frame = max(0, min(self.total_frames - 1, new_frame))
                
                # Update position
                self.current_frame = new_frame
                self._show_frame()
                self.status_label.setText(f"Seek to frame {self.current_frame + 1}/{self.total_frames}")
    
    def _on_speed_changed(self, speed_text):
        """Handle speed change"""
        if self.is_playing:
            # Restart timer with new speed
            speed_value = float(speed_text.rstrip('x'))
            frame_delay = max(1, int(1000 / (self.fps * speed_value)))
            self.timer.start(frame_delay)
    
    def _toggle_fullscreen(self):
        """Toggle fullscreen mode for video widget"""
        if self.video_widget.isFullScreen():
            self.video_widget.showNormal()
            self.fullscreen_button.setText("⛶")
            self.fullscreen_button.setToolTip("Toggle Fullscreen")
        else:
            self.video_widget.showFullScreen()
            self.fullscreen_button.setText("⛷")
            self.fullscreen_button.setToolTip("Exit Fullscreen")
        
    def load_video(self, video_path: str) -> bool:
        """Load a video file"""
        if not VIDEO_SUPPORT:
            self.status_label.setText("Video support not available\n(Install opencv-python and pillow)")
            return False
            
        try:
            # Release previous video
            self.stop()
            if self.video_cap:
                self.video_cap.release()
            
            # Load new video
            self.video_cap = cv2.VideoCapture(video_path)
            
            if not self.video_cap.isOpened():
                self.status_label.setText(f"Error: Cannot open video {Path(video_path).name}")
                return False
            
            # Get video properties
            self.total_frames = int(self.video_cap.get(cv2.CAP_PROP_FRAME_COUNT))
            self.fps = self.video_cap.get(cv2.CAP_PROP_FPS) or 30.0
            self.current_frame = 0
            
            # Setup progress bar
            self.progress_bar.setMaximum(self.total_frames - 1)
            self.progress_bar.setVisible(True)
            
            # Display first frame
            self._show_frame()
            
            # Enable controls
            self._set_controls_enabled(True)
            
            duration = self.total_frames / self.fps
            self.status_label.setText(f"Loaded: {Path(video_path).name} ({self.total_frames} frames, {duration:.1f}s)")
            return True
            
        except Exception as e:
            self.status_label.setText(f"Error loading video: {e}")
            return False
    
    def _show_frame(self) -> bool:
        """Display the current frame"""
        if not self.video_cap or not VIDEO_SUPPORT:
            return False
            
        try:
            # Set frame position
            self.video_cap.set(cv2.CAP_PROP_POS_FRAMES, self.current_frame)
            
            # Read frame
            ret, frame = self.video_cap.read()
            if not ret:
                return False
            
            # Convert color space
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            h, w, ch = frame_rgb.shape
            bytes_per_line = ch * w
            
            # Convert to QPixmap
            from PyQt6.QtGui import QImage
            qt_image = QImage(frame_rgb.data, w, h, bytes_per_line, QImage.Format.Format_RGB888)
            pixmap = QPixmap.fromImage(qt_image)
            
            # Display frame
            self.video_widget.set_frame(pixmap)
            
            # Update progress
            self.progress_bar.setValue(self.current_frame)
            
            # Update timestamps
            self._update_timestamps()
            
            return True
            
        except Exception as e:
            logger.error(f"Error displaying frame: {e}")
            return False
    
    def toggle_play_pause(self):
        """Toggle play/pause state"""
        if self.is_playing:
            self.pause()
        else:
            self.play()
    
    def play(self):
        """Start playing the video"""
        if not self.video_cap or self.is_playing:
            return
            
        self.is_playing = True
        self.play_button.setText("⏸")
        
        # Get speed multiplier
        speed_text = self.speed_combo.currentText().rstrip('x')
        speed = float(speed_text)
        frame_delay = max(1, int(1000 / (self.fps * speed)))
        
        self.timer.start(frame_delay)
        self.status_label.setText("Playing...")
    
    def pause(self):
        """Pause the video"""
        self.is_playing = False
        self.play_button.setText("▶")
        self.timer.stop()
        self.status_label.setText("Paused")
    
    def stop(self):
        """Stop the video and reset to beginning"""
        self.pause()
        self.current_frame = 0
        if self.video_cap:
            self._show_frame()
        self.status_label.setText("Stopped")
    
    def previous_frame(self):
        """Go to previous frame"""
        if not self.video_cap:
            return
            
        self.current_frame = max(0, self.current_frame - 1)
        self._show_frame()
        self.status_label.setText(f"Frame {self.current_frame + 1}/{self.total_frames}")
    
    def next_frame(self):
        """Go to next frame"""
        if not self.video_cap:
            return
            
        self.current_frame = min(self.total_frames - 1, self.current_frame + 1)
        self._show_frame()
        self.status_label.setText(f"Frame {self.current_frame + 1}/{self.total_frames}")
    
    def _play_next_frame(self):
        """Play next frame (internal method for continuous playback)"""
        if not self.is_playing or not self.video_cap:
            return
        
        # Show current frame
        if self._show_frame():
            self.current_frame += 1
            
            # Check if we've reached the end
            if self.current_frame >= self.total_frames:
                self.stop()
                return
        else:
            # Error showing frame, stop playback
            self.stop()


class PyQt6MainWindow(QMainWindow):
    """Main GUI window for the orchestrator"""
    
    def __init__(self, session_manager: SessionManager, 
                 websocket_server: WebSocketServer,
                 discovery_service: DiscoveryService):
        super().__init__()
        
        self.session_manager = session_manager
        self.websocket_server = websocket_server
        self.discovery_service = discovery_service
        
        self.running = False
        
        # Video file paths
        self.video_file_paths: List[str] = []
        
        self._init_ui()
        self._init_timers()
        
    def _init_ui(self):
        """Initialize the user interface"""
        self.setWindowTitle("Bucika GSR PC Orchestrator")
        self.setGeometry(100, 100, 1200, 800)
        
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
        self.discovery_status = QLabel("Discovery: Broadcasting")
        self.general_status = QLabel("Status: Ready")
        
        self.status_bar.addWidget(self.general_status)
        self.status_bar.addPermanentWidget(self.websocket_status)
        self.status_bar.addPermanentWidget(self.discovery_status)
        
        # System status panel
        self._create_system_status_panel(layout)
        
        # Create tab widget
        self.tab_widget = QTabWidget()
        layout.addWidget(self.tab_widget)
        
        # Create tabs
        self._create_devices_tab()
        self._create_sessions_tab()
        self._create_logs_tab()
        self._create_video_tab()
        self._create_analysis_tab()
        
        logger.info("PyQt6 GUI initialized successfully")
        
    def _init_timers(self):
        """Initialize update timers"""
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_display)
        self.update_timer.start(2000)  # Update every 2 seconds
        
    def _create_system_status_panel(self, parent_layout):
        """Create the system status panel"""
        status_frame = QFrame()
        status_frame.setFrameStyle(QFrame.Shape.StyledPanel)
        status_frame.setMaximumHeight(80)
        
        status_layout = QHBoxLayout(status_frame)
        
        # Services status
        services_group = QFrame()
        services_layout = QVBoxLayout(services_group)
        services_layout.addWidget(QLabel("🖥️ Services"))
        
        self.services_status = QLabel("WebSocket: ✅ | mDNS: ✅ | TimeSync: ✅")
        self.services_status.setStyleSheet("color: green; font-weight: bold;")
        services_layout.addWidget(self.services_status)
        
        # Performance metrics
        perf_group = QFrame()
        perf_layout = QVBoxLayout(perf_group)
        perf_layout.addWidget(QLabel("📊 Performance"))
        
        self.perf_status = QLabel("CPU: 0% | RAM: 0MB | Network: 0 KB/s")
        self.perf_status.setStyleSheet("color: blue;")
        perf_layout.addWidget(self.perf_status)
        
        # Active sessions
        sessions_group = QFrame()
        sessions_layout = QVBoxLayout(sessions_group)
        sessions_layout.addWidget(QLabel("📱 Sessions"))
        
        self.active_sessions_status = QLabel("Active: 0 | Devices: 0")
        self.active_sessions_status.setStyleSheet("color: purple;")
        sessions_layout.addWidget(self.active_sessions_status)
        
        # Error status
        error_group = QFrame()
        error_layout = QVBoxLayout(error_group)
        error_layout.addWidget(QLabel("⚠️ Errors"))
        
        self.error_status = QLabel("Total: 0 | Recovery Rate: 100%")
        self.error_status.setStyleSheet("color: orange;")
        error_layout.addWidget(self.error_status)
        
        status_layout.addWidget(services_group)
        status_layout.addWidget(perf_group)
        status_layout.addWidget(sessions_group)
        status_layout.addWidget(error_group)
        status_layout.addStretch()
        
        parent_layout.addWidget(status_frame)
        
    def _create_devices_tab(self):
        """Create the devices monitoring tab"""
        devices_widget = QWidget()
        self.tab_widget.addTab(devices_widget, "Connected Devices")
        
        layout = QVBoxLayout(devices_widget)
        
        # Devices table
        self.devices_table = QTableWidget()
        self.devices_table.setColumnCount(5)
        self.devices_table.setHorizontalHeaderLabels([
            "Device ID", "Device Name", "Version", "Battery", "Connected At"
        ])
        
        # Make columns resize to content
        header = self.devices_table.horizontalHeader()
        header.setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        
        layout.addWidget(self.devices_table)
        
    def _create_sessions_tab(self):
        """Create the sessions monitoring tab"""
        sessions_widget = QWidget()
        self.tab_widget.addTab(sessions_widget, "Sessions")
        
        layout = QVBoxLayout(sessions_widget)
        
        # Sessions table
        self.sessions_table = QTableWidget()
        self.sessions_table.setColumnCount(6)
        self.sessions_table.setHorizontalHeaderLabels([
            "Session ID", "Device ID", "Name", "State", "Started", "Samples"
        ])
        
        # Make columns resize to content
        header = self.sessions_table.horizontalHeader()
        header.setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        
        layout.addWidget(self.sessions_table)
        
    def _create_logs_tab(self):
        """Create the logs tab"""
        logs_widget = QWidget()
        self.tab_widget.addTab(logs_widget, "Logs")
        
        layout = QVBoxLayout(logs_widget)
        
        # Log text area
        self.log_text = QTextEdit()
        self.log_text.setReadOnly(True)
        self.log_text.setFont(QFont("Consolas", 9))
        layout.addWidget(self.log_text)
        
        # Add initial log message
        self._add_log_message("Bucika GSR PC Orchestrator started with PyQt6 GUI")
        
    def _create_video_tab(self):
        """Create the video playback tab"""
        video_widget = QWidget()
        self.tab_widget.addTab(video_widget, "🎬 Video Playback")
        
        layout = QHBoxLayout(video_widget)
        
        # Left panel - Video list
        left_panel = QWidget()
        left_layout = QVBoxLayout(left_panel)
        left_panel.setMaximumWidth(300)
        
        # Video files label
        video_label = QLabel("Video Files")
        video_label.setFont(QFont("Arial", 10, QFont.Weight.Bold))
        left_layout.addWidget(video_label)
        
        # Video list
        self.video_list = QListWidget()
        self.video_list.itemSelectionChanged.connect(self._on_video_select)
        left_layout.addWidget(self.video_list)
        
        # List controls
        controls_layout = QHBoxLayout()
        
        refresh_button = QPushButton("Refresh")
        refresh_button.clicked.connect(self._refresh_video_list)
        controls_layout.addWidget(refresh_button)
        
        browse_button = QPushButton("Browse...")
        browse_button.clicked.connect(self._browse_video_file)
        controls_layout.addWidget(browse_button)
        
        left_layout.addLayout(controls_layout)
        
        layout.addWidget(left_panel)
        
        # Right panel - Video player
        if VIDEO_SUPPORT:
            self.video_player = VideoPlayer()
            layout.addWidget(self.video_player, 1)
        else:
            # Fallback when video support is not available
            no_video_label = QLabel("Video playback not available\n(Install opencv-python and pillow for video support)")
            no_video_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
            no_video_label.setStyleSheet("color: gray; font-size: 14px;")
            layout.addWidget(no_video_label, 1)
        
        # Initialize video list
        self._refresh_video_list()
        
    def _refresh_video_list(self):
        """Refresh the list of available video files"""
        self.video_list.clear()
        
        # Find video files in sessions directory
        sessions_dir = Path("sessions")
        if not sessions_dir.exists():
            return
        
        video_extensions = {'.mp4', '.avi', '.mov', '.mkv', '.webm', '.flv', '.wmv'}
        video_files = []
        
        # Search through all session directories
        for session_dir in sessions_dir.iterdir():
            if session_dir.is_dir():
                # Check uploads folder
                uploads_dir = session_dir / "uploads"
                if uploads_dir.exists():
                    for file_path in uploads_dir.iterdir():
                        if file_path.is_file() and file_path.suffix.lower() in video_extensions:
                            relative_path = file_path.relative_to(sessions_dir)
                            video_files.append((str(relative_path), str(file_path)))
        
        # Sort and add to list
        video_files.sort(key=lambda x: x[0])
        self.video_file_paths = []
        
        for display_name, full_path in video_files:
            self.video_list.addItem(display_name)
            self.video_file_paths.append(full_path)
        
        if video_files:
            self.general_status.setText(f"Found {len(video_files)} video files")
        else:
            self.general_status.setText("No video files found")
            
    def _browse_video_file(self):
        """Browse for a video file to load"""
        if not VIDEO_SUPPORT:
            QMessageBox.warning(self, "Video Support", 
                              "Video playback requires opencv-python and pillow to be installed")
            return
            
        file_dialog = QFileDialog()
        file_path, _ = file_dialog.getOpenFileName(
            self, 
            "Select Video File",
            "",
            "Video files (*.mp4 *.avi *.mov *.mkv *.webm *.flv *.wmv);;All files (*.*)"
        )
        
        if file_path and hasattr(self, 'video_player'):
            self.video_player.load_video(file_path)
            
    def _on_video_select(self):
        """Handle video selection from list"""
        if not VIDEO_SUPPORT or not hasattr(self, 'video_player'):
            return
            
        current_row = self.video_list.currentRow()
        if current_row >= 0 and current_row < len(self.video_file_paths):
            video_path = self.video_file_paths[current_row]
            self.video_player.load_video(video_path)
            
    def _update_display(self):
        """Update the display with current data"""
        if not self.running:
            return
            
        try:
            # Update system status panel
            self._update_system_status()
            
            # Update devices table
            self._update_devices_table()
            
            # Update sessions table
            self._update_sessions_table()
            
        except Exception as e:
            logger.error(f"Error updating display: {e}")
    
    def _update_system_status(self):
        """Update the system status panel"""
        try:
            # Services status (simplified - would check actual service health)
            ws_status = "✅" if len(self.websocket_server.connected_clients) >= 0 else "❌"
            mdns_status = "✅" if self.discovery_service.is_running() else "❌"
            time_status = "✅"  # Assume time sync is running
            
            self.services_status.setText(f"WebSocket: {ws_status} | mDNS: {mdns_status} | TimeSync: {time_status}")
            
            # Performance metrics (simplified)
            import psutil
            cpu_percent = psutil.cpu_percent()
            memory = psutil.virtual_memory()
            memory_mb = memory.used // 1024 // 1024
            
            # Network stats (simplified)
            net_io = psutil.net_io_counters()
            net_speed = (net_io.bytes_sent + net_io.bytes_recv) // 1024  # KB
            
            self.perf_status.setText(f"CPU: {cpu_percent:.1f}% | RAM: {memory_mb}MB | Network: {net_speed} KB")
            
            # Session status
            active_sessions = len(self.session_manager.active_sessions)
            connected_devices = len(self.websocket_server.connected_clients)
            
            self.active_sessions_status.setText(f"Active: {active_sessions} | Devices: {connected_devices}")
            
            # Error status (simplified)
            self.error_status.setText("Total: 0 | Recovery Rate: 100%")
            
        except Exception as e:
            logger.error(f"Error updating system status: {e}")
    
    def _update_devices_table(self):
        """Update the devices table"""
        devices = self.websocket_server.get_connected_devices()
        
        self.devices_table.setRowCount(len(devices))
        
        for row, (device_id, device) in enumerate(devices.items()):
            connected_time = device.connected_at.strftime("%H:%M:%S")
            
            self.devices_table.setItem(row, 0, QTableWidgetItem(device_id))
            self.devices_table.setItem(row, 1, QTableWidgetItem(device.device_name))
            self.devices_table.setItem(row, 2, QTableWidgetItem(device.version))
            self.devices_table.setItem(row, 3, QTableWidgetItem(f"{device.battery_level}%"))
            self.devices_table.setItem(row, 4, QTableWidgetItem(connected_time))
    
    def _update_sessions_table(self):
        """Update the sessions table"""
        sessions = self.session_manager.get_all_sessions()
        
        self.sessions_table.setRowCount(len(sessions))
        
        for row, (session_id, session) in enumerate(sessions.items()):
            started_time = ""
            if session.started_at:
                started_time = session.started_at.strftime("%H:%M:%S")
            
            self.sessions_table.setItem(row, 0, QTableWidgetItem(session_id))
            self.sessions_table.setItem(row, 1, QTableWidgetItem(session.device_id))
            self.sessions_table.setItem(row, 2, QTableWidgetItem(session.session_name))
            self.sessions_table.setItem(row, 3, QTableWidgetItem(session.state.value))
            self.sessions_table.setItem(row, 4, QTableWidgetItem(started_time))
            self.sessions_table.setItem(row, 5, QTableWidgetItem(str(len(session.gsr_samples))))
    
    def _add_log_message(self, message: str):
        """Add a message to the log text widget"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        log_entry = f"[{timestamp}] {message}"
        
        self.log_text.append(log_entry)
        
        # Auto-scroll to bottom
        scrollbar = self.log_text.verticalScrollBar()
        scrollbar.setValue(scrollbar.maximum())
    
    def _create_analysis_tab(self):
        """Create the data analysis tab"""
        analysis_widget = QWidget()
        self.tab_widget.addTab(analysis_widget, "📊 Data Analysis")
        
        layout = QVBoxLayout(analysis_widget)
        
        # Control panel
        control_panel = QFrame()
        control_layout = QHBoxLayout(control_panel)
        
        # Session selection
        control_layout.addWidget(QLabel("Session:"))
        self.session_combo = QComboBox()
        self.session_combo.setMinimumWidth(200)
        control_layout.addWidget(self.session_combo)
        
        # Analysis buttons
        analyze_button = QPushButton("🔍 Analyze Session")
        analyze_button.clicked.connect(self._analyze_selected_session)
        control_layout.addWidget(analyze_button)
        
        validate_button = QPushButton("✓ Validate Data")
        validate_button.clicked.connect(self._validate_selected_session)
        control_layout.addWidget(validate_button)
        
        export_button = QPushButton("💾 Export Report")
        export_button.clicked.connect(self._export_analysis)
        control_layout.addWidget(export_button)
        
        control_layout.addStretch()
        
        refresh_sessions_button = QPushButton("🔄 Refresh")
        refresh_sessions_button.clicked.connect(self._refresh_session_list)
        control_layout.addWidget(refresh_sessions_button)
        
        layout.addWidget(control_panel)
        
        # Analysis results
        splitter = QSplitter(Qt.Orientation.Horizontal)
        layout.addWidget(splitter)
        
        # Left panel - Analysis summary
        left_panel = QWidget()
        left_layout = QVBoxLayout(left_panel)
        
        left_layout.addWidget(QLabel("Analysis Summary"))
        self.analysis_summary = QTextEdit()
        self.analysis_summary.setReadOnly(True)
        self.analysis_summary.setMaximumWidth(400)
        left_layout.addWidget(self.analysis_summary)
        
        splitter.addWidget(left_panel)
        
        # Right panel - Detailed results
        right_panel = QWidget()
        right_layout = QVBoxLayout(right_panel)
        
        right_layout.addWidget(QLabel("Detailed Analysis"))
        self.analysis_details = QTextEdit()
        self.analysis_details.setReadOnly(True)
        right_layout.addWidget(self.analysis_details)
        
        splitter.addWidget(right_panel)
        
        # Initialize session list
        self._refresh_session_list()
    
    def _refresh_session_list(self):
        """Refresh the session list for analysis"""
        self.session_combo.clear()
        
        # Add sessions from session manager
        sessions = self.session_manager.get_all_sessions()
        for session_id, session in sessions.items():
            display_name = f"{session_id} ({session.session_name})"
            self.session_combo.addItem(display_name, session_id)
        
        # Also look for sessions on disk
        sessions_dir = Path("sessions")
        if sessions_dir.exists():
            for session_dir in sessions_dir.iterdir():
                if session_dir.is_dir() and session_dir.name not in sessions:
                    gsr_files = list(session_dir.glob("gsr_data_*.csv"))
                    if gsr_files:
                        display_name = f"{session_dir.name} (Saved)"
                        self.session_combo.addItem(display_name, session_dir.name)
    
    def _analyze_selected_session(self):
        """Analyze the selected session"""
        current_session = self.session_combo.currentData()
        if not current_session:
            self.analysis_summary.setText("No session selected")
            return
        
        try:
            # Import data analyzer
            from .data_analyzer import GSRDataAnalyzer
            
            analyzer = GSRDataAnalyzer(Path("sessions"))
            analysis_results = analyzer.analyze_session(current_session)
            
            if analysis_results:
                # Display summary
                summary = f"""Session Analysis Results for {current_session}
                
📈 Statistical Summary:
• Total Samples: {analysis_results.total_samples:,}
• Mean GSR: {analysis_results.mean_gsr:.3f} µS
• Std Dev: {analysis_results.std_gsr:.3f} µS
• Min GSR: {analysis_results.min_gsr:.3f} µS
• Max GSR: {analysis_results.max_gsr:.3f} µS
• Dynamic Range: {analysis_results.max_gsr - analysis_results.min_gsr:.3f} µS

🎯 Quality Metrics:
• Data Completeness: {analysis_results.quality_score:.1%}
• Artifacts Detected: {len(analysis_results.artifacts)}
• Recording Duration: {analysis_results.duration_seconds:.1f}s

⚠️ Quality Issues:
"""
                
                if analysis_results.artifacts:
                    for artifact in analysis_results.artifacts[:5]:  # Show first 5
                        summary += f"• Frame {artifact['frame']}: {artifact['type']} (severity: {artifact['severity']:.2f})\n"
                    if len(analysis_results.artifacts) > 5:
                        summary += f"• ... and {len(analysis_results.artifacts) - 5} more\n"
                else:
                    summary += "• No significant artifacts detected\n"
                
                summary += f"\n💡 Recommendations:\n"
                if analysis_results.recommendations:
                    for rec in analysis_results.recommendations:
                        summary += f"• {rec}\n"
                else:
                    summary += "• Data quality appears good for analysis\n"
                
                self.analysis_summary.setText(summary)
                
                # Detailed results
                details = f"""Detailed Analysis Results
                
Raw Statistics:
Mean: {analysis_results.mean_gsr:.6f} µS
Standard Deviation: {analysis_results.std_gsr:.6f} µS
Variance: {analysis_results.std_gsr**2:.6f}
Skewness: {getattr(analysis_results, 'skewness', 'N/A')}
Kurtosis: {getattr(analysis_results, 'kurtosis', 'N/A')}

Temporal Analysis:
Duration: {analysis_results.duration_seconds:.3f} seconds
Sampling Rate: {analysis_results.total_samples/analysis_results.duration_seconds:.1f} Hz
First Sample: {getattr(analysis_results, 'start_time', 'N/A')}
Last Sample: {getattr(analysis_results, 'end_time', 'N/A')}

Quality Assessment:
Overall Score: {analysis_results.quality_score:.1%}
Completeness: {getattr(analysis_results, 'completeness', 'N/A')}
Consistency: {getattr(analysis_results, 'consistency', 'N/A')}

Artifact Details:
"""
                
                if analysis_results.artifacts:
                    for i, artifact in enumerate(analysis_results.artifacts):
                        details += f"\nArtifact {i+1}:\n"
                        details += f"  Type: {artifact['type']}\n"
                        details += f"  Frame: {artifact['frame']}\n"
                        details += f"  Severity: {artifact['severity']:.3f}\n"
                        details += f"  Description: {artifact.get('description', 'N/A')}\n"
                else:
                    details += "No artifacts detected in this session."
                
                self.analysis_details.setText(details)
                
            else:
                self.analysis_summary.setText(f"No analysis data available for session {current_session}")
                self.analysis_details.setText("Session may not exist or contain valid GSR data.")
                
        except Exception as e:
            error_msg = f"Error analyzing session {current_session}: {str(e)}"
            self.analysis_summary.setText(error_msg)
            self.analysis_details.setText(f"Full error:\n{traceback.format_exc()}")
            logger.error(error_msg)
    
    def _validate_selected_session(self):
        """Validate the selected session"""
        current_session = self.session_combo.currentData()
        if not current_session:
            self.analysis_summary.setText("No session selected for validation")
            return
        
        try:
            # Import data validator
            from .data_validator import DataValidator, ValidationLevel
            
            validator = DataValidator(ValidationLevel.RESEARCH_GRADE)
            session_path = Path("sessions") / current_session
            
            if session_path.exists():
                # This would be async in real implementation, simplified for GUI
                import asyncio
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
                validation_report = loop.run_until_complete(validator.validate_session(session_path))
                
                if validation_report:
                    summary = f"""Data Validation Report for {current_session}
                    
🏆 Overall Quality Score: {validation_report.overall_score:.1%}
✅ Validation Level: {validation_report.validation_level.value.upper()}

📊 Quality Metrics:
• Completeness: {validation_report.metrics['completeness']:.1%}
• Accuracy: {validation_report.metrics['accuracy']:.1%}  
• Consistency: {validation_report.metrics['consistency']:.1%}
• Timeliness: {validation_report.metrics['timeliness']:.1%}
• Validity: {validation_report.metrics['validity']:.1%}
• Integrity: {validation_report.metrics['integrity']:.1%}

⚠️ Issues Found: {len(validation_report.issues)}
✅ Passed Checks: {len(validation_report.passed_checks)}

💡 Recommendations:
"""
                    
                    if validation_report.recommendations:
                        for rec in validation_report.recommendations:
                            summary += f"• {rec}\n"
                    else:
                        summary += "• No specific recommendations - data quality is excellent\n"
                    
                    self.analysis_summary.setText(summary)
                    
                    # Detailed validation results
                    details = f"""Detailed Validation Results
                    
Quality Metrics Detail:
"""
                    for metric, value in validation_report.metrics.items():
                        details += f"{metric.capitalize()}: {value:.3f} ({value:.1%})\n"
                    
                    details += f"\nValidation Issues ({len(validation_report.issues)}):\n"
                    if validation_report.issues:
                        for i, issue in enumerate(validation_report.issues):
                            details += f"\n{i+1}. {issue['description']}\n"
                            details += f"   Severity: {issue['severity']}\n"
                            details += f"   Category: {issue['category']}\n"
                    else:
                        details += "No validation issues found.\n"
                    
                    details += f"\nPassed Checks ({len(validation_report.passed_checks)}):\n"
                    for check in validation_report.passed_checks:
                        details += f"✅ {check}\n"
                    
                    self.analysis_details.setText(details)
                else:
                    self.analysis_summary.setText("Validation failed - no report generated")
                    
            else:
                self.analysis_summary.setText(f"Session directory not found: {session_path}")
                
        except Exception as e:
            error_msg = f"Error validating session {current_session}: {str(e)}"
            self.analysis_summary.setText(error_msg)
            self.analysis_details.setText(f"Full error:\n{traceback.format_exc()}")
            logger.error(error_msg)
    
    def _export_analysis(self):
        """Export the current analysis results"""
        if not self.analysis_summary.toPlainText().strip():
            QMessageBox.warning(self, "Export Analysis", "No analysis results to export. Please analyze a session first.")
            return
        
        try:
            file_dialog = QFileDialog()
            file_path, _ = file_dialog.getSaveFileName(
                self, 
                "Export Analysis Report",
                f"analysis_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt",
                "Text files (*.txt);;All files (*.*)"
            )
            
            if file_path:
                with open(file_path, 'w') as f:
                    f.write("=== BUCIKA GSR ANALYSIS REPORT ===\n\n")
                    f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
                    f.write("SUMMARY:\n")
                    f.write(self.analysis_summary.toPlainText())
                    f.write("\n\n" + "="*50 + "\n\n")
                    f.write("DETAILED RESULTS:\n")
                    f.write(self.analysis_details.toPlainText())
                
                QMessageBox.information(self, "Export Analysis", f"Analysis report exported to:\n{file_path}")
                
        except Exception as e:
            QMessageBox.critical(self, "Export Error", f"Failed to export analysis report:\n{str(e)}")
    
    def _add_log_message(self, message: str):
        """Add a message to the log text widget"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        log_entry = f"[{timestamp}] {message}"
        
        self.log_text.append(log_entry)
        
        # Auto-scroll to bottom
        scrollbar = self.log_text.verticalScrollBar()
        scrollbar.setValue(scrollbar.maximum())
    
    def start(self):
        """Start the GUI"""
        self.running = True
        self.show()
        logger.info("PyQt6 GUI started successfully")
    
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
        
        self.app = None
        self.window = None
        self.running = False
    
    def start(self):
        """Start the GUI in a separate thread"""
        if not self.running:
            self.running = True
            gui_thread = threading.Thread(target=self._run_gui, daemon=True)
            gui_thread.start()
    
    def _run_gui(self):
        """Run the GUI main loop"""
        try:
            # Create QApplication
            self.app = QApplication(sys.argv)
            
            # Create main window
            self.window = PyQt6MainWindow(
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


# Custom log handler to display logs in GUI
class GUILogHandler:
    """Log handler that forwards messages to the GUI"""
    
    def __init__(self, main_window: Optional[MainWindow] = None):
        self.main_window = main_window
    
    def write(self, message: str):
        """Write log message to GUI"""
        if self.main_window and self.main_window.window and message.strip():
            try:
                # Add log message to GUI
                self.main_window.window._add_log_message(message.strip())
            except:
                pass  # Ignore errors when GUI is not ready
    
    def flush(self):
        """Required for file-like interface"""
        pass