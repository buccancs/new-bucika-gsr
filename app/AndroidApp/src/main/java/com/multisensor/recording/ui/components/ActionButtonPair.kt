package com.multisensor.recording.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class ActionButtonPair @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val leftButton: Button
    private val rightButton: Button

    enum class ButtonStyle {
        PRIMARY,
        SECONDARY,
        NEUTRAL,
        WARNING
    }

    init {
        orientation = HORIZONTAL

        leftButton = Button(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 6
            }
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
            )
        }

        rightButton = Button(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 6
            }
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
            )
        }

        addView(leftButton)
        addView(rightButton)
    }

    fun setButtons(
        leftText: String,
        rightText: String,
        leftStyle: ButtonStyle = ButtonStyle.PRIMARY,
        rightStyle: ButtonStyle = ButtonStyle.SECONDARY
    ) {
        leftButton.text = leftText
        rightButton.text = rightText

        leftButton.backgroundTintList = ContextCompat.getColorStateList(context, getColorForStyle(leftStyle))
        rightButton.backgroundTintList = ContextCompat.getColorStateList(context, getColorForStyle(rightStyle))
    }

    fun setOnClickListeners(
        leftClickListener: OnClickListener?,
        rightClickListener: OnClickListener?
    ) {
        leftButton.setOnClickListener(leftClickListener)
        rightButton.setOnClickListener(rightClickListener)
    }

    fun setButtonsEnabled(leftEnabled: Boolean, rightEnabled: Boolean) {
        leftButton.isEnabled = leftEnabled
        rightButton.isEnabled = rightEnabled
    }

    fun getLeftButton(): Button = leftButton
    fun getRightButton(): Button = rightButton

    private fun getColorForStyle(style: ButtonStyle): Int {
        return when (style) {
            ButtonStyle.PRIMARY -> android.R.color.holo_green_dark
            ButtonStyle.SECONDARY -> android.R.color.holo_red_dark
            ButtonStyle.NEUTRAL -> android.R.color.darker_gray
            ButtonStyle.WARNING -> android.R.color.holo_orange_dark
        }
    }
}
