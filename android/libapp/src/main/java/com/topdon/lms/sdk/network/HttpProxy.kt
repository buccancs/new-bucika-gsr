package com.topdon.lms.sdk.network

/**
 * Stub implementation of HttpProxy for compilation
 */
object HttpProxy {
    fun post(url: String, params: Any?, callback: IResponseCallback) {
        // Stub implementation - simulate success response
        callback.onSuccess(null)
    }
    
    fun get(url: String, params: Any?, callback: IResponseCallback) {
        // Stub implementation - simulate success response
        callback.onSuccess(null)
    }
}