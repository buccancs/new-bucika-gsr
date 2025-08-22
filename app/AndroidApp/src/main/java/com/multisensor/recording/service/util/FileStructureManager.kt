package com.multisensor.recording.service.util

import android.content.Context
import android.os.Environment
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStructureManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {

    companion object {
        private const val BASE_FOLDER_NAME = "MultiSensorRecording"
        private const val SESSION_INFO_FILE = "session_info.txt"
        private const val SESSION_CONFIG_FILE = "session_config.json"
        private const val RGB_VIDEO_FILE = "rgb_video.mp4"
        private const val THERMAL_VIDEO_FILE = "thermal_video.mp4"
        private const val THERMAL_DATA_FOLDER = "thermal_data"
        private const val RAW_FRAMES_FOLDER = "raw_frames"
        private const val SHIMMER_DATA_FILE = "shimmer_data.csv"
        private const val LOG_FILE = "session_log.txt"
        private const val CALIBRATION_FOLDER = "calibration"
        private const val STIMULUS_EVENTS_FILE = "stimulus_events.csv"
    }

    data class SessionFilePaths(
        val sessionFolder: File,
        val sessionInfoFile: File,
        val sessionConfigFile: File,
        val rgbVideoFile: File,
        val thermalVideoFile: File,
        val thermalDataFolder: File,
        val rawFramesFolder: File,
        val shimmerDataFile: File,
        val logFile: File,
        val calibrationFolder: File,
        val stimulusEventsFile: File
    )

    fun getBaseRecordingFolder(): File {
        val baseDir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(Environment.getExternalStorageDirectory(), BASE_FOLDER_NAME)
        } else {
            File(context.filesDir, BASE_FOLDER_NAME)
        }

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            logger.warning("Failed to create base recording folder, using app internal storage")
            return File(context.filesDir, BASE_FOLDER_NAME).apply {
                if (!exists()) mkdirs()
            }
        }

        return baseDir
    }

    fun createSessionFolder(sessionId: String): File {
        val baseFolder = getBaseRecordingFolder()
        val sessionFolder = File(baseFolder, sessionId)

        if (!sessionFolder.exists() && !sessionFolder.mkdirs()) {
            throw Exception("Failed to create session folder: ${sessionFolder.absolutePath}")
        }

        createSessionSubfolders(sessionFolder)
        logger.info("Created session folder structure: ${sessionFolder.absolutePath}")

        return sessionFolder
    }

    fun createSessionSubfolders(sessionFolder: File) {
        val folders = mapOf(
            RAW_FRAMES_FOLDER to "raw frames",
            THERMAL_DATA_FOLDER to "thermal data",
            CALIBRATION_FOLDER to "calibration"
        )

        folders.forEach { (folderName, description) ->
            val folder = File(sessionFolder, folderName)
            if (!folder.exists() && !folder.mkdirs()) {
                logger.warning("Failed to create $description folder: ${folder.absolutePath}")
            }
        }
    }

    fun getSessionFilePaths(sessionFolder: File): SessionFilePaths {
        return SessionFilePaths(
            sessionFolder = sessionFolder,
            sessionInfoFile = File(sessionFolder, SESSION_INFO_FILE),
            sessionConfigFile = File(sessionFolder, SESSION_CONFIG_FILE),
            rgbVideoFile = File(sessionFolder, RGB_VIDEO_FILE),
            thermalVideoFile = File(sessionFolder, THERMAL_VIDEO_FILE),
            thermalDataFolder = File(sessionFolder, THERMAL_DATA_FOLDER),
            rawFramesFolder = File(sessionFolder, RAW_FRAMES_FOLDER),
            shimmerDataFile = File(sessionFolder, SHIMMER_DATA_FILE),
            logFile = File(sessionFolder, LOG_FILE),
            calibrationFolder = File(sessionFolder, CALIBRATION_FOLDER),
            stimulusEventsFile = File(sessionFolder, STIMULUS_EVENTS_FILE)
        )
    }

    fun getAllSessionFolders(): List<File> {
        return try {
            val baseFolder = getBaseRecordingFolder()
            if (!baseFolder.exists()) {
                logger.info("Base recording folder does not exist")
                return emptyList()
            }

            baseFolder.listFiles { file ->
                file.isDirectory && file.name.startsWith("session_")
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to list session folders", e)
            emptyList()
        }
    }

    fun deleteSessionFolder(sessionFolder: File): Boolean {
        return try {
            deleteRecursively(sessionFolder)
        } catch (e: Exception) {
            logger.error("Failed to delete session folder: ${sessionFolder.absolutePath}", e)
            false
        }
    }

    fun deleteAllSessionFolders(): Pair<Int, Int> {
        var deletedCount = 0
        var failedCount = 0

        try {
            val sessionFolders = getAllSessionFolders()
            logger.info("Attempting to delete ${sessionFolders.size} session folders")

            sessionFolders.forEach { folder ->
                if (deleteSessionFolder(folder)) {
                    deletedCount++
                    logger.info("Deleted session folder: ${folder.name}")
                } else {
                    failedCount++
                    logger.warning("Failed to delete session folder: ${folder.name}")
                }
            }

            logger.info("Session cleanup completed: $deletedCount deleted, $failedCount failed")
        } catch (e: Exception) {
            logger.error("Error during session folder cleanup", e)
        }

        return Pair(deletedCount, failedCount)
    }

    fun validateSessionStructure(sessionFolder: File): Boolean {
        if (!sessionFolder.exists() || !sessionFolder.isDirectory) {
            return false
        }

        val requiredFolders = listOf(RAW_FRAMES_FOLDER, THERMAL_DATA_FOLDER, CALIBRATION_FOLDER)
        return requiredFolders.all { folderName ->
            File(sessionFolder, folderName).exists()
        }
    }

    fun getSessionFolderSize(sessionFolder: File): Long {
        return try {
            calculateFolderSize(sessionFolder)
        } catch (e: Exception) {
            logger.error("Failed to calculate session folder size: ${sessionFolder.absolutePath}", e)
            -1L
        }
    }

    fun createStimulusEventsFile(sessionFolder: File): File {
        val stimulusFile = File(sessionFolder, STIMULUS_EVENTS_FILE)
        if (!stimulusFile.exists()) {
            stimulusFile.writeText("timestamp_ms,event_type,session_time_ms\n")
            logger.info("Created stimulus events file: ${stimulusFile.absolutePath}")
        }
        return stimulusFile
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                if (!deleteRecursively(child)) {
                    return false
                }
            }
        }
        return file.delete()
    }

    private fun calculateFolderSize(folder: File): Long {
        var size = 0L
        if (folder.isDirectory) {
            folder.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateFolderSize(file)
                } else {
                    file.length()
                }
            }
        } else {
            size = folder.length()
        }
        return size
    }
}