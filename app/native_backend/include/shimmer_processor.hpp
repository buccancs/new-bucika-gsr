#pragma once

#include <vector>
#include <string>
#include <chrono>
#include <memory>

namespace bucika::shimmer {

/**
 * High-performance native Shimmer data processor
 * Optimized for real-time GSR signal processing
 */
class ShimmerProcessor {
public:
    struct SensorReading {
        double timestamp;
        double gsr_value;      // μS
        double ppg_value;      // Raw PPG
        double accel_x, accel_y, accel_z;  // m/s²
        double gyro_x, gyro_y, gyro_z;     // deg/s
        double mag_x, mag_y, mag_z;        // μT
        double battery_level;  // %
    };

    struct ProcessingConfig {
        double sampling_rate = 128.0;  // Hz
        bool enable_filtering = true;
        double filter_cutoff = 5.0;    // Hz
        bool enable_artifact_removal = true;
        double artifact_threshold = 100.0;  // μS
    };

    ShimmerProcessor();
    ~ShimmerProcessor();

    // Configuration
    void configure(const ProcessingConfig& config);
    
    // Real-time processing
    SensorReading process_raw_packet(const std::vector<uint8_t>& raw_data);
    std::vector<SensorReading> process_batch(const std::vector<std::vector<uint8_t>>& raw_packets);
    
    // Signal processing
    std::vector<double> apply_low_pass_filter(const std::vector<double>& signal, double cutoff_freq);
    std::vector<double> remove_artifacts(const std::vector<double>& gsr_signal);
    
    // Performance metrics
    double get_average_processing_time_ms() const;
    size_t get_packets_processed() const;
    void reset_performance_counters();

private:
    ProcessingConfig config_;
    std::vector<double> filter_coefficients_;
    std::vector<double> filter_state_;
    
    // Performance tracking
    mutable std::chrono::high_resolution_clock::time_point last_processing_start_;
    mutable double total_processing_time_ms_;
    mutable size_t packets_processed_;
    
    void initialize_filter();
    double convert_gsr_raw_to_microsiemens(uint16_t raw_value);
    void update_performance_metrics(std::chrono::high_resolution_clock::time_point start_time) const;
};

} // namespace bucika::shimmer