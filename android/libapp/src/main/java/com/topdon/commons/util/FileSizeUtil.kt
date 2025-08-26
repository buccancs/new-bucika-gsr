package com.topdon.commons.util

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object FileSizeUtil {
    const val SIZETYPE_B = 1
    const val SIZETYPE_KB = 2
    const val SIZETYPE_MB = 3
    const val SIZETYPE_GB = 4
    
    @JvmStatic
    fun getFileOrFilesSize(filePath: String, sizeType: Int): Double {
        val file = File(filePath)
        var blockSize = 0L
        try {
            blockSize = if (file.isDirectory) {
                getFileSizes(file)
            } else {
                getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("bcf获取文件大小", "getFileOrFilesSize-1-获取失败!")
        }
        return formatFileSize(blockSize, sizeType)
    }
    
    @JvmStatic
    fun getUnit(sizeType: Int): String {
        return when (sizeType) {
            SIZETYPE_B -> "B"
            SIZETYPE_KB -> "KB"
            SIZETYPE_MB -> "MB"
            else -> "GB"
        }
    }
    
    @JvmStatic
    fun getFilesSize(filePath: String): Long {
        val file = File(filePath)
        var blockSize = 0L
        try {
            blockSize = if (file.isDirectory) {
                getFileSizes(file)
            } else {
                getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("bcf获取文件大小--getFilesSize-2-获取失败!")
        }
        return blockSize
    }
    
    @JvmStatic
    fun getAutoFileOrFilesSize(filePath: String, sizeType: Int): String {
        val file = File(filePath)
        var blockSize = 0L
        try {
            blockSize = if (file.isDirectory) {
                getFileSizes(file)
            } else {
                getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("bcf获取文件大小", "getAutoFileOrFilesSize-3-获取失败!")
        }
        return formatFileSize(blockSize, sizeType).toString() + getUnit(sizeType)
    }
    
    @JvmStatic
    fun getAutoFileOrFilesSize(filePath: String): String {
        val file = File(filePath)
        var blockSize = 0L
        try {
            blockSize = if (file.isDirectory) {
                getFileSizes(file)
            } else {
                getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("bcf获取文件大小", "getAutoFileOrFilesSize-4-获取失败!")
        }
        return formatFileSize(blockSize)
    }
    
    private fun getFileSize(file: File): Long {
        return try {
            if (file.exists() && file.isFile) {
                FileInputStream(file).use { fis ->
                    fis.channel.use { fc ->
                        if (fc.isOpen) fc.size() else 0
                    }
                }
            } else {
                0
            }
        } catch (e: Exception) {
            println("bcf获取文件大小--getFilesSize-5-获取失败!")
            e.printStackTrace()
            0
        }
    }
    
    private fun getFileSizes(f: File): Long {
        var size = 0L
        val flist = f.listFiles()
        if (flist != null) {
            for (file in flist) {
                size += if (file.isDirectory) {
                    getFileSizes(file)
                } else {
                    getFileSize(file)
                }
            }
        }
        return size
    }
    
    @JvmStatic
    fun formatFileSize(fileS: Long): String {
        val df = DecimalFormat("#.00")
        val wrongSize = "0B"
        if (fileS == 0L) {
            return wrongSize
        }
        
        return when {
            fileS < 1024 -> df.format(fileS.toDouble()) + "B"
            fileS < 1048576 -> df.format(fileS.toDouble() / 1024) + "KB"
            fileS < 1073741824 -> df.format(fileS.toDouble() / 1048576) + "MB"
            else -> df.format(fileS.toDouble() / 1073741824) + "GB"
        }
    }
    
    @JvmStatic
    fun formatFileSize(fileS: Long, sizeType: Int): Double {
        val enLocale = Locale("en", "US")
        val df = NumberFormat.getNumberInstance(enLocale) as DecimalFormat
        df.applyPattern("#.00")
        
        return when (sizeType) {
            SIZETYPE_B -> df.format(fileS.toDouble()).toDouble()
            SIZETYPE_KB -> df.format(fileS.toDouble() / 1024).toDouble()
            SIZETYPE_MB -> df.format(fileS.toDouble() / 1048576).toDouble()
            SIZETYPE_GB -> df.format(fileS.toDouble() / 1073741824).toDouble()
            else -> 0.0
        }
    }
    
    @JvmStatic
    fun getFileSizeByWriteLog(filename: String): Long {
        return try {
            val file = File(filename)
            if (!file.exists() || !file.isFile) {
                println("bcf--getFileSize文件大小不存在")
                -1
            } else {
                file.length()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("bcf--getFileSize获取文件大小--getFilesSize-5-获取失败!")
            0
        }
    }
}