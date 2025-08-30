package com.topdon.lms.utils

import com.topdon.lms.core.IResponseCallback
import com.topdon.lms.network.HttpProxy

/**
 * Stub implementation providing instant network call utilities
 */
object instant {
    fun <T> post(url: String, params: Any?, callback: IResponseCallback<T>) {
        HttpProxy.post(url, params, callback)
    }
    
    fun <T> get(url: String, params: Any?, callback: IResponseCallback<T>) {
        HttpProxy.get(url, params, callback)
    }
}