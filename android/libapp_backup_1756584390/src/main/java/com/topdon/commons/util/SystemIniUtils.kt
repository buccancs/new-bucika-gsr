package com.topdon.commons.util

import android.text.TextUtils
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.StringUtils
import org.ini4j.Config
import org.ini4j.Ini
import java.io.File

object SystemIniUtils {
    
    @JvmStatic
    fun getSystemVersion(path: String, systemName: String, systemVersion: Int): Int {
        val file = File("$path/Version.ini")
        if (!file.exists()) {
            LLog.e("bcf", "ini不存在：${file.path}")
            return -1
        }
        
        val cfg = Config().apply {
            isLowerCaseOption = true
            isLowerCaseSection = true
            isMultiSection = true
        }
        
        val ini = Ini().apply {
            config = cfg
        }
        
        return try {
            ini.load(file)
            val tDartSWSection = ini.get("ota")
            if (tDartSWSection == null) {
                return 1
            }
            
            val firmwareSw = if (!TextUtils.isEmpty(tDartSWSection.get("firmwaresw"))) {
                tDartSWSection.get("firmwaresw") ?: ""
            } else {
                ""
            }
            
            val version = if (!TextUtils.isEmpty(tDartSWSection.get("version"))) {
                tDartSWSection.get("version") ?: ""
            } else {
                ""
            }
            
            if (StringUtils.isEmpty(firmwareSw) || StringUtils.isEmpty(version)) {
                return 1
            }
            
            val version1 = version.uppercase().replace("V", "")
            if (systemName == firmwareSw && version1 == systemVersion.toString()) {
                0
            } else {
                1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }
}