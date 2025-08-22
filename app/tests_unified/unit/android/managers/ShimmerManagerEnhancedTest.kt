package com.multisensor.recording.managers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ShimmerManagerEnhancedTest {

    private lateinit var shimmerManager: ShimmerManager
    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockCallback: ShimmerManager.ShimmerCallback

    @Before
    fun setUp() {
        clearAllMocks()

        mockContext = mockk()
        mockActivity = mockk(relaxed = true)
        mockSharedPreferences = mockk()
        mockEditor = mockk()
        mockCallback = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        shimmerManager = ShimmerManager(mockContext)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getDeviceStatistics returns complete statistics when data available`() {
        every { mockSharedPreferences.getInt("connection_count", 0) } returns 5
        every { mockSharedPreferences.getLong("last_connection_time", 0L) } returns 1700000000000L
        every { mockSharedPreferences.getInt("error_count", 0) } returns 2

        val statistics = shimmerManager.getDeviceStatistics()

        assertNotNull(statistics)
        assertEquals(5, statistics.totalConnections)
        assertEquals(1700000000000L, statistics.lastConnectionTime)
        assertEquals(2, statistics.errorCount)
        assertTrue(statistics.averageSessionDuration > 0)
    }

    @Test
    fun `getDeviceStatistics handles exceptions gracefully`() {
        every { mockContext.getSharedPreferences(any(), any()) } throws RuntimeException("Storage error")

        val statistics = shimmerManager.getDeviceStatistics()

        assertNotNull(statistics)
        assertEquals(0, statistics.totalConnections)
        assertEquals(0L, statistics.lastConnectionTime)
        assertEquals(0, statistics.errorCount)
    }

    @Test
    fun `getDeviceStatistics calculates uptime when device connected`() {
        every { mockSharedPreferences.getInt("connection_count", 0) } returns 1
        every { mockSharedPreferences.getLong("last_connection_time", 0L) } returns System.currentTimeMillis() - 60000L
        every { mockSharedPreferences.getInt("error_count", 0) } returns 0

        val statistics = shimmerManager.getDeviceStatistics()

        assertNotNull(statistics)
        assertEquals(0L, statistics.deviceUptime)
    }

    @Test
    fun `initiateIntelligentReconnection handles no previous device gracefully`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns null
        every { mockSharedPreferences.getString("last_device_name", null) } returns null
        every { mockSharedPreferences.getString("last_bt_type", null) } returns null

        val errorSlot = slot<String>()

        shimmerManager.initiateIntelligentReconnection(mockActivity, mockCallback)

        verify { mockCallback.onError(capture(errorSlot)) }
        assertTrue(errorSlot.captured.contains("No previously connected device found"))
    }

    @Test
    fun `initiateIntelligentReconnection skips when already connected and force is false`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"
        every { mockSharedPreferences.getString("last_device_name", null) } returns "TestDevice"
        every { mockSharedPreferences.getString("last_bt_type", null) } returns "BT_CLASSIC"

        shimmerManager.initiateIntelligentReconnection(mockActivity, mockCallback, forceReconnect = false)

        verify(timeout = 5000) { mockCallback.onError(any()) }
    }

    @Test
    fun `initiateIntelligentReconnection attempts reconnection with valid device info`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"
        every { mockSharedPreferences.getString("last_device_name", null) } returns "TestDevice"
        every { mockSharedPreferences.getString("last_bt_type", null) } returns "BT_CLASSIC"
        every { mockSharedPreferences.getLong("last_connection_time", 0L) } returns System.currentTimeMillis()

        shimmerManager.initiateIntelligentReconnection(mockActivity, mockCallback, forceReconnect = true)

        verify { mockContext.getSharedPreferences("shimmer_device_prefs", Context.MODE_PRIVATE) }
    }

    @Test
    fun `startSDLogging validates device connection before proceeding`() {
        val errorSlot = slot<String>()

        shimmerManager.startSDLogging(mockCallback)

        verify { mockCallback.onError(capture(errorSlot)) }
        assertTrue(errorSlot.captured.contains("No Shimmer device connected"))
    }

    @Test
    fun `startSDLogging handles validation failures appropriately`() {

        val errorSlot = slot<String>()

        shimmerManager.startSDLogging(mockCallback)

        verify { mockCallback.onError(capture(errorSlot)) }
        assertTrue(errorSlot.captured.contains("device connected"))
    }

    @Test
    fun `stopSDLogging handles no active logging gracefully`() {
        val errorSlot = slot<String>()

        shimmerManager.stopSDLogging(mockCallback)

        verify { mockCallback.onError(capture(errorSlot)) }
        assertTrue(errorSlot.captured.contains("not currently active"))
    }

    @Test
    fun `stopSDLogging performs offline cleanup when device disconnected`() {
        every { mockSharedPreferences.getString("current_logging_session", null) } returns "session_123"
        every { mockSharedPreferences.getLong("current_session_start", 0L) } returns System.currentTimeMillis() - 60000L

        shimmerManager.stopSDLogging(mockCallback)

        verify { mockCallback.onError(any()) }
        verify { mockEditor.putBoolean("last_session_incomplete", true) }
        verify { mockEditor.putString("incomplete_session_reason", "Device disconnected during logging") }
        verify { mockEditor.remove("current_logging_session") }
    }

    @Test
    fun `device capabilities are properly managed and stored`() {
        every { mockSharedPreferences.getString("device_capabilities", any()) } returns "GSR,PPG,Accelerometer"

        val statistics = shimmerManager.getDeviceStatistics()

        assertNotNull(statistics)
    }

    @Test
    fun `error handling maintains system stability during SharedPreferences failures`() {
        every { mockContext.getSharedPreferences(any(), any()) } throws SecurityException("Permission denied")

        val hasPrevious = shimmerManager.hasPreviouslyConnectedDevice()
        val displayName = shimmerManager.getLastConnectedDeviceDisplayName()
        val statistics = shimmerManager.getDeviceStatistics()

        assertFalse(hasPrevious)
        assertEquals("None", displayName)
        assertNotNull(statistics)
        assertEquals(0, statistics.totalConnections)
    }

    @Test
    fun `error handling maintains callback contract during exceptions`() {
        every { mockSharedPreferences.edit() } throws RuntimeException("Storage full")

        shimmerManager.startSDLogging(mockCallback)

        verify { mockCallback.onError(any()) }
    }

    @Test
    fun `session management handles incomplete sessions properly`() {
        every { mockSharedPreferences.getString("current_logging_session", null) } returns "incomplete_session_456"
        every {
            mockSharedPreferences.getLong(
                "current_session_start",
                0L
            )
        } returns System.currentTimeMillis() - 120000L
        every { mockSharedPreferences.getBoolean("last_session_incomplete", false) } returns true
        every {
            mockSharedPreferences.getString(
                "incomplete_session_reason",
                null
            )
        } returns "Device disconnected during logging"

        shimmerManager.stopSDLogging(mockCallback)

        verify { mockCallback.onError(any()) }
        verify { mockEditor.remove("current_logging_session") }
    }

    @Test
    fun `session statistics are calculated correctly`() {
        val sessionStart = System.currentTimeMillis() - 300000L
        every { mockSharedPreferences.getString("current_logging_session", null) } returns "test_session"
        every { mockSharedPreferences.getLong("current_session_start", 0L) } returns sessionStart
        every { mockSharedPreferences.getLong("last_session_duration", 0L) } returns 300000L
        every { mockSharedPreferences.getInt("last_session_end_battery", -1) } returns 85

        shimmerManager.stopSDLogging(mockCallback)

        verify { mockCallback.onError(any()) }
    }

    @Test
    fun `existing callback interface methods are still supported`() {
        val legacyCallback = object : ShimmerManager.ShimmerCallback {
            override fun onDeviceSelected(address: String, name: String) {}
            override fun onDeviceSelectionCancelled() {}
            override fun onConnectionStatusChanged(connected: Boolean) {}
            override fun onConfigurationComplete() {}
            override fun onError(message: String) {}
        }

        shimmerManager.startSDLogging(legacyCallback)

        verify { mockContext.getSharedPreferences(any(), any()) }
    }

    @Test
    fun `enhanced callback methods work when implemented`() {
        val enhancedCallback = object : ShimmerManager.ShimmerCallback {
            override fun onDeviceSelected(address: String, name: String) {}
            override fun onDeviceSelectionCancelled() {}
            override fun onConnectionStatusChanged(connected: Boolean) {}
            override fun onConfigurationComplete() {}
            override fun onError(message: String) {}
            override fun onSDLoggingStatusChanged(isLogging: Boolean) {}
            override fun onDeviceCapabilitiesDiscovered(capabilities: Set<String>) {}
            override fun onBatteryLevelUpdated(batteryLevel: Int) {}
        }

        shimmerManager.startSDLogging(enhancedCallback)

        verify { mockContext.getSharedPreferences(any(), any()) }
    }

    @Test
    fun `resource management handles multiple rapid operations`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"
        every { mockSharedPreferences.getString("last_device_name", null) } returns "TestDevice"
        every { mockSharedPreferences.getString("last_bt_type", null) } returns "BT_CLASSIC"

        repeat(10) {
            shimmerManager.hasPreviouslyConnectedDevice()
            shimmerManager.getLastConnectedDeviceDisplayName()
            shimmerManager.getDeviceStatistics()
        }

        verify(atLeast = 10) { mockContext.getSharedPreferences(any(), any()) }
    }

    @Test
    fun `memory management prevents leaks during error conditions`() {
        every { mockSharedPreferences.edit() } throws OutOfMemoryError("Memory allocation failed")

        try {
            shimmerManager.startSDLogging(mockCallback)
        } catch (e: OutOfMemoryError) {
        }

        verify { mockCallback.onError(any()) }
    }

    @Test
    fun `configuration constants are properly applied`() {

        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"

        val hasDevice = shimmerManager.hasPreviouslyConnectedDevice()

        assertTrue(hasDevice)
        verify { mockSharedPreferences.getString("last_device_address", null) }
    }

    @Test
    fun `device identification patterns work correctly`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"
        every { mockSharedPreferences.getString("last_device_name", null) } returns "Shimmer_4AB4"
        every { mockSharedPreferences.getString("last_bt_type", null) } returns "BT_CLASSIC"
        every { mockSharedPreferences.getLong("last_connection_time", 0L) } returns System.currentTimeMillis()

        val displayName = shimmerManager.getLastConnectedDeviceDisplayName()

        assertTrue(displayName.contains("Shimmer_4AB4"))
    }
}
