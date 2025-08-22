import sys
from PyQt5.QtCore import QObject
from PyQt5.QtWidgets import QApplication
from ..network.device_server import JsonSocketServer
from ..session.session_logger import get_session_logger
from ..session.session_manager import SessionManager
from ..utils.logging_config import get_logger
from .gui.main_window import MainWindow
from .webcam.webcam_capture import WebcamCapture
class Application(QObject):
    def __init__(self):
        super().__init__()
        self.logger = get_logger(__name__)
        self.session_manager = None
        self.json_server = None
        self.webcam_capture = None
        self.main_window = None
        self._create_services()
        self.logger.info("application initialized")
    def _create_services(self):
        try:
            self.session_manager = SessionManager()
            self.json_server = JsonSocketServer(session_manager=self.session_manager)
            self.webcam_capture = WebcamCapture()
        except Exception as e:
            self.logger.error(f"failed to create services: {e}")
            raise
    def create_main_window(self):
        try:
            self.main_window = MainWindow()
            self.logger.info("Created main window")
            return self.main_window
        except Exception as e:
            self.logger.error(f"failed to create main window: {e}")
            raise
    def run(self):
        try:
            main_window = self.create_main_window()
            main_window.show()
            self.logger.info("application started")
            return main_window
        except Exception as e:
            self.logger.error(f"failed to run application: {e}")
            raise
    def cleanup(self):
        try:
            if self.json_server:
                self.json_server.cleanup()
            if self.webcam_capture:
                self.webcam_capture.cleanup()
            self.logger.info("cleanup completed")
        except Exception as e:
            self.logger.error(f"cleanup error: {e}")
def main():
    logger = get_logger(__name__)
    qt_app = QApplication(sys.argv)
    try:
        app = Application()
        main_window = app.run()
        qt_app.aboutToQuit.connect(app.cleanup)
        sys.exit(qt_app.exec_())
    except Exception as e:
        logger.error(f"application failed: {e}")
        sys.exit(1)
if __name__ == "__main__":
    main()