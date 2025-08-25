#!/usr/bin/env python3
"""
Test suite for WebSocket server functionality
"""

import unittest
import asyncio
import json
import tempfile
import shutil
from pathlib import Path
from unittest.mock import Mock, patch, AsyncMock
from websockets.exceptions import ConnectionClosed
from src.bucika_gsr_pc.websocket_server import WebSocketServer
from src.bucika_gsr_pc.session_manager import SessionManager
from src.bucika_gsr_pc.protocol import MessageType, HelloPayload, MessageEnvelope


class TestWebSocketServer(unittest.TestCase):
    """Test WebSocket server functionality"""
    
    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()
        self.session_manager = SessionManager(base_path=Path(self.temp_dir))
        
        # Create a mock time sync service
        from src.bucika_gsr_pc.time_sync_service import TimeSyncService
        self.time_sync_service = TimeSyncService()
        
        self.server = WebSocketServer(
            port=8081,  # Use different port for tests
            session_manager=self.session_manager,
            time_sync_service=self.time_sync_service
        )
    
    def tearDown(self):
        """Clean up test environment"""
        if hasattr(self.server, '_server') and self.server._server:
            try:
                asyncio.get_event_loop().run_until_complete(self.server.stop())
            except Exception:
                pass
        shutil.rmtree(self.temp_dir)
    
    def test_server_initialization(self):
        """Test WebSocket server initialization"""
        self.assertEqual(self.server.host, "localhost")
        self.assertEqual(self.server.port, 8081)
        self.assertIsNotNone(self.server.session_manager)
        self.assertFalse(self.server.running)
        self.assertEqual(len(self.server.connected_clients), 0)
    
    def test_message_parsing(self):
        """Test message parsing functionality"""
        # Test valid message
        hello_payload = HelloPayload(
            deviceName="Test Device",
            capabilities=["GSR", "VIDEO"],
            batteryLevel=85,
            version="1.0.0"
        )
        
        envelope = MessageEnvelope.create(
            msg_id="test-123",
            msg_type=MessageType.HELLO,
            device_id="device-456",
            payload=hello_payload
        )
        
        message_json = envelope.model_dump_json()
        
        # Parse message
        parsed = self.server._parse_message(message_json)
        
        self.assertIsNotNone(parsed)
        self.assertEqual(parsed.id, "test-123")
        self.assertEqual(parsed.type, MessageType.HELLO)
        self.assertEqual(parsed.deviceId, "device-456")
    
    def test_invalid_message_handling(self):
        """Test handling of invalid messages"""
        invalid_messages = [
            "",  # Empty message
            "invalid json",  # Invalid JSON
            '{"incomplete": "message"}',  # Missing required fields
            '{"type": "INVALID_TYPE", "id": "123"}',  # Invalid message type
        ]
        
        for invalid_msg in invalid_messages:
            parsed = self.server._parse_message(invalid_msg)
            self.assertIsNone(parsed)
    
    def test_client_connection_tracking(self):
        """Test client connection tracking"""
        async def run_test():
            # Mock WebSocket connection
            mock_websocket = Mock()
            mock_websocket.remote_address = ("192.168.1.100", 12345)
            
            device_id = "test_device_001"
            
            # Add client
            self.server._add_client(device_id, mock_websocket)
            
            self.assertEqual(len(self.server.connected_clients), 1)
            self.assertIn(device_id, self.server.connected_clients)
            
            client_info = self.server.connected_clients[device_id]
            self.assertEqual(client_info['websocket'], mock_websocket)
            self.assertEqual(client_info['address'], "192.168.1.100:12345")
            
            # Remove client
            self.server._remove_client(device_id)
            
            self.assertEqual(len(self.server.connected_clients), 0)
            self.assertNotIn(device_id, self.server.connected_clients)
        
        asyncio.run(run_test())
    
    def test_hello_message_handling(self):
        """Test HELLO message handling"""
        async def run_test():
            # Mock WebSocket
            mock_websocket = Mock()
            mock_websocket.send = AsyncMock()
            
            # Create HELLO message
            hello_payload = HelloPayload(
                deviceName="Test Device",
                capabilities=["GSR", "VIDEO"],
                batteryLevel=85,
                version="1.0.0"
            )
            
            envelope = MessageEnvelope.create(
                msg_id="hello-123",
                msg_type=MessageType.HELLO,
                device_id="device-456",
                payload=hello_payload
            )
            
            # Handle message
            await self.server._handle_message(mock_websocket, envelope)
            
            # Should send REGISTER response
            mock_websocket.send.assert_called_once()
            
            # Check that client was added
            self.assertIn("device-456", self.server.connected_clients)
        
        asyncio.run(run_test())
    
    def test_start_message_handling(self):
        """Test START message handling"""
        async def run_test():
            device_id = "test_device_start"
            
            # Mock WebSocket and add client
            mock_websocket = Mock()
            mock_websocket.send = AsyncMock()
            self.server._add_client(device_id, mock_websocket)
            
            # Create START message
            start_envelope = MessageEnvelope.create(
                msg_id="start-123",
                msg_type=MessageType.START,
                device_id=device_id,
                payload={
                    "sessionName": "Test Session",
                    "participantId": "P001",
                    "metadata": {"experiment": "test"}
                }
            )
            
            # Handle message
            await self.server._handle_message(mock_websocket, start_envelope)
            
            # Should send STATUS response
            mock_websocket.send.assert_called()
            
            # Should create session
            session = self.session_manager.get_active_session(device_id)
            self.assertIsNotNone(session)
            self.assertEqual(session.session_name, "Test Session")
        
        asyncio.run(run_test())
    
    def test_stop_message_handling(self):
        """Test STOP message handling"""
        async def run_test():
            device_id = "test_device_stop"
            
            # Mock WebSocket and add client
            mock_websocket = Mock()
            mock_websocket.send = AsyncMock()
            self.server._add_client(device_id, mock_websocket)
            
            # Start a session first
            await self.session_manager.start_session(
                session_name="Test Session",
                device_id=device_id
            )
            
            # Create STOP message
            stop_envelope = MessageEnvelope.create(
                msg_id="stop-123",
                msg_type=MessageType.STOP,
                device_id=device_id,
                payload={}
            )
            
            # Handle message
            await self.server._handle_message(mock_websocket, stop_envelope)
            
            # Should send STATUS response
            mock_websocket.send.assert_called()
            
            # Session should be stopped
            session = self.session_manager.get_active_session(device_id)
            self.assertIsNone(session)
        
        asyncio.run(run_test())
    
    def test_sync_mark_message_handling(self):
        """Test SYNC_MARK message handling"""
        async def run_test():
            device_id = "test_device_sync"
            
            # Mock WebSocket and add client
            mock_websocket = Mock()
            mock_websocket.send = AsyncMock()
            self.server._add_client(device_id, mock_websocket)
            
            # Start a session first
            await self.session_manager.start_session(
                session_name="Sync Test Session",
                device_id=device_id
            )
            
            # Create SYNC_MARK message
            sync_envelope = MessageEnvelope.create(
                msg_id="sync-123",
                msg_type=MessageType.SYNC_MARK,
                device_id=device_id,
                payload={
                    "markId": "STIM_START",
                    "description": "Stimulus presentation started"
                }
            )
            
            # Handle message
            await self.server._handle_message(mock_websocket, sync_envelope)
            
            # Should send STATUS response
            mock_websocket.send.assert_called()
            
            # Should record sync mark
            session = self.session_manager.get_active_session(device_id)
            self.assertIsNotNone(session)
            self.assertEqual(len(session.sync_marks), 1)
            self.assertEqual(session.sync_marks[0]["mark_id"], "STIM_START")
        
        asyncio.run(run_test())
    
    def test_gsr_data_message_handling(self):
        """Test GSR_DATA message handling"""
        async def run_test():
            device_id = "test_device_gsr"
            
            # Mock WebSocket and add client
            mock_websocket = Mock()
            mock_websocket.send = AsyncMock()
            self.server._add_client(device_id, mock_websocket)
            
            # Start a session first
            await self.session_manager.start_session(
                session_name="GSR Test Session",
                device_id=device_id
            )
            
            # Create GSR_DATA message
            gsr_envelope = MessageEnvelope.create(
                msg_id="gsr-123",
                msg_type=MessageType.GSR_DATA,
                device_id=device_id,
                payload={
                    "samples": [{
                        "t_mono_ns": 1234567890123456789,
                        "t_utc_ns": 1234567890123456789,
                        "seq": 1,
                        "gsr_raw_uS": 2.5,
                        "gsr_filt_uS": 2.48,
                        "temp_C": 32.1,
                        "flag_spike": False,
                        "flag_sat": False,
                        "flag_dropout": False
                    }]
                }
            )
            
            # Handle message
            await self.server._handle_message(mock_websocket, gsr_envelope)
            
            # Should process GSR data
            session = self.session_manager.get_active_session(device_id)
            self.assertIsNotNone(session)
            self.assertEqual(len(session.gsr_samples), 1)
            self.assertEqual(session.gsr_samples[0].seq, 1)
        
        asyncio.run(run_test())
    
    def test_file_upload_handling(self):
        """Test FILE_UPLOAD message handling"""
        async def run_test():
            device_id = "test_device_upload"
            
            # Mock WebSocket and add client
            mock_websocket = Mock()
            mock_websocket.send = AsyncMock()
            self.server._add_client(device_id, mock_websocket)
            
            # Create FILE_UPLOAD message
            upload_envelope = MessageEnvelope.create(
                msg_id="upload-123",
                msg_type=MessageType.FILE_UPLOAD,
                device_id=device_id,
                payload={
                    "filename": "test_file.csv",
                    "size": 1024,
                    "checksum": "abc123def456",
                    "chunk_index": 0,
                    "total_chunks": 1,
                    "data": "VGVzdCBmaWxlIGNvbnRlbnQ="  # Base64 encoded data
                }
            )
            
            # Handle message
            await self.server._handle_message(mock_websocket, upload_envelope)
            
            # Should send response
            mock_websocket.send.assert_called()
        
        asyncio.run(run_test())
    
    def test_connection_cleanup(self):
        """Test connection cleanup on disconnect"""
        async def run_test():
            device_id = "test_cleanup_device"
            
            # Mock WebSocket
            mock_websocket = Mock()
            mock_websocket.remote_address = ("192.168.1.100", 12345)
            
            # Add client
            self.server._add_client(device_id, mock_websocket)
            self.assertEqual(len(self.server.connected_clients), 1)
            
            # Simulate disconnect
            await self.server._handle_disconnect(mock_websocket)
            
            # Client should be removed
            self.assertEqual(len(self.server.connected_clients), 0)
        
        asyncio.run(run_test())
    
    def test_broadcast_functionality(self):
        """Test message broadcasting to all clients"""
        async def run_test():
            # Add multiple mock clients
            mock_clients = {}
            for i in range(3):
                device_id = f"device_{i}"
                mock_websocket = Mock()
                mock_websocket.send = AsyncMock()
                mock_websocket.remote_address = ("192.168.1.100", 12340 + i)
                self.server._add_client(device_id, mock_websocket)
                mock_clients[device_id] = mock_websocket
            
            # Broadcast message
            test_message = {"type": "BROADCAST", "message": "Test broadcast"}
            await self.server.broadcast_message(test_message)
            
            # All clients should receive the message
            for mock_websocket in mock_clients.values():
                mock_websocket.send.assert_called_once()
        
        asyncio.run(run_test())
    
    def test_server_status_reporting(self):
        """Test server status reporting"""
        status = self.server.get_status()
        
        self.assertIn('running', status)
        self.assertIn('host', status)
        self.assertIn('port', status)
        self.assertIn('connected_clients', status)
        self.assertIn('total_messages_processed', status)
        
        self.assertEqual(status['host'], "localhost")
        self.assertEqual(status['port'], 8081)
        self.assertEqual(status['connected_clients'], 0)
        self.assertIsInstance(status['total_messages_processed'], int)
    
    def test_error_handling_during_message_processing(self):
        """Test error handling during message processing"""
        async def run_test():
            # Mock WebSocket
            mock_websocket = Mock()
            mock_websocket.send = AsyncMock()
            
            # Create invalid message that will cause error
            invalid_envelope = MessageEnvelope.create(
                msg_id="error-test",
                msg_type=MessageType.START,  # START without proper setup
                device_id="nonexistent_device",
                payload={}
            )
            
            # Should handle error gracefully
            try:
                await self.server._handle_message(mock_websocket, invalid_envelope)
            except Exception as e:
                self.fail(f"Message handling should not raise exception: {e}")
            
            # Should send error response
            mock_websocket.send.assert_called()
        
        asyncio.run(run_test())


if __name__ == "__main__":
    unittest.main()