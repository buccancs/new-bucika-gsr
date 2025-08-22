import os
import sys
import time
from PyQt5.QtCore import Qt, pyqtSignal
from PyQt5.QtWidgets import (
    QCheckBox,
    QDialog,
    QFileDialog,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QListWidget,
    QListWidgetItem,
    QMessageBox,
    QProgressBar,
    QPushButton,
    QSlider,
    QTabWidget,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from .calibration.calibration_manager import CalibrationManager
from .calibration.calibration_result import CalibrationResult
class CalibrationDialog(QDialog):
    calibration_completed = pyqtSignal(str, CalibrationResult)
    overlay_toggled = pyqtSignal(str, bool)
    def __init__(self, device_server, parent=None):
        super().__init__(parent)
        self.device_server = device_server
        self.calibration_manager = CalibrationManager()
        self.current_session = None
        self.device_results = {}
        self.setWindowTitle("Camera Calibration - Milestone 3.4")
        self.setModal(True)
        self.resize(800, 600)
        self.setup_ui()
        self.connect_signals()
    def setup_ui(self):
        layout = QVBoxLayout(self)
        self.create_instructions_section(layout)
        self.create_session_controls(layout)
        self.create_capture_section(layout)
        self.create_computation_section(layout)
        self.create_results_section(layout)
        self.create_action_buttons(layout)
    def create_instructions_section(self, parent_layout):
        instructions_group = QGroupBox("Calibration Instructions")
        instructions_layout = QVBoxLayout(instructions_group)
        instructions_text = """
        <b>Camera Calibration Procedure:</b><br><br>
        1. Place the calibration pattern (chessboard) in view of both RGB and thermal cameras<br>
        2. Capture at least 5-10 images from different angles and positions<br>
        3. Ensure the pattern fills about half the frame for some shots<br>
        4. Vary the pattern orientation and distance for better accuracy<br>
        5. Click 'Compute Calibration' when you have enough frames<br><br>
        <b>Tips:</b> For thermal cameras, ensure temperature contrast in the pattern
        """