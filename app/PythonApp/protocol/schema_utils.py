import json
import os
import time
from typing import Any, Dict, List, Optional
from ..utils.logging_config import get_logger
try:
    from jsonschema import Draft7Validator, ValidationError
    JSONSCHEMA_AVAILABLE = True
except ImportError:
    JSONSCHEMA_AVAILABLE = False
logger = get_logger(__name__)
if not JSONSCHEMA_AVAILABLE:
    logger.warning("jsonschema library not available. Using basic validation only.")
class SchemaManager:
    def __init__(self, schema_path: Optional[str] = None):
        self.schema_path = schema_path or self._get_default_schema_path()
        self.schema: Optional[Dict[str, Any]] = None
        self.validator: Optional[Any] = None
        self._load_schema()
    def _get_default_schema_path(self) -> str:
        current_dir = os.path.dirname(os.path.abspath(__file__))
        project_root = os.path.dirname(os.path.dirname(os.path.dirname(current_dir)))
        return os.path.join(project_root, "protocol", "message_schema.json")
    def _load_schema(self) -> None:
        try:
            with open(self.schema_path, "r", encoding="utf-8") as f:
                self.schema = json.load(f)
            if JSONSCHEMA_AVAILABLE:
                self.validator = Draft7Validator(self.schema)
            logger.info(f"Successfully loaded message schema from {self.schema_path}")
        except FileNotFoundError:
            logger.error(f"Schema file not found: {self.schema_path}")
            raise
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in schema file: {e}")
            raise
        except Exception as e:
            logger.error(f"Error loading schema: {e}")
            raise
    def reload_schema(self) -> None:
        self._load_schema()
    def validate_message(self, message: Dict[str, Any]) -> bool:
        if not self.schema:
            logger.error("Schema not loaded")
            return False
        if not isinstance(message, dict):
            logger.error("Message must be a dictionary")
            return False
        if "type" not in message:
            logger.error("Message missing required 'type' field")
            return False
        if "timestamp" not in message:
            logger.error("Message missing required 'timestamp' field")
            return False
        if JSONSCHEMA_AVAILABLE and self.validator:
            try:
                self.validator.validate(message)
                return True
            except ValidationError as e:
                logger.error(f"Schema validation failed: {e.message}")
                return False
        else:
            return self._basic_validate(message)
    def _basic_validate(self, message: Dict[str, Any]) -> bool:
        message_type = message.get("type")
        if message_type == "start_record":
            return "session_id" in message
        elif message_type == "stop_record":
            return "session_id" in message
        elif message_type == "preview_frame":
            required_fields = ["frame_id", "image_data", "width", "height"]
            return all(field in message for field in required_fields)
        elif message_type == "file_chunk":
            required_fields = [
                "file_id",
                "chunk_index",
                "total_chunks",
                "chunk_data",
                "chunk_size",
                "file_type",
            ]
            return all(field in message for field in required_fields)
        elif message_type == "device_status":
            return "device_id" in message and "status" in message
        elif message_type == "ack":
            return "message_id" in message and "success" in message
        elif message_type == "calibration_start":
            return "pattern_type" in message and "pattern_size" in message
        elif message_type == "calibration_result":
            return "success" in message
        else:
            logger.warning(f"Unknown message type: {message_type}")
            return True
    def get_valid_message_types(self) -> List[str]:
        if not self.schema:
            return []
        message_types = []
        if "oneOf" in self.schema:
            for message_def in self.schema["oneOf"]:
                if "allOf" in message_def:
                    for part in message_def["allOf"]:
                        if "properties" in part and "type" in part["properties"]:
                            type_def = part["properties"]["type"]
                            if "const" in type_def:
                                message_types.append(type_def["const"])
        return message_types
    def create_message(self, message_type: str, **kwargs) -> Dict[str, Any]:
        message = {"type": message_type, "timestamp": int(time.time() * 1000), **kwargs}
        return message
    def get_message_template(self, message_type: str) -> Dict[str, Any]:
        templates = {
            "start_record": {"type": "start_record", "timestamp": 0, "session_id": ""},
            "stop_record": {"type": "stop_record", "timestamp": 0, "session_id": ""},
            "preview_frame": {
                "type": "preview_frame",
                "timestamp": 0,
                "frame_id": 0,
                "image_data": "",
                "width": 0,
                "height": 0,
            },
            "file_chunk": {
                "type": "file_chunk",
                "timestamp": 0,
                "file_id": "",
                "chunk_index": 0,
                "total_chunks": 0,
                "chunk_data": "",
                "chunk_size": 0,
                "file_type": "video",
            },
            "device_status": {
                "type": "device_status",
                "timestamp": 0,
                "device_id": "",
                "status": "idle",
            },
            "ack": {"type": "ack", "timestamp": 0, "message_id": "", "success": True},
            "calibration_start": {
                "type": "calibration_start",
                "timestamp": 0,
                "pattern_type": "chessboard",
                "pattern_size": {"rows": 7, "cols": 6},
            },
            "calibration_result": {
                "type": "calibration_result",
                "timestamp": 0,
                "success": False,
            },
        }
        return templates.get(message_type, {"type": message_type, "timestamp": 0})
_schema_manager: Optional[SchemaManager] = None
def get_schema_manager() -> SchemaManager:
    global _schema_manager
    if _schema_manager is None:
        _schema_manager = SchemaManager()
    return _schema_manager
def validate_message(message: Dict[str, Any]) -> bool:
    return get_schema_manager().validate_message(message)
def get_valid_message_types() -> List[str]:
    return get_schema_manager().get_valid_message_types()
def create_message(message_type: str, **kwargs) -> Dict[str, Any]:
    return get_schema_manager().create_message(message_type, **kwargs)
def create_command_message(command_type: str, **kwargs) -> Dict[str, Any]:
    return create_message(command_type, **kwargs)