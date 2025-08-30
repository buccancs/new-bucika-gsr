package com.topdon.lms.sdk.network

/**
 * Stub implementation of IResponseCallback for IRCamera compatibility
 */
interface IResponseCallback {
    fun onResponse(response: String?)
    fun onFail(errorCode: Int, errorMessage: String)
    fun onFail(exception: Exception?) {}
    fun onFail(failMsg: String?) {
        // Default implementation - can be overridden
    }
}