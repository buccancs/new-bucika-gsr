package com.topdon.ble.util

interface Logger {
    
    companion object {
        const val TYPE_GENERAL = 0
        const val TYPE_SCAN_STATE = 1
        const val TYPE_CONNECTION_STATE = 2
        const val TYPE_CHARACTERISTIC_READ = 3
        const val TYPE_CHARACTERISTIC_CHANGED = 4
        const val TYPE_READ_REMOTE_RSSI = 5
        const val TYPE_MTU_CHANGED = 6
        const val TYPE_REQUEST_FAILED = 7
        const val TYPE_DESCRIPTOR_READ = 8
        const val TYPE_NOTIFICATION_CHANGED = 9
        const val TYPE_INDICATION_CHANGED = 10
        const val TYPE_CHARACTERISTIC_WRITE = 11
        const val TYPE_PHY_CHANGE = 12
    }
    
    fun log(priority: Int, type: Int, msg: String)
    fun log(priority: Int, type: Int, msg: String, th: Throwable)
    fun setEnabled(isEnabled: Boolean)
    fun isEnabled(): Boolean
}
