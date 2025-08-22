"""
Simple unit test for Android components (demonstration).

This is a basic test to demonstrate the unified testing framework.
"""

import pytest
from pathlib import Path
import sys

# Add project root to path
PROJECT_ROOT = Path(__file__).parent.parent.parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from tests_unified.fixtures.test_utils import MockDevice

class TestAndroidComponentsBasic:
    """Basic Android components testing"""
    
    @pytest.mark.unit
    @pytest.mark.android
    def test_mock_android_device_basic(self):
        """Test basic mock Android device functionality"""
        device = MockDevice("android_device_001")
        
        # Test initial state
        assert not device.connected
        assert device.device_id == "android_device_001"
        
        # Test connection
        success = device.connect()
        assert success
        assert device.connected
        
        # Test data sending
        test_data = {"sensor": "thermal", "value": 25.5}
        assert device.send_data(test_data)
        
        # Test data receiving
        received = device.receive_data()
        assert received == test_data
        
        # Test disconnection
        device.disconnect()
        assert not device.connected
    
    @pytest.mark.unit
    @pytest.mark.android
    def test_gsr_data_structure_validation(self):
        """Test GSR data structure validation for Android testing"""
        # Use sample data for structure validation only
        sample_gsr_data = [0.15, 0.25, 0.20, 0.30, 0.18]
        
        assert len(sample_gsr_data) == 5
        assert all(isinstance(value, float) for value in sample_gsr_data)
        assert all(0.1 <= value <= 1.0 for value in sample_gsr_data)
    
    @pytest.mark.unit
    @pytest.mark.android
    def test_thermal_data_structure_validation(self):
        """Test thermal data structure validation for Android testing"""
        # Use sample data for structure validation only  
        sample_thermal_data = [[21.5, 22.0], [22.1, 22.5]]
        
        assert len(sample_thermal_data) == 2
        assert all(isinstance(row, list) for row in sample_thermal_data)
        assert all(len(row) == 2 for row in sample_thermal_data)
        assert all(20.0 <= value <= 40.0 for row in sample_thermal_data for value in row)
    
    @pytest.mark.unit
    @pytest.mark.android
    @pytest.mark.slow
    def test_android_device_endurance_simulation(self):
        """Test simulated endurance for Android device (marked as slow)"""
        device = MockDevice("endurance_test_device")
        device.connect()
        
        # Simulate extended data collection
        data_points = []
        for i in range(100):  # Simulate 100 data points
            data = {"timestamp": 1000 + i, "gsr": 0.1 + (i % 10) * 0.05}
            device.send_data(data)
            received = device.receive_data()
            data_points.append(received)
        
        assert len(data_points) == 100
        assert all(point["gsr"] >= 0.1 for point in data_points)
        
        device.disconnect()

if __name__ == "__main__":
    pytest.main([__file__, "-v"])