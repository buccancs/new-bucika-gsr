package com.energy.commoncomponent.view.rangeseekbar

interface OnRangeChangedListener {
    fun onRangeChanged(view: RangeSeekBar, leftValue: Float, rightValue: Float, isFromUser: Boolean)

    fun onStartTrackingTouch(view: RangeSeekBar, isLeft: Boolean)

    fun onStopTrackingTouch(view: RangeSeekBar, isLeft: Boolean)
}
