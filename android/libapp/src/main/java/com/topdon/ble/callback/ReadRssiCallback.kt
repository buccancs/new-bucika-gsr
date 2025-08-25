package com.topdon.ble.callback

import com.topdon.ble.Request

fun interface ReadRssiCallback : RequestFailedCallback {
    
    fun onRssiRead(request: Request, rssi: Int)
}