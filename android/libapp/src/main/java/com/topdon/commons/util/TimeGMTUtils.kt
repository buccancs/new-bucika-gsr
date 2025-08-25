package com.topdon.commons.util

import android.annotation.SuppressLint
import android.text.TextUtils
import com.topdon.lms.sdk.utils.LanguageUtil
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object TimeGMTUtils {

    private fun isDaylight(zone: TimeZone, time: String): Boolean {
        return try {
            @SuppressLint("SimpleDateFormat")
            val sf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val d1 = sf.parse(time)
            zone.useDaylightTime() && zone.inDaylightTime(d1 ?: Date())
        } catch (e: ParseException) {
            e.printStackTrace()
            false
        }
    }

    @JvmStatic
    fun getGMTConvertTime(time: String?, format: String): String {
        try {
            if (TextUtils.isEmpty(time)) {
                return ""
            }
            val longTime = getStringToDate(time!!, "GMT+00:00", "yyyy-MM-dd HH:mm:ss")
            val curLocale = LanguageUtil.getSystemLocal()
            val gmt = TimeZone.getDefault().getDisplayName(
                isDaylight(TimeZone.getDefault(), time),
                TimeZone.SHORT,
                curLocale
            )
            
            return getDateToString(longTime, gmt, format)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    @JvmStatic
    fun getDateToString(milSecond: Long, gmt: String, pattern: String): String {
        val date = Date(milSecond)
        val format = SimpleDateFormat(pattern)
        val timeZone = TimeZone.getTimeZone(gmt)
        format.timeZone = timeZone
        return format.format(date)
    }

    @JvmStatic
    fun getStringToDate(dateString: String, gmt: String, pattern: String): Long {
        val dateFormat = SimpleDateFormat(pattern)
        var date = Date()
        try {
            val timeZone = TimeZone.getTimeZone(gmt)
            dateFormat.timeZone = timeZone
            date = dateFormat.parse(dateString) ?: Date()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date.time
    }
}