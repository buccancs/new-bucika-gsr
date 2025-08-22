package com.multisensor.recording.testfixtures

import com.multisensor.recording.ui.BatteryStatus
import com.multisensor.recording.ui.MainUiState

object UiStateTestFactory {

    fun createMainUiState(
        statusText: String = "Ready",
        isInitialized: Boolean = true,
        isRecording: Boolean = false,
        recordingDuration: Long = 0L,
        recordingSessionId: String? = null,
        isPcConnected: Boolean = false,
        isShimmerConnected: Boolean = false,
        isThermalConnected: Boolean = false,
        batteryLevel: Int = 85,
        batteryStatus: BatteryStatus = BatteryStatus.DISCHARGING,
        showManualControls: Boolean = true,
        showPermissionsButton: Boolean = false,
        isCalibrationRunning: Boolean = false,
        isStreaming: Boolean = false,
        streamingFrameRate: Double = 0.0,
        streamingDataSize: Long = 0L,
        errorMessage: String? = null,
        showErrorDialog: Boolean = false,
        isLoadingRecording: Boolean = false
    ): MainUiState {
        return MainUiState(
            statusText = statusText,
            isInitialized = isInitialized,
            isRecording = isRecording,
            recordingDuration = recordingDuration,
            recordingSessionId = recordingSessionId,
            isPcConnected = isPcConnected,
            isShimmerConnected = isShimmerConnected,
            isThermalConnected = isThermalConnected,
            batteryLevel = batteryLevel,
            batteryStatus = batteryStatus,
            showManualControls = showManualControls,
            showPermissionsButton = showPermissionsButton,
            isCalibrationRunning = isCalibrationRunning,
            isStreaming = isStreaming,
            streamingFrameRate = streamingFrameRate,
            streamingDataSize = streamingDataSize,
            errorMessage = errorMessage,
            showErrorDialog = showErrorDialog,
            isLoadingRecording = isLoadingRecording
        )
    }

    fun createRecordingState(
        sessionId: String = "test-session-123",
        duration: Long = 15000L
    ): MainUiState {
        return createMainUiState(
            statusText = "Recording...",
            isRecording = true,
            recordingDuration = duration,
            recordingSessionId = sessionId,
            isPcConnected = true,
            isShimmerConnected = true,
            isThermalConnected = true
        )
    }

    fun createConnectedState(): MainUiState {
        return createMainUiState(
            statusText = "All devices connected",
            isPcConnected = true,
            isShimmerConnected = true,
            isThermalConnected = true
        )
    }

    fun createDisconnectedState(): MainUiState {
        return createMainUiState(
            statusText = "Devices disconnected",
            isPcConnected = false,
            isShimmerConnected = false,
            isThermalConnected = false
        )
    }

    fun createErrorState(
        errorMessage: String = "Connection failed"
    ): MainUiState {
        return createMainUiState(
            statusText = "Error occurred",
            errorMessage = errorMessage,
            showErrorDialog = true
        )
    }

    fun createStreamingState(
        frameRate: Double = 30.0,
        dataSize: Long = 2500000L
    ): MainUiState {
        return createMainUiState(
            statusText = "Streaming active",
            isStreaming = true,
            streamingFrameRate = frameRate,
            streamingDataSize = dataSize
        )
    }

    fun createLoadingState(): MainUiState {
        return createMainUiState(
            statusText = "Loading...",
            isLoadingRecording = true
        )
    }
}
