package com.jaygoo.widget

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2018/5/8
 * 描    述:
 * ================================================
 */
interface OnRangeChangedListener {
    fun onRangeChanged(view: DefRangeSeekBar, leftValue: Float, rightValue: Float, isFromUser: Boolean)

    fun onStartTrackingTouch(view: DefRangeSeekBar, isLeft: Boolean)

    fun onStopTrackingTouch(view: DefRangeSeekBar, isLeft: Boolean)
}