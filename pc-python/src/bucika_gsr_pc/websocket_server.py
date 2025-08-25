"""
WebSocket server for handling communication with Android clients.
Maintains full compatibility with the existing Kotlin implementation.
"""

import asyncio
import json
import hashlib
import base64
from datetime import datetime
from pathlib import Path
from typing import Dict, Set, Optional
import uuid
import websockets
from websockets.server import WebSocketServerProtocol
from loguru import logger

from .protocol import (
    MessageEnvelope, MessageType, MessagePayload, EmptyPayload,
    HelloPayload, RegisterPayload, StartPayload, AckPayload, 
    ErrorPayload, GSRSamplePayload, UploadBeginPayload,
    UploadChunkPayload, UploadEndPayload, parse_message_payload
)
from .session_manager import SessionManager
from .time_sync_service import TimeSyncService


class ConnectedDevice:
    """Represents a connected Android device"""
    
    def __init__(self, device_id: str, websocket: WebSocketServerProtocol, 
                 device_name: str, capabilities: list, battery_level: int, version: str):
        self.device_id = device_id
        self.websocket = websocket
        self.device_name = device_name
        self.capabilities = capabilities
        self.battery_level = battery_level
        self.version = version
        self.connected_at = datetime.now()
        self.last_ping = None


class FileUploadTracker:
    """Tracks file upload progress"""
    
    def __init__(self, filename: str, file_size: int, md5_hash: str, chunk_size: int):
        self.filename = filename
        self.file_size = file_size
        self.md5_hash = md5_hash
        self.chunk_size = chunk_size
        self.chunks_received = 0
        self.data_chunks = {}
        self.started_at = datetime.now()


