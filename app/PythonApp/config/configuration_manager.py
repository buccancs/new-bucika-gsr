import json
import os
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional
from ..utils.logging_config import get_logger
@dataclass
class DeviceConfig:
    device_id: str
    device_type: str
    ip_address: str
    port: int
    capabilities: List[str]
    settings: Dict[str, Any]
    last_connected: str
    active: bool = True
@dataclass
class SessionConfig:
    session_id: str
    device_configs: List[DeviceConfig]
    recording_settings: Dict[str, Any]
    calibration_settings: Dict[str, Any]
    created_timestamp: str
    modified_timestamp: str
class ConfigurationManager:
    def __init__(self, config_dir: Optional[str] = None):
        self.logger = get_logger(__name__)
        if config_dir:
            self.config_dir = Path(config_dir)
        else:
            home = Path.home()
            self.config_dir = home / ".multisensor_recording" / "config"
        self.config_dir.mkdir(parents=True, exist_ok=True)
        self.device_config_file = self.config_dir / "device_configs.json"
        self.session_config_file = self.config_dir / "session_configs.json"
        self.app_settings_file = self.config_dir / "app_settings.json"
        self.device_configs: Dict[str, DeviceConfig] = {}
        self.session_configs: Dict[str, SessionConfig] = {}
        self.app_settings: Dict[str, Any] = {}
        self._load_configurations()
        self.logger.info(
            f"ConfigurationManager initialized with config dir: {self.config_dir}"
        )
    def save_device_configuration(self, config: DeviceConfig) -> bool:
        try:
            self.device_configs[config.device_id] = config
            self._save_device_configs()
            self.logger.info(f"Saved device configuration for {config.device_id}")
            return True
        except Exception as e:
            self.logger.error(f"Error saving device configuration: {e}")
            return False
    def get_device_configuration(self, device_id: str) -> Optional[DeviceConfig]:
        return self.device_configs.get(device_id)
    def get_all_device_configurations(self) -> List[DeviceConfig]:
        return list(self.device_configs.values())
    def get_active_device_configurations(self) -> List[DeviceConfig]:
        return [config for config in self.device_configs.values() if config.active]
    def remove_device_configuration(self, device_id: str) -> bool:
        try:
            if device_id in self.device_configs:
                del self.device_configs[device_id]
                self._save_device_configs()
                self.logger.info(f"Removed device configuration for {device_id}")
                return True
            return False
        except Exception as e:
            self.logger.error(f"Error removing device configuration: {e}")
            return False
    def save_session_configuration(self, config: SessionConfig) -> bool:
        try:
            config.modified_timestamp = datetime.now().isoformat()
            self.session_configs[config.session_id] = config
            self._save_session_configs()
            self.logger.info(f"Saved session configuration for {config.session_id}")
            return True
        except Exception as e:
            self.logger.error(f"Error saving session configuration: {e}")
            return False
    def restore_last_session(self) -> Optional[SessionConfig]:
        try:
            if not self.session_configs:
                self.logger.info("No session configurations found")
                return None
            latest_config = max(
                self.session_configs.values(), key=lambda x: x.modified_timestamp
            )
            self.logger.info(f"Restored last session: {latest_config.session_id}")
            return latest_config
        except Exception as e:
            self.logger.error(f"Error restoring last session: {e}")
            return None
    def get_session_configuration(self, session_id: str) -> Optional[SessionConfig]:
        return self.session_configs.get(session_id)
    def export_session_settings(
        self, session_id: str, export_path: Optional[str] = None
    ) -> Optional[str]:
        try:
            config = self.session_configs.get(session_id)
            if not config:
                self.logger.error(f"Session configuration not found: {session_id}")
                return None
            if not export_path:
                export_path = (
                    self.config_dir
                    / f"export_{session_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
                )
            else:
                export_path = Path(export_path)
            export_data = {
                "export_type": "session_configuration",
                "export_timestamp": datetime.now().isoformat(),
                "session_config": asdict(config),
            }
            with open(export_path, "w") as f:
                json.dump(export_data, f, indent=2)
            self.logger.info(f"Exported session settings to {export_path}")
            return str(export_path)
        except Exception as e:
            self.logger.error(f"Error exporting session settings: {e}")
            return None
    def import_session_settings(self, import_path: str) -> Optional[SessionConfig]:
        try:
            import_path = Path(import_path)
            if not import_path.exists():
                self.logger.error(f"Import file not found: {import_path}")
                return None
            with open(import_path, "r") as f:
                data = json.load(f)
            if data.get("export_type") != "session_configuration":
                self.logger.error("Invalid export file format")
                return None
            config_data = data["session_config"]
            device_configs = []
            for device_data in config_data["device_configs"]:
                device_config = DeviceConfig(**device_data)
                device_configs.append(device_config)
            session_config = SessionConfig(
                session_id=config_data["session_id"],
                device_configs=device_configs,
                recording_settings=config_data["recording_settings"],
                calibration_settings=config_data["calibration_settings"],
                created_timestamp=config_data["created_timestamp"],
                modified_timestamp=config_data["modified_timestamp"],
            )
            self.save_session_configuration(session_config)
            self.logger.info(f"Imported session settings from {import_path}")
            return session_config
        except Exception as e:
            self.logger.error(f"Error importing session settings: {e}")
            return None
    def update_app_setting(self, key: str, value: Any) -> bool:
        try:
            self.app_settings[key] = value
            self._save_app_settings()
            self.logger.debug(f"Updated app setting: {key} = {value}")
            return True
        except Exception as e:
            self.logger.error(f"Error updating app setting: {e}")
            return False
    def get_app_setting(self, key: str, default: Any = None) -> Any:
        return self.app_settings.get(key, default)
    def create_session_config_from_devices(
        self, session_id: str, device_ids: List[str]
    ) -> Optional[SessionConfig]:
        try:
            device_configs = []
            for device_id in device_ids:
                config = self.get_device_configuration(device_id)
                if config and config.active:
                    device_configs.append(config)
            if not device_configs:
                self.logger.error("No active device configurations found")
                return None
            session_config = SessionConfig(
                session_id=session_id,
                device_configs=device_configs,
                recording_settings=self.get_app_setting(
                    "default_recording_settings", {}
                ),
                calibration_settings=self.get_app_setting(
                    "default_calibration_settings", {}
                ),
                created_timestamp=datetime.now().isoformat(),
                modified_timestamp=datetime.now().isoformat(),
            )
            self.save_session_configuration(session_config)
            self.logger.info(f"Created session configuration: {session_id}")
            return session_config
        except Exception as e:
            self.logger.error(f"Error creating session configuration: {e}")
            return None
    def _load_configurations(self):
        self._load_device_configs()
        self._load_session_configs()
        self._load_app_settings()
    def _load_device_configs(self):
        try:
            if self.device_config_file.exists():
                with open(self.device_config_file, "r") as f:
                    data = json.load(f)
                for device_id, config_data in data.items():
                    config = DeviceConfig(**config_data)
                    self.device_configs[device_id] = config
                self.logger.info(
                    f"Loaded {len(self.device_configs)} device configurations"
                )
        except Exception as e:
            self.logger.error(f"Error loading device configurations: {e}")
    def _load_session_configs(self):
        try:
            if self.session_config_file.exists():
                with open(self.session_config_file, "r") as f:
                    data = json.load(f)
                for session_id, config_data in data.items():
                    device_configs = []
                    for device_data in config_data["device_configs"]:
                        device_config = DeviceConfig(**device_data)
                        device_configs.append(device_config)
                    config = SessionConfig(
                        session_id=config_data["session_id"],
                        device_configs=device_configs,
                        recording_settings=config_data["recording_settings"],
                        calibration_settings=config_data["calibration_settings"],
                        created_timestamp=config_data["created_timestamp"],
                        modified_timestamp=config_data["modified_timestamp"],
                    )
                    self.session_configs[session_id] = config
                self.logger.info(
                    f"Loaded {len(self.session_configs)} session configurations"
                )
        except Exception as e:
            self.logger.error(f"Error loading session configurations: {e}")
    def _load_app_settings(self):
        try:
            if self.app_settings_file.exists():
                with open(self.app_settings_file, "r") as f:
                    self.app_settings = json.load(f)
                self.logger.info("Loaded application settings")
        except Exception as e:
            self.logger.error(f"Error loading application settings: {e}")
    def _save_device_configs(self):
        try:
            data = {}
            for device_id, config in self.device_configs.items():
                data[device_id] = asdict(config)
            with open(self.device_config_file, "w") as f:
                json.dump(data, f, indent=2)
        except Exception as e:
            self.logger.error(f"Error saving device configurations: {e}")
    def _save_session_configs(self):
        try:
            data = {}
            for session_id, config in self.session_configs.items():
                data[session_id] = asdict(config)
            with open(self.session_config_file, "w") as f:
                json.dump(data, f, indent=2)
        except Exception as e:
            self.logger.error(f"Error saving session configurations: {e}")
    def _save_app_settings(self):
        try:
            with open(self.app_settings_file, "w") as f:
                json.dump(self.app_settings, f, indent=2)
        except Exception as e:
            self.logger.error(f"Error saving application settings: {e}")