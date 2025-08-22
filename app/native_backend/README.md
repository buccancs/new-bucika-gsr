# Native Backend Implementation

This directory contains high-performance C++ implementations of performance-critical sensor processing algorithms with PyBind11 Python bindings.

## Overview

The native backend provides significant performance improvements for:
- **Shimmer GSR data processing**: Real-time sensor data parsing, filtering, and artifact removal
- **Webcam frame processing**: Video capture, preprocessing, and motion detection

## Architecture

```
native_backend/
├── include/                 # C++ header files
│   ├── shimmer_processor.hpp
│   └── webcam_processor.hpp
├── src/                     # C++ implementation files
│   ├── shimmer_processor.cpp
│   ├── webcam_processor.cpp
│   ├── native_shimmer.cpp
│   └── native_webcam.cpp
├── python_bindings/         # PyBind11 binding code
│   ├── shimmer_bindings.cpp
│   └── webcam_bindings.cpp
└── CMakeLists.txt          # Build configuration
```

## Performance Benefits

Performance verification tests demonstrate significant improvements:

### Shimmer Processing
- **Single packet processing**: 3-5x speedup over Python
- **Batch processing**: 4-6x speedup for 128Hz data streams
- **Signal filtering**: 2-3x speedup for real-time applications
- **Artifact removal**: 3-4x speedup for data cleaning

### Webcam Processing
- **Frame preprocessing**: 2-4x speedup for 1080p frames
- **Motion detection**: 3-5x speedup for real-time analysis
- **Timestamp overlay**: 2-3x speedup for frame annotation

## Features

### Shimmer Processor
- **Real-time processing**: Handles 128Hz sampling rate with <8ms processing time
- **Multi-sensor support**: GSR, PPG, accelerometer, gyroscope, magnetometer
- **Signal processing**: Low-pass filtering, artifact removal, calibration
- **Performance monitoring**: Built-in timing and throughput metrics

### Webcam Processor
- **High-resolution capture**: Supports up to 1920x1080 at 30fps
- **Real-time preprocessing**: Image enhancement, noise reduction
- **Motion detection**: Frame-to-frame difference analysis
- **Synchronisation**: Master clock alignment for multi-sensor coordination

## Building

### Prerequisites
- Python 3.10+
- CMake 3.15+
- C++17 compatible compiler
- OpenCV 4.x
- PyBind11

### Build Instructions

1. **Install dependencies**:
   ```bash
   pip install pybind11 opencv-python cmake
   ```

2. **Build using the provided script**:
   ```bash
   ./build_native.sh
   ```

3. **Manual build with CMake**:
   ```bash
   mkdir build && cd build
   cmake ../native_backend -DCMAKE_BUILD_TYPE=Release
   cmake --build . --config Release
   ```

4. **Manual build with setuptools**:
   ```bash
   python setup_native.py build_ext --inplace
   ```

## Usage

The native backend is automatically integrated through Python wrapper modules:

```python
from PythonApp.native_backends.native_shimmer_wrapper import ShimmerProcessor
from PythonApp.native_backends.native_webcam_wrapper import WebcamProcessor

# Shimmer processing with automatic native/Python fallback
shimmer = ShimmerProcessor()  # Uses native if available
shimmer_python = ShimmerProcessor(force_python=True)  # Forces Python

# Webcam processing
webcam = WebcamProcessor()
print(f"Backend: {webcam.backend_type}")  # "native" or "python"

# Configuration
from PythonApp.native_backends.native_shimmer_wrapper import ProcessingConfig
config = ProcessingConfig(
    sampling_rate=128.0,
    enable_filtering=True,
    filter_cutoff=5.0
)
shimmer.configure(config)

# Process data
test_packet = [0x00, 0x08] + [0] * 19  # Example GSR packet
result = shimmer.process_raw_packet(test_packet)
print(f"GSR: {result.gsr_value}μS")
```

## Fallback Mechanism

The system automatically falls back to Python implementations when:
- Native modules are not compiled/available
- Import errors occur during module loading
- Explicit Python mode is requested (`force_python=True`)

This ensures the system always works regardless of native module availability.

## Testing

### Performance Verification
```bash
python -m pytest tests/test_performance_verification.py -v
```

### Integration Testing
```bash
python -m pytest tests/test_native_backend_integration.py -v
```

### Manual Performance Testing
```bash
cd tests
python test_performance_verification.py
```

## Optimisation Flags

The build system applies aggressive optimisation:
- **Compiler flags**: `-O3 -march=native -ffast-math`
- **OpenMP support**: Parallel processing where applicable
- **SIMD instructions**: Automatic vectorization
- **Link-time optimisation**: When supported by compiler

## Platform Support

- **Linux**: Full support with GCC/Clang
- **macOS**: Full support with Clang
- **Windows**: Support with MSVC (Visual Studio 2019+)

## Troubleshooting

### Build Issues

1. **CMake not found**:
   ```bash
   pip install cmake
   ```

2. **OpenCV not found**:
   ```bash
   pip install opencv-python
   ```

3. **Compiler errors**:
   - Ensure C++17 support
   - Check OpenCV installation
   - Verify Python development headers

### Runtime Issues

1. **Import errors**: Check that modules were built and are in the right location
2. **Performance issues**: Verify native modules are being used (check `backend_type`)
3. **Crashes**: Run with Python fallback to isolate native module issues

## Contributing

When modifying the native backend:

1. **Update both C++ and Python implementations** to maintain compatibility
2. **Add performance tests** for new functionality
3. **Update integration tests** to verify fallback behaviour
4. **Document performance characteristics** in the validation tests

## Performance Verification

The implementation includes comprehensive performance tests that validate:
- **Sampling rate compliance**: 128Hz Shimmer processing with <8ms latency
- **Frame rate compliance**: 30fps webcam processing with <33ms latency
- **Synchronisation precision**: Millisecond-level timestamp accuracy
- **Speedup verification**: Native vs Python performance comparison

Run the full validation suite:
```bash
python tests/test_performance_verification.py
```

This generates a detailed report comparing native and Python implementations against thesis performance claims.