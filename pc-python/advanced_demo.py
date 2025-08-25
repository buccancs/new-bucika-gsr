#!/usr/bin/env python3
"""
Advanced Demo - Bucika GSR PC Orchestrator

This demo showcases all the advanced features of the Python PC orchestrator including:
- Data analysis and quality assessment
- Error recovery and fault tolerance
- Comprehensive validation systems
- Performance monitoring and optimization
- Real-time metrics and reporting
"""

import asyncio
import json
import time
import random
from datetime import datetime, timedelta
from pathlib import Path
import signal
import sys

from loguru import logger
from src.bucika_gsr_pc import (
    BucikaOrchestrator, 
    BatchAnalyzer,
    BatchValidator,
    ValidationLevel,
    ErrorRecoveryManager
)


class AdvancedDemo:
    """Advanced demo showcasing all orchestrator features"""
    
    def __init__(self):
        self.orchestrator = None
        self.demo_running = False
        self.shutdown_event = asyncio.Event()
        
    async def start_demo(self):
        """Start the advanced demo"""
        logger.info("🚀 Starting Bucika GSR PC Orchestrator Advanced Demo")
        logger.info("=" * 60)
        
        try:
            # Initialize orchestrator with research-grade validation
            self.orchestrator = BucikaOrchestrator(
                headless=True,
                validation_level="research_grade"
            )
            
            # Start all services
            await self.orchestrator.start()
            self.demo_running = True
            
            # Display service status
            await self._display_service_status()
            
            # Generate sample data if none exists
            await self._generate_sample_data_if_needed()
            
            # Demonstrate advanced features
            await self._demonstrate_data_analysis()
            await self._demonstrate_validation_system()
            await self._demonstrate_error_recovery()
            await self._demonstrate_performance_monitoring()
            
            # Real-time monitoring loop
            await self._start_monitoring_loop()
            
        except Exception as e:
            logger.error(f"Demo failed: {e}")
            raise
        finally:
            if self.orchestrator:
                await self.orchestrator.stop()
    
    async def _display_service_status(self):
        """Display comprehensive service status"""
        logger.info("📊 Service Status Report:")
        logger.info("-" * 40)
        
        status = self.orchestrator.get_status()
        
        # Core services status
        services = status['services']
        
        ws_status = "🟢 OPERATIONAL" if services['websocket_server']['running'] else "🔴 OFFLINE"
        logger.info(f"🌐 WebSocket Server: localhost:8080 - {ws_status}")
        
        mdns_status = "🟢 BROADCASTING" if services['discovery_service']['running'] else "🔴 OFFLINE"
        logger.info(f"🔍 mDNS Discovery: _bucika-gsr._tcp - {mdns_status}")
        
        time_status = "🟢 HIGH PRECISION" if services['time_sync_service']['running'] else "🔴 OFFLINE"
        logger.info(f"⏰ Time Sync Service: UDP port 9123 - {time_status}")
        
        perf_status = "🟢 ACTIVE" if services['performance_monitor']['running'] else "🔴 INACTIVE"
        logger.info(f"📈 Performance Monitor: {perf_status}")
        
        error_status = "🟢 MONITORING" if services['error_recovery']['running'] else "🔴 INACTIVE"
        recovery_rate = services['error_recovery']['recovery_rate']
        logger.info(f"🛡️ Error Recovery: {error_status} (Recovery: {recovery_rate:.1%})")
        
        logger.info("")
        logger.info("🎯 Available Capabilities:")
        capabilities = [
            "✅ WebSocket communication protocol",
            "✅ mDNS automatic device discovery", 
            "✅ High-precision time synchronization",
            "✅ Session lifecycle management",
            "✅ GSR data streaming (128Hz)",
            "✅ Sync mark recording with CSV storage",
            "✅ Chunked file upload with MD5 verification",
            "✅ Real-time performance monitoring",
            "✅ Advanced error recovery and fault tolerance",
            "✅ Research-grade data validation",
            "✅ Comprehensive data analysis tools",
            "✅ Automated quality assurance"
        ]
        
        for capability in capabilities:
            logger.info(f"   {capability}")
        
        logger.info("")
    
    async def _generate_sample_data_if_needed(self):
        """Generate sample session data for demonstration if none exists"""
        sessions_dir = self.orchestrator.data_directory
        
        # Check if we have any existing sessions
        existing_sessions = [d for d in sessions_dir.iterdir() if d.is_dir()]
        
        if len(existing_sessions) < 2:
            logger.info("📁 Generating sample session data for demonstration...")
            
            for i in range(3):
                session_id = f"demo_session_{i+1}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
                session_path = sessions_dir / session_id
                session_path.mkdir(exist_ok=True)
                
                # Generate sample GSR data
                await self._generate_sample_gsr_data(session_path, duration_minutes=2 + i)
                
                # Generate sample sync marks
                await self._generate_sample_sync_marks(session_path, count=2 + i)
                
                # Generate metadata
                await self._generate_sample_metadata(session_path, session_id)
                
            logger.info(f"✅ Generated 3 sample sessions in {sessions_dir}")
        else:
            logger.info(f"📊 Found {len(existing_sessions)} existing sessions")
    
    async def _generate_sample_gsr_data(self, session_path: Path, duration_minutes: int = 2):
        """Generate realistic sample GSR data"""
        csv_file = session_path / f"gsr_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
        
        # Simulate 128 Hz sampling
        samples_per_minute = 128 * 60
        total_samples = duration_minutes * samples_per_minute
        
        start_time = time.time()
        
        with open(csv_file, 'w', newline='') as f:
            f.write("timestamp,datetime,gsr_microsiemens,temperature_celsius,signal_quality\n")
            
            base_gsr = random.uniform(2.0, 8.0)  # Baseline GSR
            
            for i in range(total_samples):
                timestamp = start_time + (i / 128.0)
                datetime_str = datetime.fromtimestamp(timestamp).isoformat()
                
                # Add realistic variation and occasional spikes
                variation = random.gauss(0, 0.3)
                if random.random() < 0.01:  # 1% chance of spike
                    variation += random.uniform(1.0, 3.0)
                
                gsr_value = max(0.1, base_gsr + variation)
                temperature = random.uniform(32.0, 37.0)
                quality = random.uniform(0.85, 1.0)
                
                f.write(f"{timestamp},{datetime_str},{gsr_value:.3f},{temperature:.2f},{quality:.3f}\n")
    
    async def _generate_sample_sync_marks(self, session_path: Path, count: int = 3):
        """Generate sample synchronization marks"""
        sync_file = session_path / "sync_marks.csv"
        
        with open(sync_file, 'w', newline='') as f:
            f.write("timestamp,marker_id,description\n")
            
            base_time = time.time()
            
            descriptions = [
                "Baseline recording start",
                "Stimulus presentation",
                "Task completion", 
                "Recovery period",
                "Session end marker"
            ]
            
            for i in range(count):
                timestamp = base_time + (i * 30)  # 30 seconds apart
                marker_id = f"SYNC_{i+1:03d}"
                description = descriptions[i % len(descriptions)]
                
                f.write(f"{timestamp},{marker_id},{description}\n")
    
    async def _generate_sample_metadata(self, session_path: Path, session_id: str):
        """Generate sample session metadata"""
        metadata_file = session_path / "session_metadata.json"
        
        metadata = {
            "session_id": session_id,
            "start_time": datetime.now().isoformat(),
            "participant_id": f"P{random.randint(1000, 9999)}",
            "device_info": {
                "device_id": "demo_device",
                "android_version": "14.0",
                "app_version": "1.2.0"
            },
            "session_config": {
                "sampling_rate": 128,
                "recording_duration": "120s",
                "sync_enabled": True
            },
            "notes": "Generated demo session for advanced feature demonstration"
        }
        
        with open(metadata_file, 'w') as f:
            json.dump(metadata, f, indent=2)
    
    async def _demonstrate_data_analysis(self):
        """Demonstrate advanced data analysis capabilities"""
        logger.info("🔬 Demonstrating Advanced Data Analysis...")
        logger.info("-" * 45)
        
        # Create batch analyzer
        batch_analyzer = BatchAnalyzer(self.orchestrator.data_directory)
        
        # Analyze all sessions
        results = await asyncio.get_event_loop().run_in_executor(
            None, batch_analyzer.analyze_all_sessions
        )
        
        if results:
            logger.info(f"📊 Analysis Results for {len(results)} Sessions:")
            
            for session_id, analysis in results.items():
                logger.info(f"")
                logger.info(f"Session: {session_id}")
                logger.info(f"  Duration: {analysis.duration_minutes:.1f} minutes")
                logger.info(f"  Samples: {analysis.sample_count:,}")
                logger.info(f"  Mean GSR: {analysis.mean_gsr:.3f} μS")
                logger.info(f"  Quality Score: {analysis.data_quality_score:.1%}")
                logger.info(f"  Sync Marks: {analysis.sync_mark_count}")
                logger.info(f"  Artifacts: {analysis.artifacts_detected}")
                
                if analysis.recommendations:
                    logger.info(f"  Recommendations:")
                    for rec in analysis.recommendations[:2]:  # Show first 2
                        logger.info(f"    • {rec}")
            
            # Generate batch report
            report_path = await asyncio.get_event_loop().run_in_executor(
                None, batch_analyzer.generate_batch_report
            )
            logger.info(f"📈 Batch analysis report saved: {report_path}")
            
            # Generate visualizations for first session
            first_session = next(iter(results.keys()))
            viz_path = await asyncio.get_event_loop().run_in_executor(
                None, self.orchestrator.data_analyzer.generate_visualization, first_session
            )
            if viz_path:
                logger.info(f"📊 Visualization generated: {viz_path}")
        
        else:
            logger.warning("No analysis results - check session data")
        
        logger.info("")
    
    async def _demonstrate_validation_system(self):
        """Demonstrate comprehensive validation system"""
        logger.info("✅ Demonstrating Research-Grade Validation System...")
        logger.info("-" * 52)
        
        # Create batch validator with research-grade level
        batch_validator = BatchValidator(ValidationLevel.RESEARCH_GRADE)
        
        # Validate all sessions
        reports = await batch_validator.validate_all_sessions(self.orchestrator.data_directory)
        
        if reports:
            logger.info(f"🔍 Validation Results for {len(reports)} Sessions:")
            
            total_score = 0
            passed_count = 0
            
            for session_id, report in reports.items():
                passed = report.overall_score >= 0.8
                status = "✅ PASS" if passed else "❌ FAIL"
                
                logger.info(f"")
                logger.info(f"Session: {session_id} - {status}")
                logger.info(f"  Overall Score: {report.overall_score:.1%}")
                logger.info(f"  Validation Level: {report.validation_level.value}")
                
                # Show key validation results
                metric_scores = {}
                for result in report.results:
                    metric = result.metric.value
                    if metric not in metric_scores:
                        metric_scores[metric] = []
                    metric_scores[metric].append(result.score)
                
                for metric, scores in metric_scores.items():
                    avg_score = sum(scores) / len(scores)
                    logger.info(f"    {metric.title()}: {avg_score:.1%}")
                
                if report.recommendations:
                    logger.info(f"  Key Recommendations:")
                    for rec in report.recommendations[:2]:  # Show first 2
                        logger.info(f"    • {rec}")
                
                total_score += report.overall_score
                if passed:
                    passed_count += 1
            
            # Overall statistics
            avg_score = total_score / len(reports)
            pass_rate = passed_count / len(reports)
            
            logger.info(f"")
            logger.info(f"📊 Overall Validation Statistics:")
            logger.info(f"  Average Score: {avg_score:.1%}")
            logger.info(f"  Pass Rate: {pass_rate:.1%} ({passed_count}/{len(reports)})")
            
            # Generate summary report
            summary_path = await asyncio.get_event_loop().run_in_executor(
                None, batch_validator.generate_summary_report
            )
            logger.info(f"📋 Validation summary saved: {summary_path}")
        
        else:
            logger.warning("No validation results - check session data")
        
        logger.info("")
    
    async def _demonstrate_error_recovery(self):
        """Demonstrate advanced error recovery system"""
        logger.info("🛡️ Demonstrating Advanced Error Recovery System...")
        logger.info("-" * 49)
        
        # Simulate some errors to show recovery capabilities
        error_scenarios = [
            ("Connection timeout", ConnectionError("WebSocket connection timed out")),
            ("Service unavailable", RuntimeError("mDNS service temporarily unavailable")), 
            ("Data validation error", ValueError("Invalid GSR reading: -1.5 μS")),
            ("Memory warning", MemoryError("Low memory condition detected")),
            ("File access issue", IOError("Permission denied accessing session file"))
        ]
        
        recovery_manager = self.orchestrator.error_recovery
        
        logger.info("Simulating error scenarios and recovery attempts:")
        logger.info("")
        
        for scenario_name, error in error_scenarios:
            logger.info(f"🔍 Scenario: {scenario_name}")
            
            # Handle the error
            success = await recovery_manager.handle_error("demo_service", error)
            
            status = "✅ RECOVERED" if success else "❌ ESCALATED"
            logger.info(f"   Result: {status}")
            
            # Brief delay between scenarios
            await asyncio.sleep(0.5)
        
        # Show error recovery statistics
        error_report = recovery_manager.get_error_report()
        
        logger.info(f"")
        logger.info(f"📈 Error Recovery Statistics:")
        logger.info(f"  Total Errors: {error_report['overall_stats']['total_errors']}")
        logger.info(f"  Recovered: {error_report['overall_stats']['recovered_errors']}")
        logger.info(f"  Recovery Rate: {error_report['overall_stats']['recovery_rate']:.1%}")
        logger.info(f"  Active Recoveries: {error_report['active_recoveries']}")
        
        if error_report['error_by_severity']:
            logger.info(f"  Errors by Severity:")
            for severity, count in error_report['error_by_severity'].items():
                logger.info(f"    {severity.title()}: {count}")
        
        logger.info("")
    
    async def _demonstrate_performance_monitoring(self):
        """Demonstrate real-time performance monitoring"""
        logger.info("⚡ Demonstrating Real-Time Performance Monitoring...")
        logger.info("-" * 54)
        
        # Get current performance report
        perf_report = self.orchestrator.get_performance_report()
        
        logger.info("📊 Current System Performance:")
        
        if 'system_metrics' in perf_report:
            sys_metrics = perf_report['system_metrics']
            
            logger.info(f"  CPU Usage: {sys_metrics.get('cpu_percent', 0):.1f}%")
            logger.info(f"  Memory Usage: {sys_metrics.get('memory_percent', 0):.1f}%")
            logger.info(f"  Available Memory: {sys_metrics.get('memory_available_mb', 0):.0f} MB")
            
            if 'network_stats' in sys_metrics:
                net_stats = sys_metrics['network_stats']
                logger.info(f"  Network Bytes Sent: {net_stats.get('bytes_sent', 0):,}")
                logger.info(f"  Network Bytes Received: {net_stats.get('bytes_recv', 0):,}")
        
        if 'application_metrics' in perf_report:
            app_metrics = perf_report['application_metrics']
            
            logger.info(f"")
            logger.info(f"🎯 Application Performance:")
            logger.info(f"  Connected Clients: {app_metrics.get('connected_clients', 0)}")
            logger.info(f"  Messages Processed: {app_metrics.get('messages_processed', 0):,}")
            logger.info(f"  Average Response Time: {app_metrics.get('avg_response_time_ms', 0):.2f} ms")
            logger.info(f"  Error Rate: {app_metrics.get('error_rate', 0):.2%}")
        
        # Show optimization status
        if 'optimizations' in perf_report:
            optimizations = perf_report['optimizations']
            logger.info(f"")
            logger.info(f"🔧 Performance Optimizations:")
            for opt_name, enabled in optimizations.items():
                status = "✅ ENABLED" if enabled else "❌ DISABLED"
                logger.info(f"  {opt_name.replace('_', ' ').title()}: {status}")
        
        logger.info("")
    
    async def _start_monitoring_loop(self):
        """Start real-time monitoring loop"""
        logger.info("🔄 Starting Real-Time Monitoring Loop...")
        logger.info("   Press Ctrl+C to stop the demo")
        logger.info("-" * 60)
        
        # Setup signal handler
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
        
        monitoring_interval = 15  # seconds
        last_report_time = datetime.now()
        
        while self.demo_running:
            try:
                await asyncio.sleep(monitoring_interval)
                
                current_time = datetime.now()
                
                # Periodic status report
                logger.info(f"📡 Status Update [{current_time.strftime('%H:%M:%S')}]:")
                
                # Service health check
                status = self.orchestrator.get_status()
                services_ok = sum(1 for svc in status['services'].values() 
                                if svc.get('running', False))
                total_services = len(status['services'])
                
                logger.info(f"   Services: {services_ok}/{total_services} operational")
                
                # Performance snapshot
                perf_report = self.orchestrator.get_performance_report()
                if 'system_metrics' in perf_report:
                    sys_metrics = perf_report['system_metrics']
                    cpu = sys_metrics.get('cpu_percent', 0)
                    memory = sys_metrics.get('memory_percent', 0)
                    logger.info(f"   Resources: CPU {cpu:.1f}%, Memory {memory:.1f}%")
                
                # Error recovery status
                error_report = self.orchestrator.get_error_report()
                total_errors = error_report['overall_stats']['total_errors']
                recovery_rate = error_report['overall_stats']['recovery_rate']
                logger.info(f"   Error Recovery: {total_errors} total, {recovery_rate:.1%} success rate")
                
                # Connected clients
                ws_status = status['services']['websocket_server']
                client_count = ws_status.get('connected_clients', 0)
                logger.info(f"   Connected Clients: {client_count}")
                
                logger.info("")
                
                # Generate comprehensive report every 5 minutes
                if (current_time - last_report_time).total_seconds() >= 300:  # 5 minutes
                    await self._generate_comprehensive_report()
                    last_report_time = current_time
                    
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in monitoring loop: {e}")
                await self.orchestrator.error_recovery.handle_error("monitoring_loop", e)
    
    async def _generate_comprehensive_report(self):
        """Generate and save comprehensive status report"""
        logger.info("📋 Generating Comprehensive Status Report...")
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'orchestrator_status': self.orchestrator.get_status(),
            'performance_metrics': self.orchestrator.get_performance_report(),
            'error_recovery_stats': self.orchestrator.get_error_report(),
            'data_analysis_summary': await self._get_data_analysis_summary(),
            'validation_summary': await self._get_validation_summary()
        }
        
        # Save report
        report_path = Path(f"comprehensive_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
        
        try:
            with open(report_path, 'w') as f:
                json.dump(report, f, indent=2, default=str)
            
            logger.info(f"✅ Comprehensive report saved: {report_path}")
            
        except Exception as e:
            logger.error(f"Error saving comprehensive report: {e}")
    
    async def _get_data_analysis_summary(self):
        """Get summary of data analysis results"""
        try:
            sessions = [d for d in self.orchestrator.data_directory.iterdir() if d.is_dir()]
            
            return {
                'total_sessions': len(sessions),
                'last_updated': datetime.now().isoformat(),
                'analysis_available': len(sessions) > 0
            }
        except Exception:
            return {'error': 'Unable to generate data analysis summary'}
    
    async def _get_validation_summary(self):
        """Get summary of validation results"""
        try:
            sessions = [d for d in self.orchestrator.data_directory.iterdir() if d.is_dir()]
            
            return {
                'total_sessions': len(sessions),
                'validation_level': 'research_grade',
                'last_updated': datetime.now().isoformat()
            }
        except Exception:
            return {'error': 'Unable to generate validation summary'}
    
    def _signal_handler(self, signum, frame):
        """Handle shutdown signals"""
        logger.info(f"\n🛑 Received shutdown signal ({signum})")
        logger.info("Gracefully stopping advanced demo...")
        
        self.demo_running = False
        self.shutdown_event.set()


async def main():
    """Main demo entry point"""
    # Configure enhanced logging
    logger.remove()
    logger.add(
        sys.stdout,
        level="INFO",
        format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>",
        colorize=True
    )
    
    # Also log to file
    logger.add(
        f"advanced_demo_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log",
        level="DEBUG",
        format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {name}:{function}:{line} - {message}"
    )
    
    demo = AdvancedDemo()
    
    try:
        await demo.start_demo()
    except KeyboardInterrupt:
        logger.info("\n👋 Demo interrupted by user")
    except Exception as e:
        logger.error(f"💥 Demo failed with error: {e}")
        raise
    finally:
        logger.info("🎯 Advanced Demo completed successfully!")
        logger.info("Thank you for exploring the Bucika GSR PC Orchestrator!")


if __name__ == "__main__":
    asyncio.run(main())