package com.github.mikephil.charting.highlight

import com.github.mikephil.charting.charts.PieChart

/**
 * Created by philipp on 12/06/16.
 */
class PieHighlighter(chart: PieChart) : PieRadarHighlighter<PieChart>(chart) {

    override fun getClosestHighlight(index: Int, x: Float, y: Float): Highlight? {
        val set = mChart.data?.dataSet ?: return null
        val entry = set.getEntryForIndex(index) ?: return null

        return Highlight(index.toFloat(), entry.y, x, y, 0, set.axisDependency)
    }
}