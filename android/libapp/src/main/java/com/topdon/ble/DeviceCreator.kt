package com.topdon.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

fun interface DeviceCreator {
    
    fun create(device: BluetoothDevice, scanResult: ScanResult?): Device?
}