package com.github.mikephil.charting.jobs

import android.view.View
import com.github.mikephil.charting.utils.ObjectPool
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * Runnable that is used for viewport modifications since they cannot be
 * executed at any time. This can be used to delay the execution of viewport
 * modifications until the onSizeChanged(...) method of the chart-view is called.
 * This is especially important if viewport modifying methods are called on the chart
 * directly after initialization.
 * 
 * @author Philipp Jahoda
 */
abstract class ViewPortJob(
    protected var mViewPortHandler: ViewPortHandler?,
    protected var xValue: Float = 0f,
    protected var yValue: Float = 0f,
    protected var mTrans: Transformer?,
    protected var view: View?
) : ObjectPool.Poolable, Runnable {

    protected val pts = FloatArray(2)

    fun getXValue(): Float = xValue

    fun getYValue(): Float = yValue
}