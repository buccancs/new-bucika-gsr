package com.energy.view.tempcanvas

import android.content.Context
import android.graphics.Canvas

/**
 * Created by fengjibo on 2023/6/25.
 */
abstract class BaseDraw(protected val mContext: Context) {
    
    companion object {
        /**
         * 用于线和框最小尺寸判断
         */
        protected const val MIN_SIZE_PIX_COUNT = 20
    }
    
    protected var mScreenDegree = 0
    protected var mTouchIndex = -1 // 手势按住已绘制的，进行拖拽
    protected var mViewWidth = 0
    protected var mViewHeight = 0

    fun setViewWidth(viewWidth: Int) {
        this.mViewWidth = viewWidth
    }

    fun setViewHeight(viewHeight: Int) {
        this.mViewHeight = viewHeight
    }

    abstract fun onDraw(canvas: Canvas, isScroll: Boolean)

    /**
     * 获取当前选中点的数组index
     */
    fun getTouchInclude(): Int = mTouchIndex

    /**
     * 手指是否选中了其中一个点
     */
    fun isTouch(): Boolean = mTouchIndex != -1
}