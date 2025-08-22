import os
import sys
import webbrowser
from PyQt5.QtCore import Qt, QTimer, qVersion
from PyQt5.QtGui import QIcon
from PyQt5.QtWidgets import QApplication, QMessageBox
try:
    from PythonApp.utils.logging_config import AppLogger, get_logger
    log_level = os.environ.get("MSR_LOG_LEVEL", "INFO")
    AppLogger.set_level(log_level)
    logger = get_logger(__name__)
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
try:
    from PythonApp.gui.main_window import MainWindow
except ImportError:
    logger.error("Main window not available")
    MainWindow = None
try:
    from PythonApp.gui.main_controller import MainController
    MAIN_CONTROLLER_AVAILABLE = True
except ImportError:
    logger.warning("MainController not available")
    MainController = None
    MAIN_CONTROLLER_AVAILABLE = False
try:
    from PythonApp.session.session_manager import SessionManager
    SESSION_MANAGER_AVAILABLE = True
except ImportError:
    logger.warning("SessionManager not available")
    SessionManager = None
    SESSION_MANAGER_AVAILABLE = False
try:
    from PythonApp.shimmer_manager import ShimmerManager
    SHIMMER_MANAGER_AVAILABLE = True
except ImportError:
    logger.warning("ShimmerManager not available")
    ShimmerManager = None
    SHIMMER_MANAGER_AVAILABLE = False
try:
    from PythonApp.network.android_device_manager import AndroidDeviceManager
    ANDROID_DEVICE_MANAGER_AVAILABLE = True
except ImportError:
    logger.warning("AndroidDeviceManager not available")
    AndroidDeviceManager = None
    ANDROID_DEVICE_MANAGER_AVAILABLE = False
try:
    from PythonApp.network.device_server import JsonSocketServer
    JSON_SOCKET_SERVER_AVAILABLE = True
except ImportError:
    logger.warning("JsonSocketServer not available")
    JsonSocketServer = None
    JSON_SOCKET_SERVER_AVAILABLE = False
try:
    from PythonApp.webcam.webcam_capture import WebcamCapture
    WEBCAM_CAPTURE_AVAILABLE = True
except ImportError:
    logger.warning("WebcamCapture not available")
    WebcamCapture = None
    WEBCAM_CAPTURE_AVAILABLE = False
try:
    from PythonApp.gui.stimulus_controller import StimulusController
    STIMULUS_CONTROLLER_AVAILABLE = True
except ImportError:
    logger.warning("StimulusController not available")
    StimulusController = None
    STIMULUS_CONTROLLER_AVAILABLE = False
try:
    from PythonApp.web_ui.integration import WebDashboardIntegration
    WEB_UI_AVAILABLE = True
except ImportError:
    logger.warning("Web UI components not available")
    WebDashboardIntegration = None
    WEB_UI_AVAILABLE = False
