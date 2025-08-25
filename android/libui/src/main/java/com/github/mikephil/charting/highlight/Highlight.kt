package com.github.mikephil.charting.highlight

import com.github.mikephil.charting.components.YAxis

/**
 * Contains information needed to determine the highlighted value.
 *
 * @author Philipp Jahoda
 */
class Highlight {

    /**
     * the x-value of the highlighted value
     */
    var x = Float.NaN
        private set

    /**
     * the y-value of the highlighted value
     */
    var y = Float.NaN
        private set

    /**
     * the x-pixel of the highlight
     */
    var xPx = 0f
        private set

    /**
     * the y-pixel of the highlight
     */
    var yPx = 0f
        private set

    /**
     * the index of the data object - in case it refers to more than one
     */
    var dataIndex = -1

    /**
     * the index of the dataset the highlighted value is in
     */
    val dataSetIndex: Int

    /**
     * index which value of a stacked bar entry is highlighted, default -1
     */
    val stackIndex: Int

    /**
     * the axis the highlighted value belongs to
     */
    val axis: YAxis.AxisDependency?

    /**
     * the x-position (pixels) on which this highlight object was last drawn
     */
    var drawX = 0f
        private set

    /**
     * the y-position (pixels) on which this highlight object was last drawn
     */
    var drawY = 0f
        private set

    constructor(x: Float, y: Float, dataSetIndex: Int) {
        this.x = x
        this.y = y
        this.dataSetIndex = dataSetIndex
        this.stackIndex = -1
        this.axis = null
    }

    constructor(x: Float, dataSetIndex: Int, stackIndex: Int) : this(x, Float.NaN, dataSetIndex) {
        this.stackIndex = stackIndex
    }

    /**
     * constructor
     *
     * @param x            the x-value of the highlighted value
     * @param y            the y-value of the highlighted value
     * @param dataSetIndex the index of the DataSet the highlighted value belongs to
     */
    constructor(x: Float, y: Float, xPx: Float, yPx: Float, dataSetIndex: Int, axis: YAxis.AxisDependency?) {
        this.x = x
        this.y = y
        this.xPx = xPx
        this.yPx = yPx
        this.dataSetIndex = dataSetIndex
        this.stackIndex = -1
        this.axis = axis
    }

    /**
     * Constructor, only used for stacked-barchart.
     *
     * @param x            the index of the highlighted value on the x-axis
     * @param y            the y-value of the highlighted value
     * @param dataSetIndex the index of the DataSet the highlighted value belongs to
     * @param stackIndex   references which value of a stacked-bar entry has been
     *                     selected
     */
    constructor(x: Float, y: Float, xPx: Float, yPx: Float, dataSetIndex: Int, stackIndex: Int, axis: YAxis.AxisDependency?) : this(x, y, xPx, yPx, dataSetIndex, axis) {
        this.stackIndex = stackIndex
    }

    val isStacked: Boolean
        get() = stackIndex >= 0

    /**
     * Sets the x- and y-position (pixels) where this highlight was last drawn.
     *
     * @param x
     * @param y
     */
    fun setDraw(x: Float, y: Float) {
        this.drawX = x
        this.drawY = y
    }

    /**
     * Returns true if this highlight object is equal to the other (compares
     * xIndex and dataSetIndex)
     *
     * @param h
     * @return
     */
    fun equalTo(h: Highlight?): Boolean {
        return if (h == null) {
            false
        } else {
            this.dataSetIndex == h.dataSetIndex && this.x == h.x && 
            this.stackIndex == h.stackIndex && this.dataIndex == h.dataIndex
        }
    }

    override fun toString(): String {
        return "Highlight, x: $x, y: $y, dataSetIndex: $dataSetIndex" +
                ", stackIndex (only stacked barentry): $stackIndex"
    }
}