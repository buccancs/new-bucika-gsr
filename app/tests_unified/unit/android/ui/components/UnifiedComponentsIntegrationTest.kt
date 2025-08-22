package com.multisensor.recording.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the unified components created to eliminate duplication
 * Tests the CommonIndicators components that replaced multiple duplicate implementations
 */
@RunWith(AndroidJUnit4::class)
class UnifiedComponentsIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unifiedComponents_workTogetherInScreen() {
        // When - Use all unified components together
        composeTestRule.setContent {
            Column {
                // Test RecordingIndicator
                RecordingIndicator()
                
                // Test DeviceStatusOverlay with different states
                DeviceStatusOverlay(
                    deviceName = "RGB Camera",
                    icon = Icons.Default.Camera,
                    isConnected = true,
                    isInitializing = false,
                    detailText = "1080p"
                )
                
                DeviceStatusOverlay(
                    deviceName = "Thermal Camera", 
                    icon = Icons.Default.Camera,
                    isConnected = false,
                    isInitializing = true
                )
                
                // Test PreviewCard
                PreviewCard(height = 100.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "Preview Content" }
                    )
                }
            }
        }

        // Then - All components should be displayed correctly
        composeTestRule
            .onNodeWithText("REC")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("RGB Camera Connected")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("1080p")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Initializing...")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithContentDescription("Preview Content")
            .assertIsDisplayed()
    }

    @Test
    fun recordingIndicator_hasConsistentStyling() {
        // Given
        composeTestRule.setContent {
            RecordingIndicator()
        }

        // Then - Should have proper styling elements
        composeTestRule
            .onNodeWithText("REC")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithContentDescription("Recording")
            .assertIsDisplayed()
    }

    @Test
    fun deviceStatusOverlay_showsAllConnectionStates() {
        // Test all three states: connected, disconnected, initializing
        var currentState by mutableStateOf(DeviceState.CONNECTED)
        
        composeTestRule.setContent {
            DeviceStatusOverlay(
                deviceName = "Test Device",
                icon = Icons.Default.Camera,
                isConnected = currentState == DeviceState.CONNECTED,
                isInitializing = currentState == DeviceState.INITIALIZING
            )
        }

        // Connected state
        composeTestRule
            .onNodeWithText("Test Device Connected")
            .assertIsDisplayed()

        // Change to initializing
        currentState = DeviceState.INITIALIZING
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithText("Initializing...")
            .assertIsDisplayed()

        // Change to disconnected
        currentState = DeviceState.DISCONNECTED
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithText("Test Device Disconnected")
            .assertIsDisplayed()
    }

    @Test
    fun previewCard_providesConsistentContainer() {
        val testHeight = 150.dp
        
        composeTestRule.setContent {
            PreviewCard(height = testHeight) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red)
                        .semantics { contentDescription = "Red Box Content" }
                )
            }
        }

        // Should provide proper container
        composeTestRule
            .onNodeWithContentDescription("Red Box Content")
            .assertIsDisplayed()
    }

    private enum class DeviceState {
        CONNECTED, DISCONNECTED, INITIALIZING
    }
}