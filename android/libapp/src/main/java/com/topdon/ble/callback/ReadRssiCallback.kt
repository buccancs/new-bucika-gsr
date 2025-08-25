package com.topdon.ble.callback

import com.topdon.ble.Request

/**
 * date: 2021/8/12 17:44
 * author: bichuanfeng
 */
fun interface ReadRssiCallback : RequestFailedCallback {
    /**
     * 读取到设备的信号强度
     *
     * @param request 请求
     * @param rssi    信号强度
     */
    fun onRssiRead(request: Request, rssi: Int)
}