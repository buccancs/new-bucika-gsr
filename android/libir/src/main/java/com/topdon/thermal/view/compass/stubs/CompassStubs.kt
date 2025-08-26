package com.topdon.thermal.view.compass.stubs

// Stub implementations for missing third-party classes

data class Bearing(val degrees: Float)

interface ICompass {
    val bearing: Bearing
    var declination: Float
    val rawBearing: Float
}

abstract class AbstractSensor {
    abstract val hasValidReading: Boolean
    protected abstract fun startImpl(): Boolean
    protected abstract fun stopImpl()
    protected fun notifyListeners() {
        // Stub implementation
    }
}

class CoroutineTimer(private val intervalMs: Long, private val callback: () -> Unit) {
    fun start() {
        // Stub implementation - would normally start a coroutine timer
    }
    
    fun stop() {
        // Stub implementation
    }
}

object Sensors {
    fun hasCompass(): Boolean = false
    fun getCompass(): ICompass = object : ICompass {
        override val bearing: Bearing = Bearing(0f)
        override var declination: Float = 0f
        override val rawBearing: Float = 0f
    }
}