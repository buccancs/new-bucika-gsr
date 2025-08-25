#!/usr/bin/env python3
"""
Integration tests for the Bucika GSR PC Orchestrator
"""

import unittest
import asyncio
import json
import tempfile
import shutil
from pathlib import Path
from unittest.mock import Mock, patch
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
            try:
                asyncio.get_event_loop().run_until_complete(self.orchestrator.stop())
            except Exception:
                pass
        shutil.rmtree(self.temp_dir)
    
    def test_orchestrator_initialization(self):
        """Test that orchestrator initializes properly"""
        orchestrator = BucikaOrchestrator(headless=True, data_directory=Path(self.temp_dir))
        
        # Check initialization
        self.assertIsNotNone(orchestrator)
        self.assertTrue(orchestrator.headless)
        self.assertEqual(orchestrator.data_directory, Path(self.temp_dir))
        
        # Check services are initialized
        self.assertIsNotNone(orchestrator.session_manager)
        self.assertIsNotNone(orchestrator.websocket_server)
        self.assertIsNotNone(orchestrator.discovery_service)
        self.assertIsNotNone(orchestrator.time_sync_service)
        self.assertIsNotNone(orchestrator.performance_monitor)
        self.assertIsNotNone(orchestrator.error_recovery)
        self.assertIsNotNone(orchestrator.data_analyzer)
        self.assertIsNotNone(orchestrator.data_validator)
    
    def test_service_startup_sequence(self):
        """Test that services start up in correct sequence"""
        async def run_test():
            self.orchestrator = BucikaOrchestrator(headless=True, data_directory=Path(self.temp_dir))
            
            # Mock the actual network services to avoid port binding in tests
            with patch.object(self.orchestrator.websocket_server, 'start') as mock_ws, \
                 patch.object(self.orchestrator.discovery_service, 'start') as mock_mdns, \
                 patch.object(self.orchestrator.time_sync_service, 'start') as mock_time, \
                 patch.object(self.orchestrator.error_recovery, 'start') as mock_error:
                
                # Start orchestrator
                await self.orchestrator.start()
                
                # Verify all services were started
                mock_error.assert_called_once()
                mock_time.assert_called_once()
                mock_mdns.assert_called_once()
                mock_ws.assert_called_once()
                
                self.assertTrue(self.orchestrator.running)
        
        asyncio.run(run_test())
    
    def test_service_status_reporting(self):
        """Test comprehensive status reporting"""
        self.orchestrator = BucikaOrchestrator(headless=True, data_directory=Path(self.temp_dir))
        
        # Get status before starting
        status = self.orchestrator.get_status()
        
        # Verify status structure
        self.assertIn('running', status)
        self.assertIn('headless', status)
        self.assertIn('data_directory', status)
        self.assertIn('services', status)
        self.assertIn('session', status)
        
        # Check services status structure
        services = status['services']
        self.assertIn('websocket_server', services)
        self.assertIn('discovery_service', services)
        self.assertIn('time_sync_service', services)
        self.assertIn('performance_monitor', services)
        self.assertIn('error_recovery', services)
        
        # Initially should not be running
        self.assertFalse(status['running'])
    
    def test_data_analysis_integration(self):
        """Test data analysis system integration"""
        async def run_test():
            self.orchestrator = BucikaOrchestrator(headless=True, data_directory=Path(self.temp_dir))
            
            # Create sample session data
            session_id = "test_session_001"
            session_path = Path(self.temp_dir) / session_id
            session_path.mkdir()
            
            # Create sample GSR data
            gsr_file = session_path / "gsr_data_20241225_120000.csv"
            with open(gsr_file, 'w') as f:
                f.write("timestamp,datetime,gsr_microsiemens\n")
                f.write("1735123200,2024-12-25T12:00:00,2.5\n")
                f.write("1735123201,2024-12-25T12:00:01,2.7\n")
                f.write("1735123202,2024-12-25T12:00:02,2.3\n")
            
            # Test analysis
            result = await self.orchestrator.analyze_session(session_id)
            
            # Verify analysis result
            self.assertIsNotNone(result)
            self.assertEqual(result.session_id, session_id)
            self.assertGreater(result.sample_count, 0)
        
        asyncio.run(run_test())
    
    def test_validation_system_integration(self):
        """Test validation system integration"""
        async def run_test():
            self.orchestrator = BucikaOrchestrator(
                headless=True, 
                data_directory=Path(self.temp_dir),
                validation_level="standard"
            )
            
            # Create sample session data
            session_id = "validation_test_001"
            session_path = Path(self.temp_dir) / session_id
            session_path.mkdir()
            
            # Create sample GSR data with good quality
            gsr_file = session_path / "gsr_data_20241225_120000.csv"
            with open(gsr_file, 'w') as f:
                f.write("timestamp,datetime,gsr_microsiemens\n")
                for i in range(100):  # 100 samples
                    timestamp = 1735123200 + i
                    datetime_str = f"2024-12-25T12:00:{i:02d}"
                    gsr_value = 2.5 + (i % 10) * 0.1  # Realistic variation
                    f.write(f"{timestamp},{datetime_str},{gsr_value:.3f}\n")
            
            # Test validation
            result = await self.orchestrator.validate_session(session_id)
            
            # Verify validation result
            self.assertIsNotNone(result)
            self.assertEqual(result.session_id, session_id)
            self.assertGreaterEqual(result.overall_score, 0.0)
            self.assertLessEqual(result.overall_score, 1.0)
            self.assertIsInstance(result.results, list)
            self.assertIsInstance(result.recommendations, list)
        
        asyncio.run(run_test())
    
    def test_error_recovery_integration(self):
        """Test error recovery system integration"""
        async def run_test():
            self.orchestrator = BucikaOrchestrator(headless=True, data_directory=Path(self.temp_dir))
            
            # Start error recovery
            await self.orchestrator.error_recovery.start()
            
            # Simulate an error
            test_error = ValueError("Test validation error")
            success = await self.orchestrator.error_recovery.handle_error("test_service", test_error)
            
            # Should handle the error (may or may not recover, but shouldn't crash)
            self.assertIsInstance(success, bool)
            
            # Check error report
            error_report = self.orchestrator.get_error_report()
            self.assertIn('overall_stats', error_report)
            self.assertGreaterEqual(error_report['overall_stats']['total_errors'], 1)
        
        asyncio.run(run_test())
    
    def test_performance_monitoring_integration(self):
        """Test performance monitoring integration"""
        async def run_test():
            orchestrator = BucikaOrchestrator(headless=True, data_directory=Path(self.temp_dir))
            
            # Start performance monitoring
            await orchestrator.performance_monitor.start()
            
            # Get performance report
            perf_report = orchestrator.get_performance_report()
            
            # Verify report structure
            self.assertIsInstance(perf_report, dict)
            # Should have some performance data even if minimal
            
            # Stop monitoring
            await orchestrator.performance_monitor.stop()
        
        asyncio.run(run_test())
    
    def test_complete_shutdown_sequence(self):
        """Test complete shutdown sequence"""
        async def run_test():
            self.orchestrator = BucikaOrchestrator(headless=True, data_directory=Path(self.temp_dir))
            
            # Mock network services to avoid actual binding
            with patch.object(self.orchestrator.websocket_server, 'start'), \
                 patch.object(self.orchestrator.websocket_server, 'stop') as mock_ws_stop, \
                 patch.object(self.orchestrator.discovery_service, 'start'), \
                 patch.object(self.orchestrator.discovery_service, 'stop') as mock_mdns_stop, \
                 patch.object(self.orchestrator.time_sync_service, 'start'), \
                 patch.object(self.orchestrator.time_sync_service, 'stop') as mock_time_stop, \
                 patch.object(self.orchestrator.error_recovery, 'start'), \
                 patch.object(self.orchestrator.error_recovery, 'stop') as mock_error_stop:
                
                # Start then stop orchestrator
                await self.orchestrator.start()
                self.assertTrue(self.orchestrator.running)
                
                await self.orchestrator.stop()
                self.assertFalse(self.orchestrator.running)
                
                # Verify all services were stopped
                mock_ws_stop.assert_called_once()
                mock_mdns_stop.assert_called_once()
                mock_time_stop.assert_called_once()
                mock_error_stop.assert_called_once()
        
        asyncio.run(run_test())


if __name__ == "__main__":
    unittest.main()