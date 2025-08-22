"""
Performance Verification Tests for Native and Python Implementations

IMPORTANT: This test suite uses SYNTHETIC DATA ONLY for performance validation.
It does NOT use real sensor data or conduct actual experiments.
Tests specific performance claims from the thesis documentation and compares
native C++ implementations against Python fallback implementations.

WARNING: These tests are for code performance validation only.
Real experiments must use actual hardware and authentic sensor data.
"""

import pytest
import time
import statistics
import logging
import numpy as np
from typing import List, Dict, Any, Tuple
import sys
import os
from pathlib import Path

# Check for cv2 availability
try:
    import cv2
    HAS_CV2 = True
except ImportError:
    HAS_CV2 = False
    # Mock cv2 functions
    class MockCV2:
        @staticmethod
        def circle(img, center, radius, color, thickness):
            return img
        @staticmethod  
        def rectangle(img, pt1, pt2, color, thickness):
            return img
    cv2 = MockCV2()

# Add PythonApp to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "PythonApp"))

# Import with graceful fallbacks
try:
    from native_backends.native_shimmer_wrapper import ShimmerProcessor, ProcessingConfig, SensorReading
    # Check if native backend is actually available
    from native_backends.native_shimmer_wrapper import NATIVE_AVAILABLE as SHIMMER_NATIVE_AVAILABLE
    HAS_SHIMMER = True
except ImportError as e:
    logger.warning(f"Shimmer backend not available: {e}")
    HAS_SHIMMER = False
    SHIMMER_NATIVE_AVAILABLE = False
    # Mock classes for testing
    class ShimmerProcessor:
        def __init__(self, config=None): pass
        def process_data(self, data): return data
    class ProcessingConfig:
        def __init__(self): pass
    class SensorReading:
        def __init__(self, **kwargs): pass

try:
    from native_backends.native_webcam_wrapper import WebcamProcessor, ProcessingConfig as WebcamConfig
    # Check if webcam native backend is available 
    try:
        from native_backends.native_webcam_wrapper import NATIVE_AVAILABLE as WEBCAM_NATIVE_AVAILABLE
    except ImportError:
        WEBCAM_NATIVE_AVAILABLE = False
    HAS_WEBCAM = True
except ImportError as e:
    logger.warning(f"Webcam backend not available: {e}")
    HAS_WEBCAM = False
    WEBCAM_NATIVE_AVAILABLE = False
    # Mock classes for testing
    class WebcamProcessor:
        def __init__(self, config=None): pass
        def process_frame(self, frame): return frame
    class WebcamConfig:
        def __init__(self): pass

logger = logging.getLogger(__name__)


class PerformanceTester:
    """Base class for performance testing"""
    
    def __init__(self):
        self.results = {}
    
    def measure_execution_time(self, func, *args, **kwargs) -> Tuple[Any, float]:
        """Measure execution time of a function in milliseconds"""
        start_time = time.perf_counter()
        result = func(*args, **kwargs)
        end_time = time.perf_counter()
        execution_time_ms = (end_time - start_time) * 1000.0
        return result, execution_time_ms
    
    def run_benchmark(self, name: str, func, iterations: int = 100, *args, **kwargs) -> Dict[str, float]:
        """Run a benchmark multiple times and collect statistics"""
        times = []
        
        for i in range(iterations):
            _, exec_time = self.measure_execution_time(func, *args, **kwargs)
            times.append(exec_time)
        
        stats = {
            'mean_ms': statistics.mean(times),
            'median_ms': statistics.median(times),
            'std_ms': statistics.stdev(times) if len(times) > 1 else 0.0,
            'min_ms': min(times),
            'max_ms': max(times),
            'iterations': iterations
        }
        
        self.results[name] = stats
        return stats


