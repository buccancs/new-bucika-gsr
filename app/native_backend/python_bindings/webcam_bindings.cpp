#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>
#include "webcam_processor.hpp"

namespace py = pybind11;
using namespace bucika::webcam;

// Helper function to convert cv::Mat to numpy array
py::array_t<uint8_t> mat_to_numpy(const cv::Mat& mat) {
    return py::array_t<uint8_t>(
        {mat.rows, mat.cols, mat.channels()},
        {sizeof(uint8_t) * mat.cols * mat.channels(), sizeof(uint8_t) * mat.channels(), sizeof(uint8_t)},
        mat.data
    );
}

// Helper function to convert numpy array to cv::Mat
cv::Mat numpy_to_mat(py::array_t<uint8_t> input) {
    py::buffer_info buf_info = input.request();
    
    int rows = buf_info.shape[0];
    int cols = buf_info.shape[1];
    int channels = (buf_info.ndim == 3) ? buf_info.shape[2] : 1;
    
    cv::Mat mat(rows, cols, CV_8UC(channels), (unsigned char*)buf_info.ptr);
    return mat.clone();
}

PYBIND11_MODULE(native_webcam, m) {
    m.doc() = "High-performance native webcam processor";
    
    // FrameData structure
    py::class_<WebcamProcessor::FrameData>(m, "FrameData")
        .def(py::init<>())
        .def_property("frame", 
                     [](const WebcamProcessor::FrameData& f) { return mat_to_numpy(f.frame); },
                     [](WebcamProcessor::FrameData& f, py::array_t<uint8_t> arr) { f.frame = numpy_to_mat(arr); })
        .def_readwrite("timestamp", &WebcamProcessor::FrameData::timestamp)
        .def_readwrite("frame_number", &WebcamProcessor::FrameData::frame_number)
        .def_readwrite("is_valid", &WebcamProcessor::FrameData::is_valid)
        .def("__repr__", [](const WebcamProcessor::FrameData& f) {
            return "<FrameData #" + std::to_string(f.frame_number) + 
                   " timestamp=" + std::to_string(f.timestamp) + 
                   " valid=" + (f.is_valid ? "true" : "false") + ">";
        });
    
    // ProcessingConfig structure
    py::class_<WebcamProcessor::ProcessingConfig>(m, "ProcessingConfig")
        .def(py::init<>())
        .def_readwrite("width", &WebcamProcessor::ProcessingConfig::width)
        .def_readwrite("height", &WebcamProcessor::ProcessingConfig::height)
        .def_readwrite("fps", &WebcamProcessor::ProcessingConfig::fps)
        .def_readwrite("enable_preprocessing", &WebcamProcessor::ProcessingConfig::enable_preprocessing)
        .def_readwrite("enable_motion_detection", &WebcamProcessor::ProcessingConfig::enable_motion_detection)
        .def_readwrite("motion_threshold", &WebcamProcessor::ProcessingConfig::motion_threshold);
    
    // PerformanceMetrics structure
    py::class_<WebcamProcessor::PerformanceMetrics>(m, "PerformanceMetrics")
        .def(py::init<>())
        .def_readwrite("average_frame_time_ms", &WebcamProcessor::PerformanceMetrics::average_frame_time_ms)
        .def_readwrite("processing_fps", &WebcamProcessor::PerformanceMetrics::processing_fps)
        .def_readwrite("frames_processed", &WebcamProcessor::PerformanceMetrics::frames_processed)
        .def_readwrite("frames_dropped", &WebcamProcessor::PerformanceMetrics::frames_dropped)
        .def_readwrite("cpu_usage_percent", &WebcamProcessor::PerformanceMetrics::cpu_usage_percent)
        .def("__repr__", [](const WebcamProcessor::PerformanceMetrics& m) {
            return "<PerformanceMetrics fps=" + std::to_string(m.processing_fps) + 
                   " processed=" + std::to_string(m.frames_processed) + 
                   " dropped=" + std::to_string(m.frames_dropped) + ">";
        });
    
    // Main WebcamProcessor class
    py::class_<WebcamProcessor>(m, "WebcamProcessor")
        .def(py::init<>())
        .def("initialize_camera", 
             py::overload_cast<int>(&WebcamProcessor::initialize_camera),
             "Initialize camera by ID",
             py::arg("camera_id") = 0)
        .def("initialize_camera",
             py::overload_cast<const std::string&>(&WebcamProcessor::initialize_camera),
             "Initialize camera by path",
             py::arg("camera_path"))
        .def("release_camera", &WebcamProcessor::release_camera)
        .def("is_camera_active", &WebcamProcessor::is_camera_active)
        .def("configure", &WebcamProcessor::configure)
        .def("get_config", &WebcamProcessor::get_config)
        .def("capture_frame", &WebcamProcessor::capture_frame)
        .def("capture_batch", &WebcamProcessor::capture_batch,
             "Capture multiple frames",
             py::arg("count"))
        .def("preprocess_frame", 
             [](WebcamProcessor& self, py::array_t<uint8_t> input) {
                 cv::Mat input_mat = numpy_to_mat(input);
                 cv::Mat result = self.preprocess_frame(input_mat);
                 return mat_to_numpy(result);
             },
             "Preprocess a frame",
             py::arg("frame"))
        .def("detect_motion",
             [](WebcamProcessor& self, py::array_t<uint8_t> current, py::array_t<uint8_t> previous) {
                 cv::Mat current_mat = numpy_to_mat(current);
                 cv::Mat previous_mat = numpy_to_mat(previous);
                 return self.detect_motion(current_mat, previous_mat);
             },
             "Detect motion between frames",
             py::arg("current_frame"), py::arg("previous_frame"))
        .def("apply_timestamp_overlay",
             [](WebcamProcessor& self, py::array_t<uint8_t> frame, double timestamp) {
                 cv::Mat frame_mat = numpy_to_mat(frame);
                 cv::Mat result = self.apply_timestamp_overlay(frame_mat, timestamp);
                 return mat_to_numpy(result);
             },
             "Apply timestamp overlay to frame",
             py::arg("frame"), py::arg("timestamp"))
        .def("set_master_clock_offset", &WebcamProcessor::set_master_clock_offset)
        .def("get_synchronized_timestamp", &WebcamProcessor::get_synchronized_timestamp)
        .def("get_performance_metrics", &WebcamProcessor::get_performance_metrics)
        .def("reset_performance_counters", &WebcamProcessor::reset_performance_counters);
}