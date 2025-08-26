package com.energy.view.tempcanvas

data class TempResultBean(
    var id: String,
    var label: String,
    var content: String,
    var minTemperature: Float,
    var maxTemperature: Float,
    var averageTemperature: Float
) {
    var order: Long = 0
    var position: Int = 0

    var tempInfoMode: TempInfoMode? = null

    var ambientTemp: Float = 0f

    var measureDistance: Float = 0f

    var emissivity: Float = 0f

    var highAlertEnable: Boolean = false

    var highThreshold: Float = 0f

    var lowAlertEnable: Boolean = false

    var lowThreshold: Float = 0f

    var x1: Int = 0
    var y1: Int = 0
    var x2_or_r1: Int = 0
    var y2_or_r2: Int = 0
    var max_temp_x: Int = 0
    var max_temp_y: Int = 0
    var min_temp_x: Int = 0
    var min_temp_y: Int = 0
}
