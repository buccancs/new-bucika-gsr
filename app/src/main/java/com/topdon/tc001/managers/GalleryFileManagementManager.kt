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

/**
 * Gallery File Management Manager
 * 
 * Handles all file operations for the IRGalleryEditActivity including:
 * - Loading and saving thermal image files
 * - File format conversion and validation
 * - Export operations with multiple formats
 * - File metadata management
 * - Storage optimization and cleanup
 * 
 * This manager encapsulates all file I/O operations to provide consistent
 * file handling and improve error recovery and resource management.
 * 
 * @see IRGalleryEditActivity
 */
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
    
    // File management state
    private var currentFilePath: String? = null
    private var isFileLoaded = false
    private var fileMetadata: FileMetadata? = null
    
    /**
     * File metadata container
     */
    data class FileMetadata(
        val filePath: String,
        val fileName: String,
        val fileSize: Long,
        val dateModified: Long,
        val imageWidth: Int,
        val imageHeight: Int,
        val thermalDataSize: Int
    )
    
    /**
     * Initialize the file management manager
     */
    fun initialize() {
        ensureDirectoriesExist()
    }
    
    /**
     * Load thermal image file from path
     */
    suspend fun loadThermalFile(filePath: String): LoadResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext LoadResult.Error("File not found: $filePath")
            }
            
            // Read file data
            val fileData = file.readBytes()
            
            // Validate file format
            if (!validateThermalFileFormat(fileData)) {
                return@withContext LoadResult.Error("Invalid thermal file format")
            }
            
            // Extract metadata
            val metadata = extractFileMetadata(file, fileData)
            
            // Update state
            currentFilePath = filePath
            fileMetadata = metadata
            isFileLoaded = true
            
            LoadResult.Success(fileData, metadata)
            
        } catch (e: Exception) {
            LoadResult.Error("Failed to load file: ${e.message}")
        }
    }
    
    /**
     * Save processed image to file
     */
    suspend fun saveProcessedImage(bitmap: Bitmap, fileName: String? = null): SaveResult = withContext(Dispatchers.IO) {
        try {
            val outputFileName = fileName ?: generateFileName("processed", EXPORT_IMAGE_EXTENSION)
            val outputFile = File(getExportDirectory(), outputFileName)
            
            // Ensure directory exists
            outputFile.parentFile?.mkdirs()
            
            // Compress and save bitmap
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            
            // Update media scanner
            scanMediaFile(outputFile.absolutePath)
            
            SaveResult.Success(outputFile.absolutePath, outputFile.length())
            
        } catch (e: Exception) {
            SaveResult.Error("Failed to save image: ${e.message}")
        }
    }
    
    /**
     * Export thermal data to CSV format
     */
    suspend fun exportThermalData(thermalData: FloatArray, fileName: String? = null): SaveResult = withContext(Dispatchers.IO) {
        try {
            val outputFileName = fileName ?: generateFileName("thermal_data", EXPORT_DATA_EXTENSION)
            val outputFile = File(getExportDirectory(), outputFileName)
            
            // Ensure directory exists
            outputFile.parentFile?.mkdirs()
            
            // Write CSV data
            outputFile.bufferedWriter().use { writer ->
                writer.write("X,Y,Temperature\n")
                
                // Assuming 256x192 thermal array (adjust based on actual dimensions)
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
    
    /**
     * Create backup of current file
     */
    suspend fun createBackup(): SaveResult = withContext(Dispatchers.IO) {
        try {
            val currentFile = currentFilePath ?: return@withContext SaveResult.Error("No file loaded")
            val sourceFile = File(currentFile)
            
            if (!sourceFile.exists()) {
                return@withContext SaveResult.Error("Source file not found")
            }
            
            val backupFileName = generateBackupFileName(sourceFile.name)
            val backupFile = File(getBackupDirectory(), backupFileName)
            
            // Ensure backup directory exists
            backupFile.parentFile?.mkdirs()
            
            // Copy file
            sourceFile.copyTo(backupFile, overwrite = true)
            
            SaveResult.Success(backupFile.absolutePath, backupFile.length())
            
        } catch (e: Exception) {
            SaveResult.Error("Failed to create backup: ${e.message}")
        }
    }
    
    /**
     * Validate thermal file format
     */
    private fun validateThermalFileFormat(fileData: ByteArray): Boolean {
        // Basic validation - check file header or signature
        // This would be implemented based on the specific thermal file format
        return fileData.size > 100 // Minimum size check
    }
    
    /**
     * Extract metadata from file
     */
    private fun extractFileMetadata(file: File, fileData: ByteArray): FileMetadata {
        return FileMetadata(
            filePath = file.absolutePath,
            fileName = file.name,
            fileSize = file.length(),
            dateModified = file.lastModified(),
            imageWidth = 256, // Default thermal image width
            imageHeight = 192, // Default thermal image height
            thermalDataSize = fileData.size
        )
    }
    
    /**
     * Generate unique filename with timestamp
     */
    private fun generateFileName(prefix: String, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${prefix}_${timestamp}${extension}"
    }
    
    /**
     * Generate backup filename
     */
    private fun generateBackupFileName(originalName: String): String {
        val nameWithoutExt = originalName.substringBeforeLast(".")
        val extension = originalName.substringAfterLast(".")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${nameWithoutExt}_backup_${timestamp}.${extension}"
    }
    
    /**
     * Get export directory path
     */
    private fun getExportDirectory(): File {
        return File(context.getExternalFilesDir(null), "ThermalExports")
    }
    
    /**
     * Get backup directory path
     */
    private fun getBackupDirectory(): File {
        return File(context.getExternalFilesDir(null), "ThermalBackups")
    }
    
    /**
     * Ensure required directories exist
     */
    private fun ensureDirectoriesExist() {
        listOf(getExportDirectory(), getBackupDirectory()).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
    
    /**
     * Scan media file to make it available in gallery
     */
    private fun scanMediaFile(filePath: String) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(filePath),
            null
        ) { _, _ -> 
            // File scan completed
        }
    }
    
    /**
     * Get current file information
     */
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
    
    /**
     * File information container
     */
    data class FileInfo(
        val filePath: String,
        val metadata: FileMetadata,
        val isLoaded: Boolean
    )
    
    /**
     * Clean up temporary files
     */
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
            // Log error but don't fail
        }
        deletedCount
    }
    
    /**
     * Load operation result
     */
    sealed class LoadResult {
        data class Success(val fileData: ByteArray, val metadata: FileMetadata) : LoadResult()
        data class Error(val message: String) : LoadResult()
    }
    
    /**
     * Save operation result
     */
    sealed class SaveResult {
        data class Success(val filePath: String, val fileSize: Long) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        lifecycleOwner.lifecycleScope.launch {
            cleanupTemporaryFiles()
        }
    }
}