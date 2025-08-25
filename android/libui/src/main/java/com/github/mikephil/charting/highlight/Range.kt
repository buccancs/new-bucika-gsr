package com.github.mikephil.charting.highlight

/**
 * Created by Philipp Jahoda on 24/07/15. Class that represents the range of one value in a stacked bar entry. e.g.
 * stack values are -10, 5, 20 -> then ranges are (-10 - 0, 0 - 5, 5 - 25).
 */
data class Range(
    var from: Float,
    var to: Float
) {

    /**
     * Returns true if this range contains (if the value is in between) the given value, false if not.
     *
     * @param value
     * @return
     */
    fun contains(value: Float): Boolean = value > from && value <= to

    fun isLarger(value: Float): Boolean = value > to

    fun isSmaller(value: Float): Boolean = value < from
}