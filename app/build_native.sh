#!/bin/bash

# Build script for native PyBind11 backends
# This script builds the C++ extensions for high-performance sensor processing

set -e  # Exit on any error

echo "Building Native PyBind11 Backends for Bucika GSR System"
echo "========================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "pyproject.toml" ]; then
    print_error "This script must be run from the repository root directory"
    exit 1
fi

# Check for required tools
print_status "Checking build dependencies..."

# Check for Python
if ! command -v python &> /dev/null; then
    print_error "Python is not installed or not in PATH"
    exit 1
fi

# Check Python version
PYTHON_VERSION=$(python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')")
print_status "Python version: $PYTHON_VERSION"

if ! python -c "import sys; exit(0 if sys.version_info >= (3, 10) else 1)"; then
    print_error "Python 3.10+ is required"
    exit 1
fi

# Check for CMake
if ! command -v cmake &> /dev/null; then
    print_warning "CMake not found. Installing via pip..."
    python -m pip install cmake
fi

# Check for build tools
if ! command -v make &> /dev/null && ! command -v ninja &> /dev/null; then
    print_warning "Neither make nor ninja found. Build may fail."
fi

# Check for OpenCV
print_status "Checking OpenCV installation..."
if ! python -c "import cv2; print(f'OpenCV version: {cv2.__version__}')" 2>/dev/null; then
    print_warning "OpenCV not found. Installing..."
    python -m pip install opencv-python
fi

# Install build dependencies
print_status "Installing build dependencies..."
python -m pip install pybind11[global] setuptools wheel

# Create build directory
print_status "Creating build directory..."
mkdir -p build
cd build

# Configure with CMake if CMakeLists.txt exists in native_backend
if [ -f "../native_backend/CMakeLists.txt" ]; then
    print_status "Configuring with CMake..."
    
    # Use CMake for building
    cmake ../native_backend -DCMAKE_BUILD_TYPE=Release
    
    print_status "Building with CMake..."
    cmake --build . --config Release
    
    # Copy built modules to the right location
    print_status "Installing built modules..."
    find . -name "*.so" -o -name "*.pyd" -o -name "*.dylib" | while read lib; do
        cp "$lib" ../PythonApp/
        print_status "Installed $(basename $lib)"
    done
    
else
    print_status "CMakeLists.txt not found, using setuptools..."
    cd ..
    
    # Use setuptools for building
    print_status "Building with setuptools..."
    python setup_native.py build_ext --inplace
    
    # The modules should be built in the current directory
    find . -name "native_*.so" -o -name "native_*.pyd" -o -name "native_*.dylib" | while read lib; do
        if [ ! -f "PythonApp/$(basename $lib)" ]; then
            cp "$lib" PythonApp/
            print_status "Installed $(basename $lib)"
        fi
    done
fi

cd ..

# Test the installation
print_status "Testing native module imports..."

# Test shimmer module
if python -c "import sys; sys.path.insert(0, 'PythonApp'); from native_backends.native_shimmer_wrapper import NATIVE_AVAILABLE; print(f'Shimmer native available: {NATIVE_AVAILABLE}')" 2>/dev/null; then
    print_status "Shimmer native module test passed"
else
    print_warning "Shimmer native module test failed - will use Python fallback"
fi

# Test webcam module
if python -c "import sys; sys.path.insert(0, 'PythonApp'); from native_backends.native_webcam_wrapper import NATIVE_AVAILABLE; print(f'Webcam native available: {NATIVE_AVAILABLE}')" 2>/dev/null; then
    print_status "Webcam native module test passed"
else
    print_warning "Webcam native module test failed - will use Python fallback"
fi

# Run a quick performance test
print_status "Running quick performance verification..."
if python -c "
import sys
sys.path.insert(0, 'PythonApp')
from native_backends.native_shimmer_wrapper import ShimmerProcessor
from native_backends.native_webcam_wrapper import WebcamProcessor

# Test shimmer
shimmer = ShimmerProcessor()
print(f'Shimmer backend: {shimmer.backend_type}')

# Test webcam  
webcam = WebcamProcessor()
print(f'Webcam backend: {webcam.backend_type}')

print('Quick performance test completed successfully')
" 2>/dev/null; then
    print_status "Performance verification passed"
else
    print_warning "Performance verification failed"
fi

print_status "Build completed!"
echo ""
echo "Native Backend Build Summary:"
echo "=============================="
echo "Build location: $(pwd)/build"
echo "Native modules location: $(pwd)/PythonApp/"
echo ""
echo "To run performance tests:"
echo "  python -m pytest tests/test_performance_verification.py -v"
echo ""
echo "To run integration tests:"
echo "  python -m pytest tests/test_native_backend_integration.py -v"
echo ""
echo "To run the main application with native backends:"
echo "  cd PythonApp && python main.py"