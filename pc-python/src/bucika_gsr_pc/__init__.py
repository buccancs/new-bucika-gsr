#!/usr/bin/env python3
"""
Bucika GSR PC Orchestrator - Python Implementation

A complete Python implementation of the PC orchestrator for coordinating
GSR data collection from Android devices with WebSocket communication,
mDNS discovery, and comprehensive session management.
"""

import asyncio
import logging
from pathlib import Path
import signal
import sys
import argparse

from loguru import logger

from .websocket_server import WebSocketServer
from .discovery_service import DiscoveryService
from .time_sync_service import TimeSyncService
from .session_manager import SessionManager

# Try to import GUI, but make it optional for headless environments
try:
    from .gui import MainWindow
    GUI_AVAILABLE = True
except ImportError as e:
    # GUI dependencies not available (e.g., tkinter missing in headless environment)
    MainWindow = None
    GUI_AVAILABLE = False

__version__ = "1.0.0"
__author__ = "Bucika GSR Team"


class BucikaOrchestrator:
    """Main orchestrator class managing all services"""
    
    def __init__(self, headless: bool = False):
        self.headless = headless
        self.session_manager = SessionManager()
        self.time_sync_service = TimeSyncService()
        self.discovery_service = DiscoveryService()
        self.websocket_server = WebSocketServer(
            port=8080,
            session_manager=self.session_manager,
            time_sync_service=self.time_sync_service
        )
        
        # Only create GUI if not headless and GUI is available
        if not headless and GUI_AVAILABLE and MainWindow:
            self.main_window = MainWindow(
                session_manager=self.session_manager,
                websocket_server=self.websocket_server,
                discovery_service=self.discovery_service
            )
        else:
            self.main_window = None
            if not headless and not GUI_AVAILABLE:
                logger.warning("GUI requested but tkinter not available. Running in headless mode.")
        
    async def start(self):
        """Start all services"""
        logger.info("Starting Bucika GSR Orchestrator v1.0.0 (Python)")
        
        try:
            # Start core services
            await self.time_sync_service.start()
            await self.discovery_service.start()
            await self.websocket_server.start()
            
            logger.info("All services started successfully")
            logger.info(f"WebSocket server running on port 8080")
            logger.info(f"Time sync service running on port 9123")
            logger.info(f"mDNS discovery broadcasting")
            
            if self.headless or not self.main_window:
                # Console mode - just wait for shutdown
                logger.info("Running in headless mode. Press Ctrl+C to stop.")
                await self.wait_for_shutdown()
            else:
                # Start GUI
                self.main_window.start()
                
        except Exception as e:
            logger.error(f"Failed to start orchestrator: {e}")
            await self.stop()
            raise
    
    async def stop(self):
        """Stop all services"""
        logger.info("Stopping Bucika GSR Orchestrator")
        
        try:
            await self.websocket_server.stop()
            await self.discovery_service.stop()
            await self.time_sync_service.stop()
            logger.info("All services stopped successfully")
        except Exception as e:
            logger.error(f"Error during shutdown: {e}")
    
    async def wait_for_shutdown(self):
        """Wait for shutdown signal"""
        shutdown_event = asyncio.Event()
        
        def signal_handler(sig, frame):
            logger.info(f"Received signal {sig}, shutting down...")
            shutdown_event.set()
        
        # Register signal handlers
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)
        
        # Wait for shutdown signal
        await shutdown_event.wait()


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Bucika GSR PC Orchestrator')
    parser.add_argument('--headless', action='store_true', 
                       help='Run in headless console mode')
    parser.add_argument('--debug', action='store_true',
                       help='Enable debug logging')
    
    args = parser.parse_args()
    
    # Configure logging
    log_level = "DEBUG" if args.debug else "INFO"
    logger.configure(
        handlers=[
            {
                "sink": sys.stdout,
                "level": log_level,
                "format": "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>"
            }
        ]
    )
    
    orchestrator = BucikaOrchestrator(headless=args.headless)
    
    try:
        asyncio.run(orchestrator.start())
    except KeyboardInterrupt:
        logger.info("Shutdown requested")
    except Exception as e:
        logger.error(f"Orchestrator failed: {e}")
        sys.exit(1)
    finally:
        asyncio.run(orchestrator.stop())


if __name__ == "__main__":
    main()