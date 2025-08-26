package com.topdon.ble

import android.bluetooth.BluetoothAdapter
import androidx.annotation.NonNull

internal class ClassicScanner(
    easyBle: EasyBLE,
    bluetoothAdapter: BluetoothAdapter
) : AbstractScanner(easyBle, bluetoothAdapter) {
    
    private var stopQuietly = false

    override fun isReady(): Boolean = true

    override fun performStartScan() {
        bluetoothAdapter.startDiscovery()
    }

    override fun performStopScan() {
        bluetoothAdapter.cancelDiscovery()
    }

    override fun setScanning(scanning: Boolean) {
        super.setScanning(scanning)
        when {
            scanning -> handleScanCallback(true, null, false, -1, "")
            !stopQuietly -> handleScanCallback(false, null, false, -1, "")
            else -> stopQuietly = false
        }
    }

    override fun stopScan(quietly: Boolean) {
        if (isScanning()) {
            stopQuietly = quietly
        }
        super.stopScan(quietly)
    }

    @NonNull
    override fun getType(): ScannerType = ScannerType.CLASSIC
}
