package com.github.mikephil.charting.buffer

import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import kotlin.math.abs

class BarBuffer(size: Int, private val mDataSetCount: Int, private val mContainsStacks: Boolean) : AbstractBuffer<IBarDataSet>(size) {

    protected var mDataSetIndex = 0
    protected var mInverted = false

    /** width of the bar on the x-axis, in values (not pixels) */
    protected var mBarWidth = 1f

    fun setBarWidth(barWidth: Float) {
        this.mBarWidth = barWidth
    }

    fun setDataSet(index: Int) {
        this.mDataSetIndex = index
    }

    fun setInverted(inverted: Boolean) {
        this.mInverted = inverted
    }

    protected fun addBar(left: Float, top: Float, right: Float, bottom: Float) {
        buffer[index++] = left
        buffer[index++] = top
        buffer[index++] = right
        buffer[index++] = bottom
    }

    override fun feed(data: IBarDataSet) {
        val size = data.entryCount * phaseX
        val barWidthHalf = mBarWidth / 2f

        for (i in 0 until size.toInt()) {
            val e = data.getEntryForIndex(i) ?: continue

            val x = e.x
            var y = e.y
            val vals = e.yVals

            if (!mContainsStacks || vals == null) {
                val left = x - barWidthHalf
                val right = x + barWidthHalf
                val bottom: Float
                val top: Float

                if (mInverted) {
                    bottom = if (y >= 0) y else 0f
                    top = if (y <= 0) y else 0f
                } else {
                    top = if (y >= 0) y else 0f
                    bottom = if (y <= 0) y else 0f
                }

                // multiply the height of the rect with the phase
                val finalTop = if (top > 0) top * phaseY else top
                val finalBottom = if (bottom < 0) bottom * phaseY else bottom

                addBar(left, finalTop, right, finalBottom)
            } else {
                var posY = 0f
                var negY = -e.negativeSum
                var yStart: Float

                // fill the stack
                for (k in vals.indices) {
                    val value = vals[k]

                    when {
                        value == 0.0f && (posY == 0.0f || negY == 0.0f) -> {
                            // Take care of the situation of a 0.0 value, which overlaps a non-zero bar
                            y = value
                            yStart = y
                        }
                        value >= 0.0f -> {
                            y = posY
                            yStart = posY + value
                            posY = yStart
                        }
                        else -> {
                            y = negY
                            yStart = negY + abs(value)
                            negY += abs(value)
                        }
                    }

                    val left = x - barWidthHalf
                    val right = x + barWidthHalf
                    val bottom: Float
                    val top: Float

                    if (mInverted) {
                        bottom = if (y >= yStart) y else yStart
                        top = if (y <= yStart) y else yStart
                    } else {
                        top = if (y >= yStart) y else yStart
                        bottom = if (y <= yStart) y else yStart
                    }

                    // multiply the height of the rect with the phase
                    val finalTop = top * phaseY
                    val finalBottom = bottom * phaseY

                    addBar(left, finalTop, right, finalBottom)
                }
            }
        }

        reset()
    }
}