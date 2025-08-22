package com.multisensor.recording.ui.util

import android.content.Context
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.multisensor.recording.R
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UIUtilsTest {

    private lateinit var context: Context
    private lateinit var mockView: View

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockView = mockk(relaxed = true)

        mockkStatic(ContextCompat::class)
        every { ContextCompat.getColor(any(), any()) } returns 0xFF000000.toInt()
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `updateConnectionIndicator should set connected colour when connected`() {
        UIUtils.updateConnectionIndicator(context, mockView, true)

        verify { ContextCompat.getColor(context, R.color.statusIndicatorConnected) }
        verify { mockView.setBackgroundColor(any()) }
    }

    @Test
    fun `updateConnectionIndicator should set disconnected colour when not connected`() {
        UIUtils.updateConnectionIndicator(context, mockView, false)

        verify { ContextCompat.getColor(context, R.color.statusIndicatorDisconnected) }
        verify { mockView.setBackgroundColor(any()) }
    }

    @Test
    fun `updateRecordingIndicator should set active colour when recording`() {
        UIUtils.updateRecordingIndicator(context, mockView, true)

        verify { ContextCompat.getColor(context, R.color.recordingActive) }
        verify { mockView.setBackgroundColor(any()) }
    }

    @Test
    fun `updateRecordingIndicator should set inactive colour when not recording`() {
        UIUtils.updateRecordingIndicator(context, mockView, false)

        verify { ContextCompat.getColor(context, R.color.recordingInactive) }
        verify { mockView.setBackgroundColor(any()) }
    }

    @Test
    fun `showStatusMessage should create short toast by default`() {
        val message = "Test message"
        mockkStatic(Toast::class)
        val mockToast = mockk<Toast>(relaxed = true)
        every { Toast.makeText(context, message, Toast.LENGTH_SHORT) } returns mockToast

        UIUtils.showStatusMessage(context, message)

        verify { Toast.makeText(context, message, Toast.LENGTH_SHORT) }
        verify { mockToast.show() }
    }

    @Test
    fun `showStatusMessage should create long toast when specified`() {
        val message = "Test long message"
        mockkStatic(Toast::class)
        val mockToast = mockk<Toast>(relaxed = true)
        every { Toast.makeText(context, message, Toast.LENGTH_LONG) } returns mockToast

        UIUtils.showStatusMessage(context, message, true)

        verify { Toast.makeText(context, message, Toast.LENGTH_LONG) }
        verify { mockToast.show() }
    }

    @Test
    fun `getConnectionStatusText should return connected status`() {
        val result = UIUtils.getConnectionStatusText("Device1", true)

        assertEquals("Device1: Connected", result)
    }

    @Test
    fun `getConnectionStatusText should return PC waiting status`() {
        val result = UIUtils.getConnectionStatusText("PC", false)

        assertEquals("PC: Waiting...", result)
    }

    @Test
    fun `getConnectionStatusText should return Shimmer off status`() {
        val result = UIUtils.getConnectionStatusText("Shimmer", false)

        assertEquals("Shimmer: Off", result)
    }

    @Test
    fun `getConnectionStatusText should return thermal off status`() {
        val result = UIUtils.getConnectionStatusText("Thermal", false)

        assertEquals("Thermal: Off", result)
    }

    @Test
    fun `getConnectionStatusText should return generic disconnected status`() {
        val result = UIUtils.getConnectionStatusText("GenericDevice", false)

        assertEquals("GenericDevice: Disconnected", result)
    }

    @Test
    fun `formatBatteryText should format positive battery level`() {
        val result = UIUtils.formatBatteryText(75)

        assertEquals("Battery: 75%", result)
    }

    @Test
    fun `formatBatteryText should format zero battery level`() {
        val result = UIUtils.formatBatteryText(0)

        assertEquals("Battery: 0%", result)
    }

    @Test
    fun `formatBatteryText should format negative battery level`() {
        val result = UIUtils.formatBatteryText(-1)

        assertEquals("Battery: ---%", result)
    }

    @Test
    fun `formatStreamingText should format active streaming`() {
        val result = UIUtils.formatStreamingText(true, 30, "1.2MB")

        assertEquals("Streaming: 30fps (1.2MB)", result)
    }

    @Test
    fun `formatStreamingText should format inactive streaming`() {
        val result = UIUtils.formatStreamingText(false, 0, "0MB")

        assertEquals("Ready to stream", result)
    }

    @Test
    fun `formatStreamingText should format ready when frame rate is zero`() {
        val result = UIUtils.formatStreamingText(true, 0, "0MB")

        assertEquals("Ready to stream", result)
    }

    @Test
    fun `getRecordingStatusText should return recording status`() {
        val result = UIUtils.getRecordingStatusText(true)

        assertEquals("Recording in progress...", result)
    }

    @Test
    fun `getRecordingStatusText should return ready status`() {
        val result = UIUtils.getRecordingStatusText(false)

        assertEquals("Ready to record", result)
    }

    @Test
    fun `styleButton should apply primary button style`() {
        UIUtils.styleButton(context, mockView, UIUtils.ButtonType.PRIMARY, true)

        verify { ContextCompat.getColor(context, R.color.colorSecondary) }
        verify { mockView.setBackgroundColor(any()) }
        verify { mockView.isEnabled = true }
        verify { mockView.alpha = 1.0f }
    }

    @Test
    fun `styleButton should apply success button style`() {
        UIUtils.styleButton(context, mockView, UIUtils.ButtonType.SUCCESS, true)

        verify { ContextCompat.getColor(context, R.color.colorPrimary) }
        verify { mockView.setBackgroundColor(any()) }
        verify { mockView.isEnabled = true }
        verify { mockView.alpha = 1.0f }
    }

    @Test
    fun `styleButton should apply danger button style`() {
        UIUtils.styleButton(context, mockView, UIUtils.ButtonType.DANGER, true)

        verify { ContextCompat.getColor(context, R.color.recordingActive) }
        verify { mockView.setBackgroundColor(any()) }
        verify { mockView.isEnabled = true }
        verify { mockView.alpha = 1.0f }
    }

    @Test
    fun `styleButton should apply secondary button style`() {
        UIUtils.styleButton(context, mockView, UIUtils.ButtonType.SECONDARY, true)

        verify { ContextCompat.getColor(context, R.color.textColorSecondary) }
        verify { mockView.setBackgroundColor(any()) }
        verify { mockView.isEnabled = true }
        verify { mockView.alpha = 1.0f }
    }

    @Test
    fun `styleButton should apply disabled state`() {
        UIUtils.styleButton(context, mockView, UIUtils.ButtonType.PRIMARY, false)

        verify { mockView.isEnabled = false }
        verify { mockView.alpha = 0.6f }
    }

    @Test
    fun `setViewVisibilityWithAnimation should show view`() {
        val mockAnimator = mockk<ViewPropertyAnimator>(relaxed = true)
        every { mockView.animate() } returns mockAnimator
        every { mockAnimator.alpha(any()) } returns mockAnimator
        every { mockAnimator.setDuration(any()) } returns mockAnimator

        UIUtils.setViewVisibilityWithAnimation(mockView, true, 100)

        verify { mockView.visibility = View.VISIBLE }
        verify { mockAnimator.alpha(1.0f) }
        verify { mockAnimator.setDuration(100) }
    }

    @Test
    fun `setViewVisibilityWithAnimation should hide view`() {
        val mockAnimator = mockk<ViewPropertyAnimator>(relaxed = true)
        every { mockView.animate() } returns mockAnimator
        every { mockAnimator.alpha(any()) } returns mockAnimator
        every { mockAnimator.setDuration(any()) } returns mockAnimator
        every { mockAnimator.withEndAction(any<Runnable>()) } returns mockAnimator

        UIUtils.setViewVisibilityWithAnimation(mockView, false, 100)

        verify { mockAnimator.alpha(0.0f) }
        verify { mockAnimator.setDuration(100) }
        verify { mockAnimator.withEndAction(any<Runnable>()) }
    }

    @Test
    fun `getOperationTimeout should return correct timeouts`() {
        assertEquals(10000L, UIUtils.getOperationTimeout(UIUtils.OperationType.CONNECTION))
        assertEquals(5000L, UIUtils.getOperationTimeout(UIUtils.OperationType.RECORDING_START))
        assertEquals(3000L, UIUtils.getOperationTimeout(UIUtils.OperationType.RECORDING_STOP))
        assertEquals(30000L, UIUtils.getOperationTimeout(UIUtils.OperationType.CALIBRATION))
        assertEquals(15000L, UIUtils.getOperationTimeout(UIUtils.OperationType.FILE_OPERATION))
    }
}
