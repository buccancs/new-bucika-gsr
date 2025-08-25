package com.energy.view.tempcanvas

import android.content.Context
import android.graphics.Canvas

abstract class BaseDraw(protected val mContext: Context) {
    
    companion object {
        
        protected const val MIN_SIZE_PIX_COUNT = 20
    }
    
    protected var mScreenDegree = 0
    protected var mTouchIndex = -1
    protected var mViewWidth = 0
    protected var mViewHeight = 0

    fun setViewWidth(viewWidth: Int) {
        this.mViewWidth = viewWidth
    }

    fun setViewHeight(viewHeight: Int) {
        this.mViewHeight = viewHeight
    }

    abstract fun onDraw(canvas: Canvas, isScroll: Boolean)

    fun getTouchInclude(): Int = mTouchIndex

    fun isTouch(): Boolean = mTouchIndex != -1
}