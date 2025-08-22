"""
Socket.IO Load and Stress Testing
=================================

Comprehensive load testing for Socket.IO connections and real-time
communication in the Multi-Sensor Recording System. Tests concurrent
connections, message throughput, and system stability under stress.

Requirements Coverage:
- NFR1: Performance with multiple concurrent connections
- NFR3: Fault tolerance with connection failures and recovery
- FR6: Real-time status updates under load
- FR8: System stability with device disconnections
"""

import pytest
import asyncio
import time
import threading
import statistics
import json
import os
import sys
from typing import Dict, List, Optional, Tuple
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timedelta
from unittest.mock import Mock, patch

# Socket.IO client imports
try:
    import socketio
    import aiohttp
    SOCKETIO_AVAILABLE = True
except ImportError:
    SOCKETIO_AVAILABLE = False
    socketio = None
    aiohttp = None

# Performance monitoring
try:
    import psutil
    PSUTIL_AVAILABLE = True
except ImportError:
    PSUTIL_AVAILABLE = False
    psutil = None

# Add PythonApp to path
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'PythonApp'))

try:
    from PythonApp.web_ui.web_dashboard import WebDashboardServer
    WEB_AVAILABLE = True
except ImportError:
    WEB_AVAILABLE = False
    WebDashboardServer = None


class LoadTestMetrics:
    """Collects and analyzes load test metrics."""
    
    def __init__(self):
        self.connection_times = []
        self.message_response_times = []
        self.connection_failures = 0
        self.message_failures = 0
        self.peak_memory_mb = 0
        self.peak_cpu_percent = 0.0
        self.start_time = None
        self.end_time = None
        self.concurrent_connections = 0
        self.messages_sent = 0
        self.messages_received = 0
    
    def start_monitoring(self):
        """Start performance monitoring."""
        self.start_time = datetime.now()
        if PSUTIL_AVAILABLE:
            self._monitor_system_resources()
    
    def stop_monitoring(self):
        """Stop performance monitoring."""
        self.end_time = datetime.now()
    
    def _monitor_system_resources(self):
        """Monitor system resource usage."""
        def monitor():
            while self.end_time is None:
                try:
                    memory_mb = psutil.virtual_memory().used / 1024 / 1024
                    cpu_percent = psutil.cpu_percent(interval=1)
                    
                    self.peak_memory_mb = max(self.peak_memory_mb, memory_mb)
                    self.peak_cpu_percent = max(self.peak_cpu_percent, cpu_percent)
                    
                    time.sleep(1)
                except:
                    break
        
        threading.Thread(target=monitor, daemon=True).start()
    
    def record_connection(self, success: bool, duration_ms: float):
        """Record connection attempt result."""
        if success:
            self.connection_times.append(duration_ms)
            self.concurrent_connections += 1
        else:
            self.connection_failures += 1
    
    def record_message(self, success: bool, response_time_ms: float):
        """Record message exchange result."""
        if success:
            self.message_response_times.append(response_time_ms)
            self.messages_sent += 1
            self.messages_received += 1
        else:
            self.message_failures += 1
            self.messages_sent += 1
    
    def get_summary(self) -> Dict:
        """Get performance summary."""
        duration_seconds = (self.end_time - self.start_time).total_seconds() if self.end_time else 0
        
        return {
            "test_duration_seconds": duration_seconds,
            "concurrent_connections": self.concurrent_connections,
            "connection_success_rate": self._success_rate(self.connection_times, self.connection_failures),
            "message_success_rate": self._success_rate(self.message_response_times, self.message_failures),
            "avg_connection_time_ms": statistics.mean(self.connection_times) if self.connection_times else 0,
            "p95_connection_time_ms": self._percentile(self.connection_times, 95),
            "avg_message_response_time_ms": statistics.mean(self.message_response_times) if self.message_response_times else 0,
            "p95_message_response_time_ms": self._percentile(self.message_response_times, 95),
            "messages_per_second": self.messages_sent / duration_seconds if duration_seconds > 0 else 0,
            "peak_memory_mb": self.peak_memory_mb,
            "peak_cpu_percent": self.peak_cpu_percent,
            "total_failures": self.connection_failures + self.message_failures
        }
    
    def _success_rate(self, successes: List, failures: int) -> float:
        """Calculate success rate percentage."""
        total = len(successes) + failures
        return (len(successes) / total * 100) if total > 0 else 0
    
    def _percentile(self, data: List[float], percentile: float) -> float:
        """Calculate percentile value."""
        if not data:
            return 0
        return statistics.quantiles(data, n=100)[int(percentile) - 1] if len(data) >= 100 else max(data)


