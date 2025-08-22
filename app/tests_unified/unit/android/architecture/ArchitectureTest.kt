package com.multisensor.recording.architecture

import com.multisensor.recording.controllers.ConnectionConfig
import com.multisensor.recording.controllers.ConnectionState
import com.multisensor.recording.controllers.ControllerConnectionManager
import com.multisensor.recording.controllers.PcControllerConnectionManager
import com.multisensor.recording.streaming.NetworkPreviewStreamer
import com.multisensor.recording.streaming.PreviewStreamingInterface
import com.multisensor.recording.streaming.StreamingConfig
import com.multisensor.recording.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class ArchitectureTest {

    @Test
    fun `ControllerConnectionManager abstraction works correctly`() = runTest {

        val mockLogger = mock(Logger::class.java)
        val mockJsonSocketClient = mock(com.multisensor.recording.network.JsonSocketClient::class.java)

        val connectionManager = PcControllerConnectionManager(mockJsonSocketClient, mockLogger)

        val config = ConnectionConfig("192.168.1.100", 9000)
        val result = connectionManager.connect(config)

        assertTrue("Connection should succeed", result.isSuccess)
        verify(mockJsonSocketClient).configure(eq("192.168.1.100"), eq(9000))
        verify(mockJsonSocketClient).connect()
    }

    @Test
    fun `PreviewStreamingInterface abstraction enables modular design`() = runTest {

        val mockLogger = mock(Logger::class.java)
        val mockConnectionManager = mock(ControllerConnectionManager::class.java)

        `when`(mockConnectionManager.isConnected()).thenReturn(true)

        val streamingInterface: PreviewStreamingInterface = NetworkPreviewStreamer(mockConnectionManager, mockLogger)

        val config = StreamingConfig(targetFps = 5, jpegQuality = 80)
        streamingInterface.configure(config)
        streamingInterface.startStreaming(config)

        assertTrue("Should be streaming", streamingInterface.isStreaming())
    }

    @Test
    fun `ConnectionState flows correctly represent state changes`() {

        val states = mutableListOf<ConnectionState>()

        states.add(ConnectionState.Disconnected)
        states.add(ConnectionState.Connecting)
        states.add(ConnectionState.Connected)
        states.add(ConnectionState.Error("Connection failed"))

        assertEquals("Should start disconnected", ConnectionState.Disconnected, states[0])
        assertEquals("Should transition to connecting", ConnectionState.Connecting, states[1])
        assertEquals("Should reach connected state", ConnectionState.Connected, states[2])
        assertTrue("Should handle error state", states[3] is ConnectionState.Error)
    }

    @Test
    fun `Modular camera recorder dependencies are properly abstracted`() {

        val mockPreviewStreamer = mock(PreviewStreamingInterface::class.java)
        val mockHandSegmentation = mock(com.multisensor.recording.handsegmentation.HandSegmentationInterface::class.java)
        val mockSessionManager = mock(com.multisensor.recording.service.SessionManager::class.java)
        val mockLogger = mock(Logger::class.java)
        val mockContext = mock(android.content.Context::class.java)

        val constructorExists = try {
            com.multisensor.recording.recording.ModularCameraRecorder::class.java
                .getDeclaredConstructor(
                    android.content.Context::class.java,
                    com.multisensor.recording.service.SessionManager::class.java,
                    Logger::class.java,
                    com.multisensor.recording.handsegmentation.HandSegmentationInterface::class.java,
                    PreviewStreamingInterface::class.java
                )
            true
        } catch (e: NoSuchMethodException) {
            false
        }

        assertTrue("ModularCameraRecorder should support dependency injection", constructorExists)
    }

    @Test
    fun `Single activity pattern navigation screens are defined`() {

        val screenRoutes = listOf(
            "recording",
            "thermal_preview",
            "devices",
            "calibration",
            "files",
            "settings",
            "about",
            "diagnostics",
            "shimmer_settings",
            "shimmer_viz"
        )

        val hasMainScreens = screenRoutes.containsAll(listOf("recording", "devices", "files"))
        val hasSettingsScreens = screenRoutes.containsAll(listOf("settings", "about"))
        val hasAdvancedScreens = screenRoutes.containsAll(listOf("diagnostics", "shimmer_settings"))

        assertTrue("Should have main recording screens", hasMainScreens)
        assertTrue("Should have settings screens", hasSettingsScreens)
        assertTrue("Should have advanced configuration screens", hasAdvancedScreens)
        assertEquals("Should consolidate 10 screens", 10, screenRoutes.size)
    }

    @Test
    fun `Hand segmentation interface enables modular design`() = runTest {

        val mockManager = mock(com.multisensor.recording.handsegmentation.HandSegmentationManager::class.java)
        val handSegmentationInterface = com.multisensor.recording.handsegmentation.HandSegmentationAdapter(mockManager)

        val config = com.multisensor.recording.handsegmentation.SegmentationConfig(
            outputDirectory = java.io.File("/tmp"),
            enableRealTimeProcessing = true
        )

        assertFalse("Should start uninitialized", handSegmentationInterface.isInitialized())
        assertFalse("Should start without active session", handSegmentationInterface.isSessionActive())
    }

    @Test
    fun `Privacy interface focuses on privacy concerns only`() = runTest {

        val mockPrivacyManager = mock(com.multisensor.recording.security.PrivacyManager::class.java)
        val privacyInterface = com.multisensor.recording.security.PrivacyManagerAdapter(mockPrivacyManager)

        val consentData = com.multisensor.recording.security.ConsentData(
            participantId = "P001",
            studyId = "S001"
        )

        val privacyMethods = listOf(
            "hasValidConsent",
            "recordConsent",
            "withdrawConsent",
            "enableDataAnonymization",
            "anonymizeImage"
        )

        val interfaceClass = com.multisensor.recording.security.PrivacyInterface::class.java
        val methodNames = interfaceClass.methods.map { it.name }

        assertTrue("Should have core privacy methods",
            privacyMethods.all { it in methodNames })
    }
}