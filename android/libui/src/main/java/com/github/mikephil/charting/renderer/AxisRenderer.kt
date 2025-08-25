package com.github.mikephil.charting.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.utils.MPPointD
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.*

/**
 * Baseclass of all axis renderers.
 *
 * @author Philipp Jahoda
 */
abstract class AxisRenderer(
    viewPortHandler: ViewPortHandler,
    /** transformer to transform values to screen pixels and return */
    protected val mTrans: Transformer,
    /** base axis this axis renderer works with */
    protected val mAxis: AxisBase
) : Renderer(viewPortHandler) {

    /**
     * paint object for the grid lines
     */
    protected val mGridPaint: Paint

    /**
     * paint for the x-label values
     */
    protected val mAxisLabelPaint: Paint

    /**
     * paint for the line surrounding the chart
     */
    protected val mAxisLinePaint: Paint

    /**
     * paint used for the limit lines
     */
    protected val mLimitLinePaint: Paint

    init {
        if (mViewPortHandler != null) {
            mAxisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            mGridPaint = Paint().apply {
                color = Color.GRAY
                strokeWidth = 1f
                style = Style.STROKE
                alpha = 90
            }

            mAxisLinePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
                style = Style.STROKE
            }

            mLimitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Style.STROKE
            }
        } else {
            mAxisLabelPaint = Paint()
            mGridPaint = Paint()
            mAxisLinePaint = Paint()
            mLimitLinePaint = Paint()
        }
    }

    /**
     * Returns the Paint object used for drawing the axis (labels).
     */
    fun getPaintAxisLabels(): Paint = mAxisLabelPaint

    /**
     * Returns the Paint object that is used for drawing the grid-lines of the
     * axis.
     */
    fun getPaintGrid(): Paint = mGridPaint

    /**
     * Returns the Paint object that is used for drawing the axis-line that goes
     * alongside the axis.
     */
    fun getPaintAxisLine(): Paint = mAxisLinePaint

    /**
     * Returns the Transformer object used for transforming the axis values.
     */
    fun getTransformer(): Transformer = mTrans

    /**
     * Computes the axis values.
     *
     * @param min - the minimum value in the data object for this axis
     * @param max - the maximum value in the data object for this axis
     */
    open fun computeAxis(min: Float, max: Float, inverted: Boolean) {
        var minValue = min
        var maxValue = max

        // calculate the starting and entry point of the y-labels (depending on
        // zoom / contentrect bounds)
        if (mViewPortHandler != null && mViewPortHandler.contentWidth() > 10 && !mViewPortHandler.isFullyZoomedOutY()) {
            val p1 = mTrans.getValuesByTouchPoint(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop())
            val p2 = mTrans.getValuesByTouchPoint(mViewPortHandler.contentLeft(), mViewPortHandler.contentBottom())

            if (!inverted) {
                minValue = p2.y.toFloat()
                maxValue = p1.y.toFloat()
            } else {
                minValue = p1.y.toFloat()
                maxValue = p2.y.toFloat()
            }

            MPPointD.recycleInstance(p1)
            MPPointD.recycleInstance(p2)
        }

        computeAxisValues(minValue, maxValue)
    }

    /**
     * Sets up the axis values. Computes the desired number of labels between the two given extremes.
     */
    protected fun computeAxisValues(min: Float, max: Float) {
        val yMin = min
        val yMax = max

        val labelCount = mAxis.labelCount
        val range = abs(yMax - yMin).toDouble()

        if (labelCount == 0 || range <= 0 || range.isInfinite()) {
            mAxis.mEntries = floatArrayOf()
            mAxis.mCenteredEntries = floatArrayOf()
            mAxis.mEntryCount = 0
            return
        }

        // Find out how much spacing (in y value space) between axis values
        val rawInterval = range / labelCount
        var interval = Utils.roundToNextSignificant(rawInterval)

        // If granularity is enabled, then do not allow the interval to go below specified granularity.
        // This is used to avoid repeated values when rounding values for display.
        if (mAxis.isGranularityEnabled)
            interval = if (interval < mAxis.granularity) mAxis.granularity.toDouble() else interval

        // Normalize interval
        val intervalMagnitude = Utils.roundToNextSignificant(10.0.pow(log10(interval).toInt()))
        val intervalSigDigit = (interval / intervalMagnitude).toInt()
        if (intervalSigDigit > 5) {
            // Use one order of magnitude higher, to avoid intervals like 0.9 or 90
            interval = floor(10 * intervalMagnitude)
        }

        var n = if (mAxis.isCenterAxisLabelsEnabled) 1 else 0

        // force label count
        if (mAxis.isForceLabelsEnabled) {
            interval = range / (labelCount - 1).toFloat()
            mAxis.mEntryCount = labelCount

            if (mAxis.mEntries.size < labelCount) {
                // Ensure stops contains at least numStops elements.
                mAxis.mEntries = FloatArray(labelCount)
            }

            var v = min
            for (i in 0 until labelCount) {
                mAxis.mEntries[i] = v
                v += interval.toFloat()
            }

            n = labelCount

        } else {
            var first = if (interval == 0.0) 0.0 else ceil(yMin / interval) * interval
            if (mAxis.isCenterAxisLabelsEnabled) {
                first -= interval
            }

            val last = if (interval == 0.0) 0.0 else Utils.nextUp(floor(yMax / interval) * interval)

            if (interval != 0.0) {
                var f = first
                while (f <= last) {
                    ++n
                    f += interval
                }
            }

            mAxis.mEntryCount = n

            if (mAxis.mEntries.size < n) {
                // Ensure stops contains at least numStops elements.
                mAxis.mEntries = FloatArray(n)
            }

            var f = first
            for (i in 0 until n) {
                if (f == 0.0) // Fix for negative zero case (Where value == -0.0, and 0.0 == -0.0)
                    f = 0.0

                mAxis.mEntries[i] = f.toFloat()
                f += interval
            }
        }

        // set decimals
        mAxis.mDecimals = if (interval < 1) {
            ceil(-log10(interval)).toInt()
        } else {
            0
        }

        if (mAxis.isCenterAxisLabelsEnabled) {
            if (mAxis.mCenteredEntries.size < n) {
                mAxis.mCenteredEntries = FloatArray(n)
            }

            val offset = (interval / 2f).toFloat()
            for (i in 0 until n) {
                mAxis.mCenteredEntries[i] = mAxis.mEntries[i] + offset
            }
        }
    }

    /**
     * Draws the axis labels to the screen.
     */
    abstract fun renderAxisLabels(c: Canvas)

    /**
     * Draws the grid lines belonging to the axis.
     */
    abstract fun renderGridLines(c: Canvas)

    /**
     * Draws the line that goes alongside the axis.
     */
    abstract fun renderAxisLine(c: Canvas)

    /**
     * Draws the LimitLines associated with this axis to the screen.
     */
    abstract fun renderLimitLines(c: Canvas)
}