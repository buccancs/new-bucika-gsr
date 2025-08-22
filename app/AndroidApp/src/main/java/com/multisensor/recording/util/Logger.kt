package com.multisensor.recording.util

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Logger
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var fileWriter: FileWriter? = null
    private var currentLogFile: File? = null

    companion object {
        private const val TAG = "MultiSensorRecording"
        private const val LOG_FOLDER = "logs"
        private const val MAX_LOG_FILES = 7
        private const val LOG_FILE_EXTENSION = ".log"
    }

    enum class LogLevel(
        val priority: Int,
    ) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARNING(Log.WARN),
        ERROR(Log.ERROR),
    }

    init {
        initializeFileLogging()
    }

    fun verbose(
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.VERBOSE, message, throwable)
    }

    fun debug(
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.DEBUG, message, throwable)
    }

    fun info(
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.INFO, message, throwable)
    }

    fun warning(
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.WARNING, message, throwable)
    }

    fun error(
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.ERROR, message, throwable)
    }

    private fun log(
        level: LogLevel,
        message: String,
        throwable: Throwable? = null,
    ) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] ${level.name}: $message"

        when (level) {
            LogLevel.VERBOSE -> Log.v(TAG, message, throwable)
            LogLevel.DEBUG -> Log.d(TAG, message, throwable)
            LogLevel.INFO -> Log.i(TAG, message, throwable)
            LogLevel.WARNING -> Log.w(TAG, message, throwable)
            LogLevel.ERROR -> Log.e(TAG, message, throwable)
        }

        try {
            writeToFile(logMessage, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }

    private fun writeToFile(
        message: String,
        throwable: Throwable?,
    ) {
        try {
            ensureLogFileExists()

            fileWriter?.let { writer ->
                writer.appendLine(message)

                throwable?.let { t ->
                    writer.appendLine("Exception: ${t.javaClass.simpleName}: ${t.message}")
                    t.stackTrace.forEach { element ->
                        writer.appendLine("  at $element")
                    }
                }

                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to log file", e)
        }
    }

    private fun ensureLogFileExists() {
        val today = fileDateFormat.format(Date())
        val expectedFileName = "multisensor_$today$LOG_FILE_EXTENSION"

        if (currentLogFile?.name != expectedFileName) {
            fileWriter?.close()

            val logDir = File(context.filesDir, LOG_FOLDER)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            currentLogFile = File(logDir, expectedFileName)
            fileWriter = FileWriter(currentLogFile, true)

            cleanupOldLogFiles(logDir)

            fileWriter?.appendLine("=== Log session started: ${dateFormat.format(Date())} ===")
            fileWriter?.flush()
        }
    }

    private fun cleanupOldLogFiles(logDir: File) {
        try {
            val logFiles =
                logDir
                    .listFiles { _, name ->
                        name.startsWith("multisensor_") && name.endsWith(LOG_FILE_EXTENSION)
                    }?.sortedByDescending { it.lastModified() }

            logFiles?.drop(MAX_LOG_FILES)?.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "Deleted old log file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old log files", e)
        }
    }

    fun getCurrentLogFilePath(): String? = currentLogFile?.absolutePath

    fun getAvailableLogFiles(): List<File> {
        val logDir = File(context.filesDir, LOG_FOLDER)
        return logDir
            .listFiles { _, name ->
                name.startsWith("multisensor_") && name.endsWith(LOG_FILE_EXTENSION)
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun exportLogs(targetDirectory: File): List<File> =
        withContext(Dispatchers.IO) {
            val exportedFiles = mutableListOf<File>()

            try {
                if (!targetDirectory.exists()) {
                    targetDirectory.mkdirs()
                }

                getAvailableLogFiles().forEach { logFile ->
                    val targetFile = File(targetDirectory, logFile.name)
                    logFile.copyTo(targetFile, overwrite = true)
                    exportedFiles.add(targetFile)
                }

                info("Exported ${exportedFiles.size} log files to ${targetDirectory.absolutePath}")
            } catch (e: Exception) {
                error("Failed to export log files", e)
            }

            exportedFiles
        }

    suspend fun clearLogs() =
        withContext(Dispatchers.IO) {
            try {
                fileWriter?.close()
                fileWriter = null

                val logDir = File(context.filesDir, LOG_FOLDER)
                val deletedCount =
                    logDir
                        .listFiles { _, name ->
                            name.startsWith("multisensor_") && name.endsWith(LOG_FILE_EXTENSION)
                        }?.count { it.delete() } ?: 0

                currentLogFile = null

                initializeFileLogging()

                info("Cleared $deletedCount log files")
            } catch (e: Exception) {
                error("Failed to clear log files", e)
            }
        }

    fun getLogStatistics(): LogStatistics {
        val logFiles = getAvailableLogFiles()
        val totalSize = logFiles.sumOf { it.length() }
        val oldestFile = logFiles.minByOrNull { it.lastModified() }
        val newestFile = logFiles.maxByOrNull { it.lastModified() }

        return LogStatistics(
            fileCount = logFiles.size,
            totalSizeBytes = totalSize,
            oldestLogDate = oldestFile?.lastModified(),
            newestLogDate = newestFile?.lastModified(),
            currentLogFile = currentLogFile?.absolutePath,
        )
    }

    data class LogStatistics(
        val fileCount: Int,
        val totalSizeBytes: Long,
        val oldestLogDate: Long?,
        val newestLogDate: Long?,
        val currentLogFile: String?,
    )

    private fun initializeFileLogging() {
        try {
            ensureLogFileExists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize file logging", e)
        }
    }

    fun logSystemInfo() {
        info("=== System Information ===")
        info("Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        info("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        info("App Version: ${getAppVersion()}")
        info("Available Memory: ${getAvailableMemory()} MB")
        info("Storage Space: ${getAvailableStorage()} MB")
        info("=== End System Information ===")
    }

    private fun getAppVersion(): String =
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
            "${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            "Unknown"
        }

    private fun getAvailableMemory(): Long =
        try {
            val runtime = Runtime.getRuntime()
            (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (1024 * 1024)
        } catch (e: Exception) {
            -1
        }

    private fun getAvailableStorage(): Long =
        try {
            context.filesDir.freeSpace / (1024 * 1024)
        } catch (e: Exception) {
            -1
        }

    fun cleanup() {
        try {
            info("Logger cleanup initiated")
            fileWriter?.close()
            fileWriter = null
            currentLogFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during logger cleanup", e)
        }
    }
}
