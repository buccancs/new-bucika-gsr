package com.topdon.thermal.view.compass

import android.content.Context
import android.hardware.SensorManager
import com.topdon.thermal.view.compass.stubs.Sensors
import com.topdon.thermal.view.compass.stubs.ICompass

class SensorService(ctx: Context) {

    private var context = ctx.applicationContext

    fun hasCompass(): Boolean {
        return Sensors.hasCompass()
    }

    fun getCompass(): ICompass {
        return Sensors.getCompass()
    }

    companion object {
        const val MOTION_SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME
        const val ENVIRONMENT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }
}
