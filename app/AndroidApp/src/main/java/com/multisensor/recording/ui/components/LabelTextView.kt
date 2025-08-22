package com.multisensor.recording.ui.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat

class LabelTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    enum class LabelStyle {
        FORM_LABEL,
        DESCRIPTION,
        INSTRUCTION,
        ERROR,
        SUCCESS
    }

    init {
        setLabelStyle(LabelStyle.FORM_LABEL)
    }

    fun setLabel(text: String, style: LabelStyle = LabelStyle.FORM_LABEL) {
        this.text = text
        setLabelStyle(style)
    }

    private fun setLabelStyle(style: LabelStyle) {
        when (style) {
            LabelStyle.FORM_LABEL -> {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.parseColor("#666666"))
                setPadding(0, 0, 0, dpToPx(4))
            }

            LabelStyle.DESCRIPTION -> {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(Color.parseColor("#888888"))
                setPadding(0, 0, 0, dpToPx(2))
            }

            LabelStyle.INSTRUCTION -> {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.parseColor("#444444"))
                setPadding(0, 0, 0, dpToPx(8))
            }

            LabelStyle.ERROR -> {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                setPadding(0, 0, 0, dpToPx(4))
            }

            LabelStyle.SUCCESS -> {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                setPadding(0, 0, 0, dpToPx(4))
            }
        }
    }

    fun setLabelTextColor(colorRes: Int) {
        setTextColor(ContextCompat.getColor(context, colorRes))
    }

    fun setLabelTextColor(hexColor: String) {
        setTextColor(Color.parseColor(hexColor))
    }

    fun setDarkTheme() {
        setTextColor(Color.parseColor("#CCCCCC"))
    }

    fun setLightTheme() {
        setTextColor(Color.parseColor("#666666"))
    }

    fun setRequired(isRequired: Boolean) {
        if (isRequired && !text.toString().endsWith("*")) {
            text = "${text}*"
            val spannableText = android.text.SpannableString(text)
            spannableText.setSpan(
                android.text.style.ForegroundColorSpan(Color.RED),
                text.length - 1,
                text.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setText(spannableText, BufferType.SPANNABLE)
        }
    }

    fun setClickableHelp(clickListener: OnClickListener?) {
        isClickable = true
        setOnClickListener(clickListener)
        paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
