package com.github.mikephil.charting.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.highlight.PieHighlighter
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import kotlin.math.*

/**
 * View that represents a pie chart. Draws cake like slices.
 *
 * @author Philipp Jahoda
 */
class PieChart : PieRadarChartBase<PieData> {

    /**
     * rect object that represents the bounds of the piechart, needed for
     * drawing the circle
     */
    private val mCircleBox = RectF()

    /**
     * flag indicating if entry labels should be drawn or not
     */
    private var mDrawEntryLabels = true

    /**
     * array that holds the width of each pie-slice in degrees
     */
    private var mDrawAngles = FloatArray(1)

    /**
     * array that holds the absolute angle in degrees of each slice
     */
    private var mAbsoluteAngles = FloatArray(1)

    /**
     * if true, the white hole inside the chart will be drawn
     */
    private var mDrawHole = true

    /**
     * if true, the hole will see-through to the inner tips of the slices
     */
    private var mDrawSlicesUnderHole = false

    /**
     * if true, the values inside the piechart are drawn as percent values
     */
    private var mUsePercentValues = false

    /**
     * if true, the slices of the piechart are rounded
     */
    private var mDrawRoundedSlices = false

    /**
     * variable for the text that is drawn in the center of the pie-chart
     */
    private var mCenterText: CharSequence = ""

    private val mCenterTextOffset = MPPointF.getInstance(0f, 0f)

    /**
     * indicates the size of the hole in the center of the piechart, default:
     * radius / 2
     */
    private var mHoleRadiusPercent = 50f

    /**
     * the radius of the transparent circle next to the chart-hole in the center
     */
    private var mTransparentCircleRadiusPercent = 55f

    /**
     * if enabled, centertext is drawn
     */
    private var mDrawCenterText = true

    private var mCenterTextRadiusPercent = 100f

    private var mMaxAngle = 360f

    /**
     * Minimum angle to draw slices, this only works if there is enough room for all slices to have
     * the minimum angle, default 0f.
     */
    private var mMinAngleForSlices = 0f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun init() {
        super.init()
        mRenderer = PieChartRenderer(this, mAnimator, mViewPortHandler)
        mXAxis = null
        mHighlighter = PieHighlighter(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mData == null) return

        mRenderer.drawData(canvas)

        if (valuesToHighlight()) {
            mRenderer.drawHighlighted(canvas, mIndicesToHighlight)
        }

        mRenderer.drawExtras(canvas)
        mRenderer.drawValues(canvas)
        mLegendRenderer.renderLegend(canvas)
        drawDescription(canvas)
        drawMarkers(canvas)
    }

    override fun calculateOffsets() {
        super.calculateOffsets()

        // prevent nullpointer when no data set
        if (mData == null) return

        val diameter = getDiameter()
        val radius = diameter / 2f
        val c = centerOffsets
        val shift = mData.dataSet.selectionShift

        // create the circle box that will contain the pie-chart (the bounds of the pie-chart)
        mCircleBox.set(
            c.x - radius + shift,
            c.y - radius + shift,
            c.x + radius - shift,
            c.y + radius - shift
        )

        MPPointF.recycleInstance(c)
    }

    override fun calcMinMax() {
        calcAngles()
    }

    override fun getMarkerPosition(highlight: Highlight): FloatArray {
        val center = getCenterCircleBox()
        var r = getRadius()
        var off = r / 10f * 3.6f

        if (isDrawHoleEnabled()) {
            off = (r - (r / 100f * getHoleRadius())) / 2f
        }

        r -= off // offset to keep things inside the chart

        val rotationAngle = getRotationAngle()
        val entryIndex = highlight.x.toInt()

        // offset needed to center the drawn text in the slice
        val offset = mDrawAngles[entryIndex] / 2

        // calculate the text position
        val x = (r * cos(Math.toRadians((rotationAngle + mAbsoluteAngles[entryIndex] - offset) * mAnimator.phaseY.toDouble())) + center.x).toFloat()
        val y = (r * sin(Math.toRadians((rotationAngle + mAbsoluteAngles[entryIndex] - offset) * mAnimator.phaseY.toDouble())) + center.y).toFloat()

        MPPointF.recycleInstance(center)
        return floatArrayOf(x, y)
    }

