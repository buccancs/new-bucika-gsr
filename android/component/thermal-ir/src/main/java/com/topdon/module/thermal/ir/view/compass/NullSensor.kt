package com.topdon.module.thermal.ir.view.compass

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.CoroutineTimer

abstract class NullSensor(private val interval: Long = 0): AbstractSensor() {
    override val hasValidReading: Boolean = true

    private val timer = CoroutineTimer {
        notifyListeners()
    }

    override fun startImpl() {
        if (interval == 0L){
            timer.once(0L)
        } else {
            timer.interval(interval)
        }
    }

    override fun stopImpl() {
        timer.stop()
    }
}