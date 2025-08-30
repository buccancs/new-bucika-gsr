package com.topdon.tc001.utils

import android.content.Context

object AppUtil {
    
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    fun getVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: Exception) {
            1
        }
    }
    
    fun installApk(context: Context, filePath: String) {
        // Stub implementation for APK installation
    }
}