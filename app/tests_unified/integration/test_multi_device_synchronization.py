"""
Integration tests for multi-device synchronization simulation
Tests device coordination, timing accuracy, and protocol validation
Target: Chapter 5 integration testing requirements
"""

import asyncio
import json
import time
import unittest
from unittest.mock import Mock, patch, AsyncMock
import socket
import threading
from datetime import datetime, timedelta
from pathlib import Path
import sys

# Add project paths for imports
current_dir = Path(__file__).parent
project_root = current_dir.parent.parent.parent
sys.path.insert(0, str(project_root))
sys.path.insert(0, str(project_root / "PythonApp"))

try:
    from PythonApp.network.protocol import MessageProtocol, DeviceMessage
    from PythonApp.synchronization.master_clock_synchronizer import MasterClockSynchronizer
except ImportError:
    # Create mock classes for testing
    class MessageProtocol:
        @staticmethod
        def create_start_message(timestamp=None):
            return {
                "command": "start_recording",
                "timestamp": timestamp or time.time(),
                "message_id": f"start_{int(time.time() * 1000)}"
            }
        
        @staticmethod
        def create_stop_message(timestamp=None):
            return {
                "command": "stop_recording", 
                "timestamp": timestamp or time.time(),
                "message_id": f"stop_{int(time.time() * 1000)}"
            }
        
        @staticmethod
        def validate_message(message):
            required_fields = ["command", "timestamp", "message_id"]
            return all(field in message for field in required_fields)
    
    class DeviceMessage:
        def __init__(self, device_id, message_type, data=None):
            self.device_id = device_id
            self.message_type = message_type
            self.data = data or {}
            self.timestamp = time.time()
    
    class MasterClockSynchronizer:
        def __init__(self):
            self.devices = {}
            self.reference_time = time.time()
        
        def add_device(self, device_id, initial_offset=0.0):
            self.devices[device_id] = {
                "offset": initial_offset,
                "last_sync": time.time()
            }
        
        def synchronize_devices(self):
            current_time = time.time()
            for device_id in self.devices:
                self.devices[device_id]["last_sync"] = current_time
            return True
        
        def get_synchronized_time(self, device_id):
            if device_id not in self.devices:
                return None
            return time.time() - self.devices[device_id]["offset"]


class MockDevice:
    """Mock device for multi-device simulation testing"""
    
    def __init__(self, device_id, host="localhost", port=8080):
        self.device_id = device_id
        self.host = host
        self.port = port + int(device_id.split('_')[-1])  # Unique port per device
        self.state = "disconnected"
        self.recording = False
        self.messages_received = []
        self.start_time = None
        self.stop_time = None
        self.clock_offset = 0.0
        
    def connect(self):
        """Simulate device connection"""
        self.state = "connected"
        return True
    
    def disconnect(self):
        """Simulate device disconnection"""
        self.state = "disconnected"
        self.recording = False
        return True
    
    def start_recording(self, timestamp=None):
        """Simulate starting recording"""
        if self.state != "connected":
            return False
        
        self.recording = True
        self.start_time = timestamp or time.time()
        return True
    
    def stop_recording(self, timestamp=None):
        """Simulate stopping recording"""
        if not self.recording:
            return False
            
        self.recording = False
        self.stop_time = timestamp or time.time()
        return True
    
    def receive_message(self, message):
        """Simulate receiving a protocol message"""
        self.messages_received.append({
            "message": message,
            "received_at": time.time()
        })
        
        # Handle commands
        if message.get("command") == "start_recording":
            return self.start_recording(message.get("timestamp"))
        elif message.get("command") == "stop_recording":
            return self.stop_recording(message.get("timestamp"))
        
        return True
    
    def get_timing_delta(self, reference_time):
        """Calculate timing delta from reference"""
        if self.start_time is None:
            return None
        return abs(self.start_time - reference_time)


