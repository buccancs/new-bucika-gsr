package com.topdon.ble.callback

import com.topdon.ble.Request

interface WriteCharacteristicCallback : RequestFailedCallback {
    
    fun onCharacteristicWrite(request: Request, value: ByteArray)
}