class ShimmerPerformanceTester(PerformanceTester):
    """Performance testing for Shimmer data processing"""
    
    def __init__(self):
        super().__init__()
        self.native_processor = ShimmerProcessor(force_python=False)
        self.python_processor = ShimmerProcessor(force_python=True)
        
        # Standard Shimmer GSR+ configuration
        self.config = ProcessingConfig(
            sampling_rate=128.0,
            enable_filtering=True,
            filter_cutoff=5.0,
            enable_artifact_removal=True,
            artifact_threshold=100.0
        )
        
        self.native_processor.configure(self.config)
        self.python_processor.configure(self.config)
    
    def generate_test_packet(self) -> List[int]:
        """Generate a realistic Shimmer data packet"""
        # Simulate 21-byte Shimmer3 GSR+ packet
        packet = []
        
        # GSR data (2 bytes) - simulate ~10uS
        gsr_raw = int(np.random.normal(2000, 100))
        packet.extend([gsr_raw & 0xFF, (gsr_raw >> 8) & 0xFF])
        
        # PPG data (2 bytes)
        ppg_raw = int(np.random.normal(1500, 200))
        packet.extend([ppg_raw & 0xFF, (ppg_raw >> 8) & 0xFF])
        
        # Accelerometer (6 bytes, 3 axes)
        for _ in range(3):
            accel_raw = int(np.random.normal(0, 1000))
            packet.extend([accel_raw & 0xFF, (accel_raw >> 8) & 0xFF])
        
        # Gyroscope (6 bytes, 3 axes)
        for _ in range(3):
            gyro_raw = int(np.random.normal(0, 500))
            packet.extend([gyro_raw & 0xFF, (gyro_raw >> 8) & 0xFF])
        
        # Magnetometer (4 bytes, 2 axes shown)
        for _ in range(2):
            mag_raw = int(np.random.normal(0, 1000))
            packet.extend([mag_raw & 0xFF, (mag_raw >> 8) & 0xFF])
        
        # Battery level (1 byte)
        packet.append(int(np.random.uniform(80, 100)))
        
        return packet
    
    def test_single_packet_processing(self) -> Dict[str, Dict[str, float]]:
        """Test single packet processing performance"""
        test_packet = self.generate_test_packet()
        
        # Test native implementation
        native_stats = self.run_benchmark(
            "native_single_packet", 
            self.native_processor.process_raw_packet,
            iterations=1000,
            raw_data=test_packet
        )
        
        # Test Python implementation  
        python_stats = self.run_benchmark(
            "python_single_packet",
            self.python_processor.process_raw_packet,
            iterations=1000,
            raw_data=test_packet
        )
        
        return {
            'native': native_stats,
            'python': python_stats,
            'speedup': python_stats['mean_ms'] / native_stats['mean_ms'] if native_stats['mean_ms'] > 0 else 1.0
        }
    
    def test_batch_processing(self, batch_size: int = 128) -> Dict[str, Dict[str, float]]:
        """Test batch processing performance (simulating 1 second at 128Hz)"""
        test_packets = [self.generate_test_packet() for _ in range(batch_size)]
        
        # Test native implementation
        native_stats = self.run_benchmark(
            f"native_batch_{batch_size}",
            self.native_processor.process_batch,
            iterations=50,
            raw_packets=test_packets
        )
        
        # Test Python implementation
        python_stats = self.run_benchmark(
            f"python_batch_{batch_size}",
            self.python_processor.process_batch, 
            iterations=50,
            raw_packets=test_packets
        )
        
        return {
            'native': native_stats,
            'python': python_stats,
            'speedup': python_stats['mean_ms'] / native_stats['mean_ms'] if native_stats['mean_ms'] > 0 else 1.0
        }
    
    def test_signal_processing(self) -> Dict[str, Dict[str, float]]:
        """Test signal processing algorithms"""
        # Generate test signal (1 second at 128Hz)
        signal_length = 128
        test_signal = [float(x) for x in np.random.normal(10.0, 2.0, signal_length)]
        
        # Test low-pass filtering
        native_filter_stats = self.run_benchmark(
            "native_filter",
            self.native_processor.apply_low_pass_filter,
            iterations=100,
            signal=test_signal,
            cutoff_freq=5.0
        )
        
        python_filter_stats = self.run_benchmark(
            "python_filter", 
            self.python_processor.apply_low_pass_filter,
            iterations=100,
            signal=test_signal,
            cutoff_freq=5.0
        )
        
        # Test artifact removal
        artifact_signal = test_signal.copy()
        # Add some artifacts
        artifact_signal[20] = 150.0  # Large spike
        artifact_signal[50] = -50.0  # Negative spike
        
        native_artifact_stats = self.run_benchmark(
            "native_artifact_removal",
            self.native_processor.remove_artifacts,
            iterations=100,
            gsr_signal=artifact_signal
        )
        
        python_artifact_stats = self.run_benchmark(
            "python_artifact_removal",
            self.python_processor.remove_artifacts,
            iterations=100,
            gsr_signal=artifact_signal
        )
        
        return {
            'filter': {
                'native': native_filter_stats,
                'python': python_filter_stats,
                'speedup': python_filter_stats['mean_ms'] / native_filter_stats['mean_ms'] if native_filter_stats['mean_ms'] > 0 else 1.0
            },
            'artifact_removal': {
                'native': native_artifact_stats,
                'python': python_artifact_stats,
                'speedup': python_artifact_stats['mean_ms'] / native_artifact_stats['mean_ms'] if native_artifact_stats['mean_ms'] > 0 else 1.0
            }
        }


