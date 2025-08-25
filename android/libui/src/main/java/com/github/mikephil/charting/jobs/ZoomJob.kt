package com.github.mikephil.charting.jobs

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
class ZoomJob(
    viewPortHandler: ViewPortHandler?,
    private var scaleX: Float,
    private var scaleY: Float,
    xValue: Float,
    yValue: Float,
    trans: Transformer?,
    private var axisDependency: YAxis.AxisDependency?,
    v: View?
) : ViewPortJob(viewPortHandler, xValue, yValue, trans, v) {

    companion object {
        private val pool: ObjectPool<ZoomJob> = ObjectPool.create(1, ZoomJob(null, 0f, 0f, 0f, 0f, null, null, null)).apply {
            setReplenishPercentage(0.5f)
        }

        fun getInstance(
            viewPortHandler: ViewPortHandler?,
            scaleX: Float,
            scaleY: Float,
            xValue: Float,
            yValue: Float,
            trans: Transformer?,
            axis: YAxis.AxisDependency?,
            v: View?
        ): ZoomJob {
            val result = pool.get()
            result.xValue = xValue
            result.yValue = yValue
            result.scaleX = scaleX
            result.scaleY = scaleY
            result.mViewPortHandler = viewPortHandler
            result.mTrans = trans
            result.axisDependency = axis
            result.view = v
            return result
        }

        fun recycleInstance(instance: ZoomJob) {
            pool.recycle(instance)
        }
    }

    private val mRunMatrixBuffer = Matrix()

    override fun run() {
        val save = mRunMatrixBuffer
        mViewPortHandler?.zoom(scaleX, scaleY, save)
        mViewPortHandler?.refresh(save, view, false)

        val barLineChart = view as? BarLineChartBase
        val yValsInView = barLineChart?.getAxis(axisDependency)?.mAxisRange?.div(mViewPortHandler?.scaleY ?: 1f) ?: 0f
        val xValsInView = barLineChart?.xAxis?.mAxisRange?.div(mViewPortHandler?.scaleX ?: 1f) ?: 0f

        pts[0] = xValue - xValsInView / 2f
        pts[1] = yValue + yValsInView / 2f

        mTrans?.pointValuesToPixel(pts)

        mViewPortHandler?.translate(pts, save)
        mViewPortHandler?.refresh(save, view, false)

        barLineChart?.calculateOffsets()
        view?.postInvalidate()

        recycleInstance(this)
    }

    override fun instantiate(): ObjectPool.Poolable {
        return ZoomJob(null, 0f, 0f, 0f, 0f, null, null, null)
    }
}