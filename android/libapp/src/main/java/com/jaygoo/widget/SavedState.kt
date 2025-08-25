package com.jaygoo.widget

import android.os.Parcel
import android.os.Parcelable
import android.view.View

class SavedState(superState: Parcelable?) : View.BaseSavedState(superState) {
    var minValue: Float = 0f
    var maxValue: Float = 0f
    var rangeInterval: Float = 0f
    var tickNumber: Int = 0
    var currSelectedMin: Float = 0f
    var currSelectedMax: Float = 0f

    constructor(parcel: Parcel) : this(null) {
        minValue = parcel.readFloat()
        maxValue = parcel.readFloat()
        rangeInterval = parcel.readFloat()
        tickNumber = parcel.readInt()
        currSelectedMin = parcel.readFloat()
        currSelectedMax = parcel.readFloat()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeFloat(minValue)
        out.writeFloat(maxValue)
        out.writeFloat(rangeInterval)
        out.writeInt(tickNumber)
        out.writeFloat(currSelectedMin)
        out.writeFloat(currSelectedMax)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}
