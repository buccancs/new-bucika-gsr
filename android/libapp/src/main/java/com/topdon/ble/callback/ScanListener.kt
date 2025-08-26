package com.topdon.ble.callback

import android.Manifest
import com.topdon.ble.Device

interface ScanListener {
    companion object {
        const val ERROR_LACK_LOCATION_PERMISSION = 0
        const val ERROR_LOCATION_SERVICE_CLOSED = 1
        const val ERROR_SCAN_FAILED = 2
    }

    fun onScanStart()
    fun onScanStop()
    fun onScanResult(device: Device, isConnectedBySys: Boolean)
    fun onScanError(errorCode: Int, errorMsg: String)
}
