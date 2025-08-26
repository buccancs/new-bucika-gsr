package com.topdon.thermal.enums

/**
 * Target detection types for thermal imaging analysis
 */
enum class TargetType {
    PERSON,
    VEHICLE,
    ANIMAL,
    OBJECT,
    TEMPERATURE_SPOT,
    UNKNOWN
}

/**
 * Setting configuration types for thermal camera controls
 */
enum class SettingType {
    TEMPERATURE_RANGE,
    PSEUDOCOLOR,
    GAIN_MODE,
    PREVIEW_MODE,
    RECORDING_FORMAT,
    CALIBRATION,
    NETWORK_CONFIG,
    SYSTEM_CONFIG
}

/**
 * Two light operation types for dual spectrum imaging
 */
enum class TwoLightType {
    THERMAL_ONLY,
    VISIBLE_ONLY,
    PICTURE_IN_PICTURE,
    FUSION_MODE,
    SIDE_BY_SIDE,
    THERMAL_OVERLAY
}