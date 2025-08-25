package com.topdon.ble.callback

import com.topdon.ble.Request

/**
 * date: 2021/8/12 17:40
 * author: bichuanfeng
 */
fun interface WriteCharacteristicCallback : RequestFailedCallback {
    /**
     * 成功写入特征值
     *
     * @param request 请求
     * @param value   写入的数据
     */
    fun onCharacteristicWrite(request: Request, value: ByteArray)
}