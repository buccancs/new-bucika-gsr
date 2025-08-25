package com.topdon.ble.callback

import com.topdon.ble.Request

/**
 * date: 2021/8/12 17:41
 * author: bichuanfeng
 */
fun interface ReadDescriptorCallback : RequestFailedCallback {
    /**
     * 读取到描述符值
     *
     * @param request 请求
     * @param value   读取到的数据
     */
    fun onDescriptorRead(request: Request, value: ByteArray)
}