package com.topdon.thermal.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView

@SuppressLint("AppCompatCustomView")
class MoveImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : ImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "MoveImageView"
        private const val MIN_CLICK_DELAY_TIME = 100
        private var lastClickTime: Long = 0

        @JvmStatic
        fun delayMoveTime(): Boolean {
            val curClickTime = System.currentTimeMillis()
            val flag = (curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME
            if (flag) {
                lastClickTime = curClickTime
            }
            Log.d(TAG, "ACTION_MOVE isFastClick flag : $flag")
            return flag
        }
    }

    private var mPreX: Float = 0f
    private var mPreY: Float = 0f
    var onMoveListener: OnMoveListener? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    interface OnMoveListener {
        fun onMove(preX: Float, preY: Float, curX: Float, curY: Float)
    }

    fun setOnMoveListener(listener: OnMoveListener?) {
        onMoveListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d(TAG, "ACTION_DOWN")
                mPreX = event.x
                mPreY = event.y
                lastClickTime = System.currentTimeMillis()
            }

            MotionEvent.ACTION_MOVE -> {
                Log.d(TAG, "ACTION_MOVE")
                val preX = mPreX
                val preY = mPreY
                val curX = event.x
                val curY = event.y

                if (onMoveListener != null && delayMoveTime()) {
                    Log.d(TAG, "ACTION_MOVE isFastClick")
                    onMoveListener?.onMove(preX, preY, curX, curY)
                    mPreX = curX
                    mPreY = curY
                }
            }

            MotionEvent.ACTION_UP -> Log.d(TAG, "ACTION_UP")
            MotionEvent.ACTION_CANCEL -> Log.d(TAG, "ACTION_CANCEL")
        }
        return true
    }
}