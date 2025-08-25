package com.jaygoo.widget

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2018/5/9
 * 描    述: it works for draw indicator text
 * ================================================
 */
data class SeekBarState(
    var indicatorText: String? = null,
    var value: Float = 0f, // now progress value
    var isMin: Boolean = false,
    var isMax: Boolean = false
) {
    override fun toString(): String {
        return "indicatorText: $indicatorText ,isMin: $isMin ,isMax: $isMax"
    }
}
