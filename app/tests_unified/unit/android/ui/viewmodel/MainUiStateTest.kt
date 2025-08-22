package com.multisensor.recording.ui.viewmodel

import com.google.common.truth.Truth.assertThat
import com.multisensor.recording.testbase.BaseUnitTest
import com.multisensor.recording.testfixtures.UiStateTestFactory
import com.multisensor.recording.ui.BatteryStatus
import org.junit.Test

class MainUiStateTest : BaseUnitTest() {

    @Test
    fun `MainUiState should initialize with correct default values`() {
        val state = UiStateTestFactory.createMainUiState()

        assertThat(state.statusText).isEqualTo("Ready")
        assertThat(state.isInitialized).isTrue()
        assertThat(state.isRecording).isFalse()
        assertThat(state.recordingDuration).isEqualTo(0L)
        assertThat(state.recordingSessionId).isNull()
        assertThat(state.isPcConnected).isFalse()
        assertThat(state.isShimmerConnected).isFalse()
        assertThat(state.isThermalConnected).isFalse()
        assertThat(state.batteryLevel).isEqualTo(85)
        assertThat(state.batteryStatus).isEqualTo(BatteryStatus.DISCHARGING)
        assertThat(state.showManualControls).isTrue()
        assertThat(state.showPermissionsButton).isFalse()
        assertThat(state.isCalibrationRunning).isFalse()
        assertThat(state.isStreaming).isFalse()
        assertThat(state.streamingFrameRate).isEqualTo(0.0)
        assertThat(state.streamingDataSize).isEqualTo(0L)
        assertThat(state.errorMessage).isNull()
        assertThat(state.showErrorDialog).isFalse()
        assertThat(state.isLoadingRecording).isFalse()
    }

    @Test
    fun `recording state should have correct values`() {
        val sessionId = "test-session-123"
        val duration = 15000L

        val state = UiStateTestFactory.createRecordingState(sessionId, duration)

        assertThat(state.statusText).isEqualTo("Recording...")
        assertThat(state.isRecording).isTrue()
        assertThat(state.recordingDuration).isEqualTo(duration)
        assertThat(state.recordingSessionId).isEqualTo(sessionId)
        assertThat(state.isPcConnected).isTrue()
        assertThat(state.isShimmerConnected).isTrue()
        assertThat(state.isThermalConnected).isTrue()
    }

    @Test
    fun `connected state should show all devices connected`() {
        val state = UiStateTestFactory.createConnectedState()

        assertThat(state.statusText).isEqualTo("All devices connected")
        assertThat(state.isPcConnected).isTrue()
        assertThat(state.isShimmerConnected).isTrue()
        assertThat(state.isThermalConnected).isTrue()
    }

    @Test
    fun `disconnected state should show no devices connected`() {
        val state = UiStateTestFactory.createDisconnectedState()

        assertThat(state.statusText).isEqualTo("Devices disconnected")
        assertThat(state.isPcConnected).isFalse()
        assertThat(state.isShimmerConnected).isFalse()
        assertThat(state.isThermalConnected).isFalse()
    }

    @Test
    fun `error state should display error information`() {
        val errorMessage = "Connection failed"

        val state = UiStateTestFactory.createErrorState(errorMessage)

        assertThat(state.statusText).isEqualTo("Error occurred")
        assertThat(state.errorMessage).isEqualTo(errorMessage)
        assertThat(state.showErrorDialog).isTrue()
    }

    @Test
    fun `streaming state should show streaming information`() {
        val frameRate = 30.0
        val dataSize = 2500000L

        val state = UiStateTestFactory.createStreamingState(frameRate, dataSize)

        assertThat(state.statusText).isEqualTo("Streaming active")
        assertThat(state.isStreaming).isTrue()
        assertThat(state.streamingFrameRate).isEqualTo(frameRate)
        assertThat(state.streamingDataSize).isEqualTo(dataSize)
    }

    @Test
    fun `loading state should show loading indicators`() {
        val state = UiStateTestFactory.createLoadingState()

        assertThat(state.statusText).isEqualTo("Loading...")
        assertThat(state.isLoadingRecording).isTrue()
    }

    @Test
    fun `state should be immutable and support copy operations`() {
        val originalState = UiStateTestFactory.createMainUiState()

        val modifiedState = originalState.copy(
            statusText = "Updated status",
            isRecording = true,
            recordingDuration = 5000L
        )

        assertThat(originalState.statusText).isEqualTo("Ready")
        assertThat(originalState.isRecording).isFalse()
        assertThat(originalState.recordingDuration).isEqualTo(0L)

        assertThat(modifiedState.statusText).isEqualTo("Updated status")
        assertThat(modifiedState.isRecording).isTrue()
        assertThat(modifiedState.recordingDuration).isEqualTo(5000L)
    }

    @Test
    fun `battery status enum should handle all values`() {
        val allBatteryStatuses = BatteryStatus.values()

        allBatteryStatuses.forEach { status ->
            val state = UiStateTestFactory.createMainUiState(batteryStatus = status)
            assertThat(state.batteryStatus).isEqualTo(status)
        }
    }

    @Test
    fun `should handle edge case battery levels`() {
        val lowBatteryState = UiStateTestFactory.createMainUiState(batteryLevel = 0)
        val fullBatteryState = UiStateTestFactory.createMainUiState(batteryLevel = 100)
        val invalidBatteryState = UiStateTestFactory.createMainUiState(batteryLevel = -1)

        assertThat(lowBatteryState.batteryLevel).isEqualTo(0)
        assertThat(fullBatteryState.batteryLevel).isEqualTo(100)
        assertThat(invalidBatteryState.batteryLevel).isEqualTo(-1)
    }

    @Test
    fun `should handle long recording durations`() {
        val longDuration = Long.MAX_VALUE / 2

        val state = UiStateTestFactory.createRecordingState(duration = longDuration)

        assertThat(state.recordingDuration).isEqualTo(longDuration)
    }

    @Test
    fun `equals and hashCode should work correctly for data class`() {
        val state1 = UiStateTestFactory.createMainUiState(statusText = "Test")
        val state2 = UiStateTestFactory.createMainUiState(statusText = "Test")
        val state3 = UiStateTestFactory.createMainUiState(statusText = "Different")

        assertThat(state1).isEqualTo(state2)
        assertThat(state1.hashCode()).isEqualTo(state2.hashCode())
        assertThat(state1).isNotEqualTo(state3)
        assertThat(state1.hashCode()).isNotEqualTo(state3.hashCode())
    }
}
