package com.topdon.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.annotation.NonNull

internal class LegacyScanner(easyBle: EasyBLE, bluetoothAdapter: BluetoothAdapter) : 
    AbstractScanner(easyBle, bluetoothAdapter), BluetoothAdapter.LeScanCallback {
    
    override fun isReady(): Boolean = true
    
    override fun performStartScan() {
        bluetoothAdapter.startLeScan(this)
    }
    
    override fun performStopScan() {
        bluetoothAdapter.stopLeScan(this)
    }
    
    override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray) {
        parseScanResult(device, false, null, rssi, scanRecord)
    }
    
    @NonNull
    override fun getType(): ScannerType = ScannerType.LEGACY
}
