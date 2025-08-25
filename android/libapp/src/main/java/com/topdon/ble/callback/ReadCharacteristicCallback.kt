package com.topdon.ble.callback

import com.topdon.ble.Request

/**
 * date: 2021/8/12 18:42
 * author: bichuanfeng
 */
fun interface ReadCharacteristicCallback : RequestFailedCallback {
    /**
     * 读取到特征值
     *
     * @param request 请求
     * @param value   读取到的数据
     */
    fun onCharacteristicRead(request: Request, value: ByteArray)
}