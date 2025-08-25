package com.topdon.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

/**
 * date: 2021/8/12 13:03
 * author: bichuanfeng
 */
internal class DefaultDeviceCreator : DeviceCreator {
    override fun create(device: BluetoothDevice, scanResult: ScanResult?): Device? {
        return Device(device)
    }
}