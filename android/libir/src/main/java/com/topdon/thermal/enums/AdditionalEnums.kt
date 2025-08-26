package com.topdon.thermal.enums

/**
 * Additional thermal analysis enums and data types
 */

/**
 * Number types for thermal measurements
 */
enum class NumberType {
    INTEGER,
    FLOAT,
    DOUBLE,
    TEMPERATURE,
    PERCENTAGE,
    PIXEL_VALUE
}

/**
 * Fence types for thermal region detection
 */
enum class FenceType {
    RECTANGLE,
    CIRCLE,
    POLYGON,
    LINE,
    POINT,
    ELLIPSE
}

/**
 * Zoom scale steps for thermal viewing
 */
enum class ZoomScaleStep {
    STEP_0_5X,
    STEP_1X,
    STEP_2X,
    STEP_4X,
    STEP_8X,
    STEP_16X
}

/**
 * RMCover auto calculation types
 */
enum class RMCoverAutoCalcType {
    DISABLED,
    AUTO_TEMPERATURE,
    AUTO_HUMIDITY,
    AUTO_DISTANCE,
    AUTO_EMISSIVITY,
    FULL_AUTO
}

/**
 * Status switch types for thermal settings
 */
enum class StatusSwith {
    ON,
    OFF,
    AUTO,
    MANUAL
}

/**
 * Product type definitions for thermal devices
 */
enum class ProductType {
    IR_CAMERA,
    THERMAL_CAMERA,
    DUAL_SPECTRUM,
    HANDHELD,
    FIXED_MOUNT,
    SMARTPHONE_ATTACHMENT
}