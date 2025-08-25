package com.energy.view.tempcanvas

/**
 * Created by fengjibo on 2024/2/20.
 */
data class TempResultBean(
    var id: String,
    var label: String,
    var content: String,
    var minTemperature: Float,
    var maxTemperature: Float,
    var averageTemperature: Float
) {
    var order: Long = 0
    var position: Int = 0 // 相对应点线框list的position

    // 类型
    var tempInfoMode: TempInfoMode? = null
    // 环境温度
    var ambientTemp: Float = 0f
    // 测温距离
    var measureDistance: Float = 0f
    // 辐射率
    var emissivity: Float = 0f
    // 高温报警开关
    var highAlertEnable: Boolean = false
    // 高温阀值
    var highThreshold: Float = 0f
    // 低温报警开关
    var lowAlertEnable: Boolean = false
    // 低温阀值
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
