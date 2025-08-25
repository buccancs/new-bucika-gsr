package com.energy.commoncomponent.view.rangeseekbar

import android.os.Parcel
import android.os.Parcelable
import android.view.View

class SavedState : View.BaseSavedState {
    var minValue: Float = 0f
    var maxValue: Float = 0f
    var rangeInterval: Float = 0f
    var tickNumber: Int = 0
    var currSelectedMin: Float = 0f
    var currSelectedMax: Float = 0f

    constructor(superState: Parcelable?) : super(superState)

    private constructor(parcel: Parcel) : super(parcel) {
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

    companion object CREATOR : Parcelable.Creator<SavedState> {
        override fun createFromParcel(parcel: Parcel): SavedState {
            return SavedState(parcel)
        }

        override fun newArray(size: Int): Array<SavedState?> {
            return arrayOfNulls(size)
        }
    }
}
