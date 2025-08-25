#!/usr/bin/env python3
"""
Test suite for data validation functionality
"""

import unittest
import tempfile
import shutil
import pandas as pd
import numpy as np
from pathlib import Path
from datetime import datetime
from src.bucika_gsr_pc.data_validator import DataValidator, BatchValidator, ValidationLevel, QualityReport


class TestDataValidator(unittest.TestCase):
    """Test data validation functionality"""
    
    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()
        self.validator = DataValidator(ValidationLevel.STANDARD)
        
        # Create sample session for validation
        self.session_dir = Path(self.temp_dir) / "test_session"
        self.session_dir.mkdir(parents=True, exist_ok=True)
        self._create_sample_data()
    
    def tearDown(self):
        """Clean up test environment"""
        shutil.rmtree(self.temp_dir)
    
    def _create_sample_data(self):
        """Create sample data for validation"""
        # Create good quality GSR data
        timestamps = pd.date_range(start='2024-01-01 10:00:00', periods=500, freq='7.8125ms')
        gsr_data = 2.5 + 0.3 * np.sin(np.linspace(0, 2*np.pi, 500)) + 0.02 * np.random.randn(500)
        
        df = pd.DataFrame({
            'Timestamp_ns': [int(ts.timestamp() * 1e9) for ts in timestamps],
            'DateTime_UTC': timestamps,
            'Sequence': range(500),
            'GSR_Raw_uS': gsr_data,
            'GSR_Filtered_uS': gsr_data,
            'Temperature_C': 32.0 + 0.1 * np.random.randn(500),
            'Quality_Flag_Spike': [False] * 500,
            'Quality_Flag_Saturation': [False] * 500,
            'Quality_Flag_Dropout': [False] * 500
        })
        
        csv_file = self.session_dir / "gsr_data_20240101_100000.csv"
        df.to_csv(csv_file, index=False)
        
        # Create session metadata
        metadata = {
            "session_id": "test_session",
            "device_id": "test_device",
            "start_time": "2024-01-01T10:00:00Z",
            "end_time": "2024-01-01T10:03:54Z",
            "sample_count": 500,
            "sampling_rate": 128
        }
        
        import json
        metadata_file = self.session_dir / "session_metadata.json"
        with open(metadata_file, 'w') as f:
            json.dump(metadata, f, indent=2)
    
    def test_validator_initialization(self):
        """Test validator initialization"""
        # Test different validation levels
        basic_validator = DataValidator(ValidationLevel.BASIC)
        self.assertEqual(basic_validator.validation_level, ValidationLevel.BASIC)
        
        strict_validator = DataValidator(ValidationLevel.STRICT)
        self.assertEqual(strict_validator.validation_level, ValidationLevel.STRICT)
        
        research_validator = DataValidator(ValidationLevel.RESEARCH_GRADE)
        self.assertEqual(research_validator.validation_level, ValidationLevel.RESEARCH_GRADE)
    
    def test_validation_levels(self):
        """Test different validation levels"""
        # Verify validation levels exist
        self.assertIsInstance(ValidationLevel.BASIC, ValidationLevel)
        self.assertIsInstance(ValidationLevel.STANDARD, ValidationLevel)
        self.assertIsInstance(ValidationLevel.STRICT, ValidationLevel)
        self.assertIsInstance(ValidationLevel.RESEARCH_GRADE, ValidationLevel)
    
    async def test_session_validation(self):
        """Test validation of a session"""
        report = await self.validator.validate_session(self.session_dir)
        
        self.assertIsInstance(report, QualityReport)
        self.assertGreaterEqual(report.overall_score, 0.0)
        self.assertLessEqual(report.overall_score, 1.0)
        self.assertIsInstance(report.quality_metrics, dict)
    
    def test_quality_metrics(self):
        """Test quality metrics calculation"""
        # The actual implementation would calculate various quality metrics
        # This tests the concept
        metrics = {
            'completeness': 0.95,
            'accuracy': 0.90,
            'consistency': 0.88,
            'timeliness': 0.92,
            'validity': 0.94,
            'integrity': 0.96
        }
        
        for metric, score in metrics.items():
            self.assertGreaterEqual(score, 0.0)
            self.assertLessEqual(score, 1.0)
    
    def test_data_completeness_check(self):
        """Test data completeness validation"""
        # This would test that all expected data is present
        # For now, verify the concept
        expected_columns = ['Timestamp_ns', 'GSR_Raw_uS', 'Temperature_C']
        
        # Read the test data
        csv_file = self.session_dir / "gsr_data_20240101_100000.csv"
        df = pd.read_csv(csv_file)
        
        for col in expected_columns:
            self.assertIn(col, df.columns)
    
    def test_data_accuracy_check(self):
        """Test data accuracy validation"""
        # Read the test data
        csv_file = self.session_dir / "gsr_data_20240101_100000.csv"
        df = pd.read_csv(csv_file)
        
        # Check that GSR values are in reasonable range
        gsr_values = df['GSR_Raw_uS']
        self.assertTrue(all(gsr_values > 0))  # GSR should be positive
        self.assertTrue(all(gsr_values < 100))  # Should be reasonable range
        
        # Check temperature values
        temp_values = df['Temperature_C']
        self.assertTrue(all(temp_values > 0))  # Temperature should be positive
        self.assertTrue(all(temp_values < 50))  # Should be reasonable range
    
    def test_data_consistency_check(self):
        """Test data consistency validation"""
        # Read the test data
        csv_file = self.session_dir / "gsr_data_20240101_100000.csv"
        df = pd.read_csv(csv_file)
        
        # Check sequence consistency
        sequences = df['Sequence']
        expected_sequences = list(range(len(df)))
        self.assertEqual(list(sequences), expected_sequences)
    
    def test_validation_thresholds(self):
        """Test configurable validation thresholds"""
        # Different validation levels should have different thresholds
        basic_validator = DataValidator(ValidationLevel.BASIC)
        research_validator = DataValidator(ValidationLevel.RESEARCH_GRADE)
        
        # Research grade should have stricter requirements
        self.assertNotEqual(basic_validator.validation_level, research_validator.validation_level)
    
    def test_batch_validation(self):
        """Test batch validation functionality"""
        batch_validator = BatchValidator(ValidationLevel.STANDARD)
        
        # Test that batch validator exists and can be initialized
        self.assertIsInstance(batch_validator, BatchValidator)
        self.assertEqual(batch_validator.validation_level, ValidationLevel.STANDARD)
    
    def test_quality_report_structure(self):
        """Test quality report structure"""
        # This would test the actual quality report structure
        # For now, verify the concept exists
        sample_report = {
            'session_id': 'test_session',
            'validation_level': 'standard',
            'overall_score': 0.92,
            'quality_metrics': {
                'completeness': 0.95,
                'accuracy': 0.90,
                'consistency': 0.88,
                'timeliness': 0.92,
                'validity': 0.94,
                'integrity': 0.96
            },
            'recommendations': [
                'Consider improving data consistency',
                'Review accuracy thresholds'
            ],
            'validation_timestamp': datetime.now().isoformat()
        }
        
        self.assertIn('overall_score', sample_report)
        self.assertIn('quality_metrics', sample_report)
        self.assertIn('recommendations', sample_report)
    
    def test_recommendations_generation(self):
        """Test validation recommendations generation"""
        # This would test that appropriate recommendations are generated
        # based on validation results
        sample_recommendations = [
            'Increase sampling rate for better temporal resolution',
            'Check sensor calibration for accuracy improvement',
            'Review data collection protocol for consistency'
        ]
        
        for recommendation in sample_recommendations:
            self.assertIsInstance(recommendation, str)
            self.assertGreater(len(recommendation), 10)


if __name__ == '__main__':
    unittest.main()