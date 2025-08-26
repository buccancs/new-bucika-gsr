package com.topdon.thermal.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.HorizontalScrollView

class DetectHorizontalScrollView(context: Context, attrs: AttributeSet) : HorizontalScrollView(context, attrs) {

    private val scrollerTask: Runnable
    private var initPosition: Int = 0
    private val newCheck: Int = 100
    private var childWidth: Int = 0

    interface OnScrollStopListener {
        fun onScrollStopped()
        fun onScrollToLeftEdge()
        fun onScrollToRightEdge()
        fun onScrollToMiddle()
        fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int)
    }

    private var onScrollStopListener: OnScrollStopListener? = null

    init {
        scrollerTask = Runnable {
            val newPosition = scrollX
            if (initPosition - newPosition == 0) {
                onScrollStopListener?.let { listener ->
                    listener.onScrollStopped()
                    val outRect = Rect()
                    getDrawingRect(outRect)
                    when {
                        scrollX == 0 -> listener.onScrollToLeftEdge()
                        childWidth + paddingLeft + paddingRight == outRect.right -> listener.onScrollToRightEdge()
                        else -> listener.onScrollToMiddle()
                    }
                }
            } else {
                initPosition = scrollX
                postDelayed(scrollerTask, newCheck.toLong())
            }
        }
    }

    fun setOnScrollStopListener(listener: OnScrollStopListener?) {
        onScrollStopListener = listener
    }

    fun startScrollerTask() {
        initPosition = scrollX
        postDelayed(scrollerTask, newCheck.toLong())
        checkTotalWidth()
    }

    private fun checkTotalWidth() {
        if (childWidth > 0) return
        
        for (i in 0 until childCount) {
            childWidth += getChildAt(i).width
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollStopListener?.onScrollChanged(l, t, oldl, oldt)
    }
}