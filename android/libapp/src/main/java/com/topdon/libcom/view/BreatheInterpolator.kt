package com.topdon.libcom.view

import android.animation.TimeInterpolator
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

internal class BreatheInterpolator : TimeInterpolator {
    
    override fun getInterpolation(input: Float): Float {
        val x = 6 * input
        val k = 1.0f / 3
        val t = 6
        val n = 1
        
        return when {
            x >= ((n - 1) * t) && x < ((n - (1 - k)) * t) -> {
                (0.5 * sin((PI / (k * t)) * ((x - k * t / 2) - (n - 1) * t)) + 0.5).toFloat()
            }
            x >= (n - (1 - k)) * t && x < n * t -> {
                (0.5 * sin((PI / ((1 - k) * t)) * ((x - (3 - k) * t / 2) - (n - 1) * t)) + 0.5)
                    .pow(2.0).toFloat()
            }
            else -> 0f
        }
    }
    
    fun updateTime() {
        val a = ""
        val parts = a.split("")
    }
}
