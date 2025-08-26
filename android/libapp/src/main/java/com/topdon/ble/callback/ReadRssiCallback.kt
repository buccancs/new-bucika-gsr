package com.topdon.ble.callback

import com.topdon.ble.Request

interface ReadRssiCallback : RequestFailedCallback {
    
    fun onRssiRead(request: Request, rssi: Int)
}