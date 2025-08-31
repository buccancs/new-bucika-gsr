package com.topdon.lms.xutils

import com.topdon.lms.xutils.common.Callback
import com.topdon.lms.xutils.http.RequestParams

/**
 * Stub implementation of x (xutils main class) for compilation
 */
object x {
    fun http(): HttpManager = HttpManager
    
    object HttpManager {
        fun post(params: RequestParams, callback: Callback.CommonCallback<String>?) {
            // Stub implementation - actual implementation would be in app module
            callback?.onSuccess("")
        }
        
        fun get(params: RequestParams, callback: Callback.CommonCallback<String>?) {
            // Stub implementation
            callback?.onSuccess("")
        }
    }
}