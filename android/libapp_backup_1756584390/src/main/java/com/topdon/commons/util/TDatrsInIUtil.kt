package com.topdon.commons.util

import android.text.TextUtils
import org.ini4j.Config
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.File

object TDatrsInIUtil {

    @JvmStatic
    fun getTdartsVersion(path: String): String {
        val file = File("${path}T-darts.ini")
        if (!file.exists()) {
            LLog.e("bcf", "  ini不存在：${file.path}")
            return ""
        }
        
        val cfg = Config().apply {
            isLowerCaseOption = true
            isLowerCaseSection = true
            isMultiSection = true
        }
        
        val ini = Ini().apply { config = cfg }
        
        return try {
            ini.load(file)
            val tDartSWSection = ini["tdartsw"] ?: return ""
            
            if (!TextUtils.isEmpty(tDartSWSection["version"])) {
                tDartSWSection["version"] ?: ""
            } else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    @JvmStatic
    fun getTdarts(path: String): HashMap<String, String> {
        val hashMap = HashMap<String, String>()
        val file = File("${path}T-darts.ini")
        
        if (!file.exists()) {
            LLog.e("bcf", "  ini不存在：${file.path}")
            return hashMap
        }
        
        val cfg = Config().apply {
            isLowerCaseOption = true
            isLowerCaseSection = true
            isMultiSection = true
        }
        
        val ini = Ini().apply { config = cfg }
        
        return try {
            ini.load(file)
            val tDartSWSection = ini["tdartsw"] ?: return hashMap
            
            if (!TextUtils.isEmpty(tDartSWSection["version"])) {
                hashMap["Version"] = tDartSWSection["version"] ?: ""
            }

            val libsSection = ini["libs"] ?: return hashMap

            val libKeys = listOf(
                "t-dartsapp" to "T-dartsApp",
                "825x_module" to "825x_module", 
                "n32s032-app" to "N32S032-app"
            )
            
            libKeys.forEach { (key, displayKey) ->
                val value = libsSection[key]
                if (!TextUtils.isEmpty(value)) {
                    hashMap[displayKey] = value ?: ""
                }
            }
            
            hashMap
        } catch (e: Exception) {
            e.printStackTrace()
            hashMap
        }
    }

    @JvmStatic
    fun getBinPath(data: Int): String {
        val path = FolderUtil.getTdartsUpgradePath()
        return when (data) {
            0 -> "${path}T-dartsApp.bin"
            1 -> "${path}825x_module.bin"
            2 -> "${path}N32S032-app.bin"
            else -> ""
        }
    }
}