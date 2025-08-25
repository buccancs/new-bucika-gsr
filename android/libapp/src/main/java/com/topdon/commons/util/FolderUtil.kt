package com.topdon.commons.util

import android.text.TextUtils
import android.util.Log
import com.topdon.lms.sdk.LMS
import java.io.File

object FolderUtil {
    @JvmStatic
    var mPath = "/data/user/0/com.topdon.diag.artidiag/files"
    
    @JvmStatic
    var mUserId: String? = null
    
    @JvmStatic
    var fileName: String? = null
    
    @JvmStatic
    var tdartsSn: String? = null

    @JvmStatic
    fun setUserId(userId: String?) {
        mUserId = userId
    }

    @JvmStatic
    fun init() {
        mUserId = PreUtil.getInstance(Topdon.getApp()).get("VCI_" + LMS.getInstance().loginName)
        setUserId(mUserId)
        Log.e("bcf", "FolderUtil mUserId: $mUserId")
        mPath = Topdon.getApp().getExternalFilesDir("")?.absolutePath ?: ""
        Log.e("bcf", "FolderUtil init: $mPath")
        initPath()
    }

    @JvmStatic
    fun initTDarts(tdSn: String?) {
        tdartsSn = tdSn
        val mPath = Topdon.getApp().getExternalFilesDir("")?.absolutePath ?: ""
        Log.e("bcf", "$fileName---FolderUtil initTDarts: $mPath")
        if (!TextUtils.isEmpty(tdSn)) {
            val rfidFile = File("$mPath$fileName$tdSn/RFID/")
            if (!rfidFile.exists()) {
                Log.e("bcf", "$fileName---FolderUtil initTDarts: create")
                rfidFile.mkdirs()
            }
        }
    }

    @JvmStatic
    fun initFilePath() {
        val basePath = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName"
        val downPath = "${basePath}Download/"
        Log.e("bcf", "$fileName--下载路径初始化--$downPath")
        val file = File(downPath)
        if (!file.exists()) {
            Log.e("bcf", "$fileName---下载路径初始化创建 ")
            file.mkdirs()
        }
    }

    private fun initPath() {
        if (!TextUtils.isEmpty(mUserId)) {
            createDirectories()
        }
    }

    private fun createDirectories() {
        val basePath = "$mPath$fileName$mUserId"
        val directories = listOf(
            "/Diagnosis/Asia/", "/Diagnosis/Europe/", "/Diagnosis/America/", 
            "/Diagnosis/China/", "/Diagnosis/Public/", "/Immo/Asia/", 
            "/Immo/Europe/", "/Immo/America/", "/Immo/China/", 
            "/Immo/Australia/", "/RFID/", "/NewEnergy/", "/Shot/", 
            "/Pdf/", "/Datastream/", "/Download/", "/History/Diagnose/", 
            "/History/Service/", "/Gallery/", "/DataLog/", "/FeedbackLog/", 
            "/autovinLog/"
        )
        
        directories.forEach { dir ->
            val file = File("$basePath$dir")
            if (!file.exists()) {
                file.mkdirs()
            }
        }

        val globalDirectories = listOf("App/", "Firmware/", "T-darts/")
        globalDirectories.forEach { dir ->
            val file = File("$mPath$fileName$dir")
            if (!file.exists()) {
                file.mkdirs()
            }
        }

        val userDataDirectories = listOf("UserData/Diagnose/", "UserData/Immo/", 
                                       "UserData/NewEnergy/", "UserData/RFID/")
        userDataDirectories.forEach { dir ->
            val file = File("$mPath$fileName$dir")
            if (!file.exists()) {
                file.mkdirs()
            }
        }
    }

    @JvmStatic
    fun getOtaPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}/s/"

    @JvmStatic
    fun getDataBasePath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName"

    @JvmStatic
    fun getTDartsRootPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$tdartsSn/"

    @JvmStatic
    fun getRootPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/"

    @JvmStatic
    fun getVehiclesPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Diagnosis/"

    @JvmStatic
    fun getImmoPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Immo/"

    @JvmStatic
    fun getRfidTopScanPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$tdartsSn/RFID/"

    @JvmStatic
    fun getRfidPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/RFID/"

    @JvmStatic
    fun getAsiaPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Diagnosis/Asia/"

    @JvmStatic
    fun getAmericaPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Diagnosis/America/"

    @JvmStatic
    fun getEuropePath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Diagnosis/Europe/"

    @JvmStatic
    fun getVehiclePublicPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Diagnosis/Public/"

    @JvmStatic
    fun getVehicleTopScanPublicPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName"

    @JvmStatic
    fun getShotPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Shot/"

    @JvmStatic
    fun getDataStreamPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Datastream/"

    @JvmStatic
    fun getPdfPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Pdf/"

    @JvmStatic
    fun getAppPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}App/"

    @JvmStatic
    fun getFirmwarePath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}Firmware/"

    @JvmStatic
    fun getTdartsUpgradePath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}T-darts/"

    @JvmStatic
    fun getDownloadPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Download/"

    @JvmStatic
    fun getDiagHistoryPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/History/Diagnose/"

    @JvmStatic
    fun getServiceHistoryPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/History/Service/"

    @JvmStatic
    fun getLogPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}Log/"

    @JvmStatic
    fun getSoLogPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}Log/SoLog/"

    @JvmStatic
    fun getGalleryPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/Gallery/"

    @JvmStatic
    fun getDataLogPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/DataLog/"

    @JvmStatic
    fun getDiagDataLogPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/DataLog/DIAG/"

    @JvmStatic
    fun getImmoDataLogPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/DataLog/IMMO/"

    @JvmStatic
    fun getFeedbackLogPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/FeedbackLog/"

    @JvmStatic
    fun getUserDataDiag(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}UserData/Diagnose/"

    @JvmStatic
    fun getUserDataImmo(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}UserData/Immo/"

    @JvmStatic
    fun getUserDataNewEnergy(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}UserData/NewEnergy/"

    @JvmStatic
    fun getUserDataRFID(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}UserData/RFID/"

    @JvmStatic
    fun getSoftDownPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}${fileName}Download/"

    @JvmStatic
    fun getAutoVinLogPath(): String = "${Topdon.getApp().getExternalFilesDir("")?.absolutePath}$fileName$mUserId/autovinLog/"
}