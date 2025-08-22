from PyQt5.QtCore import Qt, QTimer, pyqtSignal
from PyQt5.QtGui import QFont, QPixmap
from PyQt5.QtWidgets import (
    QComboBox,
    QFrame,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QMainWindow,
    QMessageBox,
    QProgressBar,
    QPushButton,
    QSizePolicy,
    QSpinBox,
    QStatusBar,
    QVBoxLayout,
    QWidget,
)
from ..utils.logging_config import get_logger, log_method_entry, performance_timer
from .webcam.dual_webcam_capture import DualWebcamCapture, test_dual_webcam_access
logger = get_logger(__name__)
class CameraPreviewWidget(QFrame):
    def __init__(self, camera_id: int, parent=None):
        super().__init__(parent)
        self.camera_id = camera_id
        self.setFrameStyle(QFrame.StyledPanel)
        self.setLineWidth(2)
        self.setMinimumSize(320, 240)
        self.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Expanding)
        self.setup_ui()
    def setup_ui(self):
        layout = QVBoxLayout(self)
        self.title_label = QLabel(f"Camera {self.camera_id}")
        self.title_label.setAlignment(Qt.AlignCenter)
        font = QFont()
        font.setBold(True)
        font.setPointSize(12)
        self.title_label.setFont(font)
        layout.addWidget(self.title_label)
        self.preview_label = QLabel("No Preview")
        self.preview_label.setAlignment(Qt.AlignCenter)
        self.preview_label.setMinimumSize(300, 200)
        self.preview_label.setStyleSheet(
        )
        layout.addWidget(self.preview_label, 1)
        status_layout = QHBoxLayout()
        self.status_label = QLabel("Disconnected")
        self.status_label.setStyleSheet("colour: red; font-weight: bold;")
        status_layout.addWidget(QLabel("Status:"))
        status_layout.addWidget(self.status_label)
        status_layout.addStretch()
        self.fps_label = QLabel("0 FPS")
        status_layout.addWidget(QLabel("FPS:"))
        status_layout.addWidget(self.fps_label)
        layout.addLayout(status_layout)
    def update_preview(self, pixmap: QPixmap):
        if pixmap and not pixmap.isNull():
            scaled_pixmap = pixmap.scaled(
                self.preview_label.size(), Qt.KeepAspectRatio, Qt.SmoothTransformation
            )
            self.preview_label.setPixmap(scaled_pixmap)
    def update_status(self, connected: bool, fps: float = 0.0):
        if connected:
            self.status_label.setText("Connected")
            self.status_label.setStyleSheet("colour: green; font-weight: bold;")
        else:
            self.status_label.setText("Disconnected")
            self.status_label.setStyleSheet("colour: red; font-weight: bold;")
        self.fps_label.setText(f"{fps:.1f} FPS")