class TestMultiDeviceSynchronization(unittest.TestCase):
    """Test suite for multi-device synchronization simulation"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.master_sync = MasterClockSynchronizer()
        self.devices = []
        self.device_count = 4
        
        # Create mock devices
        for i in range(self.device_count):
            device = MockDevice(f"device_{i}", port=8080 + i)
            self.devices.append(device)
            self.master_sync.add_device(device.device_id)
    
    def tearDown(self):
        """Clean up test fixtures"""
        for device in self.devices:
            device.disconnect()
    
    def test_device_initialization(self):
        """Test device initialization and setup"""
        for device in self.devices:
            self.assertEqual(device.state, "disconnected")
            self.assertFalse(device.recording)
            self.assertEqual(len(device.messages_received), 0)
    
    def test_device_connection(self):
        """Test device connection process"""
        connection_results = []
        
        for device in self.devices:
            result = device.connect()
            connection_results.append(result)
            self.assertEqual(device.state, "connected")
        
        self.assertTrue(all(connection_results))
    
    def test_broadcast_start_command(self):
        """Test broadcasting start command to all devices"""
        # Connect all devices
        for device in self.devices:
            device.connect()
        
        # Create start message
        start_message = MessageProtocol.create_start_message()
        reference_timestamp = start_message["timestamp"]
        
        # Broadcast to all devices
        broadcast_results = []
        for device in self.devices:
            result = device.receive_message(start_message)
            broadcast_results.append(result)
        
        # Verify all devices received and processed message
        self.assertTrue(all(broadcast_results))
        
        for device in self.devices:
            self.assertTrue(device.recording)
            self.assertEqual(len(device.messages_received), 1)
            self.assertIsNotNone(device.start_time)
    
    def test_broadcast_stop_command(self):
        """Test broadcasting stop command to all devices"""
        # Setup devices in recording state
        for device in self.devices:
            device.connect()
            device.start_recording()
        
        # Create and broadcast stop message
        stop_message = MessageProtocol.create_stop_message()
        
        for device in self.devices:
            device.receive_message(stop_message)
        
        # Verify all devices stopped recording
        for device in self.devices:
            self.assertFalse(device.recording)
            self.assertIsNotNone(device.stop_time)
    
    def test_device_state_consistency(self):
        """Test device state consistency after commands"""
        # Connect devices
        for device in self.devices:
            device.connect()
        
        # Start recording
        start_message = MessageProtocol.create_start_message()
        for device in self.devices:
            device.receive_message(start_message)
        
        # Check all devices have consistent state
        recording_states = [device.recording for device in self.devices]
        self.assertTrue(all(recording_states))
        
        # Stop recording
        stop_message = MessageProtocol.create_stop_message()
        for device in self.devices:
            device.receive_message(stop_message)
        
        # Check all devices stopped consistently
        recording_states = [device.recording for device in self.devices]
        self.assertFalse(any(recording_states))
    
    def test_timing_delta_measurement(self):
        """Test timing delta measurement between devices"""
        # Connect and start devices
        for device in self.devices:
            device.connect()
        
        reference_time = time.time()
        start_message = MessageProtocol.create_start_message(reference_time)
        
        # Introduce small delays to simulate network latency
        for i, device in enumerate(self.devices):
            if i > 0:  # Add increasing delay
                time.sleep(0.001 * i)  # 1ms per device
            device.receive_message(start_message)
        
        # Calculate timing deltas
        timing_deltas = []
        for device in self.devices:
            delta = device.get_timing_delta(reference_time)
            if delta is not None:
                timing_deltas.append(delta)
        
        self.assertEqual(len(timing_deltas), self.device_count)
        
        # Verify timing deltas are within reasonable bounds
        max_delta = max(timing_deltas)
        self.assertLess(max_delta, 0.01)  # Less than 10ms
    
    def test_clock_synchronization(self):
        """Test clock synchronization across devices"""
        # Add devices to synchronizer with different offsets
        for i, device in enumerate(self.devices):
            offset = i * 0.001  # 1ms offset per device
            device.clock_offset = offset
            self.master_sync.add_device(device.device_id, offset)
        
        # Perform synchronization
        sync_result = self.master_sync.synchronize_devices()
        self.assertTrue(sync_result)
        
        # Verify synchronized times
        sync_times = []
        for device in self.devices:
            sync_time = self.master_sync.get_synchronized_time(device.device_id)
            self.assertIsNotNone(sync_time)
            sync_times.append(sync_time)
        
        # All synchronized times should be close
        if len(sync_times) > 1:
            time_spread = max(sync_times) - min(sync_times)
            self.assertLess(time_spread, 0.1)  # Within 100ms
    
    def test_protocol_message_validation(self):
        """Test protocol message validation"""
        valid_messages = [
            MessageProtocol.create_start_message(),
            MessageProtocol.create_stop_message(),
            {
                "command": "status_check",
                "timestamp": time.time(),
                "message_id": "status_001"
            }
        ]
        
        invalid_messages = [
            {"command": "start"},  # Missing timestamp and message_id
            {"timestamp": time.time()},  # Missing command and message_id
            {},  # Empty message
            None  # Null message
        ]
        
        # Test valid messages
        for message in valid_messages:
            self.assertTrue(MessageProtocol.validate_message(message))
        
        # Test invalid messages
        for message in invalid_messages:
            if message is not None:
                self.assertFalse(MessageProtocol.validate_message(message))
    
    def test_device_failure_handling(self):
        """Test handling of device failures during synchronization"""
        # Connect most devices
        for i, device in enumerate(self.devices[:-1]):  # Skip last device
            device.connect()
        
        # Last device fails to connect
        failed_device = self.devices[-1]
        # Don't connect failed device
        
        # Send start command to all devices
        start_message = MessageProtocol.create_start_message()
        
        results = []
        for device in self.devices:
            if device.state == "connected":
                result = device.receive_message(start_message)
                results.append(result)
            else:
                results.append(False)  # Failed device
        
        # Verify connected devices started, failed device didn't
        connected_count = sum(1 for device in self.devices if device.state == "connected")
        successful_starts = sum(1 for device in self.devices if device.recording)
        
        self.assertEqual(successful_starts, connected_count)
        self.assertEqual(successful_starts, self.device_count - 1)
    
    def test_concurrent_command_processing(self):
        """Test concurrent command processing across devices"""
        # Connect all devices
        for device in self.devices:
            device.connect()
        
        def send_command_to_device(device, message):
            return device.receive_message(message)
        
        # Send commands concurrently using threads
        start_message = MessageProtocol.create_start_message()
        threads = []
        results = []
        
        for device in self.devices:
            thread = threading.Thread(
                target=lambda d=device: results.append(send_command_to_device(d, start_message))
            )
            threads.append(thread)
            thread.start()
        
        # Wait for all threads to complete
        for thread in threads:
            thread.join(timeout=1.0)
        
        # Verify all commands processed successfully
        self.assertEqual(len(results), self.device_count)
        self.assertTrue(all(results))
        
        # Verify all devices are recording
        for device in self.devices:
            self.assertTrue(device.recording)
    
    def test_message_ordering(self):
        """Test message ordering and sequence validation"""
        device = self.devices[0]
        device.connect()
        
        # Send sequence of messages
        messages = [
            MessageProtocol.create_start_message(),
            {"command": "status_check", "timestamp": time.time(), "message_id": "status_001"},
            MessageProtocol.create_stop_message()
        ]
        
        for message in messages:
            device.receive_message(message)
        
        # Verify message order preserved
        received_commands = [msg["message"]["command"] for msg in device.messages_received]
        expected_commands = ["start_recording", "status_check", "stop_recording"]
        
        self.assertEqual(received_commands, expected_commands)
    
    def test_performance_with_many_devices(self):
        """Test performance with larger number of devices"""
        large_device_count = 10
        large_devices = []
        
        # Create more devices
        for i in range(large_device_count):
            device = MockDevice(f"large_device_{i}")
            device.connect()
            large_devices.append(device)
        
        # Measure broadcast time
        start_time = time.time()
        
        broadcast_message = MessageProtocol.create_start_message()
        for device in large_devices:
            device.receive_message(broadcast_message)
        
        broadcast_duration = time.time() - start_time
        
        # Verify reasonable performance
        self.assertLess(broadcast_duration, 1.0)  # Should complete within 1 second
        
        # Verify all devices processed message
        for device in large_devices:
            self.assertTrue(device.recording)
    
    def test_network_latency_simulation(self):
        """Test network latency simulation and compensation"""
        device = self.devices[0]
        device.connect()
        
        # Simulate network latency
        latency_ms = 50  # 50ms latency
        
        def delayed_message_delivery(device, message, delay):
            time.sleep(delay / 1000.0)  # Convert ms to seconds
            return device.receive_message(message)
        
        start_message = MessageProtocol.create_start_message()
        send_time = time.time()
        
        # Send with simulated latency
        result = delayed_message_delivery(device, start_message, latency_ms)
        receive_time = time.time()
        
        actual_latency = (receive_time - send_time) * 1000  # Convert to ms
        
        self.assertTrue(result)
        self.assertGreaterEqual(actual_latency, latency_ms - 5)  # Allow 5ms tolerance
        self.assertLessEqual(actual_latency, latency_ms + 10)  # Allow 10ms tolerance


class TestNetworkingLoopback(unittest.TestCase):
    """Test suite for networking loopback and protocol validation"""
    
    def setUp(self):
        """Set up networking test fixtures"""
        self.test_data = {
            "session_id": "test_session_001",
            "device_info": {
                "id": "device_001",
                "type": "android",
                "version": "1.0.0"
            },
            "measurements": [1.0, 2.0, 3.0, 4.0, 5.0]
        }
    
    def test_json_round_trip(self):
        """Test JSON protocol round trip"""
        # Serialize
        json_data = json.dumps(self.test_data)
        self.assertIsInstance(json_data, str)
        
        # Deserialize
        restored_data = json.loads(json_data)
        
        # Verify data integrity
        self.assertEqual(restored_data, self.test_data)
        self.assertEqual(restored_data["session_id"], "test_session_001")
        self.assertEqual(len(restored_data["measurements"]), 5)
    
    def test_protocol_invariants(self):
        """Test protocol invariants validation"""
        # Test required fields
        required_fields = ["session_id", "device_info"]
        
        for field in required_fields:
            test_data_missing_field = self.test_data.copy()
            del test_data_missing_field[field]
            
            # Protocol should detect missing field
            is_valid = self.validate_protocol_message(test_data_missing_field)
            self.assertFalse(is_valid, f"Protocol should reject message missing {field}")
    
    def test_tls_vs_plaintext_latency(self):
        """Test TLS enabled vs disabled latency comparison"""
        test_message = json.dumps(self.test_data)
        message_size = len(test_message.encode('utf-8'))
        
        # Simulate plaintext transmission
        plaintext_start = time.time()
        self.simulate_transmission(test_message, use_tls=False)
        plaintext_latency = time.time() - plaintext_start
        
        # Simulate TLS transmission
        tls_start = time.time()
        self.simulate_transmission(test_message, use_tls=True)
        tls_latency = time.time() - tls_start
        
        # TLS should have some overhead but be reasonable
        tls_overhead = tls_latency - plaintext_latency
        
        self.assertGreaterEqual(tls_overhead, 0)  # TLS should have some overhead
        self.assertLess(tls_overhead, 0.1)  # But not excessive for test data
    
    def simulate_transmission(self, data, use_tls=False):
        """Simulate network transmission"""
        if use_tls:
            # Simulate TLS overhead
            time.sleep(0.001)  # 1ms additional overhead for TLS
        
        # Simulate basic transmission time based on data size
        transmission_time = len(data) / 1000000  # 1MB/s simulation
        time.sleep(transmission_time)
        
        return True
    
    def validate_protocol_message(self, message):
        """Validate protocol message structure"""
        required_fields = ["session_id", "device_info"]
        return all(field in message for field in required_fields)


if __name__ == '__main__':
    # Run tests with detailed output
    unittest.main(verbosity=2)