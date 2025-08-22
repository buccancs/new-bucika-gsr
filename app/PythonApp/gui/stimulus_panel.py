import os
from PyQt5.QtCore import Qt, pyqtSignal
from PyQt5.QtWidgets import (
    QApplication,
    QComboBox,
    QFileDialog,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QPushButton,
    QSlider,
)
class StimulusControlPanel(QGroupBox):
    file_loaded = pyqtSignal(str)
    play_requested = pyqtSignal()
    pause_requested = pyqtSignal()
    seek_requested = pyqtSignal(int)
    screen_changed = pyqtSignal(int)
    start_recording_play_requested = pyqtSignal()
    mark_event_requested = pyqtSignal()
    def __init__(self, parent=None):
        super().__init__("Stimulus Controls", parent)
        self.parent_window = parent
        self.current_file = None
        self.init_ui()
    def init_ui(self):
        stim_layout = QHBoxLayout(self)
        self.stim_file_path = QLineEdit()
        self.stim_file_path.setPlaceholderText("No file loaded")
        self.stim_file_path.setReadOnly(True)
        stim_layout.addWidget(self.stim_file_path)
        self.browse_btn = QPushButton("Load Stimulus...")
        self.browse_btn.clicked.connect(self.browse_stimulus_file)
        stim_layout.addWidget(self.browse_btn)
        self.play_btn = QPushButton("Play")
        self.play_btn.setEnabled(False)
        self.play_btn.clicked.connect(self.handle_play)
        stim_layout.addWidget(self.play_btn)
        self.pause_btn = QPushButton("Pause")
        self.pause_btn.setEnabled(False)
        self.pause_btn.clicked.connect(self.handle_pause)
        stim_layout.addWidget(self.pause_btn)
        self.timeline_slider = QSlider(Qt.Horizontal)
        self.timeline_slider.setRange(0, 100)
        self.timeline_slider.setValue(0)
        self.timeline_slider.sliderMoved.connect(self.handle_seek)
        stim_layout.addWidget(self.timeline_slider)
        self.start_recording_play_btn = QPushButton("Start Recording and Play")
        self.start_recording_play_btn.setEnabled(False)
        self.start_recording_play_btn.setStyleSheet(
            "QPushButton { background-colour: #4CAF50; colour: white; font-weight: bold; }"
        )
        self.start_recording_play_btn.clicked.connect(self.handle_start_recording_play)
        stim_layout.addWidget(self.start_recording_play_btn)
        self.mark_event_btn = QPushButton("Mark Event")
        self.mark_event_btn.setEnabled(False)
        self.mark_event_btn.setStyleSheet(
            "QPushButton { background-colour: #FF9800; colour: white; font-weight: bold; }"
        )
        self.mark_event_btn.clicked.connect(self.handle_mark_event)
        stim_layout.addWidget(self.mark_event_btn)
        screen_label = QLabel("Output Screen:")
        stim_layout.addWidget(screen_label)
        self.screen_combo = QComboBox()
        self.populate_screen_combo()
        self.screen_combo.currentIndexChanged.connect(self.handle_screen_change)
        stim_layout.addWidget(self.screen_combo)
    def populate_screen_combo(self):
        self.screen_combo.clear()
        screens = QApplication.screens()
        for i, screen in enumerate(screens):
            screen_name = screen.name() or f"Screen {i + 1}"
            screen_info = (
                f"{screen_name} ({screen.size().width()}x{screen.size().height()})"
            )
            self.screen_combo.addItem(screen_info)
        if len(screens) > 1:
            self.screen_combo.setCurrentIndex(1)
    def browse_stimulus_file(self):
        fname, _ = QFileDialog.getOpenFileName(
            self,
            "Select Stimulus Video",
            "",
            "Video Files (*.mp4 *.avi *.mov *.mkv *.wmv);;All Files (*)",
        )
        if fname:
            self.load_file(fname)
    def load_file(self, file_path):
        self.current_file = file_path
        self.stim_file_path.setText(file_path)
        self.play_btn.setEnabled(True)
        self.pause_btn.setEnabled(True)
        self.timeline_slider.setValue(0)
        self.file_loaded.emit(file_path)
        if self.parent_window and hasattr(self.parent_window, "statusBar"):
            self.parent_window.statusBar().showMessage(
                f"Loaded stimulus: {os.path.basename(file_path)}"
            )
    def handle_play(self):
        self.play_requested.emit()
        if self.parent_window and hasattr(self.parent_window, "statusBar"):
            self.parent_window.statusBar().showMessage("Play stimulus (simulation)")
    def handle_pause(self):
        self.pause_requested.emit()
        if self.parent_window and hasattr(self.parent_window, "statusBar"):
            self.parent_window.statusBar().showMessage("Pause stimulus (simulation)")
    def handle_seek(self, value):
        self.seek_requested.emit(value)
        if self.parent_window and hasattr(self.parent_window, "statusBar"):
            self.parent_window.statusBar().showMessage(f"Seek to {value}% (simulation)")
    def handle_screen_change(self, index):
        self.screen_changed.emit(index)
    def handle_start_recording_play(self):
        self.start_recording_play_requested.emit()
        if self.parent_window and hasattr(self.parent_window, "statusBar"):
            self.parent_window.statusBar().showMessage(
                "Starting synchronised recording and stimulus playback..."
            )
    def handle_mark_event(self):
        self.mark_event_requested.emit()
        if self.parent_window and hasattr(self.parent_window, "statusBar"):
            self.parent_window.statusBar().showMessage("Event marker added")
    def get_current_file(self):
        return self.current_file
    def get_timeline_position(self):
        return self.timeline_slider.value()
    def set_timeline_position(self, position):
        self.timeline_slider.setValue(position)
    def get_selected_screen(self):
        return self.screen_combo.currentIndex()
    def set_selected_screen(self, index):
        if 0 <= index < self.screen_combo.count():
            self.screen_combo.setCurrentIndex(index)
    def enable_controls(self, enabled=True):
        self.browse_btn.setEnabled(enabled)
        self.play_btn.setEnabled(enabled and self.current_file is not None)
        self.pause_btn.setEnabled(enabled and self.current_file is not None)
        self.timeline_slider.setEnabled(enabled)
        self.screen_combo.setEnabled(enabled)
        self.start_recording_play_btn.setEnabled(
            enabled and self.current_file is not None
        )
        self.mark_event_btn.setEnabled(False)
    def reset_controls(self):
        self.current_file = None
        self.stim_file_path.clear()
        self.stim_file_path.setPlaceholderText("No file loaded")
        self.play_btn.setEnabled(False)
        self.pause_btn.setEnabled(False)
        self.timeline_slider.setValue(0)
        self.populate_screen_combo()
        self.start_recording_play_btn.setEnabled(False)
        self.mark_event_btn.setEnabled(False)
    def refresh_screens(self):
        current_selection = self.screen_combo.currentIndex()
        self.populate_screen_combo()
        if current_selection < self.screen_combo.count():
            self.screen_combo.setCurrentIndex(current_selection)
    def set_experiment_active(self, active: bool):
        if active:
            self.start_recording_play_btn.setEnabled(False)
            self.mark_event_btn.setEnabled(True)
            self.browse_btn.setEnabled(False)
        else:
            self.start_recording_play_btn.setEnabled(self.current_file is not None)
            self.mark_event_btn.setEnabled(False)
            self.browse_btn.setEnabled(True)
    def update_timeline_from_position(self, position_ms: int, duration_ms: int):
        if duration_ms > 0:
            progress = int(position_ms / duration_ms * 100)
            self.timeline_slider.setValue(progress)