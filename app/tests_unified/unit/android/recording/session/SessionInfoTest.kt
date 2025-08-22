package com.multisensor.recording.recording.session

import com.google.common.truth.Truth.assertThat
import com.multisensor.recording.testbase.BaseUnitTest
import com.multisensor.recording.testfixtures.SessionInfoTestFactory
import org.junit.Test

class SessionInfoTest : BaseUnitTest() {

    @Test
    fun `SessionInfo should initialize with correct default values`() {
        val sessionId = "test-session-123"
        val sessionInfo = SessionInfoTestFactory.createSessionInfo(sessionId = sessionId)

        assertThat(sessionInfo.sessionId).isEqualTo(sessionId)
        assertThat(sessionInfo.videoEnabled).isFalse()
        assertThat(sessionInfo.rawEnabled).isFalse()
        assertThat(sessionInfo.thermalEnabled).isFalse()
        assertThat(sessionInfo.startTime).isEqualTo(0L)
        assertThat(sessionInfo.endTime).isEqualTo(0L)
        assertThat(sessionInfo.videoFilePath).isNull()
        assertThat(sessionInfo.rawFilePaths).isEmpty()
        assertThat(sessionInfo.thermalFilePath).isNull()
        assertThat(sessionInfo.cameraId).isNull()
        assertThat(sessionInfo.videoResolution).isNull()
        assertThat(sessionInfo.rawResolution).isNull()
        assertThat(sessionInfo.thermalResolution).isNull()
        assertThat(sessionInfo.thermalFrameCount).isEqualTo(0L)
        assertThat(sessionInfo.errorOccurred).isFalse()
        assertThat(sessionInfo.errorMessage).isNull()
    }

    @Test
    fun `getDurationMs should calculate correct duration for completed session`() {
        val startTime = 1000L
        val endTime = 5000L
        val sessionInfo = SessionInfoTestFactory.createSessionInfo(
            startTime = startTime,
            endTime = endTime
        )

        val duration = sessionInfo.getDurationMs()

        assertThat(duration).isEqualTo(4000L)
    }

    @Test
    fun `getDurationMs should return zero for session not started`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo()

        val duration = sessionInfo.getDurationMs()

