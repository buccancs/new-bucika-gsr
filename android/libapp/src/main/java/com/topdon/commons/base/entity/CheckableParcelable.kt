package com.topdon.commons.base.entity

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

class CheckableParcelable<T : Parcelable> : CheckableItem<T>, Parcelable {
    constructor() : super()
    
    constructor(data: T) : super(data)
    
    constructor(data: T, isChecked: Boolean) : super(data, isChecked)

    private constructor(parcel: Parcel) : super() {
        val bundle = parcel.readBundle(javaClass.classLoader)
        bundle?.let { 
            @Suppress("DEPRECATION")
            setData(it.getParcelable("items"))
        }
        setChecked(parcel.readByte() != 0.toByte())
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val bundle = Bundle().apply {
            putParcelable("items", data)
        }
        dest.writeBundle(bundle)
        dest.writeByte(if (isChecked) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<CheckableParcelable<*>> {
        override fun createFromParcel(parcel: Parcel): CheckableParcelable<*> {
            return CheckableParcelable<Parcelable>(parcel)
        }

        override fun newArray(size: Int): Array<CheckableParcelable<*>?> {
            return arrayOfNulls(size)
        }
    }
}