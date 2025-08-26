package com.topdon.thermal.view.compass

import com.topdon.thermal.view.compass.stubs.ICompass
import com.topdon.thermal.view.compass.stubs.Bearing

class NullCompass : NullSensor(), ICompass {
    override val bearing: Bearing = Bearing(0f)

    override var declination: Float = 0f

    override val rawBearing: Float = 0f
}
