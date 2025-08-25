"""
mDNS discovery service for automatic device discovery on the local network.
Maintains compatibility with the existing Kotlin implementation.
"""

import asyncio
import socket
from typing import Optional
from zeroconf import ServiceInfo, Zeroconf
from loguru import logger


class DiscoveryService:
    """mDNS service for advertising the PC orchestrator on the network"""
    
    def __init__(self, port: int = 8080, service_name: str = "BucikaGSR"):
        self.port = port
        self.service_name = service_name
        self.service_type = "_bucika-gsr._tcp.local."
        
        self.zeroconf: Optional[Zeroconf] = None
        self.service_info: Optional[ServiceInfo] = None
    
    async def start(self):
        """Start the mDNS discovery service"""
        try:
            # Get local IP address
            try:
                hostname = socket.gethostname()
                local_ip = socket.gethostbyname(hostname)
            except Exception as e:
                # Fallback to localhost if hostname resolution fails
                logger.warning(f"Failed to get hostname IP, using 127.0.0.1: {e}")
                hostname = "localhost"
                local_ip = "127.0.0.1"
            
            # Start zeroconf in a background thread since it can be blocking
            import threading
            
            def register_service():
                try:
                    # Create service info
                    self.service_info = ServiceInfo(
                        self.service_type,
                        f"{self.service_name}.{self.service_type}",
                        addresses=[socket.inet_aton(local_ip)],
                        port=self.port,
                        properties={
                            b"version": b"1.0.0",
                            b"protocol": b"BucikaGSR",
                            b"capabilities": b"websocket,sync,upload"
                        },
                        server=f"{hostname}.local."
                    )
                    
                    # Start zeroconf
                    self.zeroconf = Zeroconf()
                    self.zeroconf.register_service(self.service_info)
                    
                    logger.info(f"mDNS service started: {self.service_name} on {local_ip}:{self.port}")
                    
                except Exception as e:
                    logger.error(f"Failed to register mDNS service: {e}")
                    logger.warning("Continuing without mDNS discovery - clients will need manual connection")
            
            # Start registration in background thread
            thread = threading.Thread(target=register_service, daemon=True)
            thread.start()
            
            # Don't wait for completion - return immediately
            logger.info("mDNS service registration started in background")
            
        except Exception as e:
            # Log error but don't fail the entire startup
            logger.error(f"Failed to start mDNS discovery service: {e}")
            logger.warning("Continuing without mDNS discovery - clients will need manual connection")
    
    async def stop(self):
        """Stop the mDNS discovery service"""
        if self.zeroconf and self.service_info:
            try:
                self.zeroconf.unregister_service(self.service_info)
                self.zeroconf.close()
                logger.info("mDNS discovery service stopped")
            except Exception as e:
                logger.error(f"Error stopping mDNS service: {e}")
    
    def get_service_info(self) -> dict:
        """Get service information"""
        if not self.service_info:
            return {}
        
        return {
            "service_name": self.service_name,
            "service_type": self.service_type,
            "port": self.port,
            "addresses": [socket.inet_ntoa(addr) for addr in self.service_info.addresses],
            "properties": {k.decode(): v.decode() for k, v in self.service_info.properties.items()}
        }