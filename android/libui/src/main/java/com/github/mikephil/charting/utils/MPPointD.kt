package com.github.mikephil.charting.utils

/**
 * Point encapsulating two double values.
 *
 * @author Philipp Jahoda
 */
class MPPointD : ObjectPool.Poolable {

    var x: Double = 0.0
    var y: Double = 0.0

    companion object {
        private val pool: ObjectPool<MPPointD> = ObjectPool.create(64, MPPointD()).apply {
            replenishPercentage = 0.5f
        }

        @JvmStatic
        fun getInstance(x: Double, y: Double): MPPointD {
            val result = pool.get()
            result.x = x
            result.y = y
            return result
        }

        @JvmStatic
        fun recycleInstance(instance: MPPointD) {
            pool.recycle(instance)
        }

        @JvmStatic
        fun recycleInstances(instances: List<MPPointD>) {
            pool.recycle(instances)
        }
    }

    private constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    private constructor()

    override fun instantiate(): ObjectPool.Poolable = MPPointD()

    /**
     * returns a string representation of the object
     */
    override fun toString(): String = "MPPointD, x: $x, y: $y"
}