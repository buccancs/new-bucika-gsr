package com.topdon.lib.core.bean

import com.topdon.lib.core.utils.TemperatureUtil

data class CarDetectChildBean(
    var type: Int,
    var pos: Int,
    var description: String,
    var item: String,
    var temperature: String,
    var isSelected: Boolean = false
) {
    fun buildString(): String {
        val temperatures = temperature.split("~")
        return item + TemperatureUtil.getTempStr(temperatures[0].toInt(), temperatures[1].toInt())
    }
