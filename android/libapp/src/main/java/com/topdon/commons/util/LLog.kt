package com.topdon.commons.util

import android.util.Log
import com.elvishew.xlog.XLog
import com.topdon.lib.core.BuildConfig

object LLog {
    private val isDebug = BuildConfig.DEBUG

    @JvmStatic
    fun d(tag: String, value: String) {
        XLog.tag(tag).d(value)
    }

    @JvmStatic
    fun i(tag: String, value: String) {
        XLog.tag(tag).i(value)
    }

    @JvmStatic
    fun w(tag: String, value: String) {
        XLog.tag(tag).w(value)
    }

    @JvmStatic
    fun e(tag: String, value: String) {
        XLog.tag(tag).e(value)
    }

    private const val MAX_LENGTH = 2000

    @JvmStatic
    fun LogMaxPrint(tag: String, msg: String) {
        if (msg.length > MAX_LENGTH) {
            var length = MAX_LENGTH + 1
            var remain = msg
            var index = 0
            while (length > MAX_LENGTH) {
                index++
                Log.v("$tag[$index]", " \n${remain.substring(0, MAX_LENGTH)}")
                remain = remain.substring(MAX_LENGTH)
                length = remain.length
            }
            if (length <= MAX_LENGTH) {
                index++
                Log.v("$tag[$index]", " \n$remain")
            }
        } else {
            Log.v(tag, msg)
        }
    }
}