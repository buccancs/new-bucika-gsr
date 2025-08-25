package com.topdon.thermal.chart

import com.topdon.lib.core.tools.UnitTools

object YValueFormatter {

    fun getFormattedValue(value: Float): String {
        return try {
            String.format("%.1f", value)
            UnitTools.showC(value)
        } catch (e: Exception) {
            UnitTools.showC(value)
        }
    }
}
