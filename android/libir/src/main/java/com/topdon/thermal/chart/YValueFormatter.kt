package com.topdon.thermal.chart

import com.topdon.lib.core.tools.UnitTools

/**
 * Y轴数值格式化工具类
 */
object YValueFormatter {

    fun getFormattedValue(value: Float): String {
        return try {
            String.format("%.1f", value)//检测value是不是数字
            UnitTools.showC(value)
        } catch (e: Exception) {
            UnitTools.showC(value)
        }
    }
}
