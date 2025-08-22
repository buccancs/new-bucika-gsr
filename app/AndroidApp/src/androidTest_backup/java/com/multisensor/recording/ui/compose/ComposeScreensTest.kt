package com.multisensor.recording.ui.compose

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multisensor.recording.ui.compose.screens.DevicesScreen
import com.multisensor.recording.ui.compose.screens.CalibrationScreen
import com.multisensor.recording.ui.compose.screens.FilesScreen
import com.multisensor.recording.ui.theme.MultiSensorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun devicesScreen_displaysCorrectContent() {
        composeTestRule.setContent {
            MultiSensorTheme {
                DevicesScreen()
            }
        }

        composeTestRule.onNodeWithText("Device Management").assertExists()
        composeTestRule.onNodeWithText("Scan for Devices").assertExists()
    }

    @Test
    fun calibrationScreen_displaysCorrectContent() {
        composeTestRule.setContent {
            MultiSensorTheme {
                CalibrationScreen()
            }
        }

        composeTestRule.onNodeWithText("Calibration").assertExists()
        composeTestRule.onNodeWithText("Start Calibration").assertExists()
    }

    @Test
    fun filesScreen_displaysCorrectContent() {
        composeTestRule.setContent {
            MultiSensorTheme {
                FilesScreen()
            }
        }

        composeTestRule.onNodeWithText("File Management").assertExists()
        composeTestRule.onNodeWithText("Browse Files").assertExists()
        composeTestRule.onNodeWithText("Export Data").assertExists()
    }
}
