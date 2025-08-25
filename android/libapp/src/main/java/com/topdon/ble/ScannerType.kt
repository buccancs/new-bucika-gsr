package com.topdon.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner

enum class ScannerType {
    
    LE,
    
    LEGACY,
    
    CLASSIC
}