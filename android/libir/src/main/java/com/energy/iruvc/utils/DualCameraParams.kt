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
    enum class TypeLoadParameters {
        ROTATE_0,
        ROTATE_90,
        ROTATE_180,
        ROTATE_270,
        FLIP_HORIZONTAL,
        FLIP_VERTICAL,
        FLIP_BOTH,
        NO_TRANSFORM;
        
        companion object {
            fun fromInt(value: Int): TypeLoadParameters {
                return when (value) {
                    0 -> ROTATE_0
                    1 -> ROTATE_90
                    2 -> ROTATE_180
                    3 -> ROTATE_270
                    4 -> FLIP_HORIZONTAL
                    5 -> FLIP_VERTICAL
                    6 -> FLIP_BOTH
                    else -> NO_TRANSFORM
                }
            }
        }
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