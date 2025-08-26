package com.topdon.ble.callback

import com.topdon.ble.Request

interface MtuChangeCallback : RequestFailedCallback {
    fun onMtuChanged(request: Request, mtu: Int)
}
