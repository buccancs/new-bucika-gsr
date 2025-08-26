package com.infisense.usbir

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.*

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val TAG = "CrashHandler"
        
        @JvmStatic
        val instance: CrashHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { CrashHandler() }
    }
    
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mContext: Context? = null
    private var logFile: File? = null
    
    fun init(context: Context) {
        mContext = context
        logFile = File(context.cacheDir, "crashLog.trace")
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
    
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        ex.printStackTrace()
        
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler?.uncaughtException(thread, ex)
        } else {
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            try {
                logFile?.let { uploadErrorFileToServer(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) return false
        
        Thread {
            Looper.prepare()
            Toast.makeText(mContext, "程序发生异常，即将重启", Toast.LENGTH_LONG).show()
            Looper.loop()
        }.start()
        
        var pw: PrintWriter? = null
        try {
            logFile?.let { file ->
                if (!file.exists()) {
                    file.createNewFile()
                }
                pw = PrintWriter(file)
                collectInfoToSDCard(pw!!, ex)
                pw?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return true
    }
    
    private fun uploadErrorFileToServer(errorFile: File) {
        // Implementation placeholder
    }
    
    @Throws(PackageManager.NameNotFoundException::class)
    private fun collectInfoToSDCard(pw: PrintWriter, ex: Throwable) {
        val context = mContext ?: return
        
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
        
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        pw.print("time : ")
        pw.println(time)
        
        pw.print("versionCode : ")
        pw.println(pi.versionCode)
        
        pw.print("versionName : ")
        pw.println(pi.versionName)
        
        try {
            val fields = Build::class.java.declaredFields
            for (field in fields) {
                field.isAccessible = true
                pw.print("${field.name} : ")
                pw.println(field.get(null).toString())
            }
        } catch (e: Exception) {
            Log.i(TAG, "an error occurred when collect crash info$e")
        }
        
        ex.printStackTrace(pw)
    }
}