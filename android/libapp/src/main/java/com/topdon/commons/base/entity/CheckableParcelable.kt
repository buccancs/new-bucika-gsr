package com.topdon.commons.base.entity

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

class CheckableParcelable<T : Parcelable> : Parcelable {
    var data: T? = null
    private var isChecked: Boolean = false
    
    constructor() 
    
    constructor(data: T) {
        this.data = data
    }
    
    constructor(data: T, isChecked: Boolean) {
        this.data = data
        this.isChecked = isChecked
    }

    private constructor(parcel: Parcel) {
        val bundle = parcel.readBundle(javaClass.classLoader)
        bundle?.let { 
            @Suppress("DEPRECATION")
            data = it.getParcelable<T>("items")
        }
        isChecked = parcel.readByte() != 0.toByte()
    }

    fun isChecked(): Boolean = isChecked
    
    fun setChecked(isChecked: Boolean): CheckableParcelable<T> {
        this.isChecked = isChecked
        return this
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