    /**
     * calculates the needed angles for the chart slices
     */
    private fun calcAngles() {
        val entryCount = mData.entryCount

        if (mDrawAngles.size != entryCount) {
            mDrawAngles = FloatArray(entryCount)
        } else {
            mDrawAngles.fill(0f)
        }

        if (mAbsoluteAngles.size != entryCount) {
            mAbsoluteAngles = FloatArray(entryCount)
        } else {
            mAbsoluteAngles.fill(0f)
        }

        val yValueSum = mData.yValueSum
        val dataSets = mData.dataSets

        val hasMinAngle = mMinAngleForSlices != 0f && entryCount * mMinAngleForSlices <= mMaxAngle
        val minAngles = FloatArray(entryCount)

        var cnt = 0
        var offset = 0f
        var diff = 0f

        for (i in 0 until mData.dataSetCount) {
            val set = dataSets[i]

            for (j in 0 until set.entryCount) {
                var drawAngle = calcAngle(abs(set.getEntryForIndex(j).y), yValueSum)

                if (hasMinAngle) {
                    val temp = drawAngle - mMinAngleForSlices
                    if (temp <= 0) {
                        minAngles[cnt] = mMinAngleForSlices
                        offset += -temp
                    } else {
                        minAngles[cnt] = drawAngle
                        diff += temp
                    }
                }

                mDrawAngles[cnt] = drawAngle

                if (cnt == 0) {
                    mAbsoluteAngles[cnt] = mDrawAngles[cnt]
                } else {
                    mAbsoluteAngles[cnt] = mAbsoluteAngles[cnt - 1] + mDrawAngles[cnt]
                }

                cnt++
            }
        }

        if (hasMinAngle) {
            // Correct for minimum angle requirements
            for (i in 0 until entryCount) {
                minAngles[i] -= (offset * (minAngles[i] - mMinAngleForSlices)) / diff
                if (i == 0) {
                    mAbsoluteAngles[0] = minAngles[0]
                } else {
                    mAbsoluteAngles[i] = mAbsoluteAngles[i - 1] + minAngles[i]
                }
                mDrawAngles[i] = minAngles[i]
            }
        }
    }

    /**
     * Checks if the given index is set to be highlighted.
     *
     * @param index
     * @return
     */
    fun needsHighlight(index: Int): Boolean {
        if (!valuesToHighlight()) return false

        for (highlight in mIndicesToHighlight) {
            if (highlight.x.toInt() == index) return true
        }
        return false
    }

    /**
     * calculates the needed angle for a given value
     *
     * @param value
     * @param yValueSum
     * @return
     */
    private fun calcAngle(value: Float, yValueSum: Float): Float {
        return value / yValueSum * mMaxAngle
    }

    override fun getIndexForAngle(angle: Float): Int {
        // take the current angle of the chart into consideration
        val a = Utils.getNormalizedAngle(angle - getRotationAngle())

        for (i in mAbsoluteAngles.indices) {
            if (mAbsoluteAngles[i] > a) return i
        }

        return -1 // return -1 if no index found
    }

    /**
     * Returns the index of the DataSet this x-index belongs to.
     *
     * @param xIndex
     * @return
     */
    fun getDataSetIndexForIndex(xIndex: Int): Int {
        val dataSets = mData.dataSets
        var cnt = 0

        for (i in dataSets.indices) {
            val set = dataSets[i]

            for (j in 0 until set.entryCount) {
                if (cnt == xIndex) return i
                cnt++
            }
        }

        return -1
    }

    /**
     * returns an integer array of all the different angles the chart slices
     * have the angles in the returned array determine how much space (of 360°)
     * each slice takes
     *
     * @return
     */
    fun getDrawAngles(): FloatArray = mDrawAngles

    /**
     * returns the absolute angles of the different chart slices (where the
     * slices end)
     *
     * @return
     */
    fun getAbsoluteAngles(): FloatArray = mAbsoluteAngles

    /**
     * Set the color for the hole that is drawn in the center of the PieChart
     * (if enabled).
     *
     * @param color
     */
    fun setHoleColor(color: Int) {
        (mRenderer as PieChartRenderer).paintHole.color = color
    }

    /**
     * Enable or disable the visibility of slices under the hole.
     *
     * @param enable
     */
    fun setDrawSlicesUnderHole(enable: Boolean) {
        mDrawSlicesUnderHole = enable
    }

    /**
     * returns true if the drawing of slices under the hole is enabled, false if not
     *
     * @return
     */
    fun isDrawSlicesUnderHoleEnabled(): Boolean = mDrawSlicesUnderHole

