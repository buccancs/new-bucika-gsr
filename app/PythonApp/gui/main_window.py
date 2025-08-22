import os
import time
from datetime import datetime
from PyQt5.QtCore import QSize, Qt, QTimer, pyqtSignal
from PyQt5.QtGui import QColor, QFont, QIcon, QPainter, QPalette, QPixmap
from PyQt5.QtWidgets import (
    QAction,
    QApplication,
    QComboBox,
    QFileDialog,
    QFrame,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QMainWindow,
    QMenuBar,
    QMessageBox,
    QProgressBar,
    QPushButton,
    QSizePolicy,
    QSlider,
    QSpacerItem,
    QSplitter,
    QStatusBar,
    QTabWidget,
    QTextEdit,
    QToolBar,
    QVBoxLayout,
    QWidget,
)

try:
    from bucika_gsr.network.device_server import JsonSocketServer
except ImportError:
    try:
        from ..network.device_server import JsonSocketServer
    except ImportError:
        JsonSocketServer = None

try:
    from bucika_gsr.session.session_manager import SessionManager
except ImportError:
    try:
        from ..session.session_manager import SessionManager
    except ImportError:
        SessionManager = None

try:
    from bucika_gsr.webcam.webcam_capture import WebcamCapture
except ImportError:
    try:
        from ..webcam.webcam_capture import WebcamCapture
    except ImportError:
        WebcamCapture = None

try:
    from .device_panel import DeviceStatusPanel
except ImportError:
    DeviceStatusPanel = None

try:
    from .preview_panel import PreviewPanel
except ImportError:
    PreviewPanel = None

try:
    from .stimulus_controller import StimulusController
except ImportError:
    try:
        from .stimulus_panel import StimulusControlPanel as StimulusController
    except ImportError:
        StimulusController = None

try:
    from ..utils.logging_config import get_logger
    logger = get_logger(__name__)
except ImportError:
    import logging
    logger = logging.getLogger(__name__)


class ModernButton(QPushButton):
    """Modern styled button with primary/secondary variants."""
    
    def __init__(self, text="", icon_path=None, primary=False, parent=None):
        super().__init__(text, parent)
        self.primary = primary
        self.setFont(QFont("Segoe UI", 9))
        self.setMinimumHeight(32)
        self.setCursor(Qt.PointingHandCursor)
        
        if icon_path and os.path.exists(icon_path):
            self.setIcon(QIcon(icon_path))
            self.setIconSize(QSize(16, 16))
        
        self.update_style()
    
    def update_style(self):
        """Apply modern styling to the button."""
        if self.primary:
            self.setStyleSheet("""
                QPushButton {
                    background-color: #0078d4;
                    color: white;
                    border: 1px solid #106ebe;
                    border-radius: 4px;
                    padding: 8px 16px;
                    font-weight: bold;
                }
                QPushButton:hover {
                    background-color: #106ebe;
                }
                QPushButton:pressed {
                    background-color: #005a9e;
                }
                QPushButton:disabled {
                    background-color: #cccccc;
                    color: #888888;
                    border: 1px solid #aaaaaa;
                }
            """)
        else:
            self.setStyleSheet("""
                QPushButton {
                    background-color: #f3f2f1;
                    color: #323130;
                    border: 1px solid #d2d0ce;
                    border-radius: 4px;
                    padding: 8px 16px;
                }
                QPushButton:hover {
                    background-color: #edebe9;
                }
                QPushButton:pressed {
                    background-color: #e1dfdd;
                }
                QPushButton:disabled {
                    background-color: #f8f8f8;
                    color: #a19f9d;
                    border: 1px solid #e1dfdd;
                }
            """)


