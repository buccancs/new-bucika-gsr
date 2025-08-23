package com.shimmerresearch.driver

/**
 * Local implementation of Shimmer Configuration API
 * Matches the structure of the official Shimmer Android SDK
 */
object Configuration {
    const val CALIBRATED = "CAL"
    const val UNCALIBRATED = "RAW"
    
    // Data formats
    const val DATA_FORMAT_RAW = "RAW"
    const val DATA_FORMAT_CALIBRATED = "CAL"
    
    object Shimmer3 {
        // Sampling rates
        const val SAMPLING_RATE_1HZ = 1.0
        const val SAMPLING_RATE_10HZ = 10.24
        const val SAMPLING_RATE_51HZ = 51.2
        const val SAMPLING_RATE_102HZ = 102.4
        const val SAMPLING_RATE_128HZ = 128.0
        const val SAMPLING_RATE_204HZ = 204.8
        const val SAMPLING_RATE_256HZ = 256.0
        const val SAMPLING_RATE_512HZ = 512.0
        const val SAMPLING_RATE_1024HZ = 1024.0
        
        object ObjectClusterSensorName {
            // GSR sensor channels
            const val GSR_CONDUCTANCE = "GSR_CONDUCTANCE"
            const val GSR_RESISTANCE = "GSR_RESISTANCE" 
            const val GSR_RAW = "GSR_RAW"
            
            // Temperature sensor channels
            const val SKIN_TEMPERATURE = "SKIN_TEMPERATURE"
            const val TEMPERATURE = "TEMPERATURE"
            const val TEMPERATURE_RAW = "TEMPERATURE_RAW"
            
            // PPG sensor channels
            const val PPG_RAW = "PPG_RAW"
            const val PPG = "PPG"
            const val HEART_RATE = "HEART_RATE"
            
            // Accelerometer channels
            const val ACCEL_X = "ACCEL_X"
            const val ACCEL_Y = "ACCEL_Y" 
            const val ACCEL_Z = "ACCEL_Z"
            const val ACCEL_X_RAW = "ACCEL_X_RAW"
            const val ACCEL_Y_RAW = "ACCEL_Y_RAW"
            const val ACCEL_Z_RAW = "ACCEL_Z_RAW"
            
            // Gyroscope channels
            const val GYRO_X = "GYRO_X"
            const val GYRO_Y = "GYRO_Y"
            const val GYRO_Z = "GYRO_Z"
            
            // Magnetometer channels
            const val MAG_X = "MAG_X"
            const val MAG_Y = "MAG_Y"
            const val MAG_Z = "MAG_Z"
        }
        
        /**
         * Sensor bitmap values for enabling/disabling sensors
         */
        enum class SensorMap(val mValue: Long) {
            GSR(0x04),
            TEMPERATURE(0x08),
            PPG(0x10),
            ACCEL(0x80),
            GYRO(0x40),
            MAG(0x20),
            BATTERY(0x2000),
            TIMESTAMP(0x1000)
        }
        
        /**
         * GSR range configurations
         */
        object GSRRange {
            const val GSR_RANGE_40K = 0  // 40kΩ (High sensitivity)
            const val GSR_RANGE_287K = 1 // 287kΩ (Medium sensitivity)
            const val GSR_RANGE_1M = 2   // 1MΩ (Low sensitivity)
            const val GSR_RANGE_3M3 = 3  // 3.3MΩ (Very low sensitivity)
        }
        
        /**
         * PPG range configurations
         */
        object PPGRange {
            const val PPG_RANGE_LOW = 0
            const val PPG_RANGE_HIGH = 1
        }
        
        /**
         * Accelerometer range configurations
         */
        object AccelRange {
            const val ACCEL_RANGE_1_5G = 0
            const val ACCEL_RANGE_2G = 1
            const val ACCEL_RANGE_4G = 2
            const val ACCEL_RANGE_6G = 3
        }
        
        /**
         * Digital filter configurations
         */
        object FilterConfig {
            const val FILTER_NONE = 0
            const val FILTER_LOW_PASS = 1
            const val FILTER_HIGH_PASS = 2
            const val FILTER_BAND_PASS = 3
            
