package com.github.mikephil.charting.jobs

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.view.View
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.utils.ObjectPool
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * Created by Philipp Jahoda on 19/02/16.
 */
@SuppressLint("NewApi")
class AnimatedZoomJob(
    viewPortHandler: ViewPortHandler?,
    v: View?,
    trans: Transformer?,
    private var yAxis: YAxis?,
    private var xAxisRange: Float,
    scaleX: Float,
    scaleY: Float,
    xOrigin: Float,
    yOrigin: Float,
    private var zoomCenterX: Float,
    private var zoomCenterY: Float,
    private var zoomOriginX: Float,
    private var zoomOriginY: Float,
    duration: Long
) : AnimatedViewPortJob(viewPortHandler, scaleX, scaleY, trans, v, xOrigin, yOrigin, duration), Animator.AnimatorListener {

    companion object {
        private val pool: ObjectPool<AnimatedZoomJob> = ObjectPool.create(8, AnimatedZoomJob(null, null, null, null, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0))

        fun getInstance(
            viewPortHandler: ViewPortHandler?,
            v: View?,
            trans: Transformer?,
            axis: YAxis?,
            xAxisRange: Float,
            scaleX: Float,
            scaleY: Float,
            xOrigin: Float,
            yOrigin: Float,
            zoomCenterX: Float,
            zoomCenterY: Float,
            zoomOriginX: Float,
            zoomOriginY: Float,
            duration: Long
        ): AnimatedZoomJob {
            val result = pool.get()
            result.mViewPortHandler = viewPortHandler
            result.xValue = scaleX
            result.yValue = scaleY
            result.mTrans = trans
            result.view = v
            result.xOrigin = xOrigin
            result.yOrigin = yOrigin
            result.yAxis = axis
            result.xAxisRange = xAxisRange
            result.zoomCenterX = zoomCenterX
            result.zoomCenterY = zoomCenterY
            result.zoomOriginX = zoomOriginX
            result.zoomOriginY = zoomOriginY
            result.resetAnimator()
            result.animator.duration = duration
            return result
        }
    }

    init {
        animator.addListener(this)
    }

    private val mOnAnimationUpdateMatrixBuffer = Matrix()

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val scaleX = xOrigin + (xValue - xOrigin) * phase
        val scaleY = yOrigin + (yValue - yOrigin) * phase

        val save = mOnAnimationUpdateMatrixBuffer
        mViewPortHandler?.setZoom(scaleX, scaleY, save)
        mViewPortHandler?.refresh(save, view, false)

        val valsInView = yAxis?.mAxisRange?.div(mViewPortHandler?.scaleY ?: 1f) ?: 0f
        val xsInView = xAxisRange / (mViewPortHandler?.scaleX ?: 1f)

        pts[0] = zoomOriginX + ((zoomCenterX - xsInView / 2f) - zoomOriginX) * phase
        pts[1] = zoomOriginY + ((zoomCenterY + valsInView / 2f) - zoomOriginY) * phase

        mTrans?.pointValuesToPixel(pts)

        mViewPortHandler?.translate(pts, save)
        mViewPortHandler?.refresh(save, view, true)
    }

    override fun onAnimationEnd(animation: Animator) {
        (view as? BarLineChartBase)?.calculateOffsets()
        view?.postInvalidate()
    }

    override fun onAnimationCancel(animation: Animator) {
        // Empty implementation
    }

    override fun onAnimationRepeat(animation: Animator) {
        // Empty implementation
    }

    override fun recycleSelf() {
        // Empty implementation
    }

    override fun onAnimationStart(animation: Animator) {
        // Empty implementation
    }

    override fun instantiate(): ObjectPool.Poolable {
        return AnimatedZoomJob(null, null, null, null, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0)
    }
}