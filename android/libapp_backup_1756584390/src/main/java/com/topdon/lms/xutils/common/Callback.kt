package com.topdon.lms.xutils.common

/**
 * Stub implementation of Callback for compilation
 */
interface Callback {
    interface CommonCallback<T> {
        fun onSuccess(result: T)
        fun onError(throwable: Throwable?, isOnCallback: Boolean)
        fun onCancelled(cex: CancelledException?)
        fun onFinished()
    }
    
    // Exception class for cancelled operations
    class CancelledException : Exception()
}