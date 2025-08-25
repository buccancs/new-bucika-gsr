package com.github.mikephil.charting.data

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import kotlin.math.abs

/**
 * Subclass of Entry that holds all values for one entry in a CandleStickChart.
 * 
 * @author Philipp Jahoda
 */
@SuppressLint("ParcelCreator")
class CandleEntry : Entry {

    /** shadow-high value */
    var shadowHigh: Float = 0f
        private set

    /** shadow-low value */
    var shadowLow: Float = 0f
        private set

    /** close value */
    var close: Float = 0f
        private set

    /** open value */
    var open: Float = 0f
        private set

    /**
     * Constructor.
     * 
     * @param x The value on the x-axis
     * @param shadowH The (shadow) high value
     * @param shadowL The (shadow) low value
     * @param open The open value
     * @param close The close value
     */
    constructor(x: Float, shadowH: Float, shadowL: Float, open: Float, close: Float) : super(x, (shadowH + shadowL) / 2f) {
        this.shadowHigh = shadowH
        this.shadowLow = shadowL
        this.open = open
        this.close = close
    }

    /**
     * Constructor.
     *
     * @param x The value on the x-axis
     * @param shadowH The (shadow) high value
     * @param shadowL The (shadow) low value
     * @param open
     * @param close
     * @param data Spot for additional data this Entry represents
     */
    constructor(x: Float, shadowH: Float, shadowL: Float, open: Float, close: Float, data: Any?) : super(x, (shadowH + shadowL) / 2f, data) {
        this.shadowHigh = shadowH
        this.shadowLow = shadowL
        this.open = open
        this.close = close
    }

    /**
     * Constructor.
     *
     * @param x The value on the x-axis
     * @param shadowH The (shadow) high value
     * @param shadowL The (shadow) low value
     * @param open
     * @param close
     * @param icon Icon image
     */
    constructor(x: Float, shadowH: Float, shadowL: Float, open: Float, close: Float, icon: Drawable?) : super(x, (shadowH + shadowL) / 2f, icon) {
        this.shadowHigh = shadowH
        this.shadowLow = shadowL
        this.open = open
        this.close = close
    }

    /**
     * Constructor.
     *
     * @param x The value on the x-axis
     * @param shadowH The (shadow) high value
     * @param shadowL The (shadow) low value
     * @param open
     * @param close
     * @param icon Icon image
     * @param data Spot for additional data this Entry represents
     */
    constructor(x: Float, shadowH: Float, shadowL: Float, open: Float, close: Float, icon: Drawable?, data: Any?) : super(x, (shadowH + shadowL) / 2f, icon, data) {
        this.shadowHigh = shadowH
        this.shadowLow = shadowL
        this.open = open
        this.close = close
    }

    /**
     * Returns the overall range (difference) between shadow-high and
     * shadow-low.
     */
    val shadowRange: Float
        get() = abs(shadowHigh - shadowLow)

    /**
     * Returns the body size (difference between open and close).
     */
    val bodyRange: Float
        get() = abs(open - close)

    fun copy(): CandleEntry {
        return CandleEntry(x, shadowHigh, shadowLow, open, close, data)
    }

    /**
     * Returns the upper shadows highest value.
     */
    val high: Float
        get() = shadowHigh

    fun setHigh(shadowHigh: Float) {
        this.shadowHigh = shadowHigh
    }

    /**
     * Returns the lower shadows lowest value.
     */
    val low: Float
        get() = shadowLow

    fun setLow(shadowLow: Float) {
        this.shadowLow = shadowLow
    }

    fun setClose(close: Float) {
        this.close = close
    }

    fun setOpen(open: Float) {
        this.open = open
    }
}