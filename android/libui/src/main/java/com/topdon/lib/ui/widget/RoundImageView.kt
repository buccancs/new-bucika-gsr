package com.topdon.lib.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.topdon.lib.ui.R

class RoundImageView : AppCompatImageView {
    companion object {
        
        const val LEFT_TOP = 1

        const val RIGHT_TOP = 2

        const val LEFT_BOTTOM = 4

        const val RIGHT_BOTTOM = 8

        private const val DEFAULT_RADIUS = 10f

        private const val DEFAULT_POSITION = 15
    }

    var position = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var radius = 0
    private val path = Path()
    private var density = 0f

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        density = context.resources.displayMetrics.density

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundImageView, defStyleAttr, 0)
        radius = typedArray.getDimensionPixelSize(R.styleable.RoundImageView_round_radius, dp2px(DEFAULT_RADIUS))
        position = typedArray.getInt(R.styleable.RoundImageView_round_position, DEFAULT_POSITION)
        typedArray.recycle()

    }

    override fun onDraw(canvas: Canvas) {
        path.rewind()

        if (position and LEFT_TOP == LEFT_TOP) {
            path.moveTo(radius.toFloat(), 0f)
        }

        if (position and RIGHT_TOP == RIGHT_TOP) {
            path.lineTo((width - radius).toFloat(), 0f)
            path.quadTo(width.toFloat(), 0f, width.toFloat(), radius.toFloat())
        } else {
            path.lineTo(width.toFloat(), 0f)
        }

        if (position and RIGHT_BOTTOM == RIGHT_BOTTOM) {
            path.lineTo(width.toFloat(), (height - radius).toFloat())
            path.quadTo(width.toFloat(), height.toFloat(), (width - radius).toFloat(), height.toFloat())
        } else {
            path.lineTo(width.toFloat(), height.toFloat())
        }

        if (position and LEFT_BOTTOM == LEFT_BOTTOM) {
            path.lineTo(radius.toFloat(), height.toFloat())
            path.quadTo(0f, height.toFloat(), 0f, (height - radius).toFloat())
        } else {
            path.lineTo(0f, height.toFloat())
        }

        if (position and LEFT_TOP == LEFT_TOP) {
            path.lineTo(0f, radius.toFloat())
            path.quadTo(0f, 0f, radius.toFloat(), 0f)
        } else {
            path.lineTo(0f, 0f)
        }

        canvas.clipPath(path)
        super.onDraw(canvas)
    }

    fun setRadius(radius: Float) {
        if (this.radius != dp2px(radius)) {
            this.radius = dp2px(radius)
            invalidate()
        }
    }

    private fun dp2px(dpValue: Float): Int {
        return (dpValue * density + 0.5f).toInt()
    }
