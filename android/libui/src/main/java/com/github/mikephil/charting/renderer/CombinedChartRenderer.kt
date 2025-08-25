package com.github.mikephil.charting.renderer

import android.graphics.Canvas
import android.util.Log
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.ViewPortHandler
import java.lang.ref.WeakReference

/**
 * Renderer class that is responsible for rendering multiple different data-types.
 */
class CombinedChartRenderer(
    chart: CombinedChart, 
    animator: ChartAnimator, 
    viewPortHandler: ViewPortHandler
) : DataRenderer(animator, viewPortHandler) {

    /**
     * all renderers for the different kinds of data this combined-renderer can draw
     */
    protected val mRenderers = mutableListOf<DataRenderer>()

    protected val mChart: WeakReference<Chart> = WeakReference(chart)

    private val mHighlightBuffer = mutableListOf<Highlight>()

    init {
        createRenderers()
    }

    /**
     * Creates the renderers needed for this combined-renderer in the required order. Also takes the DrawOrder into
     * consideration.
     */
    fun createRenderers() {
        mRenderers.clear()

        val chart = mChart.get() as? CombinedChart ?: return

        val orders = chart.drawOrder

        for (order in orders) {
            when (order) {
                DrawOrder.BAR -> {
                    if (chart.barData != null)
                        mRenderers.add(BarChartRenderer(chart, mAnimator, mViewPortHandler))
                }
                DrawOrder.BUBBLE -> {
                    if (chart.bubbleData != null)
                        mRenderers.add(BubbleChartRenderer(chart, mAnimator, mViewPortHandler))
                }
                DrawOrder.LINE -> {
                    if (chart.lineData != null)
                        mRenderers.add(LineChartRenderer(chart, mAnimator, mViewPortHandler))
                }
                DrawOrder.CANDLE -> {
                    if (chart.candleData != null)
                        mRenderers.add(CandleStickChartRenderer(chart, mAnimator, mViewPortHandler))
                }
                DrawOrder.SCATTER -> {
                    if (chart.scatterData != null)
                        mRenderers.add(ScatterChartRenderer(chart, mAnimator, mViewPortHandler))
                }
            }
        }
    }

    override fun initBuffers() {
        for (renderer in mRenderers)
            renderer.initBuffers()
    }

    override fun drawData(c: Canvas) {
        for (renderer in mRenderers)
            renderer.drawData(c)
    }

    override fun drawValue(c: Canvas, valueText: String, x: Float, y: Float, color: Int) {
        Log.e("MPAndroidChart", "Erroneous call to drawValue() in CombinedChartRenderer!")
    }

    override fun drawValues(c: Canvas) {
        for (renderer in mRenderers)
            renderer.drawValues(c)
    }

    override fun drawExtras(c: Canvas) {
        for (renderer in mRenderers)
            renderer.drawExtras(c)
    }

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {
        val chart = mChart.get() ?: return

        for (renderer in mRenderers) {
            val data: ChartData<*>? = when (renderer) {
                is BarChartRenderer -> renderer.mChart.barData
                is LineChartRenderer -> renderer.mChart.lineData
                is CandleStickChartRenderer -> renderer.mChart.candleData
                is ScatterChartRenderer -> renderer.mChart.scatterData
                is BubbleChartRenderer -> renderer.mChart.bubbleData
                else -> null
            }

            val dataIndex = data?.let { 
                (chart.data as? CombinedData)?.allData?.indexOf(it) 
            } ?: -1

            mHighlightBuffer.clear()

            for (h in indices) {
                if (h.dataIndex == dataIndex || h.dataIndex == -1)
                    mHighlightBuffer.add(h)
            }

            renderer.drawHighlighted(c, mHighlightBuffer.toTypedArray())
        }
    }

    /**
     * Returns the sub-renderer object at the specified index.
     *
     * @param index
     * @return
     */
    fun getSubRenderer(index: Int): DataRenderer? {
        return if (index >= mRenderers.size || index < 0)
            null
        else
            mRenderers[index]
    }

    /**
     * Returns all sub-renderers.
     *
     * @return
     */
    fun getSubRenderers(): List<DataRenderer> {
        return mRenderers
    }

    fun setSubRenderers(renderers: List<DataRenderer>) {
        mRenderers.clear()
        mRenderers.addAll(renderers)
    }
}