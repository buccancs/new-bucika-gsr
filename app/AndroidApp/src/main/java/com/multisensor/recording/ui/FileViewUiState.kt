package com.multisensor.recording.ui

import java.io.File

data class FileViewUiState(
    val sessions: List<SessionItem> = emptyList(),
    val selectedSessionIndex: Int = -1,
    val sessionFiles: List<FileItem> = emptyList(),
    val selectedFileIndices: Set<Int> = emptySet(),

    val searchQuery: String = "",
    val filteredSessions: List<SessionItem> = emptyList(),

    val totalStorageUsed: Long = 0L,
    val availableStorage: Long = 0L,
    val storageWarningThreshold: Float = 0.8f,

    val showEmptyState: Boolean = false,

    val isLoadingSessions: Boolean = false,
    val isLoadingFiles: Boolean = false,

    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val selectedSession: SessionItem?
        get() = if (selectedSessionIndex >= 0 && selectedSessionIndex < sessions.size) {
            sessions[selectedSessionIndex]
        } else null

    val canDeleteFiles: Boolean
        get() = selectedFileIndices.isNotEmpty() && !isLoadingFiles

    val canDeleteSession: Boolean
        get() = selectedSession != null && !isLoadingSessions

    val canShareFiles: Boolean
        get() = selectedFileIndices.isNotEmpty() && !isLoadingFiles

    val storageUsagePercentage: Float
        get() {
            val totalStorage = totalStorageUsed + availableStorage
            return if (totalStorage > 0) {
                totalStorageUsed.toFloat() / totalStorage.toFloat()
            } else 0f
        }

    val showStorageWarning: Boolean
        get() = storageUsagePercentage > storageWarningThreshold

    val totalFileCount: Int
        get() = sessions.sumOf { it.fileCount }

    val selectedFilesCount: Int
        get() = selectedFileIndices.size

    val selectedFiles: List<FileItem>
        get() = selectedFileIndices.mapNotNull { index ->
            if (index >= 0 && index < sessionFiles.size) {
                sessionFiles[index]
            } else null
        }

    val searchResultsCount: Int
        get() = if (searchQuery.isBlank()) {
            sessions.size
        } else {
            filteredSessions.size
        }
}

data class SessionItem(
    val sessionId: String,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val fileCount: Int,
    val totalSize: Long,
    val deviceTypes: List<String>,
    val status: SessionStatus
) {
    val formattedDuration: String
        get() {
            val seconds = duration / 1000
            val minutes = seconds / 60
            val hours = minutes / 60

            return when {
                hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
                minutes > 0 -> "${minutes}m ${seconds % 60}s"
                else -> "${seconds}s"
            }
        }
}

enum class SessionStatus {
    COMPLETED,
    INTERRUPTED,
    CORRUPTED,
    PROCESSING
}

data class FileItem(
    val file: File,
    val type: FileType,
    val sessionId: String,
    val metadata: String = ""
)

enum class FileType(
    val displayName: String
) {
    VIDEO("Video"),
    RAW_IMAGE("RAW Image"),
    THERMAL("Thermal Data"),
    GSR("GSR Data"),
    METADATA("Metadata"),
    LOG("Log")
}

val File.extension: String
    get() {
        val name = this.name
        val lastDot = name.lastIndexOf('.')
        return if (lastDot >= 0 && lastDot < name.length - 1) {
            name.substring(lastDot + 1)
        } else ""
    }
