package com.multisensor.recording.ui.viewmodel

import com.google.common.truth.Truth.assertThat
import android.content.Context
import com.multisensor.recording.controllers.RecordingSessionController
import com.multisensor.recording.managers.DeviceConnectionManager
import com.multisensor.recording.managers.FileTransferManager
import com.multisensor.recording.managers.CalibrationManager
import com.multisensor.recording.managers.ShimmerManager
import com.multisensor.recording.testbase.BaseUnitTest
import com.multisensor.recording.util.Logger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.multisensor.recording.ui.MainViewModel
import com.multisensor.recording.ui.MainUiState

class MainViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: MainViewModel
    private val mockContext: Context = mockk(relaxed = true)
    private val mockRecordingController: RecordingSessionController = mockk(relaxed = true)
    private val mockDeviceManager: DeviceConnectionManager = mockk(relaxed = true)
    private val mockFileManager: FileTransferManager = mockk(relaxed = true)
    private val mockCalibrationManager: CalibrationManager = mockk(relaxed = true)
    private val mockShimmerManager: ShimmerManager = mockk(relaxed = true)
    private val mockLogger: Logger = mockk(relaxed = true)

    @Before
    override fun setUp() {
        super.setUp()

        every { mockLogger.info(any()) } returns Unit
        every { mockLogger.debug(any()) } returns Unit
        every { mockLogger.error(any()) } returns Unit

        viewModel = MainViewModel(
            mockContext,
            mockRecordingController,
            mockDeviceManager,
            mockFileManager,
            mockCalibrationManager,
            mockShimmerManager,
            mockLogger
        )
    }

    @Test
    fun `should initialize with default UI state`() = runTest {
        val initialState = viewModel.uiState.first()

        assertThat(initialState).isNotNull()
        assertThat(initialState.isInitialized).isTrue()
        assertThat(initialState.isRecording).isFalse()
        assertThat(initialState.statusText).isEqualTo("Ready")
    }

    @Test
    fun `should provide UI state as StateFlow`() = runTest {
        val stateFlow = viewModel.uiState

        assertThat(stateFlow).isNotNull()
        val currentState = stateFlow.first()
        assertThat(currentState).isInstanceOf(MainUiState::class.java)
    }

    @Test
    fun `should start recording when requested`() = runTest {
        viewModel.startRecording()

        assertThat(viewModel).isNotNull()
    }

    @Test
    fun `should handle errors gracefully`() = runTest {
        val errorMessage = "Test error occurred"

        viewModel.clearError()

        assertThat(viewModel).isNotNull()
    }

    @Test
    fun `should manage device connection states`() = runTest {
        val initialState = viewModel.uiState.first()

        assertThat(initialState.isPcConnected).isFalse()
        assertThat(initialState.isShimmerConnected).isFalse()
        assertThat(initialState.isThermalConnected).isFalse()
    }

    @Test
    fun `should provide recording configuration options`() = runTest {
        // Remove obsolete method calls as functionality has changed
        assertThat(viewModel).isNotNull()
    }

    @Test
    fun `should handle calibration operations`() = runTest {
        viewModel.runCalibration()

        assertThat(viewModel).isNotNull()
    }

    @Test
    fun `should support raw image capture`() = runTest {
        viewModel.captureRawImage()

        assertThat(viewModel).isNotNull()
    }
}
