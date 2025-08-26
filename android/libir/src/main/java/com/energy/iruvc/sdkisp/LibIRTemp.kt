package com.energy.iruvc.sdkisp

import android.graphics.Point
import android.graphics.Rect
import com.energy.iruvc.utils.Line

/**
 * LibIRTemp stub implementation for thermal image processing
 */
class LibIRTemp(private val width: Int, private val height: Int) {
    
    fun getTemperatureOfPoint(point: Point): TemperatureSampleResult? {
        return TemperatureSampleResult()
    }
    
    fun getTemperatureOfLine(line: Line): TemperatureSampleResult? {
        return TemperatureSampleResult()
    }
    
    fun getTemperatureOfRect(rect: Rect): TemperatureSampleResult? {
        return TemperatureSampleResult()
    }
    
    class TemperatureSampleResult {
        var count: Int = 0
        var temperatureArray: FloatArray? = null
        var maxTemperature: Float = 0f
        var minTemperature: Float = 0f
        var averageTemperature: Float = 0f
    }
}