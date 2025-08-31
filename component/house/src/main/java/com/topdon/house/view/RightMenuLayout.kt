package com.topdon.house.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Scroller
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlin.math.abs

/**
 * 从右往左滑动出现右侧隐藏菜单的 ViewGroup.
 *
 * Created by LCG on 2024/8/15.
 */
class RightMenuLayout : ConstraintLayout {

    companion object {
        /**
         * 右侧菜单滑出右侧菜单的百分之多少后，将触发自动展开。
         */
        private const val DEFAULT_AUTO_PERCENT = 0.5f

        /**
         * 保存当前处于展开状态的 View，用于保持唯一展开。
         */
        @JvmStatic
        private var currentOpenView: RightMenuLayout? = null
    }


    /**
     * 触发展开收起时使用的 Scroller
     */
    private val scroller: Scroller
    /**
     * 视为滚动的距离
     */
    private val scaledTouchSlop: Int
    /**
     * 右侧菜单滑出右侧菜单的百分之多少后，将触发自动展开。
     */
    private var autoPercent: Float = DEFAULT_AUTO_PERCENT


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        scroller = Scroller(context)
        scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }


    /**
     * 菜单即常规状态下不可见部分的宽度，单位 px
     */
    private var menuWidth = 0
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var totalWidth = 0
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            if (childView.isVisible) {
                totalWidth += childView.measuredWidth
            }
        }
        menuWidth = (totalWidth - measuredWidth).coerceAtLeast(0)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var left = 0
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            if (childView.isVisible) {
                childView.layout(left, 0, left + childView.measuredWidth, childView.measuredHeight)
                left += childView.measuredWidth
            }
        }
    }

    private var downX = 0
    private var downY = 0
    private var downScrollX = 0
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null || menuWidth <= 0) {
            return super.onInterceptTouchEvent(ev)
        }
        if (super.onInterceptTouchEvent(ev)) {
            return true
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x.toInt()
                downY = ev.y.toInt()
                downScrollX = scrollX
                if (currentOpenView !== this) {
                    currentOpenView?.close()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val distanceX = abs(ev.x.toInt() - downX)
                val distanceY = abs(ev.y.toInt() - downY)
                if (distanceX > scaledTouchSlop && distanceX > distanceY) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {

            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || menuWidth <= 0) {
            return true
        }
        val currentX = event.x.toInt()
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                scrollX = (downScrollX + downX - currentX).coerceAtMost(menuWidth).coerceAtLeast(0)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (abs(currentX - downX) < scaledTouchSlop) {
                    //搞了半天相当于什么都没动，原来什么状态就保持什么状态
                    if (scrollX != 0 && scrollX != menuWidth) {
                        scroller.startScroll(scrollX, 0, if (scrollX > 0) menuWidth - scrollX else -scrollX, 0)
                        invalidate()
                    }
                } else {
                    if (currentX > downX) {//从左往右滑，关闭 →→
                        if (currentOpenView === this) {
                            currentOpenView = null
                        }
                        scroller.startScroll(scrollX, 0, -scrollX, 0)
                    } else {//从右往左滑，展开 ←←
                        if ((downX - currentX) < (menuWidth * autoPercent).toInt()) {//滑动的距离不够，恢复收起状态
                            if (currentOpenView === this) {
                                currentOpenView = null
                            }
                            scroller.startScroll(scrollX, 0, -scrollX, 0)
                        } else {
                            currentOpenView = this
                            scroller.startScroll(scrollX, 0, menuWidth - scrollX, 0)
                        }
                    }
                    invalidate()
                }
            }
        }
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (scroller.currX == scrollX) {
                postInvalidateOnAnimation()
                return
            }
            scrollX = scroller.currX
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (currentOpenView === this) {
            currentOpenView = null
            scroller.abortAnimation()
            scrollX = 0
        }
    }

    /**
     * 将当前 View 置为收起状态
     */
    fun close() {
        if (currentOpenView === this) {
            currentOpenView = null
        }
        scroller.startScroll(scrollX, 0, -scrollX, 0)
        invalidate()
    }

    /**
     * 将当前 View 置为展开状态
     */
    fun expand() {
        if (currentOpenView !== this) {
            currentOpenView?.close()
        }

        currentOpenView = this
        scroller.startScroll(scrollX, 0, menuWidth - scrollX, 0)
        invalidate()
    }
}