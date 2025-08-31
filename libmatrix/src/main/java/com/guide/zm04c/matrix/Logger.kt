package com.guide.zm04c.matrix

import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object Logger {

    /**
     * ERROR
     */

    @JvmStatic
    fun e(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(clazz.simpleName, msg + "")
        }
    }

    @JvmStatic
    fun e(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg + "")
        }
    }

    /**
     * WARN
     */
    @JvmStatic
    fun w(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.w(clazz.simpleName, msg + "")
        }
    }

    @JvmStatic
    fun w(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg + "")
        }
    }

    /**
     * INFO
     */

    @JvmStatic
    fun i(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(clazz.simpleName, msg + "")
        }
    }

    @JvmStatic
    fun i(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg + "")
        }
    }

    /**
     * DEBUG
     */

    @JvmStatic
    fun d(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(clazz.simpleName, msg + "")
        }
    }

    @JvmStatic
    fun d(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg + "")
        }
    }

    /**
     * VERBOSE
     */

    @JvmStatic
    fun v(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.v(clazz.simpleName, msg + "")
        }
    }

    fun v(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, msg + "")
        }
    }


    private val MYLOG_PATH_SDCARD_DIR = "/sdcard/Guide/log" // 日志文件在sdcard中的路径

    private val MYLOGFILEName = "Log.txt" // 本类输出的日志文件名称

    private val myLogSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") // 日志的输出格式

    private val logfile = SimpleDateFormat("yyyy-MM-dd") // 日志文件格式

    /**
     * 打开日志文件并写入日志
     * @param mylogtype
     * @param tag
     * @param text
     */
    fun f(tag: String, text: String) { // 新建或打开日志文件
        val nowtime = Date()
        val needWriteFiel = logfile.format(nowtime)
        val needWriteMessage = myLogSdf.format(nowtime) + "    " + "    " + tag + "    " + text
        val dirsFile = File(MYLOG_PATH_SDCARD_DIR)
        if (!dirsFile.exists()) {
            dirsFile.mkdirs()
        }
        val file = File(dirsFile.toString(), needWriteFiel + MYLOGFILEName) // MYLOG_PATH_SDCARD_DIR
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
            }
        }
        try {
            val filerWriter = FileWriter(file, true) // 后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            val bufWriter = BufferedWriter(filerWriter)
            bufWriter.write(needWriteMessage)
            bufWriter.newLine()
            bufWriter.close()
            filerWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}