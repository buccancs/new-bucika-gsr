
package com.github.mikephil.charting.charts

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent

import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarLineScatterCandleBubbleData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.ChartHighlighter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.BarLineScatterCandleBubbleDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarLineScatterCandleBubbleDataSet
import com.github.mikephil.charting.jobs.AnimatedMoveViewJob
import com.github.mikephil.charting.jobs.AnimatedZoomJob
import com.github.mikephil.charting.jobs.MoveViewJob
import com.github.mikephil.charting.jobs.ZoomJob
import com.github.mikephil.charting.listener.BarLineChartTouchListener
import com.github.mikephil.charting.listener.OnDrawListener
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.renderer.YAxisRenderer
import com.github.mikephil.charting.utils.MPPointD
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils

/**
 * Base-class of LineChart, BarChart, ScatterChart and CandleStickChart.
 *
 * @author Philipp Jahoda
 */
@SuppressLint("RtlHardcoded")
abstract class BarLineChartBase<T : BarLineScatterCandleBubbleData<out IBarLineScatterCandleBubbleDataSet<out Entry>>>(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : Chart<T>(context, attrs, defStyle), BarLineScatterCandleBubbleDataProvider {
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    constructor(context: Context) : this(context, null, 0)


    /**
     * the maximum number of entries to which values will be drawn
     * (entry numbers greater than this value will cause value-labels to disappear)
     */
    protected var maxVisibleCount = 100

    /**
     * flag that indicates if auto scaling on the y axis is enabled
     */
    protected var isAutoScaleMinMaxEnabled = false

    /**
     * flag that indicates if pinch-zoom is enabled. if true, both x and y axis
     * can be scaled with 2 fingers, if false, x and y axis can be scaled
     * separately
     */
    var isPinchZoomEnabled = false
        protected set

    /**
     * flag that indicates if double tap zoom is enabled or not
     */
    var isDoubleTapToZoomEnabled = true

    /**
     * flag that indicates if highlighting per dragging over a fully zoomed out
     * chart is enabled
     */
    var isHighlightPerDragEnabled = true

    /**
     * if true, dragging is enabled for the chart
     */
    var isDragXEnabled = true
        private set
    var isDragYEnabled = true
        private set

    var isScaleXEnabled = true
        private set
    var isScaleYEnabled = true
        private set

    /**
     * paint object for the (by default) lightgrey background of the grid
     */
    protected lateinit var gridBackgroundPaint: Paint

    protected lateinit var borderPaint: Paint

    /**
     * flag indicating if the grid background should be drawn or not
     */
    var isDrawGridBackgroundEnabled = false
        protected set

    var isDrawBordersEnabled = false
        protected set

    var isClipValuesToContentEnabled = false
        protected set

    /**
     * Sets the minimum offset (padding) around the chart, defaults to 15
     */
    protected var minOffset = 15f

    /**
     * flag indicating if the chart should stay at the same position after a rotation. Default is false.
     */
    var isKeepPositionOnRotation = false

    /**
     * the listener for user drawing on the chart
     */
    var onDrawListener: OnDrawListener? = null

    /**
     * the object representing the labels on the left y-axis
     */
    lateinit var axisLeft: YAxis
        protected set

    /**
     * the object representing the labels on the right y-axis
     */
    lateinit var axisRight: YAxis
        protected set

    protected lateinit var axisRendererLeft: YAxisRenderer
    protected lateinit var axisRendererRight: YAxisRenderer

    protected lateinit var leftAxisTransformer: Transformer
    protected lateinit var rightAxisTransformer: Transformer

    protected lateinit var xAxisRenderer: XAxisRenderer

    // /** the approximator object used for data filtering */
    // private Approximator mApproximator

    override fun init() {
        super.init()

        axisLeft = YAxis(AxisDependency.LEFT)
        axisRight = YAxis(AxisDependency.RIGHT)

        leftAxisTransformer = Transformer(viewPortHandler)
        rightAxisTransformer = Transformer(viewPortHandler)

        axisRendererLeft = YAxisRenderer(viewPortHandler, axisLeft, leftAxisTransformer)
        axisRendererRight = YAxisRenderer(viewPortHandler, axisRight, rightAxisTransformer)

        xAxisRenderer = XAxisRenderer(viewPortHandler, xAxis, leftAxisTransformer)

        setHighlighter(ChartHighlighter(this))

        chartTouchListener = BarLineChartTouchListener(this, viewPortHandler.matrixTouch, 3f)

        gridBackgroundPaint = Paint()
        gridBackgroundPaint.setStyle(Style.FILL)
        // gridBackgroundPaint.setColor(Color.WHITE)
        gridBackgroundPaint.setColor(Color.rgb(240, 240, 240)); // light
        // grey

        borderPaint = Paint()
        borderPaint.setStyle(Style.STROKE)
        borderPaint.setColor(Color.BLACK)
        borderPaint.setStrokeWidth(Utils.convertDpToPixel(1f))
    }

    // for performance tracking
    private var totalTime: Long = 0
    private var drawCycles: Long = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data == null) return

        val starttime = System.currentTimeMillis()

        // execute all drawing commands
        drawGridBackground(canvas)

        if (isAutoScaleMinMaxEnabled) {
            autoScale()
        }

        if (axisLeft.isEnabled)
            axisRendererLeft.computeAxis(axisLeft.axisMinimum, axisLeft.axisMaximum, axisLeft.isInverted)

        if (axisRight.isEnabled)
            axisRendererRight.computeAxis(axisRight.axisMinimum, axisRight.axisMaximum, axisRight.isInverted)

        if (xAxis.isEnabled)
            xAxisRenderer.computeAxis(xAxis.axisMinimum, xAxis.axisMaximum, false)

        xAxisRenderer.renderAxisLine(canvas)
        axisRendererLeft.renderAxisLine(canvas)
        axisRendererRight.renderAxisLine(canvas)

        if (xAxis.isDrawGridLinesBehindDataEnabled)
            xAxisRenderer.renderGridLines(canvas)

        if (axisLeft.isDrawGridLinesBehindDataEnabled)
            axisRendererLeft.renderGridLines(canvas)

        if (axisRight.isDrawGridLinesBehindDataEnabled)
            axisRendererRight.renderGridLines(canvas)

        if (xAxis.isEnabled && xAxis.isDrawLimitLinesBehindDataEnabled)
            xAxisRenderer.renderLimitLines(canvas)

        if (axisLeft.isEnabled && axisLeft.isDrawLimitLinesBehindDataEnabled)
            axisRendererLeft.renderLimitLines(canvas)

        if (axisRight.isEnabled && axisRight.isDrawLimitLinesBehindDataEnabled)
            axisRendererRight.renderLimitLines(canvas)

        // make sure the data cannot be drawn outside the content-rect
        val clipRestoreCount = canvas.save()
        canvas.clipRect(viewPortHandler.contentRect)

        renderer.drawData(canvas)

        if (!xAxis.isDrawGridLinesBehindDataEnabled)
            xAxisRenderer.renderGridLines(canvas)

        if (!axisLeft.isDrawGridLinesBehindDataEnabled)
            axisRendererLeft.renderGridLines(canvas)

        if (!axisRight.isDrawGridLinesBehindDataEnabled)
            axisRendererRight.renderGridLines(canvas)

        // if highlighting is enabled
        if (valuesToHighlight())
            renderer.drawHighlighted(canvas, indicesToHighlight)

        // Removes clipping rectangle
        canvas.restoreToCount(clipRestoreCount)

        renderer.drawExtras(canvas)

        if (xAxis.isEnabled && !xAxis.isDrawLimitLinesBehindDataEnabled)
            xAxisRenderer.renderLimitLines(canvas)

        if (axisLeft.isEnabled && !axisLeft.isDrawLimitLinesBehindDataEnabled)
            axisRendererLeft.renderLimitLines(canvas)

        if (axisRight.isEnabled && !axisRight.isDrawLimitLinesBehindDataEnabled)
            axisRendererRight.renderLimitLines(canvas)

        xAxisRenderer.renderAxisLabels(canvas)
        axisRendererLeft.renderAxisLabels(canvas)
        axisRendererRight.renderAxisLabels(canvas)

        if (isClipValuesToContentEnabled()) {
            clipRestoreCount = canvas.save()
            canvas.clipRect(viewPortHandler.contentRect)

            renderer.drawValues(canvas)

            canvas.restoreToCount(clipRestoreCount)
        } else {
            renderer.drawValues(canvas)
        }

        legendRenderer.renderLegend(canvas)

        drawDescription(canvas)

        drawMarkers(canvas)

        if (isLogEnabled) {
            val drawtime = (System.currentTimeMillis() - starttime)
            totalTime += drawtime
            drawCycles += 1
            val average = totalTime / drawCycles
            Log.i(LOG_TAG, "Drawtime: "$$drawtime" ms, average: $average" + " ms, cycles: "
                    + drawCycles)
        }
    }

    /**
     * RESET PERFORMANCE TRACKING FIELDS
     */
    fun resetTracking() {
        totalTime = 0
        drawCycles = 0
    }

    protected fun prepareValuePxMatrix() {

        if (isLogEnabled)
            Log.i(LOG_TAG, "Preparing Value-Px Matrix, xmin: "$$xAxis.axisMinimum", xmax: "
                    + "$xAxis.axisMaximum, xdelta: $xAxis.axisRange);"

        rightAxisTransformer.prepareMatrixValuePx(xAxis.axisMinimum,
                xAxis.axisRange,
                axisRight.axisRange,
                axisRight.axisMinimum)
        leftAxisTransformer.prepareMatrixValuePx(xAxis.axisMinimum,
                xAxis.axisRange,
                axisLeft.axisRange,
                axisLeft.axisMinimum)
    }

    protected fun prepareOffsetMatrix() {

        rightAxisTransformer.prepareMatrixOffset(axisRight.isInverted)
        leftAxisTransformer.prepareMatrixOffset(axisLeft.isInverted)
    }

    override fun notifyDataSetChanged() {

        if (data == null) {
            if (isLogEnabled)
                Log.i(LOG_TAG, "Preparing... DATA NOT SET.")
            return
        } else {
            if (isLogEnabled)
                Log.i(LOG_TAG, "Preparing...")
        }

        renderer?.let { renderer.initBuffers() }

        calcMinMax()

        axisRendererLeft.computeAxis(axisLeft.axisMinimum, axisLeft.axisMaximum, axisLeft.isInverted)
        axisRendererRight.computeAxis(axisRight.axisMinimum, axisRight.axisMaximum, axisRight.isInverted)
        xAxisRenderer.computeAxis(xAxis.axisMinimum, xAxis.axisMaximum, false)

        legend?.let { legendRenderer.computeLegend(data) }

        calculateOffsets()
    }

    /**
     * Performs auto scaling of the axis by recalculating the minimum and maximum y-values based on the entries currently in view.
     */
    protected fun autoScale() {

        val fromX = getLowestVisibleX()
        val toX = getHighestVisibleX()

        data.calcMinMaxY(fromX, toX)

        xAxis.calculate(data.xMin, data.xMax)

        // calculate axis range (min / max) according to provided data

        if (axisLeft.isEnabled)
            axisLeft.calculate(data.getYMin(AxisDependency.LEFT),
                    data.getYMax(AxisDependency.LEFT))

        if (axisRight.isEnabled)
            axisRight.calculate(data.getYMin(AxisDependency.RIGHT),
                    data.getYMax(AxisDependency.RIGHT))

        calculateOffsets()
    }

    override fun calcMinMax() {

        xAxis.calculate(data.xMin, data.xMax)

        // calculate axis range (min / max) according to provided data
        axisLeft.calculate(data.getYMin(AxisDependency.LEFT), data.getYMax(AxisDependency.LEFT))
        axisRight.calculate(data.getYMin(AxisDependency.RIGHT), data.getYMax(AxisDependency
                .RIGHT))
    }

    protected void calculateLegendOffsets(RectF offsets) {

        offsets.left = 0.f
        offsets.right = 0.f
        offsets.top = 0.f
        offsets.bottom = 0.f

        // setup offsets for legend
        if (legend != null && legend.isEnabled && !legend.isDrawInsideEnabled) {
            when (legend.orientation) {
                Legend.LegendOrientation.VERTICAL -> {
                    when (legend.horizontalAlignment) {
                        Legend.LegendHorizontalAlignment.LEFT -> {
                            offsets.left += kotlin.math.min(
                                legend.neededWidth,
                                viewPortHandler.chartWidth * legend.maxSizePercent
                            ) + legend.xOffset
                        }
                        Legend.LegendHorizontalAlignment.RIGHT -> {
                            offsets.right += kotlin.math.min(
                                legend.neededWidth,
                                viewPortHandler.chartWidth * legend.maxSizePercent
                            ) + legend.xOffset
                        }
                        Legend.LegendHorizontalAlignment.CENTER -> {
                            when (legend.verticalAlignment) {
                                Legend.LegendVerticalAlignment.TOP -> {
                                    offsets.top += kotlin.math.min(
                                        legend.neededHeight,
                                        viewPortHandler.chartHeight * legend.maxSizePercent
                                    ) + legend.yOffset
                                }
                                Legend.LegendVerticalAlignment.BOTTOM -> {
                                    offsets.bottom += kotlin.math.min(
                                        legend.neededHeight,
                                        viewPortHandler.chartHeight * legend.maxSizePercent
                                    ) + legend.yOffset
                                }
                                else -> {}
                            }
                        }
                    }
                }
                Legend.LegendOrientation.HORIZONTAL -> {
                    when (legend.verticalAlignment) {
                        Legend.LegendVerticalAlignment.TOP -> {
                            offsets.top += kotlin.math.min(
                                legend.neededHeight,
                                viewPortHandler.chartHeight * legend.maxSizePercent
                            ) + legend.yOffset
                        }
                        Legend.LegendVerticalAlignment.BOTTOM -> {
                            offsets.bottom += kotlin.math.min(
                                legend.neededHeight,
                                viewPortHandler.chartHeight * legend.maxSizePercent
                            ) + legend.yOffset
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private offset: FloatsBuffer = RectF()

    override fun calculateOffsets() {

        if (!isCustomViewPortEnabled) {

            offset: FloatLeft = 0f, offsetRight = 0f, offsetTop = 0f, offsetBottom = 0f

            calculateLegendOffsets(mOffsetsBuffer)

            offsetLeft += mOffsetsBuffer.left
            offsetTop += mOffsetsBuffer.top
            offsetRight += mOffsetsBuffer.right
            offsetBottom += mOffsetsBuffer.bottom

            // offsets for y-labels
            if (axisLeft.needsOffset()) {
                offsetLeft += axisLeft.getRequiredWidthSpace(axisRendererLeft
                        .getPaintAxisLabels())
            }

            if (axisRight.needsOffset()) {
                offsetRight += axisRight.getRequiredWidthSpace(axisRendererRight
                        .getPaintAxisLabels())
            }

            if (xAxis.isEnabled && xAxis.isDrawLabelsEnabled) {

                val xLabelHeight = xAxis.labelRotatedHeight + xAxis.yOffset

                // offsets for x-labels
                if (xAxis.position == XAxisPosition.BOTTOM) {

                    offsetBottom += xLabelHeight

                } else if (xAxis.position == XAxisPosition.TOP) {

                    offsetTop += xLabelHeight

                } else if (xAxis.position == XAxisPosition.BOTH_SIDED) {

                    offsetBottom += xLabelHeight
                    offsetTop += xLabelHeight
                }
            }

            offsetTop += getExtraTopOffset()
            offsetRight += getExtraRightOffset()
            offsetBottom += getExtraBottomOffset()
            offsetLeft += getExtraLeftOffset()

            val minOffset = Utils.convertDpToPixel(mMinOffset)

            viewPortHandler.restrainViewPort(
                    kotlin.math.max(minOffset, offsetLeft),
                    kotlin.math.max(minOffset, offsetTop),
                    kotlin.math.max(minOffset, offsetRight),
                    kotlin.math.max(minOffset, offsetBottom))

            if (isLogEnabled) {
                Log.i(LOG_TAG, "offsetLeft: "$$offsetLeft", offsetTop: $offsetTop"
                        + ", offsetRight: "$$offsetRight", offsetBottom: $offsetBottom);"
                Log.i(LOG_TAG, "Content: $viewPortHandler.contentRect.toString());"
            }
        }

        prepareOffsetMatrix()
        prepareValuePxMatrix()
    }

    /**
     * draws the grid background
     */
    protected void drawGridBackground(Canvas c) {

        if (isDrawGridBackgroundEnabled) {

            // draw the grid background
            c.drawRect(viewPortHandler.contentRect, gridBackgroundPaint)
        }

        if (isDrawBordersEnabled) {
            c.drawRect(viewPortHandler.contentRect, borderPaint)
        }
    }

    /**
     * Returns the Transformer class that contains all matrices and is
     * responsible for transforming values into pixels on the screen and
     * backwards.
     *
     * @return
     */
    fun getTransformer(which: AxisDependency): Transformer {
        if (which == AxisDependency.LEFT)
            return leftAxisTransformer
        else
            return rightAxisTransformer
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        if (chartTouchListener == null || data == null)
            return false

        // check if touch gestures are enabled
        if (!isTouchEnabled)
            return false
        else
            return chartTouchListener.onTouch(this, event)
    }

    override fun computeScroll() {

        if (chartTouchListener is BarLineChartTouchListener) {
            (chartTouchListener as BarLineChartTouchListener).computeScroll()
        }
    }

    /**
     * ################ ################ ################ ################
     */
    /**
     * CODE BELOW THIS RELATED TO SCALING AND GESTURES AND MODIFICATION OF THE
     * VIEWPORT
     */

    protected Matrix mZoomMatrixBuffer = Matrix()

    /**
     * Zooms in by 1.4f, into the charts center.
     */
    fun zoomIn() {

        MPPointF center = viewPortHandler.getContentCenter()

        viewPortHandler.zoomIn(center.x, -center.y, mZoomMatrixBuffer)
        viewPortHandler.refresh(mZoomMatrixBuffer, this, false)

        MPPointF.recycleInstance(center)

        // Range might have changed, which means that Y-axis labels
        // could have changed in size, affecting Y-axis size.
        // So we need to recalculate offsets.
        calculateOffsets()
        postInvalidate()
    }

    /**
     * Zooms out by 0.7f, from the charts center.
     */
    fun zoomOut() {

        MPPointF center = viewPortHandler.getContentCenter()

        viewPortHandler.zoomOut(center.x, -center.y, mZoomMatrixBuffer)
        viewPortHandler.refresh(mZoomMatrixBuffer, this, false)

        MPPointF.recycleInstance(center)

        // Range might have changed, which means that Y-axis labels
        // could have changed in size, affecting Y-axis size.
        // So we need to recalculate offsets.
        calculateOffsets()
        postInvalidate()
    }

    /**
     * Zooms out to original size.
     */
    fun resetZoom() {

        viewPortHandler.resetZoom(mZoomMatrixBuffer)
        viewPortHandler.refresh(mZoomMatrixBuffer, this, false)

        // Range might have changed, which means that Y-axis labels
        // could have changed in size, affecting Y-axis size.
        // So we need to recalculate offsets.
        calculateOffsets()
        postInvalidate()
    }

    /**
     * Zooms in or out by the given scale factor. x and y are the coordinates
     * (in pixels) of the zoom center.
     *
     * @param scaleX if < 1f --> zoom out, if > 1f --> zoom in
     * @param scaleY if < 1f --> zoom out, if > 1f --> zoom in
     * @param x
     * @param y
     */
    fun zoom(scaleX: Float, scaleY: Float, x: Float, y: Float) {

        viewPortHandler.zoom(scaleX, scaleY, x, -y, mZoomMatrixBuffer)
        viewPortHandler.refresh(mZoomMatrixBuffer, this, false)

        // Range might have changed, which means that Y-axis labels
        // could have changed in size, affecting Y-axis size.
        // So we need to recalculate offsets.
        calculateOffsets()
        postInvalidate()
    }

    /**
     * Zooms in or out by the given scale factor.
     * x and y are the values (NOT PIXELS) of the zoom center..
     *
     * @param scaleX
     * @param scaleY
     * @param xValue
     * @param yValue
     * @param axis   the axis relative to which the zoom should take place
     */
    fun zoom(float scaleX, float scaleY, float xValue, float yValue, AxisDependency axis) {

        Runnable job = ZoomJob.getInstance(viewPortHandler, scaleX, scaleY, xValue, yValue, getTransformer(axis), axis, this)
        addViewportJob(job)
    }

    /**
     * Zooms to the center of the chart with the given scale factor.
     *
     * @param scaleX
     * @param scaleY
     */
    fun zoomToCenter(scaleX: Float, scaleY: Float) {

        MPPointF center = getCenterOffsets()

        Matrix save = mZoomMatrixBuffer
        viewPortHandler.zoom(scaleX, scaleY, center.x, -center.y, save)
        viewPortHandler.refresh(save, this, false)
    }

    /**
     * Zooms by the specified scale factor to the specified values on the specified axis.
     *
     * @param scaleX
     * @param scaleY
     * @param xValue
     * @param yValue
     * @param axis
     * @param duration
     */
    @TargetApi(11)
    fun zoomAndCenterAnimated(scaleX: Float, scaleY: Float, float xValue, float yValue, AxisDependency axis,
                                      duration: Long) {

        MPPointD origin = getValuesByTouchPoint(viewPortHandler.contentLeft(), viewPortHandler.contentTop(), axis)

        Runnable job = AnimatedZoomJob.getInstance(viewPortHandler, this, getTransformer(axis), getAxis(axis), xAxis
                        .axisRange, scaleX, scaleY, viewPortHandler.getScaleX(), viewPortHandler.getScaleY(),
                xValue, yValue, (float) origin.x, (float) origin.y, duration)
        addViewportJob(job)

        MPPointD.recycleInstance(origin)
    }

    protected Matrix mFitScreenMatrixBuffer = Matrix()

    /**
     * Resets all zooming and dragging and makes the chart fit exactly it's
     * bounds.
     */
    fun fitScreen() {
        Matrix save = mFitScreenMatrixBuffer
        viewPortHandler.fitScreen(save)
        viewPortHandler.refresh(save, this, false)

        calculateOffsets()
        postInvalidate()
    }

    /**
     * Sets the minimum scale factor value to which can be zoomed out. 1f =
     * fitScreen
     *
     * @param scaleX
     * @param scaleY
     */
    fun setScaleMinima(scaleX: Float, scaleY: Float) {
        viewPortHandler.setMinimumScaleX(scaleX)
        viewPortHandler.setMinimumScaleY(scaleY)
    }

    /**
     * Sets the size of the area (range on the x-axis) that should be maximum
     * visible at once (no further zooming out allowed). If this is e.g. set to
     * 10, no more than a range of 10 on the x-axis can be viewed at once without
     * scrolling.
     *
     * @param maxXRange The maximum visible range of x-values.
     */
    fun setVisibleXRangeMaximum(float maxXRange) {
        float xScale = xAxis.axisRange / (maxXRange)
        viewPortHandler.setMinimumScaleX(xScale)
    }

    /**
     * Sets the size of the area (range on the x-axis) that should be minimum
     * visible at once (no further zooming in allowed). If this is e.g. set to
     * 10, no less than a range of 10 on the x-axis can be viewed at once without
     * scrolling.
     *
     * @param minXRange The minimum visible range of x-values.
     */
    fun setVisibleXRangeMinimum(minXRange: Float) {
        float xScale = xAxis.axisRange / (minXRange)
        viewPortHandler.setMaximumScaleX(xScale)
    }

    /**
     * Limits the maximum and minimum x range that can be visible by pinching and zooming. e.g. minRange=10, maxRange=100 the
     * smallest range to be displayed at once is 10, and no more than a range of 100 values can be viewed at once without
     * scrolling
     *
     * @param minXRange
     * @param maxXRange
     */
    fun setVisibleXRange(minXRange: Float, float maxXRange) {
        float minScale = xAxis.axisRange / minXRange
        val maxScale = xAxis.axisRange / maxXRange
        viewPortHandler.setMinMaxScaleX(minScale, maxScale)
    }

    /**
     * Sets the size of the area (range on the y-axis) that should be maximum
     * visible at once.
     *
     * @param maxYRange the maximum visible range on the y-axis
     * @param axis      the axis for which this limit should apply
     */
    fun setVisibleYRangeMaximum(maxYRange: Float, AxisDependency axis) {
        float yScale = getAxisRange(axis) / maxYRange
        viewPortHandler.setMinimumScaleY(yScale)
    }

    /**
     * Sets the size of the area (range on the y-axis) that should be minimum visible at once, no further zooming in possible.
     *
     * @param minYRange
     * @param axis      the axis for which this limit should apply
     */
    fun setVisibleYRangeMinimum(minYRange: Float, AxisDependency axis) {
        float yScale = getAxisRange(axis) / minYRange
        viewPortHandler.setMaximumScaleY(yScale)
    }

    /**
     * Limits the maximum and minimum y range that can be visible by pinching and zooming.
     *
     * @param minYRange
     * @param maxYRange
     * @param axis
     */
    fun setVisibleYRange(minYRange: Float, float maxYRange, AxisDependency axis) {
        float minScale = getAxisRange(axis) / minYRange
        val maxScale = getAxisRange(axis) / maxYRange
        viewPortHandler.setMinMaxScaleY(minScale, maxScale)
    }


    /**
     * Moves the left side of the current viewport to the specified x-position.
     * This also refreshes the chart by calling invalidate().
     *
     * @param xValue
     */
    fun moveViewToX(xValue: Float) {

        Runnable job = MoveViewJob.getInstance(viewPortHandler, xValue, 0f,
                getTransformer(AxisDependency.LEFT), this)

        addViewportJob(job)
    }

    /**
     * This will move the left side of the current viewport to the specified
     * x-value on the x-axis, and center the viewport to the specified y value on the y-axis.
     * This also refreshes the chart by calling invalidate().
     *
     * @param xValue
     * @param yValue
     * @param axis   - which axis should be used as a reference for the y-axis
     */
    fun moveViewTo(xValue: Float, float yValue, AxisDependency axis) {

        float yInView = getAxisRange(axis) / viewPortHandler.getScaleY()

        Runnable job = MoveViewJob.getInstance(viewPortHandler, xValue, yValue + yInView / 2f,
                getTransformer(axis), this)

        addViewportJob(job)
    }

    /**
     * This will move the left side of the current viewport to the specified x-value
     * and center the viewport to the y value animated.
     * This also refreshes the chart by calling invalidate().
     *
     * @param xValue
     * @param yValue
     * @param axis
     * @param duration the duration of the animation in milliseconds
     */
    @TargetApi(11)
    fun moveViewToAnimated(xValue: Float, float yValue, AxisDependency axis, duration: Long) {

        MPPointD bounds = getValuesByTouchPoint(viewPortHandler.contentLeft(), viewPortHandler.contentTop(), axis)

        float yInView = getAxisRange(axis) / viewPortHandler.getScaleY()

        Runnable job = AnimatedMoveViewJob.getInstance(viewPortHandler, xValue, yValue + yInView / 2f,
                getTransformer(axis), this, (float) bounds.x, (float) bounds.y, duration)

        addViewportJob(job)

        MPPointD.recycleInstance(bounds)
    }

    /**
     * Centers the viewport to the specified y value on the y-axis.
     * This also refreshes the chart by calling invalidate().
     *
     * @param yValue
     * @param axis   - which axis should be used as a reference for the y-axis
     */
    fun centerViewToY(yValue: Float, AxisDependency axis) {

        float valsInView = getAxisRange(axis) / viewPortHandler.getScaleY()

        Runnable job = MoveViewJob.getInstance(viewPortHandler, 0f, yValue + valsInView / 2f,
                getTransformer(axis), this)

        addViewportJob(job)
    }

    /**
     * This will move the center of the current viewport to the specified
     * x and y value.
     * This also refreshes the chart by calling invalidate().
     *
     * @param xValue
     * @param yValue
     * @param axis   - which axis should be used as a reference for the y axis
     */
    fun centerViewTo(xValue: Float, float yValue, AxisDependency axis) {

        float yInView = getAxisRange(axis) / viewPortHandler.getScaleY()
        val xInView = getXAxis().axisRange / viewPortHandler.getScaleX()

        Runnable job = MoveViewJob.getInstance(viewPortHandler,
                xValue - xInView / 2f, yValue + yInView / 2f,
                getTransformer(axis), this)

        addViewportJob(job)
    }

    /**
     * This will move the center of the current viewport to the specified
     * x and y value animated.
     *
     * @param xValue
     * @param yValue
     * @param axis
     * @param duration the duration of the animation in milliseconds
     */
    @TargetApi(11)
    fun centerViewToAnimated(xValue: Float, float yValue, AxisDependency axis, duration: Long) {

        MPPointD bounds = getValuesByTouchPoint(viewPortHandler.contentLeft(), viewPortHandler.contentTop(), axis)

        float yInView = getAxisRange(axis) / viewPortHandler.getScaleY()
        val xInView = getXAxis().axisRange / viewPortHandler.getScaleX()

        Runnable job = AnimatedMoveViewJob.getInstance(viewPortHandler,
                xValue - xInView / 2f, yValue + yInView / 2f,
                getTransformer(axis), this, (float) bounds.x, (float) bounds.y, duration)

        addViewportJob(job)

        MPPointD.recycleInstance(bounds)
    }

    /**
     * flag that indicates if a custom viewport offset has been set
     */
    private boolean isCustomViewPortEnabled = false

    /**
     * Sets custom offsets for the current ViewPort (the offsets on the sides of
     * the actual chart window). Setting this will prevent the chart from
     * automatically calculating it's offsets. Use resetViewPortOffsets() to
     * undo this. ONLY USE THIS WHEN YOU KNOW WHAT YOU ARE DOING, else use
     * setExtraOffsets(...).
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    fun setViewPortOffsets(left: Float, top: Float,
                                   final float right, final float bottom) {

        isCustomViewPortEnabled = true
        post(Runnable() {

            @Override
            fun run() {

                viewPortHandler.restrainViewPort(left, top, right, bottom)
                prepareOffsetMatrix()
                prepareValuePxMatrix()
            }
        })
    }

    /**
     * Resets all custom offsets set via setViewPortOffsets(...) method. Allows
     * the chart to again calculate all offsets automatically.
     */
    fun resetViewPortOffsets() {
        isCustomViewPortEnabled = false
        calculateOffsets()
    }

    /**
     * ################ ################ ################ ################
     */
    /** CODE BELOW IS GETTERS AND SETTERS */

    /**
     * Returns the range of the specified axis.
     *
     * @param axis
     * @return
     */
    protected float getAxisRange(AxisDependency axis) {
        if (axis == AxisDependency.LEFT)
            return axisLeft.axisRange
        else
            return axisRight.axisRange
    }

    /**
     * Sets the OnDrawListener
     *
     * @param drawListener
     */
    fun setOnDrawListener(OnDrawListener drawListener) {
        this.mDrawListener = drawListener
    }

    /**
     * Gets the OnDrawListener. May be null.
     *
     * @return
     */
    public OnDrawListener getDrawListener() {
        return mDrawListener
    }

    protected float[] mGetPositionBuffer = new float[2]

    /**
     * Returns a recyclable MPPointF instance.
     * Returns the position (in pixels) the provided Entry has inside the chart
     * view or null, if the provided Entry is null.
     *
     * @param e
     * @return
     */
    public MPPointF getPosition(Entry e, AxisDependency axis) {

        if (e == null)
            return null

        mGetPositionBuffer[0] = e.getX()
        mGetPositionBuffer[1] = e.getY()

        getTransformer(axis).pointValuesToPixel(mGetPositionBuffer)

        return MPPointF.getInstance(mGetPositionBuffer[0], mGetPositionBuffer[1])
    }

    /**
     * sets the number of maximum visible drawn values on the chart only active
     * when setDrawValues() is enabled
     *
     * @param count
     */
    fun setMaxVisibleValueCount(count: Int) {
        this.mMaxVisibleCount = count
    }

    public val getMaxVisibleCount() {
        return mMaxVisibleCount
    }

    /**
     * Set this to true to allow highlighting per dragging over the chart
     * surface when it is fully zoomed out. Default: true
     *
     * @param enabled
     */
    fun setHighlightPerDragEnabled(boolean enabled) {
        mHighlightPerDragEnabled = enabled
    }

    fun isHighlightPerDragEnabled() {
        return mHighlightPerDragEnabled
    }

    /**
     * Sets the color for the background of the chart-drawing area (everything
     * behind the grid lines).
     *
     * @param color
     */
    fun setGridBackgroundColor(color: Int) {
        gridBackgroundPaint.setColor(color)
    }

    /**
     * Set this to true to enable dragging (moving the chart with the finger)
     * for the chart (this does not effect scaling).
     *
     * @param enabled
     */
    fun setDragEnabled(boolean enabled) {
        this.mDragXEnabled = enabled
        this.mDragYEnabled = enabled
    }

    /**
     * Returns true if dragging is enabled for the chart, false if not.
     *
     * @return
     */
    fun isDragEnabled() {
        return mDragXEnabled || mDragYEnabled
    }

    /**
     * Set this to true to enable dragging on the X axis
     *
     * @param enabled
     */
    fun setDragXEnabled(boolean enabled) {
        this.mDragXEnabled = enabled
    }

    /**
     * Returns true if dragging on the X axis is enabled for the chart, false if not.
     *
     * @return
     */
    fun isDragXEnabled() {
        return mDragXEnabled
    }

    /**
     * Set this to true to enable dragging on the Y axis
     *
     * @param enabled
     */
    fun setDragYEnabled(boolean enabled) {
        this.mDragYEnabled = enabled
    }

    /**
     * Returns true if dragging on the Y axis is enabled for the chart, false if not.
     *
     * @return
     */
    fun isDragYEnabled() {
        return mDragYEnabled
    }

    /**
     * Set this to true to enable scaling (zooming in and out by gesture) for
     * the chart (this does not effect dragging) on both X- and Y-Axis.
     *
     * @param enabled
     */
    fun setScaleEnabled(boolean enabled) {
        this.mScaleXEnabled = enabled
        this.mScaleYEnabled = enabled
    }

    fun setScaleXEnabled(boolean enabled) {
        mScaleXEnabled = enabled
    }

    fun setScaleYEnabled(boolean enabled) {
        mScaleYEnabled = enabled
    }

    fun isScaleXEnabled() {
        return mScaleXEnabled
    }

    fun isScaleYEnabled() {
        return mScaleYEnabled
    }

    /**
     * Set this to true to enable zooming in by double-tap on the chart.
     * Default: enabled
     *
     * @param enabled
     */
    fun setDoubleTapToZoomEnabled(boolean enabled) {
        mDoubleTapToZoomEnabled = enabled
    }

    /**
     * Returns true if zooming via double-tap is enabled false if not.
     *
     * @return
     */
    fun isDoubleTapToZoomEnabled() {
        return mDoubleTapToZoomEnabled
    }

    /**
     * set this to true to draw the grid background, false if not
     *
     * @param enabled
     */
    fun setDrawGridBackground(boolean enabled) {
        isDrawGridBackgroundEnabled = enabled
    }

    /**
     * When enabled, the borders rectangle will be rendered.
     * If this is enabled, there is no point drawing the axis-lines of x- and y-axis.
     *
     * @param enabled
     */
    fun setDrawBorders(boolean enabled) {
        isDrawBordersEnabled = enabled
    }

    /**
     * When enabled, the borders rectangle will be rendered.
     * If this is enabled, there is no point drawing the axis-lines of x- and y-axis.
     *
     * @return
     */
    fun isDrawBordersEnabled() {
        return isDrawBordersEnabled
    }

    /**
     * When enabled, the values will be clipped to contentRect,
     * otherwise they can bleed outside the content rect.
     *
     * @param enabled
     */
    fun setClipValuesToContent(boolean enabled) {
        isClipValuesToContentEnabled = enabled
    }

    /**
     * When enabled, the values will be clipped to contentRect,
     * otherwise they can bleed outside the content rect.
     *
     * @return
     */
    fun isClipValuesToContentEnabled() {
        return isClipValuesToContentEnabled
    }

    /**
     * Sets the width of the border lines in dp.
     *
     * @param width
     */
    fun setBorderWidth(width: Float) {
        borderPaint.setStrokeWidth(Utils.convertDpToPixel(width))
    }

    /**
     * Sets the color of the chart border lines.
     *
     * @param color
     */
    fun setBorderColor(color: Int) {
        borderPaint.setColor(color)
    }

    /**
     * Gets the minimum offset (padding) around the chart, defaults to 15.f
     */
    fun getMinOffset() {
        return mMinOffset
    }

    /**
     * Sets the minimum offset (padding) around the chart, defaults to 15.f
     */
    fun setMinOffset(float minOffset) {
        mMinOffset = minOffset
    }

    /**
     * Returns true if keeping the position on rotation is enabled and false if not.
     */
    fun isKeepPositionOnRotation() {
        return mKeepPositionOnRotation
    }

    /**
     * Sets whether the chart should keep its position (zoom / scroll) after a rotation (orientation change)
     */
    fun setKeepPositionOnRotation(boolean keepPositionOnRotation) {
        mKeepPositionOnRotation = keepPositionOnRotation
    }

    /**
     * Returns a recyclable MPPointD instance
     * Returns the x and y values in the chart at the given touch point
     * (encapsulated in a MPPointD). This method transforms pixel coordinates to
     * coordinates / values in the chart. This is the opposite method to
     * getPixelForValues(...).
     *
     * @param x
     * @param y
     * @return
     */
    public MPPointD getValuesByTouchPoint(x: Float, y: Float, AxisDependency axis) {
        MPPointD result = MPPointD.getInstance(0, 0)
        getValuesByTouchPoint(x, y, axis, result)
        return result
    }

    fun getValuesByTouchPoint(x: Float, y: Float, AxisDependency axis, MPPointD outputPoint) {
        getTransformer(axis).getValuesByTouchPoint(x, y, outputPoint)
    }

    /**
     * Returns a recyclable MPPointD instance
     * Transforms the given chart values into pixels. This is the opposite
     * method to getValuesByTouchPoint(...).
     *
     * @param x
     * @param y
     * @return
     */
    public MPPointD getPixelForValues(float x, float y, AxisDependency axis) {
        return getTransformer(axis).getPixelForValues(x, y)
    }

    /**
     * returns the Entry object displayed at the touched position of the chart
     *
     * @param x
     * @param y
     * @return
     */
    public Entry getEntryByTouchPoint(float x, float y) {
        Highlight h = getHighlightByTouchPoint(x, y)
        h?.let { {
            return data.getEntryForHighlight(h)
        }
        return null
    }

    /**
     * returns the DataSet object displayed at the touched position of the chart
     *
     * @param x
     * @param y
     * @return
     */
    public IBarLineScatterCandleBubbleDataSet getDataSetByTouchPoint(x: Float, y: Float) {
        Highlight h = getHighlightByTouchPoint(x, y)
        if (h != null) {
            return data.getDataSetByIndex(h.getDataSetIndex())
        }
        return null
    }

    /**
     * buffer for storing lowest visible x point
     */
    protected MPPointD posForGetLowestVisibleX = MPPointD.getInstance(0, 0) }

    /**
     * Returns the lowest x-index (value on the x-axis) that is still visible on
     * the chart.
     *
     * @return
     */
    @Override
    public val getLowestVisibleX() {
        getTransformer(AxisDependency.LEFT).getValuesByTouchPoint(viewPortHandler.contentLeft(),
                viewPortHandler.contentBottom(), posForGetLowestVisibleX)
        float result = (float) kotlin.math.max(xAxis.axisMinimum, posForGetLowestVisibleX.x)
        return result
    }

    /**
     * buffer for storing highest visible x point
     */
    protected MPPointD posForGetHighestVisibleX = MPPointD.getInstance(0, 0)

    /**
     * Returns the highest x-index (value on the x-axis) that is still visible
     * on the chart.
     *
     * @return
     */
    @Override
    public val getHighestVisibleX() {
        getTransformer(AxisDependency.LEFT).getValuesByTouchPoint(viewPortHandler.contentRight(),
                viewPortHandler.contentBottom(), posForGetHighestVisibleX)
        float result = (float) kotlin.math.min(xAxis.axisMaximum, posForGetHighestVisibleX.x)
        return result
    }

    /**
     * Returns the range visible on the x-axis.
     *
     * @return
     */
    fun getVisibleXRange() {
        return Math.abs(getHighestVisibleX() - getLowestVisibleX())
    }

    /**
     * returns the current x-scale factor
     */
    fun getScaleX() {
        if (viewPortHandler == null)
            return 1f
        else
            return viewPortHandler.getScaleX()
    }

    /**
     * returns the current y-scale factor
     */
    fun getScaleY() {
        if (viewPortHandler == null)
            return 1f
        else
            return viewPortHandler.getScaleY()
    }

    /**
     * if the chart is fully zoomed out, return true
     *
     * @return
     */
    fun isFullyZoomedOut() {
        return viewPortHandler.isFullyZoomedOut()
    }

    /**
     * Returns the left y-axis object. In the horizontal bar-chart, this is the
     * top axis.
     *
     * @return
     */
    public YAxis getAxisLeft() {
        return axisLeft
    }

    /**
     * Returns the right y-axis object. In the horizontal bar-chart, this is the
     * bottom axis.
     *
     * @return
     */
    public YAxis getAxisRight() {
        return axisRight
    }

    /**
     * Returns the y-axis object to the corresponding AxisDependency. In the
     * horizontal bar-chart, LEFT == top, RIGHT == BOTTOM
     *
     * @param axis
     * @return
     */
    public YAxis getAxis(AxisDependency axis) {
        if (axis == AxisDependency.LEFT)
            return axisLeft
        else
            return axisRight
    }

    @Override
    fun isInverted(AxisDependency axis) {
        return getAxis(axis).isInverted
    }

    /**
     * If set to true, both x and y axis can be scaled simultaneously with 2 fingers, if false,
     * x and y axis can be scaled separately. default: false
     *
     * @param enabled
     */
    fun setPinchZoom(boolean enabled) {
        mPinchZoomEnabled = enabled
    }

    /**
     * returns true if pinch-zoom is enabled, false if not
     *
     * @return
     */
    fun isPinchZoomEnabled() {
        return mPinchZoomEnabled
    }

    /**
     * Set an offset in dp that allows the user to drag the chart over it's
     * bounds on the x-axis.
     *
     * @param offset
     */
    fun setDragOffsetX(offset: Float) {
        viewPortHandler.setDragOffsetX(offset)
    }

    /**
     * Set an offset in dp that allows the user to drag the chart over it's
     * bounds on the y-axis.
     *
     * @param offset
     */
    fun setDragOffsetY(float offset) {
        viewPortHandler.setDragOffsetY(offset)
    }

    /**
     * Returns true if both drag offsets (x and y) are zero or smaller.
     *
     * @return
     */
    fun hasNoDragOffset() {
        return viewPortHandler.hasNoDragOffset()
    }

    public XAxisRenderer getRendererXAxis() {
        return xAxisRenderer
    }

    /**
     * Sets a custom XAxisRenderer and overrides the existing (default) one.
     *
     * @param xAxisRenderer
     */
    fun setXAxisRenderer(XAxisRenderer xAxisRenderer) {
        xAxisRenderer = xAxisRenderer
    }

    public YAxisRenderer getRendererLeftYAxis() {
        return axisRendererLeft
    }

    /**
     * Sets a custom axis renderer for the left axis and overwrites the existing one.
     *
     * @param rendererLeftYAxis
     */
    fun setRendererLeftYAxis(YAxisRenderer rendererLeftYAxis) {
        axisRendererLeft = rendererLeftYAxis
    }

    public YAxisRenderer getRendererRightYAxis() {
        return axisRendererRight
    }

    /**
     * Sets a custom axis renderer for the right acis and overwrites the existing one.
     *
     * @param rendererRightYAxis
     */
    fun setRendererRightYAxis(YAxisRenderer rendererRightYAxis) {
        axisRendererRight = rendererRightYAxis
    }

    @Override
    public val getYChartMax() {
        return kotlin.math.max(axisLeft.axisMaximum, axisRight.axisMaximum)
    }

    @Override
    fun getYChartMin() {
        return kotlin.math.min(axisLeft.axisMinimum, axisRight.axisMinimum)
    }

    /**
     * Returns true if either the left or the right or both axes are inverted.
     *
     * @return
     */
    fun isAnyAxisInverted() {
        if (axisLeft.isInverted)
            return true
        if (axisRight.isInverted)
            return true
        return false
    }

    /**
     * Flag that indicates if auto scaling on the y axis is enabled. This is
     * especially interesting for charts displaying financial data.
     *
     * @param enabled the y axis automatically adjusts to the min and max y
     *                values of the current x axis range whenever the viewport
     *                changes
     */
    fun setAutoScaleMinMaxEnabled(boolean enabled) {
        isAutoScaleMinMaxEnabled = enabled
    }

    /**
     * @return true if auto scaling on the y axis is enabled.
     * @default false
     */
    fun isAutoScaleMinMaxEnabled() {
        return isAutoScaleMinMaxEnabled
    }

    override fun setPaint(p: Paint, which: Int) {
        super.setPaint(p, which)

        when (which) {
            PAINT_GRID_BACKGROUND -> gridBackgroundPaint = p
        }
    }

    override fun getPaint(which: Int): Paint? {
        val p = super.getPaint(which)
        if (p != null) return p

        when (which) {
            PAINT_GRID_BACKGROUND -> return gridBackgroundPaint
        }

        return null
    }

    protected val onSizeChangedBuffer = floatArrayOf(0f, 0f)

    @Override
    protected void onSizeChanged(val w, int h, int oldw, int oldh) {

        // Saving current position of chart.
        mOnSizeChangedBuffer[0] = mOnSizeChangedBuffer[1] = 0

        if (mKeepPositionOnRotation) {
            mOnSizeChangedBuffer[0] = viewPortHandler.contentLeft()
            mOnSizeChangedBuffer[1] = viewPortHandler.contentTop()
            getTransformer(AxisDependency.LEFT).pixelsToValue(mOnSizeChangedBuffer)
        }

        //Superclass transforms chart.
        super.onSizeChanged(w, h, oldw, oldh)

        if (mKeepPositionOnRotation) {

            //Restoring old position of chart.
            getTransformer(AxisDependency.LEFT).pointValuesToPixel(mOnSizeChangedBuffer)
            viewPortHandler.centerViewPort(mOnSizeChangedBuffer, this)
        } else {
            viewPortHandler.refresh(viewPortHandler.matrixTouch, this, true)
        }
    }
}
