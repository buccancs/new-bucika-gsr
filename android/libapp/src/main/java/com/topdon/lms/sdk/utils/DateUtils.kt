package com.topdon.lms.sdk.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Stub implementation of DateUtils
 */
object DateUtils {
    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
}