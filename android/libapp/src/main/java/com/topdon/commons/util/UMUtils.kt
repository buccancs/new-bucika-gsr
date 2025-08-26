package com.topdon.commons.util

import android.content.Context
import android.util.Log

object UMUtils {

    fun onEvent(context: Context, event: String, value: String) {

        Log.d("UMUtils", "Event: $event, Value: $value")
    }

    fun onEvent(context: Context, event: String) {

        Log.d("UMUtils", "Event: $event")
    }
}