package com.topdon.commons.util

import android.text.TextUtils
import org.ini4j.Config
import org.ini4j.Ini
import org.ini4j.Profile.Section
import java.io.*
import java.nio.charset.Charset

object IniUtil {
    private const val NAME = "Link"
    private const val LINK = "link"
    private const val LINK_NAME = "name"
    private const val LANGUAGE = "language"
    private const val VERSION = "version"
    private const val MAINTENANCE = "maintenance"
    private const val SYSTEM = "system"

    @JvmStatic
    fun getLink(path: String): String {
        val file = File("$path/Diag.ini")
        if (!file.exists()) return ""
        
        val cfg = Config().apply {
            isLowerCaseOption = true
            isLowerCaseSection = true
            isMultiSection = true
        }
        
        val ini = Ini().apply { config = cfg }
        
        return try {
            ini.load(file)
            val linkSection = ini[LINK] ?: return ""
            linkSection[LINK_NAME] ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    @JvmStatic
    fun getVehicleName(path: String): String {
        val file = File("$path/Diag.ini")
        return if (!file.exists()) "INI_LOST" else readFileInfo("$path/Diag.ini")
    }

    private fun readFileInfo(path: String): String {
        var name = ""
        val file = File(path)

        if (file.isDirectory) {
            LLog.d("TestFile", "The File doesn't not exist.")
        } else {
            try {
                FileInputStream(file).use { instream ->
                    InputStreamReader(instream).use { inputreader ->
                        BufferedReader(inputreader).use { buffreader ->
                            val line = buffreader.readLine()
                            if (line != null) {
                                LLog.e("TestFile", "ReadTxtFile: $line")
                                name = line
                            }
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                LLog.d("TestFile", "The File doesn't not exist.")
            } catch (e: IOException) {
                LLog.d("TestFile", e.message ?: "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return name
    }

    @JvmStatic
    fun getVersion(path: String, name: String): String {
        val file = File("$path/Diag.ini")
        if (!file.exists()) {
            LLog.e("bcf", "$name  ini不存在：${file.path}")
            return "INI_LOST"
        }
        
        val cfg = Config().apply {
            isLowerCaseOption = true
            isLowerCaseSection = true
            isMultiSection = true
        }
        
        val ini = Ini().apply { config = cfg }
        
        return try {
            ini.load(file)
            val versionSection = ini[name.lowercase()] ?: return ""
            versionSection[VERSION] ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    @JvmStatic
    fun getName(language: String, path: String): String {
        val file = File("$path/Diag.ini")
        val cfg = Config().apply {
            isLowerCaseOption = true
            isLowerCaseSection = true
            isMultiSection = true
            fileEncoding = Charset.forName("UTF-8")
        }
        
        val ini = Ini().apply { config = cfg }
        
        return try {
            ini.load(file)
            val languageSection = ini[LANGUAGE] ?: return ""
            languageSection[language.lowercase()] ?: ""
        } catch (e: Exception) {
            LLog.e("bcf", "INI: error: ${e.message}")
            ""
        }
    }

    @JvmStatic
    fun getMaintenance(path: String, name: String): HashMap<String, String> {
        val hashMap = HashMap<String, String>()
        val file = File("$path/Diag.ini")
        
        if (!file.exists()) {
            LLog.e("bcf", "$name  ini不存在：${file.path}")
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
            val versionSection = ini[MAINTENANCE.lowercase()] ?: return hashMap
            
            val maintenanceFields = mapOf(
                "base_ver" to "0", "base_rdtc" to "0", "base_cdtc" to "0", "base_rds" to "0",
                "base_act" to "0", "base_fframe" to "0", "oilreset" to "0", "throttle" to "0",
                "epb" to "0", "abs" to "0", "steering" to "0", "dpf" to "0", "airbag" to "0",
                "bms" to "0", "adas" to "0", "immo" to "0", "smart_key" to "0",
                "password_reading" to "0", "brake_replace" to "0", "injector_code" to "0",
                "suspension" to "0", "tire_pressure" to "0", "ransmission" to "0",
                "gearbox_learning" to "0", "transport_mode" to "0", "head_light" to "0",
                "sunroof_init" to "0", "seat_cali" to "0", "window_cali" to "0",
                "start_stop" to "0", "egr" to "0", "odometer" to "0", "language" to "0",
                "tire_modified" to "0", "a_f_adj" to "0", "electronic_pump" to "0",
                "nox_reset" to "0", "urea_reset" to "0", "turbine_learning" to "0",
                "cylinder" to "0", "eeprom" to "0", "exhaust_processing" to "0"
            )
            
            maintenanceFields.forEach { (key, defaultValue) ->
                val value = versionSection[key]
                hashMap[key] = if (!TextUtils.isEmpty(value ?: "")) value ?: defaultValue else defaultValue
            }
            
            hashMap
        } catch (e: Exception) {
            e.printStackTrace()
            hashMap
        }
    }

    @JvmStatic
    fun getIniSysTem(path: String, name: String): HashMap<String, String> {
        val hashMap = HashMap<String, String>()
        val file = File("$path/Diag.ini")
        
        if (!file.exists()) {
            LLog.e("bcf", "$name  ini不存在：${file.path}")
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
            val versionSection = ini[SYSTEM.lowercase()] ?: return hashMap
            
            val systemFields = mapOf(
                "ecm" to "0", "tcm" to "0", "abs" to "0", "srs" to "0",
                "hvac" to "0", "adas" to "0", "immo" to "0", "bms" to "0",
                "eps" to "0", "led" to "0", "ic" to "0", "informa" to "0", "bcm" to "0"
            )
            
            systemFields.forEach { (key, defaultValue) ->
                val value = versionSection[key]
                hashMap[key] = if (!TextUtils.isEmpty(value ?: "")) value ?: defaultValue else defaultValue
            }
            
            hashMap
        } catch (e: Exception) {
            e.printStackTrace()
            hashMap
        }
    }
}