#include "shimmer_processor.hpp"
#include <algorithm>
#include <cmath>
#include <numeric>

namespace bucika::shimmer {

ShimmerProcessor::ShimmerProcessor() 
    : total_processing_time_ms_(0.0), packets_processed_(0) {
    initialize_filter();
}

ShimmerProcessor::~ShimmerProcessor() = default;

void ShimmerProcessor::configure(const ProcessingConfig& config) {
    config_ = config;
    initialize_filter();
}

void ShimmerProcessor::initialize_filter() {
    // Initialize Butterworth low-pass filter coefficients
    // 4th order Butterworth filter at cutoff frequency
    const double nyquist = config_.sampling_rate / 2.0;
    const double normalized_cutoff = config_.filter_cutoff / nyquist;
    
    // Simplified coefficient calculation for demonstration
    // In production, use proper filter design algorithms
    const double alpha = std::exp(-2.0 * M_PI * normalized_cutoff);
    filter_coefficients_ = {1.0 - alpha, alpha};
    filter_state_.resize(4, 0.0);  // 4th order filter state
}

ShimmerProcessor::SensorReading ShimmerProcessor::process_raw_packet(const std::vector<uint8_t>& raw_data) {
    auto start_time = std::chrono::high_resolution_clock::now();
    
    SensorReading reading{};
    
    if (raw_data.size() < 20) {
        reading.timestamp = get_high_precision_timestamp();
        update_performance_metrics(start_time);
        return reading;
    }
    
    // Parse raw Shimmer packet (simplified format)
    // Real implementation would follow Shimmer3 protocol specification
    uint16_t gsr_raw = (raw_data[1] << 8) | raw_data[0];
    uint16_t ppg_raw = (raw_data[3] << 8) | raw_data[2];
    
    // Accelerometer (3 channels, 2 bytes each)
    int16_t accel_x_raw = (raw_data[5] << 8) | raw_data[4];
    int16_t accel_y_raw = (raw_data[7] << 8) | raw_data[6];
    int16_t accel_z_raw = (raw_data[9] << 8) | raw_data[8];
    
    // Gyroscope (3 channels, 2 bytes each)
    int16_t gyro_x_raw = (raw_data[11] << 8) | raw_data[10];
    int16_t gyro_y_raw = (raw_data[13] << 8) | raw_data[12];
    int16_t gyro_z_raw = (raw_data[15] << 8) | raw_data[14];
    
    // Magnetometer (3 channels, 2 bytes each)
    int16_t mag_x_raw = (raw_data[17] << 8) | raw_data[16];
    int16_t mag_y_raw = (raw_data[19] << 8) | raw_data[18];
    
    // Convert to engineering units
    reading.timestamp = get_high_precision_timestamp();
    reading.gsr_value = convert_gsr_raw_to_microsiemens(gsr_raw);
    reading.ppg_value = ppg_raw * 0.001;  // Convert to voltage
    
    // Accelerometer: ±2g range, 16-bit resolution
    const double accel_scale = 4.0 / 65536.0 * 9.81;  // m/s²
    reading.accel_x = accel_x_raw * accel_scale;
    reading.accel_y = accel_y_raw * accel_scale;
    reading.accel_z = accel_z_raw * accel_scale;
    
    // Gyroscope: ±500°/s range, 16-bit resolution
    const double gyro_scale = 1000.0 / 65536.0;  // deg/s
    reading.gyro_x = gyro_x_raw * gyro_scale;
    reading.gyro_y = gyro_y_raw * gyro_scale;
    reading.gyro_z = gyro_z_raw * gyro_scale;
    
    // Magnetometer: ±4000μT range, 16-bit resolution
    const double mag_scale = 8000.0 / 65536.0;  // μT
    reading.mag_x = mag_x_raw * mag_scale;
    reading.mag_y = mag_y_raw * mag_scale;
    reading.mag_z = mag_z_raw * mag_scale;
    
    // Battery level (last byte)
    reading.battery_level = (raw_data.size() > 20) ? raw_data[20] : 100.0;
    
    // Apply filtering if enabled
    if (config_.enable_filtering) {
        // Apply low-pass filter to GSR (simplified implementation)
        filter_state_[0] = reading.gsr_value * filter_coefficients_[0] + 
                          filter_state_[1] * filter_coefficients_[1];
        reading.gsr_value = filter_state_[0];
        std::rotate(filter_state_.begin(), filter_state_.begin() + 1, filter_state_.end());
    }
    
    update_performance_metrics(start_time);
    return reading;
}

std::vector<ShimmerProcessor::SensorReading> ShimmerProcessor::process_batch(
    const std::vector<std::vector<uint8_t>>& raw_packets) {
    
    std::vector<SensorReading> readings;
    readings.reserve(raw_packets.size());
    
    for (const auto& packet : raw_packets) {
        readings.push_back(process_raw_packet(packet));
    }
    
    return readings;
}

std::vector<double> ShimmerProcessor::apply_low_pass_filter(
    const std::vector<double>& signal, double cutoff_freq) {
    
    std::vector<double> filtered = signal;
    
    if (signal.empty()) return filtered;
    
    // Simple IIR low-pass filter implementation
    const double dt = 1.0 / config_.sampling_rate;
    const double alpha = dt / (dt + 1.0 / (2.0 * M_PI * cutoff_freq));
    
    for (size_t i = 1; i < filtered.size(); ++i) {
        filtered[i] = alpha * signal[i] + (1.0 - alpha) * filtered[i-1];
    }
    
    return filtered;
}

std::vector<double> ShimmerProcessor::remove_artifacts(const std::vector<double>& gsr_signal) {
    std::vector<double> clean_signal = gsr_signal;
    
    if (!config_.enable_artifact_removal) return clean_signal;
    
    // Remove sudden spikes that exceed threshold
    for (size_t i = 1; i < clean_signal.size() - 1; ++i) {
        double diff_before = std::abs(clean_signal[i] - clean_signal[i-1]);
        double diff_after = std::abs(clean_signal[i+1] - clean_signal[i]);
        
        if (diff_before > config_.artifact_threshold || diff_after > config_.artifact_threshold) {
            // Replace with interpolated value
            clean_signal[i] = (clean_signal[i-1] + clean_signal[i+1]) / 2.0;
        }
    }
    
    return clean_signal;
}

double ShimmerProcessor::convert_gsr_raw_to_microsiemens(uint16_t raw_value) {
    // Shimmer3 GSR+ conversion formula
    // Based on hardware specifications and calibration
    const double voltage_range = 3.0;  // 3V reference
    const double adc_resolution = 4096.0;  // 12-bit ADC
    
    double voltage = (raw_value / adc_resolution) * voltage_range;
    
    // Convert voltage to conductance (simplified model)
    // Real implementation would use proper calibration curves
    double resistance = 40000.0 / voltage;  // Ohms
    double conductance = 1000000.0 / resistance;  // μS
    
    return std::max(0.0, std::min(100.0, conductance));  // Clamp to valid range
}

double ShimmerProcessor::get_high_precision_timestamp() const {
    auto now = std::chrono::high_resolution_clock::now();
    auto epoch = now.time_since_epoch();
    auto microseconds = std::chrono::duration_cast<std::chrono::microseconds>(epoch);
    return microseconds.count() / 1000.0;  // Return milliseconds with μs precision
}

void ShimmerProcessor::update_performance_metrics(std::chrono::high_resolution_clock::time_point start_time) const {
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end_time - start_time);
    
    total_processing_time_ms_ += duration.count() / 1000.0;
    packets_processed_++;
}

double ShimmerProcessor::get_average_processing_time_ms() const {
    return packets_processed_ > 0 ? total_processing_time_ms_ / packets_processed_ : 0.0;
}

size_t ShimmerProcessor::get_packets_processed() const {
    return packets_processed_;
}

void ShimmerProcessor::reset_performance_counters() {
    total_processing_time_ms_ = 0.0;
    packets_processed_ = 0;
}

} // namespace bucika::shimmer