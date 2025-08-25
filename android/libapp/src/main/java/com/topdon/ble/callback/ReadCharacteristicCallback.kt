package com.topdon.ble.callback

import com.topdon.ble.Request

fun interface ReadCharacteristicCallback : RequestFailedCallback {
    
    fun onCharacteristicRead(request: Request, value: ByteArray)
}