package com.multisensor.recording.ui.components

import android.content.Context
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusIndicatorViewTest {

    private lateinit var context: Context
    private lateinit var statusIndicatorView: StatusIndicatorView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        statusIndicatorView = StatusIndicatorView(context)
    }

    @Test
    fun testInitialState() {
        assertNotNull(statusIndicatorView)
        assertEquals(
            "Status: Disconnected",
            statusIndicatorView.findViewById<android.widget.TextView>(android.R.id.text1)?.text
        )
    }

    @Test
    fun testSetStatusConnected() {
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.CONNECTED, "PC: Connected")

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals("PC: Connected", textView.text)

        val indicator = statusIndicatorView.getChildAt(0)
        assertNotNull(indicator)
    }

    @Test
    fun testSetStatusDisconnected() {
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.DISCONNECTED, "Shimmer: Disconnected")

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals("Shimmer: Disconnected", textView.text)
    }

    @Test
    fun testSetStatusRecording() {
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.RECORDING, "Recording: Active")

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals("Recording: Active", textView.text)
    }

    @Test
    fun testSetStatusStopped() {
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.STOPPED, "Recording: Stopped")

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals("Recording: Stopped", textView.text)
    }

    @Test
    fun testSetStatusWarning() {
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.WARNING, "Warning: Low Battery")

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals("Warning: Low Battery", textView.text)
    }

    @Test
    fun testSetStatusError() {
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.ERROR, "Error: Connection Failed")

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals("Error: Connection Failed", textView.text)
    }

    @Test
    fun testSetCustomTextColor() {
        statusIndicatorView.setTextColor(android.R.color.holo_blue_light)

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertNotNull(textView)
    }

    @Test
    fun testSetCustomTextSize() {
        val customSize = 16f
        statusIndicatorView.setTextSize(customSize)

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, customSize, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, textView.textSize, 0.1f)
    }

    @Test
    fun testComponentStructure() {
        assertEquals(2, statusIndicatorView.childCount)

        val indicator = statusIndicatorView.getChildAt(0)
        assertNotNull(indicator)
        assertTrue(indicator is android.view.View)

        val textView = statusIndicatorView.getChildAt(1)
        assertNotNull(textView)
        assertTrue(textView is android.widget.TextView)
    }

    @Test
    fun testLayoutOrientation() {
        assertEquals(android.widget.LinearLayout.HORIZONTAL, statusIndicatorView.orientation)
    }

    @Test
    fun testMultipleStatusUpdates() {
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.DISCONNECTED, "Initial State")
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.CONNECTED, "Connected State")
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.RECORDING, "Recording State")

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals("Recording State", textView.text)
    }

    @Test
    fun testDefaultTextSize() {
        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, textView.textSize, 0.1f)
    }

    @Test
    fun testStatusTypeEnumValues() {
        val expectedValues = setOf(
            StatusIndicatorView.StatusType.CONNECTED,
            StatusIndicatorView.StatusType.DISCONNECTED,
            StatusIndicatorView.StatusType.RECORDING,
            StatusIndicatorView.StatusType.STOPPED,
            StatusIndicatorView.StatusType.WARNING,
            StatusIndicatorView.StatusType.ERROR
        )

        val actualValues = StatusIndicatorView.StatusType.entries.toSet()
        assertEquals(expectedValues, actualValues)
    }

    @Test
    fun testEmptyStatusText() {
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.CONNECTED, "")

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals("", textView.text)
    }

    @Test
    fun testLongStatusText() {
        val longText = "This is a very long status message that should still be handled properly by the component"
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.WARNING, longText)

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals(longText, textView.text)
    }

    @Test
    fun testSpecialCharactersInStatus() {
        val specialText = "Status with émojis 📡 and symbols ★ ♦ ♠"
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.CONNECTED, specialText)

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals(specialText, textView.text)
    }

    @Test
    fun testStatusWithNumbers() {
        val numberText = "Device 1.2.3: Connected"
        statusIndicatorView.setStatus(StatusIndicatorView.StatusType.CONNECTED, numberText)

        val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
        assertEquals(numberText, textView.text)
    }

    @Test
    fun testTextSizeWithDifferentValues() {
        val testSizes = arrayOf(12f, 14f, 16f, 18f, 20f)

        for (size in testSizes) {
            statusIndicatorView.setTextSize(size)
            val textView = statusIndicatorView.getChildAt(1) as android.widget.TextView
            val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, context.resources.displayMetrics)
            assertEquals("Text size should be $size SP", expectedSizeInPixels, textView.textSize, 0.1f)
        }
    }
}
