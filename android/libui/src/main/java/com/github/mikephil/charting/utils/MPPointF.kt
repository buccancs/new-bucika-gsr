package com.github.mikephil.charting.utils

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Tony Patino on 6/24/16.
 */
class MPPointF : ObjectPool.Poolable, Parcelable {

    var x: Float = 0f
    var y: Float = 0f

    companion object {
        private val pool: ObjectPool<MPPointF> = ObjectPool.create(32, MPPointF()).apply {
            replenishPercentage = 0.5f
        }

        @JvmStatic
        fun getInstance(x: Float, y: Float): MPPointF {
            val result = pool.get()
            result.x = x
            result.y = y
            return result
        }

        @JvmStatic
        fun getInstance(): MPPointF = pool.get()

        @JvmStatic
        fun getInstance(copy: MPPointF): MPPointF {
            val result = pool.get()
            result.x = copy.x
            result.y = copy.y
            return result
        }

        @JvmStatic
        fun recycleInstance(instance: MPPointF) {
            pool.recycle(instance)
        }

        @JvmStatic
        fun recycleInstances(instances: List<MPPointF>) {
            pool.recycle(instances)
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<MPPointF> {
            /**
             * Return a new point from the data in the specified parcel.
             */
            override fun createFromParcel(parcel: Parcel): MPPointF {
                val r = MPPointF()
                r.my_readFromParcel(parcel)
                return r
            }

            /**
             * Return an array of rectangles of the specified size.
             */
            override fun newArray(size: Int): Array<MPPointF?> = arrayOfNulls(size)
        }
    }

    constructor()

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    /**
     * Set the point's coordinates from the data stored in the specified
     * parcel. To write a point to a parcel, call writeToParcel().
     * Provided to support older Android devices.
     *
     * @param parcelIn The parcel to read the point's coordinates from
     */
    fun my_readFromParcel(parcelIn: Parcel) {
        x = parcelIn.readFloat()
        y = parcelIn.readFloat()
    }

    override fun instantiate(): ObjectPool.Poolable = MPPointF()

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeFloat(x)
        dest.writeFloat(y)
    }
}