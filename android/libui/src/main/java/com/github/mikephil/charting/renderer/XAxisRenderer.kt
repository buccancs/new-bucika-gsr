package com.github.mikephil.charting.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.RectF
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.utils.FSize
import com.github.mikephil.charting.utils.MPPointD
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.round

class XAxisRenderer(
    viewPortHandler: ViewPortHandler,
    protected val mXAxis: XAxis,
    trans: Transformer
) : AxisRenderer(viewPortHandler, trans, mXAxis) {

    init {
        mAxisLabelPaint.apply {
            color = Color.BLACK
            textAlign = Align.CENTER
            textSize = Utils.convertDpToPixel(10f)
        }
    }

    protected fun setupGridPaint() {
        mGridPaint.apply {
            color = mXAxis.gridColor
            strokeWidth = mXAxis.gridLineWidth
            pathEffect = mXAxis.gridDashPathEffect
        }
    }

    override fun computeAxis(min: Float, max: Float, inverted: Boolean) {
        var minValue = min
        var maxValue = max

        // calculate the starting and entry point of the y-labels (depending on
        // zoom / contentrect bounds)
        if (mViewPortHandler.contentWidth() > 10 && !mViewPortHandler.isFullyZoomedOutX()) {
            val p1 = mTrans.getValuesByTouchPoint(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop())
            val p2 = mTrans.getValuesByTouchPoint(mViewPortHandler.contentRight(), mViewPortHandler.contentTop())

            if (inverted) {
                minValue = p2.x.toFloat()
                maxValue = p1.x.toFloat()
            } else {
                minValue = p1.x.toFloat()
                maxValue = p2.x.toFloat()
            }

            MPPointD.recycleInstance(p1)
            MPPointD.recycleInstance(p2)
        }

        computeAxisValues(minValue, maxValue)
    }

    override fun computeAxisValues(min: Float, max: Float) {
        super.computeAxisValues(min, max)
        computeSize()
    }

    protected fun computeSize() {
        val longest = mXAxis.longestLabel

        mAxisLabelPaint.apply {
            typeface = mXAxis.typeface
            textSize = mXAxis.textSize
        }

        val labelSize = Utils.calcTextSize(mAxisLabelPaint, longest)
        val labelWidth = labelSize.width
        val labelHeight = Utils.calcTextHeight(mAxisLabelPaint, "Q")

        val labelRotatedSize = Utils.getSizeOfRotatedRectangleByDegrees(
            labelWidth,
            labelHeight,
            mXAxis.labelRotationAngle
        )

        mXAxis.mLabelWidth = round(labelWidth).toInt()
        mXAxis.mLabelHeight = round(labelHeight).toInt()
        mXAxis.mLabelRotatedWidth = round(labelRotatedSize.width).toInt()
        mXAxis.mLabelRotatedHeight = round(labelRotatedSize.height).toInt()

        FSize.recycleInstance(labelRotatedSize)
        FSize.recycleInstance(labelSize)
    }

    override fun renderAxisLabels(c: Canvas) {
        if (!mXAxis.isEnabled || !mXAxis.isDrawLabelsEnabled)
            return

        val yoffset = mXAxis.yOffset

        mAxisLabelPaint.apply {
            typeface = mXAxis.typeface
            textSize = mXAxis.textSize
            color = mXAxis.textColor
        }

        val pointF = MPPointF.getInstance(0f, 0f)
        when (mXAxis.position) {
            XAxisPosition.TOP -> {
                pointF.x = 0.5f
                pointF.y = 1.0f
                drawLabels(c, mViewPortHandler.contentTop() - yoffset, pointF)
            }
            XAxisPosition.TOP_INSIDE -> {
                pointF.x = 0.5f
                pointF.y = 1.0f
                drawLabels(c, mViewPortHandler.contentTop() + yoffset + mXAxis.mLabelRotatedHeight, pointF)
            }
            XAxisPosition.BOTTOM -> {
                pointF.x = 0.5f
                pointF.y = 0.0f
                drawLabels(c, mViewPortHandler.contentBottom() + yoffset, pointF)
            }
            XAxisPosition.BOTTOM_INSIDE -> {
                pointF.x = 0.5f
                pointF.y = 0.0f
                drawLabels(c, mViewPortHandler.contentBottom() - yoffset - mXAxis.mLabelRotatedHeight, pointF)
            }
            else -> { // BOTH_SIDED
                pointF.x = 0.5f
                pointF.y = 1.0f
                drawLabels(c, mViewPortHandler.contentTop() - yoffset, pointF)
                pointF.x = 0.5f
                pointF.y = 0.0f
                drawLabels(c, mViewPortHandler.contentBottom() + yoffset, pointF)
            }
        }
        MPPointF.recycleInstance(pointF)
    }

    override fun renderAxisLine(c: Canvas) {
        if (!mXAxis.isDrawAxisLineEnabled || !mXAxis.isEnabled)
            return

        mAxisLinePaint.apply {
            color = mXAxis.axisLineColor
            strokeWidth = mXAxis.axisLineWidth
            pathEffect = mXAxis.axisLineDashPathEffect
        }

        if (mXAxis.position == XAxisPosition.TOP ||
            mXAxis.position == XAxisPosition.TOP_INSIDE ||
            mXAxis.position == XAxisPosition.BOTH_SIDED) {
            c.drawLine(
                mViewPortHandler.contentLeft(),
                mViewPortHandler.contentTop(),
                mViewPortHandler.contentRight(),
                mViewPortHandler.contentTop(),
                mAxisLinePaint
            )
        }

        if (mXAxis.position == XAxisPosition.BOTTOM ||
            mXAxis.position == XAxisPosition.BOTTOM_INSIDE ||
            mXAxis.position == XAxisPosition.BOTH_SIDED) {
            c.drawLine(
                mViewPortHandler.contentLeft(),
                mViewPortHandler.contentBottom(),
                mViewPortHandler.contentRight(),
                mViewPortHandler.contentBottom(),
                mAxisLinePaint
            )
        }
    }

    /**
     * draws the x-labels on the specified y-position
     */
    protected fun drawLabels(c: Canvas, pos: Float, anchor: MPPointF) {
        val labelRotationAngleDegrees = mXAxis.labelRotationAngle
        val centeringEnabled = mXAxis.isCenterAxisLabelsEnabled

        val positions = FloatArray(mXAxis.mEntryCount * 2)

        for (i in positions.indices step 2) {
            // only fill x values
            if (centeringEnabled) {
                positions[i] = mXAxis.mCenteredEntries[i / 2]
            } else {
                positions[i] = mXAxis.mEntries[i / 2]
            }
        }

        mTrans.pointValuesToPixel(positions)

        for (i in positions.indices step 2) {
            var x = positions[i]

            if (mViewPortHandler.isInBoundsX(x)) {
                val label = mXAxis.valueFormatter.getAxisLabel(mXAxis.mEntries[i / 2], mXAxis)

                if (mXAxis.isAvoidFirstLastClippingEnabled) {
                    // avoid clipping of the last
                    if (i / 2 == mXAxis.mEntryCount - 1 && mXAxis.mEntryCount > 1) {
                        val width = Utils.calcTextWidth(mAxisLabelPaint, label)

                        if (width > mViewPortHandler.offsetRight() * 2 &&
                            x + width > mViewPortHandler.chartWidth) {
                            x -= width / 2
                        }

                        // avoid clipping of the first
                    } else if (i == 0) {
                        val width = Utils.calcTextWidth(mAxisLabelPaint, label)
                        x += width / 2
                    }
                }

                //chart 绘制刻度文本  -------- start --------
                if (i == 0 && mXAxis.isJumpFirstLabel) {
                    //不是哥们，你好歹好个参数来保存要不要绘制啊，查了我半天结果是因为你这里给跳过了
                    //起始刻度不需要绘制
                    continue
                }
                // -------- end --------

                drawLabel(c, label, x, pos, anchor, labelRotationAngleDegrees)
            }
        }
    }

    protected fun drawLabel(c: Canvas, formattedLabel: String, x: Float, y: Float, anchor: MPPointF, angleDegrees: Float) {
        Utils.drawXAxisValue(c, formattedLabel, x, y, mAxisLabelPaint, anchor, angleDegrees)
    }

    protected val mRenderGridLinesPath = Path()
    protected var mRenderGridLinesBuffer = FloatArray(2)

    override fun renderGridLines(c: Canvas) {
        if (!mXAxis.isDrawGridLinesEnabled || !mXAxis.isEnabled)
            return

        val clipRestoreCount = c.save()
        c.clipRect(getGridClippingRect())

        if (mRenderGridLinesBuffer.size != mAxis.mEntryCount * 2) {
            mRenderGridLinesBuffer = FloatArray(mXAxis.mEntryCount * 2)
        }
        val positions = mRenderGridLinesBuffer

        for (i in positions.indices step 2) {
            positions[i] = mXAxis.mEntries[i / 2]
            positions[i + 1] = mXAxis.mEntries[i / 2]
        }

        mTrans.pointValuesToPixel(positions)

        setupGridPaint()

        val gridLinePath = mRenderGridLinesPath
        gridLinePath.reset()

        for (i in positions.indices step 2) {
            //chart 绘制刻度线   -------- start --------
            if (i == 0) {
                continue
            }
            // -------- end --------
            drawGridLine(c, positions[i], positions[i + 1], gridLinePath)
        }

        c.restoreToCount(clipRestoreCount)
    }

    protected val mGridClippingRect = RectF()

    fun getGridClippingRect(): RectF {
        mGridClippingRect.set(mViewPortHandler.contentRect)
        mGridClippingRect.inset(-mAxis.gridLineWidth, 0f)
        return mGridClippingRect
    }

    /**
     * Draws the grid line at the specified position using the provided path.
     */
    protected fun drawGridLine(c: Canvas, x: Float, y: Float, gridLinePath: Path) {
        gridLinePath.moveTo(x, mViewPortHandler.contentBottom())
        gridLinePath.lineTo(x, mViewPortHandler.contentTop())

        // draw a path because lines don't support dashing on lower android versions
        c.drawPath(gridLinePath, mGridPaint)

        gridLinePath.reset()
    }

    protected val mLimitLineClippingRect = RectF()

    /**
     * Clips the clipping rect of the given rect based on the provided clipping rect.
     */
    fun getLimitLineClippingRect(): RectF {
        mLimitLineClippingRect.set(mViewPortHandler.contentRect)
        mLimitLineClippingRect.inset(-mXAxis.gridLineWidth, 0f)
        return mLimitLineClippingRect
    }

    protected val mLimitLinePath = Path()

    override fun renderLimitLines(c: Canvas) {
        val limitLines = mXAxis.limitLines

        if (limitLines.isEmpty())
            return

        val clipRestoreCount = c.save()
        mLimitLineClippingRect.set(mViewPortHandler.contentRect)
        mLimitLineClippingRect.inset(-mXAxis.gridLineWidth, 0f)
        c.clipRect(mLimitLineClippingRect)

        val position = FloatArray(2)

        for (limitLine in limitLines) {
            if (!limitLine.isEnabled)
                continue

            position[0] = limitLine.limit
            position[1] = 0f
            mTrans.pointValuesToPixel(position)

            renderLimitLineLine(c, limitLine, position)
            renderLimitLineLabel(c, limitLine, position, 2f + limitLine.yOffset)
        }

        c.restoreToCount(clipRestoreCount)
    }

    fun renderLimitLineLine(c: Canvas, limitLine: LimitLine, position: FloatArray) {
        mLimitLinePath.reset()
        mLimitLinePath.moveTo(position[0], mViewPortHandler.contentTop())
        mLimitLinePath.lineTo(position[0], mViewPortHandler.contentBottom())

        mLimitLinePaint.apply {
            style = Paint.Style.STROKE
            color = limitLine.lineColor
            strokeWidth = limitLine.lineWidth
            pathEffect = limitLine.dashPathEffect
        }

        c.drawPath(mLimitLinePath, mLimitLinePaint)
    }

    fun renderLimitLineLabel(c: Canvas, limitLine: LimitLine, position: FloatArray, yOffset: Float) {
        val label = limitLine.label

        // if drawing the limit-value label is enabled
        if (!label.isNullOrEmpty()) {
            mLimitLinePaint.apply {
                style = limitLine.textStyle
                pathEffect = null
                color = limitLine.textColor
                strokeWidth = 0.5f
                textSize = limitLine.textSize
            }

            val xOffset = limitLine.lineWidth + limitLine.xOffset
            val labelPosition = limitLine.labelPosition

            when (labelPosition) {
                LimitLine.LimitLabelPosition.RIGHT_TOP -> {
                    val labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label)
                    mLimitLinePaint.textAlign = Align.LEFT
                    c.drawText(
                        label,
                        position[0] + xOffset,
                        mViewPortHandler.contentTop() + yOffset + labelLineHeight,
                        mLimitLinePaint
                    )
                }
                LimitLine.LimitLabelPosition.RIGHT_BOTTOM -> {
                    mLimitLinePaint.textAlign = Align.LEFT
                    c.drawText(
                        label,
                        position[0] + xOffset,
                        mViewPortHandler.contentBottom() - yOffset,
                        mLimitLinePaint
                    )
                }
                LimitLine.LimitLabelPosition.LEFT_TOP -> {
                    mLimitLinePaint.textAlign = Align.RIGHT
                    val labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label)
                    c.drawText(
                        label,
                        position[0] - xOffset,
                        mViewPortHandler.contentTop() + yOffset + labelLineHeight,
                        mLimitLinePaint
                    )
                }
                else -> {
                    mLimitLinePaint.textAlign = Align.RIGHT
                    c.drawText(
                        label,
                        position[0] - xOffset,
                        mViewPortHandler.contentBottom() - yOffset,
                        mLimitLinePaint
                    )
                }
            }
        }
    }
}