"""
Setup script for building PyBind11 native extensions
"""

import os
import sys
import subprocess
from pathlib import Path
from pybind11.setup_helpers import Pybind11Extension, build_ext
from pybind11 import get_cmake_dir
import pybind11
from setuptools import setup, Extension

# The main interface is through Pybind11Extension.
# * You can add cxx_std=14/17/20, and then build_ext can be removed.
# * You can set include_pybind11=false to add the include directory yourself,
#   say from a submodule.
#
# Note:
#   Sort input source files if you glob sources to ensure bit-for-bit
#   reproducible builds (https://github.com/pybind/python_example/pull/53)

def get_opencv_flags():
    """Get OpenCV compilation flags"""
    try:
        import cv2
        opencv_path = cv2.__file__
        opencv_dir = Path(opencv_path).parent
        
        # Try to find OpenCV include directory
        include_dirs = []
        lib_dirs = []
        libs = []
        
        # Common OpenCV include paths
        possible_includes = [
            opencv_dir / "include",
            "/usr/include/opencv4",
            "/usr/local/include/opencv4",
            "/opt/homebrew/include/opencv4",
        ]
        
        for inc_path in possible_includes:
            if inc_path.exists():
                include_dirs.append(str(inc_path))
                break
        
        # Use pkg-config if available
        try:
            result = subprocess.run(['pkg-config', '--cflags', '--libs', 'opencv4'], 
                                  capture_output=True, text=True, check=True)
            flags = result.stdout.strip().split()
            
            for flag in flags:
                if flag.startswith('-I'):
                    include_dirs.append(flag[2:])
                elif flag.startswith('-L'):
                    lib_dirs.append(flag[2:])
                elif flag.startswith('-l'):
                    libs.append(flag[2:])
                    
        except (subprocess.CalledProcessError, FileNotFoundError):
            # Fallback to common library names
            libs = ['opencv_core', 'opencv_imgproc', 'opencv_highgui', 'opencv_imgcodecs']
            
        return include_dirs, lib_dirs, libs
        
    except ImportError:
        print("Warning: OpenCV not found, using minimal configuration")
        return [], [], []


# Get OpenCV configuration
opencv_includes, opencv_lib_dirs, opencv_libs = get_opencv_flags()

# Native backend source files
native_backend_dir = Path("native_backend")
shimmer_sources = [
    str(native_backend_dir / "src" / "shimmer_processor.cpp"),
    str(native_backend_dir / "src" / "native_shimmer.cpp"),
    str(native_backend_dir / "python_bindings" / "shimmer_bindings.cpp"),
]

webcam_sources = [
    str(native_backend_dir / "src" / "webcam_processor.cpp"),
    str(native_backend_dir / "src" / "native_webcam.cpp"),
    str(native_backend_dir / "python_bindings" / "webcam_bindings.cpp"),
]

# Common include directories
include_dirs = [
    str(native_backend_dir / "include"),
    # Path to pybind11 headers
    pybind11.get_include(),
] + opencv_includes

# Compiler flags
extra_compile_args = ["-O3", "-std=c++17"]
extra_link_args = []

# Platform-specific optimizations
if sys.platform.startswith("linux"):
    extra_compile_args.extend(["-march=native", "-ffast-math"])
elif sys.platform == "darwin":  # macOS
    extra_compile_args.extend(["-march=native", "-ffast-math"])
elif sys.platform == "win32":
    extra_compile_args = ["/O2", "/std:c++17"]

# Add OpenMP support if available
try:
    import numpy
    extra_compile_args.append("-fopenmp")
    extra_link_args.append("-fopenmp")
except:
    pass

ext_modules = [
    Pybind11Extension(
        "native_shimmer",
        shimmer_sources,
        include_dirs=include_dirs,
        libraries=opencv_libs,
        library_dirs=opencv_lib_dirs,
        cxx_std=17,
        extra_compile_args=extra_compile_args,
        extra_link_args=extra_link_args,
    ),
    Pybind11Extension(
        "native_webcam",
        webcam_sources,
        include_dirs=include_dirs,
        libraries=opencv_libs,
        library_dirs=opencv_lib_dirs,
        cxx_std=17,
        extra_compile_args=extra_compile_args,
        extra_link_args=extra_link_args,
    ),
]

if __name__ == "__main__":
    setup(
        name="bucika_native_backends",
        ext_modules=ext_modules,
        cmdclass={"build_ext": build_ext},
        zip_safe=False,
        python_requires=">=3.10",
    )