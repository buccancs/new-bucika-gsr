package com.multisensor.recording.ui.compose.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multisensor.recording.ui.MainUiState
import com.multisensor.recording.ui.MainViewModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: MainViewModel
    private lateinit var uiStateFlow: MutableStateFlow<MainUiState>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            MainUiState(
                isRecording = false,
                isInitialized = false,
                isCameraConnected = false,
                isThermalConnected = false,
                isGsrConnected = false,
                isPcConnected = false
            )
        )
        
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun recordingScreen_displaysSessionStatusCard() {
        // When
        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Initializing")
            .assertIsDisplayed()
    }

    @Test
    fun recordingScreen_displaysCameraPreviewSwitch() {
        // When
        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Camera Preview")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("RGB")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Thermal")
            .assertIsDisplayed()
    }

    @Test
    fun recordingScreen_cameraSwitchChangesPreview() {
        // Given
        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // When - Initially RGB should be selected (switch unchecked)
        composeTestRule
            .onNode(hasText("RGB"))
            .assertIsDisplayed()

        // When - Click the switch to show thermal camera
        composeTestRule
            .onNode(hasRole(androidx.compose.ui.semantics.Role.Switch))
            .performClick()

        // Then - Switch should be checked and thermal should be active
        composeTestRule
            .onNode(hasRole(androidx.compose.ui.semantics.Role.Switch))
            .assertIsOn()
    }

    @Test
    fun recordingScreen_showsRecordingButtonWhenReady() {
        // Given - System is initialized
        uiStateFlow.value = uiStateFlow.value.copy(
            isInitialized = true,
            canStartRecording = true
        )

        // When
        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .assertIsDisplayed()
    }

    @Test
    fun recordingScreen_recordingButtonStartsRecording() {
        // Given - System is ready
        uiStateFlow.value = uiStateFlow.value.copy(
            isInitialized = true,
            canStartRecording = true
        )

        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // When - Click recording button
        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .performClick()

        // Then
        verify { mockViewModel.startRecording() }
    }

    @Test
    fun recordingScreen_recordingButtonStopsRecordingWhenActive() {
        // Given - System is recording
        uiStateFlow.value = uiStateFlow.value.copy(
            isRecording = true,
            canStopRecording = true
        )

        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // When - Click recording button
        composeTestRule
            .onNodeWithContentDescription("Stop Recording")
            .performClick()

        // Then
        verify { mockViewModel.stopRecording() }
    }

    @Test
    fun recordingScreen_showsCorrectStatusWhenRecording() {
        // Given - System is recording
        uiStateFlow.value = uiStateFlow.value.copy(
            isRecording = true,
            isInitialized = true
        )

        // When
        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Recording")
            .assertIsDisplayed()
    }

    @Test
    fun recordingScreen_showsReadyWhenInitializedButNotRecording() {
        // Given - System is initialized but not recording
        uiStateFlow.value = uiStateFlow.value.copy(
            isRecording = false,
            isInitialized = true
        )

        // When
        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Ready")
            .assertIsDisplayed()
    }

    @Test
    fun recordingScreen_attemptsSystemInitializationWhenCameraReady() {
        // Given
        composeTestRule.setContent {
            RecordingScreen(viewModel = mockViewModel)
        }

        // Wait for initialization attempts - this happens in LaunchedEffect
        composeTestRule.waitForIdle()

        // Then - Should try to connect to PC automatically
        verify(timeout = 5000) { mockViewModel.connectToPC() }
    }
}