package com.energy.commoncomponent.view.rangeseekbar

data class SeekBarState(
    var indicatorText: String? = null,
    var value: Float = 0f, // now progress value
    var isMin: Boolean = false,
    var isMax: Boolean = false
) {
    override fun toString(): String {
        return "indicatorText: $indicatorText, isMin: $isMin, isMax: $isMax"
    }
}