class SocketIOLoadGenerator:
    """Generates load for Socket.IO connections."""
    
    def __init__(self, server_url: str = "http://localhost:5000"):
        self.server_url = server_url
        self.clients = []
        self.metrics = LoadTestMetrics()
        
        if not SOCKETIO_AVAILABLE:
            raise ImportError("python-socketio required for load testing. Install with: pip install python-socketio")
    
    async def create_client_connection(self, client_id: str) -> Tuple[bool, float]:
        """Create a single Socket.IO client connection."""
        start_time = time.time()
        
        try:
            sio = socketio.AsyncClient()
            
            # Set up event handlers
            @sio.event
            async def connect():
                print(f"Client {client_id} connected")
            
            @sio.event
            async def disconnect():
                print(f"Client {client_id} disconnected")
            
            @sio.event
            async def device_status(data):
                # Simulate receiving device status updates
                response_time = (time.time() - start_time) * 1000
                self.metrics.record_message(True, response_time)
            
            # Connect to server
            await sio.connect(self.server_url)
            
            connection_time = (time.time() - start_time) * 1000
            self.clients.append(sio)
            
            return True, connection_time
            
        except Exception as e:
            connection_time = (time.time() - start_time) * 1000
            print(f"Client {client_id} connection failed: {e}")
            return False, connection_time
    
    async def simulate_device_messages(self, client, device_id: str, duration_seconds: int):
        """Simulate device sending messages through Socket.IO."""
        end_time = time.time() + duration_seconds
        message_count = 0
        
        while time.time() < end_time:
            try:
                start_time = time.time()
                
                # Simulate device data message
                message = {
                    "device_id": device_id,
                    "timestamp": datetime.now().isoformat(),
                    "data": {
                        "gsr_value": 0.5 + (message_count % 100) / 200,  # Varying GSR data
                        "temperature": 98.6 + (message_count % 20) / 10,  # Varying temperature
                        "sequence": message_count
                    }
                }
                
                await client.emit('device_data', message)
                
                response_time = (time.time() - start_time) * 1000
                self.metrics.record_message(True, response_time)
                
                message_count += 1
                await asyncio.sleep(0.1)  # 10 Hz data rate
                
            except Exception as e:
                self.metrics.record_message(False, 0)
                print(f"Message failed for device {device_id}: {e}")
    
    async def stress_test_connections(self, num_clients: int, ramp_up_seconds: int = 10) -> LoadTestMetrics:
        """Stress test with multiple concurrent connections."""
        self.metrics.start_monitoring()
        
        # Ramp up connections gradually
        connection_tasks = []
        for i in range(num_clients):
            if i > 0:
                await asyncio.sleep(ramp_up_seconds / num_clients)
            
            task = asyncio.create_task(self.create_client_connection(f"stress_client_{i}"))
            connection_tasks.append(task)
        
        # Wait for all connections
        connection_results = await asyncio.gather(*connection_tasks, return_exceptions=True)
        
        # Record connection results
        for result in connection_results:
            if isinstance(result, Exception):
                self.metrics.record_connection(False, 0)
            else:
                success, duration = result
                self.metrics.record_connection(success, duration)
        
        # Simulate message traffic
        message_tasks = []
        for i, client in enumerate(self.clients):
            task = asyncio.create_task(
                self.simulate_device_messages(client, f"stress_device_{i}", 30)
            )
            message_tasks.append(task)
        
        # Wait for message simulation
        await asyncio.gather(*message_tasks, return_exceptions=True)
        
        # Clean up connections
        for client in self.clients:
            try:
                await client.disconnect()
            except:
                pass
        
        self.metrics.stop_monitoring()
        return self.metrics
    
    async def endurance_test(self, num_clients: int, duration_hours: int) -> LoadTestMetrics:
        """Long-running endurance test."""
        self.metrics.start_monitoring()
        
        # Create stable connections
        connection_tasks = [
            self.create_client_connection(f"endurance_client_{i}")
            for i in range(num_clients)
        ]
        
        connection_results = await asyncio.gather(*connection_tasks, return_exceptions=True)
        
        # Record connection results
        for result in connection_results:
            if isinstance(result, Exception):
                self.metrics.record_connection(False, 0)
            else:
                success, duration = result
                self.metrics.record_connection(success, duration)
        
        # Run endurance test
        duration_seconds = duration_hours * 3600
        message_tasks = []
        
        for i, client in enumerate(self.clients):
            task = asyncio.create_task(
                self.simulate_device_messages(client, f"endurance_device_{i}", duration_seconds)
            )
            message_tasks.append(task)
        
        await asyncio.gather(*message_tasks, return_exceptions=True)
        
        # Clean up
        for client in self.clients:
            try:
                await client.disconnect()
            except:
                pass
        
        self.metrics.stop_monitoring()
        return self.metrics


