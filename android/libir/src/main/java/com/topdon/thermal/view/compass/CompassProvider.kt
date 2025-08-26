package com.topdon.thermal.view.compass

import android.content.Context
import android.hardware.Sensor
import com.topdon.thermal.view.compass.stubs.Sensors
import com.topdon.thermal.view.compass.stubs.ICompass

class CompassProvider(private val context: Context) {

    fun get(): ICompass {
        // Simplified stub implementation - return basic compass
        return Sensors.getCompass()
    }

    companion object {
        
        fun getAvailableSources(context: Context): List<CompassSource> {
            val sources = mutableListOf<CompassSource>()

            if (Sensors.hasSensor(context, Sensor.TYPE_ROTATION_VECTOR)) {
                sources.add(CompassSource.RotationVector)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)) {
                sources.add(CompassSource.GeomagneticRotationVector)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
                sources.add(CompassSource.CustomMagnetometer)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_ORIENTATION)) {
                sources.add(CompassSource.Orientation)
            }

            return sources
        }
    }
}