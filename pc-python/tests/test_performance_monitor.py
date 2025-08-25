#!/usr/bin/env python3
"""
Test suite for performance monitoring functionality
"""

import unittest
import asyncio
import tempfile
import shutil
import time
from pathlib import Path
from unittest.mock import Mock, patch
from src.bucika_gsr_pc.performance_monitor import PerformanceMonitor


class TestPerformanceMonitor(unittest.TestCase):
    """Test performance monitoring functionality"""
    
    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()
        self.monitor = PerformanceMonitor(
            monitoring_interval=0.1,  # Fast monitoring for tests
            history_size=100
        )
    
    def tearDown(self):
        """Clean up test environment"""
        if self.monitor.running:
            try:
                asyncio.get_event_loop().run_until_complete(self.monitor.stop())
            except Exception:
                pass
        shutil.rmtree(self.temp_dir)
    
    def test_monitor_initialization(self):
        """Test performance monitor initialization"""
        self.assertEqual(self.monitor.monitoring_interval, 0.1)
        self.assertEqual(self.monitor.history_size, 100)
        self.assertFalse(self.monitor.running)
        self.assertEqual(len(self.monitor.metrics_history), 0)
    
    def test_system_metrics_collection(self):
        """Test system metrics collection"""
        metrics = self.monitor._collect_system_metrics()
        
        # Check basic metrics are present
        self.assertIn('cpu_percent', metrics)
        self.assertIn('memory_percent', metrics)
        self.assertIn('memory_available_mb', metrics)
        self.assertIn('disk_usage_percent', metrics)
        self.assertIn('network_bytes_sent', metrics)
        self.assertIn('network_bytes_recv', metrics)
        
        # Check metrics are reasonable
        self.assertGreaterEqual(metrics['cpu_percent'], 0.0)
        self.assertLessEqual(metrics['cpu_percent'], 100.0)
        self.assertGreaterEqual(metrics['memory_percent'], 0.0)
        self.assertLessEqual(metrics['memory_percent'], 100.0)
        self.assertGreaterEqual(metrics['memory_available_mb'], 0)
    
    def test_application_metrics_collection(self):
        """Test application-specific metrics collection"""
        # Add some mock application metrics
        self.monitor.add_metric('active_sessions', 5)
        self.monitor.add_metric('messages_processed', 1000)
        self.monitor.add_metric('data_points_stored', 50000)
        
        metrics = self.monitor._collect_application_metrics()
        
        self.assertEqual(metrics['active_sessions'], 5)
        self.assertEqual(metrics['messages_processed'], 1000)
        self.assertEqual(metrics['data_points_stored'], 50000)
    
    def test_monitoring_lifecycle(self):
        """Test monitoring start/stop lifecycle"""
        async def run_test():
            self.assertFalse(self.monitor.running)
            
            # Start monitoring
            await self.monitor.start()
            self.assertTrue(self.monitor.running)
            
            # Let it collect some metrics
            await asyncio.sleep(0.3)  # Allow 3 collection cycles
            
            # Stop monitoring
            await self.monitor.stop()
            self.assertFalse(self.monitor.running)
            
            # Should have collected some metrics
            self.assertGreater(len(self.monitor.metrics_history), 0)
        
        asyncio.run(run_test())
    
    def test_metrics_history_management(self):
        """Test metrics history size management"""
        async def run_test():
            # Set small history size for testing
            self.monitor.history_size = 5
            
            await self.monitor.start()
            
            # Wait for more cycles than history size
            await asyncio.sleep(0.8)  # 8 cycles, but history limited to 5
            
            await self.monitor.stop()
            
            # History should be limited to configured size
            self.assertLessEqual(len(self.monitor.metrics_history), 5)
        
        asyncio.run(run_test())
    
    def test_performance_report_generation(self):
        """Test performance report generation"""
        async def run_test():
            await self.monitor.start()
            
            # Add some custom metrics
            self.monitor.add_metric('test_counter', 100)
            self.monitor.add_metric('test_gauge', 75.5)
            
            # Let it collect metrics
            await asyncio.sleep(0.3)
            
            # Generate report
            report = self.monitor.get_performance_report()
            
            await self.monitor.stop()
            
            # Check report structure
            self.assertIn('timestamp', report)
            self.assertIn('system_metrics', report)
            self.assertIn('application_metrics', report)
            self.assertIn('statistics', report)
            
            # Check system metrics
            sys_metrics = report['system_metrics']
            self.assertIn('cpu_percent', sys_metrics)
            self.assertIn('memory_percent', sys_metrics)
            
            # Check application metrics
            app_metrics = report['application_metrics']
            self.assertIn('test_counter', app_metrics)
            self.assertIn('test_gauge', app_metrics)
            
            # Check statistics
            stats = report['statistics']
            self.assertIn('uptime_seconds', stats)
            self.assertIn('metrics_collected', stats)
        
        asyncio.run(run_test())
    
    def test_threshold_monitoring(self):
        """Test threshold-based monitoring and alerts"""
        async def run_test():
            # Set up threshold alerts
            alerts_triggered = []
            
            def alert_handler(metric_name, current_value, threshold):
                alerts_triggered.append({
                    'metric': metric_name,
                    'value': current_value,
                    'threshold': threshold
                })
            
            self.monitor.set_alert_handler(alert_handler)
            
            # Set threshold for CPU usage
            self.monitor.set_threshold('cpu_percent', max_value=50.0)
            
            # Simulate high CPU usage
            with patch('psutil.cpu_percent', return_value=75.0):
                await self.monitor.start()
                await asyncio.sleep(0.3)
                await self.monitor.stop()
            
            # Should have triggered alert
            self.assertGreater(len(alerts_triggered), 0)
            cpu_alert = next((a for a in alerts_triggered if a['metric'] == 'cpu_percent'), None)
            self.assertIsNotNone(cpu_alert)
            self.assertEqual(cpu_alert['value'], 75.0)
        
        asyncio.run(run_test())
    
    def test_custom_metric_tracking(self):
        """Test custom metric tracking"""
        # Add various types of metrics
        self.monitor.add_metric('counter_metric', 0)
        self.monitor.increment_metric('counter_metric')
        self.monitor.increment_metric('counter_metric', 5)
        
        self.monitor.add_metric('gauge_metric', 100)
        self.monitor.set_metric('gauge_metric', 150)
        
        # Check metric values
        self.assertEqual(self.monitor.get_metric('counter_metric'), 6)
        self.assertEqual(self.monitor.get_metric('gauge_metric'), 150)
        
        # Test non-existent metric
        self.assertIsNone(self.monitor.get_metric('nonexistent_metric'))
    
    def test_resource_usage_tracking(self):
        """Test resource usage tracking over time"""
        async def run_test():
            await self.monitor.start()
            
            # Let it collect baseline metrics
            await asyncio.sleep(0.2)
            
            # Simulate some work
            data = [i**2 for i in range(10000)]  # CPU work
            self.monitor.add_metric('work_completed', len(data))
            
            # Collect more metrics
            await asyncio.sleep(0.2)
            
            await self.monitor.stop()
            
            # Check that we have metrics over time
            self.assertGreater(len(self.monitor.metrics_history), 2)
            
            # Check for resource usage trends
            report = self.monitor.get_performance_report()
            stats = report['statistics']
            
            self.assertIn('avg_cpu_percent', stats)
            self.assertIn('max_memory_percent', stats)
        
        asyncio.run(run_test())
    
    def test_memory_leak_detection(self):
        """Test memory leak detection"""
        async def run_test():
            await self.monitor.start()
            
            # Simulate memory growth
            memory_hog = []
            for i in range(5):
                memory_hog.extend([j for j in range(1000)])
                await asyncio.sleep(0.1)
            
            await self.monitor.stop()
            
            # Check for memory trend detection
            report = self.monitor.get_performance_report()
            
            # Should have collected enough samples to detect trend
            self.assertGreater(len(self.monitor.metrics_history), 3)
            
            # Memory usage should be tracked
            stats = report['statistics']
            self.assertIn('avg_memory_percent', stats)
        
        asyncio.run(run_test())
    
    def test_performance_optimization_recommendations(self):
        """Test performance optimization recommendations"""
        async def run_test():
            # Simulate performance issues
            with patch('psutil.cpu_percent', return_value=85.0), \
                 patch('psutil.virtual_memory') as mock_memory:
                
                mock_memory.return_value.percent = 90.0
                mock_memory.return_value.available = 1024 * 1024 * 100  # 100MB
                
                await self.monitor.start()
                await asyncio.sleep(0.3)
                await self.monitor.stop()
            
            # Get recommendations
            recommendations = self.monitor.get_optimization_recommendations()
            
            self.assertIsInstance(recommendations, list)
            # Should have recommendations for high resource usage
            self.assertGreater(len(recommendations), 0)
            
            # Check recommendation structure
            for rec in recommendations:
                self.assertIn('category', rec)
                self.assertIn('priority', rec)
                self.assertIn('description', rec)
                self.assertIn('recommendation', rec)
        
        asyncio.run(run_test())
    
    def test_historical_data_analysis(self):
        """Test historical data analysis"""
        async def run_test():
            await self.monitor.start()
            
            # Collect data over time
            await asyncio.sleep(0.5)
            
            await self.monitor.stop()
            
            # Analyze trends
            trends = self.monitor.analyze_trends()
            
            self.assertIsInstance(trends, dict)
            
            # Should have trend analysis for key metrics
            expected_metrics = ['cpu_percent', 'memory_percent']
            for metric in expected_metrics:
                if metric in trends:
                    trend_data = trends[metric]
                    self.assertIn('trend', trend_data)  # 'increasing', 'decreasing', 'stable'
                    self.assertIn('confidence', trend_data)
        
        asyncio.run(run_test())
    
    def test_export_functionality(self):
        """Test performance data export"""
        async def run_test():
            await self.monitor.start()
            
            # Add some metrics
            self.monitor.add_metric('export_test', 42)
            
            await asyncio.sleep(0.3)
            await self.monitor.stop()
            
            # Export data
            export_file = Path(self.temp_dir) / "performance_data.json"
            success = await self.monitor.export_data(export_file)
            
            self.assertTrue(success)
            self.assertTrue(export_file.exists())
            
            # Verify file contents
            import json
            with open(export_file, 'r') as f:
                data = json.load(f)
            
            self.assertIn('metrics_history', data)
            self.assertIn('export_timestamp', data)
            self.assertGreater(len(data['metrics_history']), 0)
        
        asyncio.run(run_test())
    
    def test_real_time_monitoring_api(self):
        """Test real-time monitoring API"""
        async def run_test():
            await self.monitor.start()
            
            # Get real-time metrics
            current_metrics = self.monitor.get_current_metrics()
            
            self.assertIsInstance(current_metrics, dict)
            self.assertIn('cpu_percent', current_metrics)
            self.assertIn('memory_percent', current_metrics)
            
            # Test metric subscription (if implemented)
            callback_called = []
            
            def metrics_callback(metrics):
                callback_called.append(metrics)
            
            # This would be used for real-time dashboards
            self.monitor.add_metrics_callback(metrics_callback)
            
            await asyncio.sleep(0.3)
            await self.monitor.stop()
            
            # Callback should have been called
            self.assertGreater(len(callback_called), 0)
        
        asyncio.run(run_test())


if __name__ == "__main__":
    unittest.main()