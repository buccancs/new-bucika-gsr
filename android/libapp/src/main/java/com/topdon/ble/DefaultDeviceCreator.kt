package com.topdon.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

internal class DefaultDeviceCreator : DeviceCreator {
    override fun create(device: BluetoothDevice, scanResult: ScanResult?): Device? {
        return Device(device)
    }
}