package com.topdon.lms.core

/**
 * Stub implementation of IResponseCallback for compilation
 */
interface IResponseCallback<T> {
    fun onResponse(response: T)
    fun onFail(errorCode: Int, errorMessage: String)
    fun onFail(exception: Exception?) {}
    fun onFail(failMsg: String?, errorCode: String) {
        // Default implementation - can be overridden
    }
}