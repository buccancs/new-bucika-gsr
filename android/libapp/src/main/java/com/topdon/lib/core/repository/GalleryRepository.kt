package com.topdon.lib.core.repository

import android.content.ContentResolver
import android.media.MediaScannerConnection
import android.provider.MediaStore
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.Utils
import com.elvishew.xlog.XLog
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.utils.CommUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.TimeZone

object GalleryRepository {

    enum class DirType {
        LINE,
        TC007,
        TS004_LOCALE,
        TS004_REMOTE,
    }

    private fun copySourDir(sourceDir: File, targetDir: File): Boolean {
        return try {
            if (!sourceDir.exists()) {
                return false
            }
            if (!sourceDir.isDirectory) {
                return false
            }
            val fileList = sourceDir.listFiles()
            if(fileList?.isEmpty() == true){
                return false
            }
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            fileList?.forEach {
                val path = sourceDir.absolutePath + File.separator + it.name
                copyPictureFile(path,targetDir.absolutePath + File.separator + it.name)
            }
            return true
        } catch (ex: Exception) {
            false
        }
    }

    private fun copyPictureFile(oldPath: String, newPath: String): Boolean {
        return try {
            val streamFrom: InputStream = FileInputStream(oldPath)
            val streamTo: OutputStream = FileOutputStream(newPath)
            val buffer = ByteArray(1024)
            var len: Int
            while (streamFrom.read(buffer).also { len = it } > 0) {
                streamTo.write(buffer, 0, len)
            }
            streamFrom.close()
            streamTo.close()
            true
        } catch (ex: Exception) {
            false
        }
    }

    fun readLatest(dirType: DirType): String {
        var firstPath = ""
        try {
            val path = if (dirType == DirType.LINE) FileConfig.lineGalleryDir else FileConfig.tc007GalleryDir
            val dirFile = File(path)
            if (dirFile.isDirectory) {
                val files = dirFile.listFiles()!!

                files.sortByDescending {
                    it.lastModified()
                }
                if (files.isNotEmpty()) {
                    firstPath = "${path}${File.separator}${files.first().name}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            XLog.e("读取图库失败: ${e.message}")
            return ""
        }
        return firstPath
    }

    suspend fun loadByPage(isVideo: Boolean, dirType: DirType, pageNum: Int, pageCount: Int): ArrayList<GalleryBean>? {
        return withContext(Dispatchers.IO) {
            val resultList: ArrayList<GalleryBean> = ArrayList()
            if (dirType == DirType.TS004_REMOTE) {
                val pageList = TS004Repository.getFileByPage(if (isVideo) 1 else 0, pageNum, pageCount) ?: return@withContext null
                pageList.forEach {
                    resultList.add(GalleryBean(isVideo, it))
                }
            } else {
                try {
                    val allFileList = loadAllLocale(isVideo, dirType)
                    val startIndex = pageNum * pageCount - pageCount
                    val endIndex = pageNum * pageCount
                    for (i in startIndex until endIndex) {
                        if (i >= allFileList.size) {
                            break
                        }
                        resultList.add(GalleryBean(allFileList[i]))
                    }
                    if (resultList.isNotEmpty()) {
                        resultList.sortByDescending {
                            it.timeMillis
                        }
                    }
                } catch (e: Exception) {
                    XLog.e("读取图库失败: ${e.message}")
                }
            }

            return@withContext resultList
        }
    }

    suspend fun loadAllReportImg(dirType: DirType): ArrayList<GalleryBean> = withContext(Dispatchers.IO) {
        val resultList: ArrayList<GalleryBean> = ArrayList()
        try {
            val allFileList = loadAllLocale(false, dirType)
            allFileList.forEach {
                resultList.add(GalleryBean(it))
            }
            if (resultList.isNotEmpty()) {
                resultList.sortByDescending {
                    it.timeMillis
                }
            }
        } catch (e: Exception) {
            XLog.e("读取图库失败: ${e.message}")
        }
        return@withContext resultList
    }

    private fun loadAllLocale(isVideo: Boolean, dirType: DirType): ArrayList<File> {
        if (dirType == DirType.LINE) {
            val sourFile = File(FileConfig.gallerySourDir)
            if (sourFile.exists()) {
                val isSuccess = copySourDir(sourFile, File(FileConfig.lineGalleryDir))
                if (isSuccess) {
                    FileUtils.delete(sourFile)
                    MediaScannerConnection.scanFile(Utils.getApp(), arrayOf(FileConfig.lineGalleryDir), null, null)
                }
            }
        }
        val dirFile = when (dirType) {
            DirType.LINE -> File(FileConfig.lineGalleryDir)
            DirType.TC007 -> File(FileConfig.tc007GalleryDir)
            else -> File(FileConfig.ts004GalleryDir)
        }
        var files = dirFile.listFiles { pathname -> pathname?.isFile == true }
        if (files.isNullOrEmpty()) {
            files = loadAllLocaleByMediaStore(dirType)
        }

        val resultList: ArrayList<File> = ArrayList(files.size)
        files.forEach {
            if (it.name.endsWith(if (isVideo) "MP4" else "JPG", true)) {
                resultList.add(it)
            }
        }

        resultList.sortByDescending {
            it.lastModified()
        }
        return resultList
    }

    private fun loadAllLocaleByMediaStore(dirType: DirType): Array<out File> {
        val tc001Files: MutableList<File> = ArrayList()

        val projection = arrayOf(
            MediaStore.Images.Media.DATA
        )

        val selection = MediaStore.Images.Media.DATA + " LIKE ?"
        val path = when (dirType) {
            DirType.LINE -> "%DCIM/${CommUtils.getAppName()}%"
            DirType.TC007 -> "%DCIM/TC007%"
            else -> "%DCIM/TS004%"
        }
        val selectionArgs = arrayOf(path)

        val contentResolver: ContentResolver = Utils.getApp().contentResolver

        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor = contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                val file = File(filePath)
                tc001Files.add(file)
            }
        }
        return tc001Files.toTypedArray()
    }
}
