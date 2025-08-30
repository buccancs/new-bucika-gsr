package com.topdon.ble.callback

import android.bluetooth.BluetoothDevice
import com.topdon.ble.Request

interface PhyChangeCallback : RequestFailedCallback {
    
    fun onPhyChange(request: Request, txPhy: Int, rxPhy: Int)
}