package com.infisense.usbir.utils

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.blankj.utilcode.util.Utils
import com.energy.iruvc.utils.CommonParams
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*

object FileUtil {

    private const val TAG = "FileUtil"
    private const val DATA_SAVE_DIR = "InfiRay"
    private const val BUFFER_SIZE = 524288

    fun getDiskCacheDir(context: Context): String {
        var cachePath = context.cacheDir.path
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() ||
            !Environment.isExternalStorageRemovable()
        ) {
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null) {
                cachePath = externalCacheDir.path
            }
        }
        return cachePath
    }

    @Throws(IOException::class)
    fun copyAssetsDataToSD(context: Context, srcFileName: String, strOutFileName: String) {
        val file = File(strOutFileName)
        if (file.exists()) {
            file.delete()
        }
        val myInput: InputStream = context.assets.open(srcFileName)
        val myOutput: OutputStream = FileOutputStream(strOutFileName)
        val buffer = ByteArray(1024)
        var length = myInput.read(buffer)
        while (length > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }
        myOutput.flush()
        myInput.close()
        myOutput.close()
    }

    fun saveByteFile(context: Context, bytes: ByteArray, fileTitle: String) {
        try {
            val fileSaveDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath
            val path = File(fileSaveDir)
            if (!path.exists() && path.isDirectory) {
                path.mkdirs()
            }
            val fileName = fileTitle + SimpleDateFormat("_HHmmss_yyMMdd", Locale.getDefault())
                .format(Date(System.currentTimeMillis())) + ".bin"
            val file = File(fileSaveDir, fileName)
            Log.i(TAG, "fileSaveDir=$fileSaveDir fileName=$fileName")
            val fos = FileOutputStream(file)
            fos.write(bytes)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveByteFile(bytes: ByteArray, fileTitle: String) {

    }

    fun getTableDirPath(): String {
        return "${Utils.getApp().cacheDir.absolutePath}/table"
    }

    fun saveShortFileForDeviceData(bytes: ShortArray, fileTitle: String) {
        try {
            val fileSaveDir = getTableDirPath()
            createOrExistsDir(fileSaveDir)
            val file = File(fileSaveDir, fileTitle)
            val fos = FileOutputStream(file)
            fos.write(toByteArray(bytes))
            fos.close()
            Log.i(TAG, "$fileTitle 保存成功")
        } catch (e: IOException) {
            Log.e(TAG, "$fileTitle 保存失败：${e.message}")
        }
    }

    fun saveShortFile(bytes: ShortArray, fileTitle: String) {
        try {
            val path = File("/sdcard")
            if (!path.exists() && path.isDirectory) {
                path.mkdirs()
            }
            val file = File(
                "/sdcard/", fileTitle + SimpleDateFormat("_HHmmss_yyMMdd", Locale.getDefault())
                    .format(Date(System.currentTimeMillis())) + ".bin"
            )
            val fos = FileOutputStream(file)
            fos.write(toByteArray(bytes))
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getY16SrcTypeByDataFlowMode(dataFlowMode: CommonParams.DataFlowMode): CommonParams.Y16ModePreviewSrcType? {
        return when (dataFlowMode) {
            CommonParams.DataFlowMode.TEMP_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_TEMPERATURE
            CommonParams.DataFlowMode.IR_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_IR
            CommonParams.DataFlowMode.KBC_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_KBC
            CommonParams.DataFlowMode.HBC_DPC_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_HBC_DPC
            CommonParams.DataFlowMode.VBC_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_VBC
            CommonParams.DataFlowMode.TNR_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_TNR
            CommonParams.DataFlowMode.SNR_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_SNR
            CommonParams.DataFlowMode.AGC_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_AGC
            CommonParams.DataFlowMode.DDE_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_DDE
            CommonParams.DataFlowMode.GAMMA_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_GAMMA
            CommonParams.DataFlowMode.MIRROR_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_MIRROR
            else -> null
        }
    }

    fun createFileDir(dirFile: File?): Boolean {
        if (dirFile == null) return true
        if (dirFile.exists()) {
            return true
        }
        val parentFile = dirFile.parentFile
        return if (parentFile != null && !parentFile.exists()) {

            createFileDir(parentFile) && createFileDir(dirFile)
        } else {
            val mkdirs = dirFile.mkdirs()
            val isSuccess = mkdirs || dirFile.exists()
            if (!isSuccess) {
                Log.e("FileUtil", "createFileDir fail $dirFile")
            }
            isSuccess
        }
    }

    fun createFile(dirPath: String, fileName: String): File? {
        try {
            val dirFile = File(dirPath)
            if (!dirFile.exists()) {
                if (!createFileDir(dirFile)) {
                    Log.e(TAG, "createFile dirFile.mkdirs fail")
                    return null
                }
            } else if (!dirFile.isDirectory) {
                val delete = dirFile.delete()
                return if (delete) {
                    createFile(dirPath, fileName)
                } else {
                    Log.e(TAG, "createFile dirFile !isDirectory and delete fail")
                    null
                }
            }
            val file = File(dirPath, fileName)
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Log.e(TAG, "createFile createNewFile fail")
                    return null
                }
            }
            return file
        } catch (e: Exception) {
            Log.e(TAG, "createFile fail : ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    fun savaRawFile(bytes: ByteArray, bytes2: ByteArray) {
        try {
            val path = File("/sdcard")
            if (!path.exists() && path.isDirectory) {
                path.mkdirs()
            }
            val file = File(
                "/sdcard/", SimpleDateFormat("_HHmmss_yyMMdd", Locale.getDefault())
                    .format(Date(System.currentTimeMillis())) + ".bin"
            )
            val fos = FileOutputStream(file)
            fos.write(bytes)
            fos.write(bytes2)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun savaIRFile(bytes: ByteArray) {
        try {
            val path = File("/sdcard")
            if (!path.exists() && path.isDirectory) {
                path.mkdirs()
            }
            val file = File(
                "/sdcard/", "ir" + SimpleDateFormat("_HHmmss_yyMMdd", Locale.getDefault())
                    .format(Date(System.currentTimeMillis())) + ".bin"
            )
            val fos = FileOutputStream(file)
            fos.write(bytes)
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun savaTempFile(bytes: ByteArray) {
        try {
            val path = File("/sdcard")
            if (!path.exists() && path.isDirectory) {
                path.mkdirs()
            }
            val file = File(
                "/sdcard/", "temp" + SimpleDateFormat("_HHmmss_yyMMdd", Locale.getDefault())
                    .format(Date(System.currentTimeMillis())) + ".bin"
            )
            val fos = FileOutputStream(file)
            fos.write(bytes)
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun isFileExists(context: Context, file: File?): Boolean {
        if (file == null) {
            return false
        }
        if (file.exists()) {
            return true
        }
        return isFileExists(context, file.absolutePath)
    }

    fun isFileExists(context: Context, filePath: String?): Boolean {
        val file = File(filePath ?: return false)
        if (file.exists()) {
            return true
        }
        return isFileExistsApi29(context, filePath)
    }

    private fun isFileExistsApi29(context: Context, filePath: String): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val uri = Uri.parse(filePath)
                val cr: ContentResolver = context.contentResolver
                val afd = cr.openAssetFileDescriptor(uri, "r")
                    ?: return false
                try {
                    afd.close()
                } catch (ignore: IOException) {
                }
            } catch (e: FileNotFoundException) {
                return false
            }
            return true
        }
        return false
    }

    private fun toByteArray(src: ShortArray): ByteArray {
        val count = src.size
        val dest = ByteArray(count shl 1)
        for (i in 0 until count) {
            dest[i * 2] = ((src[i].toInt() shr 8) and 0xFF).toByte()
            dest[i * 2 + 1] = (src[i].toInt() and 0xFF).toByte()
        }
        return dest
    }

    fun toShortArray(src: ByteArray): ShortArray {
        val count = src.size shr 1
        val dest = ShortArray(count)
        for (i in 0 until count) {
            dest[i] = (((src[i * 2].toInt() and 0xFF) shl 8) or (src[2 * i + 1].toInt() and 0xFF)).toShort()
        }
        return dest
    }

    fun saveShortFile(fileDir: String, bytes: ShortArray, fileTitle: String) {

        createOrExistsDir(fileDir)
        try {
            val file = File(fileDir, "$fileTitle.bin")
            createOrExistsDir(file)
            Log.i("TAG", "getAbsolutePath = ${file.absolutePath}")
            val fos = FileOutputStream(file)
            fos.write(toByteArray(bytes))
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createOrExistsDir(file: File) {

        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun createOrExistsDir(fileDir: String) {
        val file = File(fileDir)

        if (!file.exists() && !file.isDirectory) {

            file.mkdir()
        }
    }

    fun readFile2BytesByStream(context: Context, file: File): ByteArray? {
        if (!isFileExists(context, file)) {
            return null
        }
        try {
            var os: ByteArrayOutputStream? = null
            val `is`: InputStream = BufferedInputStream(FileInputStream(file), BUFFER_SIZE)
            try {
                os = ByteArrayOutputStream()
                val b = ByteArray(BUFFER_SIZE)
                var len: Int
                while (`is`.read(b, 0, BUFFER_SIZE).also { len = it } != -1) {
                    os.write(b, 0, len)
                }
                return os.toByteArray()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            } finally {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                try {
                    os?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        }
    }

    fun copyAssetsBigDataToSD(context: Context, srcFileName: String, strOutFileName: String) {
        try {
            val file = File(strOutFileName)
            Log.i(TAG, "file.exists->getAbsolutePath = ${file.absolutePath}")
            if (file.exists()) {

                file.delete()
            }
            if (!file.createNewFile()) {
                Log.e(TAG, "创建文件 $srcFileName 失败")
                return
            }

            val myInput: InputStream = context.assets.open(srcFileName)
            val myOutput: OutputStream = FileOutputStream(strOutFileName)
            val buffer = ByteArray(1024)
            var length = myInput.read(buffer)
            while (length > 0) {
                myOutput.write(buffer, 0, length)
                length = myInput.read(buffer)
            }
            myOutput.flush()
            myInput.close()
            myOutput.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getISPConfigByGainStatus(gainStatus: CommonParams.GainStatus): String {
        return if (CommonParams.GainStatus.HIGH_GAIN == gainStatus) {
            "${infisenseSaveDir()}${File.separator}isp_H.json"
        } else {
            "${infisenseSaveDir()}${File.separator}isp_L.json"
        }
    }

    fun getISPConfigWithEncryptHexByGainStatus(gainStatus: CommonParams.GainStatus): String {
        return if (CommonParams.GainStatus.HIGH_GAIN == gainStatus) {
            "${infisenseSaveDir()}${File.separator}isp_H_encrypt_hex.json"
        } else {
            "${infisenseSaveDir()}${File.separator}isp_L_encrypt_hex.json"
        }
    }

    private fun infisenseSaveDir(): String {
        return Utils.getApp().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath
    }

    private fun deviceDataSaveDir(): String {
        return Utils.getApp().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath
    }

    fun getISPConfigWithEncryptBase64ByGainStatus(gainStatus: CommonParams.GainStatus): String {
        return if (CommonParams.GainStatus.HIGH_GAIN == gainStatus) {
            "${infisenseSaveDir()}${File.separator}isp_H_encrypt_base64.json"
        } else {
            "${infisenseSaveDir()}${File.separator}isp_L_encrypt_base64.json"
        }
    }

    fun getVersionName(context: Context): String? {
        val manager = context.packageManager
        var name: String? = null
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            name = info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return name
    }

    fun getMD5Key(string: String): String {
        if (TextUtils.isEmpty(string)) {
            return ""
        }
        try {
            val md5 = MessageDigest.getInstance("MD5")
            val bytes = md5.digest(string.toByteArray())
            var result = ""
            for (b in bytes) {
                var temp = Integer.toHexString(b.toInt() and 0xff)
                if (temp.length == 1) {
                    temp = "0$temp"
                }
                result += temp
            }
            return result
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    fun makeDirectory(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    fun getSaveFilePath(context: Context): String {
        val useExternalStorage: Boolean
        var directoryPath = ""
        useExternalStorage = if (Environment.getExternalStorageState() == "mounted") {
            Environment.getExternalStorageDirectory().freeSpace > 0
        } else {
            false
        }
        if (useExternalStorage) {
            directoryPath = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                    "${Environment.getExternalStorageDirectory().absolutePath}${File.separator}$DATA_SAVE_DIR${File.separator}"
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}${File.separator}$DATA_SAVE_DIR${File.separator}"
                }
                else -> {
                    "${context.filesDir.absolutePath}${File.separator}$DATA_SAVE_DIR${File.separator}"
                }
            }
        } else {
            directoryPath = "${context.filesDir.absolutePath}${File.separator}$DATA_SAVE_DIR${File.separator}"
        }
        return directoryPath
    }

    @Throws(IOException::class)
    private fun makeFile(filePath: String, fileName: String): File {
        makeDirectory(filePath)
        val file = File(filePath + fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    fun writeTxtToFile(bytes: ByteArray, filePath: String, fileName: String): Int {
        var result = -1
        var fc: FileChannel? = null
        try {
            makeFile(filePath, fileName)
            val file = File(filePath + fileName)
            fc = FileOutputStream(file, false).channel
            if (fc == null) {
                Log.e("FileUtils", "fc is null.")
            }
            fc!!.position(fc.size())
            fc.write(ByteBuffer.wrap(bytes))
            result = 0
        } catch (e: IOException) {
            e.printStackTrace()
            result = -1
        } finally {
            try {
                fc?.close()
            } catch (e: IOException) {
                e.printStackTrace()
                result = -1
            }
            return result
        }
    }

    fun saveStringToFile(str: String, path: String) {
        var stream: FileOutputStream? = null
        try {
            val file = File(path)
            stream = FileOutputStream(file)
            if (!file.exists()) {
                file.createNewFile()
            }
            val contentInBytes = str.toByteArray()
            stream.write(contentInBytes)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun getStringFromFile(path: String): String {
        val txtContent = StringBuffer()
        val b = ByteArray(2048)
        var input: InputStream? = null
        try {
            input = FileInputStream(path)
            var n: Int
            while (input.read(b).also { n = it } != -1) {
                txtContent.append(String(b, 0, n, Charsets.UTF_8))
            }
            input.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return txtContent.toString()
    }

    fun float2Byte(num: Float, numbyte: ByteArray) {
        val fbit = java.lang.Float.floatToIntBits(num)
        for (i in 0 until 4) {
            numbyte[i] = (fbit shr (i * 8)).toByte()
            Log.i(TAG, "numbyte[=$i]=${numbyte[i]}")
        }
    }
}
