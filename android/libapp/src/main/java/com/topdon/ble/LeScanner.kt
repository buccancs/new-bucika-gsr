package com.topdon.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.annotation.NonNull
import com.topdon.ble.callback.ScanListener
import com.topdon.ble.util.Logger

internal class LeScanner(
    easyBle: EasyBLE,
    bluetoothAdapter: BluetoothAdapter
) : AbstractScanner(easyBle, bluetoothAdapter) {
    
    private var bleScanner: BluetoothLeScanner? = null
    
    private fun getLeScanner(): BluetoothLeScanner? {
        if (bleScanner == null) {
            bleScanner = bluetoothAdapter.bluetoothLeScanner
        }
        return bleScanner
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            parseScanResult(result.device, result)
        }
        
        override fun onScanFailed(errorCode: Int) {
            handleScanCallback(
                false, 
                null, 
                false, 
                ScanListener.ERROR_SCAN_FAILED, 
                "onScanFailed. errorCode = $errorCode"
            )
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "onScanFailed. errorCode = $errorCode")
            stopScan(true)
        }
    }
    
    override fun isReady(): Boolean {
        return getLeScanner() != null
    }
    
    override fun performStartScan() {
        val settings = configuration.scanSettings ?: ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        
        bleScanner?.startScan(configuration.filters, settings, scanCallback)
    }
    
    override fun performStopScan() {
        bleScanner?.stopScan(scanCallback)
    }
    
    @NonNull
    override fun getType(): ScannerType {
        return ScannerType.LE
    }
}