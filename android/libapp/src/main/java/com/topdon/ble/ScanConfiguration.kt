package com.topdon.ble

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import androidx.annotation.RequiresApi

class ScanConfiguration {
    var scanPeriodMillis = 10000
        private set
    var acceptSysConnectedDevice = false
        private set
    var scanSettings: ScanSettings? = null
        private set
    var onlyAcceptBleDevice = false
        private set
    var rssiLowLimit = -120
        private set
    var filters: List<ScanFilter>? = null
        private set
    
    fun setScanPeriodMillis(scanPeriodMillis: Int): ScanConfiguration = apply {
        if (scanPeriodMillis >= 1000) {
            this.scanPeriodMillis = scanPeriodMillis
        }
    }
    
    fun setAcceptSysConnectedDevice(acceptSysConnectedDevice: Boolean): ScanConfiguration = apply {
        this.acceptSysConnectedDevice = acceptSysConnectedDevice
    }
    
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setScanSettings(scanSettings: ScanSettings): ScanConfiguration = apply {
        Inspector.requireNonNull(scanSettings, "scanSettings can't be null")
        this.scanSettings = scanSettings
    }
    
    fun setOnlyAcceptBleDevice(onlyAcceptBleDevice: Boolean): ScanConfiguration = apply {
        this.onlyAcceptBleDevice = onlyAcceptBleDevice
    }
    
    fun setRssiLowLimit(rssiLowLimit: Int): ScanConfiguration = apply {
        this.rssiLowLimit = rssiLowLimit
    }
    
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setFilters(filters: List<ScanFilter>): ScanConfiguration = apply {
        this.filters = filters
    }
}