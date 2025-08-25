package com.github.mikephil.charting.highlight

import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.dataprovider.CombinedDataProvider

/**
 * Created by Philipp Jahoda on 12/09/15.
 */
class CombinedHighlighter(chart: CombinedDataProvider, barChart: BarDataProvider) : 
    ChartHighlighter<CombinedDataProvider>(chart) {

    /**
     * bar highlighter for supporting stacked highlighting
     */
    protected val barHighlighter: BarHighlighter? = if (barChart.barData == null) null else BarHighlighter(barChart)

    override fun getHighlightsAtXValue(xVal: Float, x: Float, y: Float): List<Highlight> {
        mHighlightBuffer.clear()

        val dataObjects = mChart.combinedData.allData

        for (i in dataObjects.indices) {
            val dataObject = dataObjects[i]

            // in case of BarData, let the BarHighlighter take over
            if (barHighlighter != null && dataObject is BarData) {
                val high = barHighlighter.getHighlight(x, y)

                if (high != null) {
                    high.dataIndex = i
                    mHighlightBuffer.add(high)
                }
            } else {
                for (j in 0 until dataObject.dataSetCount) {
                    val dataSet = dataObjects[i].getDataSetByIndex(j)

                    // don't include datasets that cannot be highlighted
                    if (!dataSet.isHighlightEnabled) continue

                    val highs = buildHighlights(dataSet, j, xVal, DataSet.Rounding.CLOSEST)
                    for (high in highs) {
                        high.dataIndex = i
                        mHighlightBuffer.add(high)
                    }
                }
            }
        }

        return mHighlightBuffer
    }
}