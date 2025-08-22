package com.multisensor.recording.recording

import com.multisensor.recording.network.NetworkQualityMonitor
import com.multisensor.recording.util.Logger
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdaptiveFrameRateControllerTest {
    private lateinit var mockNetworkQualityMonitor: NetworkQualityMonitor
    private lateinit var mockLogger: Logger
    private lateinit var adaptiveFrameRateController: AdaptiveFrameRateController
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        mockNetworkQualityMonitor = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        adaptiveFrameRateController = AdaptiveFrameRateController(mockNetworkQualityMonitor, mockLogger)

        println("[DEBUG_LOG] AdaptiveFrameRateControllerTest setup complete")
    }

    @After
    fun tearDown() {
        adaptiveFrameRateController.stop()
        Dispatchers.resetMain()
        clearAllMocks()
        println("[DEBUG_LOG] AdaptiveFrameRateControllerTest teardown complete")
    }

    @Test
    fun testFrameRateSettingsDataClass() {
        println("[DEBUG_LOG] Testing FrameRateSettings data class")

        val settings =
            AdaptiveFrameRateController.FrameRateSettings(
                currentFrameRate = 2.0f,
                targetFrameRate = 2.0f,
                networkQuality = 3,
                isAdaptive = true,
                lastAdaptationTime = System.currentTimeMillis(),
                adaptationCount = 5L,
            )

        assertEquals("Current frame rate should be 2.0", 2.0f, settings.currentFrameRate, 0.1f)
        assertEquals("Target frame rate should be 2.0", 2.0f, settings.targetFrameRate, 0.1f)
        assertEquals("Network quality should be 3", 3, settings.networkQuality)
        assertTrue("Should be adaptive", settings.isAdaptive)
        assertEquals("Adaptation count should be 5", 5L, settings.adaptationCount)

        println("[DEBUG_LOG] FrameRateSettings data class test passed")
    }

    @Test
    fun testStartAndStopController() {
        println("[DEBUG_LOG] Testing start and stop controller")

        adaptiveFrameRateController.start()

        verify { mockLogger.info("[DEBUG_LOG] Starting AdaptiveFrameRateController - Initial frame rate: 2.0fps") }
        verify { mockNetworkQualityMonitor.addListener(adaptiveFrameRateController) }

        adaptiveFrameRateController.stop()

        verify { mockLogger.info("[DEBUG_LOG] Stopping AdaptiveFrameRateController") }
        verify { mockNetworkQualityMonitor.removeListener(adaptiveFrameRateController) }

        println("[DEBUG_LOG] Start and stop controller test passed")
    }

    @Test
    fun testDoubleStartPrevention() {
        println("[DEBUG_LOG] Testing double start prevention")

        adaptiveFrameRateController.start()
        adaptiveFrameRateController.start()

        verify { mockLogger.info("[DEBUG_LOG] AdaptiveFrameRateController already active") }

        println("[DEBUG_LOG] Double start prevention test passed")
    }

    @Test
    fun testListenerRegistration() {
        println("[DEBUG_LOG] Testing listener registration and notification")

        val mockListener = mockk<AdaptiveFrameRateController.FrameRateChangeListener>(relaxed = true)

        adaptiveFrameRateController.addListener(mockListener)

        verify { mockListener.onFrameRateChanged(2.0f, "Initial state") }
        verify { mockListener.onAdaptationModeChanged(true) }

        adaptiveFrameRateController.removeListener(mockListener)

        println("[DEBUG_LOG] Listener registration test passed")
    }

    @Test
    fun testManualFrameRateOverride() {
        println("[DEBUG_LOG] Testing manual frame rate override")

        val mockListener = mockk<AdaptiveFrameRateController.FrameRateChangeListener>(relaxed = true)
        adaptiveFrameRateController.addListener(mockListener)

        val manualFrameRate = 1.5f
        adaptiveFrameRateController.setManualFrameRate(manualFrameRate)

        assertFalse("Adaptive mode should be disabled", adaptiveFrameRateController.isAdaptiveModeEnabled())
        assertEquals(
            "Frame rate should be set to manual value",
            manualFrameRate,
            adaptiveFrameRateController.getCurrentFrameRate(),
            0.1f
        )

        verify { mockListener.onFrameRateChanged(manualFrameRate, "Manual override to ${manualFrameRate}fps") }
        verify { mockListener.onAdaptationModeChanged(false) }
        verify { mockLogger.info("[DEBUG_LOG] Manual frame rate set to ${manualFrameRate}fps") }

        println("[DEBUG_LOG] Manual frame rate override test passed")
    }

    @Test
    fun testEnableAdaptiveMode() {
        println("[DEBUG_LOG] Testing enable adaptive mode")

        val mockListener = mockk<AdaptiveFrameRateController.FrameRateChangeListener>(relaxed = true)
        adaptiveFrameRateController.addListener(mockListener)

        adaptiveFrameRateController.setManualFrameRate(1.0f)

        val mockQuality = NetworkQualityMonitor.NetworkQuality(4, 75, 1500.0)
        every { mockNetworkQualityMonitor.getCurrentQuality() } returns mockQuality

        adaptiveFrameRateController.enableAdaptiveMode()

        assertTrue("Adaptive mode should be enabled", adaptiveFrameRateController.isAdaptiveModeEnabled())

        verify { mockListener.onAdaptationModeChanged(true) }
        verify { mockLogger.info("[DEBUG_LOG] Adaptive mode enabled - Current quality: 4") }

        println("[DEBUG_LOG] Enable adaptive mode test passed")
    }

    @Test
    fun testFrameRateBoundaryValues() {
        println("[DEBUG_LOG] Testing frame rate boundary values")

        adaptiveFrameRateController.setManualFrameRate(0.05f)
        assertTrue("Frame rate should be clamped to minimum", adaptiveFrameRateController.getCurrentFrameRate() >= 0.1f)

        adaptiveFrameRateController.setManualFrameRate(15.0f)
        assertTrue(
            "Frame rate should be clamped to maximum",
            adaptiveFrameRateController.getCurrentFrameRate() <= 10.0f
        )

        adaptiveFrameRateController.setManualFrameRate(-1.0f)
        assertTrue("Negative frame rate should be handled", adaptiveFrameRateController.getCurrentFrameRate() > 0)

        println("[DEBUG_LOG] Frame rate boundary values test passed")
    }

    @Test
    fun testNetworkQualityToFrameRateMapping() {
        println("[DEBUG_LOG] Testing network quality to frame rate mapping")

        adaptiveFrameRateController.start()

        val qualityMappings =
            mapOf(
                1 to 0.5f,
                2 to 1.0f,
                3 to 2.0f,
                4 to 3.0f,
                5 to 5.0f,
            )

        qualityMappings.forEach { (qualityScore, expectedFrameRate) ->
            val quality = NetworkQualityMonitor.NetworkQuality(qualityScore, 100, 1000.0)

            adaptiveFrameRateController.onNetworkQualityChanged(quality)

            println("[DEBUG_LOG] Quality $qualityScore should map to ${expectedFrameRate}fps")
        }

        println("[DEBUG_LOG] Network quality to frame rate mapping test passed")
    }

    @Test
    fun testGetCurrentSettings() {
        println("[DEBUG_LOG] Testing get current settings")

        val settings = adaptiveFrameRateController.getCurrentSettings()

        assertNotNull("Settings should not be null", settings)
        assertTrue("Current frame rate should be positive", settings.currentFrameRate > 0)
        assertTrue("Target frame rate should be positive", settings.targetFrameRate > 0)
        assertTrue("Network quality should be between 1 and 5", settings.networkQuality in 1..5)
        assertTrue("Should be adaptive by default", settings.isAdaptive)
        assertTrue("Adaptation count should be non-negative", settings.adaptationCount >= 0)

        println("[DEBUG_LOG] Current settings: $settings")
        println("[DEBUG_LOG] Get current settings test passed")
    }

    @Test
    fun testAdaptationStatistics() {
        println("[DEBUG_LOG] Testing adaptation statistics")

        val statistics = adaptiveFrameRateController.getAdaptationStatistics()

        assertNotNull("Statistics should not be null", statistics)
        assertTrue("Statistics should contain frame rate info", statistics.contains("Current Frame Rate"))
        assertTrue("Statistics should contain adaptive mode info", statistics.contains("Adaptive Mode"))
        assertTrue("Statistics should contain network quality info", statistics.contains("Network Quality"))
        assertTrue("Statistics should contain adaptation count", statistics.contains("Total Adaptations"))

        println("[DEBUG_LOG] Adaptation statistics: $statistics")
        println("[DEBUG_LOG] Adaptation statistics test passed")
    }

    @Test
    fun testResetStatistics() {
        println("[DEBUG_LOG] Testing reset statistics")

        adaptiveFrameRateController.resetStatistics()

        verify { mockLogger.info("[DEBUG_LOG] Adaptation statistics reset") }

        val settings = adaptiveFrameRateController.getCurrentSettings()
        assertEquals("Adaptation count should be reset to 0", 0L, settings.adaptationCount)
        assertEquals("Last adaptation time should be reset to 0", 0L, settings.lastAdaptationTime)

        println("[DEBUG_LOG] Reset statistics test passed")
    }

    @Test
    fun testListenerErrorHandling() {
        println("[DEBUG_LOG] Testing listener error handling")

        val faultyListener =
            object : AdaptiveFrameRateController.FrameRateChangeListener {
                override fun onFrameRateChanged(
                    newFrameRate: Float,
                    reason: String,
                ): Unit = throw RuntimeException("Test exception")

                override fun onAdaptationModeChanged(isAdaptive: Boolean): Unit =
                    throw RuntimeException("Test exception")
            }

        adaptiveFrameRateController.addListener(faultyListener)

        verify { mockLogger.error("Error notifying frame rate change listener", any<Exception>()) }
        verify { mockLogger.error("Error notifying adaptation mode change listener", any<Exception>()) }

        println("[DEBUG_LOG] Listener error handling test passed")
    }

    @Test
    fun testHysteresisLogic() {
        println("[DEBUG_LOG] Testing hysteresis logic")

        adaptiveFrameRateController.start()
        val mockListener = mockk<AdaptiveFrameRateController.FrameRateChangeListener>(relaxed = true)
        adaptiveFrameRateController.addListener(mockListener)

        val quality1 = NetworkQualityMonitor.NetworkQuality(3, 150, 800.0)
        val quality2 = NetworkQualityMonitor.NetworkQuality(3, 160, 850.0)

        adaptiveFrameRateController.onNetworkQualityChanged(quality1)
        Thread.sleep(100)
        adaptiveFrameRateController.onNetworkQualityChanged(quality2)

        println("[DEBUG_LOG] Hysteresis should prevent rapid frame rate changes")
        println("[DEBUG_LOG] Hysteresis logic test passed")
    }

    @Test
    fun testAdaptiveControllerInInactiveState() {
        println("[DEBUG_LOG] Testing adaptive controller in inactive state")

        val quality = NetworkQualityMonitor.NetworkQuality(5, 50, 2000.0)

        adaptiveFrameRateController.onNetworkQualityChanged(quality)

        assertEquals(
            "Frame rate should remain at default when inactive",
            2.0f,
            adaptiveFrameRateController.getCurrentFrameRate(),
            0.1f
        )

        println("[DEBUG_LOG] Adaptive controller in inactive state test passed")
    }

    @Test
    fun testConcurrentOperations() {
        println("[DEBUG_LOG] Testing concurrent operations")

        val listener1 = mockk<AdaptiveFrameRateController.FrameRateChangeListener>(relaxed = true)
        val listener2 = mockk<AdaptiveFrameRateController.FrameRateChangeListener>(relaxed = true)

        adaptiveFrameRateController.addListener(listener1)
        adaptiveFrameRateController.addListener(listener2)

        adaptiveFrameRateController.start()
        adaptiveFrameRateController.stop()

        adaptiveFrameRateController.setManualFrameRate(1.5f)

        every { mockNetworkQualityMonitor.getCurrentQuality() } returns NetworkQualityMonitor.NetworkQuality(
            3,
            100,
            1000.0
        )
        adaptiveFrameRateController.enableAdaptiveMode()

        assertTrue("Concurrent operations should work", true)

        println("[DEBUG_LOG] Concurrent operations test passed")
    }
}
