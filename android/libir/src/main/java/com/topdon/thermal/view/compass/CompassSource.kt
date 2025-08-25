package com.topdon.thermal.view.compass

enum class CompassSource(val id: String) {
    RotationVector("rotation_vector"),
    GeomagneticRotationVector("geomagnetic_rotation_vector"),
    CustomMagnetometer("custom_magnetometer"),
    Orientation("orientation")
