#!/usr/bin/env python3
"""
Integration tests for the Bucika GSR PC Orchestrator
"""

import unittest
import asyncio
import json
import websockets
import tempfile
import shutil
from pathlib import Path
from src.bucika_gsr_pc import BucikaOrchestrator
from src.bucika_gsr_pc.protocol import MessageType, HelloPayload, MessageEnvelope


class TestIntegration(unittest.TestCase):
    """Integration tests for the complete orchestrator"""
    
    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()
        self.orchestrator = None
    
    def tearDown(self):
        """Clean up test environment"""
        if self.orchestrator:
            asyncio.run(self.orchestrator.stop())
        shutil.rmtree(self.temp_dir)
    
    def test_websocket_server_startup(self):
        """Test that WebSocket server starts and accepts connections"""
        async def run_test():
            # Start orchestrator in headless mode
            self.orchestrator = BucikaOrchestrator(headless=True)
            
            # Start services
            await self.orchestrator.start()
            
            # Give services time to start
            await asyncio.sleep(1)
            
            # Try to connect to WebSocket server
            try:
                async with websockets.connect("ws://localhost:8080") as websocket:
                    # Send HELLO message
                    payload = HelloPayload(
                        version="1.0.0",
                        capabilities=["GSR"],
                        deviceName="Test Device"
                    )
                    
                    envelope = MessageEnvelope.create(
                        msg_id="test-hello",
                        msg_type=MessageType.HELLO,
                        device_id="test-device-123",
                        payload=payload
                    )
                    
                    await websocket.send(envelope.model_dump_json())
                    
                    # Wait for response
                    response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                    response_data = json.loads(response)
                    
                    # Verify we got an ACK
                    self.assertEqual(response_data["type"], "ACK")
                    
            except Exception as e:
                self.fail(f"WebSocket connection failed: {e}")
        
        asyncio.run(run_test())
    
    def test_service_discovery_startup(self):
        """Test that mDNS service discovery starts"""
        async def run_test():
            self.orchestrator = BucikaOrchestrator(headless=True)
            
            # Start services
            await self.orchestrator.start()
            
            # Give services time to start
            await asyncio.sleep(2)
            
            # Verify discovery service is running
            self.assertIsNotNone(self.orchestrator.discovery_service)
            self.assertTrue(self.orchestrator.discovery_service.is_running())
        
        asyncio.run(run_test())
    
    def test_time_sync_service_startup(self):
        """Test that time sync service starts"""
        async def run_test():
            self.orchestrator = BucikaOrchestrator(headless=True)
            
            # Start services  
            await self.orchestrator.start()
            
            # Give services time to start
            await asyncio.sleep(1)
            
            # Verify time sync service is running
            self.assertIsNotNone(self.orchestrator.time_sync_service)
            self.assertTrue(self.orchestrator.time_sync_service.is_running())
        
        asyncio.run(run_test())


if __name__ == "__main__":
    unittest.main()