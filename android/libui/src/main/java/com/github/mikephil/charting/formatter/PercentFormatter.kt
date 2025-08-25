package com.github.mikephil.charting.formatter

import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieEntry
import java.text.DecimalFormat

/**
 * This IValueFormatter is just for convenience and simply puts a "%" sign after
 * each value. (Recommended for PieChart)
 *
 * @author Philipp Jahoda
 */
class PercentFormatter : ValueFormatter {

    val format: DecimalFormat = DecimalFormat("###,###,##0.0")
    private var pieChart: PieChart? = null

    constructor()

    // Can be used to remove percent signs if the chart isn't in percent mode
    constructor(pieChart: PieChart) {
        this.pieChart = pieChart
    }

    override fun getFormattedValue(value: Float): String {
        return format.format(value) + " %"
    }

    override fun getPieLabel(value: Float, pieEntry: PieEntry): String {
        return if (pieChart?.isUsePercentValuesEnabled == true) {
            // Converted to percent
            getFormattedValue(value)
        } else {
            // raw value, skip percent sign
            format.format(value)
        }
    }
}