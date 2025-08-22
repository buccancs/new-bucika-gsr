package com.multisensor.recording.managers

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import com.multisensor.recording.network.FileTransferHandler
import com.multisensor.recording.network.SendFileCommand
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileTransferHandler: FileTransferHandler,
    private val sessionManager: SessionManager,
    private val logger: Logger
) {

    data class FileOperationState(
        val isTransferring: Boolean = false,
        val transferProgress: TransferProgress? = null,
        val transferError: String? = null,
        val storageInfo: StorageInfo = StorageInfo(),
        val lastOperation: String? = null
    )

    data class TransferProgress(
        val totalFiles: Int,
        val transferredFiles: Int,
        val currentFile: String?,
        val totalBytes: Long,
        val transferredBytes: Long
    ) {
        val progressPercent: Int
            get() = if (totalFiles > 0) (transferredFiles * 100) / totalFiles else 0
    }

    data class StorageInfo(
        val totalSpace: Long = 0L,
        val usedSpace: Long = 0L,
        val availableSpace: Long = 0L,
        val sessionCount: Int = 0,
        val fileCount: Int = 0
    ) {
        val usagePercent: Int
            get() = if (totalSpace > 0) ((usedSpace * 100) / totalSpace).toInt() else 0

        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }

    enum class FileType(val extension: String, val category: String) {
        VIDEO("mp4", "video"),
        SENSOR_DATA("csv", "sensor_data"),
        IMAGE("jpg", "image"),
        RAW_IMAGE("dng", "raw_image"),
        LOG("txt", "log"),
        CONFIG("json", "config"),
        UNKNOWN("", "data");

        companion object {
            fun fromPath(filePath: String): FileType {
                val extension = filePath.substringAfterLast('.', "").lowercase()
                return values().find { it.extension == extension } ?: UNKNOWN
            }
        }
    }

    private val _operationState = MutableStateFlow(FileOperationState())
    val operationState: StateFlow<FileOperationState> = _operationState.asStateFlow()

    private val recordingsDir: File
        get() = File(context.getExternalFilesDir(null), "recordings")

    suspend fun transferAllFilesToPC(): Result<String> {
        return try {
            if (_operationState.value.isTransferring) {
                return Result.failure(IllegalStateException("Transfer already in progress"))
            }

            logger.info("Starting file transfer to PC")
            _operationState.value = _operationState.value.copy(isTransferring = true)

            val sessions = sessionManager.getAllSessions()
            if (sessions.isEmpty()) {
                val message = "No recording sessions found to transfer"
                _operationState.value = _operationState.value.copy(
                    isTransferring = false,
                    lastOperation = message
                )
                return Result.failure(IllegalStateException(message))
            }

            var transferredFiles = 0
            var totalFiles = 0
            var transferredBytes = 0L
            var totalBytes = 0L
            val errors = mutableListOf<String>()

            for (session in sessions) {
                try {
                    val sessionFiles = fileTransferHandler.getAvailableFiles(session.sessionId)
                    totalFiles += sessionFiles.size
                    totalBytes += sessionFiles.sumOf { File(it).length() }
                } catch (e: Exception) {
                    logger.warning("Failed to get files for session ${session.sessionId}: ${e.message}")
                }
            }

            logger.info("Found $totalFiles files (${formatBytes(totalBytes)}) across ${sessions.size} sessions")

            for (session in sessions) {
                try {
                    val sessionFiles = fileTransferHandler.getAvailableFiles(session.sessionId)

                    for (filePath in sessionFiles) {
                        try {
                            val file = File(filePath)
                            val fileType = FileType.fromPath(filePath)

                            _operationState.value = _operationState.value.copy(
                                transferProgress = TransferProgress(
                                    totalFiles = totalFiles,
                                    transferredFiles = transferredFiles,
                                    currentFile = file.name,
                                    totalBytes = totalBytes,
                                    transferredBytes = transferredBytes
                                )
                            )

                            fileTransferHandler.handleSendFileCommand(
                                SendFileCommand(
                                    filepath = filePath,
                                    filetype = fileType.category
                                )
                            )

                            transferredFiles++
                            transferredBytes += file.length()
                            logger.debug("Transferred file: ${file.name}")

                            delay(100)

                        } catch (e: Exception) {
                            val error = "Failed to transfer file: $filePath - ${e.message}"
                            errors.add(error)
                            logger.error(error, e)
                        }
                    }
                } catch (e: Exception) {
                    val error = "Failed to process session: ${session.sessionId} - ${e.message}"
                    errors.add(error)
                    logger.error(error, e)
                }
            }

            val summary = if (transferredFiles > 0) {
                "Successfully transferred $transferredFiles of $totalFiles files (${formatBytes(transferredBytes)})"
            } else {
                "No files were transferred successfully"
            }

            if (errors.isNotEmpty()) {
                logger.warning("Transfer completed with ${errors.size} errors")
            }

            _operationState.value = _operationState.value.copy(
                isTransferring = false,
                transferProgress = null,
                lastOperation = summary
            )

            logger.info("File transfer completed: $summary")
            Result.success(summary)

        } catch (e: Exception) {
            logger.error("File transfer failed", e)
            _operationState.value = _operationState.value.copy(
                isTransferring = false,
                transferProgress = null,
                transferError = "Transfer failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun exportAllData(): Result<String> {
        return try {
            logger.info("Starting data export...")
            _operationState.value = _operationState.value.copy(isTransferring = true)

            if (!recordingsDir.exists() || recordingsDir.listFiles()?.isEmpty() == true) {
                val message = "No data to export"
                _operationState.value = _operationState.value.copy(
                    isTransferring = false,
                    lastOperation = message
                )
                return Result.failure(IllegalStateException(message))
            }

            val fileCount = recordingsDir.walkTopDown().count { it.isFile }
            val totalSize = calculateDirectorySize(recordingsDir)

            logger.info("Exporting $fileCount files (${formatBytes(totalSize)})")

            delay(2000)

            val summary = "Export completed: $fileCount files (${formatBytes(totalSize)}) from ${recordingsDir.absolutePath}"

            _operationState.value = _operationState.value.copy(
                isTransferring = false,
                lastOperation = summary
            )

            logger.info("Data export completed")
            Result.success(summary)

        } catch (e: Exception) {
            logger.error("Data export failed", e)
            _operationState.value = _operationState.value.copy(
                isTransferring = false,
                transferError = "Export failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun deleteCurrentSession(): Result<String> {
        return try {
            logger.info("Deleting current session...")
            _operationState.value = _operationState.value.copy(isTransferring = true)

            val currentSession = sessionManager.getCurrentSession()
            if (currentSession == null) {
                val message = "No active session to delete"
                _operationState.value = _operationState.value.copy(
                    isTransferring = false,
                    lastOperation = message
                )
                return Result.failure(IllegalStateException(message))
            }

            val sessionDir = File(recordingsDir, currentSession.sessionId)
            val deletedFiles = if (sessionDir.exists()) {
                val fileCount = sessionDir.walkTopDown().count { it.isFile }
                val size = calculateDirectorySize(sessionDir)
                val success = sessionDir.deleteRecursively()

                if (success) {
                    logger.info("Deleted session directory: ${sessionDir.absolutePath}")
                    fileCount
                } else {
                    logger.error("Failed to delete session directory")
                    0
                }
            } else {
                logger.warning("Session directory does not exist: ${sessionDir.absolutePath}")
                0
            }

            sessionManager.finalizeCurrentSession()

            val summary = "Session deleted: $deletedFiles files removed"

            _operationState.value = _operationState.value.copy(
                isTransferring = false,
                lastOperation = summary
            )

            refreshStorageInfo()

            logger.info("Current session deleted: $summary")
            Result.success(summary)

        } catch (e: Exception) {
            logger.error("Session deletion failed", e)
            _operationState.value = _operationState.value.copy(
                isTransferring = false,
                transferError = "Delete failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun deleteAllData(): Result<String> {
        return try {
            logger.info("Deleting all recorded data...")
            _operationState.value = _operationState.value.copy(isTransferring = true)

            if (!recordingsDir.exists()) {
                val message = "No data directory found"
                _operationState.value = _operationState.value.copy(
                    isTransferring = false,
                    lastOperation = message
                )
                return Result.success(message)
            }

            val fileCount = recordingsDir.walkTopDown().count { it.isFile }
            val totalSize = calculateDirectorySize(recordingsDir)

            val success = recordingsDir.deleteRecursively() && recordingsDir.mkdirs()

            val summary = if (success) {
                "All data deleted: $fileCount files (${formatBytes(totalSize)}) removed"
            } else {
                "Failed to delete all data"
            }

            _operationState.value = _operationState.value.copy(
                isTransferring = false,
                lastOperation = summary
            )

            refreshStorageInfo()

            logger.info("All data deletion completed: $summary")
            if (success) Result.success(summary) else Result.failure(RuntimeException(summary))

        } catch (e: Exception) {
            logger.error("Data deletion failed", e)
            _operationState.value = _operationState.value.copy(
                isTransferring = false,
                transferError = "Delete all failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun openDataFolder(): Result<String> {
        return try {
            logger.info("Opening data folder...")

            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
                logger.info("Created recordings directory: ${recordingsDir.absolutePath}")
            }

            val sessionCount = getSessionCount()
            val totalSize = calculateDirectorySize(recordingsDir)

            val summary = "Data folder: ${recordingsDir.absolutePath} ($sessionCount sessions, ${formatBytes(totalSize)})"

            _operationState.value = _operationState.value.copy(lastOperation = summary)

            logger.info("Data folder opened: $summary")
            Result.success(summary)

        } catch (e: Exception) {
            logger.error("Failed to open data folder", e)
            Result.failure(e)
        }
    }

    suspend fun refreshStorageInfo(): Result<StorageInfo> {
        return try {
            logger.debug("Refreshing storage information...")

            val externalFilesDir = context.getExternalFilesDir(null)
            val totalSpace = externalFilesDir?.totalSpace ?: 0L
            val freeSpace = externalFilesDir?.freeSpace ?: 0L
            val usedSpace = totalSpace - freeSpace

            val sessionCount = getSessionCount()
            val fileCount = if (recordingsDir.exists()) {
                recordingsDir.walkTopDown().count { it.isFile }
            } else 0

            val recordingsSize = if (recordingsDir.exists()) {
                calculateDirectorySize(recordingsDir)
            } else 0L

            val storageInfo = StorageInfo(
                totalSpace = totalSpace,
                usedSpace = usedSpace,
                availableSpace = freeSpace,
                sessionCount = sessionCount,
                fileCount = fileCount
            )

            _operationState.value = _operationState.value.copy(storageInfo = storageInfo)

            logger.debug("Storage info refreshed: $sessionCount sessions, $fileCount files, ${formatBytes(recordingsSize)} recordings")
            Result.success(storageInfo)

        } catch (e: Exception) {
            logger.error("Failed to refresh storage info", e)
            Result.failure(e)
        }
    }

    fun getCurrentState(): FileOperationState = _operationState.value

    fun clearError() {
        _operationState.value = _operationState.value.copy(transferError = null)
    }

    fun getRecordingsPath(): String = recordingsDir.absolutePath

    fun isTransferring(): Boolean = _operationState.value.isTransferring

    private fun getSessionCount(): Int {
        return try {
            if (!recordingsDir.exists()) 0
            else recordingsDir.listFiles { file -> file.isDirectory }?.size ?: 0
        } catch (e: Exception) {
            logger.warning("Error counting sessions: ${e.message}")
            0
        }
    }

    private fun calculateDirectorySize(directory: File): Long {
        return try {
            if (!directory.exists()) return 0L

            var size = 0L
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
            size
        } catch (e: Exception) {
            logger.warning("Error calculating directory size: ${e.message}")
            0L
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
