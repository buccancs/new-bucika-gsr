package com.github.mikephil.charting.charts

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.github.mikephil.charting.animation.Easing.EasingFunction
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.listener.PieRadarChartTouchListener
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import kotlin.math.*

/**
 * Baseclass of PieChart and RadarChart.
 *
 * @author Philipp Jahoda
 */
abstract class PieRadarChartBase<T : ChartData<out IDataSet<out Entry>>> : Chart<T> {

    /**
     * holds the normalized version of the current rotation angle of the chart
     */
    private var mRotationAngle = 270f

    /**
     * holds the raw version of the current rotation angle of the chart
     */
    private var mRawRotationAngle = 270f

    /**
     * flag that indicates if rotation is enabled or not
     */
    protected var mRotateEnabled = true

    /**
     * Sets the minimum offset (padding) around the chart, defaults to 0.f
     */
    protected var mMinOffset = 0f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun init() {
        super.init()
        mChartTouchListener = PieRadarChartTouchListener(this)
    }

    override fun calcMinMax() {
        // mXAxis.mAxisRange = mData.getXVals().size - 1
    }

    override fun getMaxVisibleCount(): Int = mData.entryCount

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // use the pie- and radarchart listener own listener
        return if (mTouchEnabled && mChartTouchListener != null) {
            mChartTouchListener?.onTouch(this, event) ?: false
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun computeScroll() {
        (mChartTouchListener as? PieRadarChartTouchListener)?.computeScroll()
    }

    override fun notifyDataSetChanged() {
        if (mData == null) return

        calcMinMax()

        mLegend?.let { 
            mLegendRenderer.computeLegend(mData)
        }

        calculateOffsets()
    }

    override fun calculateOffsets() {
        var legendLeft = 0f
        var legendRight = 0f
        var legendBottom = 0f
        var legendTop = 0f

        if (mLegend != null && mLegend.isEnabled && !mLegend.isDrawInsideEnabled) {
            val fullLegendWidth = min(mLegend.mNeededWidth, mViewPortHandler.chartWidth * mLegend.maxSizePercent)

            when (mLegend.orientation) {
                Legend.LegendOrientation.VERTICAL -> {
                    var xLegendOffset = 0f

                    if (mLegend.horizontalAlignment == Legend.LegendHorizontalAlignment.LEFT ||
                        mLegend.horizontalAlignment == Legend.LegendHorizontalAlignment.RIGHT) {
                        
                        if (mLegend.verticalAlignment == Legend.LegendVerticalAlignment.CENTER) {
                            // this is the space between the legend and the chart
                            val spacing = Utils.convertDpToPixel(13f)
                            xLegendOffset = fullLegendWidth + spacing
                        } else {
                            // this is the space between the legend and the chart
                            val spacing = Utils.convertDpToPixel(8f)
                            val legendWidth = fullLegendWidth + spacing
                            val legendHeight = mLegend.mNeededHeight + mLegend.mTextHeightMax

                            val center = getCenter()
                            val bottomX = if (mLegend.horizontalAlignment == Legend.LegendHorizontalAlignment.RIGHT) {
                                width - legendWidth + 15f
                            } else {
                                legendWidth - 15f
                            }
                            val bottomY = legendHeight + 15f
                            val distLegend = distanceToCenter(bottomX, bottomY)

                            val reference = getPosition(center, getRadius(), getAngleForPoint(bottomX, bottomY))
                            val distReference = distanceToCenter(reference.x, reference.y)
                            val minOffset = Utils.convertDpToPixel(5f)

                            if (bottomY >= center.y && height - legendWidth > width) {
                                xLegendOffset = legendWidth
                            } else if (distLegend < distReference) {
                                val diff = distReference - distLegend
                                xLegendOffset = minOffset + diff
                            }

                            MPPointF.recycleInstance(center)
                            MPPointF.recycleInstance(reference)
                        }
                    }

                    when (mLegend.horizontalAlignment) {
                        Legend.LegendHorizontalAlignment.LEFT -> legendLeft = xLegendOffset
                        Legend.LegendHorizontalAlignment.RIGHT -> legendRight = xLegendOffset
                        Legend.LegendHorizontalAlignment.CENTER -> {
                            when (mLegend.verticalAlignment) {
                                Legend.LegendVerticalAlignment.TOP -> {
                                    legendTop = min(mLegend.mNeededHeight, mViewPortHandler.chartHeight * mLegend.maxSizePercent)
                                }
                                Legend.LegendVerticalAlignment.BOTTOM -> {
                                    legendBottom = min(mLegend.mNeededHeight, mViewPortHandler.chartHeight * mLegend.maxSizePercent)
                                }
                                else -> {}
                            }
                        }
                    }
                }

                Legend.LegendOrientation.HORIZONTAL -> {
                    var yLegendOffset = 0f

                    if (mLegend.verticalAlignment == Legend.LegendVerticalAlignment.TOP ||
                        mLegend.verticalAlignment == Legend.LegendVerticalAlignment.BOTTOM) {

                        yLegendOffset = min(mLegend.mNeededHeight, mViewPortHandler.chartHeight * mLegend.maxSizePercent)

                        when (mLegend.verticalAlignment) {
                            Legend.LegendVerticalAlignment.TOP -> legendTop = yLegendOffset
                            Legend.LegendVerticalAlignment.BOTTOM -> legendBottom = yLegendOffset
                            else -> {}
                        }
                    }
                }
            }

            legendLeft += getRequiredBaseOffset()
            legendRight += getRequiredBaseOffset()
            legendTop += getRequiredBaseOffset()
            legendBottom += getRequiredBaseOffset()
        }

        val minOffset = mMinOffset
        legendLeft = max(minOffset, legendLeft)
        legendTop = max(minOffset, legendTop)
        legendRight = max(minOffset, legendRight)
        legendBottom = max(minOffset, legendBottom)

        mViewPortHandler.restrainViewPort(legendLeft, legendTop, legendRight, legendBottom)

        if (mLogEnabled) {
            Log.i(LOG_TAG, "legendOffsets: left: $legendLeft, top: $legendTop, right: $legendRight, bottom: $legendBottom")
        }
    }

    /**
     * returns the angle relative to the chart center for the given point on the
     * chart in degrees. The angle is always between 0 and 360°, 0° is NORTH,
     * 90° is EAST, 180° is SOUTH, 270° is WEST.
     *
     * @param x
     * @param y
     * @return
     */
    fun getAngleForPoint(x: Float, y: Float): Float {
        val c = centerOffsets
        val tx = (x - c.x).toDouble()
        val ty = (y - c.y).toDouble()
        val length = sqrt(tx * tx + ty * ty)
        val r = acos(ty / length)

        var angle = Math.toDegrees(r).toFloat()

        if (x > c.x) {
            angle = 360f - angle
        }

        // add 90° because chart starts EAST
        angle += 90f

        // neutralize overflow
        if (angle > 360f) {
            angle -= 360f
        }

        MPPointF.recycleInstance(c)
        return angle
    }

    /**
     * Calculates the position around a center point, depending on the distance
     * from the center, and the angle of the position around the center.
     *
     * @param center
     * @param dist
     * @param angle in degrees, converted to radians internally
     * @return
     */
    fun getPosition(center: MPPointF, dist: Float, angle: Float): MPPointF {
        val p = MPPointF.getInstance(0f, 0f)
        getPosition(center, dist, angle, p)
        return p
    }

    fun getPosition(center: MPPointF, dist: Float, angle: Float, outputPoint: MPPointF) {
        outputPoint.x = (center.x + dist * cos(Math.toRadians(angle.toDouble()))).toFloat()
        outputPoint.y = (center.y + dist * sin(Math.toRadians(angle.toDouble()))).toFloat()
    }

    /**
     * Returns the distance of a certain point on the chart to the center of the
     * chart.
     *
     * @param x
     * @param y
     * @return
     */
    fun distanceToCenter(x: Float, y: Float): Float {
        val c = centerOffsets

        val xDist = abs(x - c.x)
        val yDist = abs(y - c.y)

        // pythagoras
        val dist = sqrt((xDist * xDist + yDist * yDist).toDouble()).toFloat()

        MPPointF.recycleInstance(c)
        return dist
    }

    /**
     * Returns the xIndex for the given angle around the center of the chart.
     * Returns -1 if not found / outofbounds.
     *
     * @param angle
     * @return
     */
    abstract fun getIndexForAngle(angle: Float): Int

    /**
     * Set an offset for the rotation of the RadarChart in degrees. Default 270f
     * --> top (NORTH)
     *
     * @param angle
     */
    fun setRotationAngle(angle: Float) {
        mRawRotationAngle = angle
        mRotationAngle = Utils.getNormalizedAngle(mRawRotationAngle)
    }

    /**
     * gets the raw version of the current rotation angle of the pie chart the
     * returned value could be any value, negative or positive, outside of the
     * 360 degrees. this is used when working with rotation direction, mainly by
     * gestures and animations.
     *
     * @return
     */
    fun getRawRotationAngle(): Float = mRawRotationAngle

    /**
     * gets a normalized version of the current rotation angle of the pie chart,
     * which will always be between 0.0 < 360.0
     *
     * @return
     */
    fun getRotationAngle(): Float = mRotationAngle

    /**
     * Set this to true to enable the rotation / spinning of the chart by touch.
     * Set it to false to disable it. Default: true
     *
     * @param enabled
     */
    fun setRotationEnabled(enabled: Boolean) {
        mRotateEnabled = enabled
    }

    /**
     * Returns true if rotation of the chart by touch is enabled, false if not.
     *
     * @return
     */
    fun isRotationEnabled(): Boolean = mRotateEnabled

    /**
     * Sets the minimum offset (padding) around the chart, defaults to 0.f
     *
     * @param minOffset
     */
    fun setMinOffset(minOffset: Float) {
        mMinOffset = minOffset
    }

    /**
     * Returns the minimum offset (padding) around the chart, defaults to 0.f
     *
     * @return
     */
    fun getMinOffset(): Float = mMinOffset

    /**
     * returns the diameter of the pie- or radar-chart
     *
     * @return
     */
    abstract fun getDiameter(): Float

    /**
     * Returns the radius of the chart in pixels.
     *
     * @return
     */
    abstract fun getRadius(): Float

    /**
     * Returns the required offset for the chart legend.
     */
    protected abstract fun getRequiredLegendOffset(): Float

    /**
     * Returns the base offset needed for the chart without considering the
     * legend size.
     *
     * @return
     */
    protected abstract fun getRequiredBaseOffset(): Float

    override fun getYChartMax(): Float {
        // TODO Auto-generated method stub
        return 0f
    }

    override fun getYChartMin(): Float {
        // TODO Auto-generated method stub
        return 0f
    }

    /**
     * ################ ################ ################ ################
     */
    /** CODE BELOW THIS RELATED TO ANIMATION */

    /**
     * Applies a spin animation to the Chart.
     *
     * @param durationmillis
     * @param fromangle
     * @param toangle
     */
    @SuppressLint("NewApi")
    fun spin(durationmillis: Int, fromangle: Float, toangle: Float, easing: EasingFunction) {
        setRotationAngle(fromangle)

        val spinAnimator = ObjectAnimator.ofFloat(this, "rotationAngle", fromangle, toangle)
        spinAnimator.duration = durationmillis.toLong()
        spinAnimator.interpolator = easing

        spinAnimator.addUpdateListener { postInvalidate() }
        spinAnimator.start()
    }
}