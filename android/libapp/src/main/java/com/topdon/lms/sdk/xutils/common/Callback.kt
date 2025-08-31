package com.topdon.lms.sdk.xutils.common

/**
 * Callback classes for xutils framework IRCamera integration
 */
interface Callback {
    interface CommonCallback<T> {
        fun onSuccess(result: T)
        fun onError(ex: Throwable, isOnCallback: Boolean)
        fun onFinished()
        fun onCancelled(cex: Any?)
    }
}