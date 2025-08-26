package com.topdon.ble.callback

import com.topdon.ble.Request

interface ReadDescriptorCallback : RequestFailedCallback {
    
    fun onDescriptorRead(request: Request, value: ByteArray)
}