    /**
     * Set this to true to draw the pie center empty.
     *
     * @param enabled
     */
    fun setDrawHoleEnabled(enabled: Boolean) {
        mDrawHole = enabled
    }

    /**
     * returns true if the hole in the center of the pie-chart is set to be
     * visible, false if not
     *
     * @return
     */
    fun isDrawHoleEnabled(): Boolean = mDrawHole

    /**
     * Sets the text that is displayed in the center of the pie-chart. If set to
     * null, the text is removed.
     *
     * @param text
     */
    fun setCenterText(text: CharSequence?) {
        mCenterText = text ?: ""
    }

    /**
     * returns the text that is drawn in the center of the pie-chart
     *
     * @return
     */
    fun getCenterText(): CharSequence = mCenterText

    /**
     * set this to true to draw the text that is displayed in the center of the
     * pie chart
     *
     * @param enabled
     */
    fun setDrawCenterText(enabled: Boolean) {
        mDrawCenterText = enabled
    }

    /**
     * returns true if drawing the center text is enabled
     *
     * @return
     */
    fun isDrawCenterTextEnabled(): Boolean = mDrawCenterText

    override fun getRadius(): Float {
        val c = mCircleBox
        return min(c.width() / 2f, c.height() / 2f)
    }

    /**
     * Returns the circlebox, the boundingbox of the pie-chart slices
     *
     * @return
     */
    fun getCircleBox(): RectF = mCircleBox

    /**
     * Returns the center of the circlebox
     *
     * @return
     */
    fun getCenterCircleBox(): MPPointF {
        return MPPointF.getInstance(mCircleBox.centerX(), mCircleBox.centerY())
    }

    /**
     * Sets the typeface for the center-text paint
     *
     * @param t
     */
    fun setCenterTextTypeface(t: Typeface?) {
        (mRenderer as PieChartRenderer).paintCenterText.typeface = t
    }

    /**
     * Gets the typeface for the center-text paint
     *
     * @return
     */
    fun getCenterTextTypeface(): Typeface? {
        return (mRenderer as PieChartRenderer).paintCenterText.typeface
    }

    /**
     * Sets the size of the center text of the PieChart in dp.
     *
     * @param sizeDp
     */
    fun setCenterTextSize(sizeDp: Float) {
        (mRenderer as PieChartRenderer).paintCenterText.textSize = Utils.convertDpToPixel(sizeDp)
    }

    /**
     * Gets the size of the center text of the PieChart in dp.
     *
     * @return
     */
    fun getCenterTextSize(): Float {
        return Utils.convertPixelsToDp((mRenderer as PieChartRenderer).paintCenterText.textSize)
    }

    /**
     * Sets the color of the center text of the PieChart.
     *
     * @param color
     */
    fun setCenterTextColor(color: Int) {
        (mRenderer as PieChartRenderer).paintCenterText.color = color
    }

    /**
     * Gets the color of the center text of the PieChart.
     *
     * @return
     */
    fun getCenterTextColor(): Int {
        return (mRenderer as PieChartRenderer).paintCenterText.color
    }

    /**
     * sets the radius of the hole in the center of the piechart in percent of
     * the maximum radius (max = the radius of the whole chart), default 50%
     *
     * @param percent
     */
    fun setHoleRadius(percent: Float) {
        mHoleRadiusPercent = percent
    }

    /**
     * Returns the size of the hole radius in percent of the total radius.
     *
     * @return
     */
    fun getHoleRadius(): Float = mHoleRadiusPercent

    /**
     * Sets the color of the transparent circle that is drawn next to the hole
     * in the piechart in percent of the maximum radius (max = the radius of
     * the whole chart), default 55% -> means 5% larger than the center-hole by
     * default
     *
     * @param percent
     */
    fun setTransparentCircleColor(color: Int) {
        (mRenderer as PieChartRenderer).paintTransparentCircle.color = color
    }

    /**
     * sets the radius of the transparent circle that is drawn next to the hole
     * in the piechart in percent of the maximum radius (max = the radius of
     * the whole chart), default 55% -> means 5% larger than the center-hole by
     * default
     *
     * @param percent
     */
    fun setTransparentCircleRadius(percent: Float) {
        mTransparentCircleRadiusPercent = percent
    }

    fun getTransparentCircleRadius(): Float = mTransparentCircleRadiusPercent

