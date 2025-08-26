package com.energy.iruvc.utils

/**
 * Dual camera parameters for thermal imaging
 */
object DualCameraParams {
    
    /**
     * Fusion types for dual camera thermal imaging
     */
    enum class FusionType {
        VLOnly,              // Visible light only
        IROnly,              // Infrared only  
        IROnlyNoFusion,      // Infrared only without fusion
        MeanFusion,          // Mean fusion algorithm
        HSLFusion,           // HSL color space fusion
        LPYFusion,           // LPY fusion algorithm
        ScreenFusion,        // Screen fusion
        TC007Fusion;         // TC007 fusion algorithm
        
        /**
         * Convert fusion type to boolean for backwards compatibility
         */
        fun toBoolean(): Boolean {
            return when (this) {
                IROnlyNoFusion, VLOnly -> false
                else -> true
            }
        }
        
        companion object {
            /**
             * Create fusion type from integer constant
             */
            fun fromInt(value: Int): FusionType {
                return when (value) {
                    0 -> VLOnly
                    1 -> IROnly
                    2 -> MeanFusion
                    3 -> HSLFusion
                    4 -> LPYFusion
                    5 -> ScreenFusion
                    6 -> IROnlyNoFusion
                    7 -> TC007Fusion
                    else -> LPYFusion // Default
                }
            }
        }
    }
    
    /**
     * Type load parameters for rotation and image processing
     */
    object TypeLoadParameters {
        const val ROTATE_0 = 0
        const val ROTATE_90 = 1
        const val ROTATE_180 = 2
        const val ROTATE_270 = 3
        
        // Image processing types
        const val FLIP_HORIZONTAL = 4
        const val FLIP_VERTICAL = 5
        const val FLIP_BOTH = 6
        const val NO_TRANSFORM = 0
    }
    
    // Camera configuration constants
    const val DEFAULT_IR_WIDTH = 256
    const val DEFAULT_IR_HEIGHT = 192
    const val DEFAULT_VL_WIDTH = 640
    const val DEFAULT_VL_HEIGHT = 480
    const val DUAL_BUFFER_SIZE = 512 * 384 * 4
    
    // Temperature analysis parameters
    const val TEMP_SCALE_FACTOR = 0.04f
    const val TEMP_OFFSET = 273.15f
    const val MIN_TEMPERATURE = -40.0f
    const val MAX_TEMPERATURE = 150.0f
}