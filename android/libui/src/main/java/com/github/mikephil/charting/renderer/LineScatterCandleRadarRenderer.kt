package com.github.mikephil.charting.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.blankj.utilcode.util.SizeUtils
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.interfaces.datasets.ILineScatterCandleRadarDataSet
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * Created by Philipp Jahoda on 11/07/15.
 */
abstract class LineScatterCandleRadarRenderer(
    animator: ChartAnimator, 
    viewPortHandler: ViewPortHandler
) : BarLineScatterCandleBubbleRenderer(animator, viewPortHandler) {

    /**
     * path that is used for drawing highlight-lines (drawLines(...) cannot be used because of dashes)
     */
    private val mHighlightLinePath = Path()

    /**
     * Draws vertical & horizontal highlight-lines if enabled.
     *
     * @param c
     * @param x   x-position of the highlight line intersection
     * @param y   y-position of the highlight line intersection
     * @param set the currently drawn dataset
     */
    protected fun drawHighlightLines(c: Canvas, x: Float, y: Float, set: ILineScatterCandleRadarDataSet<*>) {

        // set color and stroke-width
        mHighlightPaint.color = set.highLightColor
        mHighlightPaint.strokeWidth = set.highlightLineWidth

        // draw highlighted lines (if enabled)
        mHighlightPaint.pathEffect = set.dashPathEffectHighlight

        // draw vertical highlight lines
        if (set.isVerticalHighlightIndicatorEnabled) {

            // create vertical path
            mHighlightLinePath.reset()
            mHighlightLinePath.moveTo(x, mViewPortHandler.contentTop())
            mHighlightLinePath.lineTo(x, mViewPortHandler.contentBottom())

            c.drawPath(mHighlightLinePath, mHighlightPaint)
        }

        // draw horizontal highlight lines
        if (set.isHorizontalHighlightIndicatorEnabled) {

            // create horizontal path
            mHighlightLinePath.reset()
            mHighlightLinePath.moveTo(mViewPortHandler.contentLeft(), y)
            mHighlightLinePath.lineTo(mViewPortHandler.contentRight(), y)

            c.drawPath(mHighlightLinePath, mHighlightPaint)
        }

        //chart 绘制高亮辅助点  -------- start --------

        //内部圆
        mHighlightDotPaint.color = Color.rgb(243, 129, 47)
        mHighlightDotPaint.style = Paint.Style.FILL
        c.drawCircle(x, y, SizeUtils.dp2px(4f), mHighlightDotPaint)
        //外部圆
        mHighlightDotPaint.color = Color.argb(80, 255, 255, 255)
        mHighlightDotPaint.style = Paint.Style.STROKE
        c.drawCircle(x, y, SizeUtils.dp2px(5f), mHighlightDotPaint)

        // -------- end --------
    }
}