        assertThat(duration).isEqualTo(0L)
    }

    @Test
    fun `getDurationMs should return zero for active session with endTime before startTime`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo(
            startTime = 5000L,
            endTime = 1000L
        )

        val duration = sessionInfo.getDurationMs()

        assertThat(duration).isEqualTo(0L)
    }

    @Test
    fun `isActive should return true when session is started but not completed`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo(
            startTime = System.currentTimeMillis(),
            endTime = 0L
        )

        assertThat(sessionInfo.isActive()).isTrue()
    }

    @Test
    fun `isActive should return false when session is not started`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo()

        assertThat(sessionInfo.isActive()).isFalse()
    }

    @Test
    fun `isActive should return false when session is completed`() {
        val sessionInfo = SessionInfoTestFactory.createCompletedSession()

        assertThat(sessionInfo.isActive()).isFalse()
    }

    @Test
    fun `markCompleted should set endTime when session is active`() {
        val sessionInfo = SessionInfoTestFactory.createActiveSession()
        val originalEndTime = sessionInfo.endTime

        sessionInfo.markCompleted()

        assertThat(sessionInfo.endTime).isNotEqualTo(originalEndTime)
        assertThat(sessionInfo.endTime).isGreaterThan(sessionInfo.startTime)
        assertThat(sessionInfo.isActive()).isFalse()
    }

    @Test
    fun `markCompleted should not change endTime when already set`() {
        val sessionInfo = SessionInfoTestFactory.createCompletedSession()
        val originalEndTime = sessionInfo.endTime

        sessionInfo.markCompleted()

        assertThat(sessionInfo.endTime).isEqualTo(originalEndTime)
    }

    @Test
    fun `addRawFile should add file path to rawFilePaths list`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo()
        val filePath = "/path/to/raw/file.csv"

        sessionInfo.addRawFile(filePath)

        assertThat(sessionInfo.rawFilePaths).containsExactly(filePath)
        assertThat(sessionInfo.getRawImageCount()).isEqualTo(1)
    }

    @Test
    fun `addRawFile should handle multiple files`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo()
        val filePaths = listOf("/path/1.csv", "/path/2.csv", "/path/3.csv")

        filePaths.forEach { sessionInfo.addRawFile(it) }

        assertThat(sessionInfo.rawFilePaths).containsExactlyElementsIn(filePaths)
        assertThat(sessionInfo.getRawImageCount()).isEqualTo(3)
    }

    @Test
    fun `setThermalFile should update thermalFilePath`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo()
        val thermalPath = "/path/to/thermal.bin"

        sessionInfo.setThermalFile(thermalPath)

        assertThat(sessionInfo.thermalFilePath).isEqualTo(thermalPath)
    }

    @Test
    fun `updateThermalFrameCount should update count`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo()
        val frameCount = 1500L

        sessionInfo.updateThermalFrameCount(frameCount)

        assertThat(sessionInfo.thermalFrameCount).isEqualTo(frameCount)
    }

    @Test
    fun `isThermalActive should return true when thermal enabled and file path set`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo(
            thermalEnabled = true,
            thermalFilePath = "/path/to/thermal.bin"
        )

        assertThat(sessionInfo.isThermalActive()).isTrue()
    }

    @Test
    fun `isThermalActive should return false when thermal disabled`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo(
            thermalEnabled = false,
            thermalFilePath = "/path/to/thermal.bin"
        )

        assertThat(sessionInfo.isThermalActive()).isFalse()
    }

    @Test
    fun `isThermalActive should return false when thermal enabled but no file path`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo(
            thermalEnabled = true,
            thermalFilePath = null
        )

        assertThat(sessionInfo.isThermalActive()).isFalse()
    }

    @Test
    fun `getThermalDataSizeMB should calculate correct size for given frame count`() {
        val frameCount = 1000L
        val sessionInfo = SessionInfoTestFactory.createSessionInfo(thermalFrameCount = frameCount)

        val sizeMB = sessionInfo.getThermalDataSizeMB()

        assertThat(sizeMB).isWithin(0.1).of(93.77)
    }

    @Test
    fun `getThermalDataSizeMB should return zero for zero frames`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo()

        val sizeGB = sessionInfo.getThermalDataSizeMB()

        assertThat(sizeGB).isEqualTo(0.0)
    }

    @Test
    fun `markError should set error state and message`() {
        val sessionInfo = SessionInfoTestFactory.createSessionInfo()
        val errorMessage = "Test error occurred"

        sessionInfo.markError(errorMessage)

        assertThat(sessionInfo.errorOccurred).isTrue()
        assertThat(sessionInfo.errorMessage).isEqualTo(errorMessage)
    }

    @Test
    fun `getSummary should return complete session information`() {
        val sessionInfo = SessionInfoTestFactory.createCompletedSession()

        val summary = sessionInfo.getSummary()

        assertThat(summary).contains("SessionInfo[")
        assertThat(summary).contains("id=${sessionInfo.sessionId}")
        assertThat(summary).contains("duration=${sessionInfo.getDurationMs()}ms")
        assertThat(summary).contains("video=enabled")
        assertThat(summary).contains("raw=enabled")
        assertThat(summary).contains("thermal=enabled")
        assertThat(summary).contains("active=${sessionInfo.isActive()}")
    }

    @Test
    fun `getSummary should include error information when error occurred`() {
        val errorMessage = "Test error"
        val sessionInfo = SessionInfoTestFactory.createErrorSession(errorMessage = errorMessage)

        val summary = sessionInfo.getSummary()

        assertThat(summary).contains("ERROR: $errorMessage")
    }
}
