#include "webcam_processor.hpp"
#include <chrono>
#include <iostream>

namespace bucika::webcam {

WebcamProcessor::WebcamProcessor() 
    : master_clock_offset_ms_(0.0), total_processing_time_ms_(0.0), 
      frames_processed_(0), frames_dropped_(0) {
}

WebcamProcessor::~WebcamProcessor() {
    release_camera();
}

bool WebcamProcessor::initialize_camera(int camera_id) {
    release_camera();
    
    camera_.open(camera_id);
    if (!camera_.isOpened()) {
        return false;
    }
    
    // Set camera properties
    camera_.set(cv::CAP_PROP_FRAME_WIDTH, config_.width);
    camera_.set(cv::CAP_PROP_FRAME_HEIGHT, config_.height);
    camera_.set(cv::CAP_PROP_FPS, config_.fps);
    
    // Verify settings
    int actual_width = static_cast<int>(camera_.get(cv::CAP_PROP_FRAME_WIDTH));
    int actual_height = static_cast<int>(camera_.get(cv::CAP_PROP_FRAME_HEIGHT));
    double actual_fps = camera_.get(cv::CAP_PROP_FPS);
    
    std::cout << "Camera initialized: " << actual_width << "x" << actual_height 
              << " @ " << actual_fps << " fps" << std::endl;
    
    initialize_motion_detection();
    return true;
}

bool WebcamProcessor::initialize_camera(const std::string& camera_path) {
    release_camera();
    
    camera_.open(camera_path);
    if (!camera_.isOpened()) {
        return false;
    }
    
    camera_.set(cv::CAP_PROP_FRAME_WIDTH, config_.width);
    camera_.set(cv::CAP_PROP_FRAME_HEIGHT, config_.height);
    camera_.set(cv::CAP_PROP_FPS, config_.fps);
    
    initialize_motion_detection();
    return true;
}

void WebcamProcessor::release_camera() {
    if (camera_.isOpened()) {
        camera_.release();
    }
}

bool WebcamProcessor::is_camera_active() const {
    return camera_.isOpened();
}

void WebcamProcessor::configure(const ProcessingConfig& config) {
    config_ = config;
    
    if (camera_.isOpened()) {
        camera_.set(cv::CAP_PROP_FRAME_WIDTH, config_.width);
        camera_.set(cv::CAP_PROP_FRAME_HEIGHT, config_.height);
        camera_.set(cv::CAP_PROP_FPS, config_.fps);
    }
}

WebcamProcessor::ProcessingConfig WebcamProcessor::get_config() const {
    return config_;
}

WebcamProcessor::FrameData WebcamProcessor::capture_frame() {
    auto start_time = std::chrono::high_resolution_clock::now();
    
    FrameData frame_data{};
    frame_data.timestamp = get_synchronized_timestamp();
    frame_data.frame_number = static_cast<int>(frames_processed_);
    frame_data.is_valid = false;
    
    if (!camera_.isOpened()) {
        frames_dropped_++;
        update_performance_metrics(start_time);
        return frame_data;
    }
    
    cv::Mat raw_frame;
    bool success = camera_.read(raw_frame);
    
    if (!success || raw_frame.empty()) {
        frames_dropped_++;
        update_performance_metrics(start_time);
        return frame_data;
    }
    
    // Apply preprocessing if enabled
    if (config_.enable_preprocessing) {
        frame_data.frame = preprocess_frame(raw_frame);
    } else {
        frame_data.frame = raw_frame.clone();
    }
    
    frame_data.is_valid = true;
    
    // Update motion detection
    if (config_.enable_motion_detection && !previous_frame_.empty()) {
        detect_motion(frame_data.frame, previous_frame_);
    }
    
    // Store for next motion detection
    if (config_.enable_motion_detection) {
        cv::cvtColor(frame_data.frame, previous_frame_, cv::COLOR_BGR2GRAY);
    }
    
    update_performance_metrics(start_time);
    return frame_data;
}

std::vector<WebcamProcessor::FrameData> WebcamProcessor::capture_batch(size_t count) {
    std::vector<FrameData> frames;
    frames.reserve(count);
    
    for (size_t i = 0; i < count; ++i) {
        auto frame = capture_frame();
        frames.push_back(std::move(frame));
        
        if (!frame.is_valid) {
            break;  // Stop on first failed capture
        }
    }
    
    return frames;
}

cv::Mat WebcamProcessor::preprocess_frame(const cv::Mat& input) {
    cv::Mat processed = input.clone();
    
    // Apply color space conversion if needed
    if (config_.color_space != cv::COLOR_BGR2BGR) {
        cv::cvtColor(input, processed, config_.color_space);
    }
    
    // Apply basic image enhancements
    // Histogram equalization for better contrast
    if (processed.channels() == 1) {
        cv::equalizeHist(processed, processed);
    } else if (processed.channels() == 3) {
        std::vector<cv::Mat> channels;
        cv::split(processed, channels);
        for (auto& channel : channels) {
            cv::equalizeHist(channel, channel);
        }
        cv::merge(channels, processed);
    }
    
    // Gaussian blur for noise reduction (very light)
    cv::GaussianBlur(processed, processed, cv::Size(3, 3), 0.5);
    
    return processed;
}

bool WebcamProcessor::detect_motion(const cv::Mat& current_frame, const cv::Mat& previous_frame) {
    if (current_frame.empty() || previous_frame.empty()) {
        return false;
    }
    
    cv::Mat current_gray, prev_gray;
    
    // Convert to grayscale if needed
    if (current_frame.channels() == 3) {
        cv::cvtColor(current_frame, current_gray, cv::COLOR_BGR2GRAY);
    } else {
        current_gray = current_frame;
    }
    
    if (previous_frame.channels() == 3) {
        cv::cvtColor(previous_frame, prev_gray, cv::COLOR_BGR2GRAY);
    } else {
        prev_gray = previous_frame;
    }
    
    // Compute absolute difference
    cv::absdiff(current_gray, prev_gray, motion_diff_);
    
    // Apply threshold
    cv::threshold(motion_diff_, motion_diff_, config_.motion_threshold, 255, cv::THRESH_BINARY);
    
    // Calculate percentage of changed pixels
    int non_zero = cv::countNonZero(motion_diff_);
    double motion_percentage = (non_zero * 100.0) / (motion_diff_.rows * motion_diff_.cols);
    
    return motion_percentage > 1.0;  // Consider motion if >1% of pixels changed
}

cv::Mat WebcamProcessor::apply_timestamp_overlay(const cv::Mat& frame, double timestamp) {
    cv::Mat output = frame.clone();
    
    // Format timestamp as string
    std::string timestamp_str = std::to_string(static_cast<long long>(timestamp));
    
    // Add timestamp overlay
    cv::Point text_position(10, 30);
    cv::Scalar text_color(0, 255, 0);  // Green
    int font_face = cv::FONT_HERSHEY_SIMPLEX;
    double font_scale = 0.7;
    int thickness = 2;
    
    cv::putText(output, timestamp_str, text_position, font_face, font_scale, text_color, thickness);
    
    return output;
}

void WebcamProcessor::set_master_clock_offset(double offset_ms) {
    master_clock_offset_ms_ = offset_ms;
}

double WebcamProcessor::get_synchronized_timestamp() const {
    return get_high_precision_timestamp() + master_clock_offset_ms_;
}

void WebcamProcessor::initialize_motion_detection() {
    if (config_.enable_motion_detection) {
        previous_frame_ = cv::Mat();
        motion_diff_ = cv::Mat();
    }
}

double WebcamProcessor::get_high_precision_timestamp() const {
    auto now = std::chrono::high_resolution_clock::now();
    auto epoch = now.time_since_epoch();
    auto microseconds = std::chrono::duration_cast<std::chrono::microseconds>(epoch);
    return microseconds.count() / 1000.0;  // Return milliseconds with μs precision
}

void WebcamProcessor::update_performance_metrics(std::chrono::high_resolution_clock::time_point start_time) const {
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end_time - start_time);
    
    total_processing_time_ms_ += duration.count() / 1000.0;
    frames_processed_++;
}

WebcamProcessor::PerformanceMetrics WebcamProcessor::get_performance_metrics() const {
    PerformanceMetrics metrics{};
    
    if (frames_processed_ > 0) {
        metrics.average_frame_time_ms = total_processing_time_ms_ / frames_processed_;
        metrics.processing_fps = 1000.0 / metrics.average_frame_time_ms;
    }
    
    metrics.frames_processed = frames_processed_;
    metrics.frames_dropped = frames_dropped_;
    metrics.cpu_usage_percent = 0.0;  // Would need system-specific implementation
    
    return metrics;
}

void WebcamProcessor::reset_performance_counters() {
    total_processing_time_ms_ = 0.0;
    frames_processed_ = 0;
    frames_dropped_ = 0;
}

} // namespace bucika::webcam