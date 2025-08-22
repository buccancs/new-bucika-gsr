package com.multisensor.recording.persistence

import android.content.Context
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStateDao: SessionStateDao,
    private val logger: Logger
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun detectCrashRecovery(): Boolean {
        return try {
            val activeSessions = sessionStateDao.getActiveSessions()
            val hasCrashRecovery = activeSessions.isNotEmpty()

            if (hasCrashRecovery) {
                logger.info("CrashRecovery: Found ${activeSessions.size} active sessions requiring recovery")
                activeSessions.forEach { session ->
                    logger.info("CrashRecovery: Session ${session.sessionId} in state ${session.recordingState}")
                }
            } else {
                logger.info("CrashRecovery: No crash recovery needed")
            }

            hasCrashRecovery
        } catch (e: Exception) {
            logger.error("CrashRecovery: Error detecting crash recovery", e)
            false
        }
    }

    suspend fun recoverSession(sessionId: String) {
        try {
            logger.info("CrashRecovery: Starting recovery for session $sessionId")

            val sessionState = sessionStateDao.getSessionState(sessionId)
            if (sessionState != null) {
                val recoveredState = sessionState.copy(
                    recordingState = RecordingState.FAILED,
                    errorOccurred = true,
                    errorMessage = "Session recovered after application crash",
                    timestamp = System.currentTimeMillis()
                )

                sessionStateDao.updateSessionState(recoveredState)
                logger.info("CrashRecovery: Session $sessionId marked as failed due to crash")

                cleanupCorruptedData(sessionId)
            } else {
                logger.warning("CrashRecovery: Session $sessionId not found in database")
            }
        } catch (e: Exception) {
            logger.error("CrashRecovery: Error recovering session $sessionId", e)
        }
    }

    suspend fun cleanupCorruptedData(sessionId: String? = null) {
        try {
            logger.info("CrashRecovery: Starting corrupted data cleanup")

            val recordingDir = File(context.getExternalFilesDir(null), "MultiSensorRecording")

            if (sessionId != null) {
                val sessionDir = File(recordingDir, sessionId)
                if (sessionDir.exists()) {
                    cleanupSessionDirectory(sessionDir)
                }
            } else {
                val activeSessions = sessionStateDao.getActiveSessions()
                activeSessions.forEach { session ->
                    val sessionDir = File(recordingDir, session.sessionId)
                    if (sessionDir.exists()) {
                        cleanupSessionDirectory(sessionDir)
                    }
                }
            }

            logger.info("CrashRecovery: Corrupted data cleanup completed")
        } catch (e: Exception) {
            logger.error("CrashRecovery: Error during corrupted data cleanup", e)
        }
    }

    private fun cleanupSessionDirectory(sessionDir: File) {
        try {
            sessionDir.listFiles()?.forEach { file ->
                when {
                    file.name.endsWith(".mp4") && file.length() < 1024 -> {
                        logger.info("CrashRecovery: Removing incomplete video file ${file.name}")
                        file.delete()
                    }

                    file.name.endsWith(".tmp") -> {
                        logger.info("CrashRecovery: Removing temporary file ${file.name}")
                        file.delete()
                    }

                    file.name.endsWith(".dng") && file.length() < 1024 -> {
                        logger.info("CrashRecovery: Removing incomplete DNG file ${file.name}")
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("CrashRecovery: Error cleaning session directory ${sessionDir.name}", e)
        }
    }

    fun recoverAllActiveSessions() {
        scope.launch {
            try {
                val activeSessions = sessionStateDao.getActiveSessions()
                logger.info("CrashRecovery: Recovering ${activeSessions.size} active sessions")

                activeSessions.forEach { session ->
                    recoverSession(session.sessionId)
                }

                cleanupCorruptedData()

                logger.info("CrashRecovery: All active sessions recovered successfully")
            } catch (e: Exception) {
                logger.error("CrashRecovery: Error during bulk session recovery", e)
            }
        }
    }

    suspend fun cleanupOldSessions(daysToKeep: Int = 30) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            sessionStateDao.deleteOldSessions(cutoffTime)
            logger.info("CrashRecovery: Cleaned up sessions older than $daysToKeep days")
        } catch (e: Exception) {
            logger.error("CrashRecovery: Error cleaning up old sessions", e)
        }
    }
}
