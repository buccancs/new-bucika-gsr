"""
Bucika GSR PC Orchestrator - Advanced Python Implementation

A comprehensive solution for coordinating GSR data collection across multiple Android devices
with advanced features including real-time analysis, error recovery, and quality assurance.

Features:
- WebSocket communication server with device management
- mDNS service discovery for automatic device detection
- High-precision time synchronization (UDP/NTP-style)
- Session lifecycle management with state tracking
- Real-time GSR data streaming and storage
- Sync mark recording with persistent CSV storage
- Performance monitoring with resource tracking
- Data analysis and quality assessment tools
- Advanced error recovery and fault tolerance
- Comprehensive validation and integrity checking
"""

from .websocket_server import WebSocketServer
from .discovery_service import DiscoveryService
from .time_sync_service import TimeSyncService
from .session_manager import SessionManager
from .performance_monitor import PerformanceMonitor
from .protocol import *
from .data_analyzer import GSRDataAnalyzer, BatchAnalyzer, AnalysisResults
from .error_recovery import ErrorRecoveryManager, ServiceErrorHandler, ErrorSeverity, RecoveryAction
from .data_validator import DataValidator, BatchValidator, QualityReport, ValidationLevel

# Try to import GUI, but make it optional for headless environments
try:
    from .gui import BucikaGSRGUI, MainWindow
    GUI_AVAILABLE = True
except ImportError:
    BucikaGSRGUI = None
    MainWindow = None
    GUI_AVAILABLE = False

import asyncio
import signal
from pathlib import Path
from loguru import logger
from typing import Optional, Dict, Any

__version__ = "1.0.0"
__author__ = "Bucika GSR Team"


