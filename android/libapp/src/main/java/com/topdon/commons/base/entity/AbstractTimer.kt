package com.topdon.commons.base.entity

import android.os.Handler
import android.os.Looper
import java.util.Timer
import java.util.TimerTask

abstract class AbstractTimer(private val callbackOnMainThread: Boolean) {
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    abstract fun onTick()

    @Synchronized
    fun start(delay: Long, period: Long) {
        if (timer == null) {
            timer = Timer().apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        if (callbackOnMainThread) {
                            handler.post { onTick() }
                        } else {
                            onTick()
                        }
                    }
                }, delay, period)
            }
        }
    }

    @Synchronized
    fun stop() {
        timer?.apply {
            cancel()
            timer = null
        }
    }

    val isRunning: Boolean
        get() = timer != null
}