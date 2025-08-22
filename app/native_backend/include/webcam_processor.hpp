#pragma once

#include <opencv2/opencv.hpp>
#include <vector>
#include <string>
#include <chrono>
#include <memory>

namespace bucika::webcam {

/**
 * High-performance native webcam processor
 * Optimized for real-time video processing and synchronization
 */
class WebcamProcessor {
public:
    struct FrameData {
        cv::Mat frame;
        double timestamp;
        int frame_number;
        bool is_valid;
    };

    struct ProcessingConfig {
        int width = 1920;
        int height = 1080;
        double fps = 30.0;
        bool enable_preprocessing = true;
        bool enable_motion_detection = false;
        double motion_threshold = 30.0;
        cv::ColorConversionCodes color_space = cv::COLOR_BGR2RGB;
    };

    struct PerformanceMetrics {
        double average_frame_time_ms;
        double processing_fps;
        size_t frames_processed;
        size_t frames_dropped;
        double cpu_usage_percent;
    };

    WebcamProcessor();
    ~WebcamProcessor();

    // Camera management
    bool initialize_camera(int camera_id = 0);
    bool initialize_camera(const std::string& camera_path);
    void release_camera();
    bool is_camera_active() const;

    // Configuration
    void configure(const ProcessingConfig& config);
    ProcessingConfig get_config() const;

    // Frame capture and processing
    FrameData capture_frame();
    std::vector<FrameData> capture_batch(size_t count);
    
    // Image processing
    cv::Mat preprocess_frame(const cv::Mat& input);
    bool detect_motion(const cv::Mat& current_frame, const cv::Mat& previous_frame);
    cv::Mat apply_timestamp_overlay(const cv::Mat& frame, double timestamp);
    
    // Synchronization support
    void set_master_clock_offset(double offset_ms);
    double get_synchronized_timestamp() const;
    
    // Performance monitoring
    PerformanceMetrics get_performance_metrics() const;
    void reset_performance_counters();

private:
    cv::VideoCapture camera_;
    ProcessingConfig config_;
    cv::Mat previous_frame_;
    cv::Mat motion_diff_;
    
    // Synchronization
    double master_clock_offset_ms_;
    
    // Performance tracking
    mutable std::chrono::high_resolution_clock::time_point processing_start_;
    mutable double total_processing_time_ms_;
    mutable size_t frames_processed_;
    mutable size_t frames_dropped_;
    
    // Internal methods
    void initialize_motion_detection();
    double get_high_precision_timestamp() const;
    void update_performance_metrics(std::chrono::high_resolution_clock::time_point start_time) const;
};

} // namespace bucika::webcam