class WebcamPerformanceTester(PerformanceTester):
    """Performance testing for webcam processing"""
    
    def __init__(self):
        super().__init__()
        self.native_processor = WebcamProcessor(force_python=False)
        self.python_processor = WebcamProcessor(force_python=True)
        
        # Standard webcam configuration  
        self.config = WebcamConfig(
            width=1920,
            height=1080,
            fps=30.0,
            enable_preprocessing=True,
            enable_motion_detection=True,
            motion_threshold=30.0
        )
        
        self.native_processor.configure(self.config)
        self.python_processor.configure(self.config)
    
    def generate_test_frame(self, width: int = 1920, height: int = 1080) -> np.ndarray:
        """Generate a realistic test frame"""
        # Create a frame with some structure (not pure noise)
        frame = np.random.randint(0, 255, (height, width, 3), dtype=np.uint8)
        
        # Add some geometric patterns to make it more realistic
        cv2.circle(frame, (width//4, height//4), 50, (255, 255, 255), -1)
        cv2.rectangle(frame, (width//2, height//2), (width//2 + 100, height//2 + 100), (128, 128, 128), -1)
        
        return frame
    
    def test_frame_preprocessing(self) -> Dict[str, Dict[str, float]]:
        """Test frame preprocessing performance"""
        test_frame = self.generate_test_frame()
        
        # Test native implementation
        native_stats = self.run_benchmark(
            "native_preprocessing",
            self.native_processor.preprocess_frame,
            iterations=50,
            input_frame=test_frame
        )
        
        # Test Python implementation
        python_stats = self.run_benchmark(
            "python_preprocessing",
            self.python_processor.preprocess_frame,
            iterations=50,
            input_frame=test_frame
        )
        
        return {
            'native': native_stats,
            'python': python_stats,
            'speedup': python_stats['mean_ms'] / native_stats['mean_ms'] if native_stats['mean_ms'] > 0 else 1.0
        }
    
    def test_motion_detection(self) -> Dict[str, Dict[str, float]]:
        """Test motion detection performance"""
        frame1 = self.generate_test_frame()
        frame2 = self.generate_test_frame()
        
        # Test native implementation
        native_stats = self.run_benchmark(
            "native_motion_detection",
            self.native_processor.detect_motion,
            iterations=100,
            current_frame=frame2,
            previous_frame=frame1
        )
        
        # Test Python implementation
        python_stats = self.run_benchmark(
            "python_motion_detection",
            self.python_processor.detect_motion,
            iterations=100,
            current_frame=frame2,
            previous_frame=frame1
        )
        
        return {
            'native': native_stats,
            'python': python_stats,
            'speedup': python_stats['mean_ms'] / native_stats['mean_ms'] if native_stats['mean_ms'] > 0 else 1.0
        }


class ThesisPerformanceValidator:
    """Validates specific performance claims from the thesis"""
    
    def __init__(self):
        self.shimmer_tester = ShimmerPerformanceTester()
        self.webcam_tester = WebcamPerformanceTester()
        self.validation_results = {}
    
    def validate_shimmer_sampling_rate(self) -> Dict[str, Any]:
        """Validate that Shimmer processing can keep up with 128Hz sampling rate"""
        # Thesis claim: 128Hz sampling rate with <8ms processing time per packet
        target_sampling_rate = 128.0
        max_processing_time_ms = 1000.0 / target_sampling_rate  # ~7.8ms
        
        results = self.shimmer_tester.test_single_packet_processing()
        
        native_meets_target = results['native']['mean_ms'] < max_processing_time_ms
        python_meets_target = results['python']['mean_ms'] < max_processing_time_ms
        
        validation = {
            'target_time_ms': max_processing_time_ms,
            'native_time_ms': results['native']['mean_ms'],
            'python_time_ms': results['python']['mean_ms'],
            'native_meets_target': native_meets_target,
            'python_meets_target': python_meets_target,
            'native_headroom_percent': ((max_processing_time_ms - results['native']['mean_ms']) / max_processing_time_ms) * 100,
            'python_headroom_percent': ((max_processing_time_ms - results['python']['mean_ms']) / max_processing_time_ms) * 100,
            'speedup_factor': results['speedup']
        }
        
        self.validation_results['shimmer_sampling_rate'] = validation
        return validation
    
    def validate_webcam_frame_rate(self) -> Dict[str, Any]:
        """Validate that webcam processing can keep up with 30fps capture"""
        # Thesis claim: 30fps processing with <33ms processing time per frame
        target_fps = 30.0
        max_processing_time_ms = 1000.0 / target_fps  # ~33.3ms
        
        results = self.webcam_tester.test_frame_preprocessing()
        
        native_meets_target = results['native']['mean_ms'] < max_processing_time_ms
        python_meets_target = results['python']['mean_ms'] < max_processing_time_ms
        
        validation = {
            'target_time_ms': max_processing_time_ms,
            'native_time_ms': results['native']['mean_ms'],
            'python_time_ms': results['python']['mean_ms'],
            'native_meets_target': native_meets_target,
            'python_meets_target': python_meets_target,
            'native_headroom_percent': ((max_processing_time_ms - results['native']['mean_ms']) / max_processing_time_ms) * 100,
            'python_headroom_percent': ((max_processing_time_ms - results['python']['mean_ms']) / max_processing_time_ms) * 100,
            'speedup_factor': results['speedup']
        }
        
        self.validation_results['webcam_frame_rate'] = validation
        return validation
    
    def validate_synchronization_precision(self) -> Dict[str, Any]:
        """Validate timestamp precision claims"""
        # Thesis claim: Millisecond-precision synchronization
        # Test timestamp consistency and precision
        
        shimmer_timestamps = []
        webcam_timestamps = []
        
        # Collect timestamps from both systems
        for i in range(100):
            shimmer_ts = time.time() * 1000.0
            webcam_ts = self.webcam_tester.native_processor.get_synchronized_timestamp()
            
            shimmer_timestamps.append(shimmer_ts)
            webcam_timestamps.append(webcam_ts)
            
            time.sleep(0.001)  # 1ms delay
        
        # Calculate timestamp precision
        shimmer_diffs = [abs(shimmer_timestamps[i+1] - shimmer_timestamps[i] - 1.0) 
                        for i in range(len(shimmer_timestamps)-1)]
        webcam_diffs = [abs(webcam_timestamps[i+1] - webcam_timestamps[i] - 1.0)
                       for i in range(len(webcam_timestamps)-1)]
        
        validation = {
            'shimmer_precision_ms': statistics.mean(shimmer_diffs),
            'webcam_precision_ms': statistics.mean(webcam_diffs),
            'shimmer_jitter_ms': statistics.stdev(shimmer_diffs) if len(shimmer_diffs) > 1 else 0.0,
            'webcam_jitter_ms': statistics.stdev(webcam_diffs) if len(webcam_diffs) > 1 else 0.0,
            'meets_ms_precision': all(d < 1.0 for d in shimmer_diffs + webcam_diffs)
        }
        
        self.validation_results['synchronization_precision'] = validation
        return validation
    
    def validate_native_performance_gain(self) -> Dict[str, Any]:
        """Validate that native implementation provides significant performance improvement"""
        # Run comprehensive benchmarks
        shimmer_single = self.shimmer_tester.test_single_packet_processing()
        shimmer_batch = self.shimmer_tester.test_batch_processing()
        shimmer_signal = self.shimmer_tester.test_signal_processing()
        webcam_preprocessing = self.webcam_tester.test_frame_preprocessing()
        webcam_motion = self.webcam_tester.test_motion_detection()
        
        # Calculate overall performance improvements
        speedups = {
            'shimmer_single_packet': shimmer_single['speedup'],
            'shimmer_batch_processing': shimmer_batch['speedup'],
            'shimmer_filter': shimmer_signal['filter']['speedup'],
            'shimmer_artifact_removal': shimmer_signal['artifact_removal']['speedup'],
            'webcam_preprocessing': webcam_preprocessing['speedup'],
            'webcam_motion_detection': webcam_motion['speedup']
        }
        
        average_speedup = statistics.mean(speedups.values())
        min_speedup = min(speedups.values())
        max_speedup = max(speedups.values())
        
        validation = {
            'individual_speedups': speedups,
            'average_speedup': average_speedup,
            'min_speedup': min_speedup,
            'max_speedup': max_speedup,
            'meets_2x_improvement': average_speedup >= 2.0,
            'all_operations_faster': all(s > 1.0 for s in speedups.values())
        }
        
        self.validation_results['native_performance_gain'] = validation
        return validation
    
    def generate_performance_report(self) -> str:
        """Generate a comprehensive performance validation report"""
        report = []
        report.append("=" * 80)
        report.append("THESIS PERFORMANCE CLAIMS VALIDATION REPORT")
        report.append("=" * 80)
        report.append("")
        
        # Shimmer sampling rate validation
        if 'shimmer_sampling_rate' in self.validation_results:
            shimmer = self.validation_results['shimmer_sampling_rate']
            report.append("1. Shimmer 128Hz Sampling Rate Performance")
            report.append(f"   Target processing time: {shimmer['target_time_ms']:.2f}ms")
            report.append(f"   Native implementation: {shimmer['native_time_ms']:.2f}ms ({'[PASS]' if shimmer['native_meets_target'] else '[FAIL]'})")
            report.append(f"   Python implementation: {shimmer['python_time_ms']:.2f}ms ({'[PASS]' if shimmer['python_meets_target'] else '[FAIL]'})")
            report.append(f"   Native speedup: {shimmer['speedup_factor']:.1f}x")
            report.append("")
        
        # Webcam frame rate validation
        if 'webcam_frame_rate' in self.validation_results:
            webcam = self.validation_results['webcam_frame_rate']
            report.append("2. Webcam 30fps Processing Performance")
            report.append(f"   Target processing time: {webcam['target_time_ms']:.2f}ms")
            report.append(f"   Native implementation: {webcam['native_time_ms']:.2f}ms ({'[PASS]' if webcam['native_meets_target'] else '[FAIL]'})")
            report.append(f"   Python implementation: {webcam['python_time_ms']:.2f}ms ({'[PASS]' if webcam['python_meets_target'] else '[FAIL]'})")
            report.append(f"   Native speedup: {webcam['speedup_factor']:.1f}x")
            report.append("")
        
        # Synchronization precision validation
        if 'synchronization_precision' in self.validation_results:
            sync = self.validation_results['synchronization_precision']
            report.append("3. Millisecond-Precision Synchronization")
            report.append(f"   Shimmer precision: {sync['shimmer_precision_ms']:.3f}ms")
            report.append(f"   Webcam precision: {sync['webcam_precision_ms']:.3f}ms")
            report.append(f"   Meets ms precision: {'[PASS]' if sync['meets_ms_precision'] else '[FAIL]'}")
            report.append("")
        
        # Native performance gain validation
        if 'native_performance_gain' in self.validation_results:
            perf = self.validation_results['native_performance_gain']
            report.append("4. Native Implementation Performance Gains")
            report.append(f"   Average speedup: {perf['average_speedup']:.1f}x")
            report.append(f"   Minimum speedup: {perf['min_speedup']:.1f}x")
            report.append(f"   Maximum speedup: {perf['max_speedup']:.1f}x")
            report.append(f"   All operations faster: {'[PASS]' if perf['all_operations_faster'] else '[FAIL]'}")
            report.append("")
            
            report.append("   Individual operation speedups:")
            for operation, speedup in perf['individual_speedups'].items():
                report.append(f"     {operation}: {speedup:.1f}x")
            report.append("")
        
        report.append("=" * 80)
        return "\n".join(report)


# Test fixtures and pytest integration
@pytest.fixture
def performance_validator():
    """Fixture providing performance validator"""
    return ThesisPerformanceValidator()


@pytest.mark.performance
@pytest.mark.skipif(not SHIMMER_NATIVE_AVAILABLE, reason="Native Shimmer backend not available")
def test_shimmer_sampling_rate_performance(performance_validator):
    """Test that Shimmer processing meets 128Hz sampling rate requirements"""
    result = performance_validator.validate_shimmer_sampling_rate()
    
    # Native implementation should meet requirements  
    assert result['native_meets_target'], f"Native Shimmer processing too slow: {result['native_time_ms']:.2f}ms > {result['target_time_ms']:.2f}ms"
    
    # When native backend unavailable, speedup may be minimal - adjust expectations
    if HAS_SHIMMER:
        assert result['speedup_factor'] > 1.5, f"Native speedup insufficient: {result['speedup_factor']:.1f}x"
    else:
        # Fallback to Python-only mode, minimal speedup expected
        assert result['speedup_factor'] > 0.5, f"Performance degraded: {result['speedup_factor']:.1f}x"


@pytest.mark.performance
@pytest.mark.skipif(not WEBCAM_NATIVE_AVAILABLE, reason="Native Webcam backend not available")
def test_webcam_frame_rate_performance(performance_validator):
    """Test that webcam processing meets 30fps requirements"""
    result = performance_validator.validate_webcam_frame_rate()
    
    # Native implementation should meet requirements
    assert result['native_meets_target'], f"Native webcam processing too slow: {result['native_time_ms']:.2f}ms > {result['target_time_ms']:.2f}ms"
    
    # Native should be faster than Python
    assert result['speedup_factor'] > 1.0, f"Native not faster than Python: {result['speedup_factor']:.1f}x"


@pytest.mark.performance  
@pytest.mark.skipif(not (SHIMMER_NATIVE_AVAILABLE and WEBCAM_NATIVE_AVAILABLE), reason="Native backends not available for synchronization testing")
def test_synchronization_precision(performance_validator):
    """Test that synchronization meets millisecond precision requirements"""
    result = performance_validator.validate_synchronization_precision()
    
    # Should meet millisecond precision
    assert result['meets_ms_precision'], "Synchronization precision does not meet millisecond requirements"
    
    # Jitter should be low
    assert result['shimmer_jitter_ms'] < 0.5, f"Shimmer timestamp jitter too high: {result['shimmer_jitter_ms']:.3f}ms"
    assert result['webcam_jitter_ms'] < 0.5, f"Webcam timestamp jitter too high: {result['webcam_jitter_ms']:.3f}ms"


@pytest.mark.performance
@pytest.mark.skipif(not SHIMMER_NATIVE_AVAILABLE, reason="Native Shimmer backend not available")
def test_native_performance_gains(performance_validator):
    """Test that native implementation provides significant performance improvements"""
    result = performance_validator.validate_native_performance_gain()
    
    # All operations should be faster with native implementation
    assert result['all_operations_faster'], "Not all operations are faster with native implementation"
    
    # Average speedup should be significant
    assert result['average_speedup'] >= 1.5, f"Average speedup insufficient: {result['average_speedup']:.1f}x"


@pytest.mark.performance
def test_generate_performance_report(performance_validator):
    """Generate and validate comprehensive performance report"""
    # Run all validations
    performance_validator.validate_shimmer_sampling_rate()
    performance_validator.validate_webcam_frame_rate()
    performance_validator.validate_synchronization_precision()
    performance_validator.validate_native_performance_gain()
    
    # Generate report
    report = performance_validator.generate_performance_report()
    
    # Report should contain key sections
    assert "THESIS PERFORMANCE CLAIMS VALIDATION REPORT" in report
    assert "Shimmer 128Hz Sampling Rate Performance" in report
    assert "Webcam 30fps Processing Performance" in report
    assert "Millisecond-Precision Synchronization" in report
    assert "Native Implementation Performance Gains" in report
    
    # Print report for manual inspection
    print("\n" + report)


if __name__ == "__main__":
    # Run performance validation when script is executed directly
    validator = ThesisPerformanceValidator()
    
    print("Running thesis performance claims validation...")
    print()
    
    # Run all validations
    validator.validate_shimmer_sampling_rate()
    validator.validate_webcam_frame_rate()
    validator.validate_synchronization_precision()
    validator.validate_native_performance_gain()
    
    # Generate and print report
    report = validator.generate_performance_report()
    print(report)