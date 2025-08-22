import logging
import os
import sys
import time
from dataclasses import dataclass, field
from typing import Callable, Dict, List, Optional
from PyQt5.QtCore import QRect, Qt, QTimer, QUrl
from PyQt5.QtGui import QColor, QFont, QPalette, QPixmap
from PyQt5.QtMultimedia import QMediaContent, QMediaPlayer
from PyQt5.QtMultimediaWidgets import QVideoWidget
from PyQt5.QtWidgets import (
    QApplication,
    QDesktopWidget,
    QLabel,
    QMainWindow,
    QVBoxLayout,
    QWidget,
)
@dataclass
class MonitorInfo:
    monitor_id: int
    name: str
    geometry: QRect
    is_primary: bool = False
    dpi: float = 96.0
@dataclass
class StimulusConfig:
    stimulus_type: str = "video"
    content_path: Optional[str] = None
    duration_ms: int = 5000
    timing_precision_us: int = 1000
    audio_enabled: bool = True
    fullscreen: bool = True
    monitor_id: int = 0
    background_color: str = "#000000"
    text_content: Optional[str] = None
    font_size: int = 48
    synchronization_markers: List[int] = field(default_factory=list)
@dataclass
class StimulusEvent:
    event_type: str
    timestamp: float
    monitor_id: int
    stimulus_config: StimulusConfig
    duration_actual_ms: Optional[float] = None
class StimulusWindow(QMainWindow):
    def __init__(self, monitor_info: MonitorInfo, config: StimulusConfig):
        super().__init__()
        self.monitor_info = monitor_info
        self.config = config
        self.media_player: Optional[QMediaPlayer] = None
        self.video_widget: Optional[QVideoWidget] = None
        self.setup_window()
        self.setup_content()
    def setup_window(self):
        self.setWindowTitle("Stimulus Presentation")
        self.setWindowFlags(Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint)
        self.setGeometry(self.monitor_info.geometry)
        palette = QPalette()
        palette.setColor(QPalette.Window, QColor(self.config.background_color))
        self.setPalette(palette)
        if self.config.fullscreen:
            self.showFullScreen()
    def setup_content(self):
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        layout = QVBoxLayout(central_widget)
        layout.setContentsMargins(0, 0, 0, 0)
        if self.config.stimulus_type == "video" and self.config.content_path:
            self.setup_video_content(layout)
        elif self.config.stimulus_type == "image" and self.config.content_path:
            self.setup_image_content(layout)
        elif self.config.stimulus_type == "text":
            self.setup_text_content(layout)
        elif self.config.stimulus_type == "pattern":
            self.setup_pattern_content(layout)
    def setup_video_content(self, layout):
        self.video_widget = QVideoWidget()
        self.media_player = QMediaPlayer()
        self.media_player.setVideoOutput(self.video_widget)
        if os.path.exists(self.config.content_path):
            media_content = QMediaContent(QUrl.fromLocalFile(self.config.content_path))
            self.media_player.setMedia(media_content)
        layout.addWidget(self.video_widget)
    def setup_image_content(self, layout):
        label = QLabel()
        if os.path.exists(self.config.content_path):
            pixmap = QPixmap(self.config.content_path)
            scaled_pixmap = pixmap.scaled(
                self.monitor_info.geometry.size(),
                Qt.KeepAspectRatio,
                Qt.SmoothTransformation,
            )
            label.setPixmap(scaled_pixmap)
        label.setAlignment(Qt.AlignCenter)
        layout.addWidget(label)
    def setup_text_content(self, layout):
        label = QLabel(self.config.text_content or "Stimulus Text")
        label.setAlignment(Qt.AlignCenter)
        font = QFont()
        font.setPointSize(self.config.font_size)
        label.setFont(font)
        label.setStyleSheet("colour: white;")
        layout.addWidget(label)
    def setup_pattern_content(self, layout):
        label = QLabel("* TEST PATTERN *")
        label.setAlignment(Qt.AlignCenter)
        font = QFont()
        font.setPointSize(72)
        font.setBold(True)
        label.setFont(font)
        label.setStyleSheet("colour: white;")
        layout.addWidget(label)
    def start_presentation(self):
        if self.media_player and self.config.stimulus_type == "video":
            if self.config.audio_enabled:
                self.media_player.setMuted(False)
            else:
                self.media_player.setMuted(True)
            self.media_player.play()
    def stop_presentation(self):
        if self.media_player:
            self.media_player.stop()
