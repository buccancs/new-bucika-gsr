package com.github.mikephil.charting.data

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import com.github.mikephil.charting.highlight.Range
import kotlin.math.abs

/**
 * Entry class for the BarChart. (especially stacked bars)
 *
 * @author Philipp Jahoda
 */
@SuppressLint("ParcelCreator")
class BarEntry : Entry {

    /**
     * the values the stacked barchart holds
     */
    private var yVals: FloatArray? = null

    /**
     * the ranges for the individual stack values - automatically calculated
     */
    private var ranges: Array<Range>? = null

    /**
     * the sum of all negative values this entry (if stacked) contains
     */
    private var negativeSum: Float = 0f

    /**
     * the sum of all positive values this entry (if stacked) contains
     */
    private var positiveSum: Float = 0f

    /**
     * Constructor for normal bars (not stacked).
     *
     * @param x
     * @param y
     */
    constructor(x: Float, y: Float) : super(x, y)

    /**
     * Constructor for normal bars (not stacked).
     *
     * @param x
     * @param y
     * @param data - Spot for additional data this Entry represents.
     */
    constructor(x: Float, y: Float, data: Any?) : super(x, y, data)

    /**
     * Constructor for normal bars (not stacked).
     *
     * @param x
     * @param y
     * @param icon - icon image
     */
    constructor(x: Float, y: Float, icon: Drawable?) : super(x, y, icon)

    /**
     * Constructor for normal bars (not stacked).
     *
     * @param x
     * @param y
     * @param icon - icon image
     * @param data - Spot for additional data this Entry represents.
     */
    constructor(x: Float, y: Float, icon: Drawable?, data: Any?) : super(x, y, icon, data)

    /**
     * Constructor for stacked bar entries. One data object for whole stack
     *
     * @param x
     * @param vals - the stack values, use at least 2
     */
    constructor(x: Float, vals: FloatArray) : super(x, calcSum(vals)) {
        this.yVals = vals
        calcPosNegSum()
        calcRanges()
    }

    /**
     * Constructor for stacked bar entries. One data object for whole stack
     *
     * @param x
     * @param vals - the stack values, use at least 2
     * @param data - Spot for additional data this Entry represents.
     */
    constructor(x: Float, vals: FloatArray, data: Any?) : super(x, calcSum(vals), data) {
        this.yVals = vals
        calcPosNegSum()
        calcRanges()
    }

    /**
     * Constructor for stacked bar entries. One data object for whole stack
     *
     * @param x
     * @param vals - the stack values, use at least 2
     * @param icon - icon image
     */
    constructor(x: Float, vals: FloatArray, icon: Drawable?) : super(x, calcSum(vals), icon) {
        this.yVals = vals
        calcPosNegSum()
        calcRanges()
    }

    /**
     * Constructor for stacked bar entries. One data object for whole stack
     *
     * @param x
     * @param vals - the stack values, use at least 2
     * @param icon - icon image
     * @param data - Spot for additional data this Entry represents.
     */
    constructor(x: Float, vals: FloatArray, icon: Drawable?, data: Any?) : super(x, calcSum(vals), icon, data) {
        this.yVals = vals
        calcPosNegSum()
        calcRanges()
    }

    /**
     * Returns an exact copy of the BarEntry.
     */
    fun copy(): BarEntry {
        val copied = BarEntry(x, y, data)
        copied.setVals(yVals)
        return copied
    }

    /**
     * Returns the stacked values this BarEntry represents, or null, if only a single value is represented (then, use
     * getY()).
     */
    fun getYVals(): FloatArray? = yVals

    /**
     * Set the array of values this BarEntry should represent.
     *
     * @param vals
     */
    fun setVals(vals: FloatArray?) {
        y = calcSum(vals)
        yVals = vals
        calcPosNegSum()
        calcRanges()
    }

    /**
     * Returns the ranges of the individual stack-entries. Will return null if this entry is not stacked.
     */
    fun getRanges(): Array<Range>? = ranges

    /**
     * Returns true if this BarEntry is stacked (has a values array), false if not.
     */
    val isStacked: Boolean
        get() = yVals != null

    /**
     * Use `getSumBelow(stackIndex)` instead.
     */
    @Deprecated("Use getSumBelow(stackIndex) instead", ReplaceWith("getSumBelow(stackIndex)"))
    fun getBelowSum(stackIndex: Int): Float = getSumBelow(stackIndex)

    fun getSumBelow(stackIndex: Int): Float {
        val vals = yVals ?: return 0f

        var remainder = 0f
        var index = vals.size - 1

        while (index > stackIndex && index >= 0) {
            remainder += vals[index]
            index--
        }

        return remainder
    }

    /**
     * Returns the sum of all positive values this entry (if stacked) contains.
     */
    fun getPositiveSum(): Float = positiveSum

    /**
     * Returns the sum of all negative values this entry (if stacked) contains. (this is a positive number)
     */
    fun getNegativeSum(): Float = negativeSum

    private fun calcPosNegSum() {
        val vals = yVals
        if (vals == null) {
            negativeSum = 0f
            positiveSum = 0f
            return
        }

        var sumNeg = 0f
        var sumPos = 0f

        for (f in vals) {
            if (f <= 0f) {
                sumNeg += abs(f)
            } else {
                sumPos += f
            }
        }

        negativeSum = sumNeg
        positiveSum = sumPos
    }

    private fun calcRanges() {
        val values = getYVals() ?: return
        if (values.isEmpty()) return

        ranges = Array(values.size) { Range(0f, 0f) }

        var negRemain = -getNegativeSum()
        var posRemain = 0f

        for (i in ranges!!.indices) {
            val value = values[i]

            if (value < 0) {
                ranges!![i] = Range(negRemain, negRemain - value)
                negRemain -= value
            } else {
                ranges!![i] = Range(posRemain, posRemain + value)
                posRemain += value
            }
        }
    }

    companion object {
        /**
         * Calculates the sum across all values of the given stack.
         *
         * @param vals
         * @return
         */
        private fun calcSum(vals: FloatArray?): Float {
            if (vals == null) return 0f
            return vals.sum()
        }
    }
}