    /**
     * Sets the amount of transparency the transparent circle should have 0 = fully transparent,
     * 255 = fully opaque.
     * Default value is 100.
     *
     * @param alpha 0-255
     */
    fun setTransparentCircleAlpha(alpha: Int) {
        (mRenderer as PieChartRenderer).paintTransparentCircle.alpha = alpha
    }

    /**
     * set this to true to draw entry labels into the pie slices
     *
     * @param enabled
     */
    fun setDrawEntryLabels(enabled: Boolean) {
        mDrawEntryLabels = enabled
    }

    /**
     * returns true if drawing entry labels is enabled, false if not
     *
     * @return
     */
    fun isDrawEntryLabelsEnabled(): Boolean = mDrawEntryLabels

    /**
     * Sets the color the entry labels are drawn with.
     *
     * @param color
     */
    fun setEntryLabelColor(color: Int) {
        (mRenderer as PieChartRenderer).paintEntryLabels.color = color
    }

    /**
     * Sets the typeface used for drawing entry labels.
     *
     * @param tf
     */
    fun setEntryLabelTypeface(tf: Typeface?) {
        (mRenderer as PieChartRenderer).paintEntryLabels.typeface = tf
    }

    /**
     * Sets the size of the entry labels in dp. Default: 13dp
     *
     * @param size
     */
    fun setEntryLabelTextSize(size: Float) {
        (mRenderer as PieChartRenderer).paintEntryLabels.textSize = Utils.convertDpToPixel(size)
    }

    /**
     * If this is enabled, values inside the PieChart are drawn in percent and
     * not with their original value. Values provided for the IValueFormatter to format
     * are then provided in percent.
     *
     * @param enabled
     */
    fun setUsePercentValues(enabled: Boolean) {
        mUsePercentValues = enabled
    }

    /**
     * returns true if drawing values in percent is enabled
     *
     * @return
     */
    fun isUsePercentValuesEnabled(): Boolean = mUsePercentValues

    /**
     * the rectangular radius of the bounding box for the center text, as a percentage of the pie
     * hole default 1.f (100%)
     *
     * @param radiusPercent
     */
    fun setCenterTextRadiusPercent(radiusPercent: Float) {
        mCenterTextRadiusPercent = radiusPercent
    }

    /**
     * Gets the rectangular radius of the bounding box for the center text, as a percentage of the pie hole
     *
     * @return
     */
    fun getCenterTextRadiusPercent(): Float = mCenterTextRadiusPercent

    override fun getDiameter(): Float {
        val content = mViewPortHandler.contentRect
        return min(content.width(), content.height())
    }

    /**
     * Returns the maximum angle the chart can use for drawing. 360f means the
     * chart takes the whole circle, 180f means it takes half the circle.
     *
     * @return
     */
    fun getMaxAngle(): Float = mMaxAngle

    /**
     * Sets the max angle that is used for calculating the pie-circle. 360f means
     * it's a full PieChart, 180f results in a half-pie-chart. Default: 360f
     *
     * @param maxangle min 90, max 360
     */
    fun setMaxAngle(maxangle: Float) {
        var angle = maxangle
        if (angle > 360) angle = 360f
        if (angle < 90) angle = 90f
        mMaxAngle = angle
    }

    /**
     * The minimum angle slices on the chart are rendered with, default is 0f.
     *
     * @return minimum angle for slices
     */
    fun getMinAngleForSlices(): Float = mMinAngleForSlices

    /**
     * Set the angle to set minimum size for slices, you must call [notifyDataSetChanged]
     * and [invalidate] when changing this, only works if there is enough room for all
     * slices to have the minimum angle.
     *
     * @param minAngle minimum 0, maximum is half of [setMaxAngle]
     */
    fun setMinAngleForSlices(minAngle: Float) {
        var angle = minAngle
        if (angle > (mMaxAngle / 2f)) angle = mMaxAngle / 2f
        else if (angle < 0) angle = 0f
        mMinAngleForSlices = angle
    }

    override fun onDetachedFromWindow() {
        // releases the bitmap in the renderer to avoid oom error
        (mRenderer as? PieChartRenderer)?.releaseBitmap()
        super.onDetachedFromWindow()
    }

    override fun getRequiredLegendOffset(): Float {
        return (mRenderer as PieChartRenderer).paintEntryLabels.textSize * 2f
    }

    override fun getRequiredBaseOffset(): Float {
        return 0f
    }

    override fun getXAxis(): XAxis {
        throw RuntimeException("PieChart has no XAxis")
    }
}