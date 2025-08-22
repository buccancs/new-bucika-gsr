package com.multisensor.recording.ui.viewmodel
import com.google.common.truth.Truth.assertThat
import com.multisensor.recording.recording.DeviceStatus
import com.multisensor.recording.ui.MainUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
@ExperimentalCoroutinesApi
class RecordingViewModelStateTest {
    @Test
    fun `should update recording state correctly`() = runTest {
        val initialState = MainUiState()
        val recordingState = initialState.copy(
            isRecording = true,
            statusText = "Recording in progress",
            recordingSessionId = "test_session_123"
        )
        assertThat(recordingState.isRecording).isTrue()
        assertThat(recordingState.statusText).contains("Recording")
        assertThat(recordingState.recordingSessionId).isEqualTo("test_session_123")
    }
    @Test
    fun `should update recording stop state correctly`() = runTest {
        val recordingState = MainUiState(
            isRecording = true,
            statusText = "Recording in progress",
            recordingSessionId = "test_session_123"
        )
        val stoppedState = recordingState.copy(
            isRecording = false,
            statusText = "Recording stopped",
            recordingSessionId = null
        )
        assertThat(stoppedState.isRecording).isFalse()
        assertThat(stoppedState.statusText).contains("stopped")
        assertThat(stoppedState.recordingSessionId).isNull()
    }
    @Test
    fun `should handle recording failure state correctly`() = runTest {
        val initialState = MainUiState()
        val failureState = initialState.copy(
            isRecording = false,
            statusText = "Recording failed to start",
            errorMessage = "Device connection failed",
            showErrorDialog = true
        )
        assertThat(failureState.isRecording).isFalse()
        assertThat(failureState.statusText).contains("failed")
        assertThat(failureState.showErrorDialog).isTrue()
        assertThat(failureState.errorMessage).isEqualTo("Device connection failed")
    }
    @Test
    fun `should maintain proper state transitions`() = runTest {
        val idle = MainUiState()
        assertThat(idle.isRecording).isFalse()
        val recording = idle.copy(
            isRecording = true,
            statusText = "Recording started",
            recordingSessionId = "session_001"
        )
        assertThat(recording.isRecording).isTrue()
        assertThat(recording.recordingSessionId).isNotNull()
        val stopped = recording.copy(
            isRecording = false,
            statusText = "Recording completed",
            recordingSessionId = null
        )
        assertThat(stopped.isRecording).isFalse()
        assertThat(stopped.recordingSessionId).isNull()
        assertThat(stopped.showErrorDialog).isFalse()
    }
    @Test
    fun `should handle error state recovery`() = runTest {
        val errorState = MainUiState(
            isRecording = false,
            showErrorDialog = true,
            errorMessage = "Connection lost",
            statusText = "Error occurred"
        )
        val recoveredState = errorState.copy(
            showErrorDialog = false,
            errorMessage = null,
            statusText = "Ready to record"
        )
        assertThat(recoveredState.showErrorDialog).isFalse()
        assertThat(recoveredState.errorMessage).isNull()
        assertThat(recoveredState.statusText).isEqualTo("Ready to record")
    }
    @Test
    fun `should update recording duration correctly`() = runTest {
        val recordingState = MainUiState(isRecording = true)
        val updatedState = recordingState.copy(recordingDuration = 30000L)
        assertThat(updatedState.recordingDuration).isEqualTo(30000L)
        assertThat(updatedState.isRecording).isTrue()
    }
    @Test
    fun `should handle device status updates`() = runTest {
        val initialState = MainUiState()
        val updatedState = initialState.copy(
            connectedDevices = mapOf(
                "Camera" to DeviceStatus.CONNECTED,
                "Thermal" to DeviceStatus.CONNECTED,
                "GSR" to DeviceStatus.DISCONNECTED,
                "PC" to DeviceStatus.CONNECTED
            )
        )
        assertThat(updatedState.connectedDevices["Camera"]).isEqualTo(DeviceStatus.CONNECTED)
        assertThat(updatedState.connectedDevices["GSR"]).isEqualTo(DeviceStatus.DISCONNECTED)
        assertThat(updatedState.connectedDevices).hasSize(4)
    }
    @Test
    fun `should maintain consistent UI state`() = runTest {
        val state = MainUiState(
            isRecording = true,
            recordingSessionId = "test_session",
            statusText = "Recording",
            recordingDuration = 15000L,
            showErrorDialog = false
        )
        if (state.isRecording) {
            assertThat(state.recordingSessionId).isNotNull()
            assertThat(state.statusText).contains("Recording")
            assertThat(state.showErrorDialog).isFalse()
        }
        assertThat(state.recordingDuration).isAtLeast(0L)
    }
    @Test
    fun `should handle connection state changes`() = runTest {
        val initialState = MainUiState()
        val connectedState = initialState.copy(
            isCameraConnected = true,
            isThermalConnected = true,
            isShimmerConnected = false,
            isPcConnected = true
        )
        assertThat(connectedState.isCameraConnected).isTrue()
        assertThat(connectedState.isThermalConnected).isTrue()
        assertThat(connectedState.isShimmerConnected).isFalse()
        assertThat(connectedState.isPcConnected).isTrue()
    }
    @Test
    fun `should validate canStartRecording property`() = runTest {
        val readyState = MainUiState(
            isInitialized = true,
            isRecording = false,
            isLoadingRecording = false,
            isCameraConnected = true
        )
        assertThat(readyState.canStartRecording).isTrue()
        val recordingState = readyState.copy(isRecording = true)
        assertThat(recordingState.canStartRecording).isFalse()
    }
    @Test
    fun `should validate canStopRecording property`() = runTest {
        val recordingState = MainUiState(
            isRecording = true,
            isLoadingRecording = false
        )
        assertThat(recordingState.canStopRecording).isTrue()
        val loadingState = recordingState.copy(isLoadingRecording = true)
        assertThat(loadingState.canStopRecording).isFalse()
    }
}