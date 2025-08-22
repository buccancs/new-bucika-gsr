package com.shimmerresearch.driver

/**
 * Local implementation of Shimmer Configuration API
 * Matches the structure of the official Shimmer Android SDK
 */
object Configuration {
    const val CALIBRATED = "CAL"
    const val UNCALIBRATED = "RAW"
    
    object Shimmer3 {
        object ObjectClusterSensorName {
            const val GSR_CONDUCTANCE = "GSR_CONDUCTANCE"
            const val GSR_RESISTANCE = "GSR_RESISTANCE" 
            const val SKIN_TEMPERATURE = "SKIN_TEMPERATURE"
            const val TEMPERATURE = "TEMPERATURE"
        }
        
        object SensorMap {
            object GSR {
                const val mValue = 0x08
            }
            
            object TEMPERATURE {
                const val mValue = 0x10
            }
        }
    }
}