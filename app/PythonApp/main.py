import os
import sys
from pathlib import Path

# Add project root to Python path for absolute imports
project_root = Path(__file__).parent.parent
if str(project_root) not in sys.path:
    sys.path.insert(0, str(project_root))

from PyQt5.QtCore import Qt, qVersion
from PyQt5.QtWidgets import QApplication
from PythonApp.utils.logging_config import AppLogger, get_logger
log_level = os.environ.get("MSR_LOG_LEVEL", "INFO")
AppLogger.set_level(log_level)
from PythonApp.gui.main_window import MainWindow
from PythonApp.production.runtime_security_checker import validate_runtime_security, SecurityValidationError
logger = get_logger(__name__)
def main():
    logger.info(
        "=== Multi-Sensor Recording System Controller Starting ==="
    )
    logger.info(f"Python version: {sys.version}")
    logger.info(f"PyQt5 available, Qt version: {qVersion()}")
    try:
        logger.info("[SECURE] Performing runtime security validation...")
        validate_runtime_security()
        logger.info("[SECURE] Runtime security validation completed successfully")
    except SecurityValidationError as e:
        logger.error(f"[SECURE] SECURITY VALIDATION FAILED: {e}")
        logger.error("[SECURE] Application startup aborted due to security issues")
        print(f"\n[FAIL] SECURITY ERROR: {e}")
        print("[SECURE] Please fix security issues before running the application")
        sys.exit(1)
    except Exception as e:
        logger.warning(f"[SECURE] Security validation encountered an error: {e}")
        logger.warning("[SECURE] Continuing startup with security warning")
    try:
        logger.debug("Configuring high DPI scaling")
        QApplication.setAttribute(Qt.AA_EnableHighDpiScaling, True)
        QApplication.setAttribute(Qt.AA_UseHighDpiPixmaps, True)
        logger.debug("Creating QApplication instance")
        app = QApplication(sys.argv)
        app.setApplicationName("Multi-Sensor Recording System Controller")
        logger.info("Application properties configured")
        logger.debug("Creating MainWindow instance")
        main_window = MainWindow()
        logger.info("MainWindow created successfully")
        logger.debug("Showing main window")
        main_window.show()
        logger.info("Main window displayed")
        logger.info("Starting PyQt event loop")
        exit_code = app.exec_()
        logger.info(f"Application exiting with code: {exit_code}")
        sys.exit(exit_code)
    except Exception as e:
        logger.error(f"Fatal error during application startup: {e}", exc_info=True)
        sys.exit(1)
if __name__ == "__main__":
    logger.info("Application started from command line")
    main()
