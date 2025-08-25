package com.topdon.ble.callback

import com.topdon.ble.Request

fun interface ReadDescriptorCallback : RequestFailedCallback {
    
    fun onDescriptorRead(request: Request, value: ByteArray)
}