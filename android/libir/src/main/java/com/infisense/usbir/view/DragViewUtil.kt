package com.infisense.usbir.view

import android.view.MotionEvent
import android.view.View

object DragViewUtil {
    
    @JvmStatic
    fun registerDragAction(v: View) {
        // Empty implementation as in original
    }
    
    @JvmStatic
    fun registerDragAction(v: View, delay: Long) {
        v.setOnTouchListener(TouchListener(delay))
    }
    
    private class TouchListener(private val delay: Long = 0) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var downTime = 0L
        private var isMove = false
        private var canDrag = false
        
        private fun haveDelay(): Boolean = delay > 0
        
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    isMove = false
                    downTime = System.currentTimeMillis()
                    canDrag = !haveDelay()
                }
                
                MotionEvent.ACTION_MOVE -> {
                    if (haveDelay() && !canDrag) {
                        val currMillis = System.currentTimeMillis()
                        if (currMillis - downTime >= delay) {
                            canDrag = true
                        }
                    }
                    
                    if (!canDrag) return isMove
                    
                    val xDistance = event.x - downX
                    val yDistance = event.y - downY
                    
                    if (xDistance != 0f && yDistance != 0f) {
                        val l = (v.left + xDistance).toInt()
                        val r = l + v.width
                        val t = (v.top + yDistance).toInt()
                        val b = t + v.height
                        
                        v.apply {
                            left = l
                            top = t
                            right = r
                            bottom = b
                        }
                        isMove = true
                    }
                }
            }
            return isMove
        }
    }
}