package com.github.mikephil.charting.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.RectF
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition
import com.github.mikephil.charting.utils.MPPointD
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

class YAxisRenderer(
    viewPortHandler: ViewPortHandler,
    protected val mYAxis: YAxis,
    trans: Transformer
) : AxisRenderer(viewPortHandler, trans, mYAxis) {

    protected val mZeroLinePaint: Paint

    init {
        if (mViewPortHandler != null) {
            mAxisLabelPaint.apply {
                color = Color.BLACK
                textSize = Utils.convertDpToPixel(10f)
            }

            mZeroLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
        } else {
            mZeroLinePaint = Paint()
        }
    }

    /**
     * draws the y-axis labels to the screen
     */
    override fun renderAxisLabels(c: Canvas) {
        if (!mYAxis.isEnabled || !mYAxis.isDrawLabelsEnabled)
            return

        val positions = getTransformedPositions()

        mAxisLabelPaint.apply {
            typeface = mYAxis.typeface
            textSize = mYAxis.textSize
            color = mYAxis.textColor
        }

        val xoffset = mYAxis.xOffset
        val yoffset = Utils.calcTextHeight(mAxisLabelPaint, "A") / 2.5f + mYAxis.yOffset

        val dependency = mYAxis.axisDependency
        val labelPosition = mYAxis.labelPosition

        val xPos = when (dependency) {
            AxisDependency.LEFT -> {
                if (labelPosition == YAxisLabelPosition.OUTSIDE_CHART) {
                    mAxisLabelPaint.textAlign = Align.RIGHT
                    mViewPortHandler.offsetLeft() - xoffset
                } else {
                    mAxisLabelPaint.textAlign = Align.LEFT
                    mViewPortHandler.offsetLeft() + xoffset
                }
            }
            else -> {
                if (labelPosition == YAxisLabelPosition.OUTSIDE_CHART) {
                    mAxisLabelPaint.textAlign = Align.LEFT
                    mViewPortHandler.contentRight() + xoffset
                } else {
                    mAxisLabelPaint.textAlign = Align.RIGHT
                    mViewPortHandler.contentRight() - xoffset
                }
            }
        }

        drawYLabels(c, xPos, positions, yoffset)
    }

    override fun renderAxisLine(c: Canvas) {
        if (!mYAxis.isEnabled || !mYAxis.isDrawAxisLineEnabled)
            return

        mAxisLinePaint.apply {
            color = mYAxis.axisLineColor
            strokeWidth = mYAxis.axisLineWidth
        }

        if (mYAxis.axisDependency == AxisDependency.LEFT) {
            c.drawLine(
                mViewPortHandler.contentLeft(),
                mViewPortHandler.contentTop(),
                mViewPortHandler.contentLeft(),
                mViewPortHandler.contentBottom(),
                mAxisLinePaint
            )
        } else {
            c.drawLine(
                mViewPortHandler.contentRight(),
                mViewPortHandler.contentTop(),
                mViewPortHandler.contentRight(),
                mViewPortHandler.contentBottom(),
                mAxisLinePaint
            )
        }
    }

    /**
     * draws the y-labels on the specified x-position
     */
    protected fun drawYLabels(c: Canvas, fixedPosition: Float, positions: FloatArray, offset: Float) {
        val from = if (mYAxis.isDrawBottomYLabelEntryEnabled) 0 else 1
        val to = if (mYAxis.isDrawTopYLabelEntryEnabled) {
            mYAxis.mEntryCount
        } else {
            mYAxis.mEntryCount - 1
        }

        // draw
        for (i in from until to) {
            val text = mYAxis.getFormattedLabel(i)
            c.drawText(text, fixedPosition, positions[i * 2 + 1] + offset, mAxisLabelPaint)
        }
    }

    protected val mRenderGridLinesPath = Path()

    override fun renderGridLines(c: Canvas) {
        if (!mYAxis.isEnabled)
            return

        if (mYAxis.isDrawGridLinesEnabled) {
            val clipRestoreCount = c.save()
            c.clipRect(getGridClippingRect())

            val positions = getTransformedPositions()

            mGridPaint.apply {
                color = mYAxis.gridColor
                strokeWidth = mYAxis.gridLineWidth
                pathEffect = mYAxis.gridDashPathEffect
            }

            val gridLinePath = mRenderGridLinesPath
            gridLinePath.reset()

            // draw the grid
            for (i in positions.indices step 2) {
                // draw a path because lines don't support dashing on lower android versions
                c.drawPath(linePath(gridLinePath, i, positions), mGridPaint)
                gridLinePath.reset()
            }

            c.restoreToCount(clipRestoreCount)
        }

        if (mYAxis.isDrawZeroLineEnabled) {
            drawZeroLine(c)
        }
    }

    protected val mGridClippingRect = RectF()

    fun getGridClippingRect(): RectF {
        mGridClippingRect.set(mViewPortHandler.contentRect)
        mGridClippingRect.inset(0f, -mAxis.gridLineWidth)
        return mGridClippingRect
    }

    /**
     * Calculates the path for a grid line.
     */
    protected fun linePath(p: Path, i: Int, positions: FloatArray): Path {
        p.moveTo(mViewPortHandler.offsetLeft(), positions[i + 1])
        p.lineTo(mViewPortHandler.contentRight(), positions[i + 1])
        return p
    }

    protected var mGetTransformedPositionsBuffer = FloatArray(2)

    /**
     * Transforms the values contained in the axis entries to screen pixels and returns them in form of a float array
     * of x- and y-coordinates.
     */
    protected fun getTransformedPositions(): FloatArray {
        if (mGetTransformedPositionsBuffer.size != mYAxis.mEntryCount * 2) {
            mGetTransformedPositionsBuffer = FloatArray(mYAxis.mEntryCount * 2)
        }
        val positions = mGetTransformedPositionsBuffer

        for (i in positions.indices step 2) {
            // only fill y values, x values are not needed for y-labels
            positions[i + 1] = mYAxis.mEntries[i / 2]
        }

        mTrans.pointValuesToPixel(positions)
        return positions
    }

    protected val mDrawZeroLinePath = Path()
    protected val mZeroLineClippingRect = RectF()

    /**
     * Draws the zero line.
     */
    protected fun drawZeroLine(c: Canvas) {
        val clipRestoreCount = c.save()
        mZeroLineClippingRect.set(mViewPortHandler.contentRect)
        mZeroLineClippingRect.inset(0f, -mYAxis.zeroLineWidth)
        c.clipRect(mZeroLineClippingRect)

        // draw zero line
        val pos = mTrans.getPixelForValues(0f, 0f)

        mZeroLinePaint.apply {
            color = mYAxis.zeroLineColor
            strokeWidth = mYAxis.zeroLineWidth
        }

        val zeroLinePath = mDrawZeroLinePath
        zeroLinePath.reset()

        zeroLinePath.moveTo(mViewPortHandler.contentLeft(), pos.y.toFloat())
        zeroLinePath.lineTo(mViewPortHandler.contentRight(), pos.y.toFloat())

        // draw a path because lines don't support dashing on lower android versions
        c.drawPath(zeroLinePath, mZeroLinePaint)

        c.restoreToCount(clipRestoreCount)
    }

    protected val mRenderLimitLines = Path()
    protected val mRenderLimitLinesBuffer = FloatArray(2)
    protected val mLimitLineClippingRect = RectF()

    /**
     * Draws the LimitLines associated with this axis to the screen.
     */
    override fun renderLimitLines(c: Canvas) {
        val limitLines = mYAxis.limitLines

        if (limitLines.isEmpty())
            return

        val pts = mRenderLimitLinesBuffer
        pts[0] = 0f
        pts[1] = 0f
        val limitLinePath = mRenderLimitLines
        limitLinePath.reset()

        for (l in limitLines) {
            if (!l.isEnabled)
                continue

            val clipRestoreCount = c.save()
            mLimitLineClippingRect.set(mViewPortHandler.contentRect)
            mLimitLineClippingRect.inset(0f, -l.lineWidth)
            c.clipRect(mLimitLineClippingRect)

            mLimitLinePaint.apply {
                style = Paint.Style.STROKE
                color = l.lineColor
                strokeWidth = l.lineWidth
                pathEffect = l.dashPathEffect
            }

            pts[1] = l.limit

            mTrans.pointValuesToPixel(pts)

            limitLinePath.moveTo(mViewPortHandler.contentLeft(), pts[1])
            limitLinePath.lineTo(mViewPortHandler.contentRight(), pts[1])

            c.drawPath(limitLinePath, mLimitLinePaint)
            limitLinePath.reset()

            val label = l.label

            // if drawing the limit-value label is enabled
            if (!label.isNullOrEmpty()) {
                mLimitLinePaint.apply {
                    style = l.textStyle
                    pathEffect = null
                    color = l.textColor
                    typeface = l.typeface
                    strokeWidth = 0.5f
                    textSize = l.textSize
                }

                val labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label)
                val xOffset = Utils.convertDpToPixel(4f) + l.xOffset
                val yOffset = l.lineWidth + labelLineHeight + l.yOffset

                val position = l.labelPosition

                when (position) {
                    LimitLine.LimitLabelPosition.RIGHT_TOP -> {
                        mLimitLinePaint.textAlign = Align.RIGHT
                        c.drawText(
                            label,
                            mViewPortHandler.contentRight() - xOffset,
                            pts[1] - yOffset + labelLineHeight,
                            mLimitLinePaint
                        )
                    }
                    LimitLine.LimitLabelPosition.RIGHT_BOTTOM -> {
                        mLimitLinePaint.textAlign = Align.RIGHT
                        c.drawText(
                            label,
                            mViewPortHandler.contentRight() - xOffset,
                            pts[1] + yOffset,
                            mLimitLinePaint
                        )
                    }
                    LimitLine.LimitLabelPosition.LEFT_TOP -> {
                        mLimitLinePaint.textAlign = Align.LEFT
                        c.drawText(
                            label,
                            mViewPortHandler.contentLeft() + xOffset,
                            pts[1] - yOffset + labelLineHeight,
                            mLimitLinePaint
                        )
                    }
                    else -> {
                        mLimitLinePaint.textAlign = Align.LEFT
                        c.drawText(
                            label,
                            mViewPortHandler.offsetLeft() + xOffset,
                            pts[1] + yOffset,
                            mLimitLinePaint
                        )
                    }
                }
            }

            c.restoreToCount(clipRestoreCount)
        }
    }
}