cmake_minimum_required(VERSION 3.15)
project(bucika_native)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(pybind11 REQUIRED)
find_package(OpenCV REQUIRED)

# Include directories
include_directories(include)
include_directories(${OpenCV_INCLUDE_DIRS})

# Native Shimmer module
pybind11_add_module(native_shimmer
    src/native_shimmer.cpp
    src/shimmer_processor.cpp
    python_bindings/shimmer_bindings.cpp
)

target_link_libraries(native_shimmer PRIVATE ${OpenCV_LIBS})
target_compile_definitions(native_shimmer PRIVATE VERSION_INFO=${EXAMPLE_VERSION_INFO})

# Native Webcam module
pybind11_add_module(native_webcam
    src/native_webcam.cpp 
    src/webcam_processor.cpp
    python_bindings/webcam_bindings.cpp
)

target_link_libraries(native_webcam PRIVATE ${OpenCV_LIBS})
target_compile_definitions(native_webcam PRIVATE VERSION_INFO=${EXAMPLE_VERSION_INFO})

# Performance optimization flags
if(CMAKE_CXX_COMPILER_ID STREQUAL "GNU" OR CMAKE_CXX_COMPILER_ID STREQUAL "Clang")
    target_compile_options(native_shimmer PRIVATE -O3 -march=native -ffast-math)
    target_compile_options(native_webcam PRIVATE -O3 -march=native -ffast-math)
endif()

# Enable OpenMP for parallel processing
find_package(OpenMP)
if(OpenMP_CXX_FOUND)
    target_link_libraries(native_shimmer PRIVATE OpenMP::OpenMP_CXX)
    target_link_libraries(native_webcam PRIVATE OpenMP::OpenMP_CXX)
endif()