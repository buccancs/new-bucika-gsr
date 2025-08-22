package com.multisensor.recording.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class CardSectionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class CardStyle {
        DEFAULT,
        COMPACT,
        FLAT,
        DARK
    }

    init {
        setCardStyle(CardStyle.DEFAULT)
        orientation = VERTICAL
    }

    fun setCardStyle(style: CardStyle) {
        when (style) {
            CardStyle.DEFAULT -> {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                elevation = dpToPx(2).toFloat()
                setMargins(0, 0, 0, dpToPx(16))
            }

            CardStyle.COMPACT -> {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                elevation = dpToPx(2).toFloat()
                setMargins(0, 0, 0, dpToPx(8))
            }

            CardStyle.FLAT -> {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                elevation = 0f
                setMargins(0, 0, 0, dpToPx(16))
            }

            CardStyle.DARK -> {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                elevation = dpToPx(2).toFloat()
                setMargins(0, 0, 0, dpToPx(16))
            }
        }
    }

    fun setCardBackgroundColor(colorRes: Int) {
        setBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    fun setCardPadding(paddingDp: Int) {
        val paddingPx = dpToPx(paddingDp)
        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
    }

    fun setCardElevation(elevationDp: Int) {
        elevation = dpToPx(elevationDp).toFloat()
    }

    private fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        val params = layoutParams as? MarginLayoutParams ?: MarginLayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        params.setMargins(left, top, right, bottom)
        layoutParams = params
    }

    fun addHeader(
        headerText: String,
        headerStyle: SectionHeaderView.HeaderStyle = SectionHeaderView.HeaderStyle.SECTION_HEADER
    ) {
        val header = SectionHeaderView(context)
        header.setHeader(headerText, headerStyle)

        when {
            background != null -> {
                val backgroundColor = extractBackgroundColor()
                if (isColorDark(backgroundColor)) {
                    header.setDarkTheme()
                } else {
                    header.setLightTheme()
                }
            }

            else -> header.setLightTheme()
        }

        addView(header, 0)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun extractBackgroundColor(): Int {
        return when (val bg = background) {
            is ColorDrawable -> bg.color
            is GradientDrawable -> {
                Color.WHITE
            }

            else -> {
                Color.WHITE
            }
        }
    }

    private fun isColorDark(colour: Int): Boolean {
        val red = Color.red(colour)
        val green = Color.green(colour)
        val blue = Color.blue(colour)

        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0

        return luminance < 0.5
    }
}
