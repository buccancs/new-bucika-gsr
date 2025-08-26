package com.topdon.ble.util

import android.util.Log

class DefaultLogger(private val tag: String) : Logger {
    private var isEnabled: Boolean = false
    
    override fun setEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
    }
    
    override fun isEnabled(): Boolean = isEnabled
    
    override fun log(priority: Int, type: Int, msg: String) {
        if (isEnabled) {
            Log.println(priority, tag, msg)
        }
    }
    
    override fun log(priority: Int, type: Int, msg: String, th: Throwable) {
        if (isEnabled) {
            val logMessage = msg?.let { "$it\n${Log.getStackTraceString(th)}" } 
                ?: Log.getStackTraceString(th)
            log(priority, type, logMessage)
        }
    }
}