class StimulusManager:
    def __init__(self, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.is_initialized = False
        self.available_monitors: List[MonitorInfo] = []
        self.stimulus_windows: Dict[int, StimulusWindow] = {}
        self.presentation_timers: Dict[int, QTimer] = {}
        self.event_callbacks: List[Callable[[StimulusEvent], None]] = []
        self.presentation_history: List[StimulusEvent] = []
        self.high_precision_timer = QTimer()
        self.high_precision_timer.setSingleShot(True)
        self.synchronization_offset_ms = 0.0
        self.logger.info("StimulusManager initialized")
    def initialize(self) -> bool:
        try:
            self.logger.info("Initializing StimulusManager...")
            app = QApplication.instance()
            if not app:
                self.logger.error("No QApplication instance found")
                return False
            desktop = QDesktopWidget()
            screens = app.screens()
            self.available_monitors = []
            for i, screen in enumerate(screens):
                monitor_info = MonitorInfo(
                    monitor_id=i,
                    name=screen.name(),
                    geometry=screen.geometry(),
                    is_primary=i == desktop.primaryScreen(),
                    dpi=screen.logicalDotsPerInch(),
                )
                self.available_monitors.append(monitor_info)
                self.logger.info(
                    f"Monitor {i}: {monitor_info.name} ({monitor_info.geometry.width()}x{monitor_info.geometry.height()}) {'[PRIMARY]' if monitor_info.is_primary else ''}"
                )
            self.is_initialized = True
            self.logger.info(
                f"StimulusManager initialized with {len(self.available_monitors)} monitors"
            )
            return True
        except Exception as e:
            self.logger.error(f"Failed to initialize StimulusManager: {e}")
            return False
    def get_monitor_count(self) -> int:
        return len(self.available_monitors)
    def get_monitor_info(self, monitor_id: int) -> Optional[MonitorInfo]:
        if 0 <= monitor_id < len(self.available_monitors):
            return self.available_monitors[monitor_id]
        return None
    def get_primary_monitor_id(self) -> int:
        for monitor in self.available_monitors:
            if monitor.is_primary:
                return monitor.monitor_id
        return 0
    def get_secondary_monitor_id(self) -> Optional[int]:
        for monitor in self.available_monitors:
            if not monitor.is_primary:
                return monitor.monitor_id
        return None
    def present_stimulus(self, config: StimulusConfig) -> bool:
        try:
            if not self.is_initialized:
                self.logger.error("StimulusManager not initialized")
                return False
            if config.monitor_id >= len(self.available_monitors):
                self.logger.error(f"Invalid monitor ID: {config.monitor_id}")
                return False
            monitor_info = self.available_monitors[config.monitor_id]
            self.logger.info(
                f"Presenting {config.stimulus_type} stimulus on monitor {config.monitor_id}"
            )
            stimulus_window = StimulusWindow(monitor_info, config)
            self.stimulus_windows[config.monitor_id] = stimulus_window
            stimulus_window.show()
            start_time = time.time()
            stimulus_window.start_presentation()
            event = StimulusEvent(
                event_type="stimulus_start",
                timestamp=start_time,
                monitor_id=config.monitor_id,
                stimulus_config=config,
            )
            self.presentation_history.append(event)
            for callback in self.event_callbacks:
                try:
                    callback(event)
                except Exception as e:
                    self.logger.error(f"Error in event callback: {e}")
            if config.duration_ms > 0:
                timer = QTimer()
                timer.setSingleShot(True)
                timer.timeout.connect(
                    lambda: self._stop_stimulus_presentation(
                        config.monitor_id, start_time
                    )
                )
                timer.start(config.duration_ms)
                self.presentation_timers[config.monitor_id] = timer
            return True
        except Exception as e:
            self.logger.error(f"Error presenting stimulus: {e}")
            return False
    def stop_stimulus(self, monitor_id: int) -> bool:
        try:
            if monitor_id in self.stimulus_windows:
                self._stop_stimulus_presentation(monitor_id, time.time())
                return True
            else:
                self.logger.warning(f"No active stimulus on monitor {monitor_id}")
                return False
        except Exception as e:
            self.logger.error(f"Error stopping stimulus: {e}")
            return False
    def stop_all_stimuli(self) -> None:
        try:
            monitor_ids = list(self.stimulus_windows.keys())
            for monitor_id in monitor_ids:
                self.stop_stimulus(monitor_id)
        except Exception as e:
            self.logger.error(f"Error stopping all stimuli: {e}")
    def present_synchronized_stimuli(self, configs: List[StimulusConfig]) -> bool:
        try:
            if not configs:
                return False
            self.logger.info(f"Presenting {len(configs)} synchronised stimuli")
            windows = []
            for config in configs:
                if config.monitor_id >= len(self.available_monitors):
                    self.logger.error(f"Invalid monitor ID: {config.monitor_id}")
                    continue
                monitor_info = self.available_monitors[config.monitor_id]
                window = StimulusWindow(monitor_info, config)
                window.show()
                windows.append((window, config))
                self.stimulus_windows[config.monitor_id] = window
            start_time = time.time()
            for window, config in windows:
                window.start_presentation()
                event = StimulusEvent(
                    event_type="synchronized_stimulus_start",
                    timestamp=start_time,
                    monitor_id=config.monitor_id,
                    stimulus_config=config,
                )
                self.presentation_history.append(event)
            max_duration = max(
                config.duration_ms for config in configs if config.duration_ms > 0
            )
            if max_duration > 0:
                timer = QTimer()
                timer.setSingleShot(True)
                timer.timeout.connect(lambda: self.stop_all_stimuli())
                timer.start(max_duration)
            return True
        except Exception as e:
            self.logger.error(f"Error presenting synchronised stimuli: {e}")
            return False
    def add_event_callback(self, callback: Callable[[StimulusEvent], None]) -> None:
        self.event_callbacks.append(callback)
    def get_presentation_history(self) -> List[StimulusEvent]:
        return self.presentation_history.copy()
    def _stop_stimulus_presentation(self, monitor_id: int, start_time: float) -> None:
        try:
            if monitor_id in self.stimulus_windows:
                window = self.stimulus_windows[monitor_id]
                window.stop_presentation()
                window.close()
                del self.stimulus_windows[monitor_id]
                if monitor_id in self.presentation_timers:
                    self.presentation_timers[monitor_id].stop()
                    del self.presentation_timers[monitor_id]
                stop_time = time.time()
                duration_ms = (stop_time - start_time) * 1000
                event = StimulusEvent(
                    event_type="stimulus_stop",
                    timestamp=stop_time,
                    monitor_id=monitor_id,
                    stimulus_config=None,
                    duration_actual_ms=duration_ms,
                )
                self.presentation_history.append(event)
                for callback in self.event_callbacks:
                    try:
                        callback(event)
                    except Exception as e:
                        self.logger.error(f"Error in stop event callback: {e}")
                self.logger.info(
                    f"Stopped stimulus on monitor {monitor_id} (duration: {duration_ms:.1f}ms)"
                )
        except Exception as e:
            self.logger.error(f"Error stopping stimulus presentation: {e}")
    def cleanup(self) -> None:
        try:
            self.logger.info("Cleaning up StimulusManager...")
            self.stop_all_stimuli()
            for timer in self.presentation_timers.values():
                timer.stop()
            self.presentation_timers.clear()
            self.stimulus_windows.clear()
            self.event_callbacks.clear()
            self.is_initialized = False
            self.logger.info("StimulusManager cleanup completed")
        except Exception as e:
            self.logger.error(f"Error during cleanup: {e}")
if __name__ == "__main__":
    app = QApplication(sys.argv)
    manager = StimulusManager()
    if manager.initialize():
        print(f"Found {manager.get_monitor_count()} monitors")
        config = StimulusConfig(stimulus_type="test_pattern", duration_ms=3000)
        manager.present_stimulus(config)
    manager.cleanup()