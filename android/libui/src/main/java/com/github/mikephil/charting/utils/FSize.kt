package com.github.mikephil.charting.utils

/**
 * Class for describing width and height dimensions in some arbitrary
 * unit. Replacement for the android.Util.SizeF which is available only on API >= 21.
 */
class FSize : ObjectPool.Poolable {

    var width: Float = 0f
    var height: Float = 0f

    companion object {
        private val pool: ObjectPool<FSize> = ObjectPool.create(256, FSize()).apply {
            replenishPercentage = 0.5f
        }

        @JvmStatic
        fun getInstance(width: Float, height: Float): FSize {
            val result = pool.get()
            result.width = width
            result.height = height
            return result
        }

        @JvmStatic
        fun recycleInstance(instance: FSize) {
            pool.recycle(instance)
        }

        @JvmStatic
        fun recycleInstances(instances: List<FSize>) {
            pool.recycle(instances)
        }
    }

    constructor()

    constructor(width: Float, height: Float) {
        this.width = width
        this.height = height
    }

    override fun instantiate(): ObjectPool.Poolable = FSize()

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other is FSize) {
            return width == other.width && height == other.height
        }
        return false
    }

    override fun toString(): String = "${width}x${height}"

    override fun hashCode(): Int {
        return java.lang.Float.floatToIntBits(width) xor java.lang.Float.floatToIntBits(height)
    }
}