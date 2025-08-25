package com.topdon.ble.callback

import com.topdon.ble.Request

/**
 * date: 2021/8/12 17:39
 * author: bichuanfeng
 */
interface RequestFailedCallback : RequestCallback {
    /**
     * 请求失败
     *
     * @param request  请求
     * @param failType 失败类型。{@link Connection#REQUEST_FAIL_TYPE_GATT_IS_NULL}等
     * @param value    请求时带的数据，可能为null
     */
    fun onRequestFailed(request: Request, failType: Int, value: Any?)
}