class WebSocketServer:
    """WebSocket server for Android client communication"""
    
    def __init__(self, port: int, session_manager: SessionManager, 
                 time_sync_service: TimeSyncService):
        self.port = port
        self.session_manager = session_manager
        self.time_sync_service = time_sync_service
        
        self.server = None
        self.connected_devices: Dict[str, ConnectedDevice] = {}
        self.device_connections: Dict[str, WebSocketServerProtocol] = {}
        self.active_uploads: Dict[str, FileUploadTracker] = {}
        
        # Ping/pong tracking
        self.ping_interval = 30  # seconds
        self.ping_tasks: Set[asyncio.Task] = set()
        
    async def start(self):
        """Start the WebSocket server"""
        try:
            self.server = await websockets.serve(
                self.handle_connection,
                "0.0.0.0",  # Listen on all interfaces
                self.port,
                ping_interval=None,  # We handle ping/pong manually
                ping_timeout=None
            )
            logger.info(f"WebSocket server started on port {self.port}")
        except Exception as e:
            logger.error(f"Failed to start WebSocket server: {e}")
            raise
    
    async def stop(self):
        """Stop the WebSocket server"""
        if self.server:
            # Cancel all ping tasks
            for task in self.ping_tasks:
                task.cancel()
            
            # Close all connections
            for device in self.connected_devices.values():
                await device.websocket.close()
            
            self.server.close()
            await self.server.wait_closed()
            logger.info("WebSocket server stopped")
    
    async def handle_connection(self, websocket: WebSocketServerProtocol, path: str):
        """Handle a new WebSocket connection"""
        remote_address = websocket.remote_address
        logger.info(f"New WebSocket connection from {remote_address}")
        
        try:
            async for message in websocket:
                await self.handle_message(websocket, message)
        except websockets.exceptions.ConnectionClosed:
            logger.info(f"WebSocket connection from {remote_address} closed")
        except Exception as e:
            logger.error(f"Error handling WebSocket connection: {e}")
        finally:
            await self.cleanup_connection(websocket)
    
    async def handle_message(self, websocket: WebSocketServerProtocol, message: str):
        """Handle incoming WebSocket message"""
        try:
            # Parse message envelope
            data = json.loads(message)
            envelope = MessageEnvelope(**data)
            payload = parse_message_payload(envelope)
            
            logger.debug(f"Received {envelope.type} message from {envelope.deviceId}")
            
            # Handle message based on type
            if envelope.type == MessageType.HELLO:
                await self.handle_hello(websocket, envelope, payload)
            elif envelope.type == MessageType.PING:
                await self.handle_ping(websocket, envelope, payload)
            elif envelope.type == MessageType.START:
                await self.handle_start(websocket, envelope, payload)
            elif envelope.type == MessageType.STOP:
                await self.handle_stop(websocket, envelope, payload)
            elif envelope.type == MessageType.SYNC_MARK:
                await self.handle_sync_mark(websocket, envelope, payload)
            elif envelope.type == MessageType.GSR_SAMPLE:
                await self.handle_gsr_sample(websocket, envelope, payload)
            elif envelope.type == MessageType.UPLOAD_BEGIN:
                await self.handle_upload_begin(websocket, envelope, payload)
            elif envelope.type == MessageType.UPLOAD_CHUNK:
                await self.handle_upload_chunk(websocket, envelope, payload)
            elif envelope.type == MessageType.UPLOAD_END:
                await self.handle_upload_end(websocket, envelope, payload)
            else:
                logger.warning(f"Unknown message type: {envelope.type}")
                await self.send_error(websocket, envelope.deviceId, "UNKNOWN_MESSAGE_TYPE", 
                                    f"Unknown message type: {envelope.type}")
                
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON message: {e}")
        except Exception as e:
            logger.error(f"Error handling message: {e}")
    
    async def handle_hello(self, websocket: WebSocketServerProtocol, 
                          envelope: MessageEnvelope, payload: HelloPayload):
        """Handle HELLO message from Android client"""
        device_id = envelope.deviceId
        
        # Create connected device
        device = ConnectedDevice(
            device_id=device_id,
            websocket=websocket,
            device_name=payload.deviceName,
            capabilities=payload.capabilities,
            battery_level=payload.batteryLevel,
            version=payload.version
        )
        
        # Register device
        self.connected_devices[device_id] = device
        self.device_connections[device_id] = websocket
        
        logger.info(f"Device registered: {payload.deviceName} ({device_id})")
        
        # Send REGISTER response
        register_response = RegisterPayload(
            accepted=True,
            syncPort=9123
        )
        
        await self.send_message(websocket, device_id, MessageType.REGISTER, register_response)
        
        # Start ping task for this device
        ping_task = asyncio.create_task(self.ping_loop(websocket, device_id))
        self.ping_tasks.add(ping_task)
    
    async def handle_ping(self, websocket: WebSocketServerProtocol,
                         envelope: MessageEnvelope, payload: EmptyPayload):
        """Handle PING message"""
        device = self.connected_devices.get(envelope.deviceId)
        if device:
            device.last_ping = datetime.now()
        
        # Send PONG response
        await self.send_message(websocket, envelope.deviceId, MessageType.PONG, EmptyPayload())
    
    async def handle_start(self, websocket: WebSocketServerProtocol,
                          envelope: MessageEnvelope, payload: StartPayload):
        """Handle START session message"""
        device_id = envelope.deviceId
        
        # Start session
        session_id = await self.session_manager.start_session(
            session_name=payload.sessionName,
            device_id=device_id,
            participant_id=payload.participantId,
            metadata=payload.metadata
        )
        
        logger.info(f"Started session {session_id} for device {device_id}")
        
        # Send ACK
        ack = AckPayload(messageId=envelope.id, status="SESSION_STARTED")
        await self.send_message(websocket, device_id, MessageType.ACK, ack, session_id)
    
    async def handle_stop(self, websocket: WebSocketServerProtocol,
                         envelope: MessageEnvelope, payload: EmptyPayload):
        """Handle STOP session message"""
        device_id = envelope.deviceId
        
        # Stop session
        await self.session_manager.stop_session(device_id)
        
        logger.info(f"Stopped session for device {device_id}")
        
        # Send ACK
        ack = AckPayload(messageId=envelope.id, status="SESSION_STOPPED")
        await self.send_message(websocket, device_id, MessageType.ACK, ack)
    
    async def handle_sync_mark(self, websocket: WebSocketServerProtocol,
                              envelope: MessageEnvelope, payload):
        """Handle SYNC_MARK message"""
        # TODO: Implement sync mark handling
        logger.debug(f"Sync mark received from {envelope.deviceId}")
        
        # Send ACK
        ack = AckPayload(messageId=envelope.id, status="SYNC_MARK_RECEIVED")
        await self.send_message(websocket, envelope.deviceId, MessageType.ACK, ack)
    
    async def handle_gsr_sample(self, websocket: WebSocketServerProtocol,
                               envelope: MessageEnvelope, payload: GSRSamplePayload):
        """Handle GSR sample data"""
        device_id = envelope.deviceId
        
        # Store GSR samples
        await self.session_manager.store_gsr_samples(device_id, payload.samples)
        
        # Note: No ACK for GSR samples to avoid overwhelming the connection
    
    async def handle_upload_begin(self, websocket: WebSocketServerProtocol,
                                 envelope: MessageEnvelope, payload: UploadBeginPayload):
        """Handle file upload begin"""
        upload_key = f"{envelope.deviceId}:{payload.filename}"
        
        tracker = FileUploadTracker(
            filename=payload.filename,
            file_size=payload.fileSize,
            md5_hash=payload.md5Hash,
            chunk_size=payload.chunkSize
        )
        
        self.active_uploads[upload_key] = tracker
        
        logger.info(f"Starting upload: {payload.filename} ({payload.fileSize} bytes)")
        
        # Send ACK
        ack = AckPayload(messageId=envelope.id, status="UPLOAD_READY")
        await self.send_message(websocket, envelope.deviceId, MessageType.ACK, ack)
    
    async def handle_upload_chunk(self, websocket: WebSocketServerProtocol,
                                 envelope: MessageEnvelope, payload: UploadChunkPayload):
        """Handle file upload chunk"""
        upload_key = f"{envelope.deviceId}:{payload.filename}"
        tracker = self.active_uploads.get(upload_key)
        
        if not tracker:
            await self.send_error(websocket, envelope.deviceId, "UPLOAD_NOT_FOUND",
                                f"Upload not found for {payload.filename}")
            return
        
        # Decode and store chunk
        chunk_data = base64.b64decode(payload.data)
        tracker.data_chunks[payload.chunkIndex] = chunk_data
        tracker.chunks_received += 1
        
        logger.debug(f"Received chunk {payload.chunkIndex} for {payload.filename}")
        
        # If this is the last chunk, finalize upload
        if payload.isLast:
            await self.finalize_upload(websocket, envelope.deviceId, tracker)
            del self.active_uploads[upload_key]
    
    async def handle_upload_end(self, websocket: WebSocketServerProtocol,
                               envelope: MessageEnvelope, payload: UploadEndPayload):
        """Handle file upload end"""
        upload_key = f"{envelope.deviceId}:{payload.filename}"
        tracker = self.active_uploads.get(upload_key)
        
        if tracker:
            await self.finalize_upload(websocket, envelope.deviceId, tracker)
            del self.active_uploads[upload_key]
    
    async def finalize_upload(self, websocket: WebSocketServerProtocol, 
                             device_id: str, tracker: FileUploadTracker):
        """Finalize file upload by reassembling chunks and verifying integrity"""
        try:
            # Reassemble file data
            file_data = b""
            for i in sorted(tracker.data_chunks.keys()):
                file_data += tracker.data_chunks[i]
            
            # Verify file size
            if len(file_data) != tracker.file_size:
                await self.send_error(websocket, device_id, "SIZE_MISMATCH",
                                    f"File size mismatch: expected {tracker.file_size}, got {len(file_data)}")
                return
            
            # Verify MD5 hash
            calculated_md5 = hashlib.md5(file_data).hexdigest()
            if calculated_md5.lower() != tracker.md5_hash.lower():
                await self.send_error(websocket, device_id, "HASH_MISMATCH",
                                    f"MD5 hash mismatch: expected {tracker.md5_hash}, got {calculated_md5}")
                return
            
            # Save file
            await self.session_manager.save_uploaded_file(device_id, tracker.filename, file_data)
            
            logger.info(f"Upload completed successfully: {tracker.filename}")
            
            # Send success response
            upload_end_response = UploadEndPayload(
                filename=tracker.filename,
                totalChunks=len(tracker.data_chunks),
                success=True,
                message="Upload completed successfully"
            )
            
            await self.send_message(websocket, device_id, MessageType.UPLOAD_END, upload_end_response)
            
        except Exception as e:
            logger.error(f"Failed to finalize upload: {e}")
            await self.send_error(websocket, device_id, "UPLOAD_FAILED", str(e))
    
    async def send_message(self, websocket: WebSocketServerProtocol, device_id: str,
                          msg_type: MessageType, payload: MessagePayload,
                          session_id: Optional[str] = None):
        """Send a message to a WebSocket client"""
        try:
            envelope = MessageEnvelope.create(
                msg_id=str(uuid.uuid4()),
                msg_type=msg_type,
                device_id=device_id,
                payload=payload,
                session_id=session_id
            )
            
            message = envelope.model_dump_json()
            await websocket.send(message)
            
            logger.debug(f"Sent {msg_type} message to {device_id}")
            
        except Exception as e:
            logger.error(f"Failed to send message: {e}")
    
    async def send_error(self, websocket: WebSocketServerProtocol, device_id: str,
                        error_code: str, error_message: str):
        """Send an error message to a WebSocket client"""
        error_payload = ErrorPayload(code=error_code, message=error_message)
        await self.send_message(websocket, device_id, MessageType.ERROR, error_payload)
    
    async def ping_loop(self, websocket: WebSocketServerProtocol, device_id: str):
        """Ping loop for keeping connection alive"""
        try:
            while True:
                await asyncio.sleep(self.ping_interval)
                
                # Check if device is still connected
                if device_id not in self.connected_devices:
                    break
                
                # Send ping
                await self.send_message(websocket, device_id, MessageType.PING, EmptyPayload())
                
        except asyncio.CancelledError:
            logger.debug(f"Ping loop cancelled for device {device_id}")
        except Exception as e:
            logger.error(f"Error in ping loop for device {device_id}: {e}")
    
    async def cleanup_connection(self, websocket: WebSocketServerProtocol):
        """Clean up after connection close"""
        # Find and remove device
        device_to_remove = None
        for device_id, device in self.connected_devices.items():
            if device.websocket == websocket:
                device_to_remove = device_id
                break
        
        if device_to_remove:
            del self.connected_devices[device_to_remove]
            del self.device_connections[device_to_remove]
            logger.info(f"Device {device_to_remove} disconnected")
        
        # Cancel ping tasks
        tasks_to_remove = set()
        for task in self.ping_tasks:
            if task.done() or task.cancelled():
                tasks_to_remove.add(task)
        
        for task in tasks_to_remove:
            self.ping_tasks.remove(task)
    
    def get_connected_devices(self) -> Dict[str, ConnectedDevice]:
        """Get all connected devices"""
        return self.connected_devices.copy()
    
    def is_device_connected(self, device_id: str) -> bool:
        """Check if a device is connected"""
        return device_id in self.connected_devices