package com.topdon.ble.callback

import com.topdon.ble.Request

interface RequestFailedCallback : RequestCallback {
    
    fun onRequestFailed(request: Request, failType: Int, value: Any?)
}