class DualWebcamSettingsPanel(QGroupBox):
    settings_changed = pyqtSignal(dict)
    def __init__(self, parent=None):
        super().__init__("Camera Settings", parent)
        self.setup_ui()
    def setup_ui(self):
        layout = QGridLayout(self)
        layout.addWidget(QLabel("Camera 1 Index:"), 0, 0)
        self.camera1_spinbox = QSpinBox()
        self.camera1_spinbox.setRange(0, 10)
        self.camera1_spinbox.setValue(0)
        self.camera1_spinbox.valueChanged.connect(self.on_settings_changed)
        layout.addWidget(self.camera1_spinbox, 0, 1)
        layout.addWidget(QLabel("Camera 2 Index:"), 1, 0)
        self.camera2_spinbox = QSpinBox()
        self.camera2_spinbox.setRange(0, 10)
        self.camera2_spinbox.setValue(1)
        self.camera2_spinbox.valueChanged.connect(self.on_settings_changed)
        layout.addWidget(self.camera2_spinbox, 1, 1)
        layout.addWidget(QLabel("Resolution:"), 2, 0)
        self.resolution_combo = QComboBox()
        self.resolution_combo.addItems(
            ["3840x2160 (4K)", "1920x1080 (HD)", "1280x720 (HD Ready)", "640x480 (VGA)"]
        )
        self.resolution_combo.currentTextChanged.connect(self.on_settings_changed)
        layout.addWidget(self.resolution_combo, 2, 1)
        layout.addWidget(QLabel("Frame Rate:"), 3, 0)
        self.fps_spinbox = QSpinBox()
        self.fps_spinbox.setRange(1, 60)
        self.fps_spinbox.setValue(30)
        self.fps_spinbox.setSuffix(" FPS")
        self.fps_spinbox.valueChanged.connect(self.on_settings_changed)
        layout.addWidget(self.fps_spinbox, 3, 1)
    def on_settings_changed(self):
        settings = self.get_settings()
        self.settings_changed.emit(settings)
        logger.debug(f"Camera settings changed: {settings}")
    def get_settings(self) -> dict:
        resolution_text = self.resolution_combo.currentText()
        width, height = self.parse_resolution(resolution_text)
        return {
            "camera1_index": self.camera1_spinbox.value(),
            "camera2_index": self.camera2_spinbox.value(),
            "width": width,
            "height": height,
            "fps": self.fps_spinbox.value(),
        }
    def set_settings(self, settings: dict):
        if "camera1_index" in settings:
            self.camera1_spinbox.setValue(settings["camera1_index"])
        if "camera2_index" in settings:
            self.camera2_spinbox.setValue(settings["camera2_index"])
        if "fps" in settings:
            self.fps_spinbox.setValue(settings["fps"])
        if "width" in settings and "height" in settings:
            resolution_text = f"{settings['width']}x{settings['height']}"
            for i in range(self.resolution_combo.count()):
                if resolution_text in self.resolution_combo.itemText(i):
                    self.resolution_combo.setCurrentIndex(i)
                    break
    def parse_resolution(self, resolution_text: str) -> tuple:
        try:
            resolution_part = resolution_text.split()[0]
            width, height = map(int, resolution_part.split("x"))
            return width, height
        except (ValueError, IndexError):
            logger.warning(f"Could not parse resolution: {resolution_text}")
            return 1920, 1080
