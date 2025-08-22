package com.multisensor.recording.controllers

import android.content.Context
import android.content.Intent
import android.view.TextureView
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.multisensor.recording.ui.MainViewModel
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityCoordinatorTest {

    private val mockPermissionController = mockk<PermissionController>(relaxed = true)
    private val mockUsbController = mockk<UsbController>(relaxed = true)
    private val mockShimmerController = mockk<ShimmerController>(relaxed = true)
    private val mockRecordingController = mockk<RecordingController>(relaxed = true)
    private val mockCalibrationController = mockk<CalibrationController>(relaxed = true)
    private val mockNetworkController = mockk<NetworkController>(relaxed = true)
    private val mockStatusDisplayController = mockk<StatusDisplayController>(relaxed = true)
    private val mockUIController = mockk<UIController>(relaxed = true)

    private val mockCallback = mockk<MainActivityCoordinator.CoordinatorCallback>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockView = mockk<View>(relaxed = true)
    private val mockTextView = mockk<TextView>(relaxed = true)

    private lateinit var coordinator: MainActivityCoordinator

    @Before
    fun setUp() {
        every { mockCallback.getContext() } returns mockContext
        every { mockCallback.getBatteryLevelText() } returns mockTextView
        every { mockCallback.getPcConnectionStatus() } returns mockTextView
        every { mockCallback.getPcConnectionIndicator() } returns mockView
        every { mockCallback.getThermalConnectionStatus() } returns mockTextView
        every { mockCallback.getThermalConnectionIndicator() } returns mockView

        coordinator = MainActivityCoordinator(
            mockPermissionController,
            mockUsbController,
            mockShimmerController,
            mockRecordingController,
            mockCalibrationController,
            mockNetworkController,
            mockStatusDisplayController,
            mockUIController
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test coordinator initialization`() {
        coordinator.initialize(mockCallback)

        verify { mockPermissionController.setCallback(any()) }
        verify { mockUsbController.setCallback(any()) }
        verify { mockShimmerController.setCallback(any()) }
        verify { mockRecordingController.setCallback(any()) }
        verify { mockCalibrationController.setCallback(any()) }
        verify { mockNetworkController.setCallback(any()) }
        verify { mockStatusDisplayController.setCallback(any()) }
        verify { mockUIController.setCallback(any()) }
    }

    @Test
    fun `test coordinated operations`() {
        coordinator.initialize(mockCallback)
        val mockContext = mockk<Context>()
        val mockIntent = mockk<Intent>()
        val mockTextureView = mockk<TextureView>()
        val mockViewModel = mockk<MainViewModel>()
        val mockLifecycleScope = mockk<LifecycleCoroutineScope>()

        coordinator.checkPermissions(mockContext)

        verify { mockPermissionController.checkPermissions(mockContext) }

        coordinator.handleUsbDeviceIntent(mockContext, mockIntent)

        verify { mockUsbController.handleUsbDeviceIntent(mockContext, mockIntent) }

        coordinator.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel)

        verify { mockRecordingController.initializeRecordingSystem(mockContext, mockTextureView, mockViewModel) }

        coordinator.startRecording(mockContext, mockViewModel)

        verify { mockRecordingController.startRecording(mockContext, mockViewModel) }
        verify { mockNetworkController.updateStreamingUI(mockContext, true) }

        coordinator.stopRecording(mockContext, mockViewModel)

        verify { mockRecordingController.stopRecording(mockContext, mockViewModel) }
        verify { mockNetworkController.updateStreamingUI(mockContext, false) }

        coordinator.runCalibration(mockLifecycleScope)

        verify { mockCalibrationController.runCalibration(mockLifecycleScope) }
    }

    @Test
    fun `test system status summary`() {
        coordinator.initialize(mockCallback)
        val mockContext = mockk<Context>()

        every { mockPermissionController.getPermissionRetryCount() } returns 2
        every { mockUsbController.getUsbStatusSummary(mockContext) } returns "USB Status: Connected"
        every { mockShimmerController.getConnectionStatus() } returns "Shimmer Status: Connected"
        every { mockRecordingController.getRecordingStatus() } returns "Recording Status: Ready"
        every { mockCalibrationController.getCalibrationStatus() } returns "Calibration Status: Ready"
        every { mockNetworkController.getStreamingStatus() } returns "Network Status: Connected"

        val statusSummary = coordinator.getSystemStatusSummary(mockContext)

        assertNotNull("Status summary should not be null", statusSummary)
        assertTrue(
            "Status summary should contain coordinator info",
            statusSummary.contains("Coordinator Initialized: true")
        )
        assertTrue("Status summary should contain permission info", statusSummary.contains("Permission Retries: 2"))
        assertTrue("Status summary should contain USB status", statusSummary.contains("USB Status: Connected"))
    }

    @Test
    fun `test reset all states`() {
        coordinator.initialize(mockCallback)

        coordinator.resetAllStates()

        verify { mockPermissionController.resetState() }
        verify { mockShimmerController.resetState() }
        verify { mockRecordingController.resetState() }
        verify { mockCalibrationController.resetState() }
        verify { mockNetworkController.resetState() }
    }

    @Test
    fun `test cleanup`() {
        coordinator.initialize(mockCallback)

        coordinator.cleanup()

        verify { mockCalibrationController.cleanup() }
    }

    @Test
    fun `test menu operations`() {
        coordinator.initialize(mockCallback)
        val mockMenu = mockk<android.view.Menu>()
        val mockActivity = mockk<android.app.Activity>()
        val mockMenuItem = mockk<android.view.MenuItem>()

        val menuCreated = coordinator.createOptionsMenu(mockMenu, mockActivity)
        val menuHandled = coordinator.handleOptionsItemSelected(mockMenuItem)

        assertFalse(menuCreated)
        assertFalse(menuHandled)
    }
}