class EnhancedApplicationWithWebUI:
    def __init__(self):
        self.app = None
        self.main_window = None
        self.web_integration = None
        self.main_controller = None
        self.session_manager = None
        self.shimmer_manager = None
        self.android_device_manager = None
        self.json_server = None
        self.webcam_capture = None
        self.stimulus_controller = None
        logger.info("Enhanced Application with Web UI initialized")
    def setup_application(self):
        logger.info(
            "=== Multi-Sensor Recording System Controller Starting (Enhanced UI + Web Dashboard) ==="
        )
        logger.info(f"Python version: {sys.version}")
        logger.info(f"PyQt5 available, Qt version: {qVersion()}")
        QApplication.setAttribute(Qt.AA_EnableHighDpiScaling, True)
        QApplication.setAttribute(Qt.AA_UseHighDpiPixmaps, True)
        self.app = QApplication(sys.argv)
        self.app.setStyle("Fusion")
        self.app.setApplicationName("Multi-Sensor Recording System")
        self.app.setApplicationVersion("2.0")
        self.app.setOrganizationName("Multi-Sensor Recording Team")
        logger.info("QApplication created and configured")
    def setup_backend_services(self):
        logger.info("Setting up backend services...")
        try:
            if SESSION_MANAGER_AVAILABLE:
                self.session_manager = SessionManager(base_recordings_dir="recordings")
                logger.info("SessionManager initialized")
            if SHIMMER_MANAGER_AVAILABLE:
                self.shimmer_manager = ShimmerManager()
                logger.info("ShimmerManager initialized")
            if ANDROID_DEVICE_MANAGER_AVAILABLE:
                self.android_device_manager = AndroidDeviceManager(server_port=9000)
                logger.info("AndroidDeviceManager initialized")
            if JSON_SOCKET_SERVER_AVAILABLE:
                self.json_server = JsonSocketServer(host="0.0.0.0", port=9000)
                logger.info("JsonSocketServer initialized")
            if WEBCAM_CAPTURE_AVAILABLE:
                self.webcam_capture = WebcamCapture()
                logger.info("WebcamCapture initialized")
            if STIMULUS_CONTROLLER_AVAILABLE:
                self.stimulus_controller = StimulusController()
                logger.info("StimulusController initialized")
            if MAIN_CONTROLLER_AVAILABLE:
                self.main_controller = MainController()
                if all(
                    [
                        self.session_manager,
                        self.json_server,
                        self.webcam_capture,
                        self.stimulus_controller,
                    ]
                ):
                    self.main_controller.inject_dependencies(
                        self.session_manager,
                        self.json_server,
                        self.webcam_capture,
                        self.stimulus_controller,
                    )
                    logger.info("MainController initialized with dependencies")
                else:
                    logger.warning("Some MainController dependencies not available")
            logger.info("Backend services setup completed")
            return True
        except Exception as e:
            logger.error(f"Failed to setup backend services: {e}")
            return False
    def setup_desktop_ui(self):
        if MainWindow is None:
            logger.error("No desktop UI components available")
            return False
        try:
            self.main_window = MainWindow()
            self.main_window.setWindowTitle(
                "Multi-Sensor Recording System - UI + Web Dashboard"
            )
            if WEB_UI_AVAILABLE:
                self._add_web_dashboard_integration()
            logger.info("Desktop UI components initialized successfully")
            return True
        except Exception as e:
            logger.error(f"Failed to initialize desktop UI: {e}")
            return False
    def setup_web_dashboard(self):
        if not WEB_UI_AVAILABLE:
            logger.warning("Web UI not available, skipping web dashboard setup")
            return False
        try:
            self.web_integration = WebDashboardIntegration(
                enable_web_ui=True,
                web_port=5000,
                main_controller=self.main_controller,
                session_manager=self.session_manager,
                shimmer_manager=self.shimmer_manager,
                android_device_manager=self.android_device_manager,
            )
            if self.web_integration.start_web_dashboard():
                logger.info(
                    "Web dashboard started successfully with real application components"
                )
                self._connect_web_integration()
                return True
            else:
                logger.error("Failed to start web dashboard")
                return False
        except Exception as e:
            logger.error(f"Error setting up web dashboard: {e}")
            return False
    def _add_web_dashboard_integration(self):
        if not hasattr(self.main_window, "menuBar"):
            return
        try:
            web_menu = self.main_window.menuBar().addMenu("Web Dashboard")
            open_dashboard_action = web_menu.addAction("Open in Browser")
            open_dashboard_action.setShortcut("Ctrl+W")
            open_dashboard_action.triggered.connect(self._open_web_dashboard)
            show_url_action = web_menu.addAction("Show Dashboard URL")
            show_url_action.triggered.connect(self._show_dashboard_url)
            web_menu.addSeparator()
            toggle_web_action = web_menu.addAction("Enable Web Dashboard")
            toggle_web_action.setCheckable(True)
            toggle_web_action.setChecked(True)
            toggle_web_action.triggered.connect(self._toggle_web_dashboard)
            logger.info("Web dashboard menu items added to desktop UI")
        except Exception as e:
            logger.error(f"Failed to add web dashboard integration to UI: {e}")
    def _connect_web_integration(self):
        if not self.web_integration:
            return
        self.status_timer = QTimer()
        self.status_timer.timeout.connect(self._update_web_dashboard_status)
        self.status_timer.start(5000)
        logger.info("Web dashboard integration connected to desktop application")
    def _update_web_dashboard_status(self):
        if not self.web_integration:
            return
        try:
            try:
                import psutil
                pc_status = {
                    "status": "running",
                    "cpu_usage": psutil.cpu_percent(interval=0.1),
                    "memory_usage": psutil.virtual_memory().percent,
                    "disk_usage": (
                        psutil.disk_usage("/").percent
                        if os.name != "nt"
                        else psutil.disk_usage("C:").percent
                    ),
                    "ui_active": True,
                    "last_update": "Now",
                }
            except ImportError:
                pc_status = {
                    "status": "running",
                    "cpu_usage": 0,
                    "memory_usage": 0,
                    "ui_active": True,
                    "last_update": "Now",
                }
            if self.web_integration.controller and hasattr(
                self.web_integration.controller, "device_status_received"
            ):
                self.web_integration.controller.device_status_received.emit(
                    "desktop_app", {"type": "pc_controller", **pc_status}
                )
        except Exception as e:
            logger.error(f"Failed to update web dashboard status: {e}")
    def _open_web_dashboard(self):
        if not self.web_integration:
            QMessageBox.warning(
                self.main_window, "Web Dashboard", "Web dashboard is not available"
            )
            return
        dashboard_url = self.web_integration.get_web_dashboard_url()
        if dashboard_url:
            webbrowser.open(dashboard_url)
            logger.info(f"Opened web dashboard in browser: {dashboard_url}")
        else:
            QMessageBox.warning(
                self.main_window, "Web Dashboard", "Web dashboard is not running"
            )
    def _show_dashboard_url(self):
        if not self.web_integration:
            QMessageBox.information(
                self.main_window, "Web Dashboard", "Web dashboard is not available"
            )
            return
        dashboard_url = self.web_integration.get_web_dashboard_url()
        if dashboard_url:
            QMessageBox.information(
                self.main_window,
                "Web Dashboard URL",
                f"""Access the web dashboard at:

{dashboard_url}""",
            )
        else:
            QMessageBox.information(
                self.main_window, "Web Dashboard", "Web dashboard is not running"
            )
    def _toggle_web_dashboard(self, enabled):
        if not WEB_UI_AVAILABLE:
            QMessageBox.warning(
                self.main_window,
                "Web Dashboard",
                "Web dashboard components are not available",
            )
            return
        if enabled:
            if not self.web_integration:
                self.setup_web_dashboard()
            elif not self.web_integration.is_running:
                self.web_integration.start_web_dashboard()
        elif self.web_integration:
            self.web_integration.stop_web_dashboard()
    def run(self):
        self.setup_application()
        if not self.setup_backend_services():
            logger.error(
                "Failed to setup backend services, continuing with limited functionality"
            )
        if not self.setup_desktop_ui():
            logger.error("Failed to setup desktop UI")
            return 1
        web_success = self.setup_web_dashboard()
        if web_success:
            logger.info(
                "Application started with both desktop and web interfaces connected to real components"
            )
        else:
            logger.info("Application started with desktop interface only")
        self.main_window.show()
        logger.info("Main window displayed")
        if web_success:
            dashboard_url = self.web_integration.get_web_dashboard_url()
            QMessageBox.information(
                self.main_window,
                "Multi-Sensor Recording System",
                f"""Application started successfully!

Desktop UI: Running
Web Dashboard: {dashboard_url}
Connected Components: MainController, SessionManager, ShimmerManager, AndroidDeviceManager

The web interface is connected to the same data sources as the desktop application.""",
            )
        logger.info("Starting PyQt event loop")
        return self.app.exec_()
    def cleanup(self):
        logger.info("Cleaning up application resources...")
        if self.web_integration:
            self.web_integration.stop_web_dashboard()
            logger.info("Web dashboard stopped")
        if self.shimmer_manager and hasattr(self.shimmer_manager, "shutdown"):
            try:
                self.shimmer_manager.shutdown()
                logger.info("ShimmerManager shut down")
            except Exception as e:
                logger.error(f"Error shutting down ShimmerManager: {e}")
        if self.android_device_manager and hasattr(
            self.android_device_manager, "shutdown"
        ):
            try:
                self.android_device_manager.shutdown()
                logger.info("AndroidDeviceManager shut down")
            except Exception as e:
                logger.error(f"Error shutting down AndroidDeviceManager: {e}")
        if self.json_server and hasattr(self.json_server, "stop"):
            try:
                self.json_server.stop()
                logger.info("JsonSocketServer stopped")
            except Exception as e:
                logger.error(f"Error stopping JsonSocketServer: {e}")
        if self.webcam_capture and hasattr(self.webcam_capture, "stop"):
            try:
                self.webcam_capture.stop()
                logger.info("WebcamCapture stopped")
            except Exception as e:
                logger.error(f"Error stopping WebcamCapture: {e}")
        logger.info("Application cleanup completed")
def main():
    app = EnhancedApplicationWithWebUI()
    try:
        return app.run()
    except KeyboardInterrupt:
        logger.info("Application interrupted by user")
        return 0
    except Exception as e:
        logger.error(f"Application error: {e}")
        return 1
    finally:
        app.cleanup()
if __name__ == "__main__":
    sys.exit(main())
