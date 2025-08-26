package com.topdon.thermal.view.compass

import com.topdon.thermal.view.compass.stubs.AbstractSensor
import com.topdon.thermal.view.compass.stubs.CoroutineTimer

abstract class NullSensor(private val interval: Long = 0): AbstractSensor() {
    override val hasValidReading: Boolean = true

    private val timer = CoroutineTimer(interval) {
        notifyListeners()
    }

    override fun startImpl(): Boolean {
        if (interval == 0L){
            timer.start()
        } else {
            timer.start()
        }
        return true
    }

    override fun stopImpl() {
        timer.stop()
    }
}
