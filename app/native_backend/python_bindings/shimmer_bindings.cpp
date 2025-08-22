#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>
#include "shimmer_processor.hpp"

namespace py = pybind11;
using namespace bucika::shimmer;

PYBIND11_MODULE(native_shimmer, m) {
    m.doc() = "High-performance native Shimmer data processor";
    
    // SensorReading structure
    py::class_<ShimmerProcessor::SensorReading>(m, "SensorReading")
        .def(py::init<>())
        .def_readwrite("timestamp", &ShimmerProcessor::SensorReading::timestamp)
        .def_readwrite("gsr_value", &ShimmerProcessor::SensorReading::gsr_value)
        .def_readwrite("ppg_value", &ShimmerProcessor::SensorReading::ppg_value)
        .def_readwrite("accel_x", &ShimmerProcessor::SensorReading::accel_x)
        .def_readwrite("accel_y", &ShimmerProcessor::SensorReading::accel_y)
        .def_readwrite("accel_z", &ShimmerProcessor::SensorReading::accel_z)
        .def_readwrite("gyro_x", &ShimmerProcessor::SensorReading::gyro_x)
        .def_readwrite("gyro_y", &ShimmerProcessor::SensorReading::gyro_y)
        .def_readwrite("gyro_z", &ShimmerProcessor::SensorReading::gyro_z)
        .def_readwrite("mag_x", &ShimmerProcessor::SensorReading::mag_x)
        .def_readwrite("mag_y", &ShimmerProcessor::SensorReading::mag_y)
        .def_readwrite("mag_z", &ShimmerProcessor::SensorReading::mag_z)
        .def_readwrite("battery_level", &ShimmerProcessor::SensorReading::battery_level)
        .def("__repr__", [](const ShimmerProcessor::SensorReading& r) {
            return "<SensorReading timestamp=" + std::to_string(r.timestamp) + 
                   " gsr=" + std::to_string(r.gsr_value) + "μS>";
        });
    
    // ProcessingConfig structure
    py::class_<ShimmerProcessor::ProcessingConfig>(m, "ProcessingConfig")
        .def(py::init<>())
        .def_readwrite("sampling_rate", &ShimmerProcessor::ProcessingConfig::sampling_rate)
        .def_readwrite("enable_filtering", &ShimmerProcessor::ProcessingConfig::enable_filtering)
        .def_readwrite("filter_cutoff", &ShimmerProcessor::ProcessingConfig::filter_cutoff)
        .def_readwrite("enable_artifact_removal", &ShimmerProcessor::ProcessingConfig::enable_artifact_removal)
        .def_readwrite("artifact_threshold", &ShimmerProcessor::ProcessingConfig::artifact_threshold);
    
    // Main ShimmerProcessor class
    py::class_<ShimmerProcessor>(m, "ShimmerProcessor")
        .def(py::init<>())
        .def("configure", &ShimmerProcessor::configure)
        .def("process_raw_packet", &ShimmerProcessor::process_raw_packet,
             "Process a single raw data packet",
             py::arg("raw_data"))
        .def("process_batch", &ShimmerProcessor::process_batch,
             "Process multiple raw data packets",
             py::arg("raw_packets"))
        .def("apply_low_pass_filter", &ShimmerProcessor::apply_low_pass_filter,
             "Apply low-pass filter to signal",
             py::arg("signal"), py::arg("cutoff_freq"))
        .def("remove_artifacts", &ShimmerProcessor::remove_artifacts,
             "Remove artifacts from GSR signal",
             py::arg("gsr_signal"))
        .def("get_average_processing_time_ms", &ShimmerProcessor::get_average_processing_time_ms)
        .def("get_packets_processed", &ShimmerProcessor::get_packets_processed)
        .def("reset_performance_counters", &ShimmerProcessor::reset_performance_counters);
}