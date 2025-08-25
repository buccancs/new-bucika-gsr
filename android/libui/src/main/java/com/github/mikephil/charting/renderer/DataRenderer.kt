package com.github.mikephil.charting.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.ChartInterface
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * Superclass of all render classes for the different data types (line, bar, ...).
 *
 * @author Philipp Jahoda
 */
abstract class DataRenderer(
    /**
     * the animator object used to perform animations on the chart data
     */
    protected val mAnimator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : Renderer(viewPortHandler) {

    /**
     * main paint object used for rendering
     */
    protected val mRenderPaint: Paint

    /**
     * paint used for highlighting values
     */
    protected val mHighlightPaint: Paint

    protected val mHighlightDotPaint: Paint

    protected val mDrawPaint: Paint

    /**
     * paint object for drawing values (text representing values of chart
     * entries)
     */
    protected val mValuePaint: Paint

    init {
        mRenderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Style.FILL
        }

        mDrawPaint = Paint(Paint.DITHER_FLAG)

        mValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(63, 63, 63)
            textAlign = Align.CENTER
            textSize = Utils.convertDpToPixel(9f)
        }

        mHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Style.STROKE
            strokeWidth = 2f
            color = Color.rgb(255, 187, 115)
        }

        mHighlightDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Style.FILL_AND_STROKE
            strokeWidth = 2f
            color = Color.rgb(243, 129, 47)
        }
    }

    protected fun isDrawingValuesAllowed(chart: ChartInterface): Boolean {
        //TODO Attempt to invoke virtual method 'int com.github.mikephil.charting.data.ChartData.getEntryCount()' on a null object reference
        return chart.data?.entryCount ?: 0 < chart.maxVisibleCount * mViewPortHandler.scaleX
    }

    /**
     * Returns the Paint object this renderer uses for drawing the values
     * (value-text).
     *
     * @return
     */
    fun getPaintValues(): Paint = mValuePaint

    /**
     * Returns the Paint object this renderer uses for drawing highlight
     * indicators.
     *
     * @return
     */
    fun getPaintHighlight(): Paint = mHighlightPaint

    /**
     * Returns the Paint object used for rendering.
     *
     * @return
     */
    fun getPaintRender(): Paint = mRenderPaint

    /**
     * Applies the required styling (provided by the DataSet) to the value-paint
     * object.
     *
     * @param set
     */
    protected fun applyValueTextStyle(set: IDataSet<*>) {
        mValuePaint.typeface = set.valueTypeface
        mValuePaint.textSize = set.valueTextSize
    }

    /**
     * Initializes the buffers used for rendering with a new size. Since this
     * method performs memory allocations, it should only be called if
     * necessary.
     */
    abstract fun initBuffers()

    /**
     * Draws the actual data in form of lines, bars, ... depending on Renderer subclass.
     *
     * @param c
     */
    abstract fun drawData(c: Canvas)

    /**
     * Loops over all Entrys and draws their values.
     *
     * @param c
     */
    abstract fun drawValues(c: Canvas)

    /**
     * Draws the value of the given entry by using the provided IValueFormatter.
     *
     * @param c         canvas
     * @param valueText label to draw
     * @param x         position
     * @param y         position
     * @param color
     */
    abstract fun drawValue(c: Canvas, valueText: String, x: Float, y: Float, color: Int)

    /**
     * Draws any kind of additional information (e.g. line-circles).
     *
     * @param c
     */
    abstract fun drawExtras(c: Canvas)

    /**
     * Draws all highlight indicators for the values that are currently highlighted.
     *
     * @param c
     * @param indices the highlighted values
     */
    abstract fun drawHighlighted(c: Canvas, indices: Array<Highlight>)
}