            // Gyroscope channels
            const val GYRO_X = "GYRO_X"
            const val GYRO_Y = "GYRO_Y"
            const val GYRO_Z = "GYRO_Z"
            const val GYRO_X_RAW = "GYRO_X_RAW"
            const val GYRO_Y_RAW = "GYRO_Y_RAW"
            const val GYRO_Z_RAW = "GYRO_Z_RAW"
            
            // Magnetometer channels
            const val MAG_X = "MAG_X"
            const val MAG_Y = "MAG_Y"
            const val MAG_Z = "MAG_Z"
            const val MAG_X_RAW = "MAG_X_RAW"
            const val MAG_Y_RAW = "MAG_Y_RAW"
            const val MAG_Z_RAW = "MAG_Z_RAW"
            
            // Timestamp and system channels
            const val TIMESTAMP = "TIMESTAMP"
            const val SYSTEM_TIMESTAMP = "SYSTEM_TIMESTAMP"
            const val PACKET_RECEPTION_RATE = "PACKET_RECEPTION_RATE"
        }
        
        object SensorMap {
            object GSR {
                const val mValue = 0x08
                const val mName = "GSR"
                const val mChannels = arrayOf("GSR_CONDUCTANCE", "GSR_RESISTANCE")
            }
            
            object TEMPERATURE {
                const val mValue = 0x10
                const val mName = "TEMPERATURE"
                const val mChannels = arrayOf("SKIN_TEMPERATURE", "TEMPERATURE")
            }
            
            object PPG {
                const val mValue = 0x20
                const val mName = "PPG"
                const val mChannels = arrayOf("PPG", "HEART_RATE")
            }
            
            object ACCEL {
                const val mValue = 0x80
                const val mName = "ACCEL"
                const val mChannels = arrayOf("ACCEL_X", "ACCEL_Y", "ACCEL_Z")
            }
            
            object GYRO {
                const val mValue = 0x40
                const val mName = "GYRO"
                const val mChannels = arrayOf("GYRO_X", "GYRO_Y", "GYRO_Z")
            }
            
            object MAG {
                const val mValue = 0x01
                const val mName = "MAG"
                const val mChannels = arrayOf("MAG_X", "MAG_Y", "MAG_Z")
            }
            
            object ALL_SENSORS {
                const val mValue = GSR.mValue or TEMPERATURE.mValue or PPG.mValue or ACCEL.mValue or GYRO.mValue or MAG.mValue
            }
        }
        
        object SensorRange {
            // GSR ranges  
            const val GSR_RANGE_40uS_2540uS = 0
            const val GSR_RANGE_10uS_56uS = 1
            const val GSR_RANGE_56uS_220uS = 2
            const val GSR_RANGE_220uS_680uS = 3
            const val GSR_RANGE_680uS_4700uS = 4
            const val GSR_RANGE_AUTO = 5
            
            // Accelerometer ranges
            const val ACCEL_RANGE_1_5G = 0
            const val ACCEL_RANGE_2G = 1
            const val ACCEL_RANGE_4G = 2
            const val ACCEL_RANGE_6G = 3
            
            // Gyroscope ranges  
            const val GYRO_RANGE_250 = 0
            const val GYRO_RANGE_500 = 1
            const val GYRO_RANGE_1000 = 2
            const val GYRO_RANGE_2000 = 3
        }
        
        object SensorUnits {
            const val GSR_CONDUCTANCE_UNIT = "µS"
            const val GSR_RESISTANCE_UNIT = "kΩ" 
            const val TEMPERATURE_UNIT = "°C"
            const val PPG_UNIT = "n/a"
            const val HEART_RATE_UNIT = "BPM"
            const val ACCEL_UNIT = "g"
            const val GYRO_UNIT = "°/s"
            const val MAG_UNIT = "µT"
            const val TIMESTAMP_UNIT = "ms"
        }
        
        object CalibrationParameters {
            // GSR calibration constants
            const val GSR_UNCAL_LIMIT_RANGE_40uS_2540uS = 4095.0
            const val GSR_CAL_OFFSET_RANGE_40uS_2540uS = 0.0
            const val GSR_CAL_GAIN_RANGE_40uS_2540uS = 2.5 / 4095.0
            
            // Temperature calibration constants  
            const val TEMP_UNCAL_LIMIT = 4095.0
            const val TEMP_CAL_OFFSET = -273.15
            const val TEMP_CAL_GAIN = 0.01
        }
    }
}