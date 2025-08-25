package com.github.mikephil.charting.jobs

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.View
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * Created by Philipp Jahoda on 19/02/16.
 */
@SuppressLint("NewApi")
abstract class AnimatedViewPortJob(
    viewPortHandler: ViewPortHandler?,
    xValue: Float,
    yValue: Float,
    trans: Transformer?,
    v: View?,
    protected var xOrigin: Float,
    protected var yOrigin: Float,
    duration: Long
) : ViewPortJob(viewPortHandler, xValue, yValue, trans, v), ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    protected var animator: ObjectAnimator = ObjectAnimator.ofFloat(this, "phase", 0f, 1f).apply {
        setDuration(duration)
        addUpdateListener(this@AnimatedViewPortJob)
        addListener(this@AnimatedViewPortJob)
    }

    protected var phase: Float = 0f
        set(value) {
            field = value
        }

    @SuppressLint("NewApi")
    override fun run() {
        animator.start()
    }

    fun getPhase(): Float = phase

    fun setPhase(phase: Float) {
        this.phase = phase
    }

    fun getXOrigin(): Float = xOrigin

    fun getYOrigin(): Float = yOrigin

    abstract fun recycleSelf()

    protected fun resetAnimator() {
        animator.removeAllListeners()
        animator.removeAllUpdateListeners()
        animator.reverse()
        animator.addUpdateListener(this)
        animator.addListener(this)
    }

    override fun onAnimationStart(animation: Animator) {
        // Empty implementation
    }

    override fun onAnimationEnd(animation: Animator) {
        try {
            recycleSelf()
        } catch (e: IllegalArgumentException) {
            // don't worry about it.
        }
    }

    override fun onAnimationCancel(animation: Animator) {
        try {
            recycleSelf()
        } catch (e: IllegalArgumentException) {
            // don't worry about it.
        }
    }

    override fun onAnimationRepeat(animation: Animator) {
        // Empty implementation
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        // Empty implementation
    }
}