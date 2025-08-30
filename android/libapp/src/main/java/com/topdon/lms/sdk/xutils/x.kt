package com.topdon.lms.sdk.xutils

import com.topdon.lms.sdk.xutils.common.Callback
import com.topdon.lms.sdk.xutils.http.RequestParams

/**
 * Main xutils framework class for IRCamera integration
 */
object x {
    fun http(): HttpManager = HttpManager()
}

class HttpManager {
    fun post(params: RequestParams, callback: Callback.CommonCallback<String>) {
        // Stub implementation
        callback.onSuccess("")
    }
}