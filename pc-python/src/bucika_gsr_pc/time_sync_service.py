"""
Time synchronization service providing high-precision time sync via UDP.
Maintains compatibility with the existing SNTP-like protocol.
"""

import asyncio
import struct
import time
from typing import Optional
from loguru import logger


class TimeSyncService:
    """UDP time synchronization service for precise timing"""
    
    def __init__(self, port: int = 9123):
        self.port = port
        self.transport: Optional[asyncio.DatagramTransport] = None
        self.protocol: Optional['TimeSyncProtocol'] = None
    
    async def start(self):
        """Start the time synchronization service"""
        try:
            loop = asyncio.get_running_loop()
            
            # Create UDP server
            self.transport, self.protocol = await loop.create_datagram_endpoint(
                lambda: TimeSyncProtocol(),
                local_addr=('0.0.0.0', self.port)
            )
            
            logger.info(f"Time sync service started on UDP port {self.port}")
            
        except Exception as e:
            logger.error(f"Failed to start time sync service: {e}")
            raise
    
    async def stop(self):
        """Stop the time synchronization service"""
        if self.transport:
            self.transport.close()
            logger.info("Time sync service stopped")
    
    def get_current_time_ns(self) -> int:
        """Get current time in nanoseconds since Unix epoch"""
        return time.time_ns()


class TimeSyncProtocol(asyncio.DatagramProtocol):
    """UDP protocol handler for time synchronization requests"""
    
    def connection_made(self, transport: asyncio.DatagramTransport):
        self.transport = transport
    
    def datagram_received(self, data: bytes, addr: tuple):
        """Handle incoming time sync request"""
        try:
            # Parse request (simple 8-byte timestamp request)
            if len(data) >= 8:
                client_timestamp = struct.unpack('>Q', data[:8])[0]
                
                # Get current server time with high precision
                server_time_ns = time.time_ns()
                
                # Prepare response with server time
                response = struct.pack('>QQ', client_timestamp, server_time_ns)
                
                # Send response
                self.transport.sendto(response, addr)
                
                logger.debug(f"Time sync response sent to {addr}")
                
            else:
                logger.warning(f"Invalid time sync request from {addr}: {len(data)} bytes")
                
        except Exception as e:
            logger.error(f"Error handling time sync request from {addr}: {e}")
    
    def error_received(self, exc: Exception):
        logger.error(f"Time sync protocol error: {exc}")
    
    def connection_lost(self, exc: Optional[Exception]):
        if exc:
            logger.error(f"Time sync connection lost: {exc}")
        else:
            logger.debug("Time sync connection closed")