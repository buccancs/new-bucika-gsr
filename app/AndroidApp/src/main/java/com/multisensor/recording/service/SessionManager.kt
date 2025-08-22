package com.multisensor.recording.service

import android.content.Context
import com.multisensor.recording.persistence.*
import com.multisensor.recording.service.util.FileStructureManager
import com.multisensor.recording.util.Logger
import com.multisensor.recording.util.ThermalCameraSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    private val thermalSettings: ThermalCameraSettings,
    private val sessionStateDao: SessionStateDao,
    private val crashRecoveryManager: CrashRecoveryManager,
    private val fileStructureManager: FileStructureManager,
) {
    private var currentSession: RecordingSession? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    companion object {
        private const val SESSION_INFO_FILE = "session_info.txt"
        private const val SESSION_CONFIG_FILE = "session_config.json"
    }

    data class RecordingSession(
        val sessionId: String,
        val startTime: Long,
        val sessionFolder: File,
        var endTime: Long? = null,
        var status: SessionStatus = SessionStatus.ACTIVE,
    )

    enum class SessionStatus {
        ACTIVE,
        COMPLETED,
        FAILED,
        CANCELLED,
    }

    suspend fun createNewSession(): String =
        withContext(Dispatchers.IO) {
            try {
                val timestamp = dateFormat.format(Date())
                val sessionId = "session_$timestamp"

                logger.info("Creating new session: $sessionId")

                val sessionFolder = fileStructureManager.createSessionFolder(sessionId)

                val session =
                    RecordingSession(
                        sessionId = sessionId,
                        startTime = System.currentTimeMillis(),
                        sessionFolder = sessionFolder,
                    )

                currentSession = session

                val sessionState = SessionState(
                    sessionId = sessionId,
                    recordingState = RecordingState.STARTING,
                    deviceStates = emptyList(),
                    timestamp = System.currentTimeMillis(),
                    startTime = session.startTime
                )
                sessionStateDao.insertSessionState(sessionState)

                writeSessionInfo(session)
                writeSessionConfig(session)

                logger.info("Session created successfully: $sessionId at ${sessionFolder.absolutePath}")

                sessionId
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                logger.error("Permission error creating session: ${e.message}", e)
                throw e
            } catch (e: java.io.IOException) {
                logger.error("IO error creating session: ${e.message}", e)
                throw e
            } catch (e: IllegalStateException) {
                logger.error("Invalid state creating session: ${e.message}", e)
                throw e
            } catch (e: RuntimeException) {
                logger.error("Runtime error creating session: ${e.message}", e)
                throw e
            }
        }

    fun getCurrentSession(): RecordingSession? = currentSession

    fun getSessionOutputDir(): File? {
        return currentSession?.sessionFolder
    }

    fun addStimulusEvent(timestamp: Long, eventType: String) {
        currentSession?.let { session ->
            try {
                logger.info("Adding stimulus event to session ${session.sessionId}: type=$eventType, timestamp=$timestamp")

                val stimulusFile = fileStructureManager.createStimulusEventsFile(session.sessionFolder)
                stimulusFile.appendText("$timestamp,$eventType,${timestamp - session.startTime}\n")

                logger.info("Stimulus event recorded in: ${stimulusFile.absolutePath}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                logger.error("Permission error adding stimulus event: ${e.message}", e)
            } catch (e: java.io.IOException) {
                logger.error("IO error adding stimulus event: ${e.message}", e)
            } catch (e: IllegalStateException) {
                logger.error("Invalid state adding stimulus event: ${e.message}", e)
            } catch (e: RuntimeException) {
                logger.error("Runtime error adding stimulus event: ${e.message}", e)
            }
        } ?: logger.warning("Cannot add stimulus event: no active session")
    }

    suspend fun finalizeCurrentSession() =
        withContext(Dispatchers.IO) {
            currentSession?.let { session ->
                try {
                    logger.info("Finalizing session: ${session.sessionId}")

                    session.endTime = System.currentTimeMillis()
                    session.status = SessionStatus.COMPLETED

                    val existingState = sessionStateDao.getSessionState(session.sessionId)
                    if (existingState != null) {
                        val updatedState = existingState.copy(
                            recordingState = RecordingState.COMPLETED,
                            endTime = session.endTime!!,
                            timestamp = System.currentTimeMillis()
                        )
                        sessionStateDao.updateSessionState(updatedState)
                    }

                    writeSessionInfo(session)

                    logSessionSummary(session)

                    logger.info("Session finalized: ${session.sessionId}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SecurityException) {
                    logger.error("Permission error finalizing session ${session.sessionId}: ${e.message}", e)
                    session.status = SessionStatus.FAILED
                    writeSessionInfo(session)
                } catch (e: java.io.IOException) {
                    logger.error("IO error finalizing session ${session.sessionId}: ${e.message}", e)
                    session.status = SessionStatus.FAILED
                    writeSessionInfo(session)
                } catch (e: IllegalStateException) {
                    logger.error("Invalid state finalizing session ${session.sessionId}: ${e.message}", e)
                    session.status = SessionStatus.FAILED
                    writeSessionInfo(session)
                } catch (e: RuntimeException) {
                    logger.error("Runtime error finalizing session ${session.sessionId}: ${e.message}", e)
                    session.status = SessionStatus.FAILED
                    writeSessionInfo(session)
                } finally {
                    currentSession = null
                }
            }
        }

    suspend fun initializeCrashRecovery() {
        try {
            logger.info("SessionManager: Initializing crash recovery")

            val needsRecovery = crashRecoveryManager.detectCrashRecovery()
            if (needsRecovery) {
                logger.info("SessionManager: Crash recovery needed - starting recovery process")
                crashRecoveryManager.recoverAllActiveSessions()
            } else {
                logger.info("SessionManager: No crash recovery needed")
            }

            crashRecoveryManager.cleanupOldSessions(30)
        } catch (e: Exception) {
            logger.error("SessionManager: Error during crash recovery initialization", e)
        }
    }

    suspend fun updateSessionDeviceStates(deviceStates: List<DeviceState>) {
        currentSession?.let { session ->
            try {
                val existingState = sessionStateDao.getSessionState(session.sessionId)
                if (existingState != null) {
                    val updatedState = existingState.copy(
                        deviceStates = deviceStates,
                        timestamp = System.currentTimeMillis()
                    )
                    sessionStateDao.updateSessionState(updatedState)
                }
            } catch (e: Exception) {
                logger.error("SessionManager: Error updating session device states", e)
            }
        }
    }

    suspend fun updateSessionRecordingState(recordingState: RecordingState) {
        currentSession?.let { session ->
            try {
                val existingState = sessionStateDao.getSessionState(session.sessionId)
                if (existingState != null) {
                    val updatedState = existingState.copy(
                        recordingState = recordingState,
                        timestamp = System.currentTimeMillis()
                    )
                    sessionStateDao.updateSessionState(updatedState)
                }
            } catch (e: Exception) {
                logger.error("SessionManager: Error updating session recording state", e)
            }
        }
    }

    fun getSessionFilePaths(): FileStructureManager.SessionFilePaths? =
        currentSession?.let { session ->
            fileStructureManager.getSessionFilePaths(session.sessionFolder)
        }

    private fun writeSessionInfo(session: RecordingSession) {
        try {
            val infoFile = File(session.sessionFolder, SESSION_INFO_FILE)
            val duration = session.endTime?.let { it - session.startTime } ?: 0

            val info =
                buildString {
                    appendLine("Session ID: ${session.sessionId}")
                    appendLine("Start Time: ${Date(session.startTime)}")
                    session.endTime?.let {
                        appendLine("End Time: ${Date(it)}")
                        appendLine("Duration: ${duration / 1000} seconds")
                    }
                    appendLine("Status: ${session.status}")
                    appendLine("Session Folder: ${session.sessionFolder.absolutePath}")
                    appendLine("Created: ${Date()}")
                    appendLine("")
                    appendLine("=== Folder Structure ===")
                    appendLine("RGB Video: rgb_video.mp4")
                    appendLine("Thermal Video: thermal_video.mp4")
                    appendLine("Thermal Data: thermal_data/")
                    appendLine("Raw Frames: raw_frames/")
                    appendLine("Shimmer Data: shimmer_data.csv")
                    appendLine("Calibration: calibration/")
                    appendLine("Session Config: $SESSION_CONFIG_FILE")
                    appendLine("Log File: session_log.txt")
                }

            infoFile.writeText(info)
        } catch (e: Exception) {
            logger.error("Failed to write session info", e)
        }
    }

    private fun writeSessionConfig(session: RecordingSession) {
        try {
            val configFile = File(session.sessionFolder, SESSION_CONFIG_FILE)
            val thermalConfig = thermalSettings.getCurrentConfig()

            val configJson = buildString {
                appendLine("{")
                appendLine("  \"session_id\": \"${session.sessionId}\",")
                appendLine("  \"start_time\": ${session.startTime},")
                appendLine("  \"timestamp\": \"${Date(session.startTime)}\",")
                appendLine("  \"thermal_camera\": {")
                appendLine("    \"enabled\": ${thermalConfig.isEnabled},")
                appendLine("    \"frame_rate\": ${thermalConfig.frameRate},")
                appendLine("    \"color_palette\": \"${thermalConfig.colorPalette}\",")
                appendLine("    \"temperature_range\": \"${thermalConfig.temperatureRange}\",")
                appendLine("    \"emissivity\": ${thermalConfig.emissivity},")
                appendLine("    \"auto_calibration\": ${thermalConfig.autoCalibration},")
                appendLine("    \"high_resolution\": ${thermalConfig.highResolution},")
                appendLine("    \"temperature_units\": \"${thermalConfig.temperatureUnits}\",")
                appendLine("    \"usb_priority\": ${thermalConfig.usbPriority},")
                appendLine("    \"data_format\": \"${thermalConfig.dataFormat}\"")
                appendLine("  },")
                appendLine("  \"folder_structure\": {")
                appendLine("    \"rgb_video\": \"rgb_video.mp4\",")
                appendLine("    \"thermal_video\": \"thermal_video.mp4\",")
                appendLine("    \"thermal_data_folder\": \"thermal_data\",")
                appendLine("    \"raw_frames_folder\": \"raw_frames\",")
                appendLine("    \"shimmer_data\": \"shimmer_data.csv\",")
                appendLine("    \"calibration_folder\": \"calibration\",")
                appendLine("    \"log_file\": \"session_log.txt\"")
                appendLine("  }")
                appendLine("}")
            }

            configFile.writeText(configJson)
            logger.debug("Session configuration written to: ${configFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to write session config", e)
        }
    }

    private fun logSessionSummary(session: RecordingSession) {
        try {
            val duration = session.endTime?.let { it - session.startTime } ?: 0
            val filePaths = fileStructureManager.getSessionFilePaths(session.sessionFolder)

            logger.info("Session Summary:")
            logger.info("  Session ID: ${session.sessionId}")
            logger.info("  Duration: ${duration / 1000} seconds")
            logger.info("  RGB Video: ${if (filePaths.rgbVideoFile.exists()) "✓" else "✗"} (${filePaths.rgbVideoFile.length()} bytes)")
            logger.info(
                "  Thermal Video: ${if (filePaths.thermalVideoFile.exists()) "✓" else "✗"} (${filePaths.thermalVideoFile.length()} bytes)",
            )
            logger.info("  Raw Frames: ${filePaths.rawFramesFolder.listFiles()?.size ?: 0} files")
            logger.info(
                "  Shimmer Data: ${if (filePaths.shimmerDataFile.exists()) "✓" else "✗"} (${filePaths.shimmerDataFile.length()} bytes)",
            )
            logger.info("  Session Folder: ${session.sessionFolder.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to log session summary", e)
        }
    }

    fun getBaseRecordingFolder(): File = fileStructureManager.getBaseRecordingFolder()

    fun getAvailableStorageSpace(): Long =
        try {
            val baseFolder = getBaseRecordingFolder()
            baseFolder.freeSpace
        } catch (e: Exception) {
            logger.error("Failed to get available storage space", e)
            0L
        }

    fun hasSufficientStorage(requiredSpaceBytes: Long = 1024 * 1024 * 1024): Boolean {
        return getAvailableStorageSpace() > requiredSpaceBytes
    }

    suspend fun getAllSessions(): List<com.multisensor.recording.recording.SessionInfo> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val baseFolder = getBaseRecordingFolder()
                val sessions = mutableListOf<com.multisensor.recording.recording.SessionInfo>()

                if (!baseFolder.exists()) {
                    logger.info("Base recording folder does not exist")
                    return@withContext emptyList()
                }

                baseFolder.listFiles()?.forEach { sessionFolder: File ->
                    if (sessionFolder.isDirectory() && sessionFolder.name.startsWith("session_")) {
                        try {
                            val sessionInfo = reconstructSessionInfo(sessionFolder)
                            if (sessionInfo != null) {
                                sessions.add(sessionInfo)
                            }
                        } catch (e: Exception) {
                            logger.error("Failed to reconstruct session from folder: ${sessionFolder.name}", e)
                        }
                    }
                }

                sessions.sortedByDescending { it.startTime }
            } catch (e: Exception) {
                logger.error("Failed to get all sessions", e)
                emptyList()
            }
        }

    private fun reconstructSessionInfo(sessionFolder: File): com.multisensor.recording.recording.SessionInfo? =
        try {
            val sessionId = sessionFolder.name

            val infoFile = File(sessionFolder, SESSION_INFO_FILE)
            var startTime = sessionFolder.lastModified()
            var endTime = 0L
            var errorOccurred = false
            var errorMessage: String? = null

            if (infoFile.exists()) {
                try {
                    val infoContent = infoFile.readText()
                    infoContent.lines().forEach { line ->
                        when {
                            line.startsWith("Start Time:") -> {
                            }

                            line.startsWith("End Time:") -> {
                            }

                            line.startsWith("Status: FAILED") -> {
                                errorOccurred = true
                                errorMessage = "Session failed"
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.debug("Could not parse session info file for $sessionId", e)
                }
            }

            val filePaths = fileStructureManager.getSessionFilePaths(sessionFolder)

            val videoEnabled = filePaths.rgbVideoFile.exists() && filePaths.rgbVideoFile.length() > 0
            val thermalEnabled = filePaths.thermalVideoFile.exists() && filePaths.thermalVideoFile.length() > 0
            val rawEnabled = filePaths.rawFramesFolder.exists() && (filePaths.rawFramesFolder.listFiles()?.isNotEmpty() == true)

            val sessionInfo =
                com.multisensor.recording.recording.SessionInfo(
                    sessionId = sessionId,
                    videoEnabled = videoEnabled,
                    rawEnabled = rawEnabled,
                    thermalEnabled = thermalEnabled,
                    startTime = startTime,
                    endTime = if (endTime > 0) endTime else System.currentTimeMillis(),
                    videoFilePath = if (videoEnabled) filePaths.rgbVideoFile.absolutePath else null,
                    thermalFilePath = if (thermalEnabled) filePaths.thermalVideoFile.absolutePath else null,
                    errorOccurred = errorOccurred,
                    errorMessage = errorMessage,
                )

            if (rawEnabled) {
                filePaths.rawFramesFolder.listFiles()?.forEach { rawFile: File ->
                    if (rawFile.isFile() && rawFile.name.endsWith(".dng")) {
                        sessionInfo.addRawFile(rawFile.absolutePath)
                    }
                }
            }

            if (thermalEnabled) {
                val thermalFileSize = filePaths.thermalVideoFile.length()
                val estimatedFrameCount = thermalFileSize / (256 * 192 * 2 + 8)
                sessionInfo.updateThermalFrameCount(estimatedFrameCount)
            }

            if (endTime > 0) {
                sessionInfo.markCompleted()
            }

            logger.debug("Reconstructed session: ${sessionInfo.getSummary()}")
            sessionInfo
        } catch (e: Exception) {
            logger.error("Failed to reconstruct session info from folder: ${sessionFolder.name}", e)
            null
        }

    suspend fun deleteAllSessions(): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val (deletedCount, failedCount) = fileStructureManager.deleteAllSessionFolders()
                logger.info("Session deletion complete - Deleted: $deletedCount, Failed: $failedCount")
                deletedCount > 0 && failedCount == 0
            } catch (e: Exception) {
                logger.error("Failed to delete all sessions", e)
                false
            }
        }

}
