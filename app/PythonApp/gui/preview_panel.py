from PyQt5.QtCore import Qt, QTimer
from PyQt5.QtGui import QPixmap
from PyQt5.QtWidgets import (
    QHBoxLayout,
    QLabel,
    QPushButton,
    QTabWidget,
    QVBoxLayout,
    QWidget,
)
try:
    from ..utils.logging_config import get_logger
    logger = get_logger(__name__)
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
class PreviewPanel(QTabWidget):
    def __init__(self, parent=None):
        super().__init__(parent)
        self.init_ui()
        self.ir_preview_timer = QTimer()
        self.ir_preview_timer.timeout.connect(self.update_ir_preview)
        self.ir_preview_active = False
    def init_ui(self):
        self.device1_widget = self.create_device_tab("Device 1")
        self.addTab(self.device1_widget, "Device 1")
        self.device2_widget = self.create_device_tab("Device 2")
        self.addTab(self.device2_widget, "Device 2")
        self.webcam_widget = self.create_webcam_tab("PC Webcam")
        self.addTab(self.webcam_widget, "PC RGB Camera")
        self.ir_camera_widget = self.create_ir_camera_tab("PC IR Camera")
        self.addTab(self.ir_camera_widget, "PC IR Camera")
        self.rgb_labels = [self.device1_widget.rgb_label, self.device2_widget.rgb_label]
        self.thermal_labels = [
            self.device1_widget.thermal_label,
            self.device2_widget.thermal_label,
        ]
        self.webcam_label = self.webcam_widget.webcam_label
        self.ir_camera_label = self.ir_camera_widget.ir_label
    def create_device_tab(self, device_name):
        device_widget = QWidget()
        device_layout = QVBoxLayout(device_widget)
        rgb_label = QLabel("RGB Camera Feed")
        rgb_label.setMinimumSize(320, 240)
        rgb_label.setStyleSheet(
            "background-colour: black; colour: white; border: 1px solid grey;"
        )
        rgb_label.setAlignment(Qt.AlignCenter)
        device_layout.addWidget(rgb_label)
        thermal_label = QLabel("Thermal Camera Feed")
        thermal_label.setMinimumSize(320, 240)
        thermal_label.setStyleSheet(
            "background-colour: black; colour: white; border: 1px solid grey;"
        )
        thermal_label.setAlignment(Qt.AlignCenter)
        device_layout.addWidget(thermal_label)
        device_layout.addStretch(1)
        device_widget.rgb_label = rgb_label
        device_widget.thermal_label = thermal_label
        return device_widget
    def create_webcam_tab(self, device_name):
        webcam_widget = QWidget()
        webcam_layout = QVBoxLayout(webcam_widget)
        webcam_label = QLabel("PC RGB Camera Feed")
        webcam_label.setMinimumSize(640, 480)
        webcam_label.setStyleSheet(
            "background-colour: black; colour: white; border: 1px solid grey;"
        )
        webcam_label.setAlignment(Qt.AlignCenter)
        webcam_layout.addWidget(webcam_label)
        webcam_layout.addStretch(1)
        webcam_widget.webcam_label = webcam_label
        return webcam_widget
    def create_ir_camera_tab(self, device_name):
        ir_widget = QWidget()
        ir_layout = QVBoxLayout(ir_widget)
        control_layout = QHBoxLayout()
        self.ir_start_btn = QPushButton("Start IR Preview")
        self.ir_start_btn.clicked.connect(self.toggle_ir_preview)
        control_layout.addWidget(self.ir_start_btn)
        self.ir_capture_btn = QPushButton("Capture IR")
        self.ir_capture_btn.clicked.connect(self.capture_ir_frame)
        control_layout.addWidget(self.ir_capture_btn)
        control_layout.addStretch()
        ir_layout.addLayout(control_layout)
        ir_label = QLabel('PC IR Camera Feed\n\nClick "Start IR Preview" to begin')
        ir_label.setMinimumSize(640, 480)
        ir_label.setStyleSheet(
            "background-colour: #1a1a2e; colour: #eee; border: 2px solid #16213e; "
            "border-radius: 8px; font-size: 14px;"
        )
        ir_label.setAlignment(Qt.AlignCenter)
        ir_layout.addWidget(ir_label)
        self.ir_status_label = QLabel("IR Camera: Ready")
        self.ir_status_label.setStyleSheet("colour: #666; padding: 5px;")
        ir_layout.addWidget(self.ir_status_label)
        ir_layout.addStretch(1)
        ir_widget.ir_label = ir_label
        return ir_widget
    def toggle_ir_preview(self):
        if not self.ir_preview_active:
            self.start_ir_preview()
        else:
            self.stop_ir_preview()
    def start_ir_preview(self):
        try:
            self.ir_preview_active = True
            self.ir_start_btn.setText("Stop IR Preview")
            self.ir_camera_label.setText("Starting IR camera preview...")
            self.ir_status_label.setText("IR Camera: Starting...")
            self.ir_preview_timer.start(200)
            logger.info("IR camera preview started")
        except Exception as e:
            logger.error(f"Failed to start IR preview: {e}")
            self.ir_status_label.setText(f"IR Camera: Error - {str(e)}")
    def stop_ir_preview(self):
        try:
            self.ir_preview_active = False
            self.ir_start_btn.setText("Start IR Preview")
            self.ir_preview_timer.stop()
            self.ir_camera_label.clear()
            self.ir_camera_label.setText(
                'PC IR Camera Feed\n\nClick "Start IR Preview" to begin'
            )
            self.ir_status_label.setText("IR Camera: Stopped")
            logger.info("IR camera preview stopped")
        except Exception as e:
            logger.error(f"Failed to stop IR preview: {e}")
    def update_ir_preview(self):
        try:
            pixmap = self.generate_thermal_preview()
            if pixmap:
                self.ir_camera_label.setPixmap(pixmap)
                
                # Check if we're getting real thermal data
                real_data_available = self._get_real_thermal_frame() is not None
                if real_data_available:
                    self.ir_status_label.setText("IR Camera: Active (Real Data)")
                    self.ir_status_label.setStyleSheet("color: #00aa00; padding: 5px;")  # Green for real data
                else:
                    self.ir_status_label.setText("IR Camera: Active (Simulated - Waiting for Android Device)")
                    self.ir_status_label.setStyleSheet("color: #aa6600; padding: 5px;")  # Orange for simulated
        except Exception as e:
            logger.error(f"Error updating IR preview: {e}")
            self.ir_status_label.setText(f"IR Camera: Error - {str(e)}")
            self.ir_status_label.setStyleSheet("color: #aa0000; padding: 5px;")  # Red for error
    def generate_thermal_preview(self):
        """Generate thermal camera preview from real Android device data or fallback to simulation."""
        try:
            # Try to get real thermal data from connected Android devices first
            real_frame = self._get_real_thermal_frame()
            if real_frame is not None:
                return real_frame
            
            # Fallback to simulation if no real data available
            return self._generate_simulated_thermal_frame()
            
        except Exception as e:
            logger.error(f"Error generating thermal preview: {e}")
            return None
    
    def _get_real_thermal_frame(self):
        """Attempt to get real thermal camera frame from connected Android devices."""
        try:
            # Check if we have access to the main controller with Android device manager
            if hasattr(self.parent(), 'android_device_manager') and self.parent().android_device_manager:
                device_manager = self.parent().android_device_manager
                connected_devices = device_manager.get_connected_devices()
                
                for device_id, device_info in connected_devices.items():
                    if 'thermal' in device_info.get('capabilities', []):
                        # Try to get latest thermal frame from this device
                        thermal_frame = device_manager.get_latest_thermal_frame(device_id)
                        if thermal_frame is not None:
                            return self._convert_thermal_data_to_pixmap(thermal_frame)
            
            # Alternative: Check if there's a PC server with thermal data
            if hasattr(self.parent(), 'pc_server') and self.parent().pc_server:
                pc_server = self.parent().pc_server
                devices = pc_server.get_connected_devices()
                
                for device_id, device in devices.items():
                    if 'thermal' in device.capabilities:
                        # This would need to be implemented in the PC server
                        # thermal_data = pc_server.get_latest_thermal_data(device_id)
                        # if thermal_data:
                        #     return self._convert_thermal_data_to_pixmap(thermal_data)
                        pass
            
            return None
            
        except Exception as e:
            logger.debug(f"Could not get real thermal frame: {e}")
            return None
    
    def _convert_thermal_data_to_pixmap(self, thermal_data):
        """Convert thermal data array to QPixmap for display."""
        try:
            import numpy as np
            from PyQt5.QtGui import QImage
            
            if isinstance(thermal_data, np.ndarray):
                # Normalize thermal data to 0-255 range
                if thermal_data.dtype != np.uint8:
                    thermal_normalized = ((thermal_data - thermal_data.min()) / 
                                        (thermal_data.max() - thermal_data.min()) * 255).astype(np.uint8)
                else:
                    thermal_normalized = thermal_data
                
                # Apply thermal colormap (heat map)
                colored_frame = self._apply_thermal_colormap(thermal_normalized)
                
                h, w, ch = colored_frame.shape
                bytes_per_line = ch * w
                q_image = QImage(colored_frame.data, w, h, bytes_per_line, QImage.Format_RGB888)
                pixmap = QPixmap.fromImage(q_image)
                return pixmap.scaled(400, 300, Qt.KeepAspectRatio, Qt.SmoothTransformation)
            
            return None
            
        except Exception as e:
            logger.error(f"Error converting thermal data to pixmap: {e}")
            return None
    
    def _apply_thermal_colormap(self, thermal_array):
        """Apply thermal colormap to grayscale thermal data."""
        import numpy as np
        
        # Create RGB frame
        height, width = thermal_array.shape
        colored_frame = np.zeros((height, width, 3), dtype=np.uint8)
        
        # Apply heat map coloring: blue -> purple -> red -> yellow -> white
        for y in range(height):
            for x in range(width):
                intensity = thermal_array[y, x]
                
                if intensity < 51:  # Blue to purple (cold)
                    colored_frame[y, x] = [intensity * 5, 0, 255]
                elif intensity < 102:  # Purple to red
                    factor = intensity - 51
                    colored_frame[y, x] = [255, 0, 255 - factor * 5]
                elif intensity < 153:  # Red to orange
                    factor = intensity - 102
                    colored_frame[y, x] = [255, factor * 5, 0]
                elif intensity < 204:  # Orange to yellow
                    factor = intensity - 153
                    colored_frame[y, x] = [255, 255, factor * 5]
                else:  # Yellow to white (hot)
                    factor = intensity - 204
                    colored_frame[y, x] = [255, 255, 255]
        
        return colored_frame
    
    def _generate_simulated_thermal_frame(self):
        """Generate simulated thermal frame when no real data is available."""
        try:
            import time
            import numpy as np
            from PyQt5.QtGui import QImage
            
            width, height = 320, 240
            frame = np.zeros((height, width, 3), dtype=np.uint8)
            time_factor = time.time() * 2
            
            for y in range(height):
                for x in range(width):
                    center_x, center_y = width // 2, height // 2
                    distance = np.sqrt((x - center_x) ** 2 + (y - center_y) ** 2)
                    intensity = int(127 + 127 * np.sin(distance * 0.1 + time_factor))
                    intensity = max(0, min(255, intensity))
                    
                    if intensity < 85:
                        frame[y, x] = [intensity * 3, 0, 255 - intensity * 2]
                    elif intensity < 170:
                        frame[y, x] = [
                            255,
                            (intensity - 85) * 3,
                            max(0, 255 - (intensity - 85) * 4),
                        ]
                    else:
                        frame[y, x] = [255, 255, (intensity - 170) * 3]
            
            # Add simulated heat spots
            import random
            for _ in range(3):
                hot_x = random.randint(50, width - 50)
                hot_y = random.randint(50, height - 50)
                for dy in range(-15, 16):
                    for dx in range(-15, 16):
                        if 0 <= hot_x + dx < width and 0 <= hot_y + dy < height:
                            if dx * dx + dy * dy <= 225:
                                frame[hot_y + dy, hot_x + dx] = [255, 255, 200]
            
            h, w, ch = frame.shape
            bytes_per_line = ch * w
            q_image = QImage(frame.data, w, h, bytes_per_line, QImage.Format_RGB888)
            pixmap = QPixmap.fromImage(q_image)
            return pixmap.scaled(400, 300, Qt.KeepAspectRatio, Qt.SmoothTransformation)
            
        except Exception as e:
            logger.error(f"Error generating simulated thermal frame: {e}")
            return None
    def capture_ir_frame(self):
        try:
            if self.ir_preview_active:
                from PyQt5.QtWidgets import QMessageBox
                QMessageBox.information(
                    self,
                    "IR Capture",
                    "IR frame captured!\n\n(Simulated capture - would save thermal data in real implementation)",
                )
                logger.info("IR frame capture requested")
            else:
                from PyQt5.QtWidgets import QMessageBox
                QMessageBox.warning(self, "IR Capture", "Please start IR preview first")
        except Exception as e:
            logger.error(f"Error capturing IR frame: {e}")
    def update_rgb_feed(self, device_index, pixmap):
        if 0 <= device_index < len(self.rgb_labels):
            self.rgb_labels[device_index].setPixmap(pixmap)
    def update_thermal_feed(self, device_index, pixmap):
        if 0 <= device_index < len(self.thermal_labels):
            self.thermal_labels[device_index].setPixmap(pixmap)
    def update_webcam_feed(self, pixmap):
        if hasattr(self, "webcam_label") and self.webcam_label:
            self.webcam_label.setPixmap(pixmap)
    def clear_webcam_feed(self):
        if hasattr(self, "webcam_label") and self.webcam_label:
            self.webcam_label.clear()
            self.webcam_label.setText("PC RGB Camera Feed")
    def clear_ir_feed(self):
        if hasattr(self, "ir_camera_label") and self.ir_camera_label:
            self.stop_ir_preview()
    def clear_feed(self, device_index, feed_type="both"):
        if 0 <= device_index < len(self.rgb_labels):
            if feed_type in ["rgb", "both"]:
                self.rgb_labels[device_index].clear()
                self.rgb_labels[device_index].setText("RGB Camera Feed")
            if feed_type in ["thermal", "both"]:
                self.thermal_labels[device_index].clear()
                self.thermal_labels[device_index].setText("Thermal Camera Feed")
    def clear_all_feeds(self):
        for i in range(len(self.rgb_labels)):
            self.clear_feed(i, "both")
        self.clear_webcam_feed()
        self.clear_ir_feed()
    def set_device_tab_active(self, device_index):
        if 0 <= device_index < self.count():
            self.setCurrentIndex(device_index)
    def get_active_device_index(self):
        return self.currentIndex()
    def get_rgb_label(self, device_index):
        if 0 <= device_index < len(self.rgb_labels):
            return self.rgb_labels[device_index]
        return None
    def get_thermal_label(self, device_index):
        if 0 <= device_index < len(self.thermal_labels):
            return self.thermal_labels[device_index]
        return None