class BucikaOrchestrator:
    """Main orchestrator class coordinating all services"""
    
    def __init__(self, headless: bool = False, 
                 data_directory: Path = None,
                 validation_level: str = "standard"):
        """
        Initialize the Bucika GSR PC Orchestrator
        
        Args:
            headless: Run without GUI (default: False)
            data_directory: Directory for session data (default: ./sessions)
            validation_level: Data validation strictness ("basic", "standard", "strict", "research_grade")
        """
        self.headless = headless or not GUI_AVAILABLE
        self.data_directory = data_directory or Path("sessions")
        self.data_directory.mkdir(exist_ok=True)
        
        # Core services
        self.session_manager = SessionManager(self.data_directory)
        self.time_sync_service = TimeSyncService()
        self.websocket_server = WebSocketServer(
            port=8080,
            session_manager=self.session_manager,
            time_sync_service=self.time_sync_service
        )
        self.discovery_service = DiscoveryService()
        self.performance_monitor = PerformanceMonitor()
        
        # Advanced services
        self.error_recovery = ErrorRecoveryManager()
        self.data_analyzer = GSRDataAnalyzer(self.data_directory)
        
        # Validation level mapping
        validation_levels = {
            "basic": ValidationLevel.BASIC,
            "standard": ValidationLevel.STANDARD,
            "strict": ValidationLevel.STRICT,
            "research_grade": ValidationLevel.RESEARCH_GRADE
        }
        val_level = validation_levels.get(validation_level.lower(), ValidationLevel.STANDARD)
        self.data_validator = DataValidator(val_level)
        
        # GUI (if not headless and available)
        if not self.headless and GUI_AVAILABLE:
            if BucikaGSRGUI:
                self.gui = BucikaGSRGUI(self)
            elif MainWindow:
                self.gui = MainWindow(
                    session_manager=self.session_manager,
                    websocket_server=self.websocket_server,
                    discovery_service=self.discovery_service
                )
            else:
                self.gui = None
        else:
            self.gui = None
            if not headless and not GUI_AVAILABLE:
                logger.warning("GUI requested but tkinter not available. Running in headless mode.")
                self.headless = True
        
        # State
        self.running = False
        self._shutdown_event = asyncio.Event()
        
        # Setup error recovery callbacks
        self._setup_error_recovery()
        
        logger.info(f"Bucika GSR Orchestrator initialized (headless: {self.headless})")
        logger.info(f"Data directory: {self.data_directory}")
        logger.info(f"Validation level: {validation_level}")
        
    def _setup_error_recovery(self):
        """Setup error recovery for all services"""
        
        # WebSocket server error handling
        ws_handler = ServiceErrorHandler("websocket_server", self.error_recovery)
        ws_handler.set_restart_callback(self._restart_websocket_server)
        ws_handler.set_reset_callback(self._reset_websocket_state)
        
        # Discovery service error handling
        discovery_handler = ServiceErrorHandler("discovery_service", self.error_recovery)
        discovery_handler.set_restart_callback(self._restart_discovery_service)
        
        # Time sync service error handling
        time_sync_handler = ServiceErrorHandler("time_sync_service", self.error_recovery)
        time_sync_handler.set_restart_callback(self._restart_time_sync_service)
        
        # Session manager error handling
        session_handler = ServiceErrorHandler("session_manager", self.error_recovery)
        session_handler.set_reset_callback(self._reset_session_state)
        
    async def start(self):
        """Start all orchestrator services"""
        if self.running:
            logger.warning("Orchestrator is already running")
            return
            
        logger.info("Starting Bucika GSR Orchestrator v1.0.0 (Python)")
        
        try:
            # Start error recovery first
            await self.error_recovery.start()
            
            # Start performance monitoring
            await self.performance_monitor.start()
            logger.info("Performance monitoring started")
            
            # Start time sync service
            await self.time_sync_service.start()
            logger.info("Time sync service started on UDP port 9123")
            
            # Start mDNS discovery
            await self.discovery_service.start()
            logger.info("mDNS service registration started in background")
            
            # Start WebSocket server
            await self.websocket_server.start()
            logger.info("WebSocket server started on port 8080")
            
            # Start GUI if not headless
            if self.gui and hasattr(self.gui, 'start'):
                self.gui.start()
                logger.info("GUI started")
            
            self.running = True
            logger.info("All services started successfully")
            
            # Setup signal handlers for graceful shutdown
            if self.headless:
                signal.signal(signal.SIGINT, self._signal_handler)
                signal.signal(signal.SIGTERM, self._signal_handler)
            
        except Exception as e:
            logger.error(f"Error starting orchestrator: {e}")
            await self.error_recovery.handle_error("orchestrator", e)
            raise
            
    async def stop(self):
        """Stop all orchestrator services"""
        if not self.running:
            logger.warning("Orchestrator is not running")
            return
            
        logger.info("Stopping Bucika GSR Orchestrator...")
        self.running = False
        
        try:
            # Stop GUI first
            if self.gui and hasattr(self.gui, 'stop'):
                self.gui.stop()
                logger.info("GUI stopped")
            
            # Stop WebSocket server
            await self.websocket_server.stop()
            logger.info("WebSocket server stopped")
            
            # Stop discovery service
            await self.discovery_service.stop()
            logger.info("mDNS service stopped")
            
            # Stop time sync service
            await self.time_sync_service.stop()
            logger.info("Time sync service stopped")
            
            # Stop performance monitoring
            await self.performance_monitor.stop()
            logger.info("Performance monitoring stopped")
            
            # Stop error recovery
            await self.error_recovery.stop()
            logger.info("Error recovery stopped")
            
            logger.info("All services stopped successfully")
            
        except Exception as e:
            logger.error(f"Error stopping orchestrator: {e}")
            
    def _signal_handler(self, signum, frame):
        """Handle shutdown signals"""
        logger.info(f"Received signal {signum}, initiating shutdown...")
        self._shutdown_event.set()
        
    async def wait_for_shutdown(self):
        """Wait for shutdown signal"""
        await self._shutdown_event.wait()
        
    # Error recovery callbacks
    async def _restart_websocket_server(self):
        """Restart WebSocket server"""
        try:
            await self.websocket_server.stop()
            await asyncio.sleep(1)
            await self.websocket_server.start()
            return True
        except Exception as e:
            logger.error(f"Failed to restart WebSocket server: {e}")
            return False
            
    async def _reset_websocket_state(self):
        """Reset WebSocket server state"""
        try:
            # Clear connected clients and reset state
            self.websocket_server.connected_clients.clear()
            return True
        except Exception as e:
            logger.error(f"Failed to reset WebSocket state: {e}")
            return False
            
    async def _restart_discovery_service(self):
        """Restart mDNS discovery service"""
        try:
            await self.discovery_service.stop()
            await asyncio.sleep(1)
            await self.discovery_service.start()
            return True
        except Exception as e:
            logger.error(f"Failed to restart discovery service: {e}")
            return False
            
    async def _restart_time_sync_service(self):
        """Restart time sync service"""
        try:
            await self.time_sync_service.stop()
            await asyncio.sleep(1)
            await self.time_sync_service.start()
            return True
        except Exception as e:
            logger.error(f"Failed to restart time sync service: {e}")
            return False
            
    async def _reset_session_state(self):
        """Reset session manager state"""
        try:
            # End any active sessions gracefully
            for session_id in list(self.session_manager.active_sessions.keys()):
                await self.session_manager.end_session(session_id)
            return True
        except Exception as e:
            logger.error(f"Failed to reset session state: {e}")
            return False
    
    # Public API methods
    def get_status(self) -> Dict[str, Any]:
        """Get comprehensive orchestrator status"""
        return {
            'running': self.running,
            'headless': self.headless,
            'data_directory': str(self.data_directory),
            'services': {
                'websocket_server': {
                    'running': self.websocket_server.running if hasattr(self.websocket_server, 'running') else False,
                    'connected_clients': len(self.websocket_server.connected_clients),
                    'port': 8080
                },
                'discovery_service': {
                    'running': self.discovery_service.is_running(),
                    'service_name': 'BucikaGSR'
                },
                'time_sync_service': {
                    'running': self.time_sync_service.is_running(),
                    'port': 9123
                },
                'performance_monitor': {
                    'running': self.performance_monitor.is_monitoring
                },
                'error_recovery': {
                    'running': self.error_recovery.running,
                    'total_errors': self.error_recovery.stats['total_errors'],
                    'recovery_rate': self.error_recovery.stats['recovery_rate']
                }
            },
            'session': {
                'active': len(self.session_manager.active_sessions) > 0,
                'session_count': len(self.session_manager.active_sessions),
                'session_ids': list(self.session_manager.active_sessions.keys())
            }
        }
    
    async def analyze_session(self, session_id: str) -> Optional[AnalysisResults]:
        """Analyze a specific session"""
        return await asyncio.get_event_loop().run_in_executor(
            None, self.data_analyzer.analyze_session, session_id
        )
    
    async def validate_session(self, session_id: str) -> Optional[QualityReport]:
        """Validate a specific session"""
        session_path = self.data_directory / session_id
        if session_path.exists():
            return await self.data_validator.validate_session(session_path)
        return None
    
    def get_performance_report(self) -> Dict[str, Any]:
        """Get current performance report"""
        return self.performance_monitor.get_performance_summary()
    
    def get_error_report(self) -> Dict[str, Any]:
        """Get current error report"""
        return self.error_recovery.get_error_report()


__all__ = [
    'BucikaOrchestrator',
    'WebSocketServer',
    'DiscoveryService', 
    'TimeSyncService',
    'SessionManager',
    'PerformanceMonitor',
    'GSRDataAnalyzer',
    'BatchAnalyzer', 
    'AnalysisResults',
    'ErrorRecoveryManager',
    'ServiceErrorHandler',
    'ErrorSeverity',
    'RecoveryAction',
    'DataValidator',
    'BatchValidator',
    'QualityReport',
    'ValidationLevel'
]