package com.topdon.lib.core.tools

object CheckDoubleClick {
    
    private val records: MutableMap<String, Long> = HashMap()

    fun isFastDoubleClick(): Boolean {
        if (records.size > 1000) {
            records.clear()
        }

        val ste = Throwable().stackTrace[1]
        val key = ste.fileName + ste.lineNumber
        var lastClickTime = records[key]
        val thisClickTime = System.currentTimeMillis()
        records[key] = thisClickTime
        if (lastClickTime == null) {
            lastClickTime = 0L
        }
        val timeDuration = thisClickTime - lastClickTime
        return timeDuration in 1..499
    }