@pytest.fixture(scope="session")
def web_dashboard_server():
    """Start Web dashboard server for load testing."""
    if not WEB_AVAILABLE:
        pytest.skip("Web dashboard not available")
    
    server = WebDashboardServer(port=5000, debug=False)
    server.start()
    time.sleep(3)  # Allow server to start and stabilize
    
    yield server
    
    server.stop()


@pytest.fixture
def load_generator():
    """Socket.IO load generator instance."""
    return SocketIOLoadGenerator()


class TestSocketIOLoadPerformance:
    """Load testing for Socket.IO performance."""
    
    @pytest.mark.load
    @pytest.mark.network
    @pytest.mark.slow
    def test_light_load_performance(self, web_dashboard_server, load_generator):
        """Test performance under light load (NFR1)."""
        async def run_test():
            metrics = await load_generator.stress_test_connections(
                num_clients=5,
                ramp_up_seconds=5
            )
            return metrics
        
        metrics = asyncio.run(run_test())
        summary = metrics.get_summary()
        
        # Performance assertions for light load
        assert summary["connection_success_rate"] >= 95.0, \
            f"Connection success rate too low: {summary['connection_success_rate']:.1f}%"
        
        assert summary["message_success_rate"] >= 98.0, \
            f"Message success rate too low: {summary['message_success_rate']:.1f}%"
        
        assert summary["avg_connection_time_ms"] <= 1000, \
            f"Average connection time too high: {summary['avg_connection_time_ms']:.1f}ms"
        
        assert summary["avg_message_response_time_ms"] <= 100, \
            f"Average message response time too high: {summary['avg_message_response_time_ms']:.1f}ms"
    
    @pytest.mark.load
    @pytest.mark.network
    @pytest.mark.slow
    def test_moderate_load_performance(self, web_dashboard_server, load_generator):
        """Test performance under moderate load (NFR1)."""
        async def run_test():
            metrics = await load_generator.stress_test_connections(
                num_clients=20,
                ramp_up_seconds=10
            )
            return metrics
        
        metrics = asyncio.run(run_test())
        summary = metrics.get_summary()
        
        # Performance assertions for moderate load
        assert summary["connection_success_rate"] >= 90.0, \
            f"Connection success rate too low under moderate load: {summary['connection_success_rate']:.1f}%"
        
        assert summary["message_success_rate"] >= 95.0, \
            f"Message success rate too low under moderate load: {summary['message_success_rate']:.1f}%"
        
        assert summary["avg_connection_time_ms"] <= 2000, \
            f"Average connection time too high under moderate load: {summary['avg_connection_time_ms']:.1f}ms"
        
        assert summary["p95_message_response_time_ms"] <= 500, \
            f"95th percentile message response time too high: {summary['p95_message_response_time_ms']:.1f}ms"
    
    @pytest.mark.load
    @pytest.mark.network
    @pytest.mark.slow
    def test_heavy_load_performance(self, web_dashboard_server, load_generator):
        """Test performance under heavy load (NFR1)."""
        async def run_test():
            metrics = await load_generator.stress_test_connections(
                num_clients=50,
                ramp_up_seconds=15
            )
            return metrics
        
        metrics = asyncio.run(run_test())
        summary = metrics.get_summary()
        
        # Performance assertions for heavy load
        assert summary["connection_success_rate"] >= 80.0, \
            f"Connection success rate too low under heavy load: {summary['connection_success_rate']:.1f}%"
        
        assert summary["message_success_rate"] >= 85.0, \
            f"Message success rate too low under heavy load: {summary['message_success_rate']:.1f}%"
        
        # Under heavy load, we allow higher response times but system should remain stable
        assert summary["peak_memory_mb"] <= 1000, \
            f"Peak memory usage too high: {summary['peak_memory_mb']:.1f}MB"
        
        assert summary["peak_cpu_percent"] <= 90, \
            f"Peak CPU usage too high: {summary['peak_cpu_percent']:.1f}%"
    
    @pytest.mark.load
    @pytest.mark.network
    @pytest.mark.slow
    @pytest.mark.integration
    def test_sustained_load_performance(self, web_dashboard_server, load_generator):
        """Test performance under sustained load (NFR1)."""
        async def run_test():
            metrics = await load_generator.endurance_test(
                num_clients=10,
                duration_hours=0.1  # 6 minutes for CI
            )
            return metrics
        
        metrics = asyncio.run(run_test())
        summary = metrics.get_summary()
        
        # Performance assertions for sustained load
        assert summary["connection_success_rate"] >= 95.0, \
            f"Connection success rate degraded over time: {summary['connection_success_rate']:.1f}%"
        
        assert summary["message_success_rate"] >= 90.0, \
            f"Message success rate degraded over time: {summary['message_success_rate']:.1f}%"
        
        # Check for memory leaks
        assert summary["peak_memory_mb"] <= 800, \
            f"Potential memory leak detected: {summary['peak_memory_mb']:.1f}MB"


