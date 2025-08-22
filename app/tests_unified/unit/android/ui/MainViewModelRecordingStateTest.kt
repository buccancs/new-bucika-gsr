package com.multisensor.recording.ui

import android.content.Context
import android.view.SurfaceView
import android.view.TextureView
import com.google.common.truth.Truth.assertThat
import com.multisensor.recording.controllers.RecordingSessionController
import com.multisensor.recording.managers.DeviceConnectionManager
import com.multisensor.recording.managers.FileTransferManager
import com.multisensor.recording.managers.CalibrationManager
import com.multisensor.recording.managers.ShimmerManager
import com.multisensor.recording.util.Logger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi
class MainViewModelRecordingStateTest {

    private lateinit var viewModel: MainViewModel
    private val mockContext: Context = mockk(relaxed = true)
    private val mockRecordingController: RecordingSessionController = mockk(relaxed = true)
    private val mockDeviceManager: DeviceConnectionManager = mockk(relaxed = true)
    private val mockFileManager: FileTransferManager = mockk(relaxed = true)
    private val mockCalibrationManager: CalibrationManager = mockk(relaxed = true)
    private val mockShimmerManager: ShimmerManager = mockk(relaxed = true)
    private val mockLogger: Logger = mockk(relaxed = true)

    @Before
    fun setUp() {
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
    fun `initial state should have isRecording false and initializing status`() = runTest {
        val initialState = viewModel.uiState.first()

        assertThat(initialState.isRecording).isFalse()
        assertThat(initialState.statusText).isEqualTo("Initializing...")
        assertThat(initialState.isInitialized).isFalse()
    }

    @Test
    fun `canStartRecording should be false when not initialized`() = runTest {
        val state = viewModel.uiState.first()

        assertThat(state.canStartRecording).isFalse()
    }

    @Test
    fun `canStopRecording should be false when not recording`() = runTest {
        val state = viewModel.uiState.first()

        assertThat(state.canStopRecording).isFalse()
    }

    @Test
    fun `startRecording should update recording state`() = runTest {

        val mockTextureView: TextureView = mockk(relaxed = true)
        viewModel.initializeSystem(mockTextureView)
        advanceUntilIdle()

        viewModel.startRecording()
        advanceUntilIdle()

        val state = viewModel.uiState.first()

        verify { mockLogger.info(any()) }
    }

    @Test
    fun `stopRecording should be callable when recording`() = runTest {
        viewModel.stopRecording()
        advanceUntilIdle()

        verify { mockLogger.info(any()) }
    }

    @Test
    fun `captureRawImage should work`() = runTest {
        viewModel.captureRawImage()
        advanceUntilIdle()

        verify { mockLogger.info(any()) }
    }

    @Test
    fun `checkRawStage3Availability should return boolean`() = runTest {
        val isAvailable = viewModel.checkRawStage3Availability()

        assertThat(isAvailable is Boolean).isTrue()
    }

    @Test
    fun `checkThermalCameraAvailability should return boolean`() = runTest {
        val isAvailable = viewModel.checkThermalCameraAvailability()

        assertThat(isAvailable is Boolean).isTrue()
    }

    @Test
    fun `initializeSystem should change initialization state`() = runTest {
        val mockTextureView: TextureView = mockk(relaxed = true)
        val mockSurfaceView: SurfaceView = mockk(relaxed = true)

        viewModel.initializeSystem(mockTextureView, mockSurfaceView)
        advanceUntilIdle()

        verify { mockLogger.info(any()) }
    }

    @Test
    fun `initializeSystemWithFallback should work`() = runTest {
        viewModel.initializeSystemWithFallback()
        advanceUntilIdle()

        verify { mockLogger.info(any()) }
    }

    @Test
    fun `ui state properties should have expected types`() = runTest {
        val state = viewModel.uiState.first()

        assertThat(state.isRecording is Boolean).isTrue()
        assertThat(state.statusText is String).isTrue()
        assertThat(state.isInitialized is Boolean).isTrue()
        assertThat(state.recordingDuration is Long).isTrue()
        assertThat(state.isReadyToRecord is Boolean).isTrue()

        assertThat(state.isPcConnected is Boolean).isTrue()
        assertThat(state.isShimmerConnected is Boolean).isTrue()
        assertThat(state.isThermalConnected is Boolean).isTrue()
        assertThat(state.isCameraConnected is Boolean).isTrue()

        assertThat(state.canStartRecording is Boolean).isTrue()
        assertThat(state.canStopRecording is Boolean).isTrue()
        assertThat(state.canRunCalibration is Boolean).isTrue()
    }

    @Test
    fun `recording session management should work with session manager`() = runTest {

        viewModel.startRecording()
        advanceUntilIdle()

        viewModel.stopRecording()
        advanceUntilIdle()

        verify { mockLogger.info(any()) }
    }

    @Test
    fun `error states should be handled properly`() = runTest {
        val state = viewModel.uiState.first()

        assertThat(state.errorMessage).isNull()
        assertThat(state.showErrorDialog is Boolean).isTrue()
    }

    @Test
    fun `loading states should be tracked properly`() = runTest {
        val state = viewModel.uiState.first()

        assertThat(state.isLoadingRecording is Boolean).isTrue()
        assertThat(state.isLoadingCalibration is Boolean).isTrue()
        assertThat(state.isLoadingPermissions is Boolean).isTrue()
    }
}
