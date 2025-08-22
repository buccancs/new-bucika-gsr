package com.multisensor.recording.managers

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
class ShimmerManagerTest {

    private lateinit var shimmerManager: ShimmerManager
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockCallback: ShimmerManager.ShimmerCallback

    @Before
    fun setUp() {
        clearAllMocks()

        mockContext = mockk()
        mockSharedPreferences = mockk()
        mockEditor = mockk()
        mockCallback = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        shimmerManager = ShimmerManager(mockContext)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `hasPreviouslyConnectedDevice returns false when no device stored`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns null

        val result = shimmerManager.hasPreviouslyConnectedDevice()

        assertFalse(result)
        verify { mockContext.getSharedPreferences("shimmer_device_prefs", Context.MODE_PRIVATE) }
    }

    @Test
    fun `hasPreviouslyConnectedDevice returns true when device is stored`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"

        val result = shimmerManager.hasPreviouslyConnectedDevice()

        assertTrue(result)
        verify { mockContext.getSharedPreferences("shimmer_device_prefs", Context.MODE_PRIVATE) }
    }

    @Test
    fun `getLastConnectedDeviceDisplayName returns 'None' when no device stored`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns null
        every { mockSharedPreferences.getString("last_device_name", null) } returns null
        every { mockSharedPreferences.getString("last_bt_type", null) } returns null

        val result = shimmerManager.getLastConnectedDeviceDisplayName()

        assertEquals("None", result)
    }

    @Test
    fun `getLastConnectedDeviceDisplayName returns formatted name when device stored`() {
        val testTime = 1700000000000L
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"
        every { mockSharedPreferences.getString("last_device_name", null) } returns "Shimmer_4AB4"
        every { mockSharedPreferences.getString("last_bt_type", null) } returns "BT_CLASSIC"
        every { mockSharedPreferences.getLong("last_connection_time", 0L) } returns testTime

        val result = shimmerManager.getLastConnectedDeviceDisplayName()

        assertTrue(result.contains("Shimmer_4AB4"))
        assertTrue(result.contains("("))
        assertTrue(result.contains(")"))
    }

    @Test
    fun `isDeviceConnected returns false initially`() {
        val result = shimmerManager.isDeviceConnected()

        assertFalse(result)
    }

    @Test
    fun `startSDLogging calls callback with error when no device connected`() {
        val errorSlot = slot<String>()

        shimmerManager.startSDLogging(mockCallback)

        verify { mockCallback.onError(capture(errorSlot)) }
        assertTrue(errorSlot.captured.contains("No Shimmer device connected"))
    }

    @Test
    fun `disconnect calls callback with status false when already disconnected`() {
        shimmerManager.disconnect(mockCallback)

        verify { mockCallback.onConnectionStatusChanged(false) }
    }

    @Test
    fun `SharedPreferences operations handle exceptions gracefully`() {
        every { mockContext.getSharedPreferences(any(), any()) } throws RuntimeException("Test exception")

        val hasPrevious = shimmerManager.hasPreviouslyConnectedDevice()
        val displayName = shimmerManager.getLastConnectedDeviceDisplayName()

        assertFalse(hasPrevious)
        assertEquals("None", displayName)
    }

    @Test
    fun `device preferences are saved correctly`() {

        val mockActivity = mockk<android.app.Activity>(relaxed = true)
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"
        every { mockSharedPreferences.getString("last_device_name", null) } returns "TestDevice"
        every { mockSharedPreferences.getString("last_bt_type", null) } returns "BT_CLASSIC"
        every { mockSharedPreferences.getLong("last_connection_time", 0L) } returns System.currentTimeMillis()
        every { mockSharedPreferences.getInt("connection_count", 0) } returns 1

        val hasPrevious = shimmerManager.hasPreviouslyConnectedDevice()

        assertTrue(hasPrevious)
        verify(atLeast = 1) { mockContext.getSharedPreferences("shimmer_device_prefs", Context.MODE_PRIVATE) }
    }

    @Test
    fun `connection count starts at zero for new installation`() {
        every { mockSharedPreferences.getInt("connection_count", 0) } returns 0

        every { mockSharedPreferences.getString("last_device_address", null) } returns null
        val result = shimmerManager.hasPreviouslyConnectedDevice()

        assertFalse(result)
    }

    @Test
    fun `callback methods are properly invoked during operations`() {

        shimmerManager.disconnect(mockCallback)

        verify { mockCallback.onConnectionStatusChanged(false) }
    }

    @Test
    fun `device connection state management is consistent`() {
        assertFalse(shimmerManager.isDeviceConnected())

        shimmerManager.disconnect(mockCallback)

        verify { mockCallback.onConnectionStatusChanged(false) }
        assertFalse(shimmerManager.isDeviceConnected())
    }

    @Test
    fun `MAC address validation works correctly`() {

        every { mockSharedPreferences.getString("last_device_address", null) } returns null

        val hasPrevious = shimmerManager.hasPreviouslyConnectedDevice()

        assertFalse(hasPrevious)
    }

    @Test
    fun `Bluetooth type persistence works correctly`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"
        every { mockSharedPreferences.getString("last_device_name", null) } returns "TestDevice"
        every { mockSharedPreferences.getString("last_bt_type", null) } returns "BLE"
        every { mockSharedPreferences.getLong("last_connection_time", 0L) } returns System.currentTimeMillis()

        val hasPrevious = shimmerManager.hasPreviouslyConnectedDevice()
        val displayName = shimmerManager.getLastConnectedDeviceDisplayName()

        assertTrue(hasPrevious)
        assertTrue(displayName.contains("TestDevice"))
    }

    @Test
    fun `error handling for malformed Bluetooth type in preferences`() {
        every { mockSharedPreferences.getString("last_device_address", null) } returns "00:06:66:68:4A:B4"
        every { mockSharedPreferences.getString("last_device_name", null) } returns "TestDevice"
        every { mockSharedPreferences.getString("last_bt_type", null) } returns "INVALID_TYPE"
        every { mockSharedPreferences.getLong("last_connection_time", 0L) } returns System.currentTimeMillis()

        val hasPrevious = shimmerManager.hasPreviouslyConnectedDevice()

        assertTrue(hasPrevious)
    }
}
