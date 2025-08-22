package com.multisensor.recording.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class StatusIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val indicatorView: View
    private val statusText: TextView

    enum class StatusType {
        CONNECTED,
        DISCONNECTED,
        RECORDING,
        STOPPED,
        WARNING,
        ERROR
    }

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL

        indicatorView = View(context).apply {
            layoutParams = LayoutParams(
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 3,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 3
            ).apply {
                marginEnd = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 6
            }
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
        }

        statusText = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            text = "Status: Disconnected"
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            textSize = 14f
        }

        addView(indicatorView)
        addView(statusText)
    }

    fun setStatus(status: StatusType, text: String) {
        statusText.text = text

        val colorRes = when (status) {
            StatusType.CONNECTED -> android.R.color.holo_green_light
            StatusType.DISCONNECTED -> android.R.color.holo_red_light
            StatusType.RECORDING -> android.R.color.holo_green_dark
            StatusType.STOPPED -> android.R.color.darker_gray
            StatusType.WARNING -> android.R.color.holo_orange_light
            StatusType.ERROR -> android.R.color.holo_red_dark
        }

        indicatorView.setBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    fun setTextColor(colorRes: Int) {
        statusText.setTextColor(ContextCompat.getColor(context, colorRes))
    }

    fun setTextSize(sizeSp: Float) {
        statusText.textSize = sizeSp
    }
}
