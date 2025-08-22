package com.multisensor.recording.testfixtures

import com.multisensor.recording.recording.SessionInfo

object SessionInfoTestFactory {

    fun createSessionInfo(
        sessionId: String = "test-session-${System.currentTimeMillis()}",
        videoEnabled: Boolean = false,
        rawEnabled: Boolean = false,
        thermalEnabled: Boolean = false,
        startTime: Long = 0L,
        endTime: Long = 0L,
        videoFilePath: String? = null,
        rawFilePaths: MutableList<String> = mutableListOf(),
        thermalFilePath: String? = null,
        cameraId: String? = null,
        videoResolution: String? = null,
        rawResolution: String? = null,
        thermalResolution: String? = null,
        thermalFrameCount: Long = 0L,
        errorOccurred: Boolean = false,
        errorMessage: String? = null
    ): SessionInfo {
        return SessionInfo(
            sessionId = sessionId,
            videoEnabled = videoEnabled,
            rawEnabled = rawEnabled,
            thermalEnabled = thermalEnabled,
            startTime = startTime,
            endTime = endTime,
            videoFilePath = videoFilePath,
            rawFilePaths = rawFilePaths,
            thermalFilePath = thermalFilePath,
            cameraId = cameraId,
            videoResolution = videoResolution,
            rawResolution = rawResolution,
            thermalResolution = thermalResolution,
            thermalFrameCount = thermalFrameCount,
            errorOccurred = errorOccurred,
            errorMessage = errorMessage
        )
    }

    fun createActiveSession(
        sessionId: String = "active-session-${System.currentTimeMillis()}"
    ): SessionInfo {
        return createSessionInfo(
            sessionId = sessionId,
            videoEnabled = true,
            rawEnabled = true,
            thermalEnabled = true,
            startTime = System.currentTimeMillis() - 10000,
            cameraId = "0",
            videoResolution = "1920x1080",
            rawResolution = "1920x1080",
            thermalResolution = "256x192"
        )
    }

    fun createCompletedSession(
        sessionId: String = "completed-session-${System.currentTimeMillis()}"
    ): SessionInfo {
        val startTime = System.currentTimeMillis() - 60000
        return createSessionInfo(
            sessionId = sessionId,
            videoEnabled = true,
            rawEnabled = true,
            thermalEnabled = true,
            startTime = startTime,
            endTime = startTime + 30000,
            videoFilePath = "/storage/emulated/0/MultiSensorRecording/video_$sessionId.mp4",
            rawFilePaths = mutableListOf(
                "/storage/emulated/0/MultiSensorRecording/raw_$sessionId.csv"
            ),
            thermalFilePath = "/storage/emulated/0/MultiSensorRecording/thermal_$sessionId.bin",
            cameraId = "0",
            videoResolution = "1920x1080",
            rawResolution = "1920x1080",
            thermalResolution = "256x192",
            thermalFrameCount = 1800
        )
    }

    fun createErrorSession(
        sessionId: String = "error-session-${System.currentTimeMillis()}",
        errorMessage: String = "Test error occurred"
    ): SessionInfo {
        return createSessionInfo(
            sessionId = sessionId,
            videoEnabled = true,
            rawEnabled = true,
            thermalEnabled = true,
            startTime = System.currentTimeMillis() - 5000,
            errorOccurred = true,
            errorMessage = errorMessage
        )
    }
}
