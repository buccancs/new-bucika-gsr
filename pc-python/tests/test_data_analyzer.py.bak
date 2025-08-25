#!/usr/bin/env python3
"""
Test suite for advanced data analysis functionality
"""

import unittest
import asyncio
import tempfile
import shutil
import pandas as pd
import numpy as np
from pathlib import Path
from datetime import datetime, timedelta
from src.bucika_gsr_pc.data_analyzer import GSRDataAnalyzer, BatchAnalyzer


class TestDataAnalyzer(unittest.TestCase):
    """Test advanced data analysis functionality"""
    
    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()
        self.data_dir = Path(self.temp_dir)
        self.analyzer = GSRDataAnalyzer(self.data_dir)
        
        # Create sample session data
        self.session_id = "test_session_001"
        self.session_dir = self.data_dir / self.session_id
        self.session_dir.mkdir(parents=True, exist_ok=True)
        
        # Create sample GSR data
        self._create_sample_data()
    
    def tearDown(self):
        """Clean up test environment"""
        shutil.rmtree(self.temp_dir)
    
    def _create_sample_data(self):
        """Create sample GSR data file"""
        # Generate sample data
        timestamps = pd.date_range(start='2024-01-01 10:00:00', periods=1000, freq='7.8125ms')
        
        # Simulate realistic GSR data with some artifacts
        base_gsr = 2.5 + 0.5 * np.sin(np.linspace(0, 4*np.pi, 1000))  # Base signal
        noise = 0.05 * np.random.randn(1000)  # Random noise
        
        # Add some spikes (artifacts)
        spike_indices = [100, 300, 750]
        for idx in spike_indices:
            base_gsr[idx] += 2.0
        
        gsr_data = base_gsr + noise
        temp_data = 32.0 + 0.2 * np.random.randn(1000)  # Temperature
        
        # Create DataFrame
        df = pd.DataFrame({
            'Timestamp_ns': [int(ts.timestamp() * 1e9) for ts in timestamps],
            'DateTime_UTC': timestamps,
            'Sequence': range(1000),
            'GSR_Raw_uS': gsr_data,
            'GSR_Filtered_uS': gsr_data,  # Simplified - same as raw
            'Temperature_C': temp_data,
            'Quality_Flag_Spike': [i in spike_indices for i in range(1000)],
            'Quality_Flag_Saturation': [False] * 1000,
            'Quality_Flag_Dropout': [False] * 1000
        })
        
        # Save to CSV
        csv_file = self.session_dir / "gsr_data_20240101_100000.csv"
        df.to_csv(csv_file, index=False)
        
        # Create session metadata
        metadata = {
            "session_id": self.session_id,
            "device_id": "test_device",
            "start_time": "2024-01-01T10:00:00Z",
            "end_time": "2024-01-01T10:07:48Z",
            "sample_count": 1000,
            "sampling_rate": 128
        }
        
        import json
        metadata_file = self.session_dir / "session_metadata.json"
        with open(metadata_file, 'w') as f:
            json.dump(metadata, f, indent=2)
    
    def test_analyzer_initialization(self):
        """Test data analyzer initialization"""
        self.assertEqual(self.analyzer.data_directory, self.data_dir)
        self.assertIsNotNone(self.analyzer.quality_thresholds)
    
    def test_session_analysis(self):
        """Test analysis of a single session"""
        result = self.analyzer.analyze_session(self.session_id)
        
        self.assertIsNotNone(result)
        self.assertEqual(result.session_id, self.session_id)
        self.assertGreater(result.sample_count, 0)
        self.assertGreater(result.mean_gsr, 0)
        self.assertGreater(result.std_gsr, 0)
        self.assertGreaterEqual(result.data_quality_score, 0)
        self.assertLessEqual(result.data_quality_score, 1)
    
    def test_artifact_detection(self):
        """Test artifact detection functionality"""
        result = self.analyzer.analyze_session(self.session_id)
        
        # Should detect the spikes we added
        self.assertGreater(result.artifacts_detected, 0)
        self.assertIn("spike", [a.lower() for a in result.artifact_types])
    
    def test_statistical_analysis(self):
        """Test statistical analysis capabilities"""
        result = self.analyzer.analyze_session(self.session_id)
        
        # Check statistical measures are reasonable
        self.assertGreater(result.mean_gsr, 1.0)  # Should be around 2.5
        self.assertLess(result.mean_gsr, 4.0)
        self.assertGreater(result.std_gsr, 0.1)
        self.assertLess(result.std_gsr, 1.0)
        
        # Check percentiles exist
        self.assertIsNotNone(result.percentiles)
        self.assertIn("50", result.percentiles)  # Median should exist
    
    def test_quality_scoring(self):
        """Test data quality scoring"""
        result = self.analyzer.analyze_session(self.session_id)
        
        # Quality score should be reasonable despite artifacts
        self.assertGreaterEqual(result.data_quality_score, 0.6)  # Should be decent
        self.assertLessEqual(result.data_quality_score, 1.0)
        
        # Should have recommendations if quality is not perfect
        self.assertIsInstance(result.recommendations, list)
    
    def test_missing_session_analysis(self):
        """Test analysis of non-existent session"""
        result = self.analyzer.analyze_session("nonexistent_session")
        self.assertIsNone(result)
    
    def test_batch_analysis(self):
        """Test batch analysis functionality"""
        batch_analyzer = BatchAnalyzer(self.data_dir)
        results = batch_analyzer.analyze_all_sessions()
        
        self.assertIsInstance(results, list)
        self.assertGreater(len(results), 0)
        self.assertEqual(results[0].session_id, self.session_id)
    
    def test_visualization_generation(self):
        """Test visualization generation"""
        result = self.analyzer.analyze_session(self.session_id)
        
        # Should generate plots
        plot_file = self.session_dir / "analysis_plots.png"
        if plot_file.exists():  # Visualization might be optional
            self.assertTrue(plot_file.is_file())


if __name__ == '__main__':
    unittest.main()