class MainWindow(QMainWindow):
    """Main application window with tabbed interface for multi-sensor recording system."""
    
    # Signals
    session_started = pyqtSignal(str)
    session_stopped = pyqtSignal()
    device_connected = pyqtSignal(str)
    device_disconnected = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        self.setWindowTitle("Multi-Sensor Recording System Controller")
        self.setGeometry(100, 100, 1400, 900)
        
        # Initialize core components with error handling
        self.json_server = JsonSocketServer() if JsonSocketServer else None
        self.server_running = False
        self.webcam_capture = WebcamCapture() if WebcamCapture else None
        self.webcam_previewing = False
        self.webcam_recording = False
        self.session_manager = SessionManager() if SessionManager else None
        self.current_session_id = None
        
        # Initialize stimulus controller
        try:
            self.stimulus_controller = StimulusController(self) if StimulusController else None
        except Exception as e:
            logger.warning(f"Could not initialize stimulus controller: {e}")
            self.stimulus_controller = None
        
        # Performance monitoring
        self.performance_timer = QTimer()
        self.performance_timer.timeout.connect(self.update_performance_metrics)
        self.performance_timer.setInterval(1000)
        
        # Demo simulation timer
        self.preview_timer = QTimer()
        self.preview_timer.timeout.connect(self.update_demo_previews)
        
        # UI state tracking
        self.connected_devices = {"Thermal": False, "GSR": False, "Webcam": False, "Mobile": False}
        self.session_active = False
        
        # Initialize UI
        self.init_ui()
        self.connect_signals()
        self.init_placeholder_data()
        self.setup_demo_preview_simulation()
    
    def init_ui(self):
        """Initialize the user interface."""
        self.create_menu_bar()
        self.create_toolbar()
        self.create_central_widget()
        self.create_status_bar()
    
    def create_menu_bar(self):
        """Create the application menu bar."""
        menubar = self.menuBar()
        
        # File menu
        file_menu = menubar.addMenu('File')
        
        new_session_action = QAction('New Session', self)
        new_session_action.setShortcut('Ctrl+N')
        new_session_action.triggered.connect(self.new_session)
        file_menu.addAction(new_session_action)
        
        open_session_action = QAction('Open Session', self)
        open_session_action.setShortcut('Ctrl+O')
        open_session_action.triggered.connect(self.open_session)
        file_menu.addAction(open_session_action)
        
        file_menu.addSeparator()
        
        exit_action = QAction('Exit', self)
        exit_action.setShortcut('Ctrl+Q')
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)
        
        # View menu
        view_menu = menubar.addMenu('View')
        
        dashboard_action = QAction('Dashboard', self)
        dashboard_action.triggered.connect(lambda: self.tab_widget.setCurrentIndex(0))
        view_menu.addAction(dashboard_action)
        
        logs_action = QAction('Logs', self)
        logs_action.triggered.connect(lambda: self.tab_widget.setCurrentIndex(1))
        view_menu.addAction(logs_action)
        
        playback_action = QAction('Playback & Annotation', self)
        playback_action.triggered.connect(lambda: self.tab_widget.setCurrentIndex(2))
        view_menu.addAction(playback_action)
    
    def create_toolbar(self):
        """Create the main toolbar."""
        toolbar = QToolBar()
        self.addToolBar(toolbar)
        
        # Session controls
        self.start_session_btn = ModernButton("Start Session", primary=True)
        self.start_session_btn.clicked.connect(self.start_session)
        toolbar.addWidget(self.start_session_btn)
        
        self.stop_session_btn = ModernButton("Stop Session")
        self.stop_session_btn.clicked.connect(self.stop_session)
        self.stop_session_btn.setEnabled(False)
        toolbar.addWidget(self.stop_session_btn)
        
        toolbar.addSeparator()
        
        # Session status indicator
        self.session_status_label = QLabel("● Idle")
        self.session_status_label.setStyleSheet("color: red; font-weight: bold;")
        toolbar.addWidget(self.session_status_label)
        
        toolbar.addSeparator()
        
        # Device connection counter
        self.device_counter_label = QLabel("Connected: 0/4 devices")
        toolbar.addWidget(self.device_counter_label)
        
        toolbar.addSeparator()
        
        # Performance indicator
        self.performance_label = QLabel("Performance: Good")
        toolbar.addWidget(self.performance_label)
    
    def create_central_widget(self):
        """Create the central tabbed widget."""
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        
        layout = QVBoxLayout(central_widget)
        
        # Create tab widget
        self.tab_widget = QTabWidget()
        self.tab_widget.addTab(self.create_dashboard_tab(), "Dashboard")
        self.tab_widget.addTab(self.create_logs_tab(), "Logs")
        self.tab_widget.addTab(self.create_playback_annotation_tab(), "Playback & Annotation")
        
        layout.addWidget(self.tab_widget)
    
    def create_dashboard_tab(self):
        """Create the dashboard tab with device grid."""
        dashboard_widget = QWidget()
        layout = QVBoxLayout(dashboard_widget)
        
        # Title
        title_label = QLabel("Multi-Device Coordination Dashboard")
        title_label.setFont(QFont("Segoe UI", 14, QFont.Bold))
        title_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(title_label)
        
        # Device grid (2x2 layout)
        grid_frame = QFrame()
        grid_frame.setFrameStyle(QFrame.StyledPanel)
        grid_layout = QGridLayout(grid_frame)
        
        self.device_widgets = {}
        devices = [
            ("Thermal", "Thermal Camera", 0, 0),
            ("GSR", "GSR Sensor", 0, 1),
            ("Webcam", "PC Webcam", 1, 0),
            ("Mobile", "Mobile Device", 1, 1)
        ]
        
        for device_id, device_name, row, col in devices:
            device_widget = self.create_device_widget(device_id, device_name)
            self.device_widgets[device_id] = device_widget
            grid_layout.addWidget(device_widget, row, col)
        
        layout.addWidget(grid_frame)
        
        # Control panel
        control_panel = self.create_dashboard_controls()
        layout.addWidget(control_panel)
        
        return dashboard_widget
    
    def create_device_widget(self, device_id, device_name):
        """Create a widget for individual device control."""
        widget = QGroupBox(device_name)
        layout = QVBoxLayout(widget)
        
        # Status indicator
        status_layout = QHBoxLayout()
        status_label = QLabel("Status:")
        status_indicator = QLabel("● Disconnected")
        status_indicator.setStyleSheet("color: red; font-weight: bold;")
        status_layout.addWidget(status_label)
        status_layout.addWidget(status_indicator)
        status_layout.addStretch()
        layout.addLayout(status_layout)
        
        # Preview area
        preview_label = QLabel("No Preview")
        preview_label.setMinimumHeight(120)
        preview_label.setStyleSheet("""
            QLabel {
                border: 2px dashed #ccc;
                background-color: #f5f5f5;
                color: #888;
                text-align: center;
                font-size: 12px;
            }
        """)
        preview_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(preview_label)
        
        # Device-specific info
        info_label = QLabel("Not connected")
        info_label.setStyleSheet("color: #666; font-size: 10px;")
        layout.addWidget(info_label)
        
        # Control buttons
        button_layout = QHBoxLayout()
        connect_btn = ModernButton("Connect")
        connect_btn.clicked.connect(lambda: self.toggle_device_connection(device_id))
        record_btn = ModernButton("Record")
        record_btn.setEnabled(False)
        
        button_layout.addWidget(connect_btn)
        button_layout.addWidget(record_btn)
        layout.addLayout(button_layout)
        
        # Store references
        widget.status_indicator = status_indicator
        widget.preview_label = preview_label
        widget.info_label = info_label
        widget.connect_btn = connect_btn
        widget.record_btn = record_btn
        
        return widget
    
    def create_dashboard_controls(self):
        """Create dashboard control panel."""
        control_widget = QGroupBox("Session Controls")
        layout = QHBoxLayout(control_widget)
        
        # Session info
        session_info_layout = QVBoxLayout()
        self.session_id_label = QLabel("Session ID: None")
        self.session_duration_label = QLabel("Duration: 00:00:00")
        session_info_layout.addWidget(self.session_id_label)
        session_info_layout.addWidget(self.session_duration_label)
        layout.addLayout(session_info_layout)
        
        layout.addStretch()
        
        # Performance monitoring
        perf_layout = QVBoxLayout()
        perf_layout.addWidget(QLabel("System Performance:"))
        self.cpu_progress = QProgressBar()
        self.memory_progress = QProgressBar()
        self.cpu_progress.setMaximum(100)
        self.memory_progress.setMaximum(100)
        perf_layout.addWidget(QLabel("CPU:"))
        perf_layout.addWidget(self.cpu_progress)
        perf_layout.addWidget(QLabel("Memory:"))
        perf_layout.addWidget(self.memory_progress)
        layout.addLayout(perf_layout)
        
        return control_widget
    
    def create_logs_tab(self):
        """Create the logs tab with filtering and statistics."""
        logs_widget = QWidget()
        layout = QVBoxLayout(logs_widget)
        
        # Title and controls
        header_layout = QHBoxLayout()
        title_label = QLabel("System Logs")
        title_label.setFont(QFont("Segoe UI", 14, QFont.Bold))
        header_layout.addWidget(title_label)
        
        header_layout.addStretch()
        
        # Log filtering
        filter_combo = QComboBox()
        filter_combo.addItems(["All", "System", "Network", "Session", "Performance", "Errors", "Backend"])
        header_layout.addWidget(QLabel("Filter:"))
        header_layout.addWidget(filter_combo)
        
        clear_btn = ModernButton("Clear Logs")
        clear_btn.clicked.connect(self.clear_logs)
        header_layout.addWidget(clear_btn)
        
        export_btn = ModernButton("Export")
        export_btn.clicked.connect(self.export_logs)
        header_layout.addWidget(export_btn)
        
        layout.addLayout(header_layout)
        
        # Log statistics
        stats_layout = QHBoxLayout()
        self.log_count_label = QLabel("Messages: 0")
        self.error_count_label = QLabel("Errors: 0")
        self.warning_count_label = QLabel("Warnings: 0")
        stats_layout.addWidget(self.log_count_label)
        stats_layout.addWidget(self.error_count_label)
        stats_layout.addWidget(self.warning_count_label)
        stats_layout.addStretch()
        layout.addLayout(stats_layout)
        
        # Log display
        self.log_text_edit = QTextEdit()
        self.log_text_edit.setReadOnly(True)
        self.log_text_edit.setFont(QFont("Consolas", 9))
        layout.addWidget(self.log_text_edit)
        
        return logs_widget
    
    def create_playback_annotation_tab(self):
        """Create the playback and annotation tab."""
        playback_widget = QWidget()
        layout = QVBoxLayout(playback_widget)
        
        # Title and session loading
        header_layout = QHBoxLayout()
        title_label = QLabel("Session Playback & Annotation")
        title_label.setFont(QFont("Segoe UI", 14, QFont.Bold))
        header_layout.addWidget(title_label)
        
        header_layout.addStretch()
        
        load_session_btn = ModernButton("Load Session")
        load_session_btn.clicked.connect(self.load_playback_session)
        header_layout.addWidget(load_session_btn)
        
        layout.addLayout(header_layout)
        
        # Video playback area
        video_frame = QGroupBox("Video Playback")
        video_layout = QVBoxLayout(video_frame)
        
        self.video_display = QLabel("No session loaded")
        self.video_display.setMinimumHeight(300)
        self.video_display.setStyleSheet("""
            QLabel {
                border: 2px solid #ccc;
                background-color: #000;
                color: white;
                text-align: center;
                font-size: 14px;
            }
        """)
        self.video_display.setAlignment(Qt.AlignCenter)
        video_layout.addWidget(self.video_display)
        
        # Playback controls
        controls_layout = QHBoxLayout()
        self.play_btn = ModernButton("Play", primary=True)
        self.pause_btn = ModernButton("Pause")
        self.stop_btn = ModernButton("Stop")
        
        self.play_btn.clicked.connect(self.play_video)
        self.pause_btn.clicked.connect(self.pause_video)
        self.stop_btn.clicked.connect(self.stop_video)
        
        controls_layout.addWidget(self.play_btn)
        controls_layout.addWidget(self.pause_btn)
        controls_layout.addWidget(self.stop_btn)
        
        # Timeline and speed controls
        controls_layout.addWidget(QLabel("Timeline:"))
        self.timeline_slider = QSlider(Qt.Horizontal)
        self.timeline_slider.setMinimum(0)
        self.timeline_slider.setMaximum(100)
        controls_layout.addWidget(self.timeline_slider)
        
        controls_layout.addWidget(QLabel("Speed:"))
        self.speed_combo = QComboBox()
        self.speed_combo.addItems(["0.25x", "0.5x", "1.0x", "1.5x", "2.0x", "4.0x"])
        self.speed_combo.setCurrentText("1.0x")
        controls_layout.addWidget(self.speed_combo)
        
        video_layout.addLayout(controls_layout)
        layout.addWidget(video_frame)
        
        # GSR plot and annotation area
        bottom_layout = QHBoxLayout()
        
        # GSR plot area
        gsr_frame = QGroupBox("GSR Data")
        gsr_layout = QVBoxLayout(gsr_frame)
        self.gsr_plot_label = QLabel("GSR plot will be displayed here")
        self.gsr_plot_label.setMinimumHeight(150)
        self.gsr_plot_label.setStyleSheet("""
            QLabel {
                border: 1px solid #ccc;
                background-color: #f9f9f9;
                text-align: center;
            }
        """)
        self.gsr_plot_label.setAlignment(Qt.AlignCenter)
        gsr_layout.addWidget(self.gsr_plot_label)
        bottom_layout.addWidget(gsr_frame, 2)
        
        # Annotation panel
        annotation_frame = QGroupBox("Annotations")
        annotation_layout = QVBoxLayout(annotation_frame)
        
        # Add annotation controls
        add_annotation_layout = QHBoxLayout()
        self.annotation_input = QLineEdit()
        self.annotation_input.setPlaceholderText("Add annotation...")
        add_annotation_btn = ModernButton("Add")
        add_annotation_btn.clicked.connect(self.add_annotation)
        
        add_annotation_layout.addWidget(self.annotation_input)
        add_annotation_layout.addWidget(add_annotation_btn)
        annotation_layout.addLayout(add_annotation_layout)
        
        # Annotations list
        self.annotations_text = QTextEdit()
        self.annotations_text.setMaximumHeight(100)
        self.annotations_text.setReadOnly(True)
        annotation_layout.addWidget(self.annotations_text)
        
        # Export annotations
        export_annotations_btn = ModernButton("Export Annotations")
        export_annotations_btn.clicked.connect(self.export_annotations)
        annotation_layout.addWidget(export_annotations_btn)
        
        bottom_layout.addWidget(annotation_frame, 1)
        layout.addLayout(bottom_layout)
        
        return playback_widget
    
    def create_status_bar(self):
        """Create the status bar."""
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        
        # Add permanent widgets
        self.connection_status = QLabel("Server: Disconnected")
        self.recording_status = QLabel("Recording: Inactive")
        
        self.status_bar.addPermanentWidget(self.connection_status)
        self.status_bar.addPermanentWidget(self.recording_status)
        
        self.status_bar.showMessage("Ready")
    
    def connect_signals(self):
        """Connect all signal handlers."""
        if self.json_server:
            try:
                # Connect server signals if available
                pass
            except Exception as e:
                logger.warning(f"Could not connect server signals: {e}")
        
        if self.webcam_capture:
            try:
                # Connect webcam signals if available
                pass
            except Exception as e:
                logger.warning(f"Could not connect webcam signals: {e}")
    
    def setup_demo_preview_simulation(self):
        """Set up demo simulation for preview functionality."""
        self.preview_timer.timeout.connect(self.update_demo_previews)
        self.preview_timer.start(2000)  # Update every 2 seconds
    
    def init_placeholder_data(self):
        """Initialize with placeholder data for demonstration."""
        self.add_log_message("System", "Application started successfully")
        self.add_log_message("Network", "Server initialized on port 8080")
        self.add_log_message("System", "Demo mode active - hardware components simulated")
        
        # Initialize performance metrics
        self.cpu_progress.setValue(25)
        self.memory_progress.setValue(40)
    
    # Event handlers
    def new_session(self):
        """Create a new recording session."""
        if self.session_manager:
            try:
                session_id = self.session_manager.create_session()
                self.current_session_id = session_id
                self.session_id_label.setText(f"Session ID: {session_id}")
                self.add_log_message("Session", f"New session created: {session_id}")
                return session_id
            except Exception as e:
                self.add_log_message("Session", f"Failed to create session: {e}")
                QMessageBox.warning(self, "Session Error", f"Could not create session: {e}")
        else:
            # Demo mode
            import uuid
            session_id = str(uuid.uuid4())[:8]
            self.current_session_id = session_id
            self.session_id_label.setText(f"Session ID: {session_id}")
            self.add_log_message("Session", f"Demo session created: {session_id}")
            return session_id
    
    def open_session(self):
        """Open an existing session."""
        file_path, _ = QFileDialog.getOpenFileName(
            self, "Open Session", "", "Session files (*.json);;All files (*)"
        )
        if file_path:
            self.add_log_message("Session", f"Loading session from: {file_path}")
            # Implement session loading logic here
    
    def start_session(self):
        """Start recording session."""
        if not self.current_session_id:
            self.new_session()
        
        self.session_active = True
        self.start_session_btn.setEnabled(False)
        self.stop_session_btn.setEnabled(True)
        self.session_status_label.setText("● Recording")
        self.session_status_label.setStyleSheet("color: green; font-weight: bold;")
        self.recording_status.setText("Recording: Active")
        
        # Start performance monitoring
        self.performance_timer.start()
        
        self.add_log_message("Session", f"Session started: {self.current_session_id}")
        self.session_started.emit(self.current_session_id)
    
    def stop_session(self):
        """Stop recording session."""
        self.session_active = False
        self.start_session_btn.setEnabled(True)
        self.stop_session_btn.setEnabled(False)
        self.session_status_label.setText("● Idle")
        self.session_status_label.setStyleSheet("color: red; font-weight: bold;")
        self.recording_status.setText("Recording: Inactive")
        
        # Stop performance monitoring
        self.performance_timer.stop()
        
        self.add_log_message("Session", f"Session stopped: {self.current_session_id}")
        self.session_stopped.emit()
    
    def toggle_device_connection(self, device_id):
        """Toggle device connection status."""
        widget = self.device_widgets[device_id]
        is_connected = self.connected_devices[device_id]
        
        if is_connected:
            # Disconnect device
            self.connected_devices[device_id] = False
            widget.status_indicator.setText("● Disconnected")
            widget.status_indicator.setStyleSheet("color: red; font-weight: bold;")
            widget.connect_btn.setText("Connect")
            widget.record_btn.setEnabled(False)
            widget.preview_label.setText("No Preview")
            widget.info_label.setText("Not connected")
            self.add_log_message("Network", f"{device_id} device disconnected")
            self.device_disconnected.emit(device_id)
        else:
            # Connect device
            self.connected_devices[device_id] = True
            widget.status_indicator.setText("● Connected")
            widget.status_indicator.setStyleSheet("color: green; font-weight: bold;")
            widget.connect_btn.setText("Disconnect")
            widget.record_btn.setEnabled(True)
            widget.preview_label.setText("Live Preview")
            self.add_log_message("Network", f"{device_id} device connected")
            self.device_connected.emit(device_id)
        
        # Update device counter
        connected_count = sum(self.connected_devices.values())
        self.device_counter_label.setText(f"Connected: {connected_count}/4 devices")
    
    def update_demo_previews(self):
        """Update demo preview data for connected devices."""
        for device_id, is_connected in self.connected_devices.items():
            if is_connected:
                widget = self.device_widgets[device_id]
                current_time = time.strftime("%H:%M:%S")
                
                if device_id == "Thermal":
                    widget.info_label.setText(f"Resolution: 640x480, Temp: 23.5°C, {current_time}")
                elif device_id == "GSR":
                    import random
                    gsr_value = random.uniform(2.0, 8.0)
                    widget.info_label.setText(f"GSR: {gsr_value:.2f} μS, Rate: 250Hz, {current_time}")
                elif device_id == "Webcam":
                    widget.info_label.setText(f"Resolution: 1920x1080, FPS: 30, {current_time}")
                elif device_id == "Mobile":
                    widget.info_label.setText(f"Battery: 85%, Signal: Strong, {current_time}")
    
    def update_performance_metrics(self):
        """Update system performance indicators."""
        import random
        
        # Simulate CPU and memory usage
        cpu_usage = random.randint(20, 80)
        memory_usage = random.randint(30, 70)
        
        self.cpu_progress.setValue(cpu_usage)
        self.memory_progress.setValue(memory_usage)
        
        # Update session duration
        if self.session_active and self.current_session_id:
            # Simple duration tracking
            self.session_duration_label.setText("Duration: 00:05:23")  # Demo value
    
    def add_log_message(self, category, message):
        """Add a message to the log display."""
        timestamp = datetime.now().strftime("%H:%M:%S")
        log_entry = f"[{timestamp}] [{category}] {message}"
        
        self.log_text_edit.append(log_entry)
        
        # Update log statistics
        self.update_log_statistics()
    
    def update_log_statistics(self):
        """Update log statistics display."""
        text = self.log_text_edit.toPlainText()
        lines = text.split('\n') if text else []
        message_count = len([line for line in lines if line.strip()])
        error_count = len([line for line in lines if 'Error' in line or 'ERROR' in line])
        warning_count = len([line for line in lines if 'Warning' in line or 'WARNING' in line])
        
        self.log_count_label.setText(f"Messages: {message_count}")
        self.error_count_label.setText(f"Errors: {error_count}")
        self.warning_count_label.setText(f"Warnings: {warning_count}")
    
    def clear_logs(self):
        """Clear all log messages."""
        self.log_text_edit.clear()
        self.update_log_statistics()
        self.add_log_message("System", "Log cleared by user")
    
    def export_logs(self):
        """Export logs to file."""
        file_path, _ = QFileDialog.getSaveFileName(
            self, "Export Logs", "logs.txt", "Text files (*.txt);;All files (*)"
        )
        if file_path:
            try:
                with open(file_path, 'w') as f:
                    f.write(self.log_text_edit.toPlainText())
                self.add_log_message("System", f"Logs exported to: {file_path}")
                QMessageBox.information(self, "Export Complete", f"Logs exported to:\n{file_path}")
            except Exception as e:
                self.add_log_message("System", f"Failed to export logs: {e}")
                QMessageBox.warning(self, "Export Error", f"Could not export logs: {e}")
    
    def load_playback_session(self):
        """Load a session for playback."""
        file_path, _ = QFileDialog.getOpenFileName(
            self, "Load Session for Playback", "", "Session files (*.json);;All files (*)"
        )
        if file_path:
            self.video_display.setText(f"Session loaded: {os.path.basename(file_path)}")
            self.add_log_message("Playback", f"Session loaded for playback: {file_path}")
            
            # Enable playback controls
            self.play_btn.setEnabled(True)
            self.pause_btn.setEnabled(True)
            self.stop_btn.setEnabled(True)
    
    def play_video(self):
        """Start video playback."""
        self.video_display.setText("Playing...")
        self.add_log_message("Playback", "Video playback started")
    
    def pause_video(self):
        """Pause video playback."""
        self.video_display.setText("Paused")
        self.add_log_message("Playback", "Video playback paused")
    
    def stop_video(self):
        """Stop video playback."""
        self.video_display.setText("Stopped")
        self.add_log_message("Playback", "Video playback stopped")
    
    def add_annotation(self):
        """Add an annotation at current timestamp."""
        annotation_text = self.annotation_input.text().strip()
        if annotation_text:
            timestamp = "00:05:23"  # Demo timestamp
            annotation = f"[{timestamp}] {annotation_text}"
            
            current_text = self.annotations_text.toPlainText()
            if current_text:
                self.annotations_text.setText(current_text + "\n" + annotation)
            else:
                self.annotations_text.setText(annotation)
            
            self.annotation_input.clear()
            self.add_log_message("Annotation", f"Added annotation: {annotation_text}")
    
    def export_annotations(self):
        """Export annotations to file."""
        file_path, _ = QFileDialog.getSaveFileName(
            self, "Export Annotations", "annotations.txt", 
            "Text files (*.txt);;CSV files (*.csv);;All files (*)"
        )
        if file_path:
            try:
                with open(file_path, 'w') as f:
                    f.write(self.annotations_text.toPlainText())
                self.add_log_message("Annotation", f"Annotations exported to: {file_path}")
                QMessageBox.information(self, "Export Complete", f"Annotations exported to:\n{file_path}")
            except Exception as e:
                self.add_log_message("Annotation", f"Failed to export annotations: {e}")
                QMessageBox.warning(self, "Export Error", f"Could not export annotations: {e}")


# Backward compatibility alias
EnhancedMainWindow = MainWindow