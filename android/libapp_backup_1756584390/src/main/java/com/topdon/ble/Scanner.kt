package com.topdon.ble

import android.content.Context
import com.topdon.ble.callback.ScanListener

interface Scanner {
    
    fun addScanListener(listener: ScanListener)
    fun removeScanListener(listener: ScanListener)
    fun startScan(context: Context)
    fun stopScan(quietly: Boolean)
    fun isScanning(): Boolean
    fun onBluetoothOff()
    fun release()
    fun getType(): ScannerType
}
