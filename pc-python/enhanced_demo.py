#!/usr/bin/env python3
"""
Enhanced demo for the Bucika GSR PC Orchestrator with performance monitoring
"""

import asyncio
import json
import signal
import sys
from pathlib import Path

# Add the src directory to the Python path
sys.path.insert(0, str(Path(__file__).parent / 'src'))

from bucika_gsr_pc import BucikaOrchestrator
from loguru import logger


class EnhancedDemo:
    """Enhanced demo showcasing all features"""
    
    def __init__(self):
        self.orchestrator = None
        self.running = False
        
    async def run(self):
        """Run the enhanced demo"""
        try:
            # Initialize orchestrator with performance monitoring
            self.orchestrator = BucikaOrchestrator(headless=True)
            self.running = True
            
            # Set up signal handlers
            signal.signal(signal.SIGINT, self._signal_handler)
            signal.signal(signal.SIGTERM, self._signal_handler)
            
            logger.info("🚀 Starting Bucika GSR PC Orchestrator Enhanced Demo")
            logger.info("=" * 60)
            
            # Start the orchestrator (but don't let it wait for shutdown)
            await self._start_orchestrator_services()
            
            # Display service status
            await self._display_service_status()
            
            # Start performance monitoring loop
            await self._performance_monitoring_loop()
            
        except KeyboardInterrupt:
            logger.info("\n👋 Demo interrupted by user")
        except Exception as e:
            logger.error(f"❌ Demo failed: {e}")
        finally:
            await self._cleanup()
    
    async def _start_orchestrator_services(self):
        """Start orchestrator services without blocking"""
        logger.info("Starting Bucika GSR Orchestrator v1.0.0 (Python)")
        
        try:
            # Enable performance optimizations
            self.orchestrator.performance_optimizer.enable_optimizations()
            
            # Start performance monitoring
            await self.orchestrator.performance_monitor.start()
            
            # Start core services
            await self.orchestrator.time_sync_service.start()
            await self.orchestrator.discovery_service.start() 
            await self.orchestrator.websocket_server.start()
            
            logger.info("All services started successfully")
            
        except Exception as e:
            logger.error(f"Failed to start orchestrator: {e}")
            raise
    
    async def _display_service_status(self):
        """Display comprehensive service status"""
        logger.info("📊 Service Status Report:")
        logger.info("-" * 40)
        
        # WebSocket Server
        logger.info(f"🌐 WebSocket Server: localhost:8080 - OPERATIONAL")
        logger.info(f"🔍 mDNS Discovery: _bucika-gsr._tcp broadcasting")
        logger.info(f"⏰ Time Sync Service: UDP port 9123 - HIGH PRECISION")
        logger.info(f"📈 Performance Monitor: Real-time metrics enabled")
        
        # Display capabilities
        capabilities = [
            "✅ WebSocket communication protocol",
            "✅ mDNS automatic device discovery", 
            "✅ High-precision time synchronization",
            "✅ Session lifecycle management",
            "✅ GSR data streaming (128Hz)",
            "✅ Sync mark recording",
            "✅ Chunked file upload with MD5 verification",
            "✅ Real-time performance monitoring",
            "✅ Automatic performance optimization"
        ]
        
        logger.info("\n🎯 Available Capabilities:")
        for capability in capabilities:
            logger.info(f"   {capability}")
        
        logger.info("\n🔧 Performance Optimizations Active:")
        suggestions = self.orchestrator.performance_optimizer.suggest_optimizations()
        if suggestions:
            for suggestion in suggestions[:3]:  # Show top 3
                logger.info(f"   💡 {suggestion}")
        else:
            logger.info("   ✨ System running optimally")
        
        logger.info("\n" + "=" * 60)
        logger.info("📱 Ready for Android client connections!")
        logger.info("🔌 Connect your Android device to start data collection")
        logger.info("=" * 60)
    
    async def _performance_monitoring_loop(self):
        """Performance monitoring and reporting loop"""
        logger.info("📊 Real-time performance monitoring active...")
        logger.info("💾 Press Ctrl+C to stop and export metrics\n")
        
        report_interval = 30  # seconds
        last_report = 0
        
        while self.running:
            try:
                await asyncio.sleep(5)
                
                # Get latest metrics
                metrics = self.orchestrator.performance_monitor.get_latest_metrics()
                if metrics:
                    # Update session counts
                    active_sessions = len(self.orchestrator.session_manager.active_sessions)
                    connected_devices = len(self.orchestrator.websocket_server.connected_devices)
                    
                    self.orchestrator.performance_monitor.update_session_count(
                        active_sessions, connected_devices
                    )
                    
                    # Periodic detailed report
                    current_time = asyncio.get_event_loop().time()
                    if current_time - last_report >= report_interval:
                        await self._display_performance_report(metrics, active_sessions, connected_devices)
                        last_report = current_time
                
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Performance monitoring error: {e}")
    
    async def _display_performance_report(self, metrics, active_sessions: int, connected_devices: int):
        """Display detailed performance report"""
        logger.info("\n" + "📊 PERFORMANCE REPORT" + " " * 20 + f"{metrics.timestamp.strftime('%H:%M:%S')}")
        logger.info("─" * 60)
        
        # System metrics
        logger.info(f"🖥️  CPU Usage: {metrics.cpu_percent:.1f}%")
        logger.info(f"💾 Memory: {metrics.memory_mb:.1f} MB ({metrics.memory_percent:.1f}%)")
        logger.info(f"🌐 Network: ↑{metrics.network_sent_mb:.1f}MB ↓{metrics.network_recv_mb:.1f}MB")
        
        # Application metrics
        logger.info(f"📱 Connected Devices: {connected_devices}")
        logger.info(f"🎬 Active Sessions: {active_sessions}")
        logger.info(f"📨 Messages/sec: {metrics.messages_per_second:.1f}")
        logger.info(f"⚡ Avg Response: {metrics.avg_response_time_ms:.1f}ms")
        logger.info(f"❌ Error Rate: {metrics.error_rate_percent:.1f}%")
        
        # Performance summary
        summary = self.orchestrator.performance_monitor.get_performance_summary()
        if summary:
            logger.info("\n📈 15-Minute Summary:")
            logger.info(f"   Max CPU: {summary.get('max_cpu_percent', 0):.1f}%")
            logger.info(f"   Max Memory: {summary.get('max_memory_mb', 0):.1f}MB") 
            logger.info(f"   Avg Messages/sec: {summary.get('avg_messages_per_second', 0):.1f}")
        
        logger.info("─" * 60 + "\n")
    
    async def _export_performance_data(self):
        """Export performance data for analysis"""
        try:
            timestamp = self.orchestrator.performance_monitor.get_latest_metrics().timestamp
            export_filename = f"performance_metrics_{timestamp.strftime('%Y%m%d_%H%M%S')}.csv"
            
            self.orchestrator.performance_monitor.export_metrics_csv(
                export_filename, 
                duration_minutes=60
            )
            
            logger.info(f"📁 Performance data exported to: {export_filename}")
            
            # Display final summary
            summary = self.orchestrator.performance_monitor.get_performance_summary()
            if summary:
                logger.info("\n🎯 Final Performance Summary:")
                logger.info(f"   Average CPU: {summary.get('avg_cpu_percent', 0):.1f}%")
                logger.info(f"   Average Memory: {summary.get('avg_memory_mb', 0):.1f}MB")
                logger.info(f"   Total Network Sent: {summary.get('total_network_sent_mb', 0):.1f}MB")
                logger.info(f"   Total Network Received: {summary.get('total_network_recv_mb', 0):.1f}MB")
                logger.info(f"   Average Response Time: {summary.get('avg_response_time_ms', 0):.1f}ms")
                
        except Exception as e:
            logger.error(f"Failed to export performance data: {e}")
    
    def _signal_handler(self, signum, frame):
        """Handle shutdown signals"""
        logger.info(f"\n🛑 Received signal {signum}, initiating graceful shutdown...")
        self.running = False
    
    async def _cleanup(self):
        """Cleanup and shutdown"""
        logger.info("🧹 Cleaning up...")
        
        if self.orchestrator:
            # Export performance data before shutdown
            if hasattr(self.orchestrator, 'performance_monitor'):
                await self._export_performance_data()
            
            # Stop orchestrator
            await self.orchestrator.stop()
        
        logger.info("✅ Cleanup completed")
        logger.info("👋 Bucika GSR PC Orchestrator Enhanced Demo stopped")


async def main():
    """Main demo entry point"""
    demo = EnhancedDemo()
    await demo.run()


if __name__ == "__main__":
    # Configure enhanced logging
    logger.configure(
        handlers=[
            {
                "sink": sys.stdout,
                "level": "INFO",
                "format": "<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>"
            }
        ]
    )
    
    # Run the enhanced demo
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Demo interrupted")
    except Exception as e:
        logger.error(f"Demo failed: {e}")
        sys.exit(1)