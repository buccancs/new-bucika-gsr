package com.infisense.usbir.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.topdon.lib.core.utils.ScreenUtil

open class DragScaleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnTouchListener {
    
    companion object {
        private const val TOP = 0x15
        private const val LEFT = 0x16
        private const val BOTTOM = 0x17
        private const val RIGHT = 0x18
        private const val LEFT_TOP = 0x11
        private const val RIGHT_TOP = 0x12
        private const val LEFT_BOTTOM = 0x13
        private const val RIGHT_BOTTOM = 0x14
        private const val CENTER = 0x19
    }
    
    protected var screenWidth = 0
    protected var screenHeight = 0
    protected var lastX = 0
    protected var lastY = 0
    private var oriLeft = 0
    private var oriRight = 0
    private var oriTop = 0
    private var oriBottom = 0
    private var dragDirection = 0
    private val offset = 20
    protected val paint = Paint()
    
    init {
        setOnTouchListener(this)
        initScreenWH()
    }
    
    protected fun initScreenWH() {
        screenHeight = ScreenUtil.getScreenHeight(context) - 40
        screenWidth = ScreenUtil.getScreenWidth(context)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }
    
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN) {
            oriLeft = v.left
            oriRight = v.right
            oriTop = v.top
            oriBottom = v.bottom
            lastY = event.rawY.toInt()
            lastX = event.rawX.toInt()
            dragDirection = getDirection(v, event.x.toInt(), event.y.toInt())
        }
        
        delDrag(v, event, action)
        invalidate()
        return false
    }
    
    protected fun delDrag(v: View, event: MotionEvent, action: Int) {
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX.toInt() - lastX
                val dy = event.rawY.toInt() - lastY
                
                when (dragDirection) {
                    LEFT -> left(v, dx)
                    RIGHT -> right(v, dx)
                    BOTTOM -> bottom(v, dy)
                    TOP -> top(v, dy)
                    CENTER -> center(v, dx, dy)
                    LEFT_BOTTOM -> {
                        left(v, dx)
                        bottom(v, dy)
                    }
                    LEFT_TOP -> {
                        left(v, dx)
                        top(v, dy)
                    }
                    RIGHT_BOTTOM -> {
                        right(v, dx)
                        bottom(v, dy)
                    }
                    RIGHT_TOP -> {
                        right(v, dx)
                        top(v, dy)
                    }
                }
                
                if (dragDirection != CENTER) {
                    v.layout(oriLeft, oriTop, oriRight, oriBottom)
                }
                
                lastX = event.rawX.toInt()
                lastY = event.rawY.toInt()
            }
            MotionEvent.ACTION_UP -> {
                dragDirection = 0
            }
        }
    }
    
    private fun center(v: View, dx: Int, dy: Int) {
        var left = v.left + dx
        var top = v.top + dy
        var right = v.right + dx
        var bottom = v.bottom + dy
        
        if (left < -offset) {
            left = -offset
            right = left + v.width
        }
        if (right > screenWidth + offset) {
            right = screenWidth + offset
            left = right - v.width
        }
        if (top < -offset) {
            top = -offset
            bottom = top + v.height
        }
        if (bottom > screenHeight + offset) {
            bottom = screenHeight + offset
            top = bottom - v.height
        }
        
        v.layout(left, top, right, bottom)
    }
    
    private fun top(v: View, dy: Int) {
        oriTop += dy
        if (oriTop < -offset) {
            oriTop = -offset
        }
        if (oriBottom - oriTop - 2 * offset < 200) {
            oriTop = oriBottom - 2 * offset - 200
        }
    }
    
    private fun bottom(v: View, dy: Int) {
        oriBottom += dy
        if (oriBottom > screenHeight + offset) {
            oriBottom = screenHeight + offset
        }
        if (oriBottom - oriTop - 2 * offset < 200) {
            oriBottom = 200 + oriTop + 2 * offset
        }
    }
    
    private fun right(v: View, dx: Int) {
        oriRight += dx
        if (oriRight > screenWidth + offset) {
            oriRight = screenWidth + offset
        }
        if (oriRight - oriLeft - 2 * offset < 200) {
            oriRight = oriLeft + 2 * offset + 200
        }
    }
    
    private fun left(v: View, dx: Int) {
        oriLeft += dx
        if (oriLeft < -offset) {
            oriLeft = -offset
        }
        if (oriRight - oriLeft - 2 * offset < 200) {
            oriLeft = oriRight - 2 * offset - 200
        }
    }
    
    protected fun getDirection(v: View, x: Int, y: Int): Int {
        val left = v.left
        val right = v.right
        val bottom = v.bottom
        val top = v.top
        
        return when {
            x < 40 && y < 40 -> LEFT_TOP
            y < 40 && right - left - x < 40 -> RIGHT_TOP
            x < 40 && bottom - top - y < 40 -> LEFT_BOTTOM
            right - left - x < 40 && bottom - top - y < 40 -> RIGHT_BOTTOM
            x < 40 -> LEFT
            y < 40 -> TOP
            right - left - x < 40 -> RIGHT
            bottom - top - y < 40 -> BOTTOM
            else -> CENTER
        }
    }
    
    fun getCutWidth(): Int = width - 2 * offset
    
    fun getCutHeight(): Int = height - 2 * offset
}