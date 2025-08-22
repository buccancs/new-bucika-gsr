import json
import platform
import socket
from typing import Any, Dict, Tuple
from ..utils.logging_config import get_logger
from .config_loader import get_config_manager
from .schema_utils import get_schema_manager
logger = get_logger(__name__)
class HandshakeManager:
    def __init__(self):
        self.config_manager = get_config_manager()
        self.schema_manager = get_schema_manager()
        self.protocol_version = self.config_manager.get("protocol_version", 1)
        self.app_version = self.config_manager.get("version", "1.0.0")
    def send_handshake(self, sock: socket.socket) -> bool:
        try:
            handshake_message = self._create_handshake_message()
            message_json = json.dumps(handshake_message)
            logger.info(f"Sending handshake: {message_json}")
            sock.send(message_json.encode("utf-8"))
            sock.send(b"\n")
            logger.info("Handshake sent successfully")
            return True
        except Exception as e:
            logger.error(f"Failed to send handshake: {e}")
            return False
    def process_handshake(self, handshake_message: Dict[str, Any]) -> Tuple[bool, str]:
        try:
            if not self.schema_manager.validate_message(handshake_message):
                logger.error("Invalid handshake message format")
                return False, "Invalid handshake message format"
            client_protocol_version = handshake_message.get("protocol_version")
            device_name = handshake_message.get("device_name", "Unknown Device")
            app_version = handshake_message.get("app_version", "Unknown")
            device_type = handshake_message.get("device_type", "unknown")
            logger.info(
                f"Received handshake from {device_name} ({device_type}) v{app_version}"
            )
            logger.info(f"Client protocol version: {client_protocol_version}")
            logger.info(f"Server protocol version: {self.protocol_version}")
            compatible = self._are_versions_compatible(
                client_protocol_version, self.protocol_version
            )
            if not compatible:
                message = f"Protocol version mismatch: client v{client_protocol_version}, server v{self.protocol_version}"
                logger.warning(message)
                logger.warning(
                    "Consider updating both applications to the same version"
                )
                return False, message
            logger.info("Handshake successful - protocol versions compatible")
            return True, "Protocol versions compatible"
        except Exception as e:
            error_msg = f"Error processing handshake: {e}"
            logger.error(error_msg)
            return False, error_msg
    def send_handshake_ack(
        self, sock: socket.socket, compatible: bool, message: str = ""
    ) -> bool:
        try:
            ack_message = self._create_handshake_ack(compatible, message)
            message_json = json.dumps(ack_message)
            logger.info(f"Sending handshake ack: {message_json}")
            sock.send(message_json.encode("utf-8"))
            sock.send(b"\n")
            logger.info("Handshake acknowledgment sent successfully")
            return True
        except Exception as e:
            logger.error(f"Failed to send handshake acknowledgment: {e}")
            return False
    def process_handshake_ack(self, ack_message: Dict[str, Any]) -> bool:
        try:
            if not self.schema_manager.validate_message(ack_message):
                logger.error("Invalid handshake acknowledgment format")
                return False
            server_protocol_version = ack_message.get("protocol_version")
            server_name = ack_message.get("server_name", "Unknown Server")
            server_version = ack_message.get("server_version", "Unknown")
            compatible = ack_message.get("compatible", False)
            message = ack_message.get("message", "")
            logger.info(f"Received handshake ack from {server_name} v{server_version}")
            logger.info(f"Server protocol version: {server_protocol_version}")
            logger.info(f"Client protocol version: {self.protocol_version}")
            if not compatible:
                logger.warning("Protocol version mismatch detected!")
                logger.warning(f"Server message: {message}")
                if server_protocol_version != self.protocol_version:
                    logger.warning(
                        f"Version mismatch: Python v{self.protocol_version}, Server v{server_protocol_version}"
                    )
                    logger.warning(
                        "Consider updating both applications to the same version"
                    )
                return False
            logger.info("Handshake successful - protocol versions compatible")
            if message:
                logger.info(f"Server message: {message}")
            return True
        except Exception as e:
            logger.error(f"Error processing handshake acknowledgment: {e}")
            return False
    def _create_handshake_message(self) -> Dict[str, Any]:
        return self.schema_manager.create_message(
            "handshake",
            protocol_version=self.protocol_version,
            device_name=self._get_device_name(),
            app_version=self.app_version,
            device_type="pc",
        )
    def _create_handshake_ack(
        self, compatible: bool, message: str = ""
    ) -> Dict[str, Any]:
        ack_data = {
            "protocol_version": self.protocol_version,
            "server_name": "Python PC Controller",
            "server_version": self.app_version,
            "compatible": compatible,
        }
        if message:
            ack_data["message"] = message
        return self.schema_manager.create_message("handshake_ack", **ack_data)
    def _get_device_name(self) -> str:
        try:
            return f"{platform.system()} {platform.node()}"
        except Exception:
            return "Python PC"
    def _are_versions_compatible(
        self, client_version: int, server_version: int
    ) -> bool:
        return client_version == server_version
_handshake_manager = None
def get_handshake_manager() -> HandshakeManager:
    global _handshake_manager
    if _handshake_manager is None:
        _handshake_manager = HandshakeManager()
    return _handshake_manager
def send_handshake(sock: socket.socket) -> bool:
    return get_handshake_manager().send_handshake(sock)
def process_handshake(handshake_message: Dict[str, Any]) -> Tuple[bool, str]:
    return get_handshake_manager().process_handshake(handshake_message)
def send_handshake_ack(
    sock: socket.socket, compatible: bool, message: str = ""
) -> bool:
    return get_handshake_manager().send_handshake_ack(sock, compatible, message)
def process_handshake_ack(ack_message: Dict[str, Any]) -> bool:
    return get_handshake_manager().process_handshake_ack(ack_message)