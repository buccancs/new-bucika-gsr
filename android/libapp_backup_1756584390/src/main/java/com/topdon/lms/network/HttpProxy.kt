package com.topdon.lms.network

import com.topdon.lms.core.IResponseCallback

/**
 * Stub implementation of HttpProxy for compilation
 */
object HttpProxy {
    fun <T> post(url: String, params: Any?, callback: IResponseCallback<T>) {
        // Stub implementation - simulate success response
        try {
            // In real implementation, this would make HTTP request
            val stubResponse = ResponseBean<T>(
                code = ResponseBean.SUCCESS,
                message = "Success"
            )
            @Suppress("UNCHECKED_CAST")
            callback.onResponse(stubResponse as T)
        } catch (e: Exception) {
            callback.onFail(ResponseBean.ERROR, e.message ?: "Network error")
        }
    }
    
    fun <T> get(url: String, params: Any?, callback: IResponseCallback<T>) {
        // Stub implementation similar to post
        post(url, params, callback)
    }
}