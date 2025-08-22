package com.multisensor.recording.recording

data class SessionInfo(
    val sessionId: String,
    var videoEnabled: Boolean = false,
    var rawEnabled: Boolean = false,
    var thermalEnabled: Boolean = false,
    var startTime: Long = 0L,
    var endTime: Long = 0L,
    var videoFilePath: String? = null,
    val rawFilePaths: MutableList<String> = mutableListOf(),
    var thermalFilePath: String? = null,
    var cameraId: String? = null,
    var videoResolution: String? = null,
    var rawResolution: String? = null,
    var thermalResolution: String? = null,
    var thermalFrameCount: Long = 0L,
    var errorOccurred: Boolean = false,
    var errorMessage: String? = null,
) {
    fun getDurationMs(): Long = if (endTime > startTime) endTime - startTime else 0L

    fun getRawImageCount(): Int = rawFilePaths.size

    fun isActive(): Boolean = startTime > 0L && endTime == 0L

    fun markCompleted() {
        if (endTime == 0L) {
            endTime = System.currentTimeMillis()
        }
    }

    fun addRawFile(filePath: String) {
        rawFilePaths.add(filePath)
    }

    fun setThermalFile(filePath: String) {
        thermalFilePath = filePath
    }

    fun updateThermalFrameCount(count: Long) {
        thermalFrameCount = count
    }

    fun isThermalActive(): Boolean = thermalEnabled && thermalFilePath != null

    fun getThermalDataSizeMB(): Double {
        val bytesPerFrame = 256 * 192 * 2 + 8
        return (thermalFrameCount * bytesPerFrame) / (1024.0 * 1024.0)
    }

    fun markError(message: String) {
        errorOccurred = true
        errorMessage = message
    }

    fun isCompleted(): Boolean = endTime > 0L

    fun hasError(): Boolean = errorOccurred

    fun isValid(): Boolean = isCompleted() && !hasError() && sessionId.isNotEmpty()

    fun getThermalFrameRate(): Double {
        if (thermalFrameCount == 0L || !isCompleted()) return 0.0
        val durationSeconds = getDurationMs() / 1000.0
        return if (durationSeconds > 0) thermalFrameCount / durationSeconds else 0.0
    }

    fun getSessionSummary(): String {
        return buildString {
            append("Duration: ${getDurationMs() / 1000.0}s")
            if (videoEnabled) append(", Video: Enabled")
            if (thermalEnabled) append(", Thermal: ${getThermalFrameRate()} fps")
            if (rawEnabled) append(", Raw: ${getRawImageCount()} files")
        }
    }

    fun getSummary(): String =
        buildString {
            append("SessionInfo[")
            append("id=$sessionId, ")
            append("duration=${getDurationMs()}ms, ")
            append("video=${if (videoEnabled) "enabled" else "disabled"}, ")
            append("raw=${if (rawEnabled) "enabled (${getRawImageCount()} files)" else "disabled"}, ")
            append(
                "thermal=${
                    if (thermalEnabled) {
                        "enabled ($thermalFrameCount frames, ${
                            String.format(
                                "%.1f",
                                getThermalDataSizeMB(),
                            )
                        }MB)"
                    } else {
                        "disabled"
                    }
                }, ",
            )
            if (errorOccurred) append("ERROR: $errorMessage, ")
            append("active=${isActive()}")
            append("]")
        }
}