class TestSocketIOFaultTolerance:
    """Fault tolerance testing for Socket.IO connections."""
    
    @pytest.mark.load
    @pytest.mark.network
    def test_connection_recovery(self, web_dashboard_server, load_generator):
        """Test connection recovery after network issues (FR8)."""
        async def run_test():
            # Establish connections
            metrics = await load_generator.stress_test_connections(
                num_clients=10,
                ramp_up_seconds=5
            )
            
            # Simulate network interruption by disconnecting some clients
            for i, client in enumerate(load_generator.clients[:5]):
                try:
                    await client.disconnect()
                except:
                    pass
            
            # Wait and attempt reconnection
            await asyncio.sleep(2)
            
            # Reconnect clients
            reconnection_tasks = []
            for i in range(5):
                task = asyncio.create_task(
                    load_generator.create_client_connection(f"recovery_client_{i}")
                )
                reconnection_tasks.append(task)
            
            reconnection_results = await asyncio.gather(*reconnection_tasks, return_exceptions=True)
            
            # Record reconnection success
            recovery_success = 0
            for result in reconnection_results:
                if not isinstance(result, Exception):
                    success, _ = result
                    if success:
                        recovery_success += 1
            
            return recovery_success >= 4  # At least 4 out of 5 should recover
        
        recovery_successful = asyncio.run(run_test())
        assert recovery_successful, "Connection recovery failed - fault tolerance insufficient"
    
    @pytest.mark.load
    @pytest.mark.network
    def test_gradual_failure_handling(self, web_dashboard_server, load_generator):
        """Test handling of gradual connection failures (FR8)."""
        async def run_test():
            # Start with many connections
            initial_clients = 20
            metrics = await load_generator.stress_test_connections(
                num_clients=initial_clients,
                ramp_up_seconds=10
            )
            
            initial_success_rate = metrics.get_summary()["connection_success_rate"]
            
            # Gradually disconnect clients to simulate device failures
            disconnect_tasks = []
            for i, client in enumerate(load_generator.clients):
                if i % 3 == 0:  # Disconnect every 3rd client
                    disconnect_tasks.append(asyncio.create_task(client.disconnect()))
                await asyncio.sleep(0.5)  # Gradual disconnection
            
            await asyncio.gather(*disconnect_tasks, return_exceptions=True)
            
            # Check if server remains stable with remaining connections
            remaining_clients = [c for i, c in enumerate(load_generator.clients) if i % 3 != 0]
            
            # Test message throughput with remaining clients
            message_tasks = []
            for i, client in enumerate(remaining_clients[:5]):  # Test subset
                task = asyncio.create_task(
                    load_generator.simulate_device_messages(client, f"remaining_device_{i}", 10)
                )
                message_tasks.append(task)
            
            await asyncio.gather(*message_tasks, return_exceptions=True)
            
            final_metrics = load_generator.metrics.get_summary()
            return final_metrics["message_success_rate"] >= 85.0
        
        stability_maintained = asyncio.run(run_test())
        assert stability_maintained, "System stability compromised during gradual failures"


class TestConcurrentDeviceSimulation:
    """Test concurrent device simulation scenarios."""
    
    @pytest.mark.load
    @pytest.mark.integration
    def test_multi_device_coordination_load(self, web_dashboard_server, load_generator):
        """Test coordination with multiple simulated devices (FR2, NFR1)."""
        async def run_test():
            # Simulate different device types
            device_types = ["shimmer", "thermal", "camera"]
            clients_per_type = 5
            
            coordination_tasks = []
            
            for device_type in device_types:
                for i in range(clients_per_type):
                    device_id = f"{device_type}_device_{i}"
                    task = asyncio.create_task(
                        load_generator.create_client_connection(device_id)
                    )
                    coordination_tasks.append(task)
            
            # Wait for all devices to connect
            results = await asyncio.gather(*coordination_tasks, return_exceptions=True)
            
            successful_connections = sum(
                1 for result in results 
                if not isinstance(result, Exception) and result[0]
            )
            
            # Simulate synchronized recording session
            if successful_connections >= 10:
                # Start synchronized data streaming
                sync_tasks = []
                for i, client in enumerate(load_generator.clients[:successful_connections]):
                    task = asyncio.create_task(
                        load_generator.simulate_device_messages(
                            client, f"sync_device_{i}", 15
                        )
                    )
                    sync_tasks.append(task)
                
                await asyncio.gather(*sync_tasks, return_exceptions=True)
                
                final_metrics = load_generator.metrics.get_summary()
                return final_metrics["message_success_rate"] >= 90.0
            
            return False
        
        coordination_successful = asyncio.run(run_test())
        assert coordination_successful, "Multi-device coordination failed under load"


if __name__ == "__main__":
    # Run load tests
    pytest.main([__file__, "-v", "-m", "load", "--tb=short"])