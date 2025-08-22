package com.multisensor.recording.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommonIndicatorsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recordingIndicator_displaysCorrectContent() {
        // When
        composeTestRule.setContent {
            RecordingIndicator()
        }

        // Then
        composeTestRule
            .onNodeWithText("REC")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Recording")
            .assertIsDisplayed()
    }

    @Test
    fun deviceStatusOverlay_showsConnectedState() {
        // When
        composeTestRule.setContent {
            DeviceStatusOverlay(
                deviceName = "RGB Camera",
                icon = Icons.Default.Camera,
                isConnected = true,
                isInitializing = false
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("RGB Camera Connected")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("RGB Camera")
            .assertIsDisplayed()
    }

    @Test
    fun deviceStatusOverlay_showsDisconnectedState() {
        // When
        composeTestRule.setContent {
            DeviceStatusOverlay(
                deviceName = "Thermal Camera",
                icon = Icons.Default.Thermostat,
                isConnected = false,
                isInitializing = false
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("Thermal Camera Disconnected")
            .assertIsDisplayed()
    }

    @Test
    fun deviceStatusOverlay_showsInitializingState() {
        // When
        composeTestRule.setContent {
            DeviceStatusOverlay(
                deviceName = "RGB Camera",
                icon = Icons.Default.Camera,
                isConnected = false,
                isInitializing = true
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("Initializing...")
            .assertIsDisplayed()
    }

    @Test
    fun deviceStatusOverlay_showsDetailText() {
        val detailText = "720p @ 30fps"
        
        // When
        composeTestRule.setContent {
            DeviceStatusOverlay(
                deviceName = "RGB Camera",
                icon = Icons.Default.Camera,
                isConnected = true,
                isInitializing = false,
                detailText = detailText
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("RGB Camera Connected")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText(detailText)
            .assertIsDisplayed()
    }

    @Test
    fun previewCard_containsContent() {
        val testContentDescription = "Test Preview Content"
        
        // When
        composeTestRule.setContent {
            PreviewCard {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = testContentDescription }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription(testContentDescription)
            .assertIsDisplayed()
    }
}