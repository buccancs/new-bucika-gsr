package com.topdon.ble

internal object Inspector {
    
    fun <T> requireNonNull(obj: T?, message: String): T {
        return obj ?: throw EasyBLEException(message)
    }
}