class DualWebcamMainWindow(QMainWindow):
    def __init__(self, camera_indices: list = None, initial_settings: dict = None):
        super().__init__()
        self.camera_indices = camera_indices or [0, 1]
        self.initial_settings = initial_settings or {}
        self.dual_webcam_capture = None
        self.is_recording = False
        self.is_previewing = False
        self.frame_count = 0
        self.last_fps_update = 0
        self.setup_ui()
        self.setup_connections()
        self.apply_initial_settings()
        logger.info(
            f"DualWebcamMainWindow initialized with cameras {self.camera_indices}"
        )
    @log_method_entry
    def setup_ui(self):
        self.setWindowTitle("Dual Webcam Recording System")
        self.setGeometry(100, 100, 1200, 800)
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        main_layout = QHBoxLayout(central_widget)
        left_panel = QVBoxLayout()
        self.settings_panel = DualWebcamSettingsPanel()
        left_panel.addWidget(self.settings_panel)
        controls_group = QGroupBox("Controls")
        controls_layout = QVBoxLayout(controls_group)
        self.test_button = QPushButton("Test Cameras")
        self.test_button.clicked.connect(self.test_cameras)
        controls_layout.addWidget(self.test_button)
        self.preview_button = QPushButton("Start Preview")
        self.preview_button.clicked.connect(self.toggle_preview)
        controls_layout.addWidget(self.preview_button)
        self.record_button = QPushButton("Start Recording")
        self.record_button.clicked.connect(self.toggle_recording)
        self.record_button.setStyleSheet(
        )
        controls_layout.addWidget(self.record_button)
        left_panel.addWidget(controls_group)
        perf_group = QGroupBox("Performance")
        perf_layout = QVBoxLayout(perf_group)
        self.sync_quality_bar = QProgressBar()
        self.sync_quality_bar.setRange(0, 100)
        self.sync_quality_bar.setValue(0)
        perf_layout.addWidget(QLabel("Sync Quality:"))
        perf_layout.addWidget(self.sync_quality_bar)
        self.frame_count_label = QLabel("Frames: 0")
        perf_layout.addWidget(self.frame_count_label)
        left_panel.addWidget(perf_group)
        left_panel.addStretch()
        preview_layout = QVBoxLayout()
        self.camera1_preview = CameraPreviewWidget(self.camera_indices[0])
        self.camera2_preview = CameraPreviewWidget(self.camera_indices[1])
        preview_layout.addWidget(self.camera1_preview)
        preview_layout.addWidget(self.camera2_preview)
        main_layout.addLayout(left_panel, 1)
        main_layout.addLayout(preview_layout, 2)
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        self.status_bar.showMessage("Ready")
        self.ui_timer = QTimer()
        self.ui_timer.timeout.connect(self.update_ui)
        self.ui_timer.start(100)
    def setup_connections(self):
        self.settings_panel.settings_changed.connect(self.on_settings_changed)
    def on_dual_frame_ready(self, pixmap1: QPixmap, pixmap2: QPixmap):
        try:
            if pixmap1 and not pixmap1.isNull():
                self.camera1_preview.update_preview(pixmap1)
            if pixmap2 and not pixmap2.isNull():
                self.camera2_preview.update_preview(pixmap2)
        except Exception as e:
            logger.error(f"Error updating frame previews: {e}", exc_info=True)
    def apply_initial_settings(self):
        if self.initial_settings:
            self.settings_panel.set_settings(self.initial_settings)
            logger.info(f"Applied initial settings: {self.initial_settings}")
    @performance_timer("camera_test")
    def test_cameras(self):
        logger.info("Testing camera accessibility...")
        self.test_button.setEnabled(False)
        self.status_bar.showMessage("Testing cameras...")
        try:
            settings = self.settings_panel.get_settings()
            camera_indices = [settings["camera1_index"], settings["camera2_index"]]
            success = test_dual_webcam_access(camera_indices)
            if success:
                self.status_bar.showMessage("Camera test successful")
                QMessageBox.information(
                    self, "Camera Test", "Both cameras are accessible!"
                )
                logger.info("Camera test successful")
            else:
                self.status_bar.showMessage("Camera test failed")
                QMessageBox.warning(
                    self,
                    "Camera Test",
                    "Camera test failed. Please check camera connections.",
                )
                logger.warning("Camera test failed")
        except Exception as e:
            error_msg = f"Error testing cameras: {e}"
            logger.error(error_msg, exc_info=True)
            self.status_bar.showMessage("Camera test error")
            QMessageBox.critical(self, "Camera Test Error", error_msg)
        finally:
            self.test_button.setEnabled(True)
    @log_method_entry
    def toggle_preview(self):
        if not self.is_previewing:
            self.start_preview()
        else:
            self.stop_preview()
    @performance_timer("start_preview")
    def start_preview(self):
        try:
            settings = self.settings_panel.get_settings()
            logger.info(f"Starting preview with settings: {settings}")
            self.dual_webcam_capture = DualWebcamCapture(
                camera1_index=settings["camera1_index"],
                camera2_index=settings["camera2_index"],
                width=settings["width"],
                height=settings["height"],
                fps=settings["fps"],
            )
            self.dual_webcam_capture.dual_frame_ready.connect(self.on_dual_frame_ready)
            if self.dual_webcam_capture.start_capture():
                self.is_previewing = True
                self.preview_button.setText("Stop Preview")
                self.status_bar.showMessage("Preview started")
                logger.info("Preview started successfully")
            else:
                error_msg = "Failed to start camera preview"
                logger.error(error_msg)
                QMessageBox.warning(self, "Preview Error", error_msg)
        except Exception as e:
            error_msg = f"Error starting preview: {e}"
            logger.error(error_msg, exc_info=True)
            QMessageBox.critical(self, "Preview Error", error_msg)
    def stop_preview(self):
        try:
            if self.dual_webcam_capture:
                self.dual_webcam_capture.stop_capture()
                self.dual_webcam_capture = None
            self.is_previewing = False
            self.preview_button.setText("Start Preview")
            self.status_bar.showMessage("Preview stopped")
            self.camera1_preview.update_status(False)
            self.camera2_preview.update_status(False)
            logger.info("Preview stopped")
        except Exception as e:
            error_msg = f"Error stopping preview: {e}"
            logger.error(error_msg, exc_info=True)
    def toggle_recording(self):
        if not self.is_recording:
            self.start_recording()
        else:
            self.stop_recording()
    @performance_timer("start_recording")
    def start_recording(self):
        try:
            if not self.is_previewing:
                self.start_preview()
            if self.dual_webcam_capture:
                if self.dual_webcam_capture.start_recording():
                    self.is_recording = True
                    self.record_button.setText("Stop Recording")
                    self.record_button.setStyleSheet(
                    )
                    self.status_bar.showMessage("Recording...")
                    logger.info("Recording started")
                else:
                    QMessageBox.warning(
                        self, "Recording Error", "Failed to start recording"
                    )
        except Exception as e:
            error_msg = f"Error starting recording: {e}"
            logger.error(error_msg, exc_info=True)
            QMessageBox.critical(self, "Recording Error", error_msg)
    def stop_recording(self):
        try:
            if self.dual_webcam_capture:
                self.dual_webcam_capture.stop_recording()
            self.is_recording = False
            self.record_button.setText("Start Recording")
            self.record_button.setStyleSheet(
            )
            self.status_bar.showMessage("Recording stopped")
            logger.info("Recording stopped")
        except Exception as e:
            error_msg = f"Error stopping recording: {e}"
            logger.error(error_msg, exc_info=True)
    def on_settings_changed(self, settings: dict):
        logger.debug(f"Settings changed: {settings}")
        self.camera1_preview.camera_id = settings["camera1_index"]
        self.camera2_preview.camera_id = settings["camera2_index"]
        self.camera1_preview.title_label.setText(f"Camera {settings['camera1_index']}")
        self.camera2_preview.title_label.setText(f"Camera {settings['camera2_index']}")
        if self.is_previewing and not self.is_recording:
            logger.info("Restarting preview with new settings")
            self.stop_preview()
            QTimer.singleShot(500, self.start_preview)
    def update_ui(self):
        try:
            if self.dual_webcam_capture and self.is_previewing:
                frame_data = self.dual_webcam_capture.get_latest_frame()
                if frame_data:
                    self.frame_count += 1
                    self.frame_count_label.setText(f"Frames: {self.frame_count}")
                    sync_quality = int(frame_data.sync_quality * 100)
                    self.sync_quality_bar.setValue(sync_quality)
                    camera1_fps = self.dual_webcam_capture.get_camera_fps(1)
                    camera2_fps = self.dual_webcam_capture.get_camera_fps(2)
                    self.camera1_preview.update_status(
                        frame_data.camera1_frame is not None, camera1_fps
                    )
                    self.camera2_preview.update_status(
                        frame_data.camera2_frame is not None, camera2_fps
                    )
        except Exception as e:
            logger.error(f"Error updating UI: {e}", exc_info=True)
    def closeEvent(self, event):
        logger.info("DualWebcamMainWindow closing")
        if self.is_recording:
            self.stop_recording()
        if self.is_previewing:
            self.stop_preview()
        if hasattr(self, "ui_timer"):
            self.ui_timer.stop()
        event.accept()
        logger.info("DualWebcamMainWindow closed")