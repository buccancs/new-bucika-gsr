package com.topdon.tc001.managers

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.utils.ImageUtils
import com.topdon.lib.core.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class GalleryFileManagementManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    
    companion object {
        private const val TAG = "GalleryFileManagementManager"
        const val THERMAL_FILE_EXTENSION = ".tir"
        const val EXPORT_IMAGE_EXTENSION = ".jpg"
        const val EXPORT_DATA_EXTENSION = ".csv"
        private const val JPEG_QUALITY = 90
        private const val MAX_IMAGE_SIZE = 2048
    }
    
    private var currentFilePath: String? = null
    private var isFileLoaded = false
    private var fileMetadata: FileMetadata? = null
    
    data class FileMetadata(
        val filePath: String,
        val fileName: String,
        val fileSize: Long,
        val dateModified: Long,
        val imageWidth: Int,
        val imageHeight: Int,
        val thermalDataSize: Int
    )
    
    fun initialize() {
        ensureDirectoriesExist()
    }
    
    suspend fun loadThermalFile(filePath: String): LoadResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext LoadResult.Error("File not found: $filePath")
            }
            
            val fileData = file.readBytes()
            
            if (!validateThermalFileFormat(fileData)) {
                return@withContext LoadResult.Error("Invalid thermal file format")
            }
            
            val metadata = extractFileMetadata(file, fileData)
            
            currentFilePath = filePath
            fileMetadata = metadata
            isFileLoaded = true
            
            LoadResult.Success(fileData, metadata)
            
        } catch (e: Exception) {
            LoadResult.Error("Failed to load file: ${e.message}")
        }
    }
    
    suspend fun saveProcessedImage(bitmap: Bitmap, fileName: String? = null): SaveResult = withContext(Dispatchers.IO) {
        try {
            val outputFileName = fileName ?: generateFileName("processed", EXPORT_IMAGE_EXTENSION)
            val outputFile = File(getExportDirectory(), outputFileName)
            
            outputFile.parentFile?.mkdirs()
            
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            
            scanMediaFile(outputFile.absolutePath)
            
            SaveResult.Success(outputFile.absolutePath, outputFile.length())
            
        } catch (e: Exception) {
            SaveResult.Error("Failed to save image: ${e.message}")
        }
    }
    
    suspend fun exportThermalData(thermalData: FloatArray, fileName: String? = null): SaveResult = withContext(Dispatchers.IO) {
        try {
            val outputFileName = fileName ?: generateFileName("thermal_data", EXPORT_DATA_EXTENSION)
            val outputFile = File(getExportDirectory(), outputFileName)
            
            outputFile.parentFile?.mkdirs()
            
            outputFile.bufferedWriter().use { writer ->
                writer.write("X,Y,Temperature\n")
                
                val width = 256
                val height = 192
                
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val index = y * width + x
                        if (index < thermalData.size) {
                            writer.write("$x,$y,${thermalData[index]}\n")
                        }
                    }
                }
            }
            
            SaveResult.Success(outputFile.absolutePath, outputFile.length())
            
        } catch (e: Exception) {
            SaveResult.Error("Failed to export thermal data: ${e.message}")
        }
    }
    
    suspend fun createBackup(): SaveResult = withContext(Dispatchers.IO) {
        try {
            val currentFile = currentFilePath ?: return@withContext SaveResult.Error("No file loaded")
            val sourceFile = File(currentFile)
            
            if (!sourceFile.exists()) {
                return@withContext SaveResult.Error("Source file not found")
            }
            
            val backupFileName = generateBackupFileName(sourceFile.name)
            val backupFile = File(getBackupDirectory(), backupFileName)
            
            backupFile.parentFile?.mkdirs()
            
            sourceFile.copyTo(backupFile, overwrite = true)
            
            SaveResult.Success(backupFile.absolutePath, backupFile.length())
            
        } catch (e: Exception) {
            SaveResult.Error("Failed to create backup: ${e.message}")
        }
    }
    
    private fun validateThermalFileFormat(fileData: ByteArray): Boolean {

        return fileData.size > 100
    }
    
    private fun extractFileMetadata(file: File, fileData: ByteArray): FileMetadata {
        return FileMetadata(
            filePath = file.absolutePath,
            fileName = file.name,
            fileSize = file.length(),
            dateModified = file.lastModified(),
            imageWidth = 256,
            imageHeight = 192,
            thermalDataSize = fileData.size
        )
    }
    
    private fun generateFileName(prefix: String, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${prefix}_${timestamp}${extension}"
    }
    
    private fun generateBackupFileName(originalName: String): String {
        val nameWithoutExt = originalName.substringBeforeLast(".")
        val extension = originalName.substringAfterLast(".")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${nameWithoutExt}_backup_${timestamp}.${extension}"
    }
    
    private fun getExportDirectory(): File {
        return File(context.getExternalFilesDir(null), "ThermalExports")
    }
    
    private fun getBackupDirectory(): File {
        return File(context.getExternalFilesDir(null), "ThermalBackups")
    }
    
    private fun ensureDirectoriesExist() {
        listOf(getExportDirectory(), getBackupDirectory()).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
    
    private fun scanMediaFile(filePath: String) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(filePath),
            null
        ) { _, _ -> 

        }
    }
    
    fun getCurrentFileInfo(): FileInfo? {
        return if (isFileLoaded && fileMetadata != null) {
            FileInfo(
                filePath = currentFilePath!!,
                metadata = fileMetadata!!,
                isLoaded = isFileLoaded
            )
        } else {
            null
        }
    }
    
    data class FileInfo(
        val filePath: String,
        val metadata: FileMetadata,
        val isLoaded: Boolean
    )
    
    suspend fun cleanupTemporaryFiles(): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        try {
            val tempDir = File(context.cacheDir, "thermal_temp")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.delete()) {
                        deletedCount++
                    }
                }
            }
        } catch (e: Exception) {

        }
        deletedCount
    }
    
    sealed class LoadResult {
        data class Success(val fileData: ByteArray, val metadata: FileMetadata) : LoadResult()
        data class Error(val message: String) : LoadResult()
    }
    
    sealed class SaveResult {
        data class Success(val filePath: String, val fileSize: Long) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
    
    fun cleanup() {
        lifecycleOwner.lifecycleScope.launch {
            cleanupTemporaryFiles()
        }
    }
