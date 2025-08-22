import json
import os
from typing import Any, Dict, Optional
from ..utils.logging_config import get_logger
logger = get_logger(__name__)
class ConfigManager:
    def __init__(self, config_path: Optional[str] = None):
        self.config_path = config_path or self._get_default_config_path()
        self.config: Optional[Dict[str, Any]] = None
        self._load_config()
    def _get_default_config_path(self) -> str:
        current_dir = os.path.dirname(os.path.abspath(__file__))
        project_root = os.path.dirname(os.path.dirname(os.path.dirname(current_dir)))
        return os.path.join(project_root, "protocol", "config.json")
    def _load_config(self) -> None:
        try:
            with open(self.config_path, "r", encoding="utf-8") as f:
                self.config = json.load(f)
            logger.info(f"Successfully loaded configuration from {self.config_path}")
        except FileNotFoundError:
            logger.error(f"Configuration file not found: {self.config_path}")
            raise
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in configuration file: {e}")
            raise
        except Exception as e:
            logger.error(f"Error loading configuration: {e}")
            raise
    def reload_config(self) -> None:
        self._load_config()
    def get(self, key: str, default: Any = None) -> Any:
        if not self.config:
            return default
        keys = key.split(".")
        value = self.config
        try:
            for k in keys:
                value = value[k]
            return value
        except (KeyError, TypeError):
            return default
    def get_section(self, section: str) -> Dict[str, Any]:
        if not self.config:
            return {}
        return self.config.get(section, {})
    def get_network_config(self) -> Dict[str, Any]:
        return self.get_section("network")
    def get_devices_config(self) -> Dict[str, Any]:
        return self.get_section("devices")
    def get_ui_config(self) -> Dict[str, Any]:
        return self.get_section("UI")
    def get_calibration_config(self) -> Dict[str, Any]:
        return self.get_section("calibration")
    def get_session_config(self) -> Dict[str, Any]:
        return self.get_section("session")
    def get_logging_config(self) -> Dict[str, Any]:
        return self.get_section("logging")
    def get_testing_config(self) -> Dict[str, Any]:
        return self.get_section("testing")
    def get_performance_config(self) -> Dict[str, Any]:
        return self.get_section("performance")
    def get_security_config(self) -> Dict[str, Any]:
        return self.get_section("security")
    def get_host(self) -> str:
        return self.get("network.host", "0.0.0.0")
    def get_port(self) -> int:
        return self.get("network.port", 9000)
    def get_timeout(self) -> int:
        return self.get("network.timeout_seconds", 30)
    def get_frame_rate(self) -> int:
        return self.get("devices.frame_rate", 30)
    def get_resolution(self) -> tuple:
        res = self.get("devices.resolution", {"width": 1920, "height": 1080})
        return res["width"], res["height"]
    def get_preview_resolution(self) -> tuple:
        res = self.get("devices.preview_resolution", {"width": 640, "height": 480})
        return res["width"], res["height"]
    def get_preview_scale(self) -> float:
        return self.get("UI.preview_scale", 0.5)
    def get_calibration_pattern_size(self) -> tuple:
        rows = self.get("calibration.pattern_rows", 7)
        cols = self.get("calibration.pattern_cols", 6)
        return rows, cols
    def get_calibration_square_size(self) -> float:
        return self.get("calibration.square_size_m", 0.0245)
    def get_calibration_error_threshold(self) -> float:
        return self.get("calibration.error_threshold", 1.0)
    def get_session_directory(self) -> str:
        return self.get("session.session_directory", "recordings")
    def get_log_level(self) -> str:
        return self.get("logging.level", "INFO")
    def is_fake_device_enabled(self) -> bool:
        return self.get("testing.fake_device_enabled", False)
    def validate_config(self) -> bool:
        if not self.config:
            logger.error("Configuration not loaded")
            return False
        required_sections = ["network", "devices", "UI", "calibration"]
        for section in required_sections:
            if section not in self.config:
                logger.error(f"Missing required configuration section: {section}")
                return False
        network = self.get_network_config()
        if not network.get("host") or not network.get("port"):
            logger.error("Network configuration missing host or port")
            return False
        devices = self.get_devices_config()
        if devices.get("frame_rate", 0) <= 0:
            logger.error("Invalid frame rate in devices configuration")
            return False
        calibration = self.get_calibration_config()
        if (
            calibration.get("pattern_rows", 0) <= 0
            or calibration.get("pattern_cols", 0) <= 0
        ):
            logger.error("Invalid calibration pattern size")
            return False
        logger.info("Configuration validation passed")
        return True
_config_manager: Optional[ConfigManager] = None
def get_config_manager() -> ConfigManager:
    global _config_manager
    if _config_manager is None:
        _config_manager = ConfigManager()
    return _config_manager
def get_config(key: str, default: Any = None) -> Any:
    return get_config_manager().get(key, default)
def get_network_config() -> Dict[str, Any]:
    return get_config_manager().get_network_config()
def get_devices_config() -> Dict[str, Any]:
    return get_config_manager().get_devices_config()
def get_ui_config() -> Dict[str, Any]:
    return get_config_manager().get_ui_config()
def get_calibration_config() -> Dict[str, Any]:
    return get_config_manager().get_calibration_config()
def get_host() -> str:
    return get_config_manager().get_host()
def get_port() -> int:
    return get_config_manager().get_port()
def get_frame_rate() -> int:
    return get_config_manager().get_frame_rate()
def get_resolution() -> tuple:
    return get_config_manager().get_resolution()
def get_preview_scale() -> float:
    return get_config_manager().get_preview_scale()
def get_calibration_pattern_size() -> tuple:
    return get_config_manager().get_calibration_pattern_size()
def get_calibration_error_threshold() -> float:
    return get_config_manager().get_calibration_error_threshold()
def reload_config() -> None:
    get_config_manager().reload_config()
def validate_config() -> bool:
    return get_config_manager().validate_config()